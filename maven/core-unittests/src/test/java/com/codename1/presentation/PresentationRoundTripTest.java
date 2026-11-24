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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Every writable format read back by its own reader keeps what the deck says.
class PresentationRoundTripTest extends UITestBase {
    static void assertSurvives(Presentation p, Presentation.Format f) throws IOException {
        byte[] bytes = p.toBytes(f);
        assertEquals(f, Presentation.detect(bytes), "detected as " + f);
        Presentation back = Presentation.read(bytes);
        assertEquals(p.width, back.width, 0.5f, f + " width");
        assertEquals(p.height, back.height, 0.5f, f + " height");
        assertEquals(p.slides.size(), back.slides.size(), f + " slide count");
        for (int i = 0; i < p.slides.size(); i++) {
            Slide a = p.slides.get(i);
            Slide b = back.slides.get(i);
            String where = f + " slide " + (i + 1);
            assertEquals(a.hidden, b.hidden, where + " hidden");
            assertEquals(a.transition.type, b.transition.type, where + " transition");
            if (!Transition.NONE.equals(a.transition.type)) {
                assertEquals(a.transition.direction, b.transition.direction, where + " direction");
            }
            assertEquals(notes(a), notes(b), where + " notes");
            assertEquals(a.builds.size(), b.builds.size(), where + " builds");
            for (SlideElement e : a.elements) {
                SlideElement m = match(b, e);
                assertNotNull(m, where + " lost " + e.getClass().getSimpleName() + " "
                        + e.plainText());
                if (e instanceof DiagramElement && m instanceof GroupElement) {
                    // a diagram is written as the shapes it draws; their group hugs them,
                    // inside the diagram's box
                    assertTrue(m.x >= e.x - 0.5f && m.y >= e.y - 0.5f
                            && m.x + m.width <= e.x + e.width + 0.5f
                            && m.y + m.height <= e.y + e.height + 0.5f, where + " diagram box");
                    continue;
                }
                assertEquals(e.x, m.x, 0.5f, where + " x of " + e.id);
                assertEquals(e.y, m.y, 0.5f, where + " y of " + e.id);
                assertEquals(e.width, m.width, 0.5f, where + " width of " + e.id);
                assertEquals(e.plainText().trim(), m.plainText().trim(), where + " text");
                if (e instanceof ChartElement) {
                    ChartElement c = (ChartElement) e;
                    ChartElement d = (ChartElement) m;
                    assertEquals(c.kind, d.kind, where + " chart kind");
                    assertEquals(c.seriesCount(), d.seriesCount(), where + " series");
                    assertEquals(c.values(0)[2], d.values(0)[2], 0.0001, where + " value");
                }
            }
        }
    }

    private static String notes(Slide s) {
        StringBuilder sb = new StringBuilder();
        for (com.codename1.wordprocessing.Block b : s.notes) {
            if (b instanceof com.codename1.wordprocessing.Paragraph) {
                sb.append(((com.codename1.wordprocessing.Paragraph) b).text.plainText()).append('\n');
            }
        }
        return sb.toString().trim();
    }

    /// The element of the same kind at the same place; ids need not survive a format.
    private static SlideElement match(Slide s, SlideElement e) {
        for (SlideElement c : s.elements) {
            if (c.getClass() == e.getClass() && Math.abs(c.x - e.x) < 0.5f
                    && Math.abs(c.y - e.y) < 0.5f) {
                return c;
            }
        }
        // a diagram may come back as the group of shapes it draws
        if (e instanceof DiagramElement) {
            for (SlideElement c : s.elements) {
                if (c instanceof GroupElement && c.x >= e.x - 0.5f && c.y >= e.y - 0.5f
                        && c.x < e.x + e.width && c.y < e.y + e.height) {
                    return c;
                }
            }
        }
        return null;
    }

    @Test
    void pptx() throws IOException {
        assertSurvives(Decks.everything(), Presentation.Format.PPTX);
    }

    @Test
    void odp() throws IOException {
        assertSurvives(Decks.everything(), Presentation.Format.ODP);
    }

    @Test
    void nativeFormatIsExact() throws IOException {
        Presentation p = Decks.everything();
        assertTrue(p.equals(Presentation.read(p.toBytes(Presentation.Format.NATIVE))));
    }
}
