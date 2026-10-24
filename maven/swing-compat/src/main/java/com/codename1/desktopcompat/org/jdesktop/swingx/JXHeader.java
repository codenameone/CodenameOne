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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.LayoutManager;
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.SwingConstants;
import com.codename1.desktopcompat.rt.Fonts;

/// The banner at the top of a form or dialog: a bold title, a description
/// under it, and an icon at one end.
///
/// The three parts are Codename One labels laid out by the header: the
/// icon at the right or left end and centered vertically, the title at
/// the top, the description under it and indented.
///
/// ## What differs from SwingX
///
///  - There is no look and feel delegate, so no gradient is drawn behind
///    the header; set a background painter for one.
///  - The description is one line and is cut off when it does not fit;
///    it does not wrap.
public class JXHeader extends JXPanel {

    /// The end of the header the icon is at.
    public enum IconPosition {
        LEFT,
        RIGHT
    }

    private static final int PAD = 12;

    private final JLabel titleLabel = new JLabel();
    private final JLabel descriptionLabel = new JLabel();
    private final JLabel iconLabel = new JLabel();
    private String title;
    private String description;
    private Icon icon;
    private Font titleFont;
    private Font descriptionFont;
    private Color titleForeground;
    private Color descriptionForeground;
    private IconPosition iconPosition = IconPosition.RIGHT;

    public JXHeader() {
        this(null, null);
    }

    public JXHeader(String title, String description) {
        this(title, description, null);
    }

    public JXHeader(String title, String description, Icon icon) {
        super(new Banner());
        titleLabel.setFont(Fonts.defaultFont().deriveFont(Font.BOLD));
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
        descriptionLabel.setHorizontalAlignment(SwingConstants.LEFT);
        add(titleLabel);
        add(descriptionLabel);
        add(iconLabel);
        setTitle(title);
        setDescription(description);
        setIcon(icon);
    }

    /// Lays out the title, the description and the icon, which are the
    /// header's first three children in that order.
    private static final class Banner implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        private static Dimension size(Container parent, int index) {
            if (parent.getComponentCount() <= index || !parent.getComponent(index).isVisible()) {
                return new Dimension(0, 0);
            }
            return parent.getComponent(index).getPreferredSize();
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Insets in = parent.getInsets();
            Dimension t = size(parent, 0);
            Dimension d = size(parent, 1);
            Dimension i = size(parent, 2);
            int text = Math.max(t.width, d.width + PAD);
            int w = text + (i.width > 0 ? i.width + PAD : 0);
            int h = Math.max(t.height + d.height, i.height);
            return new Dimension(w + 2 * PAD + in.left + in.right, h + 2 * PAD + in.top + in.bottom);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override
        public void layoutContainer(Container parent) {
            if (parent.getComponentCount() < 3 || !(parent instanceof JXHeader)) {
                return;
            }
            Insets in = parent.getInsets();
            int left = in.left + PAD;
            int right = parent.getWidth() - in.right - PAD;
            int top = in.top + PAD;
            int height = Math.max(0, parent.getHeight() - in.top - in.bottom - 2 * PAD);
            Dimension i = size(parent, 2);
            if (i.width > 0) {
                int ih = Math.min(height, i.height);
                int iy = top + (height - ih) / 2;
                if (((JXHeader) parent).iconPosition == IconPosition.LEFT) {
                    parent.getComponent(2).setBounds(left, iy, i.width, ih);
                    left += i.width + PAD;
                } else {
                    right -= i.width;
                    parent.getComponent(2).setBounds(right, iy, i.width, ih);
                    right -= PAD;
                }
            }
            int width = Math.max(0, right - left);
            Dimension t = size(parent, 0);
            parent.getComponent(0).setBounds(left, top, width, t.height);
            Dimension d = size(parent, 1);
            parent.getComponent(1).setBounds(left + PAD, top + t.height, Math.max(0, width - PAD), d.height);
        }
    }

    public void setTitle(String title) {
        String old = this.title;
        this.title = title;
        titleLabel.setText(title == null ? "" : title);
        firePropertyChange("title", old, title);
        revalidate();
    }

    public String getTitle() {
        return title;
    }

    public void setDescription(String description) {
        String old = this.description;
        this.description = description;
        descriptionLabel.setText(description == null ? "" : description);
        firePropertyChange("description", old, description);
        revalidate();
    }

    public String getDescription() {
        return description;
    }

    public void setIcon(Icon icon) {
        Icon old = this.icon;
        this.icon = icon;
        iconLabel.setIcon(icon);
        iconLabel.setVisible(icon != null);
        firePropertyChange("icon", old, icon);
        revalidate();
    }

    public Icon getIcon() {
        return icon;
    }

    public void setTitleFont(Font font) {
        Font old = titleFont;
        titleFont = font;
        titleLabel.setFont(font != null ? font : Fonts.defaultFont().deriveFont(Font.BOLD));
        firePropertyChange("titleFont", old, font);
        revalidate();
    }

    /// The font set for the title, or `null` for the default bold one.
    public Font getTitleFont() {
        return titleFont;
    }

    public void setDescriptionFont(Font font) {
        Font old = descriptionFont;
        descriptionFont = font;
        descriptionLabel.setFont(font);
        firePropertyChange("descriptionFont", old, font);
        revalidate();
    }

    /// The font set for the description, or `null` for the default one.
    public Font getDescriptionFont() {
        return descriptionFont;
    }

    /// The color set for the title, or `null` for the theme's.
    public Color getTitleForeground() {
        return titleForeground;
    }

    public void setTitleForeground(Color titleForeground) {
        Color old = this.titleForeground;
        this.titleForeground = titleForeground;
        titleLabel.setForeground(titleForeground);
        firePropertyChange("titleForeground", old, titleForeground);
    }

    /// The color set for the description, or `null` for the theme's.
    public Color getDescriptionForeground() {
        return descriptionForeground;
    }

    public void setDescriptionForeground(Color descriptionForeground) {
        Color old = this.descriptionForeground;
        this.descriptionForeground = descriptionForeground;
        descriptionLabel.setForeground(descriptionForeground);
        firePropertyChange("descriptionForeground", old, descriptionForeground);
    }

    public IconPosition getIconPosition() {
        return iconPosition;
    }

    public void setIconPosition(IconPosition iconPosition) {
        IconPosition old = this.iconPosition;
        this.iconPosition = iconPosition == null ? IconPosition.RIGHT : iconPosition;
        firePropertyChange("iconPosition", old, this.iconPosition);
        revalidate();
    }
}
