package com.lylo.tablet.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.lylo.tablet.BuildConfig

/**
 * Portage de src/components/auth/LoginModal.tsx (`@react-oauth/google` GoogleLogin) via
 * Credential Manager API + Google Identity Services pour Android — remplace le SDK web.
 *
 * Important : [GetGoogleIdOption.serverClientId] doit être le **Web Client ID** OAuth
 * (type "Web application" dans Google Cloud Console), PAS un Client ID Android. C'est le même
 * identifiant que NEXT_PUBLIC_GOOGLE_CLIENT_ID côté web (voir BuildConfig.GOOGLE_WEB_CLIENT_ID,
 * injecté depuis local.properties — voir README.md). Le backend valide ensuite ce même token
 * d'identité Google (ID token JWT), donc l'app Android doit utiliser exactement le même
 * Web Client ID que le front web pour que la vérification côté serveur fonctionne.
 */
sealed interface GoogleSignInResult {
    data class Success(val idToken: String, val email: String?) : GoogleSignInResult
    data class Cancelled(val message: String?) : GoogleSignInResult
    data class Error(val message: String?) : GoogleSignInResult
}

object GoogleSignInHelper {

    /**
     * Déclenche le flow natif Credential Manager pour se connecter avec un compte Google
     * existant sur l'appareil. Doit être appelé depuis une coroutine (suspend fun),
     * typiquement dans un LaunchedEffect ou un rememberCoroutineScope().launch { } au clic sur
     * le bouton "Se connecter avec Google" de LoginModal.
     */
    suspend fun signIn(context: Context): GoogleSignInResult {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // équivalent du bouton "Continue with" générique
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            parseCredential(result.credential)
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled(e.message)
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Error(e.message)
        }
    }

    private fun parseCredential(credential: androidx.credentials.Credential): GoogleSignInResult {
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleSignInResult.Error("Unexpected credential type: ${credential.type}")
        }
        return try {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleSignInResult.Success(
                idToken = googleIdTokenCredential.idToken,
                email = googleIdTokenCredential.id, // "id" du GoogleIdTokenCredential = l'email du compte
            )
        } catch (e: GoogleIdTokenParsingException) {
            GoogleSignInResult.Error("Invalid Google ID token: ${e.message}")
        }
    }
}

/**
 * Décode la partie payload d'un JWT (id token Google) pour en extraire l'email, exactement
 * comme decodeJwt() dans LoginModal.tsx côté web. Utile en secours si
 * GoogleIdTokenCredential.id ne suffit pas, ou pour extraire d'autres claims.
 */
fun decodeJwtEmail(idToken: String): String? = runCatching {
    val payload = idToken.split(".").getOrNull(1) ?: return null
    val decodedBytes = android.util.Base64.decode(
        payload,
        android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP,
    )
    val json = String(decodedBytes, Charsets.UTF_8)
    Regex("\"email\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
}.getOrNull()
