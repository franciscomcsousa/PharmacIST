package pt.ulisboa.tecnico.pharmacist

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// Builds the requests (GET,POST, ...) with the write body to the backend
interface RetrofitAPI {

    @POST("register")
    fun sendRegister(@Body user: User?): Call<SignInResponse>

    @POST("login")
    fun sendLogin(@Body user: User?): Call<SignInResponse>

    @POST("pharmacies")
    fun getPharmacies(@Body location: Location?): Call<PharmaciesResponse>

    @POST("create_pharmacy")
    fun createPharmacy(@Body pharmacy: Pharmacy): Call<CreatePharmacyResponse>

    @POST("upload_photo")
    fun uploadPharmacyPhoto(@Body uploadPhoto: UploadPhoto): Call<UploadPhotoResponse>
}