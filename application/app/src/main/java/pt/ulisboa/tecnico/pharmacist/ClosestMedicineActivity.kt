package pt.ulisboa.tecnico.pharmacist

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class ClosestMedicineActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cloesest_medicine)

        // Retrieve the medicine and pharmacy from the intent
        val medicineName = intent.getStringExtra("medicineName")
        val medicinePurpose = intent.getStringExtra("medicinePurpose")
        val pharmacyName = intent.getStringExtra("pharmacyName")
        val pharmacyAddress = intent.getStringExtra("pharmacyAddress")
        val pharmacyImage = intent.getStringExtra("pharmacyImage")

        // TODO - Later display the medicine and pharmacy image in the activity

        // Display the medicine and pharmacy in the activity
        findViewById<TextView>(R.id.medicine_name).text = medicineName
        findViewById<TextView>(R.id.medicine_purpose).text = medicinePurpose
        findViewById<TextView>(R.id.pharmacy_name).text = pharmacyName
        findViewById<TextView>(R.id.pharmacy_address).text = pharmacyAddress
    }
}