/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
        int inc = 1;
        if ( r < 50 ){
            inc = 5;
        }
        if ( r < 20 ){
            inc = 20;
        }
        
        GeneralPath circle = new GeneralPath();
        for(int theta = 0; theta < 360; theta+=inc)
        {
            float x = (float)(cx + r * Math.cos(Math.toRadians(theta)));
            float y = (float)(cy + r * Math.sin(Math.toRadians(theta)));
            if(theta == 0)
                circle.moveTo(x,y);
            else
                circle.lineTo(x, y);
        }
        circle.closePath();
        
        drawPath(circle, paint);
 
    }

    public void drawArc(Rectangle2D oval, float currentAngle, float sweepAngle, boolean useCenter, Paint paint) {
        
        float cx = 0;
        float cy = 0;
        float r = (float)oval.getWidth()/2f;
        float inc = 1;
       
        
        
        GeneralPath circle = new GeneralPath();
        if ( useCenter ){
            circle.moveTo(cx, cy);
        }
        for(float theta = currentAngle; theta < currentAngle+sweepAngle; theta+=inc)
        {
            float x = (int)(cx + r * Math.cos(Math.toRadians(theta)));
            float y = (int)(cy + r * Math.sin(Math.toRadians(theta)));
            if(theta == currentAngle && !useCenter)
                circle.moveTo(x,y);
            else
                circle.lineTo(x,y);
        }
        if ( useCenter ){
            circle.closePath();
        }
        
        cx = (float)(oval.getX()+oval.getWidth()/2f);
        cy = (float)(oval.getY()+oval.getHeight()/2f);
        
        //Matrix at = Matrix.getTranslateInstance(cx, cy);
        Transform at = Transform.makeScale((float)1.0,(float)((oval.getHeight())/ (2.0*r)) );
        //at.scale(1.0, (oval.bottom-oval.top)/ (2.0*r));
        at.translate(cx, cy);
        
        Shape tCircle = circle.createTransformedShape(at);
        
        drawPath(tCircle, paint);
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
