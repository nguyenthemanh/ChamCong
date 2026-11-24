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
import app.chamcong.ui.theme.AccentGreen
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.chamcong.data.SheetLabelFactory
import app.chamcong.data.SheetLabels
import app.chamcong.ui.components.DayRowItem
import app.chamcong.ui.components.FirstSetupScreen
import app.chamcong.ui.components.MonthTabs
import app.chamcong.ui.components.SettingsDialog
import app.chamcong.ui.components.ShareDialog
import app.chamcong.ui.components.SummaryFooter
import app.chamcong.ui.i18n.Strings
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(SheetPaper),
        containerColor = SheetPaper,
        bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                state.updateStatus?.let { status ->
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
            val adaptive = LocalAdaptive.current
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
                compact = adaptive.isFoldExpanded,
                onSave = {
                    Toast.makeText(context, ui.saved, Toast.LENGTH_SHORT).show()
                },
                onCreateMonth = viewModel::createNewMonth,
                onShare = { viewModel.openShare(true) },
                onSettings = { viewModel.openSettings(true) },
            )

            MonthTabs(
                months = state.months,
                selected = state.selectedMonth,
                sheet = sheet,
                onSelect = viewModel::selectMonth,
                onPrev = viewModel::prevMonth,
                onNext = viewModel::nextMonth,
                prevLabel = ui.prevMonth,
                nextLabel = ui.nextMonth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
            )

            TableHeader(
                sheet = sheet,
                flagColumn = state.settings.flagLabelOff,
                showFlag = state.settings.flagEnabled,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                ) {
                    items(state.rows, key = { it.day }) { row ->
                        DayRowItem(
                            row = row,
                            sheet = sheet,
                            settings = state.settings,
                            onChange = { site, flag ->
                                viewModel.updateDay(row.day, site, flag)
                            },
                        )
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
                if (state.exporting) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }

    if (state.showSettings) {
        SettingsDialog(
            settings = state.settings,
            onDismiss = { viewModel.openSettings(false) },
            onSave = viewModel::saveSettings,
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
    compact: Boolean = false,
    onSave: () -> Unit,
    onCreateMonth: () -> Unit,
    onShare: () -> Unit,
    onSettings: () -> Unit,
) {
    val vPad = if (compact) 6.dp else 10.dp
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
                    style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displaySmall,
                    color = SheetInk,
                    fontSize = if (compact) 22.sp else 28.sp,
                )
                Text(
                    text = yearMonth,
                    style = MaterialTheme.typography.titleMedium,
                    color = SheetMuted,
                    fontSize = if (compact) 14.sp else 16.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = onCreateMonth) {
                    Text(
                        text = "+ $newMonthLabel",
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                TextButton(onClick = onSave) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                    Text(saveLabel, color = AccentGreen, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, contentDescription = shareLabel, tint = SheetInk)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = settingsLabel, tint = SheetInk)
                }
            }
        }
        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SheetHeader)
                .border(1.dp, SheetLine, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = if (compact) 6.dp else 10.dp),
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
                    fontSize = if (compact) 14.sp else 16.sp,
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
