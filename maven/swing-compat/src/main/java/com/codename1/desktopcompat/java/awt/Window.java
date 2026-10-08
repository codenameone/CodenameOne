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
package com.codename1.desktopcompat.java.awt;

import com.codename1.desktopcompat.java.awt.event.WindowEvent;
import com.codename1.desktopcompat.java.awt.event.WindowFocusListener;
import com.codename1.desktopcompat.java.awt.event.WindowListener;
import com.codename1.desktopcompat.java.awt.event.WindowStateListener;
import com.codename1.desktopcompat.rt.EventBridge;
import com.codename1.desktopcompat.rt.FrameForm;
import com.codename1.desktopcompat.rt.MenuBridge;
import com.codename1.desktopcompat.rt.Units;
import com.codename1.desktopcompat.rt.WindowHost;
import com.codename1.desktopcompat.rt.WindowHosts;
import java.util.ArrayList;

/// A top level window.
///
/// What shows a window depends on the device; see
/// [com.codename1.desktopcompat.rt.WindowHosts]. In short: the first
/// window fills a form; where there is a window manager the others are
/// windows of their own with the bounds the application set; on a phone
/// a further frame is a form over the one before it, the size of the
/// screen whatever was asked for, and a dialog floats over the current
/// form. Windows start hidden.
///
/// The window on top is the active and the focused one. Showing a window
/// puts it on top; `toBack` does nothing. `getWindows()` answers the
/// windows that were shown or packed and not disposed of since -- there
/// are no weak references to tell which of the others are still in use.
public class Window extends Container {

    private final Window owner;
    private final ArrayList<Image> icons = new ArrayList<Image>();
    private ArrayList<Window> owned;
    private ArrayList<WindowListener> windowListeners;
    private ArrayList<WindowFocusListener> windowFocusListeners;
    private ArrayList<WindowStateListener> windowStateListeners;
    private WindowHost host;
    private Component lastFocus;
    private boolean visible;
    private boolean opened;
    private boolean alwaysOnTop;
    private boolean locationByPlatform;
    private boolean focusableWindowState = true;
    private boolean autoRequestFocus = true;
    private boolean embedded;

    public Window(Frame owner) {
        this((Window) owner);
    }

    public Window(Window owner) {
        this.owner = owner;
        if (owner != null) {
            if (owner.owned == null) {
                owner.owned = new ArrayList<Window>();
            }
            owner.owned.add(this);
        }
        setLayout(new BorderLayout());
    }

    // ------------------------------------------------------------ host

    /// The form showing this window when a form of its own does, else
    /// `null`: before it is shown, for a dialog, and for a window of the
    /// window manager.
    public FrameForm cn1Form() {
        return host instanceof FrameForm ? (FrameForm) host : null;
    }

    /// What shows this window, `null` until it is shown and with no
    /// display.
    public WindowHost cn1Host() {
        return host;
    }

    /// The Codename One form this window is in -- its own form or the
    /// dialog that floats it -- or `null`.
    public com.codename1.ui.Form cn1HostForm() {
        return host == null ? null : host.form();
    }

    /// Puts the window inside a host its caller owns, for good: a
    /// Codename One component that shows a Swing tree inside a form of the
    /// application (see `com.codename1.desktopcompat.SwingInterop`).
    ///
    /// Such a window is showing from here on and is never one of the
    /// application's windows: it is not registered, it does not become the
    /// active window, and hiding or disposing of it does nothing -- the
    /// component that hosts it is what comes and goes.
    public void cn1Embed(WindowHost h) {
        embedded = true;
        host = h;
        visible = true;
        if (!isDisplayable()) {
            addNotify();
        }
        validate();
    }

    /// Whether [#cn1Embed] put this window inside a component.
    public boolean cn1Embedded() {
        return embedded;
    }

    /// The title the host carries; frames and dialogs answer theirs.
    public String cn1Title() {
        return "";
    }

    /// Whether the user may resize the window; frames and dialogs answer
    /// theirs.
    protected boolean cn1Resizable() {
        return true;
    }

    /// Whether the window has a title bar and a frame; a plain window has
    /// neither.
    protected boolean cn1Decorated() {
        return false;
    }

    /// The extended state the window should be shown in; frames answer
    /// theirs.
    protected int cn1State() {
        return Frame.NORMAL;
    }

    /// Runs what a user's request to close the window runs.
    public void cn1Closing() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    /// The window became, or stopped being, the active one. Called by the
    /// window registry; delivers the activation and window focus events
    /// and moves the keyboard focus out of, or back into, the window.
    public void cn1Activated(boolean active, Window opposite) {
        if (active) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_ACTIVATED, opposite));
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_GAINED_FOCUS, opposite));
            Component f = lastFocus;
            lastFocus = null;
            if (f != null && isAncestorOf(f) && f.isDisplayable()) {
                f.requestFocusInWindow();
            }
        } else {
            Component f = EventBridge.focusOwner();
            if (f != null && (f == this || isAncestorOf(f))) {
                lastFocus = f;
                EventBridge.setFocusOwner(null, false);
            }
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_LOST_FOCUS, opposite));
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_DEACTIVATED, opposite));
        }
    }

    /// The window was minimized or restored, or the application went to
    /// the background or came back.
    public void cn1Iconified(boolean iconified) {
        dispatchEvent(new WindowEvent(this, iconified ? WindowEvent.WINDOW_ICONIFIED
                : WindowEvent.WINDOW_DEICONIFIED));
    }

    @Override
    public void cn1SyncPeerBounds() {
    }

    // ------------------------------------------------------------ windows

    public Window getOwner() {
        return owner;
    }

    public Window[] getOwnedWindows() {
        return owned == null ? new Window[0] : owned.toArray(new Window[owned.size()]);
    }

    /// The windows that were shown or packed and not disposed of since.
    public static Window[] getWindows() {
        return WindowHosts.windows();
    }

    public static Window[] getOwnerlessWindows() {
        Window[] all = WindowHosts.windows();
        ArrayList<Window> l = new ArrayList<Window>();
        for (int i = 0; i < all.length; i++) {
            if (all[i].owner == null) {
                l.add(all[i]);
            }
        }
        return l.toArray(new Window[l.size()]);
    }

    @Override
    public Toolkit getToolkit() {
        return Toolkit.getDefaultToolkit();
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public boolean isShowing() {
        return visible;
    }

    @Override
    public boolean isValidateRoot() {
        return true;
    }

    @Override
    public Point getLocationOnScreen() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p == null) {
            return new Point(getX(), getY());
        }
        return new Point(Units.toLogical(p.getAbsoluteX()), Units.toLogical(p.getAbsoluteY()));
    }

    @Override
    public Color getBackground() {
        Color c = super.getBackground();
        return c != null ? c : EventBridge.defaultBackground();
    }

    @Override
    public Color getForeground() {
        Color c = super.getForeground();
        return c != null ? c : EventBridge.defaultForeground();
    }

    @Override
    public Font getFont() {
        Font f = super.getFont();
        return f != null ? f : com.codename1.desktopcompat.rt.Fonts.defaultFont();
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paint(g);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!embedded) {
            WindowHosts.registered(this);
        }
    }

    // ------------------------------------------------------------ bounds

    /// Sizes the window to its preferred size and lays it out. A window
    /// that fills a form keeps the size of the form.
    public void pack() {
        if (!isDisplayable()) {
            addNotify();
        }
        if (host == null || !host.fillsDisplay()) {
            Dimension d = getPreferredSize();
            setSize(d.width, d.height);
        }
        validate();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (host != null) {
            host.bounds();
        }
    }

    /// Centers the window over `c`, or on the screen when `c` is `null`
    /// or not showing. It has an effect only on a window of the window
    /// manager; the others are placed by their host.
    public void setLocationRelativeTo(Component c) {
        if (host != null && host.fillsDisplay()) {
            return;
        }
        Dimension s = getToolkit().getScreenSize();
        int cx = s.width / 2;
        int cy = s.height / 2;
        if (c != null && c.isShowing()) {
            Point p = c.getLocationOnScreen();
            cx = p.x + c.getWidth() / 2;
            cy = p.y + c.getHeight() / 2;
        }
        setLocation(Math.max(0, cx - getWidth() / 2), Math.max(0, cy - getHeight() / 2));
    }

    public boolean isLocationByPlatform() {
        return locationByPlatform;
    }

    /// Recorded only: a window of the window manager that was never
    /// moved is centered, whatever this says.
    public void setLocationByPlatform(boolean locationByPlatform) {
        this.locationByPlatform = locationByPlatform;
    }

    // ------------------------------------------------------------ showing

    @Override
    public void setVisible(boolean b) {
        if (b == visible || embedded) {
            return;
        }
        visible = b;
        if (b) {
            if (!isDisplayable()) {
                addNotify();
            }
            host = WindowHosts.open(this, host);
            if (host != null) {
                host.title(cn1Title());
                host.icon(icons.isEmpty() ? null : icons.get(0).cn1Image());
                host.resizable(cn1Resizable());
                host.decorated(cn1Decorated());
                WindowHost opening = host;
                opening.open();
                if (!visible || host != opening) {
                    // Hidden again by something that ran while it opened.
                    return;
                }
                if (cn1State() != Frame.NORMAL) {
                    host.state(cn1State());
                }
                MenuBridge.windowShown(this);
            } else {
                validate();
            }
            if (!opened) {
                opened = true;
                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_OPENED));
            }
            WindowHosts.shown(this);
        } else {
            WindowHost h = host;
            if (h != null) {
                if (!h.reusable()) {
                    host = null;
                }
                h.close();
            }
            WindowHosts.hidden(this);
        }
    }

    /// Hides the window, disposes of the windows it owns and releases
    /// what showed it. The window can be shown again.
    public void dispose() {
        if (embedded) {
            return;
        }
        boolean was = isDisplayable();
        if (owned != null) {
            Window[] ws = getOwnedWindows();
            for (int i = 0; i < ws.length; i++) {
                ws[i].dispose();
            }
        }
        setVisible(false);
        if (was) {
            if (host != null) {
                host.release();
                host = null;
            }
            removeNotify();
            opened = false;
            WindowHosts.released(this);
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSED));
        }
    }

    /// Puts the window on top of the others and makes it the active one.
    public void toFront() {
        if (visible && !embedded) {
            if (host != null) {
                host.open();
            }
            WindowHosts.shown(this);
        }
    }

    /// Does nothing.
    public void toBack() {
    }

    public boolean isActive() {
        if (embedded) {
            // Active while the form its component is on is the one showing.
            com.codename1.ui.Form f = host == null ? null : host.form();
            return f != null && com.codename1.ui.Display.isInitialized()
                    && com.codename1.ui.Display.getInstance().getCurrent() == f;
        }
        return visible && WindowHosts.active() == this;
    }

    public boolean isFocused() {
        return isActive();
    }

    public Component getFocusOwner() {
        Component c = EventBridge.focusOwner();
        return c != null && (c == this || isAncestorOf(c)) ? c : null;
    }

    public Component getMostRecentFocusOwner() {
        Component c = getFocusOwner();
        return c != null ? c : lastFocus;
    }

    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }

    /// Recorded only.
    public final void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
    }

    public boolean getFocusableWindowState() {
        return focusableWindowState;
    }

    /// Recorded only.
    public void setFocusableWindowState(boolean focusableWindowState) {
        this.focusableWindowState = focusableWindowState;
    }

    public boolean isAutoRequestFocus() {
        return autoRequestFocus;
    }

    /// Recorded only: a window that shows always becomes the active one.
    public void setAutoRequestFocus(boolean autoRequestFocus) {
        this.autoRequestFocus = autoRequestFocus;
    }

    // ------------------------------------------------------------ icon

    /// Sets the icon of the window. Only a window of the window manager
    /// shows one.
    public void setIconImage(Image image) {
        icons.clear();
        if (image != null) {
            icons.add(image);
        }
        if (host != null) {
            host.icon(image == null ? null : image.cn1Image());
        }
    }

    public void setIconImages(java.util.List<? extends Image> icons) {
        this.icons.clear();
        if (icons != null) {
            for (int i = 0; i < icons.size(); i++) {
                if (icons.get(i) != null) {
                    this.icons.add(icons.get(i));
                }
            }
        }
        if (host != null) {
            host.icon(this.icons.isEmpty() ? null : this.icons.get(0).cn1Image());
        }
    }

    public java.util.List<Image> getIconImages() {
        return new ArrayList<Image>(icons);
    }

    // ------------------------------------------------------------ events

    public void addWindowListener(WindowListener l) {
        if (l != null) {
            if (windowListeners == null) {
                windowListeners = new ArrayList<WindowListener>();
            }
            windowListeners.add(l);
        }
    }

    public void removeWindowListener(WindowListener l) {
        if (windowListeners != null) {
            windowListeners.remove(l);
        }
    }

    public WindowListener[] getWindowListeners() {
        return windowListeners == null ? new WindowListener[0]
                : windowListeners.toArray(new WindowListener[windowListeners.size()]);
    }

    public void addWindowFocusListener(WindowFocusListener l) {
        if (l != null) {
            if (windowFocusListeners == null) {
                windowFocusListeners = new ArrayList<WindowFocusListener>();
            }
            windowFocusListeners.add(l);
        }
    }

    public void removeWindowFocusListener(WindowFocusListener l) {
        if (windowFocusListeners != null) {
            windowFocusListeners.remove(l);
        }
    }

    public WindowFocusListener[] getWindowFocusListeners() {
        return windowFocusListeners == null ? new WindowFocusListener[0]
                : windowFocusListeners.toArray(new WindowFocusListener[windowFocusListeners.size()]);
    }

    public void addWindowStateListener(WindowStateListener l) {
        if (l != null) {
            if (windowStateListeners == null) {
                windowStateListeners = new ArrayList<WindowStateListener>();
            }
            windowStateListeners.add(l);
        }
    }

    public void removeWindowStateListener(WindowStateListener l) {
        if (windowStateListeners != null) {
            windowStateListeners.remove(l);
        }
    }

    public WindowStateListener[] getWindowStateListeners() {
        return windowStateListeners == null ? new WindowStateListener[0]
                : windowStateListeners.toArray(new WindowStateListener[windowStateListeners.size()]);
    }

    @Override
    protected void processEvent(AWTEvent e) {
        if (e instanceof WindowEvent) {
            int id = e.getID();
            if (id == WindowEvent.WINDOW_GAINED_FOCUS || id == WindowEvent.WINDOW_LOST_FOCUS) {
                processWindowFocusEvent((WindowEvent) e);
            } else if (id == WindowEvent.WINDOW_STATE_CHANGED) {
                processWindowStateEvent((WindowEvent) e);
            } else {
                processWindowEvent((WindowEvent) e);
            }
        } else {
            super.processEvent(e);
        }
    }

    protected void processWindowEvent(WindowEvent e) {
        WindowListener[] ls = getWindowListeners();
        for (int i = 0; i < ls.length; i++) {
            switch (e.getID()) {
                case WindowEvent.WINDOW_OPENED:
                    ls[i].windowOpened(e);
                    break;
                case WindowEvent.WINDOW_CLOSING:
                    ls[i].windowClosing(e);
                    break;
                case WindowEvent.WINDOW_CLOSED:
                    ls[i].windowClosed(e);
                    break;
                case WindowEvent.WINDOW_ICONIFIED:
                    ls[i].windowIconified(e);
                    break;
                case WindowEvent.WINDOW_DEICONIFIED:
                    ls[i].windowDeiconified(e);
                    break;
                case WindowEvent.WINDOW_ACTIVATED:
                    ls[i].windowActivated(e);
                    break;
                case WindowEvent.WINDOW_DEACTIVATED:
                    ls[i].windowDeactivated(e);
                    break;
                default:
                    break;
            }
        }
    }

    protected void processWindowFocusEvent(WindowEvent e) {
        WindowFocusListener[] ls = getWindowFocusListeners();
        for (int i = 0; i < ls.length; i++) {
            if (e.getID() == WindowEvent.WINDOW_GAINED_FOCUS) {
                ls[i].windowGainedFocus(e);
            } else {
                ls[i].windowLostFocus(e);
            }
        }
    }

    protected void processWindowStateEvent(WindowEvent e) {
        WindowStateListener[] ls = getWindowStateListeners();
        for (int i = 0; i < ls.length; i++) {
            ls[i].windowStateChanged(e);
        }
    }
}
