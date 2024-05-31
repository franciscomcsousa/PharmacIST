package pt.ulisboa.tecnico.pharmacist.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.textfield.TextInputLayout
import pt.ulisboa.tecnico.pharmacist.BuildConfig
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.localDatabase.PharmacistAPI
import pt.ulisboa.tecnico.pharmacist.utils.Location
import pt.ulisboa.tecnico.pharmacist.utils.MediaPickerHandler
import pt.ulisboa.tecnico.pharmacist.utils.PermissionUtils
import pt.ulisboa.tecnico.pharmacist.utils.Pharmacy
import java.util.Locale

class AddPharmacyActivity : AppCompatActivity() {

    private lateinit var mediaPickerHandler: MediaPickerHandler

    val pharmacistAPI = PharmacistAPI(this)

    private val REQUEST_MAP_LOCATION = 2001
    private val PERMISSION_REQUEST_ACCESS_LOCATION_CODE = 1001   // good practice
    private val PERMISSION_REQUEST_ACCESS_CAMERA_CODE = 1003


    private lateinit var autocompleteFragment: AutocompleteSupportFragment
    private var selectedAddress: String? = null
    private var latLng: LatLng? = null

    private lateinit var chosenLocationTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pharmacy)
        chosenLocationTextView = findViewById(R.id.chosen_location_text)

        val imageView = findViewById<ImageView>(R.id.add_photo_preview)

        // Initialize MediaPickerHandler with launchers
        mediaPickerHandler = MediaPickerHandler(this, imageView)
        mediaPickerHandler.initializeLaunchers(
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                mediaPickerHandler.handlePickMediaResult(uri)
            },
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                mediaPickerHandler.handleTakePictureResult(success)
            }
        )

        startPlacesAPI()
    }

    private fun getFieldName(): String {
        val nameEditText = findViewById<EditText>(R.id.name)
        return nameEditText.text.toString()
    }

    fun addPhotoClick(view: View) {
        mediaPickerHandler.choosePhoto(view)
    }

    fun createPharmacyClick(view: View) {
        val name = getFieldName()
        val formName = findViewById<TextInputLayout>(R.id.formName)
        val address = selectedAddress?: ""
        val latitude = latLng?.latitude.toString()
        val longitude = latLng?.longitude.toString()

        verifyForms(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            formName = formName,
            uri = mediaPickerHandler.currentUri) {

            // get the latitude and longitude from the place selected
            createPharmacy(name, address, latitude, longitude, mediaPickerHandler.currentUri)
            startActivity(Intent(this, NavigationDrawerActivity::class.java))
        }
    }

    private fun createPharmacy(name: String, address: String, latitude: String, longitude: String, uri: Uri?) {
        val image = mediaPickerHandler.encodeImageToBase64(uri)

        val pharmacy = Pharmacy(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            image = image)

        val onSuccess : () -> Unit = {
            Log.d("serverResponse","Pharmacy added to database!")
        }
        pharmacistAPI.createPharmacy(pharmacy, onSuccess)
    }

    private fun navigateToNavigationDrawerActivity() {
        startActivity(Intent(this, NavigationDrawerActivity::class.java))
    }

    // Chosen: Address
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
                selectedAddress = place.address
                latLng = place.latLng
            }
        })
    }

    // Chosen: Current Location
    fun useCurrentLocation(view: View){
        autocompleteFragment.view?.visibility = View.GONE
        // Retrieve the current user location
        val locationCallback : (Location?) -> Unit = { userLocation ->
            userLocation?.let {
                // Get the suer Coordinates
                Log.d("UserLocation", "Latitude: ${it.latitude}, Longitude: ${it.longitude}")
                selectedAddress = getAddressFromCoordinates(it.latitude,it.longitude)
                latLng = LatLng(it.latitude,it.longitude)
                chosenLocationTextView.text = "Current Location: $selectedAddress"
            }
        }
        PermissionUtils.getUserLocation(locationCallback, this)
        chosenLocationTextView.visibility = View.VISIBLE
    }

    // Chosen: Map Location
    fun useMapLocation(view: View){
        autocompleteFragment.view?.visibility = View.GONE
        selectLocationOnMap()
        chosenLocationTextView.visibility = View.VISIBLE
    }

    // Chosen: Address Location
    fun useAddressLocation(view: View){
        autocompleteFragment.view?.visibility = View.VISIBLE
        chosenLocationTextView.visibility = View.GONE
        startPlacesAPI()
    }


    private fun selectLocationOnMap() {
        val intent = Intent(this, MapsActivity::class.java)
        intent.action = Intent.ACTION_PICK
        startActivityForResult(intent, REQUEST_MAP_LOCATION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MAP_LOCATION && resultCode == Activity.RESULT_OK) {
            val selectedLatitude = data?.getDoubleExtra("latitude", 0.0)
            val selectedLongitude = data?.getDoubleExtra("longitude", 0.0)

            // Handle the selected latitude and longitude
            handleSelectedLocation(selectedLatitude, selectedLongitude)
        }
    }

    private fun handleSelectedLocation(latitude: Double?, longitude: Double?) {
        if (latitude != null && longitude != null) {
            //
            Log.d("SelectedLocation", "Latitude: $latitude, Longitude: $longitude")
            selectedAddress = getAddressFromCoordinates(latitude,longitude)
            latLng = LatLng(latitude,longitude)
            chosenLocationTextView.text = "Current Location: $selectedAddress"

        } else {
            Log.e("SelectedLocation", "Latitude or longitude is null")
        }
    }

    private fun getAddressFromCoordinates(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        var addressText = ""

        val addresses: List<Address>? =
            geocoder.getFromLocation(latitude, longitude, 1)
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            addressText = address.getAddressLine(0)
        }

        return addressText
    }


    private fun verifyForms(
        name: String,
        formName: TextInputLayout,
        address: String,
        latitude: String,
        longitude: String,
        uri: Uri?,
        onSuccess: () -> Unit
    ) {
        formName.error = null

        if (name.isEmpty()) {
            formName.error = "Name cannot be empty"
        }
        if (address.isEmpty()) {
            Log.e("serverResponse", "Address not selected")
            TODO("Show error message to user")
        }
        if (latitude.isEmpty()) {
            Log.e("serverResponse", "latitude not selected")
            TODO("Show error message to user")
        }
        if (longitude.isEmpty()) {
            Log.e("serverResponse", "longitude not selected")
            TODO("Show error message to user")
        }
        if (name.isNotEmpty() && address.isNotEmpty() && latitude.isNotEmpty() && longitude.isNotEmpty()) {
            onSuccess()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION_CODE || requestCode == PERMISSION_REQUEST_ACCESS_CAMERA_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                /*when (requestCode) {
                    PERMISSION_REQUEST_ACCESS_CAMERA_CODE -> addPhotoClick()
                    PERMISSION_REQUEST_ACCESS_LOCATION_CODE -> useCurrentLocation()
                }*/
            } else {
                // Permission denied, redirect to NavigationDrawer activity
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, NavigationDrawerActivity::class.java))
            }
        }
    }
 }