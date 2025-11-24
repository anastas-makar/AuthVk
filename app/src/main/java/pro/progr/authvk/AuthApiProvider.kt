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
        val http = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            // порядок важен: сначала подпишем, потом логгинг, потом реакция на 401
            .addInterceptor(signingInterceptor(auth))
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                }
            }
            .addInterceptor(UnauthorizedInterceptor(auth)) // на 401/403 локально вычистит сессию
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL) // оканчивается на /
            .client(http)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()

        return retrofit.create(AuthApi::class.java)
    }
}
