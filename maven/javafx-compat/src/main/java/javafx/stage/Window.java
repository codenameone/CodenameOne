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

import com.codename1.fxcompat.runtime.EventHandlerManager;
import com.codename1.fxcompat.runtime.StageHost;
import com.codename1.fxcompat.runtime.StageHosts;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Scene;

/// A top level window that shows a scene.
///
/// What a window is on screen depends on the device; see
/// `com.codename1.fxcompat.runtime.StageHosts`. Position and size are in
/// logical pixels. They are requests: a window shown as a form always
/// fills the screen, and its `x` and `y` stay 0.
public class Window implements EventTarget {

    private static final ObservableList<Window> WINDOWS = FXCollections.observableArrayList();
    private static final ObservableList<Window> WINDOWS_VIEW = FXCollections.unmodifiableObservableList(WINDOWS);

    private final ReadOnlyObjectWrapper<Scene> scene = new ReadOnlyObjectWrapper<Scene>(this, "scene");
    private final ReadOnlyBooleanWrapper showing = new ReadOnlyBooleanWrapper(this, "showing", false);
    private final ReadOnlyDoubleWrapper x = new ReadOnlyDoubleWrapper(this, "x", Double.NaN);
    private final ReadOnlyDoubleWrapper y = new ReadOnlyDoubleWrapper(this, "y", Double.NaN);
    private final ReadOnlyDoubleWrapper width = new ReadOnlyDoubleWrapper(this, "width", Double.NaN);
    private final ReadOnlyDoubleWrapper height = new ReadOnlyDoubleWrapper(this, "height", Double.NaN);
    private final EventHandlerManager events = new EventHandlerManager(this);
    private ObservableMap<Object, Object> properties;
    private Object userData;
    private StageHost host;

    /// Creates a window.
    protected Window() {
    }

    /// Returns the windows that are showing.
    public static ObservableList<Window> getWindows() {
        return WINDOWS_VIEW;
    }

    // ------------------------------------------------------------ scene

    /// Returns the scene shown in this window.
    public final Scene getScene() {
        return scene.get();
    }

    /// The scene shown in this window.
    public final ReadOnlyObjectProperty<Scene> sceneProperty() {
        return scene.getReadOnlyProperty();
    }

    /// Sets the scene shown in this window.
    protected void setScene(Scene value) {
        Scene old = scene.get();
        if (old == value) {
            return;
        }
        if (value != null && value.getWindow() != null && value.getWindow() != this) {
            Window other = value.getWindow();
            other.setScene(null);
        }
        scene.set(value);
        if (host != null) {
            if (old != null) {
                old.cn1SetHost(null, null);
            }
            host.sceneChanged();
        }
    }

    // --------------------------------------------------------- geometry

    /// Returns the x of the window on the screen.
    public final double getX() {
        return x.get();
    }

    /// Asks for an x on the screen.
    public final void setX(double value) {
        x.set(value);
        if (host != null) {
            host.boundsRequested();
        }
    }

    /// The x of the window on the screen.
    public final ReadOnlyDoubleProperty xProperty() {
        return x.getReadOnlyProperty();
    }

    /// Returns the y of the window on the screen.
    public final double getY() {
        return y.get();
    }

    /// Asks for a y on the screen.
    public final void setY(double value) {
        y.set(value);
        if (host != null) {
            host.boundsRequested();
        }
    }

    /// The y of the window on the screen.
    public final ReadOnlyDoubleProperty yProperty() {
        return y.getReadOnlyProperty();
    }

    /// Returns the width of the window.
    public final double getWidth() {
        return width.get();
    }

    /// Asks for a width.
    public final void setWidth(double value) {
        width.set(value);
        if (host != null) {
            host.boundsRequested();
        }
    }

    /// The width of the window.
    public final ReadOnlyDoubleProperty widthProperty() {
        return width.getReadOnlyProperty();
    }

    /// Returns the height of the window.
    public final double getHeight() {
        return height.get();
    }

    /// Asks for a height.
    public final void setHeight(double value) {
        height.set(value);
        if (host != null) {
            host.boundsRequested();
        }
    }

    /// The height of the window.
    public final ReadOnlyDoubleProperty heightProperty() {
        return height.getReadOnlyProperty();
    }

    /// Sizes the window to the preferred size of its scene, where a
    /// window can be sized.
    public void sizeToScene() {
        width.set(Double.NaN);
        height.set(Double.NaN);
        if (host != null) {
            host.boundsRequested();
        }
    }

    /// Centres the window on the screen, where a window can be moved.
    public void centerOnScreen() {
        x.set(Double.NaN);
        y.set(Double.NaN);
        if (host != null) {
            host.boundsRequested();
        }
    }

    /// Records the bounds the window really has; called by its host.
    public final void cn1Bounds(double newX, double newY, double newWidth, double newHeight) {
        x.set(newX);
        y.set(newY);
        width.set(newWidth);
        height.set(newHeight);
    }

    // ---------------------------------------------------------- showing

    /// Returns whether the window is showing.
    public final boolean isShowing() {
        return showing.get();
    }

    /// Whether the window is showing.
    public final ReadOnlyBooleanProperty showingProperty() {
        return showing.getReadOnlyProperty();
    }

    /// Returns what shows this window, or `null` while it is hidden.
    public final StageHost cn1Host() {
        return host;
    }

    /// Shows the window.
    protected void show() {
        cn1Show(false);
    }

    /// Shows the window, and with `block` returns only once it is hidden
    /// again while events keep being handled.
    protected final void cn1Show(boolean block) {
        if (showing.get()) {
            return;
        }
        fireEvent(new WindowEvent(this, WindowEvent.WINDOW_SHOWING));
        host = StageHosts.create(this);
        showing.set(true);
        WINDOWS.add(this);
        host.sceneChanged();
        host.open();
        fireEvent(new WindowEvent(this, WindowEvent.WINDOW_SHOWN));
        if (block && host != null) {
            host.block();
        }
    }

    /// Hides the window.
    public void hide() {
        if (!showing.get()) {
            return;
        }
        fireEvent(new WindowEvent(this, WindowEvent.WINDOW_HIDING));
        StageHost closing = host;
        host = null;
        showing.set(false);
        WINDOWS.remove(this);
        Scene s = getScene();
        if (s != null) {
            s.cn1SetHost(null, null);
        }
        if (closing != null) {
            closing.close();
        }
        fireEvent(new WindowEvent(this, WindowEvent.WINDOW_HIDDEN));
        if (WINDOWS.isEmpty()) {
            StageHosts.lastWindowHidden();
        }
    }

    /// The user asked to close the window, through the platform's close
    /// control or the back command: fires `WINDOW_CLOSE_REQUEST` and
    /// hides the window unless a handler consumed it. Answers whether the
    /// window was hidden.
    public final boolean cn1CloseRequested() {
        WindowEvent request = new WindowEvent(this, WindowEvent.WINDOW_CLOSE_REQUEST);
        fireEvent(request);
        if (request.isConsumed()) {
            return false;
        }
        hide();
        return true;
    }

    /// Asks for the input focus.
    public final void requestFocus() {
        if (host != null) {
            host.toFront();
        }
    }

    /// Returns the application's own object attached to this window.
    public Object getUserData() {
        return userData;
    }

    /// Attaches an object of the application to this window.
    public void setUserData(Object value) {
        userData = value;
    }

    /// Returns a map for the application's own values on this window.
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

    /// Delivers an event to this window.
    public final void fireEvent(Event event) {
        Event.fireEvent(this, event);
    }

    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        return tail.prepend(events);
    }

    /// Sets the handler of the user asking to close the window.
    public final void setOnCloseRequest(EventHandler<WindowEvent> value) {
        events.setSlot(WindowEvent.WINDOW_CLOSE_REQUEST, "onCloseRequest", value);
    }

    /// Returns the handler of the user asking to close the window.
    public final EventHandler<? super WindowEvent> getOnCloseRequest() {
        return events.getSlot(WindowEvent.WINDOW_CLOSE_REQUEST);
    }

    /// The handler of the user asking to close the window.
    public final ObjectProperty<EventHandler<? super WindowEvent>> onCloseRequestProperty() {
        return events.slot(WindowEvent.WINDOW_CLOSE_REQUEST, "onCloseRequest");
    }

    /// Sets the handler called before the window is shown.
    public final void setOnShowing(EventHandler<WindowEvent> value) {
        events.setSlot(WindowEvent.WINDOW_SHOWING, "onShowing", value);
    }

    /// Returns the handler called before the window is shown.
    public final EventHandler<? super WindowEvent> getOnShowing() {
        return events.getSlot(WindowEvent.WINDOW_SHOWING);
    }

    /// Sets the handler called after the window was shown.
    public final void setOnShown(EventHandler<WindowEvent> value) {
        events.setSlot(WindowEvent.WINDOW_SHOWN, "onShown", value);
    }

    /// Returns the handler called after the window was shown.
    public final EventHandler<? super WindowEvent> getOnShown() {
        return events.getSlot(WindowEvent.WINDOW_SHOWN);
    }

    /// Sets the handler called before the window is hidden.
    public final void setOnHiding(EventHandler<WindowEvent> value) {
        events.setSlot(WindowEvent.WINDOW_HIDING, "onHiding", value);
    }

    /// Returns the handler called before the window is hidden.
    public final EventHandler<? super WindowEvent> getOnHiding() {
        return events.getSlot(WindowEvent.WINDOW_HIDING);
    }

    /// Sets the handler called after the window was hidden.
    public final void setOnHidden(EventHandler<WindowEvent> value) {
        events.setSlot(WindowEvent.WINDOW_HIDDEN, "onHidden", value);
    }

    /// Returns the handler called after the window was hidden.
    public final EventHandler<? super WindowEvent> getOnHidden() {
        return events.getSlot(WindowEvent.WINDOW_HIDDEN);
    }
}
