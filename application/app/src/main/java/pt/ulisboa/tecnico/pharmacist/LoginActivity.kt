package pt.ulisboa.tecnico.pharmacist

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


// possible change this to jetpack compose
class LoginActivity : AppCompatActivity() {

    // for now contacts the localhost server
    private val url = "http://" + "10.0.2.2" + ":" + 5000 + "/"
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        if (Build.VERSION.SDK_INT > 21) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }

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

        RegisterUser(
            username.getText().toString(),
            password.getText().toString()
        )
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }

    // when clicking the guest button redirect to mapsActivity
    fun guestButtonClick(view: View?) {
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }


    private fun RegisterUser(username: String, password: String) {

        // retrofit builder
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            // json converter
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

        val user = User(username, password)
        // call method that receives an int -> status from the server
        val call: Call<Int?>? = retrofitAPI.sendRegister(user)
        println(retrofit.baseUrl())
        call!!.enqueue(object : Callback<Int?> {
            // when we get response
            override fun onResponse(call: Call<Int?>, response: Response<Int?>){
                //Toast.makeText(ctx, "Data posted to API", Toast.LENGTH_SHORT).show()
                val status: Int? = response.body()
                // TODO - still not printing the right code
                //println("Status is:" + status)
            }


            override fun onFailure(call: Call<Int?>, t: Throwable) {
                // we get error response from API.
                println("Error found is : " + t.message)
            }
        })

    }

}

