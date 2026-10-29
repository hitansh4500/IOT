package com.example.ids

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.model.NetworkThreat
import com.example.model.RiskLevel

class ThreatNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "smart_home_security_alerts"
        const val CHANNEL_NAME = "Smart Home Intrusion Alerts"
        const val CHANNEL_DESC = "Urgent alerts when unauthorized smart device access or hacking attempts are detected"
        private var notificationIdCounter = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun dispatchThreatAlert(threat: NetworkThreat) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_THREAT_ID", threat.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            threat.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val severityEmoji = when (threat.severity) {
            RiskLevel.CRITICAL -> "[CRITICAL ALERT]"
            RiskLevel.HIGH -> "[HIGH THREAT]"
            RiskLevel.MEDIUM -> "[WARNING]"
            else -> "[ALERT]"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("$severityEmoji ${threat.title}")
            .setContentText("Target: ${threat.targetDeviceName} | Attacker: ${threat.attackerIp}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${threat.title}\n\nTarget Device: ${threat.targetDeviceName}\nAttacker Origin: ${threat.attackerIp}\nAction: ${threat.status.label}\n\nTechnical Analysis: ${threat.technicalDetails}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            try {
                NotificationManagerCompat.from(context).notify(notificationIdCounter++, notification)
            } catch (_: SecurityException) {
                // Handled gracefully if permission revoked at runtime
            }
        }
    }
}
