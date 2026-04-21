/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;




import com.codename1.impl.html5.HTML5Implementation.NativeFont;
import com.codename1.impl.html5.HTML5Implementation.NativeImage;
import com.codename1.impl.html5.JSOImplementations.JSFontMetrics;
import com.codename1.impl.html5.graphics.ClearRect;
import com.codename1.impl.html5.graphics.ClipRect;
import com.codename1.impl.html5.graphics.ClipShape;
import com.codename1.impl.html5.graphics.ClipState;
import com.codename1.impl.html5.graphics.DrawArc;
import com.codename1.impl.html5.graphics.DrawImage;
import com.codename1.impl.html5.graphics.DrawLine;
import com.codename1.impl.html5.graphics.DrawPolygon;
import com.codename1.impl.html5.graphics.DrawRect;
import com.codename1.impl.html5.graphics.DrawRoundRect;
import com.codename1.impl.html5.graphics.DrawShape;
import com.codename1.impl.html5.graphics.DrawString;
import com.codename1.impl.html5.graphics.FillArc;
import com.codename1.impl.html5.graphics.FillLinearGradient;
import com.codename1.impl.html5.graphics.FillPolygon;
import com.codename1.impl.html5.graphics.FillRadialGradient;
import com.codename1.impl.html5.graphics.FillRect;
import com.codename1.impl.html5.graphics.FillRoundRect;
import com.codename1.impl.html5.graphics.FillShape;
import com.codename1.impl.html5.graphics.SetTransform;
import com.codename1.impl.html5.graphics.TileImage;
import com.codename1.impl.html5.graphics.ExecutableOp;
import com.codename1.teavm.geom.JSAffineTransform;
import com.codename1.ui.Stroke;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;

import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;
import com.codename1.html5.js.canvas.ImageData;
import com.codename1.html5.js.dom.HTMLCanvasElement;

/**
 * #######################################################################
 * #######################################################################
 *
 * Bundle one canvas and two paints to get one graphics object.
 */
public class HTML5Graphics {
    private final JavaScriptRenderState<NativeFont> renderState = new JavaScriptRenderState<NativeFont>();
    private Runnable mutationListener;
    private HTMLCanvasElement canvas;
    private CanvasRenderingContext2D context;
    //private Paint paint;
    HTML5Implementation impl;
    private boolean inClip = false;
    private Rectangle clipBounds=new Rectangle();
    private boolean clipBoundsDirty=true;
    private GeneralPath clipShape = new GeneralPath();
    
    private boolean isClipShape;
    private Transform transform, clipTransform;
    private boolean transformApplied = false;
   
    
    private final Rectangle clipRect = new Rectangle();
    private final JavaScriptPrimitiveRenderAdapter<NativeFont, ExecutableOp> primitiveRenderAdapter =
            new JavaScriptPrimitiveRenderAdapter<NativeFont, ExecutableOp>(renderState,
                    new JavaScriptPrimitiveRenderAdapter.OperationSink<ExecutableOp>() {
                        @Override
                        public void submit(ExecutableOp operation) {
                            notifyMutation();
                            operation.execute(context);
                        }
                    }, JavaScriptExecutableOpFactory.INSTANCE);
    private final JavaScriptImageTransformRenderAdapter<NativeImage, Shape, JSAffineTransform, ExecutableOp> imageTransformRenderAdapter =
            new JavaScriptImageTransformRenderAdapter<NativeImage, Shape, JSAffineTransform, ExecutableOp>(renderState,
                    new JavaScriptImageTransformRenderAdapter.OperationSink<ExecutableOp>() {
                        @Override
                        public void submit(ExecutableOp operation) {
                            notifyMutation();
                            operation.execute(context);
                        }
                    }, JavaScriptExecutableOpFactory.INSTANCE);
    private final JavaScriptShapeGradientRenderAdapter<Shape, Stroke, ExecutableOp> shapeGradientRenderAdapter =
            new JavaScriptShapeGradientRenderAdapter<Shape, Stroke, ExecutableOp>(renderState,
                    new JavaScriptShapeGradientRenderAdapter.OperationSink<ExecutableOp>() {
                        @Override
                        public void submit(ExecutableOp operation) {
                            notifyMutation();
                            operation.execute(context);
                        }
                    }, JavaScriptExecutableOpFactory.INSTANCE);
    
    //private final Path tmppath = new Path();
    //private final static PorterDuffXfermode PORTER = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
    
    HTML5Graphics(HTML5Implementation impl, HTMLCanvasElement canvas) {
        this.canvas = canvas;
        this.context = (CanvasRenderingContext2D)canvas.getContext("2d");
        
        this.impl = impl;
        this.clipRect.setWidth(canvas.getWidth());
        this.clipRect.setHeight(canvas.getHeight());
        //transform = JSAffineTransform.Factory.getTranslateInstance(0, 0);
        //paint.setAntiAlias(true);
        
        if(context != null) {
            context.save();
        }
        //transform = Transform.makeIdentity();
    }
    
    
    public ClipState getClipState() {
        return renderState.getClipState();
    }

    protected final JavaScriptRenderState<NativeFont> getRenderState() {
        return renderState;
    }

    public HTMLCanvasElement getCanvas(){
        return canvas;
    }
    
    void setCanvas(HTMLCanvasElement canvas) {
        this.canvas = canvas;
        this.context = null;
        if(canvas != null) {
            this.context = (CanvasRenderingContext2D)canvas.getContext("2d");
            context.save();
        }
    }

    void setCanvasNoSave(HTMLCanvasElement canvas) {
        this.canvas = canvas;
        this.context = null;
        if(canvas != null) {
            this.context = (CanvasRenderingContext2D)canvas.getContext("2d");
            
        }
    }

    NativeFont getFont() {
        return renderState.getFont();
    }

    void setFont(NativeFont font) {
        renderState.setFont(font);
        context.setFont(font.getCSS());
        
        
    }

    public static String color(int rgb){
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        //int alpha = (rgb >> 24) & 0xFF;
        return "rgb("+red+","+green+","+blue+")";
    }
    
    public static String colorWithAlpha(int argb) {
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        int alpha = (argb >> 24) & 0xFF;
        return "rgba("+red+","+green+","+blue+","+(alpha/255f)+")";
    }
    
    void setColor(int color){
    	//System.out.println("Setting color "+color(color));
        renderState.setColor(color);
        this.context.setFillStyle(color(color));
        this.context.setStrokeStyle(color(color));
        
    }
    
    void setColorWithAlpha(int color) {
        //System.out.println("Setting color "+color(color));
        renderState.setColor(color);
        this.context.setFillStyle(colorWithAlpha(color));
        this.context.setStrokeStyle(colorWithAlpha(color));
    }
    
    void setAlpha(int alpha) {
        renderState.setAlpha(alpha);
        this.context.setGlobalAlpha(alpha / 255.0);
    }
    
    int getAlpha() {
        return renderState.getAlpha();
        //return (int)(context.getGlobalAlpha() * 255);
    }

    CanvasRenderingContext2D getContext() {
        return context;
    }

    void setMutationListener(Runnable mutationListener) {
        this.mutationListener = mutationListener;
    }

    private void notifyMutation() {
        if (mutationListener != null) {
            mutationListener.run();
        }
    }


    public void drawImage(Object img, int x, int y) {
        imageTransformRenderAdapter.drawImage((NativeImage)img, x, y);
    }
    
    
    public void tileImage(Object img, int x, int y, int w, int h) {
        imageTransformRenderAdapter.tileImage((NativeImage)img, x, y, w, h);
    }
    
    
   
    
    public void setTransform(Transform t) {
        setTransform(t, true);
        
    }
    
    public void applyTransform() {
        if (!transformApplied) {
            if (transform == null) {
                transform = Transform.makeIdentity();
            }
            imageTransformRenderAdapter.applyTransform((JSAffineTransform)transform.getNativeTransform(), true);
            transformApplied = true;
        }
    }
    
    public void setTransformChanged() {
        transformApplied = false;
        clipBoundsDirty = true;
    }
    
    public void setTransform(Transform t, boolean replace) {
        if (transform == null) {
            transform = Transform.makeIdentity();
        }
        if (replace) {
            transform = t; 
        } else {
            transform.concatenate(t);
        }
        setTransformChanged();
        applyTransform();
    }
    
    public boolean isTransformSupported() {
        return true;
    }
    
    public void rotate(double angle) {
        if (transform != null) {
            transform.rotate((float)angle, 0, 0);
            setTransformChanged();
            applyTransform();
        } else {
            setTransform(Transform.makeRotation((float)angle, 0, 0), false);
        }
    }
    
    public void rotate(double angle, int pivotX, int pivotY) {
        if (transform != null) {
            transform.rotate((float)angle, pivotX, pivotY);
            setTransformChanged();
            applyTransform();
        } else {
            setTransform(Transform.makeRotation((float)angle, pivotX, pivotY), false);
        }
    }
    
//    public void shear(double shx, double shy) {
//        
//        transform = transform != null ? transform.shear(shx, shy) :
//                JSAffineTransform.Factory.getShearInstance(shx, shy);
//        setTransform(transform, false);
//        transformDirty = true;
//    }

    public Transform getTransform() {
        if (transform == null) {		
             transform = Transform.makeIdentity();		
        }		
        return transform;
    }
    
    
    
    public void scale(double sx, double sy) {
        if (transform != null) {
            transform.scale((float)sx, (float)sy);
            setTransformChanged();
            applyTransform();
        } else {
            setTransform(Transform.makeScale((float)sx, (float)sy));
        }
    }
    
    public void drawImage(Object img, int x, int y, int w, int h) {
        imageTransformRenderAdapter.drawImage((NativeImage)img, x, y, w, h);
    }

    
    public void drawLine(int x1, int y1, int x2, int y2) {
        primitiveRenderAdapter.drawLine(x1, y1, x2, y2);
    }
    
    
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        new DrawPolygon(xPoints, yPoints, nPoints, getColor(), getAlpha()).execute(context);
    }
    
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        new FillPolygon(xPoints, yPoints, nPoints, getColor(), getAlpha()).execute(context);
    }
    
    public void drawRGB(int[] rgbData, int offset, int x,
            int y, int w, int h, boolean processAlpha) {
        if (offset != 0){
            int[] copy = new int[w*h];
            System.arraycopy(rgbData, offset, copy, 0, w*h);
            rgbData = copy;
        }
        NativeImage img = (NativeImage)impl.createImage(rgbData, w, h);
        drawImage(img, x, y, w, h);
    }
    
    public void drawRect(int x, int y, int width, int height) {
    	primitiveRenderAdapter.drawRect(x, y, width, height);
    }
    
    public void drawRoundRect(int x, int y, int width,
            int height, int arcWidthInt, int arcHeightInt) {
        new DrawRoundRect(x, y, width, height, arcWidthInt, arcHeightInt, getColor(), getAlpha()).execute(context);
    }

    public void drawString(String str, int x, int y) {
        primitiveRenderAdapter.drawString(str, x, y);
    }

    public void drawArc(int x, int y, int width, int height,
            int startAngle, int arcAngle) {
        new DrawArc(x, y, width, height, startAngle, arcAngle, getColor(), getAlpha()).execute(context);
    }
    
    public void drawShape(Shape shape, Stroke stroke) {
        shapeGradientRenderAdapter.drawShape(shape, stroke);
    }
    
    public void fillShape(Shape shape) {
        shapeGradientRenderAdapter.fillShape(shape);
    }
    
    @JSBody(params={"o"}, script="console.log(o)")
    private static native void log(JSObject o);
    
    
    public void fillArc(int x, int y, int width, int height,
            int startAngle, int arcAngle) {
        new FillArc(x, y, width, height, startAngle, arcAngle, getColor(), getAlpha()).execute(context);
    }
    
    public void fillRect(int x, int y, int width, int height) {
        primitiveRenderAdapter.fillRect(x, y, width, height);
    }
    
    public void clearRect(int x, int y, int width, int height) {
        primitiveRenderAdapter.clearRect(x, y, width, height);
    }

    public void fillRoundRect(int x, int y, int width,
            int height, int arcWidthInt, int arcHeightInt) {
        new FillRoundRect(x, y, width, height, arcWidthInt, arcHeightInt, getColor(), getAlpha()).execute(context);
        
    }

    

    
    
    private void calculateClipBounds() {
        if (clipBoundsDirty) {
            clipBoundsDirty = false;
            Rectangle projectedShape = getCurrentClipProjection().getBounds();
            clipBounds.setBounds(projectedShape.getX(), projectedShape.getY(), projectedShape.getWidth(), projectedShape.getHeight());
        }
    }
    
    
    public int getClipHeight() {
        calculateClipBounds();
        return clipBounds.getHeight();
    }

    
    public int getClipWidth() {
        calculateClipBounds();
        return clipBounds.getWidth();
    }

    
    public int getClipX() {
        calculateClipBounds();
        return clipBounds.getX();
    }

    
    public int getClipY() {
        calculateClipBounds();
        return clipBounds.getY();
    }

     private Transform getInverseTransform() {
        if (transform == null) return null;
        return transform.getInverse();
    }
    
    private Shape getCurrentClipProjection() {
        if (isClipShape) {
            GeneralPath out = new GeneralPath(clipShape);
            Transform t = Transform.makeIdentity();
            if (clipTransform != null && !clipTransform.isIdentity()) {
                t.concatenate(clipTransform);
            }
            if (transform != null && !transform.isIdentity()) {
                t.concatenate(transform.getInverse());
            }
            if (!t.isIdentity()) {
                out.transform(t);
            }
            return out;
        } else {
            if (transform != null && !transform.isIdentity()) {
                GeneralPath out = new GeneralPath();
                out.setRect(clipRect, transform.getInverse());
                return out;
            } else {
                return clipRect;
            }
        }
    }
    
 
    public void setClip(Shape shape) {
        clipShape.reset();
        clipShape.setShape(shape, null);
        isClipShape = true;
        clipTransform = transform == null ? null : transform.copy();
        JSAffineTransform t = null;
        if (transform != null) {
            t = (JSAffineTransform)transform.getNativeTransform();
        }
        clipBoundsDirty = true;
        imageTransformRenderAdapter.setClipShape(shape, t);
        //upcoming.add(new ClipShape(shape, t));
    }

    private static final class ClipFrame {
        final Rectangle rect;
        final GeneralPath shape;
        final boolean isShape;
        final Transform transform;

        ClipFrame(Rectangle rect, GeneralPath shape, boolean isShape, Transform transform) {
            this.rect = new Rectangle(rect);
            this.shape = shape == null ? null : new GeneralPath(shape);
            this.isShape = isShape;
            this.transform = transform == null ? null : transform.copy();
        }
    }

    private final java.util.ArrayList<ClipFrame> clipStack = new java.util.ArrayList<ClipFrame>();

    public void pushClip() {
        clipStack.add(new ClipFrame(clipRect, clipShape, isClipShape, clipTransform));
    }

    public void popClip() {
        if (clipStack.isEmpty()) {
            return;
        }
        ClipFrame frame = clipStack.remove(clipStack.size() - 1);
        if (frame.isShape) {
            // Re-apply the saved shape clip. setClip(Shape) re-queues a
            // ClipShape op and refreshes clipTransform, so callers get the
            // same rendering result as when that clip was originally set.
            GeneralPath restored = frame.shape;
            if (frame.transform != null && !frame.transform.isIdentity()) {
                Transform savedTransform = transform;
                transform = frame.transform;
                try {
                    setClip(restored);
                } finally {
                    transform = savedTransform;
                }
            } else {
                setClip(restored);
            }
        } else {
            setClip(frame.rect.getX(), frame.rect.getY(), frame.rect.getWidth(), frame.rect.getHeight());
        }
    }
    
    private void clipShape(Shape shape) {
        if (!isClipShape) {
            isClipShape = true;
            clipShape.reset();
            clipShape.setShape(clipRect, null);
            clipTransform = null;
        }
        GeneralPath p = (GeneralPath)getCurrentClipProjection();
        p.intersect(shape);
        setClip(p);
    }
    
    public void setClip(int x, int y, int width, int height) {
        if (transform != null && !transform.isIdentity()) {
            setClip(new Rectangle(x, y, width, height));
            return;
        }
        isClipShape = false;
        clipRect.setBounds(x, y, width, height);
        clipBoundsDirty = true;
        primitiveRenderAdapter.setClipRect(x, y, width, height);
        
    }
    
   

    public void clipRect(int x, int y, int width, int height) {
        Rectangle rect = new Rectangle(x, y, width, height);
        if (isClipShape || transform != null && !transform.isIdentity()) {
            clipShape(rect);
            return;
        }
        
        if (rect.contains(clipRect)) {
            return;
        }
        clipRect.intersection(rect, clipRect);
        clipBoundsDirty = true;
        primitiveRenderAdapter.setClipRect(clipRect.getX(), clipRect.getY(), clipRect.getWidth(), clipRect.getHeight());
    }

    public int getColor() {
        return renderState.getColor();
    }
    
    public void resetAffine() {
        if (transform == null) {
            transform = Transform.makeIdentity();
        } else {
            transform.setIdentity();
        }
        setTransformChanged();
        applyTransform();
    }

   
    
    
    public int charsWidth(Object nativeFont, char[] ch, int offset, int length) {
        return JavaScriptTextMetricsAdapter.charsWidth(new JavaScriptTextMetricsAdapter.FontMetricsContext() {
            @Override
            public String getCurrentFont() {
                return context.getFont();
            }

            @Override
            public void setCurrentFont(String fontCss) {
                context.setFont(fontCss);
            }

            @Override
            public int measureWidth(String text) {
                return (int)context.measureText(text).getWidth();
            }
        }, new JavaScriptTextMetricsAdapter.FontCssSupplier<NativeFont>() {
            @Override
            public String getCss(NativeFont font) {
                return font.getCSS();
            }

            @Override
            public int getHeight(NativeFont font) {
                return font.fontHeight();
            }

            @Override
            public int getAscent(NativeFont font) {
                return font.fontAscent();
            }
        }, (NativeFont) nativeFont, ch, offset, length);
    }
    
    public int stringWidth(Object nativeFont, String str) {
        return JavaScriptTextMetricsAdapter.stringWidth(new JavaScriptTextMetricsAdapter.FontMetricsContext() {
            @Override
            public String getCurrentFont() {
                return context.getFont();
            }

            @Override
            public void setCurrentFont(String fontCss) {
                context.setFont(fontCss);
            }

            @Override
            public int measureWidth(String text) {
                return (int)context.measureText(text).getWidth();
            }
        }, new JavaScriptTextMetricsAdapter.FontCssSupplier<NativeFont>() {
            @Override
            public String getCss(NativeFont font) {
                return font.getCSS();
            }

            @Override
            public int getHeight(NativeFont font) {
                return font.fontHeight();
            }

            @Override
            public int getAscent(NativeFont font) {
                return font.fontAscent();
            }
        }, (NativeFont) nativeFont, str);
    }
    
    
    
    int getFontHeight(Object nativeFont){
        return JavaScriptTextMetricsAdapter.getFontHeight(new JavaScriptTextMetricsAdapter.FontCssSupplier<NativeFont>() {
            @Override
            public String getCss(NativeFont font) {
                return font.getCSS();
            }

            @Override
            public int getHeight(NativeFont font) {
                return font.fontHeight();
            }

            @Override
            public int getAscent(NativeFont font) {
                return font.fontAscent();
            }
        }, (NativeFont) nativeFont);
    }
    
    int getFontAscent(Object nativeFont){
        return ((NativeFont)nativeFont).fontAscent();
    }
    
    int getFontDescent(Object nativeFont){
        return JavaScriptTextMetricsAdapter.getFontDescent(new JavaScriptTextMetricsAdapter.FontCssSupplier<NativeFont>() {
            @Override
            public String getCss(NativeFont font) {
                return font.getCSS();
            }

            @Override
            public int getHeight(NativeFont font) {
                return font.fontHeight();
            }

            @Override
            public int getAscent(NativeFont font) {
                return font.fontAscent();
            }
        }, (NativeFont) nativeFont);
    }
    
//    int getFontLeading(Object nativeFont){
//        String oldFont = context.getFont();
//        context.setFont(((NativeFont)nativeFont).getCSS());
//        //this.canvas.getStyle().setProperty("font", nativeFont+"");
//        int out = (int)Math.round(((JSFontMetrics)context.measureText(alphabet)).getLeading());
//        context.setFont(oldFont);
//        return out;
//    
//        
//    }
    
    void clear(){
        context.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void fillLinearGradient(int x, int y, int width, int height, int startColor, int endColor, boolean horizontal) {
        shapeGradientRenderAdapter.fillLinearGradient(x, y, width, height, startColor, endColor, horizontal);
    }

    public void fillRadialGradient(int startColor, int endColor, int x, int y, int width, int height, int startAngle, int arcAngle) {
        shapeGradientRenderAdapter.fillRadialGradient(x, y, width, height, startColor, endColor, startAngle, arcAngle);
    }

    public void fillRadialGradient(int startColor, int endColor, int x, int y, int width, int height) {
       fillRadialGradient(startColor, endColor, x, y, width, height, 0, 360);
    }

    public void fillRectRadialGradient(int startColor, int endColor, int x, int y, int width, int height,
            float relativeX, float relativeY, float relativeSize) {
        shapeGradientRenderAdapter.fillRectRadialGradient(x, y, width, height, startColor, endColor, relativeX, relativeY, relativeSize);
    }

    
    
    
    
    
    
    
}
