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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ProtoWriterTest {

    private ByteArrayOutputStream buf;
    private ProtoWriter writer;

    private ProtoWriter newWriter() {
        buf = new ByteArrayOutputStream();
        writer = new ProtoWriter(buf);
        return writer;
    }

    private byte[] bytes() {
        return buf.toByteArray();
    }

    @Test
    void streamReturnsBackingOutputStream() {
        ProtoWriter w = newWriter();
        assertSame(buf, w.stream());
    }

    @Test
    void writeTagEncodesFieldNumberAndWireType() throws IOException {
        newWriter();
        writer.writeTag(1, ProtoWriter.WIRE_VARINT);
        // (1 << 3) | 0 == 0x08.
        assertArrayEquals(new byte[]{0x08}, bytes());

        newWriter();
        writer.writeTag(2, ProtoWriter.WIRE_LEN);
        // (2 << 3) | 2 == 0x12.
        assertArrayEquals(new byte[]{0x12}, bytes());
    }

    @Test
    void writeVarint32EncodesSmallValueInOneByte() throws IOException {
        newWriter();
        writer.writeVarint32(1);
        assertArrayEquals(new byte[]{0x01}, bytes());
    }

    @Test
    void writeVarint32EncodesMultiByteValue() throws IOException {
        newWriter();
        writer.writeVarint32(300);
        // 300 -> 0xAC 0x02 (canonical protobuf example).
        assertArrayEquals(new byte[]{(byte) 0xAC, 0x02}, bytes());
    }

    @Test
    void writeVarint32SignExtendsNegativeToTenBytes() throws IOException {
        newWriter();
        writer.writeVarint32(-1);
        // int32 -1 is sign-extended to 64 bits -> ten 0xFF/0x01 bytes.
        assertEquals(10, bytes().length);
        assertEquals((byte) 0x01, bytes()[9]);
    }

    @Test
    void writeFixed32IsLittleEndian() throws IOException {
        newWriter();
        writer.writeFixed32(0x01020304);
        assertArrayEquals(new byte[]{0x04, 0x03, 0x02, 0x01}, bytes());
    }

    @Test
    void writeFixed64IsLittleEndian() throws IOException {
        newWriter();
        writer.writeFixed64(0x0102030405060708L);
        assertArrayEquals(
                new byte[]{0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01},
                bytes());
    }

    @Test
    void zigZagEncodingMatchesProtobufSpec() {
        assertEquals(0, ProtoWriter.zigZag32(0));
        assertEquals(1, ProtoWriter.zigZag32(-1));
        assertEquals(2, ProtoWriter.zigZag32(1));
        assertEquals(3, ProtoWriter.zigZag32(-2));
        assertEquals(0L, ProtoWriter.zigZag64(0L));
        assertEquals(1L, ProtoWriter.zigZag64(-1L));
        assertEquals(2L, ProtoWriter.zigZag64(1L));
    }

    @Test
    void scalarFieldsOmitDefaultValues() throws IOException {
        newWriter();
        writer.writeInt32(1, 0);
        writer.writeInt64(2, 0L);
        writer.writeUInt32(3, 0);
        writer.writeUInt64(4, 0L);
        writer.writeSInt32(5, 0);
        writer.writeSInt64(6, 0L);
        writer.writeFixed32Field(7, 0);
        writer.writeFixed64Field(8, 0L);
        writer.writeBool(9, false);
        writer.writeFloat(10, 0.0f);
        writer.writeDouble(11, 0.0d);
        writer.writeString(12, "");
        writer.writeString(12, null);
        writer.writeBytes(13, new byte[0]);
        writer.writeBytes(13, null);
        // proto3 default-omission: nothing should have been written.
        assertEquals(0, bytes().length);
    }

    @Test
    void writeInt32EmitsTagAndVarint() throws IOException {
        newWriter();
        writer.writeInt32(1, 150);
        // field 1 varint, value 150 -> 0x08 0x96 0x01 (canonical example).
        assertArrayEquals(new byte[]{0x08, (byte) 0x96, 0x01}, bytes());
    }

    @Test
    void writeBoolTrueEmitsSingleOne() throws IOException {
        newWriter();
        writer.writeBool(1, true);
        assertArrayEquals(new byte[]{0x08, 0x01}, bytes());
    }

    @Test
    void writeStringEmitsLengthDelimitedUtf8() throws IOException {
        newWriter();
        writer.writeString(1, "AB");
        // tag (field 1, LEN) = 0x0A, length 2, then 'A','B'.
        assertArrayEquals(new byte[]{0x0A, 0x02, 'A', 'B'}, bytes());
    }

    @Test
    void writeMessageLengthPrefixesEncodedBody() throws IOException {
        ProtoCodec<int[]> codec = new ProtoCodec<int[]>() {
            public void write(ProtoWriter out, int[] value) throws IOException {
                out.writeInt32(1, value[0]);
            }

            public int[] read(ProtoReader in) {
                throw new UnsupportedOperationException();
            }
        };
        newWriter();
        writer.writeMessage(2, new int[]{150}, codec);
        // field 2 LEN tag = 0x12, length 3, body 0x08 0x96 0x01.
        assertArrayEquals(
                new byte[]{0x12, 0x03, 0x08, (byte) 0x96, 0x01}, bytes());
    }

    @Test
    void writeMessageSkipsNullValue() throws IOException {
        ProtoCodec<Object> codec = new ProtoCodec<Object>() {
            public void write(ProtoWriter out, Object value) {
                fail("codec must not be invoked for null message");
            }

            public Object read(ProtoReader in) {
                throw new UnsupportedOperationException();
            }
        };
        newWriter();
        writer.writeMessage(1, null, codec);
        assertEquals(0, bytes().length);
    }

    @Test
    void writeStringListEmitsOneEntryPerNonNullElement() throws IOException {
        newWriter();
        writer.writeStringList(1, Arrays.asList("A", null, "B"));
        // Two LEN entries; the null element is skipped.
        assertArrayEquals(
                new byte[]{0x0A, 0x01, 'A', 0x0A, 0x01, 'B'}, bytes());
    }

    @Test
    void writePackedInt32EncodesContiguousVarints() throws IOException {
        newWriter();
        writer.writePackedInt32(1, Arrays.asList(1, 2, 150));
        // LEN tag 0x0A, body length 4, then 0x01 0x02 0x96 0x01.
        assertArrayEquals(
                new byte[]{0x0A, 0x04, 0x01, 0x02, (byte) 0x96, 0x01}, bytes());
    }

    @Test
    void emptyListsEmitNothing() throws IOException {
        newWriter();
        writer.writeStringList(1, null);
        writer.writeStringList(1, java.util.Collections.<String>emptyList());
        writer.writePackedInt32(1, null);
        writer.writePackedInt64(1, null);
        writer.writeMessageList(1, null, null);
        assertEquals(0, bytes().length);
    }
}
