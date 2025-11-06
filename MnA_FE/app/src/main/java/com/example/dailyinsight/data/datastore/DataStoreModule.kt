package com.example.dailyinsight.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// Single tone - shared within the app (global)
val Context.cookieDataStore by preferencesDataStore(name = "cookies")

object CookieKeys {
    val CSRF_TOKEN = stringPreferencesKey("crsf_token")
    val ACCESS_TOKEN = stringPreferencesKey("access_token")
    val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
}

//val EXAMPLE_COUNTER = intPreferencesKey("example_counter")
//val exampleCounterFlow: Flow<Int> =
//    context.dataStore.data.map { preferences ->
//        // No type safety.
//        preferences[EXAMPLE_COUNTER] ?: 0
//    }