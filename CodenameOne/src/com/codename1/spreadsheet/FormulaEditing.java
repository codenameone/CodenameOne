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
package com.codename1.spreadsheet;

import com.codename1.ui.CodeDiagnostic;
import com.codename1.ui.CodeEditor;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Painter;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.Display;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.table.DataGrid;
import com.codename1.ui.table.GridCellPicker;
import com.codename1.ui.table.GridColumn;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.List;

/// What editing a formula is like, in a cell or in a `FormulaBar`: the references colored in
/// the text and outlined in the grid, a list of the functions and names that start with what
/// is typed, the arguments of the function the caret is in, and a tap on a cell putting its
/// address into the formula. One is attached to an editor while it is shown.
final class FormulaEditing implements GridCellPicker, Painter, ActionListener {
    private static final int VISIBLE_ITEMS = 8;

    private final Sheet sheet;
    private final DataGrid grid;
    private final CodeEditor editor;
    private final Runnable commit;
    private final Runnable cancel;
    private final Popup popup = new Popup();
    private PopupLayer layer;

    private String text = "";
    private int caret;
    private boolean focusWatched;
    private List<FormulaLexer.Token> tokens = new ArrayList<FormulaLexer.Token>();
    private boolean dark;

    // the completion list: what is shown, what is inserted, and the description
    private final List<String> items = new ArrayList<String>();
    private final List<String> inserts = new ArrayList<String>();
    private final List<String> details = new ArrayList<String>();
    private int selected;
    private int wordStart = -1;
    private boolean dismissed;
    // set while this class puts text into the editor, whose change events are then its own
    private boolean applying;
    // a cell's editor is always being typed into; a formula bar only after a key
    private boolean active = true;
    private boolean onlyWhileTyped;

    // the argument hint
    private String hintName;
    private String[] hintArgs;
    private String hintDescription;
    private int hintIndex;

    private String error;

    // the reference the last tap on a cell put in, and the cell it started from
    private int pickedStart = -1;
    private int pickedEnd = -1;
    private int anchorRow;
    private int anchorColumn;

    FormulaEditing(Sheet sheet, DataGrid grid, CodeEditor editor, String text, Runnable commit, Runnable cancel) {
        this.text = text == null ? "" : text;
        this.caret = this.text.length();
        this.sheet = sheet;
        this.grid = grid;
        this.editor = editor;
        this.commit = commit;
        this.cancel = cancel;
        FormulaHighlighter.register();
        editor.setLanguage(FormulaHighlighter.LANGUAGE);
        editor.setSingleLine(true);
        editor.addChangeListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                textChanged();
            }
        });
        editor.addKeyListener(this);
    }

    /// Makes the editing wait for a key before it takes the grid's taps and outlines anything,
    /// as a formula bar that shows the selected cell does.
    void setOnlyWhileTyped(boolean only) {
        onlyWhileTyped = only;
        active = !only;
    }

    /// Whether the text was typed into, for a formula bar, or the editor is a cell's.
    boolean isActive() {
        return active;
    }

    /// Stops the editing taking taps and outlining, until the next key, for a formula bar.
    void deactivate() {
        if (onlyWhileTyped) {
            active = false;
            pickedStart = -1;
            pickedEnd = -1;
            error = null;
            items.clear();
            hintName = null;
            release();
            changed();
        }
    }

    private void claim() {
        grid.setCellPicker(this);
        grid.setOverlayPainter(this);
    }

    private void release() {
        if (grid.getCellPicker() == this) { //NOPMD CompareObjectsWithEquals - this picker, not an equal one
            grid.setCellPicker(null);
        }
        if (grid.getOverlayPainter() == this) { //NOPMD CompareObjectsWithEquals - this painter, not an equal one
            grid.setOverlayPainter(null);
        }
    }

    /// Takes over the grid's taps and overlay and shows the popup, while the editor is shown.
    void attach() {
        Style row = grid.getUnselectedStyle();
        int bg = row.getBgColor();
        dark = (((bg >> 16) & 0xff) * 3 + ((bg >> 8) & 0xff) * 6 + (bg & 0xff)) / 10 < 110 && row.getBgTransparency() != 0;
        editor.setTheme(dark ? "dark" : "light");
        final Font cellFont = cellFont();
        editor.onReady(new Runnable() {
            @Override
            public void run() {
                // a formula is edited in the font of the cells, as a spreadsheet does
                for (int i = 0; i < editor.getComponentCount(); i++) {
                    Component inner = editor.getComponentAt(i);
                    inner.getAllStyles().setFont(cellFont);
                    if (editor instanceof CellEditor && !focusWatched) {
                        focusWatched = true;
                        inner.addFocusListener(new LeaveListener());
                    }
                }
                editor.revalidateLater();
            }
        });
        if (active) {
            claim();
        }
        Form form = editor.getComponentForm();
        if (form != null && layer == null) {
            layer = new PopupLayer(popup, editor);
            Container pane = form.getFormLayeredPane(FormulaEditing.class, true);
            pane.add(layer);
            pane.revalidate();
        }
        refresh();
    }

    /// Focus leaving a cell's editor for something that is neither it nor the grid enters the
    /// formula, as a spreadsheet does when another control is clicked; one that does not read
    /// stays in the cell with its error shown. A tap in the grid is the grid's to handle: it
    /// either points at a cell or ends the edit itself.
    private final class LeaveListener implements FocusListener {
        @Override
        public void focusGained(Component cmp) {
        }

        @Override
        public void focusLost(Component cmp) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    Form form = editor.getComponentForm();
                    if (form == null || !grid.isEditing()) {
                        return;
                    }
                    Component now = form.getFocused();
                    if (now != null && (isInside(now, editor) || isInside(now, grid))) {
                        return;
                    }
                    read();
                    String problem = problem(text);
                    if (problem != null) {
                        error = problem;
                        changed();
                        return;
                    }
                    commit.run();
                }
            });
        }
    }

    private static boolean isInside(Component c, Component parent) {
        for (Component p = c; p != null; p = p.getParent()) {
            if (p == parent) { // NOPMD CompareObjectsWithEquals - identity: the same component
                return true;
            }
        }
        return false;
    }

    /// Gives the grid back what `#attach()` took.
    void detach() {
        release();
        if (layer != null) {
            Container pane = layer.getParent();
            layer.remove();
            layer = null;
            if (pane != null) {
                pane.revalidate();
            }
        }
        grid.repaint();
    }

    /// What the completion list shows now, empty when it is not shown.
    List<String> shownItems() {
        return listShown() ? items : new ArrayList<String>();
    }

    /// The function whose arguments are shown, or null.
    String hintName() {
        return hintName;
    }

    /// The argument shown in bold, or -1.
    int hintArgument() {
        return hintName == null || listShown() || error != null ? -1 : popup.current();
    }

    /// Why the formula was not entered, or null.
    String error() {
        return error;
    }

    /// Acts on a key the editor reported, as `CodeEditor.KEY_ENTER` and the others.
    void press(int key) {
        key(key);
    }

    /// The text in the editor now.
    String text() {
        read();
        return text;
    }

    private void read() {
        if (!editor.isEditorReady()) {
            // the editor answers once its backend is up; until then it holds what it was given
            return;
        }
        editor.getText(new SuccessCallback<String>() {
            @Override
            public void onSucess(String value) {
                text = value == null ? "" : value;
            }
        });
        editor.getCursorPosition(new SuccessCallback<Integer>() {
            @Override
            public void onSucess(Integer value) {
                caret = value == null ? 0 : value.intValue();
            }
        });
        caret = Math.max(0, Math.min(caret, text.length()));
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        key(evt.getKeyEvent());
    }

    /// What a key in the editor does to the editing: wakes a formula bar and works out what to
    /// show.
    void textChangedByUser() {
        textChanged();
    }

    private void textChanged() {
        if (applying) {
            return;
        }
        if (!active) {
            active = true;
            claim();
        }
        dismissed = false;
        if (error != null) {
            error = null;
            editor.setDiagnostics(null);
        }
        refresh();
        if (pickedEnd >= 0 && caret != pickedEnd) {
            // typed past the reference a tap put in: the next tap inserts a new one
            pickedStart = -1;
            pickedEnd = -1;
        }
    }

    private boolean listShown() {
        return !items.isEmpty() && !dismissed;
    }

    private void key(int key) {
        if (listShown()) {
            switch (key) {
                case CodeEditor.KEY_ENTER:
                case CodeEditor.KEY_TAB:
                    accept(selected);
                    return;
                case CodeEditor.KEY_ESCAPE:
                    dismissed = true;
                    changed();
                    return;
                case CodeEditor.KEY_UP:
                    selected = (selected + items.size() - 1) % items.size();
                    changed();
                    return;
                case CodeEditor.KEY_DOWN:
                    selected = (selected + 1) % items.size();
                    changed();
                    return;
                default:
                    break;
            }
        }
        if (key == CodeEditor.KEY_ESCAPE) {
            cancel.run();
        } else if (key == CodeEditor.KEY_F4) {
            cycleAbsolute();
        } else if (key == CodeEditor.KEY_ENTER || key == CodeEditor.KEY_TAB || key == CodeEditor.KEY_BACK_TAB) {
            read();
            String problem = problem(text);
            if (problem != null) {
                // as a spreadsheet does, a formula that cannot be read is not entered
                error = problem;
                changed();
                return;
            }
            commit.run();
        }
    }

    /// What is wrong with a formula, marked in the editor, or null when it reads.
    private String problem(String formula) {
        if (formula.length() < 2 || formula.charAt(0) != '=') {
            return null;
        }
        try {
            FormulaParser.parse(formula.substring(1), false);
            return null;
        } catch (FormulaSyntaxException e) {
            int at = e.position < 0 ? 0 : e.position + 1;
            List<CodeDiagnostic> marks = new ArrayList<CodeDiagnostic>();
            marks.add(new CodeDiagnostic(1, Math.min(at, formula.length() - 1) + 1, 1,
                    Math.max(at + 1, formula.length()) + 1, e.getMessage()));
            editor.setDiagnostics(marks);
            return at >= formula.length() ? "The formula is not finished" : "There is a problem with this formula at \""
                    + formula.substring(at, Math.min(formula.length(), at + 12)) + "\"";
        }
    }

    /// Reads the text again and works out what to show.
    void refresh() {
        read();
        tokens = active ? FormulaLexer.lex(text) : new ArrayList<FormulaLexer.Token>();
        items.clear();
        hintName = null;
        if (active) {
            completions();
            hint();
        }
        changed();
    }

    private void changed() {
        if (editor instanceof CellEditor && editor.getParent() != null) {
            // the cell's editor widens with its text
            editor.getParent().revalidateLater();
        }
        if (layer != null) {
            popup.setShouldCalcPreferredSize(true);
            layer.revalidate();
            layer.repaint();
        }
        grid.repaint();
    }

    private void completions() {
        items.clear();
        inserts.clear();
        details.clear();
        selected = 0;
        wordStart = FormulaLexer.wordAt(tokens, caret);
        if (wordStart < 0 || caret < text.length() && text.charAt(caret) == '(') {
            return;
        }
        String prefix = FormulaParser.asciiUpper(text.substring(wordStart, caret));
        if (prefix.length() == 0 || prefix.charAt(0) == '$' || FormulaParser.isDigit(prefix.charAt(0))) {
            return;
        }
        for (String name : FormulaHelp.names()) {
            if (name.startsWith(prefix)) {
                String[] help = FormulaHelp.of(name);
                items.add(name);
                inserts.add(name + "(");
                details.add(help[1]);
            }
        }
        Workbook book = sheet.getWorkbook();
        for (String name : book.getDefinedNames()) {
            if (FormulaParser.asciiUpper(name).startsWith(prefix)) {
                items.add(name);
                inserts.add(name);
                details.add("A name defined in this workbook.");
            }
        }
        for (int i = 0; i < book.getSheetCount(); i++) {
            String name = book.getSheet(i).getName();
            if (FormulaParser.asciiUpper(name).startsWith(prefix)) {
                StringBuilder sb = new StringBuilder();
                FormulaParser.appendSheetName(sb, name);
                items.add(name);
                inserts.add(sb.append('!').toString());
                details.add("A sheet of this workbook.");
            }
        }
        if (items.size() == 1 && FormulaParser.asciiUpper(items.get(0)).equals(prefix) && !inserts.get(0).endsWith("(")) {
            // typed out already
            items.clear();
            inserts.clear();
            details.clear();
        }
    }

    private void hint() {
        hintName = null;
        Object[] call = FormulaLexer.call(tokens, text, caret);
        if (call == null) {
            return;
        }
        String name = (String) call[0];
        String[] help = FormulaHelp.of(name.startsWith("_XLFN.") ? name.substring(6) : name);
        if (help == null) {
            return;
        }
        hintName = name;
        hintArgs = FormulaHelp.arguments(help[0]);
        hintDescription = help[1];
        hintIndex = ((Integer) call[1]).intValue();
    }

    private void accept(int index) {
        if (index < 0 || index >= inserts.size() || wordStart < 0) {
            return;
        }
        String insert = inserts.get(index);
        replace(wordStart, caret, insert);
    }

    /// F4: the reference the caret is in or just after goes from relative to absolute, to an
    /// absolute row, to an absolute column and back, as a spreadsheet does.
    private void cycleAbsolute() {
        read();
        tokens = FormulaLexer.lex(text);
        for (FormulaLexer.Token t : tokens) {
            if (t.kind == FormulaLexer.REFERENCE && t.start <= caret && caret <= t.end) {
                String ref = text.substring(t.start, t.end);
                int bang = ref.lastIndexOf('!') + 1;
                String cycled = ref.substring(0, bang) + FormulaLexer.cycleAbsolute(ref.substring(bang));
                pickedEnd = -1;
                replace(t.start, t.end, cycled);
                return;
            }
        }
    }

    /// Replaces the whole text, as typing it would, with the caret at its end.
    void setText(String value) {
        text = value == null ? "" : value;
        caret = text.length();
        apply();
        refresh();
    }

    private void apply() {
        applying = true;
        try {
            editor.setText(text);
            editor.setCursorPosition(caret);
        } finally {
            applying = false;
        }
    }

    private void replace(int from, int to, String insert) {
        String next = text.substring(0, from) + insert + text.substring(to);
        text = next;
        caret = from + insert.length();
        apply();
        dismissed = false;
        refresh();
        editor.focusEditor();
    }

    // ---------------------------------------------------------------- picking cells

    @Override
    public boolean isPicking(DataGrid g) {
        if (!active || editor.getComponentForm() == null) {
            return false;
        }
        read();
        tokens = FormulaLexer.lex(text);
        if (pickedEnd >= 0 && pickedEnd == caret && pickedEnd <= text.length()) {
            return true;
        }
        return FormulaLexer.takesReference(tokens, caret);
    }

    @Override
    public void cellPicked(DataGrid g, int row, int column, boolean extend) {
        int sheetRow = SheetCells.sheetRow(sheet, g, row);
        GridColumn[] visible = g.getVisibleColumns();
        if (sheetRow < 0 || column < 0 || column >= visible.length) {
            return;
        }
        int sheetColumn = visible[column].getModelColumn();
        boolean replacing = pickedEnd >= 0 && pickedEnd == caret;
        if (!(extend && replacing)) {
            anchorRow = sheetRow;
            anchorColumn = sheetColumn;
        }
        StringBuilder sb = new StringBuilder();
        address(sb, Math.min(anchorRow, sheetRow), Math.min(anchorColumn, sheetColumn));
        if (anchorRow != sheetRow || anchorColumn != sheetColumn) {
            sb.append(':');
            address(sb, Math.max(anchorRow, sheetRow), Math.max(anchorColumn, sheetColumn));
        }
        int from = replacing ? pickedStart : caret;
        int to = replacing ? pickedEnd : caret;
        pickedStart = from;
        pickedEnd = from + sb.length();
        replace(from, to, sb.toString());
    }

    private static void address(StringBuilder sb, int row, int column) {
        FormulaParser.appendColumn(sb, column);
        sb.append(row + 1);
    }

    // ---------------------------------------------------------------- outlines in the grid

    @Override
    public void paint(Graphics g, Rectangle rect) {
        if (tokens.isEmpty()) {
            return;
        }
        int header = grid.getHeaderHeight();
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipW = g.getClipWidth();
        int clipH = g.getClipHeight();
        g.clipRect(rect.getX(), rect.getY() + header, rect.getWidth(), rect.getHeight() - header);
        int alpha = g.getAlpha();
        int[] box = new int[4];
        int[] a = new int[4];
        int[] b = new int[4];
        for (FormulaLexer.Token t : tokens) {
            if (t.kind != FormulaLexer.REFERENCE || t.sheet != null && !t.sheet.equalsIgnoreCase(sheet.getName())
                    || !box(t, box)) {
                continue;
            }
            grid.getCellBounds(box[0], box[2], a);
            grid.getCellBounds(box[1], box[3], b);
            int x = rect.getX() + a[0];
            int y = rect.getY() + a[1];
            int w = b[0] + b[2] - a[0];
            int h = b[1] + b[3] - a[1];
            g.setColor(FormulaLexer.referenceColor(t.color, dark));
            g.setAlpha(36);
            g.fillRect(x, y, w, h);
            g.setAlpha(255);
            g.drawRect(x, y, w - 1, h - 1);
            g.drawRect(x + 1, y + 1, w - 3, h - 3);
        }
        g.setAlpha(alpha);
        g.setClip(clipX, clipY, clipW, clipH);
    }

    /// The view rows and visible columns a reference covers: top, bottom, left, right. False
    /// when none of it is shown.
    private boolean box(FormulaLexer.Token t, int[] out) {
        int rows = sheet.getRowCount();
        int first = Math.max(0, t.r1 - sheet.headerRows());
        int last = Math.min(rows - 1, t.r2 - sheet.headerRows());
        if (first > last) {
            return false;
        }
        int top = Integer.MAX_VALUE;
        int bottom = -1;
        // a sorted grid can show the rows of a range anywhere; outline the rows its corners are on
        for (int m = first; m <= last; m++) {
            int v = grid.getViewRow(m);
            if (v >= 0) {
                top = Math.min(top, v);
                bottom = Math.max(bottom, v);
            }
            if (m == first + 64 && last > m) {
                m = Math.max(m, last - 64);
            }
        }
        if (bottom < 0) {
            return false;
        }
        GridColumn[] visible = grid.getVisibleColumns();
        int left = -1;
        int right = -1;
        for (int i = 0; i < visible.length; i++) {
            int c = visible[i].getModelColumn();
            if (c >= t.c1 && c <= t.c2) {
                if (left < 0) {
                    left = i;
                }
                right = i;
            }
        }
        if (left < 0) {
            return false;
        }
        out[0] = top;
        out[1] = bottom;
        out[2] = left;
        out[3] = right;
        return true;
    }

    // ---------------------------------------------------------------- the popup

    /// Text drawn twice, a pixel apart: bold at the exact size of the text around it, on every
    /// port, where a bold font derived from a theme font can come out another size.
    private static void drawBold(Graphics g, String s, int x, int y) {
        g.drawString(s, x, y);
        g.drawString(s, x + 1, y);
    }

    /// The font the grid draws its cells in, which is the row style's and not the grid's own.
    static Font cellFont() {
        Font f = UIManager.getInstance().getComponentStyle("DataGridRow").getFont();
        return f == null ? Font.getDefaultFont() : f;
    }

    private static String fit(Font font, String s, int width) {
        if (font.stringWidth(s) <= width) {
            return s;
        }
        int n = s.length();
        while (n > 0 && font.stringWidth(s.substring(0, n) + "...") > width) {
            n--;
        }
        return s.substring(0, n) + "...";
    }

    /// The list of completions, or the arguments of the function the caret is in, or why the
    /// formula cannot be entered: under the editor, in the form's layered pane, so a cell
    /// does not clip it.
    private final class Popup extends Component {
        Popup() {
            setFocusable(false);
            getAllStyles().setBgTransparency(0);
            getAllStyles().setPadding(0, 0, 0, 0);
            getAllStyles().setMargin(0, 0, 0, 0);
        }

        private Font font() {
            return cellFont();
        }

        private int lines() {
            if (listShown()) {
                return Math.min(VISIBLE_ITEMS, items.size()) + 1;
            }
            if (error != null) {
                return 1;
            }
            return hintName != null ? 2 : 0;
        }

        private int pad() {
            return Math.max(2, font().getHeight() / 3);
        }

        @Override
        protected Dimension calcPreferredSize() {
            int lines = lines();
            if (lines == 0) {
                return new Dimension(0, 0);
            }
            Font f = font();
            int w = 0;
            if (listShown()) {
                for (int i = 0; i < items.size() && i < 64; i++) {
                    w = Math.max(w, f.stringWidth(items.get(i)) + 1);
                }
                w = Math.max(w, Math.min(f.stringWidth(details.get(selected)), f.getHeight() * 28));
            } else if (error != null) {
                w = f.stringWidth(error);
            } else {
                w = Math.max(f.stringWidth(signature()) + f.stringWidth("  "), Math.min(f.stringWidth(hintDescription),
                        f.getHeight() * 28));
            }
            Form form = getComponentForm();
            int max = form == null ? w : form.getWidth() - 4 * pad();
            return new Dimension(Math.min(w, max) + 2 * pad(), lines * (f.getHeight() + pad()) + pad());
        }

        private String signature() {
            StringBuilder sb = new StringBuilder(hintName).append('(');
            for (int i = 0; i < hintArgs.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(hintArgs[i]);
            }
            return sb.append(')').toString();
        }

        private int current() {
            if (hintArgs.length == 0) {
                return -1;
            }
            if (hintIndex < hintArgs.length) {
                // past "..." the argument repeats: the one before it is the current one
                return "...".equals(hintArgs[hintIndex]) ? hintIndex - 1 : hintIndex;
            }
            return "...".equals(hintArgs[hintArgs.length - 1]) ? hintArgs.length - 2 : -1;
        }

        private int first() {
            return Math.max(0, Math.min(selected - VISIBLE_ITEMS / 2, items.size() - VISIBLE_ITEMS));
        }

        @Override
        public void paint(Graphics g) {
            int lines = lines();
            if (lines == 0) {
                return;
            }
            Style st = grid.getUnselectedStyle();
            Font f = font();
            int fg = st.getFgColor();
            int bg = st.getBgTransparency() == 0 ? (dark ? 0x202124 : 0xffffff) : st.getBgColor();
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            int p = pad();
            int lh = f.getHeight() + p;
            int alpha = g.getAlpha();
            g.setAlpha(255);
            g.setColor(bg);
            g.fillRect(x, y, w, h);
            g.setColor(dark ? 0x5f6368 : 0xc0c4c8);
            g.drawRect(x, y, w - 1, h - 1);
            int ty = y + p;
            if (listShown()) {
                int start = first();
                for (int i = start; i < items.size() && i < start + VISIBLE_ITEMS; i++) {
                    if (i == selected) {
                        g.setColor(dark ? 0x264f78 : 0xcfe2ff);
                        g.fillRect(x + 1, ty - p / 2, w - 2, lh);
                    }
                    g.setColor(fg);
                    g.setFont(f);
                    String item = fit(f, items.get(i), w - 2 * p - 1);
                    if (i == selected) {
                        drawBold(g, item, x + p, ty);
                    } else {
                        g.drawString(item, x + p, ty);
                    }
                    ty += lh;
                }
                g.setColor(dark ? 0x9aa0a6 : 0x5f6368);
                g.setFont(f);
                g.drawString(fit(f, details.get(selected), w - 2 * p), x + p, ty);
            } else if (error != null) {
                g.setColor(dark ? 0xff7b72 : 0xc4302b);
                g.setFont(f);
                g.drawString(fit(f, error, w - 2 * p), x + p, ty);
            } else {
                int cx = x + p;
                int limit = x + w - p;
                int bolded = current();
                g.setColor(fg);
                g.setFont(f);
                cx = draw(g, f, hintName + "(", cx, ty, limit, false);
                for (int i = 0; i < hintArgs.length; i++) {
                    if (i > 0) {
                        cx = draw(g, f, ", ", cx, ty, limit, false);
                    }
                    cx = draw(g, f, hintArgs[i], cx, ty, limit, i == bolded);
                }
                draw(g, f, ")", cx, ty, limit, false);
                g.setColor(dark ? 0x9aa0a6 : 0x5f6368);
                g.setFont(f);
                g.drawString(fit(f, hintDescription, w - 2 * p), x + p, ty + lh);
            }
            g.setAlpha(alpha);
        }

        private int draw(Graphics g, Font font, String s, int x, int y, int limit, boolean bold) {
            if (x >= limit) {
                return x;
            }
            String shown = fit(font, s, limit - x - (bold ? 1 : 0));
            if (bold) {
                drawBold(g, shown, x, y);
                return x + font.stringWidth(shown) + 1;
            }
            g.drawString(shown, x, y);
            return x + font.stringWidth(shown);
        }

        @Override
        public void pointerReleased(int x, int y) {
            if (!listShown()) {
                return;
            }
            int lh = font().getHeight() + pad();
            int index = first() + (y - getAbsoluteY() - pad() / 2) / lh;
            if (index >= first() && index < items.size() && index < first() + VISIBLE_ITEMS) {
                accept(index);
            }
        }
    }

    /// The layer the popup is laid in. It covers the form, so only the popup takes the pointer.
    private static final class PopupLayer extends Container {
        private final Popup popup;

        PopupLayer(final Popup popup, final Component editor) {
            super(new Layout() {
                @Override
                public void layoutContainer(Container parent) {
                    Dimension d = popup.getPreferredSize();
                    int x = editor.getAbsoluteX() - parent.getAbsoluteX();
                    int y = editor.getAbsoluteY() + editor.getHeight() - parent.getAbsoluteY() + 1;
                    if (y + d.getHeight() > parent.getHeight() && editor.getAbsoluteY() - parent.getAbsoluteY() > d.getHeight()) {
                        y = editor.getAbsoluteY() - parent.getAbsoluteY() - d.getHeight() - 1;
                    }
                    x = Math.max(0, Math.min(x, parent.getWidth() - d.getWidth()));
                    popup.setX(x);
                    popup.setY(y);
                    popup.setWidth(d.getWidth());
                    popup.setHeight(d.getHeight());
                }

                @Override
                public Dimension getPreferredSize(Container parent) {
                    // the size of the pane: a container clips what it paints to itself
                    Container pane = parent.getParent();
                    return pane == null ? new Dimension(0, 0) : new Dimension(pane.getWidth(), pane.getHeight());
                }
            });
            this.popup = popup;
            add(popup);
        }

        @Override
        public boolean contains(int x, int y) {
            return popup.getWidth() > 0 && popup.contains(x, y);
        }
    }

    /// The editor a cell of a sheet is edited in: a `CodeEditor` of one line that brings the
    /// formula editing along while it is shown.
    static final class CellEditor extends CodeEditor {
        private FormulaEditing editing;

        FormulaEditing getEditing() {
            return editing;
        }

        void setEditing(FormulaEditing editing) {
            this.editing = editing;
        }

        String currentText() {
            return editing == null ? "" : editing.text();
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            if (editing != null) {
                editing.attach();
            }
        }

        @Override
        protected void deinitialize() {
            if (editing != null) {
                editing.detach();
            }
            super.deinitialize();
        }

        @Override
        public void startEditingAsync() {
            focusEditor();
        }

        /// As wide as the cell, or wider while the text does not fit, up to the form's edge,
        /// as a spreadsheet lets what is typed run over the cells to its right.
        @Override
        public void setWidth(int width) {
            int wanted = width;
            Form form = getComponentForm();
            if (editing != null && form != null) {
                Font font = cellFont();
                int text = font.stringWidth(editing.text) + font.getHeight() * 4;
                int room = form.getAbsoluteX() + form.getWidth() - getAbsoluteX();
                wanted = Math.max(width, Math.min(text, room));
            }
            super.setWidth(wanted);
        }
    }
}
