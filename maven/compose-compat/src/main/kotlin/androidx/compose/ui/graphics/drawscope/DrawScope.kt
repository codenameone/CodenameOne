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
package androidx.compose.ui.graphics.drawscope

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/// How a shape is drawn: filled, or stroked with a width in pixels.
sealed class DrawStyle

object Fill : DrawStyle()

class Stroke(val width: Float = 0f, val cap: StrokeCap = StrokeCap.Butt) : DrawStyle()

/// The surface a [androidx.compose.foundation.Canvas] or `drawBehind` draws on.
/// Coordinates are pixels, from the top left corner of the drawing's box.
interface DrawScope : Density {
    val size: Size
    val layoutDirection: LayoutDirection get() = LayoutDirection.Ltr
    val center: Offset get() = Offset(size.width / 2f, size.height / 2f)

    fun drawRect(color: Color, topLeft: Offset = Offset.Zero, size: Size = this.size, alpha: Float = 1f,
                 style: DrawStyle = Fill)

    fun drawRoundRect(color: Color, topLeft: Offset = Offset.Zero, size: Size = this.size,
                      cornerRadius: CornerRadius = CornerRadius.Zero, alpha: Float = 1f, style: DrawStyle = Fill)

    fun drawCircle(color: Color, radius: Float = size.minDimension / 2f, center: Offset = this.center,
                   alpha: Float = 1f, style: DrawStyle = Fill)

    fun drawArc(color: Color, startAngle: Float, sweepAngle: Float, useCenter: Boolean,
                topLeft: Offset = Offset.Zero, size: Size = this.size, alpha: Float = 1f, style: DrawStyle = Fill)

    fun drawLine(color: Color, start: Offset, end: Offset, strokeWidth: Float = 0f,
                 cap: StrokeCap = StrokeCap.Butt, alpha: Float = 1f)

    fun drawImage(image: ImageBitmap, topLeft: Offset = Offset.Zero, alpha: Float = 1f)

    /// Draws `points` joined by straight segments.
    fun drawPolyline(color: Color, points: List<Offset>, strokeWidth: Float, alpha: Float = 1f)

    /// Fills the polygon `points` describe.
    fun drawPolygon(color: Color, points: List<Offset>, alpha: Float = 1f)

    fun translate(left: Float, top: Float, block: DrawScope.() -> Unit)
}

fun DrawScope.inset(horizontal: Float, vertical: Float, block: DrawScope.() -> Unit) {
    translate(horizontal, vertical, block)
}
