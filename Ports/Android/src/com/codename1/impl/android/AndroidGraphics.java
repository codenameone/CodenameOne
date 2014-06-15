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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;

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
        this.paint.setColor(0xff000000 | color);
        this.font.setColor(0xff000000 | color);
    }

    Paint getPaint() {
        return paint;
    }

    void setPaint(Paint p) {
        paint = p;
    }

    public void drawImage(Object img, int x, int y) {
        canvas.drawBitmap((Bitmap) img, x, y, paint);
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
        canvas.drawBitmap(b, src, dest, paint);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        paint.setStyle(Paint.Style.FILL);
        canvas.drawLine(x1, y1, x2, y2, paint);
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
        canvas.drawPath(this.tmppath, paint);
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
        canvas.drawPath(this.tmppath, paint);
    }
    
    public void drawRGB(int[] rgbData, int offset, int x,
            int y, int w, int h, boolean processAlpha) {
        canvas.drawBitmap(rgbData, offset, w, x, y, w, h,
                processAlpha, null);
    }
    
    public void drawRect(int x, int y, int width, int height) {
        boolean antialias = paint.isAntiAlias();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(false);
        canvas.drawRect(x, y, x + width, y + height, paint);
        paint.setAntiAlias(antialias);
    }
    
    public void drawRoundRect(int x, int y, int width,
            int height, int arcWidth, int arcHeight) {

        paint.setStyle(Paint.Style.STROKE);
        this.tmprectF.set(x, y, x + width, y + height);
        canvas.drawRoundRect(this.tmprectF, arcWidth, arcHeight, paint);
    }

    public void drawString(String str, int x, int y) {
        canvas.drawText(str, x, y - font.getFontMetricsInt().ascent, font);
    }

    public void drawArc(int x, int y, int width, int height,
            int startAngle, int arcAngle) {
        paint.setStyle(Paint.Style.STROKE);
        this.tmprectF.set(x, y, x + width, y + height);
        canvas.drawArc(this.tmprectF, 360 - startAngle,
                -arcAngle, false, paint);
    }

    public void fillArc(int x, int y, int width, int height,
            int startAngle, int arcAngle) {
        paint.setStyle(Paint.Style.FILL);
        this.tmprectF.set(x, y, x + width, y + height);
        canvas.drawArc(this.tmprectF, 360 - startAngle,
                -arcAngle, true, paint);
    }
    
    public void fillRect(int x, int y, int width, int height) {
        
        boolean antialias = paint.isAntiAlias();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(false);
        canvas.drawRect(x, y, x + width, y + height, paint);
        paint.setAntiAlias(antialias);
        
    }

    public void fillRoundRect(int x, int y, int width,
            int height, int arcWidth, int arcHeight) {
        paint.setStyle(Paint.Style.FILL);
        this.tmprectF.set(x, y, x + width, y + height);
        canvas.drawRoundRect(this.tmprectF, arcWidth, arcHeight, paint);
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
        canvas.restore();
        canvas.save();
    }

    public void scale(float x, float y) {
        canvas.scale(x, y);
    }

    public void rotate(float angle) {
        canvas.rotate((float)Math.toDegrees(angle));
    }

    public void rotate(float angle, int x, int y) {
        canvas.rotate((float)Math.toDegrees(angle), x, y);
    }
    
    public final void fillBitmap(int color) {
        canvas.drawColor(color, PorterDuff.Mode.SRC_OVER);        
    }
}
