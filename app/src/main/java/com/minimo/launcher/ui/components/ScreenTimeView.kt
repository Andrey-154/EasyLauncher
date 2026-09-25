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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect

@Composable
fun ScreenTimeView(
    horizontalAlignment: Alignment.Horizontal,
    screenTime: String,
    refreshScreenTime: () -> Unit,
    onClick: () -> Unit,
    textColor: Color,
    textShadow: Shadow?
) {
    // Refresh screen time when app resumes
    LifecycleResumeEffect(Unit) {
        refreshScreenTime()
        onPauseOrDispose { }
    }

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.clickable { onClick() },
            text = screenTime,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = textColor,
            style = LocalTextStyle.current.copy(shadow = textShadow)
        )
    }
}