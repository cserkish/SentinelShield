package uz.sentinel.presentation.netguard

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.sentinel.data.local.entity.PhishingDomainEntity
import uz.sentinel.presentation.components.GlassCard
import uz.sentinel.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetGuardScreen(
    viewModel: NetGuardViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.toggleVpn(true)
        }
    }

    val activeColor = if (uiState.isVpnActive) NeonCyan else Color.Gray

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text("NetGuard Tarmoq Himoyasi", fontWeight = FontWeight.Bold) },
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
        ) {
            // VPN Status Card (Frosted Glass Card)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = ObsidianSurface,
                borderColor = activeColor.copy(alpha = 0.35f),
                borderWidth = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.isVpnActive) {
                        NetworkWavePulse(color = NeonCyan)
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = ObsidianSurfaceElev,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderWhite),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.VpnKeyOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (uiState.isVpnActive) "Lokal DNS Himoyasi Faol" else "Tarmoq Himoyasi O'chiq",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (uiState.isVpnActive) NeonCyan else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fishing va soxta bank saytlariga kirish bloklanadi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Switch(
                        checked = uiState.isVpnActive,
                        onCheckedChange = { enable ->
                            if (enable) {
                                val prepareIntent = VpnService.prepare(context)
                                if (prepareIntent != null) {
                                    vpnLauncher.launch(prepareIntent)
                                } else {
                                    viewModel.toggleVpn(true)
                                }
                            } else {
                                viewModel.toggleVpn(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBlack,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = ObsidianSurfaceElev
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

                        Spacer(modifier = Modifier.height(14.dp))

            // Interactive Link / URL Scanner Card
            var urlInput by remember { mutableStateOf("") }
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = ObsidianSurfaceElev,
                borderColor = GlassBorderTeal,
                borderWidth = 1.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "🔍 Shubhali Havolani (Link) Tekshirish",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CyberTeal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            placeholder = { Text("Masalan: click-uz.info yoki bit.ly/...", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.checkUrlSafety(urlInput) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberTeal,
                                contentColor = ObsidianBlack
                            )
                        ) {
                            Text("Tekshirish", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    uiState.urlCheckVerdict?.let { verdict ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (verdict.isDangerous) NeonCrimson.copy(alpha = 0.15f) else NeonEmerald.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = verdict.message,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (verdict.isDangerous) NeonCrimson else NeonEmerald,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Moliyaviy va Bank Ilovalari Himoyasi Kartasi
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = ObsidianSurfaceElev,
                borderColor = GlassBorderTeal,
                borderWidth = 1.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = CyberTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Moliyaviy va Bank Ilovalari Himoyasi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CyberTeal
                        )
                        Text(
                            text = "Rasmiy fintex ekotizimi to'liq himoyalangan. Soxta fishing va mayner tarmoqlari bloklanadi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Bloklangan fishing domenlari",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.blockedDomains.isEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = ObsidianSurfaceElev,
                    borderColor = GlassBorderWhite,
                    borderWidth = 1.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonEmerald,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hozircha hech qanday shubhali so'rov aniqlanmadi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.blockedDomains) { domainEntity ->
                        ModernBlockedDomainRow(domainEntity)
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkWavePulse(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseWave")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )

    Box(
        modifier = Modifier.size(68.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 2.5f

            drawCircle(
                color = color.copy(alpha = (1.2f - pulseRadius).coerceIn(0f, 0.5f)),
                radius = baseRadius * pulseRadius,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.5f)),
            modifier = Modifier.size(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.VpnLock,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = color
                )
            }
        }
    }
}

@Composable
fun ModernBlockedDomainRow(entity: PhishingDomainEntity) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = ObsidianSurfaceElev,
        borderColor = NeonCrimson.copy(alpha = 0.3f),
        borderWidth = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.domain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = entity.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonCrimson
                )
            }
            Surface(
                shape = CircleShape,
                color = NeonCrimson.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = "Bloklangan",
                        tint = NeonCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
