package com.example.myapplication.model

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface SupabaseStorageService {
    @Multipart
    @POST("storage/v1/object/{bucket}/{fileName}")
    fun uploadImage(
        @Header("Authorization") authHeader: String,
        @Path("bucket") bucket: String,
        @Path("fileName") fileName: String,
        @Part file: MultipartBody.Part
    ): Call<ResponseBody>
    @DELETE("storage/v1/object/{bucket}/{filePath}")
    fun deleteImage(
        @Header("Authorization") authHeader: String,
        @Path("bucket") bucket: String,
        @Path(value = "filePath", encoded = true) filePath: String
    ): Call<ResponseBody>
}