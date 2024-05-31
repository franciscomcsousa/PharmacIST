package pt.ulisboa.tecnico.pharmacist.utils

import android.app.Activity
import com.google.android.material.snackbar.Snackbar

fun Activity.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(findViewById(android.R.id.content), message, duration).show()
}