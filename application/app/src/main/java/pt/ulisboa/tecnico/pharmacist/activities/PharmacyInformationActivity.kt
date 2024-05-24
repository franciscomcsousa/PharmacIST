package pt.ulisboa.tecnico.pharmacist.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pt.ulisboa.tecnico.pharmacist.recycleViewAdapters.MedicineSearchAdapter
import pt.ulisboa.tecnico.pharmacist.utils.MedicineSearchViewModel
import pt.ulisboa.tecnico.pharmacist.utils.QueryStock
import pt.ulisboa.tecnico.pharmacist.utils.QueryStockResponse
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.activities.stock.AddStockActivity
import pt.ulisboa.tecnico.pharmacist.activities.stock.PurchaseStockActivity
import pt.ulisboa.tecnico.pharmacist.localDatabase.PharmacistAPI
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PharmacyInformationActivity : AppCompatActivity(), MedicineSearchAdapter.RecyclerViewEvent {

    private val pharmacistAPI = PharmacistAPI(this)

    private lateinit var pharmacyId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pharmacy_information)

        val pharmacyName = intent.getStringExtra("pharmacyName")
        pharmacyId = intent.getStringExtra("pharmacyId").toString()

        findViewById<TextView>(R.id.panel_pharmacy_name).text = pharmacyName

        val searchView = findViewById<SearchView>(R.id.pharmacy_panel_search)
        searchView.clearFocus()

        // Get recycler view
        val recyclerview = findViewById<RecyclerView>(R.id.pharmacy_panel_recycle_view)
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

                queryStock(newText, pharmacyId)

                return true
            }
        })
    }

    override fun onItemClick(position: Int) {
        val medicineName = (findViewById<RecyclerView>(R.id.pharmacy_panel_recycle_view).adapter as MedicineSearchAdapter).medicineList[position].text

        val intent = Intent(this, MedicineInformationActivity::class.java)

        intent.putExtra("medicineName", medicineName)
        startActivity(intent)
    }

    fun queryStock(substring: String, pharmacyId: String) {

        val stockQuery = QueryStock(substring, pharmacyId)

        val call: Call<QueryStockResponse> = pharmacistAPI.getPharmacyStock(stockQuery)
        call.enqueue(object : Callback<QueryStockResponse> {
            override fun onResponse(call: Call<QueryStockResponse>, response: Response<QueryStockResponse>) {
                if (response.isSuccessful) {
                    val stockList = response.body()?.stock

                    val data = ArrayList<MedicineSearchViewModel>()

                    // For testing purposes
                    if (stockList != null) {
                        for (medicine in stockList) {
                            data.add(MedicineSearchViewModel(R.drawable.baseline_directions_24, medicine))
                        }
                    }
                    // Set the recycler view adapter to the created adapter
                    val adapter = MedicineSearchAdapter(data, this@PharmacyInformationActivity)
                    val recyclerview = findViewById<RecyclerView>(R.id.pharmacy_panel_recycle_view)
                    recyclerview.adapter = adapter

                    Log.d("serverResponse","Pharmacy stock obtained")
                }
            }

            override fun onFailure(call: Call<QueryStockResponse>, t: Throwable) {
                Log.d("serverResponse","FAILED: "+ t.message)
            }
        })
    }

    fun addStock(view: View) {
        val intent = Intent(this@PharmacyInformationActivity, AddStockActivity::class.java)
        intent.putExtra("pharmacyId", pharmacyId)
        startActivity(intent)
    }

    fun purchaseStock(view: View) {
        val intent = Intent(this@PharmacyInformationActivity, PurchaseStockActivity::class.java)
        intent.putExtra("pharmacyId", pharmacyId)
        startActivity(intent)
    }
}