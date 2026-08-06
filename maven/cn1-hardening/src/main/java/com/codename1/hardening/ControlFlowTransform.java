/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.hardening;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Inserts an opaque predicate at the entry of each real method: a branch guarded by
 * a value the renamer/decompiler cannot prove, so the disassembly grows a dead
 * arm that a reader must rule out by hand. The guard reads a synthetic per-class
 * field initialized at class-load from a non-constant runtime value
 * ({@code System.getProperty("java.home").length()}, always positive), so neither
 * javac, R8 nor a decompiler can fold it away.
 *
 * <p>This is deliberately conservative -- an entry guard, not control-flow
 * flattening. Flattening fights the ParparVM devirtualizer and the arithmetic
 * reducer, breaks the fused-constructor shape analysis, and must never touch
 * {@code <init>} or {@code @Fused} classes; the engine keeps it off the native
 * ports entirely (see {@link HardeningEngine}). The behaviour is a strict no-op:
 * the dead arm only ever throws and is never reached.
 */
public final class ControlFlowTransform {

    static final String GUARD_FIELD = "zq$cf";
    static final String GUARD_DESC = "I";

    private final ClassLoader hierarchy;
    private final int intensity;
    private int guardedMethods;

    public ControlFlowTransform() {
        this(null, 1);
    }

    public ControlFlowTransform(ClassLoader hierarchy) {
        this(hierarchy, 1);
    }

    /**
     * @param hierarchy a classloader over the (renamed) input classes plus the library jars, used
     *                  for stack-map frame computation; may be {@code null} in tests
     * @param intensity how many opaque-predicate guards to insert per method (paranoid uses 2)
     */
    public ControlFlowTransform(ClassLoader hierarchy, int intensity) {
        this.hierarchy = hierarchy;
        this.intensity = Math.max(1, intensity);
    }

    public int getGuardedMethods() {
        return guardedMethods;
    }

    public byte[] transform(byte[] classBytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(classBytes).accept(cn, ClassReader.SKIP_FRAMES);

        if ((cn.access & Opcodes.ACC_INTERFACE) != 0) {
            return classBytes;
        }
        if (hasGuardField(cn)) {
            return classBytes;
        }

        boolean changed = false;
        if (cn.methods != null) {
            for (MethodNode mn : cn.methods) {
                if (!isGuardable(mn)) {
                    continue;
                }
                for (int i = 0; i < intensity; i++) {
                    prependGuard(cn, mn);
                }
                guardedMethods++;
                changed = true;
            }
        }
        if (!changed) {
            return classBytes;
        }

        addGuardField(cn);
        initGuardField(cn);

        ClassWriter cw = new FrameClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, hierarchy);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private boolean isGuardable(MethodNode mn) {
        if (mn.instructions == null || mn.instructions.size() == 0) {
            return false;
        }
        if ((mn.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
            return false;
        }
        // A guard before super()/this() in a constructor, or before a static field
        // set in <clinit>, is unsafe. Leave both alone.
        if ("<init>".equals(mn.name) || "<clinit>".equals(mn.name)) {
            return false;
        }
        return true;
    }

    private void prependGuard(ClassNode cn, MethodNode mn) {
        InsnList pre = new InsnList();
        LabelNode ok = new LabelNode();
        pre.add(new FieldInsnNode(Opcodes.GETSTATIC, cn.name, GUARD_FIELD, GUARD_DESC));
        // if (zq$cf > 0) goto ok;  -- always taken at runtime, unprovable statically.
        pre.add(new JumpInsnNode(Opcodes.IFGT, ok));
        // dead arm: throw new RuntimeException();  -- never reached.
        pre.add(new TypeInsnNode(Opcodes.NEW, "java/lang/RuntimeException"));
        pre.add(new InsnNode(Opcodes.DUP));
        pre.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "()V", false));
        pre.add(new InsnNode(Opcodes.ATHROW));
        pre.add(ok);
        mn.instructions.insert(pre);
    }

    private boolean hasGuardField(ClassNode cn) {
        if (cn.fields == null) {
            return false;
        }
        for (FieldNode fn : cn.fields) {
            if (GUARD_FIELD.equals(fn.name)) {
                return true;
            }
        }
        return false;
    }

    private void addGuardField(ClassNode cn) {
        if (cn.fields == null) {
            cn.fields = new java.util.ArrayList<FieldNode>();
        }
        cn.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                GUARD_FIELD, GUARD_DESC, null, null));
    }

    private void initGuardField(ClassNode cn) {
        InsnList init = new InsnList();
        // zq$cf = System.getProperty("java.home", "cn1").length();  -- always >= 1, never foldable.
        // The two-arg overload guarantees a non-null result (java.home can be absent on Android),
        // so the guard can never NPE in <clinit>.
        init.add(new LdcInsnNode("java.home"));
        init.add(new LdcInsnNode("cn1"));
        init.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false));
        init.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
        init.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, GUARD_FIELD, GUARD_DESC));

        MethodNode clinit = null;
        if (cn.methods != null) {
            for (MethodNode mn : cn.methods) {
                if ("<clinit>".equals(mn.name) && "()V".equals(mn.desc)) {
                    clinit = mn;
                    break;
                }
            }
        }
        if (clinit == null) {
            clinit = new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.instructions = new InsnList();
            clinit.instructions.add(init);
            clinit.instructions.add(new InsnNode(Opcodes.RETURN));
            cn.methods.add(clinit);
        } else {
            clinit.instructions.insert(init);
        }
    }
}
