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
 * Finds classes by internal name, reading library class files on demand, and
 * holds the types the language itself refers to (Object, String, the boxes...).
 */
final class Symtab {
    private final ClassLibrary library;
    private final Map<String, ClassSymbol> classes = new HashMap<String, ClassSymbol>();
    /** Names known not to exist, so failed lookups (on-demand imports) stay cheap. */
    private final Map<String, Boolean> missing = new HashMap<String, Boolean>();

    final ClassSymbol objectSym;
    final ClassSymbol stringSym;
    final Type.ClassType objectType;
    final Type.ClassType stringType;

    Symtab(ClassLibrary library) {
        this.library = library;
        ClassSymbol obj = lookup("java/lang/Object");
        if (obj == null) {
            obj = new ClassSymbol("java/lang/Object", Symbol.ACC_PUBLIC);
            classes.put(obj.internalName, obj);
        }
        objectSym = obj;
        objectType = obj.erasure();
        ClassSymbol str = lookup("java/lang/String");
        stringSym = str != null ? str : objectSym;
        stringType = stringSym.erasure();
    }

    /** The class with this internal name, or null. */
    ClassSymbol lookup(String internalName) {
        ClassSymbol c = classes.get(internalName);
        if (c != null) {
            return c;
        }
        if (missing.containsKey(internalName)) {
            return null;
        }
        byte[] bytes = library.classBytes(internalName);
        if (bytes == null) {
            missing.put(internalName, Boolean.TRUE);
            return null;
        }
        c = new ClassSymbol(internalName, 0);
        c.pendingBytes = bytes;
        c.symtab = this;
        classes.put(internalName, c);
        // Flags are cheap and needed before completion (interface? enum?), read the header now.
        c.complete();
        return c;
    }

    /** Registers a class declared in source. */
    void enter(ClassSymbol c) {
        classes.put(c.internalName, c);
        missing.remove(c.internalName);
    }

    ClassSymbol require(String internalName) {
        ClassSymbol c = lookup(internalName);
        if (c == null) {
            c = new ClassSymbol(internalName, Symbol.ACC_PUBLIC);
            c.setSuperclass(objectType);
            classes.put(internalName, c);
        }
        return c;
    }

    Type.ClassType type(String internalName) {
        return require(internalName).erasure();
    }

    // ------------------------------------------------------------------ boxing

    Type boxedType(Type prim) {
        switch (prim.tag) {
            case BOOLEAN: return type("java/lang/Boolean");
            case BYTE: return type("java/lang/Byte");
            case SHORT: return type("java/lang/Short");
            case CHAR: return type("java/lang/Character");
            case INT: return type("java/lang/Integer");
            case LONG: return type("java/lang/Long");
            case FLOAT: return type("java/lang/Float");
            case DOUBLE: return type("java/lang/Double");
            default: return prim;
        }
    }

    /** The primitive a box unboxes to, or null. */
    Type unboxedType(Type t) {
        if (t.tag != Type.Tag.CLASS) {
            return null;
        }
        String n = ((Type.ClassType) t).sym.internalName;
        switch (n) {
            case "java/lang/Boolean": return Type.BOOLEAN;
            case "java/lang/Byte": return Type.BYTE;
            case "java/lang/Short": return Type.SHORT;
            case "java/lang/Character": return Type.CHAR;
            case "java/lang/Integer": return Type.INT;
            case "java/lang/Long": return Type.LONG;
            case "java/lang/Float": return Type.FLOAT;
            case "java/lang/Double": return Type.DOUBLE;
            default: return null;
        }
    }

    // ------------------------------------------------------------------ class files

    /** What completion needs from a class file's header: no code, no debug data. */
    private static final class ClassHeader extends ClassVisitor {
        int access;
        String signature;
        String superName;
        String[] interfaces;
        final List<String[]> innerEntries = new ArrayList<String[]>();
        final List<Object[]> fieldInfos = new ArrayList<Object[]>();
        final List<Object[]> methodInfos = new ArrayList<Object[]>();

        @Override
        public void visit(int version, int access, String name, String signature, String sup, String[] itfs) {
            this.access = access;
            this.signature = signature;
            this.superName = sup;
            this.interfaces = itfs;
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            innerEntries.add(new String[]{name, outerName, innerName, Integer.toString(access)});
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            if ((access & Symbol.ACC_SYNTHETIC) == 0) {
                fieldInfos.add(new Object[]{Integer.valueOf(access), name, desc, signature, value});
            }
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if ((access & (Symbol.ACC_SYNTHETIC | Symbol.ACC_BRIDGE)) == 0 && !"<clinit>".equals(name)) {
                methodInfos.add(new Object[]{Integer.valueOf(access), name, desc, signature, exceptions});
            }
            return null;
        }
    }

    void completeFromClassFile(final ClassSymbol c, byte[] bytes) {
        ClassHeader h = new ClassHeader();
        new ClassReader(bytes).accept(h, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        c.flags = h.access & 0xFFFF | ((h.access & 0x10000) != 0 ? Symbol.RECORD : 0);
        final List<String[]> innerEntries = h.innerEntries;
        final List<Object[]> fieldInfos = h.fieldInfos;
        final List<Object[]> methodInfos = h.methodInfos;

        // Nesting: who encloses this class, and which classes it encloses.
        for (String[] e : innerEntries) {
            if (e[0].equals(c.internalName)) {
                if (e[1] != null) {
                    c.outer = lookup(e[1]);
                } else {
                    // Local or anonymous: the enclosing class is the name prefix.
                    int d = c.internalName.lastIndexOf('$');
                    c.outer = d > 0 ? lookup(c.internalName.substring(0, d)) : null;
                    c.local = e[2] != null;
                    c.anonymous = e[2] == null;
                }
                int access = Integer.parseInt(e[3]);
                c.hasOuterInstance = (access & Symbol.ACC_STATIC) == 0 && (access & Symbol.ACC_INTERFACE) == 0
                        && (c.flags & (Symbol.ACC_INTERFACE | Symbol.ACC_ENUM)) == 0 && (c.flags & Symbol.RECORD) == 0
                        && !c.anonymous && !c.local;
                c.flags = c.flags & ~0x7 | access & 0x7 | access & Symbol.ACC_STATIC;
            } else if (c.internalName.equals(e[1]) && e[2] != null) {
                ClassSymbol member = new ClassSymbol(e[0], 0);
                ClassSymbol existing = classes.get(e[0]);
                if (existing != null) {
                    member = existing;
                } else {
                    byte[] mb = library.classBytes(e[0]);
                    if (mb == null) {
                        continue;
                    }
                    member.pendingBytes = mb;
                    member.symtab = this;
                    classes.put(e[0], member);
                }
                c.memberClasses.put(e[2], member);
            }
        }

        SignatureParser sigs = new SignatureParser(this);
        if (h.signature != null) {
            sigs.classSignature(c, h.signature);
        } else {
            c.setSuperclass(h.superName == null ? null : type(h.superName));
            if (h.interfaces != null) {
                for (String i : h.interfaces) {
                    c.interfaces().add(type(i));
                }
            }
        }
        for (Object[] f : fieldInfos) {
            int access = ((Integer) f[0]).intValue();
            String name = (String) f[1];
            Type t = f[3] != null ? sigs.fieldSignature((String) f[3], c, null) : sigs.descriptorType((String) f[2]);
            VarSymbol v = new VarSymbol(name, access, t, VarSymbol.Kind.FIELD);
            v.owner = c;
            if (f[4] != null && (access & Symbol.ACC_FINAL) != 0 && (access & Symbol.ACC_STATIC) != 0) {
                v.constValue = constantFor(f[4], t);
            }
            c.fields.add(v);
        }
        for (Object[] m : methodInfos) {
            int access = ((Integer) m[0]).intValue();
            MethodSymbol ms = new MethodSymbol((String) m[1], access, c);
            ms.descriptor = (String) m[2];
            if (m[3] != null) {
                sigs.methodSignature(ms, (String) m[3], c);
                // A signature may omit synthetic leading parameters (enum ctors, inner ctors);
                // the descriptor is authoritative for arity.
                List<Type> descParams = sigs.descriptorParams((String) m[2]);
                if (descParams.size() != ms.params.size()) {
                    List<Type> merged = new ArrayList<Type>(descParams);
                    int skip = descParams.size() - ms.params.size();
                    if (skip > 0) {
                        for (int i = 0; i < ms.params.size(); i++) {
                            merged.set(skip + i, ms.params.get(i));
                        }
                    }
                    ms.params.clear();
                    ms.params.addAll(merged);
                }
            } else {
                ms.params.addAll(sigs.descriptorParams((String) m[2]));
                ms.returnType = sigs.descriptorType(((String) m[2]).substring(((String) m[2]).indexOf(')') + 1));
                if (m[4] != null) {
                    for (String ex : (String[]) m[4]) {
                        ms.thrown.add(type(ex));
                    }
                }
            }
            if (ms.isConstructor()) {
                stripSyntheticConstructorParams(c, ms);
            }
            if ((c.flags & Symbol.ACC_INTERFACE) != 0 && (access & (Symbol.ACC_ABSTRACT | Symbol.ACC_STATIC)) == 0
                    && (access & Symbol.ACC_PRIVATE) == 0) {
                ms.flags |= Symbol.DEFAULT_METHOD;
            }
            c.methods.add(ms);
        }
        if ((c.flags & Symbol.RECORD) != 0) {
            // Record components: the canonical constructor's parameters, named by private final fields.
            for (VarSymbol f : c.fields) {
                if ((f.flags & Symbol.ACC_STATIC) == 0) {
                    c.recordComponents.add(f);
                }
            }
        }
    }

    /**
     * A library constructor's descriptor carries the outer instance (inner classes) and
     * name/ordinal (enums); source calls never pass them, so drop them from the symbol.
     */
    private void stripSyntheticConstructorParams(ClassSymbol c, MethodSymbol ctor) {
        if ((c.flags & Symbol.ACC_ENUM) != 0 && ctor.params.size() >= 2
                && ctor.descriptor.startsWith("(Ljava/lang/String;I")) {
            ctor.params.remove(0);
            ctor.params.remove(0);
        } else if (c.hasOuterInstance && !ctor.params.isEmpty() && c.outer != null
                && ctor.descriptor.startsWith("(L" + c.outer.internalName + ";")) {
            ctor.params.remove(0);
        }
    }

    private static Object constantFor(Object value, Type t) {
        switch (t.tag) {
            case BOOLEAN: return Boolean.valueOf(((Integer) value).intValue() != 0);
            case CHAR: return Character.valueOf((char) ((Integer) value).intValue());
            case BYTE: return Integer.valueOf((byte) ((Integer) value).intValue());
            case SHORT: return Integer.valueOf((short) ((Integer) value).intValue());
            default: return value;
        }
    }
}
