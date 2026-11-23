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
/// DocumentModel model = new DocumentModel(new Document());
/// try {
///     model.setDocument(Document.read(new ByteArrayInputStream(("<table><tr><th>Region</th>"
///             + "<th>Q1</th></tr><tr><td>North</td><td>120</td></tr></table>").getBytes()),
///             Document.Format.HTML));
/// } catch (IOException e) {
///     Log.e(e);
/// }
/// // wordprocessing has a Table, and so does com.codename1.ui.table
/// com.codename1.wordprocessing.Table table = null;
/// for (int i = 0; i < model.size(); i++) {
///     if (model.getDocument().get(i) instanceof com.codename1.wordprocessing.Table) {
///         table = (com.codename1.wordprocessing.Table) model.getDocument().get(i);
///     }
/// }
/// TableBinding binding = new TableBinding(model, table.id);
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
    /// Guards `disposed` and every read of the sheet. A sheet changed off the EDT is followed
    /// on it, so a sync can be under way while another thread disposes and goes on changing
    /// the sheet; checking a flag alone let such a sync read the later value and copy it into
    /// the document after the dispose. Holding the lock, dispose waits for the read.
    private final Object lock = new Object();
    private boolean disposed;
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
        synchronized (lock) {
            if (disposed) {
                return;
            }
            disposed = true;
        }
        sheet.removeDataChangeListener(sheetListener);
        model.removeDocumentListener(modelListener);
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
        Document d;
        Table t;
        Table u;
        // the lock covers reading the sheet and nothing else: a dispose waits for a read under
        // way, so no value written after it reaches the document, while the document's own
        // listeners run outside it -- one that blocks cannot hold up a dispose on the EDT
        synchronized (lock) {
            if (disposed || syncing) {
                return;
            }
            d = model.getDocument();
            t = table(d);
            if (t == null) {
                // the table was deleted; an undo can bring it back, so keep listening
                return;
            }
            syncing = true;
            try {
                // reading the sheet can fire its listeners (the header row is switched off
                // and on)
                u = TableSheets.update(t, sheet);
            } finally {
                syncing = false;
            }
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
        boolean live;
        synchronized (lock) {
            live = !disposed;
        }
        if (live) {
            syncFromModel(before, after);
        }
    }

    private void syncFromModel(Document before, Document after) {
        if (syncing) {
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
