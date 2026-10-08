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
package com.codename1.desktopcompat.java.awt;

import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.java.beans.PropertyChangeSupport;
import com.codename1.desktopcompat.rt.EventBridge;
import com.codename1.desktopcompat.rt.WindowHosts;

/// Answers who owns the keyboard focus and moves it along the traversal
/// order.
///
/// A minimal manager: the focus owner, the focused and active window (the
/// same window), next and previous traversal, the default traversal
/// policy, and the `"focusOwner"` property change. Key event dispatchers
/// and post processors, vetoable changes and focus cycle roots are absent.
public abstract class KeyboardFocusManager {

    public static final int FORWARD_TRAVERSAL_KEYS = 0;
    public static final int BACKWARD_TRAVERSAL_KEYS = 1;
    public static final int UP_CYCLE_TRAVERSAL_KEYS = 2;
    public static final int DOWN_CYCLE_TRAVERSAL_KEYS = 3;

    private static KeyboardFocusManager current;

    private FocusTraversalPolicy defaultPolicy = new DefaultFocusTraversalPolicy();
    private PropertyChangeSupport changeSupport;

    public KeyboardFocusManager() {
    }

    public static KeyboardFocusManager getCurrentKeyboardFocusManager() {
        if (current == null) {
            current = new DefaultKeyboardFocusManager();
        }
        return current;
    }

    /// Tells the manager the focus moved, so that it can tell its
    /// listeners. Called by the event bridge.
    public static void cn1FocusChanged(Component oldOwner, Component newOwner) {
        if (current != null && current.changeSupport != null) {
            current.changeSupport.firePropertyChange("focusOwner", oldOwner, newOwner);
            current.changeSupport.firePropertyChange("permanentFocusOwner", oldOwner, newOwner);
        }
    }

    public Component getFocusOwner() {
        return EventBridge.focusOwner();
    }

    public Component getPermanentFocusOwner() {
        return EventBridge.focusOwner();
    }

    public void clearGlobalFocusOwner() {
        EventBridge.setFocusOwner(null, false);
    }

    public Window getFocusedWindow() {
        return WindowHosts.active();
    }

    public Window getActiveWindow() {
        return WindowHosts.active();
    }

    public FocusTraversalPolicy getDefaultFocusTraversalPolicy() {
        return defaultPolicy;
    }

    public void setDefaultFocusTraversalPolicy(FocusTraversalPolicy defaultPolicy) {
        if (defaultPolicy == null) {
            throw new IllegalArgumentException("default focus traversal policy cannot be null");
        }
        this.defaultPolicy = defaultPolicy;
    }

    public abstract void focusNextComponent(Component aComponent);

    public abstract void focusPreviousComponent(Component aComponent);

    /// Moves the focus to the component after the focus owner, or to the
    /// first component of the active window when nothing owns the focus.
    public final void focusNextComponent() {
        Component c = getFocusOwner();
        focusNextComponent(c != null ? c : getActiveWindow());
    }

    public final void focusPreviousComponent() {
        Component c = getFocusOwner();
        focusPreviousComponent(c != null ? c : getActiveWindow());
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener != null) {
            if (changeSupport == null) {
                changeSupport = new PropertyChangeSupport(this);
            }
            changeSupport.addPropertyChangeListener(listener);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport != null) {
            changeSupport.removePropertyChangeListener(listener);
        }
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (listener != null) {
            if (changeSupport == null) {
                changeSupport = new PropertyChangeSupport(this);
            }
            changeSupport.addPropertyChangeListener(propertyName, listener);
        }
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (changeSupport != null) {
            changeSupport.removePropertyChangeListener(propertyName, listener);
        }
    }
}
