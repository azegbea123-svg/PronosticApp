package com.eeg.pronosticapp.data

import android.content.Context

object PreferencesTelephone {
    private const val PREFS_NAME = "pronosticapp_prefs"
    private const val KEY_TELEPHONE = "telephone_utilisateur"

    fun charger(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TELEPHONE, "") ?: ""
    }

    fun sauvegarder(context: Context, telephone: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TELEPHONE, telephone.trim()).apply()
    }
}
