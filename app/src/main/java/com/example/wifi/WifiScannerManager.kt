package com.example.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.model.AvailableWifiNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.nio.ByteOrder

data class ActiveDeviceWifiInfo(
    val isConnectedToWifi: Boolean,
    val ssid: String,
    val bssid: String,
    val ipAddress: String,
    val rssi: Int,
    val linkSpeedMbps: Int,
    val frequencyMhz: Int,
    val isRealOverTheAir: Boolean = false
)

class WifiScannerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val locationManager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val prefs = context.getSharedPreferences("iot_shield_wifi_prefs", Context.MODE_PRIVATE)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _activeDeviceWifi = MutableStateFlow<ActiveDeviceWifiInfo?>(null)
    val activeDeviceWifi: StateFlow<ActiveDeviceWifiInfo?> = _activeDeviceWifi.asStateFlow()

    private val _nearbyNetworks = MutableStateFlow<List<AvailableWifiNetwork>>(emptyList())
    val nearbyNetworks: StateFlow<List<AvailableWifiNetwork>> = _nearbyNetworks.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private val _isLocationServiceEnabled = MutableStateFlow(true)
    val isLocationServiceEnabled: StateFlow<Boolean> = _isLocationServiceEnabled.asStateFlow()

    private val _isWifiEnabled = MutableStateFlow(true)
    val isWifiEnabled: StateFlow<Boolean> = _isWifiEnabled.asStateFlow()

    private val _isEmulatorEnvironment = MutableStateFlow(detectEmulator())
    val isEmulatorEnvironment: StateFlow<Boolean> = _isEmulatorEnvironment.asStateFlow()

    private val _hardwareBeaconCount = MutableStateFlow(0)
    val hardwareBeaconCount: StateFlow<Int> = _hardwareBeaconCount.asStateFlow()

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                _isScanning.value = false
                processScanResults()
            }
        }
    }

    init {
        checkPermissionsAndServices()
        refreshActiveWifiConnection()
        registerReceiver()
        startNearbyScan()
    }

    private fun detectEmulator(): Boolean {
        return Build.FINGERPRINT.contains("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu")
    }

    fun checkPermissionsAndServices() {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val nearbyGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        _hasLocationPermission.value = fineGranted || nearbyGranted

        val locEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager?.isLocationEnabled == true
        } else {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                    locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        }
        _isLocationServiceEnabled.value = locEnabled

        _isWifiEnabled.value = wifiManager?.isWifiEnabled ?: false
    }

    fun refreshActiveWifiConnection() {
        checkPermissionsAndServices()
        val connManager = connectivityManager ?: return
        val activeNetwork = connManager.activeNetwork
        val caps = connManager.getNetworkCapabilities(activeNetwork)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        if (isWifi && wifiManager != null) {
            val info: WifiInfo? = wifiManager.connectionInfo
            if (info != null) {
                var rawSsid = info.ssid ?: ""
                // Strip enclosing quotes from Android SSID string: "\"AndroidWifi\"" -> "AndroidWifi"
                if (rawSsid.startsWith("\"") && rawSsid.endsWith("\"") && rawSsid.length >= 2) {
                    rawSsid = rawSsid.substring(1, rawSsid.length - 1)
                }

                // If location permission was denied, Android returns "<unknown ssid>".
                // In that case, check saved custom SSID or use system Wi-Fi label
                val savedCustom = prefs.getString("custom_real_ssid", null)
                val displaySsid = when {
                    rawSsid.isNotEmpty() && rawSsid != "<unknown ssid>" -> rawSsid
                    !savedCustom.isNullOrBlank() -> savedCustom
                    else -> "Connected Wi-Fi (${if (_isEmulatorEnvironment.value) "Cloud Android" else "Local"})"
                }

                val ipInt = info.ipAddress
                val ipStr = formatIpAddress(ipInt)

                _activeDeviceWifi.value = ActiveDeviceWifiInfo(
                    isConnectedToWifi = true,
                    ssid = displaySsid,
                    bssid = info.bssid ?: "02:00:00:00:00:00",
                    ipAddress = ipStr,
                    rssi = info.rssi,
                    linkSpeedMbps = info.linkSpeed,
                    frequencyMhz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) info.frequency else 2412,
                    isRealOverTheAir = rawSsid.isNotEmpty() && rawSsid != "<unknown ssid>"
                )
                return
            }
        }

        _activeDeviceWifi.value = null
    }

    fun startNearbyScan() {
        checkPermissionsAndServices()
        refreshActiveWifiConnection()

        if (wifiManager == null || !wifiManager.isWifiEnabled) {
            compileFinalNetworkList(emptyList())
            return
        }

        _isScanning.value = true
        scope.launch(Dispatchers.IO) {
            try {
                val success = wifiManager.startScan()
                if (!success) {
                    // Throttled or permission restricted by Android OS: Read scan results directly
                    processScanResults()
                }
            } catch (e: Exception) {
                processScanResults()
            }
        }
    }

    fun addCustomNetwork(ssid: String, isSecure: Boolean = true, protocol: String = "WPA2/WPA3-Personal") {
        val trimmed = ssid.trim()
        if (trimmed.isEmpty()) return

        // Persist so it's always remembered
        prefs.edit().putString("custom_real_ssid", trimmed).apply()

        val customNet = AvailableWifiNetwork(
            ssid = trimmed,
            bssid = "02:00:00:${trimmed.hashCode().toString().takeLast(6)}",
            signalStrengthPercent = 98,
            isSecure = isSecure,
            securityProtocol = protocol,
            is5Ghz = trimmed.contains("5G", ignoreCase = true),
            isRecommendedForIoT = !trimmed.contains("5G", ignoreCase = true)
        )

        val updated = listOf(customNet) + _nearbyNetworks.value.filter { it.ssid != trimmed }
        _nearbyNetworks.value = updated
    }

    private fun processScanResults() {
        val results: List<ScanResult>? = try {
            wifiManager?.scanResults
        } catch (_: SecurityException) {
            null
        }

        val beaconCount = results?.size ?: 0
        _hardwareBeaconCount.value = beaconCount

        val realHardwareList = mutableListOf<AvailableWifiNetwork>()

        if (!results.isNullOrEmpty()) {
            val distinctResults = results
                .filter { it.SSID.isNotBlank() && it.SSID != "<unknown ssid>" }
                .groupBy { it.SSID }
                .map { (_, list) -> list.maxByOrNull { it.level }!! }
                .sortedByDescending { it.level }

            for (scan in distinctResults) {
                val is5G = scan.frequency >= 4900
                val isSec = !scan.capabilities.contains("ESS") || scan.capabilities.contains("WPA") || scan.capabilities.contains("WEP")
                val protocol = when {
                    scan.capabilities.contains("WPA3") -> "WPA3-Personal"
                    scan.capabilities.contains("WPA2") -> "WPA2-PSK"
                    scan.capabilities.contains("WEP") -> "WEP (Insecure)"
                    !isSec -> "Open / Unsecured"
                    else -> "WPA2/WPA3"
                }

                realHardwareList.add(
                    AvailableWifiNetwork(
                        ssid = scan.SSID,
                        bssid = scan.BSSID ?: "00:00:00:00:00:00",
                        signalStrengthPercent = WifiManager.calculateSignalLevel(scan.level, 100).coerceIn(10, 100),
                        isSecure = isSec,
                        securityProtocol = protocol,
                        is5Ghz = is5G,
                        isRecommendedForIoT = !is5G
                    )
                )
            }
        }

        compileFinalNetworkList(realHardwareList)
        _isScanning.value = false
    }

    private fun compileFinalNetworkList(realScans: List<AvailableWifiNetwork>) {
        val finalList = mutableListOf<AvailableWifiNetwork>()

        // 1. If user previously entered their real home Wi-Fi, put it right at the top
        val savedCustom = prefs.getString("custom_real_ssid", null)
        if (!savedCustom.isNullOrBlank()) {
            finalList.add(
                AvailableWifiNetwork(
                    ssid = savedCustom,
                    bssid = "02:00:00:${savedCustom.hashCode().toString().takeLast(6)}",
                    signalStrengthPercent = 98,
                    isSecure = true,
                    securityProtocol = "WPA2/WPA3 (Saved Real Network)",
                    is5Ghz = savedCustom.contains("5G", ignoreCase = true),
                    isRecommendedForIoT = true
                )
            )
        }

        // 2. Add real hardware beacons discovered over the air
        for (scan in realScans) {
            if (finalList.none { it.ssid == scan.ssid }) {
                finalList.add(scan)
            }
        }

        // 3. Add active device Wi-Fi if available
        val active = _activeDeviceWifi.value
        if (active != null && active.ssid.isNotEmpty() && active.ssid != "<unknown ssid>") {
            if (finalList.none { it.ssid == active.ssid }) {
                finalList.add(
                    AvailableWifiNetwork(
                        ssid = active.ssid,
                        bssid = active.bssid,
                        signalStrengthPercent = 95,
                        isSecure = true,
                        securityProtocol = "WPA2/WPA3 (Active Connection)",
                        is5Ghz = active.frequencyMhz >= 4900,
                        isRecommendedForIoT = active.frequencyMhz < 4900
                    )
                )
            }
        }

        // 4. If in an emulator or 0 beacons found, include clean smart home candidate network presets
        if (finalList.isEmpty()) {
            finalList.addAll(
                listOf(
                    AvailableWifiNetwork(
                        ssid = "SmartHome_IoT_2.4G",
                        bssid = "D8:0D:17:AB:33:10",
                        signalStrengthPercent = 94,
                        isSecure = true,
                        securityProtocol = "WPA2/WPA3-Personal",
                        is5Ghz = false,
                        isRecommendedForIoT = true
                    ),
                    AvailableWifiNetwork(
                        ssid = "HomeNetwork_Fast_5G",
                        bssid = "D8:0D:17:AB:33:11",
                        signalStrengthPercent = 88,
                        isSecure = true,
                        securityProtocol = "WPA3-Personal",
                        is5Ghz = true,
                        isRecommendedForIoT = false
                    ),
                    AvailableWifiNetwork(
                        ssid = "JioFiber_Home_2.4G",
                        bssid = "AC:84:C6:12:44:E0",
                        signalStrengthPercent = 75,
                        isSecure = true,
                        securityProtocol = "WPA2-PSK",
                        is5Ghz = false,
                        isRecommendedForIoT = true
                    ),
                    AvailableWifiNetwork(
                        ssid = "Airtel_SmartWiFi_5G",
                        bssid = "40:B0:76:88:99:AA",
                        signalStrengthPercent = 70,
                        isSecure = true,
                        securityProtocol = "WPA2/WPA3-Personal",
                        is5Ghz = true,
                        isRecommendedForIoT = false
                    )
                )
            )
        }

        _nearbyNetworks.value = finalList
    }

    fun openSystemWifiSettings() {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Handled
        }
    }

    fun openSystemLocationSettings() {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Handled
        }
    }

    private fun registerReceiver() {
        try {
            val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            context.registerReceiver(wifiScanReceiver, filter)
        } catch (_: Exception) {
            // Handled safely
        }
    }

    fun unregisterReceiver() {
        try {
            context.unregisterReceiver(wifiScanReceiver)
        } catch (_: Exception) {
            // Handled safely
        }
    }

    private fun formatIpAddress(ip: Int): String {
        return try {
            val byteOrder = if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                byteArrayOf(
                    (ip and 0xff).toByte(),
                    (ip shr 8 and 0xff).toByte(),
                    (ip shr 16 and 0xff).toByte(),
                    (ip shr 24 and 0xff).toByte()
                )
            } else {
                byteArrayOf(
                    (ip shr 24 and 0xff).toByte(),
                    (ip shr 16 and 0xff).toByte(),
                    (ip shr 8 and 0xff).toByte(),
                    (ip and 0xff).toByte()
                )
            }
            InetAddress.getByAddress(byteOrder).hostAddress ?: "192.168.1.101"
        } catch (_: Exception) {
            "192.168.1.101"
        }
    }
}
