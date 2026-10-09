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

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.javax.swing.AbstractButton;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.UIManager;
import com.codename1.desktopcompat.javax.swing.border.Border;
import com.codename1.desktopcompat.javax.swing.border.LineBorder;
import com.codename1.desktopcompat.javax.swing.text.JTextComponent;

/// What a FlatLaf client property does to a component of this layer.
///
/// On the desktop FlatLaf's UI delegates read these properties when they
/// paint. Here there are no delegates, so the few that have a counterpart
/// are applied when the property is put; `JComponent.putClientProperty`
/// calls [#changed] for that. Every property is kept and answered by
/// `getClientProperty` whether or not it does anything.
///
/// #### Implemented
///
///  - `JTextField.placeholderText`: the hint of the text field or area.
///  - `JButton.buttonType` of `borderless` or `toolBarButton`: a button
///    with no border and no filled background. Other types are recorded.
///  - `JComponent.outline` of `error`, `warning` or a color: a line in
///    that color around the component; null takes it away.
///  - `FlatLaf.styleClass`: the font of the first class that is one of
///    `h00`, `h0`, `h1` to `h4` (and their `.regular` forms), `large`,
///    `medium`, `small`, `mini`, `light`, `semibold` and `monospaced`.
///  - `FlatLaf.style` given as a string: its `background`, `foreground`
///    and `font` entries. A font is `bold`, `italic`, a size, a size
///    change such as `+2`, or a combination.
///  - `JComponent.minimumWidth` and `JComponent.minimumHeight`.
///
/// #### Recorded only
///
/// Everything else, among them `JComponent.roundRect`, the leading and
/// trailing icons and components and the clear button of a text field,
/// `JTextField.padding` and `selectAllOnFocusPolicy`, every
/// `JTabbedPane.*`, `JScrollBar.*`, `JScrollPane.*`, `JTree.*`,
/// `JProgressBar.*`, `JToggleButton.*` and `JSplitPane.*` property, the
/// popup properties, and the title bar and native window properties.
public final class ClientProps {

    /// Where the border a component had before its outline is kept.
    private static final String BEFORE_OUTLINE = "cn1.borderBeforeOutline";

    private ClientProps() {
    }

    /// Applies a client property that was just put.
    ///
    /// #### Parameters
    ///
    /// - `c`: the component it was put on
    ///
    /// - `key`: its key
    ///
    /// - `value`: its new value, null when it was removed
    public static void changed(JComponent c, Object key, Object value) {
        if (c == null || !(key instanceof String)) {
            return;
        }
        String k = (String) key;
        if ("JTextField.placeholderText".equals(k)) {
            placeholder(c, value);
        } else if ("JButton.buttonType".equals(k)) {
            if (c instanceof AbstractButton) {
                boolean bare = "borderless".equals(value) || "toolBarButton".equals(value);
                AbstractButton b = (AbstractButton) c;
                b.setBorderPainted(!bare);
                b.setContentAreaFilled(!bare);
            }
        } else if ("JComponent.outline".equals(k)) {
            outline(c, value);
        } else if ("FlatLaf.styleClass".equals(k)) {
            if (value instanceof String) {
                styleClass(c, (String) value);
            }
        } else if ("FlatLaf.style".equals(k)) {
            if (value instanceof String) {
                style(c, (String) value);
            }
        } else if ("JComponent.minimumWidth".equals(k) || "JComponent.minimumHeight".equals(k)) {
            if (value instanceof Integer) {
                Dimension min = c.getMinimumSize();
                int n = ((Integer) value).intValue();
                boolean width = "JComponent.minimumWidth".equals(k);
                c.setMinimumSize(new Dimension(width ? n : min == null ? 0 : min.width,
                        width ? min == null ? 0 : min.height : n));
                c.revalidate();
            }
        }
    }

    private static void placeholder(JComponent c, Object value) {
        if (!(c instanceof JTextComponent)) {
            return;
        }
        com.codename1.ui.Component peer = c.cn1Peer();
        if (peer instanceof com.codename1.ui.TextArea) {
            ((com.codename1.ui.TextArea) peer).setHint(value == null ? "" : String.valueOf(value));
            c.repaint();
        }
    }

    private static void outline(JComponent c, Object value) {
        Color color = null;
        if ("error".equals(value)) {
            color = UIManager.getColor("Component.error.focusedBorderColor");
        } else if ("warning".equals(value)) {
            color = UIManager.getColor("Component.warning.focusedBorderColor");
        } else if (value instanceof Color) {
            color = (Color) value;
        } else if (value instanceof Color[] && ((Color[]) value).length > 0) {
            color = ((Color[]) value)[0];
        }
        Object before = c.getClientProperty(BEFORE_OUTLINE);
        if (color == null) {
            if (before != null) {
                c.putClientProperty(BEFORE_OUTLINE, null);
                c.setBorder(before instanceof Border ? (Border) before : null);
            }
            return;
        }
        if (before == null) {
            Border now = c.getBorder();
            c.putClientProperty(BEFORE_OUTLINE, now != null ? (Object) now : (Object) Boolean.FALSE);
        }
        c.setBorder(new LineBorder(color, 2));
    }

    private static void styleClass(JComponent c, String classes) {
        int start = 0;
        for (int i = 0; i <= classes.length(); i++) {
            if (i == classes.length() || classes.charAt(i) == ' ') {
                if (i > start) {
                    Font f = LafTheme.styleFont(classes.substring(start, i));
                    if (f != null) {
                        c.setFont(f);
                        return;
                    }
                }
                start = i + 1;
            }
        }
    }

    private static int hex(String s) {
        int v = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            int d = ch >= '0' && ch <= '9' ? ch - '0'
                    : ch >= 'a' && ch <= 'f' ? ch - 'a' + 10
                    : ch >= 'A' && ch <= 'F' ? ch - 'A' + 10 : -1;
            if (d < 0) {
                return -1;
            }
            v = v << 4 | d;
        }
        return v;
    }

    /// A color written `#rgb` or `#rrggbb`, or null.
    private static Color color(String s) {
        if (s.length() == 4 && s.charAt(0) == '#') {
            int v = hex(s.substring(1));
            return v < 0 ? null : new Color((v >> 8 & 0xf) * 17, (v >> 4 & 0xf) * 17, (v & 0xf) * 17);
        }
        if (s.length() == 7 && s.charAt(0) == '#') {
            int v = hex(s.substring(1));
            return v < 0 ? null : new Color(v);
        }
        return null;
    }

    private static int number(String s) {
        if (s.length() == 0 || s.length() > 3) {
            return -1;
        }
        int v = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') {
                return -1;
            }
            v = v * 10 + (ch - '0');
        }
        return v;
    }

    private static void font(JComponent c, String value) {
        Font f = c.getFont();
        int style = f.getStyle();
        float size = f.getSize2D();
        int start = 0;
        for (int i = 0; i <= value.length(); i++) {
            if (i == value.length() || value.charAt(i) == ' ') {
                String word = value.substring(start, i);
                start = i + 1;
                if ("bold".equals(word)) {
                    style |= Font.BOLD;
                } else if ("italic".equals(word)) {
                    style |= Font.ITALIC;
                } else if ("normal".equals(word)) {
                    style = Font.PLAIN;
                } else if (word.length() > 1 && (word.charAt(0) == '+' || word.charAt(0) == '-')) {
                    int n = number(word.substring(1));
                    if (n > 0) {
                        size = Math.max(1f, word.charAt(0) == '+' ? size + n : size - n);
                    }
                } else {
                    int n = number(word);
                    if (n > 0) {
                        size = n;
                    }
                }
            }
        }
        c.setFont(f.deriveFont(style, size));
    }

    /// Applies the entries of a style string, `key: value; key: value`.
    private static void style(JComponent c, String style) {
        int start = 0;
        for (int i = 0; i <= style.length(); i++) {
            if (i == style.length() || style.charAt(i) == ';') {
                String entry = style.substring(start, i);
                start = i + 1;
                int colon = entry.indexOf(':');
                if (colon < 0) {
                    continue;
                }
                String key = entry.substring(0, colon).trim();
                String value = entry.substring(colon + 1).trim();
                if ("background".equals(key)) {
                    Color bg = color(value);
                    if (bg != null) {
                        c.setBackground(bg);
                    }
                } else if ("foreground".equals(key)) {
                    Color fg = color(value);
                    if (fg != null) {
                        c.setForeground(fg);
                    }
                } else if ("font".equals(key)) {
                    font(c, value);
                }
            }
        }
    }
}
