package pt.ulisboa.tecnico.pharmacist.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.ulisboa.tecnico.pharmacist.utils.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.R
import pt.ulisboa.tecnico.pharmacist.utils.RetrofitAPI
import pt.ulisboa.tecnico.pharmacist.utils.SignInResponse
import pt.ulisboa.tecnico.pharmacist.utils.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RegisterActivity : AppCompatActivity() {
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
            {
                startActivity(Intent(this, NavigationDrawerActivity::class.java))
                setUsername(username)
            },
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
                    setToken(token)
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
            .baseUrl(DataStoreManager.getUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun setUsername(username: String) {
        lifecycleScope.launch {
            dataStore.setUsername(username)
        }
    }

    private fun setToken(token: String) {
        lifecycleScope.launch {
            dataStore.setToken(token)
        }
    }
}