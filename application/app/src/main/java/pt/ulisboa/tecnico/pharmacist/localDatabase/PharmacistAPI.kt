package pt.ulisboa.tecnico.pharmacist.localDatabase

import android.app.Activity
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.util.Log
import pt.ulisboa.tecnico.pharmacist.utils.CreatePharmacyResponse
import pt.ulisboa.tecnico.pharmacist.utils.DataStoreManager
import pt.ulisboa.tecnico.pharmacist.utils.FavoritePharmacy
import pt.ulisboa.tecnico.pharmacist.utils.ImageResponse
import pt.ulisboa.tecnico.pharmacist.utils.ImageUtils
import pt.ulisboa.tecnico.pharmacist.utils.Location
import pt.ulisboa.tecnico.pharmacist.utils.Medicine
import pt.ulisboa.tecnico.pharmacist.utils.MedicineLocation
import pt.ulisboa.tecnico.pharmacist.utils.MedicineNotification
import pt.ulisboa.tecnico.pharmacist.utils.MedicineResponse
import pt.ulisboa.tecnico.pharmacist.utils.MedicineStock
import pt.ulisboa.tecnico.pharmacist.utils.NearestPharmaciesResponse
import pt.ulisboa.tecnico.pharmacist.utils.PharmaciesResponse
import pt.ulisboa.tecnico.pharmacist.utils.Pharmacy
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
import java.net.URL
import java.time.Instant


class PharmacistAPI(val activity: Activity) {

    // A pharmacy timestamp can only be PHARMACY_EXPIRY seconds old
    private val pharmacyExpiry = 30

    private val retrofit = createRetrofit(DataStoreManager.getUrl())
    private val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

    private val databaseHandler = DatabaseHandler(activity)

    fun canConnect(): Boolean {
        return try {
            val myUrl = URL(DataStoreManager.getUrl())
            val connection = myUrl.openConnection()
            connection.connectTimeout = 2000
            connection.connect()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun sendRegister(@Body user: User?, onSuccess: () -> Unit, onFailure: () -> Unit, onStartToken : (String) -> Unit) {
        val call = retrofitAPI.sendRegisterRequest(user)
        call.enqueue(object : Callback<SignInResponse> {
            override fun onResponse(
                call: Call<SignInResponse>,
                response: Response<SignInResponse>
            ) {
                if (response.isSuccessful) {
                    onStartToken(response.body()!!.token)
                    onSuccess()
                }
            }

            override fun onFailure(call: Call<SignInResponse>, t: Throwable) {
                onFailureHandler(t)
                onFailure()
            }
        })
    }

    fun sendLogin(@Body user: User?, onSuccess: () -> Unit, onFailure: () -> Unit, onStartToken : (String) -> Unit) {
        val call = retrofitAPI.sendLoginRequest(user)
        call.enqueue(object : Callback<SignInResponse> {
            override fun onResponse(call: Call<SignInResponse>, response: Response<SignInResponse>) {
                if (response.isSuccessful) {
                    onStartToken(response.body()!!.token)
                    onSuccess()
                }
                else {
                    onFailure()
                }
            }
            override fun onFailure(call: Call<SignInResponse>, t: Throwable) {
                onFailureHandler(t)
                onFailure()
            }
        })
    }

    suspend fun getAuth(@Header("Authorization") token: String, onSuccess: () -> Unit, onExpiry: () -> Unit) {

        val response = retrofitAPI.getAuthRequest(token)
        if (response.isSuccessful) {
            onSuccess()
        }
        else {
            onExpiry()
        }
    }

    fun getPharmacies(@Body location: Location?, onSuccess: (List<Pharmacy>) -> Unit) {

        // Declare the database
        val database: SQLiteDatabase = databaseHandler.writableDatabase

        // Query in question
        val query = """
            SELECT * FROM pharmacies 
                ORDER BY ABS((latitude - ?)) + ABS((longitude - ?))
                ASC LIMIT 15
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

        // If was able to receive pharmacies near with a recent timestamp, execute onSuccess
        // callback with local database data
        if (pharmacies.size == 4) {
            onSuccess(pharmacies)
        }
        // If less than min number of pharmacies, fetch from remote database
        else if (pharmacies.size < 4) {
            val call: Call<PharmaciesResponse> = retrofitAPI.getPharmaciesRequest(location)
            call.enqueue(object : Callback<PharmaciesResponse> {
                override fun onResponse(
                    call: Call<PharmaciesResponse>,
                    response: Response<PharmaciesResponse>
                ) {
                    if (response.isSuccessful) {
                        val pharmaciesList = response.body()!!.pharmacies
                        pharmacies = anyListToPharmacyList(pharmaciesList)

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
                    onFailureHandler(t)
                }
            })
        }
    }

    fun createPharmacy(@Body pharmacy: Pharmacy, onSuccess: () -> Unit) {
        val call = retrofitAPI.createPharmacyRequest(pharmacy)
        call.enqueue(object : Callback<CreatePharmacyResponse> {
            override fun onResponse(call: Call<CreatePharmacyResponse>, response: Response<CreatePharmacyResponse>){
                if (response.isSuccessful) {
                    onSuccess()
                }
            }

            override fun onFailure(call: Call<CreatePharmacyResponse>, t: Throwable) {
                onFailureHandler(t)
            }
        })
    }

    fun pharmacyImage(@Body id: String, dataMode: Boolean, onSuccess: (Bitmap?) -> Unit) {

        var b64Image = ""
        var bitmap = ImageUtils.loadImageFromInternalStorage("P_$id", activity)

        if (bitmap != null) {
            onSuccess(bitmap)
        }

        else {
            if (dataMode) {
                onSuccess(null)
            }
            else {
                val call: Call<ImageResponse> = retrofitAPI.pharmacyImageRequest(id)
                call.enqueue(object : Callback<ImageResponse> {
                    override fun onResponse(
                        call: Call<ImageResponse>,
                        response: Response<ImageResponse>
                    ) {
                        if (response.isSuccessful) {
                            if (response.code() == 240) {
                                onSuccess(null)
                            }
                            else {
                                b64Image = response.body()!!.image
                                val bitmap = ImageUtils.b64ImageToBitmap(b64Image)
                                ImageUtils.saveImageToInternalStorage(bitmap, "P_$id", activity)
                                onSuccess(bitmap)
                            }
                        }
                    }

                    override fun onFailure(call: Call<ImageResponse>, t: Throwable) {
                        onFailureHandler(t)
                    }
                })
            }
        }
    }

    fun pharmacyFavorite(@Body favoritePharmacy: FavoritePharmacy, onSuccess: (Int) -> Unit) {

        val call = retrofitAPI.pharmacyFavoriteRequest(favoritePharmacy)
        call.enqueue(object : Callback<StatusResponse> {
            override fun onResponse(
                call: Call<StatusResponse>,
                response: Response<StatusResponse>
            ) {
                if (response.isSuccessful) {
                    onSuccess(response.code())
                }
            }

            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                onFailureHandler(t)
            }
        })
    }

    fun isPharmacyFavorite(@Body favoritePharmacy: FavoritePharmacy, onSuccess: (Int) -> Unit) {
        val call = retrofitAPI.isPharmacyFavoriteRequest(favoritePharmacy)
        call.enqueue(object : Callback<StatusResponse> {
            override fun onResponse(
                call: Call<StatusResponse>,
                response: Response<StatusResponse>
            ) {
                if (response.isSuccessful) {
                    onSuccess(response.code())
                }
            }

            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                onFailureHandler(t)
            }
        })
    }

    fun getFavoritePharmacies(@Body username: String, onSuccess : (List<Pharmacy>) -> Unit) {

        val call: Call<PharmaciesResponse> = retrofitAPI.getFavoritePharmaciesRequest(username)
        call.enqueue(object : Callback<PharmaciesResponse> {
            override fun onResponse(
                call: Call<PharmaciesResponse>,
                response: Response<PharmaciesResponse>
            ) {
                val anyList = response.body()?.pharmacies
                val pharmaciesList = anyList?.let { anyListToPharmacyList(it) }
                if (pharmaciesList != null) {
                    onSuccess(pharmaciesList)
                }
            }

            override fun onFailure(call: Call<PharmaciesResponse>, t: Throwable) {
                onFailureHandler(t)
            }
        })
    }

    fun getNearbyPharmacies(@Body location: Location?, onSuccess: (List<Pharmacy>) -> Unit) {
        val call: Call<PharmaciesResponse> = retrofitAPI.getNearbyPharmaciesRequest(location)
        call.enqueue(object : Callback<PharmaciesResponse> {
            override fun onResponse(
                call: Call<PharmaciesResponse>,
                response: Response<PharmaciesResponse>
            ) {
                if (response.isSuccessful) {
                    val pharmaciesList = response.body()!!.pharmacies
                    val pharmacies = anyListToPharmacyList(pharmaciesList)
                    onSuccess(pharmacies)
                }
            }

            override fun onFailure(call: Call<PharmaciesResponse>, t: Throwable) {
                onFailureHandler(t)
            }
        })
    }

    fun createMedicine(@Body medicine: MedicineStock, onSuccess: () -> Unit){
        val call = retrofitAPI.createMedicineRequest(medicine)
        call.enqueue(object : Callback<StatusResponse> {
            override fun onResponse(call: Call<StatusResponse>, response: Response<StatusResponse>){
                if (response.isSuccessful) {
                    onSuccess()
                }
            }

            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                onFailureHandler(t)
            }
        })
    }

    fun getMedicine(@Body medicine: Medicine, onSuccess: (List<List<Any>>) -> Unit) {
        val call = retrofitAPI.getMedicineRequest(medicine)
        call.enqueue(object : Callback<MedicineResponse> {
            override fun onResponse(call: Call<MedicineResponse>, response: Response<MedicineResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { onSuccess(it.medicine) }
                }
            }

            override fun onFailure(call: Call<MedicineResponse>, t: Throwable) {
                onFailureHandler(t)
            }
        })
    }

    fun getMedicineById(@Query("id") medicineId: String, onSuccess: (List<List<Any>>) -> Unit, onMedicineNotFound : () -> Unit) {
        val call = retrofitAPI.getMedicineByIdRequest(medicineId)
        call.enqueue(object : Callback<MedicineResponse> {
            override fun onResponse(
                call: Call<MedicineResponse>,
                response: Response<MedicineResponse>
            ) {
                if (response.isSuccessful) {
                    onSuccess(response.body()!!.medicine)
                }
                if(response.code() == 453) {
                    onMedicineNotFound()
                }
            }

            override fun onFailure(call: Call<MedicineResponse>, t: Throwable) {
                onFailureHandler(t)
            }
        })

    }

    fun medicineImage(@Query("id") medicineId: String, dataMode: Boolean, onSuccess: (Bitmap?) -> Unit) {
        var b64Image = ""
        var bitmap = ImageUtils.loadImageFromInternalStorage("M_$medicineId", activity)

        if (bitmap != null) {
            onSuccess(bitmap)
        }

        else {
            if (dataMode) {
                onSuccess(null)
            }
            else {
                val call: Call<ImageResponse> = retrofitAPI.medicineImageRequest(medicineId)
                call.enqueue(object : Callback<ImageResponse> {
                    override fun onResponse(
                        call: Call<ImageResponse>,
                        response: Response<ImageResponse>
                    ) {
                        if (response.isSuccessful) {
                            if (response.code() == 240) {
                                onSuccess(null)
                            }
                            else {
                                b64Image = response.body()!!.image
                                val bitmap = ImageUtils.b64ImageToBitmap(b64Image)
                                ImageUtils.saveImageToInternalStorage(
                                    bitmap,
                                    "M_$medicineId",
                                    activity
                                )
                                onSuccess(bitmap)
                            }
                        }
                    }

                    override fun onFailure(call: Call<ImageResponse>, t: Throwable) {
                        onFailureHandler(t)
                    }
                })
            }
        }
    }

    fun getMedicineLocation(@Body medicineLocation: MedicineLocation, onSuccess: (List<List<Any>>) -> Unit, onMedicineNotFound: () -> Unit){
        val call = retrofitAPI.getMedicineLocationRequest(medicineLocation)
        call.enqueue(object : Callback<MedicineResponse> {
            override fun onResponse(call: Call<MedicineResponse>, response: Response<MedicineResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { onSuccess(it.medicine) }
                }
                else if (response.code() == 453) {
                    onMedicineNotFound()
                }
            }
            override fun onFailure(call: Call<MedicineResponse>, t: Throwable) {
                Log.d("serverResponse","FAILED: "+ t.message)
            }
        })
    }

    fun medicineNotification(@Body medicineNotification: MedicineNotification, onSuccess: (Int) -> Unit): Call<StatusResponse> {
        val call: Call<StatusResponse> = retrofitAPI.medicineNotificationRequest(medicineNotification)
        call.enqueue(object : Callback<StatusResponse> {
            override fun onResponse(
                call: Call<StatusResponse>,
                response: Response<StatusResponse>
            ) {
                if (response.isSuccessful) {
                    onSuccess(response.code())
                }
            }

            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                onFailureHandler(t)
            }
        })

        return retrofitAPI.medicineNotificationRequest(medicineNotification)
    }

    fun isMedicineNotification(@Query("username") username: String,
                               @Query("medicineId") medicineId: String,
                               onSuccess : (Int) -> Unit) {
        val call = retrofitAPI.isMedicineNotificationRequest(username, medicineId)
        call.enqueue(object : Callback<StatusResponse> {
            override fun onResponse(
                call: Call<StatusResponse>,
                response: Response<StatusResponse>
            ) {
                onSuccess(response.code())
            }

            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                onFailureHandler(t)
            }
        })
    }

    fun getPharmacyStock(@Body queryStock: QueryStock, onSuccess: (List<List<Any>>) -> Unit) {
        val call = retrofitAPI.getPharmacyStockRequest(queryStock)
        call.enqueue(object : Callback<MedicineResponse> {
            override fun onResponse(
                call: Call<MedicineResponse>,
                response: Response<MedicineResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { onSuccess(it.medicine) }
                }
            }

            override fun onFailure(call: Call<MedicineResponse>, t: Throwable) {
                onFailureHandler(t)
            }
        })
    }

    fun getPharmacyStockId( @Query("medicineId") medicineId: String,  @Query("pharmacyId") pharmacyId: String, onSuccess: (List<String>) -> Unit) {
        val call = retrofitAPI.getPharmacyStockIdRequest(medicineId, pharmacyId)
        call.enqueue(object : Callback<QueryStockResponse> {
            override fun onResponse(
                call: Call<QueryStockResponse>,
                response: Response<QueryStockResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { onSuccess(it.stock) }
                }
            }

            override fun onFailure(call: Call<QueryStockResponse>, t: Throwable) {
                onFailureHandler(t)
            }
        })

    }

    fun nearbyPharmacyMedicine(@Body medicineLocation: MedicineLocation, onSuccess: (List<List<Any>>) -> Unit) {
        val call = retrofitAPI.nearbyPharmacyMedicineRequest(medicineLocation)
        call.enqueue(object : Callback<NearestPharmaciesResponse> {
            override fun onResponse(
                call: Call<NearestPharmaciesResponse>,
                response: Response<NearestPharmaciesResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { onSuccess(it.pharmaciesStock) }
                }
            }

            override fun onFailure(call: Call<NearestPharmaciesResponse>, t: Throwable) {
                onFailureHandler(t)
            }
        })
    }

    fun updateStock(@Body listMedicineStock: List<MedicineStock>, onSuccess: (Int) -> Unit) {
        val call = retrofitAPI.updateStockRequest(listMedicineStock)
        call.enqueue(object : Callback<StatusResponse> {
            override fun onResponse(
                call: Call<StatusResponse>,
                response: Response<StatusResponse>
            ) {
                if (response.isSuccessful) {
                    onSuccess(response.code())
                }
            }

            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                onFailureHandler(t)
            }
        })
    }



    // Auxiliary functions
    private fun anyListToPharmacyList(anyList : List<List<Any>>) : MutableList<Pharmacy> {
        val pharmacies : MutableList<Pharmacy> = mutableListOf()
        for (pharmacy in anyList) {
            // Transform pharmacies into a list of Pharmacy objects
            pharmacies += Pharmacy(
                // This dumb typecast is unfortunately needed
                pharmacy[0].toString().toDouble().toInt().toString(),
                pharmacy[1].toString(),
                pharmacy[2].toString(),
                pharmacy[3].toString(),
                pharmacy[4].toString(),
                "")
        }
        return pharmacies
    }

    private fun onFailureHandler(t: Throwable) {
        Log.d("serverResponse","FAILED: "+ t.message)
    }

    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}