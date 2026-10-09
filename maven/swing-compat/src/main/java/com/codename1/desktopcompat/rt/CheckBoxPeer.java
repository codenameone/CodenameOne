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
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;

/// The peer of a check box, and of a toggle button when made a toggle: a
/// Codename One check box.
public class CheckBoxPeer extends com.codename1.ui.CheckBox implements Peer {

    private final PeerSupport support;

    public CheckBoxPeer(Component owner) {
        support = new PeerSupport(owner, this);
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

    /// Whether the theme draws a toggle button that is on exactly as one
    /// that is off. Codename One draws the first in the pressed style, or
    /// in the selected one while it has the focus -- which it has right
    /// after the click that turned it on -- and a theme that never styled
    /// toggle buttons gives all of them the same background and border; a
    /// different text colour does not show on a button that is only an
    /// icon.
    private boolean onLooksLikeOff() {
        Style off = getUnselectedStyle();
        Style on = getStyle();
        if (on == off) {
            return true;
        }
        if (off.getBgColor() != on.getBgColor() || off.getBgTransparency() != on.getBgTransparency()
                || off.getBgImage() != on.getBgImage()) {
            return false;
        }
        Border a = off.getBorder();
        Border b = on.getBorder();
        if (a != b && (a == null || !a.equals(b))) {
            return false;
        }
        String text = getText();
        return text == null || text.length() == 0 || off.getFgColor() == on.getFgColor();
    }

    @Override
    public void paintNativeLook(Graphics g) {
        support.paintStyleBackground(g);
        if (isToggle() && isSelected() && onLooksLikeOff()) {
            // The state has to show: a wash of the text colour, as a
            // pressed key looks in most themes.
            int alpha = g.getAlpha();
            int color = g.getColor();
            int arc = Math.max(2, Units.toDevice(6));
            g.setColor(getUnselectedStyle().getFgColor());
            g.setAlpha(56);
            g.fillRoundRect(getX(), getY(), getWidth(), getHeight(), arc, arc);
            g.setAlpha(alpha);
            g.setColor(color);
        }
        if (getIcon() != null) {
            // In Swing an icon takes the place of the indicator; it is not
            // drawn beside it.
            getUIManager().getLookAndFeel().drawButton(g, this);
        } else {
            super.paint(g);
        }
        super.paintBorder(g);
    }

    /// With an icon there is no indicator to make room for.
    @Override
    protected com.codename1.ui.geom.Dimension calcPreferredSize() {
        if (getIcon() != null) {
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
