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

import com.codename1.desktopcompat.javax.swing.RowSorter;
import java.util.EventObject;

/// A row sorter changed: either the keys it sorts by, or the order and
/// number of the rows it shows. The second kind carries the mapping from
/// view rows to model rows as it was before.
public class RowSorterEvent extends EventObject {

    /// The two things a sorter reports.
    public enum Type {
        SORT_ORDER_CHANGED,
        SORTED
    }

    private final Type type;
    private final int[] oldViewToModel;

    public RowSorterEvent(RowSorter source) {
        this(source, Type.SORT_ORDER_CHANGED, null);
    }

    public RowSorterEvent(RowSorter source, Type type, int[] previousRowIndexToModel) {
        super(source);
        if (type == null) {
            throw new IllegalArgumentException("type must be non-null");
        }
        this.type = type;
        this.oldViewToModel = previousRowIndexToModel;
    }

    @Override
    public RowSorter getSource() {
        Object s = super.getSource();
        return s instanceof RowSorter ? (RowSorter) s : null;
    }

    public Type getType() {
        return type;
    }

    /// The model row a view row showed before the change; the row itself
    /// when the rows were in model order then.
    public int convertPreviousRowIndexToModel(int index) {
        if (oldViewToModel != null && index >= 0 && index < oldViewToModel.length) {
            return oldViewToModel[index];
        }
        return index;
    }

    public int getPreviousRowCount() {
        return oldViewToModel == null ? 0 : oldViewToModel.length;
    }
}
