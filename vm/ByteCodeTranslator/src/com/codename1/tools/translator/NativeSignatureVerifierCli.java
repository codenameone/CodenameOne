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
package com.codename1.tools.translator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Command-line entry point for {@link NativeSignatureVerifier}, driven by
 * scripts/check-native-signatures.sh.
 *
 * Split out of the verifier for two reasons, both about the self-hosted translator
 * build. It is the half that scans jars, so it is the half that needs
 * java.util.zip -- which JavaAPI cannot gain, being mirrored by Ports/CLDC11. And a
 * second class carrying a {@code main} makes ByteCodeClass.addMethod refuse the
 * translation outright with "Multiple main classes", since the clean target does
 * not set a preferred main class the way the JavaScript target does.
 *
 * A translation never comes through here: the verifier's in-process entry points
 * are what Parser calls.
 */
public final class NativeSignatureVerifierCli {
    private NativeSignatureVerifierCli() {
    }

    public static void main(String[] args) throws IOException {
        List<File> classRoots = new ArrayList<File>();
        List<File> nativeRoots = new ArrayList<File>();
        boolean orphans = true;
        for (int iter = 0; iter < args.length; iter++) {
            if ("--classes".equals(args[iter]) && iter + 1 < args.length) {
                classRoots.add(new File(args[++iter]));
            } else if ("--natives".equals(args[iter]) && iter + 1 < args.length) {
                nativeRoots.add(new File(args[++iter]));
            } else if ("--no-orphans".equals(args[iter])) {
                orphans = false;
            } else {
                System.err.println("unrecognised argument: " + args[iter]);
                usage();
                System.exit(2);
            }
        }
        if (classRoots.isEmpty() || nativeRoots.isEmpty()) {
            usage();
            System.exit(2);
        }

        List<NativeSignatureVerifier.Signature> required = new ArrayList<NativeSignatureVerifier.Signature>();
        for (File root : classRoots) {
            if (!root.exists()) {
                System.err.println("NativeSignatureVerifier: no such path: " + root);
                System.exit(2);
            }
            required.addAll(NativeSignatureVerifier.collectFromClasses(root));
        }
        List<File> sources = new ArrayList<File>();
        for (File root : nativeRoots) {
            if (!root.exists()) {
                System.err.println("NativeSignatureVerifier: no such path: " + root);
                System.exit(2);
            }
            sources.addAll(root.isDirectory()
                    ? NativeSignatureVerifier.listNativeSourcesRecursive(root) : Collections.singletonList(root));
        }

        NativeSignatureVerifier.SourceIndex index = new NativeSignatureVerifier.SourceIndex(sources);
        List<NativeSignatureVerifier.Problem> problems = NativeSignatureVerifier.verify(required, index);
        if (!orphans) {
            List<NativeSignatureVerifier.Problem> filtered = new ArrayList<NativeSignatureVerifier.Problem>();
            for (NativeSignatureVerifier.Problem problem : problems) {
                if (problem.kind != NativeSignatureVerifier.Kind.ORPHAN) {
                    filtered.add(problem);
                }
            }
            problems = filtered;
        }

        if (problems.isEmpty()) {
            System.out.println("NativeSignatureVerifier: " + required.size()
                    + " native method(s) all resolve against " + index.size()
                    + " C definition(s) in " + sources.size() + " file(s).");
            return;
        }
        int fatal = NativeSignatureVerifier.report(problems, NativeSignatureVerifier.Mode.STRICT,
                required.size() + " native methods, " + sources.size() + " native sources", true);
        System.exit(fatal > 0 ? 1 : 0);
    }

    private static void usage() {
        System.err.println("usage: NativeSignatureVerifier --classes DIR_OR_JAR [--classes ...]"
                + " --natives DIR [--natives ...] [--no-orphans]");
    }
}
