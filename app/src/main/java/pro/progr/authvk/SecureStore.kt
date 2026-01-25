package pro.progr.authvk

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.KeyTemplates

private const val SESSION_ID = "sid"

private const val SESSION_SECRET = "sec_enc"

private const val USER_NAME = "name"

class SecureStore(ctx: Context) {
    private val app = ctx.applicationContext
    private val prefs: SharedPreferences =
        app.getSharedPreferences("auth.prefs", Context.MODE_PRIVATE)

    private val aead: Aead by lazy {
        AeadConfig.register() // один раз на процесс

        buildAead()
    }

    private fun buildAead(): Aead {
        return try {
            val handle = AndroidKeysetManager.Builder()
                .withSharedPref(app, "tink_keyset", "tink_prefs") // где хранить зашифрованный keyset
                .withMasterKeyUri("android-keystore://master_key_tink") // мастер-ключ в Android Keystore
                .withKeyTemplate(KeyTemplates.get("AES256_GCM")) // ключ для AEAD
                .build()
                .keysetHandle
            handle.getPrimitive(Aead::class.java)

        } catch (t: Throwable) {

            // Хранилище сломано/несовместимо — сбрасываем и пересоздаём
            wipeTink(app)

            val handle = AndroidKeysetManager.Builder()
                .withSharedPref(app, "tink_keyset", "tink_prefs") // где хранить зашифрованный keyset
                .withMasterKeyUri("android-keystore://master_key_tink") // мастер-ключ в Android Keystore
                .withKeyTemplate(KeyTemplates.get("AES256_GCM")) // ключ для AEAD
                .build()
                .keysetHandle
            handle.getPrimitive(Aead::class.java)
        }
    }

    private fun wipeTink(context: Context) {
        // 1) снести keyset из prefs
        context.getSharedPreferences("tink_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit() // именно commit, чтобы сразу

        // 2) снести мастер-ключ из Keystore
        val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias("master_key_tink")) {
            ks.deleteEntry("master_key_tink")
        }

        // снести SESSION_SECRET
        prefs.edit()
            .remove(SESSION_SECRET)
            .commit()
    }

    // --- утилиты base64url ---
    private fun b64u(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    private fun b64uDec(s: String): ByteArray =
        Base64.decode(s, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    // ==== Публичное API для Auth ====

    fun putSessionId(sessionId: String?) {
        prefs.edit().apply {
            if (sessionId == null) remove(SESSION_ID) else putString(SESSION_ID, sessionId)
        }.apply()
    }
    fun getSessionId(): String? = prefs.getString(SESSION_ID, null)

    fun clearSession() = prefs.edit()
        .remove(SESSION_ID)
        .remove(SESSION_SECRET)
        .remove(USER_NAME)
        .apply()

    /** Шифруем sessionSecret (который пришёл от сервера, в base64url) и кладём в prefs */
    fun putSessionSecretB64u(secretB64u: String?) {
        prefs.edit().apply {
            if (secretB64u == null) {
                remove(SESSION_SECRET)
            } else {
                val ct = aead.encrypt(b64uDec(secretB64u), /*AD=*/"v1".toByteArray())
                putString(SESSION_SECRET, b64u(ct))
            }
        }.apply()
    }

    /** Достаём и расшифровываем sessionSecret как raw bytes; вернём null, если нет/ключ потерян */
    fun getSessionSecretRaw(): ByteArray? {
        val enc = prefs.getString(SESSION_SECRET, null) ?: return null
        return try {
            aead.decrypt(b64uDec(enc), /*AD=*/"v1".toByteArray())
        } catch (_: Throwable) {
            null // ключ потерян (wipe KS/переустановка ОС) — трезво обрабатывай как logout
        }
    }

    fun putDisplayName(name: String?) {
        prefs.edit().apply {
            if (name == null) remove(USER_NAME) else putString(USER_NAME, name)
        }.apply()
    }
    fun getDisplayName(): String? = prefs.getString(USER_NAME, null)

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}