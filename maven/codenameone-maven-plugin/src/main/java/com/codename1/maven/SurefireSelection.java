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
    /// `-Dtest` entries that name a whole class, which select all its methods.
    private final List<Pattern> wholeClasses = new ArrayList<Pattern>();
    /// `-Dtest` method selectors, `{class, method}`: `ApiTest#greets` selects only
    /// that method of the class, `!ApiTest#slow` leaves just that one out.
    private final List<Pattern[]> methods = new ArrayList<Pattern[]>();
    private final List<Pattern[]> excludedMethods = new ArrayList<Pattern[]>();

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
            List<String> whole = new ArrayList<String>();
            List<String[]> methods = new ArrayList<String[]>();
            List<String[]> excludedMethods = new ArrayList<String[]>();
            for (String entry : test.split(",")) {
                String e = entry.trim();
                // A "!" entry excludes, as Surefire reads it: -Dtest='*Test,!SlowTest'
                // must leave SlowTest out of the compiled run as it does the JVM's.
                boolean negative = e.startsWith("!");
                if (negative) {
                    e = e.substring(1).trim();
                }
                String method = null;
                boolean regex = e.startsWith(REGEX_PREFIX);
                int hash = e.indexOf('#', regex ? Math.max(e.indexOf(']'), 0) : 0);
                if (hash >= 0) {
                    method = e.substring(hash + 1).trim();
                    e = e.substring(0, hash);
                }
                if (e.length() == 0) {
                    continue;
                }
                // A bare class name pattern names a class in any package; a regex is
                // Surefire's own syntax and is kept as written.
                String pattern = regex || e.indexOf('/') >= 0 || e.endsWith(".java") ? e
                        : "**/" + e.replace('.', '/') + ".java";
                if (method != null && method.length() > 0) {
                    // A method selector narrows a class rather than excluding it:
                    // a class excluded by method still runs its other methods.
                    (negative ? excludedMethods : methods).add(new String[] {pattern, method});
                    if (!negative) {
                        named.add(pattern);
                    }
                } else {
                    (negative ? excluded : named).add(pattern);
                    if (!negative) {
                        whole.add(pattern);
                    }
                }
            }
            // Only exclusions: Surefire runs its usual includes minus those.
            SurefireSelection out = new SurefireSelection(named.isEmpty() ? DEFAULT_INCLUDES : named,
                    excluded);
            for (String p : whole) {
                out.wholeClasses.add(glob(p));
            }
            for (String[] m : methods) {
                out.methods.add(new Pattern[] {glob(m[0]), methodGlob(m[1])});
            }
            for (String[] m : excludedMethods) {
                out.excludedMethods.add(new Pattern[] {glob(m[0]), methodGlob(m[1])});
            }
            return out;
        }
        Plugin surefire = project.getPlugin("org.apache.maven.plugins:maven-surefire-plugin");
        Xpp3Dom config = surefire == null || !(surefire.getConfiguration() instanceof Xpp3Dom) ? null
                : (Xpp3Dom) surefire.getConfiguration();
        if (surefire != null) {
            // The test phase's run is the default-test execution: filters a pom puts
            // under <execution><configuration> are what it runs with, the plugin's
            // own configuration beneath them, as Maven merges the two.
            for (org.apache.maven.model.PluginExecution execution : surefire.getExecutions()) {
                if ("default-test".equals(execution.getId())
                        && execution.getConfiguration() instanceof Xpp3Dom) {
                    Xpp3Dom own = new Xpp3Dom((Xpp3Dom) execution.getConfiguration());
                    config = config == null ? own : Xpp3Dom.mergeXpp3Dom(own, new Xpp3Dom(config));
                }
            }
        }
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

    /// Whether the run selects the class `binaryName`, or -- given as
    /// `binaryName#method` -- that test method of a selected class.
    @Override
    public boolean test(String binaryName) {
        int hash = binaryName.indexOf('#');
        if (hash >= 0) {
            String cls = binaryName.substring(0, hash);
            return test(cls) && selectsMethod(cls, binaryName.substring(hash + 1));
        }
        boolean included = false;
        for (Pattern p : includes) {
            if (matches(p, binaryName)) {
                included = true;
                break;
            }
        }
        if (!included) {
            return false;
        }
        for (Pattern p : excludes) {
            if (matches(p, binaryName)) {
                return false;
            }
        }
        return true;
    }

    /// Whether `-Dtest` lets test `method` of class `binaryName` run: every method
    /// when the class was named whole or no method selector names it, else only the
    /// methods its selectors name -- and never one a `!` selector names.
    private boolean selectsMethod(String binaryName, String method) {
        for (Pattern[] m : excludedMethods) {
            if (matches(m[0], binaryName) && m[1].matcher(method).matches()) {
                return false;
            }
        }
        for (Pattern p : wholeClasses) {
            if (matches(p, binaryName)) {
                return true;
            }
        }
        boolean narrowed = false;
        for (Pattern[] m : methods) {
            if (matches(m[0], binaryName)) {
                if (m[1].matcher(method).matches()) {
                    return true;
                }
                narrowed = true;
            }
        }
        return !narrowed;
    }

    /// A Surefire method selector as a regular expression: `+` separates
    /// alternatives (`#one+two`), `*` matches anything and `?` one character.
    static Pattern methodGlob(String pattern) {
        StringBuilder re = new StringBuilder();
        for (String alternative : pattern.split("\\+")) {
            if (re.length() > 0) {
                re.append('|');
            }
            re.append("(?:");
            for (int i = 0 ; i < alternative.length() ; i++) {
                char c = alternative.charAt(i);
                if (c == '*') {
                    re.append(".*");
                } else if (c == '?') {
                    re.append('.');
                } else if ("\\.[]{}()+-^$|".indexOf(c) >= 0) {
                    re.append('\\').append(c);
                } else {
                    re.append(c);
                }
            }
            re.append(')');
        }
        return Pattern.compile(re.toString());
    }

    /// An Ant-style path pattern as a regular expression. A `.class` pattern is
    /// read as its `.java` counterpart, as Surefire treats the two alike.
    /// Whether `pattern` selects the class `binaryName`. A glob is matched against
    /// the class's `.java` path, as Surefire matches it; a `%regex[...]` pattern
    /// against its `.class` path, as Surefire documents for regex selection. A glob
    /// always ends in `.java` (a `.class` one is read as such), so trying both
    /// forms cannot widen what it selects.
    private static boolean matches(Pattern pattern, String binaryName) {
        String path = binaryName.replace('.', '/');
        return pattern.matcher(path + ".java").matches() || pattern.matcher(path + ".class").matches();
    }

    private static final String REGEX_PREFIX = "%regex[";

    static Pattern glob(String pattern) {
        if (pattern.startsWith(REGEX_PREFIX) && pattern.endsWith("]")) {
            // Surefire's regex selection: the pattern between the brackets is a Java
            // regular expression over the class's path. Fed to the glob converter its
            // operators were literal characters, and it selected nothing at all.
            return Pattern.compile(pattern.substring(REGEX_PREFIX.length(), pattern.length() - 1));
        }
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
