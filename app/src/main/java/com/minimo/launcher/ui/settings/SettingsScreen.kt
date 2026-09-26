package com.minimo.launcher.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.saveable.rememberSaveable
import com.minimo.launcher.ui.settings.customisation.CustomisationScreen
import com.minimo.launcher.ui.settings.customisation.SettingsSearchField
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimo.launcher.R
import com.minimo.launcher.ui.theme.Dimens
import com.minimo.launcher.utils.openHomeSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onFavouriteAppsClick: () -> Unit,
    onHiddenAppsClick: () -> Unit,
    onCustomisationClick: () -> Unit,
    onAboutAppClick: () -> Unit,
    onHomeButtonsClick: () -> Unit,
    onThemesClick: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val backupViewModel: BackupViewModel = hiltViewModel()
    var showBackupDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(backupViewModel::export) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> pendingImportUri = uri }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(R.string.backup_import)) },
            text = { Text(stringResource(R.string.backup_import_confirm)) },
            confirmButton = {
                Button(onClick = {
                    pendingImportUri = null
                    backupViewModel.import(uri)
                }) {
                    Text(stringResource(R.string.backup_import_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showBackupDialog) {
        BackupDialog(
            onExport = {
                showBackupDialog = false
                exportLauncher.launch(
                    "EasyLauncher-" + java.time.LocalDate.now() + ".json"
                )
            },
            onImport = {
                showBackupDialog = false
                importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
            },
            onDismiss = { showBackupDialog = false }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        val menuItems = listOf(
            stringResource(R.string.favourite_apps) to onFavouriteAppsClick,
            stringResource(R.string.hidden_apps) to onHiddenAppsClick,
            stringResource(R.string.themes) to onThemesClick,
            stringResource(R.string.customisation) to onCustomisationClick,
            stringResource(R.string.home_buttons) to onHomeButtonsClick,
            stringResource(R.string.set_default_launcher) to context::openHomeSettings,
            stringResource(R.string.backup) to { showBackupDialog = true },
            stringResource(R.string.about_app) to onAboutAppClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SettingsSearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                focusOnStart = false
            )

            if (searchQuery.isBlank()) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    menuItems.forEach { (name, onClick) -> SettingsItem(name = name, onClick = onClick) }
                }
            } else {
                // Matching sections of this menu, then every matching setting of Customisation
                menuItems
                    .filter { (name, _) -> name.contains(searchQuery.trim(), ignoreCase = true) }
                    .forEach { (name, onClick) -> SettingsItem(name = name, onClick = onClick) }

                Box(modifier = Modifier.weight(1f)) {
                    CustomisationScreen(
                        viewModel = hiltViewModel(),
                        onBackClick = {},
                        searchQuery = searchQuery,
                        embedded = true
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(name: String, onClick: () -> Unit) {
    Text(
        text = name,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 20.sp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick
            )
            .padding(horizontal = Dimens.APP_HORIZONTAL_SPACING, vertical = 16.dp),
    )
}
