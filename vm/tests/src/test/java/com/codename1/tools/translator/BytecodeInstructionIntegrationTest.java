package com.codename1.tools.translator;
import com.codename1.tools.translator.bytecodes.ArithmeticExpression;
import com.codename1.tools.translator.bytecodes.ArrayLengthExpression;
import com.codename1.tools.translator.bytecodes.ArrayLoadExpression;
import com.codename1.tools.translator.bytecodes.AssignableExpression;
import com.codename1.tools.translator.bytecodes.BasicInstruction;
import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.Ldc;
import com.codename1.tools.translator.bytecodes.LineNumber;
import com.codename1.tools.translator.bytecodes.LocalVariable;
import com.codename1.tools.translator.bytecodes.MultiArray;
import com.codename1.tools.translator.bytecodes.VarOp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeInstructionIntegrationTest {

    static Stream<CompilerHelper.CompilerConfig> provideCompilerConfigs() {
        List<CompilerHelper.CompilerConfig> configs = new ArrayList<>();
        configs.addAll(CompilerHelper.getAvailableCompilers("1.8"));
        configs.addAll(CompilerHelper.getAvailableCompilers("11"));
        configs.addAll(CompilerHelper.getAvailableCompilers("17"));
        configs.addAll(CompilerHelper.getAvailableCompilers("21"));
        configs.addAll(CompilerHelper.getAvailableCompilers("25"));
        return configs.stream();
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("provideCompilerConfigs")
    void translatesOptimizedBytecodeToLLVMExecutable(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("bytecode-integration-sources");
        Path classesDir = Files.createTempDirectory("bytecode-integration-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");

        Files.write(sourceDir.resolve("BytecodeInstructionApp.java"), appSource().getBytes(StandardCharsets.UTF_8));

        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");
        CompilerHelper.compileJavaAPI(javaApiDir, config);

        Path nativeReport = sourceDir.resolve("native_report.c");
        Files.write(nativeReport, nativeReportSource().getBytes(StandardCharsets.UTF_8));

        // Compile App using the specific JDK
        List<String> compileArgs = new ArrayList<>();

        int jdkMajor = CompilerHelper.getJdkMajor(config);

        if (jdkMajor >= 9) {
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             // On JDK 9+, -bootclasspath is removed.
             // We rely on the JDK's own bootstrap classes but include our JavaAPI in classpath
             // so that any non-replaced classes are found.
             compileArgs.add("-classpath");
             compileArgs.add(javaApiDir.toString());
        } else {
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-bootclasspath");
             compileArgs.add(javaApiDir.toString());
             compileArgs.add("-Xlint:-options");
        }

        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(sourceDir.resolve("BytecodeInstructionApp.java").toString());

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "BytecodeInstructionApp should compile with " + config);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Files.copy(nativeReport, classesDir.resolve("native_report.c"));

        Path outputDir = Files.createTempDirectory("bytecode-integration-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "CustomBytecodeApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project for the optimized sample");

        Path srcRoot = distDir.resolve("CustomBytecodeApp-src");

        Path generatedSource = findGeneratedSource(srcRoot);
        String generatedCode = new String(Files.readAllBytes(generatedSource), StandardCharsets.UTF_8);
        assertTrue(generatedCode.contains("CustomJump */"), "Optimized comparisons should emit CustomJump code");
        assertTrue(generatedCode.contains("BC_IINC"), "Increment operations should translate to BC_IINC macro");
        assertTrue(generatedCode.contains("VarOp.assignFrom"), "Optimized stores should rely on CustomIntruction output");
        assertTrue(generatedCode.contains("switch((*SP).data.i)"), "SwitchInstruction should emit a native switch statement");
        assertTrue(generatedCode.contains("BC_DUP(); /* DUP */"), "DupExpression should translate DUP operations to BC_DUP");

        // New assertions for expanded coverage
        assertTrue(generatedCode.contains("monitorExitBlock"), "Synchronized method should emit monitorExitBlock");
        assertTrue(generatedCode.contains("BC_ISHL_EXPR"), "Shift left should translate to BC_ISHL_EXPR");
        assertTrue(generatedCode.contains("BC_ISHR_EXPR"), "Shift right should translate to BC_ISHR_EXPR");
        assertTrue(generatedCode.contains("BC_IUSHR_EXPR"), "Unsigned shift right should translate to BC_IUSHR_EXPR");
        assertTrue(generatedCode.contains("fmod"), "Remainder should translate to fmod for floats/doubles");
        assertTrue(generatedCode.contains("allocArray"), "New array should translate to allocArray");

        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, srcRoot.getFileName().toString());

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_C_COMPILER=clang",
                "-DCMAKE_OBJC_COMPILER=clang"
        ), distDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve("CustomBytecodeApp");
        String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
        assertTrue(output.contains("RESULT="), "Compiled program should print the expected arithmetic result");
    }

    private String invokeLdcLocalVarsAppSource() {
        return "public class InvokeLdcLocalVarsApp {\n" +
                "    private static native void report(int value);\n" +
                "    private interface Multiplier { int multiply(int a, int b); }\n" +
                "    private static class MultiplierImpl implements Multiplier {\n" +
                "        public int multiply(int a, int b) { return a * b; }\n" +
                "    }\n" +
                "    private int seed;\n" +
                "    public InvokeLdcLocalVarsApp(int seed) {\n" +
                "        this.seed = seed;\n" +
                "    }\n" +
                "    private int constantsAndLocals(int extra) {\n" +
                "        int intVal = 21;\n" +
                "        long min = Long.MIN_VALUE;\n" +
                "        if (seed > 0) min = Long.MIN_VALUE; /* Force LDC */\n" +
                "        float nan = Float.NaN;\n" +
                "        double posInf = Double.POSITIVE_INFINITY;\n" +
                "        String label = \"TranslatorLdc\";\n" +
                "        Class objectType = java.util.ArrayList.class;\n" +
                "        Class arrayType = String[][].class;\n" +
                "        int[] counts = new int[] { seed, extra, 12 };\n" +
                "        int labelBonus = label != null ? 4 : 0;\n" +
                "        int classTally = (objectType != null ? 9 : 0) + (arrayType != null ? 20 : 0);\n" +
                "        byte small = 2;\n" +
                "        short shorty = 12;\n" +
                "        boolean flag = Double.isInfinite(posInf);\n" +
                "        char initial = 'Z';\n" +
                "        float nanFallback = nan;\n" +
                "        double sum = posInf;\n" +
                "        if (nan != nanFallback) {\n" +
                "            return -1;\n" +
                "        }\n" +
                "        return intVal + counts.length + labelBonus + classTally + (flag ? 5 : 0) + small + shorty + initial + (min == Long.MIN_VALUE ? 7 : 0) + extra + seed + (sum > 0 ? 3 : 0);\n" +
                "    }\n" +
                "    private static int staticInvoke(int value) {\n" +
                "        return value * 3;\n" +
                "    }\n" +
                "    private int instanceInvoke(int value) {\n" +
                "        return value + seed;\n" +
                "    }\n" +
                "    private static int interfaceInvoke(Multiplier multiplier, int a, int b) {\n" +
                "        return multiplier.multiply(a, b);\n" +
                "    }\n" +
                "    public static void main(String[] args) {\n" +
                "        InvokeLdcLocalVarsApp app = new InvokeLdcLocalVarsApp(4);\n" +
                "        Multiplier multiplier = new MultiplierImpl();\n" +
                "        int combined = staticInvoke(5)\n" +
                "                + app.instanceInvoke(6)\n" +
                "                + interfaceInvoke(multiplier, 2, 7)\n" +
                "                + app.constantsAndLocals(3);\n" +
                "        report(combined);\n" +
                "    }\n" +
                "}\n";
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("provideCompilerConfigs")
    void translatesInvokeAndLdcBytecodeToLLVMExecutable(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("invoke-ldc-sources");
        Path classesDir = Files.createTempDirectory("invoke-ldc-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");

        Files.write(sourceDir.resolve("InvokeLdcLocalVarsApp.java"), invokeLdcLocalVarsAppSource().getBytes(StandardCharsets.UTF_8));

        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");
        CompilerHelper.compileJavaAPI(javaApiDir, config);

        Path nativeReport = sourceDir.resolve("native_report.c");
        Files.write(nativeReport, nativeReportSource().getBytes(StandardCharsets.UTF_8));

        List<String> compileArgs = new ArrayList<>();

        int jdkMajor = CompilerHelper.getJdkMajor(config);

        if (jdkMajor >= 9) {
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-classpath");
             compileArgs.add(javaApiDir.toString());
        } else {
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-bootclasspath");
             compileArgs.add(javaApiDir.toString());
             compileArgs.add("-Xlint:-options");
        }

        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(sourceDir.resolve("InvokeLdcLocalVarsApp.java").toString());

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "InvokeLdcLocalVarsApp should compile with " + config);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Files.copy(nativeReport, classesDir.resolve("native_report.c"));

        Path outputDir = Files.createTempDirectory("invoke-ldc-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "InvokeLdcLocalVars");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project for Invoke/Ldc sample");

        Path srcRoot = distDir.resolve("InvokeLdcLocalVars-src");

        Path generatedSource = findGeneratedSource(srcRoot, "InvokeLdcLocalVarsApp");
        String generatedCode = new String(Files.readAllBytes(generatedSource), StandardCharsets.UTF_8);
        assertTrue(generatedCode.contains("0.0/0.0"), "NaN constants should translate through Ldc");
        assertTrue(generatedCode.contains("1.0 / 0.0"), "Infinite double constants should translate through Ldc");
        // assertTrue(generatedCode.contains("LLONG_MIN"), "Long minimum value should pass through Ldc handling");
        assertTrue(generatedCode.contains("class_array2__java_lang_String"), "Array class literals should be emitted via Ldc");
        assertTrue(generatedCode.contains("class__java_util_ArrayList"), "Object class literals should be emitted via Ldc");
        assertTrue(generatedCode.contains("llocals_3_"), "Local variable generation should declare long locals");
        assertTrue(generatedCode.contains("locals[9].data.o"), "Local variable generation should declare object locals");
        assertTrue(generatedCode.contains("InvokeLdcLocalVarsApp_instanceInvoke"), "Virtual invokes should be routed through Invoke helper");
        assertTrue(generatedCode.contains("InvokeLdcLocalVarsApp_interfaceInvoke"), "Static invokes should include helper method routing");

        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, srcRoot.getFileName().toString());

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_C_COMPILER=clang",
                "-DCMAKE_OBJC_COMPILER=clang"
        ), distDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve("InvokeLdcLocalVars");
        String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
        assertTrue(output.contains("RESULT="), "Compiled program should print the expected Invoke/Ldc result");
    }

    @Test
    void translatesJava17StringConcatInvokeDynamic() throws Exception {
        Parser.cleanup();

        List<CompilerHelper.CompilerConfig> configs = CompilerHelper.getAvailableCompilers("17");
        CompilerHelper.CompilerConfig config = configs.stream()
                .filter(c -> CompilerHelper.getJdkMajor(c) >= 17)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No JDK 17+ compiler available for invokedynamic string concat test"));

        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");

        Path sourceDir = Files.createTempDirectory("concat-indy-sources");
        Path classesDir = Files.createTempDirectory("concat-indy-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");

        Files.write(sourceDir.resolve("StringConcatInvokeDynamicApp.java"),
                stringConcatInvokeDynamicAppSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("native_report.c"), nativeReportSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.compileJavaAPI(javaApiDir, config);

        List<String> compileArgs = new ArrayList<>();
        compileArgs.add("-source");
        compileArgs.add("17");
        compileArgs.add("-target");
        compileArgs.add("17");
        compileArgs.add("-classpath");
        compileArgs.add(javaApiDir.toString());
        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(sourceDir.resolve("StringConcatInvokeDynamicApp.java").toString());

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "StringConcatInvokeDynamicApp should compile with " + config);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        Files.copy(sourceDir.resolve("native_report.c"), classesDir.resolve("native_report.c"));

        Path outputDir = Files.createTempDirectory("concat-indy-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "StringConcatInvokeDynamic");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project for invokedynamic string concat sample");

        Path srcRoot = distDir.resolve("StringConcatInvokeDynamic-src");
        Path generatedSource = findGeneratedSource(srcRoot, "StringConcatInvokeDynamicApp");
        String generatedCode = new String(Files.readAllBytes(generatedSource), StandardCharsets.UTF_8);

        assertTrue(generatedCode.contains("__NEW_java_lang_StringBuilder"),
                "String concat invokedynamic should translate into StringBuilder allocation");
        assertTrue(generatedCode.contains("virtual_java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder"),
                "String concat invokedynamic should append string segments");
        assertTrue(generatedCode.contains("virtual_java_lang_StringBuilder_append___int_R_java_lang_StringBuilder"),
                "String concat invokedynamic should append primitive values");
        assertTrue(generatedCode.contains("virtual_java_lang_StringBuilder_toString___R_java_lang_String"),
                "String concat invokedynamic should finalize to String");

        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, srcRoot.getFileName().toString());

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_C_COMPILER=clang",
                "-DCMAKE_OBJC_COMPILER=clang"
        ), distDir);

        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve("StringConcatInvokeDynamic");
        String output = CleanTargetIntegrationTest.runCommand(Arrays.asList(executable.toString()), buildDir);
        assertTrue(output.contains("RESULT=1"),
                "Translated executable should preserve invokedynamic concat semantics. Output:\n" + output);
    }

    private Set<String> snapshotArrayTypes() throws Exception {
        Field arrayTypesField = ByteCodeClass.class.getDeclaredField("arrayTypes");
        arrayTypesField.setAccessible(true);
        return new TreeSet<>((Set<String>) arrayTypesField.get(null));
    }

    private void restoreArrayTypes(Set<String> snapshot) throws Exception {
        Field arrayTypesField = ByteCodeClass.class.getDeclaredField("arrayTypes");
        arrayTypesField.setAccessible(true);
        arrayTypesField.set(null, new TreeSet<>(snapshot));
    }

    private static class StubAssignableExpression extends Instruction implements AssignableExpression {
        private final String expression;
        int dependencyCalls;

        private StubAssignableExpression(int opcode, String expression) {
            super(opcode);
            this.expression = expression;
        }

        @Override
        public void addDependencies(List<String> dependencyList) {
            dependencyCalls++;
            dependencyList.add(expression);
        }

        @Override
        public void appendInstruction(StringBuilder b) {
            b.append(expression);
        }

        @Override
        public boolean assignTo(String varName, StringBuilder sb) {
            if (varName != null) {
                sb.append(varName).append(" = ").append(expression).append(";\n");
            } else {
                sb.append(expression);
            }
            return true;
        }
    }

    @Test
    void annotationVisitorWrapperDelegatesAndHandlesNullVisitor() {
        Parser parser = new Parser();

        Parser.AnnotationVisitorWrapper wrapperWithNull = new Parser.AnnotationVisitorWrapper(null);
        assertNull(wrapperWithNull.visitArray("values"));
        assertNull(wrapperWithNull.visitAnnotation("name", "LExample;"));
        assertDoesNotThrow(() -> wrapperWithNull.visit("flag", true));
        assertDoesNotThrow(() -> wrapperWithNull.visitEnum("choice", "LExample;", "VALUE"));

        AtomicBoolean delegated = new AtomicBoolean(false);
        AnnotationVisitor delegate = new AnnotationVisitor(Opcodes.ASM5) {
            @Override
            public AnnotationVisitor visitArray(String name) {
                delegated.set(true);
                return this;
            }
        };

        Parser.AnnotationVisitorWrapper wrapperWithDelegate = new Parser.AnnotationVisitorWrapper(delegate);
        AnnotationVisitor result = wrapperWithDelegate.visitArray("values");
        assertSame(delegate, result);
        assertTrue(delegated.get(), "AnnotationVisitorWrapper should forward to the underlying visitor");
    }

    @Test
    void disableDebugInfoFlagSkipsLineNumberEmission() {
        BytecodeMethod method = new BytecodeMethod("Example", Opcodes.ACC_STATIC, "sample", "()V", null, null);
        method.setDisableDebugInfo(true);

        Instruction.setHasInstructions(true);
        LineNumber lineNumber = new LineNumber("Example.java", 42);
        lineNumber.setMethod(method);

        StringBuilder generated = new StringBuilder();
        lineNumber.appendInstruction(generated);
        assertEquals("", generated.toString(), "Disabled debug info should suppress __CN1_DEBUG_INFO emission");
    }

    @Test
    void disableNullAndArrayBoundsChecksSkipsArrayCheckEmission() {
        BytecodeMethod method = new BytecodeMethod("Example", Opcodes.ACC_STATIC, "sample", "()V", null, null);
        method.setDisableNullAndArrayBoundsChecks(true);

        BasicInstruction instruction = new BasicInstruction(Opcodes.IALOAD, 0);
        instruction.setMethod(method);

        StringBuilder generated = new StringBuilder();
        instruction.appendInstruction(generated);
        assertFalse(generated.toString().contains("CHECK_ARRAY_ACCESS"),
                "Disabled null and array bounds checks should suppress CHECK_ARRAY_ACCESS emission");
    }

    @Test
    void disableDebugInfoSkipsLocalVariableMetadataAndVolatileLocals() {
        BytecodeMethod method = new BytecodeMethod("Example", Opcodes.ACC_STATIC, "sample", "()V", null, null);
        method.setDisableDebugInfo(true);
        method.addLocalVariable("counter", "I", null, new Label(), new Label(), 1);
        method.addDebugInfo(100);
        method.addInstruction(Opcodes.RETURN);
        method.setMaxes(1, 2);

        StringBuilder generated = new StringBuilder();
        method.appendMethodC(generated);
        String c = generated.toString();
        assertFalse(c.contains("volatile JAVA_INT ilocals_1_"),
                "Debug-disabled methods should not emit volatile locals from local variable metadata");
        assertFalse(c.contains("__CN1_DEBUG_INFO("),
                "Debug-disabled methods should not emit line debug information");
    }

    @Test
    void noThrowNoMonitorNoTryMethodsUseFastMethodStackMacro() {
        BytecodeMethod method = new BytecodeMethod("Example", Opcodes.ACC_STATIC, "sample", "()V", null, null);
        method.addInstruction(Opcodes.ICONST_0);
        method.addInstruction(Opcodes.POP);
        method.addInstruction(Opcodes.RETURN);
        method.setMaxes(1, 4);

        StringBuilder generated = new StringBuilder();
        method.appendMethodC(generated);
        String c = generated.toString();
        assertTrue(c.contains("DEFINE_METHOD_STACK_FAST_PRIMITIVE("),
                "Primitive no-throw/no-monitor/no-try methods should use DEFINE_METHOD_STACK_FAST_PRIMITIVE");
        assertTrue(c.contains("if (!class__Example.initialized) __STATIC_INITIALIZER_Example(threadStateData);"),
                "Static methods should emit a fast-path loaded check before static initialization");
        assertTrue(c.contains("CN1_FAST_RETURN_RELEASE();"),
                "Fast stack methods should use inline fast return release");
    }

    @Test
    void concatenatingFileOutputStreamWritesShardedOutputs() throws Exception {
        Path outputDir = Files.createTempDirectory("concatenating-output");
        ByteCodeTranslator.OutputType original = ByteCodeTranslator.output;
        ByteCodeTranslator.output = ByteCodeTranslator.OutputType.OUTPUT_TYPE_CLEAN;
        try {
            ConcatenatingFileOutputStream stream = new ConcatenatingFileOutputStream(outputDir.toFile());
            stream.beginNextFile("first");
            stream.write("abc".getBytes(StandardCharsets.UTF_8));
            stream.close();

            stream.beginNextFile("second");
            stream.write("123".getBytes(StandardCharsets.UTF_8));
            stream.close();

            Field destField = ConcatenatingFileOutputStream.class.getDeclaredField("dest");
            destField.setAccessible(true);
            ByteArrayOutputStream[] buffers = (ByteArrayOutputStream[]) destField.get(stream);
            for (int i = 0; i < buffers.length; i++) {
                if (buffers[i] == null) {
                    buffers[i] = new ByteArrayOutputStream();
                }
            }
            destField.set(stream, buffers);

            stream.realClose();

            Path first = outputDir.resolve("concatenated_" + Math.abs("first".hashCode() % ConcatenatingFileOutputStream.MODULO) + ".c");
            Path second = outputDir.resolve("concatenated_" + Math.abs("second".hashCode() % ConcatenatingFileOutputStream.MODULO) + ".c");

            assertTrue(Files.exists(first));
            assertEquals("abc\n", new String(Files.readAllBytes(first), StandardCharsets.UTF_8));

            assertTrue(Files.exists(second));
            assertEquals("123\n", new String(Files.readAllBytes(second), StandardCharsets.UTF_8));
        } finally {
            ByteCodeTranslator.output = original;
        }
    }

    @Test
    void multiArrayAddsDependenciesAndRegistersArrayTypes() throws Exception {
        List<String> dependencies = new ArrayList<>();
        MultiArray multiArray = new MultiArray("[[Ljava/lang/String;", 2);

        Set<String> snapshot = snapshotArrayTypes();
        try {
            multiArray.addDependencies(dependencies);

            assertTrue(dependencies.contains("java_lang_String"));
            assertTrue(snapshotArrayTypes().contains("2_java_lang_String"));
        } finally {
            restoreArrayTypes(snapshot);
        }
    }

    /**
     * Regression test for the same class of bug as issue #3108, applied to
     * MultiArray. The translator used to emit
     *   alloc3DArray(td, POP_INT(), POP_INT(), POP_INT())
     * and the 4D equivalent. C does not specify function-argument evaluation
     * order, so clang on iOS would evaluate the POPs in reverse and silently
     * swap the array dimensions -- "new int[a][b][c]" could be allocated as if
     * it were "new int[c][b][a]".
     *
     * Asserts the emitted C reads each dimension with PEEK semantics
     * ((*SP).data.i, (*(SP+1)).data.i, ...) after a single SP decrement, so
     * the dimensions land in the right alloc?DArray slots regardless of how
     * the C compiler reorders the function-argument evaluation.
     */
    @Test
    void multiArrayEmissionIsArgumentOrderSafe() throws Exception {
        Set<String> snapshot = snapshotArrayTypes();
        try {
            // 3D, fully-specified ("new int[a][b][c]").
            MultiArray ma3 = new MultiArray("[[[I", 3);
            List<String> deps3 = new ArrayList<>();
            ma3.addDependencies(deps3);
            StringBuilder out3 = new StringBuilder();
            ma3.appendInstruction(out3, new ArrayList<>());
            String c3 = out3.toString();
            assertTrue(c3.contains("SP -= 3"),
                    "3D MULTIANEWARRAY should decrement SP once for all dims:\n" + c3);
            assertTrue(c3.contains("alloc3DArray(threadStateData, (*(SP+2)).data.i, (*(SP+1)).data.i, (*SP).data.i"),
                    "3D dims=3 must read dims with PEEK in (innermost, middle, outermost) order:\n" + c3);
            assertFalse(c3.contains("POP_INT(), POP_INT()"),
                    "3D MULTIANEWARRAY must not chain multiple POP_INT calls:\n" + c3);

            // 4D, fully-specified ("new int[a][b][c][d]").
            MultiArray ma4 = new MultiArray("[[[[I", 4);
            List<String> deps4 = new ArrayList<>();
            ma4.addDependencies(deps4);
            StringBuilder out4 = new StringBuilder();
            ma4.appendInstruction(out4, new ArrayList<>());
            String c4 = out4.toString();
            assertTrue(c4.contains("SP -= 4"),
                    "4D MULTIANEWARRAY should decrement SP once for all dims:\n" + c4);
            assertTrue(c4.contains("alloc4DArray(threadStateData, (*(SP+3)).data.i, (*(SP+2)).data.i, (*(SP+1)).data.i, (*SP).data.i"),
                    "4D dims=4 must read dims with PEEK in innermost-to-outermost order:\n" + c4);
            assertFalse(c4.contains("POP_INT(), POP_INT()"),
                    "4D MULTIANEWARRAY must not chain multiple POP_INT calls:\n" + c4);

            // 2D was already safe; verify the existing pattern is intact.
            MultiArray ma2 = new MultiArray("[[Ljava/lang/String;", 2);
            List<String> deps2 = new ArrayList<>();
            ma2.addDependencies(deps2);
            StringBuilder out2 = new StringBuilder();
            ma2.appendInstruction(out2, new ArrayList<>());
            String c2 = out2.toString();
            assertTrue(c2.contains("SP -= 2") &&
                            c2.contains("(*(SP+1)).data.i, (*SP).data.i"),
                    "2D MULTIANEWARRAY must keep the existing PEEK pattern:\n" + c2);
            assertFalse(c2.contains("POP_INT(), POP_INT()"),
                    "2D MULTIANEWARRAY must not chain multiple POP_INT calls:\n" + c2);
        } finally {
            restoreArrayTypes(snapshot);
        }
    }

    @Test
    void arrayLengthExpressionReducesAndAssigns() throws Exception {
        List<Instruction> instructions = new ArrayList<>();
        StubAssignableExpression arrayRef = new StubAssignableExpression(Opcodes.ALOAD, "myArray");
        Instruction arrayLength = new Instruction(Opcodes.ARRAYLENGTH) { };
        instructions.add(arrayRef);
        instructions.add(arrayLength);

        int reducedIndex = ArrayLengthExpression.tryReduce(instructions, 1);
        assertEquals(0, reducedIndex);
        assertEquals(1, instructions.size());
        ArrayLengthExpression reduced = (ArrayLengthExpression) instructions.get(0);

        StringBuilder assignment = new StringBuilder();
        assertTrue(reduced.assignTo("len", assignment));
        assertEquals("len = CN1_ARRAY_LENGTH(myArray);\n", assignment.toString());

        List<String> deps = new ArrayList<>();
        reduced.addDependencies(deps);
        assertEquals(1, arrayRef.dependencyCalls);
        assertEquals("myArray", deps.get(0));
    }

    @Test
    void arrayLoadExpressionReducesAndAssigns() {
        List<Instruction> instructions = new ArrayList<>();
        StubAssignableExpression arrayRef = new StubAssignableExpression(Opcodes.ALOAD, "items");
        StubAssignableExpression index = new StubAssignableExpression(Opcodes.ILOAD, "index");
        Instruction loadInstr = new Instruction(Opcodes.IALOAD) { };
        instructions.add(arrayRef);
        instructions.add(index);
        instructions.add(loadInstr);

        int reducedIndex = ArrayLoadExpression.tryReduce(instructions, 2);
        assertEquals(0, reducedIndex);
        assertEquals(1, instructions.size());
        ArrayLoadExpression reduced = (ArrayLoadExpression) instructions.get(0);

        StringBuilder assignment = new StringBuilder();
        assertTrue(reduced.assignTo("value", assignment));
        assertEquals("value=CN1_ARRAY_ELEMENT_INT(items, index);\n", assignment.toString());

        List<String> deps = new ArrayList<>();
        reduced.addDependencies(deps);
        assertEquals(1, arrayRef.dependencyCalls);
        assertEquals(1, index.dependencyCalls);
        assertTrue(deps.contains("items"));
        assertTrue(deps.contains("index"));
    }

    private Path findGeneratedSource(Path srcRoot) throws Exception {
        return findGeneratedSource(srcRoot, "BytecodeInstructionApp");
    }

    private Path findGeneratedSource(Path srcRoot, String classPrefix) throws Exception {
        try (Stream<Path> paths = Files.walk(srcRoot)) {
            return paths
                    .filter(p -> p.getFileName().toString().startsWith(classPrefix) && p.getFileName().toString().endsWith(".c"))
                    .sorted((a, b) -> Integer.compare(a.getFileName().toString().length(), b.getFileName().toString().length()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Translated source for BytecodeInstructionApp not found"));
        }
    }


    private String appSource() {
        return "public class BytecodeInstructionApp {\n" +
                "    private static final int STATIC_INCREMENT = 3;\n" +
                "    private static native void report(int value);\n" +
                "    private int instanceCounter = 7;\n" +
                "    private int baseField = 11;\n" +
                "    public BytecodeInstructionApp(int seed) {\n" +
                "        instanceCounter = seed;\n" +
                "        baseField = seed + 6;\n" +
                "    }\n" +
                "    private static int optimizedComputation(int a, int b) {\n" +
                "        int counter = a;\n" +
                "        counter++;\n" +
                "        int branchBase = counter;\n" +
                "        int min;\n" +
                "        if (branchBase < b) {\n" +
                "            min = branchBase;\n" +
                "        } else {\n" +
                "            min = b + 2;\n" +
                "        }\n" +
                "        int result = min + counter;\n" +
                "        return result;\n" +
                "    }\n" +
                "    private static int switchComputation(int value) {\n" +
                "        switch (value) {\n" +
                "            case 4:\n" +
                "                return value + 10;\n" +
                "            case 10:\n" +
                "                return value + 5;\n" +
                "            default:\n" +
                "                return value - 1;\n" +
                "        }\n" +
                "    }\n" +
                "    private static int synchronizedIncrement(int base) {\n" +
                "        Object lock = new Object();\n" +
                "        int result = base;\n" +
                "        synchronized (lock) {\n" +
                "            result++;\n" +
                "        }\n" +
                "        return result;\n" +
                "    }\n" +
                // New synchronized method
                "    private synchronized int synchronizedMethod(int a) {\n" +
                "        return a + 1;\n" +
                "    }\n" +
                // New arithmetic tests
                "    private int arithmeticOps(int i, long l, float f, double d) {\n" +
                "        int ires = (i & 0xFF) | (0x0F ^ 0xAA);\n" +
                "        long lres = (l & 0xFFL) | (0x0FL ^ 0xAAL);\n" +
                "        ires = ires << 1 >> 1 >>> 1;\n" +
                "        lres = lres << 1 >> 1 >>> 1;\n" +
                "        int irem = i % 3;\n" +
                "        long lrem = l % 3;\n" +
                "        float frem = f % 2.0f;\n" +
                "        double drem = d % 2.0;\n" +
                "        byte b = (byte)i;\n" +
                "        char c = (char)i;\n" +
                "        short s = (short)i;\n" +
                "        float f2 = (float)i;\n" +
                "        double d2 = (double)i;\n" +
                "        long l2 = (long)i;\n" +
                "        int i2 = (int)l;\n" +
                "        float f3 = (float)l;\n" +
                "        double d3 = (double)l;\n" +
                "        int i3 = (int)f;\n" +
                "        long l3 = (long)f;\n" +
                "        double d4 = (double)f;\n" +
                "        int i4 = (int)d;\n" +
                "        long l4 = (long)d;\n" +
                "        float f4 = (float)d;\n" +
                "        i = -i;\n" +
                "        l = -l;\n" +
                "        f = -f;\n" +
                "        d = -d;\n" +
                "        return (int)(ires + lres + irem + lrem + frem + drem + b + c + s + f2 + d2 + l2 + i2 + f3 + d3 + i3 + l3 + d4 + i4 + l4 + f4);\n" +
                "    }\n" +
                // New primitive arrays
                "    private int primitiveArrays(int val) {\n" +
                "        boolean[] b = new boolean[1]; b[0] = true;\n" +
                "        char[] c = new char[1]; c[0] = 'a';\n" +
                "        float[] f = new float[1]; f[0] = 1.0f;\n" +
                "        double[] d = new double[1]; d[0] = 1.0;\n" +
                "        byte[] by = new byte[1]; by[0] = 1;\n" +
                "        short[] s = new short[1]; s[0] = 1;\n" +
                "        int[] i = new int[1]; i[0] = val;\n" +
                "        long[] l = new long[1]; l[0] = 1;\n" +
                "        return i[0];\n" +
                "    }\n" +
                "    private int loopArrays(int base) {\n" +
                "        int[] values = { base, base + 1, base + 2, STATIC_INCREMENT };\n" +
                "        int total = 0;\n" +
                "        for (int i = 0; i < values.length; i++) {\n" +
                "            total += values[i] * (i + 1);\n" +
                "        }\n" +
                "        return total + values.length;\n" +
                "    }\n" +
                "    private int multiArrayUsage(int factor) {\n" +
                "        int[][] grid = new int[2][3];\n" +
                "        int v = factor;\n" +
                "        for (int i = 0; i < grid.length; i++) {\n" +
                "            for (int j = 0; j < grid[i].length; j++) {\n" +
                "                grid[i][j] = v++;\n" +
                "            }\n" +
                "        }\n" +
                "        int total = 0;\n" +
                "        for (int[] row : grid) {\n" +
                "            for (int cell : row) {\n" +
                "                total += cell;\n" +
                "            }\n" +
                "            total += row.length;\n" +
                "        }\n" +
                "        return total;\n" +
                "    }\n" +
                "    private int useFieldsAndMethods(int offset) {\n" +
                "        instanceCounter += offset;\n" +
                "        int[] mix = new int[] { offset, offset + baseField, instanceCounter };\n" +
                "        int altSum = 0;\n" +
                "        for (int i = 0; i < mix.length; i++) {\n" +
                "            altSum += mix[i] + i;\n" +
                "        }\n" +
                "        int nestedLoop = loopArrays(offset);\n" +
                "        int nestedMulti = multiArrayUsage(offset);\n" +
                "        report(instanceCounter);\n" +
                "        report(baseField);\n" +
                "        report(altSum);\n" +
                "        report(nestedLoop);\n" +
                "        report(nestedMulti);\n" +
                "        return nestedLoop + nestedMulti + instanceCounter + altSum + baseField;\n" +
                "    }\n" +
                "    public static void main(String[] args) {\n" +
                "        BytecodeInstructionApp app = new BytecodeInstructionApp(4);\n" +
                "        int first = optimizedComputation(1, 3);\n" +
                "        int second = optimizedComputation(5, 2);\n" +
                "        int switched = switchComputation(first) + switchComputation(second);\n" +
                "        int synchronizedValue = synchronizedIncrement(second);\n" +
                "        int syncMethodVal = app.synchronizedMethod(10);\n" +
                "        int arrays = app.loopArrays(2);\n" +
                "        int multi = app.multiArrayUsage(3);\n" +
                "        int arraysFive = app.loopArrays(5);\n" +
                "        int multiFive = app.multiArrayUsage(5);\n" +
                "        int fieldCalls = app.useFieldsAndMethods(5);\n" +
                "        int arithmetic = app.arithmeticOps(1, 1L, 1.0f, 1.0);\n" +
                "        int primitives = app.primitiveArrays(5);\n" +
                "        report(first);\n" +
                "        report(second);\n" +
                "        report(switched);\n" +
                "        report(synchronizedValue);\n" +
                "        report(syncMethodVal);\n" +
                "        report(arrays);\n" +
                "        report(multi);\n" +
                "        report(arraysFive);\n" +
                "        report(multiFive);\n" +
                "        report(fieldCalls);\n" +
                "        report(arithmetic);\n" +
                "        report(primitives);\n" +
                "        report(first + second + switched + synchronizedValue + arrays + multi + fieldCalls + syncMethodVal + arithmetic + primitives);\n" +
                "    }\n" +
                "}\n";
    }

    private String stringConcatInvokeDynamicAppSource() {
        return "public class StringConcatInvokeDynamicApp {\n" +
                "    private static native void report(int value);\n" +
                "    private static String format(String name, int count) {\n" +
                "        return \"user=\" + name + \";count=\" + count + \";ok\";\n" +
                "    }\n" +
                "    public static void main(String[] args) {\n" +
                "        String expected = \"user=alice;count=7;ok\";\n" +
                "        String actual = format(\"alice\", 7);\n" +
                "        report(expected.equals(actual) ? 1 : -1);\n" +
                "    }\n" +
                "}\n";
    }

    private String javaLangDoubleHeader() {
        return "#include \"cn1_globals.h\"\n" +
                "#include <limits.h>\n" +
                "JAVA_BOOLEAN java_lang_Double_isInfinite___double_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE value);\n";
    }

    private String javaLangDoubleSource() {
        return "#include \"java_lang_Double.h\"\n" +
                "JAVA_BOOLEAN java_lang_Double_isInfinite___double_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE value) {\n" +
                "    (void)threadStateData;\n" +
                "    return value == (JAVA_DOUBLE)(1.0 / 0.0) || value == (JAVA_DOUBLE)(-1.0 / 0.0);\n" +
                "}\n";
    }

    private String javaUtilArrayListHeader() {
        return "#include \"cn1_globals.h\"\n" +
                "extern struct clazz class__java_util_ArrayList;\n";
    }

    private String javaUtilArrayListSource() {
        return "#include \"java_util_ArrayList.h\"\n" +
                "struct clazz class__java_util_ArrayList = {0};\n";
    }

    private String nativeReportSource() {
        return "#include \"cn1_globals.h\"\n" +
                "#include <stdio.h>\n" +
                "void BytecodeInstructionApp_report___int(CODENAME_ONE_THREAD_STATE, JAVA_INT value) {\n" +
                "    printf(\"RESULT=%d\\n\", value);\n" +
                "}\n" +
                "void InvokeLdcLocalVarsApp_report___int(CODENAME_ONE_THREAD_STATE, JAVA_INT value) {\n" +
                "    printf(\"RESULT=%d\\n\", value);\n" +
                "}\n" +
                "void StringConcatInvokeDynamicApp_report___int(CODENAME_ONE_THREAD_STATE, JAVA_INT value) {\n" +
                "    printf(\"RESULT=%d\\n\", value);\n" +
                "}\n";
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("provideCompilerConfigs")
    void handleDefaultOutputWritesOutput(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();
        resetByteCodeClass();
        Path sourceDir = Files.createTempDirectory("default-output-source");
        Path outputDir = Files.createTempDirectory("default-output-dest");

        Files.write(sourceDir.resolve("resource.txt"), "data".getBytes(StandardCharsets.UTF_8));
        compileDummyMainClass(sourceDir, "com.example", "MyAppDefault", config);

        String[] args = new String[] {
                // Unrecognized output type routes to the plain copy-through default handler
                "unknown",
                sourceDir.toAbsolutePath().toString(),
                outputDir.toAbsolutePath().toString(),
                "MyAppDefault", "com.example", "My App", "1.0", "ios", "none"
        };

        ByteCodeTranslator.OutputType originalOutput = ByteCodeTranslator.output;
        try {
            ByteCodeTranslator.main(args);

            assertTrue(Files.exists(outputDir.resolve("resource.txt")), "Default output handler should copy resources");
        } finally {
            ByteCodeTranslator.output = originalOutput;
            Parser.cleanup();
            resetByteCodeClass();
        }
    }

    @Test
    void readFileAsStringBuilderReadsContent() throws Exception {
        File temp = File.createTempFile("readfile", ".txt");
        Files.write(temp.toPath(), "Hello World".getBytes(StandardCharsets.UTF_8));

        Method m = ByteCodeTranslator.class.getDeclaredMethod("readFileAsStringBuilder", File.class);
        m.setAccessible(true);
        StringBuilder sb = (StringBuilder) m.invoke(null, temp);

        assertEquals("Hello World", sb.toString());
        temp.delete();
    }

    @Test
    void replaceInFileModifiesContent() throws Exception {
        File temp = File.createTempFile("replace", ".txt");
        Files.write(temp.toPath(), "Hello World".getBytes(StandardCharsets.UTF_8));

        Method m = ByteCodeTranslator.class.getDeclaredMethod("replaceInFile", File.class, String[].class);
        m.setAccessible(true);
        m.invoke(null, temp, new String[]{"World", "Universe"});

        String content = new String(Files.readAllBytes(temp.toPath()), StandardCharsets.UTF_8);
        assertEquals("Hello Universe", content);
        temp.delete();
    }

    @Test
    void getFileTypeReturnsCorrectTypes() throws Exception {
        Method m = ByteCodeTranslator.class.getDeclaredMethod("getFileType", String.class);
        m.setAccessible(true);

        assertEquals("wrapper.framework", m.invoke(null, "foo.framework"));
        assertEquals("sourcecode.c.objc", m.invoke(null, "foo.m"));
        assertEquals("file", m.invoke(null, "foo.txt"));
        assertEquals("wrapper.plug-in", m.invoke(null, "foo.bundle"));
    }

    @Test
    void copyDirRecursivelyCopies() throws Exception {
        Path src = Files.createTempDirectory("copydir-src");
        Path dest = Files.createTempDirectory("copydir-dest");

        Files.createDirectories(src.resolve("subdir"));
        Files.write(src.resolve("file1.txt"), "content1".getBytes());
        Files.write(src.resolve("subdir/file2.txt"), "content2".getBytes());

        Method m = ByteCodeTranslator.class.getDeclaredMethod("copyDir", File.class, File.class);
        m.setAccessible(true);
        ByteCodeTranslator instance = new ByteCodeTranslator();
        m.invoke(instance, src.toFile(), dest.toFile());

        Path copiedRoot = dest.resolve(src.getFileName());
        assertTrue(Files.exists(copiedRoot));
        assertTrue(Files.exists(copiedRoot.resolve("file1.txt")));
        assertTrue(Files.exists(copiedRoot.resolve("subdir/file2.txt")));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("provideCompilerConfigs")
    void handleIosOutputGeneratesProjectStructure(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();
        resetByteCodeClass();
        Path sourceDir = Files.createTempDirectory("ios-output-source");
        Path outputDir = Files.createTempDirectory("ios-output-dest");

        Files.write(sourceDir.resolve("resource.txt"), "data".getBytes(StandardCharsets.UTF_8));
        compileDummyMainClass(sourceDir, "com.example", "MyAppIOS", config);

        // Add a bundle to test copyDir invocation in execute loop
        Path bundleDir = sourceDir.resolve("test.bundle");
        Files.createDirectories(bundleDir);
        Files.write(bundleDir.resolve("info.txt"), "bundle info".getBytes(StandardCharsets.UTF_8));

        String[] args = new String[] {
                "ios",
                sourceDir.toAbsolutePath().toString(),
                outputDir.toAbsolutePath().toString(),
                "MyAppIOS", "com.example", "My App", "1.0", "ios", "none"
        };

        ByteCodeTranslator.OutputType originalOutput = ByteCodeTranslator.output;
        try {
            ByteCodeTranslator.main(args);

            Path dist = outputDir.resolve("dist");
            assertTrue(Files.exists(dist));
            Path srcRoot = dist.resolve("MyAppIOS-src");
            assertTrue(Files.exists(srcRoot));
            assertTrue(Files.exists(srcRoot.resolve("resource.txt")));
            assertTrue(Files.exists(srcRoot.resolve("Images.xcassets")));
            assertTrue(Files.exists(dist.resolve("MyAppIOS.xcodeproj")));
            assertTrue(Files.exists(srcRoot.resolve("MyAppIOS-Info.plist")));

            // Verify bundle copied
            assertTrue(Files.exists(srcRoot.resolve("test.bundle")));
            assertTrue(Files.exists(srcRoot.resolve("test.bundle/info.txt")));

        } finally {
            ByteCodeTranslator.output = originalOutput;
            Parser.cleanup();
            resetByteCodeClass();
        }
    }

    private void resetByteCodeClass() throws Exception {
        Field mainClassField = ByteCodeClass.class.getDeclaredField("mainClass");
        mainClassField.setAccessible(true);
        mainClassField.set(null, null);

        Field arrayTypesField = ByteCodeClass.class.getDeclaredField("arrayTypes");
        arrayTypesField.setAccessible(true);
        ((Set<?>) arrayTypesField.get(null)).clear();

        Field writableFieldsField = ByteCodeClass.class.getDeclaredField("writableFields");
        writableFieldsField.setAccessible(true);
        ((Set<?>) writableFieldsField.get(null)).clear();
    }

    private void compileDummyMainClass(Path sourceDir, String packageName, String className, CompilerHelper.CompilerConfig config) throws Exception {
        Path packageDir = sourceDir;
        for (String part : packageName.split("\\.")) {
            packageDir = packageDir.resolve(part);
        }
        Files.createDirectories(packageDir);
        Path javaFile = packageDir.resolve(className + ".java");
        String content = "package " + packageName + ";\n" +
                "public class " + className + " {\n" +
                "    public static void main(String[] args) {}\n" +
                "}\n";
        Files.write(javaFile, content.getBytes(StandardCharsets.UTF_8));

        List<String> args = new ArrayList<>();
        args.add("-source");
        args.add(config.targetVersion);
        args.add("-target");
        args.add(config.targetVersion);
        args.add("-d");
        args.add(sourceDir.toString());
        args.add(javaFile.toString());

        int result = CompilerHelper.compile(config.jdkHome, args);
        assertEquals(0, result, "Compilation failed with " + config);
    }

    @Test
    void testLocalVariableCoverage() {
        Label start = new Label();
        Label end = new Label();
        LocalVariable lv = new LocalVariable("myVar", "I", null, start, end, 1);

        assertEquals(1, lv.getIndex());
        assertTrue(lv.isRightVariable(1, 'I'));
        assertFalse(lv.isRightVariable(2, 'I'));
        assertFalse(lv.isRightVariable(1, 'F')); // Mismatched type char

        // Test L type
        LocalVariable objVar = new LocalVariable("myObj", "Ljava/lang/String;", null, start, end, 2);
        assertTrue(objVar.isRightVariable(2, 'L'));

        LocalVariable arrayVar = new LocalVariable("myArr", "[I", null, start, end, 3);
        assertTrue(arrayVar.isRightVariable(3, 'L'));

        assertEquals("imyVar_1", lv.getVarName());

        LocalVariable thisVar = new LocalVariable("this", "LMyClass;", null, start, end, 0);
        assertEquals("__cn1ThisObject", thisVar.getVarName());

        StringBuilder sb = new StringBuilder();
        lv.appendInstruction(sb);
        String code = sb.toString();
        assertTrue(code.contains("JAVA_INT i"));
        assertTrue(code.contains("myVar_1"));

        sb = new StringBuilder();
        thisVar.appendInstruction(sb);
        assertTrue(sb.toString().contains("this = __cn1ThisObject"));
    }

    @Test
    void testBasicInstructionCoverage() {
        BasicInstruction bi = new BasicInstruction(Opcodes.ICONST_5, 5);
        assertEquals(5, bi.getValue());
        assertFalse(bi.isComplexInstruction());

        BasicInstruction throwInstr = new BasicInstruction(Opcodes.ATHROW, 0);
        assertTrue(throwInstr.isComplexInstruction());

        // Test appendSynchronized via appendInstruction with RETURN
        try {
            BasicInstruction.setSynchronizedMethod(true, false, "MyClass");
            BasicInstruction ret = new BasicInstruction(Opcodes.RETURN, 0);
            StringBuilder sb = new StringBuilder();
            ret.appendInstruction(sb, new ArrayList<>());
            String code = sb.toString();
            assertTrue(code.contains("monitorExitBlock"));
            assertTrue(code.contains("__cn1ThisObject"));

            // Static synchronized
            BasicInstruction.setSynchronizedMethod(true, true, "MyClass");
            sb = new StringBuilder();
            ret.appendInstruction(sb, new ArrayList<>());
            code = sb.toString();
            assertTrue(code.contains("monitorExitBlock"));
            assertTrue(code.contains("class__MyClass"));
        } finally {
            BasicInstruction.setSynchronizedMethod(false, false, null);
        }
    }

    /**
     * Regression test for issue #3108: a chained assignment with widening
     * conversion ("this.a = this.b = doubleExpr") emits dup2_x1 + two
     * putfield's. The translator's unfolded PUTFIELD path used to emit
     * "set_field_X(POP_DOUBLE(), POP_OBJ())" -- but C does not specify the
     * order of evaluation of function arguments, so clang on iOS was popping
     * the operands in the wrong order, causing the double's bits to be
     * interpreted as an object reference and crashing with NPE.
     *
     * This test asserts that the unfolded primitive PUTFIELD path uses PEEK
     * (which has no side effect on SP) followed by an explicit pop, mirroring
     * the fix that was already in place for the object PUTFIELD path.
     */
    @Test
    void putfieldUnfoldedPathUsesPeekNotPop() {
        // The unfolded path is taken when valueOp and targetOp are null,
        // i.e. when tryReduce could not fold the operands into literals.
        // Build a Field with PUTFIELD opcode for each primitive desc and
        // check the emitted C.
        String[][] cases = {
                {"D", "PEEK_DOUBLE(1)"},
                {"F", "PEEK_FLOAT(1)"},
                {"J", "PEEK_LONG(1)"},
                {"I", "PEEK_INT(1)"},
                {"Z", "PEEK_INT(1)"},
                {"B", "PEEK_INT(1)"},
                {"C", "PEEK_INT(1)"},
                {"S", "PEEK_INT(1)"},
        };
        for (String[] c : cases) {
            String desc = c[0];
            String expectedPeek = c[1];
            com.codename1.tools.translator.bytecodes.Field f =
                    new com.codename1.tools.translator.bytecodes.Field(
                            Opcodes.PUTFIELD, "MyClass", "myField", desc);
            StringBuilder out = new StringBuilder();
            f.appendInstruction(out);
            String c1 = out.toString();
            assertTrue(c1.contains(expectedPeek),
                    "PUTFIELD desc=" + desc + " should use " + expectedPeek
                            + " but emitted:\n" + c1);
            assertTrue(c1.contains("PEEK_OBJ(2)"),
                    "PUTFIELD desc=" + desc + " should read target with PEEK_OBJ(2):\n" + c1);
            // Explicit pop must follow (either SP -= 2 or POP_MANY).
            assertTrue(c1.contains("SP -= 2") || c1.contains("POP_MANY"),
                    "PUTFIELD desc=" + desc + " should pop the consumed slots explicitly:\n" + c1);
            // Most importantly: must NOT use POP_X() inside the call where
            // argument evaluation order would matter.
            assertFalse(c1.contains("POP_DOUBLE(), POP_OBJ()"),
                    "PUTFIELD double must not rely on C argument evaluation order:\n" + c1);
            assertFalse(c1.contains("POP_FLOAT(), POP_OBJ()"),
                    "PUTFIELD float must not rely on C argument evaluation order:\n" + c1);
            assertFalse(c1.contains("POP_LONG(), POP_OBJ()"),
                    "PUTFIELD long must not rely on C argument evaluation order:\n" + c1);
            assertFalse(c1.contains("POP_INT(), POP_OBJ()"),
                    "PUTFIELD int must not rely on C argument evaluation order:\n" + c1);
        }

        // Object PUTFIELD already used PEEK + POP_MANY; cover it too so a
        // future change can't silently regress it back to POP.
        com.codename1.tools.translator.bytecodes.Field objField =
                new com.codename1.tools.translator.bytecodes.Field(
                        Opcodes.PUTFIELD, "MyClass", "myField", "Ljava/lang/Object;");
        StringBuilder objOut = new StringBuilder();
        objField.appendInstruction(objOut);
        String objC = objOut.toString();
        assertTrue(objC.contains("PEEK_OBJ(1)") && objC.contains("PEEK_OBJ(2)"),
                "Object PUTFIELD must read both operands with PEEK:\n" + objC);
        assertFalse(objC.contains("POP_OBJ(), POP_OBJ()"),
                "Object PUTFIELD must not rely on C argument evaluation order:\n" + objC);
    }

    /**
     * Regression test for issue #3108 (second cause).
     *
     * The widening / narrowing conversion opcodes (I2D, I2L, F2D, F2L, L2D and
     * their inverses) used to write the new value into SP[-1].data but leave
     * the runtime type tag untouched. BC_DUP2_X1 / BC_DUP2_X2 / BC_DUP_X2
     * dispatch via IS_DOUBLE_WORD(...) on that tag, so e.g. PUSH_INT (tag=INT)
     * followed by I2D (data updated, tag still INT) followed by DUP2_X1 sent
     * the dup through the cat-1 branch and shifted SP by +2 instead of +1.
     * The chained assignment "a.x = b.x = (double) someInt" then read garbage
     * for the second putfield's operands and crashed with NPE on iOS.
     *
     * Each cat-changing conversion must rewrite SP[-1].type to the new
     * CN1_TYPE_*. Pure-arithmetic conversions (I2B/I2C/I2S, I2F/F2I) are not
     * involved in dup-dispatch but are checked for symmetry.
     */
    @Test
    void conversionOpcodesUpdateRuntimeTypeTag() {
        Object[][] cases = {
                {Opcodes.I2L, "I2L", "CN1_TYPE_LONG"},
                {Opcodes.I2D, "I2D", "CN1_TYPE_DOUBLE"},
                {Opcodes.I2F, "I2F", "CN1_TYPE_FLOAT"},
                {Opcodes.L2I, "L2I", "CN1_TYPE_INT"},
                {Opcodes.L2F, "L2F", "CN1_TYPE_FLOAT"},
                {Opcodes.L2D, "L2D", "CN1_TYPE_DOUBLE"},
                {Opcodes.F2I, "F2I", "CN1_TYPE_INT"},
                {Opcodes.F2L, "F2L", "CN1_TYPE_LONG"},
                {Opcodes.F2D, "F2D", "CN1_TYPE_DOUBLE"},
                {Opcodes.D2I, "D2I", "CN1_TYPE_INT"},
                {Opcodes.D2L, "D2L", "CN1_TYPE_LONG"},
                {Opcodes.D2F, "D2F", "CN1_TYPE_FLOAT"},
        };
        for (Object[] c : cases) {
            int opcode = (Integer) c[0];
            String name = (String) c[1];
            String expectedTypeTag = (String) c[2];
            BasicInstruction instr = new BasicInstruction(opcode, 0);
            StringBuilder out = new StringBuilder();
            instr.appendInstruction(out, new ArrayList<Instruction>());
            String emitted = out.toString();
            assertTrue(emitted.contains("SP[-1].type = " + expectedTypeTag),
                    name + " must rewrite SP[-1].type to " + expectedTypeTag
                            + " so BC_DUP2_X1 / BC_DUP2_X2 / BC_DUP_X2 dispatch correctly. Emitted:\n" + emitted);
        }
    }

    @Test
    void testArithmeticExpressionCoverage() {
        // Use tryReduce to construct an ArithmeticExpression since constructor is private
        List<Instruction> instructions = new ArrayList<>();
        instructions.add(new BasicInstruction(Opcodes.ICONST_1, 1));
        instructions.add(new BasicInstruction(Opcodes.ICONST_2, 2));
        instructions.add(new BasicInstruction(Opcodes.IADD, 0));

        int idx = ArithmeticExpression.tryReduce(instructions, 2);
        assertTrue(idx >= 0);
        Instruction result = instructions.get(idx);
        assertTrue(result instanceof ArithmeticExpression);
        ArithmeticExpression ae = (ArithmeticExpression) result;

        assertTrue(ae.isOptimized());

        assertEquals("(1 /* ICONST_1 */ + 2 /* ICONST_2 */)", ae.getExpressionAsString());

        // Test addDependencies
        List<String> deps = new ArrayList<>();
        ae.addDependencies(deps);
        // ICONSTs and IADD don't add dependencies
        assertTrue(deps.isEmpty());

        // Test with LDC that adds dependencies
        instructions.clear();
        Ldc ldc = new Ldc("someString");
        // ArithmeticExpression logic checks subexpressions.
        // We can't easily inject a dependency-heavy instruction into ArithmeticExpression via tryReduce
        // unless it is an operand.
        // But LDC is an operand.

        // Let's rely on integration tests for complex dependency checks in ArithmeticExpression,
        // or mock if possible. But here we can check basic behavior.
    }

}
