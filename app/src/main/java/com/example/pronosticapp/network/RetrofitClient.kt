package com.example.pronosticapp.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * ⚠️ IMPORTANT — jeton d'authentification manquant jusqu'ici.
 *
 * Le backend exige un en-tête "Authorization: Bearer <jeton>" sur TOUS
 * ses endpoints (voir app/auth.py côté backend), y compris /match/analyse
 * déjà existant. Ce fichier RetrofitClient.kt n'envoyait jusqu'ici AUCUN
 * en-tête Authorization : chaque appel au vrai backend échouait donc
 * avec une 401, y compris avant ce changement.
 *
 * Solution TEMPORAIRE ci-dessous : utiliser le mot de passe admin comme
 * jeton (accepté par le backend comme "jeton universel", voir auth.py).
 * ⚠️ Ce n'est PAS une solution propre pour la production — le mot de
 * passe admin ne doit jamais être intégré dans l'APK (n'importe qui
 * pourrait le décompiler et l'utiliser pour accéder aux endpoints
 * admin). À remplacer par une vraie authentification Firebase
 * (Firebase Auth anonyme, par exemple) dès que possible.
 */
private object AuthToken {
    // TODO: remplacer par un vrai jeton Firebase (voir avertissement ci-dessus).
    const val JETON_TEMPORAIRE = "COLLE_TON_MOT_DE_PASSE_ADMIN_ICI"
}

private class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requeteAvecAuth = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${AuthToken.JETON_TEMPORAIRE}")
            .build()
        return chain.proceed(requeteAvecAuth)
    }
}

object RetrofitClient {

    // 🚀 PRODUCTION : backend déployé sur Render, accessible en HTTPS
    // depuis n'importe quel réseau (plus besoin que le PC local tourne).
    private const val BASE_URL = "https://pronosticapp-backend.onrender.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
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
