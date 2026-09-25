package com.minimo.launcher.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.minimo.launcher.utils.LaunchStatsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saves / restores all launcher settings to a JSON file: preferences, per-app state
 * (favourites and their order, hidden, renamed, launch delay) and launch counters.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: DataStore<Preferences>,
    private val appInfoDao: AppInfoDao,
    private val launchStats: LaunchStatsRepository
) {
    companion object {
        private const val FORMAT = "easylauncher-backup"
        private const val VERSION = 1
    }

    suspend fun export(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject()
                .put("format", FORMAT)
                .put("version", VERSION)
                .put("preferences", exportPreferences())
                .put("apps", exportApps())
                .put("launchCounts", JSONObject(launchStats.counts.value as Map<*, *>))

            context.contentResolver.openOutputStream(uri, "wt")!!.use {
                it.write(root.toString(2).toByteArray())
            }
        }.isSuccess
    }

    /**
     * Reads and validates the whole file first; settings are changed only when it is a complete,
     * compatible backup, so a broken file never leaves the launcher half-restored.
     */
    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val backup = try {
            val text = context.contentResolver.openInputStream(uri)!!.use {
                it.readBytes().decodeToString()
            }
            val root = JSONObject(text)
            if (root.optString("format") != FORMAT) return@withContext ImportResult.NotABackup
            val version = root.optInt("version", -1)
            if (version > VERSION) return@withContext ImportResult.NewerVersion
            if (version < 1) return@withContext ImportResult.NotABackup
            parseBackup(root)
        } catch (exception: Exception) {
            Timber.e(exception)
            return@withContext ImportResult.NotABackup
        }

        try {
            applyPreferences(backup.preferences)
            val restoredApps = applyApps(backup.apps)
            launchStats.replaceAll(backup.launchCounts)
            ImportResult.Success(restoredApps)
        } catch (exception: Exception) {
            Timber.e(exception)
            ImportResult.Failed
        }
    }

    private class ParsedBackup(
        val preferences: List<Preferences.Pair<*>>,
        val apps: Map<AppKey, JSONObject>,
        val launchCounts: Map<String, Int>
    )

    private data class AppKey(val packageName: String, val itemType: String, val targetId: String)

    /** Throws on any malformed part. */
    private fun parseBackup(root: JSONObject): ParsedBackup {
        val prefsJson = root.getJSONObject("preferences")
        val preferences = prefsJson.keys().asSequence().map { name ->
            val entry = prefsJson.getJSONObject(name)
            when (val type = entry.getString("type")) {
                "boolean" -> booleanPreferencesKey(name) to entry.getBoolean("value")
                "int" -> intPreferencesKey(name) to entry.getInt("value")
                "long" -> longPreferencesKey(name) to entry.getLong("value")
                "float" -> floatPreferencesKey(name) to entry.getDouble("value").toFloat()
                "double" -> doublePreferencesKey(name) to entry.getDouble("value")
                "string" -> stringPreferencesKey(name) to entry.getString("value")
                "stringSet" -> {
                    val array = entry.getJSONArray("value")
                    stringSetPreferencesKey(name) to
                            (0 until array.length()).map { array.getString(it) }.toSet()
                }

                else -> error("Unknown preference type $type")
            }
        }.toList()

        val appsJson = root.getJSONArray("apps")
        val apps = (0 until appsJson.length()).map { appsJson.getJSONObject(it) }
            .associateBy { app ->
                AppKey(
                    app.getString("packageName"),
                    app.getString("itemType"),
                    app.getString("targetId")
                )
            }

        val countsJson = root.optJSONObject("launchCounts") ?: JSONObject()
        val launchCounts = countsJson.keys().asSequence().associateWith { countsJson.getInt(it) }

        return ParsedBackup(preferences, apps, launchCounts)
    }

    private suspend fun applyPreferences(values: List<Preferences.Pair<*>>) {
        preferences.edit { prefs ->
            prefs.clear()
            prefs.putAll(*values.toTypedArray())
        }
    }

    private suspend fun exportPreferences(): JSONObject {
        val json = JSONObject()
        preferences.data.first().asMap().forEach { (key, value) ->
            val type = when (value) {
                is Boolean -> "boolean"
                is Int -> "int"
                is Long -> "long"
                is Float -> "float"
                is Double -> "double"
                is String -> "string"
                is Set<*> -> "stringSet"
                else -> return@forEach
            }
            val jsonValue = if (value is Set<*>) JSONArray(value.toList()) else value
            json.put(key.name, JSONObject().put("type", type).put("value", jsonValue))
        }
        return json
    }

    private suspend fun exportApps(): JSONArray {
        val array = JSONArray()
        appInfoDao.getAllApps()
            // Only items with user changes are worth saving
            .filter {
                it.isFavourite || it.isHidden || it.alternateAppName.isNotEmpty() ||
                        it.launchDelaySeconds > 0
            }
            .forEach { app ->
                array.put(
                    JSONObject()
                        .put("packageName", app.packageName)
                        .put("itemType", app.itemType.name)
                        .put("targetId", app.targetId)
                        .put("alternateAppName", app.alternateAppName)
                        .put("isFavourite", app.isFavourite)
                        .put("isHidden", app.isHidden)
                        .put("orderIndex", app.orderIndex)
                        .put("launchDelaySeconds", app.launchDelaySeconds)
                )
            }
        return array
    }

    /** Matches by package + type + target, so it also works on a new phone (other user ids). */
    private suspend fun applyApps(saved: Map<AppKey, JSONObject>): Int {
        var restored = 0
        val updated = appInfoDao.getAllApps().map { app ->
            val backup = saved[AppKey(app.packageName, app.itemType.name, app.targetId)]
            if (backup == null) {
                app.copy(
                    alternateAppName = "",
                    isFavourite = false,
                    isHidden = false,
                    orderIndex = 0,
                    launchDelaySeconds = 0
                )
            } else {
                restored++
                app.copy(
                    alternateAppName = backup.optString("alternateAppName"),
                    isFavourite = backup.optBoolean("isFavourite"),
                    isHidden = backup.optBoolean("isHidden"),
                    orderIndex = backup.optInt("orderIndex"),
                    launchDelaySeconds = backup.optInt("launchDelaySeconds")
                )
            }
        }
        appInfoDao.addApps(updated)
        return restored
    }
}

sealed interface ImportResult {
    data class Success(val restoredApps: Int) : ImportResult
    data object NotABackup : ImportResult
    data object NewerVersion : ImportResult
    data object Failed : ImportResult
}
