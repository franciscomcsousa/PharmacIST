package pt.ulisboa.tecnico.pharmacist.utils

import android.content.Context
import android.provider.Settings
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
        val KEY_LOGIN_TOKEN = stringPreferencesKey("login_token")
        val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token")
        val USERNAME = stringPreferencesKey("username")
        val GUESTNAME = stringPreferencesKey("guestname")
        val DARKMODE = booleanPreferencesKey("dark_mode")
        val DEVICE_ID = stringPreferencesKey("device_id")

        fun getUrl(): String {
            // for remote testing
            // https ensures TLS
            return "https://pharmacist.francisco-sousa.pt/"
            // for local testing
            //return "http://10.0.2.2:5000"
        }
    }

    suspend fun setLoginToken(token: String) {
        context.dataStore.edit { settings ->
            settings[KEY_LOGIN_TOKEN] = token
        }
    }

    suspend fun getLoginToken(): String? {
        val values = context.dataStore.data.first()
        return values[KEY_LOGIN_TOKEN]
    }

    suspend fun setFCMToken(token: String) {
        context.dataStore.edit { settings ->
            settings[KEY_FCM_TOKEN] = token
        }
    }

    suspend fun getFCMToken(): String? {
        val values = context.dataStore.data.first()
        return values[KEY_FCM_TOKEN]
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

    suspend fun setGuestName(guestname: String) {
        context.dataStore.edit { settings ->
            settings[GUESTNAME] = guestname
        }
    }

    suspend fun getGuestName(): String? {
        val values = context.dataStore.data.first()
        return values[GUESTNAME]
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

    suspend fun setDeviceId() {
        context.dataStore.edit { settings ->
            settings[DEVICE_ID] = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }
    }
    suspend fun getDeviceId(): String {
        val values = context.dataStore.data.first()
        return values[DEVICE_ID] ?: ""
    }
}