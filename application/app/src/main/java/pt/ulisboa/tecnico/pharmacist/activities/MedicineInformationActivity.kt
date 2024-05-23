package pt.ulisboa.tecnico.pharmacist.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import pt.ulisboa.tecnico.pharmacist.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.Location
import pt.ulisboa.tecnico.pharmacist.LocationHandler
import pt.ulisboa.tecnico.pharmacist.MedicineLocation
import pt.ulisboa.tecnico.pharmacist.NearestPharmaciesResponse
import pt.ulisboa.tecnico.pharmacist.PharmacyStock
import pt.ulisboa.tecnico.pharmacist.recycleViewAdapters.PharmacyStockSearchAdapter
import pt.ulisboa.tecnico.pharmacist.PharmacyStockViewModel
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.RetrofitAPI
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MedicineInformationActivity : AppCompatActivity(),
    PharmacyStockSearchAdapter.RecyclerViewEvent {

    private val PERMISSION_REQUEST_ACCESS_LOCATION_CODE = 1001   // good practice

    private val retrofit = Retrofit.Builder()
        .baseUrl(DataStoreManager.getUrl())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medicine_information)

        LocationHandler.requestPermissions(this)

        val medicineName = intent.getStringExtra("medicineName")

        findViewById<TextView>(R.id.panel_medicine_text)?.text = medicineName

        // Get recycler view
        val recyclerView = findViewById<RecyclerView>(R.id.medicine_panel_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Render the nearest pharmacies recycle view
        if (medicineName != null) {
            nearestPharmacies(medicineName)
        }
    }

    private fun nearestPharmacies(medicineName: String) {

        val locationCallback : (Location?) -> Unit = {location ->
            if (location != null) {
                val medicineLocation =
                    MedicineLocation(medicineName, location.latitude, location.longitude)
                val call: Call<NearestPharmaciesResponse> = retrofitAPI.nearbyPharmacyMedicine(medicineLocation)
                val pharmaciesStock: MutableList<PharmacyStock> = mutableListOf()
                val data = ArrayList<PharmacyStockViewModel>()
                call.enqueue(object : Callback<NearestPharmaciesResponse> {

                    override fun onResponse(
                        call: Call<NearestPharmaciesResponse>,
                        response: Response<NearestPharmaciesResponse>
                    ) {
                        val pharmaciesStockResponse = response.body()!!.pharmaciesStock
                        for (pharmacyStock in pharmaciesStockResponse) {
                            val stock = pharmacyStock[2] as Double
                            // TODO - should pharmacyStock[0] be used anywhere?
                            data.add(
                                PharmacyStockViewModel(
                                pharmacyStock[1].toString(),
                                stock.toInt())
                            )
                        }

                        val adapter = PharmacyStockSearchAdapter(data)
                        val recyclerView = findViewById<RecyclerView>(R.id.medicine_panel_recycler_view)
                        recyclerView.adapter = adapter
                    }

                    override fun onFailure(call: Call<NearestPharmaciesResponse>, t: Throwable) {
                        Log.d("serverResponse","FAILED: "+ t.message)
                    }

                })
            }
        }
        LocationHandler.getUserLocation(locationCallback, this)
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
                Toast.makeText(this@MedicineInformationActivity, "Permission denied", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@MedicineInformationActivity, NavigationDrawerActivity::class.java))
            }
        }
    }

    override fun onItemClick(position: Int) {
        TODO("Not yet implemented")
    }

}