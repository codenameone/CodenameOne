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

/// Keeps the undoable edit listeners of an object and posts edits to them,
/// optionally gathering the edits made between [#beginUpdate] and
/// [#endUpdate] into one compound edit.
public class UndoableEditSupport {

    /// How many `beginUpdate` calls are still open.
    protected int updateLevel;

    /// The compound being gathered, or null outside an update.
    protected CompoundEdit compoundEdit;

    protected Vector<UndoableEditListener> listeners = new Vector<UndoableEditListener>();

    /// The object events name as their source.
    protected Object realSource;

    public UndoableEditSupport() {
        this(null);
    }

    public UndoableEditSupport(Object r) {
        realSource = r == null ? this : r;
    }

    public void addUndoableEditListener(UndoableEditListener l) {
        listeners.addElement(l);
    }

    public void removeUndoableEditListener(UndoableEditListener l) {
        listeners.removeElement(l);
    }

    public UndoableEditListener[] getUndoableEditListeners() {
        UndoableEditListener[] out = new UndoableEditListener[listeners.size()];
        listeners.copyInto(out);
        return out;
    }

    /// Tells every listener about `e`, bypassing any update in progress.
    protected void _postEdit(UndoableEdit e) {
        UndoableEditEvent ev = new UndoableEditEvent(realSource, e);
        UndoableEditListener[] all = getUndoableEditListeners();
        for (int i = 0; i < all.length; i++) {
            all[i].undoableEditHappened(ev);
        }
    }

    /// Posts `e` to the listeners, or adds it to the compound of the update
    /// in progress.
    public void postEdit(UndoableEdit e) {
        if (updateLevel == 0) {
            _postEdit(e);
        } else {
            compoundEdit.addEdit(e);
        }
    }

    public int getUpdateLevel() {
        return updateLevel;
    }

    public void beginUpdate() {
        if (updateLevel == 0) {
            compoundEdit = createCompoundEdit();
        }
        updateLevel++;
    }

    protected CompoundEdit createCompoundEdit() {
        return new CompoundEdit();
    }

    public void endUpdate() {
        updateLevel--;
        if (updateLevel == 0) {
            compoundEdit.end();
            _postEdit(compoundEdit);
            compoundEdit = null;
        }
    }

    @Override
    public String toString() {
        return super.toString() + " updateLevel: " + updateLevel + " listeners: " + listeners
                + " compoundEdit: " + compoundEdit;
    }
}
