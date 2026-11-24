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
package com.codename1.presentation;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Editing, undo and listeners.
class PresentationModelTest extends UITestBase {
    @Test
    void editsUndoAndRedo() {
        PresentationModel m = new PresentationModel(Decks.everything());
        final List<String> heard = new ArrayList<String>();
        m.addPresentationListener((model, before, after) -> heard.add("x"));
        assertTrue(m.setBounds(0, "e2", 10, 20, 30, 40));
        SlideElement e = m.getSlide(0).find("e2");
        assertEquals(10, e.x, 0);
        assertEquals(40, e.height, 0);
        assertTrue(m.undo());
        assertEquals(100, m.getSlide(0).find("e2").x, 0);
        assertTrue(m.redo());
        assertEquals(10, m.getSlide(0).find("e2").x, 0);
        assertEquals(3, heard.size());
    }

    @Test
    void aGroupUndoesAsOne() {
        PresentationModel m = new PresentationModel(Decks.everything());
        m.beginGroup();
        for (int i = 0; i < 50; i++) {
            m.setBounds(0, "e2", i, i, 50, 50);
        }
        m.endGroup();
        assertTrue(m.undo());
        assertFalse(m.canUndo());
        assertEquals(100, m.getSlide(0).find("e2").x, 0);
    }

    @Test
    void slidesAddMoveDuplicateRemove() {
        PresentationModel m = new PresentationModel(Decks.everything());
        String id = m.addSlide(-1);
        assertEquals(4, m.getSlideCount());
        assertEquals(id, m.getSlide(3).id);
        m.moveSlide(3, 0);
        assertEquals(id, m.getSlide(0).id);
        String copy = m.duplicateSlide(1);
        Slide orig = m.getSlide(1);
        Slide dup = m.getSlide(2);
        assertEquals(copy, dup.id);
        assertEquals(orig.elements.size(), dup.elements.size());
        assertNotEquals(orig.elements.get(1).id, dup.elements.get(1).id);
        // builds follow the copied elements
        assertEquals(orig.builds.size(), dup.builds.size());
        assertNotNull(dup.find(dup.builds.get(0).elementId));
        m.removeSlide(0);
        assertEquals(4, m.getSlideCount());
    }

    @Test
    void removingAnElementDropsItsBuilds() {
        PresentationModel m = new PresentationModel(Decks.everything());
        assertTrue(m.removeElement(0, "e2"));
        for (Build b : m.getSlide(0).builds) {
            assertNotEquals("e2", b.elementId);
        }
        assertFalse(m.removeElement(0, "nope"));
    }

    @Test
    void textEditsAndZOrder() {
        PresentationModel m = new PresentationModel(Decks.everything());
        assertTrue(m.setText(0, "e1", Decks.text("x", "New title")));
        assertEquals("New title", m.getSlide(0).title());
        assertTrue(m.moveElement(0, "e1", 99));
        Slide s = m.getSlide(0);
        assertEquals("e1", s.elements.get(s.elements.size() - 1).id);
        assertFalse(m.setText(0, "e4", Decks.text("y", "pictures have no text")));
    }

    @Test
    void newIdsAreNotEdits() {
        PresentationModel m = new PresentationModel();
        String a = m.newId('e');
        String b = m.newId('e');
        assertNotEquals(a, b);
        assertFalse(m.canUndo());
    }
}
