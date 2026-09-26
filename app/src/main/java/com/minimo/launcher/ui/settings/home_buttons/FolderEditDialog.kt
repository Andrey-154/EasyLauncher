package com.minimo.launcher.ui.settings.home_buttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.minimo.launcher.R
import com.minimo.launcher.utils.FolderIconStyle
import com.minimo.launcher.utils.FolderOpenStyle
import com.minimo.launcher.utils.FolderPlacement
import com.minimo.launcher.utils.HomeButton

/** An app that can be put into a folder: preference value + display name. */
data class FolderAppChoice(val preference: String, val name: String)

/** Everything the folder dialog edits. */
data class FolderConfig(
    val name: String,
    val apps: List<String>,
    val placement: FolderPlacement,
    val listPosition: Int,
    val iconStyle: FolderIconStyle,
    val openStyle: FolderOpenStyle,
    val showNames: Boolean
)

/**
 * Create or edit a folder: name, apps (in list order), where it lives, how it looks and opens.
 * @param favouritesCount number of favourites, for the position inside the list
 */
@Composable
fun FolderEditDialog(
    initial: HomeButton?,
    favouritesCount: Int,
    allApps: List<FolderAppChoice>,
    onSave: (FolderConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val initialApps = initial?.apps.orEmpty()
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var selected by remember { mutableStateOf(initialApps.toSet()) }
    var filter by remember { mutableStateOf("") }
    var placement by remember { mutableStateOf(initial?.placement ?: FolderPlacement.Button) }
    var position by remember { mutableIntStateOf(initial?.listPosition ?: 0) }
    var iconStyle by remember { mutableStateOf(initial?.iconStyle ?: FolderIconStyle.Folder) }
    var openStyle by remember { mutableStateOf(initial?.openStyle ?: FolderOpenStyle.Grid) }
    var showNames by remember { mutableStateOf(initial?.showNames ?: true) }

    val visibleApps = remember(allApps, filter) {
        if (filter.isBlank()) allApps else allApps.filter { it.name.contains(filter.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initial == null) R.string.folder_create else R.string.folder_edit))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 30) name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.folder_name)) },
                    modifier = Modifier.fillMaxWidth()
                )

                // ---- look ----
                SectionTitle(stringResource(R.string.folder_placement))
                Choices(
                    options = listOf(
                        FolderPlacement.Button to stringResource(R.string.folder_placement_button),
                        FolderPlacement.List to stringResource(R.string.folder_placement_list)
                    ),
                    selected = placement,
                    onSelect = { placement = it }
                )
                if (placement == FolderPlacement.List) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.folder_list_position, position + 1),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = { if (position > 0) position-- }) { Text("−") }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = { if (position < favouritesCount) position++ }) { Text("+") }
                    }
                }

                SectionTitle(stringResource(R.string.folder_icon_style))
                Choices(
                    options = listOf(
                        FolderIconStyle.Folder to stringResource(R.string.folder_icon_folder),
                        FolderIconStyle.Preview to stringResource(R.string.folder_icon_preview),
                        FolderIconStyle.Letter to stringResource(R.string.folder_icon_letter)
                    ),
                    selected = iconStyle,
                    onSelect = { iconStyle = it }
                )

                SectionTitle(stringResource(R.string.folder_open_style))
                Choices(
                    options = listOf(
                        FolderOpenStyle.Grid to stringResource(R.string.folder_open_grid),
                        FolderOpenStyle.List to stringResource(R.string.folder_open_list)
                    ),
                    selected = openStyle,
                    onSelect = { openStyle = it }
                )
                if (openStyle == FolderOpenStyle.Grid) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNames = !showNames },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.folder_show_names), modifier = Modifier.weight(1f))
                        Switch(checked = showNames, onCheckedChange = { showNames = it })
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // ---- apps ----
                SectionTitle(stringResource(R.string.folder_selected_count, selected.size))
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.search_app)) },
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(visibleApps, key = { it.preference }) { app ->
                        val checked = app.preference in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (checked) selected - app.preference
                                    else selected + app.preference
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Text(
                                text = app.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && selected.isNotEmpty(),
                onClick = {
                    // Keep the order of already chosen apps, append new ones alphabetically
                    val ordered = initialApps.filter { it in selected } +
                            allApps.map { it.preference }.filter { it in selected && it !in initialApps }
                    onSave(
                        FolderConfig(
                            name = name.trim(),
                            apps = ordered,
                            placement = placement,
                            listPosition = position.coerceIn(0, favouritesCount),
                            iconStyle = iconStyle,
                            openStyle = openStyle,
                            showNames = showNames
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun <T> Choices(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
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
