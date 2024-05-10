package pt.ulisboa.tecnico.pharmacist

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MedicineActivity : AppCompatActivity() {

    // TODO
    // For now contacts the localhost server
    private val url = "http://" + "10.0.2.2" + ":" + 5000 + "/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medicine)
    }

    fun searchMedicineClick(view: View) {
        val medicineName = findViewById<EditText>(R.id.search).text.toString()
        val formMedicineName = findViewById<TextInputLayout>(R.id.formSearch)

        verifyForm(medicineName, formMedicineName) {
            // Search for the medicine in the server
            getMedicine(medicineName)
        }
    }

    private fun getMedicine(medicineName: String) {
        // Call the server to get the medicine
        val medicineLocation = MedicineLocation(medicineName, 38.725387301488965, -9.150040089232286)
        val call: Call<MedicineResponse> = retrofitAPI.getMedicine(medicineLocation)
        call.enqueue(object : Callback<MedicineResponse> {
            override fun onResponse(call: Call<MedicineResponse>, response: Response<MedicineResponse>) {
                if (response.isSuccessful) {
                    val medicineResponse = response.body()!!.medicine
                    Log.d("serverResponse", "Medicine found: $medicineResponse")
                    //displayMedicine(medicineResponse)
                }
                else if (response.code() == 453) {
                    Log.d("serverResponse","Medicine not found")
                }
            }

            override fun onFailure(call: Call<MedicineResponse>, t: Throwable) {
                Log.d("serverResponse","FAILED: "+ t.message)
            }
        })
    }

    private fun verifyForm(medicineName: String, formMedicineName: TextInputLayout, callback: () -> Unit) {
        if (medicineName.isEmpty()) {
            formMedicineName.error = "Please fill in the medicine name"
        } else {
            formMedicineName.error = null
            callback()
        }
    }
}