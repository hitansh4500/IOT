package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceItem
import com.example.model.NetworkTelemetry
import com.example.model.NetworkThreat
import com.example.model.RiskLevel
import com.example.model.WifiConnectionState
import com.example.ui.components.ConnectedWifiStatusBar
import com.example.ui.components.CyberRadarDefenseVisualizer
import com.example.ui.components.PanicLockdownToggle
import com.example.ui.components.RiskBadge
import com.example.ui.components.getDeviceIcon
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    defenseScore: Int,
    devices: List<DeviceItem>,
    threats: List<NetworkThreat>,
    telemetry: NetworkTelemetry,
    wifiState: WifiConnectionState,
    onDisconnectWifi: () -> Unit,
    onToggleLockdown: () -> Unit,
    onStartScan: () -> Unit,
    onOpenAttackSim: () -> Unit,
    onSelectDevice: (DeviceItem) -> Unit,
    onNavigateToDevices: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vulnerableDevices = devices.filter { it.currentRiskLevel == RiskLevel.CRITICAL || it.currentRiskLevel == RiskLevel.HIGH }
    val latestThreat = threats.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Connected Wi-Fi Status Bar
        item {
            ConnectedWifiStatusBar(
                wifiState = wifiState,
                connectedDeviceCount = devices.size,
                onDisconnect = onDisconnectWifi
            )
        }

        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "IOT SHIELD DEFENSE",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Smart Home Security",
                        color = TextPrimaryDark,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                FilledTonalButton(
                    onClick = onStartScan,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CyberCyan.copy(alpha = 0.15f),
                        contentColor = CyberCyan
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("quick_scan_header_btn")
                ) {
                    Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Quick Audit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Radar Defense Visualizer
        item {
            CyberRadarDefenseVisualizer(
                defenseScore = defenseScore,
                devices = devices,
                isPanicActive = telemetry.panicLockdownActive
            )
        }

        // Emergency House Lockdown Toggle
        item {
            PanicLockdownToggle(
                isActive = telemetry.panicLockdownActive,
                onToggle = onToggleLockdown
            )
        }

        // Attack Simulator Prompt Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAttackSim() }
                    .testTag("attack_sim_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberNavyBorder))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CyberPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Test Attack",
                            tint = CyberPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Test Intruder Attack Vector",
                            color = TextPrimaryDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Simulate Alexa Voice Injection or Wall Fan Exploit & observe real-time IDS push notification.",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = TextSecondaryDark
                    )
                }
            }
        }

        // Active Threat / IDS Alert Banner
        if (latestThreat != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (latestThreat.severity == RiskLevel.CRITICAL) CriticalCrimson.copy(alpha = 0.12f) else WarningAmber.copy(alpha = 0.12f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (latestThreat.severity == RiskLevel.CRITICAL) CriticalCrimson else WarningAmber
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (latestThreat.severity == RiskLevel.CRITICAL) CriticalCrimson else WarningAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "LIVE INTRUSION DETECTED",
                                    color = if (latestThreat.severity == RiskLevel.CRITICAL) CriticalCrimson else WarningAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            RiskBadge(level = latestThreat.severity)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = latestThreat.title,
                            color = TextPrimaryDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Target: ${latestThreat.targetDeviceName} • Attacker: ${latestThreat.attackerIp}",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = latestThreat.technicalDetails,
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Telemetry Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryCard(
                    title = "IDS MONITOR",
                    value = if (telemetry.idsActive) "ACTIVE" else "OFF",
                    color = if (telemetry.idsActive) NeonEmerald else CriticalCrimson,
                    modifier = Modifier.weight(1f)
                )
                TelemetryCard(
                    title = "BLOCKED TODAY",
                    value = "${telemetry.blockedThreatsToday}",
                    color = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
                TelemetryCard(
                    title = "HONEYPOT DECOY",
                    value = if (telemetry.honeypotActive) "ONLINE" else "DISABLED",
                    color = CyberPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // High Risk Devices Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vulnerable Smart Appliances (${vulnerableDevices.size})",
                    color = TextPrimaryDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToDevices) {
                    Text("View All (${devices.size})", color = CyberCyan, fontSize = 12.sp)
                }
            }
        }

        // Vulnerable Devices List
        items(vulnerableDevices) { device ->
            DeviceRowItem(
                device = device,
                onClick = { onSelectDevice(device) }
            )
        }
    }
}

@Composable
fun TelemetryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberNavyBorder))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = TextSecondaryDark,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun DeviceRowItem(
    device: DeviceItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("device_item_${device.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (device.currentRiskLevel == RiskLevel.CRITICAL) CriticalCrimson.copy(alpha = 0.5f) else CyberNavyBorder
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        when (device.currentRiskLevel) {
                            RiskLevel.CRITICAL -> CriticalCrimson.copy(alpha = 0.2f)
                            RiskLevel.HIGH -> WarningAmber.copy(alpha = 0.2f)
                            else -> NeonEmerald.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getDeviceIcon(device.category),
                    contentDescription = device.name,
                    tint = when (device.currentRiskLevel) {
                        RiskLevel.CRITICAL -> CriticalCrimson
                        RiskLevel.HIGH -> WarningAmber
                        else -> NeonEmerald
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${device.ipAddress} • ${device.vendor}",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (device.hasDefaultPassword || device.isFirmwareOutdated) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (device.hasDefaultPassword) {
                            Text(
                                text = "DEFAULT PWD",
                                color = CriticalCrimson,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (device.isFirmwareOutdated) {
                            Text(
                                text = "OUTDATED FW",
                                color = WarningAmber,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                RiskBadge(level = device.currentRiskLevel)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Risk: ${device.riskScore}/100",
                    color = TextSecondaryDark,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
