package pt.ulisboa.tecnico.pharmacist.activities

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.localDatabase.PharmacistAPI
import pt.ulisboa.tecnico.pharmacist.utils.MediaPickerHandlerActivity
import pt.ulisboa.tecnico.pharmacist.utils.MedicineStock
import pt.ulisboa.tecnico.pharmacist.utils.StatusResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NewMedicineActivity : MediaPickerHandlerActivity() {

    private val pharmacistAPI = PharmacistAPI(this)
    private lateinit var medicineId: String
    private lateinit var pharmacyId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_medicine)
        medicineId = intent.getStringExtra("medicineId").toString()
        pharmacyId = intent.getStringExtra("pharmacyId").toString()

    }

    private fun getFields(): List<String> {
        val medicineNameEditText = findViewById<EditText>(R.id.name)
        val quantityEditText = findViewById<EditText>(R.id.quantity)
        val purposeEditText = findViewById<EditText>(R.id.purpose)
        val medicineName = medicineNameEditText.text.toString()
        val quantity = quantityEditText.text.toString()
        val purpose = purposeEditText.text.toString()

        return listOf(medicineName, quantity, purpose)
    }

    private fun getFormLayouts(): List<TextInputLayout> {
        val formMedicineName = findViewById<TextInputLayout>(R.id.formName)
        val formQuantity = findViewById<TextInputLayout>(R.id.formQuantity)
        val formPurpose = findViewById<TextInputLayout>(R.id.formPurpose)

        return listOf(formMedicineName, formQuantity, formPurpose)
    }

    fun createMedicineClick(view: View) {
        val (medicineName, quantity, purpose) = getFields()
        val (formMedicineName, formQuantity, formPurpose) = getFormLayouts()

        verifyForms(
            medicineName = medicineName,
            quantity = quantity,
            purpose = purpose,
            formMedicineName = formMedicineName,
            formQuantity = formQuantity,
            formPurpose = formPurpose,
            uri = currentUri) {

            // get the latitude and longitude from the place selected
            createMedicine(medicineName, quantity, purpose, currentUri) {
                navigateBack()            }
        }
    }

    private fun createMedicine(medicineName: String, quantity: String, purpose: String, uri: Uri?, onSuccess: () -> Unit) {
        val image = encodeImageToBase64(uri)

        onSuccess()

        // TODO
        val medicine = MedicineStock(
            id = medicineId,
            name = medicineName,
            stock = quantity.toInt(),
            purpose = purpose,
            pharmacyId = pharmacyId,
            image = image)
        val call: Call<StatusResponse> = pharmacistAPI.createMedicine(medicine)
        call.enqueue(object : Callback<StatusResponse> {
            override fun onResponse(call: Call<StatusResponse>, response: Response<StatusResponse>){
                if (response.isSuccessful) {
                    Log.d("serverResponse","Pharmacy added to database!")
                }
            }

            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                Log.d("serverResponse","FAILED: "+ t.message)
            }
        })
    }

    private fun navigateBack() {
        finish()
    }

    private fun verifyForms(
        medicineName: String,
        formMedicineName: TextInputLayout,
        quantity: String,
        formQuantity: TextInputLayout,
        purpose: String,
        formPurpose: TextInputLayout,
        uri: Uri?, // TODO - uri can't be empty?
        onSuccess: () -> Unit
    ) {
        formMedicineName.error = null
        formQuantity.error = null
        formPurpose.error = null

        if (medicineName.isEmpty()) {
            formMedicineName.error = "Name cannot be empty"
        }
        if (quantity.isEmpty()) {
            formQuantity.error = "Quantity cannot be empty"
        }
        if (purpose.isEmpty()) {
            formPurpose.error = "Latitude cannot be empty"
        }

        if (medicineName.isNotEmpty() && quantity.isNotEmpty() && purpose.isNotEmpty()) {
            onSuccess()
        }
    }
}