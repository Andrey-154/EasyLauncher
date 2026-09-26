package com.minimo.launcher.ui.home.components

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
    onClick: () -> Unit
) {
    val glyphSize = with(LocalDensity.current) { (textSize * 1.1f).toDp() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.APP_HORIZONTAL_SPACING, vertical = verticalPadding),
        horizontalArrangement = appsArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FolderGlyph(
            folder = folder,
            size = glyphSize,
            color = textColor,
            findApp = findApp,
            loadAppIcon = loadAppIcon
        )
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
