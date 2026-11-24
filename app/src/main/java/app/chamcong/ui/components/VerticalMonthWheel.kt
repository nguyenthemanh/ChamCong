package app.chamcong.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import app.chamcong.ui.theme.SheetLine
import app.chamcong.ui.theme.SheetMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs

private val ItemHeight = 44.dp
private val WheelHeight = 176.dp

/**
 * Vertical month picker (fold / tablet).
 * Highlight theo [selected] (không theo center tạm) để không nháy khi xóa tháng.
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
    val sorted = remember(months) { months.distinct().sorted() }
    if (sorted.isEmpty()) return

    var pendingDelete by remember { mutableStateOf<MonthKey?>(null) }

    MonthSnapList(
        sorted = sorted,
        selected = selected,
        sheet = sheet,
        onSelect = onSelect,
        deleteLabel = deleteLabel,
        onDeleteClick = if (onDeleteMonth != null) {
            { month -> pendingDelete = month }
        } else {
            null
        },
        modifier = modifier,
    )

    val toDelete = pendingDelete
    if (toDelete != null && onDeleteMonth != null) {
        DeleteMonthDialog(
            month = toDelete,
            sheet = sheet,
            title = deleteTitle,
            body = deleteBody,
            confirm = deleteConfirm,
            cancel = cancelLabel,
            onConfirm = {
                pendingDelete = null
                onDeleteMonth(toDelete)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthSnapList(
    sorted: List<MonthKey>,
    selected: MonthKey,
    sheet: SheetLabels,
    onSelect: (MonthKey) -> Unit,
    deleteLabel: String,
    onDeleteClick: ((MonthKey) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val safeSelected = if (sorted.contains(selected)) selected else sorted.last()
    val initial = sorted.indexOf(safeSelected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initial)
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()
    var suppressSelect by remember { mutableStateOf(false) }
    val structureKey = sorted.joinToString("|") { "${it.year}-${it.month}" }

    LaunchedEffect(safeSelected, structureKey) {
        suppressSelect = true
        val target = sorted.indexOf(safeSelected)
        if (target >= 0) {
            val center = centeredIndex(listState)
            if (center != target) {
                listState.scrollToItem(target)
            }
        }
        // Chờ layout ổn sau khi item bị gỡ — tránh onSelect loạn
        delay(120)
        suppressSelect = false
    }

    LaunchedEffect(listState, structureKey) {
        snapshotFlow { centeredIndex(listState) }
            .filter { it != null }
            .distinctUntilChanged()
            .collect { idx ->
                if (suppressSelect || idx == null) return@collect
                val m = sorted.getOrNull(idx) ?: return@collect
                if (m != safeSelected) onSelect(m)
            }
    }

    val centerIdx by remember {
        derivedStateOf { centeredIndex(listState) }
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
            itemsIndexed(
                items = sorted,
                key = { _, m -> "${m.year}-${m.month}" },
            ) { index, month ->
                val dist = centerIdx?.let { abs(index - it) }
                    ?: abs(index - initial)
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
                // Highlight theo selected thật — không nhảy theo center tạm khi xóa
                val active = month == safeSelected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ItemHeight)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .clickable {
                            onSelect(month)
                            scope.launch {
                                suppressSelect = true
                                listState.animateScrollToItem(index)
                                delay(80)
                                suppressSelect = false
                            }
                        }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = sheet.tabLabel(month),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = if (active) AccentGreen else SheetMuted,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (active) 16.sp else 14.sp,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (onDeleteClick != null) {
                        IconButton(
                            onClick = { onDeleteClick(month) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = deleteLabel,
                                tint = if (active) AccentRed else AccentRed.copy(alpha = 0.35f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else {
                        Box(Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

private fun centeredIndex(listState: androidx.compose.foundation.lazy.LazyListState): Int? {
    val info = listState.layoutInfo
    if (info.visibleItemsInfo.isEmpty()) return null
    val viewportCenter = info.viewportStartOffset +
        (info.viewportEndOffset - info.viewportStartOffset) / 2
    return info.visibleItemsInfo.minByOrNull { item ->
        abs((item.offset + item.size / 2) - viewportCenter)
    }?.index
}
