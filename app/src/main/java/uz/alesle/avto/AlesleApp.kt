package uz.alesle.avto

import android.app.Application

class AlesleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        DiagLog.init(this)
    }

    companion object {
        lateinit var instance: AlesleApp
            private set
    }
}
