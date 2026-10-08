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

import com.codename1.fxcompat.runtime.EventHandlerManager;
import com.codename1.fxcompat.runtime.SceneHost;
import com.codename1.fxcompat.runtime.SceneInput;
import com.codename1.fxcompat.runtime.StyleEngine;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Window;

/// The content of a window: a tree of nodes below one root.
///
/// The scene fills the Codename One form, dialog or desktop window that
/// shows its stage. A resizable root takes the size of the scene; layout
/// runs before a frame whenever something asked for it.
///
/// #### Input
///
/// Pointer and key callbacks of the host become JavaFX events here. A
/// mouse event goes to the deepest visible node under the pointer that is
/// not mouse transparent; between a press and its release every event
/// goes to the node that was pressed. A node that is disabled, or below a
/// disabled node, takes the pointer but receives nothing. A key event
/// goes to the focus owner, or to the scene when there is none. Tab and
/// Shift+Tab move the focus through the focus traversable nodes in
/// document order, unless a handler consumed the key. A secondary button
/// release, and a long press on a touch screen, also ask for a context
/// menu.
///
/// The camera, depth buffer, anti-aliasing, mnemonics, accelerators, drag
/// and drop and snapshots of JavaFX are not part of this layer.
public class Scene implements EventTarget {

    private static final double DRAG_THRESHOLD = 4;
    private static final long MULTI_CLICK_MILLIS = 500;

    private final ReadOnlyDoubleWrapper width = new ReadOnlyDoubleWrapper(this, "width", 0);
    private final ReadOnlyDoubleWrapper height = new ReadOnlyDoubleWrapper(this, "height", 0);
    private final ReadOnlyObjectWrapper<Window> window = new ReadOnlyObjectWrapper<Window>(this, "window");
    private final ReadOnlyObjectWrapper<Node> focusOwner = new ReadOnlyObjectWrapper<Node>(this, "focusOwner");
    private final ObjectProperty<Paint> fill = new SimpleObjectProperty<Paint>(this, "fill", Color.WHITE);
    private final ObjectProperty<Cursor> cursor = new SimpleObjectProperty<Cursor>(this, "cursor");
    private final ObservableList<String> stylesheets = FXCollections.observableArrayList();
    private final EventHandlerManager events = new EventHandlerManager(this);
    private final double initialWidth;
    private final double initialHeight;
    private ObservableMap<Object, Object> properties;
    private Object userData;
    private SceneHost host;

    private final ArrayList<Node> hovered = new ArrayList<Node>();
    private final ArrayList<Node> pressedNodes = new ArrayList<Node>();
    private EventTarget pressTarget;
    private MouseButton pressButton = MouseButton.NONE;
    private double pressX;
    private double pressY;
    private boolean still;
    private boolean dragDetected;
    private long lastClickTime;
    private double lastClickX;
    private double lastClickY;
    private int clickCount;
    private int shownCursor = -1;

    private final ObjectProperty<Parent> root = new ObjectPropertyBase<Parent>() {
        private Parent old;

        @Override
        protected void invalidated() {
            Parent value = get();
            if (value == old) {
                return;
            }
            if (value == null) {
                throw new NullPointerException("The root of a scene must not be null");
            }
            if (value.getParent() != null) {
                throw new IllegalArgumentException(value + " is already inside a scene graph and cannot be a root");
            }
            if (value.getScene() != null && value.getScene() != Scene.this) {
                throw new IllegalArgumentException(value + " is already the root of another scene");
            }
            if (old != null) {
                old.setSceneInternal(null);
            }
            old = value;
            if (!value.getStyleClass().contains("root")) {
                value.getStyleClass().add(0, "root");
            }
            value.setSceneInternal(Scene.this);
            if (host != null) {
                host.rootChanged();
            }
            value.requestLayout();
            cn1RequestPulse();
        }

        @Override
        public Object getBean() {
            return Scene.this;
        }

        @Override
        public String getName() {
            return "root";
        }
    };

    /// Creates a scene that takes its size from its root, or from the
    /// screen where a window cannot be sized.
    public Scene(Parent root) {
        this(root, -1, -1, Color.WHITE);
    }

    /// Creates a scene of a size, in logical pixels.
    public Scene(Parent root, double width, double height) {
        this(root, width, height, Color.WHITE);
    }

    /// Creates a scene with a background.
    public Scene(Parent root, Paint fill) {
        this(root, -1, -1, fill);
    }

    /// Creates a scene of a size with a background.
    public Scene(Parent root, double width, double height, Paint fill) {
        this.initialWidth = width;
        this.initialHeight = height;
        this.fill.set(fill);
        stylesheets.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> change) {
                StyleEngine.getInstance().stylesheetsChanged(Scene.this);
            }
        });
        if (width >= 0) {
            this.width.set(width);
        }
        if (height >= 0) {
            this.height.set(height);
        }
        setRoot(root);
    }

    // ---------------------------------------------------------- content

    /// Returns the root of the scene graph.
    public final Parent getRoot() {
        return root.get();
    }

    /// Replaces the root of the scene graph.
    public final void setRoot(Parent value) {
        root.set(value);
    }

    /// The root of the scene graph.
    public final ObjectProperty<Parent> rootProperty() {
        return root;
    }

    /// Returns the width of the scene.
    public final double getWidth() {
        return width.get();
    }

    /// The width of the scene.
    public final ReadOnlyDoubleProperty widthProperty() {
        return width.getReadOnlyProperty();
    }

    /// Returns the height of the scene.
    public final double getHeight() {
        return height.get();
    }

    /// The height of the scene.
    public final ReadOnlyDoubleProperty heightProperty() {
        return height.getReadOnlyProperty();
    }

    /// Returns the x of the scene in its window; always 0.
    public final double getX() {
        return 0;
    }

    /// Returns the y of the scene in its window; always 0.
    public final double getY() {
        return 0;
    }

    /// Returns the paint behind the root.
    public final Paint getFill() {
        return fill.get();
    }

    /// Sets the paint behind the root.
    public final void setFill(Paint value) {
        fill.set(value);
        if (host != null) {
            host.rootChanged();
        }
    }

    /// The paint behind the root.
    public final ObjectProperty<Paint> fillProperty() {
        return fill;
    }

    /// Returns the window showing this scene, or `null`.
    public final Window getWindow() {
        return window.get();
    }

    /// The window showing this scene.
    public final ReadOnlyObjectProperty<Window> windowProperty() {
        return window.getReadOnlyProperty();
    }

    /// Returns the cursor of the scene, used where no node sets one.
    public final Cursor getCursor() {
        return cursor.get();
    }

    /// Sets the cursor of the scene.
    public final void setCursor(Cursor value) {
        cursor.set(value);
    }

    /// The cursor of the scene.
    public final ObjectProperty<Cursor> cursorProperty() {
        return cursor;
    }

    /// Returns the style sheet locations of this scene. The list is
    /// passed to the installed style engine; without one it has no effect.
    public final ObservableList<String> getStylesheets() {
        return stylesheets;
    }

    /// Returns the first node a selector matches, or `null`; see
    /// `Node.lookup`.
    public Node lookup(String selector) {
        Parent r = getRoot();
        return r == null ? null : r.lookup(selector);
    }

    /// Returns the application's own object attached to this scene.
    public Object getUserData() {
        return userData;
    }

    /// Attaches an object of the application to this scene.
    public void setUserData(Object value) {
        userData = value;
    }

    /// Returns a map for the application's own values on this scene.
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

    // ---------------------------------------------------------- hosting

    /// Attaches the scene to what shows it, or detaches it with `null`.
    public final void cn1SetHost(SceneHost value, Window owner) {
        host = value;
        window.set(owner);
        shownCursor = -1;
    }

    /// Returns what shows this scene, or `null`.
    public final SceneHost cn1Host() {
        return host;
    }

    /// Returns the width asked for when the scene was created, or -1.
    public final double cn1InitialWidth() {
        return initialWidth;
    }

    /// Returns the height asked for when the scene was created, or -1.
    public final double cn1InitialHeight() {
        return initialHeight;
    }

    /// Asks for layout to run before the next frame.
    public final void cn1RequestPulse() {
        if (host != null) {
            host.requestPulse();
        }
    }

    /// Gives the scene a size and lays out the scene graph in it.
    public final void cn1Layout(double sceneWidth, double sceneHeight) {
        width.set(sceneWidth);
        height.set(sceneHeight);
        Parent r = getRoot();
        if (r == null) {
            return;
        }
        if (r.isResizable()) {
            r.resize(sceneWidth, sceneHeight);
        } else {
            r.autosize();
        }
        // A pass can change the bounds of a group, which the parent of the
        // group then has to place again.
        for (int pass = 0; pass < 4; pass++) {
            r.layout();
            if (!r.isNeedsLayout()) {
                break;
            }
        }
    }

    final void cn1NodeLeft(Node node) {
        if (focusOwner.get() == node) {
            node.cn1SetFocused(false);
            focusOwner.set(null);
        }
        if (hovered.remove(node)) {
            node.cn1SetHover(false);
        }
        if (pressedNodes.remove(node)) {
            node.cn1SetPressed(false);
        }
        if (pressTarget == node) {
            pressTarget = null;
        }
    }

    // ------------------------------------------------------------ focus

    /// Returns the node with the keyboard focus, or `null`.
    public final Node getFocusOwner() {
        return focusOwner.get();
    }

    /// The node with the keyboard focus.
    public final ReadOnlyObjectProperty<Node> focusOwnerProperty() {
        return focusOwner.getReadOnlyProperty();
    }

    /// Moves the focus to a node of this scene, or to nothing with
    /// `null`. With `moveNative` the node is also told to take the native
    /// focus; a control that already has it passes `false`.
    public final void cn1SetFocusOwner(Node node, boolean moveNative) {
        Node old = focusOwner.get();
        if (old == node) {
            return;
        }
        if (old != null) {
            old.cn1SetFocused(false);
        }
        focusOwner.set(node);
        if (node != null) {
            node.cn1SetFocused(true);
            if (moveNative) {
                node.cn1FocusRequested();
            }
        }
    }

    private void collectTraversable(Node n, ArrayList<Node> into) {
        if (!n.isVisible() || n.isDisabled()) {
            return;
        }
        if (n.isFocusTraversable()) {
            into.add(n);
        }
        if (n instanceof Parent) {
            ObservableList<Node> children = ((Parent) n).getChildrenUnmodifiable();
            for (int i = 0; i < children.size(); i++) {
                collectTraversable(children.get(i), into);
            }
        }
    }

    private void traverse(boolean backward) {
        ArrayList<Node> order = new ArrayList<Node>();
        if (getRoot() != null) {
            collectTraversable(getRoot(), order);
        }
        if (order.isEmpty()) {
            return;
        }
        int at = order.indexOf(focusOwner.get());
        int next;
        if (at < 0) {
            next = backward ? order.size() - 1 : 0;
        } else {
            next = (at + (backward ? order.size() - 1 : 1)) % order.size();
        }
        cn1SetFocusOwner(order.get(next), true);
    }

    // ------------------------------------------------------------ input

    private Node pick(double x, double y) {
        Parent r = getRoot();
        return r == null ? null : r.cn1Pick(x, y);
    }

    private static boolean deliverable(EventTarget target) {
        return !(target instanceof Node) || !((Node) target).isDisabled();
    }

    private MouseEvent mouse(EventType<? extends MouseEvent> type, EventTarget target, double x, double y,
            MouseButton button, int clicks, boolean down, boolean popup) {
        boolean primary = down && pressButton == MouseButton.PRIMARY;
        boolean middle = down && pressButton == MouseButton.MIDDLE;
        boolean secondary = down && pressButton == MouseButton.SECONDARY;
        return new MouseEvent(target, target, type, x, y, x, y, button, clicks, SceneInput.shiftDown(),
                SceneInput.controlDown(), SceneInput.altDown(), SceneInput.metaDown(), primary, middle, secondary,
                false, popup, still, new PickResult(target, x, y));
    }

    private void fire(EventTarget target, Event event) {
        if (target != null && deliverable(target)) {
            Event.fireEvent(target, event);
        }
    }

    private static void chain(Node from, ArrayList<Node> into) {
        for (Node n = from; n != null; n = n.getParent()) {
            into.add(n);
        }
    }

    private void updateHover(Node target, double x, double y) {
        ArrayList<Node> now = new ArrayList<Node>();
        chain(target, now);
        for (int i = 0; i < hovered.size(); i++) {
            Node n = hovered.get(i);
            if (!now.contains(n)) {
                n.cn1SetHover(false);
                fire(n, mouse(MouseEvent.MOUSE_EXITED_TARGET, n, x, y, MouseButton.NONE, 0,
                        pressTarget != null, false));
            }
        }
        for (int i = now.size() - 1; i >= 0; i--) {
            Node n = now.get(i);
            if (!hovered.contains(n)) {
                if (!n.isDisabled()) {
                    n.cn1SetHover(true);
                }
                fire(n, mouse(MouseEvent.MOUSE_ENTERED_TARGET, n, x, y, MouseButton.NONE, 0,
                        pressTarget != null, false));
            }
        }
        hovered.clear();
        hovered.addAll(now);
        Cursor wanted = null;
        for (int i = 0; i < now.size() && wanted == null; i++) {
            wanted = now.get(i).getCursor();
        }
        if (wanted == null) {
            wanted = getCursor();
        }
        int code = wanted == null ? Cursor.DEFAULT.cn1Code() : wanted.cn1Code();
        if (code != shownCursor && host != null) {
            shownCursor = code;
            host.showCursor(code);
        }
    }

    private void clearPressed() {
        for (int i = 0; i < pressedNodes.size(); i++) {
            pressedNodes.get(i).cn1SetPressed(false);
        }
        pressedNodes.clear();
    }

    private static EventTarget commonAncestor(EventTarget a, EventTarget b) {
        if (!(a instanceof Node) || !(b instanceof Node)) {
            return a == b ? a : null;
        }
        ArrayList<Node> up = new ArrayList<Node>();
        chain((Node) a, up);
        for (Node n = (Node) b; n != null; n = n.getParent()) {
            if (up.contains(n)) {
                return n;
            }
        }
        return null;
    }

    /// Feeds one pointer event into the scene, at scene coordinates. The
    /// kind is `MOUSE_PRESSED`, `MOUSE_DRAGGED`, `MOUSE_RELEASED` or
    /// `MOUSE_MOVED`. Answers whether a filter consumed what was fired.
    public final boolean cn1Pointer(EventType<MouseEvent> kind, double x, double y, MouseButton button) {
        EventHandlerManager.resetConsumedByFilter();
        Node under = pick(x, y);
        EventTarget target = under == null ? this : under;
        if (kind == MouseEvent.MOUSE_PRESSED) {
            updateHover(under, x, y);
            pressTarget = target;
            pressButton = button == null || button == MouseButton.NONE ? MouseButton.PRIMARY : button;
            pressX = x;
            pressY = y;
            still = true;
            dragDetected = false;
            if (under != null && !under.isDisabled()) {
                chain(under, pressedNodes);
                for (int i = 0; i < pressedNodes.size(); i++) {
                    pressedNodes.get(i).cn1SetPressed(true);
                }
                for (Node n = under; n != null; n = n.getParent()) {
                    if (n.isFocusTraversable()) {
                        n.requestFocus();
                        break;
                    }
                }
            }
            fire(target, mouse(MouseEvent.MOUSE_PRESSED, target, x, y, pressButton, nextClickCount(x, y), true,
                    false));
        } else if (kind == MouseEvent.MOUSE_DRAGGED) {
            if (pressTarget == null) {
                return false;
            }
            if (still && (Math.abs(x - pressX) > DRAG_THRESHOLD || Math.abs(y - pressY) > DRAG_THRESHOLD)) {
                still = false;
            }
            fire(pressTarget, mouse(MouseEvent.MOUSE_DRAGGED, pressTarget, x, y, pressButton, 0, true, false));
            if (!still && !dragDetected) {
                dragDetected = true;
                fire(pressTarget, mouse(MouseEvent.DRAG_DETECTED, pressTarget, x, y, pressButton, 0, true, false));
            }
        } else if (kind == MouseEvent.MOUSE_RELEASED) {
            if (pressTarget == null) {
                return false;
            }
            EventTarget pressedOn = pressTarget;
            MouseButton b = pressButton;
            boolean popup = b == MouseButton.SECONDARY;
            pressTarget = null;
            clearPressed();
            fire(pressedOn, mouse(MouseEvent.MOUSE_RELEASED, pressedOn, x, y, b, clickCount, false, popup));
            boolean filtered = EventHandlerManager.wasConsumedByFilter();
            EventTarget clicked = commonAncestor(pressedOn, target);
            if (clicked != null) {
                fire(clicked, mouse(MouseEvent.MOUSE_CLICKED, clicked, x, y, b, clickCount, false, false));
            }
            if (popup) {
                fire(target, new ContextMenuEvent(target, target, ContextMenuEvent.CONTEXT_MENU_REQUESTED, x, y, x,
                        y, false, new PickResult(target, x, y)));
            }
            pressButton = MouseButton.NONE;
            updateHover(under, x, y);
            return filtered;
        } else {
            updateHover(under, x, y);
            fire(target, mouse(MouseEvent.MOUSE_MOVED, target, x, y, MouseButton.NONE, 0, false, false));
        }
        return EventHandlerManager.wasConsumedByFilter();
    }

    private int nextClickCount(double x, double y) {
        long now = System.currentTimeMillis();
        if (now - lastClickTime <= MULTI_CLICK_MILLIS && Math.abs(x - lastClickX) <= DRAG_THRESHOLD
                && Math.abs(y - lastClickY) <= DRAG_THRESHOLD) {
            clickCount++;
        } else {
            clickCount = 1;
        }
        lastClickTime = now;
        lastClickX = x;
        lastClickY = y;
        return clickCount;
    }

    /// The pointer left the scene: nothing is hovered any more.
    public final void cn1PointerExited() {
        updateHover(null, -1, -1);
    }

    /// Asks for a context menu at scene coordinates, as a long press
    /// does. Answers whether a filter consumed the request.
    public final boolean cn1ContextMenu(double x, double y) {
        EventHandlerManager.resetConsumedByFilter();
        Node under = pick(x, y);
        EventTarget target = under == null ? this : under;
        fire(target, new ContextMenuEvent(target, target, ContextMenuEvent.CONTEXT_MENU_REQUESTED, x, y, x, y, false,
                new PickResult(target, x, y)));
        return EventHandlerManager.wasConsumedByFilter();
    }

    /// Feeds a turn of the wheel into the scene. Answers whether a filter
    /// consumed it.
    public final boolean cn1Wheel(double x, double y, double deltaX, double deltaY) {
        EventHandlerManager.resetConsumedByFilter();
        Node under = pick(x, y);
        EventTarget target = under == null ? this : under;
        fire(target, new ScrollEvent(target, target, ScrollEvent.SCROLL, x, y, x, y, SceneInput.shiftDown(),
                SceneInput.controlDown(), SceneInput.altDown(), SceneInput.metaDown(), false, false, deltaX, deltaY,
                deltaX, deltaY, ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, new PickResult(target, x, y)));
        return EventHandlerManager.wasConsumedByFilter();
    }

    /// Feeds a key event into the scene: `KEY_PRESSED`, `KEY_RELEASED` or
    /// `KEY_TYPED`. Answers whether a filter consumed it.
    public final boolean cn1Key(EventType<KeyEvent> kind, KeyCode code, String text) {
        EventHandlerManager.resetConsumedByFilter();
        Node owner = focusOwner.get();
        EventTarget target = owner == null ? this : owner;
        KeyEvent event = new KeyEvent(target, target, kind, text, text, code, SceneInput.shiftDown(),
                SceneInput.controlDown(), SceneInput.altDown(), SceneInput.metaDown());
        final boolean[] handled = new boolean[1];
        if (deliverable(target)) {
            handled[0] = cn1FireAndReport(target, event);
        }
        if (kind == KeyEvent.KEY_PRESSED && code == KeyCode.TAB && !handled[0]) {
            traverse(SceneInput.shiftDown());
        }
        return EventHandlerManager.wasConsumedByFilter();
    }

    /// Fires an event and answers whether anything consumed it.
    private static boolean cn1FireAndReport(EventTarget target, Event event) {
        EventDispatchChain chain = target.buildEventDispatchChain(
                new com.codename1.fxcompat.runtime.EventDispatchChainImpl());
        return chain.dispatchEvent(event) == null;
    }

    // ----------------------------------------------------------- events

    /// Adds a handler called in the bubbling phase.
    public final <T extends Event> void addEventHandler(EventType<T> eventType,
            EventHandler<? super T> eventHandler) {
        events.addEventHandler(eventType, eventHandler);
    }

    /// Removes a handler.
    public final <T extends Event> void removeEventHandler(EventType<T> eventType,
            EventHandler<? super T> eventHandler) {
        events.removeEventHandler(eventType, eventHandler);
    }

    /// Adds a filter called in the capturing phase.
    public final <T extends Event> void addEventFilter(EventType<T> eventType,
            EventHandler<? super T> eventFilter) {
        events.addEventFilter(eventType, eventFilter);
    }

    /// Removes a filter.
    public final <T extends Event> void removeEventFilter(EventType<T> eventType,
            EventHandler<? super T> eventFilter) {
        events.removeEventFilter(eventType, eventFilter);
    }

    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        EventDispatchChain chain = tail.prepend(events);
        Window w = getWindow();
        return w == null ? chain : w.buildEventDispatchChain(chain);
    }

    /// Sets the handler of a click anywhere in the scene.
    public final void setOnMouseClicked(EventHandler<? super MouseEvent> value) {
        events.setSlot(MouseEvent.MOUSE_CLICKED, "onMouseClicked", value);
    }

    /// Returns the handler of a click anywhere in the scene.
    public final EventHandler<? super MouseEvent> getOnMouseClicked() {
        return events.getSlot(MouseEvent.MOUSE_CLICKED);
    }

    /// Sets the handler of a button going down.
    public final void setOnMousePressed(EventHandler<? super MouseEvent> value) {
        events.setSlot(MouseEvent.MOUSE_PRESSED, "onMousePressed", value);
    }

    /// Returns the handler of a button going down.
    public final EventHandler<? super MouseEvent> getOnMousePressed() {
        return events.getSlot(MouseEvent.MOUSE_PRESSED);
    }

    /// Sets the handler of a button coming up.
    public final void setOnMouseReleased(EventHandler<? super MouseEvent> value) {
        events.setSlot(MouseEvent.MOUSE_RELEASED, "onMouseReleased", value);
    }

    /// Returns the handler of a button coming up.
    public final EventHandler<? super MouseEvent> getOnMouseReleased() {
        return events.getSlot(MouseEvent.MOUSE_RELEASED);
    }

    /// Sets the handler of the pointer moving with a button down.
    public final void setOnMouseDragged(EventHandler<? super MouseEvent> value) {
        events.setSlot(MouseEvent.MOUSE_DRAGGED, "onMouseDragged", value);
    }

    /// Returns the handler of the pointer moving with a button down.
    public final EventHandler<? super MouseEvent> getOnMouseDragged() {
        return events.getSlot(MouseEvent.MOUSE_DRAGGED);
    }

    /// Sets the handler of the pointer moving with no button down.
    public final void setOnMouseMoved(EventHandler<? super MouseEvent> value) {
        events.setSlot(MouseEvent.MOUSE_MOVED, "onMouseMoved", value);
    }

    /// Returns the handler of the pointer moving with no button down.
    public final EventHandler<? super MouseEvent> getOnMouseMoved() {
        return events.getSlot(MouseEvent.MOUSE_MOVED);
    }

    /// Sets the handler of a key going down.
    public final void setOnKeyPressed(EventHandler<? super KeyEvent> value) {
        events.setSlot(KeyEvent.KEY_PRESSED, "onKeyPressed", value);
    }

    /// Returns the handler of a key going down.
    public final EventHandler<? super KeyEvent> getOnKeyPressed() {
        return events.getSlot(KeyEvent.KEY_PRESSED);
    }

    /// Sets the handler of a key coming up.
    public final void setOnKeyReleased(EventHandler<? super KeyEvent> value) {
        events.setSlot(KeyEvent.KEY_RELEASED, "onKeyReleased", value);
    }

    /// Returns the handler of a key coming up.
    public final EventHandler<? super KeyEvent> getOnKeyReleased() {
        return events.getSlot(KeyEvent.KEY_RELEASED);
    }

    /// Sets the handler of a character being typed.
    public final void setOnKeyTyped(EventHandler<? super KeyEvent> value) {
        events.setSlot(KeyEvent.KEY_TYPED, "onKeyTyped", value);
    }

    /// Returns the handler of a character being typed.
    public final EventHandler<? super KeyEvent> getOnKeyTyped() {
        return events.getSlot(KeyEvent.KEY_TYPED);
    }

    /// Sets the handler of the wheel scrolling.
    public final void setOnScroll(EventHandler<? super ScrollEvent> value) {
        events.setSlot(ScrollEvent.SCROLL, "onScroll", value);
    }

    /// Returns the handler of the wheel scrolling.
    public final EventHandler<? super ScrollEvent> getOnScroll() {
        return events.getSlot(ScrollEvent.SCROLL);
    }

    /// Sets the handler of a context menu being asked for.
    public final void setOnContextMenuRequested(EventHandler<? super ContextMenuEvent> value) {
        events.setSlot(ContextMenuEvent.CONTEXT_MENU_REQUESTED, "onContextMenuRequested", value);
    }

    /// Returns the handler of a context menu being asked for.
    public final EventHandler<? super ContextMenuEvent> getOnContextMenuRequested() {
        return events.getSlot(ContextMenuEvent.CONTEXT_MENU_REQUESTED);
    }
}
