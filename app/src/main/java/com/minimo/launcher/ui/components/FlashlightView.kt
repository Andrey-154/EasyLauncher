package com.minimo.launcher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.minimo.launcher.R

/** "Flashlight" text toggle on the home screen: faint when off, bold when on. */
@Composable
fun FlashlightView(
    horizontalAlignment: Alignment.Horizontal,
    isOn: Boolean,
    onClick: () -> Unit,
    textColor: Color,
    textShadow: Shadow?
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.clickable(onClick = onClick),
            text = stringResource(if (isOn) R.string.flashlight_on else R.string.flashlight),
            fontSize = 18.sp,
            fontWeight = if (isOn) FontWeight.Bold else null,
            color = if (isOn) textColor else textColor.copy(alpha = 0.6f),
            style = LocalTextStyle.current.copy(shadow = textShadow)
        )
    }
}
