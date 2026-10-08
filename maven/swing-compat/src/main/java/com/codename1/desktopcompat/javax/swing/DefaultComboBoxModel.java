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

import java.util.Vector;

/// The model a combo box has unless it is given another.
///
/// The first element added to an empty model becomes the selection, and
/// removing the selected element selects a neighbour.
public class DefaultComboBoxModel<E> extends AbstractListModel<E> implements MutableComboBoxModel<E> {

    private final Vector<E> objects;
    private Object selectedObject;

    public DefaultComboBoxModel() {
        objects = new Vector<E>();
    }

    public DefaultComboBoxModel(E[] items) {
        objects = new Vector<E>(items.length);
        for (int i = 0; i < items.length; i++) {
            objects.addElement(items[i]);
        }
        if (getSize() > 0) {
            selectedObject = getElementAt(0);
        }
    }

    /// Uses `v` itself, not a copy of it, as the desktop does.
    public DefaultComboBoxModel(Vector<E> v) {
        objects = v;
        if (getSize() > 0) {
            selectedObject = getElementAt(0);
        }
    }

    @Override
    public void setSelectedItem(Object anObject) {
        if ((selectedObject != null && !selectedObject.equals(anObject))
                || (selectedObject == null && anObject != null)) {
            selectedObject = anObject;
            fireContentsChanged(this, -1, -1);
        }
    }

    @Override
    public Object getSelectedItem() {
        return selectedObject;
    }

    @Override
    public int getSize() {
        return objects.size();
    }

    @Override
    public E getElementAt(int index) {
        if (index >= 0 && index < objects.size()) {
            return objects.elementAt(index);
        }
        return null;
    }

    public int getIndexOf(Object anObject) {
        return objects.indexOf(anObject);
    }

    @Override
    public void addElement(E anObject) {
        objects.addElement(anObject);
        fireIntervalAdded(this, objects.size() - 1, objects.size() - 1);
        if (objects.size() == 1 && selectedObject == null && anObject != null) {
            setSelectedItem(anObject);
        }
    }

    @Override
    public void insertElementAt(E anObject, int index) {
        objects.insertElementAt(anObject, index);
        fireIntervalAdded(this, index, index);
    }

    @Override
    public void removeElementAt(int index) {
        // Identity on purpose: the element removed is the selection itself,
        // not merely one equal to it.
        Object removed = getElementAt(index);
        if (removed == selectedObject) {
            if (index == 0) {
                setSelectedItem(getSize() == 1 ? null : getElementAt(index + 1));
            } else {
                setSelectedItem(getElementAt(index - 1));
            }
        }
        objects.removeElementAt(index);
        fireIntervalRemoved(this, index, index);
    }

    @Override
    public void removeElement(Object anObject) {
        int index = objects.indexOf(anObject);
        if (index != -1) {
            removeElementAt(index);
        }
    }

    public void removeAllElements() {
        if (objects.size() > 0) {
            int lastIndex = objects.size() - 1;
            objects.removeAllElements();
            selectedObject = null;
            fireIntervalRemoved(this, 0, lastIndex);
        } else {
            selectedObject = null;
        }
    }
}
