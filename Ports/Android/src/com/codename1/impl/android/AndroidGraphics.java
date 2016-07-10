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
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.view.View;

import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.Stroke;

import com.codename1.ui.Transform;
import com.codename1.ui.geom.Shape;
import com.codename1.ui.plaf.Style;

/**
 * #######################################################################
 * #######################################################################
 *
 * Bundle one canvas and two paints to get one graphics object.
 */
class AndroidGraphics {

    protected Canvas canvas;
    protected Paint paint;
    private boolean isMutableImageGraphics;
    private CodenameOneTextPaint font;
    private Transform transform;
    private Matrix convertedTransform, convertedInverseTransform;
    private boolean transformDirty = true;
    private boolean inverseTransformDirty = true;

    private boolean clipFresh;
    private final RectF tmprectF = new RectF();
    private final Rect tmprect = new Rect();
    private final Path tmppath = new Path();
    protected final static PorterDuffXfermode PORTER = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
    AndroidImplementation impl;
    private int alpha = 255;


    AndroidGraphics(AndroidImplementation impl, Canvas canvas, boolean isMutableImageGraphics) {
        this.isMutableImageGraphics = isMutableImageGraphics;
        this.canvas = canvas;
        this.paint = new Paint();
        paint.setAntiAlias(true);
        this.font = (CodenameOneTextPaint) ((AndroidImplementation.NativeFont)impl.getDefaultFont()).font;
        if(canvas != null) {
            canvas.save();
        }
        transform = Transform.makeIdentity();
        this.impl = impl;
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

    void setFont(CodenameOneTextPaint font) {
        this.font = font;
        this.font.setColor(this.paint.getColor());
    }

    void setColor(int color){
        int c = (alpha << 24) | (color & 0xffffff);
        this.paint.setColor(c);
        this.font.setColor(c);
    }

    Paint getPaint() {
        return paint;
    }

    void setPaint(Paint p) {
        paint = p;
    }

    public void drawImage(Object img, int x, int y) {
        canvas.save();
        applyTransform();
        canvas.drawBitmap((Bitmap) img, x, y, paint);
        unapplyTransform();
        canvas.restore();
    }

    void drawImageImpl(Object img, int x, int y, int w, int h) {
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

    public void drawImage(Object img, int x, int y, int w, int h) {
        canvas.save();
        applyTransform();
        drawImageImpl(img, x, y, w, h);
        unapplyTransform();
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
        applyTransform();
        canvas.drawRect(dest, tilePainter);
        unapplyTransform();
        canvas.restore();
    }
    
    private void tileImageImpl(Object img, int x, int y, int w, int h) {
        Bitmap b = (Bitmap) img;
        Rect dest = new Rect();
        dest.top = 0;
        dest.bottom = h;
        dest.left = 0;
        dest.right = w;
        BitmapShader shader = new BitmapShader(b, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Paint tilePainter = new Paint();
        tilePainter.setShader(shader);
        tilePainter.setAntiAlias(false);
        canvas.translate(x, y);
        //canvas.concat(getTransformMatrix());
        canvas.drawRect(dest, tilePainter);
    }
    

    private Matrix getInverseTransform() {
        if (inverseTransformDirty) {
            if (convertedInverseTransform == null) {
                convertedInverseTransform = new Matrix();
            }
            getTransformMatrix().invert(convertedInverseTransform);
            inverseTransformDirty = false;
        }
        return convertedInverseTransform;
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
        applyTransform();
        canvas.drawLine(x1, y1, x2, y2, paint);
        unapplyTransform();
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
        applyTransform();
        canvas.drawPath(this.tmppath, paint);
        unapplyTransform();
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
        applyTransform();
        canvas.drawPath(this.tmppath, paint);
        unapplyTransform();
        canvas.restore();
    }

    public void drawRGB(int[] rgbData, int offset, int x,
            int y, int w, int h, boolean processAlpha) {
        canvas.save();
        applyTransform();
        canvas.drawBitmap(rgbData, offset, w, x, y, w, h,
                processAlpha, null);
        unapplyTransform();
        canvas.restore();
    }

    public void drawRect(int x, int y, int width, int height) {
        boolean antialias = paint.isAntiAlias();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(false);

        canvas.save();
        applyTransform();
        canvas.drawRect(x, y, x + width, y + height, paint);
        paint.setAntiAlias(antialias);
        unapplyTransform();
        canvas.restore();
    }

    public void drawRoundRect(int x, int y, int width,
            int height, int arcWidth, int arcHeight) {

        paint.setStyle(Paint.Style.STROKE);
        this.tmprectF.set(x, y, x + width, y + height);
        canvas.save();
        applyTransform();
        canvas.drawRoundRect(this.tmprectF, arcWidth, arcHeight, paint);
        unapplyTransform();
        canvas.restore();
    }

    public void drawString(String str, int x, int y) {
        canvas.save();
        applyTransform();
        canvas.drawText(str, x, y - font.getFontAscent(), font);
        unapplyTransform();
        canvas.restore();
    }

    public void drawArc(int x, int y, int width, int height,
            int startAngle, int arcAngle) {
        paint.setStyle(Paint.Style.STROKE);
        this.tmprectF.set(x, y, x + width, y + height);
        canvas.save();
        applyTransform();
        canvas.drawArc(this.tmprectF, 360 - startAngle,
                -arcAngle, false, paint);
        unapplyTransform();
        canvas.restore();
    }

    public void fillArc(int x, int y, int width, int height,
            int startAngle, int arcAngle) {
        paint.setStyle(Paint.Style.FILL);
        this.tmprectF.set(x, y, x + width, y + height);
        canvas.save();
        applyTransform();
        canvas.drawArc(this.tmprectF, 360 - startAngle,
                -arcAngle, true, paint);
        unapplyTransform();
        canvas.restore();
    }

    public void fillRect(int x, int y, int width, int height) {
        //System.out.println("Filling rect "+x+", "+y+", "+width+", "+height);
        //System.out.println("Clip bounds is "+canvas.getClipBounds());
        boolean antialias = paint.isAntiAlias();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(false);
        canvas.save();
        applyTransform();
        canvas.drawRect(x, y, x + width, y + height, paint);
        paint.setAntiAlias(antialias);
        unapplyTransform();
        canvas.restore();

    }

   public void fillRect(int x, int y, int w, int h, byte alpha) {
        if(alpha != 0) {
            int oldAlpha = getAlpha();
            setAlpha(alpha & 0xff);
            fillRect(x, y, w, h);
            setAlpha(oldAlpha);
        }
    }

   public void fillRectImpl(int x, int y, int w, int h, byte alpha) {
        if(alpha != 0) {
            int oldAlpha = getAlpha();
            setAlpha(alpha & 0xff);

            boolean antialias = paint.isAntiAlias();
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(false);
            canvas.drawRect(x, y, x + w, y + h, paint);
            paint.setAntiAlias(antialias);

            setAlpha(oldAlpha);
        }
    }

    public void paintComponentBackground(byte bgType, Image bgImageOrig, int bgColor, byte bgTransparency,
                                         int startColor, int endColor, float relativeX,
                                         float relativeY, float relativeSize,
                                         int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        canvas.save();
        applyTransform();
        try {
            if (bgImageOrig == null) {
                if(bgType >= Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL) {
                    drawGradientBackground(bgType, bgColor, bgTransparency, startColor, endColor,
                            relativeX, relativeY, relativeSize, x, y, width, height);
                    //canvas.restore();
                    return;
                }
                setColor(bgColor);
                fillRectImpl(x, y, width, height, bgTransparency);
                //canvas.restore();
                return;
            } else {
                int iW = bgImageOrig.getWidth();
                int iH = bgImageOrig.getHeight();
                Object bgImage = bgImageOrig.getImage();
                switch (bgType) {
                    case Style.BACKGROUND_NONE:
                        if (bgTransparency != 0) {
                            setColor(bgColor);
                            fillRectImpl(x, y, width, height, bgTransparency);
                        }
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_SCALED:
                        drawImageImpl(bgImage, x, y, width, height);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_SCALED_FILL:
                        float r = Math.max(((float) width) / ((float) iW), ((float) height) / ((float) iH));
                        int bwidth = (int) (((float) iW) * r);
                        int bheight = (int) (((float) iH) * r);
                        drawImageImpl(bgImage, x + (width - bwidth) / 2, y + (height - bheight) / 2, bwidth, bheight);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_SCALED_FIT:
                        if (bgTransparency != 0) {
                            setColor(bgColor);
                            fillRectImpl(x, y, width, height, bgTransparency);
                        }
                        float r2 = Math.min(((float) width) / ((float) iW), ((float) height) / ((float) iH));
                        int awidth = (int) (((float) iW) * r2);
                        int aheight = (int) (((float) iH) * r2);
                        drawImageImpl(bgImage, x + (width - awidth) / 2, y + (height - aheight) / 2, awidth, aheight);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_TILE_BOTH:
                        tileImageImpl(bgImage, x, y, width, height);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_TOP:
                        setColor(bgColor);
                        fillRectImpl(x, y, width, height, bgTransparency);
                        tileImageImpl(bgImage, x, y, width, iH);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_CENTER:
                        setColor(bgColor);
                        fillRectImpl(x, y, width, height, bgTransparency);
                        tileImageImpl(bgImage, x, y + (height / 2 - iH / 2), width, iH);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM:
                        setColor(bgColor);
                        fillRectImpl(x, y, width, height, bgTransparency);
                        tileImageImpl(bgImage, x, y + (height - iH), width, iH);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_LEFT:
                        setColor(bgColor);
                        fillRectImpl(x, y, width, height, bgTransparency);
                        for (int yPos = 0; yPos <= height; yPos += iH) {
                            canvas.drawBitmap((Bitmap) bgImage, x, y + yPos, paint);
                        }
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_CENTER:
                        setColor(bgColor);
                        fillRectImpl(x, y, width, height, bgTransparency);
                        for (int yPos = 0; yPos <= height; yPos += iH) {
                            canvas.drawBitmap((Bitmap) bgImage, x + (width / 2 - iW / 2), y + yPos, paint);
                        }
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_RIGHT:
                        setColor(bgColor);
                        fillRectImpl(x, y, width, height, bgTransparency);
                        for (int yPos = 0; yPos <= height; yPos += iH) {
                            canvas.drawBitmap((Bitmap) bgImage, x + width - iW, y + yPos, paint);
                        }
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_TOP:
                        setColor(bgColor);
                        fillRectImpl(x, y, width, height, bgTransparency);
                        canvas.drawBitmap((Bitmap) bgImage, x + (width / 2 - iW / 2), y, paint);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM:
                        setColor(bgColor);
                        fillRectImpl(x, y, width, height, bgTransparency);
                        canvas.drawBitmap((Bitmap) bgImage, x + (width / 2 - iW / 2), y + (height - iH), paint);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_LEFT:
                        setColor(bgColor);
                        fillRectImpl(x, y, width, height, bgTransparency);
                        canvas.drawBitmap((Bitmap) bgImage, x, y + (height / 2 - iH / 2), paint);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_RIGHT:
                        setColor(bgColor);
                        fillRectImpl(x, y, width, height, bgTransparency);
                        canvas.drawBitmap((Bitmap) bgImage, x + width - iW, y + (height / 2 - iH / 2), paint);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_CENTER:
                        setColor(bgColor);
                        fillRectImpl(x, y, width, height, bgTransparency);
                        canvas.drawBitmap((Bitmap) bgImage, x + (width / 2 - iW / 2), y + (height / 2 - iH / 2), paint);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_TOP_LEFT:
                        setColor(bgColor);
                        fillRectImpl(x, y, width, height, bgTransparency);
                        canvas.drawBitmap((Bitmap) bgImage, x, y, paint);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT:
                        setColor(bgColor);
                        fillRectImpl(x, y, width, height, bgTransparency);
                        canvas.drawBitmap((Bitmap) bgImage, x + width - iW, y, paint);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_LEFT:
                        setColor(bgColor);
                        fillRectImpl(x, y, width, height, bgTransparency);
                        canvas.drawBitmap((Bitmap) bgImage, x, y + (height - iH), paint);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_RIGHT:
                        setColor(bgColor);
                        fillRectImpl(x, y, width, height, bgTransparency);
                        canvas.drawBitmap((Bitmap) bgImage, x + width - iW, y + (height - iH), paint);
                        ///canvas.restore();
                        return;
                    case Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL:
                    case Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL:
                    case Style.BACKGROUND_GRADIENT_RADIAL:
                        drawGradientBackground(bgType, bgColor, bgTransparency, startColor, endColor,
                                relativeX, relativeY, relativeSize, x, y, width, height);
                        //canvas.restore();
                        return;
                }
            }
        } finally {
            unapplyTransform();
            canvas.restore();
        }

    }

    private void drawGradientBackground(byte bgType, int bgColor, byte bgTransparency, int startColor, int endColor, float relativeX,
                        float relativeY, float relativeSize,
                        int x, int y, int width, int height) {
        switch (bgType) {
            case Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL:
                fillLinearGradient(startColor, endColor,
                        x, y, width, height, true);
                return;
            case Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL:
                fillLinearGradient(startColor, endColor,
                        x, y, width, height, false);
                return;
            case Style.BACKGROUND_GRADIENT_RADIAL:
                fillRectRadialGradient(startColor, endColor,
                        x, y, width, height, relativeX, relativeY,
                        relativeSize);
                return;
        }
        setColor(bgColor);
        fillRectImpl(x, y, width, height, bgTransparency);
    }   
   
    public void paintComponentBackground(int x, int y, int width, int height, Style s) {
        if (width <= 0 || height <= 0) {
            return;
        }
        canvas.save();
        applyTransform();
        Image bgImageOrig = s.getBgImage();
        try {
            if (bgImageOrig == null) {
                if (s.getBackgroundType() >= Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL) {
                    drawGradientBackground(s, x, y, width, height);
                    //canvas.restore();
                    return;
                }
                setColor(s.getBgColor());
                fillRectImpl(x, y, width, height, s.getBgTransparency());
                //canvas.restore();
                return;
            } else {
                int iW = bgImageOrig.getWidth();
                int iH = bgImageOrig.getHeight();
                Object bgImage = bgImageOrig.getImage();
                switch (s.getBackgroundType()) {
                    case Style.BACKGROUND_NONE:
                        if (s.getBgTransparency() != 0) {
                            setColor(s.getBgColor());
                            fillRectImpl(x, y, width, height, s.getBgTransparency());
                        }
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_SCALED:
                        drawImageImpl(bgImage, x, y, width, height);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_SCALED_FILL:
                        float r = Math.max(((float) width) / ((float) iW), ((float) height) / ((float) iH));
                        int bwidth = (int) (((float) iW) * r);
                        int bheight = (int) (((float) iH) * r);
                        drawImageImpl(bgImage, x + (width - bwidth) / 2, y + (height - bheight) / 2, bwidth, bheight);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_SCALED_FIT:
                        if (s.getBgTransparency() != 0) {
                            setColor(s.getBgColor());
                            fillRectImpl(x, y, width, height, s.getBgTransparency());
                        }
                        float r2 = Math.min(((float) width) / ((float) iW), ((float) height) / ((float) iH));
                        int awidth = (int) (((float) iW) * r2);
                        int aheight = (int) (((float) iH) * r2);
                        drawImageImpl(bgImage, x + (width - awidth) / 2, y + (height - aheight) / 2, awidth, aheight);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_TILE_BOTH:
                        tileImageImpl(bgImage, x, y, width, height);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_TOP:
                        setColor(s.getBgColor());
                        fillRectImpl(x, y, width, height, s.getBgTransparency());
                        tileImageImpl(bgImage, x, y, width, iH);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_CENTER:
                        setColor(s.getBgColor());
                        fillRectImpl(x, y, width, height, s.getBgTransparency());
                        tileImageImpl(bgImage, x, y + (height / 2 - iH / 2), width, iH);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM:
                        setColor(s.getBgColor());
                        fillRectImpl(x, y, width, height, s.getBgTransparency());
                        tileImageImpl(bgImage, x, y + (height - iH), width, iH);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_LEFT:
                        setColor(s.getBgColor());
                        fillRectImpl(x, y, width, height, s.getBgTransparency());
                        for (int yPos = 0; yPos <= height; yPos += iH) {
                            canvas.drawBitmap((Bitmap) bgImage, x, y + yPos, paint);
                        }
                        canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_CENTER:
                        setColor(s.getBgColor());
                        fillRectImpl(x, y, width, height, s.getBgTransparency());
                        for (int yPos = 0; yPos <= height; yPos += iH) {
                            canvas.drawBitmap((Bitmap) bgImage, x + (width / 2 - iW / 2), y + yPos, paint);
                        }
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_RIGHT:
                        setColor(s.getBgColor());
                        fillRectImpl(x, y, width, height, s.getBgTransparency());
                        for (int yPos = 0; yPos <= height; yPos += iH) {
                            canvas.drawBitmap((Bitmap) bgImage, x + width - iW, y + yPos, paint);
                        }
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_TOP:
                        setColor(s.getBgColor());
                        fillRectImpl(x, y, width, height, s.getBgTransparency());
                        canvas.drawBitmap((Bitmap) bgImage, x + (width / 2 - iW / 2), y, paint);
                        canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM:
                        setColor(s.getBgColor());
                        fillRectImpl(x, y, width, height, s.getBgTransparency());
                        canvas.drawBitmap((Bitmap) bgImage, x + (width / 2 - iW / 2), y + (height - iH), paint);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_LEFT:
                        setColor(s.getBgColor());
                        fillRectImpl(x, y, width, height, s.getBgTransparency());
                        canvas.drawBitmap((Bitmap) bgImage, x, y + (height / 2 - iH / 2), paint);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_RIGHT:
                        setColor(s.getBgColor());
                        fillRectImpl(x, y, width, height, s.getBgTransparency());
                        canvas.drawBitmap((Bitmap) bgImage, x + width - iW, y + (height / 2 - iH / 2), paint);
                        canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_CENTER:
                        setColor(s.getBgColor());
                        fillRectImpl(x, y, width, height, s.getBgTransparency());
                        canvas.drawBitmap((Bitmap) bgImage, x + (width / 2 - iW / 2), y + (height / 2 - iH / 2), paint);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_TOP_LEFT:
                        setColor(s.getBgColor());
                        fillRectImpl(x, y, width, height, s.getBgTransparency());
                        canvas.drawBitmap((Bitmap) bgImage, x, y, paint);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT:
                        setColor(s.getBgColor());
                        fillRectImpl(x, y, width, height, s.getBgTransparency());
                        canvas.drawBitmap((Bitmap) bgImage, x + width - iW, y, paint);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_LEFT:
                        setColor(s.getBgColor());
                        fillRectImpl(x, y, width, height, s.getBgTransparency());
                        canvas.drawBitmap((Bitmap) bgImage, x, y + (height - iH), paint);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_RIGHT:
                        setColor(s.getBgColor());
                        fillRectImpl(x, y, width, height, s.getBgTransparency());
                        canvas.drawBitmap((Bitmap) bgImage, x + width - iW, y + (height - iH), paint);
                        //canvas.restore();
                        return;
                    case Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL:
                    case Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL:
                    case Style.BACKGROUND_GRADIENT_RADIAL:
                        drawGradientBackground(s, x, y, width, height);
                        //canvas.restore();
                        return;
                }
            }
        } finally {
            unapplyTransform();
            canvas.restore();
        }

    }

    private void drawGradientBackground(Style s, int x, int y, int width, int height) {
        switch (s.getBackgroundType()) {
            case Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL:
                fillLinearGradient(s.getBackgroundGradientStartColor(), s.getBackgroundGradientEndColor(),
                        x, y, width, height, true);
                return;
            case Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL:
                fillLinearGradient(s.getBackgroundGradientStartColor(), s.getBackgroundGradientEndColor(),
                        x, y, width, height, false);
                return;
            case Style.BACKGROUND_GRADIENT_RADIAL:
                fillRectRadialGradient(s.getBackgroundGradientStartColor(), s.getBackgroundGradientEndColor(),
                        x, y, width, height, s.getBackgroundGradientRelativeX(), s.getBackgroundGradientRelativeY(),
                        s.getBackgroundGradientRelativeSize());
                return;
        }
        setColor(s.getBgColor());
        fillRectImpl(x, y, width, height, s.getBgTransparency());
    }

    public void fillLinearGradient(int startColor, int endColor, int x, int y, int width, int height, boolean horizontal) {
        boolean antialias = paint.isAntiAlias();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(false);
        paint.setAlpha(255);
        if(!horizontal) {
            paint.setShader(new LinearGradient(0, 0, 0, height, 0xff000000 | startColor, 0xff000000 | endColor, Shader.TileMode.MIRROR));
        } else {
            paint.setShader(new LinearGradient(0, 0, width, 0, 0xff000000 | startColor, 0xff000000 | endColor, Shader.TileMode.MIRROR));
        }
        canvas.save();
        applyTransform();
        canvas.drawRect(x, y, x + width, y + height, paint);
        paint.setAntiAlias(antialias);
        paint.setShader(null);
        unapplyTransform();
        canvas.restore();
    }

    public void fillRectRadialGradient(int startColor, int endColor, int x, int y, int width, int height, float relativeX, float relativeY, float relativeSize) {
        boolean antialias = paint.isAntiAlias();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(false);
        paint.setAlpha(255);
        float radius = Math.min((float)width, (float)height) * relativeSize;
        int centerX = (int) (width * (1 - relativeX));
        int centerY = (int) (height * (1 - relativeY));

        paint.setShader(new RadialGradient(x + centerX, y + centerY, radius, 0xff000000 | startColor, 0xff000000 | endColor, Shader.TileMode.MIRROR));
        canvas.save();
        applyTransform();
        canvas.drawRect(x, y, x + width, y + height, paint);
        paint.setAntiAlias(antialias);
        paint.setShader(null);
        unapplyTransform();
        canvas.restore();
    }

    public void fillRadialGradient(int startColor, int endColor, int x, int y, int width, int height) {
        boolean antialias = paint.isAntiAlias();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(false);
        paint.setAlpha(255);
        paint.setShader(new RadialGradient(x, y, Math.max(width, height), 0xff000000 | startColor, 0xff000000 | endColor, Shader.TileMode.MIRROR));
        canvas.save();
        applyTransform();
        canvas.drawRect(x, y, x + width, y + height, paint);
        paint.setAntiAlias(antialias);
        paint.setShader(null);
        unapplyTransform();
        canvas.restore();
    }

    public void drawLabelComponent(int cmpX, int cmpY, int cmpHeight, int cmpWidth, Style style, String text,
            Bitmap icon, Bitmap stateIcon, int preserveSpaceForState, int gap, boolean rtl, boolean isOppositeSide,
            int textPosition, int stringWidth, boolean isTickerRunning, int tickerShiftText, boolean endsWith3Points, int valign) {
        int clipX = getClipX();
        int clipY = getClipY();
        int clipW = getClipWidth();
        int clipH = getClipHeight();

        Font cn1Font = style.getFont();
        Object nativeFont = cn1Font.getNativeFont();
        impl.setNativeFont(this, nativeFont);
        setColor(style.getFgColor());
        canvas.save();
        applyTransform();

        int iconWidth = 0;
        int iconHeight = 0;
        if(icon != null) {
            iconWidth = icon.getWidth();
            iconHeight = icon.getHeight();
        }

        int textDecoration = style.getTextDecoration();
        int stateIconSize = 0;
        int stateIconYPosition = 0;

        int leftPadding = style.getPaddingLeft(rtl);
        int rightPadding = style.getPaddingRight(rtl);
        int topPadding = style.getPaddingTop();
        int bottomPadding = style.getPaddingBottom();

        int fontHeight = 0;
        if (text == null) {
            text = "";
        }
        if (text.length() > 0) {
            fontHeight = cn1Font.getHeight();
        }

        if (stateIcon != null) {
            stateIconSize = stateIcon.getWidth();
            stateIconYPosition = cmpY + topPadding
                    + (cmpHeight - topPadding
                    - bottomPadding) / 2 - stateIconSize / 2;
            int tX = cmpX;
            if (isOppositeSide) {
                if (rtl) {
                    tX += leftPadding;
                } else {
                    tX = tX + cmpWidth - leftPadding - stateIconSize;
                }
                cmpWidth -= leftPadding - stateIconSize;
            } else {
                preserveSpaceForState = stateIconSize + gap;
                if (rtl) {
                    tX = tX + cmpWidth - leftPadding - stateIconSize;
                } else {
                    tX += leftPadding;
                }
            }

            drawImage(stateIcon, tX, stateIconYPosition);
        }

        //default for bottom left alignment
        int x = cmpX + leftPadding + preserveSpaceForState;
        int y = cmpY + topPadding;

        int align = reverseAlignForBidi(rtl, style.getAlignment());

        int textPos = reverseAlignForBidi(rtl, textPosition);

        //set initial x,y position according to the alignment and textPosition
        switch (align) {
            case Component.LEFT:
                switch (textPos) {
                    case Label.LEFT:
                    case Label.RIGHT:
                        y = y + (cmpHeight - (topPadding + bottomPadding + Math.max(((icon != null) ? iconHeight : 0), fontHeight))) / 2;
                        break;
                    case Label.BOTTOM:
                    case Label.TOP:
                        y = y + (cmpHeight - (topPadding + bottomPadding + ((icon != null) ? iconHeight + gap : 0) + fontHeight)) / 2;
                        break;
                }
                break;
            case Component.CENTER:
                switch (textPos) {
                    case Label.LEFT:
                    case Label.RIGHT:
                        x = x + (cmpWidth - (preserveSpaceForState
                                + leftPadding
                                + rightPadding
                                + ((icon != null) ? iconWidth + gap : 0)
                                + stringWidth)) / 2;
                        x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                        y = y + (cmpHeight - (topPadding
                                + bottomPadding
                                + Math.max(((icon != null) ? iconHeight : 0),
                                        fontHeight))) / 2;
                        break;
                    case Label.BOTTOM:
                    case Label.TOP:
                        x = x + (cmpWidth - (preserveSpaceForState + leftPadding
                                + rightPadding
                                + Math.max(((icon != null) ? iconWidth + gap : 0),
                                        stringWidth))) / 2;
                        x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                        y = y + (cmpHeight - (topPadding
                                + bottomPadding
                                + ((icon != null) ? iconHeight + gap : 0)
                                + fontHeight)) / 2;
                        break;
                }
                break;
            case Component.RIGHT:
                switch (textPos) {
                    case Label.LEFT:
                    case Label.RIGHT:
                        x = cmpX + cmpWidth - rightPadding
                                - (((icon != null) ? (iconWidth + gap) : 0)
                                + stringWidth);
                        if (rtl) {
                            x = Math.max(x - preserveSpaceForState, cmpX + leftPadding);
                        } else {
                            x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                        }
                        y = y + (cmpHeight - (topPadding
                                + bottomPadding
                                + Math.max(((icon != null) ? iconHeight : 0),
                                        fontHeight))) / 2;
                        break;
                    case Label.BOTTOM:
                    case Label.TOP:
                        x = cmpX + cmpWidth - rightPadding
                                - (Math.max(((icon != null) ? (iconWidth) : 0),
                                        stringWidth));
                        x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                        y = y + (cmpHeight - (topPadding
                                + bottomPadding
                                + ((icon != null) ? iconHeight + gap : 0) + fontHeight)) / 2;
                        break;
                }
                break;
            default:
                break;
        }

        int textSpaceW = cmpWidth - rightPadding - leftPadding;

        if (icon != null && (textPos == Label.RIGHT || textPos == Label.LEFT)) {
            textSpaceW = textSpaceW - iconWidth;
        }

        if (stateIcon != null) {
            textSpaceW = textSpaceW - stateIconSize;
        } else {
            textSpaceW = textSpaceW - preserveSpaceForState;
        }

        if (icon == null) {
            // no icon only string
            drawLabelString(nativeFont, text, x, y, textSpaceW, isTickerRunning, tickerShiftText,
                    textDecoration, rtl, endsWith3Points, stringWidth, fontHeight);
        } else {
            int strWidth = stringWidth;
            int iconStringWGap;
            int iconStringHGap;

            switch (textPos) {
                case Label.LEFT:
                    if (iconHeight > fontHeight) {
                        iconStringHGap = (iconHeight - fontHeight) / 2;
                        strWidth = drawLabelStringValign(nativeFont, text, x, y, textSpaceW, isTickerRunning,
                                tickerShiftText, textDecoration, rtl, endsWith3Points, strWidth, iconStringHGap, iconHeight,
                                fontHeight, valign);

                        drawImage(icon, x + strWidth + gap, y);
                    } else {
                        iconStringHGap = (fontHeight - iconHeight) / 2;
                        strWidth = drawLabelString(nativeFont, text, x, y, textSpaceW, isTickerRunning,
                                tickerShiftText, textDecoration, rtl, endsWith3Points, strWidth, fontHeight);

                        drawImage(icon, x + strWidth + gap, y + iconStringHGap);
                    }
                    break;
                case Label.RIGHT:
                    if (iconHeight > fontHeight) {
                        iconStringHGap = (iconHeight - fontHeight) / 2;
                        drawImage(icon, x, y);
                        drawLabelStringValign(nativeFont, text, x + iconWidth + gap, y, textSpaceW, isTickerRunning,
                                tickerShiftText, textDecoration, rtl, endsWith3Points, iconWidth, iconStringHGap, iconHeight, fontHeight, valign);
                    } else {
                        iconStringHGap = (fontHeight - iconHeight) / 2;
                        drawImage(icon, x, y + iconStringHGap);
                        drawLabelString(nativeFont, text, x + iconWidth + gap, y, textSpaceW, isTickerRunning,
                                tickerShiftText, textDecoration, rtl, endsWith3Points, iconWidth, fontHeight);
                    }
                    break;
                case Label.BOTTOM:
                    //center align the smaller
                    if (iconWidth > strWidth) {
                        iconStringWGap = (iconWidth - strWidth) / 2;
                        drawImage(icon, x, y);
                        drawLabelString(nativeFont, text, x + iconStringWGap, y + iconHeight + gap, textSpaceW,
                                isTickerRunning, tickerShiftText, textDecoration, rtl, endsWith3Points, iconWidth, fontHeight);
                    } else {
                        iconStringWGap = (Math.min(strWidth, textSpaceW) - iconWidth) / 2;
                        drawImage(icon, x + iconStringWGap, y);

                        drawLabelString(nativeFont, text, x, y + iconHeight + gap, textSpaceW, isTickerRunning,
                                tickerShiftText, textDecoration, rtl, endsWith3Points, iconWidth, fontHeight);
                    }
                    break;
                case Label.TOP:
                    //center align the smaller
                    if (iconWidth > strWidth) {
                        iconStringWGap = (iconWidth - strWidth) / 2;
                        drawLabelString(nativeFont, text, x + iconStringWGap, y, textSpaceW, isTickerRunning,
                                tickerShiftText, textDecoration, rtl, endsWith3Points, iconWidth, fontHeight);
                        drawImage(icon, x, y + fontHeight + gap);
                    } else {
                        iconStringWGap = (Math.min(strWidth, textSpaceW) - iconWidth) / 2;
                        drawLabelString(nativeFont, text, x, y, textSpaceW, isTickerRunning, tickerShiftText,
                                textDecoration, rtl, endsWith3Points, iconWidth, fontHeight);
                        drawImage(icon, x + iconStringWGap, y + fontHeight + gap);
                    }
                    break;
            }
        }
        unapplyTransform();
        canvas.restore();
        setClip(clipX, clipY, clipW, clipH);
    }

    /**
     * Implements the drawString for the text component and adjust the valign
     * assuming the icon is in one of the sides
     */
    private int drawLabelStringValign(
            Object nativeFont, String str, int x, int y, int textSpaceW,
            boolean isTickerRunning, int tickerShiftText, int textDecoration, boolean rtl,
            boolean endsWith3Points, int textWidth,
            int iconStringHGap, int iconHeight, int fontHeight, int valign) {
        switch (valign) {
            case Component.TOP:
                return drawLabelString(nativeFont, str, x, y, textSpaceW, isTickerRunning, tickerShiftText, textDecoration, rtl, endsWith3Points, textWidth, fontHeight);
            case Component.CENTER:
                return drawLabelString(nativeFont, str, x, y + iconHeight / 2 - fontHeight / 2, textSpaceW, isTickerRunning, tickerShiftText, textDecoration, rtl, endsWith3Points, textWidth, fontHeight);
            default:
                return drawLabelString(nativeFont, str, x, y + iconStringHGap, textSpaceW, isTickerRunning, tickerShiftText, textDecoration, rtl, endsWith3Points, textWidth, fontHeight);
        }
    }

    /**
     * Implements the drawString for the text component and adjust the valign
     * assuming the icon is in one of the sides
     */
    private int drawLabelString(Object nativeFont, String text, int x, int y, int textSpaceW,
            boolean isTickerRunning, int tickerShiftText, int textDecoration, boolean rtl, boolean endsWith3Points, int textWidth,
            int fontHeight) {
        int cx = getClipX();
        int cy = getClipY();
        int cw = getClipWidth();
        int ch = getClipHeight();
        clipRect(x, cy, textSpaceW, ch);

        int drawnW = drawLabelText(textDecoration, rtl, isTickerRunning, endsWith3Points, nativeFont,
                textWidth, textSpaceW, tickerShiftText, text, x, y, fontHeight);

        setClip(cx, cy, cw, ch);

        return drawnW;
    }

    private boolean fastCharWidthCheck(String s, int length, int width, int charWidth, Object f) {
        if (length * charWidth < width) {
            return true;
        }
        length = Math.min(s.length(), length);
        return impl.stringWidth(f, s.substring(0, length)) < width;
    }

    /**
     * Draws the text of a label
     *
     * @param textDecoration decoration information for the text
     * @param text the text for the label
     * @param x position for the label
     * @param y position for the label
     * @param txtW stringWidth(text) equivalent which is faster than just
     * invoking string width all the time
     * @param textSpaceW the width available for the component
     * @return the space used by the drawing
     */
    protected int drawLabelText(int textDecoration, boolean rtl, boolean isTickerRunning,
            boolean endsWith3Points, Object nativeFont, int txtW, int textSpaceW, int shiftText, String text, int x, int y, int fontHeight) {
        if ((!isTickerRunning) || rtl) {
            //if there is no space to draw the text add ... at the end
            if (txtW > textSpaceW && textSpaceW > 0) {
                // Handling of adding 3 points and in fact all text positioning when the text is bigger than
                // the allowed space is handled differently in RTL, this is due to the reverse algorithm
                // effects - i.e. when the text includes both Hebrew/Arabic and English/numbers then simply
                // trimming characters from the end of the text (as done with LTR) won't do.
                // Instead we simple reposition the text, and draw the 3 points, this is quite simple, but
                // the downside is that a part of a letter may be shown here as well.

                if (rtl) {
                    if ((!isTickerRunning) && endsWith3Points) {
                        String points = "...";
                        int pointsW = impl.stringWidth(nativeFont, points);
                        drawString(nativeFont, points, shiftText + x, y, textDecoration, fontHeight);
                        clipRect(pointsW + shiftText + x, y, textSpaceW - pointsW, fontHeight);
                    }
                    x = x - txtW + textSpaceW;
                } else if (endsWith3Points) {
                    String points = "...";
                    int index = 1;
                    int widest = impl.charWidth(nativeFont, 'W');
                    int pointsW = impl.stringWidth(nativeFont, points);
                    while (fastCharWidthCheck(text, index, textSpaceW - pointsW, widest, nativeFont) && index < text.length()) {
                        index++;
                    }
                    text = text.substring(0, Math.min(text.length(), Math.max(1, index - 1))) + points;
                    txtW = impl.stringWidth(nativeFont, text);
                }
            }
        }

        drawString(nativeFont, text, shiftText + x, y, textDecoration, fontHeight);
        return Math.min(txtW, textSpaceW);
    }

    /**
     * Draw a string using the current font and color in the x,y coordinates.
     * The font is drawn from the top position and not the baseline.
     *
     * @param nativeFont the font used
     * @param str the string to be drawn.
     * @param x the x coordinate.
     * @param y the y coordinate.
     * @param textDecoration Text decoration bitmask (See Style's
     * TEXT_DECORATION_* constants)
     */
    private void drawString(Object nativeFont, String str, int x, int y, int textDecoration, int fontHeight) {
        if (str.length() == 0) {
            return;
        }

        // this if has only the minor effect of providing a slighly faster execution path
        if (textDecoration != 0) {
            boolean raised = (textDecoration & Style.TEXT_DECORATION_3D) != 0;
            boolean lowerd = (textDecoration & Style.TEXT_DECORATION_3D_LOWERED) != 0;
            boolean north = (textDecoration & Style.TEXT_DECORATION_3D_SHADOW_NORTH) != 0;
            if (raised || lowerd || north) {
                textDecoration = textDecoration & (~Style.TEXT_DECORATION_3D) & (~Style.TEXT_DECORATION_3D_LOWERED) & (~Style.TEXT_DECORATION_3D_SHADOW_NORTH);
                int c = getColor();
                int a = getAlpha();
                int newColor = 0;
                int offset = -2;
                if (lowerd) {
                    offset = 2;
                    newColor = 0xffffff;
                } else if (north) {
                    offset = 2;
                }
                setColor(newColor);
                if (a == 0xff) {
                    setAlpha(140);
                }
                drawString(nativeFont, str, x, y + offset, textDecoration, fontHeight);
                setAlpha(a);
                setColor(c);
                drawString(nativeFont, str, x, y, textDecoration, fontHeight);
                return;
            }
            canvas.drawText(str, x, y - font.getFontAscent(), font);
            if ((textDecoration & Style.TEXT_DECORATION_UNDERLINE) != 0) {
                paint.setStyle(Paint.Style.FILL);
                canvas.drawLine(x, y + fontHeight - 1, x + impl.stringWidth(nativeFont, str), y + fontHeight - 1, paint);
            }
            if ((textDecoration & Style.TEXT_DECORATION_STRIKETHRU) != 0) {
                paint.setStyle(Paint.Style.FILL);
                canvas.drawLine(x, y + fontHeight / 2, x + impl.stringWidth(nativeFont, str), y + fontHeight / 2, paint);
            }
            if ((textDecoration & Style.TEXT_DECORATION_OVERLINE) != 0) {
                paint.setStyle(Paint.Style.FILL);
                canvas.drawLine(x, y, x + impl.stringWidth(nativeFont, str), y, paint);
            }
        } else {
            canvas.drawText(str, x, y - font.getFontAscent(), font);
        }
    }

    /**
     * Reverses alignment in the case of bidi
     */
    protected final int reverseAlignForBidi(boolean rtl, int align) {
        if (rtl) {
            switch (align) {
                case Component.RIGHT:
                    return Component.LEFT;
                case Component.LEFT:
                    return Component.RIGHT;
            }
        }
        return align;
    }

    public void fillRoundRect(int x, int y, int width,
            int height, int arcWidth, int arcHeight) {
        paint.setStyle(Paint.Style.FILL);
        this.tmprectF.set(x, y, x + width, y + height);
        canvas.save();
        applyTransform();
        canvas.drawRoundRect(this.tmprectF, arcWidth, arcHeight, paint);
        unapplyTransform();
        canvas.restore();
    }

    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int alpha) {
        if(alpha != this.alpha) {
            this.alpha = alpha;
            paint.setAlpha(alpha);
            paint.setXfermode(PORTER);
        }
    }

    private void freshClip() {
        if(!clipFresh) {
            clipFresh = true;
            canvas.save();
            applyTransform();
            canvas.getClipBounds(this.tmprect);
            unapplyTransform();
            canvas.restore();
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
        //System.out.println("Setting clip  "+x+","+y+","+width+", "+height);
        clipFresh = false;
        if (getTransform().isIdentity() || transformSemaphore > 0) {
            canvas.clipRect(x, y, x + width, y + height, Region.Op.REPLACE);
        } else {
            this.tmppath.rewind();
            this.tmppath.addRect((float) x, (float) y, (float) width + x, (float) height + y, Path.Direction.CW);
            this.tmppath.transform(getTransformMatrix());
            canvas.clipPath(this.tmppath, Region.Op.REPLACE);
        }
    }

    public void setClipRaw(int x, int y, int width, int height) {
        //System.out.println("Setting clip raw "+x+","+y+","+width+", "+height);
        clipFresh = false;
        if (!getTransform().isIdentity() && transformSemaphore > 0) {
            // If the transform is currently applied, then we need to
            // apply the inverse transform to the clip path here because
            // the "raw" variant always passes clips in global "screen" coordinates.
            this.tmppath.rewind();
            this.tmppath.addRect((float) x, (float) y, (float) width + x, (float) height + y, Path.Direction.CW);
            this.tmppath.transform(getInverseTransform());
            canvas.clipPath(this.tmppath, Region.Op.REPLACE);
        } else {
            canvas.clipRect(x, y, x + width, y + height, Region.Op.REPLACE);
        }
    }

    public void setClip(Shape clipShape) {
        //System.out.println("Setting clip to shape "+clipShape);
        clipFresh = false;
        this.tmppath.rewind();
        AndroidImplementation.cn1ShapeToAndroidPath(clipShape, this.tmppath);
        if (!getTransform().isIdentity() && transformSemaphore == 0) {
            this.tmppath.transform(getTransformMatrix());
        }
        canvas.clipPath(this.tmppath, Region.Op.REPLACE);
    }

    /**
     * Sets the clip to the provided raw path.  This path won't be transformed
     * using the current transform matrix.  It will be applied directly.
     * @param path
     */
    public void setClipRaw(Path path) {
        //System.out.println("setting clip to raw "+path);
        clipFresh = false;
        if (!getTransform().isIdentity() && transformSemaphore > 0) {
            // If the transform is currently applied, then we need to
            // apply the inverse transform to the clip path here because
            // the "raw" variant always passes clips in global "screen" coordinates.
            this.tmppath.set(path);
            this.tmppath.transform(getInverseTransform());
            canvas.clipPath(this.tmppath, Region.Op.REPLACE);
        } else {
            canvas.clipPath(path, Region.Op.REPLACE);
        }
    }
    
    public void clipRect(int x, int y, int width, int height) {
        //System.out.println("Clipping rect "+x+","+y+","+width+", "+height);
        clipFresh = false;
        if (getTransform().isIdentity() || transformSemaphore > 0) {
            canvas.clipRect(x, y, x + width, y + height, Region.Op.INTERSECT);
        } else {
            this.tmppath.rewind();
            this.tmppath.addRect(x, y, x + width, y + height, Path.Direction.CW);
            this.tmppath.transform(getTransformMatrix());

            canvas.clipPath(this.tmppath, Region.Op.INTERSECT);
        }
    }

    public int getColor() {
        return paint.getColor();
    }

    public void resetAffine() {
        getTransform().setIdentity();
        transformDirty = true;
        inverseTransformDirty = true;
        clipFresh = false;
        //canvas.restore();
        //canvas.save();
    }

    public void scale(float x, float y) {
        getTransform().scale(x, y);
        transformDirty = true;
        inverseTransformDirty = true;
        clipFresh = false;

    }

    public void rotate(float angle) {
        getTransform().rotate(angle, 0, 0);
        transformDirty = true;
        inverseTransformDirty = true;
        clipFresh = false;
    }

    public void drawView(final View v, AndroidAsyncView.LayoutParams lp) {
    }

    public void rotate(float angle, int x, int y) {
        getTransform().rotate(angle, x, y);
        transformDirty = true;
        inverseTransformDirty = true;
        clipFresh = false;
    }

    public final void fillBitmap(int color) {
        canvas.drawColor(color, PorterDuff.Mode.SRC_OVER);
    }

    public void drawPath(Path p, Stroke stroke) {
        paint.setStyle(Paint.Style.STROKE);
        Stroke old = setStroke(stroke);
        //canvas.save();
        //System.out.println("Drawing path with transform "+getTransform());
        //applyTransform();
        //System.out.println("Transform semaphore "+transformSemaphore);
        if (getTransform().isIdentity()) {
            canvas.drawPath(p, paint);
        } else {
            RectF bounds = new RectF();
            p.computeBounds(bounds, false);
            Path p2 = new Path();
            p.transform(getTransformMatrix(), p2);
            RectF bounds2 = new RectF();
            p2.computeBounds(bounds2, false);
            float ratio = Math.max(bounds2.width()/bounds.width(), bounds2.height()/bounds.height());
            if (ratio > 2 && !isMutableImageGraphics) {
                // If the canvas is hardware accelerated, then it will rasterize the path
                // first, then apply the transform which leads to blurry paths if the transform does
                // significant scaling.
                // In such cases, we
                float strokeWidthUpperBound = ratio * stroke.getLineWidth();
                Bitmap nativeBuffer = Bitmap.createBitmap(
                        (int)(bounds2.width()+2*strokeWidthUpperBound), (int)(bounds2.height()+2*strokeWidthUpperBound), Bitmap.Config.ARGB_8888);
                //int restorePoint = canvas.saveLayer(bounds2, paint, Canvas.ALL_SAVE_FLAG);
                Canvas c = new Canvas(nativeBuffer);
                Matrix translateM = new Matrix();
                translateM.set(getTransformMatrix());
                translateM.postTranslate(-bounds2.left + strokeWidthUpperBound, -bounds2.top + strokeWidthUpperBound);
                c.concat(translateM);
                c.drawPath(p, paint);
                canvas.drawBitmap(nativeBuffer, bounds2.left-strokeWidthUpperBound, bounds2.top-strokeWidthUpperBound, paint);

            } else {
                canvas.save();
                applyTransform();
                canvas.drawPath(p, paint);
                unapplyTransform();
                canvas.restore();
            }
        }
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

        if (getTransform().isIdentity()) {
            canvas.drawPath(p, paint);
        } else {
            RectF bounds = new RectF();
            p.computeBounds(bounds, false);
            Path p2 = new Path();
            p.transform(getTransformMatrix(), p2);
            RectF bounds2 = new RectF();
            p2.computeBounds(bounds2, false);
            float ratio = Math.max(bounds2.width()/bounds.width(), bounds2.height()/bounds.height());
            if (ratio > 2 && !isMutableImageGraphics) {
                // If the canvas is hardware accelerated, then it will rasterize the path
                // first, then apply the transform which leads to blurry paths if the transform does
                // significant scaling.
                // In such cases, we
                Bitmap nativeBuffer = Bitmap.createBitmap(
                        (int)(bounds2.width()), (int)(bounds2.height()), Bitmap.Config.ARGB_8888);
                //int restorePoint = canvas.saveLayer(bounds2, paint, Canvas.ALL_SAVE_FLAG);
                Canvas c = new Canvas(nativeBuffer);
                Matrix translateM = new Matrix();
                translateM.set(getTransformMatrix());
                translateM.postTranslate(-bounds2.left, -bounds2.top);
                c.concat(translateM);
                c.drawPath(p, paint);
                canvas.drawBitmap(nativeBuffer, bounds2.left, bounds2.top, paint);

            } else {
                canvas.save();
                applyTransform();
                canvas.drawPath(p, paint);
                unapplyTransform();
                canvas.restore();
            }
        }

    }

    public void setTransform(Transform transform) {
        Transform t = getTransform();
        if (t != transform) {
            t.setTransform(transform);
        }
        transformDirty = true;
        inverseTransformDirty = true;
        clipFresh = false;
    }

    public Transform getTransform() {
        if (transform == null) {
            transform = Transform.makeIdentity();
        }
        return transform;
    }


    private int transformSemaphore = 0;
    public void applyTransform() {
        if (transformSemaphore == 0) {
            canvas.concat(getTransformMatrix());
        }
        transformSemaphore++;
    }

    public void unapplyTransform() {
        transformSemaphore--;
        if (transformSemaphore < 0) {
            new RuntimeException("TransformSemaphore unbalanced").printStackTrace();
        }
    }

}
