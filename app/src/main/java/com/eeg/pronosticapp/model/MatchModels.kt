package com.eeg.pronosticapp.model

// Ce que l'appli envoie au backend
data class MatchAnalysisRequest(
    val equipe1: String,
    val equipe2: String,
    val typeMatch: String, // ex: "championnat", "coupe", "amical", ou un nom de ligue
    val telephone: String, // identifiant simple pour le suivi du quota gratuit / VIP
    val fixtureId: Long? = null // id du match dans la liste : active classement + confrontations côté serveur
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
    val pronosticsRestantsAujourdhui: Int?
)

// ---- Liste de matchs J / J+1 (nouvelle approche) ----

data class MatchItem(
    val fixture_id: Long,
    val date: String,
    val league_id: Int,
    val league: String,
    val equipe1: String, // domicile
    val equipe2: String, // extérieur
    val donneesDisponibles: Boolean?, // null = vérification en cours, pas encore cliquable
    // --- Enrichissements (absents selon la source du match : tous optionnels) ---
    val logo1: String? = null,
    val logo2: String? = null,
    val statut: String? = null,       // prevu | en_direct | termine | reporte | annule
    val score1: Int? = null,
    val score2: Int? = null,
    val minute: String? = null,       // seulement en direct, ex: "61'" ou "HT"
    val league_pays: String? = null,  // code pays (ex: "FRA") ou "INT"
    val league_logo: String? = null,
    val league_rang: Int? = null,     // plus grand = plus important ; >= 100 = championnat populaire
    val nb_sources: Int? = null       // nombre de fournisseurs ayant confirmé ce match (doublons fusionnés côté serveur)
)

data class MatchsResponse(
    val jour: String, // YYYY-MM-DD
    val matchs: List<MatchItem>
)

// ---- Contexte d'un match (GET /match/contexte) ----

data class LigneClassement(
    val equipe_id: Long?,
    val nom: String?,
    val rang: Int,
    val points: Int,
    val diff: Int,
    val joues: Int?,
    val victoires: Int?,
    val nuls: Int?,
    val defaites: Int?
)

data class ClassementDuel(
    val equipe1: LigneClassement?,
    val equipe2: LigneClassement?,
    val total_equipes: Int
)

data class BilanConfrontations(val equipe1: Int, val nuls: Int, val equipe2: Int)

data class ConfrontationPassee(
    val date: String,
    val equipe_domicile: String,
    val equipe_exterieur: String,
    val score: String,
    val competition: String?
)

data class Confrontations(
    val bilan: BilanConfrontations,
    val total: Int,
    val derniers: List<ConfrontationPassee> = emptyList()
)

data class ContexteMatch(
    val disponible: Boolean,
    val raison: String? = null,
    val classement: ClassementDuel? = null,
    val confrontations: Confrontations? = null
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
