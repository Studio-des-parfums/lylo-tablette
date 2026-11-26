package com.lylo.tablet.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage
import com.lylo.tablet.data.Brand
import com.lylo.tablet.data.rememberSecondaryLogoUrl
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lylo.tablet.R
import com.lylo.tablet.data.model.AuthUser
import com.lylo.tablet.i18n.AppLanguage
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.ui.theme.glassNav

/** Compte de l'équipe qui a accès à la page /formulas (recherche des formules générées). */
private const val FORMULAS_ACCESS_EMAIL = "it@sdp-paris.com"

/** Portage de src/components/layout/Navbar.tsx. */
@Composable
fun Navbar(
    modifier: Modifier = Modifier,
    showActions: Boolean = true,
    transparent: Boolean = false,
    user: AuthUser? = null,
    currentLanguage: AppLanguage = AppLanguage.FR,
    onLogoClick: () -> Unit = {},
    onLanguageSelect: (AppLanguage) -> Unit = {},
    onLoginClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onGetStartedClick: () -> Unit = {},
    onFormulasClick: () -> Unit = {},
    onRobotSettingsClick: () -> Unit = {},
) {
    val strings = LocalStringResolver.current
    val secondaryLogoUrl by rememberSecondaryLogoUrl()
    val brand = Brand.active

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (!transparent) Modifier.glassNav() else Modifier)
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── Logo — équivalent du <Link href="/"> React : navigue vers l'accueil ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable(onClick = onLogoClick),
            ) {
                Image(
                    painter = painterResource(id = brand.logo),
                    contentDescription = "Logo ${brand.name}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(brand.navLogoHeight.dp),
                )
                secondaryLogoUrl?.let { url ->
                    Text("×", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Light, color = LyloPrimary.copy(alpha = 0.4f))
                    AsyncImage(
                        model = url,
                        contentDescription = "Logo partenaire",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(brand.navLogoHeight.dp),
                    )
                }
                if (!showActions) {
                    Text(
                        text = strings.get(R.string.nav_home),
                        style = MaterialTheme.typography.labelMedium,
                        color = LyloPrimary.copy(alpha = 0.5f),
                    )
                }
            }

            // ── Right side ──
            if (showActions) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MicCalibrator()
                    if (user?.email == FORMULAS_ACCESS_EMAIL) {
                        IconButton(onClick = onFormulasClick, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Science, contentDescription = "Formulas", tint = LyloPrimary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onRobotSettingsClick, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.SmartToy, contentDescription = strings.get(R.string.robot_settings_title), tint = LyloPrimary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                    }
                    LanguageSelector(current = currentLanguage, onSelect = onLanguageSelect)

                    if (user != null) {
                        Text(
                            text = "${strings.get(R.string.auth_welcome)}, ${user.firstName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LyloPrimary.copy(alpha = 0.7f),
                        )
                        LyloButton(onClick = onLogoutClick, variant = LyloButtonVariant.OUTLINE) {
                            Text(strings.get(R.string.auth_logout))
                        }
                        LyloButton(onClick = onGetStartedClick, variant = LyloButtonVariant.PRIMARY) {
                            Text(strings.get(R.string.nav_get_started))
                        }
                    } else {
                        LyloButton(onClick = onLoginClick, variant = LyloButtonVariant.OUTLINE) {
                            Text(strings.get(R.string.nav_login))
                        }
                    }
                }
            }
        }
    }
}
