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

/// A titled window owned by another. It is shown as a form like any
/// window, and a modal dialog does not block: `setVisible(true)` returns at
/// once.
public class Dialog extends Window {

    private String title;
    private boolean modal;
    private boolean resizable = true;
    private boolean undecorated;

    public Dialog(Frame owner) {
        this(owner, "", false);
    }

    public Dialog(Frame owner, boolean modal) {
        this(owner, "", modal);
    }

    public Dialog(Frame owner, String title) {
        this(owner, title, false);
    }

    public Dialog(Frame owner, String title, boolean modal) {
        super(owner);
        this.title = title == null ? "" : title;
        this.modal = modal;
    }

    public Dialog(Dialog owner) {
        this(owner, "", false);
    }

    public Dialog(Dialog owner, String title) {
        this(owner, title, false);
    }

    public Dialog(Dialog owner, String title, boolean modal) {
        super((Window) owner);
        this.title = title == null ? "" : title;
        this.modal = modal;
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

    public boolean isModal() {
        return modal;
    }

    /// Recorded only; see the class description.
    public void setModal(boolean modal) {
        this.modal = modal;
    }

    public boolean isResizable() {
        return resizable;
    }

    public void setResizable(boolean resizable) {
        this.resizable = resizable;
    }

    public boolean isUndecorated() {
        return undecorated;
    }

    public void setUndecorated(boolean undecorated) {
        this.undecorated = undecorated;
    }
}
