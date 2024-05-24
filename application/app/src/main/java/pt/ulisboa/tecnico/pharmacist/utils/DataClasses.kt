package pt.ulisboa.tecnico.pharmacist.utils

import com.google.gson.annotations.SerializedName

// Stores data types for RetrofitAPI
data class User(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String)

data class ImagelessPharmacy(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String,
    @SerializedName("latitude") val latitude: String,
    @SerializedName("longitude") val longitude: String)

data class Pharmacy(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String,
    @SerializedName("latitude") val latitude: String,
    @SerializedName("longitude") val longitude: String,
    @SerializedName("image") val image: String)

data class FavoritePharmacy(
    @SerializedName("username") val username: String,
    @SerializedName("pharmacyId") val pharmacyId: String)

data class Medicine(
    @SerializedName("name") val name: String)

// Used to manage the stock in the AddStock and Purchase
data class MedicineStock(
    val id: String,
    val name: String,
    val purpose: String? = null, // used when creating new medicine
    val pharmacyId: String? = null,
    var stock: Int = 1,
    var maxStock: Int? = null,
    val image: String? = "") // used when buying


data class MedicinePurpose(
    @SerializedName("name") val name: String,
    @SerializedName("purpose") val purpose: String)

data class MedicineLocation(
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double)

data class Location(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double)

data class QueryStock(
    @SerializedName("substring") val substring: String,
    @SerializedName("pharmacyId") val pharmacyId: String)

data class PharmacyStock(
    @SerializedName("pharmacyName") val pharmacyName: String,
    @SerializedName("pharmacyId") val pharmacyId: String,
    @SerializedName("stock") val stock: Int)


// Used for medicine search recycler view
data class MedicineSearchViewModel(val image: Int, val text: String)

data class PharmacyStockViewModel(val name: String, val stock: Int)

// Server Responses data types for RetrofitAPI
// simplifies the way data is treated when received in onResponse
data class SignInResponse(val token: String)

data class StatusResponse(val status: Int)

data class PharmaciesResponse(val pharmacies: List<List<Any>>)

data class CreatePharmacyResponse(val status: Int)

data class PharmacyImageResponse(val image: String)

data class QueryStockResponse(val stock: List<String>)

data class MedicineResponse(val medicine: List<List<Any>>)

data class NearestPharmaciesResponse(val pharmaciesStock: List<List<Any>>)