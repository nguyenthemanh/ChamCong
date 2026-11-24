package app.chamcong.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.chamcong.AppLocale
import app.chamcong.BuildConfig
import app.chamcong.UpdateChecker
import app.chamcong.UpdateInstaller
import app.chamcong.data.AttendanceRepository
import app.chamcong.data.CalendarFactory
import app.chamcong.data.DayEntry
import app.chamcong.data.MonthKey
import app.chamcong.data.MonthSummary
import app.chamcong.data.PaySettings
import app.chamcong.data.SheetLabelFactory
import app.chamcong.export.ExportFormat
import app.chamcong.export.ReportExporter
import app.chamcong.ui.i18n.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class ReportUiState(
    val selectedMonth: MonthKey = MonthKey.current(),
    val months: List<MonthKey> = listOf(MonthKey.current()),
    val rows: List<app.chamcong.data.DayRow> = emptyList(),
    val summary: MonthSummary = MonthSummary(0, 0, 0, 0, 0, 0),
    val settings: PaySettings = PaySettings(),
    val setupDone: Boolean = true,
    val setupReady: Boolean = false,
    val showSettings: Boolean = false,
    val showShare: Boolean = false,
    val exporting: Boolean = false,
    val updateStatus: String? = null,
)

sealed interface ShareEvent {
    data class Ready(val intent: Intent, val note: String? = null) : ShareEvent
    data class Failed(val message: String) : ShareEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AttendanceRepository(app)

    private val selectedMonth = MutableStateFlow(MonthKey.current())
    /** Optimistic month list while deleting — avoids khung tháng nháy 1 frame. */
    private val monthsOverride = MutableStateFlow<List<MonthKey>?>(null)
    private val showSettings = MutableStateFlow(false)
    private val showShare = MutableStateFlow(false)
    private val exporting = MutableStateFlow(false)
    private val updateStatus = MutableStateFlow<String?>(null)
    private var autoUpdateStarted = false

    private val _shareEvents = MutableSharedFlow<ShareEvent>(extraBufferCapacity = 1)
    val shareEvents = _shareEvents.asSharedFlow()

    private data class Core(
        val month: MonthKey,
        val months: List<MonthKey>,
        val entries: Map<Int, DayEntry>,
        val settings: PaySettings,
        val setupDone: Boolean,
    )

    private data class Dialogs(
        val settingsOpen: Boolean,
        val shareOpen: Boolean,
        val exporting: Boolean,
        val updateStatus: String?,
    )

    /**
     * Pair month + entries in one emission so the header never shows month A
     * while the day list still has month B's cells (combine race with flatMapLatest).
     */
    private val monthWithEntries = selectedMonth.flatMapLatest { month ->
        repo.entriesFor(month).map { entries -> month to entries }
    }

    val uiState: StateFlow<ReportUiState> = combine(
        combine(
            monthWithEntries,
            repo.knownMonths,
            monthsOverride,
            repo.settings,
            repo.setupDone,
        ) { monthEntries, months, override, settings, setupDone ->
            val (month, entries) = monthEntries
            val shown = override ?: months
            Core(month, shown, entries, settings, setupDone)
        },
        combine(showSettings, showShare, exporting, updateStatus) { s, sh, e, u ->
            Dialogs(s, sh, e, u)
        },
    ) { core, dialogs ->
        val rows = CalendarFactory.buildRows(core.month, core.entries, core.settings.sheetLanguage)
        ReportUiState(
            selectedMonth = core.month,
            months = core.months.ifEmpty { listOf(core.month) },
            rows = rows,
            summary = CalendarFactory.summarize(rows, core.settings),
            settings = core.settings,
            setupDone = core.setupDone,
            setupReady = true,
            showSettings = dialogs.settingsOpen,
            showShare = dialogs.shareOpen,
            exporting = dialogs.exporting,
            updateStatus = dialogs.updateStatus,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportUiState(setupDone = true, setupReady = false),
    )

    init {
        viewModelScope.launch {
            repo.ensureMonth(MonthKey.current())
        }
    }

    /** Call once UI is ready — checks GitHub release, downloads & installs if newer. */
    fun startAutoUpdateIfNeeded() {
        if (autoUpdateStarted) return
        autoUpdateStarted = true
        viewModelScope.launch {
            val strings = Strings.of(uiState.value.settings.language)
            try {
                updateStatus.value = strings.updateChecking
                val result = withContext(Dispatchers.IO) {
                    UpdateChecker.check(BuildConfig.VERSION_NAME)
                }
                if (!result.newer) {
                    updateStatus.value = null
                    return@launch
                }
                val apkUrl = result.apkUrl
                if (apkUrl.isNullOrBlank()) {
                    updateStatus.value = strings.updateNoApk.format(result.tagName)
                    return@launch
                }
                updateStatus.value = strings.updateFound.format(result.tagName)
                val app = getApplication<Application>()
                val file = withContext(Dispatchers.IO) {
                    UpdateChecker.downloadApk(app, apkUrl) { pct ->
                        updateStatus.value = strings.updateDownloading.format(pct)
                    }
                }
                updateStatus.value = strings.updateInstalling
                UpdateInstaller.installApk(app, file, strings.updateNeedInstallPerm)
                updateStatus.value = null
            } catch (_: Exception) {
                // Silent on launch — user can still check manually in Settings.
                updateStatus.value = null
            }
        }
    }

    fun selectMonth(month: MonthKey) {
        if (month == selectedMonth.value) return
        selectedMonth.value = month
        // Do not ensureMonth here for every snap — only create via +/- / new month.
        // ensureMonth on select was re-adding a month right after delete.
    }

    fun prevMonth() = shiftMonth(false)

    fun nextMonth() = shiftMonth(true)

    fun createNewMonth() {
        viewModelScope.launch {
            val newest = uiState.value.months.maxOrNull() ?: selectedMonth.value
            val key = repo.createNextMonth(newest)
            selectedMonth.value = key
        }
    }

    fun deleteMonth(month: MonthKey) {
        viewModelScope.launch {
            val remaining = uiState.value.months.filterNot { it == month }.distinct().sorted()
            val preview = when {
                remaining.isEmpty() -> MonthKey.current()
                else -> remaining.lastOrNull { it < month }
                    ?: remaining.firstOrNull { it > month }
                    ?: remaining.last()
            }
            val shown = remaining.ifEmpty { listOf(preview) }
            // List + selected cùng lúc; giữ override đến khi DataStore khớp hệt.
            monthsOverride.value = shown
            selectedMonth.value = preview
            runCatching {
                val next = repo.deleteMonth(month)
                if (next != selectedMonth.value) {
                    selectedMonth.value = next
                }
                val want = shown.toSet()
                withTimeoutOrNull(2_000) {
                    repo.knownMonths.first { list ->
                        !list.contains(month) && list.toSet() == want
                    }
                }
            }
            monthsOverride.value = null
        }
    }

    private fun shiftMonth(next: Boolean) {
        viewModelScope.launch {
            val key = repo.addAdjacentMonth(selectedMonth.value, next)
            selectedMonth.value = key
        }
    }

    fun updateDay(day: Int, site: String, hasFlag: Boolean, overtimeMinutes: Int = 0) {
        val month = selectedMonth.value
        viewModelScope.launch {
            repo.updateEntry(
                month,
                DayEntry(
                    day = day,
                    site = site,
                    hasFlag = hasFlag,
                    overtimeMinutes = overtimeMinutes.coerceIn(0, 12 * 60),
                ),
            )
        }
    }

    fun openSettings(open: Boolean) {
        showSettings.value = open
    }

    fun openShare(open: Boolean) {
        showShare.value = open
    }

    fun saveSettings(settings: PaySettings) {
        viewModelScope.launch {
            repo.updateSettings(settings)
            AppLocale.apply(settings.language)
            showSettings.value = false
        }
    }

    fun completeFirstSetup(settings: PaySettings) {
        viewModelScope.launch {
            repo.completeFirstSetup(settings)
            AppLocale.apply(settings.language)
            startAutoUpdateIfNeeded()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repo.clearAllData()
            selectedMonth.value = MonthKey.current()
            showSettings.value = false
            showShare.value = false
            updateStatus.value = null
            autoUpdateStarted = false
            repo.ensureMonth(MonthKey.current())
        }
    }

    fun share(format: ExportFormat) {
        val state = uiState.value
        if (state.exporting) return
        viewModelScope.launch {
            exporting.value = true
            showShare.value = false
            try {
                val file = withContext(Dispatchers.Default) {
                    ReportExporter.export(
                        context = getApplication(),
                        format = format,
                        month = state.selectedMonth,
                        rows = state.rows,
                        summary = state.summary,
                        settings = state.settings,
                    )
                }
                val app = getApplication<Application>()
                val uri = FileProvider.getUriForFile(
                    app,
                    "${app.packageName}.fileprovider",
                    file,
                )
                val mime = when (format) {
                    ExportFormat.PDF -> "application/pdf"
                    ExportFormat.JPG -> "image/jpeg"
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    val sheetLabels = SheetLabelFactory.of(state.settings.sheetLanguage)
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "${state.settings.resolvedSheetTitle()} ${sheetLabels.monthLabel(state.selectedMonth)}",
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val strings = Strings.of(state.settings.language)
                val chooser = Intent.createChooser(intent, strings.shareAs)
                val note = if (format == ExportFormat.JPG) strings.shareJpgSaved else null
                _shareEvents.emit(ShareEvent.Ready(chooser, note))
            } catch (e: Exception) {
                _shareEvents.emit(
                    ShareEvent.Failed(
                        e.message ?: Strings.of(state.settings.language).shareFailed,
                    ),
                )
            } finally {
                exporting.value = false
            }
        }
    }
}
