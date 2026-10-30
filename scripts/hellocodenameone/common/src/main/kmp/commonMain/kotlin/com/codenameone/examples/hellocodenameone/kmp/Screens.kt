/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codenameone.examples.hellocodenameone.kmp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/// Box, Row and Column under padding, size, background, border and weight.
@Composable
fun LayoutScreen() {
    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(48.dp).background(Color(0xFFE53935)))
                Box(Modifier.weight(2f).height(48.dp).background(Color(0xFF43A047)))
                Box(Modifier.size(48.dp).background(Color(0xFF1E88E5), RoundedCornerShape(12.dp)))
            }
            Box(Modifier.fillMaxWidth().height(96.dp).border(2.dp, Color(0xFF6750A4), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center) {
                Text("Centered in a bordered box")
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).background(Color(0xFFFFB300)))
                Spacer(Modifier.size(12.dp))
                Column {
                    Text("Row with a spacer", fontWeight = FontWeight.Bold)
                    Text("and a nested column")
                }
            }
        }
    }
}

/// The Material 3 type scale and buttons.
@Composable
fun TypographyScreen() {
    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Headline large", style = MaterialTheme.typography.headlineLarge)
            Text("Title medium", style = MaterialTheme.typography.titleMedium)
            Text("Body medium text that is long enough to wrap onto a second line in a phone width column.",
                style = MaterialTheme.typography.bodyMedium)
            Text("Label small", style = MaterialTheme.typography.labelSmall)
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) { Text("Filled") }
                OutlinedButton(onClick = {}) { Text("Outlined") }
                TextButton(onClick = {}) { Text("Text") }
            }
        }
    }
}

/// A Scaffold with a top app bar over a LazyColumn of cards.
@Composable
fun ListScreen() {
    MaterialTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("Compose list") }) }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(30) { index ->
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Item $index", style = MaterialTheme.typography.titleMedium)
                            Text("Supporting text for row $index")
                        }
                    }
                }
            }
        }
    }
}

/// State and recomposition: a counter bumped from an effect, and an
/// AnimatedVisibility captured at its end state.
@Composable
fun StateScreen() {
    MaterialTheme {
        var count by remember { mutableIntStateOf(0) }
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            count = 3
            shown = true
        }
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Clicked $count times", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { count++ }) { Text("Click me") }
            AnimatedVisibility(visible = shown) {
                Card(Modifier.fillMaxWidth()) {
                    Text("Revealed by AnimatedVisibility", Modifier.padding(16.dp))
                }
            }
            AnimatedVisibility(visible = !shown) {
                Text("This text is hidden at the end state")
            }
        }
    }
}
