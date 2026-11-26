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
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The slide editor's commands and geometry.
class SlideViewEditingTest extends UITestBase {
    private static SlideView shown(PresentationModel m) {
        SlideView v = new SlideView(m);
        Form f = new Form("Edit", new BorderLayout());
        f.add(BorderLayout.CENTER, v);
        f.show();
        return v;
    }

    @Test
    void resizingKeepsTheOppositeCornerUnderRotation() {
        SlideElement e = new ShapeElement("e", "rect", 100, 100, 200, 100, Fill.solid(0),
                LineStyle.NONE, null).withRotation(90);
        float[][] before = SlideView.handles(e, 0);
        // drag the bottom right handle (index 4) outward
        float[] b = SlideView.resize(e, 4, 10, 20);
        SlideElement after = e.withBounds(b[0], b[1], b[2], b[3]);
        float[][] h = SlideView.handles(after, 0);
        assertEquals(before[0][0], h[0][0], 0.01f, "top left stays");
        assertEquals(before[0][1], h[0][1], 0.01f, "top left stays");
    }

    @Test
    void rotationSnaps() {
        ShapeElement e = new ShapeElement("e", "rect", 0, 0, 100, 100, Fill.solid(0),
                LineStyle.NONE, null);
        // straight to the right of the centre is 90 degrees
        assertEquals(90, SlideView.rotationTo(e, 200, 51), 0.01f);
        assertEquals(0, SlideView.rotationTo(e, 50, -100), 0.01f);
    }

    @Test
    void commandsGoThroughTheModel() {
        PresentationModel m = new PresentationModel(Decks.everything());
        SlideView v = shown(m);
        String id = v.addShape("ellipse");
        assertEquals(id, v.getSelection());
        assertNotNull(m.getSlide(0).find(id));
        v.sendToBack();
        assertEquals(id, m.getSlide(0).elements.get(0).id);
        assertTrue(v.deleteSelection());
        assertNull(m.getSlide(0).find(id));
        assertTrue(m.undo());
        assertNotNull(m.getSlide(0).find(id));
        v.setSlideIndex(1);
        assertNull(v.getSelection());
    }

    @Test
    void inPlaceTextEditingIsOneUndoStep() {
        PresentationModel m = new PresentationModel(Decks.everything());
        SlideView v = shown(m);
        assertTrue(v.beginTextEdit("e1"));
        assertNotNull(v.getTextEditor());
        v.getTextEditor().selectAll();
        v.getTextEditor().typeText("Hel");
        v.getTextEditor().typeText("lo");
        v.endTextEdit();
        assertEquals("Hello", m.getSlide(0).title());
        assertTrue(m.undo());
        assertEquals("Quarterly review", m.getSlide(0).title());
        assertFalse(v.beginTextEdit("e4"), "a picture has no text");
    }
}
