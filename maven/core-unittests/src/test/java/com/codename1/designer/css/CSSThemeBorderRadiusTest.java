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
package com.codename1.designer.css;

import com.codename1.junit.UITestBase;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.CSSBorder;
import com.codename1.ui.plaf.RoundRectBorder;
import org.junit.jupiter.api.Test;
import org.w3c.css.sac.LexicalUnit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Build-time tests for the border a `border-radius` rule compiles to.
///
/// A stylesheet expects `border-radius` to round the box it already asked for. The
/// `RoundRectBorder` the compiler emits defaults to reserving twice the radius instead,
/// which is what turned a 3mm radius into an inflated button in
/// [discussion 5454](https://github.com/codenameone/CodenameOne/discussions/5454), so every
/// border the compiler generates has to be marked as sized by the CSS box model.
/// Extends the UI test base because generating a `RoundRectBorder` measures millimetres,
/// which needs a live `Display`, not just the implementation `CSSTheme.load` reads through.
class CSSThemeBorderRadiusTest extends UITestBase {

    @Test
    void asymmetricRadiusCompilesToACssSizedRoundRectBorder() throws Exception {
        // The exact rule from the report.
        Border border = borderOf("btnSend { border-radius: 0mm 3mm 3mm 0mm; margin: 2mm 2mm 2mm 0mm;"
                + " padding: 0.6mm 4mm 0.6mm 4mm; border: none; font-size: 3mm;"
                + " color: white; background: black; }", "btnSend");

        RoundRectBorder roundRect = assertInstanceOf(RoundRectBorder.class, border);
        assertTrue(roundRect.isCssBoxModel(),
                "a generated border must not inflate the box the stylesheet sized");
        assertEquals(3f, roundRect.getCornerRadius(), 0.001f);
        assertFalse(roundRect.isTopLeft(), "the left corners stay square");
        assertFalse(roundRect.isBottomLeft(), "the left corners stay square");
        assertTrue(roundRect.isTopRight());
        assertTrue(roundRect.isBottomRight());
        assertEquals(0, roundRect.getMinimumHeight(),
                "the radius may not reserve any height of its own");
    }

    @Test
    void uniformRadiusCompilesToACssSizedRoundRectBorder() throws Exception {
        Border border = borderOf("Dialog { border-radius: 4mm; border: none; background: white; }", "Dialog");

        RoundRectBorder roundRect = assertInstanceOf(RoundRectBorder.class, border);
        assertTrue(roundRect.isCssBoxModel());
        assertTrue(roundRect.isTopLeft() && roundRect.isTopRight()
                        && roundRect.isBottomLeft() && roundRect.isBottomRight(),
                "all four corners are rounded");
    }

    @Test
    void perCornerRadiiStillCompileToACssBorder() throws Exception {
        // RoundRectBorder carries a single radius, so corners that differ from each other
        // have to keep going to CSSBorder rather than being flattened into one radius.
        Border border = borderOf("Mixed { border-radius: 0mm 2mm 4mm 0mm; border: none;"
                + " background: black; }", "Mixed");

        assertInstanceOf(CSSBorder.class, border);
    }

    private Border borderOf(String css, String uiid) throws Exception {
        File f = File.createTempFile("cn1-test-", ".css");
        f.deleteOnExit();
        // Explicit UTF-8 rather than the platform default, so the parser reads back what
        // was written no matter which charset the host defaults to.
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(css);
        }
        CSSTheme theme = CSSTheme.load(f.toURI().toURL());
        CSSTheme.Element element = theme.elements.get(uiid);
        assertNotNull(element, "Missing UIID: " + uiid);
        Map<String, LexicalUnit> styles = element.getUnselected().getFlattenedStyle();
        return element.getThemeBorder(styles);
    }
}
