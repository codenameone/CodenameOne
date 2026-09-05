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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Android port's deletable packages are excluded from compilation in
 * THREE places, and all three have to agree.
 *
 * <p>A package like {@code com/codename1/impl/android/locationbutton} compiles
 * only inside a generated application, where the builder has added the
 * dependency it needs. Every build of the port itself has to skip it, and the
 * port is built two different ways:</p>
 *
 * <ul>
 *   <li>{@code maven/android/pom.xml} -- the maven-compiler {@code <excludes>},
 *       which is what the reactor and PR CI use.</li>
 *   <li>{@code Ports/Android/build.xml} and
 *       {@code Ports/Android/nbproject/project.properties} -- the Ant build,
 *       which is what the <b>BuildDaemon</b> repository's CI uses when it
 *       clones this one.</li>
 * </ul>
 *
 * <p>Adding a package to the Maven list alone therefore builds perfectly here
 * and fails in the other repository, with a javac error naming a library
 * nothing in the message connects to the feature. That is not hypothetical:
 * it is how this test came to exist. The {@code project.properties} comment
 * already said "Mirrors the maven-compiler excludes in
 * maven/android/pom.xml", and a comment is not a check.</p>
 */
public class AndroidPortExcludeParityTest {

    /** Repository root, from the module this test runs in. */
    private static File repoFile(String relative) {
        File f = new File("../../" + relative);
        assertTrue(f.exists(), "expected to find " + f.getAbsolutePath());
        return f;
    }

    private static String read(File f) throws Exception {
        return new String(Files.readAllBytes(f.toPath()),
                StandardCharsets.UTF_8);
    }

    /** The {@code <exclude>} entries of the maven-compiler configuration. */
    private static Set<String> mavenExcludes() throws Exception {
        String pom = read(repoFile("maven/android/pom.xml"));
        Set<String> out = new LinkedHashSet<String>();
        Matcher m = Pattern.compile("<exclude>([^<]+)</exclude>").matcher(pom);
        while (m.find()) {
            out.add(m.group(1).trim());
        }
        return out;
    }

    /** A comma-separated Ant exclude list, wherever it is written. */
    private static Set<String> antExcludes(String relativePath,
            String pattern) throws Exception {
        String text = read(repoFile(relativePath));
        Matcher m = Pattern.compile(pattern).matcher(text);
        assertTrue(m.find(), "no exclude list found in " + relativePath);
        Set<String> out = new LinkedHashSet<String>();
        for (String entry : m.group(1).split(",")) {
            if (entry.trim().length() > 0) {
                out.add(entry.trim());
            }
        }
        return out;
    }

    @Test
    public void theAntBuildExcludesWhatTheMavenBuildExcludes() throws Exception {
        assertEquals(mavenExcludes(),
                antExcludes("Ports/Android/build.xml",
                        // The javac one specifically: build.xml carries other
                        // excludes= attributes (META-INF/* on a jar task among
                        // them), and matching the first found an unrelated
                        // list and compared it against the pom's.
                        "excludes=\"(com/codename1/impl/android/[^\"]+)\""),
                "Ports/Android/build.xml must exclude exactly what "
                + "maven/android/pom.xml does. A package excluded in only one "
                + "of them compiles in this repository and fails in the "
                + "BuildDaemon build, which uses the Ant one.");
    }

    @Test
    public void theNetbeansProjectExcludesWhatTheMavenBuildExcludes()
            throws Exception {
        assertEquals(mavenExcludes(),
                antExcludes("Ports/Android/nbproject/project.properties",
                        "(?m)^excludes=(.+)$"),
                "Ports/Android/nbproject/project.properties must exclude "
                + "exactly what maven/android/pom.xml does.");
    }

    /**
     * Guards the guard: a regex that stopped matching would make both
     * assertions above compare two empty sets and pass.
     */
    @Test
    public void theExcludeListsAreActuallyBeingRead() throws Exception {
        assertTrue(mavenExcludes().size() >= 4,
                "the pom exclude list looks unreadable: " + mavenExcludes());
        assertTrue(mavenExcludes().contains(
                "com/codename1/impl/android/locationbutton/**"),
                "the location button package must be excluded: "
                + mavenExcludes());
    }
}
