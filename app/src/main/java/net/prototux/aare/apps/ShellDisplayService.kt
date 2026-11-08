package net.prototux.aare.apps

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import net.prototux.aare.shell.ShellContext
import net.prototux.aare.shell.ShellContexts
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.system.exitProcess

/**
 * Runs in a separate process that Shizuku starts with adb (shell) rights: creates virtual
 * displays for phone apps shown on the car screen, starts apps on them and injects the car's
 * touches and keys into them. The calls and flags follow scrcpy's server (NewDisplayCapture,
 * Device.startApp, the InputManager and ActivityManager wrappers), which does the same as shell.
 *
 * Everything here is hidden API, reached through reflection with the hidden-API checks lifted
 * (this process isn't an app, nothing else of AARE runs in it).
 */
class ShellDisplayService : IShellDisplay.Stub {
    private val context: Context
    private val displays = HashMap<Int, VirtualDisplay>()

    constructor() : this(null)

    /** Shizuku passes a Context when the service has this constructor. */
    constructor(context: Context?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) HiddenApiBypass.addHiddenApiExemptions("L")
        this.context = ShellContext(context ?: ShellContexts.systemContext())
    }

    /** A DisplayManager whose calls carry the shell's package name, as the display service checks it against our uid. */
    private val displayManager: DisplayManager by lazy {
        DisplayManager::class.java.getDeclaredConstructor(Context::class.java).apply { isAccessible = true }.newInstance(context)
    }

    override fun create(surface: Surface?, width: Int, height: Int, densityDpi: Int): Int = try {
        val vd = displayManager.createVirtualDisplay("AARE apps", width, height, densityDpi, surface, flags())
        val id = vd.display.displayId
        synchronized(displays) { displays[id] = vd }
        keepUpright(id)
        restorePip() // left over if a previous service process died without releasing
        // The keyboard shows on this display, not on the phone (scrcpy's --display-ime-policy=local).
        runCatching { setImePolicy(id, DISPLAY_IME_POLICY_LOCAL) }.onFailure { Log.w(TAG, "IME policy", it) }
        Log.i(TAG, "display $id: ${width}x$height/$densityDpi")
        id
    } catch (e: Exception) {
        Log.w(TAG, "can't create a display", e)
        -1
    }

    override fun resize(displayId: Int, width: Int, height: Int, densityDpi: Int) {
        display(displayId)?.resize(width, height, densityDpi)
    }

    override fun setSurface(displayId: Int, surface: Surface?) {
        display(displayId)?.surface = surface
    }

    override fun launch(displayId: Int, packageName: String, activity: String): String? {
        if (display(displayId) == null) return "no display $displayId"
        denyPip(packageName)
        // An app already open on the phone would come to front there: move its task over first.
        runCatching { rootTasks().filter { it.packageName == packageName && it.displayId != displayId }.forEach { moveTask(it.taskId, displayId) } }
            .onFailure { Log.w(TAG, "moving $packageName's task", it) }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(packageName, activity))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId)
        runCatching { ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType).invoke(options, WINDOWING_MODE_FULLSCREEN) }
        return try {
            val result = startActivity(intent, options.toBundle())
            Log.i(TAG, "start $packageName/$activity on display $displayId: $result")
            if (result < 0) "start failed ($result)" else null
        } catch (e: Exception) {
            Log.w(TAG, "startActivityAsUser failed, trying am start", e)
            amStart(displayId, ComponentName(packageName, activity))
        }
    }

    override fun injectMotion(displayId: Int, event: MotionEvent?) {
        if (event == null) return
        try {
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            inject(event, displayId)
        } finally {
            event.recycle()
        }
    }

    override fun injectKey(displayId: Int, keyCode: Int) {
        val now = SystemClock.uptimeMillis()
        for (action in intArrayOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            inject(KeyEvent(now, now, action, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD), displayId)
        }
    }

    override fun topPackage(displayId: Int): String? = runCatching {
        // getAllRootTaskInfos lists the top task first. A task in picture-in-picture doesn't count:
        // it isn't drawn on this display, the app has left the screen.
        rootTasks().firstOrNull { it.displayId == displayId && it.visible && !it.pinned }?.packageName ?: ""
    }.onFailure { Log.w(TAG, "root tasks", it) }.getOrNull()

    override fun release(displayId: Int) {
        val vd = synchronized(displays) { displays.remove(displayId) } ?: return
        runCatching { vd.release() }
        Log.i(TAG, "display $displayId released")
        if (synchronized(displays) { displays.isEmpty() }) restorePip()
    }

    override fun destroy() {
        synchronized(displays) { displays.keys.toList() }.forEach(::release)
        exitProcess(0)
    }

    private fun display(id: Int): VirtualDisplay? = synchronized(displays) { displays[id] }

    /**
     * scrcpy's new-display flags: public and trusted (apps may start there), touchable, unlocked, its
     * own focus. Not ROTATES_WITH_CONTENT, unlike scrcpy: the car screen is fixed, so the display
     * must not turn when an app asks for portrait ([keepUpright]).
     */
    private fun flags(): Int {
        var f = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION or
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or FLAG_SUPPORTS_TOUCH or FLAG_DESTROY_CONTENT_ON_REMOVAL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            f = f or FLAG_TRUSTED or FLAG_OWN_DISPLAY_GROUP or FLAG_ALWAYS_UNLOCKED or FLAG_TOUCH_FEEDBACK_DISABLED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) f = f or FLAG_OWN_FOCUS or FLAG_DEVICE_DISPLAY_GROUP
        }
        return f
    }

    /**
     * Holds the display at rotation 0 and has it ignore apps' orientation requests (Android 12L+):
     * an app that wants portrait is then resized to the landscape display (Android 16+, large
     * screens) or letterboxed in the centre, instead of the whole display turning. (The letterbox
     * colour isn't set: it is global, and HyperOS doesn't draw it on this display anyway. The bars
     * are left empty and the car screen's background shows through.)
     */
    private fun keepUpright(id: Int) {
        wm("user-rotation", "-d", "$id", "lock", "0")
        wm("set-ignore-orientation-request", "-d", "$id", "true")
        wm("get-ignore-orientation-request", "-d", "$id")
    }

    private fun wm(vararg args: String): String = run("wm", *args).also { Log.i(TAG, "wm ${args.joinToString(" ")}: $it") }

    /** Runs a shell command as this (shell) process; its output, or the error. */
    private fun run(vararg cmd: String): String = runCatching {
        val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        out.trim()
    }.getOrElse { it.toString() }

    // ------------------------------------------------------------------ picture-in-picture

    /**
     * Apps on the car display must not go picture-in-picture (YouTube does on Back from a video):
     * the small window isn't drawn on this display, so the car screen would go black. While an app
     * is shown here its PICTURE_IN_PICTURE app-op is "ignore" (then Back returns to its own home
     * page); the mode it had is kept in [PIP_FILE] and put back when the last display goes, or by
     * the next service if this one dies first.
     */
    private fun denyPip(packageName: String) {
        val saved = savedPip()
        if (packageName in saved) return
        val mode = Regex("""PICTURE_IN_PICTURE: (\w+)""").find(run("appops", "get", packageName, "PICTURE_IN_PICTURE"))?.groupValues?.get(1) ?: "default"
        if (mode == "ignore") return
        runCatching { java.io.File(PIP_FILE).appendText("$packageName $mode\n") }.onFailure { Log.w(TAG, "PiP file", it) }
        Log.i(TAG, "PiP off for $packageName (was $mode): ${run("appops", "set", packageName, "PICTURE_IN_PICTURE", "ignore")}")
    }

    private fun restorePip() {
        savedPip().forEach { (pkg, mode) -> Log.i(TAG, "PiP back to $mode for $pkg: ${run("appops", "set", pkg, "PICTURE_IN_PICTURE", mode)}") }
        runCatching { java.io.File(PIP_FILE).delete() }
    }

    private fun savedPip(): Map<String, String> = runCatching {
        java.io.File(PIP_FILE).readLines().mapNotNull { line -> line.split(' ').takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
    }.getOrDefault(emptyMap())

    // ------------------------------------------------------------------ input

    private val inputManager: InputManager by lazy { context.getSystemService(InputManager::class.java) }

    private fun inject(event: InputEvent, displayId: Int): Boolean = try {
        InputEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType).invoke(event, displayId)
        InputManager::class.java.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
            .invoke(inputManager, event, INJECT_INPUT_EVENT_MODE_ASYNC) as Boolean
    } catch (e: Exception) {
        Log.w(TAG, "inject", e)
        false
    }

    // ------------------------------------------------------------------ activities

    /** IActivityManager.startActivityAsUser, as the shell (scrcpy's ActivityManager.startActivity). */
    @SuppressLint("PrivateApi")
    private fun startActivity(intent: Intent, options: Bundle?): Int {
        val am = Class.forName("android.app.ActivityManager").getMethod("getService").invoke(null)!!
        val method = am.javaClass.getMethod(
            "startActivityAsUser", Class.forName("android.app.IApplicationThread"), String::class.java, Intent::class.java,
            String::class.java, IBinder::class.java, String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
            Class.forName("android.app.ProfilerInfo"), Bundle::class.java, Int::class.javaPrimitiveType,
        )
        return method.invoke(am, null, ShellContexts.SHELL_PACKAGE, intent, null, null, null, 0, 0, null, options, USER_CURRENT) as Int
    }

    /** The shell command, if the binder call's signature changed in this Android version. */
    private fun amStart(displayId: Int, component: ComponentName): String? {
        val out = run("am", "start", "--display", displayId.toString(), "--windowingMode", WINDOWING_MODE_FULLSCREEN.toString(),
            "-a", Intent.ACTION_MAIN, "-c", Intent.CATEGORY_LAUNCHER, "-f", Intent.FLAG_ACTIVITY_NEW_TASK.toString(),
            "-n", component.flattenToShortString())
        Log.i(TAG, "am start: $out")
        return if ("Error" in out || "Exception" in out) out else null
    }

    private class RootTask(val taskId: Int, val displayId: Int, val packageName: String?, val visible: Boolean, val pinned: Boolean)

    @SuppressLint("PrivateApi")
    private fun activityTaskManager(): Any = Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null)!!

    /** The root tasks on all displays, top first (IActivityTaskManager.getAllRootTaskInfos, Android 12+). */
    private fun rootTasks(): List<RootTask> {
        val atm = activityTaskManager()
        val infos = atm.javaClass.getMethod("getAllRootTaskInfos").invoke(atm) as List<*>
        return infos.filterNotNull().map { info ->
            val c = info.javaClass
            fun field(name: String): Any? = runCatching { c.getField(name).get(info) }.getOrNull()
            val top = (field("topActivity") ?: field("baseActivity")) as? ComponentName
            val mode = runCatching {
                val wc = Configuration::class.java.getField("windowConfiguration").get(field("configuration"))!!
                wc.javaClass.getMethod("getWindowingMode").invoke(wc) as Int
            }.getOrDefault(0)
            RootTask(
                field("taskId") as Int, field("displayId") as Int, top?.packageName,
                (field("visible") ?: field("isVisible")) as? Boolean ?: true, mode == WINDOWING_MODE_PINNED,
            )
        }
    }

    private fun moveTask(taskId: Int, displayId: Int) {
        val atm = activityTaskManager()
        atm.javaClass.getMethod("moveRootTaskToDisplay", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType).invoke(atm, taskId, displayId)
        Log.i(TAG, "moved task $taskId to display $displayId")
    }

    @SuppressLint("PrivateApi")
    private fun setImePolicy(displayId: Int, policy: Int) {
        val binder = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java).invoke(null, "window") as IBinder
        val wm = Class.forName("android.view.IWindowManager\$Stub").getMethod("asInterface", IBinder::class.java).invoke(null, binder)!!
        wm.javaClass.getMethod("setDisplayImePolicy", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType).invoke(wm, displayId, policy)
    }

    private companion object {
        const val TAG = "AareShellDisplay"
        // Hidden DisplayManager flags (the values scrcpy uses).
        const val FLAG_SUPPORTS_TOUCH = 1 shl 6
        const val FLAG_DESTROY_CONTENT_ON_REMOVAL = 1 shl 8
        const val FLAG_TRUSTED = 1 shl 10
        const val FLAG_OWN_DISPLAY_GROUP = 1 shl 11
        const val FLAG_ALWAYS_UNLOCKED = 1 shl 12
        const val FLAG_TOUCH_FEEDBACK_DISABLED = 1 shl 13
        const val FLAG_OWN_FOCUS = 1 shl 14
        const val FLAG_DEVICE_DISPLAY_GROUP = 1 shl 15
        const val DISPLAY_IME_POLICY_LOCAL = 0
        const val WINDOWING_MODE_FULLSCREEN = 1
        const val WINDOWING_MODE_PINNED = 2
        /** Shell-writable: the PiP app-op modes to put back (package and mode per line). */
        const val PIP_FILE = "/data/local/tmp/aare-pip-modes.txt"
        const val INJECT_INPUT_EVENT_MODE_ASYNC = 0
        const val USER_CURRENT = -2
    }
}
