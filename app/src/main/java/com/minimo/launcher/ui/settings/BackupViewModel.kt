package com.minimo.launcher.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimo.launcher.R
import com.minimo.launcher.data.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager
) : ViewModel() {

    fun export(uri: Uri) {
        viewModelScope.launch {
            val ok = backupManager.export(uri)
            toast(context.getString(if (ok) R.string.backup_export_done else R.string.backup_export_failed))
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            val restoredApps = backupManager.import(uri)
            toast(
                if (restoredApps != null) {
                    context.getString(R.string.backup_import_done, restoredApps)
                } else {
                    context.getString(R.string.backup_import_failed)
                }
            )
        }
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
