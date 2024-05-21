package pt.ulisboa.tecnico.pharmacist

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.core.app.ActivityCompat
import android.util.Log
import android.view.View
import com.google.mlkit.vision.barcode.common.Barcode


class AddStockActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_stock)

    }

    // user wants to scan the barcode
    fun scanButton(view: View) {
        if (checkCameraPermission()) {
            startBarcodeScanning()
        } else {
            requestCameraPermission()
        }
    }

    // uses the ScannerActivity and obtains the values obtained
    private fun startBarcodeScanning() {
        // get the scanner info
        startScanner()

        // additional logic to add list of medicines and amount of stock
    }

    private fun startScanner() {
        ScannerActivity.startScanner(this) { barcodes ->
            barcodes.forEach { barcode ->
                when (barcode.format) {
                    // currently using this type
                    Barcode.FORMAT_UPC_A -> {
                        barcode.displayValue?.let {
                            Log.d("barcode", "UPC_A: $it")
                        }
                    }
                    else -> {
                        Log.d("barcode", "Other format: ${barcode.displayValue}")
                    }
                }
            }
        }
    }


    // Checks for the Camera Permission
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

    // Request Permissions: Camera
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with accessing camera
                startBarcodeScanning()
            } else {
                // Permission denied
                Toast.makeText(this@AddStockActivity, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}