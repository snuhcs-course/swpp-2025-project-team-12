package com.example.dailyinsight.data.network

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.dailyinsight.data.datastore.CookieKeys
import com.example.dailyinsight.data.datastore.cookieDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Unified Retrofit Instance for all API calls
 * Base URL: http://10.0.2.2:8000/ (for Android emulator)
 */
object RetrofitInstance {
    private const val BASE_URL = "http://ec2-13-124-209-234.ap-northeast-2.compute.amazonaws.com:8000/"
//     private const val BASE_URL = "http://10.0.2.2:8000/"
    // Toggle: true = 1st tab network calls return mock responses


    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private lateinit var client : OkHttpClient
    lateinit var api : ApiService

    lateinit var cookieJar: ProxyCookieJar
        private set

    fun init(context: Context) {
        val realCookieJar = MyCookieJar()

        cookieJar = ProxyCookieJar(
            real = realCookieJar,
            dataStore = context.cookieDataStore
        )

        client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(AuthInterceptor(context.applicationContext))
            .addInterceptor(CsrfInterceptor(context.cookieDataStore))
            .addInterceptor(logging)
            .build()

        api = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            // JSON 파싱 작업이 백그라운드 IO 스레드에서 처리됨
            .callbackExecutor(java.util.concurrent.Executors.newSingleThreadExecutor())
            .build()
            .create(ApiService::class.java)
    }

    private class AuthInterceptor(private val context: Context) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())
            if (response.code == 401) {
                // ✅ runBlocking 제거, IO 스레드에서 비동기 처리 (ANR 방지)
                CoroutineScope(Dispatchers.IO).launch {
                    context.cookieDataStore.edit { prefs ->
                        prefs.clear()
                    }
                }
            }
            return response
        }
    }

    private class CsrfInterceptor(private val dataStore: DataStore<Preferences>) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {

            val prefs = runBlocking { dataStore.data.first() }
            val csrfToken = prefs[CookieKeys.CSRF_TOKEN]
            Log.d("csrf interceptor", "csrf: ${csrfToken.toString()}")

            val original = chain.request()
            val builder = original.newBuilder()

            if (csrfToken != null && original.method != "GET") {
                builder.addHeader("X-CSRFToken", csrfToken)
            }

            return chain.proceed(builder.build())
        }
    }
}