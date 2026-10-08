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
package com.codename1.fxml;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/// Compiles an application's FXML documents into classes, after javac has
/// compiled the application and before the build relocates it.
///
/// #### Why after javac
///
/// An element of a document is a class, and the compiler has to read that
/// class to know its constructors and the type of each setter
/// ([FxmlCompiler]). A custom control -- a class of the application used as
/// an element -- has no class file until javac has run, so a document can
/// only be compiled afterwards. That costs the application nothing: no
/// source of its own names a compiled document.
///
/// So this step reads the documents' classes from the layer's jar AND from
/// the directories javac (and Kotlin) compiled into, writes one Java source
/// per document, and compiles those sources itself, in process, into the
/// classes directory. It is the second, small javac run of a build.
///
/// #### Why before the relocation, and how that is known
///
/// The generated source names the JavaFX API as an application does
/// (`javafx.scene.control.Button`), and is compiled against the classes the
/// application's own javac run left. Once the build has relocated those, a
/// custom control extends `com.codename1.fxcompat.javafx...` and no longer
/// fits where the source expects a `javafx.scene.Node`.
///
/// A build in which nothing changed does not run javac, and its classes
/// directory is still the relocated one of the build before. The step must
/// then do nothing, and it knows from the class
/// [DesktopResourceCompiler] had javac compile with the application: that
/// class names `javafx.fxml.FXMLLoader`, which a relocation renames
/// ([#relocated(File)]). The same class is why the other case cannot arise --
/// its source holds a digest of the documents, so a changed document is a
/// changed source, and javac compiles the application again.
///
/// #### Class file version
///
/// The sources are compiled at Java 8 whatever JDK runs the build: they use
/// nothing newer, and every consumer of an application's classes reads that
/// version.
public final class FxmlClassCompiler {

    /// The simple name of the class that stands for all the documents; see
    /// [DesktopResourceCompiler].
    static final String MARKER = "FxmlDocuments";
    /// What the class of [#MARKER] names until a relocation renames it.
    private static final String UNRELOCATED = "javafx/fxml/FXMLLoader";

    private final List<File> resourceDirs;
    private final List<File> classpath;
    private final List<File> classDirs;
    private final File classesOut;
    private final File sourcesOut;
    private final DesktopResourceCompiler.Log log;
    private int compiled;

    /// Creates a compiler.
    ///
    /// #### Parameters
    ///
    /// - `resourceDirs`: the directories of desktop resources; one that
    ///   does not exist is ignored
    ///
    /// - `classpath`: the application's compile class path, with the
    ///   JavaFX layer's jar on it
    ///
    /// - `classDirs`: every directory the application's own classes were
    ///   compiled into, as javac and Kotlin left them
    ///
    /// - `classesOut`: where the documents' classes are written -- the
    ///   application's classes directory, which is also read as one of
    ///   `classDirs`
    ///
    /// - `sourcesOut`: where the generated sources are kept for a reader,
    ///   or `null` to keep none. Never a directory the application's javac
    ///   run compiles from
    ///
    /// - `log`: where to report
    public FxmlClassCompiler(List<File> resourceDirs, List<File> classpath, List<File> classDirs, File classesOut,
            File sourcesOut, DesktopResourceCompiler.Log log) {
        this.resourceDirs = new ArrayList<File>(resourceDirs);
        this.classpath = new ArrayList<File>(classpath);
        this.classDirs = new ArrayList<File>();
        this.classDirs.add(classesOut);
        for (File dir : classDirs) {
            if (dir != null && !this.classDirs.contains(dir)) {
                this.classDirs.add(dir);
            }
        }
        this.classesOut = classesOut;
        this.sourcesOut = sourcesOut;
        this.log = log;
    }

    /// How many documents the last run compiled; 0 when it had nothing to
    /// do.
    public int compiled() {
        return compiled;
    }

    /// The source of the class that stands for `files`, the documents by
    /// resource path.
    static String markerSource(Map<String, File> files) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("This JDK has no SHA-256", e);
        }
        for (Map.Entry<String, File> e : files.entrySet()) {
            digest.update(e.getKey().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(DesktopResourceCompiler.read(e.getValue()));
            digest.update((byte) 0);
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) {
            hex.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
        }
        StringBuilder s = new StringBuilder();
        s.append("// Generated by the Codename One FXML compiler from the application's .fxml documents.\n");
        s.append("// Do not edit: the file is written again by every build.\n");
        s.append("package ").append(FxmlCompiler.PACKAGE).append(";\n\n");
        s.append("/// Stands for the FXML documents of the application, which are compiled once this\n");
        s.append("/// class is: a changed document changes the digest, and so recompiles the application.\n");
        s.append("public final class ").append(MARKER).append(" {\n\n");
        s.append("    public static final int CN1_COUNT = ").append(files.size()).append(";\n");
        s.append("    public static final String CN1_DIGEST = \"").append(hex).append("\";\n\n");
        s.append("    private ").append(MARKER).append("() {\n    }\n\n");
        s.append("    /// The loader the documents are read through. The build tells a classes\n");
        s.append("    /// directory it has relocated from a freshly compiled one by this name.\n");
        s.append("    public static Class<?> cn1Loader() {\n");
        s.append("        return javafx.fxml.FXMLLoader.class;\n");
        s.append("    }\n");
        s.append("}\n");
        return s.toString();
    }

    /// Whether a build has relocated `classesDir` since javac last compiled
    /// into it: the class that stands for the documents is there and no
    /// longer names `javafx.fxml.FXMLLoader`. False when the class is
    /// absent, which is the state of a directory nothing was built into.
    public static boolean relocated(File classesDir) throws IOException {
        File marker = new File(classesDir, FxmlCompiler.PACKAGE.replace('.', '/') + "/" + MARKER + ".class");
        if (!marker.isFile()) {
            return false;
        }
        final boolean[] fresh = {false};
        new ClassReader(DesktopResourceCompiler.read(marker)).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof Type && UNRELOCATED.equals(((Type) value).getInternalName())) {
                            fresh[0] = true;
                        }
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return !fresh[0];
    }

    /// Compiles every document. Answers the errors, each as
    /// `file:line:column: message`; an empty list means the documents'
    /// classes are in place, or that there was nothing to do.
    public List<String> run() throws IOException {
        compiled = 0;
        Map<String, File> files = DesktopResourceCompiler.documents(resourceDirs);
        File packageDir = new File(classesOut, FxmlCompiler.PACKAGE.replace('.', '/'));
        File sourceDir = sourcesOut == null ? null : new File(sourcesOut, FxmlCompiler.PACKAGE.replace('.', '/'));
        if (files.isEmpty()) {
            DesktopResourceCompiler.deleteStale(packageDir, "Fxml_", ".class", new HashSet<String>());
            if (sourceDir != null) {
                DesktopResourceCompiler.deleteStale(sourceDir, "Fxml_", ".java", new HashSet<String>());
            }
            return Collections.emptyList();
        }
        if (relocated(classesOut)) {
            // Nothing was compiled since the last build relocated this
            // directory, so no document changed either: its classes are the
            // ones that build made.
            return Collections.emptyList();
        }
        Messages messages = new Messages();
        Map<String, FxmlDocument> documents = DesktopResourceCompiler.parse(files, messages);
        Map<String, String> sources = new LinkedHashMap<String, String>();
        List<File> modelPath = new ArrayList<File>(classDirs);
        modelPath.addAll(classpath);
        ClassModel model = new ClassModel(modelPath);
        try {
            FxmlCompiler compiler = new FxmlCompiler(model, documents, messages);
            for (FxmlDocument doc : documents.values()) {
                String source = compiler.compile(doc);
                if (source != null) {
                    sources.put(FxmlCompiler.className(doc.path), source);
                }
            }
        } finally {
            model.close();
        }
        // The classes of the build before: of a document that is gone, or
        // that no longer compiles.
        DesktopResourceCompiler.deleteStale(packageDir, "Fxml_", ".class", new HashSet<String>());
        if (sourceDir != null) {
            Set<String> written = new HashSet<String>();
            for (Map.Entry<String, String> e : sources.entrySet()) {
                FxmlDispatchGenerator.writeIfDifferent(new File(sourceDir, e.getKey() + ".java"),
                        e.getValue().getBytes(StandardCharsets.UTF_8));
                written.add(e.getKey() + ".java");
            }
            DesktopResourceCompiler.deleteStale(sourceDir, "Fxml_", ".java", written);
        }
        if (messages.hasErrors()) {
            return messages.errors();
        }
        List<String> errors = javac(sources, sourceDir);
        if (errors.isEmpty()) {
            compiled = sources.size();
            log.info("Compiled " + compiled + " FXML document" + (compiled == 1 ? "" : "s"));
        }
        return errors;
    }

    /// A generated source, handed to javac from memory under the name it
    /// has (or would have) on disk.
    private static final class Source extends SimpleJavaFileObject {
        private final String text;

        Source(URI uri, String text) {
            super(uri, Kind.SOURCE);
            this.text = text;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

    private List<String> javac(Map<String, String> sources, File sourceDir) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return Collections.singletonList("The FXML documents cannot be compiled: the build is running on a Java"
                    + " runtime without a compiler. Run it on a JDK.");
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, Locale.ROOT,
                StandardCharsets.UTF_8);
        try {
            if (!classesOut.isDirectory() && !classesOut.mkdirs() && !classesOut.isDirectory()) {
                throw new IOException("Could not create " + classesOut);
            }
            fm.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(classesOut));
            List<File> cp = new ArrayList<File>();
            for (File dir : classDirs) {
                if (dir.isDirectory()) {
                    cp.add(dir);
                }
            }
            for (File f : classpath) {
                if (f.exists()) {
                    cp.add(f);
                }
            }
            fm.setLocation(StandardLocation.CLASS_PATH, cp);
            // Empty rather than unset: without a source path javac looks for
            // sources on the class path, and a classes directory may hold
            // some as resources.
            fm.setLocation(StandardLocation.SOURCE_PATH, Collections.<File>emptyList());
            List<JavaFileObject> units = new ArrayList<JavaFileObject>();
            for (Map.Entry<String, String> e : sources.entrySet()) {
                URI uri = sourceDir != null ? new File(sourceDir, e.getKey() + ".java").toURI()
                        : URI.create("string:///" + FxmlCompiler.PACKAGE.replace('.', '/') + "/" + e.getKey()
                                + ".java");
                units.add(new Source(uri, e.getValue()));
            }
            // -source/-target rather than --release: the build plugins still
            // run on a JDK 8. The warning newer compilers print for it is a
            // lint warning, which -Xlint:none turns off.
            List<String> options = Arrays.asList("-Xlint:none", "-proc:none", "-source", "1.8", "-target", "1.8");
            StringWriter out = new StringWriter();
            Boolean ok = compiler.getTask(out, fm, diagnostics, options, null, units).call();
            List<String> errors = new ArrayList<String>();
            if (ok == null || !ok.booleanValue()) {
                for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                    if (d.getKind() == Diagnostic.Kind.ERROR) {
                        JavaFileObject at = d.getSource();
                        String where = at == null ? "javac" : sourceDir != null ? new File(at.toUri()).getPath()
                                : at.getName();
                        errors.add(where + ":" + Math.max(d.getLineNumber(), 0) + ":"
                                + Math.max(d.getColumnNumber(), 0) + ": " + d.getMessage(Locale.ROOT)
                                + " (in the Java source generated from an FXML document; a member of a"
                                + " controller or custom control the document names is missing or has another"
                                + " type)");
                    }
                }
                if (errors.isEmpty()) {
                    errors.add("javac failed on the sources generated from the FXML documents: " + out);
                }
            }
            return errors;
        } finally {
            fm.close();
        }
    }
}
