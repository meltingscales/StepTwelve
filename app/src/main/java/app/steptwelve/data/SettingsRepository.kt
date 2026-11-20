package app.steptwelve.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

enum class SortOrder { NEGLECTED, NAME, RECENT }

@Serializable
data class Settings(
    val dailyGoal: Int = 3,
    val reminderEnabled: Boolean = false,
    /** Minutes after midnight, local time. */
    val reminderMinuteOfDay: Int = 19 * 60,
    val postCallPrompt: Boolean = true,
    val appLock: Boolean = false,
    val lockAfterMinutes: Int = 1,
    val screenPrivacy: Boolean = false,
    val discreetIcon: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NEGLECTED,
    /** Also look for tags in each contact's note, company and job title. */
    val matchExtraFields: Boolean = false,
    val onboardingDone: Boolean = false,
)

// App-private internal storage; excluded from backups by the manifest rules.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.dataStore

    private object Keys {
        val dailyGoal = intPreferencesKey("daily_goal")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderMinuteOfDay = intPreferencesKey("reminder_minute_of_day")
        val postCallPrompt = booleanPreferencesKey("post_call_prompt")
        val appLock = booleanPreferencesKey("app_lock")
        val lockAfterMinutes = intPreferencesKey("lock_after_minutes")
        val screenPrivacy = booleanPreferencesKey("screen_privacy")
        val discreetIcon = booleanPreferencesKey("discreet_icon")
        val sortOrder = intPreferencesKey("sort_order")
        val matchExtraFields = booleanPreferencesKey("match_extra_fields")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
    }

    private fun Preferences.toSettings(): Settings {
        val d = Settings()
        return Settings(
            dailyGoal = this[Keys.dailyGoal] ?: d.dailyGoal,
            reminderEnabled = this[Keys.reminderEnabled] ?: d.reminderEnabled,
            reminderMinuteOfDay = this[Keys.reminderMinuteOfDay] ?: d.reminderMinuteOfDay,
            postCallPrompt = this[Keys.postCallPrompt] ?: d.postCallPrompt,
            appLock = this[Keys.appLock] ?: d.appLock,
            lockAfterMinutes = this[Keys.lockAfterMinutes] ?: d.lockAfterMinutes,
            screenPrivacy = this[Keys.screenPrivacy] ?: d.screenPrivacy,
            discreetIcon = this[Keys.discreetIcon] ?: d.discreetIcon,
            sortOrder = SortOrder.entries.getOrNull(this[Keys.sortOrder] ?: -1) ?: d.sortOrder,
            matchExtraFields = this[Keys.matchExtraFields] ?: d.matchExtraFields,
            onboardingDone = this[Keys.onboardingDone] ?: d.onboardingDone,
        )
    }

    val settings: Flow<Settings> = store.data.map { it.toSettings() }

    suspend fun current(): Settings = settings.first()

    suspend fun update(transform: (Settings) -> Settings) {
        store.edit { p ->
            val old = p.toSettings()
            val new = transform(old)
            p[Keys.dailyGoal] = new.dailyGoal.coerceIn(1, 50)
            p[Keys.reminderEnabled] = new.reminderEnabled
            p[Keys.reminderMinuteOfDay] = new.reminderMinuteOfDay.coerceIn(0, 24 * 60 - 1)
            p[Keys.postCallPrompt] = new.postCallPrompt
            p[Keys.appLock] = new.appLock
            p[Keys.lockAfterMinutes] = new.lockAfterMinutes.coerceIn(0, 60)
            p[Keys.screenPrivacy] = new.screenPrivacy
            p[Keys.discreetIcon] = new.discreetIcon
            p[Keys.sortOrder] = new.sortOrder.ordinal
            p[Keys.matchExtraFields] = new.matchExtraFields
            p[Keys.onboardingDone] = new.onboardingDone
        }
    }
}
