package uz.sentinel.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uz.sentinel.data.local.db.LockedAppDao
import uz.sentinel.data.local.prefs.SentinelPreferences
import uz.sentinel.presentation.theme.SentinelShieldTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferences: SentinelPreferences

    @Inject
    lateinit var lockedAppDao: LockedAppDao

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        requestRequiredPermissions()

        lifecycleScope.launch {
            val isOnboardingCompleted = preferences.isOnboardingCompleted.first()
            val startDest = if (isOnboardingCompleted) Screen.Dashboard.route else Screen.Onboarding.route

            setContent {
                SentinelShieldTheme {
                    AppNavigation(
                        startDestination = startDest,
                        preferences = preferences,
                        lockedAppDao = lockedAppDao
                    )
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
