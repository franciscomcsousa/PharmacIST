package pt.ulisboa.tecnico.pharmacist.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.ArrayMap
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import pt.ulisboa.tecnico.pharmacist.BuildConfig
import pt.ulisboa.tecnico.pharmacist.utils.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.utils.FavoritePharmacy
import pt.ulisboa.tecnico.pharmacist.utils.Location
import pt.ulisboa.tecnico.pharmacist.utils.LocationUtils
import pt.ulisboa.tecnico.pharmacist.utils.Pharmacy
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.localDatabase.PharmacistAPI
import pt.ulisboa.tecnico.pharmacist.utils.StatusResponse
import pt.ulisboa.tecnico.pharmacist.databinding.ActivityMapsBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mMap: GoogleMap? = null
    private var binding: ActivityMapsBinding? = null
    private var previousLocation: Location? = null
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private var handler: Handler? = null

    private val PERMISSION_REQUEST_ACCESS_LOCATION_CODE = 1001   // good practice
    private lateinit var dataStore: DataStoreManager

    private val pharmacistAPI = PharmacistAPI(this)

    private lateinit var autocompleteFragment: AutocompleteSupportFragment
    private var selectedAddress: Place? = null

    private var pharmacies: MutableList<Pharmacy> = mutableListOf()

    private var pharmaciesFavorite: MutableList<Pharmacy> = mutableListOf<Pharmacy>()

    private var pharmacyImages: ArrayMap<String, Bitmap> = ArrayMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        dataStore = DataStoreManager(this@MapsActivity)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        startPlacesAPI()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (LocationUtils.requestPermissions(this)) {
            enableUserLocation()
            centerUserLocation()
        }

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

            pharmacyImage(pharmacyId)
            marker?.tag = Pharmacy(pharmacyId, pharmacyName, pharmacyAddress, pharmacyLatitude.toString(), pharmacyLongitude.toString(), "")

            mMap!!.setOnMarkerClickListener { clickedMarker ->
                val clickedPharmacy = clickedMarker.tag as Pharmacy
                showPharmacyDrawer(clickedPharmacy)
                true
            }

            pharmacies = mutableListOf(Pharmacy(pharmacyId, pharmacyName, pharmacyAddress, pharmacyLatitude.toString(), pharmacyLongitude.toString(), ""))

            // Fetch favorites from the server
            lifecycleScope.launch {
                getFavorites()
            }

            return
        }

        getPharmacies()

        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                handler = Handler(mainLooper)
                handler!!.post {
                    getPharmacies()
                }
            }
        }
        timer!!.schedule(timerTask, 0, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        timer?.purge()
    }

    // Information of pharmacy selected
    // Bottom drawer
    private fun showPharmacyDrawer(pharmacy: Pharmacy) {
        val bottomDrawerView = layoutInflater.inflate(R.layout.drawer_pharmacy_layout, null)
        //pharmacy.id?.let { pharmacyImage(it) }

        // Update views with pharmacy information
        // For example:
        bottomDrawerView.findViewById<TextView>(R.id.pharmacy_name)?.text = pharmacy.name
        bottomDrawerView.findViewById<TextView>(R.id.pharmacy_address)?.text = pharmacy.address
        bottomDrawerView.findViewById<ImageView>(R.id.pharmacy_image)?.setImageBitmap(pharmacyImages[pharmacy.id])

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
            val locationCallback : (Location?) -> Unit = { userLocation ->
                val source = "${userLocation?.latitude},${userLocation?.longitude}"
                val uri = "https://www.google.com/maps/dir/?api=1&origin=$source&destination=" + pharmacy.address
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                intent.setPackage("com.google.android.apps.maps")
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            }
            LocationUtils.getUserLocation(locationCallback, this)
        }

        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomDrawerView)
        bottomSheetDialog.show()
    }

    private suspend fun isPharmacyFavorite(id: String, favoriteButton: ToggleButton) {
        val username = getUsername()
        val favoritePharmacy = FavoritePharmacy(username,id)
        val call: Call<StatusResponse> = pharmacistAPI.isPharmacyFavorite(favoritePharmacy)
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
        val locationCallback : (Location?) -> Unit = { location ->
            // Fetch pharmacies only if the location has changed significantly
            if (location != null  && (previousLocation == null ||
                abs(location.latitude - previousLocation!!.latitude) > 0.0001 ||
                abs(location.longitude - previousLocation!!.longitude) > 0.0001)) {
                previousLocation = location

                val onSuccess : (List<Pharmacy>) -> Unit = { pharmaciesList ->
                    val pharmaciesFetched: MutableList<Pharmacy> = mutableListOf()
                    for (pharmacy in pharmaciesList) {
                        // transform pharmacies into a list of Pharmacy objects
                        pharmaciesFetched += pharmacy
                    }
                    // update the pharmacies list
                    pharmacies = pharmaciesFetched
                    Log.d("serverResponse", "Pharmacies retrieved")

                    lifecycleScope.launch {
                        getFavorites()
                    }

                    // TODO - this might waste too much resources
                    // Only way to correctly preview image
                    for (pharmacy in pharmacies) {
                        pharmacy.id?.let { pharmacyImage(it) }
                    }
                }

                pharmacistAPI.getPharmacies(location, onSuccess)
            }
        }
        LocationUtils.getUserLocation(locationCallback, this)
    }

    private fun pharmacyImage(id: String) {
        val onSuccess: (Bitmap) -> Unit = {bitmapImage ->
            pharmacyImages[id] = bitmapImage
            Log.d("serverResponse", "Pharmacy image retrieved")
        }
        pharmacistAPI.pharmacyImage(id, onSuccess)
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

    private suspend fun getFavorites() {
        val onSuccess : (List<Pharmacy>) -> Unit = { pharmaciesList ->
            val pharmaciesFetched: MutableList<Pharmacy> = mutableListOf()
            for (pharmacy in pharmaciesList) {
                // transform pharmacies into a list of Pharmacy objects
                pharmaciesFetched += pharmacy
            }
            pharmaciesFavorite = pharmaciesFetched
            Log.d("serverResponse", "Favorites retrieved")

            for (pharmacy in pharmacies) {
                val marker = mMap?.addMarker(MarkerOptions()
                    .position(LatLng(pharmacy.latitude.toDouble(), pharmacy.longitude.toDouble()))
                    .title(pharmacy.name)
                    .snippet(pharmacy.address)
                )

                if (pharmaciesFavorite.contains(pharmacy)) {
                    marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                }

                marker?.tag = pharmacy

                mMap!!.setOnMarkerClickListener { clickedMarker ->
                    val clickedPharmacy = clickedMarker.tag as Pharmacy
                    showPharmacyDrawer(clickedPharmacy)
                    true // Return true to indicate that the listener has consumed the event
                }
            }
        }
        val username = getUsername()
        pharmacistAPI.getFavoritePharmacies(username, onSuccess)
    }

    private suspend fun handleFavoriteButton(id: String) {
        val username = getUsername()
        val favoritePharmacy = FavoritePharmacy(username,id)
        val call: Call<StatusResponse> = pharmacistAPI.pharmacyFavorite(favoritePharmacy)
        call.enqueue(object : Callback<StatusResponse> {
            override fun onResponse(
                call: Call<StatusResponse>,
                response: Response<StatusResponse>
            ) {
                if (response.isSuccessful) {
                    Log.d("serverResponse", "UPDATED: ${response.code()}")
                    for (pharmacy in pharmacies) {
                        if (pharmacy.id == id) {
                            val marker = mMap!!.addMarker(MarkerOptions()
                                .position(LatLng(pharmacy.latitude.toDouble(), pharmacy.longitude.toDouble()))
                                .title(pharmacy.name)
                                .snippet(pharmacy.address)
                            )
                            marker?.tag = pharmacy

                            if (response.code() == 203) {
                                marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                            } else {
                                marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            }
                            mMap!!.setOnMarkerClickListener { clickedMarker ->
                                val clickedPharmacy = clickedMarker.tag as Pharmacy
                                showPharmacyDrawer(clickedPharmacy)
                                true // Return true to indicate that the listener has consumed the event
                            }
                            break
                        }
                    }
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
    private fun centerUserLocation() {
        val locationCallback : (Location?) -> Unit = { location ->
            location?.let {
                val latLng = LatLng(location.latitude, location.longitude)
                mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            }
        }
        LocationUtils.getUserLocation(locationCallback, this)
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
