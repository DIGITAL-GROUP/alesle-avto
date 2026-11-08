package uz.alesle.avto

import android.util.Log
import uz.alesle.avto.aap.AapLog

/** Protocol logs go to logcat under "AlesleAap". */
object AndroidAapLog : AapLog {
    override fun d(msg: String) { Log.d("AlesleAap", msg) }
    override fun i(msg: String) { Log.i("AlesleAap", msg) }
    override fun w(msg: String, t: Throwable?) { Log.w("AlesleAap", msg, t) }
    override fun e(msg: String, t: Throwable?) { Log.e("AlesleAap", msg, t) }
}

/** Also writes to the [DiagLog] the user sees in the app (not the debug lines). */
object DiagAapLog : AapLog {
    override fun d(msg: String) { Log.d("AlesleAap", msg) }
    override fun i(msg: String) { DiagLog.add(msg) }
    override fun w(msg: String, t: Throwable?) { Log.w("AlesleAap", msg, t); DiagLog.add(if (t != null) "$msg: ${t.message ?: t}" else msg) }
    override fun e(msg: String, t: Throwable?) { Log.e("AlesleAap", msg, t); DiagLog.add(if (t != null) "$msg: ${t.message ?: t}" else msg) }
}
