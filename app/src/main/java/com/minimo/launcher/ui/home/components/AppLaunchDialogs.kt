package com.minimo.launcher.ui.home.components

import android.os.SystemClock
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.minimo.launcher.R
import com.minimo.launcher.ui.components.AppButton
import com.minimo.launcher.ui.components.AppOutlinedButton
import com.minimo.launcher.ui.entities.AppInfo
import kotlinx.coroutines.android.awaitFrame

@Composable
fun LaunchDelayDialog(
    app: AppInfo,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var delayText by remember(app.id, app.launchDelaySeconds) {
        val text = app.launchDelaySeconds.takeIf { it > 0 }?.toString().orEmpty()
        mutableStateOf(
            TextFieldValue(
                text = text,
                selection = TextRange(text.length)
            )
        )
    }
    val delaySeconds = delayText.text.ifEmpty { "0" }.toIntOrNull()

    AppDialog(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.launch_delay),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.launch_delay_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            value = delayText,
            onValueChange = { value ->
                if (value.text.length <= 3 && value.text.all(Char::isDigit)) {
                    delayText = value
                }
            },
            label = { Text(stringResource(R.string.seconds)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(24.dp))
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { delaySeconds?.let(onSave) },
            text = stringResource(R.string.save),
            enabled = delaySeconds != null
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppOutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onDismiss,
            text = stringResource(R.string.cancel)
        )
    }

    LaunchedEffect(focusRequester) {
        awaitFrame()
        focusRequester.requestFocus()
    }
}

@Composable
fun AppLaunchConfirmationDialog(
    app: AppInfo,
    deadlineElapsedRealtimeMillis: Long,
    onLaunch: () -> Unit,
    onDismiss: () -> Unit
) {
    val totalMillis = app.launchDelaySeconds.coerceAtLeast(0).toLong() * 1_000L
    var remainingMillis by remember(app.id, deadlineElapsedRealtimeMillis) {
        mutableLongStateOf(
            (deadlineElapsedRealtimeMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        )
    }

    LaunchedEffect(app.id, deadlineElapsedRealtimeMillis) {
        while (remainingMillis > 0L) {
            withFrameNanos { }
            remainingMillis =
                (deadlineElapsedRealtimeMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        }
    }

    val remainingSeconds = ((remainingMillis + 999L) / 1_000L).toInt()
    val progress = if (totalMillis == 0L) {
        1f
    } else {
        (1f - remainingMillis.toFloat() / totalMillis).coerceIn(0f, 1f)
    }

    AppDialog(onDismiss = onDismiss) {
        Text(
            text = app.name,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (remainingSeconds > 0) {
                pluralStringResource(
                    R.plurals.launch_delay_remaining,
                    remainingSeconds,
                    remainingSeconds
                )
            } else {
                stringResource(R.string.ready_to_launch)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onLaunch,
            text = stringResource(R.string.launch),
            enabled = remainingMillis == 0L
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppOutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onDismiss,
            text = stringResource(R.string.dismiss)
        )
    }
}

@Composable
private fun AppDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                content = content
            )
        }
    }
}
