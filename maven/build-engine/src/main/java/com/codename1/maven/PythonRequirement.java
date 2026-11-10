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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/// One requirement as `requirements.txt`, `pyproject.toml` and a wheel's
/// `Requires-Dist` write it (PEP 508): `name[extra] >=1.2,<2 ; marker`.
///
/// Versions compare as PEP 440 orders them -- release segments, then
/// `a`/`b`/`rc` pre-releases, post- and dev-releases -- and specifiers
/// cover `==` (with `.*`), `!=`, `<`, `<=`, `>`, `>=`, `~=` and `===`.
/// Markers are evaluated for the environment a Codename One build targets:
/// CPython 3.13 on a POSIX system, so a dependency only Windows needs is
/// left out.
final class PythonRequirement {
    /// The marker environment: what `python_version` and the rest answer.
    static final String PYTHON_VERSION = "3.13";

    final String name;
    final List<String> extras = new ArrayList<String>();
    final List<String[]> specifiers = new ArrayList<String[]>();
    final String marker;

    private PythonRequirement(String name, String marker) {
        this.name = name;
        this.marker = marker;
    }

    /// PEP 503's normal form, which PyPI's URLs and the lock file use.
    static String normalize(String name) {
        StringBuilder sb = new StringBuilder();
        boolean sep = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '-' || c == '_' || c == '.') {
                sep = true;
            } else {
                if (sep && sb.length() > 0) {
                    sb.append('-');
                }
                sep = false;
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    /// Parses one requirement line, or answers null for one that is not a
    /// named requirement (an option, a URL, a blank line).
    static PythonRequirement parse(String line) {
        String s = line;
        int hash = s.indexOf(" #");
        if (hash >= 0) {
            s = s.substring(0, hash);
        }
        if (s.startsWith("#")) {
            return null;
        }
        s = s.trim();
        if (s.isEmpty() || s.startsWith("-") || s.contains("://")) {
            return null;
        }
        String marker = null;
        int semi = s.indexOf(';');
        if (semi >= 0) {
            marker = s.substring(semi + 1).trim();
            s = s.substring(0, semi).trim();
        }
        int end = 0;
        while (end < s.length()) {
            char c = s.charAt(end);
            if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.')) {
                break;
            }
            end++;
        }
        if (end == 0) {
            return null;
        }
        PythonRequirement r = new PythonRequirement(normalize(s.substring(0, end)), marker);
        String rest = s.substring(end).trim();
        if (rest.startsWith("[")) {
            int close = rest.indexOf(']');
            for (String e : rest.substring(1, close < 0 ? rest.length() : close).split(",")) {
                if (!e.trim().isEmpty()) {
                    r.extras.add(normalize(e.trim()));
                }
            }
            rest = close < 0 ? "" : rest.substring(close + 1).trim();
        }
        if (rest.startsWith("(") && rest.endsWith(")")) {
            rest = rest.substring(1, rest.length() - 1);
        }
        for (String spec : rest.split(",")) {
            String t = spec.trim();
            if (t.isEmpty()) {
                continue;
            }
            int i = 0;
            while (i < t.length() && "=!<>~".indexOf(t.charAt(i)) >= 0) {
                i++;
            }
            r.specifiers.add(new String[] {t.substring(0, i), t.substring(i).trim()});
        }
        return r;
    }

    /// Whether `version` satisfies every specifier. A pre-release is
    /// accepted only when a specifier names one, as pip decides by default.
    boolean accepts(String version) {
        boolean pre = isPreRelease(version);
        boolean prereleaseAsked = false;
        for (String[] s : specifiers) {
            if (isPreRelease(s[1].replace(".*", ""))) {
                prereleaseAsked = true;
            }
            if (!matches(s[0], s[1], version)) {
                return false;
            }
        }
        return !pre || prereleaseAsked;
    }

    /// Whether the marker holds for the build's environment, with `extras`
    /// the extras requested of the distribution that declared it.
    boolean applies(List<String> requestedExtras) {
        if (marker == null || marker.isEmpty()) {
            return true;
        }
        return new MarkerParser(marker, requestedExtras).parse();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        for (int i = 0; i < specifiers.size(); i++) {
            sb.append(i == 0 ? " " : ",").append(specifiers.get(i)[0]).append(specifiers.get(i)[1]);
        }
        return sb.toString();
    }

    static boolean isPreRelease(String version) {
        Object[] v = parseVersion(version);
        return v != null && (((int[]) v[1])[0] < 3 || ((int[]) v[3])[0] == 0);
    }

    static boolean matches(String op, String spec, String version) {
        if ("===".equals(op)) {
            return spec.equals(version);
        }
        if (("==".equals(op) || "!=".equals(op)) && spec.endsWith(".*")) {
            String prefix = spec.substring(0, spec.length() - 2);
            int[] want = release(prefix);
            int[] have = release(version);
            boolean same = want != null && have != null;
            for (int i = 0; same && i < want.length; i++) {
                same = i < have.length ? have[i] == want[i] : want[i] == 0;
            }
            return "==".equals(op) == same;
        }
        int c = compare(version, spec);
        if (c == Integer.MIN_VALUE) {
            return false;
        }
        switch (op) {
            case "==":
                return c == 0;
            case "!=":
                return c != 0;
            case "<":
                return c < 0;
            case "<=":
                return c <= 0;
            case ">":
                return c > 0;
            case ">=":
            case "":
                return c >= 0;
            case "~=": {
                int[] r = release(spec);
                if (r == null || r.length < 2 || c < 0) {
                    return false;
                }
                StringBuilder prefix = new StringBuilder();
                for (int i = 0; i < r.length - 1; i++) {
                    prefix.append(i == 0 ? "" : ".").append(r[i]);
                }
                return matches("==", prefix + ".*", version);
            }
            default:
                return false;
        }
    }

    /// Compares two versions; [Integer#MIN_VALUE] when either is not one.
    static int compare(String a, String b) {
        Object[] x = parseVersion(a);
        Object[] y = parseVersion(b);
        if (x == null || y == null) {
            return Integer.MIN_VALUE;
        }
        int ex = ((int[]) x[4])[0];
        int ey = ((int[]) y[4])[0];
        if (ex != ey) {
            return ex < ey ? -1 : 1;
        }
        int[] rx = (int[]) x[0];
        int[] ry = (int[]) y[0];
        for (int i = 0; i < Math.max(rx.length, ry.length); i++) {
            int p = i < rx.length ? rx[i] : 0;
            int q = i < ry.length ? ry[i] : 0;
            if (p != q) {
                return p < q ? -1 : 1;
            }
        }
        for (int k = 1; k < 4; k++) {
            int[] p = (int[]) x[k];
            int[] q = (int[]) y[k];
            for (int i = 0; i < 2; i++) {
                if (p[i] != q[i]) {
                    return p[i] < q[i] ? -1 : 1;
                }
            }
        }
        return 0;
    }

    private static int[] release(String v) {
        Object[] p = parseVersion(v);
        return p == null ? null : (int[]) p[0];
    }

    /// `{release, pre, post, dev, epoch}`, pre, post and dev each a pair whose
    /// order sorts as PEP 440 does: a dev-release before its pre-release,
    /// a pre-release before the final, the final before its post-release.
    private static Object[] parseVersion(String version) {
        String v = version.trim().toLowerCase(Locale.ROOT);
        if (v.startsWith("v")) {
            v = v.substring(1);
        }
        int bang = v.indexOf('!');
        int epoch = 0;
        if (bang >= 0) {
            try {
                epoch = Integer.parseInt(v.substring(0, bang));
            } catch (NumberFormatException e) {
                return null;
            }
            v = v.substring(bang + 1);
        }
        int plus = v.indexOf('+');
        if (plus >= 0) {
            v = v.substring(0, plus);
        }
        int i = 0;
        List<Integer> rel = new ArrayList<Integer>();
        while (i < v.length()) {
            int j = i;
            while (j < v.length() && Character.isDigit(v.charAt(j))) {
                j++;
            }
            if (j == i) {
                return null;
            }
            rel.add(Integer.valueOf(Integer.parseInt(v.substring(i, Math.min(j, i + 9)))));
            i = j;
            if (i < v.length() && v.charAt(i) == '.' && i + 1 < v.length() && Character.isDigit(v.charAt(i + 1))) {
                i++;
            } else {
                break;
            }
        }
        int[] release = new int[rel.size()];
        for (int k = 0; k < release.length; k++) {
            release[k] = rel.get(k).intValue();
        }
        int[] pre = {3, 0};
        int[] post = {0, 0};
        int[] dev = {1, 0};
        String rest = v.substring(i);
        while (!rest.isEmpty()) {
            rest = strip(rest);
            String[] tags = {"alpha", "beta", "preview", "pre", "rc", "a", "b", "c", "post", "rev", "r", "dev"};
            String tag = null;
            for (String t : tags) {
                if (rest.startsWith(t)) {
                    tag = t;
                    break;
                }
            }
            if (tag == null) {
                if (!rest.isEmpty() && Character.isDigit(rest.charAt(0)) && post[0] == 0) {
                    tag = "";
                } else {
                    return rest.isEmpty() ? new Object[] {release, pre, post, dev} : null;
                }
            }
            rest = strip(rest.substring(tag.length()));
            int j = 0;
            while (j < rest.length() && Character.isDigit(rest.charAt(j))) {
                j++;
            }
            int n = j == 0 ? 0 : Integer.parseInt(rest.substring(0, Math.min(j, 9)));
            rest = rest.substring(j);
            if ("dev".equals(tag)) {
                dev = new int[] {0, n};
            } else if ("post".equals(tag) || "rev".equals(tag) || "r".equals(tag) || tag.isEmpty()) {
                post = new int[] {1, n};
            } else if (tag.startsWith("a")) {
                pre = new int[] {0, n};
            } else if (tag.startsWith("b")) {
                pre = new int[] {1, n};
            } else {
                pre = new int[] {2, n};
            }
        }
        if (dev[0] == 0 && pre[0] == 3 && post[0] == 0) {
            // 1.0.dev1 sorts before 1.0a1
            pre = new int[] {-1, 0};
        }
        return new Object[] {release, pre, post, dev, new int[] {epoch}};
    }

    private static String strip(String s) {
        int i = 0;
        while (i < s.length() && (s.charAt(i) == '.' || s.charAt(i) == '-' || s.charAt(i) == '_')) {
            i++;
        }
        return s.substring(i);
    }

    /// PEP 508 markers: comparisons joined by `and`/`or`, in parentheses.
    private static final class MarkerParser {
        private final String s;
        private final List<String> extras;
        private int pos;

        MarkerParser(String s, List<String> extras) {
            this.s = s;
            this.extras = extras;
        }

        boolean parse() {
            boolean v = or();
            return v;
        }

        private boolean or() {
            boolean v = and();
            while (word("or")) {
                boolean w = and();
                v = v || w;
            }
            return v;
        }

        private boolean and() {
            boolean v = atom();
            while (word("and")) {
                boolean w = atom();
                v = v && w;
            }
            return v;
        }

        private boolean atom() {
            skip();
            if (pos < s.length() && s.charAt(pos) == '(') {
                pos++;
                boolean v = or();
                skip();
                if (pos < s.length() && s.charAt(pos) == ')') {
                    pos++;
                }
                return v;
            }
            String left = value();
            String op = operator();
            String right = value();
            return compareMarker(left, op, right);
        }

        private boolean compareMarker(String left, String op, String right) {
            boolean leftVar = left.startsWith("$");
            String var = leftVar ? left.substring(1) : right.startsWith("$") ? right.substring(1) : null;
            if ("extra".equals(var)) {
                String want = normalize(leftVar ? right : left);
                boolean has = extras.contains(want);
                return "==".equals(op) ? has : "!=".equals(op) ? !has : false;
            }
            String l = leftVar ? env(left.substring(1)) : left;
            String r = right.startsWith("$") ? env(right.substring(1)) : right;
            if ("in".equals(op)) {
                return r.contains(l);
            }
            if ("not in".equals(op)) {
                return !r.contains(l);
            }
            if (var != null && var.endsWith("version") && compare(l, r) != Integer.MIN_VALUE) {
                return matches(op, leftVar ? r : l, leftVar ? l : r);
            }
            switch (op) {
                case "==":
                    return l.equals(r);
                case "!=":
                    return !l.equals(r);
                default:
                    return false;
            }
        }

        private static String env(String name) {
            switch (name) {
                case "python_version":
                    return PYTHON_VERSION;
                case "python_full_version":
                    return PYTHON_VERSION + ".0";
                case "os_name":
                    return "posix";
                case "sys_platform":
                    return "linux";
                case "platform_system":
                    return "Linux";
                case "platform_machine":
                    return "aarch64";
                case "platform_python_implementation":
                    return "CPython";
                case "implementation_name":
                    return "cpython";
                case "implementation_version":
                    return PYTHON_VERSION + ".0";
                default:
                    return "";
            }
        }

        private String value() {
            skip();
            if (pos >= s.length()) {
                return "";
            }
            char c = s.charAt(pos);
            if (c == '"' || c == '\'') {
                int end = s.indexOf(c, pos + 1);
                if (end < 0) {
                    end = s.length();
                }
                String v = s.substring(pos + 1, end);
                pos = Math.min(s.length(), end + 1);
                return v;
            }
            int start = pos;
            while (pos < s.length() && (Character.isLetterOrDigit(s.charAt(pos)) || s.charAt(pos) == '_'
                    || s.charAt(pos) == '.')) {
                pos++;
            }
            return "$" + s.substring(start, pos);
        }

        private String operator() {
            skip();
            if (word("not")) {
                word("in");
                return "not in";
            }
            if (word("in")) {
                return "in";
            }
            int start = pos;
            while (pos < s.length() && "=!<>~".indexOf(s.charAt(pos)) >= 0) {
                pos++;
            }
            return s.substring(start, pos);
        }

        private boolean word(String w) {
            skip();
            if (s.startsWith(w, pos) && (pos + w.length() == s.length()
                    || !Character.isLetterOrDigit(s.charAt(pos + w.length())))) {
                pos += w.length();
                return true;
            }
            return false;
        }

        private void skip() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }
    }
}
