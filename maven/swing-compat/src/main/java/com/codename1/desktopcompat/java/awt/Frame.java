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

/// A window with a title, which the form shows in its title area.
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

    @Override
    public String cn1Title() {
        return title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        String old = this.title;
        this.title = title == null ? "" : title;
        if (cn1Form() != null) {
            cn1Form().setTitle(this.title);
        }
        firePropertyChange("title", old, this.title);
    }

    public boolean isResizable() {
        return resizable;
    }

    /// Recorded only.
    public void setResizable(boolean resizable) {
        this.resizable = resizable;
    }

    public boolean isUndecorated() {
        return undecorated;
    }

    /// Recorded only.
    public void setUndecorated(boolean undecorated) {
        this.undecorated = undecorated;
    }

    public int getExtendedState() {
        return state;
    }

    /// Recorded only: a frame always fills its form.
    public void setExtendedState(int state) {
        this.state = state;
    }

    public int getState() {
        return (state & ICONIFIED) != 0 ? ICONIFIED : NORMAL;
    }

    public void setState(int state) {
        this.state = state == ICONIFIED ? this.state | ICONIFIED : this.state & ~ICONIFIED;
    }
}
