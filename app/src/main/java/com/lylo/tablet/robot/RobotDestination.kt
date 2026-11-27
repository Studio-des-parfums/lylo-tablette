package com.lylo.tablet.robot

import com.lylo.tablet.R
import com.lylo.tablet.data.LocalPrefs

/**
 * Lieux nommés vers lesquels le robot peut accompagner le visiteur. Chaque lieu est associé,
 * dans Configuration du robot, à un point de la carte (ex. « Salle de création » → « salle 1 »).
 * L'agent vocal annonce le NOM du lieu, jamais le nom technique du point de la carte.
 * Pour ajouter une salle : une entrée ici (clé de préférence, libellés) + ses traductions.
 */
enum class RobotDestination(
    /** Clé LocalPrefs du point de la carte associé. */
    val prefKey: String,
    /** Libellé affiché dans la configuration. */
    val labelRes: Int,
    /** Nom prononcé par l'agent vocal, par langue (au nominatif ; l'agent l'accorde). */
    val spokenLabels: Map<String, String>,
) {
    // Reprend l'ancienne clé robot_goal : le point déjà configuré est conservé.
    CREATION_ROOM(
        prefKey = LocalPrefs.KEY_ROBOT_GOAL,
        labelRes = R.string.robot_destination_creation_room,
        spokenLabels = mapOf(
            "fr" to "la salle de création",
            "en" to "the creation room",
            "es" to "la sala de creación",
            "de" to "der Kreationsraum",
            "ar" to "غرفة الإبداع",
            "ru" to "зал создания ароматов",
        ),
    );

    fun spokenLabel(language: String): String = spokenLabels[language] ?: spokenLabels.getValue("fr")

    fun mapPoint(prefs: LocalPrefs): String? = prefs.getString(prefKey)?.takeIf { it.isNotBlank() }

    companion object {
        /** Lieu proposé en fin de parcours (quiz « Emmène-moi » et accompagnement vocal). */
        val END_OF_JOURNEY = CREATION_ROOM
    }
}
