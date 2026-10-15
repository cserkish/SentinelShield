package uz.sentinel.presentation.wifisecurity

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.sentinel.presentation.components.GlassCard
import uz.sentinel.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiSecurityScreen(
    viewModel: WifiSecurityViewModel,
    onNavigateToNetGuard: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val statusColor = when {
        uiState.securityScore < 75 -> NeonCrimson
        uiState.securityScore < 90 -> NeonAmber
        else -> CyberTeal
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text("Wi-Fi va Tarmoq Auditi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.scanNetworkSecurity() }, enabled = !uiState.isScanning) {
                        Icon(Icons.Default.Refresh, contentDescription = "Qayta tekshirish", tint = CyberTeal)
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
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = ObsidianSurface,
                borderColor = statusColor.copy(alpha = 0.4f),
                borderWidth = 1.5.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = uiState.connectionType,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Xavfsizlik darajasi: ${uiState.securityScore}/100" +
                                if (uiState.linkSpeedMbps > 0) " • Tezlik: ${uiState.linkSpeedMbps} Mbps" else "",
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.scanNetworkSecurity() },
                            enabled = !uiState.isScanning,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberTeal,
                                contentColor = ObsidianBlack
                            )
                        ) {
                            Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Qayta Tekshirish", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        if (!uiState.isVpnActive) {
                            OutlinedButton(
                                onClick = onNavigateToNetGuard,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.VpnLock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("NetGuard Yoqish", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tarmoq Xavfsizligi Tekshiruvlari (${uiState.checks.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyberTeal)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.checks) { item ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            backgroundColor = ObsidianSurfaceElev,
                            borderColor = if (item.isSafe) GlassBorderWhite else NeonCrimson.copy(alpha = 0.4f),
                            borderWidth = 1.dp
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (item.isSafe) MaterialTheme.colorScheme.onSurface else NeonCrimson
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.detail,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
