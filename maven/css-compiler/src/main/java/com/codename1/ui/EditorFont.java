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

package com.codename1.ui;

import com.codename1.ui.*;
import com.codename1.ui.Font;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;

/**
 * Font object for creation within the Theme Creator which stores all the fallback
 * scenarios for fonts.
 *
 * @author Shai Almog
 */
public class EditorFont extends Font {
    private static Map<Integer, String> face = new HashMap<Integer, String>();
    private static Map<Integer, String> size = new HashMap<Integer, String>();
    private static Map<Integer, String> style = new HashMap<Integer, String>();
    public static final Object[] RENDERING_HINTS = {
        RenderingHints.VALUE_TEXT_ANTIALIAS_OFF,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
        RenderingHints.VALUE_TEXT_ANTIALIAS_GASP,
        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR,
        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB,
        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR,
        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB
    };

    static {
        face.put(Font.FACE_MONOSPACE, "MONOSPACE");
        face.put(Font.FACE_PROPORTIONAL, "PROPORTIONAL");
        face.put(Font.FACE_SYSTEM, "SYSTEM");
        size.put(Font.SIZE_LARGE, "LARGE");
        size.put(Font.SIZE_MEDIUM, "MEDIUM");
        size.put(Font.SIZE_SMALL, "SMALL");
        style.put(Font.STYLE_BOLD, "BOLD");
        style.put(Font.STYLE_ITALIC, "ITALIC");
        style.put(Font.STYLE_BOLD | Font.STYLE_ITALIC, "BOLD ITALIC");
        style.put(Font.STYLE_PLAIN, "PLAIN");
        style.put(Font.STYLE_UNDERLINED, "UNDERLINED");
    }

    private Font systemFallback;
    private boolean includesBitmap;
    private byte[] truetypeFont;
    private String lookupFont;
    private Font bestFont;
    private Object bitmapAntialiasing;
    private String bitmapCharset;

    public EditorFont(Font systemFont, byte[] truetypeFont, String lookupFont, boolean includesBitmap, Object bitmapAntialiasing, String bitmapCharset) {
        this.systemFallback = systemFont;
        this.truetypeFont = truetypeFont;
        this.includesBitmap = includesBitmap;
        this.bitmapAntialiasing = bitmapAntialiasing;
        this.bitmapCharset = bitmapCharset;
        this.lookupFont = lookupFont;
        if(truetypeFont != null) {
            try {
                java.awt.Font internal = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, new ByteArrayInputStream(truetypeFont));
                bestFont = new Font(internal);
                return;
            } catch(Exception err) {
                err.printStackTrace();
                JOptionPane.showMessageDialog(java.awt.Frame.getFrames()[0], "Error creating font: " + err, "TTF Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        if(lookupFont != null) {
            bestFont = Font.create(lookupFont.split(";")[0]);
            if(bestFont != null) {
                return;
            }
        }

        bestFont = systemFont;
    }

    public String getBitmapCharset() {
        return bitmapCharset;
    }
    
    public int getFace(){
        return systemFallback.getFace();
    }

    public Object getBitmapAntiAliasing() {
        return bitmapAntialiasing;
    }

    public int getSize(){
        return systemFallback.getSize();
    }

    public int getStyle() {
        return systemFallback.getStyle();
    }

    public boolean equals(Object o) {
        if(o instanceof EditorFont) {
            EditorFont f = (EditorFont)o;
            return f.includesBitmap == includesBitmap && f.bitmapAntialiasing == bitmapAntialiasing &&
                    f.bitmapCharset == bitmapCharset && f.lookupFont == lookupFont &&
                    f.systemFallback.getSize() == systemFallback.getSize() && f.systemFallback.getFace() == systemFallback.getFace() &&
                    f.systemFallback.getStyle() == systemFallback.getStyle();
        }
        return false;
    }

    public int getRenderingHint() {
        for(int i = 0 ; i < RENDERING_HINTS.length ; i++) {
            if(RENDERING_HINTS[i] == bitmapAntialiasing) {
                return i;
            }
        }
        return -1;
    }

    public int charWidth(char ch) {
        return bestFont.charWidth(ch);
    }


    public int charsWidth(char[] ch, int offset, int length){
        return bestFont.charsWidth(ch, offset, length);
    }

    public int substringWidth(String str, int offset, int len){
        return bestFont.substringWidth(str, offset, len);
    }

    public int stringWidth(String str){
        return bestFont.stringWidth(str);
    }

    public int getHeight() {
        return bestFont.getHeight();
    }

    void drawChar(Graphics g, char character, int x, int y) {
        if(!(bestFont instanceof CustomFont)) {
            g.setFont(bestFont);
            g.drawChar(character, x, y);
        } else {
            bestFont.drawChar(g, character, x, y);
        }
    }

    void drawChars(Graphics g, char[] data, int offset, int length, int x, int y) {
        if(!(bestFont instanceof CustomFont)) {
            g.setFont(bestFont);
            g.drawChars(data, offset, length, x, y);
        } else {
            bestFont.drawChars(g, data, offset, length, x, y);
        }
    }

    /**
     * @return the systemFallback
     */
    public Font getSystemFallback() {
        return systemFallback;
    }

    /**
     * @return the bitmapFont
     */
    public Font getBitmapFont() {
        Font bitmapFont = Font.getBitmapFont(lookupFont);
        if(bitmapFont != null) {
            return bitmapFont;
        }

        BufferedImage image = new BufferedImage(5000, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D)image.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, bitmapAntialiasing);
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.setColor(new Color(0xff0000));
        g2d.setFont(java.awt.Font.decode(lookupFont.split(";")[0]));
        FontMetrics metrics = g2d.getFontMetrics();
        FontRenderContext context = g2d.getFontRenderContext();
        int height = (int)Math.ceil(metrics.getMaxDescent() + metrics.getMaxAscent());
        int baseline = (int)Math.ceil(metrics.getMaxAscent());
        String charsetStr = bitmapCharset;
        int[] offsets = new int[charsetStr.length()];
        int[] widths = new int[offsets.length];
        int currentOffset = 0;
        for(int iter = 0 ; iter < charsetStr.length() ; iter++) {
            offsets[iter] = currentOffset;
            String currentChar = charsetStr.substring(iter, iter + 1);
            g2d.drawString(currentChar, currentOffset, baseline);
            Rectangle2D rect = g2d.getFont().getStringBounds(currentChar, context);
            widths[iter] = (int)Math.ceil(rect.getWidth());

            // max advance works but it makes a HUGE image in terms of width which
            // occupies more ram
            if(g2d.getFont().isItalic()) {
                currentOffset += metrics.getMaxAdvance();
            } else {
                currentOffset += widths[iter] + 1;
            }
        }

        g2d.dispose();
        BufferedImage shrunk = new BufferedImage(currentOffset, height, BufferedImage.TYPE_INT_RGB);
        g2d = (Graphics2D)shrunk.getGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        int[] rgb = new int[shrunk.getWidth() * shrunk.getHeight()];
        shrunk.getRGB(0, 0, shrunk.getWidth(), shrunk.getHeight(), rgb, 0, shrunk.getWidth());
        com.codename1.ui.Image bitmap = com.codename1.ui.Image.createImage(rgb, shrunk.getWidth(), shrunk.getHeight());

        return com.codename1.ui.Font.createBitmapFont(lookupFont, bitmap, offsets, widths, charsetStr);
    }

    public int getBitmapFontRAMUsage() {
        BufferedImage image = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D)image.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, bitmapAntialiasing);
        g2d.setFont(java.awt.Font.decode(lookupFont));
        FontMetrics metrics = g2d.getFontMetrics();
        FontRenderContext context = g2d.getFontRenderContext();
        int baseline = (int)Math.ceil(metrics.getMaxAscent());
        int[] offsets = new int[bitmapCharset.length()];
        int[] widths = new int[offsets.length];
        int currentOffset = 0;
        for(int iter = 0 ; iter < bitmapCharset.length() ; iter++) {
            offsets[iter] = currentOffset;
            String currentChar = bitmapCharset.substring(iter, iter + 1);
            g2d.drawString(currentChar, currentOffset, baseline);
            Rectangle2D rect = g2d.getFont().getStringBounds(currentChar, context);
            widths[iter] = (int)Math.ceil(rect.getWidth());

            // max advance works but it makes a HUGE image in terms of width which
            // occupies more ram
            if(g2d.getFont().isItalic()) {
                currentOffset += metrics.getMaxAdvance();
            } else {
                currentOffset += widths[iter] + 1;
            }
        }
        g2d.dispose();
        return currentOffset * getHeight() * 4;
    }

    /**
     * @return the truetypeFont
     */
    public byte[] getTruetypeFont() {
        return truetypeFont;
    }

    /**
     * @return the lookupFont
     */
    public String getLookupFont() {
        return lookupFont;
    }

    public void setLookupFont(String lookupFont) {
        this.lookupFont = lookupFont;
    }

    @Override
    public Object getNativeFont() {
        return bestFont.getNativeFont();
    }

    private String systemString() {
        return face.get(systemFallback.getFace()) + ", " + style.get(systemFallback.getStyle()) + ", " + size.get(systemFallback.getSize());
    }

    public String toString() {
        if(lookupFont != null) {
            return lookupFont + " : " + systemString();
        }
        return systemString();
    }

    /**
     * @return the includesBitmap
     */
    public boolean isIncludesBitmap() {
        return includesBitmap;
    }
}
