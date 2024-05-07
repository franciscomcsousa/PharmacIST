package pt.ulisboa.tecnico.pharmacist

import android.os.Bundle
import android.os.Handler
import android.util.Log
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

    private var pharmacies: MutableList<Pharmacy> = mutableListOf()

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

        // Get the first batch of pharmacies from the backend
        getPharmacies()
        // TODO for now it sleeps to wait for the first batch of pharmacies, maybe change later
        Thread.sleep(100)

        // Move the camera to Marquês de Pombal
        val location = LatLng(38.725387301488965, -9.150040089232286)
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0f))
        mMap!!.setMinZoomPreference(0.0f)
        mMap!!.setMaxZoomPreference(30.0f)

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
        getPharmacies()
        for (pharmacy in pharmacies) {
            // Add a marker for each pharmacy
            mMap!!.addMarker(MarkerOptions()
                .position(LatLng(pharmacy.latitude.toDouble(), pharmacy.longitude.toDouble()))
                .title(pharmacy.name)
                .snippet(pharmacy.address)
            )
        }
    }

    private fun getPharmacies() {

        val pharmaciesFetched: MutableList<Pharmacy> = mutableListOf()
        val location = Location(38.725387301488965, -9.150040089232286)
        val call: Call<PharmaciesResponse> = retrofitAPI.getPharmacies(location)
        call.enqueue(object : Callback<PharmaciesResponse> {
            override fun onResponse(call: Call<PharmaciesResponse>, response: Response<PharmaciesResponse>) {
                val statusCode = response.code()
                if (response.isSuccessful) {
                    val pharmaciesList = response.body()!!.pharmacies
                    for (pharmacy in pharmaciesList) {
                        // transform pharmacies into a list of Pharmacy objects
                        pharmaciesFetched += Pharmacy(pharmacy[1].toString(), pharmacy[2].toString(),
                            pharmacy[3].toString(), pharmacy[4].toString())
                    }

                    // update the pharmacies list
                    pharmacies = pharmaciesFetched
                }
            }

            override fun onFailure(call: Call<PharmaciesResponse>, t: Throwable) {
                // we get error response from API.
                Log.d("serverResponse","FAILED: "+ t.message)
            }
        })
    }

    private fun stopTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer!!.purge()
        }
    }
}