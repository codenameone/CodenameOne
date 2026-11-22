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
package com.codename1.wordprocessing;

import com.codename1.spreadsheet.Sheet;
import com.codename1.spreadsheet.Workbook;
import com.codename1.ui.CN;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.table.DataGrid;

/// Keeps a table of a document and a live `Sheet` in step, so the table can be edited in a
/// `DataGrid` with Excel formulas, sorting and a formula bar, and the document follows.
///
/// ```java
/// TableBinding binding = new TableBinding(model, "t3");
/// Dialog d = new Dialog("Edit table", new BorderLayout());
/// d.add(BorderLayout.CENTER, binding.createGrid());
/// d.showPacked(BorderLayout.CENTER, true);
/// binding.dispose();
/// ```
///
/// An edit in the sheet replaces the table in the model, as one undoable step per cell. An edit
/// to the model -- typing in the table in a `DocumentView`, or an undo -- is copied into the
/// sheet. Each cell keeps its paragraphs and formatting; only the text of a changed cell
/// changes. Rows and columns added in the sheet are not copied back.
public final class TableBinding {
    private final DocumentModel model;
    private final String tableId;
    private final Sheet sheet;
    private boolean syncing;
    /// Volatile: a sheet changed off the EDT is followed on it, and must see a dispose made on
    /// the thread that changed it.
    private volatile boolean disposed; //NOPMD AvoidUsingVolatile - read on the EDT after a dispose on another thread
    private final DataChangedListener sheetListener = new DataChangedListener() {
        @Override
        public void dataChanged(int type, int index) {
            if (CN.isEdt()) {
                sheetChanged();
            } else {
                // the model is the EDT's; a sheet changed elsewhere is followed there, after
                // anything that thread did next -- a dispose included
                CN.callSerially(new Runnable() {
                    @Override
                    public void run() {
                        sheetChanged();
                    }
                });
            }
        }
    };
    private final DocumentListener modelListener = new DocumentListener() {
        @Override
        public void documentChanged(DocumentModel m, Document before, Document after) {
            modelChanged(before, after);
        }
    };

    /// Binds a top-level table of a model to a new sheet.
    ///
    /// #### Parameters
    ///
    /// - `model`: the model
    ///
    /// - `tableId`: the id of a table in the model's document
    public TableBinding(DocumentModel model, String tableId) {
        if (model == null) {
            throw new IllegalArgumentException("Model is required");
        }
        this.model = model;
        this.tableId = tableId;
        Table t = table(model.getDocument());
        if (t == null) {
            throw new IllegalArgumentException("No table " + tableId);
        }
        sheet = TableSheets.toSheet(t, new Workbook(), "Table");
        sheet.addDataChangeListener(sheetListener);
        model.addDocumentListener(modelListener);
    }

    /// The sheet the table is bound to.
    ///
    /// #### Returns
    ///
    /// the sheet
    public Sheet getSheet() {
        return sheet;
    }

    /// The id of the bound table.
    ///
    /// #### Returns
    ///
    /// the id
    public String getTableId() {
        return tableId;
    }

    /// A grid editing the sheet.
    ///
    /// #### Returns
    ///
    /// a new `DataGrid` over the sheet
    public DataGrid createGrid() {
        return new DataGrid(sheet);
    }

    /// Stops following: the sheet keeps its cells, the model no longer changes with it.
    public void dispose() {
        if (!disposed) {
            disposed = true;
            sheet.removeDataChangeListener(sheetListener);
            model.removeDocumentListener(modelListener);
        }
    }

    private Table table(Document d) {
        int i = d.indexOf(tableId);
        if (i < 0) {
            return null;
        }
        Block b = d.get(i);
        return b instanceof Table ? (Table) b : null;
    }

    void sheetChanged() {
        if (syncing || disposed) {
            return;
        }
        Document d = model.getDocument();
        Table t = table(d);
        if (t == null) {
            // the table was deleted; an undo can bring it back, so keep listening
            return;
        }
        Table u;
        syncing = true;
        try {
            // reading the sheet can fire its listeners (the header row is switched off and on)
            u = TableSheets.update(t, sheet);
        } finally {
            syncing = false;
        }
        if (u != t) { //NOPMD CompareObjectsWithEquals - update returns the same table when nothing changed
            syncing = true;
            try {
                model.setDocument(d.withBlock(d.indexOf(tableId), u));
            } finally {
                syncing = false;
            }
        }
    }

    void modelChanged(Document before, Document after) {
        if (syncing || disposed) {
            return;
        }
        Table t = table(after);
        if (t == null) {
            // the table was deleted; an undo can bring it back, so keep listening
            return;
        }
        if (t.equals(table(before))) {
            return;
        }
        syncing = true;
        try {
            TableSheets.fill(sheet, t);
        } finally {
            syncing = false;
        }
    }
}
