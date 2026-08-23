package com.eeg.pronosticapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eeg.pronosticapp.ui.theme.EmberOrange
import com.eeg.pronosticapp.ui.theme.FloodlightGoldDim
import com.eeg.pronosticapp.ui.theme.TurfGreen
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/**
 * Système de notification unique pour toute l'appli — un seul style,
 * utilisé partout (écran de connexion, résultats, dialog VIP...) pour
 * une expérience cohérente plutôt que des messages d'erreur bruts.
 */
enum class TypeNotification { ERREUR, SUCCES, INFO }

@Composable
fun NotificationCard(message: String, type: TypeNotification, modifier: Modifier = Modifier) {
    val couleurAccent = when (type) {
        TypeNotification.ERREUR -> EmberOrange
        TypeNotification.SUCCES -> TurfGreen
        TypeNotification.INFO -> FloodlightGoldDim
    }
    val icone = when (type) {
        TypeNotification.ERREUR -> "⚠"
        TypeNotification.SUCCES -> "✓"
        TypeNotification.INFO -> "ℹ"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(couleurAccent.copy(alpha = 0.10f))
            .border(1.dp, couleurAccent.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(couleurAccent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icone, fontSize = 13.sp, color = couleurAccent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(message, color = couleurAccent, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

/**
 * Traduit une exception Firebase Auth brute en message clair et
 * compréhensible, en français. Sans ça, une simple coupure réseau
 * affiche un message technique qui donne l'impression que l'appli est
 * cassée — cas vécu concrètement (souci DNS local ayant bloqué l'accès
 * aux serveurs Google, pris à tort pour un bug de l'appli).
 */
fun traduireErreurFirebase(exception: Throwable?): String {
    return when (exception) {
        is FirebaseNetworkException ->
            "Impossible de joindre le service — vérifie ta connexion internet ou réessaie dans quelques instants"

        is FirebaseAuthInvalidCredentialsException ->
            "E-mail ou mot de passe incorrect"

        is FirebaseAuthInvalidUserException ->
            "Aucun compte trouvé avec cet e-mail"

        is FirebaseAuthUserCollisionException ->
            "Un compte existe déjà avec cette adresse e-mail"

        is FirebaseAuthWeakPasswordException ->
            "Mot de passe trop court (6 caractères minimum)"

        null -> "Une erreur est survenue, réessaie"

        else -> {
            val message = exception.message.orEmpty()
            when {
                // Repli sur le contenu du message quand le type précis
                // d'exception n'est pas reconnu ci-dessus (ex: certaines
                // erreurs réseau ne remontent pas toujours en
                // FirebaseNetworkException selon le SDK/la version).
                message.contains("network", ignoreCase = true) ||
                    message.contains("timeout", ignoreCase = true) ||
                    message.contains("unable to resolve host", ignoreCase = true) ->
                    "Impossible de joindre le service — vérifie ta connexion internet ou réessaie dans quelques instants"

                message.contains("too many requests", ignoreCase = true) ||
                    message.contains("blocked", ignoreCase = true) ->
                    "Trop de tentatives — réessaie dans quelques minutes"

                message.contains("badly formatted", ignoreCase = true) ->
                    "Adresse e-mail invalide"

                else -> "Une erreur est survenue : $message"
            }
        }
    }
}
