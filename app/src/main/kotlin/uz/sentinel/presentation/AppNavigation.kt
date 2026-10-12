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
import uz.sentinel.presentation.cleaner.CleanerScreen
import uz.sentinel.presentation.cleaner.CleanerViewModel
import uz.sentinel.presentation.dashboard.MainDashboard
import uz.sentinel.presentation.dashboard.DashboardViewModel
import uz.sentinel.presentation.netguard.NetGuardScreen
import uz.sentinel.presentation.netguard.NetGuardViewModel
import uz.sentinel.presentation.onboarding.OnboardingScreen
import uz.sentinel.presentation.permissionaudit.PermissionAuditScreen
import uz.sentinel.presentation.permissionaudit.PermissionAuditViewModel
import uz.sentinel.presentation.settings.StabilityGuideScreen

import uz.sentinel.presentation.applock.IntruderPhotosScreen
import uz.sentinel.presentation.applock.IntruderPhotosViewModel

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
}

@Composable
fun AppNavigation(
    startDestination: String,
    preferences: SentinelPreferences,
    lockedAppDao: LockedAppDao
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
                onNavigateToAntivirus = { navController.navigate(Screen.Antivirus.route) },
                onNavigateToCleaner = { navController.navigate(Screen.Cleaner.route) },
                onNavigateToAppLock = { navController.navigate(Screen.AppLock.route) },
                onNavigateToNetGuard = { navController.navigate(Screen.NetGuard.route) },
                onNavigateToPermissions = { navController.navigate(Screen.PermissionAudit.route) },
                onNavigateToStability = { navController.navigate(Screen.StabilityGuide.route) }
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
    }
}
