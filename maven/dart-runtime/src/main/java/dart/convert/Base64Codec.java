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
package dart.convert;

import dart.typed_data.Uint8List;

/**
 * {@code base64} and {@code base64Url}, typed as Dart types them: {@code encode} answers a
 * String and {@code decode} bytes. The behaviour is {@link ByteCodec}'s.
 */
public final class Base64Codec extends ByteCodec {

    private final TextConverter encoder;
    private final BytesConverter decoder;

    Base64Codec(int kind) {
        super(kind);
        encoder = new TextConverter(kind);
        decoder = new BytesConverter(kind);
    }

    @Override
    public String encode(Object input) {
        return (String) super.encode(input);
    }

    @Override
    public Uint8List decode(Object input) {
        return (Uint8List) super.decode(input);
    }

    @Override
    public Uint8List decode(Object input, Boolean allowMalformed) {
        return (Uint8List) super.decode(input, allowMalformed);
    }

    @Override
    public TextConverter encoder() {
        return encoder;
    }

    @Override
    public BytesConverter decoder() {
        return decoder;
    }
}
