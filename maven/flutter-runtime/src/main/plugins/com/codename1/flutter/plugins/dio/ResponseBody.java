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
package com.codename1.flutter.plugins.dio;

import dart.async.Stream;
import dart.typed_data.Uint8List;

/// dio's `ResponseBody`: what `ResponseType.stream` delivers as `response.data`.
/// ConnectionRequest has already read the whole body when the response completes,
/// so the stream carries it as a single chunk; code written against
/// `data.stream.listen(...)` or `await for` still sees every byte, in order.
public class ResponseBody {
    private final Stream<Uint8List> stream;
    private final Long statusCode;
    private final Long contentLength;

    public ResponseBody(byte[] body, Long statusCode) {
        Uint8List bytes = Uint8List.fromBytes(body);
        this.stream = Stream.value(bytes);
        this.statusCode = statusCode;
        this.contentLength = Long.valueOf(bytes.length());
    }

    public Stream<Uint8List> stream() {
        return stream;
    }

    public Long statusCode() {
        return statusCode;
    }

    public Long contentLength() {
        return contentLength;
    }
}
