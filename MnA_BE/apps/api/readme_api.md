# Daily Insight Android API **Quickstart** (Kotlin/Retrofit)

---

## 1) 서버 실행
```bash
python manage.py runserver 8000
```
- 에뮬레이터: **BASE_URL = http://10.0.2.2:8000/api/**
- 실기기: **BASE_URL = http://<노트북IP>:8000/api/**

**AndroidManifest.xml** (HTTP 사용 시)
```xml
<application android:usesCleartextTraffic="true" />
```

---

## 2) Gradle 의존성
```kotlin
// app/build.gradle
dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
}
```

---

## 3) DTO (응답 스키마)
```kotlin
// data/model/ApiModels.kt
package com.example.myapplication.data.model

data class ApiResponse<T>(
    val items: List<T>? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val asOf: String? = null,
    val source: String? = null,
    val marketDate: String? = null,
    val personalized: Boolean? = null
)

data class HealthResponse(
    val api: String? = null,
    val s3: Any? = null,
    val db: Any? = null,
    val asOf: String? = null
)

data class Recommendation(
    val ticker: String,
    val name: String,
    val news: List<String> = emptyList(),
    val reason: List<String> = emptyList(),
    val rank: Int? = null
)
```

---

## 4) Retrofit API
```kotlin
// data/api/MnaApiService.kt
package com.example.myapplication.data.api

import com.example.myapplication.data.model.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MnaApiService {
    @GET("health")
    suspend fun getHealth(): Response<HealthResponse>

    @GET("recommendations/general")
    suspend fun getRecommendations(
        @Query("risk") risk: String,
        @Query("date") date: String? = null,
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<Recommendation>>
}
```

---

## 5) Retrofit 클라이언트
```kotlin
// data/api/RetrofitClient.kt
package com.example.myapplication.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8000/api/" // 실기기는 로컬 IP로 교체

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val api: MnaApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(MnaApiService::class.java)
}
```

---

## 6) 호출 예시 (Activity)
```kotlin
// MainActivity.kt (발췌)
lifecycleScope.launch {
    val res = RetrofitClient.api.getRecommendations(risk = "공격투자형")
    val body = res.body()
    val first = body?.items?.firstOrNull()
    textView.text = buildString {
        appendLine("source=${body?.source}, date=${body?.marketDate}, total=${body?.total}")
        if (first != null) {
            appendLine("${first.rank}. ${first.ticker} ${first.name}")
            appendLine("뉴스: ${first.news.joinToString()}")
            appendLine("이유: ${first.reason.joinToString()}")
        }
    }
}
```

---

## 7) cURL 확인
```bash
curl -G 'http://127.0.0.1:8000/api/recommendations/general' \
  --data-urlencode 'risk=공격투자형'
```

---

## 8) 문제 해결 간단 체크
- 404 → BASE_URL이 `/api/`로 끝나는지 확인
- Timeout → 서버 실행/네트워크/포트 8000 확인
- 실기기 → `10.0.2.2` 대신 노트북 IP
- HTTP 차단 → `usesCleartextTraffic="true"`
