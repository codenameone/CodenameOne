/*
 * Copyright 2009 Pader-Sync Ltd. & Co. KG.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * Visit http://www.pader-sync.com/ for contact information.
 *
 *
 * Linking this library statically or dynamically with other modules is making a
 * combined work based on this library. Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 *    As a special exception, the copyright holders of this library give you
 *    permission to link this library with independent modules to produce an
 *    executable, regardless of the license terms of these independent modules, and
 *    to copy and distribute the resulting executable under terms of your choice,
 *    provided that you also meet, for each linked independent module, the terms
 *    and conditions of the license of that module. An independent module is a
 *    module which is not derived from or based on this library. If you modify this
 *    library, you may extend this exception to your version of the library, but you
 *    are not obligated to do so. If you do not wish to do so, delete this exception
 *    statement from your version.
 *
 * March 2009
 * Thorsten Schemm
 * http://www.pader-sync.com/
 *
 */
package com.codename1.impl.android;


import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import com.codename1.ui.Stroke;

import com.codename1.ui.Transform;

/**
 * #######################################################################
 * #######################################################################
 *
 * Bundle one canvas and two paints to get one graphics object.
 */
class AndroidGraphics {

    private Canvas canvas;
    private Paint paint;
    private Paint font;
    private Transform transform;
    private Matrix convertedTransform;
    private boolean transformDirty = true;
    
    private boolean clipFresh;
    private final RectF tmprectF = new RectF();
    private final Rect tmprect = new Rect();
    private final Path tmppath = new Path();
    private final static PorterDuffXfermode PORTER = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
    
    AndroidGraphics(AndroidImplementation impl, Canvas canvas) {
        this.canvas = canvas;
        this.paint = new Paint();
        paint.setAntiAlias(true);
        this.font = (Paint) ((AndroidImplementation.NativeFont)impl.getDefaultFont()).font;
        if(canvas != null) {
            canvas.save();
        }
        transform = Transform.makeIdentity();
    }


    void setCanvas(Canvas canvas) {
        this.canvas = canvas;
        if(canvas != null) {
            canvas.save();
        }
    }

    void setCanvasNoSave(Canvas canvas) {
        this.canvas = canvas;
    }

    Paint getFont() {
        return font;
    }

    void setFont(Paint font) {
        this.font = font;
        this.font.setColor(this.paint.getColor());
    }

    void setColor(int color){
        this.paint.setColor((paint.getAlpha() << 24) | (color & 0xffffff));
        this.font.setColor((font.getAlpha() << 24) | (color & 0xffffff));
    }

    Paint getPaint() {
        return paint;
    }

    void setPaint(Paint p) {
        paint = p;
    }

    public void drawImage(Object img, int x, int y) {
        canvas.save();
        canvas.concat(getTransformMatrix());
        canvas.drawBitmap((Bitmap) img, x, y, paint);
        canvas.restore();
    }
    
    public void drawImage(Object img, int x, int y, int w, int h) {
        Bitmap b = (Bitmap) img;
        Rect src = new Rect();
        src.top = 0;
        src.bottom = b.getHeight();
        src.left = 0;
        src.right = b.getWidth();
        Rect dest = new Rect();
        dest.top = y;
        dest.bottom = y + h;
        dest.left = x;
        dest.right = x + w;
        canvas.save();
        canvas.concat(getTransformMatrix());
        canvas.drawBitmap(b, src, dest, paint);
        canvas.restore();
    }
    
    public void tileImage(Object img, int x, int y, int w, int h) {
        Bitmap b = (Bitmap) img;
        Rect dest = new Rect();
        dest.top = 0;
        dest.bottom = h;
        dest.left = 0;
        dest.right = w;
        BitmapShader shader = new BitmapShader(b, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Paint tilePainter = new Paint(paint);
        tilePainter.setShader(shader);
        tilePainter.setAntiAlias(false);
        canvas.save();
        canvas.translate(x, y);
        canvas.concat(getTransformMatrix());
        canvas.drawRect(dest, tilePainter);
        canvas.restore();
    }

    private Matrix getTransformMatrix(){
        if ( transformDirty ){
        	// Conversion from 4x4 to 3x3
        	// See http://www.w3.org/TR/2009/WD-SVG-Transforms-20090320/#_4x4-to-3x3-conversion
        	// for formula
            CN1Matrix4f m = (CN1Matrix4f)transform.getNativeTransform();
            float[] mMatrix3x3 = new float[9];
            float[] mMatrix4x4 = m.getData();
           
            
            mMatrix3x3[0] = mMatrix4x4[0];
            mMatrix3x3[1] = mMatrix4x4[4];
            mMatrix3x3[2] = mMatrix4x4[12];
            mMatrix3x3[3] = mMatrix4x4[1];
            mMatrix3x3[4] = mMatrix4x4[5];
            mMatrix3x3[5] = mMatrix4x4[13];
            mMatrix3x3[6] = mMatrix4x4[3];
            mMatrix3x3[7] = mMatrix4x4[7];
            mMatrix3x3[8] = mMatrix4x4[15];
            
            
            convertedTransform = new Matrix();
            
            convertedTransform.setValues(mMatrix3x3);
            
            transformDirty = false;
           
            
            
            
            
        }
        return convertedTransform;
        
    }
    
    public void drawLine(int x1, int y1, int x2, int y2) {
        paint.setStyle(Paint.Style.FILL);
        canvas.save();
        canvas.concat(getTransformMatrix());
        canvas.drawLine(x1, y1, x2, y2, paint);
        canvas.restore();
    }
    
    
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        if (nPoints <= 1) {
            return;
        }
        this.tmppath.rewind();
        this.tmppath.moveTo(xPoints[0], yPoints[0]);
        for (int i = 1; i < nPoints; i++) {
            this.tmppath.lineTo(xPoints[i], yPoints[i]);
        }
        paint.setStyle(Paint.Style.STROKE);
        canvas.save();
        canvas.concat(getTransformMatrix());
        canvas.drawPath(this.tmppath, paint);
        canvas.restore();
    }
    
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        if (nPoints <= 1) {
            return;
        }
        this.tmppath.rewind();
        this.tmppath.moveTo(xPoints[0], yPoints[0]);
        for (int i = 1; i < nPoints; i++) {
            this.tmppath.lineTo(xPoints[i], yPoints[i]);
        }
        paint.setStyle(Paint.Style.FILL);
        canvas.save();
        canvas.concat(getTransformMatrix());
        canvas.drawPath(this.tmppath, paint);
        canvas.restore();
    }
    
    public void drawRGB(int[] rgbData, int offset, int x,
            int y, int w, int h, boolean processAlpha) {
        canvas.save();
        canvas.concat(getTransformMatrix());
        canvas.drawBitmap(rgbData, offset, w, x, y, w, h,
                processAlpha, null);
        canvas.restore();
    }
    
    public void drawRect(int x, int y, int width, int height) {
        boolean antialias = paint.isAntiAlias();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(false);
        
        canvas.save();
        canvas.concat(getTransformMatrix());
        canvas.drawRect(x, y, x + width, y + height, paint);        
        paint.setAntiAlias(antialias);
        canvas.restore();
    }
    
    public void drawRoundRect(int x, int y, int width,
            int height, int arcWidth, int arcHeight) {

        paint.setStyle(Paint.Style.STROKE);
        this.tmprectF.set(x, y, x + width, y + height);
        canvas.save();
        canvas.concat(getTransformMatrix());
        canvas.drawRoundRect(this.tmprectF, arcWidth, arcHeight, paint);
        canvas.restore();
    }

    public void drawString(String str, int x, int y) {
        canvas.save();
        canvas.concat(getTransformMatrix());
        canvas.drawText(str, x, y - font.getFontMetricsInt().ascent, font);
        canvas.restore();
    }

    public void drawArc(int x, int y, int width, int height,
            int startAngle, int arcAngle) {
        paint.setStyle(Paint.Style.STROKE);
        this.tmprectF.set(x, y, x + width, y + height);
        canvas.save();
        canvas.concat(getTransformMatrix());
        canvas.drawArc(this.tmprectF, 360 - startAngle,
                -arcAngle, false, paint);
        canvas.restore();
    }

    public void fillArc(int x, int y, int width, int height,
            int startAngle, int arcAngle) {
        paint.setStyle(Paint.Style.FILL);
        this.tmprectF.set(x, y, x + width, y + height);
        canvas.save();
        canvas.concat(getTransformMatrix());
        canvas.drawArc(this.tmprectF, 360 - startAngle,
                -arcAngle, true, paint);
        canvas.restore();
    }
    
    public void fillRect(int x, int y, int width, int height) {
        
        boolean antialias = paint.isAntiAlias();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(false);
        canvas.save();
        canvas.concat(getTransformMatrix());
        canvas.drawRect(x, y, x + width, y + height, paint);
        paint.setAntiAlias(antialias);
        canvas.restore();
        
    }

    public void fillRoundRect(int x, int y, int width,
            int height, int arcWidth, int arcHeight) {
        paint.setStyle(Paint.Style.FILL);
        this.tmprectF.set(x, y, x + width, y + height);
        canvas.save();
        canvas.concat(getTransformMatrix());
        canvas.drawRoundRect(this.tmprectF, arcWidth, arcHeight, paint);
        canvas.restore();
    }

    public int getAlpha() {
        return paint.getAlpha();
    }

    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        paint.setXfermode(PORTER);
    }

    private void freshClip() {
        if(!clipFresh) {
            clipFresh = true;
            canvas.getClipBounds(this.tmprect);
        }
    }
    
    public int getClipHeight() {
        freshClip();
        return this.tmprect.height();
    }

    public int getClipWidth() {
        freshClip();
        return this.tmprect.width();
    }

    public int getClipX() {
        freshClip();
        return this.tmprect.left;
    }

    public int getClipY() {
        freshClip();
        return this.tmprect.top;
    }

    public void setClip(int x, int y, int width, int height) {
        clipFresh = false;
        canvas.clipRect(x, y, x + width, y + height, Region.Op.REPLACE);
    }

    public void clipRect(int x, int y, int width, int height) {
        clipFresh = false;
        canvas.clipRect(x, y, x + width, y + height);
    }

    public int getColor() {
        return paint.getColor();
    }
    
    public void resetAffine() {
        setTransform(Transform.makeIdentity());
        canvas.restore();
        canvas.save();
    }

    public void scale(float x, float y) {
        Transform t = getTransform();
        t.scale(x, y);
        setTransform(t);

    }

    public void rotate(float angle) {
        Transform t = getTransform();
        t.rotate(angle, 0, 0);
        setTransform(t);
    }

    public void rotate(float angle, int x, int y) {
        Transform t = getTransform();
        t.rotate(angle, x, y);
        setTransform(t);
    }
    
    public final void fillBitmap(int color) {
        canvas.drawColor(color, PorterDuff.Mode.SRC_OVER);        
    }
    
    public void drawPath(Path p, Stroke stroke) {
        paint.setStyle(Paint.Style.STROKE);
        Stroke old = setStroke(stroke);
        canvas.save();
        canvas.concat(getTransformMatrix());
        canvas.drawPath(p, paint);
        canvas.restore();
        setStroke(old);
    }
    
    /**
     * Sets the stroke of the current Paint object.
     * @param stroke The stroke to set.
     * @return The old stroke.
     */
    private Stroke setStroke(Stroke stroke){
        Stroke old = new Stroke(paint.getStrokeWidth(), convertStrokeCap(paint.getStrokeCap()),  convertStrokeJoin(paint.getStrokeJoin()), paint.getStrokeMiter());
        paint.setStrokeCap(convertStrokeCap(stroke.getCapStyle()));
        paint.setStrokeJoin(convertStrokeJoin(stroke.getJoinStyle()));
        paint.setStrokeMiter(stroke.getMiterLimit());
        paint.setStrokeWidth(stroke.getLineWidth());
        
        return old;
    }
    
    private int convertStrokeCap(Paint.Cap cap){
        if ( Paint.Cap.BUTT.equals(cap)){
            return Stroke.CAP_BUTT;
        } else if ( Paint.Cap.ROUND.equals(cap)){
            return Stroke.CAP_ROUND;
        } else if ( Paint.Cap.SQUARE.equals(cap)){
            return Stroke.CAP_SQUARE;
        } else {
            return Stroke.CAP_BUTT;
        }
    }
    
    private Paint.Cap convertStrokeCap(int cap){
        switch ( cap ){
            case Stroke.CAP_BUTT:
                return Paint.Cap.BUTT;
            case Stroke.CAP_ROUND:
                return Paint.Cap.ROUND;
            case Stroke.CAP_SQUARE:
                return Paint.Cap.SQUARE;
            default:
                return Paint.Cap.BUTT;
        }
    }
    
    private int convertStrokeJoin(Paint.Join join){
        if ( Paint.Join.BEVEL.equals(join)){
            return Stroke.JOIN_BEVEL;
        } else if ( Paint.Join.MITER.equals(join)){
            return Stroke.JOIN_MITER;
        } else if ( Paint.Join.ROUND.equals(join)){
            return Stroke.JOIN_ROUND;
        } else {
            return Stroke.JOIN_BEVEL;
        }
    }
    
    private Paint.Join convertStrokeJoin(int join){
        switch ( join ){
            case Stroke.JOIN_BEVEL:
                return Paint.Join.BEVEL;
            case Stroke.JOIN_MITER:
                return Paint.Join.MITER;
            case Stroke.JOIN_ROUND:
                return Paint.Join.ROUND;
            default:
                return Paint.Join.BEVEL;
        }
    }
    
    public void fillPath(Path p) {
        paint.setStyle(Paint.Style.FILL);
        canvas.save();
        canvas.concat(getTransformMatrix());
        canvas.drawPath(p, paint);
        canvas.restore();
    }
    
    public void setTransform(Transform transform) {
        this.transform = transform;
        transformDirty = true;
    }

    public Transform getTransform() {
        return transform;
    }
    
}
