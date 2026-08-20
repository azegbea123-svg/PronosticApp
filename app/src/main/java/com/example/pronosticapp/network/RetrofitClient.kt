package com.example.pronosticapp.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 🚀 PRODUCTION : backend déployé sur Render, accessible en HTTPS
    // depuis n'importe quel réseau (plus besoin que le PC local tourne).
    private const val BASE_URL = "https://pronosticapp-backend.onrender.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        // ⏱️ Délais généreux : le plan gratuit de Render peut mettre
        // 30-50s à "réveiller" le service après une période d'inactivité,
        // et une analyse de match peut elle-même prendre plusieurs
        // secondes (recherche sur plusieurs sources + confrontation
        // directe). Des délais trop courts (défaut OkHttp : 10s)
        // provoquaient des erreurs réseau qui n'en étaient pas vraiment.
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
