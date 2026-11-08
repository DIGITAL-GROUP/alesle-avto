package net.prototux.aare.apps

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.prototux.aare.BuildConfig
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * One display for phone apps shown on the car screen, created by [ShellDisplayService] through
 * Shizuku and reused for every app (each keeps its task on it, the last opened in front). The car
 * screen hands it the surface of its content area; the display follows that surface's size.
 *
 * Calls come from the main thread; the binder work runs on a thread of its own, in order.
 */
class AppDisplay(private val context: Context) {
    sealed interface State {
        data object Idle : State
        data object Starting : State
        data class Ready(val displayId: Int) : State
        data class Failed(val reason: String) : State
    }

    /** Compose state, written on the main thread. */
    var state by mutableStateOf<State>(State.Idle)
        private set

    private val main = Handler(Looper.getMainLooper())
    private val worker = HandlerThread("aare-app-display").apply { start() }
    private val bg = Handler(worker.looper)

    @Volatile private var service: IShellDisplay? = null
    @Volatile private var displayId = -1
    private var bound = false
    private var latch = CountDownLatch(1)
    // Worker thread only: what the display was last set to.
    private var width = 0
    private var height = 0
    private var dpi = 0

    private val args = Shizuku.UserServiceArgs(ComponentName(context.packageName, ShellDisplayService::class.java.name))
        .daemon(false)
        .processNameSuffix("display")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = binder?.takeIf { it.pingBinder() }?.let(IShellDisplay.Stub::asInterface)
            latch.countDown()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // Shizuku stopped (or the phone rebooted it): the display went with it.
            service = null
            displayId = -1
            bound = false
            main.post { if (state !is State.Idle) state = State.Failed("Shizuku stopped") }
        }
    }

    /** The surface the display draws into now (main thread). */
    private var current: Surface? = null

    /** The display's size in pixels, as last attached (main thread). */
    var size: Pair<Int, Int> = 0 to 0
        private set

    /** Shows the display in [surface] ([w]×[h] pixels at [densityDpi]), creating it the first time. */
    fun attach(surface: Surface, w: Int, h: Int, densityDpi: Int) {
        current = surface
        size = w to h
        if (state !is State.Ready) state = State.Starting
        bg.post {
            val s = connect() ?: return@post fail("Shizuku isn't available")
            try {
                if (displayId < 0) {
                    val id = s.create(surface, w, h, densityDpi)
                    if (id < 0) return@post fail("Could not create a display")
                    displayId = id
                } else {
                    if (w != width || h != height || densityDpi != dpi) s.resize(displayId, w, h, densityDpi)
                    s.setSurface(displayId, surface)
                }
                width = w; height = h; dpi = densityDpi
                val id = displayId
                main.post { state = State.Ready(id) }
            } catch (e: Exception) {
                Log.w(TAG, "attach", e)
                fail(e.message ?: e.toString())
            }
        }
    }

    /**
     * Stops drawing into [surface] (it is going away); the display and its apps stay for the next
     * [attach]. Ignored if another surface took over meanwhile (switching apps cross-fades two views).
     */
    fun detach(surface: Surface) {
        if (current !== surface) return
        current = null
        bg.post { runCatching { displayId.takeIf { it >= 0 }?.let { service?.setSurface(it, null) } } }
    }

    /** Starts [component] on the display (or brings it to front); [done] gets null or why it failed, on the main thread. */
    fun launch(component: ComponentName, done: (String?) -> Unit = {}) {
        bg.post {
            val s = service
            val id = displayId
            val error = if (s == null || id < 0) "No display"
            else runCatching { s.launch(id, component.packageName, component.className) }.getOrElse { it.message ?: it.toString() }
            if (error != null) Log.w(TAG, "launch ${component.flattenToShortString()}: $error")
            main.post { done(error) }
        }
    }

    /** A touch on the content area, already in display pixels. Takes [event] over (recycled after sending). */
    fun touch(event: MotionEvent) {
        val s = service
        val id = displayId
        try {
            // One-way: never blocks the UI thread, and arrives in order.
            if (s != null && id >= 0) s.injectMotion(id, event)
        } catch (e: Exception) {
            Log.w(TAG, "touch", e)
        } finally {
            event.recycle()
        }
    }

    fun back() = key(KeyEvent.KEYCODE_BACK)

    private fun key(code: Int) {
        val s = service ?: return
        val id = displayId.takeIf { it >= 0 } ?: return
        runCatching { s.injectKey(id, code) }
    }

    /** The package in front on the display ("" if none, null if unknown). Blocking: call off the main thread. */
    fun topPackage(): String? {
        val s = service ?: return null
        val id = displayId.takeIf { it >= 0 } ?: return null
        return runCatching { s.topPackage(id) }.getOrNull()
    }

    /** Removes the display (its apps are closed) and stops the shell process. */
    fun release() {
        state = State.Idle
        bg.post {
            runCatching { displayId.takeIf { it >= 0 }?.let { service?.release(it) } }
            displayId = -1
            if (bound) main.post { runCatching { Shizuku.unbindUserService(args, connection, true) } }
            bound = false
            service = null
        }
        worker.quitSafely()
    }

    /** The service, binding it first if needed. Worker thread. */
    private fun connect(): IShellDisplay? {
        service?.let { return it }
        if (access() != Access.READY) return null
        if (!bound) {
            latch = CountDownLatch(1)
            val l = latch
            bound = true
            main.post { runCatching { Shizuku.bindUserService(args, connection) }.onFailure { Log.w(TAG, "bind", it); l.countDown() } }
        }
        latch.await(10, TimeUnit.SECONDS)
        return service
    }

    private fun fail(reason: String) {
        Log.w(TAG, reason)
        main.post { state = State.Failed(reason) }
    }

    /** Whether AARE may use Shizuku now. */
    enum class Access { READY, NEEDS_PERMISSION, NOT_RUNNING, UNSUPPORTED }

    companion object {
        private const val TAG = "AareAppDisplay"

        fun access(): Access = when {
            // Root task queries and IME policies on other displays: Android 12+.
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> Access.UNSUPPORTED
            !runCatching { Shizuku.pingBinder() }.getOrDefault(false) -> Access.NOT_RUNNING
            runCatching { Shizuku.isPreV11() }.getOrDefault(true) -> Access.UNSUPPORTED
            runCatching { Shizuku.checkSelfPermission() }.getOrDefault(PackageManager.PERMISSION_DENIED) != PackageManager.PERMISSION_GRANTED -> Access.NEEDS_PERMISSION
            else -> Access.READY
        }
    }
}
