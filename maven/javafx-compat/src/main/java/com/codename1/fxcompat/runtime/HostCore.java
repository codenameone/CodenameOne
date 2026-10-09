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
package com.codename1.fxcompat.runtime;

import java.util.List;

import java.util.concurrent.atomic.AtomicBoolean;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;

import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Window;

/// What the three kinds of host share: keeping the peer of the scene's
/// root in the content pane, and waiting for a window to close.
final class HostCore {

    private final Window window;
    private final Container content;
    private final StageHost host;
    private final AtomicBoolean closed = new AtomicBoolean();
    private Scene scene;
    private Component rootPeer;

    HostCore(Window window, Container content, StageHost host) {
        this.window = window;
        this.content = content;
        this.host = host;
        PeerPaint.strip(content);
        content.setLayout(new Fit());
        content.setScrollable(false);
    }

    /// Gives the root of the scene the whole content pane, and more than
    /// that where the scene cannot be made that small.
    ///
    /// JavaFX lays a scene out in whatever its window is and clips what
    /// does not fit, which on a desktop the user answers by making the
    /// window larger. A phone's window is the screen. So a root that has
    /// a minimum size larger than the pane is laid out at that minimum,
    /// and a root that is not resizable -- a group -- at the extent of
    /// what it holds, and the pane scrolls along each axis that
    /// overflows: nothing is laid out narrower than it can be, and all of
    /// it can be reached.
    ///
    /// A minimum is not always what the scene needs, though. A root that
    /// holds a group counts the group at its full size, and an
    /// application that scales that group to its window -- a game board
    /// that follows the size of its pane -- fits a pane far smaller than
    /// the minimum says. So a resizable root is first laid out in the
    /// pane as JavaFX would, and only an axis along which what it then
    /// holds reaches past the pane is given the minimum and scrolls. A
    /// minimum the application set on the root itself is taken as given.
    private final class Fit extends Layout {

        @Override
        public void layoutContainer(Container parent) {
            if (rootPeer == null) {
                return;
            }
            int w = parent.getWidth();
            int h = parent.getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            Dimension least = least();
            boolean overX = least.getWidth() > w;
            boolean overY = least.getHeight() > h;
            if (overX || overY) {
                Dimension reach = reach(w, h);
                // A minimum the application set itself is what it asked
                // for; only one worked out from the content is doubted.
                Parent root = scene.getRoot();
                boolean setX = root instanceof Region && ((Region) root).getMinWidth() >= 0;
                boolean setY = root instanceof Region && ((Region) root).getMinHeight() >= 0;
                if (reach != null) {
                    overX = overX && (setX || reach.getWidth() > w);
                    overY = overY && (setY || reach.getHeight() > h);
                }
            }
            // Back to the start of an axis before it stops scrolling:
            // a pane that does not scroll cannot be moved back.
            if (parent.isScrollableX() != overX) {
                if (!overX) {
                    parent.scrollRectToVisible(0, parent.getScrollY(), 1, 1, parent);
                }
                parent.setScrollableX(overX);
            }
            if (parent.isScrollableY() != overY) {
                if (!overY) {
                    parent.scrollRectToVisible(parent.getScrollX(), 0, 1, 1, parent);
                }
                parent.setScrollableY(overY);
            }
            rootPeer.setX(0);
            rootPeer.setY(0);
            rootPeer.setWidth(overX ? least.getWidth() : w);
            rootPeer.setHeight(overY ? least.getHeight() : h);
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            if (rootPeer == null) {
                return new Dimension(0, 0);
            }
            Dimension preferred = rootPeer.getPreferredSize();
            Dimension least = least();
            return new Dimension(Math.max(preferred.getWidth(), least.getWidth()),
                    Math.max(preferred.getHeight(), least.getHeight()));
        }
    }

    /// The smallest the scene can be laid out at, in device pixels.
    private Dimension least() {
        Parent root = scene == null ? null : scene.getRoot();
        if (root == null) {
            return new Dimension(0, 0);
        }
        double w;
        double h;
        if (root.isResizable()) {
            w = root.minWidth(-1);
            h = root.minHeight(-1);
        } else {
            Bounds bounds = root.getLayoutBounds();
            w = bounds.getMaxX();
            h = bounds.getMaxY();
        }
        // A fraction of a pixel over is rounding, not content out of reach.
        return new Dimension(Units.toPixels(Math.floor(w)), Units.toPixels(Math.floor(h)));
    }

    /// How far what a resizable root holds reaches when the root is laid
    /// out in a pane of the given size, in device pixels; `null` for a
    /// root that is not resizable, whose extent is its own.
    private Dimension reach(int paneWidth, int paneHeight) {
        Parent root = scene == null ? null : scene.getRoot();
        if (root == null || !root.isResizable()) {
            return null;
        }
        scene.cn1Layout(Units.toLogical(paneWidth), Units.toLogical(paneHeight));
        double w = 0;
        double h = 0;
        List<Node> children = root.getChildrenUnmodifiable();
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            if (!child.isVisible()) {
                continue;
            }
            Bounds bounds = child.getBoundsInParent();
            if (bounds.getWidth() < 0 || bounds.getHeight() < 0) {
                continue;
            }
            w = Math.max(w, bounds.getMaxX());
            h = Math.max(h, bounds.getMaxY());
        }
        return new Dimension(Units.toPixels(Math.floor(w)), Units.toPixels(Math.floor(h)));
    }

    Window window() {
        return window;
    }

    Scene scene() {
        return scene;
    }

    Component rootPeer() {
        return rootPeer;
    }

    void sceneChanged() {
        scene = window.getScene();
        content.removeAll();
        rootPeer = null;
        if (scene != null) {
            scene.cn1SetHost(host, window);
            if (scene.getRoot() != null) {
                rootPeer = scene.getRoot().cn1Peer();
                Container holder = rootPeer.getParent();
                if (holder != null) {
                    holder.removeComponent(rootPeer);
                }
                content.addComponent(rootPeer);
            }
        }
        content.revalidate();
    }

    void requestPulse() {
        if (rootPeer != null) {
            rootPeer.setShouldCalcPreferredSize(true);
            content.revalidateLater();
        }
    }

    void showCursor(int cursor) {
        if (rootPeer != null) {
            rootPeer.setCursor(cursor);
        }
    }

    void sized() {
        // An animation is commonly started before its stage is shown, and
        // Codename One ticks only the form on screen: the frame clock follows
        // whichever form was laid out last.
        FrameClock.ensureCurrent();
        if (rootPeer != null) {
            double x = window.getX();
            double y = window.getY();
            window.cn1Bounds(Double.isNaN(x) ? 0 : x, Double.isNaN(y) ? 0 : y, Units.toLogical(content.getWidth()),
                    Units.toLogical(content.getHeight()));
        }
    }

    void markClosed() {
        closed.set(true);
    }

    /// Returns once [#markClosed()] was called. The event thread keeps
    /// running: Codename One handles events on a nested loop while the
    /// waiting itself happens on another thread.
    void block() {
        if (!Display.isInitialized() || closed.get()) {
            return;
        }
        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                while (!closed.get()) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException interrupted) {
                        return;
                    }
                }
            }
        });
    }
}
