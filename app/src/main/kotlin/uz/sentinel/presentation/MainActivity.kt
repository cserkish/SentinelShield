package uz.sentinel.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
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
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        requestRequiredPermissions()

        setContent {
            val isOnboardingCompleted by preferences.isOnboardingCompleted.collectAsState(initial = null)

            splashScreen.setKeepOnScreenCondition { isOnboardingCompleted == null }

            SentinelShieldTheme {
                val completed = isOnboardingCompleted
                if (completed != null) {
                    val startDest = if (completed) Screen.Dashboard.route else Screen.Onboarding.route
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
