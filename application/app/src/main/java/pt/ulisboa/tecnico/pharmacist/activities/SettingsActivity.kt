package pt.ulisboa.tecnico.pharmacist.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.localDatabase.DatabaseHandler
import pt.ulisboa.tecnico.pharmacist.utils.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.utils.ImageUtils

class SettingsActivity : AppCompatActivity() {

    private lateinit var dataStore: DataStoreManager

    private lateinit var dbHandler: DatabaseHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        dataStore = DataStoreManager(this@SettingsActivity)
        dbHandler = DatabaseHandler(this)

        checkThemeMode()
        checkDataMode()

        val darkModeSwitch = findViewById<SwitchCompat>(R.id.switch_dark_mode)
        val dataModeSwitch = findViewById<SwitchCompat>(R.id.switch_data_mode)
        darkModeSwitch.setOnClickListener(View.OnClickListener{
            if (darkModeSwitch.isChecked) {
                setAppTheme(true)
                // this is to make sure the datastore is updated before the activity restarts
                Thread.sleep(100)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                setAppTheme(false)
                // this is to make sure the datastore is updated before the activity restarts
                Thread.sleep(100)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        })

        dataModeSwitch.setOnClickListener(View.OnClickListener {
            if (dataModeSwitch.isChecked) {
                setDataTheme(true)
            }
            else {
                setDataTheme(false)
            }
        })

        findViewById<View>(R.id.clear_cache_button).setOnClickListener {
            confirmCacheDeletion()
        }
    }

    private fun confirmCacheDeletion() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.clear_cache))
        builder.setMessage(getString(R.string.are_you_sure))

        builder.setPositiveButton(getString(R.string.yes)) { dialog, _ ->
            val db = dbHandler.writableDatabase
            dbHandler.dropAllTables(db)
            dbHandler.onCreate(db)
            ImageUtils.deleteAllImagesFromInternalStorage(this)
            dialog.dismiss()
        }

        builder.setNegativeButton(getString(R.string.no)) { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun checkThemeMode() {
        lifecycleScope.launch {
            val isDarkMode = getAppTheme()
            if (isDarkMode) {
                val modeSwitch = findViewById<SwitchCompat>(R.id.switch_dark_mode)
                modeSwitch.isChecked = true
                modeSwitch.text = getString(R.string.dark_mode)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            else {
                val modeSwitch = findViewById<SwitchCompat>(R.id.switch_dark_mode)
                modeSwitch.isChecked = false
                modeSwitch.text = getString(R.string.light_mode)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    private fun checkDataMode() {
        lifecycleScope.launch {
            val isDataMode = getDataMode()
            if (isDataMode) {
                val modeSwitch = findViewById<SwitchCompat>(R.id.switch_data_mode)
                modeSwitch.isChecked = true
            }
            else {
                val modeSwitch = findViewById<SwitchCompat>(R.id.switch_data_mode)
                modeSwitch.isChecked = false
            }
        }
    }

    private suspend fun getAppTheme(): Boolean {
        return dataStore.getTheme()
    }

    private fun setAppTheme(theme: Boolean) {
        lifecycleScope.launch {
            dataStore.setTheme(theme)
        }
    }

    private suspend fun getDataMode(): Boolean {
        return dataStore.getDataMode()
    }

    private fun setDataTheme(dataMode: Boolean) {
        lifecycleScope.launch {
            dataStore.setDataMode(dataMode)
        }
    }

}