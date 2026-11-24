package app.chamcong.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.chamcong.data.DayRow
import app.chamcong.data.MonthKey
import app.chamcong.data.PaySettings
import app.chamcong.data.SheetLabelFactory
import app.chamcong.data.SheetLabels
import app.chamcong.ui.components.DayRowItem
import app.chamcong.ui.components.FirstSetupScreen
import app.chamcong.ui.components.MonthTabs
import app.chamcong.ui.components.SettingsDialog
import app.chamcong.ui.components.ShareDialog
import app.chamcong.ui.components.SummaryFooter
import app.chamcong.ui.components.SummaryVariant
import app.chamcong.ui.i18n.AppStrings
import app.chamcong.ui.i18n.Strings
import app.chamcong.ui.theme.AccentGreen
import app.chamcong.ui.theme.ChipIdle
import app.chamcong.ui.theme.SheetHeader
import app.chamcong.ui.theme.SheetInk
import app.chamcong.ui.theme.SheetLine
import app.chamcong.ui.theme.SheetMuted
import app.chamcong.ui.theme.SheetPaper
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MonthReportScreen(
    viewModel: ReportViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ui = Strings.of(state.settings.language)
    val sheet = SheetLabelFactory.of(state.settings.sheetLanguage)
    val context = LocalContext.current
    val appNameWithLang = "${ui.appTitle} · ${state.settings.language.label}"
    val adaptive = LocalAdaptive.current

    SideEffect {
        (context as? Activity)?.title = appNameWithLang
    }

    LaunchedEffect(viewModel) {
        viewModel.shareEvents.collectLatest { event ->
            when (event) {
                is ShareEvent.Ready -> {
                    event.note?.let {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    }
                    context.startActivity(event.intent)
                }
                is ShareEvent.Failed -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (!state.setupReady) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SheetPaper),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (!state.setupDone) {
        FirstSetupScreen(
            initial = state.settings,
            onFinish = viewModel::completeFirstSetup,
        )
        return
    }

    LaunchedEffect(state.setupDone) {
        if (state.setupDone) viewModel.startAutoUpdateIfNeeded()
    }

    val onSaveToast = {
        Toast.makeText(context, ui.saved, Toast.LENGTH_SHORT).show()
    }

    when (adaptive.posture) {
        DevicePosture.Closed -> CoverScreenLayout(
            state = state,
            ui = ui,
            sheet = sheet,
            appNameWithLang = appNameWithLang,
            onSave = onSaveToast,
            onCreateMonth = viewModel::createNewMonth,
            onShare = { viewModel.openShare(true) },
            onSettings = { viewModel.openSettings(true) },
            onSelectMonth = viewModel::selectMonth,
            onPrev = viewModel::prevMonth,
            onNext = viewModel::nextMonth,
            onDayChange = viewModel::updateDay,
        )
        DevicePosture.Book -> UnfoldedBookLayout(
            state = state,
            ui = ui,
            sheet = sheet,
            appNameWithLang = appNameWithLang,
            pad = adaptive.horizontalPadding,
            onSave = onSaveToast,
            onCreateMonth = viewModel::createNewMonth,
            onShare = { viewModel.openShare(true) },
            onSettings = { viewModel.openSettings(true) },
            onSelectMonth = viewModel::selectMonth,
            onPrev = viewModel::prevMonth,
            onNext = viewModel::nextMonth,
            onDayChange = viewModel::updateDay,
        )
        DevicePosture.Tabletop -> TabletopLayout(
            state = state,
            ui = ui,
            sheet = sheet,
            appNameWithLang = appNameWithLang,
            onSave = onSaveToast,
            onCreateMonth = viewModel::createNewMonth,
            onShare = { viewModel.openShare(true) },
            onSettings = { viewModel.openSettings(true) },
            onSelectMonth = viewModel::selectMonth,
            onPrev = viewModel::prevMonth,
            onNext = viewModel::nextMonth,
            onDayChange = viewModel::updateDay,
        )
    }

    if (state.showSettings) {
        SettingsDialog(
            settings = state.settings,
            onDismiss = { viewModel.openSettings(false) },
            onSave = viewModel::saveSettings,
            onClearAllData = viewModel::clearAllData,
        )
    }

    if (state.showShare || state.exporting) {
        ShareDialog(
            strings = ui,
            exporting = state.exporting,
            onDismiss = { viewModel.openShare(false) },
            onShare = viewModel::share,
        )
    }
}

/** Outer cover / closed phone: single column, summary pinned at bottom. */
@Composable
private fun CoverScreenLayout(
    state: ReportUiState,
    ui: AppStrings,
    sheet: SheetLabels,
    appNameWithLang: String,
    onSave: () -> Unit,
    onCreateMonth: () -> Unit,
    onShare: () -> Unit,
    onSettings: () -> Unit,
    onSelectMonth: (MonthKey) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDayChange: (Int, String, Boolean) -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(SheetPaper),
        containerColor = SheetPaper,
        bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                UpdateStatusBanner(state.updateStatus)
                SummaryFooter(
                    summary = state.summary,
                    sheet = sheet,
                    settings = state.settings,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
        ) {
            ReportHeader(
                appName = appNameWithLang,
                title = state.settings.resolvedSheetTitle(),
                yearMonth = sheet.monthLabel(state.selectedMonth),
                employeeName = state.settings.employeeName,
                employeeKana = state.settings.employeeNameKana,
                namePrefix = sheet.namePrefix,
                saveLabel = ui.save,
                newMonthLabel = ui.newMonth,
                shareLabel = ui.share,
                settingsLabel = ui.settings,
                mode = HeaderMode.Cover,
                onSave = onSave,
                onCreateMonth = onCreateMonth,
                onShare = onShare,
                onSettings = onSettings,
            )
            MonthTabs(
                months = state.months,
                selected = state.selectedMonth,
                sheet = sheet,
                onSelect = onSelectMonth,
                onPrev = onPrev,
                onNext = onNext,
                prevLabel = ui.prevMonth,
                nextLabel = ui.nextMonth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
            )
            DaysPane(
                rows = state.rows,
                sheet = sheet,
                settings = state.settings,
                exporting = state.exporting,
                onDayChange = onDayChange,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Inner unfolded (book): compact left rail + wide day sheet.
 * Not a scaled cover — rail is narrow and fully filled (no empty green hole).
 */
@Composable
private fun UnfoldedBookLayout(
    state: ReportUiState,
    ui: AppStrings,
    sheet: SheetLabels,
    appNameWithLang: String,
    pad: androidx.compose.ui.unit.Dp,
    onSave: () -> Unit,
    onCreateMonth: () -> Unit,
    onShare: () -> Unit,
    onSettings: () -> Unit,
    onSelectMonth: (MonthKey) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDayChange: (Int, String, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SheetPaper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = pad, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(18.dp))
                .background(ChipIdle)
                .border(1.dp, SheetLine, RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ui.appTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                    IconButton(onClick = onShare, modifier = Modifier.height(36.dp)) {
                        Icon(Icons.Outlined.Share, ui.share, tint = SheetInk)
                    }
                    IconButton(onClick = onSettings, modifier = Modifier.height(36.dp)) {
                        Icon(Icons.Outlined.Settings, ui.settings, tint = SheetInk)
                    }
                }
            }
            Text(
                text = state.settings.resolvedSheetTitle(),
                style = MaterialTheme.typography.titleMedium,
                color = SheetInk,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = sheet.monthLabel(state.selectedMonth),
                style = MaterialTheme.typography.bodyMedium,
                color = SheetMuted,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${sheet.namePrefix}${state.settings.employeeName}",
                style = MaterialTheme.typography.bodyMedium,
                color = SheetInk,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.settings.employeeNameKana.isNotBlank()) {
                Text(
                    text = state.settings.employeeNameKana,
                    style = MaterialTheme.typography.bodySmall,
                    color = SheetMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TextButton(
                    onClick = onCreateMonth,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+", color = AccentGreen, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Save, null, tint = AccentGreen)
                }
            }

            MonthTabs(
                months = state.months,
                selected = state.selectedMonth,
                sheet = sheet,
                onSelect = onSelectMonth,
                onPrev = onPrev,
                onNext = onNext,
                prevLabel = ui.prevMonth,
                nextLabel = ui.nextMonth,
                modifier = Modifier.fillMaxWidth(),
            )
            UpdateStatusBanner(state.updateStatus)

            Spacer(Modifier.weight(1f, fill = true))

            SummaryFooter(
                summary = state.summary,
                sheet = sheet,
                settings = state.settings,
                variant = SummaryVariant.Panel,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        BookPaneDivider(Modifier.padding(horizontal = 10.dp))

        DaysPane(
            rows = state.rows,
            sheet = sheet,
            settings = state.settings,
            exporting = state.exporting,
            onDayChange = onDayChange,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

/** Half-open tabletop: sheet on top half, controls + summary below hinge. */
@Composable
private fun TabletopLayout(
    state: ReportUiState,
    ui: AppStrings,
    sheet: SheetLabels,
    appNameWithLang: String,
    onSave: () -> Unit,
    onCreateMonth: () -> Unit,
    onShare: () -> Unit,
    onSettings: () -> Unit,
    onSelectMonth: (MonthKey) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDayChange: (Int, String, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SheetPaper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        DaysPane(
            rows = state.rows,
            sheet = sheet,
            settings = state.settings,
            exporting = state.exporting,
            onDayChange = onDayChange,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        FoldHingeSpacer()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SheetHeader.copy(alpha = 0.4f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ReportHeader(
                appName = appNameWithLang,
                title = state.settings.resolvedSheetTitle(),
                yearMonth = sheet.monthLabel(state.selectedMonth),
                employeeName = state.settings.employeeName,
                employeeKana = state.settings.employeeNameKana,
                namePrefix = sheet.namePrefix,
                saveLabel = ui.save,
                newMonthLabel = ui.newMonth,
                shareLabel = ui.share,
                settingsLabel = ui.settings,
                mode = HeaderMode.Tabletop,
                onSave = onSave,
                onCreateMonth = onCreateMonth,
                onShare = onShare,
                onSettings = onSettings,
            )
            MonthTabs(
                months = state.months,
                selected = state.selectedMonth,
                sheet = sheet,
                onSelect = onSelectMonth,
                onPrev = onPrev,
                onNext = onNext,
                prevLabel = ui.prevMonth,
                nextLabel = ui.nextMonth,
                modifier = Modifier.fillMaxWidth(),
            )
            UpdateStatusBanner(state.updateStatus)
            SummaryFooter(
                summary = state.summary,
                sheet = sheet,
                settings = state.settings,
            )
        }
    }
}

@Composable
private fun DaysPane(
    rows: List<DayRow>,
    sheet: SheetLabels,
    settings: PaySettings,
    exporting: Boolean,
    onDayChange: (Int, String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TableHeader(
            sheet = sheet,
            flagColumn = settings.flagLabelOff,
            showFlag = settings.flagEnabled,
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
            ) {
                items(rows, key = { it.day }) { row ->
                    DayRowItem(
                        row = row,
                        sheet = sheet,
                        settings = settings,
                        onChange = { site, flag -> onDayChange(row.day, site, flag) },
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
            if (exporting) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun UpdateStatusBanner(status: String?) {
    if (status == null) return
    Text(
        text = status,
        modifier = Modifier
            .fillMaxWidth()
            .background(SheetHeader)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = SheetMuted,
        textAlign = TextAlign.Center,
    )
}

private enum class HeaderMode { Cover, Book, Tabletop }

@Composable
private fun ReportHeader(
    appName: String,
    title: String,
    yearMonth: String,
    employeeName: String,
    employeeKana: String,
    namePrefix: String,
    saveLabel: String,
    newMonthLabel: String,
    shareLabel: String,
    settingsLabel: String,
    mode: HeaderMode,
    onSave: () -> Unit,
    onCreateMonth: () -> Unit,
    onShare: () -> Unit,
    onSettings: () -> Unit,
) {
    val vPad = when (mode) {
        HeaderMode.Cover -> 10.dp
        HeaderMode.Tabletop -> 4.dp
        HeaderMode.Book -> 8.dp
    }
    val titleSize = when (mode) {
        HeaderMode.Cover -> 26.sp
        HeaderMode.Tabletop -> 20.sp
        HeaderMode.Book -> 24.sp
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = vPad),
    ) {
        Text(
            text = appName,
            style = MaterialTheme.typography.labelLarge,
            color = AccentGreen,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall,
                    color = SheetInk,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (mode == HeaderMode.Tabletop) 1 else 2,
                )
                Text(
                    text = yearMonth,
                    style = MaterialTheme.typography.titleMedium,
                    color = SheetMuted,
                    fontSize = if (mode == HeaderMode.Tabletop) 13.sp else 15.sp,
                )
            }
            if (mode != HeaderMode.Book) {
                Spacer(Modifier.width(8.dp))
                ActionRow(
                    saveLabel = saveLabel,
                    newMonthLabel = newMonthLabel,
                    shareLabel = shareLabel,
                    settingsLabel = settingsLabel,
                    stacked = false,
                    onSave = onSave,
                    onCreateMonth = onCreateMonth,
                    onShare = onShare,
                    onSettings = onSettings,
                )
            }
        }
        if (mode != HeaderMode.Book) {
            Spacer(Modifier.height(if (mode == HeaderMode.Tabletop) 4.dp else 8.dp))
            EmployeeCard(
                namePrefix = namePrefix,
                employeeName = employeeName,
                employeeKana = employeeKana,
                dense = mode == HeaderMode.Tabletop,
            )
        }
    }
}

@Composable
private fun EmployeeCard(
    namePrefix: String,
    employeeName: String,
    employeeKana: String,
    dense: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SheetHeader)
            .border(1.dp, SheetLine, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = if (dense) 6.dp else 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$namePrefix$employeeName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SheetInk,
                fontSize = if (dense) 14.sp else 16.sp,
            )
            if (employeeKana.isNotBlank()) {
                Text(
                    employeeKana,
                    style = MaterialTheme.typography.bodySmall,
                    color = SheetMuted,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    saveLabel: String,
    newMonthLabel: String,
    shareLabel: String,
    settingsLabel: String,
    stacked: Boolean,
    onSave: () -> Unit,
    onCreateMonth: () -> Unit,
    onShare: () -> Unit,
    onSettings: () -> Unit,
) {
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TextButton(onClick = onCreateMonth, modifier = Modifier.fillMaxWidth()) {
                Text("+ $newMonthLabel", color = AccentGreen, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Save, null, tint = AccentGreen, modifier = Modifier.padding(end = 6.dp))
                Text(saveLabel, color = AccentGreen, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, shareLabel, tint = SheetInk)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Outlined.Settings, settingsLabel, tint = SheetInk)
                }
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TextButton(onClick = onCreateMonth) {
                Text("+ $newMonthLabel", color = AccentGreen, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            TextButton(onClick = onSave) {
                Icon(Icons.Outlined.Save, null, tint = AccentGreen, modifier = Modifier.padding(end = 4.dp))
                Text(saveLabel, color = AccentGreen, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, shareLabel, tint = SheetInk)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, settingsLabel, tint = SheetInk)
            }
        }
    }
}

@Composable
private fun TableHeader(sheet: SheetLabels, flagColumn: String, showFlag: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            .background(SheetHeader)
            .border(0.5.dp, SheetLine)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(sheet.colDay, Modifier.width(28.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(sheet.colWeekday, Modifier.width(36.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(sheet.colSite, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        if (showFlag) {
            Text(flagColumn, Modifier.width(72.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
    HorizontalDivider(color = SheetLine, thickness = 1.dp, modifier = Modifier.padding(horizontal = 10.dp))
}
