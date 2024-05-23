package pt.ulisboa.tecnico.pharmacist.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

class LocationHandler {

    companion object {
        private val PERMISSION_REQUEST_ACCESS_LOCATION_CODE = 1001   // good practice
        fun requestPermissions(activity: Activity) : Boolean {
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