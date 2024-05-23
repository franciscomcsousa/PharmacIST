package pt.ulisboa.tecnico.pharmacist.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.ulisboa.tecnico.pharmacist.utils.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.databinding.ActivityDrawerBinding

class NavigationDrawerActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityDrawerBinding
    private lateinit var dataStore: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dataStore = DataStoreManager(this@NavigationDrawerActivity)

        setSupportActionBar(binding.appBarDrawer.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_drawer)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
        ), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        checkThemeMode()

        val modeSwitch = findViewById<SwitchCompat>(R.id.switch_mode)
        modeSwitch.setOnClickListener(View.OnClickListener{
            if (modeSwitch.isChecked) {
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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.drawer, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_drawer)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun mapsButtonClick(view: View?) {
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }

    fun addPharmacyButtonClick(view: View?) {
        val intent = Intent(this, AddPharmacyActivity::class.java)
        startActivity(intent)
    }

    fun medicineButtonClick(view: View?) {
        val intent = Intent(this, MedicineSearchActivity::class.java)
        startActivity(intent)
    }

    private fun checkThemeMode() {
        lifecycleScope.launch {
            val isDarkMode = getAppTheme()
            if (isDarkMode) {
                val modeSwitch = findViewById<SwitchCompat>(R.id.switch_mode)
                modeSwitch.isChecked = true
                modeSwitch.text = "Dark Mode"
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            else {
                val modeSwitch = findViewById<SwitchCompat>(R.id.switch_mode)
                modeSwitch.isChecked = false
                modeSwitch.text = "Light Mode"
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
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
}