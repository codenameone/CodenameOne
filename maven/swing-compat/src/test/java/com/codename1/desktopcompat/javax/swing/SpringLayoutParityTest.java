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
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.javax.swing.border.EmptyBorder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// Runs the same sequence of spring and spring layout calls against the
/// JDK's `javax.swing.Spring` and `javax.swing.SpringLayout` and against
/// this layer's, and requires identical spring values, component bounds
/// and layout sizes.
///
/// A scenario is written once against [Side], which names components and
/// springs by number, and is played on both class libraries. The
/// components are bare ones with fixed sizes, so nothing depends on a look
/// and feel and the JDK side runs headless.
public class SpringLayoutParityTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final int HOST = -1;

    private static final String N = "North";
    private static final String S = "South";
    private static final String E = "East";
    private static final String W = "West";
    private static final String HC = "HorizontalCenter";
    private static final String VC = "VerticalCenter";
    private static final String BL = "Baseline";
    private static final String WD = "Width";
    private static final String HT = "Height";

    private static final String[] HORIZONTAL = {W, E, HC, WD};
    private static final String[] VERTICAL = {N, S, VC, HT};

    /// One class library, driven by numbers.
    private interface Side {
        void border(int top, int left, int bottom, int right);

        int comp(int pw, int ph, int minw, int minh, int maxw, int maxh, int baseline);

        void add(int comp);

        void addAsIs(int comp, int x, int y);

        void addWith(int comp, int x, int y, int w, int h);

        void addWithProperty(int comp, int x, int y);

        void remove(int comp);

        int constant(int pref);

        int constant(int min, int pref, int max);

        int sum(int a, int b);

        int max(int a, int b);

        int minus(int a);

        int scale(int a, float factor);

        int width(int comp);

        int height(int comp);

        int edge(String edge, int comp);

        int constraint(String edge, int comp);

        void put(String e1, int c1, int pad, String e2, int c2);

        void putSpring(String e1, int c1, int spring, String e2, int c2);

        void set(int comp, String edge, int spring);

        void setValue(int spring, int value);

        String spring(int spring);

        String layout(int w, int h);

        String sizes();
    }

    /// A scenario: calls on a side, and everything observed on the way.
    private interface Script {
        void run(Side side, StringBuilder out);
    }

    // ------------------------------------------------------------------
    // The JDK side

    private static final class RealLeaf extends javax.swing.JComponent {
        private static final long serialVersionUID = 1L;
        private final int baseline;

        RealLeaf(int baseline) {
            this.baseline = baseline;
        }

        @Override
        public int getBaseline(int width, int height) {
            return baseline < 0 ? -1 : Math.min(baseline, height);
        }
    }

    private static final class Real implements Side {
        final javax.swing.SpringLayout layout = new javax.swing.SpringLayout();
        final javax.swing.JComponent host = new javax.swing.JComponent() {
            private static final long serialVersionUID = 1L;
        };
        final List<javax.swing.JComponent> comps = new ArrayList<javax.swing.JComponent>();
        final List<javax.swing.Spring> springs = new ArrayList<javax.swing.Spring>();

        Real() {
            host.setLayout(layout);
        }

        private java.awt.Component c(int i) {
            return i == HOST ? host : comps.get(i);
        }

        private int s(javax.swing.Spring s) {
            if (s == null) {
                return -1;
            }
            springs.add(s);
            return springs.size() - 1;
        }

        public void border(int top, int left, int bottom, int right) {
            host.setBorder(javax.swing.BorderFactory.createEmptyBorder(top, left, bottom, right));
        }

        public int comp(int pw, int ph, int minw, int minh, int maxw, int maxh, int baseline) {
            javax.swing.JComponent k = new RealLeaf(baseline);
            k.setPreferredSize(new java.awt.Dimension(pw, ph));
            k.setMinimumSize(new java.awt.Dimension(minw, minh));
            if (maxw >= 0) {
                k.setMaximumSize(new java.awt.Dimension(maxw, maxh));
            }
            comps.add(k);
            return comps.size() - 1;
        }

        public void add(int comp) {
            host.add(comps.get(comp));
        }

        public void addAsIs(int comp, int x, int y) {
            comps.get(comp).setLocation(x, y);
            host.add(comps.get(comp), new javax.swing.SpringLayout.Constraints(comps.get(comp)));
        }

        public void addWith(int comp, int x, int y, int w, int h) {
            javax.swing.SpringLayout.Constraints cons;
            if (w < 0 && x < 0) {
                cons = new javax.swing.SpringLayout.Constraints();
            } else if (w < 0) {
                cons = new javax.swing.SpringLayout.Constraints(springs.get(x), springs.get(y));
            } else {
                cons = new javax.swing.SpringLayout.Constraints(springs.get(x), springs.get(y), springs.get(w),
                        springs.get(h));
            }
            host.add(comps.get(comp), cons);
        }

        public void addWithProperty(int comp, int x, int y) {
            comps.get(comp).putClientProperty(javax.swing.SpringLayout.class,
                    new javax.swing.SpringLayout.Constraints(springs.get(x), springs.get(y)));
            host.add(comps.get(comp));
        }

        public void remove(int comp) {
            host.remove(comps.get(comp));
        }

        public int constant(int pref) {
            return s(javax.swing.Spring.constant(pref));
        }

        public int constant(int min, int pref, int max) {
            return s(javax.swing.Spring.constant(min, pref, max));
        }

        public int sum(int a, int b) {
            return s(javax.swing.Spring.sum(springs.get(a), springs.get(b)));
        }

        public int max(int a, int b) {
            return s(javax.swing.Spring.max(springs.get(a), springs.get(b)));
        }

        public int minus(int a) {
            return s(javax.swing.Spring.minus(springs.get(a)));
        }

        public int scale(int a, float factor) {
            return s(javax.swing.Spring.scale(springs.get(a), factor));
        }

        public int width(int comp) {
            return s(javax.swing.Spring.width(c(comp)));
        }

        public int height(int comp) {
            return s(javax.swing.Spring.height(c(comp)));
        }

        public int edge(String edge, int comp) {
            return s(layout.getConstraint(edge, c(comp)));
        }

        public int constraint(String edge, int comp) {
            return s(layout.getConstraints(c(comp)).getConstraint(edge));
        }

        public void put(String e1, int c1, int pad, String e2, int c2) {
            layout.putConstraint(e1, c(c1), pad, e2, c(c2));
        }

        public void putSpring(String e1, int c1, int spring, String e2, int c2) {
            layout.putConstraint(e1, c(c1), springs.get(spring), e2, c(c2));
        }

        public void set(int comp, String edge, int spring) {
            layout.getConstraints(c(comp)).setConstraint(edge, spring < 0 ? null : springs.get(spring));
        }

        public void setValue(int spring, int value) {
            springs.get(spring).setValue(value);
        }

        public String spring(int spring) {
            if (spring < 0) {
                return "null";
            }
            javax.swing.Spring s = springs.get(spring);
            return s.getMinimumValue() + "/" + s.getPreferredValue() + "/" + s.getMaximumValue() + "=" + s.getValue();
        }

        public String layout(int w, int h) {
            host.setSize(w, h);
            layout.layoutContainer(host);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < comps.size(); i++) {
                java.awt.Component k = comps.get(i);
                if (k.getParent() != host) {
                    continue;
                }
                sb.append(i).append(' ').append(k.getX()).append(',').append(k.getY()).append(' ')
                        .append(k.getWidth()).append('x').append(k.getHeight()).append('\n');
            }
            return sb.toString();
        }

        public String sizes() {
            java.awt.Dimension p = layout.preferredLayoutSize(host);
            java.awt.Dimension m = layout.minimumLayoutSize(host);
            java.awt.Dimension x = layout.maximumLayoutSize(host);
            return "pref=" + p.width + "x" + p.height + " min=" + m.width + "x" + m.height + " max=" + x.width + "x"
                    + x.height + " align=" + layout.getLayoutAlignmentX(host) + "," + layout.getLayoutAlignmentY(host);
        }
    }

    // ------------------------------------------------------------------
    // This layer's side

    private static final class OurLeaf extends JComponent {
        private final int baseline;

        OurLeaf(int baseline) {
            this.baseline = baseline;
        }

        @Override
        public int getBaseline(int width, int height) {
            return baseline < 0 ? -1 : Math.min(baseline, height);
        }
    }

    private static final class Ours implements Side {
        final SpringLayout layout = new SpringLayout();
        final JComponent host = new JComponent() {
        };
        final List<JComponent> comps = new ArrayList<JComponent>();
        final List<Spring> springs = new ArrayList<Spring>();

        Ours() {
            host.setLayout(layout);
        }

        private Component c(int i) {
            return i == HOST ? host : comps.get(i);
        }

        private int s(Spring s) {
            if (s == null) {
                return -1;
            }
            springs.add(s);
            return springs.size() - 1;
        }

        public void border(int top, int left, int bottom, int right) {
            host.setBorder(new EmptyBorder(top, left, bottom, right));
        }

        public int comp(int pw, int ph, int minw, int minh, int maxw, int maxh, int baseline) {
            JComponent k = new OurLeaf(baseline);
            k.setPreferredSize(new Dimension(pw, ph));
            k.setMinimumSize(new Dimension(minw, minh));
            if (maxw >= 0) {
                k.setMaximumSize(new Dimension(maxw, maxh));
            }
            comps.add(k);
            return comps.size() - 1;
        }

        public void add(int comp) {
            host.add(comps.get(comp));
        }

        public void addAsIs(int comp, int x, int y) {
            comps.get(comp).setLocation(x, y);
            host.add(comps.get(comp), new SpringLayout.Constraints(comps.get(comp)));
        }

        public void addWith(int comp, int x, int y, int w, int h) {
            SpringLayout.Constraints cons;
            if (w < 0 && x < 0) {
                cons = new SpringLayout.Constraints();
            } else if (w < 0) {
                cons = new SpringLayout.Constraints(springs.get(x), springs.get(y));
            } else {
                cons = new SpringLayout.Constraints(springs.get(x), springs.get(y), springs.get(w), springs.get(h));
            }
            host.add(comps.get(comp), cons);
        }

        public void addWithProperty(int comp, int x, int y) {
            comps.get(comp).putClientProperty(SpringLayout.class,
                    new SpringLayout.Constraints(springs.get(x), springs.get(y)));
            host.add(comps.get(comp));
        }

        public void remove(int comp) {
            host.remove(comps.get(comp));
        }

        public int constant(int pref) {
            return s(Spring.constant(pref));
        }

        public int constant(int min, int pref, int max) {
            return s(Spring.constant(min, pref, max));
        }

        public int sum(int a, int b) {
            return s(Spring.sum(springs.get(a), springs.get(b)));
        }

        public int max(int a, int b) {
            return s(Spring.max(springs.get(a), springs.get(b)));
        }

        public int minus(int a) {
            return s(Spring.minus(springs.get(a)));
        }

        public int scale(int a, float factor) {
            return s(Spring.scale(springs.get(a), factor));
        }

        public int width(int comp) {
            return s(Spring.width(c(comp)));
        }

        public int height(int comp) {
            return s(Spring.height(c(comp)));
        }

        public int edge(String edge, int comp) {
            return s(layout.getConstraint(edge, c(comp)));
        }

        public int constraint(String edge, int comp) {
            return s(layout.getConstraints(c(comp)).getConstraint(edge));
        }

        public void put(String e1, int c1, int pad, String e2, int c2) {
            layout.putConstraint(e1, c(c1), pad, e2, c(c2));
        }

        public void putSpring(String e1, int c1, int spring, String e2, int c2) {
            layout.putConstraint(e1, c(c1), springs.get(spring), e2, c(c2));
        }

        public void set(int comp, String edge, int spring) {
            layout.getConstraints(c(comp)).setConstraint(edge, spring < 0 ? null : springs.get(spring));
        }

        public void setValue(int spring, int value) {
            springs.get(spring).setValue(value);
        }

        public String spring(int spring) {
            if (spring < 0) {
                return "null";
            }
            Spring s = springs.get(spring);
            return s.getMinimumValue() + "/" + s.getPreferredValue() + "/" + s.getMaximumValue() + "=" + s.getValue();
        }

        public String layout(int w, int h) {
            host.setSize(w, h);
            layout.layoutContainer(host);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < comps.size(); i++) {
                Component k = comps.get(i);
                if (k.getParent() != host) {
                    continue;
                }
                sb.append(i).append(' ').append(k.getX()).append(',').append(k.getY()).append(' ')
                        .append(k.getWidth()).append('x').append(k.getHeight()).append('\n');
            }
            return sb.toString();
        }

        public String sizes() {
            Dimension p = layout.preferredLayoutSize(host);
            Dimension m = layout.minimumLayoutSize(host);
            Dimension x = layout.maximumLayoutSize(host);
            return "pref=" + p.width + "x" + p.height + " min=" + m.width + "x" + m.height + " max=" + x.width + "x"
                    + x.height + " align=" + layout.getLayoutAlignmentX(host) + "," + layout.getLayoutAlignmentY(host);
        }
    }

    // ------------------------------------------------------------------

    private static String play(Side side, Script script) {
        StringBuilder out = new StringBuilder();
        try {
            script.run(side, out);
        } catch (RuntimeException e) {
            out.append("threw ").append(e.getClass().getSimpleName()).append('\n');
        }
        return out.toString();
    }

    private static String check(String what, Script script) {
        String real = play(new Real(), script);
        String ours = play(new Ours(), script);
        assertEquals(what, real, ours);
        assertTrue(what + ": nothing was observed", real.length() > 0);
        return real;
    }

    private static int leaf(Side s, int w, int h) {
        return s.comp(w, h, w, h, -1, -1, -1);
    }

    private static int flexible(Side s, int w, int h) {
        return s.comp(w, h, w / 2, h / 2, w * 3, h * 2, -1);
    }

    private static void observe(Side s, StringBuilder out, int... sizes) {
        out.append(s.sizes()).append('\n');
        for (int i = 0; i + 1 < sizes.length; i += 2) {
            out.append(sizes[i]).append('x').append(sizes[i + 1]).append('\n').append(s.layout(sizes[i], sizes[i + 1]));
        }
        out.append(s.sizes()).append('\n');
    }

    private static void describe(Side s, StringBuilder out, int comp) {
        String[] all = {W, E, HC, WD, N, S, VC, HT};
        for (String e : all) {
            out.append(comp).append('.').append(e).append(' ').append(s.spring(s.constraint(e, comp))).append(' ')
                    .append(s.spring(s.edge(e, comp))).append('\n');
        }
    }

    // ------------------------------------------------------------------
    // Spring arithmetic

    @Test
    public void constantSprings() {
        check("constants", new Script() {
            public void run(Side s, StringBuilder out) {
                int a = s.constant(7);
                int b = s.constant(2, 10, 40);
                int c = s.constant(-30, -5, 3);
                int[] all = {a, b, c};
                int[] values = {0, 5, 10, 100, -100, 40, 2, Integer.MIN_VALUE, 12, 12, Integer.MAX_VALUE};
                for (int sp : all) {
                    out.append(s.spring(sp)).append('\n');
                    for (int v : values) {
                        s.setValue(sp, v);
                        out.append(v).append(" -> ").append(s.spring(sp)).append('\n');
                    }
                }
            }
        });
    }

    @Test
    public void sumsSpreadTheStrain() {
        check("sum", new Script() {
            public void run(Side s, StringBuilder out) {
                int a = s.constant(10, 20, 100);
                int b = s.constant(0, 30, 40);
                int sum = s.sum(a, b);
                int[] values = {50, 70, 140, 200, 10, 25, 0, -40, 51, 49, Integer.MIN_VALUE, 33};
                out.append(s.spring(sum)).append('\n');
                for (int v : values) {
                    s.setValue(sum, v);
                    out.append(v).append(" -> ").append(s.spring(sum)).append(" = ").append(s.spring(a)).append(" + ")
                            .append(s.spring(b)).append('\n');
                }
            }
        });
        check("sum of rigid springs", new Script() {
            public void run(Side s, StringBuilder out) {
                int a = s.constant(20);
                int b = s.constant(5, 30, 30);
                int sum = s.sum(a, b);
                int[] values = {50, 70, 30, 49, 51, Integer.MIN_VALUE, 50};
                for (int v : values) {
                    s.setValue(sum, v);
                    out.append(v).append(" -> ").append(s.spring(sum)).append(" = ").append(s.spring(a)).append(" + ")
                            .append(s.spring(b)).append('\n');
                }
            }
        });
        check("nested sums", new Script() {
            public void run(Side s, StringBuilder out) {
                int a = s.constant(1, 10, 30);
                int b = s.constant(5, 10, 12);
                int c = s.constant(0, 40, 400);
                int ab = s.sum(a, b);
                int abc = s.sum(ab, c);
                int twice = s.sum(abc, abc);
                int[] values = {60, 120, 300, 20, 6, 61, Integer.MIN_VALUE};
                for (int v : values) {
                    s.setValue(abc, v);
                    out.append(v).append(" -> ").append(s.spring(abc)).append(' ').append(s.spring(ab)).append(' ')
                            .append(s.spring(a)).append(' ').append(s.spring(b)).append(' ').append(s.spring(c))
                            .append(' ').append(s.spring(twice)).append('\n');
                }
                s.setValue(twice, 200);
                out.append(s.spring(twice)).append(' ').append(s.spring(abc)).append(' ').append(s.spring(a))
                        .append('\n');
            }
        });
    }

    @Test
    public void maximaHandTheValueToBoth() {
        check("max", new Script() {
            public void run(Side s, StringBuilder out) {
                int a = s.constant(10, 20, 100);
                int b = s.constant(0, 30, 40);
                int max = s.max(a, b);
                int[] values = {30, 35, 90, 5, Integer.MIN_VALUE, 22};
                out.append(s.spring(max)).append('\n');
                for (int v : values) {
                    s.setValue(max, v);
                    out.append(v).append(" -> ").append(s.spring(max)).append(" of ").append(s.spring(a))
                            .append(" and ").append(s.spring(b)).append('\n');
                }
                // The properties are kept from the first time they are read.
                s.setValue(a, 60);
                out.append(s.spring(max)).append('\n');
                s.setValue(max, Integer.MIN_VALUE);
                out.append(s.spring(max)).append(' ').append(s.spring(a)).append('\n');
            }
        });
    }

    @Test
    public void negationAndScaling() {
        check("minus", new Script() {
            public void run(Side s, StringBuilder out) {
                int a = s.constant(10, 20, 100);
                int neg = s.minus(a);
                int back = s.minus(neg);
                int diff = s.sum(s.constant(0, 200, 500), neg);
                int[] values = {-20, -50, -5, 30, Integer.MIN_VALUE, -21};
                out.append(s.spring(neg)).append(' ').append(s.spring(back)).append(' ').append(s.spring(diff))
                        .append('\n');
                for (int v : values) {
                    s.setValue(neg, v);
                    out.append(v).append(" -> ").append(s.spring(neg)).append(' ').append(s.spring(a)).append(' ')
                            .append(s.spring(back)).append('\n');
                }
                s.setValue(diff, 300);
                out.append(s.spring(diff)).append(' ').append(s.spring(a)).append('\n');
                s.setValue(diff, 100);
                out.append(s.spring(diff)).append(' ').append(s.spring(a)).append('\n');
            }
        });
        final float[] factors = {0.5f, 2f, 1f, -1f, -0.5f, 0.33f, 1.5f, 3.7f, -2.25f, 0.1f};
        for (final float f : factors) {
            check("scale by " + f, new Script() {
                public void run(Side s, StringBuilder out) {
                    int a = s.constant(11, 21, 101);
                    int scaled = s.scale(a, f);
                    int[] values = {10, 11, 25, 63, -17, -40, 0, Integer.MIN_VALUE, 7};
                    out.append(s.spring(scaled)).append('\n');
                    for (int v : values) {
                        s.setValue(scaled, v);
                        out.append(v).append(" -> ").append(s.spring(scaled)).append(' ').append(s.spring(a))
                                .append('\n');
                    }
                }
            });
        }
    }

    @Test
    public void componentSprings() {
        check("width and height", new Script() {
            public void run(Side s, StringBuilder out) {
                int a = s.comp(80, 20, 30, 10, 300, 60, -1);
                int b = leaf(s, 55, 33);
                int[] all = {s.width(a), s.height(a), s.width(b), s.height(b), s.sum(s.width(a), s.width(b)),
                    s.max(s.height(a), s.height(b)), s.scale(s.width(a), 0.5f), s.minus(s.height(a))};
                int[] values = {40, 100, 0, Integer.MIN_VALUE, 500};
                for (int sp : all) {
                    out.append(s.spring(sp)).append('\n');
                    for (int v : values) {
                        s.setValue(sp, v);
                        out.append(v).append(" -> ").append(s.spring(sp)).append('\n');
                    }
                }
            }
        });
    }

    @Test
    public void nullArgumentsAreRejectedAlike() {
        check("scale(null)", new Script() {
            public void run(Side s, StringBuilder out) {
                out.append("scale\n");
                if (s instanceof Real) {
                    javax.swing.Spring.scale(null, 2f);
                } else {
                    Spring.scale(null, 2f);
                }
            }
        });
        check("width(null)", new Script() {
            public void run(Side s, StringBuilder out) {
                out.append("width\n");
                if (s instanceof Real) {
                    javax.swing.Spring.width(null);
                } else {
                    Spring.width(null);
                }
            }
        });
        check("height(null)", new Script() {
            public void run(Side s, StringBuilder out) {
                out.append("height\n");
                if (s instanceof Real) {
                    javax.swing.Spring.height(null);
                } else {
                    Spring.height(null);
                }
            }
        });
    }

    @Test
    public void randomSpringArithmetic() {
        for (int seed = 0; seed < 400; seed++) {
            final int fseed = seed;
            check("random springs " + seed, new Script() {
                public void run(Side s, StringBuilder out) {
                    Random rnd = new Random(7000 + fseed);
                    List<Integer> all = new ArrayList<Integer>();
                    int leaves = 2 + rnd.nextInt(3);
                    for (int i = 0; i < leaves; i++) {
                        int min = rnd.nextInt(40) - 10;
                        int pref = min + rnd.nextInt(40);
                        int max = pref + rnd.nextInt(60);
                        all.add(rnd.nextInt(5) == 0 ? s.constant(pref) : s.constant(min, pref, max));
                    }
                    int ops = 2 + rnd.nextInt(6);
                    for (int i = 0; i < ops; i++) {
                        int a = all.get(rnd.nextInt(all.size()));
                        int b = all.get(rnd.nextInt(all.size()));
                        switch (rnd.nextInt(4)) {
                            case 0:
                                all.add(s.sum(a, b));
                                break;
                            case 1:
                                all.add(s.max(a, b));
                                break;
                            case 2:
                                all.add(s.minus(a));
                                break;
                            default:
                                all.add(s.scale(a, (rnd.nextInt(17) - 8) / 4f + 0.125f));
                                break;
                        }
                    }
                    for (int step = 0; step < 8; step++) {
                        int target = all.get(rnd.nextInt(all.size()));
                        int v = rnd.nextInt(6) == 0 ? Integer.MIN_VALUE : rnd.nextInt(400) - 100;
                        s.setValue(target, v);
                        out.append(target).append(" := ").append(v).append('\n');
                        for (int sp : all) {
                            out.append("  ").append(s.spring(sp)).append('\n');
                        }
                    }
                }
            });
        }
    }

    // ------------------------------------------------------------------
    // Layouts

    @Test
    public void defaultsPlaceEverythingAtTheOrigin() {
        check("defaults", new Script() {
            public void run(Side s, StringBuilder out) {
                int a = flexible(s, 80, 20);
                int b = leaf(s, 40, 44);
                s.add(a);
                s.add(b);
                observe(s, out, 300, 200, 0, 0, 10, 10);
                describe(s, out, a);
                describe(s, out, HOST);
            }
        });
        check("empty", new Script() {
            public void run(Side s, StringBuilder out) {
                s.border(3, 4, 5, 6);
                observe(s, out, 300, 200, 0, 0);
            }
        });
    }

    @Test
    public void constraintChains() {
        check("a row", new Script() {
            public void run(Side s, StringBuilder out) {
                int a = leaf(s, 80, 20);
                int b = leaf(s, 40, 44);
                int c = flexible(s, 60, 30);
                s.add(a);
                s.add(b);
                s.add(c);
                s.put(W, a, 5, W, HOST);
                s.put(N, a, 7, N, HOST);
                s.put(W, b, 6, E, a);
                s.put(N, b, 0, N, a);
                s.put(W, c, 9, E, b);
                s.put(N, c, 3, S, b);
                s.put(E, HOST, 5, E, c);
                s.put(S, HOST, 11, S, c);
                observe(s, out, 400, 300, 215, 115, 100, 50, 0, 0);
                describe(s, out, c);
                describe(s, out, HOST);
            }
        });
        check("a column from the bottom right", new Script() {
            public void run(Side s, StringBuilder out) {
                s.border(4, 9, 2, 13);
                int a = leaf(s, 80, 20);
                int b = leaf(s, 40, 44);
                int c = leaf(s, 61, 31);
                s.add(a);
                s.add(b);
                s.add(c);
                s.put(E, a, -5, E, HOST);
                s.put(S, a, -5, S, HOST);
                s.put(E, b, 0, E, a);
                s.put(S, b, -8, N, a);
                s.put(HC, c, 0, HC, b);
                s.put(VC, c, -60, VC, b);
                observe(s, out, 400, 300, 150, 90, 0, 0);
                describe(s, out, b);
                describe(s, out, c);
            }
        });
        check("stretching between two edges", new Script() {
            public void run(Side s, StringBuilder out) {
                int a = leaf(s, 80, 20);
                int b = flexible(s, 40, 44);
                s.add(a);
                s.add(b);
                s.put(W, a, 10, W, HOST);
                s.put(E, a, -10, E, HOST);
                s.put(N, a, 10, N, HOST);
                s.put(W, b, 0, W, a);
                s.put(E, b, 0, HC, a);
                s.put(N, b, 5, S, a);
                s.put(S, b, -5, S, HOST);
                observe(s, out, 400, 300, 150, 90, 20, 20, 0, 0);
                describe(s, out, a);
                describe(s, out, b);
            }
        });
        check("springs between components", new Script() {
            public void run(Side s, StringBuilder out) {
                int a = flexible(s, 80, 20);
                int b = flexible(s, 40, 44);
                s.add(a);
                s.add(b);
                int gap = s.constant(2, 10, 100);
                s.putSpring(W, a, gap, W, HOST);
                s.putSpring(W, b, s.constant(0, 6, 60), E, a);
                s.putSpring(E, HOST, s.constant(2, 10, 100), E, b);
                s.putSpring(N, a, s.constant(0, 5, 50), N, HOST);
                s.putSpring(N, b, s.constant(0, 5, 50), N, HOST);
                s.putSpring(S, HOST, s.constant(0, 5, 50), S, b);
                observe(s, out, 146, 54, 400, 300, 100, 30, 60, 20, 0, 0, 2000, 900);
                describe(s, out, a);
                describe(s, out, b);
                describe(s, out, HOST);
            }
        });
    }

    @Test
    public void aForm() {
        check("form", new Script() {
            public void run(Side s, StringBuilder out) {
                s.border(8, 8, 8, 8);
                int rows = 4;
                int[] labels = new int[rows];
                int[] fields = new int[rows];
                int[] lw = {40, 95, 62, 18};
                int labelWidth = -1;
                for (int i = 0; i < rows; i++) {
                    labels[i] = leaf(s, lw[i], 16);
                    fields[i] = s.comp(120, 22 + i, 30, 22 + i, 2000, 22 + i, -1);
                    s.add(labels[i]);
                    s.add(fields[i]);
                    int w = s.width(labels[i]);
                    labelWidth = labelWidth < 0 ? w : s.max(labelWidth, w);
                }
                int ok = leaf(s, 70, 26);
                int cancel = leaf(s, 84, 26);
                s.add(ok);
                s.add(cancel);
                for (int i = 0; i < rows; i++) {
                    s.put(W, labels[i], 0, W, HOST);
                    s.set(labels[i], WD, labelWidth);
                    s.put(W, fields[i], 6, E, labels[i]);
                    s.put(E, fields[i], 0, E, HOST);
                    if (i == 0) {
                        s.put(N, fields[i], 0, N, HOST);
                    } else {
                        s.put(N, fields[i], 5, S, fields[i - 1]);
                    }
                    s.put(VC, labels[i], 0, VC, fields[i]);
                }
                s.put(E, cancel, 0, E, HOST);
                s.put(E, ok, -6, W, cancel);
                s.put(N, cancel, 12, S, fields[rows - 1]);
                s.put(N, ok, 0, N, cancel);
                s.put(S, HOST, 0, S, cancel);
                observe(s, out, 237, 177, 500, 400, 180, 120, 60, 60, 0, 0);
                out.append(s.spring(labelWidth)).append('\n');
                for (int i = 0; i < rows; i++) {
                    describe(s, out, labels[i]);
                    describe(s, out, fields[i]);
                }
                describe(s, out, ok);
                describe(s, out, HOST);
            }
        });
    }

    @Test
    public void theTwoMostRecentConstraintsStand() {
        final String[][] orders = {{W, E, WD}, {W, WD, E}, {E, WD, W}, {WD, E, W}, {HC, W, E}, {W, HC, WD},
            {WD, HC, W}, {W, E, W}, {W, W, E}, {E, W, HC}, {WD, W, HC}, {HC, WD, E}, {W, E, HC, WD}, {E, HC}, {HC, E}, {W, E, HC}, {WD, HC, E}};
        for (final String[] order : orders) {
            check("horizontal " + java.util.Arrays.toString(order), new Script() {
                public void run(Side s, StringBuilder out) {
                    int a = flexible(s, 80, 20);
                    s.add(a);
                    for (int i = 0; i < order.length; i++) {
                        String e = order[i];
                        if (WD.equals(e)) {
                            s.set(a, WD, s.constant(30 + i, 50 + i, 70 + i));
                        } else {
                            s.put(e, a, W.equals(e) ? 13 + i : E.equals(e) ? -17 - i : 4 + i, e, HOST);
                        }
                    }
                    observe(s, out, 300, 200, 100, 50, 0, 0);
                    describe(s, out, a);
                }
            });
        }
        final String[][] vertical = {{N, S, HT}, {N, HT, S}, {S, HT, N}, {HT, S, N}, {VC, N, S}, {N, VC, HT},
            {HT, VC, N}, {N, S, N}, {S, N, VC}, {HT, N, VC}, {VC, HT, S}, {N, S, VC, HT}, {S, VC}, {VC, S}, {N, S, VC}, {HT, VC, S}};
        for (final String[] order : vertical) {
            check("vertical " + java.util.Arrays.toString(order), new Script() {
                public void run(Side s, StringBuilder out) {
                    int a = flexible(s, 80, 20);
                    s.add(a);
                    for (int i = 0; i < order.length; i++) {
                        String e = order[i];
                        if (HT.equals(e)) {
                            s.set(a, HT, s.constant(30 + i, 50 + i, 70 + i));
                        } else {
                            s.put(e, a, N.equals(e) ? 13 + i : S.equals(e) ? -17 - i : 4 + i, e, HOST);
                        }
                    }
                    observe(s, out, 300, 200, 100, 50, 0, 0);
                    describe(s, out, a);
                }
            });
        }
    }

    @Test
    public void constraintObjects() {
        check("constraints given when adding", new Script() {
            public void run(Side s, StringBuilder out) {
                int a = flexible(s, 80, 20);
                int b = leaf(s, 40, 44);
                int c = leaf(s, 61, 31);
                int d = leaf(s, 25, 26);
                int e = leaf(s, 27, 28);
                s.addWith(a, s.constant(12), s.constant(0, 14, 90), -1, -1);
                s.addWith(b, s.constant(100), s.constant(50), s.constant(10, 66, 90), s.constant(33));
                s.addWith(c, -1, -1, -1, -1);
                s.addAsIs(d, 140, 9);
                s.addWithProperty(e, s.constant(200), s.constant(120));
                observe(s, out, 400, 300, 0, 0);
                for (int k = 0; k < 5; k++) {
                    describe(s, out, k);
                }
            }
        });
        check("clearing and replacing a constraint", new Script() {
            public void run(Side s, StringBuilder out) {
                int a = flexible(s, 80, 20);
                int b = leaf(s, 40, 44);
                s.add(a);
                s.add(b);
                s.put(W, a, 20, W, HOST);
                s.put(W, b, 5, E, a);
                observe(s, out, 300, 200);
                s.put(W, a, 60, W, HOST);
                observe(s, out, 300, 200);
                s.set(a, W, -1);
                describe(s, out, a);
                s.set(a, "Nowhere", s.constant(3));
                out.append(s.spring(s.constraint("Nowhere", a))).append('\n');
                s.set(a, E, s.constant(250));
                observe(s, out, 300, 200);
                describe(s, out, a);
                describe(s, out, b);
            }
        });
        check("removing a component forgets its constraints", new Script() {
            public void run(Side s, StringBuilder out) {
                int a = leaf(s, 80, 20);
                int b = leaf(s, 40, 44);
                s.add(a);
                s.add(b);
                s.put(W, a, 20, W, HOST);
                s.put(N, a, 30, N, HOST);
                s.put(W, b, 5, E, a);
                observe(s, out, 300, 200);
                s.remove(a);
                observe(s, out, 300, 200);
                s.add(a);
                observe(s, out, 300, 200);
                describe(s, out, a);
            }
        });
    }

    @Test
    public void baselines() {
        check("baseline read from a positioned component", new Script() {
            public void run(Side s, StringBuilder out) {
                int a = s.comp(80, 30, 80, 30, -1, -1, 21);
                int b = s.comp(40, 18, 40, 18, -1, -1, 12);
                int c = leaf(s, 33, 17);
                s.add(a);
                s.add(b);
                s.add(c);
                s.put(W, a, 5, W, HOST);
                s.put(N, a, 40, N, HOST);
                s.put(W, b, 5, E, a);
                s.put(N, b, 3, BL, a);
                s.put(W, c, 5, E, b);
                s.put(S, c, 0, BL, b);
                observe(s, out, 300, 200, 0, 0);
                out.append(s.spring(s.edge(BL, a))).append(' ').append(s.spring(s.edge(BL, b))).append(' ')
                        .append(s.spring(s.edge(BL, c))).append('\n');
            }
        });
    }

    @Test
    public void cyclesAreAbandoned() {
        java.io.PrintStream err = System.err;
        System.setErr(new java.io.PrintStream(new java.io.ByteArrayOutputStream()));
        try {
            check("two components each to the right of the other", new Script() {
                public void run(Side s, StringBuilder out) {
                    int a = leaf(s, 80, 20);
                    int b = leaf(s, 40, 44);
                    int c = leaf(s, 30, 30);
                    s.add(a);
                    s.add(b);
                    s.add(c);
                    s.put(W, a, 5, E, b);
                    s.put(W, b, 5, E, a);
                    s.put(N, c, 9, N, HOST);
                    s.put(W, c, 9, W, HOST);
                    s.put(N, a, 4, S, a);
                    out.append(s.layout(300, 200));
                    out.append(s.sizes()).append('\n');
                }
            });
        } finally {
            System.setErr(err);
        }
    }

    private static String pick(Random rnd, boolean horizontal) {
        String[] from = horizontal ? HORIZONTAL : VERTICAL;
        return from[rnd.nextInt(from.length)];
    }

    @Test
    public void randomLayouts() {
        int laidOut = 0;
        for (int seed = 0; seed < 500; seed++) {
            final int fseed = seed;
            String result = check("random layout " + seed, new Script() {
                public void run(Side s, StringBuilder out) {
                    Random rnd = new Random(31000 + fseed);
                    if (rnd.nextBoolean()) {
                        s.border(rnd.nextInt(9), rnd.nextInt(9), rnd.nextInt(9), rnd.nextInt(9));
                    }
                    int n = 2 + rnd.nextInt(5);
                    for (int i = 0; i < n; i++) {
                        int w = 10 + rnd.nextInt(90);
                        int h = 10 + rnd.nextInt(50);
                        if (rnd.nextInt(3) == 0) {
                            leaf(s, w, h);
                        } else {
                            s.comp(w, h, rnd.nextInt(w + 1), rnd.nextInt(h + 1), w + rnd.nextInt(200),
                                    h + rnd.nextInt(100), -1);
                        }
                        s.add(i);
                    }
                    // A component is only tied to earlier ones, or to the
                    // container, so that nothing is cyclic. When the far
                    // edge of the container is itself tied to a component,
                    // components only refer to its near edge.
                    boolean tieEast = rnd.nextInt(3) != 0;
                    boolean tieSouth = rnd.nextInt(3) != 0;
                    for (int i = 0; i < n; i++) {
                        for (int axis = 0; axis < 2; axis++) {
                            boolean horizontal = axis == 0;
                            int count = rnd.nextInt(4);
                            for (int k = 0; k < count; k++) {
                                String e1 = pick(rnd, horizontal);
                                int target = i == 0 || rnd.nextInt(4) == 0 ? HOST : rnd.nextInt(i);
                                boolean along = rnd.nextInt(10) == 0 ? !horizontal : horizontal;
                                String e2 = pick(rnd, along);
                                boolean rigidHost = target == HOST && (along ? tieEast : tieSouth);
                                if (rigidHost) {
                                    e2 = along ? W : N;
                                }
                                if (WD.equals(e1) || HT.equals(e1)) {
                                    if (rigidHost || rnd.nextBoolean()) {
                                        int min = rnd.nextInt(30);
                                        int pref = min + rnd.nextInt(60);
                                        s.set(i, e1, s.constant(min, pref, pref + rnd.nextInt(90)));
                                        continue;
                                    }
                                }
                                if (rnd.nextInt(3) == 0) {
                                    int min = rnd.nextInt(10);
                                    int pref = min + rnd.nextInt(20);
                                    s.putSpring(e1, i, s.constant(min, pref, pref + rnd.nextInt(60)), e2, target);
                                } else {
                                    s.put(e1, i, rnd.nextInt(40) - 8, e2, target);
                                }
                            }
                        }
                    }
                    if (tieEast) {
                        s.put(E, HOST, rnd.nextInt(12), E, rnd.nextInt(n));
                    }
                    if (tieSouth) {
                        s.put(S, HOST, rnd.nextInt(12), S, rnd.nextInt(n));
                    }
                    observe(s, out, 400, 300, 150, 100, 37, 23, 0, 0, 900, 700);
                    for (int i = 0; i < n; i++) {
                        describe(s, out, i);
                    }
                    describe(s, out, HOST);
                }
            });
            if (result.indexOf("threw") < 0) {
                laidOut++;
            }
        }
        assertTrue("only " + laidOut + " of 500 random layouts ran to the end", laidOut > 450);
    }

    // ------------------------------------------------------------------
    // Behaviour that is this layer's own

    private static JComponent box(int w, int h) {
        JComponent c = new JComponent() {
        };
        c.setPreferredSize(new Dimension(w, h));
        c.setMinimumSize(new Dimension(w, h));
        return c;
    }

    private static void assertBounds(Component c, int x, int y, int w, int h) {
        assertEquals(x + "," + y + " " + w + "x" + h,
                c.getX() + "," + c.getY() + " " + c.getWidth() + "x" + c.getHeight());
    }

    /// With the baseline as one of two constraints that leave the height
    /// open, the component keeps its preferred height.
    @Test
    public void aBaselineAndAnEdgeKeepThePreferredHeight() {
        JPanel host = new JPanel();
        SpringLayout layout = new SpringLayout();
        host.setLayout(layout);
        JComponent a = box(80, 20);
        host.add(a);
        layout.putConstraint(SpringLayout.BASELINE, a, 50, SpringLayout.NORTH, host);
        layout.putConstraint(SpringLayout.SOUTH, a, -10, SpringLayout.SOUTH, host);
        host.setSize(300, 200);
        layout.layoutContainer(host);
        assertBounds(a, 0, 170, 80, 20);
    }
}
