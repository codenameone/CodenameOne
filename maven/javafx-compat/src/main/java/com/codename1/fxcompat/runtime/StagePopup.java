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

import java.util.ArrayList;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;

import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

/// A popup on screen: the peer of its scene's root in a layer over the
/// form that shows the popup's owner.
///
/// The layer is a layered pane of the form kept for popups, so a popup
/// is drawn over everything the form shows and is clipped by the form.
/// The scene takes its preferred size and is placed with its top left
/// corner at the popup's anchor, moved back inside the form where it
/// would stick out. An owner that is in no form (a scene that is not
/// showing, or one in a separate desktop window) leaves the popup open
/// but not drawn.
///
/// The pointer reaches a popup through [SceneInput]: every host forwards
/// its pointer callbacks there, and an open popup under the pointer takes
/// them before the scene of the host does. A press outside every popup
/// hides those that hide automatically and then goes on to the host.
public final class StagePopup implements StageHost {

    private static final String X = "cn1.fx.popup.x";
    private static final String Y = "cn1.fx.popup.y";
    private static final ArrayList<StagePopup> OPEN = new ArrayList<StagePopup>();
    private static StagePopup captured;

    private final PopupWindow window;
    private Scene scene;
    private Component rootPeer;
    private boolean opened;
    private boolean placing;
    private boolean pulsePending;

    /// Creates the host of a popup.
    public StagePopup(PopupWindow window) {
        this.window = window;
    }

    /// Returns a point of a node in the coordinates popups are anchored
    /// in: scene coordinates, plus the position of the popup the node is
    /// in.
    public static Point2D anchor(Node node, double localX, double localY) {
        Point2D p = node.localToScene(localX, localY);
        double x = p.getX();
        double y = p.getY();
        Scene s = node.getScene();
        Window w = s == null ? null : s.getWindow();
        if (w instanceof PopupWindow) {
            x += clean(w.getX());
            y += clean(w.getY());
        }
        return new Point2D(x, y);
    }

    private static double clean(double v) {
        return Double.isNaN(v) ? 0 : v;
    }

    private static StagePopup at(int x, int y) {
        for (int i = OPEN.size() - 1; i >= 0; i--) {
            StagePopup p = OPEN.get(i);
            Component c = p.rootPeer;
            if (c != null && x >= c.getAbsoluteX() && y >= c.getAbsoluteY() && x < c.getAbsoluteX() + c.getWidth()
                    && y < c.getAbsoluteY() + c.getHeight()) {
                return p;
            }
        }
        return null;
    }

    private static void dismiss() {
        ArrayList<StagePopup> all = new ArrayList<StagePopup>(OPEN);
        for (int i = 0; i < all.size(); i++) {
            StagePopup p = all.get(i);
            if (p.opened && p.window.isAutoHide()) {
                p.window.hide();
            }
        }
    }

    /// Offers a pointer event at a display position to the open popups.
    /// Answers whether one took it; the host then does nothing with it.
    static boolean pointer(EventType<MouseEvent> kind, int x, int y, MouseButton button) {
        if (OPEN.isEmpty()) {
            return false;
        }
        if (kind == MouseEvent.MOUSE_PRESSED) {
            StagePopup hit = at(x, y);
            captured = hit;
            if (hit == null) {
                dismiss();
                return false;
            }
            hit.deliver(kind, x, y, button);
            return true;
        }
        if (kind == MouseEvent.MOUSE_DRAGGED || kind == MouseEvent.MOUSE_RELEASED) {
            StagePopup c = captured;
            if (c == null) {
                return false;
            }
            if (kind == MouseEvent.MOUSE_RELEASED) {
                captured = null;
            }
            if (c.opened) {
                c.deliver(kind, x, y, button);
            }
            return true;
        }
        StagePopup hit = at(x, y);
        if (hit == null) {
            return false;
        }
        hit.deliver(kind, x, y, button);
        return true;
    }

    /// Offers a turn of the wheel to the open popups.
    static boolean wheel(int x, int y, int deltaX, int deltaY) {
        StagePopup hit = OPEN.isEmpty() ? null : at(x, y);
        if (hit == null || hit.scene == null) {
            return false;
        }
        hit.scene.cn1Wheel(hit.localX(x), hit.localY(y), Units.toLogical(deltaX), Units.toLogical(deltaY));
        return true;
    }

    /// Offers a key to the open popups. The one opened last that closes
    /// by itself has the keyboard, as the popup window of a menu has in
    /// JavaFX: the key goes to its scene and to nothing behind it, and
    /// Escape closes it when nothing in it wanted the key.
    static boolean key(EventType<KeyEvent> kind, KeyCode code, String text) {
        for (int i = OPEN.size() - 1; i >= 0; i--) {
            StagePopup p = OPEN.get(i);
            if (!p.opened || p.scene == null || !p.window.isAutoHide()) {
                continue;
            }
            boolean taken = p.scene.cn1Key(kind, code, text);
            if (!taken && kind == KeyEvent.KEY_PRESSED && code == KeyCode.ESCAPE && p.opened) {
                p.window.hide();
            }
            return true;
        }
        return false;
    }

    /// Returns whether an open popup is under a display position.
    static boolean covers(int x, int y) {
        return !OPEN.isEmpty() && at(x, y) != null;
    }

    private static void closed(StagePopup popup) {
        OPEN.remove(popup);
        if (captured == popup) {
            captured = null;
        }
    }

    private static void opened(StagePopup popup) {
        if (!OPEN.contains(popup)) {
            OPEN.add(popup);
        }
    }

    private double localX(int x) {
        return Units.toLogical(x - (rootPeer == null ? 0 : rootPeer.getAbsoluteX()));
    }

    private double localY(int y) {
        return Units.toLogical(y - (rootPeer == null ? 0 : rootPeer.getAbsoluteY()));
    }

    private void deliver(EventType<MouseEvent> kind, int x, int y, MouseButton button) {
        if (scene != null) {
            scene.cn1Pointer(kind, localX(x), localY(y), button);
        }
    }

    /// Returns the peer of the root of the scene the popup floats over:
    /// the one below every popup in between.
    private Component basePeer() {
        PopupWindow p = window;
        for (int depth = 0; depth < 16; depth++) {
            Node n = p.getOwnerNode();
            Window w = p.getOwnerWindow();
            Scene s = n != null ? n.getScene() : (w == null ? null : w.getScene());
            if (s == null) {
                return null;
            }
            Window in = s.getWindow();
            if (in instanceof PopupWindow && in != p) {
                p = (PopupWindow) in;
                continue;
            }
            Parent root = s.getRoot();
            return root == null ? null : root.cn1Peer();
        }
        return null;
    }

    private void detach() {
        if (rootPeer != null) {
            Container parent = rootPeer.getParent();
            if (parent != null) {
                parent.removeComponent(rootPeer);
                parent.repaint();
            }
        }
    }

    private void place() {
        if (!opened || placing || scene == null || scene.getRoot() == null || rootPeer == null) {
            return;
        }
        placing = true;
        try {
            Parent root = scene.getRoot();
            double w = root.prefWidth(-1);
            double h = root.prefHeight(-1);
            scene.cn1Layout(w, h);
            double ax = clean(window.getAnchorX());
            double ay = clean(window.getAnchorY());
            int pw = Units.sizeToPixels(w);
            int ph = Units.sizeToPixels(h);
            int px = Units.toPixels(ax);
            int py = Units.toPixels(ay);
            Component base = basePeer();
            Form form = base == null ? null : base.getComponentForm();
            if (form != null) {
                Container layer = form.getLayeredPane(StagePopup.class, true);
                if (!(layer.getLayout() instanceof Spot)) {
                    layer.setLayout(new Spot());
                }
                int x = base.getAbsoluteX() + px - layer.getAbsoluteX();
                int y = base.getAbsoluteY() + py - layer.getAbsoluteY();
                x = Math.max(0, Math.min(x, layer.getWidth() - pw));
                y = Math.max(0, Math.min(y, layer.getHeight() - ph));
                ax = Units.toLogical(x + layer.getAbsoluteX() - base.getAbsoluteX());
                ay = Units.toLogical(y + layer.getAbsoluteY() - base.getAbsoluteY());
                rootPeer.putClientProperty(X, Integer.valueOf(x));
                rootPeer.putClientProperty(Y, Integer.valueOf(y));
                if (rootPeer.getParent() != layer) {
                    detach();
                    layer.addComponent(rootPeer);
                }
                rootPeer.setX(x);
                rootPeer.setY(y);
                rootPeer.setWidth(pw);
                rootPeer.setHeight(ph);
                layer.repaint();
            } else {
                detach();
                rootPeer.setX(px);
                rootPeer.setY(py);
                rootPeer.setWidth(pw);
                rootPeer.setHeight(ph);
            }
            window.cn1Bounds(ax, ay, w, h);
        } finally {
            placing = false;
        }
    }

    @Override
    public void sceneChanged() {
        detach();
        scene = window.getScene();
        rootPeer = null;
        if (scene != null) {
            scene.cn1SetHost(this, window);
            if (scene.getRoot() != null) {
                rootPeer = scene.getRoot().cn1Peer();
            }
        }
        place();
    }

    @Override
    public void rootChanged() {
        sceneChanged();
    }

    @Override
    public void requestPulse() {
        if (!opened || pulsePending || !Display.isInitialized()) {
            return;
        }
        pulsePending = true;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                pulsePending = false;
                place();
            }
        });
    }

    @Override
    public void showCursor(int cursor) {
    }

    @Override
    public void stageChanged() {
    }

    @Override
    public void boundsRequested() {
        place();
    }

    @Override
    public void open() {
        opened = true;
        opened(this);
        place();
    }

    @Override
    public void block() {
    }

    @Override
    public void close() {
        opened = false;
        closed(this);
        detach();
    }

    @Override
    public void toFront() {
    }

    /// Places every popup of the layer where it was last put.
    static final class Spot extends Layout {

        @Override
        public void layoutContainer(Container parent) {
            int n = parent.getComponentCount();
            for (int i = 0; i < n; i++) {
                Component c = parent.getComponentAt(i);
                Object x = c.getClientProperty(X);
                Object y = c.getClientProperty(Y);
                if (x instanceof Integer && y instanceof Integer) {
                    c.setX(((Integer) x).intValue());
                    c.setY(((Integer) y).intValue());
                }
            }
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            return new Dimension(0, 0);
        }
    }
}
