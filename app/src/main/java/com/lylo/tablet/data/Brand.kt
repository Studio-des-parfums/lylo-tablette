package com.lylo.tablet.data

import androidx.annotation.DrawableRes
import com.lylo.tablet.BuildConfig
import com.lylo.tablet.R

/** Marques ayant un thème (logo, couleurs) embarqué dans l'app — portage de ThemeId (lib/brand.ts). */
enum class ThemeId { LYLO, HELP }

data class PartnerLogo(@DrawableRes val res: Int, val alt: String)

/**
 * Portage de src/lib/brand.ts. [id] est le slug de la marque côté back (n'importe quelle marque
 * créée dans le dashboard) ; [theme] est le thème visuel appliqué : celui de la marque s'il existe
 * dans l'app, sinon Lylo. Choisi au build via LYLO_BRAND (équivalent de NEXT_PUBLIC_BRAND).
 */
data class BrandConfig(
    val id: String,
    val theme: ThemeId,
    val name: String,
    @DrawableRes val logo: Int,
    /** Hauteur du logo dans la navbar (dp) — logo horizontal pour H.E.L.P. */
    val navLogoHeight: Int,
    /** Nombre de questions imposé (un seul choix affiché dans la configuration). */
    val fixedQuestionCount: String? = null,
    /** Logo partenaire propre à la marque, affiché dans la barre des partenaires (TechBar). */
    val partnerLogo: PartnerLogo? = null,
)

object Brand {
    private fun themeConfig(theme: ThemeId, id: String): BrandConfig = when (theme) {
        ThemeId.LYLO -> BrandConfig(
            id = id,
            theme = ThemeId.LYLO,
            name = "Lylo",
            logo = R.drawable.logo_sdp,
            navLogoHeight = 40,
        )
        ThemeId.HELP -> BrandConfig(
            id = id,
            theme = ThemeId.HELP,
            name = "H.E.L.P",
            logo = R.drawable.logo_help,
            navLogoHeight = 30, // logo horizontal (4:1) : 120 px de large côté web
            fixedQuestionCount = "4",
            partnerLogo = PartnerLogo(R.drawable.logo_les_francais_avancent, "Les Français avancent"),
        )
    }

    val active: BrandConfig = run {
        val id = BuildConfig.BRAND.trim().lowercase().ifEmpty { "lylo" }
        val theme = ThemeId.entries.firstOrNull { it.name.lowercase() == id } ?: ThemeId.LYLO
        themeConfig(theme, id)
    }
}
