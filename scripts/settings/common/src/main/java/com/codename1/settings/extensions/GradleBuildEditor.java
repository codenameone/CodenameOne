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
package com.codename1.settings.extensions;

import java.util.ArrayList;
import java.util.List;

/// The Gradle [DependencyEditor]: declares dependencies in the top-level
/// `dependencies { }` block of a `build.gradle.kts` (or Groovy `build.gradle`).
///
/// A library is written in string notation, as `implementation("g:a:v")`, and a
/// cn1lib as `cn1lib("g:a:v")` -- the configuration the Codename One Gradle
/// plugin resolves cn1libs through. That split mirrors [PomEditor]: in a POM a
/// cn1lib is the dependency declared `<type>pom</type>` (the cn1lib's POM pulls
/// in its common jar and per-platform native jars), so the same
/// [MavenDependency#type()] decides the configuration here. The call form with
/// parentheses and double quotes is valid in both Kotlin and Groovy scripts, so
/// one spelling serves both.
///
/// Only the TOP-LEVEL block is edited. A `dependencies { }` nested in
/// `buildscript { }`, `subprojects { }` or a `pluginManagement` block declares
/// something else (the build's own classpath, other projects), so the scanner
/// tracks brace depth and skips comments and string literals, which may contain
/// braces of their own. When there is no top-level block, one is appended.
///
/// Matching is by group and artifact in string notation (`"g:a"` or
/// `"g:a:anything"`), inside any declaration in the block. A declaration in map
/// notation (`group = "g", name = "a"`) is not recognized: that form is rare in
/// Kotlin scripts, and missing it only means an add appends a second
/// declaration Gradle resolves to the same module.
public final class GradleBuildEditor implements DependencyEditor {
    /// The shared instance [DependencyEditor#forBuildSystem(String)] returns.
    public static final GradleBuildEditor INSTANCE = new GradleBuildEditor();

    private static final String DEFAULT_INDENT = "    ";

    private GradleBuildEditor() {
    }

    @Override
    public boolean contains(String text, MavenDependency dependency) {
        return containsDependency(text, dependency);
    }

    @Override
    public String add(String text, MavenDependency dependency) {
        return addDependency(text, dependency);
    }

    @Override
    public String remove(String text, MavenDependency dependency) {
        return removeDependency(text, dependency);
    }

    @Override
    public String fileLabel() {
        return "build.gradle.kts";
    }

    /// Whether the top-level `dependencies { }` block declares `dependency`.
    public static boolean containsDependency(String script, MavenDependency dependency) {
        if (script == null || dependency == null) {
            return false;
        }
        return findDeclaration(script, dependency) != null;
    }

    /// `script` with `dependency` declared at the end of the top-level
    /// `dependencies { }` block, appending the block when there is none.
    public static String addDependency(String script, MavenDependency dependency) {
        if (script == null) {
            script = "";
        }
        if (dependency == null || !dependency.isValid() || containsDependency(script, dependency)) {
            return script;
        }
        int[] block = dependenciesBlock(script);
        if (block == null) {
            StringBuilder sb = new StringBuilder(script);
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                sb.append('\n');
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("dependencies {\n")
                    .append(DEFAULT_INDENT).append(declaration(dependency)).append('\n')
                    .append("}\n");
            return sb.toString();
        }
        int close = block[1];
        String indent = statementIndent(script, block);
        int lineStart = close;
        while (lineStart > 0 && (script.charAt(lineStart - 1) == ' ' || script.charAt(lineStart - 1) == '\t')) {
            lineStart--;
        }
        String line = indent + declaration(dependency) + "\n";
        if (lineStart == 0 || script.charAt(lineStart - 1) == '\n') {
            // The closing brace is on a line of its own: the new declaration
            // becomes the block's last line, just above it.
            return script.substring(0, lineStart) + line + script.substring(lineStart);
        }
        // `dependencies { a() }` on one line: open a line before the brace.
        return script.substring(0, close) + "\n" + line + script.substring(close);
    }

    /// `script` without the declaration of `dependency`, removing the whole
    /// line(s) it occupied.
    public static String removeDependency(String script, MavenDependency dependency) {
        if (script == null || dependency == null) {
            return script;
        }
        int[] decl = findDeclaration(script, dependency);
        if (decl == null) {
            return script;
        }
        int lineStart = decl[0];
        while (lineStart > 0 && (script.charAt(lineStart - 1) == ' ' || script.charAt(lineStart - 1) == '\t')) {
            lineStart--;
        }
        boolean wholeLine = lineStart == 0 || script.charAt(lineStart - 1) == '\n';
        if (!wholeLine) {
            // Something else precedes the declaration on its line; cut only
            // the declaration itself.
            lineStart = decl[0];
        }
        int lineEnd = decl[1];
        while (lineEnd < script.length() && (script.charAt(lineEnd) == ' ' || script.charAt(lineEnd) == '\t'
                || script.charAt(lineEnd) == ';')) {
            lineEnd++;
        }
        if (wholeLine && lineEnd + 1 < script.length() && script.charAt(lineEnd) == '\r'
                && script.charAt(lineEnd + 1) == '\n') {
            lineEnd++;
        }
        if (lineEnd < script.length() && script.charAt(lineEnd) == '\n' && wholeLine) {
            lineEnd++;
        }
        return script.substring(0, lineStart) + script.substring(lineEnd);
    }

    /// The declaration line for `dependency`, without indentation.
    static String declaration(MavenDependency dependency) {
        String configuration = "pom".equals(dependency.type()) ? "cn1lib" : "implementation";
        StringBuilder coords = new StringBuilder();
        coords.append(dependency.groupId()).append(':').append(dependency.artifactId());
        String version = dependency.version();
        // A catalog version such as ${cn1.version} names a MAVEN property. In a
        // Kotlin or Groovy string it would be a template expression referring to
        // a variable the script does not have, which fails the build script
        // itself. The Codename One Gradle plugin gives a com.codenameone module
        // declared without a version the framework's version (ProjectSupport),
        // which is what the Maven property does in the archetype's POM, so the
        // coordinate is written without one.
        if (version.length() > 0 && !version.startsWith("${")) {
            coords.append(':').append(version);
        }
        return configuration + "(\"" + escape(coords.toString()) + "\")";
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '"' || c == '$') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /// {start, end} of the statement in the dependencies block declaring
    /// `dependency`, or null. Only the block's own statements count: a named
    /// block nested in it -- `constraints { implementation("g:a:1") }` -- holds
    /// rules about versions, not dependencies, and puts nothing on a classpath.
    /// Counting it would make installing that library a no-op, and uninstalling
    /// it remove the constraint instead.
    private static int[] findDeclaration(String script, MavenDependency dependency) {
        int[] block = dependenciesBlock(script);
        return block == null ? null : findIn(script, block, dependency);
    }

    private static int[] findIn(String s, int[] block, MavenDependency dependency) {
        for (int[] statement : statements(s, block)) {
            int brace = headBrace(s, statement);
            if (brace >= 0 && isIdentifier(s.substring(statement[0], brace).trim())) {
                // A named block (constraints, components...): not a declaration.
                continue;
            }
            // Only a declaration that puts the library on the application's
            // classpath counts: testImplementation or compileOnly does not install
            // it, and uninstalling must not remove one of those instead.
            if (!installs(configuration(s, statement[0]), dependency)) {
                continue;
            }
            // Only the declaration's own coordinate counts, not strings in its
            // configuration block ({ exclude(...) }).
            int headEnd = brace >= 0 ? brace : statement[1];
            for (String literal : stringLiterals(s, statement[0], headEnd)) {
                if (matches(literal, dependency)) {
                    return statement;
                }
            }
        }
        return null;
    }

    /// The configuration a declaration at `start` names: its leading
    /// identifier (`implementation(...)`, Groovy's `implementation '...'`) or
    /// the quoted name of Kotlin's `"implementation"(...)` form; "" if neither.
    static String configuration(String s, int start) {
        int i = start;
        while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t' || s.charAt(i) == '\n'
                || s.charAt(i) == '\r')) {
            i++;
        }
        if (i < s.length() && s.charAt(i) == '"') {
            int close = s.indexOf('"', i + 1);
            return close < 0 ? "" : s.substring(i + 1, close);
        }
        int end = i;
        while (end < s.length() && (isNameStart(s.charAt(end)) || s.charAt(end) >= '0' && s.charAt(end) <= '9')) {
            end++;
        }
        return s.substring(i, end);
    }

    /// Whether declaring `dependency` in `configuration` installs it: a cn1lib
    /// (`pom` type) only through `cn1lib`, which brings its platform jars too; a
    /// library through `implementation`, `api` or `cn1lib` (which the
    /// application's implementation extends).
    static boolean installs(String configuration, MavenDependency dependency) {
        if ("pom".equals(dependency.type())) {
            return "cn1lib".equals(configuration);
        }
        return "implementation".equals(configuration) || "api".equals(configuration)
                || "cn1lib".equals(configuration);
    }

    /// Whether `head` is a bare (possibly dotted) name -- a block such as
    /// `constraints`, not a declaration. By hand: this runs on the Codename One
    /// runtime, which has neither String.matches nor Character.isLetter.
    private static boolean isIdentifier(String head) {
        if (head.isEmpty() || !isNameStart(head.charAt(0))) {
            return false;
        }
        for (int i = 1; i < head.length(); i++) {
            char c = head.charAt(i);
            if (!(isNameStart(c) || c >= '0' && c <= '9' || c == '.')) {
                return false;
            }
        }
        return true;
    }

    /// ASCII only, by range: Gradle block names are, and the runtime has no
    /// Character.isLetter.
    private static boolean isNameStart(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';
    }

    /// The first `{` in `statement` outside any parentheses, strings and
    /// comments, or -1.
    private static int headBrace(String s, int[] statement) {
        int depth = 0;
        int i = statement[0];
        while (i < statement[1]) {
            int skipped = skipCommentOrString(s, i);
            if (skipped != i) {
                i = skipped;
                continue;
            }
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == '{' && depth <= 0) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private static boolean matches(String coordinate, MavenDependency dependency) {
        String ga = dependency.groupId() + ":" + dependency.artifactId();
        return coordinate.equals(ga) || coordinate.startsWith(ga + ":") || coordinate.startsWith(ga + "@");
    }

    /// The indentation of the block's first statement, or four spaces.
    private static String statementIndent(String script, int[] block) {
        List<int[]> statements = statements(script, block);
        if (!statements.isEmpty()) {
            int start = statements.get(0)[0];
            int lineStart = start;
            while (lineStart > 0 && (script.charAt(lineStart - 1) == ' ' || script.charAt(lineStart - 1) == '\t')) {
                lineStart--;
            }
            if (lineStart == 0 || script.charAt(lineStart - 1) == '\n') {
                return script.substring(lineStart, start);
            }
        }
        return DEFAULT_INDENT;
    }

    /// {index of `{`, index of the matching `}`} of the top-level
    /// `dependencies` block, or null when there is none.
    static int[] dependenciesBlock(String s) {
        int depth = 0;
        int i = 0;
        int n = s.length();
        while (i < n) {
            int skipped = skipCommentOrString(s, i);
            if (skipped != i) {
                i = skipped;
                continue;
            }
            char c = s.charAt(i);
            if (c == '{') {
                depth++;
                i++;
                continue;
            }
            if (c == '}') {
                depth--;
                i++;
                continue;
            }
            if (depth == 0 && Character.isJavaIdentifierStart(c)
                    && (i == 0 || !Character.isJavaIdentifierPart(s.charAt(i - 1)) && s.charAt(i - 1) != '.')) {
                int end = i;
                while (end < n && Character.isJavaIdentifierPart(s.charAt(end))) {
                    end++;
                }
                if ("dependencies".equals(s.substring(i, end))) {
                    int j = end;
                    while (j < n && Character.isWhitespace(s.charAt(j))) {
                        j++;
                    }
                    if (j < n && s.charAt(j) == '{') {
                        int close = matchingBrace(s, j);
                        if (close >= 0) {
                            return new int[]{j, close};
                        }
                        return null;
                    }
                }
                i = end;
                continue;
            }
            i++;
        }
        return null;
    }

    private static int matchingBrace(String s, int open) {
        int depth = 0;
        int i = open;
        while (i < s.length()) {
            int skipped = skipCommentOrString(s, i);
            if (skipped != i) {
                i = skipped;
                continue;
            }
            char c = s.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
            i++;
        }
        return -1;
    }

    /// The statements directly inside `block`, as {start, end}: from the first
    /// character of the statement to the end of its last line (a declaration
    /// can carry a `{ exclude(...) }` configuration spanning several lines).
    private static List<int[]> statements(String s, int[] block) {
        List<int[]> out = new ArrayList<int[]>();
        int i = block[0] + 1;
        int end = block[1];
        int start = -1;
        int depth = 0;
        while (i < end) {
            int skipped = skipCommentOrString(s, i);
            if (skipped != i) {
                boolean comment = s.startsWith("//", i) || s.startsWith("/*", i);
                if (!comment && start < 0) {
                    start = i;
                }
                i = skipped;
                continue;
            }
            char c = s.charAt(i);
            if (start < 0) {
                if (!Character.isWhitespace(c) && c != ';') {
                    start = i;
                } else {
                    i++;
                    continue;
                }
            }
            if (c == '(' || c == '{' || c == '[') {
                depth++;
            } else if (c == ')' || c == '}' || c == ']') {
                depth--;
            } else if ((c == '\n' || c == ';') && depth <= 0) {
                out.add(new int[]{start, trimEnd(s, start, i)});
                start = -1;
                depth = 0;
            }
            i++;
        }
        if (start >= 0) {
            out.add(new int[]{start, trimEnd(s, start, end)});
        }
        return out;
    }

    private static int trimEnd(String s, int start, int end) {
        while (end > start && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return end;
    }

    /// The contents of every string literal between `from` and `to`.
    private static List<String> stringLiterals(String s, int from, int to) {
        List<String> out = new ArrayList<String>();
        int i = from;
        while (i < to) {
            int skipped = skipCommentOrString(s, i);
            if (skipped == i) {
                i++;
                continue;
            }
            char c = s.charAt(i);
            if (c == '"' || c == '\'') {
                int quote = s.startsWith("\"\"\"", i) ? 3 : 1;
                int contentEnd = Math.max(i + quote, skipped - quote);
                out.add(s.substring(i + quote, Math.min(contentEnd, s.length())));
            }
            i = skipped;
        }
        return out;
    }

    /// The index just past the comment or string literal starting at `i`, or
    /// `i` itself when none starts there.
    private static int skipCommentOrString(String s, int i) {
        int n = s.length();
        char c = s.charAt(i);
        if (c == '/' && i + 1 < n) {
            char next = s.charAt(i + 1);
            if (next == '/') {
                int nl = s.indexOf('\n', i);
                // Stop AT the newline so it still ends the statement.
                return nl < 0 ? n : nl;
            }
            if (next == '*') {
                int close = s.indexOf("*/", i + 2);
                return close < 0 ? n : close + 2;
            }
            return i;
        }
        if (c == '"' && s.startsWith("\"\"\"", i)) {
            int close = s.indexOf("\"\"\"", i + 3);
            return close < 0 ? n : close + 3;
        }
        if (c == '"' || c == '\'') {
            int j = i + 1;
            while (j < n) {
                char d = s.charAt(j);
                if (d == '\\') {
                    j += 2;
                    continue;
                }
                if (d == c) {
                    return j + 1;
                }
                if (d == '\n') {
                    // An unterminated literal ends at the line; never let one
                    // stray quote swallow the rest of the script.
                    return j;
                }
                j++;
            }
            return n;
        }
        return i;
    }
}
