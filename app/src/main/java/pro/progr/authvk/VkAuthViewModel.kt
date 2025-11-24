package pro.progr.authvk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vk.id.AccessToken
import com.vk.id.VKID
import com.vk.id.VKIDAuthFail
import com.vk.id.auth.VKIDAuthCallback
import com.vk.id.auth.VKIDAuthParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pro.progr.authapi.AuthInterface

class VkAuthViewModel(
    private val auth: AuthInterface,
    private val api: AuthApi
) : ViewModel() {
    private val signingIn = MutableStateFlow(false)
    private val errorMsg  = MutableStateFlow<String?>(null)

    val ui: StateFlow<AuthUiState> = combine(
        auth.isAuthorized().distinctUntilChanged(),
        signingIn,
        errorMsg
    ) { authorized, signing, err ->
        when {
            err != null -> AuthUiState.Error(err)
            signing     -> AuthUiState.Loading
            authorized  -> AuthUiState.LoggedIn(auth.getName())
            else        -> AuthUiState.LoggedOut
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AuthUiState.Loading)

    fun signIn() {
        // на всякий случай чистим локально (старый мусор)
        if (auth.getSessionId() != null) auth.clearSession()

        errorMsg.value = null
        signingIn.value = true

        viewModelScope.launch(Dispatchers.Default) {
            VKID.instance.authorize(
                callback = object : VKIDAuthCallback {
                    override fun onAuth(accessToken: AccessToken) {
                        viewModelScope.launch(Dispatchers.IO) {
                            runCatching {
                                api.signIn(
                                    AuthVkRequest(
                                        vkAccessToken = accessToken.token,
                                        deviceId = auth.getDeviceId()
                                    )
                                ).let { resp ->
                                    if (!resp.isSuccessful) error("HTTP ${resp.code()} ${resp.message()}")
                                    resp.body() ?: error("Empty body")
                                }
                            }.onSuccess { body ->
                                auth.setSession(
                                    sessionId = body.sessionId,
                                    sessionSecret = body.sessionSecret
                                )
                                auth.setName(body.name)
                                errorMsg.value = null
                            }.onFailure { e ->
                                errorMsg.value = e.message ?: "Auth failed"
                            }.also {
                                signingIn.value = false
                            }
                        }
                    }
                    override fun onFail(fail: VKIDAuthFail) {
                        errorMsg.value = when (fail) {
                            is VKIDAuthFail.Canceled -> "Отменено"
                            else -> fail.description
                        }
                        signingIn.value = false
                    }
                },
                params = VKIDAuthParams { }
            )
        }
    }

    fun logout() = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            // если сессии уже нет — просто пройдём дальше
            if (auth.getSessionId() != null) {
                api.logout() // 204 ожидаем; UnauthorizedInterceptor при 401/403 тоже всё очистит
            }
        }.onFailure {
            // логируй при желании, но локально чистим в любом случае
        }
        auth.clearSession()
        errorMsg.value = null
    }
}

