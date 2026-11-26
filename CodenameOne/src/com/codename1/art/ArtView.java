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
package com.codename1.art;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.GeneralPath;

import java.util.List;

/// Shows an `ArtModel`'s document, with panning, pinch zoom, hit testing and the selection
/// drawn over it.
///
/// Dragging pans the document unless a `PointerHandler` is set; `ArtEditor` sets one per
/// tool. The rendered document is cached and rendered again only when the model, the zoom,
/// the pan or the size changes.
public class ArtView extends Component implements ArtListener {
    /// Receives pointer events in document coordinates.
    public interface PointerHandler {
        /// The pointer went down.
        ///
        /// #### Parameters
        ///
        /// - `view`: the view
        /// - `x`: document x
        /// - `y`: document y
        void pressed(ArtView view, double x, double y);

        /// The pointer moved while down.
        ///
        /// #### Parameters
        ///
        /// - `view`: the view
        /// - `x`: document x
        /// - `y`: document y
        void dragged(ArtView view, double x, double y);

        /// The pointer went up.
        ///
        /// #### Parameters
        ///
        /// - `view`: the view
        /// - `x`: document x
        /// - `y`: document y
        void released(ArtView view, double x, double y);
    }

    private ArtModel model;
    private double zoom = 1;
    private double panX;
    private double panY;
    private boolean fitPending = true;
    private Image cache;
    private int cacheW;
    private int cacheH;
    private boolean dirty = true;
    private PointerHandler handler;
    private int lastX;
    private int lastY;
    private boolean panning;
    private double pinchStartZoom;
    private boolean showSelection = true;
    private int selectionColor = 0x1e88e5;

    /// Creates a view of an empty document.
    public ArtView() {
        this(new ArtModel());
    }

    /// Creates a view.
    ///
    /// #### Parameters
    ///
    /// - `model`: the model to show
    public ArtView(ArtModel model) {
        setUIID("ArtView");
        setFocusable(true);
        setModel(model);
    }

    /// The model.
    public ArtModel getModel() {
        return model;
    }

    /// Shows another model.
    ///
    /// #### Parameters
    ///
    /// - `m`: the model
    public final void setModel(ArtModel m) {
        if (model != null && isInitialized()) {
            model.removeArtListener(this);
        }
        model = m;
        if (isInitialized()) {
            model.addArtListener(this);
        }
        fitPending = true;
        invalidateCache();
    }

    // The view listens only while it is on screen, so a model that outlives its forms does
    // not keep every view it was ever shown in.
    @Override
    protected void initComponent() {
        super.initComponent();
        model.addArtListener(this);
        invalidateCache();
    }

    @Override
    protected void deinitialize() {
        model.removeArtListener(this);
        super.deinitialize();
    }

    @Override
    public void artChanged(ArtModel m, boolean contentChanged) {
        if (contentChanged) {
            invalidateCache();
        } else {
            repaint();
        }
    }

    /// Renders the document again on the next paint.
    public void invalidateCache() {
        dirty = true;
        repaint();
    }

    /// The pointer handler, or null when dragging pans.
    public PointerHandler getPointerHandler() {
        return handler;
    }

    /// Sets the pointer handler.
    ///
    /// #### Parameters
    ///
    /// - `h`: the handler, or null to pan on drag
    public void setPointerHandler(PointerHandler h) {
        handler = h;
    }

    /// True draws an outline and handles around the selected nodes.
    ///
    /// #### Parameters
    ///
    /// - `show`: true to draw the selection
    public void setShowSelection(boolean show) {
        showSelection = show;
        repaint();
    }

    /// The zoom: device pixels per document unit.
    public double getZoom() {
        return zoom;
    }

    /// Sets the zoom, keeping the middle of the view where it is.
    ///
    /// #### Parameters
    ///
    /// - `z`: device pixels per document unit
    public void setZoom(double z) {
        double nz = Math.max(0.02, Math.min(64, z));
        double cx = getWidth() / 2.0;
        double cy = getHeight() / 2.0;
        panX = cx - (cx - panX) * nz / zoom;
        panY = cy - (cy - panY) * nz / zoom;
        zoom = nz;
        fitPending = false;
        invalidateCache();
    }

    /// Pans the view.
    ///
    /// #### Parameters
    ///
    /// - `x`: where the document's origin is, in pixels from the view's left
    /// - `y`: in pixels from the view's top
    public void setPan(double x, double y) {
        panX = x;
        panY = y;
        fitPending = false;
        invalidateCache();
    }

    /// Zooms and centers so the whole document is visible.
    public void fit() {
        ArtDocument d = model.getDocument();
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0 || d.getWidth() <= 0 || d.getHeight() <= 0) {
            fitPending = true;
            return;
        }
        double margin = Display.getInstance().convertToPixels(4);
        zoom = Math.max(0.02, Math.min((w - 2 * margin) / d.getWidth(), (h - 2 * margin) / d.getHeight()));
        panX = (w - d.getWidth() * zoom) / 2;
        panY = (h - d.getHeight() * zoom) / 2;
        fitPending = false;
        invalidateCache();
    }

    /// The transform from document coordinates to the view's pixels.
    public Matrix getViewTransform() {
        return Matrix.translate(panX, panY).multiply(Matrix.scale(zoom, zoom));
    }

    /// Converts a screen x (as pointer events report it) to a document x.
    ///
    /// #### Parameters
    ///
    /// - `screenX`: absolute x
    public double toDocumentX(int screenX) {
        return (screenX - getAbsoluteX() - panX) / zoom;
    }

    /// Converts a screen y to a document y.
    ///
    /// #### Parameters
    ///
    /// - `screenY`: absolute y
    public double toDocumentY(int screenY) {
        return (screenY - getAbsoluteY() - panY) / zoom;
    }

    /// The topmost visible, unlocked node under a document point, searching into groups; null
    /// when there is none.
    ///
    /// #### Parameters
    ///
    /// - `x`: document x
    /// - `y`: document y
    public ArtNode nodeAt(double x, double y) {
        double slop = Display.getInstance().convertToPixels(1.5f) / zoom;
        return hit(model.getDocument().getRoot(), Matrix.IDENTITY, x, y, slop);
    }

    private ArtNode hit(GroupNode g, Matrix parent, double x, double y, double slop) {
        Matrix m = parent.multiply(g.getTransform());
        List<ArtNode> ch = g.getChildren();
        for (int i = ch.size() - 1; i >= 0; i--) {
            ArtNode c = ch.get(i);
            if (!c.isVisible() || c.isLocked() || c instanceof AdjustmentNode) {
                continue;
            }
            if (c instanceof GroupNode) {
                ArtNode h = hit((GroupNode) c, m, x, y, slop);
                if (h != null) {
                    return h;
                }
                continue;
            }
            Matrix inv = m.multiply(c.getTransform()).invert();
            if (inv == null) {
                continue;
            }
            double lx = inv.mapX(x, y);
            double ly = inv.mapY(x, y);
            double s = slop * inv.meanScale();
            double[] b = c.getLocalBounds();
            if (b == null || lx < b[0] - s || ly < b[1] - s || lx > b[0] + b[2] + s || ly > b[1] + b[3] + s) {
                continue;
            }
            if (hitsLeaf(c, lx, ly, s)) {
                return c;
            }
        }
        return null;
    }

    /// Whether a point in a leaf's own space hits it, `s` being the slop in that space.
    private static boolean hitsLeaf(ArtNode c, double lx, double ly, double s) {
        if (c instanceof ShapeNode) {
            ShapeNode sn = (ShapeNode) c;
            GeneralPath p = sn.getPath();
            boolean filled = !sn.getFills().isEmpty();
            if (filled && Polylines.flatten(p, Matrix.IDENTITY, 0.5).contains(lx, ly)) {
                return true;
            }
            // Strokes and unfilled outlines are hit near the outline.
            return (!filled || !sn.getStrokes().isEmpty()) && nearOutline(p, lx, ly, s + strokeHalf(sn));
        }
        if (c instanceof RasterNode) {
            Raster r = ((RasterNode) c).getImage();
            return (r.get((int) Math.floor(lx), (int) Math.floor(ly)) >>> 24) >= 16;
        }
        return true;
    }

    private static double strokeHalf(ShapeNode s) {
        double w = 0;
        for (int i = 0; i < s.getStrokes().size(); i++) {
            w = Math.max(w, s.getStrokes().get(i).getWidth() / 2.0);
        }
        return w;
    }

    private static boolean nearOutline(GeneralPath p, double x, double y, double d) {
        Polylines pl = Polylines.flatten(p, Matrix.IDENTITY, 0.5);
        for (int k = 0; k < pl.lines.size(); k++) {
            double[] pts = pl.lines.get(k);
            int n = pts.length;
            for (int i = 0; i + 3 < n; i += 2) {
                if (segDist(x, y, pts[i], pts[i + 1], pts[i + 2], pts[i + 3]) <= d) {
                    return true;
                }
            }
            if (pl.closed.get(k).booleanValue() && n >= 4 && segDist(x, y, pts[n - 2], pts[n - 1], pts[0], pts[1]) <= d) {
                return true;
            }
        }
        return false;
    }

    private static double segDist(double x, double y, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double l2 = dx * dx + dy * dy;
        double t = l2 == 0 ? 0 : Math.max(0, Math.min(1, ((x - x1) * dx + (y - y1) * dy) / l2));
        double px = x1 + t * dx - x;
        double py = y1 + t * dy - y;
        return Math.sqrt(px * px + py * py);
    }

    /// The bounds of a node in view pixels, as `x, y, w, h`, or null.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    public double[] viewBounds(ArtNode n) {
        double[] b = n.getLocalBounds();
        if (b == null) {
            return null;
        }
        Matrix m = getViewTransform().multiply(model.toDocument(n));
        double[] xs = {b[0], b[0] + b[2], b[0] + b[2], b[0]};
        double[] ys = {b[1], b[1], b[1] + b[3], b[1] + b[3]};
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            double x = m.mapX(xs[i], ys[i]);
            double y = m.mapY(xs[i], ys[i]);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        return new double[]{minX, minY, maxX - minX, maxY - minY};
    }

    @Override
    protected Dimension calcPreferredSize() {
        ArtDocument d = model.getDocument();
        return new Dimension((int) Math.min(Display.getInstance().getDisplayWidth(), Math.ceil(d.getWidth())),
                (int) Math.min(Display.getInstance().getDisplayHeight(), Math.ceil(d.getHeight())));
    }

    @Override
    public void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (fitPending) {
            fit();
        }
        g.setColor(0x9e9e9e);
        g.fillRect(getX(), getY(), w, h);
        ArtDocument d = model.getDocument();
        int dx = (int) Math.floor(panX);
        int dy = (int) Math.floor(panY);
        int dw = (int) Math.ceil(d.getWidth() * zoom);
        int dh = (int) Math.ceil(d.getHeight() * zoom);
        checker(g, getX() + Math.max(0, dx), getY() + Math.max(0, dy), Math.min(w, dx + dw) - Math.max(0, dx),
                Math.min(h, dy + dh) - Math.max(0, dy));
        if (dirty || cache == null || cacheW != w || cacheH != h) {
            Raster r = new ArtRenderer().render(d, getViewTransform(), new int[]{0, 0, w, h});
            cache = Image.createImage(r.getPixels(), w, h);
            cacheW = w;
            cacheH = h;
            dirty = false;
        }
        g.drawImage(cache, getX(), getY());
        if (showSelection) {
            paintSelection(g);
        }
    }

    private static void checker(Graphics g, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int size = Math.max(4, Display.getInstance().convertToPixels(1.5f));
        int[] clip = g.getClip();
        g.clipRect(x, y, w, h);
        g.setColor(0xffffff);
        g.fillRect(x, y, w, h);
        g.setColor(0xd6d6d6);
        for (int yy = 0; yy * size < h; yy++) {
            for (int xx = yy % 2; xx * size < w; xx += 2) {
                g.fillRect(x + xx * size, y + yy * size, size, size);
            }
        }
        g.setClip(clip);
    }

    private void paintSelection(Graphics g) {
        List<ArtNode> sel = model.getSelection();
        if (sel.isEmpty()) {
            return;
        }
        int handle = Math.max(6, Display.getInstance().convertToPixels(1.2f));
        g.setColor(selectionColor);
        for (int i = 0; i < sel.size(); i++) {
            double[] b = viewBounds(sel.get(i));
            if (b == null) {
                continue;
            }
            int x = getX() + (int) Math.round(b[0]);
            int y = getY() + (int) Math.round(b[1]);
            int bw = (int) Math.round(b[2]);
            int bh = (int) Math.round(b[3]);
            g.drawRect(x, y, bw, bh);
            if (i == 0) {
                int[] hx = {x, x + bw, x + bw, x};
                int[] hy = {y, y, y + bh, y + bh};
                for (int k = 0; k < 4; k++) {
                    g.setColor(0xffffff);
                    g.fillRect(hx[k] - handle / 2, hy[k] - handle / 2, handle, handle);
                    g.setColor(selectionColor);
                    g.drawRect(hx[k] - handle / 2, hy[k] - handle / 2, handle, handle);
                }
            }
        }
    }

    @Override
    public void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);
        lastX = x;
        lastY = y;
        pinchStartZoom = 0;
        if (handler != null) {
            panning = false;
            handler.pressed(this, toDocumentX(x), toDocumentY(y));
        } else {
            panning = true;
        }
    }

    @Override
    public void pointerDragged(int x, int y) {
        if (pinchStartZoom > 0) {
            return;
        }
        if (panning || handler == null) {
            panX += x - lastX;
            panY += y - lastY;
            lastX = x;
            lastY = y;
            fitPending = false;
            invalidateCache();
            return;
        }
        handler.dragged(this, toDocumentX(x), toDocumentY(y));
    }

    @Override
    public void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
        if (pinchStartZoom > 0) {
            pinchStartZoom = 0;
            return;
        }
        if (!panning && handler != null) {
            handler.released(this, toDocumentX(x), toDocumentY(y));
        }
        panning = false;
    }

    @Override
    protected boolean pinch(float scale) {
        if (pinchStartZoom <= 0) {
            pinchStartZoom = zoom;
        }
        setZoom(pinchStartZoom * scale);
        return true;
    }

    @Override
    protected void pinchReleased(int x, int y) {
        pinchStartZoom = 0;
    }
}
