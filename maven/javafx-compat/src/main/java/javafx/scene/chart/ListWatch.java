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
package javafx.scene.chart;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/// Follows one observable list at a time on behalf of a chart: every
/// element that enters or leaves it is announced, and so is every change.
abstract class ListWatch<E> implements ListChangeListener<E> {

    private ObservableList<E> list;

    /// Stops following the list followed so far and follows another.
    final void watch(ObservableList<E> next) {
        if (list == next) {
            return;
        }
        if (list != null) {
            list.removeListener(this);
            List<E> old = new ArrayList<E>(list);
            for (int i = 0; i < old.size(); i++) {
                removed(old.get(i));
            }
        }
        list = next;
        if (next != null) {
            List<E> now = new ArrayList<E>(next);
            for (int i = 0; i < now.size(); i++) {
                added(now.get(i));
            }
            next.addListener(this);
        }
        changed();
    }

    @Override
    public final void onChanged(Change<? extends E> change) {
        while (change.next()) {
            if (change.wasRemoved()) {
                List<? extends E> gone = change.getRemoved();
                for (int i = 0; i < gone.size(); i++) {
                    removed(gone.get(i));
                }
            }
            if (change.wasAdded()) {
                List<? extends E> came = change.getAddedSubList();
                for (int i = 0; i < came.size(); i++) {
                    added(came.get(i));
                }
            }
        }
        changed();
    }

    /// An element entered the list.
    abstract void added(E element);

    /// An element left the list.
    abstract void removed(E element);

    /// The list changed in some way.
    abstract void changed();
}
