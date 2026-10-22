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

/** {@code JsonDecoder}. */
public final class JsonDecoder implements dart.async.StreamTransformer<String, Object> {
    private final Object reviver;

    public JsonDecoder() {
        this(null);
    }

    public JsonDecoder(Object reviver) {
        this.reviver = reviver;
    }

    public Object convert(String input) {
        return new JsonCodec().decode(input, reviver);
    }

    /** {@code stream.transform(json.decoder)}: the whole text, decoded once it has all arrived. */
    @Override
    public dart.async.Stream<Object> bind(final dart.async.Stream<String> source) {
        return dart.async.AsyncStar.asyncStar(new dart.runtime.Funcs.VoidFunc1<dart.async.AsyncStar.Sink<Object>>() {
            @Override
            public void call(dart.async.AsyncStar.Sink<Object> sink) {
                sink.yield$(convert(dart.async.Await.await$(source.join(""))));
            }
        });
    }
}
