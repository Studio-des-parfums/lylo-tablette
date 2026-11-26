package com.lylo.tablet.data

import android.util.Base64
import com.lylo.tablet.data.model.FormulaNote
import com.lylo.tablet.data.model.FormulaSizes
import com.lylo.tablet.data.model.SizeOption
import com.lylo.tablet.i18n.AppLanguage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Portage de src/lib/shareableFormula.ts — encode une formule + taille choisie en base64url
 * compact pour tenir dans une URL de QR code (deep link lylo://formula?data=...&lang=...). */

data class ShareableFormula(
    val profile: String,
    val size: SizeOption,
    val top: List<FormulaNote>,
    val heart: List<FormulaNote>,
    val base: List<FormulaNote>,
    val boosters: List<FormulaNote>,
)

@Serializable
private data class EncodedNote(val n: String, val m: Double)

@Serializable
private data class EncodedFormulaPayload(
    val p: String,
    val s: String,
    val t: List<EncodedNote>,
    val h: List<EncodedNote>,
    val b: List<EncodedNote>,
    val o: List<EncodedNote>,
)

private val json = Json { ignoreUnknownKeys = true }

object ShareableFormulaCodec {

    fun create(profile: String, selectedSize: SizeOption, sizes: FormulaSizes): ShareableFormula {
        val size = sizes.forOption(selectedSize)
        return ShareableFormula(
            profile = profile,
            size = selectedSize,
            top = size.topNotes,
            heart = size.heartNotes,
            base = size.baseNotes,
            boosters = size.boosters,
        )
    }

    fun encode(formula: ShareableFormula): String {
        val payload = EncodedFormulaPayload(
            p = formula.profile,
            s = formula.size.wire,
            t = formula.top.map { EncodedNote(it.name, it.ml) },
            h = formula.heart.map { EncodedNote(it.name, it.ml) },
            b = formula.base.map { EncodedNote(it.name, it.ml) },
            o = formula.boosters.map { EncodedNote(it.name, it.ml) },
        )
        val jsonStr = json.encodeToString(EncodedFormulaPayload.serializer(), payload)
        return Base64.encodeToString(
            jsonStr.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        )
    }

    fun decode(payload: String): ShareableFormula? = runCatching {
        val bytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val decoded = json.decodeFromString(EncodedFormulaPayload.serializer(), String(bytes, Charsets.UTF_8))
        ShareableFormula(
            profile = decoded.p,
            size = SizeOption.fromWire(decoded.s),
            top = decoded.t.map { FormulaNote(it.n, it.m) },
            heart = decoded.h.map { FormulaNote(it.n, it.m) },
            base = decoded.b.map { FormulaNote(it.n, it.m) },
            boosters = decoded.o.map { FormulaNote(it.n, it.m) },
        )
    }.getOrNull()

    /**
     * Portage de buildFormulaShareUrl — utilise un deep link custom scheme lylo://formula
     * (déclaré dans AndroidManifest.xml) plutôt qu'une URL web, car l'app tablette n'a pas de
     * domaine web public à elle : le QR scanné pointe vers /formula sur le FRONT WEB existant
     * (NEXT_PUBLIC_APP_URL) pour rester consultable depuis le téléphone de l'utilisateur, comme
     * conçu à l'origine. Voir README.md pour la configuration de cette URL.
     */
    fun buildShareUrl(formula: ShareableFormula, language: AppLanguage, webAppBaseUrl: String): String {
        val base = webAppBaseUrl.trimEnd('/')
        val data = encode(formula)
        return "$base/formula?data=$data&lang=${language.code}"
    }
}
