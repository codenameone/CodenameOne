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

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.codename1.ui.CN

/// The display's density in Compose terms: pixels per dp (1/160 inch). Read from
/// the port once a display exists; one pixel per dp before that, which only a
/// test that never starts a display sees.
internal object Density : androidx.compose.ui.unit.Density {
    private var cached = 0f

    override val density: Float
        get() {
            if (cached == 0f) {
                val perHundredMm = try {
                    CN.convertToPixels(100f)
                } catch (t: Throwable) {
                    0
                }
                // 1 dp = 1/160 in = 0.15875 mm.
                cached = if (perHundredMm > 0) perHundredMm / 100f * 0.15875f else 1f
            }
            return cached
        }

    override val fontScale: Float get() = 1f

    val current: androidx.compose.ui.unit.Density get() = this

    fun px(dp: Dp): Int = if (dp.value.isNaN()) 0 else Math.round(dp.value * density)

    fun pxF(dp: Dp): Float = if (dp.value.isNaN()) 0f else dp.value * density

    /// For tests that measure in a known unit.
    fun cn1Override(pixelsPerDp: Float) {
        cached = pixelsPerDp
    }
}

internal val LTR: LayoutDirection = LayoutDirection.Ltr

/// What a parent allows a child to be, in pixels. [INF] marks an unbounded axis.
internal class Constraints(val minWidth: Int, val maxWidth: Int, val minHeight: Int, val maxHeight: Int) {
    val hasBoundedWidth: Boolean get() = maxWidth != INF
    val hasBoundedHeight: Boolean get() = maxHeight != INF
    val hasFixedWidth: Boolean get() = minWidth == maxWidth
    val hasFixedHeight: Boolean get() = minHeight == maxHeight

    fun constrainWidth(w: Int): Int = Math.max(minWidth, Math.min(maxWidth, w))
    fun constrainHeight(h: Int): Int = Math.max(minHeight, Math.min(maxHeight, h))

    fun copy(minWidth: Int = this.minWidth, maxWidth: Int = this.maxWidth,
             minHeight: Int = this.minHeight, maxHeight: Int = this.maxHeight): Constraints =
        Constraints(minWidth, Math.max(minWidth, maxWidth), minHeight, Math.max(minHeight, maxHeight))

    /// Shrinks every bound by `horizontal` and `vertical` pixels (padding).
    fun offset(horizontal: Int, vertical: Int): Constraints {
        fun sub(v: Int, d: Int): Int = if (v == INF) INF else Math.max(0, v - d)
        return Constraints(Math.max(0, minWidth - horizontal), sub(maxWidth, horizontal),
            Math.max(0, minHeight - vertical), sub(maxHeight, vertical))
    }

    fun loose(): Constraints = Constraints(0, maxWidth, 0, maxHeight)

    override fun toString(): String = "Constraints(w=$minWidth..${s(maxWidth)}, h=$minHeight..${s(maxHeight)})"

    private fun s(v: Int): String = if (v == INF) "inf" else v.toString()

    companion object {
        const val INF: Int = Int.MAX_VALUE

        fun fixed(w: Int, h: Int): Constraints = Constraints(w, w, h, h)
    }
}

/// The parts of the display the system draws over (the status bar and the home
/// indicator on a notched phone) that a composition's host actually lies under,
/// in pixels. M3's top app bar and navigation bar pad themselves by these, as
/// Compose's window insets make them do; each host keeps its own. Measured against the host, not the
/// display: a host below a form's title area, in a dialog or mid-form is not
/// under the status bar, and padding it by the display's inset would leave a
/// band of empty colour.
internal object SafeInsets {
    /// The top and bottom insets of a host at `absY` that is `height` tall,
    /// packed as `top shl 32 or bottom`.
    fun of(absY: Int, height: Int): Long {
        val r = try {
            com.codename1.ui.Display.getInstance().getDisplaySafeArea(com.codename1.ui.geom.Rectangle())
        } catch (e: Throwable) {
            null
        }
        if (r == null || height <= 0) {
            return 0L
        }
        val t = Math.max(0, Math.min(height, r.y - absY))
        val b = Math.max(0, Math.min(height, absY + height - (r.y + r.height)))
        return (t.toLong() shl 32) or b.toLong()
    }
}
