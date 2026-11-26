package com.lylo.tablet.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ── Radius Tailwind (globals.css: --radius / --radius-lg / --radius-xl) ────
val RadiusSm = 4.dp   // 0.25rem
val RadiusMd = 8.dp   // 0.5rem
val RadiusLg = 12.dp  // 0.75rem
val RadiusFull = 999.dp

val LyloShapes = Shapes(
    extraSmall = RoundedCornerShape(RadiusSm),
    small = RoundedCornerShape(RadiusMd),
    medium = RoundedCornerShape(RadiusLg),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
