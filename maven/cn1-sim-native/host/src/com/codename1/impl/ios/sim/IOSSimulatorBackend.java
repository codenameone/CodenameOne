/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.ios.sim;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.ios.IOSDesktopImplementation;
import com.codename1.impl.javase.simulator.spi.SimulatorBackend;
import com.codename1.impl.javase.simulator.spi.SimulatorInputListener;
import com.codename1.ui.Display;

import java.awt.Canvas;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Simulator backend rendering through the iOS port: the port's Java side
 * (IOSImplementation) runs on the JVM, its natives resolve into
 * libcn1sim.dylib (Metal rendering pipeline + cn1jni runtime), and the screen
 * is a CAMetalLayer attached to a heavyweight AWT canvas via JAWT.
 *
 * <p>Select with -Dcn1.simulator.backend=ios. Requires
 * -Dcn1.sim.native.path=/path/to/libcn1sim.dylib and the CN1_SIM_METALLIB
 * environment variable pointing at cn1sim.metallib.</p>
 */
public class IOSSimulatorBackend implements SimulatorBackend {
    private JFrame frame;
    private Canvas canvas;
    private int width;
    private int height;

    @Override
    public String getId() {
        return "ios";
    }

    @Override
    public CodenameOneImplementation createImplementation() {
        CN1SimHost.load();
        width = Integer.getInteger("cn1.sim.width", 414);
        height = Integer.getInteger("cn1.sim.height", 736);
        if (Boolean.getBoolean("cn1.sim.pure")) {
            // pure mode: the dylib owns the window, input arrives through the
            // CN1SimHost native upcalls - no AWT anywhere
            CN1SimHost.createNativeWindow("Codename One", width, height);
            final IOSDesktopImplementation impl = new IOSDesktopImplementation();
            startNativeReadyThread();
            return impl;
        }
        Runnable createWindow = new Runnable() {
            public void run() {
                frame = new JFrame("Codename One - iOS Native Backend");
                canvas = new Canvas();
                canvas.setPreferredSize(new Dimension(width, height));
                canvas.setFocusable(true);
                frame.getContentPane().add(canvas);
                frame.pack();
                frame.setLocationByPlatform(true);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
                CN1SimHost.attachSurface(canvas, width, height);
                installInputForwarding();
            }
        };
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                createWindow.run();
            } else {
                SwingUtilities.invokeAndWait(createWindow);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create the iOS backend window", ex);
        }
        final IOSDesktopImplementation impl = new IOSDesktopImplementation();
        startNativeReadyThread();
        return impl;
    }

    /**
     * Unblocks initEDT once the EDT starts spinning; the surface is already
     * attached so the first paint has somewhere to go. Display may not be
     * fully initialized yet, so retry briefly.
     */
    private void startNativeReadyThread() {
        new Thread("CN1Sim-NativeReady") {
            public void run() {
                for (int attempt = 0; attempt < 50; attempt++) {
                    try {
                        Thread.sleep(200);
                        IOSDesktopImplementation.fireNativeReady();
                        return;
                    } catch (Throwable retry) {
                        // Display not ready yet
                    }
                }
            }
        }.start();
    }

    /**
     * AWT input translated straight into the CN1 event pipeline - the iOS
     * port's own input path (UIKit touches) does not exist on the desktop.
     */
    private void installInputForwarding() {
        MouseAdapter mouse = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                Display.getInstance().pointerPressed(new int[]{e.getX()}, new int[]{e.getY()});
            }

            public void mouseReleased(MouseEvent e) {
                Display.getInstance().pointerReleased(new int[]{e.getX()}, new int[]{e.getY()});
            }

            public void mouseDragged(MouseEvent e) {
                Display.getInstance().pointerDragged(new int[]{e.getX()}, new int[]{e.getY()});
            }
        };
        canvas.addMouseListener(mouse);
        canvas.addMouseMotionListener(mouse);
    }

    @Override
    public Component getScreenComponent() {
        return canvas;
    }

    @Override
    public boolean supportsNativePeers() {
        return false;
    }

    @Override
    public void injectPointerEvent(int type, int x, int y) {
        switch (type) {
            case MouseEvent.MOUSE_PRESSED:
                Display.getInstance().pointerPressed(new int[]{x}, new int[]{y});
                break;
            case MouseEvent.MOUSE_RELEASED:
                Display.getInstance().pointerReleased(new int[]{x}, new int[]{y});
                break;
            case MouseEvent.MOUSE_DRAGGED:
                Display.getInstance().pointerDragged(new int[]{x}, new int[]{y});
                break;
            default:
                break;
        }
    }

    @Override
    public void injectKeyEvent(int type, int keyCode) {
        if (type == KeyEvent.KEY_PRESSED) {
            Display.getInstance().keyPressed(keyCode);
        } else if (type == KeyEvent.KEY_RELEASED) {
            Display.getInstance().keyReleased(keyCode);
        }
    }

    @Override
    public void addInputListener(SimulatorInputListener listener) {
        // recording support arrives with the SimulatorHost integration
    }

    @Override
    public void stop() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }
}
