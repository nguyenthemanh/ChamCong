package app.chamcong.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.chamcong.data.MonthKey
import app.chamcong.data.SheetLabels
import app.chamcong.ui.theme.AccentGreen
import app.chamcong.ui.theme.AccentRed
import app.chamcong.ui.theme.ChipIdle
import app.chamcong.ui.theme.SheetInk
import app.chamcong.ui.theme.SheetLine

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthTabs(
    months: List<MonthKey>,
    selected: MonthKey,
    sheet: SheetLabels,
    onSelect: (MonthKey) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    prevLabel: String = "Prev",
    nextLabel: String = "Next",
    deleteLabel: String = "Delete",
    deleteTitle: String = "Delete?",
    deleteBody: String = "%1\$s",
    deleteConfirm: String = "Delete",
    cancelLabel: String = "Cancel",
    onDeleteMonth: ((MonthKey) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<MonthKey?>(null) }

    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = prevLabel,
                tint = SheetInk,
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            months.forEach { month ->
                val active = month == selected
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) AccentGreen else ChipIdle)
                        .border(
                            width = if (active) 0.dp else 1.dp,
                            color = SheetLine,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .combinedClickable(
                            onClick = { onSelect(month) },
                            onLongClick = {
                                if (onDeleteMonth != null) pendingDelete = month
                            },
                        )
                        .padding(
                            start = 12.dp,
                            end = if (active && onDeleteMonth != null) 4.dp else 12.dp,
                            top = 6.dp,
                            bottom = 6.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = sheet.tabLabel(month),
                        color = if (active) Color.White else SheetInk,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    )
                    if (active && onDeleteMonth != null) {
                        IconButton(
                            onClick = { pendingDelete = month },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = deleteLabel,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = nextLabel,
                tint = SheetInk,
            )
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
