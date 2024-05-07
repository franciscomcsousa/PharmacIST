package pt.ulisboa.tecnico.pharmacist

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
    }

    fun mapsButtonClick(view: View?) {
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }

}