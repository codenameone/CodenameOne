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

/// Dart's {@code StreamView}: a stream that forwards every event of the stream it
/// wraps, so a subclass (an HTTP body stream, for one) gets the whole Stream API.
public class StreamView<T> extends Stream<T> {

    private final Stream<T> source;
    private StreamSubscription<T> upstreamSub;

    public StreamView(Stream<T> source) {
        this.source = source;
    }

    @Override
    protected void onFirstListener() {
        final StreamView<T> self = this;
        upstreamSub = source.listen(new Funcs.VoidFunc1<T>() {
            @Override
            public void call(T v) {
                self.emit(v);
            }
        }, new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object e) {
                self.emitError(e);
            }
        }, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                self.emitDone();
            }
        });
        if (!hasListeners() && upstreamSub != null) {
            upstreamSub.cancel();
            upstreamSub = null;
        }
    }

    @Override
    protected void onPaused() {
        if (upstreamSub != null) {
            upstreamSub.pause();
        }
    }

    @Override
    protected void onResumed() {
        if (upstreamSub != null) {
            upstreamSub.resume();
        }
    }

    @Override
    protected void onNoListeners() {
        if (upstreamSub != null) {
            upstreamSub.cancel();
            upstreamSub = null;
        }
    }
}
