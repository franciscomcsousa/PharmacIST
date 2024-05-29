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
        val onSuccess : (List<String>) -> Unit = { stock ->
            // TODO - Add more error handling
            if (stock.isNotEmpty()) {
                val medicineName = stock[1]
                val maxStock = stock[0].toIntOrNull()
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
        pharmacistAPI.getPharmacyStockId(medicineId = medId, pharmacyId = pharmacyId, onSuccess)
    }

    // user sends post to backend to add the current medicine to pharmacy
    fun purchaseStockButton(view: View) {
        // makes the stock of the medicines the corresponding negative, so it subtracts from the database
        val updatedMedicines = medicines.map { it.copy(stock = -it.stock) }

        val onSuccess : (Int) -> Unit = { responseCode ->
            Log.d("serverResponse", responseCode.toString())
            resetMedicineStock()
        }
        pharmacistAPI.updateStock(updatedMedicines, onSuccess)
    }
}