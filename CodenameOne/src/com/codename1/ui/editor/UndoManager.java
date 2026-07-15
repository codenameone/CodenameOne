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

package com.codename1.ui.editor;

import java.util.ArrayList;

/// A compact undo / redo stack for the pure editors. Every mutation is recorded as a replacement of the
/// range `[start, start + removed.length)` with `inserted`; the inverse simply swaps `removed` and
/// `inserted`. Consecutive single character insertions are coalesced into one undo unit so a burst of
/// typing undoes as a word rather than a keystroke at a time.
public class UndoManager {
    static final class Edit {
        int start;
        String removed;
        String inserted;
        Object beforeState;
        Object afterState;

        Edit(int start, String removed, String inserted, Object beforeState, Object afterState) {
            this.start = start;
            this.removed = removed;
            this.inserted = inserted;
            this.beforeState = beforeState;
            this.afterState = afterState;
        }
    }

    private final ArrayList<Edit> undo = new ArrayList<Edit>();
    private final ArrayList<Edit> redo = new ArrayList<Edit>();
    private boolean coalesceAllowed;

    /// Records a mutation for later undo. Clears the redo stack.
    ///
    /// #### Parameters
    ///
    /// - `start`: the offset at which text was removed / inserted
    ///
    /// - `removed`: the text that was removed (empty for a pure insert)
    ///
    /// - `inserted`: the text that was inserted (empty for a pure delete)
    public void record(int start, String removed, String inserted) {
        record(start, removed, inserted, null, null);
    }

    void record(int start, String removed, String inserted, Object beforeState, Object afterState) {
        redo.clear();
        if (coalesceAllowed && removed.length() == 0 && inserted.length() == 1
                && inserted.charAt(0) != '\n' && !undo.isEmpty()) {
            Edit last = undo.get(undo.size() - 1);
            if (last.removed.length() == 0 && last.start + last.inserted.length() == start
                    && last.inserted.length() > 0 && last.inserted.charAt(last.inserted.length() - 1) != '\n') {
                last.inserted = last.inserted + inserted;
                last.afterState = afterState;
                return;
            }
        }
        undo.add(new Edit(start, removed, inserted, beforeState, afterState));
        coalesceAllowed = removed.length() == 0 && inserted.length() == 1;
    }

    /// Updates the post-edit feature-model snapshot of the most recent undo unit.
    void updateLastAfterState(Object afterState) {
        if (!undo.isEmpty()) {
            undo.get(undo.size() - 1).afterState = afterState;
        }
    }

    /// Breaks the current coalescing run so the next recorded insert starts a fresh undo unit. Called on
    /// caret jumps, selection changes and structural edits.
    public void breakRun() {
        coalesceAllowed = false;
    }

    /// True when there is anything to undo.
    public boolean canUndo() {
        return !undo.isEmpty();
    }

    /// True when there is anything to redo.
    public boolean canRedo() {
        return !redo.isEmpty();
    }

    /// Undoes the most recent mutation against the supplied document.
    ///
    /// #### Parameters
    ///
    /// - `doc`: the document to mutate
    ///
    /// #### Returns
    ///
    /// the caret offset after the undo, or -1 when there was nothing to undo
    public int undo(EditorDocument doc) {
        Edit edit = undoEdit(doc);
        return edit == null ? -1 : edit.start + edit.removed.length();
    }

    Edit undoEdit(EditorDocument doc) {
        if (undo.isEmpty()) {
            return null;
        }
        coalesceAllowed = false;
        Edit e = undo.remove(undo.size() - 1);
        doc.delete(e.start, e.start + e.inserted.length());
        doc.insert(e.start, e.removed);
        redo.add(e);
        return e;
    }

    /// Redoes the most recently undone mutation against the supplied document.
    ///
    /// #### Parameters
    ///
    /// - `doc`: the document to mutate
    ///
    /// #### Returns
    ///
    /// the caret offset after the redo, or -1 when there was nothing to redo
    public int redo(EditorDocument doc) {
        Edit edit = redoEdit(doc);
        return edit == null ? -1 : edit.start + edit.inserted.length();
    }

    Edit redoEdit(EditorDocument doc) {
        if (redo.isEmpty()) {
            return null;
        }
        coalesceAllowed = false;
        Edit e = redo.remove(redo.size() - 1);
        doc.delete(e.start, e.start + e.removed.length());
        doc.insert(e.start, e.inserted);
        undo.add(e);
        return e;
    }

    /// Clears all recorded history.
    public void clear() {
        undo.clear();
        redo.clear();
        coalesceAllowed = false;
    }
}
