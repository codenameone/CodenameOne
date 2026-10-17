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
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import java.util.Date;

/// The peer of a date picker: a Codename One picker of dates, which shows
/// the date as a button and opens the platform's date chooser when it is
/// tapped.
///
/// The text on the button is the owner's business: the picker asks its
/// [Host] for it whenever its value changes, and tells the host of every
/// value it takes, whether set by code or chosen by the user.
public class DatePickerPeer extends com.codename1.ui.spinner.Picker implements Peer {

    /// What the component behind the picker supplies.
    public interface Host {

        /// The text that shows `date`, which is `null` when nothing is
        /// chosen.
        String text(Date date);

        /// The picker now holds `date`.
        void valueChanged(Date date);
    }

    private final PeerSupport support;
    private final Host host;

    public DatePickerPeer(Component owner, Host host) {
        setType(Display.PICKER_TYPE_DATE);
        support = new PeerSupport(owner, this);
        support.trackFocus();
        this.host = host;
    }

    /// Shows the host's text for the value and reports the value. Called
    /// by the picker for every change, the first time from its own
    /// constructor, before there is a host.
    @Override
    protected void updateValue() {
        if (host == null) {
            super.updateValue();
            return;
        }
        Object v = getValue();
        Date d = v instanceof Date ? (Date) v : null;
        setText(host.text(d));
        host.valueChanged(d);
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
        super.paintBorder(g);
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
