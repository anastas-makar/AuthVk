package pro.progr.authvk

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pro.progr.authapi.AuthInterface
import pro.progr.diagnostics.Diagnostics
import pro.progr.diagnostics.NoOpDiagnostics
import pro.progr.personalcrypto.PersonalCrypto

class VkAuthViewModelFactory @JvmOverloads constructor(
    private val context: Context,
    private val auth: AuthInterface,
    private val personalCrypto: PersonalCrypto,
    private val api: AuthApi,
    private val diagnostics: Diagnostics = NoOpDiagnostics
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return VkAuthViewModel(
            context = context,
            auth = auth,
            personalCrypto = personalCrypto,
            api = api,
            diagnostics = diagnostics
        ) as T
    }
}
