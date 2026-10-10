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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.org.jdesktop.swingx.painter.Painter;
import com.codename1.desktopcompat.rt.Fonts;
import com.codename1.desktopcompat.rt.MiniHtml;
import com.codename1.desktopcompat.rt.Units;
import java.util.ArrayList;

/// A label that can wrap its text over several lines, draw it rotated,
/// and have its background and its content drawn by painters.
///
/// ## How it is drawn
///
/// A label that neither wraps nor rotates is the Codename One label of
/// [JLabel]. With line wrap or a rotation the label draws the icon and the
/// text itself, in the label's font and foreground, inside the border and
/// the padding the theme gives a label.
///
///  - The background painter draws first, under everything.
///  - A foreground painter, when set, draws *instead of* the icon and
///    text.
///
/// ## Line wrap
///
/// Lines break at spaces and at line feeds. The width they wrap to is the
/// label's current width, or the maximum line span when one is set, and
/// before the label has a width the text is measured unwrapped -- so a
/// wrapping label reports its real height only once it has been laid out,
/// as in SwingX.
///
/// ## What differs from SwingX
///
///  - A text that starts with `<html>` is drawn by [JLabel], which wraps
///    it by itself; it is not rotated.
///  - `JUSTIFY` aligns to the left: words are not spread.
///  - The rotated text keeps its unrotated layout and is turned about the
///    center of the label; the preferred size is the box that holds it.
///  - A painter that changes is not observed: call `repaint()`.
public class JXLabel extends JLabel {

    /// No rotation.
    public static final double NORMAL = 0;
    /// Upside down.
    public static final double INVERTED = Math.PI;
    /// Reading bottom to top.
    public static final double VERTICAL_LEFT = 3 * Math.PI / 2;
    /// Reading top to bottom.
    public static final double VERTICAL_RIGHT = Math.PI / 2;

    /// How the lines of a wrapped text are aligned among themselves.
    public enum TextAlignment {
        LEFT(0),
        CENTER(1),
        RIGHT(2),
        JUSTIFY(3);

        private final int value;

        TextAlignment(int value) {
            this.value = value;
        }

        /// The paragraph alignment constant of the Swing text package
        /// this alignment stands for.
        public int getValue() {
            return value;
        }
    }

    private Painter foregroundPainter;
    private Painter backgroundPainter;
    private boolean paintBorderInsets = true;
    private double textRotation = NORMAL;
    private boolean multiLine;
    private int maxLineSpan = -1;
    private TextAlignment textAlignment = TextAlignment.LEFT;

    public JXLabel() {
        super();
    }

    public JXLabel(Icon image) {
        super(image);
    }

    public JXLabel(Icon image, int horizontalAlignment) {
        super(image, horizontalAlignment);
    }

    public JXLabel(String text) {
        super(text);
    }

    public JXLabel(String text, Icon image, int horizontalAlignment) {
        super(text, image, horizontalAlignment);
    }

    public JXLabel(String text, int horizontalAlignment) {
        super(text, horizontalAlignment);
    }

    // ------------------------------------------------------------ painters

    public final Painter getForegroundPainter() {
        return foregroundPainter;
    }

    /// Sets the painter that draws the label's content in place of the
    /// icon and text; `null` goes back to those.
    public void setForegroundPainter(Painter painter) {
        Painter old = foregroundPainter;
        foregroundPainter = painter;
        firePropertyChange("foregroundPainter", old, painter);
        repaint();
    }

    public void setBackgroundPainter(Painter p) {
        Painter old = backgroundPainter;
        backgroundPainter = p;
        firePropertyChange("backgroundPainter", old, p);
        repaint();
    }

    public final Painter getBackgroundPainter() {
        return backgroundPainter;
    }

    public boolean isPaintBorderInsets() {
        return paintBorderInsets;
    }

    /// Whether the painters draw under the border too.
    public void setPaintBorderInsets(boolean paintBorderInsets) {
        boolean old = this.paintBorderInsets;
        this.paintBorderInsets = paintBorderInsets;
        firePropertyChange("paintBorderInsets", old, paintBorderInsets);
        if (old != paintBorderInsets) {
            repaint();
        }
    }

    // ------------------------------------------------------------ text

    public double getTextRotation() {
        return textRotation;
    }

    /// Sets the angle, in radians and clockwise, the content is turned by.
    public void setTextRotation(double textOrientation) {
        double old = textRotation;
        textRotation = textOrientation;
        if (old != textOrientation) {
            firePropertyChange("textRotation", Double.valueOf(old), Double.valueOf(textOrientation));
            revalidate();
            repaint();
        }
    }

    public int getMaxLineSpan() {
        return maxLineSpan;
    }

    /// Sets the width, in pixels, lines wrap to whatever the label's
    /// width; -1 wraps to the label.
    public void setMaxLineSpan(int maxLineSpan) {
        int old = this.maxLineSpan;
        this.maxLineSpan = maxLineSpan;
        firePropertyChange("maxLineSpan", old, maxLineSpan);
        if (old != maxLineSpan) {
            revalidate();
            repaint();
        }
    }

    public void setLineWrap(boolean b) {
        boolean old = multiLine;
        multiLine = b;
        if (old != b) {
            firePropertyChange("lineWrap", old, b);
            revalidate();
            repaint();
        }
    }

    public boolean isLineWrap() {
        return multiLine;
    }

    public TextAlignment getTextAlignment() {
        return textAlignment;
    }

    /// Sets how wrapped lines are aligned; `null` is to the left.
    public void setTextAlignment(TextAlignment alignment) {
        TextAlignment old = textAlignment;
        textAlignment = alignment == null ? TextAlignment.LEFT : alignment;
        firePropertyChange("textAlignment", old, textAlignment);
        if (old != textAlignment) {
            repaint();
        }
    }

    // ------------------------------------------------------------ layout

    /// Whether the label draws its own text rather than leaving it to the
    /// Codename One label.
    private boolean selfDrawn() {
        return (multiLine || cn1Rotated()) && !MiniHtml.isHtml(getText());
    }

    private boolean cn1Rotated() {
        double full = 2 * Math.PI;
        double a = textRotation % full;
        if (a < 0) {
            a += full;
        }
        return a > 1e-9 && full - a > 1e-9;
    }

    private Font textFont() {
        return getFont();
    }

    /// The border's insets and the padding the theme gives a label.
    private Insets contentInsets() {
        Insets in = getInsets();
        Insets out = new Insets(in.top, in.left, in.bottom, in.right);
        if (com.codename1.ui.Display.isInitialized()) {
            com.codename1.ui.plaf.Style st = cn1Peer().getStyle();
            out.top += Units.toLogicalCeil(st.getPaddingTop());
            out.bottom += Units.toLogicalCeil(st.getPaddingBottom());
            out.left += Units.toLogicalCeil(st.getPaddingLeftNoRTL());
            out.right += Units.toLogicalCeil(st.getPaddingRightNoRTL());
        }
        return out;
    }

    /// The text broken into lines no wider than `limit`; a limit of 0 or
    /// less breaks at line feeds only.
    static ArrayList<String> cn1Wrap(String text, FontMetrics fm, int limit) {
        ArrayList<String> lines = new ArrayList<String>();
        if (text == null || text.length() == 0) {
            return lines;
        }
        int start = 0;
        int n = text.length();
        while (start <= n) {
            int end = text.indexOf('\n', start);
            if (end < 0) {
                end = n;
            }
            wrapParagraph(text.substring(start, end), fm, limit, lines);
            start = end + 1;
        }
        return lines;
    }

    private static void wrapParagraph(String p, FontMetrics fm, int limit, ArrayList<String> lines) {
        if (limit <= 0 || fm.stringWidth(p) <= limit) {
            lines.add(p);
            return;
        }
        StringBuilder line = new StringBuilder();
        int i = 0;
        int n = p.length();
        while (i < n) {
            int space = p.indexOf(' ', i);
            if (space < 0) {
                space = n;
            }
            String word = p.substring(i, space);
            i = space + 1;
            if (line.length() == 0) {
                line.append(word);
            } else if (fm.stringWidth(line.toString() + " " + word) <= limit) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        lines.add(line.toString());
    }

    private Dimension iconSize() {
        Icon ic = getIcon();
        if (ic == null) {
            return new Dimension(0, 0);
        }
        int gap = getText() == null || getText().length() == 0 ? 0 : getIconTextGap();
        return new Dimension(ic.getIconWidth() + gap, ic.getIconHeight());
    }

    /// The width lines wrap to, or 0 for no wrapping.
    private int wrapLimit(Insets in) {
        if (!multiLine) {
            return 0;
        }
        int icon = iconSize().width;
        if (maxLineSpan > 0) {
            return maxLineSpan;
        }
        boolean sideways = Math.abs(Math.sin(textRotation)) > 0.7071;
        int across = sideways ? getHeight() - in.top - in.bottom : getWidth() - in.left - in.right;
        return across > icon ? across - icon : 0;
    }

    /// The size of the unrotated content: icon, gap and text block.
    private Dimension contentSize(ArrayList<String> lines, FontMetrics fm) {
        int tw = 0;
        for (int i = 0; i < lines.size(); i++) {
            tw = Math.max(tw, fm.stringWidth(lines.get(i)));
        }
        int th = lines.size() * fm.getHeight();
        Dimension icon = iconSize();
        return new Dimension(icon.width + tw, Math.max(icon.height, th));
    }

    /// The preferred size of a label that draws itself: the box that
    /// holds its wrapped and rotated content. Otherwise the Codename One
    /// label's.
    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet() || !selfDrawn()) {
            return super.getPreferredSize();
        }
        Insets in = contentInsets();
        FontMetrics fm = Fonts.metrics(textFont());
        Dimension c = contentSize(cn1Wrap(getText(), fm, wrapLimit(in)), fm);
        double sin = Math.abs(Math.sin(textRotation));
        double cos = Math.abs(Math.cos(textRotation));
        int w = (int) Math.ceil(c.width * cos + c.height * sin - 1e-6);
        int h = (int) Math.ceil(c.width * sin + c.height * cos - 1e-6);
        return new Dimension(w + in.left + in.right, h + in.top + in.bottom);
    }

    // ------------------------------------------------------------ paint

    @SuppressWarnings("unchecked")
    private void run(Painter p, Graphics g) {
        if (p == null || !(g instanceof Graphics2D)) {
            return;
        }
        Graphics copy = g.create();
        try {
            if (copy instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) copy;
                int w = getWidth();
                int h = getHeight();
                if (!paintBorderInsets) {
                    Insets i = getInsets();
                    g2.translate(i.left, i.top);
                    w -= i.left + i.right;
                    h -= i.top + i.bottom;
                }
                if (w > 0 && h > 0) {
                    p.paint(g2, this, w, h);
                }
            }
        } finally {
            copy.dispose();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (backgroundPainter == null && foregroundPainter == null && !selfDrawn()) {
            super.paintComponent(g);
            return;
        }
        if (isOpaque() && getBackground() != null) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        run(backgroundPainter, g);
        if (foregroundPainter != null) {
            run(foregroundPainter, g);
        } else if (selfDrawn()) {
            paintContent(g);
        } else {
            // The Codename One label, which draws no background of its own
            // unless the theme gives labels one.
            super.paintComponent(g);
        }
    }

    private void paintContent(Graphics g) {
        Insets in = contentInsets();
        Font f = textFont();
        FontMetrics fm = Fonts.metrics(f);
        ArrayList<String> lines = cn1Wrap(getText(), fm, wrapLimit(in));
        Dimension c = contentSize(lines, fm);
        int availW = getWidth() - in.left - in.right;
        int availH = getHeight() - in.top - in.bottom;
        Graphics copy = g.create();
        try {
            int x;
            int y;
            if (cn1Rotated() && copy instanceof Graphics2D) {
                ((Graphics2D) copy).rotate(textRotation, in.left + availW / 2.0, in.top + availH / 2.0);
                x = in.left + (availW - c.width) / 2;
                y = in.top + (availH - c.height) / 2;
            } else {
                int ha = getHorizontalAlignment();
                x = in.left;
                if (ha == CENTER) {
                    x += (availW - c.width) / 2;
                } else if (ha == RIGHT || ha == TRAILING) {
                    x += availW - c.width;
                }
                int va = getVerticalAlignment();
                y = in.top;
                if (va == CENTER) {
                    y += (availH - c.height) / 2;
                } else if (va == BOTTOM) {
                    y += availH - c.height;
                }
            }
            Icon ic = getIcon();
            if (!isEnabled() && getDisabledIcon() != null) {
                ic = getDisabledIcon();
            }
            Dimension icon = iconSize();
            int textWidth = c.width - icon.width;
            int textX = x;
            int ht = getHorizontalTextPosition();
            if (ic != null) {
                int iconX = x;
                if (ht == LEFT || ht == LEADING) {
                    iconX = x + c.width - ic.getIconWidth();
                } else {
                    textX = x + icon.width;
                }
                ic.paintIcon(this, copy, iconX, y + (c.height - ic.getIconHeight()) / 2);
            }
            copy.setFont(f);
            Color fg = isEnabled() ? getForeground() : Color.GRAY;
            if (fg != null) {
                copy.setColor(fg);
            }
            int lineY = y + (c.height - lines.size() * fm.getHeight()) / 2 + fm.getAscent();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int lx = textX;
                if (textAlignment == TextAlignment.CENTER) {
                    lx += (textWidth - fm.stringWidth(line)) / 2;
                } else if (textAlignment == TextAlignment.RIGHT) {
                    lx += textWidth - fm.stringWidth(line);
                }
                copy.drawString(line, lx, lineY);
                lineY += fm.getHeight();
            }
        } finally {
            copy.dispose();
        }
    }
}
