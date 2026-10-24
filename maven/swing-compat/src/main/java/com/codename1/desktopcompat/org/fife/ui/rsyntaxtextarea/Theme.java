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
package com.codename1.desktopcompat.org.fife.ui.rsyntaxtextarea;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.org.fife.ui.rtextarea.Gutter;
import com.codename1.desktopcompat.org.fife.ui.rtextarea.RTextAreaBase;
import com.codename1.desktopcompat.org.fife.ui.rtextarea.RTextScrollPane;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/// The colors of a [RSyntaxTextArea] and its gutter, read from a theme
/// file in the library's XML format or taken from a text area.
///
/// What a theme carries here is what this layer paints: the background,
/// the caret, the selection, the current line highlight, the mark-all
/// color, the gutter background, border and line number colors, the font
/// size, and a foreground color per token type. Bold, italic and
/// underlined token styles, and the remaining colors, are read into their
/// fields where a field exists and are not painted.
///
/// The theme files of the library itself live in its jar, which an
/// application built with this layer does not ship. Reading one of them
/// therefore hands [#load] a null stream, which fails with a
/// `NullPointerException` exactly as on the desktop; an application that
/// catches it, as the common idiom does, keeps the default colors, which
/// follow the light or dark palette.
public class Theme {

    public Font baseFont;
    public Color bgColor;
    public Color caretColor;
    public boolean useSelectionFG;
    public Color selectionFG;
    public Color selectionBG;
    public boolean selectionRoundedEdges;
    public Color currentLineHighlight;
    public boolean fadeCurrentLineHighlight;
    public Color tabLineColor;
    public Color marginLineColor;
    public Color markAllHighlightColor;
    public Color markOccurrencesColor;
    public boolean markOccurrencesBorder;
    public Color matchedBracketFG;
    public Color matchedBracketBG;
    public boolean matchedBracketHighlightBoth;
    public boolean matchedBracketAnimate;
    public Color hyperlinkFG;
    public Color gutterBackgroundColor;
    public Color gutterBorderColor;
    public Color activeLineRangeColor;
    public boolean iconRowHeaderInheritsGutterBG;
    public Color lineNumberColor;
    public Color currentLineNumberColor;
    public String lineNumberFont;
    public int lineNumberFontSize;
    public Color foldIndicatorFG;
    public Color foldIndicatorArmedFG;
    public Color foldBG;
    public Color armedFoldBG;

    /// The text color, and a color per token type; null follows the
    /// palette.
    private Color foreground;
    private Color[] tokenColors;

    private Theme(Font baseFont) {
        this.baseFont = baseFont;
    }

    /// A theme of the colors a text area has now.
    public Theme(RSyntaxTextArea textArea) {
        baseFont = textArea.getFont();
        bgColor = textArea.getBackground();
        caretColor = textArea.getCaretColor();
        useSelectionFG = textArea.getUseSelectedTextColor();
        selectionFG = textArea.getSelectedTextColor();
        selectionBG = textArea.getSelectionColor();
        selectionRoundedEdges = textArea.getRoundedSelectionEdges();
        currentLineHighlight = textArea.getCurrentLineHighlightColor();
        fadeCurrentLineHighlight = textArea.getFadeCurrentLineHighlight();
        tabLineColor = textArea.getTabLineColor();
        marginLineColor = textArea.getMarginLineColor();
        markAllHighlightColor = textArea.getMarkAllHighlightColor();
        markOccurrencesColor = textArea.getMarkOccurrencesColor();
        markOccurrencesBorder = textArea.getPaintMarkOccurrencesBorder();
        matchedBracketBG = textArea.getMatchedBracketBGColor();
        matchedBracketFG = textArea.getMatchedBracketBorderColor();
        matchedBracketHighlightBoth = textArea.getPaintMatchedBracketPair();
        matchedBracketAnimate = textArea.getAnimateBracketMatching();
        hyperlinkFG = textArea.getHyperlinkForeground();
        foreground = textArea.isForegroundSet() ? textArea.getForeground() : null;
        Color[] colors = textArea.cn1TokenColors();
        if (colors != null) {
            tokenColors = new Color[colors.length];
            System.arraycopy(colors, 0, tokenColors, 0, colors.length);
        }
        Gutter gutter = gutter(textArea);
        if (gutter != null) {
            gutterBackgroundColor = gutter.isBackgroundSet() ? gutter.getBackground() : null;
            gutterBorderColor = gutter.getBorderColor();
            activeLineRangeColor = gutter.getActiveLineRangeColor();
            iconRowHeaderInheritsGutterBG = gutter.getIconRowHeaderInheritsGutterBackground();
            lineNumberColor = gutter.getLineNumberColor();
            currentLineNumberColor = gutter.getCurrentLineNumberColor();
            lineNumberFont = gutter.getLineNumberFont().getFamily();
            lineNumberFontSize = gutter.getLineNumberFont().getSize();
            foldIndicatorFG = gutter.getFoldIndicatorForeground();
            foldIndicatorArmedFG = gutter.getFoldIndicatorArmedForeground();
            foldBG = gutter.getFoldBackground();
            armedFoldBG = gutter.getArmedFoldBackground();
        }
    }

    /// The gutter of the scroll pane a text area is in, or null.
    private static Gutter gutter(RSyntaxTextArea textArea) {
        Component c = textArea.getParent();
        for (int depth = 0; c != null && depth < 3; depth++) {
            if (c instanceof RTextScrollPane) {
                return ((RTextScrollPane) c).getGutter();
            }
            c = c.getParent();
        }
        return null;
    }

    /// Gives a text area, and the gutter of its scroll pane, the colors
    /// of this theme. A color the theme does not carry is left as it is.
    public void apply(RSyntaxTextArea textArea) {
        if (baseFont != null) {
            textArea.setFont(baseFont);
        }
        if (bgColor != null) {
            textArea.setBackground(bgColor);
        }
        if (foreground != null) {
            textArea.setForeground(foreground);
        }
        if (caretColor != null) {
            textArea.setCaretColor(caretColor);
        }
        textArea.setUseSelectedTextColor(useSelectionFG);
        if (selectionFG != null) {
            textArea.setSelectedTextColor(selectionFG);
        }
        if (selectionBG != null) {
            textArea.setSelectionColor(selectionBG);
        }
        textArea.setRoundedSelectionEdges(selectionRoundedEdges);
        if (currentLineHighlight != null) {
            textArea.setCurrentLineHighlightColor(currentLineHighlight);
        }
        textArea.setFadeCurrentLineHighlight(fadeCurrentLineHighlight);
        if (tabLineColor != null) {
            textArea.setTabLineColor(tabLineColor);
        }
        if (marginLineColor != null) {
            textArea.setMarginLineColor(marginLineColor);
        }
        if (markAllHighlightColor != null) {
            textArea.setMarkAllHighlightColor(markAllHighlightColor);
        }
        if (markOccurrencesColor != null) {
            textArea.setMarkOccurrencesColor(markOccurrencesColor);
        }
        textArea.setPaintMarkOccurrencesBorder(markOccurrencesBorder);
        if (matchedBracketBG != null) {
            textArea.setMatchedBracketBGColor(matchedBracketBG);
        }
        if (matchedBracketFG != null) {
            textArea.setMatchedBracketBorderColor(matchedBracketFG);
        }
        textArea.setPaintMatchedBracketPair(matchedBracketHighlightBoth);
        textArea.setAnimateBracketMatching(matchedBracketAnimate);
        if (hyperlinkFG != null) {
            textArea.setHyperlinkForeground(hyperlinkFG);
        }
        textArea.cn1TokenColors(tokenColors);
        Gutter gutter = gutter(textArea);
        if (gutter != null) {
            if (gutterBackgroundColor != null) {
                gutter.setBackground(gutterBackgroundColor);
            }
            if (gutterBorderColor != null) {
                gutter.setBorderColor(gutterBorderColor);
            }
            if (activeLineRangeColor != null) {
                gutter.setActiveLineRangeColor(activeLineRangeColor);
            }
            gutter.setIconRowHeaderInheritsGutterBackground(iconRowHeaderInheritsGutterBG);
            if (lineNumberColor != null) {
                gutter.setLineNumberColor(lineNumberColor);
            }
            if (currentLineNumberColor != null) {
                gutter.setCurrentLineNumberColor(currentLineNumberColor);
            }
            if (lineNumberFontSize > 0 && baseFont != null) {
                gutter.setLineNumberFont(baseFont.deriveFont((float) lineNumberFontSize));
            }
            gutter.setFoldIndicatorForeground(foldIndicatorFG);
            gutter.setFoldIndicatorArmedForeground(foldIndicatorArmedFG);
            gutter.setFoldBackground(foldBG);
            gutter.setArmedFoldBackground(armedFoldBG);
        }
        textArea.repaint();
    }

    /// Reads a theme file.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the stream cannot be read
    ///
    /// - `NullPointerException`: if the stream is null, which is what a
    ///   theme file that is not on the class path gives
    public static Theme load(InputStream in) throws IOException {
        return load(in, null);
    }

    /// Reads a theme file. The font is always monospaced; `baseFont`
    /// gives its size unless the file names one.
    public static Theme load(InputStream in, Font baseFont) throws IOException {
        if (in == null) {
            throw new NullPointerException("No theme to read");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) {
            bytes.write(buf, 0, n);
        }
        String xml = new String(bytes.toByteArray(), "UTF-8");
        Font font = RTextAreaBase.getDefaultFont();
        if (baseFont != null) {
            font = font.deriveFont((float) baseFont.getSize());
        }
        Theme theme = new Theme(font);
        int at = 0;
        while ((at = xml.indexOf('<', at)) >= 0) {
            int end = xml.indexOf('>', at);
            if (end < 0) {
                break;
            }
            char first = at + 1 < xml.length() ? xml.charAt(at + 1) : ' ';
            if (first != '/' && first != '?' && first != '!') {
                theme.element(xml.substring(at + 1, end));
            }
            at = end + 1;
        }
        return theme;
    }

    /// The value of an attribute of an element's text, or null.
    private static String attr(String element, String name) {
        int at = 0;
        while ((at = element.indexOf(name + "=", at)) >= 0) {
            boolean whole = at == 0 || element.charAt(at - 1) <= ' ';
            int open = at + name.length() + 1;
            if (whole && open < element.length()
                    && (element.charAt(open) == '"' || element.charAt(open) == '\'')) {
                int close = element.indexOf(element.charAt(open), open + 1);
                return close < 0 ? null : element.substring(open + 1, close);
            }
            at = open;
        }
        return null;
    }

    private static int hex(String s) {
        int v = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = c >= '0' && c <= '9' ? c - '0'
                    : c >= 'a' && c <= 'f' ? c - 'a' + 10
                    : c >= 'A' && c <= 'F' ? c - 'A' + 10 : -1;
            if (d < 0) {
                return -1;
            }
            v = v << 4 | d;
        }
        return v;
    }

    /// A color as a theme file writes one: six hex digits, or eight with
    /// the opacity first, with or without a leading dollar sign or hash.
    /// Null for no value, for "default" and for anything else.
    private static Color color(String s) {
        if (s == null) {
            return null;
        }
        if (s.length() > 0 && (s.charAt(0) == '$' || s.charAt(0) == '#')) {
            s = s.substring(1);
        }
        if (s.length() != 6 && s.length() != 8) {
            return null;
        }
        int rgb = hex(s.substring(s.length() - 6));
        if (rgb < 0) {
            return null;
        }
        if (s.length() == 8) {
            int alpha = hex(s.substring(0, 2));
            if (alpha < 0) {
                return null;
            }
            return new Color(rgb >> 16 & 0xff, rgb >> 8 & 0xff, rgb & 0xff, alpha);
        }
        return new Color(rgb);
    }

    private static boolean flag(String s) {
        return "true".equals(s);
    }

    private static int number(String s) {
        if (s == null || s.length() == 0 || s.length() > 4) {
            return -1;
        }
        int v = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
        }
        return v;
    }

    /// Takes what one element of a theme file says; `element` is the
    /// text between its angle brackets.
    private void element(String element) {
        int nameEnd = 0;
        while (nameEnd < element.length() && element.charAt(nameEnd) > ' ' && element.charAt(nameEnd) != '/') {
            nameEnd++;
        }
        String name = element.substring(0, nameEnd);
        if ("baseFont".equals(name)) {
            int size = number(attr(element, "size"));
            if (size > 0 && baseFont != null) {
                baseFont = baseFont.deriveFont((float) size);
            }
        } else if ("background".equals(name)) {
            bgColor = color(attr(element, "color"));
        } else if ("caret".equals(name)) {
            caretColor = color(attr(element, "color"));
        } else if ("selection".equals(name)) {
            useSelectionFG = flag(attr(element, "useFG"));
            selectionFG = color(attr(element, "fg"));
            selectionBG = color(attr(element, "bg"));
            selectionRoundedEdges = flag(attr(element, "roundedEdges"));
        } else if ("currentLineHighlight".equals(name)) {
            currentLineHighlight = color(attr(element, "color"));
            fadeCurrentLineHighlight = flag(attr(element, "fade"));
        } else if ("tabLine".equals(name)) {
            tabLineColor = color(attr(element, "color"));
        } else if ("marginLine".equals(name)) {
            marginLineColor = color(attr(element, "fg"));
        } else if ("markAllHighlight".equals(name)) {
            markAllHighlightColor = color(attr(element, "color"));
        } else if ("markOccurrencesHighlight".equals(name)) {
            markOccurrencesColor = color(attr(element, "color"));
            markOccurrencesBorder = flag(attr(element, "border"));
        } else if ("matchedBracket".equals(name)) {
            matchedBracketFG = color(attr(element, "fg"));
            matchedBracketBG = color(attr(element, "bg"));
            matchedBracketHighlightBoth = flag(attr(element, "highlightBoth"));
            matchedBracketAnimate = flag(attr(element, "animate"));
        } else if ("hyperlinks".equals(name)) {
            hyperlinkFG = color(attr(element, "fg"));
        } else if ("gutterBackground".equals(name)) {
            gutterBackgroundColor = color(attr(element, "color"));
        } else if ("gutterBorder".equals(name)) {
            gutterBorderColor = color(attr(element, "color"));
        } else if ("lineNumbers".equals(name)) {
            lineNumberColor = color(attr(element, "fg"));
            currentLineNumberColor = color(attr(element, "currentFG"));
            lineNumberFont = attr(element, "fontFamily");
            lineNumberFontSize = Math.max(0, number(attr(element, "fontSize")));
        } else if ("foldIndicator".equals(name)) {
            foldIndicatorFG = color(attr(element, "fg"));
            foldIndicatorArmedFG = color(attr(element, "armedFg"));
            foldBG = color(attr(element, "iconBg"));
            armedFoldBG = color(attr(element, "iconArmedBg"));
        } else if ("iconRowHeader".equals(name)) {
            activeLineRangeColor = color(attr(element, "activeLineRange"));
            iconRowHeaderInheritsGutterBG = flag(attr(element, "inheritsGutterBG"));
        } else if ("style".equals(name)) {
            int type = TokenNames.of(attr(element, "token"));
            Color fg = color(attr(element, "fg"));
            if (type >= 0 && fg != null) {
                if (tokenColors == null) {
                    tokenColors = new Color[TokenTypes.DEFAULT_NUM_TOKEN_TYPES];
                }
                tokenColors[type] = fg;
                if (type == TokenTypes.IDENTIFIER) {
                    foreground = fg;
                }
            }
        }
    }
}
