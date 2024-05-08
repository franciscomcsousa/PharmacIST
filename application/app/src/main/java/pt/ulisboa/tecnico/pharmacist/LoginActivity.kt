package pt.ulisboa.tecnico.pharmacist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// possible change this to jetpack compose
class LoginActivity : AppCompatActivity() {

    // for now contacts the localhost server
    private val url = "http://" + "10.0.2.2" + ":" + 5000 + "/"

    private lateinit var viewModel: MainViewModel
    private lateinit var dataStoreManager: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        dataStoreManager = DataStoreManager(this)

        /*
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/
    }

    fun loginButtonClick(view: View?) {

        // send already encrypted for safety (?)
        // get username and password from the EditText
        val username = findViewById<View>(R.id.username) as EditText
        val password = findViewById<View>(R.id.password) as EditText

        loginUser(
            username.getText().toString(),
            password.getText().toString()
        )
        val intent = Intent(this, DrawerActivity::class.java)
        startActivity(intent)
    }

    fun registerButtonClick(view: View?) {

        // send already encrypted for safety (?)
        // get username and password from the EditText
        val username = findViewById<View>(R.id.username) as EditText
        val password = findViewById<View>(R.id.password) as EditText

        registerUser(
            username.getText().toString(),
            password.getText().toString()
        )
        val intent = Intent(this, DrawerActivity::class.java)
        startActivity(intent)
    }

    // when clicking the guest button redirect to mapsActivity
    fun guestButtonClick(view: View?) {
        val intent = Intent(this, DrawerActivity::class.java)
        startActivity(intent)
    }

    private fun registerUser(username: String, password: String) {

        // retrofit builder
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            // json converter
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

        val user = User(username, password)

        val call: Call<SignInResponse> = retrofitAPI.sendRegister(user)
        call.enqueue(object : Callback<SignInResponse> {
            // when we get response
            override fun onResponse(call: Call<SignInResponse>, response: Response<SignInResponse>) {
                if (response.isSuccessful) {
                    val token = response.body()!!.token

                    // store token in preferences datastore
                    viewModel.setToken(token)

                    // get the token from the datastore and print it
                    // for testing purposes
                    viewModel.getToken.observe(this@LoginActivity) { tokenDS ->
                        Log.d("TOKEN", tokenDS)
                    }
                }
            }

            override fun onFailure(call: Call<SignInResponse>, t: Throwable) {
                // we get error response from API.
                Log.d("serverResponse","FAILED: "+ t.message)
            }
        })
    }

    private fun loginUser(username: String, password: String) {

        // retrofit builder
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            // json converter
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

        val user = User(username, password)

        val call: Call<SignInResponse> = retrofitAPI.sendLogin(user)
        call.enqueue(object : Callback<SignInResponse> {
            // when we get response
            override fun onResponse(call: Call<SignInResponse>, response: Response<SignInResponse>) {
                if (response.isSuccessful) {
                    val token = response.body()!!.token

                    // store token in preferences datastore
                    viewModel.setToken(token)

                    // get the token from the datastore and print it
                    // for testing purposes
                    viewModel.getToken.observe(this@LoginActivity) { tokenDS ->
                        Log.d("TOKEN", tokenDS)
                    }
                }
            }

            override fun onFailure(call: Call<SignInResponse>, t: Throwable) {
                // we get error response from API.
                Log.d("serverResponse","FAILED: "+ t.message)
            }
        })
    }
}

