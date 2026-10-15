package uz.sentinel.presentation.applock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import uz.sentinel.core.utils.LocaleHelper
import uz.sentinel.presentation.theme.SentinelShieldTheme

/**
 * Transparent FragmentActivity for AppLock authentication.
 * Supports hardware BiometricPrompt (Fingerprint / Face Unlock), PIN, and Pattern,
 * and prevents screenshots/recents snooping via FLAG_SECURE.
 */
@AndroidEntryPoint
class LockPromptActivity : FragmentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screenshots and recents preview of the lock screen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        val lockedPackage = intent?.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        if (lockedPackage.isBlank()) {
            finish()
            return
        }

        // Pressing Back exits to Home Launcher so user cannot bypass lock screen into locked app
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToHomeAndFinish()
            }
        })

        setContent {
            SentinelShieldTheme {
                val viewModel: LockScreenViewModel = hiltViewModel()
                LockScreen(
                    packageName = lockedPackage,
                    onUnlocked = {
                        finish()
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
                    },
                    onExitToHome = {
                        navigateToHomeAndFinish()
                    },
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun navigateToHomeAndFinish() {
        runCatching {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        }
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_locked_package_name"

        fun createIntent(context: Context, packageName: String): Intent {
            return Intent(context, LockPromptActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }
        }
    }
}
