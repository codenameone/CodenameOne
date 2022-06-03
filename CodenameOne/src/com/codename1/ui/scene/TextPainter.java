/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.ui.scene;

import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Style;

/**
 * A painter for painting text into a Node.
 * @author shannah
 */
public class TextPainter implements NodePainter {
    private String text;
    private int vAlign=Component.CENTER;
    
    /**
     * Creates a new TextPainter with the given text and vertical alignment.
     * @param text The text to paint.
     * @param valign The vertical alignment of the text.  One of {@link Component#CENTER}, {@link Component#TOP}, {@link Component#BOTTOM}.
     */
    public TextPainter(String text, int valign) {
        this.text = text;
        this.vAlign = valign;
    }

    /**
     * Paints the text onto the provided graphics context.
     * @param g The graphics to paint onto.
     * @param bounds The bounding rect.
     * @param node The node whose content we are painting.
     */
    @Override
    public void paint(Graphics g, Rectangle bounds, Node node) {
        Style style = node.getStyle();
        if (style == null) {
            return;
        }
        Font font = style.getFont();
        if (font == null) {
            font = Font.getDefaultFont();
        }
        int textWidth = font.stringWidth(text);
        int textHeight = font.getHeight();
        int innerX = bounds.getX() + style.getPaddingLeft(false);
        int innerY = bounds.getY() + style.getPaddingTop();
        int innerW = bounds.getWidth() - style.getHorizontalPadding();
        int innerH = bounds.getHeight() - style.getVerticalPadding();
        
        int x = innerX;
        int y = innerY;
        switch (style.getAlignment()) {
            case Component.CENTER:
                x = innerX + innerW/2 - textWidth/2;
                break;
            case Component.RIGHT:
                x = innerX + innerW - textWidth;
                break;
                
        }
        switch (vAlign) {
            case Component.CENTER:
                y = innerY + innerH/2 - textHeight/2;
                break;
            case Component.BOTTOM:
                y = innerY+ innerH - textHeight;
                break;
        }
        g.setFont(font);
        g.setColor(style.getFgColor());
        int alpha = g.concatenateAlpha(style.getFgAlpha());
        g.drawString(text, x, y);
        g.setAlpha(alpha);
    }

    /**
     * The text of this painter.
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text of this painter
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the vertical alignment of this text.  One of {@link Component#CENTER}, {@link Component#TOP}, {@link Component#BOTTOM}.
     * @return the vAlign 
     */
    public int getvAlign() {
        return vAlign;
    }

    /**
     * Sets the vertical alignment. One of {@link Component#CENTER}, {@link Component#TOP}, {@link Component#BOTTOM}.
     * @param vAlign the vAlign to set
     */
    public void setvAlign(int vAlign) {
        this.vAlign = vAlign;
    }
    
}
