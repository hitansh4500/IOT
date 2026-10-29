package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.model.DeviceCategory
import com.example.model.DeviceItem
import com.example.model.RiskLevel
import com.example.model.VulnerabilityFinding
import com.example.ui.components.RiskBadge
import com.example.ui.components.getDeviceIcon
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailModal(
    device: DeviceItem,
    onDismiss: () -> Unit,
    onToggleIsolation: () -> Unit,
    onToggleVoiceBridge: () -> Unit,
    onHardenDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CyberNavySurface,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
        ) {
            // Header: Device Info
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
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
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.name,
                            color = TextPrimaryDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${device.vendor} • ${device.category.displayName}",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }

                    RiskBadge(level = device.currentRiskLevel)
                }
            }

            // Specs Card: IP, MAC, Ports, Firmware
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberNavyBorder))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpecRow("IP Address", device.ipAddress)
                        SpecRow("MAC Address", device.macAddress)
                        SpecRow("Current Firmware", device.firmwareVersion)
                        SpecRow(
                            "Latest Available",
                            device.latestAvailableFirmware
                        )
                        if (device.openPorts.isNotEmpty()) {
                            SpecRow("Listening Ports", device.openPorts.joinToString(", ") { "TCP $it" })
                        }
                    }
                }
            }

            // Security Controls Section
            item {
                Text(
                    text = "ACTIVE DEFENSE CONTROLS",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            // Control 1: VLAN Isolation
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberNavyBorder))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Guest IoT VLAN Isolation",
                                color = TextPrimaryDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Quarantines device so it cannot access personal PCs, mobile devices, or router admin.",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = device.isIsolated,
                            onCheckedChange = { onToggleIsolation() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonEmerald
                            )
                        )
                    }
                }
            }

            // Control 2: Voice Bridge Restriction (for Alexa, locks, fans)
            if (device.category == DeviceCategory.VOICE_ASSISTANT ||
                device.category == DeviceCategory.ACCESS_LOCK ||
                device.category == DeviceCategory.APPLIANCE_FAN
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberNavyBorder))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Block Voice Assistant Actuation",
                                    color = TextPrimaryDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Disconnects Alexa/Hub API permissions so spoken commands cannot actuate this appliance.",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = device.voiceBridgeRestricted,
                                onCheckedChange = { onToggleVoiceBridge() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CyberCyan
                                )
                            )
                        }
                    }
                }
            }

            // Control 3: Apply Security Hardening
            item {
                Button(
                    onClick = onHardenDevice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("apply_device_hardening_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (device.isHardened) NeonEmerald else CyberCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (device.isHardened) Icons.Default.CheckCircle else Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (device.isHardened) "DEVICE HARDENED & SECURE" else "APPLY SECURITY HARDENING PATCH",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Vulnerability Findings Header
            item {
                Text(
                    text = "VULNERABILITY FINDINGS (${device.vulnerabilities.size})",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            // Vulnerability Cards
            if (device.vulnerabilities.isEmpty()) {
                item {
                    Text(
                        text = "No open vulnerabilities detected on this device. Security posture optimal.",
                        color = NeonEmerald,
                        fontSize = 13.sp
                    )
                }
            } else {
                items(device.vulnerabilities) { vuln ->
                    VulnerabilityItemCard(vuln = vuln)
                }
            }
        }
    }
}

@Composable
fun VulnerabilityItemCard(vuln: VulnerabilityFinding) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (vuln.isResolved) NeonEmerald.copy(alpha = 0.08f) else CyberNavyCard
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (vuln.isResolved) NeonEmerald.copy(alpha = 0.5f) else CyberNavyBorder
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (vuln.cveId != null) {
                    Text(
                        text = vuln.cveId,
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                RiskBadge(level = if (vuln.isResolved) RiskLevel.SECURE else vuln.severity)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = vuln.title,
                color = TextPrimaryDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = vuln.description,
                color = TextSecondaryDark,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Impact
            Text(
                text = "THREAT IMPACT:",
                color = CriticalCrimson,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = vuln.impact,
                color = TextSecondaryDark,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Remediation
            Text(
                text = "REMEDIATION FIX:",
                color = NeonEmerald,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = vuln.remediation,
                color = TextPrimaryDark,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondaryDark, fontSize = 12.sp)
        Text(
            text = value,
            color = TextPrimaryDark,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
