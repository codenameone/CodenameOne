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

import com.codename1.desktopcompat.java.awt.AWTEvent;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.accessibility.Accessible;
import com.codename1.desktopcompat.rt.Align;
import com.codename1.desktopcompat.rt.Icons;
import com.codename1.desktopcompat.rt.LabelPeer;
import com.codename1.desktopcompat.rt.MiniHtml;
import com.codename1.desktopcompat.rt.Units;

/// A line of text, an icon, or both, shown by a Codename One label.
///
/// The alignments and the text position relative to the icon are
/// honoured, with one limit: Codename One places text on one of the four
/// sides of the icon, so text centered over the icon is placed after it.
/// A disabled label shows its disabled icon if it was given one.
///
/// A text that starts with `<html>` is drawn by the label itself from
/// what [com.codename1.desktopcompat.rt.MiniHtml] makes of it: bold,
/// italic, underline, colors, sizes, line breaks, paragraphs and
/// centering. Its preferred size is that of its unwrapped lines; in a
/// narrower label the lines wrap at spaces.
///
/// The displayed mnemonic is recorded only. A click on a label focuses
/// the component it was made the label for.
public class JLabel extends JComponent implements Accessible, SwingConstants {

    private String text;
    private Icon icon;
    private Icon disabledIcon;
    private int horizontalAlignment;
    private int verticalAlignment = CENTER;
    private int horizontalTextPosition = TRAILING;
    private int verticalTextPosition = CENTER;
    private int iconTextGap = 4;
    private Component labelFor;
    private int mnemonic;
    private int mnemonicIndex = -1;
    private MiniHtml.Document html;

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
            boolean isHtml = MiniHtml.isHtml(text);
            l.setText(text == null || isHtml ? "" : text);
            l.setIcon(isHtml ? null : Icons.toNative(shownIcon(), this));
            l.setAlignment(nativeAlignment(horizontalAlignment));
            l.setVerticalAlignment(Align.vertical(verticalAlignment));
            l.setTextPosition(Align.textPosition(horizontalTextPosition, verticalTextPosition));
            l.setGap(Units.toDevice(iconTextGap));
        }
    }

    private Icon shownIcon() {
        return !isEnabled() && disabledIcon != null ? disabledIcon : icon;
    }

    // ------------------------------------------------------------ html

    private MiniHtml.Document document() {
        if (html == null) {
            html = MiniHtml.parse(text);
        }
        return html;
    }

    private Font htmlFont() {
        return getFont();
    }

    /// The space around the content of an HTML label: the border's, and
    /// the padding the theme gives a label, so that it lines up with the
    /// labels Codename One draws.
    private Insets htmlInsets() {
        Insets in = getInsets();
        Insets out = new Insets(in.top, in.left, in.bottom, in.right);
        if (com.codename1.ui.Display.isInitialized()) {
            com.codename1.ui.plaf.Style st = cn1Peer().getStyle();
            out.top += Units.toLogicalCeil(st.getPaddingTop());
            out.bottom += Units.toLogicalCeil(st.getPaddingBottom());
            out.left += Units.toLogicalCeil(st.getPaddingLeftNoRTL());
            out.right += Units.toLogicalCeil(st.getPaddingRightNoRTL());
        }
        return out;
    }

    private boolean iconBeside() {
        return horizontalTextPosition != CENTER || verticalTextPosition == CENTER;
    }

    /// The size of the icon and the text together, given the text's.
    private Dimension htmlContent(Dimension textSize) {
        Icon ic = shownIcon();
        if (ic == null) {
            return textSize;
        }
        if (iconBeside()) {
            return new Dimension(ic.getIconWidth() + iconTextGap + textSize.width,
                    Math.max(ic.getIconHeight(), textSize.height));
        }
        return new Dimension(Math.max(ic.getIconWidth(), textSize.width),
                ic.getIconHeight() + iconTextGap + textSize.height);
    }

    @Override
    protected Dimension cn1NativePreferredSize() {
        if (!MiniHtml.isHtml(text)) {
            return super.cn1NativePreferredSize();
        }
        Dimension d = htmlContent(MiniHtml.preferredSize(document(), htmlFont()));
        Insets in = htmlInsets();
        return new Dimension(d.width + in.left + in.right, d.height + in.top + in.bottom);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!MiniHtml.isHtml(text)) {
            super.paintComponent(g);
            return;
        }
        if (isOpaque()) {
            Color bg = getBackground();
            if (bg != null) {
                g.setColor(bg);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
        Insets in = htmlInsets();
        int availW = Math.max(0, getWidth() - in.left - in.right);
        int availH = Math.max(0, getHeight() - in.top - in.bottom);
        Font f = htmlFont();
        Icon ic = shownIcon();
        boolean beside = iconBeside();
        int textAvail = ic != null && beside ? Math.max(1, availW - ic.getIconWidth() - iconTextGap) : availW;
        MiniHtml.Document doc = MiniHtml.wrap(document(), f, textAvail);
        Dimension textSize = MiniHtml.preferredSize(doc, f);
        Dimension content = htmlContent(textSize);
        int x = in.left;
        int nativeAlign = nativeAlignment(horizontalAlignment);
        if (nativeAlign == com.codename1.ui.Component.CENTER) {
            x += (availW - content.width) / 2;
        } else if (nativeAlign == com.codename1.ui.Component.RIGHT) {
            x += availW - content.width;
        }
        int y = in.top;
        if (verticalAlignment == CENTER) {
            y += (availH - content.height) / 2;
        } else if (verticalAlignment == BOTTOM) {
            y += availH - content.height;
        }
        int textX = x;
        int textY = y;
        if (ic != null) {
            int iconX = x;
            int iconY = y;
            if (beside) {
                boolean textFirst = horizontalTextPosition == LEFT || horizontalTextPosition == LEADING;
                if (textFirst) {
                    iconX = x + textSize.width + iconTextGap;
                } else {
                    textX = x + ic.getIconWidth() + iconTextGap;
                }
                iconY = y + (content.height - ic.getIconHeight()) / 2;
                textY = y + (content.height - textSize.height) / 2;
            } else {
                iconX = x + (content.width - ic.getIconWidth()) / 2;
                textX = x + (content.width - textSize.width) / 2;
                if (verticalTextPosition == TOP) {
                    iconY = y + textSize.height + iconTextGap;
                } else {
                    textY = y + ic.getIconHeight() + iconTextGap;
                }
            }
            ic.paintIcon(this, g, iconX, iconY);
        }
        g.setFont(f);
        Color fg = getForeground();
        if (!isEnabled()) {
            fg = Color.GRAY;
        }
        if (fg != null) {
            g.setColor(fg);
        }
        int lineAlign = nativeAlign == com.codename1.ui.Component.CENTER ? MiniHtml.ALIGN_CENTER
                : nativeAlign == com.codename1.ui.Component.RIGHT ? MiniHtml.ALIGN_RIGHT : MiniHtml.ALIGN_LEFT;
        MiniHtml.paint(g, doc, textX, textY, textSize.width, lineAlign);
    }

    @Override
    public void setEnabled(boolean b) {
        boolean old = isEnabled();
        super.setEnabled(b);
        if (old != b && disabledIcon != null) {
            sync();
        }
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_CLICKED && labelFor != null) {
            labelFor.requestFocus();
        }
        super.processMouseEvent(e);
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
        html = null;
        firePropertyChange("text", old, text);
        setDisplayedMnemonicIndex(findMnemonic(text, mnemonic));
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

    /// Shown in place of the icon while the label is disabled.
    public void setDisabledIcon(Icon disabledIcon) {
        Icon old = this.disabledIcon;
        this.disabledIcon = disabledIcon;
        firePropertyChange("disabledIcon", old, disabledIcon);
        if (old != disabledIcon && !isEnabled()) {
            changed();
        }
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
        if (alignment != verticalAlignment) {
            int old = verticalAlignment;
            verticalAlignment = alignment;
            firePropertyChange("verticalAlignment", old, alignment);
            sync();
            repaint();
        }
    }

    public int getHorizontalTextPosition() {
        return horizontalTextPosition;
    }

    public void setHorizontalTextPosition(int textPosition) {
        if (textPosition != LEFT && textPosition != CENTER && textPosition != RIGHT && textPosition != LEADING
                && textPosition != TRAILING) {
            throw new IllegalArgumentException("horizontalTextPosition");
        }
        int old = horizontalTextPosition;
        horizontalTextPosition = textPosition;
        firePropertyChange("horizontalTextPosition", old, textPosition);
        if (old != textPosition) {
            changed();
        }
    }

    public int getVerticalTextPosition() {
        return verticalTextPosition;
    }

    public void setVerticalTextPosition(int textPosition) {
        if (textPosition != TOP && textPosition != CENTER && textPosition != BOTTOM) {
            throw new IllegalArgumentException("verticalTextPosition");
        }
        int old = verticalTextPosition;
        verticalTextPosition = textPosition;
        firePropertyChange("verticalTextPosition", old, textPosition);
        if (old != textPosition) {
            changed();
        }
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

    /// Remembers the component this label names; a click on the label
    /// then moves the focus to it.
    public void setLabelFor(Component c) {
        Component old = labelFor;
        labelFor = c;
        if (c != null) {
            enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        } else {
            disableEvents(AWTEvent.MOUSE_EVENT_MASK);
        }
        firePropertyChange("labelFor", old, c);
    }

    public int getDisplayedMnemonic() {
        return mnemonic;
    }

    /// Recorded only: no character is underlined and no key is bound.
    public void setDisplayedMnemonic(int key) {
        int old = mnemonic;
        mnemonic = key;
        firePropertyChange("displayedMnemonic", old, key);
        setDisplayedMnemonicIndex(findMnemonic(text, key));
    }

    /// The first place the mnemonic's letter stands in the text, in either
    /// case, or -1.
    private static int findMnemonic(String text, int key) {
        if (text == null || key <= 0 || key > 0xffff) {
            return -1;
        }
        int upper = key >= 'a' && key <= 'z' ? key - ('a' - 'A') : key;
        int lower = key >= 'A' && key <= 'Z' ? key + ('a' - 'A') : key;
        int u = text.indexOf((char) upper);
        int l = text.indexOf((char) lower);
        if (u < 0) {
            return l;
        }
        if (l < 0) {
            return u;
        }
        return Math.min(u, l);
    }

    public void setDisplayedMnemonic(char aChar) {
        int vk = aChar;
        if (vk >= 'a' && vk <= 'z') {
            vk -= 'a' - 'A';
        }
        setDisplayedMnemonic(vk);
    }

    public int getDisplayedMnemonicIndex() {
        return mnemonicIndex;
    }

    /// Recorded only.
    public void setDisplayedMnemonicIndex(int index) {
        int old = mnemonicIndex;
        if (index != -1) {
            int length = text == null ? 0 : text.length();
            if (index < -1 || index >= length) {
                throw new IllegalArgumentException("index == " + index);
            }
        }
        mnemonicIndex = index;
        firePropertyChange("displayedMnemonicIndex", old, index);
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",text=" + text;
    }
}
