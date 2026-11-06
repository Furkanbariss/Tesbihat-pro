package com.furkanbarissonmezisik.tesbihatpro.feature.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.furkanbarissonmezisik.tesbihatpro.TesbihApp
import com.furkanbarissonmezisik.tesbihatpro.core.content.PresetContent
import com.furkanbarissonmezisik.tesbihatpro.core.data.repository.SessionRepository
import com.furkanbarissonmezisik.tesbihatpro.core.domain.engine.CounterEngine
import com.furkanbarissonmezisik.tesbihatpro.core.domain.engine.CounterEvent
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.Zikir
import com.furkanbarissonmezisik.tesbihatpro.core.feedback.HapticsController
import com.furkanbarissonmezisik.tesbihatpro.core.feedback.SoundController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CounterUiState {
    data object Loading : CounterUiState

    data class Ready(
        val zikir: Zikir,
        val count: Int,
        /** null = sınırsız sayım */
        val target: Int?,
        val stepNumber: Int,
        val stepCount: Int,
        val completed: Boolean,
        val beadPosition: Int,
        val imames: List<Int>,
        val lastBead: Int?,
    ) : CounterUiState
}

class CounterViewModel(
    private val sessionRepository: SessionRepository,
    private val haptics: HapticsController,
    private val sound: SoundController,
) : ViewModel() {

    private val zikirsById = PresetContent.zikirs.associateBy { it.id }

    private val _uiState = MutableStateFlow<CounterUiState>(CounterUiState.Loading)
    val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

    /** Oturum yüklenene kadar null kalır; bu sürede dokunuş kabul edilmez. */
    private var counterState: CounterState? = null

    init {
        viewModelScope.launch {
            val restored = sessionRepository.load()?.takeIf { state ->
                state.steps.all { it.zikirId in zikirsById }
            }
            show(restored ?: CounterState(PresetContent.defaultTesbihat))
        }
    }

    fun onTap() = dispatch(CounterEvent.Tap)

    fun onUndo() = dispatch(CounterEvent.Undo)

    fun onResetStep() = dispatch(CounterEvent.ResetStep)

    fun onResetAll() = dispatch(CounterEvent.ResetAll)

    private fun dispatch(event: CounterEvent) {
        val current = counterState ?: return
        val transition = CounterEngine.reduce(current, event)
        transition.effect?.let { effect ->
            haptics.perform(effect)
            sound.play(effect)
        }
        if (transition.state != current) {
            show(transition.state)
            sessionRepository.save(transition.state)
        }
    }

    private fun show(state: CounterState) {
        counterState = state
        val step = state.steps[state.stepIndex]
        _uiState.value = CounterUiState.Ready(
            zikir = zikirsById.getValue(step.zikirId),
            count = state.count,
            target = step.target,
            stepNumber = state.stepIndex + 1,
            stepCount = state.steps.size,
            completed = state.completed,
            beadPosition = beadPosition(state),
            imames = imameBeads(state.steps),
            lastBead = lastBead(state.steps),
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TesbihApp).container
                CounterViewModel(
                    sessionRepository = container.sessionRepository,
                    haptics = container.hapticsController,
                    sound = container.soundController,
                )
            }
        }
    }
}
