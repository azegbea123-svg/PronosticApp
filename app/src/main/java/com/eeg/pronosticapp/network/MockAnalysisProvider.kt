package com.eeg.pronosticapp.network

import com.eeg.pronosticapp.model.MatchAnalysisResponse
import com.eeg.pronosticapp.model.ScoreProbable
import kotlin.math.abs
import kotlin.random.Random

/**
 * Génère un pronostic simulé, déterministe par paire d'équipes (même
 * saisie = même résultat, pour un rendu cohérent pendant les tests).
 *
 * Objectif : valider tout le flux de l'appli (saisie -> clic -> affichage)
 * SANS dépendre du backend. Utile pour tester l'UI sans attendre le
 * réveil du service Render, par exemple.
 */
object MockAnalysisProvider {

    // Reproduit exactement les mêmes seuils que le backend (analysis.py
    // _avis_fiabilite_v1_v2 / _avis_fiabilite_nul), pour que le mode
    // simulé reste cohérent avec le vrai comportement de l'appli.
    private fun avisV1V2(p: Double): String = when {
        p >= 0.70 -> "Fiabilité élevée sur historique récent"
        p >= 0.50 -> "Fiabilité correcte, reste un pari avec de l'incertitude"
        p >= 0.35 -> "Signal présent mais incertain"
        else -> "Peu probable sur la base de l'analyse"
    }

    private fun avisNul(p: Double): String = when {
        p >= 0.30 -> "Signal notable, mais le nul reste statistiquement le pari le plus incertain des trois"
        p >= 0.20 -> "Possibilité à surveiller, sans plus"
        else -> "Peu probable sur la base de l'analyse"
    }

    fun generer(equipe1: String, equipe2: String, typeMatch: String): MatchAnalysisResponse {
        val seed = abs((equipe1 + equipe2 + typeMatch).hashCode())
        val random = Random(seed)

        val base1 = 0.25 + random.nextDouble() * 0.4
        val nul = 0.15 + random.nextDouble() * 0.15
        val base2 = (1.0 - base1 - nul).coerceAtLeast(0.05)

        val total = base1 + nul + base2
        val p1 = base1 / total
        val pNul = nul / total
        val p2 = base2 / total

        val buts1 = 0.8 + random.nextDouble() * 2.2
        val buts2 = 0.6 + random.nextDouble() * 2.0

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
                    "resserré. (⚠️ Donnée simulée — backend non consulté.)"
        } else {
            "Match très équilibré entre $equipe1 et $equipe2 pour ce $typeMatch. " +
                    "(⚠️ Donnée simulée — backend non consulté.)"
        }

        return MatchAnalysisResponse(
            equipe1 = equipe1,
            equipe2 = equipe2,
            probabiliteVictoireEquipe1 = p1,
            probabiliteMatchNul = pNul,
            probabiliteVictoireEquipe2 = p2,
            avisFiabiliteV1 = avisV1V2(p1),
            avisFiabiliteNul = avisNul(pNul),
            avisFiabiliteV2 = avisV1V2(p2),
            facteursCles = facteurs,
            resumeAnalyse = resume,
            butsAttendusEquipe1 = buts1,
            butsAttendusEquipe2 = buts2,
            probabiliteBTTS = 0.4 + random.nextDouble() * 0.4,
            probabiliteOver05 = 0.85 + random.nextDouble() * 0.14,
            probabiliteOver15 = 0.55 + random.nextDouble() * 0.3,
            probabiliteOver25 = 0.3 + random.nextDouble() * 0.35,
            scoresProbables = listOf(
                ScoreProbable("${buts1.toInt()}-${buts2.toInt()}", 0.12),
                ScoreProbable("1-1", 0.10),
                ScoreProbable("2-1", 0.09)
            ),
            vip = false,
            pronosticsRestantsAujourdhui = 2
        )
    }
}