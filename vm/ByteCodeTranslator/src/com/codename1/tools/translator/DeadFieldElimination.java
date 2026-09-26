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
package com.codename1.tools.translator;

import com.codename1.tools.translator.bytecodes.BasicInstruction;
import com.codename1.tools.translator.bytecodes.Field;
import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.Ldc;
import com.codename1.tools.translator.bytecodes.VarOp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;

/**
 * Removes instance fields the program never reads.
 *
 * <p>A closed world can prove what a JVM cannot: every reader of a field is a
 * GETFIELD somewhere in the program or a reference in native source, and both are in
 * front of the translator. A field with neither is only ever written, so its storage,
 * its accessors and its slot in the mark function are dead weight in every instance --
 * and when it holds a reference, the object it points at is kept alive for nothing.
 *
 * <p>Runs on the raw bytecode, before any fusion pass, so every access is still a plain
 * GETFIELD or PUTFIELD. Reads are counted over every parsed method, including ones the
 * cull will delete later: conservative, and it means no later rewrite (getter inlining,
 * constructor fusion) can hide a read from this pass. A store whose object and value
 * are plain loads or constants -- the {@code this.f = arg} shape constructors are made
 * of -- is deleted whole, which leaves the constructor in a shape the constructor
 * fusions still recognise; any other store of a removed field becomes pops of its value
 * and its object, so the value expression is still evaluated. Every edited method's raw
 * plans are then re-snapshotted, because the fusions captured them while parsing.
 *
 * <p>One deliberate difference from the JVM: a PUTFIELD of a removed field on a null
 * object no longer throws NullPointerException. The store's only effect was that
 * exception, and relying on it from a field nothing reads is not a pattern worth the
 * cost of a check on every store.
 *
 * <p>Kept: every field of a {@code java.*} class (the runtime and the translator name
 * those in hand-written C, some of them by composed names no scan can find), every field
 * whose C member or accessor name appears in native source, and every field of a class
 * the pass cannot resolve a read against.
 */
final class DeadFieldElimination {
    private DeadFieldElimination() {
    }

    static int removedFields;
    static int rewrittenStores;

    static void run(List<ByteCodeClass> classes, String[] nativeSources) {
        removedFields = 0;
        rewrittenStores = 0;
        Map<String, ByteCodeClass> byName = new HashMap<String, ByteCodeClass>();
        for (ByteCodeClass bc : classes) {
            byName.put(bc.getClsName(), bc);
        }
        Set<String> nativeTokens = tokenize(nativeSources);
        Set<String> read = new HashSet<String>();
        Set<String> unresolvedReadNames = new HashSet<String>();
        for (ByteCodeClass bc : classes) {
            for (BytecodeMethod m : bc.getMethods()) {
                for (Instruction ins : m.getInstructions()) {
                    if (ins instanceof Field && ins.getOpcode() == Opcodes.GETFIELD) {
                        Field f = (Field) ins;
                        ByteCodeClass decl = declaring(byName, Util.mangle(f.getOwner()), f.getFieldName());
                        if (decl == null) {
                            unresolvedReadNames.add(f.getFieldName());
                        } else {
                            read.add(decl.getClsName() + "." + f.getFieldName());
                        }
                    }
                }
            }
        }
        Set<String> dead = new HashSet<String>();
        for (ByteCodeClass bc : classes) {
            String cls = bc.getClsName();
            if (cls.startsWith("java_")) {
                continue;
            }
            for (ByteCodeField bf : bc.getFields()) {
                if (bf.isStaticField()) {
                    continue;
                }
                String name = bf.getFieldName();
                String member = cls + "_" + name;
                if (read.contains(cls + "." + name) || unresolvedReadNames.contains(name)
                        || nativeTokens.contains(member)
                        || nativeTokens.contains("get_field_" + member)
                        || nativeTokens.contains("set_field_" + member)) {
                    continue;
                }
                dead.add(cls + "." + name);
            }
        }
        if (dead.isEmpty()) {
            return;
        }
        Set<BytecodeMethod> edited = new HashSet<BytecodeMethod>();
        for (ByteCodeClass bc : classes) {
            for (BytecodeMethod m : bc.getMethods()) {
                List<Instruction> ins = m.getInstructions();
                for (int i = 0; i < ins.size(); i++) {
                    Instruction x = ins.get(i);
                    if (!(x instanceof Field) || x.getOpcode() != Opcodes.PUTFIELD) {
                        continue;
                    }
                    Field f = (Field) x;
                    ByteCodeClass decl = declaring(byName, Util.mangle(f.getOwner()), f.getFieldName());
                    if (decl == null || !dead.contains(decl.getClsName() + "." + f.getFieldName())) {
                        continue;
                    }
                    if (i >= 2 && isPurePush(ins.get(i - 2)) && isPurePush(ins.get(i - 1))) {
                        ins.remove(i);
                        ins.remove(i - 1);
                        ins.remove(i - 2);
                        i -= 3;
                    } else {
                        char d = f.getDesc().charAt(0);
                        ins.set(i, new BasicInstruction(d == 'J' || d == 'D' ? Opcodes.POP2 : Opcodes.POP, 0));
                        ins.add(i + 1, new BasicInstruction(Opcodes.POP, 0));
                        i++;
                    }
                    rewrittenStores++;
                    edited.add(m);
                }
            }
        }
        for (BytecodeMethod m : edited) {
            m.recomputeRawMethodPlans();
        }
        for (ByteCodeClass bc : classes) {
            List<ByteCodeField> fields = bc.getFields();
            for (int i = fields.size() - 1; i >= 0; i--) {
                ByteCodeField bf = fields.get(i);
                if (!bf.isStaticField() && dead.contains(bc.getClsName() + "." + bf.getFieldName())) {
                    fields.remove(i);
                    removedFields++;
                }
            }
        }
        if (ByteCodeTranslator.verbose) {
            System.out.println("Dead field elimination: removed " + removedFields
                    + " never-read instance fields, rewrote " + rewrittenStores + " stores");
        }
    }

    /** A single push with no side effect: a local load or a constant. */
    private static boolean isPurePush(Instruction x) {
        if (x instanceof VarOp) {
            int op = x.getOpcode();
            return op >= Opcodes.ILOAD && op <= Opcodes.ALOAD;
        }
        if (x instanceof Ldc) {
            return true;
        }
        if (x instanceof BasicInstruction) {
            int op = x.getOpcode();
            return (op >= Opcodes.ACONST_NULL && op <= Opcodes.DCONST_1)
                    || op == Opcodes.BIPUSH || op == Opcodes.SIPUSH;
        }
        return false;
    }

    /** The class that declares instance field {@code name}, searching up from {@code cls}. */
    private static ByteCodeClass declaring(Map<String, ByteCodeClass> byName, String cls, String name) {
        ByteCodeClass c = byName.get(cls);
        while (c != null) {
            for (ByteCodeField bf : c.getFields()) {
                if (!bf.isStaticField() && bf.getFieldName().equals(name)) {
                    return c;
                }
            }
            c = c.getBaseClassObject();
        }
        return null;
    }

    private static Set<String> tokenize(String[] sources) {
        Set<String> out = new HashSet<String>();
        if (sources == null) {
            return out;
        }
        for (String s : sources) {
            if (s == null) {
                continue;
            }
            int n = s.length();
            int i = 0;
            while (i < n) {
                char c = s.charAt(i);
                if (Character.isJavaIdentifierStart(c)) {
                    int j = i + 1;
                    while (j < n && Character.isJavaIdentifierPart(s.charAt(j))) {
                        j++;
                    }
                    out.add(s.substring(i, j));
                    i = j;
                } else {
                    i++;
                }
            }
        }
        return out;
    }
}
