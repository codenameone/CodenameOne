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

import java.util.ArrayList;
import java.util.HashMap;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.ui.Component;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/// Items side by side, or one above the other, with a divider between
/// each two that the user drags to give one more room and the other
/// less.
///
/// The items are children of the split pane, laid out by it; between
/// them sit divider bars, regions of this control that follow the
/// pointer while it is down on them. A divider's position is the
/// fraction, 0 to 1, of the pane's length at which its bar is centred.
///
/// #### Differences from JavaFX
///
/// - A divider is never placed before the one in front of it, and where
///   there is room it leaves the items on both sides their minimum
///   size; the stored position is not rewritten when that moves the bar.
/// - Positions are fractions of the pane at every size, so every item
///   grows and shrinks with the pane: what `setResizableWithParent`
///   records is not applied.
/// - An item is not clipped to its area.
///
/// The pseudo-classes `horizontal` and `vertical` follow the
/// orientation.
public class SplitPane extends Control {

    private static final int ORIENTATION = Dirty.USER;
    private static final double BAR = 6;
    private static final String RESIZABLE_WITH_PARENT = "resizable-with-parent";
    private static final PseudoClass HORIZONTAL = PseudoClass.getPseudoClass("horizontal");
    private static final PseudoClass VERTICAL = PseudoClass.getPseudoClass("vertical");

    private final ObservableList<Node> items = FXCollections.observableArrayList();
    private final ObservableList<Divider> dividers = FXCollections.observableArrayList();
    private final ObservableList<Divider> unmodifiableDividers = FXCollections.unmodifiableObservableList(dividers);
    private final ArrayList<Region> bars = new ArrayList<Region>();
    private final HashMap<Integer, Double> pending = new HashMap<Integer, Double>();
    private final ObjectProperty<Orientation> orientation = new FxObject<Orientation>(this, "orientation",
            Orientation.HORIZONTAL, ORIENTATION | Dirty.LAYOUT);
    private final InvalidationListener moved = new InvalidationListener() {
        @Override
        public void invalidated(Observable observable) {
            requestLayout();
        }
    };

    /// Sets whether an item keeps its share when the split pane is
    /// resized. Recorded in the node's properties; see the class
    /// description.
    public static void setResizableWithParent(Node node, Boolean value) {
        if (value == null) {
            node.getProperties().remove(RESIZABLE_WITH_PARENT);
        } else {
            node.getProperties().put(RESIZABLE_WITH_PARENT, value);
        }
    }

    /// Returns what [#setResizableWithParent(Node, Boolean)] recorded for
    /// a node; true when nothing was.
    public static Boolean isResizableWithParent(Node node) {
        if (node.hasProperties()) {
            Object value = node.getProperties().get(RESIZABLE_WITH_PARENT);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        }
        return Boolean.TRUE;
    }

    /// Creates an empty split pane.
    public SplitPane() {
        this((Node[]) null);
    }

    /// Creates a split pane with items.
    public SplitPane(Node... items) {
        getStyleClass().add("split-pane");
        setFocusTraversable(false);
        pseudoClassStateChanged(HORIZONTAL, true);
        this.items.addListener(new ListChangeListener<Node>() {
            @Override
            public void onChanged(Change<? extends Node> change) {
                itemsChanged();
            }
        });
        if (items != null) {
            this.items.addAll(items);
        }
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & ORIENTATION) != 0) {
            boolean vertical = getOrientation() == Orientation.VERTICAL;
            pseudoClassStateChanged(VERTICAL, vertical);
            pseudoClassStateChanged(HORIZONTAL, !vertical);
            for (int i = 0; i < bars.size(); i++) {
                bars.get(i).setCursor(vertical ? Cursor.V_RESIZE : Cursor.H_RESIZE);
            }
        }
        super.cn1Invalidated(what);
    }

    private void itemsChanged() {
        int wanted = Math.max(0, items.size() - 1);
        while (dividers.size() > wanted) {
            dividers.remove(dividers.size() - 1);
            bars.remove(bars.size() - 1);
        }
        while (dividers.size() < wanted) {
            Divider d = new Divider();
            Double early = pending.remove(Integer.valueOf(dividers.size()));
            if (early != null) {
                d.setPosition(early.doubleValue());
            }
            d.positionProperty().addListener(moved);
            dividers.add(d);
            bars.add(bar(d));
        }
        ArrayList<Node> all = new ArrayList<Node>(items);
        all.addAll(bars);
        cn1Children().setAll(all);
        requestLayout();
    }

    /// The bar between two items, in the colour the theme gives one.
    private static final class Bar extends Region {
        @Override
        public String cn1DefaultStyle() {
            return "-fx-background-color: derive(-fx-base, -12%);";
        }
    }

    private Region bar(final Divider divider) {
        Region r = new Bar();
        r.setManaged(false);
        r.getStyleClass().add("split-pane-divider");
        r.setBackground(new Background(new BackgroundFill(Color.gray(0.8), CornerRadii.EMPTY, Insets.EMPTY)));
        r.setCursor(getOrientation() == Orientation.VERTICAL ? Cursor.V_RESIZE : Cursor.H_RESIZE);
        EventHandler<MouseEvent> drag = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getEventType() == MouseEvent.MOUSE_DRAGGED && !divider.positionProperty().isBound()) {
                    Insets in = getInsets();
                    Point2D p = sceneToLocal(event.getSceneX(), event.getSceneY());
                    boolean vertical = getOrientation() == Orientation.VERTICAL;
                    double length = vertical ? getHeight() - in.getTop() - in.getBottom()
                            : getWidth() - in.getLeft() - in.getRight();
                    double along = vertical ? p.getY() - in.getTop() : p.getX() - in.getLeft();
                    if (length > 0) {
                        divider.setPosition(Math.max(0, Math.min(1, along / length)));
                    }
                }
                event.consume();
            }
        };
        r.addEventHandler(MouseEvent.MOUSE_PRESSED, drag);
        r.addEventHandler(MouseEvent.MOUSE_DRAGGED, drag);
        return r;
    }

    // --------------------------------------------------------------- layout

    private static double along(Node n, boolean vertical, boolean min) {
        if (!n.isResizable()) {
            return vertical ? n.getLayoutBounds().getHeight() : n.getLayoutBounds().getWidth();
        }
        if (min) {
            return vertical ? n.minHeight(-1) : n.minWidth(-1);
        }
        return vertical ? n.prefHeight(-1) : n.prefWidth(-1);
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        boolean vertical = getOrientation() == Orientation.VERTICAL;
        double w = Math.max(0, getWidth() - in.getLeft() - in.getRight());
        double h = Math.max(0, getHeight() - in.getTop() - in.getBottom());
        double length = vertical ? h : w;
        int n = items.size();
        double start = 0;
        for (int i = 0; i < n; i++) {
            double end = length;
            if (i < n - 1 && i < bars.size()) {
                double p = Math.max(0, Math.min(1, dividers.get(i).getPosition()));
                double remaining = (n - 2 - i) * BAR;
                double low = start;
                double high = Math.max(low, length - BAR - remaining);
                double lowMin = start + along(items.get(i), vertical, true);
                double highMin = high - along(items.get(i + 1), vertical, true);
                if (lowMin <= highMin) {
                    low = lowMin;
                    high = highMin;
                }
                double at = p * length - BAR / 2;
                at = at < low ? low : (at > high ? high : at);
                end = vertical ? snapPositionY(at) : snapPositionX(at);
                Region bar = bars.get(i);
                if (vertical) {
                    bar.resizeRelocate(in.getLeft(), in.getTop() + end, w, BAR);
                } else {
                    bar.resizeRelocate(in.getLeft() + end, in.getTop(), BAR, h);
                }
            }
            double size = Math.max(0, end - start);
            if (vertical) {
                layoutInArea(items.get(i), in.getLeft(), in.getTop() + start, w, size, 0, HPos.CENTER, VPos.CENTER);
            } else {
                layoutInArea(items.get(i), in.getLeft() + start, in.getTop(), size, h, 0, HPos.CENTER, VPos.CENTER);
            }
            start = end + BAR;
        }
    }

    private double sum(boolean vertical, boolean min) {
        double total = bars.size() * BAR;
        for (int i = 0; i < items.size(); i++) {
            total += along(items.get(i), vertical, min);
        }
        return total;
    }

    private double widest(boolean vertical, boolean min) {
        double most = 0;
        for (int i = 0; i < items.size(); i++) {
            most = Math.max(most, along(items.get(i), vertical, min));
        }
        return most;
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        boolean vertical = getOrientation() == Orientation.VERTICAL;
        return in.getLeft() + (vertical ? widest(false, false) : sum(false, false)) + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        boolean vertical = getOrientation() == Orientation.VERTICAL;
        return in.getTop() + (vertical ? sum(true, false) : widest(true, false)) + in.getBottom();
    }

    @Override
    protected double computeMinWidth(double height) {
        Insets in = getInsets();
        boolean vertical = getOrientation() == Orientation.VERTICAL;
        return in.getLeft() + (vertical ? widest(false, true) : sum(false, true)) + in.getRight();
    }

    @Override
    protected double computeMinHeight(double width) {
        Insets in = getInsets();
        boolean vertical = getOrientation() == Orientation.VERTICAL;
        return in.getTop() + (vertical ? sum(true, true) : widest(true, true)) + in.getBottom();
    }

    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
    }

    // ------------------------------------------------------------------ API

    /// Returns the nodes this split pane shows, in order.
    public ObservableList<Node> getItems() {
        return items;
    }

    /// Returns the dividers, one fewer than the items; the list cannot be
    /// changed.
    public ObservableList<Divider> getDividers() {
        return unmodifiableDividers;
    }

    /// Sets the position of one divider. A position for a divider that
    /// does not exist yet is kept for when it does.
    public void setDividerPosition(int dividerIndex, double position) {
        if (dividerIndex >= 0 && dividerIndex < dividers.size()) {
            dividers.get(dividerIndex).setPosition(position);
        } else if (dividerIndex >= 0) {
            pending.put(Integer.valueOf(dividerIndex), Double.valueOf(position));
        }
    }

    /// Sets the positions of the dividers, in order.
    public void setDividerPositions(double... positions) {
        if (positions == null) {
            return;
        }
        for (int i = 0; i < positions.length; i++) {
            setDividerPosition(i, positions[i]);
        }
    }

    /// Returns the positions of the dividers, in order.
    public double[] getDividerPositions() {
        double[] positions = new double[dividers.size()];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = dividers.get(i).getPosition();
        }
        return positions;
    }

    /// Returns whether the items are side by side or stacked.
    public final Orientation getOrientation() {
        Orientation o = orientation.get();
        return o == null ? Orientation.HORIZONTAL : o;
    }

    /// Sets whether the items are side by side (horizontal) or stacked.
    public final void setOrientation(Orientation value) {
        orientation.set(value);
    }

    /// Whether the items are side by side or stacked.
    public final ObjectProperty<Orientation> orientationProperty() {
        return orientation;
    }

    /// The boundary between two items of a split pane.
    public static class Divider {

        private final DoubleProperty position = new SimpleDoubleProperty(this, "position", 0.5);

        /// Creates a divider in the middle.
        public Divider() {
        }

        /// Sets where the divider is, as a fraction of the pane's length.
        public final void setPosition(double value) {
            position.set(value);
        }

        /// Returns where the divider is, as a fraction of the pane's
        /// length.
        public final double getPosition() {
            return position.get();
        }

        /// Where the divider is, as a fraction of the pane's length.
        public final DoubleProperty positionProperty() {
            return position;
        }
    }
}
