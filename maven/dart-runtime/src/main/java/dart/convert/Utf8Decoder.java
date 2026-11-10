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

/// Dart's {@code Utf8Decoder}, as {@code const Utf8Decoder()} builds it: the same conversion
/// as {@code utf8.decoder}.
public final class Utf8Decoder extends Converter<Object, Object> {
    private boolean allowMalformed;

    public Utf8Decoder() {
        this(null);
    }

    public Utf8Decoder(Boolean allowMalformed) {
        this.allowMalformed = Boolean.TRUE.equals(allowMalformed);
    }

    /// The {@code allowMalformed:} constructor argument, as the transpiler passes a
    /// named argument of a const constructor.
    public Utf8Decoder allowMalformed(Boolean allow) {
        this.allowMalformed = Boolean.TRUE.equals(allow);
        return this;
    }

    @Override
    public Object convert(Object input) {
        return Convert.utf8.decode(input, Boolean.valueOf(allowMalformed));
    }
}
