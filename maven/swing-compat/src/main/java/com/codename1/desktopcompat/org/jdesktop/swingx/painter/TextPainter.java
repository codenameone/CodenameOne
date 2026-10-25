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
package com.codename1.desktopcompat.org.jdesktop.swingx.painter;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Paint;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.Shape;
import com.codename1.desktopcompat.javax.swing.AbstractButton;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.text.JTextComponent;

/// Draws one line of text, placed by the layout properties.
///
/// A painter without a text draws the text of the label, button or text
/// component it paints for; without a font it uses that component's; and
/// without a fill paint that component's foreground.
///
/// ## What differs from SwingX
///
///  - The text is drawn as a string, not as an outline: the border paint
///    and width are ignored, and [#provideShape] answers the rectangle
///    the text takes, not its glyphs.
///  - A gradient colors the text only where the port can fill text with
///    one; elsewhere its first color is used.
public class TextPainter extends AbstractAreaPainter<Object> {

    private String text = "";
    private Font font;

    public TextPainter() {
        this("");
    }

    public TextPainter(String text) {
        this(text, null, null);
    }

    public TextPainter(String text, Font font) {
        this(text, font, null);
    }

    public TextPainter(String text, Paint paint) {
        this(text, null, paint);
    }

    public TextPainter(String text, Font font, Paint paint) {
        super(paint);
        this.text = text;
        this.font = font;
    }

    public void setFont(Font f) {
        Font old = font;
        font = f;
        setDirty(true);
        firePropertyChange("font", old, f);
    }

    public Font getFont() {
        return font;
    }

    public void setText(String text) {
        String old = this.text;
        this.text = text == null ? "" : text;
        setDirty(true);
        firePropertyChange("text", old, this.text);
    }

    public String getText() {
        return text;
    }

    private Font fontFor(Graphics2D g, Object component) {
        Font f = font;
        if (f == null && component instanceof Component) {
            f = ((Component) component).getFont();
        }
        if (f == null) {
            f = g.getFont();
        }
        return f;
    }

    private String textFor(Object component) {
        String t = text;
        if (t == null || t.length() == 0) {
            if (component instanceof JTextComponent) {
                t = ((JTextComponent) component).getText();
            } else if (component instanceof JLabel) {
                t = ((JLabel) component).getText();
            } else if (component instanceof AbstractButton) {
                t = ((AbstractButton) component).getText();
            }
        }
        return t;
    }

    private Rectangle bounds(Graphics2D g, Object component, int width, int height, String t, Font f) {
        FontMetrics fm = f != null ? g.getFontMetrics(f) : g.getFontMetrics();
        return calculateLayout(fm.stringWidth(t), fm.getHeight(), width, height);
    }

    @Override
    protected void doPaint(Graphics2D g, Object component, int width, int height) {
        String t = textFor(component);
        if (t == null || t.length() == 0) {
            return;
        }
        Font f = fontFor(g, component);
        if (f != null) {
            g.setFont(f);
        }
        Paint paint = getFillPaint();
        if (paint == null && component instanceof Component) {
            paint = ((Component) component).getForeground();
        }
        if (paint == null) {
            paint = Color.BLACK;
        }
        Rectangle r = bounds(g, component, width, height, t, f);
        g.setPaint(cn1Fitted(paint, width, height));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(t, r.x, r.y + fm.getAscent());
    }

    /// The rectangle the text takes.
    @Override
    protected Shape provideShape(Graphics2D g, Object comp, int width, int height) {
        String t = textFor(comp);
        if (t == null) {
            t = "";
        }
        return bounds(g, comp, width, height, t, fontFor(g, comp));
    }
}
