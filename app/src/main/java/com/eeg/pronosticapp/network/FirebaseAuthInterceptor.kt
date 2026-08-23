package com.eeg.pronosticapp.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Ajoute automatiquement l'en-tête "Authorization: Bearer <jeton>" à
 * chaque requête sortante, à partir de l'utilisateur Firebase
 * actuellement connecté. Aucun appel individuel n'a besoin de s'en
 * soucier (voir ApiService.kt).
 *
 * `getIdToken()` est un appel asynchrone côté Firebase, mais les
 * intercepteurs OkHttp s'exécutent déjà sur un thread d'arrière-plan
 * dédié — l'attendre ici de façon bloquante (Tasks.await) est le
 * schéma standard recommandé pour ce genre d'intégration.
 */
class FirebaseAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requeteOriginale = chain.request()
        val utilisateur = FirebaseAuth.getInstance().currentUser

        val jeton = if (utilisateur != null) {
            try {
                Tasks.await(utilisateur.getIdToken(false)).token
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        val requete = if (jeton != null) {
            requeteOriginale.newBuilder()
                .addHeader("Authorization", "Bearer $jeton")
                .build()
        } else {
            requeteOriginale
        }

        return chain.proceed(requete)
    }
}
