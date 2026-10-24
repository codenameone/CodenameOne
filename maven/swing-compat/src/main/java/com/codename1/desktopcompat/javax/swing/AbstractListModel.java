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

import com.codename1.desktopcompat.javax.swing.event.ListDataEvent;
import com.codename1.desktopcompat.javax.swing.event.ListDataListener;
import java.util.ArrayList;

/// The listener half of a list model; a subclass supplies the elements and
/// calls the `fire` methods after it changed them.
public abstract class AbstractListModel<E> implements ListModel<E> {

    private final ArrayList<ListDataListener> listeners = new ArrayList<ListDataListener>();

    public AbstractListModel() {
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        // The most recently added registration goes first, as on the desktop.
        int i = listeners.lastIndexOf(l);
        if (i >= 0) {
            listeners.remove(i);
        }
    }

    public ListDataListener[] getListDataListeners() {
        return listeners.toArray(new ListDataListener[listeners.size()]);
    }

    private void fire(Object source, int type, int index0, int index1) {
        if (listeners.isEmpty()) {
            return;
        }
        ListDataEvent e = new ListDataEvent(source, type, index0, index1);
        ListDataListener[] all = getListDataListeners();
        // Last registered first: the order desktop listeners are called in.
        for (int i = all.length - 1; i >= 0; i--) {
            if (type == ListDataEvent.CONTENTS_CHANGED) {
                all[i].contentsChanged(e);
            } else if (type == ListDataEvent.INTERVAL_ADDED) {
                all[i].intervalAdded(e);
            } else {
                all[i].intervalRemoved(e);
            }
        }
    }

    protected void fireContentsChanged(Object source, int index0, int index1) {
        fire(source, ListDataEvent.CONTENTS_CHANGED, index0, index1);
    }

    protected void fireIntervalAdded(Object source, int index0, int index1) {
        fire(source, ListDataEvent.INTERVAL_ADDED, index0, index1);
    }

    protected void fireIntervalRemoved(Object source, int index0, int index1) {
        fire(source, ListDataEvent.INTERVAL_REMOVED, index0, index1);
    }
}
