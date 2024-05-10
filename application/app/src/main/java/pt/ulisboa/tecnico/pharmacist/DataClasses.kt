package pt.ulisboa.tecnico.pharmacist

import com.google.gson.annotations.SerializedName

// Stores data types for RetrofitAPI
data class User(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String)

data class Pharmacy(
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String,
    @SerializedName("latitude") val latitude: String,
    @SerializedName("longitude") val longitude: String,
    @SerializedName("image") val image: String)

data class MedicineLocation(
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double)

data class Location(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double)

// Server Responses data types for RetrofitAPI
// simplifies the way data is treated when received in onResponse
data class SignInResponse(val token: String)

data class StatusResponse(val status: Int)

data class PharmaciesResponse(val pharmacies: List<List<Any>>)

data class CreatePharmacyResponse(val status: Int)

data class PharmacyImageResponse(val image: String)

data class MedicineResponse(val medicine: String)