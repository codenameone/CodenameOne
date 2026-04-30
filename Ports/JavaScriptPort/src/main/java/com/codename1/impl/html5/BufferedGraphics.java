/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;

import com.codename1.impl.html5.HTML5Implementation.NativeFont;
import com.codename1.impl.html5.HTML5Implementation.NativeImage;
import com.codename1.impl.html5.graphics.ClearRect;
import com.codename1.impl.html5.graphics.ClipRect;
import com.codename1.impl.html5.graphics.ClipShape;
import com.codename1.impl.html5.graphics.DrawArc;
import com.codename1.impl.html5.graphics.DrawImage;
import com.codename1.impl.html5.graphics.DrawLine;
import com.codename1.impl.html5.graphics.DrawPolygon;
import com.codename1.impl.html5.graphics.DrawRect;
import com.codename1.impl.html5.graphics.DrawRoundRect;
import com.codename1.impl.html5.graphics.DrawShape;
import com.codename1.impl.html5.graphics.DrawString;
import com.codename1.impl.html5.graphics.ExecutableOp;
import com.codename1.impl.html5.graphics.FillArc;
import com.codename1.impl.html5.graphics.FillLinearGradient;
import com.codename1.impl.html5.graphics.FillPolygon;
import com.codename1.impl.html5.graphics.FillRadialGradient;
import com.codename1.impl.html5.graphics.FillRect;
import com.codename1.impl.html5.graphics.FillRoundRect;
import com.codename1.impl.html5.graphics.FillShape;
import com.codename1.impl.html5.graphics.SetTransform;
import com.codename1.impl.html5.graphics.TileImage;
import com.codename1.teavm.geom.JSAffineTransform;
import com.codename1.ui.Stroke;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;
import java.util.ArrayList;
import java.util.List;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.dom.HTMLCanvasElement;

/**
 *
 * @author shannah
 */
public class BufferedGraphics extends HTML5Graphics {
    ArrayList<ExecutableOp> upcoming = new ArrayList<ExecutableOp>();
    private Rectangle clipRect;
    private Rectangle clip=new Rectangle();
    private Rectangle clipBounds=new Rectangle();
    private boolean clipBoundsDirty=true;
    private GeneralPath clipShape = new GeneralPath();

    private boolean isClipShape;
    private Transform transform, clipTransform;
    private boolean transformApplied=false;
    private final JavaScriptPrimitiveRenderAdapter<NativeFont, ExecutableOp> primitiveRenderAdapter =
            new JavaScriptPrimitiveRenderAdapter<NativeFont, ExecutableOp>(getRenderState(),
                    new JavaScriptPrimitiveRenderAdapter.OperationSink<ExecutableOp>() {
                        @Override
                        public void submit(ExecutableOp operation) {
                            upcoming.add(operation);
                        }
                    }, JavaScriptExecutableOpFactory.INSTANCE);
    private final JavaScriptImageTransformRenderAdapter<NativeImage, Shape, JSAffineTransform, ExecutableOp> imageTransformRenderAdapter =
            new JavaScriptImageTransformRenderAdapter<NativeImage, Shape, JSAffineTransform, ExecutableOp>(getRenderState(),
                    new JavaScriptImageTransformRenderAdapter.OperationSink<ExecutableOp>() {
                        @Override
                        public void submit(ExecutableOp operation) {
                            upcoming.add(operation);
                        }
                    }, JavaScriptExecutableOpFactory.INSTANCE);
    private final JavaScriptShapeGradientRenderAdapter<Shape, Stroke, ExecutableOp> shapeGradientRenderAdapter =
            new JavaScriptShapeGradientRenderAdapter<Shape, Stroke, ExecutableOp>(getRenderState(),
                    new JavaScriptShapeGradientRenderAdapter.OperationSink<ExecutableOp>() {
                        @Override
                        public void submit(ExecutableOp operation) {
                            upcoming.add(operation);
                        }
                    }, JavaScriptExecutableOpFactory.INSTANCE);
    
    public BufferedGraphics(HTML5Implementation impl, HTMLCanvasElement canvas) {
        super(impl, canvas);
    }

    @Override
    public void drawImage(Object img, int x, int y) {
        imageTransformRenderAdapter.drawImage((NativeImage)img, x, y);
    }

    @Override
    public void drawImage(Object img, int x, int y, int w, int h) {
        imageTransformRenderAdapter.drawImage((NativeImage)img, x, y, w, h);
    }

    @Override
    public void tileImage(Object img, int x, int y, int w, int h) {
        imageTransformRenderAdapter.tileImage((NativeImage)img, x, y, w, h);
    }
    
    

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        upcoming.add(new DrawArc(x, y, width, height, startAngle, arcAngle, getColor(), getAlpha()));
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        primitiveRenderAdapter.fillRect(x, y, width, height);
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        primitiveRenderAdapter.clearRect(x, y, width, height);
    }
    
    

    @Override
    public void drawRect(int x, int y, int width, int height) {
        primitiveRenderAdapter.drawRect(x, y, width, height);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        primitiveRenderAdapter.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        upcoming.add(new DrawRoundRect(x, y, width, height, arcWidth, arcHeight, getColor(), getAlpha()));
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        upcoming.add(new FillRoundRect(x, y, width, height, arcWidth, arcHeight, getColor(), getAlpha()));
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        upcoming.add(new DrawPolygon(xPoints, yPoints, nPoints, getColor(), getAlpha()));
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        upcoming.add(new FillPolygon(xPoints, yPoints, nPoints, getColor(), getAlpha()));
    }

    @Override
    public void drawShape(Shape shape, Stroke stroke) {
        shapeGradientRenderAdapter.drawShape(shape, stroke);
    }
    
    @Override
    public void fillShape(Shape shape) {
        shapeGradientRenderAdapter.fillShape(shape);
    }

    @Override
    public void setTransform(Transform t) {
        setTransform(t, true);
    }

    @Override
    public void setTransform(Transform t, boolean replace) {
        if (transform == null || replace) {
            transform = t;
        } else if (!replace) {
            transform.concatenate(t);
        }
        setTransformChanged();
        applyTransform();
    }

    @Override
    public void applyTransform() {
        if (!transformApplied) {
            imageTransformRenderAdapter.applyTransform(((JSAffineTransform)transform.getNativeTransform()).cloneTransform(), true);
            transformApplied = true;
        }
    }
    
    @Override
    public void setTransformChanged() {
        transformApplied = false;
        clipBoundsDirty = true;
    }
    
    @Override
    public Transform getTransform() {
        if (transform == null) {
            transform = Transform.makeIdentity();
        }
        return transform;
    }

    @Override
    public void resetAffine() {
        if (transform != null && !transform.isIdentity()) {
            transform.setIdentity();
            setTransformChanged();
            applyTransform();
        }
    }

    @JSBody(params={"str"}, script="console.log(str)")
    private native static void log(String str);
    
    
    @JSBody(params={"str"}, script="console.log(str)")
    private native static void log(JSObject str);
    
    @Override
    public void rotate(double angle) {
        if (transform == null) transform = Transform.makeIdentity();
        transform.rotate((float)angle, 0, 0);
        setTransformChanged();
        applyTransform();
    }

    @Override
    public void rotate(double angle, int pivotX, int pivotY) {
        if (transform == null) transform = Transform.makeIdentity();
        transform.rotate((float)angle, pivotX, pivotY);
        setTransformChanged();
        applyTransform();
    }

    @Override
    public void scale(double sx, double sy) {
        if (transform == null) transform = Transform.makeIdentity();
        transform.scale((float)sx, (float)sy);
        setTransformChanged();
        applyTransform();
    }

    //@Override
    //public void shear(double shx, double shy) {
    //    setTransform(JSAffineTransform.Factory.getShearInstance(shx, shy), false);
    //}
    
    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        upcoming.add(new FillArc(x, y, width, height, startAngle, arcAngle, getColor(), getAlpha()));
    }

    @Override
    public void drawRGB(int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
        if (offset != 0){
            int[] copy = new int[w*h];
            System.arraycopy(rgbData, offset, copy, 0, w*h);
            rgbData = copy;
        }
        NativeImage img = (NativeImage)impl.createImage(rgbData, w, h);
        drawImage(img, x, y, w, h);
        
    }

    @Override
    public void drawString(String str, int x, int y) {
        primitiveRenderAdapter.drawString(str, x, y);
    }

    @Override
    void setAlpha(int alpha) {
        getRenderState().setAlpha(alpha);
    }

    @Override
    void setColor(int color) {
        getRenderState().setColor(color);
    }

    @Override
    void setFont(NativeFont font) {
        getRenderState().setFont(font);
    }
    
    List<ExecutableOp> flush(int x, int y, int width, int height){
        List<ExecutableOp> current;
        synchronized(upcoming){
            current = new ArrayList<ExecutableOp>(upcoming.size());
            for (int i = 0; i < upcoming.size(); i++) {
                current.add(upcoming.get(i));
            }
            upcoming.clear();
        }
        return current;
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
                out.setRect(clip, transform.getInverse());
                return out;
            } else {
                return clip;
            }
        }
    }
    
    @Override
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

    @Override
    public void pushClip() {
        clipStack.add(new ClipFrame(clip, clipShape, isClipShape, clipTransform));
    }

    @Override
    public void popClip() {
        if (clipStack.isEmpty()) {
            return;
        }
        ClipFrame frame = clipStack.remove(clipStack.size() - 1);
        if (frame.isShape) {
            if (frame.transform != null && !frame.transform.isIdentity()) {
                Transform savedTransform = transform;
                transform = frame.transform;
                try {
                    setClip(frame.shape);
                } finally {
                    transform = savedTransform;
                }
            } else {
                setClip(frame.shape);
            }
        } else {
            setClip(frame.rect.getX(), frame.rect.getY(), frame.rect.getWidth(), frame.rect.getHeight());
        }
    }
    
    private void clipShape(Shape shape) {
        if (!isClipShape) {
            isClipShape = true;
            clipShape.reset();
            clipShape.setShape(clip, null);
            clipTransform = null;
        }
        GeneralPath p = (GeneralPath)getCurrentClipProjection();
        p.intersect(shape);
        setClip(p);
    }
    

    @Override
    public void setClip(int x, int y, int width, int height) {
        if (transform != null && !transform.isIdentity()) {
            setClip(new Rectangle(x, y, width, height));
            return;
        }
        isClipShape = false;
        clip.setBounds(x, y, width, height);
        clipBoundsDirty = true;
        primitiveRenderAdapter.setClipRect(x, y, width, height);
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        Rectangle rect = new Rectangle(x, y, width, height);
        if (isClipShape || transform != null && !transform.isIdentity()) {
            clipShape(rect);
            return;
        }
        
        if (rect.contains(clip)) {
            return;
        }
        clip = clip.intersection(x, y, width, height);
        clipBoundsDirty = true;
        primitiveRenderAdapter.setClipRect(clip.getX(), clip.getY(), clip.getWidth(), clip.getHeight());
    }
    
    private void calculateClipBounds() {
        if (clipBoundsDirty) {
            clipBoundsDirty = false;
            Rectangle projectedShape = getCurrentClipProjection().getBounds();
            clipBounds.setBounds(projectedShape.getX(), projectedShape.getY(), projectedShape.getWidth(), projectedShape.getHeight());
        }
    }
    
    @Override
    public int getClipHeight() {
        calculateClipBounds();
        return clipBounds.getHeight();
    }

    @Override
    public int getClipWidth() {
        calculateClipBounds();
        return clipBounds.getWidth();
    }

    @Override
    public int getClipX() {
        calculateClipBounds();
        return clipBounds.getX();
    }

    @Override
    public int getClipY() {
        calculateClipBounds();
        return clipBounds.getY();
    }
 
    
    @Override
    public void fillLinearGradient(int x, int y, int width, int height, int startColor, int endColor, boolean horizontal) {
        shapeGradientRenderAdapter.fillLinearGradient(x, y, width, height, startColor, endColor, horizontal);
    }

    @Override
    public void fillRadialGradient(int startColor, int endColor, int x, int y, int width, int height, int startAngle, int arcAngle) {
        shapeGradientRenderAdapter.fillRadialGradient(x, y, width, height, startColor, endColor, startAngle, arcAngle);
    }
    
    @Override
    public void fillRadialGradient(int startColor, int endColor, int x, int y, int width, int height) {
        shapeGradientRenderAdapter.fillRadialGradient(x, y, width, height, startColor, endColor, 0, 360);
    }

    public void fillRectRadialGradient(int startColor, int endColor, int x, int y, int width, int height,
            float relativeX, float relativeY, float relativeSize) {
        shapeGradientRenderAdapter.fillRectRadialGradient(x, y, width, height, startColor, endColor, relativeX, relativeY, relativeSize);
    }
    
    @Override
    public int getAlpha() {
        return getRenderState().getAlpha();
    }

    @Override
    public int getColor() {
        return getRenderState().getColor();
    }

    @Override
    public NativeFont getFont() {
        return getRenderState().getFont();
    }
    
    
}
