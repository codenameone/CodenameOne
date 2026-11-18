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
package com.codenameone.examples.brickyard.rbx.place;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// What an import found that will not behave as it did in Studio, so a
/// creator sees it before players do. Counts are per distinct thing (a class,
/// a `Class.Property`, an asset id); `where` lists say where it occurred.
public final class CompatReport {
    public int instances;
    public int scripts;
    /// Classes the engine does not know; each was loaded as a `Folder`.
    public final Map<String, Integer> unknownClasses = new TreeMap<String, Integer>();
    /// `Class.Property` pairs the engine does not know; the values were dropped.
    public final Map<String, Integer> unknownProperties = new TreeMap<String, Integer>();
    /// Values that could not be converted, one line each.
    public final List<String> badValues = new ArrayList<String>();
    /// Asset ids a place refers to but does not embed. Nothing is downloaded:
    /// each is drawn as a placeholder until the creator uploads the asset.
    public final Map<String, List<String>> missingAssets = new TreeMap<String, List<String>>();
    /// Script findings: services and globals the engine lacks, by script.
    public final Map<String, List<String>> scriptFindings = new TreeMap<String, List<String>>();
    /// What the file reader itself could not decode.
    public final List<String> readerNotes = new ArrayList<String>();

    static void count(Map<String, Integer> m, String k) {
        Integer v = m.get(k);
        m.put(k, Integer.valueOf(v == null ? 1 : v.intValue() + 1));
    }

    static void add(Map<String, List<String>> m, String k, String where) {
        List<String> l = m.get(k);
        if (l == null) {
            l = new ArrayList<String>();
            m.put(k, l);
        }
        if (!l.contains(where)) {
            l.add(where);
        }
    }

    /// True when nothing at all was lost.
    public boolean isClean() {
        return unknownClasses.isEmpty() && unknownProperties.isEmpty() && badValues.isEmpty()
                && missingAssets.isEmpty() && scriptFindings.isEmpty() && readerNotes.isEmpty();
    }

    /// The number of distinct problems, for a badge on the import screen.
    public int issueCount() {
        return unknownClasses.size() + unknownProperties.size() + badValues.size() + missingAssets.size()
                + scriptFindings.size() + readerNotes.size();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(instances).append(" instances, ").append(scripts).append(" scripts, ").append(issueCount())
                .append(" issues\n");
        section(b, "Unknown classes (loaded as Folder)", unknownClasses);
        section(b, "Unknown properties (dropped)", unknownProperties);
        list(b, "Unconvertible values", badValues);
        lists(b, "Assets to upload (shown as placeholders)", missingAssets);
        lists(b, "Script findings", scriptFindings);
        list(b, "File notes", readerNotes);
        return b.toString();
    }

    private static void section(StringBuilder b, String title, Map<String, Integer> m) {
        if (m.isEmpty()) {
            return;
        }
        b.append(title).append(":\n");
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            b.append("  ").append(e.getKey()).append(" x").append(e.getValue()).append('\n');
        }
    }

    private static void list(StringBuilder b, String title, List<String> l) {
        if (l.isEmpty()) {
            return;
        }
        b.append(title).append(":\n");
        for (String s : l) {
            b.append("  ").append(s).append('\n');
        }
    }

    private static void lists(StringBuilder b, String title, Map<String, List<String>> m) {
        if (m.isEmpty()) {
            return;
        }
        b.append(title).append(":\n");
        for (Map.Entry<String, List<String>> e : m.entrySet()) {
            b.append("  ").append(e.getKey()).append(": ");
            List<String> w = e.getValue();
            for (int i = 0; i < w.size() && i < 5; i++) {
                b.append(i == 0 ? "" : ", ").append(w.get(i));
            }
            if (w.size() > 5) {
                b.append(" and ").append(w.size() - 5).append(" more");
            }
            b.append('\n');
        }
    }
}
