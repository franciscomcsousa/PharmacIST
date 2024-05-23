package pt.ulisboa.tecnico.pharmacist.activities.stock


import android.os.Bundle
import android.util.Log
import android.view.View
import pt.ulisboa.tecnico.pharmacist.utils.MedicineStock
import pt.ulisboa.tecnico.pharmacist.utils.QueryStockResponse
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.utils.StatusResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PurchaseStockActivity : StockActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase_stock)
        setupUI(R.id.btn_purchase_stock)
    }

    // fetches the medicine that corresponds to the barcode
    // and is currently available in the pharmacy
    override fun fetchMedicine(medId: String) {
        val call: Call<QueryStockResponse> = pharmacistAPI.getPharmacyStockId(medicineId = medId, pharmacyId = pharmacyId)
        call.enqueue(object : Callback<QueryStockResponse> {
            override fun onResponse(call: Call<QueryStockResponse>, response: Response<QueryStockResponse>) {
                if (response.isSuccessful) {
                    // TODO - Add more error handling
                    val stockResponse = response.body()
                    if (stockResponse != null && stockResponse.stock.isNotEmpty()) {
                        val medicineName = stockResponse.stock[1]
                        val maxStock = stockResponse.stock[0].toIntOrNull()
                        val existingMedicine = medicines.find { it.id == medId }
                        if (existingMedicine != null) {
                            updateMedicineStock(existingMedicine)
                        } else {
                            Log.d("barcode", "Adding new medicine with ID: $medId. Adding new medicine.")
                            val medicine = MedicineStock(id = medId, name = medicineName, pharmacyId = pharmacyId, maxStock = maxStock)
                            addMedicineToList(medicine)
                        }
                        Log.d("serverResponse", medicineName + maxStock)
                    }
                }
            }

            override fun onFailure(call: Call<QueryStockResponse>, t: Throwable) {
                // TODO - add more error handling (display it to the user for him to understand)
                Log.d("serverResponse","FAILED: "+ t.message)
            }
        })
    }

    // user sends post to backend to add the current medicine to pharmacy
    fun purchaseStockButton(view: View) {
        // makes the stock of the medicines the corresponding negative, so it subtracts from the database
        val updatedMedicines = medicines.map { it.copy(stock = -it.stock) }

        val call: Call<StatusResponse> = pharmacistAPI.updateStock(updatedMedicines)
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