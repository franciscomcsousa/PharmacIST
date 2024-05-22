package pt.ulisboa.tecnico.pharmacist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import pt.ulisboa.tecnico.pharmacist.databinding.ActivityScannerBinding
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors


// Called whenever an activity needs to scan barcodes
class ScannerActivity : AppCompatActivity() {

    // TODO: revisit camera stuff. Maybe change take photo to use this camera in AddPharmacy
    private lateinit var cameraSelector: CameraSelector // choose between front or rear camera
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var processCameraProvider: ProcessCameraProvider // instance to show the Camera Preview and Scan
    private lateinit var cameraPreview: Preview // display camera
    private lateinit var imageAnalysis: ImageAnalysis // analyse the camera images

    private lateinit var binding: ActivityScannerBinding

    @ExperimentalGetImage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        cameraProviderFuture = ProcessCameraProvider.getInstance(this@ScannerActivity)

        cameraProviderFuture.addListener(
            {
                try {
                    processCameraProvider = cameraProviderFuture.get()
                    bindCameraPreview() // bind the camera preview to the PreviewView
                    bindInputAnalyser()
                } catch (e: ExecutionException) {
                    Log.e(TAG, "Unhandled exception", e)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Unhandled exception", e)
                }
            }, ContextCompat.getMainExecutor(this@ScannerActivity)
        )

    }

    private fun bindCameraPreview() {
        // ensures the view is initialized
        binding.cameraView.post {
            val display = binding.cameraView.display
            // prevents Null pointer exception
            if (display != null) {
                cameraPreview = Preview.Builder()
                    .setTargetRotation(display.rotation)
                    .build()
                cameraPreview.setSurfaceProvider(binding.cameraView.surfaceProvider)
                try {
                    processCameraProvider.bindToLifecycle(this, cameraSelector, cameraPreview)
                } catch (illegalStateException: IllegalStateException) {
                    Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
                } catch (illegalArgumentException: IllegalArgumentException) {
                    Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
                }
            } else {
                Log.e(TAG, "Display is null")
            }
        }
    }

    @ExperimentalGetImage
    private fun bindInputAnalyser() {
        // ensures the view is initialized
        binding.cameraView.post {
            val display = binding.cameraView.display
            // prevents Null pointer exception
            if (display != null) {
                val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build()
                )
                imageAnalysis = ImageAnalysis.Builder()
                    .setTargetRotation(display.rotation)
                    .build()
                val cameraExecutor = Executors.newSingleThreadExecutor()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(barcodeScanner, imageProxy)
                }
                try {
                    processCameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
                } catch (illegalStateException: IllegalStateException) {
                    Log.e(TAG, illegalStateException.message ?: "IllegalStateException")
                } catch (illegalArgumentException: IllegalArgumentException) {
                    Log.e(TAG, illegalArgumentException.message ?: "IllegalArgumentException")
                }
            } else {
                Log.e(TAG, "Display is null")
            }
        }
    }

    @ExperimentalGetImage
    private fun  processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    onScan?.invoke(barcodes)
                    onScan = null
                    finish()
                }
            }
            .addOnFailureListener {
                Log.e(TAG, it.message ?: it.toString())
            }.addOnCompleteListener {
                imageProxy.close()
            }
    }


    companion object {
        private val TAG = "ScannerActivity"
        private var onScan: ((barcodes: List<Barcode>) -> Unit)? = null

        // when this function is called, it runs
        // the ScannerActivity and reads the barcode returning what read
        fun startScanner(context: Context, onScan: (barcodes: List<Barcode>) -> Unit) {
            this.onScan = onScan
            Intent(context, ScannerActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }
}