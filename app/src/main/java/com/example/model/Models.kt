package com.example.model

enum class DeviceCategory(val displayName: String) {
    VOICE_ASSISTANT("Voice Assistant & Hub"),
    APPLIANCE_FAN("Smart Appliance / Fan"),
    LIGHTING("Smart Lighting"),
    ACCESS_LOCK("Smart Lock / Entry"),
    SURVEILLANCE_CAMERA("Security Camera"),
    ENTERTAINMENT("Smart TV / Audio"),
    CLIMATE("Thermostat / Sensor"),
    ROUTER("Home Gateway / Router")
}

enum class RiskLevel(val label: String, val scoreWeight: Int) {
    CRITICAL("Critical Risk", 40),
    HIGH("High Risk", 25),
    MEDIUM("Medium Risk", 15),
    LOW("Low Risk", 5),
    SECURE("Hardened & Secure", 0)
}

data class VulnerabilityFinding(
    val id: String,
    val cveId: String? = null,
    val title: String,
    val description: String,
    val severity: RiskLevel,
    val cvssScore: Float,
    val impact: String,
    val remediation: String,
    val isResolved: Boolean = false
)

data class DeviceItem(
    val id: String,
    val name: String,
    val category: DeviceCategory,
    val ipAddress: String,
    val macAddress: String,
    val vendor: String,
    val firmwareVersion: String,
    val latestAvailableFirmware: String,
    val hasDefaultPassword: Boolean,
    val isFirmwareOutdated: Boolean,
    val hasOpenVulnerablePorts: Boolean,
    val openPorts: List<Int>,
    val riskScore: Int, // 0 - 100 (100 = dangerous)
    val isIsolated: Boolean = false, // Isolated in Guest IoT VLAN
    val isHardened: Boolean = false,
    val voiceBridgeRestricted: Boolean = false, // Disconnected from Alexa/Voice actuation
    val lastAuditedTime: Long = System.currentTimeMillis(),
    val vulnerabilities: List<VulnerabilityFinding> = emptyList()
) {
    val currentRiskLevel: RiskLevel
        get() = when {
            isHardened && isIsolated -> RiskLevel.SECURE
            riskScore >= 75 -> RiskLevel.CRITICAL
            riskScore >= 50 -> RiskLevel.HIGH
            riskScore >= 25 -> RiskLevel.MEDIUM
            riskScore > 10 -> RiskLevel.LOW
            else -> RiskLevel.SECURE
        }

    val securityHealthPercent: Int
        get() = (100 - riskScore).coerceIn(0, 100)
}

enum class ThreatType(val label: String) {
    UNAUTHORIZED_VOICE_OVERRIDE("Voice Command Privilege Escalation"),
    BRUTE_FORCE("Credential Brute-Force Attack"),
    BUFFER_OVERFLOW("Firmware Remote Code Execution"),
    RTSP_STREAM_HIJACK("Unauthenticated Camera Stream Tap"),
    PORT_SWEEP("Intrusion Port Reconnaissance"),
    DATA_EXFILTRATION("Anomalous Outbound Data Relay"),
    ARP_SPOOFING("ARP Poisoning / MitM Attempt")
}

enum class ThreatStatus(val label: String) {
    ACTIVE_BLOCKED("Blocked by Shield"),
    DEVICE_QUARANTINED("Device Quarantined"),
    MITIGATED("Threat Mitigated"),
    FLAGGED("Under Active Observation")
}

data class NetworkThreat(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val threatType: ThreatType,
    val targetDeviceId: String,
    val targetDeviceName: String,
    val attackerIp: String,
    val packetCount: Int,
    val severity: RiskLevel,
    val status: ThreatStatus,
    val technicalDetails: String
)

data class HardeningStep(
    val stepNumber: Int,
    val title: String,
    val instruction: String,
    val technicalTip: String,
    val isCompleted: Boolean = false
)

data class HardeningGuide(
    val id: String,
    val targetCategory: DeviceCategory,
    val title: String,
    val summary: String,
    val threatVectorExplosion: String,
    val difficulty: String,
    val estimatedMinutes: Int,
    val recommendedPriority: RiskLevel,
    val steps: List<HardeningStep>
)

enum class ScanStage(val stageName: String, val description: String) {
    IDLE("Idle", "Ready to initiate smart home security scan"),
    ARP_DISCOVERY("Subnet Discovery", "Broadcasting ARP pings across 192.168.1.0/24 subnet..."),
    PORT_AUDITING("Port Vulnerability Scan", "Probing Telnet (23), HTTP (80), RTSP (554), ADB (5555), UPnP (8080)..."),
    VOICE_BRIDGE_CHECK("Alexa & Voice Bridge Audit", "Inspecting voice pod trust boundaries and smart lock API authorizations..."),
    CREDENTIAL_CHECK("Credential Security Test", "Testing devices against default dictionary credentials (admin/admin, root)..."),
    FIRMWARE_CVE_MATCH("Firmware & CVE Matching", "Comparing device OS headers with Known Exploited Vulnerabilities catalog..."),
    COMPLETED("Audit Completed", "Home IoT vulnerability score and risk map updated.")
}

data class NetworkTelemetry(
    val packetsPerSec: Int = 124,
    val megabytesScannedToday: Float = 142.8f,
    val activeConnections: Int = 18,
    val blockedThreatsToday: Int = 14,
    val idsActive: Boolean = true,
    val honeypotActive: Boolean = true,
    val panicLockdownActive: Boolean = false
)
