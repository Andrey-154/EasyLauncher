package com.minimo.launcher.ui.settings.customisation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimo.launcher.R
import com.minimo.launcher.ui.theme.Dimens
import kotlinx.coroutines.android.awaitFrame

/** Current text of the settings search field; empty = show everything. */
val LocalSettingsQuery = staticCompositionLocalOf { "" }

/** True when a setting with these texts (title, subtitle, ...) matches the search query. */
@Composable
fun settingVisible(vararg texts: String?): Boolean {
    val query = LocalSettingsQuery.current.trim()
    if (query.isEmpty()) return true
    return texts.any { it != null && it.contains(query, ignoreCase = true) }
}

/** Section divider that disappears while searching (results are shown as one list). */
@Composable
fun SettingsDivider(modifier: Modifier = Modifier) {
    if (LocalSettingsQuery.current.isBlank()) HorizontalDivider(modifier = modifier)
}

@Composable
fun SettingsSpacer(height: Dp) {
    if (LocalSettingsQuery.current.isBlank()) Spacer(modifier = Modifier.height(height))
}

@Composable
fun SettingsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusOnStart: Boolean
) {
    val focusRequester = remember { FocusRequester() }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.APP_HORIZONTAL_SPACING, vertical = 8.dp)
            .focusRequester(focusRequester),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_settings)) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Text("✕", fontSize = 18.sp)
                }
            }
        } else {
            null
        }
    )

    if (focusOnStart) {
        LaunchedEffect(focusRequester) {
            awaitFrame()
            focusRequester.requestFocus()
        }
    }
}
