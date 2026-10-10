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
package com.codename1.desktopcompat.javax.swing.text;

import com.codename1.desktopcompat.java.awt.Color;

/// The well known attribute names of styled text, and the typed way to
/// read and set each.
///
/// The attributes here are the ones a `JTextPane` draws: the color of the
/// text and behind it, bold, italic, underline, strike through, the font
/// family and size, and the alignment of a paragraph. A font family is
/// honoured as far as fonts are in this layer: a monospaced family, a
/// font the application made with `Font.createFont`, or the platform's
/// font for everything else. `ALIGN_JUSTIFIED` is drawn as `ALIGN_LEFT`.
/// The rest of Swing's -- superscript, indents, spacing, tab sets, icons
/// and components in the text -- is not here, so that code asking for it
/// does not compile rather than draws something else.
public class StyleConstants {

    /// The font family, a `String`.
    public static final Object FontFamily = new StyleConstants("family");

    public static final Object Family = FontFamily;

    /// The font size in points, an `Integer`.
    public static final Object FontSize = new StyleConstants("size");

    public static final Object Size = FontSize;

    public static final Object Bold = new StyleConstants("bold");

    public static final Object Italic = new StyleConstants("italic");

    public static final Object Underline = new StyleConstants("underline");

    public static final Object StrikeThrough = new StyleConstants("strikethrough");

    /// The color of the text, a `Color`.
    public static final Object Foreground = new StyleConstants("foreground");

    /// The color behind the text, a `Color`.
    public static final Object Background = new StyleConstants("background");

    /// The alignment of a paragraph, an `Integer`: one of the `ALIGN_`
    /// constants.
    public static final Object Alignment = new StyleConstants("Alignment");

    public static final int ALIGN_LEFT = 0;

    public static final int ALIGN_CENTER = 1;

    public static final int ALIGN_RIGHT = 2;

    public static final int ALIGN_JUSTIFIED = 3;

    /// The font family of `a`; `Monospaced` when it names none.
    public static String getFontFamily(AttributeSet a) {
        Object family = a.getAttribute(FontFamily);
        return family instanceof String ? (String) family : "Monospaced";
    }

    public static void setFontFamily(MutableAttributeSet a, String fam) {
        a.addAttribute(FontFamily, fam);
    }

    /// The font size of `a`; 12 when it names none.
    public static int getFontSize(AttributeSet a) {
        Object size = a.getAttribute(FontSize);
        return size instanceof Integer ? ((Integer) size).intValue() : 12;
    }

    public static void setFontSize(MutableAttributeSet a, int s) {
        a.addAttribute(FontSize, Integer.valueOf(s));
    }

    private static boolean flag(AttributeSet a, Object key) {
        Object v = a.getAttribute(key);
        return v instanceof Boolean && ((Boolean) v).booleanValue();
    }

    public static boolean isBold(AttributeSet a) {
        return flag(a, Bold);
    }

    public static void setBold(MutableAttributeSet a, boolean b) {
        a.addAttribute(Bold, Boolean.valueOf(b));
    }

    public static boolean isItalic(AttributeSet a) {
        return flag(a, Italic);
    }

    public static void setItalic(MutableAttributeSet a, boolean b) {
        a.addAttribute(Italic, Boolean.valueOf(b));
    }

    public static boolean isUnderline(AttributeSet a) {
        return flag(a, Underline);
    }

    public static void setUnderline(MutableAttributeSet a, boolean b) {
        a.addAttribute(Underline, Boolean.valueOf(b));
    }

    public static boolean isStrikeThrough(AttributeSet a) {
        return flag(a, StrikeThrough);
    }

    public static void setStrikeThrough(MutableAttributeSet a, boolean b) {
        a.addAttribute(StrikeThrough, Boolean.valueOf(b));
    }

    /// The color of the text of `a`; black when it names none.
    public static Color getForeground(AttributeSet a) {
        Object fg = a.getAttribute(Foreground);
        return fg instanceof Color ? (Color) fg : Color.black;
    }

    public static void setForeground(MutableAttributeSet a, Color fg) {
        a.addAttribute(Foreground, fg);
    }

    /// The color behind the text of `a`; black when it names none, which
    /// is why a caller asks `isDefined` first.
    public static Color getBackground(AttributeSet a) {
        Object bg = a.getAttribute(Background);
        return bg instanceof Color ? (Color) bg : Color.black;
    }

    public static void setBackground(MutableAttributeSet a, Color bg) {
        a.addAttribute(Background, bg);
    }

    public static int getAlignment(AttributeSet a) {
        Object align = a.getAttribute(Alignment);
        return align instanceof Integer ? ((Integer) align).intValue() : ALIGN_LEFT;
    }

    public static void setAlignment(MutableAttributeSet a, int align) {
        a.addAttribute(Alignment, Integer.valueOf(align));
    }

    /// The name of an element that stands for a component.
    public static final String ComponentElementName = "component";

    /// The name of an element that stands for an icon.
    public static final String IconElementName = "icon";

    /// The attribute that holds an element's name.
    public static final Object NameAttribute = AttributeSet.NameAttribute;

    /// The attribute that holds the set attributes are inherited from.
    public static final Object ResolveAttribute = AttributeSet.ResolveAttribute;

    private final String representation;

    StyleConstants(String representation) {
        this.representation = representation;
    }

    @Override
    public String toString() {
        return representation;
    }
}
