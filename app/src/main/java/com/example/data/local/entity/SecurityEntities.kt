package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val totalDevicesScanned: Int,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val homeDefenseScore: Int,
    val summary: String,
    val flaggedDeviceNames: String
)

@Entity(tableName = "intrusion_alerts")
data class IntrusionAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val threatId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val threatType: String,
    val targetDeviceName: String,
    val attackerIp: String,
    val packetCount: Int,
    val severity: String,
    val status: String,
    val technicalDetails: String
)
