package com.example.pronosticapp.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // 🧪 TEST LOCAL (émulateur) : 10.0.2.2 est l'adresse spéciale par laquelle
    // l'émulateur Android atteint le "localhost" de ta machine. Ton serveur
    // FastAPI doit tourner en local (uvicorn app.main:app --reload) pour que
    // ça fonctionne.
    //
    // ⚠️ Si tu testes sur un téléphone physique (pas l'émulateur), remplace
    // par l'IP locale de ton PC sur le réseau Wi-Fi (ex: "http://192.168.1.42:8000/",
    // trouvable avec `ipconfig` sous Windows) — 10.0.2.2 ne fonctionne QUE
    // pour l'émulateur.
    //
    // 🚀 Une fois le backend déployé (Railway/Render/etc.), remplace cette
    // ligne par l'URL publique en HTTPS, ex: "https://ton-backend.up.railway.app/"
    private const val BASE_URL = "http://10.0.2.2:8000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
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
