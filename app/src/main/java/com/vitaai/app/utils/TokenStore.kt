package com.vitaai.app.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.dataStore by preferencesDataStore(name = "vitaai_prefs")
private val TOKEN_KEY = stringPreferencesKey("jwt_token")

object TokenStore {

    suspend fun saveToken(context: Context, token: String) {
        context.dataStore.edit { it[TOKEN_KEY] = token }
    }

    suspend fun getToken(context: Context): String? {
        val prefs = context.dataStore.data.first()
        return prefs[TOKEN_KEY]
    }

    suspend fun clearToken(context: Context) {
        context.dataStore.edit { it.remove(TOKEN_KEY) }
    }
}
