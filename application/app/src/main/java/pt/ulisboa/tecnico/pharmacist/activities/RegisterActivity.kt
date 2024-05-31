package pt.ulisboa.tecnico.pharmacist.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.localDatabase.PharmacistAPI
import pt.ulisboa.tecnico.pharmacist.utils.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.utils.User

class RegisterActivity : AppCompatActivity() {
    private lateinit var dataStore: DataStoreManager

    // Is it possible without doing it here?
    private var fcmToken: String? = null
    private lateinit var deviceId: String

    private val pharmacistAPI = PharmacistAPI(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        dataStore = DataStoreManager(this@RegisterActivity)

        retrieveFCMToken()

        lifecycleScope.launch {
            deviceId = dataStore.getDeviceId()
        }

    }


    fun registerButtonClick(view: View?) {
        val (username, password) = getUsernameAndPassword()
        val (formUsername, formPassword) = getFormLayouts()
        verifyForms(username, formUsername, password, formPassword) {
            registerUser(username, password,
            {
                startActivity(Intent(this, NavigationDrawerActivity::class.java))
                setUsername(username)
            },
            { formUsername.error = "User already exists!" }
            )
        }
    }


    private fun registerUser(username: String, password: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        // TODO - Error message in case fcmToken was not received. Or just try again.
        val user = User(username, password, fcmToken.toString(), deviceId)
        val onStartToken : (String) -> Unit = { token ->
            // store token in preferences datastore
            setLoginToken(token)
        }
        pharmacistAPI.sendRegister(user, onSuccess, onFailure, onStartToken)
    }


    // Should only be fetched when there is none in the backend or its expired!
    private fun retrieveFCMToken() {
        // only does this if there is no FCM Token stored!
        lifecycleScope.launch {
            val storedFcmToken = dataStore.getFCMToken()
            if (storedFcmToken.isNullOrEmpty()) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        lifecycleScope.launch {
                            setFCMToken(token)
                            fcmToken = token
                            Log.d("FCM", token)
                        }
                    } else {
                        Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                    }
                })
            }
        }
    }

    private fun verifyForms(
        username: String,
        formUsername: TextInputLayout,
        password: String,
        formPassword: TextInputLayout,
        onSuccess: () -> Unit
    ) {
        formUsername.error = null
        formPassword.error = null

        if (username.isEmpty()) {
            formUsername.error = "Username cannot be empty!"
        }
        if (password.isEmpty()) {
            formPassword.error = "Password cannot be empty!"
        }
        if (username.isNotEmpty() && password.isNotEmpty()) {
            onSuccess()
        }
    }

    private fun getUsernameAndPassword(): Pair<String, String> {
        val usernameEditText = findViewById<EditText>(R.id.username)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()
        return Pair(username, password)
    }

    private fun getFormLayouts(): Pair<TextInputLayout, TextInputLayout> {
        val formUsername = findViewById<TextInputLayout>(R.id.formUsername)
        val formPassword = findViewById<TextInputLayout>(R.id.formPassword)
        return Pair(formUsername, formPassword)
    }

    private fun setUsername(username: String) {
        lifecycleScope.launch {
            dataStore.setUsername(username)
        }
    }

    private fun setLoginToken(token: String) {
        lifecycleScope.launch {
            dataStore.setLoginToken(token)
        }
    }

    private fun setFCMToken(token: String) {
        lifecycleScope.launch {
            dataStore.setFCMToken(token)
        }
    }
}