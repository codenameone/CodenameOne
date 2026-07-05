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
    // True when the current clip encloses no area. Tracked reliably via
    // clipBoundsTracker (a clamped user-space rect intersection) because the
    // projected clip bounds are unreliable for an empty clip on the shape path
    // (an empty GeneralPath's getBounds() returns uninitialised extents, and a
    // degenerate clip path does not cull image blits/fills on the host). This is
    // the DISPLAY/screen graphics class (BufferedGraphics extends HTML5Graphics
    // and overrides the clip/draw methods); the same fix lives in both. #5263.
    private boolean clipEmpty;
    private final Rectangle clipBoundsTracker = new Rectangle();
    private Transform transform, clipTransform;
    private boolean transformApplied=false;
    private final JavaScriptPrimitiveRenderAdapter<NativeFont, ExecutableOp> primitiveRenderAdapter =
            new JavaScriptPrimitiveRenderAdapter<NativeFont, ExecutableOp>(getRenderState(),
                    new JavaScriptPrimitiveRenderAdapter.OperationSink<ExecutableOp>() {
                        @Override
                        public void submit(ExecutableOp operation) {
                            addOp(operation);
                        }
                    }, JavaScriptExecutableOpFactory.INSTANCE);
    private final JavaScriptImageTransformRenderAdapter<NativeImage, Shape, JSAffineTransform, ExecutableOp> imageTransformRenderAdapter =
            new JavaScriptImageTransformRenderAdapter<NativeImage, Shape, JSAffineTransform, ExecutableOp>(getRenderState(),
                    new JavaScriptImageTransformRenderAdapter.OperationSink<ExecutableOp>() {
                        @Override
                        public void submit(ExecutableOp operation) {
                            addOp(operation);
                        }
                    }, JavaScriptExecutableOpFactory.INSTANCE);
    private final JavaScriptShapeGradientRenderAdapter<Shape, Stroke, ExecutableOp> shapeGradientRenderAdapter =
            new JavaScriptShapeGradientRenderAdapter<Shape, Stroke, ExecutableOp>(getRenderState(),
                    new JavaScriptShapeGradientRenderAdapter.OperationSink<ExecutableOp>() {
                        @Override
                        public void submit(ExecutableOp operation) {
                            addOp(operation);
                        }
                    }, JavaScriptExecutableOpFactory.INSTANCE);
    
    public BufferedGraphics(HTML5Implementation impl, int width, int height) {
        // The display draws onto the well-known display surface; the host binds
        // it lazily to the output canvas. No worker-side canvas/context proxy.
        super(impl, HTML5Implementation.DISPLAY_SURFACE_ID, width, height);
        clipBoundsTracker.setBounds(0, 0, width, height);
    }

    // Single chokepoint for buffered ops. When the clip is empty, drop every
    // DRAW op -- nothing may render -- but still record clip and transform ops so
    // a later (non-empty) clip restores drawing. This is the reliable cull: a
    // degenerate empty-clip path does not cull fills or image blits on the host
    // canvas, so we must not emit the draws at all. Mirrors the GL clipBlock
    // mechanism. Issue #5263.
    private void addOp(ExecutableOp operation) {
        if (clipEmpty
                && !(operation instanceof ClipRect)
                && !(operation instanceof ClipShape)
                && !(operation instanceof SetTransform)) {
            return;
        }
        upcoming.add(operation);
    }

    @Override
    public void drawImage(Object img, int x, int y) {
        // An empty clip must cull every draw; a degenerate empty-clip path on
        // the host leaks image blits, so cull here. Issue #5263.
        if (clipEmpty) { return; }
        imageTransformRenderAdapter.drawImage((NativeImage)img, x, y);
    }

    @Override
    public void drawImage(Object img, int x, int y, int w, int h) {
        if (clipEmpty) { return; }
        imageTransformRenderAdapter.drawImage((NativeImage)img, x, y, w, h);
    }

    /// Buffers a blit of a raw canvas (an offscreen WebGL render target) into the
    /// display op stream. Used by the GPU compositing path so a RenderView's 3D
    /// frame is drawn onto the display surface in flushGraphics(), layering with
    /// the rest of the UI -- unlike {@link #drawImage} this takes a live canvas,
    /// not a NativeImage.
    public void drawCanvas(com.codename1.html5.js.dom.HTMLCanvasElement canvas, int x, int y, int w, int h) {
        if (canvas == null || w <= 0 || h <= 0) {
            return;
        }
        upcoming.add(new com.codename1.impl.html5.graphics.DrawCanvas(canvas, x, y, w, h, 255));
    }

    @Override
    public void tileImage(Object img, int x, int y, int w, int h) {
        if (clipEmpty) { return; }
        imageTransformRenderAdapter.tileImage((NativeImage)img, x, y, w, h);
    }
    
    

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        addOp(new DrawArc(x, y, width, height, startAngle, arcAngle, getColor(), getAlpha()));
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        primitiveRenderAdapter.fillRect(x, y, width, height);
    }

    @Override
    public void blurRegion(int x, int y, int width, int height, float radius, float cornerRadius) {
        // Route through addOp (this class's chokepoint) so the empty-clip cull
        // applies; the base class records into its own immediate context.
        addOp(new com.codename1.impl.html5.graphics.BlurRegion(x, y, width, height, radius, cornerRadius));
    }

    @Override
    public void lensRegion(int x, int y, int width, int height, float cornerRadius, float magnify,
            int tintColor, float tintStrength) {
        addOp(new com.codename1.impl.html5.graphics.LensRegion(x, y, width, height, cornerRadius,
                magnify, tintColor, tintStrength));
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
        addOp(new DrawRoundRect(x, y, width, height, arcWidth, arcHeight, getColor(), getAlpha()));
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        addOp(new FillRoundRect(x, y, width, height, arcWidth, arcHeight, getColor(), getAlpha()));
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        addOp(new DrawPolygon(xPoints, yPoints, nPoints, getColor(), getAlpha()));
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        addOp(new FillPolygon(xPoints, yPoints, nPoints, getColor(), getAlpha()));
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

    @Override
    public void translateMatrix(double tx, double ty) {
        // Master added Graphics.translateMatrix in commit 826d60f32 / the
        // InscribedTriangleGrid test; the framework dispatches to
        // HTML5Implementation.translateMatrix which delegates to
        // ((HTML5Graphics) graphics).translateMatrix(...). Without this
        // override BufferedGraphics inherits HTML5Graphics's translateMatrix,
        // which mutates the parent class's ``transform`` field -- a
        // *different* field from the one BufferedGraphics's own
        // scale/rotate/etc. overrides use. The result: translateMatrix on
        // the form's graphics silently no-ops as far as queued ops are
        // concerned, leaving the InscribedTriangleGrid cells anchored at
        // (0,0) instead of their per-cell column/row pivots. Override here
        // so the BufferedGraphics-side ``transform`` field receives the
        // composition and the next applyTransform() submits a SetTransform
        // op carrying the right matrix.
        if (transform == null) transform = Transform.makeIdentity();
        transform.translate((float)tx, (float)ty);
        setTransformChanged();
        applyTransform();
    }

    //@Override
    //public void shear(double shx, double shy) {
    //    setTransform(JSAffineTransform.Factory.getShearInstance(shx, shy), false);
    //}
    
    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        addOp(new FillArc(x, y, width, height, startAngle, arcAngle, getColor(), getAlpha()));
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
        // Sync the reliable user-space clip tracker; clipShape() bypasses this via
        // setClipShapeInternal so it doesn't clobber the clipEmpty/tracker that
        // clipRect computed from the exact rect intersection. Issue #5263.
        Rectangle b = shape.getBounds();
        int bw = Math.max(0, b.getWidth());
        int bh = Math.max(0, b.getHeight());
        clipBoundsTracker.setBounds(b.getX(), b.getY(), bw, bh);
        clipEmpty = bw <= 0 || bh <= 0;
        setClipShapeInternal(shape);
    }

    private void setClipShapeInternal(Shape shape) {
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
        if (clipEmpty) {
            // Empty intersection (computed reliably by clipRect): record a
            // zero-area rect clip so the canvas culls every draw. The degenerate
            // shape path leaks fills AND image blits on the host. Issue #5263.
            clipBoundsDirty = true;
            primitiveRenderAdapter.setClipRect(0, 0, 0, 0);
            return;
        }
        setClipShapeInternal(p);
    }


    @Override
    public void setClip(int x, int y, int width, int height) {
        if (transform != null && !transform.isIdentity()) {
            setClip(new Rectangle(x, y, width, height));
            return;
        }
        isClipShape = false;
        clip.setBounds(x, y, width, height);
        clipBoundsTracker.setBounds(x, y, width, height);
        clipEmpty = width <= 0 || height <= 0;
        clipBoundsDirty = true;
        primitiveRenderAdapter.setClipRect(x, y, width, height);
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        Rectangle rect = new Rectangle(x, y, width, height);
        // Track the running clip bounds in user space, clamped to >= 0, so an
        // empty intersection is detected reliably whether the clip is tracked as
        // a rect or a shape. Issue #5263.
        clipBoundsTracker.intersection(rect, clipBoundsTracker);
        clipEmpty = clipBoundsTracker.getWidth() <= 0 || clipBoundsTracker.getHeight() <= 0;
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
