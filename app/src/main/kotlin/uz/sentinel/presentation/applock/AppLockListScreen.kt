package uz.sentinel.presentation.applock

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.sentinel.data.local.db.LockedAppDao
import uz.sentinel.data.local.entity.LockedAppEntity
import uz.sentinel.data.local.prefs.SentinelPreferences
import uz.sentinel.service.AppMonitorService

data class AppLockItem(
    val packageName: String,
    val appName: String,
    val isLocked: Boolean
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

    val savedPin by preferences.appLockPinHash.collectAsState(initial = null)
    var showSetPinDialog by remember { mutableStateOf(false) }
    var pendingAppToLock by remember { mutableStateOf<AppLockItem?>(null) }

    // Recheck permissions on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
                hasUsagePermission = checkUsageStatsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || it.packageName == "com.android.chrome" }
            val lockedPackages = lockedAppDao.getLockedPackageNames().toSet()

            appList = installed.map { app ->
                AppLockItem(
                    packageName = app.packageName,
                    appName = app.loadLabel(pm).toString(),
                    isLocked = lockedPackages.contains(app.packageName)
                )
            }.sortedByDescending { it.isLocked }
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
            // Permission Warning Banner if missing
            if (!hasOverlayPermission || !hasUsagePermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Qulflash ishlashi uchun ruxsat kerak", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Ilovani qulflash uchun 'Ustma-ust ko'rsatish' va 'Foydalanish statistikasi' ruxsatlarini yoqing.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row {
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
                                    Text("Ustma-ust ruxsati")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            if (!hasUsagePermission) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text("Statistika ruxsati")
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            uz.sentinel.presentation.components.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                backgroundColor = uz.sentinel.presentation.theme.ObsidianSurface,
                borderColor = uz.sentinel.presentation.theme.GlassBorderTeal,
                borderWidth = 1.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = uz.sentinel.presentation.theme.NeonViolet
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Shaxsiy ilovalar himoyasi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = if (savedPin.isNullOrEmpty())
                                "Ilovalarni qulflash uchun shaxsiy 6 xonali PIN o'rnating."
                            else
                                "Qulflangan ilovalar ochilganda PIN kod so'raladi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                                                    appName = app.appName
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

    // Set Initial PIN Dialog
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
                                    appName = app.appName
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
        backgroundColor = uz.sentinel.presentation.theme.ObsidianSurfaceElev,
        borderColor = if (item.isLocked) uz.sentinel.presentation.theme.NeonViolet.copy(alpha = 0.4f) else uz.sentinel.presentation.theme.GlassBorderWhite,
        borderWidth = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.appName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                    checkedThumbColor = uz.sentinel.presentation.theme.ObsidianBlack,
                    checkedTrackColor = uz.sentinel.presentation.theme.NeonViolet
                )
            )
        }
    }
}
