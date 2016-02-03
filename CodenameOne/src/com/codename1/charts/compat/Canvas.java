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


import com.codename1.io.Log;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Image;
import com.codename1.ui.Stroke;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Rectangle2D;
import com.codename1.ui.geom.Shape;

import com.codename1.charts.compat.GradientDrawable.Orientation;
import com.codename1.charts.util.ColorUtil;

/**
 * An internal compatibility class for use by the Charts library.  Since the 
 * library was ported from an Android library, it made use of Canvas and Paint,
 * so rather than rewriting all of this, we added a Canvas and Paint compatibility
 * layer.
 * 
 * **DO NOT USE DIRECTLY**
 * 
 * @author shannah
 * @deprecated
 */
public class Canvas  {
    
    public com.codename1.ui.Graphics g;
    
    public Rectangle bounds = null;
    public int absoluteX = 0;
    public int absoluteY = 0;
   
    public Canvas(){
        Display d = Display.getInstance();
        bounds = new Rectangle(0,0, d.getDisplayWidth(), d.getDisplayHeight());
        absoluteX = bounds.getX();
        absoluteY = bounds.getY();
        
    }

    
    
    public void getClipBounds(Rectangle mRect) {
        int [] bounds = g.getClip();
        mRect.setBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
        
    }

    
    
    private void applyPaint(Paint paint){
        applyPaint(paint, false);
    }
    
    
    private void applyPaint(Paint paint, boolean forText){
        //Log.p("Applyingn paint : "+paint);
        g.setColor(paint.getColor());
        int alpha = ColorUtil.alpha(paint.getColor());
        g.setAlpha(alpha);
        if ( forText ){
            Font typeFace = paint.getTypeface();
            if ( typeFace != null ){
                
                if ( typeFace.getSize() != (int)paint.getTextSize()){
                    typeFace = typeFace.derive(paint.getTextSize(), Font.STYLE_PLAIN);

                }
                g.setFont(typeFace);
                
            } else {
                g.setFont(null);
            }
        }
        
        
    }
    
    
    
    
    public void drawRect(float left, float top, float right, float bottom, Paint paint) {
        applyPaint(paint);
        Paint.Style style = paint.getStyle();
        if ( Paint.Style.FILL.equals(style)){
            //Log.p("Filling it");
            g.fillRect((int)left, (int)top, (int)right-(int)left, (int)bottom-(int)top);
        } else if ( Paint.Style.STROKE.equals(style)){
            g.drawRect((int)left, (int)top, (int)right-(int)left, (int)bottom-(int)top);
        } else if ( Paint.Style.FILL_AND_STROKE.equals(style)){
            g.fillRect((int)left, (int)top, (int)right-(int)left, (int)bottom-(int)top);
            //g.drawRect((int)left+bounds.getX(), (int)top+bounds.getY(), (int)right-(int)left, (int)bottom-(int)top);
        }
            
        
    }

    public void drawText(String string, float x, float y, Paint paint) {
        applyPaint(paint, true);
        int offX = 0;
        int offY = 0;
        if ( paint.getTextAlign() == Component.CENTER){
            offX = -g.getFont().stringWidth(string)/2;
        } else if ( paint.getTextAlign() == Component.RIGHT ){
            offX = -g.getFont().stringWidth(string);
        }
        int h = g.getFont().getAscent();
        
        g.drawString(string, (int)x+offX, (int)y-h+offY);
    }

    public int getHeight() {
        if ( bounds != null ){
            return bounds.getHeight();
        }
        return g.getClipHeight();
    }

    public int getWidth() {
        if ( bounds != null ){
            return bounds.getWidth();
        }
        return g.getClipWidth();
    }

    

    private Stroke getStroke(Paint paint){
        return new Stroke(
                paint.getStrokeWidth(), 
                paint.getStrokeCap(), 
                paint.getStrokeJoin(), 
                paint.getStrokeMiter()
        );
        
    }
    
    
    
    public void drawPath(Shape p, Paint paint){
        
        applyPaint(paint);
        Paint.Style style = paint.getStyle();
        if ( style.equals(Paint.Style.FILL)){
            g.fillShape(p);
            //g.drawShape(p, getStroke(paint));
        } else if ( style.equals(Paint.Style.STROKE)){
            g.drawShape(p, getStroke(paint));
        } else if ( style.equals(Paint.Style.FILL_AND_STROKE)){
            g.fillShape(p);
            g.drawShape(p, getStroke(paint));
        }
        
    }
    
    public void drawLine(float x1, float y1, float x2, float y2, Paint paint) {
        applyPaint(paint);
        g.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
    }

    public void rotate(float angle, float x, float y) {
        //Log.p("Rotating by angle "+angle);
        Transform t = g.getTransform();
        t.rotate((float)(angle*Math.PI/180.0), x+absoluteX-bounds.getX(), y+absoluteY-bounds.getY());
        g.setTransform(t);
        
    }

    public void scale(float x, float y) {
        Transform t = g.getTransform();
        t.translate(bounds.getX(), bounds.getY());
        t.scale(x, y);
        t.translate(-bounds.getX(), -bounds.getY());
       g.setTransform(t);
    }

    public void translate(float x, float y) {
        Transform t = g.getTransform();
        t.translate(x, y);
        g.setTransform(t);
    }

    public void drawCircle(float cx, float cy, float r, Paint paint) {
        drawArc(new Rectangle2D(cx-r, cy-r, 2*r, 2*r), 0, 360, true, paint);
 
    }

    
    public void drawArc(Rectangle2D oval, float currentAngle, float sweepAngle, boolean useCenter, Paint paint) {
        
        applyPaint(paint);
        Paint.Style style = paint.getStyle();
        if ( Paint.Style.FILL.equals(style)){
            g.fillArc((int)Math.round(oval.getX()), (int)Math.round(oval.getY()), (int)Math.round(oval.getWidth()), (int)Math.round(oval.getHeight()), -(int)Math.floor(currentAngle), -(int)Math.ceil(sweepAngle));
            
        } else if ( Paint.Style.STROKE.equals(style)){
            g.drawArc((int)Math.round(oval.getX()), (int)Math.round(oval.getY()), (int)Math.round(oval.getWidth()), (int)Math.round(oval.getHeight()), -(int)Math.floor(currentAngle), -(int)Math.ceil(sweepAngle));
            
        } else if ( Paint.Style.FILL_AND_STROKE.equals(style)){
            g.fillArc((int)Math.round(oval.getX()), (int)Math.round(oval.getY()), (int)Math.round(oval.getWidth()), (int)Math.round(oval.getHeight()), -(int)Math.floor(currentAngle), -(int)Math.ceil(sweepAngle));
            g.drawArc((int)Math.round(oval.getX()), (int)Math.round(oval.getY()), (int)Math.round(oval.getWidth()), (int)Math.round(oval.getHeight()), -(int)Math.floor(currentAngle), -(int)Math.ceil(sweepAngle));
            
        }
        
    }

    
    public void drawPoint(Float get, Float get0, Paint paint) {
        throw new RuntimeException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void drawRoundRect(Rectangle2D rect, float rx, float ry, Paint mPaint) {
        applyPaint(mPaint);
        Paint.Style style = mPaint.getStyle();
        if ( Paint.Style.FILL.equals(style)){
            g.fillRoundRect((int)rect.getX(), (int)rect.getY(), (int)(rect.getWidth()), (int)(rect.getHeight()), (int)rx, (int)ry);
        } else if ( Paint.Style.STROKE.equals(style)){
            g.drawRoundRect((int)rect.getX(), (int)rect.getY(), (int)(rect.getWidth()), (int)(rect.getHeight()), (int)rx, (int)ry);
        } else if ( Paint.Style.FILL_AND_STROKE.equals(style)){
            g.fillRoundRect((int)rect.getX(), (int)rect.getY(), (int)(rect.getWidth()), (int)(rect.getHeight()), (int)rx, (int)ry);
            g.drawRoundRect((int)rect.getX(), (int)rect.getY(), (int)(getWidth()), (int)(rect.getHeight()), (int)rx, (int)ry);
        }
        g.drawRoundRect((int)rect.getX(), (int)rect.getY(), (int)(rect.getWidth()), (int)(rect.getHeight()), (int)rx, (int)ry);
        
        
    }

    public void drawBitmap(Image img, float left, float top, Paint paint) {
        g.drawImage(img, (int)left, (int)top);
    }
    
    void drawGradient(GradientDrawable gradient){
        Orientation o = gradient.orientation;
        Rectangle r = gradient.bounds;
        int[] colors = gradient.colors;
        int clen = colors.length;
               
        if ( Orientation.TOP_BOTTOM.equals(o) || Orientation.BOTTOM_TOP.equals(o)){

           if ( Orientation.BOTTOM_TOP.equals(o) ){
               colors = new int[clen];
               
               for ( int i=0; i<clen; i++){
               
                   colors[i] = gradient.colors[clen-i-1];
               }
           }
           g.fillLinearGradient(colors[0], colors[clen-1], r.getX(), r.getY(), r.getWidth(), r.getHeight(), false);
           
        } else if ( Orientation.LEFT_RIGHT.equals(o)){
           g.fillLinearGradient(gradient.colors[0], gradient.colors[clen-1], r.getX(), r.getY(), r.getWidth(), r.getHeight(), true);
        } else {
           Log.p("Gradient with type "+o+" not implemented yet.  Just filling solid rect");
           g.setColor(gradient.colors[0]);
           g.fillRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }
        
        
        
    }
    
}
