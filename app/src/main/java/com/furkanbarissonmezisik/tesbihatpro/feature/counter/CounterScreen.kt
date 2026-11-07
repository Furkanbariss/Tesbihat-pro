package com.furkanbarissonmezisik.tesbihatpro.feature.counter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.furkanbarissonmezisik.tesbihatpro.R
import com.furkanbarissonmezisik.tesbihatpro.core.ui.BeadStyle
import com.furkanbarissonmezisik.tesbihatpro.core.ui.BeadStyles
import com.furkanbarissonmezisik.tesbihatpro.core.ui.components.DimWindowEffect
import com.furkanbarissonmezisik.tesbihatpro.core.ui.components.KeepScreenOnEffect
import com.furkanbarissonmezisik.tesbihatpro.core.ui.displayTransliteration

/** Bu yüksekliğin altında (yatay telefon) metin ve sayı yan yana dizilir. */
private val COMPACT_HEIGHT = 480.dp

private const val VISIBLE_BEADS = 9
private val MIN_BEAD_SPACING = 32.dp
private val MAX_BEAD_SPACING = 84.dp

@Composable
fun CounterRoute(
    onOpenZikirs: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: CounterViewModel = viewModel(factory = CounterViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var dimmed by rememberSaveable { mutableStateOf(false) }

    // Karartma ayardan bağımsız olarak ekranı açık tutar; kapanırsa sayım yarıda kalır.
    KeepScreenOnEffect(enabled = settings.keepScreenOn || dimmed)
    DisposableEffect(viewModel, settings.volumeKeysEnabled) {
        if (settings.volumeKeysEnabled) viewModel.attachVolumeKeys()
        onDispose { viewModel.detachVolumeKeys() }
    }
    if (dimmed) {
        DimWindowEffect()
        BackHandler { dimmed = false }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CounterScreen(
            uiState = uiState,
            beadStyle = BeadStyles.find(settings.beadStyleId),
            onTap = viewModel::onTap,
            onUndo = viewModel::onUndo,
            onResetStep = viewModel::onResetStep,
            onResetAll = viewModel::onResetAll,
            onDim = { dimmed = true },
            onOpenZikirs = onOpenZikirs,
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
        )
        if (dimmed) {
            DimOverlay(onTap = viewModel::onTap, onExit = { dimmed = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    uiState: CounterUiState,
    beadStyle: BeadStyle,
    onTap: () -> Unit,
    onUndo: () -> Unit,
    onResetStep: () -> Unit,
    onResetAll: () -> Unit,
    onDim: () -> Unit,
    onOpenZikirs: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenZikirs) {
                        Icon(
                            painter = painterResource(R.drawable.ic_zikir_list),
                            contentDescription = stringResource(R.string.screen_zikirs),
                        )
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(
                            painter = painterResource(R.drawable.ic_history),
                            contentDescription = stringResource(R.string.screen_history),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.screen_settings),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState is CounterUiState.Ready) {
                CounterContent(
                    state = uiState,
                    beadStyle = beadStyle,
                    onTap = onTap,
                    onUndo = onUndo,
                    onResetStep = onResetStep,
                    onResetAll = onResetAll,
                    onDim = onDim,
                )
            }
        }
    }
}

@Composable
private fun CounterContent(
    state: CounterUiState.Ready,
    beadStyle: BeadStyle,
    onTap: () -> Unit,
    onUndo: () -> Unit,
    onResetStep: () -> Unit,
    onResetAll: () -> Unit,
    onDim: () -> Unit,
) {
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    // Düğmeler dokunma katmanının kardeşi olarak üstte durur, böylece sayıma karışmaz.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxHeight < COMPACT_HEIGHT
        val beadSpacing = (maxHeight / VISIBLE_BEADS).coerceIn(MIN_BEAD_SPACING, MAX_BEAD_SPACING)
        val beadStripWidth = beadSpacing * 1.35f
        TapArea(onTap = onTap, modifier = Modifier.matchParentSize())
        TesbihBeads(
            position = state.beadPosition,
            imames = state.imames,
            lastBead = state.lastBead,
            style = beadStyle,
            spacing = beadSpacing,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(beadStripWidth)
                .fillMaxHeight(),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 24.dp,
                    end = beadStripWidth + 8.dp,
                    top = if (compact) 8.dp else 16.dp,
                    bottom = if (compact) 8.dp else 16.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.stepCount > 1) {
                Text(
                    text = stringResource(
                        R.string.counter_step_indicator,
                        state.stepNumber,
                        state.stepCount,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Kalan yüksekliği bu alan alır; düğmeler ekranın dışına taşmaz.
            CounterDisplay(
                state = state,
                compact = compact,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            if (state.completed) {
                CompletedPanel(
                    isSequence = state.stepCount > 1,
                    compact = compact,
                    onRestart = onResetAll,
                )
                Spacer(modifier = Modifier.height(if (compact) 8.dp else 16.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FilledTonalIconButton(onClick = onUndo) {
                    Icon(
                        painter = painterResource(R.drawable.ic_undo),
                        contentDescription = stringResource(R.string.action_undo),
                    )
                }
                FilledTonalIconButton(onClick = onDim) {
                    Icon(
                        painter = painterResource(R.drawable.ic_dim),
                        contentDescription = stringResource(R.string.action_dim),
                    )
                }
                FilledTonalIconButton(onClick = { showResetDialog = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_reset),
                        contentDescription = stringResource(R.string.action_reset),
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        ResetDialog(
            isSequence = state.stepCount > 1,
            onResetStep = {
                showResetDialog = false
                onResetStep()
            },
            onResetAll = {
                showResetDialog = false
                onResetAll()
            },
            onDismiss = { showResetDialog = false },
        )
    }
}

/**
 * Her parmak inişi hemen bir sayımdır, dokunuş parmak kalkmasını beklemez. Kaydırma ne kadar uzun
 * olursa olsun tek sayımdır; iki başparmakla sırayla basmak ise her basışta sayılır.
 */
@Composable
private fun TapArea(onTap: () -> Unit, modifier: Modifier = Modifier) {
    val currentOnTap by rememberUpdatedState(onTap)
    val label = stringResource(R.string.counter_tap_label)
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { change ->
                            if (change.changedToDown()) currentOnTap()
                        }
                    }
                }
            }
            .semantics {
                contentDescription = label
                onClick(label = label) {
                    currentOnTap()
                    true
                }
            },
    )
}

@Composable
private fun CounterDisplay(
    state: CounterUiState.Ready,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val transliteration = state.zikir.displayTransliteration()
    val description = if (state.target != null) {
        stringResource(
            R.string.counter_accessibility_target,
            transliteration,
            state.count,
            state.target,
        )
    } else {
        stringResource(R.string.counter_accessibility_unlimited, transliteration, state.count)
    }
    val displayModifier = modifier.clearAndSetSemantics {
        contentDescription = description
        liveRegion = LiveRegionMode.Polite
    }
    if (compact) {
        Row(
            modifier = displayModifier,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ZikirLines(arabic = state.zikir.arabic, transliteration = transliteration)
            }
            CountLines(count = state.count, target = state.target, fontSize = 72.sp)
        }
    } else {
        Column(
            modifier = displayModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ZikirLines(arabic = state.zikir.arabic, transliteration = transliteration)
            Spacer(modifier = Modifier.height(24.dp))
            CountLines(count = state.count, target = state.target, fontSize = 96.sp)
        }
    }
}

/** Yer yetmezse metin kırpılmaz, küçülür; sayı her zaman tam boyda kalır. */
@Composable
private fun ColumnScope.ZikirLines(arabic: String?, transliteration: String) {
    if (arabic != null) {
        Text(
            text = arabic,
            modifier = Modifier.weight(1.2f, fill = false),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 12.sp,
                maxFontSize = MaterialTheme.typography.headlineMedium.fontSize,
            ),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
    Text(
        text = transliteration,
        modifier = Modifier.weight(1f, fill = false),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        autoSize = TextAutoSize.StepBased(
            minFontSize = 10.sp,
            maxFontSize = MaterialTheme.typography.titleMedium.fontSize,
        ),
    )
}

@Composable
private fun CountLines(count: Int, target: Int?, fontSize: TextUnit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = fontSize,
                lineHeight = fontSize * 1.08f,
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 36.sp,
                maxFontSize = fontSize,
                stepSize = 12.sp,
            ),
        )
        if (target != null) {
            Text(
                text = stringResource(R.string.counter_target, target),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompletedPanel(isSequence: Boolean, compact: Boolean, onRestart: () -> Unit) {
    val message = stringResource(
        if (isSequence) R.string.counter_completed_tesbihat else R.string.counter_completed,
    )
    if (compact) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = message, style = MaterialTheme.typography.titleMedium)
            Button(onClick = onRestart) {
                Text(stringResource(R.string.action_restart))
            }
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRestart) {
                Text(stringResource(R.string.action_restart))
            }
        }
    }
}

@Composable
private fun ResetDialog(
    isSequence: Boolean,
    onResetStep: () -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reset_dialog_title)) },
        text = { Text(stringResource(R.string.reset_dialog_message)) },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onResetStep) {
                    Text(
                        stringResource(
                            if (isSequence) R.string.reset_dialog_step else R.string.action_reset,
                        ),
                    )
                }
                if (isSequence) {
                    TextButton(onClick = onResetAll) {
                        Text(stringResource(R.string.reset_dialog_all))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
    )
}
