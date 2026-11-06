package com.furkanbarissonmezisik.tesbihatpro.feature.counter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import com.furkanbarissonmezisik.tesbihatpro.R
import kotlinx.coroutines.delay

private const val EXIT_HOLD_MILLIS = 1_000L
private const val HINT_MILLIS = 3_000L

/**
 * Normal ekranda dokunuş parmak iner inmez sayılır; burada çıkış uzun basışla yapıldığı için sayım
 * parmak kalkınca yapılır, yoksa çıkmak için yapılan her basış bir sayım eklerdi.
 */
internal class DimPressTracker(private val exitHoldMillis: Long) {
    private val pressedAt = mutableMapOf<Long, Long>()

    fun press(pointer: Long, time: Long) {
        pressedAt[pointer] = time
    }

    /** Bırakılan basış sayılacaksa true döner. */
    fun release(pointer: Long, time: Long): Boolean {
        val start = pressedAt.remove(pointer) ?: return false
        return time - start < exitHoldMillis
    }

    /** Basılı parmak yoksa null döner. */
    fun millisUntilExit(now: Long): Long? =
        pressedAt.values.minOrNull()?.let { (it + exitHoldMillis - now).coerceAtLeast(0) }

    fun reset() = pressedAt.clear()
}

@Composable
internal fun DimOverlay(
    onTap: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnExit by rememberUpdatedState(onExit)
    val description = stringResource(R.string.dim_accessibility)
    val tapLabel = stringResource(R.string.counter_tap_label)
    val exitLabel = stringResource(R.string.dim_exit_label)

    var showHint by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(HINT_MILLIS)
        showHint = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                val tracker = DimPressTracker(EXIT_HOLD_MILLIS)
                var now = 0L
                awaitPointerEventScope {
                    while (true) {
                        val wait = tracker.millisUntilExit(now)
                        val event = if (wait == null) {
                            awaitPointerEvent()
                        } else {
                            withTimeoutOrNull(wait) { awaitPointerEvent() }
                        }
                        if (event == null) {
                            tracker.reset()
                            currentOnExit()
                            continue
                        }
                        event.changes.forEach { change ->
                            now = change.uptimeMillis
                            if (change.changedToDown()) {
                                tracker.press(change.id.value, change.uptimeMillis)
                            } else if (change.changedToUpIgnoreConsumed()) {
                                if (tracker.release(change.id.value, change.uptimeMillis)) {
                                    currentOnTap()
                                }
                            }
                        }
                    }
                }
            }
            .semantics {
                contentDescription = description
                onClick(label = tapLabel) {
                    currentOnTap()
                    true
                }
                onLongClick(label = exitLabel) {
                    currentOnExit()
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (showHint) {
            Text(
                text = stringResource(R.string.dim_exit_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
            )
        }
    }
}
