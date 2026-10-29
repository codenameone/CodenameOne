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
package androidx.compose.foundation.lazy

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.codename1.compose.internal.BoxPolicy
import com.codename1.compose.internal.Constraints
import com.codename1.compose.internal.Density
import com.codename1.compose.internal.LTR
import com.codename1.compose.internal.NodeApplier
import com.codename1.compose.internal.UiNode
import com.codename1.compose.internal.h
import com.codename1.compose.internal.measureChild
import com.codename1.compose.internal.size
import com.codename1.compose.internal.w
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight

/// The receiver of an item's content.
@Stable
interface LazyItemScope {
    fun Modifier.fillParentMaxWidth(fraction: Float = 1f): Modifier = this.fillMaxWidth(fraction)
    fun Modifier.fillParentMaxHeight(fraction: Float = 1f): Modifier = this.fillMaxHeight(fraction)
    fun Modifier.animateItem(): Modifier = this
}

private object LazyItemScopeInstance : LazyItemScope

/// What a [LazyColumn] or [LazyRow] content block declares.
interface LazyListScope {
    fun item(key: Any? = null, contentType: Any? = null, content: @Composable LazyItemScope.() -> Unit)
    fun items(count: Int, key: ((Int) -> Any)? = null, contentType: (Int) -> Any? = { null },
              itemContent: @Composable LazyItemScope.(Int) -> Unit)
    fun stickyHeader(key: Any? = null, contentType: Any? = null, content: @Composable LazyItemScope.() -> Unit) =
        item(key, contentType, content)
}

inline fun <T> LazyListScope.items(items: List<T>, noinline key: ((T) -> Any)? = null,
                                   crossinline itemContent: @Composable LazyItemScope.(T) -> Unit) {
    items(items.size, if (key != null) { i: Int -> key(items[i]) } else null) { itemContent(items[it]) }
}

inline fun <T> LazyListScope.itemsIndexed(items: List<T>, noinline key: ((Int, T) -> Any)? = null,
                                          crossinline itemContent: @Composable LazyItemScope.(Int, T) -> Unit) {
    items(items.size, if (key != null) { i: Int -> key(i, items[i]) } else null) { itemContent(it, items[it]) }
}

private class Entry(val start: Int, val count: Int, val key: ((Int) -> Any)?,
                    val content: @Composable LazyItemScope.(Int) -> Unit)

/// What one run of a lazy list's content block declared. Only the items in view
/// are ever composed from it.
internal class LazyItems : LazyListScope {
    private val entries = ArrayList<Entry>(2)
    var count = 0
        private set

    override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
        entries.add(Entry(count, 1, if (key != null) { _: Int -> key } else null) { content() })
        count++
    }

    override fun items(count: Int, key: ((Int) -> Any)?, contentType: (Int) -> Any?,
                       itemContent: @Composable LazyItemScope.(Int) -> Unit) {
        entries.add(Entry(this.count, count, key, itemContent))
        this.count += count
    }

    private fun entryAt(index: Int): Entry {
        var e = entries[0]
        for (k in 1 until entries.size) {
            val n = entries[k]
            if (n.start > index) break
            e = n
        }
        return e
    }

    /// The item's key: the one the content block gave it, or its position.
    fun keyAt(index: Int): Any {
        val e = entryAt(index)
        val k = e.key ?: return IndexKey(index)
        return k(index - e.start)
    }

    @Composable
    fun Content(index: Int) {
        val e = entryAt(index)
        e.content(LazyItemScopeInstance, index - e.start)
    }
}

private data class IndexKey(val index: Int)

/// The scroll position of a lazy list.
@Stable
class LazyListState internal constructor(internal val scroll: ScrollState) {
    internal var node: LazyListNode? = null

    val firstVisibleItemIndex: Int get() = node?.cn1FirstVisible() ?: 0
    val firstVisibleItemScrollOffset: Int get() = scroll.value
    val canScrollForward: Boolean get() = scroll.value < scroll.maxValue
    val canScrollBackward: Boolean get() = scroll.value > 0

    suspend fun scrollToItem(index: Int, scrollOffset: Int = 0) {
        val n = node
        if (n != null) n.cn1ScrollToItem(index, scrollOffset) else scroll.scrollTo(scrollOffset)
    }

    suspend fun animateScrollToItem(index: Int, scrollOffset: Int = 0) = scrollToItem(index, scrollOffset)
}

@Composable
fun rememberLazyListState(): LazyListState = remember { LazyListState(ScrollState(0)) }

/// A vertically scrolling list that composes only the items in view, and
/// disposes of an item's composition once it has scrolled well out of it.
@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    LazyList(true, modifier, state, contentPadding, verticalArrangement.spacing, horizontalAlignment, null,
        reverseLayout, userScrollEnabled, content)
}

@Composable
fun LazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    LazyList(false, modifier, state, contentPadding, horizontalArrangement.spacing, null, verticalAlignment,
        reverseLayout, userScrollEnabled, content)
}

@Composable
private fun LazyList(vertical: Boolean, modifier: Modifier, state: LazyListState, padding: PaddingValues,
                     spacing: androidx.compose.ui.unit.Dp, horizontal: Alignment.Horizontal?,
                     verticalAlign: Alignment.Vertical?, reverseLayout: Boolean, userScrollEnabled: Boolean,
                     content: LazyListScope.() -> Unit) {
    // Laying the items out from the end is not implemented. Refusing is better than drawing a
    // chat-style list upside down and letting it look like it works.
    require(!reverseLayout) { "reverseLayout = true is not supported by LazyColumn/LazyRow" }
    val context = rememberCompositionContext()
    val holder = remember { NodeHolder() }
    DisposableEffect(holder) {
        onDispose { holder.node?.cn1Dispose() }
    }
    // The declared items are rebuilt only when the content block changes or a state it read does.
    // A parent recomposition that hands back the same block (the Compose compiler memoizes it when
    // its captures are stable) reuses the same LazyItems, so `set(items)` below does not fire: no
    // remap, and no item composition is given new content.
    val items = remember(content) { derivedStateOf { LazyItems().also(content) } }.value
    ComposeNode<LazyListNode, NodeApplier>(
        factory = { LazyListNode(vertical).also { holder.node = it } },
        update = {
            set(context) { this.context = it }
            set(state) { this.state = it }
            set(padding) { this.padding = it }
            set(spacing) { this.spacing = it }
            set(horizontal) { this.horizontalAlignment = it }
            set(verticalAlign) { this.verticalAlignment = it }
            set(userScrollEnabled) { this.userScroll = it }
            set(modifier) { this.modifier = it }
            set(items) { this.items = it }
        }
    )
}

/// Lets the list's item compositions be disposed of with the list.
private class NodeHolder {
    var node: LazyListNode? = null
}

/// The node behind [LazyColumn] and [LazyRow]. It is its own scroll container,
/// and every item in view is a composition of its own under the list's, rooted
/// at a node that is this one's child. Item sizes are learnt as items are
/// measured; the extent of the ones never seen is estimated from the average.
///
/// A pass costs the window, not the list: measured extents and their count are
/// kept in two Fenwick trees, so where any item starts is O(log n) and the item
/// at a position O(log^2 n), and the pass walks only the items within reach of
/// the view. Only a change of content is O(n), to carry sizes over by key.
internal class LazyListNode(private val vertical: Boolean) : UiNode() {
    private class Slot(val key: Any, val root: UiNode, val composition: Composition) {
        var index = -1
        var items: LazyItems? = null
        var live = false
        var start = 0
    }

    var context: CompositionContext? = null
    var state: LazyListState? = null
        set(v) {
            if (field !== v) {
                field?.node = null
                field?.scroll?.handle?.detach(this)
                field = v
                v?.node = this
                v?.scroll?.handle?.attach(this, vertical)
            }
        }
    var padding: PaddingValues = PaddingValues()
        set(v) { if (field != v) { field = v; invalidateMeasure() } }
    var spacing: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp(0f)
        set(v) { if (field != v) { field = v; invalidateMeasure() } }
    var userScroll = true
        set(v) {
            if (field != v) {
                field = v
                if (vertical) isScrollableY = v else isScrollableX = v
            }
        }
    var horizontalAlignment: Alignment.Horizontal? = null
    var verticalAlignment: Alignment.Vertical? = null
    var items: LazyItems? = null
        set(v) {
            val old = field
            field = v
            if (v != null) remap(old, v)
            invalidateMeasure()
        }

    // Composed items, by key and as a list; the list is what a pass walks, so that
    // nothing allocates an iterator.
    private val slots = HashMap<Any, Slot>()
    private val slotList = ArrayList<Slot>()
    // Per item: its key, and its extent along the scroll axis once measured (-1 before).
    private var keys = arrayOfNulls<Any>(0)
    private var extents = IntArray(0)
    // Fenwick trees over the measured extents and over how many are measured.
    private var sumTree = IntArray(1)
    private var countTree = IntArray(1)
    private var knownCount = 0
    private var knownSum = 0
    // The geometry of the last pass, which positions are computed with until the next.
    private var lead = 0
    private var gap = 0
    private var estimate = 0
    private var total = 0
    private var crossExtent = 0
    private var windowAt = Int.MIN_VALUE
    private var viewport = 0
    private var measuring = false
    // The item the view is anchored on across a pass that learns sizes: the first one in view and
    // how far into it the view starts. Without it an item above the view measured larger than its
    // estimate pushes everything in view down, and the list jumps under a still finger.
    private var anchorKey: Any? = null
    private var anchorOffset = 0
    private var anchorIndex = -1
    // Set when the anchor was taken by a remap or by scrollToItem and must be used as it is;
    // otherwise every pass takes it afresh from where the view is now.
    private var anchorFixed = false

    init {
        policy = BoxPolicy.TopStart
        if (vertical) isScrollableY = true else isScrollableX = true
    }

    override fun lazyAxis(): Int = if (!userScroll) 0 else if (vertical) 1 else 2

    private fun treeAdd(t: IntArray, index: Int, v: Int) {
        var i = index + 1
        while (i < t.size) {
            t[i] += v
            i += i and -i
        }
    }

    /// The sum over items [0, index).
    private fun treeSum(t: IntArray, index: Int): Int {
        var i = index
        var s = 0
        while (i > 0) {
            s += t[i]
            i -= i and -i
        }
        return s
    }

    private fun setExtent(i: Int, e: Int) {
        val old = extents[i]
        if (old == e) return
        extents[i] = e
        if (old < 0) {
            treeAdd(countTree, i, 1)
            knownCount++
            treeAdd(sumTree, i, e)
            knownSum += e
        } else {
            treeAdd(sumTree, i, e - old)
            knownSum += e - old
        }
    }

    /// Where item `i` starts (or, for `i == n`, where the gap after the last one ends).
    private fun startOf(i: Int): Int {
        val kc = treeSum(countTree, i)
        // In Long and clamped: a list of millions of estimated items passes Int.MAX_VALUE pixels.
        val v = lead.toLong() + treeSum(sumTree, i) + (i - kc).toLong() * estimate + i.toLong() * gap
        return if (v > MAX_EXTENT) MAX_EXTENT.toInt() else v.toInt()
    }

    private companion object {
        // Below Constraints.INF, so a clamped extent never reads as "unbounded".
        const val MAX_EXTENT = (Int.MAX_VALUE / 2).toLong()
    }

    private fun extentOf(i: Int): Int = if (extents[i] >= 0) extents[i] else estimate

    /// The first item that ends past `at`, or -1 for an empty list.
    private fun firstAt(at: Int): Int {
        val n = keys.size
        var lo = 0
        var hi = n - 1
        var found = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (startOf(mid) + extentOf(mid) > at) {
                found = mid
                hi = mid - 1
            } else {
                lo = mid + 1
            }
        }
        return found
    }

    /// Carries what is known of each item over to a new content block, by key.
    private fun remap(old: LazyItems?, now: LazyItems) {
        captureAnchor()
        anchorIndex = -1
        anchorFixed = true
        val n = now.count
        val oldKeys = keys
        val oldExtents = extents
        val newKeys = arrayOfNulls<Any>(n)
        val newExtents = IntArray(n)
        var byKey: HashMap<Any, Int>? = null
        val ak = anchorKey
        for (i in 0 until n) {
            val k = now.keyAt(i)
            newKeys[i] = k
            if (anchorIndex < 0 && ak != null && k == ak) anchorIndex = i
            var e = -1
            if (i < oldKeys.size && oldKeys[i] == k) {
                e = oldExtents[i]
            } else if (old != null && oldKeys.isNotEmpty()) {
                if (byKey == null) {
                    byKey = HashMap(oldKeys.size * 2)
                    for (j in oldKeys.indices) {
                        // The first of a repeated key wins; a repeat is refused once composed.
                        val ok = oldKeys[j]!!
                        if (!byKey.containsKey(ok)) byKey[ok] = j
                    }
                }
                val j = byKey[k]
                if (j != null) e = oldExtents[j]
            }
            newExtents[i] = e
        }
        keys = newKeys
        extents = IntArray(n) { -1 }
        sumTree = IntArray(n + 1)
        countTree = IntArray(n + 1)
        knownCount = 0
        knownSum = 0
        for (i in 0 until n) {
            if (newExtents[i] >= 0) setExtent(i, newExtents[i])
        }
    }

    override fun measureContent(c: Constraints): Long {
        val list = items
        val ctx = context
        val n = list?.count ?: 0
        if (list == null || ctx == null || n == 0) {
            disposeAll()
            total = 0
            return size(c.minWidth, c.minHeight)
        }
        measuring = true
        try {
            return measureItems(list, ctx, c, n)
        } finally {
            measuring = false
        }
    }

    private fun measureItems(list: LazyItems, ctx: CompositionContext, c: Constraints, n: Int): Long {
        // The anchor is taken with the geometry the view was drawn with, before this pass
        // changes the estimate.
        if (!anchorFixed) {
            captureAnchor()
        }
        val d = Density
        val p = padding
        lead = if (vertical) d.px(p.calculateTopPadding()) else d.px(p.calculateLeftPadding(LTR))
        val trail = if (vertical) d.px(p.calculateBottomPadding()) else d.px(p.calculateRightPadding(LTR))
        val crossLead = if (vertical) d.px(p.calculateLeftPadding(LTR)) else d.px(p.calculateTopPadding())
        val crossTrail = if (vertical) d.px(p.calculateRightPadding(LTR)) else d.px(p.calculateBottomPadding())
        gap = d.px(spacing)
        // Fixed for the pass, so that every position in it is computed the same way.
        estimate = if (knownCount > 0) knownSum / knownCount else d.px(androidx.compose.ui.unit.Dp(72f))
        val viewMain = if (vertical) c.maxHeight else c.maxWidth
        val crossMax = if (vertical) c.maxWidth else c.maxHeight
        val crossAvail = if (crossMax == Constraints.INF) Constraints.INF else Math.max(0, crossMax - crossLead - crossTrail)
        val itemC = if (vertical) Constraints(0, crossAvail, 0, Constraints.INF)
        else Constraints(0, Constraints.INF, 0, crossAvail)
        val unbounded = viewMain == Constraints.INF
        var at = if (vertical) scrollY else scrollX
        val ai0 = anchorIndex
        if (!unbounded && at > 0 && ai0 in 0 until n) {
            // Where the anchor is under this pass's estimate. The estimate moves as sizes are
            // learnt, and a window chosen from the old position can miss every item.
            val want = Math.max(0, startOf(ai0) + anchorOffset)
            if (want != at) {
                at = want
                if (vertical) scrollY = at else scrollX = at
            }
        }
        // Compose what is in view and a quarter of a view either side; keep what was
        // composed until it is half a view away, so a slow scroll does not churn.
        val margin = if (unbounded) 0 else viewMain / 4
        val composeFrom = if (unbounded) Int.MIN_VALUE else at - margin
        val composeTo = if (unbounded) Int.MAX_VALUE else at + viewMain + margin
        val keepFrom = if (unbounded) Int.MIN_VALUE else at - 2 * margin
        val keepTo = if (unbounded) Int.MAX_VALUE else at + viewMain + 2 * margin
        for (k in 0 until slotList.size) slotList[k].live = false
        var i = if (unbounded) 0 else Math.max(0, firstAt(keepFrom))
        var pos = startOf(i)
        var cross = 0
        while (i < n && pos < keepTo) {
            var e = extents[i]
            val guess = if (e >= 0) e else estimate
            val key = keys[i]!!
            var slot = slots[key]
            val overlaps = pos + guess > composeFrom && pos < composeTo
            val kept = slot != null && pos + guess > keepFrom
            if (overlaps || kept) {
                if (slot == null) {
                    slot = compose(key, ctx)
                } else if (slot.live) {
                    // Two items share a slot; the second would silently take the first's
                    // composition. Like Compose's own, the check covers the items composed.
                    throw IllegalArgumentException("Key " + key + " was already used. Every item of a "
                        + "lazy list must have a unique key.")
                }
                if (slot.items !== list || slot.index != i) {
                    slot.items = list
                    slot.index = i
                    val l = list
                    val idx = i
                    slot.composition.setContent { l.Content(idx) }
                }
                val m = measureChild(slot.root, itemC)
                e = if (vertical) m.h() else m.w()
                setExtent(i, e)
                cross = Math.max(cross, if (vertical) m.w() else m.h())
                slot.live = true
                slot.start = pos
            } else {
                e = guess
            }
            pos += e + gap
            i++
        }
        val end = startOf(n) - gap + trail
        if (total != end) {
            total = end
            // Component caches the scroll extent and clamps setScrollY against it; without this
            // the anchor below is clamped to the extent the estimate gave last pass.
            setScrollSize(null)
        }
        disposeDead()
        crossExtent = cross + crossLead + crossTrail
        val crossSize = crossExtent
        // Place what is composed; positions are along the whole scrolled extent.
        for (k in 0 until slotList.size) {
            val s = slotList[k]
            val ci = getComponentIndex(s.root)
            if (ci < 0) continue
            val iw = s.root.measuredWidth
            val ih = s.root.measuredHeight
            if (vertical) {
                val a = horizontalAlignment
                val x = crossLead + (if (a == null || crossAvail == Constraints.INF) 0 else a.align(iw, crossAvail, LTR))
                place(ci, x, s.start, iw, ih)
            } else {
                val a = verticalAlignment
                val y = crossLead + (if (a == null || crossAvail == Constraints.INF) 0 else a.align(ih, crossAvail))
                place(ci, s.start, y, iw, ih)
            }
        }
        viewport = if (unbounded) total else viewMain
        var now = at
        val ai = anchorIndex
        if (!unbounded && at > 0 && ai in 0 until n) {
            val want = Math.max(0, Math.min(startOf(ai) + anchorOffset, total - viewport))
            if (want != at) {
                now = want
                if (vertical) scrollY = want else scrollX = want
            }
        }
        windowAt = now
        anchorFixed = false
        return if (vertical) size(c.constrainWidth(crossSize), c.constrainHeight(total))
        else size(c.constrainWidth(total), c.constrainHeight(crossSize))
    }

    private fun compose(key: Any, ctx: CompositionContext): Slot {
        val root = UiNode()
        addComponent(root)
        val s = Slot(key, root, Composition(NodeApplier(root), ctx))
        slots[key] = s
        slotList.add(s)
        return s
    }

    private fun disposeDead() {
        var w = 0
        for (k in 0 until slotList.size) {
            val s = slotList[k]
            if (s.live) {
                slotList[w++] = s
            } else {
                slots.remove(s.key)
                s.composition.dispose()
                removeComponent(s.root)
            }
        }
        while (slotList.size > w) slotList.removeAt(slotList.size - 1)
    }

    private fun disposeAll() {
        for (k in 0 until slotList.size) slotList[k].live = false
        disposeDead()
    }

    override fun calcScrollSize(): com.codename1.ui.geom.Dimension =
        if (vertical) com.codename1.ui.geom.Dimension(Math.max(width, crossExtent), Math.max(height, total))
        else com.codename1.ui.geom.Dimension(Math.max(width, total), Math.max(height, crossExtent))

    override fun onScrollY(scrollY: Int) {
        super.onScrollY(scrollY)
        if (vertical) scrolled(scrollY)
    }

    override fun onScrollX(scrollX: Int) {
        super.onScrollX(scrollX)
        if (!vertical) scrolled(scrollX)
    }

    /// Composes what scrolled into view before it is painted: a quarter of a view
    /// of travel since the last pass.
    private fun scrolled(at: Int) {
        if (measuring || windowAt == Int.MIN_VALUE) return
        if (Math.abs(at - windowAt) * 4 >= viewport) {
            remeasureInPlace()
        }
    }

    private fun captureAnchor() {
        val at = if (vertical) scrollY else scrollX
        val i = firstAt(at)
        if (i < 0) {
            anchorKey = null
            anchorIndex = -1
            return
        }
        anchorKey = keys[i]
        anchorIndex = i
        anchorOffset = at - startOf(i)
    }

    internal fun cn1FirstVisible(): Int = Math.max(0, firstAt(if (vertical) scrollY else scrollX))

    /// Scrolls so that `index` starts `offset` into the view. The item is composed and measured
    /// first, so the position is its real one and not the estimate it had while out of view.
    internal fun cn1ScrollToItem(index: Int, offset: Int) {
        // A pass composes around where the estimate put the item; measuring its neighbours can
        // move it, or the end of the list, so repeat until the position holds. Each pass leaves
        // fewer estimates behind it, and two are the usual case.
        for (pass in 0 until 4) {
            if (index !in keys.indices) return
            val t = Math.max(0, Math.min(startOf(index) + offset, total - viewport))
            anchorKey = keys[index]
            anchorIndex = index
            anchorOffset = offset
            anchorFixed = true
            if ((if (vertical) scrollY else scrollX) == t && pass > 0) return
            if (vertical) scrollY = t else scrollX = t
            measuring = true
            try {
                remeasureInPlace()
            } finally {
                measuring = false
            }
        }
    }

    /// How many items the list has, and how many of them are composed.
    fun cn1ItemCount(): Int = items?.count ?: 0

    fun cn1ComposedCount(): Int = slots.size

    /// Disposes every item composition; the list is leaving the composition.
    internal fun cn1Dispose() {
        disposeAll()
    }
}
