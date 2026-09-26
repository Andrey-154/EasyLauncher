package com.minimo.launcher.ui.settings.customisation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimo.launcher.R
import com.minimo.launcher.ui.settings.customisation.settingVisible
import com.minimo.launcher.ui.theme.Dimens
import kotlin.math.roundToInt

/** How many favourites fit on the home screen (1..8); the rest are reached by scrolling. */
@Composable
fun MaxHomeAppsSlider(
    maxHomeApps: Int,
    onMaxHomeAppsChanged: (Int) -> Unit
) {
    if (!settingVisible(stringResource(R.string.max_home_apps))) return

    Row(modifier = Modifier.padding(horizontal = Dimens.APP_HORIZONTAL_SPACING)) {
        Text(text = stringResource(R.string.max_home_apps), fontSize = 20.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = maxHomeApps.toString(), fontSize = 20.sp)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Slider(
        modifier = Modifier.padding(horizontal = Dimens.APP_HORIZONTAL_SPACING),
        value = maxHomeApps.toFloat(),
        onValueChange = { onMaxHomeAppsChanged(it.roundToInt()) },
        valueRange = 1f..8f,
        steps = 6
    )

    Spacer(modifier = Modifier.height(12.dp))
}
