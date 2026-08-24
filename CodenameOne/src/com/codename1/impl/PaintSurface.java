/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.impl;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;

/// One paintable surface: its own dirty queue, its own `Graphics` and the routine
/// that drains the one into the other. The application's main surface is one of
/// these and each native window adds another, so the clip and paintable-bounds
/// handling that issue #5273 turns on cannot drift between them.
///
/// Before desktop windows existed this state was four fields on
/// `CodenameOneImplementation` and the routine was a method there. It lives here
/// instead so that opening a window adds an object rather than a set of public
/// methods to a class that is already the largest in the framework.
public final class PaintSurface {

    /// The implementation this surface paints through. A surface is meaningless
    /// without it: the flush, the overlay and the paintable bounds are all its.
    private final CodenameOneImplementation impl;

    /// The native window this surface draws into, or null for the main surface.
    private final Object nativeWindow;

    private Animation[] paintQueue = new Animation[200];
    private Animation[] paintQueueTemp = new Animation[200];
    private int paintQueueFill;
    private Graphics graphics;

    /// Scratch rectangle for the paintable bounds. Held per surface rather than
    /// shared, so the arithmetic of one surface's paint pass cannot be read by
    /// another's.
    private final Rectangle paintableBounds = new Rectangle();

    /// Creates a surface for a native window.
    ///
    /// #### Parameters
    ///
    /// - `impl`: the implementation that paints and flushes it
    ///
    /// - `nativeWindow`: the window peer the surface draws into, null for the
    /// application's main surface
    PaintSurface(CodenameOneImplementation impl, Object nativeWindow) {
        this.impl = impl;
        this.nativeWindow = nativeWindow;
    }

    /// The native window this surface draws into.
    ///
    /// #### Returns
    ///
    /// the native window peer, or null for the main surface
    Object getNativeWindow() {
        return nativeWindow;
    }

    /// The `Graphics` this surface paints through.
    ///
    /// #### Returns
    ///
    /// the graphics, or null before one has been installed
    Graphics getGraphics() {
        return graphics;
    }

    /// Installs the `Graphics` this surface paints through. `Graphics` cannot be
    /// constructed outside `com.codename1.ui`, so the framework creates it and hands
    /// it over here, exactly as it does for the main surface.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics to install
    public void setGraphics(Graphics g) {
        graphics = g;
    }

    /// Whether anything is queued on this surface.
    ///
    /// #### Returns
    ///
    /// true when the dirty queue is not empty
    boolean hasPendingPaints() {
        return paintQueueFill != 0;
    }

    /// Drops everything queued here, keeping the surface itself. A hidden window is
    /// not painted, so work queued on it would never drain -- and an undrained queue
    /// keeps `CodenameOneImplementation#hasPendingPaints()` true, which keeps the
    /// event dispatch thread awake spinning on it.
    public void clear() {
        synchronized (impl.displayLock) {
            paintQueueFill = 0;
            java.util.Arrays.fill(paintQueue, null);
            java.util.Arrays.fill(paintQueueTemp, null);
        }
    }

    /// Releases this surface, dropping anything still queued on it so a disposed
    /// window cannot pin its component tree, and unregistering it so nothing paints
    /// or sweeps it again.
    public void dispose() {
        synchronized (impl.displayLock) {
            paintQueueFill = 0;
            java.util.Arrays.fill(paintQueue, null);
            java.util.Arrays.fill(paintQueueTemp, null);
            graphics = null;
            impl.forgetWindowSurface(this);
        }
    }

    /// Removes an entry from the queue if it is there.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the animation to drop
    ///
    /// #### Returns
    ///
    /// true if it was found and dropped
    boolean cancelRepaint(Animation cmp) {
        for (int iter = 0; iter < paintQueueFill; iter++) {
            if (paintQueue[iter] == cmp) { //NOPMD CompareObjectsWithEquals
                paintQueue[iter] = null;
                return true;
            }
        }
        return false;
    }

    /// Queues an animation or component to be painted on this surface.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the animation or component to repaint
    public void repaint(Animation cmp) {
        synchronized (impl.displayLock) {
            for (int iter = 0; iter < paintQueueFill; iter++) {
                Animation ani = paintQueue[iter];
                if (ani == cmp) { //NOPMD CompareObjectsWithEquals
                    return;
                }
                //no need to paint a Component if one of its parent is already in the queue
                if (ani instanceof Container && cmp instanceof Component) {
                    Component parent = ((Component) cmp).getParent();
                    while (parent != null) {
                        if (parent == ani) { //NOPMD CompareObjectsWithEquals
                            return;
                        }
                        parent = parent.getParent();
                    }
                }
            }
            // overcrowding the queue don't try to grow the array!
            if (paintQueueFill >= paintQueue.length) {
                System.out.println("Warning paint queue size exceeded, please watch the amount of repaint calls");
                return;
            }

            paintQueue[paintQueueFill] = cmp;
            paintQueueFill++;
            impl.displayLock.notifyAll();
        }
    }

    /// Paints this surface's dirty regions.
    ///
    /// #### Parameters
    ///
    /// - `dwidth`: the surface width, used as the clip universe
    ///
    /// - `dheight`: the surface height, used as the clip universe
    public void paintDirty(int dwidth, int dheight) {
        if (graphics == null || dwidth <= 0 || dheight <= 0) {
            return;
        }
        int size = 0;
        synchronized (impl.displayLock) {
            size = paintQueueFill;
            Animation[] array = paintQueue;
            paintQueue = paintQueueTemp;
            paintQueueTemp = array;
            paintQueueFill = 0;
        }
        if (size > 0) {
            Graphics wrapper = graphics;
            int topX = dwidth;
            int topY = dheight;
            int bottomX = 0;
            int bottomY = 0;
            for (int iter = 0; iter < size; iter++) {
                Animation ani = paintQueueTemp[iter];

                // might happen due to paint queue removal
                if (ani == null) {
                    continue;
                }
                paintQueueTemp[iter] = null;
                wrapper.translate(-wrapper.getTranslateX(), -wrapper.getTranslateY());
                wrapper.resetAffine();
                // Reset the flush-region hint to the full screen before the
                // full-screen clip below so neither it nor a previous
                // component's tighter region wrongly clamps this reset (#5273).
                setDirtyRegionClip(0, 0, dwidth, dheight);
                wrapper.setClip(0, 0, dwidth, dheight);
                if (ani instanceof Component) {
                    Component cmp = (Component) ani;
                    Rectangle dirty = cmp.getDirtyRegion();
                    if (dirty != null) {
                        Dimension d = dirty.getSize();
                        wrapper.setClip(dirty.getX(), dirty.getY(), d.getWidth(), d.getHeight());
                        cmp.setDirtyRegion(null);
                    }
                    // Confine any clip this component sets while painting to its
                    // flushed region on immediate-mode ports. Use the paintable
                    // bounds -- the region retained ports clamp to via the
                    // flushGraphics call below -- NOT the dirty region, which
                    // repaint() nulls (Component.repaint), in which case it would
                    // fall back to the full screen and the clip could still escape
                    // (#5273). Computed before paintComponent (paint does not move
                    // the component) so the clip set during paint can be clamped.
                    impl.getPaintableBounds(cmp, paintableBounds);
                    setDirtyRegionClip(paintableBounds.getX(), paintableBounds.getY(),
                            paintableBounds.getWidth(), paintableBounds.getHeight());
                    cmp.paintComponent(wrapper);
                    // Recompute the paintable bounds AFTER paint for the flush
                    // region below: paintComponent can lay the component out (its
                    // bounds may change), and the retained ports clamp to / flush
                    // exactly this rect, so it must match the pre-#5273 value to
                    // the pixel (the before-paint value above is only the immediate
                    // -mode clip hint).
                    impl.getPaintableBounds(cmp, paintableBounds);
                    int cmpAbsX = paintableBounds.getX();
                    topX = Math.min(cmpAbsX, topX);
                    bottomX = Math.max(cmpAbsX + paintableBounds.getWidth(), bottomX);
                    int cmpAbsY = paintableBounds.getY();
                    topY = Math.min(cmpAbsY, topY);
                    bottomY = Math.max(cmpAbsY + paintableBounds.getHeight(), bottomY);
                } else {
                    bottomX = dwidth;
                    bottomY = dheight;
                    topX = 0;
                    topY = 0;
                    ani.paint(wrapper);
                }
            }

            if (nativeWindow == null) {
                impl.paintOverlay(wrapper);
                //Log.p("Flushing graphics : "+topX+","+topY+","+bottomX+","+bottomY);
                impl.flushGraphics(topX, topY, bottomX - topX, bottomY - topY);
            } else {
                WindowManager wm = impl.getWindowManager();
                if (wm != null) {
                    wm.flushGraphics(nativeWindow, topX, topY, bottomX - topX, bottomY - topY);
                }
            }
        }
    }

    /// Routes the flush-region hint to whichever surface this is. The window form
    /// defaults to inert rather than delegating to the main surface version, so a
    /// port that has not opted in cannot clamp a window's clip against the main
    /// window's state.
    private void setDirtyRegionClip(int x, int y, int w, int h) {
        if (nativeWindow == null) {
            impl.setPaintDirtyRegionClip(x, y, w, h);
            return;
        }
        WindowManager wm = impl.getWindowManager();
        if (wm != null) {
            wm.setPaintDirtyRegionClip(nativeWindow, x, y, w, h);
        }
    }
}
