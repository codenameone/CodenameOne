/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.components;

import com.codename1.ui.Button;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.share.ShareService;

import static org.junit.jupiter.api.Assertions.*;

class ShareButtonTest extends UITestBase {

    @FormTest
    void testDefaultConstructorSetsDefaults() {
        ShareButton button = new ShareButton();
        assertEquals("ShareButton", button.getUIID());
        assertNotNull(button.getIcon());
    }

    @FormTest
    void testTextToShareGetterAndSetter() {
        ShareButton button = new ShareButton();
        assertNull(button.getTextToShare());

        button.setTextToShare("Share this text");
        assertEquals("Share this text", button.getTextToShare());
    }

    @FormTest
    void testImageToShareWithBothParameters() {
        ShareButton button = new ShareButton();
        assertNull(button.getImagePathToShare());

        button.setImageToShare("/path/to/image.png", "image/png");
        assertEquals("/path/to/image.png", button.getImagePathToShare());
    }

    @FormTest
    void testAddShareService() {
        ShareButton button = new ShareButton();
        ShareService customService = new ShareService("Custom", null) {
            @Override
            public void share(String toShare) {}

            @Override
            public boolean canShareImage() { return false; }
        };

        button.addShareService(customService);
        // Should not throw exception
        assertNotNull(button);
    }

    @FormTest
    void testButtonIsFocusable() {
        ShareButton button = new ShareButton();
        assertTrue(button.isFocusable());
    }

    @FormTest
    void testIconIsSet() {
        ShareButton button = new ShareButton();
        assertNotNull(button.getIcon(), "Share button should have a default icon");
    }

    @FormTest
    void restylingTheButtonRegeneratesTheIconInTheNewStyle() {
        // The icon used to be baked once against the button's own UIID, so an
        // app that gave it another UIID kept that UIID's opaque background
        // around the glyph.
        for (Button b : new Button[] {new ShareButton(), new InviteButton()}) {
            Object before = b.getIcon();
            b.setUIID("Label");
            assertNotNull(b.getIcon());
            assertNotSame(before, b.getIcon(), b.getClass().getName());
        }
    }
}
