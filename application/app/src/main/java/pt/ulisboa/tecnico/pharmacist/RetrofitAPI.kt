package pt.ulisboa.tecnico.pharmacist

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// Builds the requests (GET,POST, ...) with the write body to the backend
interface RetrofitAPI {

    @POST("register")
    fun sendRegister(@Body user: User?): Call<SignInResponse>

    @POST("login")
    fun sendLogin(@Body user: User?): Call<SignInResponse>

    @GET("authorized")
    suspend fun getAuth(@Header("Authorization") token: String): Response<StatusResponse>

    // TODO - add the header authorization + token to all other requests in this app
    @POST("pharmacies")
    fun getPharmacies(@Body location: Location?): Call<PharmaciesResponse>

    @POST("create_pharmacy")
    fun createPharmacy(@Body pharmacy: Pharmacy): Call<CreatePharmacyResponse>

    @POST("upload_photo")
    fun uploadPharmacyPhoto(@Body uploadPhoto: UploadPhoto): Call<UploadPhotoResponse>
}