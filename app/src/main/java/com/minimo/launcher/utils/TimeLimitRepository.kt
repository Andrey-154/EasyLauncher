package com.minimo.launcher.utils

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Daily screen time limits in minutes, per package name. */
@Singleton
class TimeLimitRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        const val DEFAULT_WARNING_COLOR = 0xFFFFB300.toInt()
        const val DEFAULT_EXCEEDED_COLOR = 0xFFE53935.toInt()
    }

    private val prefs = context.getSharedPreferences("time_limits", Context.MODE_PRIVATE)

    private val _limits = MutableStateFlow(readAll())
    val limits: StateFlow<Map<String, Int>> = _limits.asStateFlow()

    /** [minutes] <= 0 removes the limit. */
    fun setLimit(packageName: String, minutes: Int) {
        prefs.edit {
            if (minutes > 0) putInt(packageName, minutes) else remove(packageName)
        }
        _limits.value = if (minutes > 0) {
            _limits.value + (packageName to minutes)
        } else {
            _limits.value - packageName
        }
    }

    /** Replaces all limits (used when restoring a backup). */
    fun replaceAll(limits: Map<String, Int>) {
        prefs.edit {
            clear()
            limits.forEach { (packageName, minutes) -> putInt(packageName, minutes) }
        }
        _limits.value = limits
    }

    private fun readAll(): Map<String, Int> =
        prefs.all.mapNotNull { (key, value) -> (value as? Int)?.let { key to it } }.toMap()
}
