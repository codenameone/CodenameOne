/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.io.grpc;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/// Low-level protobuf wire-format reader used by generated
/// [ProtoCodec] implementations. Wraps a `byte[]` so the reader
/// can [#remaining()] and slice nested-message bodies without
/// re-buffering.
///
/// Generated codecs call [#readTag()] in a loop, dispatch on the
/// field number, and call the appropriate `readXxx` method. Unknown
/// fields are passed to [#skipField(int)] -- the same recovery
/// behaviour `protoc`-generated code exhibits.
public final class ProtoReader {

    private final byte[] buf;
    private final int limit;
    private int pos;

    public ProtoReader(byte[] data) {
        this(data, 0, data == null ? 0 : data.length);
    }

    public ProtoReader(byte[] data, int offset, int length) {
        this.buf = data;
        this.pos = offset;
        this.limit = offset + length;
    }

    public boolean isAtEnd() {
        return pos >= limit;
    }

    public int remaining() {
        return limit - pos;
    }

    /// Reads the next tag (`(fieldNumber << 3) | wireType`) or
    /// returns 0 when the stream is at EOF. Generated codecs use
    /// the 0 sentinel as the loop exit condition.
    public int readTag() throws IOException {
        if (isAtEnd()) { return 0; }
        return readVarint32();
    }

    /// Skips a single field whose tag has already been consumed.
    /// Wire type extracted from `tag` (`tag & 0x7`).
    public void skipField(int tag) throws IOException {
        int wire = tag & 0x7;
        switch (wire) {
            case ProtoWriter.WIRE_VARINT:
                readVarint64();
                return;
            case ProtoWriter.WIRE_I64:
                advance(8);
                return;
            case ProtoWriter.WIRE_LEN: {
                int len = readVarint32();
                advance(len);
                return;
            }
            case ProtoWriter.WIRE_I32:
                advance(4);
                return;
            default:
                throw new IOException("Unsupported wire type " + wire
                        + " for field " + (tag >>> 3));
        }
    }

    // -- Primitive readers --------------------------------------------

    public int readVarint32() throws IOException {
        return (int) readVarint64();
    }

    public long readVarint64() throws IOException {
        long result = 0L;
        int shift = 0;
        while (shift < 64) {
            int b = readByte();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }
        throw new IOException("Malformed varint -- exceeds 10 bytes");
    }

    public int readFixed32() throws IOException {
        ensure(4);
        int b0 = buf[pos++] & 0xFF;
        int b1 = buf[pos++] & 0xFF;
        int b2 = buf[pos++] & 0xFF;
        int b3 = buf[pos++] & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    public long readFixed64() throws IOException {
        ensure(8);
        long lo = readFixed32() & 0xFFFFFFFFL;
        long hi = readFixed32() & 0xFFFFFFFFL;
        return (hi << 32) | lo;
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readFixed32());
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readFixed64());
    }

    public boolean readBool() throws IOException {
        return readVarint64() != 0L;
    }

    public static int zagZig32(int n) {
        return (n >>> 1) ^ -(n & 1);
    }

    public static long zagZig64(long n) {
        return (n >>> 1) ^ -(n & 1L);
    }

    public int readSInt32() throws IOException {
        return zagZig32(readVarint32());
    }

    public long readSInt64() throws IOException {
        return zagZig64(readVarint64());
    }

    public String readString() throws IOException {
        int len = readVarint32();
        ensure(len);
        try {
            String s = new String(buf, pos, len, "UTF-8");
            pos += len;
            return s;
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
    }

    public byte[] readBytes() throws IOException {
        int len = readVarint32();
        ensure(len);
        byte[] out = new byte[len];
        System.arraycopy(buf, pos, out, 0, len);
        pos += len;
        return out;
    }

    /// Reads a length-delimited sub-message via `codec`.
    public <T> T readMessage(ProtoCodec<T> codec) throws IOException {
        int len = readVarint32();
        ensure(len);
        ProtoReader sub = new ProtoReader(buf, pos, len);
        pos += len;
        return codec.read(sub);
    }

    /// Reads a packed `repeated` scalar segment and appends each
    /// element to `target` using the supplied [PackedReader] strategy.
    public <T> void readPacked(java.util.List<T> target, PackedReader<T> strategy) throws IOException {
        int len = readVarint32();
        ensure(len);
        int end = pos + len;
        ProtoReader sub = new ProtoReader(buf, pos, len);
        while (!sub.isAtEnd()) {
            target.add(strategy.read(sub));
        }
        pos = end;
    }

    private int readByte() throws IOException {
        if (pos >= limit) { throw new EOFException("Unexpected end of protobuf stream"); }
        return buf[pos++] & 0xFF;
    }

    private void ensure(int n) throws IOException {
        if (n < 0) { throw new IOException("Negative length"); }
        if (pos + n > limit) {
            throw new EOFException("Truncated protobuf stream: need " + n
                    + " more bytes but " + (limit - pos) + " remaining");
        }
    }

    private void advance(int n) throws IOException {
        ensure(n);
        pos += n;
    }

    /// Strategy hook for [#readPacked]. Lets the generated codec
    /// reuse this reader without committing to a fixed element type.
    public interface PackedReader<T> {
        T read(ProtoReader in) throws IOException;
    }

    /// Wraps the supplied byte array in a fresh reader. Convenience
    /// for unit tests; production code allocates the reader directly.
    public static ProtoReader of(byte[] data) {
        return new ProtoReader(data == null ? new byte[0] : data);
    }

    /// Convenience helper for callers that prefer `ByteArrayInputStream`
    /// over byte arrays. Not used internally but kept for symmetry
    /// with [ProtoWriter#stream()].
    public static byte[] drain(ByteArrayInputStream in) throws IOException {
        int n = in.available();
        byte[] out = new byte[n];
        int read = 0;
        while (read < n) {
            int r = in.read(out, read, n - read);
            if (r < 0) { throw new EOFException(); }
            read += r;
        }
        return out;
    }
}
