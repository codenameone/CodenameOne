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
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.MouseAdapter;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.JTextArea;
import com.codename1.desktopcompat.javax.swing.UIManager;
import com.codename1.desktopcompat.javax.swing.text.BadLocationException;
import com.codename1.desktopcompat.javax.swing.text.Document;

/// The base of the RSyntaxTextArea text components as this layer has
/// them: a text area that paints its own text.
///
/// The real library is a complete text UI of its own, which cannot run on
/// Codename One widgets. What is here instead is a [JTextArea] that draws
/// its lines itself, in a monospaced font, one row per line of the
/// document. That is what makes the rest exact: a line has a known place,
/// so [#yForLine], `modelToView`, `viewToModel`, the line highlights of
/// [RTextArea] and the line numbers of [RTextScrollPane] all agree with
/// what is painted, and a tab is expanded to the next tab stop.
///
/// What differs from the desktop:
///
///  - Lines are never wrapped; `setLineWrap(true)` is recorded only.
///  - The current line highlight follows the caret as the program moves
///    it and as the user clicks. While the user is typing, the text is
///    edited by the platform's own editor, which shows the text plainly
///    until the edit ends.
///  - Margin lines, background images and rounded or faded selections
///    are recorded and not drawn.
public abstract class RTextAreaBase extends JTextArea {

    public static final String BACKGROUND_IMAGE_PROPERTY = "background.image";
    public static final String CURRENT_LINE_HIGHLIGHT_COLOR_PROPERTY = "RTA.currentLineHighlightColor";
    public static final String CURRENT_LINE_HIGHLIGHT_FADE_PROPERTY = "RTA.currentLineHighlightFade";
    public static final String HIGHLIGHT_CURRENT_LINE_PROPERTY = "RTA.currentLineHighlight";
    public static final String ROUNDED_SELECTION_PROPERTY = "RTA.roundedSelection";

    /// Room left and right of the text, in logical pixels.
    private static final int PAD = 3;

    private boolean tabsEmulated;
    private boolean highlightCurrentLine = true;
    private boolean fadeCurrentLineHighlight;
    private boolean roundedSelectionEdges;
    private boolean marginLineEnabled;
    private int marginLinePosition = getDefaultMarginLinePosition();
    private Color marginLineColor = getDefaultMarginLineColor();
    /// Null follows the palette.
    private Color currentLineHighlightColor;
    /// The widest line in columns, or -1 when the text changed.
    private int widestColumns = -1;
    private String measuredText;

    public RTextAreaBase() {
        this(null, 0, 0);
    }

    public RTextAreaBase(String text) {
        this(text, 0, 0);
    }

    public RTextAreaBase(int rows, int cols) {
        this(null, rows, cols);
    }

    public RTextAreaBase(String text, int rows, int cols) {
        super(text, rows, cols);
        setFont(getDefaultFont());
        super.setTabSize(getDefaultTabSize());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                setCaretPosition(viewToModel(e.getPoint()));
                repaint();
            }
        });
    }

    // ------------------------------------------------------------ defaults

    public static Color getDefaultCaretColor() {
        return UIManager.getColor("TextArea.foreground");
    }

    /// A tint of the text color over the background, which reads in a
    /// light and in a dark palette.
    public static Color getDefaultCurrentLineHighlightColor() {
        Color bg = UIManager.getColor("TextArea.background");
        Color fg = UIManager.getColor("TextArea.foreground");
        if (bg == null || fg == null) {
            return new Color(255, 255, 170);
        }
        return com.codename1.desktopcompat.rt.CellTheme.mix(bg, fg, 0.07f);
    }

    public static Font getDefaultFont() {
        return new Font(Font.MONOSPACED, Font.PLAIN, 13);
    }

    public static Color getDefaultForeground() {
        return UIManager.getColor("TextArea.foreground");
    }

    public static Color getDefaultMarginLineColor() {
        return new Color(255, 224, 224);
    }

    public static int getDefaultMarginLinePosition() {
        return 80;
    }

    public static int getDefaultTabSize() {
        return 4;
    }

    /// Whether this is a Mac: a desktop build running on one.
    public static boolean isOSX() {
        return com.codename1.desktopcompat.com.formdev.flatlaf.util.SystemInfo.isMacOS;
    }

    // ------------------------------------------------------------ properties

    public Color getCurrentLineHighlightColor() {
        return currentLineHighlightColor != null ? currentLineHighlightColor
                : getDefaultCurrentLineHighlightColor();
    }

    public void setCurrentLineHighlightColor(Color color) {
        if (color == null) {
            throw new NullPointerException();
        }
        Color old = getCurrentLineHighlightColor();
        currentLineHighlightColor = color;
        firePropertyChange(CURRENT_LINE_HIGHLIGHT_COLOR_PROPERTY, old, color);
        repaint();
    }

    public boolean getHighlightCurrentLine() {
        return highlightCurrentLine;
    }

    public void setHighlightCurrentLine(boolean highlight) {
        boolean old = highlightCurrentLine;
        highlightCurrentLine = highlight;
        firePropertyChange(HIGHLIGHT_CURRENT_LINE_PROPERTY, old, highlight);
        repaint();
    }

    public boolean getFadeCurrentLineHighlight() {
        return fadeCurrentLineHighlight;
    }

    /// Recorded only.
    public void setFadeCurrentLineHighlight(boolean fade) {
        fadeCurrentLineHighlight = fade;
    }

    public boolean getRoundedSelectionEdges() {
        return roundedSelectionEdges;
    }

    /// Recorded only.
    public void setRoundedSelectionEdges(boolean rounded) {
        roundedSelectionEdges = rounded;
    }

    public boolean isMarginLineEnabled() {
        return marginLineEnabled;
    }

    /// Recorded only: no margin line is drawn.
    public void setMarginLineEnabled(boolean enabled) {
        marginLineEnabled = enabled;
    }

    public int getMarginLinePosition() {
        return marginLinePosition;
    }

    public void setMarginLinePosition(int size) {
        marginLinePosition = size;
    }

    public Color getMarginLineColor() {
        return marginLineColor;
    }

    public void setMarginLineColor(Color color) {
        marginLineColor = color;
    }

    public boolean getTabsEmulated() {
        return tabsEmulated;
    }

    /// Recorded only: typing is the platform editor's.
    public void setTabsEmulated(boolean areEmulated) {
        tabsEmulated = areEmulated;
    }

    @Override
    public void setTabSize(int size) {
        super.setTabSize(size);
        widestColumns = -1;
        revalidate();
        repaint();
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        repaint();
    }

    // ------------------------------------------------------------ geometry

    private FontMetrics metrics() {
        return getFontMetrics(getFont());
    }

    /// The height of one line of text, in logical pixels.
    public int getLineHeight() {
        return Math.max(1, metrics().getHeight());
    }

    private int charWidth() {
        return Math.max(1, metrics().charWidth('m'));
    }

    /// The text of a line with every tab replaced by the spaces that
    /// reach the next tab stop.
    protected String cn1Expand(String line) {
        if (line.indexOf('\t') < 0) {
            return line;
        }
        int tab = Math.max(1, getTabSize());
        StringBuilder sb = new StringBuilder(line.length() + 16);
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\t') {
                int n = tab - sb.length() % tab;
                for (int k = 0; k < n; k++) {
                    sb.append(' ');
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /// The column on screen of character `index` of a line, tabs counted
    /// as the spaces they are drawn as.
    private int column(String line, int index) {
        int tab = Math.max(1, getTabSize());
        int col = 0;
        int n = Math.min(index, line.length());
        for (int i = 0; i < n; i++) {
            col += line.charAt(i) == '\t' ? tab - col % tab : 1;
        }
        return col;
    }

    /// The text of line `line` without its line end, or the empty string.
    protected String cn1LineText(String all, int line) {
        try {
            int start = getLineStartOffset(line);
            int end = Math.min(all.length(), getLineEndOffset(line));
            while (end > start && (all.charAt(end - 1) == '\n' || all.charAt(end - 1) == '\r')) {
                end--;
            }
            return start >= end ? "" : all.substring(start, end);
        } catch (BadLocationException e) {
            return "";
        }
    }

    /// The left edge of the text, in logical pixels.
    protected int cn1TextX() {
        Insets in = getInsets();
        return (in == null ? 0 : in.left) + PAD;
    }

    private int top() {
        Insets in = getInsets();
        return in == null ? 0 : in.top;
    }

    /// The y coordinate of the top of a line.
    ///
    /// #### Throws
    ///
    /// - `BadLocationException`: if there is no such line
    public int yForLine(int line) throws BadLocationException {
        if (line < 0 || line >= Math.max(1, getLineCount())) {
            throw new BadLocationException("No such line", line);
        }
        return top() + line * getLineHeight();
    }

    /// The y coordinate of the top of the line that holds `offs`.
    public int yForLineContaining(int offs) throws BadLocationException {
        return yForLine(getLineOfOffset(offs));
    }

    /// Where the character at `pos` is drawn: a rectangle one pixel wide
    /// and one line high at its left edge.
    public Rectangle modelToView(int pos) throws BadLocationException {
        int line = getLineOfOffset(pos);
        String text = cn1LineText(getText(), line);
        int col = column(text, pos - getLineStartOffset(line));
        String shown = cn1Expand(text);
        int x = cn1TextX() + metrics().stringWidth(shown.substring(0, Math.min(col, shown.length())));
        return new Rectangle(x, yForLine(line), 1, getLineHeight());
    }

    /// The offset of the character drawn at `pt`: the nearest one of the
    /// line there, the end of the text below the last line.
    public int viewToModel(Point pt) {
        int lines = getLineCount();
        if (lines <= 0) {
            return 0;
        }
        int line = Math.max(0, Math.min(lines - 1, (pt.y - top()) / getLineHeight()));
        try {
            int start = getLineStartOffset(line);
            String text = cn1LineText(getText(), line);
            int want = Math.max(0, (pt.x - cn1TextX() + charWidth() / 2) / charWidth());
            int tab = Math.max(1, getTabSize());
            int col = 0;
            int i = 0;
            while (i < text.length()) {
                int w = text.charAt(i) == '\t' ? tab - col % tab : 1;
                if (col + w > want) {
                    break;
                }
                col += w;
                i++;
            }
            return start + i;
        } catch (BadLocationException e) {
            return 0;
        }
    }

    public final int getCaretLineNumber() {
        try {
            return getLineOfOffset(Math.max(0, Math.min(getCaretPosition(), cn1Length())));
        } catch (BadLocationException e) {
            return 0;
        }
    }

    public final int getCaretOffsetFromLineStart() {
        return getCaretPosition() - getLineStartOffsetOfCurrentLine();
    }

    public final int getLineStartOffsetOfCurrentLine() {
        try {
            return getLineStartOffset(getCaretLineNumber());
        } catch (BadLocationException e) {
            return 0;
        }
    }

    public final int getLineEndOffsetOfCurrentLine() {
        try {
            return getLineEndOffset(getCaretLineNumber());
        } catch (BadLocationException e) {
            return 0;
        }
    }

    private int cn1Length() {
        Document d = getDocument();
        return d == null ? 0 : d.getLength();
    }

    /// As wide as the longest line and as high as all lines, and never
    /// less than the rows and columns that were asked for.
    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        String all = getText();
        if (all == null) {
            all = "";
        }
        if (widestColumns < 0 || !all.equals(measuredText)) {
            int widest = 0;
            int tab = Math.max(1, getTabSize());
            int col = 0;
            for (int i = 0; i < all.length(); i++) {
                char c = all.charAt(i);
                if (c == '\n') {
                    widest = Math.max(widest, col);
                    col = 0;
                } else {
                    col += c == '\t' ? tab - col % tab : 1;
                }
            }
            widestColumns = Math.max(widest, col);
            measuredText = all;
        }
        Insets in = getInsets();
        int extraW = in == null ? 0 : in.left + in.right;
        int extraH = in == null ? 0 : in.top + in.bottom;
        int cols = Math.max(widestColumns + 1, getColumns());
        int rows = Math.max(Math.max(1, getLineCount()), getRows());
        return new Dimension(cols * charWidth() + 2 * PAD + extraW, rows * getLineHeight() + extraH);
    }

    // ------------------------------------------------------------ paint

    /// Paints what lies behind the text of the lines `first` to `last`:
    /// nothing here, the line highlights in [RTextArea].
    protected void cn1PaintLineBackgrounds(Graphics g, int first, int last) {
    }

    /// Paints the text of one line with its left end at `x` and its
    /// baseline at `baseline`. `shown` is the line with tabs expanded.
    protected void cn1PaintLine(Graphics g, String shown, int line, int x, int baseline) {
        g.setColor(isEnabled() ? getForeground() : getDisabledColor());
        g.drawString(shown, x, baseline);
    }

    private Color getDisabledColor() {
        Color c = UIManager.getColor("TextArea.inactiveForeground");
        return c != null ? c : getForeground();
    }

    private void fillRange(Graphics g, String all, int from, int to, int first, int last) {
        FontMetrics fm = metrics();
        int h = getLineHeight();
        for (int line = first; line <= last; line++) {
            try {
                int start = getLineStartOffset(line);
                int end = getLineEndOffset(line);
                if (to <= start || from >= end && !(from == end && line == last)) {
                    continue;
                }
                String text = cn1LineText(all, line);
                String shown = cn1Expand(text);
                int a = Math.min(shown.length(), column(text, Math.max(from, start) - start));
                int b = Math.min(shown.length(), column(text, Math.min(to, end) - start));
                int x0 = cn1TextX() + fm.stringWidth(shown.substring(0, a));
                int x1 = cn1TextX() + fm.stringWidth(shown.substring(0, b));
                if (to > end - 1 && to > start + text.length()) {
                    // The line end is selected too.
                    x1 += charWidth() / 2;
                }
                if (x1 > x0) {
                    g.fillRect(x0, yForLine(line), x1 - x0, h);
                }
            } catch (BadLocationException e) {
                continue;
            }
        }
    }

    /// Fills the background of the characters from `from` to `to` within
    /// the lines `first` to `last`, in the color of `g`.
    protected void cn1FillRange(Graphics g, String all, int from, int to, int first, int last) {
        if (to > from) {
            fillRange(g, all, from, to, first, last);
        }
    }

    /// Paints what lies between the line backgrounds and the text of the
    /// lines `first` to `last`: nothing here, the marked occurrences in
    /// [RTextArea].
    protected void cn1PaintMarks(Graphics g, String all, int first, int last) {
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        Color bg = getBackground();
        if (bg != null) {
            g.setColor(bg);
            g.fillRect(0, 0, w, h);
        }
        int lines = getLineCount();
        int lineHeight = getLineHeight();
        Rectangle clip = g.getClipBounds();
        int first = 0;
        int last = lines - 1;
        if (clip != null) {
            first = Math.max(0, (clip.y - top()) / lineHeight);
            last = Math.min(last, (clip.y + clip.height - top()) / lineHeight);
        }
        if (lines <= 0 || last < first) {
            return;
        }
        String all = getText();
        if (all == null) {
            all = "";
        }
        int caretLine = getCaretLineNumber();
        if (highlightCurrentLine && caretLine >= first && caretLine <= last
                && getSelectionStart() == getSelectionEnd()) {
            g.setColor(getCurrentLineHighlightColor());
            g.fillRect(0, top() + caretLine * lineHeight, w, lineHeight);
        }
        cn1PaintLineBackgrounds(g, first, last);
        cn1PaintMarks(g, all, first, last);
        int from = getSelectionStart();
        int to = getSelectionEnd();
        if (to > from) {
            Color sel = getSelectionColor();
            g.setColor(sel != null ? sel : UIManager.getColor("textHighlight"));
            fillRange(g, all, from, to, first, last);
        }
        FontMetrics fm = metrics();
        g.setFont(getFont());
        int x = cn1TextX();
        for (int line = first; line <= last; line++) {
            String text = cn1LineText(all, line);
            if (text.length() > 0) {
                cn1PaintLine(g, cn1Expand(text), line, x, top() + line * lineHeight + fm.getAscent());
            }
        }
        if (isFocusOwner() && isEditable() && from == to) {
            try {
                Rectangle r = modelToView(Math.max(0, Math.min(getCaretPosition(), cn1Length())));
                Color caret = getCaretColor();
                g.setColor(caret != null ? caret : getForeground());
                g.fillRect(r.x, r.y, 1, r.height);
            } catch (BadLocationException e) {
                return;
            }
        }
    }
}
