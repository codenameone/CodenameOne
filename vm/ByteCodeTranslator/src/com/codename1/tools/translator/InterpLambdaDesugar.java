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
     * <p>{@code altMetafactory} carries extra marker interfaces and bridge
     * signatures after the first three arguments. The three that matter are in
     * the same positions for both, and a marker interface is by definition
     * empty, so the extras are ignored deliberately rather than by oversight --
     * a serializable lambda runs, it simply is not serializable, and nothing on
     * the device could serialize it anyway.</p>
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
        return cn;
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
            String box = boxed(to);
            il.add(new TypeInsnNode(Opcodes.CHECKCAST, box));
            il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, box, unboxMethod(to),
                    "()" + to.getDescriptor(), false));
            return;
        }
        if (!"java/lang/Object".equals(to.getInternalName())
                && to.getSort() == Type.OBJECT) {
            il.add(new TypeInsnNode(Opcodes.CHECKCAST, to.getInternalName()));
        }
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
