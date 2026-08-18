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
        watchMonitorTopology();
    }

    /**
     * Reports monitors being attached, removed or reconfigured.
     *
     * <p>AWT has no notification for this -- {@code GraphicsEnvironment} is a
     * snapshot -- so the device set is sampled on a daemon timer and the framework is
     * told only when it actually changes. Without this the documented
     * {@code Desktop.addMonitorListener()} never fires and windows keep stale
     * per-monitor scale after a display is unplugged.</p>
     */
    private void watchMonitorTopology() {
        final java.util.Timer timer = new java.util.Timer("cn1-monitor-watch", true);
        timer.schedule(new java.util.TimerTask() {
            private String last = topologySignature();

            @Override
            public void run() {
                String now = topologySignature();
                if (!now.equals(last)) {
                    last = now;
                    Display.getInstance().monitorsChanged();
                }
            }
        }, MONITOR_POLL_MS, MONITOR_POLL_MS);
    }

    /** Cheap fingerprint of the attached displays: count, bounds and scale. */
    private static String topologySignature() {
        StringBuilder sb = new StringBuilder();
        try {
            for (GraphicsDevice device : devices()) {
                GraphicsConfiguration cfg = device.getDefaultConfiguration();
                Rectangle b = cfg.getBounds();
                AffineTransform tx = cfg.getDefaultTransform();
                sb.append(device.getIDstring()).append(':')
                        .append(b.x).append(',').append(b.y).append(',')
                        .append(b.width).append('x').append(b.height).append('@')
                        .append(tx.getScaleX()).append(';');
            }
        } catch (Throwable err) {
            // A display being reconfigured mid-query throws in AWT; the next tick sees
            // the settled state, so a failed sample is not worth reporting.
            return "unavailable";
        }
        return sb.toString();
    }

    /** Two seconds is imperceptible for a display change and costs nothing. */
    private static final int MONITOR_POLL_MS = 2000;

    /**
     * One native window: its frame, the canvas Codename One paints into, and the id
     * the framework tags this window's input events with.
     */
    static final class Peer {
        /**
         * Typed as the AWT base class rather than JFrame because an owned window has
         * to be a JDialog: Swing expresses ownership through the owner passed at
         * construction, and JFrame has no owned form. Everything here works against
         * java.awt.Window; the few Frame-only operations go through {@link #asFrame()}.
         */
        java.awt.Window frame;
        JavaSEPort.C canvas;
        int windowId;
        int monitorIndex;
        /// The application's own always-on-top setting, kept apart from the temporary
        /// elevation a modal window gets so releasing modality cannot clear it.
        boolean explicitAlwaysOnTop;
        boolean modalElevated;

        /**
         * The frame operations that only exist on a top level window. An owned window
         * is a dialog and answers null, which is also the right behaviour: a dialog is
         * iconified and restored with its owner rather than on its own.
         */
        java.awt.Frame asFrame() {
            return frame instanceof java.awt.Frame ? (java.awt.Frame) frame : null;
        }
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
            final Object parentPeer, final boolean positionSet,
            final boolean ownedByMainWindow) {
        final Peer p = new Peer();
        p.windowId = windowId;
        runOnAwtAndWait(new Runnable() {
            @Override
            public void run() {
                // An owned window stays above its owner and is iconified with it,
                // which is what setOwnerWindow() promises. Swing expresses that through
                // the owner passed at construction, and JFrame has no owned form, so an
                // owned window is a JDialog. Both are java.awt.Window subclasses and
                // everything below only uses that surface -- except the JFrame typed
                // field, which keeps its meaning for the unowned case.
                // An owned window stays above its owner and is iconified with it,
                // which is what setOwnerWindow() promises. Swing establishes that only
                // through the owner passed at construction, and JFrame has no owned
                // form, so an owned window is a JDialog.
                // An owner with no peer is the application's main window, which is
                // the port's own frame rather than one of ours.
                Peer owner = peer(parentPeer);
                java.awt.Window ownerWindow = owner != null ? owner.frame
                        : (ownedByMainWindow ? port.findTopFrame() : null);
                java.awt.Window frame;
                if (ownerWindow != null) {
                    javax.swing.JDialog dlg =
                            new javax.swing.JDialog(ownerWindow, title == null ? "" : title);
                    dlg.setDefaultCloseOperation(javax.swing.JDialog.DO_NOTHING_ON_CLOSE);
                    dlg.setUndecorated(!decorated);
                    dlg.setResizable(resizable);
                    frame = dlg;
                } else {
                    JFrame f = new JFrame(title == null ? "" : title);
                    f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    f.setUndecorated(!decorated);
                    f.setResizable(resizable);
                    frame = f;
                }
                frame.setLayout(new BorderLayout());

                JavaSEPort.C canvas = port.createWindowCanvas(windowId);
                frame.add(BorderLayout.CENTER, canvas);
                frame.setSize(new Dimension(width, height));
                if (positionSet) {
                    // Applied whatever the sign: a monitor left of or above the
                    // primary display has a negative origin, and a window restored
                    // onto it must not be re-centred on the primary one.
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
                        Display.getInstance().windowMoved(windowId);
                        // A move can also carry the window onto a different display,
                        // and a different display can mean a different backing scale,
                        // which invalidates every preferred size computed at the old
                        // one.
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
                    String text = title == null ? "" : title;
                    if (p.frame instanceof java.awt.Frame) {
                        ((java.awt.Frame) p.frame).setTitle(text);
                    } else if (p.frame instanceof java.awt.Dialog) {
                        ((java.awt.Dialog) p.frame).setTitle(text);
                    }
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
                    if (p.frame instanceof java.awt.Frame) {
                        ((java.awt.Frame) p.frame).setResizable(resizable);
                    } else if (p.frame instanceof java.awt.Dialog) {
                        ((java.awt.Dialog) p.frame).setResizable(resizable);
                    }
                }
            });
        }
    }

    @Override
    public void setMinimumSize(Object peerObj, final int width, final int height) {
        final Peer p = peer(peerObj);
        if (p != null) {
            runOnAwt(new Runnable() {
                @Override
                public void run() {
                    if (width > 0 && height > 0) {
                        p.frame.setMinimumSize(new Dimension(width, height));
                    } else {
                        p.frame.setMinimumSize(null);
                    }
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
                // Disposing an AWT window disposes everything it owns, so a window
                // with open child windows would take them down with it and only put
                // itself back. The children stayed registered and visible as far as
                // the framework knew, painting into a hierarchy that was no longer
                // displayable. Remembered here and re-shown below; setVisible(true)
                // recreates the native peer a dispose destroyed.
                java.awt.Window[] owned = p.frame.getOwnedWindows();
                java.util.List<java.awt.Window> wereVisible =
                        new java.util.ArrayList<java.awt.Window>();
                for (java.awt.Window each : owned) {
                    if (each.isVisible()) {
                        wereVisible.add(each);
                    }
                }
                if (wasVisible) {
                    p.frame.setVisible(false);
                }
                p.frame.dispose();
                if (p.frame instanceof java.awt.Frame) {
                    ((java.awt.Frame) p.frame).setUndecorated(!decorated);
                } else if (p.frame instanceof java.awt.Dialog) {
                    ((java.awt.Dialog) p.frame).setUndecorated(!decorated);
                }
                if (wasVisible) {
                    p.frame.setVisible(true);
                }
                // After the owner, so a child is never briefly parented to a window
                // that is not on screen.
                for (java.awt.Window each : wereVisible) {
                    each.setVisible(true);
                }
            }
        });
    }

    @Override
    public void setAlwaysOnTop(Object peerObj, final boolean alwaysOnTop) {
        final Peer p = peer(peerObj);
        if (p != null) {
            p.explicitAlwaysOnTop = alwaysOnTop;
            applyAlwaysOnTop(p);
        }
    }

    @Override
    public void setModal(Object peerObj, final boolean modal, boolean applicationWide,
            Object ownerPeer) {
        // Elevation only. Which windows are actually blocked is decided by the
        // framework and delivered through setInputEnabled/setMainWindowInputEnabled,
        // because that answer depends on the whole modal stack rather than on this
        // one call.
        final Peer p = peer(peerObj);
        if (p == null) {
            return;
        }
        p.modalElevated = modal;
        applyAlwaysOnTop(p);
    }

    @Override
    public void setInputEnabled(Object peerObj, final boolean enabled) {
        final Peer p = peer(peerObj);
        if (p != null) {
            runOnAwt(new Runnable() {
                @Override
                public void run() {
                    p.frame.setEnabled(enabled);
                }
            });
        }
    }

    @Override
    public void setMainWindowInputEnabled(final boolean enabled) {
        runOnAwt(new Runnable() {
            @Override
            public void run() {
                java.awt.Window main = port.findTopFrame();
                if (main != null) {
                    main.setEnabled(enabled);
                }
            }
        });
    }

    /// Floats the frame when the application asked for it or while it is modal.
    private void applyAlwaysOnTop(final Peer p) {
        runOnAwt(new Runnable() {
            @Override
            public void run() {
                p.frame.setAlwaysOnTop(p.explicitAlwaysOnTop || p.modalElevated);
            }
        });
    }

    @Override
    public void setUtilityWindow(Object peerObj, final boolean utility) {
        final Peer p = peer(peerObj);
        if (p != null) {
            runOnAwt(new Runnable() {
                @Override
                public void run() {
                    // UTILITY is the Swing window type that keeps a palette out of the
                    // task bar and gives it lighter chrome. It can only be set while the
                    // frame is undisplayable, so an already shown window is left alone
                    // rather than being flickered through a dispose/recreate cycle.
                    if (!p.frame.isDisplayable()) {
                        p.frame.setType(utility
                                ? java.awt.Window.Type.UTILITY
                                : java.awt.Window.Type.NORMAL);
                    }
                }
            });
        }
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
                // Only a top level window carries an icon; an owned dialog shows its
                // owner's.
                if (p.asFrame() != null) {
                    p.asFrame().setIconImage((java.awt.Image) nativeImage);
                }
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
                    // An owned dialog has no independent iconified state; it minimizes
                    // with its owner, which is the platform's own behaviour.
                    if (p.asFrame() != null) {
                        p.asFrame().setState(java.awt.Frame.ICONIFIED);
                    }
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
                    if (p.asFrame() != null) {
                        p.asFrame().setState(java.awt.Frame.NORMAL);
                    }
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
                    if (p.asFrame() == null) {
                        return;
                    }
                    if ((p.asFrame().getExtendedState() & java.awt.Frame.MAXIMIZED_BOTH)
                            == java.awt.Frame.MAXIMIZED_BOTH) {
                        p.asFrame().setExtendedState(java.awt.Frame.NORMAL);
                    } else {
                        p.asFrame().setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
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

    @Override
    public int getMonitorForMainWindow() {
        // The simulator's own frame, which is the window a Form is displayed in.
        // It moves between displays like any other, so reporting the primary
        // monitor for it was wrong the moment the user dragged it.
        java.awt.Window main = port.findTopFrame();
        if (main == null) {
            return getPrimaryMonitor();
        }
        return monitorIndexOfWindow(main);
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
        return monitorIndexOfWindow(p.frame);
    }

    /// Which display the given AWT window is on, shared by the secondary windows
    /// and the application's main frame.
    private int monitorIndexOfWindow(java.awt.Window frame) {
        try {
            GraphicsConfiguration cfg = frame.getGraphicsConfiguration();
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
