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
package com.codename1.desktopcompat.javax.swing.event;

import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.AdjustmentListener;
import com.codename1.desktopcompat.java.awt.event.ItemListener;

import java.lang.reflect.Array;
import java.util.EventListener;

/// A list of listeners of mixed types, stored as alternating class and
/// listener entries.
///
/// The array behind `getListenerList()` is handed out as it is, as in the
/// JDK, so that firing code can walk it without copying; an add or remove
/// replaces it rather than editing it in place. The JDK class synchronizes
/// and serializes; this one does neither.
public class EventListenerList {

    private static final Object[] NULL_ARRAY = new Object[0];

    /// The array class of every listener type this layer itself passes to
    /// `getListeners`, named so that it exists on a device.
    ///
    /// ParparVM has an array class only for a type some bytecode creates an
    /// array of -- an `anewarray`, or a class literal like the ones below. A
    /// method that merely *returns* `DocumentListener[]`, or casts to it, does
    /// not count, so `Array.newInstance(DocumentListener.class, n)` threw
    /// "the component class has no registered array class" and a native build
    /// of any application with a text field opened on an error dialog. The
    /// simulator and Android create array classes on demand and never showed
    /// it.
    ///
    /// A type an application defines and passes here itself is covered only if
    /// the application creates such an array somewhere, which the usual
    /// `new FooListener[0]` or `toArray` idiom does.
    private static final Class<?>[] LISTENER_ARRAYS = {
        ActionListener[].class, AdjustmentListener[].class, ItemListener[].class,
        CaretListener[].class, CellEditorListener[].class, ChangeListener[].class,
        DocumentListener[].class, HyperlinkListener[].class, InternalFrameListener[].class,
        ListDataListener[].class, ListSelectionListener[].class, MenuListener[].class,
        PopupMenuListener[].class, RowSorterListener[].class, TableColumnModelListener[].class,
        TableModelListener[].class, TreeExpansionListener[].class, TreeModelListener[].class,
        TreeSelectionListener[].class, TreeWillExpandListener[].class, UndoableEditListener[].class
    };

    /// For the test that holds the table above against the layer's call sites.
    static Class<?>[] listenerArrayTypes() {
        return LISTENER_ARRAYS;
    }

    protected Object[] listenerList = NULL_ARRAY;

    public Object[] getListenerList() {
        return listenerList;
    }

    @SuppressWarnings("unchecked")
    public <T extends EventListener> T[] getListeners(Class<T> t) {
        int count = getListenerCount(t);
        T[] result = (T[]) Array.newInstance(t, count);
        int j = 0;
        for (int i = listenerList.length - 2; i >= 0; i -= 2) {
            if (listenerList[i] == t) {
                result[j++] = (T) listenerList[i + 1];
            }
        }
        return result;
    }

    public int getListenerCount() {
        return listenerList.length / 2;
    }

    public int getListenerCount(Class<?> t) {
        int count = 0;
        for (int i = 0; i < listenerList.length; i += 2) {
            if (t == listenerList[i]) {
                count++;
            }
        }
        return count;
    }

    public <T extends EventListener> void add(Class<T> t, T l) {
        if (l == null) {
            return;
        }
        if (!t.isInstance(l)) {
            throw new IllegalArgumentException("Listener " + l + " is not of type " + t);
        }
        int length = listenerList.length;
        Object[] tmp = new Object[length + 2];
        System.arraycopy(listenerList, 0, tmp, 0, length);
        tmp[length] = t;
        tmp[length + 1] = l;
        listenerList = tmp;
    }

    public <T extends EventListener> void remove(Class<T> t, T l) {
        if (l == null) {
            return;
        }
        if (!t.isInstance(l)) {
            throw new IllegalArgumentException("Listener " + l + " is not of type " + t);
        }
        int index = -1;
        for (int i = listenerList.length - 2; i >= 0; i -= 2) {
            if (listenerList[i] == t && listenerList[i + 1].equals(l)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            return;
        }
        Object[] tmp = new Object[listenerList.length - 2];
        System.arraycopy(listenerList, 0, tmp, 0, index);
        if (index < tmp.length) {
            System.arraycopy(listenerList, index + 2, tmp, index, tmp.length - index);
        }
        listenerList = tmp.length == 0 ? NULL_ARRAY : tmp;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("EventListenerList: ");
        s.append(listenerList.length / 2).append(" listeners: ");
        for (int i = 0; i <= listenerList.length - 2; i += 2) {
            s.append(" type ").append(((Class<?>) listenerList[i]).getName());
            s.append(" listener ").append(listenerList[i + 1]);
        }
        return s.toString();
    }
}
