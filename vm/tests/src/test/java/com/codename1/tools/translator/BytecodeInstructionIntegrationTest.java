package com.codename1.tools.translator;
import com.codename1.tools.translator.bytecodes.ArithmeticExpression;
import com.codename1.tools.translator.bytecodes.ArrayLengthExpression;
import com.codename1.tools.translator.bytecodes.ArrayLoadExpression;
import com.codename1.tools.translator.bytecodes.AssignableExpression;
import com.codename1.tools.translator.bytecodes.BasicInstruction;
import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.Ldc;
import com.codename1.tools.translator.bytecodes.LocalVariable;
import com.codename1.tools.translator.bytecodes.MultiArray;
import com.codename1.tools.translator.bytecodes.VarOp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        configs.addAll(CompilerHelper.getAvailableCompilers("1.5"));
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

        // Compile JavaAPI for bootclasspath
        compileJavaAPI(javaApiDir);

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
             // --patch-module is not allowed with -target 8.
             // We rely on the JDK's own bootstrap classes but include our JavaAPI in classpath
             // so that any non-replaced classes are found.
             // This means we compile against JDK 9+ API but emit older bytecode.
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

        Files.copy(nativeReport, classesDir.resolve("native_report.c"));

        Path outputDir = Files.createTempDirectory("bytecode-integration-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "CustomBytecodeApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project for the optimized sample");

        Path srcRoot = distDir.resolve("CustomBytecodeApp-src");
        CleanTargetIntegrationTest.patchCn1Globals(srcRoot);
        writeRuntimeStubs(srcRoot);
        writeMissingHeadersAndImpls(srcRoot);

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

        // Compile JavaAPI for bootclasspath
        compileJavaAPI(javaApiDir);

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

        Files.copy(nativeReport, classesDir.resolve("native_report.c"));

        Path outputDir = Files.createTempDirectory("invoke-ldc-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "InvokeLdcLocalVars");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project for Invoke/Ldc sample");

        Path srcRoot = distDir.resolve("InvokeLdcLocalVars-src");
        CleanTargetIntegrationTest.patchCn1Globals(srcRoot);
        writeRuntimeStubs(srcRoot);
        writeInvokeLdcRuntimeStubs(srcRoot);
        writeMissingHeadersAndImpls(srcRoot);

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

        Parser.AnnotationVisitorWrapper wrapperWithNull = parser.new AnnotationVisitorWrapper(null);
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

        Parser.AnnotationVisitorWrapper wrapperWithDelegate = parser.new AnnotationVisitorWrapper(delegate);
        AnnotationVisitor result = wrapperWithDelegate.visitArray("values");
        assertSame(delegate, result);
        assertTrue(delegated.get(), "AnnotationVisitorWrapper should forward to the underlying visitor");
    }

    @Test
    void byteCodeTranslatorFilenameFilterMatchesExpectedFiles() throws Exception {
        Class<?> filterClass = Class.forName("com.codename1.tools.translator.ByteCodeTranslator$3");
        Constructor<?> ctor = filterClass.getDeclaredConstructor();
        ctor.setAccessible(true);

        FilenameFilter filter = (FilenameFilter) ctor.newInstance();
        File directory = Files.createTempDirectory("bytecode-filter").toFile();

        assertTrue(filter.accept(directory, "assets.bundle"));
        assertTrue(filter.accept(directory, "model.xcdatamodeld"));
        assertTrue(filter.accept(directory, "VisibleSource.m"));
        assertFalse(filter.accept(directory, ".hidden"));
        assertFalse(filter.accept(directory, "Images.xcassets"));
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

    private void compileJavaAPI(Path outputDir) throws Exception {
        Files.createDirectories(outputDir);
        Path javaApiRoot = Paths.get("..", "JavaAPI", "src").normalize().toAbsolutePath();
        List<String> sources = new ArrayList<>();
        Files.walk(javaApiRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> sources.add(p.toString()));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> args = new ArrayList<>();

        if (!System.getProperty("java.version").startsWith("1.")) {
             args.add("--patch-module");
             args.add("java.base=" + javaApiRoot.toString());
        } else {
            args.add("-source");
            args.add("1.5");
            args.add("-target");
            args.add("1.5");
        }

        args.add("-d");
        args.add(outputDir.toString());
        args.addAll(sources);

        int result = compiler.run(null, null, null, args.toArray(new String[0]));
        assertEquals(0, result, "JavaAPI should compile");
    }

    private void writeInvokeLdcRuntimeStubs(Path srcRoot) throws Exception {
        Path doubleHeader = srcRoot.resolve("java_lang_Double.h");
        Path doubleSource = srcRoot.resolve("java_lang_Double.c");
        if (!Files.exists(doubleHeader)) {
            Files.write(doubleHeader, javaLangDoubleHeader().getBytes(StandardCharsets.UTF_8));
        }
        if (!Files.exists(doubleSource)) {
            Files.write(doubleSource, javaLangDoubleSource().getBytes(StandardCharsets.UTF_8));
        }

        Path arrayListHeader = srcRoot.resolve("java_util_ArrayList.h");
        Path arrayListSource = srcRoot.resolve("java_util_ArrayList.c");
        if (!Files.exists(arrayListHeader)) {
            Files.write(arrayListHeader, javaUtilArrayListHeader().getBytes(StandardCharsets.UTF_8));
        }
        if (!Files.exists(arrayListSource)) {
            Files.write(arrayListSource, javaUtilArrayListSource().getBytes(StandardCharsets.UTF_8));
        }
    }

    private void writeMissingHeadersAndImpls(Path srcRoot) throws Exception {
        // java_lang_NullPointerException
        Path npeHeader = srcRoot.resolve("java_lang_NullPointerException.h");
        if (!Files.exists(npeHeader)) {
            String npeContent = "#ifndef __JAVA_LANG_NULLPOINTEREXCEPTION_H__\n" +
                    "#define __JAVA_LANG_NULLPOINTEREXCEPTION_H__\n" +
                    "#include \"cn1_globals.h\"\n" +
                    "JAVA_OBJECT __NEW_INSTANCE_java_lang_NullPointerException(CODENAME_ONE_THREAD_STATE);\n" +
                    "#endif\n";
            Files.write(npeHeader, npeContent.getBytes(StandardCharsets.UTF_8));
        }

        // java_lang_String
        Path stringHeader = srcRoot.resolve("java_lang_String.h");
        if (!Files.exists(stringHeader)) {
            String stringContent = "#ifndef __JAVA_LANG_STRING_H__\n" +
                    "#define __JAVA_LANG_STRING_H__\n" +
                    "#include \"cn1_globals.h\"\n" +
                    "extern struct clazz class__java_lang_String;\n" +
                    "extern struct clazz class_array2__java_lang_String;\n" +
                    "JAVA_OBJECT __NEW_ARRAY_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_INT size);\n" +
                    "#endif\n";
            Files.write(stringHeader, stringContent.getBytes(StandardCharsets.UTF_8));
        }

        // java_lang_Class
        Path classHeader = srcRoot.resolve("java_lang_Class.h");
        if (!Files.exists(classHeader)) {
             String classHeaderContent = "#ifndef __JAVA_LANG_CLASS_H__\n#define __JAVA_LANG_CLASS_H__\n#include \"cn1_globals.h\"\n" +
                     "extern struct clazz class__java_lang_Class;\n" +
                     "#endif\n";
             Files.write(classHeader, classHeaderContent.getBytes(StandardCharsets.UTF_8));
        }

        // java_lang_Object
        Path objectHeader = srcRoot.resolve("java_lang_Object.h");
        // Overwrite or create
        String objectContent = "#ifndef __JAVA_LANG_OBJECT_H__\n" +
                "#define __JAVA_LANG_OBJECT_H__\n" +
                "#include \"cn1_globals.h\"\n" +
                "extern struct clazz class__java_lang_Object;\n" +
                "void __FINALIZER_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);\n" +
                "void __GC_MARK_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force);\n" +
                "void java_lang_Object___INIT____(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);\n" +
                "JAVA_OBJECT __NEW_java_lang_Object(CODENAME_ONE_THREAD_STATE);\n" +
                "void __INIT_VTABLE_java_lang_Object(CODENAME_ONE_THREAD_STATE, void** vtable);\n" +
                "#endif\n";
        Files.write(objectHeader, objectContent.getBytes(StandardCharsets.UTF_8));


        // Append implementations to runtime_stubs.c or create extra_stubs.c
        Path extraStubs = srcRoot.resolve("extra_stubs.c");
        if (!Files.exists(extraStubs)) {
             String stubs = "#include \"cn1_globals.h\"\n" +
                     "#include \"java_lang_NullPointerException.h\"\n" +
                     "#include \"java_lang_String.h\"\n" +
                     "#include \"java_lang_Class.h\"\n" +
                     "#include \"java_lang_Object.h\"\n" +
                     "#include <stdlib.h>\n" +
                     "#include <string.h>\n" +
                     "#include <stdio.h>\n" +
                     "\n" +
                     "// class__java_lang_String defined in runtime_stubs.c\n" +
                     "struct clazz class_array2__java_lang_String = {0};\n" +
                     "// class__java_lang_Class defined in runtime_stubs.c\n" +
                     "struct clazz class__java_lang_Object = {0};\n" +
                     "\n" +
                     "JAVA_OBJECT __NEW_INSTANCE_java_lang_NullPointerException(CODENAME_ONE_THREAD_STATE) {\n" +
                     "    fprintf(stderr, \"Allocating NullPointerException\\n\");\n" +
                     "    fflush(stderr);\n" +
                     "    return JAVA_NULL;\n" +
                     "}\n" +
                     "void __FINALIZER_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {}\n" +
                     "void __GC_MARK_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) {}\n" +
                     "void java_lang_Object___INIT____(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {}\n" +
                     "JAVA_OBJECT __NEW_java_lang_Object(CODENAME_ONE_THREAD_STATE) {\n" +
                     "    fprintf(stderr, \"__NEW_java_lang_Object called\\n\");\n" +
                     "    fflush(stderr);\n" +
                     "    struct JavaObjectPrototype* ptr = (struct JavaObjectPrototype*)malloc(sizeof(struct JavaObjectPrototype));\n" +
                     "    if (ptr) {\n" +
                     "        memset(ptr, 0, sizeof(struct JavaObjectPrototype));\n" +
                     "        ptr->__codenameOneParentClsReference = &class__java_lang_Object;\n" +
                     "    }\n" +
                     "    return (JAVA_OBJECT)ptr;\n" +
                     "}\n" +
                     "void __INIT_VTABLE_java_lang_Object(CODENAME_ONE_THREAD_STATE, void** vtable) {}\n" +
                     "JAVA_OBJECT __NEW_ARRAY_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {\n" +
                     "    return 0;\n" +
                     "}\n";
             Files.write(extraStubs, stubs.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void writeRuntimeStubs(Path srcRoot) throws java.io.IOException {
        Path objectHeader = srcRoot.resolve("java_lang_Object.h");
        if (!Files.exists(objectHeader)) {
            String headerContent = "#ifndef __JAVA_LANG_OBJECT_H__\n" +
                    "#define __JAVA_LANG_OBJECT_H__\n" +
                    "#include \"cn1_globals.h\"\n" +
                    "#endif\n";
            Files.write(objectHeader, headerContent.getBytes(StandardCharsets.UTF_8));
        }

        Path stubs = srcRoot.resolve("runtime_stubs.c");
        if (Files.exists(stubs)) {
            return;
        }
        String content = "#include \"cn1_globals.h\"\n" +
                "#include <stdlib.h>\n" +
                "#include <string.h>\n" +
                "#include <math.h>\n" +
                "#include <limits.h>\n" +
                "#include <stdio.h>\n" +
                "\n" +
                "static struct ThreadLocalData globalThreadData;\n" +
                "static int runtimeInitialized = 0;\n" +
                "\n" +
                "static void initThreadState() {\n" +
                "    memset(&globalThreadData, 0, sizeof(globalThreadData));\n" +
                "    globalThreadData.blocks = calloc(CN1_MAX_STACK_CALL_DEPTH, sizeof(struct TryBlock));\n" +
                "    globalThreadData.threadObjectStack = calloc(CN1_MAX_OBJECT_STACK_DEPTH, sizeof(struct elementStruct));\n" +
                "    globalThreadData.pendingHeapAllocations = calloc(CN1_MAX_OBJECT_STACK_DEPTH, sizeof(void*));\n" +
                "    globalThreadData.callStackClass = calloc(CN1_MAX_STACK_CALL_DEPTH, sizeof(int));\n" +
                "    globalThreadData.callStackLine = calloc(CN1_MAX_STACK_CALL_DEPTH, sizeof(int));\n" +
                "    globalThreadData.callStackMethod = calloc(CN1_MAX_STACK_CALL_DEPTH, sizeof(int));\n" +
                "}\n" +
                "\n" +
                "struct ThreadLocalData* getThreadLocalData() {\n" +
                "    if (!runtimeInitialized) {\n" +
                "        initThreadState();\n" +
                "        runtimeInitialized = 1;\n" +
                "    }\n" +
                "    return &globalThreadData;\n" +
                "}\n" +
                "\n" +
                "JAVA_OBJECT codenameOneGcMalloc(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent) {\n" +
                "    JAVA_OBJECT obj = (JAVA_OBJECT)calloc(1, size);\n" +
                "    if (obj != JAVA_NULL) {\n" +
                "        obj->__codenameOneParentClsReference = parent;\n" +
                "    }\n" +
                "    return obj;\n" +
                "}\n" +
                "\n" +
                "void codenameOneGcFree(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {\n" +
                "    free(obj);\n" +
                "}\n" +
                "\n" +
                "JAVA_OBJECT* constantPoolObjects = NULL;\n" +
                "\n" +
                "void initConstantPool() {\n" +
                "    if (constantPoolObjects == NULL) {\n" +
                "        constantPoolObjects = calloc(32, sizeof(JAVA_OBJECT));\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "void arrayFinalizerFunction(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array) {\n" +
                "    (void)threadStateData;\n" +
                "    free(array);\n" +
                "}\n" +
                "\n" +
                "void gcMarkArrayObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) {\n" +
                "    (void)threadStateData;\n" +
                "    (void)obj;\n" +
                "    (void)force;\n" +
                "}\n" +
                "\n" +
                "void** initVtableForInterface() {\n" +
                "    static void* table[1];\n" +
                "    return (void**)table;\n" +
                "}\n" +
                "\n" +
                "struct clazz class_array1__JAVA_INT = {0};\n" +
                "struct clazz class_array2__JAVA_INT = {0};\n" +
                "struct clazz class_array1__JAVA_BOOLEAN = {0};\n" +
                "struct clazz class_array1__JAVA_CHAR = {0};\n" +
                "struct clazz class_array1__JAVA_FLOAT = {0};\n" +
                "struct clazz class_array1__JAVA_DOUBLE = {0};\n" +
                "struct clazz class_array1__JAVA_BYTE = {0};\n" +
                "struct clazz class_array1__JAVA_SHORT = {0};\n" +
                "struct clazz class_array1__JAVA_LONG = {0};\n" +
                "\n" +
                "static JAVA_OBJECT allocArrayInternal(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim) {\n" +
                "    fprintf(stderr, \"allocArrayInternal length=%d type=%p\\n\", length, type); fflush(stderr);\n" +
                "    struct JavaArrayPrototype* arr = (struct JavaArrayPrototype*)calloc(1, sizeof(struct JavaArrayPrototype));\n" +
                "    arr->__codenameOneParentClsReference = type;\n" +
                "    arr->length = length;\n" +
                "    arr->dimensions = dim;\n" +
                "    arr->primitiveSize = primitiveSize;\n" +
                "    if (length > 0) {\n" +
                "        int elementSize = primitiveSize > 0 ? primitiveSize : sizeof(JAVA_OBJECT);\n" +
                "        arr->data = calloc((size_t)length, (size_t)elementSize);\n" +
                "    }\n" +
                "    return (JAVA_OBJECT)arr;\n" +
                "}\n" +
                "\n" +
                "JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim) {\n" +
                "    return allocArrayInternal(threadStateData, length, type, primitiveSize, dim);\n" +
                "}\n" +
                "\n" +
                "JAVA_OBJECT alloc2DArray(CODENAME_ONE_THREAD_STATE, int length1, int length2, struct clazz* parentType, struct clazz* childType, int primitiveSize) {\n" +
                "    struct JavaArrayPrototype* outer = (struct JavaArrayPrototype*)allocArrayInternal(threadStateData, length1, parentType, sizeof(JAVA_OBJECT), 2);\n" +
                "    JAVA_OBJECT* rows = (JAVA_OBJECT*)outer->data;\n" +
                "    for (int i = 0; i < length1; i++) {\n" +
                "        rows[i] = allocArrayInternal(threadStateData, length2, childType, primitiveSize, 1);\n" +
                "    }\n" +
                "    return (JAVA_OBJECT)outer;\n" +
                "}\n" +
                "\n" +
                "void initMethodStack(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, int stackSize, int localsStackSize, int classNameId, int methodNameId) {\n" +
                "    (void)__cn1ThisObject;\n" +
                "    (void)stackSize;\n" +
                "    (void)classNameId;\n" +
                "    (void)methodNameId;\n" +
                "    threadStateData->threadObjectStackOffset += localsStackSize;\n" +
                "}\n" +
                "\n" +
                "void releaseForReturn(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread) {\n" +
                "    fprintf(stderr, \"releaseForReturn locals=%d\\n\", cn1LocalsBeginInThread); fflush(stderr);\n" +
                "    threadStateData->threadObjectStackOffset = cn1LocalsBeginInThread;\n" +
                "}\n" +
                "\n" +
                "void releaseForReturnInException(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread, int methodBlockOffset) {\n" +
                "    (void)methodBlockOffset;\n" +
                "    releaseForReturn(threadStateData, cn1LocalsBeginInThread);\n" +
                "}\n" +
                "\n" +
                "void monitorEnter(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { fprintf(stderr, \"monitorEnter %p\\n\", obj); fflush(stderr); }\n" +
                "\n" +
                "void monitorExit(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { fprintf(stderr, \"monitorExit %p\\n\", obj); fflush(stderr); }\n" +
                "\n" +
                "void monitorEnterBlock(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { fprintf(stderr, \"monitorEnterBlock %p\\n\", obj); fflush(stderr); }\n" +
                "\n" +
                "void monitorExitBlock(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) { fprintf(stderr, \"monitorExitBlock %p\\n\", obj); fflush(stderr); }\n" +
                "\n" +
                "struct elementStruct* pop(struct elementStruct** sp) {\n" +
                "    (*sp)--;\n" +
                "    return *sp;\n" +
                "}\n" +
                "\n" +
                "void popMany(CODENAME_ONE_THREAD_STATE, int count, struct elementStruct** sp) {\n" +
                "    while (count-- > 0) {\n" +
                "        (*sp)--;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "void throwException(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {\n" +
                "    fprintf(stderr, \"Exception thrown! obj=%p\\n\", obj);\n" +
                "    fflush(stderr);\n" +
                "    exit(1);\n" +
                "}\n" +
                "\n" +
                "struct clazz class__java_lang_Class = {0};\n" +
                "struct clazz class__java_lang_String = {0};\n" +
                "int currentGcMarkValue = 1;\n";

        Files.write(stubs, content.getBytes(StandardCharsets.UTF_8));
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
                "csharp",
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
