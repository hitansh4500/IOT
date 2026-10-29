package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HomeSecurityRepository
import com.example.data.local.HomeSecurityDatabase
import com.example.data.local.entity.IntrusionAlertEntity
import com.example.data.local.entity.ScanResultEntity
import com.example.ids.IntrusionDetectionEngine
import com.example.ids.LiveTrafficPacket
import com.example.ids.ThreatNotificationHelper
import com.example.model.*
import com.example.scanner.ScannerState
import com.example.scanner.VulnerabilityScannerEngine
import com.example.wifi.ActiveDeviceWifiInfo
import com.example.wifi.WifiScannerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NavigationTab(val label: String) {
    DASHBOARD("Shield"),
    DEVICES("Appliances"),
    SCANNER("Scanner"),
    GUIDES("Hardening"),
    HISTORY("Logs")
}

class HomeShieldViewModel(application: Application) : AndroidViewModel(application) {

    private val database = HomeSecurityDatabase.getInstance(application)
    private val repository = HomeSecurityRepository(database.securityHistoryDao(), viewModelScope)
    private val notificationHelper = ThreatNotificationHelper(application.applicationContext)
    private val idsEngine = IntrusionDetectionEngine(repository, notificationHelper, viewModelScope)
    private val scannerEngine = VulnerabilityScannerEngine(repository)
    private val wifiScannerManager = WifiScannerManager(application, viewModelScope)

    val devices: StateFlow<List<DeviceItem>> = repository.devices
    val threats: StateFlow<List<NetworkThreat>> = repository.threats
    val hardeningGuides: StateFlow<List<HardeningGuide>> = repository.hardeningGuides
    val telemetry: StateFlow<NetworkTelemetry> = repository.telemetry
    val scannerState: StateFlow<ScannerState> = scannerEngine.scannerState
    val livePackets: StateFlow<List<LiveTrafficPacket>> = idsEngine.livePackets

    // Real Wi-Fi Scanner States
    val isWifiScanning: StateFlow<Boolean> = wifiScannerManager.isScanning
    val hasLocationPermission: StateFlow<Boolean> = wifiScannerManager.hasLocationPermission
    val isLocationServiceEnabled: StateFlow<Boolean> = wifiScannerManager.isLocationServiceEnabled
    val isWifiHardwareEnabled: StateFlow<Boolean> = wifiScannerManager.isWifiEnabled
    val isEmulatorEnvironment: StateFlow<Boolean> = wifiScannerManager.isEmulatorEnvironment
    val hardwareBeaconCount: StateFlow<Int> = wifiScannerManager.hardwareBeaconCount
    val activeDeviceWifi: StateFlow<ActiveDeviceWifiInfo?> = wifiScannerManager.activeDeviceWifi
    val availableNetworks: StateFlow<List<AvailableWifiNetwork>> = wifiScannerManager.nearbyNetworks

    fun openSystemWifiSettings() = wifiScannerManager.openSystemWifiSettings()
    fun openSystemLocationSettings() = wifiScannerManager.openSystemLocationSettings()

    // Wi-Fi Connection State
    val wifiState: StateFlow<WifiConnectionState> = repository.wifiState

    // Room DB History
    val scanHistory: StateFlow<List<ScanResultEntity>> = repository.scanHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val intrusionAlertsHistory: StateFlow<List<IntrusionAlertEntity>> = repository.intrusionAlertsHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overallDefenseScore: StateFlow<Int> = devices.combine(telemetry) { devList, tel ->
        val baseScore = repository.calculateOverallDefenseScore(devList)
        if (tel.panicLockdownActive) {
            (baseScore + 15).coerceAtMost(100)
        } else {
            baseScore
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 54)

    private val _selectedTab = MutableStateFlow(NavigationTab.DASHBOARD)
    val selectedTab: StateFlow<NavigationTab> = _selectedTab.asStateFlow()

    private val _selectedDevice = MutableStateFlow<DeviceItem?>(null)
    val selectedDevice: StateFlow<DeviceItem?> = _selectedDevice.asStateFlow()

    private val _selectedGuide = MutableStateFlow<HardeningGuide?>(null)
    val selectedGuide: StateFlow<HardeningGuide?> = _selectedGuide.asStateFlow()

    private val _showAttackSimulationDialog = MutableStateFlow(false)
    val showAttackSimulationDialog: StateFlow<Boolean> = _showAttackSimulationDialog.asStateFlow()

    private val _deviceCategoryFilter = MutableStateFlow<DeviceCategory?>(null)
    val deviceCategoryFilter: StateFlow<DeviceCategory?> = _deviceCategoryFilter.asStateFlow()

    private val _deviceSearchQuery = MutableStateFlow("")
    val deviceSearchQuery: StateFlow<String> = _deviceSearchQuery.asStateFlow()

    private val _guideSearchQuery = MutableStateFlow("")
    val guideSearchQuery: StateFlow<String> = _guideSearchQuery.asStateFlow()

    fun selectTab(tab: NavigationTab) {
        _selectedTab.value = tab
    }

    fun selectDevice(device: DeviceItem?) {
        _selectedDevice.value = device
    }

    fun selectGuide(guide: HardeningGuide?) {
        _selectedGuide.value = guide
    }

    fun setShowAttackSimulationDialog(show: Boolean) {
        _showAttackSimulationDialog.value = show
    }

    fun setDeviceCategoryFilter(category: DeviceCategory?) {
        _deviceCategoryFilter.value = category
    }

    fun setDeviceSearchQuery(query: String) {
        _deviceSearchQuery.value = query
    }

    fun setGuideSearchQuery(query: String) {
        _guideSearchQuery.value = query
    }

    // Real Wi-Fi Scanner Actions
    fun scanNearbyWifi() {
        wifiScannerManager.startNearbyScan()
    }

    fun checkLocationPermission() {
        wifiScannerManager.checkPermissionsAndServices()
    }

    fun addCustomWifiNetwork(ssid: String, isSecure: Boolean, protocol: String) {
        wifiScannerManager.addCustomNetwork(ssid, isSecure, protocol)
    }

    fun connectToActiveDeviceWifi() {
        val active = activeDeviceWifi.value
        if (active != null) {
            val network = AvailableWifiNetwork(
                ssid = active.ssid,
                bssid = active.bssid,
                signalStrengthPercent = 95,
                isSecure = true,
                securityProtocol = "WPA2/WPA3 (Active)",
                is5Ghz = active.frequencyMhz >= 4900,
                isRecommendedForIoT = true
            )
            repository.connectToWifi(network)
        }
    }

    // Wi-Fi Connection Actions
    fun connectToWifi(network: AvailableWifiNetwork) {
        repository.connectToWifi(network)
    }

    fun disconnectWifi() {
        repository.disconnectWifi()
        wifiScannerManager.refreshActiveWifiConnection()
    }

    fun toggleDeviceIsolation(deviceId: String) {
        repository.toggleDeviceIsolation(deviceId)
        _selectedDevice.value = devices.value.find { it.id == deviceId }
    }

    fun toggleVoiceBridgeRestriction(deviceId: String) {
        repository.toggleVoiceBridgeRestriction(deviceId)
        _selectedDevice.value = devices.value.find { it.id == deviceId }
    }

    fun hardenDevice(deviceId: String) {
        repository.hardenDevice(deviceId)
        _selectedDevice.value = devices.value.find { it.id == deviceId }
    }

    fun hardenAll() {
        repository.hardenAllDevices()
    }

    fun togglePanicLockdown() {
        repository.togglePanicLockdown()
    }

    fun toggleHardeningStep(guideId: String, stepNumber: Int) {
        repository.toggleHardeningStep(guideId, stepNumber)
        _selectedGuide.value = hardeningGuides.value.find { it.id == guideId }
    }

    fun runVulnerabilityScan() {
        viewModelScope.launch {
            scannerEngine.executeVulnerabilityScan()
        }
    }

    fun simulateAttack(vectorType: Int) {
        idsEngine.simulateAttackVector(vectorType)
        _showAttackSimulationDialog.value = false
    }

    // Room DB History Actions
    fun deleteScanRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteScanResult(id)
        }
    }

    fun deleteAlertRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteAlert(id)
        }
    }

    fun clearScanHistory() {
        viewModelScope.launch {
            repository.clearScanHistory()
        }
    }

    fun clearAlertHistory() {
        viewModelScope.launch {
            repository.clearAlertHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        wifiScannerManager.unregisterReceiver()
    }
}
