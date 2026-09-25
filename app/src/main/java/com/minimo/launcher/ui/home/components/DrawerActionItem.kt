package com.minimo.launcher.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.minimo.launcher.ui.theme.Dimens

/** A non-app row in the drawer list (calculator result, web search), styled like app names. */
@Composable
fun DrawerActionItem(
    modifier: Modifier = Modifier,
    text: String,
    textSize: TextUnit,
    textColor: Color,
    textShadow: Shadow?,
    verticalPadding: Dp,
    appsArrangement: Arrangement.Horizontal,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.APP_HORIZONTAL_SPACING, vertical = verticalPadding),
        horizontalArrangement = appsArrangement
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = textSize,
            lineHeight = textSize * 1.2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = LocalTextStyle.current.copy(shadow = textShadow)
        )
    }
}
