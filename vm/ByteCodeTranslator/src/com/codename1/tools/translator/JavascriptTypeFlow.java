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

import com.codename1.tools.translator.bytecodes.Field;
import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.Invoke;
import com.codename1.tools.translator.classfile.Opcodes;
import com.codename1.tools.translator.classfile.Type;
import com.codename1.tools.translator.classfile.analysis.Analyzer;
import com.codename1.tools.translator.classfile.analysis.AnalyzerException;
import com.codename1.tools.translator.classfile.analysis.Frame;
import com.codename1.tools.translator.classfile.analysis.SourceInterpreter;
import com.codename1.tools.translator.classfile.analysis.SourceValue;
import com.codename1.tools.translator.classfile.tree.AbstractInsnNode;
import com.codename1.tools.translator.classfile.tree.FieldInsnNode;
import com.codename1.tools.translator.classfile.tree.InsnNode;
import com.codename1.tools.translator.classfile.tree.InvokeDynamicInsnNode;
import com.codename1.tools.translator.classfile.tree.LdcInsnNode;
import com.codename1.tools.translator.classfile.tree.MethodInsnNode;
import com.codename1.tools.translator.classfile.tree.MethodNode;
import com.codename1.tools.translator.classfile.tree.TryCatchBlockNode;
import com.codename1.tools.translator.classfile.tree.TypeInsnNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Whole-program type flow for the JavaScript suspension analysis: which concrete
/// classes can actually reach each virtual call site, rather than every instantiated
/// class in the receiver's declared cone.
///
/// The cone is what made the generator mass. Every `r.run()` in the program reaches
/// every instantiated `Runnable`, so one blocking `run()` (a dialog's sleep loop, a
/// network wait) makes every `run()` call site a suspension point, those callers make
/// their own signatures suspending, and the classification cascades across the
/// framework -- `Component.getStyle()` and `paint()` ended up generators because
/// `UIManager.ensureBaseTheme()` calls a `Runnable` field that only ever holds a theme
/// installer. This pass follows where each allocation goes instead.
///
/// The analysis is flow- and context-insensitive (a variable-type analysis):
///
/// - Nodes are method parameters, method returns, fields (one node per declaring
///   class and name, shared by every instance) and one node for every array element in
///   the program.
/// - Each method body is summarised once, at parse time, by frame analysis over the
///   raw bytecode: for each value that leaves the method -- an argument, a field or
///   array store, a return -- the set of places it can come from.
/// - Allocations, lambdas and constants seed concrete types; the summaries become
///   subset edges, and the sets are propagated to a fixpoint.
///
/// Soundness is the whole point, because a site classified synchronous that reaches a
/// generator at run time fails. Anything the pass cannot see is TOP, and a TOP receiver
/// keeps today's cone answer: values from natives, the host bridge and JSO callbacks,
/// exceptions, reflection, the parameters of anything the hand-written JavaScript calls,
/// fields of classes with native methods, and everything a method without a summary
/// (synthetic lambda bodies, translator-generated helpers) passes on. `this` is never
/// tracked: it is the instantiated cone of the declaring class, which is exact enough.
final class JavascriptTypeFlow {

    // Summary encoding. Strings, interned, so a method's facts survive until the
    // whole program is loaded without keeping MethodNodes or frames alive.
    //   sources: N<cls> concrete type, P<i> parameter i (0 = this on an instance
    //            method), F<owner>.<name> field read, C<op>|<owner>|<name>|<desc>
    //            call return, A array element, T unknown
    //   sinks:   I<op>|<owner>|<name>|<desc>|<j> argument j of a call (j counts the
    //            receiver of an instance call as 0), F<owner>.<name>, A, R
    private static final String TOP = "T";
    private static final String ARRAY = "A";
    private static final String RETURN = "R";
    private static final String OBJECT = "Njava_lang_Object";

    private static final AbstractInsnNode TOP_NODE = new InsnNode(Opcodes.NOP);
    private static final AbstractInsnNode NULL_NODE = new InsnNode(Opcodes.ACONST_NULL);
    private static final SourceValue ONE = new SourceValue(1);
    private static final SourceValue TWO = new SourceValue(2);
    private static final SourceValue UNKNOWN = new SourceValue(1, TOP_NODE);

    private JavascriptTypeFlow() {
    }

    private static String sanitizeField(String name) {
        return name.replace('$', '_').replace('-', '_');
    }

    private static final class FlowInterpreter extends SourceInterpreter {
        final Map<AbstractInsnNode, String> params = new IdentityHashMap<AbstractInsnNode, String>();
        MethodNode code;

        int indexOf(AbstractInsnNode node) {
            return code.instructions.indexOf(node);
        }

        final Map<TryCatchBlockNode, AbstractInsnNode> handlers = new IdentityHashMap<TryCatchBlockNode, AbstractInsnNode>();
        final int[] argOfLocal;

        FlowInterpreter(MethodNode code) {
            boolean instance = (code.access & Opcodes.ACC_STATIC) == 0;
            argOfLocal = new int[Math.max(code.maxLocals, 1) + 2];
            java.util.Arrays.fill(argOfLocal, -1);
            int local = 0;
            int arg = 0;
            if (instance) {
                argOfLocal[local++] = arg++;
            }
            for (Type t : Type.getArgumentTypes(code.desc)) {
                if (local < argOfLocal.length) {
                    argOfLocal[local] = arg;
                }
                arg++;
                local += t.getSize();
            }
        }

        private static boolean isRef(Type type) {
            return type != null && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY);
        }

        @Override public SourceValue newValue(Type type) {
            if (type == Type.VOID_TYPE) return null;
            if (type == null) return ONE;
            if (isRef(type)) return UNKNOWN;
            return type.getSize() == 2 ? TWO : ONE;
        }

        @Override public SourceValue newParameterValue(boolean isInstanceMethod, int local, Type type) {
            if (!isRef(type) || local >= argOfLocal.length || argOfLocal[local] < 0) {
                return newValue(type);
            }
            AbstractInsnNode sentinel = new InsnNode(Opcodes.NOP);
            params.put(sentinel, "P" + argOfLocal[local]);
            return new SourceValue(1, sentinel);
        }

        @Override public SourceValue newExceptionValue(TryCatchBlockNode block, Frame<SourceValue> handlerFrame, Type type) {
            // One sentinel per handler: a fresh one per visit never lets the frames converge.
            AbstractInsnNode sentinel = handlers.get(block);
            if (sentinel != null) {
                return new SourceValue(1, sentinel);
            }
            sentinel = new InsnNode(Opcodes.NOP);
            handlers.put(block, sentinel);
            params.put(sentinel, "K" + (block.type == null ? "java_lang_Throwable"
                    : JavascriptNameUtil.sanitizeClassName(block.type)));
            return new SourceValue(1, sentinel);
        }

        private static SourceValue field(AbstractInsnNode insn) {
            if (!(insn instanceof FieldInsnNode)) return UNKNOWN;
            FieldInsnNode node = (FieldInsnNode) insn;
            char kind = node.desc.charAt(0);
            if (kind == 'J' || kind == 'D') return TWO;
            if (kind == 'L' || kind == '[') return new SourceValue(1, node);
            return ONE;
        }

        @Override public SourceValue newOperation(AbstractInsnNode node) {
            switch (node.getOpcode()) {
                case Opcodes.NEW: return new SourceValue(1, node);
                case Opcodes.ACONST_NULL: return new SourceValue(1, NULL_NODE);
                case Opcodes.GETSTATIC: return field(node);
                case Opcodes.LCONST_0: case Opcodes.LCONST_1:
                case Opcodes.DCONST_0: case Opcodes.DCONST_1: return TWO;
                case Opcodes.LDC:
                    Object constant = node instanceof LdcInsnNode ? ((LdcInsnNode) node).cst : null;
                    if (constant instanceof Long || constant instanceof Double) return TWO;
                    if (constant instanceof Integer || constant instanceof Float) return ONE;
                    if (constant instanceof String || constant instanceof Type) return new SourceValue(1, node);
                    if (constant instanceof com.codename1.tools.translator.classfile.ConstantDynamic) {
                        char kind = ((com.codename1.tools.translator.classfile.ConstantDynamic) constant).getDescriptor().charAt(0);
                        return kind == 'J' || kind == 'D' ? TWO : kind == 'L' || kind == '[' ? UNKNOWN : ONE;
                    }
                    return UNKNOWN;
                default: return ONE;
            }
        }

        @Override public SourceValue copyOperation(AbstractInsnNode node, SourceValue value) {
            return value;
        }

        @Override public SourceValue unaryOperation(AbstractInsnNode node, SourceValue value) {
            switch (node.getOpcode()) {
                case Opcodes.CHECKCAST:
                    // A cast to a class is a filter node; an array cast passes through.
                    return node instanceof TypeInsnNode && ((TypeInsnNode) node).desc.startsWith("[")
                            ? value : new SourceValue(1, node);
                case Opcodes.GETFIELD: return field(node);
                case Opcodes.NEWARRAY: case Opcodes.ANEWARRAY: return new SourceValue(1, node);
                case Opcodes.LNEG: case Opcodes.DNEG: case Opcodes.I2L: case Opcodes.I2D:
                case Opcodes.L2D: case Opcodes.F2L: case Opcodes.F2D: case Opcodes.D2L: return TWO;
                default: return ONE;
            }
        }

        @Override public SourceValue binaryOperation(AbstractInsnNode node, SourceValue first, SourceValue second) {
            switch (node.getOpcode()) {
                case Opcodes.AALOAD: return new SourceValue(1, node);
                case Opcodes.LALOAD: case Opcodes.DALOAD: case Opcodes.LADD: case Opcodes.DADD:
                case Opcodes.LSUB: case Opcodes.DSUB: case Opcodes.LMUL: case Opcodes.DMUL:
                case Opcodes.LDIV: case Opcodes.DDIV: case Opcodes.LREM: case Opcodes.DREM:
                case Opcodes.LSHL: case Opcodes.LSHR: case Opcodes.LUSHR:
                case Opcodes.LAND: case Opcodes.LOR: case Opcodes.LXOR: return TWO;
                default: return ONE;
            }
        }

        @Override public SourceValue ternaryOperation(AbstractInsnNode node, SourceValue a, SourceValue b, SourceValue c) {
            return ONE;
        }

        @Override public SourceValue naryOperation(AbstractInsnNode node, List<? extends SourceValue> arguments) {
            if (node.getOpcode() == Opcodes.MULTIANEWARRAY) return new SourceValue(1, node);
            String descriptor = node instanceof MethodInsnNode ? ((MethodInsnNode) node).desc
                    : node instanceof InvokeDynamicInsnNode ? ((InvokeDynamicInsnNode) node).desc : null;
            if (descriptor == null) return UNKNOWN;
            char kind = descriptor.charAt(descriptor.indexOf(')') + 1);
            if (kind == 'V') return null;
            if (kind == 'J' || kind == 'D') return TWO;
            if (kind == 'L' || kind == '[') return new SourceValue(1, node);
            return ONE;
        }

        @Override public void returnOperation(AbstractInsnNode node, SourceValue value, SourceValue expected) {
        }
    }

    private static Map<AbstractInsnNode, String> lambdaSites(MethodNode code, List<String> indyLambdas) {
        Map<AbstractInsnNode, String> sites = new IdentityHashMap<AbstractInsnNode, String>();
        int index = 0;
        for (AbstractInsnNode node = code.instructions.getFirst(); node != null; node = node.getNext()) {
            if (node instanceof InvokeDynamicInsnNode) {
                if (index >= indyLambdas.size()) {
                    return null;
                }
                String lambda = indyLambdas.get(index++);
                if (lambda != null) {
                    sites.put(node, lambda);
                }
            }
        }
        return index == indyLambdas.size() ? sites : null;
    }

    private static String callKey(MethodInsnNode m) {
        return m.getOpcode() + "|" + JavascriptNameUtil.sanitizeClassName(m.owner) + "|" + m.name + "|" + m.desc;
    }

    private static String token(AbstractInsnNode source, FlowInterpreter interp, Map<AbstractInsnNode, String> lambdas) {
        return token(source, interp, lambdas, null);
    }

    private static String token(AbstractInsnNode source, FlowInterpreter interp, Map<AbstractInsnNode, String> lambdas,
            MethodNode code) {
        if (source == NULL_NODE) return null;
        if (source == TOP_NODE) return TOP;
        String param = interp.params.get(source);
        if (param != null) return param;
        switch (source.getOpcode()) {
            case Opcodes.NEW:
                return "N" + JavascriptNameUtil.sanitizeClassName(((TypeInsnNode) source).desc);
            case Opcodes.NEWARRAY: case Opcodes.ANEWARRAY: case Opcodes.MULTIANEWARRAY:
                return OBJECT;
            case Opcodes.LDC:
                return ((LdcInsnNode) source).cst instanceof String ? "Njava_lang_String" : "Njava_lang_Class";
            case Opcodes.GETFIELD: case Opcodes.GETSTATIC: {
                FieldInsnNode f = (FieldInsnNode) source;
                return "F" + JavascriptNameUtil.sanitizeClassName(f.owner) + "." + sanitizeField(f.name);
            }
            case Opcodes.AALOAD:
                return ARRAY;
            case Opcodes.CHECKCAST:
                return castKey((TypeInsnNode) source, interp);
            default:
                break;
        }
        if (source instanceof MethodInsnNode) {
            return "C" + callKey((MethodInsnNode) source) + "@" + interp.indexOf(source);
        }
        if (source instanceof InvokeDynamicInsnNode) {
            String lambda = lambdas.get(source);
            if (lambda != null) return "N" + JavascriptNameUtil.sanitizeClassName(lambda);
            String desc = ((InvokeDynamicInsnNode) source).desc;
            if (desc.endsWith(")Ljava/lang/String;")) return "Njava_lang_String";
            return declared(Type.getReturnType(desc));
        }
        return TOP;
    }

    private static String castKey(TypeInsnNode cast, FlowInterpreter interp) {
        return "X" + JavascriptNameUtil.sanitizeClassName(cast.desc) + "@" + interp.indexOf(cast);
    }

    /// "Anything assignable to this declared type": K<cls>, or T for Object.
    static String declared(Type t) {
        if (t.getSort() == Type.ARRAY) return OBJECT;
        if (t.getSort() != Type.OBJECT || "java/lang/Object".equals(t.getInternalName())) return TOP;
        return "K" + JavascriptNameUtil.sanitizeClassName(t.getInternalName());
    }

    private static String[] flow(String sink, SourceValue value, FlowInterpreter interp,
            Map<AbstractInsnNode, String> lambdas) {
        Set<String> sources = new LinkedHashSet<String>();
        if (value.insns.isEmpty()) {
            sources.add(TOP);
        }
        for (AbstractInsnNode source : value.insns) {
            String t = token(source, interp, lambdas);
            if (t != null) sources.add(t);
        }
        if (sources.isEmpty()) {
            return null;
        }
        String[] out = new String[sources.size() + 1];
        out[0] = sink.intern();
        int i = 1;
        for (String s : sources) out[i++] = s.intern();
        return out;
    }

    /// Records the method's flow summary on `method`, and each virtual call's receiver
    /// sources on its Invoke. A method that cannot be analysed is left without a
    /// summary, which the solver treats conservatively.
    static void capture(String owner, MethodNode code, Map<AbstractInsnNode, Invoke> invokes,
            BytecodeMethod method, List<String> indyLambdas) {
        if (code == null || method == null
                || (code.access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) != 0) {
            return;
        }
        Map<AbstractInsnNode, String> lambdas = lambdaSites(code, indyLambdas);
        if (lambdas == null) {
            return;
        }
        FlowInterpreter interp = new FlowInterpreter(code);
        interp.code = code;
        Frame<SourceValue>[] frames;
        try {
            frames = new Analyzer<SourceValue>(interp).analyze(owner, code);
        } catch (AnalyzerException error) {
            return;
        } catch (RuntimeException error) {
            return;
        }
        List<String[]> flows = new ArrayList<String[]>();
        for (int i = 0; i < code.instructions.size(); i++) {
            Frame<SourceValue> frame = frames[i];
            if (frame == null) {
                continue;
            }
            AbstractInsnNode insn = code.instructions.get(i);
            int op = insn.getOpcode();
            int top = frame.getStackSize() - 1;
            switch (op) {
                case Opcodes.PUTFIELD: case Opcodes.PUTSTATIC: {
                    FieldInsnNode f = (FieldInsnNode) insn;
                    char kind = f.desc.charAt(0);
                    if (kind == 'L' || kind == '[') {
                        add(flows, flow("F" + JavascriptNameUtil.sanitizeClassName(f.owner) + "." + sanitizeField(f.name),
                                frame.getStack(top), interp, lambdas));
                    }
                    break;
                }
                case Opcodes.CHECKCAST:
                    if (!((TypeInsnNode) insn).desc.startsWith("[")) {
                        add(flows, flow(castKey((TypeInsnNode) insn, interp), frame.getStack(top), interp, lambdas));
                    }
                    break;
                case Opcodes.INVOKEDYNAMIC: {
                    String lambda = lambdas.get(insn);
                    if (lambda == null) break;
                    Type[] args = Type.getArgumentTypes(((InvokeDynamicInsnNode) insn).desc);
                    int base = top - args.length + 1;
                    String cls = JavascriptNameUtil.sanitizeClassName(lambda);
                    for (int a = 0; a < args.length; a++) {
                        if (!isRef(args[a])) continue;
                        add(flows, flow(lambdaField(cls, a), frame.getStack(base + a), interp, lambdas));
                    }
                    break;
                }
                case Opcodes.AASTORE:
                    add(flows, flow(ARRAY, frame.getStack(top), interp, lambdas));
                    break;
                case Opcodes.ARETURN:
                    add(flows, flow(RETURN, frame.getStack(top), interp, lambdas));
                    break;
                case Opcodes.INVOKEVIRTUAL: case Opcodes.INVOKEINTERFACE:
                case Opcodes.INVOKESPECIAL: case Opcodes.INVOKESTATIC: {
                    MethodInsnNode m = (MethodInsnNode) insn;
                    Type[] args = Type.getArgumentTypes(m.desc);
                    boolean instance = op != Opcodes.INVOKESTATIC;
                    int base = top - args.length + 1;
                    String key = callKey(m);
                    for (int a = 0; a < args.length; a++) {
                        int sort = args[a].getSort();
                        if (sort != Type.OBJECT && sort != Type.ARRAY) continue;
                        add(flows, flow("I" + key + "|" + (instance ? a + 1 : a) + "@" + i, frame.getStack(base + a), interp, lambdas));
                    }
                    if (op == Opcodes.INVOKEVIRTUAL || op == Opcodes.INVOKEINTERFACE) {
                        add(flows, flow("I" + key + "|0@" + i, frame.getStack(base - 1), interp, lambdas));
                        Invoke target = invokes.get(insn);
                        if (target != null) {
                            String[] recv = flow("", frame.getStack(base - 1), interp, lambdas);
                            if (recv != null) {
                                target.setFlowReceiverSources(java.util.Arrays.copyOfRange(recv, 1, recv.length));
                            }
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }
        method.setTypeFlowSummary(flows.toArray(new String[flows.size()][]));
    }

    /// The summaries of a lambda class the parser synthesised. Its captures arrive
    /// straight from the `invokedynamic` site into the `arg$<n>` fields (see
    /// `capture`), so the constructor and factory move nothing; the interface
    /// method hands the captures and its own parameters to the implementation
    /// method, whose invoke is the last one `sam` holds.
    static void summariseLambda(String lambdaClass, BytecodeMethod ctor, BytecodeMethod factory,
            BytecodeMethod sam, Type[] captured, int implOpcode, String implOwner, String implName,
            String implDesc, boolean ctorRef) {
        String[][] none = new String[0][];
        ctor.setTypeFlowSummary(none);
        factory.setTypeFlowSummary(none);
        List<Instruction> insns = sam.getInstructions();
        int at = insns.size() - 1;
        while (at >= 0 && !(insns.get(at) instanceof Invoke)) at--;
        if (at < 0) {
            return;
        }
        Invoke impl = (Invoke) insns.get(at);
        Type[] samArgs = Type.getArgumentTypes(sam.getSignature());
        List<String> values = new ArrayList<String>();
        for (int i = 0; i < captured.length; i++) {
            values.add(isRef(captured[i]) ? lambdaField(lambdaClass, i) : null);
        }
        for (int i = 0; i < samArgs.length; i++) {
            values.add(isRef(samArgs[i]) ? "P" + (i + 1) : null);
        }
        boolean instance = !ctorRef && implOpcode != Opcodes.INVOKESTATIC;
        String key = implOpcode + "|" + JavascriptNameUtil.sanitizeClassName(implOwner) + "|" + implName + "|" + implDesc;
        List<String[]> flows = new ArrayList<String[]>();
        for (int k = 0; k < values.size(); k++) {
            String v = values.get(k);
            if (v == null) continue;
            int j = ctorRef ? k + 1 : k;
            flows.add(new String[] {("I" + key + "|" + j + "@" + at).intern(), v});
        }
        if (instance && !values.isEmpty() && values.get(0) != null
                && (implOpcode == Opcodes.INVOKEVIRTUAL || implOpcode == Opcodes.INVOKEINTERFACE)) {
            impl.setFlowReceiverSources(new String[] {values.get(0)});
        }
        if (isRef(Type.getReturnType(sam.getSignature()))) {
            flows.add(new String[] {RETURN, ctorRef ? "N" + JavascriptNameUtil.sanitizeClassName(implOwner)
                    : isRef(Type.getReturnType(implDesc)) ? "C" + key + "@" + at : TOP});
        }
        sam.setTypeFlowSummary(flows.toArray(new String[flows.size()][]));
    }

    private static boolean isRef(Type t) {
        return t.getSort() == Type.OBJECT || t.getSort() == Type.ARRAY;
    }

    static String lambdaField(String lambdaClass, int i) {
        return "F" + lambdaClass + "." + sanitizeField("arg$" + (i + 1));
    }

    private static void add(List<String[]> flows, String[] flow) {
        if (flow != null) flows.add(flow);
    }

    // ------------------------------------------------------------------ solver

    /// Answers the analysis needs from its own resolver, so the flow edges for a
    /// direct call land on exactly the body the propagation edges do.
    interface Resolver {
        BytecodeMethod resolveDirect(String owner, String name, String desc);
    }

    /// One call instruction. A virtual one is linked to the bodies its receiver
    /// types reach as those types are discovered, so an argument only flows into
    /// the implementations that can really receive it.
    private static final class Site {
        int op;
        String owner;
        String name;
        String desc;
        int recv = -1;
        int[] args;
        int ret = -1;
        boolean all;
        boolean host;
        final BitSet seen = new BitSet();
        final Set<BytecodeMethod> linked = Collections.newSetFromMap(new IdentityHashMap<BytecodeMethod, Boolean>());
    }

    private static final String DUMP = System.getProperty("parparvm.js.typeflow.dump");
    private static final String STORAGE = "java_util_NativeStorage";

    private final Map<String, ByteCodeClass> byName = new HashMap<String, ByteCodeClass>();
    private final Map<String, Integer> classIndex = new HashMap<String, Integer>();
    private final List<String> classNames = new ArrayList<String>();
    private final Map<String, Integer> nodeIds = new HashMap<String, Integer>();
    private final List<String> nodeNames = new ArrayList<String>();
    private final List<BitSet> sets = new ArrayList<BitSet>();
    private final List<List<Integer>> succ = new ArrayList<List<Integer>>();
    private final Map<Integer, Site> sitesByRecv = new HashMap<Integer, Site>();
    private final Map<String, Site> sites = new HashMap<String, Site>();
    private final Map<BytecodeMethod, Integer> methodIds = new IdentityHashMap<BytecodeMethod, Integer>();
    private final List<BytecodeMethod> methodsById = new ArrayList<BytecodeMethod>();
    private final Map<String, BitSet> coneBits = new HashMap<String, BitSet>();
    private final Map<String, String> fieldOwnerMemo = new HashMap<String, String>();
    private final Map<String, List<BytecodeMethod>> sigImpls = new HashMap<String, List<BytecodeMethod>>();
    private final Map<Integer, String> topWhy = new HashMap<Integer, String>();
    private final Deque<Integer> work = new ArrayDeque<Integer>();
    private boolean[] queued = new boolean[1024];
    private JavascriptReachability.Model rta;
    private Resolver resolver;
    private Set<String> jsoClasses;
    private Set<String> externalTokens;
    private Set<String> replacedTokens;
    private final Set<String> jsoSigs = new HashSet<String>();
    private final BitSet jsoBits = new BitSet();
    private String seedLabel = "?";
    private static final StringBuilder dbg = new StringBuilder();
    private int topBit;
    private int arrayNode;

    int refinedSites;
    int narrowedSites;
    int topSites;

    /// Runs the whole-program solve and stores, on every virtual Invoke whose receiver
    /// can be bounded, the bodies it can reach. Returns the solver for statistics.
    static JavascriptTypeFlow solve(List<ByteCodeClass> classes, JavascriptReachability.Model rta,
            Resolver resolver, Set<String> jsoClasses) {
        JavascriptTypeFlow tf = new JavascriptTypeFlow();
        dbg.setLength(0);
        tf.rta = rta;
        tf.resolver = resolver;
        tf.jsoClasses = hostClasses(classes, jsoClasses);
        tf.externalTokens = JavascriptBundleWriter.collectBridgeReferencedCn1Tokens();
        tf.replacedTokens = JavascriptBundleWriter.collectBridgeReplacedCn1Tokens();
        tf.run(classes);
        return tf;
    }

    /// The JSO types whose objects the host creates: interfaces and abstract
    /// classes. A concrete Java class that implements a JSO interface (a canvas
    /// recorder, say) is an ordinary Java object -- `new`ed by Java, entered only
    /// from Java -- and treating it as host-made types every parameter of even its
    /// private helpers as unknown.
    private static Set<String> hostClasses(List<ByteCodeClass> classes, Set<String> jso) {
        Set<String> out = new HashSet<String>();
        if (jso == null) return out;
        for (ByteCodeClass c : classes) {
            if (jso.contains(c.getClsName()) && (c.isIsInterface() || c.isIsAbstract())) out.add(c.getClsName());
        }
        return out;
    }

    // ---- nodes and sets

    private int classBit(String cls) {
        Integer i = classIndex.get(cls);
        if (i == null) {
            i = Integer.valueOf(classNames.size());
            classNames.add(cls);
            classIndex.put(cls, i);
            if (jsoClasses != null && jsoClasses.contains(cls)) {
                jsoBits.set(i.intValue());
            }
        }
        return i.intValue();
    }

    private int node(String key) {
        Integer id = nodeIds.get(key);
        if (id == null) {
            id = Integer.valueOf(sets.size());
            nodeIds.put(key, id);
            nodeNames.add(key);
            sets.add(new BitSet());
            succ.add(null);
        }
        return id.intValue();
    }

    private int methodId(BytecodeMethod m) {
        Integer id = methodIds.get(m);
        if (id == null) {
            id = Integer.valueOf(methodsById.size());
            methodIds.put(m, id);
            methodsById.add(m);
        }
        return id.intValue();
    }

    /// A checked cast: what reaches it, kept only where it is assignable to the
    /// cast type -- and "anything" becomes the cast type's cone. A value that fails
    /// the cast never gets past it.
    private int castNode(BytecodeMethod m, String key) {
        int n = node("X" + methodId(m) + key);
        if (!filters.containsKey(Integer.valueOf(n))) {
            BitSet c = cone(key.substring(1, key.lastIndexOf('@')));
            // An unknown cast type filters nothing.
            filters.put(Integer.valueOf(n), c.get(topBit) ? null : c);
        }
        return n;
    }

    private final Map<Integer, BitSet> filters = new HashMap<Integer, BitSet>();

    private int paramNode(BytecodeMethod m, int i) {
        return node("P" + methodId(m) + "#" + i);
    }

    private int returnNode(BytecodeMethod m) {
        return node("R" + methodId(m));
    }

    private void enqueue(int n) {
        if (n >= queued.length) {
            queued = java.util.Arrays.copyOf(queued, Math.max(n + 1, queued.length * 2));
        }
        if (!queued[n]) {
            queued[n] = true;
            work.add(Integer.valueOf(n));
        }
    }

    private void edge(int from, int to) {
        if (from == to) return;
        List<Integer> s = succ.get(from);
        if (s == null) {
            s = new ArrayList<Integer>(2);
            succ.set(from, s);
        }
        s.add(Integer.valueOf(to));
        if (!sets.get(from).isEmpty()) enqueue(from);
    }

    private void top(int node) {
        BitSet b = sets.get(node);
        if (b.get(topBit)) return;
        BitSet filter = filters.get(Integer.valueOf(node));
        if (filter != null) {
            addBits(node, filter);
            return;
        }
        if (DUMP != null) topWhy.put(Integer.valueOf(node), seedLabel);
        b.clear();
        b.set(topBit);
        enqueue(node);
    }

    private void addBits(int node, BitSet bits) {
        if (bits.get(topBit)) {
            top(node);
            return;
        }
        BitSet b = sets.get(node);
        if (b.get(topBit)) return;
        BitSet filter = filters.get(Integer.valueOf(node));
        if (filter != null) {
            // A copy without clone(), which JavaAPI's BitSet does not expose.
            BitSet copy = new BitSet();
            copy.or(bits);
            copy.and(filter);
            bits = copy;
        }
        int before = b.cardinality();
        b.or(bits);
        if (b.cardinality() != before) enqueue(node);
    }

    private void addBit(int node, int bit) {
        BitSet b = sets.get(node);
        if (b.get(topBit) || b.get(bit)) return;
        BitSet filter = filters.get(Integer.valueOf(node));
        if (filter != null && !filter.get(bit)) return;
        b.set(bit);
        enqueue(node);
    }

    /// The instantiated concrete classes in `cls`'s declared cone, plus the JSO
    /// types in it (host objects, never allocated in Java), as bits.
    private BitSet cone(String cls) {
        BitSet b = coneBits.get(cls);
        if (b != null) return b;
        b = new BitSet();
        Set<String> types = rta.coneTypes(cls);
        if (types == null) {
            b.set(topBit);
        } else {
            for (String t : types) {
                ByteCodeClass c = byName.get(t);
                if (c == null) continue;
                if (jsoClasses.contains(t) || (!c.isIsInterface() && !c.isIsAbstract())) b.set(classBit(t));
            }
        }
        coneBits.put(cls, b);
        return b;
    }


    private BitSet bitsOf(String d) {
        if (TOP.equals(d)) {
            BitSet b = new BitSet();
            b.set(topBit);
            return b;
        }
        if (d.charAt(0) == 'N') {
            BitSet b = new BitSet();
            b.set(classBit(d.substring(1)));
            return b;
        }
        return cone(d.substring(1));
    }

    /// Seeds `node` with everything a value of declared type `t` can be.
    private void typed(int node, Type t) {
        if (t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) return;
        addBits(node, bitsOf(declared(t)));
    }

    private void typedParams(BytecodeMethod m) {
        Type[] args = Type.getArgumentTypes(m.getSignature());
        int off = m.isStatic() ? 0 : 1;
        for (int i = 0; i < args.length; i++) typed(paramNode(m, i + off), args[i]);
    }

    // ---- program facts

    /// The class that declares field `name` as seen from `owner`: superclasses, then
    /// interfaces (a static constant can live on one).
    private String fieldOwner(String owner, String name) {
        String key = owner + "." + name;
        String memo = fieldOwnerMemo.get(key);
        if (memo != null) return memo;
        String found = findField(owner, name, new HashSet<String>());
        String result = found == null ? owner : found;
        fieldOwnerMemo.put(key, result);
        return result;
    }

    private String findField(String cls, String name, Set<String> seen) {
        if (cls == null || !seen.add(cls)) return null;
        ByteCodeClass c = byName.get(cls);
        if (c == null) return null;
        for (ByteCodeField f : c.getFields()) {
            if (sanitizeField(f.getFieldName()).equals(name)) return cls;
        }
        String base = c.getBaseClass();
        String r = base == null ? null : findField(JavascriptNameUtil.sanitizeClassName(base), name, seen);
        if (r != null) return r;
        if (c.getBaseInterfaces() != null) {
            for (String i : c.getBaseInterfaces()) {
                r = findField(JavascriptNameUtil.sanitizeClassName(i), name, seen);
                if (r != null) return r;
            }
        }
        return null;
    }

    private int fieldNode(String ref) {
        int dot = ref.lastIndexOf('.');
        String owner = fieldOwner(ref.substring(1, dot), ref.substring(dot + 1));
        return node("F" + owner + "." + ref.substring(dot + 1));
    }

    private static boolean hasNatives(ByteCodeClass c) {
        for (BytecodeMethod m : c.getMethods()) {
            if (m.isNative()) return true;
        }
        return false;
    }

    /// The bodies a call can enter judged by the cone alone. Null means "cannot say".
    private List<BytecodeMethod> coneTargets(int op, String owner, String name, String desc) {
        if (op == Opcodes.INVOKESTATIC || op == Opcodes.INVOKESPECIAL) {
            BytecodeMethod t = resolver.resolveDirect(owner, name, desc);
            return t == null ? null : Collections.singletonList(t);
        }
        List<BytecodeMethod> impls = rta.resolveImpls(owner, name, desc);
        return impls != null ? impls : sigImpls.get(name + desc);
    }

    private boolean isExternallyEntered(ByteCodeClass c, BytecodeMethod m) {
        if (jsoClasses.contains(c.getClsName())) return true;
        String full = JavascriptNameUtil.methodIdentifier(c.getClsName(), m.getMethodName(), m.getSignature());
        if (externalTokens.contains(full) || externalTokens.contains(full + "__impl")) return true;
        if (!m.isStatic() && !m.isConstructor()) {
            // A Java class implementing a JSO interface is itself a JSO bridge class
            // (checked above); the signature alone says nothing about other classes.
            // A class-free dispatch id names every override, so it can only reach a
            // method dispatch can select -- never a private one.
            if (!m.isPrivate() && externalTokens.contains(JavascriptNameUtil.dispatchMethodIdentifier(m.getMethodName(), m.getSignature()))) {
                return true;
            }
        }
        return "main".equals(m.getMethodName()) && m.isStatic();
    }

    private boolean returnsUnknown(ByteCodeClass c, BytecodeMethod m) {
        if (m.isNative() || jsoClasses.contains(c.getClsName())) return true;
        // The bridge's java.util replacements are fast paths with the Java body's
        // semantics (a map lookup still returns what was put), so the body's own
        // summary describes them.
        if (c.getClsName().startsWith("java_util_")) return false;
        String full = JavascriptNameUtil.methodIdentifier(c.getClsName(), m.getMethodName(), m.getSignature());
        if (replacedTokens.contains(full) || replacedTokens.contains(full + "__impl")) return true;
        return !m.isStatic() && !m.isConstructor()
                && replacedTokens.contains(JavascriptNameUtil.dispatchMethodIdentifier(m.getMethodName(), m.getSignature()));
    }

    /// A signature the host can answer with a JavaScript override when the receiver
    /// is a host object, or that the bridge replaces outright.
    private boolean hostSignature(String name, String desc) {
        return jsoSigs.contains(name + desc)
                || replacedTokens.contains(JavascriptNameUtil.dispatchMethodIdentifier(name, desc));
    }

    // ---- sites

    /// {@code s.split("\\|")} without a regex, which JavaAPI's String cannot take.
    private static String[] splitBars(String s) {
        List<String> parts = new ArrayList<String>();
        int from = 0;
        for (int bar = s.indexOf('|'); bar >= 0; bar = s.indexOf('|', from)) {
            parts.add(s.substring(from, bar));
            from = bar + 1;
        }
        parts.add(s.substring(from));
        // split() drops trailing empty strings.
        int n = parts.size();
        while (n > 0 && parts.get(n - 1).isEmpty()) {
            n--;
        }
        return parts.subList(0, n).toArray(new String[n]);
    }

    private Site site(BytecodeMethod m, String key) {
        int at = key.lastIndexOf('@');
        String id = methodId(m) + key.substring(at);
        Site st = sites.get(id);
        if (st != null) return st;
        String[] p = splitBars(key.substring(0, at));
        st = new Site();
        st.op = Integer.parseInt(p[0]);
        st.owner = p[1];
        st.name = p[2];
        st.desc = p[3];
        st.args = new int[Type.getArgumentTypes(st.desc).length + 1];
        java.util.Arrays.fill(st.args, -1);
        st.ret = node("S" + id);
        sites.put(id, st);
        if (st.op == Opcodes.INVOKESTATIC || st.op == Opcodes.INVOKESPECIAL) {
            BytecodeMethod t = resolver.resolveDirect(st.owner, st.name, st.desc);
            if (t == null) {
                typed(st.ret, Type.getReturnType(st.desc));
            } else {
                link(st, t);
            }
        } else {
            st.recv = node("V" + id);
            sitesByRecv.put(Integer.valueOf(st.recv), st);
        }
        return st;
    }

    private int argNode(Site st, int j) {
        if (j == 0 && st.recv >= 0) return st.recv;
        if (st.args[j] < 0) {
            st.args[j] = node("a" + st.ret + "#" + j);
            for (BytecodeMethod t : st.linked) {
                edge(st.args[j], paramNode(t, j));
            }
        }
        return st.args[j];
    }

    private void link(Site st, BytecodeMethod t) {
        if (!st.linked.add(t)) return;
        for (int j = 1; j < st.args.length; j++) {
            if (st.args[j] >= 0) edge(st.args[j], paramNode(t, j));
        }
        if (st.op == Opcodes.INVOKESTATIC && st.args.length > 0 && st.args[0] >= 0) {
            edge(st.args[0], paramNode(t, 0));
        }
        edge(returnNode(t), st.ret);
    }

    private void linkAll(Site st) {
        if (st.all) return;
        st.all = true;
        seedLabel = "unbounded receiver of " + st.owner + "." + st.name;
        List<BytecodeMethod> ts = coneTargets(st.op, st.owner, st.name, st.desc);
        if (ts == null || hostSignature(st.name, st.desc)) {
            typed(st.ret, Type.getReturnType(st.desc));
        }
        if (ts != null) {
            for (BytecodeMethod t : ts) link(st, t);
        }
    }

    /// The receiver of a virtual site gained types: link the bodies they reach.
    private void resolveSite(Site st) {
        BitSet types = sets.get(st.recv);
        if (st.all) return;
        if (types.get(topBit)) {
            linkAll(st);
            return;
        }
        Set<String> cone = rta.coneTypes(st.owner);
        if (cone == null) {
            linkAll(st);
            return;
        }
        for (int b = types.nextSetBit(0); b >= 0; b = types.nextSetBit(b + 1)) {
            if (st.seen.get(b)) continue;
            st.seen.set(b);
            String t = classNames.get(b);
            if (!cone.contains(t)) continue;
            if (jsoBits.get(b)) {
                if (!st.host) {
                    st.host = true;
                    seedLabel = "host receiver of " + st.owner + "." + st.name;
                    typed(st.ret, Type.getReturnType(st.desc));
                }
                continue;
            }
            ByteCodeClass cls = byName.get(t);
            if (cls == null) {
                linkAll(st);
                return;
            }
            // Never allocated: a type the flow carries (a declared NEW in code no
            // path runs, say) but no object of which can reach this call.
            if (cls.isIsInterface() || cls.isIsAbstract() || !rta.isInstantiated(t)) continue;
            BytecodeMethod impl = rta.resolveOn(t, st.name, st.desc);
            if (impl == null || impl.isEliminated()) {
                linkAll(st);
                return;
            }
            link(st, impl);
        }
    }

    // ---- building

    private void run(List<ByteCodeClass> classes) {
        topBit = classBit("<top>");
        for (ByteCodeClass c : classes) {
            byName.put(c.getClsName(), c);
        }
        for (ByteCodeClass c : classes) {
            for (BytecodeMethod m : c.getMethods()) {
                if (m.isEliminated() || m.isStatic() || m.isConstructor() || m.isAbstract()) continue;
                String sig = m.getMethodName() + m.getSignature();
                List<BytecodeMethod> l = sigImpls.get(sig);
                if (l == null) {
                    l = new ArrayList<BytecodeMethod>();
                    sigImpls.put(sig, l);
                }
                l.add(m);
            }
            if (jsoClasses.contains(c.getClsName())) {
                classBit(c.getClsName());
                for (BytecodeMethod m : c.getMethods()) {
                    if (!m.isStatic() && !m.isConstructor()) jsoSigs.add(m.getMethodName() + m.getSignature());
                }
            }
        }
        arrayNode = node(ARRAY);
        for (ByteCodeClass c : classes) {
            boolean jso = jsoClasses.contains(c.getClsName());
            if (jso || hasNatives(c)) {
                seedLabel = "native-class field " + c.getClsName();
                for (ByteCodeField f : c.getFields()) {
                    String d = f.getDescriptor();
                    // A native writes a field through its property name, so only a
                    // field the bridge names can hold what it put there. Typing
                    // every field of a class with one native made a graphics
                    // context's listener field hold every Runnable in the program.
                    if (!jso && !externalTokens.contains("cn1_" + c.getClsName() + "_" + f.getFieldName())) {
                        continue;
                    }
                    if (d != null && (d.startsWith("L") || d.startsWith("["))) {
                        typed(node("F" + c.getClsName() + "." + sanitizeField(f.getFieldName())), Type.getType(d));
                    }
                }
            }
            for (BytecodeMethod m : c.getMethods()) {
                if (m.isEliminated()) continue;
                seedLabel = "external " + c.getClsName() + "." + m.getMethodName();
                // A native has no body to enter: the bridge naming it implements
                // it, and what flows into its parameters is handled below.
                if (!m.isNative() && isExternallyEntered(c, m)) {
                    typedParams(m);
                }
                if (m.isNative() && (STORAGE.equals(c.getClsName())
                        || c.getClsName().startsWith("java_util_") && !c.getClsName().startsWith("java_util_concurrent"))) {
                    // The backing store of every collection: what goes in comes out,
                    // as with an array element.
                    Type[] args = Type.getArgumentTypes(m.getSignature());
                    // A java.util native (a map's get, a set's element at) only hands back
                    // what the collection was given, which is what the store holds.
                    int off = m.isStatic() ? 0 : 1;
                    for (int i = 0; i < args.length; i++) {
                        if (args[i].getSort() == Type.OBJECT) {
                            edge(paramNode(m, i + off), arrayNode);
                        }
                    }
                    Type r = Type.getReturnType(m.getSignature());
                    if (r.getSort() == Type.OBJECT && "java/lang/Object".equals(r.getInternalName())) {
                        edge(arrayNode, returnNode(m));
                    } else if (r.getSort() == Type.OBJECT) {
                        typed(returnNode(m), r);
                    } else if (r.getSort() == Type.ARRAY) {
                        addBit(returnNode(m), classBit("java_lang_Object"));
                    }
                } else if (returnsUnknown(c, m)) {
                    seedLabel = "host-return " + c.getClsName() + "." + m.getMethodName();
                    typed(returnNode(m), Type.getReturnType(m.getSignature()));
                }
                if (m.isNative()) {
                    seedLabel = "native arrays " + c.getClsName() + "." + m.getMethodName();
                    seedNativeArrays(m);
                    continue;
                }
                if (m.isAbstract()) continue;
                String[][] summary = m.getTypeFlowSummary();
                if (summary == null) {
                    if (m.getInstructions() != null && !m.getInstructions().isEmpty()) {
                        seedLabel = "unsummarised " + c.getClsName() + "." + m.getMethodName();
                        if (DUMP != null) dbg.append("UNSUMMARISED ").append(c.getClsName()).append('.').append(m.getMethodName()).append(m.getSignature()).append('\n');
                        conservative(m);
                    }
                    continue;
                }
                for (String[] flow : summary) {
                    if (DUMP != null) seedLabel = "in " + c.getClsName() + "." + m.getMethodName() + " " + java.util.Arrays.toString(flow);
                    addFlow(c, m, flow);
                }
            }
        }
        propagate();
        annotateSites(classes);
    }

    /// A native that hands out a reference array can put anything of the element
    /// type into it, and an array element is one shared node. Arrays passed IN are
    /// read by the host (call arguments, flush batches), never filled with Java
    /// references, so only the return type seeds.
    private void seedNativeArrays(BytecodeMethod m) {
        Type t = Type.getReturnType(m.getSignature());
        if (t.getSort() != Type.ARRAY) return;
        Type e = t.getElementType();
        if (e.getSort() == Type.OBJECT) {
            addBits(arrayNode, bitsOf(declared(e)));
        } else if (t.getDimensions() > 1) {
            addBit(arrayNode, classBit("java_lang_Object"));
        }
    }

    /// A body without a summary: everything it passes on can be anything of its
    /// declared type.
    private void conservative(BytecodeMethod m) {
        typed(returnNode(m), Type.getReturnType(m.getSignature()));
        for (Instruction ins : m.getInstructions()) {
            if (ins instanceof Invoke) {
                Invoke inv = (Invoke) ins;
                String owner = JavascriptNameUtil.sanitizeClassName(inv.getOwner());
                List<BytecodeMethod> ts = coneTargets(inv.getOpcode(), owner, inv.getName(), inv.getDesc());
                if (ts != null) {
                    if (DUMP != null) seedLabel = "unsummarised " + m.getClsName() + "." + m.getMethodName() + " calls " + inv.getName();
                    for (BytecodeMethod t : ts) typedParams(t);
                }
            } else if (ins instanceof Field) {
                Field f = (Field) ins;
                if ((f.getOpcode() == Opcodes.PUTFIELD || f.getOpcode() == Opcodes.PUTSTATIC)
                        && f.getDesc() != null && (f.getDesc().startsWith("L") || f.getDesc().startsWith("["))) {
                    typed(fieldNode("F" + JavascriptNameUtil.sanitizeClassName(f.getOwner()) + "."
                            + sanitizeField(f.getFieldName())), Type.getType(f.getDesc()));
                }
            } else if (ins.getOpcode() == Opcodes.AASTORE) {
                top(arrayNode);
            }
        }
    }

    private int sinkNode(BytecodeMethod m, String sink) {
        if (RETURN.equals(sink)) return returnNode(m);
        if (ARRAY.equals(sink)) return arrayNode;
        if (sink.charAt(0) == 'F') return fieldNode(sink);
        if (sink.charAt(0) == 'X') return castNode(m, sink);
        // I<op>|<owner>|<name>|<desc>|<j>@<i>
        int at = sink.lastIndexOf('@');
        int bar = sink.lastIndexOf('|', at);
        int j = Integer.parseInt(sink.substring(bar + 1, at));
        Site st = site(m, sink.substring(1, bar) + sink.substring(at));
        return argNode(st, j);
    }

    /// The node a source reads, or -1 for a constant (then `constant` holds its bits).
    private int sourceNode(ByteCodeClass c, BytecodeMethod m, String src, BitSet constant) {
        switch (src.charAt(0)) {
            case 'T':
                constant.set(topBit);
                return -1;
            case 'N':
                constant.set(classBit(src.substring(1)));
                return -1;
            case 'K':
                constant.or(cone(src.substring(1)));
                return -1;
            case 'A':
                return arrayNode;
            case 'F':
                return fieldNode(src);
            case 'P': {
                int i = Integer.parseInt(src.substring(1));
                if (i == 0 && !m.isStatic()) {
                    constant.or(cone(c.getClsName()));
                    return -1;
                }
                return paramNode(m, i);
            }
            case 'C':
                return site(m, src.substring(1)).ret;
            case 'X':
                return castNode(m, src);
            default:
                constant.set(topBit);
                return -1;
        }
    }

    private void addFlow(ByteCodeClass c, BytecodeMethod m, String[] flow) {
        int to = sinkNode(m, flow[0]);
        BitSet constant = new BitSet();
        for (int s = 1; s < flow.length; s++) {
            int from = sourceNode(c, m, flow[s], constant);
            if (from >= 0) edge(from, to);
        }
        if (!constant.isEmpty()) addBits(to, constant);
    }

    private void propagate() {
        BitSet scratch = new BitSet();
        while (!work.isEmpty()) {
            int n = work.poll().intValue();
            queued[n] = false;
            Site st = sitesByRecv.get(Integer.valueOf(n));
            if (st != null) resolveSite(st);
            BitSet src = sets.get(n);
            List<Integer> s = succ.get(n);
            if (s == null) continue;
            boolean srcTop = src.get(topBit);
            for (int k = 0; k < s.size(); k++) {
                int to = s.get(k).intValue();
                BitSet dst = sets.get(to);
                if (dst.get(topBit)) continue;
                BitSet filter = filters.get(Integer.valueOf(to));
                if (filter != null) {
                    scratch.clear();
                    scratch.or(srcTop ? filter : src);
                    if (!srcTop) scratch.and(filter);
                    scratch.andNot(dst);
                    if (scratch.isEmpty()) continue;
                    dst.or(scratch);
                    enqueue(to);
                    continue;
                }
                if (srcTop) {
                    if (DUMP != null) topWhy.put(Integer.valueOf(to), "@" + n);
                    dst.clear();
                    dst.set(topBit);
                    enqueue(to);
                    continue;
                }
                scratch.clear();
                scratch.or(src);
                scratch.andNot(dst);
                if (scratch.isEmpty()) continue;
                dst.or(scratch);
                enqueue(to);
            }
        }
    }

    // ---- results

    private void annotateSites(List<ByteCodeClass> classes) {
        StringBuilder dump = DUMP == null ? null : new StringBuilder();
        for (ByteCodeClass c : classes) {
            for (BytecodeMethod m : c.getMethods()) {
                if (m.isEliminated() || m.getInstructions() == null) continue;
                boolean summarised = m.getTypeFlowSummary() != null;
                for (Instruction ins : m.getInstructions()) {
                    if (!(ins instanceof Invoke)) continue;
                    Invoke inv = (Invoke) ins;
                    inv.setFlowImpls(null);
                    String[] sources = inv.getFlowReceiverSources();
                    int op = inv.getOpcode();
                    if (op != Opcodes.INVOKEVIRTUAL && op != Opcodes.INVOKEINTERFACE) {
                        continue;
                    }
                    if (!summarised || sources == null) {
                        if (dump != null) {
                            dump.append(summarised ? "NOSRC " : "NOSUM ").append(c.getClsName()).append('.')
                                    .append(m.getMethodName()).append(" -> ").append(inv.getOwner()).append('.')
                                    .append(inv.getName()).append('\n');
                        }
                        continue;
                    }
                    BitSet types = new BitSet();
                    int topSource = -1;
                    for (String src : sources) {
                        BitSet constant = new BitSet();
                        int n = sourceNode(c, m, src, constant);
                        if (n >= 0) {
                            types.or(sets.get(n));
                            if (sets.get(n).get(topBit) && topSource < 0) topSource = n;
                        }
                        types.or(constant);
                    }
                    if (types.get(topBit)) {
                        topSites++;
                        if (dump != null) {
                            dump.append("TOP ").append(c.getClsName()).append('.').append(m.getMethodName())
                                    .append(" -> ").append(inv.getOwner()).append('.').append(inv.getName())
                                    .append(inv.getDesc()).append(' ').append(java.util.Arrays.toString(sources));
                            if (topSource >= 0) dump.append(topChain(topSource));
                            dump.append('\n');
                        }
                        continue;
                    }
                    List<BytecodeMethod> impls = resolveFor(inv.getOwner(), inv.getName(), inv.getDesc(), types);
                    if (impls == null) {
                        topSites++;
                        if (dump != null) {
                            dump.append("UNRES ").append(c.getClsName()).append('.').append(m.getMethodName())
                                    .append(" -> ").append(inv.getOwner()).append('.').append(inv.getName())
                                    .append(inv.getDesc()).append(' ').append(describe(types)).append('\n');
                        }
                        continue;
                    }
                    refinedSites++;
                    List<BytecodeMethod> cha = rta.resolveImpls(inv.getOwner(), inv.getName(), inv.getDesc());
                    if (cha == null || cha.size() > impls.size()) narrowedSites++;
                    inv.setFlowImpls(impls);
                    if (dump != null) {
                        dump.append("BOUND ").append(c.getClsName()).append('.').append(m.getMethodName())
                                .append(" -> ").append(inv.getOwner()).append('.').append(inv.getName())
                                .append(inv.getDesc()).append(' ').append(impls.size()).append('/')
                                .append(cha == null ? -1 : cha.size()).append('\n');
                    }
                }
            }
        }
        if (dump != null) {
            try {
                try (java.io.Writer w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(DUMP), "UTF-8")) {
                    w.write(dbg.toString());
                    String why = System.getProperty("parparvm.js.typeflow.why");
                    if (why != null) w.write(whyReport(why));
                    w.write(dump.toString());
                }
            } catch (java.io.IOException e) {
                System.out.println("type flow dump failed: " + e);
            }
        }
    }

    /// The bodies the instantiated types in `types` reach for this call, restricted to
    /// the receiver's declared cone. Null when any of them cannot be resolved, or when
    /// a host object can be the receiver -- the caller then keeps the cone answer.
    private String whyReport(String needle) {
        List<List<Integer>> pred = new ArrayList<List<Integer>>();
        for (int i = 0; i < sets.size(); i++) pred.add(null);
        for (int i = 0; i < succ.size(); i++) {
            List<Integer> s = succ.get(i);
            if (s == null) continue;
            for (Integer t : s) {
                List<Integer> p = pred.get(t.intValue());
                if (p == null) { p = new ArrayList<Integer>(); pred.set(t.intValue(), p); }
                p.add(Integer.valueOf(i));
            }
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < nodeNames.size(); i++) {
            if (nodeNames.get(i).contains(needle)) whyNode(out, pred, i, 0, new HashSet<Integer>());
        }
        return out.toString();
    }

    private void whyNode(StringBuilder out, List<List<Integer>> pred, int n, int depth, Set<Integer> seen) {
        if (depth > 6 || !seen.add(Integer.valueOf(n))) return;
        for (int d = 0; d < depth; d++) out.append("  ");
        out.append("WHY ").append(sets.get(n).cardinality()).append(' ').append(nodeNames.get(n)).append(' ')
                .append(describe(n)).append('\n');
        List<Integer> p = pred.get(n);
        if (p == null) return;
        // The widest predecessors first: they are where the width comes from.
        List<Integer> sorted = new ArrayList<Integer>(p);
        Collections.sort(sorted, new java.util.Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return sets.get(b.intValue()).cardinality() - sets.get(a.intValue()).cardinality();
            }
        });
        for (int k = 0; k < Math.min(3, sorted.size()); k++) {
            whyNode(out, pred, sorted.get(k).intValue(), depth + 1, seen);
        }
    }

    private String describe(BitSet types) {
        StringBuilder b = new StringBuilder("[");
        int shown = 0;
        for (int i = types.nextSetBit(0); i >= 0; i = types.nextSetBit(i + 1)) {
            if (jsoBits.get(i) || shown < 4) {
                b.append(jsoBits.get(i) ? "JSO:" : "").append(classNames.get(i)).append(' ');
                shown++;
            }
        }
        return b.append(types.cardinality()).append(']').toString();
    }

    private List<BytecodeMethod> resolveFor(String owner, String name, String desc, BitSet types) {
        // An empty set at a reachable site is a source the seeding missed (a lambda
        // captured by a method capture() gave up on, a native filling an array it
        // was handed), never proof of no target: answering "nothing" there made a
        // call into a generator a synchronous one. Fall back to the cone.
        if (types.isEmpty()) return null;
        Set<String> cone = rta.coneTypes(owner);
        if (cone == null) return null;
        Set<BytecodeMethod> out = Collections.newSetFromMap(new IdentityHashMap<BytecodeMethod, Boolean>());
        for (int b = types.nextSetBit(0); b >= 0; b = types.nextSetBit(b + 1)) {
            String t = classNames.get(b);
            if (!cone.contains(t)) continue;
            if (jsoBits.get(b)) return null;
            ByteCodeClass cls = byName.get(t);
            if (cls == null) return null;
            if (cls.isIsInterface() || cls.isIsAbstract() || !rta.isInstantiated(t)) continue;
            BytecodeMethod impl = rta.resolveOn(t, name, desc);
            if (impl == null || impl.isEliminated()) return null;
            out.add(impl);
        }
        // Same rule as an empty set: no target is a gap, not an answer.
        return out.isEmpty() ? null : new ArrayList<BytecodeMethod>(out);
    }

    private String describe(int node) {
        String n = nodeNames.get(node);
        char k = n.charAt(0);
        if (k == 'P' || k == 'R') {
            int hash = n.indexOf('#');
            int id = Integer.parseInt(n.substring(1, hash < 0 ? n.length() : hash));
            BytecodeMethod m = methodsById.get(id);
            return k + (hash < 0 ? "" : n.substring(hash)) + " " + m.getClsName() + "." + m.getMethodName() + m.getSignature();
        }
        if (k == 'S' || k == 'V') {
            int at = n.indexOf('@');
            BytecodeMethod m = methodsById.get(Integer.parseInt(n.substring(1, at)));
            Site st = sites.get(n.substring(1));
            return k + " " + m.getClsName() + "." + m.getMethodName() + " call " + (st == null ? "?" : st.owner + "." + st.name);
        }
        return n;
    }

    private String topChain(int node) {
        StringBuilder sb = new StringBuilder();
        Set<Integer> seen = new HashSet<Integer>();
        Integer cur = Integer.valueOf(node);
        while (cur != null && seen.add(cur) && seen.size() < 40) {
            sb.append("\n      <- ").append(describe(cur.intValue()));
            String why = topWhy.get(cur);
            if (why == null) break;
            if (why.startsWith("@")) {
                cur = Integer.valueOf(why.substring(1));
            } else {
                sb.append("  [").append(why).append(']');
                break;
            }
        }
        return sb.toString();
    }
}
