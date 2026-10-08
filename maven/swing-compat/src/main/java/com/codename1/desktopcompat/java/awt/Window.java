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
import com.codename1.desktopcompat.java.awt.event.WindowListener;
import com.codename1.desktopcompat.rt.EventBridge;
import com.codename1.desktopcompat.rt.FrameForm;
import com.codename1.desktopcompat.rt.Units;
import com.codename1.ui.Display;
import java.util.ArrayList;

/// A top level window, shown as a Codename One form that it fills.
///
/// The size and position an application sets are recorded, but the window
/// is given the size of the form once it shows. Showing a second window
/// puts its form on top; hiding or disposing it goes back to the one
/// before. Windows start hidden.
public class Window extends Container {

    private final Window owner;
    private ArrayList<WindowListener> windowListeners;
    private FrameForm form;
    private boolean visible;
    private boolean opened;
    private Image iconImage;

    public Window(Frame owner) {
        this((Window) owner);
    }

    public Window(Window owner) {
        this.owner = owner;
        setLayout(new BorderLayout());
    }

    /// The form showing this window, `null` until it is shown.
    public FrameForm cn1Form() {
        return form;
    }

    /// The title the form carries; frames and dialogs answer theirs.
    public String cn1Title() {
        return "";
    }

    /// Runs what a user's request to close the window runs.
    public void cn1Closing() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    @Override
    public void cn1SyncPeerBounds() {
    }

    public Window getOwner() {
        return owner;
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

    /// Sizes the window to its preferred size where it has none yet and
    /// lays it out. Once shown the window keeps the size of its form.
    public void pack() {
        if (!isDisplayable()) {
            addNotify();
        }
        if (form == null) {
            Dimension d = getPreferredSize();
            setSize(d.width, d.height);
        }
        validate();
    }

    @Override
    public void setVisible(boolean b) {
        if (b == visible) {
            return;
        }
        visible = b;
        if (b) {
            if (!isDisplayable()) {
                addNotify();
            }
            if (Display.isInitialized()) {
                if (form == null) {
                    form = new FrameForm(this);
                }
                form.cn1Show();
            } else {
                validate();
            }
            if (!opened) {
                opened = true;
                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_OPENED));
            }
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_ACTIVATED));
        } else {
            if (form != null) {
                form.cn1Hide();
            }
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_DEACTIVATED));
        }
    }

    /// Hides the window and releases its form.
    public void dispose() {
        boolean was = isDisplayable();
        setVisible(false);
        if (was) {
            removeNotify();
            form = null;
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSED));
        }
    }

    public void toFront() {
        if (visible && form != null) {
            form.cn1Show();
        }
    }

    public void toBack() {
    }

    public boolean isActive() {
        return visible && form != null && Display.isInitialized() && Display.getInstance().getCurrent() == form;
    }

    public boolean isFocused() {
        return isActive();
    }

    public Component getFocusOwner() {
        Component c = EventBridge.focusOwner();
        return c != null && (c == this || isAncestorOf(c)) ? c : null;
    }

    /// Does nothing: a window fills its form.
    public void setLocationRelativeTo(Component c) {
    }

    public void setIconImage(Image image) {
        iconImage = image;
    }

    public java.util.List<Image> getIconImages() {
        ArrayList<Image> l = new ArrayList<Image>();
        if (iconImage != null) {
            l.add(iconImage);
        }
        return l;
    }

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

    @Override
    protected void processEvent(AWTEvent e) {
        if (e instanceof WindowEvent) {
            processWindowEvent((WindowEvent) e);
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
}
