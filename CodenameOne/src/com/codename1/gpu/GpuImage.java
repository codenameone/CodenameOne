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
package com.codename1.gpu;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;

import java.util.concurrent.atomic.AtomicInteger;

/// An image that draws itself through `com.codename1.gpu`. Wherever it is
/// drawn on the screen -- an icon, a background, `Graphics.drawImage` in a
/// `paint` method -- a GPU surface is placed over exactly the rectangle it was
/// drawn into, and the subclass renders there with a [Renderer]. Nothing is
/// rendered offscreen and read back.
///
/// A subclass supplies three things:
///
/// - [#createRenderer()]: the render-thread callbacks of the surface
/// - [#prepareFrame(int, int, int, int, int, int)]: called on the EDT on every
///   draw, to record what the next frame shows
/// - [#drawFallback(Graphics, int, int, int, int)]: the same picture through
///   `Graphics`, used where no surface can be: a platform without a GPU
///   backend (`Display.isGpuSupported()`), a subclass that declines the GPU
///   ([#usesGpu()]), a draw into a mutable image (a transition, `toImage`),
///   and the frames before the surface first exists
///
/// The surface lives in the form's layered pane, so it moves with the image
/// when it is scrolled, shrinks to the part a scrolling parent leaves
/// visible, and is taken away after a paint pass that covered its rectangle
/// without drawing the image. It ignores pointer events, which go on to the
/// component under it.
///
/// One instance has one surface and one state: shown in two places at once
/// (two labels, or twice in one `paint`), both show the same animation, and
/// the first draw of a pass takes the surface while the others draw through
/// `Graphics`. Use `scaled` or a second instance for an independent
/// placement.
///
/// The surface is placed only while the image is painted as part of
/// `Display.getCurrent()`'s own tree: a dialog painting the form behind it
/// draws that form's images through `Graphics`. When the form is
/// deinitialized (replaced, or covered by a dialog) the surface is removed
/// and released, and placed again when the form paints the image next.
///
/// The surface is a native peer, so lightweight components painted over the
/// image (a dialog, a popup) appear under it, as they do under any peer.
public abstract class GpuImage extends Image {
    private final AtomicInteger framesRendered = new AtomicInteger();
    private final int[] visible = new int[4];
    private final int[] drawn = new int[4];
    private Placement placement;
    private boolean attachQueued;
    private boolean takenThisPass;
    private int lastPass;

    /// Creates the image. Its size is what `getWidth()` and `getHeight()`
    /// answer, which a subclass overrides.
    protected GpuImage() {
        super(null);
    }

    /// Whether this image draws through the GPU at all. The default answers
    /// true; the GPU is used only where `Display.isGpuSupported()` too.
    ///
    /// #### Returns
    ///
    /// false to draw through `Graphics` everywhere
    protected boolean usesGpu() {
        return true;
    }

    /// Creates the renderer of the surface, once per surface. Its callbacks
    /// run on the render thread; share state with
    /// [#prepareFrame(int, int, int, int, int, int)] the way a `RenderView`
    /// renderer shares it with the EDT.
    ///
    /// #### Returns
    ///
    /// the renderer
    protected abstract Renderer createRenderer();

    /// Records the next frame of the surface. EDT side, called on every draw
    /// that the surface shows. The surface is `surfaceWidth` by
    /// `surfaceHeight` pixels; the full rectangle the image was drawn into is
    /// `width` by `height` pixels at `offsetX`, `offsetY` in the surface's
    /// pixels. The offsets are zero or negative: negative when a scrolling
    /// parent hides part of the image and the surface covers only the rest.
    ///
    /// #### Parameters
    ///
    /// - `surfaceWidth`: the width of the surface
    ///
    /// - `surfaceHeight`: the height of the surface
    ///
    /// - `offsetX`: where the image's left edge lies in the surface
    ///
    /// - `offsetY`: where the image's top edge lies in the surface
    ///
    /// - `width`: the width the image was drawn at
    ///
    /// - `height`: the height the image was drawn at
    protected abstract void prepareFrame(int surfaceWidth, int surfaceHeight,
                                         int offsetX, int offsetY, int width, int height);

    /// Draws the image through `Graphics` into `width` by `height` pixels at
    /// `x`, `y`, wherever no surface shows it.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics to draw with
    ///
    /// - `x`: the left edge
    ///
    /// - `y`: the top edge
    ///
    /// - `width`: the width to occupy
    ///
    /// - `height`: the height to occupy
    protected abstract void drawFallback(Graphics g, int x, int y, int width, int height);

    @Override
    public boolean requiresDrawImage() {
        return true;
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    /// True while a live GPU surface shows this image.
    ///
    /// #### Returns
    ///
    /// true if the image is on a surface
    public boolean isOnGpuSurface() {
        return placement != null && placement.getParent() != null && placement.view.getPeer() != null;
    }

    /// The frames the surfaces of this image have rendered, counted on the
    /// render thread. A test reads it to learn that the surface is live.
    ///
    /// #### Returns
    ///
    /// the number of frames rendered so far
    public int getFramesRendered() {
        return framesRendered.get();
    }

    @Override
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
        drawImage(g, nativeGraphics, x, y, getWidth(), getHeight());
    }

    @Override
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        Display d = Display.getInstance();
        Form form = d.getCurrent();
        if (form == null || !usesGpu() || !d.isGpuSupported() || !g.isScreenGraphics()
                || g.getPaintingForm() != form) { //NOPMD CompareObjectsWithEquals - same form instance
            // not part of the current form's own tree (a dialog painting the
            // form behind it): attaching would put the surface over the dialog

            drawFallback(g, x, y, w, h);
            return;
        }
        if (placement != null && placement.failed) {
            drawFallback(g, x, y, w, h);
            return;
        }
        int pass = g.getPaintPass();
        if (pass != lastPass) {
            // a new pass: whatever the last one took is free again, even when
            // the layered pane (whose paint also resets these) was not painted
            lastPass = pass;
            takenThisPass = false;
            if (placement != null) {
                placement.drawnThisPass = false;
            }
        }
        int tx = g.getTranslateX();
        int ty = g.getTranslateY();
        int ax = tx + x;
        int ay = ty + y;
        boolean same = placement != null && placement.getComponentForm() == form //NOPMD CompareObjectsWithEquals - same form instance
                && drawn[0] == ax && drawn[1] == ay && drawn[2] == w && drawn[3] == h;
        if (!visibleRect(ax, ay, w, h, g.getClipX() + tx, g.getClipY() + ty,
                g.getClipWidth(), g.getClipHeight(), same ? visible : null, visible)) {
            // nothing of it shows in this pass; the surface stays as it is
            return;
        }
        if (takenThisPass && placement != null && placement.drawnThisPass) {
            // a second placement of the same instance in one pass
            drawFallback(g, x, y, w, h);
            return;
        }
        drawn[0] = ax;
        drawn[1] = ay;
        drawn[2] = w;
        drawn[3] = h;
        if (placement == null || placement.getComponentForm() != form //NOPMD CompareObjectsWithEquals - same form instance
                || placement.view.getPeer() == null) {
            queueAttach(form);
            drawFallback(g, x, y, w, h);
            return;
        }
        takenThisPass = true;
        placement.drawnThisPass = true;
        Placement p = placement;
        if (p.rx != visible[0] || p.ry != visible[1] || p.rw != visible[2] || p.rh != visible[3]) {
            p.rx = visible[0];
            p.ry = visible[1];
            p.rw = visible[2];
            p.rh = visible[3];
            queueRelayout(p);
        }
        prepareFrame(p.rw, p.rh, ax - p.rx, ay - p.ry, w, h);
        p.view.requestRender();
    }

    /// The part of the rectangle an image is drawn into that the surface
    /// covers: the image's rectangle `ax`, `ay`, `w`, `h` (screen
    /// coordinates) intersected with the clip. When `previous` is non-null
    /// the image was drawn at the same place before and the result is the
    /// union of `previous` and this intersection, still within the image: a
    /// repaint of part of a still image clips to that part and must not
    /// shrink the surface.
    ///
    /// #### Returns
    ///
    /// false when nothing of the image is visible; `out` (x, y, width,
    /// height) is then unspecified
    static boolean visibleRect(int ax, int ay, int w, int h, int cx, int cy, int cw, int ch,
                               int[] previous, int[] out) {
        int x1 = Math.max(ax, cx);
        int y1 = Math.max(ay, cy);
        int x2 = Math.min(ax + w, cx + cw);
        int y2 = Math.min(ay + h, cy + ch);
        boolean empty = x2 <= x1 || y2 <= y1;
        if (previous != null && previous[2] > 0 && previous[3] > 0) {
            int px1 = Math.max(ax, previous[0]);
            int py1 = Math.max(ay, previous[1]);
            int px2 = Math.min(ax + w, previous[0] + previous[2]);
            int py2 = Math.min(ay + h, previous[1] + previous[3]);
            if (px2 > px1 && py2 > py1) {
                if (empty) {
                    x1 = px1;
                    y1 = py1;
                    x2 = px2;
                    y2 = py2;
                    empty = false;
                } else {
                    x1 = Math.min(x1, px1);
                    y1 = Math.min(y1, py1);
                    x2 = Math.max(x2, px2);
                    y2 = Math.max(y2, py2);
                }
            }
        }
        if (empty) {
            return false;
        }
        out[0] = x1;
        out[1] = y1;
        out[2] = x2 - x1;
        out[3] = y2 - y1;
        return true;
    }

    private void queueAttach(Form form) {
        if (attachQueued) {
            return;
        }
        attachQueued = true;
        final Form target = form;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                attachQueued = false;
                attach(target);
            }
        });
    }

    /// Puts the surface on `form`'s layered pane, outside any paint pass:
    /// adding a component while the form paints would change the hierarchy
    /// it is walking.
    private void attach(Form form) {
        if (Display.getInstance().getCurrent() != form) { //NOPMD CompareObjectsWithEquals
            return;
        }
        if (placement == null) {
            placement = new Placement(this, new SurfaceView(new CountingRenderer(createRenderer(), framesRendered)));
        }
        Placement p = placement;
        p.rx = visible[0];
        p.ry = visible[1];
        p.rw = visible[2];
        p.rh = visible[3];
        Container pane = form.getFormLayeredPane(GpuImage.class, true);
        if (p.getParent() != pane) { //NOPMD CompareObjectsWithEquals
            if (p.getParent() != null) {
                p.getParent().removeComponent(p);
            }
            pane.addComponent(p);
        }
        fillParent(p);
        p.failed = p.view.peerFailed();
        passPointerEvents(p);
        // repaint so the Graphics frames drawn meanwhile give way to the surface
        form.revalidate();
        form.repaint();
    }

    private static void queueRelayout(final Placement p) {
        if (p.relayoutQueued) {
            return;
        }
        p.relayoutQueued = true;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                p.relayoutQueued = false;
                Form f = p.getComponentForm();
                if (f == null) {
                    return;
                }
                // move only the surface: a revalidate and full repaint of the
                // form on every frame a scrolled image moves is far too much
                SurfaceView v = p.view;
                fillParent(p);
                int ox = v.getAbsoluteX();
                int oy = v.getAbsoluteY();
                int ow = v.getWidth();
                int oh = v.getHeight();
                p.layoutContainer();
                v.layoutContainer();
                int nx = v.getAbsoluteX();
                int ny = v.getAbsoluteY();
                int x1 = Math.min(ox, nx);
                int y1 = Math.min(oy, ny);
                int x2 = Math.max(ox + ow, nx + v.getWidth());
                int y2 = Math.max(oy + oh, ny + v.getHeight());
                f.repaint(x1, y1, x2 - x1, y2 - y1);
            }
        });
    }

    /// Sizes the placement to the layered pane it is on. The pane is never
    /// laid out (`setShouldLayout(false)`), so a placement left to it stays
    /// 0x0, the surface it positions falls outside its parent's clip and is
    /// never painted -- and a native peer that is never painted is never put
    /// on screen: no GL surface on Android, no frames anywhere.
    static void fillParent(Container p) {
        Container parent = p.getParent();
        if (parent == null) {
            return;
        }
        p.setX(0);
        p.setY(0);
        p.setWidth(parent.getWidth());
        p.setHeight(parent.getHeight());
    }

    private void detach(Placement p) {
        if (p != placement || p.detachQueued) { //NOPMD CompareObjectsWithEquals
            return;
        }
        p.detachQueued = true;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                Placement q = placement;
                if (q == null) {
                    return;
                }
                q.detachQueued = false;
                if (q.drawnThisPass) {
                    // drawn again since the pass that missed it
                    return;
                }
                Container parent = q.getParent();
                if (parent != null) {
                    parent.removeComponent(q);
                    parent.repaint();
                }
            }
        });
    }

    /// A form hands a pointer event to the deepest component under it, which
    /// would be the surface; marked this way it goes on to the component the
    /// image is drawn on, as `com.codename1.gaming.GameView` does with its
    /// surface.
    private static void passPointerEvents(Component c) {
        c.setIgnorePointerEvents(true);
        if (c instanceof Container) {
            Container ct = (Container) c;
            for (int i = 0; i < ct.getComponentCount(); i++) {
                passPointerEvents(ct.getComponentAt(i));
            }
        }
    }

    /// Counts the frames the subclass's renderer draws.
    private static final class CountingRenderer implements Renderer {
        private final Renderer r;
        private final AtomicInteger frames;

        CountingRenderer(Renderer r, AtomicInteger frames) {
            this.r = r;
            this.frames = frames;
        }

        @Override
        public void onInit(GraphicsDevice device) {
            r.onInit(device);
        }

        @Override
        public void onResize(GraphicsDevice device, int width, int height) {
            r.onResize(device, width, height);
        }

        @Override
        public void onFrame(GraphicsDevice device) {
            r.onFrame(device);
            frames.incrementAndGet();
        }

        @Override
        public void onDispose(GraphicsDevice device) {
            r.onDispose(device);
        }
    }

    /// Fills the layered pane and holds the surface at the rectangle the
    /// image was last drawn into. It checks, after the content under it
    /// painted, whether the image was drawn in that pass.
    private static final class Placement extends Container {
        private final GpuImage owner;
        final SurfaceView view;
        int rx;
        int ry;
        int rw;
        int rh;
        boolean drawnThisPass;
        boolean failed;
        boolean relayoutQueued;
        boolean detachQueued;

        Placement(GpuImage owner, SurfaceView view) {
            super(new PlacementLayout());
            this.owner = owner;
            this.view = view;
            addComponent(view);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (view.peerFailed()) {
                failed = true;
            }
            int tx = g.getTranslateX();
            int ty = g.getTranslateY();
            int cx = g.getClipX() + tx;
            int cy = g.getClipY() + ty;
            boolean covered = cx < rx + rw && rx < cx + g.getClipWidth()
                    && cy < ry + rh && ry < cy + g.getClipHeight();
            if (!drawnThisPass && covered) {
                owner.detach(this);
            }
            drawnThisPass = false;
            owner.takenThisPass = false;
        }

        @Override
        protected void paintBackground(Graphics g) {
        }

        @Override
        public void paintBackgrounds(Graphics g) {
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            passPointerEvents(this);
        }

        /// The form went away (replaced, or covered by a dialog): leave its
        /// layered pane and let the image drop this placement and its
        /// surface, so neither keeps the old form alive. Deferred, because
        /// the pane is walking its children while it deinitializes them.
        @Override
        protected void deinitialize() {
            super.deinitialize();
            final Placement p = this;
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    if (p.isInitialized()) {
                        // shown again before this ran
                        return;
                    }
                    Container parent = p.getParent();
                    if (parent == null) {
                        // taken away by detach(): no form holds it, keep it
                        // for the next attach
                        return;
                    }
                    parent.removeComponent(p);
                    if (owner.placement == p) { //NOPMD CompareObjectsWithEquals
                        owner.placement = null;
                    }
                }
            });
        }
    }

    /// Places the surface at the stored screen rectangle, relative to the
    /// pane, whatever the layered pane's own layout does to the pane.
    private static final class PlacementLayout extends Layout {
        @Override
        public void layoutContainer(Container parent) {
            Placement p = (Placement) parent;
            SurfaceView v = p.view;
            boolean resized = v.getWidth() != p.rw || v.getHeight() != p.rh;
            v.setX(p.rx - p.getAbsoluteX());
            v.setY(p.ry - p.getAbsoluteY());
            v.setWidth(p.rw);
            v.setHeight(p.rh);
            if (resized) {
                v.relayout();
            }
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            return new Dimension(0, 0);
        }
    }

    /// The surface, which lays its peer out again when it is resized.
    private static final class SurfaceView extends RenderView {
        SurfaceView(Renderer renderer) {
            super(renderer);
        }

        void relayout() {
            setShouldCalcPreferredSize(true);
        }

        /// The view is on a form but the platform created no peer for it.
        boolean peerFailed() {
            return isInitialized() && getPeer() == null;
        }
    }
}
