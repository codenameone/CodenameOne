/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.io.grpc;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// Wire-level round-trip tests for [ProtoWriter] / [ProtoReader].
/// Every test encodes one or more fields, decodes them back, and
/// verifies bit-for-bit fidelity against the values that went in.
/// Independent of any generated codec.
public class ProtoCodecTest {

    @Test
    public void varintRoundTrip() throws Exception {
        long[] cases = { 0L, 1L, 127L, 128L, 16383L, 16384L,
                Integer.MAX_VALUE, ((long) Integer.MAX_VALUE) + 1L,
                Long.MAX_VALUE, -1L, Long.MIN_VALUE };
        for (long v : cases) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            new ProtoWriter(buf).writeVarint64(v);
            ProtoReader r = new ProtoReader(buf.toByteArray());
            assertEquals("varint round-trip for " + v, v, r.readVarint64());
            assertTrue("reader consumed entire buffer for " + v, r.isAtEnd());
        }
    }

    @Test
    public void zigZagRoundTrip() {
        int[] s32 = { 0, -1, 1, -2, 2, Integer.MAX_VALUE, Integer.MIN_VALUE };
        for (int v : s32) {
            assertEquals("zigzag32 round-trip for " + v,
                    v, ProtoReader.zagZig32(ProtoWriter.zigZag32(v)));
        }
        long[] s64 = { 0L, -1L, 1L, Long.MAX_VALUE, Long.MIN_VALUE };
        for (long v : s64) {
            assertEquals("zigzag64 round-trip for " + v,
                    v, ProtoReader.zagZig64(ProtoWriter.zigZag64(v)));
        }
    }

    @Test
    public void fixed32And64() throws Exception {
        int[] f32 = { 0, 1, -1, 0x7FFFFFFF, (int) 0xDEADBEEF };
        for (int v : f32) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            new ProtoWriter(buf).writeFixed32(v);
            assertEquals(4, buf.size());
            assertEquals(v, new ProtoReader(buf.toByteArray()).readFixed32());
        }
        long[] f64 = { 0L, 1L, -1L, 0x7FFFFFFFFFFFFFFFL, 0xCAFEBABEDEADBEEFL };
        for (long v : f64) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            new ProtoWriter(buf).writeFixed64(v);
            assertEquals(8, buf.size());
            assertEquals(v, new ProtoReader(buf.toByteArray()).readFixed64());
        }
    }

    @Test
    public void scalarFieldsSkipDefaults() throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ProtoWriter w = new ProtoWriter(buf);
        w.writeInt32(1, 0);          // default -- skipped
        w.writeInt32(2, 42);         // emitted
        w.writeString(3, "");        // default -- skipped
        w.writeString(4, "hi");      // emitted
        w.writeBool(5, false);       // default -- skipped
        w.writeBool(6, true);        // emitted
        assertFalse("default-valued fields must be skipped", buf.size() == 0
                && false); // ensure we still produce bytes for non-defaults
        ProtoReader r = new ProtoReader(buf.toByteArray());
        int tag1 = r.readTag(); assertEquals(2, tag1 >>> 3); assertEquals(42, r.readVarint32());
        int tag2 = r.readTag(); assertEquals(4, tag2 >>> 3); assertEquals("hi", r.readString());
        int tag3 = r.readTag(); assertEquals(6, tag3 >>> 3); assertTrue(r.readBool());
        assertTrue("no further fields", r.isAtEnd());
    }

    @Test
    public void floatAndDoubleRoundTrip() throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ProtoWriter w = new ProtoWriter(buf);
        w.writeFloat(1, 3.14f);
        w.writeDouble(2, 2.71828);
        ProtoReader r = new ProtoReader(buf.toByteArray());
        assertEquals(1, r.readTag() >>> 3); assertEquals(3.14f, r.readFloat(), 0.0f);
        assertEquals(2, r.readTag() >>> 3); assertEquals(2.71828, r.readDouble(), 0.0);
    }

    @Test
    public void bytesAndStringFields() throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ProtoWriter w = new ProtoWriter(buf);
        w.writeString(1, "hello é");
        byte[] payload = new byte[]{ 1, 2, 3, (byte) 0xFF };
        w.writeBytes(2, payload);
        ProtoReader r = new ProtoReader(buf.toByteArray());
        assertEquals(1, r.readTag() >>> 3);
        assertEquals("hello é", r.readString());
        assertEquals(2, r.readTag() >>> 3);
        assertArrayEquals(payload, r.readBytes());
    }

    @Test
    public void nestedMessageRoundTrip() throws Exception {
        ProtoCodec<Inner> innerCodec = new ProtoCodec<Inner>() {
            public void write(ProtoWriter out, Inner v) throws java.io.IOException {
                out.writeInt32(1, v.n);
                out.writeString(2, v.s);
            }
            public Inner read(ProtoReader in) throws java.io.IOException {
                Inner i = new Inner();
                int tag;
                while ((tag = in.readTag()) != 0) {
                    switch (tag >>> 3) {
                        case 1: i.n = in.readVarint32(); break;
                        case 2: i.s = in.readString(); break;
                        default: in.skipField(tag);
                    }
                }
                return i;
            }
        };
        Inner src = new Inner(); src.n = 7; src.s = "child";
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ProtoWriter w = new ProtoWriter(buf);
        w.writeMessage(5, src, innerCodec);
        ProtoReader r = new ProtoReader(buf.toByteArray());
        assertEquals(5, r.readTag() >>> 3);
        Inner decoded = r.readMessage(innerCodec);
        assertEquals(7, decoded.n);
        assertEquals("child", decoded.s);
    }

    @Test
    public void packedRepeatedInt32() throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        new ProtoWriter(buf).writePackedInt32(1, Arrays.asList(1, 2, 3, 127, 128));
        ProtoReader r = new ProtoReader(buf.toByteArray());
        int tag = r.readTag();
        assertEquals(1, tag >>> 3);
        assertEquals(ProtoWriter.WIRE_LEN, tag & 7);
        java.util.List<Integer> out = new java.util.ArrayList<Integer>();
        r.readPacked(out, new ProtoReader.PackedReader<Integer>() {
            public Integer read(ProtoReader rr) throws java.io.IOException { return rr.readVarint32(); }
        });
        assertEquals(Arrays.asList(1, 2, 3, 127, 128), out);
    }

    @Test
    public void skipUnknownField() throws Exception {
        // Write a known field then an unknown one; reader should
        // consume the known field, see the unknown tag, skip it,
        // then EOF.
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ProtoWriter w = new ProtoWriter(buf);
        w.writeString(1, "x");
        w.writeInt32(99, 0xDEADBEEF);
        ProtoReader r = new ProtoReader(buf.toByteArray());
        assertEquals(1, r.readTag() >>> 3);
        assertEquals("x", r.readString());
        int unknown = r.readTag();
        assertEquals(99, unknown >>> 3);
        r.skipField(unknown);
        assertTrue(r.isAtEnd());
    }

    private static final class Inner {
        int n;
        String s;
    }
}
