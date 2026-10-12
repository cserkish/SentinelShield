package uz.sentinel.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.sentinel.core.utils.BrandDetector
import uz.sentinel.core.utils.OemSettingsLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StabilityGuideScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val brand = BrandDetector.detect()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Barqarorlik va Batareya", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aniqlangan brend: ${brand.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Xiaomi, Samsung, Huawei kabi telefonlar ilovalarni fonda avtomatik to'xtatadi. AppLock uzluksiz ishlashi uchun quyidagi 2 ta sozlamani yoqing:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step 1: AutoStart
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1-qadam: Avtomatik ishga tushirish (Autostart)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Telefon yoqilganda va qayta yuklanganda himoya avtomatik ishga tushishi uchun ruxsat bering.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { OemSettingsLauncher.openAutoStartSettings(context) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Autostart sozlamasini ochish")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Step 2: Battery Optimization Exemption
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("2-qadam: Batareya cheklovini olib tashlash", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Batareya tejash rejimida SentinelShield ilovasi uchun 'Cheklovsiz' (No restrictions) parametrini tanlang.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { OemSettingsLauncher.openBatteryOptimizationSettings(context) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Batareya sozlamasini ochish")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Eslatma: Tizim sozlamalari telefoningiz modeli va Android versiyasiga qarab biroz farq qilishi mumkin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
