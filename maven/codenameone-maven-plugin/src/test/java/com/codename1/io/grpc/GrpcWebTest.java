/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.io.grpc;

import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/// Tests gRPC-Web payload framing and trailer parsing in
/// [GrpcWeb]. Doesn't touch the network -- assembles raw response
/// bytes and runs them through [GrpcWeb#decode].
public class GrpcWebTest {

    @Test
    public void frameWrapsPayloadWithFiveByteHeader() throws Exception {
        ProtoCodec<String> codec = new ProtoCodec<String>() {
            public void write(ProtoWriter out, String v) throws java.io.IOException {
                out.writeString(1, v);
            }
            public String read(ProtoReader in) throws java.io.IOException {
                int tag = in.readTag();
                return tag == 0 ? "" : in.readString();
            }
        };
        byte[] framed = GrpcWeb.frame("hi", codec);
        assertEquals("flag byte is 0 for data frame", 0, framed[0]);
        int len = ((framed[1] & 0xFF) << 24) | ((framed[2] & 0xFF) << 16)
                | ((framed[3] & 0xFF) << 8) | (framed[4] & 0xFF);
        assertEquals("length matches payload size", framed.length - 5, len);
    }

    @Test
    public void decodeOkResponseUnframesAndDeserialises() throws Exception {
        ProtoCodec<String> codec = new ProtoCodec<String>() {
            public void write(ProtoWriter out, String v) throws java.io.IOException {
                out.writeString(1, v);
            }
            public String read(ProtoReader in) throws java.io.IOException {
                String s = "";
                int tag;
                while ((tag = in.readTag()) != 0) {
                    if ((tag >>> 3) == 1) s = in.readString();
                    else in.skipField(tag);
                }
                return s;
            }
        };
        byte[] payload = ProtoEncodeHelper.encodeString(1, "hello world");
        byte[] frame = withFrameHeader((byte) 0, payload);
        byte[] trailer = "grpc-status:0\r\ngrpc-message:OK\r\n".getBytes("UTF-8");
        byte[] trailerFrame = withFrameHeader((byte) 0x80, trailer);
        byte[] body = concat(frame, trailerFrame);

        GrpcResponse<String> resp = GrpcWeb.decode(body, 200, codec);
        assertTrue("status 0 -> ok", resp.isOk());
        assertEquals(200, resp.getHttpCode());
        assertEquals("hello world", resp.getResponseData());
    }

    @Test
    public void decodeStatusFailureLeavesNullPayload() throws Exception {
        ProtoCodec<String> codec = new ProtoCodec<String>() {
            public void write(ProtoWriter out, String v) {}
            public String read(ProtoReader in) { return ""; }
        };
        byte[] trailer = "grpc-status:7\r\ngrpc-message:nope\r\n".getBytes("UTF-8");
        byte[] body = withFrameHeader((byte) 0x80, trailer);
        GrpcResponse<String> resp = GrpcWeb.decode(body, 200, codec);
        assertFalse("non-zero gRPC status -> not ok", resp.isOk());
        assertEquals(GrpcResponse.STATUS_PERMISSION_DENIED, resp.getResponseCode());
        assertEquals("nope", resp.getResponseErrorMessage());
        assertNull(resp.getResponseData());
    }

    @Test
    public void decodeWithoutTrailerReportsTransportFailure() {
        ProtoCodec<String> codec = new ProtoCodec<String>() {
            public void write(ProtoWriter out, String v) {}
            public String read(ProtoReader in) { return ""; }
        };
        // Data frame only, no trailer -> transport failure.
        byte[] body = withFrameHeader((byte) 0, new byte[]{ 1, 2, 3 });
        GrpcResponse<String> resp = GrpcWeb.decode(body, 200, codec);
        assertEquals(GrpcResponse.STATUS_TRANSPORT_FAILURE, resp.getResponseCode());
    }

    @Test
    public void decodeEmptyBodyReportsTransportFailure() {
        ProtoCodec<String> codec = new ProtoCodec<String>() {
            public void write(ProtoWriter out, String v) {}
            public String read(ProtoReader in) { return ""; }
        };
        GrpcResponse<String> resp = GrpcWeb.decode(new byte[0], 502, codec);
        assertEquals(GrpcResponse.STATUS_TRANSPORT_FAILURE, resp.getResponseCode());
        assertEquals(502, resp.getHttpCode());
    }

    // -- helpers ------------------------------------------------------

    private static byte[] withFrameHeader(byte flags, byte[] body) {
        byte[] out = new byte[5 + body.length];
        out[0] = flags;
        out[1] = (byte) ((body.length >>> 24) & 0xFF);
        out[2] = (byte) ((body.length >>> 16) & 0xFF);
        out[3] = (byte) ((body.length >>> 8) & 0xFF);
        out[4] = (byte) (body.length & 0xFF);
        System.arraycopy(body, 0, out, 5, body.length);
        return out;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static final class ProtoEncodeHelper {
        static byte[] encodeString(int tag, String s) throws Exception {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            new ProtoWriter(buf).writeString(tag, s);
            return buf.toByteArray();
        }
    }
}
