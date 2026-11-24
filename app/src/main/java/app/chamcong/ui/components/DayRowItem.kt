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
import app.chamcong.data.MonthKey
import app.chamcong.data.PaySettings
import app.chamcong.data.SheetLabelFactory
import app.chamcong.data.SheetLabels
import app.chamcong.ui.theme.AccentGreen
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
    onChange: (site: String, hasFlag: Boolean, overtimeMinutes: Int) -> Unit,
    confirmLabel: String,
    cancelLabel: String,
    modifier: Modifier = Modifier,
    monthKey: MonthKey? = null,
) {
    var siteText by remember(monthKey, row.day, row.entry.site) { mutableStateOf(row.entry.site) }
    var focused by remember(monthKey, row.day) { mutableStateOf(false) }
    var showOtPicker by remember(monthKey, row.day) { mutableStateOf(false) }
    val otMinutes = row.entry.effectiveOvertimeMinutes

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
                val ot = if (it.isBlank()) 0 else otMinutes
                onChange(it, if (it.isBlank()) false else row.entry.hasFlag, ot)
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
        // Cột tăng ca giữa site và nhãn — nhấn mở bánh xe HH:MM
        if (settings.overtimeEnabled) {
            OtCell(
                minutes = otMinutes,
                enabled = siteText.isNotBlank(),
                onClick = { if (siteText.isNotBlank()) showOtPicker = true },
            )
        }
        if (settings.flagEnabled) {
            FlagChip(
                labelIdle = settings.flagLabelOff,
                labelActive = settings.flagLabelOn,
                enabled = siteText.isNotBlank(),
                active = row.entry.hasFlag && siteText.isNotBlank(),
                onClick = {
                    if (siteText.isNotBlank()) {
                        onChange(siteText, !row.entry.hasFlag, otMinutes)
                    }
                },
            )
        }
    }

    if (showOtPicker && settings.overtimeEnabled) {
        OvertimePickerDialog(
            initialMinutes = otMinutes,
            sheet = sheet,
            title = sheet.colOt,
            confirmLabel = confirmLabel,
            cancelLabel = cancelLabel,
            onConfirm = { mins ->
                showOtPicker = false
                onChange(siteText, row.entry.hasFlag, mins)
            },
            onDismiss = { showOtPicker = false },
        )
    }
}

@Composable
private fun OtCell(
    minutes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val hasOt = minutes > 0
    val bg by animateColorAsState(
        targetValue = when {
            !enabled -> ChipIdle.copy(alpha = 0.5f)
            hasOt -> AccentGreen.copy(alpha = 0.16f)
            else -> ChipIdle
        },
        label = "otBg",
    )
    Box(
        modifier = Modifier
            .width(54.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (hasOt) AccentGreen.copy(alpha = 0.4f) else SheetLine,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (hasOt) SheetLabelFactory.formatHhMm(minutes) else "—:—",
            color = when {
                !enabled -> SheetMuted.copy(alpha = 0.4f)
                hasOt -> AccentGreen
                else -> SheetMuted
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (hasOt) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
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
