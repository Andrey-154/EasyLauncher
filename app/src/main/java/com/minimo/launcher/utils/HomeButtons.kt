package com.minimo.launcher.utils

import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID

/** What a round quick button on the home screen does. */
enum class HomeButtonType {
    FLASHLIGHT, CAMERA, PHONE, MESSAGES, BROWSER, CALCULATOR, ALARM,
    WIFI, BLUETOOTH, VOLUME, MEDIA_PLAY_PAUSE, MEDIA_NEXT,
    LOCK_SCREEN, NOTIFICATIONS, SEARCH, SETTINGS, APP
}

enum class HomeButtonSize(val dp: Int) {
    Small(36), Medium(44), Large(56)
}

enum class HomeButtonStyle {
    Outline, Filled
}

/**
 * A button placed freely on the home screen.
 * [x] and [y] are 0..1 fractions of the free space, so positions survive other screen sizes.
 * [app] is the app preference value for [HomeButtonType.APP].
 */
data class HomeButton(
    val id: String = UUID.randomUUID().toString(),
    val type: HomeButtonType,
    val app: String = "",
    val x: Float,
    val y: Float
)

object HomeButtonsJson {
    fun toJson(buttons: List<HomeButton>): String = JSONArray().apply {
        buttons.forEach { button ->
            put(
                JSONObject()
                    .put("id", button.id)
                    .put("type", button.type.name)
                    .put("app", button.app)
                    .put("x", button.x.toDouble())
                    .put("y", button.y.toDouble())
            )
        }
    }.toString()

    fun fromJson(json: String?): List<HomeButton> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val item = array.getJSONObject(i)
                val type = HomeButtonType.entries.find { it.name == item.optString("type") }
                    ?: return@mapNotNull null
                HomeButton(
                    id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                    type = type,
                    app = item.optString("app"),
                    x = item.optDouble("x", 0.5).toFloat().coerceIn(0f, 1f),
                    y = item.optDouble("y", 0.5).toFloat().coerceIn(0f, 1f)
                )
            }
        } catch (exception: Exception) {
            Timber.e(exception)
            emptyList()
        }
    }
}
