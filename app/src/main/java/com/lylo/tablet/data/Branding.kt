package com.lylo.tablet.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.lylo.tablet.data.remote.ApiClient

/**
 * Portage de src/hooks/useBranding.ts : logo partenaire secondaire configuré dans le dashboard
 * (GET /branding), affiché à côté du logo de la marque dans la navbar et sur l'accueil.
 * null tant qu'il n'est pas chargé, ou s'il n'y en a pas.
 */
@Composable
fun rememberSecondaryLogoUrl(): State<String?> = produceState<String?>(initialValue = null) {
    value = runCatching { ApiClient.service.getBranding().body()?.logoUrl }.getOrNull()
}
