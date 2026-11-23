package app.chamcong.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import app.chamcong.ui.theme.SheetMuted

@Composable
fun SettingsDialog(
    settings: PaySettings,
    onDismiss: () -> Unit,
    onSave: (PaySettings) -> Unit,
) {
    var name by remember(settings) { mutableStateOf(settings.employeeName) }
    var kana by remember(settings) { mutableStateOf(settings.employeeNameKana) }
    var wage by remember(settings) { mutableStateOf(settings.dailyWageYen.toString()) }
    var ot by remember(settings) { mutableStateOf(settings.overtimeHourYen.toString()) }
    var flagBonus by remember(settings) { mutableStateOf(settings.flagBonusYen.toString()) }
    var deduction by remember(settings) { mutableStateOf(settings.monthlyDeductionYen.toString()) }
    var flagOn by remember(settings) { mutableStateOf(settings.flagLabelOn) }
    var flagOff by remember(settings) { mutableStateOf(settings.flagLabelOff) }
    var language by remember(settings) { mutableStateOf(settings.language) }
    var sheetLanguage by remember(settings) { mutableStateOf(settings.sheetLanguage) }
    var sheetTitle by remember(settings) { mutableStateOf(settings.sheetTitleCustom) }
    val live = Strings.of(language)
    val sheet = SheetLabelFactory.of(sheetLanguage)
    val currency = sheet.currencySuffix
    val previewTitle = sheetTitle.trim().ifBlank { sheet.title }

    fun applyRegionExamples(from: AppLanguage, to: AppLanguage) {
        if (from == to) return
        val prev = SheetLabelFactory.of(from)
        val next = SheetLabelFactory.of(to)
        if (flagOn.isBlank() || flagOn == prev.drivingOn) flagOn = next.drivingOn
        if (flagOff.isBlank() || flagOff == prev.driving) flagOff = next.driving
        if (kana.isBlank() || kana == prev.nameReadingExample) kana = next.nameReadingExample
    }

    fun buildSettings(): PaySettings {
        val region = SheetLabelFactory.of(sheetLanguage)
        return PaySettings(
            employeeName = name,
            employeeNameKana = kana,
            dailyWageYen = wage.toLongOrNull() ?: settings.dailyWageYen,
            overtimeHourYen = ot.toLongOrNull() ?: settings.overtimeHourYen,
            flagBonusYen = flagBonus.toLongOrNull() ?: settings.flagBonusYen,
            monthlyDeductionYen = deduction.toLongOrNull() ?: settings.monthlyDeductionYen,
            flagLabelOn = flagOn.trim().ifBlank { region.drivingOn },
            flagLabelOff = flagOff.trim().ifBlank { region.driving },
            language = language,
            sheetLanguage = sheetLanguage,
            sheetTitleCustom = sheetTitle,
        )
    }

    // Back / tap outside → auto-save (forget to press Lưu).
    AlertDialog(
        onDismissRequest = { onSave(buildSettings()) },
        title = { Text(live.settings) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
                NumberField(wage, { wage = it }, live.dailyWageLabel(currency))
                NumberField(ot, { ot = it }, live.overtimeHourLabel(currency))
                NumberField(deduction, { deduction = it }, live.monthlyDeductionLabel(currency))
                Text(
                    live.monthlyDeductionHintText(currency),
                    color = SheetMuted,
                    style = MaterialTheme.typography.bodySmall,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                OutlinedTextField(
                    value = flagOn,
                    onValueChange = { flagOn = it },
                    label = { Text(live.flagLabelOn) },
                    placeholder = { Text(sheet.drivingOn) },
                    supportingText = { Text(live.flagLabelOnHint.format(sheet.drivingOn)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                NumberField(
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(live.uiLanguage, fontWeight = FontWeight.SemiBold)
                Text(live.uiLanguageHint, color = SheetMuted, style = MaterialTheme.typography.bodySmall)
                LanguageRadios(language) { language = it }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(live.sheetRegion, fontWeight = FontWeight.SemiBold)
                Text(live.sheetRegionHint, color = SheetMuted, style = MaterialTheme.typography.bodySmall)
                LanguageRadios(sheetLanguage) { next ->
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
                Text(
                    text = "→ $previewTitle",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                AboutUpdateBlock(strings = live)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(buildSettings()) }) { Text(live.save) }
        },
        dismissButton = {
            // Cancel still discards; back/outside already auto-saves via onDismissRequest.
            TextButton(onClick = onDismiss) { Text(live.cancel) }
        },
    )
}

@Composable
private fun NumberField(
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
private fun LanguageRadios(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
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
            Text(lang.label)
        }
    }
}
