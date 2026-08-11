package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.IssueSeverity
import com.example.data.models.ReusedPasswordIssue
import com.example.data.models.VaultHealthReport
import com.example.data.models.WeakPasswordIssue
import com.example.ui.theme.BitwardenBlue
import kotlinx.coroutines.launch

private enum class HealthFilter {
    ALL,
    REUSED,
    WEAK,
    TOTP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHealthScreen(
    onGenerateReport: suspend () -> VaultHealthReport,
    onBack: () -> Unit
) {
    var report by remember { mutableStateOf<VaultHealthReport?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf(HealthFilter.ALL) }
    val scope = rememberCoroutineScope()

    fun refreshReport() {
        scope.launch {
            isLoading = true
            report = onGenerateReport()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshReport()
    }

    Scaffold(
        modifier = Modifier.testTag("vault_health_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Vault Security Audit", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("health_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshReport() },
                        modifier = Modifier.testTag("health_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Re-analyze Vault")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        val currentReport = report

        if (isLoading || currentReport == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = BitwardenBlue,
                    modifier = Modifier.testTag("health_loading_indicator")
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Performing offline security audit...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // 1. Hero Health Dashboard Card
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HealthScoreDashboardCard(report = currentReport)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 2. Filter Pills
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == HealthFilter.ALL,
                                onClick = { selectedFilter = HealthFilter.ALL },
                                label = { Text("All (${currentReport.totalIssuesCount})") },
                                modifier = Modifier.testTag("filter_all")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == HealthFilter.REUSED,
                                onClick = { selectedFilter = HealthFilter.REUSED },
                                label = { Text("Reused (${currentReport.reusedPasswords.size})") },
                                modifier = Modifier.testTag("filter_reused")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == HealthFilter.WEAK,
                                onClick = { selectedFilter = HealthFilter.WEAK },
                                label = { Text("Weak (${currentReport.weakPasswords.size})") },
                                modifier = Modifier.testTag("filter_weak")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == HealthFilter.TOTP,
                                onClick = { selectedFilter = HealthFilter.TOTP },
                                label = { Text("No 2FA (${currentReport.missingTotpCount})") },
                                modifier = Modifier.testTag("filter_totp")
                            )
                        }
                    }
                }

                // 3. Reused Passwords Section
                if (selectedFilter == HealthFilter.ALL || selectedFilter == HealthFilter.REUSED) {
                    item {
                        SectionHeader(
                            title = "Reused Passwords",
                            count = currentReport.reusedPasswords.size,
                            testTag = "reused_section_header"
                        )
                    }

                    if (currentReport.reusedPasswords.isEmpty()) {
                        item {
                            EmptyCategoryCard(
                                message = "No duplicate passwords detected! All password items are unique.",
                                icon = Icons.Default.CheckCircle
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        itemsIndexed(currentReport.reusedPasswords) { index, issue ->
                            ReusedPasswordCard(
                                issue = issue,
                                modifier = Modifier.testTag("reused_issue_card_$index")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // 4. Weak Passwords Section
                if (selectedFilter == HealthFilter.ALL || selectedFilter == HealthFilter.WEAK) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(
                            title = "Weak & Vulnerable Passwords",
                            count = currentReport.weakPasswords.size,
                            testTag = "weak_section_header"
                        )
                    }

                    if (currentReport.weakPasswords.isEmpty()) {
                        item {
                            EmptyCategoryCard(
                                message = "No weak passwords found. Your login passwords meet high entropy criteria.",
                                icon = Icons.Default.CheckCircle
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        items(currentReport.weakPasswords) { issue ->
                            WeakPasswordCard(
                                issue = issue,
                                modifier = Modifier.testTag("weak_issue_card_${issue.itemId}")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // 5. 2FA / TOTP Security Audit Section
                if (selectedFilter == HealthFilter.ALL || selectedFilter == HealthFilter.TOTP) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(
                            title = "Two-Factor Auth (2FA / TOTP) Audit",
                            count = currentReport.missingTotpCount,
                            testTag = "totp_section_header"
                        )
                    }

                    item {
                        TotpAuditCard(
                            totalLogins = currentReport.totalLogins,
                            missingTotp = currentReport.missingTotpCount
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun HealthScoreDashboardCard(report: VaultHealthReport) {
    val score = report.healthScore
    val scoreColor = when {
        score >= 85 -> Color(0xFF16A34A) // Green
        score >= 60 -> Color(0xFFD97706) // Amber/Orange
        else -> Color(0xFFDC2626) // Red
    }

    val statusTitle = when {
        score >= 85 -> "EXCELLENT SECURITY"
        score >= 60 -> "ACTION RECOMMENDED"
        else -> "CRITICAL RISKS DETECTED"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("health_score_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Score Circle Meter
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(72.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.size(72.dp),
                        color = scoreColor,
                        strokeWidth = 6.dp,
                        trackColor = scoreColor.copy(alpha = 0.2f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$score",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "/100",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = scoreColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = statusTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "100% Local Vault Analysis",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${report.totalLogins} login credentials scanned offline",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stat Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatMetricChip(
                    label = "Critical Risk",
                    value = "${report.criticalCount}",
                    color = Color(0xFFDC2626)
                )
                StatMetricChip(
                    label = "Reused Passwords",
                    value = "${report.reusedPasswords.size}",
                    color = Color(0xFFD97706)
                )
                StatMetricChip(
                    label = "Weak Passwords",
                    value = "${report.weakPasswords.size}",
                    color = Color(0xFFCA8A04)
                )
            }
        }
    }
}

@Composable
private fun StatMetricChip(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SeverityBadge(severity: IssueSeverity) {
    val (bgColor, textColor, icon, label) = when (severity) {
        IssueSeverity.CRITICAL -> Quadruple(
            Color(0xFFFEE2E2),
            Color(0xFFDC2626),
            Icons.Default.Dangerous,
            "CRITICAL SEVERITY"
        )
        IssueSeverity.HIGH -> Quadruple(
            Color(0xFFFEF3C7),
            Color(0xFFD97706),
            Icons.Default.Warning,
            "HIGH RISK"
        )
        IssueSeverity.MEDIUM -> Quadruple(
            Color(0xFFFEF9C3),
            Color(0xFFCA8A04),
            Icons.Default.Error,
            "MODERATE RISK"
        )
        IssueSeverity.LOW -> Quadruple(
            Color(0xFFDBEAFE),
            Color(0xFF2563EB),
            Icons.Default.Info,
            "LOW RISK"
        )
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        modifier = Modifier.testTag("severity_badge_${severity.name}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun SectionHeader(title: String, count: Int, testTag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ReusedPasswordCard(issue: ReusedPasswordIssue, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SeverityBadge(severity = issue.severity)
                Text(
                    text = "${issue.affectedItems.size} Accounts Affected",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Same password reused across multiple services. If one service is breached, all affected accounts become vulnerable.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Affected Accounts:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    issue.affectedItems.forEach { pair ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = BitwardenBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = pair.second,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeakPasswordCard(issue: WeakPasswordIssue, modifier: Modifier = Modifier) {
    val entropyVal = issue.entropy
    val progress = (entropyVal / 60.0).coerceIn(0.0, 1.0).toFloat()
    val progressColor = when {
        entropyVal < 28.0 -> Color(0xFFDC2626)
        entropyVal < 45.0 -> Color(0xFFD97706)
        else -> Color(0xFF16A34A)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SeverityBadge(severity = issue.severity)
                Text(
                    text = if (entropyVal == 0.0) "Empty Password" else "Entropy: ${entropyVal.toInt()} bits",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = issue.itemName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (issue.username.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = issue.username,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Strength bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Strength Level",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (entropyVal < 28) "Extremely Weak" else "Weak",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = progressColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun TotpAuditCard(totalLogins: Int, missingTotp: Int) {
    val configured2FA = (totalLogins - missingTotp).coerceAtLeast(0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("totp_audit_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = BitwardenBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "2FA Authenticator Coverage",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$configured2FA of $totalLogins login accounts have 2FA/TOTP authenticator keys configured in the vault.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (missingTotp > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tip: Add TOTP secrets to critical logins to generate 6-digit verification codes directly inside LS Pass.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyCategoryCard(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
