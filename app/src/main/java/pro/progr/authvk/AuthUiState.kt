package pro.progr.authvk

sealed class AuthUiState {
    data object LoggedOut : AuthUiState()
    data object Loading   : AuthUiState()
    data class LoggedIn(val name: String?) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}