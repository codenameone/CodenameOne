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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.LayoutManager2;

/// Lays components out by tying their edges to one another with
/// [Spring]s: `putConstraint` says that one edge of a component sits a
/// distance from an edge of another component, or of the container.
///
/// Each axis of a component is fixed by two of its edges and sizes. A
/// component starts out at the origin with the size it asks for; of the
/// constraints given for an axis the two most recent stand and an older
/// one is dropped.
///
/// A constraint that depends on itself is reported once on the error
/// stream and treated as unset, as in the JDK.
///
/// Not supported: a height cannot be derived from a `BASELINE` constraint
/// unless the baseline asked for is the one the component has at its
/// preferred size, since the layer's components do not say how their
/// baseline moves when they are resized. For the same reason a component
/// held by its `BASELINE` and by `SOUTH` or `VERTICAL_CENTER` alone keeps
/// the height it asks for.
public class SpringLayout implements LayoutManager2 {

    /// The top edge of a component's bounding rectangle.
    public static final String NORTH = "North";

    /// The bottom edge of a component's bounding rectangle.
    public static final String SOUTH = "South";

    /// The right edge of a component's bounding rectangle.
    public static final String EAST = "East";

    /// The left edge of a component's bounding rectangle.
    public static final String WEST = "West";

    /// The horizontal centre of a component's bounding rectangle.
    public static final String HORIZONTAL_CENTER = "HorizontalCenter";

    /// The vertical centre of a component's bounding rectangle.
    public static final String VERTICAL_CENTER = "VerticalCenter";

    /// The baseline of a component.
    public static final String BASELINE = "Baseline";

    /// The width of a component's bounding rectangle.
    public static final String WIDTH = "Width";

    /// The height of a component's bounding rectangle.
    public static final String HEIGHT = "Height";

    private static final String[] ALL_HORIZONTAL = {WEST, WIDTH, EAST, HORIZONTAL_CENTER};

    private static final String[] ALL_VERTICAL = {NORTH, HEIGHT, SOUTH, VERTICAL_CENTER, BASELINE};

    private final Map<Component, Constraints> componentConstraints = new HashMap<Component, Constraints>();

    // What a spring that depends on itself is replaced with.
    private final Spring cyclicReference = Spring.constant(Spring.UNSET);
    // Springs being examined, or found to be cyclic, and springs found
    // not to be; compared by identity.
    private List<Spring> cyclicSprings = new ArrayList<Spring>();
    private List<Spring> acyclicSprings = new ArrayList<Spring>();

    /// Constructs a new `SpringLayout`.
    public SpringLayout() {
    }

    /// The springs that position and size one component: for each axis
    /// any two of the edges, the centre and the size, from which the
    /// others follow.
    public static class Constraints {
        private Spring x;
        private Spring y;
        private Spring width;
        private Spring height;
        private Spring east;
        private Spring south;
        private Spring horizontalCenter;
        private Spring verticalCenter;
        private Spring baseline;

        // The names constrained on each axis, oldest first; never more
        // than two.
        private final List<String> horizontalHistory = new ArrayList<String>(2);
        private final List<String> verticalHistory = new ArrayList<String>(2);

        // The component these constraints are being applied to.
        private Component c;

        /// Creates empty constraints.
        public Constraints() {
        }

        /// Creates constraints with the given `x` and `y`.
        public Constraints(Spring x, Spring y) {
            setX(x);
            setY(y);
        }

        /// Creates constraints with the given position and size.
        public Constraints(Spring x, Spring y, Spring width, Spring height) {
            setX(x);
            setY(y);
            setWidth(width);
            setHeight(height);
        }

        /// Creates constraints that keep a component where it is now, at
        /// the size it asks for.
        public Constraints(Component c) {
            this.c = c;
            setX(Spring.constant(c.getX()));
            setY(Spring.constant(c.getY()));
            setWidth(Spring.width(c));
            setHeight(Spring.height(c));
        }

        // Records that `name` was constrained. A name set again moves to
        // the front; a third name on an axis pushes the oldest out, and
        // every spring that no longer has a name behind it is forgotten
        // so that it is derived from the remaining two.
        private void pushConstraint(String name, Spring value, boolean horizontal) {
            boolean valid = true;
            List<String> history = horizontal ? horizontalHistory : verticalHistory;
            if (history.contains(name)) {
                history.remove(name);
                valid = false;
            } else if (history.size() == 2 && value != null) {
                history.remove(0);
                valid = false;
            }
            if (value != null) {
                history.add(name);
            }
            if (!valid) {
                String[] all = horizontal ? ALL_HORIZONTAL : ALL_VERTICAL;
                for (int i = 0; i < all.length; i++) {
                    if (!history.contains(all[i])) {
                        setConstraint(all[i], null);
                    }
                }
            }
        }

        private static Spring sum(Spring s1, Spring s2) {
            return (s1 == null || s2 == null) ? null : Spring.sum(s1, s2);
        }

        private static Spring difference(Spring s1, Spring s2) {
            return (s1 == null || s2 == null) ? null : Spring.sum(s1, Spring.minus(s2));
        }

        private static Spring scale(Spring s, float factor) {
            return s == null ? null : Spring.scale(s, factor);
        }

        private int baselineFromHeight(int h) {
            if (h < 0) {
                // A negated height asks for the baseline measured the
                // other way round.
                return -c.getBaseline(c.getPreferredSize().width, -h);
            }
            return c.getBaseline(c.getPreferredSize().width, h);
        }

        private int heightFromBaseline(int b) {
            Dimension pref = c.getPreferredSize();
            if (c.getBaseline(pref.width, pref.height) == b) {
                return pref.height;
            }
            // How the baseline moves with the height is not known.
            return Spring.UNSET;
        }

        private Spring heightToRelativeBaseline(Spring s) {
            return new Spring.Mapped(s) {
                @Override
                int map(int i) {
                    return baselineFromHeight(i);
                }

                @Override
                int inv(int i) {
                    return heightFromBaseline(i);
                }
            };
        }

        private Spring relativeBaselineToHeight(Spring s) {
            return new Spring.Mapped(s) {
                @Override
                int map(int i) {
                    return heightFromBaseline(i);
                }

                @Override
                int inv(int i) {
                    return baselineFromHeight(i);
                }
            };
        }

        // Two constraints that name neither the origin nor the size of an
        // axis would each be derived from the other. A far edge and the
        // centre give the origin directly: the centre lies half way
        // between the two edges. The size then follows from the far edge.
        private void deriveWidthFromPair() {
            if (width == null && x == null && horizontalHistory.contains(EAST)
                    && horizontalHistory.contains(HORIZONTAL_CENTER)) {
                x = difference(scale(horizontalCenter, 2f), east);
            }
        }

        private void deriveHeightFromPair() {
            if (height != null || y != null || verticalHistory.size() < 2 || verticalHistory.contains(NORTH)
                    || verticalHistory.contains(HEIGHT)) {
                return;
            }
            if (verticalHistory.contains(SOUTH) && verticalHistory.contains(VERTICAL_CENTER)) {
                y = difference(scale(verticalCenter, 2f), south);
            } else {
                // One of the two is the baseline, and how it moves with
                // the height is not known: the component keeps the height
                // it asks for and the older constraint gives way.
                height = c == null ? Spring.constant(0) : Spring.height(c);
            }
        }

        /// Sets the spring controlling the left edge.
        public void setX(Spring x) {
            this.x = x;
            pushConstraint(WEST, x, true);
        }

        /// Returns the spring controlling the left edge, derived from
        /// the other horizontal constraints when it was not set.
        public Spring getX() {
            deriveWidthFromPair();
            if (x == null) {
                if (horizontalHistory.contains(EAST)) {
                    x = difference(east, getWidth());
                } else if (horizontalHistory.contains(HORIZONTAL_CENTER)) {
                    x = difference(horizontalCenter, scale(getWidth(), 0.5f));
                }
            }
            return x;
        }

        /// Sets the spring controlling the top edge.
        public void setY(Spring y) {
            this.y = y;
            pushConstraint(NORTH, y, false);
        }

        /// Returns the spring controlling the top edge, derived from the
        /// other vertical constraints when it was not set.
        public Spring getY() {
            deriveHeightFromPair();
            if (y == null) {
                if (verticalHistory.contains(SOUTH)) {
                    y = difference(south, getHeight());
                } else if (verticalHistory.contains(VERTICAL_CENTER)) {
                    y = difference(verticalCenter, scale(getHeight(), 0.5f));
                } else if (verticalHistory.contains(BASELINE)) {
                    y = difference(baseline, heightToRelativeBaseline(getHeight()));
                }
            }
            return y;
        }

        /// Sets the spring controlling the width.
        public void setWidth(Spring width) {
            this.width = width;
            pushConstraint(WIDTH, width, true);
        }

        /// Returns the spring controlling the width, derived from the
        /// other horizontal constraints when it was not set.
        public Spring getWidth() {
            deriveWidthFromPair();
            if (width == null) {
                if (horizontalHistory.contains(EAST)) {
                    width = difference(east, getX());
                } else if (horizontalHistory.contains(HORIZONTAL_CENTER)) {
                    width = scale(difference(horizontalCenter, getX()), 2f);
                }
            }
            return width;
        }

        /// Sets the spring controlling the height.
        public void setHeight(Spring height) {
            this.height = height;
            pushConstraint(HEIGHT, height, false);
        }

        /// Returns the spring controlling the height, derived from the
        /// other vertical constraints when it was not set.
        public Spring getHeight() {
            deriveHeightFromPair();
            if (height == null) {
                if (verticalHistory.contains(SOUTH)) {
                    height = difference(south, getY());
                } else if (verticalHistory.contains(VERTICAL_CENTER)) {
                    height = scale(difference(verticalCenter, getY()), 2f);
                } else if (verticalHistory.contains(BASELINE)) {
                    height = relativeBaselineToHeight(difference(baseline, getY()));
                }
            }
            return height;
        }

        private void setEast(Spring east) {
            this.east = east;
            pushConstraint(EAST, east, true);
        }

        private Spring getEast() {
            if (east == null) {
                east = sum(getX(), getWidth());
            }
            return east;
        }

        private void setSouth(Spring south) {
            this.south = south;
            pushConstraint(SOUTH, south, false);
        }

        private Spring getSouth() {
            if (south == null) {
                south = sum(getY(), getHeight());
            }
            return south;
        }

        private void setHorizontalCenter(Spring horizontalCenter) {
            this.horizontalCenter = horizontalCenter;
            pushConstraint(HORIZONTAL_CENTER, horizontalCenter, true);
        }

        private Spring getHorizontalCenter() {
            if (horizontalCenter == null) {
                horizontalCenter = sum(getX(), scale(getWidth(), 0.5f));
            }
            return horizontalCenter;
        }

        private void setVerticalCenter(Spring verticalCenter) {
            this.verticalCenter = verticalCenter;
            pushConstraint(VERTICAL_CENTER, verticalCenter, false);
        }

        private Spring getVerticalCenter() {
            if (verticalCenter == null) {
                verticalCenter = sum(getY(), scale(getHeight(), 0.5f));
            }
            return verticalCenter;
        }

        private void setBaseline(Spring baseline) {
            this.baseline = baseline;
            pushConstraint(BASELINE, baseline, false);
        }

        private Spring getBaseline() {
            if (baseline == null) {
                baseline = sum(getY(), heightToRelativeBaseline(getHeight()));
            }
            return baseline;
        }

        /// Sets the spring controlling the named edge or size; a name
        /// that is none of the constants is ignored.
        public void setConstraint(String edgeName, Spring s) {
            if (WEST.equals(edgeName)) {
                setX(s);
            } else if (NORTH.equals(edgeName)) {
                setY(s);
            } else if (EAST.equals(edgeName)) {
                setEast(s);
            } else if (SOUTH.equals(edgeName)) {
                setSouth(s);
            } else if (HORIZONTAL_CENTER.equals(edgeName)) {
                setHorizontalCenter(s);
            } else if (WIDTH.equals(edgeName)) {
                setWidth(s);
            } else if (HEIGHT.equals(edgeName)) {
                setHeight(s);
            } else if (VERTICAL_CENTER.equals(edgeName)) {
                setVerticalCenter(s);
            } else if (BASELINE.equals(edgeName)) {
                setBaseline(s);
            }
        }

        /// Returns the spring controlling the named edge or size, or
        /// `null` for a name that is none of the constants.
        public Spring getConstraint(String edgeName) {
            if (WEST.equals(edgeName)) {
                return getX();
            } else if (NORTH.equals(edgeName)) {
                return getY();
            } else if (EAST.equals(edgeName)) {
                return getEast();
            } else if (SOUTH.equals(edgeName)) {
                return getSouth();
            } else if (WIDTH.equals(edgeName)) {
                return getWidth();
            } else if (HEIGHT.equals(edgeName)) {
                return getHeight();
            } else if (HORIZONTAL_CENTER.equals(edgeName)) {
                return getHorizontalCenter();
            } else if (VERTICAL_CENTER.equals(edgeName)) {
                return getVerticalCenter();
            } else if (BASELINE.equals(edgeName)) {
                return getBaseline();
            }
            return null;
        }

        void cn1Reset() {
            cn1Clear(x);
            cn1Clear(y);
            cn1Clear(width);
            cn1Clear(height);
            cn1Clear(east);
            cn1Clear(south);
            cn1Clear(horizontalCenter);
            cn1Clear(verticalCenter);
            cn1Clear(baseline);
        }

        private static void cn1Clear(Spring s) {
            if (s != null) {
                s.setValue(Spring.UNSET);
            }
        }
    }

    /// An edge of a component, looked up each time it is asked: the
    /// constraint behind an edge may be replaced after another constraint
    /// was tied to it.
    private static final class EdgeSpring extends Spring {
        private final String edgeName;
        private final Component c;
        private final SpringLayout layout;

        EdgeSpring(String edgeName, Component c, SpringLayout layout) {
            this.edgeName = edgeName;
            this.c = c;
            this.layout = layout;
        }

        private Spring target() {
            return layout.getConstraints(c).getConstraint(edgeName);
        }

        @Override
        public int getMinimumValue() {
            return target().getMinimumValue();
        }

        @Override
        public int getPreferredValue() {
            return target().getPreferredValue();
        }

        @Override
        public int getMaximumValue() {
            return target().getMaximumValue();
        }

        @Override
        public int getValue() {
            return target().getValue();
        }

        @Override
        public void setValue(int size) {
            target().setValue(size);
        }

        @Override
        boolean cn1IsCyclic(SpringLayout l) {
            return l.cn1IsCyclic(target());
        }

        @Override
        public String toString() {
            return "SpringProxy for " + edgeName + " edge of " + c.getName() + ".";
        }
    }

    private void resetCyclicStatuses() {
        cyclicSprings = new ArrayList<Spring>();
        acyclicSprings = new ArrayList<Spring>();
    }

    private static boolean containsIdentical(List<Spring> list, Spring s) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i) == s) {
                return true;
            }
        }
        return false;
    }

    private static void removeIdentical(List<Spring> list, Spring s) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i) == s) {
                list.remove(i);
                return;
            }
        }
    }

    private void setParent(Container p) {
        resetCyclicStatuses();
        Constraints pc = getConstraints(p);
        pc.setX(Spring.constant(0));
        pc.setY(Spring.constant(0));
        // The default width and height of a component ask the component
        // for its sizes. For the container that would come straight back
        // here, so they are replaced with springs free to take any value.
        Spring width = pc.getWidth();
        if (width instanceof Spring.SizeOf && ((Spring.SizeOf) width).c == p) {
            pc.setWidth(Spring.constant(0, 0, Integer.MAX_VALUE));
        }
        Spring height = pc.getHeight();
        if (height instanceof Spring.SizeOf && ((Spring.SizeOf) height).c == p) {
            pc.setHeight(Spring.constant(0, 0, Integer.MAX_VALUE));
        }
    }

    boolean cn1IsCyclic(Spring s) {
        if (s == null) {
            return false;
        }
        if (containsIdentical(cyclicSprings, s)) {
            return true;
        }
        if (containsIdentical(acyclicSprings, s)) {
            return false;
        }
        cyclicSprings.add(s);
        boolean result = s.cn1IsCyclic(this);
        if (!result) {
            acyclicSprings.add(s);
            removeIdentical(cyclicSprings, s);
        } else {
            System.err.println(s + " is cyclic. ");
        }
        return result;
    }

    private Spring abandonCycles(Spring s) {
        return cn1IsCyclic(s) ? cyclicReference : s;
    }

    /// Has no effect: this layout does not use a string per component.
    @Override
    public void addLayoutComponent(String name, Component c) {
    }

    /// Removes the constraints associated with the component.
    @Override
    public void removeLayoutComponent(Component c) {
        componentConstraints.remove(c);
    }

    private static Dimension addInsets(int width, int height, Container p) {
        Insets i = p.getInsets();
        return new Dimension(width + i.left + i.right, height + i.top + i.bottom);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        setParent(parent);
        Constraints pc = getConstraints(parent);
        return addInsets(abandonCycles(pc.getWidth()).getMinimumValue(),
                abandonCycles(pc.getHeight()).getMinimumValue(), parent);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        setParent(parent);
        Constraints pc = getConstraints(parent);
        return addInsets(abandonCycles(pc.getWidth()).getPreferredValue(),
                abandonCycles(pc.getHeight()).getPreferredValue(), parent);
    }

    @Override
    public Dimension maximumLayoutSize(Container parent) {
        setParent(parent);
        Constraints pc = getConstraints(parent);
        return addInsets(abandonCycles(pc.getWidth()).getMaximumValue(),
                abandonCycles(pc.getHeight()).getMaximumValue(), parent);
    }

    /// Associates `constraints` with the component when it is a
    /// [SpringLayout.Constraints]; anything else is ignored.
    @Override
    public void addLayoutComponent(Component component, Object constraints) {
        if (constraints instanceof Constraints) {
            putConstraints(component, (Constraints) constraints);
        }
    }

    /// Returns 0.5f, centred.
    @Override
    public float getLayoutAlignmentX(Container p) {
        return 0.5f;
    }

    /// Returns 0.5f, centred.
    @Override
    public float getLayoutAlignmentY(Container p) {
        return 0.5f;
    }

    @Override
    public void invalidateLayout(Container p) {
    }

    /// Ties edge `e1` of `c1` to edge `e2` of `c2` with a fixed distance
    /// between them: `value(e1, c1) = value(e2, c2) + pad`.
    public void putConstraint(String e1, Component c1, int pad, String e2, Component c2) {
        putConstraint(e1, c1, Spring.constant(pad), e2, c2);
    }

    /// Ties edge `e1` of `c1` to edge `e2` of `c2` with the spring `s`
    /// between them.
    public void putConstraint(String e1, Component c1, Spring s, String e2, Component c2) {
        putConstraint(e1, c1, Spring.sum(s, getConstraint(e2, c2)));
    }

    private void putConstraint(String e, Component c, Spring s) {
        if (s != null) {
            getConstraints(c).setConstraint(e, s);
        }
    }

    // Completes an axis that has fewer than two constraints: the origin
    // and the size the component asks for, leaving whatever was given
    // the most recent.
    private Constraints applyDefaults(Component c, Constraints given) {
        Constraints constraints = given;
        if (constraints == null) {
            constraints = new Constraints();
        }
        if (constraints.c == null) {
            constraints.c = c;
        }
        if (constraints.horizontalHistory.size() < 2) {
            applyDefaults(constraints, WEST, Spring.constant(0), WIDTH, Spring.width(c),
                    constraints.horizontalHistory);
        }
        if (constraints.verticalHistory.size() < 2) {
            applyDefaults(constraints, NORTH, Spring.constant(0), HEIGHT, Spring.height(c),
                    constraints.verticalHistory);
        }
        return constraints;
    }

    private static void applyDefaults(Constraints constraints, String name1, Spring spring1, String name2,
            Spring spring2, List<String> history) {
        if (history.isEmpty()) {
            constraints.setConstraint(name1, spring1);
            constraints.setConstraint(name2, spring2);
        } else {
            // Exactly one constraint is defined. The size is the default
            // of choice; if it is the one defined, the origin is.
            if (constraints.getConstraint(name2) == null) {
                constraints.setConstraint(name2, spring2);
            } else {
                constraints.setConstraint(name1, spring1);
            }
            // Either way the given constraint stays the most recent.
            if (history.size() == 2) {
                String first = history.remove(0);
                history.add(first);
            }
        }
    }

    private void putConstraints(Component component, Constraints constraints) {
        componentConstraints.put(component, applyDefaults(component, constraints));
    }

    /// Returns the constraints of a component, creating them when it has
    /// none. Changing them changes the layout.
    public Constraints getConstraints(Component c) {
        Constraints result = componentConstraints.get(c);
        if (result == null) {
            if (c instanceof JComponent) {
                Object given = ((JComponent) c).getClientProperty(SpringLayout.class);
                if (given instanceof Constraints) {
                    return applyDefaults(c, (Constraints) given);
                }
            }
            result = new Constraints();
            putConstraints(c, result);
        }
        return result;
    }

    /// Returns a spring that follows the named edge of a component,
    /// whatever constraint ends up behind that edge.
    public Spring getConstraint(String edgeName, Component c) {
        return new EdgeSpring(edgeName, c, this);
    }

    @Override
    public void layoutContainer(Container parent) {
        setParent(parent);

        int n = parent.getComponentCount();
        getConstraints(parent).cn1Reset();
        for (int i = 0; i < n; i++) {
            getConstraints(parent.getComponent(i)).cn1Reset();
        }

        Insets insets = parent.getInsets();
        Constraints pc = getConstraints(parent);
        abandonCycles(pc.getX()).setValue(0);
        abandonCycles(pc.getY()).setValue(0);
        abandonCycles(pc.getWidth()).setValue(parent.getWidth() - insets.left - insets.right);
        abandonCycles(pc.getHeight()).setValue(parent.getHeight() - insets.top - insets.bottom);

        for (int i = 0; i < n; i++) {
            Component c = parent.getComponent(i);
            Constraints cc = getConstraints(c);
            int x = abandonCycles(cc.getX()).getValue();
            int y = abandonCycles(cc.getY()).getValue();
            int width = abandonCycles(cc.getWidth()).getValue();
            int height = abandonCycles(cc.getHeight()).getValue();
            c.setBounds(insets.left + x, insets.top + y, width, height);
        }
    }
}
