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
 * Sizes the "String.toCharArray() that never needed an array" opportunity.
 *
 * toCharArray() is 22.8% of all char[] allocations on the self-hosting corpus
 * (293,175 of 1,285,250), and a large share of those arrays are built only to be
 * SCANNED and dropped -- Parser.encodeStringSlashU is the worst offender in the
 * translator itself: it materialises the array, reads it in a loop, and in the
 * common case returns the original String and throws the copy away.
 *
 * RETURNING THE BACKING ARRAY UNCOPIED IS NOT THE ANSWER. That would make a
 * String mutable through its own accessor -- com.codename1.io.Util.toCharArray
 * exists precisely because some JVMs did that, calls it "a serious security hole
 * in the JVM", and DETECTS it at runtime with `s.toCharArray() == s.toCharArray()`.
 * It would also change that expression's result.
 *
 * The safe transformation is to not build the array: when the result is stored to
 * a local that is only ever read with CALOAD or ARRAYLENGTH and never escapes,
 * every `a[i]` is `s.charAt(i)` and every `a.length` is `s.length()`. No array
 * exists, so nothing can alias or mutate one, and on a COMPACT string charAt is a
 * byte load plus a mask.
 *
 * A site is counted ELIDABLE only if the local's every use is CALOAD or
 * ARRAYLENGTH. Anything else -- CASTORE (mutation), ARETURN (escape), being
 * passed as an argument, stored to a field or array, or a second ASTORE to the
 * same slot -- is reported in its own bucket rather than assumed away.
 */
public final class ToCharArrayScan {
    static long sites, elidable, escapes, mutates, notStored, reStored;
    static final Map<String,Integer> byReason = new TreeMap<String,Integer>();
    static final Map<String,Integer> elidableByClass = new TreeMap<String,Integer>();

    public static void main(String[] a) throws Exception {
        for (String r : a) scan(new File(r));
        System.out.println("String.toCharArray() call sites : " + sites);
        System.out.println("  ELIDABLE (read-only, no escape): " + elidable);
        System.out.println("  result escapes (arg/return/field/array): " + escapes);
        System.out.println("  array is MUTATED (CASTORE)      : " + mutates);
        System.out.println("  result not stored to a local    : " + notStored);
        System.out.println("  slot re-stored (ambiguous)      : " + reStored);
        System.out.println("reasons: " + byReason);
        System.out.println("elidable sites by class (top 15):");
        List<Map.Entry<String,Integer>> es = new ArrayList<Map.Entry<String,Integer>>(elidableByClass.entrySet());
        Collections.sort(es, new Comparator<Map.Entry<String,Integer>>() {
            public int compare(Map.Entry<String,Integer> x, Map.Entry<String,Integer> y) { return y.getValue()-x.getValue(); }});
        for (int i = 0; i < es.size() && i < 15; i++) {
            System.out.printf("  %4d  %s%n", es.get(i).getValue(), es.get(i).getKey());
        }
    }

    static void bump(Map<String,Integer> m, String k) {
        Integer v = m.get(k); m.put(k, v == null ? 1 : v + 1);
    }

    static void scan(File f) throws Exception {
        if (f.isDirectory()) { File[] k=f.listFiles(); if(k!=null) for(File c:k) scan(c); return; }
        if (!f.getName().endsWith(".class")) return;
        ClassReader cr; InputStream in=new FileInputStream(f);
        try { cr=new ClassReader(in); } finally { in.close(); }
        ClassNode cn=new ClassNode(); cr.accept(cn, ClassReader.SKIP_FRAMES);
        for (Object mo : cn.methods) {
            MethodNode mn = (MethodNode) mo;
            if (mn.instructions == null) continue;
            AbstractInsnNode[] ins = mn.instructions.toArray();
            for (int i = 0; i < ins.length; i++) {
                if (!(ins[i] instanceof MethodInsnNode)) continue;
                MethodInsnNode mi = (MethodInsnNode) ins[i];
                if (mi.getOpcode() != Opcodes.INVOKEVIRTUAL
                        || !"java/lang/String".equals(mi.owner)
                        || !"toCharArray".equals(mi.name)) continue;
                sites++;
                int j = nextReal(ins, i + 1);
                if (j < 0 || ins[j].getOpcode() != Opcodes.ASTORE) {
                    notStored++; bump(byReason, "not-stored"); continue;
                }
                int slot = ((VarInsnNode) ins[j]).var;
                classify(cn.name, ins, j, slot);
            }
        }
    }

    static int nextReal(AbstractInsnNode[] ins, int from) {
        for (int i = from; i < ins.length; i++) {
            int op = ins[i].getOpcode();
            if (op >= 0) return i;
        }
        return -1;
    }

    /** Walk every later instruction; any use of the slot other than a read fails the site. */
    static void classify(String cls, AbstractInsnNode[] ins, int storeIdx, int slot) {
        boolean ok = true; String why = null;
        for (int i = storeIdx + 1; i < ins.length && ok; i++) {
            AbstractInsnNode n = ins[i];
            int op = n.getOpcode();
            if (n instanceof VarInsnNode && ((VarInsnNode) n).var == slot) {
                if (op == Opcodes.ASTORE) { ok = false; why = "slot-restored"; reStored++; break; }
                if (op != Opcodes.ALOAD)  { ok = false; why = "slot-other"; break; }
                // ALOAD of the array: the very next real instruction decides.
                int k = nextReal(ins, i + 1);
                if (k < 0) { ok = false; why = "aload-dangling"; break; }
                int nop = ins[k].getOpcode();
                if (nop == Opcodes.ARRAYLENGTH) continue;          // a.length -> s.length()
                // a[i] : the index is pushed between the ALOAD and the CALOAD, so scan
                // forward for the first array op on this reference instead of assuming
                // adjacency.
                int m = firstArrayOp(ins, k);
                if (m >= 0 && ins[m].getOpcode() == Opcodes.CALOAD) continue;  // a[i] -> s.charAt(i)
                if (m >= 0 && ins[m].getOpcode() == Opcodes.CASTORE) { ok = false; why = "mutated"; mutates++; break; }
                ok = false; why = "escapes"; escapes++; break;
            }
        }
        if (ok) { elidable++; bump(elidableByClass, cls.replace('/', '.')); }
        else if (why != null) bump(byReason, why);
    }

    static int firstArrayOp(AbstractInsnNode[] ins, int from) {
        for (int i = from; i < ins.length && i < from + 12; i++) {
            int op = ins[i].getOpcode();
            if (op == Opcodes.CALOAD || op == Opcodes.CASTORE || op == Opcodes.ARRAYLENGTH) return i;
            if (op == Opcodes.INVOKEVIRTUAL || op == Opcodes.INVOKESTATIC
                    || op == Opcodes.INVOKEINTERFACE || op == Opcodes.INVOKESPECIAL
                    || op == Opcodes.ARETURN || op == Opcodes.PUTFIELD
                    || op == Opcodes.PUTSTATIC || op == Opcodes.AASTORE) return -1;
        }
        return -1;
    }
}
