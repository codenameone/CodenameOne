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

package com.codename1.ui.editor;

import com.codename1.ui.CodeCompletion;
import com.codename1.ui.CodeDiagnostic;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.CodeEditor;
import com.codename1.ui.TextInputConfig;

import java.util.ArrayList;
import java.util.List;

/// The pure code editor surface. It adds syntax highlighting (via an incremental `Tokenizer`), a line
/// number gutter, bracket / quote auto close, tab expansion, auto indent, diagnostic squiggles and a
/// code completion popup on top of the shared `EditorView` editing engine.
public class CodeView extends EditorView {
    private String languageId = "text";
    private LanguageDef def = LanguageDef.forName("text");
    private SyntaxHighlighter tokenizer = new Tokenizer(def);
    private boolean plainHighlighter = true;
    private ThemePalette palette = ThemePalette.LIGHT;
    private boolean showLineNumbers = true;
    private int tabSize = 4;

    private int[] endStates;
    private int validUpTo;

    private List<CodeDiagnostic> diagnostics;

    private boolean completionEnabled;
    private List<CodeCompletion> completionAll;
    private List<CodeCompletion> completionFiltered;
    private int completionIndex;
    private boolean completionVisible;
    private int completionAnchor;
    private int completionReqSeq;
    private int completionPending = -1;

    /// Creates a code editor view.
    public CodeView(EditorHost host) {
        super(host, true);
        applyTheme();
    }

    // ---- configuration ----

    /// Sets the highlighting language.
    public void setLanguage(String language) {
        this.languageId = language == null ? "text" : language;
        this.def = LanguageDef.forName(this.languageId);
        SyntaxHighlighter registered = CodeEditor.getRegisteredSyntaxHighlighter(this.languageId);
        this.tokenizer = registered == null ? new Tokenizer(def) : registered;
        this.plainHighlighter = registered == null && def.isPlain();
        validUpTo = 0;
        repaint();
    }

    /// Returns the highlighting language id.
    public String getLanguage() {
        return languageId;
    }

    /// Sets the color theme (`"light"` or `"dark"`).
    public void setTheme(String theme) {
        palette = ThemePalette.forName(theme);
        applyTheme();
        repaint();
    }

    private void applyTheme() {
        setBackgroundColor(palette.getBackground());
        setTextColor(palette.getForeground());
        setSelectionColor(palette.getSelection());
    }

    /// Shows or hides the line number gutter.
    public void setShowLineNumbers(boolean show) {
        this.showLineNumbers = show;
        repaint();
    }

    /// Sets the number of spaces used for a tab / indentation step.
    public void setTabSize(int tabSize) {
        this.tabSize = Math.max(1, tabSize);
    }

    /// Sets the diagnostics to render.
    public void setDiagnostics(List<CodeDiagnostic> diagnostics) {
        this.diagnostics = diagnostics;
        repaint();
    }

    /// Enables or disables the completion popup.
    public void setCompletionEnabled(boolean enabled) {
        this.completionEnabled = enabled;
        if (!enabled) {
            hideCompletion();
        }
    }

    // ---- gutter ----

    @Override
    protected int getContentLeftInset() {
        return getStyle().getPaddingLeftNoRTL() + gutterWidth();
    }

    private int gutterWidth() {
        if (!showLineNumbers) {
            return 0;
        }
        Font f = getEditorFont();
        int digits = String.valueOf(Math.max(1, getDocument().getLineCount())).length();
        return f.charWidth('0') * digits + f.charWidth('0') * 2;
    }

    @Override
    protected void paintBeforeContent(Graphics g, int originX, int originY, int firstLine, int lastLine, int lineHeight, Font f) {
        if (!showLineNumbers) {
            return;
        }
        g.setColor(palette.getGutterBackground());
        g.fillRect(getX(), getY(), originX - getX(), getHeight());
        int pad = f.charWidth('0');
        for (int ln = firstLine; ln <= lastLine; ln++) {
            String num = String.valueOf(ln + 1);
            int nx = originX - pad - f.stringWidth(num);
            int ny = originY + ln * lineHeight;
            int marker = gutterMarkerColor(ln);
            if (marker != -1) {
                g.setColor(marker);
                int dia = Math.max(6, lineHeight / 2);
                g.fillArc(getX() + pad / 2, ny + (lineHeight - dia) / 2, dia, dia, 0, 360);
            }
            g.setColor(palette.getGutterForeground());
            g.drawString(num, nx, ny);
        }
    }

    private int gutterMarkerColor(int line) {
        if (diagnostics == null) {
            return -1;
        }
        int color = -1;
        int rank = -1;
        for (CodeDiagnostic d : diagnostics) {
            if (d == null) {
                continue;
            }
            if (line >= d.getLine() - 1 && line <= d.getEndLine() - 1) {
                int r = severityRank(d.getSeverity());
                if (r > rank) {
                    rank = r;
                    color = severityColor(d.getSeverity());
                }
            }
        }
        return color;
    }

    // ---- syntax coloring ----

    @Override
    protected void paintLine(Graphics g, int line, String text, int x, int y, int lineHeight, Font f) {
        if (text.length() == 0) {
            return;
        }
        // Right-to-left / mixed lines need the bidi run renderer (per-token coloring falls back to the
        // default foreground on those lines; code keywords are ASCII so this only affects RTL string and
        // comment content).
        if (!BidiUtil.isTrivialLtr(text, isRTL())) {
            super.paintLine(g, line, text, x, y, lineHeight, f);
            return;
        }
        if (plainHighlighter) {
            g.setColor(getTextColor());
            g.drawString(text, x, y);
            return;
        }
        ensureStates(line + 1);
        int startState = line == 0 ? 0 : endStates[line - 1];
        SyntaxHighlightResult tl = tokenizer.tokenize(text, startState);
        if (tl == null || tl.tokens == null) {
            // the SyntaxHighlighter contract allows a null result (no highlighting for
            // this line); paint plain instead of crashing the render loop
            g.setColor(getTextColor());
            g.drawString(text, x, y);
            return;
        }
        int pos = 0;
        int defColor = getTextColor();
        for (int i = 0; i < tl.tokens.size(); i++) {
            SyntaxToken t = tl.tokens.get(i);
            if (t.start > pos) {
                drawSegment(g, f, text, pos, t.start, x, y, defColor);
            }
            int color = ThemePalette.DARK.equals(palette) ? t.darkColor : t.lightColor;
            drawSegment(g, f, text, t.start, t.start + t.length, x, y,
                    color < 0 ? palette.colorForKind(t.kind) : color);
            pos = t.start + t.length;
        }
        if (pos < text.length()) {
            drawSegment(g, f, text, pos, text.length(), x, y, defColor);
        }
    }

    private void drawSegment(Graphics g, Font f, String text, int a, int b, int x, int y, int color) {
        if (b <= a) {
            return;
        }
        int sx = x + f.substringWidth(text, 0, a);
        g.setColor(color);
        g.drawString(text.substring(a, b), sx, y);
    }

    private void ensureStates(int upto) {
        int lc = getDocument().getLineCount();
        if (upto > lc) {
            upto = lc;
        }
        if (endStates == null || endStates.length < lc) {
            int[] ns = new int[Math.max(lc, 16)];
            if (endStates != null) {
                System.arraycopy(endStates, 0, ns, 0, Math.min(endStates.length, ns.length));
            }
            endStates = ns;
        }
        if (validUpTo > lc) {
            validUpTo = lc;
        }
        for (int i = validUpTo; i < upto; i++) {
            int st = i == 0 ? 0 : endStates[i - 1];
            SyntaxHighlightResult r = tokenizer.tokenize(getDocument().getLineText(i), st);
            endStates[i] = r == null ? st : r.endState;
        }
        if (upto > validUpTo) {
            validUpTo = upto;
        }
    }

    @Override
    protected void documentEdited(int fromOffset) {
        int line = getDocument().lineOfOffset(fromOffset);
        if (line < validUpTo) {
            validUpTo = line;
        }
        if (completionVisible) {
            refilterCompletion();
        }
    }

    // ---- diagnostics ----

    @Override
    protected void paintLineDecorations(Graphics g, int line, String text, int x, int y, int lineHeight, Font f) {
        if (diagnostics == null) {
            return;
        }
        for (CodeDiagnostic d : diagnostics) {
            if (d == null || line < d.getLine() - 1 || line > d.getEndLine() - 1) {
                continue;
            }
            int cs = line == d.getLine() - 1 ? d.getColumn() - 1 : 0;
            int ce = line == d.getEndLine() - 1 ? d.getEndColumn() - 1 : text.length();
            cs = clampI(cs, 0, text.length());
            ce = clampI(ce, 0, text.length());
            if (ce <= cs) {
                ce = Math.min(text.length(), cs + 1);
            }
            int x1 = x + f.substringWidth(text, 0, cs);
            int x2 = x + f.substringWidth(text, 0, ce);
            if (x2 <= x1) {
                x2 = x1 + f.charWidth('m');
            }
            drawSquiggle(g, x1, x2, y + lineHeight - 2, severityColor(d.getSeverity()));
        }
    }

    private void drawSquiggle(Graphics g, int x1, int x2, int y, int color) {
        g.setColor(color);
        int amp = 2;
        boolean up = true;
        int x = x1;
        while (x < x2) {
            int nx = Math.min(x2, x + 3);
            g.drawLine(x, up ? y : y - amp, nx, up ? y - amp : y);
            up = !up;
            x = nx;
        }
    }

    private int severityRank(String s) {
        if (CodeDiagnostic.ERROR.equals(s)) {
            return 3;
        }
        if (CodeDiagnostic.WARNING.equals(s)) {
            return 2;
        }
        return 1;
    }

    private int severityColor(String s) {
        if (CodeDiagnostic.ERROR.equals(s)) {
            return palette.getErrorColor();
        }
        if (CodeDiagnostic.WARNING.equals(s)) {
            return palette.getWarningColor();
        }
        return palette.getInfoColor();
    }

    // ---- typing behavior: auto close, tab, auto indent ----

    @Override
    protected boolean handleTypedText(String text) {
        if (!isEditableState()) {
            return false;
        }
        if (completionVisible && ("\n".equals(text) || "\t".equals(text))) {
            acceptCompletion();
            return true;
        }
        if ("\n".equals(text)) {
            autoIndentNewline();
            return true;
        }
        if ("\t".equals(text)) {
            insertSpaces(tabSize);
            return true;
        }
        if (text.length() == 1) {
            char c = text.charAt(0);
            if (c == '(' || c == '[' || c == '{') {
                insertPair(c, closeFor(c));
                onTypedChar(c);
                return true;
            }
            if (c == ')' || c == ']' || c == '}') {
                if (skipOver(c)) {
                    return true;
                }
                replaceRange(getSelectionStart(), getSelectionEnd(), text, true);
                onTypedChar(c);
                return true;
            }
            if (c == '"' || c == '\'' || c == '`') {
                if (skipOver(c)) {
                    return true;
                }
                insertPair(c, c);
                onTypedChar(c);
                return true;
            }
            replaceRange(getSelectionStart(), getSelectionEnd(), text, true);
            onTypedChar(c);
            return true;
        }
        return false;
    }

    private static char closeFor(char open) {
        if (open == '(') {
            return ')';
        }
        if (open == '[') {
            return ']';
        }
        return '}';
    }

    private void insertPair(char open, char close) {
        int s = getSelectionStart();
        int e = getSelectionEnd();
        String inner = getDocument().substring(s, e);
        replaceRange(s, e, open + inner + close, true);
        moveCaret(s + 1 + inner.length(), false);
    }

    private boolean skipOver(char c) {
        int caret = getCaretOffset();
        if (getSelectionStart() == getSelectionEnd() && caret < getDocument().length()
                && getDocument().charAt(caret) == c) {
            moveCaret(caret + 1, false);
            return true;
        }
        return false;
    }

    private void insertSpaces(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(' ');
        }
        replaceRange(getSelectionStart(), getSelectionEnd(), sb.toString(), true);
    }

    private void autoIndentNewline() {
        int caret = getCaretOffset();
        int cl = getDocument().lineOfOffset(caret);
        int lineStart = getDocument().getLineStart(cl);
        int col = caret - lineStart;
        String line = getDocument().getLineText(cl);
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < col && i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == ' ' || ch == '\t') {
                indent.append(ch);
            } else {
                break;
            }
        }
        boolean extra = lastNonSpaceOpensBlock(line, col);
        StringBuilder ins = new StringBuilder("\n").append(indent);
        if (extra) {
            for (int i = 0; i < tabSize; i++) {
                ins.append(' ');
            }
        }
        replaceRange(getSelectionStart(), getSelectionEnd(), ins.toString(), true);
    }

    /// Returns whether the last non-whitespace character before `col` on `line` opens a block
    /// (one of `{`, `:`, `(`, `[`).
    private static boolean lastNonSpaceOpensBlock(String line, int col) {
        int i = col - 1;
        while (i >= 0 && i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i--;
        }
        if (i < 0 || i >= line.length()) {
            return false;
        }
        char ch = line.charAt(i);
        return ch == '{' || ch == ':' || ch == '(' || ch == '[';
    }

    // ---- completion ----

    private void onTypedChar(char c) {
        if (!completionEnabled) {
            return;
        }
        if (isIdentPart(c)) {
            requestCompletion();
        } else {
            hideCompletion();
        }
    }

    private void requestCompletion() {
        completionReqSeq++;
        completionPending = completionReqSeq;
        if (host() != null) {
            host().fireEditorEvent("complete", completionReqSeq + ":" + getCaretOffset());
        }
    }

    /// Called by the backend with the provider's proposals for a request.
    ///
    /// #### Parameters
    ///
    /// - `reqId`: the request sequence id
    ///
    /// - `items`: the proposals
    public void showCompletions(int reqId, List<CodeCompletion> items) {
        if (reqId != completionPending) {
            return;
        }
        completionAll = items;
        completionAnchor = identifierStart(getCaretOffset());
        completionIndex = 0;
        refilterCompletion();
    }

    private int identifierStart(int off) {
        int i = off;
        while (i > 0 && isIdentPart(getDocument().charAt(i - 1))) {
            i--;
        }
        return i;
    }

    private void refilterCompletion() {
        if (completionAll == null) {
            hideCompletion();
            return;
        }
        int caret = getCaretOffset();
        if (caret < completionAnchor) {
            hideCompletion();
            return;
        }
        String prefix = getDocument().substring(completionAnchor, caret).toLowerCase();
        List<CodeCompletion> filtered = new ArrayList<CodeCompletion>();
        for (CodeCompletion cc : completionAll) {
            if (cc == null) {
                continue;
            }
            String disp = cc.getDisplayText() != null ? cc.getDisplayText() : cc.getInsertText();
            if (disp != null && disp.toLowerCase().startsWith(prefix)) {
                filtered.add(cc);
            }
        }
        if (filtered.isEmpty()) {
            hideCompletion();
            return;
        }
        completionFiltered = filtered;
        if (completionIndex >= filtered.size()) {
            completionIndex = filtered.size() - 1;
        }
        if (completionIndex < 0) {
            completionIndex = 0;
        }
        completionVisible = true;
        repaint();
    }

    private void hideCompletion() {
        completionVisible = false;
        completionAll = null;
        completionFiltered = null;
        completionPending = -1;
        repaint();
    }

    private void acceptCompletion() {
        if (!completionVisible || completionFiltered == null || completionFiltered.isEmpty()) {
            return;
        }
        CodeCompletion cc = completionFiltered.get(clampI(completionIndex, 0, completionFiltered.size() - 1));
        String ins = cc.getInsertText() != null ? cc.getInsertText() : cc.getDisplayText();
        replaceRange(completionAnchor, getCaretOffset(), ins == null ? "" : ins, true);
        hideCompletion();
    }

    // ---- popup painting ----

    private String hoverTooltip;
    private int hoverLocalX;
    private int hoverLocalY;

    @Override
    public void pointerHover(int[] x, int[] y) {
        if (x != null && x.length > 0 && diagnostics != null) {
            int off = offsetAtPoint(x[0], y[0]);
            int localX = x[0] - getAbsoluteX();
            String msg = diagnosticMessageAt(off, localX);
            if (msg != null) {
                hoverTooltip = msg;
                hoverLocalX = localX;
                hoverLocalY = y[0] - getAbsoluteY();
                repaint();
                return;
            }
        }
        if (hoverTooltip != null) {
            hoverTooltip = null;
            repaint();
        }
    }

    private String diagnosticMessageAt(int off, int localX) {
        int line = getDocument().lineOfOffset(off);
        boolean inGutter = localX < getContentLeftInset();
        for (CodeDiagnostic d : diagnostics) {
            if (d == null) {
                continue;
            }
            if (line < d.getLine() - 1 || line > d.getEndLine() - 1) {
                continue;
            }
            if (inGutter) {
                return d.getMessage();
            }
            int sOff = lineColToOffset(d.getLine(), d.getColumn());
            int eOff = lineColToOffset(d.getEndLine(), d.getEndColumn());
            if (off >= sOff && off < eOff) {
                return d.getMessage();
            }
        }
        return null;
    }

    private int lineColToOffset(int line1, int col1) {
        int line = clampI(line1 - 1, 0, getDocument().getLineCount() - 1);
        int base = getDocument().getLineStart(line);
        int max = getDocument().getLineEnd(line);
        return clampI(base + col1 - 1, base, max);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (completionVisible && completionFiltered != null && !completionFiltered.isEmpty()) {
            paintCompletion(g);
        }
        if (hoverTooltip != null) {
            paintTooltip(g);
        }
    }

    private void paintTooltip(Graphics g) {
        Font f = getEditorFont();
        int pad = f.charWidth('m');
        int tw = f.stringWidth(hoverTooltip) + pad * 2;
        int th = f.getHeight() + pad;
        int tx = getX() + hoverLocalX;
        int ty = getY() + hoverLocalY + getLineHeight() / 2;
        if (tx + tw > getX() + getWidth()) {
            tx = getX() + getWidth() - tw;
        }
        if (tx < getX()) {
            tx = getX();
        }
        if (ty + th > getY() + getHeight()) {
            ty = getY() + hoverLocalY - th;
        }
        g.setColor(0x111111);
        g.fillRect(tx, ty, tw, th);
        g.setColor(0x454545);
        g.drawRect(tx, ty, tw - 1, th - 1);
        g.setColor(0xf0f0f0);
        g.setFont(f);
        g.drawString(hoverTooltip, tx + pad, ty + pad / 2);
    }

    private void paintCompletion(Graphics g) {
        Font f = getEditorFont();
        int lh = getLineHeight();
        int caret = getCaretOffset();
        int cl = getDocument().lineOfOffset(caret);
        int col = caret - getDocument().getLineStart(cl);
        String line = getDocument().getLineText(cl);
        if (col > line.length()) {
            col = line.length();
        }
        int px = getX() + getContentLeftInset() + f.substringWidth(line, 0, col) - getHorizontalScroll();
        int top = getY() + lineTop(cl);
        int visible = Math.min(completionFiltered.size(), 8);
        int itemH = lh;
        int boxW = 0;
        for (CodeCompletion cc : completionFiltered) {
            String d = cc.getDisplayText() != null ? cc.getDisplayText() : cc.getInsertText();
            if (d != null) {
                boxW = Math.max(boxW, f.stringWidth(d));
            }
        }
        boxW += f.charWidth('m') * 2;
        int boxH = visible * itemH + 2;
        int boxY = top + lh;
        if (boxY + boxH > getY() + getHeight()) {
            boxY = top - boxH;
        }
        boolean dark = palette == ThemePalette.DARK; // NOPMD - intentional identity comparison
        int popupBg = dark ? 0x252526 : 0xffffff;
        int border = dark ? 0x454545 : 0xc0c0c0;
        g.setColor(popupBg);
        g.fillRect(px, boxY, boxW, boxH);
        g.setColor(border);
        g.drawRect(px, boxY, boxW - 1, boxH - 1);
        int start = 0;
        if (completionIndex >= visible) {
            start = completionIndex - visible + 1;
        }
        for (int i = 0; i < visible; i++) {
            int idx = start + i;
            if (idx >= completionFiltered.size()) {
                break;
            }
            CodeCompletion cc = completionFiltered.get(idx);
            int iy = boxY + 1 + i * itemH;
            if (idx == completionIndex) {
                g.setColor(palette.getSelection());
                g.fillRect(px + 1, iy, boxW - 2, itemH);
            }
            String d = cc.getDisplayText() != null ? cc.getDisplayText() : cc.getInsertText();
            g.setColor(palette.getForeground());
            g.drawString(d == null ? "" : d, px + f.charWidth('m'), iy);
        }
        // remember the popup bounds (component local) so touch taps can hit an item
        popupLocalX = px - getX();
        popupLocalY = boxY - getY();
        popupW = boxW;
        popupH = boxH;
        popupItemH = itemH;
        popupStart = start;
    }

    private int popupLocalX;
    private int popupLocalY;
    private int popupW;
    private int popupH;
    private int popupItemH = 1;
    private int popupStart;

    @Override
    public void pointerPressed(int x, int y) {
        int lx = x - getAbsoluteX();
        int ly = y - getAbsoluteY();
        // tap on the completion popup accepts the tapped proposal (touch has no arrow-key nav)
        if (completionVisible && completionFiltered != null && !completionFiltered.isEmpty()
                && popupItemH > 0 && lx >= popupLocalX && lx <= popupLocalX + popupW
                && ly >= popupLocalY && ly <= popupLocalY + popupH) {
            int idx = popupStart + (ly - popupLocalY) / popupItemH;
            if (idx >= 0 && idx < completionFiltered.size()) {
                completionIndex = idx;
                acceptCompletion();
            }
            return;
        }
        // tap on a squiggle / gutter icon shows its diagnostic (no hover on touch devices)
        if (diagnostics != null) {
            int off = offsetAtPoint(x, y);
            String msg = diagnosticMessageAt(off, lx);
            if (msg != null) {
                hoverTooltip = msg;
                hoverLocalX = lx;
                hoverLocalY = ly;
                repaint();
            } else if (hoverTooltip != null) {
                hoverTooltip = null;
                repaint();
            }
        }
        super.pointerPressed(x, y);
    }

    // ---- key routing for completion ----

    @Override
    public void keyPressed(int keyCode) {
        if (completionVisible && navigateCompletion(Display.getInstance().getGameAction(keyCode))) {
            return;
        }
        super.keyPressed(keyCode);
    }

    @Override
    public void keyRepeated(int keyCode) {
        if (completionVisible && navigateCompletion(Display.getInstance().getGameAction(keyCode))) {
            return;
        }
        super.keyRepeated(keyCode);
    }

    @Override
    public void keyReleased(int keyCode) {
        if (completionVisible) {
            if (keyCode == 27) {
                hideCompletion();
                return;
            }
            if (keyCode == 10 || keyCode == 13 || keyCode == 9) {
                acceptCompletion();
                return;
            }
        }
        super.keyReleased(keyCode);
    }

    private boolean navigateCompletion(int action) {
        if (action == Display.GAME_UP) {
            completionIndex = Math.max(0, completionIndex - 1);
            repaint();
            return true;
        }
        if (action == Display.GAME_DOWN) {
            completionIndex = Math.min(completionFiltered.size() - 1, completionIndex + 1);
            repaint();
            return true;
        }
        return false;
    }

    @Override
    public void onKeyCommand(int command, int modifiers) {
        if (completionVisible) {
            if (command == KEY_UP) {
                completionIndex = Math.max(0, completionIndex - 1);
                repaint();
                return;
            }
            if (command == KEY_DOWN) {
                completionIndex = Math.min(completionFiltered.size() - 1, completionIndex + 1);
                repaint();
                return;
            }
            if (command == KEY_ESCAPE) {
                hideCompletion();
                return;
            }
        }
        super.onKeyCommand(command, modifiers);
    }

    @Override
    public void onEditorAction(int action) {
        if (completionVisible && action == TextInputConfig.ACTION_DEFAULT) {
            acceptCompletion();
            return;
        }
        super.onEditorAction(action);
    }

    private static boolean isIdentPart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '$';
    }

    private static int clampI(int v, int lo, int hi) {
        if (v < lo) {
            return lo;
        }
        if (v > hi) {
            return hi;
        }
        return v;
    }
}
