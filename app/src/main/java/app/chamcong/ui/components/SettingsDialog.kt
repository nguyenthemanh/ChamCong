package app.chamcong.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.chamcong.data.AppLanguage
import app.chamcong.data.PaySettings
import app.chamcong.data.SheetLabelFactory
import app.chamcong.ui.i18n.Strings
import app.chamcong.ui.theme.AccentRed
import app.chamcong.ui.theme.SheetMuted

@Composable
fun SettingsDialog(
    settings: PaySettings,
    onDismiss: () -> Unit,
    onSave: (PaySettings) -> Unit,
    onClearAllData: () -> Unit,
) {
    var name by remember(settings) { mutableStateOf(settings.employeeName) }
    var kana by remember(settings) { mutableStateOf(settings.employeeNameKana) }
    var salaryEnabled by remember(settings) { mutableStateOf(settings.salaryEnabled) }
    var flagEnabled by remember(settings) { mutableStateOf(settings.flagEnabled) }
    var salaryFolderOpen by remember(settings) { mutableStateOf(false) }
    var flagFolderOpen by remember(settings) { mutableStateOf(false) }
    var wage by remember(settings) { mutableStateOf(settings.dailyWageYen.toString()) }
    var ot by remember(settings) { mutableStateOf(settings.overtimeHourYen.toString()) }
    var flagBonus by remember(settings) { mutableStateOf(settings.flagBonusYen.toString()) }
    var deduction by remember(settings) { mutableStateOf(settings.monthlyDeductionYen.toString()) }
    var flagOn by remember(settings) { mutableStateOf(settings.flagLabelOn) }
    var flagOff by remember(settings) { mutableStateOf(settings.flagLabelOff) }
    var language by remember(settings) { mutableStateOf(settings.language) }
    var sheetLanguage by remember(settings) { mutableStateOf(settings.sheetLanguage) }
    var sheetTitle by remember(settings) { mutableStateOf(settings.sheetTitleCustom) }
    /** 0 = none, 1 = first warning, 2 = second warning. */
    var clearStep by remember { mutableIntStateOf(0) }
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
            salaryEnabled = salaryEnabled,
            flagEnabled = flagEnabled,
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

    AlertDialog(
        onDismissRequest = { onSave(buildSettings()) },
        containerColor = SettingsPageBg,
        title = {
            Text(live.settings, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .background(SettingsPageBg),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsSectionLabel(live.sectionProfile)
                SettingsCard {
                    SettingsTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = live.name,
                        placeholder = "NGUYEN VAN A",
                    )
                    SettingsTextField(
                        value = kana,
                        onValueChange = { kana = it },
                        label = live.nameKana,
                        placeholder = sheet.nameReadingExample,
                        supporting = sheet.nameReadingExample,
                    )
                }

                SettingsSectionLabel(live.sectionFeatures)
                SettingsCard {
                    SettingsSwitchRow(
                        title = live.salaryEnabled,
                        hint = live.salaryEnabledHint,
                        checked = salaryEnabled,
                        onCheckedChange = {
                            salaryEnabled = it
                            salaryFolderOpen = it
                        },
                    )
                    if (salaryEnabled) {
                        SettingsCardDivider()
                        SettingsFolderRow(
                            title = live.salaryFolder,
                            openTitle = live.salaryFolderOpen,
                            open = salaryFolderOpen,
                            onToggle = { salaryFolderOpen = !salaryFolderOpen },
                        ) {
                            SettingsNumberField(wage, { wage = it }, live.dailyWageLabel(currency))
                            SettingsNumberField(ot, { ot = it }, live.overtimeHourLabel(currency))
                            SettingsNumberField(
                                deduction,
                                { deduction = it },
                                live.monthlyDeductionLabel(currency),
                                supporting = live.monthlyDeductionHintText(currency),
                            )
                            if (flagEnabled) {
                                SettingsNumberField(
                                    value = flagBonus,
                                    onChange = { flagBonus = it },
                                    label = live.flagBonusLabel(currency),
                                    supporting = live.flagBonusHint.format(
                                        flagOn.ifBlank { sheet.drivingOn },
                                    ),
                                )
                            }
                        }
                    }

                    SettingsCardDivider()
                    SettingsSwitchRow(
                        title = live.flagEnabled,
                        hint = live.flagEnabledHint,
                        checked = flagEnabled,
                        onCheckedChange = {
                            flagEnabled = it
                            flagFolderOpen = it
                        },
                    )
                    if (flagEnabled) {
                        SettingsCardDivider()
                        SettingsFolderRow(
                            title = live.flagFolder,
                            openTitle = live.flagFolderOpen,
                            open = flagFolderOpen,
                            onToggle = { flagFolderOpen = !flagFolderOpen },
                        ) {
                            SettingsTextField(
                                value = flagOn,
                                onValueChange = { flagOn = it },
                                label = live.flagLabelOn,
                                placeholder = sheet.drivingOn,
                                supporting = live.flagLabelOnHint.format(sheet.drivingOn),
                            )
                            SettingsTextField(
                                value = flagOff,
                                onValueChange = { flagOff = it },
                                label = live.flagLabelOff,
                                placeholder = sheet.driving,
                                supporting = live.flagLabelOffHint.format(sheet.driving),
                            )
                        }
                    }
                }

                SettingsSectionLabel(live.sectionLanguage)
                SettingsCard {
                    Text(
                        live.uiLanguage,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        live.uiLanguageHint,
                        color = SheetMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    SettingsLanguageRadios(
                        selected = language,
                        onSelect = { language = it },
                    )

                    SettingsCardDivider()
                    Text(
                        live.sheetRegion,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        live.sheetRegionHint,
                        color = SheetMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    SettingsLanguageRadios(
                        selected = sheetLanguage,
                        onSelect = { next ->
                            applyRegionExamples(sheetLanguage, next)
                            sheetLanguage = next
                        },
                        useCountryLabel = true,
                    )

                    SettingsCardDivider()
                    SettingsTextField(
                        value = sheetTitle,
                        onValueChange = { sheetTitle = it },
                        label = live.sheetTitle,
                        placeholder = sheet.title,
                        supporting = live.sheetTitleHint.format(sheet.title),
                    )
                    Text(
                        text = "→ $previewTitle",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                SettingsSectionLabel(live.sectionAbout)
                SettingsCard {
                    AboutUpdateBlock(strings = live)
                }

                SettingsSectionLabel(live.sectionDanger)
                SettingsCard {
                    Text(
                        live.clearData,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentRed,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Text(
                        live.clearDataHint,
                        color = SheetMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                    )
                    Button(
                        onClick = { clearStep = 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    ) {
                        Text(live.clearData, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(buildSettings()) }) { Text(live.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(live.cancel) }
        },
    )

    if (clearStep == 1) {
        AlertDialog(
            onDismissRequest = { clearStep = 0 },
            title = { Text(live.clearDataWarn1Title, fontWeight = FontWeight.Bold) },
            text = { Text(live.clearDataWarn1Body) },
            confirmButton = {
                TextButton(onClick = { clearStep = 2 }) {
                    Text(live.clearDataWarn1Confirm, color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { clearStep = 0 }) { Text(live.cancel) }
            },
        )
    }
    if (clearStep == 2) {
        AlertDialog(
            onDismissRequest = { clearStep = 0 },
            title = { Text(live.clearDataWarn2Title, fontWeight = FontWeight.Bold, color = AccentRed) },
            text = { Text(live.clearDataWarn2Body) },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearStep = 0
                        onClearAllData()
                    },
                ) {
                    Text(live.clearDataWarn2Confirm, color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { clearStep = 0 }) { Text(live.cancel) }
            },
        )
    }
}
