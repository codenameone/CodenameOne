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
package androidx.compose.foundation.shape

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/// A corner's size, resolved against the shape's size.
@Immutable
interface CornerSize {
    fun toPx(shapeSize: Size, density: Density): Float
}

private data class DpCornerSize(val size: Dp) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float = size.value * density.density
}

private data class PercentCornerSize(val percent: Float) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float = shapeSize.minDimension * percent / 100f
}

private data class PxCornerSize(val px: Float) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float = px
}

fun CornerSize(size: Dp): CornerSize = DpCornerSize(size)
fun CornerSize(percent: Int): CornerSize = PercentCornerSize(percent.toFloat())
fun CornerSize(size: Float): CornerSize = PxCornerSize(size)

@Immutable
open class RoundedCornerShape(
    val topStart: CornerSize, val topEnd: CornerSize, val bottomEnd: CornerSize, val bottomStart: CornerSize
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val ts = topStart.toPx(size, density)
        val te = topEnd.toPx(size, density)
        val be = bottomEnd.toPx(size, density)
        val bs = bottomStart.toPx(size, density)
        if (ts + te + be + bs == 0f) return Outline.Rectangle(size.width, size.height)
        return Outline.Rounded(size.width, size.height, ts, te, be, bs)
    }

    override fun equals(other: Any?): Boolean = other is RoundedCornerShape && other.topStart == topStart &&
        other.topEnd == topEnd && other.bottomEnd == bottomEnd && other.bottomStart == bottomStart

    override fun hashCode(): Int = ((topStart.hashCode() * 31 + topEnd.hashCode()) * 31 + bottomEnd.hashCode()) * 31 +
        bottomStart.hashCode()

    fun copy(topStart: CornerSize = this.topStart, topEnd: CornerSize = this.topEnd, bottomEnd: CornerSize = this.bottomEnd,
             bottomStart: CornerSize = this.bottomStart): RoundedCornerShape =
        RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
}

fun RoundedCornerShape(corner: CornerSize): RoundedCornerShape = RoundedCornerShape(corner, corner, corner, corner)
fun RoundedCornerShape(size: Dp): RoundedCornerShape = RoundedCornerShape(CornerSize(size))
fun RoundedCornerShape(percent: Int): RoundedCornerShape = RoundedCornerShape(CornerSize(percent))
fun RoundedCornerShape(topStart: Dp = Dp(0f), topEnd: Dp = Dp(0f), bottomEnd: Dp = Dp(0f), bottomStart: Dp = Dp(0f)): RoundedCornerShape =
    RoundedCornerShape(CornerSize(topStart), CornerSize(topEnd), CornerSize(bottomEnd), CornerSize(bottomStart))

val CircleShape: RoundedCornerShape = RoundedCornerShape(50)
