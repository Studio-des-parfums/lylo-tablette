package com.lylo.tablet.print

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import java.io.FileOutputStream
import java.io.IOException

/**
 * Ouvre la boîte de dialogue d'impression native Android pour un PDF déjà généré (côté backend,
 * via generate_formula_pdf) — remplace le flux réseau PrintNode/CUPS configuré à l'avance :
 * l'utilisateur choisit son imprimante au moment de l'impression, comme window.print() côté web.
 *
 * Le PDF est déjà rendu (pages, mise en page) : l'adapter n'a qu'à copier les octets tels quels
 * dans le FileDescriptor fourni par le système, pas besoin de dessiner quoi que ce soit.
 */
private class PdfPrintDocumentAdapter(
    private val jobName: String,
    private val pdfBytes: ByteArray,
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        try {
            FileOutputStream(destination.fileDescriptor).use { it.write(pdfBytes) }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback.onWriteFailed(e.message)
        }
    }
}

/** Lance l'impression système du PDF donné — voir PdfPrintDocumentAdapter. */
fun printPdf(context: Context, jobName: String, pdfBytes: ByteArray) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    printManager.print(jobName, PdfPrintDocumentAdapter(jobName, pdfBytes), null)
}
