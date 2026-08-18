package com.example.pronosticapp.network

import com.example.pronosticapp.model.MatchAnalysisRequest
import com.example.pronosticapp.model.MatchAnalysisResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    // À adapter selon la route exacte définie côté backend (FastAPI)
    @POST("match/analyse")
    suspend fun analyserMatch(
        @Body request: MatchAnalysisRequest
    ): Response<MatchAnalysisResponse>
}
