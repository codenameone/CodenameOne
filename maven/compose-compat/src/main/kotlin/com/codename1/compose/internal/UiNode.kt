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

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import com.codename1.ui.Component
import com.codename1.ui.Container
import com.codename1.ui.Graphics
import com.codename1.ui.geom.Dimension
import com.codename1.ui.layouts.Layout
import com.codename1.ui.plaf.Border

/// Lays out the children of a node. `measure` returns the content size and
/// records, through [UiNode.place], where each child goes relative to the content
/// box's top left corner.
internal interface MeasurePolicy {
    fun measure(node: UiNode, children: List<Component>, constraints: Constraints): Long
}

internal fun size(w: Int, h: Int): Long = (w.toLong() shl 32) or (h.toLong() and 0xffffffffL)
internal fun Long.w(): Int = (this ushr 32).toInt()
internal fun Long.h(): Int = this.toInt()

/// A Compose layout node, which is also a Codename One container: the node's
/// children are Codename One components, and so is everything a composable emits.
///
/// Compose's layout model is reproduced here rather than mapped onto Codename One
/// layouts. A node is measured with [Constraints] and answers a size; its modifier
/// chain is walked outermost first, each layout element transforming the
/// constraints going in and the size coming out (padding shrinks one and grows the
/// other); the innermost box is where [policy] measures the children. Drawing
/// elements paint at the box their position in the chain gives them.
///
/// Measuring happens from the root of a composition ([ComposeHost]) in one pass,
/// which also fixes every child's position. Codename One's own layout pass then
/// only copies those positions out ([NodeLayout]); a node it lays out on its own
/// re-measures at its current size.
internal open class UiNode : Container(NodeLayout) {
    var policy: MeasurePolicy = BoxPolicy.TopStart
        set(value) {
            if (field != value) {
                field = value
                invalidateMeasure()
            }
        }

    var modifier: Modifier = Modifier
        set(value) {
            if (field != value) {
                val old = field
                field = value
                resolveModifier(old)
                invalidateMeasure()
                repaint()
            }
        }

    // The resolved chain.
    internal var chain: Array<Modifier.Element> = EMPTY
    internal var weight: WeightElement? = null
    internal var alignSelf: AlignSelfElement? = null
    internal var matchParentSize: Boolean = false
    internal var alpha: Float = 1f
    internal var clickable: ClickableElement? = null
    internal var tap: PointerTapElement? = null
    private var scroll: ScrollElement? = null

    // Measure results: the size at every chain position, and the box each one
    // occupies once placed (x, y, w, h per element, then the content box).
    private var sizes = LongArray(1)
    private var boxes = IntArray(4)
    internal var measured = false

    /// Whether a [SafeInsetsElement] is in the chain, so the host re-measures
    /// this node (and only it, with its ancestors) when the safe area changes.
    internal var usesSafeInsets = false

    /// The top inset this node was measured with, so placing it cannot read a
    /// newer one.
    private var placedInsetTop = 0
    private var lastConstraints: Constraints? = null
    private var placements = IntArray(0)
    internal var contentX = 0
    internal var contentY = 0
    internal var contentW = 0
    internal var contentH = 0

    // Offset modifiers move the whole node when its parent places it.
    internal var offsetX = 0
    internal var offsetY = 0

    init {
        cn1StripStyle(this)
    }

    // --- modifier resolution ------------------------------------------------

    private fun resolveModifier(old: Modifier) {
        val list = ArrayList<Modifier.Element>()
        modifier.foldIn(list) { acc, e -> acc.add(e); acc }
        var weight: WeightElement? = null
        var align: AlignSelfElement? = null
        var match = false
        var alpha = 1f
        var click: ClickableElement? = null
        var tap: PointerTapElement? = null
        var scroll: ScrollElement? = null
        var tag: String? = null
        var description: String? = null
        val chain = ArrayList<Modifier.Element>(list.size)
        for (e in list) {
            when (e) {
                is WeightElement -> weight = e
                is AlignSelfElement -> align = e
                is MatchParentSizeElement -> match = true
                is AlphaElement -> alpha *= e.alpha
                is ClickableElement -> click = e
                is PointerTapElement -> tap = e
                is TestTagElement -> tag = e.tag
                is SemanticsElement -> description = e.contentDescription
                is ScrollElement -> {
                    scroll = e
                    chain.add(e)
                }
                is LayoutElement, is DrawElement -> chain.add(e)
                else -> {}
            }
        }
        this.chain = chain.toTypedArray()
        var insets = false
        for (k in 0 until chain.size) {
            if (chain[k] is SafeInsetsElement) {
                insets = true
            }
        }
        usesSafeInsets = insets
        this.weight = weight
        this.alignSelf = align
        this.matchParentSize = match
        this.alpha = alpha
        this.clickable = click
        this.tap = tap
        if (this.scroll?.state !== scroll?.state) {
            this.scroll?.state?.detach(this)
            scroll?.state?.attach(this, scroll.vertical)
        }
        this.scroll = scroll
        isScrollableY = (scroll != null && scroll.vertical) || lazyAxis() == 1
        isScrollableX = (scroll != null && !scroll.vertical) || lazyAxis() == 2
        val interactive = (click != null && click.enabled) || tap != null
        isGrabsPointerEvents = interactive
        isFocusable = interactive
        if (tag != null) {
            name = tag
        }
        if (description != null) {
            accessibilityText = description
        }
        sizes = LongArray(this.chain.size + 1)
        boxes = IntArray((this.chain.size + 1) * 4)
    }

    // --- measuring ----------------------------------------------------------

    /// Marks this node and every ancestor up to the composition's root as needing
    /// a new measure pass, and asks the host for one.
    fun invalidateMeasure() {
        var n: Container? = this
        while (n != null) {
            if (n is UiNode) {
                if (!n.measured && n !== this) {
                    break
                }
                n.measured = false
                n.setShouldCalcPreferredSize(true)
            } else if (n is ComposeHost) {
                n.requestRemeasure()
                break
            }
            n = n.parent
        }
    }

    /// The host this node is composed into, or null while it is detached.
    private fun composeHost(): ComposeHost? {
        var n: Container? = parent
        while (n != null) {
            if (n is ComposeHost) {
                return n
            }
            n = n.parent
        }
        return null
    }

    /// Measures this node under `c` and answers its size (packed with [size]).
    fun measure(c: Constraints): Long {
        val last = lastConstraints
        if (measured && last != null && last.minWidth == c.minWidth && last.maxWidth == c.maxWidth &&
            last.minHeight == c.minHeight && last.maxHeight == c.maxHeight) {
            return sizes[0]
        }
        val result = measureChain(0, c)
        lastConstraints = c
        measured = true
        placeChain()
        return result
    }

    val measuredWidth: Int get() = sizes[0].w()
    val measuredHeight: Int get() = sizes[0].h()

    private fun measureChain(i: Int, c: Constraints): Long {
        if (i == chain.size) {
            val s = measureContent(c)
            val r = size(c.constrainWidth(s.w()), c.constrainHeight(s.h()))
            sizes[i] = r
            return r
        }
        val d = Density
        val r: Long = when (val e = chain[i]) {
            is SafeInsetsElement -> {
                val host = composeHost()
                placedInsetTop = if (e.top && host != null) host.insetTop else 0
                val v = placedInsetTop + (if (e.bottom && host != null) host.insetBottom else 0)
                val inner = measureChain(i + 1, c.offset(0, v))
                size(c.constrainWidth(inner.w()), c.constrainHeight(inner.h() + v))
            }
            is PaddingElement -> {
                val l = d.px(e.start)
                val t = d.px(e.top)
                val h = l + d.px(e.end)
                val v = t + d.px(e.bottom)
                val inner = measureChain(i + 1, c.offset(h, v))
                size(c.constrainWidth(inner.w() + h), c.constrainHeight(inner.h() + v))
            }
            is SizeElement -> {
                val inner = measureChain(i + 1, sizeConstraints(e, c))
                size(c.constrainWidth(inner.w()), c.constrainHeight(inner.h()))
            }
            is FillElement -> {
                var minW = c.minWidth
                var maxW = c.maxWidth
                var minH = c.minHeight
                var maxH = c.maxHeight
                if (e.horizontal && c.hasBoundedWidth) {
                    val w = c.constrainWidth(Math.round(c.maxWidth * e.fraction))
                    minW = w
                    maxW = w
                }
                if (e.vertical && c.hasBoundedHeight) {
                    val h = c.constrainHeight(Math.round(c.maxHeight * e.fraction))
                    minH = h
                    maxH = h
                }
                val inner = measureChain(i + 1, Constraints(minW, maxW, minH, maxH))
                size(c.constrainWidth(inner.w()), c.constrainHeight(inner.h()))
            }
            is WrapElement -> {
                val inner = measureChain(i + 1, Constraints(
                    if (e.horizontal) 0 else c.minWidth,
                    if (e.horizontal && e.unbounded) Constraints.INF else c.maxWidth,
                    if (e.vertical) 0 else c.minHeight,
                    if (e.vertical && e.unbounded) Constraints.INF else c.maxHeight))
                size(c.constrainWidth(inner.w()), c.constrainHeight(inner.h()))
            }
            is AspectRatioElement -> {
                var cc = c
                if (c.hasBoundedWidth && !c.hasFixedHeight) {
                    val h = c.constrainHeight(Math.round(c.maxWidth / e.ratio))
                    cc = Constraints(c.maxWidth, c.maxWidth, h, h)
                } else if (c.hasBoundedHeight) {
                    val w = c.constrainWidth(Math.round(c.maxHeight * e.ratio))
                    cc = Constraints(w, w, c.maxHeight, c.maxHeight)
                }
                val inner = measureChain(i + 1, cc)
                size(c.constrainWidth(inner.w()), c.constrainHeight(inner.h()))
            }
            is ScrollElement -> {
                val inner = if (e.vertical) {
                    measureChain(i + 1, Constraints(c.minWidth, c.maxWidth, 0, Constraints.INF))
                } else {
                    measureChain(i + 1, Constraints(0, Constraints.INF, c.minHeight, c.maxHeight))
                }
                size(c.constrainWidth(inner.w()), c.constrainHeight(inner.h()))
            }
            else -> measureChain(i + 1, c)
        }
        sizes[i] = r
        return r
    }

    private fun sizeConstraints(e: SizeElement, c: Constraints): Constraints {
        val d = Density
        var minW = if (e.minWidth.value.isNaN()) -1 else d.px(e.minWidth)
        var maxW = if (e.maxWidth.value.isNaN()) -1 else if (e.maxWidth.value.isInfinite()) Constraints.INF else d.px(e.maxWidth)
        var minH = if (e.minHeight.value.isNaN()) -1 else d.px(e.minHeight)
        var maxH = if (e.maxHeight.value.isNaN()) -1 else if (e.maxHeight.value.isInfinite()) Constraints.INF else d.px(e.maxHeight)
        if (e.enforceIncoming) {
            if (minW >= 0) minW = c.constrainWidth(minW)
            if (maxW >= 0) maxW = c.constrainWidth(maxW)
            if (minH >= 0) minH = c.constrainHeight(minH)
            if (maxH >= 0) maxH = c.constrainHeight(maxH)
        }
        val rMinW = if (minW >= 0) minW else if (maxW >= 0) Math.min(c.minWidth, maxW) else c.minWidth
        val rMaxW = if (maxW >= 0) Math.max(maxW, rMinW) else Math.max(c.maxWidth, rMinW)
        val rMinH = if (minH >= 0) minH else if (maxH >= 0) Math.min(c.minHeight, maxH) else c.minHeight
        val rMaxH = if (maxH >= 0) Math.max(maxH, rMinH) else Math.max(c.maxHeight, rMinH)
        return Constraints(rMinW, rMaxW, rMinH, rMaxH)
    }

    /// Measures what is inside the innermost box. Containers run [policy] over
    /// their children; leaves override this.
    protected open fun measureContent(c: Constraints): Long {
        val kids = ArrayList<Component>(componentCount)
        for (k in 0 until componentCount) {
            kids.add(getComponentAt(k))
        }
        if (placements.size != kids.size * 4) {
            placements = IntArray(kids.size * 4)
        }
        return policy.measure(this, kids, c)
    }

    /// Records where a child goes, relative to the content box.
    fun place(index: Int, x: Int, y: Int, w: Int, h: Int) {
        if (placements.size < (index + 1) * 4) {
            placements = placements.copyOf((index + 1) * 4)
        }
        placements[index * 4] = x
        placements[index * 4 + 1] = y
        placements[index * 4 + 2] = w
        placements[index * 4 + 3] = h
    }

    /// Fixes each chain position's box, outermost first.
    private fun placeChain() {
        val d = Density
        var x = 0
        var y = 0
        offsetX = 0
        offsetY = 0
        var leading = true
        for (i in 0..chain.size) {
            val w = sizes[i].w()
            val h = sizes[i].h()
            boxes[i * 4] = x
            boxes[i * 4 + 1] = y
            boxes[i * 4 + 2] = w
            boxes[i * 4 + 3] = h
            if (i == chain.size) {
                break
            }
            val e = chain[i]
            val nw = sizes[i + 1].w()
            val nh = sizes[i + 1].h()
            when (e) {
                is SafeInsetsElement -> {
                    y += placedInsetTop
                    leading = false
                }
                is PaddingElement -> {
                    x += d.px(e.start)
                    y += d.px(e.top)
                    leading = false
                }
                is OffsetElement -> {
                    if (leading) {
                        offsetX += d.px(e.x)
                        offsetY += d.px(e.y)
                    } else {
                        x += d.px(e.x)
                        y += d.px(e.y)
                    }
                }
                is WrapElement -> {
                    val o = e.align.align(androidx.compose.ui.unit.IntSize(nw, nh),
                        androidx.compose.ui.unit.IntSize(w, h), LTR)
                    x += o.x
                    y += o.y
                    leading = false
                }
                is ScrollElement -> leading = false
                is LayoutElement -> {
                    // A box forced larger than its content centers it, as Compose does.
                    x += (w - nw) / 2
                    y += (h - nh) / 2
                    leading = false
                }
                else -> leading = false
            }
        }
        val n = chain.size * 4
        contentX = boxes[n]
        contentY = boxes[n + 1]
        contentW = boxes[n + 2]
        contentH = boxes[n + 3]
    }

    /// Copies the positions the last measure recorded onto the children.
    internal fun applyPlacements() {
        val count = componentCount
        for (k in 0 until count) {
            val child = getComponentAt(k)
            if (placements.size < (k + 1) * 4) {
                break
            }
            var x = contentX + placements[k * 4]
            var y = contentY + placements[k * 4 + 1]
            if (child is UiNode) {
                x += child.offsetX
                y += child.offsetY
            }
            child.setX(x)
            child.setY(y)
            child.setWidth(placements[k * 4 + 2])
            child.setHeight(placements[k * 4 + 3])
        }
    }

    /// The axis a node scrolls along by itself, without a scroll modifier: 1 for
    /// vertical, 2 for horizontal. Lazy lists do.
    internal open fun lazyAxis(): Int = 0

    /// Measures again under the last constraints, at the same size, and places
    /// the whole subtree now rather than on the next layout pass.
    internal fun remeasureInPlace() {
        val lc = lastConstraints ?: return
        val w = measuredWidth
        val h = measuredHeight
        measured = false
        measure(lc)
        if (measuredWidth != w || measuredHeight != h) {
            // The parent placed this node at the old size; only a full pass can move its siblings.
            invalidateMeasure()
            return
        }
        applyTree()
        repaint()
    }

    private fun applyTree() {
        applyPlacements()
        for (k in 0 until componentCount) {
            val child = getComponentAt(k)
            if (child is UiNode) {
                child.applyTree()
            } else if (child is Container) {
                child.layoutContainer()
            }
        }
    }

    internal fun layoutFromCn1() {
        val w = width
        val h = height
        if (!measured || sizes[0].w() != w || sizes[0].h() != h) {
            measure(Constraints.fixed(w, h))
        }
        applyPlacements()
        if (pendingScroll >= 0) {
            val v = pendingScroll
            pendingScroll = -1
            if (isScrollableY) cn1ScrollY(v) else cn1ScrollX(v)
        }
    }

    override fun calcScrollSize(): Dimension {
        // The scrolled extent is the box just inside the scroll modifier.
        var n = chain.size * 4
        for (i in chain.indices) {
            if (chain[i] is ScrollElement) {
                n = (i + 1) * 4
                break
            }
        }
        return Dimension(Math.max(width, boxes[n] + boxes[n + 2]), Math.max(height, boxes[n + 1] + boxes[n + 3]))
    }

    override fun calcPreferredSize(): Dimension {
        val s = measure(Constraints(0, cn1DisplayWidth(), 0, Constraints.INF))
        return Dimension(s.w(), s.h())
    }

    // --- painting -----------------------------------------------------------

    override fun paintBackground(g: Graphics) {
        val old = g.alpha
        if (alpha < 1f) {
            g.concatenateAlpha(Math.round(alpha * 255))
        }
        g.isAntiAliased = true
        val ax = x
        val ay = y
        for (i in chain.indices) {
            val e = chain[i]
            if (e !is DrawElement) {
                continue
            }
            val bx = ax + boxes[i * 4]
            val by = ay + boxes[i * 4 + 1]
            val bw = boxes[i * 4 + 2]
            val bh = boxes[i * 4 + 3]
            when (e) {
                is BackgroundElement -> Painting.fill(g, e.shape, e.color, e.brush, bx, by, bw, bh)
                is BorderElement -> Painting.stroke(g, e.shape, e.color, Density.px(e.width), bx, by, bw, bh)
                is DrawBehindElement -> {
                    g.translate(bx, by)
                    try {
                        e.onDraw(Cn1DrawScope(g, Size(bw.toFloat(), bh.toFloat())))
                    } finally {
                        g.translate(-bx, -by)
                    }
                }
                else -> {}
            }
        }
        g.alpha = old
    }

    override fun paint(g: Graphics) {
        val old = g.alpha
        if (alpha < 1f) {
            g.concatenateAlpha(Math.round(alpha * 255))
        }
        val clip = clipBox()
        if (clip >= 0) {
            g.pushClip()
            clipTo(g, (chain[clip] as ClipElement).shape, x + boxes[clip * 4], y + boxes[clip * 4 + 1],
                boxes[clip * 4 + 2], boxes[clip * 4 + 3])
        }
        paintContent(g)
        if (clip >= 0) {
            g.popClip()
        }
        val click = clickable
        if (pressed && click != null && click.indication) {
            g.color = 0x000000
            g.concatenateAlpha(30)
            val shape = indicationShape()
            Painting.fill(g, shape, androidx.compose.ui.graphics.Color.Black, null, x, y, width, height)
        }
        g.alpha = old
    }

    /// Paints what is inside the content box; containers paint their children.
    protected open fun paintContent(g: Graphics) {
        super.paint(g)
    }

    /// Clips to `shape`, as Compose does: a card's children stay inside its
    /// rounded corners. `setClip(Shape)` replaces the clip, so the path is
    /// first cut to the clip in force, or a card scrolled under a bar would
    /// paint over it.
    private fun clipTo(g: Graphics, shape: Shape, cx: Int, cy: Int, cw: Int, ch: Int) {
        val o = Painting.outline(shape, cw, ch)
        if (o is androidx.compose.ui.graphics.Outline.Rounded && g.isShapeClipSupported) {
            if (!Painting.clipRounded(g, o, cx, cy)) {
                // The clip in force misses the outline (it may still meet a
                // corner the rounding cuts off): nothing is visible.
                g.clipRect(0, 0, 0, 0)
            }
            return
        }
        g.clipRect(cx, cy, cw, ch)
    }

    private fun clipBox(): Int {
        for (i in chain.indices) {
            if (chain[i] is ClipElement) {
                return i
            }
        }
        return -1
    }

    private fun indicationShape(): Shape {
        for (e in chain) {
            if (e is ClipElement) return e.shape
            if (e is BackgroundElement) return e.shape
        }
        return androidx.compose.ui.graphics.RectangleShape
    }

    // --- input --------------------------------------------------------------

    private var pressed = false
    private var pressX = 0
    private var pressY = 0
    private var moved = false

    override fun pointerPressed(x: Int, y: Int) {
        super.pointerPressed(x, y)
        if (clickable?.enabled == true || tap != null) {
            pressed = true
            moved = false
            pressX = x
            pressY = y
            repaint()
        }
    }

    override fun pointerDragged(x: Int, y: Int) {
        if (pressed && !moved) {
            val slop = Density.px(androidx.compose.ui.unit.Dp(8f))
            if (Math.abs(x - pressX) > slop || Math.abs(y - pressY) > slop) {
                moved = true
                pressed = false
                repaint()
            }
        }
        super.pointerDragged(x, y)
    }

    override fun pointerReleased(x: Int, y: Int) {
        super.pointerReleased(x, y)
        if (pressed) {
            pressed = false
            repaint()
            if (!moved && contains(x, y)) {
                tap?.onTap?.invoke((x - absoluteX).toFloat(), (y - absoluteY).toFloat())
                val c = clickable
                if (c != null && c.enabled) {
                    c.onClick()
                }
            }
        }
    }

    /// Clicks the node as a tap would, for tests and automation.
    fun cn1PerformClick(): Boolean {
        val c = clickable ?: return false
        if (!c.enabled) return false
        c.onClick()
        return true
    }

    internal fun cn1ScrollY(v: Int) {
        scrollY = Math.max(0, Math.min(v, Math.max(0, scrollDimension.height - height)))
    }

    private var pendingScroll = -1

    /// A scroll position to restore once the node has been measured.
    internal fun cn1ScrollPending(v: Int, vertical: Boolean) {
        pendingScroll = v
    }

    internal fun cn1ScrollX(v: Int) {
        scrollX = Math.max(0, Math.min(v, Math.max(0, scrollDimension.width - width)))
    }

    companion object {
        private val EMPTY = arrayOf<Modifier.Element>()
    }
}

/// Codename One's layout pass for a node: copies out what measuring decided.
internal object NodeLayout : Layout() {
    override fun layoutContainer(parent: Container) {
        // Checked, never cast: a failed cast does not throw on iOS.
        (parent as? UiNode)?.layoutFromCn1()
    }

    override fun getPreferredSize(parent: Container): Dimension {
        val node = parent as? UiNode ?: return Dimension(0, 0)
        val s = node.measure(Constraints(0, cn1DisplayWidth(), 0, Constraints.INF))
        return Dimension(s.w(), s.h())
    }
}

internal fun cn1DisplayWidth(): Int = try {
    com.codename1.ui.CN.getDisplayWidth().let { if (it > 0) it else 1080 }
} catch (t: Throwable) {
    1080
}

internal fun cn1DisplayHeight(): Int = try {
    com.codename1.ui.CN.getDisplayHeight().let { if (it > 0) it else 1920 }
} catch (t: Throwable) {
    1920
}

/// Removes every theme decoration from a component that a node paints itself.
///
/// A node is never focused, pressed or disabled, so only its unselected style is
/// ever read. Asking for `allStyles` created the other three, each with its own
/// background painter, for every node: 3,335 styles and 1 MB of a 150-card
/// screen's heap. A real input that takes focus passes `allStates`.
internal fun cn1StripStyle(c: Component, allStates: Boolean = false) {
    val s = if (allStates) c.allStyles else c.unselectedStyle
    s.setPadding(0, 0, 0, 0)
    s.setMargin(0, 0, 0, 0)
    s.setBgTransparency(0)
    s.setBorder(Border.getEmpty())
}

/// Measures any child: a node by its own rules, any other Codename One component
/// by its preferred size, constrained.
internal fun measureChild(child: Component, c: Constraints): Long {
    if (child is UiNode) {
        return child.measure(c)
    }
    val pw = child.preferredW
    val ph = child.preferredH
    return size(c.constrainWidth(pw), c.constrainHeight(ph))
}

internal fun Outline.cornerArc(): Int = when (this) {
    is Outline.Rounded -> Math.round(Math.max(Math.max(topLeft, topRight), Math.max(bottomLeft, bottomRight)) * 2)
    else -> 0
}
