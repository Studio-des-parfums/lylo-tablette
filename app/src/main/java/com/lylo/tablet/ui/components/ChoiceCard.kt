package com.lylo.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lylo.tablet.ui.theme.LyloPrimary

/**
 * Portage de src/components/interaction/ChoiceCard.tsx — carte cliquable avec image de fond
 * (notes olfactives, quiz visuel), label en bas et coche de sélection centrale.
 */
@Composable
fun ChoiceCard(
    name: String,
    modifier: Modifier = Modifier,
    /** Numéro affiché dans une pastille (l'utilisateur peut dire « le 4 »). */
    number: Int? = null,
    imageUrl: String? = null,
    selected: Boolean = false,
    clickable: Boolean = true,
    onSelect: (String) -> Unit = {},
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (selected) LyloPrimary else Color.Transparent

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFFE7E5E4))
            .border(2.dp, borderColor, shape)
            .then(if (clickable) Modifier.clickable { onSelect(name) } else Modifier),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (selected) LyloPrimary.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF292524), Color(0xFF1C1917))),
                    ),
            )
        }

        if (number != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(32.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(if (selected) LyloPrimary else Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(),
                    color = if (selected) Color.White else Color(0xFF1C1917),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }

        // Label en bas
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.3f))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center,
            )
        }

        if (selected) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(LyloPrimary)
                        .padding(10.dp),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}
