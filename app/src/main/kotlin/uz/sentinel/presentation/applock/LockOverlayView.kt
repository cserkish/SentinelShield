package uz.sentinel.presentation.applock

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import uz.sentinel.core.utils.IntruderSelfieManager
import uz.sentinel.domain.repository.AppLockRepository
import uz.sentinel.presentation.theme.SentinelShieldTheme

class LockOverlayView(
    context: Context,
    private val appLockRepository: AppLockRepository,
    private val intruderSelfieManager: IntruderSelfieManager,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()

    private var packageNameState by mutableStateOf("")
    private var onUnlockListener: (() -> Unit)? = null
    private val viewModel = LockScreenViewModel(appLockRepository, intruderSelfieManager)

    init {
        savedStateRegistryController.performRestore(null)
        setViewTreeLifecycleOwner(this)
        setViewTreeViewModelStoreOwner(this)
        setViewTreeSavedStateRegistryOwner(this)

        val composeView = ComposeView(context).apply {
            setContent {
                SentinelShieldTheme {
                    LockScreen(
                        packageName = packageNameState,
                        onUnlocked = { onUnlockListener?.invoke() },
                        viewModel = viewModel
                    )
                }
            }
        }
        addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun setPackageName(pkg: String) {
        packageNameState = pkg
        viewModel.init(pkg)
    }

    fun setOnUnlockListener(listener: () -> Unit) {
        onUnlockListener = listener
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDetachedFromWindow() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
        super.onDetachedFromWindow()
    }
}
