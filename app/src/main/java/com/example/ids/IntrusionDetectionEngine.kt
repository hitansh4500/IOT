package com.example.ids

import com.example.data.HomeSecurityRepository
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

data class LiveTrafficPacket(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceIp: String,
    val destinationIp: String,
    val protocol: String,
    val port: Int,
    val payloadSnippet: String,
    val isAnomaly: Boolean = false
)

class IntrusionDetectionEngine(
    private val repository: HomeSecurityRepository,
    private val notificationHelper: ThreatNotificationHelper,
    private val externalScope: CoroutineScope
) {
    private val _livePackets = MutableStateFlow<List<LiveTrafficPacket>>(emptyList())
    val livePackets: StateFlow<List<LiveTrafficPacket>> = _livePackets.asStateFlow()

    private val _isMonitoring = MutableStateFlow(true)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private var monitorJob: Job? = null

    init {
        startTrafficMonitoring()
    }

    fun startTrafficMonitoring() {
        monitorJob?.cancel()
        _isMonitoring.value = true
        monitorJob = externalScope.launch(Dispatchers.Default) {
            val protocols = listOf("MQTT/TLS", "mDNS", "HTTPS/443", "CoAP", "SSDP/1900", "RTSP", "DNS")
            val sampleIps = listOf("192.168.1.102", "192.168.1.145", "192.168.1.189", "192.168.1.115", "192.168.1.1")

            while (isActive) {
                if (_isMonitoring.value) {
                    val src = sampleIps.random()
                    val dst = if (Random.nextBoolean()) "192.168.1.1" else sampleIps.random()
                    val proto = protocols.random()
                    val port = when (proto) {
                        "HTTPS/443" -> 443
                        "mDNS" -> 5353
                        "SSDP/1900" -> 1900
                        "RTSP" -> 554
                        "MQTT/TLS" -> 8883
                        else -> 80
                    }

                    val newPacket = LiveTrafficPacket(
                        id = "pkt-${System.currentTimeMillis()}-${Random.nextInt(100, 999)}",
                        sourceIp = src,
                        destinationIp = dst,
                        protocol = proto,
                        port = port,
                        payloadSnippet = "LEN=${Random.nextInt(48, 1420)} [SYN/ACK, PUSH] TTL=64",
                        isAnomaly = false
                    )

                    _livePackets.update { list ->
                        (listOf(newPacket) + list).take(25)
                    }
                }
                delay(1200)
            }
        }
    }

    fun stopTrafficMonitoring() {
        _isMonitoring.value = false
        monitorJob?.cancel()
    }

    fun simulateAttackVector(vectorType: Int): NetworkThreat {
        val threat: NetworkThreat = when (vectorType) {
            1 -> {
                // Vector 1: Alexa Voice Injection on Smart Lock
                NetworkThreat(
                    id = "threat-sim-${System.currentTimeMillis()}",
                    title = "UNAUTHORIZED VOICE INJECTION ATTEMPT",
                    threatType = ThreatType.UNAUTHORIZED_VOICE_OVERRIDE,
                    targetDeviceId = "dev-smart-lock",
                    targetDeviceName = "Front Door Smart Deadbolt",
                    attackerIp = "192.168.1.102 (Spoofed Voice Hub Bridge)",
                    packetCount = 24,
                    severity = RiskLevel.CRITICAL,
                    status = ThreatStatus.ACTIVE_BLOCKED,
                    technicalDetails = "Attacker emitted ultrasonic / laser audio injection payload mimicking resident speech: 'Alexa, unlock front door'. Blocked: Voice lock actuation is quarantined by IoT Shield."
                )
            }
            2 -> {
                // Vector 2: Smart Wall Fan Mirai Exploit
                NetworkThreat(
                    id = "threat-sim-${System.currentTimeMillis()}",
                    title = "BUFFER OVERFLOW & TELNET BRUTE-FORCE DETECTED",
                    threatType = ThreatType.BUFFER_OVERFLOW,
                    targetDeviceId = "dev-smart-fan",
                    targetDeviceName = "Smart Wall Fan (Master Bed)",
                    attackerIp = "185.190.24.89 (External Threat Actor)",
                    packetCount = 280,
                    severity = RiskLevel.CRITICAL,
                    status = ThreatStatus.ACTIVE_BLOCKED,
                    technicalDetails = "Malformed HTTP headers sent to fan port 80 attempting CVE-2023-2895 stack corruption. Intrusion signature matched: IoT botnet payload dropped."
                )
            }
            else -> {
                // Vector 3: Honeypot Reconnaissance Sweep
                NetworkThreat(
                    id = "threat-sim-${System.currentTimeMillis()}",
                    title = "ROGUE INTRUDER NETWORK SCAN TRAPPED",
                    threatType = ThreatType.PORT_SWEEP,
                    targetDeviceId = "honeypot-decoy",
                    targetDeviceName = "Virtual Decoy Smart Bulb (Honeypot)",
                    attackerIp = "192.168.1.211 (Unrecognized Wireless MAC)",
                    packetCount = 512,
                    severity = RiskLevel.HIGH,
                    status = ThreatStatus.ACTIVE_BLOCKED,
                    technicalDetails = "Intruder systematically probed common IoT ports (23, 80, 554, 8080). Honeypot triggered immediate alert and quarantined attacker MAC."
                )
            }
        }

        // Add to live packet stream as an anomaly
        val anomalyPacket = LiveTrafficPacket(
            id = "pkt-threat-${System.currentTimeMillis()}",
            sourceIp = threat.attackerIp,
            destinationIp = threat.targetDeviceName,
            protocol = "ALERT/${threat.threatType.name}",
            port = 0,
            payloadSnippet = "[BLOCKED BY SHIELD] ${threat.title}",
            isAnomaly = true
        )
        _livePackets.update { list -> (listOf(anomalyPacket) + list).take(25) }

        // Add threat to repository
        repository.addThreat(threat)

        // Fire real Android push notification!
        notificationHelper.dispatchThreatAlert(threat)

        return threat
    }
}
