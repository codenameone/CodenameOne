/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import com.codename1.flutter.Color;
import com.codename1.flutter.Colors;
import com.codename1.flutter.FontWeight;
import com.codename1.flutter.TextStyle;
import dart.core.DartList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TextSpan flattening + style resolution — the pure half of RichText, testable
 * without a Display. Wrapping and painting are delegated to
 * {@link com.codename1.ui.RichTextComponent} and covered in the core unit tests.
 */
public class RichTextSpanTest {

    private TextSpan span(String text, TextStyle style, TextSpan... children) {
        TextSpan s = new TextSpan();
        if (text != null) {
            s.text(text);
        }
        if (style != null) {
            s.style(style);
        }
        if (children.length > 0) {
            DartList<TextSpan> kids = new DartList<TextSpan>();
            for (TextSpan c : children) {
                kids.add(c);
            }
            s.children(kids);
        }
        return s;
    }

    private TextStyle style(Double size, FontWeight weight, Color color) {
        TextStyle t = new TextStyle();
        if (size != null) {
            t.fontSize(size);
        }
        if (weight != null) {
            t.fontWeight(weight);
        }
        if (color != null) {
            t.color(color);
        }
        return t;
    }

    // ------------------------------------------------------------------
    // Flattening
    // ------------------------------------------------------------------

    @Test
    public void ownTextPrecedesChildrenDepthFirst() {
        TextSpan root = span("a", null,
                span("b", null, span("c", null)),
                span("d", null));
        List<RichTextRenderElement.Run> runs = RichTextRenderElement.flatten(root);
        StringBuilder sb = new StringBuilder();
        for (RichTextRenderElement.Run r : runs) {
            sb.append(r.text);
        }
        assertEquals("abcd", sb.toString());
    }

    @Test
    public void emptyOrNullTextContributesNoRunButChildrenSurvive() {
        TextSpan root = span(null, null, span("only", null));
        List<RichTextRenderElement.Run> runs = RichTextRenderElement.flatten(root);
        assertEquals(1, runs.size());
        assertEquals("only", runs.get(0).text);
    }

    @Test
    public void childInheritsParentStyleProperties() {
        TextStyle parent = style(20.0, FontWeight.bold, Colors.red);
        // child overrides only the color; size and weight must inherit
        TextSpan root = span("p", parent, span("c", style(null, null, Colors.blue)));
        List<RichTextRenderElement.Run> runs = RichTextRenderElement.flatten(root);
        assertEquals(2, runs.size());

        RichTextRenderElement.Run child = runs.get(1);
        assertEquals("c", child.text);
        assertEquals(20.0, child.style.getFontSize(), 0.001, "fontSize inherits");
        assertSame(FontWeight.bold, child.style.getFontWeight(), "fontWeight inherits");
        assertEquals(Colors.blue.value(), child.style.getColor().value(), "own color wins");
    }

    @Test
    public void deepInheritanceChains() {
        TextSpan root = span("a", style(30.0, null, null),
                span("b", null,
                        span("c", style(null, FontWeight.bold, null))));
        List<RichTextRenderElement.Run> runs = RichTextRenderElement.flatten(root);
        RichTextRenderElement.Run deepest = runs.get(2);
        assertEquals("c", deepest.text);
        assertEquals(30.0, deepest.style.getFontSize(), 0.001,
                "size inherits through an intermediate span with no style");
        assertSame(FontWeight.bold, deepest.style.getFontWeight());
    }

    // ------------------------------------------------------------------
    // Style mapping to the editor model consumed by RichTextComponent
    // ------------------------------------------------------------------

    @Test
    public void resolvedStyleMapsToEditorStyle() {
        TextStyle flutter = style(24.0, FontWeight.bold, Colors.red);
        com.codename1.ui.editor.TextStyle editor =
                RichTextRenderElement.toEditorStyle(flutter);
        assertTrue(editor.isBold(), "bold weight maps to editor bold");
        assertTrue(editor.getFontSizePx() > 0, "font size maps to an absolute pixel size");
        assertEquals(Colors.red.rgb(), editor.getForeColor(), "color maps to foreground color");
    }

    @Test
    public void nullStyleMapsToDefault() {
        assertSame(com.codename1.ui.editor.TextStyle.DEFAULT,
                RichTextRenderElement.toEditorStyle(null));
    }
}
