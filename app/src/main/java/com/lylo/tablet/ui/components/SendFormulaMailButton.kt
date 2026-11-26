package com.lylo.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lylo.tablet.R
import com.lylo.tablet.data.remote.ApiClient
import com.lylo.tablet.data.remote.SendFormulaMailRequest
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.LyloPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Portage de src/components/recommendations/SendFormulaMailButton.tsx : bouton « Recevoir par
 * mail » + modale (prénom/nom/email/société/fonction). Remplace l'ancien email saisi dans la
 * configuration : le visiteur choisit explicitement quand et à quelle adresse recevoir sa formule.
 * Appelle POST /api/formulas/{reference}/send-mail, qui persiste aussi ces infos sur la formule.
 */
@Composable
fun SendFormulaMailButton(
    reference: String,
    modifier: Modifier = Modifier,
    iconOnly: Boolean = true,
    /** Prénom donné à l'agent pendant la session vocale : pré-remplit le champ (modifiable). */
    initialFirstName: String? = null,
) {
    val strings = LocalStringResolver.current
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("idle") } // idle | sending | sent | error

    fun openDialog() {
        // Pré-remplit au moment de l'ouverture (le profil peut arriver après la composition),
        // sans écraser une saisie déjà faite.
        if (firstName.isEmpty() && !initialFirstName.isNullOrBlank()) firstName = initialFirstName
        open = true
    }

    fun close() {
        open = false
        // Laisse le temps à la fermeture avant de réinitialiser le statut (formulaire qui « saute »).
        scope.launch { delay(200); status = "idle" }
    }

    fun submit() {
        if (email.isBlank() || status == "sending") return
        status = "sending"
        scope.launch {
            status = try {
                val res = ApiClient.service.sendFormulaMail(
                    reference,
                    SendFormulaMailRequest(
                        email = email.trim(),
                        firstName = firstName.trim().ifEmpty { null },
                        lastName = lastName.trim().ifEmpty { null },
                        company = company.trim().ifEmpty { null },
                        jobTitle = jobTitle.trim().ifEmpty { null },
                    ),
                )
                if (res.isSuccessful) "sent" else "error"
            } catch (_: Exception) {
                "error"
            }
        }
    }

    if (iconOnly) {
        RoundIconButton(
            icon = Icons.Default.Mail,
            contentDescription = strings.get(R.string.send_mail_button),
            onClick = { openDialog() },
            iconSize = 16,
            modifier = modifier,
        )
    } else {
        PrimaryPillButton(
            text = strings.get(R.string.send_mail_button),
            onClick = { openDialog() },
            trailingIcon = Icons.Default.Mail,
            modifier = modifier,
        )
    }

    if (!open) return

    Dialog(onDismissRequest = { close() }) {
        Column(
            modifier = Modifier
                .widthIn(max = 384.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, LyloPrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.get(R.string.send_mail_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = LyloPrimary)
                    Text(
                        strings.get(R.string.send_mail_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LyloPrimary.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                IconButton(onClick = { close() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = strings.get(R.string.send_mail_close), tint = LyloPrimary.copy(alpha = 0.5f))
                }
            }

            if (status == "sent") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(32.dp))
                    Text(strings.get(R.string.send_mail_sent), color = Color(0xFF15803D), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MailField(firstName, { firstName = it }, strings.get(R.string.send_mail_first_name), Modifier.weight(1f))
                        MailField(lastName, { lastName = it }, strings.get(R.string.send_mail_last_name), Modifier.weight(1f))
                    }
                    MailField(email, { email = it }, strings.get(R.string.send_mail_email), Modifier.fillMaxWidth(), KeyboardType.Email)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MailField(company, { company = it }, strings.get(R.string.send_mail_company), Modifier.weight(1f))
                        MailField(jobTitle, { jobTitle = it }, strings.get(R.string.send_mail_job_title), Modifier.weight(1f))
                    }
                    val canSend = status != "sending" && email.isNotBlank()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp))
                            .background(LyloPrimary)
                            .alpha(if (canSend) 1f else 0.4f)
                            .clickable(enabled = canSend) { submit() }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (status == "sending") {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            if (status == "sending") strings.get(R.string.send_mail_sending) else strings.get(R.string.send_mail_submit),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    if (status == "error") {
                        Text(
                            strings.get(R.string.send_mail_error),
                            color = Color(0xFFEF4444),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MailField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = LyloPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, LyloPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = LyloPrimary.copy(alpha = 0.35f), maxLines = 1)
            inner()
        },
    )
}
