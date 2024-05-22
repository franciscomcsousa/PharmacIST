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

    @POST("pharmacy_image")
    fun pharmacyImage(@Body name: String): Call<PharmacyImageResponse>

    @POST("/pharmacy_favorite")
    fun pharmacyFavorite(@Body favoritePharmacy: FavoritePharmacy): Call<StatusResponse>

    @POST("/is_pharmacy_favorite")
    fun isPharmacyFavorite(@Body favoritePharmacy: FavoritePharmacy): Call<StatusResponse>

    @POST("medicine")
    fun getMedicine(@Body medicine: Medicine): Call<MedicineResponse>

    @POST("medicine_location")
    fun getMedicineLocation(@Body medicineLocation: MedicineLocation): Call<MedicineResponse>

    @POST("pharmacy_stock")
    fun getPharmacyStock(@Body queryStock: QueryStock): Call<QueryStockResponse>

    @POST("medicine_near_pharmacies")
    fun nearbyPharmacyMedicine(@Body medicineLocation: MedicineLocation): Call<NearestPharmaciesResponse>
}