package com.example.pronosticapp.model

// Ce que l'appli envoie au backend
data class MatchAnalysisRequest(
    val equipe1: String,
    val equipe2: String,
    val typeMatch: String // ex: "championnat", "coupe", "amical"
)

// Ce que le backend renvoie (structure à ajuster avec l'API réelle)
data class MatchAnalysisResponse(
    val equipe1: String,
    val equipe2: String,
    val probabiliteVictoireEquipe1: Double,
    val probabiliteMatchNul: Double,
    val probabiliteVictoireEquipe2: Double,
    val facteursCles: List<String>,
    val resumeAnalyse: String
)
