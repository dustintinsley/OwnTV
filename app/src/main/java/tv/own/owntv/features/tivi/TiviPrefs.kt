package tv.own.owntv.features.tivi

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Preferences of the TV layout itself, kept on the device beside core's settings. */
object TiviPrefs {
    private const val PREFS = "tivi_layout"
    private const val GUIDE_OK_FULL_SCREEN = "guide_ok_full_screen"
    private const val OVERLAY_OPACITY = "overlay_opacity"

    /** Menus over the playing video: how much they cover it, in percent (Settings → Appearance). */
    const val OPACITY_MIN = 40
    const val OPACITY_MAX = 100
    const val OPACITY_STEP = 5
    private const val OPACITY_DEFAULT = 85

    /** OK on a guide row watches the channel full screen (on by default, as a set-top box does). */
    fun guideOkFullScreen(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(GUIDE_OK_FULL_SCREEN, true)

    fun setGuideOkFullScreen(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(GUIDE_OK_FULL_SCREEN, on) }
    }

    @Volatile private var opacity: MutableStateFlow<Int>? = null

    private fun opacityState(context: Context): MutableStateFlow<Int> = opacity ?: synchronized(this) {
        opacity ?: MutableStateFlow(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(OVERLAY_OPACITY, OPACITY_DEFAULT)
                .coerceIn(OPACITY_MIN, OPACITY_MAX),
        ).also { opacity = it }
    }

    fun overlayOpacityFlow(context: Context): StateFlow<Int> = opacityState(context).asStateFlow()

    fun setOverlayOpacity(context: Context, percent: Int) {
        val value = percent.coerceIn(OPACITY_MIN, OPACITY_MAX)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putInt(OVERLAY_OPACITY, value) }
        opacityState(context).value = value
    }
}
