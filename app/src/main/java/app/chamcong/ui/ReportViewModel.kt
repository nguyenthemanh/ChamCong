package app.chamcong.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.chamcong.data.AttendanceRepository
import app.chamcong.data.CalendarFactory
import app.chamcong.data.DayEntry
import app.chamcong.data.MonthKey
import app.chamcong.data.MonthSummary
import app.chamcong.data.PaySettings
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReportUiState(
    val selectedMonth: MonthKey = MonthKey.current(),
    val months: List<MonthKey> = listOf(MonthKey.current()),
    val rows: List<app.chamcong.data.DayRow> = emptyList(),
    val summary: MonthSummary = MonthSummary(0, 0, 0, 0, 0, 0),
    val settings: PaySettings = PaySettings(),
    val showSettings: Boolean = false,
    val showShare: Boolean = false,
    val exporting: Boolean = false,
)

sealed interface ShareEvent {
    data class Ready(val intent: Intent) : ShareEvent
    data class Failed(val message: String) : ShareEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AttendanceRepository(app)

    private val selectedMonth = MutableStateFlow(MonthKey.current())
    private val showSettings = MutableStateFlow(false)
    private val showShare = MutableStateFlow(false)
    private val exporting = MutableStateFlow(false)

    private val _shareEvents = MutableSharedFlow<ShareEvent>(extraBufferCapacity = 1)
    val shareEvents = _shareEvents.asSharedFlow()

    private data class Core(
        val month: MonthKey,
        val months: List<MonthKey>,
        val entries: Map<Int, DayEntry>,
        val settings: PaySettings,
    )

    private data class Dialogs(
        val settingsOpen: Boolean,
        val shareOpen: Boolean,
        val exporting: Boolean,
    )

    val uiState: StateFlow<ReportUiState> = combine(
        combine(
            selectedMonth,
            repo.knownMonths,
            selectedMonth.flatMapLatest { repo.entriesFor(it) },
            repo.settings,
        ) { month, months, entries, settings ->
            Core(month, months, entries, settings)
        },
        combine(showSettings, showShare, exporting) { s, sh, e ->
            Dialogs(s, sh, e)
        },
    ) { core, dialogs ->
        val rows = CalendarFactory.buildRows(core.month, core.entries, core.settings.sheetLanguage)
        ReportUiState(
            selectedMonth = core.month,
            months = core.months.ifEmpty { listOf(core.month) },
            rows = rows,
            summary = CalendarFactory.summarize(rows, core.settings),
            settings = core.settings,
            showSettings = dialogs.settingsOpen,
            showShare = dialogs.shareOpen,
            exporting = dialogs.exporting,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportUiState(),
    )

    init {
        viewModelScope.launch {
            repo.ensureMonth(MonthKey.current())
        }
    }

    fun selectMonth(month: MonthKey) {
        selectedMonth.value = month
        viewModelScope.launch { repo.ensureMonth(month) }
    }

    fun prevMonth() = shiftMonth(false)

    fun nextMonth() = shiftMonth(true)

    /** Creates the next real calendar month after the newest known tab and opens it. */
    fun createNewMonth() {
        viewModelScope.launch {
            val newest = uiState.value.months.maxOrNull() ?: selectedMonth.value
            val key = repo.createNextMonth(newest)
            selectedMonth.value = key
        }
    }

    private fun shiftMonth(next: Boolean) {
        viewModelScope.launch {
            val key = repo.addAdjacentMonth(selectedMonth.value, next)
            selectedMonth.value = key
        }
    }

    fun updateDay(day: Int, site: String, hasFlag: Boolean) {
        val month = selectedMonth.value
        viewModelScope.launch {
            repo.updateEntry(month, DayEntry(day = day, site = site, hasFlag = hasFlag))
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
            showSettings.value = false
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
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "月報報告書 ${state.selectedMonth.labelJa()}",
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(
                    intent,
                    Strings.of(state.settings.language).shareAs,
                )
                _shareEvents.emit(ShareEvent.Ready(chooser))
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
