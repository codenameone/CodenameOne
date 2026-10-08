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
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.GridBagConstraints;
import com.codename1.desktopcompat.java.awt.GridBagLayout;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JSeparator;
import com.codename1.desktopcompat.javax.swing.SwingConstants;

/// A separator line with a title in it: a label, and a line that takes
/// the rest of the width.
///
/// The title is at the left with the line after it, at the right with
/// the line before it, or in the middle with a line on either side,
/// following [#setHorizontalAlignment(int)].
///
/// ## What differs from SwingX
///
///  - `LEADING` and `TRAILING` are read as left and right: the layer has
///    no component orientation, so the orientation members are absent.
public class JXTitledSeparator extends JXPanel {

    private JLabel label;
    private JSeparator leftSeparator;
    private JSeparator rightSeparator;

    /// A separator titled "Untitled".
    public JXTitledSeparator() {
        this("Untitled");
    }

    public JXTitledSeparator(String title) {
        this(title, SwingConstants.LEADING, null);
    }

    public JXTitledSeparator(String title, int horizontalAlignment) {
        this(title, horizontalAlignment, null);
    }

    public JXTitledSeparator(String title, int horizontalAlignment, Icon icon) {
        super();
        setLayout(new GridBagLayout());
        label = new JLabel(title);
        label.setIcon(icon);
        label.setHorizontalAlignment(horizontalAlignment);
        leftSeparator = new JSeparator();
        rightSeparator = new JSeparator();
        layoutSeparator();
        showTitle();
        setOpaque(false);
    }

    private static GridBagConstraints at(int x, boolean line) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        if (line) {
            c.weightx = 1.0;
            c.fill = GridBagConstraints.HORIZONTAL;
        } else {
            c.insets = new Insets(0, x == 0 ? 0 : 3, 0, 3);
        }
        return c;
    }

    private void layoutSeparator() {
        removeAll();
        int a = label.getHorizontalAlignment();
        if (a == SwingConstants.CENTER) {
            add(leftSeparator, at(0, true));
            add(label, at(1, false));
            add(rightSeparator, at(2, true));
        } else if (a == SwingConstants.RIGHT || a == SwingConstants.TRAILING) {
            add(rightSeparator, at(0, true));
            add(label, at(1, false));
        } else {
            add(label, at(0, false));
            add(rightSeparator, at(1, true));
        }
        revalidate();
        repaint();
    }

    /// Hides the label of an empty title, so the line takes all the width.
    protected void updateTitle() {
        showTitle();
    }

    private void showTitle() {
        if (label == null) {
            return;
        }
        String t = label.getText();
        label.setVisible((t != null && t.length() > 0) || label.getIcon() != null);
    }

    public void setTitle(String title) {
        String old = getTitle();
        label.setText(title);
        updateTitle();
        firePropertyChange("title", old, getTitle());
    }

    public String getTitle() {
        return label.getText();
    }

    /// Sets where the title is: `LEFT`, `CENTER`, `RIGHT`, `LEADING` or
    /// `TRAILING` of `SwingConstants`.
    public void setHorizontalAlignment(int alignment) {
        int old = getHorizontalAlignment();
        label.setHorizontalAlignment(alignment);
        if (old != getHorizontalAlignment()) {
            layoutSeparator();
        }
        firePropertyChange("horizontalAlignment", old, getHorizontalAlignment());
    }

    public int getHorizontalAlignment() {
        return label.getHorizontalAlignment();
    }

    /// Sets on which side of the icon the title's text is.
    public void setHorizontalTextPosition(int position) {
        int old = getHorizontalTextPosition();
        label.setHorizontalTextPosition(position);
        firePropertyChange("horizontalTextPosition", old, getHorizontalTextPosition());
    }

    public int getHorizontalTextPosition() {
        return label.getHorizontalTextPosition();
    }

    public Icon getIcon() {
        return label.getIcon();
    }

    public void setIcon(Icon icon) {
        Icon old = getIcon();
        label.setIcon(icon);
        updateTitle();
        firePropertyChange("icon", old, getIcon());
    }

    /// Sets the color of the title too.
    @Override
    public void setForeground(Color foreground) {
        if (label != null) {
            label.setForeground(foreground);
        }
        super.setForeground(foreground);
    }

    /// Sets the font of the title too.
    @Override
    public void setFont(Font font) {
        if (label != null) {
            label.setFont(font);
        }
        super.setFont(font);
    }
}
