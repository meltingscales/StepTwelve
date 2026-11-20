package app.steptwelve

import android.app.Application

class StepTwelveApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.reminderScheduler.ensureChannel()
    }
}
