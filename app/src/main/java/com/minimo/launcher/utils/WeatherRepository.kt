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
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class WeatherCity(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class CurrentWeather(
    val temperature: Int,
    @param:StringRes val descriptionRes: Int
)

/** Free weather data from Open-Meteo (https://open-meteo.com), no API key required. */
@Singleton
class WeatherRepository @Inject constructor() {

    suspend fun findCity(query: String): WeatherCity? = withContext(Dispatchers.IO) {
        try {
            val name = URLEncoder.encode(query.trim(), "UTF-8")
            val language = Locale.getDefault().language
            val json = request(
                "https://geocoding-api.open-meteo.com/v1/search" +
                        "?name=$name&count=1&language=$language&format=json"
            )
            val result = json.optJSONArray("results")?.optJSONObject(0) ?: return@withContext null
            WeatherCity(
                name = result.getString("name"),
                latitude = result.getDouble("latitude"),
                longitude = result.getDouble("longitude")
            )
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
                CurrentWeather(
                    temperature = current.getDouble("temperature_2m").roundToInt(),
                    descriptionRes = weatherCodeToText(current.getInt("weather_code"))
                )
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
