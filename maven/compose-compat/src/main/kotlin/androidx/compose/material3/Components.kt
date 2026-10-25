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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloat
import com.codename1.compose.internal.IconNode
import com.codename1.compose.internal.NodeApplier

// --- text and icons ---------------------------------------------------------

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current
) {
    val c = if (color != Color.Unspecified) color else if (style.color != Color.Unspecified) style.color else LocalContentColor.current
    val merged = style.merge(TextStyle(color = c, fontSize = fontSize, fontWeight = fontWeight, textAlign = textAlign,
        lineHeight = lineHeight))
    BasicText(text, modifier, merged, overflow, softWrap, maxLines)
}

@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, modifier: Modifier = Modifier,
         tint: Color = LocalContentColor.current) {
    ComposeNode<IconNode, NodeApplier>(
        factory = { IconNode() },
        update = {
            set(modifier) { this.modifier = it }
            set(imageVector) { this.glyph = com.codename1.compose.internal.Glyphs.of(it) }
            set(tint) { this.tint = it }
        }
    )
}

// --- surfaces ---------------------------------------------------------------

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val m = modifier.surface(shape, tint(color, tonalElevation), border)
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(m, propagateMinConstraints = true) { content() }
    }
}

@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val m = modifier.surface(shape, tint(color, tonalElevation), border).clickable(enabled, onClick = onClick)
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(m, propagateMinConstraints = true) { content() }
    }
}

private fun Modifier.surface(shape: Shape, color: Color, border: BorderStroke?): Modifier {
    var m = this.clip(shape).background(color, shape)
    if (border != null) m = m.border(border.width, border.color, shape)
    return m
}

@Composable
private fun tint(color: Color, elevation: Dp): Color {
    val cs = MaterialTheme.colorScheme
    if (color != cs.surface || elevation.value <= 0f) return color
    val f = Math.min(0.14f, elevation.value * 0.02f)
    return androidx.compose.ui.graphics.lerp(color, cs.surfaceTint, f)
}

@Immutable
class CardColors(val containerColor: Color, val contentColor: Color)

object CardDefaults {
    val shape: Shape @Composable get() = MaterialTheme.shapes.medium

    @Composable
    fun cardColors(containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
                   contentColor: Color = contentColorFor(containerColor)): CardColors = CardColors(containerColor, contentColor)

    @Composable
    fun elevatedCardColors(containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
                           contentColor: Color = contentColorFor(containerColor)): CardColors = CardColors(containerColor, contentColor)

    @Composable
    fun outlinedCardColors(containerColor: Color = MaterialTheme.colorScheme.surface,
                           contentColor: Color = contentColorFor(containerColor)): CardColors = CardColors(containerColor, contentColor)

    @Composable
    fun cardElevation(defaultElevation: Dp = 0.dp): Dp = defaultElevation

    @Composable
    fun elevatedCardElevation(defaultElevation: Dp = 1.dp): Dp = defaultElevation
}

@Composable
fun Card(modifier: Modifier = Modifier, shape: Shape = CardDefaults.shape, colors: CardColors = CardDefaults.cardColors(),
         elevation: Dp = 0.dp, border: BorderStroke? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape, colors.containerColor, colors.contentColor, elevation, border = border) {
        Column(content = content)
    }
}

@Composable
fun Card(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = CardDefaults.shape,
         colors: CardColors = CardDefaults.cardColors(), elevation: Dp = 0.dp, border: BorderStroke? = null,
         content: @Composable ColumnScope.() -> Unit) {
    Surface(onClick, modifier, enabled, shape, colors.containerColor, colors.contentColor, elevation, border = border) {
        Column(content = content)
    }
}

@Composable
fun ElevatedCard(modifier: Modifier = Modifier, shape: Shape = CardDefaults.shape,
                 colors: CardColors = CardDefaults.elevatedCardColors(), content: @Composable ColumnScope.() -> Unit) =
    Card(modifier, shape, colors, 1.dp, null, content)

@Composable
fun OutlinedCard(modifier: Modifier = Modifier, shape: Shape = CardDefaults.shape,
                 colors: CardColors = CardDefaults.outlinedCardColors(), content: @Composable ColumnScope.() -> Unit) =
    Card(modifier, shape, colors, 0.dp, BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), content)

// --- buttons ----------------------------------------------------------------

@Immutable
class ButtonColors(val containerColor: Color, val contentColor: Color, val disabledContainerColor: Color,
                   val disabledContentColor: Color) {
    internal fun container(enabled: Boolean) = if (enabled) containerColor else disabledContainerColor
    internal fun content(enabled: Boolean) = if (enabled) contentColor else disabledContentColor
}

object ButtonDefaults {
    val ContentPadding: PaddingValues = PaddingValues(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 8.dp)
    val TextButtonContentPadding: PaddingValues = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 8.dp)
    val MinWidth: Dp = 58.dp
    val MinHeight: Dp = 40.dp
    val IconSize: Dp = 18.dp
    val IconSpacing: Dp = 8.dp
    val shape: Shape @Composable get() = CircleShape

    @Composable
    fun buttonColors(containerColor: Color = MaterialTheme.colorScheme.primary,
                     contentColor: Color = MaterialTheme.colorScheme.onPrimary,
                     disabledContainerColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                     disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)): ButtonColors =
        ButtonColors(containerColor, contentColor, disabledContainerColor, disabledContentColor)

    @Composable
    fun filledTonalButtonColors(containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer): ButtonColors =
        buttonColors(containerColor, contentColor)

    @Composable
    fun outlinedButtonColors(containerColor: Color = Color.Transparent,
                             contentColor: Color = MaterialTheme.colorScheme.primary): ButtonColors =
        ButtonColors(containerColor, contentColor, Color.Transparent, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))

    @Composable
    fun textButtonColors(containerColor: Color = Color.Transparent,
                         contentColor: Color = MaterialTheme.colorScheme.primary): ButtonColors =
        ButtonColors(containerColor, contentColor, Color.Transparent, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))

    @Composable
    fun elevatedButtonColors(containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
                             contentColor: Color = MaterialTheme.colorScheme.primary): ButtonColors =
        buttonColors(containerColor, contentColor)
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    Surface(onClick, modifier, enabled, shape, colors.container(enabled), colors.content(enabled), border = border) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Row(Modifier.defaultMinSize(ButtonDefaults.MinWidth, ButtonDefaults.MinHeight).padding(contentPadding),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}

@Composable
fun FilledTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                      shape: Shape = ButtonDefaults.shape, colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
                      contentPadding: PaddingValues = ButtonDefaults.ContentPadding, content: @Composable RowScope.() -> Unit) =
    Button(onClick, modifier, enabled, shape, colors, null, contentPadding, content)

@Composable
fun ElevatedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                   shape: Shape = ButtonDefaults.shape, colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
                   contentPadding: PaddingValues = ButtonDefaults.ContentPadding, content: @Composable RowScope.() -> Unit) =
    Button(onClick, modifier, enabled, shape, colors, null, contentPadding, content)

@Composable
fun OutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                   shape: Shape = ButtonDefaults.shape, colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
                   border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                   contentPadding: PaddingValues = ButtonDefaults.ContentPadding, content: @Composable RowScope.() -> Unit) =
    Button(onClick, modifier, enabled, shape, colors, border, contentPadding, content)

@Composable
fun TextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
               shape: Shape = ButtonDefaults.shape, colors: ButtonColors = ButtonDefaults.textButtonColors(),
               contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding, content: @Composable RowScope.() -> Unit) =
    Button(onClick, modifier, enabled, shape, colors, null, contentPadding, content)

@Composable
fun IconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable () -> Unit) {
    val color = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
    Box(modifier.size(40.dp).clip(CircleShape).clickable(enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalContentColor provides color) { content() }
    }
}

@Composable
fun FloatingActionButton(onClick: () -> Unit, modifier: Modifier = Modifier,
                         shape: Shape = MaterialTheme.shapes.large,
                         containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
                         contentColor: Color = contentColorFor(containerColor),
                         content: @Composable () -> Unit) {
    Surface(onClick, modifier, true, shape, containerColor, contentColor, 6.dp) {
        Box(Modifier.defaultMinSize(56.dp, 56.dp), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun ExtendedFloatingActionButton(text: @Composable () -> Unit, icon: @Composable () -> Unit, onClick: () -> Unit,
                                 modifier: Modifier = Modifier, expanded: Boolean = true,
                                 containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
                                 contentColor: Color = contentColorFor(containerColor)) {
    Surface(onClick, modifier, true, MaterialTheme.shapes.large, containerColor, contentColor, 6.dp) {
        Row(Modifier.defaultMinSize(80.dp, 56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            if (expanded) {
                Spacer(Modifier.width(12.dp))
                ProvideTextStyle(MaterialTheme.typography.labelLarge) { text() }
            }
        }
    }
}

// --- chips ------------------------------------------------------------------

@Composable
fun FilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit, modifier: Modifier = Modifier,
               enabled: Boolean = true, leadingIcon: (@Composable () -> Unit)? = null,
               trailingIcon: (@Composable () -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    val container = if (selected) cs.secondaryContainer else Color.Transparent
    val content = if (selected) cs.onSecondaryContainer else cs.onSurfaceVariant
    Surface(onClick, modifier, enabled, MaterialTheme.shapes.small, container, content,
        border = if (selected) null else BorderStroke(1.dp, cs.outline)) {
        Row(Modifier.height(32.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (leadingIcon != null) Box(Modifier.size(18.dp)) { leadingIcon() }
            ProvideTextStyle(MaterialTheme.typography.labelLarge) { label() }
            if (trailingIcon != null) Box(Modifier.size(18.dp)) { trailingIcon() }
        }
    }
}

@Composable
fun AssistChip(onClick: () -> Unit, label: @Composable () -> Unit, modifier: Modifier = Modifier,
               leadingIcon: (@Composable () -> Unit)? = null) =
    FilterChip(false, onClick, label, modifier, true, leadingIcon)

// --- dividers and progress --------------------------------------------------

@Composable
fun HorizontalDivider(modifier: Modifier = Modifier, thickness: Dp = 1.dp,
                      color: Color = MaterialTheme.colorScheme.outlineVariant) {
    Box(modifier.fillMaxWidth().height(thickness).background(color))
}

@Composable
fun VerticalDivider(modifier: Modifier = Modifier, thickness: Dp = 1.dp,
                    color: Color = MaterialTheme.colorScheme.outlineVariant) {
    Box(modifier.width(thickness).background(color))
}

@Composable
fun LinearProgressIndicator(progress: () -> Float, modifier: Modifier = Modifier,
                            color: Color = MaterialTheme.colorScheme.primary,
                            trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest) {
    androidx.compose.foundation.Canvas(modifier.fillMaxWidth().height(4.dp)) {
        drawRoundRect(trackColor, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
        val p = Math.max(0f, Math.min(1f, progress()))
        drawRoundRect(color, size = androidx.compose.ui.geometry.Size(size.width * p, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
    }
}

@Composable
fun CircularProgressIndicator(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary,
                              strokeWidth: Dp = 4.dp) {
    val t = androidx.compose.animation.core.rememberInfiniteTransition()
    val angle = t.animateFloat(0f, 360f, androidx.compose.animation.core.infiniteRepeatable<Float>(
        androidx.compose.animation.core.tween<Float>(1100, easing = androidx.compose.animation.core.LinearEasing)))
    androidx.compose.foundation.Canvas(modifier.size(40.dp)) {
        val w = strokeWidth.toPx()
        drawArc(color, angle.value, 270f, false, androidx.compose.ui.geometry.Offset(w / 2, w / 2),
            androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
            style = androidx.compose.ui.graphics.drawscope.Stroke(w, androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

@Composable
fun CircularProgressIndicator(progress: () -> Float, modifier: Modifier = Modifier,
                              color: Color = MaterialTheme.colorScheme.primary, strokeWidth: Dp = 4.dp,
                              trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest) {
    androidx.compose.foundation.Canvas(modifier.size(40.dp)) {
        val w = strokeWidth.toPx()
        val o = androidx.compose.ui.geometry.Offset(w / 2, w / 2)
        val s = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
        val st = androidx.compose.ui.graphics.drawscope.Stroke(w, androidx.compose.ui.graphics.StrokeCap.Round)
        drawArc(trackColor, 0f, 360f, false, o, s, style = st)
        drawArc(color, -90f, 360f * Math.max(0f, Math.min(1f, progress())), false, o, s, style = st)
    }
}
