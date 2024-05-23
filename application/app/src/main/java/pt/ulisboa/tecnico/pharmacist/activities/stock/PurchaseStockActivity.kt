package pt.ulisboa.tecnico.pharmacist.activities.stock


import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import pt.ulisboa.tecnico.pharmacist.MedicineStock
import pt.ulisboa.tecnico.pharmacist.QueryStockResponse
import pt.ulisboa.tecnico.pharmacist.R
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
        val call: Call<QueryStockResponse> = retrofitAPI.getPharmacyStockId(medicineId = medId, pharmacyId = pharmacyId)
        call.enqueue(object : Callback<QueryStockResponse> {
            override fun onResponse(call: Call<QueryStockResponse>, response: Response<QueryStockResponse>) {
                if (response.isSuccessful) {
                    // TODO - Add more error handling
                    val stockResponse = response.body()
                    if (stockResponse != null && stockResponse.stock.isNotEmpty()) {
                        val medicineName = stockResponse.stock[1]
                        val maxStock = stockResponse.stock[0]
                        val existingMedicine = medicines.find { it.id == medId }

                        if (existingMedicine != null) {
                            updateMedicineStock(existingMedicine)
                        } else {
                            val medicine = MedicineStock(id = medId, name = medicineName, pharmacyId = pharmacyId)
                            addMedicineToList(medicine)
                        }
                        Log.d("serverResponse", medicineName + maxStock)
                    }
                }
            }

            override fun onFailure(call: Call<QueryStockResponse>, t: Throwable) {
                // we get error response from API.
                Log.d("serverResponse","FAILED: "+ t.message)
            }
        })
    }
}