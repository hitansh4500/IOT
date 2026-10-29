package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.HomeSecurityRepository
import com.example.data.local.HomeSecurityDatabase
import com.example.data.local.entity.IntrusionAlertEntity
import com.example.data.local.entity.ScanResultEntity
import com.example.model.AvailableWifiNetwork
import com.example.model.RiskLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: HomeSecurityDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HomeSecurityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("IoT Shield", appName)
    }

    @Test
    fun `verify smart home repository initial devices contain Alexa and Wall Fan`() {
        val repo = HomeSecurityRepository(db.securityHistoryDao())
        val devices = repo.devices.value
        val alexa = devices.find { it.id == "dev-alexa-pod" }
        val fan = devices.find { it.id == "dev-smart-fan" }

        assertTrue("Alexa pod should exist in inventory", alexa != null)
        assertEquals(RiskLevel.CRITICAL, alexa?.currentRiskLevel)

        assertTrue("Smart fan should exist in inventory", fan != null)
        assertTrue("Smart fan should have default password flagged", fan?.hasDefaultPassword == true)
        assertTrue("Smart fan should have outdated firmware flagged", fan?.isFirmwareOutdated == true)
    }

    @Test
    fun `verify device isolation reduces risk score`() {
        val repo = HomeSecurityRepository(db.securityHistoryDao())
        val initialScore = repo.devices.value.find { it.id == "dev-alexa-pod" }?.riskScore ?: 0
        repo.toggleDeviceIsolation("dev-alexa-pod")
        val postScore = repo.devices.value.find { it.id == "dev-alexa-pod" }?.riskScore ?: 0

        assertTrue("Isolating device should lower its risk score", postScore < initialScore)
    }

    @Test
    fun `verify Room database saves and retrieves scan results and intrusion alerts`() = runBlocking {
        val dao = db.securityHistoryDao()

        // Insert scan result
        val scan = ScanResultEntity(
            timestamp = System.currentTimeMillis(),
            totalDevicesScanned = 7,
            criticalCount = 2,
            highCount = 1,
            mediumCount = 1,
            lowCount = 0,
            homeDefenseScore = 68,
            summary = "Test Subnet Audit",
            flaggedDeviceNames = "Smart Fan, Alexa Pod"
        )
        val scanId = dao.insertScanResult(scan)
        assertTrue("Scan ID should be generated", scanId > 0)

        val retrievedScans = dao.getAllScanResults().first()
        assertEquals(1, retrievedScans.size)
        assertEquals(68, retrievedScans[0].homeDefenseScore)
        assertEquals("Smart Fan, Alexa Pod", retrievedScans[0].flaggedDeviceNames)

        // Insert intrusion alert
        val alert = IntrusionAlertEntity(
            threatId = "test-threat-1",
            title = "Test Voice Injection Attack",
            threatType = "UNAUTHORIZED_VOICE_OVERRIDE",
            targetDeviceName = "Front Door Deadbolt",
            attackerIp = "192.168.1.102",
            packetCount = 12,
            severity = "CRITICAL",
            status = "ACTIVE_BLOCKED",
            technicalDetails = "Simulated voice override packet"
        )
        val alertId = dao.insertAlert(alert)
        assertTrue("Alert ID should be generated", alertId > 0)

        val retrievedAlerts = dao.getAllAlerts().first()
        assertEquals(1, retrievedAlerts.size)
        assertEquals("Test Voice Injection Attack", retrievedAlerts[0].title)
    }

    @Test
    fun `verify Wi-Fi connection gating requirement`() {
        val repo = HomeSecurityRepository(db.securityHistoryDao())

        // Must start disconnected as requested by user
        assertEquals(false, repo.wifiState.value.isConnected)
        assertEquals(null, repo.wifiState.value.currentSsid)

        // Connect to a recommended Wi-Fi network
        val network = AvailableWifiNetwork(
            ssid = "SmartHome_IoT_2.4G",
            bssid = "00:11:22:33:44:55",
            signalStrengthPercent = 90,
            isSecure = true,
            securityProtocol = "WPA2-PSK",
            isRecommendedForIoT = true
        )
        repo.connectToWifi(network)
        assertTrue("isConnecting should be set to true while connecting", repo.wifiState.value.isConnecting)
    }
}
