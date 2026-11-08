package uz.alesle.avto.apps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.os.SystemClock
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.alesle.avto.R
import uz.alesle.avto.host.CarRuntime
import uz.alesle.avto.media.Placeholder
import uz.alesle.avto.ui.car.CarAppEntry
import uz.alesle.avto.ui.car.CarAppKind
import uz.alesle.avto.ui.theme.AppDisplayBackground
import rikka.shizuku.Shizuku

/**
 * A phone app ([CarAppKind.PHONE_APP]) running as itself on the car screen: the shared
 * [AppDisplay] drawn into a TextureView over the content area (the rail stays), the car's
 * touches and Back key sent to the app. TextureView rather than SurfaceView: it is drawn inside
 * the car UI's own window, so it fades and clips with the screen transitions (the phone screen
 * mirror shows the same way).
 */
@Composable
internal fun AppDisplayScreen(runtime: CarRuntime, app: CarAppEntry, modifier: Modifier) {
    // Shizuku can be started, stopped or allowed while this is on screen.
    var access by remember { mutableStateOf(AppDisplay.access()) }
    LaunchedEffect(Unit) {
        while (true) {
            access = AppDisplay.access()
            delay(1_500)
        }
    }
    val driving = runtime.ui.drivingRestricted
    val known = runtime.ui.apps.filter { it.kind == CarAppKind.NAVIGATION || it.kind == CarAppKind.MEDIA }.mapTo(HashSet()) { it.packageName }
    Box(modifier.background(AppDisplayBackground)) {
        when {
            access != AppDisplay.Access.READY -> ShizukuCard(runtime, app, access)
            driving && !PhoneApps.drivingAllowed(app.packageName, known) ->
                Placeholder(stringResource(R.string.phone_apps_driving_only), loading = false, icon = Icons.Rounded.PauseCircle)
            else -> AppSurface(runtime, app)
        }
    }
}

@Composable
private fun AppSurface(runtime: CarRuntime, app: CarAppEntry) {
    val display = runtime.appDisplay
    val component = remember(app.id) { CarRuntime.phoneAppComponent(app.id) }
    var attempt by remember(app.id) { mutableIntStateOf(0) }
    var launchError by remember(app.id) { mutableStateOf<String?>(null) }
    var launched by remember(app.id) { mutableStateOf(false) }
    /** The app was in front, then the display emptied: closed (Back on its first screen) or moved to the phone. */
    var left by remember(app.id) { mutableStateOf(false) }
    val state = display.state

    val scope = rememberCoroutineScope()
    /** When Back was last sent to the app (uptime ms). */
    var backAt by remember(app.id) { mutableLongStateOf(0L) }
    DisposableEffect(display, app.id) {
        // The car's back key and the rail's Back button go to the app. When the app has nothing
        // left to go back to (its task finishes or goes behind), the car goes back to the launcher
        // rather than showing an empty display.
        runtime.screenBack = {
            backAt = SystemClock.uptimeMillis()
            scope.launch {
                val before = withContext(Dispatchers.IO) { display.topPackage() }
                display.back()
                repeat(4) {
                    delay(400)
                    val now = withContext(Dispatchers.IO) { display.topPackage() } ?: return@repeat
                    if (PhoneApps.backedOut(app.packageName, before, now)) {
                        runtime.actions.openLauncher()
                        return@launch
                    }
                }
            }
            true
        }
        onDispose { runtime.screenBack = null }
    }
    // Start the app once the display exists, and again on "Open again" / "Try again".
    LaunchedEffect(app.id, state is AppDisplay.State.Ready, attempt) {
        if (state !is AppDisplay.State.Ready || component == null) return@LaunchedEffect
        launchError = null
        left = false
        display.launch(component) { error -> launchError = error; launched = error == null }
    }
    // Watch what is in front of the display.
    LaunchedEffect(app.id, launched) {
        if (!launched) return@LaunchedEffect
        var seen = false
        while (true) {
            delay(1_500)
            val top = withContext(Dispatchers.IO) { display.topPackage() } ?: continue
            if (top == app.packageName) seen = true
            // Another package in front is fine (Maps opening Navigator, a browser…); nothing at all isn't.
            if (top.isEmpty() && seen) {
                // Emptied by our own Back (caught here if the check after it missed): to the launcher.
                if (SystemClock.uptimeMillis() - backAt < BACK_WINDOW_MS) runtime.actions.openLauncher() else left = true
                launched = false
                break
            }
        }
    }

    androidx.compose.runtime.key(attempt) {
        AndroidView(
            factory = { ctx -> AppSurfaceView(ctx, display, runtime.densityDpi) },
            modifier = Modifier.fillMaxSize(),
        )
    }
    when {
        state is AppDisplay.State.Failed || launchError != null -> Message(
            app, stringResource(R.string.app_display_failed, app.label, (state as? AppDisplay.State.Failed)?.reason ?: launchError.orEmpty()),
            primary = stringResource(R.string.app_display_retry) to { attempt++ },
            secondary = stringResource(R.string.dash_menu_home) to { runtime.actions.goHome() },
        )
        left -> Message(
            app, stringResource(R.string.app_display_left, app.label),
            primary = stringResource(R.string.app_display_open_again) to { attempt++ },
            secondary = stringResource(R.string.dash_menu_home) to { runtime.actions.goHome() },
        )
        state !is AppDisplay.State.Ready || !launched -> Message(app, null)
    }
}

/** Shizuku isn't there or AARE isn't allowed: what to do, and the phone screen mirror as a way out. */
@Composable
private fun ShizukuCard(runtime: CarRuntime, app: CarAppEntry, access: AppDisplay.Access) {
    val mirror = runtime.ui.apps.firstOrNull { it.kind == CarAppKind.PHONE_SCREEN }
    val text = when (access) {
        AppDisplay.Access.NEEDS_PERMISSION -> stringResource(R.string.app_display_shizuku_permission)
        AppDisplay.Access.UNSUPPORTED -> stringResource(R.string.app_display_shizuku_unsupported)
        else -> stringResource(R.string.app_display_shizuku_not_running)
    } + if (mirror == null) "\n\n" + stringResource(R.string.app_display_phone_screen_hint) else ""
    Message(
        app, text,
        primary = if (access == AppDisplay.Access.NEEDS_PERMISSION) {
            stringResource(R.string.app_display_allow) to { runCatching { Shizuku.requestPermission(SHIZUKU_REQUEST) }; Unit }
        } else null,
        secondary = mirror?.let { stringResource(R.string.app_display_use_phone_screen) to { runtime.actions.openApp(it) } },
    )
}

/** The app's icon with a spinner ([text] null) or a message and up to two buttons. */
@Composable
private fun Message(
    app: CarAppEntry,
    text: String?,
    primary: Pair<String, () -> Unit>? = null,
    secondary: Pair<String, () -> Unit>? = null,
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        app.icon?.let { Image(it.asImageBitmap(), null, Modifier.size(72.dp).clip(CircleShape)) }
        Spacer(Modifier.height(20.dp))
        if (text == null) {
            CircularProgressIndicator()
            return@Column
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 640.dp))
        if (primary != null || secondary != null) {
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                primary?.let { (label, run) -> Button(label, filled = true, run) }
                secondary?.let { (label, run) -> Button(label, filled = false, run) }
            }
        }
    }
}

@Composable
private fun Button(label: String, filled: Boolean, onClick: () -> Unit) {
    val bg = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val fg = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
    Box(
        Modifier.heightIn(min = 64.dp).clip(RoundedCornerShape(32.dp)).background(bg).clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

/**
 * Draws the app display and sends it the touches it gets, in display pixels, all pointers kept.
 * The display has the view's pixel size (touches map 1:1) at a [DisplayDensity] that makes it a
 * large screen; it never rotates, so no rotation is undone here.
 */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
private class AppSurfaceView(context: Context, private val display: AppDisplay, private val dpi: Int) :
    TextureView(context), TextureView.SurfaceTextureListener {
    private var surface: Surface? = null
    private var map = DisplayTouch(1, 1, 1, 1)

    init {
        surfaceTextureListener = this
        // Where nothing is drawn (the bars beside a letterboxed app) the car background shows.
        isOpaque = false
    }

    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
        val s = Surface(st)
        surface = s
        map = DisplayTouch(w, h, w, h)
        display.attach(s, w, h, DisplayDensity.pick(w, h, dpi))
    }

    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
        val s = surface ?: return
        map = DisplayTouch(w, h, w, h)
        display.attach(s, w, h, DisplayDensity.pick(w, h, dpi))
    }

    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
        surface?.let { display.detach(it); it.release() }
        surface = null
        return true
    }

    override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val (dw, dh) = display.size
        if (dw > 0 && (dw != map.displayWidth || dh != map.displayHeight)) map = DisplayTouch(width, height, dw, dh)
        display.touch(mapped(event, map))
        return true
    }

    private fun mapped(e: MotionEvent, m: DisplayTouch): MotionEvent {
        if (m.identity) return MotionEvent.obtain(e)
        val n = e.pointerCount
        val props = Array(n) { i -> MotionEvent.PointerProperties().also { e.getPointerProperties(i, it) } }
        val coords = Array(n) { i -> MotionEvent.PointerCoords().also { e.getPointerCoords(i, it); it.x = m.x(it.x); it.y = m.y(it.y) } }
        return MotionEvent.obtain(e.downTime, e.eventTime, e.action, n, props, coords, e.metaState, e.buttonState,
            1f, 1f, e.deviceId, e.edgeFlags, e.source, e.flags)
    }
}

private const val SHIZUKU_REQUEST = 7311
private const val BACK_WINDOW_MS = 4_000L

