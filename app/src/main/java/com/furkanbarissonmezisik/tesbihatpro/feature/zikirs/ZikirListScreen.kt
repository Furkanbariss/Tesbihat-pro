package com.furkanbarissonmezisik.tesbihatpro.feature.zikirs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.furkanbarissonmezisik.tesbihatpro.R
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.Zikir
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.ZikirDraft
import com.furkanbarissonmezisik.tesbihatpro.core.ui.components.DetailTopBar
import com.furkanbarissonmezisik.tesbihatpro.core.ui.displayMeaning
import com.furkanbarissonmezisik.tesbihatpro.core.ui.displayTransliteration

@Composable
fun ZikirListRoute(
    onBack: () -> Unit,
    viewModel: ZikirsViewModel = viewModel(factory = ZikirsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()
    ZikirListScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectTesbihat = {
            viewModel.onSelectTesbihat()
            onBack()
        },
        onSelectZikir = { zikir ->
            viewModel.onSelect(zikir)
            onBack()
        },
        onAdd = viewModel::onAdd,
        onUpdate = viewModel::onUpdate,
        onDelete = viewModel::onDelete,
    )
}

@Composable
fun ZikirListScreen(
    uiState: ZikirsUiState,
    onBack: () -> Unit,
    onSelectTesbihat: () -> Unit,
    onSelectZikir: (Zikir) -> Unit,
    onAdd: (ZikirDraft) -> Unit,
    onUpdate: (Long, ZikirDraft) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            DetailTopBar(title = stringResource(R.string.screen_zikirs), onBack = onBack)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.zikirs_add),
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                TesbihatCard(summary = uiState.tesbihatSummary, onClick = onSelectTesbihat)
            }
            item { SectionTitle(stringResource(R.string.zikirs_section_presets)) }
            items(uiState.presets, key = { it.id }) { zikir ->
                ZikirRow(zikir = zikir, onClick = { onSelectZikir(zikir) })
            }
            if (uiState.own.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.zikirs_section_own)) }
                items(uiState.own, key = { it.id }) { zikir ->
                    ZikirRow(
                        zikir = zikir,
                        onClick = { onSelectZikir(zikir) },
                        onEdit = { editingId = zikir.id },
                        onDelete = { deletingId = zikir.id },
                    )
                }
            }
        }
    }

    if (adding) {
        ZikirEditDialog(
            initial = null,
            onConfirm = { draft ->
                onAdd(draft)
                adding = false
            },
            onDismiss = { adding = false },
        )
    }
    uiState.own.find { it.id == editingId }?.let { zikir ->
        ZikirEditDialog(
            initial = zikir,
            onConfirm = { draft ->
                onUpdate(zikir.id, draft)
                editingId = null
            },
            onDismiss = { editingId = null },
        )
    }
    uiState.own.find { it.id == deletingId }?.let { zikir ->
        DeleteDialog(
            name = zikir.displayTransliteration(),
            onConfirm = {
                onDelete(zikir.id)
                deletingId = null
            },
            onDismiss = { deletingId = null },
        )
    }
}

@Composable
private fun TesbihatCard(summary: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.zikirs_tesbihat_title)) },
            supportingContent = { Text(summary) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ZikirRow(
    zikir: Zikir,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        overlineContent = zikir.arabic?.let { arabic -> { Text(arabic) } },
        headlineContent = { Text(zikir.displayTransliteration()) },
        supportingContent = zikir.displayMeaning()?.let { meaning -> { Text(meaning) } },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    zikir.defaultTarget?.toString()
                        ?: stringResource(R.string.zikirs_target_unlimited),
                )
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.zikirs_edit),
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.zikirs_delete),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun DeleteDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.zikirs_delete_title)) },
        text = { Text(stringResource(R.string.zikirs_delete_message, name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
