package pt.ulisboa.tecnico.pharmacist.activities.stock

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.barcode.common.Barcode
import pt.ulisboa.tecnico.pharmacist.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.MedicineStock
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.RetrofitAPI
import pt.ulisboa.tecnico.pharmacist.activities.ScannerActivity
import pt.ulisboa.tecnico.pharmacist.recycleViewAdapters.MedicineBarcodeAdapter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

abstract class StockActivity : AppCompatActivity(), MedicineBarcodeAdapter.RecyclerViewEvent  {
    protected val CAMERA_PERMISSION_REQUEST_CODE = 1002
    protected lateinit var pharmacyId: String
    protected lateinit var medicineAdapter: MedicineBarcodeAdapter
    protected val medicines = mutableListOf<MedicineStock>()
    protected lateinit var actionButton: Button

    protected val retrofit = Retrofit.Builder()
        .baseUrl(DataStoreManager.getUrl())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    protected val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pharmacyId = intent.getStringExtra("pharmacyId").toString()
    }

    protected fun setupUI(actionButtonId: Int) {
        actionButton = findViewById(actionButtonId)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_add_stock)
        medicineAdapter = MedicineBarcodeAdapter(medicines, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = medicineAdapter
        updateActionButtonVisibility()
    }

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

    private fun extractMedicineId(upcA: String): String {
        val rawId = upcA.substring(0, upcA.length - 1)
        return rawId.toInt().toString()
    }

    protected abstract fun fetchMedicine(medId: String)

    protected fun addMedicineToList(medicine: MedicineStock) {
        runOnUiThread {
            medicines.add(medicine)
            medicineAdapter.notifyItemInserted(medicines.size - 1)
            updateActionButtonVisibility()
        }
    }

    protected fun updateMedicineStock(medicine: MedicineStock) {
        runOnUiThread {
            val position = medicines.indexOf(medicine)
            if (position != -1) {
                medicine.stock += 1
                medicineAdapter.notifyItemChanged(position)
            }
        }
    }

    protected fun resetMedicineStock() {
        runOnUiThread {
            medicines.clear()
            medicineAdapter.notifyDataSetChanged()
            updateActionButtonVisibility()
        }
    }

    override fun onStockEmpty() {
        updateActionButtonVisibility()
    }

    private fun updateActionButtonVisibility() {
        if (medicines.isNotEmpty()) {
            actionButton.visibility = View.VISIBLE
        } else {
            actionButton.visibility = View.GONE
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
                startScanner()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}