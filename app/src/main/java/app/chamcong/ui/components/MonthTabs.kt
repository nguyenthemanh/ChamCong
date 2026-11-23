package app.chamcong.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.chamcong.data.MonthKey
import app.chamcong.ui.theme.AccentGreen
import app.chamcong.ui.theme.ChipIdle
import app.chamcong.ui.theme.SheetInk
import app.chamcong.ui.theme.SheetLine

@Composable
fun MonthTabs(
    months: List<MonthKey>,
    selected: MonthKey,
    onSelect: (MonthKey) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCreateMonth: () -> Unit,
    prevLabel: String = "Prev",
    nextLabel: String = "Next",
    newMonthLabel: String = "New month",
    modifier: Modifier = Modifier,
) {
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
                Text(
                    text = month.labelTab(),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) AccentGreen else ChipIdle)
                        .border(
                            width = if (active) 0.dp else 1.dp,
                            color = SheetLine,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { onSelect(month) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (active) Color.White else SheetInk,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                )
            }
            // Explicit "create new month" chip
            Text(
                text = "+ $newMonthLabel",
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ChipIdle)
                    .border(1.dp, AccentGreen, RoundedCornerShape(12.dp))
                    .clickable(onClick = onCreateMonth)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = AccentGreen,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        IconButton(onClick = onCreateMonth) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = newMonthLabel,
                tint = AccentGreen,
            )
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = nextLabel,
                tint = SheetInk,
            )
        }
    }
}
