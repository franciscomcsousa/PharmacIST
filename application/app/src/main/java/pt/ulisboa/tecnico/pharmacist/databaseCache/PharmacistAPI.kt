package pt.ulisboa.tecnico.pharmacist.databaseCache

import pt.ulisboa.tecnico.pharmacist.utils.CreatePharmacyResponse
import pt.ulisboa.tecnico.pharmacist.utils.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.utils.FavoritePharmacy
import pt.ulisboa.tecnico.pharmacist.utils.Location
import pt.ulisboa.tecnico.pharmacist.utils.Medicine
import pt.ulisboa.tecnico.pharmacist.utils.MedicineLocation
import pt.ulisboa.tecnico.pharmacist.utils.MedicineResponse
import pt.ulisboa.tecnico.pharmacist.utils.MedicineStock
import pt.ulisboa.tecnico.pharmacist.utils.NearestPharmaciesResponse
import pt.ulisboa.tecnico.pharmacist.utils.PharmaciesResponse
import pt.ulisboa.tecnico.pharmacist.utils.Pharmacy
import pt.ulisboa.tecnico.pharmacist.utils.PharmacyImageResponse
import pt.ulisboa.tecnico.pharmacist.utils.QueryStock
import pt.ulisboa.tecnico.pharmacist.utils.QueryStockResponse
import pt.ulisboa.tecnico.pharmacist.utils.RetrofitAPI
import pt.ulisboa.tecnico.pharmacist.utils.SignInResponse
import pt.ulisboa.tecnico.pharmacist.utils.StatusResponse
import pt.ulisboa.tecnico.pharmacist.utils.User
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

class PharmacistAPI {

    protected val retrofit = Retrofit.Builder()
        .baseUrl(DataStoreManager.getUrl())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    protected val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

    fun sendRegister(@Body user: User?): Call<SignInResponse> {
        return retrofitAPI.sendRegister(user)
    }

    fun sendLogin(@Body user: User?): Call<SignInResponse> {
        return retrofitAPI.sendLogin(user)
    }

    suspend fun getAuth(@Header("Authorization") token: String): Response<StatusResponse> {
        return retrofitAPI.getAuth(token)
    }

    // TODO - add the header authorization + token to all other requests in this app
    fun getPharmacies(@Body location: Location?): Call<PharmaciesResponse> {
        return retrofitAPI.getPharmacies(location)
    }

    fun createPharmacy(@Body pharmacy: Pharmacy): Call<CreatePharmacyResponse> {
        return retrofitAPI.createPharmacy(pharmacy)
    }

    fun pharmacyImage(@Body name: String): Call<PharmacyImageResponse> {
        return retrofitAPI.pharmacyImage(name)
    }

    fun pharmacyFavorite(@Body favoritePharmacy: FavoritePharmacy): Call<StatusResponse> {
        return retrofitAPI.pharmacyFavorite(favoritePharmacy)
    }

    fun isPharmacyFavorite(@Body favoritePharmacy: FavoritePharmacy): Call<StatusResponse> {
        return retrofitAPI.isPharmacyFavorite(favoritePharmacy)
    }

    fun getFavoritePharmacies(@Body username: String): Call<PharmaciesResponse> {
        return retrofitAPI.getFavoritePharmacies(username)
    }

    fun getMedicine(@Body medicine: Medicine): Call<MedicineResponse> {
        return retrofitAPI.getMedicine(medicine)
    }

    fun getMedicineById(@Query("id") medicineId: String): Call<MedicineResponse> {
        return retrofitAPI.getMedicineById(medicineId)
    }

    fun getMedicineLocation(@Body medicineLocation: MedicineLocation): Call<MedicineResponse> {
        return retrofitAPI.getMedicineLocation(medicineLocation)
    }

    fun getPharmacyStock(@Body queryStock: QueryStock): Call<QueryStockResponse> {
        return retrofitAPI.getPharmacyStock(queryStock)
    }

    fun getPharmacyStockId(
        @Query("medicineId") medicineId: String,
        @Query("pharmacyId") pharmacyId: String
    ): Call<QueryStockResponse> {
        return retrofitAPI.getPharmacyStockId(medicineId, pharmacyId)
    }

    fun nearbyPharmacyMedicine(@Body medicineLocation: MedicineLocation): Call<NearestPharmaciesResponse> {
        return retrofitAPI.nearbyPharmacyMedicine(medicineLocation)
    }

    fun updateStock(@Body listMedicineStock: List<MedicineStock>): Call<StatusResponse> {
        return return retrofitAPI.updateStock(listMedicineStock)
    }

}