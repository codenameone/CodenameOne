/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.editor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/// Pure (no UI) unit tests for the editor document model: text mutation, the cached line index, offset
/// conversions and newline normalization.
class EditorDocumentTest {
    private EditorDocument doc;

    @BeforeEach
    void setUp() {
        doc = new EditorDocument();
    }

    @Test
    void emptyDocument() {
        assertEquals(0, doc.length());
        assertEquals("", doc.getText());
        assertEquals(1, doc.getLineCount());
        assertEquals(0, doc.lineOfOffset(0));
    }

    @Test
    void insertAndDelete() {
        doc.insert(0, "hello world");
        assertEquals("hello world", doc.getText());
        assertEquals(11, doc.length());
        doc.delete(5, 11);
        assertEquals("hello", doc.getText());
        doc.insert(0, ">> ");
        assertEquals(">> hello", doc.getText());
    }

    @Test
    void substringAndCharAt() {
        doc.setText("abcdef");
        assertEquals("cde", doc.substring(2, 5));
        assertEquals('a', doc.charAt(0));
        assertEquals('f', doc.charAt(5));
    }

    @Test
    void clampKeepsOffsetsInRange() {
        doc.setText("abc");
        assertEquals(0, doc.clamp(-5));
        assertEquals(3, doc.clamp(99));
        assertEquals(2, doc.clamp(2));
    }

    @Test
    void lineIndexForMultipleLines() {
        doc.setText("line1\nline2\nline3");
        assertEquals(3, doc.getLineCount());
        assertEquals(0, doc.getLineStart(0));
        assertEquals(6, doc.getLineStart(1));
        assertEquals(12, doc.getLineStart(2));
        assertEquals("line2", doc.getLineText(1));
        assertEquals(5, doc.getLineEnd(0));
    }

    @Test
    void offsetToLineAndColumn() {
        doc.setText("ab\ncde\nf");
        assertEquals(0, doc.lineOfOffset(0));
        assertEquals(0, doc.lineOfOffset(2));
        assertEquals(1, doc.lineOfOffset(3));
        assertEquals(1, doc.lineOfOffset(6));
        assertEquals(2, doc.lineOfOffset(7));
        assertEquals(0, doc.columnOfOffset(3)); // start of line 1
        assertEquals(3, doc.columnOfOffset(6)); // end of "cde"
    }

    @Test
    void crlfAndCrNormalizeToLf() {
        doc.setText("a\r\nb\rc\nd");
        assertEquals("a\nb\nc\nd", doc.getText());
        assertEquals(4, doc.getLineCount());
    }

    @Test
    void lineIndexUpdatesAfterEdit() {
        doc.setText("one\ntwo");
        assertEquals(2, doc.getLineCount());
        doc.insert(3, "\ninserted");
        assertEquals("one\ninserted\ntwo", doc.getText());
        assertEquals(3, doc.getLineCount());
        assertEquals("inserted", doc.getLineText(1));
    }
}
