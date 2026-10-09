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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Says what each dependency jar of a desktop application is to a device
/// build, from the classes inside it.
///
/// One answer, used twice: `cn1:import-desktop-project` prints it so a
/// developer knows what becomes of each library before building anything,
/// and the remap step ([CompatLibraries]) acts on it. Nothing here knows a
/// library by its name. A jar is read, and what its classes are called and
/// what they refer to decides its [Kind].
///
/// #### Why the classes that ship are a closure
///
/// A device has no class path. Every class of the application is translated
/// or dexed into it, so a library class nothing refers to is dead weight,
/// and one that is referred to has to be relocated with the application --
/// its descriptors name `java.io.File`, which the application's relocated
/// calls now spell `com.codename1.compat.jdk.File` -- and checked like the
/// application's own code. [#reach] therefore walks from the application's
/// classes through the libraries, and only what it reaches is unpacked.
/// Classes found only by reflection or a service loader are not reached:
/// a device has neither. A class named by a string constant
/// (`Class.forName("a.b.C")`) is.
public final class DependencyClassifier {

    /// What a dependency jar is to a device build.
    public enum Kind {
        /// Its classes live in packages a compatibility layer implements
        /// itself. The layer's classes ship; the jar never does.
        LAYER_PROVIDED("provided by a layer"),
        /// Written against Swing or JavaFX in substance. Shipped whole and
        /// relocated, since a toolkit library finds its own classes by name.
        UI_LIBRARY("UI library"),
        /// A pure-Java library with a few classes that name a desktop
        /// toolkit. Only the classes the application reaches ship.
        INCIDENTAL_TOOLKIT("pure Java, incidental toolkit use"),
        /// Names no desktop toolkit. The classes the application reaches
        /// ship, relocated for the JDK classes a device lacks, and checked.
        PURE_JAVA("pure Java"),
        /// Something the Codename One build handles on its own: a library
        /// written against the Codename One API, or the Kotlin runtime.
        PLATFORM("Codename One"),
        /// A jar of resources.
        NO_CLASSES("resources only");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        /// What a report calls it.
        public String label() {
            return label;
        }
    }

    /// The share of a jar's classes, in percent, that must name a desktop
    /// toolkit for the jar to be a library written against it. Measured on
    /// the 56 jars a real Swing application ships with: the toolkit
    /// libraries sit at 25% and above (FlatLaf 83, RSyntaxTextArea 80, an
    /// icon font's Swing binding 100, the ImageIO plugins 25 to 76), the
    /// pure-Java ones with a stray `java.beans` or `java.awt` reference at
    /// 6% and below (jackson-databind and commons-lang3 one class each, JNA
    /// 2%). Nothing fell between.
    static final int UI_SHARE_PERCENT = 20;

    /// Packages the Codename One build ships a runtime for by itself.
    private static final String[] PLATFORM_PACKAGES = {"kotlin/", "kotlinx/"};

    private static final String CN1_PACKAGE = "com/codename1/";

    private DependencyClassifier() {
    }

    /// One class of a library: what it refers to.
    static final class ClassInfo {
        final String name;
        final Set<String> references = new HashSet<String>();
        /// String constants shaped like a class name, as written (`a.b.C`).
        final Set<String> classNames = new HashSet<String>();

        ClassInfo(String name) {
            this.name = name;
        }
    }

    /// A dependency jar, classified.
    public static final class Library {
        private final File file;
        private final Map<String, ClassInfo> classes = new LinkedHashMap<String, ClassInfo>();
        private final Set<String> reached = new TreeSet<String>();
        private Kind kind = Kind.NO_CLASSES;
        private int toolkitClasses;
        private int layerClasses;
        private String layer;
        private String platform;
        private String nativeCode;
        private boolean walked;

        Library(File file) {
            this.file = file;
        }

        /// The jar.
        public File file() {
            return file;
        }

        /// The jar's file name, which is what a build calls the library.
        public String jar() {
            return file.getName();
        }

        public Kind kind() {
            return kind;
        }

        /// How many classes the jar holds.
        public int classCount() {
            return classes.size();
        }

        /// How many of them name a desktop toolkit.
        public int toolkitClassCount() {
            return toolkitClasses;
        }

        /// The layer that implements the jar's packages, for
        /// [Kind#LAYER_PROVIDED]; else null.
        public String layer() {
            return layer;
        }

        /// What handles a [Kind#PLATFORM] jar; else null.
        public String platform() {
            return platform;
        }

        /// How the jar depends on native code -- "ships native libraries",
        /// "loads native code", "calls native code through JNA" -- or null.
        /// Such code cannot run on a device whatever else the jar does.
        public String nativeCode() {
            return nativeCode;
        }

        /// The internal names of the jar's classes that [#reach] found the
        /// application to use. Empty until it has run.
        public Set<String> reached() {
            return reached;
        }

        /// The internal names of the classes that ship with the
        /// application: every class of a toolkit library that is used at
        /// all, the reached ones of a pure-Java library, none of a jar a
        /// layer or the platform stands in for.
        public Set<String> shipped() {
            if (kind == Kind.UI_LIBRARY && !reached.isEmpty()) {
                return new TreeSet<String>(classes.keySet());
            }
            if (kind == Kind.INCIDENTAL_TOOLKIT || kind == Kind.PURE_JAVA) {
                return reached;
            }
            return new TreeSet<String>();
        }

        /// Whether the build unpacks the jar, or part of it, into the
        /// application ([CompatLibraries]), rather than leaving it the
        /// dependency it was.
        public boolean managed() {
            return kind != Kind.PLATFORM && kind != Kind.NO_CLASSES;
        }

        Set<String> classNames() {
            return classes.keySet();
        }

        /// The package most of the jar's classes are in, dotted and at most
        /// three parts deep (`com.fasterxml.jackson`); null for a jar
        /// without classes. It is what names the library to a table of
        /// advice, which a jar's file name does not do reliably.
        public String mainPackage() {
            Map<String, Integer> counts = new java.util.TreeMap<String, Integer>();
            for (String c : classes.keySet()) {
                int slash = c.lastIndexOf('/');
                if (slash < 0) {
                    continue;
                }
                String[] parts = c.substring(0, slash).split("/");
                StringBuilder name = new StringBuilder();
                for (int i = 0; i < parts.length && i < 3; i++) {
                    name.append(i == 0 ? "" : ".").append(parts[i]);
                }
                Integer n = counts.get(name.toString());
                counts.put(name.toString(), n == null ? 1 : n + 1);
            }
            String best = null;
            int most = 0;
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                if (e.getValue() > most) {
                    most = e.getValue();
                    best = e.getKey();
                }
            }
            return best;
        }
    }

    /// Classifies `jars`, in order, against the desktop `layers`. A file
    /// that is not a readable jar is left out.
    public static List<Library> classify(Collection<File> jars, List<Relocation> layers) throws IOException {
        List<Library> out = new ArrayList<Library>();
        Set<String> seen = new HashSet<String>();
        if (jars != null) {
            for (File jar : jars) {
                if (jar != null && jar.isFile() && jar.getName().endsWith(".jar") && seen.add(jar.getName())) {
                    out.add(read(jar, layers));
                }
            }
        }
        return out;
    }

    private static Library read(File jar, List<Relocation> layers) throws IOException {
        Library lib = new Library(jar);
        int nativeFiles = 0;
        boolean cn1 = false;
        boolean jna = false;
        boolean loads = false;
        int platformClasses = 0;
        int ownClasses = 0;
        ZipFile zip = new ZipFile(jar);
        try {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                if (e.isDirectory()) {
                    continue;
                }
                if (isNativeLibrary(name)) {
                    nativeFiles++;
                    continue;
                }
                if (!name.endsWith(".class") || name.startsWith("META-INF/") || name.endsWith("module-info.class")) {
                    continue;
                }
                Scan scan = new Scan();
                InputStream in = zip.getInputStream(e);
                try {
                    scan.read(readAll(in));
                } catch (RuntimeException unreadable) {
                    // A class file this build cannot read names nothing that
                    // can be relocated; if the application reaches it, the
                    // compliance check says so.
                    continue;
                } finally {
                    in.close();
                }
                if (scan.info == null) {
                    continue;
                }
                lib.classes.put(scan.info.name, scan.info);
                loads |= scan.loadsNative;
            }
        } finally {
            zip.close();
        }
        for (ClassInfo c : lib.classes.values()) {
            Relocation owner = owning(c.name, layers);
            if (owner != null) {
                lib.layerClasses++;
                lib.layer = owner.name();
            }
            for (String p : PLATFORM_PACKAGES) {
                if (c.name.startsWith(p)) {
                    platformClasses++;
                }
            }
            if (c.name.startsWith(CN1_PACKAGE)) {
                ownClasses++;
            }
            boolean toolkit = false;
            for (String ref : c.references) {
                if (ref.startsWith(CN1_PACKAGE) && !lib.classes.containsKey(ref)) {
                    cn1 = true;
                }
                if (ref.startsWith("com/sun/jna/") && !lib.classes.containsKey(ref)) {
                    jna = true;
                }
                if (!toolkit && !lib.classes.containsKey(ref) && owning(ref, layers) != null) {
                    toolkit = true;
                }
            }
            if (toolkit) {
                lib.toolkitClasses++;
            }
        }
        if (nativeFiles > 0) {
            lib.nativeCode = "ships native libraries";
        } else if (loads) {
            lib.nativeCode = "loads native code";
        } else if (jna) {
            lib.nativeCode = "calls native code through JNA";
        }
        int total = lib.classes.size();
        if (total == 0) {
            lib.kind = Kind.NO_CLASSES;
        } else if (lib.layerClasses * 2 > total) {
            lib.kind = Kind.LAYER_PROVIDED;
        } else if (platformClasses * 2 > total) {
            lib.kind = Kind.PLATFORM;
            lib.platform = "Kotlin runtime";
        } else if (ownClasses * 2 > total) {
            lib.kind = Kind.PLATFORM;
            lib.platform = "Codename One";
        } else if (cn1 && lib.toolkitClasses == 0) {
            lib.kind = Kind.PLATFORM;
            lib.platform = "Codename One library";
        } else if (lib.toolkitClasses * 100 >= UI_SHARE_PERCENT * total) {
            lib.kind = Kind.UI_LIBRARY;
        } else if (lib.toolkitClasses > 0) {
            lib.kind = Kind.INCIDENTAL_TOOLKIT;
        } else {
            lib.kind = Kind.PURE_JAVA;
        }
        if (lib.kind != Kind.LAYER_PROVIDED) {
            lib.layer = null;
        }
        return lib;
    }

    private static boolean isNativeLibrary(String name) {
        return name.endsWith(".so") || name.endsWith(".dll") || name.endsWith(".dylib") || name.endsWith(".jnilib");
    }

    private static Relocation owning(String internalName, List<Relocation> layers) {
        for (Relocation r : layers) {
            if (r.owns(internalName)) {
                return r;
            }
        }
        return null;
    }

    /// Marks, in each of `libraries`, the classes reachable from
    /// `references` -- internal names, and class names as a string constant
    /// spells them -- and answers how many classes that is.
    ///
    /// A jar found earlier in the list wins a class two of them hold, as on
    /// a class path. A toolkit library that is reached at all ships whole,
    /// so every class of it is walked. A jar a layer stands in for is not
    /// walked: its classes do not ship, and what they refer to is not the
    /// application's concern.
    public static int reach(Collection<String> references, List<Library> libraries) {
        Map<String, Library> owner = new HashMap<String, Library>();
        for (Library lib : libraries) {
            for (String c : lib.classNames()) {
                if (!owner.containsKey(c)) {
                    owner.put(c, lib);
                }
            }
        }
        Deque<String> queue = new ArrayDeque<String>();
        for (String r : references) {
            queue.add(r.indexOf('.') >= 0 ? r.replace('.', '/') : r);
        }
        int count = 0;
        while (!queue.isEmpty()) {
            String name = queue.removeFirst();
            Library lib = owner.get(name);
            if (lib == null || !lib.reached.add(name)) {
                continue;
            }
            count++;
            if (lib.kind == Kind.LAYER_PROVIDED) {
                continue;
            }
            if (lib.kind == Kind.UI_LIBRARY && !lib.walked) {
                lib.walked = true;
                for (ClassInfo c : lib.classes.values()) {
                    enqueue(c, queue);
                }
            }
            enqueue(lib.classes.get(name), queue);
        }
        return count;
    }

    private static void enqueue(ClassInfo c, Deque<String> queue) {
        queue.addAll(c.references);
        for (String s : c.classNames) {
            queue.add(s.replace('.', '/'));
        }
    }

    /// What the classes under `classesDir` refer to: the seeds of [#reach]
    /// for a compiled application. `skip` are files, by their path inside
    /// the directory, that are not the application's own -- a library an
    /// earlier run unpacked. The runtimes an earlier run extracted are left
    /// out as well. A name an earlier run relocated into one of `layers` is
    /// answered as the application was compiled against it.
    public static Set<String> applicationReferences(File classesDir, Set<String> skip, List<Relocation> layers)
            throws IOException {
        Set<String> out = new HashSet<String>();
        collect(classesDir, "", skip, out);
        Set<String> originals = new HashSet<String>();
        for (String name : out) {
            for (Relocation r : layers) {
                String original = r.original(name);
                if (original != null) {
                    originals.add(original);
                }
            }
        }
        out.addAll(originals);
        return out;
    }

    private static void collect(File dir, String rel, Set<String> skip, Set<String> out) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String path = rel + f.getName();
            if (f.isDirectory()) {
                if (!CompatLayers.isExtractedRuntime(path + "/")) {
                    collect(f, path + "/", skip, out);
                }
            } else if (path.endsWith(".class") && !skip.contains(path)) {
                Scan scan = new Scan();
                try {
                    scan.read(Files.readAllBytes(f.toPath()));
                } catch (RuntimeException unreadable) {
                    continue;
                }
                if (scan.info != null) {
                    out.addAll(scan.info.references);
                    out.addAll(scan.info.classNames);
                }
            }
        }
    }

    private static final Pattern IMPORT = Pattern.compile("(?m)^\\s*import\\s+(?:static\\s+)?(\\w+(?:\\.\\w+)*)(\\.\\*)?\\s*;?");

    /// What the Java and Kotlin sources under `sourceDir` import: the seeds
    /// of [#reach] for a project that has not been compiled, which is all an
    /// import has. Each import is answered as written and, since nothing
    /// says whether its last part is a class or a member, without it too;
    /// `a.b.*` is answered as every class of `libraries` in that package.
    public static Set<String> sourceReferences(File sourceDir, List<Library> libraries) throws IOException {
        Set<String> imports = new TreeSet<String>();
        Set<String> packages = new TreeSet<String>();
        scanSources(sourceDir, imports, packages);
        Set<String> out = new TreeSet<String>();
        for (String i : imports) {
            String name = i.replace('.', '/');
            out.add(name);
            int slash = name.lastIndexOf('/');
            if (slash > 0) {
                out.add(name.substring(0, slash));
            }
        }
        for (Library lib : libraries) {
            for (String c : lib.classNames()) {
                int slash = c.lastIndexOf('/');
                if (slash > 0 && packages.contains(c.substring(0, slash).replace('/', '.'))) {
                    out.add(c);
                }
            }
        }
        return out;
    }

    private static void scanSources(File dir, Set<String> imports, Set<String> packages) throws IOException {
        File[] files = dir == null ? null : dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                scanSources(f, imports, packages);
            } else if (f.getName().endsWith(".java") || f.getName().endsWith(".kt")) {
                Matcher m = IMPORT.matcher(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
                while (m.find()) {
                    if (m.group(2) != null) {
                        packages.add(m.group(1));
                    }
                    imports.add(m.group(1));
                }
            }
        }
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n = in.read(buf);
        while (n >= 0) {
            out.write(buf, 0, n);
            n = in.read(buf);
        }
        return out.toByteArray();
    }

    /// Reads one class file for what it refers to at run time: the names in
    /// its supertypes, descriptors and instructions. Annotations, generic
    /// signatures and the nesting attributes are left out -- none of them is
    /// a class a device has to have for this one to work.
    private static final class Scan extends Remapper {
        private ClassInfo info;
        private boolean loadsNative;

        @Override
        public String map(String internalName) {
            if (info != null && internalName != null && !internalName.equals(info.name)) {
                info.references.add(internalName);
            }
            return internalName;
        }

        @Override
        public Object mapValue(Object value) {
            if (value instanceof String && info != null && looksLikeClassName((String) value)) {
                info.classNames.add((String) value);
            }
            return super.mapValue(value);
        }

        void read(byte[] bytes) {
            ClassReader reader = new ClassReader(bytes);
            info = new ClassInfo(reader.getClassName());
            reader.accept(new RuntimeOnly(new ClassRemapper(new ClassWriter(0), this)),
                    ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }

        /// Passes on what a class needs at run time and nothing else.
        private final class RuntimeOnly extends ClassVisitor {
            RuntimeOnly(ClassVisitor next) {
                super(Opcodes.ASM9, next);
            }

            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                              String[] interfaces) {
                super.visit(version, access, name, null, superName, interfaces);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                return null;
            }

            @Override
            public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor,
                                                         boolean visible) {
                return null;
            }

            @Override
            public void visitInnerClass(String name, String outerName, String innerName, int access) {
                // Not a reference: a class lists every class nested in it.
            }

            @Override
            public void visitOuterClass(String owner, String name, String descriptor) {
                // As above.
            }

            @Override
            public void visitNestHost(String nestHost) {
                // As above.
            }

            @Override
            public void visitNestMember(String nestMember) {
                // As above.
            }

            @Override
            public void visitPermittedSubclass(String permittedSubclass) {
                // As above.
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature,
                                           Object value) {
                final FieldVisitor next = super.visitField(access, name, descriptor, null, value);
                return new FieldVisitor(Opcodes.ASM9, next) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String d, boolean visible) {
                        return null;
                    }

                    @Override
                    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String d,
                                                                 boolean visible) {
                        return null;
                    }
                };
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                // A declared exception is only ever a declaration.
                final MethodVisitor next = super.visitMethod(access, name, descriptor, null, null);
                return new MethodVisitor(Opcodes.ASM9, next) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String d, boolean visible) {
                        return null;
                    }

                    @Override
                    public AnnotationVisitor visitAnnotationDefault() {
                        return null;
                    }

                    @Override
                    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String d,
                                                                 boolean visible) {
                        return null;
                    }

                    @Override
                    public AnnotationVisitor visitParameterAnnotation(int parameter, String d, boolean visible) {
                        return null;
                    }

                    @Override
                    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String d,
                                                                 boolean visible) {
                        return null;
                    }

                    @Override
                    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String d,
                                                                     boolean visible) {
                        return null;
                    }

                    @Override
                    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath,
                                                                          Label[] start, Label[] end, int[] index,
                                                                          String d, boolean visible) {
                        return null;
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method, String d, boolean itf) {
                        if (("java/lang/System".equals(owner) || "java/lang/Runtime".equals(owner))
                                && ("loadLibrary".equals(method) || "load".equals(method))) {
                            loadsNative = true;
                        }
                        super.visitMethodInsn(opcode, owner, method, d, itf);
                    }
                };
            }
        }
    }

    /// Whether `value` is shaped like a qualified class name: dotted
    /// identifiers and nothing else. What it names is decided by whether a
    /// library has such a class.
    static boolean looksLikeClassName(String value) {
        int length = value.length();
        if (length < 3 || length > 200 || value.indexOf('.') <= 0) {
            return false;
        }
        boolean start = true;
        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
            if (c == '.') {
                if (start) {
                    return false;
                }
                start = true;
            } else if (start ? Character.isJavaIdentifierStart(c) : Character.isJavaIdentifierPart(c)) {
                start = false;
            } else {
                return false;
            }
        }
        return !start;
    }
}
