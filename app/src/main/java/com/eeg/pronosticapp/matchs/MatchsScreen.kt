package com.eeg.pronosticapp.matchs

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.stickyHeader
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.eeg.pronosticapp.data.PreferencesTelephone
import com.eeg.pronosticapp.model.ContexteMatch
import com.eeg.pronosticapp.model.LigneClassement
import com.eeg.pronosticapp.model.MatchAnalysisRequest
import com.eeg.pronosticapp.model.MatchAnalysisResponse
import com.eeg.pronosticapp.model.MatchItem
import com.eeg.pronosticapp.network.RetrofitClient
import com.eeg.pronosticapp.ui.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Écran "Matchs du jour".
 *
 * Rendu : matchs REGROUPÉS PAR CHAMPIONNAT (les plus importants en tête —
 * championnats populaires d'abord, via league_rang), en-tête collant avec
 * logo + nom + pays, logos d'équipes, score et minute en direct, heure
 * locale du téléphone (le serveur renvoie de l'UTC), filtres
 * Tous / En direct / Analysables et recherche par équipe ou championnat.
 *
 * donneesDisponibles garde ses trois états :
 *   - null  : vérification BeSoccer en cours côté serveur — pas cliquable ;
 *   - true  : cliquable (sauf match terminé/reporté/annulé : inutile de
 *             dépenser une des 3 analyses gratuites du jour) ;
 *   - false : données introuvables — pas cliquable.
 *
 * Rafraîchissement silencieux : toutes les 30 s s'il y a du direct,
 * 15 s tant que des vérifications sont en attente, sinon rien.
 *
 * Tous les champs enrichis sont optionnels : un match issu d'une source
 * sans logo/score s'affiche simplement sans eux.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MatchsScreen() {
    val context = LocalContext.current
    val telephone = remember { PreferencesTelephone.charger(context) }
    val scope = rememberCoroutineScope()

    var jourSelectionne by remember { mutableIntStateOf(0) } // 0 = aujourd'hui, 1 = demain
    var matchs by remember { mutableStateOf<List<MatchItem>>(emptyList()) }
    var chargement by remember { mutableStateOf(true) }
    var erreur by remember { mutableStateOf<String?>(null) }
    var filtre by remember { mutableStateOf(Filtre.TOUS) }
    var recherche by remember { mutableStateOf("") }

    var matchEnAnalyse by remember { mutableStateOf<MatchItem?>(null) }
    var resultatAnalyse by remember { mutableStateOf<MatchAnalysisResponse?>(null) }
    var contexteAnalyse by remember { mutableStateOf<ContexteMatch?>(null) }
    var erreurAnalyse by remember { mutableStateOf<String?>(null) }

    suspend fun rafraichir(jour: Int, silencieux: Boolean) {
        if (!silencieux) chargement = true
        try {
            val reponse = RetrofitClient.apiService.listerMatchs(if (jour == 0) "today" else "tomorrow")
            if (reponse.isSuccessful) {
                matchs = reponse.body()?.matchs ?: emptyList()
                erreur = null
            } else if (!silencieux) {
                erreur = "Erreur serveur : ${reponse.code()}"
            }
        } catch (e: Exception) {
            if (!silencieux) erreur = "Erreur réseau : ${e.message}"
        } finally {
            if (!silencieux) chargement = false
        }
    }

    fun charger(jour: Int) {
        jourSelectionne = jour
        scope.launch { rafraichir(jour, silencieux = false) }
    }

    LaunchedEffect(Unit) { charger(0) }

    val ilYADuDirect = matchs.any { it.statut == "en_direct" }
    val verificationsEnAttente = matchs.any { it.donneesDisponibles == null }
    LaunchedEffect(matchs, jourSelectionne) {
        val delaiMs = when {
            ilYADuDirect -> 30_000L
            verificationsEnAttente -> 15_000L
            else -> return@LaunchedEffect
        }
        delay(delaiMs)
        rafraichir(jourSelectionne, silencieux = true)
    }

    fun analyser(match: MatchItem) {
        if (!estAnalysable(match)) return
        matchEnAnalyse = match
        erreurAnalyse = null
        resultatAnalyse = null
        contexteAnalyse = null
        scope.launch {
            try {
                // Contexte (gratuit) et analyse en parallèle : le contexte
                // s'affiche dès qu'il arrive, sans attendre l'analyse.
                coroutineScope {
                    launch {
                        try {
                            val c = RetrofitClient.apiService.contexteMatch(match.fixture_id)
                            if (c.isSuccessful) contexteAnalyse = c.body()
                        } catch (_: Exception) { /* informatif seulement */ }
                    }
                    val analyse = async {
                        RetrofitClient.apiService.analyserMatch(
                            MatchAnalysisRequest(match.equipe1, match.equipe2, match.league, telephone, match.fixture_id)
                        )
                    }
                    val reponse = analyse.await()
                    if (reponse.isSuccessful) {
                        resultatAnalyse = reponse.body()
                    } else if (reponse.code() == 429) {
                        erreurAnalyse = "Limite de 3 analyses gratuites atteinte pour aujourd'hui. Passe en VIP pour continuer."
                    } else {
                        erreurAnalyse = "Erreur serveur : ${reponse.code()}"
                    }
                }
            } catch (e: Exception) {
                erreurAnalyse = "Erreur réseau : ${e.message}"
            }
        }
    }

    val groupes = remember(matchs, filtre, recherche) { construireGroupes(matchs, filtre, recherche) }
    val nbDirect = remember(matchs) { matchs.count { it.statut == "en_direct" } }
    val nbAnalysables = remember(matchs) { matchs.count { estAnalysable(it) } }

    Column(modifier = Modifier.fillMaxSize().background(StadiumNight)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OngletJour("Aujourd'hui", jourSelectionne == 0) { charger(0) }
            OngletJour("Demain", jourSelectionne == 1) { charger(1) }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PastilleFiltre("Tous", null, filtre == Filtre.TOUS) { filtre = Filtre.TOUS }
            PastilleFiltre("En direct", nbDirect, filtre == Filtre.DIRECT) { filtre = Filtre.DIRECT }
            PastilleFiltre("Analysables", nbAnalysables, filtre == Filtre.ANALYSABLES) { filtre = Filtre.ANALYSABLES }
        }

        OutlinedTextField(
            value = recherche,
            onValueChange = { recherche = it },
            placeholder = { Text("Rechercher une équipe ou un championnat", fontSize = 13.sp, color = TextFaint) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PanelNight,
                unfocusedContainerColor = PanelNight,
                focusedBorderColor = FloodlightGold,
                unfocusedBorderColor = PitchLine,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = FloodlightGold
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(6.dp))

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
                    onClick = { charger(jourSelectionne) },
                    colors = ButtonDefaults.buttonColors(containerColor = FloodlightGold, contentColor = TextOnAccent)
                ) { Text("Réessayer", fontWeight = FontWeight.Bold) }
            }
            matchs.isEmpty() -> Box(Modifier.fillMaxSize().padding(20.dp), Alignment.Center) {
                Text("Aucun match trouvé pour ce jour.", color = TextMuted, fontSize = 13.sp)
            }
            groupes.isEmpty() -> Box(Modifier.fillMaxSize().padding(20.dp), Alignment.Center) {
                Text("Aucun match ne correspond à ce filtre.", color = TextMuted, fontSize = 13.sp)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupes.forEach { groupe ->
                    stickyHeader(key = "h-${groupe.cle}") { EnteteChampionnat(groupe) }
                    items(groupe.matchs, key = { it.fixture_id }) { match ->
                        CarteMatch(match, onClick = { analyser(match) })
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }

    matchEnAnalyse?.let { match ->
        Dialog(onDismissRequest = { matchEnAnalyse = null }) {
            DialogueAnalyse(
                match = match,
                resultat = resultatAnalyse,
                contexte = contexteAnalyse,
                erreur = erreurAnalyse,
                onFermer = { matchEnAnalyse = null }
            )
        }
    }
}

// --------------------------------------------------------------------------
// Logique de présentation (pure, sans Compose)
// --------------------------------------------------------------------------

private enum class Filtre { TOUS, DIRECT, ANALYSABLES }

private data class Groupe(
    val cle: String,
    val nom: String,
    val pays: String?,
    val logo: String?,
    val rang: Int,
    val matchs: List<MatchItem>
)

private fun estAnalysable(m: MatchItem): Boolean =
    m.donneesDisponibles == true && m.statut !in listOf("termine", "reporte", "annule")

private fun construireGroupes(matchs: List<MatchItem>, filtre: Filtre, recherche: String): List<Groupe> {
    val q = recherche.trim().lowercase()
    return matchs
        .filter {
            when (filtre) {
                Filtre.TOUS -> true
                Filtre.DIRECT -> it.statut == "en_direct"
                Filtre.ANALYSABLES -> estAnalysable(it)
            }
        }
        .filter {
            q.isEmpty() || it.equipe1.lowercase().contains(q) ||
                it.equipe2.lowercase().contains(q) || it.league.lowercase().contains(q)
        }
        .groupBy { "${it.league}|${it.league_pays.orEmpty()}" }
        .map { (cle, liste) ->
            val premier = liste.first()
            Groupe(
                cle = cle,
                nom = premier.league.ifBlank { "Autre compétition" },
                pays = premier.league_pays,
                logo = liste.firstNotNullOfOrNull { it.league_logo },
                rang = liste.maxOf { it.league_rang ?: 0 },
                matchs = liste.sortedBy { it.date }
            )
        }
        // Championnats importants d'abord ; à égalité, ordre alphabétique stable.
        .sortedWith(compareByDescending<Groupe> { it.rang }.thenBy { it.nom.lowercase() })
}

/** L'API renvoie de l'UTC ("2026-10-01T18:45:00Z") — on affiche l'heure LOCALE du téléphone. */
private fun heureLocale(isoUtc: String): String {
    return try {
        val entree = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = entree.parse(isoUtc) ?: return isoUtc.takeIf { it.length >= 16 }?.substring(11, 16) ?: ""
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } catch (e: Exception) {
        isoUtc.takeIf { it.length >= 16 }?.substring(11, 16) ?: ""
    }
}

private fun libellePays(code: String?): String? = when {
    code.isNullOrBlank() -> null
    code == "INT" -> "International"
    else -> code
}

private fun ordinal(n: Int) = if (n == 1) "1er" else "${n}e"

// --------------------------------------------------------------------------
// Composants
// --------------------------------------------------------------------------

@Composable
private fun RowScope.OngletJour(label: String, selectionne: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selectionne) FloodlightGold else PanelRaised)
            .border(1.dp, if (selectionne) FloodlightGold else PitchLine, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selectionne) TextOnAccent else TextMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun PastilleFiltre(label: String, compteur: Int?, actif: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (actif) TextPrimary else PanelNight)
            .border(1.dp, if (actif) TextPrimary else PitchLine, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (actif) PanelNight else TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        if (compteur != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                "$compteur",
                color = if (actif) FloodlightGold else TextFaint,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun EnteteChampionnat(groupe: Groupe) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StadiumNight) // opaque : l'en-tête collant masque les cartes qui défilent dessous
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (groupe.logo != null) {
            AsyncImage(
                model = groupe.logo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                groupe.nom,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            libellePays(groupe.pays)?.let { Text(it, color = TextFaint, fontSize = 10.sp) }
        }
        Text("${groupe.matchs.size}", color = TextFaint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LogoEquipe(url: String?, taille: Int = 24) {
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(taille.dp)
        )
    } else {
        // Pas de logo : pastille neutre pour garder l'alignement des noms.
        Box(Modifier.size(taille.dp).clip(CircleShape).background(PanelRaised))
    }
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
private fun CarteMatch(match: MatchItem, onClick: () -> Unit) {
    val cliquable = estAnalysable(match)
    val enDirect = match.statut == "en_direct"
    val aUnScore = match.score1 != null && match.score2 != null
    val styleScore = if (enDirect) LiveRed else TextPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelNight)
            .border(
                1.dp,
                when {
                    enDirect -> LiveRed.copy(alpha = 0.35f)
                    cliquable -> TurfGreen.copy(alpha = 0.4f)
                    else -> PitchLine
                },
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = cliquable, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colonne gauche : minute / heure / statut
        Column(Modifier.width(46.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (match.statut) {
                "en_direct" -> {
                    PastilleDirect()
                    Spacer(Modifier.height(3.dp))
                    Text(match.minute ?: "EN DIRECT", color = LiveRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                "termine" -> Text("Terminé", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                "reporte" -> Text("Reporté", color = EmberOrange, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                "annule" -> Text("Annulé", color = EmberOrange, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                else -> Text(heureLocale(match.date), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.width(10.dp))

        // Équipes + scores
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LigneEquipe(match.logo1, match.equipe1, if (aUnScore) match.score1 else null, styleScore)
            LigneEquipe(match.logo2, match.equipe2, if (aUnScore) match.score2 else null, styleScore)
        }
        Spacer(Modifier.width(10.dp))

        // Disponibilité de l'analyse
        Column(horizontalAlignment = Alignment.End) {
            when {
                match.statut in listOf("termine", "reporte", "annule") -> Unit
                match.donneesDisponibles == null -> {
                    CircularProgressIndicator(color = TextFaint, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                }
                match.donneesDisponibles == true -> {
                    Text("Analyser", color = FloodlightGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    // Plusieurs fournisseurs concordent sur ce match : information plus fiable.
                    if ((match.nb_sources ?: 1) >= 2) {
                        Text("${match.nb_sources} sources", color = TextFaint, fontSize = 9.sp)
                    }
                }
                else -> Text("Indisponible", color = TextFaint, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LigneEquipe(logo: String?, nom: String, score: Int?, couleurScore: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LogoEquipe(logo)
        Spacer(Modifier.width(8.dp))
        Text(
            nom,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (score != null) {
            Text("$score", color = couleurScore, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun DialogueAnalyse(
    match: MatchItem,
    resultat: MatchAnalysisResponse?,
    contexte: ContexteMatch?,
    erreur: String?,
    onFermer: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PanelNight)
            .border(1.dp, PitchLine, RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // En-tête : logos + noms
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                LogoEquipe(match.logo1, 40)
                Spacer(Modifier.height(4.dp))
                Text(match.equipe1, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("VS", color = FloodlightGold, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 10.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                LogoEquipe(match.logo2, 40)
                Spacer(Modifier.height(4.dp))
                Text(match.equipe2, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }

        Column(
            modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Contexte gratuit (peut arriver avant l'analyse)
            contexte?.takeIf { it.disponible }?.let { BlocContexte(it, match) }

            when {
                erreur != null -> Text(erreur, color = EmberOrange, fontSize = 13.sp)
                resultat == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = FloodlightGold, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Analyse en cours...", color = TextMuted, fontSize = 13.sp)
                }
                else -> {
                    BarreProbabilites(
                        resultat.probabiliteVictoireEquipe1,
                        resultat.probabiliteMatchNul,
                        resultat.probabiliteVictoireEquipe2
                    )
                    Text(resultat.resumeAnalyse, color = TextMuted, fontSize = 13.sp)
                    if (!resultat.vip && resultat.pronosticsRestantsAujourdhui != null) {
                        Text(
                            "🎟️ ${resultat.pronosticsRestantsAujourdhui} analyse(s) gratuite(s) restante(s) aujourd'hui",
                            color = FloodlightGoldDim,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Button(
            onClick = onFermer,
            colors = ButtonDefaults.buttonColors(containerColor = FloodlightGold, contentColor = TextOnAccent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fermer", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BarreProbabilites(p1: Double, pNul: Double, p2: Double) {
    // Poids minimal pour qu'un segment très faible reste visible.
    fun poids(p: Double) = p.toFloat().coerceAtLeast(0.04f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
            Box(Modifier.weight(poids(p1)).fillMaxHeight().background(TurfGreen))
            Box(Modifier.weight(poids(pNul)).fillMaxHeight().background(NeutralDraw))
            Box(Modifier.weight(poids(p2)).fillMaxHeight().background(EmberOrange))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("V1 ${(p1 * 100).toInt()}%", color = TurfGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Nul ${(pNul * 100).toInt()}%", color = NeutralDraw, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("V2 ${(p2 * 100).toInt()}%", color = EmberOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun BlocContexte(contexte: ContexteMatch, match: MatchItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelRaised)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        contexte.classement?.let { c ->
            if (c.equipe1 != null && c.equipe2 != null) {
                Text("CLASSEMENT (sur ${c.total_equipes})", color = TextFaint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth()) {
                    CelluleClassement(c.equipe1, Modifier.weight(1f), Alignment.Start)
                    CelluleClassement(c.equipe2, Modifier.weight(1f), Alignment.End)
                }
            }
        }
        contexte.confrontations?.let { h ->
            Text("CONFRONTATIONS DIRECTES (${h.total})", color = TextFaint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                "${h.bilan.equipe1} victoire(s) ${match.equipe1} · ${h.bilan.nuls} nul(s) · " +
                    "${h.bilan.equipe2} victoire(s) ${match.equipe2}",
                color = TextPrimary,
                fontSize = 12.sp
            )
            h.derniers.take(3).forEach { d ->
                Text(
                    "${d.date.take(4)} · ${d.equipe_domicile} ${d.score} ${d.equipe_exterieur}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CelluleClassement(ligne: LigneClassement, modifier: Modifier, alignement: Alignment.Horizontal) {
    Column(modifier, horizontalAlignment = alignement) {
        Text(ordinal(ligne.rang), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("${ligne.points} pts · diff. ${if (ligne.diff >= 0) "+" else ""}${ligne.diff}", color = TextMuted, fontSize = 11.sp)
    }
}
