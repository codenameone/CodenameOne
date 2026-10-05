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
import java.util.List;

/**
 * Creates class symbols for source declarations and enters their headers and
 * members: supertypes, type parameters, fields and method signatures, plus the
 * members the language implies (default constructors, enum {@code values}/
 * {@code valueOf}, record fields, accessors and canonical constructors).
 */
final class Enter {
    /** Method markers for bodies {@link Gen} synthesizes. */
    static final int SYNTH_ENUM_VALUES = 1 << 24;
    static final int SYNTH_ENUM_VALUEOF = 1 << 25;
    static final int SYNTH_RECORD_ACCESSOR = 1 << 26;
    static final int SYNTH_RECORD_TOSTRING = 1 << 27;
    static final int SYNTH_RECORD_HASHCODE = 1 << 28;
    static final int SYNTH_RECORD_EQUALS = 1 << 29;
    static final int SYNTH_DEFAULT_CTOR = 1 << 30;

    private final Compiler compiler;
    final List<ClassSymbol> sourceClasses = new ArrayList<ClassSymbol>();

    Enter(Compiler compiler) {
        this.compiler = compiler;
    }

    void enterUnits(List<Tree.CompilationUnit> units) {
        for (Tree.CompilationUnit unit : units) {
            checkPublicTypeFileName(unit);
            String pkg = unit.packageName.replace('.', '/');
            for (Tree.ClassDecl decl : unit.types) {
                enterClass(decl, unit, null, pkg.isEmpty() ? decl.name : pkg + "/" + decl.name);
            }
        }
        for (int i = 0; i < sourceClasses.size(); i++) {
            enterHeader(sourceClasses.get(i));
        }
        for (int i = 0; i < sourceClasses.size(); i++) {
            checkCyclicInheritance(sourceClasses.get(i));
        }
        for (int i = 0; i < sourceClasses.size(); i++) {
            enterMembers(sourceClasses.get(i));
        }
    }

    /**
     * JLS 7.6: a public top-level type lives in the file named after it, so at most one per
     * file. A script is exempt -- its source name is a label, and the class it becomes is
     * generated.
     */
    private void checkPublicTypeFileName(Tree.CompilationUnit unit) {
        if (unit.source == null || compiler.scripts.containsKey(unit.source)) {
            return;
        }
        String file = unit.source.name;
        int slash = Math.max(file.lastIndexOf('/'), file.lastIndexOf('\\'));
        file = file.substring(slash + 1);
        if (!file.endsWith(".java")) {
            return;
        }
        String base = file.substring(0, file.length() - 5);
        for (Tree.ClassDecl decl : unit.types) {
            if (decl.mods != null && (decl.mods.flags & Symbol.ACC_PUBLIC) != 0 && !decl.name.equals(base)) {
                compiler.error(unit, decl.pos, "class " + decl.name + " is public, should be declared in a file named "
                        + decl.name + ".java");
            }
        }
    }

    ClassSymbol enterClass(Tree.ClassDecl decl, Tree.CompilationUnit unit, ClassSymbol outer, String internalName) {
        int flags = decl.mods == null ? 0 : decl.mods.flags & 0xFFFF;
        if (decl.mods != null) {
            flags |= decl.mods.flags & (JavaSourceParser.SEALED | JavaSourceParser.NON_SEALED);
        }
        switch (decl.kind) {
            case INTERFACE:
                flags |= Symbol.ACC_INTERFACE | Symbol.ACC_ABSTRACT;
                break;
            case ANNOTATION:
                flags |= Symbol.ACC_INTERFACE | Symbol.ACC_ABSTRACT | Symbol.ACC_ANNOTATION;
                break;
            case ENUM:
                flags |= Symbol.ACC_ENUM;
                if (!hasConstantBodies(decl)) {
                    flags |= Symbol.ACC_FINAL;
                }
                break;
            case RECORD:
                flags |= Symbol.RECORD | Symbol.ACC_FINAL;
                break;
            default:
                break;
        }
        ClassSymbol c = new ClassSymbol(internalName, flags);
        if (decl.local || decl.anonymous) {
            // Binary names of local classes carry a counter (Outer$1Local); the source name is the declared one.
            c.simpleName = decl.anonymous ? "" : decl.name;
        }
        c.decl = decl;
        c.unit = unit;
        c.outer = outer;
        c.local = decl.local;
        c.anonymous = decl.anonymous;
        if (outer != null) {
            if (outer.isInterface() || decl.kind != Tree.ClassKind.CLASS) {
                // Member interfaces, enums, records and members of interfaces are implicitly static.
                c.flags |= Symbol.ACC_STATIC;
                if (outer.isInterface()) {
                    c.flags |= Symbol.ACC_PUBLIC;
                }
            }
            c.hasOuterInstance = (c.flags & Symbol.ACC_STATIC) == 0 && decl.kind == Tree.ClassKind.CLASS;
            if (!decl.local && !decl.anonymous) {
                outer.memberClasses.put(decl.name, c);
            }
        }
        for (Tree.TypeParameter tp : decl.typeParams) {
            Type.TypeVar tv = new Type.TypeVar(tp.name);
            tp.tvar = tv;
            rawTypeParams(c).add(tv);
        }
        if (decl.mods != null) {
            checkClassModifiers(decl, c, outer);
        }
        compiler.symtab.enter(c);
        sourceClasses.add(c);
        for (Tree member : decl.members) {
            if (member instanceof Tree.ClassDecl) {
                Tree.ClassDecl inner = (Tree.ClassDecl) member;
                enterClass(inner, unit, c, internalName + "$" + inner.name);
            }
        }
        return c;
    }

    private static boolean hasConstantBodies(Tree.ClassDecl decl) {
        for (Tree m : decl.members) {
            if (m instanceof Tree.VarDef && ((Tree.VarDef) m).init instanceof Tree.NewClass
                    && ((Tree.NewClass) ((Tree.VarDef) m).init).body != null
                    && (((Tree.VarDef) m).mods.flags & Symbol.ACC_ENUM) != 0) {
                return true;
            }
        }
        return false;
    }

    private static List<Type.TypeVar> rawTypeParams(ClassSymbol c) {
        return c.typeParams();
    }

    Env headerEnv(ClassSymbol c) {
        return compiler.attr.classEnv(c);
    }

    void enterHeader(ClassSymbol c) {
        Tree.ClassDecl decl = c.decl;
        Env env = headerEnv(c);
        Attr attr = compiler.attr;
        for (Tree.TypeParameter tp : decl.typeParams) {
            List<Type> bounds = new ArrayList<Type>();
            for (Tree b : tp.bounds) {
                bounds.add(attr.attribType(b, env));
            }
            tp.tvar.bound = bounds.isEmpty() ? compiler.symtab.objectType
                    : bounds.size() == 1 ? bounds.get(0) : new Type.IntersectionType(bounds);
        }
        switch (decl.kind) {
            case ENUM: {
                ClassSymbol enumSym = compiler.symtab.require("java/lang/Enum");
                List<Type> args = new ArrayList<Type>();
                args.add(c.erasure());
                c.setSuperclass(enumSym.typeParams().isEmpty() ? enumSym.erasure()
                        : new Type.ClassType(enumSym, args, null));
                break;
            }
            case RECORD:
                c.setSuperclass(compiler.symtab.type("java/lang/Record"));
                break;
            case INTERFACE:
            case ANNOTATION:
                c.setSuperclass(compiler.symtab.objectType);
                break;
            default:
                if (decl.extending != null) {
                    Type sup = attr.attribType(decl.extending, env);
                    if (sup.tag == Type.Tag.CLASS && ((Type.ClassType) sup).sym.isInterface()) {
                        compiler.error(c.unit, decl.extending.pos, "no interface expected here");
                    } else if (sup.tag == Type.Tag.CLASS && ((Type.ClassType) sup).sym.isFinal()
                            && !(c.anonymous && ((Type.ClassType) sup).sym.isEnum())) {
                        compiler.error(c.unit, decl.extending.pos, "cannot inherit from final " + sup);
                    }
                    c.setSuperclass(sup.tag == Type.Tag.CLASS ? sup : compiler.symtab.objectType);
                } else if (c != compiler.symtab.objectSym) {
                    c.setSuperclass(compiler.symtab.objectType);
                }
        }
        if (decl.kind == Tree.ClassKind.ANNOTATION) {
            c.interfaces().add(compiler.symtab.type("java/lang/annotation/Annotation"));
        }
        for (Tree i : decl.implementing) {
            Type t = attr.attribType(i, env);
            if (t.tag == Type.Tag.CLASS && !((Type.ClassType) t).sym.isInterface()) {
                compiler.error(c.unit, i.pos, "interface expected here");
                continue;
            }
            if (t.tag == Type.Tag.CLASS) {
                boolean repeated = false;
                for (Type earlier : c.interfaces()) {
                    if (earlier.tag == Type.Tag.CLASS && ((Type.ClassType) earlier).sym == ((Type.ClassType) t).sym) {
                        repeated = true;
                    }
                }
                if (repeated) {
                    // The class file would list it twice, which the JVM refuses to load.
                    compiler.error(c.unit, i.pos, "repeated interface");
                    continue;
                }
                c.interfaces().add(t);
            }
        }
        for (Tree pm : decl.permitting) {
            Type t = attr.attribType(pm, env);
            if (t.tag == Type.Tag.CLASS) {
                c.permitted.add(((Type.ClassType) t).sym);
            }
        }
        c.thisType = null;
    }

    // ------------------------------------------------------------------ member modifiers (JLS 8.3.1, 8.4.3, 9.3, 9.4)

    private static final int ACCESS = Symbol.ACC_PUBLIC | Symbol.ACC_PROTECTED | Symbol.ACC_PRIVATE;
    private static final int FIELD_MODIFIERS = ACCESS | Symbol.ACC_STATIC | Symbol.ACC_FINAL | Symbol.ACC_TRANSIENT
            | Symbol.ACC_VOLATILE;
    private static final int INTERFACE_FIELD_MODIFIERS = Symbol.ACC_PUBLIC | Symbol.ACC_STATIC | Symbol.ACC_FINAL;
    private static final int METHOD_MODIFIERS = ACCESS | Symbol.ACC_STATIC | Symbol.ACC_FINAL | Symbol.ACC_ABSTRACT
            | Symbol.ACC_SYNCHRONIZED | Symbol.ACC_NATIVE | Symbol.ACC_STRICT;
    private static final int INTERFACE_METHOD_MODIFIERS = Symbol.ACC_PUBLIC | Symbol.ACC_PRIVATE | Symbol.ACC_STATIC
            | Symbol.ACC_ABSTRACT | Symbol.ACC_STRICT | JavaSourceParser.DEFAULT;
    /** Every source modifier, in the order javac names them. */
    private static final int[] MODIFIER_FLAGS = {Symbol.ACC_PUBLIC, Symbol.ACC_PRIVATE, Symbol.ACC_PROTECTED,
        Symbol.ACC_STATIC, Symbol.ACC_FINAL, Symbol.ACC_SYNCHRONIZED, Symbol.ACC_VOLATILE, Symbol.ACC_TRANSIENT,
        Symbol.ACC_NATIVE, Symbol.ACC_ABSTRACT, Symbol.ACC_STRICT, JavaSourceParser.DEFAULT, JavaSourceParser.SEALED,
        JavaSourceParser.NON_SEALED};
    private static final String[] MODIFIER_NAMES = {"public", "private", "protected", "static", "final",
        "synchronized", "volatile", "transient", "native", "abstract", "strictfp", "default", "sealed", "non-sealed"};

    /**
     * A field or method modifier the declaration cannot carry, or two that exclude each other.
     * The flags are written to the class file as they are, and several share a bit with a
     * different meaning on the other kind of member (a volatile method is a bridge, a
     * transient one variable-arity), so an unchecked combination is a class the JVM refuses
     * or one that means something else.
     */
    private void checkModifiers(int flags, int allowed, ClassSymbol c, int pos) {
        for (int i = 0; i < MODIFIER_FLAGS.length; i++) {
            if ((flags & MODIFIER_FLAGS[i]) != 0 && (allowed & MODIFIER_FLAGS[i]) == 0) {
                compiler.error(c.unit, pos, "modifier " + MODIFIER_NAMES[i] + " not allowed here");
                return;
            }
        }
        String a = null;
        String b = null;
        int access = flags & ACCESS;
        if ((access & (access - 1)) != 0) {
            a = name(flags & ACCESS, 0);
            b = name(flags & ACCESS, 1);
        } else if ((flags & Symbol.ACC_FINAL) != 0 && (flags & Symbol.ACC_VOLATILE) != 0) {
            a = "final";
            b = "volatile";
        } else if ((flags & Symbol.ACC_ABSTRACT) != 0) {
            int clash = flags & (Symbol.ACC_PRIVATE | Symbol.ACC_STATIC | Symbol.ACC_FINAL | Symbol.ACC_NATIVE
                    | Symbol.ACC_SYNCHRONIZED | Symbol.ACC_STRICT | JavaSourceParser.DEFAULT);
            if (clash != 0) {
                a = "abstract";
                b = name(clash, 0);
            }
        } else if ((flags & JavaSourceParser.DEFAULT) != 0
                && (flags & (Symbol.ACC_PRIVATE | Symbol.ACC_STATIC)) != 0) {
            a = (flags & Symbol.ACC_PRIVATE) != 0 ? "private" : "static";
            b = "default";
        }
        if (a != null) {
            compiler.error(c.unit, pos, "illegal combination of modifiers: " + a + " and " + b);
        }
    }

    /**
     * Class modifiers (JLS 8.1.1, 8.9, 8.10, 9.1.1): which a declaration may carry depends on
     * whether it is top level, a member or local, and on its kind; the JVM refuses a class whose
     * flags conflict (abstract and final) and an enum or interface that is final.
     */
    private void checkClassModifiers(Tree.ClassDecl decl, ClassSymbol c, ClassSymbol outer) {
        int flags = decl.mods.flags;
        int allowed = Symbol.ACC_ABSTRACT | Symbol.ACC_FINAL | Symbol.ACC_STRICT | JavaSourceParser.SEALED
                | JavaSourceParser.NON_SEALED;
        if (decl.local) {
            allowed &= ~(JavaSourceParser.SEALED | JavaSourceParser.NON_SEALED);
        } else if (outer == null) {
            allowed |= Symbol.ACC_PUBLIC;
        } else {
            allowed |= ACCESS | Symbol.ACC_STATIC;
        }
        switch (decl.kind) {
            case INTERFACE:
            case ANNOTATION:
                allowed &= ~Symbol.ACC_FINAL;
                break;
            case ENUM:
                allowed &= ~(Symbol.ACC_ABSTRACT | Symbol.ACC_FINAL | JavaSourceParser.SEALED
                        | JavaSourceParser.NON_SEALED);
                break;
            case RECORD:
                allowed &= ~(Symbol.ACC_ABSTRACT | JavaSourceParser.SEALED | JavaSourceParser.NON_SEALED);
                break;
            default:
                break;
        }
        if ((decl.kind == Tree.ClassKind.INTERFACE || decl.kind == Tree.ClassKind.ANNOTATION)
                && (flags & Symbol.ACC_FINAL) != 0) {
            compiler.error(c.unit, decl.pos, "illegal combination of modifiers: interface and final");
            return;
        }
        for (int i = 0; i < MODIFIER_FLAGS.length; i++) {
            if ((flags & MODIFIER_FLAGS[i]) != 0 && (allowed & MODIFIER_FLAGS[i]) == 0) {
                compiler.error(c.unit, decl.pos, "modifier " + MODIFIER_NAMES[i] + " not allowed here");
                return;
            }
        }
        int access = flags & ACCESS;
        int restrictions = flags & (Symbol.ACC_FINAL | JavaSourceParser.SEALED | JavaSourceParser.NON_SEALED);
        String a = null;
        String b = null;
        if ((access & (access - 1)) != 0) {
            a = name(access, 0);
            b = name(access, 1);
        } else if ((flags & Symbol.ACC_ABSTRACT) != 0 && (flags & Symbol.ACC_FINAL) != 0) {
            a = "abstract";
            b = "final";
        } else if ((restrictions & (restrictions - 1)) != 0) {
            a = name(restrictions, 0);
            b = name(restrictions, 1);
        }
        if (a != null) {
            compiler.error(c.unit, decl.pos, "illegal combination of modifiers: " + a + " and " + b);
        }
    }

    /**
     * A class that is its own supertype (JLS 8.1.4, 9.1.3): the JVM refuses to load it with
     * ClassCircularityError. Reported once, on the first class of the cycle, which then loses
     * its supertypes so nothing later walks the cycle forever.
     */
    private void checkCyclicInheritance(ClassSymbol c) {
        if (reachesItself(c, c, new java.util.HashSet<ClassSymbol>())) {
            compiler.error(c.unit, c.decl.extending != null ? c.decl.extending.pos : c.decl.pos,
                    "cyclic inheritance involving " + c.javaName());
            c.setSuperclass(compiler.symtab.objectType);
            c.interfaces().clear();
        }
    }

    private static boolean reachesItself(ClassSymbol target, ClassSymbol from, java.util.Set<ClassSymbol> seen) {
        if (from.decl == null || !seen.add(from)) {
            return false;
        }
        List<Type> supers = new ArrayList<Type>(from.interfaces());
        if (from.superclass() != null) {
            supers.add(from.superclass());
        }
        for (Type t : supers) {
            if (t instanceof Type.ClassType) {
                ClassSymbol s = ((Type.ClassType) t).sym;
                if (s == target || reachesItself(target, s, seen)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The name of the n-th modifier set in flags, in javac's order. */
    private static String name(int flags, int n) {
        for (int i = 0; i < MODIFIER_FLAGS.length; i++) {
            if ((flags & MODIFIER_FLAGS[i]) != 0 && n-- == 0) {
                return MODIFIER_NAMES[i];
            }
        }
        return "";
    }

    void enterMembers(ClassSymbol c) {
        Tree.ClassDecl decl = c.decl;
        Env env = headerEnv(c);
        Attr attr = compiler.attr;
        boolean isInterface = c.isInterface();
        boolean hasCtor = false;
        // Record components first: they are fields in declaration order.
        for (Tree.VarDef comp : decl.recordComponents) {
            Type t = attr.attribType(comp.vartype, env);
            VarSymbol f = new VarSymbol(comp.name, Symbol.ACC_PRIVATE | Symbol.ACC_FINAL, t, VarSymbol.Kind.FIELD);
            f.owner = c;
            comp.sym = f;
            c.fields.add(f);
            c.recordComponents.add(f);
        }
        for (Tree member : decl.members) {
            if (member instanceof Tree.VarDef) {
                Tree.VarDef v = (Tree.VarDef) member;
                if ((v.mods.flags & Symbol.ACC_ENUM) == 0) {
                    checkModifiers(v.mods.flags, isInterface ? INTERFACE_FIELD_MODIFIERS : FIELD_MODIFIERS, c, v.pos);
                }
                int flags = v.mods.flags & 0xFFFF;
                Type t;
                if ((flags & Symbol.ACC_ENUM) != 0) {
                    t = c.erasure();
                } else if ((flags & Symbol.ACC_STATIC) != 0 || isInterface) {
                    // A static field's type is in a static context: the class's type variables are not.
                    Env staticEnv = new Env(env, env.unit, c, null);
                    staticEnv.isStatic = true;
                    t = attr.attribType(v.vartype, staticEnv);
                } else {
                    t = attr.attribType(v.vartype, env);
                }
                if (isInterface) {
                    flags |= Symbol.ACC_PUBLIC | Symbol.ACC_STATIC | Symbol.ACC_FINAL;
                }
                if (decl.kind == Tree.ClassKind.RECORD && (flags & Symbol.ACC_STATIC) == 0) {
                    compiler.error(c.unit, v.pos, "field declaration must be static (records cannot declare instance fields)");
                }
                VarSymbol f = new VarSymbol(v.name, flags, t, VarSymbol.Kind.FIELD);
                f.owner = c;
                f.hasInitializer = v.init != null;
                f.decl = v;
                v.sym = f;
                for (VarSymbol existing : c.fields) {
                    if (existing.name.equals(v.name)) {
                        compiler.error(c.unit, v.pos, "variable " + v.name + " is already defined in class " + c.simpleName);
                    }
                }
                c.fields.add(f);
            } else if (member instanceof Tree.MethodDecl) {
                Tree.MethodDecl m = (Tree.MethodDecl) member;
                MethodSymbol ms = enterMethod(m, c, env);
                if (ms.isConstructor()) {
                    hasCtor = true;
                }
            }
        }
        if (!isInterface) {
            if (decl.kind == Tree.ClassKind.RECORD) {
                enterRecordMembers(c, decl);
            } else if (!hasCtor && !c.anonymous) {
                MethodSymbol ctor = new MethodSymbol("<init>", SYNTH_DEFAULT_CTOR, c);
                if (c.isEnum()) {
                    ctor.flags |= Symbol.ACC_PRIVATE;
                } else {
                    ctor.flags |= c.flags & (Symbol.ACC_PUBLIC | Symbol.ACC_PROTECTED | Symbol.ACC_PRIVATE);
                }
                ctor.returnType = Type.VOID;
                c.methods.add(ctor);
            }
        }
        if (c.isEnum()) {
            MethodSymbol values = new MethodSymbol("values", Symbol.ACC_PUBLIC | Symbol.ACC_STATIC | SYNTH_ENUM_VALUES, c);
            values.returnType = new Type.ArrayType(c.erasure());
            c.methods.add(values);
            MethodSymbol valueOf = new MethodSymbol("valueOf", Symbol.ACC_PUBLIC | Symbol.ACC_STATIC | SYNTH_ENUM_VALUEOF, c);
            valueOf.params.add(compiler.symtab.stringType);
            valueOf.returnType = c.erasure();
            c.methods.add(valueOf);
        }
    }

    MethodSymbol enterMethod(Tree.MethodDecl m, ClassSymbol c, Env classEnv) {
        checkModifiers(m.mods.flags, "<init>".equals(m.name) ? ACCESS
                : c.isInterface() ? INTERFACE_METHOD_MODIFIERS : METHOD_MODIFIERS, c, m.pos);
        int flags = m.mods.flags & 0xFFFF;
        boolean isInterface = c.isInterface();
        if (isInterface) {
            if ((m.mods.flags & JavaSourceParser.DEFAULT) != 0) {
                flags |= Symbol.DEFAULT_METHOD | Symbol.ACC_PUBLIC;
            } else if ((flags & (Symbol.ACC_STATIC | Symbol.ACC_PRIVATE)) == 0) {
                flags |= Symbol.ACC_ABSTRACT | Symbol.ACC_PUBLIC;
            } else if ((flags & Symbol.ACC_PRIVATE) == 0) {
                flags |= Symbol.ACC_PUBLIC;
            }
            if (m.body == null && (flags & Symbol.ACC_ABSTRACT) == 0) {
                compiler.error(c.unit, m.pos, "missing method body, or declare abstract");
            } else if (m.body != null && (flags & Symbol.ACC_ABSTRACT) != 0) {
                compiler.error(c.unit, m.pos, "interface abstract methods cannot have body");
            }
        } else if ((flags & Symbol.ACC_NATIVE) != 0) {
            // A deliberate departure from javac. The code this compiles runs
            // inside a running ParparVM (the Playground) or is defined into the
            // simulator's JVM, and neither can bind a native method to an
            // implementation: there is no library to load it from. Codename One
            // reaches platform code through NativeInterface, never through Java
            // `native` methods, so the declaration is refused outright -- with
            // or without a body -- instead of producing a class that fails with
            // UnsatisfiedLinkError (or ClassFormatError, for a body) at run time.
            compiler.error(c.unit, m.pos, "native methods are not supported");
        } else if (m.body == null && (flags & Symbol.ACC_ABSTRACT) == 0) {
            compiler.error(c.unit, m.pos, "missing method body, or declare abstract");
        } else if (m.body != null && (flags & Symbol.ACC_ABSTRACT) != 0) {
            compiler.error(c.unit, m.pos, "abstract methods cannot have a body");
        }
        if (m.varargs) {
            flags |= Symbol.ACC_VARARGS;
        }
        MethodSymbol ms = new MethodSymbol(m.name, flags, c);
        ms.decl = m;
        m.sym = ms;
        Env env = new Env(classEnv, classEnv.unit, c, ms);
        env.isStatic = (flags & Symbol.ACC_STATIC) != 0;
        for (Tree.TypeParameter tp : m.typeParams) {
            Type.TypeVar tv = new Type.TypeVar(tp.name);
            tp.tvar = tv;
            ms.typeParams.add(tv);
        }
        Attr attr = compiler.attr;
        for (Tree.TypeParameter tp : m.typeParams) {
            List<Type> bounds = new ArrayList<Type>();
            for (Tree b : tp.bounds) {
                bounds.add(attr.attribType(b, env));
            }
            tp.tvar.bound = bounds.isEmpty() ? compiler.symtab.objectType
                    : bounds.size() == 1 ? bounds.get(0) : new Type.IntersectionType(bounds);
        }
        ms.paramSyms = new ArrayList<VarSymbol>();
        if (m.compactConstructor) {
            for (VarSymbol comp : c.recordComponents) {
                VarSymbol p = new VarSymbol(comp.name, 0, comp.type, VarSymbol.Kind.PARAM);
                ms.paramSyms.add(p);
                ms.params.add(comp.type);
            }
        } else {
            for (Tree.VarDef p : m.params) {
                Type t = attr.attribType(p.vartype, env);
                VarSymbol ps = new VarSymbol(p.name, p.mods.flags & Symbol.ACC_FINAL, t, VarSymbol.Kind.PARAM);
                p.sym = ps;
                ms.paramSyms.add(ps);
                ms.params.add(t);
            }
        }
        ms.returnType = m.returnType == null ? Type.VOID : attr.attribType(m.returnType, env);
        for (Tree t : m.thrown) {
            Type th = attr.attribType(t, env);
            // Only a Throwable may be thrown (JLS 8.4.6); anything else would reach the
            // Exceptions attribute and make the method uncallable from javac-compiled code.
            if (!th.isErroneous() && th.tag != Type.Tag.TYPEVAR
                    && !compiler.types.isSubtype(Types.erasure(th), compiler.symtab.type("java/lang/Throwable"))) {
                compiler.error(c.unit, t.pos, "incompatible types: " + th + " cannot be converted to Throwable");
                continue;
            }
            ms.thrown.add(th);
        }
        for (MethodSymbol existing : c.methods) {
            if (existing.name.equals(ms.name) && existing.params.size() == ms.params.size() && existing.decl != null) {
                boolean same = true;
                for (int i = 0; i < ms.params.size(); i++) {
                    if (!Types.descriptor(Types.erasure(existing.params.get(i))).equals(Types.descriptor(Types.erasure(ms.params.get(i))))) {
                        same = false;
                    }
                }
                if (same) {
                    compiler.error(c.unit, m.pos, "method " + ms + " is already defined in " + c.simpleName);
                }
            }
        }
        c.methods.add(ms);
        return ms;
    }

    private void enterRecordMembers(ClassSymbol c, Tree.ClassDecl decl) {
        boolean canonical = false;
        for (MethodSymbol m : c.methods) {
            if (m.isConstructor() && m.params.size() == c.recordComponents.size()) {
                boolean same = true;
                for (int i = 0; i < m.params.size(); i++) {
                    if (!Types.descriptor(Types.erasure(m.params.get(i))).equals(
                            Types.descriptor(Types.erasure(c.recordComponents.get(i).type)))) {
                        same = false;
                    }
                }
                canonical |= same;
            }
        }
        if (!canonical) {
            MethodSymbol ctor = new MethodSymbol("<init>", (c.flags & (Symbol.ACC_PUBLIC | Symbol.ACC_PROTECTED
                    | Symbol.ACC_PRIVATE)) | SYNTH_DEFAULT_CTOR, c);
            ctor.paramSyms = new ArrayList<VarSymbol>();
            for (VarSymbol comp : c.recordComponents) {
                ctor.params.add(comp.type);
                ctor.paramSyms.add(new VarSymbol(comp.name, 0, comp.type, VarSymbol.Kind.PARAM));
            }
            ctor.returnType = Type.VOID;
            c.methods.add(ctor);
        }
        for (VarSymbol comp : c.recordComponents) {
            if (findDeclared(c, comp.name, 0) == null) {
                MethodSymbol acc = new MethodSymbol(comp.name, Symbol.ACC_PUBLIC | SYNTH_RECORD_ACCESSOR, c);
                acc.returnType = comp.type;
                c.methods.add(acc);
            }
        }
        if (findDeclared(c, "toString", 0) == null) {
            MethodSymbol m = new MethodSymbol("toString", Symbol.ACC_PUBLIC | SYNTH_RECORD_TOSTRING, c);
            m.returnType = compiler.symtab.stringType;
            c.methods.add(m);
        }
        if (findDeclared(c, "hashCode", 0) == null) {
            MethodSymbol m = new MethodSymbol("hashCode", Symbol.ACC_PUBLIC | SYNTH_RECORD_HASHCODE, c);
            m.returnType = Type.INT;
            c.methods.add(m);
        }
        if (findDeclared(c, "equals", 1) == null) {
            MethodSymbol m = new MethodSymbol("equals", Symbol.ACC_PUBLIC | SYNTH_RECORD_EQUALS, c);
            m.params.add(compiler.symtab.objectType);
            m.returnType = Type.BOOLEAN;
            c.methods.add(m);
        }
    }

    private static MethodSymbol findDeclared(ClassSymbol c, String name, int arity) {
        for (MethodSymbol m : c.methods) {
            if (m.name.equals(name) && m.params.size() == arity) {
                return m;
            }
        }
        return null;
    }
}
