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
package com.codename1.maven;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/// The test classes Surefire runs, as the compiled backend test run must select
/// them: its `includes` and `excludes` (Surefire's defaults when the pom names
/// none), or `-Dtest` when that is given. The two runs are meant to exercise one
/// set of tests; a helper class with an `@Test` method, or an `IntegrationSpec`
/// the includes leave out, must not run in one and not the other.
///
/// Patterns are matched as Surefire matches them, against the class's path with
/// a `.java` suffix: `**` crosses directories, `*` and `?` do not. Tag selection
/// (`groups`, `excludedGroups`) is not read; a compiled run of a tagged-out class
/// is reported like any other.
final class SurefireSelection implements java.util.function.Predicate<String> {
    static final List<String> DEFAULT_INCLUDES = Arrays.asList(
            "**/Test*.java", "**/*Test.java", "**/*Tests.java", "**/*TestCase.java");
    static final List<String> DEFAULT_EXCLUDES = Arrays.asList("**/*$*");

    private final List<Pattern> includes = new ArrayList<Pattern>();
    private final List<Pattern> excludes = new ArrayList<Pattern>();

    SurefireSelection(List<String> includes, List<String> excludes) {
        for (String p : includes) {
            this.includes.add(glob(p));
        }
        for (String p : excludes) {
            this.excludes.add(glob(p));
        }
    }

    /// The selection `project`'s Surefire makes; `test` is the `-Dtest` value, or null.
    static SurefireSelection of(MavenProject project, String test) {
        if (test != null && test.trim().length() > 0) {
            List<String> named = new ArrayList<String>();
            List<String> excluded = new ArrayList<String>(DEFAULT_EXCLUDES);
            for (String entry : test.split(",")) {
                String e = entry.trim();
                // A "!" entry excludes, as Surefire reads it: -Dtest='*Test,!SlowTest'
                // must leave SlowTest out of the compiled run as it does the JVM's.
                boolean negative = e.startsWith("!");
                if (negative) {
                    e = e.substring(1).trim();
                }
                int hash = e.indexOf('#');
                if (hash >= 0) {
                    // A method selector narrows a class; a class it excludes by
                    // method is still run for its other methods.
                    if (negative) {
                        continue;
                    }
                    e = e.substring(0, hash);
                }
                if (e.length() == 0) {
                    continue;
                }
                // A bare class name pattern names a class in any package.
                String pattern = e.indexOf('/') >= 0 || e.endsWith(".java") ? e
                        : "**/" + e.replace('.', '/') + ".java";
                (negative ? excluded : named).add(pattern);
            }
            // Only exclusions: Surefire runs its usual includes minus those.
            return new SurefireSelection(named.isEmpty() ? DEFAULT_INCLUDES : named, excluded);
        }
        Plugin surefire = project.getPlugin("org.apache.maven.plugins:maven-surefire-plugin");
        Xpp3Dom config = surefire == null || !(surefire.getConfiguration() instanceof Xpp3Dom) ? null
                : (Xpp3Dom) surefire.getConfiguration();
        List<String> includes = patterns(config, "includes", "include");
        List<String> excludes = patterns(config, "excludes", "exclude");
        return new SurefireSelection(includes == null ? DEFAULT_INCLUDES : includes,
                excludes == null ? DEFAULT_EXCLUDES : excludes);
    }

    private static List<String> patterns(Xpp3Dom config, String list, String item) {
        Xpp3Dom parent = config == null ? null : config.getChild(list);
        if (parent == null) {
            return null;
        }
        List<String> out = new ArrayList<String>();
        for (Xpp3Dom child : parent.getChildren(item)) {
            if (child.getValue() != null && child.getValue().trim().length() > 0) {
                out.add(child.getValue().trim());
            }
        }
        return out;
    }

    @Override
    public boolean test(String binaryName) {
        String path = binaryName.replace('.', '/') + ".java";
        boolean included = false;
        for (Pattern p : includes) {
            if (p.matcher(path).matches()) {
                included = true;
                break;
            }
        }
        if (!included) {
            return false;
        }
        for (Pattern p : excludes) {
            if (p.matcher(path).matches()) {
                return false;
            }
        }
        return true;
    }

    /// An Ant-style path pattern as a regular expression. A `.class` pattern is
    /// read as its `.java` counterpart, as Surefire treats the two alike.
    static Pattern glob(String pattern) {
        String p = pattern.replace('\\', '/');
        if (p.endsWith(".class")) {
            p = p.substring(0, p.length() - ".class".length()) + ".java";
        }
        StringBuilder re = new StringBuilder();
        for (int i = 0 ; i < p.length() ; i++) {
            char c = p.charAt(i);
            if (c == '*' && i + 1 < p.length() && p.charAt(i + 1) == '*') {
                // "**/" matches any number of directories, none included.
                if (i + 2 < p.length() && p.charAt(i + 2) == '/') {
                    re.append("(?:.*/)?");
                    i += 2;
                } else {
                    re.append(".*");
                    i++;
                }
            } else if (c == '*') {
                re.append("[^/]*");
            } else if (c == '?') {
                re.append("[^/]");
            } else if ("\\.[]{}()+-^$|".indexOf(c) >= 0) {
                re.append('\\').append(c);
            } else {
                re.append(c);
            }
        }
        return Pattern.compile(re.toString());
    }
}
