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
 * Classifies invokedynamic string concatenation (JDK 9+ makeConcat /
 * makeConcatWithConstants) by ARGUMENT TYPE, because ParparVM's existing
 * cn1ConcatN fast path in Parser.visitInvokeDynamicInsn only fires when EVERY
 * argument is already String-typed and the part count is 2..5. Everything else
 * falls back to a pre-sized StringBuilder: 4 allocations plus a byte->char
 * decode per append and a char->byte re-encode in toString.
 *
 * So the question this answers is: of the indy concat sites new (JDK 17) code
 * actually emits, how many reach the fast path, and what types are the ones
 * that miss it?
 */
public final class IndyScan {
    static long indySites, allString, mixed, tooManyParts;
    static final Map<String,Integer> argKinds = new TreeMap<String,Integer>();
    static final Map<String,Integer> missKinds = new TreeMap<String,Integer>();
    static final Map<String,Integer> partCounts = new TreeMap<String,Integer>();

    public static void main(String[] a) throws Exception {
        for (String r : a) scan(new File(r));
        System.out.println("indy concat sites     : " + indySites);
        System.out.println("  all-String args     : " + allString);
        System.out.println("  has non-String arg  : " + mixed);
        System.out.println("arg type histogram    : " + argKinds);
        System.out.println("types causing a miss  : " + missKinds);
        System.out.println("arg-count histogram   : " + partCounts);
    }

    static void scan(File f) throws Exception {
        if (f.isDirectory()) { File[] k=f.listFiles(); if(k!=null) for(File c:k) scan(c); return; }
        if (!f.getName().endsWith(".class")) return;
        ClassReader cr; InputStream in=new FileInputStream(f);
        try { cr=new ClassReader(in); } finally { in.close(); }
        ClassNode cn=new ClassNode(); cr.accept(cn, ClassReader.SKIP_FRAMES);
        for (Object mo : cn.methods) {
            MethodNode m=(MethodNode)mo;
            for (AbstractInsnNode p=m.instructions.getFirst(); p!=null; p=p.getNext()) {
                if (!(p instanceof InvokeDynamicInsnNode)) continue;
                InvokeDynamicInsnNode idn=(InvokeDynamicInsnNode)p;
                String bsm=idn.bsm==null?"":idn.bsm.getName();
                if (!"makeConcat".equals(bsm) && !"makeConcatWithConstants".equals(bsm)) continue;
                indySites++;
                Type[] ats=Type.getMethodType(idn.desc).getArgumentTypes();
                boolean all=ats.length>0;
                String pk=ats.length>=6?"6+":String.valueOf(ats.length);
                bump(partCounts,pk);
                for (Type t:ats) {
                    String kind=kindOf(t);
                    bump(argKinds,kind);
                    if (!"String".equals(kind)) { all=false; bump(missKinds,kind); }
                }
                if (all) allString++; else mixed++;
            }
        }
    }
    static String kindOf(Type t) {
        if (t.getSort()==Type.OBJECT) return "java/lang/String".equals(t.getInternalName())?"String":"Object:"+t.getInternalName();
        if (t.getSort()==Type.ARRAY) return "array";
        return t.getClassName();
    }
    static void bump(Map<String,Integer> m,String k){Integer c=m.get(k);m.put(k,c==null?1:c+1);}
}
