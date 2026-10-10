package tv.own.owntv.features.tivi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Text
import tv.own.owntv.R
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.epg.displayLogoUrl
import tv.own.owntv.core.live.EpgNowNext
import tv.own.owntv.features.live.LivePlate
import tv.own.owntv.features.live.ProviderTags
import tv.own.owntv.features.live.progressOf
import tv.own.owntv.ui.format.rememberSystemTimeFormatter
import tv.own.owntv.ui.theme.StageColors
import tv.own.owntv.ui.theme.gradientWash
import tv.own.owntv.ui.theme.mpx
import tv.own.owntv.ui.theme.stageAccent
import tv.own.owntv.ui.theme.stageText

/**
 * The channel banner across the bottom of the picture, shown for a few seconds after every channel
 * change: logo, number and name, the programme on now with its times and progress, what is on next,
 * and the clock. Informational only — it never takes focus.
 */
@Composable
fun TiviInfoBar(
    channel: ChannelEntity,
    nowNext: EpgNowNext?,
    showNumber: Boolean,
    modifier: Modifier = Modifier,
) {
    val formatTime = rememberSystemTimeFormatter()
    val name = remember(channel.name) { ProviderTags.parse(channel.name).name }
    val shade = Color(0xFF0B0D10)
    Box(
        modifier
            .fillMaxWidth()
            .gradientWash(
                vertical = true,
                0f to Color.Transparent,
                0.35f to shade.copy(alpha = 0.82f),
                1f to shade.copy(alpha = 0.96f),
            )
            .padding(start = 64.mpx, end = 64.mpx, top = 90.mpx, bottom = 48.mpx),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LivePlate(channel.displayLogoUrl, 168.mpx, 112.mpx)
            Spacer(Modifier.width(32.mpx))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.mpx)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.mpx)) {
                    if (showNumber) {
                        channel.number?.let {
                            Text(it.toString(), style = stageText(28, 800), color = stageAccent.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Text(name, style = stageText(28, 800), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val now = nowNext?.now
                if (now != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.mpx)) {
                        Text(
                            now.title,
                            style = stageText(24, 700),
                            color = StageColors.Text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(
                            stringResource(R.string.content_live_time_range_plain, formatTime(now.startMs), formatTime(now.stopMs)),
                            style = stageText(18, 500),
                            color = StageColors.Muted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TiviProgress(progressOf(now), Modifier.width(720.mpx).padding(vertical = 4.mpx))
                } else {
                    Text(stringResource(R.string.content_no_epg), style = stageText(20, 500), color = StageColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                nowNext?.next?.let { next ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.mpx)) {
                        Text(
                            stringResource(R.string.content_live_next_label, formatTime(next.startMs)),
                            style = stageText(18, 700),
                            color = StageColors.Muted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            next.title,
                            style = stageText(18, 500),
                            color = StageColors.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.width(32.mpx))
            TiviClock(size = 40)
        }
    }
}
