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
package com.codename1.cil;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Assume;

/// What the sample tests share: where the repository and the .NET SDK are, and
/// how to run the C# compiler, `javac`, the translator and a child JVM.
///
/// The .NET CLI is looked for in the `dotnet.executable` system property, then
/// in the `DOTNET_ROOT` environment variable, then on the `PATH`. A test that
/// asks for it when there is none is skipped, since .NET is no requirement of
/// the framework build -- unless `unity.requireDotnet` is `true`, which is how
/// a build that exists to run these tests makes their absence a failure.
public final class UnityToolchain {
    private static String dotnet;
    private static boolean dotnetResolved;

    private UnityToolchain() {
    }

    /// The root of the checkout: `unity.repoRoot` if given, otherwise the
    /// nearest directory above the working one that has the samples.
    public static File repoRoot() {
        String given = System.getProperty("unity.repoRoot");
        File dir = new File(given != null && given.length() > 0 ? given : System.getProperty("user.dir"))
                .getAbsoluteFile();
        for (File d = dir; d != null; d = d.getParentFile()) {
            if (new File(d, "scripts/unity-compat-samples").isDirectory()) {
                try {
                    return d.getCanonicalFile();
                } catch (IOException e) {
                    return d;
                }
            }
        }
        throw new AssertionError("no scripts/unity-compat-samples above " + dir);
    }

    public static File samples() {
        return new File(repoRoot(), "scripts/unity-compat-samples");
    }

    /// The runtime module's sources and C# projects.
    public static File runtimeModule() {
        return new File(repoRoot(), "maven/unity-compat");
    }

    /// An empty directory for one test's output, under `target/`.
    public static File workDir(String name) throws IOException {
        String given = System.getProperty("unity.workDir");
        File base = given != null && given.length() > 0 ? new File(given) : new File("target/unity-tests");
        File dir = new File(base.getAbsoluteFile(), name);
        delete(dir);
        if (!dir.mkdirs()) {
            throw new IOException("cannot create " + dir);
        }
        return dir;
    }

    private static void delete(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                delete(c);
            }
        }
        f.delete();
    }

    /// The .NET CLI. Skips the calling test when there is none, or fails it
    /// when `unity.requireDotnet` is `true`.
    public static synchronized String dotnet() {
        if (!dotnetResolved) {
            dotnet = findDotnet();
            dotnetResolved = true;
        }
        if (dotnet == null) {
            String message = "The .NET SDK was not found: looked at the dotnet.executable system property ("
                    + System.getProperty("dotnet.executable") + "), the DOTNET_ROOT environment variable ("
                    + System.getenv("DOTNET_ROOT") + ") and the PATH.";
            if (Boolean.getBoolean("unity.requireDotnet")) {
                Assert.fail(message + " unity.requireDotnet is set, so this is a failure.");
            }
            Assume.assumeTrue(message + " Skipping; pass -Dunity.requireDotnet=true to fail instead.", false);
        }
        return dotnet;
    }

    private static String findDotnet() {
        String exe = System.getProperty("os.name", "").toLowerCase().contains("win") ? "dotnet.exe" : "dotnet";
        String property = System.getProperty("dotnet.executable");
        if (property != null && property.length() > 0) {
            File f = new File(property);
            if (f.isFile()) {
                return f.getAbsolutePath();
            }
            // A bare command name is a request to search the PATH for it.
            return f.getParent() == null ? onPath(property) : null;
        }
        String root = System.getenv("DOTNET_ROOT");
        if (root != null && root.length() > 0 && new File(root, exe).isFile()) {
            return new File(root, exe).getAbsolutePath();
        }
        return onPath(exe);
    }

    private static String onPath(String name) {
        String path = System.getenv("PATH");
        if (path == null) {
            return null;
        }
        for (String dir : path.split(File.pathSeparator)) {
            File f = new File(dir, name);
            if (dir.length() > 0 && f.isFile() && f.canExecute()) {
                return f.getAbsolutePath();
            }
        }
        return null;
    }

    /// `dotnet build -c Release` of one project; fails the test with the
    /// compiler's output if it does not build.
    public static void dotnetBuild(File project, File work) throws IOException, InterruptedException {
        Result r = run(work, "dotnet-build-" + project.getName(), dotnetEnvironment(),
                Arrays.asList(dotnet(), "build", "-c", "Release", "--nologo", project.getAbsolutePath()));
        r.assertOk("dotnet build " + project);
    }

    /// `dotnet run` of an already built project, with arguments for the program.
    public static Result dotnetRun(File project, File work, String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<String>(Arrays.asList(dotnet(), "run", "-c", "Release", "--no-build",
                "--project", project.getAbsolutePath(), "--"));
        cmd.addAll(Arrays.asList(args));
        return run(work, "dotnet-run-" + project.getName(), dotnetEnvironment(), cmd);
    }

    private static List<String> dotnetEnvironment() {
        return Arrays.asList("DOTNET_CLI_TELEMETRY_OPTOUT=1", "DOTNET_NOLOGO=1");
    }

    /// Where a class was loaded from: a classes directory or a jar.
    public static File locationOf(String className) {
        try {
            Class<?> c = Class.forName(className, false, UnityToolchain.class.getClassLoader());
            return new File(c.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (ClassNotFoundException e) {
            throw new AssertionError(className + " is not on the test class path", e);
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    /// The translator and scene compiler under test, with ASM.
    public static String toolClasspath() {
        return path(locationOf("com.codename1.cil.translate.Translator"), locationOf("org.objectweb.asm.ClassWriter"));
    }

    /// The Codename One core classes the engine runtime compiles against.
    public static File core() {
        return locationOf("com.codename1.gaming.physics.box2d.dynamics.World");
    }

    public static String path(File... entries) {
        StringBuilder sb = new StringBuilder();
        for (File f : entries) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(f.getAbsolutePath());
        }
        return sb.toString();
    }

    private static String jdkTool(String name) {
        File home = new File(System.getProperty("java.home"));
        String exe = System.getProperty("os.name", "").toLowerCase().contains("win") ? name + ".exe" : name;
        // A JDK 8 java.home is its jre directory; the compiler is one level up.
        for (File bin : new File[] {new File(home, "bin"), new File(home.getParentFile(), "bin")}) {
            if (new File(bin, exe).isFile()) {
                return new File(bin, exe).getAbsolutePath();
            }
        }
        throw new AssertionError("no " + name + " in " + home + ": the tests need a JDK, not a JRE");
    }

    /// Compiles every `.java` file under the source roots to Java 8 classes.
    public static void javac(File work, String label, File out, String classpath, File... sourceRoots)
            throws IOException, InterruptedException {
        List<String> sources = new ArrayList<String>();
        for (File root : sourceRoots) {
            collect(root, sources);
        }
        Collections.sort(sources);
        Assert.assertFalse("no Java sources under " + Arrays.toString(sourceRoots), sources.isEmpty());
        File list = new File(work, label + ".sources");
        StringBuilder sb = new StringBuilder();
        for (String s : sources) {
            sb.append('"').append(s.replace("\\", "\\\\")).append("\"\n");
        }
        Files.write(list.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        out.mkdirs();
        List<String> cmd = new ArrayList<String>(Arrays.asList(jdkTool("javac"), "-nowarn", "-source", "8",
                "-target", "8", "-encoding", "ASCII", "-d", out.getAbsolutePath()));
        if (classpath != null && classpath.length() > 0) {
            cmd.add("-cp");
            cmd.add(classpath);
        }
        cmd.add("@" + list.getAbsolutePath());
        run(work, label, null, cmd).assertOk("javac " + label);
    }

    private static void collect(File f, List<String> into) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                collect(c, into);
            }
        } else if (f.getName().endsWith(".java")) {
            into.add(f.getAbsolutePath());
        }
    }

    /// Runs one of this module's tools (`mainClass` of the translator jar) in a
    /// JVM of its own: both tools end a failed run with `System.exit`.
    public static void tool(File work, String label, String mainClass, List<String> args)
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<String>(Arrays.asList(jdkTool("java"), "-cp", toolClasspath(), mainClass));
        cmd.addAll(args);
        run(work, label, null, cmd).assertOk(mainClass + " " + args);
    }

    /// Runs translated code with the verifier on for every class, which is
    /// what makes a run say something about the bytecode the translator wrote.
    public static Result java(File work, String label, String classpath, String mainClass)
            throws IOException, InterruptedException {
        return run(work, label, null, Arrays.asList(jdkTool("java"), "-Xverify:all", "-cp", classpath, mainClass));
    }

    /// Runs a command to completion with its output in files under `work`, so a
    /// process that writes a lot to either stream cannot block on a full pipe.
    public static Result run(File work, String label, List<String> environment, List<String> cmd)
            throws IOException, InterruptedException {
        File out = new File(work, label + ".out.txt");
        File err = new File(work, label + ".err.txt");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(work);
        pb.redirectOutput(out);
        pb.redirectError(err);
        if (environment != null) {
            for (String e : environment) {
                int eq = e.indexOf('=');
                pb.environment().put(e.substring(0, eq), e.substring(eq + 1));
            }
        }
        int exit = pb.start().waitFor();
        return new Result(cmd, exit, read(out), read(err));
    }

    public static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    /// The lines of a text, whatever its line endings.
    public static List<String> lines(String text) {
        List<String> lines = new ArrayList<String>(Arrays.asList(text.replace("\r\n", "\n").split("\n", -1)));
        while (!lines.isEmpty() && lines.get(lines.size() - 1).length() == 0) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    /// Fails with the first line that differs, which a comparison of two whole
    /// texts does not point at.
    public static void assertSameLines(String what, String expected, String actual) {
        List<String> e = lines(expected);
        List<String> a = lines(actual);
        int n = Math.min(e.size(), a.size());
        for (int i = 0; i < n; i++) {
            if (!e.get(i).equals(a.get(i))) {
                Assert.fail(what + ": line " + (i + 1) + " differs\n  expected: " + e.get(i) + "\n  actual:   "
                        + a.get(i));
            }
        }
        if (e.size() != a.size()) {
            List<String> longer = e.size() > a.size() ? e : a;
            Assert.fail(what + ": expected " + e.size() + " lines, got " + a.size() + "; first "
                    + (e.size() > a.size() ? "missing" : "extra") + " line: " + longer.get(n));
        }
    }

    /// What a finished process left behind.
    public static final class Result {
        public final List<String> command;
        public final int exit;
        public final String out;
        public final String err;

        Result(List<String> command, int exit, String out, String err) {
            this.command = command;
            this.exit = exit;
            this.out = out;
            this.err = err;
        }

        public void assertOk(String what) {
            if (exit != 0) {
                Assert.fail(what + " exited with " + exit + "\n" + command + "\n--- stdout\n" + out + "\n--- stderr\n"
                        + err);
            }
        }
    }
}
