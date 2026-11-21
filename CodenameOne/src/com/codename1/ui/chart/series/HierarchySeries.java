/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.ui.chart.series;

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.chart.ChartContext;
import com.codename1.ui.chart.ChartPoint;
import com.codename1.ui.chart.Series;
import com.codename1.ui.chart.TreeNode;
import com.codename1.ui.chart.layout.LabelLayout;

import java.util.ArrayList;

/// Base of the hierarchical series. It keeps the tree, the node currently zoomed into and the
/// geometry of the last paint for hit testing, and handles drill down: tapping a node with
/// children zooms into it, tapping the breadcrumb goes back up.
public abstract class HierarchySeries extends Series {
    TreeNode root;
    TreeNode current;
    private boolean drillDown = true;
    private int maxDepth = 2;
    /// Nodes painted in the last frame, parallel to `cells`.
    final ArrayList<TreeNode> nodes = new ArrayList<TreeNode>();
    final ArrayList<float[]> cells = new ArrayList<float[]>();
    int[] crumb;
    // what crumb points at while the breadcrumb is shown, reused by every paint
    final int[] crumbBuf = new int[4];
    private boolean entering;
    /// Bumped by every change to the tree, so a cached layout knows it is stale.
    int treeVersion;
    // recorded geometries, reused from one paint to the next once nodes and cells are cleared
    private final ArrayList<float[]> cellPool = new ArrayList<float[]>();
    // the breadcrumb text, rebuilt only when the node, the width, the font or the tree changes
    private String crumbText;
    private TreeNode crumbNode;
    private Font crumbFont;
    private int crumbWidth;
    private int crumbVersion;

    /// Creates the series.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    ///
    /// - `root`: the tree's root
    protected HierarchySeries(String name, TreeNode root) {
        super(name);
        setRoot(root);
    }

    @Override
    public boolean isCartesian() {
        return false;
    }

    /// Replaces the tree and zooms back out to its root.
    ///
    /// #### Parameters
    ///
    /// - `root`: the new root
    ///
    /// #### Returns
    ///
    /// this series
    public HierarchySeries setRoot(TreeNode root) {
        if (getChart() != null) {
            getChart().beginChange();
        }
        if (attached && this.root != null) {
            this.root.removeChangeListener(treeListener);
        }
        this.root = root;
        this.current = root;
        if (attached && root != null) {
            root.addChangeListener(treeListener);
        }
        treeVersion++;
        dataChanged();
        return this;
    }

    // the tree is listened to only while this series is in a chart that is not off screen, so
    // a tree that outlives the chart does not keep the chart (and this series) reachable
    private boolean attached;

    @Override
    protected void attached() {
        if (attached) {
            return;
        }
        attached = true;
        // edits made while detached fired no change: cached layouts must not survive them
        treeVersion++;
        if (root != null) {
            root.addChangeListener(treeListener);
            // edits made while detached were not seen: a drilled-in node may have left the tree
            if (!isUnderRoot(current)) {
                current = root;
            }
        }
    }

    @Override
    protected void detached() {
        if (!attached) {
            return;
        }
        attached = false;
        if (root != null) {
            root.removeChangeListener(treeListener);
        }
    }

    // a change to a node of the tree animates like any other data change
    private final TreeNode.ChangeListener treeListener = new TreeNode.ChangeListener() {
        @Override
        public void treeChanging(TreeNode node) {
            if (getChart() != null) {
                getChart().beginChange();
            }
        }

        @Override
        public void treeChanged(TreeNode node) {
            // a node zoomed into that was moved out of the tree can no longer be painted or
            // drilled out of: fall back to the root
            if (!isUnderRoot(current)) {
                current = root;
            }
            treeVersion++;
            dataChanged();
        }
    };

    /// The root node.
    public TreeNode getRoot() {
        return root;
    }

    /// The node currently zoomed into.
    public TreeNode getCurrent() {
        // a series out of a chart does not listen to its tree, so a node zoomed into may have
        // been moved out since: checked here, and by every paint, rather than by a listener
        if (current != null && !isUnderRoot(current)) {
            current = root;
        }
        return current;
    }

    /// Zooms into a node, animated.
    ///
    /// #### Parameters
    ///
    /// - `node`: a node of this tree, or null for the root
    ///
    /// #### Returns
    ///
    /// this series
    public HierarchySeries setCurrent(TreeNode node) {
        if (getChart() != null) {
            getChart().beginChange();
        }
        // a node outside this tree (or above a root that is itself a subtree) would paint and drill
        // through nodes the chart does not own, so it falls back to the root
        current = isUnderRoot(node) ? node : root;
        entering = true;
        dataChanged();
        return this;
    }

    private boolean isUnderRoot(TreeNode node) {
        for (TreeNode n = node; n != null; n = n.getParent()) {
            if (n == root) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
                return true;
            }
        }
        return false;
    }

    /// True when a point, in screen coordinates, is on the breadcrumb that goes back up.
    ///
    /// #### Parameters
    ///
    /// - `ax`: the x coordinate
    ///
    /// - `ay`: the y coordinate
    ///
    /// #### Returns
    ///
    /// true on the breadcrumb
    protected boolean crumbContains(float ax, float ay) {
        return crumb != null && ax >= crumb[0] && ax <= crumb[0] + crumb[2] && ay >= crumb[1] && ay <= crumb[1] + crumb[3];
    }

    /// Enables zooming into a node by tapping it.
    ///
    /// #### Parameters
    ///
    /// - `d`: true to allow drill down
    ///
    /// #### Returns
    ///
    /// this series
    public HierarchySeries setDrillDown(boolean d) {
        drillDown = d;
        return this;
    }

    /// How many levels below the current node are drawn.
    ///
    /// #### Parameters
    ///
    /// - `depth`: 1 or more
    ///
    /// #### Returns
    ///
    /// this series
    public HierarchySeries setMaxDepth(int depth) {
        maxDepth = Math.max(1, depth);
        repaintChart();
        return this;
    }

    /// The number of levels drawn.
    public int getMaxDepth() {
        return maxDepth;
    }

    @Override
    protected void captureEntryStart() {
        entering = true;
    }

    // where each node was drawn when the data started to change, so the change animates
    private java.util.HashMap<TreeNode, float[]> fromCells;

    @Override
    protected void captureTransitionStart() {
        if (nodes.isEmpty()) {
            fromCells = null;
            return;
        }
        java.util.HashMap<TreeNode, float[]> m = new java.util.HashMap<TreeNode, float[]>();
        for (int i = 0; i < nodes.size(); i++) {
            float[] c = cells.get(i);
            float[] copy = new float[c.length];
            System.arraycopy(c, 0, copy, 0, c.length);
            m.put(nodes.get(i), copy);
        }
        fromCells = m;
    }

    @Override
    protected void transitionEnded() {
        fromCells = null;
    }

    /// Moves a node's geometry, in place, part of the way from where the node was drawn before
    /// the data changed to `geometry`, by the progress of the change. A node the change added
    /// starts from `collapsed(geometry)`. Does nothing when no change is animating.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: the paint context
    ///
    /// - `n`: the node
    ///
    /// - `geometry`: the node's geometry after the change, replaced by this frame's
    protected final void tween(ChartContext ctx, TreeNode n, float[] geometry) {
        if (fromCells == null) {
            return;
        }
        float p = ctx.getProgress();
        if (p >= 1) {
            return;
        }
        float[] from = fromCells.get(n);
        if (from == null || from.length != geometry.length) {
            from = collapsed(geometry);
        }
        for (int i = 0; i < geometry.length; i++) {
            geometry[i] = from[i] + (geometry[i] - from[i]) * p;
        }
    }

    /// The geometry a node added by a data change grows from: by default the geometry itself,
    /// so the node appears in place.
    ///
    /// #### Parameters
    ///
    /// - `geometry`: the node's geometry after the change
    ///
    /// #### Returns
    ///
    /// a new array of the same length
    protected float[] collapsed(float[] geometry) {
        float[] r = new float[geometry.length];
        System.arraycopy(geometry, 0, r, 0, geometry.length);
        return r;
    }

    /// The fraction of the zoom or entry animation done, 1 when idle.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: the paint context
    ///
    /// #### Returns
    ///
    /// 0 to 1
    protected float growth(ChartContext ctx) {
        if (!entering) {
            return 1;
        }
        if (ctx.getProgress() >= 1) {
            entering = false;
            return 1;
        }
        return ctx.getProgress();
    }

    /// The colour of a node: its own, its nearest coloured ancestor's tinted by depth, or the
    /// palette colour of its top level branch.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: the paint context
    ///
    /// - `n`: the node
    ///
    /// #### Returns
    ///
    /// an RGB colour
    protected int colorOf(ChartContext ctx, TreeNode n) {
        if (n.getColor() != null) {
            return n.getColor().intValue();
        }
        TreeNode branch = n;
        int depth = 0;
        while (branch.getParent() != null && branch.getParent() != current && branch.getParent() != root) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
            if (branch.getColor() != null) {
                break;
            }
            branch = branch.getParent();
            depth++;
        }
        int base;
        if (branch.getColor() != null) {
            base = branch.getColor().intValue();
        } else {
            // the branch's own cached position, not a search of its parent's children
            base = ctx.getTheme().getSeriesColor(branch.getIndexInParent());
        }
        return ctx.getTheme().tint(base, depth * 0.22f);
    }

    /// Paints the breadcrumb row when zoomed in and returns its height.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: the paint context
    ///
    /// #### Returns
    ///
    /// the height used, 0 at the root
    protected int paintBreadcrumb(ChartContext ctx) {
        crumb = null;
        if (current == root || current == null) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
            return 0;
        }
        Graphics g = ctx.getGraphics();
        Font f = ctx.getFont();
        int fh = f.getHeight();
        if (crumbText == null || crumbNode != current || crumbFont != f //NOPMD CompareObjectsWithEquals - identity is the cache key
                || crumbWidth != ctx.getPlotWidth() || crumbVersion != treeVersion) {
            crumbText = LabelLayout.ellipsize(f, "< " + current.getPath(), ctx.getPlotWidth());
            crumbNode = current;
            crumbFont = f;
            crumbWidth = ctx.getPlotWidth();
            crumbVersion = treeVersion;
        }
        String text = crumbText;
        g.setColor(ctx.getTheme().getSecondaryTextColor());
        g.setFont(f);
        g.drawString(text, ctx.getPlotX(), ctx.getPlotY());
        crumb = crumbBuf;
        crumb[0] = ctx.getPlotX();
        crumb[1] = ctx.getPlotY();
        crumb[2] = f.stringWidth(text);
        crumb[3] = fh;
        ctx.getLabels().reserve(crumb[0], crumb[1], crumb[2], crumb[3]);
        return fh + ctx.mm(1);
    }

    /// Records a painted node for hit testing; later records win.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    ///
    /// - `geometry`: the shape's geometry, interpreted by the subclass
    // a geometry array for the next record call: the one the same slot held last paint when it
    // has the length, so painting an unchanged tree allocates nothing
    final float[] nextCell(int len) {
        int k = cells.size();
        if (k < cellPool.size()) {
            float[] c = cellPool.get(k);
            if (c.length == len) {
                return c;
            }
            c = new float[len];
            cellPool.set(k, c);
            return c;
        }
        float[] c = new float[len];
        cellPool.add(c);
        return c;
    }

    protected void record(TreeNode n, float[] geometry) {
        nodes.add(n);
        cells.add(geometry);
    }

    /// True if the absolute point lies in a recorded geometry.
    ///
    /// #### Parameters
    ///
    /// - `geometry`: a recorded geometry
    ///
    /// - `x`: absolute x
    ///
    /// - `y`: absolute y
    ///
    /// #### Returns
    ///
    /// true on a hit
    protected abstract boolean contains(float[] geometry, float x, float y);

    /// The anchor point of a recorded geometry, `{x, y}` absolute.
    ///
    /// #### Parameters
    ///
    /// - `geometry`: a recorded geometry
    ///
    /// #### Returns
    ///
    /// the point a tooltip points at
    protected abstract float[] anchor(float[] geometry);

    @Override
    public ChartPoint hitTest(ChartContext ctx, int x, int y, int radius) {
        float ax = x + ctx.getOriginX();
        float ay = y + ctx.getOriginY();
        if (crumbContains(ax, ay)) {
            return new ChartPoint(this, -1, 0, current.getValue(), x, y, current.getParent() == null ? null : current.getParent().getLabel());
        }
        for (int i = nodes.size() - 1; i >= 0; i--) {
            if (contains(cells.get(i), ax, ay)) {
                return getPoint(ctx, i);
            }
        }
        return null;
    }

    @Override
    protected boolean isPointCountFromLayout() {
        return true;
    }

    @Override
    public int getPointCount() {
        return nodes.size();
    }

    @Override
    public ChartPoint getPoint(ChartContext ctx, int index) {
        if (index < 0 || index >= nodes.size()) {
            return null;
        }
        TreeNode n = nodes.get(index);
        float[] a = anchor(cells.get(index));
        return new ChartPoint(this, index, index, n.getValue(), Math.round(a[0]) - ctx.getOriginX(),
                Math.round(a[1]) - ctx.getOriginY(), n.getLabel());
    }

    /// The node behind a point from `hitTest` or a selection event.
    ///
    /// #### Parameters
    ///
    /// - `p`: the point
    ///
    /// #### Returns
    ///
    /// the node, or null
    public TreeNode getNode(ChartPoint p) {
        int i = p.getIndex();
        return i >= 0 && i < nodes.size() ? nodes.get(i) : null;
    }

    @Override
    public boolean handleTap(ChartPoint p) {
        if (!drillDown) {
            return false;
        }
        if (p.getIndex() < 0) {
            setCurrent(current == root ? root : current.getParent()); //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
            return true;
        }
        TreeNode n = getNode(p);
        if (n == null) {
            return false;
        }
        // zoom into the tapped node's top level branch under the current node
        TreeNode target = n;
        while (target.getParent() != null && target.getParent() != current && target != root) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
            target = target.getParent();
        }
        if (target.getChildCount() > 0 && target != current) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
            setCurrent(target);
            return true;
        }
        return false;
    }

    @Override
    public String formatTooltip(ChartContext ctx, ChartPoint p) {
        if (p.getIndex() < 0) {
            return p.getLabel() == null ? "" : "< " + p.getLabel();
        }
        TreeNode n = getNode(p);
        if (n == null) {
            return "";
        }
        String pct = "";
        if (n.getParent() != null && n.getParent().getValue() > 0) {
            pct = " (" + com.codename1.ui.chart.axis.Axis.formatNumber(n.getValue() * 100 / n.getParent().getValue(), 1) + "%)";
        }
        return n.getLabel() + ": " + formatValue(ctx, n.getValue()) + pct;
    }

    @Override
    public void collectTableRows(java.util.List<String[]> rows) {
        if (root != null) {
            collect(root, rows);
        }
    }

    private void collect(TreeNode n, java.util.List<String[]> rows) {
        for (int i = 0; i < n.getChildCount(); i++) {
            TreeNode c = n.getChild(i);
            rows.add(new String[]{getName(), c.getPath(), formatValue(null, c.getValue())});
            collect(c, rows);
        }
    }
}
