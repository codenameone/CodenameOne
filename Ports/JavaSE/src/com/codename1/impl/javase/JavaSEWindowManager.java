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
package com.codename1.impl.javase;

import com.codename1.impl.WindowManager;
import com.codename1.ui.Display;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

/**
 * The JavaSE implementation of the native windowing contract. Each Codename One
 * {@code Window} becomes a {@link JFrame} holding its own instance of the port's
 * {@code C} canvas, so it gets the whole buffered blit machine -- including the
 * aliasing fast path -- without any of it being duplicated.
 *
 * <p>Multi-window is deliberately unsupported while a phone skin is loaded. A skin
 * simulates one device screen, complete with fixed screen coordinates and a zoom
 * factor, and opening a real operating system window inside that simulation is
 * incoherent. The predicate mirrors the one {@code isFullScreenSupported} already
 * uses.</p>
 *
 * @author Shai Almog
 */
public class JavaSEWindowManager extends WindowManager {

    private final JavaSEPort port;
    private final List<Peer> peers = new ArrayList<Peer>();

    JavaSEWindowManager(JavaSEPort port) {
        this.port = port;
    }

    /**
     * One native window: its frame, the canvas Codename One paints into, and the id
     * the framework tags this window's input events with.
     */
    static final class Peer {
        JFrame frame;
        JavaSEPort.C canvas;
        int windowId;
        int monitorIndex;
    }

    private static Peer peer(Object p) {
        if (p instanceof Peer) {
            return (Peer) p;
        }
        return null;
    }

    // ---- window lifecycle ---------------------------------------------------

    @Override
    public Object createWindow(final int windowId, final String title, final int x, final int y,
            final int width, final int height, final boolean decorated, final boolean resizable,
            final Object parentPeer) {
        final Peer p = new Peer();
        p.windowId = windowId;
        runOnAwtAndWait(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame(title == null ? "" : title);
                frame.setUndecorated(!decorated);
                frame.setResizable(resizable);
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.setLayout(new BorderLayout());

                JavaSEPort.C canvas = port.createWindowCanvas(windowId);
                frame.add(BorderLayout.CENTER, canvas);
                frame.setSize(new Dimension(width, height));
                if (x >= 0 && y >= 0) {
                    frame.setLocation(x, y);
                } else {
                    frame.setLocationRelativeTo(null);
                }

                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        Display.getInstance().windowCloseRequested(windowId);
                    }

                    @Override
                    public void windowActivated(WindowEvent e) {
                        Display.getInstance().windowFocusChanged(windowId, true);
                    }

                    @Override
                    public void windowDeactivated(WindowEvent e) {
                        Display.getInstance().windowFocusChanged(windowId, false);
                    }

                    @Override
                    public void windowIconified(WindowEvent e) {
                        Display.getInstance().windowHideNotify(windowId);
                    }

                    @Override
                    public void windowDeiconified(WindowEvent e) {
                        Display.getInstance().windowShowNotify(windowId);
                    }
                });
                frame.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        Display.getInstance().windowSizeChanged(windowId,
                                scaled(p, p.canvas.getWidth()), scaled(p, p.canvas.getHeight()));
                    }

                    @Override
                    public void componentMoved(ComponentEvent e) {
                        // A move can carry the window onto a different display, and a
                        // different display can mean a different backing scale, which
                        // invalidates every preferred size computed at the old one.
                        int now = monitorIndexOf(p);
                        if (now != p.monitorIndex) {
                            p.monitorIndex = now;
                            Display.getInstance().windowMonitorChanged(windowId);
                        }
                    }
                });

                p.frame = frame;
                p.canvas = canvas;
                p.monitorIndex = monitorIndexOf(p);
            }
        });
        synchronized (peers) {
            peers.add(p);
        }
        return p;
    }

    @Override
    public void show(Object peerObj) {
        final Peer p = peer(peerObj);
        if (p == null) {
            return;
        }
        runOnAwtAndWait(new Runnable() {
            @Override
            public void run() {
                p.frame.setVisible(true);
                p.canvas.requestFocus();
            }
        });
    }

    @Override
    public void hide(Object peerObj) {
        final Peer p = peer(peerObj);
        if (p == null) {
            return;
        }
        runOnAwt(new Runnable() {
            @Override
            public void run() {
                p.frame.setVisible(false);
            }
        });
    }

    @Override
    public void dispose(Object peerObj) {
        final Peer p = peer(peerObj);
        if (p == null) {
            return;
        }
        synchronized (peers) {
            peers.remove(p);
        }
        runOnAwt(new Runnable() {
            @Override
            public void run() {
                p.frame.dispose();
            }
        });
    }

    // ---- attributes ------------------------------------------------------------

    @Override
    public void setTitle(Object peerObj, final String title) {
        final Peer p = peer(peerObj);
        if (p != null) {
            runOnAwt(new Runnable() {
                @Override
                public void run() {
                    p.frame.setTitle(title == null ? "" : title);
                }
            });
        }
    }

    @Override
    public void setBounds(Object peerObj, final int x, final int y, final int width, final int height) {
        final Peer p = peer(peerObj);
        if (p != null) {
            runOnAwt(new Runnable() {
                @Override
                public void run() {
                    p.frame.setBounds(x, y, width, height);
                }
            });
        }
    }

    @Override
    public int[] getBounds(Object peerObj, int[] out) {
        Peer p = peer(peerObj);
        if (p == null || p.frame == null) {
            return out;
        }
        Rectangle r = p.frame.getBounds();
        out[0] = r.x;
        out[1] = r.y;
        out[2] = r.width;
        out[3] = r.height;
        return out;
    }

    @Override
    public int getWidth(Object peerObj) {
        Peer p = peer(peerObj);
        if (p == null || p.canvas == null) {
            return 0;
        }
        return Math.max(1, scaled(p, p.canvas.getWidth()));
    }

    @Override
    public int getHeight(Object peerObj) {
        Peer p = peer(peerObj);
        if (p == null || p.canvas == null) {
            return 0;
        }
        return Math.max(1, scaled(p, p.canvas.getHeight()));
    }

    @Override
    public void setResizable(Object peerObj, final boolean resizable) {
        final Peer p = peer(peerObj);
        if (p != null) {
            runOnAwt(new Runnable() {
                @Override
                public void run() {
                    p.frame.setResizable(resizable);
                }
            });
        }
    }

    @Override
    public void setDecorated(Object peerObj, final boolean decorated) {
        final Peer p = peer(peerObj);
        if (p == null) {
            return;
        }
        runOnAwt(new Runnable() {
            @Override
            public void run() {
                // Swing only allows this while the frame is not displayable.
                boolean wasVisible = p.frame.isVisible();
                if (wasVisible) {
                    p.frame.setVisible(false);
                }
                p.frame.dispose();
                p.frame.setUndecorated(!decorated);
                if (wasVisible) {
                    p.frame.setVisible(true);
                }
            }
        });
    }

    @Override
    public void setAlwaysOnTop(Object peerObj, final boolean alwaysOnTop) {
        final Peer p = peer(peerObj);
        if (p != null) {
            runOnAwt(new Runnable() {
                @Override
                public void run() {
                    p.frame.setAlwaysOnTop(alwaysOnTop);
                }
            });
        }
    }

    @Override
    public void setModal(Object peerObj, final boolean modal) {
        // Codename One blocks the input itself, so nothing is required here for
        // correctness. Floating a modal window keeps the platform's own stacking
        // consistent with that.
        setAlwaysOnTop(peerObj, modal);
    }

    @Override
    public void setIcon(Object peerObj, final com.codename1.ui.Image icon) {
        final Peer p = peer(peerObj);
        if (p == null || icon == null) {
            return;
        }
        final Object nativeImage = icon.getImage();
        if (!(nativeImage instanceof java.awt.Image)) {
            return;
        }
        runOnAwt(new Runnable() {
            @Override
            public void run() {
                p.frame.setIconImage((java.awt.Image) nativeImage);
            }
        });
    }

    @Override
    public void requestFocus(Object peerObj) {
        final Peer p = peer(peerObj);
        if (p != null) {
            runOnAwt(new Runnable() {
                @Override
                public void run() {
                    p.frame.toFront();
                    p.frame.requestFocus();
                    p.canvas.requestFocus();
                }
            });
        }
    }

    @Override
    public void minimize(Object peerObj) {
        final Peer p = peer(peerObj);
        if (p != null) {
            runOnAwt(new Runnable() {
                @Override
                public void run() {
                    p.frame.setState(JFrame.ICONIFIED);
                }
            });
        }
    }

    @Override
    public void restore(Object peerObj) {
        final Peer p = peer(peerObj);
        if (p != null) {
            runOnAwt(new Runnable() {
                @Override
                public void run() {
                    p.frame.setState(JFrame.NORMAL);
                }
            });
        }
    }

    @Override
    public void toggleMaximize(Object peerObj) {
        final Peer p = peer(peerObj);
        if (p != null) {
            runOnAwt(new Runnable() {
                @Override
                public void run() {
                    if ((p.frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH) {
                        p.frame.setExtendedState(JFrame.NORMAL);
                    } else {
                        p.frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    }
                }
            });
        }
    }

    // ---- rendering ----------------------------------------------------------------

    @Override
    public Object getNativeGraphics(Object peerObj) {
        Peer p = peer(peerObj);
        if (p == null) {
            return null;
        }
        return port.getNativeGraphics(p.canvas);
    }

    @Override
    public void flushGraphics(Object peerObj, int x, int y, int width, int height) {
        Peer p = peer(peerObj);
        if (p != null && p.canvas != null) {
            p.canvas.blit();
        }
    }

    @Override
    public Object capture(Object peerObj) {
        Peer p = peer(peerObj);
        if (p == null || p.canvas == null) {
            return null;
        }
        return p.canvas.captureBuffer();
    }

    // ---- monitors ---------------------------------------------------------------------

    private static GraphicsDevice[] devices() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
    }

    @Override
    public int getMonitorCount() {
        try {
            return devices().length;
        } catch (Throwable err) {
            return 1;
        }
    }

    @Override
    public int[] getMonitorBounds(int monitor, int[] out) {
        GraphicsDevice d = device(monitor);
        Rectangle r = d == null ? new Rectangle(0, 0, 0, 0)
                : d.getDefaultConfiguration().getBounds();
        out[0] = r.x;
        out[1] = r.y;
        out[2] = r.width;
        out[3] = r.height;
        return out;
    }

    @Override
    public int[] getMonitorWorkArea(int monitor, int[] out) {
        GraphicsDevice d = device(monitor);
        if (d == null) {
            return getMonitorBounds(monitor, out);
        }
        GraphicsConfiguration cfg = d.getDefaultConfiguration();
        Rectangle r = cfg.getBounds();
        Insets in = Toolkit.getDefaultToolkit().getScreenInsets(cfg);
        out[0] = r.x + in.left;
        out[1] = r.y + in.top;
        out[2] = r.width - in.left - in.right;
        out[3] = r.height - in.top - in.bottom;
        return out;
    }

    @Override
    public int getMonitorDensity(int monitor) {
        int dpi = getMonitorDotsPerInch(monitor);
        if (dpi >= 280) {
            return Display.DENSITY_VERY_HIGH;
        }
        if (dpi >= 200) {
            return Display.DENSITY_HIGH;
        }
        if (dpi >= 140) {
            return Display.DENSITY_MEDIUM;
        }
        return Display.DENSITY_LOW;
    }

    @Override
    public double getMonitorScale(int monitor) {
        GraphicsDevice d = device(monitor);
        if (d == null) {
            return 1.0;
        }
        AffineTransform t = d.getDefaultConfiguration().getDefaultTransform();
        return t.getScaleX();
    }

    @Override
    public int getMonitorDotsPerInch(int monitor) {
        try {
            return (int) Math.round(Toolkit.getDefaultToolkit().getScreenResolution()
                    * getMonitorScale(monitor));
        } catch (Throwable err) {
            return 96;
        }
    }

    @Override
    public String getMonitorName(int monitor) {
        GraphicsDevice d = device(monitor);
        return d == null ? "unknown" : d.getIDstring();
    }

    @Override
    public int getPrimaryMonitor() {
        try {
            GraphicsDevice primary =
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            GraphicsDevice[] all = devices();
            for (int iter = 0; iter < all.length; iter++) {
                if (all[iter] == primary) {
                    return iter;
                }
            }
        } catch (Throwable err) {
            // fall through to zero
        }
        return 0;
    }

    @Override
    public int getMonitorForWindow(Object peerObj) {
        Peer p = peer(peerObj);
        if (p == null) {
            return getPrimaryMonitor();
        }
        return monitorIndexOf(p);
    }

    private static GraphicsDevice device(int monitor) {
        try {
            GraphicsDevice[] all = devices();
            if (monitor >= 0 && monitor < all.length) {
                return all[monitor];
            }
            if (all.length > 0) {
                return all[0];
            }
        } catch (Throwable err) {
            // headless
        }
        return null;
    }

    private int monitorIndexOf(Peer p) {
        if (p.frame == null) {
            return getPrimaryMonitor();
        }
        try {
            GraphicsConfiguration cfg = p.frame.getGraphicsConfiguration();
            if (cfg != null) {
                GraphicsDevice[] all = devices();
                for (int iter = 0; iter < all.length; iter++) {
                    if (all[iter] == cfg.getDevice()) {
                        return iter;
                    }
                }
            }
        } catch (Throwable err) {
            // fall through
        }
        return getPrimaryMonitor();
    }

    /**
     * Converts an AWT coordinate to the device pixels Codename One lays out in, using
     * the backing scale of the display this window is actually on rather than a
     * single global scale.
     */
    private int scaled(Peer p, int value) {
        return (int) Math.round(value * getMonitorScale(monitorIndexOf(p)));
    }

    // ---- threading -----------------------------------------------------------------

    private static void runOnAwt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    private static void runOnAwtAndWait(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(r);
        } catch (Exception err) {
            com.codename1.io.Log.e(err);
        }
    }
}
