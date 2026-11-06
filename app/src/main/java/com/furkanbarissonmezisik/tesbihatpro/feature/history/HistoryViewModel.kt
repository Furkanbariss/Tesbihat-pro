package com.furkanbarissonmezisik.tesbihatpro.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.furkanbarissonmezisik.tesbihatpro.TesbihApp
import com.furkanbarissonmezisik.tesbihatpro.core.ads.AdsController
import com.furkanbarissonmezisik.tesbihatpro.core.data.repository.HistoryRepository
import com.furkanbarissonmezisik.tesbihatpro.core.data.repository.ZikirRepository
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.DailyCount
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.Zikir
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HistoryEntry(
    /** null = zikir sonradan silinmiş */
    val zikir: Zikir?,
    val count: Int,
)

data class HistoryDay(
    val date: LocalDate,
    val total: Int,
    val entries: List<HistoryEntry>,
)

data class HistoryUiState(
    val todayTotal: Int = 0,
    val todayEntries: List<HistoryEntry> = emptyList(),
    /** Bugün hariç, yeniden eskiye. */
    val pastDays: List<HistoryDay> = emptyList(),
)

internal fun historyUiState(
    counts: List<DailyCount>,
    zikirs: List<Zikir>,
    today: LocalDate,
): HistoryUiState {
    val zikirsById = zikirs.associateBy { it.id }
    val days = counts
        .groupBy { it.date }
        .map { (date, dayCounts) ->
            HistoryDay(
                date = date,
                total = dayCounts.sumOf { it.count },
                entries = dayCounts
                    .sortedByDescending { it.count }
                    .map { HistoryEntry(zikirsById[it.zikirId], it.count) },
            )
        }
        .sortedByDescending { it.date }
    val todayDay = days.find { it.date == today }
    return HistoryUiState(
        todayTotal = todayDay?.total ?: 0,
        todayEntries = todayDay?.entries.orEmpty(),
        pastDays = days.filterNot { it.date == today },
    )
}

class HistoryViewModel(
    historyRepository: HistoryRepository,
    zikirRepository: ZikirRepository,
    val adsController: AdsController,
    today: LocalDate = LocalDate.now(),
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        historyRepository.observeSince(today.minusDays(DAYS_SHOWN - 1)),
        zikirRepository.zikirs,
    ) { counts, zikirs ->
        historyUiState(counts, zikirs, today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HistoryUiState())

    companion object {
        private const val DAYS_SHOWN = 30L
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TesbihApp).container
                HistoryViewModel(
                    historyRepository = container.historyRepository,
                    zikirRepository = container.zikirRepository,
                    adsController = container.adsController,
                )
            }
        }
    }
}
