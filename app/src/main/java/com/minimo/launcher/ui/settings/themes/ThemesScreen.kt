package com.minimo.launcher.ui.settings.themes

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.minimo.launcher.R
import com.minimo.launcher.data.LauncherTheme
import com.minimo.launcher.data.ThemeRepository
import com.minimo.launcher.utils.UndoController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themeRepository: ThemeRepository,
    private val undoController: UndoController
) : ViewModel() {
    val savedThemes: StateFlow<List<LauncherTheme>> = themeRepository.savedThemes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun apply(theme: LauncherTheme, displayName: String) {
        viewModelScope.launch {
            val before = themeRepository.captureCurrent()
            themeRepository.apply(theme.values)
            undoController.offer(context.getString(R.string.theme_applied, displayName)) {
                themeRepository.restore(before)
            }
        }
    }

    fun saveCurrent(name: String) {
        viewModelScope.launch { themeRepository.saveCurrent(name) }
    }

    fun delete(theme: LauncherTheme) {
        viewModelScope.launch {
            themeRepository.delete(theme)
            undoController.offer(context.getString(R.string.theme_deleted, theme.name)) {
                themeRepository.add(theme)
            }
        }
    }
}

/** Built-in theme ids that have translated names. */
@Composable
private fun themeName(theme: LauncherTheme): String = when (theme.name) {
    "theme_sepia" -> stringResource(R.string.theme_sepia)
    "theme_sunset" -> stringResource(R.string.theme_sunset)
    "theme_mint" -> stringResource(R.string.theme_mint)
    "theme_ocean" -> stringResource(R.string.theme_ocean)
    "theme_coffee" -> stringResource(R.string.theme_coffee)
    "theme_paper" -> stringResource(R.string.theme_paper)
    "theme_sakura" -> stringResource(R.string.theme_sakura)
    else -> theme.name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesScreen(
    viewModel: ThemesViewModel,
    onBackClick: () -> Unit
) {
    val saved by viewModel.savedThemes.collectAsStateWithLifecycle()
    var showSaveDialog by remember { mutableStateOf(false) }
    var themeToDelete by remember { mutableStateOf<LauncherTheme?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.themes)) },
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.themes_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.theme_save_current))
                    }
                }
            }

            if (saved.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle(stringResource(R.string.themes_mine))
                }
                items(saved, key = { "saved:" + it.name }) { theme ->
                    val name = themeName(theme)
                    ThemeCard(
                        theme = theme,
                        name = name,
                        onClick = { viewModel.apply(theme, name) },
                        onLongClick = { themeToDelete = theme }
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(stringResource(R.string.themes_built_in))
            }
            items(ThemeRepository.BUILT_IN, key = { "builtin:" + it.name }) { theme ->
                val name = themeName(theme)
                ThemeCard(theme = theme, name = name, onClick = { viewModel.apply(theme, name) })
            }
        }
    }

    if (showSaveDialog) {
        SaveThemeDialog(
            onSave = { name ->
                viewModel.saveCurrent(name)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    themeToDelete?.let { theme ->
        AlertDialog(
            onDismissRequest = { themeToDelete = null },
            title = { Text(theme.name) },
            text = { Text(stringResource(R.string.theme_delete_question)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.delete(theme)
                    themeToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { themeToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp)
    )
}

/** A tiny home screen in the theme's colours: clock, three app names, an accent dot. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThemeCard(
    theme: LauncherTheme,
    name: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val dark = (theme.values["KEY_THEME_MODE"] as? String) != "Light"
    val background = (theme.values["KEY_CUSTOM_BACKGROUND_COLOR"] as? Int)?.let { Color(it) }
        ?: if (dark) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)
    val text = (theme.values["KEY_CUSTOM_TEXT_COLOR"] as? Int)?.let { Color(it) }
        ?: if (dark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
    val accent = (theme.values["KEY_CUSTOM_ACCENT_COLOR"] as? Int)?.let { Color(it) } ?: Color(0xFF6750A4)
    val thinClock = theme.values["KEY_HOME_CLOCK_STYLE"] == "LargeThin"

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(22.dp))
                .background(background)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "12:30",
                    color = text,
                    fontSize = 30.sp,
                    fontWeight = if (thinClock) FontWeight.Light else FontWeight.Bold
                )
                Text(text = "26.09", color = text.copy(alpha = 0.7f), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(10.dp))
                listOf(0.75f, 0.55f, 0.65f).forEach { width ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(width)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(text.copy(alpha = 0.85f))
                    )
                }
            }
            Row(
                modifier = Modifier.align(Alignment.BottomEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 22.dp, height = 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.5f))
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SaveThemeDialog(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_save_current)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 24) name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.theme_name)) }
            )
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = { onSave(name.trim()) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
