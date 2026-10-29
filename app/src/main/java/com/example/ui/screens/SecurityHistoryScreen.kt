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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.IntrusionAlertEntity
import com.example.data.local.entity.ScanResultEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SecurityHistoryScreen(
    scanHistory: List<ScanResultEntity>,
    intrusionAlerts: List<IntrusionAlertEntity>,
    onClearScans: () -> Unit,
    onClearAlerts: () -> Unit,
    onDeleteScan: (Long) -> Unit,
    onDeleteAlert: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Scans, 1: Intrusion Alerts
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Title Header
        item {
            Column {
                Text(
                    text = "SECURITY EVENT REPOSITORY (ROOM DB)",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Security History & Logs",
                    color = TextPrimaryDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Persisted database of smart home vulnerability audits and blocked intrusion events.",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }
        }

        // Tab Selector Row
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CyberNavyCard,
                contentColor = CyberCyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("history_tab_row")
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Scan Audits (${scanHistory.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Intrusions (${intrusionAlerts.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }

        // Clear All Action Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedTab == 0) "PAST VULNERABILITY AUDITS" else "BLOCKED INTRUSION INCIDENTS",
                    color = TextSecondaryDark,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                TextButton(
                    onClick = { if (selectedTab == 0) onClearScans() else onClearAlerts() }
                ) {
                    Text("Clear Log", color = CriticalCrimson, fontSize = 11.sp)
                }
            }
        }

        // Tab Content
        if (selectedTab == 0) {
            // Scan History Records
            if (scanHistory.isEmpty()) {
                item {
                    EmptyHistoryBox("No past scan audits recorded in Room yet. Run a scan from the Scanner tab to create persistent audit history.")
                }
            } else {
                items(scanHistory, key = { it.id }) { scan ->
                    ScanHistoryItemCard(
                        scan = scan,
                        formattedDate = dateFormat.format(Date(scan.timestamp)),
                        onDelete = { onDeleteScan(scan.id) }
                    )
                }
            }
        } else {
            // Intrusion Alert Records
            if (intrusionAlerts.isEmpty()) {
                item {
                    EmptyHistoryBox("No intrusion alerts recorded in Room yet. Use the Attack Simulation Lab on the Shield tab to test detection.")
                }
            } else {
                items(intrusionAlerts, key = { it.id }) { alert ->
                    IntrusionHistoryItemCard(
                        alert = alert,
                        formattedDate = dateFormat.format(Date(alert.timestamp)),
                        onDelete = { onDeleteAlert(alert.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ScanHistoryItemCard(
    scan: ScanResultEntity,
    formattedDate: String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scan_record_${scan.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberNavyBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Radar, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedDate,
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${scan.totalDevicesScanned} Devices Audited",
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Defense Score: ${scan.homeDefenseScore}%",
                    color = if (scan.homeDefenseScore >= 70) NeonEmerald else WarningAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (scan.criticalCount > 0) {
                    PillTag("${scan.criticalCount} Critical", CriticalCrimson)
                }
                if (scan.highCount > 0) {
                    PillTag("${scan.highCount} High", Color(0xFFF97316))
                }
                if (scan.mediumCount > 0) {
                    PillTag("${scan.mediumCount} Medium", WarningAmber)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = scan.summary,
                color = TextSecondaryDark,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            if (scan.flaggedDeviceNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Flagged: ${scan.flaggedDeviceNames}",
                    color = TextSecondaryDark,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun IntrusionHistoryItemCard(
    alert: IntrusionAlertEntity,
    formattedDate: String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alert_record_${alert.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (alert.severity == "CRITICAL") CriticalCrimson.copy(alpha = 0.5f) else CyberNavyBorder
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = CriticalCrimson, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedDate,
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = alert.title,
                color = TextPrimaryDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Target: ${alert.targetDeviceName} • Attacker: ${alert.attackerIp}",
                color = TextSecondaryDark,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = alert.technicalDetails,
                color = TextSecondaryDark,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun PillTag(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun EmptyHistoryBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberNavyCard)
            .border(1.dp, CyberNavyBorder, RoundedCornerShape(12.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = TextSecondaryDark,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 17.sp
        )
    }
}
