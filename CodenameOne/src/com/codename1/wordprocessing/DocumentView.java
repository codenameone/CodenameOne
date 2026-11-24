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
package com.codename1.wordprocessing;

import com.codename1.ui.ClipboardContent;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.spellcheck.ProofIssue;
import com.codename1.spellcheck.Proofreader;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.ScrollSurface;
import com.codename1.ui.TextInputClient;
import com.codename1.ui.TextInputConfig;
import com.codename1.ui.TextInputGeometry;
import com.codename1.ui.TextInputState;
import com.codename1.ui.editor.TouchSelection;
import com.codename1.ui.editor.WordCharacters;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.util.EventDispatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Shows a document as pages and edits it, the way `DataGrid` shows and edits a `Sheet`.
///
/// ```java
/// DocumentModel model = new DocumentModel(new Document());
/// model.insertBlock(0, new Paragraph(model.newId('p'), RichText.of("Quarterly review")));
/// DocumentView view = new DocumentView(model);
/// Form form = new Form("Document", new BorderLayout());
/// form.add(BorderLayout.CENTER, view);
/// form.show();
/// ```
///
/// The view subscribes to its model while it is on a form and lays the document out again on
/// every change, so anything that edits the model -- the view itself, a toolbar, an undo button
/// -- is shown at once. Pages are laid out by `DocumentLayout` with the view's `Measurer`
/// (`ScreenMeasurer` by default, the fonts the view draws in). Set a `PdfMeasurer` to break
/// pages exactly where the PDF export does, at the cost of spacing set by the PDF's fonts.
///
/// A tap places the caret, a drag or a double tap selects, and typing, Backspace, Delete and
/// Enter edit through the model, which gives undo for free. Pinch or Control and the wheel
/// zooms; by default a page is fitted to the view's width when it would not fit.
public class DocumentView extends ScrollSurface implements TextInputClient,
        TextInputClient.CompositionCancellation, TextInputGeometry.Provider {
    private static final int DESK = 0xe3e6ea;
    /// The desk in dark mode. The pages stay white, as a word processor keeps them, so the
    /// document looks the same printed as on screen.
    private static final int DESK_DARK = 0x2b2e33;
    private static final int PAGE_EDGE = 0xc5cad1;
    private static final int SELECTION = 0x3d8bfd;
    private static final int CARET = 0x1f2933;
    private static final int INSERT_COLOR = 0x1f6feb;
    private static final int DELETE_COLOR = 0xc0392b;

    private DocumentModel model;
    private final DocumentListener listener = new DocumentListener() {
        @Override
        public void documentChanged(DocumentModel m, Document before, Document after) {
            relayout();
        }
    };
    private boolean listening;
    private Measurer measurer = new ScreenMeasurer();
    private DocumentLayout layout;
    private float zoom = 1;
    private boolean fitWidth = true;
    private boolean editable = true;
    private TextPosition caret;
    private TextPosition anchor;
    private Object inputHandle;
    private boolean touchEdit;
    private final TouchSelection touchChrome = new TouchSelection(new TouchSelection.Host() {
        @Override
        public int[] edgeRect(boolean start) {
            TextPosition[] sel = getSelection();
            TextPosition at = sel == null ? caret : sel[start ? 0 : 1];
            float[] r = at == null ? null : getLayout().caretRect(at);
            if (r == null) {
                return new int[] {0, 0, 1};
            }
            float scale = getScale();
            return new int[] {pageLeft((int) r[0]) - getOffsetX() + Math.round(r[1] * scale),
                pageTop((int) r[0]) - getOffsetY() + Math.round(r[2] * scale),
                Math.max(1, Math.round(r[3] * scale))};
        }

        @Override
        public boolean canEdit() {
            return editable;
        }

        @Override
        public void toolbarAction(int action) {
            if (action == TouchSelection.COPY) {
                copySelection();
            } else if (action == TouchSelection.CUT) {
                cutSelection();
            } else if (action == TouchSelection.PASTE) {
                pasteClipboard();
            } else {
                selectAll();
            }
        }

        @Override
        public Font chromeFont() {
            return getStyle().getFont();
        }
    });
    private boolean caretOn = true;
    private long lastBlink;
    private final ScreenMeasurer screenFonts = new ScreenMeasurer();
    /// While painting: whether the item being painted ends its line.
    private boolean paintLastOnLine = true;
    private final Map<String, Image> images = new HashMap<String, Image>();
    private final EventDispatcher caretListeners = new EventDispatcher();
    private int gapPx;
    private Runnable viewportListener;
    private Proofreader proofreader;
    private final Map<String, String> paintTexts = new HashMap<String, String>();
    private static final int SPELLING_COLOR = 0xe03131;
    private static final int GRAMMAR_COLOR = 0x1c7ed6;
    private static final int COMMENT_COLOR = 0xffe8a3;
    private static final int RESOLVED_COMMENT_COLOR = 0xf1f3f5;
    private List<String> paintOrder;

    /// A view of an empty document.
    public DocumentView() {
        this(new DocumentModel());
    }

    /// A view of a document, in a model of its own.
    ///
    /// #### Parameters
    ///
    /// - `doc`: the document
    public DocumentView(Document doc) {
        this(new DocumentModel(doc));
    }

    /// A view of a model.
    ///
    /// #### Parameters
    ///
    /// - `model`: the model
    public DocumentView(DocumentModel model) {
        setUIID("DocumentView");
        setFocusable(true);
        setModel(model);
    }

    /// The model shown.
    ///
    /// #### Returns
    ///
    /// the model
    public DocumentModel getModel() {
        return model;
    }

    /// Shows another model.
    ///
    /// #### Parameters
    ///
    /// - `value`: the model
    public final void setModel(DocumentModel value) {
        if (value == null) {
            throw new IllegalArgumentException("Model is required");
        }
        unlisten();
        model = value;
        caret = null;
        anchor = null;
        if (isInitialized()) {
            listen();
        }
        relayout();
    }

    /// The document shown; a shortcut for `getModel().getDocument()`.
    ///
    /// #### Returns
    ///
    /// the document
    public Document getDocument() {
        return model.getDocument();
    }

    /// Replaces the document, forgetting the undo history; a shortcut for
    /// `getModel().reset(doc)`.
    ///
    /// #### Parameters
    ///
    /// - `doc`: the document
    public void setDocument(Document doc) {
        model.reset(doc);
    }

    /// Sets the measurer pages are laid out with, for instance a `PdfMeasurer` with TrueType
    /// fonts registered for a script the standard fonts lack.
    ///
    /// #### Parameters
    ///
    /// - `value`: the measurer
    public void setMeasurer(Measurer value) {
        if (value == null) {
            throw new IllegalArgumentException("Measurer is required");
        }
        measurer = value;
        relayout();
    }

    /// The measurer pages are laid out with.
    ///
    /// #### Returns
    ///
    /// the measurer
    public Measurer getMeasurer() {
        return measurer;
    }

    /// The current layout.
    ///
    /// #### Returns
    ///
    /// the layout of the model's document
    public DocumentLayout getLayout() {
        if (layout == null) {
            layout = new DocumentLayout(model.getDocument(), measurer);
        }
        return layout;
    }

    /// Whether the user may edit the document.
    ///
    /// #### Returns
    ///
    /// true by default
    @Override
    public boolean isEditable() {
        return editable;
    }

    /// Lets the user edit the document, or only read and select it.
    ///
    /// #### Parameters
    ///
    /// - `value`: true to allow editing
    public void setEditable(boolean value) {
        editable = value;
        if (!value) {
            stopInput();
        }
    }

    /// The zoom factor on top of the fitted or natural size.
    ///
    /// #### Returns
    ///
    /// the factor, 1 by default
    public float getZoom() {
        return zoom;
    }

    /// Sets the zoom factor on top of the fitted or natural size.
    ///
    /// #### Parameters
    ///
    /// - `value`: between 0.25 and 6
    public void setZoom(float value) {
        zoom = Math.max(0.25f, Math.min(6, value));
        setShouldCalcPreferredSize(true);
        repaint();
        viewportMoved();
    }

    /// Whether a page wider than the view is shrunk to fit its width.
    ///
    /// #### Returns
    ///
    /// true by default
    public boolean isFitWidth() {
        return fitWidth;
    }

    /// Shrinks pages wider than the view to its width, or shows them at their natural size.
    ///
    /// #### Parameters
    ///
    /// - `value`: true to fit
    public void setFitWidth(boolean value) {
        fitWidth = value;
        repaint();
    }

    /// The caret, or null when there is none.
    ///
    /// #### Returns
    ///
    /// the caret position
    public TextPosition getCaret() {
        return caret;
    }

    /// Places the caret and clears the selection.
    ///
    /// #### Parameters
    ///
    /// - `value`: the position, or null to remove the caret
    public void setCaret(TextPosition value) {
        setSelection(value, value);
    }

    /// Selects the text between two positions; equal positions place the caret.
    ///
    /// #### Parameters
    ///
    /// - `from`: where the selection is anchored
    ///
    /// - `to`: where the caret is
    public void setSelection(TextPosition from, TextPosition to) {
        if (from == null || from.equals(to)) {
            touchChrome.setShown(false);
        }
        anchor = from;
        caret = to;
        caretOn = true;
        lastBlink = System.currentTimeMillis();
        pushInputState();
        caretListeners.fireActionEvent(new ActionEvent(this));
        repaint();
    }

    /// The selection, earlier end first, or null when nothing is selected.
    ///
    /// #### Returns
    ///
    /// the two ends, or null
    public TextPosition[] getSelection() {
        if (caret == null || anchor == null || caret.equals(anchor)) {
            return null;
        }
        return model.order(anchor, caret);
    }

    /// Adds a listener told when the caret or the selection moves, for a toolbar showing the
    /// style at the caret.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addCaretListener(ActionListener l) {
        caretListeners.addListener(l);
    }

    /// Removes a caret listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeCaretListener(ActionListener l) {
        caretListeners.removeListener(l);
    }

    // ------------------------------------------------------------------ lifecycle

    private void listen() {
        if (!listening) {
            model.addDocumentListener(listener);
            listening = true;
        }
    }

    private void unlisten() {
        if (listening && model != null) {
            model.removeDocumentListener(listener);
        }
        listening = false;
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        boolean was = listening;
        listen();
        if (!was) {
            // it may have changed while nobody listened
            relayout();
        }
    }

    @Override
    protected void deinitialize() {
        stopInput();
        unlisten();
        super.deinitialize();
    }

    private void relayout() {
        layout = null;
        if (caret != null && model.findParagraph(caret.paragraphId) == null) {
            caret = null;
            anchor = null;
        }
        if (anchor != null && model.findParagraph(anchor.paragraphId) == null) {
            anchor = caret;
        }
        setShouldCalcPreferredSize(true);
        pushInputState();
        repaint();
        viewportMoved();
    }

    // ------------------------------------------------------------------ geometry

    /// Pixels per point at the current size.
    ///
    /// #### Returns
    ///
    /// the scale
    public float getScale() {
        float natural = Display.getInstance().convertToPixels(254f) / 720f;
        float s = natural;
        int w = getWidth();
        if (fitWidth && w > 0) {
            float fit = (w - 2f * gap()) / widestPage();
            if (fit > 0 && fit < natural) {
                s = fit;
            }
        }
        return Math.max(0.05f, s * zoom);
    }

    private int gap() {
        if (gapPx == 0) {
            gapPx = Math.max(4, Display.getInstance().convertToPixels(3f));
        }
        return gapPx;
    }

    /// The widest page, in points: sections may turn pages or change their size.
    private float widestPage() {
        DocumentLayout l = getLayout();
        float w = 1;
        for (int p = 0; p < l.getPageCount(); p++) {
            w = Math.max(w, l.getPage(p).getWidth());
        }
        return w;
    }

    private int pageWidthPx() {
        return Math.round(widestPage() * getScale());
    }

    private int pageWidthPx(int page) {
        return Math.round(getLayout().getPage(page).getWidth() * getScale());
    }

    private int pageHeightPx(int page) {
        return Math.round(getLayout().getPage(page).getHeight() * getScale());
    }

    /// Told when what the view shows moves: a scroll, a zoom or a new layout.
    void setViewportListener(Runnable l) {
        viewportListener = l;
    }

    private void viewportMoved() {
        if (viewportListener != null) {
            viewportListener.run();
        }
    }

    @Override
    protected void scrollChanged() {
        super.scrollChanged();
        viewportMoved();
    }

    /// Where a laid out item is, in the view's own coordinates at the current scroll.
    ///
    /// #### Parameters
    ///
    /// - `page`: the item's page
    ///
    /// - `item`: the item
    ///
    /// #### Returns
    ///
    /// `{x, y, width, height}` in pixels
    public int[] itemBounds(int page, LayoutItem item) {
        float scale = getScale();
        int x = pageLeft(page) + Math.round(item.x * scale) - getOffsetX();
        int y = pageTop(page) + Math.round(item.y * scale) - getOffsetY();
        return new int[] {x, y, Math.max(1, Math.round(item.width * scale)),
            Math.max(1, Math.round(item.height * scale))};
    }

    private int pageLeft(int page) {
        return Math.max(gap(), (getContentWidth() - pageWidthPx(page)) / 2);
    }

    private int pageTop(int page) {
        int y = gap();
        for (int p = 0; p < page; p++) {
            y += pageHeightPx(p) + gap();
        }
        return y;
    }

    @Override
    protected void layoutSurface(int width, int height) {
        // no headers
    }

    @Override
    protected int getContentWidth() {
        return Math.max(getWidth(), pageWidthPx() + 2 * gap());
    }

    @Override
    protected int getContentHeight() {
        return pageTop(getLayout().getPageCount());
    }

    @Override
    protected boolean zoomBy(float factor, int x, int y) {
        setZoom(zoom * factor);
        return true;
    }

    // ------------------------------------------------------------------ painting

    /// Underlines spelling and grammar problems with a wavy line, red for spelling and blue
    /// for grammar.
    ///
    /// #### Parameters
    ///
    /// - `p`: the proofreader, or null to stop
    public void setProofreader(Proofreader p) {
        proofreader = p;
        repaint();
    }

    /// The proofreader underlining problems.
    ///
    /// #### Returns
    ///
    /// the proofreader, or null
    public Proofreader getProofreader() {
        return proofreader;
    }

    /// The problem under a text position, with spelling suggestions, for a menu of fixes.
    ///
    /// #### Parameters
    ///
    /// - `at`: the position
    ///
    /// - `max`: the most suggestions
    ///
    /// #### Returns
    ///
    /// the issue, or null when there is none or no proofreader
    public ProofIssue proofIssueAt(TextPosition at, int max) {
        Paragraph p = at == null || proofreader == null ? null
                : model.findParagraph(at.paragraphId);
        return p == null ? null : proofreader.issueAt(p.text.plainText(), at.offset, max);
    }

    /// Replaces the range of a problem with one of its suggestions, as one undoable edit.
    ///
    /// #### Parameters
    ///
    /// - `paragraphId`: the paragraph the issue was found in
    ///
    /// - `issue`: the issue
    ///
    /// - `replacement`: the text to put in its place
    public void applySuggestion(String paragraphId, ProofIssue issue, String replacement) {
        TextPosition a = new TextPosition(paragraphId, issue.start);
        model.beginGroup();
        try {
            TextPosition end = model.delete(a, new TextPosition(paragraphId, issue.end));
            setCaret(model.insertText(end, replacement, null));
        } finally {
            model.endGroup();
        }
    }

    @Override
    protected void paintContent(Graphics g, int x, int y) {
        paintTexts.clear();
        paintOrder = null;
        if (!model.getDocument().comments.isEmpty()) {
            paintOrder = new ArrayList<String>();
            DocumentModel.collectIds(model.getDocument().blocks, paintOrder);
        }
        // null when the platform cannot tell
        g.setColor(Boolean.TRUE.equals(Display.getInstance().isDarkMode()) ? DESK_DARK : DESK);
        // the whole viewport, not just the pages' extent: a short document leaves the rest of
        // it otherwise showing whatever is behind the view
        g.fillRect(x, y, Math.max(getContentWidth(), getOffsetX() + getWidth()),
                Math.max(getContentHeight(), getOffsetY() + getHeight()));
        DocumentLayout l = getLayout();
        float scale = getScale();
        int clipY = g.getClipY();
        int clipH = g.getClipHeight();
        TextPosition[] sel = getSelection();
        float[] caretRect = caret != null && sel == null && caretOn && editable && hasFocus()
                ? l.caretRect(caret) : null;
        for (int p = 0; p < l.getPageCount(); p++) {
            int top = y + pageTop(p);
            int pw = pageWidthPx(p);
            int ph = pageHeightPx(p);
            int left = x + pageLeft(p);
            if (top > clipY + clipH || top + ph < clipY) {
                continue;
            }
            g.setColor(PAGE_EDGE);
            g.fillRect(left + 1, top + 1, pw + 1, ph + 1);
            g.setColor(0xffffff);
            g.fillRect(left, top, pw, ph);
            List<LayoutItem> items = l.getPage(p).getItems();
            for (int i = 0; i < items.size(); i++) {
                LayoutItem it = items.get(i);
                LayoutItem next = i + 1 < items.size() ? items.get(i + 1) : null;
                paintLastOnLine = next == null || next.paragraphId == null
                        || !next.paragraphId.equals(it.paragraphId) || Math.abs(next.baseline - it.baseline) > 0.5f;
                paintItem(g, it, left, top, scale);
            }
            if (sel != null) {
                paintSelection(g, l, p, sel, left, top, scale);
            }
            if (caretRect != null && (int) caretRect[0] == p) {
                g.setColor(CARET);
                g.fillRect(left + Math.round(caretRect[1] * scale), top
                        + Math.round(caretRect[2] * scale), Math.max(1, Math.round(scale)),
                        Math.max(1, Math.round(caretRect[3] * scale)));
            }
        }
    }

    private void paintItem(Graphics g, LayoutItem it, int left, int top, float scale) {
        int x = left + Math.round(it.x * scale);
        int y = top + Math.round(it.y * scale);
        int w = Math.max(1, Math.round(it.width * scale));
        int h = Math.max(1, Math.round(it.height * scale));
        switch (it.kind) {
            case LayoutItem.FILL:
                g.setColor(it.color);
                g.fillRect(x, y, w, h);
                break;
            case LayoutItem.LINE:
                g.setColor(it.color);
                g.drawLine(x, y, left + Math.round((it.x + it.width) * scale),
                        top + Math.round((it.y + it.height) * scale));
                break;
            case LayoutItem.TEXT:
                paintText(g, it, x, top + Math.round(it.baseline * scale), scale);
                break;
            case LayoutItem.PICTURE:
                paintPicture(g, it.picture, x, y, w, h);
                break;
            case LayoutItem.COMPONENT:
                paintComponentBox(g, it, x, y, w, h);
                break;
            default:
                break;
        }
    }

    /// Paints an embedded component's box: its snapshot, or a frame naming its type. A subclass
    /// that hosts live components paints them here.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics
    ///
    /// - `item`: the laid out component
    ///
    /// - `x`: the box's left edge in pixels
    ///
    /// - `y`: its top edge
    ///
    /// - `w`: its width
    ///
    /// - `h`: its height
    protected void paintComponentBox(Graphics g, LayoutItem item, int x, int y, int w, int h) {
        Picture snap = item.component.toPicture("snapshot:" + item.component.type);
        if (snap != null) {
            paintPicture(g, snap, x, y, w, h);
            return;
        }
        g.setColor(0xf3f5f7);
        g.fillRect(x, y, w, h);
        g.setColor(PAGE_EDGE);
        g.drawRect(x, y, w - 1, h - 1);
    }

    private void paintPicture(Graphics g, Picture p, int x, int y, int w, int h) {
        String key = p.id + ":" + p.dataLength() + ":" + p.hashCode();
        Image img = images.get(key);
        if (img == null && !images.containsKey(key) && !p.isLinked()) {
            try {
                img = EncodedImage.create(p.data());
            } catch (RuntimeException e) {
                img = null;
            }
            images.put(key, img);
        }
        if (img != null) {
            g.drawImage(img, x, y, w, h);
        } else {
            g.setColor(PAGE_EDGE);
            g.drawRect(x, y, w - 1, h - 1);
        }
    }

    private void paintText(Graphics g, LayoutItem it, int x, int baseline, float scale) {
        String s = it.text;
        if (s == null || s.length() == 0) {
            return;
        }
        TextStyle st = it.style;
        // rounded down: a font drawn a pixel larger than the size it was measured at spills
        // each word into the space after it and glues short gaps shut ("Westrecovered")
        int px = Math.max(1, (int) (PdfMeasurer.drawnSize(st) * scale + 0.001f));
        Font f = font(st, px);
        Revision r = st.revision;
        int color = r == null ? st.color : r.isDeletion() ? DELETE_COLOR : INSERT_COLOR;
        if (paintOrder != null && it.paragraphId != null) {
            paintComments(g, it, scale, x, baseline - f.getAscent(), f.getHeight());
        }
        g.setColor(color);
        g.setFont(f);
        drawWords(g, it, x, baseline - f.getAscent(), scale);
        // a space inside the run is the run's own, so a line under it carries on to the next
        // word; only the line's last item stops at its last glyph
        int textW = offset(it, paintLastOnLine ? rtrim(s).length() : s.length(), scale);
        int thick = Math.max(1, px / 14);
        // a revision mark starts after the spaces that open its run: struck through, the
        // space before a deleted word reads as no space at all ("Westrecovered"). Words carry
        // their own trailing spaces, so only a run's start has leading ones, and the mark
        // stays unbroken between the words of a run
        int lead = 0;
        while (lead < s.length() && s.charAt(lead) == ' ') {
            lead++;
        }
        int markX = x + offset(it, lead, scale);
        int markW = textW - (markX - x);
        if (st.underline || st.link != null && st.link.length() > 0) {
            g.fillRect(x, baseline + Math.max(1, px / 8), textW, thick);
        } else if (r != null && !r.isDeletion() && markW > 0) {
            g.fillRect(markX, baseline + Math.max(1, px / 8), markW, thick);
        }
        if (st.strike) {
            g.fillRect(x, baseline - px / 3, textW, thick);
        } else if (r != null && r.isDeletion() && markW > 0) {
            g.fillRect(markX, baseline - px / 3, markW, thick);
        }
        if (proofreader != null && it.paragraphId != null) {
            paintIssues(g, it, scale, x, baseline + Math.max(2, px / 6), Math.max(2, px / 8));
        }
    }

    private void paintIssues(Graphics g, LayoutItem it, float scale, int x, int y, int amp) {
        String text = paintTexts.get(it.paragraphId);
        if (text == null) {
            Paragraph p = model.findParagraph(it.paragraphId);
            text = p == null ? "" : p.text.plainText();
            paintTexts.put(it.paragraphId, text);
        }
        int from = it.start;
        int to = from + it.text.length();
        for (ProofIssue i : proofreader.check(text)) {
            if (i.end <= from || i.start >= to) {
                continue;
            }
            int a = Math.max(i.start, from) - from;
            int b = Math.min(i.end, to) - from;
            int x0 = x + offset(it, a, scale);
            int x1 = x + offset(it, b, scale);
            g.setColor(i.isSpelling() ? SPELLING_COLOR : GRAMMAR_COLOR);
            wave(g, x0, x1, y, amp);
        }
    }

    /// Where the first `chars` characters of an item end, in pixels from its left edge, by
    /// the measurer the page was laid out with.
    private int offset(LayoutItem it, int chars, float scale) {
        return chars <= 0 ? 0 : Math.round(getLayout().getMeasurer().width(it.text.substring(0, chars),
                it.style) * scale);
    }

    /// Draws an item a word at a time, each where the layout put it. The font drawn at a
    /// whole pixel size is never exactly the one measured, and drawn in one piece the
    /// difference would add up along the run and over the next one; this way it stays
    /// inside the space after each word.
    private void drawWords(Graphics g, LayoutItem it, int x, int y, float scale) {
        String s = it.text;
        int n = s.length();
        int start = 0;
        while (start < n) {
            while (start < n && s.charAt(start) == ' ') {
                start++;
            }
            if (start >= n) {
                break;
            }
            int end = s.indexOf(' ', start);
            if (end < 0) {
                end = n;
            }
            int at = x + offset(it, start, scale);
            int box = x + offset(it, end, scale) - at;
            String word = s.substring(start, end);
            if (repeated(word) || ScreenMeasurer.isMonospace(it.style)) {
                // a tab leader: every dot at its own measured place, so a run of them cannot
                // drift into the text after the tab
                // drift into the text after the tab; monospaced text the same way, each
                // character centred in its cell of the grid
                for (int k = start; k < end; k++) {
                    String c = s.substring(k, k + 1);
                    int cx = x + offset(it, k, scale);
                    int cell = x + offset(it, k + 1, scale) - cx;
                    g.drawString(c, cx + (cell - g.getFont().stringWidth(c)) / 2, y);
                }
            } else {
                // the font is drawn at a whole pixel size, so a word is a little wider or
                // narrower than the box it was measured into: centring it splits the
                // difference between the gaps on either side instead of piling it on one
                g.drawString(word, at + (box - g.getFont().stringWidth(word)) / 2, y);
            }
            start = end;
        }
    }

    private static boolean repeated(String word) {
        if (word.length() < 3) {
            return false;
        }
        char c = word.charAt(0);
        for (int i = 1; i < word.length(); i++) {
            if (word.charAt(i) != c) {
                return false;
            }
        }
        return true;
    }

    private void paintComments(Graphics g, LayoutItem it, float scale, int x, int y, int h) {
        int p = paintOrder.indexOf(it.paragraphId);
        int from = it.start;
        int to = from + it.text.length();
        for (Comment c : model.getDocument().comments) {
            int[] r = DocumentModel.commentRange(c, paintOrder, p, Integer.MAX_VALUE);
            if (r == null || r[1] <= from || r[0] >= to) {
                continue;
            }
            int a = Math.max(r[0], from) - from;
            int b = Math.min(r[1], to) - from;
            int x0 = x + offset(it, a, scale);
            int x1 = x + offset(it, b, scale);
            g.setColor(c.resolved ? RESOLVED_COMMENT_COLOR : COMMENT_COLOR);
            g.fillRect(x0, y, Math.max(1, x1 - x0), h);
        }
    }

    /// The review comments over the caret.
    ///
    /// #### Returns
    ///
    /// the comments, empty when there is no caret
    public List<Comment> getCommentsAtCaret() {
        return caret == null ? new ArrayList<Comment>() : model.commentsAt(caret);
    }

    /// Adds a review comment over the selection, by the model's author.
    ///
    /// #### Parameters
    ///
    /// - `text`: the comment
    ///
    /// #### Returns
    ///
    /// the comment, or null when nothing is selected
    public Comment commentSelection(String text) {
        TextPosition[] sel = getSelection();
        return sel == null ? null : model.addComment(sel[0], sel[1], text);
    }

    private static void wave(Graphics g, int x0, int x1, int y, int amp) {
        boolean up = true;
        for (int x = x0; x < x1; x += amp) {
            int nx = Math.min(x1, x + amp);
            g.drawLine(x, up ? y + amp : y, nx, up ? y : y + amp);
            up = !up;
        }
    }

    private static String rtrim(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return s.substring(0, end);
    }

    /// The platform font text of a style is drawn in, at a pixel size.
    ///
    /// #### Parameters
    ///
    /// - `style`: the style
    ///
    /// - `px`: the size in pixels
    ///
    /// #### Returns
    ///
    /// the font
    protected Font font(TextStyle style, int px) {
        return measurer instanceof ScreenMeasurer ? ((ScreenMeasurer) measurer).font(style, px)
                : screenFonts.font(style, px);
    }

    private void paintSelection(Graphics g, DocumentLayout l, int page, TextPosition[] sel,
            int left, int top, float scale) {
        List<String> order = new ArrayList<String>();
        DocumentModel.collectIds(model.getDocument().blocks, order);
        int a = order.indexOf(sel[0].paragraphId);
        int b = order.indexOf(sel[1].paragraphId);
        g.setColor(SELECTION);
        int alpha = g.getAlpha();
        g.setAlpha(80);
        for (LayoutItem it : l.getPage(page).getItems()) {
            if (it.paragraphId == null || it.kind == LayoutItem.FILL
                    || it.kind == LayoutItem.LINE) {
                continue;
            }
            int k = order.indexOf(it.paragraphId);
            if (k < a || k > b) {
                continue;
            }
            int len = it.kind == LayoutItem.TEXT ? it.text.length() : 1;
            int from = k == a ? Math.max(0, sel[0].offset - it.start) : 0;
            int to = k == b ? Math.min(len, sel[1].offset - it.start) : len;
            if (to <= from && !(len == 0 && k > a && k <= b)) {
                continue;
            }
            float x0 = it.x;
            float x1 = it.x + it.width;
            if (it.kind == LayoutItem.TEXT && len > 0) {
                x0 = it.x + l.getMeasurer().width(it.text.substring(0, from), it.style);
                x1 = it.x + l.getMeasurer().width(it.text.substring(0, to), it.style);
            }
            g.fillRect(left + Math.round(x0 * scale), top + Math.round(it.y * scale),
                    Math.max(2, Math.round((x1 - x0) * scale)), Math.round(it.height * scale));
        }
        g.setAlpha(alpha);
    }

    // ------------------------------------------------------------------ pointer

    /// The text position under a point in the view.
    ///
    /// #### Parameters
    ///
    /// - `x`: in the component's coordinates
    ///
    /// - `y`: in the component's coordinates
    ///
    /// #### Returns
    ///
    /// the position, or null
    public TextPosition positionAt(int x, int y) {
        int cx = x + getOffsetX();
        int cy = y + getOffsetY();
        int page = 0;
        while (page + 1 < getLayout().getPageCount() && cy >= pageTop(page + 1) - gap() / 2) {
            page++;
        }
        float scale = getScale();
        return getLayout().positionAt(page, (cx - pageLeft(page)) / scale,
                (cy - pageTop(page)) / scale);
    }

    @Override
    protected void tapped(int x, int y, boolean doubleTap) {
        if (getSelection() != null
                && touchChrome.press(this, x + getAbsoluteX(), y + getAbsoluteY())
                != TouchSelection.MISSED) {
            return;
        }
        TextPosition p = positionAt(x, y);
        if (p == null) {
            return;
        }
        if (doubleTap) {
            selectWord(p);
        } else if (Display.getInstance().isShiftKeyDown() && anchor != null) {
            setSelection(anchor, p);
        } else {
            setCaret(p);
        }
        if (editable) {
            startInput();
        }
    }

    private void selectWord(TextPosition p) {
        Paragraph para = model.findParagraph(p.paragraphId);
        if (para == null) {
            return;
        }
        String t = para.text.plainText();
        int a = Math.min(p.offset, t.length());
        int b = a;
        while (a > 0 && WordCharacters.isWordCharacter(t.charAt(a - 1))) {
            a--;
        }
        while (b < t.length() && WordCharacters.isWordCharacter(t.charAt(b))) {
            b++;
        }
        setSelection(new TextPosition(p.paragraphId, a), new TextPosition(p.paragraphId, b));
    }

    @Override
    protected boolean editStarted(int x, int y, EditTrigger trigger) {
        touchEdit = trigger != EditTrigger.DRAG;
        if (trigger == EditTrigger.HANDLE_DRAG) {
            // a finger drag pans unless it starts on a selection handle
            TextPosition[] sel = getSelection();
            int hit = sel == null ? TouchSelection.MISSED
                    : touchChrome.press(this, x + getAbsoluteX(), y + getAbsoluteY());
            if (hit == TouchSelection.START_HANDLE) {
                anchor = sel[1];
                caret = sel[0];
                return true;
            }
            if (hit == TouchSelection.END_HANDLE) {
                anchor = sel[0];
                caret = sel[1];
                return true;
            }
            return false;
        }
        TextPosition p = positionAt(x, y);
        if (p == null) {
            return false;
        }
        if (trigger == EditTrigger.LONG_PRESS) {
            selectWord(p);
        } else {
            setCaret(p);
        }
        return true;
    }

    @Override
    protected void editMoved(int x, int y) {
        TextPosition p = positionAt(x, y);
        if (p != null && anchor != null) {
            setSelection(anchor, p);
        }
    }

    @Override
    protected void editEnded(boolean committed) {
        if (touchEdit) {
            touchChrome.setShown(getSelection() != null);
            repaint();
        }
        if (editable) {
            startInput();
        }
    }

    @Override
    protected boolean animateSurface() {
        if (caret != null && hasFocus() && editable) {
            long now = System.currentTimeMillis();
            if (now - lastBlink >= 500) {
                caretOn = !caretOn;
                lastBlink = now;
                repaint();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void focusGained() {
        super.focusGained();
        requestAnimation();
    }

    @Override
    protected void focusLost() {
        super.focusLost();
        stopInput();
    }

    /// Paints the view, then the touch selection handles and toolbar over it.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (getSelection() != null) {
            touchChrome.paint(g, this);
        }
    }

    // ------------------------------------------------------------------ clipboard

    /// Copies the selection to the clipboard as plain text, HTML, RTF and Markdown, so it
    /// pastes with its formatting into a word processor, a browser or a text editor alike.
    public void copySelection() {
        Document d = selectionDocument();
        if (d == null) {
            return;
        }
        ClipboardContent content = new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, d.plainText())
                .setData(ClipboardContent.MIME_HTML, HtmlFormat.write(d, false))
                .setData(ClipboardContent.MIME_RTF, RichTextDocuments.writeRtf(d))
                .setData(ClipboardContent.MIME_MARKDOWN, RichTextDocuments.writeMarkdown(d));
        Display.getInstance().copyToClipboard(content);
    }

    /// Copies the selection to the clipboard and deletes it.
    public void cutSelection() {
        if (editable && getSelection() != null) {
            copySelection();
            setCaret(deleteSelection());
        }
    }

    /// Pastes the clipboard over the selection, keeping formatting when the clipboard holds
    /// HTML, RTF or Markdown.
    public void pasteClipboard() {
        if (!editable || caret == null) {
            return;
        }
        Display display = Display.getInstance();
        ClipboardContent content = display.getClipboardContent();
        String text = null;
        Document d = null;
        if (content != null) {
            String mime = content.findPreferredMimeType(new String[] {ClipboardContent.MIME_HTML,
                    ClipboardContent.MIME_RTF, ClipboardContent.MIME_MARKDOWN,
                    ClipboardContent.MIME_TEXT});
            String value = mime == null ? null : content.getText(mime);
            if (value != null) {
                try {
                    if (ClipboardContent.MIME_HTML.equals(mime)) {
                        d = HtmlFormat.read(value);
                    } else if (ClipboardContent.MIME_RTF.equals(mime)) {
                        d = RichTextDocuments.readRtf(value);
                    } else if (ClipboardContent.MIME_MARKDOWN.equals(mime)) {
                        d = RichTextDocuments.readMarkdown(value);
                    } else {
                        text = value;
                    }
                } catch (java.io.IOException ex) {
                    text = content.getText(ClipboardContent.MIME_TEXT);
                }
            }
        } else {
            Object data = display.getPasteDataFromClipboard();
            if (data instanceof String) {
                text = (String) data;
            }
        }
        if (d != null) {
            insertDocument(d);
        } else if (text != null && text.length() > 0) {
            typeText(text);
        }
    }

    /// Selects the whole document.
    public void selectAll() {
        Paragraph first = null;
        Paragraph last = null;
        for (int i = 0; i < model.size(); i++) {
            Block b = model.getBlock(i);
            if (b instanceof Paragraph) {
                if (first == null) {
                    first = (Paragraph) b;
                }
                last = (Paragraph) b;
            }
        }
        if (first != null) {
            setSelection(new TextPosition(first.id, 0), new TextPosition(last.id,
                    last.text.length()));
        }
    }

    private int topIndex(String id) {
        for (int i = 0; i < model.size(); i++) {
            if (id.equals(model.getBlock(i).id())) {
                return i;
            }
        }
        return -1;
    }

    /// The selection as a document of its own, for the clipboard.
    private Document selectionDocument() {
        TextPosition[] sel = getSelection();
        Paragraph first = sel == null ? null : model.findParagraph(sel[0].paragraphId);
        Paragraph last = sel == null ? null : model.findParagraph(sel[1].paragraphId);
        if (first == null || last == null) {
            return null;
        }
        List<Block> out = new ArrayList<Block>();
        if (first.id.equals(last.id)) {
            out.add(first.withText(first.text.substring(sel[0].offset, sel[1].offset)));
        } else {
            int a = topIndex(first.id);
            int b = topIndex(last.id);
            out.add(first.withText(first.text.substring(sel[0].offset, first.text.length())));
            // inside a table only the two ends are paragraphs of the body
            for (int i = a + 1; a >= 0 && b > a && i < b; i++) {
                out.add(model.getBlock(i));
            }
            out.add(last.withText(last.text.substring(0, sel[1].offset)));
        }
        return new Document(out, null, "", 1);
    }

    /// Inserts a pasted document at the caret: its first paragraph joins the text before the
    /// caret, its last the text after it, and the paragraphs between keep their own style.
    private void insertDocument(Document d) {
        List<Paragraph> paras = new ArrayList<Paragraph>();
        for (int i = 0; i < d.size(); i++) {
            Block b = d.get(i);
            if (b instanceof Paragraph) {
                paras.add((Paragraph) b);
            } else if (b instanceof Picture) {
                paras.add(new Paragraph("x", RichText.of((Picture) b, TextStyle.DEFAULT)));
            } else if (b instanceof Table) {
                Table t = (Table) b;
                for (int r = 0; r < t.rows(); r++) {
                    StringBuilder row = new StringBuilder();
                    for (int c = 0; c < t.columns(); c++) {
                        row.append(c > 0 ? "\t" : "").append(t.cellText(r, c).replace('\n', ' '));
                    }
                    paras.add(new Paragraph("x", RichText.of(row.toString())));
                }
            }
        }
        if (paras.isEmpty()) {
            return;
        }
        model.beginGroup();
        try {
            TextPosition at = deleteSelection();
            Paragraph target = model.findParagraph(at.paragraphId);
            if (paras.size() == 1 || topIndex(at.paragraphId) < 0) {
                // one paragraph, or a table cell, which holds no block of its own: as text
                RichText ins = paras.get(0).text;
                for (int i = 1; i < paras.size(); i++) {
                    ins = ins.append(RichText.of(" ")).append(paras.get(i).text);
                }
                model.replaceParagraph(target.withText(target.text.insert(at.offset, ins)));
                setCaret(new TextPosition(target.id, at.offset + ins.length()));
                return;
            }
            TextPosition second = model.splitParagraph(at);
            target = model.findParagraph(at.paragraphId);
            model.replaceParagraph(target.withText(target.text.append(paras.get(0).text)));
            int index = topIndex(second.paragraphId);
            for (int i = 1; i < paras.size() - 1; i++) {
                model.insertBlock(index++, paras.get(i).withId(model.newId('p')));
            }
            Paragraph after = model.findParagraph(second.paragraphId);
            RichText tail = paras.get(paras.size() - 1).text;
            model.replaceParagraph(after.withText(tail.append(after.text)));
            setCaret(new TextPosition(after.id, tail.length()));
        } finally {
            model.endGroup();
        }
    }

    // ------------------------------------------------------------------ editing

    /// Types text at the caret, replacing the selection.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text; a line break splits the paragraph
    public void typeText(String text) {
        if (!editable || caret == null || text == null || text.length() == 0) {
            return;
        }
        TextPosition at = deleteSelection();
        int nl = text.indexOf('\n');
        while (nl >= 0) {
            if (nl > 0) {
                at = model.insertText(at, text.substring(0, nl), null);
            }
            at = model.splitParagraph(at);
            text = text.substring(nl + 1);
            nl = text.indexOf('\n');
        }
        if (text.length() > 0) {
            at = model.insertText(at, text, null);
        }
        setCaret(at);
    }

    private TextPosition deleteSelection() {
        TextPosition[] sel = getSelection();
        if (sel == null) {
            return caret;
        }
        return model.delete(sel[0], sel[1]);
    }

    /// Deletes the selection, or the character before the caret, joining paragraphs at a
    /// paragraph start.
    public void deleteBackward() {
        if (!editable || caret == null) {
            return;
        }
        if (getSelection() != null) {
            setCaret(deleteSelection());
        } else if (caret.offset > 0) {
            Paragraph p = model.findParagraph(caret.paragraphId);
            int n = p != null && isPair(p.text.plainText(), caret.offset - 2) ? 2 : 1;
            setCaret(model.delete(new TextPosition(caret.paragraphId, caret.offset - n), caret));
        } else {
            TextPosition joined = model.joinWithPrevious(caret.paragraphId);
            if (joined != null) {
                setCaret(joined);
            }
        }
    }

    /// Whether the two chars at `i` are one character (a surrogate pair, such as an emoji),
    /// which must never be split.
    private static boolean isPair(String s, int i) {
        return i >= 0 && i + 1 < s.length() && s.charAt(i) >= '\ud800' && s.charAt(i) <= '\udbff'
                && s.charAt(i + 1) >= '\udc00' && s.charAt(i + 1) <= '\udfff';
    }

    /// Deletes the selection, or the character after the caret.
    public void deleteForward() {
        if (!editable || caret == null) {
            return;
        }
        if (getSelection() != null) {
            setCaret(deleteSelection());
            return;
        }
        Paragraph p = model.findParagraph(caret.paragraphId);
        if (p != null && caret.offset < p.text.length()) {
            int n = isPair(p.text.plainText(), caret.offset) ? 2 : 1;
            TextPosition at = model.delete(caret,
                    new TextPosition(caret.paragraphId, caret.offset + n));
            Paragraph after = model.findParagraph(caret.paragraphId);
            // a tracked deletion stays in the text: step over it
            boolean kept = after != null && after.text.length() == p.text.length();
            setCaret(kept ? new TextPosition(at.paragraphId, at.offset + n) : at);
        } else if (p != null) {
            List<String> ids = new ArrayList<String>();
            DocumentModel.collectIds(model.getDocument().blocks, ids);
            int i = ids.indexOf(p.id);
            if (i >= 0 && i + 1 < ids.size()) {
                TextPosition joined = model.joinWithPrevious(ids.get(i + 1));
                if (joined != null) {
                    setCaret(joined);
                }
            }
        }
    }

    /// Moves the caret one character, or extends the selection when `extend` is true.
    ///
    /// #### Parameters
    ///
    /// - `forward`: true to move right
    ///
    /// - `extend`: true to keep the selection anchor
    public void moveCaret(boolean forward, boolean extend) {
        if (caret == null) {
            return;
        }
        Paragraph p = model.findParagraph(caret.paragraphId);
        if (p == null) {
            return;
        }
        TextPosition next;
        if (forward && caret.offset < p.text.length()) {
            next = new TextPosition(p.id, caret.offset + 1);
        } else if (!forward && caret.offset > 0) {
            next = new TextPosition(p.id, caret.offset - 1);
        } else {
            List<String> ids = new ArrayList<String>();
            DocumentModel.collectIds(model.getDocument().blocks, ids);
            int i = ids.indexOf(p.id) + (forward ? 1 : -1);
            if (i < 0 || i >= ids.size()) {
                return;
            }
            Paragraph q = model.findParagraph(ids.get(i));
            next = new TextPosition(q.id, forward ? 0 : q.text.length());
        }
        setSelection(extend ? anchor : next, next);
    }

    /// Moves the caret a line up or down, or extends the selection.
    ///
    /// #### Parameters
    ///
    /// - `down`: true to move down
    ///
    /// - `extend`: true to keep the selection anchor
    public void moveCaretLine(boolean down, boolean extend) {
        if (caret == null) {
            return;
        }
        float[] r = getLayout().caretRect(caret);
        if (r == null) {
            return;
        }
        int page = (int) r[0];
        float y = down ? r[2] + r[3] + 1 : r[2] - 1;
        float limit = getLayout().getPage(page).getHeight();
        if (y > limit && page + 1 < getLayout().getPageCount()) {
            page++;
            y = 0;
        } else if (y < 0 && page > 0) {
            page--;
            y = limit;
        }
        TextPosition next = getLayout().positionAt(page, r[1], y);
        if (next != null) {
            setSelection(extend ? anchor : next, next);
        }
    }

    @Override
    public void keyPressed(int keyCode) {
        if (inputHandle != null || !editable || caret == null) {
            super.keyPressed(keyCode);
            return;
        }
        boolean shift = Display.getInstance().isShiftKeyDown();
        int game = Display.getInstance().getGameAction(keyCode);
        if (keyCode == 8 || keyCode == -8) {
            deleteBackward();
        } else if (keyCode == 127) {
            deleteForward();
        } else if (keyCode == 10 || keyCode == 13) {
            typeText("\n");
        } else if (game == Display.GAME_LEFT) {
            moveCaret(false, shift);
        } else if (game == Display.GAME_RIGHT) {
            moveCaret(true, shift);
        } else if (game == Display.GAME_UP) {
            moveCaretLine(false, shift);
        } else if (game == Display.GAME_DOWN) {
            moveCaretLine(true, shift);
        } else if (keyCode >= 32 && keyCode < 0xffff) {
            typeText(String.valueOf((char) keyCode));
        } else {
            super.keyPressed(keyCode);
        }
    }

    // ------------------------------------------------------------------ text input

    private void startInput() {
        if (inputHandle == null && editable && caret != null
                && Display.getInstance().isTextInputSupported()) {
            inputHandle = Display.getInstance().startTextInput(this, getConfig());
        }
    }

    private void stopInput() {
        if (inputHandle != null) {
            Display.getInstance().stopTextInput(inputHandle);
        }
        inputHandle = null;
    }

    private void pushInputState() {
        if (inputHandle != null) {
            Display.getInstance().updateTextInputState(inputHandle, getEditingState());
        }
    }

    private Paragraph caretParagraph() {
        return caret == null ? null : model.findParagraph(caret.paragraphId);
    }

    /// Typed text from the platform's keyboard or input method.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text
    @Override
    public void commitText(String text) {
        typeText(text);
    }

    /// Composition is committed whole: the composing text is typed as it is.
    ///
    /// #### Parameters
    ///
    /// - `text`: the composing text
    ///
    /// - `relativeCaret`: ignored
    @Override
    public void setComposingText(String text, int relativeCaret) {
        // shown when committed
    }

    /// Nothing to do: composing text is never shown before it is committed.
    @Override
    public void finishComposing() {
        // nothing pending
    }

    /// Deletes around the caret, within its paragraph.
    ///
    /// #### Parameters
    ///
    /// - `before`: characters before the caret
    ///
    /// - `after`: characters after it
    @Override
    public void deleteSurroundingText(int before, int after) {
        Paragraph p = caretParagraph();
        if (p == null || !editable) {
            return;
        }
        if (before > 0 && caret.offset == 0) {
            deleteBackward();
            return;
        }
        int a = Math.max(0, caret.offset - before);
        int b = Math.min(p.text.length(), caret.offset + after);
        setCaret(model.delete(new TextPosition(p.id, a), new TextPosition(p.id, b)));
    }

    /// A key command from the platform.
    ///
    /// #### Parameters
    ///
    /// - `command`: one of the `TextInputClient` commands
    ///
    /// - `modifiers`: modifier bits
    @Override
    public void onKeyCommand(int command, int modifiers) {
        boolean shift = (modifiers & TextInputClient.MOD_SHIFT) != 0;
        switch (command) {
            case TextInputClient.KEY_LEFT:
                moveCaret(false, shift);
                break;
            case TextInputClient.KEY_RIGHT:
                moveCaret(true, shift);
                break;
            case TextInputClient.KEY_UP:
                moveCaretLine(false, shift);
                break;
            case TextInputClient.KEY_DOWN:
                moveCaretLine(true, shift);
                break;
            case TextInputClient.KEY_BACKSPACE:
                deleteBackward();
                break;
            case TextInputClient.KEY_DELETE:
                deleteForward();
                break;
            case TextInputClient.KEY_UNDO:
                model.undo();
                break;
            case TextInputClient.KEY_REDO:
                model.redo();
                break;
            case TextInputClient.KEY_TAB:
                typeText("\t");
                break;
            case TextInputClient.KEY_COPY:
                copySelection();
                break;
            case TextInputClient.KEY_CUT:
                cutSelection();
                break;
            case TextInputClient.KEY_PASTE:
                pasteClipboard();
                break;
            case TextInputClient.KEY_SELECT_ALL:
                selectAll();
                break;
            default:
                break;
        }
    }

    /// The Enter action splits the paragraph.
    ///
    /// #### Parameters
    ///
    /// - `action`: the action
    @Override
    public void onEditorAction(int action) {
        typeText("\n");
    }

    /// The caret's paragraph, as the platform sees the text being edited.
    ///
    /// #### Returns
    ///
    /// the state
    @Override
    public TextInputState getEditingState() {
        Paragraph p = caretParagraph();
        if (p == null) {
            return new TextInputState("", 0, 0);
        }
        int a = caret.offset;
        int b = caret.offset;
        TextPosition[] sel = getSelection();
        if (sel != null && sel[0].paragraphId.equals(p.id) && sel[1].paragraphId.equals(p.id)) {
            a = sel[0].offset;
            b = sel[1].offset;
        }
        return new TextInputState(p.text.plainText(), a, b);
    }

    /// The caret rectangle in screen coordinates.
    ///
    /// #### Returns
    ///
    /// `{x, y, width, height}`
    @Override
    public int[] getCaretRect() {
        return caret == null ? new int[] {getAbsoluteX(), getAbsoluteY(), 1, 1}
                : rectForOffset(caret.offset);
    }

    /// Multi-line text with autocorrect.
    ///
    /// #### Returns
    ///
    /// the configuration
    @Override
    public TextInputConfig getConfig() {
        return new TextInputConfig().setMultiline(true).setAutoCorrect(true)
                .setAutoCapitalize(true);
    }

    /// Nothing to do.
    @Override
    public void inputFocusGained() {
        // the caret is already showing
    }

    /// The platform let go of input.
    @Override
    public void inputFocusLost() {
        inputHandle = null;
    }

    /// Nothing to undo: composing text is never inserted before it is committed.
    @Override
    public void cancelComposing() {
        // nothing pending
    }

    /// The caret and selection rectangles the platform places its candidate window by.
    ///
    /// #### Returns
    ///
    /// the snapshot
    @Override
    public TextInputGeometry getGeometrySnapshot() {
        return TextInputGeometry.capture(this);
    }

    /// The length of the caret's paragraph.
    ///
    /// #### Returns
    ///
    /// the length
    @Override
    public int getTextLength() {
        Paragraph p = caretParagraph();
        return p == null ? 0 : p.text.length();
    }

    /// Part of the caret's paragraph.
    ///
    /// #### Parameters
    ///
    /// - `start`: from
    ///
    /// - `end`: to
    ///
    /// #### Returns
    ///
    /// the text
    @Override
    public String getTextRange(int start, int end) {
        Paragraph p = caretParagraph();
        if (p == null) {
            return "";
        }
        String t = p.text.plainText();
        int a = Math.max(0, Math.min(t.length(), start));
        int b = Math.max(a, Math.min(t.length(), end));
        return t.substring(a, b);
    }

    /// Where an offset of the caret's paragraph is on screen.
    ///
    /// #### Parameters
    ///
    /// - `offset`: the offset
    ///
    /// #### Returns
    ///
    /// `{x, y, width, height}` in screen coordinates
    @Override
    public int[] rectForOffset(int offset) {
        Paragraph p = caretParagraph();
        float[] r = p == null ? null : getLayout().caretRect(new TextPosition(p.id, offset));
        if (r == null) {
            return new int[] {getAbsoluteX(), getAbsoluteY(), 1, 1};
        }
        float scale = getScale();
        int x = getAbsoluteX() - getOffsetX() + pageLeft((int) r[0]) + Math.round(r[1] * scale);
        int y = getAbsoluteY() - getOffsetY() + pageTop((int) r[0]) + Math.round(r[2] * scale);
        return new int[] {x, y, 1, Math.max(1, Math.round(r[3] * scale))};
    }

    /// The offset in the caret's paragraph nearest a screen point.
    ///
    /// #### Parameters
    ///
    /// - `x`: screen x
    ///
    /// - `y`: screen y
    ///
    /// #### Returns
    ///
    /// the offset
    @Override
    public int offsetAtPoint(int x, int y) {
        TextPosition p = positionAt(x - getAbsoluteX(), y - getAbsoluteY());
        if (p == null || caret == null || !p.paragraphId.equals(caret.paragraphId)) {
            return caret == null ? 0 : caret.offset;
        }
        return p.offset;
    }

    /// The selection rectangles of a range of the caret's paragraph.
    ///
    /// #### Parameters
    ///
    /// - `start`: from
    ///
    /// - `end`: to
    ///
    /// #### Returns
    ///
    /// the rectangle from the start's caret to the end's, as `{x, y, w, h}`
    @Override
    public int[] selectionRects(int start, int end) {
        int[] a = rectForOffset(start);
        int[] b = rectForOffset(end);
        return new int[] {a[0], a[1], Math.max(1, b[0] - a[0]), a[3]};
    }

    /// Replaces part of the caret's paragraph.
    ///
    /// #### Parameters
    ///
    /// - `start`: from
    ///
    /// - `end`: to
    ///
    /// - `text`: the replacement
    @Override
    public void replaceRange(int start, int end, String text) {
        Paragraph p = caretParagraph();
        if (p == null || !editable) {
            return;
        }
        int a = Math.max(0, Math.min(p.text.length(), start));
        int b = Math.max(a, Math.min(p.text.length(), end));
        TextPosition at = model.delete(new TextPosition(p.id, a), new TextPosition(p.id, b));
        setCaret(at);
        typeText(text);
    }

    /// Selects part of the caret's paragraph.
    ///
    /// #### Parameters
    ///
    /// - `start`: the anchor
    ///
    /// - `end`: the caret
    @Override
    public void setSelectionRange(int start, int end) {
        Paragraph p = caretParagraph();
        if (p == null) {
            return;
        }
        int a = Math.max(0, Math.min(p.text.length(), start));
        int b = Math.max(0, Math.min(p.text.length(), end));
        setSelection(new TextPosition(p.id, a), new TextPosition(p.id, b));
    }
}
