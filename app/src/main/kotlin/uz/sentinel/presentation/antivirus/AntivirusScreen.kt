package uz.sentinel.presentation.antivirus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.sentinel.domain.model.DangerLevel
import uz.sentinel.domain.model.ScanResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntivirusScreen(
    viewModel: AntivirusViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Antivirus Himoyasi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.scanAllApps() },
                        enabled = !uiState.isScanning
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Qayta skanerlash")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        uiState.highRiskCount > 0 -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (uiState.highRiskCount > 0) Icons.Default.Warning else Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = if (uiState.highRiskCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (uiState.highRiskCount > 0)
                            "${uiState.highRiskCount} ta shubhali ilova topildi"
                        else
                            "Qurilma xavfsiz holatda",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = if (uiState.isScanning)
                            "Skanerlanmoqda: ${uiState.currentScanningApp}"
                        else
                            "Barcha o'rnatilgan ilovalar tahlil qilingan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (uiState.isScanning) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { uiState.scanProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.scanAllApps() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Qurilmani to'liq skanerlash")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Tahlil natijalari (${uiState.results.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.results, key = { it.packageName }) { result ->
                    ScanResultItem(
                        result = result,
                        onTrustToggle = { viewModel.toggleTrustApp(result.packageName, result.isUserTrusted) },
                        onUninstall = { viewModel.uninstallApp(result.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
fun ScanResultItem(
    result: ScanResult,
    onTrustToggle: () -> Unit,
    onUninstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = result.appName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = result.packageName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                // Danger Score Badge
                val badgeColor = when (result.dangerLevel) {
                    DangerLevel.SAFE -> Color(0xFF2E7D32)
                    DangerLevel.MEDIUM -> Color(0xFFF57F17)
                    DangerLevel.HIGH -> Color(0xFFD32F2F)
                }

                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${result.dangerScore}/100",
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            if (result.detectedPermissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Xavfli ruxsatlar: ${result.detectedPermissions.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (result.dangerLevel == DangerLevel.HIGH && !result.isUserTrusted) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onTrustToggle) {
                        Text("Ishonaman")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onUninstall,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("O'chirish")
                    }
                }
            }
        }
    }
}
