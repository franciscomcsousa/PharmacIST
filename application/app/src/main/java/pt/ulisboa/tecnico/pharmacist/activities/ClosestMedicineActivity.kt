package pt.ulisboa.tecnico.pharmacist.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import pt.ulisboa.tecnico.pharmacist.R

class ClosestMedicineActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_closest_medicine)

        // Retrieve the medicine and pharmacy from the intent
        val medicineName = intent.getStringExtra("medicineName")
        val medicinePurpose = intent.getStringExtra("medicinePurpose")
        val pharmacyId = intent.getStringExtra("pharmacyId")
        val pharmacyName = intent.getStringExtra("pharmacyName")
        val pharmacyAddress = intent.getStringExtra("pharmacyAddress")
        val pharmacyLatitude = intent.getStringExtra("pharmacyLatitude")
        val pharmacyLongitude = intent.getStringExtra("pharmacyLongitude")
        val pharmacyImage = intent.getStringExtra("pharmacyImage")

        // TODO - Later display the medicine and pharmacy image in the activity

        // Display the medicine and pharmacy in the activity
        findViewById<TextView>(R.id.medicine_name).text = medicineName
        findViewById<TextView>(R.id.medicine_purpose).text = medicinePurpose
        findViewById<TextView>(R.id.pharmacy_name).text = pharmacyName
        findViewById<TextView>(R.id.pharmacy_address).text = pharmacyAddress

        // set on click listener for the navigate button, which will go to the MapsActivity
        // and move the camera to the pharmacy location
        findViewById<TextView>(R.id.navigate_button).setOnClickListener {
            val intent = intent
            intent.setClass(this, MapsActivity::class.java)
            intent.putExtra("pharmacyId", pharmacyId)
            intent.putExtra("pharmacyName", pharmacyName)
            intent.putExtra("pharmacyAddress", pharmacyAddress)
            intent.putExtra("pharmacyLatitude", pharmacyLatitude)
            intent.putExtra("pharmacyLongitude", pharmacyLongitude)
            startActivity(intent)
        }

        // set on click listener for the share button which will share the pharmacy name, address
        // and medicine name with other apps
        findViewById<TextView>(R.id.share_button).setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, "I found the medicine $medicineName at $pharmacyName, $pharmacyAddress")
            intent.setType("text/plain")
            startActivity(Intent.createChooser(intent, "Share using"))
        }
    }
}