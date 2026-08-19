package com.example.pronosticapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PronosticColorScheme = darkColorScheme(
    primary = FloodlightGold,
    onPrimary = StadiumNight,
    secondary = TurfGreen,
    onSecondary = StadiumNight,
    tertiary = EmberOrange,
    onTertiary = StadiumNight,
    background = StadiumNight,
    onBackground = TextPrimary,
    surface = PanelNight,
    onSurface = TextPrimary,
    surfaceVariant = PanelRaised,
    onSurfaceVariant = TextMuted,
    outline = PitchLine,
    error = EmberOrange,
    onError = TextPrimary
)

@Composable
fun PronosticAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PronosticColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
