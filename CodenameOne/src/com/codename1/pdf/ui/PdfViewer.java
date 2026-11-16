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
package com.codename1.pdf.ui;

import com.codename1.impl.pdf.PdfContent;
import com.codename1.impl.pdf.PdfDict;
import com.codename1.impl.pdf.PdfFile;
import com.codename1.impl.pdf.PdfRenderer;
import com.codename1.io.Util;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/// Displays a PDF document: pages stacked vertically and scrolled
/// continuously, fitted to the width of the component and zoomed by pinch
/// or double tap.
///
/// Pages are drawn by a pure-Java renderer, so a document looks the same
/// on every platform and needs no native viewer:
///
/// ```java
/// PdfViewer viewer = new PdfViewer(pdfBytes);
/// form.add(BorderLayout.CENTER, viewer);
/// viewer.setPage(3);
/// ```
///
/// Pages render one at a time on the EDT, interleaved with other events,
/// and only the pages near the view are kept as images. A page that is
/// not drawn yet shows as a blank sheet.
///
/// The renderer covers vector graphics with dashes, blend modes,
/// transparency and knockout groups and soft masks; every shading type;
/// embedded TrueType, OpenType, CFF, Type 1 and Type 3 fonts, horizontal
/// and vertical; images in every PDF filter, JPEG 2000, JBIG2 and CCITT
/// fax included; and annotation appearances. Fonts that are not embedded
/// are drawn with a platform font of the same family, so their shapes
/// differ from the original.
public class PdfViewer extends Component {
    private static final int GAP = 8;
    private static final int CACHED_PAGES = 6;
    private static final float MAX_ZOOM = 5f;

    private final PdfFile file;
    private final List<PdfDict> pages;
    private final double[] widths;
    private final double[] heights;
    private double maxWidth;
    private float zoom = 1f;
    private float pinchStart = -1;
    private final Image[] cache;
    private final double[] cacheScale;
    private final long[] cacheUse;
    private long useCounter;
    private boolean renderQueued;
    private long lastRelease;
    private int lastWidth = -1;

    /// A viewer for the PDF in `data`.
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the data is not a PDF, or it is encrypted
    ///   with a user password.
    public PdfViewer(byte[] data) throws IOException {
        this(data, "");
    }

    /// A viewer for the PDF in `data`, opened with `password` (the user or
    /// the owner password) when it is encrypted.
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the data is not a PDF or the password is wrong.
    public PdfViewer(byte[] data, String password) throws IOException {
        file = new PdfFile(data, password == null ? "" : password);
        pages = file.getPages();
        int n = pages.size();
        widths = new double[n];
        heights = new double[n];
        for (int i = 0; i < n; i++) {
            PdfDict p = pages.get(i);
            double[] b = PdfContent.box(p);
            boolean swap = PdfContent.rotation(p) % 180 != 0;
            widths[i] = swap ? b[3] : b[2];
            heights[i] = swap ? b[2] : b[3];
            maxWidth = Math.max(maxWidth, widths[i]);
        }
        cache = new Image[n];
        cacheScale = new double[n];
        cacheUse = new long[n];
        setFocusable(true);
        setUIID("PdfViewer");
    }

    /// A viewer for the PDF read from `in`, which is closed.
    ///
    /// #### Throws
    ///
    /// - `IOException`: when reading fails or the data is not a PDF.
    public static PdfViewer open(InputStream in) throws IOException {
        try {
            return new PdfViewer(Util.readInputStream(in));
        } finally {
            Util.cleanup(in);
        }
    }

    /// The number of pages.
    public int getPageCount() {
        return pages.size();
    }

    /// The zoom factor, 1 when a page fits the width.
    public float getZoom() {
        return zoom;
    }

    /// Sets the zoom factor, clamped to 1..5, keeping the middle of the
    /// view in place.
    public void setZoom(float z) {
        float nz = Math.max(1f, Math.min(MAX_ZOOM, z));
        if (Math.abs(nz - zoom) < 1e-4f) {
            return;
        }
        int h = Math.max(1, getHeight());
        int w = Math.max(1, getWidth());
        double cy = (getScrollY() + h / 2.0) / zoom;
        double cx = (getScrollX() + w / 2.0) / zoom;
        zoom = nz;
        setShouldCalcPreferredSize(true);
        Dimension pref = getPreferredSize();
        int maxY = Math.max(0, pref.getHeight() - h);
        int maxX = Math.max(0, pref.getWidth() - w);
        setScrollY(clamp((int) (cy * zoom - h / 2.0), 0, maxY));
        setScrollX(clamp((int) (cx * zoom - w / 2.0), 0, maxX));
        repaint();
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }

    /// The page at the middle of the view, zero based.
    public int getPage() {
        double s = scale();
        int mid = getScrollY() + getHeight() / 2;
        int y = GAP;
        for (int i = 0; i < pages.size(); i++) {
            y += (int) (heights[i] * s) + GAP;
            if (y > mid) {
                return i;
            }
        }
        return Math.max(0, pages.size() - 1);
    }

    /// Scrolls so that page `index` (zero based) is at the top of the view.
    public void setPage(int index) {
        if (index < 0 || index >= pages.size()) {
            return;
        }
        int y = pageTop(index, scale()) - GAP;
        int max = Math.max(0, getPreferredSize().getHeight() - getHeight());
        setScrollY(clamp(y, 0, max));
        repaint();
    }

    /// Renders page `index` (zero based) to an image at `scale` pixels per
    /// point, white background, annotations included. Usable without
    /// showing the viewer, for a thumbnail or a share sheet.
    public Image renderPage(int index, double scale) {
        int w = Math.max(1, (int) Math.ceil(widths[index] * scale));
        int h = Math.max(1, (int) Math.ceil(heights[index] * scale));
        // The page is drawn on a transparent surface and put on white
        // paper afterwards: in PDF the page is a transparency group whose
        // backdrop is transparent, which blend modes can tell apart from
        // white.
        Image img = Image.createImage(w, h, 0);
        Graphics g = img.getGraphics();
        new PdfRenderer(file, new GraphicsPdfDevice(g, img, w, h)).render(pages.get(index), scale, true);
        Image paper = Image.createImage(w, h, 0xffffffff);
        paper.getGraphics().drawImage(img, 0, 0);
        return paper;
    }

    /// Pixels per point at the current width and zoom.
    private double scale() {
        int w = getWidth();
        if (w <= 0) {
            w = CN.getDisplayWidth();
        }
        double avail = Math.max(1, w - GAP * 2);
        return maxWidth <= 0 ? 1 : avail / maxWidth * zoom;
    }

    private int pageTop(int index, double s) {
        int y = GAP;
        for (int i = 0; i < index; i++) {
            y += (int) (heights[i] * s) + GAP;
        }
        return y;
    }

    @Override
    public boolean isScrollableY() {
        return true;
    }

    @Override
    public boolean isScrollableX() {
        return zoom > 1f;
    }

    @Override
    protected Dimension calcPreferredSize() {
        double s = scale();
        int h = pageTop(pages.size(), s);
        int w = (int) (maxWidth * s) + GAP * 2;
        return new Dimension(w, h);
    }

    @Override
    protected void laidOut() {
        super.laidOut();
        if (getWidth() != lastWidth) {
            lastWidth = getWidth();
            setShouldCalcPreferredSize(true);
        }
    }

    @Override
    public void paint(Graphics g) {
        double s = scale();
        int top = getScrollY();
        int bottom = top + getHeight();
        int contentW = (int) (maxWidth * s) + GAP * 2;
        int baseX = getX() + Math.max(0, (getWidth() - contentW) / 2);
        int y = getY() + GAP;
        int first = -1;
        int last = -1;
        for (int i = 0; i < pages.size(); i++) {
            int pw = (int) (widths[i] * s);
            int ph = (int) (heights[i] * s);
            int rel = y - getY();
            if (rel + ph >= top && rel <= bottom) {
                if (first < 0) {
                    first = i;
                }
                last = i;
                int x = baseX + GAP + ((int) (maxWidth * s) - pw) / 2;
                g.setColor(0xffffff);
                g.fillRect(x, y, pw, ph);
                Image img = cache[i];
                if (img != null) {
                    cacheUse[i] = ++useCounter;
                    if (img.getWidth() == pw) {
                        g.drawImage(img, x, y);
                    } else {
                        g.drawImage(img, x, y, pw, ph);
                    }
                }
                g.setColor(0xc0c0c0);
                g.drawRect(x, y, pw - 1, ph - 1);
            }
            y += ph + GAP;
        }
        if (first >= 0) {
            scheduleRender(first, last, s);
        }
    }

    private void scheduleRender(final int first, final int last, final double s) {
        if (renderQueued) {
            return;
        }
        int target = -1;
        for (int i = first; i <= last; i++) {
            if (cache[i] == null || Math.abs(cacheScale[i] - s) > 1e-6) {
                target = i;
                break;
            }
        }
        if (target < 0 || pinchStart > 0) {
            return;
        }
        renderQueued = true;
        final int page = target;
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                renderQueued = false;
                if (!isInitialized()) {
                    return;
                }
                double now = scale();
                Image img;
                try {
                    img = renderPage(page, now);
                } catch (RuntimeException e) {
                    // Draw a malformed page as blank rather than retrying it.
                    img = Image.createImage(1, 1, 0xffffffff);
                }
                store(page, img, now);
                repaint();
            }
        });
    }

    private void store(int page, Image img, double s) {
        cache[page] = img;
        cacheScale[page] = s;
        cacheUse[page] = ++useCounter;
        int count = 0;
        for (int i = 0; i < cache.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            if (cache[i] != null) {
                count++;
            }
        }
        while (count > CACHED_PAGES) {
            int oldest = -1;
            for (int i = 0; i < cache.length; i++) {
                if (cache[i] != null && i != page && (oldest < 0 || cacheUse[i] < cacheUse[oldest])) {
                    oldest = i;
                }
            }
            if (oldest < 0) {
                break;
            }
            cache[oldest] = null;
            count--;
        }
    }

    @Override
    protected boolean pinch(float scale) {
        if (pinchStart < 0) {
            pinchStart = zoom;
        }
        setZoom(pinchStart * scale);
        return true;
    }

    @Override
    protected void pinchReleased(int x, int y) {
        super.pinchReleased(x, y);
        pinchStart = -1;
        repaint();
    }

    @Override
    public void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
        long now = System.currentTimeMillis();
        if (now - lastRelease < 300) {
            setZoom(zoom > 1.01f ? 1f : 2f);
            lastRelease = 0;
        } else {
            lastRelease = now;
        }
    }

    @Override
    protected void deinitialize() {
        for (int i = 0; i < cache.length; i++) {
            cache[i] = null;
        }
        super.deinitialize();
    }
}
