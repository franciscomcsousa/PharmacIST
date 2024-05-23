package pt.ulisboa.tecnico.pharmacist.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.barcode.common.Barcode
import pt.ulisboa.tecnico.pharmacist.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.recycleViewAdapters.MedicineBarcodeAdapter
import pt.ulisboa.tecnico.pharmacist.MedicineStock
import pt.ulisboa.tecnico.pharmacist.QueryStockResponse
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.RetrofitAPI
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PurchaseStockActivity : AppCompatActivity(), MedicineBarcodeAdapter.RecyclerViewEvent {

    // FOR DEBUG UNTIL FINISHED
    //private val url = "http://" + "10.0.2.2" + ":" + 5000 + "/"
    private val retrofit = Retrofit.Builder()
        .baseUrl(DataStoreManager.getUrl()/*url*/)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val retrofitAPI = retrofit.create(RetrofitAPI::class.java)


    private val CAMERA_PERMISSION_REQUEST_CODE = 1002
    private lateinit var pharmacyId: String
    private lateinit var medicineAdapter: MedicineBarcodeAdapter
    private val medicines = mutableListOf<MedicineStock>()
    private lateinit var purchaseButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase_stock)
        pharmacyId = intent.getStringExtra("pharmacyId").toString()

        purchaseButton = findViewById(R.id.btn_purchase_stock)
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

    // fetches the medicine that corresponds to the barcode
    // and is currently available in the pharmacy
    private fun fetchMedicine(medId: String) {
        val call: Call<QueryStockResponse> = retrofitAPI.getPharmacyStockId(medicineId = medId, pharmacyId = pharmacyId)
        call.enqueue(object : Callback<QueryStockResponse> {
            override fun onResponse(call: Call<QueryStockResponse>, response: Response<QueryStockResponse>) {
                if (response.isSuccessful) {
                    // TODO - change or add new method, MedicineResponse is returning
                    //  a list of lists
                    val stockResponse = response.body()
                    if (stockResponse != null) {
                        Log.d("serverResponse", stockResponse.stock.toString())
                    }
                }
            }

            override fun onFailure(call: Call<QueryStockResponse>, t: Throwable) {
                // we get error response from API.
                Log.d("serverResponse","FAILED: "+ t.message)
            }
        })
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
            purchaseButton.visibility = View.VISIBLE
        } else {
            purchaseButton.visibility = View.GONE
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
                Toast.makeText(this@PurchaseStockActivity, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}