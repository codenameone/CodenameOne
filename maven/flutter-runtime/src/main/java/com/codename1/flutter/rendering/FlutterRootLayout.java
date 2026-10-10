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
package com.codename1.flutter.rendering;

import com.codename1.flutter.RenderElement;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;

/**
 * The CN1 layout installed on the single flat container hosting a Flutter
 * subtree. CN1 sees one container whose children are the subtree's leaf
 * components (Labels, buttons ...); this layout runs the Flutter constraint
 * pass on the render-element tree and writes the resulting absolute bounds
 * onto those components.
 */
public class FlutterRootLayout extends Layout {

    private final RenderHost host;

    public FlutterRootLayout(RenderHost host) {
        this.host = host;
    }

    public RenderHost host() {
        return host;
    }

    /// How many times a root has been laid out, and what that cost.
    ///
    /// Read the PROFILE, not the count. Start-up runs 47 passes, which looks
    /// alarming until the split shows one pass of ~549ms and 46 of ~1ms: the
    /// count is a red herring and the cost is a single full layout, which is
    /// where the transpiled widget tree is actually constructed (LayoutBuilder
    /// builds during layout, as Flutter's does).
    private static int rootPasses;
    private static long rootMs;
    private static long rootFirstMs = -1;
    private static long rootWorstMs;
    private static int rootDepth;
    private static int rootMaxDepth;

    /// The box the first few passes were given, in device pixels.
    ///
    /// A pass that runs against the WRONG box is not merely wasted: LayoutBuilder
    /// builds during layout, so the whole widget tree is constructed against
    /// that box — and an adaptive app asks the box which layout it is, so a
    /// provisional size builds the wrong application.
    private static final StringBuilder rootBoxes = new StringBuilder();
    private static int rootBoxesRecorded;

    private static void noteBox(BoxConstraints c) {
        if (rootBoxesRecorded >= 6 || c == null) {
            return;
        }
        rootBoxesRecorded++;
        if (rootBoxes.length() > 0) {
            rootBoxes.append(' ');
        }
        rootBoxes.append((int) c.maxWidth()).append('x').append((int) c.maxHeight());
    }

    /** Root layout passes so far, and their cost profile. */
    public static String rootLayoutCost() {
        return rootPasses + " root pass(es) in " + rootMs + "ms (first=" + rootFirstMs
                + "ms worst=" + rootWorstMs + "ms maxNesting=" + rootMaxDepth
                + " boxes=" + rootBoxes + ")";
    }

    /**
     * Whether a root layout pass is running right now, anywhere in the app.
     *
     * <p>Layout is not re-entrant. While a pass is walking the tree, elements
     * are being built and mounted underneath it (LayoutBuilder builds during
     * layout, as Flutter's does), so anything that asks for a FRESH pass at
     * that moment walks a tree that is half-replaced: ancestors are already
     * detached from the elements still being laid out, and every
     * {@code .of(context)} lookup made from inside the new pass answers
     * "nothing here". The gallery's splash crashed exactly this way on the
     * native build -- a PositionedTransition ticked while the first frame was
     * laying out, forced a second pass from inside the first, and the backdrop
     * then failed a {@code GalleryOptions.of(context)!} whose provider was
     * four levels above a parent pointer that had already been cleared.</p>
     *
     * <p>Requests that arrive during a pass are recorded and run once the
     * outermost pass finishes, so nothing is silently dropped.</p>
     */
    public static boolean inLayout() {
        return rootDepth > 0;
    }

    private static final java.util.List<RenderHost> PENDING =
            new java.util.ArrayList<RenderHost>();

    /** Records a relayout request that arrived while a pass was in progress. */
    static void deferRevalidate(RenderHost h) {
        if (h != null && !PENDING.contains(h)) {
            PENDING.add(h);
        }
    }

    private static void runDeferred() {
        if (PENDING.isEmpty()) {
            return;
        }
        // Bounded: a deferred pass can itself defer, and without a ceiling two
        // hosts that invalidate each other would spin here forever.
        for (int round = 0; round < 4 && !PENDING.isEmpty(); round++) {
            java.util.List<RenderHost> due = new java.util.ArrayList<RenderHost>(PENDING);
            PENDING.clear();
            for (int i = 0; i < due.size(); i++) {
                due.get(i).revalidate();
            }
        }
        PENDING.clear();
    }

    @Override
    public void layoutContainer(Container parent) {
        RenderElement root = host.rootRenderElement();
        if (root == null) {
            return;
        }
        long t0 = System.currentTimeMillis();
        rootPasses++;
        rootDepth++;
        rootMaxDepth = Math.max(rootMaxDepth, rootDepth);
        try {
            layoutRoot(parent, root);
        } finally {
            long took = System.currentTimeMillis() - t0;
            // Only top-level passes are added up: a nested pass is already
            // inside its parent's elapsed time, and counting both makes the
            // total look like multiples of the work actually done.
            if (rootDepth == 1) {
                rootMs += took;
            }
            if (rootFirstMs < 0) {
                rootFirstMs = took;
            }
            rootWorstMs = Math.max(rootWorstMs, took);
            rootDepth--;
        }
        if (rootDepth == 0) {
            runDeferred();
        }
    }

    /// How many times a single pass may re-run because it invalidated itself. Two extra
    /// attempts is enough for the case this exists for -- one subtree that builds late,
    /// and its parents remeasured once around it -- and a bound means a widget that
    /// dirties itself unconditionally degrades to a stale frame rather than a hang.
    private static final int SETTLE_ATTEMPTS = 3;

    private void layoutRoot(Container parent, RenderElement root) {
        Style s = parent.getStyle();
        BoxConstraints box = constraintsFor(parent);
        noteBox(box);
        // A pass can invalidate itself. A LayoutBuilder sits out a speculative measurement
        // and inflates its subtree on a later one, underneath the performLayout of the box
        // that contains it -- so by the time this pass finishes, the offsets it stored are
        // for a set of children that has since changed. Running once and trusting the
        // result left Reply's mail list with every card at the list's origin and a pane of
        // zero size, which reads on screen as an empty page, and no later pass repaired it
        // because they all hit the same clean cache.
        //
        // So: run, and if the tree says it is still dirty, run again with what it now
        // knows. This settles on the second pass in practice.
        for (int attempt = 0; attempt < SETTLE_ATTEMPTS; attempt++) {
            root.layout(box);
            root.position(s.getPaddingLeftNoRTL(), s.getPaddingTop());
            if (!root.needsLayout()) {
                return;
            }
        }
    }

    /**
     * The constraints this pass hands the subtree. By default the pane's own box, which
     * is right for a top-level host: CN1 owns that container's size.
     *
     * <p>A nested host (an effect's pane) overrides this, because there the Flutter pass
     * already decided the subtree's constraints and the pane's component size may not
     * reflect them yet.</p>
     */
    protected BoxConstraints constraintsFor(Container parent) {
        Style s = parent.getStyle();
        int width = parent.getLayoutWidth() - parent.getSideGap() - s.getHorizontalPadding();
        int height = parent.getLayoutHeight() - parent.getBottomGap() - s.getVerticalPadding();
        if (width < 0) {
            width = 0;
        }
        if (height < 0) {
            height = 0;
        }
        return BoxConstraints.tight(width, height);
    }

    @Override
    public Dimension getPreferredSize(Container parent) {
        RenderElement root = host.rootRenderElement();
        if (root == null) {
            return new Dimension(0, 0);
        }
        // Dry pass with loose unbounded constraints. It goes through dryLayout, which keeps
        // its own cache slot: these constraints differ from layoutContainer's tight ones, so
        // sharing one slot made the two passes evict each other on every box in the tree.
        Size sz = root.dryLayout(BoxConstraints.loose(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        Style s = parent.getStyle();
        int w = (int) Math.ceil(sz.width()) + s.getHorizontalPadding();
        int h = (int) Math.ceil(sz.height()) + s.getVerticalPadding();
        return new Dimension(w, h);
    }

    @Override
    public boolean isOverlapSupported() {
        return true;
    }
}
