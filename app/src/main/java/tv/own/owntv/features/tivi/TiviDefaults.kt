package tv.own.owntv.features.tivi

import android.content.Context
import androidx.core.content.edit
import tv.own.owntv.core.settings.StartupMode

/**
 * One-time defaults for the TV layout that live outside core's settings store.
 *
 * Core cannot tell "never chosen" from "chose Home", so the live-first startup is written once per
 * profile and remembered here. After that the profile's own Settings choice always wins.
 */
object TiviDefaults {
    private const val PREFS = "tivi_layout"

    suspend fun applyLiveFirstOnce(context: Context, profileId: Long, setStartupMode: suspend (StartupMode) -> Unit) {
        if (profileId < 0) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "live_first_applied_$profileId"
        if (prefs.getBoolean(key, false)) return
        runCatching { setStartupMode(StartupMode.LAST_CHANNEL) }
            .onSuccess { prefs.edit { putBoolean(key, true) } }
    }
}
