package pt.ulisboa.tecnico.pharmacist

import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.ImageButton
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MapsActivity : FragmentActivity(), OnMapReadyCallback {
    private var mMap: GoogleMap? = null
    private var binding: ActivityMapsBinding? = null
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private var handler: Handler? = null

    // for now contacts the localhost server
    private val url = "http://" + "10.0.2.2" + ":" + 5000 + "/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        val recenterButton = findViewById<ImageButton>(R.id.current_loc)
        recenterButton.setOnClickListener {
            // Move the camera to Marquês de Pombal (for now)
            val location = LatLng(38.725387301488965, -9.150040089232286)
            mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0f))
        }
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
        val pharmacies = getPharmaciesTest()
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
        var pharmacies: List<Pharmacy> = emptyList()
        val location = Location(38.725387301488965, -9.150040089232286)
        val call: Call<List<Pharmacy>?>? = retrofitAPI.getPharmacies(location)
        call!!.enqueue(object : Callback<List<Pharmacy>?> {
            override fun onResponse(call: Call<List<Pharmacy>?>, response: Response<List<Pharmacy>?>) {
                if (response.isSuccessful) {
                    println("Pharmacies received")
                    pharmacies = response.body()!!
                } else {
                    println("Failed to get pharmacies")
                }
            }

            override fun onFailure(call: Call<List<Pharmacy>?>, t: Throwable) {
                println("Failed to get pharmacies")
            }
        })
        return pharmacies
    }

    private fun getPharmaciesTest(): List<Pharmacy> {
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