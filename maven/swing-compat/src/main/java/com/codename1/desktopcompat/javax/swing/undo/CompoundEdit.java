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

import java.util.Vector;

/// Several edits that are undone and redone as one. Edits are collected
/// until [#end] is called; only then can the compound be undone.
public class CompoundEdit extends AbstractUndoableEdit {

    /// The edits collected so far, oldest first.
    protected Vector<UndoableEdit> edits = new Vector<UndoableEdit>();

    private boolean inProgress = true;

    public CompoundEdit() {
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        for (int i = edits.size() - 1; i >= 0; i--) {
            edits.elementAt(i).undo();
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        for (int i = 0; i < edits.size(); i++) {
            edits.elementAt(i).redo();
        }
    }

    /// The edit added last, or null.
    protected UndoableEdit lastEdit() {
        int count = edits.size();
        return count > 0 ? edits.elementAt(count - 1) : null;
    }

    @Override
    public void die() {
        for (int i = edits.size() - 1; i >= 0; i--) {
            edits.elementAt(i).die();
        }
        super.die();
    }

    @Override
    public boolean addEdit(UndoableEdit anEdit) {
        if (!inProgress) {
            return false;
        }
        UndoableEdit last = lastEdit();
        if (last == null) {
            edits.addElement(anEdit);
        } else if (!last.addEdit(anEdit)) {
            if (anEdit.replaceEdit(last)) {
                edits.removeElementAt(edits.size() - 1);
            }
            edits.addElement(anEdit);
        }
        return true;
    }

    /// Stops collecting; the compound can be undone from now on.
    public void end() {
        inProgress = false;
    }

    @Override
    public boolean canUndo() {
        return !isInProgress() && super.canUndo();
    }

    @Override
    public boolean canRedo() {
        return !isInProgress() && super.canRedo();
    }

    public boolean isInProgress() {
        return inProgress;
    }

    @Override
    public boolean isSignificant() {
        for (int i = 0; i < edits.size(); i++) {
            if (edits.elementAt(i).isSignificant()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getPresentationName() {
        UndoableEdit last = lastEdit();
        return last != null ? last.getPresentationName() : super.getPresentationName();
    }

    @Override
    public String getUndoPresentationName() {
        UndoableEdit last = lastEdit();
        return last != null ? last.getUndoPresentationName() : super.getUndoPresentationName();
    }

    @Override
    public String getRedoPresentationName() {
        UndoableEdit last = lastEdit();
        return last != null ? last.getRedoPresentationName() : super.getRedoPresentationName();
    }

    @Override
    public String toString() {
        return super.toString() + " inProgress: " + inProgress + " edits: " + edits;
    }
}
