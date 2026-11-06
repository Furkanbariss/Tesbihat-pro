package com.furkanbarissonmezisik.tesbihatpro.feature.counter

import com.furkanbarissonmezisik.tesbihatpro.core.content.PresetContent
import com.furkanbarissonmezisik.tesbihatpro.core.data.repository.SessionRepository
import com.furkanbarissonmezisik.tesbihatpro.core.domain.engine.CounterEffect
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.TesbihatStep
import com.furkanbarissonmezisik.tesbihatpro.core.feedback.HapticsController
import com.furkanbarissonmezisik.tesbihatpro.core.feedback.SoundController
import com.furkanbarissonmezisik.tesbihatpro.testing.FakeSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterViewModelTest {

    private val mainDispatcher: TestDispatcher = StandardTestDispatcher()
    private val store = FakeSessionStore()
    private val haptics = RecordingHaptics()
    private val sound = RecordingSound()

    @Before
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.createViewModel() = CounterViewModel(
        sessionRepository = SessionRepository(store, backgroundScope, mainDispatcher),
        haptics = haptics,
        sound = sound,
    )

    private fun CounterViewModel.ready() = uiState.value as CounterUiState.Ready

    @Test
    fun `starts loading until the session is read`() = runTest(mainDispatcher) {
        val viewModel = createViewModel()

        assertEquals(CounterUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `taps before the session is loaded are ignored`() = runTest(mainDispatcher) {
        store.stored = CounterState(PresetContent.defaultTesbihat, count = 5)
        val viewModel = createViewModel()

        viewModel.onTap()
        advanceUntilIdle()

        assertEquals(5, viewModel.ready().count)
        assertTrue(haptics.effects.isEmpty())
    }

    @Test
    fun `without a saved session the default tesbihat starts`() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.ready()
        assertEquals(PresetContent.SUBHANALLAH_ID, state.zikir.id)
        assertEquals(0, state.count)
        assertEquals(33, state.target)
        assertEquals(1, state.stepNumber)
        assertEquals(4, state.stepCount)
    }

    @Test
    fun `saved session is restored at the same step and count`() = runTest(mainDispatcher) {
        store.stored = CounterState(PresetContent.defaultTesbihat, stepIndex = 1, count = 12)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.ready()
        assertEquals(PresetContent.ELHAMDULILLAH_ID, state.zikir.id)
        assertEquals(12, state.count)
        assertEquals(2, state.stepNumber)
    }

    @Test
    fun `session with an unknown zikir falls back to the default tesbihat`() =
        runTest(mainDispatcher) {
            store.stored = CounterState(listOf(TesbihatStep(zikirId = 9999, target = 33)), count = 3)

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(PresetContent.SUBHANALLAH_ID, viewModel.ready().zikir.id)
            assertEquals(0, viewModel.ready().count)
        }

    @Test
    fun `tap updates the screen without waiting for the disk`() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onTap()

        assertEquals(1, viewModel.ready().count)
        assertTrue(store.writes.isEmpty())
    }

    @Test
    fun `tap triggers haptics and sound with the engine effect`() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onTap()

        assertEquals(listOf<CounterEffect>(CounterEffect.Tick), haptics.effects)
        assertEquals(listOf<CounterEffect>(CounterEffect.Tick), sound.effects)
    }

    @Test
    fun `reaching a step target shows the next zikir`() = runTest(mainDispatcher) {
        store.stored = CounterState(PresetContent.defaultTesbihat, count = 32)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onTap()

        assertEquals(PresetContent.ELHAMDULILLAH_ID, viewModel.ready().zikir.id)
        assertEquals(0, viewModel.ready().count)
        assertEquals(CounterEffect.StepCompleted(stepIndex = 0), haptics.effects.single())
    }

    @Test
    fun `tesbihat ends on the tehlil step`() = runTest(mainDispatcher) {
        store.stored = CounterState(PresetContent.defaultTesbihat, stepIndex = 3)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onTap()

        assertEquals(PresetContent.TEHLIL_ID, viewModel.ready().zikir.id)
        assertTrue(viewModel.ready().completed)
        assertEquals(CounterEffect.SequenceCompleted, haptics.effects.single())
    }

    @Test
    fun `tap while completed gives no feedback and saves nothing`() = runTest(mainDispatcher) {
        store.stored = CounterState(
            PresetContent.defaultTesbihat,
            stepIndex = 3,
            count = 1,
            completed = true,
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onTap()
        advanceUntilIdle()

        assertTrue(haptics.effects.isEmpty())
        assertTrue(sound.effects.isEmpty())
        assertTrue(store.writes.isEmpty())
    }

    @Test
    fun `undo decrements without feedback`() = runTest(mainDispatcher) {
        store.stored = CounterState(PresetContent.defaultTesbihat, count = 5)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onUndo()

        assertEquals(4, viewModel.ready().count)
        assertTrue(haptics.effects.isEmpty())
    }

    @Test
    fun `reset step clears only the current step`() = runTest(mainDispatcher) {
        store.stored = CounterState(PresetContent.defaultTesbihat, stepIndex = 1, count = 5)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onResetStep()

        assertEquals(0, viewModel.ready().count)
        assertEquals(2, viewModel.ready().stepNumber)
    }

    @Test
    fun `reset all returns to the first step and clears completed`() = runTest(mainDispatcher) {
        store.stored = CounterState(
            PresetContent.defaultTesbihat,
            stepIndex = 3,
            count = 1,
            completed = true,
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onResetAll()

        assertEquals(1, viewModel.ready().stepNumber)
        assertEquals(0, viewModel.ready().count)
        assertFalse(viewModel.ready().completed)
    }

    @Test
    fun `taps reach the store after the write delay`() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        repeat(3) { viewModel.onTap() }
        advanceTimeBy(301)
        runCurrent()

        assertEquals(1, store.writes.size)
        assertEquals(3, store.writes.single().count)
    }

    private class RecordingHaptics : HapticsController {
        val effects = mutableListOf<CounterEffect>()

        override fun perform(effect: CounterEffect) {
            effects += effect
        }
    }

    private class RecordingSound : SoundController {
        val effects = mutableListOf<CounterEffect>()

        override fun play(effect: CounterEffect) {
            effects += effect
        }
    }
}
