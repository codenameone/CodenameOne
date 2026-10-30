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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.codename1.compose.internal.AlignSelfElement
import com.codename1.compose.internal.AspectRatioElement
import com.codename1.compose.internal.BoxPolicy
import com.codename1.compose.internal.FillElement
import com.codename1.compose.internal.LayoutNode
import com.codename1.compose.internal.LeafNode
import com.codename1.compose.internal.LeafPolicy
import com.codename1.compose.internal.LinearPolicy
import com.codename1.compose.internal.MatchParentSizeElement
import com.codename1.compose.internal.OffsetElement
import com.codename1.compose.internal.PaddingElement
import com.codename1.compose.internal.SizeElement
import com.codename1.compose.internal.WeightElement
import com.codename1.compose.internal.WrapElement

// --- containers -------------------------------------------------------------

@Stable
interface BoxScope {
    fun Modifier.align(alignment: Alignment): Modifier = this.then(AlignSelfElement(null, null, alignment))
    fun Modifier.matchParentSize(): Modifier = this.then(MatchParentSizeElement)
}

@Stable
interface RowScope {
    fun Modifier.weight(weight: Float, fill: Boolean = true): Modifier = this.then(WeightElement(weight, fill))
    fun Modifier.align(alignment: Alignment.Vertical): Modifier = this.then(AlignSelfElement(null, alignment, null))
}

@Stable
interface ColumnScope {
    fun Modifier.weight(weight: Float, fill: Boolean = true): Modifier = this.then(WeightElement(weight, fill))
    fun Modifier.align(alignment: Alignment.Horizontal): Modifier = this.then(AlignSelfElement(alignment, null, null))
}

internal object BoxScopeInstance : BoxScope
internal object RowScopeInstance : RowScope
internal object ColumnScopeInstance : ColumnScope

@Composable
fun Box(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val policy = remember(contentAlignment, propagateMinConstraints) { BoxPolicy(contentAlignment, propagateMinConstraints) }
    LayoutNode(modifier, policy) { BoxScopeInstance.content() }
}

@Composable
fun Box(modifier: Modifier) {
    LeafNode(modifier, BoxPolicy.TopStart)
}

@Composable
fun Row(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit
) {
    val policy = remember(horizontalArrangement, verticalAlignment) {
        LinearPolicy(false, horizontalArrangement, null, null, verticalAlignment)
    }
    LayoutNode(modifier, policy) { RowScopeInstance.content() }
}

@Composable
fun Column(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val policy = remember(verticalArrangement, horizontalAlignment) {
        LinearPolicy(true, null, verticalArrangement, horizontalAlignment, null)
    }
    LayoutNode(modifier, policy) { ColumnScopeInstance.content() }
}

@Composable
fun Spacer(modifier: Modifier) {
    LeafNode(modifier, LeafPolicy)
}

// --- padding ----------------------------------------------------------------

@Immutable
interface PaddingValues {
    fun calculateLeftPadding(layoutDirection: androidx.compose.ui.unit.LayoutDirection): Dp
    fun calculateTopPadding(): Dp
    fun calculateRightPadding(layoutDirection: androidx.compose.ui.unit.LayoutDirection): Dp
    fun calculateBottomPadding(): Dp
}

@Immutable
private data class PaddingValuesImpl(val start: Dp, val top: Dp, val end: Dp, val bottom: Dp) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: androidx.compose.ui.unit.LayoutDirection): Dp = start
    override fun calculateTopPadding(): Dp = top
    override fun calculateRightPadding(layoutDirection: androidx.compose.ui.unit.LayoutDirection): Dp = end
    override fun calculateBottomPadding(): Dp = bottom
}

fun PaddingValues(all: Dp): PaddingValues = PaddingValuesImpl(all, all, all, all)
fun PaddingValues(horizontal: Dp = Dp(0f), vertical: Dp = Dp(0f)): PaddingValues =
    PaddingValuesImpl(horizontal, vertical, horizontal, vertical)
fun PaddingValues(start: Dp = Dp(0f), top: Dp = Dp(0f), end: Dp = Dp(0f), bottom: Dp = Dp(0f)): PaddingValues =
    PaddingValuesImpl(start, top, end, bottom)

fun Modifier.padding(all: Dp): Modifier = this.then(PaddingElement(all, all, all, all))
fun Modifier.padding(horizontal: Dp = Dp(0f), vertical: Dp = Dp(0f)): Modifier =
    this.then(PaddingElement(horizontal, vertical, horizontal, vertical))
fun Modifier.padding(start: Dp = Dp(0f), top: Dp = Dp(0f), end: Dp = Dp(0f), bottom: Dp = Dp(0f)): Modifier =
    this.then(PaddingElement(start, top, end, bottom))
fun Modifier.padding(paddingValues: PaddingValues): Modifier = this.then(PaddingElement(
    paddingValues.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr), paddingValues.calculateTopPadding(),
    paddingValues.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr), paddingValues.calculateBottomPadding()))

// --- size -------------------------------------------------------------------

fun Modifier.size(size: Dp): Modifier = this.then(SizeElement(size, size, size, size))
fun Modifier.size(width: Dp, height: Dp): Modifier = this.then(SizeElement(width, width, height, height))
fun Modifier.width(width: Dp): Modifier = this.then(SizeElement(minWidth = width, maxWidth = width))
fun Modifier.height(height: Dp): Modifier = this.then(SizeElement(minHeight = height, maxHeight = height))
fun Modifier.requiredSize(size: Dp): Modifier = this.then(SizeElement(size, size, size, size, false))
fun Modifier.requiredWidth(width: Dp): Modifier = this.then(SizeElement(minWidth = width, maxWidth = width, enforceIncoming = false))
fun Modifier.requiredHeight(height: Dp): Modifier = this.then(SizeElement(minHeight = height, maxHeight = height, enforceIncoming = false))
fun Modifier.sizeIn(minWidth: Dp = Dp.Unspecified, minHeight: Dp = Dp.Unspecified, maxWidth: Dp = Dp.Unspecified,
                    maxHeight: Dp = Dp.Unspecified): Modifier = this.then(SizeElement(minWidth, maxWidth, minHeight, maxHeight))
fun Modifier.widthIn(min: Dp = Dp.Unspecified, max: Dp = Dp.Unspecified): Modifier =
    this.then(SizeElement(minWidth = min, maxWidth = max))
fun Modifier.heightIn(min: Dp = Dp.Unspecified, max: Dp = Dp.Unspecified): Modifier =
    this.then(SizeElement(minHeight = min, maxHeight = max))
fun Modifier.defaultMinSize(minWidth: Dp = Dp.Unspecified, minHeight: Dp = Dp.Unspecified): Modifier =
    this.then(SizeElement(minWidth = minWidth, minHeight = minHeight))

fun Modifier.fillMaxWidth(fraction: Float = 1f): Modifier = this.then(FillElement(true, false, fraction))
fun Modifier.fillMaxHeight(fraction: Float = 1f): Modifier = this.then(FillElement(false, true, fraction))
fun Modifier.fillMaxSize(fraction: Float = 1f): Modifier = this.then(FillElement(true, true, fraction))

fun Modifier.wrapContentWidth(align: Alignment.Horizontal = Alignment.CenterHorizontally, unbounded: Boolean = false): Modifier =
    this.then(WrapElement(true, false, Alignment { s, sp, d -> androidx.compose.ui.unit.IntOffset(align.align(s.width, sp.width, d), 0) }, unbounded))
fun Modifier.wrapContentHeight(align: Alignment.Vertical = Alignment.CenterVertically, unbounded: Boolean = false): Modifier =
    this.then(WrapElement(false, true, Alignment { s, sp, _ -> androidx.compose.ui.unit.IntOffset(0, align.align(s.height, sp.height)) }, unbounded))
fun Modifier.wrapContentSize(align: Alignment = Alignment.Center, unbounded: Boolean = false): Modifier =
    this.then(WrapElement(true, true, align, unbounded))

fun Modifier.offset(x: Dp = Dp(0f), y: Dp = Dp(0f)): Modifier = this.then(OffsetElement(x, y))

fun Modifier.aspectRatio(ratio: Float): Modifier = this.then(AspectRatioElement(ratio))
