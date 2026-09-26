package com.minimo.launcher.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import com.minimo.launcher.R
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeatherView(
    horizontalAlignment: Alignment.Horizontal,
    weatherText: String,
    cityMissing: Boolean = false,
    refreshWeather: () -> Unit,
    onClick: () -> Unit,
    /** Long-press: change the city. */
    onLongClick: () -> Unit = {},
    textColor: Color,
    textShadow: Shadow?
) {
    // Refresh weather when app resumes
    LifecycleResumeEffect(Unit) {
        refreshWeather()
        onPauseOrDispose { }
    }

    if (weatherText.isEmpty() && !cityMissing) return

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
            text = if (cityMissing) stringResource(R.string.weather_city_missing) else weatherText,
            fontWeight = if (cityMissing) null else FontWeight.Bold,
            fontSize = 18.sp,
            color = if (cityMissing) textColor.copy(alpha = 0.45f) else textColor,
            style = LocalTextStyle.current.copy(shadow = textShadow)
        )
    }
}
