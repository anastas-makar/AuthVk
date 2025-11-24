package pro.progr.authvk

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import pro.progr.authapi.AuthInterface
import java.time.LocalDateTime
import java.time.ZoneOffset

class Auth(context: Context) : AuthInterface {
    private val rnd = SecureRandom()
    private val store = SecureStore(context.applicationContext)

    private val _authorized = MutableStateFlow(store.getSessionId() != null)
    override fun isAuthorized(): StateFlow<Boolean> = _authorized

    init { DeviceIdProvider.init(context.applicationContext) }

    override fun getDeviceId(): String = DeviceIdProvider.get()
    override fun getEpochSecond(): Long {
        return LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
    }

    override fun getSessionId(): String? = store.getSessionId()

    override fun getSignAlg(): String {
        return "hmac-sha256-v1"
    }

    override fun setSession(sessionId: String, sessionSecret: String) {
        store.putSessionId(sessionId)
        store.putSessionSecretB64u(sessionSecret) // хранится шифр-текст
        _authorized.value = true
    }

    override fun clearSession() {
        store.clearSession()
        _authorized.value = false
    }

    override fun getName(): String? = store.getDisplayName()
    override fun setName(name: String?) = store.putDisplayName(name)

    override fun getNonce(): String {
        val b = ByteArray(16); rnd.nextBytes(b)
        return Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    override fun getHash(
        sessionId: String,
        deviceId: String,
        nonce: String,
        method: String,
        pathQuery: String,
        epochSecond: Long,
        bodyBytes: ByteArray
    ): String {
        val secretRaw = store.getSessionSecretRaw() ?: error("No sessionSecret")

        val bodySha = MessageDigest.getInstance("SHA-256").digest(bodyBytes)
        val bh = Base64.encodeToString(
            bodySha,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        val canonical = buildString {
            append("sid=").append(sessionId).append('\n')
            append("did=").append(deviceId).append('\n')
            append("nonce=").append(nonce).append('\n')
            append("s=").append(epochSecond).append('\n')
            append("pqu=").append(pathQuery).append('\n')
            append("m=").append(method).append('\n')
            append("bh=").append(bh) // без завершающего \n
        }

        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(secretRaw, "HmacSHA256"))
        }
        val sig = mac.doFinal(canonical.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(sig, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
