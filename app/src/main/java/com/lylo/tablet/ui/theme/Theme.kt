package com.lylo.tablet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// La version web force le thème "light" (html className="light" en dur dans layout.tsx) —
// on reproduit ce choix : Lylo Tablet reste toujours en palette claire, quel que soit le thème
// système de la tablette, pour garantir une fidélité visuelle totale au site.
private val LyloLightColorScheme = lightColorScheme(
    primary = LyloPrimary,
    onPrimary = LyloBrandLight,
    secondary = LyloSecondary,
    onSecondary = LyloTextDark,
    background = LyloBackgroundLight,
    onBackground = LyloTextDark,
    surface = LyloBrandLight,
    onSurface = LyloTextDark,
    error = RedError,
    onError = LyloBrandLight,
)

private val LyloDarkColorScheme = darkColorScheme(
    primary = LyloPrimary,
    onPrimary = LyloBrandLight,
    secondary = LyloSecondary,
    onSecondary = LyloTextDark,
    background = LyloBackgroundDark,
    onBackground = LyloBrandLight,
    surface = Color(0xFF241F1C),
    onSurface = LyloBrandLight,
    error = RedError,
    onError = LyloBrandLight,
)

@Composable
fun LyloTabletTheme(
    useDarkTheme: Boolean = false, // volontairement figé en clair, comme la version web
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) LyloDarkColorScheme else LyloLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LyloTypography,
        shapes = LyloShapes,
        content = content,
    )
}
