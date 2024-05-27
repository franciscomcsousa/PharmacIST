package pt.ulisboa.tecnico.pharmacist.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

class ImageUtils {

    companion object {
        fun saveImageToInternalStorage(bitmap: Bitmap, imageName: String, activity: Activity) {
            // Get the internal storage directory
            val context = activity.applicationContext
            val directory = context.getDir("images", Context.MODE_PRIVATE)

            // Create a file within the directory
            val imageFile = File(directory, "$imageName.png")

            var fos: FileOutputStream? = null
            try {
                fos = FileOutputStream(imageFile)
                // Compress the bitmap and write to the output stream
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                fos?.close()
            }
        }

        fun loadImageFromInternalStorage(imageName: String, activity: Activity): Bitmap? {
            // Get the internal storage directory
            val context = activity.applicationContext
            val directory = context.getDir("images", Context.MODE_PRIVATE)

            // Create a file within the directory
            val imageFile = File(directory, "$imageName.png")

            return if (imageFile.exists()) {
                BitmapFactory.decodeFile(imageFile.absolutePath)
            } else {
                null
            }
        }

        fun deleteAllImagesFromInternalStorage(context: Context) {
            // Get the internal storage directory
            val directory = context.getDir("images", Context.MODE_PRIVATE)

            // List all files in the directory
            val files = directory.listFiles()

            // Delete each file
            files?.forEach { file ->
                if (file.isFile && file.exists()) {
                    file.delete()
                }
            }
        }

        fun b64ImageToBitmap(b64Image: String): Bitmap {
            val decodedBytes = Base64.decode(b64Image, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        }
    }
}