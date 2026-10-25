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
package androidx.compose.ui.unit

import com.codename1.compose.internal.Density as Cn1Density

/// A density-independent length: 1/160 of an inch, the unit every size in a
/// Compose layout is written in.
@JvmInline
value class Dp(val value: Float) : Comparable<Dp> {
    operator fun plus(other: Dp): Dp = Dp(value + other.value)
    operator fun minus(other: Dp): Dp = Dp(value - other.value)
    operator fun unaryMinus(): Dp = Dp(-value)
    operator fun div(other: Float): Dp = Dp(value / other)
    operator fun div(other: Int): Dp = Dp(value / other)
    operator fun div(other: Dp): Float = value / other.value
    operator fun times(other: Float): Dp = Dp(value * other)
    operator fun times(other: Int): Dp = Dp(value * other)
    override fun compareTo(other: Dp): Int = value.compareTo(other.value)
    override fun toString(): String = if (value.isNaN()) "Dp.Unspecified" else "$value.dp"

    companion object {
        val Hairline: Dp = Dp(0f)
        val Infinity: Dp = Dp(Float.POSITIVE_INFINITY)
        val Unspecified: Dp = Dp(Float.NaN)
    }
}

inline val Int.dp: Dp get() = Dp(this.toFloat())
inline val Float.dp: Dp get() = Dp(this)
inline val Double.dp: Dp get() = Dp(this.toFloat())

operator fun Int.times(other: Dp): Dp = Dp(this * other.value)
operator fun Float.times(other: Dp): Dp = Dp(this * other.value)

fun min(a: Dp, b: Dp): Dp = if (a.value <= b.value) a else b
fun max(a: Dp, b: Dp): Dp = if (a.value >= b.value) a else b

val Dp.isSpecified: Boolean get() = !value.isNaN()
val Dp.isUnspecified: Boolean get() = value.isNaN()

/// A width and a height in [Dp].
data class DpSize(val width: Dp, val height: Dp)

/// A text size. Only scaled pixels are supported; `em` is treated as a multiple of
/// the default body size.
@JvmInline
value class TextUnit internal constructor(internal val packed: Long) {
    val value: Float get() = Float.fromBits((packed and 0xffffffffL).toInt())
    val isSp: Boolean get() = (packed ushr 32) == 1L
    val isEm: Boolean get() = (packed ushr 32) == 2L
    val isUnspecified: Boolean get() = (packed ushr 32) == 0L
    val isSpecified: Boolean get() = !isUnspecified
    operator fun times(other: Float): TextUnit = textUnit(packed ushr 32, value * other)
    override fun toString(): String = when {
        isSp -> "$value.sp"
        isEm -> "$value.em"
        else -> "Unspecified"
    }

    companion object {
        val Unspecified: TextUnit = TextUnit(0L)
    }
}

internal fun textUnit(type: Long, value: Float): TextUnit =
    TextUnit((type shl 32) or (value.toRawBits().toLong() and 0xffffffffL))

val Int.sp: TextUnit get() = textUnit(1L, this.toFloat())
val Float.sp: TextUnit get() = textUnit(1L, this)
val Double.sp: TextUnit get() = textUnit(1L, this.toFloat())
val Int.em: TextUnit get() = textUnit(2L, this.toFloat())
val Float.em: TextUnit get() = textUnit(2L, this)
val Double.em: TextUnit get() = textUnit(2L, this.toFloat())

/// A size in pixels.
data class IntSize(val width: Int, val height: Int) {
    companion object {
        val Zero: IntSize = IntSize(0, 0)
    }
}

/// A position in pixels.
data class IntOffset(val x: Int, val y: Int) {
    companion object {
        val Zero: IntOffset = IntOffset(0, 0)
    }
}

enum class LayoutDirection { Ltr, Rtl }

/// Converts between [Dp] and pixels on the current display.
interface Density {
    val density: Float
    val fontScale: Float
    fun Dp.toPx(): Float = value * density
    fun Dp.roundToPx(): Int = Math.round(toPx())
    fun TextUnit.toPx(): Float = value * density * fontScale
    fun Int.toDp(): Dp = Dp(this / density)
    fun Float.toDp(): Dp = Dp(this / density)
}

fun Density(density: Float, fontScale: Float = 1f): Density = object : Density {
    override val density: Float = density
    override val fontScale: Float = fontScale
}

/// The display's density, for code that converts by hand.
val currentDisplayDensity: Density get() = Cn1Density.current
