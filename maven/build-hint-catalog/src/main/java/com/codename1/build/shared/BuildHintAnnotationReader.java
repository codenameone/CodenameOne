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
            return read(new File(out, "com/codename1/annotations/buildhints"));
        } finally {
            deleteTree(out);
        }
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
                out.add(a.toHint(group.simpleName, enums));
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
        final Scanned scanned = new Scanned();
        scanned.simpleName = classFile.getName().substring(0,
                classFile.getName().length() - ".class".length());
        InputStream in = new FileInputStream(classFile);
        try {
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
                public FieldVisitor visitField(int access, String name, String descriptor,
                                               String signature, Object value) {
                    if (!isEnum || (access & Opcodes.ACC_ENUM) == 0) {
                        return null;
                    }
                    final String constant = name;
                    return new FieldVisitor(API) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
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
        } finally {
            in.close();
        }
        return scanned;
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

        Annotation(String simpleName) {
            this.simpleName = simpleName;
        }
    }

    private static final class EnumDomain {
        private final Map<String, String> wire = new LinkedHashMap<String, String>();
        private final Map<String, String> labels = new LinkedHashMap<String, String>();
        /// alternative spelling -> the constant it means
        private final Map<String, String> accepts = new LinkedHashMap<String, String>();
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

        BuildHints.Hint toHint(String groupSimpleName, Map<String, EnumDomain> enums) {
            HintGroup group = groupOf(groupSimpleName);
            String prefix = group.keyPrefix() == null ? "" : group.keyPrefix();
            String hintName = value("name", prefix + name);
            BuildHints.Hint hint = new BuildHints.Hint(hintName).annotatedAs(group, name);
            String separator = value("separator", "");
            if ("true".equals(value("appendable", "false"))) {
                hint.separator(separator);
            }
            applyType(hint, enums, separator);
            String def = value("def", "");
            if (def.length() > 0) {
                hint.def(def);
            }
            hint.platform(value("platform", "general"));
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
            List<String> by = arrays.get("consumedBy");
            if (by != null && !by.isEmpty()) {
                hint.consumedBy(by.toArray(new String[by.size()]));
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
            List<String> wire = new ArrayList<String>(domain.wire.values());
            hint.values(simple, wire.toArray(new String[wire.size()]));
            if (!domain.accepts.isEmpty()) {
                List<String> pairs = new ArrayList<String>();
                for (Map.Entry<String, String> e : domain.accepts.entrySet()) {
                    pairs.add(e.getKey());
                    pairs.add(domain.wire.get(e.getValue()));
                }
                hint.valueAliases(pairs.toArray(new String[pairs.size()]));
            }
            if (!domain.labels.isEmpty()) {
                List<String> labels = new ArrayList<String>();
                for (String constant : domain.wire.keySet()) {
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
