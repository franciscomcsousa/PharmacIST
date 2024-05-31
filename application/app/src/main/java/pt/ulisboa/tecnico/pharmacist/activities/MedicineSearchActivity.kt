package pt.ulisboa.tecnico.pharmacist.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.localDatabase.PharmacistAPI
import pt.ulisboa.tecnico.pharmacist.recycleViewAdapters.MedicineSearchAdapter
import pt.ulisboa.tecnico.pharmacist.utils.Location
import pt.ulisboa.tecnico.pharmacist.utils.Medicine
import pt.ulisboa.tecnico.pharmacist.utils.MedicineLocation
import pt.ulisboa.tecnico.pharmacist.utils.MedicinePurpose
import pt.ulisboa.tecnico.pharmacist.utils.MedicineSearchViewModel
import pt.ulisboa.tecnico.pharmacist.utils.PermissionUtils
import pt.ulisboa.tecnico.pharmacist.utils.Pharmacy
import pt.ulisboa.tecnico.pharmacist.utils.showSnackbar

class MedicineSearchActivity : AppCompatActivity(), MedicineSearchAdapter.RecyclerViewEvent {

    private val pharmacistAPI = PharmacistAPI(this)

    private val PERMISSION_REQUEST_ACCESS_LOCATION_CODE = 1001   // good practice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicine_search)

        PermissionUtils.requestLocationPermissions(this)

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
        val medicineCall = Medicine(name = text)
        val onSuccess : (List<List<Any>>) -> Unit = {medicineResponse ->
            val data = ArrayList<MedicineSearchViewModel>()

            for (i in medicineResponse) {
                val medicine = Medicine(id = i[0].toString(),name = i[1].toString())
                data.add(MedicineSearchViewModel(R.drawable.pill, medicine.name, medicine.id.toString()))
            }
            val adapter = MedicineSearchAdapter(data, this@MedicineSearchActivity)
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
            recyclerView.adapter = adapter
        }
        pharmacistAPI.getMedicine(medicineCall, onSuccess)
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
                val medicineLocation = MedicineLocation(name = medicineName, latitude =  location.latitude, longitude =  location.longitude)

                val onSuccess : (List<List<Any>>) -> Unit = { medicineResponse ->
                    Log.d("serverResponse", "Medicine found: $medicineResponse")
                    val medicine = MedicinePurpose(id = medicineResponse[0][0].toString(), name = medicineResponse[0][1].toString(), purpose = medicineResponse[0][2].toString())
                    val pharmacy = Pharmacy(medicineResponse[1][0].toString(), medicineResponse[1][1].toString(), medicineResponse[1][2].toString(),
                        medicineResponse[1][3].toString(), medicineResponse[1][4].toString(), "")

                    // Display the medicine and pharmacy in a new activity
                    navigateToMedicineDetailsActivity(medicine, pharmacy)
                }
                // Code 453
                val onMedicineNotFound : () -> Unit = {
                    runOnUiThread {
                        showSnackbar("Medicine not found")
                    }
                    Log.d("serverResponse","Medicine not found")
                }
                pharmacistAPI.getMedicineLocation(medicineLocation, onSuccess, onMedicineNotFound)
            }
        }
        PermissionUtils.getUserLocation(locationCallback, this)
    }

    private fun navigateToMedicineDetailsActivity(medicine: MedicinePurpose, pharmacy: Pharmacy) {
        val intent = Intent(this, ClosestMedicineActivity::class.java)
        intent.putExtra("medicineId", medicine.id)
        intent.putExtra("medicineName", medicine.name)
        intent.putExtra("medicinePurpose", medicine.purpose)
        intent.putExtra("pharmacyId", pharmacy.id)
        intent.putExtra("pharmacyName", pharmacy.name)
        intent.putExtra("pharmacyAddress", pharmacy.address)
        intent.putExtra("pharmacyLatitude", pharmacy.latitude)
        intent.putExtra("pharmacyLongitude", pharmacy.longitude)

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
                    runOnUiThread {
                        showSnackbar("Permission denied")
                    }
                    // Permission denied, redirect to NavigationDrawer activity
                    startActivity(Intent(this@MedicineSearchActivity, NavigationDrawerActivity::class.java))
            }
        }
    }

}