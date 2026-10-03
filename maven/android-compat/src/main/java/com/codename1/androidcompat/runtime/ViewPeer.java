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
package com.codename1.androidcompat.runtime;

import android.view.View;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;

/// The Codename One component behind a leaf view. It has no style of its own --
/// the view draws everything -- and takes no pointer events: touches enter at
/// the activity root and are dispatched by the Android view tree.
public class ViewPeer extends Component {

    private final View view;

    public ViewPeer(View view) {
        this.view = view;
        strip(this);
        setFocusable(false);
    }

    public View getView() {
        return view;
    }

    /// Removes every trace of theme styling: the view draws its own
    /// background, padding is Android's, and nothing else may add pixels.
    public static void strip(Component c) {
        c.setUIID("AndroidView");
        Style s = c.getAllStyles();
        s.setBgTransparency(0);
        s.setBgImage(null);
        s.setBorder(Border.createEmpty());
        s.setPadding(0, 0, 0, 0);
        s.setMargin(0, 0, 0, 0);
    }

    @Override
    public boolean isIgnorePointerEvents() {
        return true;
    }

    @Override
    public void paint(Graphics g) {
        view.paintPeer(g, getX(), getY());
    }

    @Override
    protected void paintBackground(Graphics g) {
    }

    @Override
    protected Dimension calcPreferredSize() {
        return measureUnbounded(view);
    }

    /// The view's size when measured without constraints, as Codename One's
    /// preferred size; only used when a view is placed in a Codename One layout.
    static Dimension measureUnbounded(View view) {
        com.codename1.ui.Display d = com.codename1.ui.Display.getInstance();
        view.measure(View.MeasureSpec.makeMeasureSpec(d.getDisplayWidth(), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return new Dimension(view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    /// Whether `peer` is a root view's peer placed by a Codename One layout,
    /// whose position therefore must not be overwritten by the view's frame.
    public static boolean isRootPlaced(Component peer) {
        Container parent = peer.getParent();
        return parent != null && !(parent instanceof GroupPeer);
    }

    /// Asks Codename One to lay out the tree whose root peer is `peer`.
    public static void scheduleLayout(Component peer) {
        if (peer instanceof Container) {
            ((Container) peer).revalidateLater();
        } else if (peer.getParent() != null) {
            peer.getParent().revalidateLater();
        }
    }
}
