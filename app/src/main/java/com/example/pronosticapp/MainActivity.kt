package com.example.pronosticapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pronosticapp.model.MatchAnalysisRequest
import com.example.pronosticapp.model.MatchAnalysisResponse
import com.example.pronosticapp.network.RetrofitClient
import com.example.pronosticapp.network.MockAnalysisProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 🔧 INTERRUPTEUR : true = données simulées (aucun backend requis, pour tester l'appli
// dès maintenant), false = appels réels vers le backend une fois celui-ci déployé.
const val USE_MOCK_DATA = true

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Analyse de match", style = MaterialTheme.typography.headlineSmall)

        if (USE_MOCK_DATA) {
            Text(
                "Mode test : résultats simulés (backend non connecté)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        OutlinedTextField(
            value = equipe1,
            onValueChange = { equipe1 = it },
            label = { Text("Équipe 1") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = equipe2,
            onValueChange = { equipe2 = it },
            label = { Text("Équipe 2") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = typeMatch,
            onValueChange = { typeMatch = it },
            label = { Text("Type de match (championnat, coupe, amical...)") },
            modifier = Modifier.fillMaxWidth()
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (enCours) "Analyse en cours..." else "Analyser")
        }

        erreur?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        resultat?.let { r ->
            Divider()
            Text("Résultat", style = MaterialTheme.typography.titleMedium)
            Text("${r.equipe1} : ${(r.probabiliteVictoireEquipe1 * 100).toInt()}%")
            Text("Nul : ${(r.probabiliteMatchNul * 100).toInt()}%")
            Text("${r.equipe2} : ${(r.probabiliteVictoireEquipe2 * 100).toInt()}%")
            Spacer(Modifier.height(8.dp))
            Text("Facteurs clés :", style = MaterialTheme.typography.titleSmall)
            r.facteursCles.forEach { Text("• $it") }
            Spacer(Modifier.height(8.dp))
            Text(r.resumeAnalyse)
        }
    }
}
