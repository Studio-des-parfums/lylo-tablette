package com.lylo.tablet.data

import com.lylo.tablet.data.model.Choice
import com.lylo.tablet.data.model.Formula
import com.lylo.tablet.data.model.FormulaNote
import com.lylo.tablet.data.model.FormulaSize
import com.lylo.tablet.data.model.FormulaSizes
import com.lylo.tablet.data.model.Question

/**
 * Portage de src/lib/mockData.ts — utilisé uniquement quand DEV_MODE=true (BuildConfig.DEV_MODE),
 * pour naviguer sur tous les écrans avec des données statiques sans backend réel.
 */
object MockData {

    val formulas: List<Formula> = listOf(
        Formula(
            profile = "Sérénité Florale",
            description = "Une fragrance lumineuse et délicate, évoquant un jardin en fleurs au petit matin.",
            score = 94,
            topNotes = listOf("Bergamote", "Citron", "Mandarine"),
            heartNotes = listOf("Rose de Damas", "Jasmin", "Pivoine"),
            baseNotes = listOf("Santal", "Musc blanc", "Vanille"),
            sizes = FormulaSizes(
                ml10 = FormulaSize(
                    targetMl = 10.0,
                    topNotes = listOf(FormulaNote("Bergamote", 1.5), FormulaNote("Citron", 1.0), FormulaNote("Mandarine", 0.5)),
                    heartNotes = listOf(FormulaNote("Rose de Damas", 2.5), FormulaNote("Jasmin", 1.5), FormulaNote("Pivoine", 1.0)),
                    baseNotes = listOf(FormulaNote("Santal", 1.0), FormulaNote("Musc blanc", 0.5), FormulaNote("Vanille", 0.5)),
                ),
                ml30 = FormulaSize(
                    targetMl = 30.0,
                    topNotes = listOf(FormulaNote("Bergamote", 4.5), FormulaNote("Citron", 3.0), FormulaNote("Mandarine", 1.5)),
                    heartNotes = listOf(FormulaNote("Rose de Damas", 7.5), FormulaNote("Jasmin", 4.5), FormulaNote("Pivoine", 3.0)),
                    baseNotes = listOf(FormulaNote("Santal", 3.0), FormulaNote("Musc blanc", 1.5), FormulaNote("Vanille", 1.5)),
                ),
                ml50 = FormulaSize(
                    targetMl = 50.0,
                    topNotes = listOf(FormulaNote("Bergamote", 7.5), FormulaNote("Citron", 5.0), FormulaNote("Mandarine", 2.5)),
                    heartNotes = listOf(FormulaNote("Rose de Damas", 12.5), FormulaNote("Jasmin", 7.5), FormulaNote("Pivoine", 5.0)),
                    baseNotes = listOf(FormulaNote("Santal", 5.0), FormulaNote("Musc blanc", 2.5), FormulaNote("Vanille", 2.5)),
                ),
            ),
        ),
        Formula(
            profile = "Mystère Oriental",
            description = "Une fragrance envoûtante et chaleureuse, aux accents épicés et boisés qui séduisent les sens.",
            score = 88,
            topNotes = listOf("Poivre noir", "Cardamome", "Gingembre"),
            heartNotes = listOf("Oud", "Patchouli", "Iris"),
            baseNotes = listOf("Cèdre", "Ambre", "Vétiver"),
            sizes = FormulaSizes(
                ml10 = FormulaSize(
                    targetMl = 10.0,
                    topNotes = listOf(FormulaNote("Poivre noir", 1.0), FormulaNote("Cardamome", 0.8), FormulaNote("Gingembre", 0.7)),
                    heartNotes = listOf(FormulaNote("Oud", 2.5), FormulaNote("Patchouli", 2.0), FormulaNote("Iris", 1.0)),
                    baseNotes = listOf(FormulaNote("Cèdre", 1.0), FormulaNote("Ambre", 0.6), FormulaNote("Vétiver", 0.4)),
                ),
                ml30 = FormulaSize(
                    targetMl = 30.0,
                    topNotes = listOf(FormulaNote("Poivre noir", 3.0), FormulaNote("Cardamome", 2.4), FormulaNote("Gingembre", 2.1)),
                    heartNotes = listOf(FormulaNote("Oud", 7.5), FormulaNote("Patchouli", 6.0), FormulaNote("Iris", 3.0)),
                    baseNotes = listOf(FormulaNote("Cèdre", 3.0), FormulaNote("Ambre", 1.8), FormulaNote("Vétiver", 1.2)),
                ),
                ml50 = FormulaSize(
                    targetMl = 50.0,
                    topNotes = listOf(FormulaNote("Poivre noir", 5.0), FormulaNote("Cardamome", 4.0), FormulaNote("Gingembre", 3.5)),
                    heartNotes = listOf(FormulaNote("Oud", 12.5), FormulaNote("Patchouli", 10.0), FormulaNote("Iris", 5.0)),
                    baseNotes = listOf(FormulaNote("Cèdre", 5.0), FormulaNote("Ambre", 3.0), FormulaNote("Vétiver", 2.0)),
                ),
            ),
        ),
    )

    val questions: List<Question> = listOf(
        Question(
            id = 1,
            question = "Quelle ambiance vous attire le plus ?",
            choices = listOf(Choice("Forêt"), Choice("Océan"), Choice("Désert"), Choice("Montagne")),
        ),
        Question(
            id = 2,
            question = "Quel moment de la journée préférez-vous ?",
            choices = listOf(Choice("Matin"), Choice("Après-midi"), Choice("Soirée"), Choice("Nuit")),
        ),
    )
}
