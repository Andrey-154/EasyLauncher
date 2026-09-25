package com.minimo.launcher.utils

import androidx.annotation.StringRes
import com.minimo.launcher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class WeatherCity(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val region: String? = null
)

data class CurrentWeather(
    val temperature: Int,
    @param:StringRes val descriptionRes: Int
)

data class HourlyWeather(
    val time: LocalDateTime,
    val temperature: Int,
    @param:StringRes val descriptionRes: Int
)

data class DailyWeather(
    val date: LocalDate,
    val minTemperature: Int,
    val maxTemperature: Int,
    @param:StringRes val descriptionRes: Int
)

data class WeatherForecast(
    val hourly: List<HourlyWeather>,
    val daily: List<DailyWeather>
)

/** Free weather data from Open-Meteo (https://open-meteo.com), no API key required. */
@Singleton
class WeatherRepository @Inject constructor() {

    /** Up to 8 matching places with country and region; null on network error. */
    suspend fun searchCities(query: String): List<WeatherCity>? = withContext(Dispatchers.IO) {
        try {
            val name = URLEncoder.encode(query.trim(), "UTF-8")
            val language = Locale.getDefault().language
            val json = request(
                "https://geocoding-api.open-meteo.com/v1/search" +
                        "?name=$name&count=8&language=$language&format=json"
            )
            val results = json.optJSONArray("results") ?: return@withContext emptyList()
            (0 until results.length()).mapNotNull { i ->
                val result = results.optJSONObject(i) ?: return@mapNotNull null
                WeatherCity(
                    name = result.getString("name"),
                    latitude = result.getDouble("latitude"),
                    longitude = result.getDouble("longitude"),
                    country = result.optString("country").ifBlank { null },
                    region = result.optString("admin1").ifBlank { null }
                )
            }
        } catch (exception: Exception) {
            Timber.e(exception)
            null
        }
    }

    suspend fun loadWeather(latitude: Double, longitude: Double): CurrentWeather? =
        withContext(Dispatchers.IO) {
            try {
                val json = request(
                    "https://api.open-meteo.com/v1/forecast" +
                            "?latitude=$latitude&longitude=$longitude" +
                            "&current=temperature_2m,weather_code&timezone=auto"
                )
                val current = json.getJSONObject("current")
                val temperature = current.optDouble("temperature_2m")
                if (temperature.isNaN()) return@withContext null
                CurrentWeather(
                    temperature = temperature.roundToInt(),
                    descriptionRes = weatherCodeToText(current.optInt("weather_code", -1))
                )
            } catch (exception: Exception) {
                Timber.e(exception)
                null
            }
        }

    /** Next 24 hours (hourly) and 3 days (daily min/max). */
    suspend fun loadForecast(latitude: Double, longitude: Double): WeatherForecast? =
        withContext(Dispatchers.IO) {
            try {
                val json = request(
                    "https://api.open-meteo.com/v1/forecast" +
                            "?latitude=$latitude&longitude=$longitude" +
                            "&hourly=temperature_2m,weather_code" +
                            "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                            "&timezone=auto&forecast_days=3&forecast_hours=24"
                )
                val hourlyJson = json.getJSONObject("hourly")
                val hourTimes = hourlyJson.getJSONArray("time")
                val hourTemps = hourlyJson.getJSONArray("temperature_2m")
                val hourCodes = hourlyJson.getJSONArray("weather_code")
                // Open-Meteo may put null into a series: skip such entries instead of failing
                val hourly = (0 until hourTimes.length()).mapNotNull { i ->
                    val temperature = hourTemps.optDouble(i)
                    if (temperature.isNaN() || hourCodes.isNull(i)) return@mapNotNull null
                    HourlyWeather(
                        time = LocalDateTime.parse(hourTimes.getString(i)),
                        temperature = temperature.roundToInt(),
                        descriptionRes = weatherCodeToText(hourCodes.getInt(i))
                    )
                }

                val dailyJson = json.getJSONObject("daily")
                val dayDates = dailyJson.getJSONArray("time")
                val dayMax = dailyJson.getJSONArray("temperature_2m_max")
                val dayMin = dailyJson.getJSONArray("temperature_2m_min")
                val dayCodes = dailyJson.getJSONArray("weather_code")
                val daily = (0 until dayDates.length()).mapNotNull { i ->
                    val min = dayMin.optDouble(i)
                    val max = dayMax.optDouble(i)
                    if (min.isNaN() || max.isNaN() || dayCodes.isNull(i)) return@mapNotNull null
                    DailyWeather(
                        date = LocalDate.parse(dayDates.getString(i)),
                        minTemperature = min.roundToInt(),
                        maxTemperature = max.roundToInt(),
                        descriptionRes = weatherCodeToText(dayCodes.getInt(i))
                    )
                }
                WeatherForecast(hourly = hourly, daily = daily)
            } catch (exception: Exception) {
                Timber.e(exception)
                null
            }
        }

    private fun request(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    /** WMO weather interpretation codes: https://open-meteo.com/en/docs */
    @StringRes
    private fun weatherCodeToText(code: Int): Int = when (code) {
        0 -> R.string.weather_clear
        1, 2 -> R.string.weather_partly_cloudy
        3 -> R.string.weather_cloudy
        45, 48 -> R.string.weather_fog
        in 51..57 -> R.string.weather_drizzle
        in 61..67 -> R.string.weather_rain
        in 71..77 -> R.string.weather_snow
        in 80..82 -> R.string.weather_showers
        85, 86 -> R.string.weather_snow_showers
        in 95..99 -> R.string.weather_thunderstorm
        else -> R.string.weather_unknown
    }
}

/** "+5°", "-3°", "0°" */
fun formatTemperature(temperature: Int): String =
    if (temperature > 0) "+$temperature°" else "$temperature°"
