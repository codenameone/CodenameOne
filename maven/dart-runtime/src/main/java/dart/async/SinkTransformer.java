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

import dart.runtime.Funcs;

/// The transformer {@code StreamTransformer.fromHandlers} builds.
final class SinkTransformer<S, T> implements StreamTransformer<S, T> {
    private final Funcs.VoidFunc2<S, StreamSink<T>> handleData;
    private final Object handleError;
    private final Funcs.VoidFunc1<StreamSink<T>> handleDone;

    SinkTransformer(Funcs.VoidFunc2<S, StreamSink<T>> handleData, Object handleError,
            Funcs.VoidFunc1<StreamSink<T>> handleDone) {
        this.handleData = handleData;
        this.handleError = handleError;
        this.handleDone = handleDone;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Stream<T> bind(Stream<S> stream) {
        final StreamController<T> out = new StreamController<T>();
        out.onListen(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                stream.listen(new Funcs.VoidFunc1<S>() {
                    @Override
                    public void call(S v) {
                        if (handleData != null) {
                            handleData.call(v, out);
                        } else {
                            out.add((T) v);
                        }
                    }
                }, new Funcs.VoidFunc1<Object>() {
                    @Override
                    public void call(Object e) {
                        if (handleError instanceof Funcs.VoidFunc3) {
                            ((Funcs.VoidFunc3<Object, Object, StreamSink<T>>) handleError).call(e, null, out);
                        } else {
                            out.addError(e);
                        }
                    }
                }, new Funcs.VoidFunc0() {
                    @Override
                    public void call() {
                        if (handleDone != null) {
                            handleDone.call(out);
                        } else {
                            out.close();
                        }
                    }
                });
            }
        });
        return out.stream();
    }
}
