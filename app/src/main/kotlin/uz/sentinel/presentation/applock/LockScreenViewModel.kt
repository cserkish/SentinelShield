package uz.sentinel.presentation.applock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.sentinel.core.utils.IntruderSelfieManager
import uz.sentinel.domain.repository.AppLockRepository
import javax.inject.Inject

data class LockScreenUiState(
    val lockMethod: LockMethod = LockMethod.PIN,
    val pinDigits: List<Int> = emptyList(),
    val patternDots: List<Int> = emptyList(),
    val wrongAttempts: Int = 0,
    val isUnlocked: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val showBiometricPrompt: Boolean = false
)

enum class LockMethod { PIN, PATTERN, BIOMETRIC }

@HiltViewModel
class LockScreenViewModel @Inject constructor(
    private val appLockRepository: AppLockRepository,
    private val intruderSelfieManager: IntruderSelfieManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockScreenUiState())
    val uiState: StateFlow<LockScreenUiState> = _uiState.asStateFlow()

    private var targetPackage: String = ""

    fun init(packageName: String) {
        targetPackage = packageName
        viewModelScope.launch {
            val method = appLockRepository.getLockMethod(packageName)
            _uiState.update { it.copy(lockMethod = method) }
            if (method == LockMethod.BIOMETRIC) {
                _uiState.update { it.copy(showBiometricPrompt = true) }
            }
        }
    }

    fun onPinDigitAdded(digit: Int) {
        val current = _uiState.value.pinDigits
        if (current.size >= 6) return
        val updated = current + digit
        _uiState.update { it.copy(pinDigits = updated, errorMessage = null) }
        if (updated.size == 6) {
            verifyPin(updated.joinToString(""))
        }
    }

    fun onPinDigitRemoved() {
        val current = _uiState.value.pinDigits
        if (current.isEmpty()) return
        _uiState.update { it.copy(pinDigits = current.dropLast(1), errorMessage = null) }
    }

    fun verifyPin(enteredPin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val isCorrect = appLockRepository.verifyPin(targetPackage, enteredPin)
            if (isCorrect) {
                unlock()
            } else {
                onWrongAttempt()
            }
        }
    }

    fun verifyPattern(patternDots: List<Int>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val isCorrect = appLockRepository.verifyPattern(targetPackage, patternDots)
            if (isCorrect) {
                unlock()
            } else {
                onWrongAttempt()
            }
        }
    }

    fun launchBiometric(activity: FragmentActivity) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    unlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    _uiState.update { it.copy(errorMessage = errString.toString(), isLoading = false) }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onWrongAttempt()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometrik autentifikatsiya")
            .setSubtitle("Ilovaga kirish uchun barmoq izini ishlating")
            .setNegativeButtonText("PIN")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun onWrongAttempt() {
        val attempts = _uiState.value.wrongAttempts + 1
        _uiState.update {
            it.copy(
                wrongAttempts = attempts,
                isLoading = false,
                pinDigits = emptyList(),
                patternDots = emptyList(),
                errorMessage = if (attempts < 3) "Noto'g'ri! Qoldi: ${3 - attempts}" else null
            )
        }
        if (attempts >= 3) {
            captureIntruder()
        }
    }

    private fun captureIntruder() {
        viewModelScope.launch {
            intruderSelfieManager.capture(targetPackage)
            _uiState.update {
                it.copy(
                    wrongAttempts = 0,
                    errorMessage = "Kirish rad etildi. Suratga olindi."
                )
            }
        }
    }

    private fun unlock() {
        _uiState.update {
            it.copy(
                isUnlocked = true,
                isLoading = false,
                wrongAttempts = 0,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            appLockRepository.grantTemporaryAccess(targetPackage)
        }
    }

    fun onPatternDotAdded(dot: Int) {
        val current = _uiState.value.patternDots
        if (current.contains(dot)) return
        _uiState.update { it.copy(patternDots = current + dot) }
    }

    fun onPatternCompleted() {
        val dots = _uiState.value.patternDots
        if (dots.size < 4) {
            _uiState.update { it.copy(errorMessage = "Kamida 4 ta nuqta ulang", patternDots = emptyList()) }
            return
        }
        verifyPattern(dots)
    }

    fun resetPin() {
        _uiState.update { it.copy(pinDigits = emptyList(), errorMessage = null) }
    }

    fun switchToPin() {
        _uiState.update { it.copy(lockMethod = LockMethod.PIN, errorMessage = null) }
    }
}
