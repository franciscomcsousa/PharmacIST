package pt.ulisboa.tecnico.pharmacist.utils

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import pt.ulisboa.tecnico.pharmacist.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Date
import java.util.Locale

abstract class MediaPickerHandlerActivity : AppCompatActivity() {

    protected var currentUri: Uri? = null
    private val CAMERA_PERMISSION_REQUEST_CODE = 1002

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            currentUri = uri
            previewAddedPhoto(currentUri)
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentUri?.let { uri ->
                previewAddedPhoto(uri)
            }
        } else {
            Log.d("PhotoPicker", "Failed to capture photo")
        }
    }

    fun choosePhoto(view: View) {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Option")
            .setItems(options) { dialogInterface: DialogInterface, which: Int ->
                when (which) {
                    0 -> takePhoto()
                    1 -> pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                dialogInterface.dismiss()
            }
            .show()
    }

    private fun previewAddedPhoto(uri: Uri?) {
        findViewById<ImageView>(R.id.add_photo_preview)?.setImageURI(uri)
    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                currentUri = createImageFileUri()
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentUri)
                takePicture.launch(currentUri)
            } catch (ex: IOException) {
                Log.e("MediaPickerActivity", "Error occurred while creating the file", ex)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun createImageFileUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val imageFile = File(filesDir, "$imageFileName.jpg")
        return FileProvider.getUriForFile(
            this,
            "pt.ulisboa.tecnico.pharmacist.FileProvider",
            imageFile
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
                takePhoto()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Maybe useful later?
    protected fun encodeImageToBase64(uri: Uri?): String {
        if (uri == null) return ""
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

}