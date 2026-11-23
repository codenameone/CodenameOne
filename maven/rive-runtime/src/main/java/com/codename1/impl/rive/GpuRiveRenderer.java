/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.rive;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.IndexBuffer;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.PrimitiveType;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.Texture;
import com.codename1.gpu.VertexBuffer;
import com.codename1.gpu.VertexFormat;
import com.codename1.ui.Graphics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/// Draws an artboard through `com.codename1.gpu`, the renderer that really
/// deforms image meshes.
///
/// Image meshes are drawn as textured triangles. Everything else (paths,
/// gradients, plain images) is rasterized on the CPU by a
/// [GraphicsRiveRenderer] into a layer image; when a mesh follows vector
/// content, the layer is read back and later drawn as a screen quad first,
/// and cleared. Consecutive vector draws therefore batch into one layer and
/// the artboard's draw order is kept.
///
/// The work is split in two halves so the artboard is only ever touched on
/// the thread that advances it (the EDT), while the GPU calls run on the
/// render thread a `com.codename1.gpu.Renderer` is called on:
///
/// - [#beginFrame(int, int, Mat2D)], drawing the artboard into this renderer
///   and [#endFrame(int)] record a frame (layer pixels, transformed mesh
///   vertices) and publish it. Call them on the EDT.
/// - [#render(GraphicsDevice)] draws the newest published frame (or the
///   previous one again) and is the only method that calls the device. Call
///   it from `Renderer.onFrame`; [#contextLost()] from `Renderer.onInit` and
///   [#dispose(GraphicsDevice)] from `Renderer.onDispose`.
///
/// Frames are handed over through an `AtomicReference` and recycled, and the
/// GPU buffers and textures of a recorded draw are kept across frames: a
/// mesh keeps its vertex and index buffers (re-uploaded through
/// `VertexBuffer.setDirty()`, recreated only when it grows), a mesh image
/// keeps its texture. A vector layer is cropped to the bounds its draws
/// touched (rounded out to a 32 pixel grid so the size stays stable while
/// content moves) and drawn as a quad of that size; only that rectangle is
/// cleared, copied and uploaded. A layer whose cropped pixels equal what its
/// slot uploaded before is not uploaded again, so redrawing a frame that did
/// not change uploads nothing. `GraphicsDevice` has no texture update, and
/// `Image` exposes no region readback outside its package, so the layer
/// image itself is read whole once per changed layer.
///
/// Mesh blending: source-over (Rive `srcOver`) and `additive` map to
/// `RenderState.BlendMode.ALPHA` and `ADDITIVE`. `com.codename1.gpu` has no
/// other blend equation, so a mesh with any other blend mode (multiply,
/// screen, ...) is drawn into the CPU layer instead and matches the Graphics
/// path. There is no scissor either: a mesh drawn under an active clip path
/// goes to the CPU layer too, which clips it as the Graphics path does.
/// Approximation left: a layer cleared mid-frame loses a shape clip that was
/// active (the rectangle of the clip is kept).
///
/// [#begin(GraphicsDevice, int, int, Mat2D)] / [#end()] do both halves on one
/// thread, for a caller that already owns the device and the artboard.
public class GpuRiveRenderer implements RiveRenderer {
    /// Rive's `BlendMode::srcOver`.
    static final int BLEND_SRC_OVER = 3;
    /// Rive's `BlendMode::additive`.
    static final int BLEND_ADDITIVE = 12;

    private static final int OP_LAYER = 0;
    private static final int OP_MESH = 1;

    private final GraphicsRiveRenderer cpu = new GraphicsRiveRenderer();
    private final Mat2D matrix = new Mat2D();
    private final ArrayList<float[]> stack = new ArrayList<float[]>();
    private boolean[] clipStack = new boolean[8];
    private boolean clipped;
    private int depth;
    private int width;
    private int height;
    private com.codename1.ui.Image layer;
    private Graphics layerGraphics;
    private boolean layerDirty;
    // dirty bounds of the open layer in layer pixels; minX > maxX when empty
    private float minX;
    private float minY;
    private float maxX;
    private float maxY;
    private int[] full;
    private int[] cropScratch;
    private boolean recording;
    private Frame filling;
    private int layersFlushed;
    private int meshesDrawn;
    private int cpuMeshes;

    private final AtomicReference<Frame> pending = new AtomicReference<Frame>();
    private final AtomicReference<Frame> spare = new AtomicReference<Frame>();
    // frames the EDT dropped while spare was taken; their GPU resources are
    // released on the render thread (linked through Frame.nextReleased)
    private final AtomicReference<Frame> released = new AtomicReference<Frame>();

    // render thread state
    private GraphicsDevice singleThreadDevice;
    private Frame current;
    private int context = 1;
    private final HashMap<Object, Texture> textures = new HashMap<Object, Texture>();
    private final Camera camera = new Camera();
    private final float[] identity = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    private final Material material = new Material(Material.Type.UNLIT);
    private final RenderState alphaState = RenderState.transparent().setDepthTest(false).setDepthWrite(false)
            .setCullMode(RenderState.CullMode.NONE);
    private final RenderState additiveState = RenderState.transparent().setDepthTest(false).setDepthWrite(false)
            .setCullMode(RenderState.CullMode.NONE).setBlendMode(RenderState.BlendMode.ADDITIVE);
    private IndexBuffer quadIndices;
    private int quadContext;
    private int texturesCreated;

    /// One recorded draw: a vector layer or a mesh.
    static final class Op {
        int kind;
        int[] argb;
        float[] data;
        int[] indices;
        int vertexCount;
        int indexCount;
        Object image;
        int blend;
        float opacity;
        int version;
        // render thread only
        Texture layerTexture;
        int layerVersion = -1;
        VertexBuffer vb;
        IndexBuffer ib;
        Mesh mesh;
        int meshVersion = -1;
        int indexVersion = -1;
        int resourceContext;
        // layer crop rectangle and content counter (EDT writes, render reads)
        int lx;
        int ly;
        int lw;
        int lh;
        int layerContent;
        // render thread only: the quad drawn for the layer and its rectangle
        VertexBuffer qvb;
        Mesh qmesh;
        int qx = -1;
        int qy;
        int qw;
        int qh;
    }

    /// One recorded frame.
    static final class Frame {
        final ArrayList<Op> ops = new ArrayList<Op>();
        int count;
        int width;
        int height;
        int clear;
        Frame nextReleased;

        Op next() {
            if (count == ops.size()) {
                ops.add(new Op());
            }
            Op o = ops.get(count++);
            o.version++;
            return o;
        }
    }

    // ------------------------------------------------------------------ EDT

    /// Starts recording a frame of `frameWidth` x `frameHeight` pixels (the
    /// coordinate space of `base`). EDT side.
    public void beginFrame(int frameWidth, int frameHeight, Mat2D base) {
        width = frameWidth;
        height = frameHeight;
        matrix.set(base);
        depth = 0;
        clipped = false;
        layersFlushed = 0;
        meshesDrawn = 0;
        cpuMeshes = 0;
        layerDirty = false;
        resetBounds();
        recording = true;
        filling = spare.getAndSet(null);
        if (filling == null) {
            filling = new Frame();
        }
        filling.count = 0;
        filling.width = frameWidth;
        filling.height = frameHeight;
        if (openLayer() && layerGraphics != null) {
            // the layer is transparent here: new, or replaced by the last
            // flushLayer() that read what was drawn into it
            layerGraphics.setClip(0, 0, width, height);
            cpu.begin(layerGraphics, base);
        }
    }

    /// Finishes the frame begun by [#beginFrame(int, int, Mat2D)] and
    /// publishes it to [#render(GraphicsDevice)]. `clearArgb` is the color the
    /// surface is cleared with first. EDT side.
    public void endFrame(int clearArgb) {
        flushLayer();
        if (layerGraphics != null) {
            cpu.end();
        }
        recording = false;
        Frame f = filling;
        filling = null;
        if (f == null) {
            return;
        }
        f.clear = clearArgb;
        Frame old = pending.getAndSet(f);
        if (old != null && !spare.compareAndSet(null, old)) {
            // never drawn and no slot to recycle it into: hand its GPU
            // resources to the render thread to release
            while (true) {
                Frame head = released.get();
                old.nextReleased = head;
                if (released.compareAndSet(head, old)) {
                    break;
                }
            }
        }
    }

    /// True when a published frame has not been drawn yet.
    public boolean hasPendingFrame() {
        return pending.get() != null;
    }

    private static final AtomicInteger TOTAL_FRAMES_DRAWN = new AtomicInteger();

    /// Frames every renderer in the process has drawn to the end; a
    /// screenshot test reads it before and after showing a form to learn
    /// that the GPU surface has started.
    public static int getTotalFramesDrawn() {
        return TOTAL_FRAMES_DRAWN.get();
    }

    /// Records and draws in one go on the calling thread, which must own
    /// both the artboard and `graphicsDevice`.
    public void begin(GraphicsDevice graphicsDevice, int frameWidth, int frameHeight, Mat2D base) {
        singleThreadDevice = graphicsDevice;
        beginFrame(frameWidth, frameHeight, base);
    }

    /// Ends [#begin(GraphicsDevice, int, int, Mat2D)]: publishes the frame and
    /// draws it without clearing the surface.
    public void end() {
        // Flush the trailing vector layer before taking the counts, or the
        // last layer endFrame records is missing from getLayerCount().
        flushLayer();
        int layers = layersFlushed;
        int meshes = meshesDrawn;
        endFrame(0);
        if (singleThreadDevice != null) {
            render(singleThreadDevice, false);
        }
        singleThreadDevice = null;
        layersFlushed = layers;
        meshesDrawn = meshes;
    }

    /// Vector layers recorded in the last frame.
    public int getLayerCount() {
        return layersFlushed;
    }

    /// Meshes drawn as GPU triangles in the last frame.
    public int getMeshCount() {
        return meshesDrawn;
    }

    /// Meshes that went to the CPU layer in the last frame (clipped, or a
    /// blend mode the GPU state cannot express).
    public int getCpuMeshCount() {
        return cpuMeshes;
    }

    protected boolean openLayer() {
        if (layer != null && layer.getWidth() == width && layer.getHeight() == height) {
            return true;
        }
        if (width <= 0 || height <= 0 || !com.codename1.ui.Display.isInitialized()) {
            layer = null;
            layerGraphics = null;
            return false;
        }
        layer = com.codename1.ui.Image.createImage(width, height, 0);
        layerGraphics = layer.getGraphics();
        return true;
    }

    /// Replaces the layer with a transparent one. `Graphics` has no portable
    /// way to erase pixels of a mutable image (a transparent fill blends to
    /// nothing), so a layer whose content was read out is replaced, and the
    /// CPU renderer, which may be mid-frame, moves onto the new graphics.
    private void freshLayer() {
        layer = com.codename1.ui.Image.createImage(width, height, 0);
        layerGraphics = layer.getGraphics();
        layerGraphics.setClip(0, 0, width, height);
        cpu.retarget(layerGraphics);
    }

    private void resetBounds() {
        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        maxX = -Float.MAX_VALUE;
        maxY = -Float.MAX_VALUE;
    }

    private void markAll() {
        minX = 0;
        minY = 0;
        maxX = width;
        maxY = height;
    }

    /// Grows the dirty bounds by the control points of `path` under the
    /// current matrix (a cubic lies in the hull of its points), padded for
    /// a stroke (miter joins included) and antialiasing.
    private void markPath(RivePath path, RivePaint paint) {
        float[] v = matrix.v;
        float[] p = path.points;
        // pointCount counts floats, two per point
        int n = path.pointCount / 2;
        if (n == 0) {
            return;
        }
        float x0 = Float.MAX_VALUE;
        float y0 = Float.MAX_VALUE;
        float x1 = -Float.MAX_VALUE;
        float y1 = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            float x = p[i * 2];
            float y = p[i * 2 + 1];
            float tx = v[0] * x + v[2] * y + v[4];
            float ty = v[1] * x + v[3] * y + v[5];
            if (Float.isNaN(tx) || Float.isNaN(ty) || Float.isInfinite(tx) || Float.isInfinite(ty)) {
                // a point that is not finite draws nothing and must not
                // poison the bounds (NaN wins every min/max)
                continue;
            }
            x0 = Math.min(x0, tx);
            y0 = Math.min(y0, ty);
            x1 = Math.max(x1, tx);
            y1 = Math.max(y1, ty);
        }
        if (x0 > x1) {
            return;
        }
        float pad = 2;
        if (paint != null && paint.stroke) {
            // The largest stretch the matrix applies in any direction is its
            // largest singular value, which the Frobenius norm bounds from
            // above, so a skewed or anisotropic scale is covered too. Half
            // the thickness times a miter limit of 4 is twice the thickness.
            float stretch = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2] + v[3] * v[3]);
            pad += paint.thickness * stretch * 2;
        }
        // Clamped to the surface before anything casts them to int: a huge
        // coordinate would otherwise overflow and empty the layer.
        minX = Math.max(0, Math.min(minX, x0 - pad));
        minY = Math.max(0, Math.min(minY, y0 - pad));
        maxX = Math.min(width, Math.max(maxX, x1 + pad));
        maxY = Math.min(height, Math.max(maxY, y1 + pad));
    }

    /// True for the Rive blend modes `RenderState` can express.
    static boolean gpuBlend(int blendMode) {
        return blendMode == BLEND_SRC_OVER || blendMode == BLEND_ADDITIVE;
    }

    @Override
    public void save() {
        if (depth == stack.size()) {
            stack.add(new float[6]);
        }
        if (depth == clipStack.length) {
            boolean[] n = new boolean[depth * 2];
            System.arraycopy(clipStack, 0, n, 0, depth);
            clipStack = n;
        }
        System.arraycopy(matrix.v, 0, stack.get(depth), 0, 6);
        clipStack[depth] = clipped;
        depth++;
        if (layerGraphics != null) {
            cpu.save();
        }
    }

    @Override
    public void restore() {
        if (depth == 0) {
            return;
        }
        depth--;
        System.arraycopy(stack.get(depth), 0, matrix.v, 0, 6);
        clipped = clipStack[depth];
        if (layerGraphics != null) {
            cpu.restore();
        }
    }

    @Override
    public void transform(Mat2D m) {
        matrix.multiply(matrix, m);
        if (layerGraphics != null) {
            cpu.transform(m);
        }
    }

    @Override
    public void clipPath(RivePath path) {
        clipped = true;
        if (layerGraphics != null) {
            cpu.clipPath(path);
        }
    }

    @Override
    public void drawPath(RivePath path, RivePaint paint) {
        if (layerGraphics != null) {
            cpu.drawPath(path, paint);
        }
        markPath(path, paint);
        layerDirty = true;
    }

    @Override
    public void drawImage(Object image, int blendMode, float opacity) {
        if (layerGraphics != null) {
            cpu.drawImage(image, blendMode, opacity);
        }
        markAll();
        layerDirty = true;
    }

    @Override
    public void drawImageMesh(Object image, float[] vertices, float[] uvs, int vertexCount,
            int[] indices, int indexCount, int blendMode, float opacity) {
        if (vertexCount == 0 || indexCount < 3) {
            return;
        }
        if (clipped || !gpuBlend(blendMode)) {
            cpuMeshes++;
            if (layerGraphics != null) {
                cpu.drawImageMesh(image, vertices, uvs, vertexCount, indices, indexCount, blendMode, opacity);
            }
            markAll();
            layerDirty = true;
            return;
        }
        flushLayer();
        meshesDrawn++;
        if (filling == null) {
            return;
        }
        Op o = filling.next();
        o.kind = OP_MESH;
        o.image = image;
        o.blend = blendMode;
        o.opacity = opacity;
        o.vertexCount = vertexCount;
        o.indexCount = indexCount;
        int floats = vertexCount * 5;
        if (o.data == null || o.data.length < floats) {
            o.data = new float[floats];
        }
        if (o.indices == null || o.indices.length < indexCount) {
            o.indices = new int[indexCount];
        }
        float[] v = matrix.v;
        float[] d = o.data;
        for (int i = 0; i < vertexCount; i++) {
            float x = vertices[i * 2];
            float y = vertices[i * 2 + 1];
            int p = i * 5;
            d[p] = v[0] * x + v[2] * y + v[4];
            d[p + 1] = v[1] * x + v[3] * y + v[5];
            d[p + 2] = 0;
            d[p + 3] = uvs[i * 2];
            d[p + 4] = uvs[i * 2 + 1];
        }
        System.arraycopy(indices, 0, o.indices, 0, indexCount);
    }

    @Override
    public boolean supportsMeshes() {
        return true;
    }

    private void flushLayer() {
        if (!layerDirty) {
            return;
        }
        layerDirty = false;
        layersFlushed++;
        // rounded out to a 32 pixel grid: the crop size, and with it the
        // arrays and the texture, stays the same while the content moves
        int x0 = Math.max(0, ((int) Math.floor(minX)) & ~31);
        int y0 = Math.max(0, ((int) Math.floor(minY)) & ~31);
        int x1 = Math.min(width, ((int) Math.ceil(maxX) + 31) & ~31);
        int y1 = Math.min(height, ((int) Math.ceil(maxY) + 31) & ~31);
        resetBounds();
        if (filling == null || layer == null || !recording || x1 <= x0 || y1 <= y0) {
            if (layer != null) {
                freshLayer();
            }
            return;
        }
        int cw = x1 - x0;
        int ch = y1 - y0;
        int n = width * height;
        if (full == null || full.length != n) {
            full = new int[n];
        }
        layer.getRGB(full);
        Op o = filling.next();
        o.kind = OP_LAYER;
        int cn = cw * ch;
        boolean same = o.argb != null && o.argb.length == cn && o.lx == x0 && o.ly == y0 && o.lw == cw;
        if (cropScratch == null || cropScratch.length != cn) {
            cropScratch = new int[cn];
        }
        int[] dst = cropScratch;
        int[] prev = o.argb;
        for (int row = 0; row < ch; row++) {
            int src = (y0 + row) * width + x0;
            int d = row * cw;
            for (int col = 0; col < cw; col++) {
                int px = full[src + col];
                dst[d + col] = px;
                if (same && prev[d + col] != px) {
                    same = false;
                }
            }
        }
        if (!same) {
            cropScratch = prev != null && prev.length == cn ? prev : null;
            o.argb = dst;
            o.lx = x0;
            o.ly = y0;
            o.lw = cw;
            o.lh = ch;
            o.layerContent++;
        }
        freshLayer();
    }

    // --------------------------------------------------------- render thread

    /// Draws the newest published frame, or the last one again when nothing
    /// new was published. Clears the surface first. Render thread side; the
    /// caller sets the viewport (`Renderer.onResize`).
    public void render(GraphicsDevice device) {
        render(device, true);
    }

    // TEMPORARY trace for the RiveGpu emulator crash; removed once found.
    private static final AtomicInteger TRACE = new AtomicInteger();

    private static void trace(String s) {
        if (TRACE.incrementAndGet() < 200) {
            System.out.println("CN1GPU: rive " + s);
        }
    }

    private void render(GraphicsDevice device, boolean clear) {
        releaseDropped(device);
        Frame f = pending.getAndSet(null);
        if (f != null) {
            if (current != null) {
                Frame lost = spare.getAndSet(current);
                if (lost != null) {
                    releaseFrame(device, lost);
                }
            }
            current = f;
        }
        Frame fr = current;
        if (fr == null) {
            if (clear) {
                device.clear(0, true, true);
            }
            return;
        }
        if (clear) {
            device.clear(fr.clear, true, true);
        }
        int w = Math.max(1, fr.width);
        int h = Math.max(1, fr.height);
        // Looking down +z with -y up: x right, y down, in pixels.
        camera.setOrthographic(h, -1f, 1f).setAspect(w / (float) h)
                .setPosition(w / 2f, h / 2f, -0.5f).setTarget(w / 2f, h / 2f, 0f).setUp(0, -1, 0);
        device.setCamera(camera);
        trace("frame " + w + "x" + h + " ops=" + fr.count);
        for (int i = 0; i < fr.count; i++) {
            Op o = fr.ops.get(i);
            trace("op " + i + " kind=" + o.kind + " layer=" + o.lw + "x" + o.lh + " verts=" + o.vertexCount
                    + " image=" + (o.image instanceof com.codename1.ui.Image
                    ? ((com.codename1.ui.Image) o.image).getWidth() + "x" + ((com.codename1.ui.Image) o.image).getHeight() : "-"));
            if (o.resourceContext != context) {
                // handles from a lost context: forget, never dispose
                o.layerTexture = null;
                o.vb = null;
                o.ib = null;
                o.mesh = null;
                o.qvb = null;
                o.qmesh = null;
                o.qx = -1;
                o.layerVersion = -1;
                o.meshVersion = -1;
                o.indexVersion = -1;
                o.resourceContext = context;
            }
            if (o.kind == OP_LAYER) {
                drawLayer(device, o);
            } else {
                drawMesh(device, o);
            }
        }
        trace("frame done");
        TOTAL_FRAMES_DRAWN.incrementAndGet();
    }

    private void drawLayer(GraphicsDevice device, Op o) {
        if (o.layerVersion != o.layerContent || o.layerTexture == null) {
            if (o.layerTexture != null) {
                device.dispose(o.layerTexture);
            }
            o.layerTexture = device.createTexture(o.lw, o.lh, o.argb);
            texturesCreated++;
            o.layerVersion = o.layerContent;
        }
        if (quadIndices == null || quadContext != context) {
            // shared indices of every layer quad
            quadIndices = new IndexBuffer(6);
            quadIndices.setData(new int[]{0, 1, 2, 0, 2, 3});
            quadContext = context;
            o.qmesh = null;
        }
        if (o.qmesh == null || o.qx != o.lx || o.qy != o.ly || o.qw != o.lw || o.qh != o.lh) {
            if (o.qvb == null) {
                o.qvb = new VertexBuffer(VertexFormat.POSITION_TEXCOORD, 4);
            }
            float x0 = o.lx;
            float y0 = o.ly;
            float x1 = o.lx + o.lw;
            float y1 = o.ly + o.lh;
            float[] d = o.qvb.getData();
            quadVertex(d, 0, x0, y0, 0, 0);
            quadVertex(d, 1, x1, y0, 1, 0);
            quadVertex(d, 2, x1, y1, 1, 1);
            quadVertex(d, 3, x0, y1, 0, 1);
            o.qvb.setDirty();
            if (o.qmesh == null) {
                o.qmesh = new Mesh(o.qvb, quadIndices, PrimitiveType.TRIANGLES);
            }
            o.qx = o.lx;
            o.qy = o.ly;
            o.qw = o.lw;
            o.qh = o.lh;
        }
        material.setRenderState(alphaState);
        material.setTexture(o.layerTexture).setColor(0xffffffff);
        device.draw(o.qmesh, material, identity);
    }

    private static void quadVertex(float[] d, int i, float x, float y, float u, float v) {
        int p = i * 5;
        d[p] = x;
        d[p + 1] = y;
        d[p + 2] = 0;
        d[p + 3] = u;
        d[p + 4] = v;
    }

    /// Releases the frames the EDT dropped (see [#endFrame(int)]).
    private void releaseDropped(GraphicsDevice device) {
        Frame f = released.getAndSet(null);
        while (f != null) {
            Frame next = f.nextReleased;
            f.nextReleased = null;
            releaseFrame(device, f);
            f = next;
        }
    }

    private void drawMesh(GraphicsDevice device, Op o) {
        if (!(o.image instanceof com.codename1.ui.Image)) {
            return;
        }
        Texture t = textures.get(o.image);
        if (t == null) {
            t = device.createTexture((com.codename1.ui.Image) o.image);
            texturesCreated++;
            textures.put(o.image, t);
        }
        if (o.vb == null || o.vb.getVertexCount() != o.vertexCount) {
            if (o.vb != null) {
                device.dispose(o.vb);
            }
            o.vb = new VertexBuffer(VertexFormat.POSITION_TEXCOORD, o.vertexCount);
            o.mesh = null;
            o.meshVersion = -1;
        }
        if (o.ib == null || o.ib.getIndexCount() != o.indexCount) {
            if (o.ib != null) {
                device.dispose(o.ib);
            }
            o.ib = new IndexBuffer(o.indexCount);
            o.mesh = null;
            o.indexVersion = -1;
        }
        if (o.meshVersion != o.version) {
            float[] dst = o.vb.getData();
            System.arraycopy(o.data, 0, dst, 0, o.vertexCount * 5);
            o.vb.setDirty();
            if (o.indexVersion == -1 || !sameIndices(o)) {
                short[] di = o.ib.getData();
                for (int i = 0; i < o.indexCount; i++) {
                    di[i] = (short) o.indices[i];
                }
                o.ib.setDirty();
                o.indexVersion = o.version;
            }
            o.meshVersion = o.version;
        }
        if (o.mesh == null) {
            o.mesh = new Mesh(o.vb, o.ib, PrimitiveType.TRIANGLES);
        }
        int a = (int) (255 * Math.max(0f, Math.min(1f, o.opacity)));
        material.setRenderState(o.blend == BLEND_ADDITIVE ? additiveState : alphaState);
        material.setTexture(t).setColor((a << 24) | 0xffffff);
        device.draw(o.mesh, material, identity);
    }

    private static boolean sameIndices(Op o) {
        short[] di = o.ib.getData();
        for (int i = 0; i < o.indexCount; i++) {
            if (di[i] != (short) o.indices[i]) {
                return false;
            }
        }
        return true;
    }

    private void releaseFrame(GraphicsDevice device, Frame f) {
        for (int i = 0; i < f.ops.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            Op o = f.ops.get(i);
            if (o.resourceContext == context) {
                if (o.layerTexture != null) {
                    device.dispose(o.layerTexture);
                }
                if (o.vb != null) {
                    device.dispose(o.vb);
                }
                if (o.ib != null) {
                    device.dispose(o.ib);
                }
                if (o.qvb != null) {
                    device.dispose(o.qvb);
                }
            }
            o.qvb = null;
            o.qmesh = null;
            o.qx = -1;
            o.layerTexture = null;
            o.vb = null;
            o.ib = null;
            o.mesh = null;
            o.layerVersion = -1;
            o.meshVersion = -1;
            o.indexVersion = -1;
        }
    }

    /// Forgets every device handle without disposing it: the GPU context was
    /// (re)created and the old handles are gone. Call from `Renderer.onInit`.
    public void contextLost() {
        context++;
        textures.clear();
        quadIndices = null;
    }

    /// Number of textures created on the device so far (for tests).
    int getTexturesCreated() {
        return texturesCreated;
    }

    /// Releases every device resource this renderer created. Render thread.
    public void dispose(GraphicsDevice graphicsDevice) {
        for (Texture t : textures.values()) {
            graphicsDevice.dispose(t);
        }
        textures.clear();
        if (current != null) {
            releaseFrame(graphicsDevice, current);
        }
        current = null;
        Frame s = spare.getAndSet(null);
        if (s != null) {
            releaseFrame(graphicsDevice, s);
        }
        Frame p = pending.getAndSet(null);
        if (p != null) {
            releaseFrame(graphicsDevice, p);
        }
        releaseDropped(graphicsDevice);
        if (quadIndices != null && quadContext == context) {
            graphicsDevice.dispose(quadIndices);
        }
        quadIndices = null;
        context++;
    }
}
