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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.eeg.pronosticapp.model.MatchAnalysisRequest
import com.eeg.pronosticapp.model.MatchAnalysisResponse
import com.eeg.pronosticapp.model.MatchItem
import com.eeg.pronosticapp.network.RetrofitClient
import com.eeg.pronosticapp.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Écran "Matchs du jour" — liste des matchs des championnats suivis
 * pour aujourd'hui / demain, avec un indicateur (icône + couleur) qui
 * montre si les données d'analyse sont déjà disponibles avant même de cliquer.
 */
@Composable
fun MatchsScreen() {
    val scope = rememberCoroutineScope()

    var jourSelectionne by remember { mutableIntStateOf(0) } // 0 = aujourd'hui, 1 = demain
    var matchs by remember { mutableStateOf<List<MatchItem>>(emptyList()) }
    var chargement by remember { mutableStateOf(true) }
    var erreur by remember { mutableStateOf<String?>(null) }

    var matchEnAnalyse by remember { mutableStateOf<MatchItem?>(null) }
    var resultatAnalyse by remember { mutableStateOf<MatchAnalysisResponse?>(null) }
    var erreurAnalyse by remember { mutableStateOf<String?>(null) }

    fun charger(jour: Int) {
        jourSelectionne = jour
        chargement = true
        erreur = null
        scope.launch {
            try {
                val reponse = RetrofitClient.apiService.listerMatchs(if (jour == 0) "today" else "tomorrow")
                if (reponse.isSuccessful) {
                    matchs = reponse.body()?.matchs ?: emptyList()
                } else {
                    erreur = "Erreur serveur : ${reponse.code()}"
                }
            } catch (e: Exception) {
                erreur = "Erreur réseau : ${e.message}"
            } finally {
                chargement = false
            }
        }
    }

    LaunchedEffect(Unit) { charger(0) }

    fun analyser(match: MatchItem) {
        if (!match.donneesDisponibles) return
        matchEnAnalyse = match
        erreurAnalyse = null
        resultatAnalyse = null
        scope.launch {
            try {
                val reponse = RetrofitClient.apiService.analyserMatch(
                    MatchAnalysisRequest(match.equipe1, match.equipe2, match.league)
                )
                if (reponse.isSuccessful) {
                    resultatAnalyse = reponse.body()
                } else if (reponse.code() == 429) {
                    erreurAnalyse = "Limite de 3 analyses gratuites atteinte pour aujourd'hui. Passe en VIP pour continuer."
                } else {
                    erreurAnalyse = "Erreur serveur : ${reponse.code()}"
                }
            } catch (e: Exception) {
                erreurAnalyse = "Erreur réseau : ${e.message}"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(StadiumNight)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OngletJour("Aujourd'hui", jourSelectionne == 0) { charger(0) }
            OngletJour("Demain", jourSelectionne == 1) { charger(1) }
        }

        when {
            chargement -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = FloodlightGold)
            }
            erreur != null -> Box(Modifier.fillMaxSize().padding(20.dp), Alignment.Center) {
                Text(erreur ?: "", color = EmberOrange, fontSize = 14.sp)
            }
            matchs.isEmpty() -> Box(Modifier.fillMaxSize().padding(20.dp), Alignment.Center) {
                Text("Aucun match trouvé pour ce jour dans les championnats suivis.", color = TextMuted, fontSize = 13.sp)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(matchs, key = { it.fixture_id }) { match ->
                    CarteMatch(match, onClick = { analyser(match) })
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }

    matchEnAnalyse?.let { match ->
        Dialog(onDismissRequest = { matchEnAnalyse = null }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(PanelNight)
                    .border(1.dp, PitchLine, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "${match.equipe1} — ${match.equipe2}",
                    color = FloodlightGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                when {
                    erreurAnalyse != null -> Text(erreurAnalyse ?: "", color = EmberOrange, fontSize = 13.sp)
                    resultatAnalyse == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = FloodlightGold, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Analyse en cours...", color = TextMuted, fontSize = 13.sp)
                    }
                    else -> {
                        val r = resultatAnalyse!!
                        Text(
                            "V1 ${(r.probabiliteVictoireEquipe1 * 100).toInt()}% · " +
                                "Nul ${(r.probabiliteMatchNul * 100).toInt()}% · " +
                                "V2 ${(r.probabiliteVictoireEquipe2 * 100).toInt()}%",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(r.resumeAnalyse, color = TextMuted, fontSize = 13.sp)
                        if (!r.vip && r.pronosticsRestantsAujourdhui != null) {
                            Text(
                                "🎟️ ${r.pronosticsRestantsAujourdhui} analyse(s) gratuite(s) restante(s) aujourd'hui",
                                color = FloodlightGoldDim,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Button(
                    onClick = { matchEnAnalyse = null },
                    colors = ButtonDefaults.buttonColors(containerColor = FloodlightGold, contentColor = StadiumNight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fermer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

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
        Text(label, color = if (selectionne) StadiumNight else TextMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun CarteMatch(match: MatchItem, onClick: () -> Unit) {
    val heure = match.date.takeIf { it.length >= 16 }?.substring(11, 16) ?: ""
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelNight)
            .border(1.dp, if (match.donneesDisponibles) TurfGreen.copy(alpha = 0.4f) else PitchLine, RoundedCornerShape(14.dp))
            .clickable(enabled = match.donneesDisponibles, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (match.donneesDisponibles) TurfGreen else TextFaint)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${match.equipe1} — ${match.equipe2}", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("${match.league} · $heure", color = TextMuted, fontSize = 11.sp)
        }
        Text(
            if (match.donneesDisponibles) "Analyser" else "Indisponible",
            color = if (match.donneesDisponibles) FloodlightGold else TextFaint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
