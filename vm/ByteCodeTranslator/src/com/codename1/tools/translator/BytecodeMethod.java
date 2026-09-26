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
 
import com.codename1.tools.translator.bytecodes.ArithmeticExpression;
import com.codename1.tools.translator.bytecodes.ArrayLengthExpression;
import com.codename1.tools.translator.bytecodes.ArrayLoadExpression;
import com.codename1.tools.translator.bytecodes.AssignableExpression;
import com.codename1.tools.translator.bytecodes.BasicInstruction;
import com.codename1.tools.translator.bytecodes.CustomIntruction;
import com.codename1.tools.translator.bytecodes.CustomInvoke;
import com.codename1.tools.translator.bytecodes.CustomJump;
import com.codename1.tools.translator.bytecodes.DupExpression;
import com.codename1.tools.translator.bytecodes.Field;
import com.codename1.tools.translator.bytecodes.IInc;
import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.Invoke;
import com.codename1.tools.translator.bytecodes.Jump;
import com.codename1.tools.translator.bytecodes.LabelInstruction;
import com.codename1.tools.translator.bytecodes.Ldc;
import com.codename1.tools.translator.bytecodes.ScalarAllocInit;
import com.codename1.tools.translator.bytecodes.LineNumber;
import com.codename1.tools.translator.bytecodes.LocalVariable;
import com.codename1.tools.translator.bytecodes.MultiArray;
import com.codename1.tools.translator.bytecodes.SwitchInstruction;
import com.codename1.tools.translator.bytecodes.TryCatch;
import com.codename1.tools.translator.bytecodes.TypeInstruction;
import com.codename1.tools.translator.bytecodes.VarOp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class BytecodeMethod implements SignatureSet {
    // Initial native stack buffer in bytes; API-visible capacity remains unchanged.
    private static final int SB_STACK_FLOOR_UNITS = 128;

    /// Ceiling on the TOTAL stack-resident fused buffers one method may declare. These
    /// are C locals and iOS secondary threads have 512KB stacks, so the floor above
    /// must not scale a frame without bound.
    private static final int SB_STACK_BUDGET_BYTES = 2048;


    private static MethodDependencyGraph dependencyGraph;

    /**
     * @return the acceptStaticOnEquals
     */
    public static boolean isAcceptStaticOnEquals() {
        return acceptStaticOnEquals;
    }

    /**
     * @param aAcceptStaticOnEquals the acceptStaticOnEquals to set
     */
    public static void setAcceptStaticOnEquals(boolean aAcceptStaticOnEquals) {
        acceptStaticOnEquals = aAcceptStaticOnEquals;
    }

    public static void setDependencyGraph(MethodDependencyGraph dependencyGraph) {
        BytecodeMethod.dependencyGraph = dependencyGraph;
    }
    private List<ByteCodeMethodArg> arguments = new ArrayList<ByteCodeMethodArg>();
    private Set<LocalVariable> localVariables = new HashSet<LocalVariable>();
    private ByteCodeMethodArg returnType;
    private String methodName;
    private String clsName;
    private boolean constructor;
    private boolean staticMethod;
    private boolean privateMethod;
    private boolean nativeMethod;
    private boolean abstractMethod;
    private List<String> dependentClasses = new ArrayList<String>();
    //private List<String> exportedClasses = new ArrayList<String>();
    private List<Instruction> instructions = new ArrayList<Instruction>();
    private String declaration = ""; 
    private String sourceFile;
    private int maxStack;
    private int maxLocals;
    private static boolean acceptStaticOnEquals;
    private static final boolean FORCE_VOLATILE_LOCALS =
            "true".equalsIgnoreCase(Util.getProperty("CN1_FORCE_VOLATILE_LOCALS", "false"));
    // Frameless codegen gate (-Dcn1.frameless, default on). When off the
    // eligibility predicate always returns false, so every method emits the
    // legacy frame code byte-for-byte identical to before.
    private static final boolean FRAMELESS_ENABLED =
            "true".equalsIgnoreCase(Util.getProperty("cn1.frameless", "true"));
    // PHASE 3b: extend frameless codegen to OBJECT-BEARING methods (-Dcn1.frameless.objects,
    // default off). Such a method keeps its object operand stack + object locals in a
    // method-local C array on the native stack; the C runtime (built with
    // -DCN1_CONSERVATIVE_GC_ROOTS) finds those roots by conservatively scanning each
    // stopped thread's native stack. With this OFF, only primitive-only methods are
    // frameless (identical to the prior phase). Requires the conservative-GC runtime.
    private static final boolean FRAMELESS_OBJECTS_ENABLED =
            "true".equalsIgnoreCase(Util.getProperty("cn1.frameless.objects", "true"));
    // PHASE 3b: extend object-frameless to INSTANCE methods (receiver `this` becomes a
    // conservatively-scanned C parameter). Now DEFAULT ON: the intermittent multi-threaded
    // failure that previously gated this off was a pre-existing Thread.start/join visibility
    // race (alive set on the worker thread async after start() returned), fixed in
    // java_lang_Thread_start__ (993331107); with it fixed, MtStress is 50/50 deterministic.
    private static final boolean FRAMELESS_INSTANCE_ENABLED =
            "true".equalsIgnoreCase(Util.getProperty("cn1.frameless.instance", "true"));
    private int methodOffset;
    private boolean forceVirtual;
    private boolean virtualOverriden;
    private boolean finalMethod;
    private boolean synchronizedMethod;
    private final static Set<String> virtualMethodsInvoked = new TreeSet<String>();    
    private String desc;
    private boolean eliminated;
    private boolean barebone;
    private boolean disableDebugInfo;
    private boolean disableNullAndArrayBoundsChecks;
    private boolean fastMethodStackInUse;
    private boolean fastMethodStackPrimitiveOnly;
    // True when this method qualifies for frameless codegen: a primitive-only
    // static method whose frame holds zero object references, so it contributes
    // no GC roots and its per-call frame bookkeeping can be eliminated. Computed
    // once (on the raw bytecode, before optimize) and cached in appendMethodC.
    private boolean frameless;
    private String jsBodyScript;
    private String[] jsBodyParams;

    
    static boolean optimizerOn;

    /**
     * When true, the translator emits extra metadata (per-frame locals-address
     * tables, variable side-tables) and uses a debugger-aware form of
     * __CN1_DEBUG_INFO. Toggled via the cn1.onDeviceDebug system property.
     * Release builds leave this off and pay no overhead.
     */
    static boolean onDeviceDebug;

    static {
        String op = System.getProperty("optimizer");
        optimizerOn = op == null || op.equalsIgnoreCase("on");
        //optimizerOn = false;

        onDeviceDebug = "true".equalsIgnoreCase(Util.getProperty("cn1.onDeviceDebug", "false"));
    }

    public static boolean isOnDeviceDebug() {
        return onDeviceDebug;
    }

    public boolean isBarebone() {
        return barebone;
    }

    public boolean isDisableDebugInfo() {
        return disableDebugInfo;
    }

    public void setDisableDebugInfo(boolean disableDebugInfo) {
        this.disableDebugInfo = disableDebugInfo;
    }

    public boolean isDisableNullAndArrayBoundsChecks() {
        return disableNullAndArrayBoundsChecks;
    }

    public void setDisableNullAndArrayBoundsChecks(boolean disableNullAndArrayBoundsChecks) {
        this.disableNullAndArrayBoundsChecks = disableNullAndArrayBoundsChecks;
    }

    private boolean checkBarebone() {
        if(synchronizedMethod || nativeMethod || hasExceptionHandlingOrMethodCalls() || localVariables.size() > 0) {
            return false;
        }
        int argSlots = 0;
        if(!staticMethod) {
            argSlots++;
        }
        for(ByteCodeMethodArg arg : arguments) {
            argSlots++;
            if(arg.isDoubleOrLong()) {
                argSlots++;
            }
        }
        if(maxLocals > argSlots) {
            return false;
        }
        int maxLocalIndexUsed = -1;
        for (Instruction i : instructions) {
            if (i instanceof VarOp) {
                maxLocalIndexUsed = Math.max(maxLocalIndexUsed, ((VarOp)i).getIndex());
            } else if (i instanceof IInc) {
                maxLocalIndexUsed = Math.max(maxLocalIndexUsed, ((IInc)i).getVar());
            }
        }
        if (maxLocalIndexUsed >= argSlots) {
            return false;
        }
        for(Instruction i : instructions) {
            if(i instanceof LabelInstruction || i instanceof LineNumber || i instanceof IInc ||
                    i instanceof Jump || i instanceof CustomJump || i instanceof LocalVariable) {
                continue;
            }
            if(i instanceof BasicInstruction) {
                int op = i.getOpcode();
                switch(op) {
                    case Opcodes.SIPUSH:
                    case Opcodes.BIPUSH:
                    case Opcodes.ICONST_0:
                    case Opcodes.ICONST_1:
                    case Opcodes.ICONST_2:
                    case Opcodes.ICONST_3:
                    case Opcodes.ICONST_4:
                    case Opcodes.ICONST_5:
                    case Opcodes.ICONST_M1:
                    case Opcodes.LCONST_0:
                    case Opcodes.LCONST_1:
                    case Opcodes.FCONST_0:
                    case Opcodes.FCONST_1:
                    case Opcodes.FCONST_2:
                    case Opcodes.DCONST_0:
                    case Opcodes.DCONST_1:
                    case Opcodes.RETURN:
                    case Opcodes.IRETURN:
                    case Opcodes.LRETURN:
                    case Opcodes.FRETURN:
                    case Opcodes.DRETURN:
                    case Opcodes.ARETURN:
                    case Opcodes.NOP:
                    case Opcodes.POP:
                    case Opcodes.POP2:
                    case Opcodes.DUP:
                    case Opcodes.DUP2:
                    case Opcodes.DUP_X1:
                    case Opcodes.DUP2_X1:
                    case Opcodes.DUP_X2:
                    case Opcodes.DUP2_X2:
                    case Opcodes.SWAP:
                    case Opcodes.IADD:
                    case Opcodes.LADD:
                    case Opcodes.FADD:
                    case Opcodes.DADD:
                    case Opcodes.ISUB:
                    case Opcodes.LSUB:
                    case Opcodes.FSUB:
                    case Opcodes.DSUB:
                    case Opcodes.IMUL:
                    case Opcodes.LMUL:
                    case Opcodes.FMUL:
                    case Opcodes.DMUL:
                    case Opcodes.FDIV:
                    case Opcodes.DDIV:
                    case Opcodes.FREM:
                    case Opcodes.DREM:
                    case Opcodes.INEG:
                    case Opcodes.LNEG:
                    case Opcodes.FNEG:
                    case Opcodes.DNEG:
                    case Opcodes.ISHL:
                    case Opcodes.LSHL:
                    case Opcodes.ISHR:
                    case Opcodes.LSHR:
                    case Opcodes.IUSHR:
                    case Opcodes.LUSHR:
                    case Opcodes.IAND:
                    case Opcodes.LAND:
                    case Opcodes.IOR:
                    case Opcodes.LOR:
                    case Opcodes.IXOR:
                    case Opcodes.LXOR:
                    case Opcodes.I2L:
                    case Opcodes.I2F:
                    case Opcodes.I2D:
                    case Opcodes.L2I:
                    case Opcodes.L2F:
                    case Opcodes.L2D:
                    case Opcodes.F2I:
                    case Opcodes.F2L:
                    case Opcodes.F2D:
                    case Opcodes.D2I:
                    case Opcodes.D2L:
                    case Opcodes.D2F:
                    case Opcodes.I2B:
                    case Opcodes.I2C:
                    case Opcodes.I2S:
                    case Opcodes.LCMP:
                    case Opcodes.FCMPG:
                    case Opcodes.FCMPL:
                    case Opcodes.DCMPL:
                    case Opcodes.DCMPG:
                        continue;
                }
                return false;
            }
            if(i instanceof VarOp) {
                continue;
            }
            if(i instanceof ArithmeticExpression) {
                continue;
            }
            if(i instanceof Field) {
                int op = i.getOpcode();
                if(op == Opcodes.GETFIELD) {
                    continue;
                }
                if(op == Opcodes.PUTFIELD) {
                    if(((Field)i).isObject()) {
                        return false;
                    }
                    continue;
                }
            }
            return false;
        }
        return true;
    }

    private boolean canUseFastMethodStack() {
        if (synchronizedMethod || nativeMethod || abstractMethod) {
            return false;
        }
        for (Instruction instruction : instructions) {
            if (instruction instanceof TryCatch
                    || instruction instanceof Invoke
                    || instruction instanceof CustomInvoke
                    || instruction instanceof Field
                    || instruction instanceof TypeInstruction
                    || instruction instanceof MultiArray
                    || instruction instanceof CustomIntruction) {
                return false;
            }
            if (instruction instanceof ArrayLoadExpression && !disableNullAndArrayBoundsChecks) {
                return false;
            }
            if (instruction instanceof BasicInstruction) {
                int op = instruction.getOpcode();
                if (op == Opcodes.MONITORENTER || op == Opcodes.MONITOREXIT
                        || op == Opcodes.ATHROW
                        || op == Opcodes.IDIV || op == Opcodes.LDIV || op == Opcodes.IREM || op == Opcodes.LREM
                        || op == Opcodes.ARRAYLENGTH
                        || (!disableNullAndArrayBoundsChecks && (op >= Opcodes.IALOAD && op <= Opcodes.SALOAD))
                        || (!disableNullAndArrayBoundsChecks && (op >= Opcodes.IASTORE && op <= Opcodes.SASTORE))
                        || (!disableNullAndArrayBoundsChecks && (op == Opcodes.AALOAD || op == Opcodes.AASTORE
                        || op == Opcodes.BALOAD || op == Opcodes.BASTORE || op == Opcodes.CALOAD || op == Opcodes.CASTORE))
                        || op == Opcodes.NEWARRAY) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isPrimitiveOnlyFastFrameCandidate() {
        for (ByteCodeMethodArg arg : arguments) {
            if (arg.getQualifier() == 'o') {
                return false;
            }
        }
        if (!returnType.isVoid() && returnType.getQualifier() == 'o') {
            return false;
        }
        if (!staticMethod) {
            return false;
        }
        for (Instruction instruction : instructions) {
            if (instruction instanceof VarOp) {
                int op = instruction.getOpcode();
                if (op == Opcodes.ALOAD || op == Opcodes.ASTORE) {
                    return false;
                }
            }
            if (instruction instanceof BasicInstruction) {
                int op = instruction.getOpcode();
                if (op == Opcodes.ARETURN || op == Opcodes.ACONST_NULL || op == Opcodes.AALOAD || op == Opcodes.AASTORE) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean useFastReturnRelease() {
        return fastMethodStackInUse && !TryCatch.isTryCatchInMethod();
    }

    /**
     * @return true if this method was selected for frameless codegen. Valid only
     * after {@link #appendMethodC} has begun emitting this method (the flag is
     * computed there, on the raw bytecode, before optimization).
     */
    public boolean isFrameless() {
        return frameless;
    }

    /**
     * A descriptor (e.g. {@code (IJ)D}) is primitive-only when neither its
     * argument list nor its return type names an object or array type -- i.e. it
     * contains no 'L' (object) and no '[' (array).
     */
    private static boolean isPrimitiveOnlyDescriptor(String desc) {
        return desc.indexOf('L') < 0 && desc.indexOf('[') < 0;
    }

    /**
     * The BasicInstruction opcodes that are safe inside a frameless method:
     * numeric constants, all arithmetic (including the throwing IDIV/IREM/LDIV/LREM),
     * negation, shifts, bitwise, numeric conversions, the long/float/double compares,
     * primitive stack shuffles (no _X object-shape variants), and the primitive
     * returns. Object-stack opcodes (ACONST_NULL, ARETURN, DUP_X*, array load/store,
     * ARRAYLENGTH, ATHROW, MONITOR*, NEWARRAY) are deliberately excluded.
     */
    private static boolean isFramelessBasicOpcode(int op) {
        switch (op) {
            case Opcodes.NOP:
            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1:
            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2:
            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1:
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
            case Opcodes.POP:
            case Opcodes.POP2:
            case Opcodes.DUP:
            case Opcodes.DUP2:
            case Opcodes.SWAP:
            case Opcodes.IADD:
            case Opcodes.LADD:
            case Opcodes.FADD:
            case Opcodes.DADD:
            case Opcodes.ISUB:
            case Opcodes.LSUB:
            case Opcodes.FSUB:
            case Opcodes.DSUB:
            case Opcodes.IMUL:
            case Opcodes.LMUL:
            case Opcodes.FMUL:
            case Opcodes.DMUL:
            case Opcodes.IDIV:
            case Opcodes.LDIV:
            case Opcodes.FDIV:
            case Opcodes.DDIV:
            case Opcodes.IREM:
            case Opcodes.LREM:
            case Opcodes.FREM:
            case Opcodes.DREM:
            case Opcodes.INEG:
            case Opcodes.LNEG:
            case Opcodes.FNEG:
            case Opcodes.DNEG:
            case Opcodes.ISHL:
            case Opcodes.LSHL:
            case Opcodes.ISHR:
            case Opcodes.LSHR:
            case Opcodes.IUSHR:
            case Opcodes.LUSHR:
            case Opcodes.IAND:
            case Opcodes.LAND:
            case Opcodes.IOR:
            case Opcodes.LOR:
            case Opcodes.IXOR:
            case Opcodes.LXOR:
            case Opcodes.I2L:
            case Opcodes.I2F:
            case Opcodes.I2D:
            case Opcodes.L2I:
            case Opcodes.L2F:
            case Opcodes.L2D:
            case Opcodes.F2I:
            case Opcodes.F2L:
            case Opcodes.F2D:
            case Opcodes.D2I:
            case Opcodes.D2L:
            case Opcodes.D2F:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
            case Opcodes.LCMP:
            case Opcodes.FCMPL:
            case Opcodes.FCMPG:
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
            case Opcodes.RETURN:
                return true;
        }
        return false;
    }

    /**
     * Object-touching BasicInstruction opcodes that are safe inside an OBJECT-bearing
     * frameless method (-Dcn1.frameless.objects). All are SP-relative or self-contained
     * (no frame-variable references): the null constant, array loads/stores (which carry
     * an array reference on the operand stack), arraylength, the object-shape stack
     * shuffles, and primitive-array allocation. ARETURN is handled by its own frameless
     * branch. Deliberately EXCLUDED for now: ATHROW, MONITORENTER, MONITOREXIT (monitors
     * always arrive wrapped in a try/finally, which already disqualifies the method).
     */
    private static boolean isFramelessObjectOpcode(int op) {
        switch (op) {
            case Opcodes.ACONST_NULL:
            case Opcodes.AALOAD:
            case Opcodes.IALOAD:
            case Opcodes.LALOAD:
            case Opcodes.FALOAD:
            case Opcodes.DALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD:
            case Opcodes.AASTORE:
            case Opcodes.IASTORE:
            case Opcodes.LASTORE:
            case Opcodes.FASTORE:
            case Opcodes.DASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
            case Opcodes.ARRAYLENGTH:
            case Opcodes.ARETURN:
            case Opcodes.DUP_X1:
            case Opcodes.DUP_X2:
            case Opcodes.DUP2_X1:
            case Opcodes.DUP2_X2:
            case Opcodes.NEWARRAY:
            // ATHROW: an explicit throw is throwException(threadStateData, obj), which
            // reads only thread state -- the try-block stack, the exception slot -- and
            // longjmps to the nearest handler. It touches nothing a frameless frame
            // lacks, and frameless code already throws through the identical path for
            // every implicit exception (bounds and null checks call
            // throwArrayIndexOutOfBoundsException and friends from frameless bodies).
            // Excluding it made a method pay a full frame on EVERY call for a throw it
            // almost never takes: the census put ATHROW as the first blocker of ~90% of
            // the methods still excluded by an instruction (234 of 260 blockers on the
            // Bench translation; MONITORENTER the other 26). StringBuilder.resizeBuffer
            // was one -- DEFINE_INSTANCE_METHOD_STACK, line tracking and
            // releaseForReturn on every append that grows, for `throw new
            // OutOfMemoryError()`.
            //
            // The one observable difference is the one every frameless method already
            // has: it records no call-stack entry, so a trace filled in by this throw
            // does not list it.
            case Opcodes.ATHROW:
                return true;
        }
        return false;
    }

    /**
     * Conservative whitelist deciding whether this method can use frameless
     * codegen. Runs on the RAW bytecode (before {@link #optimize} fuses opcodes
     * into opaque custom instructions). Eligible iff the method is a non-native,
     * non-abstract, non-eliminated, non-synchronized, non-on-device-debug STATIC
     * method that returns a primitive/void, takes only primitive args, declares no
     * object locals, has no try/catch, is non-empty, and every instruction is in
     * the handled set (primitive loads/stores/iinc, numeric constants, all
     * arithmetic, conversions, compares, primitive branches/switches/stack ops,
     * primitive returns, and INVOKESTATIC of a primitive-only descriptor). Any
     * object-touching opcode makes it ineligible -- such a frame would carry a GC
     * root, which is exactly what frameless relies on being absent.
     */
    /// FRAMELESS CENSUS (-Dcn1.framelessCensus=true). Plan item D6 says its first step
    /// is a measurement rather than code: a method with any try/catch loses frameless
    /// codegen, bounds-check elimination and StringBuilder stack allocation at once, and
    /// the question is how many methods that exclusion ALONE is costing. Counted here
    /// because this is the one place that already knows the answer.
    static final boolean FRAMELESS_CENSUS =
            "true".equalsIgnoreCase(Util.getProperty("cn1.framelessCensus", "false"));
    static int censusEligible, censusExcludedTryCatch, censusExcludedOther, censusTotal;
    // StringBuilder stack-allocation census. The corpus measurement that motivated it:
    // StringBuilder is 9.53MB of the 168.97MB peak occupied heap and 0% of it is traced,
    // i.e. every live builder at peak is already garbage waiting to be reclaimed. Whether
    // that is worth attacking depends entirely on how many allocation sites the analysis
    // currently refuses and WHY, so count the reasons rather than guess at them.
    static int sbCensusSites, sbCensusBailTryCatch, sbCensusBailSync, sbCensusStackAllocated;
    static final boolean SB_SKIP_ESCAPE_VALIDATION =
            "true".equalsIgnoreCase(Util.getProperty("cn1.sbSkipEscapeValidation", "false"));
    /// Why the 'other' exclusions happened, so the census answers where the REMAINING
    /// frameless opportunity is rather than only ruling try/catch out.
    static int censusNoConstructor, censusNoSync, censusNoDebug, censusNoOpcode, censusEmpty;
    /// Which instruction stopped a method from going frameless, by opcode. The census
    /// used to report only "unhandledOpcode", which says a whitelist refused something
    /// and not what -- and the answer decides the next change: an opcode that blocks
    /// thousands of methods for no semantic reason is worth admitting, one that blocks
    /// a handful is not. Counts the FIRST blocker per method only.
    static final java.util.Map<String, Integer> censusBlockers = new java.util.TreeMap<String, Integer>();
    static boolean censusBlocked(Instruction i, boolean ignoreTryCatch) {
        if (FRAMELESS_CENSUS && !ignoreTryCatch) {
            String k = i.getClass().getSimpleName() + ":" + i.getOpcode();
            Integer c = censusBlockers.get(k);
            censusBlockers.put(k, c == null ? 1 : c + 1);
        }
        return false;
    }
    static void censusReason(int which) {
        switch (which) {
            case 0: censusNoConstructor++; break;
            case 1: censusNoSync++; break;
            case 2: censusNoDebug++; break;
            case 3: censusNoOpcode++; break;
            default: censusEmpty++; break;
        }
    }

    /** Set while emitting: this method is frameless AND contains a try/catch, so its
     *  frame needs the two extra names CN1_FRAMELESS_TRY_FRAME defines. */
    private boolean framelessHasTryCatch;

    /** @return true if a return in this method must unwind the try-block stack itself. */
    public boolean isFramelessWithTryCatch() {
        return framelessHasTryCatch;
    }

    private int rawFramelessEligibility = -1;
    private boolean rawFramelessWithoutHandlers;

    /** Preserve the bytecode proof before native lowering introduces opaque C instructions. */
    void freezeFramelessEligibility() {
        if (rawFramelessEligibility < 0) {
            rawFramelessEligibility = isFramelessEligibleImpl(false) ? 1 : 0;
            if (FRAMELESS_CENSUS && rawFramelessEligibility == 0) {
                rawFramelessWithoutHandlers = isFramelessEligibleImpl(true);
            }
        }
    }

    private boolean isFramelessEligible() {
        boolean r = rawFramelessEligibility < 0
                ? isFramelessEligibleImpl(false) : rawFramelessEligibility != 0;
        if (FRAMELESS_CENSUS && !nativeMethod && !abstractMethod && !eliminated) {
            censusTotal++;
            if (r) {
                censusEligible++;
            } else if (rawFramelessEligibility < 0
                    ? isFramelessEligibleImpl(true) : rawFramelessWithoutHandlers) {
                // Would be eligible if try/catch alone were not disqualifying.
                censusExcludedTryCatch++;
            } else {
                censusExcludedOther++;
            }
        }
        return r;
    }

    /// @param ignoreTryCatch when true, a TryCatch range does not disqualify -- used
    ///                       only by the census above to attribute the exclusion
    private boolean isFramelessEligibleImpl(boolean ignoreTryCatch) {
        if (!FRAMELESS_ENABLED) {
            return false;
        }
        if (nativeMethod || abstractMethod || eliminated || synchronizedMethod || onDeviceDebug) {
            if (FRAMELESS_CENSUS && !ignoreTryCatch) {
                censusReason(synchronizedMethod ? 1 : (onDeviceDebug ? 2 : 4));
            }
            return false;
        }
        // PHASE 3b: object args/return/locals are allowed only when object-frameless is on
        // (their roots are then found by the conservative native-stack scan); otherwise the
        // method must be primitive-only (the prior, GC-trivial frameless contract).
        final boolean obj = FRAMELESS_OBJECTS_ENABLED;
        if (!staticMethod) {
            // Instance methods are eligible only under object mode: their receiver
            // `this` (__cn1ThisObject, a C parameter) is an object root found by the
            // conservative native-stack scan. Constructors are deferred (super-call /
            // field-init / partially-constructed-receiver semantics need more care).
            // PHASE 3c: CONSTRUCTORS ARE FRAMELESS TOO. They were the single largest
            // remaining exclusion by a wide margin -- the census counted 3,850 of 23,445
            // methods against 357 for try/catch -- and the exclusion was a DEFERRAL
            // ("super-call / field-init / partially-constructed-receiver semantics need
            // more care"), never a proof. A constructor's receiver is a C parameter
            // exactly like any instance method's, and the conservative native-stack scan
            // finds it whether its fields are written yet or not: allocation zeroes the
            // object before the constructor is entered, so a partially built receiver
            // traces as itself plus null fields, never as garbage.
            //
            // __CLINIT__ stays excluded, and that is a semantic criterion rather than
            // caution: a static initializer is entered through the class-init guard and
            // can re-enter arbitrary other clinits.
            if (!obj || !FRAMELESS_INSTANCE_ENABLED || methodName.equals("__CLINIT__")) {
                if (FRAMELESS_CENSUS && !ignoreTryCatch) {
                    censusReason(0);
                }
                return false;
            }
        }
        if (!returnType.isVoid() && returnType.getQualifier() == 'o' && !obj) {
            return false;
        }
        for (ByteCodeMethodArg arg : arguments) {
            if (arg.getQualifier() == 'o' && !obj) {
                return false;
            }
        }
        for (LocalVariable lv : localVariables) {
            if (lv.getQualifier() == 'o' && !obj) {
                return false;
            }
        }
        boolean hasRealInstruction = false;
        for (Instruction i : instructions) {
            if (i instanceof TryCatch) {
                // A try/catch used to disqualify the method outright -- the last of the
                // three guardrails it controlled, and like the other two a DEFERRAL
                // rather than a proof. What a frameless frame lacks is two NAMES the
                // exception macros use (methodBlockOffset and
                // currentCodenameOneCallStackOffset); CN1_FRAMELESS_TRY_FRAME supplies
                // both, emitted only into the methods that need them. The setjmp
                // requirement was already met independently: volatileLocals is set by
                // the same instruction scan and selects the _VSP frame variant, so the
                // locals and SP are volatile here exactly as they are in an ordinary
                // frame.
                //
                // ignoreTryCatch is now only the census' way of asking the old question.
                continue;
            }
            if (i instanceof LabelInstruction || i instanceof LineNumber || i instanceof LocalVariable) {
                continue;
            }
            if (i instanceof Field) {
                // GETFIELD/PUTFIELD/GETSTATIC/PUTSTATIC are SP-based; safe under object mode.
                if (!obj) {
                    return censusBlocked(i, ignoreTryCatch);
                }
                hasRealInstruction = true;
                continue;
            }
            if (i instanceof TypeInstruction) {
                if (!obj) {
                    return censusBlocked(i, ignoreTryCatch);
                }
                switch (i.getOpcode()) {
                    case Opcodes.NEW:
                    case Opcodes.ANEWARRAY:
                    case Opcodes.CHECKCAST:
                    case Opcodes.INSTANCEOF:
                        hasRealInstruction = true;
                        continue;
                    default:
                        return censusBlocked(i, ignoreTryCatch);
                }
            }
            if (i instanceof MultiArray) {
                return censusBlocked(i, ignoreTryCatch); // MULTIANEWARRAY -- deferred (expand later)
            }
            if (i instanceof IInc) {
                hasRealInstruction = true;
                continue;
            }
            if (i instanceof SwitchInstruction) {
                // TABLESWITCH / LOOKUPSWITCH on an int key -- primitive, safe.
                hasRealInstruction = true;
                continue;
            }
            if (i instanceof VarOp) {
                switch (i.getOpcode()) {
                    case Opcodes.ILOAD:
                    case Opcodes.LLOAD:
                    case Opcodes.FLOAD:
                    case Opcodes.DLOAD:
                    case Opcodes.ISTORE:
                    case Opcodes.LSTORE:
                    case Opcodes.FSTORE:
                    case Opcodes.DSTORE:
                    // BIPUSH/SIPUSH parse as VarOp (visitIntInsn), NOT BasicInstruction:
                    // without these two cases ANY method containing an int constant in
                    // 6..32767 was silently disqualified from frameless codegen -- an
                    // enormous accidental exclusion (a `& 1023` mask or a char literal
                    // was enough). They just push an int constant; trivially safe.
                    case Opcodes.BIPUSH:
                    case Opcodes.SIPUSH:
                        hasRealInstruction = true;
                        continue;
                    case Opcodes.NEWARRAY:
                    case Opcodes.ALOAD:
                    case Opcodes.ASTORE:
                        // Object-mode-only ops sharing one gate: NEWARRAY (a VarOp via
                        // visitIntInsn) allocates a primitive array and pushes a fresh
                        // object ref onto the local operand array -- safe under object
                        // mode exactly like NEW/ANEWARRAY -- while ALOAD/ASTORE touch an
                        // object local slot that lives in the method-local frame array,
                        // scanned conservatively.
                        if (!obj) {
                            return censusBlocked(i, ignoreTryCatch);
                        }
                        hasRealInstruction = true;
                        continue;
                    default:
                        return censusBlocked(i, ignoreTryCatch);
                }
            }
            if (i instanceof Jump) {
                switch (i.getOpcode()) {
                    case Opcodes.IFEQ:
                    case Opcodes.IFNE:
                    case Opcodes.IFLT:
                    case Opcodes.IFGE:
                    case Opcodes.IFGT:
                    case Opcodes.IFLE:
                    case Opcodes.IF_ICMPEQ:
                    case Opcodes.IF_ICMPNE:
                    case Opcodes.IF_ICMPLT:
                    case Opcodes.IF_ICMPGE:
                    case Opcodes.IF_ICMPGT:
                    case Opcodes.IF_ICMPLE:
                    case Opcodes.GOTO:
                        hasRealInstruction = true;
                        continue;
                    case Opcodes.IF_ACMPEQ:
                    case Opcodes.IF_ACMPNE:
                    case Opcodes.IFNULL:
                    case Opcodes.IFNONNULL:
                        if (!obj) {
                            return censusBlocked(i, ignoreTryCatch);
                        }
                        hasRealInstruction = true;
                        continue;
                    default:
                        return censusBlocked(i, ignoreTryCatch); // JSR
                }
            }
            if (i instanceof Ldc) {
                Object v = ((Ldc) i).getValue();
                if (v instanceof Integer || v instanceof Long || v instanceof Float || v instanceof Double) {
                    hasRealInstruction = true;
                    continue;
                }
                if (obj) {
                    // String / Class / Type constant -> object pushed onto the local
                    // operand array; a constant-pool object, kept alive independently.
                    hasRealInstruction = true;
                    continue;
                }
                return censusBlocked(i, ignoreTryCatch);
            }
            if (i instanceof Invoke) {
                if (obj) {
                    // Any invoke (virtual/special/interface/static/dynamic, any descriptor):
                    // arguments are popped from the local SP and passed as explicit C params,
                    // so the call is self-contained on the method-local operand array.
                    hasRealInstruction = true;
                    continue;
                }
                if (i.getOpcode() != Opcodes.INVOKESTATIC) {
                    return censusBlocked(i, ignoreTryCatch);
                }
                if (!isPrimitiveOnlyDescriptor(((Invoke) i).getDesc())) {
                    return censusBlocked(i, ignoreTryCatch);
                }
                hasRealInstruction = true;
                continue;
            }
            if (i instanceof BasicInstruction) {
                if (isFramelessBasicOpcode(i.getOpcode())) {
                    hasRealInstruction = true;
                    continue;
                }
                if (obj && isFramelessObjectOpcode(i.getOpcode())) {
                    hasRealInstruction = true;
                    continue;
                }
                return censusBlocked(i, ignoreTryCatch);
            }
            // Any other instruction type (CustomInvoke/CustomJump/CustomIntruction/
            // ArithmeticExpression are produced only by optimize and must not appear
            // here; anything else is unrecognized) -> conservatively ineligible.
            if (FRAMELESS_CENSUS && !ignoreTryCatch) {
                censusReason(3);
            }
            return censusBlocked(i, ignoreTryCatch);
        }
        if (FRAMELESS_CENSUS && !ignoreTryCatch && !hasRealInstruction) {
            censusReason(4);
        }
        return hasRealInstruction;
    }

    public BytecodeMethod(String clsName, int access, String name, String desc, String signature, String[] exceptions) {
        methodName = name;
        this.clsName = clsName;
        this.desc = desc;
        privateMethod = (access & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE;
        nativeMethod = (access & Opcodes.ACC_NATIVE) == Opcodes.ACC_NATIVE;
        staticMethod = (access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
        finalMethod = (access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL;
        synchronizedMethod = (access & Opcodes.ACC_SYNCHRONIZED) == Opcodes.ACC_SYNCHRONIZED;
        abstractMethod = (access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT;
        int pos = desc.lastIndexOf(')');
        if (!staticMethod) {
            if (!dependentClasses.contains("java_lang_NullPointerException")) {
                dependentClasses.add("java_lang_NullPointerException");
            }
        } // 
        if(methodName.equals("<init>")) {
            methodName = "__INIT__";
            constructor = true;
            returnType = new ByteCodeMethodArg(PrimitiveType.VOID, 0);
        } else {
            if(methodName.equals("<clinit>")) {
                methodName = "__CLINIT__";
                returnType = new ByteCodeMethodArg(PrimitiveType.VOID, 0);
                staticMethod = true;
            } else {            
                String retType = desc.substring(pos + 1);
                if(retType.equals("V")) {
                    returnType = new ByteCodeMethodArg(PrimitiveType.VOID, 0);
                } else {
                    int dim = 0;
                    while(retType.startsWith("[")) {
                        retType = retType.substring(1);
                        dim++;
                    }
                    char currentType = retType.charAt(0);
                    switch(currentType) {
                        case 'L':
                            // Object skip until ;
                            int idx = retType.indexOf(';');
                            String objectType = retType.substring(1, idx);
                            objectType = objectType.replace('/', '_').replace('$', '_');
                            if(!dependentClasses.contains(objectType)) {
                                dependentClasses.add(objectType);
                            }
                            //if (!this.isPrivate() && !exportedClasses.contains(objectType)) {
                            //    exportedClasses.add(objectType);
                            //}
                            returnType = new ByteCodeMethodArg(objectType, dim);
                            break;
                        case 'I':
                            returnType = new ByteCodeMethodArg(PrimitiveType.INT, dim);
                            break;
                        case 'J':
                            returnType = new ByteCodeMethodArg(PrimitiveType.LONG, dim);
                            break;
                        case 'B':
                            returnType = new ByteCodeMethodArg(PrimitiveType.BYTE, dim);
                            break;
                        case 'S':
                            returnType = new ByteCodeMethodArg(PrimitiveType.SHORT, dim);
                            break;
                        case 'F':
                            returnType = new ByteCodeMethodArg(PrimitiveType.FLOAT, dim);
                            break;
                        case 'D':
                            returnType = new ByteCodeMethodArg(PrimitiveType.DOUBLE, dim);
                            break;
                        case 'Z':
                            returnType = new ByteCodeMethodArg(PrimitiveType.BOOLEAN, dim);
                            break;
                        case 'C':
                            returnType = new ByteCodeMethodArg(PrimitiveType.CHAR, dim);
                            break;
                    }
                }
            }
        }
        int currentArrayDim = 0;
        desc = desc.substring(1, pos);
        for(int i = 0 ; i < desc.length() ; i++) {
            char currentType = desc.charAt(i);
            switch(currentType) {
                case '[':
                    // array of...
                    currentArrayDim++;
                    continue;
                case 'L':
                    // Object skip until ;
                    int idx = desc.indexOf(';', i);
                    String objectType = desc.substring(i + 1, idx);
                    objectType = objectType.replace('/', '_').replace('$', '_');
                    if(!dependentClasses.contains(objectType)) {
                        dependentClasses.add(objectType);
                    }
                    //if (!this.isPrivate() && !exportedClasses.contains(objectType)) {
                    //    exportedClasses.contains(objectType);
                    //}
                    i = idx;
                    arguments.add(new ByteCodeMethodArg(objectType, currentArrayDim));
                    break;
                case 'I':
                    arguments.add(new ByteCodeMethodArg(PrimitiveType.INT, currentArrayDim));
                    break;
                case 'J':
                    arguments.add(new ByteCodeMethodArg(PrimitiveType.LONG, currentArrayDim));
                    break;
                case 'B':
                    arguments.add(new ByteCodeMethodArg(PrimitiveType.BYTE, currentArrayDim));
                    break;
                case 'S':
                    arguments.add(new ByteCodeMethodArg(PrimitiveType.SHORT, currentArrayDim));
                    break;
                case 'F':
                    arguments.add(new ByteCodeMethodArg(PrimitiveType.FLOAT, currentArrayDim));
                    break;
                case 'D':
                    arguments.add(new ByteCodeMethodArg(PrimitiveType.DOUBLE, currentArrayDim));
                    break;
                case 'Z':
                    arguments.add(new ByteCodeMethodArg(PrimitiveType.BOOLEAN, currentArrayDim));
                    break;
                case 'C':
                    arguments.add(new ByteCodeMethodArg(PrimitiveType.CHAR, currentArrayDim));
                    break;
            }
            currentArrayDim = 0;
        }

        if (dependencyGraph != null) {
            dependencyGraph.registerMethod(this);
        }
    }

    // use this instead of isMethodUsed to compare traditional with new results
    public boolean isMethodUsedTester(BytecodeMethod bm)
    {
        boolean oldway = isMethodUsedOldWay(bm);
        boolean newway = isMethodUsed(bm);
        if(oldway!=newway)
        	{ throw new Error("different result"); 
        	}
        return newway;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendFunctionPointer(sb);
        return sb.toString();
    }
    
    
    
    
    private Hashtable<String,SignatureSet> usedSigs;
    
    // [ddyer 4/2017] avoid creating a lot of temporary objects. 
    // more than 3x faster than the old way.
    public boolean isMethodUsed(BytecodeMethod bm0) {
    	SignatureSet bm = (SignatureSet)bm0;
        if(usedSigs == null) {
        	usedSigs = new Hashtable<String,SignatureSet>();
            for(Instruction ins : instructions) {
            	String sname = ins.getMethodName();
            	if(sname!=null)
            	{
            		SignatureSet ss = usedSigs.get(sname);
            		// either use the instruction itself, or create a set of them
            		ss = ss==null ? ins : new MultipleSignatureSet((SignatureSet)ins,ss); 
            		usedSigs.put(sname,ss);
            	}
            }
        }
        String name = bm.getMethodName();
        SignatureSet ss = usedSigs.get("__INIT__".equals(name)?"<init>":name);
        return ((ss==null) ? false : ss.containsSignature(bm));
    }

    /**
     * Flag to indicate whether this method is used by native sources.
     */
    private boolean usedByNative;

    /**
     * Internal use: to track the list of native sources that were used to calculate the
     * usedByNative flag.
     */
    private String[] usedByNativeSources;

    /**
     * Checks to see if this method is used by any of the provided native sources.
     * @param nativeSources The native sources to check.
     * @param cls The class that the method belongs to.  This is used to improve performance by first checking to
     *            see if the class is Not referenced in native sources.  If the class is not referenced, then we know that
     *            neither is the method.  This method will also set the {@link ByteCodeClass#setUsedByNative(boolean)} flag
     *            to improve the performance for the next method that is checked in the same class.
     * @return True if the method is used by native.
     */
    public boolean isMethodUsedByNative(String[] nativeSources, ByteCodeClass cls) {
        if (nativeSources == null) return false;
        if (nativeSources == usedByNativeSources) {
            return usedByNative;
        }
        usedByNativeSources = nativeSources;

        if (cls != null && cls.getUsedByNative() == ByteCodeClass.UsedByNativeResult.Unused) {
            // If the class isn't used, then neither is the method.
            usedByNative = false;
            return false;
        }



        // check native code: O(|symbol|) lookups against the inverted index instead
        // of an O(native_bytes) substring scan per method. Semantics are identical --
        // the index answers "is X a substring of the native source text".
        StringBuilder b = new StringBuilder();
        this.appendFunctionPointer(b);
        String str = b.toString();
        NativeSymbolIndex idx = Parser.getNativeSymbolIndex(nativeSources);
        if (idx.contains(str)) {
            usedByNative = true;
            if (cls != null) {
                cls.setUsedByNative(true);
            }
            return true;
        }
        if (cls != null && !idx.contains(clsName)) {
            // We didn't find the class at all.
            // Let's record that as it will save us time
            // when looking up other methods in this class.
            cls.setUsedByNative(false);
        }
        usedByNative = false;
        return false;
    }
    
    private Set<String> usedMethods;
    public boolean isMethodUsedOldWay(BytecodeMethod bm) {
        ensureUsedMethodsInitialized();
        if(bm.methodName.equals("__INIT__")) {
            return usedMethods.contains(bm.desc + ".<init>");
        }
        return usedMethods.contains(bm.desc + "." + bm.methodName);
    }

    public Set<String> getCalledMethodSignatures() {
        ensureUsedMethodsInitialized();
        return usedMethods;
    }

    public String getLookupSignature() {
        if(methodName.equals("__INIT__")) {
            return desc + ".<init>";
        }
        if(methodName.equals("__CLINIT__")) {
            return desc + ".<clinit>";
        }
        return desc + "." + methodName;
    }

    private void ensureUsedMethodsInitialized() {
        if(usedMethods != null) {
            return;
        }
        usedMethods = new TreeSet<String>();
        for(Instruction ins : instructions) {
            String s = ins.getMethodUsed();
            if(s != null && !usedMethods.contains(s)) {
                usedMethods.add(s);
            }
        }
    }
    
    public void findWritableFields(Set<String> outSet) {
        int len = instructions.size();
        for (int i=0; i<len; i++) {
            Instruction instr = instructions.get(i);
            if (instr instanceof Field) {
                
            }
        }
    }
    
    public static String appendMethodSignatureSuffixFromDesc(String desc, StringBuilder b, List<String> arguments) {
        int currentArrayDim = 0;
        desc = desc.substring(1);
        boolean returnVal = false;
        String returnType = null;
        for(int i = 0 ; i < desc.length() ; i++) {
            char currentType = desc.charAt(i);
            switch(currentType) {
                // return type parsing, and void return type
                case ')':
                case 'V':
                    returnVal = true;
                    continue;
                case '[':
                    // array of...
                    currentArrayDim++;
                    continue;
                case 'L':
                    if(!returnVal) {
                        arguments.add("o");
                    } else {
                        b.append("_R");
                        returnType = "JAVA_OBJECT"; 
                    }
                    // Object skip until ;
                    int idx = desc.indexOf(';', i);
                    String objectType = desc.substring(i + 1, idx);
                    objectType = objectType.replace('/', '_').replace('$', '_');
                    i = idx;
                    b.append("_");
                    b.append(objectType);
                    break;
                case 'I':
                    if(!returnVal) {
                        arguments.add("i");
                    } else {
                        b.append("_R");
                        returnType = "JAVA_INT"; 
                    }
                    b.append("_int");
                    break;
                case 'J':
                    if(!returnVal) {
                        arguments.add("l");
                    } else {
                        b.append("_R");
                        returnType = "JAVA_LONG"; 
                    }
                    b.append("_long");
                    break;
                case 'B':
                    if(!returnVal) {
                        arguments.add("i");
                    } else {
                        b.append("_R");
                        returnType = "JAVA_INT"; 
                    }
                    b.append("_byte");
                    break;
                case 'S':
                    if(!returnVal) {
                        arguments.add("i");
                    } else {
                        b.append("_R");
                        returnType = "JAVA_INT"; 
                    }
                    b.append("_short");
                    break;
                case 'F':
                    if(!returnVal) {
                        arguments.add("f");
                    } else {
                        b.append("_R");
                        returnType = "JAVA_FLOAT"; 
                    }
                    b.append("_float");
                    break;
                case 'D':
                    if(!returnVal) {
                        arguments.add("d");
                    } else {
                        returnType = "JAVA_DOUBLE"; 
                        b.append("_R");
                    }
                    b.append("_double");
                    break;
                case 'Z':
                    if(!returnVal) {
                        arguments.add("i");
                    } else {
                        returnType = "JAVA_INT"; 
                        b.append("_R");
                    }
                    b.append("_boolean");
                    break;
                case 'C':
                    if(!returnVal) {
                        arguments.add("i");
                    } else {
                        b.append("_R");
                        returnType = "JAVA_INT"; 
                    }
                    b.append("_char");
                    break;
            }
            if(currentArrayDim > 0) {
                if(!returnVal) {
                    arguments.remove(arguments.size() - 1);
                    arguments.add("o");
                } else {
                    returnType = "JAVA_OBJECT";                    
                }
                b.append("_");
                b.append(currentArrayDim);
                b.append("ARRAY");
            }
            currentArrayDim = 0;
        }
        return returnType;
    }
    
    public List<String> getDependentClasses() {
        return dependentClasses;
    }

    /**
     * Late dependency re-scan for the trivial-accessor inlining
     * (Invoke.asInlinableFieldAccess): the swapped-in GETSTATIC/GETFIELD
     * references the FIELD's declaring class directly, which can differ from
     * the invoke's owner (the lazy-holder idiom: Border.getEmpty() reads
     * Border$EmptyBorderHolder.EMPTY). Instruction addDependencies runs at
     * PARSE time, when the target class may not be loaded and the fold can't
     * be resolved -- so, like the @Concrete re-scan, this runs from
     * ByteCodeClass.updateAllDependencies when every class has been parsed.
     * Without it the caller's include list misses the field owner's header
     * and the generated C does not compile.
     */
    public void updateInlinableFieldDependencies() {
        for (Instruction i : instructions) {
            if (i instanceof Invoke) {
                ((Invoke) i).addResolvedDependencies(dependentClasses);
                Field folded = ((Invoke) i).asInlinableFieldAccess();
                if (folded != null) {
                    folded.addDependencies(dependentClasses);
                }
            }
        }
    }
    
    //public List<String> getExportedClasses() {
    //    return exportedClasses;
    //}
    
    private void appendCMethodPrefix(StringBuilder b, String prefix) {
        appendCMethodPrefix(b, prefix, clsName);
    }
    
    private void appendCMethodPrefix(StringBuilder b, String prefix, String clsName) {
        appendCMethodPrefix("\n", "", b, prefix, clsName);
    }
    
    public void appendArgumentTypes(StringBuilder b) {
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(b);
        }
        if(!returnType.isVoid()) {
            b.append("_R");
            returnType.appendCMethodExt(b);
        }
    }
    
    private void appendCMethodPrefix(String before, String after, StringBuilder b, String prefix, String clsName) {
        b.append(before);
        returnType.appendCSig(b);
        b.append(prefix);
        b.append(clsName);
        b.append("_");
        b.append(getCMethodName());
        b.append("__");
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(b);
        }
        if(!returnType.isVoid()) {
            b.append("_R");
            returnType.appendCMethodExt(b);
        }
        b.append(after);
        b.append("(CODENAME_ONE_THREAD_STATE");
        int arg = 1;
        if(!staticMethod) {
            b.append(", ");
            new ByteCodeMethodArg(clsName, 0).appendCSig(b);
            b.append(" __cn1ThisObject");
        }
        for(ByteCodeMethodArg args : arguments) {
            b.append(", ");
            args.appendCSig(b);
            b.append("__cn1Arg");
            b.append(arg);
            arg++;
        }        
        b.append(")");
    }
    
    public void addToConstantPool() {
        for(Instruction i : instructions) {
            i.addToConstantPool();
        }
    }
    
    public boolean isSynchronizedMethod() {
        return synchronizedMethod;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public List<ByteCodeMethodArg> getArguments() {
        return arguments;
    }

    public ByteCodeMethodArg getReturnType() {
        return returnType;
    }

    public int getMaxStack() {
        return maxStack;
    }

    public int getMaxLocals() {
        return maxLocals;
    }

    public boolean isConstructor() {
        return constructor;
    }

    // LEVER B (perf-tier1): the inlinable-constructor plan, computed ONCE from the RAW
    // instruction list at parse time (Parser.MethodVisitorWrapper.visitEnd, before any
    // optimize() rewrites PUTFIELDs into folded Field ops). A new-site in another class
    // looks this up at emit time -- decoupling the analysis (raw, deterministic) from
    // the cross-class emission order. null == not an inlinable ctor.
    private com.codename1.tools.translator.bytecodes.InlinableConstructor inlinableCtorPlan;
    private boolean rawPlansComputed;

    /**
     * Snapshots every analysis whose answer must be read off the RAW instruction
     * list. Called from Parser.MethodVisitorWrapper.visitEnd, before any
     * optimize() folds opcodes into custom instructions -- so that a pass running
     * over one class cannot get a different answer about another class depending
     * on which of the two was optimized first.
     */
    public void computeRawMethodPlans() {
        if (rawPlansComputed) {
            return;
        }
        rawPlansComputed = true;
        if (constructor) {
            inlinableCtorPlan = com.codename1.tools.translator.bytecodes.InlinableConstructor.analyzeRaw(this, desc);
            // FUSED OBJECTS: snapshot the fused-construction plan from the same RAW
            // list (the class-level @Fused gate is applied by the users of the plan;
            // the shape analysis is cheap and most ctors bail on the first check).
            fusedCtorPlan = com.codename1.tools.translator.bytecodes.FusedConstructor.analyzeRaw(this, desc);
            // SCALAR REPLACEMENT: same reason, same RAW list -- see srAnalyzeCtor.
            analyzeScalarConstructorRaw();
        }
        // SCALAR REPLACEMENT: trivial getters are folded into a struct member read
        // at their CALL sites, in other classes. Same rule as the plans above --
        // recognize the shape on the raw list, never on the live one.
        analyzeTrivialGetterRaw();
        // ON-DEVICE DEBUG: a local's declaring scope is a pair of labels, and
        // turning those into source lines means walking the instruction list.
        // Two consumers read the result at different times -- the frame
        // side-table during code generation, the symbol table the IDE reads
        // after every class has been generated -- with optimize() in between.
        // It happens to preserve the label and line-number instructions this
        // walk depends on, so resolving on demand would agree today; nothing
        // states or enforces that, and the two disagreeing would silently drop
        // locals from the IDE's variables view. Snapshotting off the raw list
        // removes the dependence, and costs one walk per method instead of one
        // per method per consumer.
        snapshotDebugVarScopes();
    }

    /**
     * Re-snapshots the raw plans after a pass that runs on the raw list edited it
     * (DeadFieldElimination). Every analysis above resets its own state first.
     */
    public void recomputeRawMethodPlans() {
        rawPlansComputed = false;
        computeRawMethodPlans();
    }

    public com.codename1.tools.translator.bytecodes.InlinableConstructor getInlinableConstructorPlan() {
        return inlinableCtorPlan;
    }

    private com.codename1.tools.translator.bytecodes.FusedConstructor fusedCtorPlan;

    public com.codename1.tools.translator.bytecodes.FusedConstructor getFusedConstructorPlan() {
        return fusedCtorPlan;
    }

    public String getMethodIdentifier() {
        StringBuilder b = new StringBuilder();
        b.append(clsName).append("_");
        if(methodName.equals("<init>")) {
            b.append("__INIT__");
        } else if(methodName.equals("<clinit>")) {
            b.append("__CLINIT__");
        } else {
            b.append(getCMethodName());
        }
        appendMethodSignatureSuffixFromDesc(desc, b, new ArrayList<String>());
        return b.toString();
    }
    
    private boolean hasLocalVariableWithIndex(char qualifier, int index) {
        for (LocalVariable lv : localVariables) {
            if (lv.getIndex() == index && lv.getQualifier() == qualifier) {
                return true;
            }
        }
        return false;
    }

    /**
     * The locals the on-device-debug side-table describes, in a stable order
     * shared by {@link #appendFrameInfoStruct} (which emits each row's slot and
     * type) and {@link #appendLocalsAddressTable} (which emits the matching
     * storage address). Row <em>i</em> of one is row <em>i</em> of the other.
     *
     * Sorted rather than taken in {@link #localVariables} iteration order: that
     * is a {@code HashSet}, so the order varies between builds of the same
     * input, which used to make the emitted table non-deterministic.
     *
     * Locals whose slot lies outside the frame are dropped — there is no
     * storage to point at, and keeping them would only hand the debugger an
     * address that indexes past {@code locals[]}.
     */
    public List<LocalVariable> debugVarEntries() {
        List<LocalVariable> rows = new ArrayList<LocalVariable>();
        for (LocalVariable lv : localVariables) {
            int idx = lv.getIndex();
            if (idx >= 0 && idx < maxLocals) {
                rows.add(lv);
            }
        }
        Collections.sort(rows, DEBUG_VAR_ORDER);
        return rows;
    }

    /**
     * Every local, in a deterministic order, for emitting the C declarations.
     *
     * Unlike {@link #debugVarEntries} this drops nothing: a local whose slot lies
     * outside the frame still needs its declaration, it just has no debug row.
     */
    private List<LocalVariable> declarationOrderedLocals() {
        List<LocalVariable> ordered = new ArrayList<LocalVariable>(localVariables);
        Collections.sort(ordered, DEBUG_VAR_ORDER);
        return ordered;
    }

    /** Slot first, then storage qualifier, so a reused slot's rows stay adjacent. */
    private static final Comparator<LocalVariable> DEBUG_VAR_ORDER = new Comparator<LocalVariable>() {
        @Override
        public int compare(LocalVariable a, LocalVariable b) {
            if (a.getIndex() != b.getIndex()) {
                return a.getIndex() - b.getIndex();
            }
            if (a.getQualifier() != b.getQualifier()) {
                return a.getQualifier() - b.getQualifier();
            }
            // Two declarations can share both: disjoint blocks reusing a slot
            // for the same type. Declaration order breaks the tie, so the
            // emitted table does not depend on hash order.
            return a.getSequence() - b.getSequence();
        }
    };

    /**
     * Maps each label in this method to the source line in effect there.
     *
     * javac emits a label and then the line number attached to it, so a label
     * takes the line of the next {@code LineNumber} that follows it — several
     * labels can share one line. Labels that no line follows (a trailing scope
     * close, typically) keep the last line seen, which is the right answer for
     * a scope that runs to the end of the method.
     */
    private Map<Label, Integer> lineByLabel() {
        Map<Label, Integer> lines = new HashMap<Label, Integer>();
        List<Label> pending = new ArrayList<Label>();
        for (Instruction i : instructions) {
            if (i instanceof LabelInstruction) {
                pending.add(((LabelInstruction) i).getLabel());
            } else if (i instanceof LineNumber) {
                int currentLine = ((LineNumber) i).getLine();
                for (Label pendingLabel : pending) {
                    lines.put(pendingLabel, currentLine);
                }
                pending.clear();
            }
        }
        // Labels with no line after them sit past the last statement — which is
        // where javac puts the end label of every method-wide local, `this` and
        // the parameters included. Giving them the last line would make an
        // exclusive scope end *on* that line and hide those locals at exactly
        // the breakpoint most likely to be set. They run to the end instead.
        for (Label trailing : pending) {
            lines.put(trailing, END_OF_METHOD);
        }
        return lines;
    }

    /** Marks a label that no source line follows; see {@link #lineByLabel()}. */
    private static final int END_OF_METHOD = Integer.MAX_VALUE;

    /**
     * The source-line range a local is in scope for, as {@code {start, end}}.
     *
     * {@code {0, 0}} means "always live": either the class file carried no
     * scope for it, or the translator synthesised the local from a store
     * opcode. The end is exclusive — a local declared at line 10 in a block
     * closing at line 14 is live for lines 10 through 13.
     */
    private static final int[] ALWAYS_LIVE = { 0, 0 };

    /**
     * Scopes resolved at parse time, keyed by local. Null until
     * {@link #computeRawMethodPlans()} has run — methods built by hand rather
     * than by the parser fall back to resolving on demand.
     */
    private Map<LocalVariable, int[]> debugVarScopeSnapshot;

    /** Resolves every local's scope against the raw instruction list, once. */
    private void snapshotDebugVarScopes() {
        if (localVariables.isEmpty()) {
            return;
        }
        Map<Label, Integer> lineByLabel = lineByLabel();
        Map<LocalVariable, int[]> snapshot = new HashMap<LocalVariable, int[]>();
        for (LocalVariable lv : localVariables) {
            snapshot.put(lv, resolveDebugVarScope(lv, lineByLabel));
        }
        debugVarScopeSnapshot = snapshot;
    }

    /**
     * The source-line ranges for {@code rows}, in the same order.
     *
     * Served from the parse-time snapshot so that the frame side-table emitted
     * during code generation and the symbol table written after it describe
     * the same local the same way.
     */
    public List<int[]> debugVarScopes(List<LocalVariable> rows) {
        List<int[]> scopes = new ArrayList<int[]>(rows.size());
        Map<LocalVariable, int[]> snapshot = debugVarScopeSnapshot;
        Map<Label, Integer> lineByLabel = snapshot == null ? lineByLabel() : null;
        for (LocalVariable lv : rows) {
            int[] scope = snapshot == null ? null : snapshot.get(lv);
            scopes.add(scope != null ? scope : resolveDebugVarScope(lv, lineByLabel == null
                    ? lineByLabel() : lineByLabel));
        }
        return scopes;
    }

    private int[] resolveDebugVarScope(LocalVariable lv, Map<Label, Integer> lineByLabel) {
        Label start = lv.getScopeStart();
        Label end = lv.getScopeEnd();
        if (start == null || end == null) {
            return ALWAYS_LIVE;
        }
        Integer startLine = lineByLabel.get(start);
        Integer endLine = lineByLabel.get(end);
        if (startLine == null || endLine == null || startLine <= 0
                || startLine == END_OF_METHOD) {
            return ALWAYS_LIVE;
        }
        // Open-ended only when the scope really does run to the end of the
        // method. A scope whose end resolves at or before its start is one
        // that opens and closes on a single line; giving that an open end
        // would leave the local visible for the rest of the method and let it
        // collide with a later declaration reusing its slot. It gets the one
        // line it occupies instead — an empty range would hide it everywhere.
        if (endLine == END_OF_METHOD) {
            return new int[] { startLine, 0 };
        }
        if (endLine <= startLine) {
            return new int[] { startLine, startLine + 1 };
        }
        return new int[] { startLine, endLine };
    }

    /**
     * The C expression naming the storage ParparVM emitted for one local.
     *
     * Object locals live in the frame's {@code locals[]} array (the GC scans
     * it); primitives are plain C autos named by qualifier and slot. Both
     * spellings can exist for the same slot when disjoint scopes reuse it with
     * different types, which is exactly why this resolves per local rather than
     * per slot.
     */
    private static String debugVarAddress(LocalVariable lv) {
        char q = lv.getQualifier();
        if (q == 'o') {
            return "&locals[" + lv.getIndex() + "].data.o";
        }
        return "&" + q + "locals_" + lv.getIndex() + "_";
    }

    /**
     * Emits a stack-allocated {@code void*} array holding the address backing
     * each row of this method's variable side-table, then publishes it (and the
     * static frame info) into the per-frame slots the debugger thread reads.
     *
     * Indexed by side-table row, so a row's {@code typeCode} always describes
     * the storage its address points at. The previous per-slot form could not:
     * a slot reused by an {@code int} and a reference has one address but two
     * rows, so the reference row read eight bytes out of a four-byte
     * {@code JAVA_INT} and the debugger then dereferenced the result.
     *
     * The frame-info pointer is published even when there are no rows, so the
     * frame never inherits the previous occupant's metadata.
     *
     * Only called from the non-barebone path; barebone methods carry no
     * locals and bypass this entirely.
     */
    private void appendLocalsAddressTable(StringBuilder b) {
        List<LocalVariable> rows = debugVarEntries();
        // Leading newline: callers may have left the cursor mid-line after
        // the "this" assignment when there are no other arguments, and the
        // preprocessor requires #ifdef to be the first non-whitespace token
        // on its line.
        b.append("\n#ifdef CN1_ON_DEVICE_DEBUG\n");
        if (rows.isEmpty()) {
            b.append("    threadStateData->callStackLocalsAddresses[threadStateData->callStackOffset - 1] = 0;\n");
        } else {
            b.append("    void* __cn1_local_addrs[").append(rows.size()).append("] = { ");
            for (int i = 0; i < rows.size(); i++) {
                if (i > 0) {
                    b.append(", ");
                }
                b.append(debugVarAddress(rows.get(i)));
            }
            b.append(" };\n");
            b.append("    threadStateData->callStackLocalsAddresses[threadStateData->callStackOffset - 1] = __cn1_local_addrs;\n");
        }
        b.append("    threadStateData->callStackFrameInfo[threadStateData->callStackOffset - 1] = &__cn1_finfo_").append(getMethodIdentifier()).append(";\n");
        b.append("#endif\n");
    }

    /**
     * Emits the static per-method {@code cn1_frame_info} (and its inline
     * {@code cn1_var_entry[]} side-table). Held at file scope so the
     * per-frame pointer set up in {@link #appendLocalsAddressTable} stays
     * valid for the program's lifetime.
     *
     * Each row carries the source-line range its local is in scope for, so the
     * debugger can hide a local the code has not reached. Without it, two
     * locals sharing a slot in disjoint scopes both appear at every
     * breakpoint, one of them showing the contents of storage that belongs to
     * the other scope.
     */
    private void appendFrameInfoStruct(StringBuilder b) {
        String id = getMethodIdentifier();
        int classId = Parser.getClassOffset(clsName);
        int methodId = methodOffset;
        List<LocalVariable> rows = debugVarEntries();
        b.append("#ifdef CN1_ON_DEVICE_DEBUG\n");
        if (rows.isEmpty()) {
            b.append("static const struct cn1_frame_info __cn1_finfo_").append(id).append(" = {\n");
            b.append("    ").append(classId).append(", ").append(methodId).append(", ").append(maxLocals).append(", 0, 0\n");
            b.append("};\n");
        } else {
            List<int[]> scopes = debugVarScopes(rows);
            b.append("static const struct cn1_var_entry __cn1_vars_").append(id).append("[] = {\n");
            for (int ri = 0; ri < rows.size(); ri++) {
                LocalVariable lv = rows.get(ri);
                int[] scope = scopes.get(ri);
                b.append("    { ").append(scope[0]).append(", ").append(scope[1])
                 .append(", ").append(lv.getIndex())
                 .append(", '").append(lv.getTypeCode()).append("' },\n");
            }
            b.append("};\n");
            b.append("static const struct cn1_frame_info __cn1_finfo_").append(id).append(" = {\n");
            b.append("    ").append(classId).append(", ").append(methodId).append(", ").append(maxLocals).append(", ").append(rows.size()).append(", __cn1_vars_").append(id).append("\n");
            b.append("};\n");
        }
        b.append("#endif\n");
    }
    
    private void fixUpBarebone() {
        for (Instruction i : instructions) {
            if (i instanceof CustomJump) {
                CustomJump cj = (CustomJump)i;
                String cmp = cj.getCustomCompareCode();
                if (cmp != null) {
                    cj.setCustomCompareCode(Util.rewriteLocalObjectRefs(cmp));
                }
            } else if (i instanceof CustomIntruction) {
                CustomIntruction ci = (CustomIntruction)i;
                String code = ci.getCode();
                if (code != null) {
                    ci.setCode(Util.rewriteLocalObjectRefs(code));
                }
                String complexCode = ci.getComplexCode();
                if (complexCode != null) {
                    ci.setComplexCode(Util.rewriteLocalObjectRefs(complexCode));
                }
            } else if (i instanceof CustomInvoke) {
                CustomInvoke ci = (CustomInvoke)i;
                String target = ci.getTargetObjectLiteral();
                if (target != null) {
                    ci.setTargetObjectLiteral(Util.rewriteLocalObjectRefs(target));
                }
                String[] args = ci.getLiteralArgs();
                if (args != null) {
                    for (int j=0; j<args.length; j++) {
                        if (args[j] != null) {
                            ci.setLiteralArg(j, Util.rewriteLocalObjectRefs(args[j]));
                        }
                    }
                }
            }
        }
    }

    public void appendMethodC(StringBuilder b) {
        if(nativeMethod) {
            return;
        }
        if (onDeviceDebug && !eliminated) {
            appendFrameInfoStruct(b);
        }
        appendCMethodPrefix(b, "");
        b.append(" {\n");
        if(eliminated) {
            if(returnType.isVoid()) {
                b.append("    return;\n}\n\n");
            } else {
                b.append("    return 0;\n}\n\n");
            }
            return;
        }
            
        // NativeStorage handles point outside the GC heap. Optimizing C may keep
        // only that handle and drop this, so preserve the owner through the final
        // access. This also protects iterator instances that retain their owner.
        if (!staticMethod) {
            for (Instruction instruction : instructions) {
                if (instruction instanceof Invoke
                        && "java/util/NativeStorage".equals(((Invoke) instruction).getOwner().replace('_', '/'))) {
                    b.append("    CN1_KEEP_NATIVE_OWNER(__cn1StorageOwner, __cn1ThisObject);\n");
                    break;
                }
            }
        }
        b.append(declaration);
        boolean fastMethodStackCandidate = canUseFastMethodStack();

        // Frameless eligibility is decided on the RAW bytecode, before optimize()
        // fuses opcodes into opaque custom instructions. The flag is consulted by
        // optimize()'s return fast-paths (plain return, no frame release) so it must
        // be set first.
        frameless = isFramelessEligible();
        // Phase 1 shares the raw-bytecode requirement with frameless eligibility.
        collectRetireCandidates();

        boolean hasInstructions = true;
        if(optimizerOn) {
            hasInstructions = optimize();
            // AFTER optimize(), never before. optimize() is what decides scalar
            // replacement and stack allocation, and `frameless` is set just above --
            // running this earlier reads both as "no", so every StringBuilder site
            // looks like a heap object and every frameless method looks framed. That
            // is exactly why the first wiring emitted nothing: the analysis kept 14
            // sites and codegen then discarded all of them.
            analyzeRetirableLocals();
        }

        if(hasInstructions) {
            barebone = checkBarebone();
            // Frameless takes precedence: it eliminates the frame entirely (no
            // offset bookkeeping at all), strictly subsuming the barebone / fast
            // leaf-frame paths, so suppress them when frameless is in effect.
            if (frameless) {
                barebone = false;
            }
            if (barebone) {
                fixUpBarebone();
            }
            // Local autos only need to be `volatile` when a setjmp/longjmp can land
            // back in THIS frame -- i.e. the method has a try/catch (so it emits
            // setjmp) -- or when the on-device debugger must inspect them. A method
            // with no try/catch is unwound *past* when a callee throws, so its
            // primitive autos are never read after a longjmp and need not be
            // volatile. Dropping volatile there lets the C compiler register-allocate
            // and vectorize hot loops (e.g. array reductions ran ~3x faster). It is
            // GC-safe: primitive autos are never GC roots (object locals live in the
            // scanned threadObjectStack, not here). setjmp-frame methods keep
            // volatile for longjmp correctness.
            // synchronizedMethod is included conservatively: a synchronized method
            // releases its monitor during exception unwinding and carries no explicit
            // TryCatch instruction, so we cannot prove its frame is never re-entered
            // by the unwind machinery -- keep its locals volatile.
            //
            // The old heuristic ALSO forced volatile whenever the body contained any
            // call, to keep clang from juggling callee-saved registers around
            // call-heavy loops (libm kernels). That was far too blunt: it put the
            // loop counters of every method that merely CALLS something -- e.g. a
            // recursive quicksort whose partition loops are call-free -- into memory
            // on every iteration. Measured on the current codegen (frameless +
            // inlined accessors + ThinLTO), dropping the call trigger is a large win
            // for call-bearing methods with hot call-free loops and neutral for the
            // libm-style loops the heuristic originally protected.
            // -DCN1_FORCE_VOLATILE_LOCALS=true restores the always-volatile behavior.
            // Decided before the frame macro is emitted, because SP needs the
            // same answer. A method that catches anything emits setjmp, and
            // everything longjmp can leave stale has to be volatile: the
            // locals below, and SP, which every push and pop moves inside the
            // try region and which is read again in the catch. Keeping one
            // decision for both is the point -- decided apart, only the locals
            // got it, and SP stayed undefined behaviour that gcc on musl
            // eventually refused to compile at all.
            boolean volatileLocals = FORCE_VOLATILE_LOCALS || onDeviceDebug
                    || synchronizedMethod;
            framelessHasTryCatch = false;
            for (Instruction tcScan : instructions) {
                if (tcScan instanceof TryCatch) {
                    volatileLocals = true;
                    framelessHasTryCatch = true;
                    break;
                }
            }
            // Selects the _VSP variant of whichever frame macro is emitted
            // below; empty for the ordinary ones so nothing else changes.
            String spVariant = volatileLocals ? "_VSP" : "";
            Set<String> added = new HashSet<String>();
            // Sorted, not in localVariables iteration order: that is a HashSet, so the
            // order of these declarations varied between builds of the same input.
            // debugVarEntries already had to learn this for the debug side-table; the
            // C declarations had the same defect and it stayed invisible because
            // HotSpot's identity hash is stable within a run. Translating the
            // translator with itself is what surfaced it -- a different runtime, a
            // different order, and the same input produced different C.
            for (LocalVariable lv : declarationOrderedLocals()) {
                String variableName = lv.getQualifier() + "locals_"+lv.getIndex()+"_";
                if (!added.contains(variableName) && (barebone || lv.getQualifier() != 'o')) {
                    added.add(variableName);
                    b.append("    ");
                    if (volatileLocals) {
                        b.append("volatile ");
                    }
                    switch (lv.getQualifier()) {
                        case 'i' :
                            b.append("JAVA_INT"); break;
                        case 'l' :
                            b.append("JAVA_LONG"); break;
                        case 'f' :
                            b.append("JAVA_FLOAT"); break;
                        case 'd' :
                            b.append("JAVA_DOUBLE"); break;
                        case 'o' :
                            b.append("JAVA_OBJECT"); break;
                    }
                    b.append(" ").append(lv.getQualifier()).append("locals_").append(lv.getIndex()).append("_ = 0; /* ").append(lv.getOrigName()).append(" */\n");
                }
            }
            
            boolean useFastMethodStack = !barebone && !frameless && fastMethodStackCandidate;
            boolean usePrimitiveFastFrame = useFastMethodStack && isPrimitiveOnlyFastFrameCandidate();
            fastMethodStackInUse = useFastMethodStack;
            fastMethodStackPrimitiveOnly = usePrimitiveFastFrame;
            if(!barebone) {
                if(frameless) {
                    // Frameless frame: the operand stack + object locals live in a
                    // method-local C array (no slice of the global threadObjectStack,
                    // no per-call memset, no callStack/threadObjectStack offset
                    // bookkeeping). Object roots (operand array, object locals, and
                    // `this`/__cn1ThisObject for instance methods) live in native C
                    // storage found by the conservative native-stack scan. Static
                    // methods keep their class-init check; instance methods don't carry
                    // one (the receiver already forced the class to initialize). Then
                    // guard the native C stack since this frame does not bump call depth.
                    if (staticMethod && !ByteCodeClass.eagerInit(clsName.replace('/', '_').replace('$', '_'))) {
                        String framelessCls = clsName.replace('/', '_').replace('$', '_');
                        b.append("    if (!class__").append(framelessCls);
                        b.append(".initialized) __STATIC_INITIALIZER_").append(framelessCls);
                        b.append("(threadStateData);\n");
                    }
                    b.append("    DEFINE_METHOD_STACK_FRAMELESS").append(spVariant).append("(");
                    b.append(maxStack);
                    b.append(", ");
                    b.append(maxLocals);
                    b.append(", 0);\n");
                    if (framelessHasTryCatch) {
                        b.append("    CN1_FRAMELESS_TRY_FRAME();\n");
                    }
                    // A bounded instance field getter cannot recurse or grow the
                    // call chain. Let C inline it to a load without retaining a
                    // native-stack limit test at every getter use.
                    if (!isSoeGuardUnnecessary()) {
                        b.append("    CN1_FRAMELESS_SOE_GUARD(");
                        if (!returnType.isVoid()) {
                            b.append("0");
                        }
                        b.append(");\n");
                    }
                } else if(staticMethod) {
                    if(methodName.equals("__CLINIT__")) {
                        if (useFastMethodStack) {
                            if (usePrimitiveFastFrame) {
                                b.append("    DEFINE_METHOD_STACK_FAST_PRIMITIVE").append(spVariant).append("(");
                            } else {
                                b.append("    DEFINE_METHOD_STACK_FAST_REF").append(spVariant).append("(");
                            }
                        } else {
                            b.append("    DEFINE_METHOD_STACK").append(spVariant).append("(");
                        }
                    } else {
                        // Guard omitted for an eagerly initialized class (see
                        // ByteCodeClass.isEagerInitEligible).
                        if (!ByteCodeClass.eagerInit(clsName.replace('/', '_').replace('$', '_'))) {
                            b.append("    if (!class__");
                            b.append(clsName.replace('/', '_').replace('$', '_'));
                            b.append(".initialized) __STATIC_INITIALIZER_");
                            b.append(clsName.replace('/', '_').replace('$', '_'));
                            b.append("(threadStateData);\n");
                        }
                        if (useFastMethodStack) {
                            if (usePrimitiveFastFrame) {
                                b.append("    DEFINE_METHOD_STACK_FAST_PRIMITIVE").append(spVariant).append("(");
                            } else {
                                b.append("    DEFINE_METHOD_STACK_FAST_REF").append(spVariant).append("(");
                            }
                        } else {
                            b.append("    DEFINE_METHOD_STACK").append(spVariant).append("(");
                        }
                    }
                } else {
                    if (useFastMethodStack) {
                        if (usePrimitiveFastFrame) {
                            b.append("    DEFINE_INSTANCE_METHOD_STACK_FAST_PRIMITIVE").append(spVariant).append("(");
                        } else {
                            b.append("    DEFINE_INSTANCE_METHOD_STACK_FAST_REF").append(spVariant).append("(");
                        }
                    } else {
                        b.append("    DEFINE_INSTANCE_METHOD_STACK").append(spVariant).append("(");
                    }
                }
                // The frameless branch above already emitted its complete macro
                // (with args + SOE guard); only the legacy frame macros need their
                // argument list closed here.
                if(!frameless) {
                    b.append(maxStack);
                    b.append(", ");
                    b.append(maxLocals);
                    b.append(", 0");
                    if (!useFastMethodStack) {
                        b.append(", ");
                        b.append(Parser.addToConstantPool(clsName));
                        b.append(", ");
                        b.append(Parser.addToConstantPool(methodName));
                    }
                    b.append(");\n");
                }
            } else {
                b.append("    struct elementStruct*").append(volatileLocals ? " volatile" : "")
                        .append(" SP = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset];\n");
            }
            // Frame-exit retirement scope. Emitted here because `locals` exists from
            // the DEFINE_*_METHOD_STACK macro above, and because a declaration at the
            // top of the function body is what makes its cleanup attribute cover every
            // return path below it.
            b.append(frameRetireScopeDecl());
            int startOffset = 0;
            if(synchronizedMethod) {
                if(staticMethod) {
                    b.append("    monitorEnterBlock(threadStateData, (JAVA_OBJECT)&class__");
                    b.append(clsName);
                    b.append(");\n");
                } else {
                    b.append("    monitorEnterBlock(threadStateData, __cn1ThisObject);\n");
                }
            }
            if(!staticMethod) {
                if(!barebone) {
                    b.append("    locals[0].data.o = __cn1ThisObject; locals[0].type = CN1_TYPE_OBJECT; ");
                }
                startOffset++;
            }
            int localsOffset = startOffset;
            for(int iter = 0 ; iter < arguments.size() ; iter++) {
                ByteCodeMethodArg arg = arguments.get(iter);
                if (arg.getQualifier() == 'o') {
                    if(barebone) {
                        b.append("    JAVA_OBJECT olocals_");
                        b.append(localsOffset);
                        b.append("_ = __cn1Arg");
                        b.append(iter + 1);
                        b.append(";\n");
                    } else {
                        b.append("    locals[");
                        b.append(localsOffset);
                        b.append("].data.");

                        b.append(arg.getQualifier());
                        b.append(" = __cn1Arg");
                        b.append(iter + 1);
                        b.append(";\n");
                        b.append("    locals[");
                        b.append(localsOffset);
                        b.append("].type = CN1_TYPE_OBJECT;\n");
                    }
                } else {
                    b.append("    ");
                    if (!hasLocalVariableWithIndex(arg.getQualifier(), localsOffset)) {
                        switch (arg.getQualifier()) {
                            case 'i' : b.append("JAVA_INT"); break;
                            case 'f' : b.append("JAVA_FLOAT"); break;
                            case 'd' : b.append("JAVA_DOUBLE"); break;
                            case 'l' : b.append("JAVA_LONG"); break;
                            default: b.append("JAVA_INT"); break;
                        }
                        b.append(" ");
                        
                    }
                    b.append(arg.getQualifier());
                    b.append("locals_");
                    b.append(localsOffset);
                    b.append("_");
                    b.append(" = __cn1Arg");
                    b.append(iter + 1);
                    b.append(";\n");
                }
                // For now we'll still allocate space for locals that we're not using
                // so we keep the indexes the same for objects.
                localsOffset++;
                if(arg.isDoubleOrLong()) {
                    localsOffset++;
                }
            }
            if (onDeviceDebug && !barebone) {
                appendLocalsAddressTable(b);
            }
        } else {
            if(synchronizedMethod) {
                if(staticMethod) {
                    b.append("    monitorEnterBlock(threadStateData, (JAVA_OBJECT)&class__");
                    b.append(clsName);
                    b.append(");\n");
                } else {
                    b.append("    monitorEnterBlock(threadStateData, __cn1ThisObject);\n");
                }
            }
        }
        
        BasicInstruction.setSynchronizedMethod(synchronizedMethod, staticMethod, clsName);
        TryCatch.reset();
        BasicInstruction.setHasInstructions(hasInstructions);
        // Annotation-driven stack allocation: every NEW of a @StackAllocate class
        // gets one method-scoped struct, reused across loop iterations (only one
        // instance per site is live at a time -- the annotation's contract). The
        // struct lives for the whole frame so the object's references stay valid
        // until it dies with the frame; the matching TypeInstruction NEW codegen
        // initializes the header and pushes its address instead of heap-allocating.
        for(int saIter = 0 ; saIter < instructions.size() ; saIter++) {
            Instruction saInst = instructions.get(saIter);
            if(saInst instanceof TypeInstruction) {
                TypeInstruction saTi = (TypeInstruction)saInst;
                if(saTi.isScalarReplaced()) {
                    // Scalar-replaced @StackAllocate site: declare the struct as a
                    // pure C local whose address is never taken so clang SROA can
                    // promote its fields to registers. No header is needed -- the
                    // object is primitive-only and never visible to the GC. The id
                    // is stable (assigned during optimize), independent of list pos.
                    // Use the actual NEW'd type (scalar replacement is now per-usage, not gated on
                    // the @StackAllocate annotation, so getStackAllocType() may be null here).
                    b.append("    struct obj__").append(srMangle(saTi.getTypeName()))
                            .append(" __cn1sr_").append(saTi.getScalarStructId()).append(";\n");
                    continue;
                }
                String saType = saTi.getStackAllocType();
                if(saType != null) {
                    b.append("    struct obj__").append(saType).append(" __cn1stk_").append(saIter).append(";\n");
                    if(saTi.getStackBuilderBytes() > 0) {
                        b.append("    unsigned char __cn1sbdata_").append(saIter).append("[")
                                .append(saTi.getStackBuilderBytes()).append("] __attribute__((aligned(16)));\n");
                        b.append("    struct CN1StackBuffer __cn1sbscope_").append(saIter)
                                .append(" __attribute__((cleanup(cn1StackBufferLeave))) = {threadStateData->nativeBuffers, threadStateData, (JAVA_OBJECT)&__cn1stk_")
                                .append(saIter).append(", __cn1sbdata_").append(saIter).append(", 0, ")
                                .append(saTi.getStackBuilderBytes()).append("};\n");
                        b.append("    threadStateData->nativeBuffers = &__cn1sbscope_").append(saIter).append(";\n");
                    }
                    if(saTi.getStackFusedLen() >= 0) {
                        // constant-capacity fused child buffer lives on the stack
                        // too (8-aligned via the long long element type); the NEW
                        // codegen installs an ordinary array header into it and
                        // points the owner's field at it (keep-if-null ctor keeps it)
                        b.append("    long long __cn1stkbuf_").append(saIter)
                                .append("[(CN1_FUSED_ARR_BYTES(").append(saTi.getStackFusedLen())
                                .append(", sizeof(").append(saTi.getStackFusedElemCType())
                                .append(")) + 7) / 8];\n");
                    }
                    saTi.setStackAllocId(saIter);
                }
            }
        }
        // SAME RULE AS THE ORDINARY LOCALS, and for the same reason. ParparVM implements
        // exceptions with setjmp/longjmp, and a non-volatile C local modified between the
        // setjmp and the longjmp is INDETERMINATE after the jump. The cursor, the mode
        // flag and the modCount snapshot are all modified inside the loop, so in a method
        // that catches anything they have to be volatile.
        //
        // This is not theoretical: it is what the first version got wrong. A for-each with
        // a try/catch in its body ran correctly at -O0, where the variables happen to live
        // in memory, and segfaulted at -O1, where they live in registers that the longjmp
        // leaves stale -- the loop resumed with a garbage cursor and a garbage mode flag.
        boolean feVolatile = FORCE_VOLATILE_LOCALS || onDeviceDebug || synchronizedMethod;
        if (!feVolatile) {
            for (Instruction tcScan : instructions) {
                if (tcScan instanceof TryCatch) {
                    feVolatile = true;
                    break;
                }
            }
        }
        String feQual = feVolatile ? "volatile JAVA_INT " : "JAVA_INT ";
        for(int feIter = 0 ; feIter < forEachIntrinsicCount ; feIter++) {
            if (frameless) {
                // Native collection buffers do not keep their Java owners alive.
                // One lifetime fence per traversal lets the cursor stay in C
                // registers while retaining the owner through every normal return.
                b.append("    CN1_KEEP_NATIVE_OWNER(__feOwner_").append(feIter).append(", JAVA_NULL);\n");
            }
            // The collection owner stays in a rooted Java local. The cached null
            // sentinel is also held by IdentityHashMap.NULL_OBJECT, a static root.
            // All remaining cursor state is primitive.
            b.append("    ").append(feQual).append("__feIdx_").append(feIter).append(" = 0;\n");
            if (!directForEachSites.contains(feIter)) b.append("    ").append(feQual).append("__feFast_").append(feIter).append(" = 0;\n");
            b.append("    ").append(feQual).append("__feMod_").append(feIter).append(" = 0;\n");
            b.append(feVolatile ? "    JAVA_OBJECT volatile " : "    JAVA_OBJECT ").append("__feNull_").append(feIter).append(" = JAVA_NULL;\n");
            b.append("    ").append(feQual).append("__feLast_").append(feIter).append(" = -1;\n");
            // The SLOW path still needs an iterator, and it may as well be a stack one:
            // the loop has already been proved not to let it escape, and offering the
            // buffer here is what keeps a mixed program from paying heap allocation on
            // the receivers that are not ArrayLists. Measured on ForEachT: without it the
            // intrinsic left 111 heap iterators where the buffer mechanism alone left 93.
            if (!directForEachSites.contains(feIter)) b.append("    long long __feBuf_").append(feIter)
                    .append("[(CN1_ITER_BUF_BYTES + 7) / 8];\n");
        }
        for(int sbIter = 0 ; sbIter < stackIterCount ; sbIter++) {
            // long long for 8-alignment, which every object header needs. Declared for
            // the whole frame rather than per loop: the iterator must stay valid until
            // the loop ends, and a nested for-each needs its own buffer anyway.
            b.append("    long long __cn1iterbuf_").append(sbIter)
                    .append("[(CN1_ITER_BUF_BYTES + 7) / 8];\n");
        }
        for(Instruction i : instructions) {
            i.setMethod(this);
            i.setMaxes(maxStack, maxLocals);
            i.appendInstruction(b, instructions);
        }
        if(instructions.size() == 0) {
            if(returnType.isVoid()) {
                b.append("    return;\n}\n\n");
            } else {
                b.append("    return 0;\n}\n\n");
            }
            return;
        }
        Instruction inst = instructions.get(instructions.size() - 1);
        int lastInstruction = inst.getOpcode();
        if(lastInstruction == -1 || inst instanceof LabelInstruction) {
            if(instructions.size() > 2) {
                inst = instructions.get(instructions.size() - 2);
                lastInstruction = inst.getOpcode();
            }
        }
        if(lastInstruction == Opcodes.RETURN || lastInstruction == Opcodes.ARETURN || lastInstruction == Opcodes.IRETURN || lastInstruction == Opcodes.LRETURN ||
                lastInstruction == Opcodes.FRETURN || lastInstruction == Opcodes.DRETURN || lastInstruction == -1) {
            b.append("}\n\n");
        } else {
            if(returnType.isVoid()) {
                b.append("    return;\n}\n\n");
            } else {
                b.append("    return 0;\n}\n\n");
            }
        }
    }
    
    public void appendInterfaceMethodC(StringBuilder b) {
        appendInterfaceMethodC(b, clsName);
    }

    public void appendInterfaceMethodC(StringBuilder b, String clsName) {
        appendCMethodPrefix(b, "", clsName);
        b.append(" {\n");
        if(!returnType.isVoid()) {
            b.append("return virtual_");
        } else {
            b.append("virtual_");            
        }
        b.append(clsName);
        b.append("_");
        b.append(getCMethodName());
        b.append("__");
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(b);
        }
        if(!returnType.isVoid()) {
            b.append("_R");
            returnType.appendCMethodExt(b);
        }
        b.append("(threadStateData");
        
        int arg = 1;
        b.append(", __cn1ThisObject");
        for(int iter = 0 ; iter < arguments.size() ; iter++) {
            b.append(", ");
            b.append("__cn1Arg");
            b.append(arg);
            arg++;
        }        
        b.append(");\n}\n\n");
    }

    public void appendSuperCall(StringBuilder b, String cls) {
        if(nativeMethod) {
            return;
        }
        appendCMethodPrefix(b, "", cls);
        b.append(" {\n");
        if(!returnType.isVoid()) {
            b.append("    return ");
        } 
        b.append(clsName);
        b.append("_");
        b.append(getCMethodName());
        b.append("__");
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(b);
        }
        if(!returnType.isVoid()) {
            b.append("_R");
            returnType.appendCMethodExt(b);
        }
        b.append("(threadStateData");
        int arg = 1;
        if(!staticMethod) {
            b.append(", __cn1ThisObject");
        }
        for(int iter = 0 ; iter < arguments.size() ; iter++) {
            b.append(", ");
            b.append("__cn1Arg");
            b.append(arg);
            arg++;
        }        
        
        b.append(");\n}\n\n");
    }

    public void appendMethodHeader(StringBuilder b) {
        appendMethodHeader(b, clsName);
    }
    
    public void appendMethodHeader(StringBuilder b, String clsName) {
        appendCMethodPrefix(b, "", clsName);
        b.append(";\n");
    }
    
    public void appendVirtualMethodC(String cls, StringBuilder b, int offset) {
        appendVirtualMethodC(cls, b, Integer.toString(offset));
    }
    
    public void appendVirtualMethodC(String cls, StringBuilder b, String offset) {
        appendVirtualMethodC(cls, b, offset, false);
    }
    
    public static void addVirtualMethodsInvoked(String m) {
        if(!virtualMethodsInvoked.contains(m)) {
            virtualMethodsInvoked.add(m);
        }
    }
    
    public void setForceVirtual(boolean forceVirtual) {
        this.forceVirtual = forceVirtual;
    }
    
    public boolean isForceVirtual() {
        return forceVirtual;
    }
    
    public String getFullCName() {
        return this.clsName + "_"+this.getCMethodName();
    }
    
    /* Cases for the class-id switch in an interface thunk: {classIdToken, function}.
     * Set by the interface's emitter right before the thunk is written, and cleared
     * after, because one BytecodeMethod is shared across the classes that inherit it. */
    private List<String[]> thunkCases;

    void setThunkCases(List<String[]> cases) {
        this.thunkCases = cases;
    }

    public void appendVirtualMethodC(String cls, StringBuilder b, String offset, boolean includeStaticInitializer) {
        if(virtualOverriden) {
            return;
        }
        StringBuilder bld = new StringBuilder();
        bld.append(cls);
        bld.append("_");
        bld.append(getCMethodName());
        bld.append("__");
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(bld);
        }
        if(!returnType.isVoid()) {
            bld.append("_R");
            returnType.appendCMethodExt(bld);
        }
        
        // generate the function pointer declaration
        appendCMethodPrefix("\ntypedef ", ")", b, "(*functionPtr_", cls);
        b.append(";\n");
        
        appendCMethodPrefix(b, "virtual_", cls);
        b.append(" {\n    ");

        // Devirtualize the tagged-Integer fast path for the hottest collection methods:
        // an Integer's hashCode IS its value, so HashMap's per-lookup key.hashCode() becomes
        // a bare inline untag with no indirect dispatch and no call, and equals between two
        // tagged Integers is a pointer compare (the encoding is canonical).
        //
        // These test the tag CODE, not merely "is tagged". Every other boxed type is tagged
        // too and has a different hashCode contract -- Long folds its halves, Float and
        // Double go through *ToIntBits -- so widening this to any tagged receiver returns a
        // wrong hash with no crash, which is the worst failure this scheme can produce.
        // The other types reach the right implementation through the vtable, which cn1ClassOf
        // already resolves correctly; that path is slower than this one and always correct.
        // equals stays Integer-only for a second reason: pointer equality implies value
        // equality for every tag, but the converse fails for Float and Double, where two
        // distinct NaN encodings must compare equal.
        String cn1mn = getCMethodName();
        if(cn1mn.equals("hashCode") && arguments.isEmpty() && !returnType.isVoid()) {
            b.append("\n#if CN1_TAGGED_ACTIVE\n    if(CN1_TAG_CODE(__cn1ThisObject) == CN1_TAG_INTEGER) { return CN1_UNTAG_INT(__cn1ThisObject); }\n#endif\n    ");
        } else if(cn1mn.equals("equals") && arguments.size() == 1 && !returnType.isVoid()) {
            b.append("\n#if CN1_TAGGED_ACTIVE\n    if(CN1_TAG_CODE(__cn1ThisObject) == CN1_TAG_INTEGER && CN1_TAG_CODE(__cn1Arg1) == CN1_TAG_INTEGER) { return (__cn1ThisObject == __cn1Arg1) ? JAVA_TRUE : JAVA_FALSE; }\n#endif\n    ");
        }

        // An eagerly initialized interface (no <clinit>) had its classToInterfaceMap
        // rows written in initConstantPool before any Java thread existed, so there is
        // nothing left for this guard to publish -- see
        // ByteCodeClass.isEagerInitEligible.
        if(includeStaticInitializer && !ByteCodeClass.eagerInit(cls)) {
            // GUARD IT. This is the INTERFACE thunk, and it used to call the class
            // initializer UNCONDITIONALLY -- every other class-init site in the
            // translator tests the flag first (see the frameless prologue above,
            // TypeInstruction and FusedConstructor). The initializer returns
            // immediately once loaded, but it is a huge function that clang will never
            // inline, so the call itself was the cost: an unconditional, non-inlinable
            // call on the hottest dispatch path this VM has.
            //
            // Iterator.hasNext() and Iterator.next() are interface thunks, so a
            // for-each paid it TWICE PER ELEMENT. Profiled on the self-hosting corpus,
            // __STATIC_INITIALIZER_java_util_Iterator alone was 4.4% of main-thread
            // self time doing nothing but returning.
            //
            // The flag is the right thing to test and the ordering is not incidental:
            // class__X.initialized is RELEASE-stored after the vtable and the
            // classToInterfaceMap rows are written, and this thunk is about to index
            // exactly those rows -- so the ACQUIRE load here is what makes them visible.
            // Testing __X_LOADED__ instead would be a weaker gate that publishes
            // nothing; that distinction is why the inline guards were moved onto this
            // flag in the first place.
            b.append("if(__builtin_expect(!__atomic_load_n(&class__");
            b.append(cls);
            b.append(".initialized, __ATOMIC_ACQUIRE), 0)) __STATIC_INITIALIZER_");
            b.append(cls);
            b.append("(threadStateData);\n    ");
        }
        if (Util.getProperty("INCLUDE_NPE_CHECKS", "false").equals("true")) {
            b.append("\n    if(__cn1ThisObject == JAVA_NULL) THROW_NULL_POINTER_EXCEPTION();\n    ");
        } 
        // CN1_CLASS_OF is not a field load when tagged values are compiled in: it
        // masks the pointer, tests the tag and selects between a proxy and the object
        // before it loads anything. The interface form of this thunk used it twice --
        // once for the vtable and once for the classId that indexes
        // classToInterfaceMap -- so resolving it once here removes that work from
        // every interface dispatch, which is the hottest indirect call the VM makes.
        // Skip the tagged resolve where a tagged value cannot arrive: this thunk only
        // ever sees objects whose class is its owner or below, and a boxed immediate
        // is assignable to very few of those. See CN1_CLASS_OF_UNTAGGED.
        b.append("struct clazz* cn1__cls = ")
         // `cls`, not clsName: this thunk is virtual_<cls>_<method>, so its receiver is
         // a `cls` or a subclass. clsName is where the METHOD was declared, which for
         // an Object method is java.lang.Object and would keep the tagged resolve on
         // every equals/hashCode/toString thunk in the program.
         .append(Parser.canReceiveTagged(cls) ? "CN1_CLASS_OF" : "CN1_CLASS_OF_UNTAGGED")
         .append("(__cn1ThisObject);\n    ");

        /* A SWITCH ON THE CLASS ID, ahead of the indirect dispatch below.
         *
         * The indirect form is six dependent loads -- class pointer, class id, the
         * classToInterfaceMap row, the offset within it, the vtable, the slot -- and
         * then a branch the C compiler cannot see through, so nothing on either side
         * of the call is optimised across it. The switch is ONE load and a jump table,
         * and every arm is a named callee that ThinLTO can inline.
         *
         * This is the dispatch lambdas need. A lambda becomes its own synthetic class
         * implementing the functional interface, so a listener invoked out of a
         * collection -- addActionListener(e -> ...) and then fireActionEvent -- has no
         * provable receiver at the CALL SITE and no per-site analysis can help it. The
         * thunk is the one place that sees every implementation, and every lambda is
         * simply another case here.
         *
         * Nothing is kept alive by this: the thunk already reached all of these
         * through the vtable, and the indirect form remains as the default arm, so a
         * receiver from a path the analysis did not see still dispatches correctly.
         * Their headers come in through the include-only channel, never the dependency
         * list -- see the note in ByteCodeClass.generateCCode. */
        if (thunkCases != null && !thunkCases.isEmpty()) {
            b.append("switch(cn1__cls->classId) {\n");
            for (int ci = 0 ; ci < thunkCases.size() ; ci++) {
                String[] c = thunkCases.get(ci);
                // The cases arrive grouped by target, so a run of classes that share one
                // implementation shares one arm. 116 classes implementing Iterator do not
                // mean 116 bodies -- they inherit from a handful, and stacking the labels
                // is what keeps a hot interface under the distinct-target cap instead of
                // being refused for having a wide cone.
                b.append("        case ").append(c[0]).append(":");
                if (ci + 1 < thunkCases.size() && thunkCases.get(ci + 1)[1].equals(c[1])) {
                    b.append("\n");
                    continue;
                }
                b.append(" ");
                if (!returnType.isVoid()) {
                    b.append("return ");
                }
                b.append(c[1]).append("(threadStateData, __cn1ThisObject");
                for (int iter = 0 ; iter < arguments.size() ; iter++) {
                    b.append(", __cn1Arg").append(iter + 1);
                }
                b.append(");");
                if (returnType.isVoid()) {
                    // RETURN, not break. A break leaves the switch and falls straight
                    // into the indirect dispatch below, which would call the method a
                    // SECOND time -- invisible for a query, state corruption for a
                    // mutator. MapTorture, SetTorture and IdmTorture all caught it.
                    b.append(" return;");
                }
                b.append("\n");
            }
            b.append("    }\n    ");
        }
        if(!returnType.isVoid()) {
            b.append("return (*(functionPtr_");
        } else {
            b.append("(*(functionPtr_");            
        }
        b.append(bld);
        b.append(")cn1__cls->vtable[");
        b.append(offset);
        b.append("])(threadStateData, ");
        
        int arg = 1;
        b.append("__cn1ThisObject");
        for(int iter = 0 ; iter < arguments.size() ; iter++) {
            b.append(", ");
            b.append("__cn1Arg");
            b.append(arg);
            arg++;
        }        
        b.append(");\n}\n\n");
    }

    public void appendVirtualMethodHeader(StringBuilder b, String cls) {
        StringBuilder bld = new StringBuilder();
        bld.append(cls);
        bld.append("_");
        bld.append(getCMethodName());
        bld.append("__");
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(bld);
        }
        if(!returnType.isVoid()) {
            bld.append("_R");
            returnType.appendCMethodExt(bld);
        }
        appendCMethodPrefix(b, "virtual_", cls);
        b.append(";\n");
    }

    public void appendFunctionPointer(StringBuilder b) {
        appendFunctionPointer(b, clsName);
    }

    public void appendFunctionPointer(StringBuilder b, String className) {
        b.append(className);
        b.append("_");
        b.append(getCMethodName());
        b.append("__");
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(b);
        }        
        if(!returnType.isVoid()) {
            b.append("_R");
            returnType.appendCMethodExt(b);
        }
    }

    /**
     * @return the methodName
     */
    public String getMethodName() {
        return methodName;
    }
    
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BytecodeMethod)) {
            return false;
        }

        BytecodeMethod bm = (BytecodeMethod)o;

        if (!methodName.equals(bm.methodName)) {
            return false;
        }
        if (acceptStaticOnEquals) {
            if (bm.arguments.size() != arguments.size()) {
                return false;
            }
        } else {
            if (staticMethod || bm.staticMethod || bm.arguments.size() != arguments.size()) {
                return false;
            }
        }

        for (int iter = 0; iter < arguments.size(); iter++) {
            ByteCodeMethodArg arg1 = arguments.get(iter);
            ByteCodeMethodArg arg2 = bm.arguments.get(iter);
            if (!arg1.equals(arg2)) {
                return false;
            }
        }

        if (returnType == null) {
            return bm.returnType == null;
        }
        return returnType.equals(bm.returnType);
    }

    public int hashCode() {
        int result = methodName == null ? 0 : methodName.hashCode();
        result = 31 * result + arguments.size();
        result = 31 * result + (acceptStaticOnEquals || !staticMethod ? 0 : 1);
        result = 31 * result + (returnType == null ? 0 : returnType.hashCode());
        return result;
    }
    
    /**
     * JS-target-only flag. When true the method body may transitively
     * block the cooperative scheduler (sleep / wait / monitor entry /
     * native host bridge) and must be emitted as ``function*`` with
     * ``yield*`` at every call site. When false the method can run
     * straight through and is emitted as a regular ``function`` —
     * callers invoke it directly, with no generator allocation per
     * call. Computed by {@link JavascriptSuspensionAnalysis}.
     */
    private boolean javascriptSuspending = true;

    public boolean isJavascriptSuspending() {
        return javascriptSuspending;
    }

    public void setJavascriptSuspending(boolean value) {
        this.javascriptSuspending = value;
    }

    public boolean isStatic() {
        return staticMethod;
    }

    public boolean isPrivate() {
        return privateMethod;
    }
    
    /*public boolean isVirtualBlockedDueToFinal() {
        return (!privateMethod && !staticMethod && !constructor) && finalMethod;
    }*/
    
    public boolean canBeVirtual() {
        return !privateMethod && !staticMethod && !constructor;
    }
    
    public boolean isNative() {
        return nativeMethod;
    }

    /**
     * The C prototype a native implementation of this method must have, in the
     * exact form {@link #appendCMethodPrefix} emits at the call site.
     *
     * <p>Built from that same method rather than from a second mangler, because the
     * whole point of {@link NativeSignatureVerifier} is that the two agree: a
     * verifier that mangles names its own way would bless exactly the spellings the
     * translator will not call.</p>
     */
    public NativeSignatureVerifier.Signature getNativeSignature() {
        StringBuilder prototype = new StringBuilder();
        appendCMethodPrefix("", "", prototype, "", clsName);

        StringBuilder symbol = new StringBuilder();
        symbol.append(clsName).append('_').append(getCMethodName()).append("__");
        String overloadPrefix = symbol.toString();
        appendArgumentTypes(symbol);

        StringBuilder cReturnType = new StringBuilder();
        returnType.appendCSig(cReturnType);

        List<String> params = new ArrayList<String>();
        params.add("CODENAME_ONE_THREAD_STATE");
        if (!staticMethod) {
            StringBuilder self = new StringBuilder();
            new ByteCodeMethodArg(clsName, 0).appendCSig(self);
            params.add(self.toString().trim());
        }
        for (ByteCodeMethodArg arg : arguments) {
            StringBuilder type = new StringBuilder();
            arg.appendCSig(type);
            params.add(type.toString().trim());
        }
        return new NativeSignatureVerifier.Signature(symbol.toString(), clsName,
                methodName, overloadPrefix, cReturnType.toString().trim(), params,
                Util.collapseWhitespace(prototype.toString().trim()));
    }

    public boolean isAbstract() {
        return abstractMethod;
    }
    
    public String getVariableNameForTypeIndex(int index, char type) {
        for(Instruction i : instructions) {
            if(i instanceof LocalVariable) {
                if(((LocalVariable)i).isRightVariable(index, type)) {
                    return ((LocalVariable)i).getVarName();
                }
            } else {
                return null;
            }
            
        }
        return null;
    }
    
    public void addMultiArray(String desc, int dims) {
        addInstruction(new MultiArray(desc, dims));
    }
    
    public void addTryCatchBlock(Label start, Label end, Label handler, String type) {
        addInstruction(new TryCatch(start, end, handler, type));
    }
    
    public void addLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        if (disableDebugInfo) {
            return;
        }
        //addInstruction(0, new LocalVariable(name, desc, signature, start, end, index));
        LocalVariable lv = new LocalVariable(name, desc, signature, start, end, index);
        // A store opcode visited earlier already synthesised a placeholder for
        // this (slot, qualifier) — named "vN", with no scope. localVariables is
        // a Set keyed on exactly that pair, so the placeholder would otherwise
        // keep the class file's own entry out and the debugger would show "v2"
        // for a local named "count" and treat it as live for the whole method.
        if (lv.getScopeStart() != null) {
            replaceSyntheticLocalVariable(lv);
        }
        localVariables.add(lv);
    }

    /**
     * Drops the store-synthesised placeholder a real declaration supersedes.
     *
     * Matched on storage rather than with equals, which now also compares
     * scope — a placeholder has none, so it would never equal the declaration
     * that replaces it.
     */
    private void replaceSyntheticLocalVariable(LocalVariable replacement) {
        for (Iterator<LocalVariable> it = localVariables.iterator(); it.hasNext();) {
            LocalVariable existing = it.next();
            if (existing.getScopeStart() == null
                    && existing.getIndex() == replacement.getIndex()
                    && existing.getQualifier() == replacement.getQualifier()) {
                it.remove();
                return;
            }
        }
    }
    
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * The type this method allocates and hands straight back -- NEW T, DUP, the
     * constructor arguments, T.&lt;init&gt;, ARETURN -- or null for any other shape.
     * The point of being this strict is that the caller uses the answer as a
     * certainty about the returned object's concrete class, so a body that could
     * return something it did not just allocate has to be rejected rather than
     * guessed at.
     */
    // Exact NEW provenance on every object return, captured before IR rewrites.
    String freshReturnType;

    public String allocatedReturnType() {
        List<Instruction> real = new ArrayList<Instruction>();
        for (Instruction i : instructions) {
            if (i instanceof LabelInstruction || i instanceof LineNumber || i instanceof TryCatch) {
                continue;
            }
            real.add(i);
        }
        if (real.size() < 4) {
            return null;
        }
        Instruction first = real.get(0);
        if (!(first instanceof TypeInstruction) || first.getOpcode() != Opcodes.NEW) {
            return null;
        }
        String type = ((TypeInstruction) first).getTypeName();
        if (type == null || real.get(1).getOpcode() != Opcodes.DUP) {
            return null;
        }
        if (real.get(real.size() - 1).getOpcode() != Opcodes.ARETURN) {
            return null;
        }
        Instruction ctor = real.get(real.size() - 2);
        if (!(ctor instanceof Invoke) || ctor.getOpcode() != Opcodes.INVOKESPECIAL) {
            return null;
        }
        Invoke ci = (Invoke) ctor;
        if (!"<init>".equals(ci.getName()) || !type.equals(ci.getOwner())) {
            return null;
        }
        // Everything between the DUP and the constructor has to be a plain local
        // read. Anything with a side effect could leave a different object under
        // the ARETURN, and then the type above would be a lie.
        for (int i = 2; i < real.size() - 2; i++) {
            Instruction a = real.get(i);
            if (!(a instanceof VarOp) || !isLoadOpcode(a.getOpcode())) {
                return null;
            }
        }
        return type;
    }

    private static boolean isLoadOpcode(int op) {
        return op == Opcodes.ALOAD || op == Opcodes.ILOAD || op == Opcodes.LLOAD
                || op == Opcodes.FLOAD || op == Opcodes.DLOAD;
    }

    private int nextExecutable(int from) {
        for (int i = from; i < instructions.size(); i++) {
            Instruction ins = instructions.get(i);
            if (ins instanceof LabelInstruction || ins instanceof LineNumber || ins instanceof TryCatch) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private int prevExecutable(int from) {
        for (int i = from; i >= 0; i--) {
            Instruction ins = instructions.get(i);
            if (ins instanceof LabelInstruction || ins instanceof LineNumber || ins instanceof TryCatch) {
                continue;
            }
            return i;
        }
        return -1;
    }

    /// The first local slot that cannot hold an incoming argument.
    ///
    /// Parameters occupy locals WITHOUT an ASTORE, so a slot-write count of one
    /// does not mean the slot holds one value over the method's lifetime -- an
    /// Iterator parameter in that slot is a second, earlier value. Long and double
    /// take two slots each, per the JVM numbering the instruction stream uses.
    ///
    /// @return the lowest slot index that is definitely not a parameter
    private int firstNonParameterSlot() {
        int slots = isStatic() ? 0 : 1;
        for (ByteCodeMethodArg arg : arguments) {
            char q = arg.getQualifier();
            slots += (q == 'l' || q == 'd') ? 2 : 1;
        }
        return slots;
    }

    private int countStoresTo(int slot) {
        int n = 0;
        for (Instruction ins : instructions) {
            if (ins instanceof VarOp && ins.getOpcode() == Opcodes.ASTORE
                    && ((VarOp) ins).getIndex() == slot) {
                n++;
            }
        }
        return n;
    }

    /// True if this instruction reads or writes the given local slot.
    private static boolean touchesSlot(Instruction ins, int slot) {
        if (ins instanceof VarOp) {
            return ((VarOp) ins).getIndex() == slot;
        }
        if (ins instanceof IInc) {
            return ((IInc) ins).getVar() == slot;
        }
        return false;
    }

    /// Index of the LabelInstruction carrying this label, or -1. Compared by identity:
    /// ASM Labels are unique objects, so this is exact rather than name-based.
    private int indexOfLabel(Label l) {
        for (int i = 0; i < instructions.size(); i++) {
            Instruction ins = instructions.get(i);
            if (ins instanceof LabelInstruction && ((LabelInstruction) ins).getLabel() == l) {
                return i;
            }
        }
        return -1;
    }

    /// Number of stack buffers required by escaping fallback iterators.
    private int stackIterCount;

    /// Whole-program tallies for the census: for-each sites scoped, and sites refused
    /// because the loop is not the canonical shape or the slot outlives it.
    static int stackIterScoped;
    static int stackIterRefused;

    /// Per-site state for an intrinsified for-each; one set of C locals per loop.
    private int forEachIntrinsicCount;
    private final Set<Integer> directForEachSites = new HashSet<Integer>();

    /// Whole-program tally for the census.
    static int forEachIntrinsified;

    /** Lower immediate stream pipelines before reachability culling. */
    public void fuseStreams() {
        List<Invoke> calls = StreamFusion.lower(this);
        for (Invoke call : calls) {
            addInstruction(new StreamFusion.Dependency(call));
        }
        usedSigs = null; usedMethods = null;
    }

    /**
     * Lower validated, nonescaping foreach protocols to rooted native cursors.
     * Closed-world exact receivers omit dispatch and guards. Unknown receivers
     * retain an exact-class guard and the ordinary iterator fallback. Mutation
     * checks remain at next/remove, including when the loop body invokes user code.
     */
    public void intrinsifyForEach() {
        if (!STACK_ITERATORS) {
            return;
        }
        for (int i = 0; i < instructions.size(); i++) {
            Instruction ins = instructions.get(i);
            if (!(ins instanceof Invoke)) {
                continue;
            }
            Invoke inv = (Invoke) ins;
            int op = inv.getOpcode();
            if (op != Opcodes.INVOKEINTERFACE && op != Opcodes.INVOKEVIRTUAL) {
                continue;
            }
            if (!"iterator".equals(inv.getName())
                    || !"()Ljava/util/Iterator;".equals(inv.getDesc())) {
                continue;
            }
            int recvIdx = prevExecutable(i - 1);
            if (recvIdx < 0) {
                continue;
            }
            // Fold an expression when it is already available. Raw stack
            // producers remain in the IR and use a non-assignable consumer,
            // so later expression folding cannot discard their evaluation.
            StringBuilder recvCode = new StringBuilder();
            Instruction recv = instructions.get(recvIdx);
            Invoke foldedMapView = null;
            int viewIndex = -1;
            if (recv instanceof Invoke) {
                Invoke view = (Invoke) recv;
                boolean knownMap = view.hasExactReceiver("java_util_HashMap") || view.hasExactReceiver("java_util_LinkedHashMap") || view.hasExactReceiver("java_util_IdentityHashMap");
                boolean viewCall = "keySet".equals(view.getName()) && "()Ljava/util/Set;".equals(view.getDesc())
                        || "values".equals(view.getName()) && "()Ljava/util/Collection;".equals(view.getDesc());
                int mapIndex = prevExecutable(recvIdx - 1);
                if (knownMap && viewCall && mapIndex >= 0 && instructions.get(mapIndex) instanceof AssignableExpression) {
                    StringBuilder mapCode = new StringBuilder();
                    if (((AssignableExpression) instructions.get(mapIndex)).assignTo("__c", mapCode)) {
                        foldedMapView = view; viewIndex = recvIdx; recvIdx = mapIndex;
                        recv = instructions.get(recvIdx);
                    }
                }
            }
            boolean foldedReceiver = recv instanceof AssignableExpression
                    && ((AssignableExpression) recv).assignTo("__c", recvCode);
            if (!foldedReceiver) {
                // Raw fields and call results are still operand-stack producers at
                // this stage. Keep them in the IR so later expression folding can
                // optimize them normally, and consume their one result explicitly.
                recvCode.setLength(0);
                recvCode.append("__c = POP_OBJ();\n");
            }
            int[] v = validateForEach(i);
            if (v == null) {
                if (INTRINSIC_TRACE) {
                    System.out.println("[FEI-NO] " + clsName + "." + methodName
                            + " recv=" + instructions.get(recvIdx).getClass().getSimpleName()
                            + " owner=" + inv.getOwner());
                }
                continue;
            }
            int storeIdx = v[0], itSlot = v[1], ld1 = v[2], hn = v[3];
            int ld2 = v[5], nx = v[6], endIdx = v[7];
            // -Dcn1.iterIntrinsicLimit=N intrinsifies only the first N sites in the
            // whole program. Purely a bisection aid: a crash that appears once 21 sites
            // are rewritten says nothing about WHICH one, and the alternative is guessing
            // at shapes one driver at a time.
            if (INTRINSIC_LIMIT >= 0 && forEachIntrinsified >= INTRINSIC_LIMIT) {
                continue;
            }
            int id = forEachIntrinsicCount++;
            NativeTraversal traversal = new NativeTraversal(inv, foldedMapView, id, itSlot, rawFramelessEligibility == 1);
            if (traversal.isDirect()) directForEachSites.add(id);
            forEachIntrinsified++;
            List<String> deps = traversal.dependencies();
            for (String dependency : deps) {
                if (!dependentClasses.contains(dependency)) dependentClasses.add(dependency);
            }
            String iterCall = "virtual_" + mangle(inv.getOwner()) + "_iterator___R_java_util_Iterator";

            List<Integer> removes = new ArrayList<Integer>();
            for (int k = storeIdx + 1; k < endIdx; k++) {
                if (k == ld1 || k == ld2) {
                    continue;
                }
                Instruction in = instructions.get(k);
                if (in instanceof Invoke && "remove".equals(((Invoke) in).getName())
                        && "()V".equals(((Invoke) in).getDesc())) {
                    int r = prevExecutable(k - 1);
                    if (r >= 0 && isALoadOf(r, itSlot)) {
                        removes.add(k);
                    }
                }
            }

            // Splice from the BACK so earlier indices stay valid.
            for (int ri = removes.size() - 1; ri >= 0; ri--) {
                int k = removes.get(ri);
                int r = prevExecutable(k - 1);
                String code = traversal.remove();
                instructions.set(k, new CustomIntruction(code, code, deps));
                instructions.remove(r);
            }

            if (!removes.isEmpty() && Parser.getClassObject("java_util_IdentityHashMap") != null) {
                addInstruction(new StreamFusion.Dependency(new Invoke(Opcodes.INVOKEVIRTUAL,
                        "java/util/IdentityHashMap", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", false)));
            }
            String nextCode = traversal.next();
            instructions.set(nx, new CustomIntruction(nextCode, nextCode, deps));
            instructions.remove(ld2);

            String hasCode = traversal.hasNext();
            instructions.set(hn, new CustomIntruction(hasCode, hasCode, deps));
            instructions.remove(ld1);

            String beginCode = traversal.setup(recvCode.toString(), iterCall, itSlot);
            // The slot is written HERE rather than left to the ASTORE, and the ASTORE goes
            // with it. Pushing the value and letting the existing store consume it looks
            // tidier and does not work: CustomIntruction implements AssignableExpression,
            // so the ASTORE folds the push into itself, finds no assignable expression
            // attached, and emits NOTHING -- the entire setup block disappeared from the
            // generated C while the loop that depended on it stayed.
            instructions.set(i, foldedReceiver ? new CustomIntruction(beginCode, beginCode, deps)
                    : new NativeTraversal.StackBegin(beginCode, deps));
            instructions.remove(storeIdx);
            if (viewIndex >= 0) instructions.remove(viewIndex);
            if (foldedReceiver) instructions.remove(recvIdx);
            if (INTRINSIC_TRACE) {
                System.out.println("[FEI] placed begin for " + clsName + "." + methodName
                        + " at " + i + " recvIdx=" + recvIdx + " storeIdx=" + storeIdx
                        + " listSize=" + instructions.size()
                        + " atI=" + instructions.get(i - 1).getClass().getSimpleName());
            }
        }
    }

    private static String mangle(String t) {
        return t.replace('/', '_').replace('.', '_').replace('$', '_');
    }

    public int getForEachIntrinsicCount() {
        return forEachIntrinsicCount;
    }

    /// Offer a C stack buffer to the iterator of every for-each this method still has.
    ///
    /// Runs AFTER intrinsifyForEach, so it only sees the loops that could not be
    /// turned into indexed ones -- the ones whose receiver type is not provable, which is
    /// where nearly all the remaining iterator allocation lives. It needs no receiver
    /// type: the buffer is offered to whatever iterator() turns out to allocate, and only
    /// a class the escape analysis cleared will take it.
    ///
    /// The OFFER is scoped to the call, not to the loop. Withdrawing it immediately after
    /// the iterator is stored means a `break`, a `return` or a `throw` out of the loop
    /// body cannot leave a pointer to a dead frame pending for some later allocation to
    /// pick up. The buffer itself stays valid for the whole method, so the iterator living
    /// in it is unaffected -- what is short-lived is permission to take it, not the
    /// storage.
    /// -Dcn1.stackIterators=false turns the whole mechanism off, so the A/B is one
    /// translator flag rather than two source trees -- the only way to measure it without
    /// the comparison picking up an unrelated difference.
    // One-arg getProperty on purpose: the translator is compiled against vm/JavaAPI when
    // it self-hosts, and that System has no two-argument overload. A default supplied
    // here instead of there is the difference between building and not.
    static final int INTRINSIC_LIMIT = intProperty("cn1.iterIntrinsicLimit", -1);

    private static int intProperty(String name, int def) {
        String v = System.getProperty(name);
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }
    static final boolean INTRINSIC_TRACE =
            "true".equals(System.getProperty("cn1.iterIntrinsicTrace"));

    static final boolean STACK_ITERATORS =
            !"false".equals(System.getProperty("cn1.stackIterators"));

    public void markStackIterators() {
        if (!STACK_ITERATORS) {
            return;
        }
        for (int i = 0; i < instructions.size(); i++) {
            Instruction ins = instructions.get(i);
            if (!(ins instanceof Invoke)) {
                continue;
            }
            Invoke inv = (Invoke) ins;
            int op = inv.getOpcode();
            if (op != Opcodes.INVOKEINTERFACE && op != Opcodes.INVOKEVIRTUAL) {
                continue;
            }
            if (!"iterator".equals(inv.getName())
                    || !"()Ljava/util/Iterator;".equals(inv.getDesc())) {
                continue;
            }
            int[] v = validateForEach(i);
            if (v == null) {
                // -Dcn1.iteratorCensus=true: count the for-each sites that keep their
                // heap iterator, so the next widening is aimed at a measured shape rather
                // than a guessed one. 118 of 203 surviving iterator() sites were scoped
                // on the self-hosting corpus, and the 85 that were not are where the
                // residual 377k allocations live.
                stackIterRefused++;
                continue;
            }
            int id = stackIterCount++;
            stackIterScoped++;
            String begin = "    cn1IterScopeBegin(threadStateData, __cn1iterbuf_" + id + ");\n";
            String end = "    cn1IterScopeEnd(threadStateData);\n";
            // Back to front so the earlier index stays valid: the store is after the call.
            instructions.add(v[0] + 1, new CustomIntruction(end, end, new ArrayList<String>()));
            instructions.add(i, new CustomIntruction(begin, begin, new ArrayList<String>()));
            i++;   // step over the instruction just inserted
        }
    }

    public int getStackIterCount() {
        return stackIterCount;
    }

    /// The canonical javac for-each shape plus the iterator slot's lifetime, shared by
    /// both consumers of that proof: the indexed lowering, and the stack-iterator scope.
    ///
    /// Returns {storeIdx, itSlot, ld1, hn, ifq, ld2, nx, endIdx} or null. Keeping it in
    /// one place matters because the two callers rely on the SAME guarantee -- that the
    /// iterator is read only by hasNext() and next() and is dead after the loop. If they
    /// drifted apart, the stack-allocating one would be the one that crashed.
    private int[] validateForEach(int iterIdx) {
        int storeIdx = nextExecutable(iterIdx + 1);
        if (storeIdx < 0) {
            return null;
        }
        Instruction store = instructions.get(storeIdx);
        if (!(store instanceof VarOp) || store.getOpcode() != Opcodes.ASTORE) {
            return null;
        }
        int itSlot = ((VarOp) store).getIndex();
        if (itSlot < firstNonParameterSlot()) {
            return null;
        }
        // The condition label must follow the store immediately; anything executable in
        // between is not the shape javac emits.
        int condLabelIdx = -1;
        for (int k = storeIdx + 1; k < instructions.size(); k++) {
            Instruction in = instructions.get(k);
            if (in instanceof LabelInstruction) {
                condLabelIdx = k;
                break;
            }
            if (in instanceof LineNumber || in instanceof TryCatch) {
                continue;
            }
            break;
        }
        if (condLabelIdx < 0) {
            return null;
        }
        int ld1 = nextExecutable(condLabelIdx + 1);
        int hn = ld1 < 0 ? -1 : nextExecutable(ld1 + 1);
        int ifq = hn < 0 ? -1 : nextExecutable(hn + 1);
        int ld2 = ifq < 0 ? -1 : nextExecutable(ifq + 1);
        int nx = ld2 < 0 ? -1 : nextExecutable(ld2 + 1);
        if (nx < 0) {
            return null;
        }
        if (!isALoadOf(ld1, itSlot) || !isIteratorCall(hn, "hasNext", "()Z")) {
            return null;
        }
        Instruction jump = instructions.get(ifq);
        if (!(jump instanceof Jump) || jump.getOpcode() != Opcodes.IFEQ) {
            return null;
        }
        if (!isALoadOf(ld2, itSlot) || !isIteratorCall(nx, "next", "()Ljava/lang/Object;")) {
            return null;
        }
        Label endLabel = ((Jump) jump).getLabel();
        if (endLabel == null) {
            return null;
        }
        int endIdx = indexOfLabel(endLabel);
        if (endIdx <= nx) {
            return null;
        }
        // THE ITERATOR SLOT IS CHECKED OVER THIS LOOP'S LIFETIME, NOT OVER THE METHOD.
        // Counting ASTOREs and ALOADs across the whole method looks safer and is much
        // worse: javac REUSES one slot for the iterators of sequential for-each loops,
        // so a method with two of them counts two stores and four loads and every loop
        // in it is refused. That is not a corner -- it cost 38 of ~88 eligible sites on
        // the self-hosting corpus, and ByteCodeClass and BytecodeMethod are full of the
        // shape. What actually has to hold is narrower: between the store and the end
        // label the slot is read exactly at hasNext() and next() and written nowhere,
        // and after the loop it is redefined before it is ever read again.
        for (int k = storeIdx + 1; k < endIdx; k++) {
            if (k == ld1 || k == ld2) {
                continue;
            }
            if (!touchesSlot(instructions.get(k), itSlot)) {
                continue;
            }
            // A third read is the body calling it.remove(). An INDEXED loop cannot
            // express that, which is why this used to refuse outright -- but the C
            // intrinsic can: remove() there is a memmove, a size decrement and a step
            // back of the cursor. So the pair is allowed through, and the consumer that
            // cannot handle it (lowerForEachToIndexed) checks for itself.
            Instruction cur = instructions.get(k);
            if (cur instanceof VarOp && cur.getOpcode() == Opcodes.ALOAD
                    && ((VarOp) cur).getIndex() == itSlot) {
                int nxt = nextExecutable(k + 1);
                if (nxt >= 0 && instructions.get(nxt) instanceof Invoke
                        && "remove".equals(((Invoke) instructions.get(nxt)).getName())
                        && "()V".equals(((Invoke) instructions.get(nxt)).getDesc())) {
                    continue;
                }
            }
            return null;
        }
        if (!iteratorLifetimeEnds(storeIdx, itSlot, ld1, ld2, endIdx)) return null;
        return new int[] {storeIdx, itSlot, ld1, hn, ifq, ld2, nx, endIdx};
    }

    /** Follow every successor until this value is overwritten, including primitive
     * slot reuse and exception handlers. A textual first-store search can accept
     * a read reached by jumping around that store. */
    private boolean iteratorLifetimeEnds(int store, int slot, int hasLoad, int nextLoad, int end) {
        int size = instructions.size();
        boolean[] seen = new boolean[size];
        int[] work = new int[size];
        int handlerCount = 0;
        for (Instruction candidate : instructions) if (candidate instanceof TryCatch) handlerCount++;
        int[] handlers = new int[handlerCount * 3];
        int handlerIndex = 0;
        for (Instruction candidate : instructions) if (candidate instanceof TryCatch) {
            TryCatch handler = (TryCatch) candidate;
            handlers[handlerIndex++] = indexOfLabel(handler.getStart());
            handlers[handlerIndex++] = indexOfLabel(handler.getEnd());
            handlers[handlerIndex++] = indexOfLabel(handler.getHandler());
        }
        int read = 0, write = 0;
        if (store + 1 < size) { work[write++] = store + 1; seen[store + 1] = true; }
        while (read < write) {
            int at = work[read++];
            Instruction instruction = instructions.get(at);
            int op = instruction.getOpcode();
            if (touchesSlot(instruction, slot)) {
                if (instruction instanceof VarOp && op >= Opcodes.ISTORE && op <= Opcodes.ASTORE) continue;
                boolean allowed = at == hasLoad || at == nextLoad;
                if (!allowed && at < end && isALoadOf(at, slot)) {
                    int call = nextExecutable(at + 1);
                    allowed = call >= 0 && isIteratorCall(call, "remove", "()V");
                }
                if (!allowed) return false;
            }
            // Exception edges keep the pre-instruction local value. Stores and
            // primitive operations cannot throw; including their handlers is a
            // conservative over-approximation for this proof.
            for (int h = 0; h < handlers.length; h += 3) {
                if (at >= handlers[h] && at < handlers[h + 1]) {
                    int target = handlers[h + 2];
                    if (target < 0) return false;
                    if (!seen[target]) { seen[target] = true; work[write++] = target; }
                }
            }
            if (instruction instanceof Jump) {
                int target = indexOfLabel(((Jump) instruction).getLabel());
                if (target < 0) return false;
                if (!seen[target]) { seen[target] = true; work[write++] = target; }
                if (op == Opcodes.GOTO) continue;
                if (op == Opcodes.JSR) return false;
            } else if (instruction instanceof SwitchInstruction) {
                SwitchInstruction branch = (SwitchInstruction) instruction;
                for (Label label : branch.getLabels()) {
                    int target = indexOfLabel(label);
                    if (target < 0) return false;
                    if (!seen[target]) { seen[target] = true; work[write++] = target; }
                }
                int target = indexOfLabel(branch.getDefaultLabel());
                if (target < 0) return false;
                if (!seen[target]) { seen[target] = true; work[write++] = target; }
                continue;
            }
            if (op == Opcodes.ATHROW || op >= Opcodes.IRETURN && op <= Opcodes.RETURN) continue;
            if (op == Opcodes.RET) return false;
            if (at + 1 < size && !seen[at + 1]) { seen[at + 1] = true; work[write++] = at + 1; }
        }
        return true;
    }

    private boolean isALoadOf(int idx, int slot) {
        Instruction ins = instructions.get(idx);
        return ins instanceof VarOp && ins.getOpcode() == Opcodes.ALOAD
                && ((VarOp) ins).getIndex() == slot;
    }

    private boolean isIteratorCall(int idx, String name, String desc) {
        Instruction ins = instructions.get(idx);
        if (!(ins instanceof Invoke)) {
            return false;
        }
        Invoke iv = (Invoke) ins;
        return "java/util/Iterator".equals(iv.getOwner()) && name.equals(iv.getName())
                && desc.equals(iv.getDesc());
    }

    /**
     * ITERATOR LOWERING: give a for-each loop the concrete Iterator type its
     * collection really returns, so the calls stop going through the interface.
     *
     * A for-each compiles to Iterator.hasNext()/next() through INVOKEINTERFACE,
     * which is the most expensive dispatch the VM has -- a lookup in the owning
     * class's interface map before the vtable read -- and it runs twice per
     * element. Neither the emitter's closed-world devirtualization nor ThinLTO
     * can touch it, because both start from a concrete owner and an interface
     * call does not have one: java.util.Iterator has 27 implementors here.
     *
     * The concrete type is recoverable locally even though the translator has no
     * general stack-type inference. If the collection's iterator() has exactly
     * one reachable implementation, and that implementation's whole body is
     * `return new T(...)`, then the object stored by the ASTORE that follows the
     * call is a T -- no inference needed. Retyping the calls to INVOKEVIRTUAL on
     * T is then enough on its own: the existing devirtualization in
     * Invoke.appendInstruction takes any virtual call with no reachable override
     * the rest of the way to a direct one, which ThinLTO can inline.
     *
     * The single-assignment requirement on the local is what makes this sound
     * without dataflow. If a slot were written twice, a second iterator of some
     * other class could reach the same ALOAD, and a virtual call on the wrong
     * class reads its fields out of an object that does not have them -- silent
     * on this VM, since ParparVM's CHECKCAST is unchecked.
     *
     * Like the concat fusion this must run BEFORE the unused-method cull, so the
     * newly created edges exist while reachability is computed.
     */
    public void lowerIteratorCalls() {
        for (int i = 0; i < instructions.size(); i++) {
            Instruction ins = instructions.get(i);
            if (!(ins instanceof Invoke)) {
                continue;
            }
            Invoke inv = (Invoke) ins;
            int op = inv.getOpcode();
            if (op != Opcodes.INVOKEINTERFACE && op != Opcodes.INVOKEVIRTUAL) {
                continue;
            }
            if (!"iterator".equals(inv.getName()) || !"()Ljava/util/Iterator;".equals(inv.getDesc())) {
                continue;
            }
            ByteCodeClass coll = Parser.getClassObject(Util.mangle(inv.getOwner()));
            String itType = Parser.resolveConcreteIteratorType(coll);
            if (itType == null) {
                continue;
            }
            int st = nextExecutable(i + 1);
            if (st < 0) {
                continue;
            }
            Instruction store = instructions.get(st);
            if (!(store instanceof VarOp) || store.getOpcode() != Opcodes.ASTORE) {
                continue;
            }
            int slot = ((VarOp) store).getIndex();
            // Exactly one ASTORE is not enough on its own: a parameter reaches its
            // slot without one, so a method that takes an Iterator and later reuses
            // that slot for this loop's iterator has TWO values in it. Rewriting the
            // parameter's calls to the concrete type would dispatch methods that
            // read the wrong object layout -- unchecked, on this VM.
            if (slot < firstNonParameterSlot() || countStoresTo(slot) != 1) {
                continue;
            }
            retypeIteratorUses(slot, itType, st);
        }
    }

    /// @param storeIdx index of the ASTORE that put the concrete iterator in the
    ///                 slot; only uses AFTER it are rewritten, since anything
    ///                 earlier cannot be reading the value this store wrote
    private void retypeIteratorUses(int slot, String itType, int storeIdx) {
        ByteCodeClass itClass = Parser.getClassObject(Util.mangle(itType));
        if (itClass == null) {
            return;
        }
        for (int i = storeIdx + 1; i < instructions.size(); i++) {
            Instruction ins = instructions.get(i);
            if (!(ins instanceof Invoke) || ins.getOpcode() != Opcodes.INVOKEINTERFACE) {
                continue;
            }
            Invoke inv = (Invoke) ins;
            if (!"java/util/Iterator".equals(inv.getOwner())) {
                continue;
            }
            int r = prevExecutable(i - 1);
            if (r < 0) {
                continue;
            }
            Instruction recv = instructions.get(r);
            if (!(recv instanceof VarOp) || recv.getOpcode() != Opcodes.ALOAD
                    || ((VarOp) recv).getIndex() != slot) {
                continue;
            }
            // The concrete class has to actually resolve the method, and resolve it
            // monomorphically -- otherwise the retyped call has nothing to bind to.
            if (Parser.resolveDevirtualizedOwner(itClass, inv.getName(), inv.getDesc()) == null) {
                continue;
            }
            Invoke direct = new Invoke(Opcodes.INVOKEVIRTUAL, itType, inv.getName(), inv.getDesc(), false);
            instructions.set(i, direct);
            // Register it exactly as addInstruction() would: the list entry alone
            // leaves the call with no owning method, no class dependency and no
            // edge in the dependency graph, so the cull would not see the concrete
            // iterator's methods being called.
            direct.setMethod(this);
            direct.addDependencies(dependentClasses);
            if (dependencyGraph != null) {
                dependencyGraph.recordMethodCall(this, direct.getMethodUsed());
            }
        }
    }

    /**
     * ELIDE String.toCharArray() WHERE THE ARRAY IS ONLY EVER READ.
     *
     * toCharArray() is 22.8% of all char[] allocations on the self-hosting corpus --
     * 293,175 of 1,285,250 -- and most of those arrays exist only to be scanned and
     * dropped. The array is a COPY by contract, so the allocation cannot be removed by
     * sharing it: com.codename1.io.Util.toCharArray exists precisely because some JVMs
     * returned the backing store, calls that "a serious security hole in the JVM", and
     * detects it at runtime with `s.toCharArray() == s.toCharArray()`. Handing back the
     * backing array would make every String mutable through its own accessor AND flip
     * that expression. It would also do nothing for a COMPACT string, which has a byte[]
     * and no char[] to hand back.
     *
     * So do not build the array. Where the result is stored to a local whose every use
     * is a read, the local can hold the STRING instead and each read becomes a call:
     *
     *     ALOAD a; ARRAYLENGTH          ->  ALOAD a; String.length()
     *     ALOAD a; &lt;index&gt;; CALOAD      ->  ALOAD a; &lt;index&gt;; String.charAt(I)
     *
     * and the toCharArray() call itself is deleted so the ASTORE stores the receiver.
     * Nothing aliases or mutates an array that does not exist, and on a compact string
     * charAt is a byte load and a mask.
     *
     * THE SUBSTITUTIONS ARE STACK-NEUTRAL BY CONSTRUCTION -- CALOAD and charAt are both
     * pop-2/push-1, ARRAYLENGTH and length are both pop-1/push-1 -- so maxStack does not
     * move and no local is added. That is deliberate: of the three correctness defects
     * that got the earlier bytecode rewrites on this branch withdrawn, one was maxStack
     * under-reservation and one was running after the cull. This pass avoids the first by
     * changing no stack depth, and the second by running from Parser BEFORE
     * eliminateUnusedMethods, registering each created call the way addInstruction would.
     *
     * The third was receiver identity, and the answer here is to refuse anything not
     * trivially provable rather than to track the operand stack. A use qualifies ONLY as
     * `ALOAD slot` immediately followed by ARRAYLENGTH, or by a single constant/local int
     * push and then CALOAD. `a[b[i]]`, a computed index, or any other shape disqualifies
     * the whole site. That covers the indexed loop and the array for-each, which is what
     * the scanner found in practice, and nothing it cannot prove.
     *
     * Parameter slots are excluded for the same reason lowerIteratorCalls excludes them:
     * a parameter reaches its slot with no ASTORE, so a single store does not mean a
     * single value.
     *
     * ONE ACCEPTED SEMANTIC DIFFERENCE: on a null receiver the NullPointerException now
     * arises at the first length()/charAt() rather than at toCharArray(). Both are inside
     * this method and both are an NPE; only the line number can differ. On this VM the
     * distinction is already academic -- no null check is emitted for either
     * (CN1_INCLUDE_NPE_CHECKS is off), so both are a native fault the iOS handler
     * converts. Sites with no uses at all are skipped rather than reasoned about, so the
     * call is never removed from a method that does nothing else with it.
     */
    public void elideToCharArrayScans() {
        for (int i = 0; i < instructions.size(); i++) {
            Instruction ins = instructions.get(i);
            if (!(ins instanceof Invoke) || ins.getOpcode() != Opcodes.INVOKEVIRTUAL) {
                continue;
            }
            Invoke inv = (Invoke) ins;
            if (!"java/lang/String".equals(inv.getOwner())
                    || !"toCharArray".equals(inv.getName())
                    || !"()[C".equals(inv.getDesc())) {
                continue;
            }
            int st = nextExecutable(i + 1);
            if (st < 0) {
                continue;
            }
            Instruction store = instructions.get(st);
            if (!(store instanceof VarOp) || store.getOpcode() != Opcodes.ASTORE) {
                continue;
            }
            int slot = ((VarOp) store).getIndex();
            if (slot < firstNonParameterSlot() || countStoresTo(slot) != 1) {
                continue;
            }
            if (!charArrayUsesAreReadOnly(slot, st)) {
                continue;
            }
            rewriteCharArrayReads(slot, st);
            instructions.remove(i);
            i--;
        }
    }

    /// True when EVERY use of {@code slot} after {@code storeIdx} is one of the two
    /// read shapes this pass can rewrite, and there is at least one. Validation is a
    /// separate pass from the rewrite on purpose: a site that fails halfway must leave
    /// the instruction stream untouched.
    private boolean charArrayUsesAreReadOnly(int slot, int storeIdx) {
        int uses = 0;
        for (int i = storeIdx + 1; i < instructions.size(); i++) {
            Instruction ins = instructions.get(i);
            if (!(ins instanceof VarOp) || ((VarOp) ins).getIndex() != slot) {
                continue;
            }
            if (ins.getOpcode() != Opcodes.ALOAD) {
                return false;
            }
            if (classifyCharArrayRead(i) < 0) {
                return false;
            }
            uses++;
        }
        return uses > 0;
    }

    /// @return the index of the ARRAYLENGTH or CALOAD this ALOAD feeds, or -1 when the
    ///         shape is anything else -- which includes the array escaping into a call,
    ///         a store, a return, or an index expression this pass refuses to reason about
    private int classifyCharArrayRead(int aloadIdx) {
        int n = nextExecutable(aloadIdx + 1);
        if (n < 0) {
            return -1;
        }
        if (instructions.get(n).getOpcode() == Opcodes.ARRAYLENGTH) {
            return n;
        }
        // a[i]: exactly ONE simple int push may sit between the ALOAD and the CALOAD.
        // Anything else -- a call, another array read, arithmetic -- is refused rather
        // than tracked, because tracking the operand stack is what this pass is
        // deliberately not doing.
        if (!isSimpleIntPush(instructions.get(n))) {
            return -1;
        }
        int c = nextExecutable(n + 1);
        if (c < 0 || instructions.get(c).getOpcode() != Opcodes.CALOAD) {
            return -1;
        }
        return c;
    }

    private boolean isSimpleIntPush(Instruction ins) {
        int op = ins.getOpcode();
        if (ins instanceof VarOp && op == Opcodes.ILOAD) {
            return true;
        }
        return op == Opcodes.ICONST_0 || op == Opcodes.ICONST_1 || op == Opcodes.ICONST_2
                || op == Opcodes.ICONST_3 || op == Opcodes.ICONST_4 || op == Opcodes.ICONST_5
                || op == Opcodes.BIPUSH || op == Opcodes.SIPUSH;
    }

    private void rewriteCharArrayReads(int slot, int storeIdx) {
        for (int i = storeIdx + 1; i < instructions.size(); i++) {
            Instruction ins = instructions.get(i);
            if (!(ins instanceof VarOp) || ((VarOp) ins).getIndex() != slot
                    || ins.getOpcode() != Opcodes.ALOAD) {
                continue;
            }
            int target = classifyCharArrayRead(i);
            if (target < 0) {
                continue;   // cannot happen: validated above
            }
            boolean isLength = instructions.get(target).getOpcode() == Opcodes.ARRAYLENGTH;
            Invoke call = isLength
                    ? new Invoke(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false)
                    : new Invoke(Opcodes.INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
            instructions.set(target, call);
            // Register it exactly as addInstruction() would; see retypeIteratorUses for
            // why the list entry alone is not enough.
            call.setMethod(this);
            call.addDependencies(dependentClasses);
            if (dependencyGraph != null) {
                dependencyGraph.recordMethodCall(this, call.getMethodUsed());
            }
        }
    }

    public Set<LocalVariable> getLocalVariables() {
        return localVariables;
    }
    
    public void addDebugInfo(int line) {
        if (disableDebugInfo) {
            return;
        }
        addInstruction(new LineNumber(sourceFile, line));
    }
    
    public void addLabel(Label l) {
        // Named here, in bytecode order, so the generated C label is a function of the
        // method alone. See LabelInstruction.assignLabelName.
        com.codename1.tools.translator.bytecodes.LabelInstruction.assignLabelName(l, nextLabelIndex);
        nextLabelIndex++;
        addInstruction(new com.codename1.tools.translator.bytecodes.LabelInstruction(l));
    }
    
    public void addInvoke(int opcode, String owner, String name, String desc, boolean itf) {
        addInstruction(new Invoke(opcode, owner, name, desc, itf));
    }
    
    /** Per-method label counter; see addLabel. */
    private int nextLabelIndex;

    public void setMaxes(int maxStack, int maxLocals) {
        this.maxLocals = maxLocals;
        this.maxStack = maxStack;
    }
    
    private void addInstruction(Instruction i) {
        instructions.add(i);
        i.setMethod(this);
        i.addDependencies(dependentClasses);
        if (dependencyGraph != null) {
            String methodUsed = i.getMethodUsed();
            if (methodUsed != null) {
                dependencyGraph.recordMethodCall(this, methodUsed);
            }
        }
    }
    
    public void addVariableOperation(int opcode, int var) {
        VarOp op = new VarOp(opcode, var);
        LocalVariable lv = null;
        switch (opcode) {
            case Opcodes.ISTORE:
                lv = new LocalVariable("v"+var, "I", "I", null, null, var); break;
            case Opcodes.LSTORE:
                lv = new LocalVariable("v"+var, "J", "J", null, null, var); break;    
            case Opcodes.FSTORE:
                lv = new LocalVariable("v"+var, "F", "F", null, null, var); break;
            case Opcodes.DSTORE:
                lv = new LocalVariable("v"+var, "D", "D", null, null, var); break;
        }
        if (lv != null && !localVariables.contains(lv)) {
            localVariables.add(lv);
        }
        addInstruction(op);
    }
    
    public void addTypeInstruction(int opcode, String type) {
        addInstruction(new TypeInstruction(opcode, type));
    }
    
    /**
     * Allows us to detect if this is a very simple getter/setter in which case we 
     * can significantly optimize some operations
     */
    public boolean hasExceptionHandlingOrMethodCalls() {
        for(Instruction i : instructions) {
            if(i.isComplexInstruction()) {
                return true;
            }
        }
        return false;
    }
    
    public void addIInc(int var, int num) {
        addInstruction(new IInc(var, num));
    }

    public void addLdc(Object o) {
        addInstruction(new Ldc(o));
    }
    
    public void addJump(int opcode, Label label) {
        addInstruction(new Jump(opcode, label));
    }

    public void addField(ByteCodeClass cls, int opcode, String owner, String name, String desc) {
        if (cls.getOriginalClassName().equals(owner) && (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC)) {
            cls.addWritableField(name);
        }
        addInstruction(new Field(opcode, owner, name, desc));
    }
    
    public void addInstruction(int opcode) {
        addInstruction(new BasicInstruction(opcode, 0));
    }
    
    public void addInstruction(int opcode, int value) {
        addInstruction(new BasicInstruction(opcode, value));
    }
    
    public void addSwitch(Label dflt, int[] keys, Label[] labels) {
        addInstruction(new SwitchInstruction(dflt, keys, labels));
    }

    /**
     * @return the methodOffset
     */
    public int getMethodOffset() {
        return methodOffset;
    }

    /**
     * Emits the debugger-driven invoke thunk for this method. The thunk
     * has a uniform C signature so the registry can store all thunks in
     * one function-pointer table; it unpacks args from a {@code cn1_invoke_arg}
     * union array into the typed parameters the underlying translated
     * function expects, wraps the call in a catch-all try block so an
     * uncaught throw turns into {@code result.type='X'} rather than a
     * longjmp past the debugger's cond-wait, and packs the return value
     * back into the result union.
     */
    public void appendOnDeviceDebugInvokeThunk(String declaringClsName, StringBuilder b) {
        String symbol = declaringClsName + "_";
        if ("<init>".equals(methodName)) {
            // skipped at caller, but defensive
            return;
        } else if ("<clinit>".equals(methodName)) {
            return;
        }
        symbol += getCMethodName();
        // Append the descriptor suffix the translator uses
        // (args + _R<return> for non-void).
        StringBuilder argSuffix = new StringBuilder();
        for (ByteCodeMethodArg arg : arguments) {
            arg.appendCMethodExt(argSuffix);
        }
        if (!returnType.isVoid()) {
            argSuffix.append("_R");
            returnType.appendCMethodExt(argSuffix);
        }
        String fullSymbol = symbol + "__" + argSuffix.toString();
        // Choose virtual_<sym> only when the translator actually emits
        // one. Static, private, or methods marked virtualOverriden (the
        // class is final, so the dispatch was constant-folded) have no
        // virtual_ alias in their header, and the thunk has to call the
        // plain symbol or the C file won't compile.
        boolean useVirtualPrefix = !staticMethod && !privateMethod && !virtualOverriden;
        String callSymbol = useVirtualPrefix ? ("virtual_" + fullSymbol) : fullSymbol;
        int mid = methodOffset;

        b.append("static void __cn1_dbg_invoke_").append(mid)
          .append("(struct ThreadLocalData* threadStateData, JAVA_OBJECT thisObj, const cn1_invoke_arg* args, cn1_invoke_result* result) {\n");
        b.append("    (void)args; (void)thisObj;\n");
        b.append("    int __savedCallStack = threadStateData->callStackOffset;\n");
        b.append("    int __savedLocalsBegin = threadStateData->threadObjectStackOffset;\n");
        b.append("    int __savedTryBlock = threadStateData->tryBlockOffset;\n");
        b.append("    jmp_buf __tryJmp;\n");
        b.append("    if (CN1_TRY_SETJMP(__tryJmp) == 0) {\n");
        b.append("        threadStateData->blocks[threadStateData->tryBlockOffset].monitor = 0;\n");
        b.append("        threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass = 0;\n");
        b.append("        threadStateData->blocks[threadStateData->tryBlockOffset].nativeBuffers = threadStateData->nativeBuffers;\n");
        b.append("        memcpy(threadStateData->blocks[threadStateData->tryBlockOffset].destination, __tryJmp, sizeof(jmp_buf));\n");
        b.append("        threadStateData->tryBlockOffset++;\n");
        // Emit the actual call
        b.append("        ");
        if (returnType.isVoid()) {
            b.append(callSymbol).append("(threadStateData");
        } else {
            // Capture return into a typed temp, then pack.
            char rq = returnType.getQualifier();
            if (rq == 'o') b.append("JAVA_OBJECT __r = ");
            else if (rq == 'l') b.append("JAVA_LONG __r = ");
            else if (rq == 'd') b.append("JAVA_DOUBLE __r = ");
            else if (rq == 'f') b.append("JAVA_FLOAT __r = ");
            else b.append("JAVA_INT __r = ");
            b.append(callSymbol).append("(threadStateData");
        }
        if (!staticMethod) {
            b.append(", thisObj");
        }
        for (int i = 0; i < arguments.size(); i++) {
            ByteCodeMethodArg arg = arguments.get(i);
            char q = arg.getQualifier();
            b.append(", args[").append(i).append("].");
            switch (q) {
                case 'o': b.append("o"); break;
                case 'l': b.append("j"); break;
                case 'd': b.append("d"); break;
                case 'f': b.append("f"); break;
                default:  b.append("i"); break;
            }
        }
        b.append(");\n");
        // Pop our try block (no exception path) and store the result.
        b.append("        threadStateData->tryBlockOffset--;\n");
        if (returnType.isVoid()) {
            b.append("        result->type = 'V';\n");
        } else {
            char rq = returnType.getQualifier();
            String rtc;
            String slot;
            if (rq == 'o') { rtc = "L"; slot = "o"; }
            else if (rq == 'l') { rtc = "J"; slot = "j"; }
            else if (rq == 'd') { rtc = "D"; slot = "d"; }
            else if (rq == 'f') { rtc = "F"; slot = "f"; }
            else {
                // Sub-int types still pack through the int slot;
                // we communicate the real type via the type-char.
                String d = returnType.getQualifier() == 'i' ? returnTypeChar() : "I";
                rtc = d;
                slot = "i";
            }
            b.append("        result->type = '").append(rtc).append("';\n");
            b.append("        result->value.").append(slot).append(" = __r;\n");
        }
        b.append("    } else {\n");
        b.append("        result->type = 'X';\n");
        b.append("        result->value.o = threadStateData->exception;\n");
        b.append("        threadStateData->exception = JAVA_NULL;\n");
        b.append("        threadStateData->callStackOffset = __savedCallStack;\n");
        b.append("        threadStateData->threadObjectStackOffset = __savedLocalsBegin;\n");
        b.append("        threadStateData->tryBlockOffset = __savedTryBlock;\n");
        b.append("    }\n");
        b.append("}\n");
    }

    /**
     * Returns the JDWP type-char for the method's return type. Only used
     * by {@link #appendOnDeviceDebugInvokeThunk} for sub-int primitive
     * returns where the C variable is JAVA_INT but the wire-level type
     * is more specific (e.g. boolean / byte / short / char).
     */
    private String returnTypeChar() {
        if (returnType.getPrimitiveType() == PrimitiveType.BOOLEAN) return "Z";
        if (returnType.getPrimitiveType() == PrimitiveType.BYTE)    return "B";
        if (returnType.getPrimitiveType() == PrimitiveType.SHORT)   return "S";
        if (returnType.getPrimitiveType() == PrimitiveType.CHAR) return "C";
        return "I";
    }

    /**
     * @param methodOffset the methodOffset to set
     */
    public void setMethodOffset(int methodOffset) {
        this.methodOffset = methodOffset;
    }

    /**
     * @return the staticMethod
     */
    public boolean isMain() {
        return staticMethod && methodName.equals("main") && arguments.size() == 1 && arguments.get(0).getArrayDimensions() == 1;
    }
    
    public boolean isDefaultConstructor() {
        return constructor && arguments.size() == 0;
    }

    private String cMethodName;
    
    /**
     * Gets the method name, mangled to be usable as the C method name.  This will replace illegal characters
     * with underscores.
     * @return 
     */
    public String getCMethodName() {
        if (cMethodName == null) {
            cMethodName = methodName.replace('-','_');
        }
        return cMethodName;
    }
    
    /**
     * @return the clsName
     */
    public String getClsName() {
        return clsName;
    }
    
    public boolean isFinalizer() {
        return methodName.equals("finalize") && arguments.size() == 0;
    }

    /**
     * @return the virtualOverriden
     */
    public boolean isVirtualOverriden() {
        return virtualOverriden;
    }

    /** True if the method is declared {@code final} (cannot be overridden). */
    public boolean isFinal() {
        return finalMethod;
    }

    /**
     * @param virtualOverriden the virtualOverriden to set
     */
    public void setVirtualOverriden(boolean virtualOverriden) {
        this.virtualOverriden = virtualOverriden;
    }

    /**
     * @return the eliminated
     */
    public boolean isEliminated() {
        return eliminated;
    }

    /**
     * @param eliminated the eliminated to set
     */
    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }



    private int varCounter = 0;
    // Master off-switch: -DCN1_DISABLE_BCE=true reverts to fully-checked array access.
    private static final boolean DISABLE_BCE =
            "true".equalsIgnoreCase(Util.getProperty("CN1_DISABLE_BCE", "false"));

    /**
     * Prove-safe array-bounds-check elimination. Conservative and fail-closed:
     * marks an array LOAD bounds-safe only for the canonical counted loop
     * {@code for (int i = <const >= 0>; i < arr.length; i++) { ... arr[i] ... }}
     * recognised in the RAW (pre-reduction) bytecode IR. Because the loop
     * condition re-evaluates {@code arr.length} every iteration, entering the body
     * proves {@code arr != null} and {@code i < arr.length}; we additionally prove
     * {@code i} is monotonic non-negative and that neither {@code i} nor
     * {@code arr} is mutated between the loop top and the access, and that nothing
     * branches into the body bypassing the test. Any construct we don't model
     * precisely (try/catch, switch, computed jumps, non-constant init) bails.
     */
    /* BOUNDS-CHECK ELIMINATION CENSUS (-Dcn1.bceCensus=true).
     *
     * The other two guardrails this method's javadoc names -- frameless codegen and
     * StringBuilder stack allocation -- have both been counted, and the answers were
     * 1% of methods and 17% of sites. This one never was, and it is the one that
     * costs a compare and a branch on every array access rather than a frame or an
     * allocation. Count what try/catch alone refuses before deciding it is worth
     * modelling exception edges to recover.
     */
    static final boolean BCE_CENSUS =
            "true".equalsIgnoreCase(Util.getProperty("cn1.bceCensus", "false"));
    static int bceMethods, bceMethodsWithArrays, bceRefusedTryCatch,
               bceArrayOpsTotal, bceArrayOpsRefusedTryCatch;
    /* Loops that matched the counted shape and were then refused because a handler
     * landed inside them. Non-vacuity evidence for bceHandlerLandsIn: if this is 0
     * over a corpus the size of the translator's own, the check is not a guard, it
     * is decoration, and that is worth knowing. */
    static int bceLoopsRefusedByHandler;
    /* Array accesses this pass actually cleared, split by whether the method has a
     * try/catch at all. The second number IS the change: before the per-loop check
     * it was necessarily zero, because such a method never reached the analysis. */
    static int bceAccessesMarked, bceAccessesMarkedInTryCatchMethod;
    /* WHY a candidate loop was rejected, first failing precondition wins. 106 of the
     * corpus' 19,164 array accesses are cleared, so the try/catch guardrail was never
     * the thing holding this pass back -- the shape it recognizes is. These say which
     * part of the shape does the refusing, so the widening is chosen rather than
     * guessed at. Indices match BCE_WHY below. */
    static final String[] BCE_WHY = {
        "exitNotForward", "lengthNotArraylength", "arrayNotLocal", "indexNotLocal",
        "noHeaderLabel", "handlerInside", "arrayIsIndex", "inductionNotProven",
        "headerHasOtherEntries", "arrayWrittenInBody", "accepted", "hoistedLengthUnproven"
    };
    static final int[] bceWhy = new int[BCE_WHY.length];
    /* Array ops inside an ACCEPTED loop's body that were still not cleared, by reason. */
    static final String[] BCE_MISS = {
        "notALoad", "indexNotInductionVar", "arrayNotLoopArray", "indexWrittenBeforeAccess",
        "foreignEntry", "cleared"
    };
    static final int[] bceMiss = new int[BCE_MISS.length];
    /* Which half of the hoisted-length proof refused. Only 20 of 1,154 candidates got
     * through the first version, and "the corpus does not have that shape" and "the
     * rule is too strict" look identical from the outside. */
    static final String[] BCE_HOIST = {
        "lengthLocalWrittenTwice", "lengthLocalNoCapture", "captureAfterHeader",
        "arraySlotWritten", "captureNotDominating", "unused", "proven"
    };
    static final int[] bceHoist = new int[BCE_HOIST.length];

    private int countArrayOps() {
        int c = 0;
        for (Instruction in : instructions) {
            int op = in.getOpcode();
            if (op == Opcodes.IALOAD || op == Opcodes.AALOAD || op == Opcodes.BALOAD
                    || op == Opcodes.CALOAD || op == Opcodes.SALOAD || op == Opcodes.LALOAD
                    || op == Opcodes.FALOAD || op == Opcodes.DALOAD
                    || op == Opcodes.IASTORE || op == Opcodes.AASTORE || op == Opcodes.BASTORE
                    || op == Opcodes.CASTORE || op == Opcodes.SASTORE || op == Opcodes.LASTORE
                    || op == Opcodes.FASTORE || op == Opcodes.DASTORE) {
                c++;
            }
        }
        return c;
    }

    void analyzeBoundsChecks() {
        if (DISABLE_BCE) {
            return;
        }
        if (BCE_CENSUS && !nativeMethod && !abstractMethod && !eliminated) {
            int ops = countArrayOps();
            bceMethods++;
            bceArrayOpsTotal += ops;
            if (ops > 0) {
                bceMethodsWithArrays++;
                // The instruction scan, NOT TryCatch.isTryCatchInMethod(): that
                // static flag is set in TryCatch.appendInstruction and cleared by
                // TryCatch.reset() inside appendMethodC, i.e. both happen during
                // EMISSION, while this pass runs in optimize(), before any of that.
                // Read here it answers for whichever method was emitted last, so
                // the first census over-counted by however many try/catch-free
                // methods happened to follow one that had them.
                boolean tc = false;
                for (Instruction in : instructions) {
                    if (in instanceof TryCatch) { tc = true; break; }
                }
                if (tc) {
                    bceRefusedTryCatch++;
                    bceArrayOpsRefusedTryCatch += ops;
                }
            }
        }
        // View without LineNumber noise but keeping labels for back-edge detection.
        java.util.ArrayList<Instruction> r = new java.util.ArrayList<Instruction>(instructions.size());
        java.util.ArrayList<Label> handlers = null;
        for (Instruction in : instructions) {
            if (in instanceof LineNumber) {
                continue;
            }
            if (in instanceof TryCatch) {
                // A try/catch used to disable this pass for the WHOLE method, and it
                // was the most expensive of the three guardrails try/catch controls:
                // 20% of the corpus' array accesses sat in a method it refused,
                // against 17% of StringBuilder sites and 1% of methods for frameless.
                //
                // It refuses less than that now. A TryCatch instruction is a
                // DECLARATION of an exception edge, not the edge itself, so it is not
                // control flow in this positional view and is kept out of it (leaving
                // it in would also break the fixed-width condition-shape window
                // below). What the edge can do is land control INSIDE a loop without
                // running its test -- the same hazard bceForeignEntry already refuses
                // for an ordinary Jump, minus the instruction that makes it visible.
                // So the landing pad is what matters: record it, and refuse per loop.
                if (handlers == null) {
                    handlers = new java.util.ArrayList<Label>();
                }
                handlers.add(((TryCatch) in).getHandler());
                continue;
            }
            // Control flow we don't model precisely -> disable BCE for the whole method.
            if (in instanceof SwitchInstruction || in instanceof CustomJump) {
                return;
            }
            r.add(in);
        }
        // NOT TryCatch.isTryCatchInMethod() -- that flag belongs to emission (see the
        // census note above), and reading it here answered for a different method.
        int n = r.size();
        java.util.HashMap<Label, Integer> pos = new java.util.HashMap<Label, Integer>();
        for (int i = 0; i < n; i++) {
            Instruction x = r.get(i);
            if (x instanceof LabelInstruction) {
                pos.put(((LabelInstruction) x).getLabel(), i);
            }
        }
        // Resolve every landing pad to a position. A handler we cannot locate is a
        // transfer we cannot reason about, so the method goes back to being refused
        // whole -- exactly today's behaviour, reached only when the label is missing.
        int[] handlerAt = NO_HANDLERS;
        if (handlers != null) {
            handlerAt = new int[handlers.size()];
            for (int i = 0; i < handlerAt.length; i++) {
                Integer hp = pos.get(handlers.get(i));
                if (hp == null) {
                    return;
                }
                handlerAt[i] = hp;
            }
        }
        // Canonical javac top-test counted loop:
        //   ISTORE i(=const>=0)
        //   header: ILOAD i ; ALOAD a ; ARRAYLENGTH ; IF_ICMPGE exit   (forward)
        //   body:   ... a[i] ...
        //   IINC i,+k ; GOTO header
        //   exit:
        // Falling through the IF_ICMPGE proves i < a.length (and a != null, since
        // ARRAYLENGTH ran); the body is dominated by that test.
        for (int jx = 4; jx < n; jx++) {
            Instruction ji = r.get(jx);
            if (!(ji instanceof Jump) || ji.getOpcode() != Opcodes.IF_ICMPGE) {
                continue;
            }
            Jump j = (Jump) ji;
            Integer exitP = pos.get(j.getLabel());
            if (exitP == null || exitP <= jx) {
                if (BCE_CENSUS) { bceWhy[0]++; }
                continue; // exit must be a forward target
            }
            int exit = exitP;
            // Two condition shapes. The original one reads the length at the test:
            //
            //     header: ILOAD i ; ALOAD a ; ARRAYLENGTH ; IF_ICMPGE exit
            //
            // and the second hoists it into a local, which is what NINE TENTHS of this
            // corpus' counted loops actually compile to (2,384 candidates against 133
            // for the direct form -- measured, see the census):
            //
            //     ALOAD a ; ARRAYLENGTH ; ISTORE n
            //     header: ILOAD i ; ILOAD n ; IF_ICMPGE exit
            //
            // bceHoistedLengthArray proves n IS a.length rather than assuming it, and
            // hands back the array's slot so everything below is unchanged.
            int header;
            int arrVar;
            int indVar;
            Instruction len = r.get(jx - 1);
            if (len.getOpcode() == Opcodes.ARRAYLENGTH) {
                Instruction arr = r.get(jx - 2);
                Instruction idx = r.get(jx - 3);
                Instruction hdr = r.get(jx - 4);
                if (!(arr instanceof VarOp) || arr.getOpcode() != Opcodes.ALOAD) { if (BCE_CENSUS) { bceWhy[2]++; } continue; }
                if (!(idx instanceof VarOp) || idx.getOpcode() != Opcodes.ILOAD) { if (BCE_CENSUS) { bceWhy[3]++; } continue; }
                if (!(hdr instanceof LabelInstruction)) { if (BCE_CENSUS) { bceWhy[4]++; } continue; }     // header label the back-edge returns to
                header = jx - 4;
                arrVar = ((VarOp) arr).getIndex();
                indVar = ((VarOp) idx).getIndex();
            } else {
                if (!(len instanceof VarOp) || len.getOpcode() != Opcodes.ILOAD) { if (BCE_CENSUS) { bceWhy[1]++; } continue; }
                Instruction idx = r.get(jx - 2);
                Instruction hdr = r.get(jx - 3);
                if (!(idx instanceof VarOp) || idx.getOpcode() != Opcodes.ILOAD) { if (BCE_CENSUS) { bceWhy[3]++; } continue; }
                if (!(hdr instanceof LabelInstruction)) { if (BCE_CENSUS) { bceWhy[4]++; } continue; }
                header = jx - 3;
                indVar = ((VarOp) idx).getIndex();
                arrVar = bceHoistedLengthArray(r, pos, ((VarOp) len).getIndex(), header, handlerAt);
                if (arrVar < 0) { if (BCE_CENSUS) { bceWhy[11]++; } continue; }
            }
            // An exception edge that lands anywhere in [header, exit) enters the
            // condition or the body without IF_ICMPGE having run, so i < a.length is
            // no longer established there and the whole proof below collapses. A pad
            // OUTSIDE the loop is just another way to LEAVE it; a jump from there
            // back in is an ordinary Jump, which bceForeignEntry still refuses.
            if (bceHandlerLandsIn(handlerAt, header, exit)) {
                if (BCE_CENSUS) {
                    bceLoopsRefusedByHandler++;
                    bceWhy[5]++;
                }
                continue;
            }
            if (arrVar == indVar) { if (BCE_CENSUS) { bceWhy[6]++; } continue; }
            if (!bceInductionMonotonicNonNegative(r, indVar)) { if (BCE_CENSUS) { bceWhy[7]++; } continue; }
            if (bceCountJumpsTargeting(r, header) != 1) { if (BCE_CENSUS) { bceWhy[8]++; } continue; }          // only the back-edge enters the header
            if (bceLocalWrittenInRange(r, arrVar, jx, exit)) { if (BCE_CENSUS) { bceWhy[9]++; } continue; }     // array invariant in loop body
            if (BCE_CENSUS) { bceWhy[10]++; }

            for (int k = jx + 1; k < exit; k++) {
                Instruction ld = r.get(k);
                if (bceIsArrayStoreOpcode(ld.getOpcode())) {
                    // a[i] = <value>. The value expression sits BETWEEN the index and
                    // the store, so there is nothing adjacent to match -- walk the
                    // operand stack back from the store to find where it began. A call
                    // in the value cannot touch this frame's locals and cannot change
                    // an array's length, so the proof is the same one the load path
                    // uses; only the way the pair is located differs.
                    int vbase = bceStoreValueBase(r, k);
                    if (vbase < jx + 3) { if (BCE_CENSUS) { bceMiss[0]++; } continue; }
                    Instruction si = r.get(vbase - 1);
                    Instruction sa = r.get(vbase - 2);
                    if (!(si instanceof VarOp) || si.getOpcode() != Opcodes.ILOAD || ((VarOp) si).getIndex() != indVar) { if (BCE_CENSUS) { bceMiss[1]++; } continue; }
                    if (!(sa instanceof VarOp) || sa.getOpcode() != Opcodes.ALOAD || ((VarOp) sa).getIndex() != arrVar) { if (BCE_CENSUS) { bceMiss[2]++; } continue; }
                    if (bceLocalWrittenInRange(r, indVar, jx, k)) { if (BCE_CENSUS) { bceMiss[3]++; } continue; }
                    if (bceForeignEntry(r, pos, header, k, j)) { if (BCE_CENSUS) { bceMiss[4]++; } continue; }
                    ld.markBoundsSafe();
                    if (BCE_CENSUS) {
                        bceMiss[5]++;
                        bceAccessesMarked++;
                        if (handlerAt.length > 0) { bceAccessesMarkedInTryCatchMethod++; }
                    }
                    continue;
                }
                if (!bceIsArrayLoadOpcode(ld.getOpcode())) {
                    if (BCE_CENSUS && bceIsArrayOpcode(ld.getOpcode())) { bceMiss[0]++; }
                    continue;
                }
                Instruction li = r.get(k - 1);
                Instruction la = r.get(k - 2);
                if (!(li instanceof VarOp) || li.getOpcode() != Opcodes.ILOAD || ((VarOp) li).getIndex() != indVar) { if (BCE_CENSUS) { bceMiss[1]++; } continue; }
                if (!(la instanceof VarOp) || la.getOpcode() != Opcodes.ALOAD || ((VarOp) la).getIndex() != arrVar) { if (BCE_CENSUS) { bceMiss[2]++; } continue; }
                if (bceLocalWrittenInRange(r, indVar, jx, k)) { if (BCE_CENSUS) { bceMiss[3]++; } continue; }     // i unchanged test->access
                if (bceForeignEntry(r, pos, header, k, j)) { if (BCE_CENSUS) { bceMiss[4]++; } continue; }        // no bypass entry into cond/body
                if (BCE_CENSUS) { bceMiss[5]++; }
                ld.markBoundsSafe();
                if (BCE_CENSUS) {
                    bceAccessesMarked++;
                    if (handlerAt.length > 0) {
                        bceAccessesMarkedInTryCatchMethod++;
                    }
                }
            }
        }
    }

    private static final int[] NO_HANDLERS = new int[0];

    // True if any exception landing pad sits in [from, to). Linear: a method has a
    // handful of handlers, and the alternative (a sorted array plus a binary search)
    // would cost more to get right than it saves.
    private static boolean bceHandlerLandsIn(int[] handlerAt, int from, int to) {
        for (int i = 0; i < handlerAt.length; i++) {
            if (handlerAt[i] >= from && handlerAt[i] < to) {
                return true;
            }
        }
        return false;
    }

    /**
     * The array slot whose length is parked in local {@code nVar}, or -1.
     *
     * The proof has to survive both halves changing under it, so all three of these
     * hold: {@code nVar} is written EXACTLY ONCE in the whole method and that write is
     * the ARRAYLENGTH capture; the array's slot is never written at all, so the length
     * it captured is the length it still has; and nothing jumps INTO the region between
     * the capture and the loop, so reaching the header means the capture ran. Refusing
     * every write to the array slot is stricter than it has to be -- a store before the
     * capture is harmless unless a back edge can re-run it -- and the strict form is
     * what a parameter or a field read into a fresh local already satisfies.
     */
    private static int bceHoistedLengthArray(java.util.List<Instruction> r,
                                             java.util.HashMap<Label, Integer> pos,
                                             int nVar, int header, int[] handlerAt) {
        int q = -1;
        for (int i = 0; i < r.size(); i++) {
            Instruction in = r.get(i);
            if (in instanceof IInc && ((IInc) in).getVar() == nVar) {
                if (BCE_CENSUS) { bceHoist[0]++; }
                return -1;
            }
            if (in instanceof VarOp && ((VarOp) in).getIndex() == nVar && bceIsStoreOpcode(in.getOpcode())) {
                if (q >= 0 || in.getOpcode() != Opcodes.ISTORE) {
                    if (BCE_CENSUS) { bceHoist[0]++; }
                    return -1; // written twice, or the slot is reused for another type
                }
                q = i;
            }
        }
        if (q < 2) {
            if (BCE_CENSUS) { bceHoist[1]++; }
            return -1;
        }
        if (q >= header) {
            if (BCE_CENSUS) { bceHoist[2]++; }
            return -1;
        }
        if (r.get(q - 1).getOpcode() != Opcodes.ARRAYLENGTH) {
            if (BCE_CENSUS) { bceHoist[1]++; }
            return -1;
        }
        Instruction a = r.get(q - 2);
        if (!(a instanceof VarOp) || a.getOpcode() != Opcodes.ALOAD) {
            if (BCE_CENSUS) { bceHoist[1]++; }
            return -1;
        }
        int arrVar = ((VarOp) a).getIndex();
        // Writes to the array's slot are allowed BEFORE the capture and refused at or
        // after it. Re-running such a write means jumping back to it, and everything
        // between there and the loop is then re-run too -- the capture included, so n
        // is re-taken for whatever array the slot now holds. What cannot be allowed is
        // a write the capture has already passed, which would leave n describing an
        // array the loop no longer indexes. Refusing every write regardless was the
        // first rule here and it alone accounted for 307 of the 1,134 refusals.
        for (int i = q; i < r.size(); i++) {
            Instruction in = r.get(i);
            if (in instanceof IInc && ((IInc) in).getVar() == arrVar) {
                if (BCE_CENSUS) { bceHoist[3]++; }
                return -1;
            }
            if (in instanceof VarOp && ((VarOp) in).getIndex() == arrVar && bceIsStoreOpcode(in.getOpcode())) {
                if (BCE_CENSUS) { bceHoist[3]++; }
                return -1;
            }
        }
        // Reaching the header must mean the capture ran -- q must DOMINATE the header.
        // Ask that directly: walk the control-flow graph from every entry with q deleted
        // and see whether the header is still reachable.
        //
        // The two approximations tried first were both wrong in instructive ways.
        // Refusing any jump into (q, header] refuses the BACK EDGE, which every loop
        // has, and proved 0 of 1,154 candidates. Narrowing it to (q, header) then proved
        // only the FIRST loop of any method that hoists one length and runs several
        // loops on it, because the earlier loops' own exit labels are jump targets
        // sitting between the capture and the later headers.
        if (!bceDominates(r, pos, q, header, handlerAt)) {
            if (BCE_CENSUS) { bceHoist[4]++; }
            return -1;
        }
        if (BCE_CENSUS) { bceHoist[6]++; }
        return arrVar;
    }

    /**
     * True when every path from an entry to {@code header} runs {@code barrier} -- plain
     * dominance, computed by deleting the barrier and asking whether the header is still
     * reachable. Exception handlers are entries too: a handler is entered from anywhere
     * in its range, so a handler that reaches the header without passing the capture is
     * exactly the hole a scan over jump targets cannot see.
     */
    private static boolean bceDominates(java.util.List<Instruction> r,
                                        java.util.HashMap<Label, Integer> pos,
                                        int barrier, int header, int[] handlerAt) {
        int n = r.size();
        boolean[] seen = new boolean[n];
        java.util.ArrayDeque<Integer> work = new java.util.ArrayDeque<Integer>();
        work.add(Integer.valueOf(0));
        for (int i = 0; i < handlerAt.length; i++) {
            work.add(Integer.valueOf(handlerAt[i]));
        }
        while (!work.isEmpty()) {
            int i = work.poll().intValue();
            if (i < 0 || i >= n || i == barrier || seen[i]) {
                continue;
            }
            seen[i] = true;
            if (i == header) {
                return false;
            }
            Instruction c = r.get(i);
            int op = c.getOpcode();
            if (c instanceof Jump) {
                Integer t = pos.get(((Jump) c).getLabel());
                if (t == null) {
                    return false; // a target we cannot place could be the header
                }
                work.add(t);
                if (op != Opcodes.GOTO) {
                    work.add(Integer.valueOf(i + 1));
                }
                continue;
            }
            if (op == Opcodes.ATHROW || op == Opcodes.RETURN || op == Opcodes.IRETURN
                    || op == Opcodes.LRETURN || op == Opcodes.FRETURN || op == Opcodes.DRETURN
                    || op == Opcodes.ARETURN) {
                continue; // no fallthrough
            }
            work.add(Integer.valueOf(i + 1));
        }
        return true;
    }

    private static boolean bceIsStoreOpcode(int op) {
        return op == Opcodes.ISTORE || op == Opcodes.LSTORE || op == Opcodes.FSTORE
                || op == Opcodes.DSTORE || op == Opcodes.ASTORE;
    }

    /**
     * Index of the first instruction of the VALUE expression of the array store at
     * {@code k}, or -1. Walks the operand stack backwards from the store until the
     * depth below it is exactly the array+index pair, so the caller can check those two
     * by position. Anything the stack model cannot account for -- a label, a branch, a
     * DUP -- ends the walk rather than being guessed at.
     */
    private int bceStoreValueBase(java.util.List<Instruction> r, int k) {
        int op = r.get(k).getOpcode();
        int need = (op == Opcodes.LASTORE || op == Opcodes.DASTORE) ? 2 : 1;
        int d = 2 + need; // depth after r[k-1], i.e. just before the store runs
        for (int m = k - 1; m >= 2; m--) {
            Instruction c = r.get(m);
            if (c instanceof LabelInstruction || c instanceof Jump || c instanceof CustomJump
                    || c instanceof SwitchInstruction || c instanceof TryCatch) {
                return -1;
            }
            int popped;
            int pushed;
            if (c instanceof Invoke) {
                Invoke iv = (Invoke) c;
                if (c.getOpcode() == Opcodes.INVOKEDYNAMIC) {
                    return -1;
                }
                popped = sbDescArgSlots(iv.getDesc()) + (c.getOpcode() == Opcodes.INVOKESTATIC ? 0 : 1);
                pushed = sbDescRetSlots(iv.getDesc());
            } else if (c.getOpcode() == Opcodes.CHECKCAST) {
                popped = 1;
                pushed = 1;
            } else {
                int[] pp = sbPopPush(c);
                if (pp == null) {
                    return -1;
                }
                popped = pp[0];
                pushed = pp[1];
            }
            int before = d - pushed + popped;
            if (before == 2) {
                return m;
            }
            if (before < 2) {
                return -1;
            }
            d = before;
        }
        return -1;
    }

    private static boolean bceIsArrayStoreOpcode(int op) {
        switch (op) {
            case Opcodes.IASTORE: case Opcodes.LASTORE: case Opcodes.FASTORE:
            case Opcodes.DASTORE: case Opcodes.AASTORE: case Opcodes.BASTORE:
            case Opcodes.CASTORE: case Opcodes.SASTORE: return true;
            default: return false;
        }
    }

    /** Every array element access, load or store -- what countArrayOps counts. */
    private static boolean bceIsArrayOpcode(int op) {
        switch (op) {
            case Opcodes.IALOAD: case Opcodes.LALOAD: case Opcodes.FALOAD:
            case Opcodes.DALOAD: case Opcodes.AALOAD: case Opcodes.BALOAD:
            case Opcodes.CALOAD: case Opcodes.SALOAD:
            case Opcodes.IASTORE: case Opcodes.LASTORE: case Opcodes.FASTORE:
            case Opcodes.DASTORE: case Opcodes.AASTORE: case Opcodes.BASTORE:
            case Opcodes.CASTORE: case Opcodes.SASTORE: return true;
            default: return false;
        }
    }

    private static boolean bceIsArrayLoadOpcode(int op) {
        switch (op) {
            case Opcodes.IALOAD: case Opcodes.LALOAD: case Opcodes.FALOAD:
            case Opcodes.DALOAD: case Opcodes.AALOAD: case Opcodes.BALOAD:
            case Opcodes.CALOAD: case Opcodes.SALOAD: return true;
            default: return false;
        }
    }

    // i is written only by IInc(+positive) and ISTORE of a non-negative int constant,
    // with at least one such initializing store. Anything else -> not provably >= 0.
    private static boolean bceInductionMonotonicNonNegative(java.util.List<Instruction> r, int v) {
        boolean hasInit = false;
        for (int i = 0; i < r.size(); i++) {
            Instruction in = r.get(i);
            if (in instanceof IInc && ((IInc) in).getVar() == v) {
                if (((IInc) in).getAmount() <= 0) return false;
            } else if (in instanceof VarOp && in.getOpcode() == Opcodes.ISTORE && ((VarOp) in).getIndex() == v) {
                if (i == 0) return false;
                int srcOp = r.get(i - 1).getOpcode();
                if (srcOp != Opcodes.ICONST_0 && srcOp != Opcodes.ICONST_1 && srcOp != Opcodes.ICONST_2
                        && srcOp != Opcodes.ICONST_3 && srcOp != Opcodes.ICONST_4 && srcOp != Opcodes.ICONST_5) {
                    return false;
                }
                hasInit = true;
            }
        }
        return hasInit;
    }

    private static boolean bceLocalWrittenInRange(java.util.List<Instruction> r, int v, int from, int to) {
        for (int i = from + 1; i < to; i++) {
            Instruction in = r.get(i);
            if (in instanceof IInc && ((IInc) in).getVar() == v) return true;
            if (in instanceof VarOp) {
                int op = in.getOpcode();
                if ((op == Opcodes.ISTORE || op == Opcodes.ASTORE) && ((VarOp) in).getIndex() == v) return true;
            }
        }
        return false;
    }

    private static int bceCountJumpsTargeting(java.util.List<Instruction> r, int headerIndex) {
        Instruction li = r.get(headerIndex);
        if (!(li instanceof LabelInstruction)) return -1;
        Label target = ((LabelInstruction) li).getLabel();
        int c = 0;
        for (Instruction in : r) {
            if (in instanceof Jump && ((Jump) in).getLabel() == target) c++;
        }
        return c;
    }

    private static boolean bceForeignEntry(java.util.List<Instruction> r, java.util.HashMap<Label, Integer> pos,
                                           int from, int to, Jump backEdge) {
        for (Instruction in : r) {
            if (!(in instanceof Jump)) continue;
            Jump jj = (Jump) in;
            Integer tgt = pos.get(jj.getLabel());
            if (tgt == null) continue;
            if (tgt > from && tgt <= to && jj != backEdge) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Scalar replacement of non-escaping primitive-only @StackAllocate objects.
    //
    // Recognizes exactly:   NEW X ; DUP ; <args> ; INVOKESPECIAL X.<init> ; ASTORE n
    // where X is @StackAllocate, extends java.lang.Object directly, every instance
    // field is primitive, X has no static initializer, and X.<init> is exactly
    // ALOAD0;INVOKESPECIAL Object.<init>()V followed only by groups of
    // ALOAD0; <load of one ctor param>; PUTFIELD X.f that, together, assign every
    // field exactly once (a bijection params<->fields). Local n must be used ONLY
    // as "ALOAD n; GETFIELD X.f". Anything else -> bail (leave today's stack-alloc
    // codegen, which is correct and GC-safe, in place).
    //
    // On a match the struct becomes a pure C local __cn1sr_<id> whose address is
    // never taken: NEW/DUP/ASTORE emit nothing, the <init> becomes direct field
    // assignments (ScalarAllocInit -- the arg expressions are folded straight in
    // when they reduce to pure assignables, else popped off the operand stack),
    // and each field read becomes a direct member access. clang then SROA-promotes
    // the whole struct to registers.
    // ------------------------------------------------------------------
    private static final boolean DISABLE_SCALAR_REPLACE =
            "true".equalsIgnoreCase(Util.getProperty("CN1_DISABLE_SCALAR_REPLACE", "false"));

    private static String srMangle(String s) {
        return s.replace('.', '_').replace('/', '_').replace('$', '_');
    }

    /**
     * Like {@link #srNextReal(int)} but a control-flow join ends the lookahead.
     *
     * Every scalar-replacement match is a claim about two ADJACENT instructions
     * ("this ALOAD feeds that GETFIELD", "this NEW feeds that DUP"). srNextReal
     * skips every LabelInstruction, including the ones that are jump targets --
     * so it happily pairs instructions that are not on a common path. javac
     * emits exactly that for {@code (c ? p : q).x}:
     *
     *     ALOAD p ; GOTO L ; [L1:] ALOAD q ; [L:] GETFIELD x
     *
     * Folding "ALOAD q ; GETFIELD x" into a read of q's scalar struct rewrites
     * the GETFIELD that the {@code p} path also branches into, so that path
     * silently reads q's field (and leaks its own receiver on the operand
     * stack). Stopping at the join leaves the whole site alone.
     */
    private int srNextRealNoJoin(int from) {
        for (int i = from; i < instructions.size(); i++) {
            Instruction in = instructions.get(i);
            if (in instanceof LabelInstruction) {
                if (LabelInstruction.isJumpTarget(((LabelInstruction) in).getLabel())) {
                    return -1;
                }
                continue;
            }
            if (in instanceof LineNumber || in instanceof LocalVariable) {
                continue;
            }
            return i;
        }
        return -1;
    }

    /** Next index of a non-trivia instruction at or after {@code from}, or -1. */
    private int srNextReal(int from) {
        for (int i = from; i < instructions.size(); i++) {
            Instruction in = instructions.get(i);
            if (in instanceof LineNumber || in instanceof LabelInstruction || in instanceof LocalVariable) {
                continue;
            }
            return i;
        }
        return -1;
    }

    // Mark each LineNumber whose source line contains no throwing or calling
    // instruction as elidable. The reported line of any stack frame is only ever
    // read at a call/throw/alloc site (that is the only place a trace is captured
    // or an exception originates), and every such site lives on a non-elidable
    // line -- so a line with no throwing/calling instruction can never be the line
    // a trace reports, and eliding its per-line store is trace-IDENTICAL. The
    // store is then emitted as __CN1_DEBUG_INFO_NT (no-op in release / full under
    // the on-device debugger). Conservative: default keep (non-elidable); only the
    // explicit non-throwing whitelist in canThrowOrCall() is treated as safe.
    private void analyzeElidableLineInfo() {
        LineNumber current = null;
        boolean lineHasThrowingInstruction = false;
        for (Instruction in : instructions) {
            if (in instanceof LineNumber) {
                if (current != null) {
                    current.setElidable(!lineHasThrowingInstruction);
                }
                current = (LineNumber) in;
                lineHasThrowingInstruction = false;
            } else if (current != null && canThrowOrCall(in)) {
                lineHasThrowingInstruction = true;
            }
        }
        if (current != null) {
            current.setElidable(!lineHasThrowingInstruction);
        }
    }

    // True if the instruction may throw a Java exception or transfer control to
    // another method (so the current line must be live for an accurate trace).
    // Conservative: anything not in the explicit non-throwing/non-calling whitelist
    // returns true. Excludes integer div/rem (ArithmeticException), array access
    // (NPE/AIOOBE/ArrayStore), field/static access (NPE / class-init), allocation
    // (OOM/NegativeArraySize), invoke/athrow/checkcast/monitor, and single-word LDC
    // (a class/method-type constant can trigger throwing resolution).
    private boolean canThrowOrCall(Instruction in) {
        int op = in.getOpcode();
        if (op < 0) {
            return false; // LineNumber/Label/LocalVariable/TryCatch/ScalarAllocInit/empty markers
        }
        if (in instanceof TypeInstruction && ((TypeInstruction) in).isScalarReplaced()) {
            return false; // a scalar-replaced NEW emits nothing -- no allocation, cannot throw
        }
        if (in instanceof com.codename1.tools.translator.bytecodes.Ldc) {
            // numeric and String LDC constants never resolve/throw; a class (Type),
            // method handle, or constant-dynamic LDC can -> keep those (conservative).
            Object v = ((com.codename1.tools.translator.bytecodes.Ldc) in).getValue();
            return !(v instanceof Integer || v instanceof Long || v instanceof Float
                    || v instanceof Double || v instanceof String);
        }
        switch (op) {
            case Opcodes.NOP: case Opcodes.ACONST_NULL:
            case Opcodes.ICONST_M1: case Opcodes.ICONST_0: case Opcodes.ICONST_1:
            case Opcodes.ICONST_2: case Opcodes.ICONST_3: case Opcodes.ICONST_4: case Opcodes.ICONST_5:
            case Opcodes.LCONST_0: case Opcodes.LCONST_1:
            case Opcodes.FCONST_0: case Opcodes.FCONST_1: case Opcodes.FCONST_2:
            case Opcodes.DCONST_0: case Opcodes.DCONST_1:
            case Opcodes.BIPUSH: case Opcodes.SIPUSH: // (LDC of any width falls to default/keep: a class/method-type constant can throw on resolution)
            case Opcodes.ILOAD: case Opcodes.LLOAD: case Opcodes.FLOAD: case Opcodes.DLOAD: case Opcodes.ALOAD:
            case Opcodes.ISTORE: case Opcodes.LSTORE: case Opcodes.FSTORE: case Opcodes.DSTORE: case Opcodes.ASTORE:
            case Opcodes.POP: case Opcodes.POP2:
            case Opcodes.DUP: case Opcodes.DUP_X1: case Opcodes.DUP_X2:
            case Opcodes.DUP2: case Opcodes.DUP2_X1: case Opcodes.DUP2_X2: case Opcodes.SWAP:
            case Opcodes.IADD: case Opcodes.LADD: case Opcodes.FADD: case Opcodes.DADD:
            case Opcodes.ISUB: case Opcodes.LSUB: case Opcodes.FSUB: case Opcodes.DSUB:
            case Opcodes.IMUL: case Opcodes.LMUL: case Opcodes.FMUL: case Opcodes.DMUL:
            case Opcodes.FDIV: case Opcodes.DDIV: case Opcodes.FREM: case Opcodes.DREM: // float div/rem: no throw
            case Opcodes.INEG: case Opcodes.LNEG: case Opcodes.FNEG: case Opcodes.DNEG:
            case Opcodes.ISHL: case Opcodes.ISHR: case Opcodes.IUSHR:
            case Opcodes.LSHL: case Opcodes.LSHR: case Opcodes.LUSHR:
            case Opcodes.IAND: case Opcodes.IOR: case Opcodes.IXOR:
            case Opcodes.LAND: case Opcodes.LOR: case Opcodes.LXOR:
            case Opcodes.IINC:
            case Opcodes.I2L: case Opcodes.I2F: case Opcodes.I2D:
            case Opcodes.L2I: case Opcodes.L2F: case Opcodes.L2D:
            case Opcodes.F2I: case Opcodes.F2L: case Opcodes.F2D:
            case Opcodes.D2I: case Opcodes.D2L: case Opcodes.D2F:
            case Opcodes.I2B: case Opcodes.I2C: case Opcodes.I2S:
            case Opcodes.LCMP: case Opcodes.FCMPL: case Opcodes.FCMPG: case Opcodes.DCMPL: case Opcodes.DCMPG:
            case Opcodes.IFEQ: case Opcodes.IFNE: case Opcodes.IFLT: case Opcodes.IFGE:
            case Opcodes.IFGT: case Opcodes.IFLE:
            case Opcodes.IF_ICMPEQ: case Opcodes.IF_ICMPNE: case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE: case Opcodes.IF_ICMPGT: case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPEQ: case Opcodes.IF_ACMPNE:
            case Opcodes.IFNULL: case Opcodes.IFNONNULL:
            case Opcodes.GOTO: case Opcodes.TABLESWITCH: case Opcodes.LOOKUPSWITCH:
            case Opcodes.IRETURN: case Opcodes.LRETURN: case Opcodes.FRETURN:
            case Opcodes.DRETURN: case Opcodes.ARETURN: case Opcodes.RETURN:
            case Opcodes.INSTANCEOF:
                return false;
            default:
                return true; // invoke*, *ALOAD/*ASTORE, ARRAYLENGTH, GET/PUTFIELD,
                             // GET/PUTSTATIC, IDIV/IREM/LDIV/LREM, NEW*, ATHROW,
                             // CHECKCAST, MONITORENTER/EXIT, LDC(class), JSR/RET, ...
        }
    }

    private void scalarReplaceStackAllocations() {
        if (DISABLE_SCALAR_REPLACE) {
            return;
        }
        int srId = 0;
        // (localN, structId) of every accepted site; field reads are rewritten in
        // a deferred pass (it removes the ALOAD, which shifts indices) so the
        // matching loop below -- which only uses index-stable set() edits -- stays
        // valid throughout.
        List<int[]> acceptedReads = new ArrayList<int[]>();
        List<ByteCodeClass> acceptedClasses = new ArrayList<ByteCodeClass>();
        for (int idx = 0; idx < instructions.size(); idx++) {
            Instruction ins = instructions.get(idx);
            if (!(ins instanceof TypeInstruction)) {
                continue;
            }
            TypeInstruction ti = (TypeInstruction) ins;
            if (ti.getOpcode() != Opcodes.NEW) {
                continue;
            }
            // Per-USAGE escape analysis (no per-class annotation): scalar-replace any NEW of a
            // primitive-only direct-Object value class whose instance provably does not escape at
            // this site (the srValidateLocalUses check below). Escape/identity are properties of the
            // usage, not the class, so eligibility is the class SHAPE + the local's uses -- matching
            // what an escape-analysing JIT (e.g. HotSpot) does automatically. (The old @StackAllocate
            // opt-in gate is gone.)
            String saType = srMangle(ti.getTypeName());
            ByteCodeClass x = Parser.getClassObject(saType);
            if (x == null || !srPrimitiveOnlyDirectObject(x)) {
                continue; // not a scalar-replaceable value-class shape
            }

            // DUP must immediately follow the NEW.
            int dupIdx = srNextRealNoJoin(idx + 1);
            if (dupIdx < 0 || instructions.get(dupIdx).getOpcode() != Opcodes.DUP) {
                continue;
            }

            // Find the matching INVOKESPECIAL X.<init>. Bail if the arg region
            // contains a nested NEW, another <init>, any stack-shuffle (DUP*/SWAP)
            // or any control flow -- those break the simple receiver-at-bottom shape.
            int invIdx = -1;
            boolean bail = false;
            for (int j = srNextRealNoJoin(dupIdx + 1); j >= 0; j = srNextRealNoJoin(j + 1)) {
                Instruction c = instructions.get(j);
                int op = c.getOpcode();
                if (c instanceof Invoke && op == Opcodes.INVOKESPECIAL
                        && "<init>".equals(((Invoke) c).getName())) {
                    if (srMangle(((Invoke) c).getOwner()).equals(saType)) {
                        invIdx = j;
                    } else {
                        bail = true; // a different constructor call sits in the args
                    }
                    break;
                }
                if (c instanceof TypeInstruction && op == Opcodes.NEW) { bail = true; break; }
                if (c instanceof Jump || c instanceof CustomJump || c instanceof SwitchInstruction
                        || c instanceof TryCatch) { bail = true; break; }
                switch (op) {
                    case Opcodes.DUP: case Opcodes.DUP_X1: case Opcodes.DUP_X2:
                    case Opcodes.DUP2: case Opcodes.DUP2_X1: case Opcodes.DUP2_X2:
                    case Opcodes.SWAP: case Opcodes.GOTO:
                        bail = true;
                        break;
                    default:
                        break;
                }
                if (bail) { break; }
            }
            if (bail || invIdx < 0) {
                continue;
            }
            Invoke initInv = (Invoke) instructions.get(invIdx);

            // ASTORE n must immediately follow the constructor call.
            int storeIdx = srNextRealNoJoin(invIdx + 1);
            if (storeIdx < 0) {
                continue;
            }
            Instruction st = instructions.get(storeIdx);
            if (!(st instanceof VarOp) || st.getOpcode() != Opcodes.ASTORE) {
                continue;
            }
            int localN = ((VarOp) st).getIndex();

            // Analyze the constructor into an ordered param->field map.
            String[] members = new String[0];
            char[] quals = new char[0];
            String[][] mapOut = new String[1][];
            char[][] qualOut = new char[1][];
            if (!srAnalyzeCtor(x, initInv, mapOut, qualOut)) {
                continue;
            }
            members = mapOut[0];
            quals = qualOut[0];

            // Validate every use of local n is exactly "ALOAD n; GETFIELD X.f" (or a trivial getter)
            // and there is no second store / no read before the construction.
            if (!srValidateLocalUses(localN, storeIdx, x)) {
                continue;
            }

            // -------- accept: rewrite in place (set() keeps indices stable) --------
            int id = srId++;
            ti.markScalarReplaced(id);
            instructions.set(dupIdx, srEmpty());
            instructions.set(invIdx, new ScalarAllocInit(id, members, quals));
            instructions.set(storeIdx, srEmpty());
            acceptedReads.add(new int[]{localN, id});
            acceptedClasses.add(x);
        }
        for (int s = 0; s < acceptedReads.size(); s++) {
            srRewriteFieldReads(acceptedReads.get(s)[0], acceptedReads.get(s)[1], acceptedClasses.get(s));
        }
    }

    // ------------------------------------------------------------------
    // IMPLICIT STACK ALLOCATION of non-escaping java.lang.StringBuilder.
    //
    // javac lowers every string concatenation to
    //   NEW StringBuilder ; DUP ; [args] ; INVOKESPECIAL <init> ;
    //   (INVOKEVIRTUAL append)* ; INVOKEVIRTUAL toString
    // optionally parking the builder in a local between appends. HotSpot's
    // escape analysis scalar-replaces the whole chain; this is the AOT
    // equivalent: prove the builder reference is only ever the RECEIVER of
    // java.lang.StringBuilder virtual calls (append returns `this`, so the
    // returned alias is tracked too -- it may be popped, chained into the
    // next receiver, or re-stored into the SAME local) and lower the NEW to
    // the method-scoped stack struct the @StackAllocate machinery already
    // provides. A bounded native stack buffer serves small builders; overflow
    // uses a native block reclaimed on normal return and Java exception unwind.
    //
    // Bails (keeps the heap path) on: methods with try/catch or
    // synchronization, a tracked ref crossing a label/branch/switch while
    // live on the operand stack, stack shuffles at/near the ref, the ref
    // used as a call ARGUMENT, stored to a field/array/other local,
    // returned, thrown, or any instruction whose stack effect isn't in the
    // conservative table below. StringBuilder is final, so receiver calls
    // can't dispatch to an escaping override.
    // ------------------------------------------------------------------
    private static final boolean DISABLE_SB_STACK_ALLOC =
            "true".equalsIgnoreCase(Util.getProperty("CN1_DISABLE_SB_STACK_ALLOC", "false"));
    private static final String SB_OWNER = "java/lang/StringBuilder";
    private Map<Integer, Boolean> borrowedBuilderParameters;
    private boolean builderOwnershipFrozen;

    public void freezeBuilderOwnership() { builderOwnershipFrozen = true; }

    // Compute before instruction rewriting. A false entry also breaks recursive
    // proof cycles conservatively; no caller relies on an unfinished proof.
    private static final boolean DISABLE_FRAME_RETIRE =
            "true".equalsIgnoreCase(Util.getProperty("CN1_DISABLE_FRAME_RETIRE", "false"));

    /// Locals holding an object this frame allocated and provably never let escape.
    /// Retired at frame exit; see cn1MarkDeadNow in cn1_globals.m.
    private java.util.List<Integer> retirableLocals;
    private java.util.List<Integer> retirableGuards;

    /// Where sites are lost, so a zero at the end of the pipe is explainable rather
    /// than mysterious. Printed by Parser when -Dcn1.allocCensus is on.
    static int retireSeen, retireDropStack, retireDropEscape, retireDropNoLocal,
            retireDropCallee, retireDropFrameless, retireKept;

    /// FRAME EXIT IS WHERE DEADNESS IS KNOWN.
    ///
    /// A non-escaping object dies with the frame that made it, by definition -- no
    /// last-use liveness analysis, no worrying about a local being reassigned in a loop.
    /// The question collapses to "did this reference leave the frame", which the escape
    /// analysis already answers, and the answer is consumed at exactly one place.
    ///
    /// Three conditions, all necessary:
    ///   1. newEscapesStrict == SAFE -- the reference never leaves this frame, ARETURN
    ///      included (the iterator scheme allows ARETURN because it allocates in the
    ///      CALLER's frame; retiring at THIS frame's exit does not have that property).
    ///   2. Every callee invoked ON the object keeps `this`. The walk permits the tracked
    ///      value as a receiver only because "that callee is checked on its own", and for
    ///      an arbitrary type nothing else performs that check.
    ///   3. It reached a local, and the site is not already scalar-replaced or
    ///      stack-allocated -- those have no heap object to retire.
    /// Candidate sites found on RAW bytecode, held until the allocation decisions exist.
    private java.util.Map<TypeInstruction, Integer> retireCandidates;

    /// PHASE 1, on RAW bytecode. The escape walk models real opcodes; optimize() fuses
    /// them into opaque custom instructions, after which the walk cannot follow a value
    /// and conservatively answers ESCAPES for everything. Measured: running this after
    /// optimize() dropped 209 of 209 non-stack sites, against 14 kept before it. The
    /// same ordering constraint is why `frameless` is decided on raw bytecode too.
    public void collectRetireCandidates() {
        if (DISABLE_FRAME_RETIRE || isNative() || abstractMethod) {
            return;
        }
        for (Instruction ins : instructions) {
            if (!(ins instanceof TypeInstruction) || ins.getOpcode() != Opcodes.NEW) {
                continue;
            }
            TypeInstruction ti = (TypeInstruction) ins;
            retireSeen++;
            String owner = IteratorEscape.mangle(ti.getTypeName());
            // CLUSTER walk, not the per-object one. A self-referential structure built
            // and dropped inside one frame -- the `head = new Node(v, head)` shape -- has
            // no frame-local MEMBER, because each member is stored into the next one's
            // field, yet the chain as a whole never leaves the frame. Per-object escape
            // analysis must reject every member of it; HotSpot does not scalar-replace
            // these either (-XX:-EliminateAllocations costs it only 5% on this shape).
            // -Dcn1.retireDebug=<methodName> reports the verdict per NEW site.
            String __dbg = Util.getProperty("cn1.retireDebug", "");
            boolean __on = __dbg.length() > 0 && methodName.contains(__dbg);
            int __r = IteratorEscape.clusterEscapes(this, owner);
            if (__on) {
                System.out.println("[RETIRE-DBG] " + clsName + "." + methodName
                        + " NEW " + owner + " -> " + __r + " local=" + IteratorEscape.lastTrackedLocal
                        + " reason=" + IteratorEscape.lastReason
                        + " calls=" + IteratorEscape.receiverCalls);
            }
            if (__r != IteratorEscape.SAFE) {
                retireDropEscape++;
                continue;
            }
            int local = IteratorEscape.lastTrackedLocal;
            if (local < 0) {
                retireDropNoLocal++;
                continue;
            }
            java.util.List<String> calls =
                    new java.util.ArrayList<String>(IteratorEscape.receiverCalls);
            if (!Parser.calleesKeepThis(calls, new java.util.HashMap<String, Boolean>())) {
                retireDropCallee++;
                continue;
            }
            if (retireCandidates == null) {
                // LinkedHashMap, and the generated C is WRONG-BY-DIVERGENCE without it.
                // Phase 2 numbers the guards by walking this map, so iteration order
                // decides which site gets __cn1dead_1 and which gets __cn1dead_2. A
                // HashMap keyed by TypeInstruction -- which overrides neither hashCode
                // nor equals -- iterates in IDENTITY HASH order, i.e. by allocation
                // address, so two runs of the SAME translator on the SAME input emit
                // different C. Measured: three files (XMLParser, Resources, CSSEngine)
                // differed between two runs of the self-hosted translator, each by the
                // same 8 lines with _1 and _2 exchanged. Semantically harmless -- the
                // guards are independent -- but it breaks the byte-identical output
                // invariant the whole selfhost benchmark is gated on, which is why the
                // SELFHOST row read NA. Insertion order here is bytecode order (the
                // loop above walks `instructions`), so this is also the order a reader
                // of the generated C expects.
                retireCandidates = new java.util.LinkedHashMap<TypeInstruction, Integer>();
            }
            retireCandidates.put(ti, Integer.valueOf(local));
        }
    }

    /// PHASE 2, after optimize(). Only now is it known whether a site still has a heap
    /// object -- scalar replacement and stack allocation are decided inside optimize --
    /// and whether this method has a frame to exit at all.
    public void analyzeRetirableLocals() {
        if (DISABLE_FRAME_RETIRE || retireCandidates == null) {
            return;
        }
        // NOTE: frameless is NOT a disqualifier. It only decides whether the method
        // calls releaseForReturn, and the retire scope does not ride on that -- its
        // cleanup attribute fires on scope exit either way. What DOES matter is how
        // the method names its object locals, which is isBarebone(), handled in
        // frameRetireScopeDecl. Excluding frameless here cost 3 of every 4 surviving
        // candidates for no safety reason at all.
        int guard = 0;
        for (java.util.Map.Entry<TypeInstruction, Integer> e : retireCandidates.entrySet()) {
            TypeInstruction ti = e.getKey();
            if (ti.isScalarReplaced() || ti.getStackAllocType() != null) {
                retireDropStack++;
                continue;   // no heap object exists at this site
            }
            if (guard >= 8) {
                break;      // slots[8] in CN1RetireScope
            }
            // The SITE writes its own guard, so the scope can only ever retire the
            // object this analysis actually reasoned about.
            ti.setDeadGuardId(guard);
            // The NEW may have been fused during optimize(): init-before-publish leaves
            // the TypeInstruction emitting only a NULL placeholder while an
            // InlinableConstructor performs the real allocation further down. The guard
            // has to reach whichever instruction actually produces the object.
            int tiIdx = instructions.indexOf(ti);
            if (tiIdx >= 0) {
                for (int k = tiIdx + 1; k < instructions.size() && k < tiIdx + 12; k++) {
                    Instruction fi = instructions.get(k);
                    if (fi instanceof com.codename1.tools.translator.bytecodes.CustomInvoke) {
                        ((com.codename1.tools.translator.bytecodes.CustomInvoke) fi)
                                .setDeadGuardId(guard);
                        break;
                    }
                    if (fi instanceof TypeInstruction) {
                        break;   // a different allocation starts here
                    }
                }
            }
            if (retirableGuards == null) {
                retirableGuards = new java.util.ArrayList<Integer>();
            }
            retirableGuards.add(Integer.valueOf(guard));
            guard++;
            retireKept++;
        }
    }

    private void unusedOldRetirePass() {
        for (Instruction ins : instructions) {
            if (!(ins instanceof TypeInstruction) || ins.getOpcode() != Opcodes.NEW) {
                continue;
            }
            TypeInstruction ti = (TypeInstruction) ins;
            retireSeen++;
            if (ti.isScalarReplaced() || ti.getStackAllocType() != null) {
                retireDropStack++;
                continue;   // no heap object exists at this site
            }
            String owner = IteratorEscape.mangle(ti.getTypeName());
            if (IteratorEscape.newEscapesStrict(this, owner) != IteratorEscape.SAFE) {
                retireDropEscape++;
                continue;
            }
            int local = IteratorEscape.lastTrackedLocal;
            if (local < 0) {
                retireDropNoLocal++;
                continue;
            }
            java.util.List<String> calls =
                    new java.util.ArrayList<String>(IteratorEscape.receiverCalls);
            if (!Parser.calleesKeepThis(calls, new java.util.HashMap<String, Boolean>())) {
                retireDropCallee++;
                continue;
            }
            if (frameless) {
                retireDropFrameless++;
                continue;
            }
            if (retirableLocals == null) {
                retirableLocals = new java.util.ArrayList<Integer>();
            }
            Integer key = Integer.valueOf(local);
            if (!retirableLocals.contains(key)) {
                retirableLocals.add(key);
                retireKept++;
            }
        }
    }

    /// The C emitted immediately before a frame is released. Empty when nothing
    /// qualifies, which is the overwhelming majority of methods.
    /// The scope declaration placed once at the top of the generated function. Its
    /// cleanup attribute fires on every ordinary return, so no return-emission site
    /// needs to know this optimization exists -- there are a dozen of them across
    /// BytecodeMethod and BasicInstruction, one per return type plus the exception
    /// variants, and patching each is how one gets missed.
    /// Declares one guard per retirable site plus the scope that retires them.
    ///
    /// Guards rather than locals: a guard is written by the NEW itself and by nothing
    /// else, so it cannot be holding some other object at frame exit. It also makes the
    /// emission independent of how the method names its locals (locals[] vs olocals_N_),
    /// which is decided by isBarebone() and had already cost this pass most of its
    /// population once.
    String frameRetireScopeDecl() {
        if (retirableGuards == null || retirableGuards.isEmpty()) {
            return "";
        }
        StringBuilder r = new StringBuilder();
        StringBuilder slots = new StringBuilder();
        int n = 0;
        for (Integer g : retirableGuards) {
            r.append("    JAVA_OBJECT __cn1dead_").append(g).append(" = JAVA_NULL;\n");
            if (n > 0) { slots.append(", "); }
            slots.append("&__cn1dead_").append(g);
            n++;
        }
        r.append("    struct CN1RetireScope __cn1retire __attribute__((cleanup(cn1RetireScopeLeave))) = {{")
         .append(slots).append("}, ").append(n).append("};\n");
        return r.toString();
    }

    public void analyzeBuilderOwnership() {
        if (!desc.contains("Ljava/lang/StringBuilder;")) return;
        int slot = staticMethod ? 0 : 1;
        for (org.objectweb.asm.Type type : org.objectweb.asm.Type.getArgumentTypes(desc)) {
            if ("Ljava/lang/StringBuilder;".equals(type.getDescriptor())) builderParameterIsBorrowed(slot);
            slot += type.getSize();
        }
    }

    private boolean builderParameterIsBorrowed(int slot) {
        if (isNative() || abstractMethod || synchronizedMethod) return false;
        if (builderOwnershipFrozen) {
            return borrowedBuilderParameters != null && Boolean.TRUE.equals(borrowedBuilderParameters.get(slot));
        }
        if (borrowedBuilderParameters == null) borrowedBuilderParameters = new HashMap<Integer, Boolean>();
        Boolean cached = borrowedBuilderParameters.get(slot);
        if (cached != null) return cached;
        borrowedBuilderParameters.put(slot, false);
        Map<org.objectweb.asm.Label, Integer> labels = new HashMap<org.objectweb.asm.Label, Integer>();
        Set<Integer> aliases = new HashSet<Integer>();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction in = instructions.get(i);
            if (in instanceof TryCatch) return false;
            if (in instanceof LabelInstruction) labels.put(((LabelInstruction)in).getLabel(), i);
        }
        for (int i = 0; i < instructions.size(); i++) {
            Instruction in = instructions.get(i);
            if (in instanceof VarOp && ((VarOp)in).getIndex() == slot) {
                if (in.getOpcode() == Opcodes.ALOAD) {
                    if (sbWalkUse(i + 1, 0, slot, labels, aliases) == SB_WALK_BAIL) return false;
                } else if (in.getOpcode() != Opcodes.ASTORE) return false;
            }
        }
        for (int i = 0; i < instructions.size(); i++) {
            Instruction in = instructions.get(i);
            if (in instanceof VarOp && ((VarOp)in).getIndex() == slot
                    && in.getOpcode() == Opcodes.ASTORE && !aliases.contains(i)) return false;
        }
        borrowedBuilderParameters.put(slot, true);
        return true;
    }

    private static boolean builderArgumentIsBorrowed(Invoke call, int above) {
        int remaining = sbDescArgSlots(call.getDesc());
        if (above >= remaining) return false;
        BytecodeMethod target = call.getOwnershipTarget();
        if (target == null) return false;
        int slot = call.getOpcode() == Opcodes.INVOKESTATIC ? 0 : 1;
        for (org.objectweb.asm.Type type : org.objectweb.asm.Type.getArgumentTypes(call.getDesc())) {
            remaining -= type.getSize();
            if (remaining == above) {
                return (type.getSort() == org.objectweb.asm.Type.OBJECT)
                        && target.builderParameterIsBorrowed(slot);
            }
            slot += type.getSize();
        }
        return false;
    }

    /** Slots consumed by the argument list of a method descriptor (no receiver). */
    private static int sbDescArgSlots(String desc) {
        int slots = 0;
        int i = 1; // skip '('
        while (desc.charAt(i) != ')') {
            char c = desc.charAt(i);
            if (c == 'J' || c == 'D') {
                slots += 2; i++;
            } else if (c == 'L') {
                slots += 1; i = desc.indexOf(';', i) + 1;
            } else if (c == '[') {
                do { i++; } while (desc.charAt(i) == '[');
                i = desc.charAt(i) == 'L' ? desc.indexOf(';', i) + 1 : i + 1;
                slots++;
            } else {
                slots += 1; i++;
            }
        }
        return slots;
    }

    /** Slots pushed by a method descriptor's return type. */
    private static int sbDescRetSlots(String desc) {
        char c = desc.charAt(desc.indexOf(')') + 1);
        if (c == 'V') return 0;
        if (c == 'J' || c == 'D') return 2;
        return 1;
    }

    /** Slot width of a field descriptor. */
    private static int sbFieldSlots(String desc) {
        char c = desc.charAt(0);
        return (c == 'J' || c == 'D') ? 2 : 1;
    }

    /**
     * Conservative stack effect {pops, pushes} in SLOTS for instructions that can
     * appear while a tracked StringBuilder ref is live on the operand stack.
     * Returns null for anything unmodeled (caller bails). Invokes and the
     * allowed consumers of the ref itself are handled by the walker, not here.
     */
    private static int[] sbPopPush(Instruction in) {
        int op = in.getOpcode();
        if (in instanceof com.codename1.tools.translator.bytecodes.Ldc) {
            Object v = ((com.codename1.tools.translator.bytecodes.Ldc) in).getValue();
            if (v instanceof Long || v instanceof Double) return new int[]{0, 2};
            if (v instanceof Integer || v instanceof Float || v instanceof String) return new int[]{0, 1};
            return null; // class constants can throw on resolution; unusual -- bail
        }
        if (in instanceof Field) {
            int w = sbFieldSlots(((Field) in).getDesc());
            switch (op) {
                case Opcodes.GETSTATIC: return new int[]{0, w};
                case Opcodes.PUTSTATIC: return new int[]{w, 0};
                case Opcodes.GETFIELD:  return new int[]{1, w};
                case Opcodes.PUTFIELD:  return new int[]{1 + w, 0};
            }
            return null;
        }
        if (in instanceof IInc) {
            return new int[]{0, 0};
        }
        switch (op) {
            case Opcodes.ACONST_NULL:
            case Opcodes.ICONST_M1: case Opcodes.ICONST_0: case Opcodes.ICONST_1:
            case Opcodes.ICONST_2: case Opcodes.ICONST_3: case Opcodes.ICONST_4: case Opcodes.ICONST_5:
            case Opcodes.FCONST_0: case Opcodes.FCONST_1: case Opcodes.FCONST_2:
            case Opcodes.BIPUSH: case Opcodes.SIPUSH:
            case Opcodes.ILOAD: case Opcodes.FLOAD: case Opcodes.ALOAD:
                return new int[]{0, 1};
            case Opcodes.LCONST_0: case Opcodes.LCONST_1:
            case Opcodes.DCONST_0: case Opcodes.DCONST_1:
            case Opcodes.LLOAD: case Opcodes.DLOAD:
                return new int[]{0, 2};
            case Opcodes.ISTORE: case Opcodes.FSTORE:
                return new int[]{1, 0};
            case Opcodes.LSTORE: case Opcodes.DSTORE:
                return new int[]{2, 0};
            case Opcodes.IALOAD: case Opcodes.FALOAD: case Opcodes.AALOAD:
            case Opcodes.BALOAD: case Opcodes.CALOAD: case Opcodes.SALOAD:
                return new int[]{2, 1};
            case Opcodes.LALOAD: case Opcodes.DALOAD:
                return new int[]{2, 2};
            case Opcodes.IASTORE: case Opcodes.FASTORE: case Opcodes.BASTORE:
            case Opcodes.CASTORE: case Opcodes.SASTORE:
                return new int[]{3, 0};
            case Opcodes.AASTORE:
                // an AASTORE deep in the args can't store OUR ref (that would need
                // the ref above it, and consuming past the ref bails elsewhere)
                return new int[]{3, 0};
            case Opcodes.LASTORE: case Opcodes.DASTORE:
                return new int[]{4, 0};
            case Opcodes.IADD: case Opcodes.ISUB: case Opcodes.IMUL: case Opcodes.IDIV:
            case Opcodes.IREM: case Opcodes.ISHL: case Opcodes.ISHR: case Opcodes.IUSHR:
            case Opcodes.IAND: case Opcodes.IOR: case Opcodes.IXOR:
            case Opcodes.FADD: case Opcodes.FSUB: case Opcodes.FMUL: case Opcodes.FDIV: case Opcodes.FREM:
                return new int[]{2, 1};
            case Opcodes.LADD: case Opcodes.LSUB: case Opcodes.LMUL: case Opcodes.LDIV:
            case Opcodes.LREM: case Opcodes.LAND: case Opcodes.LOR: case Opcodes.LXOR:
                return new int[]{4, 2};
            case Opcodes.LSHL: case Opcodes.LSHR: case Opcodes.LUSHR:
                return new int[]{3, 2};
            case Opcodes.DADD: case Opcodes.DSUB: case Opcodes.DMUL: case Opcodes.DDIV: case Opcodes.DREM:
                return new int[]{4, 2};
            case Opcodes.INEG: case Opcodes.FNEG:
                return new int[]{1, 1};
            case Opcodes.LNEG: case Opcodes.DNEG:
                return new int[]{2, 2};
            case Opcodes.I2F: case Opcodes.F2I: case Opcodes.I2B: case Opcodes.I2C: case Opcodes.I2S:
                return new int[]{1, 1};
            case Opcodes.I2L: case Opcodes.I2D: case Opcodes.F2L: case Opcodes.F2D:
                return new int[]{1, 2};
            case Opcodes.L2I: case Opcodes.L2F: case Opcodes.D2I: case Opcodes.D2F:
                return new int[]{2, 1};
            case Opcodes.L2D: case Opcodes.D2L:
                return new int[]{2, 2};
            case Opcodes.LCMP: case Opcodes.DCMPL: case Opcodes.DCMPG:
                return new int[]{4, 1};
            case Opcodes.FCMPL: case Opcodes.FCMPG:
                return new int[]{2, 1};
            case Opcodes.ARRAYLENGTH: case Opcodes.INSTANCEOF:
                return new int[]{1, 1};
            case Opcodes.ANEWARRAY: case Opcodes.NEWARRAY:
                return new int[]{1, 1};
            case Opcodes.NEW:
                return new int[]{0, 1};
            case Opcodes.NOP:
                return new int[]{0, 0};
            default:
                return null; // DUP*/SWAP/POP*/ASTORE/CHECKCAST/branches/returns/
                             // ATHROW/MONITOR*/MULTIANEWARRAY/JSR/RET/invokes:
                             // walker-handled or bail
        }
    }

    /**
     * CFG walk from {@code startIdx} with a tracked StringBuilder ref on the
     * operand stack ({@code above} slots above it) until every path consumes the
     * ref in a provably non-escaping way. Forward branches (the ternary-in-
     * argument shape javac emits inside append chains) are followed with a
     * per-index depth state; a join whose incoming depths disagree, a backward
     * edge, or anything unmodeled bails. {@code trackedLocal} is the single
     * local slot allowed to (re)receive the ref alias, or -1 when none is
     * established yet.
     *
     * @param aliasStoresOut indices of ASTORE instructions that received the
     *        alias (accumulated across paths)
     * @return the local that received the alias (or the incoming trackedLocal /
     *         -1), or {@code SB_WALK_BAIL} to reject the site
     */
    private static final int SB_WALK_BAIL = Integer.MIN_VALUE;

    private int sbWalkUse(int startIdx, int above, int trackedLocal,
            java.util.Map<org.objectweb.asm.Label, Integer> labelIndex,
            java.util.Set<Integer> aliasStoresOut) {
        java.util.Map<Integer, Integer> stateAt = new java.util.HashMap<Integer, Integer>();
        java.util.ArrayDeque<int[]> work = new java.util.ArrayDeque<int[]>();
        work.add(new int[]{startIdx, above});
        int storedLocal = trackedLocal;
        while (!work.isEmpty()) {
            int[] st = work.poll();
            int i = st[0];
            int cur = st[1];
            for (; i < instructions.size(); i++) {
                Instruction c = instructions.get(i);
                if (c instanceof LineNumber || c instanceof LocalVariable) {
                    continue;
                }
                if (c instanceof LabelInstruction) {
                    Integer seen = stateAt.get(i);
                    if (seen != null) {
                        if (seen.intValue() != cur) {
                            return SB_WALK_BAIL; // join with disagreeing depth
                        }
                        break; // already walked from here with this state
                    }
                    stateAt.put(i, cur);
                    continue;
                }
                int op = c.getOpcode();
                if (c instanceof TryCatch || c instanceof SwitchInstruction
                        || op == Opcodes.ATHROW
                        || op == Opcodes.MONITORENTER || op == Opcodes.MONITOREXIT
                        || op == Opcodes.IRETURN || op == Opcodes.LRETURN || op == Opcodes.FRETURN
                        || op == Opcodes.DRETURN || op == Opcodes.ARETURN || op == Opcodes.RETURN) {
                    return SB_WALK_BAIL;
                }
                if (c instanceof Jump) {
                    Integer target = labelIndex.get(((Jump) c).getLabel());
                    if (target == null || target.intValue() <= i) {
                        return SB_WALK_BAIL; // unknown or backward edge while live
                    }
                    int pops;
                    switch (op) {
                        case Opcodes.GOTO: pops = 0; break;
                        case Opcodes.IFEQ: case Opcodes.IFNE: case Opcodes.IFLT:
                        case Opcodes.IFGE: case Opcodes.IFGT: case Opcodes.IFLE:
                        case Opcodes.IFNULL: case Opcodes.IFNONNULL:
                            pops = 1; break;
                        case Opcodes.IF_ICMPEQ: case Opcodes.IF_ICMPNE: case Opcodes.IF_ICMPLT:
                        case Opcodes.IF_ICMPGE: case Opcodes.IF_ICMPGT: case Opcodes.IF_ICMPLE:
                        case Opcodes.IF_ACMPEQ: case Opcodes.IF_ACMPNE:
                            pops = 2; break;
                        default:
                            return SB_WALK_BAIL;
                    }
                    if (pops > cur) {
                        return SB_WALK_BAIL; // branches ON the ref
                    }
                    cur -= pops;
                    work.add(new int[]{target.intValue(), cur});
                    if (op == Opcodes.GOTO) {
                        break; // no fallthrough
                    }
                    continue; // fallthrough path continues in this loop
                }
                if (c instanceof Invoke) {
                    Invoke inv = (Invoke) c;
                    if (op == Opcodes.INVOKEDYNAMIC) {
                        return SB_WALK_BAIL;
                    }
                    int argSlots = sbDescArgSlots(inv.getDesc());
                    int retSlots = sbDescRetSlots(inv.getDesc());
                    boolean hasReceiver = op != Opcodes.INVOKESTATIC;
                    int consumed = argSlots + (hasReceiver ? 1 : 0);
                    if (consumed <= cur) {
                        cur += retSlots - consumed; // whole call above the ref
                        continue;
                    }
                    // the call reaches the ref: legal ONLY as the receiver of a
                    // StringBuilder virtual call with exactly the args above it
                    if (hasReceiver && op == Opcodes.INVOKEVIRTUAL
                            && SB_OWNER.equals(inv.getOwner()) && cur == argSlots) {
                        if (inv.getDesc().endsWith(")Ljava/lang/StringBuilder;")) {
                            cur = 0; // the returned alias replaces the ref
                            continue;
                        }
                        break; // consumed for good on this path
                    }
                    if (builderArgumentIsBorrowed(inv, cur)) break;
                    return SB_WALK_BAIL;
                }
                if (op == Opcodes.POP && cur == 0) {
                    break; // alias discarded on this path
                }
                if (op == Opcodes.ASTORE && cur == 0 && c instanceof VarOp) {
                    int n = ((VarOp) c).getIndex();
                    if (storedLocal == -1) {
                        storedLocal = n;
                    } else if (storedLocal != n) {
                        return SB_WALK_BAIL;
                    }
                    aliasStoresOut.add(i);
                    break; // parked in the tracked local on this path
                }
                if (op == Opcodes.DUP && cur >= 1) {
                    cur += 1; continue;
                }
                if (op == Opcodes.DUP2 && cur >= 2) {
                    cur += 2; continue;
                }
                if (op == Opcodes.POP && cur >= 1) {
                    cur -= 1; continue;
                }
                if (op == Opcodes.POP2 && cur >= 2) {
                    cur -= 2; continue;
                }
                int[] pp = sbPopPush(c);
                if (pp == null || pp[0] > cur) {
                    return SB_WALK_BAIL;
                }
                cur += pp[1] - pp[0];
            }
            if (i >= instructions.size()) {
                return SB_WALK_BAIL; // ran off the method with the ref live
            }
        }
        return storedLocal;
    }

    private void stackAllocStringBuilders() {
        long stackFusedBudget = 0;
        if (DISABLE_SB_STACK_ALLOC) {
            return;
        }
        // Count this method's StringBuilder allocation sites BEFORE any bail, or a
        // refusal reports as zero sites refused and the census answers its own
        // question wrongly.
        int sbSites = 0;
        boolean sawTryCatch = false;
        for (int i = 0; i < instructions.size(); i++) {
            Instruction in = instructions.get(i);
            if (in instanceof TryCatch) {
                sawTryCatch = true;
            } else if (in instanceof TypeInstruction && in.getOpcode() == Opcodes.NEW
                    && SB_OWNER.equals(((TypeInstruction) in).getTypeName())) {
                sbSites++;
            }
        }
        sbCensusSites += sbSites;
        if (synchronizedMethod) {
            sbCensusBailSync += sbSites;
            return;
        }
        if (sawTryCatch) {
            // ONE try/catch anywhere in the method used to disable stack allocation for
            // EVERY builder in it -- 315 of this corpus' 1,818 sites, 17%, including
            // builders nowhere near the protected range. 300 of those 315 survive the
            // analysis unchanged, and the reason is that the analysis was already
            // control-flow-INSENSITIVE where it counts.
            //
            // Walk the argument. A builder parked in a local is validated by checking
            // EVERY instruction in the method that touches that slot, in index order,
            // with no regard for how control reaches it -- so a use inside a handler is
            // checked exactly like any other, and an escape there (PUTFIELD, a
            // non-borrowing call) bails the same way. A builder never parked in a local
            // is consumed inside one expression, and an exception mid-expression
            // DISCARDS it: the catch block resets SP to &stack[1], so nothing in flight
            // survives the edge to be captured.
            //
            // What the edge cannot do is extend the object's LIFETIME, which is the only
            // thing stack allocation actually depends on. The struct is a C local of the
            // same function as the setjmp, so a longjmp lands in the frame that owns it.
            //
            // The residual risk is a wrong escape analysis, and that risk is not new --
            // it applies to every stack allocation this translator already does. It is
            // now checked rather than argued: the verifier compares every traced
            // reference against each thread's C stack range, and run-gc-verify.sh's
            // self-test6 requires it to catch a deliberately escaped builder and to stay
            // quiet on correctly compiled code.
            sbCensusBailTryCatch += sbSites;
        }
        java.util.Map<org.objectweb.asm.Label, Integer> labelIndex =
                new java.util.HashMap<org.objectweb.asm.Label, Integer>();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction in = instructions.get(i);
            if (in instanceof LabelInstruction) {
                labelIndex.put(((LabelInstruction) in).getLabel(), i);
            }
        }
        for (int idx = 0; idx < instructions.size(); idx++) {
            Instruction ins = instructions.get(idx);
            if (!(ins instanceof TypeInstruction) || ins.getOpcode() != Opcodes.NEW) {
                continue;
            }
            TypeInstruction ti = (TypeInstruction) ins;
            if (!SB_OWNER.equals(ti.getTypeName()) || ti.getStackAllocType() != null
                    || ti.isScalarReplaced() || ti.isInitBeforePublish() || ti.isFusedNew()) {
                continue;
            }
            // NEW ; DUP ; [simple straight-line args] ; INVOKESPECIAL SB.<init>
            int dupIdx = srNextReal(idx + 1);
            if (dupIdx < 0 || instructions.get(dupIdx).getOpcode() != Opcodes.DUP) {
                continue;
            }
            int invIdx = -1;
            boolean bail = false;
            int above = 0; // slots above the DUP'd ref (the ctor args build here)
            for (int j = srNextReal(dupIdx + 1); j >= 0; j = srNextReal(j + 1)) {
                Instruction c = instructions.get(j);
                int op = c.getOpcode();
                if (c instanceof LabelInstruction) { bail = true; break; }
                if (c instanceof Invoke && op == Opcodes.INVOKESPECIAL
                        && "<init>".equals(((Invoke) c).getName())) {
                    Invoke iv = (Invoke) c;
                    if (SB_OWNER.equals(iv.getOwner()) && above == sbDescArgSlots(iv.getDesc())) {
                        invIdx = j;
                    } else {
                        bail = true;
                    }
                    break;
                }
                if (c instanceof Jump || c instanceof CustomJump || c instanceof SwitchInstruction) {
                    bail = true; break;
                }
                if (c instanceof Invoke) {
                    Invoke iv = (Invoke) c;
                    if (op == Opcodes.INVOKEDYNAMIC) { bail = true; break; }
                    int consumed = sbDescArgSlots(iv.getDesc()) + (op == Opcodes.INVOKESTATIC ? 0 : 1);
                    if (consumed > above) { bail = true; break; }
                    above += sbDescRetSlots(iv.getDesc()) - consumed;
                    continue;
                }
                int[] pp = sbPopPush(c);
                if (pp == null || pp[0] > above) { bail = true; break; }
                above += pp[1] - pp[0];
            }
            if (bail || invIdx < 0) {
                continue;
            }

            // After <init> the DUP'd ref is on TOS. Walk the construction use.
            java.util.Set<Integer> aliasStores = new java.util.HashSet<Integer>();
            int trackedLocal = sbWalkUse(srNextReal(invIdx + 1), 0, -1, labelIndex, aliasStores);
            if (trackedLocal == SB_WALK_BAIL) {
                continue;
            }

            boolean ok = true;
            // -Dcn1.sbSkipEscapeValidation=true drops the whole-lifetime check below so a
            // builder that provably escapes is stack-allocated anyway. It exists for one
            // reason: the verifier's escaped-stack-object check has to be shown catching
            // a real escape, and nothing in correct code produces one. Never set outside
            // run-gc-verify.sh's self-test.
            if (trackedLocal >= 0 && !SB_SKIP_ESCAPE_VALIDATION) {
                // The construction walk parked the ref in a local. Validate the
                // slot's whole lifetime: no reads that could belong to a previous
                // scope's value, every ALOAD walks to a legal consumption, every
                // ASTORE to the slot is one of the recorded alias stores (a store
                // of anything ELSE into the slot -- javac slot reuse -- bails:
                // soundness over coverage).
                int definingStore = Integer.MAX_VALUE;
                for (int k : aliasStores) {
                    definingStore = Math.min(definingStore, k);
                }
                for (int k = 0; k < instructions.size() && ok; k++) {
                    Instruction u = instructions.get(k);
                    if (!(u instanceof VarOp)) {
                        if (u.getOpcode() == Opcodes.RET) { ok = false; }
                        continue;
                    }
                    int op = u.getOpcode();
                    int n = ((VarOp) u).getIndex();
                    if (n != trackedLocal) {
                        continue;
                    }
                    if (op == Opcodes.ALOAD) {
                        if (k < definingStore) { ok = false; break; }
                        int w = sbWalkUse(k + 1, 0, trackedLocal, labelIndex, aliasStores);
                        if (w == SB_WALK_BAIL) { ok = false; break; }
                    } else if (op == Opcodes.ASTORE || op == Opcodes.ISTORE
                            || op == Opcodes.LSTORE || op == Opcodes.FSTORE || op == Opcodes.DSTORE) {
                        // non-ASTOREs to the same slot index (javac slot sharing
                        // across types) also retarget it -- treated below
                        if (op != Opcodes.ASTORE) { ok = false; break; }
                    } else if (op == Opcodes.RET) {
                        ok = false; break;
                    }
                }
                if (ok) {
                    for (int k = 0; k < instructions.size() && ok; k++) {
                        Instruction u = instructions.get(k);
                        if (u instanceof VarOp && u.getOpcode() == Opcodes.ASTORE
                                && ((VarOp) u).getIndex() == trackedLocal
                                && !aliasStores.contains(k)) {
                            ok = false; // an unrelated value retargets the slot
                        }
                    }
                }
            }
            if (!ok) {
                continue;
            }
            ti.markImplicitStackAlloc();
            sbCensusStackAllocated++;

            int bytes = stackFusedBudget + SB_STACK_FLOOR_UNITS <= SB_STACK_BUDGET_BYTES
                    ? SB_STACK_FLOOR_UNITS : 16;
            stackFusedBudget += bytes;
            ti.setStackBuilderBytes(bytes);
        }
    }

    // ------------------------------------------------------------------
    // FUSED OBJECTS, constructor side (see FusedConstructor). For a ctor of a
    // @Fused class with a plan, replace each planned quadruple
    //   ALOAD 0 ; <len> ; NEWARRAY T ; PUTFIELD f
    // with the KEEP-IF-NULL FusedFieldInit statement. The re-match uses the
    // same shape as the parse-time analysis; instructions are still raw here
    // (this runs first in optimize()).
    // ------------------------------------------------------------------
    private void replaceFusedCtorTriples() {
        if (!constructor || fusedCtorPlan == null) {
            return;
        }
        ByteCodeClass cls = Parser.getClassObject(clsName);
        if (cls == null || !cls.isFused()) {
            return;
        }
        java.util.List<com.codename1.tools.translator.bytecodes.FusedConstructor.Child> kids =
                fusedCtorPlan.getChildren();
        int nextChild = 0;
        for (int i = 0; i < instructions.size() - 3 && nextChild < kids.size(); i++) {
            Instruction a0 = instructions.get(i);
            if (!(a0 instanceof VarOp) || a0.getOpcode() != Opcodes.ALOAD
                    || ((VarOp) a0).getIndex() != 0) {
                continue;
            }
            int i1 = srNextReal(i + 1);
            int i2 = i1 < 0 ? -1 : srNextReal(i1 + 1);
            int i3 = i2 < 0 ? -1 : srNextReal(i2 + 1);
            if (i3 < 0) {
                break;
            }
            Instruction na = instructions.get(i2);
            Instruction pf = instructions.get(i3);
            if (!(na instanceof VarOp) || na.getOpcode() != Opcodes.NEWARRAY
                    || !(pf instanceof Field) || pf.getOpcode() != Opcodes.PUTFIELD) {
                continue;
            }
            com.codename1.tools.translator.bytecodes.FusedConstructor.Child child = kids.get(nextChild);
            if (!((Field) pf).getFieldName().equals(child.getFieldName())) {
                continue; // a different (unplanned) array store; leave it alone
            }
            // replace the four real instructions with the single statement
            instructions.remove(i3);
            instructions.remove(i2);
            instructions.remove(i1);
            instructions.set(i, new com.codename1.tools.translator.bytecodes.FusedFieldInit(child));
            nextChild++;
        }
    }

    // ------------------------------------------------------------------
    // PER-OBJECT MEMSET ELIMINATION (init-before-publish).
    //
    // Recognizes:   NEW X ; DUP ; <args> ; INVOKESPECIAL X.<init>
    // where X.<init> is inlinable (InlinableConstructor -- super()==Object,
    // body is param/const field stores only). The arg region must be free of
    // nested NEW / other <init> / stack-shuffle / stack-store/pop / control-flow
    // (same shape the scalar-replace matcher requires) so the receiver reliably
    // sits at the bottom of the group and the placeholder can never leak into a
    // local. On a match the NEW is deferred (pushes only a null placeholder) and
    // the <init> is marked to allocate-build-publish; the body memset is elided
    // (the object is never a GC root while half-built -- fields the ctor does
    // not write are zeroed explicitly by the emitter).
    // Conservative: any deviation from the shape leaves today's memset path.
    // ------------------------------------------------------------------
    private void markInitBeforePublish() {
        for (int idx = 0; idx < instructions.size(); idx++) {
            Instruction ins = instructions.get(idx);
            if (!(ins instanceof TypeInstruction)) {
                continue;
            }
            TypeInstruction ti = (TypeInstruction) ins;
            if (ti.getOpcode() != Opcodes.NEW) {
                continue;
            }
            // leave @StackAllocate / scalar-replaced NEWs to their own path
            if (ti.isScalarReplaced() || ti.getStackAllocType() != null) {
                continue;
            }

            // DUP must immediately follow the NEW.
            int dupIdx = srNextReal(idx + 1);
            if (dupIdx < 0 || instructions.get(dupIdx).getOpcode() != Opcodes.DUP) {
                continue;
            }

            // Find the matching INVOKESPECIAL X.<init>; bail if the arg region
            // holds a nested NEW / other <init> / stack-shuffle / control-flow.
            int invIdx = -1;
            boolean bail = false;
            for (int j = srNextReal(dupIdx + 1); j >= 0; j = srNextReal(j + 1)) {
                Instruction c = instructions.get(j);
                int op = c.getOpcode();
                if (c instanceof Invoke && op == Opcodes.INVOKESPECIAL
                        && "<init>".equals(((Invoke) c).getName())) {
                    invIdx = j;
                    break;
                }
                if (c instanceof TypeInstruction && op == Opcodes.NEW) { bail = true; break; }
                if (c instanceof Jump || c instanceof CustomJump || c instanceof SwitchInstruction
                        || c instanceof TryCatch) { bail = true; break; }
                switch (op) {
                    case Opcodes.DUP: case Opcodes.DUP_X1: case Opcodes.DUP_X2:
                    case Opcodes.DUP2: case Opcodes.DUP2_X1: case Opcodes.DUP2_X2:
                    case Opcodes.SWAP: case Opcodes.GOTO:
                    // ASTORE could park the (deferred, null) placeholder in a local;
                    // POP/POP2 could drop it. Verifier-legal, javac never emits it
                    // in the NEW;DUP idiom -- bail so the placeholder can only flow
                    // to the DUP pair consumed by the marked <init>.
                    case Opcodes.ASTORE: case Opcodes.POP: case Opcodes.POP2:
                    case Opcodes.MONITORENTER: case Opcodes.MONITOREXIT:
                    case Opcodes.ATHROW:
                        bail = true;
                        break;
                    default:
                        break;
                }
                if (bail) { break; }
            }
            if (bail || invIdx < 0) {
                continue;
            }
            Invoke initInv = (Invoke) instructions.get(invIdx);
            // the <init> owner must match the NEW'd type (else the receiver is not
            // the freshly-NEW'd object at the bottom of the group).
            String newType = srMangle(ti.getTypeName());
            String initOwner = srMangle(initInv.getOwner());
            if (!newType.equals(initOwner)) {
                continue;
            }

            // FUSED OBJECTS take precedence over memset elimination: a @Fused
            // class's matched ctor gets the single-block owner+children
            // allocation (zeroed; the ordinary out-of-line ctor then runs with
            // the children pre-installed -- see FusedConstructor).
            com.codename1.tools.translator.bytecodes.FusedConstructor fusedPlan =
                    com.codename1.tools.translator.bytecodes.FusedConstructor.analyze(
                            initInv.getOwner(), initInv.getDesc());
            if (fusedPlan != null) {
                ti.markFusedNew();
                initInv.setFusedPlan(fusedPlan);
                continue;
            }

            com.codename1.tools.translator.bytecodes.InlinableConstructor plan =
                    com.codename1.tools.translator.bytecodes.InlinableConstructor.analyze(
                            initInv.getOwner(), initInv.getDesc());
            if (plan == null) {
                continue; // opaque ctor -> keep the memset
            }
            // A finalizer-bearing class must keep the memset path: the concurrent
            // sweep's grace branch reads parentCls->finalizerFunction on mark==-1
            // slots to raise the page's needsReclaim flag, and a mid-construction
            // elided object has parentCls==0 there -- its finalizer would go
            // unrecorded (the sweep's NULL guard skips it). Trivial-ctor classes
            // with finalize() are vanishingly rare, so nothing of value is lost.
            ByteCodeClass newCls = Parser.getClassObject(newType);
            if (newCls == null || newCls.hasFinalizer()) {
                continue;
            }

            // accept: defer the NEW, mark the <init> to allocate-build-publish.
            ti.markInitBeforePublish();
            initInv.markInitBeforePublish();
        }
    }

    /** X extends java.lang.Object directly and every instance field is primitive (no static initializer). */
    private boolean srPrimitiveOnlyDirectObject(ByteCodeClass x) {
        ByteCodeClass base = x.getBaseClassObject();
        if (base == null || !"java_lang_Object".equals(base.getClsName())) {
            return false; // require a direct Object subclass so dropping super.<init> is safe
        }
        for (BytecodeMethod m : x.getMethods()) {
            if ("__CLINIT__".equals(m.getMethodName())) {
                return false; // a static initializer would be skipped -> unsafe
            }
        }
        for (ByteCodeField f : x.getFields()) {
            if (!f.isStaticField() && f.isObjectType()) {
                return false; // an object field must stay heap/GC-visible
            }
        }
        return true;
    }

    /**
     * Verifies X.&lt;init&gt; is exactly Object.&lt;init&gt; + param-&gt;field PUTFIELD groups,
     * each ctor param consumed once, every instance field of X assigned once.
     * On success fills membersOut[0] / qualsOut[0] indexed by ctor-arg position.
     *
     * The instruction-shape half runs at PARSE time (see
     * {@link #analyzeScalarConstructorRaw()}) because optimize() folds the
     * ALOAD/xLOAD/PUTFIELD groups into opaque custom instructions -- reading
     * the ctor's live list here would make the answer depend on whether X had
     * already been optimized, i.e. on class processing order. Only the
     * class-level bijection check, which needs the resolved field list, is
     * left for here.
     */
    private boolean srAnalyzeCtor(ByteCodeClass x, Invoke initInv,
                                  String[][] membersOut, char[][] qualsOut) {
        String desc = initInv.getDesc();
        BytecodeMethod ctor = null;
        for (BytecodeMethod m : x.getMethods()) {
            if ("__INIT__".equals(m.getMethodName()) && desc.equals(m.getSignature())) {
                ctor = m;
                break;
            }
        }
        if (ctor == null || !ctor.srCtorShapeOk) {
            return false;
        }
        // every instance field of X must be assigned exactly once (definite init)
        int instanceFieldCount = 0;
        for (ByteCodeField bf : x.getFields()) {
            if (!bf.isStaticField()) {
                instanceFieldCount++;
                if (!ctor.srCtorAssigned.contains(srMangle(x.getClsName()) + "_" + bf.getFieldName())) {
                    return false;
                }
            }
        }
        if (instanceFieldCount != ctor.srCtorAssigned.size()) {
            return false;
        }

        membersOut[0] = ctor.srCtorMembers;
        qualsOut[0] = ctor.srCtorQuals;
        return true;
    }

    // Shape half of srAnalyzeCtor, snapshotted from the RAW instruction list at
    // parse time (called from computeRawMethodPlans, exactly like the
    // InlinableConstructor / FusedConstructor plans next to it).
    private String[] srCtorMembers;
    private char[] srCtorQuals;
    private java.util.Set<String> srCtorAssigned;
    private boolean srCtorShapeOk;

    private void analyzeScalarConstructorRaw() {
        srCtorShapeOk = false;
        srCtorAssigned = java.util.Collections.emptySet();
        List<ByteCodeMethodArg> args = Util.getMethodArgs(desc);
        int n = args.size();
        if (n == 0) {
            return; // empty ctor leaves fields uninitialized under scalar repl
        }
        String saType = srMangle(clsName);
        // ctor-arg local-slot layout (this=0; long/double take two slots)
        Map<Integer, Integer> slotToParam = new HashMap<Integer, Integer>();
        int slot = 1;
        for (int i = 0; i < n; i++) {
            slotToParam.put(slot, i);
            slot += args.get(i).isDoubleOrLong() ? 2 : 1;
        }

        List<Instruction> body = new ArrayList<Instruction>();
        for (Instruction in : instructions) {
            if (in instanceof LineNumber || in instanceof LabelInstruction || in instanceof LocalVariable) {
                continue;
            }
            body.add(in);
        }

        int i = 0;
        // optional (in practice always present) super call -- must be no-arg Object.<init>
        if (i + 1 < body.size()
                && body.get(i) instanceof VarOp && body.get(i).getOpcode() == Opcodes.ALOAD
                && ((VarOp) body.get(i)).getIndex() == 0
                && body.get(i + 1) instanceof Invoke && body.get(i + 1).getOpcode() == Opcodes.INVOKESPECIAL
                && "<init>".equals(((Invoke) body.get(i + 1)).getName())) {
            Invoke sup = (Invoke) body.get(i + 1);
            if (!"java/lang/Object".equals(sup.getOwner()) || !"()V".equals(sup.getDesc())) {
                return;
            }
            i += 2;
        }

        String[] members = new String[n];
        char[] quals = new char[n];
        java.util.Set<String> assigned = new java.util.HashSet<String>();
        while (i < body.size()) {
            Instruction a = body.get(i);
            if (a.getOpcode() == Opcodes.RETURN) {
                if (i != body.size() - 1) {
                    return; // trailing instructions after the void return
                }
                break;
            }
            if (i + 2 >= body.size()) {
                return;
            }
            Instruction l0 = body.get(i), l1 = body.get(i + 1), l2 = body.get(i + 2);
            if (!(l0 instanceof VarOp) || l0.getOpcode() != Opcodes.ALOAD || ((VarOp) l0).getIndex() != 0) {
                return;
            }
            if (!(l1 instanceof VarOp)) {
                return;
            }
            int lop = l1.getOpcode();
            if (lop != Opcodes.ILOAD && lop != Opcodes.LLOAD && lop != Opcodes.FLOAD && lop != Opcodes.DLOAD) {
                return; // object load -> not a primitive param store
            }
            Integer pi = slotToParam.get(((VarOp) l1).getIndex());
            if (pi == null) {
                return; // not loading a ctor param exactly
            }
            if (!(l2 instanceof Field) || l2.getOpcode() != Opcodes.PUTFIELD) {
                return;
            }
            Field f = (Field) l2;
            if (f.isObject() || !srMangle(f.getOwner()).equals(saType)) {
                return;
            }
            char q = args.get(pi).getQualifier();
            if (q != srQualifierOfDesc(f.getDesc())) {
                return; // ctor param type must match field type
            }
            if (members[pi] != null) {
                return; // param stored into two fields
            }
            String member = srMangle(f.getOwner()) + "_" + f.getFieldName();
            if (!assigned.add(member)) {
                return; // field assigned twice
            }
            members[pi] = member;
            quals[pi] = q;
            i += 3;
        }

        for (String member : members) {
            if (member == null) {
                return; // some ctor param never stored
            }
        }

        srCtorMembers = members;
        srCtorQuals = quals;
        srCtorAssigned = assigned;
        srCtorShapeOk = true;
    }

    private static char srQualifierOfDesc(String desc) {
        switch (desc.charAt(0)) {
            case 'J': return 'l';
            case 'D': return 'd';
            case 'F': return 'f';
            case 'L': case '[': return 'o';
            default:  return 'i'; // I,S,B,C,Z
        }
    }

    /** True iff every use of local n is "ALOAD n; GETFIELD" and n is not read before storeIdx. */
    private boolean srValidateLocalUses(int localN, int storeIdx, ByteCodeClass x) {
        for (int i = 0; i < instructions.size(); i++) {
            Instruction in = instructions.get(i);
            if (!(in instanceof VarOp)) {
                continue;
            }
            VarOp v = (VarOp) in;
            if (v.getIndex() != localN) {
                continue;
            }
            int op = v.getOpcode();
            if (op == Opcodes.ASTORE) {
                if (i != storeIdx) {
                    return false; // a second store to n -> slot reused, bail
                }
                continue;
            }
            if (op == Opcodes.ALOAD) {
                if (i < storeIdx) {
                    return false; // read before the construction
                }
                int g = srNextRealNoJoin(i + 1);
                if (g < 0) {
                    return false; // nothing usable, or a control-flow join in between
                }
                Instruction nxt = instructions.get(g);
                // Accept a direct field read OR a trivial-getter call (the receiver is a `new x()`,
                // so the virtual dispatch resolves to x's own getter -> equivalent to GETFIELD, and
                // it does not escape the instance). Anything else is a real escape/identity use.
                if (nxt instanceof Field && nxt.getOpcode() == Opcodes.GETFIELD) {
                    continue;
                }
                if (nxt instanceof Invoke && srTrivialGetterField((Invoke) nxt, x) != null) {
                    continue;
                }
                return false; // any use other than an immediate field read / trivial getter
            }
            return false; // ILOAD/etc. on this slot -> type confusion, bail
        }
        return true;
    }

    /** If {@code inv} calls a trivial getter (body exactly {@code ALOAD 0; GETFIELD f; xRETURN})
     *  declared on {@code x}, returns that field, else null. The receiver at the call is a
     *  {@code new x()}, so dispatch is deterministically x's own method -- folding it to a field
     *  read is sound regardless of any subclass overrides.
     *
     *  The shape itself is recognized at parse time ({@link #analyzeTrivialGetterRaw()}): by the
     *  time this call site is optimized the getter's own list may already have been folded into
     *  custom instructions, and matching against that would make the decision depend on class
     *  processing order. */
    private Field srTrivialGetterField(Invoke inv, ByteCodeClass x) {
        int op = inv.getOpcode();
        if (op != Opcodes.INVOKEVIRTUAL && op != Opcodes.INVOKESPECIAL && op != Opcodes.INVOKEINTERFACE) {
            return null;
        }
        for (BytecodeMethod m : x.getMethods()) {
            // name AND descriptor: an overload set shares the name, and binding the
            // wrong member of it would fold the read to the wrong field.
            if (!inv.getName().equals(m.getMethodName()) || !inv.getDesc().equals(m.getSignature())) {
                continue;
            }
            return m.trivialGetterField;
        }
        return null;
    }

    // The GETFIELD a trivial getter returns, snapshotted from the RAW instruction list at parse
    // time; null when this method is not a trivial getter. Only the field's owner/name/descriptor
    // are ever read from it, and those are fixed at construction.
    /**
     * True when this frameless method provably cannot deepen the native C stack
     * past its own frame, so CN1_FRAMELESS_SOE_GUARD can be left off.
     *
     * THE GUARD IS NOT FREE AND IT IS NOT RARE. It is a load, a frame-address
     * read and a two-sided compare on entry, measured at 0.277ns against a
     * 0.694ns bare call, and a counting build of the hello corpus took
     * 597,522,393 guarded entries in one translation -- 165ms of a 5.4s run.
     *
     * WHY A LEAF IS SAFE. The guard exists because a frameless frame does not
     * bump callStackOffset, so the 1024-deep call limit cannot see it and
     * unbounded Java recursion would run the C stack into a SIGSEGV instead of
     * throwing StackOverflowError. A method that issues no call cannot recurse,
     * and it adds exactly one frame below a caller that IS guarded. That frame
     * cannot cross the guard band on its own: the band is 256KB and, as the
     * guard's own comment puts it, no single frameless frame approaches 256KB.
     * So the deepest a leaf can reach is its guarded caller's depth plus a few
     * dozen bytes, which is inside the headroom the band exists to provide.
     *
     * "Issues no call" is read conservatively -- an allocation counts, because
     * its slow path reaches codenameOneGcMalloc and possibly a collection, and
     * a static field read counts, because it can run a class initializer.
     */
    private boolean isSoeGuardUnnecessary() {
        // The pre-existing carve-out: a bounded instance field getter.
        if (!staticMethod && trivialGetterField != null && maxStack + maxLocals <= 16) {
            return true;
        }
        return isLeafForStackDepth();
    }

    /** @return true if nothing in the body can transfer control into another frame. */
    private boolean isLeafForStackDepth() {
        for (Instruction i : instructions) {
            if (i instanceof Invoke || i instanceof CustomInvoke || i instanceof MultiArray) {
                return false;
            }
            if (i instanceof TypeInstruction) {
                int op = i.getOpcode();
                // NEW / ANEWARRAY reach the allocator, which can collect.
                if (op == Opcodes.NEW || op == Opcodes.ANEWARRAY) {
                    return false;
                }
                continue;
            }
            if (i instanceof Field) {
                // GETSTATIC/PUTSTATIC can run a class initializer.
                int op = i.getOpcode();
                if (op == Opcodes.GETSTATIC || op == Opcodes.PUTSTATIC) {
                    return false;
                }
                continue;
            }
            switch (i.getOpcode()) {
                case Opcodes.ATHROW:
                case Opcodes.MONITORENTER:
                case Opcodes.MONITOREXIT:
                case Opcodes.NEWARRAY:
                case Opcodes.MULTIANEWARRAY:
                    return false;
                default:
                    break;
            }
        }
        return true;
    }

    private Field trivialGetterField;

    private void analyzeTrivialGetterRaw() {
        Instruction a = null, b = null, c = null;
        int count = 0;
        for (Instruction bi : instructions) {
            if (bi instanceof LineNumber || bi instanceof LabelInstruction || bi instanceof LocalVariable) {
                continue;
            }
            count++;
            if (count == 1) {
                a = bi;
            } else if (count == 2) {
                b = bi;
            } else if (count == 3) {
                c = bi;
            } else {
                return;
            }
        }
        if (count != 3
                || !(a instanceof VarOp) || a.getOpcode() != Opcodes.ALOAD || ((VarOp) a).getIndex() != 0
                || !(b instanceof Field) || b.getOpcode() != Opcodes.GETFIELD) {
            return;
        }
        int rc = c.getOpcode();
        if (rc != Opcodes.IRETURN && rc != Opcodes.LRETURN && rc != Opcodes.FRETURN
                && rc != Opcodes.DRETURN && rc != Opcodes.ARETURN) {
            return;
        }
        trivialGetterField = (Field) b;
    }

    /**
     * Replaces every "ALOAD n; GETFIELD X.f" with a single direct member-read
     * instruction. The ALOAD is removed outright (rather than blanked) so the read
     * sits with no gap between it and its arithmetic neighbours -- otherwise a
     * leftover blank breaks the operand adjacency the arithmetic/store reduction
     * relies on and the surrounding math stays in slow operand-stack form.
     */
    private void srRewriteFieldReads(int localN, int id, ByteCodeClass x) {
        for (int i = 0; i < instructions.size(); i++) {
            Instruction in = instructions.get(i);
            if (!(in instanceof VarOp) || in.getOpcode() != Opcodes.ALOAD
                    || ((VarOp) in).getIndex() != localN) {
                continue;
            }
            int g = srNextRealNoJoin(i + 1);
            if (g < 0) {
                continue; // cannot happen for a validated local; never rewrite blind
            }
            Instruction gi = instructions.get(g);
            // Either an immediate GETFIELD or a trivial-getter call (validated above); both fold to
            // the same scalar member read, replacing the second instruction and dropping the ALOAD.
            Field f = (gi instanceof Field) ? (Field) gi : srTrivialGetterField((Invoke) gi, x);
            String member = srMangle(f.getOwner()) + "_" + f.getFieldName();
            String push;
            switch (f.getDesc().charAt(0)) {
                case 'D': push = "PUSH_DOUBLE"; break;
                case 'F': push = "PUSH_FLOAT"; break;
                case 'J': push = "PUSH_LONG"; break;
                default:  push = "PUSH_INT"; break;
            }
            final String lvalue = "__cn1sr_" + id + "." + member;
            String code = "    " + push + "(" + lvalue + "); /* scalar-replaced GETFIELD */\n";
            // Supply an AssignableExpression yielding the bare member lvalue so the
            // arithmetic/store reduction passes can fold the read straight into a
            // direct register expression (instead of round-tripping the operand
            // stack). Without this the surrounding math stays in slow SP[] form.
            AssignableExpression read = new ScalarReplacedRead(lvalue);
            instructions.set(g, new CustomIntruction(code, code, new ArrayList<String>(), read));
            instructions.remove(i);   // drop the ALOAD; read now occupies one slot
            // 'i' now points at the read (former g-1); loop's i++ moves past it.
        }
    }

    /// Bare-lvalue read for a scalar-replaced field (see srRewriteFieldReads).
    /// Static: it captures only the lvalue string, not the enclosing method.
    private static final class ScalarReplacedRead implements AssignableExpression {
        private final String lvalue;

        ScalarReplacedRead(String lvalue) {
            this.lvalue = lvalue;
        }

        public boolean assignTo(String varName, StringBuilder sb) {
            if (varName != null) {
                sb.append("    ").append(varName).append(" = ");
            }
            sb.append(lvalue);
            if (varName != null) {
                sb.append(";\n");
            }
            return true;
        }
    }

    private CustomIntruction srEmpty() {
        return new CustomIntruction("", "", new ArrayList<String>());
    }

    /**
     * Drop a CHECKCAST that immediately repeats the one before it.
     *
     * Deliberately narrow. Only a LineNumber may sit between the two, because it
     * carries no semantics; a LabelInstruction may NOT, since another path can
     * jump there with a different value on the stack, and then the second cast is
     * the only one guarding it. Same reasoning for anything else in between: if it
     * can touch the stack, the second cast is not redundant.
     */
    private void removeRepeatedCheckcasts() {
        TypeInstruction previousCast = null;
        for (int iter = 0 ; iter < instructions.size() ; iter++) {
            Instruction current = instructions.get(iter);
            if (current instanceof LineNumber) {
                continue;                       // no semantics, does not break the pair
            }
            if (current instanceof TypeInstruction
                    && current.getOpcode() == Opcodes.CHECKCAST) {
                TypeInstruction cast = (TypeInstruction) current;
                if (previousCast != null
                        && previousCast.getTypeName() != null
                        && previousCast.getTypeName().equals(cast.getTypeName())) {
                    instructions.remove(iter);
                    iter--;                     // the list shifted under us
                    continue;                   // previousCast still stands
                }
                previousCast = cast;
                continue;
            }
            previousCast = null;
        }
    }


    boolean optimize() {
        // FUSED OBJECTS, constructor side: rewrite each planned
        // `ALOAD 0; <len>; NEWARRAY T; PUTFIELD f` quadruple into the
        // self-contained KEEP-IF-NULL FusedFieldInit BEFORE any other pass can
        // fold/reorder those instructions. Runs on the raw list (first thing).
        replaceFusedCtorTriples();

        // A CHECKCAST immediately repeated to the SAME type is a no-op: the first
        // one already proved the type or threw, and neither touches the stack
        // otherwise. javac emits the pair readily -- 23 of the 122 checkcast sites
        // in a backend build were duplicates, 9 of them in java.lang.String, whose
        // charInternal is the hottest String method under a server load.
        //
        // Worth removing rather than tolerating because a checked cast is REAL work
        // here: the clean target enables checked casts unconditionally and other
        // targets can pass -Dcn1.checkedCasts=true, so BC_CHECKCAST_CHECKED walks
        // the class hierarchy instead of expanding to nothing.
        removeRepeatedCheckcasts();

        int instructionCount = instructions.size();

        // optimize away a method that only contains the void return instruction e.g. blank constructors etc.
        if(instructionCount < 6) {
            int realCount = instructionCount;
            Instruction actual = null;
            for(int iter = 0 ; iter < instructionCount ; iter++) {
                Instruction current = instructions.get(iter);
                if(current instanceof LabelInstruction) {
                    realCount--;
                    continue;
                }
                if(current instanceof LineNumber) {
                    realCount--;
                    continue;
                }
                actual = current;
            }
            
            if(realCount == 1 && actual != null && actual.getOpcode() == Opcodes.RETURN) {
                return false;
            }
        }

        // Prove-safe bounds-check elimination runs HERE, on the raw (pre-reduction)
        // instruction list, where stores/inits/lengths are still explicit opcodes
        // (the reduction passes below fuse them into opaque CustomIntructions that
        // could hide a write and make the analysis unsound).
        analyzeBoundsChecks();

        // Scalar-replace non-escaping primitive-only @StackAllocate objects. Runs
        // on the raw (pre-reduction) instruction list so the NEW/DUP/<init>/ASTORE
        // and field-access idiom is still made of explicit opcodes. Conservative:
        // anything that deviates from the exact recognized shape is left untouched
        // and falls back to the already-working stack-alloc codegen.
        scalarReplaceStackAllocations();
        // the pass above may have removed instructions (ALOADs folded into reads)
        instructionCount = instructions.size();

        // Implicitly stack-allocate provably non-escaping StringBuilders (javac
        // string concatenation). Runs on the raw instruction list, BEFORE the
        // fused/init-before-publish NEW pairing -- both skip NEW sites whose
        // getStackAllocType() is non-null, so a marked site keeps its plain
        // out-of-line <init> call (whose keep-if-null field init then heap-
        // allocates the buffer into the stack struct's value field).
        stackAllocStringBuilders();

        // Per-object memset elimination (init-before-publish). Runs on the raw
        // (pre-fusion) instruction list -- like scalar replacement -- so the
        // NEW/DUP/<init> idiom is still made of explicit opcodes. Marks matched
        // sites; the literal-arg folding below is suppressed for them so the
        // <init> stays a plain on-stack Invoke or a receiver-on-stack CustomInvoke.
        markInitBeforePublish();

        // Mark source lines whose every instruction is non-throwing/non-calling as
        // elidable (their per-line line-info store becomes a no-op in release --
        // trace-identical, see analyzeElidableLineInfo). Runs AFTER scalar
        // replacement so a scalar-replaced object's NEW/<init>/field reads (now
        // pure struct access, no alloc/NPE) are correctly seen as non-throwing, and
        // before the general fusion below so the remaining opcodes are still clean.
        analyzeElidableLineInfo();

        boolean astoreCalls = false;
        boolean hasInstructions = false; 
        
        boolean hasTryCatch = false;
        for (int iter=0; iter < instructionCount - 1; iter++) {
            Instruction current = instructions.get(iter);
            if (current instanceof TryCatch) {
                hasTryCatch = true;
            }
            current.setMethod(this);
            if (current.isOptimized()) {
                continue;
            }
            int currentOpcode = current.getOpcode();
            switch(currentOpcode) {
                case Opcodes.CHECKCAST: {
                    // Remove the check cast for now as it gets in the way of other optimizations.
                    // This removal is WHY a failed cast never throws (issue #5531): dropping the
                    // instruction here means TypeInstruction never gets to emit anything for it,
                    // so implementing the BC_CHECKCAST macro alone would have had no effect.
                    // Under -Dcn1.checkedCasts=true the instruction is kept, at the cost of the
                    // optimizations this removal was protecting.
                    if(ByteCodeTranslator.isCheckedCastsEnabled()) {
                        break;
                    }
                    instructions.remove(iter);
                    iter--;
                    instructionCount--;
                    break;
                }
            }
        }
        
        for(int iter = 0 ; iter < instructionCount - 1 ; iter++) {
            Instruction current = instructions.get(iter);
            if (current.isOptimized()) {
                // This instruction has already been optimized
                // we should skip it and proceed to the next one
                continue;
            }
            Instruction next = instructions.get(iter + 1);

            int currentOpcode = current.getOpcode();
            int nextOpcode = next.getOpcode();
            
            
            if (ArithmeticExpression.isArithmeticOp(current)) {
                int addedIndex = ArithmeticExpression.tryReduce(instructions, iter);
                if (addedIndex >= 0) {
                    iter = addedIndex;
                    instructionCount = instructions.size();
                    continue;
                }
            }
            
            if (current instanceof Field) {
                int newIter = Field.tryReduce(instructions, iter);
                if (newIter >= 0) {
                    iter = newIter;
                    instructionCount = instructions.size();
                    continue;
                }
            }

            if (current instanceof ScalarAllocInit) {
                // By now the constructor args ahead of this point have been reduced
                // to single assignable expressions; pull them straight into the
                // struct-field assignments so the args never touch the operand stack.
                int newIter = ((ScalarAllocInit) current).fold(instructions, iter);
                if (newIter >= 0) {
                    iter = newIter;
                    instructionCount = instructions.size();
                    continue;
                }
            }
            
            
            
            switch(currentOpcode) {
                
                case Opcodes.ARRAYLENGTH: {
                    if (!dependentClasses.contains("java_lang_NullPointerException")) {
                        dependentClasses.add("java_lang_NullPointerException");
                    }
                    int newIter = ArrayLengthExpression.tryReduce(instructions, iter);
                    if (newIter >= 0) {
                        instructionCount = instructions.size();
                        iter = newIter;
                        continue;
                    }
                    break;
                }
                   
                case Opcodes.DUP: {
                    int newIter = DupExpression.tryReduce(instructions, iter);
                    if (newIter >= 0) {
                        iter = newIter;
                        instructionCount = instructions.size();
                        continue;
                    }
                    break;
                }
                
                case Opcodes.POP: {
                    if (iter > 0) {
                        Instruction prev = instructions.get(iter-1);
                        if (prev instanceof CustomInvoke) {
                            CustomInvoke inv = (CustomInvoke)prev;
                            if (inv.methodHasReturnValue() && !inv.isNoReturn()) {
                                inv.setNoReturn(true);
                                instructions.remove(iter);
                                iter--;
                                instructionCount--;
                                continue;
                            }
                        }
                    }
                    break;
                }
                
                case Opcodes.ASTORE:
                case Opcodes.ISTORE:
                case Opcodes.DSTORE:
                case Opcodes.LSTORE:
                case Opcodes.FSTORE: {
                    if (iter > 0 && current instanceof VarOp) {
                        VarOp currentVarOp = (VarOp) current;
                        Instruction prev = instructions.get(iter-1);
                        if (prev instanceof AssignableExpression) {
                            AssignableExpression expr = (AssignableExpression)prev;
                            StringBuilder sb = new StringBuilder();
                            if (currentVarOp.assignFrom(expr, sb)) {
                                instructions.remove(iter-1);
                                instructions.remove(iter-1);
                                instructions.add(iter-1, new CustomIntruction(sb.toString(), sb.toString(), dependentClasses));
                                iter = iter-1;
                                instructionCount = instructions.size();
                                continue;
                            }
                            
                        } else if (prev instanceof CustomInvoke) {
                            CustomInvoke inv = (CustomInvoke)prev;
                            StringBuilder sb = new StringBuilder();
                            if (currentVarOp.assignFrom(inv, sb)) {
                                instructions.remove(iter-1);
                                instructions.remove(iter-1);
                                instructions.add(iter-1, new CustomIntruction(sb.toString(), sb.toString(), dependentClasses));
                                iter = iter-1;
                                instructionCount = instructions.size();
                                continue;
                            }
                        }
                    }
                    
                    break;
                }
                
                case Opcodes.IRETURN:
                case Opcodes.FRETURN:
                case Opcodes.ARETURN:
                case Opcodes.LRETURN:
                case Opcodes.DRETURN: {
                    if (iter > 0 && current instanceof BasicInstruction) {
                        Instruction prev = instructions.get(iter-1);
                        if (prev instanceof AssignableExpression) {
                            AssignableExpression expr = (AssignableExpression)prev;
                            StringBuilder sb= new StringBuilder();
                            if (expr.assignTo(null, sb)) {
                                instructions.remove(iter-1);
                                instructions.remove(iter-1);
                                String exprString = sb.toString().trim();
                                String retVal = exprString;
                                sb.setLength(0);
                                if (!prev.isConstant()) {
                                    sb.append("\n{\n    ");
                                    switch (currentOpcode) {
                                        case Opcodes.IRETURN:
                                            sb.append("JAVA_INT");
                                            break;
                                        case Opcodes.FRETURN:
                                            sb.append("JAVA_FLOAT");
                                            break;
                                        case Opcodes.ARETURN:
                                            sb.append("JAVA_OBJECT");
                                            break;
                                        case Opcodes.LRETURN:
                                            sb.append("JAVA_LONG");
                                            break;
                                        case Opcodes.DRETURN:
                                            sb.append("JAVA_DOUBLE");
                                            break;
                                    }
                                    sb.append(" ___returnValue=").append(exprString).append(";\n");
                                    retVal = "___returnValue";
                                }
                                if(synchronizedMethod) {
                                    if(staticMethod) {
                                        sb.append("    monitorExitBlock(threadStateData, (JAVA_OBJECT)&class__");
                                        sb.append(getClsName());
                                        sb.append(");\n");
                                    } else {
                                        sb.append("    monitorExitBlock(threadStateData, __cn1ThisObject);\n");
                                    }
                                }
                                if(frameless) {
                                    // No frame to release -- the operand stack is a method-local array.
                                    sb.append("    return ").append(retVal).append(";\n");
                                } else if(hasTryCatch) {
                                    sb.append("    releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, methodBlockOffset); return ").append(retVal).append(";\n");
                                } else {
                                    sb.append("    releaseForReturn(threadStateData, cn1LocalsBeginInThread); return ").append(retVal).append(";\n");
                                }
                                if (!prev.isConstant()) {
                                    sb.append("}\n");
                                }
                                
                                instructions.add(iter-1, new CustomIntruction(sb.toString(), sb.toString(), dependentClasses));
                                iter--;
                                instructionCount = instructions.size();
                                continue;
                                
                            }
                        } else if (prev instanceof CustomInvoke) {
                            
                            CustomInvoke expr = (CustomInvoke)prev;
                            String returnType = expr.getReturnValue();
                            if (returnType != null && !"JAVA_OBJECT".equals(returnType)) {
                                // We can't safely return a JAVA_OBJECT directly because it needs to be added 
                                // to the stack for the GC
                                StringBuilder sb= new StringBuilder();
                                if (expr.appendExpression(sb)) {
                                    instructions.remove(iter-1);
                                    instructions.remove(iter-1);
                                    String exprString = sb.toString().trim();
                                    String retVal = exprString;
                                    sb.setLength(0);
                                    if (!expr.isConstant()) {
                                        
                                        sb.append("\n{\n    ");
                                        switch (currentOpcode) {
                                            case Opcodes.IRETURN:
                                                sb.append("JAVA_INT");
                                                break;
                                            case Opcodes.FRETURN:
                                                sb.append("JAVA_FLOAT");
                                                break;
                                            case Opcodes.ARETURN:
                                                sb.append("JAVA_OBJECT");
                                                break;
                                            case Opcodes.LRETURN:
                                                sb.append("JAVA_LONG");
                                                break;
                                            case Opcodes.DRETURN:
                                                sb.append("JAVA_DOUBLE");
                                                break;
                                        }
                                    
                                        sb.append(" ___returnValue=").append(exprString).append(";\n");
                                        retVal = "___returnValue";
                                    }
                                    if(synchronizedMethod) {
                                        if(staticMethod) {
                                            sb.append("    monitorExitBlock(threadStateData, (JAVA_OBJECT)&class__");
                                            sb.append(getClsName());
                                            sb.append(");\n");
                                        } else {
                                            sb.append("    monitorExitBlock(threadStateData, __cn1ThisObject);\n");
                                        }
                                    }
                                    if(frameless) {
                                        // No frame to release -- the operand stack is a method-local array.
                                        sb.append("    return ").append(retVal).append(";\n");
                                    } else if(hasTryCatch) {
                                        sb.append("    releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, methodBlockOffset); return ").append(retVal).append(";\n");
                                    } else {
                                        sb.append("    releaseForReturn(threadStateData, cn1LocalsBeginInThread); return ").append(retVal).append(";\n");
                                    }
                                    if (!expr.isConstant()) {
                                        sb.append("}\n");
                                    }
                                    

                                    instructions.add(iter-1, new CustomIntruction(sb.toString(), sb.toString(), dependentClasses));
                                    iter--;
                                    instructionCount = instructions.size();
                                    continue;

                                }
                            }
                        }
                    }
                    break;
                }
                
                case Opcodes.BASTORE:
                case Opcodes.SASTORE:
                case Opcodes.CASTORE:
                case Opcodes.AASTORE:
                case Opcodes.IASTORE:
                case Opcodes.DASTORE:
                case Opcodes.LASTORE:
                case Opcodes.FASTORE: {
                    if (iter > 2 && current instanceof BasicInstruction) {
                        StringBuilder devNull = new StringBuilder();
                        String arrayLiteral = null;
                        String indexLiteral = null;
                        String valueLiteral = null;
                        Instruction prev3 = instructions.get(iter-3);
                        if (prev3 instanceof AssignableExpression) {
                            if (((AssignableExpression)prev3).assignTo(null, devNull)) {
                                arrayLiteral = devNull.toString().trim();
                                
                            }
                        }
                        devNull.setLength(0);
                        Instruction prev2 = instructions.get(iter-2);
                        if (prev2 instanceof AssignableExpression) {
                            if (((AssignableExpression)prev2).assignTo(null, devNull)) {
                                indexLiteral = devNull.toString().trim();
                            }
                        }
                        devNull.setLength(0);
                        Instruction prev1 = instructions.get(iter-1);
                        
                        if (prev1 instanceof AssignableExpression) {
                            if (((AssignableExpression)prev1).assignTo(null, devNull)) {
                                valueLiteral = devNull.toString().trim();
                            }
                        } else if (prev1 instanceof CustomInvoke) {
                            devNull.setLength(0);
                            if (((CustomInvoke)prev1).appendExpression(devNull)) {
                                valueLiteral = devNull.toString().trim();
                            }
                        }
                        
                        if (arrayLiteral != null  && indexLiteral != null && valueLiteral != null) {
                            String elementType = null;
                            String valueType = null;
                            switch (current.getOpcode()) {
                                case Opcodes.AASTORE:
                                    elementType = "OBJECT";
                                    valueType = "JAVA_OBJECT";
                                    break;
                                case Opcodes.IASTORE:
                                    elementType = "INT";
                                    valueType = "JAVA_INT";
                                    break;
                                case Opcodes.DASTORE:
                                    elementType = "DOUBLE";
                                    valueType = "JAVA_DOUBLE";
                                    break;
                                    
                                case Opcodes.LASTORE:
                                    elementType = "LONG";
                                    valueType = "JAVA_LONG";
                                    break;
                                case Opcodes.FASTORE:
                                    elementType = "FLOAT";
                                    valueType = "JAVA_FLOAT";
                                    break;
                                case Opcodes.CASTORE:
                                    elementType = "CHAR";
                                    valueType = "JAVA_CHAR";
                                    break;
                                case Opcodes.BASTORE:
                                    elementType = "BYTE";
                                    valueType = "JAVA_BYTE";
                                    break;
                                case Opcodes.SASTORE:
                                    elementType = "SHORT";
                                    valueType = "JAVA_SHORT";
                                    break;
                                    
                            }
                            if (elementType == null || valueType == null) {
                                break;
                            }
                            
                            instructions.remove(iter-3);
                            instructions.remove(iter-3);
                            instructions.remove(iter-3);
                            instructions.remove(iter-3);
                            String code;
                            if (frameless && !"OBJECT".equals(elementType)) {
                                // Diverging-check store (frameless only): the failure
                                // path throws and RETURNS, keeping the cold call out of
                                // the loop body so clang can hoist array header loads.
                                // OBJECT stores keep the macro (covariance check).
                                String check = (current.isBoundsSafe() || isDisableNullAndArrayBoundsChecks()) ? "" :
                                        "        CN1_ARRAY_CHECK_DIVERGE(__cn1ArrayTmp, __cn1IndexTmp, " + (returnType.isVoid() ? "" : "0") + ");\n";
                                code = "    {\n" +
                                        "        JAVA_OBJECT __cn1ArrayTmp = " + arrayLiteral + ";\n" +
                                        "        JAVA_INT __cn1IndexTmp = " + indexLiteral + ";\n" +
                                        "        " + valueType + " __cn1ValueTmp = " + valueLiteral + ";\n" +
                                        check +
                                        "        ((JAVA_ARRAY_" + elementType + "*) CN1_ARRAY_DATA((JAVA_ARRAY)__cn1ArrayTmp))[__cn1IndexTmp] = __cn1ValueTmp;\n" +
                                        "    }\n";
                            } else {
                                code = "    {\n" +
                                        "        JAVA_OBJECT __cn1ArrayTmp = " + arrayLiteral + ";\n" +
                                        "        JAVA_INT __cn1IndexTmp = " + indexLiteral + ";\n" +
                                        "        " + valueType + " __cn1ValueTmp = " + valueLiteral + ";\n" +
                                        // The macro's own comment used to claim it covariance-checks
                                        // OBJECT stores; it never did. Under -Dcn1.checkedCasts the
                                        // check is emitted here.
                                        //
                                        // GUARDED BY THE ACCESS VALIDATION, because the JLS orders
                                        // these: NullPointerException, then
                                        // ArrayIndexOutOfBoundsException, then ArrayStoreException.
                                        // Run bare, the covariance check reports a bad VALUE on a
                                        // store whose INDEX is also bad, hiding the exception the
                                        // program should have seen -- and on a null array it used to
                                        // dereference null outright. cn1_array_access_validate throws
                                        // the right one of the first two; the setter re-checks on its
                                        // in-bounds fast path, which costs a comparison.
                                        ("OBJECT".equals(elementType) && ByteCodeTranslator.isCheckedCastsEnabled()
                                                ? "        if(cn1_array_access_in_bounds(__cn1ArrayTmp, __cn1IndexTmp)\n"
                                                + "                || cn1_array_access_validate(threadStateData, __cn1ArrayTmp, __cn1IndexTmp)) {\n"
                                                + "            CN1_ARRAY_STORE_CHECK(__cn1ArrayTmp, __cn1ValueTmp);\n"
                                                + "            CN1_SET_ARRAY_ELEMENT_"+elementType+"(__cn1ArrayTmp, __cn1IndexTmp, __cn1ValueTmp);\n"
                                                + "        }\n"
                                                : "        CN1_SET_ARRAY_ELEMENT_"+elementType
                                                    + ((current.isBoundsSafe() || isDisableNullAndArrayBoundsChecks()) ? "_NOCHK" : "")
                                                    + "(__cn1ArrayTmp, __cn1IndexTmp, __cn1ValueTmp);\n") +
                                        "    }\n";
                            }
                            instructions.add(iter-3, new CustomIntruction(code, code, dependentClasses));
                            iter = iter-3;
                            instructionCount = instructions.size();
                            continue;
                        }
                    }
                    
                    break;
                }
                    
                
                case Opcodes.FALOAD:
                case Opcodes.BALOAD:
                case Opcodes.IALOAD:
                case Opcodes.LALOAD:
                case Opcodes.DALOAD:
                case Opcodes.AALOAD:
                case Opcodes.SALOAD:
                case Opcodes.CALOAD: {
                    int newIter = ArrayLoadExpression.tryReduce(instructions, iter);
                    if (newIter >= 0) {
                        iter = newIter;
                        instructionCount = instructions.size();
                        continue;
                    }
                    break;
                }
                
                
                /* Try to optimize if statements that just use constants
                   and local variables so that they don't need the intermediate
                   push and pop from the stack.
                */
                case Opcodes.IF_ACMPEQ:
                case Opcodes.IF_ACMPNE:
                case Opcodes.IF_ICMPLE:
                case Opcodes.IF_ICMPLT:
                case Opcodes.IF_ICMPNE:
                case Opcodes.IF_ICMPGT:
                case Opcodes.IF_ICMPEQ:
                case Opcodes.IF_ICMPGE: {
                    
                    if (iter > 1) {
                        Instruction leftArg = instructions.get(iter-2);
                        Instruction rightArg = instructions.get(iter-1);
                        
                        String leftLiteral = null;
                        String rightLiteral = null;
                        
                        if (leftArg instanceof AssignableExpression) {
                            StringBuilder sb = new StringBuilder();
                            if (((AssignableExpression)leftArg).assignTo(null, sb)) {
                                leftLiteral = sb.toString().trim();
                            }
                        } else if (leftArg instanceof CustomInvoke) {
                            CustomInvoke inv = (CustomInvoke)leftArg;
                            StringBuilder sb = new StringBuilder();
                            if (!"JAVA_OBJECT".equals(inv.getReturnValue()) && inv.appendExpression(sb)) {
                                leftLiteral = sb.toString().trim();
                            }
                        }
                        if (rightArg instanceof AssignableExpression) {
                            StringBuilder sb = new StringBuilder();
                            if (((AssignableExpression)rightArg).assignTo(null, sb)) {
                                rightLiteral = sb.toString().trim();
                            }
                        } else if (rightArg instanceof CustomInvoke) {
                            CustomInvoke inv = (CustomInvoke)rightArg;
                            StringBuilder sb = new StringBuilder();
                            if (!"JAVA_OBJECT".equals(inv.getReturnValue()) && inv.appendExpression(sb)) {
                                rightLiteral = sb.toString().trim();
                            }
                        }
                        
                        // DIVERGING-check prelude for array-load operands (frameless
                        // methods only): read the element via CN1_ARRAY_CHECK_DIVERGE
                        // -- whose throw path RETURNS -- instead of the merging
                        // cn1_array_element_* accessor whose cold call inside the loop
                        // body blocks clang from hoisting the array header loads.
                        String arrayCmpPrelude = null;
                        if (frameless && rightLiteral != null && leftLiteral != null) {
                            StringBuilder pre = new StringBuilder();
                            String rv = returnType.isVoid() ? "" : "0";
                            if (leftArg instanceof ArrayLoadExpression) {
                                StringBuilder t = new StringBuilder();
                                if (((ArrayLoadExpression) leftArg).emitDiverging("__cn1AjL", rv, t)) {
                                    pre.append(t);
                                    leftLiteral = "__cn1AjL";
                                }
                            }
                            if (rightArg instanceof ArrayLoadExpression) {
                                StringBuilder t = new StringBuilder();
                                if (((ArrayLoadExpression) rightArg).emitDiverging("__cn1AjR", rv, t)) {
                                    pre.append(t);
                                    rightLiteral = "__cn1AjR";
                                }
                            }
                            if (pre.length() > 0) {
                                arrayCmpPrelude = pre.toString();
                            }
                        }

                        if (rightLiteral != null && leftLiteral != null) {
                            Jump jmp = (Jump)current;
                            instructions.remove(iter-2);
                            instructions.remove(iter-2);
                            instructions.remove(iter-2);
                            //instructions.remove(iter-2);
                            iter-=2;
                            //instructionCount -= 2;
                            StringBuilder sb = new StringBuilder();
                            String operator = null;
                            String opName = null;
                            switch (currentOpcode) {
                                case Opcodes.IF_ICMPLE:
                                    operator = "<="; opName = "IF_ICMPLE"; break;
                                case Opcodes.IF_ICMPLT:
                                    operator = "<"; opName = "IF_IMPLT"; break;
                                case Opcodes.IF_ICMPNE:
                                    operator = "!="; opName = "IF_ICMPNE"; break;
                                case Opcodes.IF_ICMPGT:
                                    operator = ">"; opName = "IF_ICMPGT"; break;
                                case Opcodes.IF_ICMPGE:
                                    operator = ">="; opName = "IF_ICMPGE"; break;
                                case Opcodes.IF_ICMPEQ:
                                    operator = "=="; opName = "IF_ICMPEQ"; break;
                                case Opcodes.IF_ACMPEQ:
                                    operator = "=="; opName = "IF_ACMPEQ"; break;
                                case Opcodes.IF_ACMPNE:
                                    operator = "!="; opName = "IF_ACMPNE"; break;
                                default :
                                    throw new RuntimeException("Invalid operator during optimization of integer comparison");
                            }


                            if (arrayCmpPrelude != null) {
                                sb.append("{\n    ").append(arrayCmpPrelude);
                            }
                            sb.append("if (").append(leftLiteral).append(operator).append(rightLiteral).append(") /* ").append(opName).append(" CustomJump */ ");
                            CustomJump newJump = CustomJump.create(jmp, sb.toString());
                            //jmp.setCustomCompareCode(sb.toString());
                            if (arrayCmpPrelude != null) {
                                newJump.setCustomSuffix("    }\n");
                            }
                            newJump.setOptimized(true);
                            instructions.add(iter, newJump);
                            instructionCount = instructions.size();

                        }
                        
                    }
                break;
                }   
                case Opcodes.IFNONNULL:
                case Opcodes.IFNULL:
                
                case Opcodes.IFLE:
                case Opcodes.IFLT:
                case Opcodes.IFNE:
                case Opcodes.IFGT:
                case Opcodes.IFEQ:
                case Opcodes.IFGE: {
                    String rightArg = "0";
                    if (currentOpcode == Opcodes.IFNONNULL || currentOpcode == Opcodes.IFNULL) {
                        rightArg = "JAVA_NULL";
                    }
                    if (iter > 0) {
                        Instruction leftArg = instructions.get(iter-1);
                        
                        String leftLiteral = null;
                        
                        
                        if (leftArg instanceof AssignableExpression) {
                            StringBuilder sb = new StringBuilder();
                            if (((AssignableExpression)leftArg).assignTo(null, sb)) {
                                leftLiteral = sb.toString().trim();
                            }
                        } else if (leftArg instanceof CustomInvoke) {
                            CustomInvoke inv = (CustomInvoke)leftArg;
                            StringBuilder sb = new StringBuilder();
                            if (inv.appendExpression(sb)) {
                                leftLiteral = sb.toString().trim();
                            }
                        }
                        
                        
                        if (leftLiteral != null) {
                            Jump jmp = (Jump)current;
                            instructions.remove(iter-1);
                            instructions.remove(iter-1);
                            //instructions.remove(iter-2);
                            iter-=1;
                            //instructionCount -= 2;
                            StringBuilder sb = new StringBuilder();
                            String operator = null;
                            String opName = null;
                            switch (currentOpcode) {
                                case Opcodes.IFLE:
                                    operator = "<="; opName = "IFLE"; break;
                                case Opcodes.IFLT:
                                    operator = "<"; opName = "IFLT"; break;
                                case Opcodes.IFNE:
                                    operator = "!="; opName = "IFNE"; break;
                                case Opcodes.IFGT:
                                    operator = ">"; opName = "IFGT"; break;
                                case Opcodes.IFGE:
                                    operator = ">="; opName = "IFGE"; break;
                                case Opcodes.IFEQ:
                                    operator = "=="; opName = "IFEQ"; break;
                                case Opcodes.IFNULL:
                                    operator = "=="; opName = "IFNULL"; break;
                                case Opcodes.IFNONNULL:
                                    operator = "!="; opName = "IFNONNULL"; break;
                                default :
                                    throw new RuntimeException("Invalid operator during optimization of integer comparison");
                            }
                                    
                            
                            // Fuse LCMP+IFxx into a direct long comparison (a <op> b)
                            // instead of CN1_CMP_EXPR(a,b) <op> 0, which clang can
                            // actually analyze/vectorize. Long only -- float/double
                            // keep CN1_CMP_EXPR for NaN-correct ordering.
                            String directLongCmp = (leftArg instanceof ArithmeticExpression)
                                    ? ((ArithmeticExpression) leftArg).getLongCompareDirect(operator) : null;
                            // Same fusion for float/double compares (NaN-correct); folds
                            // FCMPx/DCMPx + IFxx into a single IEEE comparison instead of
                            // the three-way CN1_CMP_EXPR that blocks clang's FP optimisation.
                            boolean directFloatCmp = false;
                            if (directLongCmp == null && leftArg instanceof ArithmeticExpression) {
                                directLongCmp = ((ArithmeticExpression) leftArg).getFloatCompareDirect(operator);
                                directFloatCmp = directLongCmp != null;
                            }
                            // Diverging-check prelude for a fused array-load operand
                            // (frameless only) -- see the IF_ICMP arm above.
                            String arrayCmpPrelude1 = null;
                            if (directLongCmp == null && frameless && leftArg instanceof ArrayLoadExpression) {
                                StringBuilder t = new StringBuilder();
                                if (((ArrayLoadExpression) leftArg).emitDiverging("__cn1AjL",
                                        returnType.isVoid() ? "" : "0", t)) {
                                    arrayCmpPrelude1 = t.toString();
                                    leftLiteral = "__cn1AjL";
                                }
                            }
                            if (directLongCmp != null) {
                                // FP conditional guards (e.g. an `if(acc>1e12)acc-=1e12`
                                // normalization) get if-converted by clang into an fcsel, which drags
                                // the almost-never-taken body onto the FP recurrence's critical path
                                // EVERY iteration -- making ParparVM's tight FP loops slower than even
                                // HotSpot (whose JIT profiles the guard as not-taken and keeps a real
                                // branch). __builtin_expect biases clang to the same predicted branch,
                                // so the guard stays off the recurrence. The transpiler emits the jump
                                // as "skip/continue" (the common, taken direction), so expect(...,1)
                                // matches. Correctness is unaffected (identical results either way).
                                String fpCond = directFloatCmp
                                        ? "__builtin_expect(" + directLongCmp + ", 1)"
                                        : directLongCmp;
                                sb.append("if (").append(fpCond).append(") /* ").append(opName).append(" CustomJump LCMP */ ");
                            } else {
                                if (arrayCmpPrelude1 != null) {
                                    sb.append("{\n    ").append(arrayCmpPrelude1);
                                }
                                sb.append("if (").append(leftLiteral).append(operator).append(rightArg).append(") /* ").append(opName).append(" CustomJump */ ");
                            }
                            CustomJump newJump = CustomJump.create(jmp, sb.toString());
                            //jmp.setCustomCompareCode(sb.toString());
                            if (arrayCmpPrelude1 != null) {
                                newJump.setCustomSuffix("    }\n");
                            }
                            newJump.setOptimized(true);
                            instructions.add(iter, newJump);
                            instructionCount = instructions.size();
                            
                        }
                        
                    }
                break;
                }   
                   
                
                
                
                
                case Opcodes.INVOKEVIRTUAL:
                case Opcodes.INVOKESTATIC:
                case Opcodes.INVOKESPECIAL:
                case Opcodes.INVOKEINTERFACE: {
                    if (current instanceof Invoke) {
                        Invoke inv = (Invoke)current;
                        // AOT trivial accessor inlining: a monomorphic call to a
                        // `return this.field` getter / `this.field = arg` setter has the
                        // exact stack effect of a GETFIELD / PUTFIELD, so swap the call
                        // for the field op. Reprocess the slot so Field.tryReduce above
                        // folds the operands into a direct field expression.
                        Field inlinedField = inv.asInlinableFieldAccess();
                        if (inlinedField != null) {
                            instructions.set(iter, inlinedField);
                            iter--;
                            continue;
                        }
                        List<ByteCodeMethodArg> invocationArgs = inv.getArgs();
                        int numArgs = invocationArgs.size();
                        
                        //if (current.getOpcode() != Opcodes.INVOKESTATIC) {
                        //    numArgs++;
                        //}
                        if (iter >= numArgs) {
                            String[] argLiterals = new String[numArgs];
                            StringBuilder devNull = new StringBuilder();
                            for (int i=0; i<numArgs; i++) {
                                devNull.setLength(0);
                                Instruction instr = instructions.get(iter-numArgs+i);
                                if (instr instanceof AssignableExpression && ((AssignableExpression)instr).assignTo(null, devNull)) {
                                    argLiterals[i] = devNull.toString().trim();
                                } else if (instr instanceof CustomInvoke) {
                                    CustomInvoke cinv = (CustomInvoke)instr;
                                    devNull.setLength(0);
                                    if (!"JAVA_OBJECT".equals(cinv.getReturnValue()) && cinv.appendExpression(devNull)) {
                                        // We can't add invocations that return objects directly
                                        // because they need to be added to the stack for GC
                                        argLiterals[i] = devNull.toString().trim();
                                    }
                                } else if (instr instanceof ArithmeticExpression) {
                                    argLiterals[i] = ((ArithmeticExpression)instr).getExpressionAsString().trim();
                                } else if (instr instanceof VarOp) {
                                    VarOp var = (VarOp)instr;
                                    switch (instr.getOpcode()) {
                                        case Opcodes.ALOAD: {
                                            if (!isStatic() && var.getIndex() == 0) {
                                                argLiterals[i] = "__cn1ThisObject";
                                            } else {
                                                argLiterals[i] = "locals["+var.getIndex()+"].data.o";
                                            }
                                            break;
                                        }
                                        case Opcodes.ILOAD: {
                                            argLiterals[i] = "ilocals_"+var.getIndex()+"_";
                                            break;
                                        }
                                        case Opcodes.ACONST_NULL: {
                                            argLiterals[i] = "JAVA_NULL";
                                            break;
                                        }
                                        case Opcodes.DLOAD: {
                                            argLiterals[i] = "dlocals_"+var.getIndex()+"_";
                                            break;
                                        }
                                        case Opcodes.FLOAD: {
                                            argLiterals[i] = "flocals_"+var.getIndex()+"_";
                                            break;
                                        }
                                        case Opcodes.LLOAD: {
                                            argLiterals[i] = "llocals_"+var.getIndex()+"_";
                                            break;
                                        }
                                        case Opcodes.ICONST_0: {
                                            argLiterals[i] = "0";
                                            break;
                                        }
                                        case Opcodes.ICONST_1: {
                                            argLiterals[i] = "1";
                                            break;
                                        }
                                        case Opcodes.ICONST_2: {
                                            argLiterals[i] = "2";
                                            break;
                                        }
                                        case Opcodes.ICONST_3: {
                                            argLiterals[i] = "3";
                                            break;
                                        }
                                        case Opcodes.ICONST_4: {
                                            argLiterals[i] = "4";
                                            break;
                                        }
                                        case Opcodes.ICONST_5: {
                                            argLiterals[i] = "5";
                                            break;
                                        }
                                        case Opcodes.ICONST_M1: {
                                            argLiterals[i] = "-1";
                                            break;
                                        }
                                        case Opcodes.LCONST_0: {
                                            argLiterals[i] = "(JAVA_LONG)0";
                                            break;
                                        }
                                        case Opcodes.LCONST_1: {
                                            argLiterals[i] = "(JAVA_LONG)1";
                                            break;
                                        }
                                        case Opcodes.BIPUSH: 
                                        case Opcodes.SIPUSH: {
                                            argLiterals[i] = String.valueOf(var.getIndex());
                                            
                                            break;
                                        }
                                    }
                                } else {
                                    switch (instr.getOpcode()) {
                                        
                                        case Opcodes.ACONST_NULL: {
                                            argLiterals[i] = "JAVA_NULL";
                                            break;
                                        }
                                        
                                        case Opcodes.ICONST_0: {
                                            argLiterals[i] = "0";
                                            break;
                                        }
                                        case Opcodes.ICONST_1: {
                                            argLiterals[i] = "1";
                                            break;
                                        }
                                        case Opcodes.ICONST_2: {
                                            argLiterals[i] = "2";
                                            break;
                                        }
                                        case Opcodes.ICONST_3: {
                                            argLiterals[i] = "3";
                                            break;
                                        }
                                        case Opcodes.ICONST_4: {
                                            argLiterals[i] = "4";
                                            break;
                                        }
                                        case Opcodes.ICONST_5: {
                                            argLiterals[i] = "5";
                                            break;
                                        }
                                        case Opcodes.ICONST_M1: {
                                            argLiterals[i] = "-1";
                                            break;
                                        }
                                        case Opcodes.LCONST_0: {
                                            argLiterals[i] = "(JAVA_LONG)0";
                                            break;
                                        }
                                        case Opcodes.LCONST_1: {
                                            argLiterals[i] = "(JAVA_LONG)1";
                                            break;
                                        }
                                        case Opcodes.BIPUSH: {
                                            if (instr instanceof BasicInstruction) {
                                                argLiterals[i] = String.valueOf(((BasicInstruction)instr).getValue());
                                            }
                                            break;
                                        }
                                        case Opcodes.LDC : {
                                            if (instr instanceof Ldc) {
                                                Ldc ldc = (Ldc)instr;
                                                argLiterals[i] = ldc.getValueAsString();
                                                
                                            }
                                            break;
                                        }


                                    }
                                    
                                }
                            }
                            
                            
                            // Check to make sure that we have all the args as literals.
                            boolean missingLiteral = false;
                            for (String lit : argLiterals) {
                                if (lit == null) {
                                    missingLiteral = true;
                                    break;
                                }
                            }
                            
                            // We have all of the arguments as literals.  Let's
                            // add them to our invoke instruction.
                            if (!missingLiteral) {
                                CustomInvoke newInvoke = CustomInvoke.create(inv);
                                instructions.remove(iter);
                                instructions.add(iter, newInvoke);
                                int newIter = iter;
                                for (int i=0; i< numArgs; i++) {
                                    instructions.remove(iter-numArgs);
                                    newIter--;
                                    newInvoke.setLiteralArg(i, argLiterals[i]);
                                }
                                if (inv.getOpcode() != Opcodes.INVOKESTATIC) {
                                    Instruction ldTarget = instructions.get(iter-numArgs-1);
                                    if (ldTarget instanceof AssignableExpression) {
                                        StringBuilder targetExprStr = new StringBuilder();
                                        if (((AssignableExpression)ldTarget).assignTo(null, targetExprStr)) {
                                            newInvoke.setTargetObjectLiteral(targetExprStr.toString().trim());
                                            instructions.remove(iter-numArgs-1);
                                            newIter--;
                                            
                                        }
                                        
                                    } else if (ldTarget instanceof CustomInvoke) {
                                        // WE Can't pass a custom invoke as the target directly
                                        // because it the return value needs to be added to the 
                                        // stack for the GC
                                    } else {
                                        switch (ldTarget.getOpcode()) {
                                            case Opcodes.ALOAD: {
                                                VarOp v = (VarOp)ldTarget;
                                                if (isStatic() && v.getIndex() == 0) {
                                                    newInvoke.setTargetObjectLiteral("__cn1ThisObject");
                                                } else {
                                                    newInvoke.setTargetObjectLiteral("locals["+v.getIndex()+"].data.o");
                                                }
                                                instructions.remove(iter-numArgs-1);
                                                newIter--;
                                                break;
                                            }
                                        }
                                    }
                                }
                                
                                newInvoke.setOptimized(true);
                                //iter = 0;
                                instructionCount = instructions.size();
                                iter = newIter;
                                
                                
                            }
                        }
                    }
                    break;
                }
                    
                
            }
            astoreCalls = astoreCalls || currentOpcode == Opcodes.ASTORE || currentOpcode == Opcodes.ISTORE || 
                    currentOpcode == Opcodes.LSTORE || currentOpcode == Opcodes.DSTORE || currentOpcode == Opcodes.FSTORE;
            
            hasInstructions = hasInstructions | current.getOpcode() != -1;
        }
        return hasInstructions;
    }

    private boolean constReturn(int type, int value, int nextOpcode, int iter) {
        if(nextOpcode == type) {
            instructions.remove(iter);
            instructions.remove(iter);
            if(synchronizedMethod && instructions.size() > 0) {
                if(staticMethod) {
                    instructions.add(iter, new CustomIntruction("    monitorExit(threadStateData, (JAVA_OBJECT)&class__" + clsName + ");\n" +
                            "    return " + value + ";\n",
                            "    monitorExit(threadStateData, (JAVA_OBJECT)&class__" + clsName + ");\n" +
                            "    RETURN_AND_RELEASE_FROM_METHOD(" + value + ", " + maxLocals + ");\n", dependentClasses));
                } else {
                    instructions.add(iter, new CustomIntruction("    monitorExit(threadStateData, __cn1ThisObject);\n" +
                            "    return " + value + ";\n",
                            "    monitorExit(threadStateData, __cn1ThisObject);\n" +
                            "    RETURN_AND_RELEASE_FROM_METHOD(" + value + ", " + maxLocals + ");\n", dependentClasses));
                }
            } else {
                instructions.add(iter, new CustomIntruction("    return " + value + ";\n",
                        "    RETURN_AND_RELEASE_FROM_METHOD(" + value + ", " + maxLocals + ");\n", dependentClasses));
            }
            return true;
        }
        return false;
    }
    
    private int localsOffsetToArgOffset(int offset) {
        int localsOffset = 0;
        if(!staticMethod) {
            localsOffset++;
        }
        for(int iter = 0 ; iter < arguments.size() ; iter++) {
            ByteCodeMethodArg arg = arguments.get(iter);
            if(localsOffset == offset) {
                return iter + 1;
            }
            localsOffset++;
            if(arg.isDoubleOrLong()) {
                localsOffset++;
            }
        }
        return -1;
    }

    // support for the SignatureSet interface
	public boolean containsSignature(SignatureSet sig) {
		return desc.equals(sig.getSignature());
	}
	public String getSignature() {
		return desc;
	}

    @Override
    public SignatureSet nextSignature() {
        return null;
    }

    public String getJsBodyScript() {
        return jsBodyScript;
    }

    public void setJsBodyScript(String jsBodyScript) {
        this.jsBodyScript = jsBodyScript;
    }

    public String[] getJsBodyParams() {
        return jsBodyParams;
    }

    public void setJsBodyParams(String[] jsBodyParams) {
        this.jsBodyParams = jsBodyParams;
    }

    public boolean isJsBodyMethod() {
        return jsBodyScript != null;
    }


}
