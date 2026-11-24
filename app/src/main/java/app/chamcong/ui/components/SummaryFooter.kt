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
import app.chamcong.data.MonthSummary
import app.chamcong.data.PaySettings
import app.chamcong.data.SheetLabels
import app.chamcong.ui.i18n.Strings
import app.chamcong.ui.theme.SummaryBar

/** Compact reference strip — not the main workspace. */
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniBox(summary.workDaysText(sheet.workDaysSuffix.trim()))
            if (settings.flagEnabled) {
                MiniBox(summary.flagDays.toString())
            }
            if (settings.salaryEnabled) {
                Text(
                    text = "${ui.payGross} ${summary.grossYen.formatMoney(currency)}",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "−${summary.deductionYen.formatMoney(currency)}",
                    color = Color(0xFFFFE082),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
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

@Composable
private fun MiniBox(text: String) {
    Box(
        modifier = Modifier
            .widthIn(min = 40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(0.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
