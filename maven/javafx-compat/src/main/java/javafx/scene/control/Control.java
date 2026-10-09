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
package javafx.scene.control;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.geom.Dimension;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.Region;

/// The base of the controls.
///
/// #### How a control is shown
///
/// A control is a region whose peer holds one Codename One component,
/// the *native* component, below the peers of any child nodes. The
/// subclass creates it in [#cn1CreateNative()], the first time the
/// control's peer is needed, and copies the control's state into it in
/// [#cn1SyncNative()], which runs after creation and whenever a property
/// created with `Dirty.NATIVE` changes. The native component fills the
/// control inside its insets.
///
/// The preferred size of a control is the preferred size of the native
/// component, in logical pixels, plus the insets; its minimum and maximum
/// are the preferred size unless a subclass says otherwise. A subclass
/// whose native preferred size changed calls [#cn1NativeSizeChanged()].
///
/// There are no skins: `Skin`, `SkinBase` and the behaviour classes of
/// JavaFX are not part of this layer, and a control looks as the Codename
/// One theme makes the native component look. The region's own background
/// and border are drawn behind it.
///
/// #### Context menu and tooltip
///
/// A control with a context menu opens it where a context menu is asked
/// for, a secondary click or a long press, and consumes that event. A
/// tooltip is installed with `Tooltip.install`, which hands its text to
/// the native component.
public abstract class Control extends Region {

    private final ObjectProperty<ContextMenu> contextMenu = new SimpleObjectProperty<ContextMenu>(this,
            "contextMenu");
    private final ObjectProperty<Tooltip> tooltip = new SimpleObjectProperty<Tooltip>(this, "tooltip");
    private Component nativeComponent;
    private boolean creating;

    /// Creates a control.
    protected Control() {
        setFocusTraversable(true);
        addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, new EventHandler<ContextMenuEvent>() {
            @Override
            public void handle(ContextMenuEvent event) {
                ContextMenu menu = getContextMenu();
                if (menu != null && !event.isConsumed()) {
                    Point2D p = sceneToLocal(event.getSceneX(), event.getSceneY());
                    Point2D at = com.codename1.fxcompat.runtime.StagePopup.anchor(Control.this, p.getX(), p.getY());
                    menu.show(Control.this, at.getX(), at.getY());
                    event.consume();
                }
            }
        });
        tooltip.addListener(new ChangeListener<Tooltip>() {
            @Override
            public void changed(ObservableValue<? extends Tooltip> observable, Tooltip oldValue, Tooltip newValue) {
                if (oldValue != null) {
                    Tooltip.uninstall(Control.this, oldValue);
                }
                if (newValue != null) {
                    Tooltip.install(Control.this, newValue);
                }
            }
        });
    }

    /// Sets the menu opened where a context menu is asked for.
    public final void setContextMenu(ContextMenu value) {
        contextMenu.set(value);
    }

    /// Returns the menu opened where a context menu is asked for.
    public final ContextMenu getContextMenu() {
        return contextMenu.get();
    }

    /// The menu opened where a context menu is asked for.
    public final ObjectProperty<ContextMenu> contextMenuProperty() {
        return contextMenu;
    }

    /// Sets the tooltip of this control.
    public final void setTooltip(Tooltip value) {
        tooltip.set(value);
    }

    /// Returns the tooltip of this control.
    public final Tooltip getTooltip() {
        return tooltip.get();
    }

    /// The tooltip of this control.
    public final ObjectProperty<Tooltip> tooltipProperty() {
        return tooltip;
    }

    /// Creates the Codename One component this control shows, or `null`
    /// for a control made of child nodes only.
    protected abstract Component cn1CreateNative();

    /// Copies the state of this control into the native component. An
    /// override calls the super implementation, which handles what every
    /// control has: enabled and focusable.
    protected void cn1SyncNative() {
        if (nativeComponent != null) {
            nativeComponent.setEnabled(!isDisabled());
            nativeComponent.setFocusable(isFocusTraversable() && !isDisabled());
        }
    }

    /// Returns the native component, creating the peer if needed; `null`
    /// for a control without one.
    public final Component cn1Native() {
        cn1Peer();
        return nativeComponent;
    }

    /// Returns the native component if the peer was created, else `null`.
    protected final Component cn1NativeIfCreated() {
        return nativeComponent;
    }

    @Override
    protected int cn1PeerChildOffset() {
        return nativeComponent == null ? 0 : 1;
    }

    @Override
    protected void cn1PeerCreated() {
        creating = true;
        try {
            nativeComponent = cn1CreateNative();
            if (nativeComponent != null) {
                if (nativeComponent instanceof com.codename1.ui.Label) {
                    // A Codename One label that is a pixel short of its text scrolls it
                    // back and forth while the pointer is over it or it holds the focus:
                    // a phone's answer to a narrow screen. JavaFX text never moves.
                    ((com.codename1.ui.Label) nativeComponent).setTickerEnabled(false);
                }
                Component peer = cn1Peer();
                if (peer instanceof Container) {
                    ((Container) peer).addComponent(0, nativeComponent);
                }
                nativeComponent.addFocusListener(new FocusListener() {
                    @Override
                    public void focusGained(Component cmp) {
                        if (getScene() != null) {
                            getScene().cn1SetFocusOwner(Control.this, false);
                        }
                    }

                    @Override
                    public void focusLost(Component cmp) {
                        if (getScene() != null && getScene().getFocusOwner() == Control.this) {
                            getScene().cn1SetFocusOwner(null, false);
                        }
                    }
                });
            }
        } finally {
            creating = false;
        }
        super.cn1PeerCreated();
        cn1SyncNative();
        placeNative();
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & Dirty.NATIVE) != 0 && nativeComponent != null && !creating) {
            cn1SyncNative();
            nativeComponent.repaint();
        }
        super.cn1Invalidated(what);
    }

    /// Tells layout that the preferred size of the native component may
    /// have changed.
    protected final void cn1NativeSizeChanged() {
        if (nativeComponent != null) {
            nativeComponent.setShouldCalcPreferredSize(true);
        }
        requestLayout();
    }

    private void placeNative() {
        if (nativeComponent == null || !cn1HasPeer()) {
            return;
        }
        Component peer = cn1Peer();
        Insets in = getInsets();
        int left = Units.toPixels(in.getLeft());
        int top = Units.toPixels(in.getTop());
        int w = Math.max(0, peer.getWidth() - left - Units.toPixels(in.getRight()));
        int h = Math.max(0, peer.getHeight() - top - Units.toPixels(in.getBottom()));
        if (nativeComponent.getX() != left || nativeComponent.getY() != top || nativeComponent.getWidth() != w
                || nativeComponent.getHeight() != h) {
            nativeComponent.setX(left);
            nativeComponent.setY(top);
            nativeComponent.setWidth(w);
            nativeComponent.setHeight(h);
            if (nativeComponent instanceof Container) {
                // The bounds are final, so lay out in place; revalidate()
                // would walk up to the form. Invalidating the preferred
                // size is the public way to flag the container for layout.
                nativeComponent.setShouldCalcPreferredSize(true);
                ((Container) nativeComponent).layoutContainer();
            }
        }
    }

    @Override
    protected void cn1PeerResized() {
        placeNative();
    }

    @Override
    protected void layoutChildren() {
        placeNative();
        super.layoutChildren();
    }

    /// Returns the preferred size of the native component in device
    /// pixels; 0 by 0 without one.
    protected Dimension cn1NativePreferredSize() {
        Component c = cn1Native();
        return c == null ? new Dimension(0, 0) : c.getPreferredSize();
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + snapSizeX(Units.toLogical(cn1NativePreferredSize().getWidth())) + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + snapSizeY(Units.toLogical(cn1NativePreferredSize().getHeight())) + in.getBottom();
    }

    @Override
    protected double computeMinWidth(double height) {
        return computePrefWidth(height);
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    /// A control grows no larger than its preferred size, and that is the
    /// preferred size the application set when it set one: a button with
    /// no text given `setPrefSize(40, 40)` is 40 wide, not as wide as its
    /// missing text.
    @Override
    protected double computeMaxWidth(double height) {
        return prefWidth(height);
    }

    @Override
    protected double computeMaxHeight(double width) {
        return prefHeight(width);
    }

    @Override
    public double getBaselineOffset() {
        Component c = cn1Native();
        if (c == null) {
            return super.getBaselineOffset();
        }
        Dimension d = c.getPreferredSize();
        int baseline = c.getBaseline(d.getWidth(), d.getHeight());
        if (baseline < 0) {
            return BASELINE_OFFSET_SAME_AS_HEIGHT;
        }
        return getInsets().getTop() + Units.toLogical(baseline);
    }

    @Override
    protected void cn1FocusRequested() {
        Component c = cn1NativeIfCreated();
        if (c != null && c.isFocusable()) {
            c.requestFocus();
        }
    }

    @Override
    public boolean isResizable() {
        return true;
    }
}
