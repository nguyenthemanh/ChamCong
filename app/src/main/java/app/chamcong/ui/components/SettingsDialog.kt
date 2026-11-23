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
    val previewTitle = sheetTitle.trim().ifBlank {
        SheetLabelFactory.of(sheetLanguage).title
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(live.settings) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(name, { name = it }, label = { Text(live.name) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(kana, { kana = it }, label = { Text(live.nameKana) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                NumberField(wage, { wage = it }, live.dailyWage)
                NumberField(ot, { ot = it }, live.overtimeHour)
                NumberField(flagBonus, { flagBonus = it }, live.flagBonus)
                NumberField(deduction, { deduction = it }, live.monthlyDeduction)
                Text(live.monthlyDeductionHint, color = SheetMuted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = flagOn,
                    onValueChange = { flagOn = it },
                    label = { Text(live.flagLabelOn) },
                    placeholder = { Text("運転あり") },
                    supportingText = { Text(live.flagLabelOnHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = flagOff,
                    onValueChange = { flagOff = it },
                    label = { Text(live.flagLabelOff) },
                    placeholder = { Text("運転") },
                    supportingText = { Text(live.flagLabelOffHint) },
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
                LanguageRadios(sheetLanguage) { sheetLanguage = it }

                OutlinedTextField(
                    value = sheetTitle,
                    onValueChange = { sheetTitle = it },
                    label = { Text(live.sheetTitle) },
                    placeholder = { Text(SheetLabelFactory.of(sheetLanguage).title) },
                    supportingText = { Text(live.sheetTitleHint) },
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
            TextButton(
                onClick = {
                    onSave(
                        PaySettings(
                            employeeName = name,
                            employeeNameKana = kana,
                            dailyWageYen = wage.toLongOrNull() ?: settings.dailyWageYen,
                            overtimeHourYen = ot.toLongOrNull() ?: settings.overtimeHourYen,
                            flagBonusYen = flagBonus.toLongOrNull() ?: settings.flagBonusYen,
                            monthlyDeductionYen = deduction.toLongOrNull() ?: settings.monthlyDeductionYen,
                            flagLabelOn = flagOn,
                            flagLabelOff = flagOff,
                            language = language,
                            sheetLanguage = sheetLanguage,
                            sheetTitleCustom = sheetTitle,
                        ),
                    )
                },
            ) { Text(live.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(live.cancel) }
        },
    )
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { ch -> ch.isDigit() }) },
        label = { Text(label) },
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
