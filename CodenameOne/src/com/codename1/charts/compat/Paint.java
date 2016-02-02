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
package com.codename1.charts.compat;


import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.Rectangle2D;

/**
 * DO NOT USE.  Compatibility class used internally by the Charts API.
 * @author shannah
 * @deprecated
 */
public class Paint {

    
    static Graphics g;
    private boolean antiAlias;
    private Font typeface = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private int strokeCap = Cap.BUTT;
    private int strokeJoin = Join.BEVEL;
    private float strokeMiter = 1f;
    
    private Style style = Style.STROKE;
    private float strokeWidth = 1f;
    
    private int color;
    private int align;
    private float textSize = 12f;
    
    public void getTextWidths(String text, float[] widths) {
        Font f = getTypeface();
        if ( f != null ){
            char[] chars = text.toCharArray();
            int clen = chars.length;
            int wlen = widths.length;
            for ( int i=0; i<clen && i<wlen; i++){
                widths[i] = f.charWidth(chars[i]);
            }
        } else {
           throw new RuntimeException("Faild to get cn1 font");
        }
        
    }

    public int breakText(String text, boolean measureForwards, float maxWidth, float[] measuredWidth) {
        char[] chars = text.toCharArray();
        Font f = getTypeface();
        float tmp = 0;
        if ( f != null ){
            int start = measureForwards ? 0 : chars.length-1;
            int inc = measureForwards ? 1 : -1;
            
            float currWidth = 0f;
            int clen = chars.length;
            int wlen = measuredWidth != null ? measuredWidth.length : -1;
            for ( int i=start; (measureForwards && i<clen) || (!measureForwards && i>=0) ; i+=inc){
                tmp = f.charWidth(chars[i]);
                if ( currWidth + tmp > maxWidth ){
                    return i;
                }
                if (i < wlen ){
                    measuredWidth[i] = tmp;
                }
                currWidth += tmp;
            }
            
        } else {
            throw new RuntimeException("Failed to get font");
        }
        return chars.length;
    }

    public void getTextBounds(String string, int start, int count, Rectangle2D rect) {
        Font f = getTypeface();
        if ( f != null ){
            getCN1TextBounds(string, start, count, rect);
        } else {
            throw new RuntimeException("Failed to get font");
            
        }
    }
    
    void getCN1TextBounds(String string, int start, int count, Rectangle2D rect){
        Font f = getTypeface();
        if ( f != null ){
            int w = f.substringWidth(string, start, count);
            int h = f.getHeight();
            rect.setBounds(0, 0, w, h);
        }
    }
    
    

    
    float measureTextHeight(char[] chars, int start, int count){
        Font f = getTypeface();
        float h = 0f;
        if ( f != null ){
            int clen = chars.length;
            for ( int i=start; i<clen && i<start+count; i++){
                float nh = f.getHeight();
                h = nh > h ? nh : h;
            }
        } else {
            throw new RuntimeException("Failed to get font");
        }
        return h;
    }
    
    
    
    public float measureText(String newText) {
        return measureText(newText.toCharArray(), 0, newText.length());
    }
    
    public float measureText(char[] chars, int start, int count){
        float out = 0f;
        Font f = getTypeface();
        if ( f != null ){
            int clen = chars.length;
            for ( int i=start; i< clen && i < start+count; i++){
                out += f.charWidth(chars[i]);
            }
        } else {
            throw new RuntimeException("Failed to get font");
        }
        return out;
    }
    
    

    public void setAntiAlias(boolean antialiasing) {
        this.antiAlias = antialiasing;
    }

    public Font getTypeface() {
        return typeface;
    }

    public void setTypeface(Font textTypeface) {
        typeface = textTypeface;
    }

    

    public int getStrokeCap() {
        return strokeCap;
    }

    public int getStrokeJoin() {
        return strokeJoin;
    }

    public float getStrokeMiter() {
        return strokeMiter;
    }

    

    public Style getStyle() {
        return style;
    }

    public void setStrokeCap(int cap) {
        strokeCap = cap;
    }

    public void setStrokeJoin(int join) {
        strokeJoin = join;
    }

    public void setStrokeMiter(float miter) {
        strokeMiter = miter;
    }

    

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float i) {
        strokeWidth = i;
    }

    
    
    public static enum Style {
        FILL,
        FILL_AND_STROKE,
        STROKE
    }
    
    public static class Align {
        public static final int CENTER=Component.CENTER;
        public static final int LEFT=Component.LEFT;
        public static final int RIGHT=Component.RIGHT;
    }
    
    public static class Cap {
        public static final int BUTT=Stroke.CAP_BUTT;
        public static final int ROUND=Stroke.CAP_ROUND;
        public static final int SQUARE=Stroke.CAP_SQUARE;
    }
    
    public static class Join {
        public static final int BEVEL=Stroke.JOIN_BEVEL;
        public static final int MITER=Stroke.JOIN_MITER;
        public static final int ROUND=Stroke.JOIN_ROUND;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
    
    public void setStyle(Style style){
        this.style = style;
    }
    
    public void setTextAlign(int align){
        this.align = align;
    }
    
    public int getTextAlign(){
        return this.align;
    }
    
    public void setTextSize(float size){
        textSize = size;
        if ( this.typeface != null ){
            this.typeface  = this.typeface.derive(size, Font.STYLE_PLAIN);
            
        }
    }
    
    public float getTextSize(){
        return textSize;
    }
    
    
    public String toString(){
        return "Paint[ color:"+color+", align:"+align+", textSize:"+textSize+", style:"+style+", strokeWidth:"+strokeWidth+",, strokeMiter:"+strokeMiter+", strokeJoin:"+strokeJoin+" strokeCap:"+strokeCap+"]";
    }
  
    
    
}
