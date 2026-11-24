package app.chamcong.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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

enum class SummaryVariant {
    /** Phone cover / bottom bar — horizontal chips. */
    Bar,
    /** Fold inner left rail — stacked rows, no empty stretch. */
    Panel,
}

/** In-app summary only — PDF/JPG export stays numeric-only as before. */
@Composable
fun SummaryFooter(
    summary: MonthSummary,
    sheet: SheetLabels,
    settings: PaySettings,
    modifier: Modifier = Modifier,
    variant: SummaryVariant = SummaryVariant.Bar,
) {
    when (variant) {
        SummaryVariant.Bar -> SummaryBarLayout(summary, sheet, settings, modifier)
        SummaryVariant.Panel -> SummaryPanelLayout(summary, sheet, settings, modifier)
    }
}

@Composable
private fun SummaryBarLayout(
    summary: MonthSummary,
    sheet: SheetLabels,
    settings: PaySettings,
    modifier: Modifier,
) {
    val ui = Strings.of(settings.language)
    val currency = sheet.currencySuffix
    val flagName = flagCaptionForAppLanguage(settings)

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
            SummaryChip(ui.summaryWorkDays, summary.workDaysText(sheet.workDaysSuffix.trim()))
            if (settings.flagEnabled) {
                SummaryChip(ui.summaryFlagDays.format(flagName), summary.flagDays.toString())
            }
            if (settings.salaryEnabled) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MoneyTiny(ui.payGross, summary.grossYen.formatMoney(currency))
                    MoneyTiny(ui.payDeduction, "−${summary.deductionYen.formatMoney(currency)}", Color(0xFFFFE082))
                    MoneyTiny(ui.payNet, summary.netYen.formatMoney(currency), bold = true)
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

/** Narrow side panel: every row filled — no empty green hole, no overlap. */
@Composable
private fun SummaryPanelLayout(
    summary: MonthSummary,
    sheet: SheetLabels,
    settings: PaySettings,
    modifier: Modifier,
) {
    val ui = Strings.of(settings.language)
    val currency = sheet.currencySuffix
    val flagName = flagCaptionForAppLanguage(settings)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SummaryBar)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PanelRow(ui.summaryWorkDays, summary.workDaysText(sheet.workDaysSuffix.trim()))
        if (settings.flagEnabled) {
            PanelRow(ui.summaryFlagDays.format(flagName), summary.flagDays.toString())
        }
        if (settings.salaryEnabled) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.18f), thickness = 0.5.dp)
            PanelRow(ui.payGross, summary.grossYen.formatMoney(currency))
            PanelRow(ui.payDeduction, "−${summary.deductionYen.formatMoney(currency)}", Color(0xFFFFE082))
            PanelRow(ui.payNet, summary.netYen.formatMoney(currency), bold = true)
        }
    }
}

@Composable
private fun PanelRow(
    label: String,
    value: String,
    valueColor: Color = Color.White,
    bold: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun SummaryChip(caption: String, value: String) {
    Column(
        modifier = Modifier
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun MoneyTiny(
    label: String,
    value: String,
    valueColor: Color = Color.White,
    bold: Boolean = false,
) {
    Column(horizontalAlignment = Alignment.End) {
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 9.sp, maxLines = 1)
        Text(
            value,
            color = valueColor,
            fontSize = if (bold) 12.sp else 11.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

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
