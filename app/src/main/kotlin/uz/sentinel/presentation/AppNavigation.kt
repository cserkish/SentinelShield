package uz.sentinel.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import uz.sentinel.data.local.db.LockedAppDao
import uz.sentinel.data.local.prefs.SentinelPreferences
import uz.sentinel.presentation.antivirus.AntivirusScreen
import uz.sentinel.presentation.antivirus.AntivirusViewModel
import uz.sentinel.presentation.applock.AppLockListScreen
import uz.sentinel.presentation.applock.IntruderPhotosScreen
import uz.sentinel.presentation.applock.IntruderPhotosViewModel
import uz.sentinel.presentation.appmanager.AppUninstallScreen
import uz.sentinel.presentation.appmanager.AppUninstallViewModel
import uz.sentinel.presentation.cleaner.CleanerScreen
import uz.sentinel.presentation.cleaner.CleanerViewModel
import uz.sentinel.presentation.cleaner.StorageMigrationScreen
import uz.sentinel.presentation.cleaner.StorageMigrationViewModel
import uz.sentinel.presentation.dashboard.DashboardViewModel
import uz.sentinel.presentation.dashboard.MainDashboard
import uz.sentinel.presentation.gamebooster.GameBoosterScreen
import uz.sentinel.presentation.gamebooster.GameBoosterViewModel
import uz.sentinel.presentation.netguard.NetGuardScreen
import uz.sentinel.presentation.netguard.NetGuardViewModel
import uz.sentinel.presentation.onboarding.OnboardingScreen
import uz.sentinel.presentation.permissionaudit.PermissionAuditScreen
import uz.sentinel.presentation.permissionaudit.PermissionAuditViewModel
import uz.sentinel.presentation.settings.StabilityGuideScreen
import uz.sentinel.presentation.wifisecurity.WifiSecurityScreen
import uz.sentinel.presentation.wifisecurity.WifiSecurityViewModel

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object Antivirus : Screen("antivirus")
    object Cleaner : Screen("cleaner")
    object AppLock : Screen("applock")
    object IntruderPhotos : Screen("intruder_photos")
    object NetGuard : Screen("netguard")
    object PermissionAudit : Screen("permission_audit")
    object StabilityGuide : Screen("stability_guide")
    object AppUninstall : Screen("app_uninstall")
    object StorageMigration : Screen("storage_migration")
    object GameBooster : Screen("game_booster")
    object WifiSecurity : Screen("wifi_security")
}

@Composable
fun AppNavigation(
    startDestination: String,
    preferences: SentinelPreferences,
    lockedAppDao: LockedAppDao,
    onLanguageChanged: () -> Unit = {}
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinishOnboarding = {
                    scope.launch {
                        preferences.setOnboardingCompleted(true)
                    }
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val viewModel: DashboardViewModel = hiltViewModel()
            MainDashboard(
                viewModel = viewModel,
                preferences = preferences,
                onNavigateToAntivirus = { navController.navigate(Screen.Antivirus.route) },
                onNavigateToCleaner = { navController.navigate(Screen.Cleaner.route) },
                onNavigateToAppLock = { navController.navigate(Screen.AppLock.route) },
                onNavigateToNetGuard = { navController.navigate(Screen.NetGuard.route) },
                onNavigateToPermissions = { navController.navigate(Screen.PermissionAudit.route) },
                onNavigateToStability = { navController.navigate(Screen.StabilityGuide.route) },
                onNavigateToAppUninstall = { navController.navigate(Screen.AppUninstall.route) },
                onNavigateToGameBooster = { navController.navigate(Screen.GameBooster.route) },
                onNavigateToWifiSecurity = { navController.navigate(Screen.WifiSecurity.route) },
                onLanguageChanged = onLanguageChanged
            )
        }

        composable(Screen.Antivirus.route) {
            val viewModel: AntivirusViewModel = hiltViewModel()
            AntivirusScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Cleaner.route) {
            val viewModel: CleanerViewModel = hiltViewModel()
            CleanerScreen(
                viewModel = viewModel,
                onNavigateToStorageMigration = { navController.navigate(Screen.StorageMigration.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AppLock.route) {
            AppLockListScreen(
                lockedAppDao = lockedAppDao,
                preferences = preferences,
                onNavigateToIntruderPhotos = { navController.navigate(Screen.IntruderPhotos.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.IntruderPhotos.route) {
            val viewModel: IntruderPhotosViewModel = hiltViewModel()
            IntruderPhotosScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NetGuard.route) {
            val viewModel: NetGuardViewModel = hiltViewModel()
            NetGuardScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PermissionAudit.route) {
            val viewModel: PermissionAuditViewModel = hiltViewModel()
            PermissionAuditScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StabilityGuide.route) {
            StabilityGuideScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AppUninstall.route) {
            val viewModel: AppUninstallViewModel = hiltViewModel()
            AppUninstallScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StorageMigration.route) {
            val viewModel: StorageMigrationViewModel = hiltViewModel()
            StorageMigrationScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.GameBooster.route) {
            val viewModel: GameBoosterViewModel = hiltViewModel()
            GameBoosterScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.WifiSecurity.route) {
            val viewModel: WifiSecurityViewModel = hiltViewModel()
            WifiSecurityScreen(
                viewModel = viewModel,
                onNavigateToNetGuard = { navController.navigate(Screen.NetGuard.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
