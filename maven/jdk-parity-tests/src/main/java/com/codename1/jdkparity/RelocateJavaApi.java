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
package com.codename1.jdkparity;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Compiles vm/JavaAPI and copies selected classes into a test module's classes,
 * renamed under {@code jdkparity/}.
 *
 * Arguments: the JavaAPI source root, a scratch directory for its classes, the
 * output directory, and a comma separated list of internal class names to
 * relocate (each with its nested classes). Every reference to a relocated class,
 * in any copied class, is renamed with it; every other reference is left alone
 * and resolves to the JDK at run time. {@code com/codename1/impl/JdkHost} and its
 * nested classes are copied without renaming.
 */
public final class RelocateJavaApi {
    static final String PREFIX = "jdkparity/";
    private static final String JDK_HOST = "com/codename1/impl/JdkHost";

    private RelocateJavaApi() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            throw new IllegalArgumentException("usage: <javaapi-src> <javaapi-classes> <out> <a/B,c/D>");
        }
        Path src = Paths.get(args[0]);
        Path classes = Paths.get(args[1]);
        Path out = Paths.get(args[2]);
        List<String> relocated = new ArrayList<String>();
        for (String name : args[3].split(",")) {
            if (name.trim().length() > 0) {
                relocated.add(name.trim());
            }
        }
        compile(src, classes);

        Remapper remapper = new Remapper() {
            @Override
            public String map(String internalName) {
                return isRelocated(relocated, internalName) ? PREFIX + internalName : internalName;
            }
        };
        int copied = 0;
        List<Path> files;
        try (Stream<Path> s = Files.walk(classes)) {
            files = s.filter(p -> p.toString().endsWith(".class")).collect(Collectors.toList());
        }
        for (Path file : files) {
            String name = classes.relativize(file).toString().replace('\\', '/');
            name = name.substring(0, name.length() - ".class".length());
            boolean host = name.equals(JDK_HOST) || name.startsWith(JDK_HOST + "$");
            if (!host && !isRelocated(relocated, name)) {
                continue;
            }
            byte[] bytes = Files.readAllBytes(file);
            String target = name;
            if (!host) {
                ClassReader reader = new ClassReader(bytes);
                ClassWriter writer = new ClassWriter(0);
                reader.accept(new ClassRemapper(writer, remapper), 0);
                bytes = writer.toByteArray();
                target = PREFIX + name;
            }
            Path dest = out.resolve(target + ".class");
            Files.createDirectories(dest.getParent());
            Files.write(dest, bytes);
            copied++;
        }
        if (copied == 0) {
            throw new IllegalStateException("nothing relocated from " + classes);
        }
        System.out.println("[jdk-parity] copied " + copied + " JavaAPI classes into " + out);
    }

    static boolean isRelocated(List<String> relocated, String internalName) {
        for (String r : relocated) {
            if (internalName.equals(r) || internalName.startsWith(r + "$")) {
                return true;
            }
        }
        return false;
    }

    private static void compile(Path src, Path out) throws IOException {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            throw new IllegalStateException("Maven must run on a JDK to build this module");
        }
        Files.createDirectories(out);
        List<String> args = new ArrayList<String>();
        if (System.getProperty("java.specification.version").startsWith("1.")) {
            args.addAll(Arrays.asList("-source", "1.8", "-target", "1.8"));
        } else {
            // No modules at release 8, so the tree may declare java.lang itself.
            args.addAll(Arrays.asList("--release", "8"));
        }
        args.addAll(Arrays.asList("-nowarn", "-encoding", "UTF-8", "-proc:none",
                "-Xmaxerrs", "100000", "-d", out.toString()));
        try (Stream<Path> s = Files.walk(src)) {
            args.addAll(s.filter(p -> p.toString().endsWith(".java"))
                    .map(Path::toString).sorted().collect(Collectors.toList()));
        }
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int rc = javac.run(null, null, err, args.toArray(new String[0]));
        if (rc != 0) {
            throw new IllegalStateException("compiling " + src + " failed:\n"
                    + new String(err.toByteArray(), StandardCharsets.UTF_8));
        }
    }
}
