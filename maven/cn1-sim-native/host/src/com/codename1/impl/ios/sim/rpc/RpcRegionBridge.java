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
package com.codename1.impl.ios.sim.rpc;

import com.codename1.impl.ios.sim.bridge.RenderBridge;

import java.util.ArrayList;
import java.util.List;

/**
 * A universe's view of the relay connection: draw operations translate from
 * universe-local space into window coordinates and clips clamp to the
 * universe's region intersected with the zoom viewport - the RPC equivalent
 * of the JNI-era RenderBridgeImpl(offset, viewport) facade. Each facade keeps
 * its own batch so shell and app frames never interleave mid-frame; resource
 * operations (fonts, images, peers, mutables) share the connection's global
 * handle space.
 */
public class RpcRegionBridge implements RenderBridge {
    private final RpcRenderBridge conn;
    private final int offX;
    private final int offY;
    private final int width;
    private final int height;
    private final int vpX;
    private final int vpY;
    private final int vpW;
    private final int vpH;
    private final List<byte[]> batch = new ArrayList<byte[]>();

    public RpcRegionBridge(RpcRenderBridge conn, int offX, int offY, int width, int height) {
        this(conn, offX, offY, width, height, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public RpcRegionBridge(RpcRenderBridge conn, int offX, int offY, int width, int height,
            int vpX, int vpY, int vpW, int vpH) {
        this.conn = conn;
        this.offX = offX;
        this.offY = offY;
        this.width = width;
        this.height = height;
        this.vpX = vpX;
        this.vpY = vpY;
        this.vpW = vpW;
        this.vpH = vpH;
    }

    private void addOp(byte[] encoded) {
        synchronized (batch) {
            batch.add(encoded);
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
        List<byte[]> ops;
        synchronized (batch) {
            if (batch.isEmpty()) {
                return;
            }
            ops = new ArrayList<byte[]>(batch);
            batch.clear();
        }
        conn.sendBatch(ops);
    }

    /* ---- draw ops: translate + clamp ---------------------------------------- */

    @Override
    public void setClip(int x, int y, int w, int h) {
        // the scissor floor: a universe can never clip wider than its own
        // region intersected with the viewport
        int cx = offX + x;
        int cy = offY + y;
        int cx2 = cx + w;
        int cy2 = cy + h;
        int fx = Math.max(offX, vpX);
        int fy = Math.max(offY, vpY);
        long fx2l = Math.min((long) offX + width, (long) vpX + vpW);
        long fy2l = Math.min((long) offY + height, (long) vpY + vpH);
        int fx2 = (int) Math.min(Integer.MAX_VALUE, fx2l);
        int fy2 = (int) Math.min(Integer.MAX_VALUE, fy2l);
        cx = Math.max(cx, fx);
        cy = Math.max(cy, fy);
        cx2 = Math.min(cx2, fx2);
        cy2 = Math.min(cy2, fy2);
        addOp(RpcRenderBridge.enc(1 /* B_CLIP */, cx, cy, Math.max(0, cx2 - cx),
                Math.max(0, cy2 - cy)));
    }

    @Override
    public void fillRect(int color, int alpha, int x, int y, int w, int h) {
        addOp(RpcRenderBridge.enc(2 /* B_FILL_RECT */, color, alpha, offX + x, offY + y, w, h));
    }

    @Override
    public void drawLine(int color, int alpha, int x1, int y1, int x2, int y2) {
        addOp(RpcRenderBridge.enc(3 /* B_DRAW_LINE */, color, alpha, offX + x1, offY + y1,
                offX + x2, offY + y2));
    }

    @Override
    public void drawString(int color, int alpha, long fontPeer, String str, int x, int y) {
        addOp(RpcRenderBridge.encDrawString(color, alpha, fontPeer, str, offX + x, offY + y));
    }

    @Override
    public void drawImage(long peer, int alpha, int x, int y, int w, int h) {
        if (w > 500 && System.getProperty("cn1.sim.rpc.debug") != null) {
            System.out.println("[cn1sim-rpc] drawImage peer=" + peer + " at " + (offX + x)
                    + "," + (offY + y) + " " + w + "x" + h + " off=" + offX + "," + offY);
        }
        addOp(RpcRenderBridge.enc(5 /* B_DRAW_IMAGE */, (int) peer, alpha, offX + x, offY + y, w, h));
    }

    @Override
    public void tileImage(long peer, int alpha, int x, int y, int w, int h) {
        addOp(RpcRenderBridge.enc(6 /* B_TILE_IMAGE */, (int) peer, alpha, offX + x, offY + y, w, h));
    }

    @Override
    public void shape(byte[] commands, int commandsLen, float[] points, int pointsLen,
            int color, int alpha, boolean stroke, float lineWidth, int capStyle,
            int joinStyle, float miterLimit) {
        addOp(RpcRenderBridge.encShape(commands, commandsLen, points, pointsLen, color,
                alpha, stroke, lineWidth, capStyle, joinStyle, miterLimit, offX, offY));
    }

    /* ---- coordinate-bearing pass-throughs ------------------------------------ */

    @Override
    public void editString(String text, int x, int y, int w, int h,
            int fontHeightPx, int fontStyle, int fgColor, int bgColor, int bgTransparency,
            boolean multiline, int constraint, int align) {
        conn.editString(text, offX + x, offY + y, w, h, fontHeightPx, fontStyle,
                fgColor, bgColor, bgTransparency, multiline, constraint, align);
    }

    @Override
    public void peerSetFrame(long peer, int x, int y, int w, int h) {
        conn.peerSetFrame(peer, offX + x, offY + y, w, h);
    }

    /* ---- resource ops: shared handle space on the connection ----------------- */

    @Override
    public long createSystemFont(int face, int style, int size) {
        return conn.createSystemFont(face, style, size);
    }

    @Override
    public long createTruetypeFont(String name) {
        return conn.createTruetypeFont(name);
    }

    @Override
    public long deriveTruetypeFont(long peer, boolean bold, boolean italic, float size) {
        return conn.deriveTruetypeFont(peer, bold, italic, size);
    }

    @Override
    public int stringWidth(long peer, String str) {
        return conn.stringWidth(peer, str);
    }

    @Override
    public int charWidth(long peer, char ch) {
        return conn.charWidth(peer, ch);
    }

    @Override
    public int fontHeight(long peer) {
        return conn.fontHeight(peer);
    }

    @Override
    public int fontAscent(long peer) {
        return conn.fontAscent(peer);
    }

    @Override
    public int fontDescent(long peer) {
        return conn.fontDescent(peer);
    }

    @Override
    public long[] createImage(byte[] data) {
        return conn.createImage(data);
    }

    @Override
    public long createImageFromARGB(int[] argb, int w, int h) {
        return conn.createImageFromARGB(argb, w, h);
    }

    @Override
    public void imageRgbToIntArray(long peer, int[] arr, int x, int y, int w, int h,
            int imgW, int imgH) {
        conn.imageRgbToIntArray(peer, arr, x, y, w, h, imgW, imgH);
    }

    @Override
    public long scaleImage(long peer, int w, int h) {
        return conn.scaleImage(peer, w, h);
    }

    @Override
    public void releasePeer(long peer) {
        conn.releasePeer(peer);
    }

    @Override
    public void setNativeMenu(String encodedCommands) {
        conn.setNativeMenu(encodedCommands);
    }

    @Override
    public boolean saveScreenshot(String path, int x, int y, int w, int h) {
        return conn.saveScreenshot(path, offX + x, offY + y, w, h);
    }

    @Override
    public long createMutableImage(int w, int h, int argb) {
        return conn.createMutableImage(w, h, argb);
    }

    @Override
    public void mutableClip(long peer, int x, int y, int w, int h) {
        conn.mutableClip(peer, x, y, w, h);
    }

    @Override
    public void mutableFillRect(long peer, int color, int alpha, int x, int y, int w, int h) {
        conn.mutableFillRect(peer, color, alpha, x, y, w, h);
    }

    @Override
    public void mutableDrawLine(long peer, int color, int alpha, int x1, int y1, int x2, int y2) {
        conn.mutableDrawLine(peer, color, alpha, x1, y1, x2, y2);
    }

    @Override
    public void mutableDrawString(long peer, long fontPeer, int color, int alpha, String str,
            int x, int y) {
        conn.mutableDrawString(peer, fontPeer, color, alpha, str, x, y);
    }

    @Override
    public void mutableDrawImage(long peer, long imgPeer, int alpha, int x, int y, int w, int h) {
        conn.mutableDrawImage(peer, imgPeer, alpha, x, y, w, h);
    }

    @Override
    public void mutableShape(long peer, byte[] commands, int commandsLen, float[] points,
            int pointsLen, int color, int alpha, boolean stroke, float lineWidth,
            int capStyle, int joinStyle, float miterLimit) {
        conn.mutableShape(peer, commands, commandsLen, points, pointsLen, color, alpha,
                stroke, lineWidth, capStyle, joinStyle, miterLimit);
    }

    @Override
    public void finishMutable(long peer) {
        conn.finishMutable(peer);
    }

    @Override
    public long peerCreateWebView() {
        return conn.peerCreateWebView();
    }

    @Override
    public long peerCreateCamera() {
        return conn.peerCreateCamera();
    }

    @Override
    public void peerWebLoadURL(long peer, String url) {
        conn.peerWebLoadURL(peer, url);
    }

    @Override
    public void peerWebLoadHTML(long peer, String html, String baseUrl) {
        conn.peerWebLoadHTML(peer, html, baseUrl);
    }

    @Override
    public void peerRemove(long peer) {
        conn.peerRemove(peer);
    }

    @Override
    public void peerRelease(long peer) {
        conn.peerRelease(peer);
    }

    @Override
    public long mediaCreate(String url, boolean video) {
        return conn.mediaCreate(url, video);
    }

    @Override
    public void mediaControl(long peer, int op, int arg) {
        conn.mediaControl(peer, op, arg);
    }

    @Override
    public int mediaQuery(long peer, int what) {
        return conn.mediaQuery(peer, what);
    }

    @Override
    public void pickFile() {
        conn.pickFile();
    }

    @Override
    public void capturePhoto() {
        conn.capturePhoto();
    }
}
