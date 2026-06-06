/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.graphics;

import com.codename1.html5.js.canvas.CanvasGradient;
import com.codename1.html5.js.canvas.CanvasImageSource;
import com.codename1.html5.js.canvas.CanvasPattern;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;
import com.codename1.html5.js.canvas.ImageData;
import com.codename1.html5.js.canvas.TextMetrics;
import com.codename1.html5.js.dom.HTMLCanvasElement;
import com.codename1.html5.js.dom.HTMLImageElement;

/**
 * Records every canvas drawing call as a flat, self-contained COMMAND STREAM
 * targeting an opaque worker-assigned <em>surface id</em> -- instead of
 * round-tripping each call to a {@code CanvasRenderingContext2D} host-ref on the
 * main thread.
 *
 * <p>This is the keystone of the surface-id render bridge. The worker (Java)
 * side never holds a canvas or rendering-context proxy: it allocates surface
 * ids itself, records draw ops into the three parallel buffers below, and flushes
 * them <em>fire-and-forget</em> to the host, which keeps the id&rarr;{canvas,ctx}
 * lookup table and replays the stream against the real context. Only
 * {@code getRGB} (readPixels) ever crosses back. This removes the per-mutable-image
 * {@code getContext()} round-trip storm (the "Number 667" host-ref staleness) and
 * the lost-response parks it caused, because nothing the renderer issues waits on
 * a response.</p>
 *
 * <p>Because every {@link ExecutableOp} already decomposes into primitive canvas
 * calls ({@code save}/{@code setFillStyle}/{@code rect}/{@code fill}/{@code restore}),
 * this recorder simply implements {@link CanvasRenderingContext2D} and the op
 * classes record into it unchanged. The host-side replayer
 * (browser_bridge.js {@code __cn1_surface_flush__}) mirrors the opcode switch
 * below; <b>the two MUST stay in sync</b>.</p>
 *
 * <p>Buffer layout: a command is one entry in {@link #ops} (the opcode) plus a
 * fixed, opcode-known number of doubles consumed from {@link #nums} and objects
 * (strings / host-ref markers) consumed from {@link #objs}, in order.</p>
 */
public final class SurfaceCommandRecorder implements CanvasRenderingContext2D {
    // --- Opcodes. MUST mirror browser_bridge.js SURF_OP_* exactly. ---
    public static final int OP_SAVE = 1;
    public static final int OP_RESTORE = 2;
    public static final int OP_SCALE = 3;            // 2 nums
    public static final int OP_ROTATE = 4;           // 1 num
    public static final int OP_TRANSLATE = 5;        // 2 nums
    public static final int OP_TRANSFORM = 6;        // 6 nums
    public static final int OP_SET_TRANSFORM = 7;    // 6 nums
    public static final int OP_SET_GLOBAL_ALPHA = 8; // 1 num
    public static final int OP_SET_GCO = 9;          // 1 str
    public static final int OP_SET_FILL_COLOR = 10;  // 1 str
    public static final int OP_SET_STROKE_COLOR = 11;// 1 str
    public static final int OP_SET_LINE_WIDTH = 12;  // 1 num
    public static final int OP_SET_LINE_CAP = 13;    // 1 str
    public static final int OP_SET_LINE_JOIN = 14;   // 1 str
    public static final int OP_SET_MITER_LIMIT = 15; // 1 num
    public static final int OP_SET_FONT = 16;        // 1 str
    public static final int OP_SET_TEXT_ALIGN = 17;  // 1 str
    public static final int OP_SET_TEXT_BASELINE = 18;// 1 str
    public static final int OP_SET_SHADOW_COLOR = 19;// 1 str
    public static final int OP_SET_SHADOW_BLUR = 20; // 1 num
    public static final int OP_SET_SHADOW_OFFX = 21; // 1 num
    public static final int OP_SET_SHADOW_OFFY = 22; // 1 num
    public static final int OP_SET_FILTER = 23;      // 1 str
    public static final int OP_CLEAR_RECT = 24;      // 4 nums
    public static final int OP_FILL_RECT = 25;       // 4 nums
    public static final int OP_STROKE_RECT = 26;     // 4 nums
    public static final int OP_BEGIN_PATH = 27;
    public static final int OP_CLOSE_PATH = 28;
    public static final int OP_MOVE_TO = 29;         // 2 nums
    public static final int OP_LINE_TO = 30;         // 2 nums
    public static final int OP_QUAD_TO = 31;         // 4 nums
    public static final int OP_BEZIER_TO = 32;       // 6 nums
    public static final int OP_ARC = 33;             // 6 nums (last = counterclockwise 0/1)
    public static final int OP_ARC_TO = 34;          // 5 nums
    public static final int OP_ELLIPSE = 35;         // 7 nums
    public static final int OP_RECT = 36;            // 4 nums
    public static final int OP_FILL = 37;
    public static final int OP_STROKE = 38;
    public static final int OP_CLIP = 39;
    public static final int OP_FILL_TEXT = 40;       // 3 nums (x,y,maxWidth; maxWidth<0 = none) + 1 str
    public static final int OP_STROKE_TEXT = 41;     // 3 nums + 1 str
    public static final int OP_SET_LINE_DASH_OFFSET = 42; // 1 num
    public static final int OP_SET_LINE_DASH = 43;   // 1 num (count) + count nums
    // Gradients: created host-side into a "current gradient" register.
    public static final int OP_CREATE_LINEAR_GRADIENT = 50; // 4 nums
    public static final int OP_CREATE_RADIAL_GRADIENT = 51; // 6 nums
    public static final int OP_ADD_COLOR_STOP = 52;  // 1 num (offset) + 1 str (color)
    public static final int OP_SET_FILL_GRADIENT = 53;
    public static final int OP_SET_STROKE_GRADIENT = 54;
    // Patterns: created host-side from an image host-ref into a register.
    public static final int OP_CREATE_PATTERN = 55;  // 1 obj (image ref) + 1 str (repetition)
    public static final int OP_SET_FILL_PATTERN = 56;
    public static final int OP_CREATE_PATTERN_SURFACE = 57; // 1 num (srcSurfaceId) + 1 str (repetition)
    // Images. The image is referenced by HOST-REF MARKER (loaded images stay
    // host-side resources; never transported back to the worker) carried as an
    // obj arg. drawImage variants by destination/source arity:
    public static final int OP_DRAW_IMAGE_XY = 60;     // 1 obj + 2 nums
    public static final int OP_DRAW_IMAGE_XYWH = 61;   // 1 obj + 4 nums
    public static final int OP_DRAW_IMAGE_SRCDST = 62; // 1 obj + 8 nums
    // Blit another SURFACE (mutable image) onto this surface, referenced by its
    // worker-assigned surface id. This is how drawing a mutable image onto the
    // display (or another mutable image) crosses -- no host-ref, just ids.
    public static final int OP_BLIT_SURFACE_XY = 70;   // 1 num (srcSurfaceId) + 2 nums
    public static final int OP_BLIT_SURFACE_XYWH = 71; // 1 num (srcSurfaceId) + 4 nums
    public static final int OP_BLIT_SURFACE_SRCDST = 72; // 1 num (srcSurfaceId) + 8 nums

    private int[] ops = new int[64];
    private int opCount;
    private double[] nums = new double[256];
    private int numCount;
    private Object[] objs = new Object[32];
    private int objCount;

    private final RecGradient gradientToken = new RecGradient();
    private final RecPattern patternToken = new RecPattern();

    public SurfaceCommandRecorder() {
    }

    // --- buffer plumbing ---
    private void op(int code) {
        if (opCount == ops.length) {
            int[] n = new int[ops.length * 2];
            System.arraycopy(ops, 0, n, 0, ops.length);
            ops = n;
        }
        ops[opCount++] = code;
    }

    private void num(double v) {
        if (numCount == nums.length) {
            double[] n = new double[nums.length * 2];
            System.arraycopy(nums, 0, n, 0, nums.length);
            nums = n;
        }
        nums[numCount++] = v;
    }

    private void obj(Object v) {
        if (objCount == objs.length) {
            Object[] n = new Object[objs.length * 2];
            System.arraycopy(objs, 0, n, 0, objs.length);
            objs = n;
        }
        objs[objCount++] = v;
    }

    /** True if anything was recorded since the last {@link #reset()}. */
    public boolean isEmpty() {
        return opCount == 0;
    }

    /** Drop all recorded commands (after a flush, or when a surface is cleared). */
    public void reset() {
        opCount = 0;
        numCount = 0;
        // Null out object slots so flushed host-ref markers / strings don't pin
        // memory for the life of the recorder.
        for (int i = 0; i < objCount; i++) {
            objs[i] = null;
        }
        objCount = 0;
    }

    // --- flush accessors (read by HTML5Implementation native flush) ---
    public int[] opcodeBuffer() { return ops; }
    public int opcodeCount() { return opCount; }
    public double[] numBuffer() { return nums; }
    public int numCountValue() { return numCount; }
    public Object[] objBuffer() { return objs; }
    public int objCountValue() { return objCount; }

    /**
     * Record a blit of another surface (a mutable image) onto this surface.
     * {@code w}/{@code h} of {@code -1} means natural-size draw.
     */
    public void blitSurface(int srcSurfaceId, int x, int y, int w, int h) {
        if (w == -1 || h == -1) {
            op(OP_BLIT_SURFACE_XY);
            num(srcSurfaceId); num(x); num(y);
        } else {
            // Cull a zero/negative-area blit -- it copies nothing to the dest.
            if (w <= 0 || h <= 0) { return; }
            op(OP_BLIT_SURFACE_XYWH);
            num(srcSurfaceId); num(x); num(y); num(w); num(h);
        }
    }

    public void blitSurface(int srcSurfaceId, int sx, int sy, int sw, int sh,
            int dx, int dy, int dw, int dh) {
        // Cull when the source or dest area collapses -- nothing is copied.
        if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0) { return; }
        op(OP_BLIT_SURFACE_SRCDST);
        num(srcSurfaceId);
        num(sx); num(sy); num(sw); num(sh);
        num(dx); num(dy); num(dw); num(dh);
    }

    /** Create a fill pattern from another surface (a mutable image). */
    public CanvasPattern createPatternFromSurface(int srcSurfaceId, String repetition) {
        op(OP_CREATE_PATTERN_SURFACE); num(srcSurfaceId); obj(repetition);
        return patternToken;
    }

    // ============ CanvasRenderingContext2D ============

    @Override public HTMLCanvasElement getCanvas() { return null; }

    @Override public void save() { op(OP_SAVE); }
    @Override public void restore() { op(OP_RESTORE); }
    @Override public void scale(double x, double y) { op(OP_SCALE); num(x); num(y); }
    @Override public void rotate(double angle) { op(OP_ROTATE); num(angle); }
    @Override public void translate(double x, double y) { op(OP_TRANSLATE); num(x); num(y); }
    @Override public void transform(double a, double b, double c, double d, double e, double f) {
        op(OP_TRANSFORM); num(a); num(b); num(c); num(d); num(e); num(f);
    }
    @Override public void setTransform(double a, double b, double c, double d, double e, double f) {
        op(OP_SET_TRANSFORM); num(a); num(b); num(c); num(d); num(e); num(f);
    }
    @Override public void setGlobalAlpha(double alpha) { op(OP_SET_GLOBAL_ALPHA); num(alpha); }
    @Override public double getGlobalAlpha() { return 1.0; }
    @Override public void setGlobalCompositeOperation(String operation) { op(OP_SET_GCO); obj(operation); }
    @Override public String getGlobalCompositeOperation() { return "source-over"; }
    @Override public void setFillStyle(String style) { op(OP_SET_FILL_COLOR); obj(style); }
    @Override public void setFillStyle(CanvasPattern pattern) { op(OP_SET_FILL_PATTERN); }
    @Override public void setFillStyle(CanvasGradient gradient) { op(OP_SET_FILL_GRADIENT); }
    @Override public String getFillStyle() { return "#000000"; }
    @Override public void setStrokeStyle(String style) { op(OP_SET_STROKE_COLOR); obj(style); }
    @Override public void setStrokeStyle(CanvasPattern pattern) { op(OP_SET_FILL_PATTERN); }
    @Override public String getStrokeStyle() { return "#000000"; }
    @Override public void setLineWidth(double width) { op(OP_SET_LINE_WIDTH); num(width); }
    @Override public double getLineWidth() { return 1.0; }
    @Override public void setLineCap(String cap) { op(OP_SET_LINE_CAP); obj(cap); }
    @Override public String getLineCap() { return "butt"; }
    @Override public void setLineJoin(String join) { op(OP_SET_LINE_JOIN); obj(join); }
    @Override public String getLineJoin() { return "miter"; }
    @Override public void setMiterLimit(double limit) { op(OP_SET_MITER_LIMIT); num(limit); }
    @Override public double getMiterLimit() { return 10.0; }
    @Override public void setFont(String font) { op(OP_SET_FONT); obj(font); }
    @Override public String getFont() { return "10px sans-serif"; }
    @Override public void setTextAlign(String align) { op(OP_SET_TEXT_ALIGN); obj(align); }
    @Override public String getTextAlign() { return "start"; }
    @Override public void setTextBaseline(String baseline) { op(OP_SET_TEXT_BASELINE); obj(baseline); }
    @Override public String getTextBaseline() { return "alphabetic"; }
    @Override public void setShadowColor(String color) { op(OP_SET_SHADOW_COLOR); obj(color); }
    @Override public String getShadowColor() { return "rgba(0,0,0,0)"; }
    @Override public void setShadowBlur(double blur) { op(OP_SET_SHADOW_BLUR); num(blur); }
    @Override public double getShadowBlur() { return 0.0; }
    @Override public void setShadowOffsetX(double offset) { op(OP_SET_SHADOW_OFFX); num(offset); }
    @Override public double getShadowOffsetX() { return 0.0; }
    @Override public void setShadowOffsetY(double offset) { op(OP_SET_SHADOW_OFFY); num(offset); }
    @Override public double getShadowOffsetY() { return 0.0; }
    @Override public void setFilter(String filter) { op(OP_SET_FILTER); obj(filter); }
    @Override public String getFilter() { return "none"; }
    @Override public void clearRect(double x, double y, double width, double height) {
        // Cull provable no-ops: a zero/negative-area rect paints nothing, so do
        // not record (and later ship across the bridge) an op that cannot change
        // a pixel. NaN is left alone (NaN <= 0 is false) so any genuinely
        // unexpected value still reaches the host unchanged.
        if (width <= 0 || height <= 0) { return; }
        op(OP_CLEAR_RECT); num(x); num(y); num(width); num(height);
    }
    @Override public void fillRect(double x, double y, double width, double height) {
        if (width <= 0 || height <= 0) { return; }
        op(OP_FILL_RECT); num(x); num(y); num(width); num(height);
    }
    @Override public void strokeRect(double x, double y, double width, double height) {
        // A zero-area stroke rect still draws its outline at width=0 OR height=0
        // (a line), so only cull when BOTH collapse to a point.
        if (width <= 0 && height <= 0) { return; }
        op(OP_STROKE_RECT); num(x); num(y); num(width); num(height);
    }
    @Override public void beginPath() { op(OP_BEGIN_PATH); }
    @Override public void closePath() { op(OP_CLOSE_PATH); }
    @Override public void moveTo(double x, double y) { op(OP_MOVE_TO); num(x); num(y); }
    @Override public void lineTo(double x, double y) { op(OP_LINE_TO); num(x); num(y); }
    @Override public void quadraticCurveTo(double cpx, double cpy, double x, double y) {
        op(OP_QUAD_TO); num(cpx); num(cpy); num(x); num(y);
    }
    @Override public void bezierCurveTo(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y) {
        op(OP_BEZIER_TO); num(cp1x); num(cp1y); num(cp2x); num(cp2y); num(x); num(y);
    }
    @Override public void arc(double x, double y, double radius, double startAngle, double endAngle) {
        op(OP_ARC); num(x); num(y); num(radius); num(startAngle); num(endAngle); num(0);
    }
    @Override public void arc(double x, double y, double radius, double startAngle, double endAngle, boolean counterclockwise) {
        op(OP_ARC); num(x); num(y); num(radius); num(startAngle); num(endAngle); num(counterclockwise ? 1 : 0);
    }
    @Override public void arcTo(double x1, double y1, double x2, double y2, double radius) {
        op(OP_ARC_TO); num(x1); num(y1); num(x2); num(y2); num(radius);
    }
    @Override public void ellipse(double x, double y, double radiusX, double radiusY, double rotation, double startAngle, double endAngle) {
        op(OP_ELLIPSE); num(x); num(y); num(radiusX); num(radiusY); num(rotation); num(startAngle); num(endAngle);
    }
    @Override public void rect(double x, double y, double width, double height) {
        op(OP_RECT); num(x); num(y); num(width); num(height);
    }
    @Override public void fill() { op(OP_FILL); }
    @Override public void stroke() { op(OP_STROKE); }
    @Override public void clip() { op(OP_CLIP); }
    @Override public boolean isPointInPath(double x, double y) { return false; }
    @Override public void fillText(String text, double x, double y) {
        op(OP_FILL_TEXT); num(x); num(y); num(-1); obj(text);
    }
    @Override public void fillText(String text, double x, double y, double maxWidth) {
        op(OP_FILL_TEXT); num(x); num(y); num(maxWidth); obj(text);
    }
    @Override public void strokeText(String text, double x, double y) {
        op(OP_STROKE_TEXT); num(x); num(y); num(-1); obj(text);
    }
    @Override public void strokeText(String text, double x, double y, double maxWidth) {
        op(OP_STROKE_TEXT); num(x); num(y); num(maxWidth); obj(text);
    }
    // Measurement / pixel-readback never happen on the replay path. stringWidth
    // uses the worker-side OffscreenCanvas fast path; getRGB uses readPixels.
    @Override public TextMetrics measureText(String text) {
        throw new UnsupportedOperationException("measureText not available on a recording surface");
    }
    @Override public ImageData createImageData(double width, double height) {
        throw new UnsupportedOperationException("createImageData not available on a recording surface");
    }
    @Override public ImageData getImageData(double x, double y, double width, double height) {
        throw new UnsupportedOperationException("getImageData not available on a recording surface");
    }
    @Override public void putImageData(ImageData imageData, double x, double y) {
        throw new UnsupportedOperationException("putImageData not available on a recording surface");
    }
    @Override public void putImageData(ImageData imageData, double dx, double dy, double dirtyX, double dirtyY, double dirtyWidth, double dirtyHeight) {
        throw new UnsupportedOperationException("putImageData not available on a recording surface");
    }
    @Override public void drawImage(HTMLImageElement image, double dx, double dy) {
        op(OP_DRAW_IMAGE_XY); obj(image); num(dx); num(dy);
    }
    @Override public void drawImage(HTMLImageElement image, double dx, double dy, double dWidth, double dHeight) {
        if (dWidth <= 0 || dHeight <= 0) { return; }
        op(OP_DRAW_IMAGE_XYWH); obj(image); num(dx); num(dy); num(dWidth); num(dHeight);
    }
    @Override public void drawImage(HTMLImageElement image, double sx, double sy, double sWidth, double sHeight, double dx, double dy, double dWidth, double dHeight) {
        if (sWidth <= 0 || sHeight <= 0 || dWidth <= 0 || dHeight <= 0) { return; }
        op(OP_DRAW_IMAGE_SRCDST); obj(image); num(sx); num(sy); num(sWidth); num(sHeight); num(dx); num(dy); num(dWidth); num(dHeight);
    }
    @Override public void drawImage(HTMLCanvasElement canvas, double dx, double dy) {
        op(OP_DRAW_IMAGE_XY); obj(canvas); num(dx); num(dy);
    }
    @Override public void drawImage(HTMLCanvasElement canvas, double dx, double dy, double dWidth, double dHeight) {
        if (dWidth <= 0 || dHeight <= 0) { return; }
        op(OP_DRAW_IMAGE_XYWH); obj(canvas); num(dx); num(dy); num(dWidth); num(dHeight);
    }
    @Override public void drawImage(HTMLCanvasElement canvas, double sx, double sy, double sWidth, double sHeight, double dx, double dy, double dWidth, double dHeight) {
        if (sWidth <= 0 || sHeight <= 0 || dWidth <= 0 || dHeight <= 0) { return; }
        op(OP_DRAW_IMAGE_SRCDST); obj(canvas); num(sx); num(sy); num(sWidth); num(sHeight); num(dx); num(dy); num(dWidth); num(dHeight);
    }
    @Override public void drawImage(CanvasImageSource image, double dx, double dy, double dWidth, double dHeight) {
        if (dWidth <= 0 || dHeight <= 0) { return; }
        op(OP_DRAW_IMAGE_XYWH); obj(image); num(dx); num(dy); num(dWidth); num(dHeight);
    }
    @Override public void setImageData(ImageData imageData) {
        throw new UnsupportedOperationException("setImageData not available on a recording surface");
    }
    @Override public CanvasPattern createPattern(Object image, String repetition) {
        op(OP_CREATE_PATTERN); obj(image); obj(repetition);
        return patternToken;
    }
    @Override public CanvasGradient createLinearGradient(double x0, double y0, double x1, double y1) {
        op(OP_CREATE_LINEAR_GRADIENT); num(x0); num(y0); num(x1); num(y1);
        return gradientToken;
    }
    @Override public CanvasGradient createRadialGradient(double x0, double y0, double r0, double x1, double y1, double r1) {
        op(OP_CREATE_RADIAL_GRADIENT); num(x0); num(y0); num(r0); num(x1); num(y1); num(r1);
        return gradientToken;
    }
    @Override public void setLineDash(double[] segments) {
        op(OP_SET_LINE_DASH);
        int n = segments == null ? 0 : segments.length;
        num(n);
        for (int i = 0; i < n; i++) {
            num(segments[i]);
        }
    }
    @Override public double[] getLineDash() { return new double[0]; }
    @Override public void setLineDashOffset(double offset) { op(OP_SET_LINE_DASH_OFFSET); num(offset); }
    @Override public double getLineDashOffset() { return 0.0; }

    // The gradient/pattern tokens carry no state -- they're positional markers in
    // the command stream. The host's "current gradient/pattern register" model
    // works because each op builds, fills, and uses its gradient in order.
    private final class RecGradient implements CanvasGradient {
        @Override public void addColorStop(double offset, String color) {
            op(OP_ADD_COLOR_STOP); num(offset); obj(color);
        }
    }

    private final class RecPattern implements CanvasPattern {
        @Override public void setTransform(double a, double b, double c, double d, double e, double f) {
            // Pattern transforms are unused by the CN1 renderer; ignore.
        }
    }
}
