package uz.alesle.avto.shell

import android.annotation.SuppressLint
import android.content.AttributionSource
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * Helpers for the code Shizuku runs with adb (shell) rights, in its own app_process process
 * (ShellAudioService, ShellDisplayService).
 */
internal object ShellContexts {
    const val SHELL_PACKAGE = "com.android.shell"

    /** The system context of this (app_process) process. */
    @SuppressLint("PrivateApi")
    fun systemContext(): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) HiddenApiBypass.addHiddenApiExemptions("Landroid/app/ActivityThread;")
        val at = Class.forName("android.app.ActivityThread")
        val thread = at.getMethod("currentActivityThread").invoke(null) ?: at.getMethod("systemMain").invoke(null)
        return at.getMethod("getSystemContext").invoke(thread) as Context
    }
}

/** A context that says "the shell", so system services attribute calls to the uid running it. */
internal class ShellContext(base: Context) : ContextWrapper(base) {
    override fun getPackageName() = ShellContexts.SHELL_PACKAGE
    override fun getOpPackageName() = ShellContexts.SHELL_PACKAGE
    override fun getApplicationContext(): Context = this
    @RequiresApi(Build.VERSION_CODES.S)
    override fun getAttributionSource(): AttributionSource =
        AttributionSource.Builder(Process.myUid()).setPackageName(ShellContexts.SHELL_PACKAGE).build()
}
