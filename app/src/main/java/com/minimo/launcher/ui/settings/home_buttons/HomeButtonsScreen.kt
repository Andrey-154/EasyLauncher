package com.minimo.launcher.ui.settings.home_buttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimo.launcher.R
import com.minimo.launcher.ui.components.DropdownView
import com.minimo.launcher.ui.components.icon
import com.minimo.launcher.ui.components.title
import com.minimo.launcher.ui.settings.app_picker.AppPickerDialog
import com.minimo.launcher.ui.settings.customisation.components.ToggleItem
import com.minimo.launcher.ui.theme.Dimens
import com.minimo.launcher.utils.HomeButton
import com.minimo.launcher.utils.HomeButtonSize
import com.minimo.launcher.utils.HomeButtonStyle
import com.minimo.launcher.utils.HomeButtonType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeButtonsScreen(
    viewModel: HomeButtonsViewModel,
    onBackClick: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showTypePicker by remember { mutableStateOf(false) }
    val folderChoices by viewModel.folderChoices.collectAsStateWithLifecycle()
    val favouritesCount by viewModel.favouritesCount.collectAsStateWithLifecycle()
    var creatingFolder by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<HomeButton?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.home_buttons)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back_icon)
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.home_buttons_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimens.APP_HORIZONTAL_SPACING, vertical = 8.dp)
            )

            OptionRow(
                title = stringResource(R.string.home_buttons_size),
                selected = sizeName(settings.size),
                options = HomeButtonSize.entries.map { it to sizeName(it) },
                onSelected = viewModel::setSize
            )
            OptionRow(
                title = stringResource(R.string.home_buttons_style),
                selected = styleName(settings.style),
                options = HomeButtonStyle.entries.map { it to styleName(it) },
                onSelected = viewModel::setStyle
            )

            if (settings.buttons.any { it.type == HomeButtonType.FLASHLIGHT }) {
                ToggleItem(
                    title = stringResource(R.string.flashlight_auto_off),
                    subtitle = stringResource(R.string.flashlight_auto_off_description),
                    isChecked = settings.flashlightAutoOff,
                    onToggleClick = viewModel::toggleFlashlightAutoOff
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            if (settings.buttons.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_buttons_none),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Dimens.APP_HORIZONTAL_SPACING, vertical = 8.dp)
                )
            }

            settings.buttons.forEach { button ->
                ButtonRow(
                    button = button,
                    appName = if (button.type == HomeButtonType.APP) {
                        produceState("", button.app) { value = viewModel.appName(button.app) }.value
                    } else {
                        ""
                    },
                    onRemove = { viewModel.removeButton(button) },
                    onEdit = if (button.type == HomeButtonType.FOLDER) {
                        { editingFolder = button }
                    } else {
                        null
                    }
                )
            }

            Button(
                onClick = { showTypePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.APP_HORIZONTAL_SPACING, vertical = 16.dp)
            ) {
                Text(stringResource(R.string.home_buttons_add))
            }

            OutlinedButton(
                onClick = { creatingFolder = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.APP_HORIZONTAL_SPACING)
                    .padding(bottom = 16.dp)
            ) {
                Text(stringResource(R.string.folder_create))
            }
        }
    }

    if (creatingFolder) {
        FolderEditDialog(
            initial = null,
            favouritesCount = favouritesCount,
            allApps = folderChoices,
            onSave = { config ->
                viewModel.addFolder(config)
                creatingFolder = false
            },
            onDismiss = { creatingFolder = false }
        )
    }

    editingFolder?.let { folder ->
        FolderEditDialog(
            initial = folder,
            favouritesCount = favouritesCount,
            allApps = folderChoices,
            onSave = { config ->
                viewModel.updateFolder(folder, config)
                editingFolder = null
            },
            onDismiss = { editingFolder = null }
        )
    }

    if (showTypePicker) {
        AlertDialog(
            onDismissRequest = { showTypePicker = false },
            title = { Text(stringResource(R.string.home_buttons_add)) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Folders have their own "Create folder" button
                    HomeButtonType.entries.filter { it != HomeButtonType.FOLDER }.forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showTypePicker = false
                                    if (type == HomeButtonType.APP) {
                                        showAppPicker = true
                                    } else {
                                        viewModel.addButton(type)
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(type.icon(), contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(stringResource(type.title()), fontSize = 17.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTypePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAppPicker) {
        AppPickerDialog(
            onDismissRequest = { showAppPicker = false },
            onAppSelected = { appInfo ->
                viewModel.addButton(HomeButtonType.APP, appInfo.preferenceValue)
                showAppPicker = false
            }
        )
    }
}

@Composable
private fun ButtonRow(
    button: HomeButton,
    appName: String,
    onRemove: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.APP_HORIZONTAL_SPACING, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(button.type.icon(), contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = when {
                button.type == HomeButtonType.APP && appName.isNotEmpty() -> appName
                button.type == HomeButtonType.FOLDER ->
                    stringResource(R.string.folder_row, button.name, button.apps.size)

                else -> stringResource(button.type.title())
            },
            fontSize = 18.sp,
            modifier = Modifier.weight(1f)
        )
        if (onEdit != null) {
            TextButton(onClick = onEdit) {
                Text(stringResource(R.string.folder_edit_short))
            }
        }
        TextButton(onClick = onRemove) {
            Text(stringResource(R.string.delete))
        }
    }
}

@Composable
private fun <T> OptionRow(
    title: String,
    selected: String,
    options: List<Pair<T, String>>,
    onSelected: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.APP_HORIZONTAL_SPACING, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 20.sp, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(16.dp))
        DropdownView(
            selectedOption = selected,
            options = options.map { it.second },
            onOptionSelected = { name -> onSelected(options.first { it.second == name }.first) }
        )
    }
}

@Composable
private fun sizeName(size: HomeButtonSize): String = stringResource(
    when (size) {
        HomeButtonSize.Small -> R.string.home_buttons_size_small
        HomeButtonSize.Medium -> R.string.home_buttons_size_medium
        HomeButtonSize.Large -> R.string.home_buttons_size_large
    }
)

@Composable
private fun styleName(style: HomeButtonStyle): String = stringResource(
    when (style) {
        HomeButtonStyle.Outline -> R.string.home_buttons_style_outline
        HomeButtonStyle.Filled -> R.string.home_buttons_style_filled
    }
)
