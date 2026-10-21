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
package dart.io;

import dart.async.Stream;
import dart.core.DartList;
import dart.typed_data.Uint8List;

/**
 * Dart's {@code HttpClientResponse}: a stream of byte chunks -- here the whole body as
 * one chunk, delivered once someone listens -- plus the status line and headers.
 */
public class HttpClientResponse extends Stream<Object> {

    private final long statusCode;
    private final String reasonPhrase;
    private final HttpHeaders headers;
    private final byte[] body;
    private boolean delivered;

    HttpClientResponse(long statusCode, String reasonPhrase, HttpHeaders headers, byte[] body) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase == null ? "" : reasonPhrase;
        this.headers = headers;
        this.body = body;
    }

    @Override
    protected void onFirstListener() {
        if (delivered) {
            return;
        }
        delivered = true;
        new dart.async.Timer(dart.core.Duration.zero, new dart.runtime.Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (body.length > 0) {
                    emit(Uint8List.fromBytes(body));
                }
                emitDone();
            }
        });
    }

    public long statusCode() {
        return statusCode;
    }

    public String reasonPhrase() {
        return reasonPhrase;
    }

    public HttpHeaders headers() {
        return headers;
    }

    /// Set when the body was gunzipped here: the advertised length is the compressed one.
    boolean uncompressed;

    /// The length the server declared up front (Content-Length), -1 when it declared none
    /// or the body was decompressed -- not the bytes buffered: a HEAD response has none.
    public long contentLength() {
        if (uncompressed) {
            return -1;
        }
        String v = headers.value(HttpHeaders.contentLengthHeader);
        if (v == null) {
            return -1;
        }
        v = v.trim();
        if (v.length() == 0 || v.length() > 18) {
            return -1;
        }
        long n = 0;
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            n = n * 10 + (c - '0');
        }
        return n;
    }

    public boolean isRedirect() {
        return statusCode >= 300 && statusCode < 400 && headers.value(HttpHeaders.locationHeader) != null;
    }

    public boolean persistentConnection() {
        return true;
    }

    public DartList<Object> redirects() {
        return new DartList<Object>();
    }

    public DartList<Object> cookies() {
        return new DartList<Object>();
    }

    /** Dart's {@code compressionState}; the platform has already decompressed the body. */
    public String compressionState() {
        return "decompressed";
    }
}
