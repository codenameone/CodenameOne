package com.codename1.tools.translator;

import com.codename1.tools.translator.bytecodes.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/** Immediate array pipelines. No stream, cursor or lambda allocation escapes this loop. */
final class StreamFusion {
    private static final String STREAM = "java/util/stream/Stream";

    private static final class Stage {
        Invoke operation, factory;
        final List<Instruction> arguments = new ArrayList<Instruction>();
        int captureRoot;
    }

    static List<Invoke> lower(BytecodeMethod method) {
        List<Invoke> dependencies = new ArrayList<Invoke>();
        List<Instruction> code = method.getInstructions();
        // This first lowering handles straight-line construction only. A label in
        // such a method is debug metadata, never a branch or exception entry.
        for (Instruction instruction : code) {
            if (instruction instanceof Jump || instruction instanceof SwitchInstruction
                    || instruction instanceof TryCatch) return dependencies;
        }
        for (int first = 0; first < code.size(); first++) {
            Instruction ins = code.get(first);
            if (!(ins instanceof Invoke)) continue;
            Invoke source = (Invoke) ins;
            if (source.getOpcode() != Opcodes.INVOKESTATIC || !STREAM.equals(source.getOwner())
                    || !"of".equals(source.getName())
                    || !"([Ljava/lang/Object;)Ljava/util/stream/Stream;".equals(source.getDesc())) continue;
            List<Stage> stages = new ArrayList<Stage>();
            Stage stage = new Stage();
            int end = first + 1;
            boolean complete = false;
            for (; end < code.size(); end++) {
                Instruction next = code.get(end);
                if (next instanceof LineNumber || next instanceof LabelInstruction) continue;
                if (isLoad(next) && stage.factory == null) {
                    stage.arguments.add(next);
                    continue;
                }
                if (!(next instanceof Invoke)) break;
                Invoke call = (Invoke) next;
                if (call.getLambdaSam() != null && stage.factory == null) {
                    Type[] captures = Type.getArgumentTypes(call.getDesc());
                    if (captures.length != stage.arguments.size()) break;
                    boolean compatible = true;
                    for (int i = 0; i < captures.length; i++) {
                        if (kindOf(stage.arguments.get(i)) != kind(captures[i])) compatible = false;
                    }
                    if (!compatible) break;
                    stage.factory = call;
                    continue;
                }
                if (!STREAM.equals(call.getOwner()) || call.getOpcode() != Opcodes.INVOKEINTERFACE) break;
                String name = call.getName();
                boolean terminal = "count".equals(name) || "forEach".equals(name)
                        || "anyMatch".equals(name) || "allMatch".equals(name) || "noneMatch".equals(name);
                String sam = "map".equals(name) ? "apply" : "forEach".equals(name) ? "accept" : "test";
                String signature = "map".equals(name) ? "(Ljava/lang/Object;)Ljava/lang/Object;"
                        : "forEach".equals(name) ? "(Ljava/lang/Object;)V" : "(Ljava/lang/Object;)Z";
                if ("limit".equals(name) || "skip".equals(name)) {
                    if (stage.factory != null || stage.arguments.size() != 1
                            || kindOf(stage.arguments.get(0)) != 'l' || !"(J)Ljava/util/stream/Stream;".equals(call.getDesc())) break;
                } else if ("count".equals(name)) {
                    if (stage.factory != null || !stage.arguments.isEmpty() || !"()J".equals(call.getDesc())) break;
                } else {
                    if (!terminal && !"filter".equals(name) && !"map".equals(name)) break;
                    if (stage.factory == null || !sam.equals(stage.factory.getLambdaSam().getMethodName())
                            || !signature.equals(stage.factory.getLambdaSam().getSignature())) break;
                }
                stage.operation = call;
                stages.add(stage);
                if (terminal) { complete = true; break; }
                stage = new Stage();
            }
            if (!complete) continue;
            int roots = method.getMaxLocals();
            int nextRoot = roots + 2; // source and current element
            for (Stage item : stages) {
                item.captureRoot = nextRoot;
                if (item.factory != null) {
                    nextRoot += Type.getArgumentTypes(item.factory.getDesc()).length;
                    BytecodeMethod sam = item.factory.getLambdaSam();
                    dependencies.add(new Invoke(Opcodes.INVOKESPECIAL, item.factory.getOwner(),
                            sam.getMethodName(), sam.getSignature(), false));
                }
            }
            method.setMaxes(Math.max(2, method.getMaxStack()), nextRoot);
            Fused fused = new Fused(stages, roots, nextRoot);
            fused.setMethod(method);
            fused.addDependencies(method.getDependentClasses());
            code.subList(first, end + 1).clear();
            code.add(first, fused);
        }
        return dependencies;
    }

    private static char kind(Type type) {
        switch (type.getSort()) {
            case Type.OBJECT: case Type.ARRAY: return 'o';
            case Type.LONG: return 'l';
            case Type.DOUBLE: return 'd';
            case Type.FLOAT: return 'f';
            default: return 'i';
        }
    }
    private static char kindOf(Instruction ins) {
        int op = ins.getOpcode();
        if (ins instanceof Ldc) {
            Object value = ((Ldc)ins).getValue();
            if (value instanceof Long) return 'l';
            if (value instanceof Double) return 'd';
            if (value instanceof Float) return 'f';
            if (value instanceof Integer) return 'i';
            return '?';
        }
        if (op == Opcodes.ALOAD || op == Opcodes.ACONST_NULL) return 'o';
        if (op == Opcodes.LLOAD || op == Opcodes.LCONST_0 || op == Opcodes.LCONST_1) return 'l';
        if (op == Opcodes.DLOAD || op == Opcodes.DCONST_0 || op == Opcodes.DCONST_1) return 'd';
        if (op == Opcodes.FLOAD || op >= Opcodes.FCONST_0 && op <= Opcodes.FCONST_2) return 'f';
        if (op == Opcodes.ILOAD || op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5
                || op == Opcodes.BIPUSH || op == Opcodes.SIPUSH) return 'i';
        return '?';
    }
    private static boolean isLoad(Instruction ins) {
        int op = ins.getOpcode();
        return (ins instanceof VarOp && op >= Opcodes.ILOAD && op <= Opcodes.ALOAD)
                || (ins.isConstant() && kindOf(ins) != '?');
    }
    private static String root(int slot) { return "locals[" + slot + "].data.o"; }
    private static void save(StringBuilder out, int slot, String value) {
        out.append(root(slot)).append(" = ").append(value).append("; locals[").append(slot)
                .append("].type = CN1_TYPE_OBJECT;\n");
    }
    private static String callback(Stage stage, int index, String element) {
        BytecodeMethod sam = stage.factory.getLambdaSam();
        StringBuilder name = new StringBuilder(Util.mangle(stage.factory.getOwner()));
        name.append('_').append(sam.getMethodName()).append("__");
        BytecodeMethod.appendMethodSignatureSuffixFromDesc(sam.getSignature(), name, new ArrayList<String>());
        return name + "(threadStateData, (JAVA_OBJECT)&__sfLambda" + index + ", " + element + ")";
    }

    static final class Dependency extends Instruction {
        final Invoke call;
        Dependency(Invoke call) { super(-1); this.call = call; }
        @Override public String getMethodUsed() { return call.getMethodUsed(); }
        @Override public String getMethodName() { return call.getMethodName(); }
        @Override public String getSignature() { return call.getSignature(); }
        @Override public boolean containsSignature(SignatureSet signature) { return call.containsSignature(signature); }
        @Override public void addDependencies(List<String> dependencies) { call.addDependencies(dependencies); }
    }

    private static final class Fused extends Instruction {
        final List<Stage> stages;
        final int roots, endRoot;
        Fused(List<Stage> stages, int roots, int endRoot) {
            super(-1); this.stages = stages; this.roots = roots; this.endRoot = endRoot;
        }
        @Override public boolean isComplexInstruction() { return true; }
        @Override public void addDependencies(List<String> dependencies) {
            dependencies.add("java_lang_IllegalArgumentException");
            for (Stage stage : stages) if (stage.factory != null) dependencies.add(Util.mangle(stage.factory.getOwner()));
        }
        @Override public void appendInstruction(StringBuilder out, List<Instruction> ignored) {
            out.append("{ /* fused array stream; callbacks cannot escape */\n");
            save(out, roots, "SP[-1].data.o");
            out.append("POP_MANY(1);\nif (").append(root(roots)).append(" == JAVA_NULL) { cn1ThrowNullPointerHere(threadStateData); }\n");
            for (int i = 0; i < stages.size(); i++) {
                Stage stage = stages.get(i);
                for (Instruction argument : stage.arguments) argument.appendInstruction(out, Collections.<Instruction>emptyList());
                if (stage.factory != null) {
                    String cls = Util.mangle(stage.factory.getOwner());
                    Type[] captures = Type.getArgumentTypes(stage.factory.getDesc());
                    out.append("__STATIC_INITIALIZER_").append(cls).append("(threadStateData);\n");
                    out.append("struct obj__").append(cls).append(" __sfLambda").append(i).append(" = {0};\n")
                            .append("CN1_OBJ_SET_CLASS(&__sfLambda").append(i).append(", &class__").append(cls).append(");\n")
                            .append("CN1_OBJ_SET_MARK(&__sfLambda").append(i).append(", CN1_GC_MARK_FRESH);\n")
                            .append("CN1_OBJ_SET_HEAPPOS(&__sfLambda").append(i).append(", -1);\n");
                    for (int c = 0; c < captures.length; c++) {
                        char type = kind(captures[c]);
                        String value = "SP[-" + (captures.length - c) + "].data." + type;
                        if (type == 'o') {
                            save(out, stage.captureRoot + c, value);
                            value = root(stage.captureRoot + c);
                        }
                        out.append("__sfLambda").append(i).append('.').append(cls).append("_arg_").append(c + 1)
                                .append(" = ").append(value).append(";\n");
                    }
                    out.append("POP_MANY(").append(captures.length).append(");\n");
                } else if (!stage.arguments.isEmpty()) {
                    out.append("JAVA_LONG __sfRemaining").append(i).append(" = SP[-1].data.l; POP_MANY(1);\n")
                            .append("if (__sfRemaining").append(i).append(" < 0) {\n")
                            .append("throwException(threadStateData, __NEW_INSTANCE_java_lang_IllegalArgumentException(threadStateData)); }\n");
                }
            }
            String terminal = stages.get(stages.size() - 1).operation.getName();
            out.append("JAVA_LONG __sfCount = 0; JAVA_INT __sfResult = ")
                    .append("allMatch".equals(terminal) || "noneMatch".equals(terminal) ? 1 : 0).append(";\n")
                    .append("for (JAVA_INT __sfIndex = 0; __sfIndex < ((JAVA_ARRAY)").append(root(roots)).append(")->length; __sfIndex++) {\n");
            for (int i = 0; i < stages.size(); i++) if ("limit".equals(stages.get(i).operation.getName()))
                out.append("if (__sfRemaining").append(i).append(" == 0) break;\n");
            save(out, roots + 1, "((JAVA_OBJECT*)CN1_ARRAY_DATA((JAVA_ARRAY)" + root(roots) + "))[__sfIndex]");
            for (int i = 0; i < stages.size(); i++) {
                Stage stage = stages.get(i);
                String name = stage.operation.getName();
                String call = stage.factory == null ? null : callback(stage, i, root(roots + 1));
                if ("filter".equals(name)) out.append("if (!").append(call).append(") continue;\n");
                else if ("map".equals(name)) save(out, roots + 1, call);
                else if ("skip".equals(name)) out.append("if (__sfRemaining").append(i).append(" > 0) { __sfRemaining").append(i).append("--; continue; }\n");
                else if ("limit".equals(name)) out.append("__sfRemaining").append(i).append("--;\n");
                else if ("count".equals(name)) out.append("__sfCount++;\n");
                else if ("forEach".equals(name)) out.append(call).append(";\n");
                else out.append("if (").append("allMatch".equals(name) ? "!" : "").append(call)
                        .append(") { __sfResult = ").append("anyMatch".equals(name) ? 1 : 0).append("; break; }\n");
            }
            out.append("}\n");
            for (int slot = roots; slot < endRoot; slot++) out.append(root(slot)).append(" = JAVA_NULL;\n");
            if ("count".equals(terminal)) out.append("PUSH_LONG(__sfCount);\n");
            else if (!"forEach".equals(terminal)) out.append("PUSH_INT(__sfResult);\n");
            out.append("}\n");
        }
    }
}
