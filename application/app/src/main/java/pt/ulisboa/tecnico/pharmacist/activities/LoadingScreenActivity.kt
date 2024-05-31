package pt.ulisboa.tecnico.pharmacist.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.utils.DataStoreManager

class LoadingScreenActivity: AppCompatActivity() {

    private lateinit var dataStore: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        dataStore = DataStoreManager(this@LoadingScreenActivity)
        checkThemeMode()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading_screen)

        // Since this will be the launcher activity
        // Initialize Firebase
        FirebaseMessaging.getInstance().isAutoInitEnabled = true;

        Handler().postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }, 1000)

        setDeviceId()
    }

    private fun checkThemeMode() {
        lifecycleScope.launch {
            val isDarkMode = getAppTheme()
            if (isDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    private suspend fun getAppTheme(): Boolean {
        return dataStore.getTheme()
    }

    // sets deviceId if not yet defined
    private fun setDeviceId() {
        lifecycleScope.launch {
            if (dataStore.getDeviceId().isEmpty())
                dataStore.setDeviceId()
        }
    }
}