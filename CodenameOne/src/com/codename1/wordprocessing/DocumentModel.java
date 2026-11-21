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

import java.util.ArrayList;
import java.util.List;

/// The editable state behind a document view: the current immutable `Document`, its listeners
/// and its undo history.
///
/// Every change replaces the whole document, so undo is a stack of earlier documents, and since
/// a new document shares its unchanged blocks with the old one the history stays small. Edits
/// that belong together (the letters of one typed word) are joined with `beginGroup`/`endGroup`
/// into one undo step.
///
/// ```java
/// DocumentModel model = new DocumentModel(Document.read(in, Document.Format.NATIVE));
/// model.addDocumentListener(new DocumentListener() {
///     public void documentChanged(DocumentModel m, Document before, Document after) {
///         markDirty();
///     }
/// });
/// model.insertBlock(model.size(), new Paragraph(model.newId('p'), RichText.of("The end")));
/// model.undo();
/// ```
public class DocumentModel {
    private Document document;
    private final List<DocumentListener> listeners = new ArrayList<DocumentListener>();
    private final List<Document> undo = new ArrayList<Document>();
    private final List<Document> redo = new ArrayList<Document>();
    private int undoLimit = 200;
    private int groupDepth;
    private boolean groupRecorded;

    /// A model holding a new empty document.
    public DocumentModel() {
        this(new Document());
    }

    /// A model holding a document.
    ///
    /// #### Parameters
    ///
    /// - `document`: the initial document
    public DocumentModel(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("document");
        }
        this.document = document;
    }

    /// The current document.
    ///
    /// #### Returns
    ///
    /// the document
    public Document getDocument() {
        return document;
    }

    /// Replaces the document as one undoable edit and notifies the listeners. Setting an equal
    /// document does nothing.
    ///
    /// #### Parameters
    ///
    /// - `value`: the new document
    public void setDocument(Document value) {
        if (value == null) {
            throw new IllegalArgumentException("document");
        }
        if (value.equals(document) && value.nextId == document.nextId) {
            return;
        }
        if (groupDepth == 0 || !groupRecorded) {
            push(undo, document);
            groupRecorded = groupDepth > 0;
        }
        redo.clear();
        replace(value);
    }

    /// Replaces the document and forgets the history, as when a different file is opened.
    ///
    /// #### Parameters
    ///
    /// - `value`: the new document
    public void reset(Document value) {
        if (value == null) {
            throw new IllegalArgumentException("document");
        }
        undo.clear();
        redo.clear();
        replace(value);
    }

    private void replace(Document value) {
        Document before = document;
        document = value;
        DocumentListener[] l = listeners.toArray(new DocumentListener[listeners.size()]);
        for (DocumentListener listener : l) {
            listener.documentChanged(this, before, value);
        }
    }

    private void push(List<Document> stack, Document d) {
        stack.add(d);
        while (stack.size() > undoLimit) {
            stack.remove(0);
        }
    }

    /// Starts joining edits into one undo step. Groups nest; the step ends with the outermost
    /// `endGroup`.
    public void beginGroup() {
        if (groupDepth == 0) {
            groupRecorded = false;
        }
        groupDepth++;
    }

    /// Ends a group started by `beginGroup`.
    public void endGroup() {
        if (groupDepth == 0) {
            throw new IllegalStateException("endGroup without beginGroup");
        }
        groupDepth--;
    }

    /// Whether `undo` would do anything.
    ///
    /// #### Returns
    ///
    /// true when there is history
    public boolean canUndo() {
        return !undo.isEmpty();
    }

    /// Whether `redo` would do anything.
    ///
    /// #### Returns
    ///
    /// true after an undo with no edit since
    public boolean canRedo() {
        return !redo.isEmpty();
    }

    /// Restores the document before the last edit.
    ///
    /// #### Returns
    ///
    /// false when there was nothing to undo
    public boolean undo() {
        if (undo.isEmpty()) {
            return false;
        }
        push(redo, document);
        replace(undo.remove(undo.size() - 1));
        return true;
    }

    /// Re-applies the last undone edit.
    ///
    /// #### Returns
    ///
    /// false when there was nothing to redo
    public boolean redo() {
        if (redo.isEmpty()) {
            return false;
        }
        push(undo, document);
        replace(redo.remove(redo.size() - 1));
        return true;
    }

    /// Sets how many undo steps are kept.
    ///
    /// #### Parameters
    ///
    /// - `limit`: the step count, at least one
    public void setUndoLimit(int limit) {
        undoLimit = Math.max(1, limit);
        while (undo.size() > undoLimit) {
            undo.remove(0);
        }
    }

    /// Adds a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addDocumentListener(DocumentListener l) {
        listeners.add(l);
    }

    /// Removes a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeDocumentListener(DocumentListener l) {
        listeners.remove(l);
    }

    /// A fresh block id, reserved so the next call returns another.
    ///
    /// #### Parameters
    ///
    /// - `prefix`: `p` for a paragraph, `t` for a table, `i` for a picture
    ///
    /// #### Returns
    ///
    /// the id
    public String newId(char prefix) {
        String id = document.newId(prefix);
        // Reserving an id is not an edit: it changes nothing a user could undo.
        document = document.withNextId(document.nextId + 1);
        return id;
    }

    /// The number of top-level blocks.
    ///
    /// #### Returns
    ///
    /// the block count
    public int size() {
        return document.size();
    }

    /// One top-level block.
    ///
    /// #### Parameters
    ///
    /// - `index`: from zero
    ///
    /// #### Returns
    ///
    /// the block
    public Block getBlock(int index) {
        return document.get(index);
    }

    /// Replaces a block, as one undoable edit.
    ///
    /// #### Parameters
    ///
    /// - `index`: the block to replace
    ///
    /// - `block`: its replacement
    public void setBlock(int index, Block block) {
        setDocument(document.withBlock(index, block));
    }

    /// Inserts a block, as one undoable edit.
    ///
    /// #### Parameters
    ///
    /// - `index`: where, 0 to `size()`
    ///
    /// - `block`: the new block
    public void insertBlock(int index, Block block) {
        setDocument(document.insert(index, block));
    }

    /// Removes a block, as one undoable edit.
    ///
    /// #### Parameters
    ///
    /// - `index`: the block to remove
    public void removeBlock(int index) {
        setDocument(document.remove(index));
    }
}
