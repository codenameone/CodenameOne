/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.dart.transpiler.api;

import com.codename1.dart.transpiler.analyze.Program;
import com.codename1.dart.transpiler.analyze.StubRegistry;
import com.codename1.dart.transpiler.ast.Ast;
import com.codename1.dart.transpiler.codegen.JavaEmitter;
import com.codename1.dart.transpiler.parser.AstBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Whole-program Dart-to-Java transpiler entry point (called by the
 * Codename One Maven plugin's transcode-flutter goal and by tests).
 *
 * <p>Incrementality is whole-program: a digest over every input file plus
 * the transpiler version is compared against {@code stateFile}; on match the
 * transpile is skipped, otherwise everything regenerates. Output writes are
 * content-stable — unchanged files are not rewritten, keeping downstream
 * javac incremental.</p>
 */
public final class DartTranspiler {

    /** Bumped whenever emission changes so stale state files don't skip. */
    private static final String VERSION = "m1-1";

    public TranspileResult transpile(TranspileRequest req) {
        Diagnostics diags = new Diagnostics();
        List<File> dartFiles = new ArrayList<File>();
        for (File root : req.sourceRoots) {
            collectDartFiles(root, root, dartFiles);
        }
        Collections.sort(dartFiles);

        // The output PACKAGE is part of what was generated, so it is part of the
        // key: changing cn1.flutter.package alone used to leave every generated
        // source declared in the old package, because nothing else had changed.
        String digest = digest(dartFiles) + stubDigest(req.stubClasspath)
                + "-pkg:" + req.packageName;
        if (req.stateFile != null && req.stateFile.exists() && req.outputDir != null && req.outputDir.exists()) {
            try {
                String prev = new String(Files.readAllBytes(req.stateFile.toPath()), StandardCharsets.UTF_8).trim();
                if (prev.equals(digest)) {
                    return new TranspileResult(diags.asList(), new ArrayList<GeneratedFile>(), true);
                }
            } catch (IOException ignore) {
                // fall through to full transpile
            }
        }

        Program program = new Program();
        AstBuilder builder = new AstBuilder(diags);
        for (File f : dartFiles) {
            try {
                String src = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                String rel = relativize(req.sourceRoots, f);
                program.add(builder.parse(rel, src));
            } catch (IOException e) {
                diags.error(f.getName(), 0, 0, "E0003", "Cannot read file: " + e);
            } catch (RuntimeException | StackOverflowError e) {
                // Front-end robustness: a parser/AST-builder gap must never abort the whole build.
                // Record it as a diagnostic (with the crash site) so a SINGLE pass over a large real
                // app yields the full gap inventory instead of dying on the first unhandled construct.
                StackTraceElement[] st = e.getStackTrace();
                StackTraceElement top = null;
                for (StackTraceElement s : st) {
                    if (s.getClassName().startsWith("com.codename1.dart.transpiler")) { top = s; break; }
                }
                String at = top == null ? "" : "  @ "
                        + top.getClassName().substring(top.getClassName().lastIndexOf('.') + 1)
                        + "." + top.getMethodName() + ":" + top.getLineNumber();
                diags.error(f.getName(), 0, 0, "E0004",
                        "Front-end crash: " + e.getClass().getSimpleName()
                        + (e.getMessage() != null ? ": " + e.getMessage() : "") + at);
            }
        }

        StubRegistry stubs = req.stubClasspath.isEmpty()
                ? StubRegistry.loadEmbedded(diags)
                : StubRegistry.loadFromClasspath(req.stubClasspath, diags);
        JavaEmitter emitter = new JavaEmitter(program, stubs, diags, req.packageName);
        List<GeneratedFile> files = emitter.emit();

        if (!diags.hasErrors() && req.outputDir != null) {
            writeOutput(req, files, diags);
            if (req.stateFile != null && diags.hasErrors()) {
                // A file failed to write, so the output does not match these inputs.
                // Recording the digest anyway sent the next unchanged build down the
                // up-to-date path with a missing or stale generated file and no error;
                // removing the state (it may name an earlier, matching build) forces
                // the next build to write everything again.
                if (req.stateFile.exists() && !req.stateFile.delete()) {
                    diags.warn(null, "W0001", "Could not remove stale transpiler state file: " + req.stateFile);
                }
            } else if (req.stateFile != null) {
                try {
                    req.stateFile.getParentFile().mkdirs();
                    Files.write(req.stateFile.toPath(), digest.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    diags.warn(null, "W0001", "Could not write transpiler state file: " + e);
                }
            }
        }
        return new TranspileResult(diags.asList(), files, false);
    }

    private void writeOutput(TranspileRequest req, List<GeneratedFile> files, Diagnostics diags) {
        File pkgDir = new File(req.outputDir, req.packageName.replace('.', File.separatorChar));
        removePreviousPackage(req, pkgDir);
        pkgDir.mkdirs();
        Set<String> expected = new HashSet<String>();
        for (GeneratedFile gf : files) {
            expected.add(gf.relativePath);
            File out = new File(pkgDir, gf.relativePath);
            out.getParentFile().mkdirs();
            try {
                byte[] content = gf.content.getBytes(StandardCharsets.UTF_8);
                if (out.exists() && Arrays.equals(Files.readAllBytes(out.toPath()), content)) {
                    continue;  // content-stable: keep timestamp for incremental javac
                }
                Files.write(out.toPath(), content);
            } catch (IOException e) {
                diags.error(gf.relativePath, 0, 0, "E0004", "Cannot write generated file: " + e);
            }
        }
        // sweep stale generated files
        File[] existing = pkgDir.listFiles();
        if (existing != null) {
            for (File f : existing) {
                if (f.isFile() && f.getName().endsWith(".java") && !expected.contains(f.getName())) {
                    f.delete();
                }
            }
        }
    }

    /**
     * When the output package changed since the last run, deletes what that run generated
     * into the OLD package's directory. The stale-file sweep below only looks in the new
     * package's directory, so every source of the previous package stayed on the compile
     * root and was compiled and packaged beside its replacement -- a second, obsolete copy
     * of the whole generated tree. Only the generated .java files directly in that
     * directory go, so an old package that is a parent of the new one keeps the new files.
     */
    private void removePreviousPackage(TranspileRequest req, File pkgDir) {
        if (req.stateFile == null || !req.stateFile.exists()) {
            return;
        }
        String prev;
        try {
            prev = new String(Files.readAllBytes(req.stateFile.toPath()), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return;
        }
        int at = prev.lastIndexOf("-pkg:");
        if (at < 0) {
            return;
        }
        String prevPkg = prev.substring(at + 5);
        if (prevPkg.isEmpty() || prevPkg.equals(req.packageName)) {
            return;
        }
        File old = new File(req.outputDir, prevPkg.replace('.', File.separatorChar));
        File[] children = old.listFiles();
        if (children == null || old.equals(pkgDir)) {
            return;
        }
        for (File f : children) {
            if (f.isFile() && f.getName().endsWith(".java")) {
                f.delete();
            }
        }
        String[] left = old.list();
        if (left != null && left.length == 0) {
            old.delete();
        }
    }

    private void collectDartFiles(File root, File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                // Only the asset root itself -- src/main/flutter/assets -- is not
                // source. Skipping every directory NAMED assets silently dropped
                // real code such as lib/assets/generated.dart from the
                // whole-program parse, and its importers failed to resolve.
                if (!(dir.equals(root) && f.getName().equals("assets"))) {
                    collectDartFiles(root, f, out);
                }
            } else if (f.getName().endsWith(".dart")) {
                out.add(f);
            }
        }
    }

    private String relativize(List<File> roots, File f) {
        for (File root : roots) {
            String rootPath = root.getAbsolutePath();
            String path = f.getAbsolutePath();
            if (path.startsWith(rootPath)) {
                String rel = path.substring(rootPath.length());
                if (rel.startsWith(File.separator)) {
                    rel = rel.substring(1);
                }
                return rel.replace(File.separatorChar, '/');
            }
        }
        return f.getName();
    }

    private String stubDigest(List<File> stubClasspath) {
        if (stubClasspath.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("-stubs");
        for (File f : stubClasspath) {
            sb.append('|').append(f.getAbsolutePath()).append('@').append(f.lastModified());
            if (f.isDirectory()) {
                // A class directory -- a reactor or development build -- keeps its
                // stubs under META-INF/dart, and editing one of those files does not
                // touch the directory's own timestamp. The directory alone therefore
                // hashed the same after a stub change and the transpile was skipped
                // against the old runtime API.
                appendStubFiles(new File(f, "META-INF/dart"), sb);
            }
        }
        return Integer.toHexString(sb.toString().hashCode());
    }

    /** Every file under {@code dir}, in a stable order: path, size and timestamp. */
    private static void appendStubFiles(File dir, StringBuilder sb) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children);
        for (File c : children) {
            if (c.isDirectory()) {
                appendStubFiles(c, sb);
            } else {
                sb.append('|').append(c.getName()).append(':').append(c.length())
                        .append('@').append(c.lastModified());
            }
        }
    }

    private static String fingerprint;

    /**
     * A digest of the transpiler's OWN code, so that any change to what it emits
     * invalidates a cached result without anyone having to remember to.
     *
     * <p>VERSION was meant to do this and had to be bumped by hand; it stayed
     * "m1-1" through every emission change since the first milestone. A project
     * that upgraded its plugin kept a state file whose digest still matched, so
     * the transpile was skipped and the generated sources stayed whatever the
     * previous transpiler had produced. Hashing the jar (or the class directory
     * in a development build) the transpiler was loaded from makes that
     * automatic. Computed once per JVM.</p>
     */
    private static synchronized String transpilerFingerprint() {
        if (fingerprint != null) {
            return fingerprint;
        }
        try {
            File where = new File(DartTranspiler.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            List<File> parts = new ArrayList<File>();
            if (where.isDirectory()) {
                collectFiles(where, parts);
                Collections.sort(parts);
            } else {
                parts.add(where);
            }
            for (File f : parts) {
                md.update(Files.readAllBytes(f.toPath()));
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            fingerprint = sb.toString();
        } catch (Exception e) {
            // Unknown code source: never match a previous state, rather than risk
            // reusing output from a different transpiler.
            fingerprint = "unknown-" + System.nanoTime();
        }
        return fingerprint;
    }

    private static void collectFiles(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File c : children) {
            if (c.isDirectory()) {
                collectFiles(c, out);
            } else {
                out.add(c);
            }
        }
    }

    private String digest(List<File> files) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(VERSION.getBytes(StandardCharsets.UTF_8));
            md.update(transpilerFingerprint().getBytes(StandardCharsets.UTF_8));
            for (File f : files) {
                md.update(f.getAbsolutePath().getBytes(StandardCharsets.UTF_8));
                md.update(Files.readAllBytes(f.toPath()));
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "no-digest-" + System.nanoTime();
        }
    }
}
