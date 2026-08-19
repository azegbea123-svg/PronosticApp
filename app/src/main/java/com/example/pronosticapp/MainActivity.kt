package com.example.pronosticapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pronosticapp.model.MatchAnalysisRequest
import com.example.pronosticapp.model.MatchAnalysisResponse
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
    var equipe1 by remember { mutableStateOf("") }
    var equipe2 by remember { mutableStateOf("") }
    var typeMatch by remember { mutableStateOf("championnat") }
    var resultat by remember { mutableStateOf<MatchAnalysisResponse?>(null) }
    var enCours by remember { mutableStateOf(false) }
    var erreur by remember { mutableStateOf<String?>(null) }

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
            HeaderBlock(isMock = USE_MOCK_DATA)

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
                    enCours = true
                    scope.launch {
                        try {
                            if (USE_MOCK_DATA) {
                                // Mode test : pas d'appel réseau, résultat simulé instantané
                                delay(600) // simule un temps de traitement réaliste
                                resultat = MockAnalysisProvider.generer(equipe1, equipe2, typeMatch)
                            } else {
                                val response = RetrofitClient.apiService.analyserMatch(
                                    MatchAnalysisRequest(equipe1, equipe2, typeMatch)
                                )
                                if (response.isSuccessful) {
                                    resultat = response.body()
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
            }

            resultat?.let { r ->
                ResultBlock(r)
            }
        }
    }
}

@Composable
private fun HeaderBlock(isMock: Boolean) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MatchTypeChip("Championnat", "championnat", typeMatch, onTypeMatchChange)
                MatchTypeChip("Coupe", "coupe", typeMatch, onTypeMatchChange)
                MatchTypeChip("Amical", "amical", typeMatch, onTypeMatchChange)
            }
        }
    }
}

@Composable
private fun TeamField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
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
            modifier = Modifier.fillMaxWidth()
        )
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
