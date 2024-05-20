package pt.ulisboa.tecnico.pharmacist

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MedicineInformationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medicine_information)

        val medicineName = intent.getStringExtra("medicineName")

        println("WOOO")
        println(medicineName)

        findViewById<TextView>(R.id.panel_medicine_text)?.text = medicineName
    }

}