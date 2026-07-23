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
package com.codename1.videobuilder;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.Display;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintStream;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Minimal JavaSE desktop host that preserves positional CLI arguments. */
public final class VideoBuilderStub implements Runnable {
    private static JFrame frame;
    private static VideoBuilder app;
    private static PrintStream commandOut;

    public static void main(String[] args) {
        try {
            VideoBuilderCommand.set(VideoBuilderCommand.parse(args));
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            System.exit(2);
        }
        commandOut = System.out;
        VideoBuilderCommand.setOutput(commandOut);
        System.setOut(System.err);
        JavaSEPort.blockMonitors();
        JavaSEPort.setAppHomeDir(".CodenameOneVideoBuilder");
        JavaSEPort.setExposeFilesystem(true);
        JavaSEPort.setTablet(true);
        JavaSEPort.setUseNativeInput(true);
        JavaSEPort.setShowEDTWarnings(false);
        JavaSEPort.setShowEDTViolationStacks(false);
        JavaSEPort.setFontFaces("Arial", "SansSerif", "Monospaced");
        frame = new JFrame("Codename One Video Builder");
        frame.getContentPane().setPreferredSize(new Dimension(1280, 720));
        frame.pack();
        JavaSEPort.setDefaultPixelMilliRatio(Toolkit.getDefaultToolkit().getScreenResolution() / 25.4 * JavaSEPort.getRetinaScale());
        Display.init(frame.getContentPane());
        SwingUtilities.invokeLater(new VideoBuilderStub());
    }

    public void run() {
        Display.getInstance().callSerially(() -> {
            if ("mcp".equals(VideoBuilderCommand.get().name()) && VideoBuilderCommand.get().stdio()) {
                System.setOut(commandOut);
            }
            app = new VideoBuilder();
            app.init(this);
            app.start();
            if (VideoBuilderCommand.get().isInteractive()) SwingUtilities.invokeLater(() -> frame.setVisible(true));
        });
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                if (app != null) app.destroy();
                System.exit(0);
            }
        });
    }
}
