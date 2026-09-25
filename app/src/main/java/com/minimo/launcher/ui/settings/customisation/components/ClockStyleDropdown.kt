package com.minimo.launcher.ui.settings.customisation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimo.launcher.R
import com.minimo.launcher.ui.components.DropdownView
import com.minimo.launcher.ui.theme.Dimens
import com.minimo.launcher.utils.HomeClockStyle

@Composable
fun ClockStyleDropdown(
    selectedStyle: HomeClockStyle,
    onStyleSelected: (HomeClockStyle) -> Unit
) {
    val options = HomeClockStyle.entries.map { style ->
        style to stringResource(
            when (style) {
                HomeClockStyle.Normal -> R.string.clock_style_normal
                HomeClockStyle.Large -> R.string.clock_style_large
                HomeClockStyle.LargeThin -> R.string.clock_style_large_thin
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = Dimens.APP_HORIZONTAL_SPACING,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.clock_style),
            modifier = Modifier.weight(1f),
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        DropdownView(
            selectedOption = options.first { it.first == selectedStyle }.second,
            options = options.map { it.second },
            onOptionSelected = { selected ->
                onStyleSelected(options.first { it.second == selected }.first)
            }
        )
    }
}
