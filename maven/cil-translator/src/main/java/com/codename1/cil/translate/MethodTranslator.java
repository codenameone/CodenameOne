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
package com.codename1.cil.translate;

import com.codename1.cil.metadata.CilAssembly;
import com.codename1.cil.metadata.CilAssembly.FieldRef;
import com.codename1.cil.metadata.CilAssembly.MethodDef;
import com.codename1.cil.metadata.CilAssembly.MethodRef;
import com.codename1.cil.metadata.CilAssembly.MethodSig;
import com.codename1.cil.metadata.CilAssembly.TypeDef;
import com.codename1.cil.metadata.CilType;
import com.codename1.cil.metadata.MethodBody;
import com.codename1.cil.metadata.MethodBody.Clause;
import com.codename1.cil.metadata.MethodBody.Instr;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/// Lowers one CIL method body to JVM bytecode.
///
/// The body is walked once, in IL order, with the type of every evaluation
/// stack entry tracked alongside. One pass is enough because ECMA-335 fixes
/// the stack at an instruction that is only reached by a backward branch to
/// be empty, so the state at every instruction is known by the time it is
/// reached: from the instruction before it, or from a forward branch already
/// seen.
///
/// What the JVM has no word for is lowered as follows.
///
/// - **Structs** are objects. A store copies into the object already there
///   (`$assign`) and a value that something else can still see is copied
///   before it is handed on; see [Val#own].
/// - **Managed references** to a struct are the struct. To anything else they
///   are an array and an index, so a local whose address is taken lives in a
///   one-element array.
/// - **Exceptions**: all the catch clauses of one `try` share a JVM handler
///   that normalises the exception and tests each clause in turn. A `finally`
///   is a catch-all that records how it was entered and resumes from there.
/// - **Checks ParparVM does not make** -- a failed cast, an integer division
///   by zero -- are emitted explicitly.
final class MethodTranslator implements Opcodes {
    private static final int PLAIN = 0;
    private static final int PROMOTED = 1;
    private static final int STRUCT = 2;
    private static final int REF_STRUCT = 3;
    private static final int REF_PAIR = 4;

    private static final class Var {
        CilType type;
        int slot;
        int mode;
        boolean addressTaken;
        boolean stored;
    }

    private static final class Group {
        final List<Clause> catches = new ArrayList<Clause>();
        int tryStart;
        int tryEnd;
        int original;
        int wrapped;
        final Label dispatch = new Label();
    }

    private static final class Fin {
        Clause clause;
        int exception;
        int resume;
        final Label entry = new Label();
        final Label bodyLabel = new Label();
        final List<Label> continuations = new ArrayList<Label>();
    }

    private final Translator host;
    private final Names names;
    private final CilAssembly asm;
    private final TypeDef owner;
    private final MethodDef method;
    private final MethodVisitor mv;
    private final MethodBody body;
    private final boolean ownerIsStruct;
    private final boolean isConstructor;

    private final ArrayList<Val> stack = new ArrayList<Val>();
    private final Map<Integer, Label> labels = new HashMap<Integer, Label>();
    private final Map<Integer, List<Val>> states = new HashMap<Integer, List<Val>>();
    private Var[] args;
    private Var[] locals;
    private int nextSlot;
    private int offset;
    private boolean reachable;
    private CilType constrained;
    private int index;
    /// A struct constructor being emitted a second time as the static
    /// `$new(arguments..., target)`, which runs it on `target` and returns
    /// it. `new T(a, b)` evaluates its arguments before there is an object
    /// to call a constructor on; with the object last, the call is a plain
    /// expression, and ParparVM compiles a plain expression to one C
    /// expression where it would otherwise shuffle every argument through
    /// the operand stack in memory.
    private final boolean factory;

    /// Where the result goes, in a method that returns a struct.
    private int retSlot = -1;
    /// Set between a call that was handed this method's own result object and
    /// the `ret` right after it, which then has nothing left to copy.
    private boolean forwardedReturn;
    /// The scratch structs this method needs, in the order they were first
    /// asked for. Null on the first of the two passes over a method, which
    /// exists to find them out; the second allocates them all on entry.
    private final List<String> tempPlan;
    private final List<String> tempsFound = new ArrayList<String>();
    private final Map<String, Integer> tempSlots = new HashMap<String, Integer>();
    /// How many scratch structs of each class the statement being translated
    /// has used so far. Nothing is reused until the stack is empty again:
    /// that is the one point where no reference to any of them can be left.
    private Map<String, Integer> tempUse = new HashMap<String, Integer>();
    private final Map<Integer, Map<String, Integer>> tempStates = new HashMap<Integer, Map<String, Integer>>();

    private final List<Group> groups = new ArrayList<Group>();
    private final Map<Clause, Group> groupOf = new HashMap<Clause, Group>();
    private final Map<Clause, Label> catchEntry = new HashMap<Clause, Label>();
    private final Map<Clause, Fin> fins = new HashMap<Clause, Fin>();
    private final List<Runnable> trampolines = new ArrayList<Runnable>();

    MethodTranslator(Translator host, TypeDef owner, MethodDef method, MethodVisitor mv, List<String> tempPlan,
            boolean factory) {
        this.tempPlan = tempPlan;
        this.factory = factory;
        this.host = host;
        this.names = host.names;
        this.asm = owner.assembly;
        this.owner = owner;
        this.method = method;
        this.mv = mv;
        this.body = method.body();
        this.ownerIsStruct = names.isStruct(owner.asType());
        this.isConstructor = ".ctor".equals(method.name) && !ownerIsStruct;
    }

    private TranslationException fail(String message) {
        String source = asm.sourceLocation(method, offset);
        return new TranslationException(method + " at IL_" + Integer.toHexString(offset)
                + (source == null ? "" : " (" + source + ")") + ": " + message);
    }

    /// Where the instruction being translated is, for a report: the line of
    /// the C# source when the assembly came with its PDB, and the method
    /// either way.
    private String where() {
        String source = asm.sourceLocation(method, offset);
        return (source == null ? "" : source + ", in ") + method;
    }

    // ------------------------------------------------------------------ setup

    void translate() {
        try {
            mv.visitCode();
            setUpArguments();
            prescan();
            setUpClauses();
            emitEntry();
            reachable = true;
            for (index = 0; index < body.instructions.size(); index++) {
                Instr ins = body.instructions.get(index);
                offset = ins.offset;
                begin(ins);
                step(ins);
            }
            // By index: running a trampoline may queue another.
            for (int i = 0; i < trampolines.size(); i++) { // NOPMD ForLoopCanBeForeach
                trampolines.get(i).run();
            }
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        } catch (TranslationException e) {
            throw e;
        } catch (RuntimeException e) {
            TranslationException wrapped = fail(e.toString());
            wrapped.initCause(e);
            throw wrapped;
        }
    }

    private void setUpArguments() {
        MethodSig sig = method.sig;
        int count = sig.params.length + (sig.hasThis ? 1 : 0);
        args = new Var[count];
        int at = 0;
        Var self = null;
        if (sig.hasThis) {
            self = new Var();
            self.type = owner.asType();
            self.mode = ownerIsStruct ? REF_STRUCT : PLAIN;
            if (!factory) {
                self.slot = nextSlot++;
            }
            args[at++] = self;
        }
        for (CilType p : sig.params) {
            Var v = new Var();
            v.slot = nextSlot;
            if (p.kind == CilType.Kind.BYREF) {
                v.type = p.element;
                v.mode = names.isStruct(p.element) ? REF_STRUCT : REF_PAIR;
                nextSlot += v.mode == REF_PAIR ? 2 : 1;
            } else {
                v.type = p;
                v.mode = names.isStruct(p) ? STRUCT : PLAIN;
                nextSlot += slotSize(p);
            }
            args[at++] = v;
        }
        if (factory) {
            self.slot = nextSlot++;
        }
        if (names.returnsStruct(sig)) {
            retSlot = nextSlot++;
        }
        locals = new Var[body.locals.length];
        for (int i = 0; i < locals.length; i++) {
            Var v = new Var();
            CilType t = body.locals[i];
            if (t.kind == CilType.Kind.BYREF || t.kind == CilType.Kind.PTR) {
                throw fail("a local of type " + t + " (ref local or pointer) is not supported");
            }
            v.type = t;
            v.mode = names.isStruct(t) ? STRUCT : PLAIN;
            locals[i] = v;
        }
    }

    List<String> tempsFound() {
        return tempsFound;
    }

    /// The scratch struct a call or a `new` writes its result into.
    private int tempSlot(CilType type) {
        String c = names.classRef(type);
        Integer used = tempUse.get(c);
        int n = used == null ? 0 : used.intValue();
        tempUse.put(c, Integer.valueOf(n + 1));
        String key = c + "#" + n;
        Integer slot = tempSlots.get(key);
        if (slot == null) {
            if (tempPlan != null) {
                throw fail("the two passes over this method disagree about " + key);
            }
            slot = Integer.valueOf(newSlot(1));
            tempSlots.put(key, slot);
            tempsFound.add(key);
        }
        return slot.intValue();
    }

    /// True when the struct about to be produced is returned by the very
    /// next instruction, so it can be built in this method's own result
    /// object instead of in a scratch one and copied.
    private boolean returnsNext(CilType type) {
        if (retSlot < 0 || index + 1 >= body.instructions.size()) {
            return false;
        }
        Instr next = body.instructions.get(index + 1);
        return "ret".equals(next.op) && !states.containsKey(Integer.valueOf(next.offset))
                && names.classRef(type).equals(names.classRef(method.sig.returnType));
    }

    /// Stack: nothing. Pushes the object a struct result should be written to.
    private void loadResultTarget(CilType type) {
        if (returnsNext(type)) {
            mv.visitVarInsn(ALOAD, retSlot);
            forwardedReturn = true;
        } else {
            mv.visitVarInsn(ALOAD, tempSlot(type));
        }
    }

    private int slotSize(CilType type) {
        int k = names.kind(type);
        return k == Val.I8 || k == Val.R8 ? 2 : 1;
    }

    private int newSlot(int size) {
        int slot = nextSlot;
        nextSlot += size;
        return slot;
    }

    private void prescan() {
        for (Instr ins : body.instructions) {
            if ("ldloca".equals(ins.op)) {
                locals[ins.operand].addressTaken = true;
            } else if ("ldarga".equals(ins.op)) {
                args[ins.operand].addressTaken = true;
            } else if ("starg".equals(ins.op)) {
                args[ins.operand].stored = true;
            }
        }
    }

    private void setUpClauses() {
        for (Clause c : body.clauses) {
            if (c.flags == Clause.CATCH) {
                Group g = null;
                for (Group candidate : groups) {
                    if (candidate.tryStart == c.tryStart && candidate.tryEnd == c.tryEnd) {
                        g = candidate;
                    }
                }
                if (g == null) {
                    g = new Group();
                    g.tryStart = c.tryStart;
                    g.tryEnd = c.tryEnd;
                    g.original = newSlot(1);
                    g.wrapped = newSlot(1);
                    groups.add(g);
                    mv.visitTryCatchBlock(label(c.tryStart), label(c.tryEnd), g.dispatch, "java/lang/Throwable");
                }
                g.catches.add(c);
                groupOf.put(c, g);
                catchEntry.put(c, new Label());
            } else if (c.flags == Clause.FINALLY || c.flags == Clause.FAULT) {
                Fin f = new Fin();
                f.clause = c;
                f.exception = newSlot(1);
                f.resume = newSlot(1);
                fins.put(c, f);
                mv.visitTryCatchBlock(label(c.tryStart), label(c.tryEnd), f.entry, null);
            } else {
                throw fail("exception filters (catch ... when) are not supported");
            }
        }
    }

    /// Everything a method does before its first instruction: a private copy
    /// of each struct argument it is going to change, a one-element array for
    /// each variable whose address it takes, and a defined value in every
    /// local, which C# promises and the JVM verifier demands.
    private void emitEntry() {
        for (Var a : args) {
            if (a.mode == STRUCT && (a.addressTaken || a.stored)) {
                mv.visitVarInsn(ALOAD, a.slot);
                copyStruct(a.type);
                mv.visitVarInsn(ASTORE, a.slot);
            } else if (a.mode == PLAIN && a.addressTaken) {
                int from = a.slot;
                a.slot = newSlot(1);
                a.mode = PROMOTED;
                newArray(a.type, 1);
                mv.visitInsn(DUP);
                mv.visitInsn(ICONST_0);
                mv.visitVarInsn(Jvm.loadOp(names.kind(a.type)), from);
                mv.visitInsn(Jvm.arrayStore(names.norm(a.type)));
                mv.visitVarInsn(ASTORE, a.slot);
            }
        }
        for (Var v : locals) {
            if (v.mode == STRUCT) {
                // Always an object of its own, so a store is a copy into it
                // and not an allocation.
                v.slot = newSlot(1);
                newStruct(v.type);
                mv.visitVarInsn(ASTORE, v.slot);
            } else if (v.addressTaken) {
                v.mode = PROMOTED;
                v.slot = newSlot(1);
                newArray(v.type, 1);
                mv.visitVarInsn(ASTORE, v.slot);
            } else {
                v.slot = newSlot(slotSize(v.type));
                int k = names.kind(v.type);
                Jvm.pushDefault(mv, k);
                mv.visitVarInsn(Jvm.storeOp(k), v.slot);
            }
        }
        if (tempPlan != null) {
            for (String key : tempPlan) {
                String c = key.substring(0, key.indexOf('#'));
                int slot = newSlot(1);
                typeInsn(NEW, c);
                mv.visitInsn(DUP);
                invoke(INVOKESPECIAL, c, "<init>", "()V", false);
                mv.visitVarInsn(ASTORE, slot);
                tempSlots.put(key, Integer.valueOf(slot));
            }
        }
        for (Group g : groups) {
            mv.visitInsn(ACONST_NULL);
            mv.visitVarInsn(ASTORE, g.original);
            mv.visitInsn(ACONST_NULL);
            mv.visitVarInsn(ASTORE, g.wrapped);
        }
        for (Fin f : fins.values()) {
            mv.visitInsn(ACONST_NULL);
            mv.visitVarInsn(ASTORE, f.exception);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, f.resume);
        }
        if (isConstructor && !chainsToThis()) {
            host.emitInstanceStructFields(owner, mv);
        } else if (".cctor".equals(method.name)) {
            host.emitStaticStructFields(owner, mv);
        }
    }

    /// A constructor that starts with `this(...)` leaves the fields to the
    /// constructor it calls.
    private boolean chainsToThis() {
        for (Instr ins : body.instructions) {
            if ("call".equals(ins.op)) {
                MethodRef ref = asm.methodRef(ins.operand);
                if (".ctor".equals(ref.name)) {
                    return owner.fullName().equals(ref.declaringType.typeName());
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------ control flow

    private Label label(int at) {
        Label l = labels.get(Integer.valueOf(at));
        if (l == null) {
            l = new Label();
            labels.put(Integer.valueOf(at), l);
        }
        return l;
    }

    /// Places the label of an instruction, emits whatever exception plumbing
    /// starts there, and settles what is on the stack when it runs.
    private void begin(Instr ins) {
        mv.visitLabel(label(ins.offset));
        boolean handler = false;
        for (Group g : groups) {
            if (g.catches.get(0).handlerStart == ins.offset) {
                emitDispatch(g);
            }
        }
        for (Clause c : body.clauses) {
            if (c.handlerStart != ins.offset) {
                continue;
            }
            handler = true;
            stack.clear();
            if (c.flags == Clause.CATCH) {
                mv.visitLabel(catchEntry.get(c));
                stack.add(Val.ref(asm.typeFromToken(c.classToken), Val.HEAP));
            } else {
                Fin f = fins.get(c);
                mv.visitLabel(f.entry);
                mv.visitVarInsn(ASTORE, f.exception);
                mv.visitLabel(f.bodyLabel);
            }
        }
        if (!handler) {
            List<Val> recorded = states.get(Integer.valueOf(ins.offset));
            if (!reachable) {
                stack.clear();
                tempUse.clear();
                if (recorded != null) {
                    stack.addAll(recorded);
                }
            } else if (recorded != null) {
                merge(recorded);
            }
            Map<String, Integer> used = tempStates.get(Integer.valueOf(ins.offset));
            if (used != null) {
                // A value built on either path may still be on the stack.
                for (Map.Entry<String, Integer> e : used.entrySet()) {
                    Integer mine = tempUse.get(e.getKey());
                    if (mine == null || mine.intValue() < e.getValue().intValue()) {
                        tempUse.put(e.getKey(), e.getValue());
                    }
                }
            }
        }
        if (stack.isEmpty()) {
            tempUse.clear();
        }
        reachable = true;
    }

    private void merge(List<Val> other) {
        if (other.size() != stack.size()) {
            throw fail("the stack holds " + stack.size() + " values on one path and " + other.size()
                    + " on another");
        }
        for (int i = 0; i < other.size(); i++) {
            Val a = stack.get(i);
            Val b = other.get(i);
            if (a.kind != b.kind) {
                throw fail("the stack holds " + a + " on one path and " + b + " on another");
            }
            if (a.kind == Val.REF) {
                stack.set(i, Val.ref(a.type != null ? a.type : b.type, Math.max(a.own, b.own)));
            }
        }
    }

    private void flowTo(int target) {
        List<Val> recorded = states.get(Integer.valueOf(target));
        if (!stack.isEmpty() && !tempUse.isEmpty()) {
            Map<String, Integer> used = tempStates.get(Integer.valueOf(target));
            if (used == null) {
                tempStates.put(Integer.valueOf(target), new HashMap<String, Integer>(tempUse));
            } else {
                for (Map.Entry<String, Integer> e : tempUse.entrySet()) {
                    Integer theirs = used.get(e.getKey());
                    if (theirs == null || theirs.intValue() < e.getValue().intValue()) {
                        used.put(e.getKey(), e.getValue());
                    }
                }
            }
        }
        if (recorded == null) {
            states.put(Integer.valueOf(target), new ArrayList<Val>(stack));
        } else if (recorded.size() != stack.size()) {
            throw fail("branch to IL_" + Integer.toHexString(target) + " with a different stack depth");
        }
    }

    /// The shared handler of one `try`'s catch clauses. It turns a JVM
    /// exception (a null dereference, an array index) into the .NET one the
    /// C# code is written to catch, then tries the clauses in order. Emitted
    /// just before the first handler so that it sits inside every enclosing
    /// `try`, which is where a rethrow has to be caught.
    private void emitDispatch(Group g) {
        mv.visitLabel(g.dispatch);
        mv.visitVarInsn(ASTORE, g.original);
        mv.visitVarInsn(ALOAD, g.original);
        invoke(INVOKESTATIC, Names.INTEROP, "wrap", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", false);
        mv.visitVarInsn(ASTORE, g.wrapped);
        for (Clause c : g.catches) {
            String type = names.classRef(asm.typeFromToken(c.classToken));
            Label next = new Label();
            if (!type.equals(Names.OBJECT)) {
                mv.visitVarInsn(ALOAD, g.wrapped);
                typeInsn(INSTANCEOF, type);
                mv.visitJumpInsn(IFEQ, next);
            }
            mv.visitVarInsn(ALOAD, g.wrapped);
            typeInsn(CHECKCAST, type);
            mv.visitJumpInsn(GOTO, catchEntry.get(c));
            mv.visitLabel(next);
        }
        mv.visitVarInsn(ALOAD, g.original);
        mv.visitInsn(ATHROW);
    }

    private void enterFinally(Fin f, Label continuation) {
        int k = f.continuations.indexOf(continuation);
        if (k < 0) {
            k = f.continuations.size();
            f.continuations.add(continuation);
        }
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, f.exception);
        Jvm.pushInt(mv, k);
        mv.visitVarInsn(ISTORE, f.resume);
        mv.visitJumpInsn(GOTO, f.bodyLabel);
    }

    private void leave(int target) {
        for (int i = stack.size() - 1; i >= 0; i--) {
            popJvm(stack.get(i));
        }
        stack.clear();
        final List<Fin> chain = new ArrayList<Fin>();
        for (Clause c : body.clauses) {
            if (c.flags == Clause.FINALLY && offset >= c.tryStart && offset < c.tryEnd
                    && !(target >= c.tryStart && target < c.tryEnd)) {
                chain.add(fins.get(c));
            }
        }
        flowTo(target);
        if (chain.isEmpty()) {
            mv.visitJumpInsn(GOTO, label(target));
            return;
        }
        // Each finally resumes at a trampoline that enters the next one out;
        // the outermost resumes at the target.
        Label continuation = label(target);
        for (int i = chain.size() - 1; i >= 1; i--) {
            final Label here = new Label();
            final Fin next = chain.get(i);
            final Label after = continuation;
            int k = next.continuations.indexOf(after);
            if (k < 0) {
                next.continuations.add(after);
            }
            trampolines.add(new Runnable() {
                @Override
                public void run() {
                    mv.visitLabel(here);
                    enterFinally(next, after);
                }
            });
            continuation = here;
        }
        enterFinally(chain.get(0), continuation);
    }

    private void endFinally() {
        Fin f = null;
        for (Clause c : body.clauses) {
            if (c.flags != Clause.CATCH && offset >= c.handlerStart && offset < c.handlerEnd
                    && (f == null || c.handlerStart >= f.clause.handlerStart)) {
                f = fins.get(c);
            }
        }
        if (f == null) {
            throw fail("endfinally outside a finally block");
        }
        Label normal = new Label();
        mv.visitVarInsn(ALOAD, f.exception);
        mv.visitJumpInsn(IFNULL, normal);
        mv.visitVarInsn(ALOAD, f.exception);
        mv.visitInsn(ATHROW);
        mv.visitLabel(normal);
        int n = f.continuations.size();
        if (n == 0) {
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ATHROW);
        } else if (n == 1) {
            mv.visitJumpInsn(GOTO, f.continuations.get(0));
        } else {
            mv.visitVarInsn(ILOAD, f.resume);
            mv.visitTableSwitchInsn(0, n - 1, f.continuations.get(0),
                    f.continuations.toArray(new Label[0]));
        }
    }

    // ------------------------------------------------------------ stack tools

    private void push(Val v) {
        stack.add(v);
    }

    private Val pop() {
        if (stack.isEmpty()) {
            throw fail("pop from an empty stack");
        }
        return stack.remove(stack.size() - 1);
    }

    private Val peek() {
        return stack.get(stack.size() - 1);
    }

    private void popJvm(Val v) {
        if (v.size() == 2) {
            mv.visitInsn(POP2);
        } else if (v.size() == 1) {
            mv.visitInsn(POP);
        }
    }

    private void invoke(int opcode, String ownerClass, String name, String descriptor, boolean isInterface) {
        host.index.requireMethod(ownerClass, name, descriptor, where());
        mv.visitMethodInsn(opcode, ownerClass, name, descriptor, isInterface);
    }

    private void typeInsn(int opcode, String type) {
        if (!type.startsWith("[")) {
            host.index.requireClass(type, where());
        }
        mv.visitTypeInsn(opcode, type);
    }

    private void copyStruct(CilType type) {
        String c = names.classRef(type);
        invoke(INVOKEVIRTUAL, c, "$copy", "()L" + c + ";", false);
    }

    private void newStruct(CilType type) {
        String c = names.classRef(type);
        typeInsn(NEW, c);
        mv.visitInsn(DUP);
        invoke(INVOKESPECIAL, c, "<init>", "()V", false);
    }

    private void newArray(CilType element, int length) {
        Jvm.pushInt(mv, length);
        newArray(element);
    }

    /// Length is on the stack.
    private void newArray(CilType element) {
        CilType e = names.norm(element);
        if (e.isPrimitive()) {
            mv.visitIntInsn(NEWARRAY, Jvm.arrayTypeCode(e));
        } else if (names.isStruct(e)) {
            String c = names.classRef(e);
            invoke(INVOKESTATIC, c, "$newArray", "(I)[L" + c + ";", false);
        } else {
            typeInsn(ANEWARRAY, names.classRef(e));
        }
    }

    private void maskIfUnsignedByte(CilType type) {
        if (names.norm(type).kind == CilType.Kind.U1) {
            Jvm.pushInt(mv, 255);
            mv.visitInsn(IAND);
        }
    }

    private void box(CilType primitive) {
        Jvm.box(mv, names, primitive);
    }

    /// The integer on the stack becomes the runtime's boxed enum: the value
    /// and the enum's `System.Type`.
    private void boxEnum(CilType type) {
        if (names.kind(type) != Val.I8) {
            mv.visitInsn(I2L);
        }
        mv.visitFieldInsn(GETSTATIC, names.className(type.name), "$TYPE", Translator.TYPE_DESCRIPTOR);
        invoke(INVOKESTATIC, Translator.TYPE, "$box", "(J" + Translator.TYPE_DESCRIPTOR + ")Ljava/lang/Object;",
                false);
    }

    /// `typeof(T)`: the enum's own type, or one made from the class.
    private void loadType(CilType type) {
        if (host.hasEnumType(type)) {
            mv.visitFieldInsn(GETSTATIC, names.className(type.name), "$TYPE", Translator.TYPE_DESCRIPTOR);
            return;
        }
        CilType n = names.norm(type);
        String c;
        if (Names.isTypeVariable(n) || n.kind == CilType.Kind.OBJECT) {
            c = Names.OBJECT;
        } else if (n.isPrimitive()) {
            c = Names.boxClass(n);
        } else {
            c = names.classRef(n);
        }
        mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(c));
        mv.visitLdcInsn(String.valueOf(n.typeName() == null ? n.toString() : n.typeName()).replace('/', '+'));
        invoke(INVOKESTATIC, Translator.TYPE, "$of", "(Ljava/lang/Class;Ljava/lang/String;)"
                + Translator.TYPE_DESCRIPTOR, false);
    }

    private void unbox(CilType primitive) {
        Jvm.unbox(mv, names, primitive);
    }

    /// Turns the value a generic member handed back -- an `Object`, because
    /// generics are erased -- into the type this use of it was instantiated
    /// with, and says what is now on the stack.
    private Val fromErased(CilType declared, CilType substituted, int structOwn) {
        if (Names.isTypeVariable(declared)) {
            CilType s = names.norm(substituted);
            if (s.isPrimitive()) {
                unbox(s);
                return Val.of(names.kind(s), s);
            }
            if (!Names.isTypeVariable(s) && s.kind != CilType.Kind.OBJECT) {
                typeInsn(CHECKCAST, names.classRef(s));
            }
            return Val.ref(s, Val.HEAP);
        }
        if (declared.kind == CilType.Kind.SZARRAY && Names.isTypeVariable(declared.element)
                && substituted.kind == CilType.Kind.SZARRAY) {
            arrayFromErased(substituted);
            return Val.ref(substituted, Val.HEAP);
        }
        CilType d = names.norm(declared);
        if (d.isPrimitive()) {
            maskIfUnsignedByte(d);
            return Val.of(names.kind(d), d);
        }
        return Val.ref(substituted, structOwn);
    }

    /// A member declared to return `T[]` -- `List<T>.ToArray()`,
    /// `GetComponents<T>()` -- hands back an `Object[]`, the only array the
    /// erased runtime can create. C# was promised a `T[]`, and on the JVM
    /// that is a different class an `Object[]` cannot be cast to, so the
    /// elements are moved into an array of the right type here, where the
    /// type is known.
    private void arrayFromErased(CilType array) {
        CilType e = names.norm(array.element);
        String interop = Names.RUNTIME + "system/Interop";
        if (Names.isTypeVariable(e) || e.kind == CilType.Kind.OBJECT) {
            return;
        }
        if (e.isPrimitive()) {
            String d = names.descriptor(array);
            String helper;
            switch (e.kind) {
                case BOOLEAN:
                    helper = "toBooleanArray";
                    break;
                case CHAR:
                    helper = "toCharArray";
                    break;
                case I1:
                case U1:
                    helper = "toByteArray";
                    break;
                case I2:
                    helper = "toShortArray";
                    break;
                case I4:
                case U4:
                    helper = "toIntArray";
                    break;
                case I8:
                case U8:
                    helper = "toLongArray";
                    break;
                case R4:
                    helper = "toFloatArray";
                    break;
                case R8:
                    helper = "toDoubleArray";
                    break;
                default:
                    throw new TranslationException(method + ": an array of " + e + " returned by a generic member"
                            + " is not supported");
            }
            host.index.requireMethod(interop, helper, "([Ljava/lang/Object;)" + d, where());
            mv.visitMethodInsn(INVOKESTATIC, interop, helper, "([Ljava/lang/Object;)" + d, false);
            return;
        }
        if (names.isStruct(e)) {
            throw new TranslationException(method + ": an array of the struct " + e + " returned by a generic"
                    + " member is not supported yet");
        }
        mv.visitInsn(DUP);
        mv.visitInsn(ARRAYLENGTH);
        typeInsn(ANEWARRAY, names.classRef(e));
        host.index.requireMethod(interop, "copyArray", "([Ljava/lang/Object;[Ljava/lang/Object;)[Ljava/lang/Object;",
                where());
        mv.visitMethodInsn(INVOKESTATIC, interop, "copyArray",
                "([Ljava/lang/Object;[Ljava/lang/Object;)[Ljava/lang/Object;", false);
        typeInsn(CHECKCAST, names.descriptor(array));
    }

    /// The reverse of [#fromErased], applied to the value on top of the
    /// stack before it is stored into, or passed as, something declared with
    /// a type variable.
    private void toErased(CilType declared, CilType substituted, Val v) {
        if (Names.isTypeVariable(declared)) {
            CilType s = names.norm(substituted);
            if (s.isPrimitive()) {
                box(s);
            } else if (names.isStruct(s) && v.own != Val.FRESH) {
                copyStruct(s);
            }
        }
    }

    // ------------------------------------------------------------- variables

    private void loadVar(Var v, boolean isArgument) {
        switch (v.mode) {
            case PLAIN: {
                int k = names.kind(v.type);
                mv.visitVarInsn(Jvm.loadOp(k), v.slot);
                if (isArgument) {
                    maskIfUnsignedByte(v.type);
                }
                push(k == Val.REF ? Val.ref(v.type, Val.HEAP) : Val.of(k, names.norm(v.type)));
                break;
            }
            case PROMOTED: {
                mv.visitVarInsn(ALOAD, v.slot);
                mv.visitInsn(ICONST_0);
                mv.visitInsn(Jvm.arrayLoad(names.norm(v.type)));
                maskIfUnsignedByte(v.type);
                int k = names.kind(v.type);
                push(k == Val.REF ? Val.ref(v.type, Val.HEAP) : Val.of(k, names.norm(v.type)));
                break;
            }
            case STRUCT:
                mv.visitVarInsn(ALOAD, v.slot);
                push(Val.ref(v.type, isArgument ? Val.ARG : Val.LOCAL));
                break;
            case REF_STRUCT:
                mv.visitVarInsn(ALOAD, v.slot);
                push(Val.ref(v.type, Val.HEAP));
                break;
            default:
                mv.visitVarInsn(ALOAD, v.slot);
                mv.visitVarInsn(ILOAD, v.slot + 1);
                push(Val.pair(v.type));
                break;
        }
    }

    private void loadAddress(Var v) {
        switch (v.mode) {
            case PROMOTED:
                mv.visitVarInsn(ALOAD, v.slot);
                mv.visitInsn(ICONST_0);
                push(Val.pair(v.type));
                break;
            case STRUCT:
                mv.visitVarInsn(ALOAD, v.slot);
                push(Val.ref(v.type, Val.HEAP));
                break;
            default:
                throw fail("cannot take the address of this variable");
        }
    }

    private void storeVar(Var v) {
        Val value = pop();
        switch (v.mode) {
            case PLAIN:
                mv.visitVarInsn(Jvm.storeOp(names.kind(v.type)), v.slot);
                break;
            case PROMOTED:
                mv.visitVarInsn(ALOAD, v.slot);
                if (value.size() == 2) {
                    mv.visitInsn(DUP_X2);
                    mv.visitInsn(POP);
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(DUP_X2);
                    mv.visitInsn(POP);
                } else {
                    mv.visitInsn(SWAP);
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(SWAP);
                }
                mv.visitInsn(Jvm.arrayStore(names.norm(v.type)));
                break;
            case STRUCT:
                if (value.own == Val.FRESH) {
                    mv.visitVarInsn(ASTORE, v.slot);
                } else {
                    // The variable always holds an object of its own: copy into it.
                    mv.visitVarInsn(ALOAD, v.slot);
                    mv.visitInsn(SWAP);
                    assignStruct(v.type);
                }
                break;
            default:
                throw fail("cannot assign to a by-reference parameter itself");
        }
    }

    /// Stack: target struct, source struct.
    private void assignStruct(CilType type) {
        String c = names.classRef(type);
        invoke(INVOKEVIRTUAL, c, "$assign", "(L" + c + ";)V", false);
    }

    // ------------------------------------------------------------ arithmetic

    /// Brings the two operands of a binary operation to one kind. C# widens
    /// explicitly, so this only fires for the implicit `int32` with `native
    /// int` and `float32` with `float64` pairs CIL allows.
    private int unify() {
        Val b = stack.get(stack.size() - 1);
        Val a = stack.get(stack.size() - 2);
        if (a.kind == b.kind) {
            return a.kind;
        }
        int target = Math.max(a.kind, b.kind);
        if (target > Val.R8 || (a.kind <= Val.I8) != (b.kind <= Val.I8)) {
            throw fail("cannot combine " + a + " with " + b);
        }
        if (b.kind != target) {
            mv.visitInsn(target == Val.I8 ? I2L : F2D);
        } else {
            int tmp = newSlot(2);
            mv.visitVarInsn(Jvm.storeOp(b.kind), tmp);
            mv.visitInsn(target == Val.I8 ? I2L : F2D);
            mv.visitVarInsn(Jvm.loadOp(b.kind), tmp);
        }
        stack.set(stack.size() - 2, Val.of(target, null));
        stack.set(stack.size() - 1, Val.of(target, null));
        return target;
    }

    private void arithmetic(int intOpcode) {
        int k = unify();
        pop();
        pop();
        mv.visitInsn(intOpcode + k);
        push(Val.of(k, null));
    }

    /// Integer division throws on a zero divisor in C#. ParparVM answers 0
    /// instead, so the test is spelled out rather than left to the VM.
    private void divide(int intOpcode) {
        int k = unify();
        if (k == Val.I4 || k == Val.I8) {
            Label ok = new Label();
            if (k == Val.I4) {
                mv.visitInsn(DUP);
            } else {
                mv.visitInsn(DUP2);
                mv.visitInsn(LCONST_0);
                mv.visitInsn(LCMP);
            }
            mv.visitJumpInsn(IFNE, ok);
            invoke(INVOKESTATIC, Names.INTEROP, "divideByZero", "()V", false);
            mv.visitLabel(ok);
        }
        pop();
        pop();
        mv.visitInsn(intOpcode + k);
        push(Val.of(k, null));
    }

    private void unsignedDivide(String helper) {
        int k = unify();
        pop();
        pop();
        if (k == Val.I4) {
            invoke(INVOKESTATIC, Names.INTEROP, helper, "(II)I", false);
        } else if (k == Val.I8) {
            invoke(INVOKESTATIC, Names.INTEROP, helper, "(JJ)J", false);
        } else {
            throw fail("unsigned division of a floating point value");
        }
        push(Val.of(k, null));
    }

    private void bitwise(int intOpcode) {
        int k = unify();
        if (k > Val.I8) {
            throw fail("bitwise operation on a floating point value");
        }
        pop();
        pop();
        mv.visitInsn(intOpcode + k);
        push(Val.of(k, null));
    }

    private void shift(int intOpcode) {
        Val count = pop();
        Val value = pop();
        if (count.kind == Val.I8) {
            mv.visitInsn(L2I);
        }
        if (value.kind > Val.I8) {
            throw fail("shift of " + value);
        }
        mv.visitInsn(intOpcode + value.kind);
        push(Val.of(value.kind, null));
    }

    private void toInt(Val v) {
        if (v.kind == Val.I8) {
            mv.visitInsn(L2I);
        } else if (v.kind == Val.R4) {
            mv.visitInsn(F2I);
        } else if (v.kind == Val.R8) {
            mv.visitInsn(D2I);
        } else if (v.kind != Val.I4) {
            throw fail("cannot convert " + v + " to an integer");
        }
    }

    private void convert(String op) {
        Val v = pop();
        String to = op.substring(5);
        if ("i1".equals(to) || "u1".equals(to) || "i2".equals(to) || "u2".equals(to) || "i4".equals(to)
                || "u4".equals(to) || "i".equals(to) || "u".equals(to)) {
            if ("u4".equals(to) && v.kind >= Val.R4) {
                // Through 64 bits, so a value above Integer.MAX_VALUE wraps
                // instead of saturating.
                mv.visitInsn(v.kind == Val.R4 ? F2L : D2L);
                mv.visitInsn(L2I);
            } else {
                toInt(v);
            }
            if ("i1".equals(to)) {
                mv.visitInsn(I2B);
            } else if ("u1".equals(to)) {
                Jvm.pushInt(mv, 255);
                mv.visitInsn(IAND);
            } else if ("i2".equals(to)) {
                mv.visitInsn(I2S);
            } else if ("u2".equals(to)) {
                mv.visitInsn(I2C);
            }
            push(Val.of(Val.I4, null));
        } else if ("i8".equals(to) || "u8".equals(to)) {
            if (v.kind == Val.I4) {
                mv.visitInsn(I2L);
                if ("u8".equals(to)) {
                    mv.visitLdcInsn(Long.valueOf(0xFFFFFFFFL));
                    mv.visitInsn(LAND);
                }
            } else if (v.kind == Val.R4) {
                mv.visitInsn(F2L);
            } else if (v.kind == Val.R8) {
                mv.visitInsn(D2L);
            }
            push(Val.of(Val.I8, null));
        } else if ("r4".equals(to)) {
            if (v.kind == Val.I4) {
                mv.visitInsn(I2F);
            } else if (v.kind == Val.I8) {
                mv.visitInsn(L2F);
            } else if (v.kind == Val.R8) {
                mv.visitInsn(D2F);
            }
            push(Val.of(Val.R4, null));
        } else if ("r8".equals(to)) {
            if (v.kind == Val.I4) {
                mv.visitInsn(I2D);
            } else if (v.kind == Val.I8) {
                mv.visitInsn(L2D);
            } else if (v.kind == Val.R4) {
                mv.visitInsn(F2D);
            }
            push(Val.of(Val.R8, null));
        } else if ("r.un".equals(to)) {
            if (v.kind == Val.I4) {
                mv.visitInsn(I2L);
                mv.visitLdcInsn(Long.valueOf(0xFFFFFFFFL));
                mv.visitInsn(LAND);
                mv.visitInsn(L2D);
            } else if (v.kind == Val.I8) {
                invoke(INVOKESTATIC, Names.INTEROP, "unsignedToDouble", "(J)D", false);
            } else {
                throw fail("conv.r.un of " + v);
            }
            push(Val.of(Val.R8, null));
        } else {
            throw fail("checked conversion " + op + " is not supported");
        }
    }

    /// Pops two values and jumps when `condition` (`eq ne lt ge gt le`) holds
    /// between them. `unordered` is the CIL `.un` suffix: unsigned for
    /// integers, true-if-NaN for floating point.
    private void jumpIf(String condition, boolean unordered, Label target) {
        Val right = stack.get(stack.size() - 1);
        Val left = stack.get(stack.size() - 2);
        int c = "eq ne lt ge gt le".indexOf(condition) / 3;
        if (left.kind == Val.REF || right.kind == Val.REF) {
            pop();
            pop();
            if ("eq".equals(condition)) {
                mv.visitJumpInsn(IF_ACMPEQ, target);
            } else if ("ne".equals(condition) || ("gt".equals(condition) && unordered)) {
                // `x != null` is spelled `ldnull; cgt.un`.
                mv.visitJumpInsn(IF_ACMPNE, target);
            } else {
                throw fail("ordering comparison of object references");
            }
            return;
        }
        int k = unify();
        pop();
        pop();
        boolean equality = c < 2;
        if (k == Val.I4) {
            if (unordered && !equality) {
                invoke(INVOKESTATIC, Names.INTEROP, "compareUnsigned", "(II)I", false);
                mv.visitJumpInsn(IFEQ + c, target);
            } else {
                mv.visitJumpInsn(IF_ICMPEQ + c, target);
            }
        } else if (k == Val.I8) {
            if (unordered && !equality) {
                invoke(INVOKESTATIC, Names.INTEROP, "compareUnsigned", "(JJ)I", false);
            } else {
                mv.visitInsn(LCMP);
            }
            mv.visitJumpInsn(IFEQ + c, target);
        } else {
            // cmpl answers -1 for NaN and cmpg +1: pick the one that makes
            // the comparison come out the way CIL says it does.
            boolean greater = "gt".equals(condition) || "ge".equals(condition);
            boolean nanIsLow = equality || (greater != unordered);
            if (k == Val.R4) {
                mv.visitInsn(nanIsLow ? FCMPL : FCMPG);
            } else {
                mv.visitInsn(nanIsLow ? DCMPL : DCMPG);
            }
            mv.visitJumpInsn(IFEQ + c, target);
        }
    }

    private void compare(String condition, boolean unordered) {
        Label yes = new Label();
        Label end = new Label();
        jumpIf(condition, unordered, yes);
        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(GOTO, end);
        mv.visitLabel(yes);
        mv.visitInsn(ICONST_1);
        mv.visitLabel(end);
        push(Val.of(Val.I4, CilType.BOOLEAN));
    }

    private void branchOnTruth(boolean whenTrue, int target) {
        Val v = pop();
        flowTo(target);
        if (v.kind == Val.REF) {
            mv.visitJumpInsn(whenTrue ? IFNONNULL : IFNULL, label(target));
        } else if (v.kind == Val.I4) {
            mv.visitJumpInsn(whenTrue ? IFNE : IFEQ, label(target));
        } else if (v.kind == Val.I8) {
            mv.visitInsn(LCONST_0);
            mv.visitInsn(LCMP);
            mv.visitJumpInsn(whenTrue ? IFNE : IFEQ, label(target));
        } else {
            throw fail("branch on " + v);
        }
    }

    // ------------------------------------------------------------ instructions

    private void step(Instr ins) {
        String op = ins.op;
        switch (op) {
            case "nop":
            case "break":
            case "volatile.":
            case "readonly.":
            case "tail.":
            case "unaligned.":
                return;
            case "constrained.":
                constrained = asm.typeFromToken(ins.operand);
                return;
            case "ldarg":
                loadVar(args[ins.operand], true);
                return;
            case "ldarga":
                loadAddress(args[ins.operand]);
                return;
            case "starg":
                storeVar(args[ins.operand]);
                return;
            case "ldloc":
                loadVar(locals[ins.operand], false);
                return;
            case "ldloca":
                loadAddress(locals[ins.operand]);
                return;
            case "stloc":
                storeVar(locals[ins.operand]);
                return;
            case "ldnull":
                mv.visitInsn(ACONST_NULL);
                push(Val.ref(null, Val.HEAP));
                return;
            case "ldc.i4":
                Jvm.pushInt(mv, ins.operand);
                push(Val.of(Val.I4, null));
                return;
            case "ldc.i8":
                mv.visitLdcInsn(Long.valueOf(ins.longOperand));
                push(Val.of(Val.I8, null));
                return;
            case "ldc.r4":
                mv.visitLdcInsn(Float.valueOf((float) ins.doubleOperand));
                push(Val.of(Val.R4, null));
                return;
            case "ldc.r8":
                mv.visitLdcInsn(Double.valueOf(ins.doubleOperand));
                push(Val.of(Val.R8, null));
                return;
            case "ldstr":
                mv.visitLdcInsn(asm.userString(ins.operand));
                push(Val.ref(CilType.STRING, Val.HEAP));
                return;
            case "dup": {
                Val v = pop();
                if (v.size() == 2) {
                    mv.visitInsn(DUP2);
                } else if (v.size() == 1) {
                    mv.visitInsn(DUP);
                }
                // Two holders now: neither may be treated as the only one.
                Val shared = v.kind == Val.REF && v.own == Val.FRESH ? v.owned(Val.HEAP) : v;
                push(shared);
                push(shared);
                return;
            }
            case "pop":
                popJvm(pop());
                return;
            case "ret":
                doReturn();
                return;
            case "br":
                flowTo(ins.operand);
                mv.visitJumpInsn(GOTO, label(ins.operand));
                reachable = false;
                return;
            case "brtrue":
                branchOnTruth(true, ins.operand);
                return;
            case "brfalse":
                branchOnTruth(false, ins.operand);
                return;
            case "beq":
            case "bge":
            case "bgt":
            case "ble":
            case "blt":
            case "bne.un":
            case "bge.un":
            case "bgt.un":
            case "ble.un":
            case "blt.un": {
                boolean un = op.endsWith(".un");
                String condition = op.substring(1, 3);
                jumpIf(condition, un, label(ins.operand));
                flowTo(ins.operand);
                return;
            }
            case "switch": {
                pop();
                Label[] targets = new Label[ins.targets.length];
                for (int i = 0; i < targets.length; i++) {
                    flowTo(ins.targets[i]);
                    targets[i] = label(ins.targets[i]);
                }
                Label fallThrough = new Label();
                mv.visitTableSwitchInsn(0, targets.length - 1, fallThrough, targets);
                mv.visitLabel(fallThrough);
                return;
            }
            case "leave":
                leave(ins.operand);
                reachable = false;
                return;
            case "endfinally":
                endFinally();
                reachable = false;
                return;
            case "throw":
                pop();
                mv.visitTypeInsn(CHECKCAST, "java/lang/Throwable");
                mv.visitInsn(ATHROW);
                reachable = false;
                return;
            case "rethrow": {
                Group g = null;
                Clause best = null;
                for (Clause c : body.clauses) {
                    if (c.flags == Clause.CATCH && offset >= c.handlerStart && offset < c.handlerEnd
                            && (best == null || c.handlerStart >= best.handlerStart)) {
                        best = c;
                        g = groupOf.get(c);
                    }
                }
                if (g == null) {
                    throw fail("rethrow outside a catch block");
                }
                mv.visitVarInsn(ALOAD, g.original);
                mv.visitInsn(ATHROW);
                reachable = false;
                return;
            }
            case "add":
                arithmetic(IADD);
                return;
            case "sub":
                arithmetic(ISUB);
                return;
            case "mul":
                arithmetic(IMUL);
                return;
            case "div":
                divide(IDIV);
                return;
            case "rem":
                divide(IREM);
                return;
            case "div.un":
                unsignedDivide("divideUnsigned");
                return;
            case "rem.un":
                unsignedDivide("remainderUnsigned");
                return;
            case "and":
                bitwise(IAND);
                return;
            case "or":
                bitwise(IOR);
                return;
            case "xor":
                bitwise(IXOR);
                return;
            case "shl":
                shift(ISHL);
                return;
            case "shr":
                shift(ISHR);
                return;
            case "shr.un":
                shift(IUSHR);
                return;
            case "neg": {
                Val v = pop();
                mv.visitInsn(INEG + v.kind);
                push(Val.of(v.kind, null));
                return;
            }
            case "not": {
                Val v = pop();
                if (v.kind == Val.I4) {
                    mv.visitInsn(ICONST_M1);
                    mv.visitInsn(IXOR);
                } else if (v.kind == Val.I8) {
                    mv.visitLdcInsn(Long.valueOf(-1L));
                    mv.visitInsn(LXOR);
                } else {
                    throw fail("not of " + v);
                }
                push(Val.of(v.kind, null));
                return;
            }
            case "ceq":
                compare("eq", false);
                return;
            case "cgt":
                compare("gt", false);
                return;
            case "cgt.un":
                compare("gt", true);
                return;
            case "clt":
                compare("lt", false);
                return;
            case "clt.un":
                compare("lt", true);
                return;
            case "call":
                call(asm.methodRef(ins.operand), false);
                return;
            case "callvirt":
                call(asm.methodRef(ins.operand), true);
                return;
            case "newobj":
                newObject(asm.methodRef(ins.operand));
                return;
            case "ldftn":
            case "ldtoken":
                push(Val.token(ins.operand, false));
                return;
            case "ldvirtftn":
                popJvm(pop());
                push(Val.token(ins.operand, true));
                return;
            case "ldfld":
                loadField(asm.fieldRef(ins.operand), false, false);
                return;
            case "ldflda":
                loadField(asm.fieldRef(ins.operand), false, true);
                return;
            case "ldsfld":
                loadField(asm.fieldRef(ins.operand), true, false);
                return;
            case "ldsflda":
                loadField(asm.fieldRef(ins.operand), true, true);
                return;
            case "stfld":
                storeField(asm.fieldRef(ins.operand), false);
                return;
            case "stsfld":
                storeField(asm.fieldRef(ins.operand), true);
                return;
            case "newarr": {
                pop();
                CilType element = asm.typeFromToken(ins.operand);
                newArray(element);
                push(Val.ref(CilType.szArray(element), Val.HEAP));
                return;
            }
            case "ldlen":
                pop();
                mv.visitInsn(ARRAYLENGTH);
                push(Val.of(Val.I4, null));
                return;
            case "ldelem":
                loadElement(asm.typeFromToken(ins.operand));
                return;
            case "ldelem.ref": {
                pop();
                Val array = pop();
                mv.visitInsn(AALOAD);
                CilType element = array.type != null && array.type.kind == CilType.Kind.SZARRAY
                        ? array.type.element : null;
                push(Val.ref(element, Val.HEAP));
                return;
            }
            case "ldelema": {
                CilType element = asm.typeFromToken(ins.operand);
                if (names.isStruct(element)) {
                    pop();
                    pop();
                    mv.visitInsn(AALOAD);
                    push(Val.ref(element, Val.HEAP));
                } else {
                    // The array and the index already are the reference.
                    pop();
                    pop();
                    push(Val.pair(element));
                }
                return;
            }
            case "stelem":
                storeElement(asm.typeFromToken(ins.operand));
                return;
            case "stelem.ref":
                pop();
                pop();
                pop();
                mv.visitInsn(AASTORE);
                return;
            case "ldobj":
                loadIndirect(asm.typeFromToken(ins.operand));
                return;
            case "stobj":
                storeIndirect(asm.typeFromToken(ins.operand));
                return;
            case "ldind.ref":
                loadIndirect(CilType.OBJECT);
                return;
            case "stind.ref":
                storeIndirect(CilType.OBJECT);
                return;
            case "initobj": {
                CilType type = asm.typeFromToken(ins.operand);
                Val address = pop();
                if (address.kind == Val.PAIR) {
                    Jvm.pushDefault(mv, names.kind(address.type));
                    mv.visitInsn(Jvm.arrayStore(names.norm(address.type)));
                } else if (address.kind == Val.FIELD) {
                    Jvm.pushDefault(mv, names.kind(address.type));
                    putField(address.field, address.staticField, Val.of(names.kind(address.type), address.type));
                } else if (names.isStruct(type)) {
                    String c = names.classRef(type);
                    invoke(INVOKEVIRTUAL, c, "$clear", "()V", false);
                } else {
                    throw fail("initobj through " + address);
                }
                return;
            }
            case "box": {
                CilType type = asm.typeFromToken(ins.operand);
                Val v = pop();
                CilType n = names.norm(type);
                if (host.hasEnumType(type)) {
                    // A boxed enum keeps its type, so that it prints as its
                    // name and `Enum` can be asked about it.
                    boxEnum(type);
                } else if (n.isPrimitive()) {
                    box(n);
                } else if (names.isStruct(n) && v.own != Val.FRESH) {
                    copyStruct(n);
                }
                push(Val.ref(CilType.OBJECT, Val.FRESH));
                return;
            }
            case "unbox.any":
            case "unbox": {
                CilType type = asm.typeFromToken(ins.operand);
                CilType n = names.norm(type);
                pop();
                Val result;
                if (n.isPrimitive()) {
                    invoke(INVOKESTATIC, Names.INTEROP, "unbox" + Jvm.unboxSuffix(n),
                            "(Ljava/lang/Object;)" + names.descriptor(n), false);
                    maskIfUnsignedByte(n);
                    result = Val.of(names.kind(n), n);
                } else {
                    checkedCast(n);
                    result = Val.ref(n, Val.HEAP);
                }
                push("unbox".equals(op) && !names.isStruct(n) ? result.asAddress() : result);
                return;
            }
            case "castclass":
                pop();
                checkedCast(asm.typeFromToken(ins.operand));
                push(Val.ref(asm.typeFromToken(ins.operand), Val.HEAP));
                return;
            case "isinst": {
                CilType type = asm.typeFromToken(ins.operand);
                pop();
                if (!Names.isTypeVariable(type) && type.kind != CilType.Kind.OBJECT) {
                    String c = names.instanceClass(type);
                    Label ok = new Label();
                    mv.visitInsn(DUP);
                    typeInsn(INSTANCEOF, c);
                    mv.visitJumpInsn(IFNE, ok);
                    mv.visitInsn(POP);
                    mv.visitInsn(ACONST_NULL);
                    mv.visitLabel(ok);
                    typeInsn(CHECKCAST, c);
                }
                push(Val.ref(names.norm(type).isPrimitive() ? CilType.OBJECT : type, Val.HEAP));
                return;
            }
            default:
                break;
        }
        if (op.startsWith("conv.")) {
            convert(op);
        } else if (op.startsWith("ldelem.")) {
            loadElement(Jvm.suffixType(op.substring(7)));
        } else if (op.startsWith("stelem.")) {
            storeElement(Jvm.suffixType(op.substring(7)));
        } else if (op.startsWith("ldind.")) {
            loadIndirect(Jvm.suffixType(op.substring(6)));
        } else if (op.startsWith("stind.")) {
            storeIndirect(Jvm.suffixType(op.substring(6)));
        } else {
            throw fail("instruction " + op + " is not supported");
        }
    }

    private void requirePair(Val address) {
        if (address.kind != Val.PAIR) {
            throw fail("a store through " + address + " is not supported");
        }
    }

    /// `castclass` is checked on .NET. ParparVM's `CHECKCAST` is not: a
    /// failed cast there hands the wrong object on. So the test is explicit.
    private void checkedCast(CilType type) {
        if (Names.isTypeVariable(type) || type.kind == CilType.Kind.OBJECT) {
            return;
        }
        String c = names.instanceClass(type);
        Label ok = new Label();
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNULL, ok);
        mv.visitInsn(DUP);
        typeInsn(INSTANCEOF, c);
        mv.visitJumpInsn(IFNE, ok);
        invoke(INVOKESTATIC, Names.INTEROP, "invalidCast", "()V", false);
        mv.visitLabel(ok);
        typeInsn(CHECKCAST, c);
    }

    private void doReturn() {
        CilType rt = method.sig.returnType;
        if (factory) {
            mv.visitVarInsn(ALOAD, args[0].slot);
            mv.visitInsn(ARETURN);
        } else if (rt.kind == CilType.Kind.VOID) {
            mv.visitInsn(RETURN);
        } else {
            Val v = pop();
            if (retSlot >= 0) {
                if (forwardedReturn) {
                    // Already built where the caller asked for it.
                    forwardedReturn = false;
                } else if (v.own != Val.FRESH) {
                    mv.visitVarInsn(ALOAD, retSlot);
                    mv.visitInsn(SWAP);
                    assignStruct(rt);
                    mv.visitVarInsn(ALOAD, retSlot);
                }
            }
            int k = names.kind(rt);
            mv.visitInsn(k == Val.REF ? ARETURN : IRETURN + k);
        }
        reachable = false;
    }

    private void loadElement(CilType declared) {
        pop();
        Val array = pop();
        CilType element = declared;
        if (array.type != null && array.type.kind == CilType.Kind.SZARRAY
                && names.norm(array.type.element).isPrimitive() && names.norm(declared).isPrimitive()
                && Jvm.byteSize(names.norm(array.type.element)) == Jvm.byteSize(names.norm(declared))
                && names.norm(declared).kind != CilType.Kind.U1) {
            element = declared.kind == CilType.Kind.I2 || declared.kind == CilType.Kind.U2
                    ? array.type.element : declared;
        }
        CilType e = names.norm(element);
        mv.visitInsn(Jvm.arrayLoad(e));
        maskIfUnsignedByte(e);
        int k = names.kind(e);
        push(k == Val.REF ? Val.ref(element, Val.HEAP) : Val.of(k, e));
    }

    private void storeElement(CilType declared) {
        Val value = pop();
        pop();
        Val array = pop();
        // stelem.i2 stores into short[] and char[] alike, and the JVM has an
        // instruction for each: what the array really is decides.
        CilType element = declared;
        if (array.type != null && array.type.kind == CilType.Kind.SZARRAY
                && names.norm(array.type.element).isPrimitive() && names.norm(declared).isPrimitive()) {
            element = array.type.element;
        }
        if (names.isStruct(element) && value.own != Val.FRESH) {
            // Copy into the element already there: struct arrays are filled
            // when they are created.
            String c = names.classRef(element);
            invoke(INVOKESTATIC, c, "$store", "([L" + c + ";IL" + c + ";)V", false);
        } else {
            mv.visitInsn(Jvm.arrayStore(names.norm(element)));
        }
    }

    private void loadIndirect(CilType type) {
        Val address = pop();
        CilType t = names.norm(type);
        if (address.kind == Val.PAIR) {
            mv.visitInsn(Jvm.arrayLoad(names.norm(address.type)));
            maskIfUnsignedByte(address.type);
            int k = names.kind(address.type);
            push(k == Val.REF ? Val.ref(address.type, Val.HEAP) : Val.of(k, names.norm(address.type)));
        } else if (address.kind == Val.FIELD) {
            push(getField(address.field, address.staticField));
        } else if (address.valueAsAddress) {
            push(address.kind == Val.REF ? Val.ref(address.type, Val.HEAP) : Val.of(address.kind, address.type));
        } else if (address.kind == Val.REF) {
            // A reference to a struct is the struct.
            push(Val.ref(t, Val.HEAP));
        } else {
            throw fail("load through " + address);
        }
    }

    private void storeIndirect(CilType type) {
        Val value = pop();
        Val address = pop();
        if (address.kind == Val.PAIR) {
            mv.visitInsn(Jvm.arrayStore(names.norm(address.type)));
        } else if (address.kind == Val.FIELD) {
            putField(address.field, address.staticField, value);
        } else if (address.kind == Val.REF && !address.valueAsAddress && names.isStruct(type)) {
            assignStruct(type);
        } else {
            requirePair(address);
        }
    }

    // ----------------------------------------------------------------- fields

    private CilType[] typeArgsOf(CilType declaringType) {
        return declaringType.kind == CilType.Kind.GENERICINST ? declaringType.args : null;
    }

    /// string.Empty and the like: a static of a type that is a JDK class
    /// here lives on its helper class.
    private String fieldOwner(FieldRef ref) {
        String declaring = ref.declaringType.typeName();
        return Names.hasHelperClass(declaring) ? Names.helperClass(declaring) : names.classRef(ref.declaringType);
    }

    private Val getField(FieldRef ref, boolean isStatic) {
        String c = fieldOwner(ref);
        String name = Names.sanitize(ref.name);
        String descriptor = names.descriptor(ref.type);
        host.index.requireField(c, name, descriptor, where());
        mv.visitFieldInsn(isStatic ? GETSTATIC : GETFIELD, c, name, descriptor);
        return fromErased(ref.type, ref.type.substitute(typeArgsOf(ref.declaringType), null), Val.HEAP);
    }

    private void putField(FieldRef ref, boolean isStatic, Val value) {
        String c = fieldOwner(ref);
        String name = Names.sanitize(ref.name);
        String descriptor = names.descriptor(ref.type);
        host.index.requireField(c, name, descriptor, where());
        toErased(ref.type, ref.type.substitute(typeArgsOf(ref.declaringType), null), value);
        mv.visitFieldInsn(isStatic ? PUTSTATIC : PUTFIELD, c, name, descriptor);
    }

    private void loadField(FieldRef ref, boolean isStatic, boolean address) {
        CilType substituted = ref.type.substitute(typeArgsOf(ref.declaringType), null);
        if (!isStatic) {
            Val target = pop();
            if (target.kind != Val.REF) {
                throw fail("field " + ref + " read through " + target);
            }
        }
        if (address && !names.isStruct(substituted)) {
            // The object stays on the stack and stands for the field.
            push(Val.field(ref, substituted, isStatic));
            return;
        }
        push(getField(ref, isStatic));
    }

    private void storeField(FieldRef ref, boolean isStatic) {
        Val value = pop();
        if (!isStatic) {
            pop();
        }
        if (names.isStruct(ref.type) && value.own != Val.FRESH) {
            if (isConstructor) {
                // Before the base constructor has run the object cannot be
                // read, so the field is replaced rather than copied into.
                copyStruct(ref.type);
            } else {
                String c = fieldOwner(ref);
                String name = Names.sanitize(ref.name);
                String descriptor = names.descriptor(ref.type);
                if (isStatic) {
                    mv.visitFieldInsn(GETSTATIC, c, name, descriptor);
                } else {
                    mv.visitInsn(SWAP);
                    mv.visitFieldInsn(GETFIELD, c, name, descriptor);
                }
                mv.visitInsn(SWAP);
                assignStruct(ref.type);
                return;
            }
        }
        putField(ref, isStatic, value);
    }

    // ------------------------------------------------------------------ calls

    /// One thing to do to an argument once it is on top of the JVM stack.
    private static final int FIX_NONE = 0;
    private static final int FIX_BOX = 1;
    private static final int FIX_COPY = 2;
    private static final int FIX_DEREF = 3;
    private static final int FIX_FIELD_BYREF = 4;
    private static final int FIX_ARRAY_VIEW = 5;
    private static final int FIX_GENERIC_BYREF = 6;

    /// An array is every one of these on .NET and none of them on the JVM,
    /// so an array handed to a parameter of such a type goes in a view that
    /// implements them over the same elements.
    private static boolean isCollectionInterface(CilType type) {
        CilType t = type.kind == CilType.Kind.GENERICINST ? type.element : type;
        String name = t.typeName();
        return name != null && (name.equals("System.Collections.IEnumerable")
                || name.equals("System.Collections.Generic.IEnumerable`1")
                || name.equals("System.Collections.Generic.ICollection`1")
                || name.equals("System.Collections.Generic.IList`1")
                || name.equals("System.Collections.Generic.IReadOnlyCollection`1")
                || name.equals("System.Collections.Generic.IReadOnlyList`1")
                || name.equals("System.Collections.ICollection") || name.equals("System.Collections.IList"));
    }

    private final List<Runnable> afterCall = new ArrayList<Runnable>();

    /// Applies per-argument fixes to values that are already on the stack.
    /// Only the top one can be touched, so everything above the deepest
    /// argument that needs a fix is moved to temporaries and brought back one
    /// at a time.
    private void fixArguments(Val[] values, int[] fixes, CilType[] fixTypes) {
        int deepest = -1;
        for (int i = 0; i < values.length; i++) {
            if (fixes[i] != FIX_NONE) {
                deepest = i;
                break;
            }
        }
        if (deepest < 0) {
            return;
        }
        int[] temps = new int[values.length];
        for (int i = values.length - 1; i > deepest; i--) {
            temps[i] = spill(values[i]);
        }
        applyFix(fixes[deepest], fixTypes[deepest], values[deepest]);
        for (int i = deepest + 1; i < values.length; i++) {
            reload(values[i], temps[i]);
            applyFix(fixes[i], fixTypes[i], values[i]);
        }
    }

    private int spill(Val v) {
        if (v.kind == Val.PAIR) {
            int slot = newSlot(2);
            mv.visitVarInsn(ISTORE, slot + 1);
            mv.visitVarInsn(ASTORE, slot);
            return slot;
        }
        if (v.size() == 0) {
            return -1;
        }
        int slot = newSlot(v.size());
        mv.visitVarInsn(v.kind == Val.FIELD ? ASTORE : Jvm.storeOp(v.kind), slot);
        return slot;
    }

    private void reload(Val v, int slot) {
        if (v.kind == Val.PAIR) {
            mv.visitVarInsn(ALOAD, slot);
            mv.visitVarInsn(ILOAD, slot + 1);
        } else if (v.size() != 0) {
            mv.visitVarInsn(v.kind == Val.FIELD ? ALOAD : Jvm.loadOp(v.kind), slot);
        }
    }

    private void applyFix(int fix, CilType type, Val v) {
        switch (fix) {
            case FIX_BOX:
                box(type);
                break;
            case FIX_COPY:
                copyStruct(type);
                break;
            case FIX_DEREF:
                if (v.kind == Val.FIELD) {
                    getField(v.field, v.staticField);
                } else {
                    mv.visitInsn(Jvm.arrayLoad(names.norm(v.type)));
                    maskIfUnsignedByte(v.type);
                }
                break;
            case FIX_FIELD_BYREF:
                fieldByReference(v);
                break;
            case FIX_ARRAY_VIEW:
                invoke(INVOKESTATIC, Names.RUNTIME + "system/ArrayView", "$of",
                        "(Ljava/lang/Object;)L" + Names.RUNTIME + "system/ArrayView;", false);
                break;
            case FIX_GENERIC_BYREF:
                genericByReference(type);
                break;
            default:
                break;
        }
    }

    /// `ref obj.field` passed to a method. A by-reference parameter is an
    /// array and an index, and a field is neither, so the value travels in a
    /// one-element array and is written back when the call returns. That is
    /// the same thing unless the callee reads the field some other way while
    /// it runs.
    private void fieldByReference(final Val v) {
        final int holder = v.staticField ? -1 : newSlot(1);
        final int array = newSlot(1);
        final CilType element = names.norm(v.type);
        if (!v.staticField) {
            mv.visitVarInsn(ASTORE, holder);
        }
        newArray(v.type, 1);
        mv.visitVarInsn(ASTORE, array);
        mv.visitVarInsn(ALOAD, array);
        mv.visitInsn(ICONST_0);
        if (!v.staticField) {
            mv.visitVarInsn(ALOAD, holder);
        }
        getField(v.field, v.staticField);
        mv.visitInsn(Jvm.arrayStore(element));
        mv.visitVarInsn(ALOAD, array);
        mv.visitInsn(ICONST_0);
        afterCall.add(new Runnable() {
            @Override
            public void run() {
                if (!v.staticField) {
                    mv.visitVarInsn(ALOAD, holder);
                }
                mv.visitVarInsn(ALOAD, array);
                mv.visitInsn(ICONST_0);
                mv.visitInsn(Jvm.arrayLoad(element));
                maskIfUnsignedByte(element);
                int k = names.kind(element);
                putField(v.field, v.staticField, k == Val.REF ? Val.ref(v.type, Val.HEAP) : Val.of(k, element));
            }
        });
    }

    /// `out TValue` of an erased callee -- `TryGetValue` -- where the value
    /// is a primitive or a struct. The callee writes an `Object` into an
    /// `Object[]`, which is neither an `int[]` nor the struct a local is, so
    /// the value travels boxed in a one-element array and is moved back when
    /// the call returns. A null written there is `default(T)`.
    private void genericByReference(final CilType type) {
        final boolean struct = names.isStruct(type);
        final int target = newSlot(struct ? 1 : 2);
        final int array = newSlot(1);
        if (struct) {
            mv.visitVarInsn(ASTORE, target);
        } else {
            mv.visitVarInsn(ISTORE, target + 1);
            mv.visitVarInsn(ASTORE, target);
        }
        mv.visitInsn(ICONST_1);
        mv.visitTypeInsn(ANEWARRAY, Names.OBJECT);
        mv.visitVarInsn(ASTORE, array);
        mv.visitVarInsn(ALOAD, array);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ALOAD, target);
        if (struct) {
            copyStruct(type);
        } else {
            mv.visitVarInsn(ILOAD, target + 1);
            mv.visitInsn(Jvm.arrayLoad(type));
            maskIfUnsignedByte(type);
            box(type);
        }
        mv.visitInsn(AASTORE);
        mv.visitVarInsn(ALOAD, array);
        mv.visitInsn(ICONST_0);
        afterCall.add(new Runnable() {
            @Override
            public void run() {
                Label absent = new Label();
                Label done = new Label();
                mv.visitVarInsn(ALOAD, target);
                if (!struct) {
                    mv.visitVarInsn(ILOAD, target + 1);
                }
                mv.visitVarInsn(ALOAD, array);
                mv.visitInsn(ICONST_0);
                mv.visitInsn(AALOAD);
                mv.visitInsn(DUP);
                mv.visitJumpInsn(IFNULL, absent);
                if (struct) {
                    typeInsn(CHECKCAST, names.classRef(type));
                    assignStruct(type);
                } else {
                    unbox(type);
                }
                mv.visitJumpInsn(GOTO, done);
                mv.visitLabel(absent);
                mv.visitInsn(POP);
                if (struct) {
                    String c = names.classRef(type);
                    invoke(INVOKEVIRTUAL, c, "$clear", "()V", false);
                } else {
                    Jvm.pushDefault(mv, names.kind(type));
                }
                mv.visitLabel(done);
                if (!struct) {
                    mv.visitInsn(Jvm.arrayStore(type));
                }
            }
        });
    }

    /// Decides what an argument needs before the call. `erased` is false
    /// when the callee is a helper whose descriptor spells out the real
    /// types, so nothing is boxed for it.
    private void argumentFix(CilType declared, CilType substituted, Val v, boolean erased, boolean calleeIsQuiet,
            int i, int[] fixes, CilType[] fixTypes) {
        if (declared.kind == CilType.Kind.BYREF) {
            if (v.valueAsAddress) {
                throw fail("passing the address of a boxed value is not supported");
            }
            if (v.kind == Val.FIELD) {
                fixes[i] = FIX_FIELD_BYREF;
            }
            if (Names.isTypeVariable(declared.element) && erased) {
                CilType element = names.norm(substituted.kind == CilType.Kind.BYREF ? substituted.element
                        : substituted);
                boolean primitive = element.isPrimitive();
                if (primitive || names.isStruct(element)) {
                    if (v.kind != (primitive ? Val.PAIR : Val.REF)) {
                        throw fail("passing a reference to a field where the callee is generic is not supported");
                    }
                    fixes[i] = FIX_GENERIC_BYREF;
                    fixTypes[i] = element;
                }
            }
            return;
        }
        CilType s = names.norm(substituted);
        if (v.type != null && (v.type.kind == CilType.Kind.SZARRAY || v.type.kind == CilType.Kind.ARRAY)
                && isCollectionInterface(s)) {
            fixes[i] = FIX_ARRAY_VIEW;
            return;
        }
        if (Names.isTypeVariable(declared) && erased) {
            if (s.isPrimitive()) {
                fixes[i] = FIX_BOX;
                fixTypes[i] = s;
            } else if (names.isStruct(s) && v.own != Val.FRESH) {
                fixes[i] = FIX_COPY;
                fixTypes[i] = s;
            }
        } else if (names.isStruct(s) && v.own == Val.HEAP && !calleeIsQuiet) {
            // The callee may change the field or element this came from
            // while it still reads the argument.
            fixes[i] = FIX_COPY;
            fixTypes[i] = s;
        }
    }

    private static boolean isObjectVirtual(MethodRef ref) {
        return Names.objectVirtual(ref.name, ref.sig) != null;
    }

    private void call(MethodRef ref, boolean virtual) {
        CilType ct = constrained;
        constrained = null;
        MethodSig sig = ref.sig;
        String declaring = ref.declaringType.typeName();
        if (ref.declaringType.kind == CilType.Kind.ARRAY) {
            mdArrayAccess(ref);
            return;
        }
        if (declaring == null) {
            throw fail("call to " + ref + " (a method of an array type) is not supported");
        }
        if ("System.Runtime.CompilerServices.RuntimeHelpers".equals(declaring)
                && "InitializeArray".equals(ref.name)) {
            initializeArray();
            return;
        }
        if ("System.Type".equals(declaring) && "GetTypeFromHandle".equals(ref.name) && !stack.isEmpty()
                && peek().kind == Val.TOKEN) {
            Val handle = pop();
            loadType(asm.typeFromToken(handle.token));
            push(Val.ref(sig.returnType, Val.HEAP));
            return;
        }
        int n = sig.params.length;
        int first = sig.hasThis ? 1 : 0;
        Val[] values = new Val[n + first];
        for (int i = values.length - 1; i >= 0; i--) {
            values[i] = pop();
        }
        int[] fixes = new int[values.length];
        CilType[] fixTypes = new CilType[values.length];
        CilType[] typeArgs = ref.typeArgs();

        // Where the call goes.
        int opcode;
        String target;
        String name;
        String descriptor;
        boolean erased = true;
        boolean isInterface = false;
        CilType constrainedPrimitive = ct != null && names.norm(ct).isPrimitive() ? names.norm(ct) : null;
        // `side.ToString()` on an enum is the name of the value, which the
        // integer alone cannot give: the enum's type goes with it.
        boolean enumToString = constrainedPrimitive != null && host.hasEnumType(ct) && ref.name.equals("ToString")
                && n == 0;
        if (enumToString) {
            opcode = INVOKESTATIC;
            target = Translator.TYPE;
            name = "$name";
            descriptor = "(" + names.descriptor(constrainedPrimitive) + Translator.TYPE_DESCRIPTOR
                    + ")Ljava/lang/String;";
            erased = false;
        } else if (constrainedPrimitive != null) {
            // A method called on a primitive through `object` or an interface
            // it implements: the helper class of the primitive has it.
            opcode = INVOKESTATIC;
            target = Names.helperClass(constrainedPrimitive.typeName());
            name = Names.sanitize(ref.name);
            StringBuilder sb = new StringBuilder("(").append(names.descriptor(constrainedPrimitive));
            for (CilType p : sig.params) {
                sb.append(names.parameterDescriptor(p.substitute(typeArgs, ref.methodArgs)));
            }
            descriptor = sb.append(')').append(names.descriptor(
                    sig.returnType.substitute(typeArgs, ref.methodArgs))).toString();
            erased = false;
        } else if ("System.Object".equals(declaring) && ".ctor".equals(ref.name)) {
            opcode = INVOKESPECIAL;
            target = Names.OBJECT;
            name = "<init>";
            descriptor = "()V";
        } else if (sig.hasThis && isObjectVirtual(ref) && !virtual && !ownerIsStruct
                && ("System.Object".equals(declaring) || "System.ValueType".equals(declaring))) {
            // base.ToString() and friends.
            opcode = INVOKESPECIAL;
            target = Names.OBJECT;
            name = Names.objectVirtual(ref.name, sig);
            descriptor = names.methodDescriptor(sig);
        } else if (Names.hasHelperClass(declaring)) {
            opcode = INVOKESTATIC;
            target = Names.helperClass(declaring);
            name = Names.sanitize(ref.name) + names.enumSuffix(sig);
            descriptor = names.methodDescriptor(sig, sig.hasThis ? names.descriptor(ref.declaringType) : null);
            if ("System.Array".equals(declaring) && values.length > 0 && values[0].type != null
                    && values[0].type.kind == CilType.Kind.SZARRAY && names.isStruct(values[0].type.element)) {
                // The runtime cannot tell an array of structs from an array
                // of objects, and the two are copied and cleared differently.
                if ("Clone".equals(name)) {
                    name = "$cloneStructs";
                } else if ("Clear".equals(name)) {
                    name = "$clearStructs";
                }
            }
        } else if ("System.IComparable`1".equals(declaring) || "System.IComparable".equals(declaring)
                || "System.IEquatable`1".equals(declaring)) {
            // Strings and boxed primitives implement these on .NET and are
            // JDK classes here, so the call goes through a helper that knows
            // both.
            opcode = INVOKESTATIC;
            target = Names.INTEROP;
            name = Names.sanitize(ref.name);
            descriptor = names.methodDescriptor(sig, "Ljava/lang/Object;");
        } else {
            TypeDef def = host.universe.definitionOf(ref.declaringType);
            if (def == null) {
                throw fail("type " + declaring + " is not defined by any loaded assembly");
            }
            boolean targetIsStruct = names.isStruct(ref.declaringType);
            name = names.methodName(ref.name, sig, targetIsStruct);
            descriptor = names.methodDescriptor(sig);
            target = names.classRef(ref.declaringType);
            if (!sig.hasThis) {
                opcode = INVOKESTATIC;
            } else if ("<init>".equals(name)) {
                opcode = INVOKESPECIAL;
            } else if (def.isInterface()) {
                opcode = INVOKEINTERFACE;
                isInterface = true;
            } else if (!virtual && !targetIsStruct && !declaring.equals(owner.fullName())
                    && host.universe.derivesFrom(owner.fullName(), declaring) && isVirtual(ref)) {
                opcode = INVOKESPECIAL;
            } else {
                opcode = INVOKEVIRTUAL;
            }
        }

        // What each value on the stack needs first.
        if (sig.hasThis) {
            Val receiver = values[0];
            if (receiver.kind == Val.PAIR || receiver.kind == Val.FIELD) {
                boolean viaAddress = ct != null
                        || (CilType.builtin(declaring) != null && CilType.builtin(declaring).isPrimitive());
                if (!viaAddress) {
                    throw fail("call to " + ref + " through " + receiver);
                }
                fixes[0] = FIX_DEREF;
            }
        }
        boolean quiet = host.isNonInterfering(ref);
        for (int i = 0; i < n; i++) {
            argumentFix(sig.params[i], sig.params[i].substitute(typeArgs, ref.methodArgs), values[first + i],
                    erased, quiet, first + i, fixes, fixTypes);
        }
        fixArguments(values, fixes, fixTypes);
        boolean structResult = constrainedPrimitive == null && names.returnsStruct(sig);
        if (sig.genericParamCount > 0 && ref.methodArgs != null && constrainedPrimitive == null
                && !names.isTranslated(declaring)) {
            // A generic method of the hand-written runtime, `GetComponent<T>()`
            // being the one every script calls. Erasure leaves the callee no
            // way to know T, so each type argument travels as a class, after
            // the declared parameters.
            StringBuilder classes = new StringBuilder();
            for (CilType argument : ref.methodArgs) {
                CilType a = names.norm(argument);
                // A primitive or an enum travels as the class it is boxed
                // as, which is what the erased callee is handed too; a type
                // variable of the caller is not known here either.
                String c;
                if (Names.isTypeVariable(a) || a.kind == CilType.Kind.OBJECT) {
                    c = Names.OBJECT;
                } else if (a.isPrimitive()) {
                    c = Names.boxClass(a);
                } else {
                    c = names.classRef(a);
                }
                mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(c));
                classes.append("Ljava/lang/Class;");
            }
            int at = descriptor.lastIndexOf(')');
            if (structResult) {
                at -= names.descriptor(sig.returnType).length();
            }
            descriptor = descriptor.substring(0, at) + classes + descriptor.substring(at);
        }
        if (structResult) {
            loadResultTarget(sig.returnType.substitute(typeArgs, ref.methodArgs));
        }
        if (enumToString) {
            mv.visitFieldInsn(GETSTATIC, names.className(ct.name), "$TYPE", Translator.TYPE_DESCRIPTOR);
        }
        invoke(opcode, target, name, descriptor, isInterface);
        for (Runnable r : afterCall) {
            r.run();
        }
        afterCall.clear();

        CilType rt = sig.returnType;
        if (rt.kind == CilType.Kind.VOID) {
            return;
        }
        CilType substituted = rt.substitute(typeArgs, ref.methodArgs);
        if (!erased) {
            int k = names.kind(substituted);
            push(k == Val.REF ? Val.ref(substituted, Val.FRESH) : Val.of(k, names.norm(substituted)));
        } else {
            push(fromErased(rt, substituted, structResult ? Val.LOCAL : Val.FRESH));
        }
    }

    private boolean isVirtual(MethodRef ref) {
        MethodDef def = host.universe.resolve(ref);
        return def != null && def.isVirtual();
    }

    private void newObject(MethodRef ref) {
        MethodSig sig = ref.sig;
        if (ref.declaringType.kind == CilType.Kind.ARRAY) {
            newMdArray(ref);
            return;
        }
        String declaring = ref.declaringType.typeName();
        TypeDef def = host.universe.definitionOf(ref.declaringType);
        if (def != null && def.isDelegate()) {
            Val function = pop();
            pop();
            if (function.kind != Val.TOKEN) {
                throw fail("a delegate built from something other than ldftn");
            }
            String c = host.delegateClass(owner, ref.declaringType, asm.methodRef(function.token),
                    function.virtualToken);
            // target -> delegate, delegate, target
            typeInsn(NEW, c);
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKESPECIAL, c, "<init>", "(Ljava/lang/Object;)V", false);
            push(Val.ref(ref.declaringType, Val.HEAP));
            return;
        }
        int n = sig.params.length;
        Val[] values = new Val[n];
        for (int i = n - 1; i >= 0; i--) {
            values[i] = pop();
        }
        int[] fixes = new int[n];
        CilType[] fixTypes = new CilType[n];
        CilType[] typeArgs = ref.typeArgs();
        boolean quiet = host.isNonInterfering(ref);
        for (int i = 0; i < n; i++) {
            argumentFix(sig.params[i], sig.params[i].substitute(typeArgs, null), values[i], true, quiet, i, fixes,
                    fixTypes);
        }
        if (Names.hasHelperClass(declaring)) {
            // new string(char[]) and the like: a factory on the helper class.
            fixArguments(values, fixes, fixTypes);
            invoke(INVOKESTATIC, Names.helperClass(declaring), "New",
                    names.methodDescriptor(sig).replace(")V", ")" + names.descriptor(ref.declaringType)), false);
            push(Val.ref(ref.declaringType, Val.FRESH));
            return;
        }
        String structClass = names.classRef(ref.declaringType);
        if (names.isStruct(ref.declaringType)) {
            // Built in a scratch object the method owns, not a new one: a
            // constructor assigns every field, so nothing of the last use
            // shows through.
            fixArguments(values, fixes, fixTypes);
            loadResultTarget(ref.declaringType);
            String d = names.methodDescriptor(sig);
            invoke(INVOKESTATIC, structClass, "$new",
                    d.substring(0, d.length() - 2) + "L" + structClass + ";)L" + structClass + ";", false);
            push(Val.ref(ref.declaringType, Val.LOCAL));
            return;
        }
        // The arguments are already on the stack and the object has to go
        // under them, so they step aside.
        int[] temps = new int[n];
        for (int i = n - 1; i >= 0; i--) {
            temps[i] = spill(values[i]);
        }
        String c = structClass;
        typeInsn(NEW, c);
        mv.visitInsn(DUP);
        for (int i = 0; i < n; i++) {
            reload(values[i], temps[i]);
            applyFix(fixes[i], fixTypes[i], values[i]);
        }
        invoke(INVOKESPECIAL, c, "<init>", names.methodDescriptor(sig), false);
        push(Val.ref(ref.declaringType, Val.FRESH));
    }

    // ------------------------------------------ arrays of several dimensions

    /// Stack: `count` ints. Leaves one `int[]` holding them, first deepest.
    private void packInts(int count) {
        int[] temps = new int[count];
        for (int i = count - 1; i >= 0; i--) {
            temps[i] = newSlot(1);
            mv.visitVarInsn(ISTORE, temps[i]);
        }
        Jvm.pushInt(mv, count);
        mv.visitIntInsn(NEWARRAY, T_INT);
        for (int i = 0; i < count; i++) {
            mv.visitInsn(DUP);
            Jvm.pushInt(mv, i);
            mv.visitVarInsn(ILOAD, temps[i]);
            mv.visitInsn(IASTORE);
        }
    }

    /// How the indices of an array of this rank travel: two or three ints
    /// as they are, more in an `int[]`.
    private static String mdIndices(int rank) {
        return rank == 2 ? "II" : rank == 3 ? "III" : "[I";
    }

    private void popInts(int count) {
        for (int i = 0; i < count; i++) {
            Val v = pop();
            if (v.kind != Val.I4) {
                throw fail("an array dimension or index that is " + v + ", not an int32");
            }
        }
    }

    /// `new T[a, b]`. CIL spells it as a constructor of the array type.
    ///
    /// The array is one flat JVM array and its dimensions, in the runtime
    /// class for its kind of element (see [Names#mdArrayClass]). For a
    /// primitive the runtime makes both. For anything else the flat array is
    /// made here, where the element class is known, so that a `string[,]`
    /// is over a `String[]` and a reference to one of its elements is what a
    /// `ref string` parameter takes; an array of structs is filled with
    /// objects the way a one-dimensional one is.
    private void newMdArray(MethodRef ref) {
        CilType array = ref.declaringType;
        CilType element = array.element;
        int rank = array.index;
        if (ref.sig.params.length != rank || rank < 2) {
            throw fail("an array created with lower bounds (" + ref + ") is not supported");
        }
        popInts(rank);
        String c = names.mdArrayClass(element);
        String self = "L" + c + ";";
        if (names.norm(element).isPrimitive()) {
            if (rank > 3) {
                packInts(rank);
            }
            invoke(INVOKESTATIC, c, "$new", "(" + mdIndices(rank) + ")" + self, false);
        } else {
            String size = "(" + mdIndices(rank) + ")I";
            String wrap = "([Ljava/lang/Object;Z" + mdIndices(rank) + ")" + self;
            int structs = names.isStruct(element) ? 1 : 0;
            if (rank > 3) {
                packInts(rank);
                int lengths = newSlot(1);
                mv.visitVarInsn(ASTORE, lengths);
                mv.visitVarInsn(ALOAD, lengths);
                invoke(INVOKESTATIC, Names.MD_ARRAY, "size", size, false);
                newArray(element);
                Jvm.pushInt(mv, structs);
                mv.visitVarInsn(ALOAD, lengths);
            } else {
                int[] lengths = new int[rank];
                for (int i = rank - 1; i >= 0; i--) {
                    lengths[i] = newSlot(1);
                    mv.visitVarInsn(ISTORE, lengths[i]);
                }
                for (int i = 0; i < rank; i++) {
                    mv.visitVarInsn(ILOAD, lengths[i]);
                }
                invoke(INVOKESTATIC, Names.MD_ARRAY, "size", size, false);
                newArray(element);
                Jvm.pushInt(mv, structs);
                for (int i = 0; i < rank; i++) {
                    mv.visitVarInsn(ILOAD, lengths[i]);
                }
            }
            invoke(INVOKESTATIC, c, "$wrap", wrap, false);
        }
        push(Val.ref(array, Val.HEAP));
    }

    /// The element just read out of the flat array of a `T[,]` of
    /// references is an `Object` to the verifier; the array was made of the
    /// element's own class, so this cast cannot fail.
    private void castMdElement(CilType element) {
        if (!Names.isTypeVariable(element) && element.kind != CilType.Kind.OBJECT) {
            typeInsn(CHECKCAST, names.classRef(element));
        }
    }

    /// `a[i, j]`, `a[i, j] = v` and `ref a[i, j]`, which CIL spells as calls
    /// to `Get`, `Set` and `Address` on the array type.
    ///
    /// A read or a write is one call to a static method of the array's
    /// runtime class, which checks each index against its own dimension --
    /// the flat array's own bounds check would let `[0, width]` through as
    /// `[1, 0]` -- and indexes the flat array at `i * width + j`. A
    /// reference to an element is the flat array and that same checked
    /// index, the pair every reference to an array element is; to a struct
    /// it is the struct.
    private void mdArrayAccess(MethodRef ref) {
        CilType element = ref.declaringType.element;
        int rank = ref.declaringType.index;
        CilType e = names.norm(element);
        boolean primitive = e.isPrimitive();
        boolean struct = names.isStruct(element);
        String c = names.mdArrayClass(element);
        String self = "L" + c + ";";
        String ed = names.mdElementDescriptor(element);
        String get = "(" + self + mdIndices(rank) + ")" + ed;
        if ("Get".equals(ref.name) || ("Address".equals(ref.name) && struct)) {
            popInts(rank);
            pop();
            if (rank > 3) {
                packInts(rank);
            }
            invoke(INVOKESTATIC, c, "get", get, false);
            if (primitive) {
                maskIfUnsignedByte(e);
                push(Val.of(names.kind(e), e));
            } else {
                castMdElement(element);
                push(Val.ref(element, Val.HEAP));
            }
        } else if ("Set".equals(ref.name)) {
            Val value = pop();
            popInts(rank);
            pop();
            if (struct && value.own != Val.FRESH) {
                // Copy into the element already there, as for T[].
                int held = spill(value);
                if (rank > 3) {
                    packInts(rank);
                }
                invoke(INVOKESTATIC, c, "get", get, false);
                castMdElement(element);
                reload(value, held);
                assignStruct(element);
                return;
            }
            if (rank > 3) {
                int held = spill(value);
                packInts(rank);
                reload(value, held);
            }
            invoke(INVOKESTATIC, c, "set", "(" + self + mdIndices(rank) + ed + ")V", false);
        } else if ("Address".equals(ref.name)) {
            popInts(rank);
            pop();
            int[] indices = new int[rank > 3 ? 1 : rank];
            if (rank > 3) {
                packInts(rank);
                indices[0] = newSlot(1);
                mv.visitVarInsn(ASTORE, indices[0]);
            } else {
                for (int i = rank - 1; i >= 0; i--) {
                    indices[i] = newSlot(1);
                    mv.visitVarInsn(ISTORE, indices[i]);
                }
            }
            int holder = newSlot(1);
            mv.visitVarInsn(ASTORE, holder);
            mv.visitVarInsn(ALOAD, holder);
            host.index.requireField(c, "data", "[" + ed, where());
            mv.visitFieldInsn(GETFIELD, c, "data", "[" + ed);
            if (!primitive && !Names.isTypeVariable(element) && element.kind != CilType.Kind.OBJECT) {
                typeInsn(CHECKCAST, "[" + names.descriptor(element));
            }
            mv.visitVarInsn(ALOAD, holder);
            for (int slot : indices) {
                mv.visitVarInsn(rank > 3 ? ALOAD : ILOAD, slot);
            }
            invoke(INVOKEVIRTUAL, Names.MD_ARRAY, "index", "(" + mdIndices(rank) + ")I", false);
            push(Val.pair(element));
        } else {
            throw fail("call to " + ref + " (a method of an array type) is not supported");
        }
    }

    /// `RuntimeHelpers.InitializeArray(array, fieldHandle)`: an array
    /// literal. The compiler stores the element data in the assembly and
    /// copies it in at run time; here it becomes element stores.
    private void initializeArray() {
        Val handle = pop();
        Val array = pop();
        if (handle.kind != Val.TOKEN || !asm.isFieldToken(handle.token) || array.type == null
                || (array.type.kind != CilType.Kind.SZARRAY && array.type.kind != CilType.Kind.ARRAY)) {
            throw fail("InitializeArray in a form the translator does not recognise");
        }
        FieldRef field = asm.fieldRef(handle.token);
        CilType element = names.norm(array.type.element);
        if (array.type.kind == CilType.Kind.ARRAY) {
            // The literal of an `int[,]` is stored row by row, which is the
            // order of the flat array under it: the same stores, one level in.
            String c = names.mdArrayClass(element);
            String d = "[" + names.mdElementDescriptor(element);
            host.index.requireField(c, "data", d, where());
            mv.visitFieldInsn(GETFIELD, c, "data", d);
        }
        int size = Jvm.byteSize(element);
        int total;
        String typeName = field.type.typeName();
        int marker = typeName == null ? -1 : typeName.indexOf("__StaticArrayInitTypeSize=");
        if (marker >= 0) {
            total = Integer.parseInt(typeName.substring(marker + "__StaticArrayInitTypeSize=".length()));
        } else {
            total = Jvm.byteSize(names.norm(field.type));
        }
        if (size == 0 || field.def == null) {
            throw fail("array literal of " + element + " is not supported");
        }
        byte[] data = asm.fieldData(field.def, total);
        for (int i = 0; i < total / size; i++) {
            long bits = 0;
            for (int b = size - 1; b >= 0; b--) {
                bits = (bits << 8) | (data[i * size + b] & 0xFF);
            }
            if (bits == 0) {
                continue;
            }
            mv.visitInsn(DUP);
            Jvm.pushInt(mv, i);
            switch (element.kind) {
                case I8:
                case U8:
                    mv.visitLdcInsn(Long.valueOf(bits));
                    break;
                case R4:
                    mv.visitLdcInsn(Float.valueOf(Float.intBitsToFloat((int) bits)));
                    break;
                case R8:
                    mv.visitLdcInsn(Double.valueOf(Double.longBitsToDouble(bits)));
                    break;
                case I1:
                    Jvm.pushInt(mv, (byte) bits);
                    break;
                case I2:
                    Jvm.pushInt(mv, (short) bits);
                    break;
                default:
                    Jvm.pushInt(mv, (int) bits);
                    break;
            }
            mv.visitInsn(Jvm.arrayStore(element));
        }
        mv.visitInsn(POP);
    }
}
