package pro.progr.authvk

data class AuthVkResponse(
    val sessionId: String,
    val sessionSecret: String,
    val personalEncryptionKey: String,
    val personalEncryptionKeyVersion: Int,
    val name: String?
)
