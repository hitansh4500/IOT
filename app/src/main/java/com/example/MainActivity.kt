package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.HomeShieldViewModel
import com.example.viewmodel.NavigationTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HomeShieldApp()
            }
        }
    }
}

@Composable
fun HomeShieldApp(viewModel: HomeShieldViewModel = viewModel()) {
    val context = LocalContext.current

    // Request permissions (Location for real Wi-Fi scanning, Notification for IDS threat alerts)
    val permissionsToRequest = remember {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
            list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.checkLocationPermission()
        viewModel.scanNearbyWifi()
    }

    LaunchedEffect(Unit) {
        val needsLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

        if (needsLocation) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    val wifiState by viewModel.wifiState.collectAsStateWithLifecycle()
    val availableNetworks by viewModel.availableNetworks.collectAsStateWithLifecycle()
    val isWifiScanning by viewModel.isWifiScanning.collectAsStateWithLifecycle()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsStateWithLifecycle()
    val isLocationServiceEnabled by viewModel.isLocationServiceEnabled.collectAsStateWithLifecycle()
    val isEmulatorEnvironment by viewModel.isEmulatorEnvironment.collectAsStateWithLifecycle()
    val hardwareBeaconCount by viewModel.hardwareBeaconCount.collectAsStateWithLifecycle()
    val activeDeviceWifi by viewModel.activeDeviceWifi.collectAsStateWithLifecycle()

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val threats by viewModel.threats.collectAsStateWithLifecycle()
    val hardeningGuides by viewModel.hardeningGuides.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val scannerState by viewModel.scannerState.collectAsStateWithLifecycle()
    val defenseScore by viewModel.overallDefenseScore.collectAsStateWithLifecycle()

    val scanHistory by viewModel.scanHistory.collectAsStateWithLifecycle()
    val intrusionAlerts by viewModel.intrusionAlertsHistory.collectAsStateWithLifecycle()

    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
    val selectedGuide by viewModel.selectedGuide.collectAsStateWithLifecycle()
    val showAttackDialog by viewModel.showAttackSimulationDialog.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.deviceCategoryFilter.collectAsStateWithLifecycle()
    val deviceSearchQuery by viewModel.deviceSearchQuery.collectAsStateWithLifecycle()
    val guideSearchQuery by viewModel.guideSearchQuery.collectAsStateWithLifecycle()

    // Back handling
    BackHandler(enabled = selectedDevice != null || selectedGuide != null || showAttackDialog || selectedTab != NavigationTab.DASHBOARD) {
        when {
            selectedDevice != null -> viewModel.selectDevice(null)
            selectedGuide != null -> viewModel.selectGuide(null)
            showAttackDialog -> viewModel.setShowAttackSimulationDialog(false)
            selectedTab != NavigationTab.DASHBOARD -> viewModel.selectTab(NavigationTab.DASHBOARD)
        }
    }

    // Step 1: If NOT connected to Wi-Fi, enforce the Wi-Fi connection prompt first!
    if (!wifiState.isConnected) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = CyberNavyDark,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { paddingValues ->
            WifiConnectScreen(
                wifiState = wifiState,
                availableNetworks = availableNetworks,
                activeDeviceWifi = activeDeviceWifi,
                isScanning = isWifiScanning,
                hasLocationPermission = hasLocationPermission,
                isLocationServiceEnabled = isLocationServiceEnabled,
                isEmulatorEnvironment = isEmulatorEnvironment,
                hardwareBeaconCount = hardwareBeaconCount,
                onRequestLocationPermission = { permissionLauncher.launch(permissionsToRequest) },
                onOpenLocationSettings = { viewModel.openSystemLocationSettings() },
                onOpenWifiSettings = { viewModel.openSystemWifiSettings() },
                onRefreshScan = { viewModel.scanNearbyWifi() },
                onAddAndConnectCustomNetwork = { ssid, _ ->
                    viewModel.addCustomWifiNetwork(ssid, true, "WPA2/WPA3 (Home Network)")
                    viewModel.connectToWifi(
                        com.example.model.AvailableWifiNetwork(
                            ssid = ssid,
                            bssid = "02:00:00:${ssid.hashCode().toString().takeLast(6)}",
                            signalStrengthPercent = 98,
                            isSecure = true,
                            securityProtocol = "WPA2/WPA3 (Home Network)",
                            isRecommendedForIoT = true
                        )
                    )
                },
                onConnectToNetwork = { network -> viewModel.connectToWifi(network) },
                onConnectActiveDeviceWifi = { viewModel.connectToActiveDeviceWifi() },
                modifier = Modifier.padding(paddingValues)
            )
        }
        return
    }

    // Step 2: Once connected to Wi-Fi, show the complete smart home cyber defense app!
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CyberNavyDark,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = CyberNavySurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(1.dp, CyberNavyBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.DASHBOARD,
                    onClick = { viewModel.selectTab(NavigationTab.DASHBOARD) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.DASHBOARD) Icons.Filled.Shield else Icons.Outlined.Shield,
                            contentDescription = "Shield"
                        )
                    },
                    label = { Text("Shield", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan.copy(alpha = 0.2f),
                        unselectedIconColor = TextSecondaryDark,
                        unselectedTextColor = TextSecondaryDark
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                NavigationBarItem(
                    selected = selectedTab == NavigationTab.DEVICES,
                    onClick = { viewModel.selectTab(NavigationTab.DEVICES) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.DEVICES) Icons.Filled.DevicesOther else Icons.Outlined.DevicesOther,
                            contentDescription = "Appliances"
                        )
                    },
                    label = { Text("Appliances", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan.copy(alpha = 0.2f),
                        unselectedIconColor = TextSecondaryDark,
                        unselectedTextColor = TextSecondaryDark
                    ),
                    modifier = Modifier.testTag("nav_tab_devices")
                )

                NavigationBarItem(
                    selected = selectedTab == NavigationTab.SCANNER,
                    onClick = { viewModel.selectTab(NavigationTab.SCANNER) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.SCANNER) Icons.Filled.Radar else Icons.Outlined.Radar,
                            contentDescription = "Scanner"
                        )
                    },
                    label = { Text("Scanner", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan.copy(alpha = 0.2f),
                        unselectedIconColor = TextSecondaryDark,
                        unselectedTextColor = TextSecondaryDark
                    ),
                    modifier = Modifier.testTag("nav_tab_scanner")
                )

                NavigationBarItem(
                    selected = selectedTab == NavigationTab.GUIDES,
                    onClick = { viewModel.selectTab(NavigationTab.GUIDES) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.GUIDES) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                            contentDescription = "Hardening"
                        )
                    },
                    label = { Text("Hardening", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan.copy(alpha = 0.2f),
                        unselectedIconColor = TextSecondaryDark,
                        unselectedTextColor = TextSecondaryDark
                    ),
                    modifier = Modifier.testTag("nav_tab_guides")
                )

                NavigationBarItem(
                    selected = selectedTab == NavigationTab.HISTORY,
                    onClick = { viewModel.selectTab(NavigationTab.HISTORY) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "Logs"
                        )
                    },
                    label = { Text("Logs", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan.copy(alpha = 0.2f),
                        unselectedIconColor = TextSecondaryDark,
                        unselectedTextColor = TextSecondaryDark
                    ),
                    modifier = Modifier.testTag("nav_tab_history")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                NavigationTab.DASHBOARD -> {
                    DashboardScreen(
                        defenseScore = defenseScore,
                        devices = devices,
                        threats = threats,
                        telemetry = telemetry,
                        wifiState = wifiState,
                        onDisconnectWifi = { viewModel.disconnectWifi() },
                        onToggleLockdown = { viewModel.togglePanicLockdown() },
                        onStartScan = {
                            viewModel.selectTab(NavigationTab.SCANNER)
                            viewModel.runVulnerabilityScan()
                        },
                        onOpenAttackSim = { viewModel.setShowAttackSimulationDialog(true) },
                        onSelectDevice = { dev -> viewModel.selectDevice(dev) },
                        onNavigateToDevices = { viewModel.selectTab(NavigationTab.DEVICES) }
                    )
                }
                NavigationTab.DEVICES -> {
                    DevicesScreen(
                        devices = devices,
                        selectedCategory = categoryFilter,
                        searchQuery = deviceSearchQuery,
                        wifiState = wifiState,
                        onDisconnectWifi = { viewModel.disconnectWifi() },
                        onCategorySelected = { cat -> viewModel.setDeviceCategoryFilter(cat) },
                        onSearchChanged = { q -> viewModel.setDeviceSearchQuery(q) },
                        onSelectDevice = { dev -> viewModel.selectDevice(dev) },
                        onToggleIsolation = { devId -> viewModel.toggleDeviceIsolation(devId) },
                        onHardenAll = { viewModel.hardenAll() }
                    )
                }
                NavigationTab.SCANNER -> {
                    ScannerScreen(
                        scannerState = scannerState,
                        devices = devices,
                        onStartScan = { viewModel.runVulnerabilityScan() },
                        onSelectDevice = { dev -> viewModel.selectDevice(dev) },
                        onHardenDevice = { devId -> viewModel.hardenDevice(devId) },
                        onHardenAll = { viewModel.hardenAll() }
                    )
                }
                NavigationTab.GUIDES -> {
                    HardeningScreen(
                        guides = hardeningGuides,
                        searchQuery = guideSearchQuery,
                        onSearchChanged = { q -> viewModel.setGuideSearchQuery(q) },
                        onSelectGuide = { g -> viewModel.selectGuide(g) }
                    )
                }
                NavigationTab.HISTORY -> {
                    SecurityHistoryScreen(
                        scanHistory = scanHistory,
                        intrusionAlerts = intrusionAlerts,
                        onClearScans = { viewModel.clearScanHistory() },
                        onClearAlerts = { viewModel.clearAlertHistory() },
                        onDeleteScan = { id -> viewModel.deleteScanRecord(id) },
                        onDeleteAlert = { id -> viewModel.deleteAlertRecord(id) }
                    )
                }
            }
        }
    }

    // Device Detail Bottom Sheet
    selectedDevice?.let { device ->
        DeviceDetailModal(
            device = device,
            onDismiss = { viewModel.selectDevice(null) },
            onToggleIsolation = { viewModel.toggleDeviceIsolation(device.id) },
            onToggleVoiceBridge = { viewModel.toggleVoiceBridgeRestriction(device.id) },
            onHardenDevice = { viewModel.hardenDevice(device.id) }
        )
    }

    // Guide Detail Bottom Sheet
    selectedGuide?.let { guide ->
        GuideDetailModal(
            guide = guide,
            onDismiss = { viewModel.selectGuide(null) },
            onToggleStep = { stepNum -> viewModel.toggleHardeningStep(guide.id, stepNum) }
        )
    }

    // Attack Simulator Dialog
    if (showAttackDialog) {
        AttackSimulationDialog(
            onDismiss = { viewModel.setShowAttackSimulationDialog(false) },
            onSimulate = { vectorId -> viewModel.simulateAttack(vectorId) }
        )
    }
}
