package pt.ulisboa.tecnico.pharmacist.activities

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.databinding.ActivityDrawerBinding
import pt.ulisboa.tecnico.pharmacist.localDatabase.PharmacistAPI
import pt.ulisboa.tecnico.pharmacist.utils.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.utils.Location
import pt.ulisboa.tecnico.pharmacist.utils.PermissionUtils
import pt.ulisboa.tecnico.pharmacist.utils.Pharmacy
import pt.ulisboa.tecnico.pharmacist.utils.showSnackbar
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs

class NavigationDrawerActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityDrawerBinding
    private lateinit var dataStore: DataStoreManager
    private var previousLocation: Location? = null
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private var handler: Handler? = null
    private val pharmacistAPI = PharmacistAPI(this)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        dataStore = DataStoreManager(this@NavigationDrawerActivity)

        setSupportActionBar(binding.appBarDrawer.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_drawer)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.nav_home
        ), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        displayUsername(navView)

        navView.menu.findItem(R.id.nav_logout).setOnMenuItemClickListener { menuItem ->
            handleLogoutButtonClick(menuItem)
            true
        }

        if (PermissionUtils.requestLocationAndNotificationPermissions(this)) {
            setupNotificationsAndLocation()
        }
    }

    override fun onResume() {
        super.onResume()
        val navView: NavigationView = binding.navView
        displayUsername(navView)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    private fun displayUsername(navView: NavigationView) {
        lifecycleScope.launch {
            val headerView = navView.getHeaderView(0)
            val headerTitle = headerView.findViewById<TextView>(R.id.nav_header_title)
            val username = dataStore.getUsername().toString()
            if (headerTitle != null) {
                if (!username.equals("")) {
                    headerTitle.text = dataStore.getUsername().toString()
                }
                else {
                    headerTitle.text = getString(R.string.guest_user)
                }
            }
        }
    }

    private fun setupNotificationsAndLocation() {
        val channel =
            NotificationChannel("pharmacies", "Pharmacies", NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                handler = Handler(mainLooper)
                handler!!.post {
                    getNearbyPharmacies()
                }
            }
        }
        timer!!.schedule(timerTask, 0, 5000)
    }


    override fun onDestroy() {
        super.onDestroy()
        timer!!.cancel()
        timer?.purge()
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

    private fun handleLogoutButtonClick(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.nav_logout -> {
                lifecycleScope.launch {
                    dataStore.setLoginToken("")
                    dataStore.setFCMToken("")
                    dataStore.setUsername("")
                }
                Thread.sleep(100)
                onBackPressedDispatcher.onBackPressed()
                runOnUiThread {
                    showSnackbar("Successfully logged out!")
                }
            }
        }
    }

    fun enterSettings(item: MenuItem) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    private fun getNearbyPharmacies() {
        val locationCallback : (Location?) -> Unit = { location ->
            if (location != null  && (previousLocation == null ||
                abs(location.latitude - previousLocation!!.latitude) > 0.0001 ||
                abs(location.longitude - previousLocation!!.longitude) > 0.0001)) {
                previousLocation = location

                val onSuccess : (List<Pharmacy>) -> Unit = { pharmaciesList ->
                    Log.d("serverResponse", "Nearby pharmacies retrieved")
                    for (pharmacy in pharmaciesList) {
                        val intent = Intent(this, MapsActivity::class.java)
                        intent.putExtra("pharmacyId", pharmacy.id)
                        intent.putExtra("pharmacyName", pharmacy.name)
                        intent.putExtra("pharmacyAddress", pharmacy.address)
                        intent.putExtra("pharmacyLatitude", pharmacy.latitude)
                        intent.putExtra("pharmacyLongitude", pharmacy.longitude)
                        val pendingIntent = PendingIntent.getActivity(this, pharmacy.id.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
                        val action = NotificationCompat.Action.Builder(androidx.loader.R.drawable.notification_bg, "Open in Map", pendingIntent).build()

                        val notification = NotificationCompat.Builder(applicationContext, "pharmacies")
                            .setSmallIcon(androidx.loader.R.drawable.notification_bg)
                            .setContentTitle("Nearby pharmacy")
                            .setContentText("You are near ${pharmacy.name}")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)
                            .addAction(action)
                            .setAutoCancel(true)
                            .build()
                        NotificationManagerCompat.from(applicationContext).notify(pharmacy.id.hashCode(), notification)
                    }
                }
                pharmacistAPI.getNearbyPharmacies(location, onSuccess)
            }
        }
        PermissionUtils.getUserLocation(locationCallback, this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionUtils.PERMISSION_REQUEST_ACCESS_LOCATION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Location permission granted
                    if (PermissionUtils.requestNotificationPermissions(this)) {
                        // Request notification permission if location permission is granted
                        setupNotificationsAndLocation()
                    }
                } else {
                    // Location permission denied
                    runOnUiThread {
                        showSnackbar("Permission for accessing location is required!")
                    }
                }
            }
            PermissionUtils.PERMISSION_REQUEST_ACCESS_NOTIFICATION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Notification permission granted
                    setupNotificationsAndLocation()
                } else {
                    // Notification permission denied
                    //Toast.makeText(this, "Notification permission is required!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}