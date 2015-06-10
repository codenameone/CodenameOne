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
import com.codename1.ui.geom.GeneralPath;
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
            g.drawRect((int)left, (int)top, (int)right, (int)bottom-(int)top);
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
        GeneralPath p = new GeneralPath();
        float cx = 0;
        float cy = 0;
        cx = (float)(oval.getX()+oval.getWidth()/2f);
        cy = (float)(oval.getY()+oval.getHeight()/2f);
        double rx = oval.getWidth()/2.0;
        double ry = oval.getHeight()/2.0;
        double currentAngleRad = Math.toRadians(currentAngle);
        double sweepAngleRad = Math.toRadians(sweepAngle);
        if (useCenter) {
            p.moveTo(cx, cy);
            
            
        } else {
            p.moveTo(cx + rx * Math.cos(currentAngleRad), cy + ry * Math.sin(currentAngleRad));
        }
        p.arc(oval.getX(), oval.getY(), oval.getWidth(), oval.getHeight(), -currentAngleRad, -sweepAngleRad, true);
        if (useCenter) {
            p.lineTo(cx, cy);
        }
        p.closePath();
        drawPath(p, paint);
        
    }

    
    private static void addBezierArcToPath(GeneralPath path, float cx, float cy,
                                          float startX, float startY, float endX, float endY)
    {
        if ( startX != endX || startY != endY ){
            final double ax = startX - cx;
            final double ay = startY - cy;
            final double bx = endX - cx;
            final double by = endY- cy;
            final double q1 = ax * ax + ay * ay;
            final double q2 = q1 + ax * bx + ay * by;
            final double k2 = 4d / 3d * (Math.sqrt(2d * q1 * q2) - q2) / (ax * by - ay * bx);
            final float x2 = (float)(cx + ax - k2 * ay);
            final float y2 = (float)(cy + ay + k2 * ax);
            final float x3 = (float)(cx + bx + k2 * by);
            final float y3 = (float)(cy + by - k2 * bx);
            //Log.p("Curve: "+startX+","+startY+" -> "+x2+","+y2+" -> "+x3+","+y3+" -> "+endX+","+endY);

            path.curveTo(x2, y2, x3, y3, endX, endY);
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
        
        if ( Orientation.TOP_BOTTOM.equals(o) || Orientation.BOTTOM_TOP.equals(o)){

           if ( Orientation.BOTTOM_TOP.equals(o) ){
               colors = new int[colors.length];
               
               for ( int i=0; i<colors.length; i++){
               
                   colors[i] = gradient.colors[colors.length-i-1];
               }
           }
           g.fillLinearGradient(colors[0], colors[colors.length-1], r.getX(), r.getY(), r.getWidth(), r.getHeight(), false);
           
        } else if ( Orientation.LEFT_RIGHT.equals(o)){
           g.fillLinearGradient(gradient.colors[0], gradient.colors[gradient.colors.length-1], r.getX(), r.getY(), r.getWidth(), r.getHeight(), true);
        } else {
           Log.p("Gradient with type "+o+" not implemented yet.  Just filling solid rect");
           g.setColor(gradient.colors[0]);
           g.fillRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }
        
        
        
    }
    
}
