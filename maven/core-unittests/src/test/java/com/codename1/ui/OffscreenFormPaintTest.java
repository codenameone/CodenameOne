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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>A Form that is built off-screen and painted into an Image must paint its children, not
 * just its own background.</p>
 *
 * <p>This is how every animation filmstrip in the screenshot suite is captured: a Form is
 * constructed, given a size, made visible, laid out and painted into an offscreen Image
 * six times at different animation progresses. It is never shown. Ten such tests exist,
 * and when this breaks all ten produce a grid of empty cells in the Form's background
 * colour -- which is a picture, so the capture succeeds and only a human looking at it can
 * tell that the content is gone.</p>
 *
 * <p>The regression that prompted this was specific to desktop "native" title bar mode,
 * where the Toolbar is deliberately never attached to the form. That path runs only when
 * the platform reports a native menu bar, so the three knobs below are the condition --
 * without them the default toolbar mode is exercised and nothing is proven.</p>
 */
public class OffscreenFormPaintTest extends UITestBase {

    private static final int TILE_COLOR = 0xef476f;

    @FormTest
    void aFormBuiltOffScreenPaintsItsChildrenInDesktopNativeTitleBarMode() {
        implementation.setDesktop(true);
        implementation.setNativeCommandsSupported(true);
        implementation.setDesktopTitleBarMode("native");
        assertTrue(paintOffScreenFormAndLookForTheTile(),
                "A Form painted off-screen in desktop 'native' title bar mode painted its "
                + "background but none of its children. Every animation filmstrip in the "
                + "screenshot suite captures exactly this way, so the goldens become grids "
                + "of empty cells that still look like valid screenshots.");
    }

    @FormTest
    void aFormBuiltOffScreenPaintsItsChildrenInToolbarMode() {
        // The control. If this one ever fails too, the fault is in off-screen painting
        // generally rather than in the native title bar path, and the test above would
        // otherwise point at the wrong thing.
        implementation.setDesktop(true);
        implementation.setNativeCommandsSupported(true);
        implementation.setDesktopTitleBarMode("toolbar");
        assertTrue(paintOffScreenFormAndLookForTheTile(),
                "A Form painted off-screen in the default toolbar mode painted its "
                + "background but none of its children.");
    }

    /**
     * Builds and paints a Form the way AbstractContainerAnimationScreenshotTest does, and
     * reports whether the child's colour reached the image.
     *
     * @return true when the tile was painted
     */
    private boolean paintOffScreenFormAndLookForTheTile() {
        int width = 400;
        int height = 300;

        // Without this the Form has no Toolbar at all, Toolbar.initMenuBar never runs, and
        // the desktop "native" branch under test is simply not reached -- the first version
        // of this test passed in both modes for exactly that reason, which is a test that
        // cannot fail rather than a behaviour that works. The screenshot suite's app turns
        // the global toolbar on, so this also matches how those captures are produced.
        Toolbar.setGlobalToolbar(true);

        Form host = new Form("Off-screen host");
        assertNotNull(host.getToolbar(),
                "The Form under test has no Toolbar, so the title bar mode cannot matter and "
                + "this test would pass whatever the mode does.");
        host.setWidth(width);
        host.setHeight(height);
        // A Form is invisible until shown, and paintComponent is a no-op while it is. The
        // filmstrip tests flip this for the same reason.
        host.setVisible(true);
        host.setLayout(new BorderLayout());

        Container content = new Container(BoxLayout.y());
        Style contentStyle = content.getAllStyles();
        contentStyle.setBgColor(0xfafafa);
        contentStyle.setBgTransparency(255);

        Label tile = new Label("Tile");
        Style tileStyle = tile.getAllStyles();
        // Explicit and fully opaque, so the assertion is about whether the child was
        // painted at all and never about what a theme would have coloured it.
        tileStyle.setBgColor(TILE_COLOR);
        tileStyle.setFgColor(0xffffff);
        tileStyle.setBgTransparency(255);
        tileStyle.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        tileStyle.setPadding(12, 12, 12, 12);
        content.add(tile);

        host.add(BorderLayout.CENTER, content);
        host.forceRevalidate();

        Image frame = Image.createImage(width, height, 0xffffffff);
        Graphics g = frame.getGraphics();
        host.paintComponent(g, true);

        int[] pixels = new int[width * height];
        frame.getRGB(pixels, 0, 0, 0, width, height);
        for (int i = 0; i < pixels.length; i++) {
            if ((pixels[i] & 0xffffff) == TILE_COLOR) {
                return true;
            }
        }
        return false;
    }
}
