/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Font class implementing the new TTF functionality from Codename One where applicable
 * 
 * @author Shai Almog
 */
public class EditorTTFFont extends Font {
    private Font systemFont;
    private File fontFile;
    private Font actualFont;
    private int sizeSetting;
    private float actualSize;
    private String nativeFontName;
    public EditorTTFFont(File fontFile, int sizeSetting, float actualSize, Font systemFont) {
        this.fontFile = fontFile;
        this.sizeSetting = sizeSetting;
        this.actualSize = actualSize;
        this.systemFont = systemFont;
        refresh();
    }
    
    public EditorTTFFont(String nativeFontName, int sizeSetting, float actualSize, Font systemFont) {
        this.nativeFontName = nativeFontName;
        this.sizeSetting = sizeSetting;
        this.actualSize = actualSize;
        this.systemFont = systemFont;
        refresh();
    }

    public void refresh() {
        if(fontFile != null && fontFile.exists() || nativeFontName != null) {
            try {
                java.awt.Font f;
                if(nativeFontName != null) {
                    String res; 
                    switch(nativeFontName) {
                        case "native:MainThin":
                            res = "Thin";
                            break;

                        case "native:MainLight":
                            res = "Light";
                            break;

                        case "native:MainRegular":
                            res = "Medium";
                            break;

                        case "native:MainBold":
                            res = "Bold";
                            break;

                        case "native:MainBlack":
                            res = "Black";
                            break;

                        case "native:ItalicThin":
                            res = "ThinItalic";
                            break;

                        case "native:ItalicLight": 
                            res = "LightItalic";
                            break;

                        case "native:ItalicRegular":
                            res = "Italic";
                            break;

                        case "native:ItalicBold":
                            res = "BoldItalic";
                            break;

                        case "native:ItalicBlack":
                            res = "BlackItalic";
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported native font type: " + nativeFontName);
                    }
                    InputStream is = getClass().getResourceAsStream("/com/codename1/impl/javase/Roboto-" + res + ".ttf");
                    try {
                        f = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, is);
                        is.close();
                    } catch(Exception err) {
                        err.printStackTrace();
                        return;
                    }
                } else {
                    f = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fontFile);                    
                }
                switch(sizeSetting) {
                    case 0:
                        f = f.deriveFont((float)
                                com.codename1.ui.Font.createSystemFont(com.codename1.ui.Font.FACE_SYSTEM, 
                                com.codename1.ui.Font.STYLE_PLAIN, com.codename1.ui.Font.SIZE_SMALL).getHeight());
                        break;
                    case 1:
                        f = f.deriveFont((float)
                                com.codename1.ui.Font.createSystemFont(com.codename1.ui.Font.FACE_SYSTEM, 
                                com.codename1.ui.Font.STYLE_PLAIN, com.codename1.ui.Font.SIZE_MEDIUM).getHeight());
                        break;
                    case 2:
                        f = f.deriveFont((float)
                                com.codename1.ui.Font.createSystemFont(com.codename1.ui.Font.FACE_SYSTEM, 
                                com.codename1.ui.Font.STYLE_PLAIN, com.codename1.ui.Font.SIZE_LARGE).getHeight());
                        break;
                    case 3:
                        f = f.deriveFont(Display.getInstance().convertToPixels(Math.round(actualSize * 10), false) / 10.0f);
                        break;
                    default:
                        f = f.deriveFont(actualSize);
                        break;
                }
                actualFont = new com.codename1.ui.Font(f);
            } catch(Throwable t) {
                t.printStackTrace();
            }
        } else {
            actualFont = systemFont;
        }        
    }
    
    /**
     * @return the systemFont
     */
    public Font getSystemFont() {
        return systemFont;
    }

    /**
     * @return the fontFile
     */
    public File getFontFile() {
        return fontFile;
    }

    public String getNativeFontName() {
        return nativeFontName;
    }
    
    /**
     * @return the sizeSetting
     */
    public int getSizeSetting() {
        return sizeSetting;
    }

    /**
     * @return the actualSize
     */
    public float getActualSize() {
        return actualSize;
    }
    
    public int getFace(){
        return systemFont.getFace();
    }

    public int getSize(){
        return systemFont.getSize();
    }

    public int getStyle() {
        return systemFont.getStyle();
    }
    
    public boolean equals(Object o) {
        if(o instanceof EditorTTFFont) {
            EditorTTFFont f = (EditorTTFFont)o;
            if(fontFile == null && nativeFontName == null) {
                return f.systemFont.equals(systemFont) && f.fontFile == null;
            }
            if(nativeFontName != null) {
                return f.systemFont.equals(systemFont) && nativeFontName.equals(f.nativeFontName) && f.actualSize == actualSize && f.sizeSetting == sizeSetting;
            }
            return f.systemFont.equals(systemFont) && fontFile.equals(f.fontFile) && f.actualSize == actualSize && f.sizeSetting == sizeSetting;
        }
        return false;
    }


    public int charWidth(char ch) {
        return actualFont.charWidth(ch);
    }


    public int charsWidth(char[] ch, int offset, int length){
        return actualFont.charsWidth(ch, offset, length);
    }

    public int substringWidth(String str, int offset, int len){
        return actualFont.substringWidth(str, offset, len);
    }

    public int stringWidth(String str){
        return actualFont.stringWidth(str);
    }

    public int getHeight() {
        return actualFont.getHeight();
    }

    void drawChar(Graphics g, char character, int x, int y) {
        actualFont.drawChar(g, character, x, y);
    }

    void drawChars(Graphics g, char[] data, int offset, int length, int x, int y) {
        actualFont.drawChars(g, data, offset, length, x, y);
    }

    @Override
    public Object getNativeFont() {
        return actualFont.getNativeFont();
    }
}
