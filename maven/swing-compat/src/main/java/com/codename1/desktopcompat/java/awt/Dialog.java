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

import com.codename1.io.Util;
import com.codename1.ui.Display;

/// A titled window owned by another.
///
/// **A modal dialog blocks.** `setVisible(true)` on a modal dialog, called
/// on the event dispatch thread, returns only once the dialog was hidden
/// or disposed of. Meanwhile the event dispatch thread keeps running
/// events -- painting, input, timers, `invokeLater` -- from inside that
/// call, exactly as a nested event loop does on the desktop. The three
/// modal modality types behave the same: other windows get no input.
///
/// Called on another thread, with no display, or on a modeless dialog,
/// `setVisible(true)` returns at once.
public class Dialog extends Window {

    /// How a dialog keeps input from other windows. Everything but
    /// `MODELESS` blocks the caller of `setVisible(true)`.
    public enum ModalityType {
        MODELESS,
        DOCUMENT_MODAL,
        APPLICATION_MODAL,
        TOOLKIT_MODAL
    }

    /// Which windows a modal dialog leaves usable. Asked for with
    /// `Window.setModalExclusionType`; see there for what is honoured.
    public enum ModalExclusionType {
        NO_EXCLUDE,
        APPLICATION_EXCLUDE,
        TOOLKIT_EXCLUDE
    }

    public static final ModalityType DEFAULT_MODALITY_TYPE = ModalityType.APPLICATION_MODAL;

    private static final Runnable PAUSE = new Pause();

    private String title;
    private ModalityType modality;
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
        this((Window) owner, title, modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS);
    }

    public Dialog(Dialog owner) {
        this(owner, "", false);
    }

    public Dialog(Dialog owner, String title) {
        this(owner, title, false);
    }

    public Dialog(Dialog owner, String title, boolean modal) {
        this((Window) owner, title, modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS);
    }

    public Dialog(Window owner) {
        this(owner, "", ModalityType.MODELESS);
    }

    public Dialog(Window owner, String title) {
        this(owner, title, ModalityType.MODELESS);
    }

    public Dialog(Window owner, ModalityType modalityType) {
        this(owner, "", modalityType);
    }

    public Dialog(Window owner, String title, ModalityType modalityType) {
        super(owner);
        this.title = title == null ? "" : title;
        this.modality = modalityType == null ? ModalityType.MODELESS : modalityType;
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

    public boolean isModal() {
        return modality != ModalityType.MODELESS;
    }

    /// Takes effect the next time the dialog is shown.
    public void setModal(boolean modal) {
        modality = modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS;
    }

    public ModalityType getModalityType() {
        return modality;
    }

    /// Takes effect the next time the dialog is shown.
    public void setModalityType(ModalityType type) {
        modality = type == null ? ModalityType.MODELESS : type;
    }

    /// Shows or hides the dialog. Showing a modal dialog blocks; see the
    /// class description.
    @Override
    public void setVisible(boolean b) {
        boolean was = isVisible();
        super.setVisible(b);
        if (b && !was && isModal() && Display.isInitialized() && Display.getInstance().isEdt()) {
            while (isVisible()) {
                Display.getInstance().invokeAndBlock(PAUSE);
            }
        }
    }

    public boolean isResizable() {
        return resizable;
    }

    public void setResizable(boolean resizable) {
        this.resizable = resizable;
        if (cn1Host() != null) {
            cn1Host().resizable(resizable);
        }
    }

    public boolean isUndecorated() {
        return undecorated;
    }

    /// Takes effect the next time the dialog is shown.
    public void setUndecorated(boolean undecorated) {
        this.undecorated = undecorated;
    }

    /// What the blocked caller waits in between two looks at whether the
    /// dialog is still showing; the event dispatch thread runs events
    /// meanwhile.
    private static final class Pause implements Runnable {
        @Override
        public void run() {
            Util.sleep(15);
        }
    }
}
