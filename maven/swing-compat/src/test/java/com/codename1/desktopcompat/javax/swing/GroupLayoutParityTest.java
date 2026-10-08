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
package com.codename1.desktopcompat.javax.swing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Test;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.javax.swing.border.EmptyBorder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Builds the same group layout with the JDK's `javax.swing.GroupLayout`
/// and with this layer's, and requires identical bounds for every
/// component and identical preferred, minimum and maximum layout sizes.
///
/// A layout is described once, as a [Spec], and built from both class
/// libraries. The components are bare ones with fixed sizes and the layout
/// style is an explicit one answering the same numbers on both sides, so
/// nothing depends on a look and feel and the JDK side runs headless.
/// Each layout is run at its preferred size, smaller, larger and at zero,
/// and a few hundred more are drawn from a seeded random generator.
public class GroupLayoutParityTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final int SEQ = 0;
    private static final int PAR = 1;
    private static final int COMP = 2;
    private static final int GAP = 3;
    private static final int AUTO_GAP = 4;
    private static final int PAIR_GAP = 5;
    private static final int EDGE_GAP = 6;

    private static final int LEADING = 0;
    private static final int TRAILING = 1;
    private static final int CENTER = 2;
    private static final int BASELINE = 3;

    private static final int RELATED = 0;
    private static final int UNRELATED = 1;
    private static final int INDENT = 2;

    private static final int D = -1;
    private static final int P = -2;

    /// One element of a group, independent of the class library.
    private static final class S {
        final int kind;
        int align = LEADING;
        boolean resizable = true;
        // 0: createParallelGroup, 1: baseline group anchored to the top,
        // 2: baseline group anchored to the bottom
        int anchor;
        int childAlign = -1;
        boolean asBaseline;
        int comp;
        int comp2;
        int min = D;
        int pref = D;
        int max = D;
        int type;
        final List<S> kids = new ArrayList<S>();

        S(int kind) {
            this.kind = kind;
        }

        S al(int a) {
            childAlign = a;
            return this;
        }

        S bl() {
            asBaseline = true;
            return this;
        }

        S fixed() {
            resizable = false;
            return this;
        }

        void describe(StringBuilder sb, String indent) {
            sb.append(indent);
            switch (kind) {
                case SEQ:
                    sb.append("seq");
                    break;
                case PAR:
                    sb.append("par align=").append(align).append(" resizable=").append(resizable).append(" anchor=")
                            .append(anchor);
                    break;
                case COMP:
                    sb.append("comp ").append(comp).append(' ').append(min).append('/').append(pref).append('/')
                            .append(max);
                    break;
                case GAP:
                    sb.append("gap ").append(min).append('/').append(pref).append('/').append(max);
                    break;
                case AUTO_GAP:
                    sb.append("autogap type=").append(type).append(' ').append(pref).append('/').append(max);
                    break;
                case PAIR_GAP:
                    sb.append("pairgap ").append(comp).append(',').append(comp2).append(" type=").append(type)
                            .append(' ').append(pref).append('/').append(max);
                    break;
                default:
                    sb.append("edgegap ").append(pref).append('/').append(max);
                    break;
            }
            if (childAlign >= 0) {
                sb.append(" childAlign=").append(childAlign);
            }
            if (asBaseline) {
                sb.append(" asBaseline");
            }
            sb.append('\n');
            for (S k : kids) {
                k.describe(sb, indent + "  ");
            }
        }
    }

    /// One component, independent of the class library.
    private static final class C {
        int pw;
        int ph;
        int minw;
        int minh;
        int maxw = -1;
        int maxh = -1;
        boolean plain;
        boolean hidden;
        int baseline = -1;
        // 0: follows the layout, 1: Boolean.TRUE, 2: Boolean.FALSE
        int honors;

        C min(int w, int h) {
            minw = w;
            minh = h;
            return this;
        }

        C max(int w, int h) {
            maxw = w;
            maxh = h;
            return this;
        }

        C plain() {
            plain = true;
            return this;
        }

        C hide() {
            hidden = true;
            return this;
        }

        C base(int b) {
            baseline = b;
            return this;
        }

        C honors(boolean b) {
            honors = b ? 1 : 2;
            return this;
        }
    }

    /// A whole layout, independent of the class library.
    private static final class Spec {
        final List<C> comps = new ArrayList<C>();
        S h;
        S v;
        boolean autoGaps;
        boolean autoContainerGaps;
        boolean honorsVisibility = true;
        int[] border;
        final List<int[]> links = new ArrayList<int[]>();

        int add(C c) {
            comps.add(c);
            return comps.size() - 1;
        }

        /// Links the listed components along `axis`: 0 horizontal, 1
        /// vertical, 2 both.
        Spec link(int axis, int... which) {
            int[] l = new int[which.length + 1];
            l[0] = axis;
            System.arraycopy(which, 0, l, 1, which.length);
            links.add(l);
            return this;
        }

        String describe() {
            StringBuilder sb = new StringBuilder();
            sb.append("autoGaps=").append(autoGaps).append(" autoContainerGaps=").append(autoContainerGaps)
                    .append(" honorsVisibility=").append(honorsVisibility).append(" border=")
                    .append(java.util.Arrays.toString(border)).append('\n');
            for (int i = 0; i < comps.size(); i++) {
                C c = comps.get(i);
                sb.append("  ").append(i).append(": pref=").append(c.pw).append('x').append(c.ph).append(" min=")
                        .append(c.minw).append('x').append(c.minh).append(" max=").append(c.maxw).append('x')
                        .append(c.maxh).append(c.plain ? " plain" : "").append(c.hidden ? " hidden" : "")
                        .append(" baseline=").append(c.baseline).append(" honors=").append(c.honors).append('\n');
            }
            for (int[] l : links) {
                sb.append("  link ").append(java.util.Arrays.toString(l)).append('\n');
            }
            sb.append("horizontal\n");
            h.describe(sb, "  ");
            sb.append("vertical\n");
            v.describe(sb, "  ");
            return sb.toString();
        }
    }

    private static C comp(int w, int h) {
        C c = new C();
        c.pw = w;
        c.ph = h;
        c.minw = w;
        c.minh = h;
        return c;
    }

    private static S seq(S... kids) {
        S s = new S(SEQ);
        Collections.addAll(s.kids, kids);
        return s;
    }

    private static S par(int align, S... kids) {
        S s = new S(PAR);
        s.align = align;
        Collections.addAll(s.kids, kids);
        return s;
    }

    private static S base(boolean anchorTop, S... kids) {
        S s = new S(PAR);
        s.anchor = anchorTop ? 1 : 2;
        Collections.addAll(s.kids, kids);
        return s;
    }

    private static S c(int index) {
        S s = new S(COMP);
        s.comp = index;
        return s;
    }

    private static S c(int index, int min, int pref, int max) {
        S s = c(index);
        s.min = min;
        s.pref = pref;
        s.max = max;
        return s;
    }

    private static S gap(int size) {
        return gap(size, size, size);
    }

    private static S gap(int min, int pref, int max) {
        S s = new S(GAP);
        s.min = min;
        s.pref = pref;
        s.max = max;
        return s;
    }

    private static S auto(int type) {
        return auto(type, D, D);
    }

    private static S auto(int type, int pref, int max) {
        S s = new S(AUTO_GAP);
        s.type = type;
        s.pref = pref;
        s.max = max;
        return s;
    }

    private static S pair(int a, int b, int type) {
        return pair(a, b, type, D, P);
    }

    private static S pair(int a, int b, int type, int pref, int max) {
        S s = new S(PAIR_GAP);
        s.comp = a;
        s.comp2 = b;
        s.type = type;
        s.pref = pref;
        s.max = max;
        return s;
    }

    private static S edge() {
        return edge(D, D);
    }

    private static S edge(int pref, int max) {
        S s = new S(EDGE_GAP);
        s.pref = pref;
        s.max = max;
        return s;
    }

    // The numbers both layout styles answer. They vary with the kind of
    // gap, with the position and with the components, so that a wrong
    // argument shows up as a different layout.
    private static int styleGap(int type, int position, int w1, int h2) {
        int gap = type == RELATED ? 5 : type == UNRELATED ? 11 : 17;
        if (position == 5) {
            gap += 2;
        } else if (position != 3) {
            gap += 40;
        }
        return gap + w1 % 3 + h2 % 2;
    }

    private static int styleEdge(int position, int h) {
        int gap;
        switch (position) {
            case 7:
                gap = 7;
                break;
            case 1:
                gap = 9;
                break;
            case 3:
                gap = 4;
                break;
            case 5:
                gap = 13;
                break;
            default:
                gap = 60;
                break;
        }
        return gap + h % 2;
    }

    // ------------------------------------------------------------------
    // The JDK side

    private static final class RealStyle extends javax.swing.LayoutStyle {
        @Override
        public int getPreferredGap(javax.swing.JComponent a, javax.swing.JComponent b,
                javax.swing.LayoutStyle.ComponentPlacement type, int position, java.awt.Container parent) {
            return styleGap(type.ordinal(), position, a.getPreferredSize().width, b.getPreferredSize().height);
        }

        @Override
        public int getContainerGap(javax.swing.JComponent c, int position, java.awt.Container parent) {
            return styleEdge(position, c.getPreferredSize().height);
        }
    }

    private static final class RealLeaf extends javax.swing.JComponent {
        private static final long serialVersionUID = 1L;
        private final int baseline;

        RealLeaf(int baseline) {
            this.baseline = baseline;
        }

        @Override
        public int getBaseline(int width, int height) {
            return baseline;
        }
    }

    private static final class Real {
        javax.swing.JComponent host;
        javax.swing.GroupLayout layout;
        java.awt.Component[] comps;
    }

    private static javax.swing.GroupLayout.Alignment realAlign(int a) {
        return javax.swing.GroupLayout.Alignment.values()[a];
    }

    private static javax.swing.LayoutStyle.ComponentPlacement realType(int t) {
        return javax.swing.LayoutStyle.ComponentPlacement.values()[t];
    }

    private static javax.swing.GroupLayout.Group real(Real r, S s) {
        javax.swing.GroupLayout l = r.layout;
        if (s.kind == SEQ) {
            javax.swing.GroupLayout.SequentialGroup g = l.createSequentialGroup();
            for (S k : s.kids) {
                switch (k.kind) {
                    case COMP:
                        if (k.asBaseline) {
                            if (k.min == D && k.pref == D && k.max == D) {
                                g.addComponent(true, r.comps[k.comp]);
                            } else {
                                g.addComponent(true, r.comps[k.comp], k.min, k.pref, k.max);
                            }
                        } else if (k.min == D && k.pref == D && k.max == D) {
                            g.addComponent(r.comps[k.comp]);
                        } else {
                            g.addComponent(r.comps[k.comp], k.min, k.pref, k.max);
                        }
                        break;
                    case GAP:
                        if (k.min == k.pref && k.pref == k.max) {
                            g.addGap(k.pref);
                        } else {
                            g.addGap(k.min, k.pref, k.max);
                        }
                        break;
                    case AUTO_GAP:
                        if (k.pref == D && k.max == D) {
                            g.addPreferredGap(realType(k.type));
                        } else {
                            g.addPreferredGap(realType(k.type), k.pref, k.max);
                        }
                        break;
                    case PAIR_GAP:
                        if (k.pref == D && k.max == P) {
                            g.addPreferredGap((javax.swing.JComponent) r.comps[k.comp],
                                    (javax.swing.JComponent) r.comps[k.comp2], realType(k.type));
                        } else {
                            g.addPreferredGap((javax.swing.JComponent) r.comps[k.comp],
                                    (javax.swing.JComponent) r.comps[k.comp2], realType(k.type), k.pref, k.max);
                        }
                        break;
                    case EDGE_GAP:
                        if (k.pref == D && k.max == D) {
                            g.addContainerGap();
                        } else {
                            g.addContainerGap(k.pref, k.max);
                        }
                        break;
                    default:
                        if (k.asBaseline) {
                            g.addGroup(true, real(r, k));
                        } else {
                            g.addGroup(real(r, k));
                        }
                        break;
                }
            }
            return g;
        }
        javax.swing.GroupLayout.ParallelGroup g;
        if (s.anchor != 0) {
            g = l.createBaselineGroup(s.resizable, s.anchor == 1);
        } else if (s.resizable && s.align == LEADING) {
            g = l.createParallelGroup();
        } else if (s.resizable) {
            g = l.createParallelGroup(realAlign(s.align));
        } else {
            g = l.createParallelGroup(realAlign(s.align), false);
        }
        for (S k : s.kids) {
            switch (k.kind) {
                case COMP:
                    if (k.childAlign >= 0) {
                        if (k.min == D && k.pref == D && k.max == D) {
                            g.addComponent(r.comps[k.comp], realAlign(k.childAlign));
                        } else {
                            g.addComponent(r.comps[k.comp], realAlign(k.childAlign), k.min, k.pref, k.max);
                        }
                    } else if (k.min == D && k.pref == D && k.max == D) {
                        g.addComponent(r.comps[k.comp]);
                    } else {
                        g.addComponent(r.comps[k.comp], k.min, k.pref, k.max);
                    }
                    break;
                case GAP:
                    if (k.min == k.pref && k.pref == k.max) {
                        g.addGap(k.pref);
                    } else {
                        g.addGap(k.min, k.pref, k.max);
                    }
                    break;
                default:
                    if (k.childAlign >= 0) {
                        g.addGroup(realAlign(k.childAlign), real(r, k));
                    } else {
                        g.addGroup(real(r, k));
                    }
                    break;
            }
        }
        return g;
    }

    private static Real real(Spec spec) {
        Real r = new Real();
        r.host = new javax.swing.JComponent() {
            private static final long serialVersionUID = 1L;
        };
        if (spec.border != null) {
            r.host.setBorder(javax.swing.BorderFactory.createEmptyBorder(spec.border[0], spec.border[1],
                    spec.border[2], spec.border[3]));
        }
        r.layout = new javax.swing.GroupLayout(r.host);
        r.host.setLayout(r.layout);
        r.layout.setLayoutStyle(new RealStyle());
        r.comps = new java.awt.Component[spec.comps.size()];
        for (int i = 0; i < r.comps.length; i++) {
            C c = spec.comps.get(i);
            java.awt.Component k = c.plain ? new java.awt.Component() {
                private static final long serialVersionUID = 1L;
            } : new RealLeaf(c.baseline);
            k.setPreferredSize(new java.awt.Dimension(c.pw, c.ph));
            k.setMinimumSize(new java.awt.Dimension(c.minw, c.minh));
            if (c.maxw >= 0) {
                k.setMaximumSize(new java.awt.Dimension(c.maxw, c.maxh));
            }
            k.setVisible(!c.hidden);
            r.comps[i] = k;
        }
        r.layout.setAutoCreateGaps(spec.autoGaps);
        r.layout.setAutoCreateContainerGaps(spec.autoContainerGaps);
        r.layout.setHonorsVisibility(spec.honorsVisibility);
        r.layout.setHorizontalGroup(real(r, spec.h));
        r.layout.setVerticalGroup(real(r, spec.v));
        for (int i = 0; i < r.comps.length; i++) {
            int honors = spec.comps.get(i).honors;
            if (honors != 0) {
                r.layout.setHonorsVisibility(r.comps[i], honors == 1 ? Boolean.TRUE : Boolean.FALSE);
            }
        }
        for (int[] link : spec.links) {
            java.awt.Component[] set = new java.awt.Component[link.length - 1];
            for (int i = 0; i < set.length; i++) {
                set[i] = r.comps[link[i + 1]];
            }
            if (link[0] == 2) {
                r.layout.linkSize(set);
            } else {
                r.layout.linkSize(link[0], set);
            }
        }
        return r;
    }

    private static String dump(Real r) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < r.comps.length; i++) {
            java.awt.Component c = r.comps[i];
            sb.append(i).append(' ').append(c.getX()).append(',').append(c.getY()).append(' ').append(c.getWidth())
                    .append('x').append(c.getHeight()).append('\n');
        }
        java.awt.Dimension p = r.layout.preferredLayoutSize(r.host);
        java.awt.Dimension m = r.layout.minimumLayoutSize(r.host);
        java.awt.Dimension x = r.layout.maximumLayoutSize(r.host);
        sb.append("pref=").append(p.width).append('x').append(p.height).append(" min=").append(m.width).append('x')
                .append(m.height).append(" max=").append(x.width).append('x').append(x.height).append('\n');
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // This layer's side

    private static final class OurStyle extends LayoutStyle {
        @Override
        public int getPreferredGap(JComponent a, JComponent b, LayoutStyle.ComponentPlacement type, int position,
                Container parent) {
            return styleGap(type.ordinal(), position, a.getPreferredSize().width, b.getPreferredSize().height);
        }

        @Override
        public int getContainerGap(JComponent c, int position, Container parent) {
            return styleEdge(position, c.getPreferredSize().height);
        }
    }

    private static final class OurLeaf extends JComponent {
        private final int baseline;

        OurLeaf(int baseline) {
            this.baseline = baseline;
        }

        @Override
        public int getBaseline(int width, int height) {
            return baseline;
        }
    }

    private static final class Ours {
        JComponent host;
        GroupLayout layout;
        Component[] comps;
    }

    private static GroupLayout.Alignment ourAlign(int a) {
        return GroupLayout.Alignment.values()[a];
    }

    private static LayoutStyle.ComponentPlacement ourType(int t) {
        return LayoutStyle.ComponentPlacement.values()[t];
    }

    private static GroupLayout.Group ours(Ours r, S s) {
        GroupLayout l = r.layout;
        if (s.kind == SEQ) {
            GroupLayout.SequentialGroup g = l.createSequentialGroup();
            for (S k : s.kids) {
                switch (k.kind) {
                    case COMP:
                        if (k.asBaseline) {
                            if (k.min == D && k.pref == D && k.max == D) {
                                g.addComponent(true, r.comps[k.comp]);
                            } else {
                                g.addComponent(true, r.comps[k.comp], k.min, k.pref, k.max);
                            }
                        } else if (k.min == D && k.pref == D && k.max == D) {
                            g.addComponent(r.comps[k.comp]);
                        } else {
                            g.addComponent(r.comps[k.comp], k.min, k.pref, k.max);
                        }
                        break;
                    case GAP:
                        if (k.min == k.pref && k.pref == k.max) {
                            g.addGap(k.pref);
                        } else {
                            g.addGap(k.min, k.pref, k.max);
                        }
                        break;
                    case AUTO_GAP:
                        if (k.pref == D && k.max == D) {
                            g.addPreferredGap(ourType(k.type));
                        } else {
                            g.addPreferredGap(ourType(k.type), k.pref, k.max);
                        }
                        break;
                    case PAIR_GAP:
                        if (k.pref == D && k.max == P) {
                            g.addPreferredGap((JComponent) r.comps[k.comp], (JComponent) r.comps[k.comp2],
                                    ourType(k.type));
                        } else {
                            g.addPreferredGap((JComponent) r.comps[k.comp], (JComponent) r.comps[k.comp2],
                                    ourType(k.type), k.pref, k.max);
                        }
                        break;
                    case EDGE_GAP:
                        if (k.pref == D && k.max == D) {
                            g.addContainerGap();
                        } else {
                            g.addContainerGap(k.pref, k.max);
                        }
                        break;
                    default:
                        if (k.asBaseline) {
                            g.addGroup(true, ours(r, k));
                        } else {
                            g.addGroup(ours(r, k));
                        }
                        break;
                }
            }
            return g;
        }
        GroupLayout.ParallelGroup g;
        if (s.anchor != 0) {
            g = l.createBaselineGroup(s.resizable, s.anchor == 1);
        } else if (s.resizable && s.align == LEADING) {
            g = l.createParallelGroup();
        } else if (s.resizable) {
            g = l.createParallelGroup(ourAlign(s.align));
        } else {
            g = l.createParallelGroup(ourAlign(s.align), false);
        }
        for (S k : s.kids) {
            switch (k.kind) {
                case COMP:
                    if (k.childAlign >= 0) {
                        if (k.min == D && k.pref == D && k.max == D) {
                            g.addComponent(r.comps[k.comp], ourAlign(k.childAlign));
                        } else {
                            g.addComponent(r.comps[k.comp], ourAlign(k.childAlign), k.min, k.pref, k.max);
                        }
                    } else if (k.min == D && k.pref == D && k.max == D) {
                        g.addComponent(r.comps[k.comp]);
                    } else {
                        g.addComponent(r.comps[k.comp], k.min, k.pref, k.max);
                    }
                    break;
                case GAP:
                    if (k.min == k.pref && k.pref == k.max) {
                        g.addGap(k.pref);
                    } else {
                        g.addGap(k.min, k.pref, k.max);
                    }
                    break;
                default:
                    if (k.childAlign >= 0) {
                        g.addGroup(ourAlign(k.childAlign), ours(r, k));
                    } else {
                        g.addGroup(ours(r, k));
                    }
                    break;
            }
        }
        return g;
    }

    private static Ours ours(Spec spec) {
        Ours r = new Ours();
        r.host = new JComponent() {
        };
        if (spec.border != null) {
            r.host.setBorder(new EmptyBorder(spec.border[0], spec.border[1], spec.border[2], spec.border[3]));
        }
        r.layout = new GroupLayout(r.host);
        r.host.setLayout(r.layout);
        r.layout.setLayoutStyle(new OurStyle());
        r.comps = new Component[spec.comps.size()];
        for (int i = 0; i < r.comps.length; i++) {
            C c = spec.comps.get(i);
            Component k = c.plain ? new Component() {
            } : new OurLeaf(c.baseline);
            k.setPreferredSize(new Dimension(c.pw, c.ph));
            k.setMinimumSize(new Dimension(c.minw, c.minh));
            if (c.maxw >= 0) {
                k.setMaximumSize(new Dimension(c.maxw, c.maxh));
            }
            k.setVisible(!c.hidden);
            r.comps[i] = k;
        }
        r.layout.setAutoCreateGaps(spec.autoGaps);
        r.layout.setAutoCreateContainerGaps(spec.autoContainerGaps);
        r.layout.setHonorsVisibility(spec.honorsVisibility);
        r.layout.setHorizontalGroup(ours(r, spec.h));
        r.layout.setVerticalGroup(ours(r, spec.v));
        for (int i = 0; i < r.comps.length; i++) {
            int honors = spec.comps.get(i).honors;
            if (honors != 0) {
                r.layout.setHonorsVisibility(r.comps[i], honors == 1 ? Boolean.TRUE : Boolean.FALSE);
            }
        }
        for (int[] link : spec.links) {
            Component[] set = new Component[link.length - 1];
            for (int i = 0; i < set.length; i++) {
                set[i] = r.comps[link[i + 1]];
            }
            if (link[0] == 2) {
                r.layout.linkSize(set);
            } else {
                r.layout.linkSize(link[0], set);
            }
        }
        return r;
    }

    private static String dump(Ours r) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < r.comps.length; i++) {
            Component c = r.comps[i];
            sb.append(i).append(' ').append(c.getX()).append(',').append(c.getY()).append(' ').append(c.getWidth())
                    .append('x').append(c.getHeight()).append('\n');
        }
        Dimension p = r.layout.preferredLayoutSize(r.host);
        Dimension m = r.layout.minimumLayoutSize(r.host);
        Dimension x = r.layout.maximumLayoutSize(r.host);
        sb.append("pref=").append(p.width).append('x').append(p.height).append(" min=").append(m.width).append('x')
                .append(m.height).append(" max=").append(x.width).append('x').append(x.height).append('\n');
        return sb.toString();
    }

    // ------------------------------------------------------------------

    // How many steps the JDK laid out without throwing.
    private static int laidOut;

    /// Runs one step on each side and requires the same outcome, a dump or
    /// the class of what was thrown.
    private static void same(String what, Spec spec, Real r, Ours o, int w, int h) {
        String expected;
        try {
            r.host.setSize(w, h);
            r.host.invalidate();
            r.host.doLayout();
            expected = dump(r);
            laidOut++;
        } catch (RuntimeException e) {
            expected = "threw " + e.getClass().getName();
        }
        String actual;
        try {
            o.host.setSize(w, h);
            o.host.invalidate();
            o.host.doLayout();
            actual = dump(o);
        } catch (RuntimeException e) {
            actual = "threw " + e.getClass().getName() + " " + e.getMessage();
            if (expected.startsWith("threw ") && actual.startsWith(expected)) {
                actual = expected;
            }
        }
        if (!expected.equals(actual)) {
            assertEquals(what + " at " + w + "x" + h + "\n" + spec.describe(), expected, actual);
        }
    }

    /// Builds the layout from both libraries and lays it out at its
    /// preferred size, around it, at nothing and at the sizes given.
    private static void check(String what, Spec spec, int... sizes) {
        Real r;
        String thrown = null;
        try {
            r = real(spec);
        } catch (RuntimeException e) {
            r = null;
            thrown = e.getClass().getName();
        }
        Ours o;
        try {
            o = ours(spec);
            if (thrown != null) {
                fail(what + ": the JDK threw " + thrown + " building this, the layer did not\n" + spec.describe());
            }
        } catch (RuntimeException e) {
            if (thrown == null) {
                throw new AssertionError(what + ": only the layer threw building this\n" + spec.describe(), e);
            }
            assertEquals(what + "\n" + spec.describe(), thrown, e.getClass().getName());
            return;
        }
        java.awt.Dimension p;
        try {
            p = r.layout.preferredLayoutSize(r.host);
        } catch (RuntimeException e) {
            p = new java.awt.Dimension(100, 100);
        }
        same(what, spec, r, o, p.width, p.height);
        same(what, spec, r, o, p.width + 57, p.height + 31);
        same(what, spec, r, o, p.width - 13, p.height - 7);
        same(what, spec, r, o, p.width / 2, p.height / 2);
        same(what, spec, r, o, 0, 0);
        same(what, spec, r, o, 1200, 900);
        same(what, spec, r, o, p.width, p.height);
        for (int i = 0; i < sizes.length; i += 2) {
            same(what, spec, r, o, sizes[i], sizes[i + 1]);
        }
    }

    @After
    public void restoreSharedStyle() {
        LayoutStyle.setInstance(null);
    }

    // ------------------------------------------------------------------

    @Test
    public void sequentialAndParallelWithExplicitGaps() {
        Spec s = new Spec();
        int a = s.add(comp(40, 20));
        int b = s.add(comp(70, 35).min(20, 10).max(300, 400));
        int d = s.add(comp(55, 28).min(11, 7));
        s.h = seq(c(a), gap(8), c(b), gap(4, 10, 30), c(d));
        s.v = par(LEADING, c(a), c(b), c(d));
        check("row", s, 400, 100);

        s = new Spec();
        a = s.add(comp(40, 20));
        b = s.add(comp(70, 35).min(20, 10).max(300, 400));
        d = s.add(comp(55, 28).min(11, 7).plain());
        s.border = new int[] {3, 5, 7, 9};
        s.h = par(LEADING, c(a), c(b), c(d));
        s.v = seq(gap(6), c(a), gap(0, 5, Short.MAX_VALUE), c(b), gap(2, 9, P), c(d), gap(P, 3, 3));
        check("column with insets", s, 200, 300);
    }

    @Test
    public void everyAlignment() {
        for (int group = LEADING; group <= CENTER; group++) {
            for (int resizable = 0; resizable < 2; resizable++) {
                Spec s = new Spec();
                int a = s.add(comp(40, 20));
                int b = s.add(comp(70, 35).max(90, 50));
                int d = s.add(comp(55, 28).min(11, 7).max(55, 28));
                int e = s.add(comp(30, 12).min(30, 12).max(30, 12));
                int f = s.add(comp(31, 13).max(31, 13));
                S hp = par(group, c(a), c(b).al(TRAILING), c(d).al(CENTER), c(e).al(LEADING), c(f));
                S vp = par(group, c(a).al(CENTER), c(b), c(d).al(TRAILING), c(e), c(f).al(LEADING));
                if (resizable == 0) {
                    hp.fixed();
                    vp.fixed();
                }
                s.h = seq(gap(3), hp);
                s.v = seq(vp, gap(3));
                check("alignment " + group + " resizable " + resizable, s, 150, 150);
            }
        }
    }

    @Test
    public void baselineGroups() {
        for (int variant = 0; variant < 6; variant++) {
            Spec s = new Spec();
            int a = s.add(comp(40, 20).base(14));
            int b = s.add(comp(70, 35).base(30).max(90, 60));
            int d = s.add(comp(55, 28).min(11, 7));
            int e = s.add(comp(30, 44).base(9).min(30, 10));
            s.h = seq(c(a), c(b), c(d), c(e));
            S row;
            if (variant == 0) {
                row = par(BASELINE, c(a), c(b), c(d), c(e));
            } else if (variant == 1) {
                row = par(BASELINE, c(a), c(b), c(d), c(e)).fixed();
            } else if (variant == 2) {
                row = base(true, c(a), c(b).al(BASELINE), c(d).al(CENTER), c(e).al(TRAILING));
            } else if (variant == 3) {
                row = base(false, c(a), c(b), c(d), c(e));
            } else if (variant == 4) {
                row = base(false, c(a), c(b).al(LEADING), c(e)).fixed();
                s.h = seq(c(a), c(b), c(e), c(d));
                s.v = seq(row, c(d));
                check("baseline variant " + variant, s);
                continue;
            } else {
                // A baseline group of sequences, each naming its baseline
                // element.
                row = par(BASELINE, seq(gap(4), c(a).bl(), gap(2, 6, 20)), seq(c(d), c(b).bl()), c(e));
            }
            s.v = seq(gap(2), row, gap(0, 3, 50));
            check("baseline variant " + variant, s, 300, 200);
        }
    }

    @Test
    public void baselineAlongTheHorizontalAxisIsRejected() {
        Spec s = new Spec();
        int a = s.add(comp(40, 20).base(14));
        int b = s.add(comp(70, 35).base(30));
        s.h = par(BASELINE, c(a), c(b));
        s.v = seq(c(a), c(b));
        check("horizontal baseline", s);
    }

    @Test
    public void preferredGaps() {
        for (int autoGaps = 0; autoGaps < 2; autoGaps++) {
            for (int autoEdges = 0; autoEdges < 2; autoEdges++) {
                Spec s = new Spec();
                s.autoGaps = autoGaps == 1;
                s.autoContainerGaps = autoEdges == 1;
                int a = s.add(comp(40, 20));
                int b = s.add(comp(71, 35).min(20, 10).max(300, 400));
                int d = s.add(comp(55, 28).min(11, 7));
                int e = s.add(comp(30, 13));
                int f = s.add(comp(64, 22).plain());
                s.h = seq(c(a), auto(RELATED), c(b), auto(UNRELATED), c(d), c(e), auto(RELATED, 20, 40), c(f));
                s.v = par(LEADING, c(a), c(b), c(d), c(e), c(f));
                check("auto gaps " + autoGaps + " edges " + autoEdges, s, 500, 80);

                s = new Spec();
                s.autoGaps = autoGaps == 1;
                s.autoContainerGaps = autoEdges == 1;
                a = s.add(comp(40, 20));
                b = s.add(comp(71, 35).min(20, 10).max(300, 400));
                d = s.add(comp(55, 28).min(11, 7));
                e = s.add(comp(30, 13));
                f = s.add(comp(64, 22).plain());
                s.border = new int[] {1, 2, 3, 4};
                s.h = seq(edge(), par(LEADING, c(a), c(b), seq(c(d), c(e), auto(UNRELATED, P, 90), c(f))),
                        edge(20, 30));
                s.v = seq(edge(2, P), c(a), pair(a, b, UNRELATED), c(b), pair(b, d, INDENT, 30, 60),
                        par(TRAILING, c(d), c(e), c(f)), edge());
                check("form with gaps " + autoGaps + " edges " + autoEdges, s, 400, 300);
            }
        }
    }

    @Test
    public void containerGapsInTheMiddleAndAround() {
        Spec s = new Spec();
        s.autoContainerGaps = true;
        s.autoGaps = true;
        int a = s.add(comp(40, 21));
        int b = s.add(comp(71, 35).plain());
        int d = s.add(comp(55, 28));
        s.h = par(CENTER, c(a), seq(c(b), c(d)));
        s.v = seq(c(a), par(LEADING, c(b), c(d)));
        check("auto everything", s, 300, 200);

        s = new Spec();
        a = s.add(comp(40, 21));
        b = s.add(comp(71, 35));
        s.h = seq(c(a), edge(), c(b), edge());
        s.v = seq(edge(), par(LEADING, c(a), c(b)));
        check("edge gap between components", s, 300, 200);
    }

    @Test
    public void linkedSizes() {
        for (int axis = 0; axis <= 2; axis++) {
            Spec s = new Spec();
            int a = s.add(comp(40, 20).min(5, 5));
            int b = s.add(comp(70, 35).min(20, 10).max(300, 400));
            int d = s.add(comp(55, 28).min(11, 7));
            int e = s.add(comp(90, 12));
            s.h = seq(c(a), gap(5), c(b), gap(5), c(d), gap(1, 5, 100), c(e));
            s.v = par(CENTER, c(a), c(b), c(d), c(e));
            s.link(axis, a, b, d);
            check("link axis " + axis, s, 500, 100);

            s = new Spec();
            a = s.add(comp(40, 20).min(5, 5));
            b = s.add(comp(70, 35).hide());
            d = s.add(comp(55, 28).min(11, 7));
            e = s.add(comp(90, 12).plain());
            s.autoGaps = true;
            s.h = seq(c(a), c(b), c(d), c(e));
            s.v = par(CENTER, c(a), c(b), c(d), c(e));
            s.link(axis, a, b).link(axis, d, e).link(axis, b, e);
            check("merged links with a hidden member, axis " + axis, s, 500, 100);
        }
    }

    @Test
    public void invisibleComponents() {
        for (int honors = 0; honors < 2; honors++) {
            for (int autoGaps = 0; autoGaps < 2; autoGaps++) {
                Spec s = new Spec();
                s.honorsVisibility = honors == 1;
                s.autoGaps = autoGaps == 1;
                s.autoContainerGaps = autoGaps == 1;
                int a = s.add(comp(40, 20));
                int b = s.add(comp(70, 35).hide());
                int d = s.add(comp(55, 28).hide().honors(false));
                int e = s.add(comp(90, 12).hide().honors(true));
                int f = s.add(comp(33, 33));
                s.h = seq(c(a), gap(5), c(b), auto(RELATED), c(d), c(e), auto(UNRELATED), c(f));
                s.v = seq(par(LEADING, c(a), c(b), c(d)), par(TRAILING, c(e), c(f)));
                check("invisible, honors " + honors + " gaps " + autoGaps, s, 400, 200);
            }
        }
    }

    @Test
    public void nestedGroups() {
        Spec s = new Spec();
        s.autoGaps = true;
        s.autoContainerGaps = true;
        s.border = new int[] {4, 4, 4, 4};
        int l1 = s.add(comp(60, 16));
        int l2 = s.add(comp(85, 16));
        int l3 = s.add(comp(45, 16));
        int f1 = s.add(comp(120, 22).min(40, 22).max(Short.MAX_VALUE, 22));
        int f2 = s.add(comp(120, 22).min(40, 22).max(Short.MAX_VALUE, 22));
        int f3 = s.add(comp(120, 80).min(40, 30).max(Short.MAX_VALUE, Short.MAX_VALUE));
        int ok = s.add(comp(70, 26));
        int cancel = s.add(comp(90, 26));
        s.h = par(LEADING,
                seq(par(TRAILING, c(l1), c(l2), c(l3)), par(LEADING, c(f1), c(f2), c(f3))),
                seq(gap(0, 0, Short.MAX_VALUE), c(ok), c(cancel)).al(TRAILING));
        s.v = seq(par(CENTER, c(l1), c(f1)), par(CENTER, c(l2), c(f2)), par(LEADING, c(l3), c(f3)),
                auto(UNRELATED), par(LEADING, c(ok), c(cancel)));
        s.link(0, ok, cancel);
        check("form", s, 640, 480, 150, 120, 320, 240);
    }

    @Test
    public void explicitComponentSizes() {
        Spec s = new Spec();
        int a = s.add(comp(40, 20).min(10, 10).max(200, 200));
        int b = s.add(comp(70, 35).min(20, 10).max(300, 400));
        int d = s.add(comp(55, 28).min(11, 7));
        s.h = seq(c(a, P, D, P), c(b, 0, 50, Short.MAX_VALUE), c(d, P, 80, 120));
        s.v = par(LEADING, c(a, 5, 25, 25), c(b, D, P, P), c(d, D, D, P).al(TRAILING));
        check("explicit sizes", s, 600, 200, 100, 40);
    }

    @Test
    public void invalidArgumentsAreRejectedAlike() {
        int[][] gaps = {{-3, 5, 5}, {5, -1, 5}, {5, 4, 9}, {1, 5, 3}, {P, 5, P}, {D, 5, 5}, {5, 5, D}};
        for (int[] g : gaps) {
            Spec s = new Spec();
            int a = s.add(comp(40, 20));
            s.h = seq(c(a), gap(g[0], g[1], g[2]));
            s.v = seq(c(a));
            check("gap " + g[0] + "/" + g[1] + "/" + g[2], s);
        }
        int[][] sizes = {{-3, D, D}, {5, 4, D}, {D, 9, 3}, {D, D, -5}, {P, P, P}};
        for (int[] g : sizes) {
            Spec s = new Spec();
            int a = s.add(comp(40, 20));
            s.h = seq(c(a, g[0], g[1], g[2]));
            s.v = seq(c(a));
            check("component " + g[0] + "/" + g[1] + "/" + g[2], s);
        }
        int[][] prefGaps = {{-3, D}, {9, 3}, {P, P}, {D, -7}, {4, P}};
        for (int[] g : prefGaps) {
            Spec s = new Spec();
            int a = s.add(comp(40, 20));
            int b = s.add(comp(40, 20));
            s.h = seq(c(a), auto(RELATED, g[0], g[1]), c(b));
            s.v = par(LEADING, c(a), c(b));
            check("auto gap " + g[0] + "/" + g[1], s);
            s = new Spec();
            a = s.add(comp(40, 20));
            b = s.add(comp(40, 20));
            s.h = seq(c(a), pair(a, b, RELATED, g[0], g[1]), c(b));
            s.v = par(LEADING, c(a), c(b));
            check("pair gap " + g[0] + "/" + g[1], s);
            s = new Spec();
            a = s.add(comp(40, 20));
            b = s.add(comp(40, 20));
            s.h = seq(edge(g[0], g[1]), c(a), c(b));
            s.v = par(LEADING, c(a), c(b));
            check("edge gap " + g[0] + "/" + g[1], s);
        }
        // INDENT is only a gap between two named components.
        Spec s = new Spec();
        int a = s.add(comp(40, 20));
        s.h = seq(c(a), auto(INDENT));
        s.v = seq(c(a));
        check("auto indent", s);
        // A component in one axis only.
        s = new Spec();
        a = s.add(comp(40, 20));
        int b = s.add(comp(40, 20));
        s.h = seq(c(a), c(b));
        s.v = seq(c(a));
        check("missing from the vertical group", s);
        // BASELINE as a child alignment of an ordinary parallel group.
        s = new Spec();
        a = s.add(comp(40, 20));
        s.h = seq(c(a));
        s.v = par(LEADING, c(a).al(BASELINE));
        check("baseline child of a plain group", s);
    }

    // ------------------------------------------------------------------
    // Random layouts

    private static S randomTree(Random rnd, Spec spec, List<Integer> items, boolean vertical, int depth) {
        if (items.size() == 1 && (depth > 0 || rnd.nextInt(3) == 0)) {
            S leaf = c(items.get(0));
            int kind = rnd.nextInt(8);
            C comp = spec.comps.get(items.get(0));
            if (kind == 0) {
                leaf.min = P;
                leaf.max = P;
            } else if (kind == 1) {
                leaf.max = Short.MAX_VALUE;
            } else if (kind == 2) {
                leaf.min = 0;
                leaf.pref = (vertical ? comp.ph : comp.pw) + rnd.nextInt(9);
            }
            return leaf;
        }
        boolean sequential = rnd.nextBoolean();
        int parts = items.size() == 1 ? 1 : 2 + rnd.nextInt(Math.min(3, items.size() - 1));
        List<List<Integer>> chunks = new ArrayList<List<Integer>>();
        for (int i = 0; i < parts; i++) {
            chunks.add(new ArrayList<Integer>());
        }
        for (int i = 0; i < items.size(); i++) {
            // Every chunk gets one, the rest land anywhere.
            chunks.get(i < parts ? i : rnd.nextInt(parts)).add(items.get(i));
        }
        if (sequential) {
            S s = seq();
            if (rnd.nextInt(6) == 0) {
                s.kids.add(edge());
            }
            for (int i = 0; i < parts; i++) {
                if (i > 0) {
                    int kind = rnd.nextInt(9);
                    if (kind == 0) {
                        s.kids.add(gap(rnd.nextInt(12)));
                    } else if (kind == 1) {
                        s.kids.add(gap(rnd.nextInt(4), 4 + rnd.nextInt(8), 12 + rnd.nextInt(40)));
                    } else if (kind == 2) {
                        s.kids.add(auto(rnd.nextInt(2)));
                    } else if (kind == 3) {
                        s.kids.add(auto(rnd.nextInt(2), rnd.nextInt(20), 20 + rnd.nextInt(60)));
                    } else if (kind == 4) {
                        s.kids.add(gap(0, rnd.nextInt(6), Short.MAX_VALUE));
                    } else if (kind == 5) {
                        int a = chunks.get(i - 1).get(0);
                        int b = chunks.get(i).get(0);
                        if (!spec.comps.get(a).plain && !spec.comps.get(b).plain) {
                            s.kids.add(pair(a, b, rnd.nextInt(3)));
                        }
                    }
                }
                S kid = randomTree(rnd, spec, chunks.get(i), vertical, depth + 1);
                if (vertical && rnd.nextInt(4) == 0) {
                    kid.bl();
                }
                s.kids.add(kid);
            }
            if (rnd.nextInt(6) == 0) {
                s.kids.add(rnd.nextBoolean() ? edge() : edge(rnd.nextInt(10), 10 + rnd.nextInt(30)));
            }
            return s;
        }
        S s;
        boolean baseline = vertical && rnd.nextInt(3) == 0;
        if (baseline) {
            int how = rnd.nextInt(3);
            s = how == 0 ? par(BASELINE) : base(how == 1);
        } else {
            s = par(rnd.nextInt(3));
        }
        if (rnd.nextInt(4) == 0) {
            s.fixed();
        }
        for (int i = 0; i < parts; i++) {
            S kid = randomTree(rnd, spec, chunks.get(i), vertical, depth + 1);
            if (rnd.nextInt(3) == 0) {
                kid.al(rnd.nextInt(baseline ? 4 : 3));
            }
            s.kids.add(kid);
        }
        if (rnd.nextInt(8) == 0) {
            s.kids.add(gap(rnd.nextInt(60)));
        }
        return s;
    }

    private static Spec randomSpec(Random rnd) {
        Spec spec = new Spec();
        int count = 1 + rnd.nextInt(7);
        List<Integer> items = new ArrayList<Integer>();
        for (int i = 0; i < count; i++) {
            int w = 5 + rnd.nextInt(90);
            int h = 5 + rnd.nextInt(60);
            C c = comp(w, h);
            if (rnd.nextInt(2) == 0) {
                c.min(rnd.nextInt(w + 1), rnd.nextInt(h + 1));
            }
            if (rnd.nextInt(2) == 0) {
                c.max(w + rnd.nextInt(200), h + rnd.nextInt(200));
            }
            if (rnd.nextInt(6) == 0) {
                c.plain();
            } else if (rnd.nextInt(2) == 0) {
                c.base(rnd.nextInt(h + 1));
            }
            if (rnd.nextInt(9) == 0) {
                c.hide();
            }
            if (rnd.nextInt(12) == 0) {
                c.honors(rnd.nextBoolean());
            }
            items.add(spec.add(c));
        }
        spec.autoGaps = rnd.nextInt(3) == 0;
        spec.autoContainerGaps = rnd.nextInt(3) == 0;
        spec.honorsVisibility = rnd.nextInt(5) != 0;
        if (rnd.nextInt(4) == 0) {
            spec.border = new int[] {rnd.nextInt(8), rnd.nextInt(8), rnd.nextInt(8), rnd.nextInt(8)};
        }
        Collections.shuffle(items, rnd);
        spec.h = randomTree(rnd, spec, items, false, 0);
        Collections.shuffle(items, rnd);
        spec.v = randomTree(rnd, spec, items, true, 0);
        if (count > 1 && rnd.nextInt(4) == 0) {
            int n = 2 + rnd.nextInt(count - 1);
            int[] which = new int[n];
            for (int i = 0; i < n; i++) {
                which[i] = items.get(i);
            }
            spec.link(rnd.nextInt(3), which);
        }
        return spec;
    }

    @Test
    public void randomLayouts() {
        Random rnd = new Random(77120931L);
        int before = laidOut;
        for (int round = 0; round < 600; round++) {
            Spec spec = randomSpec(rnd);
            check("random layout " + round, spec, rnd.nextInt(700), rnd.nextInt(500));
        }
        // Eight steps a layout; most of them must be real layouts rather
        // than two matching exceptions.
        assertTrue("only " + (laidOut - before) + " steps laid out", laidOut - before > 600 * 8 * 3 / 4);
    }

    // ------------------------------------------------------------------
    // Changes after the first layout, driven on both sides step by step

    @Test
    public void changesAfterLayout() {
        Spec s = new Spec();
        s.autoGaps = true;
        int a = s.add(comp(40, 20));
        int b = s.add(comp(70, 35).min(20, 10).max(300, 400));
        int d = s.add(comp(55, 28).min(11, 7));
        s.h = seq(c(a), c(b), c(d));
        s.v = par(CENTER, c(a), c(b), c(d));
        Real r = real(s);
        Ours o = ours(s);
        same("first", s, r, o, 300, 100);

        r.comps[1].setVisible(false);
        o.comps[1].setVisible(false);
        same("hidden", s, r, o, 300, 100);

        r.layout.setHonorsVisibility(false);
        o.layout.setHonorsVisibility(false);
        same("visibility ignored", s, r, o, 300, 100);

        r.layout.setHonorsVisibility(r.comps[1], Boolean.TRUE);
        o.layout.setHonorsVisibility(o.comps[1], Boolean.TRUE);
        same("honoured for one", s, r, o, 300, 100);

        r.comps[1].setVisible(true);
        o.comps[1].setVisible(true);
        r.layout.setAutoCreateContainerGaps(true);
        o.layout.setAutoCreateContainerGaps(true);
        same("container gaps on", s, r, o, 300, 100);

        java.awt.Component rn = new RealLeaf(-1);
        rn.setPreferredSize(new java.awt.Dimension(91, 17));
        Component on = new OurLeaf(-1);
        on.setPreferredSize(new Dimension(91, 17));
        r.layout.replace(r.comps[0], rn);
        o.layout.replace(o.comps[0], on);
        assertEquals(r.comps[0].getParent() == null, o.comps[0].getParent() == null);
        assertSame(o.host, on.getParent());
        r.comps[0] = rn;
        o.comps[0] = on;
        same("replaced", s, r, o, 300, 100);

        r.layout.linkSize(javax.swing.SwingConstants.VERTICAL, r.comps[0], r.comps[2]);
        o.layout.linkSize(SwingConstants.VERTICAL, o.comps[0], o.comps[2]);
        same("linked", s, r, o, 300, 100);

        r.host.remove(r.comps[2]);
        o.host.remove(o.comps[2]);
        same("removed", s, r, o, 300, 100);
        same("removed, smaller", s, r, o, 120, 30);

        r.layout.setAutoCreateContainerGaps(false);
        o.layout.setAutoCreateContainerGaps(false);
        r.layout.setLayoutStyle(null);
        o.layout.setLayoutStyle(null);
        javax.swing.LayoutStyle before = javax.swing.LayoutStyle.getInstance();
        try {
            javax.swing.LayoutStyle.setInstance(new RealStyle());
            LayoutStyle.setInstance(new OurStyle());
            same("shared style", s, r, o, 300, 100);
        } finally {
            javax.swing.LayoutStyle.setInstance(before);
        }
    }

    // ------------------------------------------------------------------
    // Expectations worked out by hand

    private static JComponent box(int w, int h) {
        JComponent c = new JComponent() {
        };
        c.setPreferredSize(new Dimension(w, h));
        c.setMinimumSize(new Dimension(w, h));
        c.setMaximumSize(new Dimension(w, h));
        return c;
    }

    private static void assertBounds(Component c, int x, int y, int w, int h) {
        assertEquals("x", x, c.getX());
        assertEquals("y", y, c.getY());
        assertEquals("width", w, c.getWidth());
        assertEquals("height", h, c.getHeight());
    }

    @Test
    public void defaultStyleGaps() {
        JPanel host = new JPanel();
        GroupLayout layout = new GroupLayout(host);
        host.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        JComponent a = box(40, 20);
        JComponent b = box(30, 10);
        JComponent d = box(50, 30);
        layout.setHorizontalGroup(layout.createSequentialGroup().addComponent(a).addComponent(b)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(d));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING).addComponent(a)
                .addComponent(b).addComponent(d));
        // 6 + 40 + 6 + 30 + 12 + 50 + 6 wide, 6 + 30 + 6 high
        assertEquals(new Dimension(150, 42), layout.preferredLayoutSize(host));
        assertEquals(new Dimension(150, 42), layout.minimumLayoutSize(host));
        host.setSize(150, 42);
        host.doLayout();
        assertBounds(a, 6, 16, 40, 20);
        assertBounds(b, 52, 26, 30, 10);
        assertBounds(d, 94, 6, 50, 30);
    }

    @Test
    public void defaultStyleMatchesTheJdkDefault() throws Exception {
        javax.swing.LayoutStyle real;
        try {
            real = (javax.swing.LayoutStyle) Class.forName("sun.swing.DefaultLayoutStyle").getMethod("getInstance")
                    .invoke(null);
        } catch (ClassNotFoundException e) {
            org.junit.Assume.assumeNoException(e);
            return;
        }
        LayoutStyle ours = LayoutStyle.getInstance();
        javax.swing.JComponent ra = new RealLeaf(-1);
        javax.swing.JComponent rb = new RealLeaf(-1);
        JComponent oa = new OurLeaf(-1);
        JComponent ob = new OurLeaf(-1);
        int[] positions = {1, 3, 5, 7};
        for (int position : positions) {
            for (int type = 0; type < 3; type++) {
                assertEquals("gap " + type + " at " + position,
                        real.getPreferredGap(ra, rb, realType(type), position, null),
                        ours.getPreferredGap(oa, ob, ourType(type), position, null));
            }
            assertEquals("edge at " + position, real.getContainerGap(ra, position, null),
                    ours.getContainerGap(oa, position, null));
        }
        try {
            ours.getContainerGap(oa, 2, null);
            fail("NORTH_EAST is not a position");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
        try {
            ours.getPreferredGap(oa, null, LayoutStyle.ComponentPlacement.RELATED, 1, null);
            fail("null component");
        } catch (NullPointerException expected) {
            assertTrue(true);
        }
        LayoutStyle custom = new OurStyle();
        LayoutStyle.setInstance(custom);
        assertSame(custom, LayoutStyle.getInstance());
        LayoutStyle.setInstance(null);
        assertSame(ours, LayoutStyle.getInstance());
    }

    @Test
    public void growingAndShrinkingByHand() {
        Container host = new Container();
        GroupLayout layout = new GroupLayout(host);
        host.setLayout(layout);
        Component fixed = box(40, 20);
        Component grows = new Component() {
        };
        grows.setPreferredSize(new Dimension(60, 20));
        grows.setMinimumSize(new Dimension(20, 20));
        grows.setMaximumSize(new Dimension(100, 20));
        layout.setHorizontalGroup(
                layout.createSequentialGroup().addComponent(fixed).addGap(10).addComponent(grows).addGap(0, 5, 15));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER).addComponent(fixed)
                .addComponent(grows, 10, 10, 10));
        assertEquals(new Dimension(115, 20), layout.preferredLayoutSize(host));
        assertEquals(new Dimension(70, 20), layout.minimumLayoutSize(host));
        assertEquals(new Dimension(165, 20), layout.maximumLayoutSize(host));
        assertEquals(2, host.getComponentCount());

        // 30 extra pixels: the gap can take 10 and takes its even share of
        // 15 first, capped at 10; the component takes the remaining 20.
        // The vertical group cannot grow past its 20, so it stays at the
        // top.
        host.setSize(145, 40);
        host.doLayout();
        assertBounds(fixed, 0, 0, 40, 20);
        assertBounds(grows, 50, 5, 80, 10);

        // 25 pixels short: the gap gives its 5, the component the other 20.
        host.setSize(90, 20);
        host.invalidate();
        host.doLayout();
        assertBounds(fixed, 0, 0, 40, 20);
        assertBounds(grows, 50, 5, 40, 10);
    }
}
