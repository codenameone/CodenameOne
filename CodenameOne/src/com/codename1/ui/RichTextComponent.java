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

package com.codename1.ui;

import com.codename1.ui.editor.HtmlImporter;
import com.codename1.ui.editor.RichBlocks;
import com.codename1.ui.editor.RichRunPainter;
import com.codename1.ui.editor.RichTextImporter;
import com.codename1.ui.editor.TextStyle;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A read-only component that renders multi-styled, word-wrapped rich text: headings, bold / italic /
/// underline / strike, inline code, colored and highlighted spans, per-paragraph alignment and
/// indentation, ordered and unordered lists, block quotes, preformatted blocks, inline images and
/// tappable hyperlinks. Content is supplied as HTML, Markdown, AsciiDoc, RTF or plain text, or built
/// up programmatically from styled runs.
///
/// <p>Unlike a single-style {@code Label}/{@code SpanLabel} it keeps a distinct style per character,
/// and unlike a full {@code BrowserComponent} it is a lightweight native component that measures and
/// paints text directly, so it embeds cleanly inside ordinary layouts and reports an accurate
/// height-for-width preferred size. It is the read-only counterpart to the rich text editor: both
/// share the same content model and {@link RichRunPainter} styling, so styled text rendered by one
/// matches the other.</p>
///
/// <p>Typical use:</p>
/// <pre>{@code
/// RichTextComponent rt = new RichTextComponent();
/// rt.setMarkdown("# Title\n\nSome **bold** and *italic* text with a [link](https://codenameone.com).");
/// rt.addLinkListener(e -> CN.execute((String) e.getSource()));
/// form.add(rt);
/// }</pre>
///
/// <p>The default {@link SizeMode#SHRINK} makes the component as tall as its wrapped content at the
/// width it is given (ideal inside a scrollable {@code Form}); {@link SizeMode#SCROLL} keeps the
/// component at the size its parent assigns and scrolls its content vertically.</p>
///
/// @author Codename One
public class RichTextComponent extends Component {

    /// Controls how the component sizes itself relative to its content.
    public enum SizeMode {
        /// Report a preferred height equal to the wrapped content height at the assigned width.
        SHRINK,
        /// Keep the parent-assigned size and scroll content vertically when it overflows.
        SCROLL
    }

    /// Resolves an image source string (as it appears in the markup, e.g. an {@code <img src>} URL or
    /// a Markdown image target) to a loaded {@link Image}. Return {@code null} to render a placeholder.
    public interface ImageResolver {
        /// Resolves a source to an image.
        /// @param source the source string from the markup
        /// @return the image, or null if it cannot be resolved
        Image resolve(String source);
    }

    private static final char OBJECT_REPLACEMENT = '\uFFFC';

    // --- content model (parallel per-character arrays + per-paragraph blocks) ---
    private final StringBuilder text = new StringBuilder(); // NOPMD - intentional owned buffer
    private List<TextStyle> charStyles = new ArrayList<TextStyle>();
    private List<String> charLinks = new ArrayList<String>();
    private List<String> charImages = new ArrayList<String>();
    private List<RichBlocks.BlockAttr> blocks = new ArrayList<RichBlocks.BlockAttr>();

    private SizeMode sizeMode = SizeMode.SHRINK;
    private int linkColor = 0x1a73e8;
    private int alignOverride = -1;
    private ImageResolver imageResolver;
    private EventDispatcher linkListeners;

    private final RichRunPainter painter = new RichRunPainter();
    private final Map<String, Image> imageCache = new HashMap<String, Image>();

    // --- layout cache ---
    private List<Row> rows;
    private int layoutWidth = -1;
    private int contentW;
    private int contentH;

    /// Creates an empty rich text component.
    public RichTextComponent() {
        setUIID("RichTextComponent");
        setFocusable(false);
        getAllStyles().setBgTransparency(0);
        blocks.add(new RichBlocks.BlockAttr());
    }

    /// Creates a rich text component showing the given plain text.
    /// @param plainText the text
    public RichTextComponent(String plainText) {
        this();
        setText(plainText);
    }

    // ------------------------------------------------------------------
    // Content setters
    // ------------------------------------------------------------------

    /// Replaces the content with the given HTML fragment.
    /// @param html the HTML markup
    /// @return this, for chaining
    public RichTextComponent setHtml(String html) {
        load(HtmlImporter.parse(html));
        return this;
    }

    /// Replaces the content with the given Markdown source.
    /// @param markdown the Markdown source
    /// @return this, for chaining
    public RichTextComponent setMarkdown(String markdown) {
        load(RichTextImporter.parse(markdown, RichTextFormat.MARKDOWN));
        return this;
    }

    /// Replaces the content parsed from the given format.
    /// @param content the source content
    /// @param format the format the content is written in
    /// @return this, for chaining
    public RichTextComponent setContent(String content, RichTextFormat format) {
        load(RichTextImporter.parse(content, format == null ? RichTextFormat.PLAIN_TEXT : format));
        return this;
    }

    /// Replaces the content with unstyled plain text. Newlines become paragraph breaks.
    /// @param plainText the text
    /// @return this, for chaining
    public RichTextComponent setText(String plainText) {
        clearContent();
        String value = plainText == null ? "" : plainText;
        for (int i = 0; i < value.length(); i++) {
            appendChar(value.charAt(i), TextStyle.DEFAULT, null, null);
        }
        syncBlocksToText();
        invalidateLayout();
        return this;
    }

    /// Removes all content, leaving the component empty. Useful before rebuilding content run by run
    /// with {@link #append(String, TextStyle)}.
    /// @return this, for chaining
    public RichTextComponent clear() {
        clearContent();
        blocks.add(new RichBlocks.BlockAttr());
        invalidateLayout();
        return this;
    }

    /// Appends a run of text in the given style to the current content (builder style). A run may
    /// contain newlines to start new paragraphs.
    /// @param runText the run text
    /// @param style the run style, or null for the default style
    /// @return this, for chaining
    public RichTextComponent append(String runText, TextStyle style) {
        return append(runText, style, null);
    }

    /// Appends a run of styled text that acts as a hyperlink.
    /// @param runText the run text
    /// @param style the run style, or null for the default style
    /// @param link the hyperlink target reported to link listeners, or null for none
    /// @return this, for chaining
    public RichTextComponent append(String runText, TextStyle style, String link) {
        if (runText != null) {
            TextStyle s = style == null ? TextStyle.DEFAULT : style;
            for (int i = 0; i < runText.length(); i++) {
                appendChar(runText.charAt(i), s, link, null);
            }
            syncBlocksToText();
            invalidateLayout();
        }
        return this;
    }

    /// The plain text of the current content (styling and structure removed).
    /// @return the plain text
    public String getText() {
        return text.toString();
    }

    /// Overrides the horizontal alignment of every paragraph, ignoring any alignment carried by the
    /// markup. Pass one of {@link Component#LEFT}, {@link Component#CENTER}, {@link Component#RIGHT},
    /// or {@code -1} to restore per-paragraph alignment from the content.
    /// @param align the alignment constant, or -1 to clear the override
    /// @return this, for chaining
    public RichTextComponent setTextAlign(int align) {
        if (align != alignOverride) {
            alignOverride = align;
            invalidateLayout();
        }
        return this;
    }

    /// Lays the content out at the given outer width and returns the resulting preferred size
    /// (including this component's padding). This is a stateless height-for-width query useful for
    /// custom layout managers that need the wrapped size before assigning bounds.
    /// @param width the outer width available to the component
    /// @return the preferred size at that width
    public Dimension preferredSizeForWidth(int width) {
        ensurePainter();
        Style style = getStyle();
        int hPad = style.getHorizontalPadding();
        int vPad = style.getVerticalPadding();
        layout(Math.max(1, width - hPad));
        return new Dimension(contentW + hPad, contentH + vPad);
    }

    // ------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------

    /// Sets the sizing behavior. Defaults to {@link SizeMode#SHRINK}.
    /// @param mode the size mode
    /// @return this, for chaining
    public RichTextComponent setSizeMode(SizeMode mode) {
        if (mode != null && mode != sizeMode) {
            sizeMode = mode;
            invalidateLayout();
        }
        return this;
    }

    /// The current size mode.
    /// @return the size mode
    public SizeMode getSizeMode() {
        return sizeMode;
    }

    @Override
    public boolean isScrollableY() {
        return sizeMode == SizeMode.SCROLL && getScrollDimension().getHeight() > getHeight();
    }

    /// Sets the color (0xRRGGBB) used for hyperlink text that does not carry an explicit color.
    /// @param rgb the link color
    /// @return this, for chaining
    public RichTextComponent setLinkColor(int rgb) {
        linkColor = rgb;
        repaint();
        return this;
    }

    /// Sets the resolver used to load inline images from their source strings.
    /// @param resolver the image resolver, or null to render placeholders
    /// @return this, for chaining
    public RichTextComponent setImageResolver(ImageResolver resolver) {
        imageResolver = resolver;
        imageCache.clear();
        invalidateLayout();
        return this;
    }

    /// Adds a listener notified when a hyperlink is tapped. The {@link ActionEvent#getSource()} is the
    /// link target string.
    /// @param l the listener
    public void addLinkListener(ActionListener l) {
        if (linkListeners == null) {
            linkListeners = new EventDispatcher();
        }
        linkListeners.addListener(l);
    }

    /// Removes a previously added link listener.
    /// @param l the listener
    public void removeLinkListener(ActionListener l) {
        if (linkListeners != null) {
            linkListeners.removeListener(l);
        }
    }

    // ------------------------------------------------------------------
    // Model plumbing
    // ------------------------------------------------------------------

    private void load(HtmlImporter.Result r) {
        clearContent();
        text.append(r.getText());
        charStyles = new ArrayList<TextStyle>(r.getStyles());
        charLinks = new ArrayList<String>(r.getLinks());
        charImages = new ArrayList<String>(r.getImageSources());
        blocks = new ArrayList<RichBlocks.BlockAttr>();
        for (RichBlocks.BlockAttr b : r.getBlocks()) {
            blocks.add(b);
        }
        normalizeModel();
        invalidateLayout();
    }

    private void clearContent() {
        text.setLength(0);
        charStyles = new ArrayList<TextStyle>();
        charLinks = new ArrayList<String>();
        charImages = new ArrayList<String>();
        blocks = new ArrayList<RichBlocks.BlockAttr>();
    }

    private void appendChar(char c, TextStyle style, String link, String image) {
        text.append(c);
        charStyles.add(style);
        charLinks.add(link);
        charImages.add(image);
    }

    /// Ensures per-character lists match the text length and there is one block per paragraph.
    private void normalizeModel() {
        int len = text.length();
        while (charStyles.size() < len) {
            charStyles.add(TextStyle.DEFAULT);
        }
        while (charLinks.size() < len) {
            charLinks.add(null);
        }
        while (charImages.size() < len) {
            charImages.add(null);
        }
        syncBlocksToText();
    }

    /// Keeps the block list length equal to the paragraph count (newlines + 1).
    private void syncBlocksToText() {
        int paragraphs = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                paragraphs++;
            }
        }
        if (blocks.isEmpty()) {
            blocks.add(new RichBlocks.BlockAttr());
        }
        while (blocks.size() < paragraphs) {
            blocks.add(new RichBlocks.BlockAttr());
        }
        while (blocks.size() > paragraphs) {
            blocks.remove(blocks.size() - 1);
        }
    }

    private void invalidateLayout() {
        layoutWidth = -1;
        rows = null;
        setShouldCalcPreferredSize(true);
        if (sizeMode == SizeMode.SCROLL) {
            setScrollSize(null);
        }
        repaint();
    }

    // ------------------------------------------------------------------
    // Sizing (height-for-width)
    // ------------------------------------------------------------------

    @Override
    public void setWidth(int width) {
        if (width != getWidth() && sizeMode == SizeMode.SHRINK) {
            layoutWidth = -1;
            rows = null;
            setShouldCalcPreferredSize(true);
        }
        super.setWidth(width);
    }

    @Override
    protected Dimension calcPreferredSize() {
        Style style = getStyle();
        int hPad = style.getHorizontalPadding();
        int vPad = style.getVerticalPadding();
        int w = getWidth();
        int avail;
        if (w > 0) {
            avail = w - hPad;
        } else {
            // no width assigned yet: measure the natural (unwrapped) width
            avail = Integer.MAX_VALUE / 4;
        }
        layout(Math.max(1, avail));
        return new Dimension(contentW + hPad, contentH + vPad);
    }

    @Override
    protected Dimension calcScrollSize() {
        Style style = getStyle();
        int hPad = style.getHorizontalPadding();
        int vPad = style.getVerticalPadding();
        int avail = Math.max(1, getWidth() - hPad);
        layout(avail);
        return new Dimension(contentW + hPad, contentH + vPad);
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private int baseSize() {
        return painter.getBaseSizePx();
    }

    private void ensurePainter() {
        Style style = getStyle();
        Font f = style.getFont();
        painter.setBaseSizePx(0);
        painter.setBaseFont(f == null ? Font.getDefaultFont() : f);
        painter.setTextColor(style.getFgColor());
    }

    /// Lays the content out into visual rows for the given available (inner) width; caches by width.
    private void layout(int availWidth) {
        if (rows != null && layoutWidth == availWidth) {
            return;
        }
        ensurePainter();
        normalizeModel();
        List<Row> out = new ArrayList<Row>();
        int base = baseSize();
        int indentUnit = Math.round(base * 1.6f);
        int lineGap = Math.round(base * 0.2f);
        int paraGap = Math.round(base * 0.45f);

        int y = 0;
        int maxRowWidth = 0;
        int paraStart = 0;
        int paraIndex = 0;
        int textLen = text.length();
        for (int i = 0; i <= textLen; i++) {
            if (i < textLen && text.charAt(i) != '\n') {
                continue;
            }
            // paragraph is [paraStart, i)
            RichBlocks.BlockAttr b = blocks.get(Math.min(paraIndex, blocks.size() - 1));
            boolean wrap = b.type != RichBlocks.PRE;
            int indentPx = b.indent * indentUnit;
            if (b.type == RichBlocks.BLOCKQUOTE) {
                indentPx += Math.round(base * 0.8f);
            }
            String bullet = null;
            int bulletWidth = 0;
            if (b.listType != RichBlocks.LIST_NONE) {
                bullet = markerFor(b, paraIndex);
                Font mf = painter.fontFor(base, false, false);
                bulletWidth = mf.stringWidth(bullet);
                indentPx += Math.round(base * 1.4f);
            }
            if (paraIndex > 0) {
                y += paraGap;
            }
            if (RichRunPainter.isHeading(b.type) && paraIndex > 0) {
                y += Math.round(base * 0.3f);
            }

            int avail = wrap ? Math.max(1, availWidth - indentPx) : Integer.MAX_VALUE / 4;
            List<Token> tokens = tokenize(paraStart, i, b.type);
            List<Row> paraRows = wrapTokens(tokens, avail, base);
            for (int r = 0; r < paraRows.size(); r++) {
                Row row = paraRows.get(r);
                row.y = y;
                row.indentPx = indentPx;
                row.align = alignOverride >= 0 ? toBlockAlign(alignOverride) : b.align;
                row.blockquote = b.type == RichBlocks.BLOCKQUOTE;
                if (r == 0 && bullet != null) {
                    row.bullet = bullet;
                    row.bulletWidth = bulletWidth;
                }
                y += row.height + lineGap;
                maxRowWidth = Math.max(maxRowWidth, indentPx + row.contentWidth);
                out.add(row);
            }

            paraStart = i + 1;
            paraIndex++;
        }

        rows = out;
        layoutWidth = availWidth;
        contentW = maxRowWidth;
        contentH = Math.max(base, y);
    }

    /// Ordered-list ordinal or bullet glyph for a list paragraph.
    private String markerFor(RichBlocks.BlockAttr b, int paraIndex) {
        if (b.listType == RichBlocks.LIST_ORDERED) {
            int ordinal = 1;
            for (int p = paraIndex - 1; p >= 0; p--) {
                RichBlocks.BlockAttr prev = blocks.get(p);
                if (prev.listType == RichBlocks.LIST_ORDERED && prev.indent == b.indent) {
                    ordinal++;
                } else if (prev.indent < b.indent) {
                    continue;
                } else {
                    break;
                }
            }
            return ordinal + ". ";
        }
        return "\u2022 ";
    }

    /// Splits a paragraph into word / space / image tokens. A word may span several styles (each style
    /// boundary is a piece), so multi-styled words render and wrap as a unit.
    private List<Token> tokenize(int start, int end, int blockType) {
        List<Token> tokens = new ArrayList<Token>();
        Token word = null;
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            TextStyle st = styleAt(i);
            String link = linkAt(i);
            if (c == OBJECT_REPLACEMENT) {
                if (word != null) {
                    tokens.add(word);
                    word = null;
                }
                tokens.add(imageToken(i, st, link));
                continue;
            }
            if (c == ' ' || c == '\t') {
                if (word != null) {
                    tokens.add(word);
                    word = null;
                }
                Font f = painter.runFont(blockType, st);
                Token sp = new Token(Token.SPACE);
                Piece p = new Piece();
                p.text = " ";
                p.style = st;
                p.link = link;
                p.font = f;
                p.width = f.charWidth(' ');
                p.height = f.getHeight();
                sp.pieces.add(p);
                sp.width = p.width;
                tokens.add(sp);
                continue;
            }
            if (word == null) {
                word = new Token(Token.WORD);
            }
            Font f = painter.runFont(blockType, st);
            // extend the current piece if the style/font is unchanged, else start a new piece
            Piece last = word.pieces.isEmpty() ? null : word.pieces.get(word.pieces.size() - 1);
            if (last != null && f.equals(last.font) && eq(last.link, link) && last.style.equals(st)) {
                last.text += c;
            } else {
                Piece p = new Piece();
                p.text = String.valueOf(c);
                p.style = st;
                p.link = link;
                p.font = f;
                word.pieces.add(p);
            }
        }
        if (word != null) {
            tokens.add(word);
        }
        // finalize per-piece and per-word widths
        for (Token t : tokens) {
            if (t.type == Token.WORD) {
                int w = 0;
                for (Piece p : t.pieces) {
                    p.width = p.font.stringWidth(p.text);
                    p.height = p.font.getHeight();
                    w += p.width;
                }
                t.width = w;
            }
        }
        return tokens;
    }

    private Token imageToken(int index, TextStyle st, String link) {
        Token t = new Token(Token.IMAGE);
        Piece p = new Piece();
        p.style = st;
        p.link = link;
        String src = index < charImages.size() ? charImages.get(index) : null;
        Image img = resolveImage(src);
        int base = baseSize();
        if (img != null) {
            p.image = img;
            p.width = img.getWidth();
            p.height = img.getHeight();
        } else {
            // placeholder square keeps layout stable when an image cannot be resolved
            p.width = base;
            p.height = base;
        }
        t.pieces.add(p);
        t.width = p.width;
        return t;
    }

    private Image resolveImage(String source) {
        if (source == null || source.length() == 0 || imageResolver == null) {
            return null;
        }
        if (imageCache.containsKey(source)) {
            return imageCache.get(source);
        }
        Image img = null;
        try {
            img = imageResolver.resolve(source);
        } catch (Throwable t) { // NOPMD - a bad resolver must not break layout
            img = null;
        }
        imageCache.put(source, img);
        return img;
    }

    /// Greedy line breaking of tokens into rows at the available width.
    private List<Row> wrapTokens(List<Token> tokens, int avail, int base) {
        List<Row> paraRows = new ArrayList<Row>();
        Row row = new Row();
        int curX = 0;
        List<Token> pendingSpaces = new ArrayList<Token>();
        for (Token t : tokens) {
            if (t.type == Token.SPACE) {
                pendingSpaces.add(t);
                continue;
            }
            int spaceW = 0;
            for (Token sp : pendingSpaces) {
                spaceW += sp.width;
            }
            if (!row.segs.isEmpty() && curX + spaceW + t.width > avail) {
                // wrap: discard the trailing spaces and start a new row with this token
                finishRow(row, base);
                paraRows.add(row);
                row = new Row();
                curX = 0;
                pendingSpaces.clear();
            } else {
                for (Token sp : pendingSpaces) {
                    curX = placeToken(row, sp, curX);
                }
                pendingSpaces.clear();
            }
            curX = placeToken(row, t, curX);
        }
        finishRow(row, base);
        paraRows.add(row);
        return paraRows;
    }

    private int placeToken(Row row, Token t, int x) {
        for (Piece p : t.pieces) {
            Seg seg = new Seg();
            seg.text = p.text;
            seg.image = p.image;
            seg.style = p.style;
            seg.link = p.link;
            seg.font = p.font;
            seg.x = x;
            seg.width = p.width;
            seg.height = p.height;
            row.segs.add(seg);
            x += p.width;
        }
        row.contentWidth = x;
        return x;
    }

    private void finishRow(Row row, int base) {
        int h = 0;
        for (Seg s : row.segs) {
            h = Math.max(h, s.height);
        }
        row.height = h > 0 ? h : base;
    }

    // ------------------------------------------------------------------
    // Paint
    // ------------------------------------------------------------------

    @Override
    public void paint(Graphics g) {
        Style style = getStyle();
        int hPad = style.getPaddingLeftNoRTL();
        int vPad = style.getPaddingTop();
        int avail = Math.max(1, getWidth() - style.getHorizontalPadding());
        layout(avail);
        int originX = getX() + hPad;
        int originY = getY() + vPad;
        int clipY = g.getClipY();
        int clipBottom = clipY + g.getClipHeight();

        for (Row row : rows) {
            int rowTop = originY + row.y;
            if (rowTop + row.height < clipY || rowTop > clipBottom) {
                continue;
            }
            int shift = 0;
            int free = avail - row.indentPx - row.contentWidth;
            if (row.align == RichBlocks.ALIGN_CENTER && free > 0) {
                shift = free / 2;
            } else if (row.align == RichBlocks.ALIGN_RIGHT && free > 0) {
                shift = free;
            }
            int rowLeft = originX + row.indentPx + shift;
            if (row.blockquote) {
                g.setColor(0xcccccc);
                int barX = originX + row.indentPx - Math.round(baseSize() * 0.6f);
                g.fillRect(barX, rowTop, Math.max(2, Math.round(baseSize() * 0.15f)), row.height);
            }
            if (row.bullet != null) {
                Font mf = painter.fontFor(baseSize(), false, false);
                g.setColor(style.getFgColor());
                g.setFont(mf);
                g.drawString(row.bullet, rowLeft - row.bulletWidth, rowTop
                        + Math.max(0, row.height - mf.getHeight()));
            }
            for (Seg seg : row.segs) {
                int sx = rowLeft + seg.x;
                if (seg.image != null) {
                    g.drawImage(seg.image, sx, rowTop + Math.max(0, row.height - seg.height));
                } else if (seg.text != null) {
                    TextStyle st = seg.style;
                    if (seg.link != null && (st == null || st.getForeColor() < 0)) {
                        st = (st == null ? TextStyle.DEFAULT : st).withForeColor(linkColor).withUnderline(true);
                    }
                    painter.paintRun(g, seg.text, st, seg.font, sx, rowTop, row.height);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Link hit-testing
    // ------------------------------------------------------------------

    @Override
    public void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
        if (linkListeners == null) {
            return;
        }
        Style style = getStyle();
        int avail = Math.max(1, getWidth() - style.getHorizontalPadding());
        layout(avail);
        int localX = x - getAbsoluteX() - style.getPaddingLeftNoRTL();
        int localY = y - getAbsoluteY() + getScrollY() - style.getPaddingTop();
        for (Row row : rows) {
            if (localY < row.y || localY > row.y + row.height) {
                continue;
            }
            int shift = 0;
            int free = avail - row.indentPx - row.contentWidth;
            if (row.align == RichBlocks.ALIGN_CENTER && free > 0) {
                shift = free / 2;
            } else if (row.align == RichBlocks.ALIGN_RIGHT && free > 0) {
                shift = free;
            }
            int rowLeft = row.indentPx + shift;
            for (Seg seg : row.segs) {
                int sx = rowLeft + seg.x;
                if (seg.link != null && localX >= sx && localX <= sx + seg.width) {
                    ActionEvent ev = new ActionEvent(seg.link, x, y);
                    linkListeners.fireActionEvent(ev);
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private TextStyle styleAt(int i) {
        if (i >= 0 && i < charStyles.size()) {
            TextStyle s = charStyles.get(i);
            return s == null ? TextStyle.DEFAULT : s;
        }
        return TextStyle.DEFAULT;
    }

    private String linkAt(int i) {
        return i >= 0 && i < charLinks.size() ? charLinks.get(i) : null;
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static int toBlockAlign(int componentAlign) {
        if (componentAlign == CENTER) {
            return RichBlocks.ALIGN_CENTER;
        }
        if (componentAlign == RIGHT) {
            return RichBlocks.ALIGN_RIGHT;
        }
        return RichBlocks.ALIGN_LEFT;
    }

    // ------------------------------------------------------------------
    // Layout structures
    // ------------------------------------------------------------------

    /// A styling piece within a token: a same-style text fragment or an image.
    private static final class Piece {
        String text;
        Image image;
        TextStyle style;
        String link;
        Font font;
        int width;
        int height;
    }

    /// A wrap unit: a word (one or more style pieces), a single space, or an image.
    private static final class Token {
        static final int WORD = 0;
        static final int SPACE = 1;
        static final int IMAGE = 2;
        final int type;
        final List<Piece> pieces = new ArrayList<Piece>();
        int width;

        Token(int type) {
            this.type = type;
        }
    }

    /// A positioned run within a visual row.
    private static final class Seg {
        String text;
        Image image;
        TextStyle style;
        String link;
        Font font;
        int x;
        int width;
        int height;
    }

    /// A single visual (wrapped) row.
    private static final class Row {
        final List<Seg> segs = new ArrayList<Seg>();
        int y;
        int height;
        int contentWidth;
        int indentPx;
        int align;
        boolean blockquote;
        String bullet;
        int bulletWidth;
    }
}
