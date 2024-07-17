package com.example.imagerecogniser

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Path

interface Genai {
    @GET("/api/genai/{message}")
    fun getResponse(@Path("message") message: String): retrofit2.Call<JsonObject>
}