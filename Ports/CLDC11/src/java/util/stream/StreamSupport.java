/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package java.util.stream;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

/// Makes a [Stream] of a [Spliterator]: what `Collection.stream()` does, and
/// how a library turns any `Iterable` into a stream
/// (`StreamSupport.stream(iterable.spliterator(), false)`).
///
/// Streams here run sequentially; `parallel` is what `isParallel()`
/// reports and changes nothing else, which the JDK allows.
public final class StreamSupport {

    private StreamSupport() {
    }

    /// The elements of `spliterator`, read when a terminal operation asks
    /// for them.
    public static <T> Stream<T> stream(Spliterator<T> spliterator, boolean parallel) {
        if (spliterator == null) {
            throw new NullPointerException();
        }
        Stream<T> out = ObjPipeline.over(new Source<T>(spliterator, null));
        return parallel ? out.parallel() : out;
    }

    /// The elements of the spliterator `supplier` answers when a terminal
    /// operation first asks for one.
    public static <T> Stream<T> stream(Supplier<? extends Spliterator<T>> supplier, int characteristics,
                                       boolean parallel) {
        if (supplier == null) {
            throw new NullPointerException();
        }
        Stream<T> out = ObjPipeline.over(new Source<T>(null, supplier));
        return parallel ? out.parallel() : out;
    }

    /// The first stage of such a stream: one `tryAdvance` per element.
    private static final class Source<T> extends PullIterator<T> implements Consumer<T> {
        private Spliterator<T> spliterator;
        private final Supplier<? extends Spliterator<T>> supplier;

        Source(Spliterator<T> spliterator, Supplier<? extends Spliterator<T>> supplier) {
            this.spliterator = spliterator;
            this.supplier = supplier;
        }

        @Override
        public void accept(T t) {
            emit(t);
        }

        @Override
        boolean advance() {
            if (spliterator == null) {
                spliterator = supplier.get();
            }
            return spliterator.tryAdvance(this);
        }
    }
}
