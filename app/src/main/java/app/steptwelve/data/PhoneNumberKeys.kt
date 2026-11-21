package app.steptwelve.data

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Turns phone numbers into comparable keys, so "+1 (555) 123-4567" in a contact and
 * "5551234567" in the call log meet. Prefers E.164 using the phone's country; falls
 * back to bare digits when a number can't be parsed (short codes, odd formats).
 */
class PhoneNumberKeys(context: Context) {

    private val countryIso: String = run {
        val tm = context.getSystemService(TelephonyManager::class.java)
        listOfNotNull(tm?.networkCountryIso, tm?.simCountryIso, Locale.getDefault().country)
            .firstOrNull { it.isNotBlank() }
            ?.uppercase(Locale.ROOT)
            ?: "US"
    }

    /**
     * Parsing is slow (libphonenumber) and the same few numbers fill most of a call log, so
     * results are cached per raw number. An empty string stands for "no key".
     */
    private val cache = ConcurrentHashMap<String, String>()

    /** [normalized] is a provider-supplied E.164 value (Phone.NORMALIZED_NUMBER, CACHED_NORMALIZED_NUMBER). */
    fun key(number: String?, normalized: String? = null): String? {
        if (!normalized.isNullOrBlank() && normalized.startsWith("+")) return normalized
        if (number.isNullOrBlank()) return null
        return cache.getOrPut(number) {
            PhoneNumberUtils.formatNumberToE164(number, countryIso)
                ?: PhoneNumberUtils.normalizeNumber(number)
        }.ifEmpty { null }
    }
}
