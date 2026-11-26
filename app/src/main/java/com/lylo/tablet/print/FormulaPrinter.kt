package com.lylo.tablet.print

import android.content.Context
import com.lylo.tablet.data.model.FormulaSize
import com.lylo.tablet.data.remote.ApiClient
import com.lylo.tablet.data.remote.PrintFormulaPayload
import com.lylo.tablet.data.remote.PrintNoteEntry
import com.lylo.tablet.data.remote.PrintNotes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Équivalent de PrintableFormula + window.print() côté web : le PDF (logo, profil, date ·
 * référence, notes par famille — même mise en page, générée par pdf_service côté back) est
 * imprimé via la boîte de dialogue native Android, l'utilisateur choisit son imprimante à ce
 * moment-là. Retourne false si le PDF n'a pas pu être récupéré.
 */
suspend fun printFormula(context: Context, profile: String, size: FormulaSize, reference: String? = null): Boolean = try {
    val date = SimpleDateFormat("d MMMM yyyy", Locale.FRANCE).format(Date())
    val res = ApiClient.service.getFormulaPdf(
        PrintFormulaPayload(
            profile = profile,
            notes = PrintNotes(
                top = size.topNotes.map { PrintNoteEntry(it.name, it.ml) },
                heart = size.heartNotes.map { PrintNoteEntry(it.name, it.ml) },
                base = size.baseNotes.map { PrintNoteEntry(it.name, it.ml) },
            ),
            date = date,
            reference = reference?.takeIf { it.isNotEmpty() },
        ),
    )
    val bytes = if (res.isSuccessful) res.body()?.bytes() else null
    if (bytes != null) {
        printPdf(context, profile, bytes)
        true
    } else {
        false
    }
} catch (_: Exception) {
    false
}
