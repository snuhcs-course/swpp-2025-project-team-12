package com.example.dailyinsight.data.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.Protocol
import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection

/**
 * Unified Retrofit Instance for all API calls
 * Base URL: http://10.0.2.2:8000/ (for Android emulator)
 */
object RetrofitInstance {
    private const val BASE_URL = "http://ec2-3-34-197-82.ap-northeast-2.compute.amazonaws.com:8000/"

    // Toggle: true = today/history network calls return mock responses
    private const val MOCK_MODE = true

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val client by lazy {
        OkHttpClient.Builder()
            .apply {
                if (MOCK_MODE) addInterceptor(MockInterceptor())
                addInterceptor(logging)
            }
            .build()
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }


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
      "success": true,
      "data": {
        "ticker": "005930",
        "name": "삼성전자",
        "price": 74800,
        "change": 1200,
        "change_rate": 1.63,
        "market_cap": "444,000,000,000,000",
        "shares_outstanding": "5,969,782,550",

        "price_financial_info": {
          "price": {
            "2025-04-21": 70100,
            "2025-05-21": 71600,
            "2025-06-21": 72400,
            "2025-07-21": 73900,
            "2025-08-21": 74200,
            "2025-09-21": 74600,
            "2025-10-17": 74800
          }
        },

        "valuation": {
          "pe_annual": "21.3",
          "pe_ttm": "19.8",
          "forward_pe": "17.6",
          "ps_ttm": "2.1",
          "pb": "1.7",
          "pcf_ttm": "12.4",
          "pfcf_ttm": "16.8"
        },
        "solvency": {
          "current_ratio": "2.05",
          "quick_ratio": "1.67",
          "de_ratio": "0.32"
        },
        "dividend": {
          "payout_ratio": "35.1",
          "yield": "1.9",
          "latest_exdate": "2025-03-27"
        },

        "net_income": {
          "annual": [
            {"period": "2024", "value": "17,531.000 B"},
            {"period": "2023", "value": "15,210.000 B"},
            {"period": "2022", "value": "30,215.000 B"},
            {"period": "2021", "value": "39,240.000 B"},
            {"period": "TTM (Q3)", "value": "18,900.000 B"}
          ],
          "quarter": [
            {"period": "2025 Q2", "value": "4.630 B"},
            {"period": "2025 Q1", "value": "4.210 B"},
            {"period": "2024 Q4", "value": "4.000 B"},
            {"period": "2024 Q3", "value": "3.900 B"}
          ]
        }
      }
    }
    """.trimIndent()
}