package com.minimo.launcher.ui.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.minimo.launcher.ui.components.FolderGlyph
import com.minimo.launcher.ui.entities.AppInfo
import com.minimo.launcher.ui.theme.Dimens
import com.minimo.launcher.utils.HomeButton

/** A row of the home favourites list: an app or a folder placed "in the list". */
sealed interface HomeEntry {
    val key: String

    data class App(val app: AppInfo) : HomeEntry {
        override val key: String get() = app.id
    }

    data class Folder(val folder: HomeButton) : HomeEntry {
        override val key: String get() = "folder:" + folder.id
    }
}

/** A folder row styled like an app name: small folder glyph + folder name. */
@Composable
fun FolderListRow(
    modifier: Modifier,
    folder: HomeButton,
    textSize: TextUnit,
    textColor: Color,
    textShadow: Shadow?,
    appsArrangement: Arrangement.Horizontal,
    verticalPadding: Dp,
    findApp: (String) -> AppInfo?,
    loadAppIcon: suspend (AppInfo, Int) -> ImageBitmap?,
    /** Gets the folder glyph centre on the screen (the folder grows out of it). */
    onClick: (Offset) -> Unit
) {
    val glyphSize = with(LocalDensity.current) { (textSize * 1.1f).toDp() }
    var glyphCenter by remember { mutableStateOf(Offset.Zero) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(glyphCenter) }
            .padding(horizontal = Dimens.APP_HORIZONTAL_SPACING, vertical = verticalPadding),
        horizontalArrangement = appsArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.onScreenCenter { glyphCenter = it }) {
            FolderGlyph(
                folder = folder,
                size = glyphSize,
                color = textColor,
                findApp = findApp,
                loadAppIcon = loadAppIcon
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = folder.name,
            color = textColor,
            fontSize = textSize,
            lineHeight = textSize * 1.2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = LocalTextStyle.current.copy(shadow = textShadow)
        )
    }
}
