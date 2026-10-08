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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.list.ListModel;

/// The peer of a combo box: a Codename One combo box, which draws the
/// closed box and shows the popup the platform's way.
public class ComboPeer extends com.codename1.ui.ComboBox<Object> implements Peer {

    /// Told when the popup opens and when it has closed.
    public interface PopupWatcher {
        void popupOpening();

        /// `cancelled` when the popup was closed from code rather than by
        /// the user picking a row or dismissing it.
        void popupClosed(boolean cancelled);
    }

    private final PeerSupport support;
    private PopupWatcher watcher;
    private com.codename1.ui.Dialog popup;
    private boolean closedFromCode;

    public ComboPeer(Component owner, ListModel<Object> model) {
        super(model);
        support = new PeerSupport(owner, this);
        support.trackFocus();
    }

    public void setPopupWatcher(PopupWatcher watcher) {
        this.watcher = watcher;
    }

    /// Opens the popup as a tap on the box would.
    public void openPopup() {
        if (isEnabled() && !isShowingPopupDialog()) {
            fireClicked();
        }
    }

    /// Closes an open popup without taking what was highlighted in it.
    public void closePopup() {
        com.codename1.ui.Dialog d = popup;
        if (d != null && isShowingPopupDialog()) {
            closedFromCode = true;
            d.dispose();
        }
    }

    @Override
    protected com.codename1.ui.Dialog createPopupDialog(com.codename1.ui.List<Object> l) {
        popup = super.createPopupDialog(l);
        return popup;
    }

    /// The popup is modal: this returns when it has closed. The owner is
    /// told on both sides, so what the user highlights on the way is not a
    /// selection until the popup is closed with it.
    @Override
    protected void fireClicked() {
        closedFromCode = false;
        if (watcher != null) {
            watcher.popupOpening();
        }
        try {
            super.fireClicked();
        } finally {
            popup = null;
            if (watcher != null) {
                watcher.popupClosed(closedFromCode);
            }
        }
    }

    @Override
    public PeerSupport support() {
        return support;
    }

    @Override
    public boolean nativeLook() {
        return true;
    }

    @Override
    public void paintNativeLook(Graphics g) {
        support.paintStyleBackground(g);
        super.paint(g);
    }

    @Override
    public void paintNativeChildren(Graphics g) {
    }

    @Override
    public void paint(Graphics g) {
        support.paintOwner(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
    }

    @Override
    public void repaint() {
        if (support == null || !support.routeRepaint()) {
            super.repaint();
        }
    }
}
