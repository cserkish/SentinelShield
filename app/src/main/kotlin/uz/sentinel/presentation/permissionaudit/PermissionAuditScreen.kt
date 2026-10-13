package uz.sentinel.presentation.permissionaudit

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.sentinel.domain.model.AppPermissionInfo
import uz.sentinel.presentation.components.GlassCard
import uz.sentinel.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionAuditScreen(
    viewModel: PermissionAuditViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Hammasi", "Mikrofon", "Kamera", "Joylashuv", "Kontaktlar")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text("Ruxsatlar Auditi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Orqaga")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = CyberTeal,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyberTeal
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) CyberTeal else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            when (val state = uiState) {
                is PermissionAuditUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyberTeal)
                    }
                }
                is PermissionAuditUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = NeonCrimson)
                    }
                }
                is PermissionAuditUiState.Success -> {
                    val filteredApps = when (selectedTab) {
                        1 -> state.microphoneApps
                        2 -> state.cameraApps
                        3 -> state.locationApps
                        4 -> state.contactsApps
                        else -> state.all
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { appInfo ->
                            ModernPermissionAppRow(appInfo) {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", appInfo.packageName, null)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernPermissionAppRow(
    app: AppPermissionInfo,
    onOpenSettings: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = ObsidianSurfaceElev,
        borderColor = if (app.isSystemApp) GlassBorderWhite else NeonAmber.copy(alpha = 0.25f),
        borderWidth = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (app.isSystemApp) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeonEmerald.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "Tizim ilovasi",
                                    color = NeonEmerald,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Sozlamalar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ruxsatlar: ${app.grantedDangerousPerms.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = if (app.isSystemApp) MaterialTheme.colorScheme.onSurfaceVariant else NeonAmber
            )
        }
    }
}
