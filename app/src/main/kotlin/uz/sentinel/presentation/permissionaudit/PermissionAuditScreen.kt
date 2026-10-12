package uz.sentinel.presentation.permissionaudit

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
        topBar = {
            TopAppBar(
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
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            val filteredApps = when (selectedTab) {
                1 -> uiState.apps.filter { it.grantedDangerousPerms.any { p -> p.contains("AUDIO") } }
                2 -> uiState.apps.filter { it.grantedDangerousPerms.any { p -> p.contains("CAMERA") } }
                3 -> uiState.apps.filter { it.grantedDangerousPerms.any { p -> p.contains("LOCATION") } }
                4 -> uiState.apps.filter { it.grantedDangerousPerms.any { p -> p.contains("CONTACTS") } }
                else -> uiState.apps
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { appInfo ->
                        PermissionAppRow(appInfo) {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", appInfo.packageName, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionAppRow(
    app: AppPermissionInfo,
    onOpenSettings: () -> Unit
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
                    Text(text = app.appName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = app.packageName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Sozlamalar")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Berilgan ruxsatlar: ${app.grantedDangerousPerms.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
