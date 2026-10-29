package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.model.HardeningGuide
import com.example.model.HardeningStep
import com.example.ui.components.RiskBadge
import com.example.ui.components.getDeviceIcon
import com.example.ui.theme.*

@Composable
fun HardeningScreen(
    guides: List<HardeningGuide>,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onSelectGuide: (HardeningGuide) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredGuides = guides.filter { guide ->
        searchQuery.isEmpty() ||
                guide.title.contains(searchQuery, ignoreCase = true) ||
                guide.summary.contains(searchQuery, ignoreCase = true) ||
                guide.targetCategory.displayName.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Title Header
        item {
            Column {
                Text(
                    text = "DEVICE HARDENING REPOSITORY",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Security Hardening Guides",
                    color = TextPrimaryDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Actionable playbooks to secure Alexa, wall fans, smart deadbolts, cameras, and Wi-Fi gateways.",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }
        }

        // Search Field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("guide_search_input"),
                placeholder = { Text("Search by appliance type or vulnerability...", color = TextTertiaryDark, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = CyberCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondaryDark)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberNavyBorder,
                    focusedContainerColor = CyberNavyCard,
                    unfocusedContainerColor = CyberNavyCard,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                ),
                singleLine = true
            )
        }

        // Guide Cards List
        items(filteredGuides) { guide ->
            HardeningGuideCard(
                guide = guide,
                onClick = { onSelectGuide(guide) }
            )
        }
    }
}

@Composable
fun HardeningGuideCard(
    guide: HardeningGuide,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedStepsCount = guide.steps.count { it.isCompleted }
    val progress = if (guide.steps.isNotEmpty()) completedStepsCount.toFloat() / guide.steps.size else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("guide_card_${guide.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberNavyBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getDeviceIcon(guide.targetCategory),
                        contentDescription = guide.title,
                        tint = CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = guide.title,
                        color = TextPrimaryDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${guide.estimatedMinutes} min read • ${guide.difficulty} difficulty",
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                }

                RiskBadge(level = guide.recommendedPriority)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = guide.summary,
                color = TextSecondaryDark,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HARDENING PROGRESS",
                    color = TextSecondaryDark,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$completedStepsCount / ${guide.steps.size} STEPS COMPLETED",
                    color = if (completedStepsCount == guide.steps.size) NeonEmerald else CyberCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (completedStepsCount == guide.steps.size) NeonEmerald else CyberCyan,
                trackColor = Color(0xFF1E293B)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideDetailModal(
    guide: HardeningGuide,
    onDismiss: () -> Unit,
    onToggleStep: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CyberNavySurface,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getDeviceIcon(guide.targetCategory),
                            contentDescription = guide.title,
                            tint = CyberCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = guide.title,
                            color = TextPrimaryDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = guide.targetCategory.displayName,
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }

                    RiskBadge(level = guide.recommendedPriority)
                }
            }

            // Threat Vector Analysis Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1528)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberPurple))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "THREAT VECTOR ANALYSIS",
                                color = CyberPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = guide.threatVectorExplosion,
                            color = TextPrimaryDark,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Step Checklist Header
            item {
                Text(
                    text = "ACTIONABLE HARDENING CHECKLIST",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            // Checklist Steps
            items(guide.steps) { step ->
                StepChecklistItem(
                    step = step,
                    onToggle = { onToggleStep(step.stepNumber) }
                )
            }
        }
    }
}

@Composable
fun StepChecklistItem(
    step: HardeningStep,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (step.isCompleted) NeonEmerald.copy(alpha = 0.08f) else CyberNavyCard
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (step.isCompleted) NeonEmerald.copy(alpha = 0.5f) else CyberNavyBorder
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = step.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = NeonEmerald,
                    checkmarkColor = Color.Black,
                    uncheckedColor = TextSecondaryDark
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Step ${step.stepNumber}: ${step.title}",
                    color = if (step.isCompleted) NeonEmerald else TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = step.instruction,
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F172A))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "DEFENSE TIP: ",
                            color = CyberCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = step.technicalTip,
                            color = TextSecondaryDark,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
