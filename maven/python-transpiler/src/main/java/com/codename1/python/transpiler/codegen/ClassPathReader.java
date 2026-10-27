/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.transpiler.codegen;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Reads the public shape of compiled classes -- access, supertypes, fields
/// and methods -- from a class path of jars and directories, and from the
/// running JDK for `java.*`. Only what the interop generator needs, and no
/// bytecode library: a class file's constant pool and member tables.
public final class ClassPathReader implements AutoCloseable {
    public static final int PUBLIC = 0x1;
    public static final int STATIC = 0x8;
    public static final int BRIDGE = 0x40;
    public static final int VARARGS = 0x80;
    public static final int INTERFACE = 0x200;
    public static final int ABSTRACT = 0x400;
    public static final int SYNTHETIC = 0x1000;

    /// One field or method.
    public static final class Member {
        public final int access;
        public final String name;
        public final String desc;

        Member(int access, String name, String desc) {
            this.access = access;
            this.name = name;
            this.desc = desc;
        }

        public boolean is(int flag) {
            return (access & flag) != 0;
        }
    }

    /// One class.
    public static final class ClassInfo {
        public final int access;
        public final String superName;
        public final List<String> interfaces;
        public final List<Member> fields;
        public final List<Member> methods;

        ClassInfo(int access, String superName, List<String> interfaces, List<Member> fields,
                List<Member> methods) {
            this.access = access;
            this.superName = superName;
            this.interfaces = interfaces;
            this.fields = fields;
            this.methods = methods;
        }

        public boolean is(int flag) {
            return (access & flag) != 0;
        }
    }

    private final List<Object> roots = new ArrayList<Object>();
    /// Whether the class path carries a class library of its own (an
    /// application's `java-runtime`, ParparVM's `JavaAPI`): then `java.*`
    /// resolves there alone, and an API the device lacks is reported at the
    /// import instead of being bridged from the host JDK.
    private final boolean ownClassLibrary;
    private final Map<String, ClassInfo> cache = new HashMap<String, ClassInfo>();
    private FileSystem jrt;
    private ZipFile rtJar;

    public ClassPathReader(List<File> classPath) throws IOException {
        for (File f : classPath) {
            if (f.isDirectory()) {
                roots.add(f);
            } else if (f.isFile()) {
                roots.add(new ZipFile(f));
            }
        }
        ownClassLibrary = fromRoots("java/lang/Object.class") != null;
    }

    /// The class with internal name `name` (`java/util/List`), or null.
    public ClassInfo find(String name) {
        if (cache.containsKey(name)) {
            return cache.get(name);
        }
        ClassInfo c = null;
        try {
            byte[] b = bytes(name + ".class");
            if (b != null) {
                c = parse(b);
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read class " + name + ": " + e.getMessage(), e);
        }
        cache.put(name, c);
        return c;
    }

    private byte[] bytes(String path) throws IOException {
        byte[] b = fromRoots(path);
        if (b != null) {
            return b;
        }
        if (!ownClassLibrary && (path.startsWith("java/") || path.startsWith("javax/"))) {
            return jdkClass(path);
        }
        return null;
    }

    private byte[] fromRoots(String path) throws IOException {
        for (Object r : roots) {
            if (r instanceof File) {
                File f = new File((File) r, path);
                if (f.isFile()) {
                    return Files.readAllBytes(f.toPath());
                }
            } else {
                ZipFile z = (ZipFile) r; // NOPMD CloseResource - a class path root, closed by close()
                ZipEntry e = z.getEntry(path);
                if (e != null) {
                    try (InputStream in = z.getInputStream(e)) {
                        return readAll(in);
                    }
                }
            }
        }
        return null;
    }

    /// A JDK class: from the module image on JDK 9 and newer, from `rt.jar`
    /// on JDK 8.
    private byte[] jdkClass(String path) throws IOException {
        if (jrt == null && rtJar == null) {
            File rt = new File(System.getProperty("java.home"), "lib/rt.jar");
            if (rt.isFile()) {
                rtJar = new ZipFile(rt);
            } else {
                jrt = FileSystems.getFileSystem(URI.create("jrt:/"));
            }
        }
        if (rtJar != null) {
            ZipEntry e = rtJar.getEntry(path);
            if (e == null) {
                return null;
            }
            try (InputStream in = rtJar.getInputStream(e)) {
                return readAll(in);
            }
        }
        Path p = jrt.getPath("modules", "java.base", path);
        return Files.isRegularFile(p) ? Files.readAllBytes(p) : null;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static ClassInfo parse(byte[] b) throws IOException {
        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(b));
        if (in.readInt() != 0xCAFEBABE) {
            throw new IOException("not a class file");
        }
        in.readUnsignedShort();
        in.readUnsignedShort();
        int n = in.readUnsignedShort();
        String[] utf = new String[n];
        int[] classIdx = new int[n];
        for (int i = 1; i < n; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1:
                    utf[i] = in.readUTF();
                    break;
                case 7:
                    classIdx[i] = in.readUnsignedShort();
                    break;
                case 8: case 16: case 19: case 20:
                    in.readUnsignedShort();
                    break;
                case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18:
                    in.readInt();
                    break;
                case 5: case 6:
                    in.readLong();
                    i++;
                    break;
                case 15:
                    in.readUnsignedByte();
                    in.readUnsignedShort();
                    break;
                default:
                    throw new IOException("bad constant pool tag " + tag);
            }
        }
        int access = in.readUnsignedShort();
        in.readUnsignedShort(); // this_class: the caller already knows the name
        int sup = in.readUnsignedShort();
        String superName = sup == 0 ? null : utf[classIdx[sup]];
        int ni = in.readUnsignedShort();
        List<String> interfaces = new ArrayList<String>();
        for (int i = 0; i < ni; i++) {
            interfaces.add(utf[classIdx[in.readUnsignedShort()]]);
        }
        List<Member> fields = members(in, utf);
        List<Member> methods = members(in, utf);
        return new ClassInfo(access, superName, interfaces, fields, methods);
    }

    private static List<Member> members(DataInputStream in, String[] utf) throws IOException {
        int n = in.readUnsignedShort();
        List<Member> r = new ArrayList<Member>(n);
        for (int i = 0; i < n; i++) {
            int access = in.readUnsignedShort();
            String name = utf[in.readUnsignedShort()];
            String desc = utf[in.readUnsignedShort()];
            int na = in.readUnsignedShort();
            for (int a = 0; a < na; a++) {
                in.readUnsignedShort();
                int len = in.readInt();
                in.readFully(new byte[len]);
            }
            r.add(new Member(access, name, desc));
        }
        return r;
    }

    @Override
    public void close() throws IOException {
        for (Object r : roots) {
            if (r instanceof ZipFile) {
                ((ZipFile) r).close();
            }
        }
        if (rtJar != null) {
            rtJar.close();
        }
    }
}
