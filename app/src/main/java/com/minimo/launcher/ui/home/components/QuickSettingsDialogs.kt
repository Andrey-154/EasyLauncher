package com.minimo.launcher.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimo.launcher.R
import com.minimo.launcher.ui.components.BlurBehind
import com.minimo.launcher.utils.HomeClockMode
import com.minimo.launcher.utils.HomeClockStyle

/** Long-press on the clock: its look, changed live. */
@Composable
fun ClockQuickSettingsDialog(
    style: HomeClockStyle,
    mode: HomeClockMode,
    twentyFourHours: Boolean,
    showBattery: Boolean,
    onStyleChange: (HomeClockStyle) -> Unit,
    onModeChange: (HomeClockMode) -> Unit,
    onTwentyFourHoursChange: (Boolean) -> Unit,
    onShowBatteryChange: (Boolean) -> Unit,
    onHideClock: () -> Unit,
    onDismiss: () -> Unit
) {
    BlurBehind()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quick_clock_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(stringResource(R.string.clock_style))
                Chips(
                    options = listOf(
                        HomeClockStyle.Normal to stringResource(R.string.clock_style_normal),
                        HomeClockStyle.Large to stringResource(R.string.clock_style_large),
                        HomeClockStyle.LargeThin to stringResource(R.string.clock_style_large_thin)
                    ),
                    selected = style,
                    onSelect = onStyleChange
                )

                SectionTitle(stringResource(R.string.clock_mode))
                Chips(
                    options = listOf(
                        HomeClockMode.Full to stringResource(R.string.full),
                        HomeClockMode.TimeOnly to stringResource(R.string.time_only),
                        HomeClockMode.DateOnly to stringResource(R.string.date_only)
                    ),
                    selected = mode,
                    onSelect = onModeChange
                )

                SwitchRow(
                    title = stringResource(R.string.twenty_four_hour_format),
                    checked = twentyFourHours,
                    onChange = onTwentyFourHoursChange
                )
                SwitchRow(
                    title = stringResource(R.string.show_battery_level),
                    checked = showBattery,
                    onChange = onShowBatteryChange
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        },
        dismissButton = {
            TextButton(onClick = onHideClock) { Text(stringResource(R.string.quick_hide_clock)) }
        }
    )
}

/** Long-press on the note: edit, clear or hide it. */
@Composable
fun NoteQuickDialog(
    hasNote: Boolean,
    onEdit: () -> Unit,
    onClear: () -> Unit,
    onHide: () -> Unit,
    onDismiss: () -> Unit
) {
    BlurBehind()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_note)) },
        text = {
            Column {
                ActionRow(stringResource(R.string.quick_note_edit), onEdit)
                if (hasNote) ActionRow(stringResource(R.string.quick_note_clear), onClear)
                ActionRow(stringResource(R.string.quick_note_hide), onHide)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun <T> Chips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionRow(title: String, onClick: () -> Unit) {
    Text(
        text = title,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    )
}
