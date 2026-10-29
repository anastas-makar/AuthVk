package pro.progr.authvk

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vk.id.AccessToken
import com.vk.id.VKID
import com.vk.id.VKIDAuthFail
import com.vk.id.auth.VKIDAuthCallback
import com.vk.id.auth.VKIDAuthParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pro.progr.diagnostics.Diagnostics
import pro.progr.diagnostics.NoOpDiagnostics
import pro.progr.authapi.AuthInterface
import pro.progr.personalcrypto.PersonalCrypto

private class AuthHttpException(val statusCode: Int) : RuntimeException("auth_http_$statusCode")

class VkAuthViewModel @JvmOverloads constructor(
    context: Context,
    private val auth: AuthInterface,
    private val personalCrypto: PersonalCrypto,
    private val api: AuthApi,
    private val diagnostics: Diagnostics = NoOpDiagnostics
) : ViewModel() {
    private val appContext = context.applicationContext
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

    init {
        normalizeLocalAuthorizationState()
    }

    fun signIn() {
        diagnostics.breadcrumb("auth: sign_in_started")
        // на всякий случай чистим локально (старый мусор)
        if (auth.getSessionId() != null || personalCrypto.hasKey()) {
            auth.clearSession()
            personalCrypto.clearKey()
            diagnostics.breadcrumb("auth: stale_local_credentials_cleared")
        }

        errorMsg.value = null
        signingIn.value = true

        viewModelScope.launch(Dispatchers.Default) {
            try {
                VKID.instance.authorize(
                    callback = object : VKIDAuthCallback {
                        override fun onAuth(accessToken: AccessToken) {
                            diagnostics.breadcrumb("auth: vk_authorization_succeeded")
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    diagnostics.breadcrumb("auth: key_service_request_started")
                                    val response = api.signIn(
                                        AuthVkRequest(
                                            vkAccessToken = accessToken.token,
                                            deviceId = auth.getDeviceId()
                                        )
                                    )
                                    if (!response.isSuccessful) {
                                        diagnostics.breadcrumb("auth: key_service_http_error status=${response.code()}")
                                        throw AuthHttpException(response.code())
                                    }
                                    val body = response.body()
                                        ?: throw IllegalStateException("auth_key_service_empty_response")
                                    diagnostics.breadcrumb("auth: key_service_response_received")
                                    saveAuthorization(body)
                                    diagnostics.breadcrumb("auth: local_session_saved")
                                    errorMsg.value = null
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: AuthHttpException) {
                                    val issueKey = if (e.statusCode >= 500) {
                                        "auth_key_service_server_error"
                                    } else {
                                        "auth_key_service_rejected"
                                    }
                                    diagnostics.reportNonFatal(e, issueKey = issueKey)
                                    errorMsg.value = e.message ?: appContext.getString(R.string.auth_failed)
                                } catch (e: java.io.IOException) {
                                    diagnostics.breadcrumb("auth: sign_in_network_failure")
                                    diagnostics.reportNonFatal(e, issueKey = "auth_sign_in_network_failure")
                                    errorMsg.value = e.message ?: appContext.getString(R.string.auth_failed)
                                } catch (e: Throwable) {
                                    diagnostics.breadcrumb("auth: sign_in_unexpected_failure type=${e.javaClass.simpleName}")
                                    diagnostics.reportNonFatal(e, issueKey = "auth_sign_in_unexpected")
                                    errorMsg.value = e.message ?: appContext.getString(R.string.auth_failed)
                                } finally {
                                    signingIn.value = false
                                }
                            }
                        }
                        override fun onFail(fail: VKIDAuthFail) {
                            errorMsg.value = when (fail) {
                                is VKIDAuthFail.Canceled -> {
                                    diagnostics.breadcrumb("auth: vk_authorization_cancelled")
                                    appContext.getString(R.string.auth_cancelled)
                                }
                                else -> {
                                    diagnostics.breadcrumb("auth: vk_authorization_failed")
                                    diagnostics.reportNonFatal(
                                        IllegalStateException("vk_id_authorization_failed"),
                                        issueKey = "auth_vk_id_authorization_failed"
                                    )
                                    fail.description
                                }
                            }
                            signingIn.value = false
                        }
                    },
                    params = VKIDAuthParams { }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                diagnostics.breadcrumb("auth: vk_authorize_call_failed type=${e.javaClass.simpleName}")
                diagnostics.reportNonFatal(e, issueKey = "auth_vk_authorize_call_failed")
                errorMsg.value = e.message ?: appContext.getString(R.string.auth_failed)
                signingIn.value = false
            }
        }
    }

    fun logout() = viewModelScope.launch(Dispatchers.IO) {
        diagnostics.breadcrumb("auth: logout_started")
        try {
            // если сессии уже нет — просто пройдём дальше
            if (auth.getSessionId() != null) {
                val response = api.logout() // 204 ожидаем; UnauthorizedInterceptor при 401/403 тоже всё очистит
                if (!response.isSuccessful) {
                    diagnostics.breadcrumb("auth: logout_http_error status=${response.code()}")
                    if (response.code() >= 500) {
                        diagnostics.reportNonFatal(
                            IllegalStateException("auth_logout_http_${response.code()}"),
                            issueKey = "auth_logout_server_error"
                        )
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: java.io.IOException) {
            diagnostics.breadcrumb("auth: logout_network_failure")
        } catch (e: Throwable) {
            diagnostics.breadcrumb("auth: logout_unexpected_failure type=${e.javaClass.simpleName}")
            diagnostics.reportNonFatal(e, issueKey = "auth_logout_unexpected")
        } finally {
            // Локальную сессию очищаем независимо от результата сетевого logout.
            auth.clearSession()
            personalCrypto.clearKey()
            errorMsg.value = null
            diagnostics.breadcrumb("auth: local_session_cleared")
        }
    }

    private fun saveAuthorization(body: AuthVkResponse) {
        try {
            // Сначала сохраняем ключ. Авторизованное состояние Auth публикуется
            // только после успешного сохранения обеих частей.
            personalCrypto.setKey(
                keyB64u = body.personalEncryptionKey,
                version = body.personalEncryptionKeyVersion
            )
            auth.setSession(
                sessionId = body.sessionId,
                sessionSecret = body.sessionSecret
            )
            auth.setName(body.name)
        } catch (t: Throwable) {
            auth.clearSession()
            personalCrypto.clearKey()
            throw t
        }
    }

    private fun normalizeLocalAuthorizationState() {
        val hasSession = auth.getSessionId() != null
        val hasPersonalKey = personalCrypto.hasKey()
        if (hasSession != hasPersonalKey) {
            diagnostics.breadcrumb("auth: local_authorization_state_inconsistent")
            diagnostics.reportNonFatal(
                IllegalStateException("auth_local_authorization_state_inconsistent"),
                issueKey = "auth_local_state_inconsistent"
            )
            auth.clearSession()
            personalCrypto.clearKey()
        }
    }
}

