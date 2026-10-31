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

import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.Invoke;
import com.codename1.tools.translator.bytecodes.LineNumber;
import com.codename1.tools.translator.bytecodes.MultiArray;
import com.codename1.tools.translator.bytecodes.TypeInstruction;
import com.codename1.tools.translator.classfile.Opcodes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Proves that the steady state of a real-time task never allocates.
///
/// A `com.codename1.realtime.RealtimeTask` runs `cycle()` and `periodsMissed(int)`
/// on a thread that must not wait for the garbage collector, so neither may allocate
/// on the heap. The roots are those two methods in every subclass of the task class.
/// From each root the pass follows only calls whose target is certain: static and
/// special calls, and virtual or interface calls when the whole program holds exactly
/// one non-abstract method of that name and descriptor, so dispatch can only land
/// there. A call it cannot resolve is not followed, which keeps the check free of
/// false positives at the price of missing an allocation behind a polymorphic call.
///
/// In every method reached it reports `NEW`, the array allocations and the boxing
/// `valueOf` methods. Not reported: a `NEW` of a `Throwable`, because a throw ends the
/// steady state and the runtime handles it, and `<clinit>`. A method annotated
/// `@com.codename1.realtime.AllocationAllowed` is neither walked nor reported at its
/// call sites. Runs on the raw bytecode, before any pass rewrites an instruction.
/// With no subclass of the task class in the program it does nothing.
final class RealtimeAllocationVerifier {
    private static final String TASK_CLASS = "com_codename1_realtime_RealtimeTask";
    private static final String THROWABLE = "java_lang_Throwable";
    private static final String[] BOXES = {"java/lang/Integer", "java/lang/Long", "java/lang/Short",
        "java/lang/Byte", "java/lang/Character", "java/lang/Boolean", "java/lang/Float",
        "java/lang/Double"};

    private RealtimeAllocationVerifier() {
    }

    /// Checks the program.
    ///
    /// @param classes every parsed class
    /// @throws NativeSignatureVerifier.VerificationFailedException listing every allocation found
    static void run(List<ByteCodeClass> classes) throws NativeSignatureVerifier.VerificationFailedException {
        List<BytecodeMethod> roots = new ArrayList<BytecodeMethod>();
        for (ByteCodeClass cls : classes) {
            if (!isTaskSubclass(cls)) {
                continue;
            }
            for (BytecodeMethod m : cls.getMethods()) {
                if (!m.isAbstract() && !m.isNative() && !m.isStatic()
                        && (("cycle".equals(m.getMethodName()) && "()V".equals(m.getDesc()))
                        || ("periodsMissed".equals(m.getMethodName()) && "(I)V".equals(m.getDesc())))) {
                    roots.add(m);
                }
            }
        }
        if (roots.isEmpty()) {
            return;
        }
        Map<String, List<BytecodeMethod>> concrete = new HashMap<String, List<BytecodeMethod>>();
        for (ByteCodeClass cls : classes) {
            for (BytecodeMethod m : cls.getMethods()) {
                if (!m.isAbstract()) {
                    String key = m.getMethodName() + m.getDesc();
                    List<BytecodeMethod> l = concrete.get(key);
                    if (l == null) {
                        l = new ArrayList<BytecodeMethod>(1);
                        concrete.put(key, l);
                    }
                    l.add(m);
                }
            }
        }
        Map<BytecodeMethod, BytecodeMethod> parent = new HashMap<BytecodeMethod, BytecodeMethod>();
        List<BytecodeMethod> queue = new ArrayList<BytecodeMethod>();
        for (BytecodeMethod r : roots) {
            if (!r.isAllocationAllowed()) {
                parent.put(r, null);
                queue.add(r);
            }
        }
        List<String> findings = new ArrayList<String>();
        for (int q = 0; q < queue.size(); q++) {
            BytecodeMethod m = queue.get(q);
            List<Instruction> ins = m.getInstructions();
            if (ins == null || "__CLINIT__".equals(m.getMethodName())) {
                continue;
            }
            int line = -1;
            for (Instruction i : ins) {
                if (i instanceof LineNumber) {
                    line = ((LineNumber) i).getLine();
                    continue;
                }
                String what = allocation(i);
                if (what != null) {
                    findings.add(describe(m, line, what, parent));
                    continue;
                }
                if (i instanceof Invoke) {
                    BytecodeMethod target = resolve((Invoke) i, concrete);
                    if (target != null && !target.isAllocationAllowed() && !parent.containsKey(target)) {
                        parent.put(target, m);
                        queue.add(target);
                    }
                }
            }
        }
        if (!findings.isEmpty()) {
            StringBuilder b = new StringBuilder();
            b.append("Real-time tasks must not allocate in cycle() or periodsMissed(int); ")
                    .append(findings.size()).append(" allocation(s) found. Remove them, or annotate the ")
                    .append("method with @com.codename1.realtime.AllocationAllowed:");
            for (String f : findings) {
                b.append("\n  ").append(f);
            }
            throw new NativeSignatureVerifier.VerificationFailedException(b.toString());
        }
    }

    private static boolean isTaskSubclass(ByteCodeClass cls) {
        Set<String> seen = new HashSet<String>();
        ByteCodeClass c = base(cls);
        while (c != null && seen.add(c.getClsName())) {
            if (TASK_CLASS.equals(c.getClsName())) {
                return true;
            }
            c = base(c);
        }
        return false;
    }

    private static ByteCodeClass base(ByteCodeClass c) {
        String b = c.getBaseClass();
        return b == null ? null : Parser.getClassObject(Util.mangle(b));
    }

    private static boolean isThrowable(String type) {
        Set<String> seen = new HashSet<String>();
        ByteCodeClass c = Parser.getClassObject(Util.mangle(type));
        while (c != null && seen.add(c.getClsName())) {
            if (THROWABLE.equals(c.getClsName()) || THROWABLE.equals(Util.mangle(c.getBaseClass()))) {
                return true;
            }
            c = base(c);
        }
        return false;
    }

    /// Describes the allocation an instruction makes, or null.
    private static String allocation(Instruction i) {
        int op = i.getOpcode();
        if (i instanceof TypeInstruction) {
            String type = ((TypeInstruction) i).getTypeName();
            if (op == Opcodes.NEW) {
                return isThrowable(type) ? null : "new " + type.replace('/', '.');
            }
            if (op == Opcodes.ANEWARRAY) {
                return "new " + type.replace('/', '.') + "[]";
            }
            return null;
        }
        if (i instanceof MultiArray) {
            return "multi-dimensional array " + ((MultiArray) i).getDesc();
        }
        if (op == Opcodes.NEWARRAY) {
            return "new primitive array";
        }
        if (i instanceof Invoke && op == Opcodes.INVOKESTATIC) {
            Invoke inv = (Invoke) i;
            if ("valueOf".equals(inv.getName()) && !inv.getDesc().startsWith("(Ljava/lang/String;")) {
                for (String box : BOXES) {
                    if (box.equals(inv.getOwner())) {
                        return "boxing " + box.substring(10) + ".valueOf";
                    }
                }
            }
        }
        return null;
    }

    /// The one method a call can land on, or null when that is not certain.
    private static BytecodeMethod resolve(Invoke inv, Map<String, List<BytecodeMethod>> concrete) {
        int op = inv.getOpcode();
        String name = "<init>".equals(inv.getName()) ? "__INIT__" : inv.getName();
        if (op == Opcodes.INVOKESTATIC || op == Opcodes.INVOKESPECIAL) {
            Set<String> seen = new HashSet<String>();
            ByteCodeClass c = Parser.getClassObject(Util.mangle(inv.getOwner()));
            while (c != null && seen.add(c.getClsName())) {
                for (BytecodeMethod m : c.getMethods()) {
                    if (name.equals(m.getMethodName()) && inv.getDesc().equals(m.getDesc())) {
                        return m.isAbstract() ? null : m;
                    }
                }
                c = base(c);
            }
            return null;
        }
        if (op == Opcodes.INVOKEVIRTUAL || op == Opcodes.INVOKEINTERFACE) {
            List<BytecodeMethod> l = concrete.get(name + inv.getDesc());
            return l != null && l.size() == 1 ? l.get(0) : null;
        }
        return null;
    }

    private static String describe(BytecodeMethod m, int line, String what,
            Map<BytecodeMethod, BytecodeMethod> parent) {
        StringBuilder b = new StringBuilder();
        b.append(what).append(" in ").append(label(m));
        if (m.getSourceFile() != null) {
            b.append(" (").append(m.getSourceFile());
            if (line >= 0) {
                b.append(':').append(line);
            }
            b.append(')');
        }
        List<String> chain = new ArrayList<String>();
        for (BytecodeMethod p = parent.get(m); p != null; p = parent.get(p)) {
            chain.add(label(p));
        }
        if (!chain.isEmpty()) {
            b.append(", called from ");
            for (int k = 0; k < chain.size(); k++) {
                if (k > 0) {
                    b.append(" <- ");
                }
                b.append(chain.get(k));
            }
        }
        return b.toString();
    }

    private static String label(BytecodeMethod m) {
        String n = "__INIT__".equals(m.getMethodName()) ? "<init>" : m.getMethodName();
        return m.getClsName().replace('_', '.') + "." + n + m.getDesc();
    }
}
