package app.chamcong.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.chamcong.data.SheetLabelFactory
import app.chamcong.data.SheetLabels
import app.chamcong.ui.theme.AccentGreen
import app.chamcong.ui.theme.ChipIdle
import app.chamcong.ui.theme.SheetInk
import app.chamcong.ui.theme.SheetLine
import app.chamcong.ui.theme.SheetMuted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs

private val WheelItemH = 40.dp
private val WheelH = 160.dp
private const val MaxHours = 12

@Composable
fun OvertimePickerDialog(
    initialMinutes: Int,
    sheet: SheetLabels,
    title: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: (minutes: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val start = initialMinutes.coerceIn(0, MaxHours * 60)
    var hours by remember { mutableIntStateOf(start / 60) }
    var minutes by remember { mutableIntStateOf(start % 60) }
    val previewWords = remember(hours, minutes, sheet) {
        sheet.formatDuration(hours * 60 + minutes)
    }
    val previewHhMm = SheetLabelFactory.formatHhMm(hours * 60 + minutes)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.SemiBold, color = SheetInk)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = previewHhMm,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen,
                )
                if (previewWords.isNotBlank()) {
                    Text(
                        text = previewWords,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SheetMuted,
                        textAlign = TextAlign.Center,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ChipIdle)
                        .border(1.dp, SheetLine, RoundedCornerShape(14.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NumberWheel(
                        value = hours,
                        range = 0..MaxHours,
                        onChange = { hours = it },
                        modifier = Modifier.width(72.dp),
                    )
                    Text(
                        text = ":",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = SheetInk,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    NumberWheel(
                        value = minutes,
                        range = 0..59,
                        onChange = { minutes = it },
                        modifier = Modifier.width(72.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hours * 60 + minutes) }) {
                Text(confirmLabel, color = AccentGreen, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel, color = SheetMuted)
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberWheel(
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val values = remember(range) { range.toList() }
    val initial = values.indexOf(value).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initial)
    val fling = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(value, values) {
        val idx = values.indexOf(value)
        if (idx >= 0 && listState.firstVisibleItemIndex != idx) {
            listState.scrollToItem(idx)
        }
    }

    LaunchedEffect(listState, values) {
        snapshotFlow {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@snapshotFlow null
            val mid = info.viewportStartOffset +
                (info.viewportEndOffset - info.viewportStartOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - mid) }?.index
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { idx ->
                values.getOrNull(idx)?.let { if (it != value) onChange(it) }
            }
    }

    val centerIdx by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@derivedStateOf initial
            val mid = info.viewportStartOffset +
                (info.viewportEndOffset - info.viewportStartOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - mid) }?.index
                ?: initial
        }
    }

    Box(
        modifier = modifier.height(WheelH),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WheelItemH)
                .clip(RoundedCornerShape(8.dp))
                .background(AccentGreen.copy(alpha = 0.12f))
                .border(1.dp, AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        )
        LazyColumn(
            state = listState,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = (WheelH - WheelItemH) / 2),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(values.size) { index ->
                val n = values[index]
                val active = index == centerIdx
                Text(
                    text = "%02d".format(n),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WheelItemH)
                        .padding(vertical = 6.dp),
                    textAlign = TextAlign.Center,
                    fontSize = if (active) 22.sp else 16.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) AccentGreen else SheetMuted,
                )
            }
        }
    }
}
