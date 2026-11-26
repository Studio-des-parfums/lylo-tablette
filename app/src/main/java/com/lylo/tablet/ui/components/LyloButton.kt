package com.lylo.tablet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.ui.theme.LyloTextDark

/**
 * Portage de src/components/ui/Button.tsx (variants "primary" | "outline" | "ghost", sizes
 * "default" | "lg"). Utilisé sur Navbar, HeroSection, tous les CTA de l'app.
 */
enum class LyloButtonVariant { PRIMARY, OUTLINE, GHOST }
enum class LyloButtonSize { DEFAULT, LARGE }

@Composable
fun LyloButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: LyloButtonVariant = LyloButtonVariant.PRIMARY,
    size: LyloButtonSize = LyloButtonSize.DEFAULT,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val height = if (size == LyloButtonSize.LARGE) 56.dp else 40.dp
    val horizontalPadding = if (size == LyloButtonSize.LARGE) 24.dp else 20.dp
    val shape = RoundedCornerShape(8.dp)

    when (variant) {
        LyloButtonVariant.PRIMARY -> Button(
            onClick = onClick,
            modifier = modifier.height(height).defaultMinSize(minWidth = 100.dp),
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = LyloPrimary, contentColor = Color(0xFFFAFAF9)),
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            content = content,
        )

        LyloButtonVariant.OUTLINE -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(height).defaultMinSize(minWidth = 100.dp),
            enabled = enabled,
            shape = shape,
            border = BorderStroke(1.dp, LyloPrimary.copy(alpha = 0.2f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LyloTextDark),
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            content = content,
        )

        LyloButtonVariant.GHOST -> Button(
            onClick = onClick,
            modifier = modifier.height(height).defaultMinSize(minWidth = 100.dp),
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.1f),
                contentColor = Color.White,
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            content = content,
        )
    }
}
