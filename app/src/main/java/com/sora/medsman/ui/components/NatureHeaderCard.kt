package com.sora.medsman.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sora.medsman.R
import com.sora.medsman.ui.theme.MedsManTheme
import com.sora.medsman.util.formatDayHeading
import java.time.LocalDate
import java.util.Calendar

enum class NatureScene {
    MISTY_MOUNTAINS,
    TRANQUIL_SUNRISE,
    SERENE_FOREST
}

@Composable
fun NatureHeaderCard(
    userName: String,
    takenCount: Int,
    totalScheduledCount: Int,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    lowStockCount: Int = 0,
    onLowStockClick: (() -> Unit)? = null
) {
    var currentScene by remember { mutableStateOf(NatureScene.MISTY_MOUNTAINS) }
    val context = LocalContext.current

    val todayDateString = remember(context) {
        formatDayHeading(context, LocalDate.now())
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = stringResource(
        when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
    )
    val greetingText = if (userName.isBlank()) {
        greeting
    } else {
        stringResource(R.string.greeting_with_name, greeting, userName)
    }

    val adherencePercentage = if (totalScheduledCount > 0) {
        ((takenCount.toFloat() / totalScheduledCount.toFloat()) * 100).toInt()
    } else {
        0
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Nature Artwork Canvas Header Background
            NatureBackdrop(
                scene = currentScene,
                modifier = Modifier.matchParentSize()
            )

            // Header Content Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar Row (Greeting + Profile Button + Scene Selector)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                currentScene = when (currentScene) {
                                    NatureScene.MISTY_MOUNTAINS -> NatureScene.TRANQUIL_SUNRISE
                                    NatureScene.TRANQUIL_SUNRISE -> NatureScene.SERENE_FOREST
                                    NatureScene.SERENE_FOREST -> NatureScene.MISTY_MOUNTAINS
                                }
                            },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (currentScene) {
                                        NatureScene.MISTY_MOUNTAINS -> Icons.Filled.Landscape
                                        NatureScene.TRANQUIL_SUNRISE -> Icons.Filled.WbSunny
                                        NatureScene.SERENE_FOREST -> Icons.Filled.Park
                                    },
                                    contentDescription = stringResource(R.string.nature_change_scene),
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = greetingText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = todayDateString,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }

                    // Profile Action Button
                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = stringResource(R.string.nature_user_profile),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Daily Adherence Summary Glassmorphic Card Overlay
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Circular Progress Indicator & Adherence %
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(64.dp)
                        ) {
                            val animatedProgress by animateFloatAsState(
                                targetValue = if (totalScheduledCount > 0) takenCount.toFloat() / totalScheduledCount.toFloat() else 0f,
                                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                                label = "AdherenceProgress"
                            )

                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 6.dp,
                                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$adherencePercentage%",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = stringResource(R.string.common_today),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Stats Summary Text
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = when {
                                    takenCount == totalScheduledCount && totalScheduledCount > 0 ->
                                        stringResource(R.string.nature_all_taken)
                                    totalScheduledCount == 0 ->
                                        stringResource(R.string.nature_none_scheduled)
                                    else -> stringResource(
                                        R.string.nature_completed,
                                        takenCount,
                                        totalScheduledCount
                                    )
                                },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            Text(
                                text = stringResource(R.string.nature_keep_routine),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            if (lowStockCount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .clickable(enabled = onLowStockClick != null) {
                                            onLowStockClick?.invoke()
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WarningAmber,
                                        contentDescription = stringResource(R.string.nature_low_stock_alert),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.nature_low_stock, lowStockCount),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NatureBackdrop(scene: NatureScene, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        when (scene) {
            NatureScene.MISTY_MOUNTAINS -> drawMistyMountainsScene(width, height)
            NatureScene.TRANQUIL_SUNRISE -> drawSunriseScene(width, height)
            NatureScene.SERENE_FOREST -> drawForestLakeScene(width, height)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMistyMountainsScene(width: Float, height: Float) {
    // Sky Gradient (Teal-Blue serene transition)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F3832),
                Color(0xFF1E5B53),
                Color(0xFF388E83)
            )
        )
    )

    // Sun / Moon Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF7C2), Color(0x00FFF7C2)),
            center = Offset(width * 0.8f, height * 0.35f),
            radius = 140f
        ),
        center = Offset(width * 0.8f, height * 0.35f),
        radius = 140f
    )
    drawCircle(
        color = Color(0xFFFFFDF0),
        center = Offset(width * 0.8f, height * 0.35f),
        radius = 28f
    )

    // Far Mountains
    val mountainPath1 = Path().apply {
        moveTo(0f, height * 0.7f)
        lineTo(width * 0.25f, height * 0.42f)
        lineTo(width * 0.5f, height * 0.65f)
        lineTo(width * 0.75f, height * 0.48f)
        lineTo(width, height * 0.68f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(
        path = mountainPath1,
        color = Color(0xFF194943).copy(alpha = 0.6f)
    )

    // Mid Mountains (with mist gradient)
    val mountainPath2 = Path().apply {
        moveTo(0f, height * 0.8f)
        lineTo(width * 0.35f, height * 0.52f)
        lineTo(width * 0.65f, height * 0.78f)
        lineTo(width, height * 0.58f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(
        path = mountainPath2,
        color = Color(0xFF0D332D).copy(alpha = 0.85f)
    )

    // Pine Tree Silhouettes
    val treeColor = Color(0xFF061E1A)
    drawTreeSilhouette(this, width * 0.1f, height * 0.68f, 40f, 70f, treeColor)
    drawTreeSilhouette(this, width * 0.18f, height * 0.65f, 50f, 90f, treeColor)
    drawTreeSilhouette(this, width * 0.85f, height * 0.62f, 45f, 80f, treeColor)
    drawTreeSilhouette(this, width * 0.92f, height * 0.66f, 35f, 60f, treeColor)

    // Soft Mist / Lake water band
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0x40FFFFFF), Color.Transparent),
            startY = height * 0.65f,
            endY = height * 0.85f
        ),
        topLeft = Offset(0f, height * 0.65f),
        size = Size(width, height * 0.2f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSunriseScene(width: Float, height: Float) {
    // Warm Sunrise Sky Gradient
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF4A154B), // Deep twilight violet
                Color(0xFF8C335E), // Warm magenta
                Color(0xFFD96B43), // Sunset orange
                Color(0xFFF2B05E)  // Golden horizon
            )
        )
    )

    // Glowing Sun
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF4B8), Color(0x00FFD275)),
            center = Offset(width * 0.5f, height * 0.5f),
            radius = 180f
        ),
        center = Offset(width * 0.5f, height * 0.5f),
        radius = 180f
    )
    drawCircle(
        color = Color(0xFFFFF7DB),
        center = Offset(width * 0.5f, height * 0.5f),
        radius = 36f
    )

    // Rolling Hills
    val hillPath = Path().apply {
        moveTo(0f, height * 0.65f)
        cubicTo(width * 0.25f, height * 0.55f, width * 0.4f, height * 0.72f, width * 0.65f, height * 0.60f)
        cubicTo(width * 0.8f, height * 0.52f, width * 0.95f, height * 0.68f, width, height * 0.65f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(
        path = hillPath,
        color = Color(0xFF3B1E38).copy(alpha = 0.9f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawForestLakeScene(width: Float, height: Float) {
    // Forest Emerald Gradient
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1B4332), // Deep forest
                Color(0xFF2D6A4F), // Emerald green
                Color(0xFF52B788)  // Mint sunrise
            )
        )
    )

    // Lake Reflection Line
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF74C69D).copy(alpha = 0.4f), Color(0xFF1B4332).copy(alpha = 0.8f))
        ),
        topLeft = Offset(0f, height * 0.65f),
        size = Size(width, height * 0.35f)
    )

    // Forest Tree Line
    val forestColor = Color(0xFF081C15)
    for (i in 0..12) {
        val xPos = width * (i / 12f)
        val treeHeight = 50f + (i % 4) * 15f
        val treeWidth = 30f + (i % 3) * 10f
        drawTreeSilhouette(this, xPos, height * 0.65f, treeWidth, treeHeight, forestColor)
    }
}

private fun drawTreeSilhouette(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    x: Float,
    baseY: Float,
    width: Float,
    height: Float,
    color: Color
) {
    val treePath = Path().apply {
        moveTo(x, baseY - height)
        lineTo(x - width / 2, baseY)
        lineTo(x + width / 2, baseY)
        close()
    }
    drawScope.drawPath(path = treePath, color = color)
}

@Preview(showBackground = true)
@Composable
fun NatureHeaderCardPreview() {
    MedsManTheme {
        NatureHeaderCard(
            userName = "Jane",
            takenCount = 2,
            totalScheduledCount = 3,
            lowStockCount = 1,
            onProfileClick = {}
        )
    }
}
