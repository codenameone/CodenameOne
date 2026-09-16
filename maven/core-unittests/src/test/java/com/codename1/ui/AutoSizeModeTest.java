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
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// Whether a label in auto size mode still occupies the screen.
class AutoSizeModeTest extends UITestBase {

    @FormTest
    void anAutoSizedLabelKeepsAHeight() {
        Form hi = new Form("AutoSize", BoxLayout.y());
        Label plain = new Label("Short Text");
        Label auto = new Label("Short Text");
        auto.setAutoSizeMode(true);
        hi.addAll(plain, auto);
        hi.show();
        hi.revalidate();

        assertTrue(plain.getHeight() > 0, "precondition: a plain label has a height");
        assertTrue(auto.getHeight() > 0,
                "an auto sized label collapsed to height " + auto.getHeight()
                        + " while a plain one beside it is " + plain.getHeight());
    }

    @FormTest
    void anAutoSizedButtonKeepsAHeight() {
        Form hi = new Form("AutoSize", BoxLayout.y());
        Button auto = new Button("Short Text");
        auto.setAutoSizeMode(true);
        hi.add(auto);
        hi.show();
        hi.revalidate();

        assertTrue(auto.getHeight() > 0, "an auto sized button collapsed to height " + auto.getHeight());
    }

    /// The chapter's own sample: three labels and three buttons, all auto
    /// sized, with text long enough that the font has to shrink a long way.
    @FormTest
    void theChaptersAutoSizeSampleKeepsEveryRow() {
        Form hi = new Form("AutoSize", BoxLayout.y());

        Label a = new Label("Short Text");
        a.setAutoSizeMode(true);
        Label b = new Label("Much Longer Text than the previous line...");
        b.setAutoSizeMode(true);
        Label c = new Label("MUCH MUCH MUCH Much Longer Text than the previous line by a pretty big margin...");
        c.setAutoSizeMode(true);

        Label a1 = new Button("Short Text");
        a1.setAutoSizeMode(true);
        Label b1 = new Button("Much Longer Text than the previous line...");
        b1.setAutoSizeMode(true);
        Label c1 = new Button("MUCH MUCH MUCH Much Longer Text than the previous line by a pretty big margin...");
        c1.setAutoSizeMode(true);
        hi.addAll(a, b, c, a1, b1, c1);

        hi.show();
        hi.revalidate();

        StringBuilder heights = new StringBuilder();
        Label[] all = {a, b, c, a1, b1, c1};
        for (Label l : all) {
            heights.append(l.getClass().getSimpleName()).append('=').append(l.getHeight()).append(' ');
        }
        for (Label l : all) {
            assertTrue(l.getHeight() > 0, "a row collapsed; heights were " + heights);
        }
    }
}
