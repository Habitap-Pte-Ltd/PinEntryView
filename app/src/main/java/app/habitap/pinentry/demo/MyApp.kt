package app.habitap.pinentry.demo

import android.app.Application
import app.habitap.pinentry.demo.log.LogcatAppender

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LogcatAppender.register(this, BuildConfig.DEBUG)
    }
}
