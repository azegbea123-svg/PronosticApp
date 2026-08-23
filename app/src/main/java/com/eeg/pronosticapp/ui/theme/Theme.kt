package com.eeg.pronosticapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PronosticColorScheme = lightColorScheme(
    primary = FloodlightGold,
    onPrimary = TextOnAccent,
    secondary = TurfGreen,
    onSecondary = TextOnAccent,
    tertiary = EmberOrange,
    onTertiary = TextOnAccent,
    background = StadiumNight,
    onBackground = TextPrimary,
    surface = PanelNight,
    onSurface = TextPrimary,
    surfaceVariant = PanelRaised,
    onSurfaceVariant = TextMuted,
    outline = PitchLine,
    error = EmberOrange,
    onError = TextOnAccent
)

@Composable
fun PronosticAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PronosticColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
