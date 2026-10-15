package uz.sentinel.presentation.cleaner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import uz.sentinel.domain.model.JunkCategory
import uz.sentinel.domain.model.JunkItem
import uz.sentinel.presentation.components.GlassCard
import uz.sentinel.presentation.components.StorageCategoryData
import uz.sentinel.presentation.components.StorageDonutChart
import uz.sentinel.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScreen(
    viewModel: CleanerViewModel,
    onNavigateToStorageMigration: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val mbTotal = String.format("%.1f", uiState.totalSizeBytes / (1024.0 * 1024.0))

    val categoriesData = listOf(
        StorageCategoryData(
            name = "Ilovalar keshi",
            sizeBytes = uiState.items.filter { it.category == JunkCategory.APP_CACHE }.sumOf { it.sizeBytes },
            color = CyberTeal
        ),
        StorageCategoryData(
            name = "Vaqtinchalik fayllar",
            sizeBytes = uiState.items.filter { it.category == JunkCategory.TEMP_FILES }.sumOf { it.sizeBytes },
            color = NeonAmber
        ),
        StorageCategoryData(
            name = "Miniatyuralar",
            sizeBytes = uiState.items.filter { it.category == JunkCategory.THUMBNAILS }.sumOf { it.sizeBytes },
            color = NeonViolet
        ),
        StorageCategoryData(
            name = "APK qoldiqlari",
            sizeBytes = uiState.items.filter { it.category == JunkCategory.DOWNLOADED_APKS }.sumOf { it.sizeBytes },
            color = NeonCyan
        )
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text("Xotira Tozalash", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startScan() }, enabled = !uiState.isScanning) {
                        Icon(Icons.Default.Refresh, contentDescription = "Qayta tahlil", tint = CyberTeal)
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
            // Zamonaviy Donut Telemetriya Kartasi
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = ObsidianSurface,
                borderColor = GlassBorderTeal,
                borderWidth = 1.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StorageDonutChart(
                        categories = categoriesData,
                        totalSizeBytes = uiState.totalSizeBytes
                    )

                    if (uiState.cleanSuccessMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeonEmerald.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "✓ ${uiState.cleanSuccessMessage!!}",
                                color = NeonEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.cleanSelected() },
                        enabled = uiState.items.any { it.isSelected } && !uiState.isCleaning && !uiState.isScanning,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberTeal,
                            contentColor = ObsidianBlack,
                            disabledContainerColor = ObsidianSurfaceElev,
                            disabledContentColor = Color.Gray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (uiState.isCleaning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = ObsidianBlack,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Xavfsiz tozalanmoqda...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Xavfsiz tozalash ($mbTotal MB)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Katta fayllarni ko'chirish (Qo'shimcha xotira / Fleshka)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = ObsidianSurfaceElev,
                borderColor = GlassBorderTeal,
                borderWidth = 1.dp,
                onClick = onNavigateToStorageMigration
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NeonAmber.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SdStorage,
                                contentDescription = null,
                                tint = NeonAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Katta fayllarni ko'chirish (Fleshka / Qo'shimcha)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "50 MB dan katta video va arxivlarni xavfsiz ko'chiring",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Topilgan axlat fayllar (${uiState.items.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                if (uiState.items.isNotEmpty()) {
                    Text(
                        text = "Hammasi xavfsiz",
                        fontSize = 11.sp,
                        color = NeonEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyberTeal)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Xotira chuqur tahlil qilinmoqda...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (uiState.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonEmerald,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Qurilmangiz toza!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Keraksiz kesh yoki qoldiq fayllar topilmadi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.items, key = { it.path }) { item ->
                        ModernJunkRowItem(
                            item = item,
                            onToggle = { viewModel.toggleItemSelection(item.path) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernJunkRowItem(item: JunkItem, onToggle: () -> Unit) {
    val sizeKb = String.format("%.1f", item.sizeBytes / 1024.0)
    val categoryColor = when (item.category) {
        JunkCategory.APP_CACHE -> CyberTeal
        JunkCategory.TEMP_FILES -> NeonAmber
        JunkCategory.THUMBNAILS -> NeonViolet
        JunkCategory.DOWNLOADED_APKS -> NeonCyan
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = ObsidianSurfaceElev,
        borderColor = GlassBorderWhite,
        borderWidth = 1.dp,
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = CyberTeal,
                    checkmarkColor = ObsidianBlack
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = item.category.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = categoryColor
                )
            }
            Text(
                text = "$sizeKb KB",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
