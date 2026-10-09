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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// The nearest supported alternative to an API a device does not have.
///
/// The rows live in `compliance-alternatives.txt` beside this class, one per
/// line, so that adding one is adding a line; the file's own header
/// describes the format. A row names the classes and members its advice
/// points at, and a test resolves every one of them against the built
/// Codename One jars: advice cannot outlive what it recommends.
public final class ComplianceAlternatives {

    static final String RESOURCE = "compliance-alternatives.txt";

    /// One row of the table.
    public static final class Row {
        private final String prefix;
        private final List<String> references;
        private final String advice;

        Row(String prefix, List<String> references, String advice) {
            this.prefix = prefix;
            this.references = references;
            this.advice = advice;
        }

        /// The API the row is about: a package ending in a dot, a class or
        /// a member, as Java spells it.
        public String prefix() {
            return prefix;
        }

        /// What the advice names: `a.b.C` or `a.b.C#member`. Empty when the
        /// row says there is no equivalent.
        public List<String> references() {
            return references;
        }

        /// What to tell the developer.
        public String advice() {
            return advice;
        }
    }

    private static final List<Row> ROWS = load();

    private ComplianceAlternatives() {
    }

    /// Every row, in the file's order.
    public static List<Row> rows() {
        return ROWS;
    }

    private static List<Row> load() {
        InputStream in = ComplianceAlternatives.class.getResourceAsStream(RESOURCE);
        if (in == null) {
            throw new IllegalStateException(RESOURCE + " is missing beside " + ComplianceAlternatives.class.getName());
        }
        try {
            try {
                return parse(new String(DependencyClassifier.readAll(in), StandardCharsets.UTF_8));
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + RESOURCE, e);
        }
    }

    /// Reads a table. A line that is not a row -- three columns separated by
    /// `|`, the first and the last not empty -- is an error, not a row to
    /// skip: a typo would otherwise silently drop advice.
    static List<Row> parse(String text) {
        List<Row> rows = new ArrayList<Row>();
        int number = 0;
        for (String line : text.split("\n")) {
            number++;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.charAt(0) == '#') {
                continue;
            }
            int first = trimmed.indexOf('|');
            int second = first < 0 ? -1 : trimmed.indexOf('|', first + 1);
            if (second < 0) {
                throw new IllegalArgumentException(RESOURCE + ":" + number + ": expected \"api | names | advice\"");
            }
            String prefix = trimmed.substring(0, first).trim();
            String advice = trimmed.substring(second + 1).trim();
            if (prefix.isEmpty() || advice.isEmpty()) {
                throw new IllegalArgumentException(RESOURCE + ":" + number + ": the API and the advice are required");
            }
            List<String> references = new ArrayList<String>();
            for (String r : trimmed.substring(first + 1, second).split(",")) {
                if (!r.trim().isEmpty()) {
                    references.add(r.trim());
                }
            }
            rows.add(new Row(prefix, Collections.unmodifiableList(references), advice));
        }
        return Collections.unmodifiableList(rows);
    }

    /// The advice for `api`, or null. `api` is a class (`java.sql.Connection`)
    /// or a member of one (`java.lang.Runtime.exec`), as Java spells it and
    /// without a parameter list. The row with the longest matching prefix
    /// wins, so a member's row beats its class's and a class's its package's.
    public static String lookup(String api) {
        Row row = row(api);
        return row == null ? null : row.advice;
    }

    static Row row(String api) {
        if (api == null) {
            return null;
        }
        Row best = null;
        for (Row row : ROWS) {
            if (matches(row.prefix, api) && (best == null || row.prefix.length() > best.prefix.length())) {
                best = row;
            }
        }
        return best;
    }

    /// Whether `prefix` covers `api`: a package covers everything in it, a
    /// class itself, its nested classes and its members, and a member every
    /// member whose name starts with it (`Class.getDeclared` covers
    /// `getDeclaredFields`).
    private static boolean matches(String prefix, String api) {
        if (!api.startsWith(prefix)) {
            return false;
        }
        if (prefix.endsWith(".") || api.length() == prefix.length()) {
            return true;
        }
        char next = api.charAt(prefix.length());
        if (next == '.' || next == '$') {
            return true;
        }
        // A member prefix: the last part starts in lower case.
        int dot = prefix.lastIndexOf('.');
        return dot >= 0 && dot + 1 < prefix.length() && Character.isLowerCase(prefix.charAt(dot + 1))
                && api.indexOf('.', prefix.length()) < 0;
    }
}
