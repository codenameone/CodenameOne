/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.editor;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.TextInputClient;
import com.codename1.ui.TextInputConfig;
import com.codename1.ui.TextInputState;
import com.codename1.ui.RichTextClipboardData;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.WheelEvent;
import com.codename1.ui.geom.Dimension;

/// The pure Codename One text editing surface. It renders a plain text `EditorDocument` with its own
/// `Graphics` code, owns the caret and selection, handles pointer and keyboard interaction, and captures
/// text through the low level `TextInputClient` contract (soft keyboard / IME) when the platform
/// supports it, falling back to raw `keyPressed` capture on physical keyboard platforms.
///
/// This class provides the shared editing behavior. Feature layers (syntax highlighting, the gutter,
/// styled rich text) subclass it and override `#paintContent(Graphics, int, int, int, int)` and the
/// document/measurement hooks.
public class EditorView extends Component implements TextInputClient {
    private final EditorHost host;
    private final boolean codeMode;
    private EditorDocument doc = new EditorDocument();
    private final UndoManager undo = new UndoManager();

    private int caret;
    private int anchor = -1;
    private int composingStart = -1;
    private int composingEnd = -1;
    private boolean editable = true;

    private int scrollY;
    private int scrollX;
    private boolean caretOn = true;
    private long lastBlink;
    private boolean animRegistered;

    private Object inputHandle;
    private boolean inputActive;

    private int bgColor = 0xffffff;
    private int fgColor = 0x111111;
    private int selColor = 0xb3d4fc;

    /// Creates an editor view.
    ///
    /// #### Parameters
    ///
    /// - `host`: the bridge to the owning editor component / platform input source
    ///
    /// - `codeMode`: true for a code editor (autocorrect and autocapitalize disabled)
    public EditorView(EditorHost host, boolean codeMode) {
        this.host = host;
        this.codeMode = codeMode;
        setFocusable(true);
        setGrabsPointerEvents(true);
        // own the arrow / enter / tab keys so the form's focus traversal does not steal them
        setHandlesInput(true);
        // handle the mouse wheel / trackpad (shift = horizontal) ourselves so scrolling works without a
        // scrollable ancestor; consuming the event stops the default scroll gesture from selecting text
        addMouseWheelListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (evt instanceof WheelEvent) {
                    WheelEvent we = (WheelEvent) evt;
                    scrollByPixels(we.getDeltaX(), we.getDeltaY());
                    evt.consume();
                }
            }
        });
    }

    /// Scrolls the content by the given pixel deltas (as delivered by the mouse wheel gesture).
    protected void scrollByPixels(int deltaX, int deltaY) {
        scrollX = clampInt(scrollX - deltaX, 0, maxScrollX());
        scrollY = clampInt(scrollY - deltaY, 0, maxScroll());
        repaint();
    }

    /// Returns the backing document.
    public EditorDocument getDocument() {
        return doc;
    }

    /// Returns the undo manager.
    public UndoManager getUndoManager() {
        return undo;
    }

    /// Returns the current caret offset.
    public int getCaretOffset() {
        return caret;
    }

    /// Sets the surface background color.
    public void setBackgroundColor(int color) {
        this.bgColor = color;
    }

    /// Sets the default text color.
    public void setTextColor(int color) {
        this.fgColor = color;
    }

    /// Sets the selection highlight color.
    public void setSelectionColor(int color) {
        this.selColor = color;
    }

    /// Returns the default text color used by the base renderer.
    protected int getTextColor() {
        return fgColor;
    }

    /// Replaces the whole document text and resets the caret and history.
    public void setText(String text) {
        doc.setText(text);
        caret = 0;
        anchor = -1;
        composingStart = -1;
        composingEnd = -1;
        undo.clear();
        scrollY = 0;
        scrollX = 0;
        documentEdited(0);
        documentReset();
        onDocumentChanged();
    }

    /// Returns the whole document text.
    public String getText() {
        return doc.getText();
    }

    /// Enables or disables editing.
    public void setEditableState(boolean editable) {
        this.editable = editable;
    }

    /// True when editing is enabled.
    public boolean isEditableState() {
        return editable;
    }

    /// Relinquishes focus and stops the active platform text-input session.
    public void blur() {
        com.codename1.ui.Form form = getComponentForm();
        if (form != null && equals(form.getFocused())) {
            form.setFocused(null);
        } else {
            stopInput();
        }
    }

    // ---- font / geometry ----

    private Font cachedBase;
    private Font cachedFrom;
    private int fontSizeOverridePx;

    /// Returns the base editor font size in pixels.
    protected int baseFontSizePixels() {
        return getEditorFont().getHeight();
    }

    /// Optionally overrides the editor font size in device independent pixels. Pass 0 to use the font
    /// supplied by the component's style (the default, which is correctly sized for the current skin).
    public void setFontSizeDips(int dips) {
        this.fontSizeOverridePx = dips <= 0 ? 0 : Display.getInstance().convertToPixels((float) dips);
        cachedBase = null;
        invalidateLayout();
        repaint();
    }

    /// Returns the font used to render text. By default this is the component's style font (managed and
    /// correctly sized by Codename One, and refreshed when the skin / theme reloads, which avoids stale
    /// native font handles rendering as squares). Subclasses may override to supply styled variants.
    protected Font getEditorFont() {
        Font sf = getStyle().getFont();
        if (sf == null) {
            sf = Font.getDefaultFont();
        }
        if (cachedBase != null && cachedFrom == sf) { // NOPMD - intentional identity comparison
            return cachedBase;
        }
        cachedFrom = sf;
        Font result = sf;
        if (fontSizeOverridePx > 0 && sf.isTTFNativeFont()) {
            try {
                result = sf.derive(fontSizeOverridePx, Font.STYLE_PLAIN);
            } catch (Throwable t) {
                result = sf;
            }
        }
        cachedBase = result;
        return result;
    }

    /// Returns the default height of a single visual line in pixels.
    protected int getLineHeight() {
        int h = getEditorFont().getHeight();
        return h + Math.max(2, h / 8);
    }

    /// Returns the height of the given line. The default is uniform (`#getLineHeight()`); the rich text
    /// editor overrides this for headings and font size runs.
    protected int lineHeightAt(int line) {
        return getLineHeight();
    }

    /// True when every line has the same height (the common case, and the fast path). Feature layers with
    /// variable line heights override this to return false.
    protected boolean uniformLineHeight() {
        return true;
    }

    private int[] cumTops;
    private boolean layoutDirty = true;

    /// Invalidates the cached line layout (used by feature layers when line heights change).
    protected void invalidateLayout() {
        layoutDirty = true;
        cachedMaxLineWidth = -1;
    }

    private void ensureCum() {
        int lc = doc.getLineCount();
        if (!layoutDirty && cumTops != null && cumTops.length == lc + 1) {
            return;
        }
        cumTops = new int[lc + 1];
        int y = 0;
        for (int i = 0; i < lc; i++) {
            cumTops[i] = y;
            y += lineHeightAt(i);
        }
        cumTops[lc] = y;
        layoutDirty = false;
    }

    /// Returns the top y pixel of the given line relative to the content top (before scroll).
    protected int lineTopContent(int line) {
        if (uniformLineHeight()) {
            return line * getLineHeight();
        }
        ensureCum();
        int l = line < 0 ? 0 : (line > doc.getLineCount() ? doc.getLineCount() : line);
        return cumTops[l];
    }

    /// Returns the total content height in pixels.
    protected int contentTotalHeight() {
        if (uniformLineHeight()) {
            return doc.getLineCount() * getLineHeight();
        }
        ensureCum();
        return cumTops[doc.getLineCount()];
    }

    /// Returns the line index at the given content relative y pixel.
    protected int lineAtContentY(int y) {
        int lc = doc.getLineCount();
        if (uniformLineHeight()) {
            return clampInt(y / getLineHeight(), 0, lc - 1);
        }
        ensureCum();
        int lo = 0;
        int hi = lc - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (cumTops[mid] <= y) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }

    /// Returns the left inset (padding plus any gutter) at which text starts.
    protected int getContentLeftInset() {
        return getStyle().getPaddingLeftNoRTL();
    }

    private int contentX() {
        return getX() + getContentLeftInset();
    }

    private int contentY() {
        return getY() + getStyle().getPaddingTop();
    }

    /// Returns the width available for text (component width minus padding and any gutter).
    protected int contentWidth() {
        return getWidth() - getContentLeftInset() - getStyle().getPaddingRightNoRTL();
    }

    private int contentHeight() {
        return getHeight() - getStyle().getPaddingTop() - getStyle().getPaddingBottom();
    }

    // ---- selection helpers ----

    /// True when a non empty selection exists.
    public boolean hasSelection() {
        return anchor >= 0 && anchor != caret;
    }

    /// Returns the inclusive start offset of the selection (or the caret when none).
    public int getSelectionStart() {
        if (anchor < 0) {
            return caret;
        }
        return Math.min(anchor, caret);
    }

    /// Returns the exclusive end offset of the selection (or the caret when none).
    public int getSelectionEnd() {
        if (anchor < 0) {
            return caret;
        }
        return Math.max(anchor, caret);
    }

    // ---- painting ----

    @Override
    public void paint(Graphics g) {
        g.setColor(bgColor);
        g.fillRect(getX(), getY(), getWidth(), getHeight());
        paintContent(g, contentX(), contentY() - scrollY, contentWidth(), contentHeight());
        if (touchSelection && hasSelection()) {
            paintSelectionUI(g);
        }
        if (loupeActive) {
            paintLoupe(g);
        }
    }

    // ---- touch selection UI (long-press, draggable handles, context toolbar, magnifier loupe) ----

    private boolean touchSelection;
    private int draggingHandle;
    private int[] startHandleLocal;
    private int[] endHandleLocal;
    private int[][] toolbarButtons;
    private java.util.List<String> toolbarLabels;
    private boolean loupeActive;
    private int loupeOffset;
    private Font loupeFontCache;
    private Font loupeBaseFont;

    /// The magnified font used inside the loupe (1.5x the editor font), cached against the base font so a
    /// theme/skin change regenerates it.
    private Font loupeFont() {
        Font base = getEditorFont();
        if (loupeFontCache == null || loupeBaseFont != base) { // NOPMD - intentional identity comparison
            loupeBaseFont = base;
            loupeFontCache = base.derive(base.getHeight() * 1.5f, Font.STYLE_PLAIN);
        }
        return loupeFontCache;
    }

    /// Draws the selection magnifier: a rounded callout above the touched offset showing the text around
    /// it enlarged, with a caret marker centered on the touch. Shown while placing the caret with a long
    /// press and while dragging a selection handle, mirroring the native touch selection feel.
    private void paintLoupe(Graphics g) {
        int off = doc.clamp(loupeOffset);
        int ln = doc.lineOfOffset(off);
        String line = doc.getLineText(ln);
        int col = off - doc.getLineStart(ln);
        Font base = getEditorFont();
        Font big = loupeFont();
        int lh = big.getHeight();
        int pad = base.charWidth('m');
        int bubbleH = lh + pad;
        int bubbleW = Math.min(getWidth() - 8, big.charWidth('m') * 16);
        int caretBaseX = measureColumnX(ln, line, col, base);
        int caretLocalX = getContentLeftInset() + lineContentOffsetX(ln) + caretBaseX - scrollX;
        int caretLocalY = getStyle().getPaddingTop() + lineTopContent(ln) - scrollY;
        int bx = clampInt(caretLocalX - bubbleW / 2, 4, Math.max(4, getWidth() - 4 - bubbleW));
        int by = caretLocalY - bubbleH - handleRadius() * 3;
        if (by < 4) {
            by = caretLocalY + lineHeightAt(ln) + handleRadius() * 3;
        }
        int ax = getX() + bx;
        int ay = getY() + by;
        int arc = Math.max(8, bubbleH / 2);
        g.setColor(0xffffff);
        g.fillRoundRect(ax, ay, bubbleW, bubbleH, arc, arc);
        g.setColor(0x1a73e8);
        g.drawRoundRect(ax, ay, bubbleW, bubbleH, arc, arc);
        g.pushClip();
        g.clipRect(ax + 2, ay + 2, bubbleW - 4, bubbleH - 4);
        // scale the (bidi aware) caret x from the base to the magnified font so the touched glyph is centered
        int scaledCaretX = caretBaseX * lh / Math.max(1, base.getHeight());
        int textX = ax + bubbleW / 2 - scaledCaretX;
        int textY = ay + (bubbleH - lh) / 2;
        g.setColor(fgColor);
        g.setFont(big);
        g.drawString(line, textX, textY);
        g.setColor(0x1a73e8);
        g.fillRect(ax + bubbleW / 2, textY, Math.max(1, lh / 14), lh);
        g.popClip();
        g.setFont(base);
    }

    private int handleRadius() {
        return Math.max(6, getEditorFont().getHeight() / 3);
    }

    // returns {localX, localYTop, lineHeight} for an offset (component-local, matches paint origin getX/getY)
    private int[] offsetLocal(int off) {
        Font f = getEditorFont();
        int line = doc.lineOfOffset(off);
        int col = off - doc.getLineStart(line);
        String text = doc.getLineText(line);
        if (col > text.length()) {
            col = text.length();
        }
        int lx = getContentLeftInset() + lineContentOffsetX(line) + measureColumnX(line, text, col, f) - scrollX;
        int ly = getStyle().getPaddingTop() + lineTopContent(line) - scrollY;
        return new int[]{lx, ly, lineHeightAt(line)};
    }

    private void paintSelectionUI(Graphics g) {
        int hr = handleRadius();
        int[] s = offsetLocal(getSelectionStart());
        int[] e = offsetLocal(getSelectionEnd());
        int color = 0x1a73e8;
        // start handle: stem + ball above the line top
        g.setColor(color);
        g.fillRect(getX() + s[0] - 1, getY() + s[1], 2, s[2]);
        g.fillArc(getX() + s[0] - hr, getY() + s[1] - hr * 2, hr * 2, hr * 2, 0, 360);
        startHandleLocal = new int[]{s[0], s[1] - hr};
        // end handle: stem + ball below the line bottom
        g.setColor(color);
        g.fillRect(getX() + e[0] - 1, getY() + e[1], 2, e[2]);
        g.fillArc(getX() + e[0] - hr, getY() + e[1] + e[2], hr * 2, hr * 2, 0, 360);
        endHandleLocal = new int[]{e[0], e[1] + e[2] + hr};
        paintToolbar(g, s);
    }

    private void paintToolbar(Graphics g, int[] anchor) {
        Font f = getEditorFont();
        java.util.List<String> labels = new java.util.ArrayList<String>();
        labels.add("Copy");
        if (editable) {
            labels.add("Cut");
            labels.add("Paste");
        }
        labels.add("Select All");
        int pad = f.charWidth('m');
        int bh = f.getHeight() + pad;
        int[] w = new int[labels.size()];
        int total = 0;
        for (int i = 0; i < labels.size(); i++) {
            w[i] = f.stringWidth(labels.get(i)) + pad * 2;
            total += w[i];
        }
        int tx = anchor[0] - total / 2;
        int ty = anchor[1] - bh - handleRadius() * 2;
        if (tx < 0) {
            tx = 0;
        }
        if (tx + total > getWidth()) {
            tx = getWidth() - total;
        }
        if (ty < 0) {
            ty = anchor[1] + anchor[2] + handleRadius() * 2;
        }
        g.setColor(0x222222);
        g.fillRect(getX() + tx, getY() + ty, total, bh);
        toolbarButtons = new int[labels.size()][4];
        toolbarLabels = labels;
        int cx = tx;
        for (int i = 0; i < labels.size(); i++) {
            toolbarButtons[i] = new int[]{cx, ty, w[i], bh};
            g.setColor(0xf0f0f0);
            g.setFont(f);
            g.drawString(labels.get(i), getX() + cx + pad, getY() + ty + pad / 2);
            cx += w[i];
            if (i < labels.size() - 1) {
                g.setColor(0x555555);
                g.drawLine(getX() + cx, getY() + ty + 4, getX() + cx, getY() + ty + bh - 4);
            }
        }
    }

    @Override
    public void longPointerPress(int x, int y) {
        int off = offsetAtPoint(x, y);
        selectWordAt(off);
        touchSelection = hasSelection();
        loupeActive = true;
        loupeOffset = off;
        repaint();
    }

    private boolean nearLocal(int[] handleLocal, int x, int y) {
        if (handleLocal == null) {
            return false;
        }
        int lx = x - getAbsoluteX();
        int ly = y - getAbsoluteY();
        int r = handleRadius() * 3;
        int dx = lx - handleLocal[0];
        int dy = ly - handleLocal[1];
        return dx * dx + dy * dy <= r * r;
    }

    private boolean handleTouchSelectionPress(int x, int y) {
        if (!touchSelection) {
            return false;
        }
        int lx = x - getAbsoluteX();
        int ly = y - getAbsoluteY();
        if (toolbarButtons != null) {
            for (int i = 0; i < toolbarButtons.length; i++) {
                int[] r = toolbarButtons[i];
                if (lx >= r[0] && lx <= r[0] + r[2] && ly >= r[1] && ly <= r[1] + r[3]) {
                    doToolbarAction(toolbarLabels.get(i));
                    return true;
                }
            }
        }
        if (nearLocal(startHandleLocal, x, y)) {
            anchor = getSelectionEnd();
            caret = getSelectionStart();
            draggingHandle = 1;
            return true;
        }
        if (nearLocal(endHandleLocal, x, y)) {
            anchor = getSelectionStart();
            caret = getSelectionEnd();
            draggingHandle = 2;
            return true;
        }
        // tap outside the selection UI dismisses it
        touchSelection = false;
        startHandleLocal = null;
        endHandleLocal = null;
        toolbarButtons = null;
        return false;
    }

    private void doToolbarAction(String label) {
        if ("Copy".equals(label)) {
            copySelection();
        } else if ("Cut".equals(label)) {
            cutSelection();
        } else if ("Paste".equals(label)) {
            pasteClipboard();
        } else if ("Select All".equals(label)) {
            selectAll();
            touchSelection = true;
            repaint();
            return;
        }
        touchSelection = false;
        repaint();
    }

    /// Paints the document content. `originY` is already offset by the scroll position, so line `n` is at
    /// `originY + n * getLineHeight()`. Subclasses override to add highlighting, a gutter or styled runs
    /// but should call through for the shared caret / selection rendering, or reimplement it.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics context
    ///
    /// - `originX`: the x pixel at which line text starts
    ///
    /// - `originY`: the y pixel of the first line (scroll adjusted)
    ///
    /// - `width`: the content width
    ///
    /// - `height`: the content height
    protected void paintContent(Graphics g, int originX, int originY, int width, int height) {
        Font f = getEditorFont();
        g.setFont(f);
        int lineCount = doc.getLineCount();
        int first = lineAtContentY(scrollY);
        int last = Math.min(lineCount - 1, lineAtContentY(scrollY + height) + 1);
        paintBeforeContent(g, originX, originY, first, last, getLineHeight(), f);
        // clip the text area (so horizontally scrolled text does not paint over the gutter) and apply
        // the horizontal scroll offset
        g.pushClip();
        g.clipRect(originX, getY() + getStyle().getPaddingTop(), width, height);
        int tx = originX - scrollX;
        int ss = getSelectionStart();
        int se = getSelectionEnd();
        int eolPad = f.charWidth(' ');
        for (int ln = first; ln <= last; ln++) {
            int ly = originY + lineTopContent(ln);
            int lh = lineHeightAt(ln);
            String text = doc.getLineText(ln);
            int lineStart = doc.getLineStart(ln);
            int lineEnd = doc.getLineEnd(ln);
            if (ss != se && ss <= lineEnd && se >= lineStart) {
                int lineOff = lineContentOffsetX(ln);
                int a = clampInt(ss - lineStart, 0, text.length());
                boolean pastEol = se > lineEnd;
                int b = pastEol ? text.length() : clampInt(se - lineStart, 0, text.length());
                g.setColor(selColor);
                int[] segs = lineVisualSegments(ln, text, a, b, f);
                for (int si = 0; si < segs.length; si += 2) {
                    g.fillRect(tx + lineOff + segs[si], ly, segs[si + 1], lh);
                }
                if (pastEol) {
                    // selection continues across the line break: pad at the visual end of the line
                    int endX = tx + lineOff + measureColumnX(ln, text, text.length(), f);
                    g.fillRect(endX, ly, eolPad, lh);
                }
            }
            paintLine(g, ln, text, tx, ly, lh, f);
            paintLineDecorations(g, ln, text, tx, ly, lh, f);
            paintComposingUnderline(g, ln, text, lineStart, lineEnd, tx, ly, lh, f);
        }
        paintCaret(g, tx, originY, f);
        g.popClip();
    }

    /// Draws the IME composition (marked text) underline for the portion of the active composing range
    /// that falls on the given line. The composing range is set by `#setComposingText(String, int)` while
    /// an input method is composing (e.g. CJK) and cleared by `#finishComposing()`.
    protected void paintComposingUnderline(Graphics g, int line, String text, int lineStart, int lineEnd,
            int tx, int ly, int lineHeight, Font f) {
        if (composingStart < 0 || composingEnd <= composingStart) {
            return;
        }
        if (composingStart > lineEnd || composingEnd < lineStart) {
            return;
        }
        int lineOff = lineContentOffsetX(line);
        int a = clampInt(composingStart - lineStart, 0, text.length());
        int b = clampInt(Math.min(composingEnd, lineEnd) - lineStart, 0, text.length());
        int xa = tx + lineOff + measureColumnX(line, text, a, f);
        int xb = tx + lineOff + measureColumnX(line, text, b, f);
        int left = Math.min(xa, xb);
        int w = Math.abs(xb - xa);
        if (w <= 0) {
            return;
        }
        int thickness = Math.max(1, lineHeight / 16);
        int uy = ly + lineHeight - thickness - Math.max(1, lineHeight / 12);
        g.setColor(fgColor);
        g.fillRect(left, uy, w, thickness);
    }

    /// Hook invoked before the visible lines are drawn, used by the code editor to paint the line number
    /// gutter. `originY` is scroll adjusted so line `n` sits at `originY + n * lineHeight`.
    protected void paintBeforeContent(Graphics g, int originX, int originY, int firstLine, int lastLine, int lineHeight, Font f) {
    }

    /// Hook invoked after a line's text is drawn, used by the code editor to paint diagnostic squiggles.
    protected void paintLineDecorations(Graphics g, int line, String text, int x, int y, int lineHeight, Font f) {
    }

    /// Returns the y pixel of the top of the given line in component local coordinates (scroll adjusted).
    protected int lineTop(int line) {
        return getStyle().getPaddingTop() - scrollY + lineTopContent(line);
    }

    /// Returns the first fully or partially visible line index.
    protected int firstVisibleLine() {
        return lineAtContentY(scrollY);
    }

    /// Returns the current horizontal scroll offset in pixels.
    protected int getHorizontalScroll() {
        return scrollX;
    }

    /// Returns the current vertical scroll offset in pixels.
    protected int getVerticalScroll() {
        return scrollY;
    }

    /// Returns the largest valid vertical content offset.
    protected int getMaximumVerticalScroll() {
        return maxScroll();
    }

    /// Paints a single line of text. Subclasses override to add syntax coloring; the base implementation
    /// draws the whole line in the default text color.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics context
    ///
    /// - `line`: the zero based line index
    ///
    /// - `text`: the line text
    ///
    /// - `x`: the x pixel at which the line text starts
    ///
    /// - `y`: the top y pixel of the line
    ///
    /// - `lineHeight`: the line height
    ///
    /// - `f`: the font
    protected void paintLine(Graphics g, int line, String text, int x, int y, int lineHeight, Font f) {
        if (text.length() == 0) {
            return;
        }
        g.setColor(fgColor);
        if (BidiUtil.isTrivialLtr(text, isRTL())) {
            g.drawString(text, x, y);
            return;
        }
        // Draw each directional run as its own substring at its visual x; the platform font engine shapes
        // and orders the glyphs within the (single-direction) run.
        int[][] runs = bidiRuns(text, f);
        for (int[] run : runs) {
            g.drawString(text.substring(run[0], run[1]), x + run[3], y);
        }
    }

    private void paintCaret(Graphics g, int originX, int originY, Font f) {
        if (!caretOn || !hasFocus() || !editable) {
            return;
        }
        int cl = doc.lineOfOffset(caret);
        int col = caret - doc.getLineStart(cl);
        String line = doc.getLineText(cl);
        if (col > line.length()) {
            col = line.length();
        }
        int cx = originX + lineContentOffsetX(cl) + measureColumnX(cl, line, col, f);
        int cy = originY + lineTopContent(cl);
        int cw = Math.max(1, f.getHeight() / 16);
        g.setColor(fgColor);
        g.fillRect(cx, cy, cw, lineHeightAt(cl));
    }

    private static int clampInt(int v, int lo, int hi) {
        if (v < lo) {
            return lo;
        }
        if (v > hi) {
            return hi;
        }
        return v;
    }

    // ---- hit testing ----

    /// Returns the document offset nearest the given absolute screen point.
    ///
    /// #### Parameters
    ///
    /// - `absX`: absolute x
    ///
    /// - `absY`: absolute y
    @Override
    public int offsetAtPoint(int absX, int absY) {
        Font f = getEditorFont();
        // pointer coordinates are absolute (form space); convert to content-local
        int localY = absY - getAbsoluteY() - getStyle().getPaddingTop() + scrollY;
        int ln = lineAtContentY(localY);
        int lineStart = doc.getLineStart(ln);
        String text = doc.getLineText(ln);
        int localX = absX - getAbsoluteX() - getContentLeftInset() + scrollX - lineContentOffsetX(ln);
        return lineStart + columnAtX(ln, text, localX, f);
    }

    // ---- editing primitives ----

    /// Replaces the range `[start, end)` with `text`, updating the caret, history and platform input
    /// state.
    ///
    /// #### Parameters
    ///
    /// - `start`: inclusive start offset
    ///
    /// - `end`: exclusive end offset
    ///
    /// - `text`: the replacement text (may be empty)
    ///
    /// - `record`: true to record the mutation for undo
    protected void replaceRange(int start, int end, String text, boolean record) {
        start = doc.clamp(start);
        end = doc.clamp(end);
        if (start > end) {
            int t = start;
            start = end;
            end = t;
        }
        String value = EditorDocument.normalizeText(text == null ? "" : text);
        String removed = doc.substring(start, end);
        Object beforeState = record ? captureDocumentState() : null;
        doc.delete(start, end);
        doc.insert(start, value);
        caret = start + value.length();
        anchor = -1;
        documentEdited(start);
        documentReplaced(start, removed, value);
        if (record) {
            undo.record(start, removed, value, beforeState, captureDocumentState());
        }
        onDocumentChanged();
    }

    /// Hook invoked when the document is mutated at `fromOffset`, used by feature layers (e.g. the code
    /// editor) to invalidate cached tokenization from the affected line onward.
    protected void documentEdited(int fromOffset) {
    }

    /// Hook invoked when a range is replaced, carrying the exact removed and inserted text so feature
    /// layers (e.g. the rich text editor) can keep a parallel style / block model in sync. The default is
    /// a no-op.
    ///
    /// #### Parameters
    ///
    /// - `start`: the offset at which the replacement happened
    ///
    /// - `removed`: the removed text
    ///
    /// - `inserted`: the inserted text
    protected void documentReplaced(int start, String removed, String inserted) {
    }

    /// Hook invoked when the whole document is reset (e.g. by `#setText(String)`), so feature layers can
    /// rebuild their parallel models. The default is a no-op.
    protected void documentReset() {
    }

    /// Captures feature-layer state associated with the document for undo/redo. Plain and code editors
    /// return null; rich editors override this to snapshot formatting, blocks, links and images.
    protected Object captureDocumentState() {
        return null;
    }

    /// Restores a feature-layer snapshot captured by `#captureDocumentState()`.
    protected void restoreDocumentState(Object state) {
    }

    /// Refreshes the most recent undo unit after a feature layer finishes decorating an inserted range.
    protected final void refreshUndoAfterState() {
        undo.updateLastAfterState(captureDocumentState());
    }

    /// Inserts text at the caret, replacing any active selection.
    public void insertText(String text) {
        if (!editable || text == null || text.length() == 0) {
            return;
        }
        if (handleTypedText(text)) {
            return;
        }
        replaceRange(getSelectionStart(), getSelectionEnd(), text, true);
    }

    /// Hook giving feature layers a chance to intercept typed text before the default insertion (used by
    /// the code editor for bracket / quote auto close, tab expansion and auto indent). Return true when
    /// the text was consumed. The default returns false.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text being typed / committed
    protected boolean handleTypedText(String text) {
        return false;
    }

    /// Returns the host bridge, for feature layers that need to fire editor events.
    protected EditorHost host() {
        return host;
    }

    private void deleteBackward() {
        if (!editable) {
            return;
        }
        if (hasSelection()) {
            replaceRange(getSelectionStart(), getSelectionEnd(), "", true);
            return;
        }
        if (caret > 0) {
            int prev = caret - 1;
            if (prev > 0 && Character.isLowSurrogate(doc.charAt(prev)) && Character.isHighSurrogate(doc.charAt(prev - 1))) {
                prev--;
            }
            replaceRange(prev, caret, "", true);
        }
    }

    private void deleteForward() {
        if (!editable) {
            return;
        }
        if (hasSelection()) {
            replaceRange(getSelectionStart(), getSelectionEnd(), "", true);
            return;
        }
        if (caret < doc.length()) {
            int next = caret + 1;
            if (next < doc.length() && Character.isHighSurrogate(doc.charAt(caret)) && Character.isLowSurrogate(doc.charAt(next))) {
                next++;
            }
            replaceRange(caret, next, "", true);
        }
    }

    // ---- navigation ----

    /// Moves the caret to a new offset, optionally extending the selection.
    ///
    /// #### Parameters
    ///
    /// - `newCaret`: the target offset (clamped to the document)
    ///
    /// - `extend`: true to extend the selection from the current anchor
    public void moveCaret(int newCaret, boolean extend) {
        touchSelection = false;
        newCaret = doc.clamp(newCaret);
        if (extend) {
            if (anchor < 0) {
                anchor = caret;
            }
        } else {
            anchor = -1;
        }
        caret = newCaret;
        undo.breakRun();
        resetBlink();
        scrollCaretVisible();
        pushInputState();
        repaint();
    }

    private int caretLeft() {
        int p = caret - 1;
        if (p > 0 && Character.isLowSurrogate(doc.charAt(p)) && Character.isHighSurrogate(doc.charAt(p - 1))) {
            p--;
        }
        return p;
    }

    private int caretRight() {
        int p = caret + 1;
        if (p < doc.length() && Character.isHighSurrogate(doc.charAt(caret)) && Character.isLowSurrogate(doc.charAt(p))) {
            p++;
        }
        return p;
    }

    private int caretVertical(int delta) {
        int cl = doc.lineOfOffset(caret);
        int col = caret - doc.getLineStart(cl);
        int target = clampInt(cl + delta, 0, doc.getLineCount() - 1);
        int lineLen = doc.getLineEnd(target) - doc.getLineStart(target);
        return doc.getLineStart(target) + Math.min(col, lineLen);
    }

    private void lineHome(boolean extend) {
        moveCaret(doc.getLineStart(doc.lineOfOffset(caret)), extend);
    }

    private void lineEndKey(boolean extend) {
        moveCaret(doc.getLineEnd(doc.lineOfOffset(caret)), extend);
    }

    /// Selects the entire document.
    public void selectAll() {
        anchor = 0;
        caret = doc.length();
        resetBlink();
        pushInputState();
        repaint();
    }

    // ---- clipboard ----

    protected void copySelection() {
        if (hasSelection()) {
            Display.getInstance().copyToClipboard(doc.substring(getSelectionStart(), getSelectionEnd()));
        }
    }

    private void cutSelection() {
        if (hasSelection() && editable) {
            copySelection();
            replaceRange(getSelectionStart(), getSelectionEnd(), "", true);
        }
    }

    private void pasteClipboard() {
        if (!editable) {
            return;
        }
        Object data = Display.getInstance().getPasteDataFromClipboard();
        pasteClipboardData(data);
    }

    /// Inserts clipboard data. Rich editor subclasses may override this to preserve formatting.
    protected void pasteClipboardData(Object data) {
        if (data instanceof RichTextClipboardData) {
            insertText(((RichTextClipboardData) data).getPlainText());
            return;
        }
        if (data instanceof String) {
            insertText((String) data);
        }
    }

    // ---- document change plumbing ----

    private void onDocumentChanged() {
        composingStart = -1;
        composingEnd = -1;
        touchSelection = false;
        invalidateLayout();
        resetBlink();
        scrollCaretVisible();
        if (host != null) {
            host.editorChanged();
        }
        pushInputState();
        repaint();
    }

    private int maxScroll() {
        return Math.max(0, contentTotalHeight() - contentHeight());
    }

    private int cachedMaxLineWidth = -1;

    private int maxScrollX() {
        if (cachedMaxLineWidth < 0) {
            Font f = getEditorFont();
            int w = 0;
            int lc = doc.getLineCount();
            for (int i = 0; i < lc; i++) {
                int lw = lineContentOffsetX(i) + f.stringWidth(doc.getLineText(i));
                if (lw > w) {
                    w = lw;
                }
            }
            cachedMaxLineWidth = w;
        }
        return Math.max(0, cachedMaxLineWidth - contentWidth() + getEditorFont().charWidth('m') * 2);
    }

    /// Returns the extra left offset of a line's text within the content area (0 for plain text and
    /// code; the rich text editor overrides this for list bullets, indentation, block quotes and
    /// alignment so the caret and selection line up with the drawn text).
    protected int lineContentOffsetX(int line) {
        return 0;
    }

    /// Pixel width of the substring `text[from, to)`. `Font.substringWidth` takes an offset and a length,
    /// so this converts an end index to a length for the bidi geometry helpers.
    private static int subw(Font f, String text, int from, int to) {
        if (to <= from) {
            return 0;
        }
        return f.substringWidth(text, from, to - from);
    }

    /// Returns the visual pixel x (from the start of a line) of the caret before logical column `col`.
    /// For plain left-to-right text this is simply the substring width; for lines containing right-to-left
    /// or mixed (bidirectional) text it maps the logical column through the resolved visual order so the
    /// caret lines up with the reordered glyphs. The rich text editor overrides this to measure per styled
    /// run.
    protected int measureColumnX(int line, String text, int col, Font f) {
        if (BidiUtil.isTrivialLtr(text, isRTL())) {
            return f.substringWidth(text, 0, col);
        }
        int[][] runs = bidiRuns(text, f);
        for (int[] run : runs) {
            int lo = run[0];
            int hi = run[1];
            int lvl = run[2];
            int rx = run[3];
            if (col >= lo && col <= hi) {
                if ((lvl & 1) == 0) {
                    return rx + subw(f, text, lo, col);
                }
                return rx + subw(f, text, col, hi);
            }
        }
        return f.substringWidth(text, 0, Math.min(col, text.length()));
    }

    /// Returns the column nearest a given content-local x pixel on a line. Bidi aware: it locates the
    /// visual run containing the pixel and maps back to a logical column. The rich text editor overrides
    /// it to be styled-run aware.
    protected int columnAtX(int line, String text, int localX, Font f) {
        if (BidiUtil.isTrivialLtr(text, isRTL())) {
            int col = 0;
            while (col < text.length()) {
                int wFull = measureColumnX(line, text, col + 1, f);
                int wHalf = (measureColumnX(line, text, col, f) + wFull) / 2;
                if (localX < wHalf) {
                    break;
                }
                col++;
            }
            return col;
        }
        int[][] runs = bidiRuns(text, f);
        for (int r = 0; r < runs.length; r++) {
            int lo = runs[r][0];
            int hi = runs[r][1];
            int lvl = runs[r][2];
            int rx = runs[r][3];
            int rw = subw(f, text, lo, hi);
            boolean lastRun = r == runs.length - 1;
            if (localX < rx + rw || lastRun) {
                if ((lvl & 1) == 0) {
                    int col = lo;
                    while (col < hi) {
                        int wa = rx + subw(f, text, lo, col);
                        int wb = rx + subw(f, text, lo, col + 1);
                        if (localX < (wa + wb) / 2) {
                            break;
                        }
                        col++;
                    }
                    return col;
                }
                int col = hi;
                while (col > lo) {
                    int wa = rx + subw(f, text, col, hi);
                    int wb = rx + subw(f, text, col - 1, hi);
                    if (localX < (wa + wb) / 2) {
                        break;
                    }
                    col--;
                }
                return col;
            }
        }
        return text.length();
    }

    /// Builds the visual directional runs of a line for bidi layout. Each entry is
    /// `{logicalStart, logicalEnd, level, runX}` where `runX` is the pixel offset of the run's left edge
    /// from the line content start. Runs are returned in visual (left to right) order.
    private int[][] bidiRuns(String text, Font f) {
        int n = text.length();
        byte[] levels = BidiUtil.resolveLevels(text, isRTL());
        int[] v2l = BidiUtil.reorderVisual(levels);
        java.util.List<int[]> runs = new java.util.ArrayList<int[]>();
        int i = 0;
        while (i < n) {
            int lvl = levels[v2l[i]];
            int j = i;
            int lo = v2l[i];
            int hi = v2l[i];
            while (j < n && levels[v2l[j]] == lvl) {
                if (v2l[j] < lo) {
                    lo = v2l[j];
                }
                if (v2l[j] > hi) {
                    hi = v2l[j];
                }
                j++;
            }
            runs.add(new int[]{lo, hi + 1, lvl, 0});
            i = j;
        }
        int x = 0;
        int[][] out = runs.toArray(new int[runs.size()][]);
        for (int r = 0; r < out.length; r++) {
            out[r][3] = x;
            x += subw(f, text, out[r][0], out[r][1]);
        }
        return out;
    }

    /// Returns the visual selection segments for the logical range `[from, to)` on a line as a flat array
    /// of `{x, width, x, width, ...}` in content-local pixels. A bidi line can yield several disjoint
    /// segments; a plain line yields a single segment.
    protected int[] lineVisualSegments(int line, String text, int from, int to, Font f) {
        from = clampInt(from, 0, text.length());
        to = clampInt(to, 0, text.length());
        if (from >= to) {
            return new int[0];
        }
        if (BidiUtil.isTrivialLtr(text, isRTL())) {
            int xa = measureColumnX(line, text, from, f);
            int xb = measureColumnX(line, text, to, f);
            return new int[]{Math.min(xa, xb), Math.abs(xb - xa)};
        }
        int[][] runs = bidiRuns(text, f);
        java.util.List<int[]> segs = new java.util.ArrayList<int[]>();
        for (int[] run : runs) {
            int lo = run[0];
            int hi = run[1];
            int lvl = run[2];
            int rx = run[3];
            int a = Math.max(from, lo);
            int b = Math.min(to, hi);
            if (a >= b) {
                continue;
            }
            int xl;
            int xr;
            if ((lvl & 1) == 0) {
                xl = rx + subw(f, text, lo, a);
                xr = rx + subw(f, text, lo, b);
            } else {
                xl = rx + subw(f, text, b, hi);
                xr = rx + subw(f, text, a, hi);
            }
            segs.add(new int[]{Math.min(xl, xr), Math.abs(xr - xl)});
        }
        int[] flat = new int[segs.size() * 2];
        for (int k = 0; k < segs.size(); k++) {
            flat[k * 2] = segs.get(k)[0];
            flat[k * 2 + 1] = segs.get(k)[1];
        }
        return flat;
    }

    private void scrollCaretVisible() {
        if (contentHeight() <= 0) {
            // The component has not been laid out yet (height is 0, so contentHeight() is
            // negative once padding is subtracted). Scrolling to the caret here would push
            // scrollY to roughly one line, hiding the first line under the title bar once the
            // real bounds arrive. Defer until laidOut() runs with a real viewport.
            return;
        }
        int cl = doc.lineOfOffset(caret);
        int top = lineTopContent(cl);
        int bottom = top + lineHeightAt(cl);
        if (top - scrollY < 0) {
            scrollY = top;
        } else if (bottom - scrollY > contentHeight()) {
            scrollY = bottom - contentHeight();
        }
        scrollY = clampInt(scrollY, 0, maxScroll());

        int caretX = caretContentX();
        int viewW = contentWidth();
        int margin = getEditorFont().charWidth('m') * 2;
        if (caretX - scrollX < margin) {
            scrollX = Math.max(0, caretX - margin);
        } else if (caretX - scrollX > viewW - margin) {
            scrollX = caretX - viewW + margin;
        }
        int maxX = Math.max(0, caretLineWidth() - viewW + margin);
        scrollX = clampInt(scrollX, 0, maxX);
    }

    private int caretContentX() {
        int cl = doc.lineOfOffset(caret);
        int col = caret - doc.getLineStart(cl);
        String line = doc.getLineText(cl);
        if (col > line.length()) {
            col = line.length();
        }
        return lineContentOffsetX(cl) + measureColumnX(cl, line, col, getEditorFont());
    }

    private int caretLineWidth() {
        int cl = doc.lineOfOffset(caret);
        return lineContentOffsetX(cl) + getEditorFont().stringWidth(doc.getLineText(cl));
    }

    private void resetBlink() {
        caretOn = true;
        lastBlink = System.currentTimeMillis();
    }

    @Override
    protected void laidOut() {
        super.laidOut();
        // Now that the real viewport height is known, apply any scroll-to-caret that was
        // deferred while the component was unlaid-out (see scrollCaretVisible), and re-clamp
        // a stale scroll offset into range.
        scrollY = clampInt(scrollY, 0, maxScroll());
        scrollCaretVisible();
    }

    // ---- focus + input source lifecycle ----

    @Override
    protected void focusGained() {
        super.focusGained();
        startInput();
        if (!animRegistered && getComponentForm() != null) {
            getComponentForm().registerAnimated(this);
            animRegistered = true;
        }
        resetBlink();
        repaint();
    }

    @Override
    protected void focusLost() {
        super.focusLost();
        stopInput();
        if (animRegistered && getComponentForm() != null) {
            getComponentForm().deregisterAnimated(this);
            animRegistered = false;
        }
        repaint();
    }

    private void startInput() {
        if (host != null && inputHandle == null && host.isTextInputSupported()) {
            inputHandle = host.startTextInput(this, getConfig());
            inputActive = inputHandle != null;
        }
    }

    private void stopInput() {
        if (host != null && inputHandle != null) {
            host.stopTextInput(inputHandle);
        }
        inputHandle = null;
        inputActive = false;
    }

    private void pushInputState() {
        if (host != null && inputActive) {
            host.updateTextInputState(inputHandle, getEditingState());
        }
    }

    @Override
    public boolean animate() {
        boolean sup = super.animate();
        if (hasFocus()) {
            long now = System.currentTimeMillis();
            if (now - lastBlink >= 500) {
                caretOn = !caretOn;
                lastBlink = now;
                return true;
            }
        }
        return sup;
    }

    // ---- pointer ----

    private long lastClickTime;
    private int lastClickOffset = -1;
    private int clickCount;
    private boolean multiClickSelecting;
    private boolean touchPointerGesture;
    private boolean touchScrolling;
    private int pointerPressX;
    private int pointerPressY;
    private int pointerPressScrollX;
    private int pointerPressScrollY;
    private int pointerPressCaret;
    private int pointerPressAnchor;

    @Override
    public void pointerPressed(int x, int y) {
        requestFocus();
        touchPointerGesture = !Display.getInstance().isDesktop();
        touchScrolling = false;
        pointerPressX = x;
        pointerPressY = y;
        pointerPressScrollX = scrollX;
        pointerPressScrollY = scrollY;
        pointerPressCaret = caret;
        pointerPressAnchor = anchor;
        if (handleTouchSelectionPress(x, y)) {
            return;
        }
        int off = offsetAtPoint(x, y);
        long now = System.currentTimeMillis();
        if (now - lastClickTime < 400 && Math.abs(off - lastClickOffset) <= 1) {
            clickCount++;
        } else {
            clickCount = 1;
        }
        lastClickTime = now;
        lastClickOffset = off;
        if (clickCount == 2) {
            multiClickSelecting = true;
            selectWordAt(off);
            return;
        }
        if (clickCount >= 3) {
            multiClickSelecting = true;
            selectLineAt(off);
            return;
        }
        multiClickSelecting = false;
        caret = off;
        anchor = off;
        resetBlink();
        pushInputState();
        repaint();
    }

    @Override
    public void pointerDragged(int x, int y) {
        if (draggingHandle != 0) {
            caret = offsetAtPoint(x, y);
            loupeActive = true;
            loupeOffset = caret;
            resetBlink();
            scrollCaretVisible();
            repaint();
            return;
        }
        if (loupeActive) {
            // long-press caret placement drag: keep the magnifier following the finger
            caret = offsetAtPoint(x, y);
            loupeOffset = caret;
            resetBlink();
            scrollCaretVisible();
            repaint();
            return;
        }
        if (touchPointerGesture && !multiClickSelecting) {
            int dx = x - pointerPressX;
            int dy = y - pointerPressY;
            int threshold = Math.max(4, getEditorFont().getHeight() / 3);
            if (touchScrolling || Math.abs(dx) > threshold || Math.abs(dy) > threshold) {
                touchScrolling = true;
                caret = pointerPressCaret;
                anchor = pointerPressAnchor;
                scrollX = clampInt(pointerPressScrollX - dx, 0, maxScrollX());
                scrollY = clampInt(pointerPressScrollY - dy, 0, maxScroll());
                clickCount = 0;
                lastClickOffset = -1;
                repaint();
            }
            return;
        }
        if (multiClickSelecting) {
            return;
        }
        caret = offsetAtPoint(x, y);
        resetBlink();
        scrollCaretVisible();
        repaint();
    }

    @Override
    public void pointerReleased(int x, int y) {
        boolean wasLoupe = loupeActive;
        loupeActive = false;
        if (touchScrolling) {
            touchScrolling = false;
            pushInputState();
            repaint();
            return;
        }
        if (draggingHandle != 0) {
            caret = offsetAtPoint(x, y);
            draggingHandle = 0;
            touchSelection = hasSelection();
            pushInputState();
            repaint();
            return;
        }
        if (wasLoupe) {
            // finished a long-press caret placement / word selection; keep the current selection or caret
            pushInputState();
            repaint();
            return;
        }
        if (multiClickSelecting) {
            multiClickSelecting = false;
            return;
        }
        caret = offsetAtPoint(x, y);
        if (anchor == caret) {
            anchor = -1;
        }
        pushInputState();
        repaint();
    }

    // ---- raw keyboard (reference path when no platform input source) ----

    private boolean isPrimaryModifierDown() {
        Display d = Display.getInstance();
        return d.isMetaKeyDown() || d.isControlKeyDown();
    }

    private boolean handleShortcut(int keyCode) {
        Display d = Display.getInstance();
        if (!(d.isMetaKeyDown() || d.isControlKeyDown()) || d.isAltKeyDown()) {
            return false;
        }
        char ch = Character.toLowerCase((char) keyCode);
        if (ch == 'c') {
            copySelection();
            return true;
        }
        if (ch == 'x') {
            cutSelection();
            return true;
        }
        if (ch == 'v') {
            pasteClipboard();
            return true;
        }
        if (ch == 'a') {
            selectAll();
            return true;
        }
        if (ch == 'z') {
            if (d.isShiftKeyDown()) {
                performRedo();
            } else {
                performUndo();
            }
            return true;
        }
        if (ch == 'y') {
            performRedo();
            return true;
        }
        return false;
    }

    private boolean handleNavKey(int keyCode) {
        Display d = Display.getInstance();
        boolean shift = d.isShiftKeyDown();
        boolean word = d.isAltKeyDown();
        boolean lineJump = d.isMetaKeyDown() || d.isControlKeyDown();
        int action = d.getGameAction(keyCode);
        if (action == Display.GAME_LEFT) {
            if (lineJump) {
                lineHome(shift);
            } else if (word) {
                moveCaret(wordLeft(), shift);
            } else {
                moveCaret(caretLeft(), shift);
            }
            return true;
        }
        if (action == Display.GAME_RIGHT) {
            if (lineJump) {
                lineEndKey(shift);
            } else if (word) {
                moveCaret(wordRight(), shift);
            } else {
                moveCaret(caretRight(), shift);
            }
            return true;
        }
        if (action == Display.GAME_UP) {
            if (lineJump) {
                moveCaret(0, shift);
            } else {
                moveCaret(caretVertical(-1), shift);
            }
            return true;
        }
        if (action == Display.GAME_DOWN) {
            if (lineJump) {
                moveCaret(doc.length(), shift);
            } else {
                moveCaret(caretVertical(1), shift);
            }
            return true;
        }
        if (keyCode == 8) {
            deleteBackward();
            return true;
        }
        if (keyCode == 127) {
            deleteForward();
            return true;
        }
        return false;
    }

    private static boolean isWordChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }

    private int wordLeft() {
        int p = caret;
        while (p > 0 && !isWordChar(doc.charAt(p - 1))) {
            p--;
        }
        while (p > 0 && isWordChar(doc.charAt(p - 1))) {
            p--;
        }
        return p;
    }

    private int wordRight() {
        int p = caret;
        int n = doc.length();
        while (p < n && !isWordChar(doc.charAt(p))) {
            p++;
        }
        while (p < n && isWordChar(doc.charAt(p))) {
            p++;
        }
        return p;
    }

    private void selectWordAt(int offset) {
        int s = offset;
        int e = offset;
        while (s > 0 && isWordChar(doc.charAt(s - 1))) {
            s--;
        }
        while (e < doc.length() && isWordChar(doc.charAt(e))) {
            e++;
        }
        anchor = s;
        caret = e;
        resetBlink();
        pushInputState();
        repaint();
    }

    private void selectLineAt(int offset) {
        int line = doc.lineOfOffset(offset);
        anchor = doc.getLineStart(line);
        caret = doc.getLineEnd(line);
        resetBlink();
        pushInputState();
        repaint();
    }

    @Override
    public void keyPressed(int keyCode) {
        if (!inputActive && editable) {
            if (handleShortcut(keyCode)) {
                return;
            }
            if (handleNavKey(keyCode)) {
                return;
            }
        }
        super.keyPressed(keyCode);
    }

    @Override
    public void keyRepeated(int keyCode) {
        if (!inputActive && editable && !isPrimaryModifierDown() && handleNavKey(keyCode)) {
            return;
        }
        super.keyRepeated(keyCode);
    }

    @Override
    public void keyReleased(int keyCode) {
        if (!inputActive && editable) {
            if (isPrimaryModifierDown() || Display.getInstance().isAltKeyDown()) {
                // modifier combos are handled in keyPressed; never type the raw character
                return;
            }
            if (keyCode == 10 || keyCode == 13) {
                insertText("\n");
                return;
            }
            if (keyCode == 9) {
                insertText("\t");
                return;
            }
            int action = Display.getInstance().getGameAction(keyCode);
            if (action == 0 && keyCode >= 32 && keyCode != 127) {
                insertText(String.valueOf((char) keyCode));
                return;
            }
        }
        super.keyReleased(keyCode);
    }

    // ---- TextInputClient ----

    @Override
    public void commitText(String text) {
        if (!editable) {
            return;
        }
        if (composingStart >= 0) {
            replaceRange(composingStart, composingEnd, text, true);
            return;
        }
        if (handleTypedText(text)) {
            return;
        }
        replaceRange(getSelectionStart(), getSelectionEnd(), text, true);
    }

    @Override
    public void setComposingText(String text, int relativeCaret) {
        if (!editable) {
            return;
        }
        String value = EditorDocument.normalizeText(text == null ? "" : text);
        int start;
        int end;
        if (composingStart >= 0) {
            start = composingStart;
            end = composingEnd;
        } else {
            start = getSelectionStart();
            end = getSelectionEnd();
        }
        start = doc.clamp(start);
        end = doc.clamp(end);
        String removed = doc.substring(start, end);
        doc.delete(start, end);
        doc.insert(start, value);
        composingStart = start;
        composingEnd = start + value.length();
        caret = composingEnd;
        anchor = -1;
        // composition is not recorded until finalized; keep undo aware of the net removal only once
        undo.breakRun();
        documentEdited(start);
        documentReplaced(start, removed, value);
        if (removed.length() > 0 || value.length() > 0) {
            resetBlink();
            scrollCaretVisible();
            if (host != null) {
                host.editorChanged();
            }
            pushInputState();
            repaint();
        }
    }

    @Override
    public void finishComposing() {
        composingStart = -1;
        composingEnd = -1;
    }

    @Override
    public void deleteSurroundingText(int before, int after) {
        if (!editable) {
            return;
        }
        int s = doc.clamp(caret - Math.max(0, before));
        int e = doc.clamp(caret + Math.max(0, after));
        replaceRange(s, e, "", true);
    }

    @Override
    public void onKeyCommand(int command, int modifiers) {
        boolean extend = (modifiers & MOD_SHIFT) != 0;
        switch (command) {
            case KEY_LEFT:
                moveCaret(caretLeft(), extend);
                break;
            case KEY_RIGHT:
                moveCaret(caretRight(), extend);
                break;
            case KEY_UP:
                moveCaret(caretVertical(-1), extend);
                break;
            case KEY_DOWN:
                moveCaret(caretVertical(1), extend);
                break;
            case KEY_HOME:
                lineHome(extend);
                break;
            case KEY_END:
                lineEndKey(extend);
                break;
            case KEY_PAGE_UP:
                moveCaret(caretVertical(-Math.max(1, contentHeight() / getLineHeight())), extend);
                break;
            case KEY_PAGE_DOWN:
                moveCaret(caretVertical(Math.max(1, contentHeight() / getLineHeight())), extend);
                break;
            case KEY_BACKSPACE:
                deleteBackward();
                break;
            case KEY_DELETE:
                deleteForward();
                break;
            case KEY_COPY:
                copySelection();
                break;
            case KEY_CUT:
                cutSelection();
                break;
            case KEY_PASTE:
                pasteClipboard();
                break;
            case KEY_SELECT_ALL:
                selectAll();
                break;
            case KEY_UNDO:
                performUndo();
                break;
            case KEY_REDO:
                performRedo();
                break;
            default:
                break;
        }
    }

    /// Undoes the last edit.
    public void performUndo() {
        UndoManager.Edit edit = undo.undoEdit(doc);
        if (edit != null) {
            caret = doc.clamp(edit.start + edit.removed.length());
            anchor = -1;
            documentEdited(edit.start);
            documentReplaced(edit.start, edit.inserted, edit.removed);
            restoreDocumentState(edit.beforeState);
            onDocumentChanged();
        }
    }

    /// Redoes the last undone edit.
    public void performRedo() {
        UndoManager.Edit edit = undo.redoEdit(doc);
        if (edit != null) {
            caret = doc.clamp(edit.start + edit.inserted.length());
            anchor = -1;
            documentEdited(edit.start);
            documentReplaced(edit.start, edit.removed, edit.inserted);
            restoreDocumentState(edit.afterState);
            onDocumentChanged();
        }
    }

    @Override
    public void onEditorAction(int action) {
        if (action == TextInputConfig.ACTION_DEFAULT && editable) {
            insertText("\n");
        }
    }

    @Override
    public TextInputState getEditingState() {
        return new TextInputState(doc.getText(), getSelectionStart(), getSelectionEnd(), composingStart, composingEnd);
    }

    @Override
    public int[] getCaretRect() {
        return rectForOffset(caret);
    }

    @Override
    public int[] rectForOffset(int offset) {
        offset = doc.clamp(offset);
        Font f = getEditorFont();
        int cl = doc.lineOfOffset(offset);
        int col = offset - doc.getLineStart(cl);
        String line = doc.getLineText(cl);
        if (col > line.length()) {
            col = line.length();
        }
        int localX = getContentLeftInset() + lineContentOffsetX(cl) + measureColumnX(cl, line, col, f) - scrollX;
        int localY = getStyle().getPaddingTop() + lineTopContent(cl) - scrollY;
        return new int[]{getAbsoluteX() + localX, getAbsoluteY() + localY, Math.max(1, f.getHeight() / 16), lineHeightAt(cl)};
    }

    @Override
    public int getTextLength() {
        return doc.length();
    }

    @Override
    public String getTextRange(int start, int end) {
        start = doc.clamp(start);
        end = doc.clamp(end);
        if (start > end) {
            int t = start;
            start = end;
            end = t;
        }
        return doc.substring(start, end);
    }

    @Override
    public int[] selectionRects(int start, int end) {
        start = doc.clamp(start);
        end = doc.clamp(end);
        if (start > end) {
            int t = start;
            start = end;
            end = t;
        }
        Font f = getEditorFont();
        int firstLine = doc.lineOfOffset(start);
        int lastLine = doc.lineOfOffset(end);
        java.util.List<int[]> rects = new java.util.ArrayList<int[]>();
        for (int ln = firstLine; ln <= lastLine; ln++) {
            int ls = doc.getLineStart(ln);
            int le = doc.getLineEnd(ln);
            String text = doc.getLineText(ln);
            int a = clampInt(Math.max(start, ls) - ls, 0, text.length());
            int b = clampInt(Math.min(end, le) - ls, 0, text.length());
            int lx = getContentLeftInset() + lineContentOffsetX(ln) - scrollX;
            int y = getStyle().getPaddingTop() + lineTopContent(ln) - scrollY;
            // A bidi line can produce several disjoint visual rectangles for one logical range.
            int[] segs = lineVisualSegments(ln, text, a, b, f);
            boolean crossesEol = ln < lastLine && end > le;
            if (segs.length == 0 && crossesEol) {
                int ex = lx + measureColumnX(ln, text, text.length(), f);
                rects.add(new int[]{getAbsoluteX() + ex, getAbsoluteY() + y, f.charWidth(' '), lineHeightAt(ln)});
            }
            for (int si = 0; si < segs.length; si += 2) {
                int rx = lx + segs[si];
                int rw = segs[si + 1];
                // extend the last segment to cover the trailing newline when the selection wraps
                if (crossesEol && si == segs.length - 2) {
                    rw += f.charWidth(' ');
                }
                rects.add(new int[]{getAbsoluteX() + rx, getAbsoluteY() + y, Math.max(1, rw), lineHeightAt(ln)});
            }
        }
        int[] out = new int[rects.size() * 4];
        for (int i = 0; i < rects.size(); i++) {
            int[] r = rects.get(i);
            out[i * 4] = r[0];
            out[i * 4 + 1] = r[1];
            out[i * 4 + 2] = r[2];
            out[i * 4 + 3] = r[3];
        }
        return out;
    }

    @Override
    public void replaceRange(int start, int end, String text) {
        replaceRange(start, end, text, true);
    }

    @Override
    public void setSelectionRange(int start, int end) {
        start = doc.clamp(start);
        end = doc.clamp(end);
        anchor = start == end ? -1 : start;
        caret = end;
        resetBlink();
        scrollCaretVisible();
        repaint();
    }

    @Override
    public TextInputConfig getConfig() {
        TextInputConfig cfg = new TextInputConfig();
        if (codeMode) {
            cfg.setAutoCorrect(false).setAutoCapitalize(false);
        }
        cfg.setMultiline(true);
        return cfg;
    }

    @Override
    public void inputFocusGained() {
    }

    @Override
    public void inputFocusLost() {
    }

    // ---- preferred size ----

    @Override
    protected Dimension calcPreferredSize() {
        Font f = getEditorFont();
        int w = 20 * f.charWidth('m') + getContentLeftInset() + getStyle().getPaddingRightNoRTL();
        int h = 6 * getLineHeight() + getStyle().getPaddingTop() + getStyle().getPaddingBottom();
        return new Dimension(w, h);
    }
}
