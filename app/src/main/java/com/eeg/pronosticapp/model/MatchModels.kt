package com.eeg.pronosticapp.model

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
    val avisFiabiliteV1: String,
    val avisFiabiliteNul: String,
    val avisFiabiliteV2: String,
    val facteursCles: List<String>,
    val resumeAnalyse: String,
    val butsAttendusEquipe1: Double,
    val butsAttendusEquipe2: Double,
    val probabiliteBTTS: Double,
    val probabiliteOver05: Double,
    val probabiliteOver15: Double,
    val probabiliteOver25: Double,
    val scoresProbables: List<ScoreProbable>,
    val vip: Boolean,
    val pronosticsRestantsAujourdhui: Int?,
    val predictionExterne: String? = null,
    val sourcePredictionExterne: String? = null,
    val probabilitePredictionExterne: Double? = null,
    val confianceGlobale: Double? = null
)

// ---- Liste de matchs J / J+1 (nouvelle approche) ----

data class MatchItem(
    val fixture_id: String,
    val event_id: Int? = null,
    val date: String,
    val league_id: Int,
    val league: String,
    val equipe1: String,
    val equipe2: String,
    val donneesDisponibles: Boolean?
)

data class MatchStatRow(
    val label: String,
    val home: String,
    val away: String
)

data class H2HMatch(
    val date: String?,
    val home: String?,
    val away: String?,
    val score: String?,
    val league: String?
)

data class MatchDetailResponse(
    val fixture_id: String,
    val event_id: Int?,
    val date: String?,
    val league: String?,
    val equipe1: String,
    val equipe2: String,
    val statut: Map<String, Any>?,
    val score: List<Map<String, Any>> = emptyList(),
    val statistiques: List<Map<String, Any>> = emptyList(),
    val statistiquesTop: List<MatchStatRow> = emptyList(),
    val confrontations: Map<String, Any>?,
    val h2hRecents: List<H2HMatch> = emptyList(),
    val compositions: Map<String, Any> = emptyMap(),
    val formations: Map<String, Any> = emptyMap(),
    val detail: Map<String, Any>?
)

data class MatchsResponse(
    val jour: String, // YYYY-MM-DD
    val matchs: List<MatchItem>
)

// ---- Compte utilisateur ----

data class ProfilRequest(
    val telephone: String?
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
