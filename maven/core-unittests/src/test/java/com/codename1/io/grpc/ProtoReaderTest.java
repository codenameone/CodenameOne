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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProtoReaderTest {

    private static ProtoWriter writer(ByteArrayOutputStream buf) {
        return new ProtoWriter(buf);
    }

    @Test
    void emptyReaderIsAtEndImmediately() {
        ProtoReader r = new ProtoReader(new byte[0]);
        assertTrue(r.isAtEnd());
        assertEquals(0, r.remaining());
    }

    @Test
    void nullDataYieldsEmptyReader() {
        ProtoReader r = new ProtoReader(null);
        assertTrue(r.isAtEnd());
        assertEquals(0, r.remaining());
    }

    @Test
    void readTagReturnsZeroSentinelAtEnd() throws IOException {
        ProtoReader r = new ProtoReader(new byte[0]);
        assertEquals(0, r.readTag());
    }

    @Test
    void remainingTracksConsumption() throws IOException {
        ProtoReader r = new ProtoReader(new byte[]{0x01, 0x02, 0x03});
        assertEquals(3, r.remaining());
        r.readVarint32();
        assertEquals(2, r.remaining());
    }

    @Test
    void varintRoundTrips() throws IOException {
        int[] samples = {0, 1, 127, 128, 300, 16384, Integer.MAX_VALUE, -1};
        for (int sample : samples) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            writer(buf).writeVarint32(sample);
            ProtoReader r = ProtoReader.of(buf.toByteArray());
            assertEquals(sample, r.readVarint32(), "varint32 round-trip for " + sample);
        }
    }

    @Test
    void varint64RoundTrips() throws IOException {
        long[] samples = {0L, 1L, 0xFFFFFFFFL, Long.MAX_VALUE, -1L};
        for (long sample : samples) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            writer(buf).writeVarint64(sample);
            ProtoReader r = ProtoReader.of(buf.toByteArray());
            assertEquals(sample, r.readVarint64(), "varint64 round-trip for " + sample);
        }
    }

    @Test
    void fixed32RoundTrips() throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writer(buf).writeFixed32(0x01020304);
        assertEquals(0x01020304, ProtoReader.of(buf.toByteArray()).readFixed32());
    }

    @Test
    void fixed64RoundTrips() throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writer(buf).writeFixed64(0x0102030405060708L);
        assertEquals(0x0102030405060708L, ProtoReader.of(buf.toByteArray()).readFixed64());
    }

    @Test
    void floatAndDoubleRoundTrip() throws IOException {
        ByteArrayOutputStream fbuf = new ByteArrayOutputStream();
        writer(fbuf).writeFixed32(Float.floatToIntBits(3.5f));
        assertEquals(3.5f, ProtoReader.of(fbuf.toByteArray()).readFloat());

        ByteArrayOutputStream dbuf = new ByteArrayOutputStream();
        writer(dbuf).writeFixed64(Double.doubleToLongBits(2.718281828));
        assertEquals(2.718281828, ProtoReader.of(dbuf.toByteArray()).readDouble());
    }

    @Test
    void boolReadsNonZeroAsTrue() throws IOException {
        assertTrue(ProtoReader.of(new byte[]{0x01}).readBool());
        assertFalse(ProtoReader.of(new byte[]{0x00}).readBool());
    }

    @Test
    void zagZigDecodingInvertsZigZagEncoding() {
        int[] ints = {0, -1, 1, -2, 2, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int v : ints) {
            assertEquals(v, ProtoReader.zagZig32(ProtoWriter.zigZag32(v)));
        }
        long[] longs = {0L, -1L, 1L, Long.MAX_VALUE, Long.MIN_VALUE};
        for (long v : longs) {
            assertEquals(v, ProtoReader.zagZig64(ProtoWriter.zigZag64(v)));
        }
    }

    @Test
    void sintFieldsRoundTrip() throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ProtoWriter w = writer(buf);
        w.writeSInt32(1, -123456);
        w.writeSInt64(2, -9876543210L);

        ProtoReader r = ProtoReader.of(buf.toByteArray());
        assertEquals(1, r.readTag() >>> 3);
        assertEquals(-123456, r.readSInt32());
        assertEquals(2, r.readTag() >>> 3);
        assertEquals(-9876543210L, r.readSInt64());
    }

    @Test
    void stringRoundTripsIncludingUnicode() throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writer(buf).writeString(1, "héllo→世界");
        ProtoReader r = ProtoReader.of(buf.toByteArray());
        r.readTag();
        assertEquals("héllo→世界", r.readString());
    }

    @Test
    void bytesRoundTrip() throws IOException {
        byte[] payload = {0, 1, 2, 127, (byte) 255};
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writer(buf).writeBytes(1, payload);
        ProtoReader r = ProtoReader.of(buf.toByteArray());
        r.readTag();
        assertArrayEquals(payload, r.readBytes());
    }

    @Test
    void skipFieldAdvancesPastEachWireType() throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ProtoWriter w = writer(buf);
        w.writeInt32(1, 150);          // VARINT
        w.writeFixed32Field(2, 42);    // I32
        w.writeFixed64Field(3, 99L);   // I64
        w.writeString(4, "skip me");   // LEN
        w.writeBool(5, true);          // the field we actually want

        ProtoReader r = ProtoReader.of(buf.toByteArray());
        int tag;
        boolean found = false;
        while ((tag = r.readTag()) != 0) {
            if ((tag >>> 3) == 5) {
                assertTrue(r.readBool());
                found = true;
            } else {
                r.skipField(tag);
            }
        }
        assertTrue(found, "field 5 should be reachable after skipping fields 1-4");
        assertTrue(r.isAtEnd());
    }

    @Test
    void skipFieldRejectsUnsupportedWireType() {
        // Wire type 3 (group start) is not supported by skipField.
        ProtoReader r = ProtoReader.of(new byte[]{(byte) ((1 << 3) | 3)});
        assertThrows(IOException.class, () -> {
            int tag = r.readTag();
            r.skipField(tag);
        });
    }

    @Test
    void truncatedVarintThrowsEof() {
        // Continuation bit set on the final byte -> stream ends mid-varint.
        ProtoReader r = ProtoReader.of(new byte[]{(byte) 0x80});
        assertThrows(EOFException.class, r::readVarint64);
    }

    @Test
    void overlongVarintThrows() {
        // Eleven continuation bytes exceed the 10-byte varint ceiling.
        byte[] data = new byte[11];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) 0x80;
        }
        ProtoReader r = ProtoReader.of(data);
        assertThrows(IOException.class, r::readVarint64);
    }

    @Test
    void readFixed32OnTruncatedStreamThrowsEof() {
        ProtoReader r = ProtoReader.of(new byte[]{1, 2});
        assertThrows(EOFException.class, r::readFixed32);
    }

    @Test
    void readStringRejectsNegativeLength() {
        // A varint that decodes to -1 (length) must be rejected, not allocate.
        byte[] data = new byte[10];
        for (int i = 0; i < 9; i++) {
            data[i] = (byte) 0xFF;
        }
        data[9] = 0x01;
        ProtoReader r = ProtoReader.of(data);
        assertThrows(IOException.class, r::readString);
    }

    @Test
    void readMessageReadsLengthDelimitedSubMessage() throws IOException {
        ProtoCodec<int[]> codec = new ProtoCodec<int[]>() {
            public void write(ProtoWriter out, int[] value) throws IOException {
                out.writeInt32(1, value[0]);
            }

            public int[] read(ProtoReader in) throws IOException {
                int[] result = {0};
                int tag;
                while ((tag = in.readTag()) != 0) {
                    if ((tag >>> 3) == 1) {
                        result[0] = in.readVarint32();
                    } else {
                        in.skipField(tag);
                    }
                }
                return result;
            }
        };

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        new ProtoWriter(buf).writeMessage(2, new int[]{777}, codec);

        ProtoReader r = ProtoReader.of(buf.toByteArray());
        assertEquals(2, r.readTag() >>> 3);
        int[] decoded = r.readMessage(codec);
        assertEquals(777, decoded[0]);
        assertTrue(r.isAtEnd());
    }

    @Test
    void readPackedConsumesEntireSegment() throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        new ProtoWriter(buf).writePackedInt32(1, java.util.Arrays.asList(3, 270, 86942));

        ProtoReader r = ProtoReader.of(buf.toByteArray());
        r.readTag();
        List<Integer> out = new ArrayList<>();
        r.readPacked(out, new ProtoReader.PackedReader<Integer>() {
            public Integer read(ProtoReader in) throws IOException {
                return in.readVarint32();
            }
        });
        assertEquals(java.util.Arrays.asList(3, 270, 86942), out);
        assertTrue(r.isAtEnd());
    }

    @Test
    void sliceConstructorReadsOnlyWithinBounds() throws IOException {
        // Reader windowed over the middle byte only.
        ProtoReader r = new ProtoReader(new byte[]{(byte) 0xFF, 0x05, (byte) 0xFF}, 1, 1);
        assertEquals(1, r.remaining());
        assertEquals(5, r.readVarint32());
        assertTrue(r.isAtEnd());
    }

    @Test
    void drainReadsAllAvailableBytes() throws IOException {
        byte[] src = {9, 8, 7, 6};
        byte[] out = ProtoReader.drain(new ByteArrayInputStream(src));
        assertArrayEquals(src, out);
    }
}
