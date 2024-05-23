package pt.ulisboa.tecnico.pharmacist.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.textfield.TextInputLayout
import pt.ulisboa.tecnico.pharmacist.BuildConfig
import pt.ulisboa.tecnico.pharmacist.utils.CreatePharmacyResponse
import pt.ulisboa.tecnico.pharmacist.utils.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.utils.Pharmacy
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.databaseCache.PharmacistAPI
import pt.ulisboa.tecnico.pharmacist.utils.RetrofitAPI
import pt.ulisboa.tecnico.pharmacist.utils.MediaPickerHandlerActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AddPharmacyActivity : MediaPickerHandlerActivity() {

    val pharmacistAPI = PharmacistAPI()

    private lateinit var autocompleteFragment: AutocompleteSupportFragment
    private var selectedAddress: Place? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_pharmacy)
        startPlacesAPI()
    }

    private fun getFieldName(): String {
        val nameEditText = findViewById<EditText>(R.id.name)
        return nameEditText.text.toString()
    }

    fun createPharmacyClick(view: View) {
        val name = getFieldName()
        val formName = findViewById<TextInputLayout>(R.id.formName)
        val address = selectedAddress?.address ?: ""
        val latLng = selectedAddress?.latLng
        val latitude = latLng?.latitude.toString()
        val longitude = latLng?.longitude.toString()

        verifyForms(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            formName = formName,
            uri = currentUri) {

            // get the latitude and longitude from the place selected
            createPharmacy(name, address, latitude, longitude, currentUri) {
                navigateToNavigationDrawerActivity()
            }
            startActivity(Intent(this, NavigationDrawerActivity::class.java))
        }
    }

    private fun createPharmacy(name: String, address: String, latitude: String, longitude: String, uri: Uri?, onSuccess: () -> Unit) {
        val image = encodeImageToBase64(uri)

        val pharmacy = Pharmacy(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            image = image)
        val call: Call<CreatePharmacyResponse> = pharmacistAPI.createPharmacy(pharmacy)
        call.enqueue(object : Callback<CreatePharmacyResponse> {
            override fun onResponse(call: Call<CreatePharmacyResponse>, response: Response<CreatePharmacyResponse>){
                if (response.isSuccessful) {
                    Log.d("serverResponse","Pharmacy added to database!")
                }
            }

            override fun onFailure(call: Call<CreatePharmacyResponse>, t: Throwable) {
                Log.d("serverResponse","FAILED: "+ t.message)
            }
        })
    }

    private fun navigateToNavigationDrawerActivity() {
        startActivity(Intent(this, NavigationDrawerActivity::class.java))
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
            }
        })
    }

    private fun verifyForms(
        name: String,
        formName: TextInputLayout,
        address: String,
        latitude: String,
        longitude: String,
        uri: Uri?, // TODO - uri can't be empty?
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
}