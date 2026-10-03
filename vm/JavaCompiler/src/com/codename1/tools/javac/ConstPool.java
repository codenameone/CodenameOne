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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The constant pool of a class being written. Entries are interned by content;
 * {@link #entry(int)} gives back what an index refers to, which the frame
 * computer needs to type ldc, field and invoke instructions.
 */
final class ConstPool {
    static final int UTF8 = 1;
    static final int INTEGER = 3;
    static final int FLOAT = 4;
    static final int LONG = 5;
    static final int DOUBLE = 6;
    static final int CLASS = 7;
    static final int STRING = 8;
    static final int FIELDREF = 9;
    static final int METHODREF = 10;
    static final int IMETHODREF = 11;
    static final int NAME_AND_TYPE = 12;
    static final int METHOD_HANDLE = 15;
    static final int METHOD_TYPE = 16;
    static final int INVOKE_DYNAMIC = 18;

    final ByteBuf buf = new ByteBuf(1024);
    private int count = 1;
    private final Map<String, Integer> index = new HashMap<String, Integer>();
    /** Per index: {tag, a, b, c} where a/b/c are the decoded strings (owner, name, descriptor) or values. */
    private final List<Object[]> entries = new ArrayList<Object[]>();
    private final ByteBuf bootstrap = new ByteBuf();
    private int bootstrapCount;
    private final Map<String, Integer> bootstrapIndex = new HashMap<String, Integer>();

    ConstPool() {
        entries.add(null);
    }

    int size() {
        return count;
    }

    Object[] entry(int i) {
        return entries.get(i);
    }

    private Integer lookup(String key) {
        return index.get(key);
    }

    private int add(String key, Object[] entry, int slots) {
        int i = count;
        index.put(key, Integer.valueOf(i));
        entries.add(entry);
        if (slots == 2) {
            entries.add(null);
        }
        count += slots;
        return i;
    }

    int utf8(String s) {
        String key = "U" + s;
        Integer i = lookup(key);
        if (i != null) {
            return i.intValue();
        }
        buf.u1(UTF8);
        int lenPos = buf.length;
        buf.u2(0);
        int start = buf.length;
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            if (c >= 1 && c <= 0x7F) {
                buf.u1(c);
            } else if (c <= 0x7FF) {
                buf.u1(0xC0 | c >> 6 & 0x1F);
                buf.u1(0x80 | c & 0x3F);
            } else {
                buf.u1(0xE0 | c >> 12 & 0x0F);
                buf.u1(0x80 | c >> 6 & 0x3F);
                buf.u1(0x80 | c & 0x3F);
            }
        }
        int len = buf.length - start;
        if (len > 65535) {
            throw new CompileError("constant string too long");
        }
        buf.put2(lenPos, len);
        return add(key, new Object[]{Integer.valueOf(UTF8), s}, 1);
    }

    int cls(String internalName) {
        String key = "C" + internalName;
        Integer i = lookup(key);
        if (i != null) {
            return i.intValue();
        }
        int n = utf8(internalName);
        buf.u1(CLASS).u2(n);
        return add(key, new Object[]{Integer.valueOf(CLASS), internalName}, 1);
    }

    int string(String s) {
        String key = "S" + s;
        Integer i = lookup(key);
        if (i != null) {
            return i.intValue();
        }
        int n = utf8(s);
        buf.u1(STRING).u2(n);
        return add(key, new Object[]{Integer.valueOf(STRING), s}, 1);
    }

    int integer(int v) {
        String key = "I" + v;
        Integer i = lookup(key);
        if (i != null) {
            return i.intValue();
        }
        buf.u1(INTEGER).u4(v);
        return add(key, new Object[]{Integer.valueOf(INTEGER), Integer.valueOf(v)}, 1);
    }

    int floatConst(float v) {
        int bits = Float.floatToRawIntBits(v);
        String key = "F" + bits;
        Integer i = lookup(key);
        if (i != null) {
            return i.intValue();
        }
        buf.u1(FLOAT).u4(bits);
        return add(key, new Object[]{Integer.valueOf(FLOAT), Float.valueOf(v)}, 1);
    }

    int longConst(long v) {
        String key = "J" + v;
        Integer i = lookup(key);
        if (i != null) {
            return i.intValue();
        }
        buf.u1(LONG).u4((int) (v >>> 32)).u4((int) v);
        return add(key, new Object[]{Integer.valueOf(LONG), Long.valueOf(v)}, 2);
    }

    int doubleConst(double v) {
        long bits = Double.doubleToRawLongBits(v);
        String key = "D" + bits;
        Integer i = lookup(key);
        if (i != null) {
            return i.intValue();
        }
        buf.u1(DOUBLE).u4((int) (bits >>> 32)).u4((int) bits);
        return add(key, new Object[]{Integer.valueOf(DOUBLE), Double.valueOf(v)}, 2);
    }

    int nameAndType(String name, String desc) {
        String key = "N" + name + ";" + desc;
        Integer i = lookup(key);
        if (i != null) {
            return i.intValue();
        }
        int n = utf8(name);
        int d = utf8(desc);
        buf.u1(NAME_AND_TYPE).u2(n).u2(d);
        return add(key, new Object[]{Integer.valueOf(NAME_AND_TYPE), name, desc}, 1);
    }

    int field(String owner, String name, String desc) {
        return member(FIELDREF, owner, name, desc);
    }

    int method(String owner, String name, String desc, boolean itf) {
        return member(itf ? IMETHODREF : METHODREF, owner, name, desc);
    }

    private int member(int tag, String owner, String name, String desc) {
        String key = "M" + tag + owner + "." + name + ":" + desc;
        Integer i = lookup(key);
        if (i != null) {
            return i.intValue();
        }
        int c = cls(owner);
        int nt = nameAndType(name, desc);
        buf.u1(tag).u2(c).u2(nt);
        return add(key, new Object[]{Integer.valueOf(tag), owner, name, desc}, 1);
    }

    int methodHandle(int kind, String owner, String name, String desc, boolean itf) {
        String key = "H" + kind + owner + "." + name + ":" + desc + itf;
        Integer i = lookup(key);
        if (i != null) {
            return i.intValue();
        }
        int ref = kind <= 4 ? field(owner, name, desc) : method(owner, name, desc, itf);
        buf.u1(METHOD_HANDLE).u1(kind).u2(ref);
        return add(key, new Object[]{Integer.valueOf(METHOD_HANDLE), owner, name, desc}, 1);
    }

    int methodType(String desc) {
        String key = "T" + desc;
        Integer i = lookup(key);
        if (i != null) {
            return i.intValue();
        }
        int d = utf8(desc);
        buf.u1(METHOD_TYPE).u2(d);
        return add(key, new Object[]{Integer.valueOf(METHOD_TYPE), desc}, 1);
    }

    /** Registers a bootstrap method entry (method handle index + static argument indexes). */
    int bootstrapMethod(int handle, int[] args) {
        StringBuilder k = new StringBuilder().append(handle);
        for (int a : args) {
            k.append(',').append(a);
        }
        Integer i = bootstrapIndex.get(k.toString());
        if (i != null) {
            return i.intValue();
        }
        bootstrap.u2(handle).u2(args.length);
        for (int a : args) {
            bootstrap.u2(a);
        }
        int idx = bootstrapCount++;
        bootstrapIndex.put(k.toString(), Integer.valueOf(idx));
        return idx;
    }

    int invokeDynamic(int bsm, String name, String desc) {
        String key = "Y" + bsm + name + ":" + desc;
        Integer i = lookup(key);
        if (i != null) {
            return i.intValue();
        }
        int nt = nameAndType(name, desc);
        buf.u1(INVOKE_DYNAMIC).u2(bsm).u2(nt);
        return add(key, new Object[]{Integer.valueOf(INVOKE_DYNAMIC), null, name, desc}, 1);
    }

    boolean hasBootstrapMethods() {
        return bootstrapCount > 0;
    }

    /** The BootstrapMethods attribute body (count + entries). */
    ByteBuf bootstrapAttribute() {
        ByteBuf b = new ByteBuf();
        b.u2(bootstrapCount);
        b.bytes(bootstrap);
        return b;
    }
}
