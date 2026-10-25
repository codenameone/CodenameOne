/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package keynote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/// What the application shows, hoisted so the benchmark can drive it.
class AppState {
    var tab by mutableIntStateOf(0)
    var day by mutableIntStateOf(1)
    var query by mutableStateOf("")
    var selected by mutableStateOf<Session?>(null)
    val favorites = mutableStateListOf<Int>()

    fun toggleFavorite(id: Int) {
        if (!favorites.remove(id)) favorites.add(id)
    }
}

private val Brand = Color(0xFF7F52FF)

@Composable
fun App(state: AppState = remember { AppState() }) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(primary = Color(0xFFB9A4FF), secondaryContainer = Color(0xFF3A2D6B))
    else lightColorScheme(primary = Brand, secondaryContainer = Color(0xFFE9E1FF))
    MaterialTheme(colorScheme = scheme) {
        val sessions by remember { Programme.load() }.collectAsState(emptyList())
        val selected = state.selected
        if (selected != null) {
            SessionDetail(selected, state.favorites.contains(selected.id), { state.toggleFavorite(selected.id) }) {
                state.selected = null
            }
            return@MaterialTheme
        }
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(title = { Text(if (state.tab == 2) "Insights" else "KotlinConf-ish 2026",
                    fontWeight = FontWeight.SemiBold) })
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(state.tab == 0, { state.tab = 0 }, { Icon(Icons.Default.DateRange, null) },
                        label = { Text("Schedule") }, modifier = Modifier.testTag("tab-schedule"))
                    NavigationBarItem(state.tab == 1, { state.tab = 1 }, { Icon(Icons.Default.Favorite, null) },
                        label = { Text("Favorites") }, modifier = Modifier.testTag("tab-favorites"))
                    NavigationBarItem(state.tab == 2, { state.tab = 2 }, { Icon(Icons.Default.Info, null) },
                        label = { Text("Insights") }, modifier = Modifier.testTag("tab-insights"))
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                if (sessions.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else when (state.tab) {
                    0 -> Schedule(sessions, state)
                    1 -> SessionList(sessions.filter { state.favorites.contains(it.id) }, state,
                        empty = "Tap the heart on a session to keep it here.")
                    else -> Insights(sessions, state)
                }
            }
        }
    }
}

@Composable
private fun Schedule(sessions: List<Session>, state: AppState) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(state.query, { state.query = it }, Modifier.fillMaxWidth().padding(16.dp, 8.dp).testTag("search"),
            placeholder = { Text("Search talks and speakers") }, leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true, shape = RoundedCornerShape(28.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(1, 2, 3)) { d ->
                FilterChip(state.day == d, { state.day = d }, { Text("Day $d") }, Modifier.testTag("day-$d"))
            }
        }
        val q = state.query.trim()
        val visible = sessions.filter {
            it.day == state.day && (q.isEmpty() || it.title.contains(q, ignoreCase = true) ||
                it.speaker.name.contains(q, ignoreCase = true))
        }
        SessionList(visible, state, empty = "No session matches \"$q\".")
    }
}

@Composable
private fun SessionList(sessions: List<Session>, state: AppState, empty: String) {
    if (sessions.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(empty, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().testTag("list"), contentPadding = PaddingValues(16.dp, 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(sessions, key = { it.id }) { s ->
            SessionCard(s, state.favorites.contains(s.id), { state.toggleFavorite(s.id) }) { state.selected = s }
        }
    }
}

@Composable
fun SessionCard(s: Session, favorite: Boolean, onFavorite: () -> Unit, onOpen: () -> Unit) {
    Card(onOpen, Modifier.fillMaxWidth().testTag("session-${s.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(start = 0.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(6.dp).height(84.dp).background(Color(s.track.color)))
            Column(Modifier.weight(1f).padding(12.dp)) {
                Text(s.timeLabel + "  -  " + s.room, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(s.speaker.name + ", " + s.speaker.company, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FavoriteButton(favorite, onFavorite, Modifier.testTag("fav-${s.id}"))
        }
    }
}

@Composable
private fun FavoriteButton(favorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(if (favorite) 1f else 0.8f)
    IconButton(onClick, modifier) {
        Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            if (favorite) "Remove from favorites" else "Add to favorites",
            Modifier.size((24 * scale).dp),
            tint = if (favorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SessionDetail(s: Session, favorite: Boolean, onFavorite: () -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text(s.track.label) },
            navigationIcon = { IconButton(onBack, Modifier.testTag("back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = { FavoriteButton(favorite, onFavorite) })
    }) { padding ->
        Column(Modifier.padding(padding).padding(20.dp).fillMaxWidth()) {
            Text(s.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(Color(s.track.color), CircleShape), contentAlignment = Alignment.Center) {
                    Text(s.speaker.name.split(" ").joinToString("") { it.take(1) }, color = Color.White,
                        fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(s.speaker.name, style = MaterialTheme.typography.titleMedium)
                    Text(s.speaker.company, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            DetailLine(Icons.Default.Schedule, "Day ${s.day}, ${s.timeLabel}")
            DetailLine(Icons.Default.Place, s.room)
            Spacer(Modifier.height(16.dp))
            Text(s.description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun DetailLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/// Sessions per track, as bars that grow in when the tab opens.
@Composable
private fun Insights(sessions: List<Session>, state: AppState) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val grow by animateFloatAsState(if (shown) 1f else 0f, tween(700))
    val counts = Track.entries.map { t -> t to sessions.count { it.track == t && it.day == state.day } }
    val max = (counts.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Sessions per track, day ${state.day}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        val bar = MaterialTheme.colorScheme.surfaceContainerHighest
        Canvas(Modifier.fillMaxWidth().height(220.dp).testTag("chart")) {
            val gap = 16.dp.toPx()
            val w = (size.width - gap * (counts.size - 1)) / counts.size
            counts.forEachIndexed { i, (track, n) ->
                val x = i * (w + gap)
                drawRoundRect(bar, Offset(x, 0f), Size(w, size.height), CornerRadius(8.dp.toPx()))
                val h = size.height * n / max * grow
                drawRoundRect(Color(track.color), Offset(x, size.height - h), Size(w, h), CornerRadius(8.dp.toPx()))
            }
        }
        Spacer(Modifier.height(12.dp))
        counts.forEach { (track, n) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(Color(track.color), CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(track.label, Modifier.weight(1f))
                Text("$n", fontWeight = FontWeight.SemiBold)
            }
        }
        AnimatedVisibility(state.favorites.isNotEmpty()) {
            Text("You have ${state.favorites.size} favorite sessions.", Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.primary)
        }
    }
}
