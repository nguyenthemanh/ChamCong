package app.chamcong.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.chamcong.data.AppLanguage
import app.chamcong.data.PaySettings
import app.chamcong.data.SheetLabelFactory
import app.chamcong.ui.i18n.Strings
import app.chamcong.ui.theme.AccentGreen
import app.chamcong.ui.theme.SheetInk
import app.chamcong.ui.theme.SheetMuted
import app.chamcong.ui.theme.SheetPaper

/** Full-screen first-run setup form. */
@Composable
fun FirstSetupScreen(
    initial: PaySettings,
    onFinish: (PaySettings) -> Unit,
) {
    var name by remember { mutableStateOf(initial.employeeName) }
    var kana by remember { mutableStateOf(initial.employeeNameKana) }
    var wage by remember { mutableStateOf(initial.dailyWageYen.toString()) }
    var ot by remember { mutableStateOf(initial.overtimeHourYen.toString()) }
    var flagBonus by remember { mutableStateOf(initial.flagBonusYen.toString()) }
    var deduction by remember { mutableStateOf(initial.monthlyDeductionYen.toString()) }
    var flagOn by remember { mutableStateOf(initial.flagLabelOn) }
    var flagOff by remember { mutableStateOf(initial.flagLabelOff) }
    var language by remember { mutableStateOf(initial.language) }
    var sheetLanguage by remember { mutableStateOf(initial.sheetLanguage) }
    var sheetTitle by remember { mutableStateOf(initial.sheetTitleCustom) }

    val live = Strings.of(language)
    val sheet = SheetLabelFactory.of(sheetLanguage)
    val currency = sheet.currencySuffix

    fun applyRegionExamples(from: AppLanguage, to: AppLanguage) {
        if (from == to) return
        val prev = SheetLabelFactory.of(from)
        val next = SheetLabelFactory.of(to)
        if (flagOn.isBlank() || flagOn == prev.drivingOn) flagOn = next.drivingOn
        if (flagOff.isBlank() || flagOff == prev.driving) flagOff = next.driving
        if (kana.isBlank() || kana == prev.nameReadingExample) kana = next.nameReadingExample
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SheetPaper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = live.setupTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SheetInk,
        )
        Text(
            text = live.setupSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = SheetMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(live.uiLanguage, fontWeight = FontWeight.SemiBold)
            Text(live.uiLanguageHint, color = SheetMuted, style = MaterialTheme.typography.bodySmall)
            LangRadios(language) { language = it }

            HorizontalDivider()
            Text(live.sheetRegion, fontWeight = FontWeight.SemiBold)
            Text(live.sheetRegionHint, color = SheetMuted, style = MaterialTheme.typography.bodySmall)
            LangRadios(sheetLanguage) { next ->
                applyRegionExamples(sheetLanguage, next)
                sheetLanguage = next
            }

            OutlinedTextField(
                value = sheetTitle,
                onValueChange = { sheetTitle = it },
                label = { Text(live.sheetTitle) },
                placeholder = { Text(sheet.title) },
                supportingText = { Text(live.sheetTitleHint.format(sheet.title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(live.name) },
                placeholder = { Text("NGUYEN VAN A") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = kana,
                onValueChange = { kana = it },
                label = { Text(live.nameKana) },
                placeholder = { Text(sheet.nameReadingExample) },
                supportingText = { Text(sheet.nameReadingExample) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SetupNumber(wage, { wage = it }, live.dailyWageLabel(currency))
            SetupNumber(ot, { ot = it }, live.overtimeHourLabel(currency))
            SetupNumber(deduction, { deduction = it }, live.monthlyDeductionLabel(currency))
            Text(
                live.monthlyDeductionHintText(currency),
                color = SheetMuted,
                style = MaterialTheme.typography.bodySmall,
            )

            HorizontalDivider()
            OutlinedTextField(
                value = flagOn,
                onValueChange = { flagOn = it },
                label = { Text(live.flagLabelOn) },
                placeholder = { Text(sheet.drivingOn) },
                supportingText = { Text(live.flagLabelOnHint.format(sheet.drivingOn)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SetupNumber(
                value = flagBonus,
                onChange = { flagBonus = it },
                label = live.flagBonusLabel(currency),
                supporting = live.flagBonusHint.format(flagOn.ifBlank { sheet.drivingOn }),
            )
            OutlinedTextField(
                value = flagOff,
                onValueChange = { flagOff = it },
                label = { Text(live.flagLabelOff) },
                placeholder = { Text(sheet.driving) },
                supportingText = { Text(live.flagLabelOffHint.format(sheet.driving)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SpacerBottom()
        }

        Button(
            onClick = {
                val region = SheetLabelFactory.of(sheetLanguage)
                onFinish(
                    PaySettings(
                        employeeName = name.trim().ifBlank { "NGUYEN VAN A" },
                        employeeNameKana = kana.trim().ifBlank { region.nameReadingExample },
                        dailyWageYen = wage.toLongOrNull() ?: 0L,
                        overtimeHourYen = ot.toLongOrNull() ?: 0L,
                        flagBonusYen = flagBonus.toLongOrNull() ?: 0L,
                        monthlyDeductionYen = deduction.toLongOrNull() ?: 0L,
                        flagLabelOn = flagOn.trim().ifBlank { region.drivingOn },
                        flagLabelOff = flagOff.trim().ifBlank { region.driving },
                        language = language,
                        sheetLanguage = sheetLanguage,
                        sheetTitleCustom = sheetTitle.trim(),
                    ),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text(live.setupContinue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SpacerBottom() {
    androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 8.dp))
}

@Composable
private fun SetupNumber(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { ch -> ch.isDigit() }) },
        label = { Text(label) },
        supportingText = supporting?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LangRadios(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    AppLanguage.entries.forEach { lang ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected == lang,
                    onClick = { onSelect(lang) },
                    role = Role.RadioButton,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected == lang, onClick = { onSelect(lang) })
            Text(lang.label, color = if (selected == lang) AccentGreen else SheetInk)
        }
    }
}
