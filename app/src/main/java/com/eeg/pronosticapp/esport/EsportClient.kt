package com.eeg.pronosticapp.esport

import com.eeg.pronosticapp.network.FirebaseAuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** Endpoints /esport/* du backend (AJOUT : ApiService existant non modifié). */
interface EsportApi {
    @GET("esport/jeux")
    suspend fun jeux(): Response<EsportJeuxResponse>

    // Mise en cache 3 h côté serveur : appeler souvent ne coûte rien en quota API.
    @GET("esport/matchs")
    suspend fun matchs(@Query("jeu") jeu: String? = null): Response<EsportMatchsResponse>

    // avec = "cartes,duel,votes" (chacun coûte 1 appel API côté serveur) ; vide = match seul.
    @GET("esport/match/{id}")
    suspend fun detail(@Path("id") id: Long, @Query("avec") avec: String = ""): Response<EsportDetail>

    @POST("esport/pronostic")
    suspend fun pronostic(@Body requete: EsportPronosticRequest): Response<EsportPronostic>

    @GET("esport/bilan")
    suspend fun bilan(): Response<EsportBilan>
}

/**
 * Client dédié au module esport : même serveur et même authentification
 * Firebase que RetrofitClient, mais sans le modifier. Journal limité aux
 * lignes de requête (BASIC) : le niveau BODY de l'autre client écrit aussi
 * le jeton d'authentification dans Logcat.
 */
object EsportClient {
    // Doit rester identique à RetrofitClient.BASE_URL (privé, donc répété ici).
    private const val BASE_URL = "https://pronosticapp-backend.onrender.com/"

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(FirebaseAuthInterceptor())
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: EsportApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EsportApi::class.java)
    }

    /** Message lisible d'une réponse en erreur : le "detail" renvoyé par FastAPI, sinon le code HTTP. */
    fun messageErreur(reponse: Response<*>): String {
        val detail = try {
            reponse.errorBody()?.string()?.let { JSONObject(it).optString("detail") }
        } catch (e: Exception) {
            null
        }
        return if (detail.isNullOrBlank()) "Erreur serveur : ${reponse.code()}" else detail
    }
}
