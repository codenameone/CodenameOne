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

import com.codename1.ui.Desktop;
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
    /** Returned by {@link #topologySignature()} when the sample itself failed. */
    private static final String TOPOLOGY_UNAVAILABLE = "unavailable";

    /// The topology poller, kept so a port restart can stop it. A simulator session
    /// that cycles through Display.deinitialize()/init() builds a new window manager
    /// each time, and every previous poller went on waking every two seconds for the
    /// life of the process -- each of them reporting the same topology change.
    private java.util.Timer monitorWatch;

    /// Stops the topology poller. Called when the port is torn down.
    void stopWatchingMonitorTopology() {
        java.util.Timer timer = monitorWatch;
        monitorWatch = null;
        if (timer != null) {
            timer.cancel();
        }
    }

    private void watchMonitorTopology() {
        final java.util.Timer timer = new java.util.Timer("cn1-monitor-watch", true);
        monitorWatch = timer;
        timer.schedule(new java.util.TimerTask() {
            private String last = topologySignature();

            @Override
            public void run() {
                String now = topologySignature();
                if (TOPOLOGY_UNAVAILABLE.equals(now)) {
                    // A failed sample is not a topology change, and the comment on
                    // that branch already said so. Recording it fired a notification
                    // for the failure and a second one when the next sample
                    // succeeded, and the first pass could rebuild monitor data from
                    // fallbacks and relayout every window against them.
                    return;
                }
                if (!now.equals(last)) {
                    last = now;
                    Desktop.getInstance().monitorsChanged();
                }
            }
        }, MONITOR_POLL_MS, MONITOR_POLL_MS);
    }

    /**
     * Cheap fingerprint of the attached displays: count, bounds, scale and screen
     * insets.
     *
     * The insets matter as much as the bounds. A taskbar or dock that moves edge,
     * changes size or toggles auto-hide reconfigures the work area while leaving the
     * monitor's bounds and scale identical, so a fingerprint without them never fired
     * monitorsChanged(): windows kept a stale work area and centerOnDesktop() could
     * place one underneath the taskbar that had just appeared.
     */
    private static String topologySignature() {
        StringBuilder sb = new StringBuilder();
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            for (GraphicsDevice device : devices()) {
                GraphicsConfiguration cfg = device.getDefaultConfiguration();
                Rectangle b = cfg.getBounds();
                AffineTransform tx = cfg.getDefaultTransform();
                Insets in = toolkit.getScreenInsets(cfg);
                sb.append(device.getIDstring()).append(':')
                        .append(b.x).append(',').append(b.y).append(',')
                        .append(b.width).append('x').append(b.height).append('@')
                        .append(tx.getScaleX()).append('/')
                        .append(in.top).append(',').append(in.left).append(',')
                        .append(in.bottom).append(',').append(in.right).append(';');
            }
        } catch (Throwable err) {
            // A display being reconfigured mid-query throws in AWT; the next tick sees
            // the settled state, so a failed sample is not worth reporting.
            return TOPOLOGY_UNAVAILABLE;
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
        /**
         * True while the AWT peer is being torn down and rebuilt to apply a chrome
         * change. That cycle calls setVisible(false) and setVisible(true) on a window
         * the framework still considers shown, so the component listener would report
         * it as a minimize and a restore -- firing Minimized and Restored events and
         * cancelling pending input for the window and its owned children. Only ever
         * touched on the AWT thread, which is where both the cycle and the callbacks
         * run.
         */
        boolean reconfiguring;

        /// Visibility events AWT is about to deliver for a show() or hide() this
        /// manager asked for, rather than for something the user or window manager did.
        ///
        /// Only ever touched on the AWT thread, which is also where the events arrive,
        /// so the increment always precedes the delivery it accounts for.
        int selfInflictedVisibilityEvents;
        /// The application's own always-on-top setting, kept apart from the temporary
        /// elevation a modal window gets so releasing modality cannot clear it.
        boolean explicitAlwaysOnTop;
        boolean modalElevated;
        /// The requested minimum, in the device pixels Codename One lays out in. AWT
        /// wants logical units, and the two differ by the backing scale of whatever
        /// monitor the window is on -- which changes when the user drags it to another
        /// display -- so the request is kept in its original units and re-converted
        /// rather than converted once at the point of the call.
        int minWidth;
        int minHeight;

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
                        Desktop.getInstance().windowCloseRequested(windowId);
                    }

                    @Override
                    public void windowActivated(WindowEvent e) {
                        Desktop.getInstance().windowFocusChanged(windowId, true);
                    }

                    @Override
                    public void windowDeactivated(WindowEvent e) {
                        Desktop.getInstance().windowFocusChanged(windowId, false);
                    }

                    @Override
                    public void windowIconified(WindowEvent e) {
                        Desktop.getInstance().windowHideNotify(windowId);
                    }

                    @Override
                    public void windowDeiconified(WindowEvent e) {
                        Desktop.getInstance().windowShowNotify(windowId);
                    }
                });
                frame.addComponentListener(new ComponentAdapter() {
                    /**
                     * AWT hides a window's owned dialogs along with it and shows them
                     * again with it, without any window event of its own. Nothing told
                     * the framework, so an owned window kept nativeVisible true with no
                     * native surface behind it: isWindowShowing() went on reporting it,
                     * and it went on painting and animating, which also keeps the event
                     * dispatch thread awake.
                     *
                     * <p>Safe for the explicit path too. Window.hide() and show()
                     * clear or set nativeVisible before calling this manager, so the
                     * notification they trigger here finds the state already correct
                     * and does nothing.</p>
                     */
                    @Override
                    public void componentHidden(ComponentEvent e) {
                        if (p.reconfiguring || consumeSelfInflicted(p)) {
                            return;
                        }
                        Desktop.getInstance().windowHideNotify(windowId);
                    }

                    @Override
                    public void componentShown(ComponentEvent e) {
                        if (p.reconfiguring || consumeSelfInflicted(p)) {
                            return;
                        }
                        Desktop.getInstance().windowShowNotify(windowId);
                    }

                    @Override
                    public void componentResized(ComponentEvent e) {
                        Desktop.getInstance().windowSizeChanged(windowId,
                                scaled(p, p.canvas.getWidth()), scaled(p, p.canvas.getHeight()));
                    }

                    @Override
                    public void componentMoved(ComponentEvent e) {
                        Desktop.getInstance().windowMoved(windowId);
                        // A move can also carry the window onto a different display,
                        // and a different display can mean a different backing scale,
                        // which invalidates every preferred size computed at the old
                        // one.
                        int now = monitorIndexOf(p);
                        if (now != p.monitorIndex) {
                            p.monitorIndex = now;
                            // The minimum is held in Codename One pixels, so the AWT
                            // constraint means something different on a display with
                            // another backing scale and has to be re-converted.
                            applyMinimumSize(p);
                            Desktop.getInstance().windowMonitorChanged(windowId);
                            // The Swing editor is placed by dividing by the canvas's
                            // backing scale, so a move to a display with another one
                            // leaves it offset and mis-sized over its field. The
                            // hierarchy is re-laid out for the new scale; nothing
                            // moved the editor.
                            port.reapplyEditorBounds(windowId);
                            // Native peers divide by the same scale and cache the
                            // result, and a move often leaves the Codename One bounds
                            // untouched, so nothing else would ask them to re-place.
                            port.reapplyPeerBounds(windowId);
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

    /// Whether this AWT visibility event was caused by show() or hide() here, rather
    /// than by the user or the window manager.
    ///
    /// The framework already knows about its own show and hide -- Window sets
    /// nativeVisible before calling this manager -- so reporting them again is at best
    /// redundant. It is not merely redundant, though, because the report is queued onto
    /// the Codename One event dispatch thread rather than delivered inline: a show and
    /// a hide in the same turn both queue, and both then run against the state the
    /// second one left. The pair is read as a minimize and a restore, and in the
    /// show-then-hide order the window ends up hidden but marked iconified -- which is
    /// the state showModal() waits on, so its caller waits for good.
    private static boolean consumeSelfInflicted(Peer p) {
        if (p.selfInflictedVisibilityEvents > 0) {
            p.selfInflictedVisibilityEvents--;
            return true;
        }
        return false;
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
                // Counted only when the frame is actually changing state, because AWT
                // delivers nothing when it is not and the count would then be spent on
                // some later event that the user caused.
                if (!p.frame.isVisible()) {
                    p.selfInflictedVisibilityEvents++;
                }
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
        // Waits, exactly as show() does. Queued, a hide followed by a show in the same
        // EDT turn ran after the show had already put nativeVisible back: the frame's
        // componentHidden then arrived with the window visible and was reported as a
        // minimize, and the show's componentShown as a restore -- a spurious
        // Minimized/Restored pair after the real Hidden/Shown, with minimize listeners
        // firing for a window that is on screen.
        runOnAwtAndWait(new Runnable() {
            @Override
            public void run() {
                if (p.frame.isVisible()) {
                    p.selfInflictedVisibilityEvents++;
                }
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
                // Before the frame goes: the canvas registers an AWTEventListener on
                // the global Toolkit for the magnification wheel fallback, and the
                // Toolkit holds it for the life of the VM. Disposing only the frame
                // left that listener retaining the canvas and its whole hierarchy,
                // and inspecting every wheel event in the application, once per
                // window ever opened.
                if (p.canvas != null) {
                    p.canvas.disposeGestureListeners();
                    // The screen graphics registry keys a Graphics2D to its canvas
                    // strongly, so disposing only the frame left the canvas and its
                    // buffers reachable for the life of the application.
                    p.canvas.releaseScreenGraphics();
                }
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

    /**
     * Applied on the AWT thread <em>before returning</em>, not queued.
     *
     * <p>Geometry is read back synchronously: {@code setWindowSize()} followed by
     * {@code centerOnDesktop()} or {@code centerOn()} in one Codename One event
     * dispatch turn has the centring read {@link #getBounds}, work out an origin from
     * it and write the whole rectangle back. Queued, the read saw the frame's old
     * dimensions, so the second write carried the old size and the later AWT task
     * undid the resize -- the resize silently did nothing.
     *
     * <p>Waiting here follows what {@code createWindow} and {@code show} already do
     * in this class for the same reason.
     */
    @Override
    public void setBounds(Object peerObj, final int x, final int y, final int width, final int height) {
        final Peer p = peer(peerObj);
        if (p != null) {
            runOnAwtAndWait(new Runnable() {
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
    public void setMinimumSize(Object peerObj, int width, int height) {
        Peer p = peer(peerObj);
        if (p != null) {
            p.minWidth = width;
            p.minHeight = height;
            applyMinimumSize(p);
        }
    }

    /**
     * Pushes the stored minimum to AWT in AWT's own units.
     *
     * The SPI supplies the minimum in the device pixels Codename One lays out in,
     * while {@code java.awt.Window.setMinimumSize} takes logical units -- the same
     * distinction {@link #scaled} applies in the other direction when reporting a
     * window's size. Handing the device value straight to AWT made a requested 320
     * pixel minimum a 640 device pixel floor on a 2x display, and the meaning changed
     * again whenever the window was dragged to a monitor with a different scale, so
     * this is re-applied on a monitor change rather than converted once.
     */
    private void applyMinimumSize(final Peer p) {
        final int w = p.minWidth;
        final int h = p.minHeight;
        if (p.frame == null) {
            return;
        }
        final double scale = getMonitorScale(monitorIndexOf(p));
        runOnAwt(new Runnable() {
            @Override
            public void run() {
                if (w > 0 && h > 0) {
                    double s = scale > 0 ? scale : 1.0;
                    p.frame.setMinimumSize(new Dimension(
                            Math.max(1, (int) Math.round(w / s)),
                            Math.max(1, (int) Math.round(h / s))));
                } else {
                    p.frame.setMinimumSize(null);
                }
            }
        });
    }

    @Override
    public void setDecorated(Object peerObj, final boolean decorated) {
        final Peer p = peer(peerObj);
        if (p == null) {
            return;
        }
        applyWhileUndisplayable(p, new Runnable() {
            @Override
            public void run() {
                if (p.frame instanceof java.awt.Frame) {
                    ((java.awt.Frame) p.frame).setUndecorated(!decorated);
                } else if (p.frame instanceof java.awt.Dialog) {
                    ((java.awt.Dialog) p.frame).setUndecorated(!decorated);
                }
            }
        });
    }

    /**
     * Runs a change that Swing only permits while a window is undisplayable, taking
     * the frame down and putting it back around it.
     *
     * Disposing an AWT window disposes everything it owns, so a window with open child
     * windows would take them down with it and only put itself back. The children
     * stayed registered and visible as far as the framework knew, painting into a
     * hierarchy that was no longer displayable. They are remembered here and re-shown
     * after the owner, so a child is never briefly parented to a window that is not on
     * screen.
     *
     * Shared by the decoration and utility-window setters. The utility setter used to
     * skip the change outright once the window was showing, which left the platform on
     * the old taskbar behaviour while {@code Window.isUtilityWindow()} reported the
     * requested value -- a setter that silently did nothing.
     */
    /// Every window owned by the given one, at any depth.
    ///
    /// AWT's hide and show of owned windows is recursive, so anything less than the
    /// full tree leaves descendants observing transitions that are an implementation
    /// detail of a chrome change.
    private static void collectOwnedWindows(java.awt.Window root,
            java.util.List<java.awt.Window> out) {
        for (java.awt.Window each : root.getOwnedWindows()) {
            out.add(each);
            collectOwnedWindows(each, out);
        }
    }

    /// The peer that owns the given AWT window, or null when it is not one of ours.
    private Peer peerFor(java.awt.Window frame) {
        synchronized (peers) {
            for (Peer each : peers) {
                if (each.frame == frame) { //NOPMD CompareObjectsWithEquals
                    return each;
                }
            }
        }
        return null;
    }

    private void applyWhileUndisplayable(final Peer p, final Runnable change) {
        runOnAwt(new Runnable() {
            @Override
            public void run() {
                // The hide and show below are an implementation detail of applying the
                // change, not a visibility transition the framework should hear about.
                p.reconfiguring = true;
                java.util.List<Peer> childrenSuppressed = new java.util.ArrayList<Peer>();
                try {
                boolean wasVisible = p.frame.isVisible();
                // The whole owned tree, not just the direct children. AWT hides and
                // shows every descendant recursively, so a visible grandchild takes
                // the same implicit hide and explicit reshow -- and collecting only
                // getOwnedWindows() left it unsuppressed and reporting the spurious
                // pair, which is the same mistake one level down.
                java.util.List<java.awt.Window> owned =
                        new java.util.ArrayList<java.awt.Window>();
                collectOwnedWindows(p.frame, owned);
                java.util.List<java.awt.Window> wereVisible =
                        new java.util.ArrayList<java.awt.Window>();
                for (java.awt.Window each : owned) {
                    if (each.isVisible()) {
                        wereVisible.add(each);
                        // The owner's hide takes its visible children down with it and
                        // they are put back explicitly below, so their listeners see
                        // the same spurious pair the owner's did. Marking only the
                        // owner left every owned window reporting a minimize and a
                        // restore, and cancelling its pending input.
                        Peer child = peerFor(each);
                        if (child != null) {
                            child.reconfiguring = true;
                            childrenSuppressed.add(child);
                        }
                    }
                }
                if (wasVisible) {
                    p.frame.setVisible(false);
                }
                p.frame.dispose();
                change.run();
                if (wasVisible) {
                    // setVisible(true) recreates the native peer the dispose destroyed.
                    p.frame.setVisible(true);
                }
                for (java.awt.Window each : wereVisible) {
                    each.setVisible(true);
                }
                } finally {
                    p.reconfiguring = false;
                    for (Peer child : childrenSuppressed) {
                        child.reconfiguring = false;
                    }
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
            // UTILITY is the Swing window type that keeps a palette out of the task
            // bar and gives it lighter chrome. Swing only allows the type to change
            // while the frame is undisplayable, so a window that is already up is taken
            // down and put back rather than being left on the old behaviour -- which is
            // what it used to do, silently, while isUtilityWindow() reported otherwise.
            applyWhileUndisplayable(p, new Runnable() {
                @Override
                public void run() {
                    p.frame.setType(utility
                            ? java.awt.Window.Type.UTILITY
                            : java.awt.Window.Type.NORMAL);
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

    /**
     * Installs the window's commands as a native menu bar on its own frame.
     *
     * The main window builds its menu the same way through
     * {@code JavaSEPort.setNativeCommands}; this is the per-window counterpart, so a
     * command added to a Window is displayed and activated rather than only recorded.
     * Gated on desktop native chrome mode for the same reason the main window is: in
     * skin mode there is no native frame to hang a menu on.
     */
    @Override
    public void setCommands(Object peerObj, final com.codename1.ui.Command[] commands) {
        final Peer p = peer(peerObj);
        if (p == null || !port.isDesktopNativeChromeMode()) {
            return;
        }
        // Activation is routed back through this window so its command listeners see it.
        final com.codename1.ui.Window owner =
                com.codename1.ui.Desktop.getInstance().windowById(p.windowId);
        final java.util.ArrayList<com.codename1.ui.Command> named =
                new java.util.ArrayList<com.codename1.ui.Command>();
        if (commands != null) {
            for (com.codename1.ui.Command c : commands) {
                String name = c == null ? null : c.getCommandName();
                if (name != null && name.length() > 0) {
                    // Icon-only commands have nothing to label a menu item with.
                    named.add(c);
                }
            }
        }
        runOnAwt(new Runnable() {
            @Override
            public void run() {
                javax.swing.JMenuBar bar = named.isEmpty()
                        ? null : port.buildWindowMenuBar(named, owner);
                if (p.frame instanceof javax.swing.JFrame) {
                    ((javax.swing.JFrame) p.frame).setJMenuBar(bar);
                } else if (p.frame instanceof javax.swing.JDialog) {
                    ((javax.swing.JDialog) p.frame).setJMenuBar(bar);
                } else {
                    return;
                }
                p.frame.revalidate();
            }
        });
    }

    /**
     * The application's main frame in desktop coordinates.
     *
     * findTopFrame() is deliberately the primary window only -- every other caller of
     * it is a main-window operation -- and that is exactly what is wanted here: a Form
     * lives in that frame, so centring a Window over a Form centres over it.
     */
    @Override
    public int[] getMainWindowBounds(int[] out) {
        java.awt.Window main = port.findTopFrame();
        if (main == null || out == null || out.length < 4) {
            return null;
        }
        Rectangle b = main.getBounds();
        out[0] = b.x;
        out[1] = b.y;
        out[2] = b.width;
        out[3] = b.height;
        return out;
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
