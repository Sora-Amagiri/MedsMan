package com.sora.medsman.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.sora.medsman.R

/**
 * Renders the adaptive launcher icon layers at the 72/108 safe-area scale so it
 * matches what a launcher shows after masking.
 */
@Composable
fun AppIcon(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape
) {
    Box(modifier = modifier.clip(shape)) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.5f),
            contentScale = ContentScale.FillBounds
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.5f),
            contentScale = ContentScale.FillBounds
        )
    }
}
