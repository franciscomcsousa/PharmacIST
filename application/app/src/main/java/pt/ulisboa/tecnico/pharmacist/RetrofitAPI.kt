package pt.ulisboa.tecnico.pharmacist

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// Builds the requests (GET,POST, ...) with the write body to the backend
interface RetrofitAPI {

    @POST("register")
    fun sendRegister(@Body user: User?): Call<Int?>?

}