package com.lylo.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lylo.tablet.R
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.GreenSuccess
import com.lylo.tablet.ui.theme.GreenSuccessBg
import com.lylo.tablet.ui.theme.GreenSuccessBorder
import com.lylo.tablet.ui.theme.RedError
import com.lylo.tablet.ui.theme.RedErrorBg
import com.lylo.tablet.ui.theme.RedErrorBorder
import com.lylo.tablet.ui.theme.StoneGray400

data class ConfirmChoice(val label: String, val image: String? = null)

/** Portage de src/components/interaction/AnswerConfirmation.tsx. */
@Composable
fun AnswerConfirmation(
    top2: List<ConfirmChoice>,
    bottom2: List<ConfirmChoice>,
    modifier: Modifier = Modifier,
    /** Libellés du questionnaire (format de réponse) ; défaut : « Vos préférés » / « Les moins aimés ». */
    positiveLabel: String = "",
    negativeLabel: String = "",
) {
    val strings = LocalStringResolver.current
    val hasBottom = bottom2.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            text = strings.get(R.string.confirmation_subtitle),
            style = MaterialTheme.typography.labelMedium,
            color = com.lylo.tablet.ui.theme.LyloPrimary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = positiveLabel.ifEmpty { strings.get(R.string.confirmation_preferred) }.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = com.lylo.tablet.ui.theme.LyloPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                top2.forEach { ConfirmCard(item = it, isTop = true) }
            }
            if (hasBottom) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = negativeLabel.ifEmpty { strings.get(R.string.confirmation_least_favored) }.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = StoneGray400,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    bottom2.forEach { ConfirmCard(item = it, isTop = false) }
                }
            }
        }
    }
}

@Composable
private fun ConfirmCard(item: ConfirmChoice, isTop: Boolean) {
    val borderColor = if (isTop) GreenSuccessBorder else RedErrorBorder
    val bg = if (isTop) GreenSuccessBg else RedErrorBg
    val textColor = if (isTop) GreenSuccess else RedError
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(width = 2.dp, color = borderColor, shape = shape),
    ) {
        if (item.image != null) {
            AsyncImage(
                model = item.image,
                contentDescription = item.label,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                contentScale = ContentScale.Crop,
            )
        }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .padding(vertical = 10.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Text(text = item.label, color = textColor, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}
