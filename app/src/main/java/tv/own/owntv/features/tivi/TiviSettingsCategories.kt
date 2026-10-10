package tv.own.owntv.features.tivi

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import tv.own.owntv.R
import tv.own.owntv.core.i18n.HorizontalDirection
import tv.own.owntv.core.i18n.horizontalDirection
import tv.own.owntv.features.shell.components.SettingsGroup
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.theme.StageColors
import tv.own.owntv.ui.theme.mpx
import tv.own.owntv.ui.theme.stageAccent
import tv.own.owntv.ui.theme.stageText

/**
 * Settings' category list, down the left of the page (TV layout): one plain row per group. Moving
 * through it shows that group's settings on the right without leaving the list; OK or ▶ goes into
 * them. Coming back with ◀ lands on the group being shown.
 */
@Composable
internal fun TiviSettingsCategories(
    selected: Int,
    onShowGroup: (Int) -> Unit,
    rowsFocus: FocusRequester,
    entryFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val scope = rememberCoroutineScope()
    val accent = stageAccent.accent
    val groups = SettingsGroup.entries
    val requesters = remember(groups.size) { List(groups.size) { FocusRequester() } }
    val enterRows: (Int) -> Unit = { index ->
        onShowGroup(index)
        scope.launch {
            withFrameNanos { }
            runCatching { rowsFocus.requestFocus() }
        }
    }
    Column(
        modifier
            .focusProperties { onEnter = { runCatching { requesters[selected.coerceIn(0, groups.size - 1)].requestFocus() } } }
            .focusGroup()
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key.horizontalDirection(layoutDirection) == HorizontalDirection.END) {
                    enterRows(selected); true
                } else {
                    false
                }
            },
    ) {
        Text(
            stringResource(R.string.common_nav_settings),
            style = stageText(40, 800),
            color = StageColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 20.mpx, top = 56.mpx, bottom = 28.mpx),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            groups.forEachIndexed { index, group ->
                val current = index == selected
                TiviRow(
                    height = 62.mpx,
                    onClick = { enterRows(index) },
                    onFocus = { if (index != selected) onShowGroup(index) },
                    modifier = Modifier
                        .focusRequester(requesters[index])
                        .then(if (current) Modifier.focusRequester(entryFocus) else Modifier),
                ) { focused ->
                    Box(
                        Modifier
                            .width(4.mpx)
                            .fillMaxHeight()
                            .padding(vertical = 16.mpx)
                            .background(if (current) accent else Color.Transparent, RoundedCornerShape(2.mpx)),
                    )
                    Spacer(Modifier.width(16.mpx))
                    OwnTVIcon(
                        group.tiviIcon,
                        if (current) accent else if (focused) Color.White else StageColors.Muted,
                        Modifier.size(26.mpx),
                    )
                    Spacer(Modifier.width(18.mpx))
                    Text(
                        stringResource(group.titleRes),
                        style = stageText(21, if (current) 700 else 500),
                        color = when {
                            focused -> Color.White
                            current -> accent
                            else -> StageColors.ItemText
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private val SettingsGroup.tiviIcon: OwnTVIcon
    get() = when (this) {
        SettingsGroup.QUICK -> OwnTVIcon.SPARKLE
        SettingsGroup.PROFILE -> OwnTVIcon.PERSON
        SettingsGroup.SOURCES -> OwnTVIcon.LIST
        SettingsGroup.APPEARANCE -> OwnTVIcon.PALETTE
        SettingsGroup.LAYOUT -> OwnTVIcon.GRID
        SettingsGroup.CONTENT -> OwnTVIcon.MOVIES
        SettingsGroup.PLAYER -> OwnTVIcon.PLAY_CIRCLE
        SettingsGroup.PICTURE -> OwnTVIcon.EXPAND
        SettingsGroup.SOUND -> OwnTVIcon.HEADPHONES
        SettingsGroup.LIVE -> OwnTVIcon.LIVE_TV
        SettingsGroup.WATCHING -> OwnTVIcon.REC
        SettingsGroup.APP -> OwnTVIcon.INFO
    }
