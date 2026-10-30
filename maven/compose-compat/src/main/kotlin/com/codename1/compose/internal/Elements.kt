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
package com.codename1.compose.internal

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp

// The modifier elements the public extension functions create. Each is a value
// (data class) where it can be, so that an unchanged chain compares equal and a
// recomposition that rebuilds it does not relayout.

/// Changes how a node measures; applied in chain order.
internal sealed interface LayoutElement : Modifier.Element

internal data class PaddingElement(val start: Dp, val top: Dp, val end: Dp, val bottom: Dp) : LayoutElement

/// Padding by the system bars, read when the node is measured rather than when
/// it is composed: at the first composition the native view may not know its
/// safe area yet, and a value captured then would stay. The host re-measures
/// when the safe area changes (`ComposeHost.layoutChildren`).
internal data class SafeInsetsElement(val top: Boolean, val bottom: Boolean) : LayoutElement

/// A size request. An unspecified bound leaves the incoming one alone; with
/// `enforceIncoming` the result still respects what the parent allows.
internal data class SizeElement(
    val minWidth: Dp = Dp.Unspecified, val maxWidth: Dp = Dp.Unspecified,
    val minHeight: Dp = Dp.Unspecified, val maxHeight: Dp = Dp.Unspecified,
    val enforceIncoming: Boolean = true
) : LayoutElement

internal data class FillElement(val horizontal: Boolean, val vertical: Boolean, val fraction: Float) : LayoutElement

internal data class WrapElement(val horizontal: Boolean, val vertical: Boolean, val align: Alignment,
                                val unbounded: Boolean) : LayoutElement

internal data class OffsetElement(val x: Dp, val y: Dp) : LayoutElement

internal data class AspectRatioElement(val ratio: Float) : LayoutElement

internal class ScrollElement(val state: ScrollStateHandle, val vertical: Boolean) : LayoutElement {
    override fun equals(other: Any?): Boolean =
        other is ScrollElement && other.state === state && other.vertical == vertical

    override fun hashCode(): Int = System.identityHashCode(state) * 2 + if (vertical) 1 else 0
}

/// Read by the parent's measure policy, not by the node.
internal sealed interface ParentDataElement : Modifier.Element

internal data class WeightElement(val weight: Float, val fill: Boolean) : ParentDataElement

internal data class AlignSelfElement(val horizontal: Alignment.Horizontal?, val vertical: Alignment.Vertical?,
                                     val box: Alignment?) : ParentDataElement

internal object MatchParentSizeElement : ParentDataElement

/// Paints at the position it has in the chain.
internal sealed interface DrawElement : Modifier.Element

internal data class BackgroundElement(val color: Color, val brush: Brush?, val shape: Shape) : DrawElement

internal data class BorderElement(val width: Dp, val color: Color, val shape: Shape) : DrawElement

internal data class ClipElement(val shape: Shape) : DrawElement

internal class DrawBehindElement(val onDraw: DrawScope.() -> Unit) : DrawElement

internal data class AlphaElement(val alpha: Float) : Modifier.Element

internal class ClickableElement(val enabled: Boolean, val onClick: () -> Unit, val onLongClick: (() -> Unit)?,
                                val indication: Boolean) : Modifier.Element

internal class PointerTapElement(val onTap: (Float, Float) -> Unit) : Modifier.Element

internal data class TestTagElement(val tag: String) : Modifier.Element

internal data class SemanticsElement(val contentDescription: String?) : Modifier.Element

/// What a scroll modifier binds to; implemented by
/// [androidx.compose.foundation.ScrollState].
internal interface ScrollStateHandle {
    fun attach(node: UiNode, vertical: Boolean)
    fun detach(node: UiNode)
}
