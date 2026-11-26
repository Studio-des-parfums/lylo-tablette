package com.lylo.tablet.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.lylo.tablet.R

// ── Police d'affichage : EB Garamond (--font-display / --font-serif côté web) ──
// Chargée via le Google Fonts Provider (androidx.compose.ui.text.googlefonts). Nécessite que
// Google Play Services soit disponible sur la tablette (cas standard) ; le certificat de
// vérification du provider est déclaré dans res/values/font_certs.xml
// (com_google_android_gms_fonts_certs), généré à partir du snippet officiel Android.
private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val ebGaramondGoogleFont = GoogleFont(name = "EB Garamond")

// FontFamily EB Garamond avec plusieurs graisses, résolue à l'exécution via le provider.
// Compose retombe silencieusement sur le typeface système par défaut si le provider est
// indisponible (pas de Play Services / pas de réseau) — pas de crash, juste un rendu non-serif
// tant que la police n'est pas mise en cache.
val EbGaramondFamily = FontFamily(
    Font(googleFont = ebGaramondGoogleFont, fontProvider = googleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = ebGaramondGoogleFont, fontProvider = googleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = ebGaramondGoogleFont, fontProvider = googleFontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = ebGaramondGoogleFont, fontProvider = googleFontsProvider, weight = FontWeight.Bold),
    Font(googleFont = ebGaramondGoogleFont, fontProvider = googleFontsProvider, weight = FontWeight.ExtraBold),
)

// Fallback système utilisé pour composer un style avant que la police distante soit résolue,
// et documentaire pour tout appelant qui préfère explicitement un serif générique.
val EbGaramondFallback = FontFamily.Serif

// Helper : exprime un letter-spacing en "em" relatif à une taille de police en sp.
private fun em(value: Double, fontSizeSp: Int) = (value * fontSizeSp).sp

val LyloTypography = Typography(
    displayLarge = TextStyle(fontFamily = EbGaramondFamily, fontWeight = FontWeight.Light, fontSize = 57.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = EbGaramondFamily, fontWeight = FontWeight.Light, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = EbGaramondFamily, fontWeight = FontWeight.Light, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = EbGaramondFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.02).sp),
    headlineMedium = TextStyle(fontFamily = EbGaramondFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = EbGaramondFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = EbGaramondFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = em(0.05, 22)),
    titleMedium = TextStyle(fontFamily = EbGaramondFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleSmall = TextStyle(fontFamily = EbGaramondFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = EbGaramondFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = EbGaramondFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = EbGaramondFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = EbGaramondFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = em(0.1, 14)),
    labelMedium = TextStyle(fontFamily = EbGaramondFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = em(0.15, 12)),
    labelSmall = TextStyle(fontFamily = EbGaramondFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = em(0.2, 10)),
)

// ── luxury-title (globals.css: letter-spacing 0.05em; color #333) ──────────
val LuxuryTitleStyle = TextStyle(
    fontFamily = EbGaramondFamily,
    letterSpacing = em(0.05, 20),
    color = LyloLuxuryTitle,
)

// ── brand-text (globals.css: letter-spacing 0.2em; uppercase; font-size 0.8rem) ────
val BrandTextStyle = TextStyle(
    letterSpacing = em(0.2, 13),
    fontSize = 13.sp,
)
