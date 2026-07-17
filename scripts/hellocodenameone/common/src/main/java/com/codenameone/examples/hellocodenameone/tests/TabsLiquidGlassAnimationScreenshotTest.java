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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.Util;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Tabs;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

import java.io.IOException;
import java.io.InputStream;

/// Mac JavaSE frame probe for the iOS 26 UITabBar Liquid Glass selection morph.
///
/// The native reference video is 1178x2556. Its 60mm x 16mm tab tile is the
/// centred 1088x290 crop at (45,1133). This test renders that exact tile size and
/// emits every distinct frame from the first complete native last-to-first
/// transition, including the long ease-out/settle tail, rather than scaling six
/// samples into a contact sheet.
///
/// Native source frames (seconds):
/// 1.163333 through 1.760000. Relative times are normalized to the recorded
/// 350ms travel; the remaining frames deliberately hold the settled state so a
/// lingering optical/shape effect cannot disappear from the comparison. Each
/// emitted PNG remains full resolution so magnification, refraction, rim light,
/// glyph lift, and antialiasing can be compared one frame at a time.
public class TabsLiquidGlassAnimationScreenshotTest extends BaseTest {
    private static final int TILE_WIDTH = 1088;
    private static final int TILE_HEIGHT = 290;
    private static final int[] FRAME_MS = {
            0, 17, 35, 53, 70, 88, 103, 118,
            137, 155, 170, 188, 202, 220, 237, 253,
            268, 287, 302, 320, 335, 353, 370, 387,
            402, 457, 468, 487, 522, 538, 553, 597
    };
    private static final int[] FRAME_PROGRESS = {
            0, 5, 10, 15, 20, 25, 29, 34,
            39, 44, 49, 54, 58, 63, 68, 72,
            77, 82, 86, 91, 96, 100, 100, 100,
            100, 100, 100, 100, 100, 100, 100, 100
    };

    private Form layoutHost;
    private Container tile;
    private Tabs tabs;

    @Override
    public boolean runTest() {
        if (!"SE".equals(Display.getInstance().getProperty("OS", ""))) {
            System.out.println("CN1SS:INFO:test=TabsLiquidGlassAnimation"
                    + " status=SKIPPED reason=mac-javase-reference-only platform="
                    + Display.getInstance().getPlatformName());
            done();
            return true;
        }
        if (!installIosModernTheme()) {
            fail("Unable to load /iOSModernTheme.res for the Liquid Glass frame probe");
            return false;
        }
        System.out.println("CN1SS:INFO:test=TabsLiquidGlassAnimation"
                + " display=" + Display.getInstance().getDisplayWidth() + "x"
                + Display.getInstance().getDisplayHeight()
                + " pxPerMm=" + Display.getInstance().convertToPixels(1f)
                + " platform=" + Display.getInstance().getPlatformName());
        markCaptureStarted();
        renderAppearance(false, "light", 0);
        return true;
    }

    private void renderAppearance(boolean dark, String appearance, int frameIndex) {
        if (frameIndex == 0) {
            Display.getInstance().setDarkMode(Boolean.valueOf(dark));
            UIManager.getInstance().refreshTheme();
            buildTile(dark);
        }
        if (frameIndex >= FRAME_PROGRESS.length) {
            if (!dark) {
                renderAppearance(true, "dark", 0);
            } else {
                restoreAppTheme();
                done();
            }
            return;
        }

        int progress = FRAME_PROGRESS[frameIndex];
        tabs.setMorphTestState(2, 0, progress);
        Image frame = Image.createImage(TILE_WIDTH, TILE_HEIGHT, 0xff808080);
        Graphics g = frame.getGraphics();
        tile.paintComponent(g, true);
        String name = "TabsLiquidGlassAnimation_" + appearance
                + "_ms" + pad3(FRAME_MS[frameIndex])
                + "_p" + pad3(progress);
        final int next = frameIndex + 1;
        Cn1ssDeviceRunnerHelper.emitImage(frame, name,
                () -> renderAppearance(dark, appearance, next));
    }

    private void buildTile(boolean dark) {
        // No title bar: the native reference crop begins at UITabBar y=0.
        // A titled offscreen Form shifts its content pane down by the title's
        // 104px preferred height even though only the tile itself is painted.
        layoutHost = new Form(new BorderLayout());
        layoutHost.setWidth(TILE_WIDTH);
        layoutHost.setHeight(TILE_HEIGHT);
        layoutHost.setVisible(true);

        // Match the native UITabBar's component frame, not the JavaSE Tabs
        // preferred size.  The visible floating pill is inset by the iOS theme,
        // but UIKit gives the tab bar the full 60mm x 16mm capture tile.  Letting
        // FlowLayout shrink Tabs to JavaSE's preferred size produced a 260px toy
        // pill and made every lens comparison meaningless.
        tile = new Container(new BorderLayout());
        Style tileStyle = tile.getAllStyles();
        tileStyle.setBgColor(0x808080);
        tileStyle.setBgTransparency(255);
        tileStyle.setPadding(0, 0, 0, 0);
        tileStyle.setMargin(0, 0, 0, 0);

        tabs = new Tabs(Component.TOP);
        tabs.setTabTextPosition(Component.BOTTOM);
        Style iconStyle = new Style();
        iconStyle.setFgColor(dark ? 0xebebf5 : 0x2c2c2e);
        iconStyle.setBgTransparency(0);
        Image star = FontImage.createSFOrMaterial(FontImage.MATERIAL_STAR, iconStyle, 4.1f);
        Image search = FontImage.createSFOrMaterial(FontImage.MATERIAL_SEARCH, iconStyle, 4.1f);
        Image more = FontImage.createSFOrMaterial(FontImage.MATERIAL_MORE_HORIZ, iconStyle, 4.1f);
        tabs.addTab("Featured", star, star, new Container());
        tabs.addTab("Search", search, search, new Container());
        tabs.addTab("More", more, more, new Container());
        tabs.setSelectedIndex(0);

        // UIKit's bar frame starts at the top edge of this crop.  NORTH keeps
        // the production Tabs preferred height while preserving the full tile
        // width, which lands the themed pill at the native y=0 position.
        tile.add(BorderLayout.NORTH, tabs);
        layoutHost.add(BorderLayout.CENTER, tile);
        layoutHost.layoutContainer();
        tile.layoutContainer();
        tabs.layoutContainer();
        tabs.getTabsContainer().layoutContainer();
        System.out.println("CN1SS:INFO:test=TabsLiquidGlassAnimation geometry="
                + "tile=" + tile.getX() + "," + tile.getY() + ","
                + tile.getWidth() + "x" + tile.getHeight()
                + " tabs=" + tabs.getX() + "," + tabs.getY() + ","
                + tabs.getWidth() + "x" + tabs.getHeight()
                + " bar=" + tabs.getTabsContainer().getX() + ","
                + tabs.getTabsContainer().getY() + ","
                + tabs.getTabsContainer().getWidth() + "x"
                + tabs.getTabsContainer().getHeight()
                + " tab0=" + tabs.getTabsContainer().getComponentAt(0).getX() + ","
                + tabs.getTabsContainer().getComponentAt(0).getY() + ","
                + tabs.getTabsContainer().getComponentAt(0).getWidth() + "x"
                + tabs.getTabsContainer().getComponentAt(0).getHeight());
    }

    private boolean installIosModernTheme() {
        InputStream in = Display.getInstance().getResourceAsStream(
                TabsLiquidGlassAnimationScreenshotTest.class, "/iOSModernTheme.res");
        if (in == null) {
            in = TabsLiquidGlassAnimationScreenshotTest.class.getResourceAsStream("/iOSModernTheme.res");
        }
        if (in == null) {
            return false;
        }
        try {
            Resources resources = Resources.open(in);
            String[] names = resources.getThemeResourceNames();
            if (names == null || names.length == 0) {
                return false;
            }
            UIManager.getInstance().setThemeProps(resources.getTheme(names[0]));
            UIManager.getInstance().refreshTheme();
            return UIManager.getInstance().isThemeConstant("glassMaterialBool", false)
                    && UIManager.getInstance().isThemeConstant("tabsSelectionCapsuleBool", false);
        } catch (IOException ex) {
            System.out.println("CN1SS:ERR:test=TabsLiquidGlassAnimation theme=" + ex);
            return false;
        } finally {
            Util.cleanup(in);
        }
    }

    private void restoreAppTheme() {
        tabs.setMorphTestState(2, 0, -1);
        layoutHost = null;
        tile = null;
        tabs = null;
        Display.getInstance().setDarkMode(null);
        UIManager.initFirstTheme("/theme");
        UIManager.getInstance().refreshTheme();
    }

    private static String pad3(int value) {
        if (value < 10) {
            return "00" + value;
        }
        if (value < 100) {
            return "0" + value;
        }
        return String.valueOf(value);
    }
}
