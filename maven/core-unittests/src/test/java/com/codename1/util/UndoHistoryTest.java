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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The history's order, its compounds and what it ignores.
class UndoHistoryTest {
    /// Appends a letter, and takes it back.
    private static final class Append implements UndoableEdit {
        private final StringBuilder text;
        private final char c;
        private final UndoHistory reenter;

        Append(StringBuilder text, char c, UndoHistory reenter) {
            this.text = text;
            this.c = c;
            this.reenter = reenter;
            text.append(c);
        }

        @Override
        public void undo() {
            text.setLength(text.length() - 1);
            if (reenter != null) {
                reenter.record(this);
            }
        }

        @Override
        public void redo() {
            text.append(c);
        }

        @Override
        public String getName() {
            return "Type " + c;
        }
    }

    @Test
    void undoesInReverseAndRedoesInOrder() {
        UndoHistory h = new UndoHistory();
        StringBuilder s = new StringBuilder();
        h.record(new Append(s, 'a', null));
        h.record(new Append(s, 'b', null));
        assertEquals("Type b", h.getUndoName());
        assertTrue(h.undo());
        assertEquals("a", s.toString());
        assertEquals("Type b", h.getRedoName());
        assertTrue(h.undo());
        assertEquals("", s.toString());
        assertFalse(h.undo());
        assertNull(h.getUndoName());
        assertTrue(h.redo());
        assertTrue(h.redo());
        assertEquals("ab", s.toString());
        assertFalse(h.redo());
    }

    @Test
    void aNewEditDropsWhatCouldBeRedone() {
        UndoHistory h = new UndoHistory();
        StringBuilder s = new StringBuilder();
        h.record(new Append(s, 'a', null));
        h.undo();
        assertTrue(h.canRedo());
        h.record(new Append(s, 'c', null));
        assertFalse(h.canRedo());
        assertEquals("c", s.toString());
    }

    @Test
    void aCompoundIsOneStepAndNests() {
        UndoHistory h = new UndoHistory();
        StringBuilder s = new StringBuilder();
        h.beginCompound("Paste");
        h.record(new Append(s, 'a', null));
        h.beginCompound("inner");
        h.record(new Append(s, 'b', null));
        h.endCompound();
        assertTrue(h.isCompoundOpen());
        assertFalse(h.canUndo());
        h.record(new Append(s, 'c', null));
        h.endCompound();
        assertEquals(1, h.getUndoCount());
        assertEquals("Paste", h.getUndoName());
        h.undo();
        assertEquals("", s.toString());
        h.redo();
        assertEquals("abc", s.toString());
    }

    @Test
    void anEmptyCompoundLeavesNoStep() {
        UndoHistory h = new UndoHistory();
        h.beginCompound("Nothing");
        h.endCompound();
        assertFalse(h.canUndo());
        h.endCompound();
        assertFalse(h.isCompoundOpen());
    }

    @Test
    void editsRecordedWhileReplayingAreIgnored() {
        UndoHistory h = new UndoHistory();
        StringBuilder s = new StringBuilder();
        h.record(new Append(s, 'a', h));
        h.undo();
        assertEquals(0, h.getUndoCount());
        assertEquals(1, h.getRedoCount());
    }

    @Test
    void theLimitDropsTheOldestAndListenersHear() {
        UndoHistory h = new UndoHistory();
        final int[] events = new int[1];
        h.addListener(new UndoHistoryListener() {
            @Override
            public void historyChanged(UndoHistory history) {
                events[0]++;
            }
        });
        h.setLimit(2);
        StringBuilder s = new StringBuilder();
        h.record(new Append(s, 'a', null));
        h.record(new Append(s, 'b', null));
        h.record(new Append(s, 'c', null));
        assertEquals(2, h.getUndoCount());
        assertEquals(3, events[0]);
        h.undo();
        h.undo();
        assertEquals("a", s.toString());
        h.clear();
        assertFalse(h.canRedo());
        assertEquals(6, events[0]);
    }

    @Test
    void anEditThatThrowsStaysInTheHistory() {
        UndoHistory h = new UndoHistory();
        final int[] calls = new int[1];
        h.record(new UndoableEdit() {
            @Override
            public void undo() {
                calls[0]++;
                if (calls[0] == 1) {
                    throw new IllegalStateException("model refused");
                }
            }

            @Override
            public void redo() {
            }

            @Override
            public String getName() {
                return "Flaky";
            }
        });
        try {
            h.undo();
        } catch (IllegalStateException expected) {
            // the edit's own failure reaches the caller
        }
        assertEquals("Flaky", h.getUndoName());
        assertFalse(h.canRedo());
        assertTrue(h.undo());
        assertEquals("Flaky", h.getRedoName());
    }
}
