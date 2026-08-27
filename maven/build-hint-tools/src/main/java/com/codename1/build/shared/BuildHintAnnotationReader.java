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

package com.codename1.build.shared;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the build hint annotations back out of their compiled classes.
 *
 * <p>The annotations in {@code com.codename1.annotations.buildhints} are the
 * source of truth for the hints they expose: the attribute's type IS the hint's
 * type, the enum's constants ARE its value domain, and {@code @Hint} carries the
 * rest. Nothing restates any of that, so nothing can disagree with it.</p>
 *
 * <p>From the CLASS files rather than the sources. {@code @Hint} has CLASS
 * retention, so every value is in the class file as a
 * {@code RuntimeInvisibleAnnotations} attribute, and reading it is a well-defined
 * operation rather than another hand-rolled Java parser.</p>
 */
public final class BuildHintAnnotationReader {

    private static final int API = Opcodes.ASM9;
    private static final String HINT = "Lcom/codename1/annotations/buildhints/Hint;";
    private static final String HINT_VALUE = "Lcom/codename1/annotations/buildhints/HintValue;";
    private static final String HINT_UNSET = "Lcom/codename1/annotations/buildhints/HintUnset;";
    /// The enum standing in for a boolean, so the hint keeps a third state.
    private static final String TOGGLE = "Toggle";

    private BuildHintAnnotationReader() {
    }

    /**
     * Every hint the annotation SOURCES in {@code srcDir} declare.
     *
     * <p>Compiles them to a temporary directory first. That keeps this
     * independent of whether the core has been built: the doc build, the drift
     * gate and the tests all run it, and none of them should have to build a
     * framework to render a table. Eighteen small files compile in well under a
     * second.</p>
     */
    public static List<BuildHints.Hint> readFromSources(File srcDir) throws IOException {
        File[] files = srcDir.listFiles();
        if (files == null) {
            throw new IOException("Not a directory: " + srcDir);
        }
        List<String> args = new ArrayList<String>();
        File out = java.nio.file.Files.createTempDirectory("cn1-buildhints").toFile();
        args.add("-d");
        args.add(out.getAbsolutePath());
        for (File f : files) {
            if (f.getName().endsWith(".java")) {
                args.add(f.getAbsolutePath());
            }
        }
        javax.tools.JavaCompiler javac = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            throw new IOException("No Java compiler available; run this on a JDK");
        }
        if (javac.run(null, null, null, args.toArray(new String[args.size()])) != 0) {
            throw new IOException("The build hint annotations under " + srcDir
                    + " do not compile");
        }
        try {
            List<BuildHints.Hint> hints =
                    read(new File(out, "com/codename1/annotations/buildhints"));
            Map<String, String> docs = docComments(files);
            for (BuildHints.Hint h : hints) {
                String doc = docs.get(h.group().annotationSimpleName() + "#" + h.attr());
                if (doc != null) {
                    h.doc(doc);
                }
            }
            return hints;
        } finally {
            deleteTree(out);
        }
    }

    /**
     * The `///` documentation above each attribute, keyed `Annotation#attribute`.
     *
     * <p>Read from the source text rather than through
     * {@code Elements.getDocComment}, which would be the obvious route and does
     * not work here: these are JEP 467 markdown doc comments, and to the JDK 8
     * javac that builds the core they are ordinary line comments carrying no
     * documentation at all.</p>
     *
     * <p>The prose lives in the javadoc and nowhere else, so an IDE shows it on
     * completion -- which is the whole point of a checked annotation. Reading it
     * back for the guide's table is this method's job. Nothing here parses Java:
     * it collects the run of `///` lines that precedes a declaration, and the
     * round trip test fails if any attribute comes back without one.</p>
     */
    private static Map<String, String> docComments(File[] files) throws IOException {
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (File f : files) {
            if (!f.getName().endsWith(".java")) {
                continue;
            }
            String simple = f.getName().substring(0, f.getName().length() - ".java".length());
            List<String> pending = new ArrayList<String>();
            java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new FileInputStream(f), "UTF-8"));
            try {
                String line;
                int insideAnnotation = 0;
                while ((line = r.readLine()) != null) {
                    String trimmed = line.trim();
                    if (insideAnnotation > 0) {
                        insideAnnotation += balance(trimmed);
                        continue;
                    }
                    if (trimmed.startsWith("///")) {
                        pending.add(trimmed.length() > 3 ? trimmed.substring(3).trim() : "");
                        continue;
                    }
                    if (trimmed.startsWith("@")) {
                        insideAnnotation = balance(trimmed);
                        continue;
                    }
                    String attribute = attributeDeclaredBy(trimmed);
                    if (attribute != null) {
                        if (!pending.isEmpty()) {
                            out.put(simple + "#" + attribute, join(pending));
                        }
                        pending.clear();
                    } else if (trimmed.length() > 0) {
                        // Anything else -- `public @interface Android {` above
                        // all -- means the comment was documenting that, not an
                        // attribute. Letting it through attached the type's own
                        // javadoc to whichever attribute came first.
                        pending.clear();
                    }
                }
            } finally {
                r.close();
            }
        }
        return out;
    }

    /// The attribute an annotation-member declaration declares, or null.
    private static String attributeDeclaredBy(String trimmed) {
        int parens = trimmed.indexOf("()");
        if (parens <= 0 || !trimmed.endsWith(";")) {
            return null;
        }
        String head = trimmed.substring(0, parens);
        int space = head.lastIndexOf(' ');
        if (space < 0) {
            return null;
        }
        String name = head.substring(space + 1);
        return name.length() > 0 && Character.isJavaIdentifierStart(name.charAt(0))
                ? name : null;
    }

    /// How far this line opens or closes a parenthesised annotation.
    private static int balance(String trimmed) {
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
        }
        return depth;
    }

    private static String join(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (sb.length() > 0) {
                sb.append(line.length() == 0 ? "\n" : " ");
            }
            sb.append(line);
        }
        return sb.toString().trim();
    }

    private static void deleteTree(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteTree(child);
            }
        }
        if (!f.delete()) {
            f.deleteOnExit();
        }
    }

    /**
     * Every hint the annotations in {@code classesDir} declare.
     *
     * @param classesDir the compiled {@code com/codename1/annotations/buildhints}
     *                   package
     */
    public static List<BuildHints.Hint> read(File classesDir) throws IOException {
        Map<String, EnumDomain> enums = new LinkedHashMap<String, EnumDomain>();
        List<Annotation> groups = new ArrayList<Annotation>();
        File[] files = classesDir.listFiles();
        if (files == null) {
            throw new IOException("Not a directory: " + classesDir);
        }
        for (File f : files) {
            if (!f.getName().endsWith(".class")) {
                continue;
            }
            Scanned scanned = scan(f);
            if (scanned.enumDomain != null) {
                enums.put(scanned.simpleName, scanned.enumDomain);
            } else if (scanned.annotation != null) {
                groups.add(scanned.annotation);
            }
        }
        List<BuildHints.Hint> out = new ArrayList<BuildHints.Hint>();
        for (Annotation group : groups) {
            for (Attribute a : group.attributes) {
                out.add(a.toHint(group.simpleName, group.defaults, enums));
            }
        }
        Collections.sort(out, new Comparator<BuildHints.Hint>() {
            public int compare(BuildHints.Hint a, BuildHints.Hint b) {
                return a.name().compareTo(b.name());
            }
        });
        return out;
    }

    private static Scanned scan(File classFile) throws IOException {
        String name = classFile.getName();
        InputStream in = new FileInputStream(classFile);
        try {
            return scan(name.substring(0, name.length() - ".class".length()), in);
        } finally {
            in.close();
        }
    }

    private static Scanned scan(String simpleName, InputStream in) throws IOException {
        final Scanned scanned = new Scanned();
        scanned.simpleName = simpleName;
        {
            new ClassReader(in).accept(new ClassVisitor(API) {
                private boolean isAnnotation;
                private boolean isEnum;

                @Override
                public void visit(int version, int access, String name, String signature,
                                  String superName, String[] interfaces) {
                    isAnnotation = (access & Opcodes.ACC_ANNOTATION) != 0;
                    isEnum = (access & Opcodes.ACC_ENUM) != 0;
                    if (isEnum) {
                        scanned.enumDomain = new EnumDomain();
                    } else if (isAnnotation && groupNamed(scanned.simpleName) != null) {
                        // @Hint and @HintValue live in the same package and are
                        // annotations too, but they describe hints rather than
                        // being one.
                        scanned.annotation = new Annotation(scanned.simpleName);
                    } else {
                        isAnnotation = false;
                    }
                }

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    // The @Hint on the TYPE supplies the defaults for every
                    // attribute in it. Every hint in @Android is an Android hint
                    // read by AndroidGradleBuilder, and saying so on each of the
                    // twenty-four was noise that could also be got wrong.
                    if (!isAnnotation || !HINT.equals(desc) || scanned.annotation == null) {
                        return null;
                    }
                    final Attribute defaults = scanned.annotation.defaults;
                    return new AnnotationVisitor(API) {
                        @Override
                        public void visit(String member, Object v) {
                            defaults.values.put(member, String.valueOf(v));
                        }

                        @Override
                        public AnnotationVisitor visitArray(final String member) {
                            final List<String> items = new ArrayList<String>();
                            defaults.arrays.put(member, items);
                            return new AnnotationVisitor(API) {
                                @Override
                                public void visit(String ignored, Object v) {
                                    items.add(String.valueOf(v));
                                }
                            };
                        }
                    };
                }

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor,
                                               String signature, Object value) {
                    if (!isEnum || (access & Opcodes.ACC_ENUM) == 0) {
                        return null;
                    }
                    final String constant = name;
                    return new FieldVisitor(API) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            if (HINT_UNSET.equals(desc)) {
                                scanned.enumDomain.unset = constant;
                                return null;
                            }
                            if (!HINT_VALUE.equals(desc)) {
                                return null;
                            }
                            return new AnnotationVisitor(API) {
                                @Override
                                public void visit(String member, Object v) {
                                    if ("value".equals(member)) {
                                        scanned.enumDomain.wire.put(constant, String.valueOf(v));
                                    } else if ("label".equals(member)) {
                                        scanned.enumDomain.labels.put(constant, String.valueOf(v));
                                    }
                                }

                                @Override
                                public AnnotationVisitor visitArray(String member) {
                                    if (!"accepts".equals(member)) {
                                        return null;
                                    }
                                    return new AnnotationVisitor(API) {
                                        @Override
                                        public void visit(String ignored, Object v) {
                                            scanned.enumDomain.accepts.put(String.valueOf(v),
                                                    constant);
                                        }
                                    };
                                }
                            };
                        }
                    };
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    if (!isAnnotation || (access & Opcodes.ACC_ABSTRACT) == 0) {
                        return null;
                    }
                    final Attribute attribute = new Attribute(name, Type.getReturnType(descriptor));
                    scanned.annotation.attributes.add(attribute);
                    return new MethodVisitor(API) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            if (!HINT.equals(desc)) {
                                return null;
                            }
                            return new AnnotationVisitor(API) {
                                @Override
                                public void visit(String member, Object v) {
                                    attribute.values.put(member, String.valueOf(v));
                                }

                                @Override
                                public void visitEnum(String member, String desc, String value) {
                                    attribute.values.put(member, value);
                                }

                                @Override
                                public AnnotationVisitor visitArray(final String member) {
                                    final List<String> items = new ArrayList<String>();
                                    attribute.arrays.put(member, items);
                                    return new AnnotationVisitor(API) {
                                        @Override
                                        public void visit(String ignored, Object v) {
                                            items.add(String.valueOf(v));
                                        }
                                    };
                                }
                            };
                        }
                    };
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        }
        return scanned;
    }

    /// The package the annotations live in, as a JVM path.
    private static final String PKG_PATH = "com/codename1/annotations/buildhints/";

    /// Everything the annotation package declares, read off `classpath`.
    ///
    /// The package is ENUMERATED, never listed. A generated table naming each
    /// annotation was a second place the set of them existed, and adding one
    /// meant remembering to regenerate; this finds whatever is in the package,
    /// which is the only statement of what a build hint annotation is.
    ///
    /// Read as bytecode rather than through reflection because `@Hint` has CLASS
    /// retention: loading these types and asking them would return nothing.
    public static Bindings bindingsFromClasspath(List<String> classpath) throws IOException {
        Map<String, EnumDomain> enums = new LinkedHashMap<String, EnumDomain>();
        List<Annotation> groups = new ArrayList<Annotation>();
        if (classpath != null) {
            for (String element : classpath) {
                if (element == null || element.length() == 0) {
                    continue;
                }
                scanElement(new File(element), enums, groups);
            }
        }
        return new Bindings(groups, enums);
    }

    /// A binding that knows nothing, for a classpath that could not be read.
    public static Bindings emptyBindings() {
        return new Bindings(new ArrayList<Annotation>(), new LinkedHashMap<String, EnumDomain>());
    }

    private static void scanElement(File element, Map<String, EnumDomain> enums,
                                    List<Annotation> groups) throws IOException {
        if (element.isDirectory()) {
            File dir = new File(element, PKG_PATH);
            File[] files = dir.listFiles();
            if (files == null) {
                return;
            }
            for (File f : files) {
                if (f.getName().endsWith(".class")) {
                    collect(scan(f), enums, groups);
                }
            }
            return;
        }
        if (!element.isFile() || !element.getName().endsWith(".jar")) {
            return;
        }
        java.util.zip.ZipFile zip = new java.util.zip.ZipFile(element);
        try {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(PKG_PATH) || !name.endsWith(".class")
                        || name.indexOf('/', PKG_PATH.length()) >= 0) {
                    continue;
                }
                String simple = name.substring(PKG_PATH.length(),
                        name.length() - ".class".length());
                InputStream in = zip.getInputStream(entry);
                try {
                    collect(scan(simple, in), enums, groups);
                } finally {
                    in.close();
                }
            }
        } finally {
            zip.close();
        }
    }

    private static void collect(Scanned scanned, Map<String, EnumDomain> enums,
                                List<Annotation> groups) {
        if (scanned.enumDomain != null) {
            enums.put(scanned.simpleName, scanned.enumDomain);
        } else if (scanned.annotation != null) {
            groups.add(scanned.annotation);
        }
    }

    /// What an annotation member sets and what an enum constant sends, read from
    /// the annotation classes rather than from a table generated beside them.
    public static final class Bindings {
        private final Map<String, String> hints = new LinkedHashMap<String, String>();
        private final Map<String, String> wire = new LinkedHashMap<String, String>();
        private final java.util.Set<String> unset = new java.util.HashSet<String>();
        private final List<String> descriptors = new ArrayList<String>();

        Bindings(List<Annotation> groups, Map<String, EnumDomain> enums) {
            for (Annotation group : groups) {
                String descriptor = "L" + PKG_PATH + group.simpleName + ";";
                descriptors.add(descriptor);
                for (Attribute a : group.attributes) {
                    // toHint resolves the group and throws when the annotation
                    // names none, which is the loud failure for an annotation type
                    // that HintGroup does not know about.
                    BuildHints.Hint h = a.toHint(group.simpleName, group.defaults, enums);
                    hints.put(descriptor + "#" + a.name, h.name());
                }
            }
            for (Map.Entry<String, EnumDomain> e : enums.entrySet()) {
                for (Map.Entry<String, String> v : e.getValue().wire.entrySet()) {
                    wire.put(e.getKey() + "#" + v.getKey(), v.getValue());
                }
                if (e.getValue().unset != null) {
                    unset.add(e.getKey() + "#" + e.getValue().unset);
                }
            }
        }

        /// Every build hint annotation descriptor, in JVM internal form.
        public java.util.Collection<String> descriptors() {
            return Collections.unmodifiableList(descriptors);
        }

        /// The hint an annotation member sets, or null when the pair is not one.
        public String hintFor(String descriptor, String member) {
            return hints.get(descriptor + "#" + member);
        }

        /// The value the build receives for an enum constant, or null.
        public String wireValue(String enumDescriptorOrName, String constant) {
            return wire.get(simpleNameOf(enumDescriptorOrName) + "#" + constant);
        }

        /// Whether a constant means the developer said nothing, so the hint is
        /// not written at all and the build server applies its own default.
        public boolean isUnset(String enumDescriptorOrName, String constant) {
            return unset.contains(simpleNameOf(enumDescriptorOrName) + "#" + constant);
        }

        private static String simpleNameOf(String enumDescriptorOrName) {
            String simple = enumDescriptorOrName;
            int slash = simple.lastIndexOf('/');
            if (slash >= 0) {
                simple = simple.substring(slash + 1);
            }
            if (simple.endsWith(";")) {
                simple = simple.substring(0, simple.length() - 1);
            }
            return simple;
        }
    }

    /// The group `@Name` exposes, or null when the annotation is not one.
    private static HintGroup groupNamed(String annotationSimpleName) {
        for (HintGroup g : HintGroup.values()) {
            if (annotationSimpleName.equals(g.annotationSimpleName())) {
                return g;
            }
        }
        return null;
    }

    private static final class Scanned {
        private String simpleName;
        private Annotation annotation;
        private EnumDomain enumDomain;
    }

    private static final class Annotation {
        private final String simpleName;
        private final List<Attribute> attributes = new ArrayList<Attribute>();
        /// The type-level @Hint, whose members are the defaults for every
        /// attribute that does not state its own.
        private final Attribute defaults = new Attribute("", Type.VOID_TYPE);

        Annotation(String simpleName) {
            this.simpleName = simpleName;
        }
    }

    private static final class EnumDomain {
        private final Map<String, String> wire = new LinkedHashMap<String, String>();
        private final Map<String, String> labels = new LinkedHashMap<String, String>();
        /// alternative spelling -> the constant it means
        private final Map<String, String> accepts = new LinkedHashMap<String, String>();
        /// The constant marked @HintUnset, which sends nothing.
        private String unset;
    }

    private static final class Attribute {
        private final String name;
        private final Type type;
        private final Map<String, String> values = new LinkedHashMap<String, String>();
        private final Map<String, List<String>> arrays =
                new LinkedHashMap<String, List<String>>();

        Attribute(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        private String value(String member, String fallback) {
            String v = values.get(member);
            return v == null ? fallback : v;
        }

        BuildHints.Hint toHint(String groupSimpleName, Attribute groupDefaults,
                               Map<String, EnumDomain> enums) {
            HintGroup group = groupOf(groupSimpleName);
            String prefix = group.keyPrefix() == null ? "" : group.keyPrefix();
            String hintName = value("name", prefix + name);
            BuildHints.Hint hint = new BuildHints.Hint(hintName).annotatedAs(group, name);
            String separator = value("separator", "");
            if ("true".equals(value("appendable", "false"))) {
                hint.separator(separator);
            }
            applyType(hint, enums, separator);
            String valuePattern = value("valuePattern", "");
            if (valuePattern.length() > 0) {
                hint.valuePattern(valuePattern);
            }
            hint.platform(value("platform", groupDefaults.value("platform", "general")));
            if (values.containsKey("aliasOf")) {
                hint.aliasOf(values.get("aliasOf"));
            }
            if (values.containsKey("deprecated")) {
                hint.deprecated(values.get("deprecated"));
            }
            if ("true".equals(value("external", "false"))) {
                hint.external();
            }
            if ("true".equals(value("enterpriseOnly", "false"))) {
                hint.enterpriseOnly();
            }
            String link = value("link", "");
            if (link.length() > 0) {
                hint.link(link);
            }
            return hint.doc(value("doc", ""));
        }

        /// The hint's type IS the attribute's Java type -- that is the point of
        /// the whole arrangement, so nothing restates it.
        private void applyType(BuildHints.Hint hint, Map<String, EnumDomain> enums,
                               String separator) {
            if (type.getSort() == Type.BOOLEAN) {
                hint.type(HintType.BOOLEAN);
                return;
            }
            if (type.getSort() == Type.INT) {
                hint.type(HintType.INT);
                return;
            }
            if (type.getSort() == Type.ARRAY) {
                hint.type(HintType.STRING_LIST);
                if (separator.length() == 0) {
                    throw new IllegalStateException("Build hint " + hint.name()
                            + " is a list but declares no @Hint(separator)");
                }
                return;
            }
            String simple = simpleNameOf(type);
            EnumDomain domain = enums.get(simple);
            if (domain == null) {
                hint.type(kindType(value("kind", "DEFAULT")));
                return;
            }
            // A shared enum, narrowed to what THIS hint accepts. ThemeMode is one
            // type for every hint that picks a theme -- three near-identical
            // enums were three things to keep in step -- and the attribute's
            // valuePattern says which of its constants the hint really takes.
            // Recording the whole union instead would offer `ios7` in the Android
            // editor and document it as a legal Android value.
            String accepted = value("valuePattern", "");
            List<String> wire = new ArrayList<String>();
            List<String> constants = new ArrayList<String>();
            for (Map.Entry<String, String> e : domain.wire.entrySet()) {
                if (accepted.length() > 0 && !e.getValue().matches(accepted)) {
                    continue;
                }
                constants.add(e.getKey());
                wire.add(e.getValue());
            }
            hint.values(simple, wire.toArray(new String[wire.size()]));
            hint.valueConstants(constants.toArray(new String[constants.size()]));
            if (domain.unset != null) {
                hint.unsetConstant(domain.unset);
            }
            // Toggle is how a hint that the build reads as true/false keeps a
            // third state. It stays a BOOLEAN hint -- that is what the editor
            // renders and what the documentation says -- while the binding still
            // learns that ON sends "true", which upper-casing the value could
            // never tell it.
            if (TOGGLE.equals(simple)) {
                hint.type(HintType.BOOLEAN);
                return;
            }
            if (!domain.accepts.isEmpty()) {
                List<String> pairs = new ArrayList<String>();
                for (Map.Entry<String, String> e : domain.accepts.entrySet()) {
                    String target = domain.wire.get(e.getValue());
                    // An alias for a value this hint does not accept is not an
                    // alias this hint has.
                    if (target == null || !wire.contains(target)) {
                        continue;
                    }
                    pairs.add(e.getKey());
                    pairs.add(target);
                }
                hint.valueAliases(pairs.toArray(new String[pairs.size()]));
            }
            if (!domain.labels.isEmpty()) {
                List<String> labels = new ArrayList<String>();
                for (String constant : constants) {
                    String label = domain.labels.get(constant);
                    labels.add(label == null ? "" : label);
                }
                hint.valueLabels(labels.toArray(new String[labels.size()]));
            }
        }

        /// What kind of string this is. The Java type says `String` for a
        /// version, a path, a secret and an XML fragment alike, and the editor
        /// picks a different control for each.
        private static HintType kindType(String kind) {
            if ("DEFAULT".equals(kind)) {
                return HintType.STRING;
            }
            return HintType.valueOf(kind);
        }

        private static String simpleNameOf(Type type) {
            String internal = type.getInternalName();
            int slash = internal.lastIndexOf('/');
            return slash < 0 ? internal : internal.substring(slash + 1);
        }

        private static HintGroup groupOf(String annotationSimpleName) {
            HintGroup g = groupNamed(annotationSimpleName);
            if (g == null) {
                throw new IllegalStateException("No hint group is exposed as @"
                        + annotationSimpleName);
            }
            return g;
        }
    }
}
