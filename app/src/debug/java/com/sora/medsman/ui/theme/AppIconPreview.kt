package com.sora.medsman.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sora.medsman.ui.components.AppIcon

@Preview(widthDp = 380, heightDp = 150, showBackground = true)
@Composable
private fun AppIconPreview() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4FBF7))
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdaptiveIconPreview(shape = CircleShape, size = 96)
        AdaptiveIconPreview(shape = RoundedCornerShape(26.dp), size = 96)
        AdaptiveIconPreview(shape = CircleShape, size = 48)
    }
}

@Composable
private fun AdaptiveIconPreview(shape: Shape, size: Int) {
    AppIcon(
        modifier = Modifier.size(size.dp),
        shape = shape
    )
}
