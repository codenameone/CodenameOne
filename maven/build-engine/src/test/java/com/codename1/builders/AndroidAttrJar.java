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
package com.codename1.builders;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Writes a stand-in {@code android.jar} holding nothing but
 * {@code android.R$attr} and the attribute names given.
 *
 * <p>The class file is emitted byte by byte rather than compiled, so the test
 * that reads it needs no JDK compiler on the test JVM and never skips. A class
 * carrying only static fields needs no methods and no attributes, which is
 * what keeps this short.</p>
 */
final class AndroidAttrJar {

    private static final int MAJOR_VERSION = 50;
    private static final int ACC_PUBLIC_FINAL_SUPER = 0x0031;
    private static final int ACC_PUBLIC_STATIC_FINAL = 0x0019;
    private static final int CONSTANT_CLASS = 7;
    private static final int CONSTANT_UTF8 = 1;

    private AndroidAttrJar() {
    }

    static void write(File jar, String... attributeNames) throws IOException {
        JarOutputStream out = new JarOutputStream(new FileOutputStream(jar));
        try {
            out.putNextEntry(new JarEntry("android/R$attr.class"));
            out.write(attrClass(attributeNames));
            out.closeEntry();
        } finally {
            out.close();
        }
    }

    private static byte[] attrClass(String[] attributeNames) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(0xCAFEBABE);
        out.writeShort(0);
        out.writeShort(MAJOR_VERSION);

        // #1 "android/R$attr", #2 its class, #3 "java/lang/Object", #4 its
        // class, #5 the int descriptor, then one name per field.
        out.writeShort(6 + attributeNames.length);
        writeUtf8(out, "android/R$attr");
        writeClass(out, 1);
        writeUtf8(out, "java/lang/Object");
        writeClass(out, 3);
        writeUtf8(out, "I");
        for (String name : attributeNames) {
            writeUtf8(out, name);
        }

        out.writeShort(ACC_PUBLIC_FINAL_SUPER);
        out.writeShort(2);
        out.writeShort(4);
        out.writeShort(0);

        out.writeShort(attributeNames.length);
        for (int i = 0; i < attributeNames.length; i++) {
            out.writeShort(ACC_PUBLIC_STATIC_FINAL);
            out.writeShort(6 + i);
            out.writeShort(5);
            out.writeShort(0);
        }

        out.writeShort(0);
        out.writeShort(0);
        out.flush();
        return bytes.toByteArray();
    }

    private static void writeUtf8(DataOutputStream out, String value) throws IOException {
        out.writeByte(CONSTANT_UTF8);
        out.writeUTF(value);
    }

    private static void writeClass(DataOutputStream out, int nameIndex) throws IOException {
        out.writeByte(CONSTANT_CLASS);
        out.writeShort(nameIndex);
    }
}
