package pt.ulisboa.tecnico.pharmacist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RegisterActivity : AppCompatActivity() {
    private val url = "http://10.0.2.2:5000/"
    private lateinit var dataStore: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        dataStore = DataStoreManager(this@RegisterActivity)
    }


    fun registerButtonClick(view: View?) {
        val (username, password) = getUsernameAndPassword()
        val (formUsername, formPassword) = getFormLayouts()
        verifyForms(username, formUsername, password, formPassword) {
            registerUser(username, password,
            { startActivity(Intent(this, NavigationDrawerActivity::class.java)) },
            { formUsername.error = "User already exists!" }
            )
        }
    }


    private fun registerUser(username: String, password: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val retrofit = buildRetrofit()
        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)
        val user = User(username, password)
        val call = retrofitAPI.sendRegister(user)
        handleResponse(call, onSuccess, onFailure)
    }

    private fun handleResponse(call: Call<SignInResponse>, onSuccess: () -> Unit, onFailure: () -> Unit) {
        call.enqueue(object : Callback<SignInResponse> {
            override fun onResponse(
                call: Call<SignInResponse>,
                response: Response<SignInResponse>
            ) {
                if (response.isSuccessful) {
                    val token = response.body()!!.token
                    // store token in preferences datastore
                    lifecycleScope.launch {
                        setUserToken(token)
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