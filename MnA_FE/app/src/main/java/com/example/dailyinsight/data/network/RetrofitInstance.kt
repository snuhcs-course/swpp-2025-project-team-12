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
import java.net.Proxy

/**
 * Unified Retrofit Instance for all API calls
 * Base URL: http://10.0.2.2:8000/ (for Android emulator)
 */
object RetrofitInstance {
    private const val BASE_URL = "http://ec2-13-124-209-234.ap-northeast-2.compute.amazonaws.com:8000/"
    // private const val BASE_URL = "http://10.0.2.2:8000/"

    // Toggle: true = 1st tab network calls return mock responses
    private const val MOCK_MODE = true

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
//            .addInterceptor(AuthInterceptor(context.applicationContext))
//            .apply {
//                if (MOCK_MODE) addInterceptor(MockInterceptor())
//                addInterceptor(logging)
//            }
            .addInterceptor(logging)
            .build()

        api = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private class AuthInterceptor(private val context: Context) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())

            if (response.code == 401) {
                runBlocking {
                    context.cookieDataStore.edit { it.clear() }
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


    private val contentType = "application/json".toMediaType()


    // Simple mocking interceptor (MOCK_MODE=true replaces /api/* requests with local JSON)
    private class MockInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val req = chain.request()
            val path = req.url.encodedPath
            Log.d("MockInterceptor", "MOCK ${req.method} $path")

            // 2) Today's recommendations
            if (req.method == "GET" && path == "/api/recommendations/today") {
                val body = TODAY_JSON.toResponseBody(contentType)
                return Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(HttpURLConnection.HTTP_OK)
                    .message("OK")
                    .body(body)
                    .build()
            }

            // 3) Health check
            if (req.method == "GET" && path == "/health") {
                val body = HEALTH_JSON.toResponseBody(contentType)
                return Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(HttpURLConnection.HTTP_OK)
                    .message("OK")
                    .body(body)
                    .build()
            }

            // Otherwise, proceed with actual network call
            return chain.proceed(req)
        }
    }

    // Sample JSON responses (ApiResponse<T> format)
    private val HEALTH_JSON = """
    { "success": true, "data": { "status": "ok" } }
    """.trimIndent()

    private val TODAY_JSON = """
    {
      "success": true,
      "data": [
        {
          "stockInfo": { "ticker": "005930", "company_name": "삼성전자", "market_type": "KOSPI" },
          "reason": "반도체 업황 개선 기대 재부각",
          "links": ["https://news.example.com/1"],
          "date": "2025-10-19T09:30:00+09:00",
          "price": 74800,
          "change": 1200,
          "change_rate": 1.63
        }
      ]
    }
    """.trimIndent()
}