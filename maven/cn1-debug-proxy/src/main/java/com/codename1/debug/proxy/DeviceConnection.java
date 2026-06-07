/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.debug.proxy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the TCP connection from a Codename One iOS app's on-device-debug
 * runtime ({@link cn1_debugger.m}). The app dials out to the developer's
 * laptop, so this side listens for a single accept then services events
 * and commands.
 *
 * Events arrive from the device; this class decodes the wire frame and
 * dispatches to a {@link DeviceListener}. The listener (typically the
 * JDWP server) calls back into {@link #sendCommand} to manipulate
 * breakpoints, resume threads, request stack/locals, etc.
 *
 * Commands and events use the same length-prefixed framing — see
 * {@link WireProtocol} for codes and payload layouts.
 */
public final class DeviceConnection implements AutoCloseable {

    public interface DeviceListener {
        void onHello(int version);
        void onBreakpointHit(long threadId, int methodId, int line);
        void onStepComplete(long threadId, int methodId, int line);
        void onStack(long threadId, int[] methodIds, int[] lines);
        void onLocals(int[] slots, byte[] typeCodes, long[] values);
        void onVmDeath();
        void onStringValue(String value);
        void onObjectClass(int classId, boolean isArray);
        void onObjectFields(byte[] typeCodes, long[] values);
        void onInvokeResult(byte type, long value);
        void onArrayLength(int length);
        /**
         * Raw array values from the device. {@code tag} is the JVM type-char
         * shared by every element, {@code rawBytes} is the wire payload with
         * each element packed in big-endian.
         */
        void onArrayValues(byte tag, int count, byte[] rawBytes);
        void onReplyStatus();
        void onStdoutLine(String line);
        void onStderrLine(String line);
        void onUnknownEvent(int code, byte[] payload);
        void onDisconnected();
    }

    private final int listenPort;
    private final DeviceListener listener;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile ServerSocket server;
    private volatile Socket socket;
    private volatile DataInputStream in;
    private volatile DataOutputStream out;
    private final Object sendLock = new Object();

    public DeviceConnection(int listenPort, DeviceListener listener) {
        this.listenPort = listenPort;
        this.listener = listener;
    }

    public void acceptAndServe() throws IOException {
        server = new ServerSocket(listenPort);
        System.out.println("[device] listening on port " + listenPort + " for ParparVM app to dial in");
        socket = server.accept();
        socket.setTcpNoDelay(true);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        System.out.println("[device] connected from " + socket.getRemoteSocketAddress());
        // We've taken the one connection we need; close the listener so
        // a stray reconnect attempt fails fast instead of stacking up.
        try { server.close(); } catch (IOException ignore) {}

        try {
            readLoop();
        } finally {
            close();
            listener.onDisconnected();
        }
    }

    private void readLoop() throws IOException {
        while (!closed.get()) {
            int payloadLen = in.readInt(); // big-endian by DataInput contract
            if (payloadLen < 0 || payloadLen > (1 << 20)) {
                throw new IOException("Unreasonable payload length " + payloadLen);
            }
            int code = in.readUnsignedByte();
            byte[] payload = new byte[payloadLen];
            in.readFully(payload);
            dispatch(code, payload);
        }
    }

    private void dispatch(int code, byte[] p) {
        switch (code) {
            case WireProtocol.EVT_HELLO: {
                int ver = p.length >= 3 ? p[2] & 0xff : 0;
                listener.onHello(ver);
                return;
            }
            case WireProtocol.EVT_BP_HIT: {
                if (p.length < 16) { listener.onUnknownEvent(code, p); return; }
                long tid = readLong(p, 0);
                int mid = readInt(p, 8);
                int line = readInt(p, 12);
                listener.onBreakpointHit(tid, mid, line);
                return;
            }
            case WireProtocol.EVT_STEP_COMPLETE: {
                if (p.length < 16) { listener.onUnknownEvent(code, p); return; }
                long tid = readLong(p, 0);
                int mid = readInt(p, 8);
                int line = readInt(p, 12);
                listener.onStepComplete(tid, mid, line);
                return;
            }
            case WireProtocol.EVT_STACK: {
                if (p.length < 12) { listener.onUnknownEvent(code, p); return; }
                long tid = readLong(p, 0);
                int n = readInt(p, 8);
                int[] mids = new int[n];
                int[] lines = new int[n];
                for (int i = 0; i < n; i++) {
                    mids[i] = readInt(p, 12 + i * 8);
                    lines[i] = readInt(p, 12 + i * 8 + 4);
                }
                listener.onStack(tid, mids, lines);
                return;
            }
            case WireProtocol.EVT_LOCALS: {
                if (p.length < 4) { listener.onUnknownEvent(code, p); return; }
                int n = readInt(p, 0);
                int[] slots = new int[n];
                byte[] types = new byte[n];
                long[] values = new long[n];
                int off = 4;
                for (int i = 0; i < n; i++) {
                    slots[i] = readInt(p, off); off += 4;
                    types[i] = p[off]; off += 1;
                    values[i] = readLong(p, off); off += 8;
                }
                listener.onLocals(slots, types, values);
                return;
            }
            case WireProtocol.EVT_VM_DEATH:
                listener.onVmDeath();
                return;
            case WireProtocol.EVT_STRING_VALUE: {
                String s = new String(p, java.nio.charset.StandardCharsets.UTF_8);
                listener.onStringValue(s);
                return;
            }
            case WireProtocol.EVT_OBJECT_CLASS: {
                int cid = p.length >= 4 ? readInt(p, 0) : -1;
                // 5th byte is isArray flag; older devices omit it.
                boolean isArr = p.length >= 5 && p[4] != 0;
                listener.onObjectClass(cid, isArr);
                return;
            }
            case WireProtocol.EVT_OBJECT_FIELDS: {
                if (p.length < 4) { listener.onUnknownEvent(code, p); return; }
                int n = readInt(p, 0);
                byte[] types = new byte[n];
                long[] values = new long[n];
                int off = 4;
                for (int i = 0; i < n; i++) {
                    types[i] = p[off]; off += 1;
                    values[i] = readLong(p, off); off += 8;
                }
                listener.onObjectFields(types, values);
                return;
            }
            case WireProtocol.EVT_ARRAY_LENGTH: {
                int len = p.length >= 4 ? readInt(p, 0) : 0;
                listener.onArrayLength(len);
                return;
            }
            case WireProtocol.EVT_ARRAY_VALUES: {
                if (p.length < 5) { listener.onUnknownEvent(code, p); return; }
                byte tag = p[0];
                int n = readInt(p, 1);
                byte[] raw = new byte[p.length - 5];
                System.arraycopy(p, 5, raw, 0, raw.length);
                listener.onArrayValues(tag, n, raw);
                return;
            }
            case WireProtocol.EVT_INVOKE_RESULT: {
                // Payload: type(1) + value(8). Value packing depends on type:
                //   I/F/Z/B/S/C use the int slot, J/D the long slot,
                //   L/[ /X carry an object reference, V leaves value=0.
                if (p.length < 9) { listener.onUnknownEvent(code, p); return; }
                listener.onInvokeResult(p[0], readLong(p, 1));
                return;
            }
            case WireProtocol.EVT_STDOUT_LINE:
            case WireProtocol.EVT_STDERR_LINE: {
                // Payload: 4-byte big-endian length, then UTF-8 line bytes.
                if (p.length < 4) { listener.onUnknownEvent(code, p); return; }
                int len = readInt(p, 0);
                if (len < 0 || 4 + len > p.length) { listener.onUnknownEvent(code, p); return; }
                String s = new String(p, 4, len, java.nio.charset.StandardCharsets.UTF_8);
                if (code == WireProtocol.EVT_STDOUT_LINE) {
                    listener.onStdoutLine(s);
                } else {
                    listener.onStderrLine(s);
                }
                return;
            }
            case WireProtocol.EVT_REPLY_STATUS:
                listener.onReplyStatus();
                return;
            default:
                listener.onUnknownEvent(code, p);
        }
    }

    public void sendCommand(int cmd, byte[] payload) throws IOException {
        synchronized (sendLock) {
            if (out == null) throw new IOException("Device not connected");
            int len = payload == null ? 0 : payload.length;
            out.writeInt(len);
            out.writeByte(cmd);
            if (len > 0) out.write(payload);
            out.flush();
        }
    }

    public void setBreakpoint(int methodId, int line) throws IOException {
        byte[] p = new byte[8];
        writeInt(p, 0, methodId);
        writeInt(p, 4, line);
        sendCommand(WireProtocol.CMD_SET_BREAKPOINT, p);
    }

    public void clearBreakpoint(int methodId, int line) throws IOException {
        byte[] p = new byte[8];
        writeInt(p, 0, methodId);
        writeInt(p, 4, line);
        sendCommand(WireProtocol.CMD_CLEAR_BREAKPOINT, p);
    }

    public void resume(long threadId) throws IOException {
        byte[] p = new byte[8];
        writeLong(p, 0, threadId);
        sendCommand(WireProtocol.CMD_RESUME, p);
    }

    public void resumeAll() throws IOException {
        sendCommand(WireProtocol.CMD_RESUME, new byte[8]);
    }

    public void step(long threadId, int stepKind) throws IOException {
        byte[] p = new byte[9];
        writeLong(p, 0, threadId);
        p[8] = (byte) stepKind;
        sendCommand(WireProtocol.CMD_STEP, p);
    }

    public void getStack(long threadId) throws IOException {
        byte[] p = new byte[8];
        writeLong(p, 0, threadId);
        sendCommand(WireProtocol.CMD_GET_STACK, p);
    }

    public void getLocals(long threadId, int frameOffsetFromTop) throws IOException {
        byte[] p = new byte[12];
        writeLong(p, 0, threadId);
        writeInt(p, 8, frameOffsetFromTop);
        sendCommand(WireProtocol.CMD_GET_LOCALS, p);
    }

    public void dispose() throws IOException {
        sendCommand(WireProtocol.CMD_DISPOSE, null);
    }

    public void getObjectClass(long objPtr) throws IOException {
        byte[] p = new byte[8];
        writeLong(p, 0, objPtr);
        sendCommand(WireProtocol.CMD_GET_OBJECT_CLASS, p);
    }

    public void getString(long objPtr) throws IOException {
        byte[] p = new byte[8];
        writeLong(p, 0, objPtr);
        sendCommand(WireProtocol.CMD_GET_STRING, p);
    }

    /**
     * Dispatches a debugger-driven method invocation to the device. The
     * suspended thread (identified by {@code threadId}) runs the call;
     * {@code thisObj} is ignored for static methods (use 0). Each arg
     * carries a JVM type-char and an 8-byte value — the device's thunk
     * unpacks it into the typed C parameter the underlying function
     * expects.
     */
    public void getArrayLength(long arrayPtr) throws IOException {
        byte[] p = new byte[8];
        writeLong(p, 0, arrayPtr);
        sendCommand(WireProtocol.CMD_GET_ARRAY_LENGTH, p);
    }

    public void getArrayValues(long arrayPtr, int firstIndex, int count) throws IOException {
        byte[] p = new byte[16];
        writeLong(p, 0, arrayPtr);
        writeInt(p, 8, firstIndex);
        writeInt(p, 12, count);
        sendCommand(WireProtocol.CMD_GET_ARRAY_VALUES, p);
    }

    public void invokeMethod(long threadId, int methodId, long thisObj,
                             byte[] argTypes, long[] argValues) throws IOException {
        int n = argTypes.length;
        byte[] p = new byte[8 + 4 + 8 + 4 + n * 9];
        writeLong(p, 0, threadId);
        writeInt(p, 8, methodId);
        writeLong(p, 12, thisObj);
        writeInt(p, 20, n);
        for (int i = 0; i < n; i++) {
            p[24 + i * 9] = argTypes[i];
            writeLong(p, 24 + i * 9 + 1, argValues[i]);
        }
        sendCommand(WireProtocol.CMD_INVOKE_METHOD, p);
    }

    public void getObjectFields(long objPtr, int[] fieldIds) throws IOException {
        byte[] p = new byte[8 + 4 + fieldIds.length * 4];
        writeLong(p, 0, objPtr);
        writeInt(p, 8, fieldIds.length);
        for (int i = 0; i < fieldIds.length; i++) {
            writeInt(p, 12 + i * 4, fieldIds[i]);
        }
        sendCommand(WireProtocol.CMD_GET_OBJECT_FIELDS, p);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try { if (socket != null) socket.close(); } catch (IOException ignore) {}
            try { if (server != null) server.close(); } catch (IOException ignore) {}
        }
    }

    private static int readInt(byte[] b, int off) {
        return ((b[off] & 0xff) << 24) | ((b[off + 1] & 0xff) << 16)
             | ((b[off + 2] & 0xff) << 8) | (b[off + 3] & 0xff);
    }

    private static long readLong(byte[] b, int off) {
        return ((long) readInt(b, off) << 32) | (readInt(b, off + 4) & 0xffffffffL);
    }

    private static void writeInt(byte[] b, int off, int v) {
        b[off]   = (byte)(v >>> 24);
        b[off+1] = (byte)(v >>> 16);
        b[off+2] = (byte)(v >>> 8);
        b[off+3] = (byte)v;
    }

    private static void writeLong(byte[] b, int off, long v) {
        writeInt(b, off, (int)(v >>> 32));
        writeInt(b, off + 4, (int) v);
    }
}
