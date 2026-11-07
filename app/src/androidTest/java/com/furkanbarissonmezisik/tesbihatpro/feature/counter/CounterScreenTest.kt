package com.furkanbarissonmezisik.tesbihatpro.feature.counter

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.furkanbarissonmezisik.tesbihatpro.R
import com.furkanbarissonmezisik.tesbihatpro.core.content.PresetContent
import com.furkanbarissonmezisik.tesbihatpro.core.ui.BeadStyles
import com.furkanbarissonmezisik.tesbihatpro.core.ui.theme.TesbihatProTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CounterScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var taps = 0

    private val tapArea
        get() = composeRule.onNodeWithContentDescription(
            context.getString(R.string.counter_tap_label),
        )

    @Before
    fun setUp() {
        composeRule.setContent {
            TesbihatProTheme {
                CounterScreen(
                    uiState = CounterUiState.Ready(
                        zikir = PresetContent.zikirs.first(),
                        count = 0,
                        target = 33,
                        stepNumber = 1,
                        stepCount = 4,
                        completed = false,
                        beadPosition = 0,
                        imames = listOf(33, 66, 99, 100),
                        lastBead = 100,
                    ),
                    beadStyle = BeadStyles.Amber,
                    onTap = { taps++ },
                    onUndo = {},
                    onResetStep = {},
                    onResetAll = {},
                    onDim = {},
                    onOpenZikirs = {},
                    onOpenHistory = {},
                    onOpenSettings = {},
                )
            }
        }
    }

    @Test
    fun tapCountsOnce() {
        tapArea.performTouchInput {
            down(center)
            up()
        }

        assertEquals(1, taps)
    }

    @Test
    fun countingDoesNotWaitForTheFingerToLift() {
        tapArea.performTouchInput { down(center) }

        assertEquals(1, taps)
    }

    @Test
    fun longSwipeDownCountsOnce() {
        tapArea.performTouchInput { swipeDown(startY = top, endY = bottom) }

        assertEquals(1, taps)
    }

    @Test
    fun slowSwipeDownCountsOnce() {
        tapArea.performTouchInput {
            swipeDown(startY = top, endY = bottom, durationMillis = 2_000)
        }

        assertEquals(1, taps)
    }

    @Test
    fun swipeUpCountsOnce() {
        tapArea.performTouchInput { swipeUp(startY = bottom, endY = top) }

        assertEquals(1, taps)
    }

    @Test
    fun threeSwipesCountThree() {
        repeat(3) {
            tapArea.performTouchInput { swipeDown(startY = top, endY = bottom) }
        }

        assertEquals(3, taps)
    }

    @Test
    fun overlappingFingersCountOnce() {
        tapArea.performTouchInput {
            down(pointerId = 1, position = center)
            advanceEventTime(50)
            down(pointerId = 2, position = centerLeft)
            advanceEventTime(50)
            up(pointerId = 1)
            up(pointerId = 2)
        }

        assertEquals(1, taps)
    }

    @Test
    fun threeFingersAtOnceCountOnce() {
        tapArea.performTouchInput {
            down(pointerId = 1, position = center - Offset(60f, 0f))
            advanceEventTime(15)
            down(pointerId = 2, position = center)
            advanceEventTime(15)
            down(pointerId = 3, position = center + Offset(60f, 0f))
            advanceEventTime(100)
            up(pointerId = 1)
            up(pointerId = 2)
            up(pointerId = 3)
        }

        assertEquals(1, taps)
    }

    @Test
    fun fingerLandingWhileAnotherRestsDoesNotCount() {
        tapArea.performTouchInput {
            down(pointerId = 1, position = center)
            advanceEventTime(50)
            down(pointerId = 2, position = centerLeft)
            up(pointerId = 2)
            advanceEventTime(200)
            down(pointerId = 2, position = centerLeft)
            up(pointerId = 2)
        }

        assertEquals(1, taps)
    }

    @Test
    fun nextTapCountsOnceEveryFingerHasLifted() {
        tapArea.performTouchInput {
            down(pointerId = 1, position = center)
            down(pointerId = 2, position = centerLeft)
            up(pointerId = 1)
            up(pointerId = 2)
        }
        tapArea.performTouchInput {
            down(center)
            up()
        }

        assertEquals(2, taps)
    }
}
