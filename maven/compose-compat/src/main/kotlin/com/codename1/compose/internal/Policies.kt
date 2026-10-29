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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntSize
import com.codename1.ui.Component

private fun Component.weightOf(): WeightElement? = (this as? UiNode)?.weight
private fun Component.alignOf(): AlignSelfElement? = (this as? UiNode)?.alignSelf

/// Stacks children, each aligned in the box.
internal class BoxPolicy(val alignment: Alignment, val propagateMinConstraints: Boolean) : MeasurePolicy {
    override fun measure(node: UiNode, children: List<Component>, constraints: Constraints): Long {
        val childC = if (propagateMinConstraints) constraints else constraints.loose()
        var w = constraints.minWidth
        var h = constraints.minHeight
        val sizes = LongArray(children.size)
        // Children that match the parent's size are measured once the others have
        // decided it.
        for (i in children.indices) {
            val c = children[i]
            if ((c as? UiNode)?.matchParentSize == true) continue
            val s = measureChild(c, childC)
            sizes[i] = s
            w = Math.max(w, s.w())
            h = Math.max(h, s.h())
        }
        w = constraints.constrainWidth(w)
        h = constraints.constrainHeight(h)
        for (i in children.indices) {
            val c = children[i]
            if ((c as? UiNode)?.matchParentSize == true) {
                sizes[i] = measureChild(c, Constraints.fixed(w, h))
            }
            val s = sizes[i]
            val a = c.alignOf()?.box ?: alignment
            val o = a.align(IntSize(s.w(), s.h()), IntSize(w, h), LTR)
            node.place(i, o.x, o.y, s.w(), s.h())
        }
        return size(w, h)
    }

    override fun equals(other: Any?): Boolean =
        other is BoxPolicy && other.alignment == alignment && other.propagateMinConstraints == propagateMinConstraints

    override fun hashCode(): Int = alignment.hashCode() * 2 + if (propagateMinConstraints) 1 else 0

    companion object {
        val TopStart: BoxPolicy = BoxPolicy(Alignment.TopStart, false)
    }
}

/// Row and Column: children along the main axis, with weights and an
/// arrangement, aligned on the cross axis.
internal class LinearPolicy(
    val vertical: Boolean,
    val horizontalArrangement: Arrangement.Horizontal?,
    val verticalArrangement: Arrangement.Vertical?,
    val horizontalAlignment: Alignment.Horizontal?,
    val verticalAlignment: Alignment.Vertical?
) : MeasurePolicy {
    override fun measure(node: UiNode, children: List<Component>, constraints: Constraints): Long {
        val d = Density
        val spacing = if (vertical) d.px(verticalArrangement!!.spacing) else d.px(horizontalArrangement!!.spacing)
        val mainMax = if (vertical) constraints.maxHeight else constraints.maxWidth
        val mainMin = if (vertical) constraints.minHeight else constraints.minWidth
        val crossMax = if (vertical) constraints.maxWidth else constraints.maxHeight
        val crossMin = if (vertical) constraints.minWidth else constraints.minHeight
        val n = children.size
        val mains = IntArray(n)
        val crosses = IntArray(n)
        var used = if (n > 1) spacing * (n - 1) else 0
        var totalWeight = 0f
        var cross = 0
        for (i in 0 until n) {
            val c = children[i]
            val wt = c.weightOf()
            if (wt != null && wt.weight > 0f) {
                totalWeight += wt.weight
                continue
            }
            val remaining = if (mainMax == Constraints.INF) Constraints.INF else Math.max(0, mainMax - used)
            val cc = if (vertical) Constraints(0, crossMax, 0, remaining) else Constraints(0, remaining, 0, crossMax)
            val s = measureChild(c, cc)
            mains[i] = if (vertical) s.h() else s.w()
            crosses[i] = if (vertical) s.w() else s.h()
            used += mains[i]
            cross = Math.max(cross, crosses[i])
        }
        if (totalWeight > 0f) {
            val space = if (mainMax == Constraints.INF) 0 else Math.max(0, mainMax - used)
            var left = space
            var lastWeighted = -1
            for (i in 0 until n) {
                val wt = children[i].weightOf()
                if (wt != null && wt.weight > 0f) lastWeighted = i
            }
            for (i in 0 until n) {
                val c = children[i]
                val wt = c.weightOf() ?: continue
                if (wt.weight <= 0f) continue
                val share = if (i == lastWeighted) left else Math.round(space * wt.weight / totalWeight)
                left -= share
                val min = if (wt.fill) share else 0
                val cc = if (vertical) Constraints(0, crossMax, min, share) else Constraints(min, share, 0, crossMax)
                val s = measureChild(c, cc)
                mains[i] = if (vertical) s.h() else s.w()
                crosses[i] = if (vertical) s.w() else s.h()
                used += mains[i]
                cross = Math.max(cross, crosses[i])
            }
        }
        val mainSize = Math.max(mainMin, if (totalWeight > 0f && mainMax != Constraints.INF) mainMax else
            Math.min(used, mainMax))
        val crossSize = Math.max(crossMin, Math.min(cross, crossMax))
        // Arrange along the main axis.
        val positions = IntArray(n)
        if (vertical) {
            verticalArrangement!!.arrange(mainSize, mains, positions)
        } else {
            horizontalArrangement!!.arrange(mainSize, mains, LTR, positions)
        }
        for (i in 0 until n) {
            val c = children[i]
            val a = c.alignOf()
            val crossPos = if (vertical) {
                (a?.horizontal ?: horizontalAlignment!!).align(crosses[i], crossSize, LTR)
            } else {
                (a?.vertical ?: verticalAlignment!!).align(crosses[i], crossSize)
            }
            if (vertical) {
                node.place(i, crossPos, positions[i], crosses[i], mains[i])
            } else {
                node.place(i, positions[i], crossPos, mains[i], crosses[i])
            }
        }
        return if (vertical) size(crossSize, mainSize) else size(mainSize, crossSize)
    }

    override fun equals(other: Any?): Boolean = other is LinearPolicy && other.vertical == vertical &&
        other.horizontalArrangement == horizontalArrangement && other.verticalArrangement == verticalArrangement &&
        other.horizontalAlignment == horizontalAlignment && other.verticalAlignment == verticalAlignment

    override fun hashCode(): Int = (if (vertical) 1 else 0) + 31 * (horizontalArrangement.hashCode() +
        31 * (verticalArrangement.hashCode() + 31 * (horizontalAlignment.hashCode() + 31 * verticalAlignment.hashCode())))
}

/// A node with no children whose content measures itself (text, an icon, a
/// canvas).
internal object LeafPolicy : MeasurePolicy {
    override fun measure(node: UiNode, children: List<Component>, constraints: Constraints): Long =
        size(constraints.minWidth, constraints.minHeight)
}
