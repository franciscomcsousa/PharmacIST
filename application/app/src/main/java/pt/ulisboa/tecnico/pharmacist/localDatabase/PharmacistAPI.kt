package pt.ulisboa.tecnico.pharmacist.localDatabase

import android.app.Activity
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import okio.ByteString.Companion.decodeBase64
import pt.ulisboa.tecnico.pharmacist.utils.CreatePharmacyResponse
import pt.ulisboa.tecnico.pharmacist.utils.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.utils.FavoritePharmacy
import pt.ulisboa.tecnico.pharmacist.utils.ImageUtils
import pt.ulisboa.tecnico.pharmacist.utils.Location
import pt.ulisboa.tecnico.pharmacist.utils.Medicine
import pt.ulisboa.tecnico.pharmacist.utils.MedicineLocation
import pt.ulisboa.tecnico.pharmacist.utils.MedicineResponse
import pt.ulisboa.tecnico.pharmacist.utils.MedicineStock
import pt.ulisboa.tecnico.pharmacist.utils.NearestPharmaciesResponse
import pt.ulisboa.tecnico.pharmacist.utils.PharmaciesResponse
import pt.ulisboa.tecnico.pharmacist.utils.Pharmacy
import pt.ulisboa.tecnico.pharmacist.utils.ImageResponse
import pt.ulisboa.tecnico.pharmacist.utils.QueryStock
import pt.ulisboa.tecnico.pharmacist.utils.QueryStockResponse
import pt.ulisboa.tecnico.pharmacist.utils.RetrofitAPI
import pt.ulisboa.tecnico.pharmacist.utils.SignInResponse
import pt.ulisboa.tecnico.pharmacist.utils.StatusResponse
import pt.ulisboa.tecnico.pharmacist.utils.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.time.Instant

class PharmacistAPI(val activity: Activity) {

    // A pharmacy timestamp can only be PHARMACY_EXPIRY seconds old
    private val pharmacyExpiry = 60

    private val retrofit = Retrofit.Builder()
        .baseUrl(DataStoreManager.getUrl())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

    private val databaseHandler = DatabaseHandler(activity)

    fun sendRegister(@Body user: User?): Call<SignInResponse> {
        return retrofitAPI.sendRegisterRequest(user)
    }

    fun sendLogin(@Body user: User?): Call<SignInResponse> {
        return retrofitAPI.sendLoginRequest(user)
    }

    suspend fun getAuth(@Header("Authorization") token: String): Response<StatusResponse> {
        return retrofitAPI.getAuthRequest(token)
    }

    fun getPharmacies(@Body location: Location?, onSuccess: (List<Pharmacy>) -> Unit) {

        // Declare the database
        val database: SQLiteDatabase = databaseHandler.writableDatabase

        // Query in question
        val query = """
            SELECT * FROM pharmacies 
                ORDER BY ABS((latitude - ?)) + ABS((longitude - ?))
                ASC LIMIT 3
        """.trimIndent()

        // Local database query
        val cursor = database.rawQuery(query, arrayOf(location?.latitude.toString(), location?.longitude.toString()))
        var pharmacies = mutableListOf<Pharmacy>()

        // Iterate with cursor
        with(cursor) {
            while (moveToNext()) {
                val timestamp = getString(getColumnIndexOrThrow("timestamp"))
                if (Instant.now().epochSecond.toInt() - timestamp.toInt() > pharmacyExpiry) {
                    continue
                }
                pharmacies.add(Pharmacy(
                    getString(getColumnIndexOrThrow("pharmacy_id")),
                    getString((getColumnIndexOrThrow("name"))),
                    getString(getColumnIndexOrThrow("address")),
                    getString(getColumnIndexOrThrow("latitude")),
                    getString(getColumnIndexOrThrow("longitude")),
                    ""
                ))
            }
        }

        // If was able to receive 3 pharmacies near with a recent timestamp, execute onSuccess
        // callback with local database data
        if (pharmacies.size == 3) {
            onSuccess(pharmacies)
        }
        // If less than 3 pharmacies, fetch from remote database
        else if (pharmacies.size < 3) {
            val call: Call<PharmaciesResponse> = retrofitAPI.getPharmaciesRequest(location)
            call.enqueue(object : Callback<PharmaciesResponse> {
                override fun onResponse(
                    call: Call<PharmaciesResponse>,
                    response: Response<PharmaciesResponse>
                ) {
                    if (response.isSuccessful) {
                        val pharmaciesList = response.body()!!.pharmacies
                        pharmacies = mutableListOf()
                        for (pharmacy in pharmaciesList) {
                            // Transform pharmacies into a list of Pharmacy objects
                            pharmacies += Pharmacy(
                                pharmacy[0].toString(),
                                pharmacy[1].toString(),
                                pharmacy[2].toString(),
                                pharmacy[3].toString(),
                                pharmacy[4].toString(),
                                "")
                        }

                        // Update local database with new remotely fetched pharmacies
                        for (pharmacy in pharmaciesList) {
                            val contentValues = ContentValues().apply {
                                put("pharmacy_id", pharmacy[0].toString())
                                put("name", pharmacy[1].toString())
                                put("address", pharmacy[2].toString())
                                put("latitude", pharmacy[3].toString())
                                put("longitude", pharmacy[4].toString())
                                put("timestamp", Instant.now().epochSecond.toInt())
                            }
                            database.insertWithOnConflict("pharmacies", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
                        }
                        // Execute onSuccess callback
                        onSuccess(pharmacies)
                    }
                }
                override fun onFailure(call: Call<PharmaciesResponse>, t: Throwable) {
                    Log.d("serverResponse", "FAILED: " + t.message)
                }
            })
        }
    }

    fun createPharmacy(@Body pharmacy: Pharmacy): Call<CreatePharmacyResponse> {
        return retrofitAPI.createPharmacyRequest(pharmacy)
    }

    fun pharmacyImage(@Body id: String, onSuccess: (Bitmap) -> Unit): Call<ImageResponse> {

        var b64Image = ""
        var bitmap = ImageUtils.loadImageFromInternalStorage("P_$id", activity)

        // Uncomment this to delete all cached images
        // ImageUtils.deleteAllImagesFromInternalStorage(activity)

        if (bitmap != null) {
            // Maybe change this to a function
            println("WOOOO")
            println("USED CACHE :D")
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            onSuccess(bitmap)
        }

        else {
            val call: Call<ImageResponse> = retrofitAPI.pharmacyImageRequest(id)
            call.enqueue(object : Callback<ImageResponse> {
                override fun onResponse(
                    call: Call<ImageResponse>,
                    response: Response<ImageResponse>
                ) {
                    if (response.isSuccessful) {
                        b64Image = response.body()!!.image
                        val decodedBytes = Base64.decode(b64Image, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        ImageUtils.saveImageToInternalStorage(bitmap, "P_$id", activity)
                        onSuccess(bitmap)
                    }
                }

                override fun onFailure(call: Call<ImageResponse>, t: Throwable) {
                    Log.d("serverResponse", "FAILED: " + t.message)
                }
            })
        }

        return retrofitAPI.pharmacyImageRequest(id)
    }

    fun pharmacyFavorite(@Body favoritePharmacy: FavoritePharmacy): Call<StatusResponse> {
        return retrofitAPI.pharmacyFavoriteRequest(favoritePharmacy)
    }

    fun isPharmacyFavorite(@Body favoritePharmacy: FavoritePharmacy): Call<StatusResponse> {
        return retrofitAPI.isPharmacyFavoriteRequest(favoritePharmacy)
    }

    fun getFavoritePharmacies(@Body username: String): Call<PharmaciesResponse> {
        return retrofitAPI.getFavoritePharmaciesRequest(username)
    }

    fun createMedicine(@Body medicine: MedicineStock): Call<StatusResponse> {
        return retrofitAPI.createMedicineRequest(medicine)
    }

    fun getMedicine(@Body medicine: Medicine): Call<MedicineResponse> {
        return retrofitAPI.getMedicineRequest(medicine)
    }

    fun getMedicineById(@Query("id") medicineId: String): Call<MedicineResponse> {
        return retrofitAPI.getMedicineByIdRequest(medicineId)
    }

    fun medicineImage(@Query("id") medicineId: String): Call<ImageResponse> {
        return retrofitAPI.medicineImageRequest(medicineId)
    }

    fun getMedicineLocation(@Body medicineLocation: MedicineLocation): Call<MedicineResponse> {
        return retrofitAPI.getMedicineLocationRequest(medicineLocation)
    }

    fun getPharmacyStock(@Body queryStock: QueryStock): Call<MedicineResponse> {
        return retrofitAPI.getPharmacyStockRequest(queryStock)
    }

    fun getPharmacyStockId(
        @Query("medicineId") medicineId: String,
        @Query("pharmacyId") pharmacyId: String
    ): Call<QueryStockResponse> {
        return retrofitAPI.getPharmacyStockIdRequest(medicineId, pharmacyId)
    }

    fun nearbyPharmacyMedicine(@Body medicineLocation: MedicineLocation): Call<NearestPharmaciesResponse> {
        return retrofitAPI.nearbyPharmacyMedicineRequest(medicineLocation)
    }

    fun updateStock(@Body listMedicineStock: List<MedicineStock>): Call<StatusResponse> {
        return return retrofitAPI.updateStockRequest(listMedicineStock)
    }

}