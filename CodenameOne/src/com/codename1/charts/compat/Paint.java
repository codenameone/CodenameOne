/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.codename1.charts.compat;


import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.Rectangle2D;

/**
 *
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
            for ( int i=0; i<chars.length && i<widths.length; i++){
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
            for ( int i=start; (measureForwards && i<chars.length) || (!measureForwards && i>=0) ; i+=inc){
                tmp = f.charWidth(chars[i]);
                if ( currWidth + tmp > maxWidth ){
                    return i;
                }
                if ( measuredWidth != null && i < measuredWidth.length ){
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
            for ( int i=start; i<chars.length && i<start+count; i++){
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
            for ( int i=start; i< chars.length && i < start+count; i++){
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
