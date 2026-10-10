package tv.own.owntv.features.tivi

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import tv.own.owntv.R
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.epg.displayLogoUrl
import tv.own.owntv.core.i18n.HorizontalDirection
import tv.own.owntv.core.i18n.horizontalDirection
import tv.own.owntv.core.live.LiveKey
import tv.own.owntv.core.nav.MainSection
import tv.own.owntv.core.parser.XtEpgEntry
import tv.own.owntv.features.live.LivePlate
import tv.own.owntv.features.live.LiveRailItem
import tv.own.owntv.features.live.ProviderTags
import tv.own.owntv.features.live.liveCategoryEntries
import tv.own.owntv.features.live.progressOf
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.components.trapAllFocusExit
import tv.own.owntv.ui.format.rememberSystemTimeFormatter
import tv.own.owntv.ui.theme.StageColors
import tv.own.owntv.ui.theme.gradientWash
import tv.own.owntv.ui.theme.mpx
import tv.own.owntv.ui.theme.stageAccent
import tv.own.owntv.ui.theme.stageText

/** Which column of the TV overlay holds focus. Columns further left only show while focus is in them. */
private enum class TiviColumn { MENU, GROUPS, CHANNELS }

/**
 * The live-first TV overlay, drawn over the playing channel: the channel list of the current group,
 * with the groups one press of ◀ away and the main menu one more press beyond them.
 *
 * - Channels: OK plays the channel and closes the overlay, long OK toggles the favourite, ◀ opens the
 *   groups, Back closes. The focused channel's programme is described at the bottom right.
 * - Groups: OK (or ▶) shows that group's channels, ◀ opens the menu, Back returns to the channels.
 * - Menu: OK opens a section (or exits), ▶ or Back returns to the groups.
 *
 * Picking a group re-arms the zap list, so CH+/CH− and ▲/▼ afterwards step through that group, as
 * they do on a set-top box.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TiviChannelOverlay(
    channels: List<ChannelEntity>,
    playingId: Long?,
    groupTitle: String,
    railItems: List<LiveRailItem>,
    railCounts: Map<LiveKey, Int>,
    currentGroupKey: LiveKey?,
    nowProgrammes: Map<Long, XtEpgEntry>,
    nowPlaying: Map<Long, String>,
    favoriteIds: Set<Long>,
    showNumbers: Boolean,
    menuSections: List<MainSection>,
    onPlay: (ChannelEntity) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    onPickGroup: (LiveKey) -> Unit,
    onOpenSection: (MainSection) -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val accent = stageAccent.accent
    var column by remember { mutableStateOf(TiviColumn.CHANNELS) }

    val (groups, _) = liveCategoryEntries(railItems, railCounts)
    val groupIndex = remember(groups, currentGroupKey) {
        groups.indexOfFirst { it.item.key == currentGroupKey }.coerceAtLeast(0)
    }
    val playingIndex = remember(channels, playingId) {
        channels.indexOfFirst { it.id == playingId }.coerceAtLeast(0)
    }
    // The row the channel column lands on when it takes focus: the playing channel at first, then
    // whichever row was left when focus moved out to the groups.
    var channelTarget by remember(channels) { mutableIntStateOf(playingIndex) }
    var focusedChannel by remember(channels) { mutableIntStateOf(playingIndex) }

    val menuFocus = remember { FocusRequester() }
    val groupFocus = remember { FocusRequester() }
    val channelFocus = remember { FocusRequester() }
    val channelState = rememberLazyListState()
    val groupState = rememberLazyListState()

    LaunchedEffect(column, channels) {
        when (column) {
            TiviColumn.CHANNELS -> {
                if (channels.isEmpty()) {
                    // A group with nothing in it (or still loading): give it a moment, then hand focus
                    // back to the groups rather than leaving nothing focused.
                    delay(1_500)
                    column = TiviColumn.GROUPS
                } else {
                    runCatching { channelState.scrollToItem((channelTarget - 3).coerceAtLeast(0)) }
                    withFrameNanos { }
                    runCatching { channelFocus.requestFocus() }
                }
            }
            TiviColumn.GROUPS -> {
                runCatching { groupState.scrollToItem((groupIndex - 3).coerceAtLeast(0)) }
                withFrameNanos { }
                runCatching { groupFocus.requestFocus() }
            }
            TiviColumn.MENU -> {
                withFrameNanos { }
                runCatching { menuFocus.requestFocus() }
            }
        }
    }

    BackHandler {
        when (column) {
            TiviColumn.CHANNELS -> onDismiss()
            TiviColumn.GROUPS -> column = TiviColumn.CHANNELS
            TiviColumn.MENU -> column = TiviColumn.GROUPS
        }
    }

    val shade = Color(0xFF0B0D10)
    Box(
        modifier = modifier
            .fillMaxSize()
            // The HUD's full-screen catch target sits underneath; never let ▲/▼ past the ends of a
            // list hand focus to it.
            .trapAllFocusExit()
            .focusGroup()
            .gradientWash(
                vertical = false,
                0f to shade.copy(alpha = 0.96f),
                0.45f to shade.copy(alpha = 0.90f),
                0.62f to shade.copy(alpha = 0.55f),
                0.8f to Color.Transparent,
            ),
    ) {
        Row(Modifier.fillMaxHeight().padding(vertical = 36.mpx)) {
            if (column == TiviColumn.MENU) {
                TiviMenuColumn(
                    sections = menuSections,
                    focusRequester = menuFocus,
                    onOpenSection = onOpenSection,
                    onExit = onExit,
                    modifier = Modifier
                        .width(300.mpx)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.04f))
                        .onPreviewKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (e.key.horizontalDirection(layoutDirection)) {
                                HorizontalDirection.END -> { column = TiviColumn.GROUPS; true }
                                HorizontalDirection.START -> true
                                else -> false
                            }
                        },
                )
            }
            if (column != TiviColumn.CHANNELS) {
                Column(
                    Modifier
                        .width(400.mpx)
                        .fillMaxHeight()
                        .onPreviewKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (e.key.horizontalDirection(layoutDirection)) {
                                HorizontalDirection.START -> { column = TiviColumn.MENU; true }
                                HorizontalDirection.END -> { column = TiviColumn.CHANNELS; true }
                                else -> false
                            }
                        },
                ) {
                    TiviHeader(stringResource(R.string.content_live_groups), null)
                    LazyColumn(
                        state = groupState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.mpx, vertical = 4.mpx),
                    ) {
                        itemsIndexed(groups, key = { _, g -> g.item.key.toString() }) { index, group ->
                            val current = index == groupIndex
                            TiviRow(
                                height = 60.mpx,
                                onClick = {
                                    onPickGroup(group.item.key)
                                    column = TiviColumn.CHANNELS
                                },
                                modifier = if (current) Modifier.focusRequester(groupFocus) else Modifier,
                            ) { focused ->
                                group.icon?.let {
                                    OwnTVIcon(it, if (current) accent else StageColors.Muted, Modifier.size(24.mpx))
                                    Spacer(Modifier.width(14.mpx))
                                }
                                Text(
                                    group.label,
                                    style = stageText(20, if (current) 700 else 500),
                                    color = when {
                                        current -> accent
                                        focused -> Color.White
                                        else -> StageColors.ItemText
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                group.count?.let {
                                    Text(it.toString(), style = stageText(16, 500), color = StageColors.Dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
            Column(
                Modifier
                    .width(if (column == TiviColumn.CHANNELS) 860.mpx else 760.mpx)
                    .fillMaxHeight()
                    .onPreviewKeyEvent { e ->
                        if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        if (column != TiviColumn.CHANNELS) return@onPreviewKeyEvent false
                        when (e.key.horizontalDirection(layoutDirection)) {
                            HorizontalDirection.START -> {
                                channelTarget = focusedChannel
                                column = TiviColumn.GROUPS
                                true
                            }
                            // Nothing lives to the right of the list; keep focus from wandering off it.
                            HorizontalDirection.END -> true
                            else -> false
                        }
                    },
            ) {
                TiviHeader(groupTitle, channels.size.toString())
                if (channels.isEmpty()) {
                    Text(
                        stringResource(R.string.content_no_channels_here),
                        style = stageText(20, 500),
                        color = StageColors.Muted,
                        modifier = Modifier.padding(horizontal = 36.mpx, vertical = 24.mpx),
                    )
                }
                LazyColumn(
                    state = channelState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.mpx, vertical = 4.mpx),
                ) {
                    itemsIndexed(channels, key = { _, ch -> ch.id }) { index, ch ->
                        TiviChannelRow(
                            channel = ch,
                            playing = ch.id == playingId,
                            now = nowProgrammes[ch.id],
                            nowTitle = nowPlaying[ch.id],
                            favorite = ch.id in favoriteIds,
                            showNumber = showNumbers,
                            dimmed = column != TiviColumn.CHANNELS,
                            onFocus = { focusedChannel = index },
                            onClick = { onPlay(ch) },
                            onLongClick = { onToggleFavorite(ch) },
                            modifier = if (index == channelTarget) Modifier.focusRequester(channelFocus) else Modifier,
                        )
                    }
                }
            }
        }

        // The focused channel's programme, over the picture at the bottom right.
        if (column == TiviColumn.CHANNELS) {
            val focused = channels.getOrNull(focusedChannel)
            val prog = focused?.let { nowProgrammes[it.id] }
            if (focused != null && prog != null) {
                TiviProgrammePane(
                    channel = focused,
                    programme = prog,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 56.mpx, bottom = 56.mpx)
                        .width(820.mpx),
                )
            }
        }
        TiviClock(Modifier.align(Alignment.TopEnd).padding(top = 40.mpx, end = 56.mpx))
    }
}

@Composable
private fun TiviHeader(title: String, trailing: String?) {
    Row(
        Modifier.fillMaxWidth().padding(start = 32.mpx, end = 32.mpx, bottom = 14.mpx),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.mpx),
    ) {
        Text(
            title,
            style = stageText(26, 800),
            color = StageColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        trailing?.let { Text(it, style = stageText(18, 500), color = StageColors.Dim, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

/** A flat, dense list row: a soft light plate when focused, nothing at rest. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TiviRow(
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onFocus: () -> Unit = {},
    content: @Composable androidx.compose.foundation.layout.RowScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(10.mpx))
            .background(if (focused) Color.White.copy(alpha = 0.16f) else Color.Transparent)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(horizontal = 20.mpx),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content(focused)
    }
}

@Composable
private fun TiviChannelRow(
    channel: ChannelEntity,
    playing: Boolean,
    now: XtEpgEntry?,
    nowTitle: String?,
    favorite: Boolean,
    showNumber: Boolean,
    dimmed: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = stageAccent.accent
    val name = remember(channel.name) { ProviderTags.parse(channel.name).name }
    TiviRow(
        height = 76.mpx,
        onClick = onClick,
        onLongClick = onLongClick,
        onFocus = onFocus,
        modifier = modifier,
    ) { focused ->
        val nameColor = when {
            playing -> accent
            focused -> Color.White
            dimmed -> StageColors.Muted
            else -> StageColors.Text
        }
        if (showNumber) {
            Text(
                channel.number?.toString().orEmpty(),
                style = stageText(20, 600),
                color = if (playing) accent else StageColors.Dim,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(64.mpx),
            )
        }
        LivePlate(channel.displayLogoUrl, 76.mpx, 52.mpx)
        Spacer(Modifier.width(18.mpx))
        Column(Modifier.weight(1f)) {
            Text(name, style = stageText(21, 700), color = nameColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val title = now?.title ?: nowTitle
            if (title != null) {
                Text(
                    title,
                    style = stageText(17, 400),
                    color = if (focused) Color(0xFFD9E2E6) else StageColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (now != null) {
                Spacer(Modifier.height(6.mpx))
                TiviProgress(progressOf(now), Modifier.width(260.mpx))
            }
        }
        if (channel.catchup) {
            OwnTVIcon(OwnTVIcon.REWIND, StageColors.Dim, Modifier.padding(start = 10.mpx).size(20.mpx))
        }
        if (favorite) {
            OwnTVIcon(OwnTVIcon.FAVORITE, accent, Modifier.padding(start = 10.mpx).size(20.mpx), filled = true)
        }
    }
}

@Composable
internal fun TiviProgress(fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier.height(4.mpx).clip(RoundedCornerShape(2.mpx)).background(Color.White.copy(alpha = 0.18f))) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(stageAccent.accent),
        )
    }
}

@Composable
private fun TiviProgrammePane(channel: ChannelEntity, programme: XtEpgEntry, modifier: Modifier = Modifier) {
    val formatTime = rememberSystemTimeFormatter()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.mpx)) {
        Text(
            ProviderTags.parse(channel.name).name,
            style = stageText(18, 600),
            color = StageColors.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            programme.title,
            style = stageText(34, 800),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            stringResource(R.string.content_live_time_range_plain, formatTime(programme.startMs), formatTime(programme.stopMs)),
            style = stageText(18, 500),
            color = StageColors.Muted,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        TiviProgress(progressOf(programme), Modifier.width(420.mpx))
        programme.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = stageText(18, 400), color = StageColors.ItemText, maxLines = 5, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TiviMenuColumn(
    sections: List<MainSection>,
    focusRequester: FocusRequester,
    onOpenSection: (MainSection) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(top = 60.mpx, start = 12.mpx, end = 12.mpx)) {
        sections.forEachIndexed { index, section ->
            TiviRow(
                height = 64.mpx,
                onClick = { onOpenSection(section) },
                modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
            ) { focused ->
                OwnTVIcon(section.tiviIcon, if (focused) Color.White else StageColors.Muted, Modifier.size(26.mpx))
                Spacer(Modifier.width(18.mpx))
                Text(
                    stringResource(section.labelRes),
                    style = stageText(21, 600),
                    color = if (focused) Color.White else StageColors.ItemText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        TiviRow(
            height = 64.mpx,
            onClick = onExit,
            modifier = if (sections.isEmpty()) Modifier.focusRequester(focusRequester) else Modifier,
        ) { focused ->
            OwnTVIcon(OwnTVIcon.POWER, if (focused) Color.White else StageColors.Muted, Modifier.size(26.mpx))
            Spacer(Modifier.width(18.mpx))
            Text(
                stringResource(R.string.common_exit),
                style = stageText(21, 600),
                color = if (focused) Color.White else StageColors.ItemText,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The wall clock, top right, refreshed often enough that the minute turns over on time. */
@Composable
internal fun TiviClock(modifier: Modifier = Modifier, size: Int = 30) {
    val formatTime = rememberSystemTimeFormatter()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(5_000)
        }
    }
    Text(formatTime(now), style = stageText(size, 700), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

private val MainSection.tiviIcon: OwnTVIcon
    get() = when (this) {
        MainSection.SEARCH -> OwnTVIcon.SEARCH
        MainSection.HOME -> OwnTVIcon.HOME
        MainSection.LIVE_TV -> OwnTVIcon.LIVE_TV
        MainSection.MOVIES -> OwnTVIcon.MOVIES
        MainSection.SERIES -> OwnTVIcon.SERIES
        MainSection.DOWNLOADS -> OwnTVIcon.DOWNLOADS
        MainSection.EPG -> OwnTVIcon.EPG
        MainSection.SETTINGS -> OwnTVIcon.SETTINGS
        MainSection.MORE -> OwnTVIcon.MORE
    }
