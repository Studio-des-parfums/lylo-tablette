package com.lylo.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lylo.tablet.R
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.LyloPrimary

/** Portage de src/components/interaction/StepProgress.tsx. */
@Composable
fun StepProgress(currentStep: Int, totalSteps: Int, modifier: Modifier = Modifier) {
    val strings = LocalStringResolver.current
    val progress = if (totalSteps > 0) currentStep.toFloat() / totalSteps else 0f

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${strings.get(R.string.interaction_step)} ${currentStep.toString().padStart(2, '0')} / ${totalSteps.toString().padStart(2, '0')}",
            style = MaterialTheme.typography.labelSmall,
            color = LyloPrimary,
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .width(128.dp)
                .height(1.dp)
                .background(LyloPrimary.copy(alpha = 0.2f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(LyloPrimary),
            )
        }
    }
}
