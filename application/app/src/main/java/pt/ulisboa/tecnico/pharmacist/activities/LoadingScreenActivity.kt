package pt.ulisboa.tecnico.pharmacist.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.ulisboa.tecnico.pharmacist.utils.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.R

class LoadingScreenActivity: AppCompatActivity() {

    private lateinit var dataStore: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        dataStore = DataStoreManager(this@LoadingScreenActivity)
        checkThemeMode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading_screen)
        enableEdgeToEdge()

        Handler().postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }, 1000)
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
}