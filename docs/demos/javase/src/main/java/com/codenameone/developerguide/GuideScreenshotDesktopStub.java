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

package com.codenameone.developerguide;

import com.codename1.ui.Display;
import com.codenameone.developerguide.screenshots.FigureDevice;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * JavaSE desktop entry point used by docs automation to regenerate guide screenshots.
 */
public final class GuideScreenshotDesktopStub implements Runnable {
    private static final String MODE_SCHEMATIC = "schematic";
    private static final String APP_TITLE = "Guide Screenshots";
    private static final String APP_NAME = "DemoCodeScreenshots";
    private static final String APP_VERSION = "1.0";
    private static final int APP_WIDTH = 800;
    private static final int APP_HEIGHT = 600;
    private static final boolean APP_RESIZEABLE = false;
    private static final boolean APP_FULLSCREEN = false;

    private static JFrame frame;

    public static void main(String[] args) {
        System.out.println("Starting guide screenshot desktop stub");
        System.setProperty("java.awt.headless", "false");
        System.setProperty("guide.screenshot.output", args.length > 0 ? args[0] : "target/generated-guide-screenshots");

        // "schematic" draws the layout diagrams; a device key ("ios", "android")
        // draws the themed figures at that profile. They are separate JVMs because
        // the density below is read once during Display.init and then cached on the
        // implementation instance, so one process cannot render two profiles.
        String mode = args.length > 1 ? args[1] : MODE_SCHEMATIC;
        System.setProperty("guide.screenshot.mode", mode);

        // Pin what the host would otherwise decide. retinaScale is computed from
        // the display and multiplies the system font sizes above 1.5, so a Retina
        // Mac and a CI runner disagree without it.
        System.setProperty("cn1.retinaScale", "1");

        // The same argument applies to the locale and the time zone, and they are
        // easier to miss because most figures do not visibly depend on them.
        // JavaSE's L10NManager is built from Locale.getDefault(), so currency,
        // number, date, language and country strings all follow whoever ran the
        // generator -- the committed localization figure was rendered on a Mac set
        // to Israel and could not be reproduced anywhere else. Set them before
        // Display.init, because the implementation reads them as it comes up.
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // A simulator skin decides platform name, fonts, geometry and device class,
        // and it is selected from these two properties: JavaSEPort.hasSkins() is
        // "skin != null || dskin != null", and every skin path sits behind it, the
        // stored preference included. Whoever launches the generator could have
        // either set -- from a shell, an IDE run configuration, an inherited
        // environment -- and the figures would then be rendered against that
        // device's state while still being written to the same golden files. The
        // renderer refuses an iOS skin by platform name, but an Android skin
        // reports "and" and would pass that check while changing everything.
        // Clearing both is what actually guarantees no skin loads.
        System.clearProperty("skin");
        System.clearProperty("dskin");
        if (!MODE_SCHEMATIC.equals(mode)) {
            FigureDevice device = FigureDevice.fromKey(mode);
            System.setProperty("cn1.javase.pixelMilliRatio",
                    String.valueOf(device.pixelMilliRatio()));
        }

        frame = new JFrame(APP_TITLE);
        Display.init(frame.getContentPane());
        Display.getInstance().setProperty("AppName", APP_NAME);
        Display.getInstance().setProperty("AppVersion", APP_VERSION);
        Display.getInstance().setProperty("Platform", System.getProperty("os.name"));
        Display.getInstance().setProperty("OSVer", System.getProperty("os.version"));

        SwingUtilities.invokeLater(new GuideScreenshotDesktopStub());
    }

    @Override
    public void run() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (APP_FULLSCREEN && gd.isFullScreenSupported()) {
            frame.setResizable(false);
            frame.setUndecorated(true);
            gd.setFullScreenWindow(frame);
        } else {
            frame.setLocationByPlatform(true);
            frame.setResizable(APP_RESIZEABLE);
            frame.getContentPane().setPreferredSize(new java.awt.Dimension(APP_WIDTH, APP_HEIGHT));
            frame.getContentPane().setMinimumSize(new java.awt.Dimension(APP_WIDTH, APP_HEIGHT));
            frame.getContentPane().setMaximumSize(new java.awt.Dimension(APP_WIDTH, APP_HEIGHT));
            frame.pack();
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                try {
                    GuideScreenshotGeneratorStub.generateInto(
                            new File(System.getProperty("guide.screenshot.output")),
                            System.getProperty("guide.screenshot.mode", MODE_SCHEMATIC));
                } catch (Exception err) {
                    err.printStackTrace();
                    System.exit(1);
                }
                Display.getInstance().exitApplication();
            }
        });
    }
}
