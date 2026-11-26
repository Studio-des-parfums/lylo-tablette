package com.lylo.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lylo.tablet.R
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.LyloPrimary

private const val PERFUME_NAME_MAX_LENGTH = 60

/**
 * Champ « nom du parfum » + bouton de validation — portage de PerfumeNameForm.tsx (mode vocal,
 * avec sous-titre) et de PerfumeNameStep dans quiz/page.tsx (mode visuel, sans sous-titre).
 * Un seul envoi possible : le bouton passe à « Envoyé ! » et le champ se désactive.
 */
@Composable
fun PerfumeNameForm(
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    showSubtitle: Boolean = true,
    translucentField: Boolean = true,
) {
    val strings = LocalStringResolver.current
    var value by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    fun submit() {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || submitted) return
        submitted = true
        onSubmit(trimmed)
    }

    Column(
        modifier = modifier.widthIn(max = 448.dp).fillMaxWidth().padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (showSubtitle) 16.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showSubtitle) {
            Text(
                strings.get(R.string.perfume_name_subtitle).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = LyloPrimary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= PERFUME_NAME_MAX_LENGTH) value = it },
            enabled = !submitted,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(color = LyloPrimary, fontWeight = FontWeight.Light, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .clip(RoundedCornerShape(12.dp))
                .background(if (translucentField) Color.White.copy(alpha = 0.8f) else Color.White)
                .border(1.dp, LyloPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .alpha(if (submitted) 0.6f else 1f)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        strings.get(R.string.perfume_name_placeholder),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Light,
                        color = LyloPrimary.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                inner()
            },
        )
        PrimaryPillButton(
            text = if (submitted) strings.get(R.string.perfume_name_submitted) else strings.get(R.string.perfume_name_submit),
            onClick = { submit() },
            enabled = value.isNotBlank() && !submitted,
            trailingIcon = null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
