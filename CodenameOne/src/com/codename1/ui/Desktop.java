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
package com.codename1.ui;

import com.codename1.impl.WindowManager;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.util.EventDispatcher;

import java.util.ArrayList;
import java.util.Hashtable;

/// The desktop a windowed application runs on: the monitors attached to it and the
/// `Window` instances open on them.
///
/// This sits alongside `Display` rather than replacing any of it. `Display` answers
/// "how big is the application's main surface", which is the only question a phone
/// has; `Desktop` answers "what screens exist and what windows are open", which only
/// a windowing system can answer.
///
/// Every method degrades safely on a platform with no windowing system:
/// `#getWindows()` returns an empty array, `#getMonitors()` returns a single monitor
/// describing the main display, and `#getFocusedWindow()` returns null. Only
/// constructing a `Window` throws.
///
/// @author Shai Almog
public final class Desktop {

    private static final Desktop INSTANCE = new Desktop();

    /// Dots per inch of the medium-density baseline, used when there is no
    /// implementation to ask.
    private static final int MEDIUM_DENSITY_DPI = 160;
    private static final int[] BOUNDS_SCRATCH = new int[4];

    private final ArrayList<Window> windows = new ArrayList<Window>();
    private final EventDispatcher monitorListeners = new EventDispatcher();
    private final EventDispatcher windowListeners = new EventDispatcher();
    private Window focusedWindow;
    private int nextWindowId = 1;

    private Desktop() {
    }

    /// Returns the singleton instance.
    ///
    /// #### Returns
    ///
    /// the desktop instance
    public static Desktop getInstance() {
        return INSTANCE;
    }

    /// Indicates whether this platform has a windowing system, and therefore whether
    /// `Window` can be used at all.
    ///
    /// #### Returns
    ///
    /// true if additional native windows can be opened
    public static boolean isSupported() {
        return Display.impl != null && Display.impl.getWindowManager() != null;
    }

    private static WindowManager manager() {
        if (Display.impl == null) {
            return null;
        }
        return Display.impl.getWindowManager();
    }

    // ---- monitors --------------------------------------------------------------

    /// Returns every monitor attached to the desktop.
    ///
    /// On a platform with no windowing system this reports a single monitor covering
    /// the main display, so layout code that positions against a monitor works
    /// everywhere.
    ///
    /// #### Returns
    ///
    /// the monitors, never empty and never null
    public Monitor[] getMonitors() {
        WindowManager wm = manager();
        if (wm == null) {
            return new Monitor[]{fallbackMonitor()};
        }
        int count = wm.getMonitorCount();
        if (count <= 0) {
            return new Monitor[]{fallbackMonitor()};
        }
        int primary = wm.getPrimaryMonitor();
        Monitor[] out = new Monitor[count];
        for (int iter = 0; iter < count; iter++) {
            out[iter] = readMonitor(wm, iter, primary);
        }
        return out;
    }

    /// Returns the monitor the platform treats as the origin of the desktop.
    ///
    /// #### Returns
    ///
    /// the primary monitor
    public Monitor getPrimaryMonitor() {
        WindowManager wm = manager();
        if (wm == null) {
            return fallbackMonitor();
        }
        return readMonitor(wm, wm.getPrimaryMonitor(), wm.getPrimaryMonitor());
    }

    /// Returns the monitor containing the given desktop coordinate.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x coordinate in desktop space
    ///
    /// - `y`: the y coordinate in desktop space
    ///
    /// #### Returns
    ///
    /// the monitor containing that point, or the primary monitor when none does
    public Monitor getMonitorAt(int x, int y) {
        Monitor[] all = getMonitors();
        for (Monitor m : all) {
            if (m.getBounds().contains(x, y)) {
                return m;
            }
        }
        return getPrimaryMonitor();
    }

    /// Returns the monitor a top level is currently displayed on.
    ///
    /// #### Parameters
    ///
    /// - `topLevel`: the form or window to locate
    ///
    /// #### Returns
    ///
    /// the monitor it sits on, or the primary monitor when that cannot be determined
    public Monitor getMonitorFor(TopLevelContainer topLevel) {
        WindowManager wm = manager();
        if (wm != null) {
            Container c = topLevel == null ? null : topLevel.asContainer();
            Object peer = c == null ? null : c.topLevelNativePeer();
            if (peer != null) {
                return readMonitor(wm, wm.getMonitorForWindow(peer), wm.getPrimaryMonitor());
            }
            // A window that has not been shown yet has no peer and so no monitor; it
            // falls through to the primary rather than borrowing the main window's.
            if (c != null && !c.isNativeWindow()) {
                // A Form lives in the application's main window, which has no peer to
                // ask about. Reporting the primary monitor was wrong as soon as the
                // application had been dragged to a second display: everything
                // positioned against the main form got another monitor's work area,
                // scale and density.
                return readMonitor(wm, wm.getMonitorForMainWindow(), wm.getPrimaryMonitor());
            }
        }
        return getPrimaryMonitor();
    }

    /// Returns the union of every monitor's bounds.
    ///
    /// #### Returns
    ///
    /// the whole desktop area
    public Rectangle getDesktopBounds() {
        Monitor[] all = getMonitors();
        Rectangle out = all[0].getBounds();
        for (int iter = 1; iter < all.length; iter++) {
            Rectangle b = all[iter].getBounds();
            int x = Math.min(out.getX(), b.getX());
            int y = Math.min(out.getY(), b.getY());
            int right = Math.max(out.getX() + out.getWidth(), b.getX() + b.getWidth());
            int bottom = Math.max(out.getY() + out.getHeight(), b.getY() + b.getHeight());
            out = new Rectangle(x, y, right - x, bottom - y);
        }
        return out;
    }

    private Monitor readMonitor(WindowManager wm, int index, int primary) {
        int[] b;
        int[] w;
        synchronized (BOUNDS_SCRATCH) {
            b = copyOf(wm.getMonitorBounds(index, BOUNDS_SCRATCH));
            w = copyOf(wm.getMonitorWorkArea(index, BOUNDS_SCRATCH));
        }
        return new Monitor(index,
                new Rectangle(b[0], b[1], b[2], b[3]),
                new Rectangle(w[0], w[1], w[2], w[3]),
                wm.getMonitorDensity(index),
                wm.getMonitorScale(index),
                wm.getMonitorDotsPerInch(index),
                wm.getMonitorName(index),
                index == primary);
    }

    private static int[] copyOf(int[] src) {
        if (src == null) {
            return new int[4];
        }
        return new int[]{src[0], src[1], src[2], src[3]};
    }

    private Monitor fallbackMonitor() {
        // Every value here has to survive Display having no implementation yet. This
        // method is the documented answer during startup, so guarding the size and then
        // asking the same missing implementation for the density and the dots per inch
        // -- which is what getDeviceDensity() and convertToPixels() do -- threw exactly
        // when the fallback was supposed to be returned.
        boolean uninitialized = Display.impl == null;
        int w = uninitialized ? 0 : Display.impl.getDisplayWidth();
        int h = uninitialized ? 0 : Display.impl.getDisplayHeight();
        Rectangle r = new Rectangle(0, 0, w, h);
        int density = uninitialized
                ? Display.DENSITY_MEDIUM : Display.getInstance().getDeviceDensity();
        int dotsPerInch = uninitialized
                ? MEDIUM_DENSITY_DPI : Display.getInstance().convertToPixels(254, true) / 10;
        return new Monitor(0, r, new Rectangle(r), density, 1.0, dotsPerInch, "main", true);
    }

    // ---- windows ----------------------------------------------------------------

    /// Returns every window currently open, not counting the application's main form.
    ///
    /// #### Returns
    ///
    /// the open windows, empty when there are none or the platform has no windows
    public Window[] getWindows() {
        synchronized (windows) {
            return windows.toArray(new Window[windows.size()]);
        }
    }

    /// Returns the window that currently holds keyboard focus.
    ///
    /// #### Returns
    ///
    /// the focused window, or null when the main form has focus or none is open
    public Window getFocusedWindow() {
        return focusedWindow;
    }

    /// Adds a listener notified when a monitor is attached, removed or reconfigured.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public void addMonitorListener(ActionListener l) {
        monitorListeners.addListener(l);
        // Touching the manager is what makes a port start watching for display
        // changes; without it a listener registered before anything else looked at
        // the windowing system would never hear about one.
        //
        // Registering a listener before Codename One is initialized is a reasonable
        // thing to do -- it is the point at which an application knows it wants to hear
        // about monitors -- and it used to throw. Guarding alone was not enough either:
        // the ports start watching when their window manager is first created, so a
        // listener registered early and never followed by anything that touches the
        // desktop would simply never hear about a change. Display.init() calls
        // startMonitorWatchingIfListening() once an implementation exists.
        if (Display.impl != null) {
            Display.impl.getWindowManager();
        }
    }

    /// Starts the port watching for display changes if anything is listening for them.
    ///
    /// Called from `Display#init(java.lang.Object)`, because a listener registered
    /// before there was an implementation could not start the watch itself.
    static void startMonitorWatchingIfListening() {
        if (Display.impl != null && INSTANCE.monitorListeners.hasListeners()) {
            Display.impl.getWindowManager();
        }
    }

    /// Removes a previously added monitor listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public void removeMonitorListener(ActionListener l) {
        monitorListeners.removeListener(l);
    }

    /// Adds a listener notified when any window is shown, hidden, moved or resized.
    ///
    /// This is the multi-window counterpart of
    /// `Display#addWindowListener(com.codename1.ui.events.ActionListener)`, which
    /// continues to report only the application's main window.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public void addWindowListener(ActionListener l) {
        windowListeners.addListener(l);
    }

    /// Removes a previously added window listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public void removeWindowListener(ActionListener l) {
        windowListeners.removeListener(l);
    }

    // ---- framework internals -------------------------------------------------------

    /// Hands out the next window id.
    ///
    /// Synchronized on the same monitor as the registry. A window is constructed on
    /// whatever thread calls `new Window(...)` -- a constructor cannot be marshalled,
    /// since it has to return the object -- so two background threads creating
    /// windows at once could take the same id from an unsynchronized post-increment.
    /// Both native windows would then answer to one id, and `#windowById(int)`
    /// returns the first match, so every input and lifecycle callback meant for the
    /// second would have been delivered to the first.
    ///
    /// #### Returns
    ///
    /// an id no other window has been given
    int nextWindowId() {
        synchronized (windows) {
            return nextWindowId++;
        }
    }

    void registerWindow(Window w) {
        synchronized (windows) {
            if (!windows.contains(w)) {
                windows.add(w);
            }
        }
    }

    /// Returns the open windows owned by the given top level. Used by
    /// `Window#dispose()`, which cannot outlive its children -- the platform would
    /// leave them open with nothing behind them.
    Window[] windowsOwnedBy(TopLevelContainer owner) {
        ArrayList<Window> owned = new ArrayList<Window>();
        synchronized (windows) {
            int len = windows.size();
            for (int iter = 0; iter < len; iter++) {
                Window w = windows.get(iter);
                if (w.getOwnerWindow() == owner) { //NOPMD CompareObjectsWithEquals
                    owned.add(w);
                }
            }
        }
        return owned.toArray(new Window[owned.size()]);
    }

    void deregisterWindow(Window w) {
        synchronized (windows) {
            windows.remove(w);
        }
        if (focusedWindow == w) { //NOPMD CompareObjectsWithEquals
            focusedWindow = null;
        }
    }

    void setFocusedWindow(Window w) {
        focusedWindow = w;
    }

    /// Returns the window carrying the given framework assigned id, which is how an
    /// event that arrived off the event dispatch thread is routed back to its tree.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id from `Window#getWindowId()`
    ///
    /// #### Returns
    ///
    /// the matching window, or null when none is open with that id
    public Window windowById(int windowId) {
        synchronized (windows) {
            int len = windows.size();
            for (int iter = 0; iter < len; iter++) {
                Window w = windows.get(iter);
                if (w.getWindowId() == windowId) {
                    return w;
                }
            }
        }
        return null;
    }

    boolean hasOpenWindows() {
        synchronized (windows) {
            return !windows.isEmpty();
        }
    }

    boolean hasVisibleWindows() {
        synchronized (windows) {
            int len = windows.size();
            for (int iter = 0; iter < len; iter++) {
                if (windows.get(iter).isWindowShowing()) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Snapshot used by the paint pass. Callers iterate by index because a nested
    /// event loop can dispose a window mid iteration.
    ArrayList<Window> windowList() {
        return windows;
    }

    void fireMonitorChanged() {
        monitorListeners.fireActionEvent(new ActionEvent(this));
    }

    void fireWindowEvent(ActionEvent evt) {
        windowListeners.fireActionEvent(evt);
    }

    /// Disposes every open window. Invoked as the application shuts down so a window
    /// cannot outlive the event dispatch thread that paints it.
    void disposeAll() {
        Window[] all = getWindows();
        for (Window w : all) {
            w.dispose();
        }
    }
    // ---- modality ----------------------------------------------------------------
    //
    // Modality lives here rather than in Display because it is a question about
    // windows, and the window registry is here. Display asks whether input for a
    // window is blocked; it does not need to know what a modal is.

    private final ArrayList<Window> modalWindows = new ArrayList<Window>();

    /// Drops a disposed window's modal registration and re-syncs what the ports
    /// block, so a window that went away stops blocking.
    void forgetModal(Window w) {
        modalWindows.remove(w);
        syncNativeModalBlocking();
    }

    void pushModalWindow(Window w) {
        modalWindows.add(w);
        syncNativeModalBlocking();
    }

    void popModalWindow(Window w) {
        modalWindows.remove(w);
        syncNativeModalBlocking();
    }

    /// Tells every native window whether input to it is currently blocked.
    ///
    /// The framework already decides this, in `#isBlockedByModal(int)`, and it is the
    /// only place that can: the answer depends on the whole modal stack, on each
    /// window's scope and on who owns it. A port that tried to derive it from a single
    /// "this window became modal" call has to reinvent nesting and ownership, and gets
    /// them wrong -- releasing an inner modal re-enabled everything the outer one was
    /// still blocking, application modality left the other secondary windows enabled,
    /// and an unowned window modal disabled a main window it never claimed.
    ///
    /// This matters beyond appearances, because a blocked window's own title bar is
    /// outside the framework's input filter: its close button still reaches the
    /// application.
    void syncNativeModalBlocking() {
        WindowManager wm = Display.impl.getWindowManager();
        if (wm == null) {
            return;
        }
        wm.setMainWindowInputEnabled(!isBlockedByModal(0));
        for (Window each : getWindows()) {
            Object peer = each.getNativePeer();
            if (peer != null) {
                wm.setInputEnabled(peer, !isBlockedByModal(each.getWindowId()));
            }
        }
    }

    /// Whether input aimed at the given window is currently blocked by a modal.
    ///
    /// Public because the implementation needs it: a wheel gesture is played as four
    /// steps queued on the event dispatch thread, and a listener can show a modal
    /// between the first check and the last step.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// #### Returns
    ///
    /// true when input to that window is currently blocked
    public boolean isWindowInputBlocked(int windowId) {
        return isBlockedByModal(windowId);
    }

    private boolean isBlockedByModal(int windowId) {
        // Every registered blocker is consulted rather than only the newest one.
        // Modal windows nest: a window modal opened from inside an application modal
        // blocks only its own owner, and stopping at the top of the stack would let
        // input back into the main form and every unrelated window for as long as the
        // narrower one was up.
        Window self = windowId > 0 ? windowById(windowId) : null;
        int len = modalWindows.size();
        for (int iter = len - 1; iter >= 0; iter--) {
            Window modal = modalWindows.get(iter);
            if (modal.getWindowId() == windowId) {
                // Never blocked by itself -- but keep looking at the outer blockers.
                // Returning here exempted a modal window from *every* other modal,
                // so an unrelated modal shown while an application modal was up
                // accepted input that application modality is meant to stop.
                continue;
            }
            if (self != null && ownedBy(self, modal)) {
                // A modal opened from inside another is not blocked by the one it was
                // opened from. That is the exemption the self check was reaching for,
                // and it applies to the owner chain rather than to any modal at all.
                continue;
            }
            if (isModalOutOfReach(modal)) {
                // Hidden along with an owner the application hid. The registration is
                // kept on purpose -- hideNotify() cannot tell an owner cascade from a
                // minimize, and a minimized modal is still open and still modal -- but a
                // modal nobody can see or dismiss must not go on blocking. Left in, the
                // owner's hide froze the main surface and every unrelated window with no
                // window on screen to release them, until the owner was shown again.
                continue;
            }
            if (blocks(modal, windowId)) {
                return true;
            }
        }
        return false;
    }

    /// Whether `w` sits inside `candidateOwner`'s ownership chain.
    private static boolean ownedBy(Window w, Window candidateOwner) {
        TopLevelContainer owner = w.getOwnerWindow();
        while (owner instanceof Window) {
            if (owner == candidateOwner) { //NOPMD CompareObjectsWithEquals
                return true;
            }
            owner = ((Window) owner).getOwnerWindow();
        }
        return false;
    }

    /// Whether one modal window blocks input to the window with the given id.
    /// Whether a registered modal is currently unreachable because an owner above it
    /// is off screen.
    ///
    /// Only the owner chain is consulted, never the modal's own visibility: a modal the
    /// user minimized is still open and still blocks, which is what keeps a minimized
    /// modal from quietly releasing the application. An owner that is not showing is a
    /// different situation -- its children went with it and cannot be restored
    /// independently, so nothing on screen can dismiss them.
    ///
    /// #### Parameters
    ///
    /// - `modal`: the registered modal window
    ///
    /// #### Returns
    ///
    /// true when an owner of this modal is not showing
    private static boolean isModalOutOfReach(Window modal) {
        TopLevelContainer owner = modal.getOwnerWindow();
        while (owner instanceof Window) {
            Window w = (Window) owner;
            if (!w.isWindowShowing()) {
                return true;
            }
            owner = w.getOwnerWindow();
        }
        return false;
    }

    private boolean blocks(Window modal, int windowId) {
        if (modal.getModalityType() == Window.MODALITY_APPLICATION) {
            return true;
        }
        // window modal: only the owner is blocked
        TopLevelContainer owner = modal.getOwnerWindow();
        if (owner instanceof Window) {
            return ((Window) owner).getWindowId() == windowId;
        }
        if (owner != null) {
            // owned by the main form
            return windowId == 0;
        }
        // No owner at all. Window modality blocks the owning window, and there is
        // none, so it blocks nothing -- treating this as main-form ownership would
        // block the main form on a window that never claimed it.
        return false;
    }

    // ---- window lifecycle --------------------------------------------------------
    //
    // The platform reports a window shown, hidden, moved, resized, focused or closed,
    // and these turn that into framework state. They live here rather than in Display
    // because every one of them is about a window, and the registry that resolves an
    // id to a window is here.

    private final Hashtable pendingWindowSizes = new Hashtable();

    /// Guards `#monitorsChangedPending`, which is set from the port's native event
    /// thread and cleared on the event dispatch thread.
    private final Object monitorsChangedLock = new Object();

    /// Whether a monitor-topology notification is already queued. See
    /// `#monitorsChanged()`.
    private boolean monitorsChangedPending;


    /// Notifies Codename One that a native window became visible.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    public void windowShowNotify(int windowId) {
        if (windowId > 0) {
            // Deliberately not the packed input queue. That queue drops events when it
            // is full and while invokeAndBlock is running in drop mode, and nothing
            // reconciles a lost one afterwards: a dropped show leaves a visible window
            // the framework believes is iconified and never paints again, and a
            // dropped hide leaves a hidden window painting and keeping the event
            // dispatch thread awake. Lifecycle notifications are not droppable.
            Display.getInstance().callSerially(new WindowCallback(windowId, WindowCallback.SHOWN));
        }
    }

    /// Notifies Codename One that a native window stopped being visible.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    public void windowHideNotify(int windowId) {
        if (windowId > 0) {
            // See windowShowNotify: not droppable.
            Display.getInstance().callSerially(new WindowCallback(windowId, WindowCallback.HIDDEN));
        }
    }

    /// Notifies Codename One that a native window gained or lost keyboard focus.
    /// Marshalled onto the event dispatch thread, since it runs application code.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `gained`: true when the window gained focus
    public void windowFocusChanged(int windowId, boolean gained) {
        Display.getInstance().callSerially(new WindowCallback(windowId,
                gained ? WindowCallback.FOCUS_GAINED : WindowCallback.FOCUS_LOST));
    }

    /// Notifies Codename One that the user activated a native window's close control.
    /// Marshalled onto the event dispatch thread, since it runs application code and
    /// may dispose the window.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    public void windowCloseRequested(int windowId) {
        // Queued first, and the modality check made on the event dispatch thread inside
        // the callback. The modal stack is mutated there by show, hide and dispose, so
        // testing it from the port's callback thread raced: isBlockedByModal takes the
        // stack's size and then indexes it, which a concurrent removal turns into an
        // exception, and a stale read could let a blocked window's close through.
        Display.getInstance().callSerially(new WindowCallback(windowId, WindowCallback.CLOSE_REQUESTED));
    }

    /// Notifies Codename One that the platform has already destroyed a window's
    /// native surface, so the window is gone whatever the application would prefer.
    ///
    /// Distinct from `#windowCloseRequested(int)`, which asks. Some platforms do not
    /// offer the close control as a question: a Mac Catalyst scene is disconnected
    /// after the fact, with nothing left to veto. Reporting that as a request would
    /// let `DO_NOTHING_ON_CLOSE` leave a registered window painting into a surface
    /// that no longer exists, so it is reported as what it is and the window is
    /// disposed.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    public void windowClosedNatively(int windowId) {
        Display.getInstance().callSerially(new WindowCallback(windowId, WindowCallback.CLOSED_NATIVELY));
    }

    /// Notifies Codename One that the platform refused to create a window's native
    /// surface, so the window will never appear.
    ///
    /// Separate from `#windowHideNotify(int)` because that one means "minimized",
    /// which keeps a modal window's registration: a modal that never appeared would
    /// otherwise block input to every other window while `showModal()` waited for it.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the window whose native surface could not be created
    public void windowActivationFailed(int windowId) {
        if (windowId > 0) {
            WindowCallback failure = new WindowCallback(windowId,
                    WindowCallback.ACTIVATION_FAILED);
            if (Display.getInstance().isEdt()) {
                // Applied in the caller's own turn when it is already on the event
                // dispatch thread. A port validates this failure against the request
                // token it belongs to and then reports it, and queueing again splits
                // those two steps across turns: a retrying show() can run in between,
                // start a new request, and be marked hidden and stripped of its
                // modality by a failure that no longer applies to it. Running here
                // keeps the check and its consequence in one unit, which is what the
                // check is for.
                failure.run();
                return;
            }
            // Not droppable, for the same reason as the other lifecycle notifications.
            Display.getInstance().callSerially(failure);
        }
    }

    /// Notifies Codename One that the user moved a native window.
    ///
    /// Separate from `#windowMonitorChanged(int)`, which is only for a move that
    /// carried the window onto a different display: an ordinary move within one
    /// monitor still has to reach the listeners, or nothing can persist a window's
    /// position.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    public void windowMoved(int windowId) {
        if (windowId > 0) {
            Display.getInstance().callSerially(new WindowCallback(windowId, WindowCallback.MOVED));
        }
    }

    /// Notifies Codename One that a native window moved to a monitor with different
    /// characteristics, so that its scale and layout are recomputed.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    public void windowMonitorChanged(int windowId) {
        Display.getInstance().callSerially(new WindowCallback(windowId, WindowCallback.MONITOR_CHANGED));
    }

    /// Notifies Codename One that a native window changed size. Invoked by the
    /// implementation.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `w`: the new drawable width
    ///
    /// - `h`: the new drawable height
    public void windowSizeChanged(int windowId, int w, int h) {
        if (windowId <= 0) {
            return;
        }
        // Coalesced onto the event dispatch thread rather than queued as a packet.
        // The packed stack drops when it is full, which live resizing does easily, and
        // the dropped packet can be the *final* size -- the native surface has already
        // adopted it, so the hierarchy stays laid out for an earlier one with nothing
        // guaranteed to correct it, leaving painting and hit testing misaligned.
        //
        // Coalescing is what makes a non-droppable path affordable here: only one
        // notification per window is ever outstanding, and it carries whatever the
        // latest dimensions are when it runs, so a drag that produces hundreds of
        // resizes still costs one queued runnable at a time.
        final Integer key = Integer.valueOf(windowId);
        boolean queue;
        synchronized (pendingWindowSizes) {
            int[] latest = (int[]) pendingWindowSizes.get(key);
            if (latest == null) {
                latest = new int[2];
                pendingWindowSizes.put(key, latest);
                queue = true;
            } else {
                queue = false;
            }
            latest[0] = w;
            latest[1] = h;
        }
        if (!queue) {
            return;
        }
        final int id = windowId;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                int width;
                int height;
                synchronized (pendingWindowSizes) {
                    int[] latest = (int[]) pendingWindowSizes.remove(key);
                    if (latest == null) {
                        return;
                    }
                    width = latest[0];
                    height = latest[1];
                }
                Window w = windowById(id);
                if (w != null) {
                    w.sizeChangedInternal(width, height);
                }
            }
        });
    }

    /// Notifies Codename One that the set of attached monitors changed.
    public void monitorsChanged() {
        synchronized (monitorsChangedLock) {
            // Genuinely coalesced rather than merely documented as such. One physical
            // display change is reported many times over: Windows broadcasts
            // WM_DISPLAYCHANGE to every top level window, and GTK fires geometry,
            // work-area and scale-factor notifications separately for each monitor.
            // Each notification relays out every open window and fires every monitor
            // listener, so without this a single resolution change did that work N
            // times. A change arriving while one is queued is already covered by it.
            if (monitorsChangedPending) {
                return;
            }
            monitorsChangedPending = true;
        }
        Display.getInstance().callSerially(new WindowCallback(0, WindowCallback.MONITORS_CHANGED));
    }

    /// Lets the queued notification re-arm the coalescing guard. See
    /// `#monitorsChanged()`.
    void clearMonitorsChangedPending() {
        synchronized (monitorsChangedLock) {
            monitorsChangedPending = false;
        }
    }

    /// Marshals a window notification that arrived on the platform's own thread onto
    /// the event dispatch thread. A named static class rather than an anonymous one so
    /// it does not retain the `Display` it was created from.
    private static final class WindowCallback implements Runnable {
        private static final int FOCUS_GAINED = 0;
        private static final int FOCUS_LOST = 1;
        private static final int CLOSE_REQUESTED = 2;
        private static final int MONITOR_CHANGED = 3;
        private static final int MONITORS_CHANGED = 4;
        private static final int MOVED = 5;
        private static final int CLOSED_NATIVELY = 6;
        private static final int SHOWN = 7;
        private static final int HIDDEN = 8;
        private static final int ACTIVATION_FAILED = 9;

        private final int windowId;
        private final int kind;

        WindowCallback(int windowId, int kind) {
            this.windowId = windowId;
            this.kind = kind;
        }

        @Override
        public void run() {
            Desktop desktop = Desktop.getInstance();
            Window w = desktop.windowById(windowId);
            switch (kind) {
                case FOCUS_GAINED:
                    desktop.setFocusedWindow(w);
                    break;
                case FOCUS_LOST:
                    if (desktop.getFocusedWindow() == w) { //NOPMD CompareObjectsWithEquals
                        desktop.setFocusedWindow(null);
                    }
                    if (w != null) {
                        // The fifth way a window stops being reachable, after hide,
                        // minimize, dispose and modal blocking. The physical key-up
                        // goes to whatever has focus now, so without this a held key
                        // repeats into this window for as long as it stays open and
                        // a pressed component stays latched.
                        w.cancelPendingInput();
                    }
                    break;
                case CLOSE_REQUESTED:
                    // A close arrives outside the packed input queue, so it bypasses the
                    // modality filter that guards every other event. A port that cannot
                    // disable a blocked window natively -- Catalyst has no such control
                    // -- would otherwise let the user close a window an application
                    // modal is supposed to be blocking. Checked here rather than at the
                    // callback, so the modal stack is only ever read on this thread.
                    if (w != null && !Desktop.getInstance().isWindowInputBlocked(windowId)) {
                        w.closeRequested();
                    }
                    break;
                case MONITOR_CHANGED:
                    // One window moved to another display. Deliberately not
                    // desktop.fireMonitorChanged(): Desktop.addMonitorListener is
                    // documented for a monitor being attached, removed or
                    // reconfigured, and firing it for every drag across a mixed-DPI
                    // desktop turned an ordinary window move into a topology event --
                    // repeatedly re-running whatever display reconfiguration work the
                    // application does there. The window itself re-reads its scale and
                    // lays out below, and an application that wants to follow one
                    // window across displays sees it through that window's Moved
                    // event plus getMonitor().
                    if (w != null) {
                        w.monitorChanged();
                    }
                    break;
                case MONITORS_CHANGED:
                    // Cleared before the work, not after: a display change that
                    // happens while this runs describes a topology this pass has not
                    // read yet, so it has to queue another one rather than be
                    // swallowed as a duplicate.
                    desktop.clearMonitorsChangedPending();
                    for (Window each : desktop.getWindows()) {
                        each.monitorChanged();
                    }
                    desktop.fireMonitorChanged();
                    break;
                case MOVED:
                    if (w != null) {
                        w.moved();
                    }
                    break;
                case SHOWN:
                    if (w != null) {
                        w.showNotify();
                        // A visibility change can change what is blocked: a modal whose
                        // owner went away stops blocking, and blocks again when the
                        // owner returns. Only push, pop and dispose re-synced the native
                        // flags, so the framework and the platform disagreed for as long
                        // as the window stayed hidden -- the platform kept the main
                        // surface disabled with no modal on screen to release it.
                        Desktop.getInstance().syncNativeModalBlocking();
                    }
                    break;
                case HIDDEN:
                    if (w != null) {
                        w.hideNotify();
                        Desktop.getInstance().syncNativeModalBlocking();
                    }
                    break;
                case ACTIVATION_FAILED:
                    if (w != null) {
                        w.activationFailed();
                    }
                    break;
                case CLOSED_NATIVELY:
                    if (w != null) {
                        // A window a modal is blocking must not be closable. Where the
                        // platform's close control cannot be disabled the close has
                        // already happened, so the only way to honour the contract is
                        // to put the window back; a port that cannot returns false and
                        // the window is disposed, because the surface is genuinely gone.
                        if (!Desktop.getInstance().isWindowInputBlocked(windowId)
                                || !w.reopenNativeSurface()) {
                            w.dispose();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    // ---- window geometry queried by the ports ------------------------------------

    /// Indicates whether input aimed at the given window is currently blocked by a
    /// modal window above it.
    /// The drag-region status at a point inside one of the additional native windows,
    /// used by the implementation's drag activation filter.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the window to ask
    ///
    /// - `x`: x in the window's coordinates
    ///
    /// - `y`: y in the window's coordinates
    ///
    /// #### Returns
    ///
    /// the drag region status, or `Component#DRAG_REGION_NOT_DRAGGABLE` when there is
    /// no such window
    public int windowDragRegionStatus(int windowId, int x, int y) {
        Window w = windowById(windowId);
        return w == null ? Component.DRAG_REGION_NOT_DRAGGABLE : w.getDragRegionStatus(x, y);
    }

    /// The width of one of the additional native windows, or 0 when there is no such
    /// window.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the window to ask
    ///
    /// #### Returns
    ///
    /// the window's width in Codename One coordinates
    public int windowWidth(int windowId) {
        Window w = windowById(windowId);
        return w == null ? 0 : w.getWidth();
    }

    /// The height of one of the additional native windows, or 0 when there is no such
    /// window.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the window to ask
    ///
    /// #### Returns
    ///
    /// the window's height in Codename One coordinates
    public int windowHeight(int windowId) {
        Window w = windowById(windowId);
        return w == null ? 0 : w.getHeight();
    }

    // ---- input reported by the ports ---------------------------------------------
    //
    // A port calls these when something happens in one of its windows. They hand the
    // event to Display's input queue, which is the one thing about a window event that
    // is genuinely Display's: there is a single queue and a single event dispatch
    // thread, shared with the main surface.


    /// Pushes a key press event aimed at one native window into Codename One.
    /// Invoked by the implementation, off the event dispatch thread.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `keyCode`: keycode of the key event
    public void windowKeyPressed(int windowId, int keyCode) {
        if (windowId > 0) {
            Display.getInstance().keyPressedImpl(windowId, keyCode);
        }
    }

    /// Pushes a key release aimed at one native window into Codename One.
    /// Invoked by the implementation, off the event dispatch thread.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `keyCode`: keycode of the key event
    public void windowKeyReleased(int windowId, int keyCode) {
        if (windowId > 0) {
            Display.getInstance().keyReleasedImpl(windowId, keyCode);
        }
    }

    /// Pushes a hover press aimed at one native window into Codename One. Invoked by
    /// the implementation, off the event dispatch thread.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `x`: the x position of the pointer, in window coordinates
    ///
    /// - `y`: the y position of the pointer, in window coordinates
    public void windowPointerHoverPressed(int windowId, int[] x, int[] y) {
        if (windowId > 0) {
            Display.getInstance().pointerHoverPressedImpl(windowId, x, y);
        }
    }

    /// Pushes a hover release aimed at one native window into Codename One. Invoked by
    /// the implementation, off the event dispatch thread.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `x`: the x position of the pointer, in window coordinates
    ///
    /// - `y`: the y position of the pointer, in window coordinates
    public void windowPointerHoverReleased(int windowId, int[] x, int[] y) {
        if (windowId > 0) {
            Display.getInstance().pointerHoverReleasedImpl(windowId, x, y);
        }
    }

    /// Dispatches a wheel event that arrived over a native window.
    ///
    /// A port with desktop windows has to route the wheel explicitly: the main
    /// surface version resolves the component from the current form, so a wheel over
    /// a second window would either do nothing or scroll the main form instead.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created, or 0 for
    /// the application's main surface
    ///
    /// - `x`: the pointer x position in window pixels
    ///
    /// - `y`: the pointer y position in window pixels
    ///
    /// - `scrollX`: the horizontal scroll amount in display pixels
    ///
    /// - `scrollY`: the vertical scroll amount in display pixels
    ///
    /// - `precise`: true if the deltas come from a high resolution device such as a
    /// trackpad
    ///
    /// - `modifiers`: bitmask of the held keyboard modifiers
    ///
    /// #### Returns
    ///
    /// true if a listener consumed the wheel event
    public boolean windowMouseWheelEvent(int windowId, int x, int y, int scrollX, int scrollY,
            boolean precise, int modifiers) {
        return Display.getInstance().windowMouseWheelEventImpl(windowId, x, y, scrollX, scrollY,
                precise, modifiers);
    }

    /// Dispatches a magnify (pinch) gesture that arrived over a native window. Window
    /// 0 is the application's main surface.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `x`: the gesture x position in pixels, relative to that window
    ///
    /// - `y`: the gesture y position in pixels, relative to that window
    ///
    /// - `scale`: the magnification scale, larger than 1 zooms in and smaller than 1
    /// zooms out
    public void windowMagnifyGesture(int windowId, int x, int y, float scale) {
        Display.getInstance().windowMagnifyGestureImpl(windowId, x, y, scale);
    }

    /// Dispatches a rotation (twist) gesture that arrived over a native window. Window
    /// 0 is the application's main surface.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `x`: the gesture x position in pixels, relative to that window
    ///
    /// - `y`: the gesture y position in pixels, relative to that window
    ///
    /// - `radians`: the incremental rotation in radians, positive is clockwise
    public void windowRotationGesture(int windowId, int x, int y, float radians) {
        Display.getInstance().windowRotationGestureImpl(windowId, x, y, radians);
    }

    /// Returns the native window peer owning the given component, or null when it
    /// belongs to the application's main surface. Ports use this to place native peers
    /// and native text editors into the correct window.
    ///
    /// It lives here rather than on `Display` because this class owns the windows;
    /// `Display` answers for the application's single main surface and knowing which
    /// window a component is in is not a question about that surface.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the component to locate
    ///
    /// #### Returns
    ///
    /// the owning window's native peer, or null for the main surface
    public Object getWindowPeerForComponent(Component cmp) {
        if (cmp == null) {
            return null;
        }
        TopLevelContainer top = cmp.getTopLevelContainer();
        return top == null ? null : top.asContainer().topLevelNativePeer();
    }

    /// Pushes a pointer drag aimed at one native window into Codename One.
    /// Invoked by the implementation, off the event dispatch thread.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `x`: the x positions of the pointer
    ///
    /// - `y`: the y positions of the pointer
    public void windowPointerDragged(int windowId, int[] x, int[] y) {
        if (windowId > 0) {
            Display.getInstance().pointerDraggedImpl(windowId, x, y);
        }
    }

    /// Pushes a pointer hover event that arrived over a specific native window.
    ///
    /// A port with desktop windows has to say which window the pointer was over, or
    /// hovering a second window sends the event to whatever the main form has at the
    /// same coordinates -- so the window gets no tooltips and the main form gets
    /// spurious ones.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `x`: the x position of the pointer, in window coordinates
    ///
    /// - `y`: the y position of the pointer, in window coordinates
    public void windowPointerHover(int windowId, final int[] x, final int[] y) {
        if (windowId > 0) {
            Display.getInstance().pointerHoverImpl(windowId, x, y);
        }
    }

    /// Pushes a pointer press aimed at one native window into Codename One.
    /// Invoked by the implementation, off the event dispatch thread.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `x`: the x positions of the pointer
    ///
    /// - `y`: the y positions of the pointer
    public void windowPointerPressed(int windowId, int[] x, int[] y) {
        if (windowId > 0) {
            Display.getInstance().pointerPressedImpl(windowId, x, y);
        }
    }

    /// Pushes a pointer release aimed at one native window into Codename One.
    /// Invoked by the implementation, off the event dispatch thread.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id the port was given when the window was created
    ///
    /// - `x`: the x positions of the pointer
    ///
    /// - `y`: the y positions of the pointer
    public void windowPointerReleased(int windowId, int[] x, int[] y) {
        if (windowId > 0) {
            Display.getInstance().pointerReleasedImpl(windowId, x, y);
        }
    }

    // ---- painting the open windows -----------------------------------------------
    //
    // Called once per pass from Display's event loop. The loop is Display's; walking
    // the windows it has to paint is not.

    /// Creates the `Graphics` a window paints through and hands it to the
    /// implementation. `Graphics` cannot be constructed outside this package, which
    /// is why this lives here rather than on the window or the implementation --
    /// exactly as `#init(java.lang.Object)` does for the main surface.
    Graphics createWindowGraphics(Window w) {
        Graphics g = new Graphics(Display.impl.getWindowManager().getNativeGraphics(w.getNativePeer()));
        g.paintPeersBehind = Display.impl.paintNativePeersBehind();
        w.getPaintSurface().setGraphics(g);
        return g;
    }

    void paintWindows() {
        ArrayList<Window> open = windowList();
        for (int iter = 0; iter < open.size(); iter++) { // NOPMD ForLoopCanBeForeach
            Window w = open.get(iter);
            if (!w.isWindowShowing()) {
                continue;
            }
            Graphics g = w.getWindowGraphics();
            Object peer = w.getNativePeer();
            // The manager as well as the graphics and the peer. A window stays
            // registered until it is disposed, so one can outlive the platform's
            // window manager -- and dereferencing it here throws on the event dispatch
            // thread, which catches the exception, comes straight back round the loop
            // and throws again. That spins forever rather than losing a frame, so the
            // one thing this must not do is assume the manager is still there.
            WindowManager wm = Display.impl.getWindowManager();
            if (g == null || peer == null || wm == null) {
                continue;
            }
            g.setGraphics(wm.getNativeGraphics(peer));
            w.flushRevalidateQueue();
            w.getPaintSurface().paintDirty(w.getWidth(), w.getHeight());
            w.repaintAnimations();
            // The window's raster exists from the moment it is shown, so a capture
            // before this point returns a blank frame of the right size. Recording
            // that a cycle completed is what lets a caller wait for real content.
            w.markPainted();
        }
    }

    boolean anyWindowHasAnimations() {
        ArrayList<Window> open = windowList();
        for (int iter = 0; iter < open.size(); iter++) { // NOPMD ForLoopCanBeForeach
            Window w = open.get(iter);
            if (w.isWindowShowing() && w.hasAnimations()) {
                return true;
            }
        }
        return false;
    }

    /// Repaints every window that is on screen. The main surface is the caller's
    /// business; this is the window half of it.
    void repaintWindows() {
        ArrayList<Window> open = windowList();
        for (int iter = 0; iter < open.size(); iter++) { // NOPMD ForLoopCanBeForeach
            open.get(iter).repaint();
        }
    }

}
