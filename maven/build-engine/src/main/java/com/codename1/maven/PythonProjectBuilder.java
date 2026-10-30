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

import com.codename1.build.Log;
import com.codename1.builders.BuildException;
import com.codename1.python.transpiler.api.PythonSyntaxError;
import com.codename1.python.transpiler.api.PythonTranspiler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/// Transpiles `src/main/python` to Java source, for the Maven plugin's
/// `transpile-python` goal and the Gradle plugin's `transpilePython` task.
///
/// Every `.py` file under the source directory is a module named by its path
/// (`pkg/mod.py` is `pkg.mod`). The output directory is owned by this builder:
/// a file is rewritten only when its text changed, so javac recompiles only
/// what did, and a file no module produces any more is deleted.
///
/// The entry module, run by `PythonModules.runMain()`, is the one given, or
/// `main` when `main.py` exists. An entry of the form `module:function`, as
/// `[project.scripts]` names one, calls that function and exits with what it
/// returns; without either the sources are a library,
/// reached from Java through `PythonModules.registerAll()` and the generated
/// module classes.
public final class PythonProjectBuilder {
    /// The runtime every generated class calls.
    public static final String RUNTIME_ARTIFACT = "codenameone-python-runtime";

    /// The module generated for a `module:function` entry point.
    static final String ENTRY_MODULE = "__cn1_entry__";

    private final File sourceDir;
    private final File outputDir;
    private final String mainModule;
    private final Log log;
    private java.util.List<File> classPath = java.util.Collections.emptyList();

    public PythonProjectBuilder(File sourceDir, File outputDir, String mainModule, Log log) {
        this.sourceDir = sourceDir;
        this.outputDir = outputDir;
        this.mainModule = mainModule;
        this.log = log;
    }

    /// The build hint naming the C stack a served request's virtual thread
    /// gets, such as `2MB`; see [#virtualThreadStack(String)].
    public static final String VIRTUAL_THREAD_STACK_HINT = "codename1.arg.python.virtualThreadStack";

    private String virtualThreadStack;

    /// The value of [#VIRTUAL_THREAD_STACK_HINT]: bytes, or a number with a
    /// `KB` or `MB` suffix. Null or blank keeps the transpiler's 1MB default;
    /// anything else that is not a size fails [#run()].
    public PythonProjectBuilder virtualThreadStack(String size) {
        this.virtualThreadStack = size;
        return this;
    }

    /// The jars and class directories `from java.<package> import <Class>`
    /// resolves against: the project's compile class path, whose
    /// `java-runtime` supplies the device's `java.*`. The project's own
    /// Java sources come from [#javaSources(File, File)].
    public PythonProjectBuilder classPath(java.util.List<File> entries) {
        this.classPath = entries;
        return this;
    }

    private File javaSourceDir;
    private File javaScratchDir;

    /// The project's own Java sources (`src/main/java`), and a directory to
    /// compile them into. They are not compiled yet when Python is
    /// transpiled, so a class Python imports from them is compiled here
    /// first, with whatever it needs from the same tree, and only for its
    /// signatures: javac compiles the project again afterwards. Such a class
    /// cannot itself use the Java generated from Python.
    public PythonProjectBuilder javaSources(File dir, File scratch) {
        this.javaSourceDir = dir;
        this.javaScratchDir = scratch;
        return this;
    }

    private static final java.util.regex.Pattern JAVA_IMPORT = java.util.regex.Pattern.compile(
            "^[ \\t]*from[ \\t]+java\\.([\\w.]+)[ \\t]+import[ \\t]+(?:\\(([^)]*)\\)|([\\w, \\t]+))",
            java.util.regex.Pattern.MULTILINE);

    /// The class path with the project's own Java classes that `sources`
    /// import compiled in front of it, or the class path as it is when they
    /// import none.
    private java.util.List<File> withProjectClasses(Map<String, String> sources) throws BuildException {
        if (javaSourceDir == null || javaScratchDir == null || !javaSourceDir.isDirectory()) {
            return classPath;
        }
        Set<String> files = new java.util.TreeSet<String>();
        for (String text : sources.values()) {
            java.util.regex.Matcher m = JAVA_IMPORT.matcher(text);
            while (m.find()) {
                String pkg = m.group(1).replace('.', File.separatorChar);
                for (String name : (m.group(2) != null ? m.group(2) : m.group(3)).split("[,\\s]+")) {
                    File java = new File(javaSourceDir, pkg + File.separator + name + ".java");
                    if (!name.isEmpty() && java.isFile()) {
                        files.add(java.getAbsolutePath());
                    }
                }
            }
        }
        if (files.isEmpty()) {
            return classPath;
        }
        javax.tools.JavaCompiler javac = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            throw new BuildException("Python imports Java classes of this project, which have to be compiled "
                    + "before the Python is transpiled, and this JVM has no Java compiler. Run the build on a JDK.");
        }
        javaScratchDir.mkdirs();
        StringBuilder cp = new StringBuilder();
        for (File f : classPath) {
            cp.append(cp.length() == 0 ? "" : File.pathSeparator).append(f.getAbsolutePath());
        }
        List<String> args = new ArrayList<String>(Arrays.asList("-proc:none", "-implicit:class", "-nowarn",
                "-encoding", "UTF-8", "-d", javaScratchDir.getAbsolutePath(),
                "-sourcepath", javaSourceDir.getAbsolutePath(), "-classpath", cp.toString()));
        args.addAll(files);
        java.io.ByteArrayOutputStream err = new java.io.ByteArrayOutputStream();
        int status = javac.run(null, null, err, args.toArray(new String[0]));
        if (status != 0) {
            throw new BuildException("Python imports Java classes of this project (" + files.size()
                    + " file(s)), which are compiled before the Python is transpiled and so cannot use the "
                    + "Java generated from it. javac reported:\n"
                    + new String(err.toByteArray(), StandardCharsets.UTF_8));
        }
        List<File> r = new ArrayList<File>();
        r.add(javaScratchDir);
        r.addAll(classPath);
        return r;
    }

    /// Whether `dir` holds at least one `.py` file.
    public static boolean isPythonProject(File dir) {
        if (dir == null) {
            return false;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return false;
        }
        for (File c : children) {
            if (c.isDirectory() ? isPythonProject(c) : c.getName().endsWith(".py")) {
                return true;
            }
        }
        return false;
    }

    /// Transpiles the sources. Returns false when there are none, after
    /// emptying the output directory of what an earlier build left there.
    public boolean run() throws BuildException {
        if (!isPythonProject(sourceDir)) {
            sync(new TreeMap<String, String>());
            return false;
        }
        Map<String, String> sources = new TreeMap<String, String>();
        Map<String, String> nativeModules = new TreeMap<String, String>();
        try {
            collect(sourceDir, "", sources);
            // PyPI packages import resolved beside the project; the
            // project's own file wins where both have one.
            File packages = new File(sourceDir.getParentFile(), PythonPackageResolver.PACKAGES_DIR);
            if (packages.isDirectory()) {
                Map<String, String> vendored = new TreeMap<String, String>();
                collect(packages, "", vendored);
                vendored.putAll(sources);
                sources = vendored;
                nativeModules = PythonExtensionStubs.readIndex(new File(packages, PythonExtensionStubs.INDEX));
            }
        } catch (IOException e) {
            throw new BuildException("Could not read " + sourceDir + ": " + e.getMessage(), e);
        }
        String entry = mainModule;
        if (entry == null && sources.containsKey("main.py")) {
            entry = "main";
        }
        int colon = entry == null ? -1 : entry.indexOf(':');
        if (colon > 0) {
            // A console-script target, `pkg.cli:main`: the function is what
            // runs, as the script pip writes for it calls it.
            String module = entry.substring(0, colon).trim();
            String function = entry.substring(colon + 1).trim();
            sources.put(ENTRY_MODULE + ".py", "import sys\nfrom " + module + " import " + function
                    + " as _entry\nsys.exit(_entry())\n");
            entry = ENTRY_MODULE;
        }
        Map<String, String> out;
        try {
            out = PythonTranspiler.transpile(sources, entry, withProjectClasses(sources), nativeModules,
                    stackBytes());
        } catch (PythonSyntaxError e) {
            throw new BuildException("src/main/python/" + e.file + ":" + e.line + ":" + e.col + ": " + e.reason, e);
        } catch (IllegalArgumentException e) {
            throw new BuildException(e.getMessage(), e);
        }
        int written = sync(out);
        if (written == 0) {
            log.info("Python sources are up to date");
        } else {
            log.info("Transpiled " + sources.size() + " Python module(s) to " + outputDir + " (" + written
                    + " file(s) changed)");
        }
        return true;
    }

    private static void collect(File dir, String prefix, Map<String, String> into) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children);
        for (File c : children) {
            if (c.isDirectory()) {
                if (!c.getName().startsWith(".") && !"__pycache__".equals(c.getName())) {
                    collect(c, prefix + c.getName() + "/", into);
                }
            } else if (c.getName().endsWith(".py")) {
                into.put(prefix + c.getName(), new String(Files.readAllBytes(c.toPath()), StandardCharsets.UTF_8));
            }
        }
    }

    /// Makes the output directory hold exactly `files`, writing only those
    /// whose text changed. Returns how many were written or removed.
    private int sync(Map<String, String> files) throws BuildException {
        int changed = 0;
        try {
            for (Map.Entry<String, String> e : files.entrySet()) {
                File f = new File(outputDir, e.getKey());
                byte[] text = e.getValue().getBytes(StandardCharsets.US_ASCII);
                if (f.isFile() && Arrays.equals(Files.readAllBytes(f.toPath()), text)) {
                    continue;
                }
                File parent = f.getParentFile();
                if (parent != null) {
                    Files.createDirectories(parent.toPath());
                }
                Files.write(f.toPath(), text);
                changed++;
            }
            Set<String> keep = new HashSet<String>();
            for (String k : files.keySet()) {
                keep.add(new File(outputDir, k).getAbsolutePath());
            }
            List<File> stale = new ArrayList<File>();
            listFiles(outputDir, stale);
            for (File f : stale) {
                if (!keep.contains(f.getAbsolutePath())) {
                    Files.delete(f.toPath());
                    changed++;
                }
            }
        } catch (IOException e) {
            throw new BuildException("Could not write " + outputDir + ": " + e.getMessage(), e);
        }
        return changed;
    }

    private static void listFiles(File dir, List<File> into) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File c : children) {
            if (c.isDirectory()) {
                listFiles(c, into);
            } else {
                into.add(c);
            }
        }
    }

    private int stackBytes() throws BuildException {
        if (virtualThreadStack == null || virtualThreadStack.trim().length() == 0) {
            return PythonTranspiler.DEFAULT_VIRTUAL_THREAD_STACK;
        }
        int bytes = PythonTranspiler.parseStackSize(virtualThreadStack);
        if (bytes <= 0) {
            throw new BuildException(VIRTUAL_THREAD_STACK_HINT + "=" + virtualThreadStack
                    + " is not a stack size. Write a positive number of bytes, or a number followed by "
                    + "KB or MB, such as 2MB.");
        }
        return bytes;
    }
}
