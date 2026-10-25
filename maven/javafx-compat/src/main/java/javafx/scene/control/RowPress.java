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

/// What a press on a row does to the selection of a list or a table.
final class RowPress {

    private RowPress() {
    }

    /// Applies a press on a row and answers the row a later Shift press
    /// extends from.
    static <T> int apply(MultipleSelectionModel<T> model, boolean primary, boolean shortcut, boolean shift, int row,
            int anchor) {
        if (!primary) {
            // A context menu acts on the row it was asked for.
            if (!model.isSelected(row)) {
                model.clearAndSelect(row);
            }
            return row;
        }
        boolean multiple = model.getSelectionMode() == SelectionMode.MULTIPLE;
        if (multiple && shortcut) {
            if (model.isSelected(row)) {
                model.clearSelection(row);
            } else {
                model.select(row);
            }
            return row;
        }
        if (multiple && shift && anchor >= 0) {
            model.clearSelection();
            if (anchor <= row) {
                model.selectRange(anchor, row + 1);
            } else {
                model.selectRange(anchor, row - 1);
            }
            return anchor;
        }
        model.clearAndSelect(row);
        return row;
    }
}
