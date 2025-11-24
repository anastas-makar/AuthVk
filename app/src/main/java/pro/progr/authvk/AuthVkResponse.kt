package pro.progr.authvk

data class AuthVkResponse(
    val sessionId: String,
    val sessionSecret: String,//приходит один раз при успешной авторизации, хранится на устройстве в зашифрованном виде
    val name: String?
)