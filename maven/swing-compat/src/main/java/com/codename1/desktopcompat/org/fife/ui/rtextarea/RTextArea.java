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
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.javax.swing.UIManager;
import com.codename1.desktopcompat.javax.swing.text.BadLocationException;
import com.codename1.desktopcompat.org.fife.ui.rsyntaxtextarea.DocumentRange;
import com.codename1.desktopcompat.rt.CellTheme;
import java.util.ArrayList;
import java.util.List;

/// The text area of RSyntaxTextArea's `rtextarea` package: [RTextAreaBase]
/// with line highlights and marked occurrences, both really painted.
///
/// A line highlight is a band the width of the component behind one line,
/// which is how a diff or a blame view colors its lines. A marked
/// occurrence is the background of a range of characters, which is what
/// [SearchEngine#markAll] produces.
///
/// A line highlight stays with the line number it was given for; it does
/// not follow its text when lines are inserted above it. Undo, redo,
/// macros, the popup menu and the overwrite caret are not provided: the
/// methods that exist for them are recorded or do nothing, as each says.
public class RTextArea extends RTextAreaBase {

    public static final int INSERT_MODE = 0;
    public static final int OVERWRITE_MODE = 1;
    public static final String MARK_ALL_COLOR_PROPERTY = "RTA.markAllColor";
    public static final String MARK_ALL_ON_OCCURRENCE_SEARCHES_PROPERTY = "RTA.markAllOnOccurrenceSearches";
    public static final String MARK_ALL_OCCURRENCES_CHANGED_PROPERTY = "RTA.markAllOccurrencesChanged";
    public static final int COPY_ACTION = 0;
    public static final int CUT_ACTION = 1;
    public static final int DELETE_ACTION = 2;
    public static final int PASTE_ACTION = 3;
    public static final int REDO_ACTION = 4;
    public static final int SELECT_ALL_ACTION = 5;
    public static final int UNDO_ACTION = 6;

    /// One highlighted line; the object [#addLineHighlight] hands back.
    private static final class LineMark {
        final int line;
        final Color color;

        LineMark(int line, Color color) {
            this.line = line;
            this.color = color;
        }
    }

    private static String selectedOccurrenceText;

    private final ArrayList<LineMark> lineMarks = new ArrayList<LineMark>();
    private List<DocumentRange> marked = new ArrayList<DocumentRange>();
    private Color markAllHighlightColor = getDefaultMarkAllHighlightColor();
    private boolean markAllOnOccurrenceSearches = true;
    private int textMode;

    public RTextArea() {
        super();
    }

    public RTextArea(String text) {
        super(text);
    }

    public RTextArea(int rows, int cols) {
        super(rows, cols);
    }

    public RTextArea(String text, int rows, int cols) {
        super(text, rows, cols);
    }

    public RTextArea(int textMode) {
        super();
        setTextMode(textMode);
    }

    // ------------------------------------------------------------ line highlights

    /// Paints a band of `color` behind a line.
    ///
    /// #### Parameters
    ///
    /// - `line`: the line, counted from zero
    ///
    /// #### Returns
    ///
    /// the object to hand to [#removeLineHighlight]
    ///
    /// #### Throws
    ///
    /// - `BadLocationException`: if there is no such line
    public Object addLineHighlight(int line, Color color) throws BadLocationException {
        if (line < 0 || line >= Math.max(1, getLineCount())) {
            throw new BadLocationException("No such line", line);
        }
        LineMark mark = new LineMark(line, color);
        lineMarks.add(mark);
        repaint();
        return mark;
    }

    public void removeLineHighlight(Object tag) {
        if (lineMarks.remove(tag)) {
            repaint();
        }
    }

    public void removeAllLineHighlights() {
        if (!lineMarks.isEmpty()) {
            lineMarks.clear();
            repaint();
        }
    }

    @Override
    protected void cn1PaintLineBackgrounds(Graphics g, int first, int last) {
        int h = getLineHeight();
        int w = getWidth();
        for (int i = 0; i < lineMarks.size(); i++) {
            LineMark mark = lineMarks.get(i);
            if (mark.line < first || mark.line > last || mark.color == null) {
                continue;
            }
            try {
                g.setColor(mark.color);
                g.fillRect(0, yForLine(mark.line), w, h);
            } catch (BadLocationException e) {
                continue;
            }
        }
    }

    // ------------------------------------------------------------ marked occurrences

    /// A tint of the accent color, which reads in a light and in a dark
    /// palette.
    public static Color getDefaultMarkAllHighlightColor() {
        Color bg = UIManager.getColor("TextArea.background");
        Color accent = UIManager.getColor("Component.accentColor");
        if (bg == null || accent == null) {
            return new Color(0xff, 0xc8, 0x00);
        }
        return CellTheme.mix(bg, accent, 0.45f);
    }

    public Color getMarkAllHighlightColor() {
        return markAllHighlightColor;
    }

    public void setMarkAllHighlightColor(Color color) {
        Color old = markAllHighlightColor;
        markAllHighlightColor = color;
        firePropertyChange(MARK_ALL_COLOR_PROPERTY, old, color);
        repaint();
    }

    public boolean getMarkAllOnOccurrenceSearches() {
        return markAllOnOccurrenceSearches;
    }

    public void setMarkAllOnOccurrenceSearches(boolean markAll) {
        boolean old = markAllOnOccurrenceSearches;
        markAllOnOccurrenceSearches = markAll;
        firePropertyChange(MARK_ALL_ON_OCCURRENCE_SEARCHES_PROPERTY, old, markAll);
    }

    /// Marks these ranges, in place of the ranges marked before.
    public void markAll(List<DocumentRange> ranges) {
        marked = new ArrayList<DocumentRange>();
        if (ranges != null) {
            marked.addAll(ranges);
        }
        firePropertyChange(MARK_ALL_OCCURRENCES_CHANGED_PROPERTY, null, null);
        repaint();
    }

    public void clearMarkAllHighlights() {
        if (!marked.isEmpty()) {
            markAll(null);
        }
    }

    /// The ranges that are marked, for the classes of this layer.
    public List<DocumentRange> cn1Marked() {
        return marked;
    }

    @Override
    protected void cn1PaintMarks(Graphics g, String all, int first, int last) {
        if (marked.isEmpty() || markAllHighlightColor == null) {
            return;
        }
        g.setColor(markAllHighlightColor);
        for (int i = 0; i < marked.size(); i++) {
            DocumentRange r = marked.get(i);
            cn1FillRange(g, all, r.getStartOffset(), Math.min(all.length(), r.getEndOffset()), first, last);
        }
    }

    public static String getSelectedOccurrenceText() {
        return selectedOccurrenceText;
    }

    public static void setSelectedOccurrenceText(String text) {
        selectedOccurrenceText = text;
    }

    // ------------------------------------------------------------ recorded or absent

    public final int getTextMode() {
        return textMode;
    }

    /// Recorded only: text is always inserted.
    public void setTextMode(int mode) {
        textMode = mode == OVERWRITE_MODE ? OVERWRITE_MODE : INSERT_MODE;
    }

    /// Always false: there is no undo history.
    public boolean canUndo() {
        return false;
    }

    /// Always false: there is no undo history.
    public boolean canRedo() {
        return false;
    }

    /// Does nothing: there is no undo history.
    public void undoLastAction() {
    }

    /// Does nothing: there is no undo history.
    public void redoLastAction() {
    }

    /// Does nothing: there is no undo history.
    public void discardAllEdits() {
    }

    /// Does nothing: there is no undo history to group edits in.
    public void beginAtomicEdit() {
    }

    /// Does nothing: there is no undo history to group edits in.
    public void endAtomicEdit() {
    }

    public int getMaxAscent() {
        return getFontMetrics(getFont()).getAscent();
    }

    @Override
    public void setRoundedSelectionEdges(boolean rounded) {
        super.setRoundedSelectionEdges(rounded);
    }
}
