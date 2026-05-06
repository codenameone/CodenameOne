package com.codename1.tools.translator;

import com.codename1.tools.translator.bytecodes.BasicInstruction;
import com.codename1.tools.translator.bytecodes.Field;
import com.codename1.tools.translator.bytecodes.IInc;
import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.Invoke;
import com.codename1.tools.translator.bytecodes.Jump;
import com.codename1.tools.translator.bytecodes.LabelInstruction;
import com.codename1.tools.translator.bytecodes.Ldc;
import com.codename1.tools.translator.bytecodes.LineNumber;
import com.codename1.tools.translator.bytecodes.LocalVariable;
import com.codename1.tools.translator.bytecodes.MultiArray;
import com.codename1.tools.translator.bytecodes.SwitchInstruction;
import com.codename1.tools.translator.bytecodes.TryCatch;
import com.codename1.tools.translator.bytecodes.TypeInstruction;
import com.codename1.tools.translator.bytecodes.VarOp;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

final class JavascriptMethodGenerator {
    // Global class-name to ByteCodeClass index, used by appendFieldInstruction
    // to resolve a getfield/putfield instruction's class reference (the
    // "current receiver type" from the bytecode's Fieldref) to the actual
    // class that declares the field. Java bytecode allows the reference
    // to name any accessible class on the receiver's hierarchy — the JVM
    // resolves it at link time by walking up from there — but the
    // translator was emitting the unresolved owner as the property
    // prefix, producing ``target["cn1_<subclass>_<field>"]`` reads on
    // fields declared on an ancestor. Under the identifier mangler those
    // two prefixes pick up unrelated mangled forms, so the runtime
    // property read misses (returns undefined) and the next field access
    // blows up with "Cannot read properties of undefined".
    //
    // We rebuild this map at the start of every bundle generation so
    // resolution sees the fully loaded class graph, not a partial view.
    private static volatile Map<String, ByteCodeClass> classIndex = null;
    // Set of ``className + '\0' + fieldName`` pairs that any reachable
    // method references via GETSTATIC or PUTSTATIC. Populated by
    // ``setClassIndex`` (after the RTA / suspension analyses are done)
    // and consulted by the class-def emitter to elide static field
    // entries that nobody ever reads. Typical win: the FontImage
    // material-icon constant table (~2.2k entries, 60 KiB) where
    // Initializr only touches a handful of icons.
    private static volatile java.util.Set<String> referencedStaticFields = null;
    // Instance-field counterpart to ``referencedStaticFields``: set
    // of ``className + '\0' + fieldName`` pairs reached by a
    // GETFIELD / PUTFIELD somewhere in the bundle. Consulted by the
    // instance-field emitter (``f:[...]``) to skip entries no code
    // reads or writes.
    private static volatile java.util.Set<String> referencedInstanceFields = null;
    // Dispatch IDs referenced by at least one INVOKEVIRTUAL /
    // INVOKEINTERFACE somewhere in the reachable code. Methods
    // whose (name+sig) doesn't appear here don't need a
    // methods-map entry — they're invoked only via
    // INVOKESPECIAL / INVOKESTATIC direct calls, which use the
    // class-specific function identifier at the call site, not
    // the class's ``methods`` table.
    private static volatile java.util.Set<String> referencedDispatchIds = null;
    // The class whose method is currently being emitted. Used by
    // ``appendInterpreterEnsureClassInitialized`` to elide
    // ``_I("X")`` when ``X`` is the containing class or one of
    // its ancestors — those are guaranteed to be already initialized
    // by the JVM spec before any method on ``currentEmissionClass``
    // runs. Set at the start of each ``appendMethod`` call and cleared
    // at the end.
    private static ByteCodeClass currentEmissionClass = null;
    // Name of the clinit function for the class currently being
    // emitted, or ``null`` if this class has no clinit. Captured at
    // method-emission time and consumed by the subsequent
    // ``_Z({...})`` emission so the clinit is attached via a ``c:``
    // property on the class def instead of a separate post-Z
    // ``jvm.classes["cls"].clinit = $fn`` statement (which used to
    // run before the class def with the new method-first emission
    // order, causing a "Cannot set properties of undefined"
    // TypeError).
    private static String currentClassClinitFn = null;

    private JavascriptMethodGenerator() {
    }

    static void setClassIndex(List<ByteCodeClass> allClasses) {
        if (allClasses == null) {
            classIndex = null;
            referencedStaticFields = null;
            return;
        }
        HashMap<String, ByteCodeClass> index = new HashMap<String, ByteCodeClass>();
        for (ByteCodeClass c : allClasses) {
            if (c != null && c.getClsName() != null) {
                index.put(c.getClsName(), c);
            }
        }
        classIndex = index;
        // Scan every reachable method's bytecode for field ops and
        // record the (owner, fieldName) pairs each one touches. The
        // class-def emitter consults the resulting sets to omit
        // static / instance field entries nobody references. A
        // field that's only WRITTEN (by its declaring ``<clinit>``
        // or a constructor) but never READ is still considered
        // referenced — the write itself is retained, and ripping
        // the field out of the metadata would break that assignment
        // at runtime.
        //
        // Instance fields get a small walk-up-the-hierarchy step
        // too: a subclass access ``GETFIELD <subclass>.field``
        // resolves to the field's declaring ancestor. Instead of
        // doing the resolve here at collect time (which would
        // duplicate logic from ``resolveFieldOwner``), we populate
        // the set with the raw declared owner AND every ancestor
        // that has a field by the same name. Over-approximation is
        // safe: we never accidentally drop a referenced field.
        java.util.Set<String> fieldRefs = new java.util.HashSet<String>();
        java.util.Set<String> instanceRefs = new java.util.HashSet<String>();
        java.util.Set<String> dispatchRefs = new java.util.HashSet<String>();
        for (ByteCodeClass c : allClasses) {
            if (c == null) continue;
            for (BytecodeMethod m : c.getMethods()) {
                if (m == null || m.isEliminated()) continue;
                List<Instruction> insns = m.getInstructions();
                if (insns == null) continue;
                for (Instruction instr : insns) {
                    if (instr instanceof com.codename1.tools.translator.bytecodes.Invoke) {
                        int op = instr.getOpcode();
                        if (op == Opcodes.INVOKEVIRTUAL || op == Opcodes.INVOKEINTERFACE) {
                            com.codename1.tools.translator.bytecodes.Invoke inv =
                                    (com.codename1.tools.translator.bytecodes.Invoke) instr;
                            dispatchRefs.add(JavascriptNameUtil.dispatchMethodIdentifier(inv.getName(), inv.getDesc()));
                        }
                    }
                    if (instr instanceof com.codename1.tools.translator.bytecodes.Field) {
                        int op = instr.getOpcode();
                        com.codename1.tools.translator.bytecodes.Field f =
                                (com.codename1.tools.translator.bytecodes.Field) instr;
                        String owner = JavascriptNameUtil.sanitizeClassName(f.getOwner());
                        String name = f.getFieldName();
                        if (op == Opcodes.GETSTATIC || op == Opcodes.PUTSTATIC) {
                            fieldRefs.add(owner + "\0" + name);
                        } else if (op == Opcodes.GETFIELD || op == Opcodes.PUTFIELD) {
                            // Walk declared owner + every superclass /
                            // interface that declares a field by this
                            // name. Keeps the reference alive on the
                            // actual declaring class even when the
                            // access uses a subclass owner.
                            String current = owner;
                            while (current != null) {
                                instanceRefs.add(current + "\0" + name);
                                ByteCodeClass currentCls = index.get(current);
                                if (currentCls == null) break;
                                String base = currentCls.getBaseClass();
                                current = base == null ? null : JavascriptNameUtil.sanitizeClassName(base);
                            }
                        }
                    }
                }
            }
        }
        // JSO bridge methods are dispatched from the host (JS) at
        // runtime, NOT via INVOKEVIRTUAL / INVOKEINTERFACE in the
        // bundle — the worker yields a host-bridge call, the host
        // looks up the dispatch id on the receiver wrapper's m: map,
        // and round-trips back through ``worker-callback``. The scan
        // above only sees bytecode-visible call sites, so SAM impls
        // like ``LocalForage$1.callback`` would be skipped by
        // ``appendPrimaryRegistration`` (which gates the m: entry on
        // ``referencedDispatchIds``) even though RTA un-elimination
        // kept the function body alive. Without the m: entry the
        // host-side dispatch lookup misses and the calling Java
        // thread deadlocks on the corresponding wait/notify pair.
        // Tag every method on a JSO bridge type as referenced so the
        // entry survives.
        for (ByteCodeClass c : allClasses) {
            if (c == null || !isJsoBridgeType(c, index)) continue;
            for (BytecodeMethod m : c.getMethods()) {
                if (m == null || m.isStatic()) continue;
                String name = m.getMethodName();
                String desc = m.getSignature();
                if (name == null || desc == null) continue;
                if ("__INIT__".equals(name) || "__CLINIT__".equals(name)) continue;
                dispatchRefs.add(JavascriptNameUtil.dispatchMethodIdentifier(name, desc));
            }
        }
        referencedStaticFields = fieldRefs;
        referencedInstanceFields = instanceRefs;
        referencedDispatchIds = dispatchRefs;
    }

    /**
     * True if {@code cls}'s ancestry contains
     * ``com_codename1_html5_js_JSObject`` (transitively via
     * baseClass / interfaces). Mirrors the same walk
     * ``JavascriptReachability.isJsoBridgeType`` /
     * ``JavascriptBundleWriter.isJsoBridgeClass`` use; here we
     * consult the local class index so we can flag every method on
     * a JSO bridge type as runtime-referenced (the host invokes them
     * via the JSO bridge, not via bytecode visible to the
     * INVOKEVIRTUAL / INVOKEINTERFACE scan above).
     */
    private static boolean isJsoBridgeType(ByteCodeClass cls, Map<String, ByteCodeClass> idx) {
        java.util.Set<String> seen = new java.util.HashSet<String>();
        java.util.Deque<ByteCodeClass> stack = new java.util.ArrayDeque<ByteCodeClass>();
        stack.push(cls);
        while (!stack.isEmpty()) {
            ByteCodeClass current = stack.pop();
            if (current == null || !seen.add(current.getClsName())) continue;
            if ("com_codename1_html5_js_JSObject".equals(current.getClsName())) return true;
            String base = current.getBaseClass();
            if (base != null) {
                ByteCodeClass baseObj = idx.get(JavascriptNameUtil.sanitizeClassName(base));
                if (baseObj != null) stack.push(baseObj);
            }
            List<String> ifaces = current.getBaseInterfaces();
            if (ifaces != null) {
                for (String iface : ifaces) {
                    ByteCodeClass ifaceObj = idx.get(JavascriptNameUtil.sanitizeClassName(iface));
                    if (ifaceObj != null) stack.push(ifaceObj);
                }
            }
        }
        return false;
    }

    /**
     * Walk up the class hierarchy rooted at the declared owner and
     * return the first non-abstract, non-eliminated method matching
     * the invoke's name + descriptor. Used by the emitter to decide
     * whether a direct (invokestatic / invokespecial) call should be
     * wrapped in ``yield*``: if the resolved target is flagged
     * synchronous, the call site emits a plain function call; if it
     * suspends, the call site emits ``yield* target(...)`` and the
     * caller is itself suspending. Returns null for virtual /
     * interface dispatches (resolution is runtime-only) and for
     * unresolvable targets (caller treats those as suspending for
     * safety — matches the conservative default on
     * {@link BytecodeMethod#isJavascriptSuspending}).
     */
    private static BytecodeMethod resolveDirectInvokeTarget(Invoke invoke) {
        int op = invoke.getOpcode();
        if (op != Opcodes.INVOKESTATIC && op != Opcodes.INVOKESPECIAL) {
            return null;
        }
        Map<String, ByteCodeClass> idx = classIndex;
        if (idx == null || invoke.getOwner() == null) {
            return null;
        }
        String name = invoke.getName();
        String normalizedName;
        if ("<init>".equals(name)) {
            normalizedName = "__INIT__";
        } else if ("<clinit>".equals(name)) {
            normalizedName = "__CLINIT__";
        } else {
            normalizedName = name;
        }
        String desc = invoke.getDesc();
        String current = JavascriptNameUtil.sanitizeClassName(invoke.getOwner());
        java.util.HashSet<String> visited = new java.util.HashSet<String>();
        while (current != null && visited.add(current)) {
            ByteCodeClass cls = idx.get(current);
            if (cls == null) {
                return null;
            }
            for (BytecodeMethod m : cls.getMethods()) {
                if (m.isEliminated() || m.isAbstract()) {
                    continue;
                }
                if (normalizedName.equals(m.getMethodName()) && desc.equals(m.getSignature())) {
                    return m;
                }
            }
            String base = cls.getBaseClass();
            current = base == null ? null : JavascriptNameUtil.sanitizeClassName(base);
        }
        return null;
    }

    /**
     * True when the given invoke's callee is (or must be conservatively
     * treated as) suspending. Virtual / interface dispatches go through
     * {@code cn1_iv*} which is a generator, so they are always
     * suspending from the emitter's perspective. Unresolved direct
     * dispatches default to suspending as a safety net — the
     * {@link BytecodeMethod#isJavascriptSuspending} flag itself
     * defaults to {@code true} for the same reason.
     */
    private static boolean isInvokeSuspending(Invoke invoke) {
        int op = invoke.getOpcode();
        if (op == Opcodes.INVOKEVIRTUAL || op == Opcodes.INVOKEINTERFACE) {
            // CHA result: the signature is sync only if NO class's
            // impl is suspending. Consult the set exported by the
            // suspension analysis. Default (no set → all dispatches
            // suspending) preserves the historical over-conservative
            // behaviour when the analysis is disabled.
            java.util.Set<String> suspendingSigs = JavascriptSuspensionAnalysis.exportedSuspendingSigs;
            if (suspendingSigs == null) {
                return true;
            }
            String sig = invoke.getName() + invoke.getDesc();
            return suspendingSigs.contains(sig);
        }
        BytecodeMethod target = resolveDirectInvokeTarget(invoke);
        return target == null || target.isJavascriptSuspending();
    }

    private static String resolveFieldOwner(String owner, String fieldName) {
        Map<String, ByteCodeClass> idx = classIndex;
        if (idx == null || owner == null || fieldName == null) {
            return owner;
        }
        // ByteCodeClass stores names in the translator's sanitized form
        // (underscored), but callers may hand us either the sanitized
        // name or the raw JVM-style ``java/util/HashMap``. Normalise to
        // the sanitized form before every lookup, and also apply the
        // same normalisation when following ``getBaseClass()`` — it
        // returns the JVM-style reference the class reader saw.
        String current = JavascriptNameUtil.sanitizeClassName(owner);
        while (current != null) {
            ByteCodeClass cls = idx.get(current);
            if (cls == null) {
                return current;
            }
            for (ByteCodeField f : cls.getFields()) {
                if (!f.isStaticField() && fieldName.equals(f.getFieldName())) {
                    return current;
                }
            }
            String base = cls.getBaseClass();
            current = base == null ? null : JavascriptNameUtil.sanitizeClassName(base);
        }
        return JavascriptNameUtil.sanitizeClassName(owner);
    }

    static String generateClassJavascript(ByteCodeClass cls, List<ByteCodeClass> allClasses) {
        // Populate the resolution index lazily on first call and keep it
        // alive for the rest of the generation pass. The size check is
        // not enough to detect a fresh translator run: when test
        // harnesses (Parser.cleanup() between fixtures) feed the same
        // total class count from a different ByteCodeClass instance
        // set, the stale index points at the previous run's classes —
        // which can be missing methods that only the new instances
        // have (e.g. javac 17+ enum ``$values()`` synthetics emitted by
        // the second compile but not the first). Rebuild whenever the
        // index doesn't already contain the EXACT class instance we're
        // about to emit, which catches both "first time" and "swapped
        // list" without iterating the full list on every call.
        if (classIndex == null || classIndex.size() != (allClasses == null ? 0 : allClasses.size())
                || (cls != null && classIndex.get(cls.getClsName()) != cls)) {
            setClassIndex(allClasses);
        }
        StringBuilder out = new StringBuilder();
        // Collects virtual-method registrations (primary + aliases) so the
        // whole class can be attached via a single ``_M("cls",{...})``
        // call at the end. Per-method ``jvm.addVirtualMethod(...)`` emits
        // were previously 62% of the bundle — ~190k call sites at ~90
        // bytes each. Batching drops each entry to ``$methodId,`` (5
        // bytes via ES2015 property shorthand) or ``$ancestorId:$fn,``
        // (~12 bytes for ancestor aliases).
        StringBuilder regs = new StringBuilder();
        StringBuilder methodsOut = new StringBuilder();
        // Emit function declarations FIRST (they hoist), then the
        // ``_Z({...})`` class def with the methods map attached
        // inline. Saves the ``,_M("cls",...)`` separate-call
        // boilerplate per class. The class def also carries the
        // clinit function name (``c:$fn``) so the old inline
        // ``jvm.classes["cls"].clinit = $fn`` assignment — which
        // ran between emission and ``_Z`` — is no longer needed.
        currentClassClinitFn = null;
        for (BytecodeMethod method : cls.getMethods()) {
            if (method.isNative() || method.isAbstract() || method.isEliminated()) {
                continue;
            }
            appendMethod(methodsOut, regs, cls, method);
        }
        for (BytecodeMethod method : cls.getMethods()) {
            if (!method.isNative() || method.isEliminated()) {
                continue;
            }
            appendNativeStubIfNeeded(methodsOut, cls, method);
            if (!method.isStatic() && !method.isConstructor()) {
                String jsMethodName = jsMethodIdentifier(cls, method);
                String dispatchId = JavascriptNameUtil.dispatchMethodIdentifier(method.getMethodName(), method.getSignature());
                appendPrimaryRegistration(regs, dispatchId, jsMethodName);
            }
        }
        appendSyntheticClinitIfNeeded(methodsOut, cls);

        out.append("// ").append(cls.getClsName()).append("\n");
        out.append(methodsOut);
        appendClassRegistration(out, cls, allClasses, regs, currentClassClinitFn);
        currentClassClinitFn = null;
        return out.toString();
    }

    /**
     * Append an object-literal entry for one of this class's declared
     * methods. The map key is a class-free ``dispatchId`` so every class
     * that implements the same Java method stores under the same key —
     * this lets ``resolveVirtual``'s inheritance walk resolve inherited
     * methods directly on the ancestor's table without the child class
     * needing to re-register them. The value is the per-class function
     * identifier. Both sides mangle independently but in lockstep with
     * call sites.
     */
    private static void appendPrimaryRegistration(StringBuilder regs, String dispatchId, String functionId) {
        // Virtual-dispatch RTA: if no reachable INVOKEVIRTUAL /
        // INVOKEINTERFACE in the bundle resolves to this
        // dispatchId, skip the entry. The method's body is still
        // emitted (INVOKESPECIAL / INVOKESTATIC call sites use
        // ``functionId`` directly), but it doesn't need a slot in
        // the virtual-dispatch table. ~50% of Initializr's methods
        // fit this bucket (private / package-private helpers that
        // never participate in virtual dispatch).
        java.util.Set<String> dispatchRefs = referencedDispatchIds;
        if (dispatchRefs != null && !dispatchRefs.contains(dispatchId)) {
            return;
        }
        if (regs.length() > 0) {
            regs.append(',');
        }
        regs.append(dispatchId).append(':').append(functionId);
    }

    /**
     * Append an object-literal entry that points an ancestor method id
     * (or any id that differs from the backing function's identifier)
     * at a specific function. Emits ``$ancestorId:$implFn`` — the
     * impl is a bareword reference, which works because we wrap the
     * entire methods object in a deferred thunk (see
     * ``flushRegistrations``). The thunk isn't evaluated until first
     * virtual dispatch, by which time every translated_app_N.js chunk
     * has loaded and all ``function*`` declarations are attached to
     * globalThis.
     */
    private static void appendAliasRegistration(StringBuilder regs, String methodId, String implMethodId) {
        if (regs.length() > 0) {
            regs.append(',');
        }
        regs.append(methodId).append(':').append(implMethodId);
    }

    private static void flushRegistrations(StringBuilder out, ByteCodeClass cls, StringBuilder regs) {
        if (regs.length() == 0) {
            return;
        }
        // Emit the methods object directly. The historical ``()=>(...)``
        // thunk deferred evaluation to the first virtual dispatch so
        // forward references to functions declared in LATER chunks
        // still resolved. With the post-RTA Initializr bundle now
        // fitting in a single ``translated_app.js`` file, every
        // referenced function is hoisted onto the worker globalThis
        // before ``_M`` runs at top level — no thunk needed. Each
        // saved thunk is ``()=>(`` + ``)`` = 5 chars × ~600 classes.
        // Kill-switch ``parparvm.js.mthunk.keep`` restores the thunk
        // form for debugging.
        if (System.getProperty("parparvm.js.mthunk.keep") != null) {
            out.append("_M(\"").append(cls.getClsName()).append("\",()=>({").append(regs).append("}));\n");
        } else {
            out.append("_M(\"").append(cls.getClsName()).append("\",{").append(regs).append("});\n");
        }
    }

    private static void appendClassRegistration(StringBuilder out, ByteCodeClass cls, List<ByteCodeClass> allClasses) {
        appendClassRegistration(out, cls, allClasses, null, null);
    }

    private static void appendClassRegistration(StringBuilder out, ByteCodeClass cls, List<ByteCodeClass> allClasses, StringBuilder methodsMap, String clinitFn) {
        // Property names are the single-char short forms the runtime
        // reads: n=name, b=baseClass, i=interfaces, A=isAbstract, I=isInterface,
        // a=assignableTo, f=instanceFields, s=staticFields. Each class
        // was previously ~60 chars of property-name overhead (``name:``,
        // ``baseClass:``, ``interfaces:``, ``assignableTo:``,
        // ``instanceFields:``, ``staticFields:``); collapsing to
        // single chars saves ~60 chars × 1590 classes ≈ 95 KiB.
        out.append("_Z({\n");
        // Full class name goes into ``n:`` — runtime uses this for
        // both ``def.name`` and the auto-populate of ``assignableTo``.
        out.append("  n: \"").append(cls.getClsName()).append("\",\n");
        // ``b:`` omitted entirely when the class directly extends
        // ``java_lang_Object`` — runtime defaults to Object. Saves
        // 7-8 chars × ~1200 leaf classes. ``b:null`` stays explicit
        // for the Object class itself (so defineClass knows not to
        // chase a parent).
        String baseClass = cls.getBaseClass();
        if (baseClass == null) {
            out.append("  b: null,\n");
        } else if (!"java_lang_Object".equals(JavascriptNameUtil.sanitizeClassName(baseClass))) {
            out.append("  b: \"").append(JavascriptNameUtil.sanitizeClassName(baseClass)).append("\",\n");
        }
        // else: base is Object, emit nothing; runtime defaults to it.
        boolean first;
        if (!cls.getBaseInterfaces().isEmpty()) {
            out.append("  i: [");
            first = true;
            for (String iface : cls.getBaseInterfaces()) {
                if (!first) {
                    out.append(", ");
                }
                first = false;
                out.append("\"").append(JavascriptNameUtil.sanitizeClassName(iface)).append("\"");
            }
            out.append("],\n");
        }
        if (cls.isIsInterface()) {
            out.append("  I: 1,\n");
        }
        if (cls.isIsAbstract()) {
            out.append("  A: 1,\n");
        }
        appendAssignableTypes(out, cls, allClasses);
        // Instance fields used to serialize as
        //   {owner:"$X",name:"hour",desc:"$je",prop:"$Ne"}
        // per field — ~50 chars each, 4k fields across Initializr. We
        // now emit a 2-element tuple ``[prop, desc]``. ``owner`` and
        // ``name`` were only consulted by the runtime's fallback
        // ``prop || fieldProperty(owner,name)``; that fallback is
        // dead because the emitter always writes a concrete ``prop``.
        // Omit ``instanceFields`` / ``staticFields`` when empty. The
        // runtime's ``defineClass`` defaults a missing ``instanceFields``
        // to ``[]`` and missing ``staticFields`` to ``{}``. Leaf-only
        // classes and interfaces often have neither, so omitting
        // these saves ~30 chars per such class.
        boolean hasInstanceField = false;
        boolean hasStaticField = false;
        for (ByteCodeField field : cls.getFields()) {
            if (field.isStaticField()) hasStaticField = true;
            else hasInstanceField = true;
        }
        if (hasInstanceField) {
            // Field-level RTA for instance fields: skip entries no
            // reachable code reads or writes. Missing fields cause
            // GETFIELD-on-fresh-object to see ``undefined`` instead
            // of the Java default (0 / null); the RTA covers only
            // fields with ZERO references, so the missing default
            // can never be observed.
            //
            // Packed encoding: ``f: "$a|$b:I|$c"`` — fields separated
            // by ``|``, primitive type descriptors tacked onto the
            // field name with ``:`` (reference fields drop the
            // suffix entirely). Runtime ``initInstanceFields``
            // splits on ``|`` / ``:`` — same number of branches, but
            // ~14 KiB shorter on the wire compared to the prior
            // ``[["$a"],["$b","I"],["$c"]]`` tuple-array form.
            java.util.Set<String> refs = referencedInstanceFields;
            StringBuilder packed = new StringBuilder();
            boolean anyEmitted = false;
            for (ByteCodeField field : cls.getFields()) {
                if (field.isStaticField()) {
                    continue;
                }
                if (refs != null
                        && !refs.contains(cls.getClsName() + "\0" + field.getFieldName())) {
                    continue;
                }
                if (anyEmitted) {
                    packed.append('|');
                }
                anyEmitted = true;
                String desc = field.getRuntimeDescriptor();
                packed.append(JavascriptNameUtil.fieldProperty(field.getClsName(), field.getFieldName()));
                if (desc != null && !desc.isEmpty() && isPrimitiveDescriptor(desc)) {
                    packed.append(':').append(desc);
                }
            }
            if (anyEmitted) {
                out.append("  f: \"").append(packed).append("\",\n");
            }
        }
        if (hasStaticField) {
            // Track whether any surviving field needs to be emitted;
            // if every static field is unreferenced, skip the ``s``
            // map entirely so we don't ship an empty ``s:{}``.
            java.util.Set<String> refs = referencedStaticFields;
            StringBuilder staticBuf = new StringBuilder();
            boolean anyEmitted = false;
            for (ByteCodeField field : cls.getFields()) {
                if (!field.isStaticField()) {
                    continue;
                }
                // Field-level RTA: if no reachable method performs a
                // GETSTATIC / PUTSTATIC against this (owner, field),
                // nothing can ever observe or mutate its value —
                // skip the entry. Java ``public static final``
                // constants compile to a ``ConstantValue`` attribute
                // plus a JVM-fabricated clinit PUTSTATIC; eliminating
                // both the PUTSTATIC and the map slot for unused
                // constants is safe because no later code reads them.
                // Kept references are everything else: fields that
                // somebody somewhere in the surviving bundle touches.
                if (refs != null
                        && !refs.contains(cls.getClsName() + "\0" + field.getFieldName())) {
                    continue;
                }
                if (anyEmitted) {
                    staticBuf.append(", ");
                }
                anyEmitted = true;
                staticBuf.append("\"").append(field.getFieldName()).append("\": ")
                        .append(renderStaticFieldInitialValue(field));
            }
            if (anyEmitted) {
                out.append("  s: {").append(staticBuf).append("},\n");
            }
        }
        // ``m:`` inline-methods map — consolidates the prior
        // ``_M("cls",{...})`` call into the class def, saving the
        // per-class ``,_M("cls",`` prefix (~6-10 chars × 612 classes
        // ≈ 5 KiB). Runtime ``defineClass`` treats ``def.m`` the
        // same way ``jvm.m`` used to — applies entries via
        // ``applyMethodMap`` at registration time.
        if (methodsMap != null && methodsMap.length() > 0) {
            out.append("  m: {").append(methodsMap).append("},\n");
        }
        // ``c:`` inlines the clinit function reference — used to be
        // a separate ``jvm.classes["cls"].clinit = $fn`` statement.
        // Consolidating it here lets us emit methods first (so their
        // ``function*`` declarations hoist) with the ``_Z({...})``
        // class def following, without the clinit attachment trying
        // to write to a not-yet-registered ``jvm.classes["cls"]``.
        //
        // ``t:`` inlines the no-arg constructor function reference.
        // Without this, the runtime's reflective ``Class.newInstance()``
        // and ``jvm.createException()`` paths build the lookup string
        // as ``"cn1_" + def.name + "___INIT__"`` and read
        // ``global[...]`` — but ``def.name`` is the *mangled* short
        // class symbol (e.g. ``$cm`` for MenuBar) while the actual
        // ``cn1_<class>___INIT__`` global was renamed by the mangler
        // to a different short-form symbol. The lookup never matches
        // anything and ``newInstance`` returns an object whose
        // constructor never ran. That's how every reflectively-created
        // Component (most commonly
        // ``laf.getMenuBarClass().newInstance()`` in
        // ``Form.installMenuBar``) ends up with ``bounds == null`` and
        // trips an NPE the first time pointer-event hit-testing calls
        // ``getX()``. Emit the ctor as a direct function reference so
        // the runtime can store it on the classDef and skip the broken
        // string-concat path.
        BytecodeMethod noArgCtor = findNoArgConstructor(cls);
        boolean hasClinit = (clinitFn != null);
        boolean hasNoArgCtor = (noArgCtor != null);
        if (hasClinit) {
            out.append("  c: ").append(clinitFn);
            if (hasNoArgCtor) {
                out.append(",");
            }
            out.append("\n");
        }
        if (hasNoArgCtor) {
            out.append("  t: ").append(jsMethodIdentifier(cls, noArgCtor)).append("\n");
        }
        // ``methods`` and ``classObject`` are always populated/
        // overwritten by the runtime (defineClass creates the
        // methods map and classObject; _M() adds entries). Emitting
        // the explicit placeholders wastes ~28 chars × 1590 classes.
        out.append("});\n");
    }

    private static void appendAssignableTypes(StringBuilder out, ByteCodeClass cls, List<ByteCodeClass> allClasses) {
        // ``a:{...}`` used to enumerate the full transitive closure
        // of every type this class is assignable to — self, every
        // base class, every interface (plus their bases and parent
        // interfaces). That meant ~40-60 repeating class names per
        // class × 1.5k classes ≈ 62 KiB of duplicated name strings.
        // We now emit ONLY the class's own name; ``defineClass`` on
        // the runtime side unions in the parent / interface sets
        // (both already registered by the time the child's
        // ``defineClass`` runs, since base classes are always
        // emitted before their subclasses).
        //
        // Kill-switch ``parparvm.js.assignableto.full`` restores the
        // historical full emission if a future host calls
        // ``defineClass`` on a class whose ancestors haven't been
        // registered yet.
        if (System.getProperty("parparvm.js.assignableto.full") != null) {
            List<String> assignableTypes = new java.util.ArrayList<String>();
            collectAssignableTypes(cls, allClasses, assignableTypes);
            out.append("  a: {");
            for (int i = 0; i < assignableTypes.size(); i++) {
                if (i > 0) {
                    out.append(", ");
                }
                out.append("\"").append(assignableTypes.get(i)).append("\":1");
            }
            out.append("},\n");
            return;
        }
        // Default: emit nothing. ``defineClass`` treats a missing
        // ``a:`` key the same way as ``a:1`` — auto-populate the
        // assignableTo union from the base-class + interfaces
        // metadata already on the def.
    }

    private static void collectAssignableTypes(ByteCodeClass cls, List<ByteCodeClass> allClasses, List<String> out) {
        addAssignableType(out, JavascriptNameUtil.runtimeTypeName(cls.getClsName()));
        addAssignableType(out, "java_lang_Object");
        collectAssignableTypesFromBase(cls.getBaseClass(), allClasses, out);
        for (String iface : cls.getBaseInterfaces()) {
            collectAssignableTypesFromBase(iface, allClasses, out);
        }
    }

    private static void collectAssignableTypesFromBase(String className, List<ByteCodeClass> allClasses, List<String> out) {
        String normalized = JavascriptNameUtil.runtimeTypeName(className);
        if (normalized == null || containsType(out, normalized)) {
            return;
        }
        addAssignableType(out, normalized);
        ByteCodeClass base = findClass(normalized, allClasses);
        if (base == null) {
            return;
        }
        collectAssignableTypesFromBase(base.getBaseClass(), allClasses, out);
        for (String iface : base.getBaseInterfaces()) {
            collectAssignableTypesFromBase(iface, allClasses, out);
        }
    }

    private static void addAssignableType(List<String> out, String className) {
        if (className != null && !containsType(out, className)) {
            out.add(className);
        }
    }

    private static boolean containsType(List<String> types, String className) {
        for (int i = 0; i < types.size(); i++) {
            if (className.equals(types.get(i))) {
                return true;
            }
        }
        return false;
    }

    private static ByteCodeClass findClass(String className, List<ByteCodeClass> allClasses) {
        for (int i = 0; i < allClasses.size(); i++) {
            ByteCodeClass candidate = allClasses.get(i);
            if (className.equals(candidate.getClsName())) {
                return candidate;
            }
        }
        return null;
    }

    private static String renderStaticConstant(ByteCodeField field) {
        Object value = field.getValue();
        if (value instanceof String) {
            return "_L(\"" + JavascriptNameUtil.escapeJs((String) value) + "\")";
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue() ? "1" : "0";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        return JavascriptNameUtil.defaultValue(field.getRuntimeDescriptor());
    }

    private static String renderStaticFieldInitialValue(ByteCodeField field) {
        if (requiresDeferredStaticInitialization(field)) {
            return JavascriptNameUtil.defaultValue(field.getRuntimeDescriptor());
        }
        return field.getValue() == null ? JavascriptNameUtil.defaultValue(field.getRuntimeDescriptor()) : renderStaticConstant(field);
    }

    private static boolean requiresDeferredStaticInitialization(ByteCodeField field) {
        return field.getValue() instanceof String;
    }

    private static boolean hasDeferredStaticInitialization(ByteCodeClass cls) {
        for (ByteCodeField field : cls.getFields()) {
            if (field.isStaticField() && requiresDeferredStaticInitialization(field)) {
                return true;
            }
        }
        return false;
    }

    private static void appendDeferredStaticFieldInitialization(StringBuilder out, ByteCodeClass cls) {
        for (ByteCodeField field : cls.getFields()) {
            if (!field.isStaticField() || !requiresDeferredStaticInitialization(field)) {
                continue;
            }
            // ``_S["cls"]["field"]`` is the same ~20-char win as at
            // GETSTATIC / PUTSTATIC sites; these deferred-init
            // statements are emitted inside clinit bodies where the
            // verbose ``jvm.classes[...].staticFields[...]`` form used
            // to dominate the wire size.
            out.append("  _S[\"").append(cls.getClsName()).append("\"][\"")
                    .append(field.getFieldName()).append("\"] = ").append(renderStaticConstant(field)).append(";\n");
        }
    }

    private static boolean hasExplicitClinit(ByteCodeClass cls) {
        for (BytecodeMethod method : cls.getMethods()) {
            if (!method.isEliminated() && "__CLINIT__".equals(method.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void appendSyntheticClinitIfNeeded(StringBuilder out, ByteCodeClass cls) {
        if (!hasDeferredStaticInitialization(cls) || hasExplicitClinit(cls)) {
            return;
        }
        String fn = "cn1_" + cls.getClsName() + "___CLINIT___deferred";
        out.append("function* ").append(fn).append("(){\n");
        appendDeferredStaticFieldInitialization(out, cls);
        out.append("  return null;\n");
        out.append("}\n");
        currentClassClinitFn = fn;
    }

    private static void appendMethod(StringBuilder out, StringBuilder regs, ByteCodeClass cls, BytecodeMethod method) {
        currentEmissionClass = cls;
        try {
            // Emit into a local buffer so we can run a peephole pass
            // over the assembled method body before flushing to the
            // real output. The peephole collapses common push/pop
            // dataflow patterns the emitter can't see across
            // instructions (e.g., ALOAD + GETFIELD ≈ push X.Y).
            StringBuilder methodOut = new StringBuilder();
            appendMethodImpl(methodOut, regs, cls, method);
            out.append(applyMethodPeephole(methodOut));
        } finally {
            currentEmissionClass = null;
        }
    }

    /**
     * Peephole pass over an emitted method body. Collapses common
     * stack-dataflow patterns the per-instruction emitter can't see:
     *
     * <ul>
     * <li>{@code stack.p(X),stack.p(stack.q().$F)} → {@code stack.p(X.$F)}
     *     — push value, pop-and-getfield → push X.$F. ~2.9k sites.</li>
     * </ul>
     *
     * Each rewrite keeps running until no further match, since the
     * collapsed form can itself chain with a subsequent getfield
     * (``.p(X.$F),.p(.q().$G)`` → ``.p(X.$F.$G)``).
     */
    private static String applyMethodPeephole(CharSequence body) {
        String s = body.toString();
        // Safe-strip has already elided pc advances between adjacent
        // non-throwing instructions, so ALOAD + GETFIELD collapse to
        // two consecutive ``stack.p(...)`` expressions separated by
        // whitespace inside a single case block.
        //
        // push X; pop+getfield Y  ≡  push X.Y
        //   ``stack.p(X) stack.p(stack.q()["$prop"])``
        // →  ``stack.p(X["$prop"])``
        //
        // Chained GETFIELDs collapse further (e.g. ``push X; .$a; .$b``
        // → ``push X[$a][$b]``) because the rewritten RHS still
        // matches the pattern. Iterate until no more matches.
        //
        // X is conservatively captured as a short expression shape:
        // identifier + optional bracket accesses. Anything more
        // complicated (parens, commas, operators) bails out to the
        // literal push.
        String prev;
        do {
            prev = s;
            // Field-name tokens at this pre-mangle stage look like
            // ``cn1_java_lang_String_value`` (long ``cn1_*`` form).
            // The mangler later rewrites them to ``$...``. We match
            // the raw form here and preserve the quoted string so
            // both sides of the substitution stay intact.
            //
            // Rule 1: ALOAD + GETFIELD → inline field access.
            //   stack.p(X); stack.p(stack.q()["F"])  →  stack.p(X["F"])
            // Chained field accesses collapse via iteration (the
            // rewritten form with ``X["F"]`` matches the pattern for
            // the next GETFIELD).
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\(stack\\.q\\(\\)\\[\"([\\w\\$]+)\"\\]\\)",
                    "stack.p($1[\"$2\"])");
            // Rule 2: ALOAD + const + PUTFIELD → inline field store.
            //   stack.p(T); stack.p(V); { let v=stack.q(); stack.q()["F"]=v; pc=N; break; }
            //     → T["F"]=V; pc=N; break;
            // Value shape is conservative: simple identifier, literal
            // number, ``jvm.classes...`` expr, or another simple
            // identifier[prop] access.
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\(([^,;(){}]+)\\);?\\s*\\{\\s*let v = stack\\.q\\(\\);\\s*stack\\.q\\(\\)\\[\"([\\w\\$]+)\"\\] = v;\\s*(pc = \\d+; break;)\\s*\\}",
                    "$1[\"$3\"] = $2; $4");
            // Rule 3: ALOAD + ASTORE → locals[M] = locals[N].
            //   stack.p(X); locals[N] = stack.q();
            //     → locals[N] = X;
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*)\\);?\\s*locals\\[(\\d+)\\] = stack\\.q\\(\\);",
                    "locals[$2] = $1;");
            // Rule 4: IADD/ISUB/IMUL/IAND/IOR/IXOR with int-coercion.
            //   stack.p(X); stack.p(Y);
            //   { let b = stack.q(); let a = stack.q(); stack.p((a|0) OP (b|0)); }
            //     → stack.p(((X)|0) OP ((Y)|0));
            // Conservative X/Y shape to avoid runaway matches.
            s = s.replaceAll(
                    "stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*\\{\\s*let b = stack\\.q\\(\\);\\s*let a = stack\\.q\\(\\);\\s*stack\\.p\\(\\(a\\|0\\)\\s*([+\\-*&|\\^])\\s*\\(b\\|0\\)\\);\\s*\\}",
                    "stack.p(($1|0)$3($2|0));");
            // Rule 5: LADD/LSUB/LMUL/FADD/FSUB/FMUL/DADD/DSUB/DMUL
            // plus LAND/LOR/LXOR (no int-coercion form).
            //   stack.p(X); stack.p(Y);
            //   { let b = stack.q(); let a = stack.q(); stack.p(a OP b); }
            //     → stack.p((X) OP (Y));
            s = s.replaceAll(
                    "stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*\\{\\s*let b = stack\\.q\\(\\);\\s*let a = stack\\.q\\(\\);\\s*stack\\.p\\(a\\s*([+\\-*&|\\^])\\s*b\\);\\s*\\}",
                    "stack.p(($1)$3($2));");
            // Rule 5b: ISHL/ISHR with (b & 31) shift-distance mask.
            //   stack.p(X); stack.p(Y);
            //   { let b=stack.q(); let a=stack.q(); stack.p((a|0) OP (b & 31)); }
            //     → stack.p(((X)|0) OP ((Y) & 31));
            s = s.replaceAll(
                    "stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*\\{\\s*let b = stack\\.q\\(\\);\\s*let a = stack\\.q\\(\\);\\s*stack\\.p\\(\\(a\\|0\\)\\s*(<<|>>)\\s*\\(b & 31\\)\\);\\s*\\}",
                    "stack.p(($1|0)$3(($2) & 31));");
            // Rule 5c: IUSHR with ((a >>> (b & 31)) | 0) canonicalisation.
            s = s.replaceAll(
                    "stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*\\{\\s*let b = stack\\.q\\(\\);\\s*let a = stack\\.q\\(\\);\\s*stack\\.p\\(\\(a >>> \\(b & 31\\)\\) \\| 0\\);\\s*\\}",
                    "stack.p((($1) >>> (($2) & 31)) | 0);");
            // Rule 6: DUP preceded by a push — duplicate the value.
            //   stack.p(X); stack.p(stack[stack.length - 1]);
            //     → stack.p(X); stack.p(X);
            // Simpler yet: we can't do ``X`` twice if X has side
            // effects (e.g. a function call), so restrict to simple
            // identifiers and bracket accesses.
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\(stack\\[stack\\.length - 1\\]\\);",
                    "stack.p($1); stack.p($1);");
            // Rule 7: inline 0-arg virtual dispatch when the target
            // was just pushed.
            //   stack.p(T); stack.p(yield* cn1_iv0(stack.q(), "mid"));
            //     → stack.p(yield* cn1_iv0(T, "mid"));
            // T restricted to simple identifier+index shape.
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\(yield\\* cn1_iv0\\(stack\\.q\\(\\), \"([^\"]+)\"\\)\\);",
                    "stack.p(yield* cn1_iv0($1, \"$2\"));");
            // Rule 8: inline 1-arg virtual dispatch when target+arg
            // were just pushed.
            //   stack.p(T); stack.p(A);
            //   { let __arg0 = stack.q(); stack.p(yield* cn1_iv1(stack.q(), "mid", __arg0)); pc = N; break; }
            //     → stack.p(yield* cn1_iv1(T, "mid", A)); pc = N; break;
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*\\{ let __arg0 = stack\\.q\\(\\); stack\\.p\\(yield\\* cn1_iv1\\(stack\\.q\\(\\), \"([^\"]+)\", __arg0\\)\\); (pc = \\d+; break;) \\}",
                    "stack.p(yield* cn1_iv1($1, \"$3\", $2)); $4");
            // Rule 8b: extended arg pattern allowing ONE level of
            // balanced parens inside the arg push — captures common
            // shapes like ``_L("...")``, ``_O("...")``, ``_F(N)``,
            // ``$fn(x)`` that the restrictive Rule 8 skips. The target
            // push still requires the simple identifier/bracket shape
            // so the rewrite stays safe. 2-level nested calls (e.g.
            // ``yield* $fn(stack.q())``) stay on the slow path.
            //
            // The negative lookahead ``(?!stack\.q\()`` rejects EXPR
            // shapes that include a stack pop (``stack.q()|0`` from
            // F2I/I2B/I2C/I2S/L2I/D2I etc.). Such pops consume the
            // FIRST push, so the rule's invariant — that the call
            // block's outer ``stack.q()`` will pop the second push —
            // no longer holds, and inlining would emit the receiver
            // and arg in swapped slots. (Reproduced as
            // setBgTransparency((int) f) → "Missing virtual method on
            // float" in Toolbar.show*SidemenuImpl.)
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\(((?:(?!stack\\.q\\()[^;{}()]|\\([^()]*\\))+)\\);?\\s*\\{ let __arg0 = stack\\.q\\(\\); stack\\.p\\(yield\\* cn1_iv1\\(stack\\.q\\(\\), \"([^\"]+)\", __arg0\\)\\); (pc = \\d+; break;) \\}",
                    "stack.p(yield* cn1_iv1($1, \"$3\", $2)); $4");
            // Rule 9: same as Rule 8 but for void return.
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*\\{ let __arg0 = stack\\.q\\(\\); yield\\* cn1_iv1\\(stack\\.q\\(\\), \"([^\"]+)\", __arg0\\); (pc = \\d+; break;) \\}",
                    "yield* cn1_iv1($1, \"$3\", $2); $4");
            // Rule 9b: extended arg — balanced-parens variant of Rule 9.
            // See Rule 8b for the ``(?!stack\.q\()`` lookahead rationale.
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\(((?:(?!stack\\.q\\()[^;{}()]|\\([^()]*\\))+)\\);?\\s*\\{ let __arg0 = stack\\.q\\(\\); yield\\* cn1_iv1\\(stack\\.q\\(\\), \"([^\"]+)\", __arg0\\); (pc = \\d+; break;) \\}",
                    "yield* cn1_iv1($1, \"$3\", $2); $4");
            // Rule 10: 2-arg virtual with target + two args all pushed.
            //   stack.p(T); stack.p(A0); stack.p(A1);
            //   { let __arg1 = stack.q(); let __arg0 = stack.q(); stack.p(yield* cn1_iv2(stack.q(), "mid", __arg0, __arg1)); pc = N; break; }
            //     → stack.p(yield* cn1_iv2(T, "mid", A0, A1)); pc = N; break;
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*\\{ let __arg1 = stack\\.q\\(\\); let __arg0 = stack\\.q\\(\\); stack\\.p\\(yield\\* cn1_iv2\\(stack\\.q\\(\\), \"([^\"]+)\", __arg0, __arg1\\)\\); (pc = \\d+; break;) \\}",
                    "stack.p(yield* cn1_iv2($1, \"$4\", $2, $3)); $5");
            // Rule 10c: 2-arg virtual with balanced-parens args.
            // See Rule 8b for the ``(?!stack\.q\()`` lookahead rationale.
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\(((?:(?!stack\\.q\\()[^;{}()]|\\([^()]*\\))+)\\);?\\s*stack\\.p\\(((?:(?!stack\\.q\\()[^;{}()]|\\([^()]*\\))+)\\);?\\s*\\{ let __arg1 = stack\\.q\\(\\); let __arg0 = stack\\.q\\(\\); stack\\.p\\(yield\\* cn1_iv2\\(stack\\.q\\(\\), \"([^\"]+)\", __arg0, __arg1\\)\\); (pc = \\d+; break;) \\}",
                    "stack.p(yield* cn1_iv2($1, \"$4\", $2, $3)); $5");
            // Rule 11: 0-arg INVOKESPECIAL with inline target.
            //   stack.p(T); stack.p(yield* $ctor(stack.q())); pc = N; break;
            //     → stack.p(yield* $ctor(T)); pc = N; break;
            // Also the sync (no yield*) variant.
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\((yield\\* )?([a-zA-Z_\\$][\\w\\$]*)\\(stack\\.q\\(\\)\\)\\);",
                    "stack.p($2$3($1));");
            // Rule 12: 0-arg INVOKESTATIC with inline arg.
            //   stack.p(A); stack.p(yield* $fn(stack.q())); pc = N; break;
            //     → stack.p(yield* $fn(A));
            // (Already covered by Rule 11's shape since INVOKESTATIC
            // 1-arg looks identical.)
            // Rule 13: straight-line slot value-propagation.
            //   sN = EXPR; sN = sN["$prop"];
            //     → sN = EXPR["$prop"];
            // Chains further through iteration (sN=X; sN=sN.a; sN=sN.b
            // → sN=X.a.b). Matches only simple slot-to-slot flow where
            // the next statement reads and writes the SAME slot.
            s = s.replaceAll(
                    "(\\s+s(\\d+) = )([^;]+);\\s+s\\2 = s\\2(\\[\"[\\w\\$]+\"\\]);",
                    "$1$3$4;");
            // Rule 14: straight-line slot-to-return chain.
            //   sN = EXPR; return sN;
            //     → return EXPR;
            // The trailing slot assignment is dead — the return
            // consumes the value directly.
            s = s.replaceAll(
                    "\\s+s(\\d+) = ([^;]+);\\s+return s\\1;",
                    "\n  return $2;");
            // Rule 14b: straight-line slot propagation into a same-slot
            // function call (covers the common INVOKEVIRTUAL /
            // INVOKEINTERFACE / INVOKESTATIC shape where the result
            // overwrites the slot that supplied the receiver/first
            // argument).
            //   sN = EXPR;
            //   sN = yield* fn(sN, extra-args);
            //     → sN = yield* fn(EXPR, extra-args);
            // EXPR is conservatively a simple identifier / field
            // access / bracket path so we don't duplicate a call-site
            // or ``yield*`` across the substitution.
            s = s.replaceAll(
                    "(\\s+s(\\d+) = )([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\]|\\[\"[\\w\\$]+\"\\]|\\.\\$?[\\w]+)*);\\s+s\\2 = ((?:yield\\* )?[\\w\\$]+(?:\\.[\\w\\$]+)*)\\(s\\2((?:, [^)]*)?)\\);",
                    "$1$4($3$5);");
            // Rule 14c: same as 14b but the wrapping statement
            // doesn't reassign — it's a bare call (void method).
            //   sN = EXPR;
            //   fn(sN, ...);    or   yield* fn(sN, ...);
            // → fn(EXPR, ...);
            // Only safe when sN has no LATER reads — conservatively
            // this rewrite assumes the next statement is the final
            // use, which is a common straight-line shape. Skipping
            // for now to avoid risk; Rule 14b covers the clear case
            // where the slot is overwritten.
            // Rule 15: 3-arg virtual with target + three args all pushed.
            //   stack.p(T); stack.p(A0); stack.p(A1); stack.p(A2);
            //   { let __arg2=q; let __arg1=q; let __arg0=q; stack.p(yield* cn1_iv3(q, "mid", __arg0, __arg1, __arg2)); pc=N; break; }
            //     → stack.p(yield* cn1_iv3(T, "mid", A0, A1, A2)); pc=N; break;
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*\\{ let __arg2 = stack\\.q\\(\\); let __arg1 = stack\\.q\\(\\); let __arg0 = stack\\.q\\(\\); stack\\.p\\(yield\\* cn1_iv3\\(stack\\.q\\(\\), \"([^\"]+)\", __arg0, __arg1, __arg2\\)\\); (pc = \\d+; break;) \\}",
                    "stack.p(yield* cn1_iv3($1, \"$5\", $2, $3, $4)); $6");
            // Rule 15b: void-return variant of Rule 15.
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*\\{ let __arg2 = stack\\.q\\(\\); let __arg1 = stack\\.q\\(\\); let __arg0 = stack\\.q\\(\\); yield\\* cn1_iv3\\(stack\\.q\\(\\), \"([^\"]+)\", __arg0, __arg1, __arg2\\); (pc = \\d+; break;) \\}",
                    "yield* cn1_iv3($1, \"$5\", $2, $3, $4); $6");
            // Rule 16: 4-arg virtual with target + four args all pushed.
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*\\{ let __arg3 = stack\\.q\\(\\); let __arg2 = stack\\.q\\(\\); let __arg1 = stack\\.q\\(\\); let __arg0 = stack\\.q\\(\\); stack\\.p\\(yield\\* cn1_iv4\\(stack\\.q\\(\\), \"([^\"]+)\", __arg0, __arg1, __arg2, __arg3\\)\\); (pc = \\d+; break;) \\}",
                    "stack.p(yield* cn1_iv4($1, \"$6\", $2, $3, $4, $5)); $7");
            // Rule 16b: void-return variant of Rule 16.
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*\\{ let __arg3 = stack\\.q\\(\\); let __arg2 = stack\\.q\\(\\); let __arg1 = stack\\.q\\(\\); let __arg0 = stack\\.q\\(\\); yield\\* cn1_iv4\\(stack\\.q\\(\\), \"([^\"]+)\", __arg0, __arg1, __arg2, __arg3\\); (pc = \\d+; break;) \\}",
                    "yield* cn1_iv4($1, \"$6\", $2, $3, $4, $5); $7");
            // Rule 10b: void-return variant of Rule 10 (2-arg virtual).
            s = s.replaceAll(
                    "stack\\.p\\(([a-zA-Z_\\$][\\w\\$]*(?:\\[\\d+\\])*(?:\\[\"[\\w\\$]+\"\\])*)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*\\{ let __arg1 = stack\\.q\\(\\); let __arg0 = stack\\.q\\(\\); yield\\* cn1_iv2\\(stack\\.q\\(\\), \"([^\"]+)\", __arg0, __arg1\\); (pc = \\d+; break;) \\}",
                    "yield* cn1_iv2($1, \"$4\", $2, $3); $5");
            // Rule 17: array load (AALOAD/IALOAD/BALOAD/CALOAD/SALOAD)
            // with inlined array + index pushes.
            //   stack.p(A); stack.p(I);
            //   { let idx=stack.q(); let arr=stack.q(); stack.p(_A(arr, idx)); pc=N; break; }
            //     → stack.p(_A(A, I)); pc=N; break;
            // _A can throw (AIOOBE / NPE) so we retain the pc advance.
            s = s.replaceAll(
                    "stack\\.p\\(([^;(){},]+)\\);?\\s*stack\\.p\\(([^;(){},]+)\\);?\\s*\\{ let idx = stack\\.q\\(\\); let arr = stack\\.q\\(\\); stack\\.p\\(_A\\(arr, idx\\)\\); (pc = \\d+; break;) \\}",
                    "stack.p(_A($1, $2)); $3");
        } while (!prev.equals(s));
        // Post-pass: drop straight-line slot / local declarations
        // (``let sN;`` / ``let lN;``) whose name is never referenced
        // elsewhere in the method body. Rule 13 / 14 and the
        // slot-propagation rewrites above often eliminate the only
        // remaining use of a slot, leaving its bare declaration as
        // dead bytes. Each dead decl is ~8 chars.
        s = removeDeadLetDecls(s);
        // Compress ``stack[stack.length - 1]`` (DUP-style peek) to
        // ``stack.t()``. The runtime adds
        // ``Array.prototype.t = function(){return this[this.length-1];}``
        // alongside the existing ``.p``/``.q`` push/pop helpers.
        // ~10 chars saved per occurrence; 3k+ occurrences in the
        // Initializr build = ~30 KiB raw.
        s = s.replaceAll("stack\\[stack\\.length - 1\\]", "stack.t()");
        // Post-pass: strip ``case N:`` labels that aren't actually
        // jump targets (no ``pc=N`` writes them anywhere in the body
        // and they're not a try/catch handler / start / end pc, and
        // they're not the entry case 0). About 30% of the case
        // labels in our switch+pc emit are dead -- 40k+ instances on
        // Initializr's translated_app.js (~340 KB raw / ~30 KB gz).
        //
        // Why does the translator emit them at all? ``computeJumpTargets``
        // adds ``i+1`` to the target set for non-throwing-checked
        // instructions so the switch+pc emit doesn't accidentally
        // drop their body via ``!isTarget && !blockOpen`` -> continue.
        // The result is a label at every "could-throw" boundary. By
        // the time we get here all the bodies have been emitted, so
        // the labels themselves are pure overhead.
        s = stripDeadCaseLabels(s);
        // Collapse ``pc = N; break } case N: {`` immediate-jump
        // sequences when N is set in exactly one place. The runtime
        // does ``set pc, exit switch, re-enter switch via for(;;),
        // dispatch to case N`` — a no-op when N is the immediately
        // following case and nothing else jumps there. Saves
        // ``pc = N; break } case N: {`` (≈18 chars) per match.
        // esbuild --minify-syntax already collapses many of these,
        // but only when the case body is empty / pure-comment;
        // ours aren't, so esbuild leaves them. We do it here.
        s = collapseUniqueImmediateCaseFallthrough(s);
        // Final pass: shorten the per-method local registers ``stack``
        // → ``S`` and ``locals`` → ``L``. They appear ~210k times
        // (stack.p / stack.q / locals[N] / let stack=… / let locals=…)
        // in the Initializr translated_app.js — renaming to single
        // chars saves ~1.1 MiB raw of identifier bytes (~13% of the
        // generated JS) before brotli, and ~120 KiB after. The peephole
        // rules above all operate on the long names so they keep firing
        // unchanged; the rename runs strictly after every other peephole
        // has finished. Strings (``"..."`` / ``'...'``) are excluded so
        // theme-key literals containing the word ``stack`` survive.
        s = shortenStackAndLocals(s);
        // Final size pass: merge consecutive ``S.p(X),S.p(Y)`` into
        // ``S.p(X,Y)``. ``Array.prototype.push(...args)`` pushes
        // every argument in order, so multi-arg push is
        // semantically identical to a comma-sequence of single
        // pushes (X is fully evaluated before Y in both forms).
        // Conservative shape: each push arg captured as
        // ``[^,(){}]+`` -- bails if the arg contains a comma or
        // any bracket. That excludes ``yield*$fn(a,b)`` / ``L[0]``
        // etc., but matches the simpler literal / identifier /
        // dot-property pushes. Iterate so 3+ chains collapse to a
        // single multi-arg call.
        // The translator's per-instruction emit puts each push on
        // its own statement, so the body shape we see at this
        // point is ``S.p(X);\n  S.p(Y);`` (newline + indent
        // between pushes), not ``S.p(X),S.p(Y)``. Esbuild
        // collapses ``;`` to ``,`` later. We do the merge here so
        // the pre-minify text already carries the multi-arg push,
        // which then survives any further peephole and esbuild
        // passes intact.
        String prevS;
        do {
            prevS = s;
            s = s.replaceAll("S\\.p\\(([^,(){}]+)\\)\\s*[;,]\\s*S\\.p\\(([^,(){}]+)\\)",
                    "S.p($1,$2)");
        } while (!prevS.equals(s));
        // Replace the locals frame array with named locals.
        // ``let L = _F(N, T, A1, A2, ...)`` is the switch+pc
        // prelude; uses are ``L[0]``, ``L[1]``, etc. Each ``L[i]``
        // is 4 chars; replacing with ``l<i>`` (e.g. ``l3``) saves
        // ~2 chars per access. Initializr has ~80k accesses, so
        // ~160 KiB raw savings. The straight-line emission path
        // already uses named ``l0, l1, ...`` locals, so we're
        // bringing the switch+pc emit in line with it.
        s = renameLocalsArrayToNamedLocals(s);
        return s;
    }

    /**
     * Find ``let L=_F(N,arg0,arg1,...);`` (or with whitespace)
     * and convert to named local-variable declarations
     * ``let l0=arg0,l1=arg1,...,lN-1;``. Then replace every
     * ``L[i]`` access in the body with ``l<i>``. Per-method --
     * only fires for methods that emitted the full switch+pc
     * locals frame; straight-line methods don't have ``_F`` and
     * already use named locals.
     */
    private static String renameLocalsArrayToNamedLocals(String body) {
        // Try the _F prelude first: ``let L=_F(N, T, A1, A2, ...);``
        java.util.regex.Pattern letL = java.util.regex.Pattern.compile(
                "let\\s+L\\s*=\\s*_F\\s*\\(\\s*(\\d+)\\s*((?:,[^,()]+)*)\\s*\\)\\s*;");
        java.util.regex.Matcher m = letL.matcher(body);
        if (!m.find()) {
            // Fall back to the _N prelude (long/double-arg methods):
            //   ``let L=_N(N); ...; L[0]=T; L[1]=A1; L[2]=null; L[3]=A2; ...``
            // The L[i]=expr; assignments aren't comma-listed; they
            // appear as separate statements right after the _N call
            // (interleaved with ``let S=[];`` and ``let pc=0;`` --
            // the assignments themselves are contiguous though).
            return renameLocalsNPrelude(body);
        }
        int totalSize;
        try {
            totalSize = Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return body;
        }
        if (totalSize <= 0 || totalSize > 256) {
            // Sanity bound: avoid pathological sizes.
            return body;
        }
        // Collect comma-separated args (group(2) starts with leading
        // comma if present).
        java.util.List<String> args = new java.util.ArrayList<String>();
        String tail = m.group(2);
        if (tail != null && !tail.isEmpty()) {
            // Skip leading comma, then split on `,` -- args are
            // captured as ``[^,()]`` so they have no commas/parens.
            String[] parts = tail.substring(1).split(",");
            for (String p : parts) {
                args.add(p.trim());
            }
        }
        // Build the replacement: declarations for slots 0..totalSize-1.
        StringBuilder repl = new StringBuilder();
        repl.append("let ");
        for (int i = 0; i < totalSize; i++) {
            if (i > 0) {
                repl.append(",");
            }
            repl.append("l").append(i);
            if (i < args.size()) {
                repl.append("=").append(args.get(i));
            }
        }
        repl.append(";");
        // Replace the matched ``let L=_F(...);`` and rewrite L[i] in
        // the rest of the body. We do NOT touch L[i] inside string
        // literals; the walker tracks string state to be safe.
        StringBuilder out = new StringBuilder(body.length());
        out.append(body, 0, m.start());
        out.append(repl);
        int rest = m.end();
        char inString = 0;
        for (int i = rest; i < body.length(); ) {
            char c = body.charAt(i);
            if (inString != 0) {
                out.append(c);
                if (c == '\\' && i + 1 < body.length()) {
                    out.append(body.charAt(i + 1));
                    i += 2; continue;
                }
                if (c == inString) inString = 0;
                i++; continue;
            }
            if (c == '"' || c == '\'' || c == '`') {
                out.append(c);
                inString = c;
                i++; continue;
            }
            // Match ``L[<digits>]`` -- ensure we're not inside a
            // larger identifier (``XL[0]`` shouldn't be touched,
            // though that pattern shouldn't occur here).
            if (c == 'L' && i + 1 < body.length() && body.charAt(i + 1) == '[') {
                if (i == rest || !isIdentPart(body.charAt(i - 1))) {
                    int end = i + 2;
                    int numStart = end;
                    while (end < body.length() && Character.isDigit(body.charAt(end))) {
                        end++;
                    }
                    if (end > numStart && end < body.length() && body.charAt(end) == ']') {
                        out.append('l').append(body, numStart, end);
                        i = end + 1;
                        continue;
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /**
     * Variant for methods using the _N(N) prelude (double/long
     * arguments). The emit shape is
     *   ``let L=_N(N);let S=[];let pc=0;L[0]=T;L[1]=A1; ...``
     * Match the ``let L=_N(N);`` and the run of ``L[i]=expr;``
     * statements that follow (interleaved with the ``let S=[]``
     * / ``let pc=0`` lines), then rewrite to a single
     * ``let l0=T,l1=A1,l2,l3,...`` named-local declaration.
     * Gracefully bails if the shape doesn't fully match.
     */
    private static String renameLocalsNPrelude(String body) {
        java.util.regex.Pattern letN = java.util.regex.Pattern.compile(
                "let\\s+L\\s*=\\s*_N\\s*\\(\\s*(\\d+)\\s*\\)\\s*;");
        java.util.regex.Matcher m = letN.matcher(body);
        if (!m.find()) {
            return body;
        }
        int totalSize;
        try {
            totalSize = Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return body;
        }
        if (totalSize <= 0 || totalSize > 256) {
            return body;
        }
        // Walk forward from the match end, collecting any
        // ``L[i]=expr;`` statements (and skipping over the
        // intervening ``let S=[];`` / ``let pc=0;`` lines).
        java.util.Map<Integer, String> initExprs = new java.util.HashMap<Integer, String>();
        int scanPos = m.end();
        java.util.regex.Pattern lAssign = java.util.regex.Pattern.compile(
                "L\\[(\\d+)\\]\\s*=\\s*([^;]+);");
        java.util.regex.Pattern letPrelude = java.util.regex.Pattern.compile(
                "\\s*let\\s+S\\s*=\\s*\\[\\s*\\]\\s*;|\\s*let\\s+pc\\s*=\\s*0\\s*;|\\s+");
        while (scanPos < body.length()) {
            java.util.regex.Matcher pre = letPrelude.matcher(body).region(scanPos, body.length());
            pre.useAnchoringBounds(true);
            if (pre.lookingAt()) {
                scanPos = pre.end();
                continue;
            }
            java.util.regex.Matcher la = lAssign.matcher(body).region(scanPos, body.length());
            la.useAnchoringBounds(true);
            if (la.lookingAt()) {
                int idx;
                try {
                    idx = Integer.parseInt(la.group(1));
                } catch (NumberFormatException nfe) {
                    break;
                }
                if (idx < 0 || idx >= totalSize) break;
                String expr = la.group(2).trim();
                // Don't overwrite an earlier assignment for the
                // same slot -- we only inline the FIRST.
                if (!initExprs.containsKey(idx)) {
                    initExprs.put(idx, expr);
                }
                scanPos = la.end();
                continue;
            }
            break;
        }
        if (initExprs.isEmpty()) {
            return body;
        }
        // Build merged decl ``let l0=expr0,l1=expr1,...,lN-1;``
        // where slots without an init expression decl with no value.
        StringBuilder repl = new StringBuilder();
        repl.append("let ");
        for (int i = 0; i < totalSize; i++) {
            if (i > 0) repl.append(",");
            repl.append("l").append(i);
            String expr = initExprs.get(i);
            if (expr != null) {
                repl.append("=").append(expr);
            }
        }
        repl.append(";");

        // Replace the matched ``let L=_N(N);`` and consume the
        // assignment statements + intermixed prelude lines we
        // walked over. The ``let S=[];`` / ``let pc=0;`` are
        // re-emitted before the merged decl so they're still in
        // scope.
        StringBuilder out = new StringBuilder(body.length());
        out.append(body, 0, m.start());
        out.append("let S=[];let pc=0;");
        out.append(repl);

        // Now rewrite L[i] → l<i> in the rest of the body, skipping
        // string literals.
        char inString = 0;
        for (int i = scanPos; i < body.length(); ) {
            char c = body.charAt(i);
            if (inString != 0) {
                out.append(c);
                if (c == '\\' && i + 1 < body.length()) {
                    out.append(body.charAt(i + 1));
                    i += 2; continue;
                }
                if (c == inString) inString = 0;
                i++; continue;
            }
            if (c == '"' || c == '\'' || c == '`') {
                out.append(c);
                inString = c;
                i++; continue;
            }
            if (c == 'L' && i + 1 < body.length() && body.charAt(i + 1) == '[') {
                if (i == scanPos || !isIdentPart(body.charAt(i - 1))) {
                    int end = i + 2;
                    int numStart = end;
                    while (end < body.length() && Character.isDigit(body.charAt(end))) {
                        end++;
                    }
                    if (end > numStart && end < body.length() && body.charAt(end) == ']') {
                        out.append('l').append(body, numStart, end);
                        i = end + 1;
                        continue;
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /**
     * Rename whole-word ``stack`` → ``S``, ``locals`` → ``L``,
     * ``__cn1ThisObject`` → ``T``, and ``__cn1Arg<N>`` → ``A<N>`` in
     * a method body, skipping string literals and comments. All four
     * identifiers are emitter-private — they appear only in
     * ``appendMethodImpl`` (and a couple of straight-line setup
     * helpers that share the same name space) — so a per-method
     * rename is safe. Strings (``"..."`` / ``'...'`` / template
     * literals) are tracked so theme-key strings or mangled
     * symbol-table literals like ``"$T"`` / ``"$A2"`` are preserved.
     *
     * Per-method savings on the Initializr translated_app.js
     * (post-esbuild minify):
     *   - stack / locals: ~1.3 MiB raw, ~120 KiB gzip
     *   - __cn1ThisObject / __cn1Arg<N>: ~660 KiB raw, ~70 KiB gzip
     */
    private static String shortenStackAndLocals(String body) {
        int len = body.length();
        if (len == 0) {
            return body;
        }
        StringBuilder out = new StringBuilder(len);
        int i = 0;
        char inString = 0;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        while (i < len) {
            char c = body.charAt(i);
            if (inLineComment) {
                out.append(c);
                if (c == '\n') {
                    inLineComment = false;
                }
                i++;
                continue;
            }
            if (inBlockComment) {
                out.append(c);
                if (c == '*' && i + 1 < len && body.charAt(i + 1) == '/') {
                    out.append(body.charAt(i + 1));
                    i += 2;
                    inBlockComment = false;
                    continue;
                }
                i++;
                continue;
            }
            if (inString != 0) {
                out.append(c);
                if (c == '\\' && i + 1 < len) {
                    out.append(body.charAt(i + 1));
                    i += 2;
                    continue;
                }
                if (c == inString) {
                    inString = 0;
                }
                i++;
                continue;
            }
            if (c == '"' || c == '\'' || c == '`') {
                out.append(c);
                inString = c;
                i++;
                continue;
            }
            if (c == '/' && i + 1 < len) {
                char nx = body.charAt(i + 1);
                if (nx == '/') {
                    out.append(c).append(nx);
                    i += 2;
                    inLineComment = true;
                    continue;
                }
                if (nx == '*') {
                    out.append(c).append(nx);
                    i += 2;
                    inBlockComment = true;
                    continue;
                }
            }
            if ((c == 's' || c == 'l' || c == '_') && isIdentStart(body, i)) {
                int end = i + 1;
                while (end < len && isIdentPart(body.charAt(end))) {
                    end++;
                }
                String word = body.substring(i, end);
                if ("stack".equals(word)) {
                    out.append('S');
                } else if ("locals".equals(word)) {
                    out.append('L');
                } else if ("__cn1ThisObject".equals(word)) {
                    out.append('T');
                } else if (word.length() > 7
                        && word.charAt(0) == '_'
                        && word.charAt(1) == '_'
                        && word.startsWith("__cn1Arg")
                        && allDigits(word, 8)) {
                    // ``__cn1Arg<N>`` → ``A<N>``. Only collapse when
                    // the suffix is purely numeric so we don't catch
                    // hypothetical future names like ``__cn1ArgList``.
                    out.append('A').append(word, 8, word.length());
                } else if (word.length() > 5
                        && word.charAt(0) == '_'
                        && word.charAt(1) == '_'
                        && word.startsWith("__arg")
                        && allDigits(word, 5)) {
                    // ``__arg<N>`` (used inside invoke peephole arg
                    // blocks) -> ``_<N>``. ~25k decl + use sites,
                    // each saves 4 chars (``__arg0`` 6 -> ``_0`` 2).
                    // Distinct from ``__cn1Arg<N>`` (parameter names
                    // at function-scope) handled above.
                    out.append('_').append(word, 5, word.length());
                } else {
                    out.append(word);
                }
                i = end;
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static boolean isIdentStart(String body, int i) {
        if (i > 0) {
            char prev = body.charAt(i - 1);
            if (Character.isLetterOrDigit(prev) || prev == '_' || prev == '$') {
                return false;
            }
        }
        return true;
    }

    private static boolean allDigits(String s, int from) {
        if (from >= s.length()) {
            return false;
        }
        for (int k = from; k < s.length(); k++) {
            char ch = s.charAt(k);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }

    private static boolean isIdentPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    /**
     * Strip ``case N:`` labels from a method body when N is never set
     * via ``pc=N`` AND isn't a try/catch boundary AND isn't the
     * entry case 0. About 30% of the case labels in the switch+pc
     * emit fall into this category — they exist solely because
     * ``computeJumpTargets`` adds ``i+1`` to the target set for
     * non-throwing-checked instructions to keep their bodies from
     * being eliminated, but the resulting label has no real
     * dispatcher pointing at it. By the time we run this post-pass
     * the body has already been emitted, so the labels are pure
     * overhead.
     *
     * Operates only inside the ``switch(pc){ ... default:return}``
     * block; everything outside that scope passes through verbatim.
     * Tracks string state so values like ``_L("case 5:")`` aren't
     * mistakenly stripped (none should be in mangled output, but
     * the walker is cheap insurance).
     */
    private static String stripDeadCaseLabels(String body) {
        // Find the switch(pc){ ... } region. We expect exactly one
        // such block per method body; the case+pc emit is gated on
        // the method needing the interpreter so methods that took
        // the straight-line path don't enter this scope.
        // Translator emits ``switch (pc) {`` (with whitespace);
        // post-peephole rules collapse to ``switch(pc){``. Match
        // either form so this pass works pre- and post-peephole.
        int switchOpen = body.indexOf("switch (pc) {");
        int searchOffset;
        if (switchOpen >= 0) {
            searchOffset = switchOpen + "switch (pc) ".length();
        } else {
            switchOpen = body.indexOf("switch(pc){");
            if (switchOpen < 0) {
                return body;
            }
            searchOffset = switchOpen + "switch(pc)".length();
        }
        int blockOpen = body.indexOf('{', searchOffset);
        if (blockOpen < 0) {
            return body;
        }
        // Brace-balance to find the closing }
        int depth = 1, j = blockOpen + 1;
        char inString = 0;
        while (j < body.length() && depth > 0) {
            char c = body.charAt(j);
            if (inString != 0) {
                if (c == '\\' && j + 1 < body.length()) { j += 2; continue; }
                if (c == inString) { inString = 0; }
            } else if (c == '"' || c == '\'' || c == '`') {
                inString = c;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) break;
            }
            j++;
        }
        if (depth != 0) {
            return body;
        }
        String prefix = body.substring(0, blockOpen + 1);
        String region = body.substring(blockOpen + 1, j);
        String suffix = body.substring(j);

        // Collect every integer that's a real jump target inside the region.
        // Sources:
        //   - ``pc=<expr>`` writes; we extract every digit run from the RHS
        //     so ternaries like ``pc=cond?A:B`` contribute both A and B.
        //   - ``__cn1TryCatch`` table entries ``{s:N,e:M,h:K,t:"..."}``
        //     above the switch — those are part of the prefix, but
        //     handler pcs land in the region's case dispatch via
        //     ``pc=handler`` set by exception unwind, so we already
        //     pick them up via the pc=expr scan. Try-range start/end
        //     pcs in the table can ALSO be reached only via fall-through
        //     within the case run, so they don't strictly need their
        //     own case label — but the runtime's findExceptionHandler
        //     reads pc from the frame and dispatches, so handler pcs
        //     must remain. The pc=expr scan covers the dispatch path.
        java.util.Set<String> liveTargets = new java.util.HashSet<String>();
        // Initial entry pc -- ``pc=0`` is set in the prelude's
        // ``let pc=0`` so case 0 is always reachable.
        liveTargets.add("0");
        // Also scan ``__cn1TryCatch`` table from the prefix for
        // {s:N,e:M,h:K,...} so handler / range pcs aren't dropped.
        java.util.regex.Matcher tryRangesPrefix = java.util.regex.Pattern.compile(
                "\\{s:(\\d+),e:(\\d+),h:(\\d+),").matcher(prefix);
        while (tryRangesPrefix.find()) {
            liveTargets.add(tryRangesPrefix.group(3));
        }
        java.util.regex.Matcher tryRanges = java.util.regex.Pattern.compile(
                "\\{s:(\\d+),e:(\\d+),h:(\\d+),").matcher(region);
        while (tryRanges.find()) {
            liveTargets.add(tryRanges.group(3));
        }
        // Match both ``pc=N`` (post-peephole/inline) and ``pc = N``
        // (pre-peephole emission with whitespace). Capture
        // everything up to the next statement terminator (``;`` or
        // closing brace) and let ``digitRun`` pluck out the integer
        // values. We DON'T stop at ``)`` because expressions like
        // ``pc = S.q() == null ? 79 : 57`` would truncate at the
        // ``S.q()`` call's close-paren and miss the real target
        // numerals. Over-marking integer literals from other parts
        // of the same expression (e.g. method args) is safe — it
        // just means we keep an unused case label, never strip a
        // live one.
        java.util.regex.Matcher pcWrites = java.util.regex.Pattern.compile(
                "pc\\s*=\\s*([^;}]+)").matcher(region);
        java.util.regex.Pattern digitRun = java.util.regex.Pattern.compile("\\d+");
        while (pcWrites.find()) {
            String rhs = pcWrites.group(1);
            java.util.regex.Matcher digits = digitRun.matcher(rhs);
            while (digits.find()) {
                liveTargets.add(digits.group());
            }
        }

        // Now strip ``case N:`` labels not in liveTargets, but ONLY
        // at the top level of the outer switch(pc) -- i.e. brace
        // depth == 0 relative to the region. Java ``switch`` statements
        // in user code are emitted as a NESTED switch inside a case
        // body (``let __switchValue = stack.q()|0; switch(__switchValue)
        // { case 5: pc=12; break; ... }``), and those nested case
        // labels match user values, NOT pc -- they must NOT be
        // touched. Multi-label chains like ``case 5:case 6:case 7:
        // {...}`` are processed label-by-label; if 6 is dead but 5
        // and 7 are live we keep ``case 5:case 7:{...}``. Preserve
        // ``default:`` always.
        StringBuilder out = new StringBuilder(region.length());
        int i = 0;
        char rInString = 0;
        int braceDepth = 0; // 0 == outer switch(pc) body level
        while (i < region.length()) {
            char c = region.charAt(i);
            if (rInString != 0) {
                out.append(c);
                if (c == '\\' && i + 1 < region.length()) {
                    out.append(region.charAt(i + 1));
                    i += 2; continue;
                }
                if (c == rInString) rInString = 0;
                i++; continue;
            }
            if (c == '"' || c == '\'' || c == '`') {
                out.append(c);
                rInString = c;
                i++; continue;
            }
            if (c == '{') {
                out.append(c);
                braceDepth++;
                i++; continue;
            }
            if (c == '}') {
                out.append(c);
                braceDepth--;
                i++; continue;
            }
            // Only consider case labels at the top level of the
            // outer switch. Nested user switches live at depth >= 1.
            if (braceDepth == 0 && region.startsWith("case ", i)) {
                int end = i + 5;
                int numStart = end;
                while (end < region.length() && Character.isDigit(region.charAt(end))) {
                    end++;
                }
                if (end > numStart && end < region.length() && region.charAt(end) == ':') {
                    String num = region.substring(numStart, end);
                    int after = end + 1;
                    if (!liveTargets.contains(num)) {
                        i = after;
                        continue;
                    }
                    out.append(region, i, after);
                    i = after;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return prefix + out + suffix;
    }

    /**
     * Collapse ``pc = N; break } case N: {`` (or the post-peephole
     * ``pc=N;break}case N:{``) into a single ``;`` when ``pc = N``
     * appears only at this one site in the method body. The runtime
     * sets pc, exits the switch, re-iterates the for(;;), and
     * dispatches back to ``case N:`` -- a no-op when no other code
     * path reaches case N. Saves ~18 chars per match.
     *
     * Conservative: only fires when (a) the source pc value matches
     * the destination case label, (b) the case has a body (``case
     * N: { ... }`` shape, not bare ``case N:``), and (c) the same
     * integer appears exactly once after ``pc = `` / ``pc=``
     * elsewhere in the method body. esbuild --minify-syntax catches
     * empty-body case fall-throughs but won't merge case bodies
     * across yields, so most of our hits are residual.
     */
    private static String collapseUniqueImmediateCaseFallthrough(String body) {
        int switchOpen = body.indexOf("switch (pc) {");
        int searchOffset;
        if (switchOpen >= 0) {
            searchOffset = switchOpen + "switch (pc) ".length();
        } else {
            switchOpen = body.indexOf("switch(pc){");
            if (switchOpen < 0) {
                return body;
            }
            searchOffset = switchOpen + "switch(pc)".length();
        }
        int blockOpen = body.indexOf('{', searchOffset);
        if (blockOpen < 0) {
            return body;
        }
        int depth = 1, j = blockOpen + 1;
        char inString = 0;
        while (j < body.length() && depth > 0) {
            char c = body.charAt(j);
            if (inString != 0) {
                if (c == '\\' && j + 1 < body.length()) { j += 2; continue; }
                if (c == inString) inString = 0;
            } else if (c == '"' || c == '\'' || c == '`') {
                inString = c;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) break;
            }
            j++;
        }
        if (depth != 0) return body;
        String prefix = body.substring(0, blockOpen + 1);
        String region = body.substring(blockOpen + 1, j);
        String suffix = body.substring(j);

        // Count occurrences of every digit literal that appears as
        // a target in any ``pc = <expr>`` write. Critical: a
        // ``pc = cond ? 5 : 3`` expression sets pc to 5 OR 3, so
        // both are reached -- if we only counted direct ``pc=N``
        // assignments we'd undercount and incorrectly collapse a
        // case that's still reachable via a ternary, producing a
        // runtime NPE (default:return falls off the case dispatch).
        // ``[^;}]+`` matches up to the next statement terminator
        // (``)`` is allowed inside the RHS so calls like
        // ``S.q() == null`` don't truncate); then digit-run
        // extraction grabs every integer literal mentioned.
        java.util.Map<String, Integer> pcCount = new java.util.HashMap<String, Integer>();
        java.util.regex.Matcher pcWrites = java.util.regex.Pattern.compile(
                "pc\\s*=\\s*([^;}]+)").matcher(region);
        java.util.regex.Pattern digitRun = java.util.regex.Pattern.compile("\\d+");
        while (pcWrites.find()) {
            String rhs = pcWrites.group(1);
            java.util.regex.Matcher digits = digitRun.matcher(rhs);
            while (digits.find()) {
                String num = digits.group();
                Integer prev = pcCount.get(num);
                pcCount.put(num, prev == null ? 1 : prev + 1);
            }
        }

        // Apply: ``pc = N; break; } case N: {`` (and the no-space
        // variant). The trailing ``{`` opens a new case body that
        // we want to merge with the prior one, so we just replace
        // the whole stretch with ``;``. This collapses two case
        // bodies into one syntactically.
        java.util.regex.Pattern pat = java.util.regex.Pattern.compile(
                "pc\\s*=\\s*(\\d+)\\s*;\\s*break\\s*;?\\s*\\}\\s*case\\s+(\\d+)\\s*:\\s*\\{");
        java.util.regex.Matcher m = pat.matcher(region);
        StringBuilder out = new StringBuilder(region.length());
        int last = 0;
        while (m.find()) {
            String src = m.group(1);
            String dst = m.group(2);
            if (!src.equals(dst)) continue;
            Integer count = pcCount.get(src);
            if (count == null || count != 1) continue;
            out.append(region, last, m.start());
            // Replace with empty (just falls through into the body
            // that came after ``case N: {``).
            last = m.end();
        }
        if (last == 0) return body;
        out.append(region, last, region.length());
        return prefix + out + suffix;
    }


    /**
     * Scan ``body`` for ``let (s|l)N;`` declarations and drop any
     * whose identifier doesn't appear anywhere else. The check uses a
     * word-boundary regex so ``l1`` doesn't accidentally match inside
     * ``l10``, ``locals1``, or similar. Runs once after the fixed-
     * point peephole pass — dropping a decl can't enable further
     * rewrites, so one pass suffices.
     */
    private static String removeDeadLetDecls(String body) {
        java.util.regex.Pattern declPattern = java.util.regex.Pattern.compile(
                "  let ([sl]\\d+);\\n");
        java.util.regex.Matcher m = declPattern.matcher(body);
        StringBuilder out = new StringBuilder(body.length());
        int last = 0;
        while (m.find()) {
            String ident = m.group(1);
            // Count word-boundary occurrences of ``ident`` in the
            // WHOLE body (not just post-decl): a later assignment
            // might reassign the slot without reading, in which case
            // the decl is still dead. Any use (read or write)
            // elsewhere keeps the decl alive.
            int count = countWholeIdentifier(body, ident);
            // The decl itself contributes one occurrence; anything
            // else means the slot is used.
            if (count > 1) {
                continue;
            }
            out.append(body, last, m.start());
            last = m.end();
        }
        if (last == 0) {
            return body;
        }
        out.append(body, last, body.length());
        return out.toString();
    }

    private static int countWholeIdentifier(String body, String ident) {
        int count = 0;
        int from = 0;
        int len = ident.length();
        while ((from = body.indexOf(ident, from)) >= 0) {
            char before = from > 0 ? body.charAt(from - 1) : ' ';
            int endIdx = from + len;
            char after = endIdx < body.length() ? body.charAt(endIdx) : ' ';
            boolean leftOk = !Character.isLetterOrDigit(before) && before != '_' && before != '$';
            boolean rightOk = !Character.isLetterOrDigit(after) && after != '_' && after != '$';
            if (leftOk && rightOk) {
                count++;
            }
            from += len;
        }
        return count;
    }

    private static void appendMethodImpl(StringBuilder out, StringBuilder regs, ByteCodeClass cls, BytecodeMethod method) {
        List<Instruction> instructions = method.getInstructions();
        Map<Label, Integer> labelToIndex = buildLabelMap(instructions);
        String jsMethodName = jsMethodIdentifier(cls, method);
        String jsMethodBodyName = jsMethodBodyIdentifier(cls, method);
        boolean wrappedStaticMethod = isWrappedStaticMethod(method);
        // Suspension flag drives whether the method is emitted as a
        // generator (``function*``) or a plain sync function. Only sync
        // methods can be invoked without the ``yield*`` ceremony, so a
        // mis-classification toward sync would break runtime dispatch.
        // The classifier conservatively defaults to suspending, so this
        // flag is only false when the analysis has proven the body
        // cannot yield the cooperative scheduler.
        boolean methodSuspending = method.isJavascriptSuspending();
        String fnKeyword = methodSuspending ? "function* " : "function ";
        out.append(fnKeyword).append(wrappedStaticMethod ? jsMethodBodyName : jsMethodName).append("(");
        boolean first = true;
        if (!method.isStatic()) {
            out.append("__cn1ThisObject");
            first = false;
        }
        List<ByteCodeMethodArg> arguments = method.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append("__cn1Arg").append(i + 1);
        }
        out.append("){\n");
        if (!wrappedStaticMethod && method.isStatic() && !"__CLINIT__".equals(method.getMethodName())) {
            out.append("  _I(\"").append(cls.getClsName()).append("\");\n");
        }
        if ("__CLINIT__".equals(method.getMethodName())) {
            appendDeferredStaticFieldInitialization(out, cls);
        }
        if (appendStraightLineMethodBody(out, regs, cls, method, instructions, wrappedStaticMethod ? jsMethodBodyName : jsMethodName)) {
            if (wrappedStaticMethod && shouldEmitStaticWrapper(method)) {
                appendWrappedStaticMethod(out, cls, method, jsMethodName, jsMethodBodyName);
            }
            return;
        }
        // Both per-method caches are gone now: the virtual-dispatch
        // cache moved to a global ``resolvedVirtualCache`` keyed on
        // className|methodId (see ``jvm.resolveVirtual``), and the
        // class-init cache was dropped because
        // ``jvm.ensureClassInitialized`` already early-returns on its
        // own ``cls.initialized`` flag. The booleans are hard-coded
        // here so the legacy ``appendInstruction(...,
        // usesClassInitCache, usesVirtualDispatchCache)`` signatures
        // don't need cascading edits.
        boolean usesClassInitCache = false;
        boolean usesVirtualDispatchCache = false;
        // Compact frame setup: ``_F(N, thisOrNull, arg1, arg2, ...)``
        // returns a size-N locals array with consecutive slots
        // pre-populated. Saves ~15-30 chars per method vs the previous
        // form of ``_N(N)`` + separate ``locals[i] = ...`` lines.
        // For methods with long/double arguments the extra slot they
        // consume stays as the default ``null`` from ``jvm.aN`` — no
        // special padding needed because the corresponding ``__cn1ArgK``
        // is still the long/double value, and the next ``__cn1ArgK+1``
        // lands at ``localIndex + 2`` via the emitter below. We pass
        // ``null`` in the skipped slot when a long/double arg is
        // present so positional args after it line up correctly.
        boolean hasDoubleOrLong = false;
        for (int i = 0; i < arguments.size(); i++) {
            if (arguments.get(i).isDoubleOrLong()) { hasDoubleOrLong = true; break; }
        }
        if (hasDoubleOrLong) {
            // Keep the explicit-slot emission for long/double-bearing
            // methods so slot layout stays correct without complicating
            // jvm.fr.
            out.append("  let locals = _N(").append(Math.max(1, method.getMaxLocals())).append(");\n");
            out.append("  let stack = [];\n");
            out.append("  let pc = 0;\n");
            if (!method.isStatic()) {
                out.append("  locals[0] = __cn1ThisObject;\n");
            }
            int localIndex = method.isStatic() ? 0 : 1;
            for (int i = 0; i < arguments.size(); i++) {
                out.append("  locals[").append(localIndex).append("] = __cn1Arg").append(i + 1).append(";\n");
                localIndex++;
                if (arguments.get(i).isDoubleOrLong()) {
                    localIndex++;
                }
            }
        } else {
            out.append("  let locals = _F(").append(Math.max(1, method.getMaxLocals()));
            if (!method.isStatic()) {
                out.append(",__cn1ThisObject");
            }
            for (int i = 0; i < arguments.size(); i++) {
                out.append(",__cn1Arg").append(i + 1);
            }
            out.append(");\n");
            out.append("  let stack = [];\n");
            out.append("  let pc = 0;\n");
        }
        // Only emit the exception-dispatch scaffolding when the method
        // actually has a try/catch block. Many simple methods have no
        // exception table, in which case the ``const __cn1TryCatch =
        // []; ... try { switch (pc) { ... } } catch (__cn1Error) {
        // const __handler = jvm.findExceptionHandler(__cn1TryCatch,
        // pc, __cn1Error); if (!__handler) throw __cn1Error; ... }``
        // wrapper is pure overhead (~200 bytes/method, ~5-6 MiB total
        // across an app the size of Initializr). When it is omitted,
        // uncaught JS throws propagate naturally up through the
        // generator's ``yield*`` chain — identical observable
        // semantics without the boilerplate.
        //
        // Kill-switch: flip ``parparvm.js.tryelide.off`` to always
        // emit the wrapper (matches the historical emission).
        boolean forceTryWrapper = System.getProperty("parparvm.js.tryelide.off") != null;
        boolean hasTryCatch = forceTryWrapper || methodHasTryCatch(instructions);
        if (hasTryCatch) {
            appendTryCatchTable(out, instructions, labelToIndex);
        }
        if (method.isSynchronizedMethod()) {
            out.append("  let __cn1Monitor = ").append(method.isStatic() ? "jvm.getClassObject(\"" + cls.getClsName() + "\")" : "__cn1ThisObject").append(";\n");
            // ``yield* _me(...)`` lets the calling green thread park if
            // the monitor is contended; the non-contended case is a
            // fast no-yield. See parparvm_runtime.js ``_me`` /
            // ``monitorEnter``.
            out.append("  yield* _me(__cn1Monitor);\n");
            out.append("  try {\n");
        }
        out.append("  while (true) {\n");
        if (hasTryCatch) {
            out.append("    try {\n");
        }
        out.append("    switch (pc) {\n");
        // Merge sequential non-branch-target instructions into a single
        // case block. Each instruction ordinarily emits its body with a
        // trailing ``pc = N+1; break;`` to leave the switch and let the
        // outer ``while(true)`` re-enter at pc N+1. When the next
        // instruction isn't a jump target and isn't itself a branch,
        // we can drop that tail and rely on JS switch fall-through to
        // execute the next instruction's body in the same dispatch
        // iteration. For a typical Initializr method body (long runs of
        // stack / local / field / invoke ops punctuated by occasional
        // jumps), this collapses hundreds of per-case ``pc = N+1;
        // break;`` tails and their closing braces into a single block.
        // Kill-switch for the case-merge optimization. When set, every
        // non-no-op instruction emits its own ``case N: { ... }`` with
        // a real ``pc = N+1; break;`` tail — the pre-merge emission
        // shape. Flip via the JVM system property
        // ``parparvm.js.merge.off``.
        boolean mergeCases = System.getProperty("parparvm.js.merge.off") == null;
        java.util.Set<Integer> jumpTargets = mergeCases
                ? computeJumpTargets(instructions, labelToIndex)
                : java.util.Collections.<Integer>emptySet();
        boolean blockOpen = false;
        // True when the previous emission was a bare ``case N:`` label
        // for a no-op instruction (line number, local var, try/catch
        // range marker, label). The next substantive instruction must
        // open a real block — it supplies the executable body for that
        // no-op label, and control reaches it only via switch
        // fall-through (so it's not in ``jumpTargets`` and would
        // otherwise be elided as dead code).
        boolean pendingBareLabel = false;
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            // When merging is disabled, treat every non-no-op PC as a
            // jump target so each instruction gets its own
            // self-contained case block.
            boolean isTarget = !mergeCases || i == 0 || jumpTargets.contains(i);
            if (!mergeCases) {
                // Pre-merge path: every non-no-op instruction emits a
                // fully-closed ``case N: { body }`` block with its own
                // pc advance, matching the historical emission.
                if (isPcSkippableNoOp(instruction) && i + 1 < instructions.size()) {
                    out.append("      case ").append(i).append(":\n");
                    continue;
                }
                out.append("      case ").append(i).append(": {\n");
                appendInstruction(out, method, instructions, labelToIndex, instruction, i, usesClassInitCache, usesVirtualDispatchCache);
                out.append("      }\n");
                continue;
            }
            if (isPcSkippableNoOp(instruction) && i + 1 < instructions.size()) {
                // Emit just the case label — fall through into the next
                // instruction's block (which supplies the executable
                // body). If a ``{`` block is currently open, close it
                // first so the case label is syntactically at switch
                // scope rather than inside a previous block.
                if (blockOpen) {
                    out.append("      }\n");
                    blockOpen = false;
                }
                // The bare ``case i:`` label is only reachable if
                // something branches to pc=i (jumpTargets contains i),
                // or if pc starts at i (i == 0). Line numbers and local
                // var range markers are never branch targets, so their
                // labels are dead code. Dropping them still sets
                // pendingBareLabel so the next real instruction opens
                // its own case block — preceding case labels (kept or
                // elided) simply fall through to that block.
                if (i == 0 || jumpTargets.contains(i)) {
                    out.append("      case ").append(i).append(":\n");
                }
                pendingBareLabel = true;
                continue;
            }
            // SAFE STRIP: drop the trailing ``pc = N+1; break;`` when
            // the current instruction is PROVABLY non-throwing AND the
            // next pc isn't a branch target. A general strip is
            // unsafe — if the later instruction in the merged block
            // throws, the frame's pc would still reference the
            // earlier one and exception dispatch might pick the wrong
            // try-range. But when the earlier instruction cannot
            // throw AT ALL (pure stack/local/compute ops, no memory
            // access), conflating its pc with the next is harmless.
            boolean nextIsNewBlock = i + 1 >= instructions.size() || jumpTargets.contains(i + 1);
            boolean isTerminal = isTerminatingInstruction(instruction);
            boolean strip = !isTerminal && !nextIsNewBlock && isNonThrowingInstruction(instruction);
            if (isTarget || pendingBareLabel) {
                if (blockOpen) {
                    out.append("      }\n");
                    blockOpen = false;
                }
                out.append("      case ").append(i).append(": {\n");
                // Pin pc to this block's instruction index BEFORE the
                // body runs. When prior bare-case labels (line numbers,
                // try-range markers) fell through into this block, the
                // pc variable still holds whichever case label the
                // enclosing switch entered on. That's fine for normal
                // control flow — the body's trailing ``pc = N+1; break``
                // overwrites pc before the next switch dispatch — but
                // fatal for exception handling: a throw mid-body invokes
                // ``_E(table, pc, err, ...)`` with the stale entry pc,
                // and findExceptionHandler skips the (otherwise matching)
                // try-range entry whose [s, e) interval starts at a
                // later pc than the bare-case label. Surfaced as
                // InterruptedException uncaught when Thread.sleep
                // threw inside a method whose try-range began past the
                // first merged label. ~7 chars per case block; the
                // correctness win outweighs the size hit.
                if (mergeCases && hasExceptionHandlers(method) && needsPcPin(instructions, i)) {
                    out.append("        pc = ").append(i).append(";\n");
                }
                blockOpen = true;
                pendingBareLabel = false;
            } else if (!blockOpen) {
                continue;
            }
            if (strip) {
                StringBuilder buf = new StringBuilder();
                appendInstruction(buf, method, instructions, labelToIndex, instruction, i, usesClassInitCache, usesVirtualDispatchCache);
                out.append(stripTrailingPcAdvance(buf.toString(), i + 1));
            } else {
                appendInstruction(out, method, instructions, labelToIndex, instruction, i, usesClassInitCache, usesVirtualDispatchCache);
            }
            if ((isTerminal || nextIsNewBlock || !strip) && blockOpen) {
                out.append("      }\n");
                blockOpen = false;
            }
        }
        if (blockOpen) {
            out.append("      }\n");
            blockOpen = false;
        }
        // ``default:return`` guards against a pc landing on an
        // instruction index that the emission loop elided. That
        // happens whenever a throwing instruction's ``pc = i + 1;
        // break;`` tail targets a no-op (LineNumber, LocalVariable
        // range marker) whose bare ``case i+1:`` was dropped by the
        // dead-label elision pass: without a matching ``case``
        // arm, the enclosing ``while (true) switch(pc)`` loop spins
        // on the same pc forever. Keeping the explicit default
        // costs ~14 chars × ~3k methods ≈ 42 KiB but buys a clean
        // method exit in that corner case. Kill-switch
        // ``parparvm.js.defaultreturn.off`` skips the default for
        // experimental builds that also arrange for every pc tail
        // to land on a real label.
        if (System.getProperty("parparvm.js.defaultreturn.off") != null) {
            out.append("    }\n");
        } else {
            out.append("      default:return}\n");
        }
        if (hasTryCatch) {
            // ``_E`` (runtime helper) wraps the repeated catch-block
            // boilerplate: find the matching handler, rethrow if none,
            // otherwise reset the stack to hold the pending exception
            // and return the handler pc. Per-method cost drops from
            // ~150 chars of inlined catch plumbing to ~30 chars.
            out.append("    } catch (__cn1Error) { pc = _E(__cn1TryCatch, pc, __cn1Error, stack); }\n");
        }
        out.append("  }\n");
        if (method.isSynchronizedMethod()) {
            out.append("  } finally {\n");
            out.append("    _mx(__cn1Monitor);\n");
            out.append("  }\n");
        }
        out.append("}\n");
        if (wrappedStaticMethod && shouldEmitStaticWrapper(method)) {
            appendWrappedStaticMethod(out, cls, method, jsMethodName, jsMethodBodyName);
        }
        if ("__CLINIT__".equals(method.getMethodName())) {
            currentClassClinitFn = wrappedStaticMethod ? jsMethodBodyName : jsMethodName;
        }
        if (!method.isStatic() && !method.isConstructor()) {
            String dispatchId = JavascriptNameUtil.dispatchMethodIdentifier(method.getMethodName(), method.getSignature());
            appendPrimaryRegistration(regs, dispatchId, jsMethodName);
        }
    }

    private static boolean isWrappedStaticMethod(BytecodeMethod method) {
        return method.isStatic() && !"__CLINIT__".equals(method.getMethodName());
    }

    /**
     * Non-native static methods have their body emitted as
     * ``$name__impl`` AND a public wrapper ``$name`` that did
     * ``_I(className); return yield* $name__impl(args)``. Every
     * INVOKESTATIC callsite that the emitter produces calls
     * ``$name__impl`` directly (after its own ``_I`` elision logic),
     * so the wrapper has no in-bundle callers. The only reason to
     * keep it is NATIVE static methods, where the ``__impl`` name
     * doesn't exist and the wrapper IS the entry point. Skip the
     * wrapper for non-native statics entirely. Kill-switch
     * ``parparvm.js.staticwrapper.keep`` restores the old behaviour.
     */
    private static boolean shouldEmitStaticWrapper(BytecodeMethod method) {
        // The wrapper is the CANONICAL global-scope name for a
        // static method: runtime / port.js code that references the
        // method by its unsuffixed identifier (``cn1_Cls_m_sig``)
        // relies on it — most importantly ``jvm.setMain`` looks up
        // ``global[cn1_<mainCls>_main_...]`` to obtain the generator
        // factory at boot. Eliding the wrapper for non-native
        // statics saved ~88 KiB after mangling but broke the main
        // entry point and any @JSBody / bindNative overlay that
        // resolves methods through the unsuffixed name. Keep the
        // wrapper for every non-clinit static; the extra bytes are
        // worth the boot-time correctness guarantee.
        //
        // Kill-switch ``parparvm.js.staticwrapper.elide`` re-enables
        // the (aggressive, risky) elision for experimentation.
        if (System.getProperty("parparvm.js.staticwrapper.elide") != null) {
            return method.isNative();
        }
        return true;
    }

    private static void appendWrappedStaticMethod(StringBuilder out, ByteCodeClass cls, BytecodeMethod method, String wrapperName, String bodyName) {
        // Wrapper matches the body's suspension. If the body is sync,
        // the wrapper can be sync too — ``jvm.ensureClassInitialized``
        // runs the clinit generator to completion synchronously and
        // then returns, so the wrapper never yields on that call.
        boolean suspending = method.isJavascriptSuspending();
        out.append(suspending ? "function* " : "function ").append(wrapperName).append("(");
        appendMethodParameters(out, method);
        out.append("){\n");
        out.append("  _I(\"").append(cls.getClsName()).append("\");\n");
        out.append("  return ").append(suspending ? "yield* " : "").append(bodyName).append("(");
        appendMethodParameterArguments(out, method);
        out.append(");\n");
        out.append("}\n");
    }

    private static void appendMethodParameters(StringBuilder out, BytecodeMethod method) {
        boolean first = true;
        if (!method.isStatic()) {
            out.append("__cn1ThisObject");
            first = false;
        }
        List<ByteCodeMethodArg> arguments = method.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append("__cn1Arg").append(i + 1);
        }
    }

    private static void appendMethodParameterArguments(StringBuilder out, BytecodeMethod method) {
        boolean first = true;
        if (!method.isStatic()) {
            out.append("__cn1ThisObject");
            first = false;
        }
        List<ByteCodeMethodArg> arguments = method.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append("__cn1Arg").append(i + 1);
        }
    }

    private static void appendInheritedMethodAliases(StringBuilder out, StringBuilder regs, ByteCodeClass cls) {
        Map<String, BytecodeMethod> inherited = new LinkedHashMap<String, BytecodeMethod>();
        collectInheritedMethodAliases(cls.getBaseClassObject(), inherited, new HashSet<ByteCodeClass>());
        List<ByteCodeClass> baseInterfaces = cls.getBaseInterfacesObject();
        if (baseInterfaces != null) {
            for (ByteCodeClass iface : baseInterfaces) {
                collectInheritedMethodAliases(iface, inherited, new HashSet<ByteCodeClass>());
            }
        }
        for (BytecodeMethod method : inherited.values()) {
            if (method == null || method.isConstructor() || method.isAbstract() || method.isEliminated()) {
                continue;
            }
            if (declaresMethod(cls, method.getMethodName(), method.getSignature())) {
                continue;
            }
            String aliasName = JavascriptNameUtil.methodIdentifier(cls.getClsName(), method.getMethodName(), method.getSignature());
            String targetName = JavascriptNameUtil.methodIdentifier(method.getClsName(), method.getMethodName(), method.getSignature());
            if (aliasName.equals(targetName)) {
                continue;
            }
            // An inherited-method bridge used to be emitted as a
            // forwarding wrapper ``function* cn1_Child_m(args) { return
            // yield* cn1_Parent_m(args); }`` plus a methods-map entry
            // registering ``cn1_Child_m`` on the child's virtual table.
            // ~60k such bridges accounted for ~3 MiB of Initializr's
            // translated JS. Direct-by-name call sites never reach
            // these bridges: ``resolveDirectInvokeOwner`` walks the
            // hierarchy for INVOKESTATIC/INVOKESPECIAL and emits the
            // actual declaring class's identifier. That leaves only
            // the methods-map lookup used by virtual dispatch — and
            // that lookup is happy with a plain alias entry
            // ``cn1_Child_m: cn1_Parent_m`` pointing at the upstream
            // impl. So we skip the wrapper function entirely and
            // emit an alias registration instead. Both names are
            // mangled independently but in lockstep with call sites,
            // so the alias resolves correctly under mangling.
            if (!method.isStatic()) {
                appendAliasRegistration(regs, aliasName, targetName);
            }
        }
    }

    /**
     * Register cls under every ancestor method id its concrete methods
     * satisfy. Without these aliases the runtime would depend on
     * {@code methodTail} / {@code remappedMethodId} — which reconstruct
     * method ids by concatenating class names with literal method
     * suffixes at runtime — to dispatch interface or base-class method
     * calls to cls's implementing method. That reconstruction works
     * only while method ids are the verbose translator-owned
     * ``cn1_&lt;class&gt;_&lt;method&gt;`` form. When the cross-file
     * identifier mangler rewrites those ids to short ``$a`` tokens,
     * the ancestor's id and cls's id pick up unrelated mangled forms,
     * so runtime concatenation of the ancestor class + unmangled tail
     * no longer matches the mangled key in the methods table and the
     * dispatch either fails ("Missing virtual method") or silently
     * resolves to the ancestor's own impl — breaking polymorphism.
     *
     * Emitting explicit {@code addVirtualMethod(cls, ancestorId, fn)}
     * pairs here lets {@code resolveVirtual}'s direct-lookup branch
     * succeed before any remapping is attempted, regardless of
     * whether the ids are mangled (both sides move in lockstep).
     *
     * We cover BOTH base-class overrides and interface implementations
     * in a single pass: the ancestor walk visits the full class +
     * interface hierarchy reachable from cls, including interfaces
     * inherited transitively via base classes and parent interfaces.
     */
    private static void appendInterfaceMethodAliases(StringBuilder regs, ByteCodeClass cls) {
        if (cls.isIsInterface()) {
            return;
        }
        // Resolvable = every (name+sig) whose ``cn1_<cls>_<m>`` is
        // guaranteed to exist as a function at load time:
        //   (a) cls declares the method concretely (appendMethod /
        //       appendNativeStubIfNeeded emits the function body),
        //   (b) cls does NOT declare the method but inherits a
        //       concrete impl from a base class or interface default
        //       method, and appendInheritedMethodAliases mirrors that
        //       impl under cls's prefix via a wrapper function.
        //
        // We include inherited-resolvable methods so that a concrete
        // subclass dispatches correctly even for method ids declared
        // abstractly in an intermediate ancestor whose own base class
        // provides the impl: in that case the intermediate can't emit
        // the alias (it doesn't declare the method concretely) and the
        // concrete base can't either (the intermediate isn't in its
        // ancestor chain), so the concrete subclass has to register
        // the mapping on its own methods table.
        Set<String> resolvable = new HashSet<String>();
        for (BytecodeMethod method : cls.getMethods()) {
            if (method == null || method.isAbstract() || method.isEliminated() || method.isConstructor() || method.isStatic()) {
                continue;
            }
            resolvable.add(method.getMethodName() + method.getSignature());
        }
        Map<String, BytecodeMethod> inherited = new LinkedHashMap<String, BytecodeMethod>();
        collectInheritedMethodAliases(cls.getBaseClassObject(), inherited, new HashSet<ByteCodeClass>());
        List<ByteCodeClass> directInterfaces = cls.getBaseInterfacesObject();
        if (directInterfaces != null) {
            for (ByteCodeClass iface : directInterfaces) {
                collectInheritedMethodAliases(iface, inherited, new HashSet<ByteCodeClass>());
            }
        }
        for (Map.Entry<String, BytecodeMethod> entry : inherited.entrySet()) {
            BytecodeMethod method = entry.getValue();
            if (method == null || method.isConstructor() || method.isAbstract() || method.isEliminated() || method.isStatic()) {
                continue;
            }
            // Only inherited methods that appendInheritedMethodAliases
            // actually generated a wrapper for — i.e. ones cls does not
            // declare itself. Otherwise cn1_<cls>_<m> is either cls's
            // own concrete declaration (handled above) or an abstract
            // declaration that has no function body to reference.
            if (declaresMethod(cls, method.getMethodName(), method.getSignature())) {
                continue;
            }
            resolvable.add(method.getMethodName() + method.getSignature());
        }

        Set<ByteCodeClass> visited = new HashSet<ByteCodeClass>();
        Set<String> emitted = new HashSet<String>();
        Deque<ByteCodeClass> pending = new ArrayDeque<ByteCodeClass>();
        // Enqueue the full ancestor set: every base class (except cls
        // itself — cls's own ids are emitted by appendMethod) and every
        // interface reachable from cls or any of its base classes.
        // Parent interfaces are enumerated inside the loop body so the
        // walk covers transitive interface inheritance too.
        ByteCodeClass baseWalk = cls.getBaseClassObject();
        while (baseWalk != null) {
            pending.add(baseWalk);
            List<ByteCodeClass> interfaces = baseWalk.getBaseInterfacesObject();
            if (interfaces != null) {
                for (ByteCodeClass iface : interfaces) {
                    if (iface != null) {
                        pending.add(iface);
                    }
                }
            }
            baseWalk = baseWalk.getBaseClassObject();
        }
        if (directInterfaces != null) {
            for (ByteCodeClass iface : directInterfaces) {
                if (iface != null) {
                    pending.add(iface);
                }
            }
        }
        while (!pending.isEmpty()) {
            ByteCodeClass ancestor = pending.pop();
            if (ancestor == null || ancestor == cls || !visited.add(ancestor)) {
                continue;
            }
            // A method id may legitimately be built from any ancestor in
            // cls's hierarchy, not just the ancestor that declares the
            // method. Java's ``invokevirtual X.m`` encodes X's name into
            // the method id regardless of which ancestor of X actually
            // declares m — so e.g. an ``invokevirtual AbstractList.size``
            // emits ``cn1_java_util_AbstractList_size`` even though size
            // is declared abstract on AbstractCollection. Collect every
            // method reachable from this ancestor (via its own
            // declarations or transitive inheritance) so we emit the
            // alias on cls for any id a call site might produce.
            Set<String> accessible = new HashSet<String>();
            collectAccessibleMethods(ancestor, accessible, new HashSet<ByteCodeClass>());
            String ancestorClassName = ancestor.getClsName();
            for (BytecodeMethod ownMethod : cls.getMethods()) {
                if (ownMethod == null || ownMethod.isAbstract() || ownMethod.isEliminated()
                        || ownMethod.isConstructor() || ownMethod.isStatic()) {
                    continue;
                }
                String name = ownMethod.getMethodName();
                String signature = ownMethod.getSignature();
                if (!resolvable.contains(name + signature) || !accessible.contains(name + signature)) {
                    continue;
                }
                String ancestorMethodId = JavascriptNameUtil.methodIdentifier(ancestorClassName, name, signature);
                String implMethodId = JavascriptNameUtil.methodIdentifier(cls.getClsName(), name, signature);
                if (ancestorMethodId.equals(implMethodId) || !emitted.add(ancestorMethodId)) {
                    continue;
                }
                appendAliasRegistration(regs, ancestorMethodId, implMethodId);
            }
            // Inherited resolvable methods: cls doesn't declare them,
            // but inherits them from a base class or interface default.
            // Previously ``appendInheritedMethodAliases`` emitted a
            // forwarding wrapper under cls's prefix and we pointed the
            // ancestor alias at that wrapper. The wrapper is gone now
            // — we point the ancestor alias directly at the method's
            // upstream declaring class's impl (``method.getClsName()``
            // via methodIdentifier), skipping cls's prefix entirely.
            for (Map.Entry<String, BytecodeMethod> entry : inherited.entrySet()) {
                BytecodeMethod method = entry.getValue();
                if (method == null || method.isConstructor() || method.isAbstract() || method.isEliminated()
                        || method.isStatic()) {
                    continue;
                }
                String name = method.getMethodName();
                String signature = method.getSignature();
                if (declaresMethod(cls, name, signature)) {
                    continue;
                }
                if (!resolvable.contains(name + signature) || !accessible.contains(name + signature)) {
                    continue;
                }
                String ancestorMethodId = JavascriptNameUtil.methodIdentifier(ancestorClassName, name, signature);
                String upstreamMethodId = JavascriptNameUtil.methodIdentifier(method.getClsName(), name, signature);
                if (ancestorMethodId.equals(upstreamMethodId) || !emitted.add(ancestorMethodId)) {
                    continue;
                }
                appendAliasRegistration(regs, ancestorMethodId, upstreamMethodId);
            }
            List<ByteCodeClass> parents = ancestor.getBaseInterfacesObject();
            if (parents != null) {
                for (ByteCodeClass parent : parents) {
                    if (parent != null) {
                        pending.push(parent);
                    }
                }
            }
        }
    }

    private static void collectAccessibleMethods(ByteCodeClass owner, Set<String> out, Set<ByteCodeClass> visited) {
        if (owner == null || !visited.add(owner)) {
            return;
        }
        for (BytecodeMethod method : owner.getMethods()) {
            if (method == null || method.isConstructor() || method.isEliminated() || method.isStatic()) {
                continue;
            }
            out.add(method.getMethodName() + method.getSignature());
        }
        collectAccessibleMethods(owner.getBaseClassObject(), out, visited);
        List<ByteCodeClass> parents = owner.getBaseInterfacesObject();
        if (parents != null) {
            for (ByteCodeClass parent : parents) {
                collectAccessibleMethods(parent, out, visited);
            }
        }
    }

    private static void collectInheritedMethodAliases(ByteCodeClass owner, Map<String, BytecodeMethod> out, Set<ByteCodeClass> visited) {
        if (owner == null || !visited.add(owner)) {
            return;
        }
        for (BytecodeMethod method : owner.getMethods()) {
            if (method == null || method.isConstructor() || method.isAbstract() || method.isEliminated()) {
                continue;
            }
            String key = method.getMethodName() + method.getSignature();
            if (!out.containsKey(key)) {
                out.put(key, method);
            }
        }
        collectInheritedMethodAliases(owner.getBaseClassObject(), out, visited);
        List<ByteCodeClass> baseInterfaces = owner.getBaseInterfacesObject();
        if (baseInterfaces != null) {
            for (ByteCodeClass iface : baseInterfaces) {
                collectInheritedMethodAliases(iface, out, visited);
            }
        }
    }

    private static boolean declaresMethod(ByteCodeClass cls, String name, String signature) {
        for (BytecodeMethod method : cls.getMethods()) {
            if (method.getMethodName().equals(name) && signature.equals(method.getSignature())) {
                return true;
            }
        }
        return false;
    }

    private static boolean appendStraightLineMethodBody(StringBuilder out, StringBuilder regs, ByteCodeClass cls, BytecodeMethod method,
            List<Instruction> instructions, String jsMethodName) {
        if (!isStraightLineEligible(method, instructions)) {
            return false;
        }
        try {
            StringBuilder setup = new StringBuilder();
            StringBuilder instructionBody = new StringBuilder();
            StringBuilder body = new StringBuilder();
            StraightLineContext ctx = new StraightLineContext(method.getMaxLocals(), method.getMaxStack());
            // The containing class (and every ancestor) is guaranteed
            // to be initialized by the time any method on ``cls``
            // runs. Pre-seed the straight-line emitter's
            // ``initializedClasses`` set so ``jvm.eI`` emissions for
            // these classes are elided — mirrors the logic in
            // ``appendInterpreterEnsureClassInitialized`` for the
            // switch-based emission path.
            ByteCodeClass walk = cls;
            int hops = 0;
            while (walk != null && hops++ < 64) {
                ctx.initializedClasses.add(walk.getClsName());
                walk = walk.getBaseClassObject();
            }
            if (!method.isStatic()) {
                setup.append("  let l0 = __cn1ThisObject;\n");
                ctx.localsInitialized[0] = true;
                ctx.localsUsed[0] = true;
            }
            List<ByteCodeMethodArg> arguments = method.getArguments();
            int localIndex = method.isStatic() ? 0 : 1;
            for (int i = 0; i < arguments.size(); i++) {
                setup.append("  let l").append(localIndex).append(" = __cn1Arg").append(i + 1).append(";\n");
                ctx.localsInitialized[localIndex] = true;
                ctx.localsUsed[localIndex] = true;
                localIndex++;
                if (arguments.get(i).isDoubleOrLong()) {
                    localIndex++;
                }
            }
            for (int i = 0; i < instructions.size(); i++) {
                Instruction instruction = instructions.get(i);
                if (!appendStraightLineInstruction(instructionBody, method, instruction, ctx)) {
                    return false;
                }
            }
            body.append(setup);
            // Stack slots and ``used but not arg-initialized`` locals
            // are always written before they're read (bytecode-verifier
            // invariant + no branches in a straight-line body), so the
            // initial ``= null`` is dead ceremony. ``let sN;`` /
            // ``let lN;`` saves 7 chars per declaration × ~5-10 per
            // method × ~1k straight-line methods ≈ 30-70 KiB.
            for (int i = 0; i < method.getMaxLocals(); i++) {
                if (!ctx.localsInitialized[i] && ctx.localsUsed[i]) {
                    body.append("  let l").append(i).append(";\n");
                }
            }
            for (int i = 0; i < ctx.getMaxObservedStack(); i++) {
                body.append("  let s").append(i).append(";\n");
            }
            if (method.isSynchronizedMethod()) {
                body.append("  let __cn1Monitor = ").append(method.isStatic() ? "jvm.getClassObject(\"" + cls.getClsName() + "\")" : "__cn1ThisObject").append(";\n");
                body.append("  yield* _me(__cn1Monitor);\n");
                body.append("  try {\n");
            }
            body.append(instructionBody);
            if (method.isSynchronizedMethod()) {
                body.append("  } finally {\n");
                body.append("    _mx(__cn1Monitor);\n");
                body.append("  }\n");
            }
            out.append(body);
            out.append("}\n");
            if ("__CLINIT__".equals(method.getMethodName())) {
                currentClassClinitFn = jsMethodName;
            }
            if (!method.isStatic() && !method.isConstructor()) {
                String dispatchId = JavascriptNameUtil.dispatchMethodIdentifier(method.getMethodName(), method.getSignature());
                appendPrimaryRegistration(regs, dispatchId, jsMethodName);
            }
            return true;
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("Straight-line JS lowering")) {
                return false;
            }
            throw ex;
        }
    }

    /**
     * Word-boundary containment check — matches ``ident`` exactly so
     * ``l1`` doesn't accidentally match inside ``l10``, ``locals1``,
     * or similar. Used to decide whether a straight-line slot/local
     * declaration is dead after peephole rewrites.
     */
    private static boolean containsWholeIdentifier(String body, String ident) {
        int from = 0;
        int len = ident.length();
        while ((from = body.indexOf(ident, from)) >= 0) {
            char before = from > 0 ? body.charAt(from - 1) : ' ';
            int endIdx = from + len;
            char after = endIdx < body.length() ? body.charAt(endIdx) : ' ';
            boolean leftOk = !Character.isLetterOrDigit(before) && before != '_' && before != '$';
            boolean rightOk = !Character.isLetterOrDigit(after) && after != '_' && after != '$';
            if (leftOk && rightOk) {
                return true;
            }
            from += len;
        }
        return false;
    }

    private static boolean isStraightLineEligible(BytecodeMethod method, List<Instruction> instructions) {
        if (method.isSynchronizedMethod()) {
            return false;
        }
        // ATHROW is straight-line-friendly: we just emit ``throw
        // stack.q();`` and anything past it is dead. The earlier
        // exclusion was conservative; allowing it lets many simple
        // ``throw new Foo(msg)`` methods skip the full switch/case
        // interpreter scaffolding.
        //
        // Jump / SwitchInstruction / TryCatch / MultiArray still
        // require the interpreter because they either branch or
        // implement a runtime exception table the straight-line
        // emitter has no place to hang.
        //
        // MONITORENTER/EXIT also need the interpreter so the stack
        // entry that holds the monitor can be tracked across the
        // implicit ``pc`` progression.
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            if (instruction instanceof Jump || instruction instanceof SwitchInstruction || instruction instanceof TryCatch
                    || instruction instanceof MultiArray) {
                return false;
            }
            if (instruction instanceof BasicInstruction) {
                int opcode = ((BasicInstruction) instruction).getOpcode();
                if (opcode == Opcodes.MONITORENTER || opcode == Opcodes.MONITOREXIT) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean appendStraightLineInstruction(StringBuilder out, BytecodeMethod method, Instruction instruction,
            StraightLineContext ctx) {
        if (instruction instanceof LabelInstruction || instruction instanceof LineNumber || instruction instanceof LocalVariable) {
            return true;
        }
        if (instruction instanceof BasicInstruction) {
            return appendStraightLineBasicInstruction(out, method, (BasicInstruction) instruction, ctx);
        }
        if (instruction instanceof VarOp) {
            return appendStraightLineVarInstruction(out, (VarOp) instruction, ctx);
        }
        if (instruction instanceof IInc) {
            IInc iinc = (IInc) instruction;
            ctx.localsUsed[iinc.getVar()] = true;
            out.append("  l").append(iinc.getVar()).append(" = (l").append(iinc.getVar()).append(" || 0) + ")
                    .append(iinc.getAmount()).append(";\n");
            return true;
        }
        if (instruction instanceof Ldc) {
            return appendStraightLineLdcInstruction(out, (Ldc) instruction, ctx);
        }
        if (instruction instanceof TypeInstruction) {
            return appendStraightLineTypeInstruction(out, (TypeInstruction) instruction, ctx);
        }
        if (instruction instanceof Field) {
            return appendStraightLineFieldInstruction(out, (Field) instruction, ctx);
        }
        if (instruction instanceof Invoke) {
            return appendStraightLineInvokeInstruction(out, (Invoke) instruction, ctx);
        }
        return false;
    }

    private static boolean appendStraightLineBasicInstruction(StringBuilder out, BytecodeMethod method, BasicInstruction instruction,
            StraightLineContext ctx) {
        switch (instruction.getOpcode()) {
            case Opcodes.NOP:
                return true;
            case Opcodes.ACONST_NULL:
                out.append("  ").append(ctx.push("null")).append(";\n");
                return true;
            case Opcodes.ICONST_M1:
                out.append("  ").append(ctx.push("-1")).append(";\n");
                return true;
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
                out.append("  ").append(ctx.push(Integer.toString(instruction.getOpcode() - Opcodes.ICONST_0))).append(";\n");
                return true;
            case Opcodes.LCONST_0:
                out.append("  ").append(ctx.push("0")).append(";\n");
                return true;
            case Opcodes.LCONST_1:
                out.append("  ").append(ctx.push("1")).append(";\n");
                return true;
            case Opcodes.FCONST_0:
            case Opcodes.DCONST_0:
                out.append("  ").append(ctx.push("0.0")).append(";\n");
                return true;
            case Opcodes.FCONST_1:
            case Opcodes.DCONST_1:
                out.append("  ").append(ctx.push("1.0")).append(";\n");
                return true;
            case Opcodes.FCONST_2:
                out.append("  ").append(ctx.push("2.0")).append(";\n");
                return true;
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                out.append("  ").append(ctx.push(Integer.toString(instruction.getValue()))).append(";\n");
                return true;
            case Opcodes.ILOAD:
            case Opcodes.LLOAD:
            case Opcodes.FLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ALOAD:
                ctx.localsUsed[instruction.getValue()] = true;
                out.append("  ").append(ctx.push("l" + instruction.getValue())).append(";\n");
                return true;
            case Opcodes.ISTORE:
            case Opcodes.LSTORE:
            case Opcodes.FSTORE:
            case Opcodes.DSTORE:
            case Opcodes.ASTORE:
                ctx.localsUsed[instruction.getValue()] = true;
                out.append("  l").append(instruction.getValue()).append(" = ").append(ctx.pop()).append(";\n");
                return true;
            case Opcodes.POP:
                ctx.pop();
                return true;
            case Opcodes.POP2:
                ctx.pop();
                ctx.pop();
                return true;
            case Opcodes.DUP: {
                String value = ctx.peek(0);
                String temp = ctx.nextTemp("__dup");
                out.append("  let ").append(temp).append(" = ").append(value).append(";\n");
                out.append("  ").append(ctx.push(temp)).append(";\n");
                return true;
            }
            case Opcodes.DUP_X1: {
                String v1 = ctx.pop();
                String v2 = ctx.pop();
                String t1 = ctx.nextTemp("__dup");
                String t2 = ctx.nextTemp("__dup");
                out.append("  let ").append(t1).append(" = ").append(v1).append(";\n");
                out.append("  let ").append(t2).append(" = ").append(v2).append(";\n");
                out.append("  ").append(ctx.push(t1)).append(";\n");
                out.append("  ").append(ctx.push(t2)).append(";\n");
                out.append("  ").append(ctx.push(t1)).append(";\n");
                return true;
            }
            case Opcodes.DUP_X2: {
                String v1 = ctx.pop();
                String v2 = ctx.pop();
                String v3 = ctx.pop();
                String t1 = ctx.nextTemp("__dup");
                String t2 = ctx.nextTemp("__dup");
                String t3 = ctx.nextTemp("__dup");
                out.append("  let ").append(t1).append(" = ").append(v1).append(";\n");
                out.append("  let ").append(t2).append(" = ").append(v2).append(";\n");
                out.append("  let ").append(t3).append(" = ").append(v3).append(";\n");
                out.append("  ").append(ctx.push(t1)).append(";\n");
                out.append("  ").append(ctx.push(t3)).append(";\n");
                out.append("  ").append(ctx.push(t2)).append(";\n");
                out.append("  ").append(ctx.push(t1)).append(";\n");
                return true;
            }
            case Opcodes.DUP2: {
                String v1 = ctx.pop();
                String v2 = ctx.pop();
                String t1 = ctx.nextTemp("__dup");
                String t2 = ctx.nextTemp("__dup");
                out.append("  let ").append(t1).append(" = ").append(v1).append(";\n");
                out.append("  let ").append(t2).append(" = ").append(v2).append(";\n");
                out.append("  ").append(ctx.push(t2)).append(";\n");
                out.append("  ").append(ctx.push(t1)).append(";\n");
                out.append("  ").append(ctx.push(t2)).append(";\n");
                out.append("  ").append(ctx.push(t1)).append(";\n");
                return true;
            }
            case Opcodes.DUP2_X1: {
                String v1 = ctx.pop();
                String v2 = ctx.pop();
                String v3 = ctx.pop();
                String t1 = ctx.nextTemp("__dup");
                String t2 = ctx.nextTemp("__dup");
                String t3 = ctx.nextTemp("__dup");
                out.append("  let ").append(t1).append(" = ").append(v1).append(";\n");
                out.append("  let ").append(t2).append(" = ").append(v2).append(";\n");
                out.append("  let ").append(t3).append(" = ").append(v3).append(";\n");
                out.append("  ").append(ctx.push(t2)).append(";\n");
                out.append("  ").append(ctx.push(t1)).append(";\n");
                out.append("  ").append(ctx.push(t3)).append(";\n");
                out.append("  ").append(ctx.push(t2)).append(";\n");
                out.append("  ").append(ctx.push(t1)).append(";\n");
                return true;
            }
            case Opcodes.DUP2_X2: {
                String v1 = ctx.pop();
                String v2 = ctx.pop();
                String v3 = ctx.pop();
                String v4 = ctx.pop();
                String t1 = ctx.nextTemp("__dup");
                String t2 = ctx.nextTemp("__dup");
                String t3 = ctx.nextTemp("__dup");
                String t4 = ctx.nextTemp("__dup");
                out.append("  let ").append(t1).append(" = ").append(v1).append(";\n");
                out.append("  let ").append(t2).append(" = ").append(v2).append(";\n");
                out.append("  let ").append(t3).append(" = ").append(v3).append(";\n");
                out.append("  let ").append(t4).append(" = ").append(v4).append(";\n");
                out.append("  ").append(ctx.push(t2)).append(";\n");
                out.append("  ").append(ctx.push(t1)).append(";\n");
                out.append("  ").append(ctx.push(t4)).append(";\n");
                out.append("  ").append(ctx.push(t3)).append(";\n");
                out.append("  ").append(ctx.push(t2)).append(";\n");
                out.append("  ").append(ctx.push(t1)).append(";\n");
                return true;
            }
            case Opcodes.SWAP: {
                String v1 = ctx.pop();
                String v2 = ctx.pop();
                out.append("  ").append(ctx.push(v1)).append(";\n");
                out.append("  ").append(ctx.push(v2)).append(";\n");
                return true;
            }
            case Opcodes.IADD:
                return emitBinary(out, ctx, "((%s|0) + (%s|0))");
            case Opcodes.ISUB:
                return emitBinary(out, ctx, "((%s|0) - (%s|0))");
            case Opcodes.IMUL:
                return emitBinary(out, ctx, "((%s|0) * (%s|0))");
            case Opcodes.LADD:
            case Opcodes.FADD:
            case Opcodes.DADD:
                return emitBinary(out, ctx, "(%s + %s)");
            case Opcodes.LSUB:
            case Opcodes.FSUB:
            case Opcodes.DSUB:
                return emitBinary(out, ctx, "(%s - %s)");
            case Opcodes.LMUL:
            case Opcodes.FMUL:
            case Opcodes.DMUL:
                return emitBinary(out, ctx, "(%s * %s)");
            case Opcodes.IDIV:
                return emitBinary(out, ctx, "(((%s|0) / (%s|0)) | 0)");
            case Opcodes.LDIV:
                return emitBinary(out, ctx, "Math.trunc(%s / %s)");
            case Opcodes.FDIV:
            case Opcodes.DDIV:
                return emitBinary(out, ctx, "(%s / %s)");
            case Opcodes.IREM:
                return emitBinary(out, ctx, "((%s|0) %% (%s|0))");
            case Opcodes.LREM:
            case Opcodes.FREM:
            case Opcodes.DREM:
                return emitBinary(out, ctx, "(%s %% %s)");
            case Opcodes.INEG:
                return emitUnary(out, ctx, "-(%s|0)");
            case Opcodes.LNEG:
            case Opcodes.FNEG:
            case Opcodes.DNEG:
                return emitUnary(out, ctx, "-%s");
            case Opcodes.ISHL:
                return emitBinary(out, ctx, "((%s|0) << (%s & 31))");
            case Opcodes.LSHL:
                return emitBinary(out, ctx, "(%s * Math.pow(2, %s & 63))");
            case Opcodes.ISHR:
                return emitBinary(out, ctx, "((%s|0) >> (%s & 31))");
            case Opcodes.LSHR:
                return emitBinary(out, ctx, "Math.trunc(%s / Math.pow(2, %s & 63))");
            case Opcodes.IUSHR:
                return emitBinary(out, ctx, "((%s >>> (%s & 31)) | 0)");
            case Opcodes.LUSHR:
                return emitBinary(out, ctx, "Math.floor((%s < 0 ? %s + 18446744073709551616 : %s) / Math.pow(2, %s & 63))",
                        true);
            case Opcodes.IAND:
                return emitBinary(out, ctx, "((%s|0) & (%s|0))");
            case Opcodes.LAND:
                return emitBinary(out, ctx, "(%s & %s)");
            case Opcodes.IOR:
                return emitBinary(out, ctx, "((%s|0) | (%s|0))");
            case Opcodes.LOR:
                return emitBinary(out, ctx, "(%s | %s)");
            case Opcodes.IXOR:
                return emitBinary(out, ctx, "((%s|0) ^ (%s|0))");
            case Opcodes.LXOR:
                return emitBinary(out, ctx, "(%s ^ %s)");
            case Opcodes.I2L:
            case Opcodes.I2F:
            case Opcodes.I2D:
            case Opcodes.L2F:
            case Opcodes.L2D:
            case Opcodes.F2D:
            case Opcodes.D2F:
                return true;
            case Opcodes.I2B:
                return emitUnary(out, ctx, "((%s << 24) >> 24)");
            case Opcodes.I2C:
                return emitUnary(out, ctx, "(%s & 65535)");
            case Opcodes.I2S:
                return emitUnary(out, ctx, "((%s << 16) >> 16)");
            case Opcodes.L2I:
            case Opcodes.F2I:
            case Opcodes.D2I:
                return emitUnary(out, ctx, "(%s | 0)");
            case Opcodes.F2L:
            case Opcodes.D2L:
                return true;
            case Opcodes.LCMP:
                return emitBinary(out, ctx, "(%s < %s ? -1 : (%s > %s ? 1 : 0))", true);
            case Opcodes.FCMPL:
            case Opcodes.DCMPL:
                return emitBinary(out, ctx, "((isNaN(%s) || isNaN(%s)) ? -1 : (%s < %s ? -1 : (%s > %s ? 1 : 0)))", true);
            case Opcodes.FCMPG:
            case Opcodes.DCMPG:
                return emitBinary(out, ctx, "((isNaN(%s) || isNaN(%s)) ? 1 : (%s < %s ? -1 : (%s > %s ? 1 : 0)))", true);
            case Opcodes.IRETURN:
            case Opcodes.ARETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
                out.append("  return ").append(ctx.pop()).append(";\n");
                return true;
            case Opcodes.RETURN:
                out.append("  return null;\n");
                return true;
            case Opcodes.ATHROW:
                out.append("  throw ").append(ctx.pop()).append(";\n");
                return true;
            case Opcodes.ARRAYLENGTH:
                return emitUnary(out, ctx, "%s.length");
            case Opcodes.AALOAD:
            case Opcodes.IALOAD:
            case Opcodes.LALOAD:
            case Opcodes.FALOAD:
            case Opcodes.DALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD: {
                String idx = ctx.pop();
                String arr = ctx.pop();
                out.append("  ").append(ctx.push("_A(" + arr + ", " + idx + ")")).append(";\n");
                return true;
            }
            case Opcodes.AASTORE:
            case Opcodes.IASTORE:
            case Opcodes.LASTORE:
            case Opcodes.FASTORE:
            case Opcodes.DASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE: {
                String value = ctx.pop();
                String idx = ctx.pop();
                String arr = ctx.pop();
                out.append("  _T(").append(arr).append(", ").append(idx).append(", ").append(value).append(");\n");
                return true;
            }
            default:
                return false;
        }
    }

    private static boolean emitBinary(StringBuilder out, StraightLineContext ctx, String format) {
        return emitBinary(out, ctx, format, false);
    }

    private static boolean emitBinary(StringBuilder out, StraightLineContext ctx, String format, boolean repeatedArgs) {
        String b = ctx.pop();
        String a = ctx.pop();
        String expr;
        if (repeatedArgs) {
            expr = String.format(format, a, b, a, b, a, b);
        } else {
            expr = String.format(format, a, b);
        }
        out.append("  ").append(ctx.push(expr)).append(";\n");
        return true;
    }

    private static boolean emitUnary(StringBuilder out, StraightLineContext ctx, String format) {
        String value = ctx.pop();
        out.append("  ").append(ctx.push(String.format(format, value))).append(";\n");
        return true;
    }

    private static boolean appendStraightLineVarInstruction(StringBuilder out, VarOp instruction, StraightLineContext ctx) {
        switch (instruction.getOpcode()) {
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                out.append("  ").append(ctx.push(Integer.toString(instruction.getIndex()))).append(";\n");
                return true;
            case Opcodes.NEWARRAY: {
                String size = ctx.pop();
                out.append("  ").append(ctx.push("_j(" + size + ", \"" + primitiveArrayType(instruction.getIndex()) + "\", 1)")).append(";\n");
                return true;
            }
            case Opcodes.ILOAD:
            case Opcodes.LLOAD:
            case Opcodes.FLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ALOAD:
                ctx.localsUsed[instruction.getIndex()] = true;
                out.append("  ").append(ctx.push("l" + instruction.getIndex())).append(";\n");
                return true;
            case Opcodes.ISTORE:
            case Opcodes.LSTORE:
            case Opcodes.FSTORE:
            case Opcodes.DSTORE:
            case Opcodes.ASTORE:
                ctx.localsUsed[instruction.getIndex()] = true;
                out.append("  l").append(instruction.getIndex()).append(" = ").append(ctx.pop()).append(";\n");
                return true;
            default:
                return false;
        }
    }

    private static boolean appendStraightLineLdcInstruction(StringBuilder out, Ldc instruction, StraightLineContext ctx) {
        Object value = instruction.getValue();
        if (value instanceof String) {
            out.append("  ").append(ctx.push("_L(\"" + JavascriptNameUtil.escapeJs((String) value) + "\")")).append(";\n");
            return true;
        }
        if (value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double) {
            out.append("  ").append(ctx.push(value.toString())).append(";\n");
            return true;
        }
        if (value instanceof Type) {
            Type type = (Type) value;
            if (type.getSort() == Type.OBJECT) {
                out.append("  ").append(ctx.push("jvm.getClassObject(\"" + JavascriptNameUtil.sanitizeClassName(type.getInternalName()) + "\")")).append(";\n");
                return true;
            }
        }
        return false;
    }

    private static boolean appendStraightLineTypeInstruction(StringBuilder out, TypeInstruction instruction, StraightLineContext ctx) {
        String typeName = JavascriptNameUtil.runtimeTypeName(instruction.getTypeName());
        switch (instruction.getOpcode()) {
            case Opcodes.NEW:
                out.append("  ").append(ctx.push("_O(\"" + typeName + "\")")).append(";\n");
                return true;
            case Opcodes.ANEWARRAY: {
                String size = ctx.pop();
                out.append("  ").append(ctx.push("_j(" + size + ", \"" + typeName + "\", 1)")).append(";\n");
                return true;
            }
            case Opcodes.CHECKCAST: {
                String value = ctx.peek(0);
                appendDirectCheckCast(out, "  ", value, typeName, "throw new Error(\"ClassCastException\")");
                return true;
            }
            case Opcodes.INSTANCEOF: {
                String value = ctx.pop();
                out.append("  ").append(ctx.push(directInstanceOfExpression(value, typeName) + " ? 1 : 0")).append(";\n");
                return true;
            }
            default:
                return false;
        }
    }

    private static boolean appendStraightLineFieldInstruction(StringBuilder out, Field field, StraightLineContext ctx) {
        String rawOwner = field.getOwner();
        String fieldName = field.getFieldName();
        String instanceOwner = resolveFieldOwner(rawOwner, fieldName);
        String owner = JavascriptNameUtil.sanitizeClassName(rawOwner);
        String propertyName = JavascriptNameUtil.fieldProperty(instanceOwner, fieldName);
        switch (field.getOpcode()) {
            case Opcodes.GETSTATIC:
                appendStraightLineEnsureClassInitialized(out, ctx, owner);
                out.append("  ").append(ctx.push("_S[\"" + owner + "\"][\"" + fieldName + "\"]")).append(";\n");
                return true;
            case Opcodes.PUTSTATIC:
                appendStraightLineEnsureClassInitialized(out, ctx, owner);
                out.append("  _S[\"").append(owner).append("\"][\"").append(fieldName).append("\"] = ")
                        .append(ctx.pop()).append(";\n");
                return true;
            case Opcodes.GETFIELD: {
                String target = ctx.pop();
                out.append("  ").append(ctx.push(target + "[\"" + propertyName + "\"]")).append(";\n");
                return true;
            }
            case Opcodes.PUTFIELD: {
                String value = ctx.pop();
                String target = ctx.pop();
                out.append("  ").append(target).append("[\"").append(propertyName).append("\"] = ").append(value).append(";\n");
                return true;
            }
            default:
                return false;
        }
    }

    private static void appendStraightLineEnsureClassInitialized(StringBuilder out, StraightLineContext ctx, String owner) {
        if (ctx.initializedClasses.add(owner)) {
            out.append("  _I(\"").append(owner).append("\");\n");
        }
    }

    private static boolean appendStraightLineInvokeInstruction(StringBuilder out, Invoke invoke, StraightLineContext ctx) {
        String declaredOwner = invoke.getOwner();
        String directOwner = resolveDirectInvokeOwner(invoke);
        String owner = JavascriptNameUtil.sanitizeClassName(directOwner);
        String methodOwner = (invoke.getOpcode() == Opcodes.INVOKEVIRTUAL || invoke.getOpcode() == Opcodes.INVOKEINTERFACE)
                ? declaredOwner
                : directOwner;
        String methodId = JavascriptNameUtil.methodIdentifier(methodOwner, invoke.getName(), invoke.getDesc());
        String methodBodyId = jsStaticMethodBodyIdentifier(methodOwner, invoke.getName(), invoke.getDesc());
        List<String> args = JavascriptNameUtil.argumentTypes(invoke.getDesc());
        boolean hasReturn = invoke.getDesc().charAt(invoke.getDesc().length() - 1) != 'V';
        String[] argValues = new String[args.size()];
        for (int i = args.size() - 1; i >= 0; i--) {
            argValues[i] = ctx.pop();
        }
        String target = null;
        switch (invoke.getOpcode()) {
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKEINTERFACE:
            case Opcodes.INVOKESPECIAL:
                target = ctx.pop();
                break;
            case Opcodes.INVOKESTATIC:
                break;
            default:
                return false;
        }
        if (invoke.getOpcode() == Opcodes.INVOKEVIRTUAL || invoke.getOpcode() == Opcodes.INVOKEINTERFACE) {
            // Straight-line INVOKEVIRTUAL / INVOKEINTERFACE: emit one cn1_iv*
            // helper call instead of __classDef/resolveVirtual boilerplate.
            // See appendCompactVirtualDispatch for the shared emission rules.
            // Uses the class-free dispatch id so the runtime walk in
            // ``resolveVirtual`` handles inheritance without per-class alias
            // entries.
            String dispatchId = JavascriptNameUtil.dispatchMethodIdentifier(invoke.getName(), invoke.getDesc());
            if (hasReturn) {
                out.append("  {\n");
                appendCompactVirtualDispatch(out, "    ", dispatchId, argValues.length, true, target, false, argValues);
                out.append("    ").append(ctx.push("__result")).append(";\n");
                out.append("  }\n");
            } else {
                appendCompactVirtualDispatch(out, "  ", dispatchId, argValues.length, false, target, false, argValues);
            }
            return true;
        }
        if (invoke.getOpcode() == Opcodes.INVOKESTATIC) {
            appendStraightLineEnsureClassInitialized(out, ctx, owner);
        }
        // For INVOKESTATIC, pick between the public wrapper and the
        // ``__impl`` body at emit time based on whether the target is
        // native. Non-native statics have a real ``__impl`` function
        // (the body we want to call directly, skipping the wrapper's
        // redundant ``jvm.eI`` — the interpreter already emitted one
        // above). Native statics only have the public wrapper (their
        // ``__impl`` name isn't declared), so calling that is the
        // only safe option. Previously emitted
        // ``typeof X==="function"?X:Y`` (~30 chars) at every site; now
        // either ``methodBodyId`` or ``methodId`` directly.
        String invokedName = invoke.getOpcode() == Opcodes.INVOKESTATIC
                ? (isInvokeTargetNative(invoke) ? methodId : methodBodyId)
                : methodId;
        // Sync targets are invoked directly; generator targets keep the
        // ``yield*`` ceremony so the cooperative scheduler can interleave
        // them with other threads.
        String yieldPrefix = isInvokeSuspending(invoke) ? "yield* " : "";
        if (hasReturn) {
            out.append("  { let __result = ").append(yieldPrefix).append(invokedName).append("(");
        } else {
            out.append("  { ").append(yieldPrefix).append(invokedName).append("(");
        }
        appendInvocationArgumentExpressions(out, target, argValues);
        out.append(");");
        if (hasReturn) {
            out.append(" ").append(ctx.push("__result")).append(";");
        }
        out.append(" }\n");
        return true;
    }

    private static void appendInvocationArgumentExpressions(StringBuilder out, String target, String[] args) {
        boolean first = true;
        if (target != null) {
            out.append(target);
            first = false;
        }
        for (int i = 0; i < args.length; i++) {
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append(args[i]);
        }
    }

    private static final class StraightLineContext {
        private final boolean[] localsInitialized;
        private final boolean[] localsUsed;
        private final Set<String> initializedClasses;
        private int sp;
        private int maxObservedStack;
        private int nextTempId;

        private StraightLineContext(int maxLocals, int maxStack) {
            this.localsInitialized = new boolean[Math.max(1, maxLocals)];
            this.localsUsed = new boolean[Math.max(1, maxLocals)];
            this.initializedClasses = new HashSet<String>();
            this.sp = 0;
            this.maxObservedStack = 0;
            this.nextTempId = 0;
        }

        private String push(String expression) {
            String slot = "s" + sp++;
            if (sp > maxObservedStack) {
                maxObservedStack = sp;
            }
            return slot + " = " + expression;
        }

        private String pop() {
            sp--;
            if (sp < 0) {
                throw new IllegalStateException("Straight-line JS lowering stack underflow");
            }
            return "s" + sp;
        }

        private String peek(int depth) {
            int index = sp - 1 - depth;
            if (index < 0) {
                throw new IllegalStateException("Straight-line JS lowering stack underflow");
            }
            return "s" + index;
        }

        private int getMaxObservedStack() {
            return maxObservedStack;
        }

        private String nextTemp(String prefix) {
            return prefix + (nextTempId++);
        }
    }

    private static void appendTryCatchTable(StringBuilder out, List<Instruction> instructions, Map<Label, Integer> labelToIndex) {
        // Short property names (s/e/h/t for start/end/handler/type)
        // save ~15 chars per entry × ~700 entries ≈ 10 KiB. Runtime
        // ``findExceptionHandler`` / ``_E`` read the short names
        // directly.
        out.append("  let __cn1TryCatch = [");
        boolean first = true;
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            if (!(instruction instanceof TryCatch)) {
                continue;
            }
            TryCatch tryCatch = (TryCatch) instruction;
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append("{s:").append(resolveLabelIndex(labelToIndex, tryCatch.getStart(), "try start"));
            out.append(",e:").append(resolveLabelIndex(labelToIndex, tryCatch.getEnd(), "try end"));
            out.append(",h:").append(resolveLabelIndex(labelToIndex, tryCatch.getHandler(), "try handler"));
            if (tryCatch.getType() != null) {
                out.append(",t:\"").append(JavascriptNameUtil.runtimeTypeName(tryCatch.getType())).append("\"");
            }
            out.append("}");
        }
        out.append("];\n");
    }

    /**
     * Locate the no-arg ``<init>`` constructor on this class, if one
     * survives RTA. Used by {@link #appendClassRegistration} to attach
     * a direct function reference to the class def under ``t:`` so
     * reflective construction (``Class.newInstance()``,
     * ``jvm.createException()``) doesn't have to reconstruct the
     * mangled global name from a string concat that no longer matches
     * after the post-translation identifier mangler runs.
     */
    private static BytecodeMethod findNoArgConstructor(ByteCodeClass cls) {
        for (BytecodeMethod method : cls.getMethods()) {
            if (!"__INIT__".equals(method.getMethodName())) {
                continue;
            }
            if (method.isEliminated() || method.isAbstract() || method.isNative()) {
                continue;
            }
            // Empty parameter list = ``()V`` descriptor. ``isStatic`` is
            // always false for ``<init>`` (constructors aren't static).
            if (method.getArguments() == null || method.getArguments().isEmpty()) {
                return method;
            }
        }
        return null;
    }

    private static String jsMethodIdentifier(ByteCodeClass cls, BytecodeMethod method) {
        return JavascriptNameUtil.methodIdentifier(cls.getClsName(), method.getMethodName(), method.getSignature());
    }

    private static String jsMethodBodyIdentifier(ByteCodeClass cls, BytecodeMethod method) {
        if (isWrappedStaticMethod(method)) {
            return jsStaticMethodBodyIdentifier(cls.getClsName(), method.getMethodName(), method.getSignature());
        }
        return jsMethodIdentifier(cls, method);
    }

    private static String jsStaticMethodBodyIdentifier(String owner, String name, String desc) {
        return JavascriptNameUtil.methodIdentifier(owner, name, desc) + "__impl";
    }

private static void appendNativeStubIfNeeded(StringBuilder out, ByteCodeClass cls, BytecodeMethod method) {
        String jsMethodName = jsMethodIdentifier(cls, method);
        if (method.isJsBodyMethod()) {
            appendJsBodyMethod(out, cls, method, jsMethodName);
        } else {
            JavascriptNativeRegistry.NativeCategory category = JavascriptNativeRegistry.categoryFor(jsMethodName);
            if (category == JavascriptNativeRegistry.NativeCategory.RUNTIME_IMPLEMENTED) {
                return;
            }
            String reason = JavascriptNativeRegistry.unsupportedReason(jsMethodName);
            out.append("if (typeof ").append(jsMethodName).append(" === \"undefined\") {\n");
            out.append("  ").append(jsMethodName).append(" = function*(");
            boolean first = true;
            if (!method.isStatic()) {
                out.append("__cn1ThisObject");
                first = false;
            }
            List<ByteCodeMethodArg> arguments = method.getArguments();
            for (int i = 0; i < arguments.size(); i++) {
                if (!first) {
                    out.append(", ");
                }
                first = false;
                out.append("__cn1Arg").append(i + 1);
            }
            out.append(") { ");
            if (category == JavascriptNativeRegistry.NativeCategory.HOST_HOOK) {
                out.append("return yield jvm.invokeHostNative(\"").append(jsMethodName).append("\", [");
                boolean firstArg = true;
                if (!method.isStatic()) {
                    out.append("__cn1ThisObject");
                    firstArg = false;
                }
                for (int i = 0; i < arguments.size(); i++) {
                    if (!firstArg) {
                        out.append(", ");
                    }
                    firstArg = false;
                    out.append("__cn1Arg").append(i + 1);
                }
                out.append("]);");
            } else {
                out.append("throw new Error(\"");
                if (reason == null) {
                    out.append("Missing javascript native method ").append(jsMethodName);
                } else {
                    out.append(JavascriptNameUtil.escapeJs(reason));
                }
                out.append("\");");
            }
            out.append(" };\n");
            out.append("}\n");
        }
    }

private static void appendJsBodyMethod(StringBuilder out, ByteCodeClass cls, BytecodeMethod method, String jsMethodName) {
        String script = method.getJsBodyScript();
        String[] params = method.getJsBodyParams();
        ByteCodeMethodArg returnType = method.getReturnType();
        boolean isVoid = returnType == null || returnType.isVoid();
        
        out.append("if (typeof ").append(jsMethodName).append(" === \"undefined\") {\n");
        out.append("  ").append(jsMethodName).append(" = function*(");
        
        java.util.List<ByteCodeMethodArg> arguments = method.getArguments();
        int paramOffset = 0;
        boolean first = true;
        
        if (!method.isStatic()) {
            out.append("__cn1ThisObject");
            first = false;
            paramOffset = 1;
        }
        
        for (int i = 0; i < arguments.size(); i++) {
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append("__cn1Arg").append(i + 1);
        }
        out.append(") {\n");
        
        out.append("    ");
        
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                String paramName = params[i];
                int argIndex = paramOffset + i;
                ByteCodeMethodArg argType = i < arguments.size() ? arguments.get(i) : null;
                if (argType != null && argType.isObject()) {
                    out.append("let ").append(paramName).append(" = jvm.unwrapJsValue(__cn1Arg").append(argIndex + 1).append("); ");
                } else {
                    out.append("let ").append(paramName).append(" = __cn1Arg").append(argIndex + 1).append("; ");
                }
            }
            out.append("\n    ");
        }
        
        if (!isVoid) {
            out.append("let __jsBodyResult = (function() { ").append(script).append(" }).call(this);\n");
            String returnTypeName = returnType.getTypeName();
            String jsReturnType;
            if (returnTypeName != null) {
                jsReturnType = JavascriptNameUtil.sanitizeClassName(returnTypeName);
            } else {
                Class primitiveType = returnType.getPrimitiveType();
                if (primitiveType == Integer.TYPE) {
                    jsReturnType = "int";
                } else if (primitiveType == Long.TYPE) {
                    jsReturnType = "long";
                } else if (primitiveType == Double.TYPE) {
                    jsReturnType = "double";
                } else if (primitiveType == Float.TYPE) {
                    jsReturnType = "float";
                } else if (primitiveType == Boolean.TYPE) {
                    jsReturnType = "boolean";
                } else if (primitiveType == Byte.TYPE) {
                    jsReturnType = "byte";
                } else if (primitiveType == Short.TYPE) {
                    jsReturnType = "short";
                } else if (primitiveType == Character.TYPE) {
                    jsReturnType = "char";
                } else {
                    jsReturnType = "java_lang_Object";
                }
            }
            out.append("    return jvm.wrapJsResult(__jsBodyResult, \"").append(jsReturnType).append("\");\n");
        } else {
            out.append(script).append("\n");
        }
        
        out.append("  }\n");
        out.append("}\n");
    }

    /**
     * Instructions that don't emit any meaningful JS on their own — they're
     * debug/metadata bytecode nodes that translate to a plain PC increment.
     * Callers elide the per-instruction case block for these and let the
     * switch fall through to the next real instruction's body. See the
     * emission loop in appendMethodJavaScript for the rationale.
     */
    private static boolean methodHasTryCatch(List<Instruction> instructions) {
        for (int i = 0; i < instructions.size(); i++) {
            if (instructions.get(i) instanceof TryCatch) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPcSkippableNoOp(Instruction instruction) {
        return instruction instanceof LabelInstruction
                || instruction instanceof LineNumber
                || instruction instanceof LocalVariable
                || instruction instanceof TryCatch;
    }

    /**
     * Collect every instruction index that might be branched to. That
     * includes explicit {@link Jump} / {@link SwitchInstruction}
     * targets, the instruction following any branch or throw (the
     * fall-through PC for conditional jumps and a re-entry point that
     * incoming gotos may target even after an unconditional branch),
     * exception-handler starts, and every label referenced by the
     * method's try/catch table. Any index NOT in this set can be
     * reached only by fall-through inside the switch, so its case
     * label is purely decorative and a preceding ``pc = i; break;``
     * may be omitted.
     */
    private static java.util.Set<Integer> computeJumpTargets(List<Instruction> instructions, Map<Label, Integer> labelToIndex) {
        java.util.Set<Integer> targets = new java.util.HashSet<Integer>();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instr = instructions.get(i);
            if (instr instanceof Jump) {
                Label target = ((Jump) instr).getLabel();
                Integer idx = target == null ? null : labelToIndex.get(target);
                if (idx != null) {
                    targets.add(idx);
                }
                // Conditional jumps fall through to ``i+1`` when the
                // predicate is false, so ``i+1`` must be a real case
                // label. For ``GOTO`` (the unconditional branch in
                // this family), control leaves the basic block via
                // the target label and reaches ``i+1`` only if some
                // OTHER instruction branches there — in which case
                // that other instruction already adds it. JSR/RET are
                // kept conservatively on the ``always add i+1`` path
                // because subroutine return semantics are trickier.
                if (instr.getOpcode() != Opcodes.GOTO && i + 1 < instructions.size()) {
                    targets.add(i + 1);
                }
            } else if (instr instanceof SwitchInstruction) {
                SwitchInstruction sw = (SwitchInstruction) instr;
                Label dflt = sw.getDefaultLabel();
                if (dflt != null) {
                    Integer idx = labelToIndex.get(dflt);
                    if (idx != null) {
                        targets.add(idx);
                    }
                }
                Label[] labels = sw.getLabels();
                if (labels != null) {
                    for (Label label : labels) {
                        Integer idx = label == null ? null : labelToIndex.get(label);
                        if (idx != null) {
                            targets.add(idx);
                        }
                    }
                }
                if (i + 1 < instructions.size()) {
                    targets.add(i + 1);
                }
            } else if (instr instanceof TryCatch) {
                TryCatch tc = (TryCatch) instr;
                Integer start = tc.getStart() == null ? null : labelToIndex.get(tc.getStart());
                Integer end = tc.getEnd() == null ? null : labelToIndex.get(tc.getEnd());
                Integer handler = tc.getHandler() == null ? null : labelToIndex.get(tc.getHandler());
                if (start != null) targets.add(start);
                if (end != null) targets.add(end);
                if (handler != null) targets.add(handler);
            } else if (instr instanceof BasicInstruction) {
                int op = instr.getOpcode();
                if (op == Opcodes.ATHROW || op == Opcodes.RETURN || op == Opcodes.IRETURN
                        || op == Opcodes.LRETURN || op == Opcodes.FRETURN || op == Opcodes.DRETURN
                        || op == Opcodes.ARETURN) {
                    if (i + 1 < instructions.size()) {
                        targets.add(i + 1);
                    }
                }
            }
            // Any non-terminal instruction whose emission keeps its
            // ``pc = i + 1; break;`` tail implicitly jumps to ``i+1``.
            // Safe-strip only elides that tail for non-throwing
            // instructions, so throwing non-terminal ops (ANEWARRAY,
            // NEW, CHECKCAST, INVOKE*, GETFIELD, PUTSTATIC, IDIV, …)
            // always leave a runtime pc-jump to ``i+1``. Without this
            // marker the case-merge pass assumes ``i+1`` is dead code
            // and drops the instruction there — producing a switch
            // whose body targets a missing case label and silently
            // falls through to ``default:return``.
            if (!(instr instanceof Jump) && !(instr instanceof SwitchInstruction)
                    && !(instr instanceof LabelInstruction) && !(instr instanceof LineNumber)
                    && !(instr instanceof LocalVariable) && !(instr instanceof TryCatch)
                    && !isTerminatingInstruction(instr) && !isNonThrowingInstruction(instr)
                    && i + 1 < instructions.size()) {
                targets.add(i + 1);
            }
        }
        return targets;
    }

    /**
     * True for instructions that write their own {@code pc = ...;
     * break;} tail (or otherwise transfer control without the
     * translator's standard ``pc = index + 1; break;`` suffix). These
     * cannot participate in case-merging because stripping a tail
     * that isn't there corrupts their control flow.
     */
    private static boolean hasExceptionHandlers(BytecodeMethod method) {
        List<Instruction> insns = method.getInstructions();
        if (insns == null) {
            return false;
        }
        for (Instruction instr : insns) {
            if (instr instanceof TryCatch) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when the case block at {@code blockStart} contains at least
     * one instruction that can throw (i.e. needs an accurate {@code pc}
     * for {@link findExceptionHandler} dispatch). Pure no-op /
     * non-throwing blocks don't need the pin and we save the bytes.
     */
    private static boolean needsPcPin(List<Instruction> instructions, int blockStart) {
        for (int j = blockStart; j < instructions.size(); j++) {
            Instruction instr = instructions.get(j);
            if (instr instanceof Jump || instr instanceof SwitchInstruction
                    || isTerminatingInstruction(instr)) {
                return false;
            }
            if (!isNonThrowingInstruction(instr)) {
                return true;
            }
            // Stop scanning once we hit something that obviously
            // ends the block — non-throwing terminating ops won't
            // benefit from a pc pin anyway.
        }
        return false;
    }

    private static boolean isTerminatingInstruction(Instruction instruction) {
        if (instruction instanceof Jump || instruction instanceof SwitchInstruction) {
            return true;
        }
        if (instruction instanceof BasicInstruction) {
            int op = instruction.getOpcode();
            return op == Opcodes.ATHROW
                    || op == Opcodes.RETURN
                    || op == Opcodes.IRETURN
                    || op == Opcodes.LRETURN
                    || op == Opcodes.FRETURN
                    || op == Opcodes.DRETURN
                    || op == Opcodes.ARETURN;
        }
        return false;
    }

    /**
     * True when the instruction cannot throw any exception — i.e.
     * merging it into a preceding case block without advancing pc
     * preserves exception-dispatch semantics. If a later instruction
     * in the same case block throws, the frame's pc still points at
     * the earlier non-throwing op; but since that earlier op couldn't
     * have thrown anything itself, the handler lookup at the earlier
     * pc resolves to the same handler as the lookup at the later pc
     * (the active try/catch set is the same throughout a basic block
     * — try/catch start/end boundaries are already in jumpTargets, so
     * nextIsNewBlock=true at those points and strip is suppressed).
     *
     * <p>Conservative: only pure-compute opcodes (local var load/store,
     * stack manipulation, integer / float arithmetic without divide,
     * integer widening, boolean comparisons, INSTANCEOF, IINC) are
     * marked non-throwing. Anything that touches memory
     * (GETFIELD/PUTFIELD, array ops), invokes a method, checks a
     * cast, divides, throws, enters/exits a monitor, or triggers
     * class init is considered throwing.
     */
    private static boolean isPrimitiveDescriptor(String desc) {
        if (desc.length() == 1 && "ZCBSIFJD".indexOf(desc) >= 0) {
            return true;
        }
        return desc.startsWith("JAVA_") && !desc.contains("[]");
    }

    private static boolean isNonThrowingInstruction(Instruction instruction) {
        if (instruction instanceof Jump || instruction instanceof SwitchInstruction
                || instruction instanceof Invoke || instruction instanceof Field
                || instruction instanceof TypeInstruction || instruction instanceof MultiArray) {
            return false;
        }
        if (instruction instanceof VarOp || instruction instanceof IInc || instruction instanceof Ldc) {
            return true;
        }
        if (instruction instanceof BasicInstruction) {
            int op = instruction.getOpcode();
            switch (op) {
                case Opcodes.NOP:
                case Opcodes.ACONST_NULL:
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
                case Opcodes.DUP_X1:
                case Opcodes.DUP_X2:
                case Opcodes.DUP2:
                case Opcodes.DUP2_X1:
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
                case Opcodes.FCMPL:
                case Opcodes.FCMPG:
                case Opcodes.DCMPL:
                case Opcodes.DCMPG:
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    /**
     * Strip a trailing {@code pc = <nextIndex>; break;} from an
     * instruction's emitted body. The pattern is emitted by every
     * non-branch, non-throw sub-emitter (basic ops, locals, fields,
     * invokes, multi-array, etc.) and always uses the literal
     * advance to ``nextIndex``. We match the exact sub-string with
     * surrounding whitespace so the strip is unambiguous — the
     * instruction's own code can legitimately contain the words
     * ``pc``/``break`` for unrelated reasons (diagnostics, bridge
     * emission) and we don't want to hit those.
     */
    private static String stripTrailingPcAdvance(String body, int nextIndex) {
        String pattern = "pc = " + nextIndex + "; break;";
        int idx = body.lastIndexOf(pattern);
        if (idx < 0) {
            return body;
        }
        int end = idx + pattern.length();
        // Also swallow a trailing newline so the next instruction's
        // body starts cleanly on a fresh line.
        if (end < body.length() && body.charAt(end) == '\n') {
            end++;
        }
        // And any same-line whitespace preceding the tail so we don't
        // leave a row of dangling spaces.
        int stripStart = idx;
        while (stripStart > 0 && (body.charAt(stripStart - 1) == ' ' || body.charAt(stripStart - 1) == '\t')) {
            stripStart--;
        }
        return body.substring(0, stripStart) + body.substring(end);
    }

    private static Map<Label, Integer> buildLabelMap(List<Instruction> instructions) {
        Map<Label, Integer> out = new HashMap<Label, Integer>();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            if (instruction instanceof LabelInstruction) {
                out.put(((LabelInstruction) instruction).getLabel(), Integer.valueOf(i));
            }
        }
        return out;
    }

    private static void appendInstruction(StringBuilder out, BytecodeMethod method, List<Instruction> allInstructions,
            Map<Label, Integer> labelToIndex, Instruction instruction, int index, boolean usesClassInitCache,
            boolean usesVirtualDispatchCache) {
        if (instruction instanceof LabelInstruction || instruction instanceof LineNumber || instruction instanceof LocalVariable
                || instruction instanceof TryCatch) {
            out.append("        pc = ").append(index + 1).append("; break;\n");
            return;
        }
        if (instruction instanceof BasicInstruction) {
            appendBasicInstruction(out, method, (BasicInstruction) instruction, index);
            return;
        }
        if (instruction instanceof VarOp) {
            appendVarInstruction(out, (VarOp) instruction, index);
            return;
        }
        if (instruction instanceof IInc) {
            IInc iinc = (IInc) instruction;
            out.append("        locals[").append(iinc.getVar()).append("] = (locals[").append(iinc.getVar())
                    .append("] || 0) + ").append(iinc.getAmount()).append(";\n");
            out.append("        pc = ").append(index + 1).append("; break;\n");
            return;
        }
        if (instruction instanceof Ldc) {
            appendLdcInstruction(out, (Ldc) instruction, index);
            return;
        }
        if (instruction instanceof TypeInstruction) {
            appendTypeInstruction(out, (TypeInstruction) instruction, index);
            return;
        }
        if (instruction instanceof Field) {
            appendFieldInstruction(out, (Field) instruction, index, usesClassInitCache);
            return;
        }
        if (instruction instanceof Jump) {
            appendJumpInstruction(out, (Jump) instruction, labelToIndex, index);
            return;
        }
        if (instruction instanceof Invoke) {
            appendInvokeInstruction(out, (Invoke) instruction, index, usesClassInitCache, usesVirtualDispatchCache);
            return;
        }
        if (instruction instanceof SwitchInstruction) {
            appendSwitchInstruction(out, (SwitchInstruction) instruction, labelToIndex, index);
            return;
        }
        if (instruction instanceof MultiArray) {
            appendMultiArrayInstruction(out, (MultiArray) instruction, index);
            return;
        }
        throw new IllegalArgumentException("Unsupported instruction type in javascript output: "
                + instruction.getClass().getName() + " for " + method.getMethodIdentifier());
    }

    private static void appendMultiArrayInstruction(StringBuilder out, MultiArray instruction, int index) {
        String desc = instruction.getDesc();
        int totalDimensions = arrayDescriptorDimensions(desc);
        String componentType = arrayDescriptorComponent(desc);
        int allocatedDimensions = instruction.getDimensionsToAllocate();
        out.append("        { let sizes = new Array(").append(totalDimensions).append(");");
        out.append(" for (let i = ").append(allocatedDimensions - 1).append("; i >= 0; i--) { sizes[i] = stack.q() | 0; }");
        out.append(" for (let i = ").append(allocatedDimensions).append("; i < ").append(totalDimensions)
                .append("; i++) { sizes[i] = -1; }");
        out.append(" stack.p(jvm.newMultiArray(sizes, \"").append(componentType).append("\", ")
                .append(totalDimensions).append(")); pc = ").append(index + 1).append("; break; }\n");
    }

    private static int arrayDescriptorDimensions(String desc) {
        int dimensions = 0;
        while (dimensions < desc.length() && desc.charAt(dimensions) == '[') {
            dimensions++;
        }
        return dimensions;
    }

    private static String arrayDescriptorComponent(String desc) {
        int dimensions = arrayDescriptorDimensions(desc);
        char kind = desc.charAt(dimensions);
        if (kind == 'L') {
            return JavascriptNameUtil.sanitizeClassName(desc.substring(dimensions + 1, desc.length() - 1));
        }
        switch (kind) {
            case 'Z':
                return "JAVA_BOOLEAN";
            case 'C':
                return "JAVA_CHAR";
            case 'F':
                return "JAVA_FLOAT";
            case 'D':
                return "JAVA_DOUBLE";
            case 'B':
                return "JAVA_BYTE";
            case 'S':
                return "JAVA_SHORT";
            case 'I':
                return "JAVA_INT";
            case 'J':
                return "JAVA_LONG";
            default:
                throw new IllegalArgumentException("Unsupported MULTIANEWARRAY descriptor " + desc);
        }
    }

    private static void appendSwitchInstruction(StringBuilder out, SwitchInstruction instruction, Map<Label, Integer> labelToIndex, int index) {
        out.append("        let __switchValue = stack.q() | 0;\n");
        out.append("        switch (__switchValue) {\n");
        int[] keys = instruction.getKeys();
        Label[] labels = instruction.getLabels();
        for (int i = 0; i < keys.length; i++) {
            out.append("          case ").append(keys[i]).append(": pc = ")
                    .append(resolveLabelIndex(labelToIndex, labels[i], "switch case")).append("; break;\n");
        }
        Label defaultLabel = instruction.getDefaultLabel();
        if (defaultLabel != null) {
            out.append("          default: pc = ").append(resolveLabelIndex(labelToIndex, defaultLabel, "switch default")).append("; break;\n");
        } else {
            out.append("          default: pc = ").append(index + 1).append("; break;\n");
        }
        out.append("        }\n");
        out.append("        break;\n");
    }

    private static int resolveLabelIndex(Map<Label, Integer> labelToIndex, Label label, String context) {
        Integer target = labelToIndex.get(label);
        if (target == null) {
            throw new IllegalStateException("Missing label target for " + context + " in JS backend");
        }
        return target.intValue();
    }

    private static void appendBasicInstruction(StringBuilder out, BytecodeMethod method, BasicInstruction instruction, int index) {
        switch (instruction.getOpcode()) {
            case Opcodes.NOP:
                out.append("        pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.ACONST_NULL:
                out.append("        stack.p(null); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.ICONST_M1:
                out.append("        stack.p(-1); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
                out.append("        stack.p(").append(instruction.getOpcode() - Opcodes.ICONST_0).append("); pc = ")
                        .append(index + 1).append("; break;\n");
                return;
            case Opcodes.LCONST_0:
                out.append("        stack.p(0); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.LCONST_1:
                out.append("        stack.p(1); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.FCONST_0:
            case Opcodes.DCONST_0:
                out.append("        stack.p(0.0); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.FCONST_1:
            case Opcodes.DCONST_1:
                out.append("        stack.p(1.0); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.FCONST_2:
                out.append("        stack.p(2.0); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                out.append("        stack.p(").append(instruction.getValue()).append("); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.ILOAD:
            case Opcodes.LLOAD:
            case Opcodes.FLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ALOAD:
                out.append("        stack.p(locals[").append(instruction.getValue()).append("]); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.ISTORE:
            case Opcodes.LSTORE:
            case Opcodes.FSTORE:
            case Opcodes.DSTORE:
            case Opcodes.ASTORE:
                out.append("        locals[").append(instruction.getValue()).append("] = stack.q(); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.POP:
                out.append("        stack.q(); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.POP2:
                out.append("        stack.q(); stack.q(); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.DUP:
                out.append("        stack.p(stack[stack.length - 1]); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.DUP_X1:
                out.append("        { let v1 = stack.q(); let v2 = stack.q(); stack.p(v1); stack.p(v2); stack.p(v1); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.DUP_X2:
                out.append("        { let v1 = stack.q(); let v2 = stack.q(); let v3 = stack.q(); stack.p(v1); stack.p(v3); stack.p(v2); stack.p(v1); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.DUP2:
                out.append("        { let v1 = stack.q(); let v2 = stack.q(); stack.p(v2); stack.p(v1); stack.p(v2); stack.p(v1); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.DUP2_X1:
                out.append("        { let v1 = stack.q(); let v2 = stack.q(); let v3 = stack.q(); stack.p(v2); stack.p(v1); stack.p(v3); stack.p(v2); stack.p(v1); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.DUP2_X2:
                out.append("        { let v1 = stack.q(); let v2 = stack.q(); let v3 = stack.q(); let v4 = stack.q(); stack.p(v2); stack.p(v1); stack.p(v4); stack.p(v3); stack.p(v2); stack.p(v1); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.SWAP:
                out.append("        { let v1 = stack.q(); let v2 = stack.q(); stack.p(v1); stack.p(v2); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IADD:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p((a|0) + (b|0)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.ISUB:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p((a|0) - (b|0)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IMUL:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p((a|0) * (b|0)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LADD:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(a + b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LSUB:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(a - b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LMUL:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(a * b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.FADD:
            case Opcodes.DADD:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(a + b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.FSUB:
            case Opcodes.DSUB:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(a - b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.FMUL:
            case Opcodes.DMUL:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(a * b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IDIV:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(((a|0) / (b|0)) | 0); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LDIV:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(Math.trunc(a / b)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.FDIV:
            case Opcodes.DDIV:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(a / b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IREM:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p((a|0) % (b|0)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LREM:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(a % b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.FREM:
            case Opcodes.DREM:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(a % b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.INEG:
                out.append("        stack.p(-(stack.q()|0)); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.LNEG:
                out.append("        stack.p(-stack.q()); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.FNEG:
            case Opcodes.DNEG:
                out.append("        stack.p(-stack.q()); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.ISHL:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p((a|0) << (b & 31)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LSHL:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(a * Math.pow(2, b & 63)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.ISHR:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p((a|0) >> (b & 31)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LSHR:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(Math.trunc(a / Math.pow(2, b & 63))); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IUSHR:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p((a >>> (b & 31)) | 0); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LUSHR:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(Math.floor((a < 0 ? a + 18446744073709551616 : a) / Math.pow(2, b & 63))); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IAND:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p((a|0) & (b|0)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LAND:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(a & b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IOR:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p((a|0) | (b|0)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LOR:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(a | b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IXOR:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p((a|0) ^ (b|0)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LXOR:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(a ^ b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.I2L:
            case Opcodes.F2D:
            case Opcodes.D2F:
                out.append("        pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.I2B:
                out.append("        stack.p((stack.q() << 24) >> 24); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.I2C:
                out.append("        stack.p(stack.q() & 65535); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.I2S:
                out.append("        stack.p((stack.q() << 16) >> 16); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.L2I:
            case Opcodes.F2I:
            case Opcodes.D2I:
                out.append("        stack.p(stack.q() | 0); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.I2F:
            case Opcodes.I2D:
            case Opcodes.L2F:
            case Opcodes.L2D:
            case Opcodes.F2L:
            case Opcodes.D2L:
                out.append("        pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.LCMP:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p(a < b ? -1 : (a > b ? 1 : 0)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.FCMPL:
            case Opcodes.DCMPL:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p((isNaN(a) || isNaN(b)) ? -1 : (a < b ? -1 : (a > b ? 1 : 0))); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.FCMPG:
            case Opcodes.DCMPG:
                out.append("        { let b = stack.q(); let a = stack.q(); stack.p((isNaN(a) || isNaN(b)) ? 1 : (a < b ? -1 : (a > b ? 1 : 0))); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IRETURN:
            case Opcodes.ARETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
                out.append("        return stack.q();\n");
                return;
            case Opcodes.ATHROW:
                out.append("        throw stack.q();\n");
                return;
            case Opcodes.RETURN:
                // Void RETURN — emit ``return`` without ``null``. Java
                // callers of void methods ignore the return value.
                out.append("        return;\n");
                return;
            case Opcodes.ARRAYLENGTH:
                out.append("        { let arr = stack.q(); stack.p(arr.length); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.AALOAD:
            case Opcodes.IALOAD:
            case Opcodes.LALOAD:
            case Opcodes.FALOAD:
            case Opcodes.DALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD:
                out.append("        { let idx = stack.q(); let arr = stack.q(); stack.p(_A(arr, idx)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.AASTORE:
            case Opcodes.IASTORE:
            case Opcodes.LASTORE:
            case Opcodes.FASTORE:
            case Opcodes.DASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
                out.append("        { let value = stack.q(); let idx = stack.q(); let arr = stack.q(); _T(arr, idx, value); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.MONITORENTER:
                out.append("        yield* _me(stack.q()); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.MONITOREXIT:
                out.append("        _mx(stack.q()); pc = ").append(index + 1).append("; break;\n");
                return;
            default:
                throw new IllegalArgumentException("Unsupported basic opcode " + instruction.getOpcode()
                        + " in " + method.getMethodIdentifier());
        }
    }

    private static void appendVarInstruction(StringBuilder out, VarOp instruction, int index) {
        switch (instruction.getOpcode()) {
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                out.append("        stack.p(").append(instruction.getIndex()).append("); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.NEWARRAY:
                out.append("        stack.p(_j(stack.q(), \"")
                        .append(primitiveArrayType(instruction.getIndex())).append("\", 1)); pc = ")
                        .append(index + 1).append("; break;\n");
                return;
            case Opcodes.ILOAD:
            case Opcodes.LLOAD:
            case Opcodes.FLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ALOAD:
                out.append("        stack.p(locals[").append(instruction.getIndex()).append("]); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.ISTORE:
            case Opcodes.LSTORE:
            case Opcodes.FSTORE:
            case Opcodes.DSTORE:
            case Opcodes.ASTORE:
                out.append("        locals[").append(instruction.getIndex()).append("] = stack.q(); pc = ").append(index + 1).append("; break;\n");
                return;
            default:
                throw new IllegalArgumentException("Unsupported var opcode " + instruction.getOpcode());
        }
    }

    private static String primitiveArrayType(int operand) {
        switch (operand) {
            case Opcodes.T_BOOLEAN:
                return "JAVA_BOOLEAN";
            case Opcodes.T_CHAR:
                return "JAVA_CHAR";
            case Opcodes.T_FLOAT:
                return "JAVA_FLOAT";
            case Opcodes.T_DOUBLE:
                return "JAVA_DOUBLE";
            case Opcodes.T_BYTE:
                return "JAVA_BYTE";
            case Opcodes.T_SHORT:
                return "JAVA_SHORT";
            case Opcodes.T_INT:
                return "JAVA_INT";
            case Opcodes.T_LONG:
                return "JAVA_LONG";
            default:
                throw new IllegalArgumentException("Unsupported NEWARRAY operand " + operand);
        }
    }

    private static void appendLdcInstruction(StringBuilder out, Ldc instruction, int index) {
        Object value = instruction.getValue();
        if (value instanceof String) {
            out.append("        stack.p(_L(\"")
                    .append(JavascriptNameUtil.escapeJs((String) value)).append("\")); pc = ").append(index + 1).append("; break;\n");
            return;
        }
        if (value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double) {
            out.append("        stack.p(").append(value.toString()).append("); pc = ").append(index + 1).append("; break;\n");
            return;
        }
        if (value instanceof Type) {
            Type type = (Type) value;
            if (type.getSort() == Type.OBJECT) {
                out.append("        stack.p(jvm.getClassObject(\"").append(JavascriptNameUtil.sanitizeClassName(type.getInternalName()))
                        .append("\")); pc = ").append(index + 1).append("; break;\n");
                return;
            }
        }
        throw new IllegalArgumentException("Unsupported ldc constant in javascript backend: " + value);
    }

    private static void appendTypeInstruction(StringBuilder out, TypeInstruction instruction, int index) {
        String typeName = JavascriptNameUtil.runtimeTypeName(instruction.getTypeName());
        switch (instruction.getOpcode()) {
            case Opcodes.NEW:
                out.append("        stack.p(_O(\"").append(typeName).append("\")); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.ANEWARRAY:
                out.append("        stack.p(_j(stack.q(), \"").append(typeName)
                        .append("\", 1)); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.CHECKCAST:
                // Peek TOS and run the cast check inline — no temp let
                // binding or extra braces. ``_C`` evaluates its first
                // argument exactly once (standard JS semantics), so
                // reading ``stack[stack.length - 1]`` here produces the
                // same value the next instruction will pop.
                out.append("        _C(stack[stack.length - 1], \"").append(typeName).append("\"); pc = ")
                        .append(index + 1).append("; break;\n");
                return;
            case Opcodes.INSTANCEOF:
                // Inline: pop directly into the ``_D`` (instanceOf)
                // call, push the boolean-as-int result. Same eval
                // order as the old let-binding form, ~15 chars
                // shorter per call site.
                out.append("        stack.p(").append(directInstanceOfExpression("stack.q()", typeName))
                        .append(" ? 1 : 0); pc = ").append(index + 1).append("; break;\n");
                return;
            default:
                throw new IllegalArgumentException("Unsupported type opcode " + instruction.getOpcode());
        }
    }

    private static void appendDirectCheckCast(StringBuilder out, String indent, String valueExpression, String typeName, String failureStatement) {
        // CHECKCAST used to expand to ~280 chars of inline type-check
        // boilerplate per call site (null guard, assignableTo lookup,
        // enhanceJsWrapper fallback, second assignableTo lookup, throw).
        // ~2200 call sites in Initializr, ~300 KiB total. Factored into
        // a single ``_C(value, typeName)`` helper in the runtime
        // that encodes the same sequence and throws ClassCastException
        // on failure; emitted call site collapses to ~30 chars.
        // ``failureStatement`` is always the ClassCastException throw
        // — the only caller that ever passes anything else is dead
        // code; we encode it in the helper directly.
        out.append(indent).append("_C(").append(valueExpression).append(", \"").append(typeName).append("\");\n");
    }

    private static String directInstanceOfExpression(String valueExpression, String typeName) {
        // ``jvm.iO`` (instanceOf) encodes the ~110-char inline chain
        // as a helper call, halving call-site size across ~360
        // INSTANCEOF sites. Returns 1/0 directly so emission doesn't
        // need to wrap the result in a ``? 1 : 0`` ternary.
        return "_D(" + valueExpression + ", \"" + typeName + "\")";
    }

    private static void appendFieldInstruction(StringBuilder out, Field field, int index, boolean usesStaticFieldInitCache) {
        String rawOwner = field.getOwner();
        String fieldName = field.getFieldName();
        // Resolve the bytecode's class reference to the actual declaring
        // class so the emitted ``target["cn1_<declaring>_<field>"]`` access
        // stays aligned with the prop the translator emitted on the
        // declaring class's classDef.instanceFields entry. Without this,
        // reads against a subclass receiver access a never-set property
        // and come back undefined under mangling (initFieldAliases sets
        // up the alias under the verbose ``cn1_<subclass>_...`` key,
        // which mangling rewrites inconsistently).
        String instanceOwner = resolveFieldOwner(rawOwner, fieldName);
        String owner = JavascriptNameUtil.sanitizeClassName(rawOwner);
        String propertyName = JavascriptNameUtil.fieldProperty(instanceOwner, fieldName);
        switch (field.getOpcode()) {
            case Opcodes.GETSTATIC:
                appendInterpreterEnsureClassInitialized(out, owner, usesStaticFieldInitCache);
                // ``_S["owner"]["fieldName"]`` (runtime-maintained
                // per-class staticFields map) is ~18 chars shorter per
                // call site than the equivalent
                // ``jvm.classes["owner"].staticFields["fieldName"]``
                // expansion; the map is populated at defineClass time.
                out.append("        stack.p(_S[\"").append(owner).append("\"][\"")
                        .append(fieldName).append("\"]); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.PUTSTATIC:
                appendInterpreterEnsureClassInitialized(out, owner, usesStaticFieldInitCache);
                out.append("        _S[\"").append(owner).append("\"][\"").append(fieldName)
                        .append("\"] = stack.q(); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.GETFIELD:
                // Inline: evaluate pop() first, then field access. Same
                // NPE semantics as the previous ``{const t=pop();
                // push(t[prop])}`` form but shorter after minification
                // (~10 chars saved per site × ~5k GETFIELDs).
                out.append("        stack.p(stack.q()[\"").append(propertyName)
                        .append("\"]); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.PUTFIELD:
                // Capture value first, then use a block-scoped temp for
                // target so the emitted form stays readable under
                // minification while still popping in stack order.
                out.append("        { let v = stack.q(); stack.q()[\"").append(propertyName)
                        .append("\"] = v; pc = ").append(index + 1).append("; break; }\n");
                return;
            default:
                throw new IllegalArgumentException("Unsupported field opcode " + field.getOpcode());
        }
    }

    private static void appendInterpreterEnsureClassInitialized(StringBuilder out, String owner, boolean usesStaticFieldInitCache) {
        // Skip ``_I("owner")`` when ``owner`` is the class we're
        // currently emitting a method for — or any of its ancestors.
        // The JVM spec guarantees a class's supertypes are initialized
        // before the class itself, which in turn is initialized before
        // any of its methods can run, so both are already live by the
        // time this method body executes. ~30% of the 7.7k ``jvm.eI``
        // sites resolve to the containing class or its ancestors.
        if (isClassAlreadyInitializedForCurrentEmission(owner)) {
            return;
        }
        out.append("        _I(\"").append(owner).append("\");\n");
    }

    private static boolean isClassAlreadyInitializedForCurrentEmission(String owner) {
        ByteCodeClass cls = currentEmissionClass;
        if (cls == null || owner == null) {
            return false;
        }
        String target = JavascriptNameUtil.sanitizeClassName(owner);
        ByteCodeClass walk = cls;
        int hops = 0;
        while (walk != null && hops++ < 64) {
            if (target.equals(walk.getClsName())) {
                return true;
            }
            walk = walk.getBaseClassObject();
        }
        return false;
    }

    private static boolean hasClassInitSensitiveAccess(List<Instruction> instructions) {
        for (Instruction instruction : instructions) {
            if (instruction instanceof Field) {
                int opcode = instruction.getOpcode();
                if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {
                    return true;
                }
            }
            if (instruction instanceof Invoke && instruction.getOpcode() == Opcodes.INVOKESTATIC) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasVirtualDispatchAccess(List<Instruction> instructions) {
        for (Instruction instruction : instructions) {
            if (instruction instanceof Invoke) {
                int opcode = instruction.getOpcode();
                if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void appendJumpInstruction(StringBuilder out, Jump jump, Map<Label, Integer> labelToIndex, int index) {
        Integer target = labelToIndex.get(jump.getLabel());
        if (target == null) {
            throw new IllegalStateException("Missing label target for jump in JS backend");
        }
        switch (jump.getOpcode()) {
            case Opcodes.GOTO:
                out.append("        pc = ").append(target.intValue()).append("; break;\n");
                return;
            case Opcodes.IFEQ:
                out.append("        pc = ((stack.q()|0) == 0) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.IFNE:
                out.append("        pc = ((stack.q()|0) != 0) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.IFLT:
                out.append("        pc = ((stack.q()|0) < 0) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.IFLE:
                out.append("        pc = ((stack.q()|0) <= 0) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.IFGT:
                out.append("        pc = ((stack.q()|0) > 0) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.IFGE:
                out.append("        pc = ((stack.q()|0) >= 0) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.IFNULL:
                out.append("        pc = (stack.q() == null) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.IFNONNULL:
                out.append("        pc = (stack.q() != null) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.IF_ICMPEQ:
                out.append("        { let b = stack.q(); let a = stack.q(); pc = ((a|0) == (b|0)) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IF_ICMPNE:
                out.append("        { let b = stack.q(); let a = stack.q(); pc = ((a|0) != (b|0)) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IF_ICMPLT:
                out.append("        { let b = stack.q(); let a = stack.q(); pc = ((a|0) < (b|0)) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IF_ICMPLE:
                out.append("        { let b = stack.q(); let a = stack.q(); pc = ((a|0) <= (b|0)) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IF_ICMPGT:
                out.append("        { let b = stack.q(); let a = stack.q(); pc = ((a|0) > (b|0)) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IF_ICMPGE:
                out.append("        { let b = stack.q(); let a = stack.q(); pc = ((a|0) >= (b|0)) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IF_ACMPEQ:
                out.append("        { let b = stack.q(); let a = stack.q(); pc = (a === b) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IF_ACMPNE:
                out.append("        { let b = stack.q(); let a = stack.q(); pc = (a !== b) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break; }\n");
                return;
            default:
                throw new IllegalArgumentException("Unsupported jump opcode " + jump.getOpcode());
        }
    }

    private static void appendInvokeInstruction(StringBuilder out, Invoke invoke, int index, boolean usesClassInitCache,
            boolean usesVirtualDispatchCache) {
        String declaredOwner = invoke.getOwner();
        String directOwner = resolveDirectInvokeOwner(invoke);
        String owner = JavascriptNameUtil.sanitizeClassName(directOwner);
        String methodOwner = (invoke.getOpcode() == Opcodes.INVOKEVIRTUAL || invoke.getOpcode() == Opcodes.INVOKEINTERFACE)
                ? declaredOwner
                : directOwner;
        String methodId = JavascriptNameUtil.methodIdentifier(methodOwner, invoke.getName(), invoke.getDesc());
        // Virtual / interface dispatch uses a class-free ``dispatch`` id
        // so every class that implements a given Java method stores it
        // under the same key. The runtime's hierarchy walk in
        // ``resolveVirtual`` handles inheritance without the translator
        // emitting explicit alias entries — which used to account for
        // ~25% of Initializr's translated JS.
        String dispatchId = JavascriptNameUtil.dispatchMethodIdentifier(invoke.getName(), invoke.getDesc());
        String methodBodyId = jsStaticMethodBodyIdentifier(methodOwner, invoke.getName(), invoke.getDesc());
        List<String> args = JavascriptNameUtil.argumentTypes(invoke.getDesc());
        boolean hasReturn = invoke.getDesc().charAt(invoke.getDesc().length() - 1) != 'V';
        int argCount = args.size();
        switch (invoke.getOpcode()) {
            case Opcodes.INVOKESTATIC:
            case Opcodes.INVOKESPECIAL:
                break;
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKEINTERFACE:
                break;
            default:
                throw new IllegalArgumentException("Unsupported invoke opcode " + invoke.getOpcode());
        }

        if (invoke.getOpcode() == Opcodes.INVOKEVIRTUAL || invoke.getOpcode() == Opcodes.INVOKEINTERFACE) {
            // Fast path for 0-arg virtual dispatch: inline the
            // target pop into the iv0 call. Pops TOS inside the
            // invoke's arg list, so the full block collapses to a
            // single statement — saves ~25 chars × thousands of
            // call sites.
            if (argCount == 0) {
                if (hasReturn) {
                    out.append("        stack.p(yield* cn1_iv0(stack.q(), \"").append(dispatchId).append("\"));\n");
                } else {
                    out.append("        yield* cn1_iv0(stack.q(), \"").append(dispatchId).append("\");\n");
                }
                out.append("        pc = ").append(index + 1).append("; break;\n");
                return;
            }
            if (argCount == 1) {
                out.append("        { let __arg0 = stack.q(); ");
                if (hasReturn) {
                    out.append("stack.p(yield* cn1_iv1(stack.q(), \"").append(dispatchId).append("\", __arg0));");
                } else {
                    out.append("yield* cn1_iv1(stack.q(), \"").append(dispatchId).append("\", __arg0);");
                }
                out.append(" pc = ").append(index + 1).append("; break; }\n");
                return;
            }
            if (argCount == 2) {
                out.append("        { let __arg1 = stack.q(); let __arg0 = stack.q(); ");
                if (hasReturn) {
                    out.append("stack.p(yield* cn1_iv2(stack.q(), \"").append(dispatchId).append("\", __arg0, __arg1));");
                } else {
                    out.append("yield* cn1_iv2(stack.q(), \"").append(dispatchId).append("\", __arg0, __arg1);");
                }
                out.append(" pc = ").append(index + 1).append("; break; }\n");
                return;
            }
            if (argCount == 3) {
                out.append("        { let __arg2 = stack.q(); let __arg1 = stack.q(); let __arg0 = stack.q(); ");
                if (hasReturn) {
                    out.append("stack.p(yield* cn1_iv3(stack.q(), \"").append(dispatchId).append("\", __arg0, __arg1, __arg2));");
                } else {
                    out.append("yield* cn1_iv3(stack.q(), \"").append(dispatchId).append("\", __arg0, __arg1, __arg2);");
                }
                out.append(" pc = ").append(index + 1).append("; break; }\n");
                return;
            }
            if (argCount == 4) {
                out.append("        { let __arg3 = stack.q(); let __arg2 = stack.q(); let __arg1 = stack.q(); let __arg0 = stack.q(); ");
                if (hasReturn) {
                    out.append("stack.p(yield* cn1_iv4(stack.q(), \"").append(dispatchId).append("\", __arg0, __arg1, __arg2, __arg3));");
                } else {
                    out.append("yield* cn1_iv4(stack.q(), \"").append(dispatchId).append("\", __arg0, __arg1, __arg2, __arg3);");
                }
                out.append(" pc = ").append(index + 1).append("; break; }\n");
                return;
            }
            // Virtual-dispatch call site for arity ≥ 2. We used to
            // emit ~15 lines of inline __classDef lookup +
            // resolveVirtual fallback + per-method cache around
            // every INVOKEVIRTUAL / INVOKEINTERFACE; the
            // ``cn1_iv2..cn1_iv4 / cn1_ivN`` helpers collapse that
            // into one call with the same fast-path / fallback
            // semantics.
            out.append("        {\n");
            appendInvocationArgumentBindings(out, argCount, "          ", "stack.q()");
            out.append("          let __target = stack.q();\n");
            appendCompactVirtualDispatch(out, "          ", dispatchId, argCount, hasReturn, "__target", true);
            out.append("          pc = ").append(index + 1).append("; break;\n");
            out.append("        }\n");
            return;
        }

        // For INVOKESTATIC, pick between the public wrapper and the
        // ``__impl`` body at emit time based on whether the target is
        // native. Non-native statics have a real ``__impl`` function
        // (the body we want to call directly, skipping the wrapper's
        // redundant ``jvm.eI`` — the interpreter already emitted one
        // above). Native statics only have the public wrapper (their
        // ``__impl`` name isn't declared), so calling that is the
        // only safe option. Previously emitted
        // ``typeof X==="function"?X:Y`` (~30 chars) at every site; now
        // either ``methodBodyId`` or ``methodId`` directly.
        String invokedName = invoke.getOpcode() == Opcodes.INVOKESTATIC
                ? (isInvokeTargetNative(invoke) ? methodId : methodBodyId)
                : methodId;
        String interpYieldPrefix = isInvokeSuspending(invoke) ? "yield* " : "";
        // Fast path for 0-arg + static invoke: eI(), call, push. No
        // arg bindings needed, no ``let __target`` (INVOKESTATIC
        // doesn't consume a receiver from the stack).
        if (argCount == 0 && invoke.getOpcode() == Opcodes.INVOKESPECIAL) {
            // INVOKESPECIAL 0-arg (mostly constructor calls where the
            // class has already been new'd): pop target, call as
            // non-virtual.
            if (hasReturn) {
                out.append("        stack.p(").append(interpYieldPrefix).append(invokedName).append("(stack.q()));\n");
            } else {
                out.append("        ").append(interpYieldPrefix).append(invokedName).append("(stack.q());\n");
            }
            out.append("        pc = ").append(index + 1).append("; break;\n");
            return;
        }
        if (argCount == 0 && invoke.getOpcode() == Opcodes.INVOKESTATIC) {
            appendInterpreterEnsureClassInitialized(out, owner, usesClassInitCache);
            if (hasReturn) {
                out.append("        stack.p(").append(interpYieldPrefix).append(invokedName).append("());\n");
            } else {
                out.append("        ").append(interpYieldPrefix).append(invokedName).append("();\n");
            }
            out.append("        pc = ").append(index + 1).append("; break;\n");
            return;
        }
        // Fast path for 1-arg INVOKESPECIAL: pop arg (preserve eval
        // order via let), inline target pop.
        if (argCount == 1 && invoke.getOpcode() == Opcodes.INVOKESPECIAL) {
            out.append("        { let __arg0 = stack.q(); ");
            if (hasReturn) {
                out.append("stack.p(").append(interpYieldPrefix).append(invokedName).append("(stack.q(), __arg0));");
            } else {
                out.append(interpYieldPrefix).append(invokedName).append("(stack.q(), __arg0);");
            }
            out.append(" pc = ").append(index + 1).append("; break; }\n");
            return;
        }
        // Fast path for 1-arg INVOKESTATIC: eI(), pop arg, call.
        if (argCount == 1 && invoke.getOpcode() == Opcodes.INVOKESTATIC) {
            appendInterpreterEnsureClassInitialized(out, owner, usesClassInitCache);
            if (hasReturn) {
                out.append("        stack.p(").append(interpYieldPrefix).append(invokedName).append("(stack.q()));\n");
            } else {
                out.append("        ").append(interpYieldPrefix).append(invokedName).append("(stack.q());\n");
            }
            out.append("        pc = ").append(index + 1).append("; break;\n");
            return;
        }
        // Fast path for 2-arg INVOKESPECIAL.
        if (argCount == 2 && invoke.getOpcode() == Opcodes.INVOKESPECIAL) {
            out.append("        { let __arg1 = stack.q(); let __arg0 = stack.q(); ");
            if (hasReturn) {
                out.append("stack.p(").append(interpYieldPrefix).append(invokedName).append("(stack.q(), __arg0, __arg1));");
            } else {
                out.append(interpYieldPrefix).append(invokedName).append("(stack.q(), __arg0, __arg1);");
            }
            out.append(" pc = ").append(index + 1).append("; break; }\n");
            return;
        }
        // Fast path for 2-arg INVOKESTATIC.
        if (argCount == 2 && invoke.getOpcode() == Opcodes.INVOKESTATIC) {
            appendInterpreterEnsureClassInitialized(out, owner, usesClassInitCache);
            out.append("        { let __arg1 = stack.q(); ");
            if (hasReturn) {
                out.append("stack.p(").append(interpYieldPrefix).append(invokedName).append("(stack.q(), __arg1));");
            } else {
                out.append(interpYieldPrefix).append(invokedName).append("(stack.q(), __arg1);");
            }
            out.append(" pc = ").append(index + 1).append("; break; }\n");
            return;
        }
        out.append("        {\n");
        appendInvocationArgumentBindings(out, argCount, "          ", "stack.q()");
        if (invoke.getOpcode() != Opcodes.INVOKESTATIC) {
            out.append("          let __target = stack.q();\n");
        } else {
            appendInterpreterEnsureClassInitialized(out, owner, usesClassInitCache);
        }
        if (hasReturn) {
            out.append("          let __result = ").append(interpYieldPrefix).append(invokedName).append("(");
        } else {
            out.append("          ").append(interpYieldPrefix).append(invokedName).append("(");
        }
        appendInvocationArguments(out, invoke.getOpcode() != Opcodes.INVOKESTATIC, argCount);
        out.append(");\n");
        if (hasReturn) {
            out.append("          stack.p(__result);\n");
        }
        out.append("          pc = ").append(index + 1).append("; break;\n");
        out.append("        }\n");
    }

    private static void appendInvocationArguments(StringBuilder out, boolean includeTarget, int argCount) {
        boolean first = true;
        if (includeTarget) {
            out.append("__target");
            first = false;
        }
        for (int i = 0; i < argCount; i++) {
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append("__arg").append(i);
        }
    }

    private static void appendInvocationArgumentBindings(StringBuilder out, int argCount, String indent, String sourceExpression) {
        for (int i = argCount - 1; i >= 0; i--) {
            out.append(indent).append("let __arg").append(i).append(" = ").append(sourceExpression).append(";\n");
        }
    }

    /**
     * Emit a virtual-dispatch invocation using the cn1_iv* helpers in
     * parparvm_runtime.js. Replaces ~15 lines of inline boilerplate with one
     * helper call and saves ~500 bytes per call site (on Initializr: ~14 MiB
     * of translated_app.js was this one pattern). The helpers preserve the
     * original semantics: target.__classDef.methods fast-path, then
     * jvm.resolveVirtual fallback which owns a class-wide cache.
     *
     * @param out             Output builder.
     * @param indent          Leading whitespace for the emitted statement.
     * @param methodId        Resolved method identifier string.
     * @param argCount        Number of non-receiver arguments on the stack.
     * @param hasReturn       Whether the method returns a value (must be pushed).
     * @param targetExpr      Expression evaluating to the receiver.
     * @param argsFromStack   If true, call-site already bound stack values to
     *                        __arg0..__arg{N-1}. If false, argValues provides
     *                        the arg expressions directly (straight-line path).
     */
    private static void appendCompactVirtualDispatch(StringBuilder out, String indent, String methodId,
            int argCount, boolean hasReturn, String targetExpr, boolean argsFromStack) {
        appendCompactVirtualDispatch(out, indent, methodId, argCount, hasReturn, targetExpr, argsFromStack, null);
    }

    private static void appendCompactVirtualDispatch(StringBuilder out, String indent, String methodId,
            int argCount, boolean hasReturn, String targetExpr, boolean argsFromStack, String[] argExpressions) {
        String helper;
        boolean variadic = false;
        switch (argCount) {
            case 0: helper = "cn1_iv0"; break;
            case 1: helper = "cn1_iv1"; break;
            case 2: helper = "cn1_iv2"; break;
            case 3: helper = "cn1_iv3"; break;
            case 4: helper = "cn1_iv4"; break;
            default:
                helper = "cn1_ivN";
                variadic = true;
                break;
        }
        out.append(indent);
        if (hasReturn && argsFromStack) {
            out.append("stack.p(yield* ").append(helper).append("(").append(targetExpr).append(", \"").append(methodId).append("\"");
        } else if (hasReturn) {
            out.append("let __result = yield* ").append(helper).append("(").append(targetExpr).append(", \"").append(methodId).append("\"");
        } else {
            out.append("yield* ").append(helper).append("(").append(targetExpr).append(", \"").append(methodId).append("\"");
        }
        if (variadic) {
            out.append(", [");
            for (int i = 0; i < argCount; i++) {
                if (i > 0) out.append(", ");
                out.append(argsFromStack ? ("__arg" + i) : argExpressions[i]);
            }
            out.append("]");
        } else {
            for (int i = 0; i < argCount; i++) {
                out.append(", ").append(argsFromStack ? ("__arg" + i) : argExpressions[i]);
            }
        }
        out.append(")");
        if (hasReturn && argsFromStack) {
            out.append(");\n");
        } else {
            out.append(";\n");
        }
    }

    private static String staticInvocationTargetExpression(String methodId, String methodBodyId) {
        return "typeof " + methodBodyId + "==\"function\"?" + methodBodyId + ":" + methodId;
    }

    /**
     * True when the method targeted by an INVOKESTATIC is declared
     * native — i.e. only the public wrapper exists at runtime and
     * ``methodBodyId`` refers to an undefined identifier. Used to
     * skip the ``typeof X==='function'?X:Y`` runtime check at call
     * sites when the translator can tell statically whether the body
     * exists.
     */
    private static boolean isInvokeTargetNative(Invoke invoke) {
        String owner = invoke.getOwner();
        if (owner == null) {
            return false;
        }
        String sanitized = JavascriptNameUtil.sanitizeClassName(owner);
        ByteCodeClass cls = Parser.getClassObject(sanitized);
        BytecodeMethod resolved = resolveStaticMethodOnHierarchy(cls, invoke.getName(), invoke.getDesc());
        return resolved != null && resolved.isNative();
    }

    private static BytecodeMethod resolveStaticMethodOnHierarchy(ByteCodeClass cls, String name, String desc) {
        if (cls == null) {
            return null;
        }
        List<BytecodeMethod> methods = cls.getMethods();
        if (methods != null) {
            for (BytecodeMethod method : methods) {
                if (method.isStatic()
                        && method.getMethodName().equals(name)
                        && desc.equals(method.getSignature())) {
                    return method;
                }
            }
        }
        return resolveStaticMethodOnHierarchy(cls.getBaseClassObject(), name, desc);
    }

    private static String resolveDirectInvokeOwner(Invoke invoke) {
        String owner = invoke.getOwner();
        if (owner == null) {
            return null;
        }
        if (invoke.getOpcode() == Opcodes.INVOKESTATIC) {
            ByteCodeClass ownerClass = Parser.getClassObject(owner.replace('/', '_').replace('$', '_'));
            String resolvedOwner = findActualStaticOwner(ownerClass, invoke.getName(), invoke.getDesc());
            return resolvedOwner != null ? resolvedOwner : owner;
        }
        if (invoke.getOpcode() == Opcodes.INVOKESPECIAL
                && !"<init>".equals(invoke.getName())
                && !"<clinit>".equals(invoke.getName())) {
            String resolvedOwner = Util.resolveInvokeSpecialOwner(owner, invoke.getName(), invoke.getDesc());
            return resolvedOwner != null ? resolvedOwner : owner;
        }
        return owner;
    }

    private static String findActualStaticOwner(ByteCodeClass ownerClass, String name, String desc) {
        if (ownerClass == null) {
            return null;
        }
        List<BytecodeMethod> methods = ownerClass.getMethods();
        if (methods != null) {
            for (BytecodeMethod method : methods) {
                if (method.isStatic()
                        && method.getMethodName().equals(name)
                        && desc.equals(method.getSignature())) {
                    return ownerClass.getOriginalClassName();
                }
            }
        }
        return findActualStaticOwner(ownerClass.getBaseClassObject(), name, desc);
    }
}
