package com.eeg.pronosticapp.esport

/**
 * Modèles du module esport (réponses des routes « esport/… » du backend).
 * Tous les champs susceptibles d'être absents sont nullables : Gson laisse
 * simplement `null` quand une clé manque.
 */

data class EsportEquipe(
    val id: Long?,
    val nom: String,
    val code: String? = null,
    val pays: String? = null
)

data class EsportMatch(
    val id: Long,
    val custom_id: String? = null,
    val jeu: String? = null,        // identifiant du jeu, ex. "csgo"
    val jeu_nom: String? = null,    // ex. "Counter Strike"
    val tournoi: String? = null,
    val equipe1: EsportEquipe,
    val equipe2: EsportEquipe,
    val statut: String,             // prevu | en_direct | termine | reporte | annule | inconnu
    val forfait: Boolean = false,
    val ts: Long,                   // coup d'envoi, secondes UTC
    val debut: String,
    val best_of: Int? = null,
    val score1: Int? = null,        // CARTES gagnées dans la série
    val score2: Int? = null,
    val vainqueur: Int? = null,     // 1, 2 (ou 3 = nul)
    val favori: Boolean = false     // une des deux équipes est suivie côté serveur
)

data class EsportQuota(
    val limite: Int? = null,
    val restant: Int? = null,
    val reinitialisation_dans_s: Long? = null
)

data class EsportMatchsResponse(
    val matchs: List<EsportMatch> = emptyList(),
    val jeux_en_echec: List<String> = emptyList(), // jeux dont la liste n'a pas pu être chargée (quota, réseau)
    val direct_disponible: Boolean = true,
    val quota: EsportQuota? = null,
    val conseil: String? = null
)

data class EsportJeu(val jeu: String?, val nom: String?, val categorie_id: Int? = null, val equipes: Int = 0)

data class EsportJeuxResponse(
    val jeux: List<EsportJeu> = emptyList(),
    val quota: EsportQuota? = null
)

data class EsportCarte(
    val carte: String? = null,
    val vainqueur: Int? = null,
    val score1: Int? = null,
    val score2: Int? = null
)

data class EsportDuel(val victoires1: Int? = null, val victoires2: Int? = null, val nuls: Int? = null)

data class EsportVotes(val equipe1: Int? = null, val equipe2: Int? = null)

data class EsportDetail(
    val match: EsportMatch,
    val cartes: List<EsportCarte>? = null,
    val duel: EsportDuel? = null,
    val votes: EsportVotes? = null
)

data class EsportPronosticRequest(val eventId: Long)

data class EsportPronostic(
    val eventId: Long,
    val equipe1: String,
    val equipe2: String,
    val best_of: Int? = null,
    val probabiliteVictoireEquipe1: Double,
    val probabiliteMatchNul: Double,
    val probabiliteVictoireEquipe2: Double,
    val confiance: String,                      // faible | moyenne | bonne
    val facteurs: List<String> = emptyList(),
    val limites: List<String> = emptyList(),
    val deja_calcule: Boolean? = null,
    val vip: Boolean? = null,
    val pronosticsRestantsAujourdhui: Int? = null
)

data class EsportBilan(
    val verifies: Int = 0,
    val en_attente: Int = 0,
    val precision: Double? = null,
    val brier: Double? = null,
    val fiable: Boolean = false,
    val note: String = ""
)
