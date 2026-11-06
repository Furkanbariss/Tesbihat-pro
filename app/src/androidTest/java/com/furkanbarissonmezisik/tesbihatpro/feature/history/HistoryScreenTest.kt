package com.furkanbarissonmezisik.tesbihatpro.feature.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.furkanbarissonmezisik.tesbihatpro.R
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.Zikir
import com.furkanbarissonmezisik.tesbihatpro.core.ui.theme.TesbihatProTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val zikir = Zikir(
        id = 1000,
        arabic = null,
        transliterationTr = "Ya Latif",
        transliterationEn = "Ya Latif",
        meaningTr = null,
        meaningEn = null,
        defaultTarget = 129,
        isPreset = false,
    )

    private fun show(state: HistoryUiState, onBack: () -> Unit = {}) {
        composeRule.setContent {
            TesbihatProTheme {
                HistoryScreen(uiState = state, onBack = onBack)
            }
        }
    }

    private fun longDate(date: LocalDate): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            .withLocale(context.resources.configuration.locales[0])
            .format(date)

    @Test
    fun emptyDayExplainsThatNothingWasCounted() {
        show(HistoryUiState())

        composeRule.onNodeWithText(context.getString(R.string.history_empty)).assertIsDisplayed()
        composeRule.onNodeWithText("0").assertIsDisplayed()
    }

    @Test
    fun todayShowsTheTotalAndEachZikir() {
        show(
            HistoryUiState(
                todayTotal = 142,
                todayEntries = listOf(HistoryEntry(zikir, 129), HistoryEntry(null, 13)),
            ),
        )

        composeRule.onNodeWithText("142").assertIsDisplayed()
        composeRule.onNodeWithText("Ya Latif").assertIsDisplayed()
        composeRule.onNodeWithText("129").assertIsDisplayed()
    }

    @Test
    fun deletedZikirIsNamedAsDeleted() {
        show(HistoryUiState(todayTotal = 13, todayEntries = listOf(HistoryEntry(null, 13))))

        composeRule.onNodeWithText(context.getString(R.string.history_deleted_zikir))
            .assertIsDisplayed()
    }

    @Test
    fun pastDaysShowTheirDateAndTotal() {
        val date = LocalDate.of(2027, 2, 7)
        show(
            HistoryUiState(
                pastDays = listOf(
                    HistoryDay(date, total = 261, entries = listOf(HistoryEntry(zikir, 261))),
                ),
            ),
        )

        composeRule.onNodeWithText(context.getString(R.string.history_last_30_days))
            .assertIsDisplayed()
        composeRule.onNodeWithText(longDate(date)).assertIsDisplayed()
    }

    @Test
    fun pastSectionIsHiddenWithoutPastDays() {
        show(HistoryUiState())

        composeRule.onNodeWithText(context.getString(R.string.history_last_30_days))
            .assertDoesNotExist()
    }

    @Test
    fun backButtonLeavesTheScreen() {
        var wentBack = false
        show(HistoryUiState(), onBack = { wentBack = true })

        composeRule.onNodeWithContentDescription(context.getString(R.string.action_back))
            .performClick()

        assertTrue(wentBack)
    }
}
