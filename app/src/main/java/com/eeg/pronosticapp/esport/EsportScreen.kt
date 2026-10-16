package com.eeg.pronosticapp.esport

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.eeg.pronosticapp.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Onglet "Esport" (module AJOUTÉ — l'écran football n'est pas modifié).
 *
 * Données : matchs des équipes SUIVIES côté serveur (en direct, à venir,
 * résultats des 7 derniers jours), filtrables par jeu. L'API esport du
 * serveur est limitée (plan gratuit : 50 requêtes/jour) : toutes les listes
 * sont mises en cache 3 h côté serveur, aucun rafraîchissement automatique
 * n'est fait ici, et le bandeau orange prévient quand le quota devient bas.
 */
@Composable
fun EsportScreen() {
    val scope = rememberCoroutineScope()

    var jeux by remember { mutableStateOf<List<EsportJeu>>(emptyList()) }
    var jeuChoisi by remember { mutableStateOf<String?>(null) }
    var matchs by remember { mutableStateOf<List<EsportMatch>>(emptyList()) }
    var quota by remember { mutableStateOf<EsportQuota?>(null) }
    var conseil by remember { mutableStateOf<String?>(null) }
    var statutChoisi by remember { mutableStateOf(FiltreStatut.TOUS) }
    var bilan by remember { mutableStateOf<EsportBilan?>(null) }
    var historique by remember { mutableStateOf<EsportHistorique?>(null) }
    var chargement by remember { mutableStateOf(true) }
    var erreur by remember { mutableStateOf<String?>(null) }
    var selection by remember { mutableStateOf<EsportMatch?>(null) }

    suspend fun charger() {
        chargement = true
        erreur = null
        try {
            val r = EsportClient.api.matchs()
            if (r.isSuccessful) {
                val corps = r.body()
                matchs = corps?.matchs ?: emptyList()
                quota = corps?.quota
                conseil = corps?.conseil
                historique = corps?.historique
            } else {
                erreur = EsportClient.messageErreur(r)
            }
            val j = EsportClient.api.jeux()
            if (j.isSuccessful) jeux = j.body()?.jeux ?: emptyList()
        } catch (e: Exception) {
            erreur = "Erreur réseau : ${e.message}"
        } finally {
            chargement = false
        }
        // Bilan en dernier : il peut déclencher quelques vérifications côté serveur.
        try {
            val b = EsportClient.api.bilan()
            if (b.isSuccessful) bilan = b.body()
        } catch (_: Exception) { /* informatif seulement */ }
    }

    LaunchedEffect(Unit) { charger() }

    val duJeu = remember(matchs, jeuChoisi) { matchs.filter { jeuChoisi == null || it.jeu == jeuChoisi } }
    val visibles = remember(duJeu, statutChoisi) { duJeu.filter { statutChoisi.accepte(it) } }

    Column(modifier = Modifier.fillMaxSize().background(StadiumNight)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Pastille("Tous", jeuChoisi == null) { jeuChoisi = null }
                jeux.forEach { j ->
                    Pastille(j.nom ?: j.jeu ?: "?", jeuChoisi == j.jeu) { jeuChoisi = j.jeu }
                }
            }
            Text(
                "Actualiser",
                color = FloodlightGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { scope.launch { charger() } }.padding(4.dp)
            )
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FiltreStatut.values().forEach { f ->
                Pastille(
                    if (f == FiltreStatut.TOUS) f.libelle else "${f.libelle} ${duJeu.count { f.accepte(it) }}",
                    statutChoisi == f
                ) { statutChoisi = f }
            }
        }

        quota?.restant?.let { restant ->
            if (restant <= 15) {
                Text(
                    "⚠️ Quota de l'API esport bas : $restant requête(s) restante(s) aujourd'hui.",
                    color = EmberOrange,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp)
                )
            }
        }
        bilan?.takeIf { it.verifies > 0 || it.en_attente > 0 }?.let { b -> BandeauBilan(b) }
        historique?.let { h ->
            if (!h.amorcage_termine) {
                Text(
                    "Préparation des pronostics : ${h.matchs_memorises} matchs mémorisés. Le serveur récupère l'historique à chaque actualisation ; les pronostics apparaissent au fur et à mesure.",
                    color = EmberOrange,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp)
                )
            } else if (h.jours_couverts < h.jours_cibles) {
                Text(
                    "Historique : ${h.jours_couverts}/${h.jours_cibles} jours mémorisés. La fiabilité des pronostics grandit chaque jour.",
                    color = TextFaint,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp)
                )
            }
        }
        when {
            chargement -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = FloodlightGold)
            }
            erreur != null -> Column(
                Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(erreur ?: "", color = EmberOrange, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { scope.launch { charger() } },
                    colors = ButtonDefaults.buttonColors(containerColor = FloodlightGold, contentColor = TextOnAccent)
                ) { Text("Réessayer", fontWeight = FontWeight.Bold) }
            }
            visibles.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                Text(
                    conseil ?: if (duJeu.isEmpty()) "Aucun match en direct, à venir ou récent pour le moment."
                    else "Aucun match ne correspond à ce filtre.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibles, key = { it.id }) { m -> CarteEsport(m) { selection = m } }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }

    selection?.let { m ->
        Dialog(onDismissRequest = { selection = null }) { DialogueEsport(m) { selection = null } }
    }
}

// --------------------------------------------------------------------------
// Présentation
// --------------------------------------------------------------------------

private enum class FiltreStatut(val libelle: String) {
    TOUS("Tous"), DIRECT("En direct"), A_VENIR("À venir"), TERMINES("Terminés");

    fun accepte(m: EsportMatch): Boolean = when (this) {
        TOUS -> true
        DIRECT -> m.statut == "en_direct"
        A_VENIR -> m.statut == "prevu" || m.statut == "inconnu"
        TERMINES -> m.statut == "termine"
    }
}

private fun heureLocale(tsSecondes: Long): String =
    SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(tsSecondes * 1000))

private fun initiales(nom: String): String =
    nom.split(" ", "-").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "?" }

@Composable
private fun Pastille(label: String, actif: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (actif) TextPrimary else PanelNight)
            .border(1.dp, if (actif) TextPrimary else PitchLine, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (actif) PanelNight else TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
private fun BandeauBilan(b: EsportBilan) {
    val texte = if (b.verifies == 0) {
        "Bilan : ${b.en_attente} pronostic(s) en attente de vérification."
    } else {
        "Bilan : ${b.verifies} vérifié(s) · ${((b.precision ?: 0.0) * 100).toInt()} % de réussite" +
            if (b.fiable) "" else " (échantillon trop petit pour conclure)"
    }
    Text(
        texte,
        color = TextFaint,
        fontSize = 11.sp,
        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp)
    )
}

@Composable
private fun PastilleDirect() {
    val transition = rememberInfiniteTransition(label = "direct")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(Modifier.size(7.dp).alpha(alpha).clip(CircleShape).background(LiveRed))
}

@Composable
private fun Ecusson(nom: String, taille: Int = 28) {
    // L'API n'expose pas les logos sans en-tête d'authentification (et chacun coûterait
    // une requête du quota) : on affiche les initiales de l'équipe.
    Box(
        modifier = Modifier.size(taille.dp).clip(CircleShape).background(PanelRaised)
            .border(1.dp, PitchLine, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(initiales(nom), color = TextPrimary, fontSize = (taille / 2.6).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LigneEquipeEsport(nom: String, score: Int?, gagnant: Boolean, couleurScore: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Ecusson(nom)
        Spacer(Modifier.width(8.dp))
        Text(
            nom,
            color = TextPrimary,
            fontWeight = if (gagnant) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (score != null) Text("$score", color = couleurScore, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun CarteEsport(m: EsportMatch, onClick: () -> Unit) {
    val enDirect = m.statut == "en_direct"
    val termine = m.statut == "termine"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelNight)
            .border(1.dp, if (enDirect) LiveRed.copy(alpha = 0.35f) else PitchLine, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.width(62.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (m.statut) {
                "en_direct" -> {
                    PastilleDirect()
                    Spacer(Modifier.height(3.dp))
                    Text("EN DIRECT", color = LiveRed, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
                "termine" -> Text(if (m.forfait) "Forfait" else "Terminé", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                "reporte" -> Text("Reporté", color = EmberOrange, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                "annule" -> Text("Annulé", color = EmberOrange, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                else -> Text(heureLocale(m.ts), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                (if (m.favori) "★ " else "") + listOfNotNull(m.jeu_nom, m.tournoi).joinToString(" · "),
                color = TextFaint,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            LigneEquipeEsport(m.equipe1.nom, m.score1, termine && m.vainqueur == 1, if (enDirect) LiveRed else TextPrimary)
            LigneEquipeEsport(m.equipe2.nom, m.score2, termine && m.vainqueur == 2, if (enDirect) LiveRed else TextPrimary)
            LignePronostic(m)
        }
        m.best_of?.let {
            Spacer(Modifier.width(8.dp))
            Text("BO$it", color = FloodlightGoldDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun poidsBarre(p: Double): Float = p.toFloat().coerceAtLeast(0.04f)

private fun couleurConfiance(confiance: String): Color = when (confiance) {
    "bonne" -> TurfGreen
    "moyenne" -> FloodlightGold
    else -> EmberOrange
}

private fun nomFavori(m: EsportMatch, favori: Int): String = when (favori) {
    1 -> m.equipe1.nom
    2 -> m.equipe2.nom
    else -> "Nul"
}

/** Pronostic affiché directement sous les équipes : aucun clic, aucune requête de l'appli. */
@Composable
private fun LignePronostic(m: EsportMatch) {
    val p = m.pronostic
    if (p == null) {
        if (m.pronostic_raison != null && (m.statut == "prevu" || m.statut == "inconnu")) {
            Text("Pronostic en préparation : pas assez de matchs connus pour ces équipes", color = TextMuted, fontSize = 11.sp)
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${(p.p1 * 100).toInt()} %",
                color = if (p.favori == 1) TurfGreen else TextMuted,
                fontWeight = if (p.favori == 1) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
            )
            Spacer(Modifier.width(8.dp))
            Row(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))) {
                Box(Modifier.weight(poidsBarre(p.p1)).fillMaxHeight().background(TurfGreen))
                if (p.pnul > 0.0) Box(Modifier.weight(poidsBarre(p.pnul)).fillMaxHeight().background(NeutralDraw))
                Box(Modifier.weight(poidsBarre(p.p2)).fillMaxHeight().background(EmberOrange))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "${(p.p2 * 100).toInt()} %",
                color = if (p.favori == 2) EmberOrange else TextMuted,
                fontWeight = if (p.favori == 2) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
            )
        }
        val verdict = when (p.correct) {
            true -> " · ✓ pronostic juste"
            false -> " · ✗ pronostic raté"
            null -> ""
        }
        Text(
            "Favori : ${nomFavori(m, p.favori)} · confiance ${p.confiance}$verdict",
            color = couleurConfiance(p.confiance),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BarreProbabilitesEsport(p1: Double, pNul: Double, p2: Double) {
    fun poids(p: Double) = p.toFloat().coerceAtLeast(0.04f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
            Box(Modifier.weight(poids(p1)).fillMaxHeight().background(TurfGreen))
            if (pNul > 0.0) Box(Modifier.weight(poids(pNul)).fillMaxHeight().background(NeutralDraw))
            Box(Modifier.weight(poids(p2)).fillMaxHeight().background(EmberOrange))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${(p1 * 100).toInt()} %", color = TurfGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (pNul > 0.0) Text("Nul ${(pNul * 100).toInt()} %", color = NeutralDraw, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("${(p2 * 100).toInt()} %", color = EmberOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DialogueEsport(initial: EsportMatch, onFermer: () -> Unit) {
    val scope = rememberCoroutineScope()
    var match by remember { mutableStateOf(initial) }
    var detail by remember { mutableStateOf<EsportDetail?>(null) }
    var erreur by remember { mutableStateOf<String?>(null) }
    var occupe by remember { mutableStateOf(false) }

    suspend fun actualiser(avec: String) {
        occupe = true
        erreur = null
        try {
            val r = EsportClient.api.detail(match.id, avec)
            if (r.isSuccessful) {
                r.body()?.let { d ->
                    // Le détail ne contient pas le pronostic : on conserve celui de la liste.
                    match = d.match.copy(
                        favori = match.favori,
                        pronostic = match.pronostic,
                        pronostic_raison = match.pronostic_raison
                    )
                    detail = d
                }
            } else erreur = EsportClient.messageErreur(r)
        } catch (e: Exception) {
            erreur = "Erreur réseau : ${e.message}"
        } finally {
            occupe = false
        }
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PanelNight)
            .border(1.dp, PitchLine, RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Ecusson(match.equipe1.nom, 44)
                Spacer(Modifier.height(4.dp))
                Text(match.equipe1.nom, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(
                if (match.score1 != null && match.score2 != null) "${match.score1} - ${match.score2}" else "VS",
                color = if (match.statut == "en_direct") LiveRed else FloodlightGold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Ecusson(match.equipe2.nom, 44)
                Spacer(Modifier.height(4.dp))
                Text(match.equipe2.nom, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(
            listOfNotNull(match.jeu_nom, match.tournoi, match.best_of?.let { "BO$it" }, heureLocale(match.ts)).joinToString(" · "),
            color = TextFaint, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            match.pronostic?.let { p ->
                BarreProbabilitesEsport(p.p1, p.pnul, p.p2)
                Text(
                    "Favori : ${nomFavori(match, p.favori)} · confiance ${p.confiance}" +
                        (if (p.gele) " · pronostic figé avant le match" else " · calculé pendant le match (non compté au bilan)"),
                    color = couleurConfiance(p.confiance), fontWeight = FontWeight.Bold, fontSize = 12.sp
                )
                p.details?.let { d ->
                    Text(
                        "Classement Elo : ${d.elo1 ?: 0} contre ${d.elo2 ?: 0} · ${d.matchs1 ?: 0} et ${d.matchs2 ?: 0} matchs analysés",
                        color = TextMuted, fontSize = 12.sp
                    )
                }
                p.limites.forEach { Text("ℹ️ $it", color = TextFaint, fontSize = 11.sp) }
            }
            if (match.pronostic == null && match.pronostic_raison != null) {
                Text(match.pronostic_raison ?: "", color = TextFaint, fontSize = 12.sp)
            }
            detail?.cartes?.takeIf { it.isNotEmpty() }?.let { cartes ->
                Text("CARTES", color = TextFaint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                cartes.forEach { c ->
                    Text("${c.carte ?: "?"} : ${c.score1 ?: "-"} - ${c.score2 ?: "-"}", color = TextMuted, fontSize = 12.sp)
                }
            }
            detail?.duel?.takeIf { it.victoires1 != null }?.let { d ->
                Text("Confrontations : ${d.victoires1} - ${d.victoires2}", color = TextMuted, fontSize = 12.sp)
            }
            detail?.votes?.takeIf { it.equipe1 != null }?.let { v ->
                Text("Votes du public : ${v.equipe1} - ${v.equipe2}", color = TextMuted, fontSize = 12.sp)
            }
            erreur?.let { Text(it, color = EmberOrange, fontSize = 13.sp) }
            if (occupe) {
                CircularProgressIndicator(color = FloodlightGold, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        OutlinedButton(
            onClick = { scope.launch { actualiser(if (detail == null) "cartes,duel" else "") } },
            enabled = !occupe,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (match.statut == "en_direct") "Actualiser le score" else if (detail == null) "Cartes et confrontations" else "Actualiser",
                color = TextPrimary
            )
        }
        TextButton(onClick = onFermer, modifier = Modifier.fillMaxWidth()) { Text("Fermer", color = TextMuted) }
    }
}
