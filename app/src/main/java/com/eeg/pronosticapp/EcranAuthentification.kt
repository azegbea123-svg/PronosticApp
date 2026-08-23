package com.eeg.pronosticapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eeg.pronosticapp.model.ProfilRequest
import com.eeg.pronosticapp.network.RetrofitClient
import com.eeg.pronosticapp.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/** Validation simple : chiffres uniquement, + optionnel au début, 8 à 15 chiffres. */
fun telephoneValide(tel: String): Boolean {
    return Regex("^\\+?[0-9]{8,15}$").matches(tel.trim())
}

@Composable
fun EcranAuthentification() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var modeInscription by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var confirmationMotDePasse by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var enCours by remember { mutableStateOf(false) }
    var erreur by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StadiumNight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "PRONOSTIC",
                color = FloodlightGold,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 3.sp
            )
            Text(
                if (modeInscription) "Créer un compte" else "Connexion",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail", color = TextMuted) },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary),
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

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = motDePasse,
                onValueChange = { motDePasse = it },
                label = { Text("Mot de passe", color = TextMuted) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = TextStyle(color = TextPrimary),
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

            if (modeInscription) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmationMotDePasse,
                    onValueChange = { confirmationMotDePasse = it },
                    label = { Text("Confirmer le mot de passe", color = TextMuted) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = TextStyle(color = TextPrimary),
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

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = telephone,
                    onValueChange = { telephone = it },
                    label = { Text("Numéro de téléphone", color = TextMuted) },
                    placeholder = { Text("Ex : +22890000000", color = TextFaint) },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary),
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
                Text(
                    "Utilisé uniquement pour te contacter si besoin (ex: souci de paiement VIP).",
                    color = TextFaint,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            erreur?.let {
                Spacer(Modifier.height(10.dp))
                NotificationCard(message = it, type = TypeNotification.ERREUR)
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    erreur = null
                    if (email.isBlank() || motDePasse.isBlank()) {
                        erreur = "Remplis tous les champs."
                        return@Button
                    }
                    if (modeInscription) {
                        if (motDePasse != confirmationMotDePasse) {
                            erreur = "Les mots de passe ne correspondent pas."
                            return@Button
                        }
                        if (!telephoneValide(telephone)) {
                            erreur = "Entre un numéro de téléphone valide (8 à 15 chiffres)."
                            return@Button
                        }
                    }

                    enCours = true
                    val auth = FirebaseAuth.getInstance()

                    if (modeInscription) {
                        auth.createUserWithEmailAndPassword(email.trim(), motDePasse)
                            .addOnCompleteListener { t ->
                                if (!t.isSuccessful) {
                                    enCours = false
                                    erreur = traduireErreurFirebase(t.exception)
                                    return@addOnCompleteListener
                                }
                                scope.launch {
                                    try {
                                        val reponse = RetrofitClient.apiService.majProfil(
                                            ProfilRequest(telephone.trim())
                                        )
                                        if (reponse.isSuccessful) {
                                            val uid = t.result?.user?.uid
                                            if (uid != null) {
                                                context.getSharedPreferences("pronosticapp_prefs", android.content.Context.MODE_PRIVATE)
                                                    .edit().putBoolean("telephone_confirme_$uid", true).apply()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Non bloquant
                                    } finally {
                                        enCours = false
                                    }
                                }
                            }
                    } else {
                        // Firebase garde la session ouverte automatiquement après
                        // une connexion réussie — pas besoin de retaper le mot de
                        // passe aux ouvertures suivantes, tant qu'on ne se
                        // déconnecte pas explicitement.
                        auth.signInWithEmailAndPassword(email.trim(), motDePasse)
                            .addOnCompleteListener { t ->
                                enCours = false
                                if (!t.isSuccessful) {
                                    erreur = traduireErreurFirebase(t.exception)
                                }
                            }
                    }
                },
                enabled = !enCours,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FloodlightGold,
                    contentColor = TextOnAccent
                )
            ) {
                if (enCours) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextOnAccent, strokeWidth = 2.dp)
                } else {
                    Text(
                        if (modeInscription) "Créer mon compte" else "Se connecter",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = if (modeInscription) "Déjà un compte ? Se connecter" else "Pas encore de compte ? S'inscrire",
                color = FloodlightGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        modeInscription = !modeInscription
                        erreur = null
                    }
                    .padding(vertical = 8.dp)
            )
        }
    }
}
