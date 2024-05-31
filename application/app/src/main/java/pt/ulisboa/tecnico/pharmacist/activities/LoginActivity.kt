package pt.ulisboa.tecnico.pharmacist.activities

import android.app.Dialog
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
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
import pt.ulisboa.tecnico.pharmacist.utils.showSnackbar


// possible change this to jetpack compose
class LoginActivity : AppCompatActivity() {
    private lateinit var dataStore: DataStoreManager

    // Is it possible without doing it here?
    private var fcmToken: String? = null
    private lateinit var deviceId: String

    private lateinit var dialog: Dialog

    private val pharmacistAPI = PharmacistAPI(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        enableEdgeToEdge()
        dataStore = DataStoreManager(this@LoginActivity)

        dialog = Dialog(this)
        dialog.setContentView(R.layout.network_dialog_box)
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_bg)
        dialog.setCancelable(false)

        val continueButton = dialog.findViewById<View>(R.id.network_continue_button)
        continueButton.setOnClickListener {
            setLoginToken("")
            navigateToNavigationDrawerActivity()
            dialog.dismiss()
        }

        val retryButton = dialog.findViewById<View>(R.id.network_retry_button)
        retryButton.setOnClickListener {
            if (hasNetworkConnection()) {
                attemptUserAutoLogin()
                dialog.dismiss()
            }
        }

        // TODO - review this logic
        // Fetch the FCM
        retrieveFCMToken()

        // Check if there is network connection
        if (!hasNetworkConnection()) {
            dialog.show()
        }
        else {
            attemptUserAutoLogin()
        }
    }

    private fun attemptUserAutoLogin() {
        lifecycleScope.launch {
            deviceId = dataStore.getDeviceId()
            val storedToken = getLoginToken()
            if (storedToken.isNotEmpty() && storedToken != "null") {
                // If a token is stored, attempt automatic login
                autoLogin(storedToken) {
                    navigateToNavigationDrawerActivity()
                }
            }
        }
    }

    fun loginButtonClick(view: View?) {
        val (username, password) = getUsernameAndPassword()
        val (formUsername, formPassword) = getFormLayouts()
        verifyForms(username, formUsername, password, formPassword) {
            loginUser(username, password,
                {
                    navigateToNavigationDrawerActivity()
                    setUsername(username)
                },
                { formUsername.error = "User or Password incorrect!" }
            )
        }
    }

    fun registerButtonClick(view: View?) {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    fun guestButtonClick(view: View?) {
        lifecycleScope.launch {
            val storedGuestName = getGuestName()
            if (storedGuestName.isNotEmpty() && storedGuestName != "null") {
                loginUser(storedGuestName, "",
                    {
                        navigateToNavigationDrawerActivity()
                        setUsername(storedGuestName)
                    },
                    { Toast.makeText(this@LoginActivity, "Failed to login as guest!", Toast.LENGTH_LONG).show() }
                )
            } else {
                val randomNumber = (0..9999).random()
                val guestName = "guest_$randomNumber"
                registerUser(guestName, "",
                    {
                        navigateToNavigationDrawerActivity()
                        setGuestName(guestName)
                        setUsername(guestName)
                    },
                    { Toast.makeText(this@LoginActivity, "Failed to register as guest!", Toast.LENGTH_LONG).show() })
            }
        }
    }

    private fun registerUser(username: String, password: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val user = User(username, password, fcmToken.toString(), deviceId)
        Log.d("FCM", "FCM ??: $fcmToken")
        val onStartToken : (String) -> Unit = { token ->
            // store token in preferences datastore
            setLoginToken(token)
        }
        pharmacistAPI.sendRegister(user, onSuccess, onFailure, onStartToken)
    }

    private fun loginUser(username: String, password: String, onSuccess: () -> Unit, onFailure: () -> Unit) {

        val user = User(username, password, fcmToken.toString(), deviceId)
        val onStartToken : (String) -> Unit = { token ->
            // store token in preferences datastore
            setLoginToken(token)
        }
        pharmacistAPI.sendLogin(user, onSuccess, onFailure, onStartToken)
    }

    private fun navigateToNavigationDrawerActivity() {
        startActivity(Intent(this, NavigationDrawerActivity::class.java))
    }

    private suspend fun autoLogin(storedToken: String, onSuccess: () -> Unit) {
        val onExpiry : () -> Unit = {
            runOnUiThread {
                showSnackbar("Session expired! Please login again.")
            }
        }
        pharmacistAPI.getAuth(storedToken, onSuccess, onExpiry)
    }

    private fun hasNetworkConnection(): Boolean {
        try {
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.getActiveNetworkInfo()
            return networkInfo != null && networkInfo.isConnected()
        } catch (e: Exception) {
            Log.e("Network Error", e.toString())
        }
        return false
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

    private suspend fun getLoginToken(): String {
        // returns "null" string if token is null
        return dataStore.getLoginToken().toString()
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

    private fun setUsername(username: String) {
        lifecycleScope.launch {
            dataStore.setUsername(username)
        }
    }

    private fun setGuestName(guestname: String) {
        lifecycleScope.launch {
            dataStore.setGuestName(guestname)
        }
    }

    private suspend fun getGuestName(): String {
        return dataStore.getGuestName().toString()
    }

}