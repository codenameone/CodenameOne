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

import dart.async.Stream;
import dart.runtime.Funcs;

/// Dart's {@code Converter<S, T>}, the base a user converter extends: subclasses
/// implement {@link #convert}; chunked conversion and {@link #bind} convert each
/// chunk on its own, as Dart's default {@code startChunkedConversion} does.
public abstract class Converter<S, T> implements dart.async.StreamTransformer<S, T> {

    public abstract T convert(S input);

    /// {@code startChunkedConversion(sink)}: Dart's default refuses, and a converter
    /// that supports chunking overrides it.
    public Sink<S> startChunkedConversion(Object sink) {
        throw new dart.core.UnsupportedError("This converter does not support chunked conversions: "
                + "Instance of '" + getClass().getSimpleName() + "'");
    }

    /// Dart's default bind goes through startChunkedConversion; a converter that does not
    /// override it converts each event on its own here instead of failing the stream.
    @Override
    public Stream<T> bind(Stream<S> stream) {
        return stream.map(new Funcs.Func1<S, T>() {
            @Override
            public T call(S v) {
                return convert(v);
            }
        });
    }

    /// {@code converter.fuse(other)}: this conversion, then {@code other}'s.
    public <R> Converter<S, R> fuse(final Converter<T, R> other) {
        final Converter<S, T> self = this;
        return new Converter<S, R>() {
            @Override
            public R convert(S input) {
                return other.convert(self.convert(input));
            }
        };
    }

    /// Dart's {@code Sink<T>}: add and close.
    public interface Sink<T> {
        void add(T chunk);

        void close();
    }
}
