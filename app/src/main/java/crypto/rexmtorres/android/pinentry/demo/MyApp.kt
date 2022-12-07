package crypto.rexmtorres.android.pinentry.demo

import android.app.Application
import crypto.rexmtorres.android.pinentry.demo.log.LogcatAppender

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LogcatAppender.register(this, BuildConfig.DEBUG)
    }
}
