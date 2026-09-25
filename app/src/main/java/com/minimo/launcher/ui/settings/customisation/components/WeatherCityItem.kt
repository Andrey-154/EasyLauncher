package com.minimo.launcher.ui.settings.customisation.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimo.launcher.R
import com.minimo.launcher.ui.theme.Dimens
import kotlinx.coroutines.android.awaitFrame

@Composable
fun WeatherCityItem(
    currentCity: String,
    onCityEntered: (query: String, onResult: (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(currentCity.isEmpty()) }
    var isSearching by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = Dimens.APP_HORIZONTAL_SPACING, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.weather_city),
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = currentCity.ifEmpty { stringResource(R.string.weather_city_not_set) }
        )
    }

    if (showDialog) {
        WeatherCityDialog(
            currentCity = currentCity,
            isSearching = isSearching,
            onSearchClick = { query ->
                isSearching = true
                onCityEntered(query) { found ->
                    isSearching = false
                    if (found) {
                        showDialog = false
                    } else {
                        Toast.makeText(
                            context,
                            R.string.weather_city_not_found,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onCancelClick = { showDialog = false }
        )
    }
}

@Composable
private fun WeatherCityDialog(
    currentCity: String,
    isSearching: Boolean,
    onSearchClick: (String) -> Unit,
    onCancelClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var query by remember { mutableStateOf(currentCity) }

    AlertDialog(
        onDismissRequest = onCancelClick,
        title = { Text(stringResource(R.string.weather_city)) },
        text = {
            OutlinedTextField(
                modifier = Modifier.focusRequester(focusRequester),
                value = query,
                onValueChange = { if (it.length <= 60) query = it },
                singleLine = true,
                enabled = !isSearching,
                label = { Text(stringResource(R.string.weather_enter_city)) }
            )
        },
        confirmButton = {
            Button(
                enabled = !isSearching && query.isNotBlank(),
                onClick = { onSearchClick(query) }
            ) {
                Text(
                    stringResource(
                        if (isSearching) R.string.weather_searching else R.string.save
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelClick) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    LaunchedEffect(focusRequester) {
        awaitFrame()
        focusRequester.requestFocus()
    }
}
