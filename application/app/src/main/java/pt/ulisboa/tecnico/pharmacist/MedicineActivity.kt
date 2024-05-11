package pt.ulisboa.tecnico.pharmacist

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MedicineActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_ACCESS_LOCATION_CODE = 1001   // good practice
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // TODO
    // For now contacts the localhost server
    private val url = "http://" + "10.0.2.2" + ":" + 5000 + "/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medicine)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@MedicineActivity)
        requestPermissions()
    }

    fun searchMedicineClick(view: View) {
        val medicineName = findViewById<EditText>(R.id.search).text.toString()
        val formMedicineName = findViewById<TextInputLayout>(R.id.formSearch)

        verifyForm(medicineName, formMedicineName) {
            // Search for the medicine in the server
            getMedicine(medicineName)
        }
    }

    private fun getMedicine(name: String) {
        // If the name is in lowercase, capitalize the first letter for user friendliness
        val medicineName = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        // Call the server to get the medicine
        getUserLocation { location ->
            // sometimes it might not be able to fetch
            // TODO - maybe when null use the last non-null value
            if (location != null) {
                val medicineLocation = MedicineLocation(medicineName, location.latitude, location.longitude)
                val call: Call<MedicineResponse> = retrofitAPI.getMedicine(medicineLocation)
                call.enqueue(object : Callback<MedicineResponse> {
                    override fun onResponse(call: Call<MedicineResponse>, response: Response<MedicineResponse>) {
                        if (response.isSuccessful) {
                            val medicineResponse = response.body()!!.medicine
                            Log.d("serverResponse", "Medicine found: $medicineResponse")
                            val medicine = Medicine(medicineResponse[0][1].toString(), medicineResponse[0][2].toString())
                            val pharmacy = Pharmacy(medicineResponse[1][1].toString(), medicineResponse[1][2].toString(),
                                medicineResponse[1][3].toString(), medicineResponse[1][4].toString(), "")

                            // Display the medicine and pharmacy in a new activity
                            navigateToMedicineDetailsActivity(medicine, pharmacy)
                        }
                        else if (response.code() == 453) {
                            Toast.makeText(this@MedicineActivity, "Medicine not found", Toast.LENGTH_SHORT).show()
                            Log.d("serverResponse","Medicine not found")
                        }
                    }

                    override fun onFailure(call: Call<MedicineResponse>, t: Throwable) {
                        Log.d("serverResponse","FAILED: "+ t.message)
                    }
                })
            }
        }
    }

    private fun verifyForm(medicineName: String, formMedicineName: TextInputLayout, callback: () -> Unit) {
        if (medicineName.isEmpty()) {
            formMedicineName.error = "Please fill in the medicine name"
        } else {
            formMedicineName.error = null
            callback()
        }
    }

    private fun navigateToMedicineDetailsActivity(medicine: Medicine, pharmacy: Pharmacy) {
        val intent = Intent(this, MedicineDetailsActivity::class.java)
        intent.putExtra("medicineName", medicine.name)
        intent.putExtra("medicinePurpose", medicine.purpose)
        intent.putExtra("pharmacyName", pharmacy.name)
        intent.putExtra("pharmacyAddress", pharmacy.address)
        intent.putExtra("pharmacyImage", pharmacy.image)

        startActivity(intent)
    }

    private fun requestPermissions() {
        // verify permissions
        if (ContextCompat.checkSelfPermission(
                this@MedicineActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) else {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this@MedicineActivity,
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
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                else {
                // Permission denied, redirect to NavigationDrawer activity
                Toast.makeText(this@MedicineActivity, "Permission denied", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@MedicineActivity, NavigationDrawerActivity::class.java))
            }
        }
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
}