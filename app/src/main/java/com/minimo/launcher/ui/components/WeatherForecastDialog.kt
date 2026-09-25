package com.minimo.launcher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minimo.launcher.R
import com.minimo.launcher.utils.DailyWeather
import com.minimo.launcher.utils.WeatherForecast
import com.minimo.launcher.utils.formatTemperature
import java.time.format.DateTimeFormatter

private sealed interface ForecastState {
    data object Loading : ForecastState
    data object Error : ForecastState
    data class Loaded(val forecast: WeatherForecast) : ForecastState
}

@Composable
fun WeatherForecastDialog(
    cityName: String,
    loadForecast: suspend () -> WeatherForecast?,
    onDismiss: () -> Unit
) {
    val forecastState by produceState<ForecastState>(ForecastState.Loading) {
        value = loadForecast()?.let { ForecastState.Loaded(it) } ?: ForecastState.Error
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(cityName.ifEmpty { stringResource(R.string.weather_forecast) }) },
        text = {
            when (val state = forecastState) {
                ForecastState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                ForecastState.Error -> Text(stringResource(R.string.weather_forecast_error))

                is ForecastState.Loaded -> ForecastContent(state.forecast)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dismiss))
            }
        }
    )
}

@Composable
private fun ForecastContent(forecast: WeatherForecast) {
    val hourFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Column(
        modifier = Modifier
            .heightIn(max = 480.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SectionTitle(stringResource(R.string.weather_next_24_hours))
        // Every 3 hours keeps the list short but covers the whole day
        forecast.hourly.filterIndexed { index, _ -> index % 3 == 0 }.forEach { hour ->
            ForecastRow(
                label = hour.time.format(hourFormatter),
                temperature = formatTemperature(hour.temperature),
                description = stringResource(hour.descriptionRes)
            )
        }

        forecast.daily.drop(1).forEachIndexed { index, day ->
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            DayRow(
                title = stringResource(
                    if (index == 0) R.string.weather_tomorrow else R.string.weather_day_after_tomorrow
                ),
                day = day
            )
        }
    }
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
private fun ForecastRow(label: String, temperature: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.width(64.dp))
        Text(text = temperature, fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp))
        Text(text = description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DayRow(title: String, day: DailyWeather) {
    SectionTitle(title)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${formatTemperature(day.minTemperature)} … ${formatTemperature(day.maxTemperature)}",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = stringResource(day.descriptionRes),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
