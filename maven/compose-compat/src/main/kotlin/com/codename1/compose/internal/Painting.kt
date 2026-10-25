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

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import com.codename1.ui.Graphics
import com.codename1.ui.geom.GeneralPath

/// Fills and strokes shapes on Codename One graphics.
internal object Painting {
    fun outline(shape: Shape, w: Int, h: Int): Outline =
        shape.createOutline(Size(w.toFloat(), h.toFloat()), LTR, Density)

    fun fill(g: Graphics, shape: Shape, color: Color, brush: Brush?, x: Int, y: Int, w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        val o = outline(shape, w, h)
        val b = brush
        if (b is LinearGradient && b.colors.size >= 2 && o is Outline.Rectangle) {
            val a = b.colors[0]
            val e = b.colors[b.colors.size - 1]
            val old = g.alpha
            g.concatenateAlpha(Math.round(a.alpha * 255))
            g.fillLinearGradient(a.argb and 0xffffff, e.argb and 0xffffff, x, y, w, h, !b.vertical)
            g.alpha = old
            return
        }
        val c = when (b) {
            is SolidColor -> b.value
            is LinearGradient -> if (b.colors.isEmpty()) color else b.colors[0]
            else -> color
        }
        if (c.alpha <= 0f) return
        val old = g.alpha
        g.color = c.argb and 0xffffff
        if (c.alpha < 1f) g.concatenateAlpha(Math.round(c.alpha * 255))
        when (o) {
            is Outline.Rounded -> {
                if (uniform(o)) {
                    val r = Math.round(o.topLeft * 2)
                    g.fillRoundRect(x, y, w, h, r, r)
                } else if (g.isShapeSupported) {
                    g.fillShape(roundedPath(o, x.toFloat(), y.toFloat()))
                } else {
                    val r = o.cornerArc()
                    g.fillRoundRect(x, y, w, h, r, r)
                }
            }
            else -> g.fillRect(x, y, w, h)
        }
        g.alpha = old
    }

    fun stroke(g: Graphics, shape: Shape, color: Color, width: Int, x: Int, y: Int, w: Int, h: Int) {
        if (w <= 0 || h <= 0 || width <= 0 || color.alpha <= 0f) return
        val old = g.alpha
        g.color = color.argb and 0xffffff
        if (color.alpha < 1f) g.concatenateAlpha(Math.round(color.alpha * 255))
        val o = outline(shape, w, h)
        val half = width / 2f
        if (g.isShapeSupported) {
            val path = when (o) {
                is Outline.Rounded -> roundedPath(Outline.Rounded(w - width.toFloat(), h - width.toFloat(),
                    Math.max(0f, o.topLeft - half), Math.max(0f, o.topRight - half),
                    Math.max(0f, o.bottomRight - half), Math.max(0f, o.bottomLeft - half)), x + half, y + half)
                else -> rectPath(x + half, y + half, w - width.toFloat(), h - width.toFloat())
            }
            g.drawShape(path, com.codename1.ui.Stroke(width.toFloat(), com.codename1.ui.Stroke.CAP_BUTT,
                com.codename1.ui.Stroke.JOIN_MITER, 4f))
        } else {
            for (i in 0 until width) {
                when (o) {
                    is Outline.Rounded -> {
                        val r = o.cornerArc()
                        g.drawRoundRect(x + i, y + i, w - 1 - 2 * i, h - 1 - 2 * i, r, r)
                    }
                    else -> g.drawRect(x + i, y + i, w - 1 - 2 * i, h - 1 - 2 * i)
                }
            }
        }
        g.alpha = old
    }

    private fun uniform(o: Outline.Rounded): Boolean =
        o.topLeft == o.topRight && o.topRight == o.bottomRight && o.bottomRight == o.bottomLeft

    fun rectPath(x: Float, y: Float, w: Float, h: Float): GeneralPath {
        val p = GeneralPath()
        p.moveTo(x, y)
        p.lineTo(x + w, y)
        p.lineTo(x + w, y + h)
        p.lineTo(x, y + h)
        p.closePath()
        return p
    }

    fun roundedPath(o: Outline.Rounded, x: Float, y: Float): GeneralPath {
        val w = o.width
        val h = o.height
        val max = Math.min(w, h) / 2f
        val tl = Math.min(o.topLeft, max)
        val tr = Math.min(o.topRight, max)
        val br = Math.min(o.bottomRight, max)
        val bl = Math.min(o.bottomLeft, max)
        val p = GeneralPath()
        p.moveTo(x + tl, y)
        p.lineTo(x + w - tr, y)
        if (tr > 0) p.arc(x + w - 2 * tr, y, 2 * tr, 2 * tr, HALF_PI, -HALF_PI, true)
        p.lineTo(x + w, y + h - br)
        if (br > 0) p.arc(x + w - 2 * br, y + h - 2 * br, 2 * br, 2 * br, 0f, -HALF_PI, true)
        p.lineTo(x + bl, y + h)
        if (bl > 0) p.arc(x, y + h - 2 * bl, 2 * bl, 2 * bl, -HALF_PI, -HALF_PI, true)
        p.lineTo(x, y + tl)
        if (tl > 0) p.arc(x, y, 2 * tl, 2 * tl, PI, -HALF_PI, true)
        p.closePath()
        return p
    }

    private const val PI = Math.PI.toFloat()
    private const val HALF_PI = (Math.PI / 2).toFloat()
}

/// [DrawScope] over Codename One graphics, already translated to the box's
/// origin.
internal class Cn1DrawScope(private val g: Graphics, override val size: Size) : DrawScope {
    override val density: Float get() = Density.density
    override val fontScale: Float get() = 1f

    private inline fun paint(color: Color, alpha: Float, block: () -> Unit) {
        val a = color.alpha * alpha
        if (a <= 0f) return
        val old = g.alpha
        g.color = color.argb and 0xffffff
        if (a < 1f) g.concatenateAlpha(Math.round(a * 255))
        g.isAntiAliased = true
        try {
            block()
        } finally {
            g.alpha = old
        }
    }

    private fun stroke(width: Float, cap: StrokeCap): com.codename1.ui.Stroke =
        com.codename1.ui.Stroke(Math.max(1f, width), when (cap) {
            StrokeCap.Round -> com.codename1.ui.Stroke.CAP_ROUND
            StrokeCap.Square -> com.codename1.ui.Stroke.CAP_SQUARE
            else -> com.codename1.ui.Stroke.CAP_BUTT
        }, com.codename1.ui.Stroke.JOIN_ROUND, 4f)

    private fun r(v: Float): Int = Math.round(v)

    override fun drawRect(color: Color, topLeft: Offset, size: Size, alpha: Float, style: DrawStyle) = paint(color, alpha) {
        if (style is Stroke) {
            g.drawShape(Painting.rectPath(topLeft.x, topLeft.y, size.width, size.height), stroke(style.width, style.cap))
        } else {
            g.fillRect(r(topLeft.x), r(topLeft.y), r(size.width), r(size.height))
        }
    }

    override fun drawRoundRect(color: Color, topLeft: Offset, size: Size, cornerRadius: CornerRadius, alpha: Float,
                               style: DrawStyle) = paint(color, alpha) {
        val rr = cornerRadius.x
        if (style is Stroke) {
            g.drawShape(Painting.roundedPath(Outline.Rounded(size.width, size.height, rr, rr, rr, rr), topLeft.x, topLeft.y),
                stroke(style.width, style.cap))
        } else {
            g.fillRoundRect(r(topLeft.x), r(topLeft.y), r(size.width), r(size.height), r(rr * 2), r(cornerRadius.y * 2))
        }
    }

    override fun drawCircle(color: Color, radius: Float, center: Offset, alpha: Float, style: DrawStyle) = paint(color, alpha) {
        if (style is Stroke) {
            val p = GeneralPath()
            p.arc(center.x - radius, center.y - radius, radius * 2, radius * 2, 0f, (Math.PI * 2).toFloat())
            g.drawShape(p, stroke(style.width, style.cap))
        } else {
            g.fillArc(r(center.x - radius), r(center.y - radius), r(radius * 2), r(radius * 2), 0, 360)
        }
    }

    override fun drawArc(color: Color, startAngle: Float, sweepAngle: Float, useCenter: Boolean, topLeft: Offset,
                         size: Size, alpha: Float, style: DrawStyle) = paint(color, alpha) {
        // Compose measures angles clockwise; Codename One counter-clockwise.
        if (style is Stroke) {
            val p = GeneralPath()
            val rad = (Math.PI / 180).toFloat()
            p.arc(topLeft.x, topLeft.y, size.width, size.height, -startAngle * rad, -sweepAngle * rad)
            g.drawShape(p, stroke(style.width, style.cap))
        } else {
            g.fillArc(r(topLeft.x), r(topLeft.y), r(size.width), r(size.height), r(-startAngle), r(-sweepAngle))
        }
    }

    override fun drawLine(color: Color, start: Offset, end: Offset, strokeWidth: Float, cap: StrokeCap, alpha: Float) =
        paint(color, alpha) {
            if (strokeWidth <= 1f) {
                g.drawLine(r(start.x), r(start.y), r(end.x), r(end.y))
            } else {
                val p = GeneralPath()
                p.moveTo(start.x, start.y)
                p.lineTo(end.x, end.y)
                g.drawShape(p, stroke(strokeWidth, cap))
            }
        }

    override fun drawImage(image: ImageBitmap, topLeft: Offset, alpha: Float) = paint(Color.Black, alpha) {
        g.drawImage(image.image, r(topLeft.x), r(topLeft.y))
    }

    override fun drawPolyline(color: Color, points: List<Offset>, strokeWidth: Float, alpha: Float) = paint(color, alpha) {
        if (points.size >= 2) {
            val p = GeneralPath()
            p.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) p.lineTo(points[i].x, points[i].y)
            g.drawShape(p, stroke(strokeWidth, StrokeCap.Round))
        }
    }

    override fun drawPolygon(color: Color, points: List<Offset>, alpha: Float) = paint(color, alpha) {
        if (points.size >= 3) {
            val p = GeneralPath()
            p.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) p.lineTo(points[i].x, points[i].y)
            p.closePath()
            g.fillShape(p)
        }
    }

    override fun translate(left: Float, top: Float, block: DrawScope.() -> Unit) {
        val dx = r(left)
        val dy = r(top)
        g.translate(dx, dy)
        try {
            block()
        } finally {
            g.translate(-dx, -dy)
        }
    }
}
