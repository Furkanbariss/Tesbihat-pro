package com.furkanbarissonmezisik.tesbihatpro.feature.counter

import com.furkanbarissonmezisik.tesbihatpro.core.content.PresetContent
import com.furkanbarissonmezisik.tesbihatpro.core.data.repository.HistoryRepository
import com.furkanbarissonmezisik.tesbihatpro.core.data.repository.SessionRepository
import com.furkanbarissonmezisik.tesbihatpro.core.data.repository.SettingsRepository
import com.furkanbarissonmezisik.tesbihatpro.core.data.repository.ZikirRepository
import com.furkanbarissonmezisik.tesbihatpro.core.domain.engine.CounterEffect
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.AppSettings
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.CounterState
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.TesbihatStep
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.ZikirDraft
import com.furkanbarissonmezisik.tesbihatpro.core.feedback.HapticsController
import com.furkanbarissonmezisik.tesbihatpro.core.feedback.SoundController
import com.furkanbarissonmezisik.tesbihatpro.core.input.VolumeKeyDispatcher
import com.furkanbarissonmezisik.tesbihatpro.testing.FakeContentVersionStore
import com.furkanbarissonmezisik.tesbihatpro.testing.FakeDailyCountDao
import com.furkanbarissonmezisik.tesbihatpro.testing.FakeSessionStore
import com.furkanbarissonmezisik.tesbihatpro.testing.FakeSettingsStore
import com.furkanbarissonmezisik.tesbihatpro.testing.FakeZikirDao
import android.view.KeyEvent
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
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
    private val dao = FakeZikirDao()
    private val dailyDao = FakeDailyCountDao()
    private val settingsStore = FakeSettingsStore()
    private val volumeKeys = VolumeKeyDispatcher()
    private val day = "2027-02-08"
    private val haptics = RecordingHaptics()
    private val sound = RecordingSound()

    @Before
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private lateinit var repository: SessionRepository

    private fun TestScope.sessionRepository(): SessionRepository {
        repository = SessionRepository(store, backgroundScope, mainDispatcher)
        return repository
    }

    private fun TestScope.historyRepository() = HistoryRepository(
        dao = dailyDao,
        scope = backgroundScope,
        ioDispatcher = mainDispatcher,
        today = { LocalDate.parse(day) },
    )

    private fun TestScope.passWriteDelay() {
        advanceTimeBy(301)
        runCurrent()
    }

    private fun TestScope.createViewModel() = CounterViewModel(
        sessionRepository = sessionRepository(),
        zikirRepository = ZikirRepository(dao, FakeContentVersionStore(), backgroundScope),
        historyRepository = historyRepository(),
        settingsRepository = SettingsRepository(settingsStore, backgroundScope),
        volumeKeys = volumeKeys,
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
        runCurrent()

        assertEquals(5, viewModel.ready().count)
        assertTrue(haptics.effects.isEmpty())
    }

    @Test
    fun `without a saved session the default tesbihat starts`() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        runCurrent()

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
        runCurrent()

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
            runCurrent()

            assertEquals(PresetContent.SUBHANALLAH_ID, viewModel.ready().zikir.id)
            assertEquals(0, viewModel.ready().count)
        }

    @Test
    fun `tap updates the screen without waiting for the disk`() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onTap()

        assertEquals(1, viewModel.ready().count)
        assertTrue(store.writes.isEmpty())
    }

    @Test
    fun `tap triggers haptics and sound with the engine effect`() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onTap()

        assertEquals(listOf<CounterEffect>(CounterEffect.Tick), haptics.effects)
        assertEquals(listOf<CounterEffect>(CounterEffect.Tick), sound.effects)
    }

    @Test
    fun `reaching a step target shows the next zikir`() = runTest(mainDispatcher) {
        store.stored = CounterState(PresetContent.defaultTesbihat, count = 32)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onTap()

        assertEquals(PresetContent.ELHAMDULILLAH_ID, viewModel.ready().zikir.id)
        assertEquals(0, viewModel.ready().count)
        assertEquals(CounterEffect.StepCompleted(stepIndex = 0), haptics.effects.single())
    }

    @Test
    fun `bead position keeps moving forward across a step change`() = runTest(mainDispatcher) {
        store.stored = CounterState(PresetContent.defaultTesbihat, count = 32)
        val viewModel = createViewModel()
        runCurrent()
        assertEquals(32, viewModel.ready().beadPosition)

        viewModel.onTap()

        assertEquals(33, viewModel.ready().beadPosition)
        assertEquals(listOf(33, 66, 99, 100), viewModel.ready().imames)
        assertEquals(100, viewModel.ready().lastBead)
    }

    @Test
    fun `undo at a step boundary moves the beads back by one`() = runTest(mainDispatcher) {
        store.stored = CounterState(PresetContent.defaultTesbihat, stepIndex = 1)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onUndo()

        assertEquals(32, viewModel.ready().beadPosition)
    }

    @Test
    fun `tesbihat ends on the tehlil step`() = runTest(mainDispatcher) {
        store.stored = CounterState(PresetContent.defaultTesbihat, stepIndex = 3)
        val viewModel = createViewModel()
        runCurrent()

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
        runCurrent()

        viewModel.onTap()
        runCurrent()

        assertTrue(haptics.effects.isEmpty())
        assertTrue(sound.effects.isEmpty())
        assertTrue(store.writes.isEmpty())
    }

    @Test
    fun `undo decrements without feedback`() = runTest(mainDispatcher) {
        store.stored = CounterState(PresetContent.defaultTesbihat, count = 5)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onUndo()

        assertEquals(4, viewModel.ready().count)
        assertTrue(haptics.effects.isEmpty())
    }

    @Test
    fun `reset step clears only the current step`() = runTest(mainDispatcher) {
        store.stored = CounterState(PresetContent.defaultTesbihat, stepIndex = 1, count = 5)
        val viewModel = createViewModel()
        runCurrent()

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
        runCurrent()

        viewModel.onResetAll()

        assertEquals(1, viewModel.ready().stepNumber)
        assertEquals(0, viewModel.ready().count)
        assertFalse(viewModel.ready().completed)
    }

    @Test
    fun `taps reach the store after the write delay`() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        repeat(3) { viewModel.onTap() }
        advanceTimeBy(301)
        runCurrent()

        assertEquals(1, store.writes.size)
        assertEquals(3, store.writes.single().count)
    }


    private fun TestScope.ownZikirRepository() =
        ZikirRepository(dao, FakeContentVersionStore(), backgroundScope)

    @Test
    fun `selected zikir starts a new session`() = runTest(mainDispatcher) {
        store.stored = CounterState(PresetContent.defaultTesbihat, count = 5)
        val viewModel = createViewModel()
        runCurrent()

        repository.requestSteps(listOf(TesbihatStep(PresetContent.SALAVAT_ID, 100)))
        runCurrent()

        assertEquals(PresetContent.SALAVAT_ID, viewModel.ready().zikir.id)
        assertEquals(0, viewModel.ready().count)
        assertEquals(1, viewModel.ready().stepCount)
    }

    @Test
    fun `request is cleared once the counter takes it`() = runTest(mainDispatcher) {
        createViewModel()
        runCurrent()

        repository.requestSteps(listOf(TesbihatStep(PresetContent.SALAVAT_ID, 100)))
        runCurrent()

        assertEquals(null, repository.requestedSteps.value)
    }

    @Test
    fun `selecting the running zikir keeps its count`() = runTest(mainDispatcher) {
        val steps = listOf(TesbihatStep(PresetContent.SALAVAT_ID, 100))
        store.stored = CounterState(steps, count = 40)
        val viewModel = createViewModel()
        runCurrent()

        repository.requestSteps(steps)
        runCurrent()

        assertEquals(40, viewModel.ready().count)
    }

    @Test
    fun `selecting the tesbihat while it runs keeps its position`() = runTest(mainDispatcher) {
        store.stored = CounterState(PresetContent.defaultTesbihat, stepIndex = 2, count = 7)
        val viewModel = createViewModel()
        runCurrent()

        repository.requestSteps(PresetContent.defaultTesbihat)
        runCurrent()

        assertEquals(3, viewModel.ready().stepNumber)
        assertEquals(7, viewModel.ready().count)
    }

    @Test
    fun `selection made before the counter exists is not lost`() = runTest(mainDispatcher) {
        val session = sessionRepository()
        session.requestSteps(listOf(TesbihatStep(PresetContent.SALAVAT_ID, 100)))

        val viewModel = CounterViewModel(
            sessionRepository = session,
            zikirRepository = ownZikirRepository(),
            historyRepository = historyRepository(),
            settingsRepository = SettingsRepository(settingsStore, backgroundScope),
            volumeKeys = volumeKeys,
            haptics = haptics,
            sound = sound,
        )
        runCurrent()

        assertEquals(PresetContent.SALAVAT_ID, viewModel.ready().zikir.id)
    }

    @Test
    fun `new session is written to the store`() = runTest(mainDispatcher) {
        createViewModel()
        runCurrent()

        repository.requestSteps(listOf(TesbihatStep(PresetContent.SALAVAT_ID, 100)))
        runCurrent()
        advanceTimeBy(301)
        runCurrent()

        assertEquals(PresetContent.SALAVAT_ID, store.stored?.steps?.single()?.zikirId)
    }

    @Test
    fun `users own zikir is counted and restored`() = runTest(mainDispatcher) {
        val zikirs = ownZikirRepository()
        zikirs.zikirs.first()
        zikirs.add(ZikirDraft("Ya Latif", arabic = null, meaning = null, target = 129))
        val id = zikirs.zikirs.first().last().id
        store.stored = CounterState(listOf(TesbihatStep(id, 129)), count = 12)

        val viewModel = createViewModel()
        runCurrent()

        assertEquals("Ya Latif", viewModel.ready().zikir.transliterationTr)
        assertEquals(12, viewModel.ready().count)
        assertEquals(129, viewModel.ready().target)
    }

    @Test
    fun `editing the running zikir updates the screen and keeps the count`() =
        runTest(mainDispatcher) {
            val zikirs = ownZikirRepository()
            zikirs.zikirs.first()
            zikirs.add(ZikirDraft("Ya Latif", arabic = null, meaning = null, target = 129))
            val id = zikirs.zikirs.first().last().id
            store.stored = CounterState(listOf(TesbihatStep(id, 129)), count = 12)
            val viewModel = createViewModel()
            runCurrent()

            zikirs.update(id, ZikirDraft("Ya Hafiz", arabic = null, meaning = null, target = 129))
            runCurrent()

            assertEquals("Ya Hafiz", viewModel.ready().zikir.transliterationTr)
            assertEquals(12, viewModel.ready().count)
        }

    @Test
    fun `deleting the running zikir returns to the default tesbihat`() = runTest(mainDispatcher) {
        val zikirs = ownZikirRepository()
        zikirs.zikirs.first()
        zikirs.add(ZikirDraft("Ya Latif", arabic = null, meaning = null, target = 129))
        val id = zikirs.zikirs.first().last().id
        store.stored = CounterState(listOf(TesbihatStep(id, 129)), count = 12)
        val viewModel = createViewModel()
        runCurrent()

        zikirs.delete(id)
        runCurrent()

        assertEquals(PresetContent.SUBHANALLAH_ID, viewModel.ready().zikir.id)
        assertEquals(0, viewModel.ready().count)
        assertEquals(4, viewModel.ready().stepCount)
    }


    @Test
    fun `taps are added to the daily total`() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        repeat(3) { viewModel.onTap() }
        passWriteDelay()

        assertEquals(3, dailyDao.count(day, PresetContent.SUBHANALLAH_ID))
    }

    @Test
    fun `tap that finishes a step counts for the finished zikir`() = runTest(mainDispatcher) {
        store.stored = CounterState(PresetContent.defaultTesbihat, count = 32)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onTap()
        passWriteDelay()

        assertEquals(1, dailyDao.count(day, PresetContent.SUBHANALLAH_ID))
        assertEquals(null, dailyDao.count(day, PresetContent.ELHAMDULILLAH_ID))
    }

    @Test
    fun `whole tesbihat adds up to one hundred`() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        repeat(100) { viewModel.onTap() }
        passWriteDelay()

        assertEquals(33, dailyDao.count(day, PresetContent.SUBHANALLAH_ID))
        assertEquals(33, dailyDao.count(day, PresetContent.ELHAMDULILLAH_ID))
        assertEquals(33, dailyDao.count(day, PresetContent.ALLAHU_EKBER_ID))
        assertEquals(1, dailyDao.count(day, PresetContent.TEHLIL_ID))
    }

    @Test
    fun `undo lowers the daily total`() = runTest(mainDispatcher) {
        dailyDao.put(day, PresetContent.SUBHANALLAH_ID, 10)
        store.stored = CounterState(PresetContent.defaultTesbihat, count = 10)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onUndo()
        passWriteDelay()

        assertEquals(9, dailyDao.count(day, PresetContent.SUBHANALLAH_ID))
    }

    @Test
    fun `undo at a step boundary lowers the previous zikir`() = runTest(mainDispatcher) {
        dailyDao.put(day, PresetContent.SUBHANALLAH_ID, 33)
        store.stored = CounterState(PresetContent.defaultTesbihat, stepIndex = 1)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onUndo()
        passWriteDelay()

        assertEquals(32, dailyDao.count(day, PresetContent.SUBHANALLAH_ID))
    }

    @Test
    fun `tap while completed adds nothing to the daily total`() = runTest(mainDispatcher) {
        store.stored = CounterState(
            PresetContent.defaultTesbihat,
            stepIndex = 3,
            count = 1,
            completed = true,
        )
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onTap()
        passWriteDelay()

        assertEquals(null, dailyDao.count(day, PresetContent.TEHLIL_ID))
    }

    @Test
    fun `resets leave the daily total alone`() = runTest(mainDispatcher) {
        dailyDao.put(day, PresetContent.SUBHANALLAH_ID, 12)
        store.stored = CounterState(PresetContent.defaultTesbihat, count = 12)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onResetStep()
        viewModel.onResetAll()
        passWriteDelay()

        assertEquals(12, dailyDao.count(day, PresetContent.SUBHANALLAH_ID))
    }

    @Test
    fun `selecting another zikir leaves the daily total alone`() = runTest(mainDispatcher) {
        dailyDao.put(day, PresetContent.SUBHANALLAH_ID, 12)
        store.stored = CounterState(PresetContent.defaultTesbihat, count = 12)
        createViewModel()
        runCurrent()

        repository.requestSteps(listOf(TesbihatStep(PresetContent.SALAVAT_ID, 100)))
        runCurrent()
        passWriteDelay()

        assertEquals(12, dailyDao.count(day, PresetContent.SUBHANALLAH_ID))
        assertEquals(null, dailyDao.count(day, PresetContent.SALAVAT_ID))
    }


    @Test
    fun `sound off silences taps but keeps vibration`() = runTest(mainDispatcher) {
        settingsStore.update { it.copy(soundEnabled = false) }
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onTap()

        assertTrue(sound.effects.isEmpty())
        assertEquals(1, haptics.effects.size)
    }

    @Test
    fun `vibration off keeps sound`() = runTest(mainDispatcher) {
        settingsStore.update { it.copy(hapticsEnabled = false) }
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onTap()

        assertTrue(haptics.effects.isEmpty())
        assertEquals(1, sound.effects.size)
    }

    @Test
    fun `counting goes on with both turned off`() = runTest(mainDispatcher) {
        settingsStore.update { it.copy(soundEnabled = false, hapticsEnabled = false) }
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onTap()

        assertEquals(1, viewModel.ready().count)
    }

    @Test
    fun `setting changes apply to the next tap without restarting`() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onTap()

        settingsStore.update { it.copy(soundEnabled = false) }
        runCurrent()
        viewModel.onTap()

        assertEquals(1, sound.effects.size)
        assertEquals(2, haptics.effects.size)
    }

    @Test
    fun `volume up counts and volume down undoes once attached`() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.attachVolumeKeys()

        repeat(3) {
            volumeKeys.onKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, repeatCount = 0)
        }
        volumeKeys.onKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, repeatCount = 0)

        assertEquals(2, viewModel.ready().count)
    }

    @Test
    fun `detached counter leaves volume keys to the system`() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.attachVolumeKeys()
        viewModel.detachVolumeKeys()

        val consumed = volumeKeys.onKeyEvent(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.ACTION_DOWN,
            repeatCount = 0,
        )

        assertFalse(consumed)
        assertEquals(0, viewModel.ready().count)
    }

    @Test
    fun `settings reach the screen`() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        settingsStore.update { it.copy(keepScreenOn = false, beadStyleId = "dark_wood") }
        runCurrent()

        assertEquals(
            AppSettings(keepScreenOn = false, beadStyleId = "dark_wood"),
            viewModel.settings.value,
        )
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
