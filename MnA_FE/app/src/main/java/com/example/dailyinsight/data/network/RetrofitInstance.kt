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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection

/**
 * Unified Retrofit Instance for all API calls
 * Base URL: http://10.0.2.2:8000/ (for Android emulator)
 */
object RetrofitInstance {
    private const val BASE_URL = "http://ec2-3-34-197-82.ap-northeast-2.compute.amazonaws.com:8000/"
    // private const val BASE_URL = "http://10.0.2.2:8000/"

    // Toggle: true = today/history network calls return mock responses
    private const val MOCK_MODE = true

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private lateinit var client : OkHttpClient
    lateinit var api : ApiService

    fun init(context: Context) {
        client = OkHttpClient.Builder()
            .cookieJar(MyCookieJar(context.applicationContext))
            .apply {
                if (MOCK_MODE) addInterceptor(MockInterceptor())
                addInterceptor(logging)
            }
            .build()

        api = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
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

            // 1) Stock detail mocking
            val stockRegex = Regex("^/api/stocks/[^/]+$")
            if (req.method == "GET" && stockRegex.matches(path)) {
                val body = STOCK_DETAIL_JSON.toResponseBody(contentType)
                return Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(HttpURLConnection.HTTP_OK)
                    .message("OK")
                    .body(body)
                    .build()
            }

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

    private val STOCK_DETAIL_JSON = """
    {
  "date": "2025-11-13",
  "ticker": "000660",
  "name": "SK 하이닉스",
  "price": 579000,
  "change": -7000,
  "change_rate": -1.19,

  "financials": [
    {
      "period": "2025 (F)",
      "market_cap": null,
      "BPS": "138231.0",
      "PER": "6.1",
      "PBR": "1.4",
      "EPS": "39634.0",
      "ROE": "0.331",
      "DPS": "1830.0",
      "DIV": "0.30"
    },
    {
      "period": "2024",
      "market_cap": "421558000000000",
      "BPS": "96708.0",
      "PER": "7.0",
      "PBR": "1.8",
      "EPS": "27182.0",
      "ROE": "0.268",
      "DPS": "2204.0",
      "DIV": "0.36"
    },
    {
      "period": "2023",
      "market_cap": "99000000000000",
      "BPS": "85500.0",
      "PER": null,
      "PBR": "1.9",
      "EPS": "-12551.0",
      "ROE": "-0.156",
      "DPS": "1200.0",
      "DIV": "0.85"
    }
  ],

  "chart": [
    { "t": 1605225600000, "v": 95000.0 },
    { "t": 1613174400000, "v": 130000.0 },
    { "t": 1621036800000, "v": 120000.0 },
    { "t": 1628812800000, "v": 105000.0 },
    { "t": 1636761600000, "v": 110000.0 },
    { "t": 1644624000000, "v": 130000.0 },
    { "t": 1652313600000, "v": 115000.0 },
    { "t": 1660176000000, "v": 98000.0 },
    { "t": 1668297600000, "v": 89000.0 },
    { "t": 1676246400000, "v": 92000.0 },
    { "t": 1684022400000, "v": 90000.0 },
    { "t": 1691884800000, "v": 118000.0 },
    { "t": 1700006400000, "v": 130000.0 },
    { "t": 1704067200000, "v": 141000.0 },
    { "t": 1706745600000, "v": 135000.0 },
    { "t": 1709251200000, "v": 150000.0 },
    { "t": 1711929600000, "v": 180000.0 },
    { "t": 1714521600000, "v": 175000.0 },
    { "t": 1717199900000, "v": 190000.0 },
    { "t": 1719878300000, "v": 210000.0 },
    { "t": 1723507200000, "v": 450000.0 },
    { "t": 1728796800000, "v": 520000.0 },
    { "t": 1730851200000, "v": 586000.0 },
    { "t": 1731110400000, "v": 581000.0 },
    { "t": 1731456000000, "v": 579000.0 }
  ]
}
    """.trimIndent()
}