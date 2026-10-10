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
package com.codename1.desktopcompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.LayoutManager;
import com.codename1.desktopcompat.java.awt.LayoutManager2;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.border.EmptyBorder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// Lays the same component tree out twice -- once with the JDK's own layout
/// managers and once with this layer's -- and requires identical results.
///
/// A tree is described once, as [Node]s, and built from both class
/// libraries. Leaves are bare lightweight components with fixed minimum,
/// preferred and maximum sizes, so nothing depends on fonts, a look and feel
/// or a display, and the JDK side runs headless. After the root is sized and
/// every container laid out top down, the two trees are written out as text
/// -- the bounds and visibility of every component, and what each
/// container's layout manager answers for its preferred, minimum and maximum
/// size -- and the two texts are compared.
///
/// Besides the hand written cases, `GridBagLayout` and `BoxLayout` are run
/// over a few hundred trees drawn from a seeded random generator, since
/// their arithmetic has far more paths than a handful of examples reach.
public class LayoutParityTest {

    /// The comparison is with a desktop, whose display holds the rows laid
    /// out here: on one that is narrower a flow layout asks for the height
    /// of the rows it wraps into, which no desktop does.
    @org.junit.Before
    public void aDisplayWideEnough() {
        com.codename1.desktopcompat.rt.Units.setScale(1f);
    }

    @org.junit.After
    public void theDisplaysOwnScale() {
        com.codename1.desktopcompat.rt.Units.setScale(0);
    }

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final int LEAF = 0;
    private static final int PANEL = 1;
    private static final int HGLUE = 2;
    private static final int VGLUE = 3;
    private static final int GLUE = 4;
    private static final int HSTRUT = 5;
    private static final int VSTRUT = 6;
    private static final int RIGID = 7;

    private static final int FLOW = 0;
    private static final int BORDER = 1;
    private static final int GRID = 2;
    private static final int CARD = 3;
    private static final int BOX = 4;
    private static final int GRIDBAG = 5;
    private static final int OVERLAY = 6;

    /// One component of a tree, independent of the class library it is
    /// built from.
    private static final class Node {
        final int kind;
        int pw;
        int ph;
        int minw;
        int minh;
        int maxw = -1;
        int maxh = -1;
        float ax = 0.5f;
        float ay = 0.5f;
        boolean hidden;
        int base = -1;
        int[] layout;
        int[] border;
        int[] colW;
        int[] rowH;
        double[] colWt;
        double[] rowWt;
        final List<Node> kids = new ArrayList<Node>();
        final List<Object> cons = new ArrayList<Object>();

        Node(int kind) {
            this.kind = kind;
        }

        Node min(int w, int h) {
            minw = w;
            minh = h;
            return this;
        }

        Node max(int w, int h) {
            maxw = w;
            maxh = h;
            return this;
        }

        Node align(float x, float y) {
            ax = x;
            ay = y;
            return this;
        }

        Node hide() {
            hidden = true;
            return this;
        }

        /// Gives the leaf a baseline that many pixels from its top,
        /// whatever its size.
        Node base(int b) {
            base = b;
            return this;
        }

        Node border(int top, int left, int bottom, int right) {
            border = new int[] {top, left, bottom, right};
            return this;
        }

        Node add(Node child) {
            return add(child, null);
        }

        Node add(Node child, Object constraint) {
            kids.add(child);
            cons.add(constraint);
            return this;
        }
    }

    /// Grid bag constraints, independent of the class library.
    private static final class G {
        int gridx = -1;
        int gridy = -1;
        int gw = 1;
        int gh = 1;
        double wx;
        double wy;
        int anchor = 10;
        int fill;
        int top;
        int left;
        int bottom;
        int right;
        int ipadx;
        int ipady;

        G at(int x, int y) {
            gridx = x;
            gridy = y;
            return this;
        }

        G span(int w, int h) {
            gw = w;
            gh = h;
            return this;
        }

        G weight(double x, double y) {
            wx = x;
            wy = y;
            return this;
        }

        G anchor(int a) {
            anchor = a;
            return this;
        }

        G fill(int f) {
            fill = f;
            return this;
        }

        G insets(int t, int l, int b, int r) {
            top = t;
            left = l;
            bottom = b;
            right = r;
            return this;
        }

        G ipad(int x, int y) {
            ipadx = x;
            ipady = y;
            return this;
        }
    }

    private static Node leaf(int w, int h) {
        Node n = new Node(LEAF);
        n.pw = w;
        n.ph = h;
        n.minw = w;
        n.minh = h;
        return n;
    }

    private static Node filler(int kind, int w, int h) {
        Node n = new Node(kind);
        n.pw = w;
        n.ph = h;
        return n;
    }

    private static Node panel(int... layout) {
        Node n = new Node(PANEL);
        n.layout = layout;
        return n;
    }

    private static G g() {
        return new G();
    }

    // ------------------------------------------------------------------
    // The JDK side

    private static final class RealLeaf extends java.awt.Component {
        private static final long serialVersionUID = 1L;
        private final float ax;
        private final float ay;

        private final int base;

        RealLeaf(float ax, float ay, int base) {
            this.ax = ax;
            this.ay = ay;
            this.base = base;
        }

        @Override
        public int getBaseline(int width, int height) {
            return base;
        }

        @Override
        public float getAlignmentX() {
            return ax;
        }

        @Override
        public float getAlignmentY() {
            return ay;
        }
    }

    private static java.awt.Component real(Node n) {
        switch (n.kind) {
            case LEAF: {
                java.awt.Component c = new RealLeaf(n.ax, n.ay, n.base);
                c.setPreferredSize(new java.awt.Dimension(n.pw, n.ph));
                c.setMinimumSize(new java.awt.Dimension(n.minw, n.minh));
                if (n.maxw >= 0) {
                    c.setMaximumSize(new java.awt.Dimension(n.maxw, n.maxh));
                }
                c.setVisible(!n.hidden);
                return c;
            }
            case HGLUE:
                return javax.swing.Box.createHorizontalGlue();
            case VGLUE:
                return javax.swing.Box.createVerticalGlue();
            case GLUE:
                return javax.swing.Box.createGlue();
            case HSTRUT:
                return javax.swing.Box.createHorizontalStrut(n.pw);
            case VSTRUT:
                return javax.swing.Box.createVerticalStrut(n.ph);
            case RIGID:
                return javax.swing.Box.createRigidArea(new java.awt.Dimension(n.pw, n.ph));
            default:
                break;
        }
        java.awt.Container c;
        if (n.border != null) {
            javax.swing.JComponent jc = new javax.swing.JComponent() {
                private static final long serialVersionUID = 1L;
            };
            jc.setBorder(javax.swing.BorderFactory.createEmptyBorder(n.border[0], n.border[1], n.border[2],
                    n.border[3]));
            c = jc;
        } else {
            c = new java.awt.Container();
        }
        int[] l = n.layout;
        switch (l[0]) {
            case FLOW:
                c.setLayout(new java.awt.FlowLayout(l[1], l[2], l[3]));
                break;
            case BORDER:
                c.setLayout(new java.awt.BorderLayout(l[1], l[2]));
                break;
            case GRID:
                c.setLayout(new java.awt.GridLayout(l[1], l[2], l[3], l[4]));
                break;
            case CARD:
                c.setLayout(new java.awt.CardLayout(l[1], l[2]));
                break;
            case BOX:
                c.setLayout(new javax.swing.BoxLayout(c, l[1]));
                break;
            case OVERLAY:
                c.setLayout(new javax.swing.OverlayLayout(c));
                break;
            default: {
                java.awt.GridBagLayout gb = new java.awt.GridBagLayout();
                gb.columnWidths = n.colW;
                gb.rowHeights = n.rowH;
                gb.columnWeights = n.colWt;
                gb.rowWeights = n.rowWt;
                c.setLayout(gb);
                break;
            }
        }
        for (int i = 0; i < n.kids.size(); i++) {
            Object con = n.cons.get(i);
            if (con instanceof G) {
                G s = (G) con;
                con = new java.awt.GridBagConstraints(s.gridx, s.gridy, s.gw, s.gh, s.wx, s.wy, s.anchor, s.fill,
                        new java.awt.Insets(s.top, s.left, s.bottom, s.right), s.ipadx, s.ipady);
            }
            java.awt.Component kid = real(n.kids.get(i));
            if (con == null) {
                c.add(kid);
            } else {
                c.add(kid, con);
            }
        }
        if (n.hidden) {
            c.setVisible(false);
        }
        return c;
    }

    private static void layout(java.awt.Component c) {
        if (c instanceof java.awt.Container && !(c instanceof javax.swing.Box.Filler)) {
            java.awt.Container k = (java.awt.Container) c;
            k.invalidate();
            k.doLayout();
            for (int i = 0; i < k.getComponentCount(); i++) {
                layout(k.getComponent(i));
            }
        }
    }

    private static void dump(java.awt.Component c, String path, StringBuilder sb) {
        sb.append(path).append(' ').append(c.getX()).append(',').append(c.getY()).append(' ').append(c.getWidth())
                .append('x').append(c.getHeight()).append(c.isVisible() ? "" : " hidden");
        if (c instanceof java.awt.Container && !(c instanceof javax.swing.Box.Filler)) {
            java.awt.Container k = (java.awt.Container) c;
            java.awt.LayoutManager lm = k.getLayout();
            if (lm != null) {
                java.awt.Dimension p = lm.preferredLayoutSize(k);
                java.awt.Dimension m = lm.minimumLayoutSize(k);
                sb.append(" pref=").append(p.width).append('x').append(p.height);
                sb.append(" min=").append(m.width).append('x').append(m.height);
                if (lm instanceof java.awt.LayoutManager2) {
                    java.awt.Dimension x = ((java.awt.LayoutManager2) lm).maximumLayoutSize(k);
                    sb.append(" max=").append(x.width).append('x').append(x.height);
                }
            }
            java.awt.Insets in = k.getInsets();
            sb.append(" in=").append(in.top).append(',').append(in.left).append(',').append(in.bottom).append(',')
                    .append(in.right).append('\n');
            for (int i = 0; i < k.getComponentCount(); i++) {
                dump(k.getComponent(i), path + "/" + i, sb);
            }
        } else {
            sb.append('\n');
        }
    }

    // ------------------------------------------------------------------
    // This layer's side

    private static final class OurLeaf extends Component {
        private final float ax;
        private final float ay;

        private final int base;

        OurLeaf(float ax, float ay, int base) {
            this.ax = ax;
            this.ay = ay;
            this.base = base;
        }

        @Override
        public int getBaseline(int width, int height) {
            return base;
        }

        @Override
        public float getAlignmentX() {
            return ax;
        }

        @Override
        public float getAlignmentY() {
            return ay;
        }
    }

    private static Component ours(Node n) {
        switch (n.kind) {
            case LEAF: {
                Component c = new OurLeaf(n.ax, n.ay, n.base);
                c.setPreferredSize(new Dimension(n.pw, n.ph));
                c.setMinimumSize(new Dimension(n.minw, n.minh));
                if (n.maxw >= 0) {
                    c.setMaximumSize(new Dimension(n.maxw, n.maxh));
                }
                c.setVisible(!n.hidden);
                return c;
            }
            case HGLUE:
                return com.codename1.desktopcompat.javax.swing.Box.createHorizontalGlue();
            case VGLUE:
                return com.codename1.desktopcompat.javax.swing.Box.createVerticalGlue();
            case GLUE:
                return com.codename1.desktopcompat.javax.swing.Box.createGlue();
            case HSTRUT:
                return com.codename1.desktopcompat.javax.swing.Box.createHorizontalStrut(n.pw);
            case VSTRUT:
                return com.codename1.desktopcompat.javax.swing.Box.createVerticalStrut(n.ph);
            case RIGID:
                return com.codename1.desktopcompat.javax.swing.Box.createRigidArea(new Dimension(n.pw, n.ph));
            default:
                break;
        }
        Container c;
        if (n.border != null) {
            JComponent jc = new JComponent() {
            };
            jc.setBorder(new EmptyBorder(n.border[0], n.border[1], n.border[2], n.border[3]));
            c = jc;
        } else {
            c = new Container();
        }
        int[] l = n.layout;
        switch (l[0]) {
            case FLOW:
                c.setLayout(new com.codename1.desktopcompat.java.awt.FlowLayout(l[1], l[2], l[3]));
                break;
            case BORDER:
                c.setLayout(new com.codename1.desktopcompat.java.awt.BorderLayout(l[1], l[2]));
                break;
            case GRID:
                c.setLayout(new com.codename1.desktopcompat.java.awt.GridLayout(l[1], l[2], l[3], l[4]));
                break;
            case CARD:
                c.setLayout(new com.codename1.desktopcompat.java.awt.CardLayout(l[1], l[2]));
                break;
            case BOX:
                c.setLayout(new com.codename1.desktopcompat.javax.swing.BoxLayout(c, l[1]));
                break;
            case OVERLAY:
                c.setLayout(new com.codename1.desktopcompat.javax.swing.OverlayLayout(c));
                break;
            default: {
                com.codename1.desktopcompat.java.awt.GridBagLayout gb =
                        new com.codename1.desktopcompat.java.awt.GridBagLayout();
                gb.columnWidths = n.colW;
                gb.rowHeights = n.rowH;
                gb.columnWeights = n.colWt;
                gb.rowWeights = n.rowWt;
                c.setLayout(gb);
                break;
            }
        }
        for (int i = 0; i < n.kids.size(); i++) {
            Object con = n.cons.get(i);
            if (con instanceof G) {
                G s = (G) con;
                con = new com.codename1.desktopcompat.java.awt.GridBagConstraints(s.gridx, s.gridy, s.gw, s.gh, s.wx,
                        s.wy, s.anchor, s.fill, new Insets(s.top, s.left, s.bottom, s.right), s.ipadx, s.ipady);
            }
            Component kid = ours(n.kids.get(i));
            if (con == null) {
                c.add(kid);
            } else {
                c.add(kid, con);
            }
        }
        if (n.hidden) {
            c.setVisible(false);
        }
        return c;
    }

    private static void layout(Component c) {
        if (c instanceof Container && !(c instanceof com.codename1.desktopcompat.javax.swing.Box.Filler)) {
            Container k = (Container) c;
            k.invalidate();
            k.doLayout();
            for (int i = 0; i < k.getComponentCount(); i++) {
                layout(k.getComponent(i));
            }
        }
    }

    private static void dump(Component c, String path, StringBuilder sb) {
        sb.append(path).append(' ').append(c.getX()).append(',').append(c.getY()).append(' ').append(c.getWidth())
                .append('x').append(c.getHeight()).append(c.isVisible() ? "" : " hidden");
        if (c instanceof Container && !(c instanceof com.codename1.desktopcompat.javax.swing.Box.Filler)) {
            Container k = (Container) c;
            LayoutManager lm = k.getLayout();
            if (lm != null) {
                Dimension p = lm.preferredLayoutSize(k);
                Dimension m = lm.minimumLayoutSize(k);
                sb.append(" pref=").append(p.width).append('x').append(p.height);
                sb.append(" min=").append(m.width).append('x').append(m.height);
                if (lm instanceof LayoutManager2) {
                    Dimension x = ((LayoutManager2) lm).maximumLayoutSize(k);
                    sb.append(" max=").append(x.width).append('x').append(x.height);
                }
            }
            Insets in = k.getInsets();
            sb.append(" in=").append(in.top).append(',').append(in.left).append(',').append(in.bottom).append(',')
                    .append(in.right).append('\n');
            for (int i = 0; i < k.getComponentCount(); i++) {
                dump(k.getComponent(i), path + "/" + i, sb);
            }
        } else {
            sb.append('\n');
        }
    }

    // ------------------------------------------------------------------

    private static String dump(java.awt.Component c) {
        StringBuilder sb = new StringBuilder();
        dump(c, "", sb);
        return sb.toString();
    }

    private static String dump(Component c) {
        StringBuilder sb = new StringBuilder();
        dump(c, "", sb);
        return sb.toString();
    }

    /// Builds the tree from both libraries, lays it out at each of the
    /// sizes given as width and height pairs, and compares.
    private static void check(String what, Node tree, int... sizes) {
        java.awt.Component r = real(tree);
        Component o = ours(tree);
        for (int i = 0; i < sizes.length; i += 2) {
            r.setSize(sizes[i], sizes[i + 1]);
            o.setSize(sizes[i], sizes[i + 1]);
            layout(r);
            layout(o);
            String expected = dump(r);
            String actual = dump(o);
            if (!expected.equals(actual)) {
                assertEquals(what + " at " + sizes[i] + "x" + sizes[i + 1] + "\n" + describe(tree, ""), expected,
                        actual);
            }
        }
    }

    /// The tree as text, for the message of a failed comparison.
    private static String describe(Node n, String indent) {
        StringBuilder sb = new StringBuilder(indent);
        sb.append("kind=").append(n.kind).append(" pref=").append(n.pw).append('x').append(n.ph).append(" min=")
                .append(n.minw).append('x').append(n.minh).append(" max=").append(n.maxw).append('x').append(n.maxh)
                .append(" align=").append(n.ax).append(',').append(n.ay).append(n.hidden ? " hidden" : "");
        if (n.layout != null) {
            sb.append(" layout=").append(java.util.Arrays.toString(n.layout));
        }
        if (n.border != null) {
            sb.append(" border=").append(java.util.Arrays.toString(n.border));
        }
        if (n.colW != null || n.rowH != null || n.colWt != null || n.rowWt != null) {
            sb.append(" overrides=").append(java.util.Arrays.toString(n.colW)).append(java.util.Arrays.toString(n.rowH))
                    .append(java.util.Arrays.toString(n.colWt)).append(java.util.Arrays.toString(n.rowWt));
        }
        sb.append('\n');
        for (int i = 0; i < n.kids.size(); i++) {
            Object con = n.cons.get(i);
            if (con instanceof G) {
                G s = (G) con;
                sb.append(indent).append("  at ").append(s.gridx).append(',').append(s.gridy).append(" span ")
                        .append(s.gw).append(',').append(s.gh).append(" weight ").append(s.wx).append(',')
                        .append(s.wy).append(" anchor ").append(s.anchor).append(" fill ").append(s.fill)
                        .append(" insets ").append(s.top).append(',').append(s.left).append(',').append(s.bottom)
                        .append(',').append(s.right).append(" ipad ").append(s.ipadx).append(',').append(s.ipady)
                        .append('\n');
            } else if (con != null) {
                sb.append(indent).append("  at ").append(con).append('\n');
            }
            sb.append(describe(n.kids.get(i), indent + "  "));
        }
        return sb.toString();
    }

    private static Node sixLeaves(Node p) {
        p.add(leaf(40, 20)).add(leaf(70, 35)).add(leaf(25, 10).hide()).add(leaf(55, 28)).add(leaf(90, 14))
                .add(leaf(33, 41).min(10, 12));
        return p;
    }

    @Test
    public void flowLayout() {
        int[][] gaps = {{5, 5}, {7, 3}, {0, 0}, {-2, 4}};
        for (int align = 0; align <= 4; align++) {
            for (int[] gap : gaps) {
                check("flow align " + align + " gap " + gap[0], sixLeaves(panel(FLOW, align, gap[0], gap[1])), 400,
                        200, 331, 90, 150, 300, 121, 77, 60, 60, 0, 0);
                check("flow with insets, align " + align,
                        sixLeaves(panel(FLOW, align, gap[0], gap[1]).border(3, 11, 6, 2)), 400, 200, 175, 80, 20, 20);
            }
        }
        check("empty flow", panel(FLOW, 1, 5, 5), 100, 100);
        check("all hidden", panel(FLOW, 0, 5, 5).add(leaf(10, 10).hide()).add(leaf(20, 20).hide()), 100, 100);
    }

    @Test
    public void borderLayout() {
        String[] names = {"North", "South", "East", "West", "Center"};
        int[][] sizes = {{60, 20}, {80, 25}, {30, 50}, {35, 45}, {100, 70}};
        int[][] gaps = {{0, 0}, {4, 6}};
        for (int[] gap : gaps) {
            for (int mask = 0; mask < 32; mask++) {
                Node p = panel(BORDER, gap[0], gap[1]);
                Node q = panel(BORDER, gap[0], gap[1]).border(5, 7, 9, 11);
                for (int i = 0; i < 5; i++) {
                    if ((mask & (1 << i)) != 0) {
                        p.add(leaf(sizes[i][0], sizes[i][1]).min(sizes[i][0] / 2, sizes[i][1] / 3), names[i]);
                        q.add(leaf(sizes[i][0], sizes[i][1]).min(sizes[i][0] / 2, sizes[i][1] / 3), names[i]);
                    }
                }
                check("border regions " + mask, p, 300, 200, 50, 30, 301, 77);
                check("border regions with insets " + mask, q, 300, 200, 50, 30);
            }
        }
        check("relative names",
                panel(BORDER, 3, 2).add(leaf(60, 20), "First").add(leaf(80, 25), "Last").add(leaf(30, 50), "Before")
                        .add(leaf(35, 45), "After").add(leaf(10, 10), null),
                300, 200);
        check("relative names win over absolute ones",
                panel(BORDER, 3, 2).add(leaf(60, 20), "North").add(leaf(61, 31), "First").add(leaf(30, 50), "West")
                        .add(leaf(37, 50), "Before").add(leaf(10, 10), "Center"),
                300, 200);
        check("hidden edge",
                panel(BORDER, 3, 2).add(leaf(60, 20).hide(), "North").add(leaf(80, 25), "South")
                        .add(leaf(30, 50).hide(), "East").add(leaf(10, 10), "Center"),
                300, 200);
    }

    @Test
    public void gridLayout() {
        int[][] shapes = {{2, 0}, {0, 3}, {3, 2}, {1, 0}, {0, 1}, {4, 7}};
        int[][] gaps = {{0, 0}, {5, 3}, {-1, 2}};
        int[] counts = {0, 1, 5, 7, 12};
        for (int[] shape : shapes) {
            for (int[] gap : gaps) {
                for (int count : counts) {
                    Node p = panel(GRID, shape[0], shape[1], gap[0], gap[1]);
                    Node q = panel(GRID, shape[0], shape[1], gap[0], gap[1]).border(4, 1, 2, 9);
                    for (int i = 0; i < count; i++) {
                        Node a = leaf(20 + i * 7, 15 + (i * 5) % 13).min(3 + i, 9 - (i % 4));
                        Node b = leaf(20 + i * 7, 15 + (i * 5) % 13).min(3 + i, 9 - (i % 4));
                        if (i == 2) {
                            a.hide();
                            b.hide();
                        }
                        p.add(a);
                        q.add(b);
                    }
                    String what = "grid " + shape[0] + "x" + shape[1] + " gap " + gap[0] + " count " + count;
                    check(what, p, 301, 203, 100, 100, 17, 5);
                    check(what + " with insets", q, 301, 203, 64, 31);
                }
            }
        }
    }

    @Test
    public void cardLayout() {
        Node tree = panel(CARD, 4, 6).border(1, 2, 3, 4).add(leaf(50, 40), "a").add(leaf(90, 20).min(5, 6), "b")
                .add(leaf(30, 80), "c").add(leaf(10, 10), "d");
        java.awt.Container r = (java.awt.Container) real(tree);
        Container o = (Container) ours(tree);
        java.awt.CardLayout rl = (java.awt.CardLayout) r.getLayout();
        com.codename1.desktopcompat.java.awt.CardLayout ol =
                (com.codename1.desktopcompat.java.awt.CardLayout) o.getLayout();
        r.setSize(200, 150);
        o.setSize(200, 150);
        for (int step = 0; step < 12; step++) {
            switch (step) {
                case 1:
                case 2:
                case 9:
                    rl.next(r);
                    ol.next(o);
                    break;
                case 3:
                    rl.show(r, "a");
                    ol.show(o, "a");
                    break;
                case 4:
                case 10:
                    rl.previous(r);
                    ol.previous(o);
                    break;
                case 5:
                    rl.first(r);
                    ol.first(o);
                    break;
                case 6:
                    rl.show(r, "c");
                    ol.show(o, "c");
                    break;
                case 7:
                    rl.show(r, "nothing by this name");
                    ol.show(o, "nothing by this name");
                    break;
                case 8:
                    rl.last(r);
                    ol.last(o);
                    break;
                case 11:
                    r.remove(r.getComponent(0));
                    o.remove(o.getComponent(0));
                    break;
                default:
                    break;
            }
            layout(r);
            layout(o);
            assertEquals("card step " + step, dump(r), dump(o));
        }
        check("empty card", panel(CARD, 0, 0), 100, 100);
        check("all cards hidden before layout", panel(CARD, 0, 0).add(leaf(5, 5).hide(), "x").add(leaf(6, 6), "y"),
                100, 100);
    }

    private static Node boxKids(Node p) {
        p.add(leaf(40, 20).max(60, 30).align(0f, 0f));
        p.add(filler(HSTRUT, 7, 0));
        p.add(leaf(70, 35).min(20, 10).max(300, 400).align(1f, 1f));
        p.add(filler(GLUE, 0, 0));
        p.add(leaf(25, 10).hide().align(0.25f, 0.75f));
        p.add(filler(VSTRUT, 0, 9));
        p.add(leaf(55, 28).min(11, 7).align(0.3f, 0.8f));
        p.add(filler(RIGID, 13, 17));
        p.add(leaf(33, 41).min(33, 41).max(33, 41));
        return p;
    }

    @Test
    public void boxLayout() {
        for (int axis = 0; axis <= 3; axis++) {
            check("box axis " + axis, boxKids(panel(BOX, axis)), 600, 300, 300, 600, 243, 200, 200, 243, 120, 90, 90,
                    120, 30, 30, 0, 0, 5000, 4000);
            check("box with insets, axis " + axis, boxKids(panel(BOX, axis).border(6, 4, 2, 8)), 600, 300, 150, 150);
        }
        check("glue between",
                panel(BOX, 0).add(leaf(30, 30)).add(filler(HGLUE, 0, 0)).add(leaf(40, 20)).add(filler(HGLUE, 0, 0))
                        .add(leaf(10, 50).max(10, 50)),
                401, 100, 80, 20);
        check("vertical glue",
                panel(BOX, 1).add(leaf(30, 30).align(0f, 0.5f)).add(filler(VGLUE, 0, 0))
                        .add(leaf(40, 20).max(40, 20).align(0f, 0.5f)).add(filler(VSTRUT, 0, 12))
                        .add(leaf(10, 50).max(1000, 50).align(0f, 0.5f)),
                200, 400, 20, 80);
        check("nested boxes",
                panel(BOX, 0).border(2, 3, 4, 5)
                        .add(panel(BOX, 1).add(leaf(30, 30).max(90, 40)).add(filler(VGLUE, 0, 0)).add(leaf(50, 20)))
                        .add(filler(HSTRUT, 10, 0))
                        .add(panel(BOX, 1).border(1, 1, 1, 1).add(leaf(60, 15).align(1f, 0f))
                                .add(leaf(20, 15).align(0f, 0f).max(25, 15)))
                        .add(leaf(44, 44).align(0.5f, 0.2f)),
                500, 220, 140, 60);
        check("empty box", panel(BOX, 1), 50, 50);
        check("overlay", boxKids(panel(OVERLAY)), 300, 200, 40, 40);
        check("overlay with insets", boxKids(panel(OVERLAY).border(3, 5, 7, 9)), 300, 200);
    }

    @Test
    public void boxLayoutRandom() {
        Random rnd = new Random(20240607L);
        float[] aligns = {0f, 0.5f, 1f, 0.25f, 0.7f};
        for (int round = 0; round < 300; round++) {
            Node p = panel(BOX, rnd.nextInt(4));
            if (rnd.nextInt(3) == 0) {
                p.border(rnd.nextInt(6), rnd.nextInt(6), rnd.nextInt(6), rnd.nextInt(6));
            }
            int count = rnd.nextInt(7);
            for (int i = 0; i < count; i++) {
                int kind = rnd.nextInt(10);
                if (kind == 0) {
                    p.add(filler(GLUE, 0, 0));
                } else if (kind == 1) {
                    p.add(filler(HSTRUT, rnd.nextInt(30), 0));
                } else if (kind == 2) {
                    p.add(filler(VSTRUT, 0, rnd.nextInt(30)));
                } else {
                    int w = 5 + rnd.nextInt(90);
                    int h = 5 + rnd.nextInt(90);
                    Node n = leaf(w, h).min(rnd.nextInt(w + 1), rnd.nextInt(h + 1)).align(
                            aligns[rnd.nextInt(aligns.length)], aligns[rnd.nextInt(aligns.length)]);
                    if (rnd.nextInt(3) != 0) {
                        n.max(w + rnd.nextInt(200), h + rnd.nextInt(200));
                    }
                    if (rnd.nextInt(9) == 0) {
                        n.hide();
                    }
                    p.add(n);
                }
            }
            check("random box " + round, p, rnd.nextInt(700), rnd.nextInt(500), rnd.nextInt(200), rnd.nextInt(200));
        }
    }

    @Test
    public void gridBagLayout() {
        // 1: weights and fill
        check("weights and fill",
                panel(GRIDBAG).add(leaf(50, 20), g().at(0, 0).weight(1, 0).fill(2))
                        .add(leaf(30, 20), g().at(1, 0).weight(2, 0).fill(1))
                        .add(leaf(40, 30), g().at(0, 1).weight(0, 1).fill(3))
                        .add(leaf(40, 30), g().at(1, 1).weight(0, 3)),
                400, 300, 401, 299, 100, 40, 60, 30);
        // 2: every anchor, in a grid of cells larger than the components
        Node anchors = panel(GRIDBAG);
        int cell = 0;
        for (int a = 10; a <= 26; a++) {
            anchors.add(leaf(20 + a, 10 + a / 2), g().at(cell % 5, cell / 5).weight(1, 1).anchor(a));
            cell++;
        }
        check("anchors", anchors, 640, 480, 333, 257);
        // 3: spans
        check("spans",
                panel(GRIDBAG).add(leaf(120, 20), g().at(0, 0).span(3, 1).fill(2))
                        .add(leaf(30, 90), g().at(0, 1).span(1, 2).fill(3).weight(0, 1))
                        .add(leaf(40, 25), g().at(1, 1).weight(1, 0)).add(leaf(45, 25), g().at(2, 1).weight(3, 0))
                        .add(leaf(200, 31), g().at(1, 2).span(2, 1).weight(0, 2).anchor(14)),
                500, 400, 230, 150, 90, 60);
        // 4: REMAINDER and RELATIVE
        check("remainder and relative",
                panel(GRIDBAG).add(leaf(30, 20), g()).add(leaf(31, 21), g()).add(leaf(32, 22), g().span(0, 1))
                        .add(leaf(90, 23), g().span(-1, 1).fill(2).weight(1, 0)).add(leaf(34, 24), g().span(0, 1))
                        .add(leaf(35, 25), g().span(0, 1).weight(0, 1).fill(1)).add(leaf(36, 26), g().span(1, 0))
                        .add(leaf(37, 27), g().span(0, 0).anchor(18)),
                400, 300, 150, 120);
        // 5: insets and internal padding
        check("insets and ipad",
                panel(GRIDBAG).border(3, 4, 5, 6)
                        .add(leaf(50, 20), g().at(0, 0).insets(1, 2, 3, 4).ipad(10, 6))
                        .add(leaf(30, 20), g().at(1, 0).insets(7, 0, 0, 9).weight(1, 1).fill(1).ipad(3, 0))
                        .add(leaf(40, 30), g().at(0, 1).span(2, 1).insets(5, 5, 5, 5).fill(2))
                        .add(leaf(20, 20).hide(), g().at(5, 5).weight(9, 9)),
                300, 200, 301, 201, 110, 70);
        // 6: shrinking below the preferred size falls back to minimum sizes
        check("minimum sizes",
                panel(GRIDBAG).add(leaf(100, 40).min(20, 10), g().at(0, 0).weight(1, 1).fill(1))
                        .add(leaf(80, 40).min(30, 15), g().at(1, 0).weight(2, 0))
                        .add(leaf(150, 30).min(40, 5), g().at(0, 1).span(2, 1).weight(0, 1).fill(2)),
                400, 200, 179, 60, 60, 20, 30, 10);
        // 7: the public override arrays
        Node forced = panel(GRIDBAG).add(leaf(20, 20), g().at(0, 0)).add(leaf(30, 30), g().at(1, 1).fill(1))
                .add(leaf(10, 10), g().at(3, 0).anchor(12));
        forced.colW = new int[] {50, 10, 40, 0, 25};
        forced.rowH = new int[] {5, 60};
        forced.colWt = new double[] {0, 1, 0, 0.5};
        forced.rowWt = new double[] {1, 0, 4};
        check("override arrays", forced, 400, 300, 100, 50);
        // 8: nothing has weight, so the grid is centred
        check("centred", panel(GRIDBAG).border(1, 9, 3, 2).add(leaf(50, 20), g().at(0, 0))
                .add(leaf(30, 60), g().at(1, 1)).add(leaf(10, 10), g().at(4, 0).span(1, 2).fill(3)), 301, 201, 60,
                40);
        // 9: relative position mixed with absolute
        check("mixed positions",
                panel(GRIDBAG).add(leaf(20, 20), g().at(2, -1)).add(leaf(21, 21), g().at(2, -1))
                        .add(leaf(22, 22), g().at(-1, 1)).add(leaf(23, 23), g().at(-1, 0).span(2, 1))
                        .add(leaf(24, 24), g().at(0, 3).span(0, 1).fill(2)).add(leaf(25, 25), g()),
                300, 300);
        check("empty grid bag", panel(GRIDBAG), 100, 100);
    }

    @Test
    public void gridBagLayoutRandom() {
        Random rnd = new Random(99173L);
        for (int round = 0; round < 400; round++) {
            Node p = panel(GRIDBAG);
            if (rnd.nextInt(3) == 0) {
                p.border(rnd.nextInt(8), rnd.nextInt(8), rnd.nextInt(8), rnd.nextInt(8));
            }
            if (rnd.nextInt(8) == 0) {
                p.colW = new int[] {rnd.nextInt(40), rnd.nextInt(40), rnd.nextInt(40)};
                p.rowWt = new double[] {rnd.nextInt(3), rnd.nextInt(3)};
            }
            int count = 1 + rnd.nextInt(8);
            boolean relative = rnd.nextInt(4) == 0;
            for (int i = 0; i < count; i++) {
                int w = 5 + rnd.nextInt(90);
                int h = 5 + rnd.nextInt(60);
                Node n = leaf(w, h).min(rnd.nextInt(w + 1), rnd.nextInt(h + 1));
                if (rnd.nextInt(10) == 0) {
                    n.hide();
                }
                G c = g();
                if (relative) {
                    c.at(rnd.nextInt(3) == 0 ? rnd.nextInt(4) : -1, rnd.nextInt(5) == 0 ? rnd.nextInt(4) : -1);
                    c.span(rnd.nextInt(4) == 0 ? rnd.nextInt(3) - 1 : 1, rnd.nextInt(9) == 0 ? rnd.nextInt(3) - 1 : 1);
                } else {
                    c.at(rnd.nextInt(5), rnd.nextInt(5));
                    c.span(1 + rnd.nextInt(3), 1 + rnd.nextInt(3));
                }
                if (rnd.nextInt(2) == 0) {
                    c.weight(rnd.nextInt(4) * 0.5, rnd.nextInt(4) * 0.25);
                }
                c.anchor(10 + rnd.nextInt(17)).fill(rnd.nextInt(4));
                if (rnd.nextInt(3) == 0) {
                    c.insets(rnd.nextInt(6), rnd.nextInt(6), rnd.nextInt(6), rnd.nextInt(6));
                }
                if (rnd.nextInt(4) == 0) {
                    c.ipad(rnd.nextInt(9), rnd.nextInt(9));
                }
                p.add(n, c);
            }
            check("random grid bag " + round, p, 200 + rnd.nextInt(500), 150 + rnd.nextInt(400), rnd.nextInt(250),
                    rnd.nextInt(200));
        }
    }

    @Test
    public void overlayLayout() {
        check("corners",
                panel(OVERLAY).add(leaf(40, 20).align(0f, 0f)).add(leaf(70, 35).align(1f, 1f))
                        .add(leaf(30, 50).align(0f, 1f)).add(leaf(60, 10).align(1f, 0f)),
                300, 200, 110, 85, 60, 40, 0, 0);
        check("centred, growing to the maximum",
                panel(OVERLAY).add(leaf(40, 20).max(1000, 1000)).add(leaf(70, 35).max(90, 500))
                        .add(leaf(30, 50).max(30, 50)).add(leaf(60, 10)),
                300, 200, 70, 50, 20, 20);
        check("shrinking to the minimum",
                panel(OVERLAY).border(2, 4, 6, 8).add(leaf(140, 120).min(10, 10).align(0.25f, 0.75f))
                        .add(leaf(170, 135).min(60, 20).align(0.75f, 0.25f)).add(leaf(90, 90).min(90, 90).align(1f, 0f)),
                400, 400, 200, 150, 100, 60, 12, 8);
        check("hidden children still take part",
                panel(OVERLAY).add(leaf(40, 20).align(0f, 0f)).add(leaf(170, 135).hide().align(1f, 1f))
                        .add(leaf(30, 50).hide()),
                300, 200, 50, 50);
        check("one child", panel(OVERLAY).border(1, 2, 3, 4).add(leaf(40, 20).max(45, 300).align(0.3f, 0.6f)), 300,
                200, 10, 10);
        check("empty overlay", panel(OVERLAY).border(1, 2, 3, 4), 50, 50);
        check("nested overlays",
                panel(OVERLAY).border(3, 3, 3, 3)
                        .add(panel(OVERLAY).add(leaf(40, 20).align(0f, 0f)).add(leaf(20, 60).align(1f, 0.5f)))
                        .add(panel(BOX, 1).add(leaf(30, 30).max(90, 40)).add(filler(VGLUE, 0, 0)).add(leaf(50, 20)))
                        .add(panel(FLOW, 1, 5, 5).add(leaf(33, 12)).add(leaf(44, 12))).add(leaf(10, 10).max(900, 900)),
                400, 300, 120, 90, 30, 30);
        check("an overlay inside other layouts",
                panel(BORDER, 2, 2).add(panel(OVERLAY).add(leaf(40, 20).align(0f, 1f)).add(leaf(70, 35)), "North")
                        .add(panel(OVERLAY).border(1, 1, 1, 1).add(leaf(55, 44).max(60, 400).align(1f, 0f))
                                .add(leaf(22, 66).min(5, 5)), "West")
                        .add(panel(OVERLAY).add(leaf(10, 10).max(5000, 5000)).add(leaf(80, 80).max(80, 80)), "Center"),
                500, 400, 100, 100);
    }

    @Test
    public void overlayLayoutRandom() {
        Random rnd = new Random(551177L);
        float[] aligns = {0f, 0.5f, 1f, 0.25f, 0.7f, 0.1f, 0.9f};
        for (int round = 0; round < 400; round++) {
            Node p = panel(OVERLAY);
            if (rnd.nextInt(3) == 0) {
                p.border(rnd.nextInt(6), rnd.nextInt(6), rnd.nextInt(6), rnd.nextInt(6));
            }
            int count = rnd.nextInt(6);
            for (int i = 0; i < count; i++) {
                int w = 5 + rnd.nextInt(90);
                int h = 5 + rnd.nextInt(90);
                Node n = leaf(w, h).min(rnd.nextInt(w + 1), rnd.nextInt(h + 1)).align(
                        aligns[rnd.nextInt(aligns.length)], aligns[rnd.nextInt(aligns.length)]);
                if (rnd.nextInt(3) != 0) {
                    n.max(w + rnd.nextInt(200), h + rnd.nextInt(200));
                }
                if (rnd.nextInt(9) == 0) {
                    n.hide();
                }
                p.add(n);
            }
            check("random overlay " + round, p, rnd.nextInt(500), rnd.nextInt(400), rnd.nextInt(120),
                    rnd.nextInt(120), 0, 0);
        }
    }

    /// The anchors that are relative to the baseline of a row, with
    /// components that have a baseline and components that have none.
    @Test
    public void gridBagBaselineAnchors() {
        int[] anchors = {0x100, 0x200, 0x300, 0x400, 0x500, 0x600, 0x700, 0x800, 0x900};
        Node none = panel(GRIDBAG);
        Node some = panel(GRIDBAG).border(2, 3, 4, 5);
        Node filled = panel(GRIDBAG);
        for (int i = 0; i < anchors.length; i++) {
            none.add(leaf(20 + i, 10 + 3 * i), g().at(i % 3, i / 3).weight(1, 1).anchor(anchors[i]));
            some.add(leaf(20 + i, 14 + 3 * i).base(4 + 2 * i), g().at(i % 3, i / 3).weight(1, 1).anchor(anchors[i])
                    .insets(1, 2, 3, i));
            filled.add(leaf(20 + i, 14 + 3 * i).base(i % 2 == 0 ? 5 + i : -1),
                    g().at(i % 4, i / 4).weight(i % 2, 1).anchor(anchors[i]).fill(i % 4).ipad(i, 2));
        }
        check("baseline anchors without baselines", none, 400, 300, 90, 70, 30, 20);
        check("baseline anchors", some, 400, 300, 401, 299, 90, 70, 30, 20);
        check("baseline anchors with fill", filled, 400, 300, 120, 80);
        check("a row on one baseline",
                panel(GRIDBAG).add(leaf(40, 20).base(15), g().at(0, 0).anchor(0x100))
                        .add(leaf(60, 30).base(22), g().at(1, 0).anchor(0x200).weight(1, 0))
                        .add(leaf(30, 12).base(3), g().at(2, 0).anchor(0x300))
                        .add(leaf(30, 40), g().at(3, 0).anchor(0x100))
                        .add(leaf(30, 16).base(8), g().at(0, 1).anchor(0x400))
                        .add(leaf(35, 26).base(20), g().at(1, 1).anchor(0x100))
                        .add(leaf(30, 16).base(8), g().at(2, 1).anchor(0x700).weight(0, 1)),
                400, 300, 200, 80, 50, 30);
    }

    /// Grid bag cases the other tests leave out: fractional and uneven
    /// weights, negative padding, spans that reach past the last occupied
    /// cell, and vertical runs placed relatively.
    @Test
    public void gridBagLayoutMore() {
        check("uneven weights",
                panel(GRIDBAG).add(leaf(50, 20), g().at(0, 0).weight(0.1, 0.3).fill(1))
                        .add(leaf(30, 20), g().at(1, 0).weight(0.25, 0).fill(1))
                        .add(leaf(40, 30), g().at(2, 0).weight(0.65, 0.7).fill(1))
                        .add(leaf(40, 30), g().at(0, 1).span(3, 1).weight(7, 0.05).fill(2)),
                400, 300, 403, 307, 997, 601, 121, 51, 50, 20);
        check("weight on a spanning component only",
                panel(GRIDBAG).add(leaf(50, 20), g().at(0, 0)).add(leaf(30, 20), g().at(1, 0))
                        .add(leaf(40, 30), g().at(2, 0))
                        .add(leaf(200, 30), g().at(0, 1).span(2, 1).weight(1, 1).fill(1))
                        .add(leaf(10, 90), g().at(2, 1).span(1, 2).weight(0, 2).fill(3))
                        .add(leaf(20, 20), g().at(0, 2)),
                500, 400, 250, 140, 100, 60);
        check("negative padding and insets",
                panel(GRIDBAG).border(1, 1, 1, 1).add(leaf(50, 20), g().at(0, 0).ipad(-10, -4))
                        .add(leaf(30, 20), g().at(1, 0).ipad(-40, 3).insets(-2, -3, 4, 5).fill(1).weight(1, 1))
                        .add(leaf(40, 30).min(4, 4), g().at(0, 1).span(2, 1).insets(9, -5, -5, 9).fill(2)),
                300, 200, 80, 40, 20, 10);
        check("spans past the grid",
                panel(GRIDBAG).add(leaf(50, 20), g().at(0, 0).span(4, 1).fill(2).weight(1, 0))
                        .add(leaf(30, 20), g().at(1, 1).span(1, 5).fill(3).weight(0, 1))
                        .add(leaf(40, 30), g().at(6, 3).anchor(14)),
                300, 200, 100, 60);
        check("a column placed relatively",
                panel(GRIDBAG).add(leaf(30, 20), g().at(0, -1)).add(leaf(31, 21), g().at(0, -1))
                        .add(leaf(32, 22), g().at(0, -1).span(1, 0))
                        .add(leaf(33, 23), g().at(1, -1).span(1, -1).fill(3).weight(0, 1))
                        .add(leaf(34, 24), g().at(1, -1).span(1, 0))
                        .add(leaf(35, 25), g().at(-1, -1).span(0, 0).weight(1, 0).fill(1)),
                400, 300, 120, 100);
        check("hidden components leave their cells",
                panel(GRIDBAG).add(leaf(50, 20), g().at(0, 0)).add(leaf(300, 200).hide(), g().at(1, 0).weight(5, 5))
                        .add(leaf(40, 30), g().at(2, 0).weight(1, 0)).add(leaf(40, 30).hide(), g())
                        .add(leaf(41, 31), g().span(0, 1)).add(leaf(42, 32), g()),
                400, 300, 60, 40);
        check("nested grid bags",
                panel(GRIDBAG).border(2, 2, 2, 2)
                        .add(panel(GRIDBAG).add(leaf(30, 20), g().at(0, 0).weight(1, 0).fill(2))
                                .add(leaf(31, 21), g().at(1, 0)), g().at(0, 0).weight(1, 0).fill(2))
                        .add(panel(GRIDBAG).border(1, 1, 1, 1).add(leaf(60, 40).min(6, 4), g().weight(1, 1).fill(1)),
                                g().at(0, 1).weight(1, 1).fill(1).insets(3, 3, 3, 3))
                        .add(panel(OVERLAY).add(leaf(20, 20)).add(leaf(44, 12)), g().at(1, 0).span(1, 2).anchor(11)),
                500, 400, 130, 80, 40, 30);
    }

    @Test
    public void gridBagLayoutRandomWide() {
        Random rnd = new Random(4410882L);
        int rejected = 0;
        int[] anchors = {10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 0x100, 0x200, 0x300,
            0x400, 0x500, 0x600, 0x700, 0x800, 0x900};
        for (int round = 0; round < 500; round++) {
            Node p = panel(GRIDBAG);
            if (rnd.nextInt(3) == 0) {
                p.border(rnd.nextInt(8), rnd.nextInt(8), rnd.nextInt(8), rnd.nextInt(8));
            }
            if (rnd.nextInt(6) == 0) {
                p.colW = new int[1 + rnd.nextInt(4)];
                for (int i = 0; i < p.colW.length; i++) {
                    p.colW[i] = rnd.nextInt(60);
                }
            }
            if (rnd.nextInt(6) == 0) {
                p.rowH = new int[1 + rnd.nextInt(4)];
                for (int i = 0; i < p.rowH.length; i++) {
                    p.rowH[i] = rnd.nextInt(40);
                }
            }
            if (rnd.nextInt(6) == 0) {
                p.colWt = new double[1 + rnd.nextInt(4)];
                for (int i = 0; i < p.colWt.length; i++) {
                    p.colWt[i] = rnd.nextInt(5) * 0.3;
                }
            }
            if (rnd.nextInt(6) == 0) {
                p.rowWt = new double[1 + rnd.nextInt(4)];
                for (int i = 0; i < p.rowWt.length; i++) {
                    p.rowWt[i] = rnd.nextInt(5) * 0.7;
                }
            }
            int count = 1 + rnd.nextInt(9);
            int mode = rnd.nextInt(3);
            for (int i = 0; i < count; i++) {
                int w = 5 + rnd.nextInt(90);
                int h = 5 + rnd.nextInt(60);
                Node n = leaf(w, h).min(rnd.nextInt(w + 1), rnd.nextInt(h + 1));
                if (rnd.nextInt(3) == 0) {
                    n.base(rnd.nextInt(h));
                }
                if (rnd.nextInt(10) == 0) {
                    n.hide();
                }
                G c = g();
                if (mode == 0) {
                    c.at(rnd.nextInt(3) == 0 ? rnd.nextInt(4) : -1, rnd.nextInt(5) == 0 ? rnd.nextInt(4) : -1);
                    c.span(rnd.nextInt(4) == 0 ? rnd.nextInt(3) - 1 : 1, rnd.nextInt(9) == 0 ? rnd.nextInt(3) - 1 : 1);
                } else if (mode == 1) {
                    // Columns filled downwards.
                    c.at(rnd.nextInt(3), -1);
                    c.span(1, rnd.nextInt(4) == 0 ? rnd.nextInt(3) - 1 : 1);
                } else {
                    c.at(rnd.nextInt(6), rnd.nextInt(6));
                    c.span(1 + rnd.nextInt(4), 1 + rnd.nextInt(4));
                }
                if (rnd.nextInt(2) == 0) {
                    c.weight(rnd.nextInt(7) * 0.17, rnd.nextInt(7) * 0.31);
                }
                c.anchor(anchors[rnd.nextInt(anchors.length)]).fill(rnd.nextInt(4));
                if (rnd.nextInt(3) == 0) {
                    c.insets(rnd.nextInt(8) - 2, rnd.nextInt(8) - 2, rnd.nextInt(8) - 2, rnd.nextInt(8) - 2);
                }
                if (rnd.nextInt(4) == 0) {
                    c.ipad(rnd.nextInt(14) - 5, rnd.nextInt(14) - 5);
                }
                p.add(n, c);
            }
            int[] sizes = {200 + rnd.nextInt(500), 150 + rnd.nextInt(400), rnd.nextInt(250), rnd.nextInt(200), 0, 0};
            if (!realLaysOut(p, sizes)) {
                rejected++;
                continue;
            }
            check("random wide grid bag " + round, p, sizes);
        }
        assertTrue(rejected + " of 500 random grids were rejected by the JDK", rejected < 50);
    }

    /// Whether the JDK lays the tree out at all. Its grid bag layout indexes
    /// past its per-row baseline arrays when a relatively placed component
    /// lands beyond the rows it counted first and something in the grid is
    /// anchored to a baseline; this layer lays such a grid out, and there
    /// is nothing to compare it with.
    private static boolean realLaysOut(Node tree, int[] sizes) {
        java.awt.Component r = real(tree);
        try {
            for (int i = 0; i < sizes.length; i += 2) {
                r.setSize(sizes[i], sizes[i + 1]);
                layout(r);
                dump(r);
            }
            return true;
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    @Test
    public void nestedContainersWithBorders() {
        Node form = panel(GRIDBAG).border(8, 8, 8, 8);
        for (int row = 0; row < 4; row++) {
            form.add(leaf(60 + row * 5, 16), g().at(0, row).anchor(13).insets(2, 2, 2, 6));
            form.add(leaf(120, 22).min(40, 22), g().at(1, row).weight(1, 0).fill(2).insets(2, 0, 2, 2));
        }
        form.add(leaf(10, 10), g().at(0, 4).span(2, 1).weight(0, 1).fill(1));

        Node buttons = panel(FLOW, 2, 6, 4).border(2, 0, 2, 0).add(leaf(70, 26)).add(leaf(70, 26)).add(leaf(90, 26));
        Node side = panel(BOX, 1).border(4, 4, 4, 4).add(leaf(80, 24).max(200, 24).align(0f, 0.5f))
                .add(filler(VSTRUT, 0, 5)).add(leaf(60, 24).max(200, 24).align(0f, 0.5f)).add(filler(VGLUE, 0, 0))
                .add(leaf(80, 40).align(0f, 0.5f));
        Node tiles = panel(GRID, 2, 3, 4, 4).border(1, 2, 3, 4);
        for (int i = 0; i < 6; i++) {
            tiles.add(panel(BORDER, 1, 1).border(1, 1, 1, 1).add(leaf(30, 10), "North").add(leaf(20 + i, 20), null));
        }
        Node centre = panel(BORDER, 5, 5).add(form, "Center").add(tiles, "South");
        Node root = panel(BORDER, 6, 3).border(10, 12, 14, 16).add(leaf(200, 30), "North").add(side, "West")
                .add(centre, "Center").add(buttons, "South");
        check("nested", root, 800, 600, 640, 480, 320, 240, 100, 100);

        Node cards = panel(CARD, 2, 2).border(5, 5, 5, 5).add(panel(FLOW, 1, 5, 5).add(leaf(40, 40)), "one")
                .add(panel(GRID, 0, 2, 1, 1).add(leaf(10, 10)).add(leaf(20, 20)).add(leaf(30, 30)), "two");
        check("cards of panels", cards, 300, 200);
    }
}
