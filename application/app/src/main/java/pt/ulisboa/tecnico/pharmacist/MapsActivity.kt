package pt.ulisboa.tecnico.pharmacist

import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import pt.ulisboa.tecnico.pharmacist.databinding.ActivityMapsBinding
import java.util.Timer
import java.util.TimerTask

class MapsActivity : FragmentActivity(), OnMapReadyCallback {
    private var mMap: GoogleMap? = null
    private var binding: ActivityMapsBinding? = null
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private var handler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Move the camera to Marquês de Pombal
        val location = LatLng(38.725387301488965, -9.150040089232286)
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0f))
        mMap!!.setMinZoomPreference(0.0f)
        mMap!!.setMaxZoomPreference(16.0f)

        // Add a blue marker in Marquês de Pombal
        mMap!!.addMarker(MarkerOptions()
            .position(location)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            .title("Marquês de Pombal")
            .snippet("I am here"))

        // Make a call to the backend to get the pharmacies every 10 seconds in a new thread
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                handler = Handler(mainLooper)
                handler!!.post {
                    println("Updating pharmacies")
                    addPharmacies()
                    println("Pharmacies updated")
                }
            }
        }
        timer!!.schedule(timerTask, 0, 10000)
    }

    private fun addPharmacies() {
        val pharmacies = getPharmacies()
        for (pharmacy in pharmacies) {
            // Add a marker for each pharmacy
            mMap!!.addMarker(MarkerOptions()
                .position(LatLng(pharmacy.latitude, pharmacy.longitude))
                .title(pharmacy.name)
                .snippet(pharmacy.address)
            )
        }
    }

    private fun getPharmacies(): List<Pharmacy> {
        // TODO Make a call to the backend to get the pharmacies
        return listOf(
            Pharmacy("Farmácia A", "Rua A", 38.728467, -9.148590),
            Pharmacy("Farmácia B", "Rua B", 38.724075, -9.150967),
            Pharmacy("Farmácia C", "Rua C", 38.725360, -9.148243),
        )
    }

    private fun stopTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer!!.purge()
        }
    }
}