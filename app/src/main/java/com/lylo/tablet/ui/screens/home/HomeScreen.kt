package com.lylo.tablet.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.layout.height
import coil3.compose.AsyncImage
import com.lylo.tablet.R
import com.lylo.tablet.data.Brand
import com.lylo.tablet.data.rememberSecondaryLogoUrl
import com.lylo.tablet.data.model.AuthUser
import com.lylo.tablet.i18n.AppLanguage
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.components.LyloButton
import com.lylo.tablet.ui.components.LyloButtonSize
import com.lylo.tablet.ui.components.LyloButtonVariant
import com.lylo.tablet.ui.components.Navbar
import com.lylo.tablet.ui.theme.LyloPrimary

/** Portage de src/app/page.tsx : Navbar + HeroSection + TechBar. */
@Composable
fun HomeScreen(
    user: AuthUser?,
    currentLanguage: AppLanguage,
    onLanguageSelect: (AppLanguage) -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onGetStartedClick: () -> Unit,
    onEnterSessionCleanup: () -> Unit,
    onFormulasClick: () -> Unit = {},
    onRobotSettingsClick: () -> Unit = {},
) {
    val strings = LocalStringResolver.current

    LaunchedEffect(Unit) { onEnterSessionCleanup() }
    // Message d'accueil vocal à la détection d'une personne : uniquement sur cette page.
    LaunchedEffect(currentLanguage) { com.lylo.tablet.robot.GreetingPlayer.language = currentLanguage.code }
    androidx.compose.runtime.DisposableEffect(Unit) {
        com.lylo.tablet.robot.GreetingPlayer.enabled = true
        onDispose {
            com.lylo.tablet.robot.GreetingPlayer.enabled = false
            com.lylo.tablet.robot.GreetingPlayer.stop()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Navbar(
            user = user,
            currentLanguage = currentLanguage,
            onLanguageSelect = onLanguageSelect,
            onLoginClick = onLoginClick,
            onLogoutClick = onLogoutClick,
            onGetStartedClick = onGetStartedClick,
            onFormulasClick = onFormulasClick,
            onRobotSettingsClick = onRobotSettingsClick,
        )

        Column(modifier = Modifier.weight(1f)) {
            HeroSection(
                user = user,
                currentLanguage = currentLanguage,
                onGetStartedClick = onGetStartedClick,
                onLoginClick = onLoginClick,
                modifier = Modifier.weight(1f),
            )
            TechBar()
        }
    }
}

@Composable
private fun HeroSection(
    user: AuthUser?,
    currentLanguage: AppLanguage,
    onGetStartedClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStringResolver.current
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Portage de HeroSection.tsx handlePlayAudio : intro-girl-fr.wav en FR, intro-girl-en.wav sinon
    // (LYLO_INTRO_AUDIO_BY_LOCALE ne couvre que fr/en, repli sur l'anglais).
    fun playOrStopIntro() {
        mediaPlayer?.let {
            it.stop()
            it.release()
            mediaPlayer = null
        }
        if (isPlaying) {
            isPlaying = false
            return
        }
        val assetName = if (currentLanguage == AppLanguage.FR) "audio/intro_girl_fr.wav" else "audio/intro_girl_en.wav"
        runCatching {
            val afd = context.assets.openFd(assetName)
            MediaPlayer().apply {
                // Attributs explicites usage=MEDIA/content=SPEECH : sans ça, MediaPlayer utilise
                // USAGE_UNKNOWN par défaut, ce qui fonctionne la plupart du temps mais laisse le
                // routage de sortie (haut-parleur vs Bluetooth) moins prévisible que côté web où
                // l'élément <audio> est toujours traité comme un média classique.
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                setOnCompletionListener { isPlaying = false }
                start()
            }
        }.onSuccess {
            mediaPlayer = it
            isPlaying = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { mediaPlayer?.release() }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // Portage de .hero-gradient (globals.css) : background-lylo.png + assombrissement.
        Image(
            painter = painterResource(id = R.drawable.background_lylo),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(listOf(com.lylo.tablet.ui.theme.LyloBackgroundDark.copy(alpha = 0.4f), com.lylo.tablet.ui.theme.LyloBackgroundDark.copy(alpha = 0.6f))),
                ),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            // Logo partenaire (dashboard) s'il existe — le logo de la marque n'est plus répété ici.
            val secondaryLogoUrl by rememberSecondaryLogoUrl()
            secondaryLogoUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Logo partenaire",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(80.dp).padding(bottom = 16.dp),
                )
            }

            Text(
                Brand.active.name,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Light,
            )
            Text(
                strings.get(R.string.home_by_word),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
            Text(
                strings.get(R.string.home_title),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
            )

            Row(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(LyloPrimary))
                Text(strings.get(R.string.home_badge), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }

            Row(modifier = Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (user != null) {
                    LyloButton(onClick = onGetStartedClick, size = LyloButtonSize.LARGE) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Text(strings.get(R.string.home_get_started), modifier = Modifier.padding(start = 8.dp))
                    }
                } else {
                    LyloButton(onClick = onLoginClick, size = LyloButtonSize.LARGE) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Text(strings.get(R.string.nav_login), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                LyloButton(onClick = { playOrStopIntro() }, variant = LyloButtonVariant.GHOST, size = LyloButtonSize.LARGE) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayCircle, contentDescription = null)
                    Text(
                        if (isPlaying) strings.get(R.string.home_playing) else strings.get(R.string.home_introduction),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TechBar() {
    val strings = LocalStringResolver.current
    // Logo partenaire propre à la marque (Help : Les Français avancent).
    val partnerLogo = Brand.active.partnerLogo
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.5f))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            if (partnerLogo != null) {
                Image(
                    painter = painterResource(id = partnerLogo.res),
                    contentDescription = partnerLogo.alt,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(40.dp),
                )
            }
            TechItem(Icons.Default.AutoAwesome, "Mistral")
            TechItem(Icons.Default.CloudQueue, "AWS")
            TechItem(Icons.Default.Psychology, "OpenAI")
            TechItem(Icons.Default.Search, "Google")
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.4f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(LyloPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(strings.get(R.string.tech_bar_rose_assistant), style = MaterialTheme.typography.labelSmall, color = LyloPrimary)
                Text("“${strings.get(R.string.tech_bar_rose_message)}”", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TechItem(icon: androidx.compose.ui.graphics.vector.ImageVector, name: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = Color(0xFF6B7280).copy(alpha = 0.6f))
        Text(name, style = MaterialTheme.typography.titleSmall, color = Color(0xFF6B7280).copy(alpha = 0.6f))
    }
}
