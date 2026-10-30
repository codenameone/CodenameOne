/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.plaf.Style;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// A backdrop blur under a background that covers it is never computed.
class BackdropUnderOpaqueFillTest extends UITestBase {

    private int blursPainting(Label l) {
        Image img = Image.createImage(100, 50);
        l.setWidth(100);
        l.setHeight(50);
        int before = implementation.getBackdropBlurCount();
        l.paintComponent(img.getGraphics(), true);
        return implementation.getBackdropBlurCount() - before;
    }

    private Label blurred(int transparency) {
        Label l = new Label("x");
        Style s = l.getAllStyles();
        s.setBackdropFilterBlurRadius(20);
        s.setBgColor(0xffffff);
        s.setBgTransparency(transparency);
        return l;
    }

    @FormTest
    void opaqueFillSkipsTheBlur() {
        assertEquals(0, blursPainting(blurred(255)));
    }

    @FormTest
    void opaqueRoundedFillSkipsTheBlur() {
        Label l = blurred(255);
        l.getAllStyles().setBorder(RoundRectBorder.create().cornerRadius(2));
        assertEquals(0, blursPainting(l));
    }

    @FormTest
    void translucentFillKeepsTheBlur() {
        assertEquals(1, blursPainting(blurred(128)));
    }
}
