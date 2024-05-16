package pt.ulisboa.tecnico.pharmacist

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PharmacyInformationActivity : AppCompatActivity() {

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
                val data = ArrayList<MedicineViewModel>()

                // For testing purposes
                for (i in 1..10) {
                    data.add(MedicineViewModel(R.drawable.baseline_directions_24, newText + " " + i))
                }
                // Set the recycler view adapter to the created adapter
                val adapter = PharmacyPanelSearchAdapter(data)
                recyclerview.adapter = adapter

                return true
            }
        })
    }
}