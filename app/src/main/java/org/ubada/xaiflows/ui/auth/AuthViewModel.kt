/**
 * AuthViewModel.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Thin ViewModel wrapping AuthRepository for the three auth screens
 * (Login/Signup/VerifyOtp). Owns only request lifecycle state
 * (AuthUiState) — the actual "is the user logged in" truth lives in
 * AuthSession, which the screens/AuthNavHost observe independently.
 *
 * Shared across all three screens (constructed once by AuthNavHost) so
 * e.g. the email typed on SignupScreen is still available when
 * VerifyOtpScreen needs it, without re-plumbing it through navigation
 * arguments for every field.
 */
package org.ubada.xaiflows.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.ubada.xaiflows.core.auth.AuthRepository
import org.ubada.xaiflows.core.utils.AuthErrorParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Called when navigating between auth screens so a stale Error/Success
     *  from the previous screen doesn't flash on the next one. */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun register(name: String, email: String, password: String, turnstileToken: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val response = AuthRepository.register(name, email, password, turnstileToken)
                AuthUiState.RegisterSuccess(response.email)
            } catch (e: Exception) {
                AuthUiState.Error(AuthErrorParser.parse(e).message)
            }
        }
    }

    fun verifyOtp(email: String, otp: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                AuthRepository.verifyOtp(email, otp)
                AuthUiState.VerifyOtpSuccess
            } catch (e: Exception) {
                AuthUiState.Error(AuthErrorParser.parse(e).message)
            }
        }
    }

    fun login(email: String, password: String, turnstileToken: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                AuthRepository.login(email, password, turnstileToken)
                AuthUiState.LoginSuccess
            } catch (e: Exception) {
                AuthUiState.Error(AuthErrorParser.parse(e).message)
            }
        }
    }
}
