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
package com.codename1.tools.javac;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes a method's StackMapTable and max stack by abstract interpretation of
 * the finished bytecode, the way a class-file library's frame computation does:
 * types flow forward from the method's descriptor, and reference types meeting
 * at a join are merged through the class hierarchy the compiler already knows.
 *
 * <p>Verification types are strings: {@code T} top, {@code I} int, {@code F},
 * {@code J} long, {@code D} double, {@code N} null, {@code U} uninitialized this,
 * {@code u<offset>} the result of a {@code new} at that offset; reference types
 * are kept in descriptor form ({@code Ljava/lang/String;}, {@code [I}), which can
 * never be mistaken for one of those tags -- a class may well be named {@code F}.
 */
final class FrameComputer {
    /** Answers the superclass and interface-ness of a class by internal name. */
    interface Hierarchy {
        /** The superclass's internal name, or null for Object or an unknown class. */
        String superclass(String internalName);

        boolean isInterface(String internalName);
    }

    private static final String TOP = "T";
    private static final String INT = "I";
    private static final String FLOAT = "F";
    private static final String LONG = "J";
    private static final String DOUBLE = "D";
    private static final String NULL = "N";
    private static final String UNINIT_THIS = "U";

    private final ByteBuf code;
    private final ConstPool pool;
    private final Hierarchy hierarchy;
    private final String owner;
    private final int maxLocals;
    private final List<int[]> handlers;
    private String[][] inLocals;
    private List<String>[] inStacks;
    private final Map<Integer, String> newTypes = new HashMap<Integer, String>();
    int maxStack;

    FrameComputer(ByteBuf code, ConstPool pool, Hierarchy hierarchy, String owner, int maxLocals, List<int[]> handlers) {
        this.code = code;
        this.pool = pool;
        this.hierarchy = hierarchy;
        this.owner = owner;
        this.maxLocals = maxLocals;
        this.handlers = handlers;
    }

    // ------------------------------------------------------------------ descriptors

    static String typeOfDescriptor(String desc) {
        switch (desc.charAt(0)) {
            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I':
                return INT;
            case 'F':
                return FLOAT;
            case 'J':
                return LONG;
            case 'D':
                return DOUBLE;
            default:
                return desc;
        }
    }

    static List<String> argDescriptors(String methodDesc) {
        List<String> out = new ArrayList<String>();
        int i = 1;
        while (methodDesc.charAt(i) != ')') {
            int start = i;
            while (methodDesc.charAt(i) == '[') {
                i++;
            }
            if (methodDesc.charAt(i) == 'L') {
                i = methodDesc.indexOf(';', i);
            }
            i++;
            out.add(methodDesc.substring(start, i));
        }
        return out;
    }

    static String returnDescriptor(String methodDesc) {
        return methodDesc.substring(methodDesc.indexOf(')') + 1);
    }

    /** A constant-pool class name (internal name or array descriptor) in descriptor form. */
    static String refType(String classOrArray) {
        return classOrArray.charAt(0) == '[' ? classOrArray : "L" + classOrArray + ";";
    }

    /** The internal name (or array descriptor) of a reference type in descriptor form. */
    private static String className(String refType) {
        return refType.charAt(0) == 'L' ? refType.substring(1, refType.length() - 1) : refType;
    }

    private static boolean isCat2(String t) {
        return LONG.equals(t) || DOUBLE.equals(t);
    }

    private static boolean isRef(String t) {
        return !(t.equals(TOP) || t.equals(INT) || t.equals(FLOAT) || t.equals(LONG) || t.equals(DOUBLE));
    }

    private static boolean isUninit(String t) {
        return t.equals(UNINIT_THIS) || t.charAt(0) == 'u' && t.length() > 1 && Character.isDigit(t.charAt(1));
    }

    // ------------------------------------------------------------------ merging

    private String merge(String a, String b) {
        if (a.equals(b)) {
            return a;
        }
        if (!isRef(a) || !isRef(b) || isUninit(a) || isUninit(b)) {
            return TOP;
        }
        if (a.equals(NULL)) {
            return b;
        }
        if (b.equals(NULL)) {
            return a;
        }
        return refType(commonSuper(className(a), className(b)));
    }

    private String commonSuper(String a, String b) {
        if (a.charAt(0) == '[' || b.charAt(0) == '[') {
            if (a.charAt(0) == '[' && b.charAt(0) == '[') {
                String ea = a.substring(1);
                String eb = b.substring(1);
                boolean refA = ea.charAt(0) == 'L' || ea.charAt(0) == '[';
                boolean refB = eb.charAt(0) == 'L' || eb.charAt(0) == '[';
                if (refA && refB) {
                    String ca = ea.charAt(0) == 'L' ? ea.substring(1, ea.length() - 1) : ea;
                    String cb = eb.charAt(0) == 'L' ? eb.substring(1, eb.length() - 1) : eb;
                    String c = commonSuper(ca, cb);
                    return "[" + (c.charAt(0) == '[' ? c : "L" + c + ";");
                }
            }
            return "java/lang/Object";
        }
        if (hierarchy.isInterface(a) || hierarchy.isInterface(b)) {
            return "java/lang/Object";
        }
        List<String> chain = new ArrayList<String>();
        for (String c = a; c != null; c = hierarchy.superclass(c)) {
            chain.add(c);
        }
        for (String c = b; c != null; c = hierarchy.superclass(c)) {
            if (chain.contains(c)) {
                return c;
            }
        }
        return "java/lang/Object";
    }

    /** Merges a frame into the one recorded at pc; true when that changed it (or it was new). */
    private boolean mergeInto(int pc, String[] locals, List<String> stack) {
        if (inLocals[pc] == null) {
            inLocals[pc] = locals.clone();
            inStacks[pc] = new ArrayList<String>(stack);
            return true;
        }
        boolean changed = false;
        String[] cur = inLocals[pc];
        for (int i = 0; i < cur.length; i++) {
            String m = merge(cur[i], locals[i]);
            if (!m.equals(cur[i])) {
                cur[i] = m;
                changed = true;
            }
        }
        List<String> cs = inStacks[pc];
        if (cs.size() != stack.size()) {
            throw new CompileError("inconsistent stack height at " + pc + " (" + cs + " vs " + stack + ")");
        }
        for (int i = 0; i < cs.size(); i++) {
            String m = merge(cs.get(i), stack.get(i));
            if (!m.equals(cs.get(i))) {
                cs.set(i, m);
                changed = true;
            }
        }
        return changed;
    }

    // ------------------------------------------------------------------ analysis

    /** Runs the analysis; returns the StackMapTable attribute body, or null when no frames are needed. */
    @SuppressWarnings("unchecked")
    ByteBuf compute(boolean isStatic, boolean isConstructor, String methodDesc) {
        int len = code.length;
        inLocals = new String[len + 1][];
        inStacks = new List[len + 1];
        boolean[] frameNeeded = new boolean[len + 1];
        String[] init = new String[maxLocals];
        for (int i = 0; i < maxLocals; i++) {
            init[i] = TOP;
        }
        int slot = 0;
        if (!isStatic) {
            init[slot++] = isConstructor && !owner.equals("java/lang/Object") ? UNINIT_THIS : refType(owner);
        }
        for (String a : argDescriptors(methodDesc)) {
            String t = typeOfDescriptor(a);
            init[slot++] = t;
            if (isCat2(t)) {
                slot++;
            }
        }
        for (int[] h : handlers) {
            frameNeeded[h[2]] = true;
        }
        List<Integer> work = new ArrayList<Integer>();
        mergeInto(0, init, new ArrayList<String>());
        work.add(Integer.valueOf(0));
        while (!work.isEmpty()) {
            int pc = work.remove(work.size() - 1).intValue();
            String[] locals = inLocals[pc].clone();
            List<String> stack = new ArrayList<String>(inStacks[pc]);
            // Handlers covering this instruction see its incoming locals.
            for (int[] h : handlers) {
                if (pc >= h[0] && pc < h[1]) {
                    List<String> hs = new ArrayList<String>();
                    hs.add(refType(h[3] == 0 ? "java/lang/Throwable" : (String) pool.entry(h[3])[1]));
                    if (mergeInto(h[2], locals, hs)) {
                        work.add(Integer.valueOf(h[2]));
                    }
                }
            }
            int[] succ = execute(pc, locals, stack);
            int depth = 0;
            for (String s : stack) {
                depth += isCat2(s) ? 2 : 1;
            }
            if (depth > maxStack) {
                maxStack = depth;
            }
            int next = succ[0];
            for (int i = 1; i < succ.length; i++) {
                int target = succ[i];
                if (target == -1) {
                    continue;
                }
                frameNeeded[target] = true;
                if (mergeInto(target, locals, stack)) {
                    work.add(Integer.valueOf(target));
                }
            }
            if (next >= 0 && next < len) {
                if (mergeInto(next, locals, stack)) {
                    work.add(Integer.valueOf(next));
                }
            } else if (next < 0 && -next - 1 < len) {
                // No fall-through: the next instruction starts a block of its own.
                frameNeeded[-next - 1] = true;
            }
        }
        // Unreachable blocks: nop ... athrow, with a frame that only has a Throwable on the stack.
        int pc = 0;
        while (pc < len) {
            int size = instructionLength(pc);
            if (inLocals[pc] == null) {
                int start = pc;
                int end = pc;
                while (end < len && inLocals[end] == null) {
                    end += instructionLength(end);
                }
                for (int i = start; i < end - 1; i++) {
                    code.data[i] = (byte) Code.NOP;
                }
                code.data[end - 1] = (byte) Code.ATHROW;
                inLocals[start] = new String[0];
                List<String> st = new ArrayList<String>();
                st.add("Ljava/lang/Throwable;");
                inStacks[start] = st;
                frameNeeded[start] = true;
                for (int i = start + 1; i < end; i++) {
                    frameNeeded[i] = false;
                }
                if (maxStack < 1) {
                    maxStack = 1;
                }
                removeDeadRanges(start, end);
                pc = end;
                continue;
            }
            pc += size;
        }
        ByteBuf out = new ByteBuf();
        int count = 0;
        int last = -1;
        for (int i = 0; i < len; i++) {
            if (!frameNeeded[i] || inLocals[i] == null || i == 0 && !hasJumpTo(0)) {
                continue;
            }
            int delta = last < 0 ? i : i - last - 1;
            last = i;
            count++;
            out.u1(255).u2(delta);
            String[] locals = inLocals[i];
            int n = locals.length;
            while (n > 0 && locals[n - 1].equals(TOP)) {
                n--;
            }
            List<String> encoded = new ArrayList<String>();
            for (int k = 0; k < n; k++) {
                encoded.add(locals[k]);
                if (isCat2(locals[k])) {
                    k++;
                }
            }
            out.u2(encoded.size());
            for (String t : encoded) {
                writeType(out, t);
            }
            List<String> stack = inStacks[i];
            out.u2(stack.size());
            for (String t : stack) {
                writeType(out, t);
            }
        }
        if (count == 0) {
            return null;
        }
        ByteBuf attr = new ByteBuf();
        attr.u2(count);
        attr.bytes(out);
        return attr;
    }

    private boolean hasJumpTo(int target) {
        for (int[] h : handlers) {
            if (h[2] == target) {
                return true;
            }
        }
        return jumpTargets().contains(Integer.valueOf(target));
    }

    private List<Integer> jumpTargetsCache;

    private List<Integer> jumpTargets() {
        if (jumpTargetsCache == null) {
            jumpTargetsCache = new ArrayList<Integer>();
            int pc = 0;
            while (pc < code.length) {
                int op = code.u1At(pc);
                if (op >= Code.IFEQ && op <= Code.IF_ACMPNE || op == Code.GOTO || op == Code.IFNULL || op == Code.IFNONNULL) {
                    jumpTargetsCache.add(Integer.valueOf(pc + code.s2At(pc + 1)));
                }
                pc += instructionLength(pc);
            }
        }
        return jumpTargetsCache;
    }

    private void removeDeadRanges(int start, int end) {
        List<int[]> out = new ArrayList<int[]>();
        for (int[] h : handlers) {
            if (h[1] <= start || h[0] >= end) {
                out.add(h);
                continue;
            }
            if (h[0] < start) {
                out.add(new int[]{h[0], start, h[2], h[3]});
            }
            if (h[1] > end) {
                out.add(new int[]{end, h[1], h[2], h[3]});
            }
        }
        handlers.clear();
        handlers.addAll(out);
    }

    private void writeType(ByteBuf out, String t) {
        if (t.equals(TOP)) {
            out.u1(0);
        } else if (t.equals(INT)) {
            out.u1(1);
        } else if (t.equals(FLOAT)) {
            out.u1(2);
        } else if (t.equals(DOUBLE)) {
            out.u1(3);
        } else if (t.equals(LONG)) {
            out.u1(4);
        } else if (t.equals(NULL)) {
            out.u1(5);
        } else if (t.equals(UNINIT_THIS)) {
            out.u1(6);
        } else if (isUninit(t)) {
            out.u1(8).u2(Integer.parseInt(t.substring(1)));
        } else {
            out.u1(7).u2(pool.cls(className(t)));
        }
    }

    // ------------------------------------------------------------------ instructions

    int instructionLength(int pc) {
        int op = code.u1At(pc);
        switch (op) {
            case Code.BIPUSH:
            case Code.LDC:
            case Code.NEWARRAY:
                return 2;
            case Code.SIPUSH:
            case Code.LDC_W:
            case Code.LDC2_W:
            case Code.GETSTATIC:
            case Code.PUTSTATIC:
            case Code.GETFIELD:
            case Code.PUTFIELD:
            case Code.INVOKEVIRTUAL:
            case Code.INVOKESPECIAL:
            case Code.INVOKESTATIC:
            case Code.NEW:
            case Code.ANEWARRAY:
            case Code.CHECKCAST:
            case Code.INSTANCEOF:
            case Code.IINC:
            case Code.GOTO:
            case Code.IFNULL:
            case Code.IFNONNULL:
                return 3;
            case Code.MULTIANEWARRAY:
                return 4;
            case Code.INVOKEINTERFACE:
            case Code.INVOKEDYNAMIC:
            case Code.GOTO_W:
                return 5;
            case Code.WIDE:
                return code.u1At(pc + 1) == Code.IINC ? 6 : 4;
            case Code.TABLESWITCH: {
                int p = pc + 1;
                while (p % 4 != 0) {
                    p++;
                }
                int lo = code.s4At(p + 4);
                int hi = code.s4At(p + 8);
                return p + 12 + (hi - lo + 1) * 4 - pc;
            }
            case Code.LOOKUPSWITCH: {
                int p = pc + 1;
                while (p % 4 != 0) {
                    p++;
                }
                int n = code.s4At(p + 4);
                return p + 8 + n * 8 - pc;
            }
            default:
                if (op >= Code.ILOAD && op <= Code.ALOAD || op >= Code.ISTORE && op <= Code.ASTORE) {
                    return 2;
                }
                if (op >= Code.IFEQ && op <= Code.IF_ACMPNE) {
                    return 3;
                }
                return 1;
        }
    }

    private static String pop(List<String> stack) {
        if (stack.isEmpty()) {
            throw new CompileError("stack underflow");
        }
        return stack.remove(stack.size() - 1);
    }

    private void store(String[] locals, int slot, String t) {
        if (slot > 0 && isCat2(locals[slot - 1])) {
            locals[slot - 1] = TOP;
        }
        locals[slot] = t;
        if (isCat2(t)) {
            locals[slot + 1] = TOP;
        }
    }

    private static String arrayElement(String arr) {
        if (arr.equals(NULL)) {
            return NULL;
        }
        String e = arr.substring(1);
        return typeOfDescriptor(e);
    }

    private String cpClass(int index) {
        return (String) pool.entry(index)[1];
    }

    /**
     * Applies the instruction at pc to the frame. Returns {fall-through pc (or -(next)-1 when
     * there is none), branch targets...}.
     */
    private int[] execute(int pc, String[] locals, List<String> stack) {
        int op = code.u1At(pc);
        int next = pc + instructionLength(pc);
        switch (op) {
            case Code.NOP:
                break;
            case Code.ACONST_NULL:
                stack.add(NULL);
                break;
            case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07: case 0x08:
            case Code.BIPUSH:
            case Code.SIPUSH:
                stack.add(INT);
                break;
            case 0x09: case 0x0a:
                stack.add(LONG);
                break;
            case 0x0b: case 0x0c: case 0x0d:
                stack.add(FLOAT);
                break;
            case 0x0e: case 0x0f:
                stack.add(DOUBLE);
                break;
            case Code.LDC:
            case Code.LDC_W:
            case Code.LDC2_W: {
                int idx = op == Code.LDC ? code.u1At(pc + 1) : code.u2At(pc + 1);
                Object[] e = pool.entry(idx);
                int tag = ((Integer) e[0]).intValue();
                switch (tag) {
                    case ConstPool.INTEGER: stack.add(INT); break;
                    case ConstPool.FLOAT: stack.add(FLOAT); break;
                    case ConstPool.LONG: stack.add(LONG); break;
                    case ConstPool.DOUBLE: stack.add(DOUBLE); break;
                    case ConstPool.STRING: stack.add("Ljava/lang/String;"); break;
                    case ConstPool.CLASS: stack.add("Ljava/lang/Class;"); break;
                    case ConstPool.METHOD_TYPE: stack.add("Ljava/lang/invoke/MethodType;"); break;
                    default: stack.add("Ljava/lang/invoke/MethodHandle;"); break;
                }
                break;
            }
            case Code.ILOAD: case Code.LLOAD: case Code.FLOAD: case Code.DLOAD: case Code.ALOAD:
                stack.add(op == Code.ALOAD ? locals[code.u1At(pc + 1)] : op == Code.ILOAD ? INT
                        : op == Code.LLOAD ? LONG : op == Code.FLOAD ? FLOAT : DOUBLE);
                break;
            case Code.ISTORE: case Code.LSTORE: case Code.FSTORE: case Code.DSTORE: case Code.ASTORE:
                store(locals, code.u1At(pc + 1), pop(stack));
                break;
            case Code.WIDE: {
                int wop = code.u1At(pc + 1);
                int slot = code.u2At(pc + 2);
                if (wop == Code.IINC) {
                    break;
                }
                if (wop >= Code.ILOAD && wop <= Code.ALOAD) {
                    stack.add(wop == Code.ALOAD ? locals[slot] : wop == Code.ILOAD ? INT
                            : wop == Code.LLOAD ? LONG : wop == Code.FLOAD ? FLOAT : DOUBLE);
                } else {
                    store(locals, slot, pop(stack));
                }
                break;
            }
            case Code.IALOAD: case Code.BALOAD: case Code.CALOAD: case Code.SALOAD:
                pop(stack);
                pop(stack);
                stack.add(INT);
                break;
            case Code.LALOAD:
                pop(stack);
                pop(stack);
                stack.add(LONG);
                break;
            case Code.FALOAD:
                pop(stack);
                pop(stack);
                stack.add(FLOAT);
                break;
            case Code.DALOAD:
                pop(stack);
                pop(stack);
                stack.add(DOUBLE);
                break;
            case Code.AALOAD: {
                pop(stack);
                String arr = pop(stack);
                stack.add(arrayElement(arr));
                break;
            }
            case Code.IASTORE: case Code.LASTORE: case Code.FASTORE: case Code.DASTORE: case Code.AASTORE:
            case Code.BASTORE: case Code.CASTORE: case Code.SASTORE:
                pop(stack);
                pop(stack);
                pop(stack);
                break;
            case Code.POP:
                pop(stack);
                break;
            case Code.POP2: {
                String a = pop(stack);
                if (!isCat2(a)) {
                    pop(stack);
                }
                break;
            }
            case Code.DUP: {
                String a = pop(stack);
                stack.add(a);
                stack.add(a);
                break;
            }
            case Code.DUP_X1: {
                String a = pop(stack);
                String b = pop(stack);
                stack.add(a);
                stack.add(b);
                stack.add(a);
                break;
            }
            case Code.DUP_X2: {
                String a = pop(stack);
                String b = pop(stack);
                if (isCat2(b)) {
                    stack.add(a);
                    stack.add(b);
                    stack.add(a);
                } else {
                    String c = pop(stack);
                    stack.add(a);
                    stack.add(c);
                    stack.add(b);
                    stack.add(a);
                }
                break;
            }
            case Code.DUP2: {
                String a = pop(stack);
                if (isCat2(a)) {
                    stack.add(a);
                    stack.add(a);
                } else {
                    String b = pop(stack);
                    stack.add(b);
                    stack.add(a);
                    stack.add(b);
                    stack.add(a);
                }
                break;
            }
            case Code.DUP2_X1: {
                String a = pop(stack);
                if (isCat2(a)) {
                    String b = pop(stack);
                    stack.add(a);
                    stack.add(b);
                    stack.add(a);
                } else {
                    String b = pop(stack);
                    String c = pop(stack);
                    stack.add(b);
                    stack.add(a);
                    stack.add(c);
                    stack.add(b);
                    stack.add(a);
                }
                break;
            }
            case Code.DUP2_X2: {
                String a = pop(stack);
                if (isCat2(a)) {
                    String b = pop(stack);
                    if (isCat2(b)) {
                        stack.add(a);
                        stack.add(b);
                        stack.add(a);
                    } else {
                        String c = pop(stack);
                        stack.add(a);
                        stack.add(c);
                        stack.add(b);
                        stack.add(a);
                    }
                } else {
                    String b = pop(stack);
                    String c = pop(stack);
                    if (isCat2(c)) {
                        stack.add(b);
                        stack.add(a);
                        stack.add(c);
                        stack.add(b);
                        stack.add(a);
                    } else {
                        String d = pop(stack);
                        stack.add(b);
                        stack.add(a);
                        stack.add(d);
                        stack.add(c);
                        stack.add(b);
                        stack.add(a);
                    }
                }
                break;
            }
            case Code.SWAP: {
                String a = pop(stack);
                String b = pop(stack);
                stack.add(a);
                stack.add(b);
                break;
            }
            case Code.IINC:
                break;
            case Code.LCMP: case Code.FCMPL: case Code.FCMPG: case Code.DCMPL: case Code.DCMPG:
                pop(stack);
                pop(stack);
                stack.add(INT);
                break;
            case Code.IFEQ: case Code.IFNE: case Code.IFLT: case Code.IFGE: case Code.IFGT: case Code.IFLE:
            case Code.IFNULL: case Code.IFNONNULL:
                pop(stack);
                return new int[]{next, pc + code.s2At(pc + 1)};
            case Code.IF_ICMPEQ: case Code.IF_ICMPNE: case Code.IF_ICMPLT: case Code.IF_ICMPGE: case Code.IF_ICMPGT:
            case Code.IF_ICMPLE: case Code.IF_ACMPEQ: case Code.IF_ACMPNE:
                pop(stack);
                pop(stack);
                return new int[]{next, pc + code.s2At(pc + 1)};
            case Code.GOTO:
                return new int[]{-next - 1, pc + code.s2At(pc + 1)};
            case Code.GOTO_W:
                return new int[]{-next - 1, pc + code.s4At(pc + 1)};
            case Code.TABLESWITCH: {
                pop(stack);
                int p = pc + 1;
                while (p % 4 != 0) {
                    p++;
                }
                int lo = code.s4At(p + 4);
                int hi = code.s4At(p + 8);
                int[] out = new int[2 + hi - lo + 1];
                out[0] = -next - 1;
                out[1] = pc + code.s4At(p);
                for (int i = 0; i <= hi - lo; i++) {
                    out[2 + i] = pc + code.s4At(p + 12 + i * 4);
                }
                return out;
            }
            case Code.LOOKUPSWITCH: {
                pop(stack);
                int p = pc + 1;
                while (p % 4 != 0) {
                    p++;
                }
                int n = code.s4At(p + 4);
                int[] out = new int[2 + n];
                out[0] = -next - 1;
                out[1] = pc + code.s4At(p);
                for (int i = 0; i < n; i++) {
                    out[2 + i] = pc + code.s4At(p + 12 + i * 8);
                }
                return out;
            }
            case Code.IRETURN: case Code.LRETURN: case Code.FRETURN: case Code.DRETURN: case Code.ARETURN:
                pop(stack);
                return new int[]{-next - 1};
            case Code.RETURN:
                return new int[]{-next - 1};
            case Code.ATHROW:
                pop(stack);
                return new int[]{-next - 1};
            case Code.GETSTATIC:
                stack.add(typeOfDescriptor((String) pool.entry(code.u2At(pc + 1))[3]));
                break;
            case Code.PUTSTATIC:
                pop(stack);
                break;
            case Code.GETFIELD:
                pop(stack);
                stack.add(typeOfDescriptor((String) pool.entry(code.u2At(pc + 1))[3]));
                break;
            case Code.PUTFIELD:
                pop(stack);
                pop(stack);
                break;
            case Code.INVOKEVIRTUAL: case Code.INVOKESPECIAL: case Code.INVOKESTATIC: case Code.INVOKEINTERFACE:
            case Code.INVOKEDYNAMIC: {
                Object[] e = pool.entry(code.u2At(pc + 1));
                String name = (String) e[2];
                String desc = (String) e[3];
                for (int i = argDescriptors(desc).size(); i > 0; i--) {
                    pop(stack);
                }
                if (op != Code.INVOKESTATIC && op != Code.INVOKEDYNAMIC) {
                    String recv = pop(stack);
                    if (op == Code.INVOKESPECIAL && "<init>".equals(name)) {
                        String initialized = refType(recv.equals(UNINIT_THIS) ? owner
                                : newTypes.get(Integer.valueOf(Integer.parseInt(recv.substring(1)))));
                        for (int i = 0; i < locals.length; i++) {
                            if (locals[i].equals(recv)) {
                                locals[i] = initialized;
                            }
                        }
                        for (int i = 0; i < stack.size(); i++) {
                            if (stack.get(i).equals(recv)) {
                                stack.set(i, initialized);
                            }
                        }
                    }
                }
                String ret = returnDescriptor(desc);
                if (!ret.equals("V")) {
                    stack.add(typeOfDescriptor(ret));
                }
                break;
            }
            case Code.NEW:
                newTypes.put(Integer.valueOf(pc), cpClass(code.u2At(pc + 1)));
                stack.add("u" + pc);
                break;
            case Code.NEWARRAY: {
                pop(stack);
                int t = code.u1At(pc + 1);
                String[] names = {null, null, null, null, "[Z", "[C", "[F", "[D", "[B", "[S", "[I", "[J"};
                stack.add(names[t]);
                break;
            }
            case Code.ANEWARRAY: {
                pop(stack);
                String c = cpClass(code.u2At(pc + 1));
                stack.add("[" + refType(c));
                break;
            }
            case Code.ARRAYLENGTH:
                pop(stack);
                stack.add(INT);
                break;
            case Code.CHECKCAST:
                pop(stack);
                stack.add(refType(cpClass(code.u2At(pc + 1))));
                break;
            case Code.INSTANCEOF:
                pop(stack);
                stack.add(INT);
                break;
            case Code.MONITORENTER:
            case Code.MONITOREXIT:
                pop(stack);
                break;
            case Code.MULTIANEWARRAY: {
                int dims = code.u1At(pc + 3);
                for (int i = 0; i < dims; i++) {
                    pop(stack);
                }
                stack.add(refType(cpClass(code.u2At(pc + 1))));
                break;
            }
            default:
                if (op >= 0x1a && op <= 0x2d) {
                    int k = op - 0x1a;
                    int kind = k / 4;
                    int slot = k % 4;
                    stack.add(kind == 4 ? locals[slot] : kind == 0 ? INT : kind == 1 ? LONG : kind == 2 ? FLOAT : DOUBLE);
                } else if (op >= 0x3b && op <= 0x4e) {
                    int k = op - 0x3b;
                    store(locals, k % 4, pop(stack));
                } else if (op >= Code.IADD && op <= 0x83) {
                    arithmetic(op, stack);
                } else if (op >= Code.I2L && op <= Code.I2S) {
                    pop(stack);
                    stack.add(conversionResult(op));
                } else {
                    throw new CompileError("unsupported opcode " + op);
                }
        }
        return new int[]{next};
    }

    private static String conversionResult(int op) {
        switch (op) {
            case Code.I2L: case Code.F2L: case Code.D2L: return LONG;
            case Code.I2F: case Code.L2F: case Code.D2F: return FLOAT;
            case Code.I2D: case Code.L2D: case Code.F2D: return DOUBLE;
            default: return INT;
        }
    }

    private static void arithmetic(int op, List<String> stack) {
        // 0x60-0x73: add/sub/mul/div/rem in I,L,F,D order; 0x74-0x77 neg; 0x78-0x7d shifts; 0x7e-0x83 and/or/xor.
        if (op >= 0x74 && op <= 0x77) {
            return; // negation keeps the type
        }
        String result;
        if (op >= 0x78 && op <= 0x7d) {
            pop(stack);
            result = (op - 0x78) % 2 == 0 ? INT : LONG;
            pop(stack);
            stack.add(result);
            return;
        }
        int kind = op >= 0x7e ? (op - 0x7e) % 2 : (op - 0x60) % 4;
        result = kind == 0 ? INT : kind == 1 ? LONG : kind == 2 ? FLOAT : DOUBLE;
        pop(stack);
        pop(stack);
        stack.add(result);
    }
}
