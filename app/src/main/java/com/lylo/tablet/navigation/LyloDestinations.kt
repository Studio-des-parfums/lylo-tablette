package com.lylo.tablet.navigation

/**
 * Portage des routes Next.js App Router (src/app/.../page.tsx) vers des destinations Compose
 * Navigation. Les query params (ex: /formula?data=...&lang=...) deviennent des arguments de
 * route typés.
 */
object LyloDestinations {
    const val HOME = "home"
    const val CONFIGURE = "configure"
    const val QUIZ = "quiz"
    const val QUIZ_RESULTS = "quiz_results"
    const val RECOMMENDATIONS = "recommendations"
    const val FORMULA = "formula?data={data}&lang={lang}"
    const val PREPARATION = "preparation"
    const val INTERACTION = "interaction"
    const val FORMULAS = "formulas"
    const val ROBOT_SETTINGS = "robot_settings"

    fun formulaRoute(data: String, lang: String): String {
        val encodedData = java.net.URLEncoder.encode(data, "UTF-8")
        return "formula?data=$encodedData&lang=$lang"
    }
}
