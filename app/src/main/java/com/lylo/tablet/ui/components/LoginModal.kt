package com.lylo.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
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
import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lylo.tablet.R
import com.lylo.tablet.auth.GoogleSignInHelper
import com.lylo.tablet.auth.GoogleSignInResult
import com.lylo.tablet.data.model.AuthError
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.ui.theme.RedError
import com.lylo.tablet.ui.theme.RedErrorBg
import kotlinx.coroutines.launch

/**
 * Portage de src/components/auth/LoginModal.tsx. Le bouton Google déclenche le flow natif
 * Credential Manager (GoogleSignInHelper.signIn) au lieu du widget web <GoogleLogin>.
 * Une fois l'ID token obtenu, on récupère l'email (GoogleIdTokenCredential.id, secouru par un
 * décodage JWT manuel) et on appelle onLoginWithEmail — identique au flux web qui décode le
 * JWT côté client puis appelle login(email) contre /lookup.
 */
private fun isValidEmail(value: String): Boolean = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(value)

@Composable
fun LoginModal(
    open: Boolean,
    isLoading: Boolean,
    error: AuthError?,
    onLoginWithEmail: (email: String, onResult: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!open) return
    val strings = LocalStringResolver.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var googleSignInError by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var emailFormError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFDFAF7))
                .padding(28.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Icon(Icons.Default.Close, contentDescription = strings.get(R.string.auth_close), tint = LyloPrimary.copy(alpha = 0.5f))
                }

                Box(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .width(48.dp)
                        .clip(CircleShape)
                        .background(LyloPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = LyloPrimary,
                        modifier = Modifier.padding(12.dp),
                    )
                }

                Text(
                    text = strings.get(R.string.auth_modal_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = LyloPrimary,
                )
                Text(
                    text = strings.get(R.string.auth_modal_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LyloPrimary.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                )

                when {
                    isLoading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.width(18.dp), strokeWidth = 2.dp, color = LyloPrimary)
                        Text(strings.get(R.string.auth_loading), color = LyloPrimary.copy(alpha = 0.6f))
                    }

                    error != AuthError.NO_SESSIONS -> LyloButton(
                        onClick = {
                            googleSignInError = null
                            scope.launch {
                                when (val result = GoogleSignInHelper.signIn(context)) {
                                    is GoogleSignInResult.Success -> {
                                        val email = result.email
                                            ?: com.lylo.tablet.auth.decodeJwtEmail(result.idToken)
                                        if (email != null) {
                                            onLoginWithEmail(email) { success -> if (success) onDismiss() }
                                        } else {
                                            Log.w("LoginModal", "Google sign-in succeeded but no email could be extracted from the ID token")
                                            googleSignInError = strings.get(R.string.auth_error)
                                        }
                                    }
                                    is GoogleSignInResult.Cancelled -> {
                                        // L'utilisateur a fermé la feuille Credential Manager de lui-même :
                                        // pas une vraie erreur, on ne montre rien (fidèle au web).
                                        Log.d("LoginModal", "Google sign-in cancelled: ${result.message}")
                                    }
                                    is GoogleSignInResult.Error -> {
                                        // Cause la plus fréquente en dev : aucun compte Google configuré
                                        // sur l'appareil/l'émulateur (NoCredentialException).
                                        Log.e("LoginModal", "Google sign-in failed: ${result.message}")
                                        googleSignInError = result.message ?: strings.get(R.string.auth_error)
                                    }
                                }
                            }
                        },
                        variant = LyloButtonVariant.OUTLINE,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(strings.get(R.string.auth_google_signin))
                    }
                }

                if (!isLoading && error != AuthError.NO_SESSIONS) {
                    // Séparateur « ou »
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.weight(1f).height(1.dp).background(LyloPrimary.copy(alpha = 0.1f)))
                        Text(
                            strings.get(R.string.auth_or_divider).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = LyloPrimary.copy(alpha = 0.4f),
                        )
                        Box(Modifier.weight(1f).height(1.dp).background(LyloPrimary.copy(alpha = 0.1f)))
                    }

                    // Connexion par email seul (portage du formulaire email de LoginModal.tsx)
                    fun submitEmail() {
                        if (!isValidEmail(email.trim())) {
                            emailFormError = strings.get(R.string.auth_invalid_email)
                            return
                        }
                        emailFormError = null
                        googleSignInError = null
                        onLoginWithEmail(email.trim()) { success -> if (success) onDismiss() }
                    }
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BasicTextField(
                            value = email,
                            onValueChange = { email = it; emailFormError = null },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = LyloPrimary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submitEmail() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, LyloPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            decorationBox = { inner ->
                                if (email.isEmpty()) {
                                    Text(strings.get(R.string.auth_email_placeholder), style = MaterialTheme.typography.bodyMedium, color = LyloPrimary.copy(alpha = 0.35f))
                                }
                                inner()
                            },
                        )
                        emailFormError?.let {
                            Text(it, color = Color(0xFFDC2626), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                        Text(
                            strings.get(R.string.auth_email_submit).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LyloPrimary)
                                .clickable { submitEmail() }
                                .padding(vertical = 12.dp),
                        )
                    }
                }

                if (googleSignInError != null) {
                    Row(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RedErrorBg)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = RedError)
                        Text(googleSignInError ?: "", color = RedError, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (error != null) {
                    val message = when (error) {
                        AuthError.UNAUTHORIZED -> strings.get(R.string.auth_unauthorized)
                        AuthError.NO_SESSIONS -> strings.get(R.string.auth_no_sessions)
                        AuthError.ERROR -> strings.get(R.string.auth_error)
                    }
                    Row(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RedErrorBg)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = RedError)
                        Text(message, color = RedError, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
