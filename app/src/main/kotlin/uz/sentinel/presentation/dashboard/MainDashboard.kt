package uz.sentinel.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: DashboardViewModel,
    onNavigateToAntivirus: () -> Unit,
    onNavigateToCleaner: () -> Unit,
    onNavigateToAppLock: () -> Unit,
    onNavigateToNetGuard: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToStability: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SentinelShield", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToStability) {
                        Icon(Icons.Default.BatterySaver, contentDescription = "Batareya sozlamasi")
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
                .verticalScroll(rememberScrollState())
        ) {
            // Main Big Shield Card
            val shieldColor = if (uiState.isDeviceSafe) Color(0xFF2E7D32) else Color(0xFFD32F2F)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAntivirus() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = shieldColor.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = shieldColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isDeviceSafe) Icons.Default.Shield else Icons.Default.GppBad,
                            contentDescription = null,
                            tint = shieldColor,
                            modifier = Modifier
                                .padding(18.dp)
                                .fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (uiState.isDeviceSafe) "Qurilmangiz Himoyalangan" else "Xavf Aniqlangan!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = shieldColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.isDeviceSafe)
                            "Zararli ilovalar va fishing tahdidlari topilmadi"
                        else
                            "${uiState.highRiskAppCount} ta ilovada xavfli ruxsatlar mavjud",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Asosiy Himoya Vositalari", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(12.dp))

            // 2x2 Grid Cards
            Row(modifier = Modifier.fillMaxWidth()) {
                ModuleCard(
                    modifier = Modifier.weight(1f),
                    title = "Antivirus",
                    subtitle = "Ilovalarni tahlil qilish",
                    icon = Icons.Default.BugReport,
                    iconColor = Color(0xFF1976D2),
                    onClick = onNavigateToAntivirus
                )
                Spacer(modifier = Modifier.width(12.dp))
                ModuleCard(
                    modifier = Modifier.weight(1f),
                    title = "Tozalash",
                    subtitle = "Kesh va qoldiqlar",
                    icon = Icons.Default.CleaningServices,
                    iconColor = Color(0xFF388E3C),
                    onClick = onNavigateToCleaner
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ModuleCard(
                    modifier = Modifier.weight(1f),
                    title = "AppLock",
                    subtitle = "${uiState.lockedAppCount} ta ilova qulflangan",
                    icon = Icons.Default.Lock,
                    iconColor = Color(0xFFF57C00),
                    onClick = onNavigateToAppLock
                )
                Spacer(modifier = Modifier.width(12.dp))
                ModuleCard(
                    modifier = Modifier.weight(1f),
                    title = "NetGuard",
                    subtitle = if (uiState.isNetGuardActive) "Faol" else "O'chiq",
                    icon = Icons.Default.VpnLock,
                    iconColor = Color(0xFF7B1FA2),
                    onClick = onNavigateToNetGuard
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Full-width Permission Auditor Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPermissions() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Maxfiylik va Ruxsatlar Auditi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Kamera, mikrofon va joylashuvdan foydalanayotgan ilovalar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun ModuleCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
        }
    }
}
