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
 * Please contact Codename One through http://www.codenameone.com/ if
 * you need additional information or have any questions.
 */
package com.codename1.impl.javase;

import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A string the bundled fonts cannot display -- Hebrew, Arabic, CJK, emoji --
/// is drawn as an attributed string in a fallback font, and was measured by
/// the ink of that drawing rather than by its advance. The ink leaves out the
/// side bearings and starts after the pen, so the text ended past the width
/// stringWidth() answered: right aligned Hebrew in a label with no side
/// padding lost the edge of its first letter.
public class JavaSEPortMixedFontWidthTest {
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    private static AttributedString text(String value) {
        AttributedString text = new AttributedString(value);
        text.addAttribute(TextAttribute.FONT, new Font(Font.SANS_SERIF, Font.PLAIN, 36));
        return text;
    }

    @Test
    void widthCoversEverythingTheDrawingPaints() {
        for (String value : new String[]{"Account", "\u05d7\u05e9\u05d1\u05d5\u05df", "WAVE", "jelly"}) {
            TextLayout layout = new TextLayout(text(value).getIterator(), FRC);
            Rectangle2D ink = layout.getBounds();
            Rectangle2D measured = JavaSEPort.mixedFontBounds(text(value).getIterator(), FRC);
            assertEquals(0d, measured.getX(), 0d, "measured from the pen, where drawString starts");
            assertTrue(measured.getWidth() >= ink.getX() + ink.getWidth(),
                    value + ": the ink ends at " + (ink.getX() + ink.getWidth())
                            + " and the width answered was " + measured.getWidth());
            assertTrue(measured.getWidth() >= layout.getAdvance(), value + ": narrower than its advance");
        }
    }

    @Test
    void trailingSpaceHasWidth() {
        // No ink at all in a space, so measuring the ink made "a " as wide as "a"
        // and a string of spaces zero wide.
        double word = JavaSEPort.mixedFontBounds(text("word").getIterator(), FRC).getWidth();
        double spaced = JavaSEPort.mixedFontBounds(text("word  ").getIterator(), FRC).getWidth();
        assertTrue(spaced > word, "two trailing spaces added nothing: " + word + " then " + spaced);
    }

    @Test
    void agreesWithTheSingleFontMeasure() {
        // The same string in one font is measured by Font.getStringBounds; the
        // two paths must not disagree about it, or a label changes width the
        // moment one unsupported character is typed into it.
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 36);
        double single = font.getStringBounds("Account", FRC).getWidth();
        double mixed = JavaSEPort.mixedFontBounds(text("Account").getIterator(), FRC).getWidth();
        assertEquals(single, mixed, 1.5d);
    }
}
