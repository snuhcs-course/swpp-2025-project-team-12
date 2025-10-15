package com.example.dailyinsight.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.Protocol


object RetrofitClient {

    // 에뮬레이터에서 로컬 서버 접근
    private const val BASE_URL = "http://10.0.2.2:8000"

    // 필요 시 모킹 모드 토글
    private const val MOCK_MODE = false

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 간단 모킹용 (MOCK_MODE=true면 /api/* 요청을 로컬 JSON으로 대체)
    private val mockInterceptor = Interceptor { chain ->
        val req = chain.request()
        if (!MOCK_MODE) return@Interceptor chain.proceed(req)

        // 엔드포인트별 간단 분기 (필요시 assets 읽도록 확장)
        val path = req.url.encodedPath
        val body = when {
            path.contains("/api/recommendations/today") ->
                """{"status":"ok","data":[{"code":"005930","name":"삼성전자","price":1415000,"change":-10000,"changeRate":-0.29,"time":"09:30"}]}"""
            path.contains("/api/recommendations/history") ->
                """{"status":"ok","data":{"2025-10-14":[{"code":"000660","name":"SK하이닉스","price":1234000,"change":12000,"changeRate":0.98,"time":"09:30"}]}}"""
            path.contains("/api/indices/main") ->
                """{"status":"ok","data":[{"code":"KOSPI","name":"코스피","price":2564.12,"change":-10.32,"changeRate":-0.40,"time":"09:30"}]}"""
            else ->
                """{"status":"ok","data":null}"""
        }
        val mediaType = "application/json".toMediaType()
        val respBody = body.toResponseBody(mediaType)

        val resp = Response.Builder()
            .request(req)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK(Mock)")
            .body(respBody)
            .addHeader("content-type", "application/json")
            .build()

        resp

    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(mockInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
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
}