package com.eeg.pronosticapp.ui.theme

import androidx.compose.ui.graphics.Color

// Fond — clair, aéré
val StadiumNight = Color(0xFFF7F8FA)   // fond général de l'appli (nom conservé pour compat, mais clair désormais)
val PanelNight = Color(0xFFFFFFFF)     // fond des cartes
val PanelRaised = Color(0xFFF1F3F6)    // fond des champs de saisie / éléments "en creux"
val PitchLine = Color(0xFFE2E5EA)      // bordures, séparateurs

// Accent principal — doré
val FloodlightGold = Color(0xFFE8A93A)
val FloodlightGoldDim = Color(0xFFC98F22)

// Couleurs équipes
val TurfGreen = Color(0xFF2F9E52)   // domicile
val EmberOrange = Color(0xFFE0632E) // extérieur
val NeutralDraw = Color(0xFF8A94A3) // nul

// Texte
val TextPrimary = Color(0xFF1B1F27)  // texte principal, foncé sur fond clair
val TextMuted = Color(0xFF6B7280)
val TextFaint = Color(0xFF9AA3B0)

// Texte/icônes posés sur un fond doré ou coloré (boutons, badges) — a
// besoin d'un fort contraste, distinct du fond de page qui est clair.
val TextOnAccent = Color(0xFF1B1F27)
