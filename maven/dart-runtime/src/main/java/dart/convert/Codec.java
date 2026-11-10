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

/**
 * Dart's {@code Codec<S, T>}, as far as the byte codecs need it: a variable typed
 * {@code Codec<String, List<int>>} holding {@code utf8} reaches {@code encode},
 * {@code decode}, {@code encoder} and {@code decoder} through this interface.
 * A codec declared in Dart implements it too, with its own Converter subclasses as
 * encoder and decoder; {@code encode}/{@code decode} default to converting through
 * them, as Dart's do. {@link ByteCodec} is its own encoder and decoder and decides
 * direction by the input's type.
 *
 * @param <S> the decoded type
 * @param <T> the encoded type
 */
public interface Codec<S, T> {

    /** Dart's {@code Codec.encode}: {@code encoder.convert(input)} unless overridden. */
    @SuppressWarnings("unchecked")
    default Object encode(Object input) {
        return ((Converter<Object, Object>) (Converter<?, ?>) encoder()).convert(input);
    }

    /** Dart's {@code Codec.decode}: {@code decoder.convert(encoded)} unless overridden. */
    @SuppressWarnings("unchecked")
    default Object decode(Object encoded) {
        return ((Converter<Object, Object>) (Converter<?, ?>) decoder()).convert(encoded);
    }

    /// Dart's {@code Codec.encoder}. A Converter, as in Dart, so a codec written in Dart
    /// returns its own {@code Converter} subclass and the override is a covariant one.
    Converter<S, T> encoder();

    /// Dart's {@code Codec.decoder}, the reverse conversion.
    Converter<T, S> decoder();
}
