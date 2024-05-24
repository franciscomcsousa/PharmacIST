package pt.ulisboa.tecnico.pharmacist.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pt.ulisboa.tecnico.pharmacist.utils.Location
import pt.ulisboa.tecnico.pharmacist.utils.LocationUtils
import pt.ulisboa.tecnico.pharmacist.utils.Medicine
import pt.ulisboa.tecnico.pharmacist.utils.MedicineLocation
import pt.ulisboa.tecnico.pharmacist.utils.MedicinePurpose
import pt.ulisboa.tecnico.pharmacist.utils.MedicineResponse
import pt.ulisboa.tecnico.pharmacist.recycleViewAdapters.MedicineSearchAdapter
import pt.ulisboa.tecnico.pharmacist.utils.MedicineSearchViewModel
import pt.ulisboa.tecnico.pharmacist.utils.Pharmacy
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.localDatabase.PharmacistAPI
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MedicineSearchActivity : AppCompatActivity(), MedicineSearchAdapter.RecyclerViewEvent {

    private val pharmacistAPI = PharmacistAPI(this)

    private val PERMISSION_REQUEST_ACCESS_LOCATION_CODE = 1001   // good practice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medicine_search)

        LocationUtils.requestPermissions(this)

        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.clearFocus()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.length < 3) {
                    recyclerView.adapter = null
                    return true
                }
                queryMedicines(newText)
                return true
            }
        })
    }

    private fun queryMedicines(text: String) {
        // perform a call to the server to get the list of medicines
        val medicineCall = Medicine(text)
        val call: Call<MedicineResponse> = pharmacistAPI.getMedicine(medicineCall)
        call.enqueue(object : Callback<MedicineResponse> {
            override fun onResponse(call: Call<MedicineResponse>, response: Response<MedicineResponse>) {
                if (response.isSuccessful) {
                    val medicineResponse = response.body()!!.medicine
                    val data = ArrayList<MedicineSearchViewModel>()

                    for (i in medicineResponse) {
                        val medicine = Medicine(i[1].toString())
                        data.add(MedicineSearchViewModel(R.drawable.pill, medicine.name))
                    }
                    val adapter = MedicineSearchAdapter(data, this@MedicineSearchActivity)
                    val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
                    recyclerView.adapter = adapter
                }
            }

            override fun onFailure(call: Call<MedicineResponse>, t: Throwable) {
                Log.d("serverResponse", "FAILED: " + t.message)
            }
        })
    }

    override fun onItemClick(position: Int) {
        val medicineName = (findViewById<RecyclerView>(R.id.recyclerView).adapter as MedicineSearchAdapter).medicineList[position].text
        getMedicine(medicineName)
    }

    private fun getMedicine(name: String) {
        // If the name is in lowercase, capitalize the first letter for user friendliness
        val medicineName = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        // Call the server to get the medicine
        val locationCallback : (Location?) -> Unit = { location ->
            // sometimes it might not be able to fetch
            // TODO - maybe when null use the last non-null value
            if (location != null) {
                val medicineLocation = MedicineLocation(medicineName, location.latitude, location.longitude)
                val call: Call<MedicineResponse> = pharmacistAPI.getMedicineLocation(medicineLocation)
                call.enqueue(object : Callback<MedicineResponse> {
                    override fun onResponse(call: Call<MedicineResponse>, response: Response<MedicineResponse>) {
                        if (response.isSuccessful) {
                            val medicineResponse = response.body()!!.medicine
                            Log.d("serverResponse", "Medicine found: $medicineResponse")
                            val medicine = MedicinePurpose(medicineResponse[0][1].toString(), medicineResponse[0][2].toString())
                            val pharmacy = Pharmacy(medicineResponse[1][0].toString(), medicineResponse[1][1].toString(), medicineResponse[1][2].toString(),
                                medicineResponse[1][3].toString(), medicineResponse[1][4].toString(), "")

                            // Display the medicine and pharmacy in a new activity
                            navigateToMedicineDetailsActivity(medicine, pharmacy)
                        }
                        else if (response.code() == 453) {
                            Toast.makeText(this@MedicineSearchActivity, "Medicine not found", Toast.LENGTH_SHORT).show()
                            Log.d("serverResponse","Medicine not found")
                        }
                    }

                    override fun onFailure(call: Call<MedicineResponse>, t: Throwable) {
                        Log.d("serverResponse","FAILED: "+ t.message)
                    }
                })
            }
        }
        LocationUtils.getUserLocation(locationCallback, this)
    }

    private fun navigateToMedicineDetailsActivity(medicine: MedicinePurpose, pharmacy: Pharmacy) {
        val intent = Intent(this, ClosestMedicineActivity::class.java)
        intent.putExtra("medicineName", medicine.name)
        intent.putExtra("medicinePurpose", medicine.purpose)
        intent.putExtra("pharmacyId", pharmacy.id)
        intent.putExtra("pharmacyName", pharmacy.name)
        intent.putExtra("pharmacyAddress", pharmacy.address)
        intent.putExtra("pharmacyLatitude", pharmacy.latitude)
        intent.putExtra("pharmacyLongitude", pharmacy.longitude)
        intent.putExtra("pharmacyImage", pharmacy.image)

        startActivity(intent)
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
                Toast.makeText(this@MedicineSearchActivity, "Permission denied", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@MedicineSearchActivity, NavigationDrawerActivity::class.java))
            }
        }
    }

}