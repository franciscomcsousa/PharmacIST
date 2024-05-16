package pt.ulisboa.tecnico.pharmacist

import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PharmacyInformationActivity : AppCompatActivity() {

    // TODO
    // For now contacts the localhost server
    private val url = "http://" + "10.0.2.2" + ":" + 5000 + "/"

    private var currentUri: Uri? = null
    private val retrofit = Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pharmacy_information)

        val pharmacyName = intent.getStringExtra("pharmacyName")
        val pharmacyId = intent.getStringExtra("pharmacyId")

        findViewById<TextView>(R.id.panel_pharmacy_name).text = pharmacyName

        val searchView = findViewById<SearchView>(R.id.pharmacy_panel_search)

        // Get recycler view
        val recyclerview = findViewById<RecyclerView>(R.id.panel_recycle_view)
        recyclerview.layoutManager = LinearLayoutManager(this)

        // Search query listener
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                // TODO - should something be here?
                return true
            }

            // Everytime search text changes this function is called
            override fun onQueryTextChange(newText: String): Boolean {

                // Clear the recycler view if characters of query are less than 3
                if (newText.length < 3) {
                    recyclerview.adapter = null
                    return true
                }

                if (pharmacyId != null) {
                    queryStock(newText, pharmacyId)
                }

                return true
            }
        })
    }

    fun queryStock(substring: String, pharmacyId: String) {

        val stockQuery = QueryStock(substring, pharmacyId)

        val call: Call<QueryStockResponse> = retrofitAPI.getPharmacyStock(stockQuery)
        call.enqueue(object : Callback<QueryStockResponse> {
            override fun onResponse( call: Call<QueryStockResponse>, response: Response<QueryStockResponse>) {
                if (response.isSuccessful) {
                    val stockList = response.body()?.stock

                    val data = ArrayList<MedicineViewModel>()

                    // For testing purposes
                    if (stockList != null) {
                        for (medicine in stockList!!) {
                            data.add(MedicineViewModel(R.drawable.baseline_directions_24, medicine))
                        }
                    }
                    // Set the recycler view adapter to the created adapter
                    val adapter = PharmacyPanelSearchAdapter(data)
                    val recyclerview = findViewById<RecyclerView>(R.id.panel_recycle_view)
                    recyclerview.adapter = adapter

                    Log.d("serverResponse","Pharmacy stock obtained")
                }
            }

            override fun onFailure(call: Call<QueryStockResponse>, t: Throwable) {
                Log.d("serverResponse","FAILED: "+ t.message)
            }
        })
    }
}