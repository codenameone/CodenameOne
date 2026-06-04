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
package com.codename1.lottie.transcoder;

import com.codename1.svg.transcoder.SVGTranscoder;
import com.codename1.svg.transcoder.SVGTranscoder.GeneratedClass;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Headless command-line front end for the SVG / Lottie transcoders -- the
 * standalone analog of the codenameone-maven-plugin {@code transcode-svg} goal,
 * for build environments that compile a Codename One app without the Maven
 * plugin (e.g. the native Windows port CI, which builds hellocodenameone-common
 * with raw kotlinc/javac).
 *
 * <p>Usage: {@code java -jar codenameone-lottie-transcoder-*-jar-with-dependencies.jar
 * <sourceDir> <outputDir> [package]}. Every {@code *.svg} under {@code sourceDir}
 * is transcoded via {@link SVGTranscoder} and every {@code *.json} / {@code *.lottie}
 * via {@link LottieTranscoder} into {@code outputDir/<package>/}, and a
 * {@code SVGRegistry} with {@code installGlobal()} is emitted alongside. The
 * images are registered without CSS sizing hints, so they render at their
 * intrinsic SVG size (the registry's no-arg constructor path).</p>
 */
public final class TranscoderCli {
    private static final String DEFAULT_PACKAGE = "com.codename1.generated.svg";
    private static final String REGISTRY_CLASS_NAME = "SVGRegistry";

    private TranscoderCli() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: TranscoderCli <sourceDir> <outputDir> [package]");
            System.exit(2);
            return;
        }
        File sourceDir = new File(args[0]);
        File outputDir = new File(args[1]);
        String pkg = args.length > 2 ? args[2] : DEFAULT_PACKAGE;
        File packageDir = new File(outputDir, pkg.replace('.', '/'));
        if (!packageDir.mkdirs() && !packageDir.isDirectory()) {
            System.err.println("Could not create output directory: " + packageDir);
            System.exit(1);
            return;
        }

        List<File> assets = new ArrayList<File>();
        collect(sourceDir, assets);
        Collections.sort(assets, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return a.getName().compareTo(b.getName());
            }
        });

        List<GeneratedClass> generated = new ArrayList<GeneratedClass>();
        Set<String> usedClassNames = new HashSet<String>();
        for (File f : assets) {
            String resourceName = f.getName();
            String lower = resourceName.toLowerCase();
            String className = uniqueClassName(SVGTranscoder.classNameFor(resourceName), usedClassNames);
            usedClassNames.add(className);
            File outFile = new File(packageDir, className + ".java");
            if (lower.endsWith(".svg")) {
                SVGTranscoder.transcode(f, pkg, className, outFile);
            } else {
                // .json / .lottie -- Bodymovin lowered into the same SVG model.
                LottieTranscoder.transcode(f, pkg, className, outFile);
            }
            generated.add(new GeneratedClass(pkg, className, resourceName));
            System.out.println("Transcoded " + resourceName + " -> " + className + ".java");
        }

        if (!generated.isEmpty()) {
            File registryFile = new File(packageDir, REGISTRY_CLASS_NAME + ".java");
            Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(registryFile), StandardCharsets.UTF_8));
            try {
                SVGTranscoder.writeRegistry(pkg, REGISTRY_CLASS_NAME, generated, w);
            } finally {
                w.close();
            }
        }
        System.out.println("Transcoded " + generated.size() + " vector image(s) into " + packageDir);
    }

    /** Recursively collects *.svg / *.json / *.lottie files under {@code dir}. */
    private static void collect(File dir, List<File> out) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collect(child, out);
            } else {
                String name = child.getName().toLowerCase();
                if (name.endsWith(".svg") || name.endsWith(".json") || name.endsWith(".lottie")) {
                    out.add(child);
                }
            }
        }
    }

    /** Ensures a unique class name by appending a numeric suffix on collision. */
    private static String uniqueClassName(String base, Set<String> used) {
        if (!used.contains(base)) {
            return base;
        }
        int i = 2;
        while (used.contains(base + i)) {
            i++;
        }
        return base + i;
    }
}
