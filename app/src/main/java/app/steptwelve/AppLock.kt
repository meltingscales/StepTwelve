package app.steptwelve

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Tracks whether the app lock should be showing. Lock decisions use elapsed realtime. */
class AppLock {
    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked
    private var backgroundedAt: Long? = null
    private var everUnlocked = false

    fun onBackground() {
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    /** Called when the app comes to the foreground. */
    fun onForeground(lockEnabled: Boolean, lockAfterMinutes: Int) {
        if (!lockEnabled) {
            _locked.value = false
            return
        }
        val away = backgroundedAt?.let { SystemClock.elapsedRealtime() - it }
        if (!everUnlocked || (away != null && away >= lockAfterMinutes * 60_000L)) _locked.value = true
        backgroundedAt = null
    }

    fun unlock() {
        everUnlocked = true
        _locked.value = false
    }
}
