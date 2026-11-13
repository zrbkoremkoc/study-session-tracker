package com.studysession.tracker.presentation.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.studysession.tracker.R
import com.studysession.tracker.core.ui.SecondaryButton

@Composable
fun ConfirmActionDialog(titleRes: Int, message: String, confirmRes: Int, onConfirm: () -> Unit, onDismiss: () -> Unit, busy: Boolean = false) {
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(stringResource(titleRes)) }, text = { Text(message) },
        confirmButton = { SecondaryButton(confirmRes, onConfirm, enabled = !busy) },
        dismissButton = { TextButton(onDismiss, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) } })
}

@Composable
fun UnsavedChangesDialog(onSave: () -> Unit, onDiscard: () -> Unit, onKeepEditing: () -> Unit, saving: Boolean) {
    AlertDialog(onDismissRequest = onKeepEditing, title = { Text(stringResource(R.string.unsaved_title)) },
        text = { Text(stringResource(R.string.unsaved_hint)) },
        confirmButton = { SecondaryButton(R.string.save, onSave, enabled = !saving) },
        dismissButton = {
            Column { SecondaryButton(R.string.discard, onDiscard, enabled = !saving)
                TextButton(onKeepEditing, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.keep_editing)) } }
        })
}
