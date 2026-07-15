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

import com.codename1.ui.Font;
import com.codename1.ui.ClipboardContent;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Display;
import com.codename1.ui.RichTextClipboardData;
import com.codename1.ui.RichTextFormat;
import com.codename1.ui.plaf.Style;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The pure rich text editor surface. It renders styled text (bold / italic / underline / strike,
/// foreground and highlight color, relative font size) organized into paragraphs with block attributes
/// (headings, preformatted, block quotes, alignment, ordered / unordered lists and indentation) on top
/// of the shared `EditorView` engine, keeping a parallel `InlineStyles` and `RichBlocks` model in lockstep
/// with the text.
public class RichView extends EditorView {
    private InlineStyles inline = new InlineStyles(0);
    private RichBlocks blocks = new RichBlocks(1);
    private TextStyle pendingStyle;
    private String pendingLink;
    private String placeholder = "";

    // parallel to the document characters: an inline image for each object-replacement char, else null
    private static final char OBJ = '\uFFFC';
    private final List<Image> imageRuns = new ArrayList<Image>();
    private final List<String> imageSources = new ArrayList<String>();
    private final List<String> linkRuns = new ArrayList<String>();

    private Font baseTtf;
    private final Map<Long, Font> fontCache = new HashMap<Long, Font>();

    /// Creates a rich text view.
    public RichView(EditorHost host) {
        super(host, false);
        setBackgroundColor(0xffffff);
        setTextColor(0x24292e);
        setSelectionColor(0xb3d4fc);
    }

    // ---- model access for the backend ----

    /// Returns the inline style model.
    public InlineStyles getInlineStyles() {
        return inline;
    }

    /// Returns the block attribute model.
    public RichBlocks getBlocks() {
        return blocks;
    }

    /// Returns the hyperlink target parallel model.
    public List<String> getLinkRuns() {
        return linkRuns;
    }

    /// Returns the image source parallel model.
    public List<String> getImageSources() {
        return imageSources;
    }

    /// Replaces the document with imported content: text plus per character styles and per paragraph
    /// block attributes. Used by the HTML importer.
    ///
    /// #### Parameters
    ///
    /// - `text`: the plain text
    ///
    /// - `perChar`: one style per character (may be shorter; missing entries default)
    ///
    /// - `blockList`: one block attribute per paragraph (may be shorter; missing entries default)
    public void importContent(String text, List<TextStyle> perChar, List<RichBlocks.BlockAttr> blockList,
                              List<String> links, List<Image> images, List<String> sources) {
        setText(text);
        for (int i = 0; i < perChar.size() && i < inline.length(); i++) {
            inline.setAt(i, perChar.get(i));
        }
        for (int i = 0; i < inline.length(); i++) {
            if (i < links.size()) {
                linkRuns.set(i, links.get(i));
            }
            if (i < images.size()) {
                imageRuns.set(i, images.get(i));
            }
            if (i < sources.size()) {
                imageSources.set(i, sources.get(i));
            }
        }
        for (int i = 0; i < blockList.size() && i < blocks.count(); i++) {
            RichBlocks.BlockAttr dst = blocks.get(i);
            RichBlocks.BlockAttr src = blockList.get(i);
            dst.type = src.type;
            dst.align = src.align;
            dst.listType = src.listType;
            dst.indent = src.indent;
        }
        invalidateLayout();
        repaint();
    }

    /// Inserts imported rich content at the selection while preserving its inline and block metadata.
    public void insertContent(String text, List<TextStyle> perChar, List<RichBlocks.BlockAttr> blockList,
                              List<String> links, List<Image> images, List<String> sources,
                              boolean blockContent) {
        int at = getSelectionStart();
        getUndoManager().breakRun();
        replaceRange(getSelectionStart(), getSelectionEnd(), text, true);
        int insertedLength = EditorDocument.normalizeText(text == null ? "" : text).length();
        for (int i = 0; i < insertedLength; i++) {
            int offset = at + i;
            if (i < perChar.size()) {
                inline.setAt(offset, perChar.get(i));
            }
            if (offset < linkRuns.size() && i < links.size()) {
                linkRuns.set(offset, links.get(i));
            }
            if (offset < imageRuns.size() && i < images.size()) {
                imageRuns.set(offset, images.get(i));
            }
            if (offset < imageSources.size() && i < sources.size()) {
                imageSources.set(offset, sources.get(i));
            }
        }
        if (blockContent) {
            int firstLine = getDocument().lineOfOffset(at);
            for (int i = 0; i < blockList.size() && firstLine + i < blocks.count(); i++) {
                copyBlock(blocks.get(firstLine + i), blockList.get(i));
            }
        }
        refreshUndoAfterState();
        getUndoManager().breakRun();
        invalidateLayout();
        repaint();
    }

    @Override
    protected void pasteClipboardData(Object data) {
        RichTextFormat format = null;
        String content = null;
        if (data instanceof ClipboardContent) {
            ClipboardContent clipboard = (ClipboardContent) data;
            String mimeType = clipboard.findPreferredMimeType(new String[] {
                    ClipboardContent.MIME_HTML, ClipboardContent.MIME_RTF,
                    ClipboardContent.MIME_MARKDOWN, ClipboardContent.MIME_ASCIIDOC,
                    ClipboardContent.MIME_TEXT
            });
            if (ClipboardContent.MIME_HTML.equals(mimeType)) {
                format = RichTextFormat.HTML;
            } else if (ClipboardContent.MIME_RTF.equals(mimeType)) {
                format = RichTextFormat.RTF;
            } else if (ClipboardContent.MIME_MARKDOWN.equals(mimeType)) {
                format = RichTextFormat.MARKDOWN;
            } else if (ClipboardContent.MIME_ASCIIDOC.equals(mimeType)) {
                format = RichTextFormat.ASCIIDOC;
            } else if (ClipboardContent.MIME_TEXT.equals(mimeType)) {
                format = RichTextFormat.PLAIN_TEXT;
            }
            content = clipboard.getText(mimeType);
        } else if (data instanceof String && ((String) data).trim().startsWith("{\\rtf")) {
            format = RichTextFormat.RTF;
            content = (String) data;
        }
        if (format == null || format == RichTextFormat.PLAIN_TEXT) {
            super.pasteClipboardData(data);
            return;
        }
        HtmlImporter.Result imported = RichTextImporter.parse(content, format);
        insertContent(imported.getText(), imported.getStyles(), imported.getBlocks(), imported.getLinks(),
                RichPureEditor.loadImages(imported.getImageSources()), imported.getImageSources(),
                imported.hasBlockContent());
    }

    @Override
    protected void copySelection() {
        if (!hasSelection()) {
            return;
        }
        int start = getSelectionStart();
        int end = getSelectionEnd();
        String plain = getDocument().substring(start, end);
        EditorDocument fragment = new EditorDocument();
        fragment.setText(plain);
        InlineStyles fragmentStyles = new InlineStyles(fragment.length());
        List<String> fragmentLinks = new ArrayList<String>(fragment.length());
        List<String> fragmentSources = new ArrayList<String>(fragment.length());
        for (int i = 0; i < fragment.length(); i++) {
            fragmentStyles.setAt(i, inline.styleAt(start + i));
            fragmentLinks.add(valueAt(linkRuns, start + i));
            fragmentSources.add(valueAt(imageSources, start + i));
        }
        RichBlocks fragmentBlocks = new RichBlocks(fragment.getLineCount());
        int firstLine = getDocument().lineOfOffset(start);
        for (int i = 0; i < fragmentBlocks.count(); i++) {
            copyBlock(fragmentBlocks.get(i), blocks.get(firstLine + i));
        }
        String html = HtmlSerializer.serialize(fragment, fragmentStyles, fragmentBlocks,
                fragmentLinks, fragmentSources);
        RichTextClipboardData clipboard = new RichTextClipboardData(plain).setHtml(html);
        clipboard.setRtf(RichTextSerializer.serialize(fragment, fragmentStyles, fragmentBlocks,
                fragmentLinks, fragmentSources, RichTextFormat.RTF));
        clipboard.setMarkdown(RichTextSerializer.serialize(fragment, fragmentStyles, fragmentBlocks,
                fragmentLinks, fragmentSources, RichTextFormat.MARKDOWN));
        clipboard.setAsciiDoc(RichTextSerializer.serialize(fragment, fragmentStyles, fragmentBlocks,
                fragmentLinks, fragmentSources, RichTextFormat.ASCIIDOC));
        Display.getInstance().copyToClipboard(clipboard);
    }

    private static String valueAt(List<String> values, int index) {
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }

    private static void copyBlock(RichBlocks.BlockAttr destination, RichBlocks.BlockAttr source) {
        destination.type = source.type;
        destination.align = source.align;
        destination.listType = source.listType;
        destination.indent = source.indent;
    }

    /// Sets the placeholder text shown when the document is empty.
    public void setPlaceholder(String text) {
        this.placeholder = text == null ? "" : text;
        repaint();
    }

    // ---- fonts ----

    private int baseSize() {
        return super.getEditorFont().getHeight();
    }

    private Font fontFor(boolean bold, boolean italic, int px) {
        Font base = super.getEditorFont();
        if (base != baseTtf) { // NOPMD - intentional identity comparison
            // the style font changed (skin / theme reload) - drop stale derived fonts
            baseTtf = base;
            fontCache.clear();
        }
        if (!base.isTTFNativeFont()) {
            // cannot derive size / weight from a non TTF font; use it as-is (correct size, no squares)
            return base;
        }
        if (px < 6) {
            px = 6;
        }
        long key = ((long) px << 2) | (bold ? 2 : 0) | (italic ? 1 : 0);
        Font f = fontCache.get(Long.valueOf(key));
        if (f != null) {
            return f;
        }
        int style = (bold ? Font.STYLE_BOLD : 0) | (italic ? Font.STYLE_ITALIC : 0);
        try {
            f = base.derive(px, style);
        } catch (Throwable t) {
            f = base;
        }
        fontCache.put(Long.valueOf(key), f);
        return f;
    }

    private static float headingScale(int type) {
        if (type == RichBlocks.H1) {
            return 2.0f;
        }
        if (type == RichBlocks.H1 + 1) {
            return 1.5f;
        }
        if (type == RichBlocks.H1 + 2) {
            return 1.25f;
        }
        if (type == RichBlocks.H1 + 3) {
            return 1.1f;
        }
        if (type == RichBlocks.H1 + 4) {
            return 0.9f;
        }
        if (type == RichBlocks.H1 + 5) {
            return 0.8f;
        }
        return 1.0f;
    }

    private static boolean isHeading(int type) {
        return type >= RichBlocks.H1 && type <= RichBlocks.H1 + 5;
    }

    private static float sizeLevelScale(int level) {
        switch (level) {
            case 1:
                return 0.7f;
            case 2:
                return 0.85f;
            case 4:
                return 1.15f;
            case 5:
                return 1.5f;
            case 6:
                return 2.0f;
            case 7:
                return 2.5f;
            default:
                return 1.0f;
        }
    }

    private int runPx(int blockType, int level) {
        return Math.round(baseSize() * headingScale(blockType) * sizeLevelScale(level));
    }

    @Override
    protected boolean uniformLineHeight() {
        return false;
    }

    @Override
    protected int lineHeightAt(int line) {
        RichBlocks.BlockAttr b = blocks.get(line);
        int lineStart = getDocument().getLineStart(line);
        String text = getDocument().getLineText(line);
        int maxLevel = 0;
        for (int i = 0; i < text.length(); i++) {
            int lv = inline.styleAt(lineStart + i).getFontSizeLevel();
            if (lv > maxLevel) {
                maxLevel = lv;
            }
        }
        int px = runPx(b.type, maxLevel);
        int h = fontFor(isHeading(b.type), false, px).getHeight();
        int textH = h + Math.max(2, h / 6);
        int imgH = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == OBJ) {
                Image im = imageAt(lineStart + i);
                if (im != null) {
                    imgH = Math.max(imgH, imageDisplayHeight(im));
                }
            }
        }
        return Math.max(textH, imgH + 4);
    }

    // ---- model sync ----

    @Override
    protected void documentReset() {
        inline.reset(getDocument().length());
        blocks.reset(getDocument().getLineCount());
        imageRuns.clear();
        imageSources.clear();
        linkRuns.clear();
        int len = getDocument().length();
        for (int i = 0; i < len; i++) {
            imageRuns.add(null);
            imageSources.add(null);
            linkRuns.add(null);
        }
        pendingStyle = null;
        pendingLink = null;
        invalidateLayout();
    }

    @Override
    protected void documentReplaced(int start, String removed, String inserted) {
        TextStyle ins = pendingStyle != null ? pendingStyle
                : (start > 0 ? inline.styleAt(start - 1) : TextStyle.DEFAULT);
        inline.applyEdit(start, removed.length(), inserted.length(), ins);
        int rem = removed.length();
        for (int i = 0; i < rem && start < imageRuns.size(); i++) {
            imageRuns.remove(start);
            imageSources.remove(start);
            linkRuns.remove(start);
        }
        for (int i = 0; i < inserted.length(); i++) {
            imageRuns.add(Math.min(start + i, imageRuns.size()), null);
            imageSources.add(Math.min(start + i, imageSources.size()), null);
            linkRuns.add(Math.min(start + i, linkRuns.size()), pendingLink);
        }
        int para = getDocument().lineOfOffset(start);
        blocks.applyEdit(para, countNewlines(removed), countNewlines(inserted));
        invalidateLayout();
    }

    private static final class RichState {
        final List<TextStyle> styles = new ArrayList<TextStyle>();
        final List<RichBlocks.BlockAttr> blockAttrs = new ArrayList<RichBlocks.BlockAttr>();
        final List<Image> images = new ArrayList<Image>();
        final List<String> sources = new ArrayList<String>();
        final List<String> links = new ArrayList<String>();
        TextStyle pendingStyle;
        String pendingLink;
    }

    @Override
    protected Object captureDocumentState() {
        RichState state = new RichState();
        for (int i = 0; i < inline.length(); i++) {
            state.styles.add(inline.styleAt(i));
            state.images.add(i < imageRuns.size() ? imageRuns.get(i) : null);
            state.sources.add(i < imageSources.size() ? imageSources.get(i) : null);
            state.links.add(i < linkRuns.size() ? linkRuns.get(i) : null);
        }
        for (int i = 0; i < blocks.count(); i++) {
            state.blockAttrs.add(blocks.get(i).copy());
        }
        state.pendingStyle = pendingStyle;
        state.pendingLink = pendingLink;
        return state;
    }

    @Override
    protected void restoreDocumentState(Object snapshot) {
        if (!(snapshot instanceof RichState)) {
            return;
        }
        RichState state = (RichState) snapshot;
        inline.reset(getDocument().length());
        imageRuns.clear();
        imageSources.clear();
        linkRuns.clear();
        for (int i = 0; i < getDocument().length(); i++) {
            if (i < state.styles.size()) {
                inline.setAt(i, state.styles.get(i));
            }
            imageRuns.add(i < state.images.size() ? state.images.get(i) : null);
            imageSources.add(i < state.sources.size() ? state.sources.get(i) : null);
            linkRuns.add(i < state.links.size() ? state.links.get(i) : null);
        }
        blocks.reset(getDocument().getLineCount());
        for (int i = 0; i < state.blockAttrs.size() && i < blocks.count(); i++) {
            copyBlock(blocks.get(i), state.blockAttrs.get(i));
        }
        pendingStyle = state.pendingStyle;
        pendingLink = state.pendingLink;
        invalidateLayout();
    }

    private Image imageAt(int offset) {
        return offset >= 0 && offset < imageRuns.size() ? imageRuns.get(offset) : null;
    }

    private int imageDisplayWidth(Image img) {
        int maxW = Math.max(16, contentWidth() - baseSize());
        int w = img.getWidth();
        return Math.min(w, maxW);
    }

    private int imageDisplayHeight(Image img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int dw = imageDisplayWidth(img);
        return w > 0 ? h * dw / w : h;
    }

    /// Inserts an inline image at the caret (replacing any selection).
    public void insertImageObject(Image img, String source) {
        if (!isEditableState() || img == null) {
            return;
        }
        int at = getSelectionStart();
        getUndoManager().breakRun();
        replaceRange(getSelectionStart(), getSelectionEnd(), String.valueOf(OBJ), true);
        if (at < imageRuns.size()) {
            imageRuns.set(at, img);
            imageSources.set(at, source);
        }
        refreshUndoAfterState();
        getUndoManager().breakRun();
        invalidateLayout();
        repaint();
    }

    private static int countNewlines(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                n++;
            }
        }
        return n;
    }

    @Override
    public void moveCaret(int newCaret, boolean extend) {
        pendingStyle = null;
        pendingLink = null;
        super.moveCaret(newCaret, extend);
    }

    private TextStyle styleAtCaret() {
        int c = getCaretOffset();
        return inline.styleAt(c > 0 ? c - 1 : 0);
    }

    // ---- rendering ----

    private String listPrefix(int line, RichBlocks.BlockAttr b) {
        if (b.listType == RichBlocks.LIST_UNORDERED) {
            return "\u2022  ";
        }
        if (b.listType == RichBlocks.LIST_ORDERED) {
            return ordinal(line) + ".  ";
        }
        return null;
    }

    private int prefixWidth(int line, RichBlocks.BlockAttr b) {
        String p = listPrefix(line, b);
        if (p == null) {
            return 0;
        }
        return fontFor(false, false, runPx(b.type, 0)).stringWidth(p);
    }

    // ---- bidi atom layout: a line is split into atoms that are single bidi level + single inline style
    // (an inline image is one atom); the atoms are reordered into visual order so mixed LTR/RTL styled
    // text lays out, hit tests and paints correctly while each atom keeps its own font. ----

    /// Splits a line into atoms in logical order. Each atom is `{logStart, logEnd, level, isImage}`.
    private List<int[]> richAtomsLogical(String text, int lineStart, byte[] levels) {
        List<int[]> atoms = new ArrayList<int[]>();
        int c = 0;
        int n = text.length();
        while (c < n) {
            if (text.charAt(c) == OBJ && imageAt(lineStart + c) != null) {
                atoms.add(new int[]{c, c + 1, levels[c], 1});
                c++;
                continue;
            }
            TextStyle st = inline.styleAt(lineStart + c);
            int lvl = levels[c];
            int e = c + 1;
            while (e < n && text.charAt(e) != OBJ && levels[e] == lvl
                    && inline.styleAt(lineStart + e).equals(st)) {
                e++;
            }
            atoms.add(new int[]{c, e, lvl, 0});
            c = e;
        }
        return atoms;
    }

    /// Pixel width of `text[from, to)` measured with the inline style at `from` (the atom is single style).
    private int styledWidth(String text, int lineStart, int from, int to, RichBlocks.BlockAttr b) {
        if (to <= from) {
            return 0;
        }
        TextStyle st = inline.styleAt(lineStart + from);
        Font rf = fontFor(st.isBold() || isHeading(b.type), st.isItalic(), runPx(b.type, st.getFontSizeLevel()));
        return rf.stringWidth(text.substring(from, to));
    }

    private int atomWidth(String text, int lineStart, int[] atom, RichBlocks.BlockAttr b) {
        if (atom[3] == 1) {
            Image img = imageAt(lineStart + atom[0]);
            return img != null ? imageDisplayWidth(img) : 0;
        }
        return styledWidth(text, lineStart, atom[0], atom[1], b);
    }

    /// Visual (left to right) atom layout: each entry is `{logStart, logEnd, level, isImage, xLeft, width}`.
    private int[][] richVisualAtoms(int line, String text, int lineStart) {
        byte[] levels = BidiUtil.resolveLevels(text, isRTL());
        List<int[]> logical = richAtomsLogical(text, lineStart, levels);
        byte[] al = new byte[logical.size()];
        for (int i = 0; i < al.length; i++) {
            al[i] = (byte) logical.get(i)[2];
        }
        int[] vis = BidiUtil.reorderVisual(al);
        RichBlocks.BlockAttr b = blocks.get(line);
        int[][] out = new int[vis.length][6];
        int x = 0;
        for (int vi = 0; vi < vis.length; vi++) {
            int[] a = logical.get(vis[vi]);
            int w = atomWidth(text, lineStart, a, b);
            out[vi] = new int[]{a[0], a[1], a[2], a[3], x, w};
            x += w;
        }
        return out;
    }

    @Override
    protected int measureColumnX(int line, String text, int col, Font f) {
        int lineStart = getDocument().getLineStart(line);
        RichBlocks.BlockAttr b = blocks.get(line);
        if (!BidiUtil.isTrivialLtr(text, isRTL())) {
            int[][] atoms = richVisualAtoms(line, text, lineStart);
            for (int[] atom : atoms) {
                int as = atom[0];
                int ae = atom[1];
                int lvl = atom[2];
                int img = atom[3];
                int ax = atom[4];
                if (col >= as && col <= ae) {
                    if (img == 1) {
                        boolean before = (lvl & 1) == 0 ? col == as : col == ae;
                        return before ? ax : ax + atom[5];
                    }
                    if ((lvl & 1) == 0) {
                        return ax + styledWidth(text, lineStart, as, col, b);
                    }
                    return ax + styledWidth(text, lineStart, col, ae, b);
                }
            }
            return atoms.length > 0 ? atoms[atoms.length - 1][4] + atoms[atoms.length - 1][5] : 0;
        }
        int x = 0;
        int c = 0;
        while (c < col && c < text.length()) {
            Image img = text.charAt(c) == OBJ ? imageAt(lineStart + c) : null;
            if (img != null) {
                x += imageDisplayWidth(img);
                c++;
                continue;
            }
            TextStyle st = inline.styleAt(lineStart + c);
            int runEnd = c + 1;
            while (runEnd < col && runEnd < text.length() && text.charAt(runEnd) != OBJ
                    && inline.styleAt(lineStart + runEnd).equals(st)) {
                runEnd++;
            }
            Font rf = fontFor(st.isBold() || isHeading(b.type), st.isItalic(), runPx(b.type, st.getFontSizeLevel()));
            x += rf.stringWidth(text.substring(c, runEnd));
            c = runEnd;
        }
        return x;
    }

    @Override
    protected int columnAtX(int line, String text, int localX, Font f) {
        if (BidiUtil.isTrivialLtr(text, isRTL())) {
            return super.columnAtX(line, text, localX, f);
        }
        int lineStart = getDocument().getLineStart(line);
        RichBlocks.BlockAttr b = blocks.get(line);
        int[][] atoms = richVisualAtoms(line, text, lineStart);
        for (int i = 0; i < atoms.length; i++) {
            int as = atoms[i][0];
            int ae = atoms[i][1];
            int lvl = atoms[i][2];
            int img = atoms[i][3];
            int ax = atoms[i][4];
            int aw = atoms[i][5];
            boolean last = i == atoms.length - 1;
            if (localX < ax + aw || last) {
                if (img == 1) {
                    boolean rightHalf = localX > ax + aw / 2;
                    if ((lvl & 1) == 0) {
                        return rightHalf ? ae : as;
                    }
                    return rightHalf ? as : ae;
                }
                if ((lvl & 1) == 0) {
                    int c = as;
                    while (c < ae) {
                        int wa = ax + styledWidth(text, lineStart, as, c, b);
                        int wb = ax + styledWidth(text, lineStart, as, c + 1, b);
                        if (localX < (wa + wb) / 2) {
                            break;
                        }
                        c++;
                    }
                    return c;
                }
                int c = ae;
                while (c > as) {
                    int wa = ax + styledWidth(text, lineStart, c, ae, b);
                    int wb = ax + styledWidth(text, lineStart, c - 1, ae, b);
                    if (localX < (wa + wb) / 2) {
                        break;
                    }
                    c--;
                }
                return c;
            }
        }
        return text.length();
    }

    @Override
    protected int[] lineVisualSegments(int line, String text, int from, int to, Font f) {
        if (BidiUtil.isTrivialLtr(text, isRTL())) {
            return super.lineVisualSegments(line, text, from, to, f);
        }
        int lineStart = getDocument().getLineStart(line);
        RichBlocks.BlockAttr b = blocks.get(line);
        int[][] atoms = richVisualAtoms(line, text, lineStart);
        List<int[]> segs = new ArrayList<int[]>();
        for (int[] atom : atoms) {
            int as = atom[0];
            int ae = atom[1];
            int lvl = atom[2];
            int img = atom[3];
            int ax = atom[4];
            int aw = atom[5];
            int a = Math.max(from, as);
            int e = Math.min(to, ae);
            if (a >= e) {
                continue;
            }
            int xl;
            int xr;
            if (img == 1) {
                xl = ax;
                xr = ax + aw;
            } else if ((lvl & 1) == 0) {
                xl = ax + styledWidth(text, lineStart, as, a, b);
                xr = ax + styledWidth(text, lineStart, as, e, b);
            } else {
                xl = ax + styledWidth(text, lineStart, e, ae, b);
                xr = ax + styledWidth(text, lineStart, a, ae, b);
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

    @Override
    protected int lineContentOffsetX(int line) {
        RichBlocks.BlockAttr b = blocks.get(line);
        int off = b.indent * baseSize();
        if (b.type == RichBlocks.BLOCKQUOTE) {
            off += baseSize() / 2;
        }
        int prefixW = prefixWidth(line, b);
        String text = getDocument().getLineText(line);
        int totalW = prefixW + measureLine(line, text);
        int avail = contentWidth() - off;
        if (b.align == RichBlocks.ALIGN_CENTER && totalW < avail) {
            off += (avail - totalW) / 2;
        } else if (b.align == RichBlocks.ALIGN_RIGHT && totalW < avail) {
            off += avail - totalW;
        }
        return off + prefixW;
    }

    @Override
    protected void paintLine(Graphics g, int line, String text, int x, int y, int lineHeight, Font f) {
        RichBlocks.BlockAttr b = blocks.get(line);
        int lineStart = getDocument().getLineStart(line);
        int indentPx = b.indent * baseSize();

        // block quote bar sits at the indented left edge
        if (b.type == RichBlocks.BLOCKQUOTE) {
            g.setColor(0xcccccc);
            g.fillRect(x + indentPx, y, Math.max(2, baseSize() / 8), lineHeight);
        }

        // text starts at the shared content offset so the caret / selection line up
        int textStart = x + lineContentOffsetX(line);

        String prefix = listPrefix(line, b);
        if (prefix != null) {
            Font prefixFont = fontFor(false, false, runPx(b.type, 0));
            int prefixW = prefixFont.stringWidth(prefix);
            g.setColor(getTextColor());
            g.setFont(prefixFont);
            g.drawString(prefix, textStart - prefixW, y + baseline(lineHeight, prefixFont));
        }

        // styled runs
        int col = 0;
        int cx = textStart;
        while (col < text.length()) {
            Image img = text.charAt(col) == OBJ ? imageAt(lineStart + col) : null;
            if (img != null) {
                int iw = imageDisplayWidth(img);
                int ih = imageDisplayHeight(img);
                g.drawImage(img, cx, y + Math.max(0, lineHeight - ih), iw, ih);
                cx += iw;
                col++;
                continue;
            }
            TextStyle st = inline.styleAt(lineStart + col);
            int runEnd = col + 1;
            while (runEnd < text.length() && text.charAt(runEnd) != OBJ
                    && inline.styleAt(lineStart + runEnd).equals(st)) {
                runEnd++;
            }
            String run = text.substring(col, runEnd);
            boolean bold = st.isBold() || isHeading(b.type);
            Font rf = fontFor(bold, st.isItalic(), runPx(b.type, st.getFontSizeLevel()));
            int w = rf.stringWidth(run);
            int ry = y + baseline(lineHeight, rf);
            if (st.getHighlight() >= 0) {
                g.setColor(st.getHighlight());
                g.fillRect(cx, y, w, lineHeight);
            }
            int decoration = 0;
            if (st.isUnderline()) {
                decoration |= Style.TEXT_DECORATION_UNDERLINE;
            }
            if (st.isStrike()) {
                decoration |= Style.TEXT_DECORATION_STRIKETHRU;
            }
            g.setColor(st.getForeColor() >= 0 ? st.getForeColor() : getTextColor());
            g.setFont(rf);
            g.drawString(run, cx, ry, decoration);
            cx += w;
            col = runEnd;
        }

        // placeholder
        if (line == 0 && getDocument().length() == 0 && placeholder.length() > 0 && !hasFocus()) {
            g.setColor(0x9aa0a6);
            g.setFont(fontFor(false, false, baseSize()));
            g.drawString(placeholder, x, y + baseline(lineHeight, fontFor(false, false, baseSize())));
        }
    }

    private int measureLine(int line, String text) {
        return measureColumnX(line, text, text.length(), getEditorFont());
    }

    private int baseline(int lineHeight, Font f) {
        return lineHeight - f.getHeight();
    }

    private int ordinal(int line) {
        int n = 1;
        for (int i = line - 1; i >= 0; i--) {
            RichBlocks.BlockAttr pb = blocks.get(i);
            if (pb.listType == RichBlocks.LIST_ORDERED) {
                n++;
            } else {
                break;
            }
        }
        return n;
    }

    @Override
    protected Font getEditorFont() {
        return fontFor(false, false, baseSize());
    }

    // ---- inline formatting commands ----

    private void applyToggle(int which) {
        int s = getSelectionStart();
        int e = getSelectionEnd();
        boolean current = isSetInRange(s, e, which);
        final boolean value = !current;
        final int w = which;
        if (s == e) {
            TextStyle base = pendingStyle != null ? pendingStyle : styleAtCaret();
            pendingStyle = withFlag(base, w, value);
            return;
        }
        inline.transformRange(s, e, new InlineStyles.StyleTransform() {
            @Override
            public TextStyle apply(TextStyle st) {
                return withFlag(st, w, value);
            }
        });
        afterStyleChange();
    }

    private static TextStyle withFlag(TextStyle st, int which, boolean value) {
        switch (which) {
            case 0:
                return st.withBold(value);
            case 1:
                return st.withItalic(value);
            case 2:
                return st.withUnderline(value);
            default:
                return st.withStrike(value);
        }
    }

    private boolean isSetInRange(int s, int e, final int which) {
        return inline.allInRange(s, e, new InlineStyles.StylePredicate() {
            @Override
            public boolean test(TextStyle st) {
                switch (which) {
                    case 0:
                        return st.isBold();
                    case 1:
                        return st.isItalic();
                    case 2:
                        return st.isUnderline();
                    default:
                        return st.isStrike();
                }
            }
        });
    }

    /// Toggles bold on the selection (or the pending typing style when the selection is empty).
    public void toggleBold() {
        applyToggle(0);
    }

    /// Toggles italic.
    public void toggleItalic() {
        applyToggle(1);
    }

    /// Toggles underline.
    public void toggleUnderline() {
        applyToggle(2);
    }

    /// Toggles strike-through.
    public void toggleStrike() {
        applyToggle(3);
    }

    /// Sets the foreground color on the selection.
    public void setForeColor(final int rgb) {
        applyStyle(new InlineStyles.StyleTransform() {
            @Override
            public TextStyle apply(TextStyle st) {
                return st.withForeColor(rgb);
            }
        });
    }

    /// Sets the highlight color on the selection.
    public void setHighlight(final int rgb) {
        applyStyle(new InlineStyles.StyleTransform() {
            @Override
            public TextStyle apply(TextStyle st) {
                return st.withHighlight(rgb);
            }
        });
    }

    /// Sets the relative font size level (1..7) on the selection.
    public void setFontSizeLevel(final int level) {
        applyStyle(new InlineStyles.StyleTransform() {
            @Override
            public TextStyle apply(TextStyle st) {
                return st.withFontSizeLevel(level);
            }
        });
    }

    /// Removes all inline formatting from the selection.
    public void removeFormat() {
        int s = getSelectionStart();
        int e = getSelectionEnd();
        if (s == e) {
            pendingStyle = TextStyle.DEFAULT;
            return;
        }
        inline.transformRange(s, e, new InlineStyles.StyleTransform() {
            @Override
            public TextStyle apply(TextStyle st) {
                return TextStyle.DEFAULT;
            }
        });
        afterStyleChange();
    }

    private void applyStyle(InlineStyles.StyleTransform t) {
        int s = getSelectionStart();
        int e = getSelectionEnd();
        if (s == e) {
            TextStyle base = pendingStyle != null ? pendingStyle : styleAtCaret();
            pendingStyle = t.apply(base);
            return;
        }
        inline.transformRange(s, e, t);
        afterStyleChange();
    }

    private void afterStyleChange() {
        invalidateLayout();
        if (host() != null) {
            host().editorChanged();
        }
        repaint();
    }

    // ---- block formatting commands ----

    private void forEachSelectedBlock(BlockOp op) {
        int s = getSelectionStart();
        int e = getSelectionEnd();
        int firstLine = getDocument().lineOfOffset(s);
        int lastLine = getDocument().lineOfOffset(e);
        for (int ln = firstLine; ln <= lastLine; ln++) {
            op.apply(blocks.get(ln));
        }
        afterStyleChange();
    }

    private interface BlockOp {
        void apply(RichBlocks.BlockAttr b);
    }

    /// Sets the block type (`p`, `h1`..`h6`, `pre`, `blockquote`) on the selected paragraphs.
    public void setBlockFormat(String tag) {
        final int type = blockTypeForTag(tag);
        forEachSelectedBlock(new BlockOp() {
            @Override
            public void apply(RichBlocks.BlockAttr b) {
                b.type = type;
                b.listType = RichBlocks.LIST_NONE;
            }
        });
    }

    private static int blockTypeForTag(String tag) {
        if (tag == null) {
            return RichBlocks.PARAGRAPH;
        }
        if (tag.length() == 2 && (tag.charAt(0) == 'h' || tag.charAt(0) == 'H')) {
            int n = tag.charAt(1) - '0';
            if (n >= 1 && n <= 6) {
                return RichBlocks.H1 + (n - 1);
            }
        }
        if ("pre".equalsIgnoreCase(tag)) {
            return RichBlocks.PRE;
        }
        if ("blockquote".equalsIgnoreCase(tag)) {
            return RichBlocks.BLOCKQUOTE;
        }
        return RichBlocks.PARAGRAPH;
    }

    /// Sets the alignment on the selected paragraphs.
    public void setAlign(final int align) {
        forEachSelectedBlock(new BlockOp() {
            @Override
            public void apply(RichBlocks.BlockAttr b) {
                b.align = align;
            }
        });
    }

    /// Applies (or toggles off) an ordered / unordered list on the selected paragraphs.
    public void setList(final int listType) {
        int s = getSelectionStart();
        int firstLine = getDocument().lineOfOffset(s);
        boolean allSame = blocks.get(firstLine).listType == listType;
        final int target = allSame ? RichBlocks.LIST_NONE : listType;
        forEachSelectedBlock(new BlockOp() {
            @Override
            public void apply(RichBlocks.BlockAttr b) {
                b.listType = target;
            }
        });
    }

    /// Increases the indentation of the selected paragraphs.
    public void indentBlocks() {
        forEachSelectedBlock(new BlockOp() {
            @Override
            public void apply(RichBlocks.BlockAttr b) {
                b.indent++;
            }
        });
    }

    /// Decreases the indentation of the selected paragraphs.
    public void outdentBlocks() {
        forEachSelectedBlock(new BlockOp() {
            @Override
            public void apply(RichBlocks.BlockAttr b) {
                if (b.indent > 0) {
                    b.indent--;
                }
            }
        });
    }

    /// Applies a hyperlink target and its default appearance to the selection.
    public void applyLink(String url) {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        if (start == end) {
            pendingLink = url;
        } else {
            for (int i = start; i < end && i < linkRuns.size(); i++) {
                linkRuns.set(i, url);
            }
        }
        applyStyle(new InlineStyles.StyleTransform() {
            @Override
            public TextStyle apply(TextStyle st) {
                return st.withUnderline(true).withForeColor(0x1a73e8);
            }
        });
    }

    /// Removes the link appearance from the selection.
    public void removeLinkStyle() {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        if (start == end) {
            pendingLink = null;
        } else {
            for (int i = start; i < end && i < linkRuns.size(); i++) {
                linkRuns.set(i, null);
            }
        }
        applyStyle(new InlineStyles.StyleTransform() {
            @Override
            public TextStyle apply(TextStyle st) {
                return st.withUnderline(false).withForeColor(-1);
            }
        });
    }

    /// Returns whether an inline command (`bold`, `italic`, `underline`, `strikeThrough`) is active for
    /// the current selection.
    public boolean queryState(String command) {
        int s = getSelectionStart();
        int e = getSelectionEnd();
        if ("bold".equals(command)) {
            return isSetInRange(s, e, 0);
        }
        if ("italic".equals(command)) {
            return isSetInRange(s, e, 1);
        }
        if ("underline".equals(command)) {
            return isSetInRange(s, e, 2);
        }
        if ("strikeThrough".equals(command)) {
            return isSetInRange(s, e, 3);
        }
        return false;
    }
}
