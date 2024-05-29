package pt.ulisboa.tecnico.pharmacist.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import pt.ulisboa.tecnico.pharmacist.utils.Location
import pt.ulisboa.tecnico.pharmacist.utils.PermissionUtils
import pt.ulisboa.tecnico.pharmacist.utils.MedicineLocation
import pt.ulisboa.tecnico.pharmacist.utils.NearestPharmaciesResponse
import pt.ulisboa.tecnico.pharmacist.utils.PharmacyStock
import pt.ulisboa.tecnico.pharmacist.recycleViewAdapters.PharmacyStockSearchAdapter
import pt.ulisboa.tecnico.pharmacist.utils.PharmacyStockViewModel
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.localDatabase.PharmacistAPI
import pt.ulisboa.tecnico.pharmacist.utils.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.utils.MedicineNotification
import pt.ulisboa.tecnico.pharmacist.utils.Pharmacy
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MedicineInformationActivity : AppCompatActivity(),
    PharmacyStockSearchAdapter.RecyclerViewEvent {

    private val PERMISSION_REQUEST_ACCESS_LOCATION_CODE = 1001   // good practice

    private val pharmacistAPI = PharmacistAPI(this)

    private lateinit var dataStore: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicine_information)

        PermissionUtils.requestLocationPermissions(this)

        val medicineName = intent.getStringExtra("medicineName")
        val medicineId = intent.getStringExtra("medicineId")

        findViewById<TextView>(R.id.panel_medicine_text)?.text = medicineName
        medicineImage(medicineId.toString())
        // Get recycler view
        val recyclerView = findViewById<RecyclerView>(R.id.medicine_panel_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Render the nearest pharmacies recycle view
        if (medicineName != null) {
            nearestPharmacies(medicineName)
        }

        dataStore = DataStoreManager(this@MedicineInformationActivity)

        // Medicine Notification button
        val notificationButton = findViewById<Button>(R.id.medicine_notification_btn)
        notificationButton.setOnClickListener {
            if (medicineId != null) {
                lifecycleScope.launch {
                    handleNotificationButton(medicineId)
                }
            }
        }
    }

    private fun nearestPharmacies(medicineName: String) {

        val locationCallback : (Location?) -> Unit = { location ->
            if (location != null) {
                val medicineLocation =
                    MedicineLocation(name = medicineName, latitude = location.latitude, longitude = location.longitude)

                val pharmaciesStock: MutableList<PharmacyStock> = mutableListOf()
                val data = ArrayList<PharmacyStockViewModel>()

                val onSuccess : (List<List<Any>>) -> Unit = {pharmaciesStockResponse ->
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
                pharmacistAPI.nearbyPharmacyMedicine(medicineLocation, onSuccess)
            }
        }
        PermissionUtils.getUserLocation(locationCallback, this)
    }

    private fun medicineImage(medicineId: String) {
        val onSuccess : (Bitmap) -> Unit = {bitmap ->
            // Set the bitmap to the ImageView
            findViewById<ImageView>(R.id.panel_medicine_image).setImageBitmap(bitmap)

            Log.d("serverResponse", "Medicine image retrieved")
        }
        pharmacistAPI.medicineImage(medicineId, onSuccess)
    }

    private suspend fun handleNotificationButton(medicineId: String) {
        val username = dataStore.getUsername().toString()
        val medicineNotification = MedicineNotification(username, medicineId)
        val onSuccess: (Int) -> Unit = {responseCode ->
            if (responseCode == 205) {
                // TODO ? - Perhaps some console logs
            }
            else if (responseCode == 206) {
                // TODO ? - Perhaps some console logs
            }
        }

        pharmacistAPI.medicineNotification(medicineNotification, onSuccess)
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