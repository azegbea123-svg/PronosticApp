package com.example.pronosticapp.data

import android.content.Context

/**
 * Stockage local très simple du numéro de téléphone de l'utilisateur —
 * sert d'identifiant pour le suivi du quota gratuit et le statut VIP
 * côté backend. Pas de vrai compte/authentification, volontairement
 * minimal pour cette v1.
 */
object PreferencesTelephone {
    private const val PREFS = "pronosticapp_prefs"
    private const val CLE_TELEPHONE = "telephone"

    fun charger(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(CLE_TELEPHONE, "") ?: ""
    }

    fun sauvegarder(context: Context, telephone: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(CLE_TELEPHONE, telephone).apply()
    }
}
