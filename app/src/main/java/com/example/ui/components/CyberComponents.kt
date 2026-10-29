package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceCategory
import com.example.model.DeviceItem
import com.example.model.RiskLevel
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
    val (bgColor, textColor, borderColor) = when (level) {
        RiskLevel.CRITICAL -> Triple(Color(0x33EF4444), Color(0xFFFCA5A5), Color(0xFFEF4444))
        RiskLevel.HIGH -> Triple(Color(0x33F97316), Color(0xFFFDBA74), Color(0xFFF97316))
        RiskLevel.MEDIUM -> Triple(Color(0x33F59E0B), Color(0xFFFDE68A), Color(0xFFF59E0B))
        RiskLevel.LOW -> Triple(Color(0x333B82F6), Color(0xFF93C5FD), Color(0xFF3B82F6))
        RiskLevel.SECURE -> Triple(Color(0x3310B981), Color(0xFF6EE7B7), Color(0xFF10B981))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = level.label.uppercase(),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun CyberRadarDefenseVisualizer(
    defenseScore: Int,
    devices: List<DeviceItem>,
    isPanicActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    val ringColor = if (isPanicActive) CriticalCrimson else CyberCyan
    val coreGlowColor = if (defenseScore >= 75) NeonEmerald else if (defenseScore >= 45) WarningAmber else CriticalCrimson

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D1424), Color(0xFF090D1A))
                )
            )
            .border(1.dp, CyberNavyBorder, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.height * 0.42f

            // Radar concentric rings
            drawCircle(
                color = ringColor.copy(alpha = 0.15f),
                radius = maxRadius,
                center = center,
                style = Stroke(width = 1.2f)
            )
            drawCircle(
                color = ringColor.copy(alpha = 0.25f),
                radius = maxRadius * 0.65f,
                center = center,
                style = Stroke(width = 1f)
            )
            drawCircle(
                color = ringColor.copy(alpha = 0.4f),
                radius = maxRadius * 0.35f,
                center = center,
                style = Stroke(width = 1f)
            )

            // Sweep radar line
            val rad = Math.toRadians(sweepAngle.toDouble())
            val lineEnd = Offset(
                x = center.x + (maxRadius * cos(rad)).toFloat(),
                y = center.y + (maxRadius * sin(rad)).toFloat()
            )
            drawLine(
                brush = Brush.radialGradient(
                    listOf(ringColor.copy(alpha = 0.8f), Color.Transparent),
                    center = center,
                    radius = maxRadius
                ),
                start = center,
                end = lineEnd,
                strokeWidth = 2f
            )

            // Device satellite blips
            devices.take(6).forEachIndexed { index, dev ->
                val angle = Math.toRadians((index * 60 + 20).toDouble())
                val dist = maxRadius * (0.45f + (index % 3) * 0.2f)
                val blipPos = Offset(
                    x = center.x + (dist * cos(angle)).toFloat(),
                    y = center.y + (dist * sin(angle)).toFloat()
                )
                val blipColor = when (dev.currentRiskLevel) {
                    RiskLevel.CRITICAL -> CriticalCrimson
                    RiskLevel.HIGH -> Color(0xFFF97316)
                    RiskLevel.MEDIUM -> WarningAmber
                    else -> NeonEmerald
                }

                drawCircle(
                    color = blipColor.copy(alpha = pulseAlpha),
                    radius = 8f,
                    center = blipPos
                )
                drawCircle(
                    color = blipColor,
                    radius = 4f,
                    center = blipPos
                )
            }
        }

        // Center Shield Hub Info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(coreGlowColor.copy(alpha = 0.2f))
                    .border(2.dp, coreGlowColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPanicActive) Icons.Default.Lock else if (defenseScore >= 70) Icons.Default.Shield else Icons.Default.Warning,
                    contentDescription = "Shield Status",
                    tint = coreGlowColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$defenseScore%",
                color = TextPrimaryDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (isPanicActive) "AIR-GAP LOCKDOWN" else if (defenseScore >= 70) "HOME PROTECTED" else "VULNERABILITIES ACTIVE",
                color = if (isPanicActive) CriticalCrimson else if (defenseScore >= 70) NeonEmerald else WarningAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun PanicLockdownToggle(
    isActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isActive) CriticalCrimson else Color(0xFF1E293B)
    val contentColor = if (isActive) Color.White else Color(0xFFF87171)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("panic_lockdown_button"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(CriticalCrimson, Color(0xFF991B1B))
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color.White.copy(alpha = 0.25f) else CriticalCrimson.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.LockOpen else Icons.Default.Security,
                    contentDescription = "Panic Lockdown",
                    tint = if (isActive) Color.White else CriticalCrimson,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isActive) "LOCKDOWN ACTIVE: VOICE AIR-GAPPED" else "EMERGENCY HOUSE LOCKDOWN",
                    color = if (isActive) Color.White else TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isActive)
                        "Voice pod links to locks & appliances severed. Tap to restore normal mode."
                    else
                        "Instantly disconnect Alexa/Hub bridge from smart locks & smart fans.",
                    color = if (isActive) Color.White.copy(alpha = 0.9f) else TextSecondaryDark,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF991B1B),
                    uncheckedThumbColor = CriticalCrimson,
                    uncheckedTrackColor = Color(0xFF0F172A)
                )
            )
        }
    }
}

fun getDeviceIcon(category: DeviceCategory): ImageVector {
    return when (category) {
        DeviceCategory.VOICE_ASSISTANT -> Icons.Default.Mic
        DeviceCategory.APPLIANCE_FAN -> Icons.Default.ModeFanOff
        DeviceCategory.LIGHTING -> Icons.Default.Lightbulb
        DeviceCategory.ACCESS_LOCK -> Icons.Default.Lock
        DeviceCategory.SURVEILLANCE_CAMERA -> Icons.Default.Videocam
        DeviceCategory.ENTERTAINMENT -> Icons.Default.Tv
        DeviceCategory.CLIMATE -> Icons.Default.Thermostat
        DeviceCategory.ROUTER -> Icons.Default.Router
    }
}

@Composable
fun ConnectedWifiStatusBar(
    wifiState: com.example.model.WifiConnectionState,
    connectedDeviceCount: Int,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("connected_wifi_status_bar"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1A33)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CyberCyan.copy(alpha = 0.5f))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NeonEmerald)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "WI-FI: ${wifiState.currentSsid ?: "Home Network"}",
                        color = TextPrimaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Gateway ${wifiState.gatewayIp} • $connectedDeviceCount Devices Online",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            TextButton(
                onClick = onDisconnect,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondaryDark)
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = "Disconnect", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Switch", fontSize = 11.sp)
            }
        }
    }
}
