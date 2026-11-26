package com.lylo.tablet.util

/** Affiche un volume comme le ferait JavaScript (`${note.ml}`) : 3.0 → "3", 4.5 → "4.5". */
fun formatMl(ml: Double): String =
    if (ml % 1.0 == 0.0) ml.toLong().toString() else ml.toBigDecimal().stripTrailingZeros().toPlainString()
