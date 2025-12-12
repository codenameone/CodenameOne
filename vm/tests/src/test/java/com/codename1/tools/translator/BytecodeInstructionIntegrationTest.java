package com.codename1.tools.translator;
import com.codename1.tools.translator.bytecodes.ArrayLengthExpression;
import com.codename1.tools.translator.bytecodes.ArrayLoadExpression;
import com.codename1.tools.translator.bytecodes.AssignableExpression;
import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.MultiArray;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
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

    @Test
    void translatesOptimizedBytecodeToLLVMExecutable() throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("bytecode-integration-sources");
        Path classesDir = Files.createTempDirectory("bytecode-integration-classes");

        Files.createDirectories(sourceDir.resolve("java/lang"));
        Files.write(sourceDir.resolve("BytecodeInstructionApp.java"), appSource().getBytes(StandardCharsets.UTF_8));
        writeJavaLangSources(sourceDir);

        Path nativeReport = sourceDir.resolve("native_report.c");
        Files.write(nativeReport, nativeReportSource().getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A JDK is required to compile test sources");

        List<String> compileArgs = new ArrayList<>(Arrays.asList(
                "-d", classesDir.toString(),
                sourceDir.resolve("BytecodeInstructionApp.java").toString()
        ));
        compileArgs.addAll(javaLangStubPaths(sourceDir));
        int compileResult = compiler.run(
                null,
                null,
                null,
                compileArgs.toArray(new String[0])
        );
        assertEquals(0, compileResult, "BytecodeInstructionApp should compile");

        Files.copy(nativeReport, classesDir.resolve("native_report.c"));

        Path outputDir = Files.createTempDirectory("bytecode-integration-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "CustomBytecodeApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project for the optimized sample");

        Path srcRoot = distDir.resolve("CustomBytecodeApp-src");
        CleanTargetIntegrationTest.patchCn1Globals(srcRoot);
        CleanTargetIntegrationTest.writeRuntimeStubs(srcRoot);

        Path generatedSource = findGeneratedSource(srcRoot);
        String generatedCode = new String(Files.readAllBytes(generatedSource), StandardCharsets.UTF_8);
        assertTrue(generatedCode.contains("CustomJump */"), "Optimized comparisons should emit CustomJump code");
        assertTrue(generatedCode.contains("BC_IINC"), "Increment operations should translate to BC_IINC macro");
        assertTrue(generatedCode.contains("VarOp.assignFrom"), "Optimized stores should rely on CustomIntruction output");
        assertTrue(generatedCode.contains("switch((*SP).data.i)"), "SwitchInstruction should emit a native switch statement");
        assertTrue(generatedCode.contains("BC_DUP(); /* DUP */"), "DupExpression should translate DUP operations to BC_DUP");

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
        assertTrue(output.contains("RESULT=293"), "Compiled program should print the expected arithmetic result");
    }

    private String invokeLdcLocalVarsAppSource() {
        return "public class InvokeLdcLocalVarsApp {\n" +
                "    private static native void report(int value);\n" +
                "    private interface Multiplier { int multiply(int a, int b); }\n" +
                "    private static class MultiplierImpl implements Multiplier {\n" +
                "        public int multiply(int a, int b) { return a * b; }\n" +
                "    }\n" +
                "    private final int seed;\n" +
                "    public InvokeLdcLocalVarsApp(int seed) {\n" +
                "        this.seed = seed;\n" +
                "    }\n" +
                "    private int constantsAndLocals(int extra) {\n" +
                "        int intVal = 21;\n" +
                "        long min = Long.MIN_VALUE;\n" +
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

    @Test
    void translatesInvokeAndLdcBytecodeToLLVMExecutable() throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("invoke-ldc-sources");
        Path classesDir = Files.createTempDirectory("invoke-ldc-classes");

        Files.createDirectories(sourceDir.resolve("java/lang"));
        Files.write(sourceDir.resolve("InvokeLdcLocalVarsApp.java"), invokeLdcLocalVarsAppSource().getBytes(StandardCharsets.UTF_8));
        writeJavaLangSources(sourceDir);

        Path nativeReport = sourceDir.resolve("native_report.c");
        Files.write(nativeReport, nativeReportSource().getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A JDK is required to compile test sources");

        List<String> compileArgs = new ArrayList<>(Arrays.asList(
                "-d", classesDir.toString(),
                sourceDir.resolve("InvokeLdcLocalVarsApp.java").toString()
        ));
        compileArgs.addAll(javaLangStubPaths(sourceDir));
        int compileResult = compiler.run(
                null,
                null,
                null,
                compileArgs.toArray(new String[0])
        );
        assertEquals(0, compileResult, "InvokeLdcLocalVarsApp should compile");

        Files.copy(nativeReport, classesDir.resolve("native_report.c"));

        Path outputDir = Files.createTempDirectory("invoke-ldc-output");
        CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "InvokeLdcLocalVars");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project for Invoke/Ldc sample");

        Path srcRoot = distDir.resolve("InvokeLdcLocalVars-src");
        CleanTargetIntegrationTest.patchCn1Globals(srcRoot);
        CleanTargetIntegrationTest.writeRuntimeStubs(srcRoot);
        writeInvokeLdcRuntimeStubs(srcRoot);

        Path generatedSource = findGeneratedSource(srcRoot, "InvokeLdcLocalVarsApp");
        String generatedCode = new String(Files.readAllBytes(generatedSource), StandardCharsets.UTF_8);
        assertTrue(generatedCode.contains("0.0/0.0"), "NaN constants should translate through Ldc");
        assertTrue(generatedCode.contains("1.0 / 0.0"), "Infinite double constants should translate through Ldc");
        assertTrue(generatedCode.contains("LLONG_MIN"), "Long minimum value should pass through Ldc handling");
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

    private List<String> javaLangStubPaths(Path sourceDir) {
        return Arrays.asList(
                sourceDir.resolve("java/lang/Object.java").toString(),
                sourceDir.resolve("java/lang/String.java").toString(),
                sourceDir.resolve("java/lang/Class.java").toString(),
                sourceDir.resolve("java/lang/Throwable.java").toString(),
                sourceDir.resolve("java/lang/Exception.java").toString(),
                sourceDir.resolve("java/lang/RuntimeException.java").toString(),
                sourceDir.resolve("java/lang/NullPointerException.java").toString()
        );
    }

    private void writeJavaLangSources(Path sourceDir) throws Exception {
        Files.write(sourceDir.resolve("java/lang/Object.java"), CleanTargetIntegrationTest.javaLangObjectSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/String.java"), CleanTargetIntegrationTest.javaLangStringSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/Class.java"), CleanTargetIntegrationTest.javaLangClassSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/Throwable.java"), CleanTargetIntegrationTest.javaLangThrowableSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/Exception.java"), CleanTargetIntegrationTest.javaLangExceptionSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/RuntimeException.java"), CleanTargetIntegrationTest.javaLangRuntimeExceptionSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("java/lang/NullPointerException.java"), CleanTargetIntegrationTest.javaLangNullPointerExceptionSource().getBytes(StandardCharsets.UTF_8));
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
                "        String[][] labels = new String[][] { { \"a\", \"b\" }, { \"c\", \"d\" } };\n" +
                "        int labelLength = 0;\n" +
                "        for (int i = 0; i < labels.length; i++) {\n" +
                "            labelLength += labels[i].length;\n" +
                "        }\n" +
                "        return total + labelLength;\n" +
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
                "        int arrays = app.loopArrays(2);\n" +
                "        int multi = app.multiArrayUsage(3);\n" +
                "        int arraysFive = app.loopArrays(5);\n" +
                "        int multiFive = app.multiArrayUsage(5);\n" +
                "        int fieldCalls = app.useFieldsAndMethods(5);\n" +
                "        report(first);\n" +
                "        report(second);\n" +
                "        report(switched);\n" +
                "        report(synchronizedValue);\n" +
                "        report(arrays);\n" +
                "        report(multi);\n" +
                "        report(arraysFive);\n" +
                "        report(multiFive);\n" +
                "        report(fieldCalls);\n" +
                "        report(first + second + switched + synchronizedValue + arrays + multi + fieldCalls);\n" +
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

    @Test
    void handleDefaultOutputWritesOutput() throws Exception {
        Parser.cleanup();
        resetByteCodeClass();
        Path sourceDir = Files.createTempDirectory("default-output-source");
        Path outputDir = Files.createTempDirectory("default-output-dest");

        Files.write(sourceDir.resolve("resource.txt"), "data".getBytes(StandardCharsets.UTF_8));
        compileDummyMainClass(sourceDir, "com.example", "MyAppDefault");

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

    @Test
    void handleIosOutputGeneratesProjectStructure() throws Exception {
        Parser.cleanup();
        resetByteCodeClass();
        Path sourceDir = Files.createTempDirectory("ios-output-source");
        Path outputDir = Files.createTempDirectory("ios-output-dest");

        Files.write(sourceDir.resolve("resource.txt"), "data".getBytes(StandardCharsets.UTF_8));
        compileDummyMainClass(sourceDir, "com.example", "MyAppIOS");

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

    private void compileDummyMainClass(Path sourceDir, String packageName, String className) throws Exception {
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

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null,
                "-source", "1.8",
                "-target", "1.8",
                "-d", sourceDir.toString(),
                javaFile.toString());
        assertEquals(0, result, "Compilation failed");
    }
}
