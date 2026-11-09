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
package com.codename1.tools.translator;

import com.codename1.tools.translator.bytecodes.CustomIntruction;
import com.codename1.tools.translator.bytecodes.Invoke;
import com.codename1.tools.translator.classfile.Opcodes;
import com.codename1.tools.translator.classfile.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lowers a lambda to a function pointer instead of a class.
 *
 * A lambda is a function plus the values it captured; it has no behaviour of its own
 * beyond calling that function. Giving every lambda its own class costs a struct, a
 * class object, a vtable, a constructor, a factory and a virtual wrapper per inherited
 * method -- about 150 lines of C and one of the 65535 class ids each -- and a Python
 * application transpiles to tens of thousands of lambdas.
 *
 * Here a lambda is an instance of a CARRIER: one class per functional interface and
 * capture shape, shared by every lambda of that shape. It holds a function address
 * in {@code fn} and the captures in {@code c0..cN}, erased to Object, int, long, float
 * or double, and its single method calls {@code fn(captures..., arguments...)}.
 *
 * That is the calling convention javac already compiles a lambda body to -- a private
 * method of the enclosing class taking the captures first, with {@code this} as the
 * receiver when the body uses it -- so {@code fn} is normally that method itself, and a
 * static method reference is the referenced method. Only when the C types differ
 * (boxing, widening, a constructor reference, a virtual target) does the declaring
 * class get a static bridge with the same convention that adapts and calls on.
 *
 * So calling a lambda is the interface dispatch it always was, followed by one
 * indirect call. A lambda that captures nothing is still allocated once.
 *
 * Only the C targets use this. The JavaScript backend keeps a class per lambda,
 * because its type-flow analysis summarises each lambda class individually.
 */
final class LambdaCarriers {
    private static final Map<String, ByteCodeClass> CARRIERS = new HashMap<String, ByteCodeClass>();
    private static int counter;

    private LambdaCarriers() {
    }

    static void reset() {
        CARRIERS.clear();
        counter = 0;
    }

    /** The erased kind a capture of this type is stored as: O, I, J, F or D. */
    static char kind(Type t) {
        switch (t.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                return 'O';
            case Type.LONG:
                return 'J';
            case Type.FLOAT:
                return 'F';
            case Type.DOUBLE:
                return 'D';
            default:
                return 'I';
        }
    }

    static String fieldDesc(char kind) {
        return kind == 'O' ? "Ljava/lang/Object;" : String.valueOf(kind);
    }

    /** The C type a capture of this kind is stored and passed as. */
    static String kindCType(char kind) {
        switch (kind) {
            case 'O': return "JAVA_OBJECT";
            case 'J': return "JAVA_LONG";
            case 'F': return "JAVA_FLOAT";
            case 'D': return "JAVA_DOUBLE";
            default: return "JAVA_INT";
        }
    }

    private static String kindMember(char kind) {
        switch (kind) {
            case 'O': return "o";
            case 'J': return "l";
            case 'F': return "f";
            case 'D': return "d";
            default: return "i";
        }
    }

    static String shape(Type[] captures) {
        StringBuilder b = new StringBuilder(captures.length);
        for (Type t : captures) {
            b.append(kind(t));
        }
        return b.toString();
    }

    static String cType(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN: return "JAVA_BOOLEAN";
            case Type.BYTE: return "JAVA_BYTE";
            case Type.CHAR: return "JAVA_CHAR";
            case Type.SHORT: return "JAVA_SHORT";
            case Type.INT: return "JAVA_INT";
            case Type.LONG: return "JAVA_LONG";
            case Type.FLOAT: return "JAVA_FLOAT";
            case Type.DOUBLE: return "JAVA_DOUBLE";
            case Type.VOID: return "JAVA_VOID";
            default: return "JAVA_OBJECT";
        }
    }

    private static String push(Type t) {
        switch (t.getSort()) {
            case Type.LONG: return "PUSH_LONG";
            case Type.FLOAT: return "PUSH_FLOAT";
            case Type.DOUBLE: return "PUSH_DOUBLE";
            case Type.OBJECT:
            case Type.ARRAY: return "PUSH_OBJ";
            default: return "PUSH_INT";
        }
    }

    /** The carrier's interface method. */
    static BytecodeMethod sam(ByteCodeClass carrier, String name, String desc) {
        for (BytecodeMethod m : carrier.getMethods()) {
            if (name.equals(m.getMethodName()) && desc.equals(m.getSignature())) {
                return m;
            }
        }
        return null;
    }

    /**
     * The carrier for lambdas implementing {@code samName samDesc} of {@code iface}
     * with captures of the given shape, created on first use.
     */
    static ByteCodeClass carrier(String iface, String samName, String samDesc, String shape,
            List<ByteCodeClass> classes) {
        String key = iface + '.' + samName + samDesc + '/' + shape;
        ByteCodeClass c = CARRIERS.get(key);
        if (c != null) {
            return c;
        }
        String original = iface + "$cn1fn" + (counter++);
        String name = Util.mangle(original);
        c = new ByteCodeClass(name, original);
        c.setBaseClass("java/lang/Object");
        c.setBaseInterfaces(new String[]{iface});
        c.addField(new ByteCodeField(name, Opcodes.ACC_PRIVATE, "fn", "J", null, null));
        for (int i = 0; i < shape.length(); i++) {
            c.addField(new ByteCodeField(name, Opcodes.ACC_PRIVATE, "c" + i, fieldDesc(shape.charAt(i)), null, null));
        }

        BytecodeMethod ctor = new BytecodeMethod(name, Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        c.addMethod(ctor);
        ctor.addVariableOperation(Opcodes.ALOAD, 0);
        ctor.addInvoke(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.addInstruction(Opcodes.RETURN);
        ctor.setMaxes(1, 1);

        // The interface method: call fn with the captures, in order, followed by the
        // arguments exactly as received. Captures and fn are read with real GETFIELDs
        // (so the dead-field pass sees their readers) and taken off the stack by C.
        BytecodeMethod sam = new BytecodeMethod(name, Opcodes.ACC_PUBLIC, samName, samDesc, null, null);
        c.addMethod(sam);
        Type[] args = Type.getArgumentTypes(samDesc);
        Type ret = Type.getReturnType(samDesc);
        int n = shape.length();
        for (int i = 0; i < n; i++) {
            sam.addVariableOperation(Opcodes.ALOAD, 0);
            sam.addField(c, Opcodes.GETFIELD, name, "c" + i, fieldDesc(shape.charAt(i)));
        }
        sam.addVariableOperation(Opcodes.ALOAD, 0);
        sam.addField(c, Opcodes.GETFIELD, name, "fn", "J");
        StringBuilder fnType = new StringBuilder("(").append(cType(ret)).append(" (*)(CODENAME_ONE_THREAD_STATE");
        StringBuilder call = new StringBuilder("(threadStateData");
        for (int i = 0; i < n; i++) {
            char k = shape.charAt(i);
            fnType.append(", ").append(kindCType(k));
            call.append(", SP[-").append(n + 1 - i).append("].data.").append(kindMember(k));
        }
        for (int i = 0; i < args.length; i++) {
            fnType.append(", ").append(cType(args[i]));
            call.append(", __cn1Arg").append(i + 1);
        }
        fnType.append("))");
        call.append(')');
        String target = "(" + fnType + "(intptr_t)SP[-1].data.l)" + call;
        String code = ret.getSort() == Type.VOID
                ? "    " + target + "; SP -= " + (n + 1) + ";\n"
                : "    { " + cType(ret) + " cn1fnResult = " + target + "; SP -= " + (n + 1) + "; "
                        + push(ret) + "(cn1fnResult); }\n";
        sam.addRawInstruction(new IndirectCall(code));
        sam.addInstruction(ret.getOpcode(Opcodes.IRETURN));
        sam.setMaxes(n + 4, args.length * 2 + 1);

        CARRIERS.put(key, c);
        classes.add(c);
        return c;
    }

    /** The call through fn: a call, so its method gets a real frame. */
    static final class IndirectCall extends CustomIntruction {
        IndirectCall(String code) {
            super(code, code, null);
        }

        @Override
        public boolean isComplexInstruction() {
            return true;
        }
    }

    /**
     * Pushes the address of a static method as a long. It counts as a call of that
     * method, so the dead-code pass keeps a bridge that nothing calls by name.
     */
    static final class FunctionAddress extends CustomIntruction {
        private final Invoke target;

        FunctionAddress(String owner, String name, String desc) {
            super(code(owner, name, desc), code(owner, name, desc),
                    java.util.Collections.singletonList(Util.mangle(owner)));
            target = new Invoke(Opcodes.INVOKESTATIC, owner, name, desc, false);
        }

        private static String code(String owner, String name, String desc) {
            StringBuilder sym = new StringBuilder(Util.mangle(owner)).append('_').append(name).append("__");
            BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, sym, new ArrayList<String>());
            return "    PUSH_LONG((JAVA_LONG)(intptr_t)&" + sym + ");\n";
        }

        @Override public String getMethodUsed() { return target.getMethodUsed(); }
        @Override public String getMethodName() { return target.getMethodName(); }
        @Override public String getSignature() { return target.getSignature(); }
        @Override public boolean containsSignature(SignatureSet s) { return target.containsSignature(s); }
    }
}
