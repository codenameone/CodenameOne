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

import com.codename1.impl.ios.sim.bridge.BridgeRegistry;
import com.codename1.impl.ios.sim.bridge.EditingCallback;
import com.codename1.impl.ios.sim.bridge.RenderBridge;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RenderBridge backed by the OUT-OF-PROCESS relay: a real Mac Catalyst build
 * of Codename One (full UIKit, real bundle identity) that executes our
 * drawing batches through its own native pipeline and hosts genuinely native
 * services (text editing first). The relay connects to us over a localhost
 * socket; this class is the protocol's JVM end.
 *
 * <p>This replaces the in-process JNI binding: nothing platform-specific is
 * reimplemented on the JVM side anymore - the relay's port IS the
 * implementation.</p>
 */
public class RpcRenderBridge implements RenderBridge {
    /* frame opcodes JVM -> relay (mirrors SimRelayService) */
    private static final int OP_BATCH = 1;
    private static final int OP_CREATE_SYSTEM_FONT = 2;
    private static final int OP_DERIVE_FONT = 3;
    private static final int OP_CREATE_TTF_FONT = 4;
    private static final int OP_STRING_WIDTH = 5;
    private static final int OP_CHAR_WIDTH = 6;
    private static final int OP_CREATE_IMAGE = 8;
    private static final int OP_CREATE_IMAGE_ARGB = 9;
    private static final int OP_SCALE_IMAGE = 10;
    private static final int OP_GET_RGB = 11;
    private static final int OP_RELEASE = 12;
    private static final int OP_EDIT_STRING = 13;
    private static final int OP_DISPLAY_INFO = 14;
    private static final int OP_PEER_CREATE = 16;
    private static final int OP_PEER_SET_FRAME = 17;
    private static final int OP_PEER_CMD = 18;
    private static final int OP_PEER_QUERY = 19;
    private static final int OP_MUTABLE_CREATE = 20;
    private static final int OP_MUTABLE_OPS = 21;
    private static final int OP_SET_TOUCH_CORR = 22;
    private static final int OP_CAPTURE_PHOTO = 23;
    private static final int OP_SET_NATIVE_MENU = 24;
    private static final int OP_SET_ALWAYS_ON_TOP = 25;
    private static final int OP_SET_WINDOW_SIZE = 26;

    private static final int PEER_WEBVIEW = 1;
    private static final int PEER_MEDIA = 2;
    private static final int PEER_CAMERA = 4;

    private static final int B_CLIP = 1;
    private static final int B_FILL_RECT = 2;
    private static final int B_DRAW_LINE = 3;
    private static final int B_DRAW_STRING = 4;
    private static final int B_DRAW_IMAGE = 5;
    private static final int B_TILE_IMAGE = 6;
    private static final int B_SHAPE = 7;

    private static final int K_REPLY = 0;
    private static final int K_EVENT = 1;

    private static final int E_POINTER_PRESSED = 1;
    private static final int E_POINTER_RELEASED = 2;
    private static final int E_POINTER_DRAGGED = 3;
    private static final int E_KEY_PRESSED = 4;
    private static final int E_KEY_RELEASED = 5;
    private static final int E_SIZE_CHANGED = 6;
    private static final int E_EDIT_UPDATE = 7;
    private static final int E_EDIT_DONE = 8;
    private static final int E_LOG = 9;
    private static final int E_CAPTURE_DONE = 10;
    private static final int E_MENU_SELECTED = 11;

    /** receives the relay's input events and lifecycle notifications */
    public interface RelayEvents {
        void pointerEvent(int type, int x, int y);

        void keyEvent(int type, int code);

        void sizeChanged(int w, int h);
    }

    private final DataOutputStream out;
    private final DataInputStream in;
    private final Object outLock = new Object();
    private final Map<Integer, int[]> replies = new HashMap<Integer, int[]>();
    private int nextReq = 1;
    private volatile RelayEvents events;
    private int displayW = 320;
    private int displayH = 480;

    /** the font metrics piggybacked on font creation replies */
    private final Map<Long, int[]> fontMetrics = new HashMap<Long, int[]>();

    /** batched draw ops, flushed as one frame */
    private final List<byte[]> batch = new ArrayList<byte[]>();

    /**
     * Listens on the port and blocks until the relay app connects, then
     * queries the display metrics.
     *
     * @param port the rendezvous port (the relay dials 127.0.0.1:port)
     */
    public RpcRenderBridge(int port) throws IOException {
        ServerSocket server = new ServerSocket(port);
        System.out.println("[cn1sim-rpc] waiting for the relay app on port " + port + "...");
        java.net.Socket s = server.accept();
        s.setTcpNoDelay(true);
        System.out.println("[cn1sim-rpc] relay connected from " + s.getRemoteSocketAddress());
        out = new DataOutputStream(new java.io.BufferedOutputStream(s.getOutputStream(), 1 << 16));
        in = new DataInputStream(new java.io.BufferedInputStream(s.getInputStream(), 1 << 16));
        Thread reader = new Thread(new Runnable() {
            public void run() {
                try {
                    readLoop();
                } catch (IOException ex) {
                    System.err.println("[cn1sim-rpc] relay connection lost: " + ex);
                    System.exit(1);
                }
            }
        }, "CN1Sim-RpcReader");
        reader.setDaemon(true);
        reader.start();
        int[] info = call(OP_DISPLAY_INFO, new int[0], 3);
        displayW = info[0];
        displayH = info[1];
        System.out.println("[cn1sim-rpc] relay surface " + displayW + "x" + displayH);
        // Draw-vs-touch offset. With the impl-native draw path (draws go through
        // the same GLViewController pipeline as touches), draw and touch share
        // one coordinate system and the relay's draw-side canvas offset already
        // aligns them, so no correction is needed -> default 0. (The old 44 was
        // compensating for the now-removed mutable-image buffer detour.)
        // Override with -Dcn1.touchcorr if a skin/density ever shifts it.
        int tc = Integer.getInteger("cn1.touchcorr", 0).intValue();
        try {
            synchronized (outLock) {
                out.writeInt(OP_SET_TOUCH_CORR);
                out.writeInt(tc);
                out.flush();
            }
            System.out.println("[cn1sim-rpc] sent touchCorr=" + tc);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setRelayEvents(RelayEvents e) {
        events = e;
    }

    private void readLoop() throws IOException {
        while (true) {
            int kind = in.readInt();
            if (kind == K_REPLY) {
                // self-describing: [reqId][count][ints]
                int reqId = in.readInt();
                int len = in.readInt();
                int[] vals = new int[len];
                for (int i = 0; i < len; i++) {
                    vals[i] = in.readInt();
                }
                if (System.getProperty("cn1.sim.rpc.debug") != null) {
                    System.out.println("[cn1sim-rpc] reply reqId=" + reqId + " len=" + len);
                }
                synchronized (replies) {
                    replies.put(reqId, vals);
                    replies.notifyAll();
                }
            } else if (kind == K_EVENT) {
                int code = in.readInt();
                int a = in.readInt();
                int b = in.readInt();
                if (System.getProperty("cn1.sim.rpc.debug") != null) {
                    System.out.println("[cn1sim-rpc] event " + code + " " + a + "," + b);
                }
                RelayEvents e = events;
                try {
                switch (code) {
                    case E_POINTER_PRESSED:
                    case E_POINTER_RELEASED:
                    case E_POINTER_DRAGGED:
                        if (e != null) {
                            e.pointerEvent(code, a, b);
                        }
                        break;
                    case E_KEY_PRESSED:
                        if (e != null) {
                            e.keyEvent(1, a);
                        }
                        break;
                    case E_KEY_RELEASED:
                        if (e != null) {
                            e.keyEvent(2, a);
                        }
                        break;
                    case E_SIZE_CHANGED:
                        displayW = a;
                        displayH = b;
                        if (e != null) {
                            e.sizeChanged(a, b);
                        }
                        break;
                    case E_EDIT_UPDATE: {
                        String text = in.readUTF();
                        EditingCallback cb = BridgeRegistry.getEditingCallback();
                        if (cb != null) {
                            cb.editingUpdate(text);
                        }
                        break;
                    }
                    case E_EDIT_DONE: {
                        String text = in.readUTF();
                        EditingCallback cb = BridgeRegistry.takeEditingCallback();
                        if (cb != null) {
                            cb.editingDone(text);
                        }
                        break;
                    }
                    case E_CAPTURE_DONE: {
                        // path to the captured image on the shared filesystem,
                        // or "" when the user cancelled / denied the camera
                        String path = in.readUTF();
                        com.codename1.impl.ios.sim.bridge.PickCallback pc =
                                BridgeRegistry.takePickCallback();
                        if (pc != null) {
                            pc.picked(path == null || path.isEmpty() ? null : path);
                        }
                        break;
                    }
                    case E_MENU_SELECTED: {
                        // the selected row index travels in the event header's
                        // first int; route it to the shell that pushed the menu
                        com.codename1.impl.ios.sim.bridge.MenuDispatcher md =
                                BridgeRegistry.getMenuDispatcher();
                        if (md != null) {
                            md.fireMenuCommand(a);
                        }
                        break;
                    }
                    case E_LOG:
                        System.out.println("[relay] " + in.readUTF());
                        break;
                    default:
                        break;
                }
                } catch (RuntimeException ex) {
                    // event handlers must never kill the reader; the frame was
                    // fully consumed so the stream stays consistent
                    System.err.println("[cn1sim-rpc] event dispatch failed: " + ex);
                }
            } else {
                throw new IOException("Bad frame kind " + kind);
            }
        }
    }

    /**
     * Synchronous request: writes [opcode][reqId][payload writer runs] and
     * blocks for the reply.
     *
     * @param expectedInts reply length, or -1 for length-prefixed
     */
    private int[] call(int opcode, int[] intArgs, int expectedIntsIgnored) {
        int reqId = newRequest();
        if (System.getProperty("cn1.sim.rpc.debug") != null) {
            System.out.println("[cn1sim-rpc] send op=" + opcode + " reqId=" + reqId
                    + " thread=" + Thread.currentThread().getName());
        }
        try {
            synchronized (outLock) {
                out.writeInt(opcode);
                out.writeInt(reqId);
                for (int a : intArgs) {
                    out.writeInt(a);
                }
                out.flush();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return await(reqId);
    }

    private int newRequest() {
        synchronized (replies) {
            return nextReq++;
        }
    }

    private int[] await(int reqId) {
        synchronized (replies) {
            while (true) {
                int[] v = replies.remove(reqId);
                if (v != null) {
                    return v;
                }
                try {
                    replies.wait();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    /* ---- display ------------------------------------------------------------ */

    @Override
    public int getAppWidth() {
        return displayW;
    }

    @Override
    public int getAppHeight() {
        return displayH;
    }

    private void addOp(byte[] encoded) {
        synchronized (batch) {
            batch.add(encoded);
        }
    }

    static byte[] enc(int... vals) {
        byte[] b = new byte[vals.length * 4];
        for (int i = 0; i < vals.length; i++) {
            int v = vals[i];
            b[i * 4] = (byte) (v >>> 24);
            b[i * 4 + 1] = (byte) (v >>> 16);
            b[i * 4 + 2] = (byte) (v >>> 8);
            b[i * 4 + 3] = (byte) v;
        }
        return b;
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
        sendBatch(ops);
    }

    /** writes a complete frame of ops; region bridges share this connection */
    void sendBatch(List<byte[]> ops) {
        if (ops.isEmpty()) {
            return;
        }
        try {
            synchronized (outLock) {
                out.writeInt(OP_BATCH);
                out.writeInt(ops.size());
                for (byte[] op : ops) {
                    out.write(op);
                }
                out.flush();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void setClip(int x, int y, int w, int h) {
        addOp(enc(B_CLIP, x, y, w, h));
    }

    @Override
    public void fillRect(int color, int alpha, int x, int y, int w, int h) {
        addOp(enc(B_FILL_RECT, color, alpha, x, y, w, h));
    }

    @Override
    public void drawLine(int color, int alpha, int x1, int y1, int x2, int y2) {
        addOp(enc(B_DRAW_LINE, color, alpha, x1, y1, x2, y2));
    }

    @Override
    public void drawString(int color, int alpha, long fontPeer, String str, int x, int y) {
        addOp(encDrawString(color, alpha, fontPeer, str, x, y));
    }

    static byte[] encDrawString(int color, int alpha, long fontPeer, String str, int x, int y) {
        try {
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(bo);
            d.writeInt(B_DRAW_STRING);
            d.writeInt(color);
            d.writeInt(alpha);
            d.writeInt((int) fontPeer);
            d.writeInt(x);
            d.writeInt(y);
            d.writeUTF(str);
            return bo.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void shape(byte[] commands, int commandsLen, float[] points, int pointsLen,
            int color, int alpha, boolean stroke, float lineWidth, int capStyle,
            int joinStyle, float miterLimit) {
        addOp(encShape(commands, commandsLen, points, pointsLen, color, alpha, stroke,
                lineWidth, capStyle, joinStyle, miterLimit, 0, 0));
    }

    /** offX/offY translate path points into window space (region bridges) */
    static byte[] encShape(byte[] commands, int commandsLen, float[] points, int pointsLen,
            int color, int alpha, boolean stroke, float lineWidth, int capStyle,
            int joinStyle, float miterLimit, int offX, int offY) {
        try {
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(bo);
            d.writeInt(B_SHAPE);
            d.writeInt(color);
            d.writeInt(alpha);
            d.writeInt(stroke ? 1 : 0);
            d.writeInt(capStyle);
            d.writeInt(joinStyle);
            d.writeFloat(lineWidth);
            d.writeFloat(miterLimit);
            d.writeInt(commandsLen);
            d.write(commands, 0, commandsLen);
            d.writeInt(pointsLen);
            for (int i = 0; i < pointsLen; i++) {
                // points alternate x,y
                d.writeFloat(points[i] + ((i & 1) == 0 ? offX : offY));
            }
            return bo.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /* ---- fonts --------------------------------------------------------------- */

    @Override
    public long createSystemFont(int face, int style, int size) {
        int[] r = call(OP_CREATE_SYSTEM_FONT, new int[]{face, style, size}, 4);
        fontMetrics.put((long) r[0], new int[]{r[1], r[2], r[3]});
        return r[0];
    }

    @Override
    public long createTruetypeFont(String name) {
        try {
            int reqId = newRequest();
            synchronized (outLock) {
                out.writeInt(OP_CREATE_TTF_FONT);
                out.writeInt(reqId);
                out.writeUTF(name);
                out.flush();
            }
            int[] r = await(reqId);
            fontMetrics.put((long) r[0], new int[]{r[1], r[2], r[3]});
            return r[0];
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public long deriveTruetypeFont(long peer, boolean bold, boolean italic, float size) {
        try {
            int reqId = newRequest();
            if (System.getProperty("cn1.sim.rpc.debug") != null) {
                System.out.println("[cn1sim-rpc] send op=DERIVE_FONT reqId=" + reqId
                        + " thread=" + Thread.currentThread().getName());
            }
            synchronized (outLock) {
                out.writeInt(OP_DERIVE_FONT);
                out.writeInt(reqId);
                out.writeInt((int) peer);
                out.writeBoolean(bold);
                out.writeBoolean(italic);
                out.writeFloat(size);
                out.flush();
            }
            int[] r = await(reqId);
            fontMetrics.put((long) r[0], new int[]{r[1], r[2], r[3]});
            return r[0];
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int stringWidth(long peer, String str) {
        try {
            int reqId = newRequest();
            synchronized (outLock) {
                out.writeInt(OP_STRING_WIDTH);
                out.writeInt(reqId);
                out.writeInt((int) peer);
                out.writeUTF(str != null ? str : "");
                out.flush();
            }
            return await(reqId)[0];
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int charWidth(long peer, char ch) {
        try {
            int reqId = newRequest();
            synchronized (outLock) {
                out.writeInt(OP_CHAR_WIDTH);
                out.writeInt(reqId);
                out.writeInt((int) peer);
                out.writeChar(ch);
                out.flush();
            }
            return await(reqId)[0];
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int fontHeight(long peer) {
        int[] m = fontMetrics.get(peer);
        return m != null ? m[0] : 16;
    }

    @Override
    public int fontAscent(long peer) {
        int[] m = fontMetrics.get(peer);
        return m != null ? m[1] : 12;
    }

    @Override
    public int fontDescent(long peer) {
        int[] m = fontMetrics.get(peer);
        return m != null ? m[2] : 4;
    }

    /* ---- images --------------------------------------------------------------- */

    @Override
    public long[] createImage(byte[] data) {
        try {
            int reqId = newRequest();
            synchronized (outLock) {
                out.writeInt(OP_CREATE_IMAGE);
                out.writeInt(reqId);
                out.writeInt(data.length);
                out.write(data);
                out.flush();
            }
            int[] r = await(reqId);
            if (r[0] == 0) {
                return null;
            }
            return new long[]{r[0], r[1], r[2]};
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public long createImageFromARGB(int[] argb, int w, int h) {
        int[] args = new int[2 + argb.length];
        args[0] = w;
        args[1] = h;
        System.arraycopy(argb, 0, args, 2, argb.length);
        return call(OP_CREATE_IMAGE_ARGB, args, 1)[0];
    }

    @Override
    public void imageRgbToIntArray(long peer, int[] arr, int x, int y, int w, int h,
            int imgW, int imgH) {
        int[] r = call(OP_GET_RGB, new int[]{(int) peer}, -1);
        // the relay returns the FULL image; crop the requested region
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                int sx = x + col;
                int sy = y + row;
                int idx = sy * imgW + sx;
                arr[row * w + col] = idx >= 0 && idx < r.length ? r[idx] : 0;
            }
        }
    }

    @Override
    public long scaleImage(long peer, int w, int h) {
        return call(OP_SCALE_IMAGE, new int[]{(int) peer, w, h}, 1)[0];
    }

    @Override
    public void drawImage(long peer, int alpha, int x, int y, int w, int h) {
        addOp(enc(B_DRAW_IMAGE, (int) peer, alpha, x, y, w, h));
    }

    @Override
    public void tileImage(long peer, int alpha, int x, int y, int w, int h) {
        addOp(enc(B_TILE_IMAGE, (int) peer, alpha, x, y, w, h));
    }

    @Override
    public void releasePeer(long peer) {
        try {
            synchronized (outLock) {
                out.writeInt(OP_RELEASE);
                out.writeInt((int) peer);
                out.flush();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /* ---- editing -------------------------------------------------------------- */

    @Override
    public void editString(String text, int x, int y, int w, int h,
            int fontHeightPx, int fontStyle, int fgColor, int bgColor, int bgTransparency,
            boolean multiline, int constraint, int align) {
        try {
            synchronized (outLock) {
                out.writeInt(OP_EDIT_STRING);
                out.writeUTF(text != null ? text : "");
                out.writeInt(x);
                out.writeInt(y);
                out.writeInt(w);
                out.writeInt(h);
                out.writeInt(fontHeightPx);
                out.writeInt(fontStyle);
                out.writeInt(fgColor);
                out.writeInt(bgColor);
                out.writeInt(bgTransparency);
                out.writeBoolean(multiline);
                out.writeInt(constraint);
                out.writeInt(align);
                out.flush();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /* ---- native menu bar ------------------------------------------------------ */

    @Override
    public void setNativeMenu(String encodedCommands) {
        try {
            synchronized (outLock) {
                out.writeInt(OP_SET_NATIVE_MENU);
                out.writeUTF(encodedCommands != null ? encodedCommands : "");
                out.flush();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /** Floats the relay window above all others (Always On Top) or restores its normal level. */
    public void setAlwaysOnTop(boolean onTop) {
        try {
            synchronized (outLock) {
                out.writeInt(OP_SET_ALWAYS_ON_TOP);
                out.writeInt(onTop ? 1 : 0);
                out.flush();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /** Resizes the relay window's content area to the given size in points (fit-to-skin). */
    public void setWindowSize(int width, int height) {
        try {
            synchronized (outLock) {
                out.writeInt(OP_SET_WINDOW_SIZE);
                out.writeInt(width);
                out.writeInt(height);
                out.flush();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean saveScreenshot(String path, int x, int y, int w, int h) {
        return false;
    }

    /** pending draw ops per mutable image, published by finishMutable */
    private final Map<Long, List<byte[]>> mutableOps = new HashMap<Long, List<byte[]>>();

    private void addMutableOp(long peer, byte[] encoded) {
        synchronized (mutableOps) {
            List<byte[]> l = mutableOps.get(peer);
            if (l == null) {
                l = new ArrayList<byte[]>();
                mutableOps.put(peer, l);
            }
            l.add(encoded);
        }
    }

    @Override
    public long createMutableImage(int w, int h, int argb) {
        return call(OP_MUTABLE_CREATE, new int[]{w, h, argb}, 1)[0];
    }

    @Override
    public void mutableClip(long peer, int x, int y, int w, int h) {
        addMutableOp(peer, enc(B_CLIP, x, y, w, h));
    }

    @Override
    public void mutableFillRect(long peer, int color, int alpha, int x, int y, int w, int h) {
        addMutableOp(peer, enc(B_FILL_RECT, color, alpha, x, y, w, h));
    }

    @Override
    public void mutableDrawLine(long peer, int color, int alpha, int x1, int y1, int x2, int y2) {
        addMutableOp(peer, enc(B_DRAW_LINE, color, alpha, x1, y1, x2, y2));
    }

    @Override
    public void mutableDrawString(long peer, long fontPeer, int color, int alpha, String str,
            int x, int y) {
        addMutableOp(peer, encDrawString(color, alpha, fontPeer, str, x, y));
    }

    @Override
    public void mutableDrawImage(long peer, long imgPeer, int alpha, int x, int y, int w, int h) {
        addMutableOp(peer, enc(B_DRAW_IMAGE, (int) imgPeer, alpha, x, y, w, h));
    }

    @Override
    public void mutableShape(long peer, byte[] commands, int commandsLen, float[] points,
            int pointsLen, int color, int alpha, boolean stroke, float lineWidth,
            int capStyle, int joinStyle, float miterLimit) {
        addMutableOp(peer, encShape(commands, commandsLen, points, pointsLen, color, alpha,
                stroke, lineWidth, capStyle, joinStyle, miterLimit, 0, 0));
    }

    @Override
    public void finishMutable(long peer) {
        List<byte[]> ops;
        synchronized (mutableOps) {
            ops = mutableOps.remove(peer);
        }
        if (ops == null || ops.isEmpty()) {
            return;
        }
        try {
            synchronized (outLock) {
                out.writeInt(OP_MUTABLE_OPS);
                out.writeInt((int) peer);
                out.writeInt(ops.size());
                for (byte[] op : ops) {
                    out.write(op);
                }
                out.flush();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /** creates a peer in the relay; blocks for the handle */
    private long peerCreate(int kind, int flag, String arg) {
        int reqId = newRequest();
        try {
            synchronized (outLock) {
                out.writeInt(OP_PEER_CREATE);
                out.writeInt(reqId);
                out.writeInt(kind);
                out.writeInt(flag);
                out.writeUTF(arg == null ? "" : arg);
                out.flush();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        int id = await(reqId)[0];
        if (System.getProperty("cn1.sim.rpc.debug") != null) {
            System.out.println("[cn1sim-rpc] peerCreate kind=" + kind + " -> " + id);
        }
        return id;
    }

    /** fire-and-forget control frame for an existing peer */
    private void peerCmd(long peer, int cmd, int arg, String sArg) {
        try {
            synchronized (outLock) {
                out.writeInt(OP_PEER_CMD);
                out.writeInt((int) peer);
                out.writeInt(cmd);
                out.writeInt(arg);
                out.writeUTF(sArg == null ? "" : sArg);
                out.flush();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public long peerCreateWebView() {
        return peerCreate(PEER_WEBVIEW, 0, "");
    }

    @Override
    public long peerCreateCamera() {
        return peerCreate(PEER_CAMERA, 0, "");
    }

    @Override
    public void peerWebLoadURL(long peer, String url) {
        peerCmd(peer, 1, 0, url);
    }

    @Override
    public void peerWebLoadHTML(long peer, String html, String baseUrl) {
        peerCmd(peer, 2, 0, html);
    }

    @Override
    public void peerSetFrame(long peer, int x, int y, int w, int h) {
        try {
            synchronized (outLock) {
                out.writeInt(OP_PEER_SET_FRAME);
                out.writeInt((int) peer);
                out.writeInt(x);
                out.writeInt(y);
                out.writeInt(w);
                out.writeInt(h);
                out.flush();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void peerRemove(long peer) {
        peerCmd(peer, 20, 0, "");
    }

    @Override
    public void peerRelease(long peer) {
        peerCmd(peer, 21, 0, "");
    }

    @Override
    public long mediaCreate(String url, boolean video) {
        return peerCreate(PEER_MEDIA, video ? 1 : 0, url);
    }

    @Override
    public void mediaControl(long peer, int op, int arg) {
        // op: 0=play 1=pause 2=seek -> relay cmds 10/11/12
        peerCmd(peer, 10 + op, arg, "");
    }

    @Override
    public int mediaQuery(long peer, int what) {
        int reqId = newRequest();
        try {
            synchronized (outLock) {
                out.writeInt(OP_PEER_QUERY);
                out.writeInt(reqId);
                out.writeInt((int) peer);
                out.writeInt(what);
                out.flush();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return await(reqId)[0];
    }

    @Override
    public void pickFile() {
    }

    @Override
    public void capturePhoto() {
        // fire-and-forget: the relay opens the native camera and replies later
        // with an E_CAPTURE_DONE event carrying the saved image path.
        try {
            synchronized (outLock) {
                out.writeInt(OP_CAPTURE_PHOTO);
                out.flush();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
