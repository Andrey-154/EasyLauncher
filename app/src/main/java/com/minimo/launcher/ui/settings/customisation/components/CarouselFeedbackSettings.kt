package com.minimo.launcher.ui.settings.customisation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimo.launcher.R
import com.minimo.launcher.ui.components.DropdownView
import com.minimo.launcher.ui.settings.customisation.settingVisible
import com.minimo.launcher.ui.theme.Dimens
import com.minimo.launcher.utils.CarouselHaptic
import com.minimo.launcher.utils.CarouselSound
import kotlin.math.roundToInt

/** Vibration and sound while scrolling the favourites carousel. */
@Composable
fun CarouselFeedbackSettings(
    haptic: CarouselHaptic,
    sound: CarouselSound,
    soundVolume: Int,
    onHapticChanged: (CarouselHaptic) -> Unit,
    onSoundChanged: (CarouselSound) -> Unit,
    onSoundVolumeChanged: (Int) -> Unit
) {
    OptionRow(
        title = stringResource(R.string.carousel_haptic),
        selected = haptic,
        options = listOf(
            CarouselHaptic.Off to stringResource(R.string.off),
            CarouselHaptic.Light to stringResource(R.string.carousel_haptic_light),
            CarouselHaptic.Strong to stringResource(R.string.carousel_haptic_strong)
        ),
        onSelected = onHapticChanged
    )

    OptionRow(
        title = stringResource(R.string.carousel_sound),
        selected = sound,
        options = listOf(
            CarouselSound.Off to stringResource(R.string.off),
            CarouselSound.Tick to stringResource(R.string.carousel_sound_tick),
            CarouselSound.Click to stringResource(R.string.carousel_sound_click)
        ),
        onSelected = onSoundChanged
    )

    if (sound != CarouselSound.Off && settingVisible(stringResource(R.string.carousel_sound_volume))) {
        Row(modifier = Modifier.padding(horizontal = Dimens.APP_HORIZONTAL_SPACING)) {
            Text(text = stringResource(R.string.carousel_sound_volume), fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "$soundVolume%", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            modifier = Modifier.padding(horizontal = Dimens.APP_HORIZONTAL_SPACING),
            value = soundVolume.toFloat(),
            onValueChange = { onSoundVolumeChanged(it.roundToInt()) },
            valueRange = 10f..100f,
            steps = 8
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun <T> OptionRow(
    title: String,
    selected: T,
    options: List<Pair<T, String>>,
    onSelected: (T) -> Unit
) {
    if (!settingVisible(title)) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.APP_HORIZONTAL_SPACING, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 20.sp, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(16.dp))
        DropdownView(
            selectedOption = options.first { it.first == selected }.second,
            options = options.map { it.second },
            onOptionSelected = { name -> onSelected(options.first { it.second == name }.first) }
        )
    }
}
