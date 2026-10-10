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
package com.codename1.desktopcompat.javax.swing.border;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;

/// A border with a title drawn on or beside its top or bottom edge, the
/// edge broken where the text crosses it.
///
/// There is no look and feel here, so the defaults are fixed: a border
/// without one of its own draws a lowered [EtchedBorder], and a title
/// without a font or colour takes those of the component it is painted on.
/// Every container is laid out left to right, so `LEADING` is the left and
/// `TRAILING` the right. A title too long for the border is clipped rather
/// than shortened with an ellipsis.
public class TitledBorder extends AbstractBorder {

    public static final int DEFAULT_POSITION = 0;

    public static final int ABOVE_TOP = 1;

    public static final int TOP = 2;

    public static final int BELOW_TOP = 3;

    public static final int ABOVE_BOTTOM = 4;

    public static final int BOTTOM = 5;

    public static final int BELOW_BOTTOM = 6;

    public static final int DEFAULT_JUSTIFICATION = 0;

    public static final int LEFT = 1;

    public static final int CENTER = 2;

    public static final int RIGHT = 3;

    public static final int LEADING = 4;

    public static final int TRAILING = 5;

    protected static final int EDGE_SPACING = 2;

    protected static final int TEXT_SPACING = 2;

    protected static final int TEXT_INSET_H = 5;

    private static final Border FALLBACK = new EtchedBorder(EtchedBorder.LOWERED);

    protected String title;

    protected Border border;

    protected int titlePosition;

    protected int titleJustification;

    protected Font titleFont;

    protected Color titleColor;

    public TitledBorder(String title) {
        this(null, title, LEADING, DEFAULT_POSITION, null, null);
    }

    public TitledBorder(Border border) {
        this(border, "", LEADING, DEFAULT_POSITION, null, null);
    }

    public TitledBorder(Border border, String title) {
        this(border, title, LEADING, DEFAULT_POSITION, null, null);
    }

    public TitledBorder(Border border, String title, int titleJustification, int titlePosition) {
        this(border, title, titleJustification, titlePosition, null, null);
    }

    public TitledBorder(Border border, String title, int titleJustification, int titlePosition, Font titleFont) {
        this(border, title, titleJustification, titlePosition, titleFont, null);
    }

    public TitledBorder(Border border, String title, int titleJustification, int titlePosition, Font titleFont,
            Color titleColor) {
        this.title = title;
        this.border = border;
        this.titleFont = titleFont;
        this.titleColor = titleColor;
        checkJustification(titleJustification);
        checkPosition(titlePosition);
        this.titleJustification = titleJustification;
        this.titlePosition = titlePosition;
    }

    private static void checkPosition(int titlePosition) {
        if (titlePosition < DEFAULT_POSITION || titlePosition > BELOW_BOTTOM) {
            throw new IllegalArgumentException(titlePosition + " is not a valid title position.");
        }
    }

    private static void checkJustification(int titleJustification) {
        if (titleJustification < DEFAULT_JUSTIFICATION || titleJustification > TRAILING) {
            throw new IllegalArgumentException(titleJustification + " is not a valid title justification.");
        }
    }

    private boolean hasTitle() {
        return title != null && title.length() > 0;
    }

    private int position() {
        return titlePosition == DEFAULT_POSITION ? TOP : titlePosition;
    }

    private Border frame() {
        return border != null ? border : FALLBACK;
    }

    private static FontMetrics metrics(Component c, Font f) {
        return c == null || f == null ? null : c.getFontMetrics(f);
    }

    /// The height of a line of the title, estimated from the font size when
    /// the component cannot measure it.
    private static int lineHeight(FontMetrics fm, Font f) {
        if (fm != null) {
            return fm.getHeight();
        }
        return f != null ? f.getSize() + f.getSize() / 4 : 15;
    }

    private int textWidth(FontMetrics fm, Font f) {
        if (fm != null) {
            return fm.stringWidth(title);
        }
        return title.length() * (f != null ? f.getSize() : 12) / 2;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Border frame = frame();
        if (!hasTitle()) {
            frame.paintBorder(c, g, x, y, width, height);
            return;
        }
        Font font = getFont(c);
        FontMetrics fm = metrics(c, font);
        if (fm == null) {
            fm = g.getFontMetrics(font);
        }
        int th = lineHeight(fm, font);
        int ascent = fm != null ? fm.getAscent() : th * 4 / 5;
        Insets bi = frame.getBorderInsets(c);

        // The frame, inset from the component's edge, and the top of the
        // line of text.
        int bx = x + EDGE_SPACING;
        int by = y + EDGE_SPACING;
        int bw = width - EDGE_SPACING * 2;
        int bh = height - EDGE_SPACING * 2;
        int textTop;
        int pos = position();
        switch (pos) {
            case ABOVE_TOP:
                textTop = by;
                by += th;
                bh -= th;
                break;
            case BELOW_TOP:
                textTop = by + bi.top + TEXT_SPACING;
                break;
            case ABOVE_BOTTOM:
                textTop = by + bh - bi.bottom - TEXT_SPACING - th;
                break;
            case BOTTOM: {
                int drop = th > bi.bottom ? (th - bi.bottom) / 2 : 0;
                bh -= drop;
                textTop = by + bh - bi.bottom + (bi.bottom - th) / 2;
                break;
            }
            case BELOW_BOTTOM:
                bh -= th;
                textTop = by + bh;
                break;
            default: {
                int drop = th > bi.top ? (th - bi.top) / 2 : 0;
                by += drop;
                bh -= drop;
                textTop = by + (bi.top - th) / 2;
                break;
            }
        }

        int room = bw - bi.left - bi.right - TEXT_INSET_H * 2;
        int tw = Math.min(textWidth(fm, font), Math.max(room, 0));
        int tx;
        switch (titleJustification) {
            case CENTER:
                tx = bx + (bw - tw) / 2;
                break;
            case RIGHT:
            case TRAILING:
                tx = bx + bw - bi.right - TEXT_INSET_H - tw;
                break;
            default:
                tx = bx + bi.left + TEXT_INSET_H;
                break;
        }

        if (pos == TOP || pos == BOTTOM) {
            // The frame in three pieces, leaving out the stretch of edge
            // the text sits on.
            int gapLeft = tx - TEXT_SPACING;
            int gapRight = tx + tw + TEXT_SPACING;
            paintClipped(frame, c, g, bx, by, bw, bh, x, y, gapLeft - x, height);
            paintClipped(frame, c, g, bx, by, bw, bh, gapRight, y, x + width - gapRight, height);
            if (pos == TOP) {
                int below = textTop + th;
                paintClipped(frame, c, g, bx, by, bw, bh, gapLeft, below, gapRight - gapLeft, y + height - below);
            } else {
                paintClipped(frame, c, g, bx, by, bw, bh, gapLeft, y, gapRight - gapLeft, textTop - y);
            }
        } else {
            frame.paintBorder(c, g, bx, by, bw, bh);
        }

        Color color = getColor(c);
        Graphics text = g.create();
        text.clipRect(tx, textTop, tw, th);
        text.setFont(font);
        if (color != null) {
            text.setColor(color);
        }
        text.drawString(title, tx, textTop + ascent);
        text.dispose();
    }

    private static void paintClipped(Border frame, Component c, Graphics g, int bx, int by, int bw, int bh, int cx,
            int cy, int cw, int ch) {
        if (cw <= 0 || ch <= 0) {
            return;
        }
        Graphics part = g.create();
        part.clipRect(cx, cy, cw, ch);
        frame.paintBorder(c, part, bx, by, bw, bh);
        part.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        Insets bi = frame().getBorderInsets(c);
        int top = bi.top;
        int bottom = bi.bottom;
        if (hasTitle()) {
            Font font = getFont(c);
            int th = lineHeight(metrics(c, font), font);
            switch (position()) {
                case ABOVE_TOP:
                    top += th;
                    break;
                case BELOW_TOP:
                    top += th + TEXT_SPACING;
                    break;
                case ABOVE_BOTTOM:
                    bottom += th + TEXT_SPACING;
                    break;
                case BOTTOM:
                    bottom = Math.max(bottom, th);
                    break;
                case BELOW_BOTTOM:
                    bottom += th;
                    break;
                default:
                    top = Math.max(top, th);
                    break;
            }
        }
        int pad = EDGE_SPACING + TEXT_SPACING;
        insets.set(top + pad, bi.left + pad, bottom + pad, bi.right + pad);
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    public String getTitle() {
        return title;
    }

    /// The border drawn around the title. Without a look and feel there is
    /// no default to hand out, so this is null until one is set; painting
    /// then uses a lowered etched border.
    public Border getBorder() {
        return border;
    }

    public int getTitlePosition() {
        return titlePosition;
    }

    public int getTitleJustification() {
        return titleJustification;
    }

    /// The font set on this border, or null when the title takes the font
    /// of the component it is painted on.
    public Font getTitleFont() {
        return titleFont;
    }

    /// The colour set on this border, or null when the title takes the
    /// foreground of the component it is painted on.
    public Color getTitleColor() {
        return titleColor;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setBorder(Border border) {
        this.border = border;
    }

    public void setTitlePosition(int titlePosition) {
        checkPosition(titlePosition);
        this.titlePosition = titlePosition;
    }

    public void setTitleJustification(int titleJustification) {
        checkJustification(titleJustification);
        this.titleJustification = titleJustification;
    }

    public void setTitleFont(Font titleFont) {
        this.titleFont = titleFont;
    }

    public void setTitleColor(Color titleColor) {
        this.titleColor = titleColor;
    }

    public Dimension getMinimumSize(Component c) {
        Insets in = getBorderInsets(c);
        Dimension min = new Dimension(in.right + in.left, in.top + in.bottom);
        if (hasTitle()) {
            Font font = getFont(c);
            int tw = textWidth(metrics(c, font), font);
            int pos = position();
            if (pos == ABOVE_TOP || pos == BELOW_BOTTOM) {
                if (min.width < tw) {
                    min.width = tw;
                }
            } else {
                min.width += tw;
            }
        }
        return min;
    }

    protected Font getFont(Component c) {
        if (titleFont != null) {
            return titleFont;
        }
        Font f = c == null ? null : c.getFont();
        return f != null ? f : new Font("Dialog", Font.PLAIN, 12);
    }

    private Color getColor(Component c) {
        if (titleColor != null) {
            return titleColor;
        }
        return c == null ? null : c.getForeground();
    }
}
