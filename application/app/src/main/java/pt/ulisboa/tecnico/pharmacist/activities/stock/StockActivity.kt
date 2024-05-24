package pt.ulisboa.tecnico.pharmacist.activities.stock

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
import pt.ulisboa.tecnico.pharmacist.utils.MedicineStock
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.localDatabase.PharmacistAPI
import pt.ulisboa.tecnico.pharmacist.recycleViewAdapters.MedicineBarcodeAdapter

abstract class StockActivity : AppCompatActivity(), MedicineBarcodeAdapter.RecyclerViewEvent  {
    protected val CAMERA_PERMISSION_REQUEST_CODE = 1002
    protected lateinit var pharmacyId: String
    protected lateinit var medicineAdapter: MedicineBarcodeAdapter
    protected val medicines = mutableListOf<MedicineStock>()
    protected lateinit var actionButton: Button

    protected val pharmacistAPI = PharmacistAPI(this)

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
            if (medicine.maxStock == null || medicine.stock < medicine.maxStock!!) {
                medicines.add(medicine)
                medicineAdapter.notifyItemInserted(medicines.size - 1)
                updateActionButtonVisibility()
            } else {
                Log.d("PurchaseStockActivity", "Max stock reached for ${medicine.name}")
                notifyMaxStockReached(medicine.name)
            }
        }
    }

    protected fun updateMedicineStock(medicine: MedicineStock) {
        runOnUiThread {
            if (medicine.maxStock == null || medicine.stock < medicine.maxStock!!) {
                medicine.stock += 1
                val position = medicines.indexOf(medicine)
                if (position != -1) {
                    medicineAdapter.notifyItemChanged(position)
                }
            } else {
                // Handle case where stock cannot be increased
                Log.d("PurchaseStockActivity", "Max stock reached for ${medicine.name}")
                notifyMaxStockReached(medicine.name)
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

    private fun notifyMaxStockReached(medicineName: String) {
        runOnUiThread {
            Toast.makeText(this, "Max stock reached for $medicineName", Toast.LENGTH_SHORT).show()
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