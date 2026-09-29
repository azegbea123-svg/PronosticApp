package com.eeg.pronosticapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.window.Dialog
import com.eeg.pronosticapp.data.CompetitionsConnues
import com.eeg.pronosticapp.data.EquipesConnues
import com.eeg.pronosticapp.model.ActiverCodeRequest
import com.eeg.pronosticapp.model.ConfirmationRequest
import com.eeg.pronosticapp.model.MatchAnalysisRequest
import com.eeg.pronosticapp.model.MatchAnalysisResponse
import com.eeg.pronosticapp.model.PaiementRequest
import com.eeg.pronosticapp.model.VipStatutResponse
import com.eeg.pronosticapp.network.RetrofitClient
import com.eeg.pronosticapp.network.MockAnalysisProvider
import com.eeg.pronosticapp.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
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
                    // L'appli occupe 80% de l'écran (largeur ET hauteur),
                    // centrée, quelle que soit la taille de l'écran
                    // (petit téléphone, grand téléphone, tablette...).
                    // Le fond de la Surface ci-dessus reste visible tout
                    // autour, dans les 20% restants.
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .fillMaxHeight(0.9f)
                        ) {
                            AppRacine()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Point d'entrée : bascule entre l'écran de connexion et l'appli
 * principale selon l'état d'authentification Firebase. Toute l'appli
 * nécessite désormais un compte — plus de mode anonyme par téléphone.
 */
@Composable
fun AppRacine() {
    var utilisateurConnecte by remember {
        mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
    }

    DisposableEffect(Unit) {
        val ecouteur = FirebaseAuth.AuthStateListener { auth ->
            utilisateurConnecte = auth.currentUser != null
        }
        FirebaseAuth.getInstance().addAuthStateListener(ecouteur)
        onDispose { FirebaseAuth.getInstance().removeAuthStateListener(ecouteur) }
    }

    if (utilisateurConnecte) {
        PronosticScreen()
    } else {
        EcranAuthentification()
    }
}

@Composable
fun PronosticScreen() {
    var equipe1 by remember { mutableStateOf("") }
    var equipe2 by remember { mutableStateOf("") }
    var typeMatch by remember { mutableStateOf("Ligue 1") }
    var resultat by remember { mutableStateOf<MatchAnalysisResponse?>(null) }
    var enCours by remember { mutableStateOf(false) }
    var erreur by remember { mutableStateOf<String?>(null) }
    var limiteAtteinte by remember { mutableStateOf(false) }
    var pronosticsRestants by remember { mutableStateOf<Int?>(null) }
    val statutVip = resultat?.vip ?: false

    var statutVipComplet by remember { mutableStateOf<VipStatutResponse?>(null) }
    var afficherDialogVip by remember { mutableStateOf(false) }
    var afficherDialogInfo by remember { mutableStateOf(false) }
    var afficherMenuCompte by remember { mutableStateOf(false) }
    var afficherDialogSupprimerCompte by remember { mutableStateOf(false) }
    var afficherCalculateurMises by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    suspend fun rafraichirStatutVip() {
        if (USE_MOCK_DATA) return
        try {
            val reponse = RetrofitClient.apiService.vipStatut()
            if (reponse.isSuccessful) {
                statutVipComplet = reponse.body()
            }
        } catch (e: Exception) {
            // Silencieux : la bannière VIP est un bonus d'affichage, pas
            // grave si elle ne se charge pas (ex: pas encore réveillé Render)
        }
    }

    LaunchedEffect(Unit) {
        rafraichirStatutVip()
    }

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
            HeaderBlock(
                isMock = USE_MOCK_DATA,
                onVipClick = { afficherDialogVip = true },
                onMenuClick = { afficherMenuCompte = true }
            )

            statutVipComplet?.let { s ->
                if (s.vip) {
                    BanniereVip(statut = s)
                }
            }

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
                                    MatchAnalysisRequest(equipe1, equipe2, typeMatch)
                                )
                                if (response.isSuccessful) {
                                    resultat = response.body()
                                    pronosticsRestants = response.body()?.pronosticsRestantsAujourdhui
                                    rafraichirStatutVip()
                                } else if (response.code() == 429) {
                                    limiteAtteinte = true
                                    erreur = "Limite gratuite atteinte pour aujourd'hui."
                                } else {
                                    // Corps de l'erreur inclus pour diagnostiquer précisément
                                    // (ex: 422 = un champ de la requête ne respecte pas le format attendu)
                                    val corpsErreur = response.errorBody()?.string()
                                    erreur = "Erreur serveur : ${response.code()}" +
                                        if (!corpsErreur.isNullOrBlank()) " — $corpsErreur" else ""
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
                    contentColor = TextOnAccent,
                    disabledContainerColor = PanelRaised,
                    disabledContentColor = TextFaint
                )
            ) {
                if (enCours) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = TextOnAccent,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("ANALYSE EN COURS", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                } else {
                    Text("ANALYSER LE MATCH", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            if (!statutVip && pronosticsRestants != null) {
                Text(
                    text = "🎟️ $pronosticsRestants pronostic(s) gratuit(s) restant(s) aujourd'hui",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
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
                            contentColor = TextOnAccent
                        )
                    ) {
                        Text("💎 PASSER VIP — ACCÈS ILLIMITÉ", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }

            resultat?.let { r ->
                ResultBlock(r)

                Button(
                    onClick = {
                        equipe1 = ""
                        equipe2 = ""
                        resultat = null
                        erreur = null
                        limiteAtteinte = false
                        pronosticsRestants = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PanelRaised,
                        contentColor = TextPrimary
                    ),
                    border = BorderStroke(1.dp, PitchLine)
                ) {
                    Text("🧹 EFFACER — NOUVEAU PRONOSTIC", fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                }
            }

            Text(
                "⚠️ Pronostics informatifs uniquement, sans garantie de résultat. Jeu responsable — interdit aux mineurs.",
                color = TextFaint,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }

        if (afficherDialogVip) {
            DialogVip(
                onFerme = { afficherDialogVip = false },
                onVipActive = {
                    limiteAtteinte = false
                    erreur = null
                    scope.launch { rafraichirStatutVip() }
                }
            )
        }

        if (afficherDialogInfo) {
            DialogInformations(onFerme = { afficherDialogInfo = false })
        }

        if (afficherCalculateurMises) {
            DialogCalculateurMises(onFerme = { afficherCalculateurMises = false })
        }

        if (afficherMenuCompte) {
            DialogMenuCompte(
                onFerme = { afficherMenuCompte = false },
                onInformations = {
                    afficherMenuCompte = false
                    afficherDialogInfo = true
                },
                onCalculateurMises = {
                    afficherMenuCompte = false
                    afficherCalculateurMises = true
                },
                onDeconnexion = {
                    afficherMenuCompte = false
                    FirebaseAuth.getInstance().signOut()
                },
                onSupprimerCompte = {
                    afficherMenuCompte = false
                    afficherDialogSupprimerCompte = true
                }
            )
        }

        if (afficherDialogSupprimerCompte) {
            DialogSupprimerCompte(
                onFerme = { afficherDialogSupprimerCompte = false },
                onSupprime = {
                    // Compte supprimé côté serveur : on nettoie aussi la
                    // session locale pour repartir sur l'écran de connexion
                    // proprement.
                    FirebaseAuth.getInstance().signOut()
                    afficherDialogSupprimerCompte = false
                }
            )
        }
    }
}

@Composable
private fun HeaderBlock(isMock: Boolean, onVipClick: () -> Unit, onMenuClick: () -> Unit) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(FloodlightGold)
                        .clickable { onVipClick() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("💎 VIP", color = TextOnAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, PitchLine, RoundedCornerShape(10.dp))
                        .clickable { onMenuClick() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("☰", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
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
private fun BanniereVip(statut: VipStatutResponse) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FloodlightGold.copy(alpha = 0.12f))
            .border(1.dp, FloodlightGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(FloodlightGold),
            contentAlignment = Alignment.Center
        ) {
            Text("💎", fontSize = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "STATUT VIP ACTIF",
                color = FloodlightGoldDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            val jours = statut.vipJoursRestants
            if (jours != null) {
                Text(
                    text = if (jours <= 1) "Expire aujourd'hui" else "Encore $jours jours d'accès illimité",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            statut.vipExpireLeAffichage?.let {
                Text(
                    "Expire le $it",
                    color = TextMuted,
                    fontSize = 11.sp
                )
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
                    .background(FloodlightGold)
                    .border(1.5.dp, PanelNight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("VS", color = TextOnAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TeamField(
                label = "TYPE DE RENCONTRE",
                value = typeMatch,
                onValueChange = onTypeMatchChange,
                accent = FloodlightGoldDim,
                placeholder = "Ex : Ligue 1, Coupe, Amical...",
                suggestionsPour = { CompetitionsConnues.suggestions(it, limite = 5) }
            )
        }
    }
}

@Composable
private fun TeamField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    placeholder: String = "Équipe",
    suggestionsPour: (String) -> List<String> = { EquipesConnues.suggestions(it, limite = 4) }
) {
    var estFocus by remember { mutableStateOf(false) }
    val suggestions = remember(value) { suggestionsPour(value) }
    val afficherSuggestions = estFocus && suggestions.isNotEmpty()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextFaint) },
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
            color = if (isSelected) TextOnAccent else TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    NotificationCard(message = message, type = TypeNotification.ERREUR)
}

/**
 * Retire toute mention de source entre parenthèses en fin de ligne
 * (ex: "(BeSoccer)", "(BeSoccer, intègre déjà...)") avant affichage à
 * l'utilisateur. Purement cosmétique côté appli — le backend continue
 * d'inclure la source dans sa réponse (utile pour le debug admin via
 * /debug/analyser-match), seul l'affichage grand public est nettoyé.
 */
private fun _masquerSource(texte: String): String {
    return texte.replace(Regex("""\s*\((BeSoccer|Sofascore|FlashScore)[^)]*\)\s*$"""), "").trim()
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
                ScoreDigit(value = p1, color = TurfGreen, big = true, align = Alignment.Start, modifier = Modifier.weight(1f), avis = r.avisFiabiliteV1)
                ScoreDigit(value = pNul, color = NeutralDraw, big = false, align = Alignment.CenterHorizontally, modifier = Modifier.weight(0.7f), avis = r.avisFiabiliteNul)
                ScoreDigit(value = p2, color = EmberOrange, big = true, align = Alignment.End, modifier = Modifier.weight(1f), avis = r.avisFiabiliteV2)
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

        // Carte marchés — over/under, BTTS, scores les plus probables.
        // Plus de verrou par contenu : tous les pronostics (gratuits ou
        // VIP) affichent tout. Seule la QUANTITÉ de pronostics par jour
        // distingue gratuit et VIP (3/jour puis VIP pour illimité).
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
                "MARCHÉS AVANCÉS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MarcheStat(
                    label = "+0.5 BUT",
                    valeur = "${(r.probabiliteOver05 * 100).toInt()}%",
                    couleur = FloodlightGoldDim,
                    modifier = Modifier.weight(1f)
                )
                MarcheStat(
                    label = "+1.5 BUT",
                    valeur = "${(r.probabiliteOver15 * 100).toInt()}%",
                    couleur = FloodlightGoldDim,
                    modifier = Modifier.weight(1f)
                )
                MarcheStat(
                    label = "2 MARQUENT",
                    valeur = "${(r.probabiliteBTTS * 100).toInt()}%",
                    couleur = FloodlightGoldDim,
                    modifier = Modifier.weight(1f)
                )
            }

            if (r.scoresProbables.isNotEmpty()) {
                PitchDivider()

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
                    Text(_masquerSource(facteur), color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
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
    modifier: Modifier = Modifier,
    avis: String? = null
) {
    Column(modifier = modifier, horizontalAlignment = align) {
        Text(
            text = "${value.toInt()}%",
            color = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (big) 30.sp else 18.sp
        )
        if (avis != null) {
            Text(
                text = avis,
                color = TextFaint,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                textAlign = if (align == Alignment.End) TextAlign.End else if (align == Alignment.Start) TextAlign.Start else TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
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
private fun DialogMenuCompte(
    onFerme: () -> Unit,
    onInformations: () -> Unit,
    onCalculateurMises: () -> Unit,
    onDeconnexion: () -> Unit,
    onSupprimerCompte: () -> Unit
) {
    val emailUtilisateur = FirebaseAuth.getInstance().currentUser?.email

    Dialog(onDismissRequest = onFerme) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PanelNight)
                .border(1.dp, PitchLine, RoundedCornerShape(16.dp))
                .padding(vertical = 8.dp)
        ) {
            emailUtilisateur?.let {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
                    Text("CONNECTÉ EN TANT QUE", color = TextFaint, fontSize = 10.sp, letterSpacing = 1.sp)
                    Text(it, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                PitchDivider()
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInformations() }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ℹ️", fontSize = 18.sp)
                Spacer(Modifier.width(14.dp))
                Text("Informations", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCalculateurMises() }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🧮", fontSize = 18.sp)
                Spacer(Modifier.width(14.dp))
                Text("Calculateur de mises", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDeconnexion() }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🚪", fontSize = 18.sp)
                Spacer(Modifier.width(14.dp))
                Text("Se déconnecter", color = EmberOrange, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            PitchDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSupprimerCompte() }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🗑️", fontSize = 18.sp)
                Spacer(Modifier.width(14.dp))
                Text("Supprimer mon compte", color = TextFaint, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DialogSupprimerCompte(onFerme: () -> Unit, onSupprime: () -> Unit) {
    var etape by remember { mutableStateOf("confirmation") } // "confirmation" | "traitement" | "echec"
    var messageErreur by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (etape != "traitement") onFerme() }) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(PanelNight)
                .border(1.dp, EmberOrange.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (etape) {
                "confirmation" -> {
                    Text("Supprimer ton compte ?", color = EmberOrange, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        "Cette action est DÉFINITIVE et IRRÉVERSIBLE. Ton compte, ton statut VIP " +
                            "éventuel et tout ton historique de pronostics seront supprimés pour toujours.",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onFerme,
                            colors = ButtonDefaults.buttonColors(containerColor = PanelRaised, contentColor = TextPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                etape = "traitement"
                                scope.launch {
                                    try {
                                        val reponse = RetrofitClient.apiService.supprimerCompte()
                                        if (reponse.isSuccessful) {
                                            onSupprime()
                                        } else {
                                            etape = "echec"
                                            messageErreur = "Erreur serveur (${reponse.code()})."
                                        }
                                    } catch (e: Exception) {
                                        etape = "echec"
                                        messageErreur = "Erreur réseau : ${e.message}"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmberOrange, contentColor = TextOnAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Supprimer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "traitement" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = EmberOrange, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Suppression en cours...", color = TextMuted, fontSize = 13.sp)
                    }
                }
                "echec" -> {
                    NotificationCard(message = messageErreur, type = TypeNotification.ERREUR)
                    Button(
                        onClick = onFerme,
                        colors = ButtonDefaults.buttonColors(containerColor = PanelRaised, contentColor = TextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fermer")
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogCalculateurMises(onFerme: () -> Unit) {
    // Entrée libre par l'utilisateur — les cotes de SES bookmakers, pas
    // celles de l'appli. Pur calcul local, aucun appel réseau nécessaire.
    var cotes by remember { mutableStateOf(listOf("", "")) }
    var budget by remember { mutableStateOf("") }
    var resultat by remember { mutableStateOf<ResultatDutching?>(null) }
    var erreur by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onFerme) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PanelNight)
                .border(1.dp, PitchLine, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Calculateur de mises", color = FloodlightGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Entre les cotes de tes paris (chez tes bookmakers) et le budget total à répartir. " +
                    "La répartition donne le MÊME gain quel que soit le pari qui passe.",
                color = TextMuted,
                fontSize = 12.sp
            )

            cotes.forEachIndexed { index, valeur ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = valeur,
                        onValueChange = { nouveau ->
                            cotes = cotes.toMutableList().also { it[index] = nouveau }
                            resultat = null
                            erreur = null
                        },
                        label = { Text("Cote pari ${index + 1}", color = TextFaint, fontSize = 12.sp) },
                        placeholder = { Text("Ex : 1.64", color = TextFaint) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FloodlightGold,
                            unfocusedBorderColor = PitchLine,
                            focusedContainerColor = PanelRaised,
                            unfocusedContainerColor = PanelRaised,
                            cursorColor = FloodlightGold
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    if (cotes.size > 2) {
                        IconButton(onClick = {
                            cotes = cotes.toMutableList().also { it.removeAt(index) }
                            resultat = null
                        }) {
                            Text("✕", color = EmberOrange, fontSize = 16.sp)
                        }
                    }
                }
            }

            TextButton(onClick = { cotes = cotes + "" }) {
                Text("+ Ajouter une cote", color = TurfGreen, fontSize = 13.sp)
            }

            OutlinedTextField(
                value = budget,
                onValueChange = { budget = it; resultat = null; erreur = null },
                label = { Text("Budget total à répartir", color = TextFaint, fontSize = 12.sp) },
                placeholder = { Text("Ex : 10000", color = TextFaint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

            erreur?.let {
                Text(it, color = EmberOrange, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    val cotesNum = cotes.mapNotNull { it.replace(",", ".").toDoubleOrNull() }
                    val budgetNum = budget.replace(",", ".").toDoubleOrNull()
                    when {
                        cotesNum.size < 2 || cotesNum.size != cotes.size -> {
                            erreur = "Entre une cote valide (> 1.0) pour chaque pari."
                            resultat = null
                        }
                        cotesNum.any { it <= 1.0 } -> {
                            erreur = "Chaque cote doit être un nombre supérieur à 1.0."
                            resultat = null
                        }
                        budgetNum == null || budgetNum <= 0 -> {
                            erreur = "Entre un budget valide, supérieur à 0."
                            resultat = null
                        }
                        else -> {
                            erreur = null
                            resultat = calculerDutching(cotesNum, budgetNum)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FloodlightGold, contentColor = PanelNight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Calculer", fontWeight = FontWeight.Bold)
            }

            resultat?.let { r ->
                PitchDivider()
                Text(
                    "Marge des bookmaker(s) sur ces cotes combinées : ${r.margeBookmakerPourcent}%",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                r.misesParPari.forEachIndexed { i, mise ->
                    Text(
                        "Pari ${i + 1} (cote ${cotes[i]}) → miser ${mise.toInt()}",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }
                Text(
                    "Retour si l'UN des paris passe : ${r.retourSiLunGagne.toInt()}",
                    color = if (r.rentable) TurfGreen else EmberOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    if (r.rentable)
                        "Soit un gain net de ${r.gainNetSiLunGagne.toInt()} par rapport au budget engagé."
                    else
                        "Soit une PERTE de ${-r.gainNetSiLunGagne.toInt()} par rapport au budget engagé — " +
                            "ces cotes combinées ne permettent pas de gain garanti avec ce budget.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            TextButton(onClick = onFerme, modifier = Modifier.fillMaxWidth()) {
                Text("Fermer", color = TextFaint)
            }
        }
    }
}

private data class ResultatDutching(
    val misesParPari: List<Double>,
    val retourSiLunGagne: Double,
    val gainNetSiLunGagne: Double,
    val margeBookmakerPourcent: Double,
    val rentable: Boolean
)

/**
 * Répartit `budget` entre les paris de `cotes` pour un gain IDENTIQUE
 * quel que soit celui qui gagne (dutching) — pur calcul local, aucune
 * dépendance réseau. Même logique que /outils/calculateur-mises côté
 * backend, dupliquée ici pour un calcul instantané sans aller-retour
 * serveur (l'utilisateur entre ses propres cotes de bookmaker, aucune
 * donnée de l'appli n'est nécessaire).
 */
private fun calculerDutching(cotes: List<Double>, budget: Double): ResultatDutching {
    val inverses = cotes.map { 1.0 / it }
    val sommeInverses = inverses.sum()
    val mises = inverses.map { budget * it / sommeInverses }
    val retourEgal = budget / sommeInverses
    val gainNet = retourEgal - budget
    return ResultatDutching(
        misesParPari = mises,
        retourSiLunGagne = retourEgal,
        gainNetSiLunGagne = gainNet,
        margeBookmakerPourcent = ((sommeInverses - 1.0) * 100.0).let { if (it > 0) Math.round(it * 10) / 10.0 else 0.0 },
        rentable = gainNet > 0
    )
}

@Composable
private fun DialogInformations(onFerme: () -> Unit) {
    Dialog(onDismissRequest = onFerme) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(PanelNight)
                .border(1.dp, PitchLine, RoundedCornerShape(16.dp))
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Informations", color = FloodlightGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)

            // Avertissement joueurs — en premier, volontairement mis en avant
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(EmberOrange.copy(alpha = 0.10f))
                    .border(1.dp, EmberOrange.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("⚠️ AVERTISSEMENT — JEU RESPONSABLE", color = EmberOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Les pronostics fournis par cette application sont générés automatiquement à partir " +
                        "de statistiques et sont donnés à titre purement informatif. Ils ne constituent en " +
                        "aucun cas une garantie de résultat, un conseil de pari ou un conseil financier.\n\n" +
                        "Les paris sportifs comportent un risque réel de perte d'argent. Ne pariez jamais " +
                        "plus que ce que vous pouvez vous permettre de perdre.\n\n" +
                        "Cette application est interdite aux mineurs. Si tu penses avoir un problème avec " +
                        "le jeu, parles-en à un proche ou contacte une structure d'aide dédiée près de chez toi.",
                    color = TextPrimary,
                    fontSize = 12.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("À PROPOS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    "PronoFoot analyse des statistiques de matchs (forme récente, confrontations, " +
                        "ELO...) pour générer des pronostics football complets : victoire/nul/victoire, " +
                        "over/under, BTTS et scores probables. 3 pronostics complets gratuits chaque jour, " +
                        "le mode VIP débloque un accès illimité.",
                    color = TextPrimary,
                    fontSize = 12.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("CONFIDENTIALITÉ", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    "Nous collectons ton e-mail (compte) et ton numéro de téléphone (uniquement pour te " +
                        "contacter si besoin, par exemple en cas de souci de paiement VIP). Ton historique " +
                        "de pronostics est conservé pour suivre ton quota gratuit et ton statut VIP. Ces " +
                        "données ne sont jamais vendues ni partagées avec des tiers à des fins publicitaires.",
                    color = TextPrimary,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onFerme,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PanelRaised, contentColor = TextPrimary)
            ) {
                Text("Fermer")
            }

            Text(
                "PronoFoot — version ${com.eeg.pronosticapp.BuildConfig.VERSION_NAME}",
                color = TextFaint,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DialogVip(
    onFerme: () -> Unit,
    onVipActive: () -> Unit
) {
    var reseau by remember { mutableStateOf("TMONEY") }
    var telephonePaiement by remember { mutableStateOf("") }
    var codeSaisi by remember { mutableStateOf("") }
    var afficherActivationCode by remember { mutableStateOf(false) }
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
                        "500 FCFA — accès illimité aux pronostics pendant 3 jours.",
                        color = TextPrimary,
                        fontSize = 14.sp
                    )

                    if (!afficherActivationCode) {
                        Text("Numéro pour le paiement :", color = TextMuted, fontSize = 12.sp)
                        OutlinedTextField(
                            value = telephonePaiement,
                            onValueChange = { telephonePaiement = it },
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

                        Text("Choisis ton opérateur :", color = TextMuted, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MatchTypeChip("TMoney", "TMONEY", reseau, { reseau = it })
                            MatchTypeChip("Moov Money", "FLOOZ", reseau, { reseau = it })
                        }

                        Button(
                            onClick = {
                                if (telephonePaiement.isBlank()) {
                                    message = "Entre ton numéro pour continuer."
                                    return@Button
                                }
                                etape = "attente"
                                message = "Paiement en cours..."
                                scope.launch {
                                    try {
                                        val payReponse = RetrofitClient.apiService.vipPayer(
                                            PaiementRequest(telephonePaiement.trim(), reseau)
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
                                        // Vérification toutes les 2s (au lieu de 5s) : détecte la
                                        // confirmation bien plus vite en moyenne, sans changer le
                                        // plafond total (25 x 2s = 50s, comparable à avant).
                                        val tentativesMax = 25

                                        while (!confirme && tentative < tentativesMax) {
                                            delay(2000)
                                            tentative++
                                            val confirmReponse = RetrofitClient.apiService.vipConfirmer(
                                                ConfirmationRequest(txReference)
                                            )
                                            if (confirmReponse.isSuccessful && confirmReponse.body()?.confirme == true) {
                                                confirme = true
                                            }
                                        }

                                        if (confirme) {
                                            etape = "succes"
                                            message = "VIP activé pour 3 jours 🎉"
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
                                contentColor = TextOnAccent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Payer 500 FCFA", fontWeight = FontWeight.Bold)
                        }

                        message.takeIf { it.isNotBlank() }?.let {
                            NotificationCard(message = it, type = TypeNotification.ERREUR)
                        }

                        Text(
                            text = "J'ai déjà un code d'activation",
                            color = FloodlightGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { afficherActivationCode = true; message = "" }
                                .padding(vertical = 6.dp)
                        )
                    } else {
                        Text("Code d'activation :", color = TextMuted, fontSize = 12.sp)
                        OutlinedTextField(
                            value = codeSaisi,
                            onValueChange = { codeSaisi = it.uppercase() },
                            placeholder = { Text("Ex : AB12CD34", color = TextFaint) },
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
                            onClick = {
                                if (codeSaisi.isBlank()) {
                                    message = "Entre un code."
                                    return@Button
                                }
                                etape = "attente"
                                message = "Vérification du code..."
                                scope.launch {
                                    try {
                                        val reponse = RetrofitClient.apiService.vipActiverCode(
                                            ActiverCodeRequest(codeSaisi.trim())
                                        )
                                        if (reponse.isSuccessful && reponse.body()?.active == true) {
                                            etape = "succes"
                                            message = "VIP activé 🎉"
                                            onVipActive()
                                        } else {
                                            etape = "echec"
                                            message = when (reponse.code()) {
                                                404 -> "Code invalide."
                                                409 -> "Ce code a déjà été utilisé."
                                                else -> "Erreur lors de l'activation."
                                            }
                                        }
                                    } catch (e: Exception) {
                                        etape = "echec"
                                        message = "Erreur réseau : ${e.message}"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FloodlightGold,
                                contentColor = TextOnAccent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Activer le code", fontWeight = FontWeight.Bold)
                        }

                        message.takeIf { it.isNotBlank() }?.let {
                            NotificationCard(message = it, type = TypeNotification.ERREUR)
                        }

                        Text(
                            text = "← Revenir au paiement",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { afficherActivationCode = false; message = "" }
                                .padding(vertical = 6.dp)
                        )
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
                    NotificationCard(message = message, type = TypeNotification.SUCCES)
                    Button(
                        onClick = onFerme,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TurfGreen,
                            contentColor = TextOnAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fermer", fontWeight = FontWeight.Bold)
                    }
                }

                "echec" -> {
                    NotificationCard(message = message, type = TypeNotification.ERREUR)
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
