package com.example.pronosticapp.network

import com.example.pronosticapp.model.ConfirmationRequest
import com.example.pronosticapp.model.ConfirmationResponse
import com.example.pronosticapp.model.MatchAnalysisRequest
import com.example.pronosticapp.model.MatchAnalysisResponse
import com.example.pronosticapp.model.MatchsResponse
import com.example.pronosticapp.model.PaiementRequest
import com.example.pronosticapp.model.PaiementResponse
import com.example.pronosticapp.model.VipStatutResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("match/analyse")
    suspend fun analyserMatch(
        @Body request: MatchAnalysisRequest
    ): Response<MatchAnalysisResponse>

    // Nouvelle approche : liste des matchs du jour / J+1 (championnats
    // suivis), avec disponibilité des données déjà calculée côté
    // serveur. jour = "today" ou "tomorrow".
    @GET("matchs")
    suspend fun listerMatchs(
        @Query("jour") jour: String
    ): Response<MatchsResponse>

    @GET("vip/statut")
    suspend fun vipStatut(
        @Query("telephone") telephone: String
    ): Response<VipStatutResponse>

    @POST("vip/payer")
    suspend fun vipPayer(
        @Body request: PaiementRequest
    ): Response<PaiementResponse>

    @POST("vip/confirmer")
    suspend fun vipConfirmer(
        @Body request: ConfirmationRequest
    ): Response<ConfirmationResponse>
}
