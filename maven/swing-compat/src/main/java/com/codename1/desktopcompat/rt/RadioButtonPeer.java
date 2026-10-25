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

/// The peer of a radio button: a Codename One radio button. Grouping is
/// done by the Swing button group, not by Codename One.
public class RadioButtonPeer extends com.codename1.ui.RadioButton implements Peer {

    private final PeerSupport support;

    public RadioButtonPeer(Component owner) {
        support = new PeerSupport(owner, this);
        // The room between the mark and its text.
        setGap(Units.toDevice(4));
        support.trackFocus();
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
        if (getIcon() != null || ownMark()) {
            // In Swing an icon takes the place of the indicator; it is not
            // drawn beside it. Without one the indicator is the mark drawn
            // here, which the button shows as its icon.
            if (ownMark()) {
                Indicators.labelText(this, support);
            }
            getUIManager().getLookAndFeel().drawButton(g, this);
        } else {
            super.paint(g);
        }
        super.paintBorder(g);
    }

    /// Whether the indicator is the mark drawn here rather than the
    /// theme's: always, unless the button is a toggle or shows an icon in
    /// its place.
    private boolean ownMark() {
        return !isToggle() && getIcon() == null;
    }

    private com.codename1.ui.Image mark() {
        return Indicators.mark(true, isSelected(), isEnabled(), Indicators.size(getUnselectedStyle()));
    }

    @Override
    public com.codename1.ui.Image getIconFromState() {
        return ownMark() ? mark() : super.getIconFromState();
    }

    @Override
    public com.codename1.ui.Image getMaskedIcon() {
        return ownMark() ? mark() : super.getMaskedIcon();
    }

    /// With an icon there is no indicator to make room for, and the mark
    /// drawn here is measured as one.
    @Override
    protected com.codename1.ui.geom.Dimension calcPreferredSize() {
        if (ownMark()) {
            Indicators.labelText(this, support);
        }
        if (getIcon() != null || ownMark()) {
            return getUIManager().getLookAndFeel().getButtonPreferredSize(this);
        }
        return super.calcPreferredSize();
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
