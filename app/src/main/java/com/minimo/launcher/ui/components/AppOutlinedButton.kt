package com.minimo.launcher.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppOutlinedButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    enabled: Boolean = true
) {
    OutlinedButton(
        modifier = modifier.height(56.dp),
        onClick = onClick,
        enabled = enabled
    ) {
        Text(text = text, fontSize = 18.sp)
    }
}
