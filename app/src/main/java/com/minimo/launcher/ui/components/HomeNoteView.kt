package com.minimo.launcher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.minimo.launcher.R
import kotlinx.coroutines.android.awaitFrame

private const val MAX_NOTE_LENGTH = 120

/** One-line note under the clock; shows a faint hint when empty. */
@Composable
fun HomeNoteView(
    horizontalAlignment: Alignment.Horizontal,
    note: String,
    onClick: () -> Unit,
    textColor: Color,
    textShadow: Shadow?
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.clickable(onClick = onClick),
            text = note.ifEmpty { stringResource(R.string.home_note_hint) },
            fontSize = 18.sp,
            color = if (note.isEmpty()) textColor.copy(alpha = 0.45f) else textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = LocalTextStyle.current.copy(shadow = textShadow)
        )
    }
}

@Composable
fun HomeNoteDialog(
    currentNote: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var note by remember {
        mutableStateOf(TextFieldValue(currentNote, selection = TextRange(currentNote.length)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_note)) },
        text = {
            OutlinedTextField(
                modifier = Modifier.focusRequester(focusRequester),
                value = note,
                onValueChange = { if (it.text.length <= MAX_NOTE_LENGTH) note = it },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onSave(note.text) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            if (currentNote.isNotEmpty()) {
                TextButton(onClick = { onSave("") }) {
                    Text(stringResource(R.string.delete))
                }
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    LaunchedEffect(focusRequester) {
        awaitFrame()
        focusRequester.requestFocus()
    }
}
