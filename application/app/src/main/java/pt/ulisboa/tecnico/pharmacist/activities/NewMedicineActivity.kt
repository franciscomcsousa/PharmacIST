package pt.ulisboa.tecnico.pharmacist.activities

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.localDatabase.PharmacistAPI
import pt.ulisboa.tecnico.pharmacist.utils.MediaPickerHandler
import pt.ulisboa.tecnico.pharmacist.utils.MedicineStock
import pt.ulisboa.tecnico.pharmacist.utils.showSnackbar

class NewMedicineActivity : AppCompatActivity() {

    private val pharmacistAPI = PharmacistAPI(this)
    private lateinit var medicineId: String
    private lateinit var pharmacyId: String
    private lateinit var mediaPickerHandler: MediaPickerHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_medicine)
        medicineId = intent.getStringExtra("medicineId").toString()
        pharmacyId = intent.getStringExtra("pharmacyId").toString()

        val imageView = findViewById<ImageView>(R.id.add_photo_preview)

        // Initialize MediaPickerHandler with launchers
        mediaPickerHandler = MediaPickerHandler(this, imageView)
        mediaPickerHandler.initializeLaunchers(
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                mediaPickerHandler.handlePickMediaResult(uri)
            },
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                mediaPickerHandler.handleTakePictureResult(success)
            }
        )

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

    fun addPhotoClick(view: View) {
        mediaPickerHandler.choosePhoto(view)
    }

    fun createMedicineClick(view: View) {
        val (medicineName, quantity, purpose) = getFields()
        val (formMedicineName, formQuantity, formPurpose) = getFormLayouts()

        verifyForms(
            medicineName = medicineName,
            formMedicineName = formMedicineName,
            quantity = quantity,
            formQuantity = formQuantity,
            purpose = purpose,
            formPurpose = formPurpose
        ) {

            // get the latitude and longitude from the place selected
            createMedicine(medicineName, quantity, purpose, mediaPickerHandler.currentUri)
        }
    }

    private fun createMedicine(medicineName: String, quantity: String, purpose: String, uri: Uri?) {
        val image = mediaPickerHandler.encodeImageToBase64(uri)
        navigateBack()
        val medicine = MedicineStock(
            id = medicineId,
            name = medicineName,
            stock = quantity.toInt(),
            purpose = purpose,
            pharmacyId = pharmacyId,
            image = image)
        val onSuccess : () -> Unit = {
            runOnUiThread {
                showSnackbar("Medicine is now registered and added to pharmacy stock")
            }
            Log.d("serverResponse","Pharmacy added to database!")
        }
        pharmacistAPI.createMedicine(medicine, onSuccess)
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