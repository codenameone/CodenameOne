package com.codename1.maven;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.codename1.maven.PathUtil.path;

/**
 * Performs bytecode-level API compliance checks by scanning compiled classes.
 */
@Mojo(name = "bytecode-compliance", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.TEST)
public class BytecodeComplianceMojo extends AbstractCN1Mojo {

    private static final Map<String, String> SUGGESTED_REPLACEMENTS;
    private static final Set<String> SIMD_OWNER_NAMES;

    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("java/lang/System#exit(I)V", "Use com.codename1.ui.CN.exitApplication() to close apps on supported targets.");
        m.put("java/lang/Thread#sleep(J)V", "Use com.codename1.ui.util.UITimer or Display.callSerially() instead of blocking sleeps.");
        m.put("java/lang/Thread#sleep(JI)V", "Use com.codename1.ui.util.UITimer or Display.callSerially() instead of blocking sleeps.");
        m.put("java/lang/Runtime#getRuntime()Ljava/lang/Runtime;", "Use Codename One platform services instead of raw java.lang.Runtime access.");
        SUGGESTED_REPLACEMENTS = Collections.unmodifiableMap(m);
        Set<String> simdOwners = new HashSet<String>();
        simdOwners.add("com/codename1/util/Simd");
        simdOwners.add("com/codename1/impl/ios/IOSSimd");
        simdOwners.add("com/codename1/impl/javase/JavaSESimd");
        SIMD_OWNER_NAMES = Collections.unmodifiableSet(simdOwners);
    }

    private static final int MAX_CLASS_MAJOR_VERSION = Opcodes.V17;
    private static final String JDK_API_REWRITE_HELPER_INTERNAL_NAME = "com/codename1/impl/JdkApiRewriteHelper";
    private static final String SIMD_INTERNAL_NAME = "com/codename1/util/Simd";
    private static final Map<MethodRef, MethodRef> INVOCATION_REWRITE_RULES = createInvocationRewriteRules();

    private File complianceOutputFile;
    private InvocationRewriteSummary lastInvocationRewriteSummary = new InvocationRewriteSummary();

    private static Map<MethodRef, MethodRef> createInvocationRewriteRules() {
        Map<MethodRef, MethodRef> rules = new LinkedHashMap<MethodRef, MethodRef>();
        rules.put(
                MethodRef.virtual("java/lang/String", "split", "(Ljava/lang/String;)[Ljava/lang/String;"),
                MethodRef.staticRef(JDK_API_REWRITE_HELPER_INTERNAL_NAME, "split", "(Ljava/lang/String;Ljava/lang/String;)[Ljava/lang/String;")
        );
        rules.put(
                MethodRef.virtual("java/lang/String", "split", "(Ljava/lang/String;I)[Ljava/lang/String;"),
                MethodRef.staticRef(JDK_API_REWRITE_HELPER_INTERNAL_NAME, "split", "(Ljava/lang/String;Ljava/lang/String;I)[Ljava/lang/String;")
        );
        return Collections.unmodifiableMap(rules);
    }

    private static boolean isSimdOwner(String owner) {
        return owner != null && SIMD_OWNER_NAMES.contains(owner);
    }

    private static boolean isSimdAllocaMethod(String owner, String name, String descriptor) {
        if (!isSimdOwner(owner)) {
            return false;
        }
        return name != null
                && name.startsWith("alloca")
                && name.length() > "alloca".length()
                && Character.isUpperCase(name.charAt("alloca".length()))
                && isSimdAllocaDescriptor(descriptor);
    }

    private static boolean isSimdAllocaDescriptor(String descriptor) {
        if (descriptor == null) {
            return false;
        }
        Type returnType = Type.getReturnType(descriptor);
        if (returnType == null || returnType.getSort() != Type.ARRAY || returnType.getDimensions() != 1) {
            return false;
        }
        Type elementType = returnType.getElementType();
        int elementSort = elementType.getSort();
        return elementSort == Type.BYTE || elementSort == Type.INT || elementSort == Type.FLOAT;
    }

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (shouldSkipComplianceCheck() || !isCN1ProjectDir()) {
            return;
        }

        complianceOutputFile = new File(path(project.getBuild().getDirectory(), "codenameone", "compliance_check.txt"));
        getLog().info("Running bytecode compliance check against Codename One Java Runtime API");
        getLog().info("See https://www.codenameone.com/javadoc/ for supported Classes and Methods");

        if (!hasChangedSinceLastCheck()) {
            getLog().info("Sources haven't changed since the last compliance check. Skipping check");
            return;
        }

        copyKotlinIncrementalCompileOutputToOutputDir();

        File outputDir = new File(project.getBuild().getOutputDirectory());
        if (!outputDir.isDirectory()) {
            writeComplianceSuccess("No output classes found for compliance check in " + outputDir.getAbsolutePath(), 0);
            return;
        }

        int rewrittenClassCount = enforceMaxClassVersion(outputDir, MAX_CLASS_MAJOR_VERSION);
        InvocationRewriteSummary invocationRewriteSummary = applyInvocationRewrites(outputDir);
        lastInvocationRewriteSummary = invocationRewriteSummary;

        List<File> dependencyJars = getDependencyJarsForScanning();
        Map<String, ClassMetadata> allowedIndex = buildClassIndex(Arrays.asList(getJavaRuntimeJar(), getCodenameOneJar()));
        Map<String, ClassMetadata> projectAndDependencyIndex = buildClassIndexWithOutput(outputDir, dependencyJars);

        List<Violation> violations = scanProjectClasses(outputDir, allowedIndex, projectAndDependencyIndex);
        if (!violations.isEmpty()) {
            writeComplianceReport(violations, outputDir, dependencyJars, rewrittenClassCount);
            logViolationSummary(violations);
            throw new MojoFailureException(buildFailureSummary(violations));
        }

        writeComplianceSuccess("Completed compliance check on " + project.getName(), rewrittenClassCount);
        getLog().info("Invocation rewrite summary: classes rewritten=" + invocationRewriteSummary.rewrittenClasses + ", callsites rewritten=" + invocationRewriteSummary.rewrittenCallsites);
    }

    private boolean shouldSkipComplianceCheck() {
        if ("true".equals(System.getProperty("skipComplianceCheck", "false"))) {
            return true;
        }
        if ("true".equals(project.getProperties().getProperty("skipComplianceCheck", "false"))) {
            return true;
        }
        if ("true".equals(System.getProperty("reloadClasses", "false"))) {
            return true;
        }
        return "true".equals(project.getProperties().getProperty("reloadClasses", "false"));
    }

    private boolean hasChangedSinceLastCheck() {
        if (!complianceOutputFile.exists()) {
            return true;
        }
        try {
            return getSourcesModificationTime(true) > complianceOutputFile.lastModified();
        } catch (IOException ex) {
            getLog().error("Failed to check sources modification time for compliance check", ex);
            return true;
        }
    }

    private void writeComplianceSuccess(String message, int rewrittenClassCount) throws MojoExecutionException {
        complianceOutputFile.getParentFile().mkdirs();
        try {
            StringBuilder content = new StringBuilder();
            content.append(message).append("\n");
            content.append("Rewritten class files to Java 17 major version: ").append(rewrittenClassCount).append("\n");
            content.append("Rewritten JDK API callsites: ").append(lastInvocationRewriteSummary.rewrittenCallsites)
                    .append(" across ").append(lastInvocationRewriteSummary.rewrittenClasses).append(" class(es)").append("\n");
            FileUtils.writeStringToFile(complianceOutputFile, content.toString(), "UTF-8");
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write compliance file", ex);
        }
    }

    private void writeComplianceReport(List<Violation> violations, File outputDir, List<File> dependencyJars, int rewrittenClassCount) throws MojoExecutionException {
        StringBuilder report = new StringBuilder();
        report.append("Codename One compliance check failed.\n");
        report.append("Project: ").append(project.getName()).append("\n");
        report.append("Output classes: ").append(outputDir.getAbsolutePath()).append("\n");
        report.append("Dependency jars scanned: ").append(dependencyJars.size()).append("\n");
        report.append("Rewritten class files to Java 17 major version: ").append(rewrittenClassCount).append("\n\n");
        report.append("Rewritten JDK API callsites: ").append(lastInvocationRewriteSummary.rewrittenCallsites)
                .append(" across ").append(lastInvocationRewriteSummary.rewrittenClasses).append(" class(es)").append("\n\n");
        report.append("Violations (").append(violations.size()).append(")\n");
        report.append("========================================\n");
        int i = 1;
        for (Violation violation : violations) {
            report.append(i++).append(") ").append(violation.render()).append("\n\n");
        }

        complianceOutputFile.getParentFile().mkdirs();
        try {
            FileUtils.writeStringToFile(complianceOutputFile, report.toString(), "UTF-8");
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write compliance report", ex);
        }
    }

    private String buildFailureSummary(List<Violation> violations) {
        int maxInMessage = Math.min(5, violations.size());
        StringBuilder sb = new StringBuilder();
        sb.append("Compliance check failed with ").append(violations.size()).append(" forbidden API reference");
        if (violations.size() != 1) {
            sb.append("s");
        }
        sb.append(".\n");
        sb.append("See ").append(complianceOutputFile.getAbsolutePath()).append(" for the full report.\n");
        sb.append("First ").append(maxInMessage).append(" violation(s):");
        for (int i = 0; i < maxInMessage; i++) {
            Violation v = violations.get(i);
            sb.append("\n - ").append(v.renderInline());
        }
        return sb.toString();
    }

    private void logViolationSummary(List<Violation> violations) {
        int maxToLog = Math.min(5, violations.size());
        getLog().error("Bytecode compliance check found " + violations.size() + " violation(s).");
        getLog().error("Detailed report written to " + complianceOutputFile.getAbsolutePath());
        for (int i = 0; i < maxToLog; i++) {
            getLog().error("[" + (i + 1) + "] " + violations.get(i).renderInline());
        }
    }


    private int enforceMaxClassVersion(File outputDir, final int maxVersion) throws MojoExecutionException {
        List<File> classFiles = new ArrayList<File>();
        collectClassFiles(outputDir, classFiles);
        int rewritten = 0;
        for (File classFile : classFiles) {
            try {
                byte[] originalBytes = FileUtils.readFileToByteArray(classFile);
                ClassVersionInfo versionInfo = readClassVersion(originalBytes);
                if (versionInfo.majorVersion > maxVersion) {
                    byte[] rewrittenBytes = rewriteClassVersion(originalBytes, maxVersion);
                    FileUtils.writeByteArrayToFile(classFile, rewrittenBytes);
                    rewritten++;
                    getLog().info("Rewrote class major version " + versionInfo.majorVersion + " -> " + maxVersion + " for " + classFile.getAbsolutePath());
                }
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to enforce class version for " + classFile, ex);
            }
        }
        if (rewritten > 0) {
            getLog().info("Rewrote " + rewritten + " class file(s) to Java 17 major version " + maxVersion);
        }
        return rewritten;
    }

    private ClassVersionInfo readClassVersion(byte[] classBytes) {
        final ClassVersionInfo out = new ClassVersionInfo();
        ClassReader reader = new ClassReader(classBytes);
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                out.majorVersion = version;
                out.className = name;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return out;
    }

    private byte[] rewriteClassVersion(byte[] classBytes, final int maxVersion) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                int effectiveVersion = version > maxVersion ? maxVersion : version;
                super.visit(effectiveVersion, access, name, signature, superName, interfaces);
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private InvocationRewriteSummary applyInvocationRewrites(File outputDir) throws MojoExecutionException {
        InvocationRewriteSummary summary = new InvocationRewriteSummary();
        List<File> classFiles = new ArrayList<File>();
        collectClassFiles(outputDir, classFiles);
        for (File classFile : classFiles) {
            try {
                byte[] originalBytes = FileUtils.readFileToByteArray(classFile);
                InvocationRewriteResult rewriteResult = rewriteClassInvocations(originalBytes);
                if (rewriteResult.rewrittenCallsites > 0) {
                    validateClass(rewriteResult.bytes, classFile);
                    FileUtils.writeByteArrayToFile(classFile, rewriteResult.bytes);
                    summary.rewrittenClasses++;
                    summary.rewrittenCallsites += rewriteResult.rewrittenCallsites;
                    getLog().info("Applied " + rewriteResult.rewrittenCallsites + " invocation rewrite(s) in " + classFile.getAbsolutePath());
                }
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to rewrite invocations for " + classFile, ex);
            }
        }
        return summary;
    }

    private InvocationRewriteResult rewriteClassInvocations(byte[] classBytes) {
        final InvocationRewriteResult result = new InvocationRewriteResult();
        final ClassReader reader = new ClassReader(classBytes);
        final ClassWriter writer = new ClassWriter(reader, 0);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, delegate) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName, String methodDescriptor, boolean isInterface) {
                        MethodRef source = new MethodRef(opcode, owner, methodName, methodDescriptor);
                        MethodRef target = INVOCATION_REWRITE_RULES.get(source);
                        if (target != null) {
                            result.rewrittenCallsites++;
                            super.visitMethodInsn(target.opcode, target.owner, target.name, target.descriptor, false);
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }
                };
            }
        };
        reader.accept(visitor, 0);
        result.bytes = result.rewrittenCallsites > 0 ? writer.toByteArray() : classBytes;
        return result;
    }

    private void validateClass(byte[] classBytes, File classFile) throws MojoExecutionException {
        try {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            CheckClassAdapter.verify(new ClassReader(classBytes), false, printWriter);
            printWriter.flush();
            String validationOutput = stringWriter.toString().trim();
            if (!validationOutput.isEmpty()) {
                throw new MojoExecutionException("Bytecode validation failed for " + classFile + ": " + validationOutput);
            }
        } catch (RuntimeException ex) {
            throw new MojoExecutionException("Bytecode validation failed for " + classFile, ex);
        }
    }

    private List<Violation> scanProjectClasses(File outputDir, final Map<String, ClassMetadata> allowedIndex, final Map<String, ClassMetadata> projectAndDependencyIndex) throws MojoExecutionException {
        List<File> classFiles = new ArrayList<File>();
        collectClassFiles(outputDir, classFiles);
        List<Violation> violations = new ArrayList<Violation>();
        for (File classFile : classFiles) {
            try {
                InputStream inputStream = new BufferedInputStream(new FileInputStream(classFile));
                try {
                    ClassReader reader = new ClassReader(inputStream);
                    reader.accept(new ComplianceScanner(classFile, outputDir, allowedIndex, projectAndDependencyIndex, violations), ClassReader.SKIP_FRAMES);
                    addSimdAllocaViolations(classFile, outputDir, reader, violations);
                } finally {
                    inputStream.close();
                }
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to scan class " + classFile, ex);
            }
        }
        return violations;
    }

    private void addSimdAllocaViolations(File classFile, File outputDir, ClassReader reader, List<Violation> violations) throws IOException, MojoExecutionException {
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);
        for (MethodNode method : classNode.methods) {
            if (method.instructions == null || method.instructions.size() == 0) {
                continue;
            }
            Frame<BasicValue>[] frames;
            try {
                Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(new SimdAllocaInterpreter());
                frames = analyzer.analyze(classNode.name, method);
            } catch (AnalyzerException ex) {
                throw new MojoExecutionException("Failed to analyze SIMD alloca usage for " + classFile + " in " + classNode.name + "#" + method.name + method.desc, ex);
            }
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                int index = method.instructions.indexOf(instruction);
                Frame<BasicValue> frame = frames[index];
                if (frame == null) {
                    continue;
                }
                int opcode = instruction.getOpcode();
                if (opcode == Opcodes.ARETURN) {
                    if (isAllocaValue(frame.getStack(frame.getStackSize() - 1))) {
                        addViolation(violations, classFile, outputDir, classNode.name, method, "SIMD alloca value returned from method");
                    }
                    continue;
                }
                if (opcode == Opcodes.PUTSTATIC) {
                    if (isAllocaValue(frame.getStack(frame.getStackSize() - 1))) {
                        addViolation(violations, classFile, outputDir, classNode.name, method, "SIMD alloca value stored into static field");
                    }
                    continue;
                }
                if (opcode == Opcodes.PUTFIELD) {
                    if (isAllocaValue(frame.getStack(frame.getStackSize() - 1))) {
                        addViolation(violations, classFile, outputDir, classNode.name, method, "SIMD alloca value stored into instance field");
                    }
                    continue;
                }
                if (opcode == Opcodes.AASTORE) {
                    if (isAllocaValue(frame.getStack(frame.getStackSize() - 1))) {
                        addViolation(violations, classFile, outputDir, classNode.name, method, "SIMD alloca value stored into object array");
                    }
                    continue;
                }
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) instruction;
                    int argumentCount = Type.getArgumentTypes(methodInsn.desc).length;
                    boolean usesAlloca = false;
                    for (int i = 0; i < argumentCount; i++) {
                        if (isAllocaValue(frame.getStack(frame.getStackSize() - 1 - i))) {
                            usesAlloca = true;
                            break;
                        }
                    }
                    // Non-static calls also consume the receiver object from the stack.
                    if (!usesAlloca && opcode != Opcodes.INVOKESTATIC
                            && isAllocaValue(frame.getStack(frame.getStackSize() - 1 - argumentCount))) {
                        usesAlloca = true;
                    }
                    if (usesAlloca && !isSimdOwner(methodInsn.owner)) {
                        addViolation(violations, classFile, outputDir, classNode.name, method,
                                "SIMD alloca value passed to non-Simd method " + methodInsn.owner + "#" + methodInsn.name + methodInsn.desc);
                    }
                    continue;
                }
                if (instruction instanceof InvokeDynamicInsnNode) {
                    Type[] args = Type.getArgumentTypes(((InvokeDynamicInsnNode) instruction).desc);
                    for (int i = 0; i < args.length; i++) {
                        if (isAllocaValue(frame.getStack(frame.getStackSize() - 1 - i))) {
                            addViolation(violations, classFile, outputDir, classNode.name, method, "SIMD alloca value passed to invokedynamic");
                            break;
                        }
                    }
                }
            }
        }
    }

    private void addViolation(List<Violation> violations, File classFile, File outputDir, String sourceClass, MethodNode method, String referencedMember) {
        String relativePath = classFile.getAbsolutePath().replace(outputDir.getAbsolutePath(), "");
        if (relativePath.startsWith(File.separator)) {
            relativePath = relativePath.substring(1);
        }
        violations.add(new Violation(sourceClass, method.name + method.desc, referencedMember,
                "Keep SIMD alloca scratch arrays method-local and only pass them to Simd methods.", relativePath));
    }

    private static boolean isAllocaValue(BasicValue value) {
        return value instanceof SimdAllocaValue && ((SimdAllocaValue) value).alloca;
    }

    private static final class SimdAllocaValue extends BasicValue {
        private final boolean alloca;

        private SimdAllocaValue(Type type, boolean alloca) {
            super(type);
            this.alloca = alloca;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SimdAllocaValue)) {
                return false;
            }
            SimdAllocaValue other = (SimdAllocaValue) obj;
            Type type = getType();
            Type otherType = other.getType();
            if (type == null ? otherType != null : !type.equals(otherType)) {
                return false;
            }
            return alloca == other.alloca;
        }

        @Override
        public int hashCode() {
            Type type = getType();
            return 31 * (type != null ? type.hashCode() : 0) + (alloca ? 1 : 0);
        }
    }

    private static final class SimdAllocaInterpreter extends BasicInterpreter {
        private SimdAllocaInterpreter() {
            super(Opcodes.ASM9);
        }

        @Override
        public BasicValue newValue(Type type) {
            BasicValue base = super.newValue(type);
            if (base == null || base == BasicValue.UNINITIALIZED_VALUE) {
                return base;
            }
            return new SimdAllocaValue(base.getType(), false);
        }

        @Override
        public BasicValue copyOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
            return value;
        }

        @Override
        public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
            BasicValue base = super.unaryOperation(insn, value);
            if (base == null) {
                return null;
            }
            return new SimdAllocaValue(base.getType(), isAllocaValue(value));
        }

        @Override
        public BasicValue binaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2) throws AnalyzerException {
            BasicValue base = super.binaryOperation(insn, value1, value2);
            if (base == null) {
                return null;
            }
            return new SimdAllocaValue(base.getType(), isAllocaValue(value1) || isAllocaValue(value2));
        }

        @Override
        public BasicValue ternaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2, BasicValue value3) throws AnalyzerException {
            BasicValue base = super.ternaryOperation(insn, value1, value2, value3);
            if (base == null) {
                return null;
            }
            return new SimdAllocaValue(base.getType(), isAllocaValue(value1) || isAllocaValue(value2) || isAllocaValue(value3));
        }

        @Override
        public BasicValue naryOperation(AbstractInsnNode insn, List<? extends BasicValue> values) throws AnalyzerException {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                if (isSimdAllocaMethod(methodInsn.owner, methodInsn.name, methodInsn.desc)) {
                    return new SimdAllocaValue(Type.getReturnType(methodInsn.desc), true);
                }
            }
            BasicValue base = super.naryOperation(insn, values);
            if (base == null) {
                return null;
            }
            boolean alloca = false;
            for (BasicValue value : values) {
                if (isAllocaValue(value)) {
                    alloca = true;
                    break;
                }
            }
            return new SimdAllocaValue(base.getType(), alloca);
        }

        @Override
        public BasicValue merge(BasicValue value1, BasicValue value2) {
            BasicValue base = super.merge(value1, value2);
            if (base == null) {
                return null;
            }
            return new SimdAllocaValue(base.getType(), isAllocaValue(value1) || isAllocaValue(value2));
        }
    }

    private void collectClassFiles(File file, List<File> out) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isFile() && file.getName().endsWith(".class")) {
            out.add(file);
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) {
                return;
            }
            for (File child : children) {
                collectClassFiles(child, out);
            }
        }
    }

    private Map<String, ClassMetadata> buildClassIndexWithOutput(File outputDir, List<File> dependencyJars) throws MojoExecutionException {
        Map<String, ClassMetadata> index = buildClassIndex(Collections.singletonList(outputDir));
        Map<String, ClassMetadata> dependencyIndex = buildClassIndex(dependencyJars);
        index.putAll(dependencyIndex);
        return index;
    }

    private Map<String, ClassMetadata> buildClassIndex(List<File> roots) throws MojoExecutionException {
        Map<String, ClassMetadata> index = new HashMap<String, ClassMetadata>();
        for (File root : roots) {
            if (root == null || !root.exists()) {
                continue;
            }
            if (root.isDirectory()) {
                List<File> classFiles = new ArrayList<File>();
                collectClassFiles(root, classFiles);
                for (File classFile : classFiles) {
                    try {
                        InputStream inputStream = new BufferedInputStream(new FileInputStream(classFile));
                        try {
                            ClassMetadata metadata = readClassMetadata(inputStream, classFile.getAbsolutePath());
                            if (metadata != null) {
                                index.put(metadata.name, metadata);
                            }
                        } finally {
                            inputStream.close();
                        }
                    } catch (IOException ex) {
                        throw new MojoExecutionException("Failed reading class metadata from " + classFile, ex);
                    }
                }
            } else if (isClassArchive(root)) {
                try {
                    indexArchive(root, index);
                } catch (IOException ex) {
                    throw new MojoExecutionException("Failed reading jar metadata from " + root, ex);
                }
            }
        }
        return index;
    }

    private void indexArchive(File archive, Map<String, ClassMetadata> index) throws IOException {
        InputStream fis = new BufferedInputStream(new FileInputStream(archive));
        try {
            indexArchiveStream(fis, archive.getAbsolutePath(), index);
        } finally {
            fis.close();
        }
    }

    private void indexArchiveStream(InputStream archiveStream, String sourcePrefix, Map<String, ClassMetadata> index) throws IOException {
        ZipInputStream zip = new ZipInputStream(archiveStream);
        try {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                byte[] bytes = readAllBytes(zip);
                if (entryName.endsWith(".class")) {
                    if (shouldSkipArchiveClassEntry(entryName)) {
                        continue;
                    }
                    ClassMetadata metadata = readClassMetadata(new ByteArrayInputStream(bytes), sourcePrefix + "!" + entryName);
                    if (metadata != null) {
                        index.put(metadata.name, metadata);
                    }
                } else if (isClassArchiveName(entryName)) {
                    indexArchiveStream(new ByteArrayInputStream(bytes), sourcePrefix + "!" + entryName, index);
                }
            }
        } finally {
            zip.close();
        }
    }

    private byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = input.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        return out.toByteArray();
    }

    private boolean shouldSkipArchiveClassEntry(String name) {
        if (name == null || name.isEmpty()) {
            return true;
        }
        if (!name.endsWith(".class")) {
            return true;
        }
        if ("module-info.class".equals(name)) {
            return true;
        }
        return name.startsWith("META-INF/versions/");
    }

    private boolean isClassArchive(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        return isClassArchiveName(file.getName());
    }

    private boolean isClassArchiveName(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase();
        return lower.endsWith(".jar") || lower.endsWith(".cn1lib") || lower.endsWith(".zip");
    }

    private ClassMetadata readClassMetadata(InputStream inputStream, String sourceDescription) throws IOException {
        final ClassMetadata metadata = new ClassMetadata();
        try {
            ClassReader reader = new ClassReader(inputStream);
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    metadata.name = name;
                    metadata.superName = superName;
                    metadata.interfaces = interfaces == null ? Collections.<String>emptyList() : Arrays.asList(interfaces);
                }

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    metadata.fields.add(memberKey(name, descriptor));
                    return null;
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    metadata.methods.add(memberKey(name, descriptor));
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return metadata;
        } catch (RuntimeException ex) {
            getLog().warn("Skipping unreadable class metadata from " + sourceDescription + ": " + ex.getMessage());
            return null;
        }
    }

    private List<File> getDependencyJarsForScanning() {
        List<File> jars = new ArrayList<File>();
        for (Artifact artifact : project.getArtifacts()) {
            if (artifact == null || artifact.getScope() == null) {
                continue;
            }
            if (artifact.getGroupId().equals("com.codenameone") && artifact.getArtifactId().equals("codenameone-core")) {
                continue;
            }
            if (artifact.getGroupId().equals("com.codenameone") && artifact.getArtifactId().equals("java-runtime")) {
                continue;
            }
            if ("compile".equals(artifact.getScope())
                    || "provided".equals(artifact.getScope())
                    || "system".equals(artifact.getScope())
                    || "runtime".equals(artifact.getScope())
                    || "test".equals(artifact.getScope())) {
                File jar = getJar(artifact);
                if (isClassArchive(jar)) {
                    jars.add(jar);
                }
            }
        }
        return jars;
    }

    private File getJavaRuntimeJar() {
        for (Artifact artifact : project.getArtifacts()) {
            if (JAVA_RUNTIME_ARTIFACT_ID.equals(artifact.getArtifactId()) && GROUP_ID.equals(artifact.getGroupId())) {
                return getJar(artifact);
            }
        }
        for (Artifact artifact : pluginArtifacts) {
            if (JAVA_RUNTIME_ARTIFACT_ID.equals(artifact.getArtifactId()) && GROUP_ID.equals(artifact.getGroupId())) {
                return getJar(artifact);
            }
        }
        throw new RuntimeException(JAVA_RUNTIME_ARTIFACT_ID + " not found in dependencies");
    }

    private File getCodenameOneJar() {
        String codenameOneCoreId = "codenameone-core";
        for (Artifact artifact : project.getArtifacts()) {
            if (codenameOneCoreId.equals(artifact.getArtifactId()) && GROUP_ID.equals(artifact.getGroupId())) {
                return getJar(artifact);
            }
        }
        for (Artifact artifact : pluginArtifacts) {
            if (codenameOneCoreId.equals(artifact.getArtifactId()) && GROUP_ID.equals(artifact.getGroupId())) {
                return getJar(artifact);
            }
        }
        throw new RuntimeException(codenameOneCoreId + " not found in dependencies");
    }

    private static String memberKey(String name, String descriptor) {
        return name + descriptor;
    }

    private static final class MethodRef {
        final int opcode;
        final String owner;
        final String name;
        final String descriptor;

        private MethodRef(int opcode, String owner, String name, String descriptor) {
            this.opcode = opcode;
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
        }

        private static MethodRef virtual(String owner, String name, String descriptor) {
            return new MethodRef(Opcodes.INVOKEVIRTUAL, owner, name, descriptor);
        }

        private static MethodRef staticRef(String owner, String name, String descriptor) {
            return new MethodRef(Opcodes.INVOKESTATIC, owner, name, descriptor);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MethodRef)) {
                return false;
            }
            MethodRef other = (MethodRef) obj;
            return opcode == other.opcode
                    && owner.equals(other.owner)
                    && name.equals(other.name)
                    && descriptor.equals(other.descriptor);
        }

        @Override
        public int hashCode() {
            int result = opcode;
            result = 31 * result + owner.hashCode();
            result = 31 * result + name.hashCode();
            result = 31 * result + descriptor.hashCode();
            return result;
        }
    }

    private static final class InvocationRewriteResult {
        byte[] bytes;
        int rewrittenCallsites;
    }

    private static final class InvocationRewriteSummary {
        int rewrittenClasses;
        int rewrittenCallsites;
    }

    private static final class ClassVersionInfo {
        int majorVersion;
        String className;
    }

    private static final class ClassMetadata {
        String name;
        String superName;
        List<String> interfaces = Collections.emptyList();
        Set<String> methods = new HashSet<String>();
        Set<String> fields = new HashSet<String>();
    }

    private final class ComplianceScanner extends ClassVisitor {
        private final File classFile;
        private final File outputDir;
        private final Map<String, ClassMetadata> allowedIndex;
        private final Map<String, ClassMetadata> projectAndDependencyIndex;
        private final List<Violation> violations;
        private String className;

        private ComplianceScanner(File classFile,
                                  File outputDir,
                                  Map<String, ClassMetadata> allowedIndex,
                                  Map<String, ClassMetadata> projectAndDependencyIndex,
                                  List<Violation> violations) {
            super(Opcodes.ASM9);
            this.classFile = classFile;
            this.outputDir = outputDir;
            this.allowedIndex = allowedIndex;
            this.projectAndDependencyIndex = projectAndDependencyIndex;
            this.violations = violations;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            className = name;
        }

        @Override
        public MethodVisitor visitMethod(int access, final String name, final String descriptor, String signature, String[] exceptions) {
            final String sourceMethod = name + descriptor;
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String memberName, String memberDescriptor, boolean isInterface) {
                    checkMethodReference(className, sourceMethod, owner, memberName, memberDescriptor);
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String memberName, String memberDescriptor) {
                    checkFieldReference(className, sourceMethod, owner, memberName, memberDescriptor);
                }

                @Override
                public void visitTypeInsn(int opcode, String type) {
                    checkTypeReference(className, sourceMethod, type);
                }
            };
        }

        private void checkMethodReference(String sourceClass, String sourceMethod, String owner, String memberName, String memberDescriptor) {
            if (shouldAllowMethod(owner, memberName, memberDescriptor)) {
                return;
            }
            addViolation(sourceClass, sourceMethod, owner + "#" + memberName + memberDescriptor);
        }

        private void checkFieldReference(String sourceClass, String sourceMethod, String owner, String memberName, String memberDescriptor) {
            if (shouldAllowField(owner, memberName, memberDescriptor)) {
                return;
            }
            addViolation(sourceClass, sourceMethod, owner + "#" + memberName + ":" + memberDescriptor);
        }

        private void checkTypeReference(String sourceClass, String sourceMethod, String owner) {
            if (isArrayDescriptor(owner)) {
                return;
            }
            if (isInternalRewriteHelper(owner)) {
                return;
            }
            if (projectAndDependencyIndex.containsKey(owner) || allowedIndex.containsKey(owner)) {
                return;
            }
            addViolation(sourceClass, sourceMethod, owner + " (type)");
        }

        private void addViolation(String sourceClass, String sourceMethod, String referencedMember) {
            String relativePath = classFile.getAbsolutePath().replace(outputDir.getAbsolutePath(), "");
            if (relativePath.startsWith(File.separator)) {
                relativePath = relativePath.substring(1);
            }
            violations.add(new Violation(sourceClass, sourceMethod, referencedMember, replacementFor(referencedMember), relativePath));
        }

        private boolean shouldAllowMethod(String owner, String name, String descriptor) {
            if (isArrayDescriptor(owner)) {
                return true;
            }
            if (isInternalRewriteHelper(owner)) {
                return true;
            }
            return resolveMember(owner, memberKey(name, descriptor), true);
        }

        private boolean shouldAllowField(String owner, String name, String descriptor) {
            if (isArrayDescriptor(owner)) {
                return true;
            }
            if (isInternalRewriteHelper(owner)) {
                return true;
            }
            return resolveMember(owner, memberKey(name, descriptor), false);
        }

        private boolean resolveMember(String owner, String member, boolean method) {
            if (owner == null || owner.isEmpty()) {
                return false;
            }
            Deque<String> queue = new ArrayDeque<String>();
            Set<String> seen = new HashSet<String>();
            queue.add(owner);
            while (!queue.isEmpty()) {
                String current = queue.removeFirst();
                if (!seen.add(current)) {
                    continue;
                }
                ClassMetadata metadata = projectAndDependencyIndex.get(current);
                if (metadata == null) {
                    metadata = allowedIndex.get(current);
                }
                if (metadata == null) {
                    continue;
                }
                Set<String> members = method ? metadata.methods : metadata.fields;
                if (members.contains(member)) {
                    return true;
                }
                if (metadata.superName != null) {
                    queue.add(metadata.superName);
                }
                for (String iface : metadata.interfaces) {
                    queue.add(iface);
                }
            }
            return false;
        }
    }

    private static boolean isArrayDescriptor(String type) {
        return type != null && type.startsWith("[");
    }

    private static boolean isInternalRewriteHelper(String owner) {
        return JDK_API_REWRITE_HELPER_INTERNAL_NAME.equals(owner);
    }

    private static String replacementFor(String referencedMember) {
        String direct = SUGGESTED_REPLACEMENTS.get(referencedMember);
        if (direct != null) {
            return direct;
        }
        int hashPos = referencedMember.indexOf('#');
        if (hashPos > 0) {
            String ownerOnly = referencedMember.substring(0, hashPos);
            if (ownerOnly.startsWith("java/awt/") || ownerOnly.startsWith("javax/swing/")) {
                return "Codename One does not support AWT/Swing APIs. Use com.codename1.ui components for UI logic.";
            }
        }
        return null;
    }

    private static final class Violation {
        private final String sourceClass;
        private final String sourceMethod;
        private final String referencedMember;
        private final String suggestion;
        private final String sourcePath;

        private Violation(String sourceClass, String sourceMethod, String referencedMember, String suggestion, String sourcePath) {
            this.sourceClass = sourceClass;
            this.sourceMethod = sourceMethod;
            this.referencedMember = referencedMember;
            this.suggestion = suggestion;
            this.sourcePath = sourcePath;
        }

        private String render() {
            StringBuilder sb = new StringBuilder();
            sb.append("Source class: ").append(sourceClass).append("\n");
            sb.append("Source method: ").append(sourceMethod).append("\n");
            sb.append("Source bytecode file: ").append(sourcePath).append("\n");
            sb.append("Forbidden reference: ").append(referencedMember);
            if (suggestion != null && !suggestion.isEmpty()) {
                sb.append("\nSuggested replacement: ").append(suggestion);
            }
            return sb.toString();
        }

        private String renderInline() {
            StringBuilder sb = new StringBuilder();
            sb.append(sourceClass).append("#").append(sourceMethod)
                    .append(" -> ").append(referencedMember)
                    .append(" (").append(sourcePath).append(")");
            if (suggestion != null && !suggestion.isEmpty()) {
                sb.append(" Suggestion: ").append(suggestion);
            }
            return sb.toString();
        }
    }
}
