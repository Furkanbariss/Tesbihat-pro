package com.furkanbarissonmezisik.tesbihatpro.feature.counter

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.furkanbarissonmezisik.tesbihatpro.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DimOverlayTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private var taps = 0
    private var exits = 0

    private fun show() {
        composeRule.setContent {
            DimOverlay(
                onTap = { taps++ },
                onExit = { exits++ },
                modifier = Modifier.testTag("dim"),
            )
        }
    }

    private fun press(heldMillis: Long) {
        composeRule.onNodeWithTag("dim").performTouchInput {
            down(center)
            advanceEventTime(heldMillis)
        }
        composeRule.mainClock.advanceTimeBy(heldMillis)
        composeRule.onNodeWithTag("dim").performTouchInput { up() }
        composeRule.waitForIdle()
    }

    @Test
    fun shortPressCountsOnce() {
        show()

        press(heldMillis = 100)

        assertEquals(1, taps)
        assertEquals(0, exits)
    }

    @Test
    fun pressUnderOneSecondStillCounts() {
        show()

        press(heldMillis = 800)

        assertEquals(1, taps)
        assertEquals(0, exits)
    }

    @Test
    fun holdingForASecondExitsWithoutCounting() {
        show()

        press(heldMillis = 1_200)

        assertEquals(0, taps)
        assertEquals(1, exits)
    }

    @Test
    fun overlappingFingersEachCount() {
        show()

        composeRule.onNodeWithTag("dim").performTouchInput {
            down(pointerId = 1, position = center)
            advanceEventTime(50)
            down(pointerId = 2, position = centerLeft)
            advanceEventTime(50)
            up(pointerId = 1)
            advanceEventTime(50)
            up(pointerId = 2)
        }
        composeRule.waitForIdle()

        assertEquals(2, taps)
    }

    @Test
    fun exitHintDisappearsAfterAFewSeconds() {
        val hint = context.getString(R.string.dim_exit_hint)
        composeRule.mainClock.autoAdvance = false
        show()
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithText(hint).assertExists()

        composeRule.mainClock.advanceTimeBy(3_100)
        composeRule.onNodeWithText(hint).assertDoesNotExist()
    }
}
