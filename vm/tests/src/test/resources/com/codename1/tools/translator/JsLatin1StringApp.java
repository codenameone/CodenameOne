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
public class JsLatin1StringApp {
    public static int result;

    public static void main(String[] args) throws Exception {
        int mask = 0;
        // A String built at runtime from Latin-1 characters is stored as a byte[]
        // (JavaAPI's compact strings), and a Java byte is signed: the JS runtime
        // read 0xE7 back as U+FFE7 until it masked the byte. Literals were never
        // affected -- they arrive as native strings -- so every probe here builds
        // its String at runtime.
        char[] chars = new char[] {'C', 'a', 'f', '\u00e9'};
        String built = new String(chars);
        if (built.charAt(3) == '\u00e9') mask |= 1;
        if (built.equals("Caf\u00e9")) mask |= 2;
        if (built.hashCode() == "Caf\u00e9".hashCode()) mask |= 4;
        StringBuilder sb = new StringBuilder();
        sb.append('\u00e7');
        String one = sb.toString();
        if (one.length() == 1 && one.charAt(0) == '\u00e7') mask |= 8;
        // String.getBytes answered the encoder's unsigned 0..255 instead of Java's
        // signed bytes: 0xC3 must read as -61.
        byte[] utf8 = "\u00e9".getBytes("UTF-8");
        if (utf8.length == 2 && utf8[0] == (byte) 0xC3 && utf8[1] == (byte) 0xA9) mask |= 16;
        if (utf8[0] < 0) mask |= 32;
        if (new String(utf8, "UTF-8").equals("\u00e9")) mask |= 64;
        result = mask;
        System.exit(mask);
    }
}
