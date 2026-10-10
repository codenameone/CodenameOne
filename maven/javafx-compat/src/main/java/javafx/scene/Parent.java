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
package javafx.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.codename1.fxcompat.runtime.FxPeer;
import com.codename1.fxcompat.runtime.ParentPeer;
import com.codename1.fxcompat.runtime.StyleEngine;
import com.codename1.ui.Component;
import com.codename1.ui.Container;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

/// A node with children.
///
/// The peer of a parent is a Codename One container that holds the peers
/// of the children in the same order. Layout does not run in that
/// container: [#layout()] runs on the nodes, from the root of the scene,
/// and every node pushes its own bounds into its peer.
///
/// A parent that is not a region has no size of its own: its layout
/// bounds are the union of its children's bounds.
public abstract class Parent extends Node {

    private final ObservableList<Node> children = FXCollections.observableArrayList();
    private final ObservableList<Node> unmodifiableChildren = FXCollections.unmodifiableObservableList(children);
    private ObservableList<String> stylesheets;
    private final ReadOnlyBooleanWrapper needsLayout = new ReadOnlyBooleanWrapper(this, "needsLayout", true);
    private boolean performingLayout;
    private double prefWidthCache = -1;
    private double prefHeightCache = -1;
    private double minWidthCache = -1;
    private double minHeightCache = -1;

    /// Creates a parent with no children.
    protected Parent() {
        children.addListener(new ListChangeListener<Node>() {
            @Override
            public void onChanged(Change<? extends Node> change) {
                childrenChanged(change);
            }
        });
    }

    private void childrenChanged(ListChangeListener.Change<? extends Node> change) {
        while (change.next()) {
            if (change.wasPermutated()) {
                continue;
            }
            List<? extends Node> removed = change.getRemoved();
            for (int i = 0; i < removed.size(); i++) {
                Node n = removed.get(i);
                if (n.getParent() == this && !children.contains(n)) {
                    n.setParentInternal(null);
                    n.setSceneInternal(null);
                }
            }
            List<? extends Node> added = change.getAddedSubList();
            for (int i = 0; i < added.size(); i++) {
                Node n = added.get(i);
                if (n == null) {
                    throw new NullPointerException("A child must not be null");
                }
                Parent old = n.getParent();
                if (old == this) {
                    if (children.indexOf(n) != children.lastIndexOf(n)) {
                        throw new IllegalArgumentException("Duplicate child added: " + n);
                    }
                    continue;
                }
                for (Node up = this; up != null; up = up.getParent()) {
                    if (up == n) {
                        throw new IllegalArgumentException("A node cannot be its own ancestor: " + n);
                    }
                }
                if (old != null) {
                    old.children.remove(n);
                } else if (n.getScene() != null && n.getScene().getRoot() == n) {
                    throw new IllegalArgumentException("The root of a scene cannot be added as a child: " + n);
                }
                n.setParentInternal(this);
                n.setSceneInternal(getScene());
            }
        }
        if (cn1HasPeer()) {
            syncPeerChildren();
        }
        requestLayout();
        if (cn1SizedByChildren()) {
            cn1GeometryChanged();
        }
        cn1Repaint();
    }

    /// Returns the number of components at the start of the peer that are
    /// not children: a control keeps its native component there.
    protected int cn1PeerChildOffset() {
        return 0;
    }

    private void syncPeerChildren() {
        Component c = cn1Peer();
        if (!(c instanceof Container)) {
            return;
        }
        Container peer = (Container) c;
        int offset = cn1PeerChildOffset();
        for (int i = peer.getComponentCount() - 1; i >= offset; i--) {
            Component child = peer.getComponentAt(i);
            Node n = child instanceof FxPeer ? ((FxPeer) child).node() : null;
            if (n == null || n.getParent() != this) {
                peer.removeComponent(child);
            }
        }
        for (int i = 0; i < children.size(); i++) {
            Component expected = children.get(i).cn1Peer();
            int at = offset + i;
            if (at < peer.getComponentCount() && peer.getComponentAt(at) == expected) {
                continue;
            }
            Container holder = expected.getParent();
            if (holder != null) {
                holder.removeComponent(expected);
            }
            if (at >= peer.getComponentCount()) {
                peer.addComponent(expected);
            } else {
                peer.addComponent(at, expected);
            }
        }
    }

    /// Returns the modifiable list of children, for a constructor: unlike
    /// [#getChildren()] it cannot be overridden, so it is safe to call
    /// before a subclass is initialized.
    protected final ObservableList<Node> cn1Children() {
        return children;
    }

    @Override
    protected Component cn1CreatePeer() {
        return new ParentPeer(this);
    }

    @Override
    protected void cn1PeerCreated() {
        syncPeerChildren();
    }

    /// Returns the modifiable list of children; public in the panes.
    protected ObservableList<Node> getChildren() {
        return children;
    }

    /// Returns a read-only view of the children.
    public ObservableList<Node> getChildrenUnmodifiable() {
        return unmodifiableChildren;
    }

    /// Returns the children the layout places: the managed ones.
    protected <E extends Node> List<E> getManagedChildren() {
        ArrayList<E> managed = new ArrayList<E>();
        for (int i = 0; i < children.size(); i++) {
            Node n = children.get(i);
            if (n.isManaged()) {
                @SuppressWarnings("unchecked")
                E e = (E) n;
                managed.add(e);
            }
        }
        return managed;
    }

    /// Returns whether this parent has style sheets of its own, without
    /// creating the list [#getStylesheets()] answers.
    public final boolean cn1HasStylesheets() {
        return stylesheets != null && !stylesheets.isEmpty();
    }

    /// Returns the style sheet locations of this parent. The list is
    /// passed to the installed style engine; without one it has no effect.
    public final ObservableList<String> getStylesheets() {
        if (stylesheets == null) {
            stylesheets = FXCollections.observableArrayList();
            stylesheets.addListener(new ListChangeListener<String>() {
                @Override
                public void onChanged(Change<? extends String> change) {
                    StyleEngine.getInstance().stylesheetsChanged(Parent.this);
                }
            });
        }
        return stylesheets;
    }

    @Override
    void setSceneInternal(Scene value) {
        super.setSceneInternal(value);
        for (int i = 0; i < children.size(); i++) {
            children.get(i).setSceneInternal(value);
        }
    }

    @Override
    void cn1DisabledChanged() {
        for (int i = 0; i < children.size(); i++) {
            children.get(i).updateDisabled();
        }
    }

    @Override
    void collect(String compound, Set<Node> into) {
        super.collect(compound, into);
        for (int i = 0; i < children.size(); i++) {
            children.get(i).collect(compound, into);
        }
    }

    final void cn1ToFront(Node child) {
        int at = children.indexOf(child);
        if (at >= 0 && at != children.size() - 1) {
            children.remove(at);
            children.add(child);
        }
    }

    final void cn1ToBack(Node child) {
        int at = children.indexOf(child);
        if (at > 0) {
            children.remove(at);
            children.add(0, child);
        }
    }

    // ---------------------------------------------------------- geometry

    /// Returns whether the layout bounds of this parent are the union of
    /// its children's bounds, which is so unless it is a region.
    protected boolean cn1SizedByChildren() {
        return true;
    }

    final void cn1ChildBoundsChanged(Node child) {
        if (cn1SizedByChildren() && !performingLayout && cn1LayoutBoundsStale(unionOfChildren())) {
            cn1GeometryChanged();
            for (int i = 0; i < children.size(); i++) {
                children.get(i).cn1SyncPeerBounds();
            }
        }
    }

    @Override
    protected Bounds cn1ComputeLayoutBounds() {
        return unionOfChildren();
    }

    private Bounds unionOfChildren() {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        boolean any = false;
        for (int i = 0; i < children.size(); i++) {
            Node n = children.get(i);
            if (!n.isVisible()) {
                continue;
            }
            Bounds b = n.getBoundsInParent();
            if (b.getWidth() < 0 || b.getHeight() < 0) {
                continue;
            }
            any = true;
            minX = Math.min(minX, b.getMinX());
            minY = Math.min(minY, b.getMinY());
            maxX = Math.max(maxX, b.getMaxX());
            maxY = Math.max(maxY, b.getMaxY());
        }
        if (!any) {
            return new BoundingBox(0, 0, 0, 0);
        }
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    public boolean contains(double localX, double localY) {
        for (int i = children.size() - 1; i >= 0; i--) {
            Node n = children.get(i);
            if (n.isVisible()) {
                javafx.geometry.Point2D p = n.parentToLocal(localX, localY);
                if (n.contains(p.getX(), p.getY())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected Node cn1PickLocal(double x, double y) {
        for (int i = children.size() - 1; i >= 0; i--) {
            Node hit = children.get(i).cn1Pick(x, y);
            if (hit != null) {
                return hit;
            }
        }
        return cn1PickSelf(x, y) ? this : null;
    }

    /// Returns whether a point that hit no child hits this parent itself.
    protected boolean cn1PickSelf(double x, double y) {
        return isPickOnBounds() && getLayoutBounds().contains(x, y);
    }

    // ------------------------------------------------------------ layout

    /// Returns whether the layout of this parent is out of date.
    public final boolean isNeedsLayout() {
        return needsLayout.get();
    }

    /// Whether the layout of this parent is out of date.
    public final ReadOnlyBooleanProperty needsLayoutProperty() {
        return needsLayout.getReadOnlyProperty();
    }

    /// Sets whether the layout is out of date; for subclasses.
    protected final void setNeedsLayout(boolean value) {
        needsLayout.set(value);
    }

    /// Marks the layout of this parent and of its ancestors as out of
    /// date. It runs before the next frame.
    public void requestLayout() {
        prefWidthCache = -1;
        prefHeightCache = -1;
        minWidthCache = -1;
        minHeightCache = -1;
        cn1ClearSizeCache();
        if (performingLayout) {
            return;
        }
        needsLayout.set(true);
        Parent p = getParent();
        if (p != null) {
            p.requestLayout();
        } else {
            Scene s = getScene();
            if (s != null) {
                s.cn1RequestPulse();
            }
        }
    }

    /// Called when cached sizes are no longer valid.
    protected void cn1ClearSizeCache() {
    }

    /// Marks this parent alone as needing layout, without telling its
    /// ancestors; used when a parent resizes it during their own layout.
    protected final void cn1MarkNeedsLayout() {
        needsLayout.set(true);
    }

    /// Lays out this parent and everything below it that needs it.
    public final void layout() {
        if (needsLayout.get()) {
            needsLayout.set(false);
            performingLayout = true;
            try {
                layoutChildren();
            } finally {
                performingLayout = false;
            }
        }
        for (int i = 0; i < children.size(); i++) {
            Node n = children.get(i);
            if (n instanceof Parent) {
                ((Parent) n).layout();
            }
        }
        if (cn1SizedByChildren()) {
            cn1ChildBoundsChanged(null);
        }
    }

    /// Places the children. The default gives every managed resizable
    /// child its preferred size and leaves it where it is.
    protected void layoutChildren() {
        for (int i = 0; i < children.size(); i++) {
            Node n = children.get(i);
            if (n.isResizable() && n.isManaged()) {
                n.autosize();
            }
        }
    }

    @Override
    public double prefWidth(double height) {
        if (height == -1) {
            if (prefWidthCache == -1) {
                prefWidthCache = clean(computePrefWidth(-1));
            }
            return prefWidthCache;
        }
        return clean(computePrefWidth(height));
    }

    @Override
    public double prefHeight(double width) {
        if (width == -1) {
            if (prefHeightCache == -1) {
                prefHeightCache = clean(computePrefHeight(-1));
            }
            return prefHeightCache;
        }
        return clean(computePrefHeight(width));
    }

    @Override
    public double minWidth(double height) {
        if (height == -1) {
            if (minWidthCache == -1) {
                minWidthCache = clean(computeMinWidth(-1));
            }
            return minWidthCache;
        }
        return clean(computeMinWidth(height));
    }

    @Override
    public double minHeight(double width) {
        if (width == -1) {
            if (minHeightCache == -1) {
                minHeightCache = clean(computeMinHeight(-1));
            }
            return minHeightCache;
        }
        return clean(computeMinHeight(width));
    }

    private static double clean(double v) {
        return Double.isNaN(v) || v < 0 ? 0 : v;
    }

    /// Computes the preferred width: the extent of the managed children at
    /// their preferred sizes.
    protected double computePrefWidth(double height) {
        double minX = 0;
        double maxX = 0;
        boolean first = true;
        for (int i = 0; i < children.size(); i++) {
            Node n = children.get(i);
            if (n.isManaged()) {
                double x = n.getLayoutBounds().getMinX() + n.getLayoutX();
                double right = x + Math.max(n.minWidth(-1), Math.min(n.prefWidth(-1), n.maxWidth(-1)));
                minX = first ? x : Math.min(minX, x);
                maxX = first ? right : Math.max(maxX, right);
                first = false;
            }
        }
        return maxX - minX;
    }

    /// Computes the preferred height: the extent of the managed children
    /// at their preferred sizes.
    protected double computePrefHeight(double width) {
        double minY = 0;
        double maxY = 0;
        boolean first = true;
        for (int i = 0; i < children.size(); i++) {
            Node n = children.get(i);
            if (n.isManaged()) {
                double y = n.getLayoutBounds().getMinY() + n.getLayoutY();
                double bottom = y + Math.max(n.minHeight(-1), Math.min(n.prefHeight(-1), n.maxHeight(-1)));
                minY = first ? y : Math.min(minY, y);
                maxY = first ? bottom : Math.max(maxY, bottom);
                first = false;
            }
        }
        return maxY - minY;
    }

    /// Computes the minimum width; the preferred width unless overridden.
    protected double computeMinWidth(double height) {
        return prefWidth(height);
    }

    /// Computes the minimum height; the preferred height unless
    /// overridden.
    protected double computeMinHeight(double width) {
        return prefHeight(width);
    }

    @Override
    public double getBaselineOffset() {
        for (int i = 0; i < children.size(); i++) {
            Node n = children.get(i);
            if (n.isManaged()) {
                double offset = n.getBaselineOffset();
                if (offset == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                    continue;
                }
                return n.getLayoutBounds().getMinY() + n.getLayoutY() + offset;
            }
        }
        return super.getBaselineOffset();
    }
}
