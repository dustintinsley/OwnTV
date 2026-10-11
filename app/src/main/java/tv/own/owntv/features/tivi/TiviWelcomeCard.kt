package tv.own.owntv.features.tivi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import tv.own.owntv.R
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.theme.mpx
import tv.own.owntv.ui.theme.stageAccent
import tv.own.owntv.ui.theme.stageText

/**
 * The first thing a fresh install shows (no profile, so no playlist yet): one quiet card on a plain
 * dark screen — "Welcome to" and the app's name, three lines on what it does, the note that it brings
 * no content of its own, and Continue. Continue goes on to the setup wizard.
 */
@Composable
internal fun TiviWelcomeCard(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val accent = stageAccent.accent
    val continueFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(60)
        runCatching { continueFocus.requestFocus() }
    }
    Box(modifier.fillMaxSize().background(WelcomeBackground), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .width(780.mpx)
                .clip(RoundedCornerShape(36.mpx))
                .background(WelcomeCard)
                .padding(horizontal = 26.mpx, vertical = 30.mpx),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.tivi_welcome_to),
                style = stageText(26, 700),
                color = WelcomeText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.app_name),
                style = stageText(96, 900),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.mpx, bottom = 30.mpx),
            )
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 10.mpx),
                verticalArrangement = Arrangement.spacedBy(26.mpx),
            ) {
                WelcomeLine(OwnTVIcon.PLAY_CIRCLE, stringResource(R.string.tivi_welcome_line_playlists))
                WelcomeLine(OwnTVIcon.EPG, stringResource(R.string.tivi_welcome_line_guide))
                WelcomeLine(OwnTVIcon.PHONE, stringResource(R.string.tivi_welcome_line_phone))
            }
            Text(
                stringResource(R.string.tivi_welcome_disclaimer),
                style = stageText(16, 500),
                color = WelcomeDim,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 40.mpx, bottom = 14.mpx, start = 20.mpx, end = 20.mpx),
            )
            WelcomeButton(
                text = stringResource(R.string.setup_continue),
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().focusRequester(continueFocus),
            )
        }
    }
}

@Composable
private fun WelcomeLine(icon: OwnTVIcon, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OwnTVIcon(icon, stageAccent.accent, Modifier.size(40.mpx))
        Spacer(Modifier.width(30.mpx))
        Text(text, style = stageText(22, 600), color = WelcomeText, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** A full-width pill: grey at rest, white with dark text and a slight lift when focused. */
@Composable
internal fun WelcomeButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .scale(if (focused) 1.03f else 1f)
            .height(76.mpx)
            .clip(RoundedCornerShape(38.mpx))
            .background(if (focused) Color.White else WelcomeButtonIdle)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = stageText(24, 600),
            color = if (focused) Color(0xFF1C1C1E) else WelcomeText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val WelcomeBackground = Color(0xFF1E1E1E)
private val WelcomeCard = Color(0xFF4A4A4C)
private val WelcomeButtonIdle = Color(0xFF5C5C5E)
private val WelcomeText = Color(0xFFF2F2F2)
private val WelcomeDim = Color(0xFFB0B0B2)
