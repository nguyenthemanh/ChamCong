package app.chamcong.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.chamcong.data.DayRow
import app.chamcong.data.PaySettings
import app.chamcong.data.SheetLabels
import app.chamcong.ui.theme.ChipActive
import app.chamcong.ui.theme.ChipIdle
import app.chamcong.ui.theme.SaturdayWash
import app.chamcong.ui.theme.SheetInk
import app.chamcong.ui.theme.SheetLine
import app.chamcong.ui.theme.SheetMuted
import app.chamcong.ui.theme.SundayWash

@Composable
fun DayRowItem(
    row: DayRow,
    sheet: SheetLabels,
    settings: PaySettings,
    onChange: (site: String, hasFlag: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var siteText by remember(row.day, row.entry.site) { mutableStateOf(row.entry.site) }
    var focused by remember { mutableStateOf(false) }

    val bg by animateColorAsState(
        targetValue = when {
            row.isSunday -> SundayWash
            row.isSaturday -> SaturdayWash
            else -> Color.Transparent
        },
        label = "rowBg",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
            .border(0.5.dp, SheetLine)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = row.day.toString(),
            modifier = Modifier.width(28.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = SheetInk,
        )
        Text(
            text = row.weekdayLabel,
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            color = if (row.isSunday) Color(0xFF1E5F8A) else SheetMuted,
            fontWeight = if (row.isSunday || row.isSaturday) FontWeight.Bold else FontWeight.Normal,
        )
        BasicTextField(
            value = siteText,
            onValueChange = {
                siteText = it
                onChange(it, row.entry.hasFlag)
            },
            singleLine = true,
            textStyle = TextStyle(
                color = SheetInk,
                fontSize = 15.sp,
                fontWeight = if (siteText.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
            ),
            cursorBrush = SolidColor(ChipActive),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (focused) Color.White.copy(alpha = 0.7f) else Color.Transparent)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (siteText.isEmpty() && !focused) {
                        Text(
                            text = sheet.sitePlaceholder,
                            color = SheetMuted.copy(alpha = 0.55f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    inner()
                }
            },
        )
        FlagChip(
            labelIdle = settings.flagLabelOff,
            labelActive = settings.flagLabelOn,
            enabled = siteText.isNotBlank(),
            active = row.entry.hasFlag && siteText.isNotBlank(),
            onClick = {
                if (siteText.isNotBlank()) {
                    onChange(siteText, !row.entry.hasFlag)
                }
            },
        )
    }
}

@Composable
private fun FlagChip(
    labelIdle: String,
    labelActive: String,
    enabled: Boolean,
    active: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (active) ChipActive else ChipIdle,
        label = "chipBg",
    )
    val fg = if (active) Color.White else SheetMuted.copy(alpha = if (enabled) 1f else 0.45f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (active) labelActive else labelIdle,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
