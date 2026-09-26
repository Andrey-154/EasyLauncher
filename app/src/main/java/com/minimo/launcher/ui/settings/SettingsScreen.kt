package com.minimo.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.minimo.launcher.ui.settings.customisation.SettingsSection
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
    onSectionClick: (SettingsSection) -> Unit,
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
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back_icon)
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        val groups = listOf(
            stringResource(R.string.group_look) to listOf(
                MenuEntry(Icons.Rounded.Palette, stringResource(R.string.themes), stringResource(R.string.themes_subtitle), onThemesClick),
                MenuEntry(Icons.Rounded.Brush, stringResource(R.string.section_look), stringResource(R.string.section_look_subtitle)) { onSectionClick(SettingsSection.Look) },
                MenuEntry(Icons.Rounded.Home, stringResource(R.string.section_home), stringResource(R.string.section_home_subtitle)) { onSectionClick(SettingsSection.Home) },
                MenuEntry(Icons.Rounded.Schedule, stringResource(R.string.section_widgets), stringResource(R.string.section_widgets_subtitle)) { onSectionClick(SettingsSection.Widgets) },
                MenuEntry(Icons.Rounded.Widgets, stringResource(R.string.home_buttons), stringResource(R.string.home_buttons_subtitle), onHomeButtonsClick)
            ),
            stringResource(R.string.group_apps) to listOf(
                MenuEntry(Icons.Rounded.Apps, stringResource(R.string.section_drawer), stringResource(R.string.section_drawer_subtitle)) { onSectionClick(SettingsSection.Drawer) },
                MenuEntry(Icons.Rounded.Search, stringResource(R.string.section_search), stringResource(R.string.section_search_subtitle)) { onSectionClick(SettingsSection.Search) },
                MenuEntry(Icons.Rounded.Star, stringResource(R.string.favourite_apps), stringResource(R.string.favourite_apps_subtitle), onFavouriteAppsClick),
                MenuEntry(Icons.Rounded.VisibilityOff, stringResource(R.string.hidden_apps), stringResource(R.string.hidden_apps_subtitle), onHiddenAppsClick)
            ),
            stringResource(R.string.group_system) to listOf(
                MenuEntry(Icons.Rounded.TouchApp, stringResource(R.string.section_behavior), stringResource(R.string.section_behavior_subtitle)) { onSectionClick(SettingsSection.Behavior) },
                MenuEntry(Icons.Rounded.CheckCircle, stringResource(R.string.set_default_launcher), stringResource(R.string.set_default_launcher_subtitle), context::openHomeSettings),
                MenuEntry(Icons.Rounded.SettingsBackupRestore, stringResource(R.string.backup), stringResource(R.string.backup_subtitle)) { showBackupDialog = true },
                MenuEntry(Icons.Rounded.Info, stringResource(R.string.about_app), stringResource(R.string.about_app_subtitle), onAboutAppClick)
            )
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
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp)
                ) {
                    groups.forEach { (title, entries) -> MenuGroup(title, entries) }
                }
            } else {
                // Matching menu entries, then every matching setting from all pages
                val query = searchQuery.trim()
                val matches = groups.flatMap { it.second }.filter {
                    it.title.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true)
                }
                if (matches.isNotEmpty()) MenuGroup(title = null, entries = matches)

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

private class MenuEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

/** A titled card of menu rows. */
@Composable
private fun MenuGroup(title: String?, entries: List<MenuEntry>) {
    if (title != null) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 28.dp, top = 18.dp, bottom = 8.dp)
        )
    }
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = if (title == null) 6.dp else 0.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        entries.forEachIndexed { index, entry ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
            MenuRow(entry)
        }
    }
}

@Composable
private fun MenuRow(entry: MenuEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = entry.onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = entry.subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
