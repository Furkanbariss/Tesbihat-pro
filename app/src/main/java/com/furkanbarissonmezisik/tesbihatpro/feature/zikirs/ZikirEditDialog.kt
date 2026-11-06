package com.furkanbarissonmezisik.tesbihatpro.feature.zikirs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.furkanbarissonmezisik.tesbihatpro.R
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.Zikir
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.ZikirDraft

/** [initial] null ise yeni zikir eklenir. */
@Composable
internal fun ZikirEditDialog(
    initial: Zikir?,
    onConfirm: (ZikirDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.transliterationTr.orEmpty()) }
    var arabic by rememberSaveable { mutableStateOf(initial?.arabic.orEmpty()) }
    var meaning by rememberSaveable { mutableStateOf(initial?.meaningTr.orEmpty()) }
    var target by rememberSaveable {
        mutableStateOf(initial?.defaultTarget?.toString().orEmpty())
    }
    val draft = ZikirForm(name, arabic, meaning, target).toDraft()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initial == null) R.string.zikirs_add else R.string.zikirs_edit))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.zikirs_field_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                )
                OutlinedTextField(
                    value = arabic,
                    onValueChange = { arabic = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.zikirs_field_arabic)) },
                )
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.zikirs_field_meaning)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { input ->
                        target = input.filter(Char::isDigit).take(ZikirForm.MAX_TARGET_DIGITS)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.zikirs_field_target)) },
                    supportingText = { Text(stringResource(R.string.zikirs_field_target_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { draft?.let(onConfirm) },
                enabled = draft != null,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
