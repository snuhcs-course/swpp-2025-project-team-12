package com.example.dailyinsight.data.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.Protocol
import okhttp3.JavaNetCookieJar
import android.util.Log
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import com.example.dailyinsight.data.datastore.cookieDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Proxy
import java.util.concurrent.Executors

/**
 * Unified Retrofit Instance for all API calls
 * Base URL: http://10.0.2.2:8000/ (for Android emulator)
 */
object RetrofitInstance {
    private const val BASE_URL = "http://ec2-13-124-209-234.ap-northeast-2.compute.amazonaws.com:8000/"
    // private const val BASE_URL = "http://10.0.2.2:8000/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private lateinit var client : OkHttpClient
    lateinit var api : ApiService

    lateinit var cookieJar: ProxyCookieJar
        private set

    fun init(context: Context) {
        val realCookieJar = MyCookieJar(context.applicationContext)

        cookieJar = ProxyCookieJar(
            real = realCookieJar,
            dataStore = context.cookieDataStore
        )

        client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(AuthInterceptor(context.applicationContext))
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
                // runBlocking 제거, IO 스레드에서 비동기 처리 (ANR 방지)
                CoroutineScope(Dispatchers.IO).launch {
                    context.cookieDataStore.edit { prefs ->
                        prefs.clear()
                    }
                }
            }
            return response
        }
    }


//    val cookieManager = CookieManager()

//    private val client by lazy {
////        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
//        OkHttpClient.Builder()
////            .cookieJar(MyCookieJar())
////            .cookieJar(JavaNetCookieJar(cookieManager))
////            .addInterceptor { chain ->
////                val original = chain.request()
////
////                val cookies = cookieManager.cookieStore.cookies
////                    .filter { original.url.host.endsWith(it.domain.trimStart('.')) }
////                    .joinToString("; ") { "${it.name}=${it.value}" }
////
////                val csrfToken = cookieManager.cookieStore.cookies
////                    .firstOrNull { it.name == "csrftoken" }?.value
////
////                val requestWithCookie = original.newBuilder()
////                    .header("Cookie", cookies)
////                    .apply {
////                        if (csrfToken != null) header("X-CSRFToken", csrfToken)
////                    }
////                    .build()
////
////                chain.proceed(requestWithCookie)
////            }
//            .apply {
//                if (MOCK_MODE) addInterceptor(MockInterceptor())
//                addInterceptor(logging)
//            }
//            .build()
//    }
//
//    val api: ApiService by lazy {
//        Retrofit.Builder()
//            .baseUrl(BASE_URL)
//            .client(client)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(ApiService::class.java)
//    }

}