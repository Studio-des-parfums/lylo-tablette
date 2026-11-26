package com.lylo.tablet.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lylo.tablet.i18n.AppLanguage
import com.lylo.tablet.ui.theme.LyloPrimary

/** Portage de src/components/ui/LanguageSelector.tsx : liste déroulante FR / EN / ES / DE / AR / RU. */
@Composable
fun LanguageSelector(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.clickable { expanded = true }.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(current.code.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = LyloPrimary)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = LyloPrimary, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLanguage.entries.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Text(
                            lang.code.uppercase(),
                            fontWeight = if (lang == current) FontWeight.Black else FontWeight.Normal,
                            color = LyloPrimary,
                        )
                    },
                    onClick = { expanded = false; onSelect(lang) },
                )
            }
        }
    }
}
