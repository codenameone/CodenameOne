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
package javafx.scene.layout;

import java.util.List;

import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;

/// What the layout panes share: the per child constraints, the keyword
/// forms of the styled values, the offsets of an alignment, baselines and
/// the distribution of spare space along a row or a column of children.
final class LayoutSupport {

    private static final Pos[] POSITIONS = Pos.values();
    private static final Orientation[] ORIENTATIONS = Orientation.values();

    private LayoutSupport() {
    }

    // ------------------------------------------------------- constraints

    static Insets insets(Node child, Object key) {
        Object v = Pane.getConstraint(child, key);
        return v instanceof Insets ? (Insets) v : null;
    }

    static Priority priority(Node child, Object key) {
        Object v = Pane.getConstraint(child, key);
        return v instanceof Priority ? (Priority) v : null;
    }

    static Pos pos(Node child, Object key) {
        Object v = Pane.getConstraint(child, key);
        return v instanceof Pos ? (Pos) v : null;
    }

    static Double number(Node child, Object key) {
        Object v = Pane.getConstraint(child, key);
        return v instanceof Double ? (Double) v : null;
    }

    static Integer integer(Node child, Object key) {
        Object v = Pane.getConstraint(child, key);
        return v instanceof Integer ? (Integer) v : null;
    }

    // ------------------------------------------------------ styled values

    private static boolean sameKeyword(String name, String text) {
        if (name.length() != text.length()) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char a = name.charAt(i);
            char b = text.charAt(i);
            if (a == '_') {
                a = '-';
            }
            if (b == '_') {
                b = '-';
            }
            if (a >= 'A' && a <= 'Z') {
                a = (char) (a + ('a' - 'A'));
            }
            if (b >= 'A' && b <= 'Z') {
                b = (char) (b + ('a' - 'A'));
            }
            if (a != b) {
                return false;
            }
        }
        return true;
    }

    /// Returns the position a styled value stands for: the constant, or
    /// its CSS spelling (`center-left`); `null` for anything else.
    static Pos toPos(Object value) {
        if (value instanceof Pos) {
            return (Pos) value;
        } else if (value instanceof String) {
            String s = ((String) value).trim();
            for (int i = 0; i < POSITIONS.length; i++) {
                if (sameKeyword(POSITIONS[i].name(), s)) {
                    return POSITIONS[i];
                }
            }
        }
        return null;
    }

    /// Returns the orientation a styled value stands for, or `null`.
    static Orientation toOrientation(Object value) {
        if (value instanceof Orientation) {
            return (Orientation) value;
        } else if (value instanceof String) {
            String s = ((String) value).trim();
            for (int i = 0; i < ORIENTATIONS.length; i++) {
                if (sameKeyword(ORIENTATIONS[i].name(), s)) {
                    return ORIENTATIONS[i];
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------- offsets

    static double xOffset(double available, double content, HPos pos) {
        if (pos == HPos.CENTER) {
            return (available - content) / 2;
        } else if (pos == HPos.RIGHT) {
            return available - content;
        }
        return 0;
    }

    static double yOffset(double available, double content, VPos pos) {
        if (pos == VPos.CENTER) {
            return (available - content) / 2;
        } else if (pos == VPos.BOTTOM) {
            return available - content;
        }
        return 0;
    }

    /// A share of spare space on a device pixel boundary, rounded towards
    /// zero so that the shares never add up to more than there is.
    static double portion(Region pane, double value) {
        if (!pane.isSnapToPixel()) {
            return value;
        }
        double s = com.codename1.fxcompat.runtime.Units.scale();
        if (s <= 0) {
            return value;
        }
        double scaled = value * s;
        return (scaled >= 0 ? Math.floor(scaled + 1e-6) : Math.ceil(scaled - 1e-6)) / s;
    }

    // --------------------------------------------------------- baselines

    /// Returns the height a child takes when it is not stretched, without
    /// its margin.
    static double restHeight(Node child) {
        if (!child.isResizable()) {
            return child.getLayoutBounds().getHeight();
        }
        return Region.boundedSize(child.minHeight(-1), child.prefHeight(-1), child.maxHeight(-1));
    }

    /// Returns the distance from the top of a child of a given height to
    /// its baseline: the whole height for a child without text.
    static double baseline(Node child, double height) {
        double offset = child.getBaselineOffset();
        if (Double.compare(offset, Node.BASELINE_OFFSET_SAME_AS_HEIGHT) == 0) {
            return height;
        }
        if (child.isResizable()) {
            // A resizable node that reports its current bottom edge has
            // no baseline of its own; the edge moves with the height.
            Bounds lb = child.getLayoutBounds();
            if (Double.compare(offset, lb.getMinY() + lb.getHeight()) == 0) {
                return height;
            }
        }
        return offset;
    }

    static double top(Insets margin) {
        return margin == null ? 0 : margin.getTop();
    }

    static double bottom(Insets margin) {
        return margin == null ? 0 : margin.getBottom();
    }

    /// Returns the baseline of an area shared by several children: the
    /// largest distance from the top of the area to a child's baseline.
    static double areaBaseline(Region pane, List<Node> children, Object marginKey) {
        double max = 0;
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            double above = pane.snapSpaceY(top(insets(child, marginKey))) + baseline(child, restHeight(child));
            if (above > max) {
                max = above;
            }
        }
        return max;
    }

    /// Returns the height of an area whose children sit on one baseline.
    static double baselineAreaHeight(Region pane, List<Node> children, Object marginKey) {
        double above = 0;
        double below = 0;
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            Insets m = insets(child, marginKey);
            double h = restHeight(child);
            double b = baseline(child, h);
            above = Math.max(above, pane.snapSpaceY(top(m)) + b);
            below = Math.max(below, h - b + pane.snapSpaceY(bottom(m)));
        }
        return above + below;
    }

    /// Places a child with its baseline on the baseline of an area. The
    /// child keeps its preferred height.
    static void layoutOnBaseline(Region pane, Node child, double x, double y, double width, double height,
            double areaBaseline, Insets margin, boolean fillWidth, HPos hpos) {
        Insets m = margin == null ? Insets.EMPTY : margin;
        double shift = areaBaseline - pane.snapSpaceY(m.getTop()) - baseline(child, restHeight(child));
        Insets moved = new Insets(m.getTop() + (shift > 0 ? shift : 0), m.getRight(), m.getBottom(), m.getLeft());
        pane.layoutInArea(child, x, y, width, height, 0, moved, fillWidth, false, hpos, VPos.TOP);
    }

    // ------------------------------------------------- rows and columns

    /// Returns the length along a box of the area of each child: its
    /// minimum or its preferred size plus its margin.
    static double[] areaSizes(Region pane, List<Node> children, boolean horizontal, Object marginKey, double cross,
            boolean minimum) {
        double[] sizes = new double[children.size()];
        for (int i = 0; i < sizes.length; i++) {
            Node child = children.get(i);
            Insets m = insets(child, marginKey);
            if (horizontal) {
                sizes[i] = minimum ? pane.computeChildMinAreaWidth(child, m, cross)
                        : pane.computeChildPrefAreaWidth(child, m, cross);
            } else {
                sizes[i] = minimum ? pane.computeChildMinAreaHeight(child, m, cross)
                        : pane.computeChildPrefAreaHeight(child, m, cross);
            }
        }
        return sizes;
    }

    static double sum(double[] sizes, double gap) {
        double total = 0;
        for (int i = 0; i < sizes.length; i++) {
            total += sizes[i];
        }
        return sizes.length > 1 ? total + gap * (sizes.length - 1) : total;
    }

    /// Hands spare space to the areas that may take it, or takes missing
    /// space from all of them, in equal shares within each area's limit.
    /// `limits` holds the limit of each area taking part and NaN for the
    /// others. Returns what could not be handed out.
    static double distribute(Region pane, double[] sizes, double[] limits, double extra) {
        int taking = 0;
        for (int i = 0; i < limits.length; i++) {
            if (!Double.isNaN(limits[i])) {
                taking++;
            }
        }
        double available = extra;
        boolean done = false;
        while (!done && Math.abs(available) > 1 && taking > 0) {
            double share = portion(pane, available / taking);
            if (!(Math.abs(share) > 0)) {
                break;
            }
            for (int i = 0; i < sizes.length; i++) {
                if (Double.isNaN(limits[i])) {
                    continue;
                }
                double room = limits[i] - sizes[i];
                double change = Math.abs(room) <= Math.abs(share) ? room : share;
                if (extra > 0 ? change < 0 : change > 0) {
                    change = 0;
                }
                sizes[i] += change;
                available -= change;
                if (Math.abs(available) < 1) {
                    done = true;
                    break;
                }
                if (Math.abs(change) < Math.abs(share)) {
                    limits[i] = Double.NaN;
                    taking--;
                }
            }
        }
        return available;
    }

    /// Fits the areas of a box to the length it has: grows the children
    /// that always grow, then those that sometimes do, or shrinks them
    /// all towards their minimum. Returns the length used, gaps included.
    static double fit(Region pane, List<Node> children, boolean horizontal, Object marginKey, Object growKey,
            double[] sizes, double gap, double length, double cross) {
        double content = sum(sizes, gap);
        double extra = length - content;
        if (sizes.length == 0 || !(Math.abs(extra) > 1)) {
            return content;
        }
        double remaining = extra;
        if (extra < 0) {
            double[] limits = areaSizes(pane, children, horizontal, marginKey, cross, true);
            remaining = distribute(pane, sizes, limits, remaining);
        } else {
            for (int pass = 0; pass < 2; pass++) {
                Priority wanted = pass == 0 ? Priority.ALWAYS : Priority.SOMETIMES;
                double[] limits = new double[sizes.length];
                for (int i = 0; i < limits.length; i++) {
                    Node child = children.get(i);
                    if (priority(child, growKey) == wanted) {
                        Insets m = insets(child, marginKey);
                        limits[i] = horizontal ? pane.computeChildMaxAreaWidth(child, m, cross)
                                : pane.computeChildMaxAreaHeight(child, m, cross);
                    } else {
                        limits[i] = Double.NaN;
                    }
                }
                remaining = distribute(pane, sizes, limits, remaining);
            }
        }
        return content + extra - remaining;
    }

    /// Returns the largest area across a box that its children ask for.
    static double maxCross(Region pane, List<Node> children, boolean horizontal, Object marginKey, double[] along,
            boolean minimum) {
        double max = 0;
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            Insets m = insets(child, marginKey);
            double given = along == null ? -1 : along[i];
            double v;
            if (horizontal) {
                v = minimum ? pane.computeChildMinAreaHeight(child, m, given)
                        : pane.computeChildPrefAreaHeight(child, m, given);
            } else {
                v = minimum ? pane.computeChildMinAreaWidth(child, m, given)
                        : pane.computeChildPrefAreaWidth(child, m, given);
            }
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    /// Returns the content bias of the first managed child that has one.
    static Orientation bias(List<Node> children) {
        Orientation found = null;
        for (int i = 0; i < children.size(); i++) {
            Orientation b = children.get(i).getContentBias();
            if (b != null) {
                found = b;
                if (b == Orientation.HORIZONTAL) {
                    break;
                }
            }
        }
        return found;
    }
}
