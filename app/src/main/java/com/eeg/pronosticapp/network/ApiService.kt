package com.eeg.pronosticapp.network

import com.eeg.pronosticapp.model.ActiverCodeRequest
import com.eeg.pronosticapp.model.ActiverCodeResponse
import com.eeg.pronosticapp.model.ConfirmationRequest
import com.eeg.pronosticapp.model.ConfirmationResponse
import com.eeg.pronosticapp.model.MatchAnalysisRequest
import com.eeg.pronosticapp.model.MatchAnalysisResponse
import com.eeg.pronosticapp.model.PaiementRequest
import com.eeg.pronosticapp.model.PaiementResponse
import com.eeg.pronosticapp.model.ProfilRequest
import com.eeg.pronosticapp.model.ProfilResponse
import com.eeg.pronosticapp.model.VipStatutResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

// Aucun de ces appels n'a besoin de passer explicitement le jeton
// d'authentification : FirebaseAuthInterceptor (voir RetrofitClient.kt)
// l'ajoute automatiquement à chaque requête sortante.
interface ApiService {
    @POST("match/analyse")
    suspend fun analyserMatch(
        @Body request: MatchAnalysisRequest
    ): Response<MatchAnalysisResponse>

    @GET("profil")
    suspend fun obtenirProfil(): Response<ProfilResponse>

    @POST("profil")
    suspend fun majProfil(
        @Body request: ProfilRequest
    ): Response<Unit>

    @GET("vip/statut")
    suspend fun vipStatut(): Response<VipStatutResponse>

    @POST("vip/payer")
    suspend fun vipPayer(
        @Body request: PaiementRequest
    ): Response<PaiementResponse>

    @POST("vip/confirmer")
    suspend fun vipConfirmer(
        @Body request: ConfirmationRequest
    ): Response<ConfirmationResponse>

    @POST("vip/activer-code")
    suspend fun vipActiverCode(
        @Body request: ActiverCodeRequest
    ): Response<ActiverCodeResponse>

    @DELETE("compte")
    suspend fun supprimerCompte(): Response<Unit>
}
