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
    private JavascriptMethodGenerator() {
    }

    static String generateClassJavascript(ByteCodeClass cls, List<ByteCodeClass> allClasses) {
        StringBuilder out = new StringBuilder();
        out.append("// ").append(cls.getClsName()).append("\n");
        appendClassRegistration(out, cls, allClasses);
        for (BytecodeMethod method : cls.getMethods()) {
            if (method.isNative() || method.isAbstract() || method.isEliminated()) {
                continue;
            }
            appendMethod(out, cls, method);
        }
        appendInheritedMethodAliases(out, cls);
        for (BytecodeMethod method : cls.getMethods()) {
            if (!method.isNative() || method.isEliminated()) {
                continue;
            }
            appendNativeStubIfNeeded(out, cls, method);
            if (!method.isStatic() && !method.isConstructor()) {
                String jsMethodName = jsMethodIdentifier(cls, method);
                out.append("jvm.addVirtualMethod(\"").append(cls.getClsName()).append("\", \"")
                        .append(jsMethodName).append("\", ")
                        .append(jsMethodName).append(");\n");
            }
        }
        appendSyntheticClinitIfNeeded(out, cls);
        return out.toString();
    }

    private static void appendClassRegistration(StringBuilder out, ByteCodeClass cls, List<ByteCodeClass> allClasses) {
        out.append("jvm.defineClass({\n");
        out.append("  name: \"").append(cls.getClsName()).append("\",\n");
        out.append("  baseClass: ");
        if (cls.getBaseClass() == null) {
            out.append("null");
        } else {
            out.append("\"").append(JavascriptNameUtil.sanitizeClassName(cls.getBaseClass())).append("\"");
        }
        out.append(",\n");
        out.append("  interfaces: [");
        boolean first = true;
        for (String iface : cls.getBaseInterfaces()) {
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append("\"").append(JavascriptNameUtil.sanitizeClassName(iface)).append("\"");
        }
        out.append("],\n");
        out.append("  isInterface: ").append(cls.isIsInterface()).append(",\n");
        out.append("  isAbstract: ").append(cls.isIsAbstract()).append(",\n");
        appendAssignableTypes(out, cls, allClasses);
        out.append("  instanceFields: [");
        first = true;
        for (ByteCodeField field : cls.getFields()) {
            if (field.isStaticField()) {
                continue;
            }
            if (!first) {
                out.append(", ");
            }
            first = false;
            String desc = field.getRuntimeDescriptor();
            out.append("{ owner: \"").append(field.getClsName()).append("\", name: \"")
                    .append(field.getFieldName()).append("\", desc: \"")
                    .append(JavascriptNameUtil.escapeJs(desc == null ? "" : desc)).append("\", prop: \"")
                    .append(JavascriptNameUtil.fieldProperty(field.getClsName(), field.getFieldName())).append("\" }");
        }
        out.append("],\n");
        out.append("  staticFields: {");
        first = true;
        for (ByteCodeField field : cls.getFields()) {
            if (!field.isStaticField()) {
                continue;
            }
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append("\"").append(field.getFieldName()).append("\": ")
                    .append(renderStaticFieldInitialValue(field));
        }
        out.append("},\n");
        out.append("  methods: {},\n");
        out.append("  classObject: null\n");
        out.append("});\n");
    }

    private static void appendAssignableTypes(StringBuilder out, ByteCodeClass cls, List<ByteCodeClass> allClasses) {
        List<String> assignableTypes = new java.util.ArrayList<String>();
        collectAssignableTypes(cls, allClasses, assignableTypes);
        out.append("  assignableTo: {");
        for (int i = 0; i < assignableTypes.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append("\"").append(assignableTypes.get(i)).append("\": true");
        }
        out.append("},\n");
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
            return "jvm.createStringLiteral(\"" + JavascriptNameUtil.escapeJs((String) value) + "\")";
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
            out.append("  jvm.classes[\"").append(cls.getClsName()).append("\"].staticFields[\"")
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
        out.append("jvm.classes[\"").append(cls.getClsName()).append("\"].clinit = ").append(fn).append(";\n");
    }

    private static void appendMethod(StringBuilder out, ByteCodeClass cls, BytecodeMethod method) {
        List<Instruction> instructions = method.getInstructions();
        Map<Label, Integer> labelToIndex = buildLabelMap(instructions);
        String jsMethodName = jsMethodIdentifier(cls, method);
        String jsMethodBodyName = jsMethodBodyIdentifier(cls, method);
        boolean wrappedStaticMethod = isWrappedStaticMethod(method);
        out.append("function* ").append(wrappedStaticMethod ? jsMethodBodyName : jsMethodName).append("(");
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
            out.append("  jvm.ensureClassInitialized(\"").append(cls.getClsName()).append("\");\n");
        }
        if ("__CLINIT__".equals(method.getMethodName())) {
            appendDeferredStaticFieldInitialization(out, cls);
        }
        if (appendStraightLineMethodBody(out, cls, method, instructions, wrappedStaticMethod ? jsMethodBodyName : jsMethodName)) {
            if (wrappedStaticMethod) {
                appendWrappedStaticMethod(out, cls, method, jsMethodName, jsMethodBodyName);
            }
            return;
        }
        boolean usesClassInitCache = hasClassInitSensitiveAccess(instructions);
        boolean usesVirtualDispatchCache = hasVirtualDispatchAccess(instructions);
        out.append("  const locals = new Array(").append(Math.max(1, method.getMaxLocals())).append(").fill(null);\n");
        out.append("  const stack = [];\n");
        out.append("  let pc = 0;\n");
        if (usesClassInitCache) {
            out.append("  const __cn1Init = Object.create(null);\n");
            if (method.isStatic() && !"__CLINIT__".equals(method.getMethodName())) {
                out.append("  __cn1Init[\"").append(cls.getClsName()).append("\"] = true;\n");
            }
        }
        if (usesVirtualDispatchCache) {
            out.append("  const __cn1Virtual = Object.create(null);\n");
        }
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
        appendTryCatchTable(out, instructions, labelToIndex);
        if (method.isSynchronizedMethod()) {
            out.append("  const __cn1Monitor = ").append(method.isStatic() ? "jvm.getClassObject(\"" + cls.getClsName() + "\")" : "__cn1ThisObject").append(";\n");
            out.append("  jvm.monitorEnter(jvm.currentThread, __cn1Monitor);\n");
            out.append("  try {\n");
        }
        out.append("  while (true) {\n");
        out.append("    try {\n");
        out.append("    switch (pc) {\n");
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            out.append("      case ").append(i).append(": {\n");
            appendInstruction(out, method, instructions, labelToIndex, instruction, i, usesClassInitCache, usesVirtualDispatchCache);
            out.append("      }\n");
        }
        out.append("      default:\n");
        out.append("        return null;\n");
        out.append("    }\n");
        out.append("    } catch (__cn1Error) {\n");
        out.append("      const __handler = jvm.findExceptionHandler(__cn1TryCatch, pc, __cn1Error);\n");
        out.append("      if (!__handler) {\n");
        out.append("        throw __cn1Error;\n");
        out.append("      }\n");
        out.append("      stack.length = 0;\n");
        out.append("      stack.push(__cn1Error);\n");
        out.append("      pc = __handler.handler;\n");
        out.append("    }\n");
        out.append("  }\n");
        if (method.isSynchronizedMethod()) {
            out.append("  } finally {\n");
            out.append("    jvm.monitorExit(jvm.currentThread, __cn1Monitor);\n");
            out.append("  }\n");
        }
        out.append("}\n");
        if (wrappedStaticMethod) {
            appendWrappedStaticMethod(out, cls, method, jsMethodName, jsMethodBodyName);
        }
        if ("__CLINIT__".equals(method.getMethodName())) {
            out.append("jvm.classes[\"").append(cls.getClsName()).append("\"].clinit = ")
                    .append(wrappedStaticMethod ? jsMethodBodyName : jsMethodName).append(";\n");
        }
        if (!method.isStatic() && !method.isConstructor()) {
            out.append("jvm.addVirtualMethod(\"").append(cls.getClsName()).append("\", \"")
                    .append(jsMethodName).append("\", ").append(jsMethodName).append(");\n");
        }
    }

    private static boolean isWrappedStaticMethod(BytecodeMethod method) {
        return method.isStatic() && !"__CLINIT__".equals(method.getMethodName());
    }

    private static void appendWrappedStaticMethod(StringBuilder out, ByteCodeClass cls, BytecodeMethod method, String wrapperName, String bodyName) {
        out.append("function* ").append(wrapperName).append("(");
        appendMethodParameters(out, method);
        out.append("){\n");
        out.append("  jvm.ensureClassInitialized(\"").append(cls.getClsName()).append("\");\n");
        out.append("  return yield* ").append(bodyName).append("(");
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

    private static void appendInheritedMethodAliases(StringBuilder out, ByteCodeClass cls) {
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
            out.append("function* ").append(aliasName).append("(");
            appendMethodParameters(out, method);
            out.append("){\n");
            out.append("  return yield* ").append(targetName).append("(");
            appendMethodParameterArguments(out, method);
            out.append(");\n");
            out.append("}\n");
            if (!method.isStatic()) {
                out.append("jvm.addVirtualMethod(\"").append(cls.getClsName()).append("\", \"")
                        .append(aliasName).append("\", ").append(aliasName).append(");\n");
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

    private static boolean appendStraightLineMethodBody(StringBuilder out, ByteCodeClass cls, BytecodeMethod method,
            List<Instruction> instructions, String jsMethodName) {
        if (!isStraightLineEligible(method, instructions)) {
            return false;
        }
        try {
            StringBuilder setup = new StringBuilder();
            StringBuilder instructionBody = new StringBuilder();
            StringBuilder body = new StringBuilder();
            StraightLineContext ctx = new StraightLineContext(method.getMaxLocals(), method.getMaxStack());
            if (isWrappedStaticMethod(method)) {
                ctx.initializedClasses.add(cls.getClsName());
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
            for (int i = 0; i < method.getMaxLocals(); i++) {
                if (!ctx.localsInitialized[i] && ctx.localsUsed[i]) {
                    body.append("  let l").append(i).append(" = null;\n");
                }
            }
            for (int i = 0; i < ctx.getMaxObservedStack(); i++) {
                body.append("  let s").append(i).append(" = null;\n");
            }
            if (method.isSynchronizedMethod()) {
                body.append("  const __cn1Monitor = ").append(method.isStatic() ? "jvm.getClassObject(\"" + cls.getClsName() + "\")" : "__cn1ThisObject").append(";\n");
                body.append("  jvm.monitorEnter(jvm.currentThread, __cn1Monitor);\n");
                body.append("  try {\n");
            }
            body.append(instructionBody);
            if (method.isSynchronizedMethod()) {
                body.append("  } finally {\n");
                body.append("    jvm.monitorExit(jvm.currentThread, __cn1Monitor);\n");
                body.append("  }\n");
            }
            out.append(body);
            out.append("}\n");
            if ("__CLINIT__".equals(method.getMethodName())) {
                out.append("jvm.classes[\"").append(cls.getClsName()).append("\"].clinit = ").append(jsMethodName).append(";\n");
            }
            if (!method.isStatic() && !method.isConstructor()) {
                out.append("jvm.addVirtualMethod(\"").append(cls.getClsName()).append("\", \"")
                        .append(jsMethodName).append("\", ").append(jsMethodName).append(");\n");
            }
            return true;
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("Straight-line JS lowering")) {
                return false;
            }
            throw ex;
        }
    }

    private static boolean isStraightLineEligible(BytecodeMethod method, List<Instruction> instructions) {
        if (method.isSynchronizedMethod()) {
            return false;
        }
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            if (instruction instanceof Jump || instruction instanceof SwitchInstruction || instruction instanceof TryCatch
                    || instruction instanceof MultiArray) {
                return false;
            }
            if (instruction instanceof BasicInstruction) {
                int opcode = ((BasicInstruction) instruction).getOpcode();
                if (opcode == Opcodes.MONITORENTER || opcode == Opcodes.MONITOREXIT || opcode == Opcodes.ATHROW) {
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
                out.append("  const ").append(temp).append(" = ").append(value).append(";\n");
                out.append("  ").append(ctx.push(temp)).append(";\n");
                return true;
            }
            case Opcodes.DUP_X1: {
                String v1 = ctx.pop();
                String v2 = ctx.pop();
                String t1 = ctx.nextTemp("__dup");
                String t2 = ctx.nextTemp("__dup");
                out.append("  const ").append(t1).append(" = ").append(v1).append(";\n");
                out.append("  const ").append(t2).append(" = ").append(v2).append(";\n");
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
                out.append("  const ").append(t1).append(" = ").append(v1).append(";\n");
                out.append("  const ").append(t2).append(" = ").append(v2).append(";\n");
                out.append("  const ").append(t3).append(" = ").append(v3).append(";\n");
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
                out.append("  const ").append(t1).append(" = ").append(v1).append(";\n");
                out.append("  const ").append(t2).append(" = ").append(v2).append(";\n");
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
                out.append("  const ").append(t1).append(" = ").append(v1).append(";\n");
                out.append("  const ").append(t2).append(" = ").append(v2).append(";\n");
                out.append("  const ").append(t3).append(" = ").append(v3).append(";\n");
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
                out.append("  const ").append(t1).append(" = ").append(v1).append(";\n");
                out.append("  const ").append(t2).append(" = ").append(v2).append(";\n");
                out.append("  const ").append(t3).append(" = ").append(v3).append(";\n");
                out.append("  const ").append(t4).append(" = ").append(v4).append(";\n");
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
                String arrayTemp = ctx.nextTemp("__arr");
                String indexTemp = ctx.nextTemp("__idx");
                out.append("  { const ").append(arrayTemp).append(" = ").append(arr)
                        .append("; const ").append(indexTemp).append(" = ").append(idx)
                        .append("; if (!").append(arrayTemp).append(" || !").append(arrayTemp).append(".__array) throw new Error(\"Array expected: \" + (")
                        .append(arrayTemp).append(" == null ? \"null\" : (").append(arrayTemp).append(".__class || typeof ").append(arrayTemp).append("))); if (")
                        .append(indexTemp).append(" < 0 || ").append(indexTemp).append(" >= ").append(arrayTemp)
                        .append(".length) throw new Error(\"ArrayIndexOutOfBoundsException\"); ")
                        .append(ctx.push(arrayTemp + "[" + indexTemp + "]")).append("; }\n");
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
                String arrayTemp = ctx.nextTemp("__arr");
                String indexTemp = ctx.nextTemp("__idx");
                out.append("  { const ").append(arrayTemp).append(" = ").append(arr)
                        .append("; const ").append(indexTemp).append(" = ").append(idx)
                        .append("; if (!").append(arrayTemp).append(" || !").append(arrayTemp).append(".__array) throw new Error(\"Array expected: \" + (")
                        .append(arrayTemp).append(" == null ? \"null\" : (").append(arrayTemp).append(".__class || typeof ").append(arrayTemp).append("))); if (")
                        .append(indexTemp).append(" < 0 || ").append(indexTemp).append(" >= ").append(arrayTemp)
                        .append(".length) throw new Error(\"ArrayIndexOutOfBoundsException\"); ")
                        .append(arrayTemp).append("[").append(indexTemp).append("] = ").append(value).append("; }\n");
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
                out.append("  ").append(ctx.push("jvm.newArray(" + size + ", \"" + primitiveArrayType(instruction.getIndex()) + "\", 1)")).append(";\n");
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
            out.append("  ").append(ctx.push("jvm.createStringLiteral(\"" + JavascriptNameUtil.escapeJs((String) value) + "\")")).append(";\n");
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
                out.append("  ").append(ctx.push("jvm.newObject(\"" + typeName + "\")")).append(";\n");
                return true;
            case Opcodes.ANEWARRAY: {
                String size = ctx.pop();
                out.append("  ").append(ctx.push("jvm.newArray(" + size + ", \"" + typeName + "\", 1)")).append(";\n");
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
        String owner = JavascriptNameUtil.sanitizeClassName(field.getOwner());
        String fieldName = field.getFieldName();
        String propertyName = JavascriptNameUtil.fieldProperty(field.getOwner(), fieldName);
        switch (field.getOpcode()) {
            case Opcodes.GETSTATIC:
                appendStraightLineEnsureClassInitialized(out, ctx, owner);
                out.append("  ").append(ctx.push("jvm.classes[\"" + owner + "\"].staticFields[\"" + fieldName + "\"]")).append(";\n");
                return true;
            case Opcodes.PUTSTATIC:
                appendStraightLineEnsureClassInitialized(out, ctx, owner);
                out.append("  jvm.classes[\"").append(owner).append("\"].staticFields[\"").append(fieldName).append("\"] = ")
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
            out.append("  jvm.ensureClassInitialized(\"").append(owner).append("\");\n");
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
            out.append("  {\n");
            out.append("    const __target = ").append(target).append(";\n");
            out.append("    const __classDef = __target.__classDef;\n");
            out.append("    const __method = ((__classDef && __classDef.methods) ? __classDef.methods[\"").append(methodId)
                    .append("\"] : null) || jvm.resolveVirtual(__target.__class, \"").append(methodId).append("\");\n");
            if (hasReturn) {
                out.append("    const __result = yield* __method(");
                appendInvocationArgumentExpressions(out, "__target", argValues);
                out.append(");\n");
                out.append("    ").append(ctx.push("__result")).append(";\n");
            } else {
                out.append("    yield* __method(");
                appendInvocationArgumentExpressions(out, "__target", argValues);
                out.append(");\n");
            }
            out.append("  }\n");
            return true;
        }
        if (invoke.getOpcode() == Opcodes.INVOKESTATIC) {
            appendStraightLineEnsureClassInitialized(out, ctx, owner);
        }
        String invokedName = invoke.getOpcode() == Opcodes.INVOKESTATIC
                ? "(" + staticInvocationTargetExpression(methodId, methodBodyId) + ")"
                : methodId;
        if (hasReturn) {
            out.append("  { const __result = yield* ").append(invokedName).append("(");
        } else {
            out.append("  { yield* ").append(invokedName).append("(");
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
        out.append("  const __cn1TryCatch = [");
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
            out.append("{ start: ").append(resolveLabelIndex(labelToIndex, tryCatch.getStart(), "try start"));
            out.append(", end: ").append(resolveLabelIndex(labelToIndex, tryCatch.getEnd(), "try end"));
            out.append(", handler: ").append(resolveLabelIndex(labelToIndex, tryCatch.getHandler(), "try handler"));
            out.append(", type: ");
            if (tryCatch.getType() == null) {
                out.append("null");
            } else {
                out.append("\"").append(JavascriptNameUtil.runtimeTypeName(tryCatch.getType())).append("\"");
            }
            out.append("}");
        }
        out.append("];\n");
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
            out.append("const __jsBodyResult = (function() { ").append(script).append(" }).call(this);\n");
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
        out.append("        { const sizes = new Array(").append(totalDimensions).append(");");
        out.append(" for (let i = ").append(allocatedDimensions - 1).append("; i >= 0; i--) { sizes[i] = stack.pop() | 0; }");
        out.append(" for (let i = ").append(allocatedDimensions).append("; i < ").append(totalDimensions)
                .append("; i++) { sizes[i] = -1; }");
        out.append(" stack.push(jvm.newMultiArray(sizes, \"").append(componentType).append("\", ")
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
        out.append("        const __switchValue = stack.pop() | 0;\n");
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
                out.append("        stack.push(null); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.ICONST_M1:
                out.append("        stack.push(-1); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
                out.append("        stack.push(").append(instruction.getOpcode() - Opcodes.ICONST_0).append("); pc = ")
                        .append(index + 1).append("; break;\n");
                return;
            case Opcodes.LCONST_0:
                out.append("        stack.push(0); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.LCONST_1:
                out.append("        stack.push(1); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.FCONST_0:
            case Opcodes.DCONST_0:
                out.append("        stack.push(0.0); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.FCONST_1:
            case Opcodes.DCONST_1:
                out.append("        stack.push(1.0); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.FCONST_2:
                out.append("        stack.push(2.0); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                out.append("        stack.push(").append(instruction.getValue()).append("); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.ILOAD:
            case Opcodes.LLOAD:
            case Opcodes.FLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ALOAD:
                out.append("        stack.push(locals[").append(instruction.getValue()).append("]); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.ISTORE:
            case Opcodes.LSTORE:
            case Opcodes.FSTORE:
            case Opcodes.DSTORE:
            case Opcodes.ASTORE:
                out.append("        locals[").append(instruction.getValue()).append("] = stack.pop(); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.POP:
                out.append("        stack.pop(); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.POP2:
                out.append("        stack.pop(); stack.pop(); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.DUP:
                out.append("        stack.push(stack[stack.length - 1]); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.DUP_X1:
                out.append("        { const v1 = stack.pop(); const v2 = stack.pop(); stack.push(v1); stack.push(v2); stack.push(v1); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.DUP_X2:
                out.append("        { const v1 = stack.pop(); const v2 = stack.pop(); const v3 = stack.pop(); stack.push(v1); stack.push(v3); stack.push(v2); stack.push(v1); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.DUP2:
                out.append("        { const v1 = stack.pop(); const v2 = stack.pop(); stack.push(v2); stack.push(v1); stack.push(v2); stack.push(v1); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.DUP2_X1:
                out.append("        { const v1 = stack.pop(); const v2 = stack.pop(); const v3 = stack.pop(); stack.push(v2); stack.push(v1); stack.push(v3); stack.push(v2); stack.push(v1); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.DUP2_X2:
                out.append("        { const v1 = stack.pop(); const v2 = stack.pop(); const v3 = stack.pop(); const v4 = stack.pop(); stack.push(v2); stack.push(v1); stack.push(v4); stack.push(v3); stack.push(v2); stack.push(v1); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.SWAP:
                out.append("        { const v1 = stack.pop(); const v2 = stack.pop(); stack.push(v1); stack.push(v2); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IADD:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push((a|0) + (b|0)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.ISUB:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push((a|0) - (b|0)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IMUL:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push((a|0) * (b|0)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LADD:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(a + b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LSUB:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(a - b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LMUL:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(a * b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.FADD:
            case Opcodes.DADD:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(a + b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.FSUB:
            case Opcodes.DSUB:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(a - b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.FMUL:
            case Opcodes.DMUL:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(a * b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IDIV:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(((a|0) / (b|0)) | 0); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LDIV:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(Math.trunc(a / b)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.FDIV:
            case Opcodes.DDIV:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(a / b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IREM:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push((a|0) % (b|0)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LREM:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(a % b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.FREM:
            case Opcodes.DREM:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(a % b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.INEG:
                out.append("        stack.push(-(stack.pop()|0)); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.LNEG:
                out.append("        stack.push(-stack.pop()); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.FNEG:
            case Opcodes.DNEG:
                out.append("        stack.push(-stack.pop()); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.ISHL:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push((a|0) << (b & 31)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LSHL:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(a * Math.pow(2, b & 63)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.ISHR:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push((a|0) >> (b & 31)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LSHR:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(Math.trunc(a / Math.pow(2, b & 63))); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IUSHR:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push((a >>> (b & 31)) | 0); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LUSHR:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(Math.floor((a < 0 ? a + 18446744073709551616 : a) / Math.pow(2, b & 63))); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IAND:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push((a|0) & (b|0)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LAND:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(a & b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IOR:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push((a|0) | (b|0)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LOR:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(a | b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IXOR:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push((a|0) ^ (b|0)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.LXOR:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(a ^ b); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.I2L:
            case Opcodes.F2D:
            case Opcodes.D2F:
                out.append("        pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.I2B:
                out.append("        stack.push((stack.pop() << 24) >> 24); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.I2C:
                out.append("        stack.push(stack.pop() & 65535); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.I2S:
                out.append("        stack.push((stack.pop() << 16) >> 16); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.L2I:
            case Opcodes.F2I:
            case Opcodes.D2I:
                out.append("        stack.push(stack.pop() | 0); pc = ").append(index + 1).append("; break;\n");
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
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push(a < b ? -1 : (a > b ? 1 : 0)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.FCMPL:
            case Opcodes.DCMPL:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push((isNaN(a) || isNaN(b)) ? -1 : (a < b ? -1 : (a > b ? 1 : 0))); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.FCMPG:
            case Opcodes.DCMPG:
                out.append("        { const b = stack.pop(); const a = stack.pop(); stack.push((isNaN(a) || isNaN(b)) ? 1 : (a < b ? -1 : (a > b ? 1 : 0))); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IRETURN:
            case Opcodes.ARETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
                out.append("        return stack.pop();\n");
                return;
            case Opcodes.ATHROW:
                out.append("        throw stack.pop();\n");
                return;
            case Opcodes.RETURN:
                out.append("        return null;\n");
                return;
            case Opcodes.ARRAYLENGTH:
                out.append("        { const arr = stack.pop(); stack.push(arr.length); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.AALOAD:
            case Opcodes.IALOAD:
            case Opcodes.LALOAD:
            case Opcodes.FALOAD:
            case Opcodes.DALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD:
                out.append("        { const idx = stack.pop(); const arr = stack.pop(); if (!arr || !arr.__array) throw new Error(\"Array expected: \" + (arr == null ? \"null\" : (arr.__class || typeof arr))); if (idx < 0 || idx >= arr.length) throw new Error(\"ArrayIndexOutOfBoundsException\"); stack.push(arr[idx]); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.AASTORE:
            case Opcodes.IASTORE:
            case Opcodes.LASTORE:
            case Opcodes.FASTORE:
            case Opcodes.DASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
                out.append("        { const value = stack.pop(); const idx = stack.pop(); const arr = stack.pop(); if (!arr || !arr.__array) throw new Error(\"Array expected: \" + (arr == null ? \"null\" : (arr.__class || typeof arr))); if (idx < 0 || idx >= arr.length) throw new Error(\"ArrayIndexOutOfBoundsException\"); arr[idx] = value; pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.MONITORENTER:
                out.append("        jvm.monitorEnter(jvm.currentThread, stack.pop()); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.MONITOREXIT:
                out.append("        jvm.monitorExit(jvm.currentThread, stack.pop()); pc = ").append(index + 1).append("; break;\n");
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
                out.append("        stack.push(").append(instruction.getIndex()).append("); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.NEWARRAY:
                out.append("        { const size = stack.pop(); stack.push(jvm.newArray(size, \"")
                        .append(primitiveArrayType(instruction.getIndex())).append("\", 1)); pc = ")
                        .append(index + 1).append("; break; }\n");
                return;
            case Opcodes.ILOAD:
            case Opcodes.LLOAD:
            case Opcodes.FLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ALOAD:
                out.append("        stack.push(locals[").append(instruction.getIndex()).append("]); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.ISTORE:
            case Opcodes.LSTORE:
            case Opcodes.FSTORE:
            case Opcodes.DSTORE:
            case Opcodes.ASTORE:
                out.append("        locals[").append(instruction.getIndex()).append("] = stack.pop(); pc = ").append(index + 1).append("; break;\n");
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
            out.append("        stack.push(jvm.createStringLiteral(\"")
                    .append(JavascriptNameUtil.escapeJs((String) value)).append("\")); pc = ").append(index + 1).append("; break;\n");
            return;
        }
        if (value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double) {
            out.append("        stack.push(").append(value.toString()).append("); pc = ").append(index + 1).append("; break;\n");
            return;
        }
        if (value instanceof Type) {
            Type type = (Type) value;
            if (type.getSort() == Type.OBJECT) {
                out.append("        stack.push(jvm.getClassObject(\"").append(JavascriptNameUtil.sanitizeClassName(type.getInternalName()))
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
                out.append("        stack.push(jvm.newObject(\"").append(typeName).append("\")); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.ANEWARRAY:
                out.append("        { const size = stack.pop(); stack.push(jvm.newArray(size, \"").append(typeName)
                        .append("\", 1)); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.CHECKCAST:
                out.append("        { const value = stack[stack.length - 1]; ");
                appendDirectCheckCast(out, "", "value", typeName, "throw new Error(\"ClassCastException\")");
                out.append(" pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.INSTANCEOF:
                out.append("        { const value = stack.pop(); stack.push(").append(directInstanceOfExpression("value", typeName))
                        .append(" ? 1 : 0); pc = ").append(index + 1).append("; break; }\n");
                return;
            default:
                throw new IllegalArgumentException("Unsupported type opcode " + instruction.getOpcode());
        }
    }

    private static void appendDirectCheckCast(StringBuilder out, String indent, String valueExpression, String typeName, String failureStatement) {
        out.append(indent).append("if (").append(valueExpression).append(" != null) { const __classDef = ").append(valueExpression)
                .append(".__classDef; if (").append(valueExpression).append(".__class !== \"").append(typeName)
                .append("\" && !(__classDef && __classDef.assignableTo && __classDef.assignableTo[\"").append(typeName)
                .append("\"])) {")
                .append(" if (").append(valueExpression).append(".__jsValue !== undefined) {")
                .append(" jvm.enhanceJsWrapper(").append(valueExpression).append(", \"").append(typeName).append("\");")
                .append(" const __enhancedClassDef = ").append(valueExpression).append(".__classDef;")
                .append(" if (").append(valueExpression).append(".__class !== \"").append(typeName)
                .append("\" && !(__enhancedClassDef && __enhancedClassDef.assignableTo && __enhancedClassDef.assignableTo[\"").append(typeName)
                .append("\"])) ").append(failureStatement).append(";")
                .append(" } else ").append(failureStatement).append(";")
                .append(" } }\n");
    }

    private static String directInstanceOfExpression(String valueExpression, String typeName) {
        return "(" + valueExpression + " != null && (" + valueExpression + ".__class === \"" + typeName
                + "\" || (" + valueExpression + ".__classDef && " + valueExpression + ".__classDef.assignableTo && "
                + valueExpression + ".__classDef.assignableTo[\"" + typeName + "\"])))";
    }

    private static void appendFieldInstruction(StringBuilder out, Field field, int index, boolean usesStaticFieldInitCache) {
        String owner = JavascriptNameUtil.sanitizeClassName(field.getOwner());
        String fieldName = field.getFieldName();
        String propertyName = JavascriptNameUtil.fieldProperty(field.getOwner(), fieldName);
        switch (field.getOpcode()) {
            case Opcodes.GETSTATIC:
                appendInterpreterEnsureClassInitialized(out, owner, usesStaticFieldInitCache);
                out.append("        stack.push(jvm.classes[\"").append(owner).append("\"].staticFields[\"")
                        .append(fieldName).append("\"]); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.PUTSTATIC:
                appendInterpreterEnsureClassInitialized(out, owner, usesStaticFieldInitCache);
                out.append("        jvm.classes[\"").append(owner).append("\"].staticFields[\"").append(fieldName)
                        .append("\"] = stack.pop(); pc = ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.GETFIELD:
                out.append("        { const target = stack.pop(); stack.push(target[\"").append(propertyName)
                        .append("\"]); pc = ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.PUTFIELD:
                out.append("        { const value = stack.pop(); const target = stack.pop(); target[\"").append(propertyName)
                        .append("\"] = value; pc = ").append(index + 1).append("; break; }\n");
                return;
            default:
                throw new IllegalArgumentException("Unsupported field opcode " + field.getOpcode());
        }
    }

    private static void appendInterpreterEnsureClassInitialized(StringBuilder out, String owner, boolean usesStaticFieldInitCache) {
        if (usesStaticFieldInitCache) {
            out.append("        if (!__cn1Init[\"").append(owner).append("\"]) { jvm.ensureClassInitialized(\"")
                    .append(owner).append("\"); __cn1Init[\"").append(owner).append("\"] = true; }\n");
            return;
        }
        out.append("        jvm.ensureClassInitialized(\"").append(owner).append("\");\n");
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
                out.append("        pc = ((stack.pop()|0) == 0) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.IFNE:
                out.append("        pc = ((stack.pop()|0) != 0) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.IFLT:
                out.append("        pc = ((stack.pop()|0) < 0) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.IFLE:
                out.append("        pc = ((stack.pop()|0) <= 0) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.IFGT:
                out.append("        pc = ((stack.pop()|0) > 0) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.IFGE:
                out.append("        pc = ((stack.pop()|0) >= 0) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.IFNULL:
                out.append("        pc = (stack.pop() == null) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.IFNONNULL:
                out.append("        pc = (stack.pop() != null) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break;\n");
                return;
            case Opcodes.IF_ICMPEQ:
                out.append("        { const b = stack.pop(); const a = stack.pop(); pc = ((a|0) == (b|0)) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IF_ICMPNE:
                out.append("        { const b = stack.pop(); const a = stack.pop(); pc = ((a|0) != (b|0)) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IF_ICMPLT:
                out.append("        { const b = stack.pop(); const a = stack.pop(); pc = ((a|0) < (b|0)) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IF_ICMPLE:
                out.append("        { const b = stack.pop(); const a = stack.pop(); pc = ((a|0) <= (b|0)) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IF_ICMPGT:
                out.append("        { const b = stack.pop(); const a = stack.pop(); pc = ((a|0) > (b|0)) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IF_ICMPGE:
                out.append("        { const b = stack.pop(); const a = stack.pop(); pc = ((a|0) >= (b|0)) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IF_ACMPEQ:
                out.append("        { const b = stack.pop(); const a = stack.pop(); pc = (a === b) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break; }\n");
                return;
            case Opcodes.IF_ACMPNE:
                out.append("        { const b = stack.pop(); const a = stack.pop(); pc = (a !== b) ? ").append(target.intValue()).append(" : ").append(index + 1).append("; break; }\n");
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
            out.append("        {\n");
            appendInvocationArgumentBindings(out, argCount, "          ", "stack.pop()");
            out.append("          const __target = stack.pop();\n");
            out.append("          const __classDef = __target.__classDef;\n");
            out.append("          let __method = (__classDef && __classDef.methods) ? __classDef.methods[\"").append(methodId)
                    .append("\"] : null;\n");
            if (usesVirtualDispatchCache) {
                out.append("          if (!__method) {\n");
                out.append("            const __cacheKey = __target.__class + \"|").append(methodId).append("\";\n");
                out.append("            __method = __cn1Virtual[__cacheKey];\n");
                out.append("            if (!__method) {\n");
                out.append("              __method = jvm.resolveVirtual(__target.__class, \"").append(methodId).append("\");\n");
                out.append("              __cn1Virtual[__cacheKey] = __method;\n");
                out.append("            }\n");
                out.append("          }\n");
            } else {
                out.append("          if (!__method) __method = jvm.resolveVirtual(__target.__class, \"").append(methodId).append("\");\n");
            }
            if (hasReturn) {
                out.append("          const __result = yield* __method(");
                appendInvocationArguments(out, true, argCount);
                out.append(");\n");
                out.append("          stack.push(__result);\n");
            } else {
                out.append("          yield* __method(");
                appendInvocationArguments(out, true, argCount);
                out.append(");\n");
            }
            out.append("          pc = ").append(index + 1).append("; break;\n");
            out.append("        }\n");
            return;
        }

        out.append("        {\n");
        appendInvocationArgumentBindings(out, argCount, "          ", "stack.pop()");
        if (invoke.getOpcode() != Opcodes.INVOKESTATIC) {
            out.append("          const __target = stack.pop();\n");
        } else {
            appendInterpreterEnsureClassInitialized(out, owner, usesClassInitCache);
        }
        String invokedName = invoke.getOpcode() == Opcodes.INVOKESTATIC
                ? "(" + staticInvocationTargetExpression(methodId, methodBodyId) + ")"
                : methodId;
        if (hasReturn) {
            out.append("          const __result = yield* ").append(invokedName).append("(");
        } else {
            out.append("          yield* ").append(invokedName).append("(");
        }
        appendInvocationArguments(out, invoke.getOpcode() != Opcodes.INVOKESTATIC, argCount);
        out.append(");\n");
        if (hasReturn) {
            out.append("          stack.push(__result);\n");
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
            out.append(indent).append("const __arg").append(i).append(" = ").append(sourceExpression).append(";\n");
        }
    }

    private static String staticInvocationTargetExpression(String methodId, String methodBodyId) {
        return "typeof " + methodBodyId + " === \"function\" ? " + methodBodyId + " : " + methodId;
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
