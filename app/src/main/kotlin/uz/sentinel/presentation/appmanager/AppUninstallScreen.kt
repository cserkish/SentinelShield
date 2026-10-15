package uz.sentinel.presentation.appmanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.sentinel.domain.model.AppInfoItem
import uz.sentinel.presentation.components.GlassCard
import uz.sentinel.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUninstallScreen(
    viewModel: AppUninstallViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val uninstallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.loadInstalledApps()
    }
    val tabs = listOf("O'rnatilgan ilovalar (${uiState.userApps.size})", "Zavod bloatware (${uiState.bloatwareApps.size})")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text("Ilovalarni O'chirish", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadInstalledApps() }, enabled = !uiState.isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yangilash", tint = CyberTeal)
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = CyberTeal,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = CyberTeal
                        )
                    }
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
                                color = if (selectedTab == index) CyberTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyberTeal)
                }
            } else {
                val currentList = if (selectedTab == 0) uiState.userApps else uiState.bloatwareApps

                if (currentList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (selectedTab == 0) "O'rnatilgan uchinchi tomon ilovasi yo'q" else "Keraksiz bloatware topilmadi",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(currentList, key = { it.packageName }) { appItem ->
                            AppUninstallRow(
                                item = appItem,
                                isBloatwareTab = selectedTab == 1,
                                onUninstallClick = {
                                    val intent = Intent(Intent.ACTION_DELETE).apply {
                                        data = Uri.parse("package:${appItem.packageName}")
                                        putExtra(Intent.EXTRA_RETURN_RESULT, true)
                                    }
                                    uninstallLauncher.launch(intent)
                                },
                                onCopyAdbClick = {
                                    val cmd = "adb shell pm uninstall -k --user 0 ${appItem.packageName}"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", cmd))
                                    Toast.makeText(context, "Nusxalandi: $cmd", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppUninstallRow(
    item: AppInfoItem,
    isBloatwareTab: Boolean,
    onUninstallClick: () -> Unit,
    onCopyAdbClick: () -> Unit
) {
    val sizeMb = String.format("%.1f", item.sizeBytes / (1024.0 * 1024.0))

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = ObsidianSurfaceElev,
        borderColor = if (isBloatwareTab) NeonAmber.copy(alpha = 0.3f) else GlassBorderWhite,
        borderWidth = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.appName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$sizeMb MB",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyberTeal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "v${item.versionName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isBloatwareTab) {
                Button(
                    onClick = onUninstallClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCrimson.copy(alpha = 0.15f),
                        contentColor = NeonCrimson
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("O'chirish", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onCopyAdbClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADB buyrug'i", fontSize = 11.sp)
                }
            }
        }
    }
}
