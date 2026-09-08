package pro.progr.authvk

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pro.progr.authapi.AuthInterface
import pro.progr.authapi.UnauthorizedInterceptor
import pro.progr.authapi.signingInterceptor
import pro.progr.authvk.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AuthApiProvider {
    fun api(auth: AuthInterface): AuthApi {
        val keyServiceHttp = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    // BODY/HEADERS запрещены: ответ содержит ключи и секрет сессии.
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()

        val authHttp = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(signingInterceptor(auth))
            .addInterceptor(UnauthorizedInterceptor(auth))
            .build()

        val gson = GsonConverterFactory.create(GsonBuilder().create())
        val keyServiceApi = Retrofit.Builder()
            .baseUrl(BuildConfig.KEY_SERVICE_BASE_URL)
            .client(keyServiceHttp)
            .addConverterFactory(gson)
            .build()
            .create(KeyServiceApi::class.java)

        val authSessionApi = Retrofit.Builder()
            .baseUrl(BuildConfig.AUTH_API_BASE_URL)
            .client(authHttp)
            .addConverterFactory(gson)
            .build()
            .create(AuthSessionApi::class.java)

        return object : AuthApi {
            override suspend fun signIn(body: AuthVkRequest) = keyServiceApi.signIn(body)
            override suspend fun logout() = authSessionApi.logout()
        }
    }
}
