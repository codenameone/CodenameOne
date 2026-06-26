/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.impl.ios;

import com.codename1.impl.ios.sim.bridge.RenderBridge;

import java.util.ArrayList;
import java.util.List;

/**
 * Parent-universe implementation of the render bridge: forwards drawing into
 * the native pipeline through the IOSNative bindings, translating local
 * coordinates by this bridge's region offset and confining clips to the
 * region.
 *
 * <p>Drawing is BATCHED per bridge and submitted atomically under a global
 * lock on {@link #flush()}: the simulator shell (parent universe) and the
 * isolated app (child universe) each render from their own EDT into the same
 * native op queue, and batching guarantees one universe's clip state never
 * bleeds into the other's draw calls.</p>
 */
public class RenderBridgeImpl implements RenderBridge {
    /** serializes batch submission + flush across all bridges/universes */
    private static final Object SUBMIT_LOCK = new Object();

    private final IOSNative nativeInstance = new IOSNative();
    private final int offsetX;
    private final int offsetY;
    private final int width;
    private final int height;

    /** one queued drawing op; replayed into IOSNative under the submit lock */
    private abstract static class Op {
        abstract void replay(IOSNative n);
    }

    private final List<Op> batch = new ArrayList<Op>();

    /**
     * @param offsetX left edge of this universe's region inside the window
     * @param offsetY top edge of this universe's region inside the window
     * @param width region width
     * @param height region height
     */
    public RenderBridgeImpl(int offsetX, int offsetY, int width, int height) {
        this(offsetX, offsetY, width, height, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * @param viewportX window-coordinate viewport this universe may paint
     * into - in zoom mode the app rectangle extends beyond the scroller and
     * must not bleed over the surrounding chrome
     */
    public RenderBridgeImpl(int offsetX, int offsetY, int width, int height,
            int viewportX, int viewportY, int viewportW, int viewportH) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.width = width;
        this.height = height;
        this.viewportX = viewportX;
        this.viewportY = viewportY;
        this.viewportW = viewportW;
        this.viewportH = viewportH;
    }

    private final int viewportX;
    private final int viewportY;
    private final int viewportW;
    private final int viewportH;

    private void add(Op op) {
        synchronized (batch) {
            batch.add(op);
        }
    }

    @Override
    public int getAppWidth() {
        return width;
    }

    @Override
    public int getAppHeight() {
        return height;
    }

    @Override
    public void flush() {
        if (com.codename1.impl.ios.sim.bridge.BridgeRegistry.isAppPaused()
                && this == com.codename1.impl.ios.sim.bridge.BridgeRegistry.getBridge()) {
            // Pause App: drop the app universe's frames so the shell's
            // paused overlay stays visible
            synchronized (batch) {
                batch.clear();
            }
            return;
        }
        List<Op> toSubmit;
        synchronized (batch) {
            if (batch.isEmpty()) {
                return;
            }
            toSubmit = new ArrayList<Op>(batch);
            batch.clear();
        }
        synchronized (SUBMIT_LOCK) {
            if (System.getenv("CN1_SIM_DEBUG") != null) {
                System.err.println("cn1sim: flush bridge=" + offsetX + "," + offsetY
                        + " " + width + "x" + height + " ops=" + toSubmit.size()
                        + " vp=" + viewportX + "," + viewportY + " "
                        + viewportW + "x" + viewportH);
            }
            // confine this universe's draws to its window region: the
            // universe clip is a scissor FLOOR that even "open the scissor"
            // ops (polygon fills, cached text) cannot escape, and the base
            // clip covers ops before the batch's first setClip
            int bx1 = Math.max(offsetX, viewportX);
            int by1 = Math.max(offsetY, viewportY);
            int bx2 = (int) Math.min((long) offsetX + width, (long) viewportX + viewportW);
            int by2 = (int) Math.min((long) offsetY + height, (long) viewportY + viewportH);
            int bw = Math.max(0, bx2 - bx1);
            int bh = Math.max(0, by2 - by1);
            nativeInstance.setUniverseClipGlobal(bx1, by1, bw, bh);
            nativeInstance.setNativeClippingGlobal(bx1, by1, bw, bh, true);
            for (Op op : toSubmit) {
                op.replay(nativeInstance);
            }
            nativeInstance.flushBuffer(0, 0, 0, width, height);
        }
    }

    @Override
    public void setClip(final int x, final int y, final int w, final int h) {
        // intersect with the region so a universe can never paint outside it
        int cx1 = Math.max(0, x);
        int cy1 = Math.max(0, y);
        int cx2 = Math.min(width, x + w);
        int cy2 = Math.min(height, y + h);
        // window-coordinate clamp against the viewport (zoom-mode scrolling
        // pushes the region partially outside the visible scroller)
        int wx1 = Math.max(offsetX + cx1, viewportX);
        int wy1 = Math.max(offsetY + cy1, viewportY);
        int wx2 = (int) Math.min((long) offsetX + cx2, (long) viewportX + viewportW);
        int wy2 = (int) Math.min((long) offsetY + cy2, (long) viewportY + viewportH);
        final int fx = wx1;
        final int fy = wy1;
        final int fw = Math.max(0, wx2 - wx1);
        final int fh = Math.max(0, wy2 - wy1);
        add(new Op() {
            void replay(IOSNative n) {
                n.setNativeClippingGlobal(fx, fy, fw, fh, true);
            }
        });
    }

    @Override
    public void fillRect(final int color, final int alpha, final int x, final int y, final int w, final int h) {
        add(new Op() {
            void replay(IOSNative n) {
                n.nativeFillRectGlobal(color, alpha, offsetX + x, offsetY + y, w, h);
            }
        });
    }

    @Override
    public void drawLine(final int color, final int alpha, final int x1, final int y1, final int x2, final int y2) {
        add(new Op() {
            void replay(IOSNative n) {
                n.nativeDrawLineGlobal(color, alpha, offsetX + x1, offsetY + y1, offsetX + x2, offsetY + y2);
            }
        });
    }

    @Override
    public void drawString(final int color, final int alpha, final long fontPeer, final String str,
            final int x, final int y) {
        add(new Op() {
            void replay(IOSNative n) {
                n.nativeDrawStringGlobal(color, alpha, fontPeer, str, offsetX + x, offsetY + y);
            }
        });
    }

    @Override
    public void drawImage(final long peer, final int alpha, final int x, final int y, final int w, final int h) {
        add(new Op() {
            void replay(IOSNative n) {
                n.nativeDrawImageGlobal(peer, alpha, offsetX + x, offsetY + y, w, h, 0);
            }
        });
    }

    @Override
    public void tileImage(final long peer, final int alpha, final int x, final int y, final int w, final int h) {
        add(new Op() {
            void replay(IOSNative n) {
                n.nativeTileImageGlobal(peer, alpha, offsetX + x, offsetY + y, w, h);
            }
        });
    }

    /* ---- synchronous queries (no batching needed) --------------------------- */

    @Override
    public long createSystemFont(int face, int style, int size) {
        return nativeInstance.createSystemFont(face, style, size);
    }

    @Override
    public long createTruetypeFont(String name) {
        return nativeInstance.createTruetypeFont(name);
    }

    @Override
    public long deriveTruetypeFont(long peer, boolean bold, boolean italic, float size) {
        return nativeInstance.deriveTruetypeFont(peer, bold, italic, size);
    }

    @Override
    public int stringWidth(long peer, String str) {
        return nativeInstance.stringWidthNative(peer, str);
    }

    @Override
    public int charWidth(long peer, char ch) {
        return nativeInstance.charWidthNative(peer, ch);
    }

    @Override
    public int fontHeight(long peer) {
        return nativeInstance.getFontHeightNative(peer);
    }

    @Override
    public int fontAscent(long peer) {
        return nativeInstance.fontAscentNative(peer);
    }

    @Override
    public int fontDescent(long peer) {
        return nativeInstance.fontDescentNative(peer);
    }

    @Override
    public long[] createImage(byte[] data) {
        int[] wh = new int[2];
        long peer = nativeInstance.createImage(data, wh);
        if (peer == 0) {
            return null;
        }
        return new long[]{peer, wh[0], wh[1]};
    }

    @Override
    public long createImageFromARGB(int[] argb, int w, int h) {
        return nativeInstance.createImageFromARGB(argb, w, h);
    }

    @Override
    public void imageRgbToIntArray(long peer, int[] arr, int x, int y, int w, int h, int imgW, int imgH) {
        nativeInstance.imageRgbToIntArray(peer, arr, x, y, w, h, imgW, imgH);
    }

    @Override
    public long scaleImage(long peer, int w, int h) {
        return nativeInstance.scale(peer, w, h);
    }

    @Override
    public void releasePeer(long peer) {
        nativeInstance.releasePeer(peer);
    }

    @Override
    public void setNativeMenu(String encodedCommands) {
        nativeInstance.setNativeMenuCommands(encodedCommands);
    }

    @Override
    public boolean saveScreenshot(String path, int x, int y, int w, int h) {
        return com.codename1.impl.ios.sim.CN1SimHost.saveScreenshot(path, x, y, w, h);
    }

    @Override
    public void editString(String text, int x, int y, int w, int h,
            int fontHeightPx, int fontStyle, int fgColor, int bgColor, int bgTransparency,
            boolean multiline, int constraint, int align) {
        // Legacy JNI editing path: the native editor keeps its 8-arg signature
        // (a font peer, no style block); pass the pixel height in the peer slot
        // so the native side still sizes the font, dropping the rest rather than
        // forcing a dylib re-spin.
        com.codename1.impl.ios.sim.CN1SimHost.editString(text,
                offsetX + x, offsetY + y, w, h, fontHeightPx, fgColor, multiline);
    }

    /* mutable images: immediate CPU-side calls, no batching - the bitmap is
     * independent of the screen pipeline */

    @Override
    public long createMutableImage(int w, int h, int argb) {
        return nativeInstance.createMutableImageSim(w, h, argb);
    }

    @Override
    public void mutableClip(long peer, int x, int y, int w, int h) {
        nativeInstance.mutableClipSim(peer, x, y, w, h);
    }

    @Override
    public void mutableFillRect(long peer, int color, int alpha, int x, int y, int w, int h) {
        nativeInstance.mutableFillRectSim(peer, color, alpha, x, y, w, h);
    }

    @Override
    public void mutableDrawLine(long peer, int color, int alpha, int x1, int y1, int x2, int y2) {
        nativeInstance.mutableDrawLineSim(peer, color, alpha, x1, y1, x2, y2);
    }

    @Override
    public void mutableDrawString(long peer, long fontPeer, int color, int alpha, String str,
            int x, int y) {
        nativeInstance.mutableDrawStringSim(peer, fontPeer, color, alpha, str, x, y);
    }

    @Override
    public void mutableDrawImage(long peer, long imgPeer, int alpha, int x, int y, int w, int h) {
        nativeInstance.mutableDrawImageSim(peer, imgPeer, alpha, x, y, w, h);
    }

    @Override
    public void mutableShape(long peer, byte[] commands, int commandsLen, float[] points,
            int pointsLen, int color, int alpha, boolean stroke, float lineWidth,
            int capStyle, int joinStyle, float miterLimit) {
        byte[] cmd = new byte[commandsLen];
        System.arraycopy(commands, 0, cmd, 0, commandsLen);
        float[] pts = new float[pointsLen];
        System.arraycopy(points, 0, pts, 0, pointsLen);
        nativeInstance.mutableShapeSim(peer, cmd, commandsLen, pts, pointsLen, color, alpha,
                stroke, lineWidth, capStyle, joinStyle, miterLimit);
    }

    @Override
    public void finishMutable(long peer) {
        nativeInstance.mutableFinishSim(peer);
    }

    /* native peers + media + picker: direct host calls (parent universe owns
     * the JNI binding); frames translate by this bridge's region offset */

    @Override
    public long peerCreateWebView() {
        return com.codename1.impl.ios.sim.CN1SimHost.peerCreateWebView();
    }

    @Override
    public void peerWebLoadURL(long peer, String url) {
        com.codename1.impl.ios.sim.CN1SimHost.peerWebLoadURL(peer, url);
    }

    @Override
    public void peerWebLoadHTML(long peer, String html, String baseUrl) {
        com.codename1.impl.ios.sim.CN1SimHost.peerWebLoadHTML(peer, html, baseUrl);
    }

    @Override
    public void peerSetFrame(long peer, int x, int y, int w, int h) {
        com.codename1.impl.ios.sim.CN1SimHost.peerSetFrame(peer, offsetX + x, offsetY + y, w, h);
    }

    @Override
    public void peerRemove(long peer) {
        com.codename1.impl.ios.sim.CN1SimHost.peerRemove(peer);
    }

    @Override
    public void peerRelease(long peer) {
        com.codename1.impl.ios.sim.CN1SimHost.peerRelease(peer);
    }

    @Override
    public long mediaCreate(String url, boolean video) {
        return com.codename1.impl.ios.sim.CN1SimHost.mediaCreate(url, video);
    }

    @Override
    public void mediaControl(long peer, int op, int arg) {
        com.codename1.impl.ios.sim.CN1SimHost.mediaControl(peer, op, arg);
    }

    @Override
    public int mediaQuery(long peer, int what) {
        return com.codename1.impl.ios.sim.CN1SimHost.mediaQuery(peer, what);
    }

    @Override
    public void pickFile() {
        com.codename1.impl.ios.sim.CN1SimHost.pickFile();
    }

    @Override
    public void capturePhoto() {
        // Legacy JNI path has no separate camera entry yet; fall back to the
        // existing file picker so capture is never a dead end.
        com.codename1.impl.ios.sim.CN1SimHost.pickFile();
    }

    @Override
    public void shape(byte[] commands, final int commandsLen, float[] points,
            final int pointsLen, final int color, final int alpha, final boolean stroke,
            final float lineWidth, final int capStyle, final int joinStyle,
            final float miterLimit) {
        // batched ops replay later - copy the caller's (reused) arrays
        final byte[] cmd = new byte[commandsLen];
        System.arraycopy(commands, 0, cmd, 0, commandsLen);
        final float[] pts = new float[pointsLen];
        System.arraycopy(points, 0, pts, 0, pointsLen);
        add(new Op() {
            void replay(IOSNative n) {
                n.nativeShapeGlobalSim(cmd, commandsLen, pts, pointsLen, color, alpha,
                        stroke, lineWidth, capStyle, joinStyle, miterLimit,
                        offsetX, offsetY);
            }
        });
    }
}
