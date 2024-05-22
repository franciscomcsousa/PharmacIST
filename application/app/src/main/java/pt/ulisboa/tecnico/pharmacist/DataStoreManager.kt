package pt.ulisboa.tecnico.pharmacist

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(val context: Context) {

    companion object {
        val KEY_TOKEN = stringPreferencesKey("token")
        val USERNAME = stringPreferencesKey("username")
        val DARKMODE = booleanPreferencesKey("dark_mode")

        fun getUrl(): String {
            // for remote testing
            return "http://172.232.42.26/"
            // for local testing
            //return "http://10.0.2.2:5000"
        }
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

    suspend fun setUsername(username: String) {
        context.dataStore.edit { settings ->
            settings[USERNAME] = username
        }
    }

    suspend fun getUsername(): String? {
        val values = context.dataStore.data.first()
        return values[USERNAME]
    }

    suspend fun setTheme(isDarkMode: Boolean) {
        context.dataStore.edit { settings ->
            settings[DARKMODE] = isDarkMode
        }
    }

    suspend fun getTheme(): Boolean {
        val values = context.dataStore.data.first()
        return values[DARKMODE] ?: false
    }
}