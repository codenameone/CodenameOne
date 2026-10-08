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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.rt.Icons;
import com.codename1.desktopcompat.rt.LabelPeer;

/// A line of text, an icon, or both, shown by a Codename One label.
///
/// The horizontal alignment is honoured; the vertical alignment and the
/// text position relative to the icon are recorded and the label keeps
/// Codename One's arrangement (icon before the text, centered vertically).
/// HTML text is shown as it is written.
public class JLabel extends JComponent implements SwingConstants {

    private String text;
    private Icon icon;
    private Icon disabledIcon;
    private int horizontalAlignment;
    private int verticalAlignment = CENTER;
    private int horizontalTextPosition = TRAILING;
    private int verticalTextPosition = CENTER;
    private int iconTextGap = 4;
    private Component labelFor;

    public JLabel(String text, Icon icon, int horizontalAlignment) {
        this.text = text;
        this.icon = icon;
        setHorizontalAlignment(horizontalAlignment);
        setAlignmentX(LEFT_ALIGNMENT);
    }

    public JLabel(String text, int horizontalAlignment) {
        this(text, null, horizontalAlignment);
    }

    public JLabel(String text) {
        this(text, null, LEADING);
    }

    public JLabel(Icon image, int horizontalAlignment) {
        this(null, image, horizontalAlignment);
    }

    public JLabel(Icon image) {
        this(null, image, CENTER);
    }

    public JLabel() {
        this("", null, LEADING);
    }

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        return new LabelPeer(this);
    }

    @Override
    protected void cn1PeerCreated() {
        super.cn1PeerCreated();
        sync();
    }

    private void sync() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof com.codename1.ui.Label) {
            com.codename1.ui.Label l = (com.codename1.ui.Label) p;
            l.setText(text == null ? "" : text);
            l.setIcon(Icons.toNative(icon, this));
            l.setAlignment(nativeAlignment(horizontalAlignment));
            l.setGap(com.codename1.desktopcompat.rt.Units.toDevice(iconTextGap));
        }
    }

    static int nativeAlignment(int alignment) {
        switch (alignment) {
            case CENTER:
                return com.codename1.ui.Component.CENTER;
            case RIGHT:
            case TRAILING:
                return com.codename1.ui.Component.RIGHT;
            default:
                return com.codename1.ui.Component.LEFT;
        }
    }

    private void changed() {
        sync();
        revalidate();
        repaint();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        String old = this.text;
        this.text = text;
        firePropertyChange("text", old, text);
        if (old == null ? text != null : !old.equals(text)) {
            changed();
        }
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        Icon old = this.icon;
        this.icon = icon;
        firePropertyChange("icon", old, icon);
        if (old != icon) {
            changed();
        }
    }

    public Icon getDisabledIcon() {
        return disabledIcon;
    }

    /// Recorded only.
    public void setDisabledIcon(Icon disabledIcon) {
        this.disabledIcon = disabledIcon;
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public void setHorizontalAlignment(int alignment) {
        if (alignment != LEFT && alignment != CENTER && alignment != RIGHT && alignment != LEADING
                && alignment != TRAILING) {
            throw new IllegalArgumentException("horizontalAlignment");
        }
        if (alignment != horizontalAlignment) {
            int old = horizontalAlignment;
            horizontalAlignment = alignment;
            firePropertyChange("horizontalAlignment", old, alignment);
            sync();
            repaint();
        }
    }

    public int getVerticalAlignment() {
        return verticalAlignment;
    }

    public void setVerticalAlignment(int alignment) {
        if (alignment != TOP && alignment != CENTER && alignment != BOTTOM) {
            throw new IllegalArgumentException("verticalAlignment");
        }
        verticalAlignment = alignment;
    }

    public int getHorizontalTextPosition() {
        return horizontalTextPosition;
    }

    public void setHorizontalTextPosition(int textPosition) {
        horizontalTextPosition = textPosition;
    }

    public int getVerticalTextPosition() {
        return verticalTextPosition;
    }

    public void setVerticalTextPosition(int textPosition) {
        verticalTextPosition = textPosition;
    }

    public int getIconTextGap() {
        return iconTextGap;
    }

    public void setIconTextGap(int iconTextGap) {
        if (iconTextGap != this.iconTextGap) {
            this.iconTextGap = iconTextGap;
            changed();
        }
    }

    public Component getLabelFor() {
        return labelFor;
    }

    public void setLabelFor(Component c) {
        labelFor = c;
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",text=" + text;
    }
}
