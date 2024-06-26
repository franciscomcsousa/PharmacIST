package pt.ulisboa.tecnico.pharmacist.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.localDatabase.PharmacistAPI
import pt.ulisboa.tecnico.pharmacist.utils.DataStoreManager

class ClosestMedicineActivity : AppCompatActivity() {

    private val pharmacistAPI = PharmacistAPI(this)

    private lateinit var dataStore: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_closest_medicine)

        dataStore = DataStoreManager(this@ClosestMedicineActivity)

        val medicineId = intent.getStringExtra("medicineId")
        val medicineName = intent.getStringExtra("medicineName")
        val medicinePurpose = intent.getStringExtra("medicinePurpose")
        val pharmacyId = intent.getStringExtra("pharmacyId")
        val pharmacyName = intent.getStringExtra("pharmacyName")
        val pharmacyAddress = intent.getStringExtra("pharmacyAddress")
        val pharmacyLatitude = intent.getStringExtra("pharmacyLatitude")
        val pharmacyLongitude = intent.getStringExtra("pharmacyLongitude")

        findViewById<TextView>(R.id.medicine_name).text = medicineName
        findViewById<TextView>(R.id.medicine_purpose).text = medicinePurpose
        findViewById<TextView>(R.id.pharmacy_name).text = pharmacyName
        findViewById<TextView>(R.id.pharmacy_address).text = pharmacyAddress

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

        findViewById<TextView>(R.id.medicine_info_button).setOnClickListener {
            val intent = intent
            intent.setClass(this, MedicineInformationActivity::class.java)
            intent.putExtra("medicineId", medicineId)
            intent.putExtra("medicineName", medicineName)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.share_button).setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, "I found the medicine $medicineName at $pharmacyName, $pharmacyAddress")
            intent.setType("text/plain")
            startActivity(Intent.createChooser(intent, "Share using"))
        }

        // Fetches medicine image and loads it
        medicineImage(medicineId.toString())
    }

    private fun medicineImage(medicineId: String) {
        val onSuccess : (Bitmap?) -> Unit = { bitmap ->
            // Set the bitmap to the ImageView
            val imageView = findViewById<ImageView>(R.id.medicine_image)

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            }
            else {
                val bitmapDefault = BitmapFactory.decodeResource(resources, R.drawable.default_medicine)
                imageView.setImageBitmap(bitmapDefault)
            }

            Log.d("serverResponse", "Medicine image retrieved")
        }
        lifecycleScope.launch {
            val dataMode = dataStore.getDataMode()
            pharmacistAPI.medicineImage(medicineId, dataMode, onSuccess)
        }
    }
}