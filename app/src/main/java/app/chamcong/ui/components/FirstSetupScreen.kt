package app.chamcong.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import app.chamcong.ui.theme.AccentGreen
import app.chamcong.ui.theme.SheetInk
import app.chamcong.ui.theme.SheetMuted

/** Full-screen first-run setup — One UI–style grouped cards. */
@Composable
fun FirstSetupScreen(
    initial: PaySettings,
    onFinish: (PaySettings) -> Unit,
) {
    var name by remember { mutableStateOf(initial.employeeName) }
    var kana by remember { mutableStateOf(initial.employeeNameKana) }
    var salaryEnabled by remember { mutableStateOf(initial.salaryEnabled) }
    var flagEnabled by remember { mutableStateOf(initial.flagEnabled) }
    var salaryFolderOpen by remember { mutableStateOf(initial.salaryEnabled) }
    var flagFolderOpen by remember { mutableStateOf(initial.flagEnabled) }
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
            .background(SettingsPageBg)
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
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsSectionLabel(live.sectionLanguage)
            SettingsCard {
                Text(live.uiLanguage, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
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
                Text(live.sheetRegion, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
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
            }

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
        }

        Button(
            onClick = {
                val region = SheetLabelFactory.of(sheetLanguage)
                onFinish(
                    PaySettings(
                        employeeName = name.trim().ifBlank { "NGUYEN VAN A" },
                        employeeNameKana = kana.trim().ifBlank { region.nameReadingExample },
                        salaryEnabled = salaryEnabled,
                        flagEnabled = flagEnabled,
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
                .padding(top = 10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
        ) {
            Text(live.setupContinue, fontWeight = FontWeight.Bold)
        }
    }
}
