package com.eeg.pronosticapp.model

// Ce que l'appli envoie au backend — l'identité de l'utilisateur vient
// désormais du jeton d'authentification (en-tête Authorization), plus
// besoin de l'envoyer dans le corps de la requête.
data class MatchAnalysisRequest(
    val equipe1: String,
    val equipe2: String,
    val typeMatch: String // ex: "championnat", "coupe", "amical", ou un nom de ligue
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
    val scoresProbables: List<ScoreProbable>,
    val vip: Boolean,
    val pronosticsRestantsAujourdhui: Int?
)

// ---- Profil / compte ----

data class ProfilResponse(
    val uid: String,
    val email: String?,
    val telephone: String?,
    val vip: Boolean,
    val vipExpireLe: String?,
    val pronosticsUtilisesAujourdhui: Int,
    val limiteQuotidienneGratuite: Int
)

data class ProfilRequest(
    val telephone: String?
)

// ---- Système VIP ----

data class VipStatutResponse(
    val vip: Boolean,
    val vipExpireLe: String?,
    val vipExpireLeAffichage: String?,
    val vipJoursRestants: Int?,
    val pronosticsUtilisesAujourdhui: Int,
    val limiteQuotidienneGratuite: Int
)

data class PaiementRequest(
    val telephone: String, // requis ici : PayGate a besoin d'un vrai numéro pour débiter
    val reseau: String // "TMONEY" ou "FLOOZ"
)

data class PaiementResponse(
    val txReference: String,
    val montant: Int,
    val dureeJours: Int
)

data class ConfirmationRequest(
    val txReference: String
)

data class ConfirmationResponse(
    val confirme: Boolean,
    val vipExpireLe: String?
)

data class ActiverCodeRequest(
    val code: String
)

data class ActiverCodeResponse(
    val active: Boolean,
    val vipExpireLe: String?
)

