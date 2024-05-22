package pt.ulisboa.tecnico.pharmacist

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.ArrayMap
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import pt.ulisboa.tecnico.pharmacist.databinding.ActivityMapsBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mMap: GoogleMap? = null
    private var binding: ActivityMapsBinding? = null
    private var previousLocation: Location? = null
    private var needNewMarkers = false
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private var handler: Handler? = null

    private val PERMISSION_REQUEST_ACCESS_LOCATION_CODE = 1001   // good practice
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var dataStore: DataStoreManager

    private val retrofit = Retrofit.Builder()
        .baseUrl(DataStoreManager.getUrl())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

    private lateinit var autocompleteFragment: AutocompleteSupportFragment
    private var selectedAddress: Place? = null

    private var pharmacies: MutableList<Pharmacy> = mutableListOf()

    private var pharmacyFavorites: ArrayMap<String, Boolean> = ArrayMap()

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

        startPlacesAPI()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        requestPermissions()

        // If the intent was started by the ClosestMedicineActivity, show the pharmacy only
        if (intent.hasExtra("pharmacyLatitude") && intent.hasExtra("pharmacyLongitude")) {
            val pharmacyId = intent.getStringExtra("pharmacyId")!!
            val pharmacyName = intent.getStringExtra("pharmacyName")!!
            val pharmacyAddress = intent.getStringExtra("pharmacyAddress")!!
            val pharmacyLatitude = intent.getStringExtra("pharmacyLatitude")!!.toDouble()
            val pharmacyLongitude = intent.getStringExtra("pharmacyLongitude")!!.toDouble()

            val marker = mMap?.addMarker(MarkerOptions()
                .position(LatLng(pharmacyLatitude, pharmacyLongitude))
                .title(pharmacyName)
                .snippet(pharmacyAddress)
            )

            pharmacyImage(pharmacyName)
            marker?.tag = Pharmacy(pharmacyId, pharmacyName, pharmacyAddress, pharmacyLatitude.toString(), pharmacyLongitude.toString(), "")

            mMap!!.setOnMarkerClickListener { clickedMarker ->
                val clickedPharmacy = clickedMarker.tag as Pharmacy
                showPharmacyDrawer(clickedPharmacy)
                true
            }

            return
        }

        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                handler = Handler(mainLooper)
                handler!!.post {
                    mapPharmacies()
                }
            }
        }
        timer!!.schedule(timerTask, 0, 2000)
    }

    private fun mapPharmacies() {
        // Fetch pharmacies from the server
        getPharmacies()

        // Only add markers if new pharmacies were fetched
        if (needNewMarkers) {
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
            needNewMarkers = false
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

        val favoriteButton = bottomDrawerView.findViewById<ToggleButton>(R.id.favorite_btn)
        lifecycleScope.launch {
            isPharmacyFavorite(pharmacy.id.toString(), favoriteButton)
        }

        val directionButton = bottomDrawerView.findViewById<Button>(R.id.btn_navigate)

        // Set click listener for the toggle button
        favoriteButton.setOnClickListener {
            // sends updated information to backend
            lifecycleScope.launch {
                handleFavoriteButton(pharmacy.id.toString())
            }
        }

        // Show More (Pharmacy Information Panel) button
        val informationButton = bottomDrawerView.findViewById<Button>(R.id.show_pharmacy_info)
        informationButton.setOnClickListener {
            lifecycleScope.launch {
                handleShowMore(pharmacy)
            }
        }

        // Go to maps
        directionButton.setOnClickListener {
            getUserLocation {userLocation ->
                val source = "${userLocation?.latitude},${userLocation?.longitude}"
                val uri = "https://www.google.com/maps/dir/?api=1&origin=$source&destination=" + pharmacy.address
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                intent.setPackage("com.google.android.apps.maps")
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            }
        }

        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomDrawerView)
        bottomSheetDialog.show()
    }

    private suspend fun isPharmacyFavorite(id: String, favoriteButton: ToggleButton) {
        val username = getUsername()
        val favoritePharmacy = FavoritePharmacy(username,id)
        val call: Call<StatusResponse> = retrofitAPI.isPharmacyFavorite(favoritePharmacy)
        call.enqueue(object : Callback<StatusResponse> {
            override fun onResponse(
                call: Call<StatusResponse>,
                response: Response<StatusResponse>
            ) {
                if (response.isSuccessful) {
                    Log.d("serverResponse", "SUCCESSFUL: ${response.code()}")
                    favoriteButton.isChecked = response.code() == 203
                }
            }

            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                Log.d("serverResponse", "FAILED: ${t.message}")
            }
        })
    }

    private fun handleShowMore(pharmacy: Pharmacy) {
        val intent = Intent(this, PharmacyInformationActivity::class.java)

        intent.putExtra("pharmacyId", pharmacy.id)
        intent.putExtra("pharmacyName", pharmacy.name)
        startActivity(intent)
    }

    private fun getPharmacies() {
        getUserLocation { location ->
            // sometimes it might not be able to fetch
            // TODO - maybe when null use the last non-null value
            // Fetch pharmacies only if the location has changed significantly
            if (location != null  && (previousLocation == null ||
                abs(location.latitude - previousLocation!!.latitude) > 0.0001 ||
                abs(location.longitude - previousLocation!!.longitude) > 0.0001)) {
                val pharmaciesFetched: MutableList<Pharmacy> = mutableListOf()
                val call: Call<PharmaciesResponse> = retrofitAPI.getPharmacies(location)
                call.enqueue(object : Callback<PharmaciesResponse> {
                    override fun onResponse(call: Call<PharmaciesResponse>, response: Response<PharmaciesResponse>) {
                        if (response.isSuccessful) {
                            val pharmaciesList = response.body()!!.pharmacies
                            for (pharmacy in pharmaciesList) {
                                // transform pharmacies into a list of Pharmacy objects
                                pharmaciesFetched += Pharmacy(pharmacy[0].toString() ,pharmacy[1].toString(), pharmacy[2].toString(),
                                    pharmacy[3].toString(), pharmacy[4].toString(), "")
                            }
                            // update the pharmacies list
                            pharmacies = pharmaciesFetched
                            Log.d("serverResponse", "Pharmacies retrieved")

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
                previousLocation = location
                needNewMarkers = true
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

    private fun startPlacesAPI() {
        val apiKey = BuildConfig.MAPS_API_KEY
        Places.initialize(applicationContext, apiKey)

        autocompleteFragment = supportFragmentManager.findFragmentById(R.id.places_autocomplete)
                as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ADDRESS, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener {

            override fun onError(status: Status) {
                Log.d("placesError", "An error occurred: $status")
            }

            override fun onPlaceSelected(place: Place) {
                place.address?.let { Log.d("serverResponse", "This was the address selected!: $it") }
                autocompleteFragment.setHint(place.address)
                selectedAddress = place
                mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(place.latLng!!, 16f))
            }
        })
    }

    private suspend fun handleFavoriteButton(id: String) {
        val username = getUsername()
        val favoritePharmacy = FavoritePharmacy(username,id)
        val call: Call<StatusResponse> = retrofitAPI.pharmacyFavorite(favoritePharmacy)
        call.enqueue(object : Callback<StatusResponse> {
            override fun onResponse(
                call: Call<StatusResponse>,
                response: Response<StatusResponse>
            ) {
                // TODO - may not be necessary, delete
                if (response.isSuccessful) {
                    Log.d("serverResponse", "UPDATED: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                Log.d("serverResponse", "FAILED: ${t.message}")
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

    private suspend fun getUsername(): String {
        // returns "null" string if token is null
        return dataStore.getUsername().toString()
    }

}
