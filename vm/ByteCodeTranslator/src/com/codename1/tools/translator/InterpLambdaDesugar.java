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
package com.codename1.tools.translator;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewrites lambdas and method references into ordinary classes.
 *
 * <p>A lambda compiles to an {@code invokedynamic} that asks
 * {@code LambdaMetafactory} to spin a class at run time. Neither target can do
 * that: ParparVM has no {@code invokedynamic} and no {@code defineClass}, and
 * Android is barred from loading dex it generates. The translator already
 * desugars lambdas when it compiles an application ahead of time; a pushed
 * bundle gets no such pass, which is why this one exists.</p>
 *
 * <p>It runs on the desktop, over ASM trees, and produces exactly what the
 * metafactory would have: one class per lambda site implementing the functional
 * interface, holding the captured values in fields and forwarding the single
 * abstract method to the lambda body. The call site becomes a plain static
 * call. Nothing is left for the device to resolve.</p>
 *
 * <p>The synthesized class inherits the source file of the class that contained
 * the lambda, which is both true -- that is where the lambda is written -- and
 * required, since the runtime refuses to execute a class whose source it cannot
 * show the user.</p>
 *
 * @author Shai Almog
 */
final class InterpLambdaDesugar {
    private static final String METAFACTORY = "java/lang/invoke/LambdaMetafactory";

    private InterpLambdaDesugar() {
    }

    /**
     * Desugars every lambda in the given classes.
     *
     * <p>The classes are rewritten in place; the synthesized classes are
     * returned for the caller to add to the bundle.</p>
     */
    static List<ClassNode> desugar(List<ClassNode> classes) {
        List<ClassNode> generated = new ArrayList<ClassNode>();
        for (ClassNode cn : classes) {
            int counter = 0;
            for (MethodNode mn : cn.methods) {
                if (mn.instructions == null) {
                    continue;
                }
                AbstractInsnNode insn = mn.instructions.getFirst();
                while (insn != null) {
                    AbstractInsnNode next = insn.getNext();
                    if (insn instanceof InvokeDynamicInsnNode) {
                        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                        if (isLambda(indy)) {
                            ClassNode lambda = synthesize(cn, indy, counter++);
                            generated.add(lambda);
                            mn.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC,
                                    lambda.name, "create", indy.desc, false));
                            // The captured values are already on the stack in
                            // the order the factory takes them, so replacing the
                            // instruction in place is the whole rewrite.
                        }
                    }
                    insn = next;
                }
            }
        }
        return generated;
    }

    private static boolean isLambda(InvokeDynamicInsnNode indy) {
        return METAFACTORY.equals(indy.bsm.getOwner())
                && ("metafactory".equals(indy.bsm.getName())
                    || "altMetafactory".equals(indy.bsm.getName()));
    }

    /**
     * Builds the class the metafactory would have spun.
     *
     * <p>The first three bootstrap arguments mean the same thing for
     * {@code metafactory} and {@code altMetafactory}. The latter then carries a
     * flags word, marker interfaces and bridge signatures, and those are read
     * rather than dropped: an intersection cast such as {@code (A & B) () ->
     * "x"} names A as a marker and A's erased {@code ()Object} as a bridge, so
     * a class carrying only B and only B's method fails the call site's own
     * cast and cannot answer a call through A.</p>
     */
    private static ClassNode synthesize(ClassNode owner, InvokeDynamicInsnNode indy, int index) {
        Type[] captured = Type.getArgumentTypes(indy.desc);
        Type functionalInterface = Type.getReturnType(indy.desc);
        Type sam = (Type) indy.bsmArgs[0];
        Handle impl = (Handle) indy.bsmArgs[1];
        Type instantiated = (Type) indy.bsmArgs[2];

        ClassNode cn = new ClassNode();
        cn.version = owner.version;
        cn.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | Opcodes.ACC_SYNTHETIC;
        cn.name = owner.name + "$$Lambda$" + index;
        cn.superName = "java/lang/Object";
        cn.interfaces = new ArrayList<String>();
        cn.interfaces.add(functionalInterface.getInternalName());
        cn.sourceFile = owner.sourceFile;
        cn.fields = new ArrayList<FieldNode>();
        cn.methods = new ArrayList<MethodNode>();

        for (int i = 0; i < captured.length; i++) {
            cn.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                    "arg$" + i, captured[i].getDescriptor(), null, null));
        }

        cn.methods.add(constructor(cn, captured));
        cn.methods.add(factory(cn, captured, indy.desc));
        cn.methods.add(samMethod(cn, indy.name, sam, instantiated, impl, captured));
        addAltExtras(cn, indy, sam);
        return cn;
    }

    /// Set in altMetafactory's flags word when the lambda is serializable.
    private static final int FLAG_SERIALIZABLE = 1;

    /// Set in altMetafactory's flags word when marker interfaces follow.
    private static final int FLAG_MARKERS = 2;

    /// Set in altMetafactory's flags word when bridge signatures follow.
    private static final int FLAG_BRIDGES = 4;

    /**
     * Adds altMetafactory's marker interfaces and bridge methods.
     *
     * <p>The extras are positional: a flags word, then -- each only when its
     * flag is set, and in this order -- a count of marker interfaces followed
     * by that many types, then a count of bridge signatures followed by that
     * many method types. A shape that does not parse is left alone rather than
     * guessed at, which is the same outcome as before this was read at all.</p>
     */
    private static void addAltExtras(ClassNode cn, InvokeDynamicInsnNode indy, Type sam) {
        if (!"altMetafactory".equals(indy.bsm.getName()) || indy.bsmArgs.length < 4
                || !(indy.bsmArgs[3] instanceof Integer)) {
            return;
        }
        int flags = ((Integer) indy.bsmArgs[3]).intValue();
        int at = 4;
        if ((flags & FLAG_SERIALIZABLE) != 0
                && !cn.interfaces.contains("java/io/Serializable")) {
            // `(Runnable & Serializable) () -> {}` sets this flag and names no
            // marker at all, and javac still emits the cast to Serializable --
            // so a class carrying only Runnable fails an intersection cast that
            // is otherwise ordinary. The interface is added; writeReplace is
            // not, so such a lambda is Serializable and would fail to serialize,
            // which is what it does on the device anyway.
            cn.interfaces.add("java/io/Serializable");
        }
        if ((flags & FLAG_MARKERS) != 0) {
            if (at >= indy.bsmArgs.length || !(indy.bsmArgs[at] instanceof Integer)) {
                return;
            }
            int count = ((Integer) indy.bsmArgs[at++]).intValue();
            for (int i = 0; i < count && at < indy.bsmArgs.length; i++) {
                Object marker = indy.bsmArgs[at++];
                if (marker instanceof Type) {
                    String name = ((Type) marker).getInternalName();
                    if (!cn.interfaces.contains(name)) {
                        cn.interfaces.add(name);
                    }
                }
            }
        }
        if ((flags & FLAG_BRIDGES) == 0) {
            return;
        }
        if (at >= indy.bsmArgs.length || !(indy.bsmArgs[at] instanceof Integer)) {
            return;
        }
        int count = ((Integer) indy.bsmArgs[at++]).intValue();
        for (int i = 0; i < count && at < indy.bsmArgs.length; i++) {
            Object bridge = indy.bsmArgs[at++];
            if (bridge instanceof Type
                    && !sam.getDescriptor().equals(((Type) bridge).getDescriptor())) {
                cn.methods.add(bridgeMethod(cn, indy.name, sam, (Type) bridge));
            }
        }
    }

    /**
     * One bridge: another erasure of the same method, forwarding to the SAM.
     *
     * <p>Two interfaces can declare the same method with different erased
     * signatures -- {@code Object m()} and {@code String m()} -- and a class
     * implementing both needs a body for each. This one adapts its arguments,
     * calls the real implementation and adapts the result back.</p>
     */
    private static MethodNode bridgeMethod(ClassNode cn, String name, Type sam, Type bridge) {
        Type[] bridgeArgs = bridge.getArgumentTypes();
        Type[] samArgs = sam.getArgumentTypes();
        MethodNode mn = new MethodNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC,
                name, bridge.getDescriptor(), null, null);
        InsnList il = mn.instructions;
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int local = 1;
        int stack = 1;
        for (int i = 0; i < bridgeArgs.length; i++) {
            il.add(new VarInsnNode(bridgeArgs[i].getOpcode(Opcodes.ILOAD), local));
            local += bridgeArgs[i].getSize();
            if (i < samArgs.length) {
                adapt(il, bridgeArgs[i], samArgs[i]);
            }
            stack += 2;
        }
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, cn.name, name,
                sam.getDescriptor(), false));
        Type samReturn = sam.getReturnType();
        Type bridgeReturn = bridge.getReturnType();
        if (bridgeReturn.getSort() == Type.VOID) {
            if (samReturn.getSort() != Type.VOID) {
                il.add(new InsnNode(samReturn.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
            }
            il.add(new InsnNode(Opcodes.RETURN));
        } else {
            adapt(il, samReturn, bridgeReturn);
            il.add(new InsnNode(bridgeReturn.getOpcode(Opcodes.IRETURN)));
        }
        mn.maxStack = Math.max(stack + 2, 4);
        mn.maxLocals = local;
        return mn;
    }

    private static MethodNode constructor(ClassNode cn, Type[] captured) {
        MethodNode mn = new MethodNode(Opcodes.ACC_PRIVATE, "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, captured), null, null);
        InsnList il = mn.instructions;
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        int local = 1;
        for (int i = 0; i < captured.length; i++) {
            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
            il.add(new VarInsnNode(captured[i].getOpcode(Opcodes.ILOAD), local));
            il.add(new FieldInsnNode(Opcodes.PUTFIELD, cn.name, "arg$" + i,
                    captured[i].getDescriptor()));
            local += captured[i].getSize();
        }
        il.add(new InsnNode(Opcodes.RETURN));
        mn.maxStack = 3;
        mn.maxLocals = local;
        return mn;
    }

    /**
     * The static factory the call site invokes.
     *
     * <p>It exists so the rewrite is a one-instruction substitution. Doing
     * {@code new} at the call site would need the reference underneath the
     * captured values that are already on the stack, which cannot be arranged
     * without shuffling a stack whose shape depends on the captures.</p>
     */
    private static MethodNode factory(ClassNode cn, Type[] captured, String desc) {
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "create",
                desc, null, null);
        InsnList il = mn.instructions;
        il.add(new TypeInsnNode(Opcodes.NEW, cn.name));
        il.add(new InsnNode(Opcodes.DUP));
        int local = 0;
        int size = 0;
        for (int i = 0; i < captured.length; i++) {
            il.add(new VarInsnNode(captured[i].getOpcode(Opcodes.ILOAD), local));
            local += captured[i].getSize();
            size += captured[i].getSize();
        }
        il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, cn.name, "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, captured), false));
        il.add(new InsnNode(Opcodes.ARETURN));
        mn.maxStack = size + 2;
        mn.maxLocals = Math.max(local, 1);
        return mn;
    }

    /** The functional interface's single abstract method, forwarding to the body. */
    private static MethodNode samMethod(ClassNode cn, String name, Type sam, Type instantiated,
                                        Handle impl, Type[] captured) {
        Type[] samArgs = sam.getArgumentTypes();
        Type[] instArgs = instantiated.getArgumentTypes();
        Type[] implArgs = Type.getArgumentTypes(impl.getDesc());

        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, name, sam.getDescriptor(), null, null);
        InsnList il = mn.instructions;

        boolean isConstructorRef = impl.getTag() == Opcodes.H_NEWINVOKESPECIAL;
        boolean hasReceiver = impl.getTag() == Opcodes.H_INVOKEVIRTUAL
                || impl.getTag() == Opcodes.H_INVOKEINTERFACE
                || impl.getTag() == Opcodes.H_INVOKESPECIAL;

        // What the body expects, in order. A bound method reference takes its
        // receiver from the captures; an unbound one takes it from the first
        // argument of the interface method. Both are just the first value.
        List<Type> targets = new ArrayList<Type>();
        if (hasReceiver) {
            targets.add(Type.getObjectType(impl.getOwner()));
        }
        for (int i = 0; i < implArgs.length; i++) {
            targets.add(implArgs[i]);
        }

        if (isConstructorRef) {
            il.add(new TypeInsnNode(Opcodes.NEW, impl.getOwner()));
            il.add(new InsnNode(Opcodes.DUP));
        }

        int target = 0;
        int stack = isConstructorRef ? 2 : 0;
        for (int i = 0; i < captured.length; i++) {
            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
            il.add(new FieldInsnNode(Opcodes.GETFIELD, cn.name, "arg$" + i,
                    captured[i].getDescriptor()));
            adapt(il, captured[i], targets.get(target++));
            stack += 2;
        }
        int local = 1;
        for (int i = 0; i < samArgs.length; i++) {
            il.add(new VarInsnNode(samArgs[i].getOpcode(Opcodes.ILOAD), local));
            local += samArgs[i].getSize();
            // The interface signature is erased; the lambda body is written
            // against the instantiated one, so the value is narrowed first.
            Type given = i < instArgs.length ? instArgs[i] : samArgs[i];
            adapt(il, samArgs[i], given);
            adapt(il, given, targets.get(target++));
            stack += 2;
        }

        il.add(new MethodInsnNode(invokeOpcode(impl), impl.getOwner(), impl.getName(),
                impl.getDesc(), impl.isInterface()));

        Type implReturn = isConstructorRef
                ? Type.getObjectType(impl.getOwner())
                : Type.getReturnType(impl.getDesc());
        Type samReturn = sam.getReturnType();
        if (samReturn.getSort() == Type.VOID) {
            if (implReturn.getSort() != Type.VOID) {
                il.add(new InsnNode(implReturn.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
            }
            il.add(new InsnNode(Opcodes.RETURN));
        } else {
            adapt(il, implReturn, samReturn);
            il.add(new InsnNode(samReturn.getOpcode(Opcodes.IRETURN)));
        }

        mn.maxStack = Math.max(stack + 4, 6);
        mn.maxLocals = local;
        return mn;
    }

    private static int invokeOpcode(Handle impl) {
        switch (impl.getTag()) {
            case Opcodes.H_INVOKESTATIC:
                return Opcodes.INVOKESTATIC;
            case Opcodes.H_INVOKEINTERFACE:
                return Opcodes.INVOKEINTERFACE;
            case Opcodes.H_INVOKESPECIAL:
            case Opcodes.H_NEWINVOKESPECIAL:
                return Opcodes.INVOKESPECIAL;
            default:
                return Opcodes.INVOKEVIRTUAL;
        }
    }

    /**
     * Converts a value of one type to another, the way the metafactory would.
     *
     * <p>Boxing, unboxing, primitive widening and reference narrowing are all
     * permitted between a functional interface's erased signature and the body
     * it is bound to, and a lambda over {@code Integer} calling a method taking
     * {@code int} is entirely ordinary.</p>
     */
    private static void adapt(InsnList il, Type from, Type to) {
        if (from.equals(to) || to.getSort() == Type.VOID) {
            return;
        }
        boolean fromPrimitive = isPrimitive(from);
        boolean toPrimitive = isPrimitive(to);

        if (fromPrimitive && toPrimitive) {
            widen(il, from, to);
            return;
        }
        if (fromPrimitive) {
            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, boxed(from), "valueOf",
                    "(" + from.getDescriptor() + ")L" + boxed(from) + ";", false));
            return;
        }
        if (toPrimitive) {
            // Unbox what actually arrived, then widen. Casting to the
            // destination's wrapper instead is wrong whenever the two differ:
            // `Function<Integer, Long> f = Long::valueOf` hands the SAM an
            // Integer and the implementation wants a long, and a CHECKCAST to
            // Long fails on an Integer that was never anything else.
            Type source = from.getSort() == Type.OBJECT && isBoxed(from)
                    ? unboxedType(from) : to;
            String box = boxed(source);
            il.add(new TypeInsnNode(Opcodes.CHECKCAST, box));
            il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, box, unboxMethod(source),
                    "()" + source.getDescriptor(), false));
            widen(il, source, to);
            return;
        }
        if (!"java/lang/Object".equals(to.getInternalName())
                && to.getSort() == Type.OBJECT) {
            il.add(new TypeInsnNode(Opcodes.CHECKCAST, to.getInternalName()));
        }
    }

    /// Whether a reference type is one of the eight primitive wrappers.
    private static boolean isBoxed(Type t) {
        return unboxedTypeOrNull(t) != null;
    }

    /// The primitive a wrapper wraps.
    private static Type unboxedType(Type t) {
        Type p = unboxedTypeOrNull(t);
        return p == null ? t : p;
    }

    private static Type unboxedTypeOrNull(Type t) {
        String n = t.getInternalName();
        if ("java/lang/Integer".equals(n)) {
            return Type.INT_TYPE;
        }
        if ("java/lang/Long".equals(n)) {
            return Type.LONG_TYPE;
        }
        if ("java/lang/Short".equals(n)) {
            return Type.SHORT_TYPE;
        }
        if ("java/lang/Byte".equals(n)) {
            return Type.BYTE_TYPE;
        }
        if ("java/lang/Character".equals(n)) {
            return Type.CHAR_TYPE;
        }
        if ("java/lang/Boolean".equals(n)) {
            return Type.BOOLEAN_TYPE;
        }
        if ("java/lang/Float".equals(n)) {
            return Type.FLOAT_TYPE;
        }
        if ("java/lang/Double".equals(n)) {
            return Type.DOUBLE_TYPE;
        }
        return null;
    }

    private static void widen(InsnList il, Type from, Type to) {
        // byte, short, char and boolean are all int on the stack, so only the
        // four wide conversions actually emit anything.
        int f = from.getSort();
        int t = to.getSort();
        if (t == Type.LONG && f != Type.LONG) {
            il.add(new InsnNode(f == Type.FLOAT ? Opcodes.F2L : f == Type.DOUBLE ? Opcodes.D2L : Opcodes.I2L));
        } else if (t == Type.FLOAT && f != Type.FLOAT) {
            il.add(new InsnNode(f == Type.LONG ? Opcodes.L2F : f == Type.DOUBLE ? Opcodes.D2F : Opcodes.I2F));
        } else if (t == Type.DOUBLE && f != Type.DOUBLE) {
            il.add(new InsnNode(f == Type.LONG ? Opcodes.L2D : f == Type.FLOAT ? Opcodes.F2D : Opcodes.I2D));
        }
    }

    private static boolean isPrimitive(Type t) {
        int s = t.getSort();
        return s != Type.OBJECT && s != Type.ARRAY && s != Type.METHOD;
    }

    private static String boxed(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN: return "java/lang/Boolean";
            case Type.BYTE: return "java/lang/Byte";
            case Type.CHAR: return "java/lang/Character";
            case Type.SHORT: return "java/lang/Short";
            case Type.INT: return "java/lang/Integer";
            case Type.LONG: return "java/lang/Long";
            case Type.FLOAT: return "java/lang/Float";
            default: return "java/lang/Double";
        }
    }

    private static String unboxMethod(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN: return "booleanValue";
            case Type.BYTE: return "byteValue";
            case Type.CHAR: return "charValue";
            case Type.SHORT: return "shortValue";
            case Type.INT: return "intValue";
            case Type.LONG: return "longValue";
            case Type.FLOAT: return "floatValue";
            default: return "doubleValue";
        }
    }
}
