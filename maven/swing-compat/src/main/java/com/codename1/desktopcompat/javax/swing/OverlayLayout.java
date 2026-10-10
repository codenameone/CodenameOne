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

/// Stacks components over one another, lining up the alignment point of
/// each on both axes.
///
/// A layout handed a container other than its own throws an
/// `IllegalArgumentException` where the JDK throws `AWTError`.
public class OverlayLayout implements LayoutManager2 {

    private final Container target;

    public OverlayLayout(Container target) {
        this.target = target;
    }

    public final Container getTarget() {
        return target;
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
            throw new IllegalArgumentException("OverlayLayout can't be shared");
        }
    }

    private SizeRequirements[] requests(boolean x) {
        int n = target.getComponentCount();
        SizeRequirements[] out = new SizeRequirements[n];
        for (int i = 0; i < n; i++) {
            Component c = target.getComponent(i);
            Dimension min = c.getMinimumSize();
            Dimension pref = c.getPreferredSize();
            Dimension max = c.getMaximumSize();
            out[i] = x ? new SizeRequirements(min.width, pref.width, max.width, c.getAlignmentX())
                    : new SizeRequirements(min.height, pref.height, max.height, c.getAlignmentY());
        }
        return out;
    }

    private Dimension size(Container c, int which) {
        check(c);
        SizeRequirements xt = SizeRequirements.getAlignedSizeRequirements(requests(true));
        SizeRequirements yt = SizeRequirements.getAlignedSizeRequirements(requests(false));
        int w = which == 0 ? xt.minimum : which == 1 ? xt.preferred : xt.maximum;
        int h = which == 0 ? yt.minimum : which == 1 ? yt.preferred : yt.maximum;
        Insets in = c.getInsets();
        return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
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
        return SizeRequirements.getAlignedSizeRequirements(requests(true)).alignment;
    }

    @Override
    public float getLayoutAlignmentY(Container target) {
        check(target);
        return SizeRequirements.getAlignedSizeRequirements(requests(false)).alignment;
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
        Insets in = target.getInsets();
        SizeRequirements.calculateAlignedPositions(target.getWidth() - in.left - in.right,
                SizeRequirements.getAlignedSizeRequirements(xc), xc, xo, xs);
        SizeRequirements.calculateAlignedPositions(target.getHeight() - in.top - in.bottom,
                SizeRequirements.getAlignedSizeRequirements(yc), yc, yo, ys);
        for (int i = 0; i < n; i++) {
            target.getComponent(i).setBounds(in.left + xo[i], in.top + yo[i], xs[i], ys[i]);
        }
    }
}
