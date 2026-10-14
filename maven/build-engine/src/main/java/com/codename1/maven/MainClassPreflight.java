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

import com.codename1.build.BuildFailureException;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.processors.BuildHintAnnotationProcessor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Checks the configured entry point without loading or executing application code. */
final class MainClassPreflight {
    private static final String LIFECYCLE = "com/codename1/system/Lifecycle";
    private static final String FIX = "Run mvn cn1:settings to set your app's main class and package, "
            + "or edit codename1.mainName and codename1.packageName in codenameone_settings.properties "
            + "to match your app's main class, then rebuild.";

    private MainClassPreflight() {
    }

    static void validate(File stagedJar, Properties settings) throws BuildFailureException {
        validate(stagedJar, settings, Collections.<File>emptyList(), null);
    }

    static void validate(File stagedJar, Properties settings, List<File> inputs,
                         AnnotationBuildHints.Sources sources) throws BuildFailureException {
        String packageName = settings.getProperty("codename1.packageName", "");
        String mainName = settings.getProperty("codename1.mainName", "");
        if (packageName.trim().isEmpty() || mainName.trim().isEmpty()) {
            throw new BuildFailureException("The app's main class is not configured: "
                    + "codename1.mainName and codename1.packageName must both be set. " + FIX);
        }
        String className = packageName + "." + mainName;
        try (JarFile jar = new JarFile(stagedJar)) {
            JarEntry entry = jar.getJarEntry(className.replace('.', '/') + ".class");
            boolean present = entry != null && !entry.isDirectory();
            if (present && hasBackingSource(jar, entry, inputs, sources)) {
                return;
            }
            String candidate = findCandidate(jar, inputs, sources);
            throw new BuildFailureException("The main class " + className
                    + " named in codenameone_settings.properties "
                    + (present ? "has no matching source; its compiled class is stale. "
                            + "Run mvn clean to remove stale output after correcting the settings. "
                            : "was not found in the compiled classes. ")
                    + FIX + (candidate == null ? "" : " Possible main class: " + candidate + "."));
        } catch (IOException ex) {
            throw new BuildFailureException("Could not check the app's main class in " + stagedJar, ex);
        }
    }

    /** Only known project outputs require source; installed binary dependencies remain valid. */
    private static boolean hasBackingSource(JarFile staged, JarEntry entry, List<File> inputs,
                                             AnnotationBuildHints.Sources sources)
            throws IOException, BuildFailureException {
        if (sources == null) {
            return true;
        }
        for (File input : inputs) {
            boolean contains;
            if (input.isDirectory()) {
                contains = new File(input, entry.getName()).isFile();
            } else if (input.isFile()) {
                try (JarFile jar = new JarFile(input)) {
                    contains = jar.getJarEntry(entry.getName()) != null;
                }
            } else {
                continue;
            }
            if (!contains) {
                continue;
            }
            List<String> roots = sources.rootsFor(input);
            if (roots == null || roots.isEmpty()) {
                return true;
            }
            try (InputStream in = staged.getInputStream(entry)) {
                AnnotatedClass cls = ClassScanner.readClass(in, input);
                // The desktop layer's remap generates the main class into the
                // classes directory and marks it with its own SourceFile; no
                // source file ever backs it, and that is not staleness.
                if (DesktopEntryPoints.GENERATED_SOURCE.equals(cls.getSourceFile())) {
                    return true;
                }
                return BuildHintAnnotationProcessor.hasBackingSource(cls,
                        roots, sources.encodingFor(input));
            } catch (ProcessingException ex) {
                throw new BuildFailureException("Could not inspect the main class source for " + entry.getName(), ex);
            }
        }
        return true;
    }

    /** Suggestions are best effort; unreadable/unsupported bytecode must not hide the fix. */
    private static String findCandidate(JarFile jar, List<File> inputs, AnnotationBuildHints.Sources sources) {
        Map<String, ClassInfo> classes = new HashMap<String, ClassInfo>();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().endsWith(".class")
                    || entry.getName().startsWith("META-INF/")) {
                continue;
            }
            try (InputStream in = jar.getInputStream(entry)) {
                ClassReader reader = new ClassReader(in);
                ClassInfo info = new ClassInfo();
                reader.accept(info, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                classes.put(reader.getClassName(), info);
            } catch (IOException | IllegalArgumentException ex) {
                // The incomplete scan cannot establish that a candidate is unique.
                return null;
            }
        }
        String candidate = null;
        for (Map.Entry<String, ClassInfo> entry : classes.entrySet()) {
            ClassInfo info = entry.getValue();
            if (!info.concretePublic || LIFECYCLE.equals(entry.getKey())) {
                continue;
            }
            Set<String> methods = new HashSet<String>();
            Set<String> visited = new HashSet<String>();
            String name = entry.getKey();
            boolean lifecycle = false;
            while (name != null && visited.add(name)) {
                if (LIFECYCLE.equals(name)) {
                    lifecycle = true;
                    break;
                }
                ClassInfo parent = classes.get(name);
                if (parent == null) {
                    break;
                }
                methods.addAll(parent.methods);
                name = parent.superName;
            }
            if (lifecycle || methods.size() == 4) {
                try {
                    if (!hasBackingSource(jar, jar.getJarEntry(entry.getKey() + ".class"), inputs, sources)) {
                        continue;
                    }
                } catch (IOException | BuildFailureException ex) {
                    return null;
                }
                if (candidate != null) {
                    return null;
                }
                candidate = entry.getKey().replace('/', '.');
            }
        }
        return candidate;
    }

    private static final class ClassInfo extends ClassVisitor {
        String superName;
        boolean concretePublic;
        final Set<String> methods = new HashSet<String>();

        ClassInfo() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.superName = superName;
            concretePublic = (access & Opcodes.ACC_PUBLIC) != 0
                    && (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) == 0;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            if ((access & Opcodes.ACC_PUBLIC) != 0 && (access & Opcodes.ACC_STATIC) == 0
                    && (("init".equals(name) && "(Ljava/lang/Object;)V".equals(descriptor))
                    || (("start".equals(name) || "stop".equals(name) || "destroy".equals(name))
                    && "()V".equals(descriptor)))) {
                methods.add(name);
            }
            return null;
        }
    }
}
