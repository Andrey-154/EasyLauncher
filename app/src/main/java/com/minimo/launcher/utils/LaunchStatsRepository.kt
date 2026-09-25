package com.minimo.launcher.utils

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Counts how many times each item was opened from the launcher, for "sort by usage".
 * Keys are [com.minimo.launcher.ui.entities.AppInfo.id].
 */
@Singleton
class LaunchStatsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("launch_stats", Context.MODE_PRIVATE)

    private val _counts = MutableStateFlow(readAll())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    fun recordLaunch(appId: String) {
        val newCount = (_counts.value[appId] ?: 0) + 1
        prefs.edit { putInt(appId, newCount) }
        _counts.update { it + (appId to newCount) }
    }

    /** Replaces all counters (used when restoring a backup). */
    fun replaceAll(counts: Map<String, Int>) {
        prefs.edit {
            clear()
            counts.forEach { (id, count) -> putInt(id, count) }
        }
        _counts.value = counts
    }

    private fun readAll(): Map<String, Int> =
        prefs.all.mapNotNull { (key, value) -> (value as? Int)?.let { key to it } }.toMap()
}
