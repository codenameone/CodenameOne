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
package com.codename1.desktopcompat.javax.accessibility;

import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.java.beans.PropertyChangeSupport;

/// What a component tells assistive technology about itself: a name, a
/// description, a parent and its relations to other components.
public abstract class AccessibleContext {

    public static final String ACCESSIBLE_NAME_PROPERTY = "AccessibleName";

    public static final String ACCESSIBLE_DESCRIPTION_PROPERTY = "AccessibleDescription";

    protected Accessible accessibleParent;

    protected String accessibleName;

    protected String accessibleDescription;

    private PropertyChangeSupport changes;

    private final AccessibleRelationSet relations = new AccessibleRelationSet();

    public String getAccessibleName() {
        return accessibleName;
    }

    public void setAccessibleName(String s) {
        String old = accessibleName;
        accessibleName = s;
        firePropertyChange(ACCESSIBLE_NAME_PROPERTY, old, s);
    }

    public String getAccessibleDescription() {
        return accessibleDescription;
    }

    public void setAccessibleDescription(String s) {
        String old = accessibleDescription;
        accessibleDescription = s;
        firePropertyChange(ACCESSIBLE_DESCRIPTION_PROPERTY, old, s);
    }

    public Accessible getAccessibleParent() {
        return accessibleParent;
    }

    public void setAccessibleParent(Accessible a) {
        accessibleParent = a;
    }

    public AccessibleRelationSet getAccessibleRelationSet() {
        return relations;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changes == null) {
            changes = new PropertyChangeSupport(this);
        }
        changes.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changes != null) {
            changes.removePropertyChangeListener(listener);
        }
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (changes != null) {
            changes.firePropertyChange(propertyName, oldValue, newValue);
        }
    }
}
