package app.chamcong.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.chamcong.data.MonthKey
import app.chamcong.data.SheetLabels
import app.chamcong.ui.theme.AccentGreen
import app.chamcong.ui.theme.AccentRed
import app.chamcong.ui.theme.ChipIdle
import app.chamcong.ui.theme.SheetInk
import app.chamcong.ui.theme.SheetLine
import app.chamcong.ui.theme.SheetMuted
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val WheelCycles = 40
private val ItemHeight = 44.dp
private val WheelHeight = 176.dp // ~4 items visible

/**
 * Vertical looping month picker for fold-open / tablet side rail.
 * Scroll snaps to center; long-press deletes (optional).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalMonthWheel(
    months: List<MonthKey>,
    selected: MonthKey,
    sheet: SheetLabels,
    onSelect: (MonthKey) -> Unit,
    deleteLabel: String = "Delete",
    deleteTitle: String = "Delete?",
    deleteBody: String = "%1\$s",
    deleteConfirm: String = "Delete",
    cancelLabel: String = "Cancel",
    onDeleteMonth: ((MonthKey) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (months.isEmpty()) return

    val sorted = remember(months) { months.sortedDescending() }
    val base = sorted.size
    val total = base * WheelCycles
    val midCycle = WheelCycles / 2

    fun indexOfMonth(m: MonthKey): Int {
        val i = sorted.indexOf(m).coerceAtLeast(0)
        return midCycle * base + i
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = indexOfMonth(selected))
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<MonthKey?>(null) }
    var suppressSelect by remember { mutableStateOf(false) }

    // Keep wheel centered on [selected] when it changes from outside (create/delete/arrows).
    LaunchedEffect(selected, sorted) {
        val target = indexOfMonth(selected)
        val visible = listState.firstVisibleItemIndex
        val centerApprox = visible + 1
        if (sorted.getOrNull(centerApprox % base) != selected) {
            suppressSelect = true
            listState.scrollToItem(target)
            suppressSelect = false
        }
    }

    // When user snaps, notify selection.
    LaunchedEffect(listState, sorted) {
        snapshotFlow {
            val info = listState.layoutInfo
            val viewportCenter = info.viewportStartOffset +
                (info.viewportEndOffset - info.viewportStartOffset) / 2
            info.visibleItemsInfo.minByOrNull { item ->
                abs((item.offset + item.size / 2) - viewportCenter)
            }?.index
        }
            .filter { it != null }
            .distinctUntilChanged()
            .collect { idx ->
                if (suppressSelect || idx == null) return@collect
                val m = sorted[idx % base]
                if (m != selected) onSelect(m)
            }
    }

    val centeredIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val viewportCenter = info.viewportStartOffset +
                (info.viewportEndOffset - info.viewportStartOffset) / 2
            info.visibleItemsInfo.minByOrNull { item ->
                abs((item.offset + item.size / 2) - viewportCenter)
            }?.index
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(WheelHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(ChipIdle)
            .border(1.dp, SheetLine, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // Center highlight band
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ItemHeight)
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AccentGreen.copy(alpha = 0.14f))
                .border(1.dp, AccentGreen.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
        )

        LazyColumn(
            state = listState,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = (WheelHeight - ItemHeight) / 2),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(count = total, key = { it }) { index ->
                val month = sorted[index % base]
                val dist = if (centeredIndex != null) abs(index - centeredIndex!!) else 0
                val scale = when (dist) {
                    0 -> 1f
                    1 -> 0.92f
                    else -> 0.84f
                }
                val alpha = when (dist) {
                    0 -> 1f
                    1 -> 0.55f
                    else -> 0.32f
                }
                val active = dist == 0
                Text(
                    text = sheet.tabLabel(month),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ItemHeight)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .combinedClickable(
                            onClick = {
                                scope.launch {
                                    listState.animateScrollToItem(index)
                                }
                                onSelect(month)
                            },
                            onLongClick = {
                                if (onDeleteMonth != null) pendingDelete = month
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    color = if (active) AccentGreen else SheetMuted,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    fontSize = if (active) 16.sp else 14.sp,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }

    val toDelete = pendingDelete
    if (toDelete != null && onDeleteMonth != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(deleteTitle, fontWeight = FontWeight.Bold) },
            text = { Text(deleteBody.format(sheet.monthLabel(toDelete))) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMonth(toDelete)
                        pendingDelete = null
                    },
                ) {
                    Text(deleteConfirm, color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(cancelLabel)
                }
            },
        )
    }
}
