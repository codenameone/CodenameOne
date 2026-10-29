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
package androidx.compose.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/// Where a child of a given size goes in the space it has.
@Immutable
fun interface Alignment {
    fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection): IntOffset

    @Immutable
    fun interface Horizontal {
        fun align(size: Int, space: Int, layoutDirection: LayoutDirection): Int
    }

    @Immutable
    fun interface Vertical {
        fun align(size: Int, space: Int): Int
    }

    companion object {
        val TopStart: Alignment = BiasAlignment(-1f, -1f)
        val TopCenter: Alignment = BiasAlignment(0f, -1f)
        val TopEnd: Alignment = BiasAlignment(1f, -1f)
        val CenterStart: Alignment = BiasAlignment(-1f, 0f)
        val Center: Alignment = BiasAlignment(0f, 0f)
        val CenterEnd: Alignment = BiasAlignment(1f, 0f)
        val BottomStart: Alignment = BiasAlignment(-1f, 1f)
        val BottomCenter: Alignment = BiasAlignment(0f, 1f)
        val BottomEnd: Alignment = BiasAlignment(1f, 1f)

        val Top: Vertical = BiasAlignment.Vertical(-1f)
        val CenterVertically: Vertical = BiasAlignment.Vertical(0f)
        val Bottom: Vertical = BiasAlignment.Vertical(1f)

        val Start: Horizontal = BiasAlignment.Horizontal(-1f)
        val CenterHorizontally: Horizontal = BiasAlignment.Horizontal(0f)
        val End: Horizontal = BiasAlignment.Horizontal(1f)
    }
}

/// An alignment expressed as a bias: -1 is the start or top, 0 the center, 1 the
/// end or bottom.
@Immutable
data class BiasAlignment(val horizontalBias: Float, val verticalBias: Float) : Alignment {
    override fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection): IntOffset {
        val h = Horizontal(horizontalBias).align(size.width, space.width, layoutDirection)
        val v = Vertical(verticalBias).align(size.height, space.height)
        return IntOffset(h, v)
    }

    @Immutable
    data class Horizontal(val bias: Float) : Alignment.Horizontal {
        override fun align(size: Int, space: Int, layoutDirection: LayoutDirection): Int {
            val b = if (layoutDirection == LayoutDirection.Ltr) bias else -bias
            return Math.round((space - size) / 2f * (1 + b))
        }
    }

    @Immutable
    data class Vertical(val bias: Float) : Alignment.Vertical {
        override fun align(size: Int, space: Int): Int = Math.round((space - size) / 2f * (1 + bias))
    }
}
