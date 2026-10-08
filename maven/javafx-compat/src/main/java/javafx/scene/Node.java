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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.EventHandlerManager;
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.FxString;
import com.codename1.fxcompat.runtime.NodePeer;
import com.codename1.fxcompat.runtime.Renderer;
import com.codename1.fxcompat.runtime.StyleEngine;
import com.codename1.fxcompat.runtime.StyleTarget;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

/// The base of everything in a scene graph.
///
/// #### How a node reaches the screen
///
/// Each node has one Codename One component, its peer, created the first
/// time it is needed ([#cn1Peer()]). The peer of a parent is a container
/// holding the peers of its children. JavaFX layout runs on the nodes, in
/// logical pixels, and the result is pushed into the peers as device
/// pixels. A node that draws itself does so through [#cn1Paint(Renderer)];
/// a control shows a native Codename One component instead.
///
/// The peer covers the node's layout bounds moved by `layoutX/Y` and
/// `translateX/Y`. Scale, rotation and the `getTransforms()` list are
/// applied when painting, about the centre of the layout bounds for the
/// first two.
///
/// #### Styling
///
/// A node is the subject of a style sheet through `getTypeSelector()`,
/// `getId()`, `getStyleClass()`, `getStyle()` and its pseudo-class states,
/// and receives values through
/// [#cn1ApplyStyle(String, Object)]; see
/// `com.codename1.fxcompat.runtime.StyleTarget` for the contract and
/// `com.codename1.fxcompat.runtime.StyleEngine` for when a restyle is
/// requested. A subclass adds styleable attributes by overriding
/// [#cn1StyleValue(String)] and [#cn1SetStyleValue(String, Object)].
///
/// #### Not part of this layer
///
/// Effects, clipping by a node, blend modes, caching hints, snapshots,
/// three dimensional transforms, accessibility attributes and drag and
/// drop.
public abstract class Node implements EventTarget, Styleable, StyleTarget, Dirty.Owner {

    /// The baseline offset of a node whose baseline is its bottom edge.
    public static final double BASELINE_OFFSET_SAME_AS_HEIGHT = Double.NEGATIVE_INFINITY;

    /// What [#cn1StyleValue(String)] answers for a property the node does
    /// not have.
    protected static final Object CN1_NO_STYLE = new Object();

    private static final Object NULL_VALUE = new Object();
    private static final int VISIBILITY = 1 << 20;
    private static final int DISABLE = 1 << 21;
    private static final PseudoClass HOVER = PseudoClass.getPseudoClass("hover");
    private static final PseudoClass PRESSED = PseudoClass.getPseudoClass("pressed");
    private static final PseudoClass DISABLED = PseudoClass.getPseudoClass("disabled");
    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");

    private Component peer;
    private EventHandlerManager events;
    private final ReadOnlyObjectWrapper<Parent> parent = new ReadOnlyObjectWrapper<Parent>(this, "parent");
    private final ReadOnlyObjectWrapper<Scene> scene = new ReadOnlyObjectWrapper<Scene>(this, "scene");

    private final StringProperty id = new FxString(this, "id", null, Dirty.STYLE);
    private final StringProperty style = new FxString(this, "style", "", Dirty.STYLE);
    private final ObservableList<String> styleClass = FXCollections.observableArrayList();
    private ObservableSet<PseudoClass> pseudoClasses;
    private HashMap<String, Object> styledFrom;

    private final BooleanProperty visible = new FxBoolean(this, "visible", true, Dirty.PAINT | VISIBILITY);
    private final BooleanProperty managed = new FxBoolean(this, "managed", true, Dirty.LAYOUT);
    private final BooleanProperty disable = new FxBoolean(this, "disable", false, DISABLE);
    private final ReadOnlyBooleanWrapper disabled = new ReadOnlyBooleanWrapper(this, "disabled", false);
    private final ReadOnlyBooleanWrapper hover = new ReadOnlyBooleanWrapper(this, "hover", false);
    private final ReadOnlyBooleanWrapper pressed = new ReadOnlyBooleanWrapper(this, "pressed", false);
    private final ReadOnlyBooleanWrapper focused = new ReadOnlyBooleanWrapper(this, "focused", false);
    private final BooleanProperty focusTraversable = new FxBoolean(this, "focusTraversable", false, Dirty.NATIVE);
    private final BooleanProperty mouseTransparent = new FxBoolean(this, "mouseTransparent", false, Dirty.NONE);
    private final BooleanProperty pickOnBounds = new FxBoolean(this, "pickOnBounds", false, Dirty.NONE);

    private final DoubleProperty opacity = new FxDouble(this, "opacity", 1, Dirty.PAINT);
    private final DoubleProperty layoutX = new FxDouble(this, "layoutX", 0, Dirty.BOUNDS);
    private final DoubleProperty layoutY = new FxDouble(this, "layoutY", 0, Dirty.BOUNDS);
    private final DoubleProperty translateX = new FxDouble(this, "translateX", 0, Dirty.BOUNDS);
    private final DoubleProperty translateY = new FxDouble(this, "translateY", 0, Dirty.BOUNDS);
    private final DoubleProperty scaleX = new FxDouble(this, "scaleX", 1, Dirty.BOUNDS);
    private final DoubleProperty scaleY = new FxDouble(this, "scaleY", 1, Dirty.BOUNDS);
    private final DoubleProperty rotate = new FxDouble(this, "rotate", 0, Dirty.BOUNDS);

    private ObjectProperty<Cursor> cursor;
    private Object userData;
    private ObservableMap<Object, Object> properties;
    private Bounds layoutBoundsCache;
    private ReadOnlyObjectWrapper<Bounds> layoutBoundsProperty;
    private ReadOnlyObjectWrapper<Bounds> boundsInLocalProperty;
    private ReadOnlyObjectWrapper<Bounds> boundsInParentProperty;
    private boolean restyling;

    /// Creates a node.
    protected Node() {
        styleClass.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> change) {
                cn1Invalidated(Dirty.STYLE);
            }
        });
    }

    // ------------------------------------------------------------- peer

    /// Returns the Codename One component that shows this node, creating
    /// it the first time.
    public final Component cn1Peer() {
        if (peer == null) {
            peer = cn1CreatePeer();
            peer.setVisible(isVisible());
            cn1PeerCreated();
            cn1SyncPeerBounds();
        }
        return peer;
    }

    /// Returns whether the peer exists yet.
    public final boolean cn1HasPeer() {
        return peer != null;
    }

    /// Creates the peer. A node that draws itself keeps the default.
    protected Component cn1CreatePeer() {
        return new NodePeer(this);
    }

    /// Called once, right after the peer was created.
    protected void cn1PeerCreated() {
    }

    /// Draws this node. The renderer's origin is the node's local origin
    /// and its matrix already holds the node's scale, rotation and
    /// transforms. A parent is drawn before its children.
    public void cn1Paint(Renderer renderer) {
    }

    /// Returns whether this node draws beyond its layout bounds, as a
    /// shape with a stroke centred on its outline does. Its peer is then
    /// clipped by the parent rather than by its own bounds.
    public boolean cn1PaintsOutsideBounds() {
        return false;
    }

    /// Pushes the position and size of this node into its peer, in device
    /// pixels relative to the parent's peer. The root of a scene is placed
    /// by the form that hosts it instead.
    public final void cn1SyncPeerBounds() {
        if (peer == null) {
            return;
        }
        Parent p = getParent();
        if (p == null) {
            return;
        }
        Bounds lb = getLayoutBounds();
        Bounds plb = p.getLayoutBounds();
        double x = getLayoutX() + getTranslateX() + lb.getMinX() - plb.getMinX();
        double y = getLayoutY() + getTranslateY() + lb.getMinY() - plb.getMinY();
        int x1 = Units.toPixels(x);
        int y1 = Units.toPixels(y);
        int x2 = Units.toPixels(x + lb.getWidth());
        int y2 = Units.toPixels(y + lb.getHeight());
        if (peer.getX() != x1 || peer.getY() != y1 || peer.getWidth() != x2 - x1 || peer.getHeight() != y2 - y1) {
            peer.setX(x1);
            peer.setY(y1);
            peer.setWidth(x2 - x1);
            peer.setHeight(y2 - y1);
            cn1PeerResized();
        }
    }

    /// Called when the peer's bounds changed.
    protected void cn1PeerResized() {
    }

    /// Returns the scale, rotation and transforms of this node as the
    /// matrix `{a, b, c, d, tx, ty}` in local coordinates: everything
    /// between the node's own coordinates and its translated position in
    /// the parent. Answers `null` when there is none, the common case.
    public double[] cn1PaintMatrix() {
        double sx = getScaleX();
        double sy = getScaleY();
        double r = getRotate();
        if (sx == 1 && sy == 1 && r == 0) {
            return null;
        }
        Bounds lb = getLayoutBounds();
        double px = lb.getMinX() + lb.getWidth() / 2;
        double py = lb.getMinY() + lb.getHeight() / 2;
        double rad = Math.toRadians(r);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double a = cos * sx;
        double b = sin * sx;
        double c = -sin * sy;
        double d = cos * sy;
        return new double[] {a, b, c, d, px - a * px - c * py, py - b * px - d * py};
    }

    private double[] localToParentMatrix() {
        double[] m = cn1PaintMatrix();
        double tx = getLayoutX() + getTranslateX();
        double ty = getLayoutY() + getTranslateY();
        if (m == null) {
            return new double[] {1, 0, 0, 1, tx, ty};
        }
        m[4] += tx;
        m[5] += ty;
        return m;
    }

    // --------------------------------------------------------- dirtiness

    /// Reacts to a property change; see
    /// `com.codename1.fxcompat.runtime.Dirty` for the bits. A subclass
    /// that defines bits of its own overrides this and calls the super
    /// implementation.
    @Override
    public void cn1Invalidated(int what) {
        if ((what & VISIBILITY) != 0 && peer != null) {
            peer.setVisible(isVisible());
        }
        if ((what & DISABLE) != 0) {
            updateDisabled();
        }
        if ((what & Dirty.STYLE) != 0) {
            cn1Restyle();
        }
        if ((what & Dirty.GEOMETRY) != 0) {
            cn1GeometryChanged();
        }
        if ((what & Dirty.LAYOUT) != 0) {
            cn1RequestLayout();
        }
        if ((what & Dirty.BOUNDS) != 0) {
            cn1SyncPeerBounds();
            fireBoundsChanged();
            Parent p = getParent();
            if (p != null) {
                p.cn1ChildBoundsChanged(this);
            }
        }
        if ((what & (Dirty.PAINT | Dirty.BOUNDS)) != 0) {
            repaint((what & Dirty.BOUNDS) != 0);
        }
    }

    /// Schedules this node to be drawn again. A node that is transformed,
    /// translucent or below such a node is redrawn through the parent of
    /// the outermost one, since only that paints the transform.
    public final void cn1Repaint() {
        repaint(false);
    }

    private void repaint(boolean moved) {
        if (peer == null) {
            return;
        }
        Node top = null;
        for (Node n = this; n != null; n = n.getParent()) {
            if (n.cn1PaintMatrix() != null || n.getOpacity() < 1) {
                top = n;
            }
        }
        Node target = top == null ? this : top;
        // A moved or transformed node also uncovers part of its parent.
        if ((top != null || moved) && target.getParent() != null) {
            target = target.getParent();
        }
        if (target.peer != null) {
            target.peer.repaint();
        }
    }

    /// Asks for the layout that places this node to run again.
    protected void cn1RequestLayout() {
        Parent p = getParent();
        if (p != null) {
            p.requestLayout();
        }
    }

    /// Forgets the cached layout bounds; called when the geometry of a
    /// node that computes its own changed.
    protected final void cn1GeometryChanged() {
        layoutBoundsCache = null;
        fireBoundsChanged();
        cn1SyncPeerBounds();
        cn1RequestLayout();
        repaint(true);
        Parent p = getParent();
        if (p != null) {
            p.cn1ChildBoundsChanged(this);
        }
    }

    final boolean cn1LayoutBoundsStale(Bounds fresh) {
        Bounds old = layoutBoundsCache;
        return old != null && (old.getMinX() != fresh.getMinX() || old.getMinY() != fresh.getMinY()
                || old.getWidth() != fresh.getWidth() || old.getHeight() != fresh.getHeight());
    }

    private void fireBoundsChanged() {
        if (layoutBoundsProperty != null) {
            layoutBoundsProperty.set(getLayoutBounds());
        }
        if (boundsInLocalProperty != null) {
            boundsInLocalProperty.set(getBoundsInLocal());
        }
        if (boundsInParentProperty != null) {
            boundsInParentProperty.set(getBoundsInParent());
        }
    }

    // -------------------------------------------------------------- tree

    /// Returns the parent; `null` for a root or a detached node.
    public final Parent getParent() {
        return parent.get();
    }

    /// The parent of this node.
    public final ReadOnlyObjectProperty<Parent> parentProperty() {
        return parent.getReadOnlyProperty();
    }

    final void setParentInternal(Parent value) {
        parent.set(value);
        updateDisabled();
    }

    /// Returns the scene this node is in, or `null`.
    public final Scene getScene() {
        return scene.get();
    }

    /// The scene this node is in.
    public final ReadOnlyObjectProperty<Scene> sceneProperty() {
        return scene.getReadOnlyProperty();
    }

    void setSceneInternal(Scene value) {
        Scene old = scene.get();
        if (old == value) {
            return;
        }
        if (old != null) {
            old.cn1NodeLeft(this);
        }
        scene.set(value);
        if (value != null) {
            cn1Restyle();
        }
    }

    /// Moves this node in front of its siblings.
    public void toFront() {
        Parent p = getParent();
        if (p != null) {
            p.cn1ToFront(this);
        }
    }

    /// Moves this node behind its siblings.
    public void toBack() {
        Parent p = getParent();
        if (p != null) {
            p.cn1ToBack(this);
        }
    }

    // ----------------------------------------------------------- styling

    /// Returns the id, which an `#id` selector and [#lookup(String)] match.
    public final String getId() {
        return id.get();
    }

    /// Sets the id.
    public final void setId(String value) {
        id.set(value);
    }

    /// The id of this node.
    public final StringProperty idProperty() {
        return id;
    }

    /// Returns the inline style.
    public final String getStyle() {
        String s = style.get();
        return s == null ? "" : s;
    }

    /// Sets the inline style, CSS declarations such as
    /// `-fx-background-color: red;`. It has an effect once a style engine
    /// is installed.
    public final void setStyle(String value) {
        style.set(value);
    }

    /// The inline style of this node.
    public final StringProperty styleProperty() {
        return style;
    }

    @Override
    public final ObservableList<String> getStyleClass() {
        return styleClass;
    }

    @Override
    public String getTypeSelector() {
        Class<?> c = getClass();
        String name = c.getName();
        int dollar = name.lastIndexOf('$');
        // An anonymous subclass is selected as the class it extends.
        while (dollar >= 0 && dollar + 1 < name.length() && name.charAt(dollar + 1) >= '0'
                && name.charAt(dollar + 1) <= '9') {
            c = c.getSuperclass();
            if (c == null) {
                break;
            }
            name = c.getName();
            dollar = name.lastIndexOf('$');
        }
        int dot = Math.max(name.lastIndexOf('.'), dollar);
        return name.substring(dot + 1);
    }

    @Override
    public Styleable getStyleableParent() {
        return getParent();
    }

    @Override
    public final ObservableSet<PseudoClass> getPseudoClassStates() {
        if (pseudoClasses == null) {
            pseudoClasses = FXCollections.observableSet(new HashSet<PseudoClass>());
        }
        return pseudoClasses;
    }

    /// Turns a pseudo-class state on or off and requests a restyle when
    /// that changed something.
    public final void pseudoClassStateChanged(PseudoClass pseudoClass, boolean active) {
        if (pseudoClass == null) {
            return;
        }
        boolean changed = active ? getPseudoClassStates().add(pseudoClass)
                : (pseudoClasses != null && pseudoClasses.remove(pseudoClass));
        if (changed) {
            cn1Restyle();
        }
    }

    /// Asks the style engine to style this node now.
    public final void applyCss() {
        cn1Restyle();
    }

    /// Requests a restyle of this node from the installed engine. Does
    /// nothing outside a scene, or while the engine is styling this node.
    protected final void cn1Restyle() {
        if (getScene() == null || restyling) {
            return;
        }
        restyling = true;
        try {
            StyleEngine.getInstance().restyle(this);
        } finally {
            restyling = false;
        }
    }

    /// Sets, or with `null` restores, one styleable attribute; the entry
    /// point of `com.codename1.fxcompat.runtime.StyleTarget`.
    @Override
    public final boolean cn1ApplyStyle(String property, Object value) {
        if (property == null) {
            return false;
        }
        if (value == null) {
            if (styledFrom != null && styledFrom.containsKey(property)) {
                Object original = styledFrom.remove(property);
                cn1SetStyleValue(property, original == NULL_VALUE ? null : original);
                return true;
            }
            return cn1StyleValue(property) != CN1_NO_STYLE;
        }
        Object current = cn1StyleValue(property);
        if (current == CN1_NO_STYLE) {
            return false;
        }
        if (!cn1SetStyleValue(property, value)) {
            return false;
        }
        if (styledFrom == null) {
            styledFrom = new HashMap<String, Object>();
        }
        if (!styledFrom.containsKey(property)) {
            styledFrom.put(property, current == null ? NULL_VALUE : current);
        }
        return true;
    }

    /// Returns the current value of a styleable attribute in a form
    /// [#cn1SetStyleValue(String, Object)] accepts back, or
    /// [#CN1_NO_STYLE] when this node does not have the property.
    /// Overridden per node family; an override handles its own names and
    /// passes the rest to the super implementation.
    protected Object cn1StyleValue(String property) {
        if ("-fx-opacity".equals(property)) {
            return Double.valueOf(getOpacity());
        } else if ("-fx-rotate".equals(property)) {
            return Double.valueOf(getRotate());
        } else if ("-fx-scale-x".equals(property)) {
            return Double.valueOf(getScaleX());
        } else if ("-fx-scale-y".equals(property)) {
            return Double.valueOf(getScaleY());
        } else if ("-fx-translate-x".equals(property)) {
            return Double.valueOf(getTranslateX());
        } else if ("-fx-translate-y".equals(property)) {
            return Double.valueOf(getTranslateY());
        } else if ("-fx-cursor".equals(property)) {
            return getCursor();
        } else if ("visibility".equals(property)) {
            return Boolean.valueOf(isVisible());
        } else if ("-fx-managed".equals(property)) {
            return Boolean.valueOf(isManaged());
        }
        return CN1_NO_STYLE;
    }

    /// Sets a styleable attribute from an already parsed value. Answers
    /// `false`, changing nothing, for a property this node does not have
    /// or a value of a type the property cannot use. `null` is only
    /// passed for a property whose value was `null` before it was styled.
    protected boolean cn1SetStyleValue(String property, Object value) {
        if ("-fx-cursor".equals(property)) {
            if (value == null || value instanceof Cursor) {
                setCursor((Cursor) value);
                return true;
            }
            return false;
        } else if ("visibility".equals(property)) {
            if (value instanceof Boolean) {
                setVisible(((Boolean) value).booleanValue());
                return true;
            } else if (value instanceof String) {
                setVisible("visible".equalsIgnoreCase(((String) value).trim()));
                return true;
            }
            return false;
        } else if ("-fx-managed".equals(property)) {
            if (value instanceof Boolean) {
                setManaged(((Boolean) value).booleanValue());
                return true;
            }
            return false;
        }
        if (!(value instanceof Number)) {
            return false;
        }
        double v = ((Number) value).doubleValue();
        if ("-fx-opacity".equals(property)) {
            setOpacity(v);
        } else if ("-fx-rotate".equals(property)) {
            setRotate(v);
        } else if ("-fx-scale-x".equals(property)) {
            setScaleX(v);
        } else if ("-fx-scale-y".equals(property)) {
            setScaleY(v);
        } else if ("-fx-translate-x".equals(property)) {
            setTranslateX(v);
        } else if ("-fx-translate-y".equals(property)) {
            setTranslateY(v);
        } else {
            return false;
        }
        return true;
    }

    /// Returns the first node in this subtree, this node included, that a
    /// selector matches. Supported are type, `#id` and `.class` selectors,
    /// compounds of them and descendant chains separated by spaces.
    public Node lookup(String selector) {
        Set<Node> all = lookupAll(selector);
        return all.isEmpty() ? null : all.iterator().next();
    }

    /// Returns every node in this subtree, this node included, that a
    /// selector matches, in document order.
    public Set<Node> lookupAll(String selector) {
        LinkedHashSet<Node> found = new LinkedHashSet<Node>();
        if (selector == null) {
            return found;
        }
        String s = selector.trim();
        if (s.length() == 0) {
            return found;
        }
        int space = s.indexOf(' ');
        String first = space < 0 ? s : s.substring(0, space);
        String rest = space < 0 ? null : s.substring(space + 1).trim();
        LinkedHashSet<Node> stage = new LinkedHashSet<Node>();
        collect(first, stage);
        if (rest == null || rest.length() == 0) {
            return stage;
        }
        for (Node n : stage) {
            if (n instanceof Parent) {
                ObservableList<Node> children = ((Parent) n).getChildrenUnmodifiable();
                for (int i = 0; i < children.size(); i++) {
                    found.addAll(children.get(i).lookupAll(rest));
                }
            }
        }
        return found;
    }

    void collect(String compound, Set<Node> into) {
        if (cn1Matches(compound)) {
            into.add(this);
        }
    }

    /// Returns whether a compound selector without combinators, such as
    /// `Button.primary#ok`, matches this node.
    public final boolean cn1Matches(String compound) {
        int i = 0;
        int n = compound.length();
        while (i < n) {
            char kind = compound.charAt(i);
            int start = kind == '#' || kind == '.' ? i + 1 : i;
            int end = start;
            while (end < n && compound.charAt(end) != '#' && compound.charAt(end) != '.') {
                end++;
            }
            String token = compound.substring(start, end);
            if (kind == '#') {
                if (!token.equals(getId())) {
                    return false;
                }
            } else if (kind == '.') {
                if (!styleClass.contains(token)) {
                    return false;
                }
            } else if (!"*".equals(token) && !token.equals(getTypeSelector())) {
                return false;
            }
            i = end;
        }
        return n > 0;
    }

    // ------------------------------------------------------------- state

    /// Returns whether the node is shown. An invisible node still takes
    /// its place in layout.
    public final boolean isVisible() {
        return visible.get();
    }

    /// Sets whether the node is shown.
    public final void setVisible(boolean value) {
        visible.set(value);
    }

    /// Whether the node is shown.
    public final BooleanProperty visibleProperty() {
        return visible;
    }

    /// Returns whether the parent lays this node out.
    public final boolean isManaged() {
        return managed.get();
    }

    /// Sets whether the parent lays this node out.
    public final void setManaged(boolean value) {
        managed.set(value);
    }

    /// Whether the parent lays this node out.
    public final BooleanProperty managedProperty() {
        return managed;
    }

    /// Returns whether this node itself was disabled.
    public final boolean isDisable() {
        return disable.get();
    }

    /// Disables or enables this node and everything below it.
    public final void setDisable(boolean value) {
        disable.set(value);
    }

    /// Whether this node itself was disabled.
    public final BooleanProperty disableProperty() {
        return disable;
    }

    /// Returns whether this node or an ancestor is disabled.
    public final boolean isDisabled() {
        return disabled.get();
    }

    /// Whether this node or an ancestor is disabled.
    public final ReadOnlyBooleanProperty disabledProperty() {
        return disabled.getReadOnlyProperty();
    }

    /// Sets whether this node is disabled; for subclasses.
    protected final void setDisabled(boolean value) {
        if (disabled.get() != value) {
            disabled.set(value);
            pseudoClassStateChanged(DISABLED, value);
            cn1Invalidated(Dirty.NATIVE | Dirty.PAINT);
            cn1DisabledChanged();
        }
    }

    final void updateDisabled() {
        Parent p = getParent();
        setDisabled(isDisable() || (p != null && p.isDisabled()));
    }

    void cn1DisabledChanged() {
    }

    /// Returns whether the pointer is over this node.
    public final boolean isHover() {
        return hover.get();
    }

    /// Whether the pointer is over this node.
    public final ReadOnlyBooleanProperty hoverProperty() {
        return hover.getReadOnlyProperty();
    }

    /// Sets whether the pointer is over this node; for subclasses.
    protected final void setHover(boolean value) {
        if (hover.get() != value) {
            hover.set(value);
            pseudoClassStateChanged(HOVER, value);
        }
    }

    /// Returns whether a button is pressed on this node.
    public final boolean isPressed() {
        return pressed.get();
    }

    /// Whether a button is pressed on this node.
    public final ReadOnlyBooleanProperty pressedProperty() {
        return pressed.getReadOnlyProperty();
    }

    /// Sets whether a button is pressed on this node; for subclasses.
    protected final void setPressed(boolean value) {
        if (pressed.get() != value) {
            pressed.set(value);
            pseudoClassStateChanged(PRESSED, value);
        }
    }

    final void cn1SetHover(boolean value) {
        setHover(value);
    }

    final void cn1SetPressed(boolean value) {
        setPressed(value);
    }

    /// Returns whether this node has the keyboard focus.
    public final boolean isFocused() {
        return focused.get();
    }

    /// Whether this node has the keyboard focus.
    public final ReadOnlyBooleanProperty focusedProperty() {
        return focused.getReadOnlyProperty();
    }

    /// Sets whether this node has the focus; for subclasses.
    protected final void setFocused(boolean value) {
        if (focused.get() != value) {
            focused.set(value);
            pseudoClassStateChanged(FOCUSED, value);
        }
    }

    final void cn1SetFocused(boolean value) {
        setFocused(value);
    }

    /// Returns whether Tab moves the focus to this node.
    public final boolean isFocusTraversable() {
        return focusTraversable.get();
    }

    /// Sets whether Tab moves the focus to this node.
    public final void setFocusTraversable(boolean value) {
        focusTraversable.set(value);
    }

    /// Whether Tab moves the focus to this node.
    public final BooleanProperty focusTraversableProperty() {
        return focusTraversable;
    }

    /// Asks for the keyboard focus. It is given when the node is in a
    /// scene, visible and not disabled.
    public void requestFocus() {
        Scene s = getScene();
        if (s != null && isVisible() && !isDisabled()) {
            s.cn1SetFocusOwner(this, true);
        }
    }

    /// Called when the scene gives this node the focus through
    /// [#requestFocus()], so a control can move the native focus too.
    protected void cn1FocusRequested() {
    }

    /// Returns whether pointer events pass through this node and its
    /// children.
    public final boolean isMouseTransparent() {
        return mouseTransparent.get();
    }

    /// Sets whether pointer events pass through this node.
    public final void setMouseTransparent(boolean value) {
        mouseTransparent.set(value);
    }

    /// Whether pointer events pass through this node.
    public final BooleanProperty mouseTransparentProperty() {
        return mouseTransparent;
    }

    /// Returns whether the node is hit anywhere in its bounds rather than
    /// only on its shape.
    public final boolean isPickOnBounds() {
        return pickOnBounds.get();
    }

    /// Sets whether the node is hit anywhere in its bounds.
    public final void setPickOnBounds(boolean value) {
        pickOnBounds.set(value);
    }

    /// Whether the node is hit anywhere in its bounds.
    public final BooleanProperty pickOnBoundsProperty() {
        return pickOnBounds;
    }

    /// Returns the cursor shown over this node, or `null` to inherit.
    public final Cursor getCursor() {
        return cursor == null ? null : cursor.get();
    }

    /// Sets the cursor shown over this node.
    public final void setCursor(Cursor value) {
        if (value != null || cursor != null) {
            cursorProperty().set(value);
        }
    }

    /// The cursor shown over this node.
    public final ObjectProperty<Cursor> cursorProperty() {
        if (cursor == null) {
            cursor = new FxObject<Cursor>(this, "cursor", null, Dirty.NATIVE);
        }
        return cursor;
    }

    /// Returns the application's own object attached to this node.
    public Object getUserData() {
        return userData;
    }

    /// Attaches an object of the application to this node.
    public void setUserData(Object value) {
        userData = value;
    }

    /// Returns a map for the application's and the layout panes' own
    /// values on this node.
    public final ObservableMap<Object, Object> getProperties() {
        if (properties == null) {
            properties = FXCollections.observableHashMap();
        }
        return properties;
    }

    /// Returns whether [#getProperties()] holds anything.
    public boolean hasProperties() {
        return properties != null && !properties.isEmpty();
    }

    // ---------------------------------------------------------- geometry

    /// Returns the opacity, 0 to 1.
    public final double getOpacity() {
        return opacity.get();
    }

    /// Sets the opacity of this node and its children.
    public final void setOpacity(double value) {
        opacity.set(value);
    }

    /// The opacity of this node.
    public final DoubleProperty opacityProperty() {
        return opacity;
    }

    /// Returns the x the layout placed this node at.
    public final double getLayoutX() {
        return layoutX.get();
    }

    /// Sets the x of this node in its parent.
    public final void setLayoutX(double value) {
        layoutX.set(value);
    }

    /// The x of this node in its parent.
    public final DoubleProperty layoutXProperty() {
        return layoutX;
    }

    /// Returns the y the layout placed this node at.
    public final double getLayoutY() {
        return layoutY.get();
    }

    /// Sets the y of this node in its parent.
    public final void setLayoutY(double value) {
        layoutY.set(value);
    }

    /// The y of this node in its parent.
    public final DoubleProperty layoutYProperty() {
        return layoutY;
    }

    /// Returns the x offset added after layout.
    public final double getTranslateX() {
        return translateX.get();
    }

    /// Sets the x offset added after layout.
    public final void setTranslateX(double value) {
        translateX.set(value);
    }

    /// The x offset added after layout.
    public final DoubleProperty translateXProperty() {
        return translateX;
    }

    /// Returns the y offset added after layout.
    public final double getTranslateY() {
        return translateY.get();
    }

    /// Sets the y offset added after layout.
    public final void setTranslateY(double value) {
        translateY.set(value);
    }

    /// The y offset added after layout.
    public final DoubleProperty translateYProperty() {
        return translateY;
    }

    /// Returns the horizontal scale about the centre.
    public final double getScaleX() {
        return scaleX.get();
    }

    /// Sets the horizontal scale about the centre.
    public final void setScaleX(double value) {
        scaleX.set(value);
    }

    /// The horizontal scale about the centre.
    public final DoubleProperty scaleXProperty() {
        return scaleX;
    }

    /// Returns the vertical scale about the centre.
    public final double getScaleY() {
        return scaleY.get();
    }

    /// Sets the vertical scale about the centre.
    public final void setScaleY(double value) {
        scaleY.set(value);
    }

    /// The vertical scale about the centre.
    public final DoubleProperty scaleYProperty() {
        return scaleY;
    }

    /// Returns the clockwise rotation about the centre, in degrees.
    public final double getRotate() {
        return rotate.get();
    }

    /// Sets the clockwise rotation about the centre, in degrees.
    public final void setRotate(double value) {
        rotate.set(value);
    }

    /// The rotation about the centre.
    public final DoubleProperty rotateProperty() {
        return rotate;
    }

    /// Computes the layout bounds; overridden by every node with a size.
    protected Bounds cn1ComputeLayoutBounds() {
        return new BoundingBox(0, 0, 0, 0);
    }

    /// Returns the rectangle layout uses for this node, in its own
    /// coordinates.
    public final Bounds getLayoutBounds() {
        if (layoutBoundsCache == null) {
            layoutBoundsCache = cn1ComputeLayoutBounds();
        }
        return layoutBoundsCache;
    }

    /// The rectangle layout uses for this node.
    public final ReadOnlyObjectProperty<Bounds> layoutBoundsProperty() {
        if (layoutBoundsProperty == null) {
            layoutBoundsProperty = new ReadOnlyObjectWrapper<Bounds>(this, "layoutBounds", getLayoutBounds());
        }
        return layoutBoundsProperty.getReadOnlyProperty();
    }

    /// Returns the bounds of what this node draws, in its own coordinates.
    public final Bounds getBoundsInLocal() {
        return cn1ComputeBoundsInLocal();
    }

    /// The bounds of what this node draws.
    public final ReadOnlyObjectProperty<Bounds> boundsInLocalProperty() {
        if (boundsInLocalProperty == null) {
            boundsInLocalProperty = new ReadOnlyObjectWrapper<Bounds>(this, "boundsInLocal", getBoundsInLocal());
        }
        return boundsInLocalProperty.getReadOnlyProperty();
    }

    /// Computes the bounds in local coordinates; the layout bounds unless
    /// overridden.
    protected Bounds cn1ComputeBoundsInLocal() {
        return getLayoutBounds();
    }

    /// Returns the bounds of this node in its parent's coordinates, after
    /// every transform.
    public final Bounds getBoundsInParent() {
        return transformBounds(getBoundsInLocal(), localToParentMatrix());
    }

    /// The bounds of this node in its parent's coordinates.
    public final ReadOnlyObjectProperty<Bounds> boundsInParentProperty() {
        if (boundsInParentProperty == null) {
            boundsInParentProperty = new ReadOnlyObjectWrapper<Bounds>(this, "boundsInParent", getBoundsInParent());
        }
        return boundsInParentProperty.getReadOnlyProperty();
    }

    private static Bounds transformBounds(Bounds b, double[] m) {
        double[] xs = {b.getMinX(), b.getMaxX(), b.getMaxX(), b.getMinX()};
        double[] ys = {b.getMinY(), b.getMinY(), b.getMaxY(), b.getMaxY()};
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            double x = m[0] * xs[i] + m[2] * ys[i] + m[4];
            double y = m[1] * xs[i] + m[3] * ys[i] + m[5];
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    /// Converts a point from this node's coordinates to its parent's.
    public Point2D localToParent(double localX, double localY) {
        double[] m = localToParentMatrix();
        return new Point2D(m[0] * localX + m[2] * localY + m[4], m[1] * localX + m[3] * localY + m[5]);
    }

    /// Converts a point from this node's coordinates to its parent's.
    public Point2D localToParent(Point2D localPoint) {
        return localToParent(localPoint.getX(), localPoint.getY());
    }

    /// Converts bounds from this node's coordinates to its parent's.
    public Bounds localToParent(Bounds localBounds) {
        return transformBounds(localBounds, localToParentMatrix());
    }

    /// Converts a point from the parent's coordinates to this node's.
    public Point2D parentToLocal(double parentX, double parentY) {
        double[] m = localToParentMatrix();
        double det = m[0] * m[3] - m[1] * m[2];
        if (det == 0) {
            return new Point2D(0, 0);
        }
        double x = parentX - m[4];
        double y = parentY - m[5];
        return new Point2D((m[3] * x - m[2] * y) / det, (m[0] * y - m[1] * x) / det);
    }

    /// Converts a point from the parent's coordinates to this node's.
    public Point2D parentToLocal(Point2D parentPoint) {
        return parentToLocal(parentPoint.getX(), parentPoint.getY());
    }

    /// Converts a point from this node's coordinates to the scene's.
    public Point2D localToScene(double localX, double localY) {
        Point2D p = localToParent(localX, localY);
        Parent up = getParent();
        while (up != null) {
            p = up.localToParent(p.getX(), p.getY());
            up = up.getParent();
        }
        return p;
    }

    /// Converts a point from this node's coordinates to the scene's.
    public Point2D localToScene(Point2D localPoint) {
        return localToScene(localPoint.getX(), localPoint.getY());
    }

    /// Converts bounds from this node's coordinates to the scene's.
    public Bounds localToScene(Bounds localBounds) {
        Bounds b = localToParent(localBounds);
        Parent up = getParent();
        while (up != null) {
            b = up.localToParent(b);
            up = up.getParent();
        }
        return b;
    }

    /// Converts a point from the scene's coordinates to this node's.
    public Point2D sceneToLocal(double sceneX, double sceneY) {
        Parent up = getParent();
        Point2D p = up == null ? new Point2D(sceneX, sceneY) : up.sceneToLocal(sceneX, sceneY);
        return parentToLocal(p.getX(), p.getY());
    }

    /// Converts a point from the scene's coordinates to this node's.
    public Point2D sceneToLocal(Point2D scenePoint) {
        return sceneToLocal(scenePoint.getX(), scenePoint.getY());
    }

    /// Returns whether a point in this node's coordinates is on the node.
    public boolean contains(double localX, double localY) {
        return getLayoutBounds().contains(localX, localY);
    }

    /// Returns whether a point in this node's coordinates is on the node.
    public boolean contains(Point2D localPoint) {
        return contains(localPoint.getX(), localPoint.getY());
    }

    /// Returns whether a rectangle in this node's coordinates touches it.
    public boolean intersects(double localX, double localY, double localWidth, double localHeight) {
        return getBoundsInLocal().intersects(localX, localY, localWidth, localHeight);
    }

    /// Returns whether bounds in this node's coordinates touch it.
    public boolean intersects(Bounds localBounds) {
        return getBoundsInLocal().intersects(localBounds);
    }

    /// Returns the node under a point given in the parent's coordinates:
    /// this node, one below it, or `null`.
    public Node cn1Pick(double parentX, double parentY) {
        if (!isVisible() || isMouseTransparent()) {
            return null;
        }
        Point2D local = parentToLocal(parentX, parentY);
        return cn1PickLocal(local.getX(), local.getY());
    }

    /// Returns the node under a point in this node's coordinates.
    protected Node cn1PickLocal(double x, double y) {
        if (isPickOnBounds() ? getLayoutBounds().contains(x, y) : contains(x, y)) {
            return this;
        }
        return null;
    }

    // ------------------------------------------------------------ layout

    /// Returns whether a parent may resize this node.
    public boolean isResizable() {
        return false;
    }

    /// Returns whether one dimension of the preferred size depends on the
    /// other, and which leads; `null` when they are independent.
    public Orientation getContentBias() {
        return null;
    }

    /// Returns the smallest width this node can take.
    public double minWidth(double height) {
        return prefWidth(height);
    }

    /// Returns the smallest height this node can take.
    public double minHeight(double width) {
        return prefHeight(width);
    }

    /// Returns the width this node would like.
    public double prefWidth(double height) {
        double w = getLayoutBounds().getWidth();
        return Double.isNaN(w) || w < 0 ? 0 : w;
    }

    /// Returns the height this node would like.
    public double prefHeight(double width) {
        double h = getLayoutBounds().getHeight();
        return Double.isNaN(h) || h < 0 ? 0 : h;
    }

    /// Returns the largest width this node can take.
    public double maxWidth(double height) {
        return prefWidth(height);
    }

    /// Returns the largest height this node can take.
    public double maxHeight(double width) {
        return prefHeight(width);
    }

    /// Sets the size of a resizable node; does nothing for others.
    public void resize(double width, double height) {
    }

    /// Moves the node so the top left corner of its layout bounds is at a
    /// point of the parent.
    public void relocate(double x, double y) {
        Bounds lb = getLayoutBounds();
        setLayoutX(x - lb.getMinX());
        setLayoutY(y - lb.getMinY());
    }

    /// Resizes and moves the node.
    public void resizeRelocate(double x, double y, double width, double height) {
        resize(width, height);
        relocate(x, y);
    }

    /// Gives a resizable node its preferred size.
    public final void autosize() {
        if (isResizable()) {
            Orientation bias = getContentBias();
            double w;
            double h;
            if (bias == null) {
                w = bounded(prefWidth(-1), minWidth(-1), maxWidth(-1));
                h = bounded(prefHeight(-1), minHeight(-1), maxHeight(-1));
            } else if (bias == Orientation.HORIZONTAL) {
                w = bounded(prefWidth(-1), minWidth(-1), maxWidth(-1));
                h = bounded(prefHeight(w), minHeight(w), maxHeight(w));
            } else {
                h = bounded(prefHeight(-1), minHeight(-1), maxHeight(-1));
                w = bounded(prefWidth(h), minWidth(h), maxWidth(h));
            }
            resize(w, h);
        }
    }

    private static double bounded(double value, double min, double max) {
        return Math.min(Math.max(value, min), Math.max(min, max));
    }

    /// Returns the distance from the top of the layout bounds to the
    /// baseline of the node's text; the bottom edge when it has none.
    public double getBaselineOffset() {
        Bounds lb = getLayoutBounds();
        return lb.getMinY() + lb.getHeight();
    }

    // ------------------------------------------------------------ events

    /// Returns the filters and handlers of this node.
    protected final EventHandlerManager cn1Events() {
        if (events == null) {
            events = new EventHandlerManager(this);
        }
        return events;
    }

    /// Adds a handler called in the bubbling phase.
    public final <T extends Event> void addEventHandler(EventType<T> eventType,
            EventHandler<? super T> eventHandler) {
        cn1Events().addEventHandler(eventType, eventHandler);
    }

    /// Removes a handler.
    public final <T extends Event> void removeEventHandler(EventType<T> eventType,
            EventHandler<? super T> eventHandler) {
        cn1Events().removeEventHandler(eventType, eventHandler);
    }

    /// Adds a filter called in the capturing phase.
    public final <T extends Event> void addEventFilter(EventType<T> eventType,
            EventHandler<? super T> eventFilter) {
        cn1Events().addEventFilter(eventType, eventFilter);
    }

    /// Removes a filter.
    public final <T extends Event> void removeEventFilter(EventType<T> eventType,
            EventHandler<? super T> eventFilter) {
        cn1Events().removeEventFilter(eventType, eventFilter);
    }

    /// Sets the one convenience handler of an event type; for subclasses
    /// that offer `setOn...` methods.
    protected final <T extends Event> void setEventHandler(EventType<T> eventType,
            EventHandler<? super T> eventHandler) {
        cn1Events().setSlot(eventType, eventType.getName(), eventHandler);
    }

    /// Delivers an event to this node through the scene graph.
    public final void fireEvent(Event event) {
        Event.fireEvent(this, event);
    }

    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        EventDispatchChain chain = tail;
        Node n = this;
        while (n != null) {
            if (n.events != null) {
                chain = chain.prepend(n.events);
            }
            n = n.getParent();
        }
        Scene s = getScene();
        if (s != null) {
            chain = s.buildEventDispatchChain(chain);
        }
        return chain;
    }

    private <T extends Event> ObjectProperty<EventHandler<? super T>> on(EventType<T> type, String name) {
        return cn1Events().slot(type, name);
    }

    private <T extends Event> EventHandler<? super T> handler(EventType<T> type) {
        return events == null ? null : events.getSlot(type);
    }

    /// Sets the handler of a click on this node.
    public final void setOnMouseClicked(EventHandler<? super MouseEvent> value) {
        cn1Events().setSlot(MouseEvent.MOUSE_CLICKED, "onMouseClicked", value);
    }

    /// Returns the handler of a click on this node.
    public final EventHandler<? super MouseEvent> getOnMouseClicked() {
        return handler(MouseEvent.MOUSE_CLICKED);
    }

    /// The handler of a click on this node.
    public final ObjectProperty<EventHandler<? super MouseEvent>> onMouseClickedProperty() {
        return on(MouseEvent.MOUSE_CLICKED, "onMouseClicked");
    }

    /// Sets the handler of a button going down on this node.
    public final void setOnMousePressed(EventHandler<? super MouseEvent> value) {
        cn1Events().setSlot(MouseEvent.MOUSE_PRESSED, "onMousePressed", value);
    }

    /// Returns the handler of a button going down on this node.
    public final EventHandler<? super MouseEvent> getOnMousePressed() {
        return handler(MouseEvent.MOUSE_PRESSED);
    }

    /// The handler of a button going down on this node.
    public final ObjectProperty<EventHandler<? super MouseEvent>> onMousePressedProperty() {
        return on(MouseEvent.MOUSE_PRESSED, "onMousePressed");
    }

    /// Sets the handler of a button coming up.
    public final void setOnMouseReleased(EventHandler<? super MouseEvent> value) {
        cn1Events().setSlot(MouseEvent.MOUSE_RELEASED, "onMouseReleased", value);
    }

    /// Returns the handler of a button coming up.
    public final EventHandler<? super MouseEvent> getOnMouseReleased() {
        return handler(MouseEvent.MOUSE_RELEASED);
    }

    /// The handler of a button coming up.
    public final ObjectProperty<EventHandler<? super MouseEvent>> onMouseReleasedProperty() {
        return on(MouseEvent.MOUSE_RELEASED, "onMouseReleased");
    }

    /// Sets the handler of the pointer moving with a button down.
    public final void setOnMouseDragged(EventHandler<? super MouseEvent> value) {
        cn1Events().setSlot(MouseEvent.MOUSE_DRAGGED, "onMouseDragged", value);
    }

    /// Returns the handler of the pointer moving with a button down.
    public final EventHandler<? super MouseEvent> getOnMouseDragged() {
        return handler(MouseEvent.MOUSE_DRAGGED);
    }

    /// The handler of the pointer moving with a button down.
    public final ObjectProperty<EventHandler<? super MouseEvent>> onMouseDraggedProperty() {
        return on(MouseEvent.MOUSE_DRAGGED, "onMouseDragged");
    }

    /// Sets the handler of the pointer moving with no button down.
    public final void setOnMouseMoved(EventHandler<? super MouseEvent> value) {
        cn1Events().setSlot(MouseEvent.MOUSE_MOVED, "onMouseMoved", value);
    }

    /// Returns the handler of the pointer moving with no button down.
    public final EventHandler<? super MouseEvent> getOnMouseMoved() {
        return handler(MouseEvent.MOUSE_MOVED);
    }

    /// The handler of the pointer moving with no button down.
    public final ObjectProperty<EventHandler<? super MouseEvent>> onMouseMovedProperty() {
        return on(MouseEvent.MOUSE_MOVED, "onMouseMoved");
    }

    /// Sets the handler of the pointer entering this node.
    public final void setOnMouseEntered(EventHandler<? super MouseEvent> value) {
        cn1Events().setSlot(MouseEvent.MOUSE_ENTERED, "onMouseEntered", value);
    }

    /// Returns the handler of the pointer entering this node.
    public final EventHandler<? super MouseEvent> getOnMouseEntered() {
        return handler(MouseEvent.MOUSE_ENTERED);
    }

    /// The handler of the pointer entering this node.
    public final ObjectProperty<EventHandler<? super MouseEvent>> onMouseEnteredProperty() {
        return on(MouseEvent.MOUSE_ENTERED, "onMouseEntered");
    }

    /// Sets the handler of the pointer leaving this node.
    public final void setOnMouseExited(EventHandler<? super MouseEvent> value) {
        cn1Events().setSlot(MouseEvent.MOUSE_EXITED, "onMouseExited", value);
    }

    /// Returns the handler of the pointer leaving this node.
    public final EventHandler<? super MouseEvent> getOnMouseExited() {
        return handler(MouseEvent.MOUSE_EXITED);
    }

    /// The handler of the pointer leaving this node.
    public final ObjectProperty<EventHandler<? super MouseEvent>> onMouseExitedProperty() {
        return on(MouseEvent.MOUSE_EXITED, "onMouseExited");
    }

    /// Sets the handler of a drag gesture being recognised.
    public final void setOnDragDetected(EventHandler<? super MouseEvent> value) {
        cn1Events().setSlot(MouseEvent.DRAG_DETECTED, "onDragDetected", value);
    }

    /// Returns the handler of a drag gesture being recognised.
    public final EventHandler<? super MouseEvent> getOnDragDetected() {
        return handler(MouseEvent.DRAG_DETECTED);
    }

    /// The handler of a drag gesture being recognised.
    public final ObjectProperty<EventHandler<? super MouseEvent>> onDragDetectedProperty() {
        return on(MouseEvent.DRAG_DETECTED, "onDragDetected");
    }

    /// Sets the handler of a key going down while this node has the focus.
    public final void setOnKeyPressed(EventHandler<? super KeyEvent> value) {
        cn1Events().setSlot(KeyEvent.KEY_PRESSED, "onKeyPressed", value);
    }

    /// Returns the handler of a key going down.
    public final EventHandler<? super KeyEvent> getOnKeyPressed() {
        return handler(KeyEvent.KEY_PRESSED);
    }

    /// The handler of a key going down.
    public final ObjectProperty<EventHandler<? super KeyEvent>> onKeyPressedProperty() {
        return on(KeyEvent.KEY_PRESSED, "onKeyPressed");
    }

    /// Sets the handler of a key coming up.
    public final void setOnKeyReleased(EventHandler<? super KeyEvent> value) {
        cn1Events().setSlot(KeyEvent.KEY_RELEASED, "onKeyReleased", value);
    }

    /// Returns the handler of a key coming up.
    public final EventHandler<? super KeyEvent> getOnKeyReleased() {
        return handler(KeyEvent.KEY_RELEASED);
    }

    /// The handler of a key coming up.
    public final ObjectProperty<EventHandler<? super KeyEvent>> onKeyReleasedProperty() {
        return on(KeyEvent.KEY_RELEASED, "onKeyReleased");
    }

    /// Sets the handler of a character being typed.
    public final void setOnKeyTyped(EventHandler<? super KeyEvent> value) {
        cn1Events().setSlot(KeyEvent.KEY_TYPED, "onKeyTyped", value);
    }

    /// Returns the handler of a character being typed.
    public final EventHandler<? super KeyEvent> getOnKeyTyped() {
        return handler(KeyEvent.KEY_TYPED);
    }

    /// The handler of a character being typed.
    public final ObjectProperty<EventHandler<? super KeyEvent>> onKeyTypedProperty() {
        return on(KeyEvent.KEY_TYPED, "onKeyTyped");
    }

    /// Sets the handler of the wheel scrolling over this node.
    public final void setOnScroll(EventHandler<? super ScrollEvent> value) {
        cn1Events().setSlot(ScrollEvent.SCROLL, "onScroll", value);
    }

    /// Returns the handler of the wheel scrolling over this node.
    public final EventHandler<? super ScrollEvent> getOnScroll() {
        return handler(ScrollEvent.SCROLL);
    }

    /// The handler of the wheel scrolling over this node.
    public final ObjectProperty<EventHandler<? super ScrollEvent>> onScrollProperty() {
        return on(ScrollEvent.SCROLL, "onScroll");
    }

    /// Sets the handler of a context menu being asked for.
    public final void setOnContextMenuRequested(EventHandler<? super ContextMenuEvent> value) {
        cn1Events().setSlot(ContextMenuEvent.CONTEXT_MENU_REQUESTED, "onContextMenuRequested", value);
    }

    /// Returns the handler of a context menu being asked for.
    public final EventHandler<? super ContextMenuEvent> getOnContextMenuRequested() {
        return handler(ContextMenuEvent.CONTEXT_MENU_REQUESTED);
    }

    /// The handler of a context menu being asked for.
    public final ObjectProperty<EventHandler<? super ContextMenuEvent>> onContextMenuRequestedProperty() {
        return on(ContextMenuEvent.CONTEXT_MENU_REQUESTED, "onContextMenuRequested");
    }

    /// Returns the class name and, when set, the id and style classes.
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(getTypeSelector());
        String nodeId = getId();
        if (nodeId != null && nodeId.length() > 0) {
            s.append("[id=").append(nodeId).append(']');
        }
        if (!styleClass.isEmpty()) {
            s.append(styleClass);
        }
        return s.toString();
    }
}
