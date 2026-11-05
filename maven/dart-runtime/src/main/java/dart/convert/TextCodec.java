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
 * {@code utf8}, {@code ascii} and {@code latin1}: the text codecs, typed as Dart types them
 * ({@code encode} answers bytes, {@code decode} a String) so a caller that hands the result
 * to a {@code List<int>} or String needs no cast. The behaviour is {@link ByteCodec}'s.
 */
public final class TextCodec extends ByteCodec {

    private final BytesConverter encoder;
    private final TextConverter decoder;

    TextCodec(int kind) {
        super(kind);
        encoder = new BytesConverter(kind);
        decoder = new TextConverter(kind);
    }

    @Override
    public Uint8List encode(Object input) {
        return (Uint8List) super.encode(input);
    }

    @Override
    public String decode(Object input) {
        return (String) super.decode(input);
    }

    @Override
    public String decode(Object input, Boolean allowMalformed) {
        return (String) super.decode(input, allowMalformed);
    }

    @Override
    public BytesConverter encoder() {
        return encoder;
    }

    @Override
    public TextConverter decoder() {
        return decoder;
    }
}
