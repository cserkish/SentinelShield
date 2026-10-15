package uz.sentinel.presentation.applock

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.sentinel.data.local.db.LockedAppDao
import uz.sentinel.data.local.entity.LockedAppEntity
import uz.sentinel.data.local.prefs.SentinelPreferences
import uz.sentinel.presentation.theme.*
import uz.sentinel.service.AppMonitorService

data class AppLockItem(
    val packageName: String,
    val appName: String,
    val isLocked: Boolean,
    val isAntiTamperTarget: Boolean = false
)

private val CRITICAL_PROTECTABLE_SYSTEM_PACKAGES = setOf(
    "com.android.settings",
    "com.google.android.packageinstaller",
    "com.android.packageinstaller",
    "com.miui.packageinstaller",
    "com.samsung.android.packageinstaller",
    "com.android.vending",
    "com.android.chrome",
    "com.sec.android.gallery3d",
    "com.google.android.apps.photos",
    "com.google.android.apps.messaging"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockListScreen(
    lockedAppDao: LockedAppDao,
    preferences: SentinelPreferences,
    onNavigateToIntruderPhotos: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var appList by remember { mutableStateOf<List<AppLockItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var hasOverlayPermission by remember {
        mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true)
    }
    var hasUsagePermission by remember {
        mutableStateOf(checkUsageStatsPermission(context))
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val savedPin by preferences.appLockPinHash.collectAsState(initial = null)
    val defaultLockMethod by preferences.appLockDefaultMethod.collectAsState(initial = "PIN")
    var showSetPinDialog by remember { mutableStateOf(false) }
    var pendingAppToLock by remember { mutableStateOf<AppLockItem?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
                hasUsagePermission = checkUsageStatsPermission(context)
                hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { app ->
                    app.packageName != context.packageName && (
                        (app.flags and ApplicationInfo.FLAG_SYSTEM == 0) ||
                        app.packageName in CRITICAL_PROTECTABLE_SYSTEM_PACKAGES
                    )
                }
            val lockedPackages = lockedAppDao.getLockedPackageNames().toSet()

            appList = installed.map { app ->
                val isAntiTamper = app.packageName in setOf(
                    "com.android.settings",
                    "com.google.android.packageinstaller",
                    "com.android.packageinstaller",
                    "com.miui.packageinstaller",
                    "com.samsung.android.packageinstaller"
                )
                AppLockItem(
                    packageName = app.packageName,
                    appName = app.loadLabel(pm).toString(),
                    isLocked = lockedPackages.contains(app.packageName),
                    isAntiTamperTarget = isAntiTamper
                )
            }.sortedWith(
                compareByDescending<AppLockItem> { it.isLocked }
                    .thenByDescending { it.isAntiTamperTarget }
                    .thenBy { it.appName.lowercase() }
            )
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ilovalarni Qulflash", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    if (!savedPin.isNullOrEmpty()) {
                        TextButton(onClick = { showSetPinDialog = true }) {
                            Text("PIN o'zgartirish", color = CyberTeal, fontSize = 12.sp)
                        }
                    }
                    IconButton(onClick = onNavigateToIntruderPhotos) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Suratlar")
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
            if (!hasOverlayPermission || !hasUsagePermission || !hasCameraPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("To'liq himoya uchun ruxsatlar", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "AppLock va Intruder Selfie (bosqinchi rasmini olish) ishlashi uchun quyidagi ruxsatlarni yoqing:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!hasOverlayPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                Button(
                                    onClick = {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text("Ustma-ust", fontSize = 12.sp)
                                }
                            }
                            if (!hasUsagePermission) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text("Statistika", fontSize = 12.sp)
                                }
                            }
                            if (!hasCameraPermission) {
                                OutlinedButton(
                                    onClick = { cameraPermLauncher.launch(Manifest.permission.CAMERA) }
                                ) {
                                    Text("Kamera (Selfie)", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Lock Method Selector (PIN / PATTERN / BIOMETRIC)
            uz.sentinel.presentation.components.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                backgroundColor = ObsidianSurface,
                borderColor = GlassBorderTeal,
                borderWidth = 1.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (defaultLockMethod == "BIOMETRIC") Icons.Default.Fingerprint else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = NeonViolet
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shaffof Qulf & Anti-Tamper", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "Sozlamalar ilovasini qulflab qo'ysangiz, begonalar SentinelShield'ni o'chira olmaydi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("PIN" to "PIN Kod", "PATTERN" to "Grafik kalit", "BIOMETRIC" to "Barmoq izi").forEach { (code, label) ->
                            val selected = defaultLockMethod == code
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        preferences.setAppLockDefaultMethod(code)
                                        lockedAppDao.updateAllLockTypes(code)
                                    }
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(appList, key = { it.packageName }) { app ->
                        AppLockRow(
                            item = app,
                            onToggle = { isChecked ->
                                if (isChecked) {
                                    if (savedPin.isNullOrEmpty()) {
                                        pendingAppToLock = app
                                        showSetPinDialog = true
                                    } else {
                                        scope.launch(Dispatchers.IO) {
                                            lockedAppDao.lockApp(
                                                LockedAppEntity(
                                                    packageName = app.packageName,
                                                    appName = app.appName,
                                                    lockType = defaultLockMethod
                                                )
                                            )
                                            appList = appList.map {
                                                if (it.packageName == app.packageName) it.copy(isLocked = true) else it
                                            }
                                        }
                                        AppMonitorService.start(context)
                                    }
                                } else {
                                    scope.launch(Dispatchers.IO) {
                                        lockedAppDao.setLockStatus(app.packageName, false)
                                        appList = appList.map {
                                            if (it.packageName == app.packageName) it.copy(isLocked = false) else it
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSetPinDialog) {
        SetPinDialog(
            onDismiss = {
                showSetPinDialog = false
                pendingAppToLock = null
            },
            onPinSet = { pin ->
                showSetPinDialog = false
                scope.launch {
                    preferences.savePinHash(pin)
                    preferences.setAppLockEnabled(true)
                    pendingAppToLock?.let { app ->
                        withContext(Dispatchers.IO) {
                            lockedAppDao.lockApp(
                                LockedAppEntity(
                                    packageName = app.packageName,
                                    appName = app.appName,
                                    lockType = defaultLockMethod
                                )
                            )
                        }
                        appList = appList.map {
                            if (it.packageName == app.packageName) it.copy(isLocked = true) else it
                        }
                        AppMonitorService.start(context)
                    }
                    pendingAppToLock = null
                }
            }
        )
    }
}

@Composable
fun SetPinDialog(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yangi PIN kod o'rnating", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Qulflangan ilovalarni ochish uchun 6 xonali shaxsiy PIN kod kiriting.")
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("6 xonali PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text("PIN kodni tasdiqlang") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length != 6) {
                        errorMessage = "PIN aynan 6 ta raqam bo'lishi kerak"
                    } else if (pin != confirmPin) {
                        errorMessage = "Kiritilgan PIN kodlar mos kelmadi"
                    } else {
                        onPinSet(pin)
                    }
                }
            ) {
                Text("Saqlash va Qulflash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish")
            }
        }
    )
}

private fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

@Composable
fun AppLockRow(
    item: AppLockItem,
    onToggle: (Boolean) -> Unit
) {
    uz.sentinel.presentation.components.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = ObsidianSurfaceElev,
        borderColor = when {
            item.isLocked -> NeonViolet.copy(alpha = 0.45f)
            item.isAntiTamperTarget -> NeonAmber.copy(alpha = 0.35f)
            else -> GlassBorderWhite
        },
        borderWidth = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.appName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (item.isAntiTamperTarget) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonAmber.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "🛡️ Anti-Tamper",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = item.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = item.isLocked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ObsidianBlack,
                    checkedTrackColor = NeonViolet
                )
            )
        }
    }
}
