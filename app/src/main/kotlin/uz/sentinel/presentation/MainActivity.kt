package uz.sentinel.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import uz.sentinel.core.utils.LocaleHelper
import uz.sentinel.data.local.db.LockedAppDao
import uz.sentinel.data.local.prefs.SentinelPreferences
import uz.sentinel.presentation.theme.SentinelShieldTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private companion object {
        private const val TAG = "MainActivity"
    }

    @Inject
    lateinit var preferences: SentinelPreferences

    @Inject
    lateinit var lockedAppDao: LockedAppDao

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Notification permission result: $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContent {
                SentinelShieldTheme {
                    val onboardingState = preferences.isOnboardingCompleted.collectAsState(initial = null)
                    val completed = onboardingState.value

                    if (completed == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0D1B2A))
                        )
                    } else {
                        val startDest = if (completed) Screen.Dashboard.route else Screen.Onboarding.route
                        AppNavigation(
                            startDestination = startDest,
                            preferences = preferences,
                            lockedAppDao = lockedAppDao,
                            onLanguageChanged = { recreate() }
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        try {
                            if (ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to request notification permission", e)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Fatal startup caught in onCreate", e)
            setContent {
                SentinelShieldTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0D1B2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SentinelShield ishga tushirilmoqda...",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
