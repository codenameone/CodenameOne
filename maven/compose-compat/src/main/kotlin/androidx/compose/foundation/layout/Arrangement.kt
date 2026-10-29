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
package androidx.compose.foundation.layout

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.codename1.compose.internal.Density

/// How a Row or Column distributes its children along its main axis.
object Arrangement {
    @Immutable
    interface Horizontal {
        val spacing: Dp get() = Dp(0f)
        fun arrange(totalSize: Int, sizes: IntArray, layoutDirection: LayoutDirection, outPositions: IntArray)
    }

    @Immutable
    interface Vertical {
        val spacing: Dp get() = Dp(0f)
        fun arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray)
    }

    @Immutable
    interface HorizontalOrVertical : Horizontal, Vertical {
        override val spacing: Dp get() = Dp(0f)
    }

    private class Kind(val name: String, val gap: (Int, Int) -> Pair<Int, Int>) : HorizontalOrVertical {
        // gap(free, count) answers (leading, between).
        override fun arrange(totalSize: Int, sizes: IntArray, layoutDirection: LayoutDirection, outPositions: IntArray) =
            arrange(totalSize, sizes, outPositions)

        override fun arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
            var used = 0
            for (s in sizes) used += s
            val (lead, between) = gap(Math.max(0, totalSize - used), sizes.size)
            var p = lead
            for (i in sizes.indices) {
                outPositions[i] = p
                p += sizes[i] + between
            }
        }

        override fun toString(): String = "Arrangement#$name"
    }

    val Start: Horizontal = Kind("Start") { _, _ -> 0 to 0 }
    val End: Horizontal = Kind("End") { f, _ -> f to 0 }
    val Top: Vertical = Kind("Top") { _, _ -> 0 to 0 }
    val Bottom: Vertical = Kind("Bottom") { f, _ -> f to 0 }
    val Center: HorizontalOrVertical = Kind("Center") { f, _ -> f / 2 to 0 }
    val SpaceBetween: HorizontalOrVertical = Kind("SpaceBetween") { f, n -> 0 to if (n > 1) f / (n - 1) else 0 }
    val SpaceEvenly: HorizontalOrVertical = Kind("SpaceEvenly") { f, n -> f / (n + 1) to f / (n + 1) }
    val SpaceAround: HorizontalOrVertical = Kind("SpaceAround") { f, n ->
        if (n == 0) 0 to 0 else f / (2 * n) to f / n
    }

    /// Places children `space` apart, from the start.
    fun spacedBy(space: Dp): HorizontalOrVertical = SpacedAligned(space, null, null)

    fun spacedBy(space: Dp, alignment: Alignment.Horizontal): Horizontal = SpacedAligned(space, alignment, null)

    fun spacedBy(space: Dp, alignment: Alignment.Vertical): Vertical = SpacedAligned(space, null, alignment)

    private data class SpacedAligned(val space: Dp, val h: Alignment.Horizontal?, val v: Alignment.Vertical?) :
        HorizontalOrVertical {
        override val spacing: Dp get() = space

        override fun arrange(totalSize: Int, sizes: IntArray, layoutDirection: LayoutDirection, outPositions: IntArray) {
            place(totalSize, sizes, outPositions) { used -> h?.align(used, totalSize, layoutDirection) ?: 0 }
        }

        override fun arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
            place(totalSize, sizes, outPositions) { used -> v?.align(used, totalSize) ?: 0 }
        }

        private inline fun place(totalSize: Int, sizes: IntArray, out: IntArray, lead: (Int) -> Int) {
            val gap = Density.px(space)
            var used = 0
            for (s in sizes) used += s
            if (sizes.size > 1) used += gap * (sizes.size - 1)
            var p = lead(used)
            for (i in sizes.indices) {
                out[i] = p
                p += sizes[i] + gap
            }
        }
    }
}
