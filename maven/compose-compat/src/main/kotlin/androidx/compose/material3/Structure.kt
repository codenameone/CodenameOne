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
package androidx.compose.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.getValue
import com.codename1.compose.internal.Fonts
import com.codename1.compose.internal.SafeInsetsElement
import com.codename1.compose.internal.NodeApplier
import com.codename1.compose.internal.TextInputNode

// --- scaffold ---------------------------------------------------------------

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable (PaddingValues) -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Column(modifier.fillMaxSize().background(containerColor)) {
            topBar()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                // The bars are laid out above and below the content, not over
                // it, so the content needs no padding for them; upstream hands
                // their heights and the window insets here because its bars
                // overlap the content. A Scaffold with no top bar in a host under
                // the status bar therefore starts its content there; the top bar
                // is what pads for it.
                Box(Modifier.fillMaxSize()) { content(PaddingValues(0.dp)) }
                Box(Modifier.align(Alignment.BottomEnd).padding(16.dp)) { floatingActionButton() }
                Box(Modifier.align(Alignment.BottomCenter)) { snackbarHost() }
            }
            bottomBar()
        }
    }
}

// --- app bars ---------------------------------------------------------------

@Immutable
class TopAppBarColors(val containerColor: Color, val titleContentColor: Color, val navigationIconContentColor: Color,
                      val actionIconContentColor: Color)

object TopAppBarDefaults {
    @Composable
    fun topAppBarColors(containerColor: Color = MaterialTheme.colorScheme.surface,
                        titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor: Color = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant): TopAppBarColors =
        TopAppBarColors(containerColor, titleContentColor, navigationIconContentColor, actionIconContentColor)

    @Composable
    fun centerAlignedTopAppBarColors(containerColor: Color = MaterialTheme.colorScheme.surface,
                                     titleContentColor: Color = MaterialTheme.colorScheme.onSurface): TopAppBarColors =
        topAppBarColors(containerColor, titleContentColor)
}

@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors()
) = AppBar(title, modifier, navigationIcon, actions, colors, false)

@Composable
fun CenterAlignedTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors()
) = AppBar(title, modifier, navigationIcon, actions, colors, true)

@Composable
private fun AppBar(title: @Composable () -> Unit, modifier: Modifier, navigationIcon: @Composable () -> Unit,
                   actions: @Composable RowScope.() -> Unit, colors: TopAppBarColors, centered: Boolean) {
    Row(modifier.fillMaxWidth().background(colors.containerColor).then(SafeInsetsElement(top = true, bottom = false)).height(64.dp)
        .padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        CompositionLocalProvider(LocalContentColor provides colors.navigationIconContentColor) { navigationIcon() }
        Box(Modifier.weight(1f).padding(horizontal = 12.dp),
            contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart) {
            CompositionLocalProvider(LocalContentColor provides colors.titleContentColor) {
                ProvideTextStyle(MaterialTheme.typography.titleLarge) { title() }
            }
        }
        CompositionLocalProvider(LocalContentColor provides colors.actionIconContentColor) {
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

// --- navigation bar ---------------------------------------------------------

@Composable
fun NavigationBar(modifier: Modifier = Modifier, containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
                  contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant, content: @Composable RowScope.() -> Unit) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(modifier.fillMaxWidth().background(containerColor).then(SafeInsetsElement(top = false, bottom = true)).height(80.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
fun RowScope.NavigationBarItem(selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit,
                               modifier: Modifier = Modifier, enabled: Boolean = true,
                               label: (@Composable () -> Unit)? = null, alwaysShowLabel: Boolean = true) {
    val cs = MaterialTheme.colorScheme
    val indicator by animateColorAsState(if (selected) cs.secondaryContainer else cs.secondaryContainer.copy(alpha = 0f))
    Column(modifier.weight(1f).fillMaxSize().clickable(enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(64.dp, 32.dp).background(indicator, CircleShape), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides if (selected) cs.onSecondaryContainer else cs.onSurfaceVariant) {
                icon()
            }
        }
        if (label != null && (alwaysShowLabel || selected)) {
            Spacer(Modifier.height(4.dp))
            CompositionLocalProvider(LocalContentColor provides if (selected) cs.onSurface else cs.onSurfaceVariant) {
                ProvideTextStyle(MaterialTheme.typography.labelMedium) { label() }
            }
        }
    }
}

// --- selection controls -----------------------------------------------------

@Composable
fun Switch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val cs = MaterialTheme.colorScheme
    val track by animateColorAsState(if (checked) cs.primary else cs.surfaceContainerHighest)
    val thumb by animateColorAsState(if (checked) cs.onPrimary else cs.outline)
    val offset by animateDpAsState(if (checked) 24.dp else 4.dp)
    val thumbSize by animateDpAsState(if (checked) 24.dp else 16.dp)
    var m = modifier.size(52.dp, 32.dp).clip(CircleShape).background(track, CircleShape)
    if (!checked) m = m.border(2.dp, cs.outline, CircleShape)
    if (onCheckedChange != null) m = m.clickable(enabled) { onCheckedChange(!checked) }
    Box(m, contentAlignment = Alignment.CenterStart) {
        Box(Modifier.offset(x = offset + (24.dp - thumbSize) / 2).size(thumbSize).background(thumb, CircleShape))
    }
}

@Composable
fun Checkbox(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val cs = MaterialTheme.colorScheme
    var m = modifier.size(40.dp)
    if (onCheckedChange != null) m = m.clip(CircleShape).clickable(enabled) { onCheckedChange(!checked) }
    Box(m, contentAlignment = Alignment.Center) {
        val box = Modifier.size(18.dp).clip(RoundedCornerShape(2.dp))
        if (checked) {
            Box(box.background(cs.primary), contentAlignment = Alignment.Center) {
                Icon(androidx.compose.ui.graphics.vector.ImageVector.cn1Material("Check", '\ue5ca'), null,
                    Modifier.size(16.dp), cs.onPrimary)
            }
        } else {
            Box(box.border(2.dp, cs.onSurfaceVariant, RoundedCornerShape(2.dp)))
        }
    }
}

// --- text fields ------------------------------------------------------------

@Composable
private fun TextInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier, enabled: Boolean,
                      singleLine: Boolean, hint: String, style: TextStyle, color: Color) {
    val font = Fonts.get(Math.max(1, Math.round((if (style.fontSize.isSpecified) style.fontSize.value else 16f) *
        com.codename1.compose.internal.Density.density)), style.fontWeight)
    ComposeNode<TextInputNode, NodeApplier>(
        factory = { TextInputNode() },
        update = {
            set(modifier) { this.modifier = it }
            set(value) { this.setValue(it) }
            set(onValueChange) { this.onValueChange = it }
            set(enabled) { this.enabledInput = it }
            set(singleLine) { this.singleLine = it }
            set(hint) { this.hint = it }
            set(color) { this.textColor = it }
            set(font) { this.font = it }
        }
    )
}

@Composable
fun BasicTextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                   textStyle: TextStyle = TextStyle.Default, singleLine: Boolean = false) {
    TextInput(value, onValueChange, modifier, enabled, singleLine, "", textStyle,
        if (textStyle.color != Color.Unspecified) textStyle.color else LocalContentColor.current)
}

@Composable
private fun DecoratedField(value: String, onValueChange: (String) -> Unit, modifier: Modifier, enabled: Boolean,
                           label: (@Composable () -> Unit)?, placeholder: (@Composable () -> Unit)?, leadingIcon: (@Composable () -> Unit)?,
                           trailingIcon: (@Composable () -> Unit)?, singleLine: Boolean, shape: Shape,
                           container: Color, outline: Color?) {
    val cs = MaterialTheme.colorScheme
    var m = modifier.clip(shape).background(container, shape)
    if (outline != null) m = m.border(1.dp, outline, shape)
    Row(m.height(if (singleLine) 56.dp else 112.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (leadingIcon != null) {
            CompositionLocalProvider(LocalContentColor provides cs.onSurfaceVariant) { leadingIcon() }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            if (label != null && value.isNotEmpty()) {
                CompositionLocalProvider(LocalContentColor provides cs.primary) {
                    ProvideTextStyle(MaterialTheme.typography.bodySmall) { label() }
                }
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                TextInput(value, onValueChange, Modifier.fillMaxWidth(), enabled, singleLine, "",
                    MaterialTheme.typography.bodyLarge, cs.onSurface)
                // The placeholder, or the label while there is no value, paints over the
                // empty field; text takes no touches, so the field still gets them.
                val hint = placeholder ?: label
                if (value.isEmpty() && hint != null) {
                    CompositionLocalProvider(LocalContentColor provides cs.onSurfaceVariant) {
                        ProvideTextStyle(MaterialTheme.typography.bodyLarge) { hint() }
                    }
                }
            }
        }
        if (trailingIcon != null) {
            Spacer(Modifier.width(12.dp))
            CompositionLocalProvider(LocalContentColor provides cs.onSurfaceVariant) { trailingIcon() }
        }
    }
}

/// A filled text field. The placeholder (or the label) shows while the field is empty.
/// Editing is the platform's own: the field is a Codename One `TextField`.
@Composable
fun TextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
              label: (@Composable () -> Unit)? = null, placeholder: (@Composable () -> Unit)? = null,
              leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null,
              singleLine: Boolean = false, shape: Shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)) {
    DecoratedField(value, onValueChange, modifier, enabled, label, placeholder, leadingIcon, trailingIcon, singleLine,
        shape, MaterialTheme.colorScheme.surfaceContainerHighest, null)
}

@Composable
fun OutlinedTextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                      label: (@Composable () -> Unit)? = null, placeholder: (@Composable () -> Unit)? = null,
                      leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null,
                      singleLine: Boolean = false, shape: Shape = RoundedCornerShape(4.dp)) {
    DecoratedField(value, onValueChange, modifier, enabled, label, placeholder, leadingIcon, trailingIcon, singleLine,
        shape, Color.Transparent, MaterialTheme.colorScheme.outline)
}
