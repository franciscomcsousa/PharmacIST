package pt.ulisboa.tecnico.pharmacist

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream

class AddPharmacyActivity : AppCompatActivity() {

    // TODO
    // For now contacts the localhost server
    private val url = "http://" + "10.0.2.2" + ":" + 5000 + "/"

    private var currentUri: Uri? = null
    private val retrofit = Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

    // Registers a photo picker activity launcher in single-select mode.
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            currentUri = uri
            previewAddedPhoto(currentUri)
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }
    // address autocomplete
    private lateinit var autocompleteFragment: AutocompleteSupportFragment
    private var selectedAddress: Place? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_pharmacy)
        startPlacesAPI()

    }

    fun choosePhoto(view: View){
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun previewAddedPhoto(uri: Uri?) {
        findViewById<ImageView>(R.id.add_photo_preview).setImageURI(uri)
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
        var image = ""
        if (uri != null) {
            // Get bitmap from URI
            val inputStream = this.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Encode bitmap into Base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val imageB64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
            image = imageB64
        }

        val pharmacy = Pharmacy(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            image = image)
        val call: Call<CreatePharmacyResponse> = retrofitAPI.createPharmacy(pharmacy)
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
                TODO("Not yet implemented")
            }

            override fun onPlaceSelected(place: Place) {
                place.address?.let { Log.d("serverResponse", "This was the address selected!: $it") }
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