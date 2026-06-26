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
package com.codename1.impl.javase.simulator.backend.swing;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.javase.JavaJMFSEPort;
import com.codename1.impl.javase.JavaSEPort;
import com.codename1.impl.javase.cef.JavaCEFSEPort;
import com.codename1.impl.javase.fx.JavaFXSEPort;
import com.codename1.impl.javase.simulator.spi.SimulatorBackend;
import com.codename1.impl.javase.simulator.spi.SimulatorInputListener;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * The default simulator backend: the Swing JavaSEPort family. Picks the CEF,
 * JavaFX or JMF variant based on classpath availability and the
 * cn1.javase.implementation system property, exactly as the simulator always
 * has.
 */
public class SwingSimulatorBackend implements SimulatorBackend {
    private final List<SimulatorInputListener> listeners = new CopyOnWriteArrayList<SimulatorInputListener>();
    private boolean listenerInstalled;

    @Override
    public String getId() {
        return "swing";
    }

    @Override
    public CodenameOneImplementation createImplementation() {
        boolean cefSupported = false;
        boolean fxSupported = false;
        try {
            Class.forName("javafx.embed.swing.JFXPanel");
            fxSupported = true;
        } catch (Throwable ex) {
        }

        try {
            Class.forName("org.cef.CefApp");
            cefSupported = true;
        } catch (Throwable ex) {
        }

        String implementation = System.getProperty("cn1.javase.implementation", "");

        if (implementation.equalsIgnoreCase("cef") && cefSupported) {
            // We will use CEF
            return new JavaCEFSEPort();
        }
        if (implementation.equalsIgnoreCase("fx") && fxSupported) {
            return new JavaFXSEPort();
        }
        if (implementation.equalsIgnoreCase("jmf")) {
            return new JavaJMFSEPort();
        }
        if ("".equals(implementation)) {
            if (cefSupported) {
                return new JavaCEFSEPort();
            } else if (fxSupported) {
                return new JavaFXSEPort();
            } else {
                return new JavaJMFSEPort();
            }
        }

        return new JavaJMFSEPort();
    }

    private JavaSEPort port() {
        return JavaSEPort.instance;
    }

    @Override
    public Component getScreenComponent() {
        JavaSEPort p = port();
        return p != null ? p.getCanvasComponent() : null;
    }

    @Override
    public boolean supportsNativePeers() {
        return true;
    }

    @Override
    public void injectPointerEvent(final int type, final int x, final int y) {
        final JavaSEPort p = port();
        final JComponent canvas = p != null ? p.getCanvasComponent() : null;
        if (canvas == null) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Point cp = p.displayToCanvasCoordinate(x, y);
                int button = MouseEvent.BUTTON1;
                int modifiers = MouseEvent.BUTTON1_DOWN_MASK;
                canvas.dispatchEvent(new MouseEvent(canvas, type, System.currentTimeMillis(),
                        modifiers, cp.x, cp.y, 1, false, button));
            }
        });
    }

    @Override
    public void injectKeyEvent(final int type, final int keyCode) {
        final JavaSEPort p = port();
        final JComponent canvas = p != null ? p.getCanvasComponent() : null;
        if (canvas == null) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                canvas.dispatchEvent(new KeyEvent(canvas, type, System.currentTimeMillis(),
                        0, keyCode, KeyEvent.CHAR_UNDEFINED));
            }
        });
    }

    @Override
    public void addInputListener(SimulatorInputListener listener) {
        listeners.add(listener);
        installCanvasListener();
    }

    private void installCanvasListener() {
        if (listenerInstalled) {
            return;
        }
        final JavaSEPort p = port();
        JComponent canvas = p != null ? p.getCanvasComponent() : null;
        if (canvas == null) {
            return;
        }
        listenerInstalled = true;
        MouseAdapter adapter = new MouseAdapter() {
            private void fire(MouseEvent e) {
                Point dp = p.canvasToDisplayCoordinate(e.getX(), e.getY());
                for (SimulatorInputListener l : listeners) {
                    l.pointerEvent(e.getID(), dp.x, dp.y);
                }
            }

            public void mousePressed(MouseEvent e) {
                fire(e);
            }

            public void mouseReleased(MouseEvent e) {
                fire(e);
            }

            public void mouseDragged(MouseEvent e) {
                fire(e);
            }
        };
        canvas.addMouseListener(adapter);
        canvas.addMouseMotionListener(adapter);
        canvas.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                for (SimulatorInputListener l : listeners) {
                    l.keyEvent(e.getID(), e.getKeyCode());
                }
            }

            public void keyReleased(KeyEvent e) {
                for (SimulatorInputListener l : listeners) {
                    l.keyEvent(e.getID(), e.getKeyCode());
                }
            }
        });
    }

    @Override
    public void stop() {
        JavaSEPort p = port();
        if (p != null) {
            p.deinitializeSync();
        }
    }
}
