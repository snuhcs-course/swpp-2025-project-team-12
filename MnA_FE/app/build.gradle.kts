plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.dailyinsight"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.dailyinsight"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Activity / Fragment KTX (for by viewModels)
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("androidx.fragment:fragment-ktx:1.6.1")
    // Retrofit & Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.9.1")

    // data store for cookies
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    implementation("androidx.tracing:tracing:1.3.0")
    // test
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation ("androidx.test:core:1.5.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.7.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")

    // Unit testing dependencies
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // Fragment testing
    debugImplementation("androidx.fragment:fragment-testing:1.6.1")
    androidTestImplementation("androidx.fragment:fragment-testing:1.6.1")

    // Navigation testing
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.7")

    // Mockito for Android testing
    androidTestImplementation("org.mockito:mockito-android:5.5.0")
    androidTestImplementation("org.mockito:mockito-core:5.5.0")

    // Material Design 3
    implementation("com.google.android.material:material:1.13.0")

    // Retrofit & OkHttp (Refer to Week 2 Tutorial slides)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // MPAndroidChart For Implementing Graphs
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.google.android.material:material:1.13.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // Retrofit + Kotlin serialization
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // OkHttp (로그용 선택)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    // SwipeRefresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // RecyclerView DiffUtil
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0") // 그래프

    //  Room Database
    val room_version = "2.8.3" // 1. 'def'를 'val'로 변경
    // 1. Room 런타임
    implementation("androidx.room:room-runtime:$room_version")
    // 2. Room 컴파일러 (kapt로 추가)
    kapt("androidx.room:room-compiler:$room_version")
    // 3. Kotlin 코루틴 및 Flow 지원 (필수)
    implementation("androidx.room:room-ktx:$room_version")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
}