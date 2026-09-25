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
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.*;

/**
 * Detects the javac string-concatenation idiom in bytecode and reports which
 * occurrences are FUSIBLE -- i.e. the StringBuilder provably never escapes, so a
 * translator pass could lower the whole chain to one exact-sized allocation.
 *
 * The idiom javac emits for a + b is:
 *     NEW java/lang/StringBuilder
 *     DUP
 *     INVOKESPECIAL <init>()V            (or (I)V / (Ljava/lang/String;)V)
 *     INVOKEVIRTUAL append(...)          x N
 *     INVOKEVIRTUAL toString()Ljava/lang/String;
 *
 * A site is counted NON-fusible when anything between the NEW and the toString
 * consumes the builder reference other than an append on it or the terminating
 * toString: a store to a local/field/array, use as a call argument, ARETURN, or
 * a branch crossing the chain. Those are the cases where a naive fusion would
 * change behaviour, so they are reported separately rather than assumed away.
 */
public final class ConcatScan {
    static long sites, fusible, appends, nonFusEscape, nonFusBranch;
    static final Map<String,Integer> byAppendCount = new TreeMap<String,Integer>();
    static final Map<String,Integer> reasons = new TreeMap<String,Integer>();
    static final Map<String,Integer> appendTypes = new TreeMap<String,Integer>();
    static long allStringChains, mixedChains;

    public static void main(String[] a) throws Exception {
        for (String root : a) scan(new File(root));
        System.out.println("concat sites          : " + sites);
        System.out.println("  fusible (no escape) : " + fusible);
        System.out.println("  escapes             : " + nonFusEscape);
        System.out.println("  branch in chain     : " + nonFusBranch);
        System.out.println("appends in chains     : " + appends);
        System.out.println("fusible by chain len  : " + byAppendCount);
        System.out.println("  all-String chains   : " + allStringChains);
        System.out.println("  mixed-type chains   : " + mixedChains);
        System.out.println("append arg types      : " + appendTypes);
        System.out.println("non-fusible reasons   : " + reasons);
    }

    static void scan(File f) throws Exception {
        if (f.isDirectory()) { File[] k = f.listFiles(); if (k != null) for (File c : k) scan(c); return; }
        if (!f.getName().endsWith(".class")) return;
        ClassReader cr; InputStream in = new FileInputStream(f);
        try { cr = new ClassReader(in); } finally { in.close(); }
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_FRAMES);
        for (Object mo : cn.methods) analyse((MethodNode) mo);
    }

    static void analyse(MethodNode m) {
        InsnList il = m.instructions;
        for (AbstractInsnNode p = il.getFirst(); p != null; p = p.getNext()) {
            if (p.getOpcode() != Opcodes.NEW) continue;
            if (!"java/lang/StringBuilder".equals(((TypeInsnNode) p).desc)) continue;
            sites++;
            int n = 0; boolean ok = true, sawToString = false; String why = null; boolean allStr = true; List<String> kinds = new ArrayList<String>();
            for (AbstractInsnNode q = p.getNext(); q != null; q = q.getNext()) {
                int op = q.getOpcode();
                if (op == Opcodes.INVOKEVIRTUAL || op == Opcodes.INVOKESPECIAL) {
                    MethodInsnNode mi = (MethodInsnNode) q;
                    if ("java/lang/StringBuilder".equals(mi.owner)) {
                        if ("toString".equals(mi.name)) { sawToString = true; break; }
                        if ("append".equals(mi.name)) { n++; String k = mi.desc.substring(1, mi.desc.indexOf(')')); kinds.add(k); if (!"Ljava/lang/String;".equals(k)) allStr = false; continue; }
                        if ("<init>".equals(mi.name)) continue;
                        ok = false; why = "sb." + mi.name; break;
                    }
                    continue;
                }
                if (op == Opcodes.ASTORE || op == Opcodes.PUTFIELD || op == Opcodes.PUTSTATIC
                        || op == Opcodes.AASTORE || op == Opcodes.ARETURN) { ok = false; why = "store/return"; break; }
                if (q instanceof JumpInsnNode || q instanceof LabelNode && n > 0 && false) { }
                if (q instanceof JumpInsnNode) { ok = false; why = "branch"; break; }
            }
            if (!sawToString) { ok = false; if (why == null) why = "no toString"; }
            if (ok) {
                fusible++; appends += n;
                for (String k : kinds) { Integer kc = appendTypes.get(k); appendTypes.put(k, kc == null ? 1 : kc + 1); }
                if (allStr) allStringChains++; else mixedChains++;
                String k = n >= 5 ? "5+" : String.valueOf(n);
                Integer c = byAppendCount.get(k); byAppendCount.put(k, c == null ? 1 : c + 1);
            } else {
                if ("branch".equals(why)) nonFusBranch++; else nonFusEscape++;
                Integer c = reasons.get(why); reasons.put(why, c == null ? 1 : c + 1);
            }
        }
    }
}
