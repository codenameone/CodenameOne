/*
 * Copyright (c) 2021, 2026, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.build.shared.BuildHints;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Joins a build hint contributed by a cn1lib's {@code codenameone_library_appended.properties}
 * onto the value the project's own {@code codenameone_settings.properties} already carries.
 *
 * <p>The merge used to be a bare {@code existing + addition}, which is correct only for the
 * XML-fragment hints the mechanism was designed around ({@code android.xpermissions} and
 * friends, where two fragments really do abut). For a hint whose value is a
 * <em>delimited list</em> it silently welds the last entry of one value onto the first entry
 * of the next: a project pinning its own Gradle dependencies plus a library adding one more
 * produced</p>
 *
 * <pre>implementation 'com.google.firebase:firebase-messaging:23.2.1'implementation 'com.scottyab:rootbeer-lib:0.0.8'</pre>
 *
 * <p>which Gradle parses as a method call on the first dependency and rejects with a Groovy
 * stack trace naming a {@code build.gradle} line the developer never wrote. Library authors
 * worked around it by baking a separator into their own value, but the two halves of that
 * convention never agreed -- CN1JailbreakDetect ships {@code ios.pods} with a <em>leading</em>
 * comma and {@code android.gradleDep} with a <em>trailing</em> semicolon, and only the leading
 * one can work, because the library value is always the right-hand side of the join.</p>
 *
 * <p>So the separator is decided here, from the hint, rather than by each library guessing.
 * Values already carrying a separator on the joining edge are accepted as-is rather than
 * doubled, so libraries written to either convention merge correctly and unchanged.</p>
 */
public class LibraryHintMerger {

    /** Prefix every build hint carries inside a settings/library properties file. */
    private static final String ARG_PREFIX = "codename1.arg.";

    private LibraryHintMerger() {
    }

    /**
     * The separator two values of this hint must be joined with, or an empty string when the
     * hint's values abut directly (the XML-fragment hints).
     *
     * <p>The table lives in {@link BuildHints}, which is also what the build hint annotations
     * are generated from. Keeping one copy is what stops a {@code String[]} attribute being
     * joined with one delimiter here and split with another by the builder.</p>
     *
     * @param propertyName hint name, with or without the {@code codename1.arg.} prefix
     * @return the separator, never null
     */
    public static String separatorFor(String propertyName) {
        if (propertyName == null) {
            return "";
        }
        String name = propertyName.startsWith(ARG_PREFIX)
                ? propertyName.substring(ARG_PREFIX.length())
                : propertyName;
        return BuildHints.separatorFor(name);
    }

    /**
     * Whether {@code existing} already declares what {@code addition} would add.
     *
     * <p>Compared with the addition's own separators and surrounding whitespace trimmed off,
     * because a library value's separator is decoration for the join rather than part of the
     * thing being added -- {@code "implementation 'x:y:1';"} and {@code ";implementation
     * 'x:y:1'"} both mean the same dependency, and a project that pinned it by hand wrote
     * neither form exactly.</p>
     *
     * @param propertyName hint name, with or without the {@code codename1.arg.} prefix
     * @param existing the project's current value, may be null
     * @param addition the library's contribution, may be null
     * @return true when the addition would be redundant
     */
    public static boolean alreadyContains(String propertyName, String existing, String addition) {
        if (existing == null || addition == null) {
            return false;
        }
        String needle = trimEdges(addition, separatorFor(propertyName));
        return needle.length() > 0 && existing.contains(needle);
    }

    /**
     * Joins a library's contribution onto the project's existing value for a hint.
     *
     * @param propertyName hint name, with or without the {@code codename1.arg.} prefix
     * @param existing the project's current value, may be null
     * @param addition the library's contribution, may be null
     * @return the merged value, never null
     */
    public static String append(String propertyName, String existing, String addition) {
        String left = existing == null ? "" : existing;
        String right = addition == null ? "" : addition;
        String separator = separatorFor(propertyName);
        if (separator.length() == 0) {
            // Historical behaviour, and the right one for an XML fragment.
            return left + right;
        }
        String leftTrimmed = trimEdges(left, separator);
        String rightTrimmed = trimEdges(right, separator);
        if (leftTrimmed.length() == 0) {
            return rightTrimmed;
        }
        if (rightTrimmed.length() == 0) {
            return leftTrimmed;
        }
        return leftTrimmed + separator + rightTrimmed;
    }

    /**
     * A Gradle configuration keyword opening a dependency declaration, sitting directly after
     * the closing quote of the previous one with no {@code ;} or newline in between.
     *
     * <p>Groovy reads that as a method call on the dependency the previous statement returned,
     * and reports it as "Could not find method implementation() ... on
     * DefaultExternalModuleDependency" against a generated {@code build.gradle} line the
     * developer never wrote. Naming the hint instead is the difference between a one line fix
     * and reading a Gradle stack trace.</p>
     */
    private static final Pattern UNSEPARATED_STATEMENT = Pattern.compile(
            "['\"][ \\t]*(implementation|api|compile|compileOnly|runtimeOnly|annotationProcessor"
            + "|kapt|testImplementation|androidTestImplementation|debugImplementation"
            + "|releaseImplementation)[ \\t]*['\"(]");

    /**
     * Checks a merged Gradle dependency hint for two declarations run together without a
     * separator.
     *
     * @param propertyName hint name, with or without the {@code codename1.arg.} prefix
     * @param value the merged value to check, may be null
     * @return an actionable description of the problem, or null when the value is well formed
     */
    public static String findUnseparatedStatement(String propertyName, String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        Matcher matcher = UNSEPARATED_STATEMENT.matcher(value);
        if (!matcher.find()) {
            return null;
        }
        int from = Math.max(0, matcher.start() - 40);
        int to = Math.min(value.length(), matcher.end() + 40);
        String excerpt = (from > 0 ? "..." : "") + value.substring(from, to)
                + (to < value.length() ? "..." : "");
        return propertyName + " runs two Gradle statements together with no separator between "
                + "them, which Gradle rejects as a method call on the preceding dependency:\n\n    "
                + excerpt + "\n\nSeparate them with ';' or a newline. If you did not write this "
                + "value by hand, a cn1lib appended to it: a library's "
                + "codenameone_library_appended.properties contributes to this hint, and a "
                + "library value that carries no leading separator used to be welded onto the "
                + "end of yours. Update the Codename One Maven plugin, or add a ';' to the end "
                + "of your own value.";
    }

    /**
     * Strips whitespace and the hint's separator from both ends of a value, so the join
     * decides the separator exactly once regardless of which convention the library followed.
     */
    private static String trimEdges(String value, String separator) {
        String result = value.trim();
        if (separator.length() == 0) {
            return result;
        }
        boolean changed = true;
        while (changed && result.length() > 0) {
            changed = false;
            if (result.startsWith(separator)) {
                result = result.substring(separator.length()).trim();
                changed = true;
            }
            if (result.endsWith(separator)) {
                result = result.substring(0, result.length() - separator.length()).trim();
                changed = true;
            }
        }
        return result;
    }
}
