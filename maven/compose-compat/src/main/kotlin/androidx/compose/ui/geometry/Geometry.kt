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
package androidx.compose.ui.geometry

/// A point in pixels, relative to the drawing area's top left corner.
data class Offset(val x: Float, val y: Float) {
    operator fun plus(other: Offset): Offset = Offset(x + other.x, y + other.y)
    operator fun minus(other: Offset): Offset = Offset(x - other.x, y - other.y)
    operator fun times(operand: Float): Offset = Offset(x * operand, y * operand)
    operator fun div(operand: Float): Offset = Offset(x / operand, y / operand)
    operator fun unaryMinus(): Offset = Offset(-x, -y)

    companion object {
        val Zero: Offset = Offset(0f, 0f)
    }
}

/// A width and a height in pixels.
data class Size(val width: Float, val height: Float) {
    val minDimension: Float get() = Math.min(width, height)
    val maxDimension: Float get() = Math.max(width, height)
    val center: Offset get() = Offset(width / 2f, height / 2f)
    operator fun times(operand: Float): Size = Size(width * operand, height * operand)
    operator fun div(operand: Float): Size = Size(width / operand, height / operand)

    companion object {
        val Zero: Size = Size(0f, 0f)
    }
}

/// A rectangle in pixels.
data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    constructor(offset: Offset, size: Size) : this(offset.x, offset.y, offset.x + size.width, offset.y + size.height)

    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val size: Size get() = Size(width, height)
    val topLeft: Offset get() = Offset(left, top)
    val center: Offset get() = Offset(left + width / 2f, top + height / 2f)
}

/// A corner radius in pixels.
data class CornerRadius(val x: Float, val y: Float = x) {
    companion object {
        val Zero: CornerRadius = CornerRadius(0f, 0f)
    }
}
