package pt.ulisboa.tecnico.pharmacist.activities.stock

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.activities.NewMedicineActivity
import pt.ulisboa.tecnico.pharmacist.utils.MedicineStock


class AddStockActivity : StockActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_stock)
        setupUI(R.id.add_stock_btn)
    }

    // fetches the medicine that corresponds to the barcode
    override fun fetchMedicine(medId: String) {
        val onSuccess : (List<List<Any>>) -> Unit = { medicine ->
            if (medicine.isNotEmpty()) {
                val medicineName = "${medicine[0][1]}"
                val medicineId = medicine[0][0].toString()
                val existingMedicine = medicines.find { it.id == medicineId }

                if (existingMedicine != null) {
                    updateMedicineStock(existingMedicine)
                } else {
                    val medicineStock = MedicineStock(id = medicineId, name = medicineName, pharmacyId = pharmacyId)
                    addMedicineToList(medicineStock)
                }
                Log.d("serverResponse", medicineName)
            }
        }
        val onMedicineNotFound : () -> Unit = {
            val intent = Intent(applicationContext, NewMedicineActivity::class.java)
            intent.putExtra("medicineId", medId)
            intent.putExtra("pharmacyId", pharmacyId)
            startActivity(intent)
        }
        pharmacistAPI.getMedicineById(medId, onSuccess, onMedicineNotFound)
    }

    // user sends post to backend to add the current medicine to pharmacy
    fun addStockButton(view: View) {
        val onSuccess : (Int) -> Unit = { responseCode ->
            Log.d("serverResponse", responseCode.toString())
            resetMedicineStock()
        }
        pharmacistAPI.updateStock(medicines, onSuccess)
    }
}