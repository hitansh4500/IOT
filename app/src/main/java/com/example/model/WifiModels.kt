package com.example.model

data class AvailableWifiNetwork(
    val ssid: String,
    val bssid: String,
    val signalStrengthPercent: Int,
    val isSecure: Boolean,
    val securityProtocol: String,
    val is5Ghz: Boolean = false,
    val isRecommendedForIoT: Boolean = false
)

data class WifiConnectionState(
    val isConnected: Boolean = false,
    val currentSsid: String? = null,
    val currentBssid: String? = null,
    val localIp: String = "",
    val gatewayIp: String = "192.168.1.1",
    val subnetMask: String = "255.255.255.0",
    val signalDbm: Int = -52,
    val isConnecting: Boolean = false,
    val connectionError: String? = null
)
