package com.lylo.tablet.ui.screens.formulas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lylo.tablet.R
import com.lylo.tablet.data.model.AuthUser
import com.lylo.tablet.data.remote.ApiClient
import com.lylo.tablet.data.remote.FormulaRow
import com.lylo.tablet.data.remote.FormulasResponse
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.components.FormulaCard
import com.lylo.tablet.ui.components.Navbar
import com.lylo.tablet.ui.theme.LyloBackgroundLight
import com.lylo.tablet.ui.theme.LyloPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.OffsetDateTime
import java.time.LocalDateTime
import java.util.Date

private const val PAGE_SIZE = 100

/**
 * Portage de src/app/formulas/page.tsx : recherche et consultation des formules générées
 * (réservée au compte de l'équipe, bouton dans la navbar). Recherche par référence avec
 * debounce 300 ms, pagination par 100, rafraîchissement, et détail d'une formule en plein écran.
 * Le bouton « plein écran » du web n'a pas d'équivalent : l'app tourne déjà en plein écran.
 */
@Composable
fun FormulasScreen(
    user: AuthUser?,
    onNavigateHome: () -> Unit,
) {
    val strings = LocalStringResolver.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(user) { if (user == null) onNavigateHome() }
    if (user == null) return

    var search by remember { mutableStateOf("") }
    var debouncedSearch by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }
    var data by remember { mutableStateOf<FormulasResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<FormulaRow?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(search) {
        delay(300)
        if (debouncedSearch != search.trim()) {
            debouncedSearch = search.trim()
            page = 1
        }
    }

    LaunchedEffect(page, debouncedSearch, refreshTick) {
        loading = true
        error = null
        try {
            val res = ApiClient.service.listFormulas(page, PAGE_SIZE, debouncedSearch)
            if (!res.isSuccessful) throw Exception(strings.get(R.string.formulas_load_error))
            data = res.body()
        } catch (e: Exception) {
            error = e.message ?: strings.get(R.string.formulas_load_error)
        } finally {
            loading = false
        }
    }

    val detail = selected
    if (detail?.sizes != null) {
        Box(modifier = Modifier.fillMaxSize().background(LyloBackgroundLight), contentAlignment = Alignment.TopCenter) {
            Column(modifier = Modifier.widthIn(max = 500.dp).fillMaxSize().padding(horizontal = 16.dp, vertical = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                    IconButton(onClick = { selected = null }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.get(R.string.formulas_close), tint = LyloPrimary)
                    }
                    Text(detail.reference, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = LyloPrimary)
                }
                FormulaCard(
                    name = detail.reference,
                    sizes = detail.sizes,
                    large = true,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
        return
    }

    val totalPages = data?.let { maxOf(1, (it.total + PAGE_SIZE - 1) / PAGE_SIZE) } ?: 1

    Column(modifier = Modifier.fillMaxSize().background(LyloBackgroundLight)) {
        Navbar(showActions = false, onLogoClick = onNavigateHome)

        Column(
            modifier = Modifier.weight(1f).widthIn(max = 1200.dp).fillMaxWidth().align(Alignment.CenterHorizontally).padding(horizontal = 40.dp, vertical = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Text(strings.get(R.string.formulas_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = LyloPrimary, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, LyloPrimary.copy(alpha = 0.15f), CircleShape)
                        .alpha(if (loading) 0.4f else 1f)
                        .clickable(enabled = !loading) { refreshTick++ },
                    contentAlignment = Alignment.Center,
                ) {
                    if (loading) CircularProgressIndicator(color = LyloPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    else Icon(Icons.Default.Refresh, contentDescription = strings.get(R.string.formulas_refresh), tint = LyloPrimary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }
            Text(strings.get(R.string.formulas_subtitle), style = MaterialTheme.typography.bodyMedium, color = LyloPrimary.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

            BasicTextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = LyloPrimary),
                modifier = Modifier
                    .widthIn(max = 448.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, LyloPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                decorationBox = { inner ->
                    if (search.isEmpty()) Text(strings.get(R.string.formulas_search_placeholder), style = MaterialTheme.typography.bodyMedium, color = LyloPrimary.copy(alpha = 0.35f))
                    inner()
                },
            )

            error?.let { Text(it, color = Color(0xFFDC2626), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp)) }

            Column(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, LyloPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    HeaderCell(strings.get(R.string.formulas_col_reference), 1.2f)
                    HeaderCell(strings.get(R.string.formulas_col_perfume_name), 1.1f)
                    HeaderCell(strings.get(R.string.formulas_col_first_name), 0.9f)
                    HeaderCell(strings.get(R.string.formulas_col_email), 1.4f)
                    HeaderCell(strings.get(R.string.formulas_col_type), 0.7f)
                    HeaderCell(strings.get(R.string.formulas_col_date), 1f)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(LyloPrimary.copy(alpha = 0.1f)))
                val rows = data?.results.orEmpty()
                when {
                    loading -> Text(strings.get(R.string.formulas_loading), color = LyloPrimary.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp))
                    rows.isEmpty() -> Text(strings.get(R.string.formulas_no_results), color = LyloPrimary.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp))
                    else -> LazyColumn {
                        items(rows, key = { it.id }) { f ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (f.sizes != null) Modifier.clickable { selected = f } else Modifier)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BodyCell(f.reference, 1.2f, mono = true)
                                BodyCell(f.perfumeName ?: "—", 1.1f)
                                BodyCell(f.customerFirstName ?: "—", 0.9f)
                                BodyCell(f.customerEmail ?: "—", 1.4f)
                                BodyCell(f.formulaType ?: "—", 0.7f)
                                BodyCell(formatDate(f.createdAt), 1f, dim = true)
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(LyloPrimary.copy(alpha = 0.05f)))
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    data?.let { strings.get(R.string.formulas_total_count).replace("{count}", it.total.toString()) } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = LyloPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                )
                PagerButton(strings.get(R.string.formulas_prev), enabled = page > 1 && !loading) { page = maxOf(1, page - 1) }
                Text(
                    strings.get(R.string.formulas_page_of).replace("{page}", page.toString()).replace("{totalPages}", totalPages.toString()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LyloPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                PagerButton(strings.get(R.string.formulas_next), enabled = page < totalPages && !loading) { page = minOf(totalPages, page + 1) }
            }
        }
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = LyloPrimary.copy(alpha = 0.7f), modifier = Modifier.weight(weight))
}

@Composable
private fun RowScope.BodyCell(text: String, weight: Float, mono: Boolean = false, dim: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = if (mono) FontFamily.Monospace else null,
        fontWeight = if (mono) FontWeight.Medium else null,
        color = if (dim) LyloPrimary.copy(alpha = 0.8f) else LyloPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(weight).padding(end = 8.dp),
    )
}

@Composable
private fun PagerButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyMedium,
        color = LyloPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, LyloPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .alpha(if (enabled) 1f else 0.3f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** Équivalent de new Date(created_at).toLocaleString() (format local de la tablette). */
private fun formatDate(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    val instant = runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(iso).atZone(java.time.ZoneId.systemDefault()).toInstant() }.getOrNull()
        ?: return iso
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date.from(instant))
}
