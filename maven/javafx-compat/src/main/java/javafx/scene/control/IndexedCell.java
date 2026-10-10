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
package javafx.scene.control;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.css.PseudoClass;

/// A cell that shows the row at an index of its view. The pseudo-class
/// states `even` and `odd` follow the index.
public class IndexedCell<T> extends Cell<T> {

    private static final PseudoClass ODD = PseudoClass.getPseudoClass("odd");
    private static final PseudoClass EVEN = PseudoClass.getPseudoClass("even");

    private final ReadOnlyIntegerWrapper index = new ReadOnlyIntegerWrapper(this, "index", -1);

    /// Creates a cell that shows no row.
    public IndexedCell() {
        getStyleClass().add("indexed-cell");
    }

    /// Returns the row this cell shows, -1 for none.
    public final int getIndex() {
        return index.get();
    }

    /// The row this cell shows.
    public final ReadOnlyIntegerProperty indexProperty() {
        return index.getReadOnlyProperty();
    }

    /// Gives the cell another row to show; a view does.
    public void updateIndex(int i) {
        int old = index.get();
        if (old != i) {
            index.set(i);
            boolean even = i % 2 == 0;
            pseudoClassStateChanged(EVEN, i >= 0 && even);
            pseudoClassStateChanged(ODD, i >= 0 && !even);
        }
        indexChanged(old, i);
    }

    /// Called after every [#updateIndex(int)], changed or not: a subclass
    /// fetches the item of the row here.
    void indexChanged(int oldIndex, int newIndex) {
    }
}
