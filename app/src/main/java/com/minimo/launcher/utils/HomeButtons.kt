package com.minimo.launcher.utils

import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID

/** What a round quick button on the home screen does. */
enum class HomeButtonType {
    FLASHLIGHT, CAMERA, PHONE, MESSAGES, BROWSER, CALCULATOR, ALARM,
    WIFI, BLUETOOTH, VOLUME, MEDIA_PLAY_PAUSE, MEDIA_NEXT,
    LOCK_SCREEN, NOTIFICATIONS, SEARCH, SETTINGS, APP, FOLDER
}

enum class HomeButtonSize(val dp: Int) {
    Small(36), Medium(44), Large(56)
}

enum class HomeButtonStyle {
    Outline, Filled
}

/** Where a folder lives: a free button, or a row inside the favourites list/carousel. */
enum class FolderPlacement { Button, List }

/** How a closed folder looks. */
enum class FolderIconStyle { Folder, Preview, Letter }

/** How a folder opens. */
enum class FolderOpenStyle { Grid, List }

/**
 * A button placed freely on the home screen.
 * [x] and [y] are 0..1 fractions of the free space, so positions survive other screen sizes.
 * [app] is the app preference value for [HomeButtonType.APP].
 * [name] and [apps] (app preference values) describe a [HomeButtonType.FOLDER];
 * [listPosition] is its 0-based place among the favourites when [placement] is List.
 */
data class HomeButton(
    val id: String = UUID.randomUUID().toString(),
    val type: HomeButtonType,
    val app: String = "",
    val x: Float,
    val y: Float,
    val name: String = "",
    val apps: List<String> = emptyList(),
    val placement: FolderPlacement = FolderPlacement.Button,
    val listPosition: Int = 0,
    val iconStyle: FolderIconStyle = FolderIconStyle.Folder,
    val openStyle: FolderOpenStyle = FolderOpenStyle.Grid,
    val showNames: Boolean = true
) {
    val isListFolder: Boolean
        get() = type == HomeButtonType.FOLDER && placement == FolderPlacement.List
}

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
                    .put("name", button.name)
                    .put("apps", JSONArray(button.apps))
                    .put("placement", button.placement.name)
                    .put("listPosition", button.listPosition)
                    .put("iconStyle", button.iconStyle.name)
                    .put("openStyle", button.openStyle.name)
                    .put("showNames", button.showNames)
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
                    y = item.optDouble("y", 0.5).toFloat().coerceIn(0f, 1f),
                    name = item.optString("name"),
                    apps = item.optJSONArray("apps")?.let { array ->
                        (0 until array.length()).map { array.getString(it) }
                    } ?: emptyList(),
                    placement = enumOr(item.optString("placement"), FolderPlacement.Button),
                    listPosition = item.optInt("listPosition", 0).coerceAtLeast(0),
                    iconStyle = enumOr(item.optString("iconStyle"), FolderIconStyle.Folder),
                    openStyle = enumOr(item.optString("openStyle"), FolderOpenStyle.Grid),
                    showNames = item.optBoolean("showNames", true)
                )
            }
        } catch (exception: Exception) {
            Timber.e(exception)
            emptyList()
        }
    }
}

private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
    enumValues<T>().find { it.name == name } ?: default
