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
package com.codename1.desktopcompat.java.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/// Keeps the listeners of a bean's bound properties and delivers events to
/// them.
///
/// Listeners added with a property name receive only that property's events;
/// the others receive all. A `PropertyChangeListenerProxy` passed to
/// `addPropertyChangeListener(PropertyChangeListener)` is registered under
/// the name it carries. No event is fired when the old and new values are
/// both non-null and equal. The user interface is single threaded, so unlike
/// the JDK class this does not synchronize.
public class PropertyChangeSupport {

    private final Object source;

    private final ArrayList<PropertyChangeListener> common = new ArrayList<PropertyChangeListener>();

    private final HashMap<String, ArrayList<PropertyChangeListener>> named =
            new HashMap<String, ArrayList<PropertyChangeListener>>();

    public PropertyChangeSupport(Object sourceBean) {
        if (sourceBean == null) {
            throw new NullPointerException();
        }
        source = sourceBean;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (listener instanceof PropertyChangeListenerProxy) {
            PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) listener;
            Object wrapped = proxy.getListener();
            if (wrapped instanceof PropertyChangeListener) {
                addPropertyChangeListener(proxy.getPropertyName(), (PropertyChangeListener) wrapped);
            }
        } else {
            common.add(listener);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (listener instanceof PropertyChangeListenerProxy) {
            PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) listener;
            Object wrapped = proxy.getListener();
            if (wrapped instanceof PropertyChangeListener) {
                removePropertyChangeListener(proxy.getPropertyName(), (PropertyChangeListener) wrapped);
            }
        } else {
            common.remove(listener);
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        ArrayList<PropertyChangeListener> all = new ArrayList<PropertyChangeListener>(common);
        for (Map.Entry<String, ArrayList<PropertyChangeListener>> e : named.entrySet()) {
            for (PropertyChangeListener l : e.getValue()) {
                all.add(new PropertyChangeListenerProxy(e.getKey(), l));
            }
        }
        return all.toArray(new PropertyChangeListener[all.size()]);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (listener == null || propertyName == null) {
            return;
        }
        if (listener instanceof PropertyChangeListenerProxy) {
            addPropertyChangeListener(listener);
            return;
        }
        ArrayList<PropertyChangeListener> list = named.get(propertyName);
        if (list == null) {
            list = new ArrayList<PropertyChangeListener>();
            named.put(propertyName, list);
        }
        list.add(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (listener == null || propertyName == null) {
            return;
        }
        if (listener instanceof PropertyChangeListenerProxy) {
            removePropertyChangeListener(listener);
            return;
        }
        ArrayList<PropertyChangeListener> list = named.get(propertyName);
        if (list != null) {
            list.remove(listener);
            if (list.isEmpty()) {
                named.remove(propertyName);
            }
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        ArrayList<PropertyChangeListener> list = propertyName == null ? null : named.get(propertyName);
        if (list == null) {
            return new PropertyChangeListener[0];
        }
        return list.toArray(new PropertyChangeListener[list.size()]);
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (oldValue == null || newValue == null || !oldValue.equals(newValue)) {
            firePropertyChange(new PropertyChangeEvent(source, propertyName, oldValue, newValue));
        }
    }

    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
        if (oldValue != newValue) {
            firePropertyChange(propertyName, Integer.valueOf(oldValue), Integer.valueOf(newValue));
        }
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            firePropertyChange(propertyName, Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
        }
    }

    public void firePropertyChange(PropertyChangeEvent event) {
        Object oldValue = event.getOldValue();
        Object newValue = event.getNewValue();
        if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
            return;
        }
        String name = event.getPropertyName();
        PropertyChangeListener[] first = common.toArray(new PropertyChangeListener[common.size()]);
        for (int i = 0; i < first.length; i++) {
            first[i].propertyChange(event);
        }
        if (name != null) {
            PropertyChangeListener[] second = getPropertyChangeListeners(name);
            for (int i = 0; i < second.length; i++) {
                second[i].propertyChange(event);
            }
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, Object oldValue, Object newValue) {
        if (oldValue == null || newValue == null || !oldValue.equals(newValue)) {
            firePropertyChange(new IndexedPropertyChangeEvent(source, propertyName, oldValue, newValue, index));
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, int oldValue, int newValue) {
        if (oldValue != newValue) {
            fireIndexedPropertyChange(propertyName, index, Integer.valueOf(oldValue), Integer.valueOf(newValue));
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, boolean oldValue,
            boolean newValue) {
        if (oldValue != newValue) {
            fireIndexedPropertyChange(propertyName, index, Boolean.valueOf(oldValue),
                    Boolean.valueOf(newValue));
        }
    }

    public boolean hasListeners(String propertyName) {
        if (!common.isEmpty()) {
            return true;
        }
        if (propertyName == null) {
            return false;
        }
        ArrayList<PropertyChangeListener> list = named.get(propertyName);
        return list != null && !list.isEmpty();
    }
}
