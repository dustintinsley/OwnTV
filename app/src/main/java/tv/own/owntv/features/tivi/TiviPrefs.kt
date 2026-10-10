package tv.own.owntv.features.tivi

import android.content.Context
import androidx.core.content.edit

/** Preferences of the TV layout itself, kept on the device beside core's settings. */
object TiviPrefs {
    private const val PREFS = "tivi_layout"
    private const val GUIDE_OK_FULL_SCREEN = "guide_ok_full_screen"

    /** OK on a guide row watches the channel full screen (on by default, as a set-top box does). */
    fun guideOkFullScreen(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(GUIDE_OK_FULL_SCREEN, true)

    fun setGuideOkFullScreen(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(GUIDE_OK_FULL_SCREEN, on) }
    }
}
