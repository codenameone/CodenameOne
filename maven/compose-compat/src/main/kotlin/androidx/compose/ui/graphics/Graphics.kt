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
package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/// An sRGB color. The value is packed as Compose packs an sRGB color: the ARGB
/// integer in the upper 32 bits.
@JvmInline
value class Color(val value: ULong) {
    val alpha: Float get() = ((argb ushr 24) and 0xff) / 255f
    val red: Float get() = ((argb ushr 16) and 0xff) / 255f
    val green: Float get() = ((argb ushr 8) and 0xff) / 255f
    val blue: Float get() = (argb and 0xff) / 255f

    internal val argb: Int get() = (value shr 32).toInt()

    fun copy(alpha: Float = this.alpha, red: Float = this.red, green: Float = this.green, blue: Float = this.blue): Color =
        Color(red, green, blue, alpha)

    override fun toString(): String =
        if (this == Unspecified) "Color.Unspecified" else "Color(#" + Integer.toHexString(argb) + ")"

    companion object {
        val Black: Color = Color(0xFF000000)
        val DarkGray: Color = Color(0xFF444444)
        val Gray: Color = Color(0xFF888888)
        val LightGray: Color = Color(0xFFCCCCCC)
        val White: Color = Color(0xFFFFFFFF)
        val Red: Color = Color(0xFFFF0000)
        val Green: Color = Color(0xFF00FF00)
        val Blue: Color = Color(0xFF0000FF)
        val Yellow: Color = Color(0xFFFFFF00)
        val Cyan: Color = Color(0xFF00FFFF)
        val Magenta: Color = Color(0xFFFF00FF)
        val Transparent: Color = Color(0x00000000)

        /// Distinct from every real color: the default of a parameter that means
        /// "use the theme's".
        val Unspecified: Color = Color(0x10UL)
    }
}

fun Color(color: Long): Color = Color(((color and 0xffffffffL).toULong()) shl 32)

fun Color(color: Int): Color = Color((color.toLong() and 0xffffffffL).toULong() shl 32)

fun Color(red: Float, green: Float, blue: Float, alpha: Float = 1f): Color {
    fun c(v: Float): Int = Math.round(Math.max(0f, Math.min(1f, v)) * 255f)
    return Color((c(alpha) shl 24) or (c(red) shl 16) or (c(green) shl 8) or c(blue))
}

fun Color(red: Int, green: Int, blue: Int, alpha: Int = 0xff): Color =
    Color(((alpha and 0xff) shl 24) or ((red and 0xff) shl 16) or ((green and 0xff) shl 8) or (blue and 0xff))

val Color.isSpecified: Boolean get() = this != Color.Unspecified
val Color.isUnspecified: Boolean get() = this == Color.Unspecified

inline fun Color.takeOrElse(block: () -> Color): Color = if (isSpecified) this else block()

fun Color.toArgb(): Int = argb

fun lerp(start: Color, stop: Color, fraction: Float): Color {
    fun mix(a: Float, b: Float): Float = a + (b - a) * fraction
    return Color(mix(start.red, stop.red), mix(start.green, stop.green), mix(start.blue, stop.blue),
        mix(start.alpha, stop.alpha))
}

/// What fills a shape: one color, or a gradient sampled along an axis.
sealed class Brush {
    companion object {
        fun verticalGradient(colors: List<Color>, startY: Float = 0f, endY: Float = Float.POSITIVE_INFINITY): Brush =
            LinearGradient(colors, true)

        fun horizontalGradient(colors: List<Color>, startX: Float = 0f, endX: Float = Float.POSITIVE_INFINITY): Brush =
            LinearGradient(colors, false)

        fun linearGradient(colors: List<Color>): Brush = LinearGradient(colors, true)
    }
}

class SolidColor(val value: Color) : Brush()

internal class LinearGradient(val colors: List<Color>, val vertical: Boolean) : Brush()

/// The outline a shape resolves to at a given size.
sealed class Outline {
    class Rectangle(val width: Float, val height: Float) : Outline()

    /// A rounded rectangle; each radius is in pixels.
    class Rounded(
        val width: Float, val height: Float,
        val topLeft: Float, val topRight: Float, val bottomRight: Float, val bottomLeft: Float
    ) : Outline()
}

interface Shape {
    fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline
}

val RectangleShape: Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rectangle(size.width, size.height)

    override fun toString(): String = "RectangleShape"
}

/// A decoded bitmap. Wraps a Codename One image.
class ImageBitmap internal constructor(internal val image: com.codename1.ui.Image) {
    val width: Int get() = image.width
    val height: Int get() = image.height

    companion object {
        /// Wraps an image the application already has, for [androidx.compose.foundation.Image].
        fun fromCodenameOne(image: com.codename1.ui.Image): ImageBitmap = ImageBitmap(image)
    }
}

fun com.codename1.ui.Image.asImageBitmap(): ImageBitmap = ImageBitmap(this)

enum class StrokeCap { Butt, Round, Square }

object DefaultAlpha {
    const val value: Float = 1f
}

/// How a drawing combines with what is under it. Only normal source-over drawing
/// is supported; the other modes draw as [SrcOver].
enum class BlendMode { SrcOver, Src, Multiply, Screen }

class ColorFilter private constructor(internal val tint: Color) {
    companion object {
        fun tint(color: Color, blendMode: BlendMode = BlendMode.SrcOver): ColorFilter = ColorFilter(color)
    }
}

internal fun Offset.isFinite(): Boolean = !x.isInfinite() && !y.isInfinite()
