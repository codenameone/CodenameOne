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

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;

/// The Codename One container behind a view group.
///
/// Its layout manager runs Android's measure and layout passes; it draws the
/// group's background and `onDraw` below the children and `dispatchDraw` and
/// the foreground above them. The peer of a *root* view group -- one with no
/// Android parent, such as an activity's decor or an Android view embedded in
/// a Codename One form -- is also where touches enter: it turns Codename One
/// pointer events into `MotionEvent`s and dispatches them down the tree.
public class GroupPeer extends Container {

    private final ViewGroup view;
    private long downTime;
    private boolean tracking;

    public GroupPeer(ViewGroup view) {
        super(new Bridge());
        this.view = view;
        ViewPeer.strip(this);
        setScrollable(false);
        setFocusable(false);
    }

    public ViewGroup getView() {
        return view;
    }

    public void addChildPeer(Component c, int index) {
        if (c.getParent() != null) {
            c.getParent().removeComponent(c);
        }
        if (index < 0 || index > getComponentCount()) {
            addComponent(c);
        } else {
            addComponent(index, c);
        }
    }

    public void removeChildPeer(Component c) {
        if (c.getParent() == this) {
            removeComponent(c);
        }
    }

    private boolean isRoot() {
        return view.getParent() == null;
    }

    @Override
    public boolean isIgnorePointerEvents() {
        return !isRoot();
    }

    @Override
    protected boolean isStickyDrag() {
        return isRoot();
    }

    private int childClipX;
    private int childClipY;
    private int childClipW;
    private int childClipH;
    private boolean hasChildClip;

    /// The area this group's children may paint in, in their coordinates:
    /// the group's own visible area, recorded when it last painted. Android
    /// clips a child that is rotated or scaled only by its parent, not by
    /// its own bounds; answers false before the first paint.
    public boolean childClip(int[] out) {
        if (!hasChildClip) {
            return false;
        }
        out[0] = childClipX;
        out[1] = childClipY;
        out[2] = childClipW;
        out[3] = childClipH;
        return true;
    }

    @Override
    public void paint(Graphics g) {
        if (isRoot()) {
            dispatchTreeDraw();
        }
        // Codename One paints the children translated by this peer's origin.
        childClipX = g.getClipX() - getX();
        childClipY = g.getClipY() - getY();
        childClipW = g.getClipWidth();
        childClipH = g.getClipHeight();
        hasChildClip = true;
        view.beginGroupChildren(g);
        super.paint(g);
        view.paintGroupAbove(g, getX(), getY());
    }

    /// The tree observer's pre-draw and draw listeners, before a root group
    /// paints. A pre-draw listener that cancels gets another pass at the
    /// next paint, which is scheduled now. The frame is painted all the same:
    /// Android keeps the previous frame on screen, but Codename One repaints
    /// this area from nothing, so skipping it would show a blank frame
    /// instead. A child repainted alone does not dispatch -- the root's next
    /// paint does.
    private void dispatchTreeDraw() {
        ViewTreeObserver obs = view.peekViewTreeObserver();
        if (obs == null) {
            return;
        }
        if (obs.dispatchOnPreDraw()) {
            repaint();
        }
        obs.dispatchOnDraw();
    }

    /// The Android background and `onDraw`. Codename One paints it before the
    /// children, and also as the backdrop when it repaints one child alone.
    @Override
    protected void paintBackground(Graphics g) {
        view.paintGroupBackground(g, getX(), getY());
    }

    @Override
    protected Dimension calcPreferredSize() {
        return ViewPeer.measureUnbounded(view);
    }

    // ------------------------------------------------------------ touch

    private MotionEvent event(int action, int x, int y) {
        return MotionEvent.create(action, x - getAbsoluteX(), y - getAbsoluteY(), x, y, downTime);
    }

    @Override
    public void pointerPressed(int x, int y) {
        if (!isRoot()) {
            return;
        }
        downTime = System.currentTimeMillis();
        tracking = true;
        AndroidRuntime.dispatchTouch(view, event(MotionEvent.ACTION_DOWN, x, y));
    }

    @Override
    public void pointerDragged(int x, int y) {
        if (!isRoot() || !tracking) {
            return;
        }
        AndroidRuntime.dispatchTouch(view, event(MotionEvent.ACTION_MOVE, x, y));
    }

    @Override
    public void pointerDragged(int[] x, int[] y) {
        pointerDragged(x[0], y[0]);
    }

    @Override
    public void pointerReleased(int x, int y) {
        if (!isRoot() || !tracking) {
            return;
        }
        tracking = false;
        AndroidRuntime.dispatchTouch(view, event(MotionEvent.ACTION_UP, x, y));
    }

    @Override
    public void pointerReleased(int[] x, int[] y) {
        pointerReleased(x[0], y[0]);
    }

    /// Codename One cancels a gesture it hands elsewhere (a native text
    /// editor, a dialog); Android views must see that as ACTION_CANCEL.
    public void cancelTouch() {
        if (tracking) {
            tracking = false;
            AndroidRuntime.dispatchTouch(view, event(MotionEvent.ACTION_CANCEL, getAbsoluteX(), getAbsoluteY()));
        }
    }

    // ------------------------------------------------------------ layout

    /// Runs Android's layout over a view group. Only a root group lays out its
    /// subtree; a nested group was positioned by its parent's pass already.
    static final class Bridge extends Layout {

        @Override
        public void layoutContainer(Container parent) {
            ViewGroup v = ((GroupPeer) parent).view;
            if (v.getParent() != null) {
                return;
            }
            int w = parent.getWidth();
            int h = parent.getHeight();
            if (w <= 0 && h <= 0) {
                return;
            }
            v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
            v.layout(0, 0, w, h);
            ViewTreeObserver obs = v.peekViewTreeObserver();
            if (obs != null) {
                obs.dispatchOnGlobalLayout();
            }
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            ViewGroup v = ((GroupPeer) parent).view;
            if (v.getParent() != null) {
                return new Dimension(v.getMeasuredWidth(), v.getMeasuredHeight());
            }
            return ViewPeer.measureUnbounded(v);
        }
    }
}
