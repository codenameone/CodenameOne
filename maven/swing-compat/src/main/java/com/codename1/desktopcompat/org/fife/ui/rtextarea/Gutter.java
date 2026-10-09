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
package com.codename1.desktopcompat.org.fife.ui.rtextarea;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.UIManager;
import com.codename1.desktopcompat.javax.swing.event.DocumentEvent;
import com.codename1.desktopcompat.javax.swing.event.DocumentListener;
import com.codename1.desktopcompat.javax.swing.text.Document;

/// The strip left of the text of a [RTextScrollPane], which paints the
/// line numbers.
///
/// Each number is painted in the row of its line, with the line height of
/// the text area, and the strip scrolls with the text, so a number stays
/// beside its line. The strip is as wide as the highest line number needs.
///
/// Line numbers are what it paints. The fold indicator, bookmarks and
/// tracking icons are not provided: their switches and colors are
/// recorded and take no room.
public class Gutter extends JPanel {

    public static final Color DEFAULT_ACTIVE_LINE_RANGE_COLOR = new Color(51, 153, 255);

    /// Room left and right of the numbers, in logical pixels.
    private static final int PAD = 6;

    private RTextArea textArea;
    private Document listened;
    private boolean lineNumbersEnabled = true;
    private boolean foldIndicatorEnabled;
    private boolean iconRowHeaderEnabled;
    private boolean bookmarkingEnabled;
    private boolean iconRowHeaderInheritsGutterBackground;
    private int lineNumberingStartIndex = 1;
    private Color lineNumberColor;
    private Color currentLineNumberColor;
    private Color borderColor;
    private Color activeLineRangeColor = DEFAULT_ACTIVE_LINE_RANGE_COLOR;
    private Color foldBackground;
    private Color armedFoldBackground;
    private Color foldIndicatorForeground;
    private Color foldIndicatorArmedForeground;
    private Font lineNumberFont;
    private int lines = -1;

    private final DocumentListener changes = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            textChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            textChanged();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            textChanged();
        }
    };

    public Gutter(RTextArea textArea) {
        setTextArea(textArea);
    }

    /// The text area whose lines are numbered.
    public void setTextArea(RTextArea textArea) {
        if (listened != null) {
            listened.removeDocumentListener(changes);
            listened = null;
        }
        this.textArea = textArea;
        if (textArea != null) {
            listened = textArea.getDocument();
            if (listened != null) {
                listened.addDocumentListener(changes);
            }
        }
        lines = -1;
        revalidate();
        repaint();
    }

    private void textChanged() {
        int now = textArea == null ? 0 : textArea.getLineCount();
        if (now != lines) {
            lines = now;
            revalidate();
        }
        repaint();
    }

    private Font font() {
        if (lineNumberFont != null) {
            return lineNumberFont;
        }
        return textArea != null ? textArea.getFont() : RTextAreaBase.getDefaultFont();
    }

    private static int digits(int n) {
        int d = 1;
        while (n >= 10) {
            n /= 10;
            d++;
        }
        return d;
    }

    /// As wide as the highest line number, and as high as the text.
    @Override
    public Dimension getPreferredSize() {
        if (textArea == null || !lineNumbersEnabled) {
            return new Dimension(0, 0);
        }
        FontMetrics fm = getFontMetrics(font());
        int highest = Math.max(1, textArea.getLineCount()) + lineNumberingStartIndex - 1;
        int w = Math.max(2, digits(Math.max(1, highest))) * Math.max(1, fm.charWidth('0')) + 2 * PAD;
        return new Dimension(w, Math.max(textArea.getPreferredSize().height, textArea.getHeight()));
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        Color bg = isBackgroundSet() ? getBackground()
                : textArea != null ? textArea.getBackground() : UIManager.getColor("Panel.background");
        if (bg != null) {
            g.setColor(bg);
            g.fillRect(0, 0, w, h);
        }
        Color line = borderColor != null ? borderColor : UIManager.getColor("Component.borderColor");
        if (line != null) {
            g.setColor(line);
            g.fillRect(w - 1, 0, 1, h);
        }
        if (textArea == null || !lineNumbersEnabled) {
            return;
        }
        Font f = font();
        FontMetrics fm = getFontMetrics(f);
        g.setFont(f);
        int lineHeight = textArea.getLineHeight();
        int count = Math.max(1, textArea.getLineCount());
        int first = 0;
        int last = count - 1;
        Rectangle clip = g.getClipBounds();
        if (clip != null) {
            first = Math.max(0, clip.y / lineHeight);
            last = Math.min(last, (clip.y + clip.height) / lineHeight);
        }
        Color normal = lineNumberColor != null ? lineNumberColor : UIManager.getColor("Label.disabledForeground");
        Color current = currentLineNumberColor != null ? currentLineNumberColor : normal;
        int caretLine = textArea.getCaretLineNumber();
        int top = textArea.getInsets() == null ? 0 : textArea.getInsets().top;
        for (int i = first; i <= last; i++) {
            String number = String.valueOf(i + lineNumberingStartIndex);
            g.setColor(i == caretLine ? current : normal);
            g.drawString(number, w - PAD - fm.stringWidth(number), top + i * lineHeight + fm.getAscent());
        }
    }

    public boolean getLineNumbersEnabled() {
        return lineNumbersEnabled;
    }

    public void setLineNumbersEnabled(boolean enabled) {
        if (lineNumbersEnabled != enabled) {
            lineNumbersEnabled = enabled;
            revalidate();
            repaint();
        }
    }

    public Color getLineNumberColor() {
        return lineNumberColor != null ? lineNumberColor : UIManager.getColor("Label.disabledForeground");
    }

    public void setLineNumberColor(Color color) {
        lineNumberColor = color;
        repaint();
    }

    public Color getCurrentLineNumberColor() {
        return currentLineNumberColor;
    }

    public void setCurrentLineNumberColor(Color color) {
        currentLineNumberColor = color;
        repaint();
    }

    public Font getLineNumberFont() {
        return font();
    }

    public void setLineNumberFont(Font font) {
        lineNumberFont = font;
        revalidate();
        repaint();
    }

    public int getLineNumberingStartIndex() {
        return lineNumberingStartIndex;
    }

    public void setLineNumberingStartIndex(int index) {
        lineNumberingStartIndex = index;
        revalidate();
        repaint();
    }

    public Color getBorderColor() {
        return borderColor != null ? borderColor : UIManager.getColor("Component.borderColor");
    }

    public void setBorderColor(Color color) {
        borderColor = color;
        repaint();
    }

    public boolean isFoldIndicatorEnabled() {
        return foldIndicatorEnabled;
    }

    /// Recorded only: there is no code folding.
    public void setFoldIndicatorEnabled(boolean enabled) {
        foldIndicatorEnabled = enabled;
    }

    public boolean isIconRowHeaderEnabled() {
        return iconRowHeaderEnabled;
    }

    /// Recorded only: there are no gutter icons.
    public void setIconRowHeaderEnabled(boolean enabled) {
        iconRowHeaderEnabled = enabled;
    }

    public boolean isBookmarkingEnabled() {
        return bookmarkingEnabled;
    }

    /// Recorded only: there are no bookmarks.
    public void setBookmarkingEnabled(boolean enabled) {
        bookmarkingEnabled = enabled;
    }

    public boolean getIconRowHeaderInheritsGutterBackground() {
        return iconRowHeaderInheritsGutterBackground;
    }

    /// Recorded only.
    public void setIconRowHeaderInheritsGutterBackground(boolean inherits) {
        iconRowHeaderInheritsGutterBackground = inherits;
    }

    public Color getActiveLineRangeColor() {
        return activeLineRangeColor;
    }

    /// Recorded only.
    public void setActiveLineRangeColor(Color color) {
        activeLineRangeColor = color;
    }

    public Color getFoldBackground() {
        return foldBackground;
    }

    /// Recorded only.
    public void setFoldBackground(Color color) {
        foldBackground = color;
    }

    public Color getArmedFoldBackground() {
        return armedFoldBackground;
    }

    /// Recorded only.
    public void setArmedFoldBackground(Color color) {
        armedFoldBackground = color;
    }

    public Color getFoldIndicatorForeground() {
        return foldIndicatorForeground;
    }

    /// Recorded only.
    public void setFoldIndicatorForeground(Color color) {
        foldIndicatorForeground = color;
    }

    public Color getFoldIndicatorArmedForeground() {
        return foldIndicatorArmedForeground;
    }

    /// Recorded only.
    public void setFoldIndicatorArmedForeground(Color color) {
        foldIndicatorArmedForeground = color;
    }

    /// Always false: there is no fold indicator to arm.
    public boolean isArmed() {
        return false;
    }
}
