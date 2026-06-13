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

import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.DisplayTest;
import com.codename1.util.OnComplete;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link GrpcWeb}: the public gRPC-Web framing helper
 * ({@link GrpcWeb#frame}), the response decoder ({@link GrpcWeb#decode})
 * across success / error / malformed bodies, and the end-to-end
 * {@link GrpcWeb#invokeUnary} HTTP path against the mock network layer in
 * {@link TestCodenameOneImplementation}.
 */
class GrpcWebTest extends UITestBase {

    private static final String BASE_URL = "https://grpc.example.com";
    private static final String SERVICE = "helloworld.Greeter";
    private static final String METHOD = "SayHello";
    private static final String URL = BASE_URL + "/" + SERVICE + "/" + METHOD;

    @AfterEach
    void clearMocks() {
        TestCodenameOneImplementation.getInstance().clearNetworkMocks();
    }

    // A minimal real codec: a message is a single proto3 string field #1.
    // Stateless, as ProtoCodec requires.
    private static final ProtoCodec<String> STRING_CODEC = new ProtoCodec<String>() {
        public void write(ProtoWriter out, String value) throws IOException {
            out.writeString(1, value);
        }

        public String read(ProtoReader in) throws IOException {
            String result = "";
            int tag;
            while ((tag = in.readTag()) != 0) {
                if ((tag >>> 3) == 1) {
                    result = in.readString();
                } else {
                    in.skipField(tag);
                }
            }
            return result;
        }
    };

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /** Builds a length-prefixed data frame (flags byte + be32 length + payload). */
    private static byte[] dataFrame(byte[] payload) {
        byte[] out = new byte[5 + payload.length];
        out[0] = 0;
        out[1] = (byte) ((payload.length >>> 24) & 0xFF);
        out[2] = (byte) ((payload.length >>> 16) & 0xFF);
        out[3] = (byte) ((payload.length >>> 8) & 0xFF);
        out[4] = (byte) (payload.length & 0xFF);
        System.arraycopy(payload, 0, out, 5, payload.length);
        return out;
    }

    /** Builds a trailer frame (flags 0x80 + be32 length + trailer text). */
    private static byte[] trailerFrame(String trailer) {
        byte[] body = utf8(trailer);
        byte[] out = new byte[5 + body.length];
        out[0] = (byte) GrpcWeb.FLAG_TRAILER;
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

    /** Encodes a string as the body of a single-string proto message. */
    private static byte[] encodeStringMessage(String s) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        STRING_CODEC.write(new ProtoWriter(b), s);
        return b.toByteArray();
    }

    // ---- frame() -----------------------------------------------------

    @Test
    void frameWritesFlagsLengthAndPayload() throws IOException {
        byte[] framed = GrpcWeb.frame("hi", STRING_CODEC);
        byte[] body = encodeStringMessage("hi");
        // 5-byte header + the encoded body.
        assertEquals(5 + body.length, framed.length);
        assertEquals(0, framed[0] & 0xFF, "data frame flag byte must be 0");
        int len = ((framed[1] & 0xFF) << 24) | ((framed[2] & 0xFF) << 16)
                | ((framed[3] & 0xFF) << 8) | (framed[4] & 0xFF);
        assertEquals(body.length, len, "be32 length prefix must equal payload length");
        for (int i = 0; i < body.length; i++) {
            assertEquals(body[i], framed[5 + i], "payload byte " + i);
        }
    }

    @Test
    void frameOfEmptyMessageIsBareFiveByteHeader() throws IOException {
        // proto3 omits a default/empty string, so the body is zero bytes.
        byte[] framed = GrpcWeb.frame("", STRING_CODEC);
        assertArrayEquals(new byte[]{0, 0, 0, 0, 0}, framed);
    }

    @Test
    void frameRoundTripsThroughDecode() throws IOException {
        byte[] framed = GrpcWeb.frame("hello", STRING_CODEC);
        // A real server response = a data frame + a trailer frame.
        byte[] response = concat(framed, trailerFrame("grpc-status:0\r\n"));
        GrpcResponse<String> r = GrpcWeb.decode(response, 200, STRING_CODEC);
        assertTrue(r.isOk());
        assertEquals("hello", r.getResponseData());
    }

    // ---- decode() ----------------------------------------------------

    @Test
    void decodeNullBodyIsTransportFailure() {
        GrpcResponse<String> r = GrpcWeb.decode(null, 200, STRING_CODEC);
        assertEquals(GrpcResponse.STATUS_TRANSPORT_FAILURE, r.getResponseCode());
        assertEquals(200, r.getHttpCode());
        assertNull(r.getResponseData());
        assertEquals("Empty response body", r.getResponseErrorMessage());
    }

    @Test
    void decodeEmptyBodyIsTransportFailure() {
        GrpcResponse<String> r = GrpcWeb.decode(new byte[0], 200, STRING_CODEC);
        assertEquals(GrpcResponse.STATUS_TRANSPORT_FAILURE, r.getResponseCode());
    }

    @Test
    void decodeSuccessParsesPayloadAndStatusZero() throws IOException {
        byte[] data = dataFrame(encodeStringMessage("world"));
        byte[] response = concat(data, trailerFrame("grpc-status:0\r\n"));
        GrpcResponse<String> r = GrpcWeb.decode(response, 200, STRING_CODEC);
        assertTrue(r.isOk());
        assertEquals(GrpcResponse.STATUS_OK, r.getResponseCode());
        assertEquals("world", r.getResponseData());
        assertNull(r.getResponseErrorMessage());
    }

    @Test
    void decodeNonZeroStatusSurfacesErrorAndDropsPayload() throws IOException {
        byte[] data = dataFrame(encodeStringMessage("ignored"));
        byte[] response = concat(data,
                trailerFrame("grpc-status:5\r\ngrpc-message:Not found\r\n"));
        GrpcResponse<String> r = GrpcWeb.decode(response, 200, STRING_CODEC);
        assertFalse(r.isOk());
        assertEquals(GrpcResponse.STATUS_NOT_FOUND, r.getResponseCode());
        assertEquals("Not found", r.getResponseErrorMessage());
        // The payload is discarded on a non-OK status.
        assertNull(r.getResponseData());
    }

    @Test
    void decodeTrailerOnlyResponseSucceedsWithEmptyMessage() {
        byte[] response = trailerFrame("grpc-status:0\r\n");
        GrpcResponse<String> r = GrpcWeb.decode(response, 200, STRING_CODEC);
        assertTrue(r.isOk());
        // No data frame -> the codec reads an empty buffer -> default string.
        assertEquals("", r.getResponseData());
    }

    @Test
    void decodeTrailerWithLfOnlyLineEndingsParses() {
        byte[] response = trailerFrame("grpc-status:7\ngrpc-message:Denied\n");
        GrpcResponse<String> r = GrpcWeb.decode(response, 200, STRING_CODEC);
        assertEquals(GrpcResponse.STATUS_PERMISSION_DENIED, r.getResponseCode());
        assertEquals("Denied", r.getResponseErrorMessage());
    }

    @Test
    void decodeTrailerHeaderMatchingIsCaseInsensitive() {
        byte[] response = trailerFrame("Grpc-Status:13\r\nGrpc-Message:boom\r\n");
        GrpcResponse<String> r = GrpcWeb.decode(response, 200, STRING_CODEC);
        assertEquals(GrpcResponse.STATUS_INTERNAL, r.getResponseCode());
        assertEquals("boom", r.getResponseErrorMessage());
    }

    @Test
    void decodeMissingTrailerIsTransportFailure() throws IOException {
        // Data frame only, no trailer frame at all.
        byte[] response = dataFrame(encodeStringMessage("lonely"));
        GrpcResponse<String> r = GrpcWeb.decode(response, 200, STRING_CODEC);
        assertEquals(GrpcResponse.STATUS_TRANSPORT_FAILURE, r.getResponseCode());
        assertTrue(r.getResponseErrorMessage().contains("missing the trailer"));
    }

    @Test
    void decodeMissingGrpcStatusHeaderIsUnknown() {
        // Trailer frame present but without a grpc-status line.
        byte[] response = trailerFrame("grpc-message:weird\r\n");
        GrpcResponse<String> r = GrpcWeb.decode(response, 200, STRING_CODEC);
        assertEquals(GrpcResponse.STATUS_UNKNOWN, r.getResponseCode());
    }

    @Test
    void decodeNonNumericStatusIsUnknown() {
        byte[] response = trailerFrame("grpc-status:notanumber\r\n");
        GrpcResponse<String> r = GrpcWeb.decode(response, 200, STRING_CODEC);
        assertEquals(GrpcResponse.STATUS_UNKNOWN, r.getResponseCode());
    }

    @Test
    void decodeTruncatedFrameIsTransportFailure() {
        // Header claims 10 payload bytes but only 2 are present.
        byte[] response = new byte[]{0, 0, 0, 0, 10, 1, 2};
        GrpcResponse<String> r = GrpcWeb.decode(response, 200, STRING_CODEC);
        assertEquals(GrpcResponse.STATUS_TRANSPORT_FAILURE, r.getResponseCode());
        assertTrue(r.getResponseErrorMessage().contains("Truncated"));
    }

    @Test
    void decodeConcatenatesMultipleDataFrames() throws IOException {
        // Two data frames whose payloads concatenate to one proto message.
        byte[] full = encodeStringMessage("splitme");
        int half = full.length / 2;
        byte[] firstHalf = new byte[half];
        byte[] secondHalf = new byte[full.length - half];
        System.arraycopy(full, 0, firstHalf, 0, half);
        System.arraycopy(full, half, secondHalf, 0, secondHalf.length);
        byte[] response = concat(concat(dataFrame(firstHalf), dataFrame(secondHalf)),
                trailerFrame("grpc-status:0\r\n"));
        GrpcResponse<String> r = GrpcWeb.decode(response, 200, STRING_CODEC);
        assertTrue(r.isOk());
        assertEquals("splitme", r.getResponseData());
    }

    // ---- invokeUnary() guards + HTTP path ----------------------------

    @Test
    void invokeUnaryRejectsNullCallback() {
        assertThrows(IllegalArgumentException.class, () -> GrpcWeb.<String, String>invokeUnary(
                BASE_URL, SERVICE, METHOD, null, "req", STRING_CODEC, STRING_CODEC, null));
    }

    @Test
    void invokeUnaryDeliversDecodedResponseOnSuccess() throws IOException {
        byte[] body = concat(dataFrame(encodeStringMessage("pong")),
                trailerFrame("grpc-status:0\r\n"));
        TestCodenameOneImplementation.getInstance()
                .addNetworkMockResponse(URL, 200, "OK", body);

        GrpcResponse<String> r = await("ping").get();
        assertTrue(r.isOk());
        assertEquals("pong", r.getResponseData());
        assertEquals(200, r.getHttpCode());
    }

    @Test
    void invokeUnarySurfacesGrpcStatusFromTrailer() throws IOException {
        byte[] body = concat(dataFrame(encodeStringMessage("x")),
                trailerFrame("grpc-status:16\r\ngrpc-message:no auth\r\n"));
        TestCodenameOneImplementation.getInstance()
                .addNetworkMockResponse(URL, 200, "OK", body);

        GrpcResponse<String> r = await("ping").get();
        assertEquals(GrpcResponse.STATUS_UNAUTHENTICATED, r.getResponseCode());
        assertEquals("no auth", r.getResponseErrorMessage());
    }

    @Test
    void invokeUnaryReportsTransportFailureOnHttpError() {
        // Non-2xx from the proxy itself -> handleErrorResponseCode ->
        // STATUS_TRANSPORT_FAILURE carrying the HTTP code.
        TestCodenameOneImplementation.getInstance()
                .addNetworkMockResponse(URL, 502, "Bad Gateway", utf8("upstream down"));

        GrpcResponse<String> r = await("ping").get();
        assertEquals(GrpcResponse.STATUS_TRANSPORT_FAILURE, r.getResponseCode());
        assertEquals(502, r.getHttpCode());
    }

    @Test
    void invokeUnaryReportsTransportFailureOnEmptyBody() {
        // 200 but an empty body has no trailer frame -> decode() flags it.
        TestCodenameOneImplementation.getInstance()
                .addNetworkMockResponse(URL, 200, "OK", new byte[0]);

        GrpcResponse<String> r = await("ping").get();
        assertEquals(GrpcResponse.STATUS_TRANSPORT_FAILURE, r.getResponseCode());
    }

    /** Fires invokeUnary and blocks (driving the EDT) until the callback runs. */
    private Holder await(String request) {
        final AtomicReference<GrpcResponse<String>> ref =
                new AtomicReference<GrpcResponse<String>>();
        final CountDownLatch latch = new CountDownLatch(1);
        GrpcWeb.<String, String>invokeUnary(BASE_URL, SERVICE, METHOD, null,
                request, STRING_CODEC, STRING_CODEC,
                new OnComplete<GrpcResponse<String>>() {
                    public void completed(GrpcResponse<String> v) {
                        ref.set(v);
                        latch.countDown();
                    }
                });
        int budget = 20000;
        while (latch.getCount() > 0 && budget > 0) {
            DisplayTest.flushEdt();
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            budget -= 10;
        }
        assertEquals(0, latch.getCount(), "gRPC callback did not fire within the timeout");
        return new Holder(ref.get());
    }

    private static final class Holder {
        private final GrpcResponse<String> value;

        Holder(GrpcResponse<String> value) {
            this.value = value;
        }

        GrpcResponse<String> get() {
            assertNotNull(value, "callback delivered a null response");
            return value;
        }
    }
}
