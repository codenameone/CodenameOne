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
package player;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.Display;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/// Opens a desktop window and runs [UnityPlayer] in it, on the Codename One
/// JavaSE port -- as a plain desktop application, with no device skin and
/// none of the simulator's menus. `run-unity-project.sh` starts it.
///
/// It does what the Codename One Maven plugin's generated desktop stub
/// does, less what a game does not need: the embedded browser's bootstrap,
/// the application icons and full screen.
///
/// System properties, all optional:
///
/// - `unity.player.title`: the window title; `Unity Player` without it.
/// - `unity.player.width`, `unity.player.height`: the size of the game
///   inside the window, 960 by 540 without them. The window can be resized.
/// - `unity.player.seed`: see [UnityPlayer].
public final class UnityPlayerDesktop {
    private static UnityPlayer app;

    private UnityPlayerDesktop() {
    }

    public static void main(String[] args) {
        final String title = System.getProperty("unity.player.title", "Unity Player");
        final int width = Integer.getInteger("unity.player.width", 960).intValue();
        final int height = Integer.getInteger("unity.player.height", 540).intValue();

        JavaSEPort.blockMonitors();
        JavaSEPort.setExposeFilesystem(true);
        JavaSEPort.setTablet(true);
        JavaSEPort.setUseNativeInput(true);
        JavaSEPort.setShowEDTViolationStacks(false);
        JavaSEPort.setShowEDTWarnings(false);
        JavaSEPort.setFullScreen(false);
        // The operating system's title bar, and no toolbar drawn by the form.
        JavaSEPort.setDesktopTitleBarMode("native");

        final JFrame frame = new JFrame(title);
        JavaSEPort.setDefaultPixelMilliRatio(Toolkit.getDefaultToolkit().getScreenResolution() / 25.4
                * JavaSEPort.getRetinaScale());
        Display.init(frame.getContentPane());
        Display.getInstance().setProperty("AppName", title);
        Display.getInstance().setProperty("Platform", System.getProperty("os.name"));
        Display.getInstance().setProperty("OSVer", System.getProperty("os.version"));
        String seed = System.getProperty("unity.player.seed");
        if (seed != null) {
            Display.getInstance().setProperty("unity.player.seed", seed);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        close();
                    }
                });
                frame.setLocationByPlatform(true);
                frame.setResizable(true);
                frame.getContentPane().setPreferredSize(new Dimension(width, height));
                frame.pack();
                // The application starts on the Codename One thread, and
                // the window is shown once it has: the first thing seen is
                // the game.
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        app = new UnityPlayer();
                        app.init(null);
                        app.start();
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                frame.setVisible(true);
                            }
                        });
                    }
                });
            }
        });
    }

    private static void close() {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (app != null) {
                    app.stop();
                    app.destroy();
                }
                Display.getInstance().exitApplication();
            }
        });
    }
}
