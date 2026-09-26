package com.minimo.launcher.ui.settings.customisation.components

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimo.launcher.R
import com.minimo.launcher.ui.components.BlurBehind
import com.minimo.launcher.ui.settings.customisation.settingVisible
import com.minimo.launcher.ui.theme.Dimens
import com.minimo.launcher.utils.WeatherCity
import kotlinx.coroutines.android.awaitFrame

@Composable
fun WeatherCityItem(
    currentCity: String,
    onSearch: (query: String, onResult: (List<WeatherCity>?) -> Unit) -> Unit,
    onCitySelected: (WeatherCity) -> Unit,
    onDetectLocation: (onResult: (Boolean) -> Unit) -> Unit
) {
    if (!settingVisible(stringResource(R.string.weather_city), currentCity)) return

    var showDialog by remember { mutableStateOf(currentCity.isEmpty()) }

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
            onSearch = onSearch,
            onCitySelected = { city ->
                onCitySelected(city)
                showDialog = false
            },
            onDetectLocation = onDetectLocation,
            onLocationDetected = { showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun WeatherCityDialog(
    onSearch: (query: String, onResult: (List<WeatherCity>?) -> Unit) -> Unit,
    onCitySelected: (WeatherCity) -> Unit,
    onDetectLocation: (onResult: (Boolean) -> Unit) -> Unit,
    onLocationDetected: () -> Unit,
    onDismiss: () -> Unit
) {
    BlurBehind()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WeatherCity>?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    fun search() {
        if (query.isBlank() || isBusy) return
        isBusy = true
        onSearch(query) { found ->
            isBusy = false
            results = found ?: emptyList()
            if (found == null) {
                Toast.makeText(context, R.string.weather_city_not_found, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun detect() {
        isBusy = true
        onDetectLocation { success ->
            isBusy = false
            if (success) {
                onLocationDetected()
            } else {
                Toast.makeText(context, R.string.weather_location_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            detect()
        } else {
            Toast.makeText(context, R.string.weather_location_denied, Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weather_city)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    value = query,
                    onValueChange = { if (it.length <= 60) query = it },
                    singleLine = true,
                    enabled = !isBusy,
                    label = { Text(stringResource(R.string.weather_enter_city)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { search() })
                )

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isBusy,
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                ) {
                    Text(stringResource(R.string.weather_detect_location))
                }

                if (isBusy) {
                    Text(
                        text = stringResource(R.string.weather_searching),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                results?.let { cities ->
                    if (cities.isEmpty()) {
                        Text(stringResource(R.string.weather_city_not_found))
                    } else {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            cities.forEachIndexed { index, city ->
                                if (index > 0) HorizontalDivider()
                                CityRow(city = city, onClick = { onCitySelected(city) })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !isBusy && query.isNotBlank(), onClick = { search() }) {
                Text(stringResource(R.string.search))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    LaunchedEffect(focusRequester) {
        awaitFrame()
        focusRequester.requestFocus()
    }
}

@Composable
private fun CityRow(city: WeatherCity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Text(text = city.name, fontSize = 18.sp)
        val details = listOfNotNull(city.country, city.region).distinct().joinToString(", ")
        if (details.isNotEmpty()) {
            Text(
                text = details,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
