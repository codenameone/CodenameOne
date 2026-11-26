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

import com.codename1.ui.EncodedImage;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Paints laid out items on a `Graphics`, the way `DocumentView` paints its pages.
///
/// Anything that shows text laid out by `DocumentLayout` -- a slide's text box, a thumbnail,
/// a print preview -- paints it with this class, so it looks exactly as it does in the word
/// processor: each word is drawn where the layout measured it, underlines and strike lines
/// follow the measured text, and pictures are decoded once and kept.
///
/// ```java
/// void paintFirstPage(Graphics g, Document doc, int x, int y) {
///     DocumentLayout layout = new DocumentLayout(doc, new ScreenMeasurer());
///     LayoutPainter painter = new LayoutPainter(layout.getMeasurer());
///     painter.paint(g, layout.getPage(0).getItems(), x, y, 2f);
/// }
/// ```
public class LayoutPainter {
    private static final int INSERT_COLOR = 0x1f6feb;
    private static final int DELETE_COLOR = 0xc0392b;
    private static final int FRAME = 0xc5cad1;
    private final Measurer measurer;
    private final ScreenMeasurer screenFonts;
    private final Map<String, Image> images = new HashMap<String, Image>();

    /// A painter for items laid out with a measurer.
    ///
    /// #### Parameters
    ///
    /// - `measurer`: the measurer the items were laid out with; word positions are computed
    ///   with it
    public LayoutPainter(Measurer measurer) {
        if (measurer == null) {
            throw new IllegalArgumentException("A measurer is required");
        }
        this.measurer = measurer;
        this.screenFonts = measurer instanceof ScreenMeasurer ? (ScreenMeasurer) measurer
                : new ScreenMeasurer();
    }

    /// The measurer items are positioned with.
    ///
    /// #### Returns
    ///
    /// the measurer
    public Measurer getMeasurer() {
        return measurer;
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
    public Font font(TextStyle style, int px) {
        return screenFonts.font(style, px);
    }

    /// Paints items.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics
    ///
    /// - `items`: the items, in points relative to their page
    ///
    /// - `left`: where the page's left edge is, in pixels
    ///
    /// - `top`: where the page's top edge is, in pixels
    ///
    /// - `scale`: pixels per point
    public void paint(Graphics g, List<LayoutItem> items, int left, int top, float scale) {
        for (int i = 0; i < items.size(); i++) {
            LayoutItem it = items.get(i);
            LayoutItem next = i + 1 < items.size() ? items.get(i + 1) : null;
            boolean last = next == null || next.paragraphId == null
                    || !next.paragraphId.equals(it.paragraphId)
                    || Math.abs(next.baseline - it.baseline) > 0.5f;
            paintItem(g, it, left, top, scale, last);
        }
    }

    /// Paints one item.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics
    ///
    /// - `it`: the item
    ///
    /// - `left`: the page's left edge in pixels
    ///
    /// - `top`: the page's top edge in pixels
    ///
    /// - `scale`: pixels per point
    ///
    /// - `lastOnLine`: true when it is the last item of its line, whose underline stops at its
    ///   last glyph instead of running through its trailing spaces
    public void paintItem(Graphics g, LayoutItem it, int left, int top, float scale,
            boolean lastOnLine) {
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
                paintText(g, it, x, top + Math.round(it.baseline * scale), scale, lastOnLine);
                break;
            case LayoutItem.PICTURE:
                paintPicture(g, it.picture, x, y, w, h);
                break;
            case LayoutItem.COMPONENT:
                Picture snap = it.component.toPicture("snapshot:" + it.component.type);
                if (snap != null) {
                    paintPicture(g, snap, x, y, w, h);
                } else {
                    g.setColor(FRAME);
                    g.drawRect(x, y, w - 1, h - 1);
                }
                break;
            default:
                break;
        }
    }

    /// Paints a text item: its words, underline, strike line and revision marks.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics
    ///
    /// - `it`: a `LayoutItem.TEXT` item
    ///
    /// - `x`: its left edge in pixels
    ///
    /// - `baseline`: its baseline in pixels
    ///
    /// - `scale`: pixels per point
    ///
    /// - `lastOnLine`: true when it ends its line
    public void paintText(Graphics g, LayoutItem it, int x, int baseline, float scale,
            boolean lastOnLine) {
        String s = it.text;
        if (s == null || s.length() == 0) {
            return;
        }
        TextStyle st = it.style;
        // rounded down: a font drawn a pixel larger than the size it was measured at spills
        // each word into the space after it
        int px = Math.max(1, (int) (PdfMeasurer.drawnSize(st) * scale + 0.001f));
        Font f = font(st, px);
        Revision r = st.revision;
        int color = r == null ? st.color : r.isDeletion() ? DELETE_COLOR : INSERT_COLOR;
        g.setColor(color);
        g.setFont(f);
        drawWords(g, it, x, baseline - f.getAscent(), scale);
        int textW = offset(it, lastOnLine ? rtrim(s).length() : s.length(), scale);
        int thick = Math.max(1, px / 14);
        if (st.underline || r != null && !r.isDeletion()
                || st.link != null && st.link.length() > 0) {
            g.fillRect(x, baseline + Math.max(1, px / 8), textW, thick);
        }
        if (st.strike || r != null && r.isDeletion()) {
            g.fillRect(x, baseline - px / 3, textW, thick);
        }
    }

    /// Paints a picture scaled into a box, or a frame when it cannot be decoded.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics
    ///
    /// - `p`: the picture
    ///
    /// - `x`: the box's left edge
    ///
    /// - `y`: its top edge
    ///
    /// - `w`: its width
    ///
    /// - `h`: its height
    public void paintPicture(Graphics g, Picture p, int x, int y, int w, int h) {
        Image img = image(p);
        if (img != null) {
            g.drawImage(img, x, y, w, h);
        } else {
            g.setColor(FRAME);
            g.drawRect(x, y, w - 1, h - 1);
        }
    }

    /// The decoded image of a picture, decoded once; null when it is linked or not an image
    /// the platform reads.
    ///
    /// #### Parameters
    ///
    /// - `p`: the picture
    ///
    /// #### Returns
    ///
    /// the image or null
    public Image image(Picture p) {
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
        return img;
    }

    /// Where the first `chars` characters of an item end, in pixels from its left edge, by
    /// the measurer the item was laid out with.
    ///
    /// #### Parameters
    ///
    /// - `it`: a text item
    ///
    /// - `chars`: the number of characters
    ///
    /// - `scale`: pixels per point
    ///
    /// #### Returns
    ///
    /// the offset in pixels
    public int offset(LayoutItem it, int chars, float scale) {
        return chars <= 0 ? 0 : Math.round(measurer.width(it.text.substring(0, chars),
                it.style) * scale);
    }

    /// Draws an item a word at a time, each where the layout put it, in the graphics' current
    /// font and color. The font drawn at a whole pixel size is never exactly the one
    /// measured, and drawn in one piece the difference would add up along the run; this way
    /// it stays inside the space after each word.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics
    ///
    /// - `it`: a text item
    ///
    /// - `x`: its left edge in pixels
    ///
    /// - `y`: the top of its glyphs in pixels
    ///
    /// - `scale`: pixels per point
    public void drawWords(Graphics g, LayoutItem it, int x, int y, float scale) {
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
                // a tab leader, or monospaced text: every character at its own measured place,
                // centred in its cell, so a run of them cannot drift into the text after it
                for (int k = start; k < end; k++) {
                    String c = s.substring(k, k + 1);
                    int cx = x + offset(it, k, scale);
                    int cell = x + offset(it, k + 1, scale) - cx;
                    g.drawString(c, cx + (cell - g.getFont().stringWidth(c)) / 2, y);
                }
            } else {
                Font f = g.getFont();
                int ww = f.stringWidth(word);
                if (ww > box && box > 0 && f.isTTFNativeFont()) {
                    // At a few pixels a glyph's advance rounds up, so a word drawn at the
                    // scaled size comes out wider than the box it was measured into, and
                    // centred there it covered its neighbours: a thumbnail's title read
                    // "Shapeandfills". Shrink just that word until it fits, on the same
                    // baseline. At ordinary sizes no word overflows and nothing is derived.
                    Font fit = f.derive(Math.max(1f, f.getPixelSize() * box / (float) ww),
                            f.getStyle());
                    g.setFont(fit);
                    g.drawString(word, at + (box - fit.stringWidth(word)) / 2,
                            y + f.getAscent() - fit.getAscent());
                    g.setFont(f);
                } else {
                    // centring the word splits the rounding difference between the gaps on
                    // either side instead of piling it on one
                    g.drawString(word, at + (box - ww) / 2, y);
                }
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

    static String rtrim(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return s.substring(0, end);
    }
}
