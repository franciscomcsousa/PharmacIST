// MediaPickerHandler.kt
package pt.ulisboa.tecnico.pharmacist.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.Date
import java.util.Locale

class MediaPickerHandler(private val activity: Activity, private val imageView: ImageView) {

    private val MAX_IMAGE_SIZE = 100 // in kB
    var currentUri: Uri? = null
    private val CAMERA_PERMISSION_REQUEST_CODE = 1003

    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var takePicture: ActivityResultLauncher<Uri>

    fun initializeLaunchers(pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>, takePictureLauncher: ActivityResultLauncher<Uri>) {
        pickMedia = pickMediaLauncher
        takePicture = takePictureLauncher
    }

    fun choosePhoto(view: View) {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(activity)
            .setTitle("Select Option")
            .setItems(options) { dialogInterface: DialogInterface, which: Int ->
                when (which) {
                    0 -> requestCameraPermission()
                    1 -> pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                dialogInterface.dismiss()
            }
            .show()
    }

    private fun previewAddedPhoto(uri: Uri?) {
        imageView.setImageURI(uri)
    }

    fun handlePickMediaResult(uri: Uri?) {
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            currentUri = uri
            previewAddedPhoto(currentUri)
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    fun handleTakePictureResult(success: Boolean) {
        if (success) {
            currentUri?.let { uri ->
                previewAddedPhoto(uri)
            }
        } else {
            Log.d("PhotoPicker", "Failed to capture photo")
        }
    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                currentUri = createImageFileUri()
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentUri)
                takePicture.launch(currentUri)
            } catch (ex: IOException) {
                Log.e("MediaPickerHandler", "Error occurred while creating the file", ex)
            }
        } else {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    private fun requestCameraPermission() {
        if (PermissionUtils.requestCameraPermissions(activity)) {
            takePhoto()
        }
    }

    private fun createImageFileUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val imageFile = File(activity.filesDir, "$imageFileName.jpg")
        return FileProvider.getUriForFile(
            activity,
            "pt.ulisboa.tecnico.pharmacist.FileProvider",
            imageFile
        )
    }

    // Maybe useful later?
    fun encodeImageToBase64(uri: Uri?): String {
        if (uri == null) return ""
        val inputStream = activity.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val maxFileSize = MAX_IMAGE_SIZE * 1024
        var quality = 100
        val byteArrayOutputStream = ByteArrayOutputStream()
        // Compress the image until its size is less than the maximum
        do {
            byteArrayOutputStream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            quality -= 10
        } while (byteArrayOutputStream.size() > maxFileSize && quality > 0)

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}
