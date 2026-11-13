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
public final class JsonDecoder extends Converter<String, Object> {
    private final Object reviver;

    public JsonDecoder() {
        this(null);
    }

    public JsonDecoder(Object reviver) {
        this.reviver = reviver;
    }


    @Override
    public Object convert(String input) {
        return new JsonCodec().decode(input, reviver);
    }

    /** {@code stream.transform(json.decoder)}: the whole text, decoded once it has all arrived. */
    @Override
    public dart.async.Stream<Object> bind(final dart.async.Stream<String> source) {
        // Errors in the text stream are forwarded; the whole text decodes at done.
        return dart.async.Forward.bind(source, new dart.runtime.Funcs.Func0<dart.async.Forward<String, Object>>() {
            @Override
            public dart.async.Forward<String, Object> call() {
                return new dart.async.Forward<String, Object>() {
                    private final StringBuilder text = new StringBuilder();

                    @Override
                    protected void data(String chunk) {
                        text.append(chunk);
                    }

                    @Override
                    protected void done() {
                        add(convert(text.toString()));
                        close();
                    }
                };
            }
        });
    }
}
