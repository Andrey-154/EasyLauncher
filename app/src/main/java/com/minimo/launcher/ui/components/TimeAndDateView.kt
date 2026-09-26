package com.minimo.launcher.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.minimo.launcher.utils.HomeClockMode
import com.minimo.launcher.utils.HomeClockStyle
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimeAndDateView(
    horizontalAlignment: Alignment.Horizontal,
    clockMode: HomeClockMode,
    clockStyle: HomeClockStyle = HomeClockStyle.Normal,
    twentyFourHourFormat: Boolean,
    showBatteryLevel: Boolean,
    textColor: Color,
    textShadow: Shadow?,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    /** Long-press on the time or the date: quick clock settings. */
    onLongClick: () -> Unit = {},
    onBatteryClick: () -> Unit,
    /** Shown to the right of the time, e.g. the flashlight button. */
    timeTrailingContent: (@Composable () -> Unit)? = null
) {
    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }

    val timeFormatter = remember(twentyFourHourFormat) {
        DateTimeFormatter.ofPattern(if (twentyFourHourFormat) "HH:mm" else "hh:mm a")
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, dd MMMM") }

    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            while (true) {
                currentDateTime = LocalDateTime.now()

                val nextMinute = currentDateTime.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES)
                val nextMinuteDelay = ChronoUnit.MILLIS.between(currentDateTime, nextMinute) + 1000

                delay(nextMinuteDelay)
            }
        }
    }

    val timeFontSize = when (clockStyle) {
        HomeClockStyle.Normal -> 30.sp
        HomeClockStyle.Large, HomeClockStyle.LargeThin -> 72.sp
    }
    val timeFontWeight = when (clockStyle) {
        HomeClockStyle.LargeThin -> FontWeight.Light
        else -> FontWeight.Bold
    }

    val dateFontSize = if (clockMode == HomeClockMode.DateOnly) 26.sp else 18.sp
    val dateFontWeight = if (clockMode == HomeClockMode.DateOnly) FontWeight.Bold else null

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (clockMode != HomeClockMode.DateOnly) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.combinedClickable(onClick = onClockClick, onLongClick = onLongClick),
                    text = currentDateTime.format(timeFormatter).uppercase(),
                    fontSize = timeFontSize,
                    lineHeight = timeFontSize,
                    fontWeight = timeFontWeight,
                    color = textColor,
                    style = LocalTextStyle.current.copy(shadow = textShadow)
                )
                if (timeTrailingContent != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    timeTrailingContent()
                }
            }
        }
        if (clockMode != HomeClockMode.TimeOnly) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.combinedClickable(onClick = onDateClick, onLongClick = onLongClick),
                    text = currentDateTime.format(dateFormatter),
                    fontSize = dateFontSize,
                    fontWeight = dateFontWeight,
                    color = textColor,
                    style = LocalTextStyle.current.copy(shadow = textShadow)
                )

                if (showBatteryLevel) {
                    Text(
                        "  |  ",
                        fontSize = dateFontSize,
                        fontWeight = dateFontWeight,
                        color = textColor,
                        style = LocalTextStyle.current.copy(shadow = textShadow)
                    )

                    BatteryPercentView(
                        fontSize = dateFontSize,
                        fontWeight = dateFontWeight,
                        textColor = textColor,
                        textShadow = textShadow,
                        onClick = onBatteryClick
                    )
                }
            }
        }
    }
}
