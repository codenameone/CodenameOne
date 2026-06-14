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
import com.codename1.impl.html5.graphics.SurfaceCommandRecorder;
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
    // Surface dimensions, known Java-side at construction. Paint ops never read
    // them back across the barrier.
    private int canvasWidth;
    private int canvasHeight;
    // The opaque worker-assigned id of the host-side surface this graphics
    // draws onto. The worker NEVER holds the canvas / 2D-context proxy; it
    // records draw calls into ``context`` (a command recorder) and flushes them
    // by surface id to the host, which keeps the id->{canvas,ctx} table and
    // replays them. Only getRGB ever reads pixels back. See
    // SurfaceCommandRecorder and HTML5Implementation.nativeSurface*.
    private int surfaceId;
    // The command recorder this graphics' ExecutableOps draw into. It implements
    // CanvasRenderingContext2D so the op classes record into it unchanged; each
    // call appends a self-contained opcode rather than round-tripping a host
    // canvas method. ``flush()`` ships the recorded batch to the surface.
    private final SurfaceCommandRecorder context = new SurfaceCommandRecorder();
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
                            dispatchOp(operation);
                        }
                    }, JavaScriptExecutableOpFactory.INSTANCE);
    private final JavaScriptImageTransformRenderAdapter<NativeImage, Shape, JSAffineTransform, ExecutableOp> imageTransformRenderAdapter =
            new JavaScriptImageTransformRenderAdapter<NativeImage, Shape, JSAffineTransform, ExecutableOp>(renderState,
                    new JavaScriptImageTransformRenderAdapter.OperationSink<ExecutableOp>() {
                        @Override
                        public void submit(ExecutableOp operation) {
                            dispatchOp(operation);
                        }
                    }, JavaScriptExecutableOpFactory.INSTANCE);
    private final JavaScriptShapeGradientRenderAdapter<Shape, Stroke, ExecutableOp> shapeGradientRenderAdapter =
            new JavaScriptShapeGradientRenderAdapter<Shape, Stroke, ExecutableOp>(renderState,
                    new JavaScriptShapeGradientRenderAdapter.OperationSink<ExecutableOp>() {
                        @Override
                        public void submit(ExecutableOp operation) {
                            dispatchOp(operation);
                        }
                    }, JavaScriptExecutableOpFactory.INSTANCE);
    
    //private final Path tmppath = new Path();
    //private final static PorterDuffXfermode PORTER = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
    
    // ``width``/``height`` are the surface dimensions, known Java-side, so the
    // clip bounds are seeded without ever reading them back across the barrier.
    // ``surfaceId`` is the opaque host-side surface this graphics draws onto;
    // the caller has already created the surface on the host
    // (HTML5Implementation.nativeSurfaceCreate) -- except the display surface
    // (DISPLAY_SURFACE_ID), which the host binds lazily to the output canvas.
    HTML5Graphics(HTML5Implementation impl, int surfaceId, int width, int height) {
        this.impl = impl;
        this.surfaceId = surfaceId;
        this.canvasWidth = width;
        this.canvasHeight = height;
        this.clipRect.setWidth(width);
        this.clipRect.setHeight(height);
    }

    // Single chokepoint for every drawing op: record it into the surface command
    // buffer. Nothing crosses the barrier here -- the batch is shipped by
    // flush().
    private void dispatchOp(ExecutableOp operation) {
        notifyMutation();
        operation.execute(context);
    }

    int getSurfaceId() {
        return surfaceId;
    }

    // Ship the recorded command batch to the host surface, fire-and-forget (one
    // round-trip whose response is null, never a canvas/number). After a flush
    // the recorder is empty. Idempotent: a no-op when nothing was recorded.
    void flush() {
        if (context.isEmpty()) {
            return;
        }
        impl.nativeSurfaceFlush(surfaceId, canvasWidth, canvasHeight,
                context.opcodeBuffer(), context.opcodeCount(),
                context.numBuffer(), context.numCountValue(),
                context.objBuffer(), context.objCountValue());
        context.reset();
    }

    int getCanvasWidth() {
        return canvasWidth;
    }

    int getCanvasHeight() {
        return canvasHeight;
    }


    public ClipState getClipState() {
        return renderState.getClipState();
    }

    protected final JavaScriptRenderState<NativeFont> getRenderState() {
        return renderState;
    }

    NativeFont getFont() {
        return renderState.getFont();
    }

    void setFont(NativeFont font) {
        // Drawing ops (DrawString) carry their own font, so the renderState is
        // the single source of truth; no command is recorded here.
        renderState.setFont(font);
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
    
    // Drawing ops (FillRect/DrawString/...) each carry their own color/alpha and
    // bracket themselves in save()/restore(), so the render state is the single
    // source of truth -- no command is recorded by these setters.
    void setColor(int color){
        renderState.setColor(color);
    }

    void setColorWithAlpha(int color) {
        renderState.setColor(color);
    }

    void setAlpha(int alpha) {
        renderState.setAlpha(alpha);
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

    public void translateMatrix(double tx, double ty) {
        // Compose T(x, y) onto the impl-side matrix, mirroring scale/rotate.
        // Lets Graphics.translateMatrix produce matrix-correct semantics on
        // HTML5; see Graphics.translateMatrix javadoc.
        if (transform != null) {
            transform.translate((float)tx, (float)ty);
            setTransformChanged();
            applyTransform();
        } else {
            setTransform(Transform.makeTranslation((float)tx, (float)ty));
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
        // Re-sync canvas transform to the Java-side value. The ClipRect/
        // ClipShape op's own context.restore() pops the transform back to
        // whatever was saved at the matching clip's save() -- which under
        // a rotated pushClip/popClip pair is the rotation, not identity.
        // The ops' inline setTransform(identity) reset is silently swallowed
        // on off-screen contexts via the cooperative-scheduler virtual-
        // dispatch path; a SetTransform op (proven @JSBody bridge) always
        // lands. See ClipUnderRotation, where the green sentinel at (2,2)
        // got drawn at the rotated location and clipped out without this.
        setTransformChanged();
        applyTransform();
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

   
    
    
    /**
     * Measure ``text`` with the given CSS font using a worker-side
     * OffscreenCanvas. Returns the rounded pixel width or ``-1`` when
     * OffscreenCanvas isn't available (older browsers / Safari < 16.4).
     *
     * Empirical Initializr boot before this fast path: 56 measureText
     * calls routed through the main thread, each costing 3
     * round-trips (getFont, measureText, TextMetrics.width) = ~168
     * round-trips. The OffscreenCanvas path stays entirely in the
     * worker.
     */
    @JSBody(params = {"css", "text"}, script = ""
            + "if (typeof OffscreenCanvas !== 'function') return -1;"
            + "var ctx = self.__cn1OcCtx;"
            + "if (ctx === null) return -1;"
            + "if (ctx === undefined) {"
            + "  try { ctx = new OffscreenCanvas(1, 1).getContext('2d'); }"
            + "  catch (e) { self.__cn1OcCtx = null; return -1; }"
            + "  if (!ctx) { self.__cn1OcCtx = null; return -1; }"
            + "  self.__cn1OcCtx = ctx;"
            + "}"
            + "var f = (typeof jvm !== 'undefined' && typeof jvm.toNativeString === 'function' && css && css.__class === 'java_lang_String') ? jvm.toNativeString(css) : String(css);"
            + "var s = (typeof jvm !== 'undefined' && typeof jvm.toNativeString === 'function' && text && text.__class === 'java_lang_String') ? jvm.toNativeString(text) : String(text);"
            + "if (ctx.font !== f) ctx.font = f;"
            + "return Math.round(ctx.measureText(s).width);")
    private static native int stringWidthOffscreen(String css, String text);

    public int charsWidth(Object nativeFont, char[] ch, int offset, int length) {
        return stringWidth(nativeFont, new String(ch, offset, length));
    }


    public int stringWidth(Object nativeFont, String str) {
        // Fast path: measure on a worker-side OffscreenCanvas. The
        // legacy path round-tripped 3x to the main thread per call
        // (getFont, measureText, TextMetrics.width) -- ~168
        // round-trips during Initializr boot alone. OffscreenCanvas
        // keeps the entire call in the worker.
        NativeFont font = (NativeFont) nativeFont;
        int offscreenWidth = stringWidthOffscreen(font.getCSS(), str);
        if (offscreenWidth >= 0) {
            return offscreenWidth + 1;
        }
        // OffscreenCanvas is the real measurement route in the surface render
        // model (the worker holds no canvas context to measure against) and
        // succeeds in every browser we ship. This estimate is only a defensive
        // fallback for the pathological case where OffscreenCanvas is missing.
        return str.length() * Math.max(1, font.fontHeight() / 2) + 1;
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
        // Clearing the whole surface == discarding any not-yet-flushed commands
        // and wiping the host canvas. The host surface persists pixels across
        // flushes, so a ClearRect over the full bounds is recorded to wipe it.
        context.reset();
        context.clearRect(0, 0, canvasWidth, canvasHeight);
        notifyMutation();
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
