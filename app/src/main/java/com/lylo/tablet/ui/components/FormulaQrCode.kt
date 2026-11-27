package com.lylo.tablet.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.lylo.tablet.R
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.LyloPrimary

/**
 * Portage de src/components/recommendations/FormulaQrCode.tsx — au lieu d'appeler un service
 * externe (api.qrserver.com) comme côté web, on génère le QR code localement avec ZXing, ce qui
 * fonctionne aussi hors-ligne et évite une dépendance réseau tierce supplémentaire sur tablette.
 */
@Composable
fun FormulaQrCode(
    shareUrl: String,
    buttonLabel: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    /** Icône seule, sans le libellé — pour les emplacements compacts (bouton rond côte à côte). */
    iconOnly: Boolean = false,
) {
    val strings = LocalStringResolver.current
    var open by remember { mutableStateOf(false) }

    if (iconOnly) {
        RoundIconButton(
            icon = Icons.Default.QrCode2,
            contentDescription = buttonLabel,
            onClick = { open = true },
            style = RoundIconStyle.FILLED,
            modifier = modifier,
        )
    } else {
        androidx.compose.material3.Button(
            onClick = { open = true },
            modifier = modifier,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = LyloPrimary, contentColor = Color.White),
            shape = RoundedCornerShape(999.dp),
        ) {
            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = buttonLabel, modifier = Modifier.padding(start = 8.dp))
        }
    }

    if (open) {
        LyloDialog(onDismissRequest = { open = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium, color = LyloPrimary)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LyloPrimary.copy(alpha = 0.65f))
                    }
                    IconButton(onClick = { open = false }) {
                        Icon(Icons.Default.Close, contentDescription = strings.get(R.string.recommendations_qr_close), tint = LyloPrimary.copy(alpha = 0.5f))
                    }
                }

                Box(modifier = Modifier.padding(top = 16.dp)) {
                    QrCodeImage(content = shareUrl, sizeDp = 220)
                }
            }
        }
    }
}

/** Génère et affiche un QR code localement à partir d'une chaîne, via ZXing. */
@Composable
private fun QrCodeImage(content: String, sizeDp: Int) {
    val density = LocalDensity.current
    val sizePx = remember(sizeDp, density) { with(density) { sizeDp.dp.toPx().toInt() } }
    val bitmap = remember(content, sizePx) { generateQrBitmap(content, sizePx) }
    if (bitmap != null) {
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(sizeDp.dp))
    }
}

private fun generateQrBitmap(content: String, sizePx: Int): android.graphics.Bitmap? = runCatching {
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    bmp
}.getOrNull()
