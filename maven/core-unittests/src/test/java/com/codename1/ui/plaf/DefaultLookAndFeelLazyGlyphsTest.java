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
package com.codename1.ui.plaf;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// DefaultLookAndFeel builds its check box, radio button and combo box glyphs the first
/// time something asks for them instead of on every theme install. What callers see
/// must not change: after a refresh the getters still return the images the theme
/// defines, and an image an application sets after a refresh is kept, not replaced by
/// the late build.
public class DefaultLookAndFeelLazyGlyphsTest extends UITestBase {

    @Test
    public void imagesAreThereWhenAskedForAfterARefresh() {
        DefaultLookAndFeel laf = (DefaultLookAndFeel) UIManager.getInstance().getLookAndFeel();
        laf.refreshTheme(true);
        // The test implementation supports TrueType fonts, so the Material glyph
        // fallback applies and every group has images.
        assertNotNull(laf.getCheckBoxImages());
        assertNotNull(laf.getCheckBoxFocusImages());
        assertNotNull(laf.getRadioButtonImages());
        assertNotNull(laf.getRadioButtonFocusImages());
    }

    @Test
    public void anImageSetAfterARefreshIsNotReplacedByTheLateBuild() {
        DefaultLookAndFeel laf = (DefaultLookAndFeel) UIManager.getInstance().getLookAndFeel();
        laf.refreshTheme(true);
        Image checked = Image.createImage(4, 4, 0xff00ff00);
        Image unchecked = Image.createImage(4, 4, 0xffff0000);
        laf.setCheckBoxImages(checked, unchecked, checked, unchecked);
        laf.setRadioButtonImages(checked, unchecked, checked, unchecked);
        // The first read after the setter must return the application's images.
        assertSame(checked, laf.getCheckBoxImages()[1]);
        assertSame(unchecked, laf.getCheckBoxImages()[0]);
        assertSame(checked, laf.getRadioButtonImages()[1]);
        // A new refresh re-derives them from the theme, as it always did.
        laf.refreshTheme(true);
        assertNotNull(laf.getCheckBoxImages());
    }
}
