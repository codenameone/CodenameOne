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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/// Low-level protobuf wire-format writer used by generated
/// [ProtoCodec] implementations. Provides per-scalar-type field
/// emitters that skip default values (proto3 default-omission
/// behaviour) so the byte sequence matches what `protoc`-generated
/// code would emit for the same input.
///
/// Wire types (3-bit tag suffix):
///
/// | Code | Name         | Used for                                    |
/// |------|--------------|---------------------------------------------|
/// | 0    | VARINT       | int32, int64, uint32, uint64, sint*, bool, enum |
/// | 1    | I64          | fixed64, sfixed64, double                   |
/// | 2    | LEN          | string, bytes, embedded messages, packed repeated |
/// | 5    | I32          | fixed32, sfixed32, float                    |
public final class ProtoWriter {

    public static final int WIRE_VARINT = 0;
    public static final int WIRE_I64 = 1;
    public static final int WIRE_LEN = 2;
    public static final int WIRE_I32 = 5;

    private final OutputStream out;

    public ProtoWriter(OutputStream out) {
        this.out = out;
    }

    /// Backing stream so generated codecs can stage a sub-message
    /// into a `ByteArrayOutputStream` and length-prefix the result.
    public OutputStream stream() {
        return out;
    }

    // -- Primitive emitters -------------------------------------------

    public void writeTag(int fieldNumber, int wireType) throws IOException {
        writeVarint32((fieldNumber << 3) | wireType);
    }

    public void writeVarint32(int value) throws IOException {
        // Per protobuf spec, int32 is sign-extended to 64 bits before
        // varint encoding. Cast to long with sign extension so negative
        // ints occupy 10 bytes on the wire (matching protoc).
        writeVarint64((long) value);
    }

    public void writeVarint64(long value) throws IOException {
        while (true) {
            if ((value & ~0x7FL) == 0L) {
                out.write((int) value);
                return;
            }
            out.write(((int) value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    public void writeFixed32(int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 24) & 0xFF);
    }

    public void writeFixed64(long value) throws IOException {
        out.write((int) (value & 0xFF));
        out.write((int) ((value >>> 8) & 0xFF));
        out.write((int) ((value >>> 16) & 0xFF));
        out.write((int) ((value >>> 24) & 0xFF));
        out.write((int) ((value >>> 32) & 0xFF));
        out.write((int) ((value >>> 40) & 0xFF));
        out.write((int) ((value >>> 48) & 0xFF));
        out.write((int) ((value >>> 56) & 0xFF));
    }

    public static int zigZag32(int n) {
        return (n << 1) ^ (n >> 31);
    }

    public static long zigZag64(long n) {
        return (n << 1) ^ (n >> 63);
    }

    // -- Field helpers ------------------------------------------------
    //
    // Each per-type helper skips default values (proto3 semantics).
    // Generated codecs call these unconditionally so the per-field
    // emit code is one line.

    public void writeInt32(int field, int value) throws IOException {
        if (value == 0) return;
        writeTag(field, WIRE_VARINT);
        writeVarint32(value);
    }

    public void writeInt64(int field, long value) throws IOException {
        if (value == 0L) return;
        writeTag(field, WIRE_VARINT);
        writeVarint64(value);
    }

    public void writeUInt32(int field, int value) throws IOException {
        if (value == 0) return;
        writeTag(field, WIRE_VARINT);
        // uint32 is encoded as unsigned varint -- mask to 32 bits.
        writeVarint64(value & 0xFFFFFFFFL);
    }

    public void writeUInt64(int field, long value) throws IOException {
        if (value == 0L) return;
        writeTag(field, WIRE_VARINT);
        writeVarint64(value);
    }

    public void writeSInt32(int field, int value) throws IOException {
        if (value == 0) return;
        writeTag(field, WIRE_VARINT);
        writeVarint64(zigZag32(value) & 0xFFFFFFFFL);
    }

    public void writeSInt64(int field, long value) throws IOException {
        if (value == 0L) return;
        writeTag(field, WIRE_VARINT);
        writeVarint64(zigZag64(value));
    }

    public void writeFixed32Field(int field, int value) throws IOException {
        if (value == 0) return;
        writeTag(field, WIRE_I32);
        writeFixed32(value);
    }

    public void writeFixed64Field(int field, long value) throws IOException {
        if (value == 0L) return;
        writeTag(field, WIRE_I64);
        writeFixed64(value);
    }

    public void writeBool(int field, boolean value) throws IOException {
        if (!value) return;
        writeTag(field, WIRE_VARINT);
        out.write(1);
    }

    public void writeFloat(int field, float value) throws IOException {
        if (value == 0.0f) return;
        writeTag(field, WIRE_I32);
        writeFixed32(Float.floatToIntBits(value));
    }

    public void writeDouble(int field, double value) throws IOException {
        if (value == 0.0d) return;
        writeTag(field, WIRE_I64);
        writeFixed64(Double.doubleToLongBits(value));
    }

    public void writeString(int field, String value) throws IOException {
        if (value == null || value.length() == 0) return;
        writeBytes(field, utf8(value));
    }

    public void writeBytes(int field, byte[] value) throws IOException {
        if (value == null || value.length == 0) return;
        writeTag(field, WIRE_LEN);
        writeVarint32(value.length);
        out.write(value);
    }

    /// Writes a nested `@ProtoMessage` value as a length-delimited
    /// field. Generated codecs call into [ProtoCodecs#lookup(Class)]
    /// to find the nested codec.
    public <T> void writeMessage(int field, T value, ProtoCodec<T> codec) throws IOException {
        if (value == null) return;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ProtoWriter sub = new ProtoWriter(buf);
        codec.write(sub, value);
        byte[] body = buf.toByteArray();
        writeTag(field, WIRE_LEN);
        writeVarint32(body.length);
        out.write(body);
    }

    /// Writes a `repeated` field of nested messages (one tag + length
    /// prefix per element -- proto3 doesn't pack length-delimited
    /// repeated entries).
    public <T> void writeMessageList(int field, java.util.List<T> values,
                                     ProtoCodec<T> codec) throws IOException {
        if (values == null || values.isEmpty()) return;
        for (int i = 0, n = values.size(); i < n; i++) {
            writeMessage(field, values.get(i), codec);
        }
    }

    /// Writes a `repeated` field of strings (one tag + length prefix
    /// per element).
    public void writeStringList(int field, java.util.List<String> values) throws IOException {
        if (values == null || values.isEmpty()) return;
        for (int i = 0, n = values.size(); i < n; i++) {
            String v = values.get(i);
            if (v == null) continue;
            byte[] body = utf8(v);
            writeTag(field, WIRE_LEN);
            writeVarint32(body.length);
            out.write(body);
        }
    }

    /// Writes a packed `repeated int32` field (proto3 default packing
    /// for scalar lists).
    public void writePackedInt32(int field, java.util.List<Integer> values) throws IOException {
        if (values == null || values.isEmpty()) return;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ProtoWriter sub = new ProtoWriter(buf);
        for (int i = 0, n = values.size(); i < n; i++) {
            Integer v = values.get(i);
            sub.writeVarint32(v == null ? 0 : v.intValue());
        }
        byte[] body = buf.toByteArray();
        writeTag(field, WIRE_LEN);
        writeVarint32(body.length);
        out.write(body);
    }

    /// Writes a packed `repeated int64` field.
    public void writePackedInt64(int field, java.util.List<Long> values) throws IOException {
        if (values == null || values.isEmpty()) return;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ProtoWriter sub = new ProtoWriter(buf);
        for (int i = 0, n = values.size(); i < n; i++) {
            Long v = values.get(i);
            sub.writeVarint64(v == null ? 0L : v.longValue());
        }
        byte[] body = buf.toByteArray();
        writeTag(field, WIRE_LEN);
        writeVarint32(body.length);
        out.write(body);
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            // UTF-8 is mandatory on every JVM CN1 targets.
            throw new RuntimeException(uee);
        }
    }
}
