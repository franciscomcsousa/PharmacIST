package pt.ulisboa.tecnico.pharmacist.utils

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

// Builds the requests (GET,POST, ...) with the write body to the backend
interface RetrofitAPI {

    @POST("register")
    fun sendRegisterRequest(@Body user: User?): Call<SignInResponse>

    @POST("login")
    fun sendLoginRequest(@Body user: User?): Call<SignInResponse>

    @GET("authorized")
    suspend fun getAuthRequest(@Header("Authorization") token: String): Response<StatusResponse>

    // TODO - add the header authorization + token to all other requests in this app
    @POST("pharmacies")
    fun getPharmaciesRequest(@Body location: Location?): Call<PharmaciesResponse>

    @POST("nearby_pharmacies")
    fun getNearbyPharmaciesRequest(@Body location: Location?): Call<PharmaciesResponse>

    @POST("create_pharmacy")
    fun createPharmacyRequest(@Body pharmacy: Pharmacy): Call<CreatePharmacyResponse>

    @POST("pharmacy_image")
    fun pharmacyImageRequest(@Body id: String): Call<ImageResponse>

    @POST("pharmacy_favorite")
    fun pharmacyFavoriteRequest(@Body favoritePharmacy: FavoritePharmacy): Call<StatusResponse>

    @POST("is_pharmacy_favorite")
    fun isPharmacyFavoriteRequest(@Body favoritePharmacy: FavoritePharmacy): Call<StatusResponse>

    @POST("get_favorite_pharmacies")
    fun getFavoritePharmaciesRequest(@Body username: String): Call<PharmaciesResponse>

    @POST("medicine")
    fun getMedicineRequest(@Body medicine: Medicine): Call<MedicineResponse>

    @GET("medicine")
    fun getMedicineByIdRequest(@Query("id") medicineId: String): Call<MedicineResponse>

    @GET("medicine_image")
    fun medicineImageRequest(@Query("id") medicineId: String): Call<ImageResponse>

    @POST("create_medicine")
    fun createMedicineRequest(@Body medicine: MedicineStock): Call<StatusResponse>

    @POST("medicine_location")
    fun getMedicineLocationRequest(@Body medicineLocation: MedicineLocation): Call<MedicineResponse>

    @GET("medicine_notification")
    fun isMedicineNotificationRequest(@Query("username") username: String,
                                      @Query("medicineId") medicineId: String): Call<StatusResponse>

    @POST("medicine_notification")
    fun medicineNotificationRequest(@Body medicineNotification: MedicineNotification): Call<StatusResponse>

    @POST("pharmacy_stock")
    fun getPharmacyStockRequest(@Body queryStock: QueryStock): Call<MedicineResponse>

    @GET("pharmacy_stock")
    fun getPharmacyStockIdRequest(
        @Query("medicineId") medicineId: String,
        @Query("pharmacyId") pharmacyId: String
    ): Call<QueryStockResponse>

    @POST("medicine_near_pharmacies")
    fun nearbyPharmacyMedicineRequest(@Body medicineLocation: MedicineLocation): Call<NearestPharmaciesResponse>
    @POST("update_stock")
    fun updateStockRequest(@Body listMedicineStock: List<MedicineStock>): Call<StatusResponse>
}