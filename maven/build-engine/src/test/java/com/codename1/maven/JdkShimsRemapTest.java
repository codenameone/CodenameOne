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
package com.codename1.maven;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/// The JDK stand-ins, end to end: source written against the JDK is compiled,
/// run, relocated by the remap step and run again, and has to answer the same.
public class JdkShimsRemapTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private RemapDifferential java8() throws Exception {
        return new RemapDifferential(tmp.newFolder(), 8);
    }

    @Test
    public void textIsEncodedAndDecodedByCharset() throws Exception {
        java8().same(""
                + "byte[] utf = \"caf\\u00e9 \\u20ac\".getBytes(StandardCharsets.UTF_8);\n"
                + "out.add(utf);\n"
                + "out.add(new String(utf, StandardCharsets.UTF_8));\n"
                + "out.add(new String(utf, 0, 3, StandardCharsets.US_ASCII));\n"
                + "out.add(new String(utf, 1, 4, StandardCharsets.ISO_8859_1).length());\n"
                + "out.add(StandardCharsets.UTF_8.name());\n"
                + "out.add(StandardCharsets.ISO_8859_1.toString());\n"
                + "out.add(Charset.forName(\"utf-8\").name());\n"
                + "out.add(Charset.forName(\"UTF8\") == StandardCharsets.UTF_8);\n"
                + "out.add(Charset.forName(\"latin1\").name());\n"
                + "out.add(Charset.forName(\"ascii\").name());\n"
                + "out.add(Charset.isSupported(\"UTF-8\"));\n"
                + "out.add(Charset.defaultCharset() != null);\n"
                + "Reader r = new InputStreamReader(new ByteArrayInputStream(utf), StandardCharsets.UTF_8);\n"
                + "StringBuilder sb = new StringBuilder();\n"
                + "for (int c = r.read(); c >= 0; c = r.read()) { sb.append((char) c); }\n"
                + "out.add(sb.toString());\n"
                + "ByteArrayOutputStream bytes = new ByteArrayOutputStream();\n"
                + "Writer w = new OutputStreamWriter(bytes, StandardCharsets.UTF_8);\n"
                + "w.write(\"na\\u00efve\");\n"
                + "w.close();\n"
                + "out.add(bytes.toByteArray());\n"
                + "out.add(bytes.toString(\"UTF-8\"));\n", 14);
    }
}
