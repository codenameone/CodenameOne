/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.build.Log;
import com.codename1.maven.PythonPackageResolver;
import com.codename1.maven.PythonProjectBuilder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/// Builds one workload the way `cn1:import-python-project` and the build
/// do: resolves the requirements from PyPI, then transpiles the program and
/// its packages and writes the package data.
///
///     HeroBuild <workload.py> <requirements.txt> <work dir> [<package dir>...]
///
/// Each package dir (an upstream test suite) is copied beside the program,
/// data files included, as an application's own packages are.
/// leaves Java sources in `<work>/gen` and `cn1python.dat` in
/// `<work>/classes`.
public final class HeroBuild {
    private HeroBuild() {
    }

    public static void main(String[] args) throws Exception {
        File work = new File(args[2]);
        File common = new File(work, "common");
        File python = new File(common, "src/main/python");
        python.mkdirs();
        Files.copy(new File(args[0]).toPath(), new File(python, "main.py").toPath());
        for (int i = 3; i < args.length; i++) {
            copyTree(new File(args[i]).toPath(), new File(python, new File(args[i]).getName()).toPath());
        }
        List<String> requirements = new ArrayList<String>();
        for (String line : Files.readAllLines(new File(args[1]).toPath(), StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#")) {
                requirements.add(t);
            }
        }
        Log log = new StderrLog();
        new PythonPackageResolver(log).resolve(requirements, common);
        File classes = new File(work, "classes");
        classes.mkdirs();
        new PythonProjectBuilder(python, new File(work, "gen"), "main", log).packageData(classes).run();
    }

    private static void copyTree(java.nio.file.Path from, java.nio.file.Path to) throws java.io.IOException {
        try (java.util.stream.Stream<java.nio.file.Path> walk = Files.walk(from)) {
            for (java.nio.file.Path p : (Iterable<java.nio.file.Path>) walk::iterator) {
                java.nio.file.Path dest = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(dest);
                } else if (!p.toString().contains("__pycache__")) {
                    Files.copy(p, dest);
                }
            }
        }
    }

    private static final class StderrLog implements Log {
        public boolean isDebugEnabled() {
            return false;
        }

        public void debug(CharSequence content) {
        }

        public void debug(CharSequence content, Throwable error) {
        }

        public void debug(Throwable error) {
        }

        public boolean isInfoEnabled() {
            return true;
        }

        public void info(CharSequence content) {
            System.err.println(content);
        }

        public void info(CharSequence content, Throwable error) {
            System.err.println(content);
        }

        public void info(Throwable error) {
            System.err.println(error);
        }

        public boolean isWarnEnabled() {
            return true;
        }

        public void warn(CharSequence content) {
            System.err.println("warning: " + content);
        }

        public void warn(CharSequence content, Throwable error) {
            System.err.println("warning: " + content);
        }

        public void warn(Throwable error) {
            System.err.println("warning: " + error);
        }

        public boolean isErrorEnabled() {
            return true;
        }

        public void error(CharSequence content) {
            System.err.println("error: " + content);
        }

        public void error(CharSequence content, Throwable error) {
            System.err.println("error: " + content);
        }

        public void error(Throwable error) {
            System.err.println("error: " + error);
        }
    }
}
