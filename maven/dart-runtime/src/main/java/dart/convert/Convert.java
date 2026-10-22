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

import dart.core.DartList;
import dart.typed_data.Uint8List;

/** The top-level values and functions of {@code dart:convert}. */
public final class Convert {

    private Convert() {
    }

    public static final JsonCodec json = new JsonCodec();
    public static final ByteCodec utf8 = new ByteCodec(ByteCodec.UTF8);
    public static final ByteCodec ascii = new ByteCodec(ByteCodec.ASCII);
    public static final ByteCodec latin1 = new ByteCodec(ByteCodec.LATIN1);
    public static final ByteCodec base64 = new ByteCodec(ByteCodec.BASE64);
    public static final ByteCodec base64Url = new ByteCodec(ByteCodec.BASE64_URL);

    public static Object jsonDecode(String source) {
        return json.decode(source, null);
    }

    public static Object jsonDecode(String source, Object reviver) {
        return json.decode(source, reviver);
    }

    public static String jsonEncode(Object value) {
        return json.encode(value, null);
    }

    public static String jsonEncode(Object value, Object toEncodable) {
        return json.encode(value, toEncodable);
    }

    public static String base64Encode(Object bytes) {
        return ByteCodec.base64(ByteCodec.bytesOf(bytes), false);
    }

    public static String base64UrlEncode(Object bytes) {
        return ByteCodec.base64(ByteCodec.bytesOf(bytes), true);
    }

    public static Uint8List base64Decode(String source) {
        return Uint8List.fromBytes(ByteCodec.unbase64(source));
    }

    public static DartList<String> splitLines(String s) {
        return ByteCodec.splitLines(s);
    }
}
