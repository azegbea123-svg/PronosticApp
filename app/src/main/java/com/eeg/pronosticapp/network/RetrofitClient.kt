package com.eeg.pronosticapp.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Authentification Firebase — le backend exige un en-tête
 * "Authorization: Bearer <jeton>" sur TOUS ses endpoints (voir
 * app/auth.py côté backend), qui sait déjà vérifier un vrai jeton
 * Firebase via firebase_auth.verify_id_token, rien à changer de ce
 * côté-là.
 *
 * Ici : connexion anonyme (aucun écran de connexion, aucun mot de
 * passe utilisateur à gérer) — Firebase attribue un uid stable à
 * l'installation, réutilisé à chaque lancement tant que l'appli n'est
 * pas désinstallée. On récupère un jeton frais à chaque requête
 * (getIdToken(false) renvoie le jeton en cache s'il est encore valide,
 * sans réseau superflu — seulement un peu avant son expiration, ~1h,
 * il en redemande un nouveau automatiquement).
 *
 * ⚠️ Prérequis avant que ça fonctionne (voir le reste de la
 * conversation) :
 *   1. Un fichier google-services.json (projet Firebase de l'app,
 *      celui déjà utilisé par le backend pour Firestore) placé dans
 *      app/ à la racine du module.
 *   2. La connexion "Anonyme" activée dans la console Firebase,
 *      Authentication > Sign-in method.
 */
private class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        obtenirJetonFirebase()?.let { jeton ->
            builder.addHeader("Authorization", "Bearer $jeton")
        }
        return chain.proceed(builder.build())
    }

    /**
     * Bloquant volontairement : un intercepteur OkHttp tourne déjà sur
     * le thread réseau (jamais le thread principal tant que l'appel
     * Retrofit est fait depuis une coroutine sur Dispatchers.IO, ce qui
     * est le cas partout dans cette appli), donc Tasks.await() ici ne
     * gèle pas l'interface.
     */
    private fun obtenirJetonFirebase(): String? = try {
        val auth = FirebaseAuth.getInstance()
        val utilisateur = auth.currentUser ?: Tasks.await(auth.signInAnonymously()).user
        utilisateur?.let { Tasks.await(it.getIdToken(false)).token }
    } catch (e: Exception) {
        null // pas de connexion, Firebase mal configuré, etc. — la requête part sans jeton, le backend répondra 401 plutôt que de planter l'appli
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
