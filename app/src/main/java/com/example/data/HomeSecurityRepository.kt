package com.example.data

import com.example.data.local.dao.SecurityHistoryDao
import com.example.data.local.entity.IntrusionAlertEntity
import com.example.data.local.entity.ScanResultEntity
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeSecurityRepository(
    private val securityHistoryDao: SecurityHistoryDao? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val _devices = MutableStateFlow<List<DeviceItem>>(getInitialDevices())
    val devices: StateFlow<List<DeviceItem>> = _devices.asStateFlow()

    private val _threats = MutableStateFlow<List<NetworkThreat>>(getInitialThreats())
    val threats: StateFlow<List<NetworkThreat>> = _threats.asStateFlow()

    private val _hardeningGuides = MutableStateFlow<List<HardeningGuide>>(getInitialHardeningGuides())
    val hardeningGuides: StateFlow<List<HardeningGuide>> = _hardeningGuides.asStateFlow()

    private val _telemetry = MutableStateFlow(NetworkTelemetry())
    val telemetry: StateFlow<NetworkTelemetry> = _telemetry.asStateFlow()

    // Wi-Fi Connection State
    private val _wifiState = MutableStateFlow(
        WifiConnectionState(
            isConnected = false,
            currentSsid = null,
            currentBssid = null,
            localIp = "",
            isConnecting = false
        )
    )
    val wifiState: StateFlow<WifiConnectionState> = _wifiState.asStateFlow()

    private val _availableNetworks = MutableStateFlow<List<AvailableWifiNetwork>>(getInitialWifiNetworks())
    val availableNetworks: StateFlow<List<AvailableWifiNetwork>> = _availableNetworks.asStateFlow()

    // Room Database Flows
    val scanHistory: Flow<List<ScanResultEntity>> = securityHistoryDao?.getAllScanResults() ?: flowOf(emptyList())
    val intrusionAlertsHistory: Flow<List<IntrusionAlertEntity>> = securityHistoryDao?.getAllAlerts() ?: flowOf(emptyList())

    init {
        // Seed initial history into Room if empty
        scope.launch {
            securityHistoryDao?.let { dao ->
                // Check if any existing alerts
                val existing = dao.getAllAlerts().firstOrNull() ?: emptyList()
                if (existing.isEmpty()) {
                    dao.insertAlert(
                        IntrusionAlertEntity(
                            threatId = "threat-seed-1",
                            timestamp = System.currentTimeMillis() - 86400000L,
                            title = "Alexa Voice Injection to Door Lock Intercepted",
                            threatType = ThreatType.UNAUTHORIZED_VOICE_OVERRIDE.name,
                            targetDeviceName = "Front Door Smart Deadbolt",
                            attackerIp = "192.168.1.102 (Alexa Pod Hub)",
                            packetCount = 18,
                            severity = "CRITICAL",
                            status = "ACTIVE_BLOCKED",
                            technicalDetails = "Unauthenticated audio override packet filtered by IoT Shield rule. 4-digit voice PIN bypass attempted."
                        )
                    )
                    dao.insertAlert(
                        IntrusionAlertEntity(
                            threatId = "threat-seed-2",
                            timestamp = System.currentTimeMillis() - 172800000L,
                            title = "Mirai Botnet Telnet Probe on Smart Wall Fan",
                            threatType = ThreatType.BRUTE_FORCE.name,
                            targetDeviceName = "Smart Wall Fan (Master Bed)",
                            attackerIp = "185.220.101.42",
                            packetCount = 142,
                            severity = "HIGH",
                            status = "ACTIVE_BLOCKED",
                            technicalDetails = "Dictionary attack testing default credentials 'admin/admin'. Source IP dropped and blacklisted."
                        )
                    )
                    dao.insertScanResult(
                        ScanResultEntity(
                            timestamp = System.currentTimeMillis() - 86400000L,
                            totalDevicesScanned = 7,
                            criticalCount = 3,
                            highCount = 2,
                            mediumCount = 1,
                            lowCount = 1,
                            homeDefenseScore = 48,
                            summary = "Initial audit identified unpatched voice bridge and default credentials on wall fan.",
                            flaggedDeviceNames = "Alexa Echo Pod Hub, Smart Wall Fan, Front Door Smart Deadbolt"
                        )
                    )
                }
            }
        }
    }

    fun connectToWifi(network: AvailableWifiNetwork) {
        _wifiState.value = _wifiState.value.copy(isConnecting = true, connectionError = null)
        scope.launch {
            kotlinx.coroutines.delay(1200) // Realistic connection handshake simulation
            _wifiState.value = WifiConnectionState(
                isConnected = true,
                currentSsid = network.ssid,
                currentBssid = network.bssid,
                localIp = "192.168.1.101",
                gatewayIp = "192.168.1.1",
                subnetMask = "255.255.255.0",
                signalDbm = -48,
                isConnecting = false,
                connectionError = null
            )
        }
    }

    fun disconnectWifi() {
        _wifiState.value = WifiConnectionState(
            isConnected = false,
            currentSsid = null,
            currentBssid = null,
            localIp = "",
            isConnecting = false
        )
    }

    suspend fun recordScanResult(
        totalDevices: Int,
        critical: Int,
        high: Int,
        medium: Int,
        low: Int,
        defenseScore: Int,
        summary: String,
        flaggedDevices: String
    ) {
        securityHistoryDao?.insertScanResult(
            ScanResultEntity(
                timestamp = System.currentTimeMillis(),
                totalDevicesScanned = totalDevices,
                criticalCount = critical,
                highCount = high,
                mediumCount = medium,
                lowCount = low,
                homeDefenseScore = defenseScore,
                summary = summary,
                flaggedDeviceNames = flaggedDevices
            )
        )
    }

    suspend fun clearScanHistory() {
        securityHistoryDao?.clearScanHistory()
    }

    suspend fun clearAlertHistory() {
        securityHistoryDao?.clearAlertHistory()
    }

    suspend fun deleteScanResult(id: Long) {
        securityHistoryDao?.deleteScanResultById(id)
    }

    suspend fun deleteAlert(id: Long) {
        securityHistoryDao?.deleteAlertById(id)
    }

    // Overall Home Defense Score (0 - 100, where 100 is impenetrable)
    fun calculateOverallDefenseScore(devicesList: List<DeviceItem>): Int {
        if (devicesList.isEmpty()) return 100
        val totalHealth = devicesList.sumOf { it.securityHealthPercent }
        return (totalHealth / devicesList.size).coerceIn(0, 100)
    }

    fun toggleDeviceIsolation(deviceId: String) {
        _devices.update { current ->
            current.map { device ->
                if (device.id == deviceId) {
                    val newIsolated = !device.isIsolated
                    val newScore = if (newIsolated) (device.riskScore - 25).coerceAtLeast(5) else (device.riskScore + 25).coerceAtMost(95)
                    device.copy(
                        isIsolated = newIsolated,
                        riskScore = newScore
                    )
                } else device
            }
        }
    }

    fun toggleVoiceBridgeRestriction(deviceId: String) {
        _devices.update { current ->
            current.map { device ->
                if (device.id == deviceId) {
                    val newRestricted = !device.voiceBridgeRestricted
                    val newScore = if (newRestricted) (device.riskScore - 20).coerceAtLeast(5) else (device.riskScore + 20).coerceAtMost(95)
                    device.copy(
                        voiceBridgeRestricted = newRestricted,
                        riskScore = newScore
                    )
                } else device
            }
        }
    }

    fun hardenDevice(deviceId: String) {
        _devices.update { current ->
            current.map { device ->
                if (device.id == deviceId) {
                    val updatedVulns = device.vulnerabilities.map { it.copy(isResolved = true) }
                    device.copy(
                        isHardened = true,
                        hasDefaultPassword = false,
                        isFirmwareOutdated = false,
                        firmwareVersion = device.latestAvailableFirmware,
                        hasOpenVulnerablePorts = false,
                        riskScore = 8,
                        vulnerabilities = updatedVulns
                    )
                } else device
            }
        }
    }

    fun hardenAllDevices() {
        _devices.update { current ->
            current.map { device ->
                val updatedVulns = device.vulnerabilities.map { it.copy(isResolved = true) }
                device.copy(
                    isHardened = true,
                    isIsolated = true,
                    voiceBridgeRestricted = true,
                    hasDefaultPassword = false,
                    isFirmwareOutdated = false,
                    firmwareVersion = device.latestAvailableFirmware,
                    hasOpenVulnerablePorts = false,
                    riskScore = 5,
                    vulnerabilities = updatedVulns
                )
            }
        }
    }

    fun togglePanicLockdown() {
        val currentLockdown = _telemetry.value.panicLockdownActive
        val newLockdown = !currentLockdown
        _telemetry.update { it.copy(panicLockdownActive = newLockdown) }

        if (newLockdown) {
            // Cut all vulnerable bridge permissions to access locks and fans
            _devices.update { list ->
                list.map { device ->
                    if (device.category == DeviceCategory.ACCESS_LOCK ||
                        device.category == DeviceCategory.VOICE_ASSISTANT ||
                        device.category == DeviceCategory.APPLIANCE_FAN
                    ) {
                        device.copy(isIsolated = true, voiceBridgeRestricted = true)
                    } else device
                }
            }
            // Add threat log entry for lockdown
            addThreat(
                NetworkThreat(
                    id = "threat-lockdown-${System.currentTimeMillis()}",
                    title = "Emergency House Lockdown Activated",
                    threatType = ThreatType.UNAUTHORIZED_VOICE_OVERRIDE,
                    targetDeviceId = "all-critical",
                    targetDeviceName = "All Access Locks & Smart Fans",
                    attackerIp = "Internal Network Isolation",
                    packetCount = 0,
                    severity = RiskLevel.SECURE,
                    status = ThreatStatus.ACTIVE_BLOCKED,
                    technicalDetails = "Direct API bridge from Alexa Pod to smart locks and fans severed. Air-gap isolation enforced."
                )
            )
        }
    }

    fun toggleIdsService(active: Boolean) {
        _telemetry.update { it.copy(idsActive = active) }
    }

    fun toggleHoneypot(active: Boolean) {
        _telemetry.update { it.copy(honeypotActive = active) }
    }

    fun addThreat(threat: NetworkThreat) {
        _threats.update { current ->
            listOf(threat) + current
        }
        _telemetry.update {
            it.copy(blockedThreatsToday = it.blockedThreatsToday + 1)
        }
        scope.launch {
            securityHistoryDao?.insertAlert(
                IntrusionAlertEntity(
                    threatId = threat.id,
                    timestamp = threat.timestamp,
                    title = threat.title,
                    threatType = threat.threatType.name,
                    targetDeviceName = threat.targetDeviceName,
                    attackerIp = threat.attackerIp,
                    packetCount = threat.packetCount,
                    severity = threat.severity.name,
                    status = threat.status.name,
                    technicalDetails = threat.technicalDetails
                )
            )
        }
    }

    fun toggleHardeningStep(guideId: String, stepNumber: Int) {
        _hardeningGuides.update { list ->
            list.map { guide ->
                if (guide.id == guideId) {
                    val updatedSteps = guide.steps.map { step ->
                        if (step.stepNumber == stepNumber) {
                            step.copy(isCompleted = !step.isCompleted)
                        } else step
                    }
                    guide.copy(steps = updatedSteps)
                } else guide
            }
        }
    }

    companion object {
        fun getInitialDevices(): List<DeviceItem> {
            return listOf(
                DeviceItem(
                    id = "dev-alexa-pod",
                    name = "Alexa Echo Pod Hub",
                    category = DeviceCategory.VOICE_ASSISTANT,
                    ipAddress = "192.168.1.102",
                    macAddress = "FC:65:DE:28:44:91",
                    vendor = "Amazon Lab126",
                    firmwareVersion = "v6.5.4.1 (UPnP Unpatched)",
                    latestAvailableFirmware = "v7.1.0.3 (Secure)",
                    hasDefaultPassword = false,
                    isFirmwareOutdated = true,
                    hasOpenVulnerablePorts = true,
                    openPorts = listOf(8080, 55443, 49153),
                    riskScore = 86,
                    isIsolated = false,
                    isHardened = false,
                    voiceBridgeRestricted = false,
                    vulnerabilities = listOf(
                        VulnerabilityFinding(
                            id = "v-alexa-1",
                            cveId = "CVE-2023-38257",
                            title = "Unrestricted Voice Command Bridge to Entry Locks",
                            description = "Alexa voice commands can actuate the front door lock and garage without requiring a spoken secondary security PIN.",
                            severity = RiskLevel.CRITICAL,
                            cvssScore = 9.1f,
                            impact = "Intruders can shout commands through closed windows or use laser/audio injection to unlock physical doors.",
                            remediation = "Enforce 4-digit Voice Confirmation PIN or decouple Smart Lock actuation from voice assistant."
                        ),
                        VulnerabilityFinding(
                            id = "v-alexa-2",
                            cveId = "CWE-200",
                            title = "Open UPnP Port Auto-Forwarding (Port 8080)",
                            description = "Device advertises UPnP SSDP discovery and opens dynamic gateway ports to the external internet without authentication.",
                            severity = RiskLevel.HIGH,
                            cvssScore = 7.8f,
                            impact = "Allows external threat actors to discover and probe your internal smart hub directly from the WAN.",
                            remediation = "Disable Universal Plug and Play (UPnP) on your router and Alexa network settings."
                        ),
                        VulnerabilityFinding(
                            id = "v-alexa-3",
                            cveId = "CWE-306",
                            title = "Unencrypted Local LAN Skill Broadcasts",
                            description = "Local discovery packets and skill invocation metadata are transmitted over unencrypted UDP multicast (239.255.255.250).",
                            severity = RiskLevel.MEDIUM,
                            cvssScore = 5.9f,
                            impact = "Compromised appliances on the same Wi-Fi can eavesdrop on speech triggers and smart home states.",
                            remediation = "Isolate Alexa and IoT devices onto a segmented Guest IoT VLAN."
                        )
                    )
                ),
                DeviceItem(
                    id = "dev-smart-fan",
                    name = "Smart Wall Fan (Master Bed)",
                    category = DeviceCategory.APPLIANCE_FAN,
                    ipAddress = "192.168.1.145",
                    macAddress = "24:62:AB:F9:11:80",
                    vendor = "ESP8266 IoT Controller",
                    firmwareVersion = "v1.0.4 (Legacy)",
                    latestAvailableFirmware = "v2.2.0 (TLS Encrypted)",
                    hasDefaultPassword = true,
                    isFirmwareOutdated = true,
                    hasOpenVulnerablePorts = true,
                    openPorts = listOf(80, 23),
                    riskScore = 82,
                    isIsolated = false,
                    isHardened = false,
                    voiceBridgeRestricted = false,
                    vulnerabilities = listOf(
                        VulnerabilityFinding(
                            id = "v-fan-1",
                            cveId = "CWE-259",
                            title = "Default Factory Admin Credentials (admin:admin)",
                            description = "The fan's embedded web configuration interface uses standard hardcoded factory credentials.",
                            severity = RiskLevel.CRITICAL,
                            cvssScore = 9.4f,
                            impact = "Any device on the Wi-Fi or compromised hub can log into the fan and modify firmware or pivot deeper into the network.",
                            remediation = "Change the web management password to a random 16+ character passphrase."
                        ),
                        VulnerabilityFinding(
                            id = "v-fan-2",
                            cveId = "CVE-2023-2895",
                            title = "Unauthenticated HTTP Buffer Overflow in Web Server",
                            description = "Sending malformed HTTP GET request headers to port 80 triggers a stack overflow allowing remote code execution (RCE).",
                            severity = RiskLevel.HIGH,
                            cvssScore = 8.6f,
                            impact = "Hackers can convert this wall fan into an active botnet node or persistent proxy inside your home.",
                            remediation = "Update firmware to v2.2.0 and block incoming port 80/23 traffic."
                        ),
                        VulnerabilityFinding(
                            id = "v-fan-3",
                            cveId = "CWE-319",
                            title = "Cleartext HTTP Control Protocol",
                            description = "Speed, oscillation, and power toggle commands are passed over plaintext HTTP without HMAC signature verification.",
                            severity = RiskLevel.MEDIUM,
                            cvssScore = 6.2f,
                            impact = "Intruders can forge fan commands or use it as a trigger sensor for presence detection.",
                            remediation = "Enforce HTTPS/MQTT-TLS encryption for local smart home controllers."
                        )
                    )
                ),
                DeviceItem(
                    id = "dev-smart-lock",
                    name = "Front Door Smart Deadbolt",
                    category = DeviceCategory.ACCESS_LOCK,
                    ipAddress = "192.168.1.189",
                    macAddress = "B8:27:EB:42:01:FE",
                    vendor = "Yale / Zigbee Bridge",
                    firmwareVersion = "v3.1.2",
                    latestAvailableFirmware = "v3.1.4",
                    hasDefaultPassword = false,
                    isFirmwareOutdated = false,
                    hasOpenVulnerablePorts = false,
                    openPorts = listOf(8443),
                    riskScore = 68,
                    isIsolated = false,
                    isHardened = false,
                    voiceBridgeRestricted = false,
                    vulnerabilities = listOf(
                        VulnerabilityFinding(
                            id = "v-lock-1",
                            cveId = "CWE-287",
                            title = "Voice Assistant Direct Unlocking Allowed",
                            description = "Connected smart hub (Alexa) has master override permissions to withdraw the deadbolt without multi-factor authorization.",
                            severity = RiskLevel.CRITICAL,
                            cvssScore = 9.0f,
                            impact = "Burglars exploiting voice assistant vulnerabilities or audio spoofing can unlock the front door.",
                            remediation = "Sever Alexa voice assistant unlocking permission in the IoT Shield app or enforce biometric confirmation."
                        ),
                        VulnerabilityFinding(
                            id = "v-lock-2",
                            cveId = "CWE-294",
                            title = "Bluetooth Low Energy Replay Susceptibility",
                            description = "Close-proximity unlock packets do not employ sliding-window nonces on older protocol revisions.",
                            severity = RiskLevel.HIGH,
                            cvssScore = 7.1f,
                            impact = "An attacker stationed outside within 30 feet can record an unlock RF burst and replay it later.",
                            remediation = "Update to firmware v3.1.4 and disable Auto-Unlock on proximity."
                        )
                    )
                ),
                DeviceItem(
                    id = "dev-security-camera",
                    name = "Front Porch Security Camera",
                    category = DeviceCategory.SURVEILLANCE_CAMERA,
                    ipAddress = "192.168.1.115",
                    macAddress = "48:02:2A:77:99:32",
                    vendor = "Hikvision / OEM Vision",
                    firmwareVersion = "v5.2.0 (Vulnerable RTSP)",
                    latestAvailableFirmware = "v5.5.8",
                    hasDefaultPassword = true,
                    isFirmwareOutdated = true,
                    hasOpenVulnerablePorts = true,
                    openPorts = listOf(554, 8000),
                    riskScore = 74,
                    isIsolated = false,
                    isHardened = false,
                    voiceBridgeRestricted = false,
                    vulnerabilities = listOf(
                        VulnerabilityFinding(
                            id = "v-cam-1",
                            cveId = "CVE-2021-36260",
                            title = "Unauthenticated RTSP Video Stream Port 554",
                            description = "RTSP live stream can be accessed on the local network without strong digest authentication.",
                            severity = RiskLevel.CRITICAL,
                            cvssScore = 9.8f,
                            impact = "Anyone connected to the Wi-Fi (or via compromised Alexa) can tap into your live household video feed.",
                            remediation = "Enable strong digest authentication and isolate camera to private surveillance VLAN."
                        ),
                        VulnerabilityFinding(
                            id = "v-cam-2",
                            cveId = "CWE-259",
                            title = "Default RTSP Stream Passphrase (123456)",
                            description = "Device ships with weak factory password pre-configured for streaming port 8000.",
                            severity = RiskLevel.HIGH,
                            cvssScore = 7.5f,
                            impact = "Vulnerable to automated internet-wide scanner bots.",
                            remediation = "Change default camera password and disable P2P cloud relay."
                        )
                    )
                ),
                DeviceItem(
                    id = "dev-smart-lights",
                    name = "Living Room Smart Bulb Mesh",
                    category = DeviceCategory.LIGHTING,
                    ipAddress = "192.168.1.108",
                    macAddress = "00:17:88:6A:B2:CC",
                    vendor = "Philips Hue / Zigbee Bridge",
                    firmwareVersion = "v1953188020",
                    latestAvailableFirmware = "v1953188020",
                    hasDefaultPassword = false,
                    isFirmwareOutdated = false,
                    hasOpenVulnerablePorts = false,
                    openPorts = listOf(80, 443),
                    riskScore = 28,
                    isIsolated = false,
                    isHardened = false,
                    voiceBridgeRestricted = false,
                    vulnerabilities = listOf(
                        VulnerabilityFinding(
                            id = "v-light-1",
                            cveId = "CWE-326",
                            title = "Legacy Zigbee Touchlink Commissioning Active",
                            description = "Bridge accepts physical touchlink pairing requests from nearby transceivers within 20cm without button press.",
                            severity = RiskLevel.LOW,
                            cvssScore = 3.8f,
                            impact = "Attacker with high-gain antenna within close range could theoretically factory-reset bulbs.",
                            remediation = "Disable open Zigbee join window in bridge settings."
                        )
                    )
                ),
                DeviceItem(
                    id = "dev-smart-tv",
                    name = "Living Room 65\" Smart TV",
                    category = DeviceCategory.ENTERTAINMENT,
                    ipAddress = "192.168.1.130",
                    macAddress = "60:01:94:E1:5C:88",
                    vendor = "LG webOS / Android TV",
                    firmwareVersion = "v04.50.25",
                    latestAvailableFirmware = "v04.50.25",
                    hasDefaultPassword = false,
                    isFirmwareOutdated = false,
                    hasOpenVulnerablePorts = true,
                    openPorts = listOf(5555, 8001),
                    riskScore = 38,
                    isIsolated = false,
                    isHardened = false,
                    voiceBridgeRestricted = false,
                    vulnerabilities = listOf(
                        VulnerabilityFinding(
                            id = "v-tv-1",
                            cveId = "CWE-284",
                            title = "Android Debug Bridge (ADB) Port 5555 Open",
                            description = "Developer mode ADB daemon is listening on local TCP port 5555 without cryptographic key authorization prompt.",
                            severity = RiskLevel.MEDIUM,
                            cvssScore = 6.4f,
                            impact = "Compromised local devices can sideload unauthorized APKs or record screen activity.",
                            remediation = "Turn off Developer Mode in TV settings and close port 5555."
                        )
                    )
                ),
                DeviceItem(
                    id = "dev-thermostat",
                    name = "Ecobee Smart Thermostat",
                    category = DeviceCategory.CLIMATE,
                    ipAddress = "192.168.1.140",
                    macAddress = "44:61:32:89:12:34",
                    vendor = "Ecobee Inc.",
                    firmwareVersion = "v4.7.5.352 (Secure)",
                    latestAvailableFirmware = "v4.7.5.352",
                    hasDefaultPassword = false,
                    isFirmwareOutdated = false,
                    hasOpenVulnerablePorts = false,
                    openPorts = listOf(443),
                    riskScore = 14,
                    isIsolated = true,
                    isHardened = true,
                    voiceBridgeRestricted = false,
                    vulnerabilities = emptyList()
                )
            )
        }

        fun getInitialThreats(): List<NetworkThreat> {
            val now = System.currentTimeMillis()
            return listOf(
                NetworkThreat(
                    id = "threat-1",
                    timestamp = now - 180_000,
                    title = "Unauthorized Alexa Voice Bridge Command Intercepted",
                    threatType = ThreatType.UNAUTHORIZED_VOICE_OVERRIDE,
                    targetDeviceId = "dev-smart-lock",
                    targetDeviceName = "Front Door Smart Deadbolt",
                    attackerIp = "192.168.1.102 (Alexa Pod Hub)",
                    packetCount = 18,
                    severity = RiskLevel.CRITICAL,
                    status = ThreatStatus.ACTIVE_BLOCKED,
                    technicalDetails = "Unauthenticated 'UnlockFrontDoor' JSON payload triggered without required 4-digit voice pin. Blocked by Shield Virtual Firewall."
                ),
                NetworkThreat(
                    id = "threat-2",
                    timestamp = now - 920_000,
                    title = "Brute-Force Telnet / HTTP Attack on Smart Wall Fan",
                    threatType = ThreatType.BRUTE_FORCE,
                    targetDeviceId = "dev-smart-fan",
                    targetDeviceName = "Smart Wall Fan (Master Bed)",
                    attackerIp = "185.220.101.42 (Known Mirai Botnet Scanner)",
                    packetCount = 142,
                    severity = RiskLevel.HIGH,
                    status = ThreatStatus.ACTIVE_BLOCKED,
                    technicalDetails = "Repeated dictionary attempts testing default 'admin/admin', 'root/123456' on port 80 & 23. Source IP blacklisted."
                ),
                NetworkThreat(
                    id = "threat-3",
                    timestamp = now - 3_600_000,
                    title = "Unauthenticated RTSP Stream Hijack Probed",
                    threatType = ThreatType.RTSP_STREAM_HIJACK,
                    targetDeviceId = "dev-security-camera",
                    targetDeviceName = "Front Porch Security Camera",
                    attackerIp = "192.168.1.240 (Rogue MAC 00:0C:29:XX)",
                    packetCount = 57,
                    severity = RiskLevel.HIGH,
                    status = ThreatStatus.MITIGATED,
                    technicalDetails = "Unauthorized RTSP DESCRIBE request sent to port 554 without authentication challenge response. Blocked."
                ),
                NetworkThreat(
                    id = "threat-4",
                    timestamp = now - 14_400_000,
                    title = "Honeypot Decoy Probed (Reconnaissance Port Sweep)",
                    threatType = ThreatType.PORT_SWEEP,
                    targetDeviceId = "honeypot-decoy",
                    targetDeviceName = "Virtual Decoy Bulb (Honeypot)",
                    attackerIp = "192.168.1.177",
                    packetCount = 310,
                    severity = RiskLevel.MEDIUM,
                    status = ThreatStatus.ACTIVE_BLOCKED,
                    technicalDetails = "Host scanned TCP ports 21, 22, 23, 80, 8080, 55443. Attacker logged and isolated."
                )
            )
        }

        fun getInitialHardeningGuides(): List<HardeningGuide> {
            return listOf(
                HardeningGuide(
                    id = "guide-alexa",
                    targetCategory = DeviceCategory.VOICE_ASSISTANT,
                    title = "Alexa Pod & Smart Hub Defense",
                    summary = "Secure voice assistants from window whisper attacks, unauthorized voice ordering, and unencrypted appliance bridging.",
                    threatVectorExplosion = "Smart speakers act as the master controller for the entire house. Intruders who can trigger voice commands or exploit unpatched UPnP ports can pivot through the Alexa hub to actuate smart locks, disable security cams, or control appliances.",
                    difficulty = "Easy",
                    estimatedMinutes = 5,
                    recommendedPriority = RiskLevel.CRITICAL,
                    steps = listOf(
                        HardeningStep(
                            stepNumber = 1,
                            title = "Enforce 4-Digit Voice PIN for Physical Entry",
                            instruction = "Open the Alexa app > Settings > Smart Home > Smart Locks. Toggle 'Unlock by Voice' to require a custom 4-digit spoken security code.",
                            technicalTip = "Prevents outside intruders from shouting 'Alexa, unlock front door' through an open window or screen door.",
                            isCompleted = false
                        ),
                        HardeningStep(
                            stepNumber = 2,
                            title = "Disable UPnP Auto-Port Forwarding",
                            instruction = "Log into your home Wi-Fi router (usually 192.168.1.1) > Advanced Settings > NAT / UPnP. Toggle UPnP to 'Disabled'.",
                            technicalTip = "Blocks the Alexa hub from dynamically punching open ports through your router firewall to the public internet.",
                            isCompleted = false
                        ),
                        HardeningStep(
                            stepNumber = 3,
                            title = "Restrict Third-Party Skill Permissions",
                            instruction = "Go to Alexa Settings > Skills & Games > Manage Skills. Audit all installed skills and revoke smart home device control permissions from unverified developers.",
                            technicalTip = "Malicious or abandoned skills can abuse webhook callbacks to spoof appliance state changes.",
                            isCompleted = false
                        ),
                        HardeningStep(
                            stepNumber = 4,
                            title = "Turn Off 'Help Improve Alexa' Data Sharing",
                            instruction = "In Alexa Privacy Settings > Manage Your Alexa Data, turn off 'Use of voice recordings' and enable automatic deletion of voice logs every 30 days.",
                            technicalTip = "Reduces the attack surface of cloud-stored voice profiles and transcripts.",
                            isCompleted = false
                        )
                    )
                ),
                HardeningGuide(
                    id = "guide-fan",
                    targetCategory = DeviceCategory.APPLIANCE_FAN,
                    title = "Smart Wall Fans & Embedded Plugs",
                    summary = "Prevent cheap Wi-Fi appliances from becoming weaponized entry points or botnet zombies on your home network.",
                    threatVectorExplosion = "Smart fans and cheap smart plugs often run lightweight ESP8266/ESP32 microcontrollers with unmaintained open-source web servers. Hackers use automated worms (like Mirai) to breach default credentials and turn them into local proxies.",
                    difficulty = "Intermediate",
                    estimatedMinutes = 8,
                    recommendedPriority = RiskLevel.CRITICAL,
                    steps = listOf(
                        HardeningStep(
                            stepNumber = 1,
                            title = "Replace Default Factory Passwords Immediately",
                            instruction = "Access the fan's local IP address in a web browser. Go to Admin/Security and change the default 'admin/admin' credentials to a strong 16+ character password.",
                            technicalTip = "Default credentials are the #1 vulnerability exploited by automated IoT botnets.",
                            isCompleted = false
                        ),
                        HardeningStep(
                            stepNumber = 2,
                            title = "Quarantine Fan to an Isolated IoT Guest Network",
                            instruction = "Connect your smart fan exclusively to your router's 2.4GHz Guest Wi-Fi network and enable 'Client Isolation' or 'AP Isolation'.",
                            technicalTip = "Even if a hacker compromises the fan, Client Isolation prevents them from accessing your PC, phone, or smart locks.",
                            isCompleted = false
                        ),
                        HardeningStep(
                            stepNumber = 3,
                            title = "Block Outbound WAN Access (Firewall Egress Rule)",
                            instruction = "In your home router, create a firewall rule blocking the fan's MAC address from accessing external internet IPs, allowing only local LAN control.",
                            technicalTip = "Neutralizes command-and-control (C2) botnet callbacks and unauthorized Chinese/overseas cloud tunnels.",
                            isCompleted = false
                        ),
                        HardeningStep(
                            stepNumber = 4,
                            title = "Check & Apply Vendor Firmware Patch",
                            instruction = "Use the vendor companion app to flash the latest firmware that fixes CVE-2023 buffer overflows.",
                            technicalTip = "Patches stack overflow vectors on port 80 and telnet port 23.",
                            isCompleted = false
                        )
                    )
                ),
                HardeningGuide(
                    id = "guide-locks",
                    targetCategory = DeviceCategory.ACCESS_LOCK,
                    title = "Smart Locks & Perimeter Entry",
                    summary = "Air-gap physical door locks and entry mechanisms away from vulnerable voice hubs and replay attacks.",
                    threatVectorExplosion = "If an attacker compromises your voice assistant (Alexa) or sniffs RF replay packets, they can grant physical access into your residence.",
                    difficulty = "Easy",
                    estimatedMinutes = 4,
                    recommendedPriority = RiskLevel.CRITICAL,
                    steps = listOf(
                        HardeningStep(
                            stepNumber = 1,
                            title = "Sever Direct Alexa Unlocking Permissions",
                            instruction = "In the IoT Shield app or Alexa app, disconnect direct unlock voice bindings. Restrict Alexa to 'Lock Only' and lock status inquiries.",
                            technicalTip = "You can still check if the door is locked by voice, but nobody can command Alexa to unlock it without physical credentials.",
                            isCompleted = false
                        ),
                        HardeningStep(
                            stepNumber = 2,
                            title = "Enable Biometric Two-Factor Authorization (2FA)",
                            instruction = "Require fingerprint or Face Unlock confirmation on your mobile app before the deadbolt can be retracted remotely.",
                            technicalTip = "Stops unauthorized unlock signals dispatched by malware or stolen sessions.",
                            isCompleted = false
                        ),
                        HardeningStep(
                            stepNumber = 3,
                            title = "Disable Proximity Auto-Unlock via Bluetooth",
                            instruction = "Turn off 'Auto-Unlock when phone arrives near door' in the lock settings.",
                            technicalTip = "Eliminates Bluetooth Low Energy relay attacks where attackers boost your phone's RF signal from down the street.",
                            isCompleted = false
                        )
                    )
                ),
                HardeningGuide(
                    id = "guide-cameras",
                    targetCategory = DeviceCategory.SURVEILLANCE_CAMERA,
                    title = "Security Cameras & Video Doorbells",
                    summary = "Protect household video feeds and audio microphones from unauthenticated eavesdropping and leaks.",
                    threatVectorExplosion = "Exposed RTSP video streams allow voyeurs and burglars to monitor when residents leave home, check keypad inputs, and map internal room layouts.",
                    difficulty = "Intermediate",
                    estimatedMinutes = 6,
                    recommendedPriority = RiskLevel.HIGH,
                    steps = listOf(
                        HardeningStep(
                            stepNumber = 1,
                            title = "Disable Default RTSP Port 554 and Enforce TLS/SRTP",
                            instruction = "In camera network configuration, change RTSP port 554 to a non-standard port (e.g., 8554) and enforce Digest/TLS authentication.",
                            technicalTip = "Blocks automated crawlers like Shodan from detecting open camera streams.",
                            isCompleted = false
                        ),
                        HardeningStep(
                            stepNumber = 2,
                            title = "Disable P2P Cloud Relay & UPnP",
                            instruction = "Uncheck 'Enable P2P Cloud' and 'Enable UPnP' in camera settings.",
                            technicalTip = "Forces video streams to remain local to your NVR/private storage instead of routing through third-party overseas relay servers.",
                            isCompleted = false
                        ),
                        HardeningStep(
                            stepNumber = 3,
                            title = "Create a Strong 16-Character Camera Admin Password",
                            instruction = "Never retain the factory PIN (e.g. 123456 or admin). Set an alphanumeric password with special characters.",
                            technicalTip = "Protects against brute-force password spraying.",
                            isCompleted = false
                        )
                    )
                ),
                HardeningGuide(
                    id = "guide-lights",
                    targetCategory = DeviceCategory.LIGHTING,
                    title = "Smart Lighting & Mesh Hubs",
                    summary = "Prevent bridge takeover and physical mesh sniffing from compromising smart lighting systems.",
                    threatVectorExplosion = "Zigbee bulbs form an ad-hoc mesh. Attackers within radio range can exploit open commissioning windows to reset devices or flood the mesh network.",
                    difficulty = "Easy",
                    estimatedMinutes = 3,
                    recommendedPriority = RiskLevel.MEDIUM,
                    steps = listOf(
                        HardeningStep(
                            stepNumber = 1,
                            title = "Disable Open Zigbee Join Window",
                            instruction = "Ensure the smart bridge pairing mode is only active for 60 seconds while explicitly pairing a new bulb.",
                            technicalTip = "Closes rogue node injection windows.",
                            isCompleted = false
                        ),
                        HardeningStep(
                            stepNumber = 2,
                            title = "Update Bridge Firmware Regularly",
                            instruction = "Enable Automatic Updates on your Hue / smart light bridge to receive cryptographic patch fixes.",
                            technicalTip = "Mitigates CVE memory corruptions in Zigbee gateway microcode.",
                            isCompleted = false
                        )
                    )
                ),
                HardeningGuide(
                    id = "guide-router",
                    targetCategory = DeviceCategory.ROUTER,
                    title = "Home Wi-Fi & Gateway Defense (The Master Shield)",
                    summary = "The cornerstone of smart home cybersecurity: segment your network so appliances cannot talk to personal computers.",
                    threatVectorExplosion = "If your smart fan, Alexa, and laptop are on the same Wi-Fi subnet, a compromised appliance gives the attacker a straight route into your banking data and personal devices.",
                    difficulty = "Advanced",
                    estimatedMinutes = 12,
                    recommendedPriority = RiskLevel.CRITICAL,
                    steps = listOf(
                        HardeningStep(
                            stepNumber = 1,
                            title = "Create a Dedicated 2.4GHz 'IoT Guest' SSID",
                            instruction = "In your Wi-Fi router settings, create a separate Guest Wi-Fi network named 'SmartHome_IoT'. Connect all smart appliances and Alexa to this SSID.",
                            technicalTip = "Keeps your laptops, smartphones, and NAS drives completely isolated on your primary 5GHz network.",
                            isCompleted = false
                        ),
                        HardeningStep(
                            stepNumber = 2,
                            title = "Enable 'AP Isolation' / 'Client Isolation'",
                            instruction = "On the IoT Guest network, check the box labeled 'AP Isolation' or 'Block intra-BSS communication'.",
                            technicalTip = "Prevents a compromised smart wall fan from sending ARP pings or scanning Alexa and smart locks.",
                            isCompleted = false
                        ),
                        HardeningStep(
                            stepNumber = 3,
                            title = "Disable WPS (Wi-Fi Protected Setup) and Remote Management",
                            instruction = "In router security settings, disable PIN-based WPS and uncheck 'Enable WAN Remote Management'.",
                            technicalTip = "WPS PINs can be cracked in under 4 hours using Reaver brute-force attacks.",
                            isCompleted = false
                        ),
                        HardeningStep(
                            stepNumber = 4,
                            title = "Enforce WPA3-Personal or WPA2-AES Encryption",
                            instruction = "Ensure encryption mode is set to WPA3-Personal (or WPA2-AES). Never use TKIP or open networks.",
                            technicalTip = "Guarantees robust forward secrecy for all over-the-air packets.",
                            isCompleted = false
                        )
                    )
                )
            )
        }

        fun getInitialWifiNetworks(): List<AvailableWifiNetwork> {
            return listOf(
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
                    ssid = "SecureOffice_WiFi",
                    bssid = "AC:84:C6:12:44:E0",
                    signalStrengthPercent = 65,
                    isSecure = true,
                    securityProtocol = "WPA2-Enterprise",
                    is5Ghz = true,
                    isRecommendedForIoT = false
                ),
                AvailableWifiNetwork(
                    ssid = "Neighbor_Guest_Open",
                    bssid = "00:11:22:33:44:55",
                    signalStrengthPercent = 35,
                    isSecure = false,
                    securityProtocol = "Unsecured / Open",
                    is5Ghz = false,
                    isRecommendedForIoT = false
                )
            )
        }
    }
}
