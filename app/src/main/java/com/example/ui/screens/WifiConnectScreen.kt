package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AvailableWifiNetwork
import com.example.model.WifiConnectionState
import com.example.ui.theme.*
import com.example.wifi.ActiveDeviceWifiInfo

@Composable
fun WifiConnectScreen(
    wifiState: WifiConnectionState,
    availableNetworks: List<AvailableWifiNetwork>,
    activeDeviceWifi: ActiveDeviceWifiInfo?,
    isScanning: Boolean,
    hasLocationPermission: Boolean,
    isLocationServiceEnabled: Boolean,
    isEmulatorEnvironment: Boolean,
    hardwareBeaconCount: Int,
    onRequestLocationPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onRefreshScan: () -> Unit,
    onAddAndConnectCustomNetwork: (String, String) -> Unit,
    onConnectToNetwork: (AvailableWifiNetwork) -> Unit,
    onConnectActiveDeviceWifi: () -> Unit,
    modifier: Modifier = Modifier
) {
    var quickSsidInput by remember { mutableStateOf("") }
    var quickPasswordInput by remember { mutableStateOf("") }
    var selectedNetwork by remember { mutableStateOf<AvailableWifiNetwork?>(null) }
    var networkPasswordInput by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "wifi_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wifiAlpha"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 48.dp)
    ) {
        // Hero Visualizer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .border(
                            2.dp,
                            if (wifiState.isConnecting || isScanning) CyberCyan.copy(alpha = pulseAlpha) else CyberCyan,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (wifiState.isConnecting || isScanning) Icons.Default.WifiFind else Icons.Default.Wifi,
                        contentDescription = "Wi-Fi",
                        tint = CyberCyan,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SMART HOME WI-FI GATEWAY",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Connect Your Home Wi-Fi",
                    color = TextPrimaryDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Select or enter your actual home Wi-Fi to scan connected smart appliances (Alexa, fan, deadbolt, camera) on your subnet.",
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 17.sp
                )
            }
        }

        // --- DIRECT INPUT CARD: Connect to YOUR Actual Wi-Fi Network ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("enter_actual_wifi_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101E38)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CyberCyan)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Router, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ENTER YOUR ACTUAL WI-FI NAME",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Connect to your home router directly",
                                color = TextPrimaryDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = quickSsidInput,
                        onValueChange = { quickSsidInput = it },
                        placeholder = { Text("Enter your real Wi-Fi name (e.g. MyHome_5G, JioFiber)", color = TextTertiaryDark, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null, tint = CyberCyan) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("actual_wifi_ssid_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberNavyBorder,
                            focusedContainerColor = Color(0xFF090E1C),
                            unfocusedContainerColor = Color(0xFF090E1C),
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = quickPasswordInput,
                        onValueChange = { quickPasswordInput = it },
                        placeholder = { Text("Wi-Fi Passphrase (optional)", color = TextTertiaryDark, fontSize = 12.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondaryDark) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("actual_wifi_password_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberNavyBorder,
                            focusedContainerColor = Color(0xFF090E1C),
                            unfocusedContainerColor = Color(0xFF090E1C),
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (quickSsidInput.isNotBlank()) {
                                onAddAndConnectCustomNetwork(quickSsidInput, quickPasswordInput)
                            }
                        },
                        enabled = quickSsidInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("connect_actual_wifi_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF1E2D4A),
                            disabledContentColor = TextSecondaryDark
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (quickSsidInput.isBlank()) "TYPE YOUR WI-FI NAME ABOVE" else "CONNECT TO '${quickSsidInput.uppercase()}' & AUDIT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Active Device Connection Detected Banner
        if (activeDeviceWifi != null && activeDeviceWifi.isConnectedToWifi) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_device_wifi_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0E223D)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CyberCyan)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.WifiTethering,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "ACTIVE SYSTEM NETWORK",
                                        color = CyberCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NeonEmerald.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "ONLINE",
                                            color = NeonEmerald,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                Text(
                                    text = activeDeviceWifi.ssid,
                                    color = TextPrimaryDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "IP: ${activeDeviceWifi.ipAddress} • Link: ${activeDeviceWifi.linkSpeedMbps} Mbps",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onConnectActiveDeviceWifi,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("use_active_device_wifi_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan.copy(alpha = 0.2f),
                                contentColor = CyberCyan
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "USE THIS ACTIVE CONNECTION (${activeDeviceWifi.ssid})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Emulator Environment Explanation Box
        if (isEmulatorEnvironment) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14192B)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberNavyBorder))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CLOUD EMULATOR ENVIRONMENT",
                                color = WarningAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This app is running in a Cloud Android streaming container. Because virtual cloud servers lack physical 802.11 radio antennas to capture radio waves in your house, enter your home Wi-Fi name in the card above or select from the networks below.",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Location Services Warning (Required on real phones for Wi-Fi scanning)
        if (!hasLocationPermission || !isLocationServiceEnabled) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261D10)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(WarningAmber)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (!hasLocationPermission) "Location Permission Required" else "Location Service is OFF",
                                color = TextPrimaryDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (!hasLocationPermission)
                                    "Android blocks Wi-Fi SSID detection unless Location permission is granted."
                                else
                                    "Turn ON Location in Android settings to allow 802.11 beacon scans.",
                                color = TextSecondaryDark,
                                fontSize = 10.sp
                            )
                        }
                        Button(
                            onClick = {
                                if (!hasLocationPermission) onRequestLocationPermission() else onOpenLocationSettings()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("enable_location_action_btn")
                        ) {
                            Text(if (!hasLocationPermission) "Grant" else "Turn On", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Connecting Progress Banner
        if (wifiState.isConnecting) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CyberCyan)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = CyberCyan,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Connecting & Negotiating Subnet...",
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Probing gateway 192.168.1.1 and inspecting IoT smart appliances...",
                                color = TextSecondaryDark,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Available Networks Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SELECT FROM DETECTED NETWORKS (${availableNetworks.size})",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    if (hardwareBeaconCount > 0) {
                        Text(
                            text = "$hardwareBeaconCount live hardware beacons discovered",
                            color = NeonEmerald,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = onRefreshScan,
                        enabled = !isScanning,
                        modifier = Modifier.testTag("refresh_wifi_scan_btn")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(10.dp), color = CyberCyan, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scanning...", fontSize = 11.sp, color = CyberCyan)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(13.dp), tint = CyberCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan", fontSize = 11.sp, color = CyberCyan)
                        }
                    }

                    TextButton(
                        onClick = onOpenWifiSettings,
                        modifier = Modifier.testTag("open_system_wifi_btn")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(13.dp), tint = TextSecondaryDark)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Settings", fontSize = 11.sp, color = TextSecondaryDark)
                    }
                }
            }
        }

        // Networks List
        items(availableNetworks) { network ->
            val isSelected = selectedNetwork?.ssid == network.ssid

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !wifiState.isConnecting) {
                        selectedNetwork = network
                    }
                    .testTag("wifi_item_${network.ssid}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF132240) else CyberNavyCard
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isSelected) CyberCyan else CyberNavyBorder
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (network.isRecommendedForIoT) NeonEmerald.copy(alpha = 0.15f) else CyberCyan.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (network.isSecure) Icons.Default.WifiLock else Icons.Default.Wifi,
                                contentDescription = null,
                                tint = if (network.isRecommendedForIoT) NeonEmerald else CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = network.ssid,
                                    color = TextPrimaryDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (network.isRecommendedForIoT) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NeonEmerald.copy(alpha = 0.2f))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "RECOMMENDED",
                                            color = NeonEmerald,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${network.securityProtocol} • Signal ${network.signalStrengthPercent}%",
                                color = TextSecondaryDark,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedNetwork = network },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = CyberCyan,
                                unselectedColor = TextSecondaryDark
                            )
                        )
                    }

                    // Password input if selected
                    AnimatedVisibility(visible = isSelected) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            OutlinedTextField(
                                value = networkPasswordInput,
                                onValueChange = { networkPasswordInput = it },
                                placeholder = { Text("Enter WPA Passphrase (optional)...", fontSize = 11.sp, color = TextTertiaryDark) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wifi_password_input"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = CyberNavyBorder,
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { onConnectToNetwork(network) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("connect_wifi_submit_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberCyan,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CONNECT & AUDIT THIS NETWORK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
