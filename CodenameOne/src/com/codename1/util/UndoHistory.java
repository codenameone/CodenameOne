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
package com.codename1.util;

import java.util.ArrayList;
import java.util.List;

/// The edits a user made, in order, so they can be undone and redone.
///
/// A history knows nothing about what is edited. A component that supports
/// undo records an `UndoableEdit` for every change the user makes through it,
/// and one history can be shared by several components so that a single
/// "Undo" takes back the last change wherever it was made:
///
/// ```java
/// UndoHistory history = new UndoHistory();
/// graph.setUndoHistory(history);
/// board.setUndoHistory(history);
/// undoButton.addActionListener(e -> history.undo());
/// ```
///
/// Edits recorded between `beginCompound` and `endCompound` are one step:
/// dragging five selected nodes is a single "Move".
///
/// Recording a new edit drops everything that could have been redone. An edit
/// recorded while the history is itself undoing or redoing is ignored, so an
/// edit may freely call the same code a user's action does.
///
/// Like the components that use it, a history belongs to the event dispatch
/// thread.
public class UndoHistory {
    private final List<UndoableEdit> undone = new ArrayList<UndoableEdit>();
    private final List<UndoableEdit> done = new ArrayList<UndoableEdit>();
    private final List<UndoHistoryListener> listeners = new ArrayList<UndoHistoryListener>();
    private int limit = 100;
    private Compound open;
    private int depth;
    private boolean replaying;

    /// Records an edit that was just made.
    ///
    /// Inside a compound the edit joins it. Otherwise it becomes the next
    /// thing `undo` takes back, and nothing can be redone any more.
    public void record(UndoableEdit edit) {
        if (edit == null || replaying) {
            return;
        }
        if (open != null) {
            open.edits.add(edit);
            return;
        }
        push(edit);
    }

    private void push(UndoableEdit edit) {
        done.add(edit);
        undone.clear();
        while (done.size() > limit) {
            done.remove(0);
        }
        fireChanged();
    }

    /// Starts a group of edits that undo and redo as one step. Calls nest:
    /// only the outermost pair makes a step, named by the outermost call.
    ///
    /// #### Parameters
    ///
    /// - `name`: the step's name, or null
    public void beginCompound(String name) {
        if (replaying) {
            return;
        }
        if (depth == 0) {
            open = new Compound(name);
        }
        depth++;
    }

    /// Ends the group `beginCompound` started. A group in which nothing was
    /// recorded leaves no step behind.
    public void endCompound() {
        if (replaying || depth == 0) {
            return;
        }
        depth--;
        if (depth > 0) {
            return;
        }
        Compound c = open;
        open = null;
        if (c.edits.size() == 1 && c.name == null) {
            push(c.edits.get(0));
        } else if (!c.edits.isEmpty()) {
            push(c);
        }
    }

    /// True between `beginCompound` and its `endCompound`.
    public boolean isCompoundOpen() {
        return depth > 0;
    }

    /// True when there is an edit to take back.
    public boolean canUndo() {
        return !done.isEmpty();
    }

    /// True when an undone edit can be made again.
    public boolean canRedo() {
        return !undone.isEmpty();
    }

    /// The name of the edit `undo` would take back, or null.
    public String getUndoName() {
        return done.isEmpty() ? null : done.get(done.size() - 1).getName();
    }

    /// The name of the edit `redo` would make again, or null.
    public String getRedoName() {
        return undone.isEmpty() ? null : undone.get(undone.size() - 1).getName();
    }

    /// Takes back the last edit.
    ///
    /// #### Returns
    ///
    /// false when there was nothing to undo
    public boolean undo() {
        if (done.isEmpty() || replaying || depth > 0) {
            return false;
        }
        UndoableEdit e = done.remove(done.size() - 1);
        replaying = true;
        boolean ok = false;
        try {
            e.undo();
            ok = true;
        } finally {
            replaying = false;
            // an edit that threw stays where it was, so it is not lost
            if (ok) {
                undone.add(e);
            } else {
                done.add(e);
            }
        }
        // only after success: a failed edit left both stacks as they were
        fireChanged();
        return true;
    }

    /// Makes the last undone edit again.
    ///
    /// #### Returns
    ///
    /// false when there was nothing to redo
    public boolean redo() {
        if (undone.isEmpty() || replaying || depth > 0) {
            return false;
        }
        UndoableEdit e = undone.remove(undone.size() - 1);
        replaying = true;
        boolean ok = false;
        try {
            e.redo();
            ok = true;
        } finally {
            replaying = false;
            if (ok) {
                done.add(e);
            } else {
                undone.add(e);
            }
        }
        fireChanged();
        return true;
    }

    /// True while `undo` or `redo` is running an edit. A component checks it
    /// to tell a replayed change from one the user is making.
    public boolean isReplaying() {
        return replaying;
    }

    /// Forgets every edit, for instance after loading another document.
    public void clear() {
        done.clear();
        undone.clear();
        open = null;
        depth = 0;
        fireChanged();
    }

    /// How many steps are kept; older ones are dropped. 100 by default.
    public int getLimit() {
        return limit;
    }

    /// Sets how many steps are kept, at least one.
    public void setLimit(int limit) {
        this.limit = Math.max(1, limit);
        boolean dropped = false;
        while (done.size() > this.limit) {
            done.remove(0);
            dropped = true;
        }
        if (dropped) {
            fireChanged();
        }
    }

    /// The number of steps that can be undone.
    public int getUndoCount() {
        return done.size();
    }

    /// The number of steps that can be redone.
    public int getRedoCount() {
        return undone.size();
    }

    /// Adds a listener told whenever what can be undone or redone changes.
    public void addListener(UndoHistoryListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    /// Removes a listener.
    public void removeListener(UndoHistoryListener l) {
        listeners.remove(l);
    }

    private void fireChanged() {
        // a copy: a listener may add or remove listeners while it is told
        UndoHistoryListener[] all = listeners.toArray(new UndoHistoryListener[listeners.size()]);
        for (UndoHistoryListener l : all) {
            l.historyChanged(this);
        }
    }

    /// Several edits that undo in reverse and redo in order.
    private static final class Compound implements UndoableEdit {
        private final String name;
        private final List<UndoableEdit> edits = new ArrayList<UndoableEdit>();

        Compound(String name) {
            this.name = name;
        }

        // the list's cast kept out of the try blocks below: ParparVM does
        // not check a cast, so none may sit where a catch could take it
        private UndoableEdit at(int i) {
            return edits.get(i);
        }

        @Override
        public void undo() {
            int i = edits.size() - 1;
            try {
                for (; i >= 0; i--) {
                    at(i).undo();
                }
            } catch (RuntimeException err) {
                // the step stays in the history whole: what was taken back of
                // it is made again, so a later undo starts from all of it
                for (int k = i + 1; k < edits.size(); k++) {
                    at(k).redo();
                }
                throw err;
            }
        }

        @Override
        public void redo() {
            int i = 0;
            try {
                for (; i < edits.size(); i++) {
                    at(i).redo();
                }
            } catch (RuntimeException err) {
                for (int k = i - 1; k >= 0; k--) {
                    at(k).undo();
                }
                throw err;
            }
        }

        @Override
        public String getName() {
            if (name != null) {
                return name;
            }
            return edits.isEmpty() ? null : edits.get(0).getName();
        }
    }
}
