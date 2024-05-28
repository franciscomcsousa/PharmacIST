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
import android.widget.Toast
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pt.ulisboa.tecnico.pharmacist.utils.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.databinding.ActivityDrawerBinding
import pt.ulisboa.tecnico.pharmacist.utils.Location
import pt.ulisboa.tecnico.pharmacist.utils.PermissionUtils
import pt.ulisboa.tecnico.pharmacist.utils.Pharmacy
import pt.ulisboa.tecnico.pharmacist.localDatabase.PharmacistAPI
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

    private val PERMISSION_REQUEST_ACCESS_LOCATION_CODE = 1001   // good practice

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

        if (PermissionUtils.requestPermissions(this) && PermissionUtils.requestNotificationPermissions(this)) {
            val channel = NotificationChannel("pharmacies", "Pharmacies", NotificationManager.IMPORTANCE_DEFAULT)
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

                        val notification = NotificationCompat.Builder(applicationContext, pharmacy.id!!)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            else {
                // Permission denied, redirect to login activity
                Toast.makeText(this@NavigationDrawerActivity, "Permission denied", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@NavigationDrawerActivity, LoginActivity::class.java))
            }
        }
    }
}