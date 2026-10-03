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
package com.codename1.tools.javac;

import com.codename1.tools.translator.classfile.ClassReader;
import com.codename1.tools.translator.classfile.ClassVisitor;
import com.codename1.tools.translator.classfile.FieldVisitor;
import com.codename1.tools.translator.classfile.MethodVisitor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A library of API stubs: class files with the code and debug information
 * removed, packed into one file. It is what the Playground ships so that the
 * compiler (and the incremental JavaScript translator) running in the browser can
 * see the API of the classes the running VM contains without downloading them.
 *
 * <p>A stub keeps what compiling against a class and linking to it need: access
 * flags, supertypes, generic signatures, fields (with constant values), methods
 * (with thrown exceptions), the {@code <clinit>} declaration, inner-class entries
 * and record components. It drops method bodies, private members (but not a
 * record's components, which are its private fields), synthetic and bridge
 * members, annotations and debug attributes.
 *
 * <p>File format: {@code "CN1STUB1"}, a u4 class count, then per class a u2 name
 * length, the internal name (UTF-8, so an ASCII name is one byte a character), a u4
 * length and the stub class file.
 */
public final class StubLibrary implements ClassLibrary {
    private static final String MAGIC = "CN1STUB1";

    private final byte[] data;
    private final Map<String, int[]> index = new HashMap<String, int[]>();
    private final List<String> names = new ArrayList<String>();

    /** Reads a packed library. */
    public StubLibrary(byte[] data) {
        this.data = data;
        for (int i = 0; i < MAGIC.length(); i++) {
            if (data.length < MAGIC.length() || data[i] != MAGIC.charAt(i)) {
                throw new IllegalArgumentException("not a stub library");
            }
        }
        int p = MAGIC.length();
        int count = u4(data, p);
        p += 4;
        for (int i = 0; i < count; i++) {
            int nameLen = (data[p] & 0xFF) << 8 | data[p + 1] & 0xFF;
            p += 2;
            String name = decodeUtf8(data, p, nameLen);
            p += nameLen;
            int len = u4(data, p);
            p += 4;
            index.put(name, new int[]{p, len});
            names.add(name);
            p += len;
        }
    }

    private static int u4(byte[] b, int p) {
        return (b[p] & 0xFF) << 24 | (b[p + 1] & 0xFF) << 16 | (b[p + 2] & 0xFF) << 8 | b[p + 3] & 0xFF;
    }

    @Override
    public byte[] classBytes(String internalName) {
        int[] e = index.get(internalName);
        if (e == null) {
            return null;
        }
        byte[] out = new byte[e[1]];
        System.arraycopy(data, e[0], out, 0, e[1]);
        return out;
    }

    /** Every class in the library, in packing order. */
    public List<String> classNames() {
        return names;
    }

    public int size() {
        return names.size();
    }

    /** UTF-8 by hand: the compiler is translated to run on ParparVM, so no charset lookup. */
    static byte[] encodeUtf8(String s) {
        ByteBuf b = new ByteBuf(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            if (Character.isHighSurrogate((char) c) && i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1))) {
                c = Character.toCodePoint((char) c, s.charAt(++i));
            }
            if (c < 0x80) {
                b.u1(c);
            } else if (c < 0x800) {
                b.u1(0xC0 | c >> 6).u1(0x80 | c & 0x3F);
            } else if (c < 0x10000) {
                b.u1(0xE0 | c >> 12).u1(0x80 | c >> 6 & 0x3F).u1(0x80 | c & 0x3F);
            } else {
                b.u1(0xF0 | c >> 18).u1(0x80 | c >> 12 & 0x3F).u1(0x80 | c >> 6 & 0x3F).u1(0x80 | c & 0x3F);
            }
        }
        return b.toByteArray();
    }

    static String decodeUtf8(byte[] d, int p, int len) {
        StringBuilder out = new StringBuilder(len);
        int end = p + len;
        while (p < end) {
            int b0 = d[p++] & 0xFF;
            if (b0 < 0x80) {
                out.append((char) b0);
            } else if (b0 < 0xE0) {
                out.append((char) ((b0 & 0x1F) << 6 | d[p++] & 0x3F));
            } else if (b0 < 0xF0) {
                out.append((char) ((b0 & 0x0F) << 12 | (d[p++] & 0x3F) << 6 | d[p++] & 0x3F));
            } else {
                int cp = (b0 & 0x07) << 18 | (d[p++] & 0x3F) << 12 | (d[p++] & 0x3F) << 6 | d[p++] & 0x3F;
                out.append(Character.highSurrogate(cp)).append(Character.lowSurrogate(cp));
            }
        }
        return out.toString();
    }

    // ------------------------------------------------------------------ writing

    /** Packs stubs (internal name to stub class file) into the library format. */
    public static byte[] pack(Map<String, byte[]> stubs) {
        ByteBuf b = new ByteBuf(1 << 20);
        for (int i = 0; i < MAGIC.length(); i++) {
            b.u1(MAGIC.charAt(i));
        }
        b.u4(stubs.size());
        for (Map.Entry<String, byte[]> e : stubs.entrySet()) {
            // UTF-8: a Java class may be named in any script, and one byte a character
            // would truncate everything above U+00FF.
            byte[] n = encodeUtf8(e.getKey());
            b.u2(n.length);
            b.bytes(n, 0, n.length);
            b.u4(e.getValue().length);
            b.bytes(e.getValue(), 0, e.getValue().length);
        }
        return b.toByteArray();
    }

    /** The stub of one class file (see the class comment for what it keeps). */
    public static byte[] stub(byte[] classFile) {
        final ConstPool pool = new ConstPool();
        final int[] header = new int[4];
        final String[] names = new String[3];
        final List<String> itfs = new ArrayList<String>();
        final List<ByteBuf> fields = new ArrayList<ByteBuf>();
        final List<ByteBuf> methods = new ArrayList<ByteBuf>();
        final List<String[]> inner = new ArrayList<String[]>();
        final List<String[]> recordComponents = new ArrayList<String[]>();
        final String[] outer = new String[3];
        new ClassReader(classFile).accept(new ClassVisitor() {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                header[0] = version;
                header[1] = access;
                names[0] = name;
                names[1] = superName;
                names[2] = signature;
                if (interfaces != null) {
                    for (String i : interfaces) {
                        itfs.add(i);
                    }
                }
            }

            @Override
            public void visitOuterClass(String owner, String name, String desc) {
                outer[0] = owner;
                outer[1] = name;
                outer[2] = desc;
            }

            @Override
            public void visitInnerClass(String name, String outerName, String innerName, int access) {
                inner.add(new String[]{name, outerName, innerName, Integer.toString(access)});
            }

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                boolean record = (header[1] & 0x10000) != 0;
                if ((access & Symbol.ACC_SYNTHETIC) != 0) {
                    return null;
                }
                if ((access & Symbol.ACC_PRIVATE) != 0 && !(record && (access & Symbol.ACC_STATIC) == 0)) {
                    return null;
                }
                if (record && (access & Symbol.ACC_STATIC) == 0) {
                    recordComponents.add(new String[]{name, desc, signature});
                }
                ByteBuf f = new ByteBuf();
                f.u2(access & 0xFFFF).u2(pool.utf8(name)).u2(pool.utf8(desc));
                int attrs = (signature != null ? 1 : 0) + (value != null ? 1 : 0);
                f.u2(attrs);
                if (signature != null) {
                    f.u2(pool.utf8("Signature")).u4(2).u2(pool.utf8(signature));
                }
                if (value != null) {
                    f.u2(pool.utf8("ConstantValue")).u4(2).u2(constant(pool, value));
                }
                fields.add(f);
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if ((access & (Symbol.ACC_SYNTHETIC | Symbol.ACC_BRIDGE)) != 0 && !"<clinit>".equals(name)) {
                    return null;
                }
                if ((access & Symbol.ACC_PRIVATE) != 0) {
                    return null;
                }
                ByteBuf m = new ByteBuf();
                m.u2(access & 0xFFFF).u2(pool.utf8(name)).u2(pool.utf8(desc));
                int attrs = (signature != null ? 1 : 0) + (exceptions != null && exceptions.length > 0 ? 1 : 0);
                m.u2(attrs);
                if (signature != null) {
                    m.u2(pool.utf8("Signature")).u4(2).u2(pool.utf8(signature));
                }
                if (exceptions != null && exceptions.length > 0) {
                    m.u2(pool.utf8("Exceptions")).u4(2 + 2 * exceptions.length).u2(exceptions.length);
                    for (String e : exceptions) {
                        m.u2(pool.cls(e));
                    }
                }
                methods.add(m);
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        int thisIdx = pool.cls(names[0]);
        int superIdx = names[1] == null ? 0 : pool.cls(names[1]);
        int[] itfIdx = new int[itfs.size()];
        for (int i = 0; i < itfIdx.length; i++) {
            itfIdx[i] = pool.cls(itfs.get(i));
        }
        List<ByteBuf> attrs = new ArrayList<ByteBuf>();
        if (names[2] != null) {
            ByteBuf a = new ByteBuf();
            a.u2(pool.utf8("Signature")).u4(2).u2(pool.utf8(names[2]));
            attrs.add(a);
        }
        if (!inner.isEmpty()) {
            ByteBuf body = new ByteBuf();
            body.u2(inner.size());
            for (String[] e : inner) {
                body.u2(pool.cls(e[0]));
                body.u2(e[1] == null ? 0 : pool.cls(e[1]));
                body.u2(e[2] == null ? 0 : pool.utf8(e[2]));
                body.u2(Integer.parseInt(e[3]));
            }
            ByteBuf a = new ByteBuf();
            a.u2(pool.utf8("InnerClasses")).u4(body.length).bytes(body);
            attrs.add(a);
        }
        if (outer[0] != null) {
            ByteBuf a = new ByteBuf();
            int nt = outer[1] == null ? 0 : pool.nameAndType(outer[1], outer[2]);
            a.u2(pool.utf8("EnclosingMethod")).u4(4).u2(pool.cls(outer[0])).u2(nt);
            attrs.add(a);
        }
        if ((header[1] & 0x10000) != 0) {
            ByteBuf body = new ByteBuf();
            body.u2(recordComponents.size());
            for (String[] rc : recordComponents) {
                body.u2(pool.utf8(rc[0])).u2(pool.utf8(rc[1]));
                if (rc[2] != null) {
                    body.u2(1).u2(pool.utf8("Signature")).u4(2).u2(pool.utf8(rc[2]));
                } else {
                    body.u2(0);
                }
            }
            ByteBuf a = new ByteBuf();
            a.u2(pool.utf8("Record")).u4(body.length).bytes(body);
            attrs.add(a);
        }
        ByteBuf out = new ByteBuf(1024);
        out.u4(0xCAFEBABE).u2(header[0] >>> 16).u2(header[0] & 0xFFFF);
        out.u2(pool.size()).bytes(pool.buf);
        out.u2(header[1] & 0xFFFF).u2(thisIdx).u2(superIdx);
        out.u2(itfIdx.length);
        for (int i : itfIdx) {
            out.u2(i);
        }
        out.u2(fields.size());
        for (ByteBuf f : fields) {
            out.bytes(f);
        }
        out.u2(methods.size());
        for (ByteBuf m : methods) {
            out.bytes(m);
        }
        out.u2(attrs.size());
        for (ByteBuf a : attrs) {
            out.bytes(a);
        }
        return out.toByteArray();
    }

    private static int constant(ConstPool pool, Object v) {
        if (v instanceof Integer) {
            return pool.integer(((Integer) v).intValue());
        }
        if (v instanceof Long) {
            return pool.longConst(((Long) v).longValue());
        }
        if (v instanceof Float) {
            return pool.floatConst(((Float) v).floatValue());
        }
        if (v instanceof Double) {
            return pool.doubleConst(((Double) v).doubleValue());
        }
        return pool.string(String.valueOf(v));
    }
}
