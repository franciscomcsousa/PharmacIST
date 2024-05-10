package pt.ulisboa.tecnico.pharmacist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


// possible change this to jetpack compose
class LoginActivity : AppCompatActivity() {

    // for now contacts the localhost server
    private val url = "http://10.0.2.2:5000/"
    private lateinit var dataStore: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        dataStore = DataStoreManager(this@LoginActivity)

        lifecycleScope.launch {
            val storedToken = getUserToken()
            if (storedToken.isNotEmpty()) {
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
                { navigateToNavigationDrawerActivity() },
                { formUsername.error = "User or Password incorrect!" }
            )
        }
    }

    fun registerButtonClick(view: View?) {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    fun guestButtonClick(view: View?) {
        // whenever using the guest, uses the preferences of this registered user!

        // does not do this if there is a username stored!
        val randomNumber = (0..9999).random()
        val guestName = "guest_$randomNumber"
        loginUser(guestName, "", {
            // also store the user name in preferences in order to reuse the same guest
            navigateToNavigationDrawerActivity()
        } , { })

        startActivity(Intent(this, NavigationDrawerActivity::class.java))
    }

    private fun loginUser(username: String, password: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val retrofit = buildRetrofit()
        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)
        val user = User(username, password)
        val call = retrofitAPI.sendLogin(user)
        handleResponse(call, onSuccess, onFailure)
    }

    private fun handleResponse(call: Call<SignInResponse>, onSuccess: () -> Unit, onFailure: () -> Unit) {
        call.enqueue(object : Callback<SignInResponse> {
            override fun onResponse(call: Call<SignInResponse>, response: Response<SignInResponse>) {
                if (response.isSuccessful) {
                    val token = response.body()!!.token
                    // store token in preferences datastore
                    lifecycleScope.launch {
                        setUserToken(token)
                        // get the token from the datastore and print it
                        // for testing purposes
                        Log.d("TOKEN", getUserToken())
                    }
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
        val retrofit = buildRetrofit()
        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)
        Log.d("serverResponse", "TOKEN IS: $storedToken")
        val response = retrofitAPI.getAuth(storedToken)
        if (response.isSuccessful) {
            onSuccess()
        } else {
            // Show toast message for error messages
            /*val message = when(response.code()) {
                600 -> "Token is missing."
                601 -> "Token has expired."
                602 -> "Invalid token."
                else -> "An error occurred. Please try again later."
            }*/
            Toast.makeText(this@LoginActivity, "Session expired!", Toast.LENGTH_SHORT).show()
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

    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    suspend fun setUserToken(token: String) {
        // stores the token in the datastore
        dataStore.setToken(token)
    }

    suspend fun getUserToken(): String {
        // gets the token from the datastore
        // returns "null" string if token is null
        return dataStore.getToken().toString()
    }
}