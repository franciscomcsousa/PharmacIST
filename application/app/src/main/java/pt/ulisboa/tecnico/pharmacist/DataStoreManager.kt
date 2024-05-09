package pt.ulisboa.tecnico.pharmacist

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(val context: Context) {

    companion object {
        val KEY_TOKEN = stringPreferencesKey("token")
    }

    suspend fun setToken(token: String) {
        context.dataStore.edit { settings ->
            settings[KEY_TOKEN] = token
        }
    }

    suspend fun getToken(): String? {
        val values = context.dataStore.data.first()
        return values[KEY_TOKEN]
    }

    suspend fun clearToken() {
        context.dataStore.edit { settings ->
            settings.remove(KEY_TOKEN)
        }
    }
}