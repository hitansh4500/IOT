package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.model.RiskLevel
import com.example.model.ScanStage
import com.example.scanner.ScannerState
import com.example.ui.components.RiskBadge
import com.example.ui.components.getDeviceIcon
import com.example.ui.theme.*

@Composable
fun ScannerScreen(
    scannerState: ScannerState,
    devices: List<DeviceItem>,
    onStartScan: () -> Unit,
    onSelectDevice: (DeviceItem) -> Unit,
    onHardenDevice: (String) -> Unit,
    onHardenAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val terminalListState = rememberLazyListState()

    LaunchedEffect(scannerState.terminalLogs.size) {
        if (scannerState.terminalLogs.isNotEmpty()) {
            terminalListState.animateScrollToItem(scannerState.terminalLogs.size - 1)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Title Header
        item {
            Column {
                Text(
                    text = "VULNERABILITY AUDIT MODULE",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Network & Device Scanner",
                    color = TextPrimaryDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Scans ARP, open ports, factory credentials, voice assistant overrides, and firmware CVEs.",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }
        }

        // Scan Action Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (scannerState.isScanning) CyberCyan else CyberNavyBorder)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (scannerState.isScanning) "SCAN IN PROGRESS..." else "READY FOR AUDIT",
                                color = if (scannerState.isScanning) CyberCyan else TextPrimaryDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (scannerState.isScanning)
                                    scannerState.currentStage.description
                                else
                                    "Tap below to run deep multi-phase vulnerability scan across all smart devices.",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                        }

                        if (scannerState.isScanning) {
                            CircularProgressIndicator(
                                progress = { scannerState.progressPercent },
                                modifier = Modifier.size(36.dp),
                                color = CyberCyan,
                                trackColor = Color(0xFF1E293B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (scannerState.isScanning) {
                        LinearProgressIndicator(
                            progress = { scannerState.progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = CyberCyan,
                            trackColor = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PHASE: ${scannerState.currentStage.stageName.uppercase()}",
                                color = CyberCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(scannerState.progressPercent * 100).toInt()}%",
                                color = TextPrimaryDark,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Button(
                            onClick = onStartScan,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_vulnerability_scan_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "START COMPREHENSIVE VULNERABILITY AUDIT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Live Diagnostic Terminal Output
        if (scannerState.terminalLogs.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "SCANNER CONSOLE LOG",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF060913))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        LazyColumn(
                            state = terminalListState,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(scannerState.terminalLogs) { log ->
                                val logColor = when {
                                    log.contains("[!]") || log.contains("ALERT") || log.contains("CRITICAL") -> CriticalCrimson
                                    log.contains("[+]") || log.contains("SUCCESS") -> NeonEmerald
                                    log.contains("[*]") -> CyberCyan
                                    else -> TextSecondaryDark
                                }
                                Text(
                                    text = log,
                                    color = logColor,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Scan Results / Findings Overview
        val report = scannerState.lastReport
        if (report != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberNavyBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AUDIT FINDINGS MATRIX",
                                color = TextPrimaryDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Home Risk: ${report.overallHomeRiskScore}%",
                                color = if (report.overallHomeRiskScore > 40) CriticalCrimson else NeonEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4 Stat Counters Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricPill(
                                label = "CRITICAL",
                                count = "${report.criticalVulnsCount}",
                                color = CriticalCrimson,
                                modifier = Modifier.weight(1f)
                            )
                            MetricPill(
                                label = "DEFAULT PWD",
                                count = "${report.devicesWithDefaultPasswords.size}",
                                color = WarningAmber,
                                modifier = Modifier.weight(1f)
                            )
                            MetricPill(
                                label = "OUTDATED FW",
                                count = "${report.devicesWithOutdatedFirmware.size}",
                                color = Color(0xFFF97316),
                                modifier = Modifier.weight(1f)
                            )
                            MetricPill(
                                label = "OPEN PORTS",
                                count = "${report.devicesWithExposedPorts.size}",
                                color = CyberCyan,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "TOP PRIORITY RECOMMENDATION:",
                            color = WarningAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = report.topRecommendation,
                            color = TextPrimaryDark,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Per-Device Risk Score & Hardening List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Appliance Vulnerability Breakdown",
                    color = TextPrimaryDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onHardenAll) {
                    Text("Auto-Fix All", color = CyberCyan, fontSize = 12.sp)
                }
            }
        }

        items(devices.sortedByDescending { it.riskScore }) { device ->
            DeviceVulnerabilityCard(
                device = device,
                onClick = { onSelectDevice(device) },
                onHarden = { onHardenDevice(device.id) }
            )
        }
    }
}

@Composable
fun MetricPill(
    label: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count,
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = label,
                color = TextSecondaryDark,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun DeviceVulnerabilityCard(
    device: DeviceItem,
    onClick: () -> Unit,
    onHarden: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("scan_device_card_${device.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                when (device.currentRiskLevel) {
                    RiskLevel.CRITICAL -> CriticalCrimson.copy(alpha = 0.6f)
                    RiskLevel.HIGH -> WarningAmber.copy(alpha = 0.6f)
                    else -> CyberNavyBorder
                }
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        color = TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${device.ipAddress} • Risk Score: ${device.riskScore}/100",
                        color = when (device.currentRiskLevel) {
                            RiskLevel.CRITICAL -> CriticalCrimson
                            RiskLevel.HIGH -> WarningAmber
                            else -> NeonEmerald
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                RiskBadge(level = device.currentRiskLevel)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Identified specific vulnerabilities
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (device.hasDefaultPassword) {
                    ThreatBullet("Default Credentials active (admin:admin / 123456)")
                }
                if (device.isFirmwareOutdated) {
                    ThreatBullet("Firmware ${device.firmwareVersion} contains known CVE buffer overflow")
                }
                if (device.hasOpenVulnerablePorts) {
                    ThreatBullet("Exposed Ports: ${device.openPorts.joinToString(", ")} unfirewalled")
                }
                if (device.category == com.example.model.DeviceCategory.VOICE_ASSISTANT) {
                    ThreatBullet("Alexa voice actuation bridge to physical door locks lacks PIN")
                }
                if (!device.hasDefaultPassword && !device.isFirmwareOutdated && !device.hasOpenVulnerablePorts) {
                    Text(
                        text = "• All firmware and port perimeters verified secure",
                        color = NeonEmerald,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CyberNavyBorder)
                    )
                ) {
                    Text("Inspect Audit", fontSize = 11.sp)
                }

                Button(
                    onClick = onHarden,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (device.isHardened) NeonEmerald else CyberCyan,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = if (device.isHardened) "Secured" else "Auto-Harden",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ThreatBullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text = "•", color = CriticalCrimson, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = TextSecondaryDark, fontSize = 11.sp)
    }
}
