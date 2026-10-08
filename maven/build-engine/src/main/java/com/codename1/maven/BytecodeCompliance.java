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
package com.codename1.maven;

import org.apache.commons.io.FileUtils;
import com.codename1.build.BuildArtifact;
import com.codename1.build.BuildExecutionException;
import com.codename1.build.BuildFailureException;
import com.codename1.build.Log;
import com.codename1.build.ProjectHost;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
/// Checks the compiled application against the Codename One Java runtime API and
/// rewrites what it can: caps class files at Java 17, redirects the JDK calls the
/// runtime provides elsewhere, and fails the build on anything a device build
/// could not link.
///
/// The body of the Maven plugin's `bytecode-compliance` goal, shared with the
/// Gradle plugin, which runs it after every compile.
public class BytecodeCompliance {
    protected static final String GROUP_ID = "com.codenameone";
    protected static final String JAVA_RUNTIME_ARTIFACT_ID = "java-runtime";

    /// The build tool's answers about the project.
    protected final ProjectHost host;

    private List<File> siblingClassRoots = Collections.emptyList();

    private Set<String> pendingProjectClasses = Collections.emptySet();

    private List<Relocation> activeLayers;

    /// A check of the project `host` describes.
    public BytecodeCompliance(ProjectHost host) {
        this.host = host;
    }

    /// Other directories of this project's own compiled classes, which the
    /// checked classes may refer to but which are not checked here. Gradle
    /// compiles Kotlin and Java into separate directories and checks each in its
    /// own compile task, so a Java class calling a Kotlin one needs Kotlin's
    /// directory as a sibling. Maven compiles both into one directory and needs
    /// none.
    public BytecodeCompliance siblingClassRoots(List<File> roots) {
        this.siblingClassRoots = roots == null ? Collections.<File>emptyList() : new ArrayList<File>(roots);
        return this;
    }

    /// Internal names (`a/b/C`) of this project's classes that are not compiled
    /// yet, so a reference to one -- or to a class nested in one -- is the
    /// project's own and allowed. Kotlin compiles before javac, so a Kotlin class
    /// calling a Java one is checked before that class exists. The Java class
    /// itself is checked by javac's own pass; what this gives up is only the
    /// inherited-member walk through it, for members the Kotlin compiler has
    /// already resolved against the Java source.
    public BytecodeCompliance pendingProjectClasses(Set<String> internalNames) {
        this.pendingProjectClasses = internalNames == null ? Collections.<String>emptySet()
                : new HashSet<String>(internalNames);
        return this;
    }

    /// The compatibility layers the checked classes were relocated by. Left
    /// unset, [#execute] works them out from the project's dependencies
    /// ([CompatLayers#active]), which is right for every build; a caller that
    /// already knows them can say so. They decide two things: a reference
    /// into a layer's packages is reported under the name the application was
    /// compiled against, and it is looked for in more places (see
    /// `ComplianceScanner`).
    public BytecodeCompliance activeLayers(List<Relocation> layers) {
        this.activeLayers = layers == null ? null : new ArrayList<Relocation>(layers);
        return this;
    }

    private List<Relocation> layers() {
        return activeLayers == null ? Collections.<Relocation>emptyList() : activeLayers;
    }

    private boolean isPendingProjectClass(String owner) {
        if (pendingProjectClasses.isEmpty() || owner == null) {
            return false;
        }
        int nested = owner.indexOf('$');
        return pendingProjectClasses.contains(nested < 0 ? owner : owner.substring(0, nested));
    }

    protected Log getLog() {
        return host.log();
    }

    /// The newest modification time among the sources that decide whether the
    /// last check still stands.
    protected long sourcesModificationTime() throws IOException {
        return host.sourcesModificationTime();
    }

    /// Runs before the output is examined; the Maven plugin copies Kotlin's
    /// incremental output into place here.
    protected void beforeCheck() {
    }

    protected static long lastModifiedRecursive(File file, FilenameFilter filter) {
        long lastModified = 0L;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    lastModified = Math.max(lastModifiedRecursive(child, filter), lastModified);
                }
            }
        } else if (filter.accept(file.getParentFile(), file.getName())) {
            lastModified = file.lastModified();
        }
        return lastModified;
    }


    private static final Map<String, String> SUGGESTED_REPLACEMENTS;
    private static final Set<String> SIMD_OWNER_NAMES;
    private static final Set<String> PRIMITIVE_WRAPPER_INTERNAL_NAMES;

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
        Set<String> primitiveWrappers = new HashSet<String>();
        primitiveWrappers.add("java/lang/Boolean");
        primitiveWrappers.add("java/lang/Byte");
        primitiveWrappers.add("java/lang/Character");
        primitiveWrappers.add("java/lang/Double");
        primitiveWrappers.add("java/lang/Float");
        primitiveWrappers.add("java/lang/Integer");
        primitiveWrappers.add("java/lang/Long");
        primitiveWrappers.add("java/lang/Short");
        PRIMITIVE_WRAPPER_INTERNAL_NAMES = Collections.unmodifiableSet(primitiveWrappers);
    }

    private static final int MAX_CLASS_MAJOR_VERSION = Opcodes.V17;
    private static final String JDK_API_REWRITE_HELPER_INTERNAL_NAME = "com/codename1/impl/JdkApiRewriteHelper";
    private static final String SIMD_INTERNAL_NAME = "com/codename1/util/Simd";
    private static final Map<MethodRef, MethodRef> INVOCATION_REWRITE_RULES = createInvocationRewriteRules();

    private File complianceOutputFile;
    private InvocationRewriteSummary lastInvocationRewriteSummary = new InvocationRewriteSummary();
    private URLClassLoader validationClassLoader;

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
        rules.put(
                MethodRef.virtual("java/lang/String", "replaceAll", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
                MethodRef.staticRef(JDK_API_REWRITE_HELPER_INTERNAL_NAME, "replaceAll", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")
        );
        rules.put(
                MethodRef.virtual("java/lang/String", "replaceFirst", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
                MethodRef.staticRef(JDK_API_REWRITE_HELPER_INTERNAL_NAME, "replaceFirst", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")
        );
        // Java 8's static wrapper hash and compare methods (Kotlin data classes
        // call them), onto the helper's identical implementations.
        String[][] wrappers = {
            {"java/lang/Integer", "I"}, {"java/lang/Long", "J"}, {"java/lang/Double", "D"},
            {"java/lang/Float", "F"}, {"java/lang/Boolean", "Z"}, {"java/lang/Character", "C"},
            {"java/lang/Short", "S"}, {"java/lang/Byte", "B"},
        };
        for (String[] w : wrappers) {
            rules.put(MethodRef.staticRef(w[0], "hashCode", "(" + w[1] + ")I"),
                    MethodRef.staticRef(JDK_API_REWRITE_HELPER_INTERNAL_NAME, "hashCode", "(" + w[1] + ")I"));
        }
        for (String[] w : new String[][] {{"java/lang/Boolean", "Z"}, {"java/lang/Character", "C"},
                {"java/lang/Byte", "B"}}) {
            rules.put(MethodRef.staticRef(w[0], "compare", "(" + w[1] + w[1] + ")I"),
                    MethodRef.staticRef(JDK_API_REWRITE_HELPER_INTERNAL_NAME, "compare", "(" + w[1] + w[1] + ")I"));
        }
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

    /// Runs the check.
    public void execute() throws BuildExecutionException {
        if (shouldSkipComplianceCheck()) {
            return;
        }

        complianceOutputFile = new File(path(host.buildDirectory().getPath(), "codenameone", "compliance_check.txt"));
        getLog().info("Running bytecode compliance check against Codename One Java Runtime API");
        getLog().info("See https://www.codenameone.com/javadoc/ for supported Classes and Methods");

        if (!hasChangedSinceLastCheck()) {
            getLog().info("Sources haven't changed since the last compliance check. Skipping check");
            return;
        }

        beforeCheck();

        File outputDir = new File(host.outputDirectory().getPath());
        if (!outputDir.isDirectory()) {
            writeComplianceSuccess("No output classes found for compliance check in " + outputDir.getAbsolutePath(), 0);
            return;
        }

        int rewrittenClassCount = enforceMaxClassVersion(outputDir, MAX_CLASS_MAJOR_VERSION);
        InvocationRewriteSummary invocationRewriteSummary = applyInvocationRewrites(outputDir);
        lastInvocationRewriteSummary = invocationRewriteSummary;

        // Both calls above rewrite class files IN PLACE -- capping a class to the
        // supported version, and redirecting a call the runtime does not have. If
        // the main class is one of them, its bytes no longer match the digest the
        // build hint manifest recorded, and the simulator -- which has no bytecode
        // reader and so compares the class file itself -- reads a manifest written
        // moments ago as stale and publishes none of the annotated hints.
        //
        // Every pom in this repository runs process-annotations after this goal,
        // where the stamp would be taken from the rewritten bytes anyway. Stamping
        // here as well is what makes that ordering stop mattering: whichever of
        // the two runs last leaves a manifest describing the class on disk. A
        // no-op when there is no manifest, which is every project that declares
        // its hints in codenameone_settings.properties.
        try {
            com.codename1.maven.processors.BuildHintAnnotationProcessor
                    .restampClassDigest(outputDir);
        } catch (IOException ioe) {
            throw new BuildExecutionException(
                    "Could not stamp the build hint manifest under " + outputDir, ioe);
        }

        List<File> dependencyJars = getDependencyJarsForScanning();
        Map<String, ClassMetadata> allowedIndex = buildClassIndex(Arrays.asList(getJavaRuntimeJar(), getCodenameOneJar()));
        // A library the remap step unpacked into the output directory is
        // application code now: the relocated copy there is the one to
        // index, not the jar it came from, whose classes still name the
        // desktop toolkit.
        Set<String> bundled = CompatLibraries.bundledJarNames(Collections.singletonList(outputDir));
        if (!bundled.isEmpty()) {
            List<File> kept = new ArrayList<File>();
            for (File jar : dependencyJars) {
                if (!bundled.contains(jar.getName())) {
                    kept.add(jar);
                }
            }
            dependencyJars = kept;
        }
        Map<String, ClassMetadata> projectAndDependencyIndex = buildClassIndexWithOutput(outputDir, dependencyJars);
        projectAndDependencyIndex.putAll(buildClassIndex(siblingClassRoots));
        if (activeLayers == null) {
            activeLayers = CompatLayers.active(dependencyJars);
        }

        List<Violation> violations = scanProjectClasses(outputDir, allowedIndex, projectAndDependencyIndex);
        if (!violations.isEmpty()) {
            // The developer did not write a bundled library's classes: say
            // which jar a finding is in.
            Map<String, String> origins = CompatLibraries.classOrigins(outputDir);
            if (!origins.isEmpty()) {
                for (Violation v : violations) {
                    v.library = origins.get(v.sourceClass.replace('.', '/'));
                }
            }
            writeComplianceReport(violations, outputDir, dependencyJars, rewrittenClassCount);
            logViolationSummary(violations);
            throw new BuildFailureException(buildFailureSummary(violations));
        }

        writeComplianceSuccess("Completed compliance check on " + host.finalName(), rewrittenClassCount);
        getLog().info("Invocation rewrite summary: classes rewritten=" + invocationRewriteSummary.rewrittenClasses + ", callsites rewritten=" + invocationRewriteSummary.rewrittenCallsites);
    }

    private boolean shouldSkipComplianceCheck() {
        if ("true".equals(System.getProperty("skipComplianceCheck", "false"))) {
            return true;
        }
        if ("true".equals(host.projectProperties().getProperty("skipComplianceCheck", "false"))) {
            return true;
        }
        if ("true".equals(System.getProperty("reloadClasses", "false"))) {
            return true;
        }
        return "true".equals(host.projectProperties().getProperty("reloadClasses", "false"));
    }

    private boolean hasChangedSinceLastCheck() {
        if (!complianceOutputFile.exists()) {
            return true;
        }
        try {
            long lastCheck = complianceOutputFile.lastModified();
            if (sourcesModificationTime() > lastCheck) {
                return true;
            }
            if (lastCheckFailed()) {
                // A failure report is not a completed check. Without this a
                // rerun with unchanged sources would skip straight past the
                // violations that just failed the build.
                return true;
            }
            // The invocation rewrites and the class-version cap mutate the
            // compiled classes in place, so gating on source mtimes alone is
            // not enough: a later compile pass can regenerate target/classes
            // with unchanged sources (e.g. another `mvn package` re-running
            // javac), silently shedding the rewrites. Shipping such classes
            // breaks device builds -- the iOS translator emits calls to
            // virtual_java_lang_String_replaceAll etc. that ParparVM's
            // JavaAPI never declares. Re-run whenever any compiled class is
            // newer than the last check; the marker is written after the
            // rewrites, so an up-to-date output tree stays skippable.
            return getCompiledClassesModificationTime() > lastCheck;
        } catch (IOException ex) {
            getLog().error("Failed to check sources/classes modification time for compliance check", ex);
            return true;
        }
    }

    private boolean lastCheckFailed() throws IOException {
        String content = FileUtils.readFileToString(complianceOutputFile, "UTF-8");
        return content.startsWith(FAILURE_REPORT_HEADER);
    }

    private static final FilenameFilter CLASS_FILES_FILTER = (dir, name) -> name.endsWith(".class");

    private long getCompiledClassesModificationTime() {
        long mTime = lastModifiedRecursive(new File(host.outputDirectory().getPath()), CLASS_FILES_FILTER);
        // With kotlin.compiler.incremental the Kotlin compiler writes to its
        // own output tree which executeImpl copies into the output directory,
        // so fresh classes can sit there before any copy has happened.
        File kotlinIncrementalOutputDir = new File(path(host.buildDirectory().getPath(), "kotlin-ic", "compile", "classes"));
        if (kotlinIncrementalOutputDir.exists()) {
            mTime = Math.max(mTime, lastModifiedRecursive(kotlinIncrementalOutputDir, CLASS_FILES_FILTER));
        }
        return mTime;
    }

    private void writeComplianceSuccess(String message, int rewrittenClassCount) throws BuildExecutionException {
        complianceOutputFile.getParentFile().mkdirs();
        try {
            StringBuilder content = new StringBuilder();
            content.append(message).append("\n");
            content.append("Rewritten class files to Java 17 major version: ").append(rewrittenClassCount).append("\n");
            content.append("Rewritten JDK API callsites: ").append(lastInvocationRewriteSummary.rewrittenCallsites)
                    .append(" across ").append(lastInvocationRewriteSummary.rewrittenClasses).append(" class(es)").append("\n");
            FileUtils.writeStringToFile(complianceOutputFile, content.toString(), "UTF-8");
        } catch (IOException ex) {
            throw new BuildExecutionException("Failed to write compliance file", ex);
        }
    }

    private static final String FAILURE_REPORT_HEADER = "Codename One compliance check failed.";

    private void writeComplianceReport(List<Violation> violations, File outputDir, List<File> dependencyJars, int rewrittenClassCount) throws BuildExecutionException {
        StringBuilder report = new StringBuilder();
        report.append(FAILURE_REPORT_HEADER).append("\n");
        report.append("Project: ").append(host.finalName()).append("\n");
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
            throw new BuildExecutionException("Failed to write compliance report", ex);
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


    private int enforceMaxClassVersion(File outputDir, final int maxVersion) throws BuildExecutionException {
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
                throw new BuildExecutionException("Failed to enforce class version for " + classFile, ex);
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

    private InvocationRewriteSummary applyInvocationRewrites(File outputDir) throws BuildExecutionException {
        InvocationRewriteSummary summary = new InvocationRewriteSummary();
        List<File> classFiles = new ArrayList<File>();
        collectClassFiles(outputDir, classFiles);
        try {
            for (File classFile : classFiles) {
                try {
                    byte[] originalBytes = FileUtils.readFileToByteArray(classFile);
                    InvocationRewriteResult rewriteResult = rewriteClassInvocations(originalBytes);
                    if (rewriteResult.rewrittenCallsites > 0) {
                        validateClass(rewriteResult.bytes, classFile, outputDir);
                        FileUtils.writeByteArrayToFile(classFile, rewriteResult.bytes);
                        summary.rewrittenClasses++;
                        summary.rewrittenCallsites += rewriteResult.rewrittenCallsites;
                        getLog().info("Applied " + rewriteResult.rewrittenCallsites + " invocation rewrite(s) in " + classFile.getAbsolutePath());
                    }
                } catch (IOException ex) {
                    throw new BuildExecutionException("Failed to rewrite invocations for " + classFile, ex);
                }
            }
        } finally {
            closeValidationClassLoader();
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

    private void validateClass(byte[] classBytes, File classFile, File outputDir) throws BuildExecutionException {
        ClassLoader loader = getValidationClassLoader(outputDir);
        try {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            CheckClassAdapter.verify(new ClassReader(classBytes), loader, false, printWriter);
            printWriter.flush();
            String validationOutput = stringWriter.toString().trim();
            if (!validationOutput.isEmpty()) {
                if (isUnresolvableTypeOutput(validationOutput)) {
                    getLog().debug("Skipping deep verification for " + classFile.getName()
                            + ": referenced type(s) not on classpath. Output: " + validationOutput);
                    return;
                }
                throw new BuildExecutionException("Bytecode validation failed for " + classFile + ": " + validationOutput);
            }
        } catch (RuntimeException ex) {
            if (isUnresolvableTypeCause(ex)) {
                getLog().debug("Skipping deep verification for " + classFile.getName()
                        + ": referenced type(s) not on classpath (" + ex.getMessage() + ").");
                return;
            }
            throw new BuildExecutionException("Bytecode validation failed for " + classFile, ex);
        }
    }

    private ClassLoader getValidationClassLoader(File outputDir) {
        if (validationClassLoader != null) {
            return validationClassLoader;
        }
        List<URL> urls = new ArrayList<URL>();
        try {
            if (outputDir != null && outputDir.isDirectory()) {
                urls.add(outputDir.toURI().toURL());
            }
            if (host != null) {
                for (File jar : getDependencyJarsForScanning()) {
                    if (jar != null && jar.exists()) {
                        urls.add(jar.toURI().toURL());
                    }
                }
            }
        } catch (MalformedURLException ex) {
            getLog().debug("Failed to assemble validation classloader URLs; falling back to plugin classloader.", ex);
            return getClass().getClassLoader();
        }
        validationClassLoader = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
        return validationClassLoader;
    }

    private void closeValidationClassLoader() {
        if (validationClassLoader != null) {
            try {
                validationClassLoader.close();
            } catch (IOException ex) {
                getLog().debug("Failed to close validation classloader", ex);
            }
            validationClassLoader = null;
        }
    }

    private static boolean isUnresolvableTypeCause(Throwable ex) {
        Throwable t = ex;
        while (t != null) {
            if (t instanceof ClassNotFoundException || t instanceof TypeNotPresentException || t instanceof NoClassDefFoundError) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static boolean isUnresolvableTypeOutput(String output) {
        return output.contains("ClassNotFoundException")
                || output.contains("TypeNotPresentException")
                || output.contains("NoClassDefFoundError")
                || output.contains(" not present");
    }

    private List<Violation> scanProjectClasses(File outputDir, final Map<String, ClassMetadata> allowedIndex, final Map<String, ClassMetadata> projectAndDependencyIndex) throws BuildExecutionException {
        List<File> classFiles = new ArrayList<File>();
        collectClassFiles(outputDir, classFiles);
        List<Violation> violations = new ArrayList<Violation>();
        for (File classFile : classFiles) {
            try {
                InputStream inputStream = new BufferedInputStream(new FileInputStream(classFile));
                try {
                    ClassReader reader = new ClassReader(inputStream);
                    reader.accept(new ComplianceScanner(classFile, outputDir, allowedIndex, projectAndDependencyIndex, violations), ClassReader.SKIP_FRAMES);
                    addSemanticStackViolations(classFile, outputDir, reader, violations);
                } finally {
                    inputStream.close();
                }
            } catch (IOException ex) {
                throw new BuildExecutionException("Failed to scan class " + classFile, ex);
            }
        }
        return violations;
    }

    private void addSemanticStackViolations(File classFile, File outputDir, ClassReader reader, List<Violation> violations) throws IOException, BuildExecutionException {
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
                throw new BuildExecutionException("Failed to analyze bytecode semantics for " + classFile + " in " + classNode.name + "#" + method.name + method.desc, ex);
            }
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                int index = method.instructions.indexOf(instruction);
                Frame<BasicValue> frame = frames[index];
                if (frame == null) {
                    continue;
                }
                int opcode = instruction.getOpcode();
                if (opcode == Opcodes.MONITORENTER) {
                    BasicValue lockValue = frame.getStack(frame.getStackSize() - 1);
                    if (isPrimitiveWrapperValue(lockValue)) {
                        addSemanticViolation(violations, classFile, outputDir, classNode.name, method,
                                "Synchronization on primitive wrapper " + lockValue.getType().getInternalName(),
                                "Use a dedicated Object lock instead of synchronizing on primitive wrapper values.");
                    }
                    continue;
                }
                if (opcode == Opcodes.ARETURN) {
                    if (isAllocaValue(frame.getStack(frame.getStackSize() - 1))) {
                        addSemanticViolation(violations, classFile, outputDir, classNode.name, method,
                                "SIMD alloca value returned from method",
                                "Keep SIMD alloca scratch arrays method-local and only pass them to Simd methods.");
                    }
                    continue;
                }
                if (opcode == Opcodes.PUTSTATIC) {
                    if (isAllocaValue(frame.getStack(frame.getStackSize() - 1))) {
                        addSemanticViolation(violations, classFile, outputDir, classNode.name, method,
                                "SIMD alloca value stored into static field",
                                "Keep SIMD alloca scratch arrays method-local and only pass them to Simd methods.");
                    }
                    continue;
                }
                if (opcode == Opcodes.PUTFIELD) {
                    if (isAllocaValue(frame.getStack(frame.getStackSize() - 1))) {
                        addSemanticViolation(violations, classFile, outputDir, classNode.name, method,
                                "SIMD alloca value stored into instance field",
                                "Keep SIMD alloca scratch arrays method-local and only pass them to Simd methods.");
                    }
                    continue;
                }
                if (opcode == Opcodes.AASTORE) {
                    if (isAllocaValue(frame.getStack(frame.getStackSize() - 1))) {
                        addSemanticViolation(violations, classFile, outputDir, classNode.name, method,
                                "SIMD alloca value stored into object array",
                                "Keep SIMD alloca scratch arrays method-local and only pass them to Simd methods.");
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
                        addSemanticViolation(violations, classFile, outputDir, classNode.name, method,
                                "SIMD alloca value passed to non-Simd method " + methodInsn.owner + "#" + methodInsn.name + methodInsn.desc,
                                "Keep SIMD alloca scratch arrays method-local and only pass them to Simd methods.");
                    }
                    continue;
                }
                if (instruction instanceof InvokeDynamicInsnNode) {
                    Type[] args = Type.getArgumentTypes(((InvokeDynamicInsnNode) instruction).desc);
                    for (int i = 0; i < args.length; i++) {
                        if (isAllocaValue(frame.getStack(frame.getStackSize() - 1 - i))) {
                            addSemanticViolation(violations, classFile, outputDir, classNode.name, method,
                                    "SIMD alloca value passed to invokedynamic",
                                    "Keep SIMD alloca scratch arrays method-local and only pass them to Simd methods.");
                            break;
                        }
                    }
                }
            }
        }
    }

    private void addSemanticViolation(List<Violation> violations, File classFile, File outputDir, String sourceClass, MethodNode method, String referencedMember, String suggestion) {
        String relativePath = classFile.getAbsolutePath().replace(outputDir.getAbsolutePath(), "");
        if (relativePath.startsWith(File.separator)) {
            relativePath = relativePath.substring(1);
        }
        violations.add(new Violation(sourceClass, method.name + method.desc, referencedMember,
                suggestion, relativePath));
    }

    private static boolean isAllocaValue(BasicValue value) {
        return value instanceof SimdAllocaValue && ((SimdAllocaValue) value).alloca;
    }

    private static boolean isPrimitiveWrapperValue(BasicValue value) {
        if (value == null || value.getType() == null || value.getType().getSort() != Type.OBJECT) {
            return false;
        }
        return PRIMITIVE_WRAPPER_INTERNAL_NAMES.contains(value.getType().getInternalName());
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
            if (obj == null || obj.getClass() != SimdAllocaValue.class) {
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
            if (type == null) {
                return BasicValue.UNINITIALIZED_VALUE;
            }
            if (type == Type.VOID_TYPE) {
                return null;
            }
            return new SimdAllocaValue(type, false);
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
            boolean alloca = isAllocaValue(value1) || isAllocaValue(value2);
            if (value1.equals(value2)) {
                return new SimdAllocaValue(value1.getType(), alloca);
            }
            if (isReferenceValue(value1) && isReferenceValue(value2)) {
                return new SimdAllocaValue(Type.getObjectType("java/lang/Object"), alloca);
            }
            BasicValue base = super.merge(value1, value2);
            if (base == null) {
                return null;
            }
            return new SimdAllocaValue(base.getType(), alloca);
        }
    }

    private static boolean isReferenceValue(BasicValue value) {
        if (value == null || value.getType() == null) {
            return false;
        }
        int sort = value.getType().getSort();
        return sort == Type.OBJECT || sort == Type.ARRAY;
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

    private Map<String, ClassMetadata> buildClassIndexWithOutput(File outputDir, List<File> dependencyJars) throws BuildExecutionException {
        Map<String, ClassMetadata> index = buildClassIndex(Collections.singletonList(outputDir));
        Map<String, ClassMetadata> dependencyIndex = buildClassIndex(dependencyJars);
        index.putAll(dependencyIndex);
        return index;
    }

    private Map<String, ClassMetadata> buildClassIndex(List<File> roots) throws BuildExecutionException {
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
                        throw new BuildExecutionException("Failed reading class metadata from " + classFile, ex);
                    }
                }
            } else if (isClassArchive(root)) {
                try {
                    indexArchive(root, index);
                } catch (IOException ex) {
                    throw new BuildExecutionException("Failed reading jar metadata from " + root, ex);
                }
            }
        }
        return index;
    }

    private void indexArchive(File archive, Map<String, ClassMetadata> index) throws IOException {
        InputStream fis = new BufferedInputStream(new FileInputStream(archive));
        try {
            indexArchiveStream(fis, archive.getAbsolutePath(), index,
                    CompatLayers.EVERY.layerOf(archive) != null);
        } finally {
            fis.close();
        }
    }

    private void indexArchiveStream(InputStream archiveStream, String sourcePrefix, Map<String, ClassMetadata> index) throws IOException {
        indexArchiveStream(archiveStream, sourcePrefix, index, false);
    }

    /// `relocated`: a compatibility layer's runtime, which the remap step
    /// ships relocated; its classes are indexed under those names too, so a
    /// pass checked before the runtime is copied in (Gradle's Kotlin pass)
    /// resolves the relocated references.
    private void indexArchiveStream(InputStream archiveStream, String sourcePrefix, Map<String, ClassMetadata> index,
                                    boolean relocated) throws IOException {
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
                    ClassMetadata moved = null;
                    if (relocated) {
                        moved = readClassMetadata(new ByteArrayInputStream(CompatLayers.EVERY.remap(bytes)),
                                sourcePrefix + "!" + entryName);
                    }
                    // A runtime authored under the names it ships with (the
                    // Swing layer's) keeps its name through relocation. It is
                    // then one class, not two, and the relocated reading is
                    // the one to keep: its members name the JDK classes the
                    // device has, as the application's relocated calls do.
                    if (metadata != null && (moved == null || !metadata.name.equals(moved.name))) {
                        index.put(metadata.name, metadata);
                    }
                    if (moved != null) {
                        index.put(moved.name, moved);
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
        for (BuildArtifact artifact : host.artifacts()) {
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
                File jar = host.getJar(artifact);
                if (isClassArchive(jar)) {
                    jars.add(jar);
                }
            }
        }
        return jars;
    }

    private File getJavaRuntimeJar() {
        for (BuildArtifact artifact : host.artifacts()) {
            if (JAVA_RUNTIME_ARTIFACT_ID.equals(artifact.getArtifactId()) && GROUP_ID.equals(artifact.getGroupId())) {
                return host.getJar(artifact);
            }
        }
        File fromTool = host.getJar(GROUP_ID, JAVA_RUNTIME_ARTIFACT_ID, null);
        if (fromTool != null) {
            return fromTool;
        }
        throw new RuntimeException(JAVA_RUNTIME_ARTIFACT_ID + " not found in dependencies");
    }

    private File getCodenameOneJar() {
        String codenameOneCoreId = "codenameone-core";
        for (BuildArtifact artifact : host.artifacts()) {
            if (codenameOneCoreId.equals(artifact.getArtifactId()) && GROUP_ID.equals(artifact.getGroupId())) {
                return host.getJar(artifact);
            }
        }
        File fromTool = host.getJar(GROUP_ID, codenameOneCoreId, null);
        if (fromTool != null) {
            return fromTool;
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

    /// Reports every reference a class makes that the device could not link.
    ///
    /// A project with no compatibility layer is checked at its method, field
    /// and type instructions, and each failing instruction is one violation.
    ///
    /// A reference into an ACTIVE layer's relocated packages is held to more,
    /// because for the Swing layer this check is the only compiler there is:
    /// the application compiled against the real JDK, so javac accepted every
    /// call the JDK has, and nothing before this point knows which of them
    /// the layer provides. Such a reference is therefore also looked for in
    /// the superclass and interfaces, the types of declared fields and
    /// methods, declared `throws`, `catch` types, class constants, array
    /// types, and the three parts of an `invokedynamic` -- which is what every
    /// lambda and method reference compiles to: the functional interface in
    /// its descriptor, the bootstrap method types, and the method handle's
    /// target. It is reported under the name the developer wrote
    /// (`javax.swing.JTable.setAutoCreateRowSorter(boolean)`), once per source
    /// line, with a missing class reported as the class rather than as each
    /// of its members.
    ///
    /// None of the additional places is examined for any other reference, so
    /// the result for a project with no layer is what it always was.
    ///
    /// #### What it cannot see
    ///
    /// An application class that overrides or implements a method the JDK
    /// type declares but the layer's type does not -- `paintComponent` on a
    /// component whose compat class never calls it, a listener method the
    /// compat interface lacks -- passes. The override is an ordinary method
    /// of the application's own class; nothing in the class file says which
    /// inherited method it was written against, and telling would need the
    /// JDK's own signatures to compare the layer with. The method is then
    /// simply never called on a device. Only an explicit `super.method()`
    /// call, which is a method instruction like any other, is caught.
    private final class ComplianceScanner extends ClassVisitor {
        private final File classFile;
        private final File outputDir;
        private final Map<String, ClassMetadata> allowedIndex;
        private final Map<String, ClassMetadata> projectAndDependencyIndex;
        private final List<Violation> violations;
        private final List<Relocation> layers = layers();
        private final ClassRelocator layerNames = new ClassRelocator(layers);
        /// Layer symbols already reported for this class: `symbol` alone, and
        /// `symbol@line` for each line it was reported at.
        private final Set<String> reportedLayerSymbols = new HashSet<String>();
        /// Layer symbols missing from a declaration, which has no line of its
        /// own. Reported at the end, and only when no instruction reported
        /// the same symbol with a line.
        private final Map<String, DeclaredSymbol> declaredLayerSymbols = new LinkedHashMap<String, DeclaredSymbol>();
        private String className;
        private String sourceFile;
        private int currentLine;

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
            if (layers.isEmpty()) {
                return;
            }
            List<DeclaredSymbol> declared = new ArrayList<DeclaredSymbol>();
            checkLayerType("(extends)", superName, declared);
            if (interfaces != null) {
                for (String iface : interfaces) {
                    checkLayerType("(implements)", iface, declared);
                }
            }
        }

        @Override
        public void visitSource(String source, String debug) {
            sourceFile = source;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (!layers.isEmpty()) {
                checkLayerDescriptor("(field " + name + ")", descriptor, new ArrayList<DeclaredSymbol>());
            }
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, final String name, final String descriptor, String signature, String[] exceptions) {
            final String sourceMethod = name + descriptor;
            currentLine = 0;
            // What the method's own declaration is missing. It has no line, so
            // it is given the method's first one when the code turns up.
            final List<DeclaredSymbol> declared = new ArrayList<DeclaredSymbol>();
            if (!layers.isEmpty()) {
                checkLayerDescriptor(sourceMethod, descriptor, declared);
                if (exceptions != null) {
                    for (String thrown : exceptions) {
                        checkLayerType(sourceMethod, thrown, declared);
                    }
                }
            }
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitLineNumber(int line, Label start) {
                    if (currentLine == 0) {
                        for (DeclaredSymbol d : declared) {
                            d.line = line;
                        }
                    }
                    currentLine = line;
                }

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

                @Override
                public void visitMultiANewArrayInsn(String arrayDescriptor, int dimensions) {
                    if (!layers.isEmpty()) {
                        checkLayerType(sourceMethod, arrayDescriptor, null);
                    }
                }

                @Override
                public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                    // Visited before the code it covers, so before any line:
                    // it is a declaration of the method, like its throws.
                    if (!layers.isEmpty() && type != null) {
                        checkLayerType(sourceMethod, type, declared);
                    }
                }

                @Override
                public void visitLdcInsn(Object value) {
                    if (!layers.isEmpty()) {
                        checkLayerConstant(sourceMethod, value);
                    }
                }

                @Override
                public void visitInvokeDynamicInsn(String indyName, String indyDescriptor, Handle bootstrap,
                                                   Object... bootstrapArguments) {
                    if (layers.isEmpty()) {
                        return;
                    }
                    int before = violations.size();
                    checkLayerDescriptor(sourceMethod, indyDescriptor, null);
                    for (Object argument : bootstrapArguments) {
                        checkLayerConstant(sourceMethod, argument);
                    }
                    if (violations.size() == before) {
                        // Only when every class it names exists: a missing
                        // parameter type already explains a missing method.
                        checkFunctionalInterfaceMethod(sourceMethod, indyName, indyDescriptor, bootstrap,
                                bootstrapArguments);
                    }
                }
            };
        }

        @Override
        public void visitEnd() {
            for (Map.Entry<String, DeclaredSymbol> e : declaredLayerSymbols.entrySet()) {
                if (reportedLayerSymbols.add(e.getKey())) {
                    DeclaredSymbol d = e.getValue();
                    violations.add(new Violation(className, d.sourceMethod, d.message, null, relativePath(),
                            sourceFile, d.line));
                }
            }
        }

        private void checkMethodReference(String sourceClass, String sourceMethod, String owner, String memberName, String memberDescriptor) {
            if (shouldAllowMethod(owner, memberName, memberDescriptor)) {
                return;
            }
            if (reportLayerMember(sourceMethod, owner, memberName, memberDescriptor, true)) {
                return;
            }
            addViolation(sourceClass, sourceMethod, owner, owner + "#" + memberName + memberDescriptor);
        }

        private void checkFieldReference(String sourceClass, String sourceMethod, String owner, String memberName, String memberDescriptor) {
            if (shouldAllowField(owner, memberName, memberDescriptor)) {
                return;
            }
            if (reportLayerMember(sourceMethod, owner, memberName, memberDescriptor, false)) {
                return;
            }
            addViolation(sourceClass, sourceMethod, owner, owner + "#" + memberName + ":" + memberDescriptor);
        }

        private void checkTypeReference(String sourceClass, String sourceMethod, String owner) {
            if (isArrayDescriptor(owner)) {
                // An array of a layer's class is still a reference to the
                // class; of anything else it was never examined.
                if (!layers.isEmpty()) {
                    checkLayerType(sourceMethod, owner, null);
                }
                return;
            }
            if (isInternalRewriteHelper(owner)) {
                return;
            }
            if (isKnownClass(owner)) {
                return;
            }
            Relocation layer = layerOf(owner);
            if (layer != null) {
                reportLayerSymbol(sourceMethod, layer, owner, javaName(owner), null);
                return;
            }
            addViolation(sourceClass, sourceMethod, owner, owner + " (type)");
        }

        private boolean isKnownClass(String internalName) {
            return projectAndDependencyIndex.containsKey(internalName) || allowedIndex.containsKey(internalName)
                    || isPendingProjectClass(internalName);
        }

        private String relativePath() {
            String relativePath = classFile.getAbsolutePath().replace(outputDir.getAbsolutePath(), "");
            if (relativePath.startsWith(File.separator)) {
                relativePath = relativePath.substring(1);
            }
            return relativePath;
        }

        private void addViolation(String sourceClass, String sourceMethod, String owner, String referencedMember) {
            violations.add(new Violation(sourceClass, sourceMethod, referencedMember,
                    replacementFor(referencedMember, owner, layers), relativePath(), sourceFile, currentLine));
        }

        /// The active layer whose relocated packages `internalName` is in, or
        /// null: the references the wider scan applies to.
        private Relocation layerOf(String internalName) {
            if (internalName == null) {
                return null;
            }
            for (Relocation layer : layers) {
                if (layer.original(internalName) != null) {
                    return layer;
                }
            }
            return null;
        }

        /// Records that `api` is missing from `layer`. `declared` is non-null
        /// for a reference made by a declaration rather than by an
        /// instruction; such a reference is held in `declaredLayerSymbols`.
        private void reportLayerSymbol(String sourceMethod, Relocation layer, String symbol, String api,
                                       List<DeclaredSymbol> declared) {
            String message = api + " is not supported by the Codename One " + layer.name() + " compatibility layer";
            if (declared != null) {
                if (!declaredLayerSymbols.containsKey(symbol)) {
                    DeclaredSymbol d = new DeclaredSymbol(sourceMethod, message);
                    declaredLayerSymbols.put(symbol, d);
                    declared.add(d);
                }
                return;
            }
            if (!reportedLayerSymbols.add(symbol + "@" + currentLine)) {
                return;
            }
            reportedLayerSymbols.add(symbol);
            violations.add(new Violation(className, sourceMethod, message, null, relativePath(), sourceFile,
                    currentLine));
        }

        /// Reports an unresolved member whose owner is a layer's class, and
        /// answers whether it was one. A class the layer lacks altogether is
        /// reported as the class: its constructor and every method called on
        /// it would otherwise each say the same thing.
        private boolean reportLayerMember(String sourceMethod, String owner, String memberName, String memberDescriptor,
                                          boolean method) {
            Relocation layer = layerOf(owner);
            if (layer == null) {
                return false;
            }
            if (!isKnownClass(owner)) {
                reportLayerSymbol(sourceMethod, layer, owner, javaName(owner), null);
            } else {
                reportLayerSymbol(sourceMethod, layer, owner + "#" + memberName + (method ? "" : ":") + memberDescriptor,
                        javaMember(owner, memberName, memberDescriptor, method), null);
            }
            return true;
        }

        /// `type` is an internal name or an array descriptor.
        private void checkLayerType(String sourceMethod, String type, List<DeclaredSymbol> declared) {
            if (type == null) {
                return;
            }
            String name = type;
            if (isArrayDescriptor(type)) {
                Type element = Type.getType(type).getElementType();
                if (element.getSort() != Type.OBJECT) {
                    return;
                }
                name = element.getInternalName();
            }
            Relocation layer = layerOf(name);
            if (layer != null && !isKnownClass(name)) {
                reportLayerSymbol(sourceMethod, layer, name, javaName(name), declared);
            }
        }

        /// Every class a field or method descriptor names.
        private void checkLayerDescriptor(String sourceMethod, String descriptor, List<DeclaredSymbol> declared) {
            Type type = Type.getType(descriptor);
            if (type.getSort() == Type.METHOD) {
                for (Type argument : type.getArgumentTypes()) {
                    checkLayerValueType(sourceMethod, argument, declared);
                }
                checkLayerValueType(sourceMethod, type.getReturnType(), declared);
            } else {
                checkLayerValueType(sourceMethod, type, declared);
            }
        }

        private void checkLayerValueType(String sourceMethod, Type type, List<DeclaredSymbol> declared) {
            Type element = type.getSort() == Type.ARRAY ? type.getElementType() : type;
            if (element.getSort() == Type.OBJECT) {
                checkLayerType(sourceMethod, element.getInternalName(), declared);
            }
        }

        /// A constant-pool value an instruction loads: a class, a method type,
        /// a method handle or a dynamic constant. Anything else names no class.
        private void checkLayerConstant(String sourceMethod, Object value) {
            if (value instanceof Type) {
                checkLayerDescriptor(sourceMethod, ((Type) value).getDescriptor(), null);
            } else if (value instanceof Handle) {
                checkLayerHandle(sourceMethod, (Handle) value);
            } else if (value instanceof ConstantDynamic) {
                ConstantDynamic constant = (ConstantDynamic) value;
                checkLayerDescriptor(sourceMethod, constant.getDescriptor(), null);
                for (int i = 0; i < constant.getBootstrapMethodArgumentCount(); i++) {
                    checkLayerConstant(sourceMethod, constant.getBootstrapMethodArgument(i));
                }
            }
        }

        /// A method handle is a call or field access spelled as a constant:
        /// `JTable::setAutoCreateRowSorter` reaches the method through one and
        /// through no method instruction.
        private void checkLayerHandle(String sourceMethod, Handle handle) {
            String owner = handle.getOwner();
            boolean method = handle.getTag() >= Opcodes.H_INVOKEVIRTUAL;
            if (!isArrayDescriptor(owner) && layerOf(owner) != null
                    && !resolveMember(owner, memberKey(handle.getName(), handle.getDesc()), method)) {
                reportLayerMember(sourceMethod, owner, handle.getName(), handle.getDesc(), method);
            }
            checkLayerDescriptor(sourceMethod, handle.getDesc(), null);
        }

        /// A lambda implements one method of the interface its `invokedynamic`
        /// returns. The interface existing is not enough: the layer's version
        /// has to declare that method, or the lambda can never be called.
        private void checkFunctionalInterfaceMethod(String sourceMethod, String methodName, String indyDescriptor,
                                                    Handle bootstrap, Object[] bootstrapArguments) {
            if (!"java/lang/invoke/LambdaMetafactory".equals(bootstrap.getOwner()) || bootstrapArguments.length == 0
                    || !(bootstrapArguments[0] instanceof Type)) {
                return;
            }
            Type functional = Type.getReturnType(indyDescriptor);
            Type implemented = (Type) bootstrapArguments[0];
            if (functional.getSort() != Type.OBJECT || implemented.getSort() != Type.METHOD) {
                return;
            }
            String owner = functional.getInternalName();
            if (layerOf(owner) != null && isKnownClass(owner)
                    && !resolveMember(owner, memberKey(methodName, implemented.getDescriptor()), true)) {
                reportLayerMember(sourceMethod, owner, methodName, implemented.getDescriptor(), true);
            }
        }

        /// `javax.swing.JTable` for the internal name the class ships under.
        private String javaName(String internalName) {
            String original = layerNames.original(internalName);
            if (original.startsWith(Relocation.JDK_PACKAGE)) {
                for (String[] shim : Relocation.JDK_SHIMS) {
                    if (shim[1].equals(original)) {
                        original = shim[0];
                        break;
                    }
                }
            }
            return original.replace('/', '.').replace('$', '.');
        }

        private String javaTypeName(Type type) {
            if (type.getSort() == Type.ARRAY) {
                StringBuilder sb = new StringBuilder(javaTypeName(type.getElementType()));
                for (int i = 0; i < type.getDimensions(); i++) {
                    sb.append("[]");
                }
                return sb.toString();
            }
            return type.getSort() == Type.OBJECT ? javaName(type.getInternalName()) : type.getClassName();
        }

        /// The member as its documentation spells it:
        /// `javax.swing.JTable.setAutoCreateRowSorter(boolean)`,
        /// `new javax.swing.JTable(int, int)`, `javax.swing.JTable.AUTO_RESIZE_OFF`.
        private String javaMember(String owner, String memberName, String memberDescriptor, boolean method) {
            String ownerName = javaName(owner);
            if (!method) {
                return ownerName + "." + memberName;
            }
            StringBuilder sb = new StringBuilder();
            if ("<init>".equals(memberName)) {
                sb.append("new ").append(ownerName);
            } else {
                sb.append(ownerName).append('.').append(memberName);
            }
            sb.append('(');
            Type[] arguments = Type.getArgumentTypes(memberDescriptor);
            for (int i = 0; i < arguments.length; i++) {
                sb.append(i == 0 ? "" : ", ").append(javaTypeName(arguments[i]));
            }
            return sb.append(')').toString();
        }

        private boolean shouldAllowMethod(String owner, String name, String descriptor) {
            if (isArrayDescriptor(owner)) {
                return true;
            }
            if (isInternalRewriteHelper(owner) || isPendingProjectClass(owner)) {
                return true;
            }
            return resolveMember(owner, memberKey(name, descriptor), true);
        }

        private boolean shouldAllowField(String owner, String name, String descriptor) {
            if (isArrayDescriptor(owner)) {
                return true;
            }
            if (isInternalRewriteHelper(owner) || isPendingProjectClass(owner)) {
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
                if (isPendingProjectClass(current)) {
                    // A Kotlin class extending a Java one not compiled yet
                    // inherits members the Kotlin compiler already resolved.
                    return true;
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

    /// The advice printed beside a violation, or null. `owner` is the class
    /// the reference names. A class of a compatibility layer's API that
    /// arrives here unrelocated belongs to a project that has not switched
    /// the layer on; the wording for that is [CompatLayers#enableHint].
    private static String replacementFor(String referencedMember, String owner, List<Relocation> active) {
        String direct = SUGGESTED_REPLACEMENTS.get(referencedMember);
        if (direct != null) {
            return direct;
        }
        if (owner != null) {
            Relocation layer = CompatLayers.owning(owner);
            if (layer != null && !active.contains(layer)) {
                return CompatLayers.enableHint(layer);
            }
        }
        return null;
    }

    /// A reference missing from a declaration, held back until the class has
    /// been read; see `ComplianceScanner`.
    private static final class DeclaredSymbol {
        private final String sourceMethod;
        private final String message;
        private int line;

        private DeclaredSymbol(String sourceMethod, String message) {
            this.sourceMethod = sourceMethod;
            this.message = message;
        }
    }

    private static final class Violation {
        private final String sourceClass;
        private final String sourceMethod;
        private final String referencedMember;
        private final String suggestion;
        private final String sourcePath;
        /// The source file the class was compiled from and the line of the
        /// reference, when the class carries debug information: null and 0
        /// otherwise, each on its own.
        private final String sourceFile;
        private final int line;
        /// The bundled library jar the class came from, or null for a class
        /// of the application's own.
        private String library;

        private Violation(String sourceClass, String sourceMethod, String referencedMember, String suggestion, String sourcePath) {
            this(sourceClass, sourceMethod, referencedMember, suggestion, sourcePath, null, 0);
        }

        private Violation(String sourceClass, String sourceMethod, String referencedMember, String suggestion, String sourcePath,
                          String sourceFile, int line) {
            this.sourceClass = sourceClass;
            this.sourceMethod = sourceMethod;
            this.referencedMember = referencedMember;
            this.suggestion = suggestion;
            this.sourcePath = sourcePath;
            this.sourceFile = sourceFile;
            this.line = line;
        }

        /// `Foo.java:123`, `Foo.java` when only the file is known, else null.
        private String location() {
            if (sourceFile == null || sourceFile.isEmpty()) {
                return null;
            }
            return line > 0 ? sourceFile + ":" + line : sourceFile;
        }

        private String render() {
            StringBuilder sb = new StringBuilder();
            sb.append("Source class: ").append(sourceClass).append("\n");
            sb.append("Source method: ").append(sourceMethod).append("\n");
            String location = location();
            if (location != null) {
                sb.append("Source location: ").append(location).append("\n");
            }
            sb.append("Source bytecode file: ").append(sourcePath).append("\n");
            if (library != null) {
                sb.append("Source library: ").append(library).append(" (").append(CompatLibraries.origin(library))
                        .append(")\n");
            }
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
            if (library != null) {
                sb.append(" ").append(CompatLibraries.origin(library));
            }
            String location = location();
            if (location != null) {
                sb.append(" at ").append(location);
            }
            if (suggestion != null && !suggestion.isEmpty()) {
                sb.append(" Suggestion: ").append(suggestion);
            }
            return sb.toString();
        }
    }
}
