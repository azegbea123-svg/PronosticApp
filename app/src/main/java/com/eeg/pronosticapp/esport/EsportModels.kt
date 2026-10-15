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
    val favori: Boolean = false,    // une des deux équipes est suivie côté serveur
    val pronostic: EsportPronostic? = null,   // calculé par le serveur, déjà là dans la liste (aucun clic)
    val pronostic_raison: String? = null      // pourquoi il n'y en a pas (historique insuffisant...)
)

/** Pronostic d'un match, fourni directement avec le match. */
data class EsportPronostic(
    val p1: Double,
    val pnul: Double,
    val p2: Double,
    val favori: Int,                 // 1, 2 ou 3 (nul)
    val confiance: String,           // faible | moyenne | bonne
    val gele: Boolean = true,        // figé avant le début du match (compté dans le bilan)
    val correct: Boolean? = null,    // match terminé : le favori a-t-il gagné ?
    val details: EsportDetailsElo? = null,
    val limites: List<String> = emptyList()
)

data class EsportDetailsElo(
    val elo1: Int? = null,
    val elo2: Int? = null,
    val matchs1: Int? = null,
    val matchs2: Int? = null,
    val jours_depuis_dernier_match: Int? = null
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
    val historique: EsportHistorique? = null,
    val quota: EsportQuota? = null,
    val conseil: String? = null
)

data class EsportHistorique(
    val jours_couverts: Int = 0,
    val jours_cibles: Int = 0,
    val matchs_memorises: Int = 0
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

data class EsportBilan(
    val verifies: Int = 0,
    val en_attente: Int = 0,
    val precision: Double? = null,
    val brier: Double? = null,
    val fiable: Boolean = false,
    val note: String = ""
)
