package com.minimo.launcher.ui.home.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.minimo.launcher.R
import com.minimo.launcher.ui.components.AppButton
import com.minimo.launcher.ui.components.AppOutlinedButton
import com.minimo.launcher.ui.home.HomeScreenState
import com.minimo.launcher.utils.Constants
import kotlinx.coroutines.android.awaitFrame

/** Daily limit in minutes for one app; empty or 0 removes the limit. */
@Composable
fun TimeLimitDialog(
    appName: String,
    currentMinutes: Int?,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var minutesText by remember {
        val text = currentMinutes?.toString().orEmpty()
        mutableStateOf(TextFieldValue(text, selection = TextRange(text.length)))
    }
    val minutes = minutesText.text.ifEmpty { "0" }.toIntOrNull()

    AppDialog(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.time_limit),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.time_limit_description, appName),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            value = minutesText,
            onValueChange = { value ->
                if (value.text.length <= 4 && value.text.all(Char::isDigit)) minutesText = value
            },
            label = { Text(stringResource(R.string.minutes_per_day)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(24.dp))
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { minutes?.let(onSave) },
            text = stringResource(R.string.save),
            enabled = minutes != null && minutes <= 24 * 60
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

/** What to show next to an app name: usage text and the limit colour. */
data class UsageDecoration(
    val text: String? = null,
    /** Colour for the usage text (warning / exceeded), null = normal. */
    val color: Color? = null,
    /** True when the whole app name should use [color], not only the time. */
    val colorName: Boolean = false
)

/**
 * @param showUsageWithoutLimit show today's time even for apps without a limit
 *  (the "screen time per app" option in the drawer).
 */
@Composable
fun usageDecoration(
    packageName: String,
    isShortcut: Boolean,
    state: HomeScreenState,
    showUsageWithoutLimit: Boolean
): UsageDecoration {
    if (isShortcut || packageName == Constants.MINIMO_SETTINGS_PACKAGE) return UsageDecoration()
    val limitMinutes = state.timeLimits[packageName]
    val usedMillis = state.appUsageMillis[packageName] ?: 0L

    if (limitMinutes == null) {
        return if (showUsageWithoutLimit && usedMillis >= 60_000) {
            UsageDecoration(text = formatDuration(usedMillis))
        } else {
            UsageDecoration()
        }
    }

    val ratio = usedMillis / (limitMinutes * 60_000.0)
    val color = when {
        ratio >= 1.0 -> Color(state.limitExceededColor)
        ratio >= 0.8 -> Color(state.limitWarningColor)
        else -> null
    }
    return UsageDecoration(
        text = formatDuration(usedMillis) + " / " + formatDuration(limitMinutes * 60_000L),
        color = color,
        colorName = color != null && !state.limitColorTimeOnly
    )
}

@Composable
private fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        stringResource(R.string.screen_time_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.screen_time_minutes, minutes)
    }
}
