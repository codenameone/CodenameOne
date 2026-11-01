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

/// Dart's {@code Sink}: the add/close protocol alone. A program class that implements
/// {@code Sink<T>} implements this, so it owes neither {@code done} nor a close that
/// answers a future, which {@link StreamSink} (Dart's StreamSink and EventSink) has.
public interface Sink<T> {
    void add(T data);

    void close();

    /// A StreamSink (Dart's StreamSink, EventSink and IOSink) seen as the Sink it is in
    /// Dart. Java cannot make StreamSink extend this interface -- its close() answers a
    /// Future -- so the transpiler adapts at the point a sink reaches a Sink.
    static <T> Sink<T> of(final Object sink) {
        if (sink == null || sink instanceof Sink) {
            @SuppressWarnings("unchecked")
            Sink<T> same = (Sink<T>) sink;
            return same;
        }
        if (!(sink instanceof StreamSink) && sink instanceof StreamController) {
            return of(((StreamController<?>) sink).sink());
        }
        @SuppressWarnings("unchecked")
        final StreamSink<T> ss = (StreamSink<T>) sink;
        return new Sink<T>() {
            @Override
            public void add(T data) {
                ss.add(data);
            }

            @Override
            public void close() {
                ss.close();
            }
        };
    }
}
