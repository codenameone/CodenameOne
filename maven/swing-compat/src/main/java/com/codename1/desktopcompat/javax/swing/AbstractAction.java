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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.event.SwingPropertyChangeSupport;

import java.util.HashMap;

/// A base for `Action` implementations that keeps the property map, the
/// enabled state and the property change listeners.
///
/// Changing a property or the enabled state fires a property change event
/// named after the key (`"enabled"` for the enabled state). `clone()` of the
/// JDK class is not provided, because cloning does not work on the device.
public abstract class AbstractAction implements Action {

    /// Whether the action is enabled.
    protected boolean enabled = true;

    /// The listeners of the action, created on demand.
    protected SwingPropertyChangeSupport changeSupport;

    private final HashMap<String, Object> values = new HashMap<String, Object>();

    public AbstractAction() {
    }

    public AbstractAction(String name) {
        putValue(Action.NAME, name);
    }

    public AbstractAction(String name, Icon icon) {
        this(name);
        putValue(Action.SMALL_ICON, icon);
    }

    public Object getValue(String key) {
        return values.get(key);
    }

    public void putValue(String key, Object newValue) {
        Object oldValue = values.get(key);
        if (newValue == null) {
            values.remove(key);
        } else {
            values.put(key, newValue);
        }
        firePropertyChange(key, oldValue, newValue);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean newValue) {
        boolean oldValue = enabled;
        if (oldValue != newValue) {
            enabled = newValue;
            firePropertyChange("enabled", Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
        }
    }

    /// The keys with a value, or `null` when there are none.
    public Object[] getKeys() {
        if (values.isEmpty()) {
            return null;
        }
        return values.keySet().toArray();
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (changeSupport == null || oldValue != null && newValue != null && oldValue.equals(newValue)) {
            return;
        }
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new SwingPropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            return;
        }
        changeSupport.removePropertyChangeListener(listener);
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        if (changeSupport == null) {
            return new PropertyChangeListener[0];
        }
        return changeSupport.getPropertyChangeListeners();
    }
}
