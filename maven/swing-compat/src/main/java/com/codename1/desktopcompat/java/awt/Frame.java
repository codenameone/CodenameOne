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
import java.util.ArrayList;

/// A window with a title.
///
/// The title shows in the title area of the form, or the title bar of
/// the window where there is a window manager. Resizability, decoration
/// and the extended state -- iconified, maximized -- apply to a window of
/// the window manager and are only recorded for a frame that fills a
/// form, which is always the size of the screen.
public class Frame extends Window {

    public static final int NORMAL = 0;
    public static final int ICONIFIED = 1;
    public static final int MAXIMIZED_HORIZ = 2;
    public static final int MAXIMIZED_VERT = 4;
    public static final int MAXIMIZED_BOTH = 6;

    private String title;
    private boolean resizable = true;
    private boolean undecorated;
    private int state;

    public Frame() {
        this("");
    }

    public Frame(String title) {
        super((Window) null);
        this.title = title == null ? "" : title;
    }

    /// The frames that were shown or packed and not disposed of since.
    public static Frame[] getFrames() {
        Window[] all = Window.getWindows();
        ArrayList<Frame> l = new ArrayList<Frame>();
        for (int i = 0; i < all.length; i++) {
            if (all[i] instanceof Frame) {
                l.add((Frame) all[i]);
            }
        }
        return l.toArray(new Frame[l.size()]);
    }

    @Override
    public String cn1Title() {
        return title;
    }

    @Override
    protected boolean cn1Resizable() {
        return resizable;
    }

    @Override
    protected boolean cn1Decorated() {
        return !undecorated;
    }

    @Override
    protected int cn1State() {
        return state;
    }

    @Override
    public void cn1Iconified(boolean iconified) {
        int old = state;
        state = iconified ? state | ICONIFIED : state & ~ICONIFIED;
        super.cn1Iconified(iconified);
        if (old != state) {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_STATE_CHANGED, old, state));
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        String old = this.title;
        this.title = title == null ? "" : title;
        if (cn1Host() != null) {
            cn1Host().title(this.title);
        }
        firePropertyChange("title", old, this.title);
    }

    public Image getIconImage() {
        java.util.List<Image> l = getIconImages();
        return l.isEmpty() ? null : l.get(0);
    }

    public boolean isResizable() {
        return resizable;
    }

    public void setResizable(boolean resizable) {
        boolean old = this.resizable;
        this.resizable = resizable;
        if (cn1Host() != null) {
            cn1Host().resizable(resizable);
        }
        firePropertyChange("resizable", old, resizable);
    }

    public boolean isUndecorated() {
        return undecorated;
    }

    /// Takes effect the next time the frame is shown.
    public void setUndecorated(boolean undecorated) {
        this.undecorated = undecorated;
    }

    public int getExtendedState() {
        return state;
    }

    /// Minimizes, maximizes or restores a window of the window manager,
    /// and tells the window state listeners. A frame that fills a form
    /// records the state and stays as it is.
    public void setExtendedState(int state) {
        int old = this.state;
        if (old == state) {
            return;
        }
        this.state = state;
        if (cn1Host() != null) {
            cn1Host().state(state);
        }
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_STATE_CHANGED, old, state));
    }

    public int getState() {
        return (state & ICONIFIED) != 0 ? ICONIFIED : NORMAL;
    }

    public void setState(int state) {
        setExtendedState(state == ICONIFIED ? this.state | ICONIFIED : this.state & ~ICONIFIED);
    }
}
