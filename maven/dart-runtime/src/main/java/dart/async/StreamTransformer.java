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
package dart.async;

/**
 * Dart's {@code StreamTransformer<S, T>}: what {@link Stream#transform} applies. The
 * converters of dart:convert ({@code utf8.decoder}, {@code LineSplitter()},
 * {@code json.decoder}) implement it, so {@code response.transform(utf8.decoder)} works.
 */
public interface StreamTransformer<S, T> {
    Stream<T> bind(Stream<S> stream);

    /// {@code StreamTransformer.fromHandlers(handleData:, handleError:, handleDone:)}.
    static <S, T> StreamTransformer<S, T> fromHandlers(
            dart.runtime.Funcs.VoidFunc2<S, StreamSink<T>> handleData, Object handleError,
            dart.runtime.Funcs.VoidFunc1<StreamSink<T>> handleDone) {
        return new SinkTransformer<S, T>(handleData, handleError, handleDone);
    }

    /// The unnamed factory {@code StreamTransformer(onListen)}: each listen on a bound stream
    /// calls {@code onListen(source, cancelOnError)} and hands the listener's handlers to the
    /// subscription it returns, as Dart's {@code _StreamSubscriptionTransformer} does.
    static <S, T> StreamTransformer<S, T> $create(
            final dart.runtime.Funcs.Func2<Stream<S>, Boolean, StreamSubscription<T>> onListen) {
        return new StreamTransformer<S, T>() {
            @Override
            public Stream<T> bind(final Stream<S> source) {
                return new Stream<T>() {
                    @Override
                    public StreamSubscription<T> listen(dart.runtime.Funcs.VoidFunc1<T> onData, Object onError,
                            dart.runtime.Funcs.VoidFunc0 onDone, Boolean cancelOnError) {
                        StreamSubscription<T> sub = onListen.call(source,
                                Boolean.valueOf(cancelOnError != null && cancelOnError.booleanValue()));
                        sub.onData(onData);
                        sub.onError(onError);
                        sub.onDone(onDone);
                        return sub;
                    }
                };
            }
        };
    }
}
