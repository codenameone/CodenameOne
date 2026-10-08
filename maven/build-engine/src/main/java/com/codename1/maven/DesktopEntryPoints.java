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
package com.codename1.maven;

import com.codename1.build.Log;
import com.codename1.builders.BuildException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

/// Generates the Codename One main class of a Swing or JavaFX application:
/// the class `codename1.packageName` and `codename1.mainName` name, which
/// every target starts.
///
/// A device runs no `main` method and has no reflection to find a
/// `javafx.application.Application` with, so the class that starts the
/// application is written here, from the compiled classes and the entry
/// record ([DesktopSources#ENTRY_RECORD]):
///
/// - a Swing application gets a subclass of the Swing layer's
///   `DesktopLifecycle` whose `runMain()` calls the recorded class's
///   `main(new String[0])`;
/// - a JavaFX application gets a subclass of the JavaFX layer's `FxLifecycle`
///   whose `createApplication()` answers `new` of the recorded class. Its
///   `main` is never called: all a JavaFX `main` does is `launch`, and the
///   lifecycle is what launches here.
///
/// Both call `CompatBoot.cn1Init()` before any application code, so the
/// application's resources are registered before its first lookup.
///
/// #### Why the class is generated and has no source
///
/// The Android layer's importer writes its main class as a source file,
/// because all that class does is name a class generated BEFORE javac runs.
/// What a desktop entry point has to say is only known AFTER it: whether the
/// recorded class exists, whether it is an `Application` or holds a `main`,
/// and under which name it ships. So the class is written into the classes
/// directory by the remap step, which every target's build runs -- the
/// simulator's included -- and the importer makes room for it by setting the
/// project's own main class source aside.
///
/// A project that keeps a main class of its own is respected: when that
/// class already extends one of the two lifecycles nothing is generated, and
/// when it does not while an entry record asks for one, the build stops and
/// says which of the two to remove.
///
/// #### Without an entry record
///
/// The application's only concrete `Application` subclass is used, else its
/// only class with a `main` method; the choice is logged. More than one
/// candidate, or none, is a build error that explains the record.
final class DesktopEntryPoints {

    /// The main class generated when the build was not told the project's
    /// own (`codename1.packageName` and `codename1.mainName`).
    static final String DEFAULT_MAIN = "com/codename1/generated/desktop/DesktopMain";

    /// The `SourceFile` of a class this step generated, which is how a later
    /// run tells its own output from a class the application declares.
    static final String GENERATED_SOURCE = "cn1-desktop-entry";

    /// The class generated beside a main class that is not public, to call
    /// its `main` from inside its package.
    static final String BRIDGE = "CN1DesktopEntryBridge";

    static final String SWING_LIFECYCLE = "com/codename1/desktopcompat/rt/DesktopLifecycle";
    private static final String FX_LIFECYCLE_SOURCE = "com/codename1/fxcompat/runtime/FxLifecycle";
    private static final String FX_APPLICATION_SOURCE = "javafx/application/Application";
    private static final String BOOT = Relocation.JDK_PACKAGE + "CompatBoot";
    private static final String MAIN_DESCRIPTOR = "([Ljava/lang/String;)V";

    /// What one class file says that an entry point depends on.
    private static final class Info {
        String name;
        String superName;
        int access;
        boolean main;
        boolean publicNoArgConstructor;
        String source;
    }

    private final File classesDir;
    private final List<File> handlerDirs;
    private final ClassRelocator relocator;
    private final boolean swingActive;
    private final boolean fxActive;
    private final boolean boot;
    private final Log log;
    private final Map<String, Info> infos = new HashMap<String, Info>();

    /// `boot` says whether the shared JDK classes ship, and so whether there
    /// is a `CompatBoot` to call.
    DesktopEntryPoints(File classesDir, List<File> handlerDirs, ClassRelocator relocator, List<Relocation> active,
                       boolean boot, Log log) {
        this.classesDir = classesDir;
        this.handlerDirs = handlerDirs;
        this.relocator = relocator;
        this.swingActive = active.contains(CompatLayers.SWING);
        this.fxActive = active.contains(CompatLayers.JAVAFX);
        this.boot = boot;
        this.log = log;
    }

    private static String fxLifecycle() {
        return CompatLayers.JAVAFX.relocate(FX_LIFECYCLE_SOURCE);
    }

    private static String fxApplication() {
        return CompatLayers.JAVAFX.relocate(FX_APPLICATION_SOURCE);
    }

    private static String dotted(String internalName) {
        return internalName.replace('/', '.');
    }

    /// Reads the entry record, or answers null when there is none.
    static Properties read(File record) throws IOException {
        if (record == null || !record.isFile()) {
            return null;
        }
        Properties p = new Properties();
        InputStream in = new FileInputStream(record);
        try {
            p.load(in);
        } finally {
            in.close();
        }
        return p;
    }

    private static String value(Properties record, String key) {
        String v = record == null ? null : record.getProperty(key);
        v = v == null ? null : v.trim();
        return v == null || v.length() == 0 ? null : v;
    }

    /// Generates the main class. `mainClass` is the project's own, dotted, or
    /// null when the build does not know it; `appClasses` are the
    /// application's relocated internal names. Answers the internal name of
    /// the class written, or null when the application needs none.
    String generate(Properties record, String mainClass, List<String> appClasses)
            throws IOException, BuildException {
        List<String> classes = new ArrayList<String>(new TreeSet<String>(appClasses));
        for (File dir : handlerDirs) {
            collect(dir, "", classes);
        }
        String target = mainClass == null || mainClass.trim().length() == 0 ? null
                : mainClass.trim().replace('.', '/');
        Info existing = target == null ? null : info(target);
        if (existing != null && GENERATED_SOURCE.equals(existing.source)) {
            // An earlier run's output, which this run replaces.
            existing = null;
            classes.remove(target);
        }
        removeBridges(classes);
        if (existing != null) {
            if (extendsOneOf(existing, SWING_LIFECYCLE, fxLifecycle())) {
                log.debug(dotted(target) + " is a desktop lifecycle of the application's own; none is generated");
                return null;
            }
            if (record == null) {
                log.debug(dotted(target) + " is the application's own main class; no desktop entry point is "
                        + "generated without " + DesktopSources.ENTRY_RECORD);
                return null;
            }
            throw new BuildException(dotted(target) + " is this project's main class and has a source of its own, "
                    + "but " + DesktopSources.ENTRY_RECORD + " asks the build to generate the class that starts "
                    + value(record, "mainClass") + ". Delete the source of " + dotted(target)
                    + " so that the build generates it, or delete " + DesktopSources.ENTRY_RECORD
                    + " to keep starting the application yourself.");
        }
        if (record == null && target == null) {
            log.debug("No " + DesktopSources.ENTRY_RECORD + " and no main class name; no desktop entry point");
            return null;
        }
        String declared = value(record, "mainClass");
        String kind = value(record, "kind");
        Info entry;
        if (declared == null) {
            if (record != null) {
                throw new BuildException(DesktopSources.ENTRY_RECORD + " has no mainClass. " + recordHelp(classes));
            }
            entry = onlyCandidate(classes);
            kind = isApplication(entry) ? DesktopSources.KIND_JAVAFX : DesktopSources.KIND_SWING;
            log.info("No " + DesktopSources.ENTRY_RECORD + ": the application starts through " + dotted(entry.name)
                    + " (" + kind + "), the only class it could start through");
        } else {
            String name = relocator.map(declared.replace('.', '/'));
            entry = classes.contains(name) ? info(name) : null;
            if (entry == null) {
                throw new BuildException(DesktopSources.ENTRY_RECORD + " names mainClass=" + declared
                        + ", which is not among the application's compiled classes. " + recordHelp(classes));
            }
            if (kind == null) {
                kind = isApplication(entry) ? DesktopSources.KIND_JAVAFX : DesktopSources.KIND_SWING;
                if (!isApplication(entry)) {
                    List<Info> applications = applications(classes);
                    if (applications.size() == 1) {
                        log.info(declared + " is a launcher; the application starts through "
                                + dotted(applications.get(0).name));
                        entry = applications.get(0);
                        kind = DesktopSources.KIND_JAVAFX;
                    }
                }
            }
        }
        String out = target == null ? DEFAULT_MAIN : target;
        byte[] cls;
        if (DesktopSources.KIND_JAVAFX.equalsIgnoreCase(kind)) {
            cls = javafx(out, entry, classes);
        } else if (DesktopSources.KIND_SWING.equalsIgnoreCase(kind)) {
            cls = swing(out, entry, classes);
        } else {
            throw new BuildException(DesktopSources.ENTRY_RECORD + " has kind=" + kind + "; it is "
                    + DesktopSources.KIND_SWING + " or " + DesktopSources.KIND_JAVAFX + ".");
        }
        File dest = new File(classesDir, out + ".class");
        File parent = dest.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        ClassRelocator.writeIfDifferent(dest, cls);
        log.info("Generated the main class " + dotted(out) + ", which starts " + dotted(entry.name) + " (" + kind
                + ")");
        return out;
    }

    // ---- the two kinds ------------------------------------------------------

    private byte[] swing(String out, Info entry, List<String> classes) throws IOException, BuildException {
        if (!swingActive) {
            throw new BuildException("The application starts through the main method of " + dotted(entry.name)
                    + ", which takes the Swing compatibility layer, but its classes use nothing of java.awt or "
                    + "javax.swing. " + (fxActive ? "For a JavaFX application, record the "
                    + "javafx.application.Application subclass with kind=" + DesktopSources.KIND_JAVAFX + ". " : "")
                    + recordHelp(classes));
        }
        String owner = mainOwner(entry);
        if (owner == null) {
            throw new BuildException(dotted(entry.name) + " does not declare public static void main(String[]), so "
                    + "the application cannot be started through it. " + recordHelp(classes));
        }
        if (isApplication(entry)) {
            log.warn(dotted(entry.name) + " extends javafx.application.Application but is started through its main "
                    + "method (kind=" + DesktopSources.KIND_SWING + "): Application.launch does nothing there. Use "
                    + "kind=" + DesktopSources.KIND_JAVAFX + ".");
        }
        String callOn = entry.name;
        if ((entry.access & Opcodes.ACC_PUBLIC) == 0) {
            // A main class need not be public for a JVM to start it, but only
            // a class of its own package can call it.
            int slash = entry.name.lastIndexOf('/');
            callOn = entry.name.substring(0, slash + 1) + BRIDGE;
            ClassRelocator.writeIfDifferent(new File(classesDir, callOn + ".class"), bridge(callOn, entry.name));
        }
        ClassWriter cw = lifecycle(out, SWING_LIFECYCLE);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PROTECTED, "runMain", "()V", null,
                new String[] {"java/lang/Exception"});
        mv.visitCode();
        boot(mv);
        callMain(mv, callOn);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private byte[] javafx(String out, Info entry, List<String> classes) throws IOException, BuildException {
        if (!fxActive) {
            throw new BuildException(DesktopSources.ENTRY_RECORD + " says the application is a JavaFX one (kind="
                    + DesktopSources.KIND_JAVAFX + "), but its classes use nothing of javafx. "
                    + recordHelp(classes));
        }
        String problem = null;
        if (!isApplication(entry)) {
            problem = "does not extend javafx.application.Application";
        } else if ((entry.access & Opcodes.ACC_ABSTRACT) != 0) {
            problem = "is abstract";
        } else if ((entry.access & Opcodes.ACC_PUBLIC) == 0) {
            problem = "is not public";
        } else if (!entry.publicNoArgConstructor) {
            problem = "has no public constructor without parameters";
        }
        if (problem != null) {
            throw new BuildException(dotted(entry.name) + " " + problem + ", so it cannot be started as a JavaFX "
                    + "application. " + recordHelp(classes));
        }
        ClassWriter cw = lifecycle(out, fxLifecycle());
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PROTECTED, "createApplication",
                "()L" + fxApplication() + ";", null, null);
        mv.visitCode();
        boot(mv);
        mv.visitTypeInsn(Opcodes.NEW, entry.name);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, entry.name, "<init>", "()V", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /// A public class extending `superName`, with its constructor.
    private static ClassWriter lifecycle(String name, String superName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, name, null, superName, null);
        cw.visitSource(GENERATED_SOURCE, null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        return cw;
    }

    private void boot(MethodVisitor mv) {
        if (boot) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, BOOT, "cn1Init", "()V", false);
        }
    }

    private static void callMain(MethodVisitor mv, String owner) {
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "main", MAIN_DESCRIPTOR, false);
    }

    private static byte[] bridge(String name, String mainClass) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, name, null,
                "java/lang/Object", null);
        cw.visitSource(GENERATED_SOURCE, null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main", MAIN_DESCRIPTOR, null,
                new String[] {"java/lang/Exception"});
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, mainClass, "main", MAIN_DESCRIPTOR, false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    // ---- reading the application's classes ----------------------------------

    /// Adds the classes under a handler directory, by the names they ship
    /// under: such a directory may not have been relocated yet.
    private void collect(File dir, String rel, List<String> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                String child = rel + f.getName() + "/";
                if (!CompatLayers.isExtractedRuntime(child)) {
                    collect(f, child, out);
                }
            } else if (f.getName().endsWith(".class")) {
                String name = relocator.map(rel + f.getName().substring(0, f.getName().length() - 6));
                if (!out.contains(name)) {
                    out.add(name);
                }
            }
        }
    }

    /// Takes an earlier run's bridge classes out of the candidates: each
    /// declares a `main`, and none is the application's.
    private void removeBridges(List<String> classes) throws IOException {
        for (int i = classes.size() - 1; i >= 0; i--) {
            String name = classes.get(i);
            if (name.endsWith("/" + BRIDGE) || name.equals(BRIDGE)) {
                Info info = info(name);
                if (info != null && GENERATED_SOURCE.equals(info.source)) {
                    classes.remove(i);
                }
            }
        }
    }

    private File find(String name) {
        File f = new File(classesDir, name + ".class");
        if (f.isFile()) {
            return f;
        }
        for (File dir : handlerDirs) {
            f = new File(dir, name + ".class");
            if (f.isFile()) {
                return f;
            }
            // Not relocated yet: under the name it was compiled to.
            for (Relocation r : relocator.relocations()) {
                String original = r.original(name);
                if (original != null && new File(dir, original + ".class").isFile()) {
                    return new File(dir, original + ".class");
                }
            }
        }
        return null;
    }

    /// What the class file of `name` says, or null when the application and
    /// the extracted runtimes have no such class.
    private Info info(String name) throws IOException {
        if (infos.containsKey(name)) {
            return infos.get(name);
        }
        File f = find(name);
        Info out = null;
        if (f != null) {
            final Info info = new Info();
            new ClassReader(Files.readAllBytes(f.toPath())).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String n, String signature, String superName,
                                  String[] interfaces) {
                    info.name = relocator.map(n);
                    info.superName = superName == null ? null : relocator.map(superName);
                    info.access = access;
                }

                @Override
                public void visitSource(String source, String debug) {
                    info.source = source;
                }

                @Override
                public MethodVisitor visitMethod(int access, String n, String descriptor, String signature,
                                                 String[] exceptions) {
                    int publicStatic = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
                    if ("main".equals(n) && MAIN_DESCRIPTOR.equals(descriptor)
                            && (access & publicStatic) == publicStatic) {
                        info.main = true;
                    }
                    if ("<init>".equals(n) && "()V".equals(descriptor) && (access & Opcodes.ACC_PUBLIC) != 0) {
                        info.publicNoArgConstructor = true;
                    }
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
            out = info;
        }
        infos.put(name, out);
        return out;
    }

    /// Whether `info` is, or inherits from, one of `ancestors`.
    private boolean extendsOneOf(Info info, String... ancestors) throws IOException {
        Info at = info;
        // The bound only guards against a malformed, cyclic hierarchy.
        for (int depth = 0; at != null && depth < 64; depth++) {
            for (String ancestor : ancestors) {
                if (ancestor.equals(at.name) || ancestor.equals(at.superName)) {
                    return true;
                }
            }
            at = at.superName == null ? null : info(at.superName);
        }
        return false;
    }

    private boolean isApplication(Info info) throws IOException {
        return !fxApplication().equals(info.name) && extendsOneOf(info, fxApplication());
    }

    /// The class that declares the `main` a call on `info` resolves to: its
    /// own, or one it inherits. Null when there is none.
    private String mainOwner(Info info) throws IOException {
        Info at = info;
        for (int depth = 0; at != null && depth < 64; depth++) {
            if (at.main) {
                return at.name;
            }
            at = at.superName == null ? null : info(at.superName);
        }
        return null;
    }

    /// The application's concrete, public `Application` subclasses.
    private List<Info> applications(List<String> classes) throws IOException {
        List<Info> out = new ArrayList<Info>();
        if (!fxActive) {
            return out;
        }
        for (String name : classes) {
            Info info = info(name);
            if (info != null && isApplication(info) && (info.access & Opcodes.ACC_ABSTRACT) == 0
                    && (info.access & Opcodes.ACC_PUBLIC) != 0 && info.publicNoArgConstructor) {
                out.add(info);
            }
        }
        return out;
    }

    private List<Info> mains(List<String> classes) throws IOException {
        List<Info> out = new ArrayList<Info>();
        for (String name : classes) {
            Info info = info(name);
            if (info != null && info.main) {
                out.add(info);
            }
        }
        return out;
    }

    private Info onlyCandidate(List<String> classes) throws IOException, BuildException {
        List<Info> applications = applications(classes);
        if (applications.size() == 1) {
            return applications.get(0);
        }
        List<Info> mains = applications.isEmpty() ? mains(classes) : applications;
        if (mains.size() == 1) {
            return mains.get(0);
        }
        throw new BuildException((mains.isEmpty()
                ? "The application has no class to start through: none extends javafx.application.Application or "
                + "declares public static void main(String[]). "
                : "The application has several classes it could start through, and nothing says which. ")
                + recordHelp(classes));
    }

    /// What the entry record is, and what could go in it.
    private String recordHelp(List<String> classes) throws IOException {
        StringBuilder b = new StringBuilder("Name the class that starts the application in src/main/desktop/"
                + DesktopSources.ENTRY_RECORD + ":\n  mainClass=<fully qualified class name>\n  kind="
                + DesktopSources.KIND_SWING + " (started through its main method) or " + DesktopSources.KIND_JAVAFX
                + " (it extends javafx.application.Application)\n");
        List<String> lines = new ArrayList<String>();
        for (Info info : applications(classes)) {
            lines.add("  " + dotted(relocator.original(info.name)) + " (" + DesktopSources.KIND_JAVAFX + ")");
        }
        for (Info info : mains(classes)) {
            if (!isApplication(info)) {
                lines.add("  " + dotted(relocator.original(info.name)) + " (" + DesktopSources.KIND_SWING + ")");
            }
        }
        if (lines.isEmpty()) {
            b.append("No class of the application extends javafx.application.Application or declares "
                    + "public static void main(String[]).");
        } else {
            b.append("Classes it could be:\n");
            int shown = Math.min(lines.size(), 20);
            for (int i = 0; i < shown; i++) {
                b.append(lines.get(i)).append('\n');
            }
            if (shown < lines.size()) {
                b.append("  ... and ").append(lines.size() - shown).append(" more\n");
            }
        }
        return b.toString();
    }
}
