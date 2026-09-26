package com.minimo.launcher.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimo.launcher.R
import com.minimo.launcher.ui.entities.AppInfo
import com.minimo.launcher.utils.HomeButton

private val ICON_SIZE = 32.dp

/** Apps of a home screen folder; tapping one launches it. */
@Composable
fun FolderDialog(
    folder: HomeButton,
    findApp: (String) -> AppInfo?,
    loadAppIcon: suspend (AppInfo, Int) -> ImageBitmap?,
    onAppClick: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    // Apps that were uninstalled since the folder was made are simply not shown
    val apps = folder.apps.mapNotNull(findApp)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(folder.name.ifEmpty { stringResource(R.string.folder) }) },
        text = {
            if (apps.isEmpty()) {
                Text(stringResource(R.string.folder_empty))
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    apps.forEach { app ->
                        FolderAppRow(app = app, loadAppIcon = loadAppIcon, onClick = { onAppClick(app) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dismiss))
            }
        }
    )
}

@Composable
private fun FolderAppRow(
    app: AppInfo,
    loadAppIcon: suspend (AppInfo, Int) -> ImageBitmap?,
    onClick: () -> Unit
) {
    val iconSizePx = with(LocalDensity.current) { ICON_SIZE.roundToPx() }
    val icon by produceState<ImageBitmap?>(null, app.id) { value = loadAppIcon(app, iconSizePx) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Image(bitmap = it, contentDescription = null, modifier = Modifier.size(ICON_SIZE))
            Spacer(modifier = Modifier.width(14.dp))
        }
        Text(
            text = app.name,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
