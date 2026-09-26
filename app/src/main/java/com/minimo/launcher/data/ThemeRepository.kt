package com.minimo.launcher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A look of the launcher: colours, font, wallpaper, sizes, clock style…
 * [values] maps preference key names to Boolean / Int / String.
 */
data class LauncherTheme(
    val name: String,
    val values: Map<String, Any>,
    val builtIn: Boolean = false
)

/** Built-in themes and the user's saved ones (only appearance, never apps or personal data). */
@Singleton
class ThemeRepository @Inject constructor(
    private val preferences: DataStore<Preferences>
) {
    companion object {
        private val KEY_SAVED_THEMES = stringPreferencesKey("KEY_SAVED_THEMES")

        /** Everything a theme may change. Apps, weather city, notes, buttons… are never touched. */
        val THEME_KEYS = listOf(
            "KEY_THEME_MODE", "KEY_BLACK_THEME", "KEY_DYNAMIC_THEME",
            "KEY_CUSTOM_BACKGROUND_COLOR", "KEY_CUSTOM_TEXT_COLOR", "KEY_CUSTOM_ACCENT_COLOR",
            "KEY_FONT_PREFERENCE",
            "KEY_ENABLE_WALLPAPER", "KEY_ENABLE_WALLPAPER_ON_DRAWER", "KEY_DIM_WALLPAPER",
            "KEY_DIM_WALLPAPER_PERCENTAGE", "KEY_LIGHT_TEXT_ON_WALLPAPER",
            "KEY_SHOW_APP_ICON_IN_HOME", "KEY_SHOW_APP_ICON_IN_DRAWER", "KEY_APP_ICON_SIZE_PERCENT",
            "KEY_HOME_APP_ICON_ALIGNMENT", "KEY_DRAWER_APP_ICON_ALIGNMENT",
            "KEY_HOME_TEXT_SIZE", "KEY_HOME_APP_VERTICAL_PADDING",
            "KEY_HOME_APPS_ALIGN", "KEY_DRAWER_APPS_ALIGN_HORIZONTAL", "KEY_HOME_APPS_ALIGN_VERTICAL",
            "KEY_SHOW_HOME_CLOCK", "KEY_HOME_CLOCK_ALIGNMENT", "KEY_HOME_CLOCK_MODE",
            "KEY_HOME_CLOCK_STYLE", "KEY_TWENTY_FOUR_HOUR_FORMAT", "KEY_SHOW_BATTERY_LEVEL",
            "KEY_SEARCH_BAR_BORDER_PERCENT", "KEY_SEARCH_BAR_BACKGROUND",
            "KEY_HOME_BUTTON_SIZE", "KEY_HOME_BUTTON_STYLE",
            "KEY_LIMIT_HOME_APPS", "KEY_MAX_HOME_APPS", "KEY_BLUR_BEHIND"
        )

        private fun colors(mode: String, background: Long, text: Long, accent: Long, black: Boolean = false) =
            mapOf(
                "KEY_THEME_MODE" to mode,
                "KEY_BLACK_THEME" to black,
                "KEY_DYNAMIC_THEME" to false,
                "KEY_ENABLE_WALLPAPER" to false,
                "KEY_ENABLE_WALLPAPER_ON_DRAWER" to false,
                "KEY_CUSTOM_BACKGROUND_COLOR" to background.toInt(),
                "KEY_CUSTOM_TEXT_COLOR" to text.toInt(),
                "KEY_CUSTOM_ACCENT_COLOR" to accent.toInt()
            )

        /** Names are translated in the UI by [LauncherTheme.name] used as a lookup id. */
        val BUILT_IN: List<LauncherTheme> = listOf(
            // The author's own setup: black, large thin clock, icons, dimmed wallpaper, 5-app carousel
            LauncherTheme(
                name = "Easy",
                builtIn = true,
                values = mapOf(
                    "KEY_THEME_MODE" to "Dark",
                    "KEY_BLACK_THEME" to true,
                    "KEY_DYNAMIC_THEME" to false,
                    "KEY_CUSTOM_BACKGROUND_COLOR" to 0xFF000000.toInt(),
                    "KEY_CUSTOM_TEXT_COLOR" to 0xFFFFFFFF.toInt(),
                    "KEY_CUSTOM_ACCENT_COLOR" to 0xFF1E88E5.toInt(),
                    "KEY_ENABLE_WALLPAPER" to true,
                    "KEY_ENABLE_WALLPAPER_ON_DRAWER" to true,
                    "KEY_DIM_WALLPAPER" to true,
                    "KEY_DIM_WALLPAPER_PERCENTAGE" to 80,
                    "KEY_SHOW_APP_ICON_IN_HOME" to true,
                    "KEY_SHOW_APP_ICON_IN_DRAWER" to true,
                    "KEY_APP_ICON_SIZE_PERCENT" to 160,
                    "KEY_HOME_TEXT_SIZE" to 20,
                    "KEY_HOME_APP_VERTICAL_PADDING" to 10,
                    "KEY_HOME_APPS_ALIGN" to "Start",
                    "KEY_DRAWER_APPS_ALIGN_HORIZONTAL" to "Start",
                    "KEY_SHOW_HOME_CLOCK" to true,
                    "KEY_HOME_CLOCK_STYLE" to "LargeThin",
                    "KEY_TWENTY_FOUR_HOUR_FORMAT" to true,
                    "KEY_SHOW_BATTERY_LEVEL" to true,
                    "KEY_SEARCH_BAR_BORDER_PERCENT" to 32,
                    "KEY_SEARCH_BAR_BACKGROUND" to false,
                    "KEY_HOME_BUTTON_STYLE" to "Outline",
                    "KEY_LIMIT_HOME_APPS" to true,
                    "KEY_MAX_HOME_APPS" to 5
                )
            ),
            LauncherTheme("AMOLED", colors("Dark", 0xFF000000, 0xFFFFFFFF, 0xFFE0E0E0, black = true), true),
            LauncherTheme("Nord", colors("Dark", 0xFF2E3440, 0xFFECEFF4, 0xFF88C0D0), true),
            LauncherTheme("Dracula", colors("Dark", 0xFF282A36, 0xFFF8F8F2, 0xFFBD93F9), true),
            LauncherTheme("theme_sepia", colors("Light", 0xFFF4ECD8, 0xFF5B4636, 0xFFA0522D), true),
            LauncherTheme("theme_sunset", colors("Dark", 0xFF2B1B2E, 0xFFFFD6BA, 0xFFFF7A59), true),
            LauncherTheme("theme_mint", colors("Dark", 0xFF0F1F1C, 0xFFD8F3EC, 0xFF3DDC97), true),
            LauncherTheme("theme_ocean", colors("Dark", 0xFF0B1D2E, 0xFFCFE8FF, 0xFF4FC3F7), true),
            LauncherTheme("theme_coffee", colors("Dark", 0xFF1E1612, 0xFFEAD7C3, 0xFFC08552), true),
            LauncherTheme("theme_paper", colors("Light", 0xFFFAFAF7, 0xFF1F1F1F, 0xFF1F1F1F), true),
            LauncherTheme("theme_sakura", colors("Light", 0xFFFFF0F3, 0xFF5A2A3A, 0xFFE56B8A), true)
        )
    }

    val savedThemes: Flow<List<LauncherTheme>> = preferences.data.map { prefs ->
        parseSaved(prefs[KEY_SAVED_THEMES])
    }

    /** The current look, to save it or to undo a theme change. */
    suspend fun captureCurrent(): Map<String, Any> {
        val all = preferences.data.first().asMap()
        return all.entries
            .filter { (key, value) ->
                key.name in THEME_KEYS && (value is Boolean || value is Int || value is String)
            }
            .associate { (key, value) -> key.name to value }
    }

    /** Sets the theme's values; other settings stay as they are. */
    suspend fun apply(values: Map<String, Any>) {
        preferences.edit { prefs -> values.forEach { (name, value) -> prefs.put(name, value) } }
    }

    /** Puts back exactly a captured look (keys it did not have go back to their defaults). */
    suspend fun restore(snapshot: Map<String, Any>) {
        preferences.edit { prefs ->
            THEME_KEYS.forEach { name ->
                val value = snapshot[name]
                if (value != null) {
                    prefs.put(name, value)
                } else {
                    prefs.remove(booleanPreferencesKey(name))
                    prefs.remove(intPreferencesKey(name))
                    prefs.remove(stringPreferencesKey(name))
                }
            }
        }
    }

    suspend fun saveCurrent(name: String) {
        val theme = LauncherTheme(name = name, values = captureCurrent())
        preferences.edit { prefs ->
            val saved = parseSaved(prefs[KEY_SAVED_THEMES]).filterNot { it.name == name } + theme
            prefs[KEY_SAVED_THEMES] = toJson(saved)
        }
    }

    suspend fun delete(theme: LauncherTheme) {
        preferences.edit { prefs ->
            prefs[KEY_SAVED_THEMES] = toJson(parseSaved(prefs[KEY_SAVED_THEMES]).filterNot { it.name == theme.name })
        }
    }

    /** Puts a deleted theme back (undo). */
    suspend fun add(theme: LauncherTheme) {
        preferences.edit { prefs ->
            prefs[KEY_SAVED_THEMES] = toJson(parseSaved(prefs[KEY_SAVED_THEMES]) + theme)
        }
    }

    private fun MutablePreferences.put(name: String, value: Any) {
        when (value) {
            is Boolean -> this[booleanPreferencesKey(name)] = value
            is Int -> this[intPreferencesKey(name)] = value
            is String -> this[stringPreferencesKey(name)] = value
        }
    }

    private fun toJson(themes: List<LauncherTheme>): String = JSONArray().apply {
        themes.forEach { theme ->
            val values = JSONObject()
            theme.values.forEach { (key, value) -> values.put(key, value) }
            put(JSONObject().put("name", theme.name).put("values", values))
        }
    }.toString()

    private fun parseSaved(json: String?): List<LauncherTheme> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                val values = item.getJSONObject("values")
                LauncherTheme(
                    name = item.getString("name"),
                    values = values.keys().asSequence()
                        .filter { it in THEME_KEYS }
                        .associateWith { values.get(it) }
                        .filterValues { it is Boolean || it is Int || it is String }
                )
            }
        } catch (exception: Exception) {
            Timber.e(exception)
            emptyList()
        }
    }
}
