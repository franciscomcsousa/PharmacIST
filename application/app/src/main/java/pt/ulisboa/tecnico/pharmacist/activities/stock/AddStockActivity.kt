package pt.ulisboa.tecnico.pharmacist.activities.stock

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import pt.ulisboa.tecnico.pharmacist.utils.MedicineResponse
import pt.ulisboa.tecnico.pharmacist.utils.MedicineStock
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.activities.NewMedicineActivity
import pt.ulisboa.tecnico.pharmacist.utils.StatusResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class AddStockActivity : StockActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_stock)
        setupUI(R.id.add_stock_btn)
    }

    // fetches the medicine that corresponds to the barcode
    override fun fetchMedicine(medId: String) {
        val call: Call<MedicineResponse> = retrofitAPI.getMedicineById(medId)
        call.enqueue(object : Callback<MedicineResponse> {
            override fun onResponse(call: Call<MedicineResponse>, response: Response<MedicineResponse>) {
                if (response.isSuccessful) {
                    // TODO - change or add new method, MedicineResponse is returning
                    //  a list of lists
                    val medicineResponse = response.body()
                    if (medicineResponse != null && medicineResponse.medicine.isNotEmpty()) {
                        val medicineName = "${medicineResponse.medicine[0][1]}"
                        val medicineId = medicineResponse.medicine[0][0].toString()
                        val existingMedicine = medicines.find { it.id == medicineId }

                        if (existingMedicine != null) {
                            updateMedicineStock(existingMedicine)
                        } else {
                            val medicine = MedicineStock(id = medicineId, name = medicineName, pharmacyId = pharmacyId)
                            addMedicineToList(medicine)
                        }
                        Log.d("serverResponse", medicineName)
                    }
                }
                // if the medicine is not recognizable, goes to new medicine activity
                if(response.code() == 453) {
                    startActivity(Intent(applicationContext, NewMedicineActivity::class.java))
                }
            }

            override fun onFailure(call: Call<MedicineResponse>, t: Throwable) {
                // TODO - add more error handling (display it to the user for him to understand)
                Log.d("serverResponse","FAILED: "+ t.message )
            }
        })
    }

    // user sends post to backend to add the current medicine to pharmacy
    fun addStockButton(view: View) {
        val call: Call<StatusResponse> = retrofitAPI.updateStock(medicines)
        call.enqueue(object : Callback<StatusResponse> {
            override fun onResponse(call: Call<StatusResponse>, response: Response<StatusResponse>) {
                if (response.isSuccessful) {
                    Log.d("serverResponse", response.code().toString())
                    resetMedicineStock()
                }
            }

            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                // we get error response from API.
                Log.d("serverResponse","FAILED: "+ t.message)
            }
        })
    }
}