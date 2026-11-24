package app.chamcong.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.chamcong.data.AppLanguage
import app.chamcong.data.MonthSummary
import app.chamcong.data.PaySettings
import app.chamcong.data.SheetLabelFactory
import app.chamcong.data.SheetLabels
import app.chamcong.ui.i18n.Strings
import app.chamcong.ui.theme.SummaryBar

/** Compact in-app summary strip — PDF/JPG export stays numeric-only as before. */
@Composable
fun SummaryFooter(
    summary: MonthSummary,
    sheet: SheetLabels,
    settings: PaySettings,
    modifier: Modifier = Modifier,
) {
    val ui = Strings.of(settings.language)
    val currency = sheet.currencySuffix

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .background(SummaryBar)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryStat(
                caption = ui.summaryWorkDays,
                value = summary.workDaysText(sheet.workDaysSuffix.trim()),
            )
            if (settings.flagEnabled) {
                // Caption follows app UI language; custom text kept if user renamed it.
                val flagName = flagCaptionForAppLanguage(settings)
                SummaryStat(
                    caption = ui.summaryFlagDays.format(flagName),
                    value = summary.flagDays.toString(),
                )
            }
            if (settings.salaryEnabled) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = ui.payGross,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 9.sp,
                        maxLines = 1,
                    )
                    Text(
                        text = summary.grossYen.formatMoney(currency),
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = ui.payDeduction,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 9.sp,
                        maxLines = 1,
                    )
                    Text(
                        text = "−${summary.deductionYen.formatMoney(currency)}",
                        color = Color(0xFFFFE082),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = ui.payNet,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 9.sp,
                        maxLines = 1,
                    )
                    Text(
                        text = summary.netYen.formatMoney(currency),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
        if (settings.salaryEnabled) {
            Text(
                text = ui.payFormulaLine(
                    summary.grossYen,
                    summary.deductionYen,
                    summary.netYen,
                    currency,
                ),
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * If the stored label is still a built-in default from any region,
 * show the equivalent name in the app UI language. Custom names stay as typed.
 */
private fun flagCaptionForAppLanguage(settings: PaySettings): String {
    val raw = settings.flagLabelOff.trim()
        .ifBlank { settings.flagLabelOn.trim() }
    val uiSheet = SheetLabelFactory.of(settings.language)
    if (raw.isBlank()) return uiSheet.driving
    val isBuiltIn = AppLanguage.entries.any { lang ->
        val s = SheetLabelFactory.of(lang)
        raw.equals(s.driving, ignoreCase = true) ||
            raw.equals(s.drivingOn, ignoreCase = true) ||
            raw.equals(s.colDriving, ignoreCase = true)
    }
    return if (isBuiltIn) uiSheet.driving else raw
}

@Composable
private fun SummaryStat(caption: String, value: String) {
    Column(
        modifier = Modifier
            .widthIn(min = 56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(0.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = caption,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
