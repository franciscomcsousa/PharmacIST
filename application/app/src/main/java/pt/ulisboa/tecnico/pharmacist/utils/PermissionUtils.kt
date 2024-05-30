package pt.ulisboa.tecnico.pharmacist.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

class PermissionUtils {

    // good practice
    companion object {
        val PERMISSION_REQUEST_ACCESS_LOCATION_CODE = 1001
        val PERMISSION_REQUEST_ACCESS_NOTIFICATION_CODE = 1002
        private val PERMISSION_REQUEST_ACCESS_CAMERA_CODE = 1003

        fun requestLocationPermissions(activity: Activity) : Boolean {
            // verify permissions
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                return true
            }
                else {
                // Permission is not granted, request it
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_ACCESS_LOCATION_CODE
                )
                return false
            }
        }

        fun requestCameraPermissions(activity: Activity): Boolean {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                return true

            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.CAMERA),
                    PERMISSION_REQUEST_ACCESS_CAMERA_CODE
                )
                return false
            }
        }

        fun requestNotificationPermissions(activity: Activity) : Boolean {
            // verify permissions
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                return true
            }
                else {
                // Permission is not granted, request it
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        PERMISSION_REQUEST_ACCESS_NOTIFICATION_CODE
                    )
                }
                return false
            }
        }

        fun requestLocationAndNotificationPermissions(activity: Activity): Boolean {
            val locationGranted = requestLocationPermissions(activity)
            val notificationGranted = requestNotificationPermissions(activity)
            return locationGranted && notificationGranted
        }

        @SuppressLint("MissingPermission")  // IDE does not consider how this function is called
        fun getUserLocation(callback: (Location?) -> Unit, activity: Activity) {
            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
            val locationTask = fusedLocationProviderClient.lastLocation
            locationTask.addOnSuccessListener { location ->
                // Check if location is not null before using it
                val userLocation = location?.let {
                    Location(it.latitude, it.longitude)
                }
                callback(userLocation)
            }
        }

    }

}