package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AttackSimulationDialog(
    onDismiss: () -> Unit,
    onSimulate: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberNavySurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CriticalCrimson.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = CriticalCrimson, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ATTACK SIMULATION LAB",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Test Intrusion Defense",
                        color = TextPrimaryDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Launch a simulated intruder exploit to verify the Intrusion Detection System (IDS), real-time packet dropping, and urgent push notifications.",
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // Vector 1: Alexa Voice Injection
                SimScenarioCard(
                    vectorId = 1,
                    title = "Alexa Voice Injection (Door Unlock)",
                    subtitle = "Attacker exploits voice hub without PIN to actuate smart lock.",
                    badge = "CRITICAL",
                    badgeColor = CriticalCrimson,
                    onClick = { onSimulate(1) }
                )

                // Vector 2: Smart Wall Fan Exploit
                SimScenarioCard(
                    vectorId = 2,
                    title = "Smart Wall Fan Buffer Overflow",
                    subtitle = "External botnet sweeps port 80 and attempts memory corruption.",
                    badge = "HIGH",
                    badgeColor = Color(0xFFF97316),
                    onClick = { onSimulate(2) }
                )

                // Vector 3: Honeypot Decoy Sweep
                SimScenarioCard(
                    vectorId = 3,
                    title = "Rogue Intruder Network Recon",
                    subtitle = "Attacker probes open ports on the decoy smart bulb trap.",
                    badge = "MEDIUM",
                    badgeColor = WarningAmber,
                    onClick = { onSimulate(3) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondaryDark)
            }
        }
    )
}

@Composable
fun SimScenarioCard(
    vectorId: Int,
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("simulate_vector_$vectorId"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberNavyBorder))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextPrimaryDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = TextSecondaryDark,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
