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
package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import com.codename1.compose.internal.BackgroundElement
import com.codename1.compose.internal.BorderElement
import com.codename1.compose.internal.ClickableElement
import com.codename1.compose.internal.DrawBehindElement
import com.codename1.compose.internal.ImageNode
import com.codename1.compose.internal.LeafNode
import com.codename1.compose.internal.LeafPolicy
import com.codename1.compose.internal.ScrollElement
import com.codename1.compose.internal.ScrollStateHandle
import com.codename1.compose.internal.UiNode
import androidx.compose.runtime.ComposeNode
import com.codename1.compose.internal.NodeApplier

fun Modifier.background(color: Color, shape: Shape = RectangleShape): Modifier =
    this.then(BackgroundElement(color, null, shape))

fun Modifier.background(brush: Brush, shape: Shape = RectangleShape, alpha: Float = 1f): Modifier =
    this.then(BackgroundElement(Color.Unspecified, brush, shape))

fun Modifier.border(width: Dp, color: Color, shape: Shape = RectangleShape): Modifier =
    this.then(BorderElement(width, color, shape))

fun Modifier.border(border: BorderStroke, shape: Shape = RectangleShape): Modifier =
    this.then(BorderElement(border.width, border.color, shape))

class BorderStroke(val width: Dp, val color: Color)

fun Modifier.clickable(enabled: Boolean = true, onClickLabel: String? = null, onClick: () -> Unit): Modifier =
    this.then(ClickableElement(enabled, onClick, null, true))

fun Modifier.combinedClickable(enabled: Boolean = true, onLongClick: (() -> Unit)? = null, onClick: () -> Unit): Modifier =
    this.then(ClickableElement(enabled, onClick, onLongClick, true))

/// The scroll position of a [verticalScroll] or [horizontalScroll] modifier, in
/// pixels.
@Stable
class ScrollState(initial: Int) {
    private var node: UiNode? = null
    private var vertical = true
    private var stored by mutableIntStateOf(initial)

    val value: Int
        get() {
            val n = node ?: return stored
            return if (vertical) n.scrollY else n.scrollX
        }

    val maxValue: Int
        get() {
            val n = node ?: return 0
            return if (vertical) Math.max(0, n.scrollDimension.height - n.height)
            else Math.max(0, n.scrollDimension.width - n.width)
        }

    fun scrollTo(value: Int) {
        stored = value
        val n = node ?: return
        if (vertical) n.cn1ScrollY(value) else n.cn1ScrollX(value)
    }

    suspend fun animateScrollTo(value: Int) {
        scrollTo(value)
    }

    internal val handle: ScrollStateHandle = object : ScrollStateHandle {
        override fun attach(node: UiNode, vertical: Boolean) {
            this@ScrollState.node = node
            this@ScrollState.vertical = vertical
            if (stored != 0) {
                node.cn1ScrollPending(stored, vertical)
            }
        }

        override fun detach(node: UiNode) {
            if (this@ScrollState.node === node) this@ScrollState.node = null
        }
    }
}

@Composable
fun rememberScrollState(initial: Int = 0): ScrollState = remember { ScrollState(initial) }

fun Modifier.verticalScroll(state: ScrollState, enabled: Boolean = true): Modifier =
    this.then(ScrollElement(state.handle, true))

fun Modifier.horizontalScroll(state: ScrollState, enabled: Boolean = true): Modifier =
    this.then(ScrollElement(state.handle, false))

/// A box that draws with [onDraw]; its size comes from `modifier`.
@Composable
fun Canvas(modifier: Modifier, onDraw: DrawScope.() -> Unit) {
    LeafNode(modifier.then(DrawBehindElement(onDraw)), LeafPolicy)
}

@Composable
fun Image(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: androidx.compose.ui.layout.ContentScale = androidx.compose.ui.layout.ContentScale.Fit,
    alpha: Float = 1f
) {
    ComposeNode<ImageNode, NodeApplier>(
        factory = { ImageNode() },
        update = {
            set(modifier) { this.modifier = it }
            set(bitmap) { this.bitmap = it }
            set(contentScale) { this.contentScale = it }
        }
    )
}

@Composable
fun isSystemInDarkTheme(): Boolean = com.codename1.compose.internal.Theme.isDark()
