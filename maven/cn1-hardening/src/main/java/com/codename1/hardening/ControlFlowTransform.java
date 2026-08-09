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

    /** Widest encoding of one entry guard (GETSTATIC, IFGT, NEW, DUP, INVOKESPECIAL, ATHROW). */
    private static final int GUARD_BYTES = 16;
    /** Widest encoding of the guard-field setup prepended to {@code <clinit>} (2 calls + PUTSTATIC). */
    private static final int GUARD_INIT_BYTES = 16;
    /**
     * Conservative constant-pool entries the guard adds: the guard field (Utf8/Fieldref/NameAndType),
     * the {@code Runtime}/{@code getRuntime}/{@code availableProcessors} references, and the
     * {@code RuntimeException} constructor reference. Fixed per class, so a class whose pool is already
     * near the 65535-entry limit cannot be guarded at all.
     */
    private static final int GUARD_POOL_OVERHEAD = 32;

    private final ClassLoader hierarchy;
    private final int intensity;
    private int guardedMethods;
    private int oversizedMethods;

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

    /**
     * Methods left unguarded because they are already so close to the 65,535-byte method limit that
     * prepending the guard(s) would overflow them. Skipped rather than aborting the build; reported so
     * a paranoid build knows a large generated method kept its plain control flow.
     */
    public int getOversizedMethods() {
        return oversizedMethods;
    }

    public byte[] transform(byte[] classBytes) {
        ClassNode cn = new ClassNode();
        ClassReader reader = new ClassReader(classBytes);
        reader.accept(cn, ClassReader.SKIP_FRAMES);

        if ((cn.access & Opcodes.ACC_INTERFACE) != 0) {
            return classBytes;
        }
        // The guard adds a field and references (Runtime, RuntimeException, the guard field) at a fixed
        // constant-pool cost. If the class's pool is already so full it cannot fit that overhead, the
        // class cannot be guarded without ClassTooLargeException. Skip it and report the methods that
        // stay plain rather than aborting the entire hardened build.
        if (reader.getItemCount() + GUARD_POOL_OVERHEAD > MethodSize.SAFE_POOL_ITEMS) {
            oversizedMethods += countGuardable(cn);
            return classBytes;
        }
        // Pick a guard field name that collides with no existing member, so a class that happens to
        // declare a zq$cf field (reachable on Android, where the engine doesn't rename first, or in a
        // pre-obfuscated dependency) is still guarded instead of being returned unchanged on the false
        // assumption that the collision means it was already transformed.
        String guardField = resolveGuardField(cn);

        // The guards read a field initialized in <clinit>; if <clinit> is already near the method limit
        // the setup cannot be added, and an uninitialized field is 0 -- which makes every guard take its
        // dead (throwing) arm. So the whole class cannot be guarded then: skip it (reporting the methods
        // that stay plain) rather than corrupt behaviour or abort the build with MethodTooLargeException.
        if (MethodSize.estimateBytes(findClinit(cn)) + GUARD_INIT_BYTES > MethodSize.SAFE_LIMIT) {
            oversizedMethods += countGuardable(cn);
            return classBytes;
        }

        boolean changed = false;
        if (cn.methods != null) {
            for (MethodNode mn : cn.methods) {
                if (!isGuardable(mn)) {
                    continue;
                }
                // A generated method can already be near the 65,535-byte limit; prepending guards would
                // overflow it and make ASM abort the whole build. Skip (and report) such a method rather
                // than fail on a valid input class.
                if (MethodSize.estimateBytes(mn) + GUARD_BYTES * intensity > MethodSize.SAFE_LIMIT) {
                    oversizedMethods++;
                    continue;
                }
                for (int i = 0; i < intensity; i++) {
                    prependGuard(cn, mn, guardField);
                }
                guardedMethods++;
                changed = true;
            }
        }
        if (!changed) {
            return classBytes;
        }

        addGuardField(cn, guardField);
        initGuardField(cn, guardField);

        FrameClassWriter cw = new FrameClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, hierarchy);
        cn.accept(cw);
        if (cw.isHierarchyIncomplete()) {
            // A frame merge collapsed to Object because a supertype is absent from the supplied jars, so
            // the recomputed StackMapTable may be too weak and fail on-device verification. Ship this class
            // UNHARDENED (original valid frames) rather than a possibly-invalid one. Control flow leaves
            // string literals untouched, so no jar-wide literal exclusion is needed here. Reset the guard
            // counts: no guard is actually emitted for a discarded class, and a stale count would let the
            // engine advertise controlFlow and stamp cn1.hardened=true for byte-unmodified output.
            guardedMethods = 0;
            oversizedMethods = 0;
            return classBytes;
        }
        return cw.toByteArray();
    }

    private static MethodNode findClinit(ClassNode cn) {
        if (cn.methods != null) {
            for (MethodNode mn : cn.methods) {
                if ("<clinit>".equals(mn.name) && "()V".equals(mn.desc)) {
                    return mn;
                }
            }
        }
        return null;
    }

    private int countGuardable(ClassNode cn) {
        int n = 0;
        if (cn.methods != null) {
            for (MethodNode mn : cn.methods) {
                if (isGuardable(mn)) {
                    n++;
                }
            }
        }
        return n;
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

    private void prependGuard(ClassNode cn, MethodNode mn, String guardField) {
        InsnList pre = new InsnList();
        LabelNode ok = new LabelNode();
        pre.add(new FieldInsnNode(Opcodes.GETSTATIC, cn.name, guardField, GUARD_DESC));
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

    /** A guard field name not already declared by {@code cn} (lengthens the suffix until free). */
    private String resolveGuardField(ClassNode cn) {
        String name = GUARD_FIELD;
        if (cn.fields != null) {
            boolean clash = true;
            while (clash) {
                clash = false;
                for (FieldNode fn : cn.fields) {
                    if (name.equals(fn.name)) {
                        name = name + "$";
                        clash = true;
                        break;
                    }
                }
            }
        }
        return name;
    }

    private void addGuardField(ClassNode cn, String guardField) {
        if (cn.fields == null) {
            cn.fields = new java.util.ArrayList<FieldNode>();
        }
        cn.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                guardField, GUARD_DESC, null, null));
    }

    private void initGuardField(ClassNode cn, String guardField) {
        InsnList init = new InsnList();
        // zq$cf = Runtime.getRuntime().availableProcessors();  -- contractually >= 1 on every JVM,
        // and a runtime call the optimizer/decompiler cannot fold, so the guard is always taken and
        // can neither NPE nor (unlike a possibly-empty system property) collapse to a zero value.
        init.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Runtime", "getRuntime",
                "()Ljava/lang/Runtime;", false));
        init.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Runtime", "availableProcessors",
                "()I", false));
        init.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, guardField, GUARD_DESC));

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
