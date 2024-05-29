package pt.ulisboa.tecnico.pharmacist.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.localDatabase.PharmacistAPI
import pt.ulisboa.tecnico.pharmacist.utils.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.utils.SignInResponse
import pt.ulisboa.tecnico.pharmacist.utils.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


// possible change this to jetpack compose
class LoginActivity : AppCompatActivity() {
    private lateinit var dataStore: DataStoreManager

    // Is it possible without doing it here?
    private var fcmToken: String? = null
    private lateinit var deviceId: String

    private val pharmacistAPI = PharmacistAPI(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        dataStore = DataStoreManager(this@LoginActivity)

        // TODO - review this logic
        // Fetch the FCM
        retrieveFCMToken()

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
        Log.d("FCM", "FCM ??: " + fcmToken)
        val call = pharmacistAPI.sendRegister(user)
        handleRegisterResponse(call, onSuccess, onFailure)
    }

    private fun handleRegisterResponse(call: Call<SignInResponse>, onSuccess: () -> Unit, onFailure: () -> Unit) {
        call.enqueue(object : Callback<SignInResponse> {
            override fun onResponse(
                call: Call<SignInResponse>,
                response: Response<SignInResponse>
            ) {
                if (response.isSuccessful) {
                    val token = response.body()!!.token
                    // store token in preferences datastore
                    setLoginToken(token)
                    onSuccess()
                }
                else {
                    onFailure()
                }
            }

            override fun onFailure(call: Call<SignInResponse>, t: Throwable) {
                Log.d("serverResponse", "FAILED: ${t.message}")
                onFailure()
            }
        })
    }

    private fun loginUser(username: String, password: String, onSuccess: () -> Unit, onFailure: () -> Unit) {

        val user = User(username, password, fcmToken.toString(), deviceId)
        val call = pharmacistAPI.sendLogin(user)
        handleLoginResponse(call, onSuccess, onFailure)
    }

    private fun handleLoginResponse(call: Call<SignInResponse>, onSuccess: () -> Unit, onFailure: () -> Unit) {
        call.enqueue(object : Callback<SignInResponse> {
            override fun onResponse(call: Call<SignInResponse>, response: Response<SignInResponse>) {
                if (response.isSuccessful) {
                    val token = response.body()!!.token
                    // store token in preferences datastore
                    setLoginToken(token)
                    onSuccess()
                }
                else {
                    onFailure()
                }
            }

            override fun onFailure(call: Call<SignInResponse>, t: Throwable) {
                Log.d("serverResponse", "FAILED: ${t.message}")
                onFailure()
            }
        })
    }

    private fun navigateToNavigationDrawerActivity() {
        startActivity(Intent(this, NavigationDrawerActivity::class.java))
    }

    private suspend fun autoLogin(storedToken: String, onSuccess: () -> Unit) {
        val response = pharmacistAPI.getAuth(storedToken)
        if (response.isSuccessful) {
            onSuccess()
        } else {
            // TODO - maybe change this to a persistent message displayed
            // Show toast message for error messages
            /*val message = when(response.code()) {
                600 -> "Token is missing."
                601 -> "Token has expired."
                602 -> "Invalid token."
                else -> "An error occurred. Please try again later."
            }*/
            Toast.makeText(this@LoginActivity, "Session expired!", Toast.LENGTH_LONG).show()
        }
    }


    // TODO - make a class to simplify logic in Login and also (maybe) Register
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