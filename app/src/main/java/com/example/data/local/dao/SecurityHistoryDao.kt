package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.IntrusionAlertEntity
import com.example.data.local.entity.ScanResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityHistoryDao {

    // --- Scan History ---
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScanResults(): Flow<List<ScanResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanResult(scan: ScanResultEntity): Long

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteScanResultById(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun clearScanHistory()

    // --- Intrusion Alerts ---
    @Query("SELECT * FROM intrusion_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<IntrusionAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: IntrusionAlertEntity): Long

    @Query("DELETE FROM intrusion_alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Long)

    @Query("DELETE FROM intrusion_alerts")
    suspend fun clearAlertHistory()
}
