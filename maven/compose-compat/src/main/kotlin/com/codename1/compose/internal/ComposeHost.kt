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

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import com.codename1.ui.Component
import com.codename1.ui.Container
import com.codename1.ui.geom.Dimension
import com.codename1.ui.layouts.Layout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/// Inserts, moves and removes the components a composition emits.
internal class NodeApplier(root: Container) : AbstractApplier<Component>(root) {
    override fun insertTopDown(index: Int, instance: Component) {
        // Children are attached bottom up, so a parent is complete when it is added.
    }

    override fun insertBottomUp(index: Int, instance: Component) {
        val parent = current as? Container ?: return
        if (index >= parent.componentCount) {
            parent.addComponent(instance)
        } else {
            parent.addComponent(index, instance)
        }
        invalidate(parent)
    }

    override fun remove(index: Int, count: Int) {
        val parent = current as? Container ?: return
        for (i in 0 until count) {
            parent.removeComponent(parent.getComponentAt(index))
        }
        invalidate(parent)
    }

    override fun move(from: Int, to: Int, count: Int) {
        val parent = current as? Container ?: return
        val moved = ArrayList<Component>(count)
        for (i in 0 until count) {
            moved.add(parent.getComponentAt(from))
            parent.removeComponent(parent.getComponentAt(from))
        }
        val dest = if (from > to) to else to - count
        for (i in 0 until count) {
            parent.addComponent(dest + i, moved[i])
        }
        invalidate(parent)
    }

    override fun onClear() {
        (root as? Container)?.removeAll()
    }

    override fun onEndChanges() {
        val r = root
        if (r is ComposeHost) {
            r.requestRemeasure()
        }
    }

    private fun invalidate(c: Container) {
        if (c is UiNode) {
            c.invalidateMeasure()
        }
    }
}

/// The Codename One component a composition lives in. Its children are the
/// composition's top level nodes, stacked as a `Box` would stack them.
internal class ComposeHost : Container(HostLayout) {
    private val clock = BroadcastFrameClock { scheduleFrame() }
    // Immediate, so a frame resumes its awaiters inside sendFrame. Dispatched,
    // every resumption waited for the end of the EDT cycle, and so did the
    // state it wrote: a change reached the screen three cycles after it was
    // made. Now the recomposer applies what a frame callback writes in that
    // same frame (see scheduleFrame for when the frame runs).
    private val scope = CoroutineScope(EdtDispatcher.Immediate + clock + Job())
    private val recomposer = Recomposer(scope.coroutineContext)
    private var composition: Composition? = null
    private var inFrame = false
    private var sentInLayout = false
    private var animatedOn: com.codename1.ui.Form? = null
    private var frameRequested = false
    private var timerPending = false

    init {
        cn1StripStyle(this)
        GlobalSnapshotManager.ensureStarted()
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
        }
    }

    fun setContent(content: @Composable () -> Unit) {
        val c = composition ?: Composition(NodeApplier(this), recomposer).also { composition = it }
        c.setContent(content)
        requestRemeasure()
    }

    fun dispose() {
        composition?.dispose()
        composition = null
        recomposer.cancel()
        scope.cancel()
        animatedOn?.deregisterAnimated(this)
        animatedOn = null
    }

    /// Whether a composition change, a state change or a frame is pending.
    fun cn1HasPendingWork(): Boolean = recomposer.hasPendingWork || clock.hasAwaiters

    // --- frames -------------------------------------------------------------

    /// A frame runs at the start of the next EDT cycle's layout pass, as
    /// Android's Choreographer runs a frame's callbacks before traversal: the
    /// frame, recomposition, layout and paint then share one cycle. Sent from
    /// `animate()`, which runs after the cycle has painted, a change waited a
    /// whole cycle more. The cycle's own wait paces continuous animation:
    /// `revalidateLater` does not wake the EDT. `animate()`, after the paint,
    /// is the fallback for a cycle whose layout pass did not reach this host
    /// (the form defers revalidation while it animates); the timer is for a
    /// host that is not showing. A timer per frame cost a thread per frame.
    private fun scheduleFrame() {
        frameRequested = true
        val f = componentForm
        if (f != null && f.isVisible) {
            if (animatedOn !== f) {
                animatedOn?.deregisterAnimated(this)
                f.registerAnimated(this)
                animatedOn = f
            }
            if (!inFrame) {
                revalidateLater()
            }
            return
        }
        if (!timerPending) {
            timerPending = true
            com.codename1.ui.CN.setTimeout(16) {
                timerPending = false
                sendFrame()
            }
        }
    }

    internal fun sendFrame() {
        if (frameRequested) {
            frameRequested = false
            clock.sendFrame(System.nanoTime())
        }
    }


    override fun animate(): Boolean {
        if (sentInLayout) {
            sentInLayout = false
        } else {
            sendFrame()
        }
        return false
    }

    override fun deinitialize() {
        animatedOn?.deregisterAnimated(this)
        animatedOn = null
        super.deinitialize()
    }

    override fun paint(g: com.codename1.ui.Graphics) {
        super.paint(g)
    }

    override fun initComponent() {
        super.initComponent()
        if (frameRequested) {
            scheduleFrame()
        }
    }

    // --- measuring ----------------------------------------------------------

    private var remeasurePending = false

    fun requestRemeasure() {
        if (inFrame) {
            // This pass measures what the frame applies.
            return
        }
        if (!remeasurePending) {
            remeasurePending = true
            setShouldCalcPreferredSize(true)
            revalidateLater()
            repaint()
        }
    }


    /// The status bar and home indicator this host lies under, in pixels, as
    /// of its last pass; read by its nodes' [SafeInsetsElement]s.
    internal var insetTop = 0
    internal var insetBottom = 0
    private var insetsKnown = false

    internal fun layoutChildren() {
        if (frameRequested) {
            // What the frame applies is measured by this pass, so it must not
            // queue another; a frame asked for during it goes to the next cycle.
            inFrame = true
            try {
                sendFrame()
            } finally {
                inFrame = false
            }
            sentInLayout = true
            if (frameRequested) {
                revalidateLater()
            }
        }
        remeasurePending = false
        val insets = SafeInsets.of(absoluteY, height)
        val top = (insets ushr 32).toInt()
        val bottom = insets.toInt()
        val changed = !insetsKnown || top != insetTop || bottom != insetBottom
        insetTop = top
        insetBottom = bottom
        insetsKnown = true
        if (changed) {
            // A node's measure is cached by constraints, and a safe area that
            // arrives late (or a rotation) changes no constraint: forget the
            // measures of the nodes that pad by it. Forgetting every node's
            // rebuilt the lazy lists' items and cost a megabyte at start-up.
            // This pass measures them; the nodes must not ask for another.
            remeasurePending = true
            forgetMeasures(this)
            remeasurePending = false
        }
        val c = Constraints(0, width, 0, height)
        for (k in 0 until componentCount) {
            val child = getComponentAt(k)
            val s = measureChild(child, c)
            child.setX(if (child is UiNode) child.offsetX else 0)
            child.setY(if (child is UiNode) child.offsetY else 0)
            child.setWidth(s.w())
            child.setHeight(s.h())
            if (child is UiNode) {
                child.applyPlacements()
            }
        }
    }

    private fun forgetMeasures(c: Container) {
        for (k in 0 until c.componentCount) {
            val child = c.getComponentAt(k)
            if (child is UiNode && child.usesSafeInsets) {
                child.invalidateMeasure()
            }
            if (child is Container) {
                forgetMeasures(child)
            }
        }
    }

    internal fun preferred(): Dimension {
        var w = 0
        var h = 0
        val insets = SafeInsets.of(absoluteY, cn1DisplayHeight())
        insetTop = (insets ushr 32).toInt()
        insetBottom = insets.toInt()
        // Measured with a guessed height: the next pass measures again.
        insetsKnown = false
        val c = Constraints(0, cn1DisplayWidth(), 0, Constraints.INF)
        for (k in 0 until componentCount) {
            val s = measureChild(getComponentAt(k), c)
            w = Math.max(w, s.w())
            h = Math.max(h, s.h())
        }
        return Dimension(w, h)
    }
}

internal object HostLayout : Layout() {
    override fun layoutContainer(parent: Container) {
        // Checked, never cast: a failed cast does not throw on iOS.
        (parent as? ComposeHost)?.layoutChildren()
    }

    override fun getPreferredSize(parent: Container): Dimension =
        (parent as? ComposeHost)?.preferred() ?: Dimension(0, 0)
}
