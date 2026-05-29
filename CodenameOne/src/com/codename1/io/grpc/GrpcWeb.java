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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.Data;
import com.codename1.ui.CN;
import com.codename1.util.OnComplete;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/// High-level gRPC-Web invoker used by generated `@GrpcClient`
/// implementations. Handles HTTP-level transport, gRPC-Web payload
/// framing (`flags + length + payload`), and trailer parsing
/// (`grpc-status`, `grpc-message`).
///
/// Wire details (from the upstream gRPC-Web spec):
///
/// - URL: `<baseUrl>/<service-fqn>/<method-name>` (no trailing slash).
/// - HTTP method: `POST`.
/// - Request `Content-Type`: `application/grpc-web+proto`.
/// - Request headers: `X-Grpc-Web: 1`, `X-User-Agent: grpc-web-cn1/<ver>`.
/// - Request body: `0x00 <length-be32> <payload>` -- one data frame.
/// - Response body: zero or more data frames (flags low bit 0)
///   followed by one trailer frame (flags high bit `0x80`) carrying
///   `grpc-status:<n>\r\ngrpc-message:<text>\r\n`.
///
/// All methods are static. Generated impls call [#invokeUnary] and
/// receive the parsed [GrpcResponse] on the supplied callback.
public final class GrpcWeb {

    /// Content type for binary gRPC-Web payloads. The text variant
    /// (`application/grpc-web-text`, base64-encoded) is not
    /// supported -- modern Envoy/gRPC-Web proxies all speak binary.
    public static final String CONTENT_TYPE = "application/grpc-web+proto";

    /// gRPC-Web frame flag set in the trailer frame's first byte.
    public static final int FLAG_TRAILER = 0x80;

    private GrpcWeb() {
    }

    /// Sends a unary gRPC-Web request and invokes `callback` with
    /// the decoded response (or a transport-failure marker).
    ///
    /// The `baseUrl` should not include a trailing slash; the
    /// `service` is the fully qualified service path
    /// (e.g. `helloworld.Greeter`) and `method` is the gRPC method
    /// name (e.g. `SayHello`). The resulting URL is
    /// `<baseUrl>/<service>/<method>`.
    public static <Req, Resp> void invokeUnary(
            String baseUrl,
            String service,
            String method,
            String bearerToken,
            Req request,
            ProtoCodec<Req> reqCodec,
            ProtoCodec<Resp> respCodec,
            final OnComplete<GrpcResponse<Resp>> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        byte[] framed;
        try {
            framed = frame(request, reqCodec);
        } catch (IOException ioe) {
            callback.completed(new GrpcResponse<Resp>(
                    GrpcResponse.STATUS_INTERNAL, 0, null,
                    "Failed to encode request: " + ioe.getMessage()));
            return;
        }

        String url = joinUrl(baseUrl, service, method);
        final GrpcConnection conn = new GrpcConnection(respCodec, callback);
        conn.setUrl(url);
        conn.setHttpMethod("POST");
        conn.setPost(true);
        conn.setContentType(CONTENT_TYPE);
        conn.addRequestHeader("X-Grpc-Web", "1");
        conn.addRequestHeader("Accept", CONTENT_TYPE);
        if (bearerToken != null && bearerToken.length() > 0) {
            conn.addRequestHeader("Authorization", bearerToken);
        }
        conn.setRequestBody(new ByteData(framed));
        CN.addToQueue(conn);
    }

    /// Wraps a serialised request message in a single gRPC-Web data
    /// frame: `[0x00][length-be32][payload]`. Public so unit tests
    /// can verify framing independently of the network path.
    public static <T> byte[] frame(T request, ProtoCodec<T> codec) throws IOException {
        ByteArrayOutputStream bodyBuf = new ByteArrayOutputStream();
        ProtoWriter w = new ProtoWriter(bodyBuf);
        codec.write(w, request);
        byte[] body = bodyBuf.toByteArray();
        byte[] out = new byte[5 + body.length];
        out[0] = 0; // data frame, no compression
        out[1] = (byte) ((body.length >>> 24) & 0xFF);
        out[2] = (byte) ((body.length >>> 16) & 0xFF);
        out[3] = (byte) ((body.length >>> 8) & 0xFF);
        out[4] = (byte) (body.length & 0xFF);
        System.arraycopy(body, 0, out, 5, body.length);
        return out;
    }

    /// Decodes a complete gRPC-Web response body. Iterates frames,
    /// concatenates data payloads, and parses the trailer frame for
    /// `grpc-status` / `grpc-message`. Public so callers can replay
    /// canned responses in unit tests.
    public static <Resp> GrpcResponse<Resp> decode(byte[] response, int httpCode,
                                                   ProtoCodec<Resp> respCodec) {
        if (response == null || response.length == 0) {
            return new GrpcResponse<Resp>(GrpcResponse.STATUS_TRANSPORT_FAILURE,
                    httpCode, null, "Empty response body");
        }
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        int status = GrpcResponse.STATUS_OK;
        String message = null;
        boolean sawTrailer = false;
        int pos = 0;
        while (pos + 5 <= response.length) {
            int flags = response[pos] & 0xFF;
            int len = ((response[pos + 1] & 0xFF) << 24)
                    | ((response[pos + 2] & 0xFF) << 16)
                    | ((response[pos + 3] & 0xFF) << 8)
                    | (response[pos + 4] & 0xFF);
            pos += 5;
            if (pos + len > response.length) {
                return new GrpcResponse<Resp>(GrpcResponse.STATUS_TRANSPORT_FAILURE,
                        httpCode, null, "Truncated gRPC-Web frame (need "
                                + len + " bytes, have " + (response.length - pos) + ")");
            }
            if ((flags & FLAG_TRAILER) != 0) {
                String trailer;
                try {
                    trailer = new String(response, pos, len, "UTF-8");
                } catch (java.io.UnsupportedEncodingException uee) {
                    trailer = "";
                }
                int[] parsed = parseTrailerStatus(trailer);
                status = parsed[0];
                message = trailerMessage(trailer);
                sawTrailer = true;
            } else {
                payload.write(response, pos, len);
            }
            pos += len;
        }
        if (!sawTrailer) {
            return new GrpcResponse<Resp>(GrpcResponse.STATUS_TRANSPORT_FAILURE,
                    httpCode, null,
                    "gRPC-Web response is missing the trailer frame");
        }
        if (status != GrpcResponse.STATUS_OK) {
            return new GrpcResponse<Resp>(status, httpCode, null, message);
        }
        Resp parsed;
        try {
            ProtoReader r = new ProtoReader(payload.toByteArray());
            parsed = respCodec.read(r);
        } catch (IOException ioe) {
            return new GrpcResponse<Resp>(GrpcResponse.STATUS_INTERNAL, httpCode, null,
                    "Failed to decode response: " + ioe.getMessage());
        }
        return new GrpcResponse<Resp>(GrpcResponse.STATUS_OK, httpCode, parsed, null);
    }

    private static int[] parseTrailerStatus(String trailer) {
        // Lines are CRLF-separated; the spec allows LF too.
        int idx = indexOfHeader(trailer, "grpc-status");
        if (idx < 0) return new int[]{GrpcResponse.STATUS_UNKNOWN};
        int eol = endOfLine(trailer, idx);
        String value = trailer.substring(idx, eol).trim();
        try {
            return new int[]{Integer.parseInt(value)};
        } catch (NumberFormatException nfe) {
            return new int[]{GrpcResponse.STATUS_UNKNOWN};
        }
    }

    private static String trailerMessage(String trailer) {
        int idx = indexOfHeader(trailer, "grpc-message");
        if (idx < 0) return null;
        int eol = endOfLine(trailer, idx);
        return trailer.substring(idx, eol).trim();
    }

    /// Returns the character index just past the `<name>:` prefix,
    /// or -1 if `name` is absent. Header names are matched
    /// case-insensitively per gRPC-Web spec.
    private static int indexOfHeader(String trailer, String name) {
        int i = 0;
        int n = trailer.length();
        int nameLen = name.length();
        while (i < n) {
            int lineStart = i;
            // Scan to end of line.
            int eol = i;
            while (eol < n && trailer.charAt(eol) != '\n' && trailer.charAt(eol) != '\r') {
                eol++;
            }
            int colon = trailer.indexOf(':', lineStart);
            if (colon >= 0 && colon < eol && (colon - lineStart) == nameLen) {
                if (trailer.regionMatches(true, lineStart, name, 0, nameLen)) {
                    return colon + 1;
                }
            }
            // Skip CRLF / LF.
            i = eol;
            if (i < n && trailer.charAt(i) == '\r') i++;
            if (i < n && trailer.charAt(i) == '\n') i++;
        }
        return -1;
    }

    private static int endOfLine(String s, int from) {
        int n = s.length();
        int i = from;
        while (i < n && s.charAt(i) != '\n' && s.charAt(i) != '\r') {
            i++;
        }
        return i;
    }

    private static String joinUrl(String baseUrl, String service, String method) {
        StringBuilder sb = new StringBuilder();
        sb.append(baseUrl == null ? "" : baseUrl);
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '/') {
            sb.setLength(sb.length() - 1);
        }
        sb.append('/').append(service).append('/').append(method);
        return sb.toString();
    }

    /// Body adapter so a raw `byte[]` can flow through
    /// `ConnectionRequest.setRequestBody(Data)` without an extra
    /// copy.
    private static final class ByteData implements Data {
        private final byte[] body;

        ByteData(byte[] body) {
            this.body = body;
        }

        @Override
        public void appendTo(OutputStream output) throws IOException {
            output.write(body);
        }

        @Override
        public long getSize() throws IOException {
            return body.length;
        }
    }

    /// Subclasses `ConnectionRequest` so we can suppress its default
    /// "non-2xx is an error" handling -- gRPC-Web always returns
    /// 200 OK and surfaces failures in the trailer, so a 4xx/5xx
    /// from the proxy itself is the only HTTP-level failure mode we
    /// need to translate.
    private static final class GrpcConnection extends ConnectionRequest {
        private final ProtoCodec respCodec;
        private final OnComplete callback;
        private boolean failed;
        private int failedCode;
        private String failedMessage;

        GrpcConnection(ProtoCodec respCodec, OnComplete callback) {
            this.respCodec = respCodec;
            this.callback = callback;
            // Disable framework's modal error dialog; we surface
            // failures via the callback.
            setFailSilently(true);
        }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            failed = true;
            failedCode = code;
            failedMessage = message;
            // Do not call super -- swallow the framework default
            // (which would post an error event); we report through
            // the callback in postResponse.
        }

        @Override
        protected void handleException(Exception err) {
            failed = true;
            failedCode = 0;
            failedMessage = err.getMessage();
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        protected void postResponse() {
            super.postResponse();
            if (failed) {
                callback.completed(new GrpcResponse(GrpcResponse.STATUS_TRANSPORT_FAILURE,
                        failedCode, null, failedMessage));
                return;
            }
            byte[] body = getResponseData();
            GrpcResponse parsed = decode(body, getResponseCode(), respCodec);
            callback.completed(parsed);
        }
    }

}
