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

import com.codename1.tools.translator.bytecodes.Invoke;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/**
 * Allocation proofs for collection calls and native traversal. Reference
 * provenance is captured only where a call, field write or fresh return needs it.
 * Private collection fields are exact only when every write has known allocation
 * provenance and the declaring class has no native methods. Parameters and unknown
 * factory results retain guards. Declared types are never exact facts.
 * No method frames or whole-program constraint graph survive capture().
 */
final class LocalReceiverTypes {
    // Empty provenance is the identity of SourceInterpreter's union. It cannot
    // represent an unknown reference: merging an argument with NEW would erase
    // the argument and falsely prove an exact allocation. Keep an explicit top.
    private static final AbstractInsnNode UNKNOWN_REFERENCE = new InsnNode(Opcodes.NOP);
    private static final SourceValue ONE = new SourceValue(1);
    private static final SourceValue TWO = new SourceValue(2);
    private static final SourceValue UNKNOWN = new SourceValue(1, UNKNOWN_REFERENCE);
    private static final SourceValue NULL_REFERENCE_VALUE = new SourceValue(1, new InsnNode(Opcodes.ACONST_NULL));
    private static final List<Pending> pending = new ArrayList<Pending>();
    private static final class Call {
        final String owner, name, desc;
        final int opcode;
        Call(MethodInsnNode instruction) {
            owner = instruction.owner.replace('/', '_').replace('$', '_');
            name = instruction.name; desc = instruction.desc; opcode = instruction.getOpcode();
        }
    }
    private static final class FieldKey {
        final String owner, name, desc;
        FieldKey(FieldInsnNode field) {
            owner = field.owner.replace('/', '_').replace('$', '_');
            name = field.name.replace('$', '_'); desc = field.desc;
        }
        @Override public int hashCode() { return 31 * owner.hashCode() + name.hashCode(); }
        @Override public boolean equals(Object other) {
            if (!(other instanceof FieldKey)) return false;
            FieldKey field = (FieldKey) other;
            return owner.equals(field.owner) && name.equals(field.name) && desc.equals(field.desc);
        }
    }
    private static final class Writes {
        final Set<String> types = new HashSet<String>();
        final List<Call> calls = new ArrayList<Call>();
        boolean unknown;
    }
    private static final Map<FieldKey, Writes> fieldWrites = new HashMap<FieldKey, Writes>();
    private static boolean collectionField(String desc) {
        return "Ljava/util/List;".equals(desc) || "Ljava/util/Collection;".equals(desc)
                || "Ljava/lang/Iterable;".equals(desc) || "Ljava/util/Set;".equals(desc)
                || "Ljava/util/Map;".equals(desc) || "Ljava/util/ArrayList;".equals(desc)
                || "Ljava/util/HashSet;".equals(desc) || "Ljava/util/LinkedHashSet;".equals(desc)
                || "Ljava/util/HashMap;".equals(desc) || "Ljava/util/LinkedHashMap;".equals(desc)
                || "Ljava/util/IdentityHashMap;".equals(desc);
    }
    private static boolean collectionWrite(AbstractInsnNode node) {
        return (node.getOpcode() == Opcodes.PUTFIELD || node.getOpcode() == Opcodes.PUTSTATIC)
                && collectionField(((FieldInsnNode) node).desc);
    }
    private static Writes writes(FieldInsnNode field) {
        FieldKey key = new FieldKey(field);
        Writes result = fieldWrites.get(key);
        if (result == null) { result = new Writes(); fieldWrites.put(key, result); }
        return result;
    }
    private static boolean privateJavaField(FieldKey key, NativeSymbolIndex nativeSymbols) {
        // A different class's native method can also write this class's fields.
        // Exclude every declaring type mentioned by the native source corpus.
        if (nativeSymbols != null && nativeSymbols.contains(key.owner)) return false;
        ByteCodeClass owner = Parser.getClassObject(key.owner);
        if (owner == null) return false;
        for (BytecodeMethod method : owner.getMethods()) if (method.isNative()) return false;
        for (ByteCodeField field : owner.getFields()) {
            if (field.getFieldName().equals(key.name)
                    && field.getRuntimeDescriptor().equals(key.desc.substring(1, key.desc.length() - 1)
                            .replace('/', '_').replace('$', '_'))) return field.isPrivate();
        }
        return false;
    }
    private static final class Pending {
        final Invoke invoke;
        final Set<String> types;
        final List<Call> calls;
        final List<FieldKey> fields;
        Pending(Invoke invoke, Set<String> types, List<Call> calls, List<FieldKey> fields) {
            this.invoke = invoke; this.types = types; this.calls = calls; this.fields = fields;
        }
    }
    static void clear() { pending.clear(); fieldWrites.clear(); }

    // Deferred until class loading is complete. Retain only call signatures,
    // never MethodNodes, instruction provenance sets, or analysis frames.
    private static boolean resolveCalls(List<Call> calls, Set<String> types) {
        for (Call call : calls) {
            ByteCodeClass owner = Parser.getClassObject(call.owner);
            String resolved = call.opcode == Opcodes.INVOKESTATIC || call.opcode == Opcodes.INVOKESPECIAL
                    ? call.owner : Parser.resolveDevirtualizedOwner(owner, call.name, call.desc);
            ByteCodeClass target = resolved == null ? null : Parser.getClassObject(resolved);
            String type = null;
            if (target != null) for (BytecodeMethod method : target.getMethods()) {
                if (method.getMethodName().equals(call.name) && method.getDesc().equals(call.desc)) {
                    type = method.freshReturnType;
                    break;
                }
            }
            if (type == null) return false;
            types.add(type);
        }
        return true;
    }
    static void resolveFactories() { resolveFactories(null); }

    static void resolveFactories(NativeSymbolIndex nativeSymbols) {
        for (Map.Entry<FieldKey, Writes> entry : fieldWrites.entrySet()) {
            Writes writes = entry.getValue();
            if (!privateJavaField(entry.getKey(), nativeSymbols) || !resolveCalls(writes.calls, writes.types)) writes.unknown = true;
        }
        for (Pending site : pending) {
            boolean exact = resolveCalls(site.calls, site.types);
            for (FieldKey field : site.fields) {
                Writes writes = fieldWrites.get(field);
                if (writes == null || writes.unknown) { exact = false; break; }
                site.types.addAll(writes.types);
            }
            if (exact && !site.types.isEmpty()) site.invoke.setClosedWorldReceiverTypes(site.types);
        }
        clear();
    }

    private static boolean allocationFactory(MethodNode code) {
        boolean returns = false, allocates = false;
        for (AbstractInsnNode node = code.instructions.getFirst(); node != null; node = node.getNext()) {
            allocates |= node.getOpcode() == Opcodes.NEW;
            if (node.getOpcode() != Opcodes.ARETURN) continue;
            returns = true;
            AbstractInsnNode producer = node.getPrevious();
            while (producer != null && producer.getOpcode() < 0) producer = producer.getPrevious();
            // A method call (notably StringBuilder.toString), field read or array
            // load cannot carry NEW provenance in our interpreter. An ALOAD can:
            // a factory may populate a fresh collection before returning it.
            if (producer == null || !(producer.getOpcode() == Opcodes.ALOAD
                    || producer.getOpcode() == Opcodes.CHECKCAST
                    || producer.getOpcode() == Opcodes.INVOKESPECIAL
                    && "<init>".equals(((MethodInsnNode) producer).name))) return false;
        }
        // The frame proof still checks every return, including merged unknowns.
        return returns && allocates;
    }

    /** Primitive provenance cannot prove a receiver; share its category values. */
    private static final class ReceiverInterpreter extends SourceInterpreter {
        ReceiverInterpreter() { super(Opcodes.ASM9); }
        @Override public SourceValue newValue(Type type) {
            if (type == Type.VOID_TYPE) return null;
            if (type == null) return ONE;
            if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) return UNKNOWN;
            return type.getSize() == 2 ? TWO : ONE;
        }
        private SourceValue field(FieldInsnNode node) {
            char kind = node.desc.charAt(0);
            if (kind == 'J' || kind == 'D') return TWO;
            if (kind == 'L' || kind == '[') return collectionField(node.desc) ? new SourceValue(1, node) : UNKNOWN;
            return ONE;
        }
        @Override public SourceValue newOperation(AbstractInsnNode node) {
            switch (node.getOpcode()) {
                case Opcodes.NEW: return new SourceValue(1, node);
                case Opcodes.ACONST_NULL: return NULL_REFERENCE_VALUE;
                case Opcodes.GETSTATIC: return field((FieldInsnNode) node);
                case Opcodes.LCONST_0: case Opcodes.LCONST_1:
                case Opcodes.DCONST_0: case Opcodes.DCONST_1: return TWO;
                case Opcodes.LDC:
                    Object constant = ((LdcInsnNode) node).cst;
                    if (constant instanceof Long || constant instanceof Double) return TWO;
                    if (constant instanceof Integer || constant instanceof Float) return ONE;
                    if (constant instanceof org.objectweb.asm.ConstantDynamic) {
                        char kind = ((org.objectweb.asm.ConstantDynamic) constant).getDescriptor().charAt(0);
                        return kind == 'J' || kind == 'D' ? TWO : kind == 'L' || kind == '[' ? UNKNOWN : ONE;
                    }
                    return UNKNOWN;
                default: return ONE;
            }
        }
        @Override public SourceValue copyOperation(AbstractInsnNode node, SourceValue value) { return value; }
        @Override public SourceValue unaryOperation(AbstractInsnNode node, SourceValue value) {
            switch (node.getOpcode()) {
                case Opcodes.CHECKCAST: return value;
                case Opcodes.GETFIELD: return field((FieldInsnNode) node);
                case Opcodes.NEWARRAY: case Opcodes.ANEWARRAY: return UNKNOWN;
                case Opcodes.LNEG: case Opcodes.DNEG: case Opcodes.I2L: case Opcodes.I2D:
                case Opcodes.L2D: case Opcodes.F2L: case Opcodes.F2D: case Opcodes.D2L: return TWO;
                default: return ONE;
            }
        }
        @Override public SourceValue binaryOperation(AbstractInsnNode node, SourceValue first, SourceValue second) {
            switch (node.getOpcode()) {
                case Opcodes.AALOAD: return UNKNOWN;
                case Opcodes.LALOAD: case Opcodes.DALOAD: case Opcodes.LADD: case Opcodes.DADD:
                case Opcodes.LSUB: case Opcodes.DSUB: case Opcodes.LMUL: case Opcodes.DMUL:
                case Opcodes.LDIV: case Opcodes.DDIV: case Opcodes.LREM: case Opcodes.DREM:
                case Opcodes.LSHL: case Opcodes.LSHR: case Opcodes.LUSHR:
                case Opcodes.LAND: case Opcodes.LOR: case Opcodes.LXOR: return TWO;
                default: return ONE;
            }
        }
        @Override public SourceValue ternaryOperation(AbstractInsnNode node, SourceValue first, SourceValue second, SourceValue third) {
            return ONE;
        }
        @Override public SourceValue naryOperation(AbstractInsnNode node, List<? extends SourceValue> arguments) {
            if (node.getOpcode() == Opcodes.MULTIANEWARRAY) return UNKNOWN;
            String descriptor = node instanceof MethodInsnNode ? ((MethodInsnNode) node).desc : ((InvokeDynamicInsnNode) node).desc;
            char kind = descriptor.charAt(descriptor.indexOf(')') + 1);
            if (kind == 'V') return null;
            if (kind == 'J' || kind == 'D') return TWO;
            if (kind == '[') return UNKNOWN;
            // An invokedynamic used to dissolve into top. A lambda's indy is the most
            // precise allocation site in the whole program -- the closure's class is
            // decided right there -- so it carries provenance like NEW does. Consumers
            // that do not recognise it still see a source whose opcode is not NEW and
            // stay conservative, which is what they did with top.
            if (kind == 'L') return new SourceValue(1, node);
            return ONE;
        }
    }

    static boolean isCandidate(int opcode, String owner, String name, String descriptor) {
        return (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE) && (
                owner.startsWith("java/util/")
                || "iterator".equals(name) && "()Ljava/util/Iterator;".equals(descriptor)
                || "keySet".equals(name) && "()Ljava/util/Set;".equals(descriptor)
                || "values".equals(name) && "()Ljava/util/Collection;".equals(descriptor));
    }

    static boolean isLambdaCandidate(int opcode) {
        return opcode == Opcodes.INVOKEINTERFACE;
    }

    /**
     * Ties each InvokeDynamicInsnNode in the analysis tree to the synthetic lambda
     * class the parser made for it. The parser visits a different node object for the
     * same instruction, so the two are matched by their order within the method --
     * which is exact, because both walk the same post-JSR-inlined stream.
     */
    private static Map<AbstractInsnNode, String> lambdaSites(MethodNode code, List<String> indyLambdas) {
        Map<AbstractInsnNode, String> sites = new HashMap<AbstractInsnNode, String>();
        int index = 0;
        for (AbstractInsnNode node = code.instructions.getFirst(); node != null; node = node.getNext()) {
            if (node instanceof InvokeDynamicInsnNode) {
                if (index >= indyLambdas.size()) {
                    // The two walks disagree on how many indy instructions this method
                    // has, so position no longer identifies anything. Prove nothing.
                    return java.util.Collections.emptyMap();
                }
                String lambda = indyLambdas.get(index++);
                if (lambda != null) {
                    sites.put(node, lambda);
                }
            }
        }
        return index == indyLambdas.size() ? sites : java.util.Collections.<AbstractInsnNode, String>emptyMap();
    }

    static Frame<SourceValue>[] capture(String owner, MethodNode code, Map<AbstractInsnNode, Invoke> invokes,
            BytecodeMethod method, List<String> indyLambdas) {
        Map<AbstractInsnNode, String> lambdaSites = lambdaSites(code, indyLambdas);
        boolean factory = allocationFactory(code);
        boolean hasWrites = false;
        for (AbstractInsnNode node = code.instructions.getFirst(); node != null; node = node.getNext()) {
            if (collectionWrite(node)) { hasWrites = true; break; }
        }
        if ((!factory && invokes.isEmpty() && !hasWrites) || (code.access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) != 0) return null;
        try {
            SourceInterpreter interpreter = new ReceiverInterpreter();
            Frame<SourceValue>[] frames = new Analyzer<SourceValue>(interpreter).analyze(owner, code);
            String fresh = null;
            boolean factoryExact = factory;
            for (int i = 0; i < code.instructions.size(); i++) {
                if (factory && frames[i] != null && code.instructions.get(i).getOpcode() == Opcodes.ARETURN) {
                    SourceValue returned = frames[i].getStack(frames[i].getStackSize() - 1);
                    if (returned.insns.isEmpty()) factoryExact = false;
                    for (AbstractInsnNode source : returned.insns) {
                        if (source.getOpcode() != Opcodes.NEW) { factoryExact = false; break; }
                        String type = ((TypeInsnNode) source).desc.replace('/', '_').replace('$', '_');
                        if (fresh != null && !fresh.equals(type)) factoryExact = false;
                        fresh = type;
                    }
                }
                AbstractInsnNode instruction = code.instructions.get(i);
                if (collectionWrite(instruction) && frames[i] != null) {
                    Writes writes = writes((FieldInsnNode) instruction);
                    SourceValue value = frames[i].getStack(frames[i].getStackSize() - 1);
                    if (value.insns.isEmpty()) writes.unknown = true;
                    for (AbstractInsnNode source : value.insns) {
                        if (source.getOpcode() == Opcodes.NEW) {
                            writes.types.add(((TypeInsnNode) source).desc.replace('/', '_').replace('$', '_'));
                        } else if (source instanceof MethodInsnNode) {
                            writes.calls.add(new Call((MethodInsnNode) source));
                        } else if (source.getOpcode() != Opcodes.ACONST_NULL) {
                            writes.unknown = true;
                        }
                    }
                }
                Invoke invoke = invokes.get(instruction);
                if (invoke == null || frames[i] == null) continue;
                Frame<SourceValue> frame = frames[i];
                SourceValue receiver = frame.getStack(frame.getStackSize() - Type.getArgumentTypes(invoke.getDesc()).length - 1);
                if (receiver.insns.isEmpty()) continue;
                Set<String> types = new HashSet<String>();
                boolean exact = true;
                // Non-null needs EVERY source to be an allocation. ACONST_NULL is
                // tolerated by the exactness test above -- a null merges with anything
                // without disproving its type -- and is exactly what must disprove this.
                boolean nonNull = true;
                List<Call> calls = new ArrayList<Call>();
                List<FieldKey> fields = new ArrayList<FieldKey>();
                for (AbstractInsnNode source : receiver.insns) {
                    String lambda = lambdaSites.get(source);
                    if (lambda != null) {
                        // The receiver is the closure this very method built. Its class
                        // was decided at the indy and cannot be anything else, so the
                        // interface call collapses to a direct one -- this is the
                        // addActionListener(e -> ...) shape, seen from the side where
                        // the type is still known.
                        types.add(lambda);
                    } else if (source.getOpcode() == Opcodes.NEW) {
                        types.add(((TypeInsnNode) source).desc.replace('/', '_').replace('$', '_'));
                    } else if (source instanceof MethodInsnNode) {
                        nonNull = false;
                        calls.add(new Call((MethodInsnNode) source));
                    } else if ((source.getOpcode() == Opcodes.GETFIELD || source.getOpcode() == Opcodes.GETSTATIC)
                            && collectionField(((FieldInsnNode) source).desc)) {
                        nonNull = false;
                        fields.add(new FieldKey((FieldInsnNode) source));
                    } else if (source.getOpcode() == Opcodes.ACONST_NULL) {
                        nonNull = false;
                    } else if (source.getOpcode() != Opcodes.ACONST_NULL) {
                        exact = false;
                        break;
                    }
                }
                if (exact && (!calls.isEmpty() || !fields.isEmpty())) pending.add(new Pending(invoke, types, calls, fields));
                else if (exact && !types.isEmpty()) {
                    invoke.setClosedWorldReceiverTypes(types);
                    invoke.setExactReceiverNonNull(nonNull);
                }
            }
            if (factoryExact) method.freshReturnType = fresh;
            return frames; // Also reuse the category sizes for DUP/POP2 resolution.
        } catch (AnalyzerException error) {
            for (AbstractInsnNode node = code.instructions.getFirst(); node != null; node = node.getNext()) {
                if (collectionWrite(node)) writes((FieldInsnNode) node).unknown = true;
            }
            return null; // The guarded native path remains available.
        }
    }
}
