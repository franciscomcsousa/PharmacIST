package pt.ulisboa.tecnico.pharmacist

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.util.ArrayMap
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import pt.ulisboa.tecnico.pharmacist.databinding.ActivityMapsBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Timer
import java.util.TimerTask


class MapsActivity : FragmentActivity(), OnMapReadyCallback {
    private var mMap: GoogleMap? = null
    private var binding: ActivityMapsBinding? = null
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private var handler: Handler? = null

    private val PERMISSION_REQUEST_ACCESS_LOCATION_CODE = 1001   // good practice
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var dataStore: DataStoreManager

    // for now contacts the localhost server
    private val url = "http://" + "10.0.2.2" + ":" + 5000 + "/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

    private var pharmacies: MutableList<Pharmacy> = mutableListOf()

    // TODO - Later create a cache to store these images
    private var pharmacyImages: ArrayMap<String, Bitmap> = ArrayMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        dataStore = DataStoreManager(this@MapsActivity)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@MapsActivity)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        requestPermissions()

        // Get the first batch of pharmacies from the backend
        getPharmacies()
        // TODO for now it sleeps to wait for the first batch of pharmacies, maybe change later
        Thread.sleep(300)

        // TODO - Not fetch again if in the same location: saves resources
        // Make a call to the backend to get the pharmacies every 10 seconds in a new thread
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                handler = Handler(mainLooper)
                handler!!.post {
                    println("Updating pharmacies")
                    mapPharmacies()
                    println("Pharmacies updated")
                }
            }
        }
        timer!!.schedule(timerTask, 0, 10000)
    }

    private fun mapPharmacies() {
        getPharmacies()
        for (pharmacy in pharmacies) {
            // Add a marker for each pharmacy
            val marker = mMap!!.addMarker(MarkerOptions()
                .position(LatLng(pharmacy.latitude.toDouble(), pharmacy.longitude.toDouble()))
                .title(pharmacy.name)
                .snippet(pharmacy.address)
            )

            marker?.tag = pharmacy // Store pharmacy data as a tag

            mMap!!.setOnMarkerClickListener { clickedMarker ->
                val clickedPharmacy = clickedMarker.tag as Pharmacy
                showPharmacyDrawer(clickedPharmacy)
                true // Return true to indicate that the listener has consumed the event
            }
        }
    }


    // TODO - possibly change it to be a fragment ?
    // Information of pharmacy selected
    // Bottom drawer
    private fun showPharmacyDrawer(pharmacy: Pharmacy) {
        val bottomDrawerView = layoutInflater.inflate(R.layout.pharmacy_drawer_layout, null)
        pharmacyImage(pharmacy.name)

        // Update views with pharmacy information
        // For example:
        bottomDrawerView.findViewById<TextView>(R.id.pharmacy_name)?.text = pharmacy.name
        bottomDrawerView.findViewById<TextView>(R.id.pharmacy_address)?.text = pharmacy.address
        bottomDrawerView.findViewById<ImageView>(R.id.pharmacy_image)?.setImageBitmap(pharmacyImages[pharmacy.name])

        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomDrawerView)
        bottomSheetDialog.show()
    }

    private fun getPharmacies() {

        val pharmaciesFetched: MutableList<Pharmacy> = mutableListOf()
        getUserLocation { location ->
            // sometimes it might not be able to fetch
            // TODO - maybe when null use the last non-null value
            if (location != null) {
                val call: Call<PharmaciesResponse> = retrofitAPI.getPharmacies(location)
                call.enqueue(object : Callback<PharmaciesResponse> {
                    override fun onResponse(call: Call<PharmaciesResponse>, response: Response<PharmaciesResponse>) {
                        if (response.isSuccessful) {
                            val pharmaciesList = response.body()!!.pharmacies
                            for (pharmacy in pharmaciesList) {
                                // transform pharmacies into a list of Pharmacy objects
                                pharmaciesFetched += Pharmacy(pharmacy[1].toString(), pharmacy[2].toString(),
                                    pharmacy[3].toString(), pharmacy[4].toString(), "")
                            }
                            // update the pharmacies list
                            pharmacies = pharmaciesFetched
                            // TODO - this might waste too much resources
                            // Only way to correctly preview image
                            for (pharmacy in pharmacies) {
                                pharmacyImage(pharmacy.name)
                            }
                        }
                    }

                    override fun onFailure(call: Call<PharmaciesResponse>, t: Throwable) {
                        // we get error response from API.
                        Log.d("serverResponse","FAILED: "+ t.message)
                    }
                })
            }
        }
    }

    private fun pharmacyImage(name: String) {

        var b64Image = ""
        val call: Call<PharmacyImageResponse> = retrofitAPI.pharmacyImage(name)
        call.enqueue(object : Callback<PharmacyImageResponse> {
            override fun onResponse(
                call: Call<PharmacyImageResponse>,
                response: Response<PharmacyImageResponse>
            ) {
                if (response.isSuccessful) {
                    b64Image = response.body()!!.image
                    val decodedBytes = Base64.decode(b64Image, Base64.DEFAULT)
                    pharmacyImages[name] = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    Log.d("serverResponse", "Pharmacy image retrieved")
                }
            }

            override fun onFailure(call: Call<PharmacyImageResponse>, t: Throwable) {
                Log.d("serverResponse", "FAILED: " + t.message)
            }
        })
    }

    // User Location and Permissions Logic
    @SuppressLint("MissingPermission")  // IDE does not consider how this function is called
    private fun enableUserLocation() {
        mMap?.isMyLocationEnabled = true

    }

    @SuppressLint("MissingPermission")  // IDE does not consider how this function is called
    private fun getUserLocation(callback: (Location?) -> Unit) {
        val locationTask = fusedLocationProviderClient.lastLocation
        locationTask.addOnSuccessListener { location ->
            // Check if location is not null before using it
            val userLocation = location?.let {
                Location(it.latitude, it.longitude)
            }
            callback(userLocation)
        }
    }
    private fun centerUserLocation() {
        getUserLocation { location ->
            location?.let {
                val latLng = LatLng(location.latitude, location.longitude)
                mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            }
        }
    }

    private fun requestPermissions() {
        // verify permissions
        if (ContextCompat.checkSelfPermission(
                this@MapsActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is granted
            enableUserLocation()
            centerUserLocation()
        } else {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this@MapsActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_ACCESS_LOCATION_CODE
            )
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with your location-related tasks
                enableUserLocation()
                centerUserLocation()
            } else {
                // Permission denied, redirect to NavigationDrawer activity
                Toast.makeText(this@MapsActivity, "Permission denied", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@MapsActivity, NavigationDrawerActivity::class.java))
            }
        }
    }

}