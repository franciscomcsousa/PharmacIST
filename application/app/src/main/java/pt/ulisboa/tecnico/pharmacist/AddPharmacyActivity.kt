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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_pharmacy)
    }

    fun choosePhoto(view: View){
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun previewAddedPhoto(uri: Uri?) {
        findViewById<ImageView>(R.id.add_photo_preview).setImageURI(uri)
    }

    private fun getFields(): List<String> {
        val nameEditText = findViewById<EditText>(R.id.name)
        val addressEditText = findViewById<EditText>(R.id.address)
        val name = nameEditText.text.toString()
        val address = addressEditText.text.toString()

        val latitudeEditText = findViewById<EditText>(R.id.latitude)
        val longitudeEditText = findViewById<EditText>(R.id.longitude)
        val latitude = latitudeEditText.text.toString()
        val longitude = longitudeEditText.text.toString()

        return listOf(name, address, latitude, longitude)
    }

    private fun getFormLayouts(): List<TextInputLayout> {
        val formName = findViewById<TextInputLayout>(R.id.formName)
        val formAddress = findViewById<TextInputLayout>(R.id.formAddress)
        val formLatitude = findViewById<TextInputLayout>(R.id.formLatitude)
        val formLongitude = findViewById<TextInputLayout>(R.id.formLongitude)

        return listOf(formName, formAddress, formLatitude, formLongitude)
    }

    fun createPharmacyClick(view: View) {
        val (name, address, latitude, longitude) = getFields()
        val (formName, formAddress, formLatitude, formLongitude) = getFormLayouts()
        verifyForms(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            formName = formName,
            formAddress = formAddress,
            formLatitude = formLatitude,
            formLongitude = formLongitude,
            uri = currentUri) {
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


        val pharmacy = Pharmacy(name, address, latitude, longitude, image)
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

    private fun verifyForms(
        name: String,
        formName: TextInputLayout,
        address: String,
        formAddress: TextInputLayout,
        latitude: String,
        formLatitude: TextInputLayout,
        longitude: String,
        formLongitude: TextInputLayout,
        uri: Uri?, // TODO - uri can't be empty?
        onSuccess: () -> Unit
    ) {
        formName.error = null
        formAddress.error = null
        formLatitude.error = null
        formLongitude.error = null

        if (name.isEmpty()) {
            formName.error = "Name cannot be empty"
        }
        if (address.isEmpty()) {
            formAddress.error = "Address cannot be empty"
        }
        if (latitude.isEmpty()) {
            formLatitude.error = "Latitude cannot be empty"
        }
        if (longitude.isEmpty()) {
            formLongitude.error = "Longitude cannot be empty"
        }
        if (name.isNotEmpty() && address.isNotEmpty() && latitude.isNotEmpty() && longitude.isNotEmpty()) {
            onSuccess()
        }
    }


}