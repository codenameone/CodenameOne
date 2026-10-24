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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.LayoutManager2;

/// Places components in a single row or a single column, honouring each
/// one's minimum, preferred and maximum size and its alignment across the
/// row.
///
/// There is no component orientation in this layer: `LINE_AXIS` is the same
/// as `X_AXIS` and `PAGE_AXIS` the same as `Y_AXIS`. Where the JDK throws
/// `AWTError` -- an axis that is none of the four, or a layout handed a
/// container other than its own -- this throws an `IllegalArgumentException`.
public class BoxLayout implements LayoutManager2 {

    public static final int X_AXIS = 0;

    public static final int Y_AXIS = 1;

    public static final int LINE_AXIS = 2;

    public static final int PAGE_AXIS = 3;

    private final int axis;
    private final Container target;

    public BoxLayout(Container target, int axis) {
        if (axis != X_AXIS && axis != Y_AXIS && axis != LINE_AXIS && axis != PAGE_AXIS) {
            throw new IllegalArgumentException("Invalid axis");
        }
        this.axis = axis;
        this.target = target;
    }

    public final Container getTarget() {
        return target;
    }

    public final int getAxis() {
        return axis;
    }

    @Override
    public void invalidateLayout(Container target) {
        check(target);
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
    }

    private void check(Container c) {
        if (target != c) {
            throw new IllegalArgumentException("BoxLayout can't be shared");
        }
    }

    private boolean horizontal() {
        return axis == X_AXIS || axis == LINE_AXIS;
    }

    /// The requirements of every child along one axis; a hidden child asks
    /// for nothing but keeps its alignment.
    private SizeRequirements[] requests(boolean x) {
        int n = target.getComponentCount();
        SizeRequirements[] out = new SizeRequirements[n];
        for (int i = 0; i < n; i++) {
            Component c = target.getComponent(i);
            if (!c.isVisible()) {
                out[i] = new SizeRequirements(0, 0, 0, x ? c.getAlignmentX() : c.getAlignmentY());
                continue;
            }
            Dimension min = c.getMinimumSize();
            Dimension pref = c.getPreferredSize();
            Dimension max = c.getMaximumSize();
            out[i] = x ? new SizeRequirements(min.width, pref.width, max.width, c.getAlignmentX())
                    : new SizeRequirements(min.height, pref.height, max.height, c.getAlignmentY());
        }
        return out;
    }

    private SizeRequirements total(SizeRequirements[] children, boolean x) {
        return x == horizontal() ? SizeRequirements.getTiledSizeRequirements(children)
                : SizeRequirements.getAlignedSizeRequirements(children);
    }

    private static int clamp(long v) {
        return v > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) v;
    }

    private Dimension size(Container c, int which) {
        check(c);
        SizeRequirements xt = total(requests(true), true);
        SizeRequirements yt = total(requests(false), false);
        int w = which == 0 ? xt.minimum : which == 1 ? xt.preferred : xt.maximum;
        int h = which == 0 ? yt.minimum : which == 1 ? yt.preferred : yt.maximum;
        Insets in = c.getInsets();
        return new Dimension(clamp((long) w + (long) in.left + (long) in.right),
                clamp((long) h + (long) in.top + (long) in.bottom));
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return size(target, 1);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return size(target, 0);
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
        return size(target, 2);
    }

    @Override
    public float getLayoutAlignmentX(Container target) {
        check(target);
        return total(requests(true), true).alignment;
    }

    @Override
    public float getLayoutAlignmentY(Container target) {
        check(target);
        return total(requests(false), false).alignment;
    }

    @Override
    public void layoutContainer(Container target) {
        check(target);
        int n = target.getComponentCount();
        int[] xo = new int[n];
        int[] xs = new int[n];
        int[] yo = new int[n];
        int[] ys = new int[n];
        SizeRequirements[] xc = requests(true);
        SizeRequirements[] yc = requests(false);
        SizeRequirements xt = total(xc, true);
        SizeRequirements yt = total(yc, false);
        Insets in = target.getInsets();
        int w = target.getWidth() - in.left - in.right;
        int h = target.getHeight() - in.top - in.bottom;
        if (horizontal()) {
            SizeRequirements.calculateTiledPositions(w, xt, xc, xo, xs, true);
            SizeRequirements.calculateAlignedPositions(h, yt, yc, yo, ys);
        } else {
            SizeRequirements.calculateAlignedPositions(w, xt, xc, xo, xs, true);
            SizeRequirements.calculateTiledPositions(h, yt, yc, yo, ys);
        }
        for (int i = 0; i < n; i++) {
            target.getComponent(i).setBounds(clamp((long) in.left + (long) xo[i]),
                    clamp((long) in.top + (long) yo[i]), xs[i], ys[i]);
        }
    }
}
