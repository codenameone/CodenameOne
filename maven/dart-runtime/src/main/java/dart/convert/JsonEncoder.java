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

/** {@code JsonEncoder} and {@code JsonEncoder.withIndent}. */
public final class JsonEncoder {
    private final Object toEncodable;
    private final String indent;

    public JsonEncoder() {
        this(null);
    }

    public JsonEncoder(Object toEncodable) {
        this(null, toEncodable);
    }

    private JsonEncoder(String indent, Object toEncodable) {
        this.indent = indent;
        this.toEncodable = toEncodable;
    }

    public static JsonEncoder withIndent(String indent) {
        return new JsonEncoder(indent, null);
    }

    public static JsonEncoder withIndent(String indent, Object toEncodable) {
        return new JsonEncoder(indent, toEncodable);
    }

    public String convert(Object value) {
        StringBuilder sb = new StringBuilder();
        JsonCodec.write(sb, value, toEncodable, indent, "", 0);
        return sb.toString();
    }
}
