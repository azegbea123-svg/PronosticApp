package com.example.pronosticapp.network

import com.example.pronosticapp.model.MatchAnalysisResponse
import kotlin.math.abs
import kotlin.random.Random

/**
 * Génère un pronostic simulé, déterministe par paire d'équipes (même
 * saisie = même résultat, pour un rendu cohérent pendant les tests).
 *
 * Objectif : valider tout le flux de l'appli (saisie -> clic -> affichage)
 * SANS dépendre du backend. Une fois le backend prêt, il suffira de mettre
 * RetrofitClient.USE_MOCK_DATA à false dans MainActivity.
 */
object MockAnalysisProvider {

    fun generer(equipe1: String, equipe2: String, typeMatch: String): MatchAnalysisResponse {
        val seed = abs((equipe1 + equipe2 + typeMatch).hashCode())
        val random = Random(seed)

        val base1 = 0.25 + random.nextDouble() * 0.4  // entre 0.25 et 0.65
        val nul = 0.15 + random.nextDouble() * 0.15    // entre 0.15 et 0.30
        val base2 = (1.0 - base1 - nul).coerceAtLeast(0.05)

        val total = base1 + nul + base2
        val p1 = base1 / total
        val pNul = nul / total
        val p2 = base2 / total

        val facteurs = listOf(
            "Forme récente sur les 5 derniers matchs",
            "Confrontations directes historiques",
            "Absences et blessures notables",
            "Position actuelle au classement"
        )

        val favori = when {
            p1 > p2 && p1 > pNul -> equipe1
            p2 > p1 && p2 > pNul -> equipe2
            else -> null
        }

        val resume = if (favori != null) {
            "$favori part légèrement favori pour ce $typeMatch, mais l'écart reste " +
                "resserré. Les facteurs clés ci-dessous méritent d'être suivis avant le coup d'envoi."
        } else {
            "Match très équilibré entre $equipe1 et $equipe2 pour ce $typeMatch, sans favori net " +
                "selon les indicateurs disponibles."
        }

        return MatchAnalysisResponse(
            equipe1 = equipe1,
            equipe2 = equipe2,
            probabiliteVictoireEquipe1 = p1,
            probabiliteMatchNul = pNul,
            probabiliteVictoireEquipe2 = p2,
            facteursCles = facteurs,
            resumeAnalyse = "$resume (⚠️ Donnée simulée — backend non encore connecté.)"
        )
    }
}
