package pt.ulisboa.tecnico.pharmacist.activities

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.core.app.ActivityCompat
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.barcode.common.Barcode
import pt.ulisboa.tecnico.pharmacist.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.recycleViewAdapters.MedicineBarcodeAdapter
import pt.ulisboa.tecnico.pharmacist.MedicineResponse
import pt.ulisboa.tecnico.pharmacist.MedicineStock
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.RetrofitAPI
import pt.ulisboa.tecnico.pharmacist.StatusResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class AddStockActivity : AppCompatActivity(), MedicineBarcodeAdapter.RecyclerViewEvent {

    private val CAMERA_PERMISSION_REQUEST_CODE = 1002
    private lateinit var medicineAdapter: MedicineBarcodeAdapter
    private val medicines = mutableListOf<MedicineStock>()
    private lateinit var pharmacyId: String
    private lateinit var addStockButton: Button

    private val retrofit = Retrofit.Builder()
        .baseUrl(DataStoreManager.getUrl())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_stock)
        pharmacyId = intent.getStringExtra("pharmacyId").toString()

        addStockButton = findViewById(R.id.add_stock_btn)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_add_stock)
        medicineAdapter = MedicineBarcodeAdapter(medicines, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = medicineAdapter

        updateAddButtonVisibility()

    }

    // user wants to scan the barcode
    fun scanButton(view: View) {
        if (checkCameraPermission()) {
            startScanner()
        } else {
            requestCameraPermission()
        }
    }

    private fun startScanner() {
        ScannerActivity.startScanner(this) { barcodes ->
            barcodes.forEach { barcode ->
                when (barcode.format) {
                    // currently using this type
                    Barcode.FORMAT_UPC_A -> {
                        barcode.displayValue?.let {
                            Log.d("barcode", "UPC_A: $it")
                            val medicineId = extractMedicineId(it)
                            fetchMedicine(medicineId)
                        }
                    }

                    else -> {
                        Log.d("barcode", "Other format: ${barcode.displayValue}")
                    }
                }
            }
        }
    }

    private fun addMedicineToList(medicine: MedicineStock) {
        runOnUiThread {
            medicines.add(medicine)
            medicineAdapter.notifyItemInserted(medicines.size - 1)
            updateAddButtonVisibility()
        }
    }

    private fun extractMedicineId(upcA: String): String {
        // Remove the checksum digit (last digit) from the UPC-A barcode
        // Convert to integer and back to string to remove leading zeros
        val rawId = upcA.substring(0, upcA.length - 1)
        return rawId.toInt().toString()
    }

    // fetches the medicine that corresponds to the barcode
    private fun fetchMedicine(medId: String) {
        val call: Call<MedicineResponse> = retrofitAPI.getMedicineById(medId)
        call.enqueue(object : Callback<MedicineResponse> {
            override fun onResponse(call: Call<MedicineResponse>, response: Response<MedicineResponse>) {
                if (response.isSuccessful) {
                    // TODO - change or add new method, MedicineResponse is returning
                    //  a list of lists
                    val medicineResponse = response.body()
                    if (medicineResponse != null && medicineResponse.medicine.isNotEmpty()) {
                        val medicineName = "${medicineResponse.medicine[0][1]}"
                        val medicineId = medicineResponse.medicine[0][0].toString()
                        val existingMedicine = medicines.find { it.id == medicineId }

                        if (existingMedicine != null) {
                            updateMedicineStock(existingMedicine)
                        } else {
                            val medicine = MedicineStock(id = medicineId, name = medicineName, pharmacyId = pharmacyId)
                            addMedicineToList(medicine)
                        }
                        Log.d("serverResponse", medicineName)
                    }
                }
            }

            override fun onFailure(call: Call<MedicineResponse>, t: Throwable) {
                // we get error response from API.
                Log.d("serverResponse","FAILED: "+ t.message)
            }
        })
    }


    // user sends post to backend to add the current medicine to pharmacy
    fun addStockButton(view: View) {
        val call: Call<StatusResponse> = retrofitAPI.updateStock(medicines)
        call.enqueue(object : Callback<StatusResponse> {
            override fun onResponse(call: Call<StatusResponse>, response: Response<StatusResponse>) {
                if (response.isSuccessful) {
                    Log.d("serverResponse", response.code().toString())
                    resetMedicineStock()
                }
            }

            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                // we get error response from API.
                Log.d("serverResponse","FAILED: "+ t.message)
            }
        })
    }

    private fun updateMedicineStock(medicine: MedicineStock) {
        runOnUiThread {
            val position = medicines.indexOf(medicine)
            if (position != -1) {
                medicine.stock += 1
                medicineAdapter.notifyItemChanged(position)
            }
        }
    }

    private fun resetMedicineStock() {
        runOnUiThread {
            medicines.clear()
            // TODO - possibly change this
            medicineAdapter.notifyDataSetChanged()
            updateAddButtonVisibility()
        }
    }

    override fun onStockEmpty() {
        updateAddButtonVisibility()
    }
    private fun updateAddButtonVisibility() {
        if (medicines.isNotEmpty()) {
            addStockButton.visibility = View.VISIBLE
        } else {
            addStockButton.visibility = View.GONE
        }
    }

    // Request Permissions: Camera
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with accessing camera
                startScanner()
            } else {
                // Permission denied
                Toast.makeText(this@AddStockActivity, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}