/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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

import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/// Shows that the desktop integration features are inert on the phone/tablet ports and reshape the
/// UI on every desktop port, where {@code CN.isDesktop()} is true.
///
/// The exact same code runs on every port:
///
/// * On Android / iOS / JavaScript / the JavaSE simulator ({@code CN.isDesktop() == false}) it
///   renders an ordinary mobile screen - a CN1 {@code Toolbar} with a hamburger side-menu button
///   and the usual fading touch scrollbar, which has settled to invisible by the time the
///   screenshot is taken. So the desktop features have no visible impact there.
/// * On a desktop port ({@code CN.isDesktop() == true}) the screenshot looks different: the
///   commands are in the platform's menu rather than a hamburger, and the scrollbar shows an
///   always-visible, draggable thumb with a reserved gutter that the mobile ports never display.
///
/// Where those commands actually land differs by port, and that is the point of capturing this
/// screen on all of them. macOS and the Java SE desktop build have a real menu bar, so the
/// in-app Toolbar is hidden and the commands move into it -- which is why they are not in the
/// raster. Windows and Linux have no native menu bar yet, so the Toolbar stays and draws them:
/// {@code Form.isDesktopHideToolbar()} will not hide the only place the commands exist. Both
/// outcomes are correct, and the difference between the two baselines is what records it.
///
/// The command keyboard accelerators are exercised on the desktop too, though a still screenshot
/// cannot show them.
public class DesktopModeScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() throws Exception {
        // This test used to switch desktop mode on for itself and switch it back off in
        // done(), because it was the only screen in the suite that ran with the desktop
        // chrome. It is not any more: codenameone_settings.properties sets
        // desktop.titleBar=native and desktop.interactiveScrollbars=true for the whole
        // application, and the desktop ports install their platform's native theme, which
        // turns interactiveScrollBool on through the theme rather than through a hook here.
        //
        // Keeping the local opt-in would now hide a regression rather than demonstrate a
        // feature: whatever this test switched on for itself would look right even if the
        // suite-wide settings had stopped working. What it demonstrates instead is the
        // chrome the whole suite renders in.
        Form form = createForm("Desktop Mode", new BorderLayout(), "DesktopMode");
        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);
        form.setTitle("Desktop Mode");

        // Commands surface as a hamburger side menu on mobile and as native macOS menu items on the
        // desktop. The shortcuts (Cmd+S / Cmd+R) only take effect in the desktop native menu.
        Command save = new Command("Save");
        save.setDesktopMenu(Command.DESKTOP_MENU_FILE);
        save.setDesktopShortcut('S');
        Command refresh = new Command("Refresh");
        refresh.setDesktopMenu(Command.DESKTOP_MENU_VIEW);
        refresh.setDesktopShortcut('R');
        Command about = new Command("About");
        about.setDesktopMenu(Command.DESKTOP_MENU_ABOUT);
        if (CN.isDesktop()) {
            // In desktop "native" mode the Toolbar is hidden and its side menu is never
            // constructed, so addCommandToSideMenu would NPE. Form.addCommand still registers the
            // commands, which the framework harvests into the native macOS menu bar.
            form.addCommand(save);
            form.addCommand(refresh);
            form.addCommand(about);
        } else {
            // Mobile: a hamburger side menu the desktop build never shows.
            toolbar.addCommandToSideMenu(save);
            toolbar.addCommandToSideMenu(refresh);
            toolbar.addCommandToSideMenu(about);
        }

        // A tall, overflowing list so the scrollbar is meaningful: an always-visible interactive
        // thumb on the desktop versus a faded (invisible) touch scrollbar on mobile.
        Container list = new Container(BoxLayout.y());
        list.setScrollableY(true);
        Style listStyle = list.getAllStyles();
        listStyle.setBgColor(0xfafafa);
        listStyle.setBgTransparency(255);
        listStyle.setPadding(4, 4, 4, 4);
        for (int i = 0; i < 30; i++) {
            Label row = new Label("Row " + (i + 1));
            Style rowStyle = row.getAllStyles();
            rowStyle.setBgColor(rowColor(i));
            rowStyle.setFgColor(0xffffff);
            rowStyle.setBgTransparency(255);
            rowStyle.setMargin(2, 2, 2, 2);
            rowStyle.setPadding(12, 12, 12, 12);
            list.add(row);
        }
        form.add(BorderLayout.CENTER, list);
        form.show();
        return true;
    }

    /// The 30-row scrollable form is heavy enough that on the iOS Metal backend
    /// its first frame is occasionally presented just after the capture fires,
    /// so Display.screenshot() reads the previous test's framebuffer and this
    /// test "captures the wrong form". Force a repaint and a short extra settle
    /// so the DesktopMode form is fully presented before the screenshot.
    @Override
    protected long extraSettleBeforeCaptureMillis() {
        return 700;
    }

    private static int rowColor(int i) {
        int[] palette = {0x118ab2, 0x06d6a0, 0xffd166, 0xef476f, 0x8338ec, 0x073b4c};
        return palette[i % palette.length];
    }
}
