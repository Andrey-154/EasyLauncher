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

    /** @return number of restored apps, or null when the file is not a valid backup. */
    suspend fun import(uri: Uri): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)!!.use {
                it.readBytes().decodeToString()
            }
            val root = JSONObject(text)
            require(root.optString("format") == FORMAT)

            importPreferences(root.getJSONObject("preferences"))
            val restoredApps = importApps(root.getJSONArray("apps"))

            root.optJSONObject("launchCounts")?.let { counts ->
                launchStats.replaceAll(counts.keys().asSequence().associateWith { counts.getInt(it) })
            }
            restoredApps
        }.getOrNull()
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

    private suspend fun importPreferences(json: JSONObject) {
        preferences.edit { prefs ->
            prefs.clear()
            json.keys().forEach { name ->
                val entry = json.getJSONObject(name)
                when (entry.getString("type")) {
                    "boolean" -> prefs[booleanPreferencesKey(name)] = entry.getBoolean("value")
                    "int" -> prefs[intPreferencesKey(name)] = entry.getInt("value")
                    "long" -> prefs[longPreferencesKey(name)] = entry.getLong("value")
                    "float" -> prefs[floatPreferencesKey(name)] = entry.getDouble("value").toFloat()
                    "double" -> prefs[doublePreferencesKey(name)] = entry.getDouble("value")
                    "string" -> prefs[stringPreferencesKey(name)] = entry.getString("value")
                    "stringSet" -> {
                        val array = entry.getJSONArray("value")
                        prefs[stringSetPreferencesKey(name)] =
                            (0 until array.length()).map { array.getString(it) }.toSet()
                    }
                }
            }
        }
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
    private suspend fun importApps(array: JSONArray): Int {
        val saved = (0 until array.length()).map { array.getJSONObject(it) }
            .associateBy { Triple(it.getString("packageName"), it.getString("itemType"), it.getString("targetId")) }

        val updated = appInfoDao.getAllApps().map { app ->
            val backup = saved[Triple(app.packageName, app.itemType.name, app.targetId)]
            if (backup == null) {
                app.copy(
                    alternateAppName = "",
                    isFavourite = false,
                    isHidden = false,
                    orderIndex = 0,
                    launchDelaySeconds = 0
                )
            } else {
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
        return updated.count { app ->
            saved.containsKey(Triple(app.packageName, app.itemType.name, app.targetId))
        }
    }
}
