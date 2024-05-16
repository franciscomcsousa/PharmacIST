package pt.ulisboa.tecnico.pharmacist

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class PharmacyInformationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pharmacy_information)

        val pharmacyName = intent.getStringExtra("pharmacyName")
        val pharmacyId = intent.getStringExtra("pharmacyId")

        findViewById<TextView>(R.id.panel_pharmacy_name).text = pharmacyName
    }
}