package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.model.WifiConnectionState
import com.example.ui.components.ConnectedWifiStatusBar
import com.example.ui.components.RiskBadge
import com.example.ui.components.getDeviceIcon
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    devices: List<DeviceItem>,
    selectedCategory: DeviceCategory?,
    searchQuery: String,
    wifiState: WifiConnectionState,
    onDisconnectWifi: () -> Unit,
    onCategorySelected: (DeviceCategory?) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSelectDevice: (DeviceItem) -> Unit,
    onToggleIsolation: (String) -> Unit,
    onHardenAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredDevices = devices.filter { device ->
        (selectedCategory == null || device.category == selectedCategory) &&
                (searchQuery.isEmpty() ||
                        device.name.contains(searchQuery, ignoreCase = true) ||
                        device.vendor.contains(searchQuery, ignoreCase = true) ||
                        device.ipAddress.contains(searchQuery, ignoreCase = true))
    }

    val criticalCount = devices.count { it.currentRiskLevel == RiskLevel.CRITICAL }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Wi-Fi Header
        item {
            ConnectedWifiStatusBar(
                wifiState = wifiState,
                connectedDeviceCount = devices.size,
                onDisconnect = onDisconnectWifi
            )
        }
        // Screen Title
        item {
            Column {
                Text(
                    text = "APPLIANCE MATRIX",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Smart Home Appliances",
                    color = TextPrimaryDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Monitor vulnerabilities across Alexa, smart fans, lights, cameras, and deadbolts.",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("device_search_input"),
                placeholder = { Text("Search by appliance name, IP, or vendor...", color = TextTertiaryDark, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = CyberCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondaryDark)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberNavyBorder,
                    focusedContainerColor = CyberNavyCard,
                    unfocusedContainerColor = CyberNavyCard,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                ),
                singleLine = true
            )
        }

        // Category Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onCategorySelected(null) },
                        label = { Text("All (${devices.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan
                        )
                    )
                }
                items(DeviceCategory.values()) { cat ->
                    val count = devices.count { it.category == cat }
                    if (count > 0) {
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onCategorySelected(if (selectedCategory == cat) null else cat) },
                            label = { Text("${cat.displayName} ($count)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyberCyan
                            )
                        )
                    }
                }
            }
        }

        // "Harden All" Banner if vulnerabilities exist
        if (criticalCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CriticalCrimson.copy(alpha = 0.12f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CriticalCrimson))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$criticalCount High/Critical Vulnerabilities Detected",
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Alexa and smart fans have unpatched default configurations.",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = onHardenAll,
                            colors = ButtonDefaults.buttonColors(containerColor = CriticalCrimson),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("harden_all_btn")
                        ) {
                            Text("Harden All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Device Cards List
        items(filteredDevices) { device ->
            DetailedDeviceCard(
                device = device,
                onClick = { onSelectDevice(device) },
                onToggleIsolation = { onToggleIsolation(device.id) }
            )
        }
    }
}

@Composable
fun DetailedDeviceCard(
    device: DeviceItem,
    onClick: () -> Unit,
    onToggleIsolation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("device_card_${device.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                when (device.currentRiskLevel) {
                    RiskLevel.CRITICAL -> CriticalCrimson.copy(alpha = 0.7f)
                    RiskLevel.HIGH -> WarningAmber.copy(alpha = 0.7f)
                    RiskLevel.SECURE -> NeonEmerald.copy(alpha = 0.5f)
                    else -> CyberNavyBorder
                }
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Icon, Name, Category, Risk Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            when (device.currentRiskLevel) {
                                RiskLevel.CRITICAL -> CriticalCrimson.copy(alpha = 0.2f)
                                RiskLevel.HIGH -> WarningAmber.copy(alpha = 0.2f)
                                RiskLevel.SECURE -> NeonEmerald.copy(alpha = 0.2f)
                                else -> CyberCyan.copy(alpha = 0.2f)
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
                            RiskLevel.SECURE -> NeonEmerald
                            else -> CyberCyan
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        color = TextPrimaryDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = device.category.displayName,
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )
                }

                RiskBadge(level = device.currentRiskLevel)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Technical details row: IP, MAC, Firmware
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "IP: ${device.ipAddress}",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "FW: ${device.firmwareVersion}",
                    color = if (device.isFirmwareOutdated) WarningAmber else NeonEmerald,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Vulnerability tags
            if (device.vulnerabilities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val unresolvedCount = device.vulnerabilities.count { !it.isResolved }
                    if (unresolvedCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CriticalCrimson.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$unresolvedCount KNOWN VULNERABILITIES",
                                color = CriticalCrimson,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (device.hasDefaultPassword) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = WarningAmber.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "DEFAULT CREDENTIALS",
                                color = WarningAmber,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Risk Score Progress bar & Quick Isolation Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "RISK SCORE",
                            color = TextSecondaryDark,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${device.riskScore}/100",
                            color = when (device.currentRiskLevel) {
                                RiskLevel.CRITICAL -> CriticalCrimson
                                RiskLevel.HIGH -> WarningAmber
                                else -> NeonEmerald
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (device.riskScore / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = when (device.currentRiskLevel) {
                            RiskLevel.CRITICAL -> CriticalCrimson
                            RiskLevel.HIGH -> WarningAmber
                            else -> NeonEmerald
                        },
                        trackColor = Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                OutlinedButton(
                    onClick = onToggleIsolation,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (device.isIsolated) NeonEmerald else CyberCyan
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (device.isIsolated) NeonEmerald else CyberCyan
                        )
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = if (device.isIsolated) Icons.Default.Shield else Icons.Default.GppBad,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (device.isIsolated) "VLAN Isolated" else "Isolate",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
