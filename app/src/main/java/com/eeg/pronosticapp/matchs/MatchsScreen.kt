package com.eeg.pronosticapp.matchs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.eeg.pronosticapp.data.PreferencesTelephone
import com.eeg.pronosticapp.model.*
import com.eeg.pronosticapp.network.RetrofitClient
import com.eeg.pronosticapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MatchsScreen() {
    val context = LocalContext.current
    val telephone = remember { PreferencesTelephone.charger(context) }
    val scope = rememberCoroutineScope()

    var jourSelectionne by remember { mutableIntStateOf(0) }
    var matchs by remember { mutableStateOf<List<MatchItem>>(emptyList()) }
    var chargement by remember { mutableStateOf(true) }
    var erreur by remember { mutableStateOf<String?>(null) }

    var matchDetail by remember { mutableStateOf<MatchDetailResponse?>(null) }
    var chargementDetail by remember { mutableStateOf(false) }
    var erreurDetail by remember { mutableStateOf<String?>(null) }

    var matchAnalyse by remember { mutableStateOf<MatchItem?>(null) }
    var resultatAnalyse by remember { mutableStateOf<MatchAnalysisResponse?>(null) }
    var erreurAnalyse by remember { mutableStateOf<String?>(null) }

    suspend fun rafraichir(jour: Int, silencieux: Boolean) {
        if (!silencieux) chargement = true
        try {
            val r = RetrofitClient.apiService.listerMatchs(if (jour == 0) "today" else "tomorrow")
            if (r.isSuccessful) {
                matchs = r.body()?.matchs ?: emptyList()
                erreur = null
            } else if (!silencieux) erreur = "Erreur serveur : ${r.code()}"
        } catch (e: Exception) {
            if (!silencieux) erreur = "Connexion indisponible."
        } finally {
            if (!silencieux) chargement = false
        }
    }

    fun charger(jour: Int) {
        jourSelectionne = jour
        scope.launch { rafraichir(jour, false) }
    }

    fun ouvrirFiche(match: MatchItem) {
        matchDetail = null
        erreurDetail = null
        chargementDetail = true
        scope.launch {
            try {
                val r = RetrofitClient.apiService.detailMatch(match.fixture_id)
                if (r.isSuccessful) matchDetail = r.body()
                else erreurDetail = "Fiche détaillée indisponible pour cette source."
            } catch (_: Exception) {
                erreurDetail = "Impossible de charger les détails."
            } finally {
                chargementDetail = false
            }
        }
    }

    fun analyser(match: MatchItem) {
        if (match.donneesDisponibles != true) return
        matchAnalyse = match
        erreurAnalyse = null
        resultatAnalyse = null
        scope.launch {
            try {
                val r = RetrofitClient.apiService.analyserMatch(
                    MatchAnalysisRequest(match.equipe1, match.equipe2, match.league, telephone)
                )
                if (r.isSuccessful) resultatAnalyse = r.body()
                else erreurAnalyse = if (r.code() == 429)
                    "Limite de 3 analyses gratuites atteinte aujourd'hui. Passez VIP pour continuer."
                else "Erreur serveur : ${r.code()}"
            } catch (_: Exception) {
                erreurAnalyse = "Connexion indisponible."
            }
        }
    }

    LaunchedEffect(Unit) { charger(0) }
    LaunchedEffect(matchs, jourSelectionne) {
        if (matchs.any { it.donneesDisponibles == null }) {
            delay(15_000)
            rafraichir(jourSelectionne, true)
        }
    }

    Column(Modifier.fillMaxSize().background(StadiumNight)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OngletJour("Aujourd'hui", jourSelectionne == 0) { charger(0) }
            OngletJour("Demain", jourSelectionne == 1) { charger(1) }
        }

        if (!chargement && erreur == null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${matchs.size} rencontre(s)", color = TextMuted, fontSize = 12.sp)
                Text(
                    "Données enrichies · H2H · Stats · Compositions",
                    color = FloodlightGoldDim,
                    fontSize = 10.sp
                )
            }
        }

        when {
            chargement -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = FloodlightGold)
            }
            erreur != null -> Box(Modifier.fillMaxSize().padding(20.dp), Alignment.Center) {
                Text(erreur!!, color = EmberOrange, fontSize = 14.sp)
            }
            matchs.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Aucun match trouvé pour ce jour.", color = TextMuted, fontSize = 13.sp)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(matchs, key = { it.fixture_id }) { match ->
                    CarteMatch(
                        match = match,
                        onFiche = { ouvrirFiche(match) },
                        onAnalyse = { analyser(match) }
                    )
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }

    if (chargementDetail || matchDetail != null || erreurDetail != null) {
        Dialog(onDismissRequest = {
            if (!chargementDetail) {
                matchDetail = null
                erreurDetail = null
            }
        }) {
            if (chargementDetail) {
                CarteDialog {
                    CircularProgressIndicator(color = FloodlightGold)
                    Text("Chargement de la fiche match…", color = TextMuted, fontSize = 13.sp)
                }
            } else if (matchDetail != null) {
                FicheMatch(matchDetail!!) { matchDetail = null }
            } else {
                CarteDialog {
                    Text(erreurDetail ?: "Données indisponibles", color = EmberOrange)
                    Button(onClick = { matchDetail = null; erreurDetail = null }) { Text("Fermer") }
                }
            }
        }
    }

    matchAnalyse?.let { match ->
        Dialog(onDismissRequest = { matchAnalyse = null }) {
            CarteDialog {
                Text("${match.equipe1}  —  ${match.equipe2}", color = FloodlightGold, fontWeight = FontWeight.Bold)
                when {
                    erreurAnalyse != null -> Text(erreurAnalyse!!, color = EmberOrange, fontSize = 13.sp)
                    resultatAnalyse == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = FloodlightGold, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Calcul Poisson · Dixon-Coles · ELO…", color = TextMuted, fontSize = 13.sp)
                    }
                    else -> ResultatAnalyseCompact(resultatAnalyse!!)
                }
                Button(
                    onClick = { matchAnalyse = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = FloodlightGold, contentColor = StadiumNight)
                ) { Text("Fermer", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun CarteMatch(match: MatchItem, onFiche: () -> Unit, onAnalyse: () -> Unit) {
    val heure = match.date.takeIf { it.length >= 16 }?.substring(11, 16) ?: "--:--"
    val disponible = match.donneesDisponibles == true

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PanelNight)
            .border(1.dp, if (disponible) TurfGreen.copy(.35f) else PitchLine, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(10.dp).clip(RoundedCornerShape(5.dp))
                    .background(if (disponible) TurfGreen else TextFaint)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${match.equipe1}  —  ${match.equipe2}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${match.league}  ·  $heure", color = TextMuted, fontSize = 11.sp)
            }
            Text(
                if (disponible) "DISPONIBLE" else "VÉRIFICATION",
                color = if (disponible) TurfGreen else TextFaint,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onFiche,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 7.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FloodlightGold)
            ) { Text("FICHE MATCH", fontSize = 11.sp) }
            Button(
                onClick = onAnalyse,
                enabled = disponible,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 7.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FloodlightGold, contentColor = StadiumNight)
            ) { Text("ANALYSER", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun FicheMatch(d: MatchDetailResponse, fermer: () -> Unit) {
    val score = d.score.joinToString("  ") {
        "${it["name"] ?: ""} ${it["score"] ?: ""}"
    }
    val status = d.statut?.get("reason") as? Map<*, *>
    val statutTexte = status?.get("long")?.toString()
        ?: d.statut?.get("scoreStr")?.toString()
        ?: "À venir"

    CarteDialog {
        Text(d.league ?: "Football", color = FloodlightGoldDim, fontSize = 11.sp)
        Text("${d.equipe1}  —  ${d.equipe2}", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("$statutTexte${if (score.isNotBlank()) " · $score" else ""}", color = TurfGreen, fontWeight = FontWeight.Bold)

        if (d.formations.isNotEmpty()) {
            Text(
                "Compositions : ${d.formations["home"] ?: "?"} vs ${d.formations["away"] ?: "?"}",
                color = TextMuted, fontSize = 12.sp
            )
        }

        if (d.statistiquesTop.isNotEmpty()) {
            Text("STATISTIQUES", color = FloodlightGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            d.statistiquesTop.take(8).forEach {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(it.home, color = TextPrimary, fontSize = 11.sp)
                    Text(it.label, color = TextMuted, fontSize = 10.sp)
                    Text(it.away, color = TextPrimary, fontSize = 11.sp)
                }
            }
        }

        if (d.h2hRecents.isNotEmpty()) {
            Text("CONFRONTATIONS RÉCENTES", color = FloodlightGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            d.h2hRecents.forEach {
                Text(
                    "${it.home ?: "?"} ${it.score ?: "-"} ${it.away ?: "?"}",
                    color = TextMuted, fontSize = 11.sp
                )
            }
        }

        Button(
            onClick = fermer,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = FloodlightGold, contentColor = StadiumNight)
        ) { Text("Fermer", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ResultatAnalyseCompact(r: MatchAnalysisResponse) {
    Text(
        "1  ${pct(r.probabiliteVictoireEquipe1)}   |   X  ${pct(r.probabiliteMatchNul)}   |   2  ${pct(r.probabiliteVictoireEquipe2)}",
        color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp
    )
    Text(
        "Buts attendus : ${"%.2f".format(r.butsAttendusEquipe1)} — ${"%.2f".format(r.butsAttendusEquipe2)}",
        color = TextMuted, fontSize = 12.sp
    )
    Text(
        "BTTS ${pct(r.probabiliteBTTS)} · Over 1.5 ${pct(r.probabiliteOver15)} · Over 2.5 ${pct(r.probabiliteOver25)}",
        color = TextMuted, fontSize = 12.sp
    )
    r.scoresProbables.take(3).forEach {
        Text("${it.score} · ${pct(it.probabilite)}", color = FloodlightGoldDim, fontSize = 12.sp)
    }
    r.predictionExterne?.let {
        Text("Contrôle externe : $it", color = TurfGreen, fontSize = 11.sp)
    }
    r.confianceGlobale?.let {
        Text("Confiance globale : ${pct(it)}", color = FloodlightGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

private fun pct(v: Double) = "${(v * 100).toInt()}%"

@Composable
private fun CarteDialog(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PanelNight)
            .border(1.dp, PitchLine, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun RowScope.OngletJour(label: String, selectionne: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
            .background(if (selectionne) FloodlightGold else PanelRaised)
            .border(1.dp, if (selectionne) FloodlightGold else PitchLine, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selectionne) StadiumNight else TextMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
