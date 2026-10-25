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
package javafx.stage;

import java.util.ArrayList;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.Scene;

/// A window without decoration that floats over the window of its owner:
/// the base of menus, tooltips and other popups.
///
/// A popup is not a window of the device. Its scene is drawn in a layer
/// over the form that shows the owner, at the anchor, and moved back
/// inside the form where it would stick out; see
/// `com.codename1.fxcompat.runtime.StagePopup`. An owner shown in a
/// separate desktop window has no such layer, and a popup of it is not
/// drawn.
///
/// The anchor is in the coordinates the input events of this layer report
/// as screen coordinates: those of the scene of the owning window, or,
/// for a popup opened from a popup, of the scene below both.
///
/// A popup with `autoHide` is hidden by a press outside every open popup;
/// the press then goes on to what is under it. `autoFix`,
/// `hideOnEscape`, `consumeAutoHidingEvents` and the anchor location of
/// JavaFX are not part of this layer.
public abstract class PopupWindow extends Window {

    private final ReadOnlyObjectWrapper<Window> ownerWindow = new ReadOnlyObjectWrapper<Window>(this, "ownerWindow");
    private final ReadOnlyObjectWrapper<Node> ownerNode = new ReadOnlyObjectWrapper<Node>(this, "ownerNode");
    private final BooleanProperty autoHide = new SimpleBooleanProperty(this, "autoHide", false);
    private final ReadOnlyDoubleWrapper anchorX = new ReadOnlyDoubleWrapper(this, "anchorX", Double.NaN);
    private final ReadOnlyDoubleWrapper anchorY = new ReadOnlyDoubleWrapper(this, "anchorY", Double.NaN);

    /// Creates a popup that is not showing.
    public PopupWindow() {
    }

    /// The window this popup floats over.
    public final ReadOnlyObjectProperty<Window> ownerWindowProperty() {
        return ownerWindow.getReadOnlyProperty();
    }

    /// Returns the window this popup floats over, `null` while hidden.
    public final Window getOwnerWindow() {
        return ownerWindow.get();
    }

    /// The node this popup was opened for.
    public final ReadOnlyObjectProperty<Node> ownerNodeProperty() {
        return ownerNode.getReadOnlyProperty();
    }

    /// Returns the node this popup was opened for, or `null`.
    public final Node getOwnerNode() {
        return ownerNode.get();
    }

    /// Sets whether a press outside the popup hides it.
    public final void setAutoHide(boolean value) {
        autoHide.set(value);
    }

    /// Returns whether a press outside the popup hides it.
    public final boolean isAutoHide() {
        return autoHide.get();
    }

    /// Whether a press outside the popup hides it.
    public final BooleanProperty autoHideProperty() {
        return autoHide;
    }

    /// The x the top left corner of the popup is asked to be at.
    public final ReadOnlyDoubleProperty anchorXProperty() {
        return anchorX.getReadOnlyProperty();
    }

    /// Asks for an x of the top left corner.
    public final void setAnchorX(final double value) {
        anchorX.set(value);
        if (cn1Host() != null) {
            cn1Host().boundsRequested();
        }
    }

    /// Returns the x the top left corner is asked to be at.
    public final double getAnchorX() {
        return anchorX.get();
    }

    /// The y the top left corner of the popup is asked to be at.
    public final ReadOnlyDoubleProperty anchorYProperty() {
        return anchorY.getReadOnlyProperty();
    }

    /// Asks for a y of the top left corner.
    public final void setAnchorY(final double value) {
        anchorY.set(value);
        if (cn1Host() != null) {
            cn1Host().boundsRequested();
        }
    }

    /// Returns the y the top left corner is asked to be at.
    public final double getAnchorY() {
        return anchorY.get();
    }

    /// Shows the popup over a window, where it last was.
    public void show(Window owner) {
        if (owner == null) {
            throw new NullPointerException("The owner must not be null");
        }
        ownerNode.set(null);
        ownerWindow.set(owner);
        cn1Show(false);
    }

    /// Shows the popup for a node, with its top left corner at the
    /// anchor. Unlike in JavaFX the node need not be in a showing window;
    /// the popup is then open but not drawn.
    public void show(Node ownerNode, double anchorX, double anchorY) {
        if (ownerNode == null) {
            throw new NullPointerException("The owner node must not be null");
        }
        Scene s = ownerNode.getScene();
        this.ownerNode.set(ownerNode);
        this.ownerWindow.set(s == null ? null : s.getWindow());
        place(anchorX, anchorY);
    }

    /// Shows the popup over a window, with its top left corner at the
    /// anchor.
    public void show(Window ownerWindow, double anchorX, double anchorY) {
        if (ownerWindow == null) {
            throw new NullPointerException("The owner window must not be null");
        }
        this.ownerNode.set(null);
        this.ownerWindow.set(ownerWindow);
        place(anchorX, anchorY);
    }

    private void place(double x, double y) {
        anchorX.set(x);
        anchorY.set(y);
        if (isShowing()) {
            if (cn1Host() != null) {
                cn1Host().boundsRequested();
            }
        } else {
            cn1Show(false);
        }
    }

    private boolean opened(PopupWindow other) {
        if (other.getOwnerWindow() == this) {
            return true;
        }
        Node n = other.getOwnerNode();
        return n != null && n.getScene() != null && n.getScene().getWindow() == this;
    }

    /// Hides the popup and every popup opened from it.
    @Override
    public void hide() {
        if (!isShowing()) {
            return;
        }
        ArrayList<Window> all = new ArrayList<Window>(Window.getWindows());
        for (int i = 0; i < all.size(); i++) {
            Window w = all.get(i);
            if (w != this && w instanceof PopupWindow && w.isShowing() && opened((PopupWindow) w)) {
                w.hide();
            }
        }
        super.hide();
        ownerWindow.set(null);
        ownerNode.set(null);
    }
}
