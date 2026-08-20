package com.example.pronosticapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.pronosticapp.data.EquipesConnues
import com.example.pronosticapp.data.PreferencesTelephone
import com.example.pronosticapp.model.ConfirmationRequest
import com.example.pronosticapp.model.MatchAnalysisRequest
import com.example.pronosticapp.model.MatchAnalysisResponse
import com.example.pronosticapp.model.PaiementRequest
import com.example.pronosticapp.network.RetrofitClient
import com.example.pronosticapp.network.MockAnalysisProvider
import com.example.pronosticapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 🔧 INTERRUPTEUR : true = données simulées (aucun backend requis), false = appels
// réels vers le backend. Actuellement sur false pour tester avec le vrai backend
// local via l'émulateur (voir RetrofitClient.kt pour l'adresse utilisée).
const val USE_MOCK_DATA = false

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PronosticAppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    PronosticScreen()
                }
            }
        }
    }
}

@Composable
fun PronosticScreen() {
    val context = LocalContext.current

    var equipe1 by remember { mutableStateOf("") }
    var equipe2 by remember { mutableStateOf("") }
    var typeMatch by remember { mutableStateOf("Ligue 1") }
    var resultat by remember { mutableStateOf<MatchAnalysisResponse?>(null) }
    var enCours by remember { mutableStateOf(false) }
    var erreur by remember { mutableStateOf<String?>(null) }
    var limiteAtteinte by remember { mutableStateOf(false) }

    var telephone by remember { mutableStateOf(PreferencesTelephone.charger(context)) }
    var afficherDialogTelephone by remember { mutableStateOf(telephone.isBlank()) }
    var afficherDialogVip by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // Motif d'ambiance : évoque discrètement les lignes d'un terrain
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = PitchLine.copy(alpha = 0.4f),
                radius = size.width * 0.55f,
                center = Offset(size.width * 1.05f, size.height * 0.06f),
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = FloodlightGold.copy(alpha = 0.05f),
                radius = size.width * 0.9f,
                center = Offset(size.width * -0.25f, size.height * 0.95f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 32.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeaderBlock(isMock = USE_MOCK_DATA, onVipClick = { afficherDialogVip = true })

            MatchupCard(
                equipe1 = equipe1,
                onEquipe1Change = { equipe1 = it },
                equipe2 = equipe2,
                onEquipe2Change = { equipe2 = it },
                typeMatch = typeMatch,
                onTypeMatchChange = { typeMatch = it }
            )

            Button(
                onClick = {
                    erreur = null
                    limiteAtteinte = false
                    enCours = true
                    scope.launch {
                        try {
                            if (USE_MOCK_DATA) {
                                // Mode test : pas d'appel réseau, résultat simulé instantané
                                delay(600) // simule un temps de traitement réaliste
                                resultat = MockAnalysisProvider.generer(equipe1, equipe2, typeMatch)
                            } else {
                                val response = RetrofitClient.apiService.analyserMatch(
                                    MatchAnalysisRequest(equipe1, equipe2, typeMatch, telephone)
                                )
                                if (response.isSuccessful) {
                                    resultat = response.body()
                                } else if (response.code() == 429) {
                                    limiteAtteinte = true
                                    erreur = "Limite gratuite atteinte pour aujourd'hui."
                                } else {
                                    erreur = "Erreur serveur : ${response.code()}"
                                }
                            }
                        } catch (e: Exception) {
                            erreur = "Erreur réseau : ${e.message}"
                        } finally {
                            enCours = false
                        }
                    }
                },
                enabled = equipe1.isNotBlank() && equipe2.isNotBlank() && !enCours,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FloodlightGold,
                    contentColor = StadiumNight,
                    disabledContainerColor = PanelRaised,
                    disabledContentColor = TextFaint
                )
            ) {
                if (enCours) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = StadiumNight,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("ANALYSE EN COURS", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                } else {
                    Text("ANALYSER LE MATCH", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            erreur?.let {
                ErrorCard(message = it)
                if (limiteAtteinte) {
                    Button(
                        onClick = { afficherDialogVip = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FloodlightGold,
                            contentColor = StadiumNight
                        )
                    ) {
                        Text("💎 PASSER VIP — ACCÈS ILLIMITÉ", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }

            resultat?.let { r ->
                ResultBlock(r)
            }
        }

        if (afficherDialogTelephone) {
            DialogTelephone(
                valeurInitiale = telephone,
                onValider = { saisie ->
                    telephone = saisie
                    PreferencesTelephone.sauvegarder(context, saisie)
                    afficherDialogTelephone = false
                }
            )
        }

        if (afficherDialogVip) {
            DialogVip(
                telephone = telephone,
                onFerme = { afficherDialogVip = false },
                onVipActive = { limiteAtteinte = false; erreur = null }
            )
        }
    }
}

@Composable
private fun HeaderBlock(isMock: Boolean, onVipClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "PRONOSTIC",
                    color = FloodlightGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 3.sp
                )
                Text(
                    text = "Analyse de match",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(FloodlightGold)
                    .clickable { onVipClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("💎 VIP", color = StadiumNight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        PitchDivider()
        if (isMock) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .border(1.dp, FloodlightGoldDim, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "MODE TEST · DONNÉES SIMULÉES",
                        color = FloodlightGoldDim,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PitchDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = PitchLine,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )
    }
}

@Composable
private fun MatchupCard(
    equipe1: String,
    onEquipe1Change: (String) -> Unit,
    equipe2: String,
    onEquipe2Change: (String) -> Unit,
    typeMatch: String,
    onTypeMatchChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PanelNight)
            .border(1.dp, PitchLine, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TeamField(
                    label = "DOMICILE",
                    value = equipe1,
                    onValueChange = onEquipe1Change,
                    accent = TurfGreen,
                    modifier = Modifier.weight(1f)
                )
                TeamField(
                    label = "EXTÉRIEUR",
                    value = equipe2,
                    onValueChange = onEquipe2Change,
                    accent = EmberOrange,
                    modifier = Modifier.weight(1f)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(StadiumNight)
                    .border(1.5.dp, FloodlightGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("VS", color = FloodlightGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "TYPE DE RENCONTRE",
                color = TextMuted,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Saisie libre : n'importe quelle ligue/compétition, même absente
            // de la liste ci-dessous, peut être tapée directement.
            OutlinedTextField(
                value = typeMatch,
                onValueChange = onTypeMatchChange,
                placeholder = { Text("Ex : Ligue 1, Coupe, Amical...", color = TextFaint) },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FloodlightGold,
                    unfocusedBorderColor = PitchLine,
                    focusedContainerColor = PanelRaised,
                    unfocusedContainerColor = PanelRaised,
                    cursorColor = FloodlightGold
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Raccourcis pour les ligues courantes — un simple appui
            // remplit le champ, mais ne bloque jamais la saisie libre
            // ci-dessus si la ligue recherchée n'y figure pas.
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TYPES_DE_MATCH.forEach { (libelle, valeur) ->
                    MatchTypeChip(libelle, valeur, typeMatch, onTypeMatchChange)
                }
            }
        }
    }
}

// Ligues principales + catégories génériques. La valeur (deuxième élément)
// est ce qui est envoyé au backend et inséré dans le résumé du pronostic.
private val TYPES_DE_MATCH = listOf(
    "Ligue 1" to "Ligue 1",
    "LaLiga" to "LaLiga",
    "Premier League" to "Premier League",
    "Serie A" to "Serie A",
    "Bundesliga" to "Bundesliga",
    "Primeira Liga" to "Primeira Liga",
    "Ligue des Champions" to "Ligue des Champions",
    "Coupe" to "coupe",
    "Amical" to "amical"
)

@Composable
private fun TeamField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    var estFocus by remember { mutableStateOf(false) }
    val suggestions = remember(value) { EquipesConnues.suggestions(value, limite = 4) }
    val afficherSuggestions = estFocus && suggestions.isNotEmpty()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Équipe", color = TextFaint) },
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = PitchLine,
                focusedContainerColor = PanelRaised,
                unfocusedContainerColor = PanelRaised,
                cursorColor = accent
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { estFocus = it.isFocused }
        )

        // Suggestions locales — filtrage instantané, aucun appel réseau.
        // La saisie libre reste toujours possible même si rien ne correspond.
        if (afficherSuggestions) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(PanelRaised)
                    .border(1.dp, PitchLine, RoundedCornerShape(10.dp))
            ) {
                suggestions.forEach { suggestion ->
                    Text(
                        text = suggestion,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onValueChange(suggestion)
                                estFocus = false
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchTypeChip(
    label: String,
    value: String,
    selected: String,
    onSelect: (String) -> Unit
) {
    val isSelected = selected == value
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) FloodlightGold else PanelRaised)
            .border(1.dp, if (isSelected) FloodlightGold else PitchLine, RoundedCornerShape(10.dp))
            .clickable { onSelect(value) }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (isSelected) StadiumNight else TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EmberOrange.copy(alpha = 0.12f))
            .border(1.dp, EmberOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⚠", color = EmberOrange, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(message, color = EmberOrange, fontSize = 13.sp)
    }
}

@Composable
private fun ResultBlock(r: MatchAnalysisResponse) {
    val p1 = r.probabiliteVictoireEquipe1 * 100
    val pNul = r.probabiliteMatchNul * 100
    val p2 = r.probabiliteVictoireEquipe2 * 100

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PitchDivider()
        Text(
            "RÉSULTAT",
            color = FloodlightGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        // Carte scoreboard — élément signature de l'écran
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(PanelNight)
                .border(1.dp, PitchLine, RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    r.equipe1.uppercase(),
                    color = TurfGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Text(
                    r.equipe2.uppercase(),
                    color = EmberOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                ScoreDigit(value = p1, color = TurfGreen, big = true, align = Alignment.Start, modifier = Modifier.weight(1f))
                ScoreDigit(value = pNul, color = NeutralDraw, big = false, align = Alignment.CenterHorizontally, modifier = Modifier.weight(0.7f))
                ScoreDigit(value = p2, color = EmberOrange, big = true, align = Alignment.End, modifier = Modifier.weight(1f))
            }

            // Barre de probabilités façon tableau de cotes de stade
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                Box(Modifier.weight(p1.toFloat().coerceAtLeast(1f)).fillMaxHeight().background(TurfGreen))
                Box(Modifier.weight(pNul.toFloat().coerceAtLeast(1f)).fillMaxHeight().background(NeutralDraw))
                Box(Modifier.weight(p2.toFloat().coerceAtLeast(1f)).fillMaxHeight().background(EmberOrange))
            }
            Text(
                "NUL",
                color = TextFaint,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // Carte marchés — over/under, BTTS, scores les plus probables
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(PanelNight)
                .border(1.dp, PitchLine, RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "BUTS ATTENDUS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MarcheStat(
                    label = r.equipe1.uppercase(),
                    valeur = String.format("%.1f", r.butsAttendusEquipe1),
                    couleur = TurfGreen,
                    modifier = Modifier.weight(1f)
                )
                MarcheStat(
                    label = r.equipe2.uppercase(),
                    valeur = String.format("%.1f", r.butsAttendusEquipe2),
                    couleur = EmberOrange,
                    modifier = Modifier.weight(1f)
                )
            }

            PitchDivider()

            Text(
                "AUTRES MARCHÉS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MarcheStat(
                    label = "+0.5 BUT",
                    valeur = "${(r.probabiliteOver05 * 100).toInt()}%",
                    couleur = FloodlightGold,
                    modifier = Modifier.weight(1f)
                )
                MarcheStat(
                    label = "+1.5 BUT",
                    valeur = "${(r.probabiliteOver15 * 100).toInt()}%",
                    couleur = FloodlightGold,
                    modifier = Modifier.weight(1f)
                )
                MarcheStat(
                    label = "2 MARQUENT",
                    valeur = "${(r.probabiliteBTTS * 100).toInt()}%",
                    couleur = FloodlightGold,
                    modifier = Modifier.weight(1f)
                )
            }

            if (r.scoresProbables.isNotEmpty()) {
                Text(
                    "SCORES LES PLUS PROBABLES",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    r.scoresProbables.take(3).forEach { sp ->
                        MarcheStat(
                            label = "${(sp.probabilite * 100).toInt()}%",
                            valeur = sp.score,
                            couleur = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(PanelNight)
                .border(1.dp, PitchLine, RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "FACTEURS CLÉS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            r.facteursCles.forEach { facteur ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("▸", color = FloodlightGold, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(facteur, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PanelRaised)
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(FloodlightGold)
            )
            Text(
                r.resumeAnalyse,
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
private fun ScoreDigit(
    value: Double,
    color: Color,
    big: Boolean,
    align: Alignment.Horizontal,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = align) {
        Text(
            text = "${value.toInt()}%",
            color = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (big) 30.sp else 18.sp
        )
    }
}

@Composable
private fun MarcheStat(
    label: String,
    valeur: String,
    couleur: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(PanelRaised)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = valeur,
            color = couleur,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = label,
            color = TextFaint,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun DialogTelephone(
    valeurInitiale: String,
    onValider: (String) -> Unit
) {
    var texte by remember { mutableStateOf(valeurInitiale) }

    Dialog(onDismissRequest = { /* pas d'annulation : le téléphone est nécessaire pour continuer */ }) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(PanelNight)
                .border(1.dp, PitchLine, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Bienvenue 👋", color = FloodlightGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Entre ton numéro de téléphone pour commencer. Il sert à suivre ton quota " +
                    "de pronostics gratuits et à activer le mode VIP si tu choisis d'y passer.",
                color = TextMuted,
                fontSize = 13.sp
            )
            OutlinedTextField(
                value = texte,
                onValueChange = { texte = it },
                placeholder = { Text("Ex : +22890000000", color = TextFaint) },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FloodlightGold,
                    unfocusedBorderColor = PitchLine,
                    focusedContainerColor = PanelRaised,
                    unfocusedContainerColor = PanelRaised,
                    cursorColor = FloodlightGold
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { if (texte.isNotBlank()) onValider(texte.trim()) },
                enabled = texte.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FloodlightGold,
                    contentColor = StadiumNight
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuer", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DialogVip(
    telephone: String,
    onFerme: () -> Unit,
    onVipActive: () -> Unit
) {
    var reseau by remember { mutableStateOf("TMONEY") }
    var etape by remember { mutableStateOf("choix") } // "choix" | "attente" | "succes" | "echec"
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (etape != "attente") onFerme() }) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(PanelNight)
                .border(1.dp, FloodlightGold, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("💎 Passer VIP", color = FloodlightGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)

            when (etape) {
                "choix" -> {
                    Text(
                        "300 FCFA — accès illimité aux pronostics pendant 30 jours.",
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text("Choisis ton opérateur :", color = TextMuted, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MatchTypeChip("TMoney", "TMONEY", reseau, { reseau = it })
                        MatchTypeChip("Moov Money", "FLOOZ", reseau, { reseau = it })
                    }
                    Button(
                        onClick = {
                            etape = "attente"
                            message = "Paiement en cours..."
                            scope.launch {
                                try {
                                    val payReponse = RetrofitClient.apiService.vipPayer(
                                        PaiementRequest(telephone, reseau)
                                    )
                                    val corpsPay = payReponse.body()
                                    if (!payReponse.isSuccessful || corpsPay == null) {
                                        etape = "echec"
                                        message = "Impossible de lancer le paiement. Réessaie."
                                        return@launch
                                    }

                                    val txReference = corpsPay.txReference
                                    var confirme = false
                                    var tentative = 0

                                    while (!confirme && tentative < 12) {
                                        delay(5000)
                                        tentative++
                                        val confirmReponse = RetrofitClient.apiService.vipConfirmer(
                                            ConfirmationRequest(telephone, txReference)
                                        )
                                        if (confirmReponse.isSuccessful && confirmReponse.body()?.confirme == true) {
                                            confirme = true
                                        }
                                    }

                                    if (confirme) {
                                        etape = "succes"
                                        message = "VIP activé pour 30 jours 🎉"
                                        onVipActive()
                                    } else {
                                        etape = "echec"
                                        message = "Paiement non confirmé après 1 minute. Réessaie, ou vérifie que tu as bien validé avec ton code secret Mobile Money."
                                    }
                                } catch (e: Exception) {
                                    etape = "echec"
                                    message = "Erreur réseau : ${e.message}"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FloodlightGold,
                            contentColor = StadiumNight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Payer 300 FCFA", fontWeight = FontWeight.Bold)
                    }
                }

                "attente" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = FloodlightGold,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(message, color = TextMuted, fontSize = 13.sp)
                    }
                    Text(
                        "Valide le paiement avec ton code secret Mobile Money si on te le demande.",
                        color = TextFaint,
                        fontSize = 11.sp
                    )
                }

                "succes" -> {
                    Text("✅ $message", color = TurfGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = onFerme,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TurfGreen,
                            contentColor = StadiumNight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fermer", fontWeight = FontWeight.Bold)
                    }
                }

                "echec" -> {
                    Text("❌ $message", color = EmberOrange, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { etape = "choix" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PanelRaised,
                                contentColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Réessayer")
                        }
                        Button(
                            onClick = onFerme,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PanelRaised,
                                contentColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fermer")
                        }
                    }
                }
            }
        }
    }
}
