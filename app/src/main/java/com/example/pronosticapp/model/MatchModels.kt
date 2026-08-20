package com.example.pronosticapp.model

// Ce que l'appli envoie au backend
data class MatchAnalysisRequest(
    val equipe1: String,
    val equipe2: String,
    val typeMatch: String, // ex: "championnat", "coupe", "amical", ou un nom de ligue
    val telephone: String  // identifiant simple pour le suivi du quota gratuit / VIP
)

data class ScoreProbable(
    val score: String,
    val probabilite: Double
)

// Ce que le backend renvoie
data class MatchAnalysisResponse(
    val equipe1: String,
    val equipe2: String,
    val probabiliteVictoireEquipe1: Double,
    val probabiliteMatchNul: Double,
    val probabiliteVictoireEquipe2: Double,
    val facteursCles: List<String>,
    val resumeAnalyse: String,
    val butsAttendusEquipe1: Double,
    val butsAttendusEquipe2: Double,
    val probabiliteBTTS: Double,
    val probabiliteOver05: Double,
    val probabiliteOver15: Double,
    val probabiliteOver25: Double,
    val scoresProbables: List<ScoreProbable>
)

// ---- Système VIP ----

data class VipStatutResponse(
    val vip: Boolean,
    val vipExpireLe: String?,
    val pronosticsUtilisesAujourdhui: Int,
    val limiteQuotidienneGratuite: Int
)

data class PaiementRequest(
    val telephone: String,
    val reseau: String // "TMONEY" ou "FLOOZ"
)

data class PaiementResponse(
    val txReference: String,
    val montant: Int,
    val dureeJours: Int
)

data class ConfirmationRequest(
    val telephone: String,
    val txReference: String
)

data class ConfirmationResponse(
    val confirme: Boolean,
    val vipExpireLe: String?
)

