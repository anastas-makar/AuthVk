package pro.progr.authvk

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    suspend fun signIn(body: AuthVkRequest): Response<AuthVkResponse>
    suspend fun logout(): Response<Unit>
}

internal interface KeyServiceApi {
    @POST("auth/vk")
    suspend fun signIn(@Body body: AuthVkRequest): Response<AuthVkResponse>
}

internal interface AuthSessionApi {
    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}
