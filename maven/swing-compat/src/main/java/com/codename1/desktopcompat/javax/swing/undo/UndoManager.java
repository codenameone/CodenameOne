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
package com.codename1.desktopcompat.javax.swing.undo;

import com.codename1.desktopcompat.javax.swing.event.UndoableEditEvent;
import com.codename1.desktopcompat.javax.swing.event.UndoableEditListener;

import java.util.Vector;

/// A history of edits with a position in it: `undo` steps back over the
/// last significant edit, `redo` steps forward again, and adding an edit
/// drops everything that had been undone. The history holds at most
/// [#getLimit] edits, a hundred unless set otherwise.
///
/// Once [#end] is called the manager behaves as the single compound edit it
/// extends.
public class UndoManager extends CompoundEdit implements UndoableEditListener {

    private int indexOfNextAdd;
    private int limit = 100;

    public UndoManager() {
        edits.ensureCapacity(limit);
    }

    public int getLimit() {
        return limit;
    }

    /// Empties the history, telling every edit it is dead.
    public void discardAllEdits() {
        for (int i = 0; i < edits.size(); i++) {
            edits.elementAt(i).die();
        }
        edits = new Vector<UndoableEdit>();
        indexOfNextAdd = 0;
    }

    /// Drops the edits furthest from the current position until the history
    /// fits the limit.
    protected void trimForLimit() {
        if (limit >= 0) {
            int size = edits.size();
            if (size > limit) {
                int halfLimit = limit / 2;
                int keepFrom = indexOfNextAdd - 1 - halfLimit;
                int keepTo = indexOfNextAdd - 1 + halfLimit;
                if (keepTo - keepFrom + 1 > limit) {
                    keepFrom++;
                }
                if (keepFrom < 0) {
                    keepTo -= keepFrom;
                    keepFrom = 0;
                }
                if (keepTo >= size) {
                    int delta = size - keepTo - 1;
                    keepTo += delta;
                    keepFrom += delta;
                }
                trimEdits(keepTo + 1, size - 1);
                trimEdits(0, keepFrom - 1);
            }
        }
    }

    /// Removes the edits from `from` to `to`, both included, newest first.
    protected void trimEdits(int from, int to) {
        if (from <= to) {
            for (int i = to; from <= i; i--) {
                UndoableEdit e = edits.elementAt(i);
                e.die();
                edits.removeElementAt(i);
            }
            if (indexOfNextAdd > to) {
                indexOfNextAdd -= to - from + 1;
            } else if (indexOfNextAdd >= from) {
                indexOfNextAdd = from;
            }
        }
    }

    public void setLimit(int l) {
        if (!isInProgress()) {
            throw new RuntimeException("Attempt to call UndoManager.setLimit() after UndoManager.end() has been called");
        }
        limit = l;
        trimForLimit();
    }

    /// The significant edit `undo` would take back, or null.
    protected UndoableEdit editToBeUndone() {
        int i = indexOfNextAdd;
        while (i > 0) {
            UndoableEdit edit = edits.elementAt(--i);
            if (edit.isSignificant()) {
                return edit;
            }
        }
        return null;
    }

    /// The significant edit `redo` would apply again, or null.
    protected UndoableEdit editToBeRedone() {
        int count = edits.size();
        int i = indexOfNextAdd;
        while (i < count) {
            UndoableEdit edit = edits.elementAt(i++);
            if (edit.isSignificant()) {
                return edit;
            }
        }
        return null;
    }

    /// Undoes every edit back to `edit`, included.
    protected void undoTo(UndoableEdit edit) throws CannotUndoException {
        boolean done = false;
        while (!done) {
            UndoableEdit next = edits.elementAt(--indexOfNextAdd);
            next.undo();
            done = next == edit;
        }
    }

    /// Redoes every edit up to `edit`, included.
    protected void redoTo(UndoableEdit edit) throws CannotRedoException {
        boolean done = false;
        while (!done) {
            UndoableEdit next = edits.elementAt(indexOfNextAdd++);
            next.redo();
            done = next == edit;
        }
    }

    /// Undoes when the position is at the end of the history, redoes
    /// otherwise: what a single "undo" key that toggles does.
    public void undoOrRedo() throws CannotRedoException, CannotUndoException {
        if (indexOfNextAdd == edits.size()) {
            undo();
        } else {
            redo();
        }
    }

    public boolean canUndoOrRedo() {
        if (indexOfNextAdd == edits.size()) {
            return canUndo();
        }
        return canRedo();
    }

    @Override
    public void undo() throws CannotUndoException {
        if (isInProgress()) {
            UndoableEdit edit = editToBeUndone();
            if (edit == null) {
                throw new CannotUndoException();
            }
            undoTo(edit);
        } else {
            super.undo();
        }
    }

    @Override
    public boolean canUndo() {
        if (isInProgress()) {
            UndoableEdit edit = editToBeUndone();
            return edit != null && edit.canUndo();
        }
        return super.canUndo();
    }

    @Override
    public void redo() throws CannotRedoException {
        if (isInProgress()) {
            UndoableEdit edit = editToBeRedone();
            if (edit == null) {
                throw new CannotRedoException();
            }
            redoTo(edit);
        } else {
            super.redo();
        }
    }

    @Override
    public boolean canRedo() {
        if (isInProgress()) {
            UndoableEdit edit = editToBeRedone();
            return edit != null && edit.canRedo();
        }
        return super.canRedo();
    }

    @Override
    public boolean addEdit(UndoableEdit anEdit) {
        // Whatever had been undone is gone for good.
        trimEdits(indexOfNextAdd, edits.size() - 1);
        boolean retVal = super.addEdit(anEdit);
        if (isInProgress()) {
            retVal = true;
        }
        // The compound may have merged the edit into the last one.
        indexOfNextAdd = edits.size();
        trimForLimit();
        return retVal;
    }

    @Override
    public void end() {
        super.end();
        trimEdits(indexOfNextAdd, edits.size() - 1);
    }

    public String getUndoOrRedoPresentationName() {
        if (indexOfNextAdd == edits.size()) {
            return getUndoPresentationName();
        }
        return getRedoPresentationName();
    }

    @Override
    public String getUndoPresentationName() {
        if (isInProgress()) {
            if (canUndo()) {
                UndoableEdit edit = editToBeUndone();
                if (edit != null) {
                    return edit.getUndoPresentationName();
                }
            }
            return UndoName;
        }
        return super.getUndoPresentationName();
    }

    @Override
    public String getRedoPresentationName() {
        if (isInProgress()) {
            if (canRedo()) {
                UndoableEdit edit = editToBeRedone();
                if (edit != null) {
                    return edit.getRedoPresentationName();
                }
            }
            return RedoName;
        }
        return super.getRedoPresentationName();
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        addEdit(e.getEdit());
    }

    @Override
    public String toString() {
        return super.toString() + " limit: " + limit + " indexOfNextAdd: " + indexOfNextAdd;
    }
}
