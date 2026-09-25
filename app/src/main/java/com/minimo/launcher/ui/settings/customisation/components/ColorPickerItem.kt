package com.minimo.launcher.ui.settings.customisation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimo.launcher.R
import com.minimo.launcher.ui.settings.customisation.settingVisible
import com.minimo.launcher.ui.theme.Dimens

private val PresetColors = listOf(
    0xFF000000, 0xFF1C1C1E, 0xFF3A3A3C, 0xFF8E8E93, 0xFFD1D1D6, 0xFFFFFFFF,
    0xFFF5F0E6, 0xFF3E2723, 0xFF0D1B2A, 0xFF1B263B, 0xFF102A1E, 0xFF2D0A31,
    0xFFE53935, 0xFFFF7043, 0xFFFFB300, 0xFFFFEB3B, 0xFF7CB342, 0xFF43A047,
    0xFF00897B, 0xFF00ACC1, 0xFF1E88E5, 0xFF3949AB, 0xFF8E24AA, 0xFFD81B60
).map { Color(it) }

/** A settings row showing the chosen color; tap opens a palette + HEX dialog. */
@Composable
fun ColorPickerItem(
    title: String,
    color: Int?,
    onColorSelected: (Int?) -> Unit
) {
    if (!settingVisible(title, stringResource(R.string.custom_colors))) return

    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = Dimens.APP_HORIZONTAL_SPACING, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 20.sp, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(16.dp))
        if (color == null) {
            Text(
                text = stringResource(R.string.default_app),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            ColorCircle(color = Color(color), selected = false, onClick = { showDialog = true })
        }
    }

    if (showDialog) {
        ColorPickerDialog(
            title = title,
            initialColor = color,
            onSave = {
                onColorSelected(it)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun ColorPickerDialog(
    title: String,
    initialColor: Int?,
    onSave: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(initialColor?.let { Color(it) }) }
    var hex by remember { mutableStateOf(initialColor?.let { toHex(Color(it)) } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PresetColors.forEach { preset ->
                        ColorCircle(
                            color = preset,
                            selected = preset == selected,
                            onClick = {
                                selected = preset
                                hex = toHex(preset)
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = hex,
                    onValueChange = { value ->
                        hex = value.take(7)
                        parseHex(hex)?.let { selected = it }
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.color_hex)) },
                    leadingIcon = selected?.let { color ->
                        {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(color, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selected?.toArgb()) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave(null) }) {
                    Text(stringResource(R.string.reset_color))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

@Composable
private fun ColorCircle(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .padding(if (selected) 5.dp else 1.dp)
            .background(color, CircleShape)
            .clickable(onClick = onClick)
    )
}

private fun toHex(color: Color): String =
    "#%06X".format(color.toArgb() and 0xFFFFFF)

private fun parseHex(text: String): Color? {
    val digits = text.trim().removePrefix("#")
    if (digits.length != 6) return null
    return digits.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
}
