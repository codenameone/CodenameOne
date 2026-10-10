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
package com.codename1.desktopcompat.org.jdesktop.beans;

import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.java.beans.PropertyChangeSupport;
import com.codename1.desktopcompat.java.beans.PropertyVetoException;
import com.codename1.desktopcompat.java.beans.VetoableChangeListener;
import java.util.ArrayList;

/// A base class that carries the property change and vetoable change
/// listener lists of a bean.
///
/// `clone()` is absent: a device cannot copy an object it knows nothing
/// about. Vetoable listeners registered for one property name are told of
/// changes to that property only, as in the JDK.
public abstract class AbstractBean {

    private final PropertyChangeSupport pcs;
    private final ArrayList<Object[]> vetoers = new ArrayList<Object[]>();

    protected AbstractBean() {
        pcs = new PropertyChangeSupport(this);
    }

    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public final PropertyChangeListener[] getPropertyChangeListeners() {
        return pcs.getPropertyChangeListeners();
    }

    public final void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public final void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    public final PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return pcs.getPropertyChangeListeners(propertyName);
    }

    protected final void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }

    protected final void firePropertyChange(PropertyChangeEvent evt) {
        pcs.firePropertyChange(evt);
    }

    protected final void fireIndexedPropertyChange(String propertyName, int index, Object oldValue,
            Object newValue) {
        pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
    }

    protected final boolean hasPropertyChangeListeners(String propertyName) {
        return pcs.hasListeners(propertyName);
    }

    protected final boolean hasVetoableChangeListeners(String propertyName) {
        for (int i = 0; i < vetoers.size(); i++) {
            Object name = vetoers.get(i)[0];
            if (name == null || name.equals(propertyName)) {
                return true;
            }
        }
        return false;
    }

    public final void addVetoableChangeListener(VetoableChangeListener listener) {
        addVetoableChangeListener(null, listener);
    }

    public final void removeVetoableChangeListener(VetoableChangeListener listener) {
        removeVetoableChangeListener(null, listener);
    }

    public final VetoableChangeListener[] getVetoableChangeListeners() {
        VetoableChangeListener[] out = new VetoableChangeListener[vetoers.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = (VetoableChangeListener) vetoers.get(i)[1];
        }
        return out;
    }

    public final void addVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
        if (listener != null) {
            vetoers.add(new Object[]{propertyName, listener});
        }
    }

    public final void removeVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
        for (int i = 0; i < vetoers.size(); i++) {
            Object[] v = vetoers.get(i);
            boolean sameName = propertyName == null ? v[0] == null : propertyName.equals(v[0]);
            if (sameName && v[1] == listener) {
                vetoers.remove(i);
                return;
            }
        }
    }

    public final VetoableChangeListener[] getVetoableChangeListeners(String propertyName) {
        ArrayList<VetoableChangeListener> out = new ArrayList<VetoableChangeListener>();
        for (int i = 0; i < vetoers.size(); i++) {
            Object[] v = vetoers.get(i);
            if (propertyName != null && propertyName.equals(v[0])) {
                out.add((VetoableChangeListener) v[1]);
            }
        }
        return out.toArray(new VetoableChangeListener[out.size()]);
    }

    protected final void fireVetoableChange(String propertyName, Object oldValue, Object newValue)
            throws PropertyVetoException {
        fireVetoableChange(new PropertyChangeEvent(this, propertyName, oldValue, newValue));
    }

    /// Tells every vetoable listener of the property. When one vetoes, the
    /// listeners already told are told of the change back, and the veto is
    /// thrown on.
    protected final void fireVetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        Object oldValue = evt.getOldValue();
        Object newValue = evt.getNewValue();
        if (oldValue != null && oldValue.equals(newValue)) {
            return;
        }
        Object[][] all = vetoers.toArray(new Object[vetoers.size()][]);
        String name = evt.getPropertyName();
        int told = 0;
        try {
            for (; told < all.length; told++) {
                if (all[told][0] == null || all[told][0].equals(name)) {
                    ((VetoableChangeListener) all[told][1]).vetoableChange(evt);
                }
            }
        } catch (PropertyVetoException veto) {
            PropertyChangeEvent back = new PropertyChangeEvent(this, name, newValue, oldValue);
            for (int i = 0; i < told; i++) {
                if (all[i][0] == null || all[i][0].equals(name)) {
                    try {
                        ((VetoableChangeListener) all[i][1]).vetoableChange(back);
                    } catch (PropertyVetoException ignored) {
                        // A veto of the roll back changes nothing.
                        continue;
                    }
                }
            }
            throw veto;
        }
    }
}
