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
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates class files from attributed trees. Lowers what the JVM does not have:
 * inner classes (outer instance and captured locals become constructor
 * parameters and synthetic fields), lambdas and method references (a private
 * static body method plus a LambdaMetafactory invokedynamic), enum and record
 * members, string/enum/pattern switches, try-with-resources and finally
 * (inlined at every exit), string concatenation (StringBuilder) and boxing.
 * Output is class-file version 61 with nest attributes and StackMapTable frames.
 */
final class Gen {
    private static final int VERSION = 61;
    private static final String LMF_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
            + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;"
            + "Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";

    private final Compiler compiler;
    private final Symtab syms;
    private final Types types;
    private final Attr attr;

    // ---- per class
    private ClassSymbol cls;
    private ConstPool pool;
    private List<ByteBuf> fieldInfos;
    private List<ByteBuf> methodInfos;
    private Set<String> emittedMethods;
    private final Map<Tree.Lambda, String> lambdaNames = new IdentityHashMap<Tree.Lambda, String>();
    private List<Tree.Lambda> pendingLambdas;
    /** Whether the class being generated contains an {@code assert} (outside its nested classes). */
    private boolean usesAssert;

    // ---- per method
    private Code code;
    private MethodSymbol method;
    private Map<VarSymbol, Integer> slots;
    private Map<VarSymbol, Type> slotTypes;
    /** Before the super()/this() call of a constructor: the outer instance and captures come from parameters. */
    private boolean prologue;
    private int outerParamSlot = -1;
    private Map<VarSymbol, Integer> capturedParamSlots;
    /** Jump targets and finalizers in nesting order (see {@link Target} and {@link Fin}). */
    private List<Object> jumpStack;

    Gen(Compiler compiler) {
        this.compiler = compiler;
        this.syms = compiler.symtab;
        this.types = compiler.types;
        this.attr = compiler.attr;
    }

    private void error(int pos, String message) {
        compiler.error(cls.unit, pos, message);
    }

    private int line(int pos) {
        return cls.unit == null || cls.unit.source == null ? 0 : cls.unit.source.line(pos);
    }

    // ================================================================== class

    byte[] genClass(ClassSymbol c) {
        cls = c;
        pool = new ConstPool();
        fieldInfos = new ArrayList<ByteBuf>();
        methodInfos = new ArrayList<ByteBuf>();
        emittedMethods = new LinkedHashSet<String>();
        pendingLambdas = new ArrayList<Tree.Lambda>();
        usesAssert = containsAssert(c.decl);
        try {
            genFields(c);
            for (MethodSymbol m : new ArrayList<MethodSymbol>(c.methods)) {
                genMember(m);
            }
            genClassInit(c);
            genBridges(c);
            for (int i = 0; i < pendingLambdas.size(); i++) {
                genLambdaBody(pendingLambdas.get(i));
            }
            return assemble(c);
        } catch (CompileError e) {
            error(c.decl == null ? 0 : c.decl.pos, e.getMessage());
            return null;
        }
    }

    private int classAccess(ClassSymbol c) {
        int f = 0;
        if (c.isPublic() || c.isProtected()) {
            f |= Symbol.ACC_PUBLIC;
        }
        if (c.isInterface()) {
            f |= Symbol.ACC_INTERFACE | Symbol.ACC_ABSTRACT;
            if (c.isAnnotation()) {
                f |= Symbol.ACC_ANNOTATION;
            }
        } else {
            f |= Symbol.ACC_SUPER;
            if (c.isFinal() || c.anonymous && !c.isEnum()) {
                f |= c.anonymous ? 0 : Symbol.ACC_FINAL;
            }
            if (isAbstractClass(c)) {
                f |= Symbol.ACC_ABSTRACT;
            }
            if (c.isEnum() && !c.anonymous) {
                f |= Symbol.ACC_ENUM;
            }
        }
        if (c.isFinal() && !c.isInterface()) {
            f |= Symbol.ACC_FINAL;
        }
        return f;
    }

    private boolean isAbstractClass(ClassSymbol c) {
        if (c.isAbstract()) {
            return true;
        }
        if (c.isEnum()) {
            for (MethodSymbol m : c.methods) {
                if (m.isAbstract()) {
                    return true;
                }
            }
        }
        return false;
    }

    private int innerAccess(ClassSymbol c) {
        int f = c.flags & (Symbol.ACC_PUBLIC | Symbol.ACC_PRIVATE | Symbol.ACC_PROTECTED | Symbol.ACC_STATIC
                | Symbol.ACC_FINAL | Symbol.ACC_INTERFACE | Symbol.ACC_ABSTRACT | Symbol.ACC_ANNOTATION | Symbol.ACC_ENUM);
        if (c.anonymous || c.local) {
            f &= ~(Symbol.ACC_PUBLIC | Symbol.ACC_PRIVATE | Symbol.ACC_PROTECTED | Symbol.ACC_STATIC);
            if (c.anonymous) {
                f &= ~Symbol.ACC_FINAL;
            }
        }
        if (isAbstractClass(c) && !c.isInterface()) {
            f |= Symbol.ACC_ABSTRACT;
        }
        return f;
    }

    private byte[] assemble(ClassSymbol c) {
        int thisIdx = pool.cls(c.internalName);
        String superName;
        if (c.isInterface()) {
            superName = "java/lang/Object";
        } else {
            Type sup = c.superclass();
            superName = sup == null ? null : internalName(sup);
        }
        int superIdx = superName == null ? 0 : pool.cls(superName);
        List<Integer> itfs = new ArrayList<Integer>();
        for (Type i : c.interfaces()) {
            itfs.add(Integer.valueOf(pool.cls(internalName(i))));
        }
        // Attributes.
        List<ByteBuf> attrs = new ArrayList<ByteBuf>();
        if (c.unit != null && c.unit.source != null) {
            ByteBuf a = new ByteBuf();
            a.u2(pool.utf8("SourceFile")).u4(2).u2(pool.utf8(c.unit.source.name));
            attrs.add(a);
        }
        List<ClassSymbol> inner = new ArrayList<ClassSymbol>();
        if (c.outer != null) {
            inner.add(c);
        }
        for (ClassSymbol k : compiler.enter.sourceClasses) {
            if (k.outer == c && !inner.contains(k)) {
                inner.add(k);
            }
        }
        if (!inner.isEmpty()) {
            ByteBuf a = new ByteBuf();
            a.u2(pool.utf8("InnerClasses"));
            ByteBuf body = new ByteBuf();
            body.u2(inner.size());
            for (ClassSymbol k : inner) {
                body.u2(pool.cls(k.internalName));
                body.u2(k.local || k.anonymous ? 0 : pool.cls(k.outer.internalName));
                body.u2(k.anonymous ? 0 : pool.utf8(k.simpleName));
                body.u2(innerAccess(k));
            }
            a.u4(body.length).bytes(body);
            attrs.add(a);
        }
        if ((c.local || c.anonymous) && c.outer != null) {
            ByteBuf a = new ByteBuf();
            int nt = 0;
            if (c.enclosingMethod != null && c.enclosingMethod.owner == c.outer) {
                nt = pool.nameAndType(c.enclosingMethod.name, jvmDescriptor(c.enclosingMethod));
            }
            a.u2(pool.utf8("EnclosingMethod")).u4(4).u2(pool.cls(c.outer.internalName)).u2(nt);
            attrs.add(a);
        }
        ClassSymbol host = Attr.outermost(c);
        if (host != c) {
            ByteBuf a = new ByteBuf();
            a.u2(pool.utf8("NestHost")).u4(2).u2(pool.cls(host.internalName));
            attrs.add(a);
        } else {
            List<ClassSymbol> members = new ArrayList<ClassSymbol>();
            for (ClassSymbol k : compiler.enter.sourceClasses) {
                if (k != c && Attr.outermost(k) == c) {
                    members.add(k);
                }
            }
            if (!members.isEmpty()) {
                ByteBuf a = new ByteBuf();
                a.u2(pool.utf8("NestMembers")).u4(2 + 2 * members.size()).u2(members.size());
                for (ClassSymbol k : members) {
                    a.u2(pool.cls(k.internalName));
                }
                attrs.add(a);
            }
        }
        if (pool.hasBootstrapMethods()) {
            ByteBuf body = pool.bootstrapAttribute();
            ByteBuf a = new ByteBuf();
            a.u2(pool.utf8("BootstrapMethods")).u4(body.length).bytes(body);
            attrs.add(a);
        }
        ByteBuf out = new ByteBuf(4096);
        out.u4(0xCAFEBABE).u2(0).u2(VERSION);
        out.u2(pool.size()).bytes(pool.buf);
        out.u2(classAccess(c)).u2(thisIdx).u2(superIdx);
        out.u2(itfs.size());
        for (Integer i : itfs) {
            out.u2(i.intValue());
        }
        out.u2(fieldInfos.size());
        for (ByteBuf f : fieldInfos) {
            out.bytes(f);
        }
        out.u2(methodInfos.size());
        for (ByteBuf m : methodInfos) {
            out.bytes(m);
        }
        out.u2(attrs.size());
        for (ByteBuf a : attrs) {
            out.bytes(a);
        }
        return out.toByteArray();
    }

    // ================================================================== names and descriptors

    String internalName(Type t) {
        return types.internalName(t);
    }

    static String desc(Type erased) {
        return Types.descriptor(erased);
    }

    private Type erased(Type t) {
        if (t == null) {
            return syms.objectType;
        }
        if (t.isPrimitive()) {
            return t;
        }
        return types.erased(types.resolveInference(t));
    }

    /** Does a constructor of c take its outer instance, a captured outer for its superclass, captured locals? */
    private boolean anonSuperNeedsOuter(ClassSymbol c) {
        if (!c.anonymous) {
            return false;
        }
        Type sup = c.superclass();
        return sup != null && sup.tag == Type.Tag.CLASS && ((Type.ClassType) sup).sym.hasOuterInstance;
    }

    /** The JVM descriptor of a method or constructor, including a source constructor's synthetic parameters. */
    String jvmDescriptor(MethodSymbol m) {
        ClassSymbol c = m.owner;
        if (c.decl == null) {
            return m.descriptor();
        }
        if (!m.isConstructor()) {
            return m.descriptor();
        }
        StringBuilder b = new StringBuilder("(");
        if (c.isEnum() || c.anonymous && isEnumConstantBody(c)) {
            b.append("Ljava/lang/String;I");
        }
        if (c.hasOuterInstance) {
            b.append('L').append(c.outer.internalName).append(';');
        }
        if (anonSuperNeedsOuter(c)) {
            ClassSymbol sup = ((Type.ClassType) c.superclass()).sym;
            b.append('L').append(sup.outer.internalName).append(';');
        }
        for (Type p : m.params) {
            b.append(desc(erased(p)));
        }
        for (VarSymbol v : c.capturedVars) {
            b.append(desc(erased(v.type)));
        }
        return b.append(")V").toString();
    }

    private boolean isEnumConstantBody(ClassSymbol c) {
        Type sup = c.superclass();
        return c.anonymous && sup != null && sup.tag == Type.Tag.CLASS && ((Type.ClassType) sup).sym.isEnum()
                && ((Type.ClassType) sup).sym.decl != null && c.outer == ((Type.ClassType) sup).sym && !c.hasOuterInstance
                && c.declEnv != null && c.declEnv.enclMethod == null && isEnumConstantDecl(c);
    }

    private boolean isEnumConstantDecl(ClassSymbol c) {
        ClassSymbol e = c.outer;
        for (Tree m : e.decl.members) {
            if (m instanceof Tree.VarDef && ((Tree.VarDef) m).init instanceof Tree.NewClass
                    && ((Tree.NewClass) ((Tree.VarDef) m).init).body == c.decl) {
                return true;
            }
        }
        return false;
    }

    // ================================================================== fields

    private void genFields(ClassSymbol c) {
        for (VarSymbol f : c.fields) {
            int access = f.flags & 0x50DF;
            ByteBuf b = new ByteBuf();
            b.u2(access).u2(pool.utf8(f.name)).u2(pool.utf8(desc(erased(f.type))));
            Object cv = f.isStatic() && f.isFinal() ? f.constValue : null;
            if (cv != null) {
                b.u2(1).u2(pool.utf8("ConstantValue")).u4(2).u2(constantIndex(cv, f.type));
            } else {
                b.u2(0);
            }
            fieldInfos.add(b);
        }
        if (c.hasOuterInstance) {
            syntheticField("this$0", "L" + c.outer.internalName + ";", Symbol.ACC_FINAL | Symbol.ACC_SYNTHETIC);
        }
        for (VarSymbol v : c.capturedVars) {
            syntheticField("val$" + v.name, desc(erased(v.type)), Symbol.ACC_PRIVATE | Symbol.ACC_FINAL | Symbol.ACC_SYNTHETIC);
        }
        if (usesAssert) {
            // javac's layout: a class with an assert caches whether assertions are enabled for it.
            // An interface field must be public; elsewhere it is package access, as javac emits it.
            syntheticField(ASSERTIONS_DISABLED, "Z", Symbol.ACC_STATIC | Symbol.ACC_FINAL | Symbol.ACC_SYNTHETIC
                    | (c.isInterface() ? Symbol.ACC_PUBLIC : 0));
        }
        if (c.isEnum() && !c.anonymous) {
            syntheticField("$VALUES", "[L" + c.internalName + ";", Symbol.ACC_PRIVATE | Symbol.ACC_STATIC | Symbol.ACC_FINAL | Symbol.ACC_SYNTHETIC);
        }
    }

    private void syntheticField(String name, String descriptor, int access) {
        ByteBuf b = new ByteBuf();
        b.u2(access).u2(pool.utf8(name)).u2(pool.utf8(descriptor)).u2(0);
        fieldInfos.add(b);
    }

    private int constantIndex(Object v, Type t) {
        Object c = Constants.cast(v, t);
        if (c instanceof String) {
            return pool.string((String) c);
        }
        if (c instanceof Long) {
            return pool.longConst(((Long) c).longValue());
        }
        if (c instanceof Float) {
            return pool.floatConst(((Float) c).floatValue());
        }
        if (c instanceof Double) {
            return pool.doubleConst(((Double) c).doubleValue());
        }
        if (c instanceof Boolean) {
            return pool.integer(((Boolean) c).booleanValue() ? 1 : 0);
        }
        return pool.integer(Constants.intValue(c));
    }

    // ================================================================== methods

    private void beginMethod(MethodSymbol m, boolean isStatic) {
        code = new Code(pool);
        method = m;
        slots = new IdentityHashMap<VarSymbol, Integer>();
        slotTypes = new IdentityHashMap<VarSymbol, Type>();
        capturedParamSlots = new IdentityHashMap<VarSymbol, Integer>();
        jumpStack = new ArrayList<Object>();
        currentReturnType = null;
        prologue = false;
        outerParamSlot = -1;
        if (!isStatic) {
            code.newLocal(syms.objectType);
        }
    }

    private int declareLocal(VarSymbol v, Type slotType) {
        int s = code.newLocal(slotType);
        slots.put(v, Integer.valueOf(s));
        slotTypes.put(v, slotType);
        return s;
    }

    /** Writes a method_info for the code just generated. */
    private void endMethod(int access, String name, String descriptor, boolean isStatic, boolean isCtor) {
        String key = name + descriptor;
        if (!emittedMethods.add(key)) {
            return;
        }
        ByteBuf m = new ByteBuf();
        m.u2(access & 0xFFFF).u2(pool.utf8(name)).u2(pool.utf8(descriptor));
        if (code == null) {
            m.u2(0);
            methodInfos.add(m);
            return;
        }
        FrameComputer fc = new FrameComputer(code.bc, pool, hierarchy, cls.internalName, code.maxLocals, code.exceptionTable);
        ByteBuf frames = fc.compute(isStatic, isCtor, descriptor);
        if (code.bc.length > 65535) {
            throw new CompileError("code too large");
        }
        ByteBuf body = new ByteBuf();
        body.u2(fc.maxStack).u2(code.maxLocals).u4(code.bc.length).bytes(code.bc);
        body.u2(code.exceptionTable.size());
        for (int[] h : code.exceptionTable) {
            body.u2(h[0]).u2(h[1]).u2(h[2]).u2(h[3]);
        }
        int attrCount = (frames != null ? 1 : 0) + (code.lineNumbers.isEmpty() ? 0 : 1);
        body.u2(attrCount);
        if (frames != null) {
            body.u2(pool.utf8("StackMapTable")).u4(frames.length).bytes(frames);
        }
        if (!code.lineNumbers.isEmpty()) {
            body.u2(pool.utf8("LineNumberTable")).u4(2 + 4 * code.lineNumbers.size()).u2(code.lineNumbers.size());
            for (int[] ln : code.lineNumbers) {
                body.u2(ln[0]).u2(ln[1]);
            }
        }
        m.u2(1).u2(pool.utf8("Code")).u4(body.length).bytes(body);
        methodInfos.add(m);
        code = null;
    }

    private final FrameComputer.Hierarchy hierarchy = new FrameComputer.Hierarchy() {
        @Override
        public String superclass(String internalName) {
            ClassSymbol c = syms.lookup(internalName);
            if (c == null || c == syms.objectSym) {
                return null;
            }
            Type s = c.superclass();
            return s == null ? null : types.internalName(s);
        }

        @Override
        public boolean isInterface(String internalName) {
            ClassSymbol c = syms.lookup(internalName);
            return c != null && c.isInterface();
        }
    };

    private void genMember(MethodSymbol m) {
        int flags = m.flags;
        if ((flags & Enter.SYNTH_DEFAULT_CTOR) != 0) {
            genConstructor(m, null);
        } else if ((flags & Enter.SYNTH_ENUM_VALUES) != 0) {
            genEnumValues(m);
        } else if ((flags & Enter.SYNTH_ENUM_VALUEOF) != 0) {
            genEnumValueOf(m);
        } else if ((flags & Enter.SYNTH_RECORD_ACCESSOR) != 0) {
            genRecordAccessor(m);
        } else if ((flags & Enter.SYNTH_RECORD_TOSTRING) != 0) {
            genRecordToString(m);
        } else if ((flags & Enter.SYNTH_RECORD_HASHCODE) != 0) {
            genRecordHashCode(m);
        } else if ((flags & Enter.SYNTH_RECORD_EQUALS) != 0) {
            genRecordEquals(m);
        } else if (m.superAccessTarget != null) {
            genSuperAccessor(m);
        } else if (m.decl == null && m.isConstructor() && cls.anonymous) {
            genConstructor(m, null);
        } else if (m.decl != null) {
            if (m.isConstructor()) {
                genConstructor(m, m.decl);
            } else {
                genMethod(m);
            }
        }
    }

    private int methodAccess(MethodSymbol m) {
        int a = m.flags & (Symbol.ACC_PUBLIC | Symbol.ACC_PRIVATE | Symbol.ACC_PROTECTED | Symbol.ACC_STATIC
                | Symbol.ACC_FINAL | Symbol.ACC_SYNCHRONIZED | Symbol.ACC_VARARGS | Symbol.ACC_NATIVE
                | Symbol.ACC_ABSTRACT | Symbol.ACC_STRICT);
        return a;
    }

    private void genMethod(MethodSymbol m) {
        Tree.MethodDecl d = m.decl;
        if (d.body == null) {
            code = null;
            endMethod(methodAccess(m), m.name, jvmDescriptor(m), m.isStatic(), false);
            return;
        }
        beginMethod(m, m.isStatic());
        for (VarSymbol p : m.paramSyms) {
            declareLocal(p, erased(p.type));
        }
        genStats(d.body.stats);
        if (code.alive) {
            if (m.returnType.tag == Type.Tag.VOID) {
                code.op(Code.RETURN);
            } else {
                error(d.pos, "missing return statement");
                code.op(Code.ACONST_NULL);
                code.op(Code.ATHROW);
            }
        }
        endMethod(methodAccess(m), m.name, jvmDescriptor(m), m.isStatic(), false);
    }

    // ------------------------------------------------------------------ constructors

    private void genConstructor(MethodSymbol m, Tree.MethodDecl d) {
        ClassSymbol c = cls;
        beginMethod(m, false);
        int nameSlot = -1;
        int ordinalSlot = -1;
        boolean enumCtor = c.isEnum() && !c.anonymous || c.anonymous && isEnumConstantBody(c);
        if (enumCtor) {
            nameSlot = code.newLocal(syms.stringType);
            ordinalSlot = code.newLocal(Type.INT);
        }
        if (c.hasOuterInstance) {
            outerParamSlot = code.newLocal(c.outer.erasure());
        }
        int superOuterSlot = -1;
        if (anonSuperNeedsOuter(c)) {
            superOuterSlot = code.newLocal(syms.objectType);
        }
        List<Integer> paramSlots = new ArrayList<Integer>();
        if (m.paramSyms != null && !m.paramSyms.isEmpty()) {
            for (VarSymbol p : m.paramSyms) {
                paramSlots.add(Integer.valueOf(declareLocal(p, erased(p.type))));
            }
        } else {
            for (Type p : m.params) {
                paramSlots.add(Integer.valueOf(code.newLocal(erased(p))));
            }
        }
        for (VarSymbol v : c.capturedVars) {
            int s = code.newLocal(erased(v.type));
            capturedParamSlots.put(v, Integer.valueOf(s));
        }
        String descriptor = jvmDescriptor(m);
        Tree.MethodCall explicit = d == null ? null : Attr.explicitConstructorCall(d.body);
        boolean callsThis = explicit != null && !explicit.superCall;
        prologue = true;
        if (!callsThis) {
            // Synthetic fields first: the superclass constructor may call an overridden method that reads them.
            if (c.hasOuterInstance) {
                code.varOp(Code.ALOAD, 0);
                code.varOp(Code.ALOAD, outerParamSlot);
                code.op2(Code.PUTFIELD, pool.field(c.internalName, "this$0", "L" + c.outer.internalName + ";"));
            }
            for (VarSymbol v : c.capturedVars) {
                code.varOp(Code.ALOAD, 0);
                Type t = erased(v.type);
                code.varOp(loadOp(t), capturedParamSlots.get(v).intValue());
                code.op2(Code.PUTFIELD, pool.field(c.internalName, "val$" + v.name, desc(t)));
            }
        }
        if (explicit != null) {
            code.line(line(explicit.pos));
            genExplicitConstructorCall(explicit, nameSlot, ordinalSlot);
        } else if (enumCtor && !c.anonymous) {
            code.varOp(Code.ALOAD, 0);
            code.varOp(Code.ALOAD, nameSlot);
            code.varOp(Code.ILOAD, ordinalSlot);
            code.op2(Code.INVOKESPECIAL, pool.method("java/lang/Enum", "<init>", "(Ljava/lang/String;I)V", false));
        } else if (c.anonymous) {
            // Pass the parameters straight to the superclass constructor.
            MethodSymbol sup = m.superCtor;
            code.varOp(Code.ALOAD, 0);
            String superOwner = c.superclass() == null ? "java/lang/Object" : internalName(c.superclass());
            if (enumCtor) {
                code.varOp(Code.ALOAD, nameSlot);
                code.varOp(Code.ILOAD, ordinalSlot);
            }
            if (sup != null && sup.owner.hasOuterInstance && sup.owner.decl != null) {
                code.varOp(Code.ALOAD, superOuterSlot);
            } else if (superOuterSlot >= 0) {
                code.varOp(Code.ALOAD, superOuterSlot);
            }
            for (int i = 0; i < paramSlots.size(); i++) {
                Type pt = erased(m.params.get(i));
                code.varOp(loadOp(pt), paramSlots.get(i).intValue());
            }
            if (sup != null) {
                code.op2(Code.INVOKESPECIAL, pool.method(superOwner, "<init>", superCtorDescriptor(sup), false));
            } else {
                code.op2(Code.INVOKESPECIAL, pool.method("java/lang/Object", "<init>", "()V", false));
            }
        } else {
            genImplicitSuper(m);
        }
        prologue = false;
        if (!callsThis) {
            genInstanceInitializers(c);
        }
        if (d != null) {
            List<Tree> stats = d.body.stats;
            for (int i = explicit != null ? 1 : 0; i < stats.size(); i++) {
                genStat(stats.get(i));
            }
        }
        if (c.isRecord() && (d == null || d.compactConstructor) && !callsThis && code.alive) {
            // Canonical constructor: assign the fields from the (possibly reassigned) parameters.
            for (int i = 0; i < c.recordComponents.size(); i++) {
                VarSymbol comp = c.recordComponents.get(i);
                Type t = erased(comp.type);
                code.varOp(Code.ALOAD, 0);
                code.varOp(loadOp(t), paramSlots.get(i).intValue());
                code.op2(Code.PUTFIELD, pool.field(c.internalName, comp.name, desc(t)));
            }
        }
        if (code.alive) {
            code.op(Code.RETURN);
        }
        int access = methodAccess(m);
        if (c.isEnum() && !c.anonymous) {
            access = access & ~(Symbol.ACC_PUBLIC | Symbol.ACC_PROTECTED) | Symbol.ACC_PRIVATE;
        }
        if (d == null && !c.isEnum()) {
            access = c.anonymous ? 0 : access & (Symbol.ACC_PUBLIC | Symbol.ACC_PROTECTED | Symbol.ACC_PRIVATE);
        }
        endMethod(access, "<init>", descriptor, false, true);
    }

    /** A superclass constructor's descriptor (its own synthetic parameters included). */
    private String superCtorDescriptor(MethodSymbol sup) {
        return jvmDescriptor(sup);
    }

    private void genImplicitSuper(MethodSymbol m) {
        MethodSymbol sup = m.superCtor;
        Type st = cls.superclass();
        if (st == null) {
            return;
        }
        code.varOp(Code.ALOAD, 0);
        if (sup == null) {
            code.op2(Code.INVOKESPECIAL, pool.method(internalName(st), "<init>", "()V", false));
            return;
        }
        ClassSymbol s = sup.owner;
        if (s.hasOuterInstance) {
            loadInstance(s.outer, 0);
        }
        if (s.decl != null && s.isEnum()) {
            throw new CompileError("enum superclass");
        }
        if (s.decl != null) {
            // A local superclass's captured variables.
            for (VarSymbol v : s.capturedVars) {
                loadVar(v);
            }
        }
        if (sup.isVarargs() && sup.params.size() == 1) {
            Type arr = erased(sup.params.get(0));
            code.iconst(0);
            newArray(((Type.ArrayType) arr).elem);
        }
        code.op2(Code.INVOKESPECIAL, pool.method(internalName(st), "<init>", jvmDescriptor(sup), false));
    }

    private void genExplicitConstructorCall(Tree.MethodCall call, int nameSlot, int ordinalSlot) {
        MethodSymbol target = call.sym;
        ClassSymbol owner = target.owner;
        code.varOp(Code.ALOAD, 0);
        if (owner.decl != null && (owner.isEnum() && !owner.anonymous)) {
            code.varOp(Code.ALOAD, nameSlot);
            code.varOp(Code.ILOAD, ordinalSlot);
        }
        if (owner.hasOuterInstance) {
            if (call.superCall && call.receiver != null) {
                Type t = genExpr(call.receiver);
                nullCheck();
                coerce(t, owner.outer.erasure());
            } else if (!call.superCall) {
                code.varOp(Code.ALOAD, outerParamSlot);
            } else {
                loadInstance(owner.outer, 0);
            }
        }
        genArgs(target, call.args, call.varargsCall);
        if (owner.decl != null) {
            for (VarSymbol v : owner.capturedVars) {
                if (owner == cls) {
                    code.varOp(loadOp(erased(v.type)), capturedParamSlots.get(v).intValue());
                } else {
                    loadVar(v);
                }
            }
        }
        code.op2(Code.INVOKESPECIAL, pool.method(owner.internalName, "<init>", jvmDescriptor(target), false));
    }

    private void genInstanceInitializers(ClassSymbol c) {
        for (Tree member : c.decl.members) {
            if (member instanceof Tree.VarDef) {
                Tree.VarDef v = (Tree.VarDef) member;
                if (v.sym == null || v.sym.isStatic() || v.init == null) {
                    continue;
                }
                code.line(line(v.pos));
                code.varOp(Code.ALOAD, 0);
                Type t = erased(v.sym.type);
                genExprAs(v.init, t);
                code.op2(Code.PUTFIELD, pool.field(c.internalName, v.name, desc(t)));
            } else if (member instanceof Tree.Block && !((Tree.Block) member).isStatic) {
                genStat(member);
            }
        }
    }

    // ------------------------------------------------------------------ static initializer

    private void genClassInit(ClassSymbol c) {
        boolean needed = c.isEnum() && !c.anonymous || usesAssert;
        for (Tree member : c.decl.members) {
            if (member instanceof Tree.VarDef) {
                Tree.VarDef v = (Tree.VarDef) member;
                if (v.sym != null && v.sym.isStatic() && v.init != null && !(v.sym.isFinal() && v.sym.constValue != null)) {
                    needed = true;
                }
            } else if (member instanceof Tree.Block && ((Tree.Block) member).isStatic) {
                needed = true;
            }
        }
        if (!needed) {
            return;
        }
        MethodSymbol clinit = new MethodSymbol("<clinit>", Symbol.ACC_STATIC, c);
        clinit.returnType = Type.VOID;
        beginMethod(clinit, true);
        if (usesAssert) {
            // $assertionsDisabled = !Outermost.class.desiredAssertionStatus(), first, so a static
            // initializer that asserts already sees it. The outermost class is asked, as javac
            // does, so -ea:pkg.Outer enables the asserts of its nested classes too.
            ClassSymbol top = c;
            while (top.outer != null) {
                top = top.outer;
            }
            code.ldc(pool.cls(top.internalName));
            code.op2(Code.INVOKEVIRTUAL, pool.method("java/lang/Class", "desiredAssertionStatus", "()Z", false));
            code.iconst(1);
            code.op(Code.IXOR);
            code.op2(Code.PUTSTATIC, pool.field(c.internalName, ASSERTIONS_DISABLED, "Z"));
        }
        int ordinal = 0;
        List<VarSymbol> constants = new ArrayList<VarSymbol>();
        boolean valuesDone = !(c.isEnum() && !c.anonymous);
        for (Tree member : c.decl.members) {
            if (member instanceof Tree.VarDef) {
                Tree.VarDef v = (Tree.VarDef) member;
                VarSymbol f = v.sym;
                if (f == null || !f.isStatic()) {
                    continue;
                }
                if ((f.flags & Symbol.ACC_ENUM) != 0) {
                    code.line(line(v.pos));
                    genEnumConstant(c, v, ordinal++);
                    constants.add(f);
                    continue;
                }
                if (!valuesDone) {
                    genValuesInit(c, constants);
                    valuesDone = true;
                }
                if (v.init == null || f.isFinal() && f.constValue != null) {
                    continue;
                }
                code.line(line(v.pos));
                Type t = erased(f.type);
                genExprAs(v.init, t);
                code.op2(Code.PUTSTATIC, pool.field(c.internalName, f.name, desc(t)));
            } else if (member instanceof Tree.Block && ((Tree.Block) member).isStatic) {
                if (!valuesDone) {
                    genValuesInit(c, constants);
                    valuesDone = true;
                }
                genStat(member);
            }
        }
        if (!valuesDone) {
            genValuesInit(c, constants);
        }
        if (code.alive) {
            code.op(Code.RETURN);
        }
        endMethod(Symbol.ACC_STATIC, "<clinit>", "()V", true, false);
    }

    private void genEnumConstant(ClassSymbol c, Tree.VarDef v, int ordinal) {
        Tree.NewClass nc = (Tree.NewClass) v.init;
        ClassSymbol k = nc.clazzSym != null ? nc.clazzSym : c;
        MethodSymbol ctor = nc.constructor;
        code.op2(Code.NEW, pool.cls(k.internalName));
        code.op(Code.DUP);
        code.ldc(pool.string(v.name));
        code.iconst(ordinal);
        if (ctor != null) {
            genArgs(ctor, nc.args, nc.varargsCall, nc.varargsElem);
            code.op2(Code.INVOKESPECIAL, pool.method(k.internalName, "<init>", jvmDescriptor(ctor), false));
        }
        code.op2(Code.PUTSTATIC, pool.field(c.internalName, v.name, "L" + c.internalName + ";"));
    }

    private void genValuesInit(ClassSymbol c, List<VarSymbol> constants) {
        code.iconst(constants.size());
        code.op2(Code.ANEWARRAY, pool.cls(c.internalName));
        for (int i = 0; i < constants.size(); i++) {
            code.op(Code.DUP);
            code.iconst(i);
            code.op2(Code.GETSTATIC, pool.field(c.internalName, constants.get(i).name, "L" + c.internalName + ";"));
            code.op(Code.AASTORE);
        }
        code.op2(Code.PUTSTATIC, pool.field(c.internalName, "$VALUES", "[L" + c.internalName + ";"));
    }

    // ------------------------------------------------------------------ enum and record members

    private void genEnumValues(MethodSymbol m) {
        beginMethod(m, true);
        String arr = "[L" + cls.internalName + ";";
        code.op2(Code.GETSTATIC, pool.field(cls.internalName, "$VALUES", arr));
        code.op2(Code.INVOKEVIRTUAL, pool.method(arr, "clone", "()Ljava/lang/Object;", false));
        code.op2(Code.CHECKCAST, pool.cls(arr));
        code.op(Code.ARETURN);
        endMethod(Symbol.ACC_PUBLIC | Symbol.ACC_STATIC, "values", "()" + arr, true, false);
    }

    private void genEnumValueOf(MethodSymbol m) {
        beginMethod(m, true);
        code.newLocal(syms.stringType);
        code.ldc(pool.cls(cls.internalName));
        code.varOp(Code.ALOAD, 0);
        code.op2(Code.INVOKESTATIC, pool.method("java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false));
        code.op2(Code.CHECKCAST, pool.cls(cls.internalName));
        code.op(Code.ARETURN);
        endMethod(Symbol.ACC_PUBLIC | Symbol.ACC_STATIC, "valueOf", "(Ljava/lang/String;)L" + cls.internalName + ";", true, false);
    }

    /** A static accessor for Outer.super.m(): (Outer self, args...) -> invokespecial Super.m. */
    private void genSuperAccessor(MethodSymbol acc) {
        beginMethod(acc, true);
        MethodSymbol target = acc.superAccessTarget;
        for (Type p : acc.params) {
            Type e = erased(p);
            code.varOp(loadOp(e), code.newLocal(e));
        }
        ClassSymbol q = acc.superAccessQualifier != null ? acc.superAccessQualifier : target.owner;
        invoke(Code.INVOKESPECIAL, q.internalName, target.name, target.descriptor(), q.isInterface());
        code.op(returnOp(erased(target.returnType)));
        endMethod(Symbol.ACC_STATIC | Symbol.ACC_SYNTHETIC, acc.name, acc.descriptor(), true, false);
    }

    private void genRecordAccessor(MethodSymbol m) {
        beginMethod(m, false);
        Type t = erased(m.returnType);
        code.varOp(Code.ALOAD, 0);
        code.op2(Code.GETFIELD, pool.field(cls.internalName, m.name, desc(t)));
        code.op(returnOp(t));
        endMethod(Symbol.ACC_PUBLIC, m.name, "()" + desc(t), false, false);
    }

    private void genRecordToString(MethodSymbol m) {
        beginMethod(m, false);
        newStringBuilder();
        code.ldc(pool.string(cls.simpleName + "["));
        appendTo(syms.stringType);
        List<VarSymbol> comps = cls.recordComponents;
        for (int i = 0; i < comps.size(); i++) {
            VarSymbol comp = comps.get(i);
            code.ldc(pool.string((i > 0 ? ", " : "") + comp.name + "="));
            appendTo(syms.stringType);
            Type t = erased(comp.type);
            code.varOp(Code.ALOAD, 0);
            code.op2(Code.GETFIELD, pool.field(cls.internalName, comp.name, desc(t)));
            appendTo(t);
        }
        code.ldc(pool.string("]"));
        appendTo(syms.stringType);
        code.op2(Code.INVOKEVIRTUAL, pool.method("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
        code.op(Code.ARETURN);
        endMethod(Symbol.ACC_PUBLIC | Symbol.ACC_FINAL, "toString", "()Ljava/lang/String;", false, false);
    }

    private void genRecordHashCode(MethodSymbol m) {
        beginMethod(m, false);
        code.iconst(0);
        for (VarSymbol comp : cls.recordComponents) {
            code.iconst(31);
            code.op(Code.IMUL);
            Type t = erased(comp.type);
            code.varOp(Code.ALOAD, 0);
            code.op2(Code.GETFIELD, pool.field(cls.internalName, comp.name, desc(t)));
            if (t.isPrimitive()) {
                String box = internalName(syms.boxedType(t));
                code.op2(Code.INVOKESTATIC, pool.method(box, "hashCode", "(" + desc(t) + ")I", false));
            } else {
                code.op2(Code.INVOKESTATIC, pool.method("java/util/Objects", "hashCode", "(Ljava/lang/Object;)I", false));
            }
            code.op(Code.IADD);
        }
        code.op(Code.IRETURN);
        endMethod(Symbol.ACC_PUBLIC | Symbol.ACC_FINAL, "hashCode", "()I", false, false);
    }

    private void genRecordEquals(MethodSymbol m) {
        beginMethod(m, false);
        code.newLocal(syms.objectType);
        Code.Label isFalse = new Code.Label();
        Code.Label notSame = new Code.Label();
        code.varOp(Code.ALOAD, 0);
        code.varOp(Code.ALOAD, 1);
        code.jump(Code.IF_ACMPNE, notSame);
        code.iconst(1);
        code.op(Code.IRETURN);
        code.place(notSame);
        code.varOp(Code.ALOAD, 1);
        code.op2(Code.INSTANCEOF, pool.cls(cls.internalName));
        code.jump(Code.IFEQ, isFalse);
        int other = code.newLocal(cls.erasure());
        code.varOp(Code.ALOAD, 1);
        code.op2(Code.CHECKCAST, pool.cls(cls.internalName));
        code.varOp(Code.ASTORE, other);
        for (VarSymbol comp : cls.recordComponents) {
            Type t = erased(comp.type);
            String fd = desc(t);
            int fref = pool.field(cls.internalName, comp.name, fd);
            code.varOp(Code.ALOAD, 0);
            code.op2(Code.GETFIELD, fref);
            code.varOp(Code.ALOAD, other);
            code.op2(Code.GETFIELD, fref);
            switch (t.tag) {
                case FLOAT:
                    code.op2(Code.INVOKESTATIC, pool.method("java/lang/Float", "compare", "(FF)I", false));
                    code.jump(Code.IFNE, isFalse);
                    break;
                case DOUBLE:
                    code.op2(Code.INVOKESTATIC, pool.method("java/lang/Double", "compare", "(DD)I", false));
                    code.jump(Code.IFNE, isFalse);
                    break;
                case LONG:
                    code.op(Code.LCMP);
                    code.jump(Code.IFNE, isFalse);
                    break;
                case BOOLEAN:
                case BYTE:
                case SHORT:
                case CHAR:
                case INT:
                    code.jump(Code.IF_ICMPNE, isFalse);
                    break;
                default:
                    code.op2(Code.INVOKESTATIC, pool.method("java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false));
                    code.jump(Code.IFEQ, isFalse);
                    break;
            }
        }
        code.iconst(1);
        code.op(Code.IRETURN);
        code.place(isFalse);
        code.iconst(0);
        code.op(Code.IRETURN);
        endMethod(Symbol.ACC_PUBLIC | Symbol.ACC_FINAL, "equals", "(Ljava/lang/Object;)Z", false, false);
    }

    // ------------------------------------------------------------------ bridges

    /** Bridge methods: an override whose erasure differs from the method it overrides. */
    private void genBridges(ClassSymbol c) {
        if (c.isInterface()) {
            return;
        }
        Set<String> own = new LinkedHashSet<String>();
        for (MethodSymbol m : c.methods) {
            if (!m.isConstructor()) {
                own.add(m.name + jvmDescriptor(m));
            }
        }
        List<ClassSymbol> supers = new ArrayList<ClassSymbol>();
        collectSupertypes(c, supers);
        for (MethodSymbol m : new ArrayList<MethodSymbol>(c.methods)) {
            if (m.isConstructor() || m.isStatic() || m.isPrivate() || "<clinit>".equals(m.name)) {
                continue;
            }
            String mdesc = jvmDescriptor(m);
            for (ClassSymbol s : supers) {
                for (MethodSymbol sm : s.methods()) {
                    if (!sm.name.equals(m.name) || sm.params.size() != m.params.size() || sm.isStatic() || sm.isPrivate()
                            || sm.isConstructor()) {
                        continue;
                    }
                    String sdesc = sm.owner.decl == null && sm.descriptor != null ? sm.descriptor : jvmDescriptor(sm);
                    if (sdesc.equals(mdesc) || own.contains(m.name + sdesc) || !overridesIn(m, sm, c)) {
                        continue;
                    }
                    own.add(m.name + sdesc);
                    genBridge(m, sdesc);
                }
            }
        }
    }

    private void collectSupertypes(ClassSymbol c, List<ClassSymbol> out) {
        Type sup = c.superclass();
        if (sup != null && sup.tag == Type.Tag.CLASS) {
            ClassSymbol s = ((Type.ClassType) sup).sym;
            if (!out.contains(s)) {
                out.add(s);
                collectSupertypes(s, out);
            }
        }
        for (Type i : c.interfaces()) {
            if (i.tag == Type.Tag.CLASS) {
                ClassSymbol s = ((Type.ClassType) i).sym;
                if (!out.contains(s)) {
                    out.add(s);
                    collectSupertypes(s, out);
                }
            }
        }
    }

    /** Does m (in c) override sm once sm's class type parameters are instantiated as c sees them? */
    private boolean overridesIn(MethodSymbol m, MethodSymbol sm, ClassSymbol c) {
        Map<Type.TypeVar, Type> map = types.memberMapping(c.thisType(), sm.owner);
        for (int i = 0; i < m.params.size(); i++) {
            Type a = erased(m.params.get(i));
            Type b = erased(Type.substitute(sm.params.get(i), map));
            if (!desc(a).equals(desc(b))) {
                return false;
            }
        }
        return true;
    }

    private void genBridge(MethodSymbol m, String bridgeDesc) {
        beginMethod(m, false);
        List<String> args = FrameComputer.argDescriptors(bridgeDesc);
        int[] argSlots = new int[args.size()];
        for (int i = 0; i < args.size(); i++) {
            argSlots[i] = code.newLocal(typeForDescriptor(args.get(i)));
        }
        code.varOp(Code.ALOAD, 0);
        for (int i = 0; i < args.size(); i++) {
            Type from = typeForDescriptor(args.get(i));
            code.varOp(loadOp(from), argSlots[i]);
            coerce(from, erased(m.params.get(i)));
        }
        String mdesc = jvmDescriptor(m);
        code.op2(Code.INVOKEVIRTUAL, pool.method(cls.internalName, m.name, mdesc, false));
        Type ret = typeForDescriptor(FrameComputer.returnDescriptor(bridgeDesc));
        Type mret = erased(m.returnType);
        if (ret.tag == Type.Tag.VOID) {
            code.op(Code.RETURN);
        } else {
            coerce(mret, ret);
            code.op(returnOp(ret));
        }
        endMethod(Symbol.ACC_PUBLIC | Symbol.ACC_SYNTHETIC | Symbol.ACC_BRIDGE, m.name, bridgeDesc, false, false);
    }

    /** A Type for an erased field descriptor (for bridges and synthetic code). */
    private Type typeForDescriptor(String d) {
        switch (d.charAt(0)) {
            case 'Z': return Type.BOOLEAN;
            case 'B': return Type.BYTE;
            case 'C': return Type.CHAR;
            case 'S': return Type.SHORT;
            case 'I': return Type.INT;
            case 'J': return Type.LONG;
            case 'F': return Type.FLOAT;
            case 'D': return Type.DOUBLE;
            case 'V': return Type.VOID;
            case '[': return new Type.ArrayType(typeForDescriptor(d.substring(1)));
            default: return syms.require(d.substring(1, d.length() - 1)).erasure();
        }
    }

    // ================================================================== type helpers

    static int loadOp(Type t) {
        switch (t.tag) {
            case BOOLEAN: case BYTE: case SHORT: case CHAR: case INT: return Code.ILOAD;
            case LONG: return Code.LLOAD;
            case FLOAT: return Code.FLOAD;
            case DOUBLE: return Code.DLOAD;
            default: return Code.ALOAD;
        }
    }

    static int storeOp(Type t) {
        return loadOp(t) - Code.ILOAD + Code.ISTORE;
    }

    static int returnOp(Type t) {
        switch (t.tag) {
            case VOID: return Code.RETURN;
            case BOOLEAN: case BYTE: case SHORT: case CHAR: case INT: return Code.IRETURN;
            case LONG: return Code.LRETURN;
            case FLOAT: return Code.FRETURN;
            case DOUBLE: return Code.DRETURN;
            default: return Code.ARETURN;
        }
    }

    static int arrayLoadOp(Type elem) {
        switch (elem.tag) {
            case BOOLEAN: case BYTE: return Code.BALOAD;
            case SHORT: return Code.SALOAD;
            case CHAR: return Code.CALOAD;
            case INT: return Code.IALOAD;
            case LONG: return Code.LALOAD;
            case FLOAT: return Code.FALOAD;
            case DOUBLE: return Code.DALOAD;
            default: return Code.AALOAD;
        }
    }

    static int arrayStoreOp(Type elem) {
        return arrayLoadOp(elem) - Code.IALOAD + Code.IASTORE;
    }

    private void pop(Type t) {
        if (t.tag == Type.Tag.VOID) {
            return;
        }
        code.op(t.slots() == 2 ? Code.POP2 : Code.POP);
    }

    private void dup(Type t) {
        code.op(t.slots() == 2 ? Code.DUP2 : Code.DUP);
    }

    /** dup_x1 / dup_x2 of a value of type t under one (or two) stack words. */
    private void dupX(Type t, int under) {
        boolean wide = t.slots() == 2;
        if (under == 1) {
            code.op(wide ? Code.DUP2_X1 : Code.DUP_X1);
        } else {
            code.op(wide ? Code.DUP2_X2 : Code.DUP_X2);
        }
    }

    private void newArray(Type elem) {
        Type e = erased(elem);
        if (e.isPrimitive()) {
            int atype;
            switch (e.tag) {
                case BOOLEAN: atype = 4; break;
                case CHAR: atype = 5; break;
                case FLOAT: atype = 6; break;
                case DOUBLE: atype = 7; break;
                case BYTE: atype = 8; break;
                case SHORT: atype = 9; break;
                case INT: atype = 10; break;
                default: atype = 11; break;
            }
            code.op1(Code.NEWARRAY, atype);
        } else {
            code.op2(Code.ANEWARRAY, pool.cls(internalName(e)));
        }
    }

    private void nullCheck() {
        code.op(Code.DUP);
        code.op2(Code.INVOKESTATIC, pool.method("java/util/Objects", "requireNonNull", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
        code.op(Code.POP);
    }

    private void pushConst(Object v, Type t) {
        Object c = Constants.cast(v, t.isPrimitive() || t.tag == Type.Tag.CLASS ? t : Type.INT);
        if (c == null) {
            c = v;
        }
        if (c instanceof String) {
            code.ldc(pool.string((String) c));
        } else if (c instanceof Boolean) {
            code.iconst(((Boolean) c).booleanValue() ? 1 : 0);
        } else if (c instanceof Long) {
            long l = ((Long) c).longValue();
            if (l == 0L || l == 1L) {
                code.op(Code.LCONST_0 + (int) l);
            } else {
                code.ldc2(pool.longConst(l));
            }
        } else if (c instanceof Float) {
            float f = ((Float) c).floatValue();
            if ((f == 0.0f && Float.floatToRawIntBits(f) == 0) || f == 1.0f || f == 2.0f) {
                code.op(Code.FCONST_0 + (int) f);
            } else {
                code.ldc(pool.floatConst(f));
            }
        } else if (c instanceof Double) {
            double d = ((Double) c).doubleValue();
            if ((d == 0.0 && Double.doubleToRawLongBits(d) == 0L) || d == 1.0) {
                code.op(Code.DCONST_0 + (int) d);
            } else {
                code.ldc2(pool.doubleConst(d));
            }
        } else {
            code.iconst(Constants.intValue(c));
        }
    }

    /** Converts the value on the stack from erased type {@code from} to erased type {@code to}. */
    void coerce(Type from, Type to) {
        if (from == null || to == null || from.tag == Type.Tag.VOID || to.tag == Type.Tag.VOID || from.isErroneous()
                || to.isErroneous()) {
            return;
        }
        if (from.isPrimitive() && to.isPrimitive()) {
            convertPrimitive(from, to);
            return;
        }
        if (from.isPrimitive()) {
            Type target = syms.unboxedType(to);
            Type prim = target != null ? target : from;
            convertPrimitive(from, prim);
            box(prim);
            Type boxed = syms.boxedType(prim);
            if (!isSubclass(boxed, to)) {
                code.op2(Code.CHECKCAST, pool.cls(internalName(to)));
            }
            return;
        }
        if (to.isPrimitive()) {
            Type unboxed = syms.unboxedType(from);
            if (unboxed == null) {
                Type box = syms.boxedType(to);
                code.op2(Code.CHECKCAST, pool.cls(internalName(box)));
                unbox(to);
                return;
            }
            unbox(unboxed);
            convertPrimitive(unboxed, to);
            return;
        }
        if (from.tag == Type.Tag.NULL) {
            return;
        }
        if (!isSubclass(from, to)) {
            code.op2(Code.CHECKCAST, pool.cls(internalName(to)));
        }
    }

    /** Erased reference subtyping, for deciding whether a checkcast is needed. */
    private boolean isSubclass(Type from, Type to) {
        Type e = erased(to);
        if (e.tag == Type.Tag.CLASS && ((Type.ClassType) e).sym == syms.objectSym) {
            return true;
        }
        Type f = erased(from);
        if (f.tag == Type.Tag.NULL) {
            return true;
        }
        if (f.tag == Type.Tag.ARRAY || e.tag == Type.Tag.ARRAY) {
            if (f.tag != Type.Tag.ARRAY || e.tag != Type.Tag.ARRAY) {
                return false;
            }
            Type fe = ((Type.ArrayType) f).elem;
            Type ee = ((Type.ArrayType) e).elem;
            if (fe.isPrimitive() || ee.isPrimitive()) {
                return fe.tag == ee.tag;
            }
            return isSubclass(fe, ee);
        }
        if (f.tag != Type.Tag.CLASS || e.tag != Type.Tag.CLASS) {
            return false;
        }
        return types.isSubClass(((Type.ClassType) f).sym, ((Type.ClassType) e).sym);
    }

    private void box(Type prim) {
        Type boxed = syms.boxedType(prim);
        String owner = internalName(boxed);
        code.op2(Code.INVOKESTATIC, pool.method(owner, "valueOf", "(" + desc(prim) + ")L" + owner + ";", false));
    }

    private void unbox(Type prim) {
        Type boxed = syms.boxedType(prim);
        String owner = internalName(boxed);
        code.op2(Code.INVOKEVIRTUAL, pool.method(owner, ((Type.Prim) prim).name + "Value", "()" + desc(prim), false));
    }

    private void convertPrimitive(Type from, Type to) {
        if (from.tag == to.tag) {
            return;
        }
        int f = kind(from);
        int t = kind(to);
        if (f != t) {
            // I=0 L=1 F=2 D=3
            int[][] ops = {
                {-1, Code.I2L, Code.I2F, Code.I2D},
                {Code.L2I, -1, Code.L2F, Code.L2D},
                {Code.F2I, Code.F2L, -1, Code.F2D},
                {Code.D2I, Code.D2L, Code.D2F, -1}
            };
            code.op(ops[f][t]);
        }
        if (t == 0) {
            switch (to.tag) {
                case BYTE:
                    if (from.tag != Type.Tag.BYTE) {
                        code.op(Code.I2B);
                    }
                    break;
                case SHORT:
                    if (from.tag != Type.Tag.SHORT && from.tag != Type.Tag.BYTE) {
                        code.op(Code.I2S);
                    }
                    break;
                case CHAR:
                    if (from.tag != Type.Tag.CHAR) {
                        code.op(Code.I2C);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private static int kind(Type t) {
        switch (t.tag) {
            case LONG: return 1;
            case FLOAT: return 2;
            case DOUBLE: return 3;
            default: return 0;
        }
    }

    // ================================================================== statements

    /** A break/continue/yield target. */
    private static final class Target {
        Tree node;
        String label;
        Code.Label breakLabel;
        Code.Label continueLabel;
        /** For a switch expression: the type its arms leave on the stack. */
        Type yieldType;
    }

    /** A protected region whose exits must run code (finally, synchronized, try-with-resources). */
    private abstract class Fin {
        /** Ranges protected by the catch clauses (open only while in the try body). */
        Region catchRegion;
        /** Ranges protected by the catch-any handler. */
        Region anyRegion;
        boolean reopenCatch;
        boolean reopenAny;

        abstract void genExit();
    }

    private static final class Region {
        final List<int[]> ranges = new ArrayList<int[]>();
        int openAt = -1;

        void open(int pc) {
            openAt = pc;
        }

        boolean isOpen() {
            return openAt >= 0;
        }

        void close(int pc) {
            if (openAt >= 0 && pc > openAt) {
                ranges.add(new int[]{openAt, pc});
            }
            openAt = -1;
        }
    }

    void genStats(List<Tree> stats) {
        for (Tree s : stats) {
            genStat(s);
        }
    }

    void genStat(Tree t) {
        if (t == null || !code.alive) {
            return;
        }
        if (!(t instanceof Tree.Block)) {
            code.line(line(t.pos));
        }
        if (t instanceof Tree.Block) {
            genStats(((Tree.Block) t).stats);
        } else if (t instanceof Tree.VarDef) {
            Tree.VarDef v = (Tree.VarDef) t;
            Type st = erased(v.sym.type);
            int s = declareLocal(v.sym, st);
            if (v.init != null) {
                genExprAs(v.init, st);
                code.varOp(storeOp(st), s);
            }
        } else if (t instanceof Tree.ExpressionStatement) {
            genEffect(((Tree.ExpressionStatement) t).expr);
        } else if (t instanceof Tree.If) {
            genIf((Tree.If) t);
        } else if (t instanceof Tree.WhileLoop) {
            genWhile((Tree.WhileLoop) t, null);
        } else if (t instanceof Tree.DoLoop) {
            genDo((Tree.DoLoop) t, null);
        } else if (t instanceof Tree.ForLoop) {
            genFor((Tree.ForLoop) t, null);
        } else if (t instanceof Tree.ForEach) {
            genForEach((Tree.ForEach) t, null);
        } else if (t instanceof Tree.Labeled) {
            genLabeled((Tree.Labeled) t);
        } else if (t instanceof Tree.Switch) {
            genSwitch((Tree.Switch) t);
        } else if (t instanceof Tree.Return) {
            genReturn((Tree.Return) t);
        } else if (t instanceof Tree.Break) {
            Target target = findTarget(((Tree.Break) t).label, false);
            if (target != null) {
                jumpOut(target, target.breakLabel);
            }
        } else if (t instanceof Tree.Continue) {
            Target target = findTarget(((Tree.Continue) t).label, true);
            if (target != null) {
                jumpOut(target, target.continueLabel);
            }
        } else if (t instanceof Tree.Yield) {
            genYield((Tree.Yield) t);
        } else if (t instanceof Tree.Throw) {
            Type et = genExpr(((Tree.Throw) t).expr);
            coerce(et, syms.type("java/lang/Throwable"));
            code.op(Code.ATHROW);
        } else if (t instanceof Tree.Try) {
            genTry((Tree.Try) t);
        } else if (t instanceof Tree.Synchronized) {
            genSynchronized((Tree.Synchronized) t);
        } else if (t instanceof Tree.Assert) {
            genAssert((Tree.Assert) t);
        } else if (t instanceof Tree.Empty || t instanceof Tree.ClassDecl) {
            // Local classes are generated on their own.
        } else {
            genEffect(t);
        }
    }

    private static final String ASSERTIONS_DISABLED = "$assertionsDisabled";

    /**
     * {@code if (!$assertionsDisabled && !cond) throw new AssertionError(detail);} -- the check is
     * emitted whatever the target does with it: whether assertions run is decided when the class
     * is loaded ({@code -ea} on the JVM), not when it is compiled.
     */
    private void genAssert(Tree.Assert a) {
        Code.Label end = new Code.Label();
        code.op2(Code.GETSTATIC, pool.field(cls.internalName, ASSERTIONS_DISABLED, "Z"));
        code.jump(Code.IFNE, end);
        genCond(a.cond, end, true);
        code.op2(Code.NEW, pool.cls("java/lang/AssertionError"));
        code.op(Code.DUP);
        String ctor = "()V";
        if (a.detail != null) {
            Type t = genExpr(a.detail);
            switch (t.tag) {
                case BOOLEAN: ctor = "(Z)V"; break;
                case CHAR: ctor = "(C)V"; break;
                case BYTE: case SHORT: case INT: ctor = "(I)V"; break;
                case LONG: ctor = "(J)V"; break;
                case FLOAT: ctor = "(F)V"; break;
                case DOUBLE: ctor = "(D)V"; break;
                default: ctor = "(Ljava/lang/Object;)V"; break;
            }
        }
        code.op2(Code.INVOKESPECIAL, pool.method("java/lang/AssertionError", "<init>", ctor, false));
        code.op(Code.ATHROW);
        code.place(end);
    }

    /** Whether {@code decl} has an {@code assert} of its own: nested and anonymous classes are separate classes. */
    private static boolean containsAssert(final Tree.ClassDecl decl) {
        final boolean[] found = new boolean[1];
        new TreeScanner() {
            @Override
            void scan(Tree t) {
                if (found[0] || t == null) {
                    return;
                }
                if (t instanceof Tree.Assert) {
                    found[0] = true;
                } else if (t instanceof Tree.ClassDecl && t != decl) {
                    return;
                } else if (t instanceof Tree.NewClass) {
                    scan(((Tree.NewClass) t).outer);
                    scan(((Tree.NewClass) t).args);
                    return;
                }
                super.scan(t);
            }
        }.scan(decl);
        return found[0];
    }

    /** Evaluates an expression for its side effects only. */
    private void genEffect(Tree e) {
        Tree x = e;
        while (x instanceof Tree.Parens) {
            x = ((Tree.Parens) x).expr;
        }
        if (x instanceof Tree.Assign) {
            genAssign((Tree.Assign) x, false);
            return;
        }
        if (x instanceof Tree.CompoundAssign) {
            genCompoundAssign((Tree.CompoundAssign) x, false);
            return;
        }
        if (x instanceof Tree.Unary && (((Tree.Unary) x).op == Token.Kind.PLUSPLUS || ((Tree.Unary) x).op == Token.Kind.SUBSUB)) {
            genIncrement((Tree.Unary) x, false);
            return;
        }
        Type t = genExpr(x);
        pop(t);
    }

    private void genIf(Tree.If s) {
        if (s.cond.constant instanceof Boolean) {
            if (((Boolean) s.cond.constant).booleanValue()) {
                genStat(s.thenPart);
            } else if (s.elsePart != null) {
                genStat(s.elsePart);
            }
            return;
        }
        Code.Label elseL = new Code.Label();
        Code.Label end = new Code.Label();
        genCond(s.cond, elseL, false);
        genStat(s.thenPart);
        if (s.elsePart != null) {
            if (code.alive) {
                code.jump(Code.GOTO, end);
            }
            code.place(elseL);
            genStat(s.elsePart);
            code.place(end);
        } else {
            code.place(elseL);
        }
    }

    private Target pushLoop(Tree node, String label) {
        Target t = new Target();
        t.node = node;
        t.label = label;
        t.breakLabel = new Code.Label();
        t.continueLabel = new Code.Label();
        jumpStack.add(t);
        return t;
    }

    private void popTarget(Target t) {
        jumpStack.remove(t);
    }

    private void genWhile(Tree.WhileLoop w, String label) {
        Target t = pushLoop(w, label);
        Code.Label start = t.continueLabel;
        code.place(start);
        code.alive = true;
        if (!Attr.isTrue(w.cond)) {
            genCond(w.cond, t.breakLabel, false);
        }
        genStat(w.body);
        if (code.alive) {
            code.jump(Code.GOTO, start);
        }
        popTarget(t);
        code.place(t.breakLabel);
    }

    private void genDo(Tree.DoLoop d, String label) {
        Target t = pushLoop(d, label);
        Code.Label start = new Code.Label();
        code.place(start);
        start.jumpedTo = true;
        genStat(d.body);
        code.place(t.continueLabel);
        if (code.alive) {
            if (Attr.isTrue(d.cond)) {
                code.jump(Code.GOTO, start);
            } else {
                genCond(d.cond, start, true);
            }
        }
        popTarget(t);
        code.place(t.breakLabel);
    }

    private void genFor(Tree.ForLoop f, String label) {
        for (Tree i : f.init) {
            genStat(i);
        }
        Target t = pushLoop(f, label);
        Code.Label start = new Code.Label();
        code.place(start);
        start.jumpedTo = true;
        if (f.cond != null && !Attr.isTrue(f.cond)) {
            genCond(f.cond, t.breakLabel, false);
        }
        genStat(f.body);
        code.place(t.continueLabel);
        if (code.alive) {
            for (Tree s : f.step) {
                genEffect(s instanceof Tree.ExpressionStatement ? ((Tree.ExpressionStatement) s).expr : s);
            }
            code.jump(Code.GOTO, start);
        }
        popTarget(t);
        code.place(t.breakLabel);
    }

    private void genForEach(Tree.ForEach f, String label) {
        Type exprType = genExpr(f.expr);
        VarSymbol v = f.var.sym;
        Type vt = erased(v.type);
        if (exprType.tag == Type.Tag.ARRAY || erased(f.expr.type).tag == Type.Tag.ARRAY) {
            Type arrType = exprType.tag == Type.Tag.ARRAY ? exprType : erased(f.expr.type);
            coerce(exprType, arrType);
            int arr = code.newLocal(arrType);
            code.varOp(Code.ASTORE, arr);
            int len = code.newLocal(Type.INT);
            code.varOp(Code.ALOAD, arr);
            code.op(Code.ARRAYLENGTH);
            code.varOp(Code.ISTORE, len);
            int idx = code.newLocal(Type.INT);
            code.iconst(0);
            code.varOp(Code.ISTORE, idx);
            Target t = pushLoop(f, label);
            Code.Label start = new Code.Label();
            code.place(start);
            start.jumpedTo = true;
            code.varOp(Code.ILOAD, idx);
            code.varOp(Code.ILOAD, len);
            code.jump(Code.IF_ICMPGE, t.breakLabel);
            Type elem = ((Type.ArrayType) arrType).elem;
            code.varOp(Code.ALOAD, arr);
            code.varOp(Code.ILOAD, idx);
            code.op(arrayLoadOp(elem));
            coerce(elem, vt);
            int vs = declareLocal(v, vt);
            code.varOp(storeOp(vt), vs);
            genStat(f.body);
            code.place(t.continueLabel);
            if (code.alive) {
                code.iinc(idx, 1);
                code.jump(Code.GOTO, start);
            }
            popTarget(t);
            code.place(t.breakLabel);
            return;
        }
        // Iterable.
        Type st = erased(exprType);
        String owner = st.tag == Type.Tag.CLASS ? internalName(st) : "java/lang/Iterable";
        boolean itf = st.tag != Type.Tag.CLASS || ((Type.ClassType) st).sym.isInterface();
        if (st.tag == Type.Tag.CLASS && !types.isSubClass(((Type.ClassType) st).sym, syms.require("java/lang/Iterable"))) {
            code.op2(Code.CHECKCAST, pool.cls("java/lang/Iterable"));
            owner = "java/lang/Iterable";
            itf = true;
        }
        invoke(itf ? Code.INVOKEINTERFACE : Code.INVOKEVIRTUAL, owner, "iterator", "()Ljava/util/Iterator;", itf);
        int it = code.newLocal(syms.type("java/util/Iterator"));
        code.varOp(Code.ASTORE, it);
        Target t = pushLoop(f, label);
        code.place(t.continueLabel);
        t.continueLabel.jumpedTo = true;
        code.alive = true;
        code.varOp(Code.ALOAD, it);
        invoke(Code.INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
        code.jump(Code.IFEQ, t.breakLabel);
        code.varOp(Code.ALOAD, it);
        invoke(Code.INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
        Type elem = erased(f.type == null ? syms.objectType : f.type);
        coerce(syms.objectType, elem.isPrimitive() ? syms.boxedType(elem) : elem);
        coerce(elem.isPrimitive() ? syms.boxedType(elem) : elem, vt);
        int vs = declareLocal(v, vt);
        code.varOp(storeOp(vt), vs);
        genStat(f.body);
        if (code.alive) {
            code.jump(Code.GOTO, t.continueLabel);
        }
        popTarget(t);
        code.place(t.breakLabel);
    }

    private void invoke(int opcode, String owner, String name, String descriptor, boolean itf) {
        int idx = pool.method(owner, name, descriptor, itf);
        if (opcode == Code.INVOKEINTERFACE) {
            int args = 1;
            for (String a : FrameComputer.argDescriptors(descriptor)) {
                args += a.equals("J") || a.equals("D") ? 2 : 1;
            }
            code.bc.u1(Code.INVOKEINTERFACE).u2(idx).u1(args).u1(0);
        } else {
            code.op2(opcode, idx);
        }
    }

    private void genLabeled(Tree.Labeled l) {
        Tree body = l.body;
        if (body instanceof Tree.WhileLoop) {
            genWhile((Tree.WhileLoop) body, l.label);
        } else if (body instanceof Tree.DoLoop) {
            genDo((Tree.DoLoop) body, l.label);
        } else if (body instanceof Tree.ForLoop) {
            genFor((Tree.ForLoop) body, l.label);
        } else if (body instanceof Tree.ForEach) {
            genForEach((Tree.ForEach) body, l.label);
        } else {
            Target t = new Target();
            t.node = l;
            t.label = l.label;
            t.breakLabel = new Code.Label();
            jumpStack.add(t);
            genStat(body);
            popTarget(t);
            code.place(t.breakLabel);
        }
    }

    private Target findTarget(String label, boolean isContinue) {
        for (int i = jumpStack.size() - 1; i >= 0; i--) {
            Object o = jumpStack.get(i);
            if (!(o instanceof Target)) {
                continue;
            }
            Target t = (Target) o;
            if (t.yieldType != null) {
                continue;
            }
            if (label != null) {
                if (label.equals(t.label) && (!isContinue || t.continueLabel != null)) {
                    return t;
                }
                continue;
            }
            if (isContinue ? t.continueLabel != null : t.continueLabel != null || t.node instanceof Tree.Switch) {
                return t;
            }
        }
        return null;
    }

    /** Runs the finalizers between here and target (innermost first), then jumps. */
    private void jumpOut(Object target, Code.Label label) {
        List<Fin> passed = runFinalizersDownTo(target);
        code.jump(Code.GOTO, label);
        reopen(passed);
    }

    private List<Fin> runFinalizersDownTo(Object target) {
        List<Fin> passed = new ArrayList<Fin>();
        for (int i = jumpStack.size() - 1; i >= 0; i--) {
            Object o = jumpStack.get(i);
            if (o == target) {
                break;
            }
            if (o instanceof Fin) {
                Fin f = (Fin) o;
                boolean catchOpen = f.catchRegion != null && f.catchRegion.isOpen();
                boolean anyOpen = f.anyRegion != null && f.anyRegion.isOpen();
                if (f.catchRegion != null) {
                    f.catchRegion.close(code.pc());
                }
                if (f.anyRegion != null) {
                    f.anyRegion.close(code.pc());
                }
                List<Object> saved = new ArrayList<Object>(jumpStack.subList(i, jumpStack.size()));
                while (jumpStack.size() > i) {
                    jumpStack.remove(jumpStack.size() - 1);
                }
                f.genExit();
                jumpStack.addAll(saved);
                if (catchOpen || anyOpen) {
                    passed.add(f);
                    f.reopenCatch = catchOpen;
                    f.reopenAny = anyOpen;
                }
            }
        }
        return passed;
    }

    private void reopen(List<Fin> passed) {
        for (Fin f : passed) {
            if (f.reopenCatch) {
                f.catchRegion.open(code.pc());
            }
            if (f.reopenAny) {
                f.anyRegion.open(code.pc());
            }
        }
    }

    private boolean hasFinalizers(Object downTo) {
        for (int i = jumpStack.size() - 1; i >= 0; i--) {
            Object o = jumpStack.get(i);
            if (o == downTo) {
                return false;
            }
            if (o instanceof Fin) {
                return true;
            }
        }
        return false;
    }

    private Type currentReturnType;

    private void genReturn(Tree.Return r) {
        if (r.expr == null) {
            List<Fin> passed = runFinalizersDownTo(null);
            code.op(Code.RETURN);
            reopen(passed);
            return;
        }
        Type rt = currentReturnType != null ? currentReturnType : erased(method.returnType);
        genExprAs(r.expr, rt);
        if (hasFinalizers(null)) {
            int tmp = code.newLocal(rt);
            code.varOp(storeOp(rt), tmp);
            List<Fin> passed = runFinalizersDownTo(null);
            code.varOp(loadOp(rt), tmp);
            code.op(returnOp(rt));
            reopen(passed);
        } else {
            code.op(returnOp(rt));
        }
    }

    private void genYield(Tree.Yield y) {
        Target t = null;
        for (int i = jumpStack.size() - 1; i >= 0; i--) {
            Object o = jumpStack.get(i);
            if (o instanceof Target && ((Target) o).yieldType != null) {
                t = (Target) o;
                break;
            }
        }
        if (t == null) {
            return;
        }
        genExprAs(y.value, t.yieldType);
        if (hasFinalizers(t)) {
            int tmp = code.newLocal(t.yieldType);
            code.varOp(storeOp(t.yieldType), tmp);
            List<Fin> passed = runFinalizersDownTo(t);
            code.varOp(loadOp(t.yieldType), tmp);
            code.jump(Code.GOTO, t.breakLabel);
            reopen(passed);
        } else {
            code.jump(Code.GOTO, t.breakLabel);
        }
    }

    // ------------------------------------------------------------------ try

    private interface BodyGen {
        void gen();
    }

    private void genTry(final Tree.Try t) {
        if (!t.resources.isEmpty()) {
            if (t.catches.isEmpty() && t.finalizer == null) {
                genResources(t, 0);
            } else {
                genTryCatchFinally(new BodyGen() {
                    @Override
                    public void gen() {
                        genResources(t, 0);
                    }
                }, t.catches, t.finalizer);
            }
            return;
        }
        genTryCatchFinally(new BodyGen() {
            @Override
            public void gen() {
                genStat(t.body);
            }
        }, t.catches, t.finalizer);
    }

    private void genTryCatchFinally(BodyGen body, List<Tree.Catch> catches, final Tree.Block finalizer) {
        Fin fin = new Fin() {
            @Override
            void genExit() {
                if (finalizer != null) {
                    genStat(finalizer);
                }
            }
        };
        if (!catches.isEmpty()) {
            fin.catchRegion = new Region();
        }
        if (finalizer != null) {
            fin.anyRegion = new Region();
        }
        jumpStack.add(fin);
        if (fin.catchRegion != null) {
            fin.catchRegion.open(code.pc());
        }
        if (fin.anyRegion != null) {
            fin.anyRegion.open(code.pc());
        }
        body.gen();
        if (fin.catchRegion != null) {
            fin.catchRegion.close(code.pc());
        }
        Code.Label exit = new Code.Label();
        if (code.alive) {
            if (fin.anyRegion != null) {
                fin.anyRegion.close(code.pc());
            }
            jumpStack.remove(fin);
            fin.genExit();
            jumpStack.add(fin);
            if (code.alive) {
                code.jump(Code.GOTO, exit);
            }
        }
        for (Tree.Catch c : catches) {
            if (fin.catchRegion.ranges.isEmpty()) {
                continue;
            }
            Code.Label handler = new Code.Label();
            code.placeReachable(handler);
            List<Type> catchTypes = new ArrayList<Type>();
            if (c.param.unionTypes != null && !c.param.unionTypes.isEmpty()) {
                for (Tree u : c.param.unionTypes) {
                    catchTypes.add(erased(u.type));
                }
            } else {
                catchTypes.add(erased(c.param.vartype.type));
            }
            for (int[] range : fin.catchRegion.ranges) {
                for (Type ct : catchTypes) {
                    code.addHandler(range[0], range[1], handler.pos, pool.cls(internalName(ct)));
                }
            }
            if (fin.anyRegion != null) {
                fin.anyRegion.open(code.pc());
            }
            Type pt = erased(c.param.sym.type);
            int slot = declareLocal(c.param.sym, pt);
            code.varOp(Code.ASTORE, slot);
            genStat(c.body);
            if (fin.anyRegion != null) {
                fin.anyRegion.close(code.pc());
            }
            if (code.alive) {
                jumpStack.remove(fin);
                fin.genExit();
                jumpStack.add(fin);
                if (code.alive) {
                    code.jump(Code.GOTO, exit);
                }
            }
        }
        jumpStack.remove(fin);
        if (finalizer != null) {
            fin.anyRegion.close(code.pc());
            if (!fin.anyRegion.ranges.isEmpty()) {
                Code.Label handler = new Code.Label();
                code.placeReachable(handler);
                for (int[] range : fin.anyRegion.ranges) {
                    code.addHandler(range[0], range[1], handler.pos, 0);
                }
                Type th = syms.type("java/lang/Throwable");
                int slot = code.newLocal(th);
                code.varOp(Code.ASTORE, slot);
                genStat(finalizer);
                if (code.alive) {
                    code.varOp(Code.ALOAD, slot);
                    code.op(Code.ATHROW);
                }
            }
        }
        code.place(exit);
    }

    /** try-with-resources (JLS 14.20.3.1) for resources[index..], then the body. */
    private void genResources(final Tree.Try t, int index) {
        if (index == t.resources.size()) {
            genStat(t.body);
            return;
        }
        Tree res = t.resources.get(index);
        final int slot;
        final Type rt;
        if (res instanceof Tree.VarDef) {
            Tree.VarDef v = (Tree.VarDef) res;
            rt = erased(v.sym.type);
            slot = declareLocal(v.sym, rt);
            genExprAs(v.init, rt);
            code.varOp(Code.ASTORE, slot);
        } else {
            Type et = genExpr(res);
            rt = erased(res.type);
            coerce(et, rt);
            slot = code.newLocal(rt);
            code.varOp(Code.ASTORE, slot);
        }
        Fin fin = new Fin() {
            @Override
            void genExit() {
                genClose(slot, rt);
            }
        };
        fin.anyRegion = new Region();
        jumpStack.add(fin);
        fin.anyRegion.open(code.pc());
        genResources(t, index + 1);
        fin.anyRegion.close(code.pc());
        jumpStack.remove(fin);
        Code.Label exit = new Code.Label();
        if (code.alive) {
            genClose(slot, rt);
            code.jump(Code.GOTO, exit);
        }
        if (!fin.anyRegion.ranges.isEmpty()) {
            Code.Label handler = new Code.Label();
            code.placeReachable(handler);
            int thIdx = pool.cls("java/lang/Throwable");
            for (int[] range : fin.anyRegion.ranges) {
                code.addHandler(range[0], range[1], handler.pos, thIdx);
            }
            Type th = syms.type("java/lang/Throwable");
            int primary = code.newLocal(th);
            code.varOp(Code.ASTORE, primary);
            Code.Label rethrow = new Code.Label();
            code.varOp(Code.ALOAD, slot);
            code.jump(Code.IFNULL, rethrow);
            int closeStart = code.pc();
            code.varOp(Code.ALOAD, slot);
            invokeClose(rt);
            int closeEnd = code.pc();
            code.jump(Code.GOTO, rethrow);
            Code.Label suppressed = new Code.Label();
            code.placeReachable(suppressed);
            code.addHandler(closeStart, closeEnd, suppressed.pos, thIdx);
            int x = code.newLocal(th);
            code.varOp(Code.ASTORE, x);
            code.varOp(Code.ALOAD, primary);
            code.varOp(Code.ALOAD, x);
            invoke(Code.INVOKEVIRTUAL, "java/lang/Throwable", "addSuppressed", "(Ljava/lang/Throwable;)V", false);
            code.place(rethrow);
            code.alive = true;
            code.varOp(Code.ALOAD, primary);
            code.op(Code.ATHROW);
        }
        code.place(exit);
    }

    private void genClose(int slot, Type rt) {
        Code.Label skip = new Code.Label();
        code.varOp(Code.ALOAD, slot);
        code.jump(Code.IFNULL, skip);
        code.varOp(Code.ALOAD, slot);
        invokeClose(rt);
        code.place(skip);
    }

    private void invokeClose(Type rt) {
        Type e = erased(rt);
        ClassSymbol c = e.tag == Type.Tag.CLASS ? ((Type.ClassType) e).sym : syms.require("java/lang/AutoCloseable");
        boolean itf = c.isInterface();
        invoke(itf ? Code.INVOKEINTERFACE : Code.INVOKEVIRTUAL, c.internalName, "close", "()V", itf);
    }

    private void genSynchronized(Tree.Synchronized s) {
        Type lt = genExpr(s.lock);
        coerce(lt, syms.objectType);
        code.op(Code.DUP);
        final int lock = code.newLocal(syms.objectType);
        code.varOp(Code.ASTORE, lock);
        code.op(Code.MONITORENTER);
        Fin fin = new Fin() {
            @Override
            void genExit() {
                code.varOp(Code.ALOAD, lock);
                code.op(Code.MONITOREXIT);
            }
        };
        fin.anyRegion = new Region();
        jumpStack.add(fin);
        fin.anyRegion.open(code.pc());
        genStat(s.body);
        fin.anyRegion.close(code.pc());
        jumpStack.remove(fin);
        Code.Label exit = new Code.Label();
        if (code.alive) {
            fin.genExit();
            code.jump(Code.GOTO, exit);
        }
        if (!fin.anyRegion.ranges.isEmpty()) {
            Code.Label handler = new Code.Label();
            code.placeReachable(handler);
            for (int[] range : fin.anyRegion.ranges) {
                code.addHandler(range[0], range[1], handler.pos, 0);
            }
            int t = code.newLocal(syms.type("java/lang/Throwable"));
            code.varOp(Code.ASTORE, t);
            fin.genExit();
            code.varOp(Code.ALOAD, t);
            code.op(Code.ATHROW);
        }
        code.place(exit);
    }

    // ================================================================== expressions

    /** Generates t and converts the result to erased type {@code to}. */
    void genExprAs(Tree t, Type to) {
        Type from = genExpr(t);
        coerce(from, erased(to));
    }

    /**
     * A generic member's value arrives as its erasure -- {@code Map.Entry<Integer, ?>.getKey()}
     * leaves an Object -- while the expression's type is the substituted one. Cast to that,
     * as javac does, so every later conversion starts from the real type: unboxing then
     * widening an Integer into a long must not treat it as a Long.
     */
    private Type narrowToStatic(Type produced, Tree t) {
        if (produced == null || produced.isPrimitive() || produced.tag == Type.Tag.VOID || t.type == null
                || t.type.isErroneous() || t.type.isPrimitive()) {
            return produced;
        }
        Type stat = erased(t.type);
        if (stat.tag != Type.Tag.CLASS && stat.tag != Type.Tag.ARRAY || isSubclass(produced, stat)) {
            return produced;
        }
        code.op2(Code.CHECKCAST, pool.cls(internalName(stat)));
        return stat;
    }

    /** Generates t; returns the erased type of the value it leaves on the stack (VOID when none). */
    Type genExpr(Tree t) {
        if (t.constant != null && t.type != null && Attr.isConstantType(t.type)) {
            Type ct = erased(t.type);
            pushConst(t.constant, ct);
            return ct;
        }
        if (t instanceof Tree.Parens) {
            return genExpr(((Tree.Parens) t).expr);
        }
        if (t instanceof Tree.Literal) {
            if (((Tree.Literal) t).value == null) {
                code.op(Code.ACONST_NULL);
                return Type.NULL;
            }
            Type lt = erased(t.type);
            pushConst(((Tree.Literal) t).value, lt);
            return lt;
        }
        if (t instanceof Tree.Ident) {
            return narrowToStatic(genIdent((Tree.Ident) t), t);
        }
        if (t instanceof Tree.Select) {
            return narrowToStatic(genSelect((Tree.Select) t), t);
        }
        if (t instanceof Tree.MethodCall) {
            return narrowToStatic(genCall((Tree.MethodCall) t), t);
        }
        if (t instanceof Tree.NewClass) {
            return genNew((Tree.NewClass) t);
        }
        if (t instanceof Tree.NewArray) {
            return genNewArray((Tree.NewArray) t);
        }
        if (t instanceof Tree.ArrayAccess) {
            Tree.ArrayAccess a = (Tree.ArrayAccess) t;
            Type at = genExpr(a.array);
            Type arrType = arrayTypeOf(at, a.array);
            coerce(at, arrType);
            genExprAs(a.index, Type.INT);
            Type elem = ((Type.ArrayType) arrType).elem;
            code.op(arrayLoadOp(elem));
            return narrowToStatic(elem, t);
        }
        if (t instanceof Tree.Assign) {
            return genAssign((Tree.Assign) t, true);
        }
        if (t instanceof Tree.CompoundAssign) {
            return genCompoundAssign((Tree.CompoundAssign) t, true);
        }
        if (t instanceof Tree.Unary) {
            return genUnary((Tree.Unary) t);
        }
        if (t instanceof Tree.Binary) {
            return genBinary((Tree.Binary) t);
        }
        if (t instanceof Tree.Conditional) {
            Tree.Conditional c = (Tree.Conditional) t;
            Type rt = erased(c.type);
            Code.Label elseL = new Code.Label();
            Code.Label end = new Code.Label();
            genCond(c.cond, elseL, false);
            genExprAs(c.truePart, rt);
            code.jump(Code.GOTO, end);
            code.place(elseL);
            code.alive = true;
            genExprAs(c.falsePart, rt);
            code.place(end);
            return rt;
        }
        if (t instanceof Tree.InstanceOf) {
            return genBooleanValue(t);
        }
        if (t instanceof Tree.Cast) {
            return genCast((Tree.Cast) t);
        }
        if (t instanceof Tree.Lambda) {
            return genLambda((Tree.Lambda) t);
        }
        if (t instanceof Tree.MethodRef) {
            Tree.MethodRef r = (Tree.MethodRef) t;
            return genLambda(r.lambda);
        }
        if (t instanceof Tree.ClassLiteral) {
            Type ct = ((Tree.ClassLiteral) t).clazz.type;
            if (ct.isPrimitive() || ct.tag == Type.Tag.VOID) {
                String box = ct.tag == Type.Tag.VOID ? "java/lang/Void" : internalName(syms.boxedType(ct));
                code.op2(Code.GETSTATIC, pool.field(box, "TYPE", "Ljava/lang/Class;"));
            } else {
                code.ldc(pool.cls(internalName(erased(ct))));
            }
            return syms.type("java/lang/Class");
        }
        if (t instanceof Tree.Switch) {
            return genSwitch((Tree.Switch) t);
        }
        if (t instanceof Tree.ResolvedType) {
            return Type.VOID;
        }
        throw new CompileError("cannot generate " + t.getClass().getName());
    }

    private Type arrayTypeOf(Type stackType, Tree expr) {
        if (stackType.tag == Type.Tag.ARRAY) {
            return stackType;
        }
        Type e = erased(expr.type);
        if (e.tag == Type.Tag.ARRAY) {
            return e;
        }
        return new Type.ArrayType(syms.objectType);
    }

    // ------------------------------------------------------------------ names

    private void loadThis() {
        code.varOp(Code.ALOAD, 0);
    }

    /**
     * Pushes an instance of {@code target}: this, or an enclosing instance reached
     * through the this$0 chain. In a constructor prologue the first hop comes from
     * the outer-instance parameter (this is not yet initialized).
     */
    private void loadInstance(ClassSymbol target, int pos) {
        ClassSymbol k = cls;
        if (types.isSubClass(k, target)) {
            loadThis();
            return;
        }
        if (prologue && outerParamSlot >= 0) {
            code.varOp(Code.ALOAD, outerParamSlot);
        } else {
            if (!k.hasOuterInstance) {
                throw new CompileError("no enclosing instance of " + target.javaName());
            }
            loadThis();
            code.op2(Code.GETFIELD, pool.field(k.internalName, "this$0", "L" + k.outer.internalName + ";"));
        }
        k = k.outer;
        while (!types.isSubClass(k, target)) {
            if (!k.hasOuterInstance || k.outer == null) {
                throw new CompileError("no enclosing instance of " + target.javaName());
            }
            code.op2(Code.GETFIELD, pool.field(k.internalName, "this$0", "L" + k.outer.internalName + ";"));
            k = k.outer;
        }
    }

    /** Loads a local, parameter or captured variable; returns its erased slot type. */
    private Type loadVar(VarSymbol v) {
        Integer s = slots.get(v);
        if (s != null) {
            Type t = slotTypes.get(v);
            code.varOp(loadOp(t), s.intValue());
            return t;
        }
        Integer cp = capturedParamSlots.get(v);
        if (cp != null && prologue) {
            Type t = erased(v.type);
            code.varOp(loadOp(t), cp.intValue());
            return t;
        }
        // Captured by this (local or anonymous) class, or by a class it is nested in.
        ClassSymbol k = cls;
        boolean first = true;
        while (k != null) {
            if (k.capturedVars.contains(v)) {
                Type t = erased(v.type);
                if (first) {
                    loadThis();
                }
                code.op2(Code.GETFIELD, pool.field(k.internalName, "val$" + v.name, desc(t)));
                return t;
            }
            if (!k.hasOuterInstance) {
                break;
            }
            if (first) {
                loadThis();
                first = false;
            }
            code.op2(Code.GETFIELD, pool.field(k.internalName, "this$0", "L" + k.outer.internalName + ";"));
            k = k.outer;
        }
        throw new CompileError("variable " + v.name + " is not available here");
    }

    private void storeVar(VarSymbol v) {
        Integer s = slots.get(v);
        if (s == null) {
            throw new CompileError("cannot assign captured variable " + v.name);
        }
        code.varOp(storeOp(slotTypes.get(v)), s.intValue());
    }

    private Type genIdent(Tree.Ident id) {
        if ("this".equals(id.name)) {
            loadThis();
            return cls.erasure();
        }
        if (!(id.sym instanceof VarSymbol)) {
            return Type.VOID;
        }
        VarSymbol v = (VarSymbol) id.sym;
        if (v.kind != VarSymbol.Kind.FIELD) {
            return loadVar(v);
        }
        Type ft = erased(v.type);
        if (v.isStatic()) {
            code.op2(Code.GETSTATIC, pool.field(v.owner.internalName, v.name, desc(ft)));
        } else {
            loadInstance(id.site != null ? id.site : v.owner, id.pos);
            code.op2(Code.GETFIELD, pool.field(fieldOwner(v, id.site), v.name, desc(ft)));
        }
        return ft;
    }

    /** The class named in a Fieldref: the declaring class (private fields are not inherited). */
    private String fieldOwner(VarSymbol f, ClassSymbol site) {
        return f.owner.internalName;
    }

    private Type genSelect(Tree.Select sel) {
        if ("this".equals(sel.name)) {
            ClassSymbol target = (ClassSymbol) sel.sym;
            loadInstance(target, sel.pos);
            return target.erasure();
        }
        if (sel.sym == null && "length".equals(sel.name)) {
            Type at = genExpr(sel.selected);
            coerce(at, arrayTypeOf(at, sel.selected));
            code.op(Code.ARRAYLENGTH);
            return Type.INT;
        }
        if (!(sel.sym instanceof VarSymbol)) {
            return Type.VOID;
        }
        VarSymbol f = (VarSymbol) sel.sym;
        Type ft = erased(f.type);
        if (f.isStatic()) {
            if (!sel.staticRef && !(sel.selected instanceof Tree.Ident && "super".equals(((Tree.Ident) sel.selected).name))) {
                pop(genExpr(sel.selected));
            }
            code.op2(Code.GETSTATIC, pool.field(f.owner.internalName, f.name, desc(ft)));
            return ft;
        }
        genReceiverForField(sel, f);
        code.op2(Code.GETFIELD, pool.field(f.owner.internalName, f.name, desc(ft)));
        return ft;
    }

    private void genReceiverForField(Tree.Select sel, VarSymbol f) {
        if (sel.selected instanceof Tree.Ident && "super".equals(((Tree.Ident) sel.selected).name)) {
            loadThis();
            return;
        }
        Type rt = genExpr(sel.selected);
        coerce(rt, f.owner.erasure());
    }

    // ------------------------------------------------------------------ calls

    private Type genCall(Tree.MethodCall c) {
        MethodSymbol m = c.sym;
        if ("<init>".equals(c.name)) {
            throw new CompileError("call to " + (c.superCall ? "super" : "this") + " must be first statement in constructor");
        }
        String owner;
        int opcode;
        boolean itf;
        if (m.isStatic()) {
            if (c.receiver != null && !(c.receiver instanceof Tree.ResolvedType) && !isTypeName(c.receiver)) {
                pop(genExpr(c.receiver));
            }
            owner = m.owner.internalName;
            opcode = Code.INVOKESTATIC;
            itf = m.owner.isInterface();
        } else if (c.superAccessor != null) {
            // Outer.super.m() from an inner class: through Outer's static accessor.
            loadInstance(c.site, c.pos);
            genArgs(m, c.args, c.varargsCall, c.varargsElem);
            MethodSymbol acc = c.superAccessor;
            invoke(Code.INVOKESTATIC, acc.owner.internalName, acc.name, acc.descriptor(), false);
            return erased(acc.returnType);
        } else if (c.superCall) {
            if (c.site != null && c.site != cls && !c.site.isInterface() && c.receiver != null) {
                throw new CompileError("Outer.super.method() calls are not supported");
            }
            loadThis();
            owner = c.qualifier != null ? c.qualifier.internalName : m.owner.internalName;
            ClassSymbol oc = c.qualifier != null ? c.qualifier : m.owner;
            if (m.owner.isInterface() && !oc.isInterface()) {
                // super.m() reaching an interface default through the superclass: name the superclass.
                itf = false;
            } else {
                itf = oc.isInterface();
            }
            opcode = Code.INVOKESPECIAL;
        } else {
            Type recvType;
            if (c.receiver == null) {
                ClassSymbol site = c.site != null ? c.site : cls;
                loadInstance(site, c.pos);
                recvType = site.erasure();
                if (!types.isSubClass(site, m.owner)) {
                    recvType = m.owner.erasure();
                }
            } else {
                recvType = genExpr(c.receiver);
            }
            Type qual = qualifierFor(recvType, c.receiver, m);
            coerce(recvType, qual);
            owner = internalName(qual);
            itf = qual.tag == Type.Tag.CLASS && ((Type.ClassType) qual).sym.isInterface();
            if (m.isPrivate() && m.owner == cls) {
                opcode = Code.INVOKESPECIAL;
                itf = m.owner.isInterface();
            } else {
                opcode = itf ? Code.INVOKEINTERFACE : Code.INVOKEVIRTUAL;
            }
        }
        genArgs(m, c.args, c.varargsCall, c.varargsElem);
        String d = jvmDescriptor(m);
        if (!m.isStatic() && !c.superCall) {
            for (String[] r : compiler.redirects) {
                if (r[0].equals(m.owner.internalName) && r[1].equals(m.name) && r[2].equals(d)) {
                    // The receiver is already on the stack: it becomes the first argument.
                    String rd = "(L" + m.owner.internalName + ";" + d.substring(1);
                    invoke(Code.INVOKESTATIC, r[3], r[4], rd, false);
                    return typeForDescriptor(FrameComputer.returnDescriptor(d));
                }
            }
        }
        invoke(opcode, owner, m.name, d, itf);
        Type ret = typeForDescriptor(FrameComputer.returnDescriptor(d));
        return ret;
    }

    private boolean isTypeName(Tree t) {
        if (t instanceof Tree.Ident) {
            return ((Tree.Ident) t).sym instanceof ClassSymbol;
        }
        if (t instanceof Tree.Select) {
            return ((Tree.Select) t).sym instanceof ClassSymbol || ((Tree.Select) t).staticRef && !(((Tree.Select) t).sym instanceof VarSymbol);
        }
        return t instanceof Tree.TypeApply || t instanceof Tree.ArrayTypeTree || t instanceof Tree.PrimitiveTypeTree;
    }

    /** The class named in an instance call's Methodref. */
    private Type qualifierFor(Type recvType, Tree receiver, MethodSymbol m) {
        Type rt = erased(recvType);
        if (rt.tag == Type.Tag.ARRAY) {
            return m.name.equals("clone") ? rt : syms.objectType;
        }
        if (m.owner == syms.objectSym || m.isPrivate()) {
            return m.owner.erasure();
        }
        if (rt.tag == Type.Tag.CLASS) {
            ClassSymbol r = ((Type.ClassType) rt).sym;
            if (r != syms.objectSym && types.isSubClass(r, m.owner)) {
                return rt;
            }
        }
        if (receiver != null && receiver.type != null) {
            Type declared = erased(receiver.type);
            if (declared.tag == Type.Tag.CLASS && types.isSubClass(((Type.ClassType) declared).sym, m.owner)) {
                return declared;
            }
        }
        return m.owner.erasure();
    }

    /** Pushes the arguments, packing variable-arity ones into an array. */
    private void genArgs(MethodSymbol m, List<Tree> args, boolean varargs) {
        genArgs(m, args, varargs, null);
    }

    private void genArgs(MethodSymbol m, List<Tree> args, boolean varargs, Type inferredElem) {
        int n = m.params.size();
        int fixed = varargs ? n - 1 : args.size();
        for (int i = 0; i < fixed && i < args.size(); i++) {
            genExprAs(args.get(i), erased(m.params.get(i)));
        }
        if (varargs) {
            Type arr = erased(m.params.get(n - 1));
            Type elem = arr.tag == Type.Tag.ARRAY ? ((Type.ArrayType) arr).elem : syms.objectType;
            if (inferredElem != null && !inferredElem.isErroneous() && !inferredElem.isPrimitive()
                    && inferredElem.tag != Type.Tag.NULL) {
                elem = inferredElem;
            }
            code.iconst(args.size() - fixed);
            newArray(elem);
            for (int i = fixed; i < args.size(); i++) {
                code.op(Code.DUP);
                code.iconst(i - fixed);
                genExprAs(args.get(i), elem);
                code.op(arrayStoreOp(elem));
            }
        }
    }

    private Type genNew(Tree.NewClass nc) {
        ClassSymbol c = nc.clazzSym;
        MethodSymbol ctor = nc.constructor;
        code.op2(Code.NEW, pool.cls(c.internalName));
        code.op(Code.DUP);
        if (c.decl != null) {
            if (c.hasOuterInstance) {
                if (nc.outer != null && !c.anonymous) {
                    Type ot = genExpr(nc.outer);
                    nullCheck();
                    coerce(ot, c.outer.erasure());
                } else {
                    loadInstance(c.outer, nc.pos);
                }
            }
            if (anonSuperNeedsOuter(c)) {
                ClassSymbol sup = ((Type.ClassType) c.superclass()).sym;
                if (nc.outer != null) {
                    Type ot = genExpr(nc.outer);
                    nullCheck();
                    coerce(ot, sup.outer.erasure());
                } else {
                    loadInstance(sup.outer, nc.pos);
                }
            }
            genArgs(ctor, nc.args, nc.varargsCall, nc.varargsElem);
            for (VarSymbol v : c.capturedVars) {
                loadVar(v);
            }
        } else {
            if (c.hasOuterInstance) {
                if (nc.outer != null) {
                    Type ot = genExpr(nc.outer);
                    nullCheck();
                    coerce(ot, c.outer.erasure());
                } else {
                    loadInstance(c.outer, nc.pos);
                }
            }
            genArgs(ctor, nc.args, nc.varargsCall, nc.varargsElem);
        }
        code.op2(Code.INVOKESPECIAL, pool.method(c.internalName, "<init>", jvmDescriptor(ctor), false));
        return c.erasure();
    }

    private Type genNewArray(Tree.NewArray na) {
        Type t = erased(na.type);
        if (na.elems != null) {
            Type elem = ((Type.ArrayType) t).elem;
            code.iconst(na.elems.size());
            newArray(elem);
            for (int i = 0; i < na.elems.size(); i++) {
                code.op(Code.DUP);
                code.iconst(i);
                genExprAs(na.elems.get(i), elem);
                code.op(arrayStoreOp(elem));
            }
            return t;
        }
        for (Tree d : na.dims) {
            genExprAs(d, Type.INT);
        }
        if (na.dims.size() == 1) {
            newArray(((Type.ArrayType) t).elem);
        } else {
            code.bc.u1(Code.MULTIANEWARRAY).u2(pool.cls(internalName(t))).u1(na.dims.size());
        }
        return t;
    }

    private Type genCast(Tree.Cast c) {
        Type target = c.type;
        Tree inner = c.expr;
        while (inner instanceof Tree.Parens) {
            inner = ((Tree.Parens) inner).expr;
        }
        if (inner instanceof Tree.Lambda || inner instanceof Tree.MethodRef) {
            // The lambda already implements every bound (markers): no checkcast.
            return genExpr(c.expr);
        }
        Type from = genExpr(c.expr);
        if (target.tag == Type.Tag.INTERSECTION) {
            List<Type> bounds = ((Type.IntersectionType) target).bounds;
            for (int i = bounds.size() - 1; i >= 1; i--) {
                code.op2(Code.CHECKCAST, pool.cls(internalName(bounds.get(i))));
            }
            Type first = erased(bounds.get(0));
            coerce(from, first);
            return first;
        }
        Type to = erased(target);
        if (from.isPrimitive() && !to.isPrimitive() && syms.unboxedType(to) == null) {
            // (Object) 5 or (Comparable) 'x': box to the value's own wrapper.
            box(from);
            Type boxed = syms.boxedType(from);
            if (!isSubclass(boxed, to)) {
                code.op2(Code.CHECKCAST, pool.cls(internalName(to)));
            }
            return to;
        }
        if (!from.isPrimitive() && to.isPrimitive() && syms.unboxedType(from) == null && !(from.tag == Type.Tag.NULL)) {
            // (int) obj: checkcast Integer then unbox.
            Type box = syms.boxedType(to);
            code.op2(Code.CHECKCAST, pool.cls(internalName(box)));
            unbox(to);
            return to;
        }
        if (!from.isPrimitive() && !to.isPrimitive() && !isSubclass(from, to)) {
            code.op2(Code.CHECKCAST, pool.cls(internalName(to)));
            return to;
        }
        coerce(from, to);
        return to;
    }

    // ------------------------------------------------------------------ operators

    private Type genUnary(Tree.Unary u) {
        switch (u.op) {
            case PLUSPLUS:
            case SUBSUB:
                return genIncrement(u, true);
            case BANG:
                return genBooleanValue(u);
            case PLUS: {
                Type t = erased(u.operandType);
                genExprAs(u.arg, t);
                return t;
            }
            case SUB: {
                Type t = erased(u.operandType);
                genExprAs(u.arg, t);
                code.op(Code.INEG + kind(t));
                return t;
            }
            case TILDE: {
                Type t = erased(u.operandType);
                genExprAs(u.arg, t);
                if (t.tag == Type.Tag.LONG) {
                    code.ldc2(pool.longConst(-1L));
                    code.op(Code.LXOR);
                } else {
                    code.iconst(-1);
                    code.op(Code.IXOR);
                }
                return t;
            }
            default:
                throw new CompileError("unary " + u.op);
        }
    }

    private Type genBinary(Tree.Binary b) {
        switch (b.op) {
            case AMPAMP: case BARBAR: case EQEQ: case BANGEQ: case LT: case GT: case LTEQ: case GTEQ:
                return genBooleanValue(b);
            default:
                break;
        }
        if (b.op == Token.Kind.PLUS && attr.isString(b.type)) {
            return genConcat(b);
        }
        Type t = erased(b.operandType);
        genExprAs(b.lhs, t);
        boolean shift = b.op == Token.Kind.LTLT || b.op == Token.Kind.GTGT || b.op == Token.Kind.GTGTGT;
        genExprAs(b.rhs, shift ? Type.INT : t);
        code.op(arithOp(b.op, t));
        return t;
    }

    private static int arithOp(Token.Kind op, Type t) {
        int k = kind(t);
        switch (op) {
            case PLUS: return Code.IADD + k;
            case SUB: return Code.ISUB + k;
            case STAR: return Code.IMUL + k;
            case SLASH: return Code.IDIV + k;
            case PERCENT: return Code.IREM + k;
            case LTLT: return Code.ISHL + (k == 1 ? 1 : 0);
            case GTGT: return Code.ISHR + (k == 1 ? 1 : 0);
            case GTGTGT: return Code.IUSHR + (k == 1 ? 1 : 0);
            case AMP: return Code.IAND + (k == 1 ? 1 : 0);
            case BAR: return Code.IOR + (k == 1 ? 1 : 0);
            case CARET: return Code.IXOR + (k == 1 ? 1 : 0);
            default: throw new CompileError("operator " + op);
        }
    }

    /** A boolean-valued expression: materialized as 0/1 through conditional jumps. */
    private Type genBooleanValue(Tree t) {
        Code.Label falseL = new Code.Label();
        Code.Label end = new Code.Label();
        genCond(t, falseL, false);
        code.iconst(1);
        code.jump(Code.GOTO, end);
        code.place(falseL);
        code.alive = true;
        code.iconst(0);
        code.place(end);
        return Type.BOOLEAN;
    }

    // ------------------------------------------------------------------ string concatenation

    private void newStringBuilder() {
        code.op2(Code.NEW, pool.cls("java/lang/StringBuilder"));
        code.op(Code.DUP);
        code.op2(Code.INVOKESPECIAL, pool.method("java/lang/StringBuilder", "<init>", "()V", false));
    }

    /** Appends the value on the stack (of erased type t) to the StringBuilder under it. */
    private void appendTo(Type t) {
        String d;
        switch (t.tag) {
            case BOOLEAN: d = "Z"; break;
            case CHAR: d = "C"; break;
            case BYTE: case SHORT: case INT: d = "I"; break;
            case LONG: d = "J"; break;
            case FLOAT: d = "F"; break;
            case DOUBLE: d = "D"; break;
            default:
                d = attr.isString(t) ? "Ljava/lang/String;" : "Ljava/lang/Object;";
        }
        code.op2(Code.INVOKEVIRTUAL, pool.method("java/lang/StringBuilder", "append", "(" + d + ")Ljava/lang/StringBuilder;", false));
    }

    private void collectConcat(Tree t, List<Tree> out) {
        Tree x = t;
        if (x instanceof Tree.Binary && ((Tree.Binary) x).op == Token.Kind.PLUS && attr.isString(x.type) && x.constant == null) {
            collectConcat(((Tree.Binary) x).lhs, out);
            collectConcat(((Tree.Binary) x).rhs, out);
        } else {
            out.add(x);
        }
    }

    private Type genConcat(Tree.Binary b) {
        List<Tree> parts = new ArrayList<Tree>();
        collectConcat(b, parts);
        newStringBuilder();
        for (Tree p : parts) {
            appendValue(p);
        }
        code.op2(Code.INVOKEVIRTUAL, pool.method("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
        return syms.stringType;
    }

    private void appendValue(Tree p) {
        Type st = genExpr(p);
        Type declared = p.type == null ? st : erased(p.type);
        if (attr.isString(declared) && !attr.isString(st) && st.tag != Type.Tag.NULL) {
            coerce(st, syms.stringType);
            st = syms.stringType;
        } else if (declared.isPrimitive() && !st.isPrimitive()) {
            coerce(st, declared);
            st = declared;
        }
        appendTo(st.tag == Type.Tag.NULL ? syms.objectType : st);
    }

    // ------------------------------------------------------------------ assignment

    /** What an assignment target needs: how to load its receiver, read it and write it. */
    private final class LValue {
        Tree tree;
        VarSymbol var;
        boolean isStatic;
        boolean isArray;
        boolean isField;
        Type type;
        String owner;
        /** Words the receiver occupies on the stack (0 locals/static, 1 field, 2 array). */
        int receiverWords;

        void genReceiver() {
            if (isArray) {
                Tree.ArrayAccess a = (Tree.ArrayAccess) tree;
                Type at = genExpr(a.array);
                Type arrType = arrayTypeOf(at, a.array);
                coerce(at, arrType);
                genExprAs(a.index, Type.INT);
            } else if (isField && !isStatic) {
                if (tree instanceof Tree.Ident) {
                    Tree.Ident id = (Tree.Ident) tree;
                    loadInstance(id.site != null ? id.site : var.owner, id.pos);
                } else {
                    genReceiverForField((Tree.Select) tree, var);
                }
            } else if (isField && tree instanceof Tree.Select && !((Tree.Select) tree).staticRef
                    && !(((Tree.Select) tree).selected instanceof Tree.Ident && "super".equals(((Tree.Ident) ((Tree.Select) tree).selected).name))) {
                pop(genExpr(((Tree.Select) tree).selected));
            }
        }

        void dupReceiver() {
            if (receiverWords == 1) {
                code.op(Code.DUP);
            } else if (receiverWords == 2) {
                code.op(Code.DUP2);
            }
        }

        void load() {
            if (isArray) {
                code.op(arrayLoadOp(type));
            } else if (isField) {
                code.op2(isStatic ? Code.GETSTATIC : Code.GETFIELD, pool.field(owner, var.name, desc(type)));
            } else {
                loadVar(var);
            }
        }

        void store() {
            if (isArray) {
                code.op(arrayStoreOp(type));
            } else if (isField) {
                code.op2(isStatic ? Code.PUTSTATIC : Code.PUTFIELD, pool.field(owner, var.name, desc(type)));
            } else {
                storeVar(var);
            }
        }

        /** Duplicates a value of this target's type below the receiver, so it survives the store. */
        void dupValue() {
            if (receiverWords == 0) {
                dup(type);
            } else {
                dupX(type, receiverWords);
            }
        }
    }

    private LValue lvalue(Tree t) {
        while (t instanceof Tree.Parens) {
            t = ((Tree.Parens) t).expr;
        }
        LValue lv = new LValue();
        lv.tree = t;
        if (t instanceof Tree.ArrayAccess) {
            lv.isArray = true;
            Tree.ArrayAccess a = (Tree.ArrayAccess) t;
            Type at = erased(a.array.type);
            lv.type = at.tag == Type.Tag.ARRAY ? ((Type.ArrayType) at).elem : syms.objectType;
            lv.receiverWords = 2;
            return lv;
        }
        VarSymbol v = (VarSymbol) (t instanceof Tree.Ident ? ((Tree.Ident) t).sym : ((Tree.Select) t).sym);
        lv.var = v;
        if (v.kind == VarSymbol.Kind.FIELD) {
            lv.isField = true;
            lv.isStatic = v.isStatic();
            lv.owner = v.owner.internalName;
            lv.type = erased(v.type);
            lv.receiverWords = lv.isStatic ? 0 : 1;
        } else {
            lv.type = slotTypes.containsKey(v) ? slotTypes.get(v) : erased(v.type);
        }
        return lv;
    }

    private Type genAssign(Tree.Assign a, boolean needValue) {
        LValue lv = lvalue(a.lhs);
        lv.genReceiver();
        genExprAs(a.rhs, lv.type);
        if (needValue) {
            lv.dupValue();
        }
        lv.store();
        return needValue ? lv.type : Type.VOID;
    }

    private Type genCompoundAssign(Tree.CompoundAssign a, boolean needValue) {
        LValue lv = lvalue(a.lhs);
        Token.Kind op = Attr.binaryOf(a.op);
        Type operand = erased(a.operandType);
        if (!lv.isField && !lv.isArray && lv.type.tag == Type.Tag.INT && operand.tag == Type.Tag.INT
                && (op == Token.Kind.PLUS || op == Token.Kind.SUB) && a.rhs.constant != null
                && Constants.cast(a.rhs.constant, Type.INT) != null && slots.containsKey(lv.var)) {
            int delta = Constants.intValue(Constants.cast(a.rhs.constant, Type.INT));
            if (op == Token.Kind.SUB) {
                delta = -delta;
            }
            if (delta >= -32768 && delta <= 32767) {
                code.iinc(slots.get(lv.var).intValue(), delta);
                if (needValue) {
                    lv.load();
                    return lv.type;
                }
                return Type.VOID;
            }
        }
        lv.genReceiver();
        lv.dupReceiver();
        lv.load();
        if (attr.isString(operand)) {
            // s += x: StringBuilder over the old value and x.
            Type cur = lv.type;
            int tmp = code.newLocal(cur);
            code.varOp(storeOp(cur), tmp);
            newStringBuilder();
            code.varOp(loadOp(cur), tmp);
            appendTo(cur);
            appendValue(a.rhs);
            code.op2(Code.INVOKEVIRTUAL, pool.method("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
            coerce(syms.stringType, lv.type);
        } else {
            coerce(lv.type, operand);
            boolean shift = op == Token.Kind.LTLT || op == Token.Kind.GTGT || op == Token.Kind.GTGTGT;
            genExprAs(a.rhs, shift ? Type.INT : operand);
            code.op(arithOp(op, operand));
            Type target = lv.type.isPrimitive() ? lv.type : syms.unboxedType(lv.type) != null ? syms.unboxedType(lv.type) : lv.type;
            convertPrimitive(operand, target.isPrimitive() ? target : operand);
            if (!lv.type.isPrimitive()) {
                coerce(target.isPrimitive() ? target : operand, lv.type);
            }
        }
        if (needValue) {
            lv.dupValue();
        }
        lv.store();
        return needValue ? lv.type : Type.VOID;
    }

    private Type genIncrement(Tree.Unary u, boolean needValue) {
        LValue lv = lvalue(u.arg);
        boolean inc = u.op == Token.Kind.PLUSPLUS;
        if (!lv.isField && !lv.isArray && lv.type.tag == Type.Tag.INT && slots.containsKey(lv.var)) {
            int slot = slots.get(lv.var).intValue();
            if (needValue && u.postfix) {
                lv.load();
            }
            code.iinc(slot, inc ? 1 : -1);
            if (needValue && !u.postfix) {
                lv.load();
            }
            return needValue ? Type.INT : Type.VOID;
        }
        Type prim = lv.type.isPrimitive() ? lv.type : syms.unboxedType(lv.type);
        Type operand = Types.unaryPromote(prim);
        lv.genReceiver();
        lv.dupReceiver();
        lv.load();
        if (needValue && u.postfix) {
            lv.dupValue();
        }
        coerce(lv.type, operand);
        pushConst(Integer.valueOf(1), operand);
        code.op((inc ? Code.IADD : Code.ISUB) + kind(operand));
        convertPrimitive(operand, prim);
        coerce(prim, lv.type);
        if (needValue && !u.postfix) {
            lv.dupValue();
        }
        lv.store();
        return needValue ? lv.type : Type.VOID;
    }

    // ================================================================== conditions

    /** Jumps to target when cond evaluates to jumpIfTrue; falls through otherwise. */
    void genCond(Tree cond, Code.Label target, boolean jumpIfTrue) {
        if (cond.constant instanceof Boolean) {
            if (((Boolean) cond.constant).booleanValue() == jumpIfTrue) {
                code.jump(Code.GOTO, target);
            }
            return;
        }
        if (cond instanceof Tree.Parens) {
            genCond(((Tree.Parens) cond).expr, target, jumpIfTrue);
            return;
        }
        if (cond instanceof Tree.Unary && ((Tree.Unary) cond).op == Token.Kind.BANG) {
            genCond(((Tree.Unary) cond).arg, target, !jumpIfTrue);
            return;
        }
        if (cond instanceof Tree.InstanceOf) {
            genInstanceOf((Tree.InstanceOf) cond, target, jumpIfTrue);
            return;
        }
        if (cond instanceof Tree.Binary) {
            Tree.Binary b = (Tree.Binary) cond;
            switch (b.op) {
                case AMPAMP:
                    if (jumpIfTrue) {
                        Code.Label skip = new Code.Label();
                        genCond(b.lhs, skip, false);
                        genCond(b.rhs, target, true);
                        code.place(skip);
                    } else {
                        genCond(b.lhs, target, false);
                        genCond(b.rhs, target, false);
                    }
                    return;
                case BARBAR:
                    if (jumpIfTrue) {
                        genCond(b.lhs, target, true);
                        genCond(b.rhs, target, true);
                    } else {
                        Code.Label skip = new Code.Label();
                        genCond(b.lhs, skip, true);
                        genCond(b.rhs, target, false);
                        code.place(skip);
                    }
                    return;
                case EQEQ: case BANGEQ: case LT: case GT: case LTEQ: case GTEQ:
                    genCompare(b, target, jumpIfTrue);
                    return;
                default:
                    break;
            }
        }
        genExprAs(cond, Type.BOOLEAN);
        code.jump(jumpIfTrue ? Code.IFNE : Code.IFEQ, target);
    }

    private static Token.Kind negate(Token.Kind op) {
        switch (op) {
            case EQEQ: return Token.Kind.BANGEQ;
            case BANGEQ: return Token.Kind.EQEQ;
            case LT: return Token.Kind.GTEQ;
            case GTEQ: return Token.Kind.LT;
            case GT: return Token.Kind.LTEQ;
            default: return Token.Kind.GT;
        }
    }

    /** if&lt;cond&gt; opcode comparing an int against zero; the if_icmp form is this + 6. */
    private static int ifOp(Token.Kind op) {
        switch (op) {
            case EQEQ: return Code.IFEQ;
            case BANGEQ: return Code.IFNE;
            case LT: return Code.IFLT;
            case GTEQ: return Code.IFGE;
            case GT: return Code.IFGT;
            default: return Code.IFLE;
        }
    }

    private void genCompare(Tree.Binary b, Code.Label target, boolean jumpIfTrue) {
        Token.Kind op = jumpIfTrue ? b.op : negate(b.op);
        Type t = erased(b.operandType);
        if (!t.isPrimitive()) {
            boolean lnull = isNullLiteral(b.lhs);
            boolean rnull = isNullLiteral(b.rhs);
            if (lnull || rnull) {
                genExpr(lnull ? b.rhs : b.lhs);
                code.jump(op == Token.Kind.EQEQ ? Code.IFNULL : Code.IFNONNULL, target);
                return;
            }
            genExpr(b.lhs);
            genExpr(b.rhs);
            code.jump(op == Token.Kind.EQEQ ? Code.IF_ACMPEQ : Code.IF_ACMPNE, target);
            return;
        }
        switch (t.tag) {
            case LONG:
                genExprAs(b.lhs, t);
                genExprAs(b.rhs, t);
                code.op(Code.LCMP);
                code.jump(ifOp(op), target);
                return;
            case FLOAT:
            case DOUBLE: {
                genExprAs(b.lhs, t);
                genExprAs(b.rhs, t);
                // NaN must make <, <=, >, >= false: pick the compare whose NaN result fails the test.
                boolean lessFamily = b.op == Token.Kind.LT || b.op == Token.Kind.LTEQ;
                int cmp = t.tag == Type.Tag.FLOAT ? (lessFamily ? Code.FCMPG : Code.FCMPL) : (lessFamily ? Code.DCMPG : Code.DCMPL);
                code.op(cmp);
                code.jump(ifOp(op), target);
                return;
            }
            default:
                if (b.rhs.constant != null && Constants.isZero(Constants.cast(b.rhs.constant, Type.INT))
                        && !(b.rhs.constant instanceof Boolean)) {
                    genExprAs(b.lhs, Type.INT);
                    code.jump(ifOp(op), target);
                    return;
                }
                genExprAs(b.lhs, t.tag == Type.Tag.BOOLEAN ? Type.BOOLEAN : Type.INT);
                genExprAs(b.rhs, t.tag == Type.Tag.BOOLEAN ? Type.BOOLEAN : Type.INT);
                code.jump(ifOp(op) + 6, target);
        }
    }

    private static boolean isNullLiteral(Tree t) {
        while (t instanceof Tree.Parens) {
            t = ((Tree.Parens) t).expr;
        }
        return t instanceof Tree.Literal && ((Tree.Literal) t).kind == Token.Kind.NULL;
    }

    // ------------------------------------------------------------------ instanceof and patterns

    private void genInstanceOf(Tree.InstanceOf io, Code.Label target, boolean jumpIfTrue) {
        Tree p = io.pattern;
        if (!(p instanceof Tree.BindingPattern || p instanceof Tree.RecordPattern)) {
            Type et = genExpr(io.expr);
            coerce(et, syms.objectType);
            code.op2(Code.INSTANCEOF, pool.cls(internalName(erased(p.type))));
            code.jump(jumpIfTrue ? Code.IFNE : Code.IFEQ, target);
            return;
        }
        Type et = genExpr(io.expr);
        Type vt = et.tag == Type.Tag.NULL ? syms.objectType : et;
        int tmp = code.newLocal(vt);
        code.varOp(Code.ASTORE, tmp);
        Code.Label fail = jumpIfTrue ? new Code.Label() : target;
        // Null never matches at the top level of instanceof.
        Type pt = erased(p.type);
        code.varOp(Code.ALOAD, tmp);
        code.op2(Code.INSTANCEOF, pool.cls(internalName(pt)));
        code.jump(Code.IFEQ, fail);
        genPatternMatch(p, tmp, vt, fail);
        if (jumpIfTrue) {
            code.jump(Code.GOTO, target);
            code.place(fail);
        }
    }

    /**
     * Matches the value in slot (of erased type valueType, known non-null at the top level
     * of instanceof) against a pattern; binds its variables and jumps to fail on mismatch.
     */
    private void genPatternMatch(Tree p, int slot, Type valueType, Code.Label fail) {
        if (p instanceof Tree.BindingPattern) {
            VarSymbol v = ((Tree.BindingPattern) p).var.sym;
            Type vt = erased(v.type);
            int vs = declareLocal(v, vt);
            code.varOp(loadOp(valueType), slot);
            if (vt.isPrimitive() || valueType.isPrimitive()) {
                coerce(valueType, vt);
            } else if (!isSubclass(valueType, vt)) {
                code.varOp(Code.ALOAD, slot);
                code.op(Code.POP);
                code.op2(Code.CHECKCAST, pool.cls(internalName(vt)));
            }
            code.varOp(storeOp(vt), vs);
            return;
        }
        Tree.RecordPattern rp = (Tree.RecordPattern) p;
        ClassSymbol rec = rp.record;
        Type rt = rec.erasure();
        int r = slot;
        if (!isSubclass(valueType, rt)) {
            code.varOp(Code.ALOAD, slot);
            code.op2(Code.CHECKCAST, pool.cls(rec.internalName));
            r = code.newLocal(rt);
            code.varOp(Code.ASTORE, r);
        }
        List<VarSymbol> comps = rec.recordComponents;
        for (int i = 0; i < rp.nested.size(); i++) {
            VarSymbol comp = comps.get(i);
            Type ct = erased(comp.type);
            code.varOp(Code.ALOAD, r);
            invoke(Code.INVOKEVIRTUAL, rec.internalName, comp.name, "()" + desc(ct), false);
            int cs = code.newLocal(ct);
            code.varOp(storeOp(ct), cs);
            Tree n = rp.nested.get(i);
            Type nt = erased(n.type);
            if (n instanceof Tree.RecordPattern || !ct.isPrimitive() && !isSubclass(ct, nt)) {
                // A nested pattern that is not total: null and other types fail.
                code.varOp(Code.ALOAD, cs);
                code.op2(Code.INSTANCEOF, pool.cls(internalName(nt)));
                code.jump(Code.IFEQ, fail);
            }
            genPatternMatch(n, cs, ct, fail);
        }
    }

    // ================================================================== switch

    private Type genSwitch(Tree.Switch sw) {
        Target t = new Target();
        t.node = sw;
        t.breakLabel = new Code.Label();
        Type resultType = Type.VOID;
        if (sw.isExpression) {
            resultType = erased(sw.type);
            t.yieldType = resultType;
        }
        jumpStack.add(t);
        int n = sw.cases.size();
        Code.Label[] bodies = new Code.Label[n];
        for (int i = 0; i < n; i++) {
            bodies[i] = new Code.Label();
        }
        Code.Label dflt = null;
        for (int i = 0; i < n; i++) {
            if (sw.cases.get(i).isDefault) {
                dflt = bodies[i];
            }
        }
        Code.Label throwLabel = null;
        if (dflt == null) {
            if (sw.needsDefaultThrow) {
                throwLabel = new Code.Label();
                dflt = throwLabel;
            } else {
                dflt = t.breakLabel;
            }
        }
        switch (sw.switchKind) {
            case Attr.SW_INT:
                genIntDispatch(sw, bodies, dflt);
                break;
            case Attr.SW_STRING:
                genStringDispatch(sw, bodies, dflt);
                break;
            case Attr.SW_ENUM:
                genEnumDispatch(sw, bodies, dflt);
                break;
            default:
                genPatternDispatch(sw, bodies, dflt);
                break;
        }
        for (int i = 0; i < n; i++) {
            Tree.Case c = sw.cases.get(i);
            code.place(bodies[i]);
            if (!code.alive) {
                continue;
            }
            if (sw.arrows) {
                if (c.arrowExpr != null) {
                    if (sw.isExpression) {
                        genExprAs(c.arrowExpr, resultType);
                    } else {
                        genEffect(c.arrowExpr);
                    }
                } else {
                    genStats(c.stats);
                }
                if (code.alive) {
                    code.jump(Code.GOTO, t.breakLabel);
                }
            } else {
                genStats(c.stats);
            }
        }
        if (throwLabel != null) {
            code.place(throwLabel);
            if (code.alive) {
                code.op2(Code.NEW, pool.cls("java/lang/IncompatibleClassChangeError"));
                code.op(Code.DUP);
                code.op2(Code.INVOKESPECIAL, pool.method("java/lang/IncompatibleClassChangeError", "<init>", "()V", false));
                code.op(Code.ATHROW);
            }
        }
        jumpStack.remove(t);
        code.place(t.breakLabel);
        return resultType;
    }

    private void emitSwitch(List<Integer> keys, List<Code.Label> targets, Code.Label dflt) {
        int n = keys.size();
        int[] k = new int[n];
        Code.Label[] l = new Code.Label[n];
        // Sort by key (lookupswitch requires it).
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = Integer.valueOf(i);
        }
        for (int i = 1; i < n; i++) {
            Integer cur = order[i];
            int j = i - 1;
            while (j >= 0 && keys.get(order[j].intValue()).intValue() > keys.get(cur.intValue()).intValue()) {
                order[j + 1] = order[j];
                j--;
            }
            order[j + 1] = cur;
        }
        for (int i = 0; i < n; i++) {
            k[i] = keys.get(order[i].intValue()).intValue();
            l[i] = targets.get(order[i].intValue());
        }
        if (n == 0) {
            code.op(Code.POP);
            code.jump(Code.GOTO, dflt);
            return;
        }
        long lo = k[0];
        long hi = k[n - 1];
        long range = hi - lo + 1;
        if (range <= 2L * n + 10) {
            Code.Label[] table = new Code.Label[(int) range];
            for (int i = 0; i < table.length; i++) {
                table[i] = dflt;
            }
            for (int i = 0; i < n; i++) {
                table[(int) (k[i] - lo)] = l[i];
            }
            code.tableSwitch((int) lo, (int) hi, dflt, table);
        } else {
            code.lookupSwitch(dflt, k, l);
        }
    }

    private void genIntDispatch(Tree.Switch sw, Code.Label[] bodies, Code.Label dflt) {
        genExprAs(sw.selector, Type.INT);
        List<Integer> keys = new ArrayList<Integer>();
        List<Code.Label> targets = new ArrayList<Code.Label>();
        for (int i = 0; i < sw.cases.size(); i++) {
            for (Tree l : sw.cases.get(i).labels) {
                if (l.constant != null) {
                    keys.add(Integer.valueOf(Constants.intValue(l.constant)));
                    targets.add(bodies[i]);
                }
            }
        }
        emitSwitch(keys, targets, dflt);
    }

    private void genStringDispatch(Tree.Switch sw, Code.Label[] bodies, Code.Label dflt) {
        Type st = genExpr(sw.selector);
        coerce(st, syms.stringType);
        int tmp = code.newLocal(syms.stringType);
        code.varOp(Code.ASTORE, tmp);
        code.varOp(Code.ALOAD, tmp);
        invoke(Code.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        code.op(Code.POP);
        for (int i = 0; i < sw.cases.size(); i++) {
            for (Tree l : sw.cases.get(i).labels) {
                if (l.constant instanceof String) {
                    code.ldc(pool.string((String) l.constant));
                    code.varOp(Code.ALOAD, tmp);
                    invoke(Code.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
                    code.jump(Code.IFNE, bodies[i]);
                }
            }
        }
        code.jump(Code.GOTO, dflt);
    }

    private int enumOrdinal(VarSymbol constant) {
        int i = 0;
        for (VarSymbol f : constant.owner.fields()) {
            if (f == constant) {
                return i;
            }
            if ((f.flags & Symbol.ACC_ENUM) != 0) {
                i++;
            }
        }
        throw new CompileError("not an enum constant: " + constant.name);
    }

    private void genEnumDispatch(Tree.Switch sw, Code.Label[] bodies, Code.Label dflt) {
        Type st = genExpr(sw.selector);
        Type et = erased(sw.selector.type);
        coerce(st, et);
        invoke(Code.INVOKEVIRTUAL, internalName(et), "ordinal", "()I", false);
        List<Integer> keys = new ArrayList<Integer>();
        List<Code.Label> targets = new ArrayList<Code.Label>();
        for (int i = 0; i < sw.cases.size(); i++) {
            for (Tree l : sw.cases.get(i).labels) {
                Symbol s = l instanceof Tree.Ident ? ((Tree.Ident) l).sym : l instanceof Tree.Select ? ((Tree.Select) l).sym : null;
                if (s instanceof VarSymbol) {
                    keys.add(Integer.valueOf(enumOrdinal((VarSymbol) s)));
                    targets.add(bodies[i]);
                }
            }
        }
        emitSwitch(keys, targets, dflt);
    }

    private void genPatternDispatch(Tree.Switch sw, Code.Label[] bodies, Code.Label dflt) {
        Type st = genExpr(sw.selector);
        Type vt = st.isPrimitive() ? syms.boxedType(st) : st.tag == Type.Tag.NULL ? syms.objectType : st;
        coerce(st, vt);
        int tmp = code.newLocal(vt);
        code.varOp(Code.ASTORE, tmp);
        Code.Label nullTarget = null;
        for (int i = 0; i < sw.cases.size(); i++) {
            if (sw.cases.get(i).hasNull) {
                nullTarget = bodies[i];
            }
        }
        if (nullTarget != null) {
            code.varOp(Code.ALOAD, tmp);
            code.jump(Code.IFNULL, nullTarget);
        } else {
            code.varOp(Code.ALOAD, tmp);
            invoke(Code.INVOKESTATIC, "java/util/Objects", "requireNonNull", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            code.op(Code.POP);
        }
        Type unboxedSel = syms.unboxedType(erased(sw.selector.type));
        for (int i = 0; i < sw.cases.size(); i++) {
            Tree.Case c = sw.cases.get(i);
            for (Tree l : c.labels) {
                Code.Label next = new Code.Label();
                if (l instanceof Tree.BindingPattern || l instanceof Tree.RecordPattern) {
                    Type pt = erased(l.type);
                    if (!isSubclass(vt, pt) || l instanceof Tree.RecordPattern) {
                        code.varOp(Code.ALOAD, tmp);
                        code.op2(Code.INSTANCEOF, pool.cls(internalName(pt)));
                        code.jump(Code.IFEQ, next);
                    }
                    genPatternMatch(l, tmp, vt, next);
                } else {
                    Symbol s = l instanceof Tree.Ident ? ((Tree.Ident) l).sym : l instanceof Tree.Select ? ((Tree.Select) l).sym : null;
                    if (s instanceof VarSymbol && (s.flags & Symbol.ACC_ENUM) != 0) {
                        VarSymbol f = (VarSymbol) s;
                        code.varOp(Code.ALOAD, tmp);
                        code.op2(Code.GETSTATIC, pool.field(f.owner.internalName, f.name, "L" + f.owner.internalName + ";"));
                        code.jump(Code.IF_ACMPNE, next);
                    } else if (l.constant != null) {
                        Object cv = l.constant;
                        Type ct = cv instanceof String ? syms.stringType : unboxedSel != null ? unboxedSel : erased(l.type);
                        pushConst(cv, ct);
                        if (ct.isPrimitive()) {
                            box(ct);
                        }
                        code.varOp(Code.ALOAD, tmp);
                        invoke(Code.INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z", false);
                        code.jump(Code.IFEQ, next);
                    } else {
                        continue;
                    }
                }
                if (c.guard != null) {
                    genCond(c.guard, next, false);
                }
                code.jump(Code.GOTO, bodies[i]);
                code.place(next);
                code.alive = true;
            }
        }
        code.jump(Code.GOTO, dflt);
    }

    // ================================================================== lambdas

    private String lambdaName(Tree.Lambda l) {
        String name = lambdaNames.get(l);
        if (name == null) {
            String base = method == null ? "static" : method.isConstructor() ? "new"
                    : "<clinit>".equals(method.name) ? "static" : method.name;
            if (base.startsWith("lambda$")) {
                base = base.substring(7, base.lastIndexOf('$') > 7 ? base.lastIndexOf('$') : base.length());
            }
            name = "lambda$" + base + "$" + cls.lambdaCount++;
            lambdaNames.put(l, name);
            pendingLambdas.add(l);
        }
        return name;
    }

    /** Parameter types of the lambda's body method as the functional method's caller passes them. */
    private List<Type> lambdaParamTypes(Tree.Lambda l) {
        List<Type> out = new ArrayList<Type>();
        MethodSymbol sam = l.target;
        for (int i = 0; i < l.params.size(); i++) {
            Type pt = erased(l.params.get(i).sym.type);
            Type samParam = erased(sam.params.get(i));
            if (pt.isPrimitive() != samParam.isPrimitive() || !pt.isPrimitive() && !isSubclass(pt, samParam)) {
                pt = samParam;
            }
            out.add(pt);
        }
        return out;
    }

    private Type lambdaReturnType(Tree.Lambda l) {
        MethodSymbol sam = l.target;
        if (sam.returnType.tag == Type.Tag.VOID) {
            return Type.VOID;
        }
        Map<Type.TypeVar, Type> map = types.memberMapping(l.targetType, sam.owner);
        Type r = Type.substitute(sam.returnType, map);
        if (r.tag == Type.Tag.WILDCARD) {
            r = types.upperBound(r);
        }
        Type er = erased(r);
        Type samRet = erased(sam.returnType);
        if (er.isPrimitive() != samRet.isPrimitive() || !er.isPrimitive() && !isSubclass(er, samRet)) {
            er = samRet;
        }
        return er;
    }

    private String implDescriptor(Tree.Lambda l) {
        StringBuilder b = new StringBuilder("(");
        if (l.capturesThis) {
            b.append('L').append(cls.internalName).append(';');
        }
        if (l.boundVar != null) {
            b.append(desc(erased(l.boundVar.type)));
        }
        if (l.captured != null) {
            for (VarSymbol v : l.captured) {
                b.append(desc(erased(v.type)));
            }
        }
        for (Type p : lambdaParamTypes(l)) {
            b.append(desc(p));
        }
        return b.append(')').append(desc(lambdaReturnType(l))).toString();
    }

    private Type genLambda(Tree.Lambda l) {
        String name = lambdaName(l);
        StringBuilder indyDesc = new StringBuilder("(");
        if (l.capturesThis) {
            loadThis();
            indyDesc.append('L').append(cls.internalName).append(';');
        }
        if (l.boundVar != null) {
            Type bt = genExpr(l.boundExpr);
            Type want = erased(l.boundVar.type);
            coerce(bt, want);
            nullCheck();
            indyDesc.append(desc(want));
        }
        if (l.captured != null) {
            for (VarSymbol v : l.captured) {
                Type vt = loadVar(v);
                Type want = erased(v.type);
                coerce(vt, want);
                indyDesc.append(desc(want));
            }
        }
        Type target = erased(l.targetType);
        indyDesc.append(')').append(desc(target));
        MethodSymbol sam = l.target;
        String samDesc = sam.owner.decl == null && sam.descriptor != null ? sam.descriptor : jvmDescriptor(sam);
        StringBuilder inst = new StringBuilder("(");
        for (Type p : lambdaParamTypes(l)) {
            inst.append(desc(p));
        }
        inst.append(')').append(desc(lambdaReturnType(l)));
        List<String> markers = new ArrayList<String>();
        if (l.targetType != null && l.targetType.tag == Type.Tag.INTERSECTION) {
            // (Runnable & Serializable) () -> ...: the extra interfaces are LambdaMetafactory markers.
            String main = internalName(target);
            for (Type b : ((Type.IntersectionType) l.targetType).bounds) {
                String n = internalName(b);
                if (!n.equals(main) && !markers.contains(n)) {
                    markers.add(n);
                }
            }
        }
        int bsm;
        int[] args;
        if (markers.isEmpty()) {
            bsm = pool.methodHandle(6, "java/lang/invoke/LambdaMetafactory", "metafactory", LMF_DESC, false);
            args = new int[]{
                pool.methodType(samDesc),
                pool.methodHandle(6, cls.internalName, name, implDescriptor(l), cls.isInterface()),
                pool.methodType(inst.toString())
            };
        } else {
            bsm = pool.methodHandle(6, "java/lang/invoke/LambdaMetafactory", "altMetafactory",
                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                    + "[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;", false);
            args = new int[5 + markers.size()];
            args[0] = pool.methodType(samDesc);
            args[1] = pool.methodHandle(6, cls.internalName, name, implDescriptor(l), cls.isInterface());
            args[2] = pool.methodType(inst.toString());
            args[3] = pool.integer(2);
            args[4] = pool.integer(markers.size());
            for (int i = 0; i < markers.size(); i++) {
                args[5 + i] = pool.cls(markers.get(i));
            }
        }
        int bsmIndex = pool.bootstrapMethod(bsm, args);
        int indy = pool.invokeDynamic(bsmIndex, sam.name, indyDesc.toString());
        code.bc.u1(Code.INVOKEDYNAMIC).u2(indy).u2(0);
        return target;
    }

    private void genLambdaBody(Tree.Lambda l) {
        String name = lambdaNames.get(l);
        MethodSymbol saved = method;
        MethodSymbol m = new MethodSymbol(name, Symbol.ACC_PRIVATE | Symbol.ACC_STATIC | Symbol.ACC_SYNTHETIC, cls);
        m.returnType = Type.VOID;
        beginMethod(m, true);
        method = m;
        if (l.capturesThis) {
            code.newLocal(cls.erasure());
        }
        if (l.boundVar != null) {
            declareLocal(l.boundVar, erased(l.boundVar.type));
        }
        if (l.captured != null) {
            for (VarSymbol v : l.captured) {
                declareLocal(v, erased(v.type));
            }
        }
        List<Type> pts = lambdaParamTypes(l);
        for (int i = 0; i < l.params.size(); i++) {
            declareLocal(l.params.get(i).sym, pts.get(i));
        }
        Type ret = lambdaReturnType(l);
        currentReturnType = ret;
        if (l.body instanceof Tree.Block) {
            genStat(l.body);
            if (code.alive) {
                if (ret.tag == Type.Tag.VOID) {
                    code.op(Code.RETURN);
                } else {
                    code.op(Code.ACONST_NULL);
                    code.op(Code.ATHROW);
                }
            }
        } else {
            code.line(line(l.body.pos));
            if (ret.tag == Type.Tag.VOID) {
                genEffect(l.body);
                code.op(Code.RETURN);
            } else {
                genExprAs(l.body, ret);
                code.op(returnOp(ret));
            }
        }
        endMethod(Symbol.ACC_PRIVATE | Symbol.ACC_STATIC | Symbol.ACC_SYNTHETIC, name, implDescriptor(l), true, false);
        method = saved;
    }
}
