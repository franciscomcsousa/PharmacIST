package pt.ulisboa.tecnico.pharmacist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val dataStore = DataStoreManager(application)

    fun setToken(token: String) {
        viewModelScope.launch {
            dataStore.setToken(token)
        }
    }

    val getToken = dataStore.getToken().asLiveData(Dispatchers.IO)
}