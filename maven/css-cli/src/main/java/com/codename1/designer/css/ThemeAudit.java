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
package com.codename1.designer.css;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/// Reads a theme's stylesheets and reports where the design system has
/// drifted: colours written as literals that a variable already names,
/// variables nothing reads or nothing defines, a literal repeated often enough
/// to deserve a name, text that cannot be read against its background, and
/// UIIDs no code ever asks for.
///
/// It reads the CSS as text and compiles nothing, so it answers in
/// milliseconds and needs no resource file. That is also its limit: it sees
/// the stylesheet as written, not the theme the compiler builds from it, and a
/// UIID an application composes at run time (`"Tab" + state`) is invisible to
/// the UIID check. Every finding is therefore advice for a person or an agent
/// to act on, and only `-strict` turns findings into a failing exit status.
public final class ThemeAudit {

    /// How much a finding matters. `ERROR` is a theme that does not mean what
    /// it says (a variable read and never defined); `WARNING` is drift;
    /// `INFO` is a suggestion.
    public enum Severity { ERROR, WARNING, INFO }

    /// One thing the audit found, at a place in a stylesheet.
    public static final class Finding {
        public final Severity severity;
        public final String check;
        public final File file;
        public final int line;
        public final String message;

        Finding(Severity severity, String check, File file, int line, String message) {
            this.severity = severity;
            this.check = check;
            this.file = file;
            this.line = line;
            this.message = message;
        }

        @Override
        public String toString() {
            return file.getPath() + ":" + line + ": " + severity.name().toLowerCase(java.util.Locale.ROOT)
                    + " [" + check + "] " + message;
        }
    }

    /// WCAG 2 AA for body text.
    static final double MIN_CONTRAST = 4.5;

    /// How many rules must repeat a colour literal before it is suggested as a
    /// variable.
    static final int REPEAT_THRESHOLD = 3;

    /// Every `var(` reference, nested fallbacks included; group 2 is set
    /// when a fallback follows.
    private static final Pattern VAR_REF = Pattern.compile("var\\(\\s*(--[A-Za-z0-9_-]+)\\s*(,)?");
    private static final Pattern COLOR_LITERAL = Pattern.compile(
            "#[0-9A-Fa-f]{8}\\b|#[0-9A-Fa-f]{6}\\b|#[0-9A-Fa-f]{3}\\b|rgba?\\([^)]*\\)");
    private static final Pattern IDENT = Pattern.compile("^[A-Za-z_][A-Za-z0-9_-]*");

    private static final class Declaration {
        final String property;
        final String value;
        final int line;

        Declaration(String property, String value, int line) {
            this.property = property;
            this.value = value;
            this.line = line;
        }
    }

    private static final class Rule {
        final File file;
        final String selector;
        final int line;
        final List<Declaration> declarations = new ArrayList<Declaration>();
        /// Variables defined inside the same `@media` block, which override
        /// the top-level ones for this rule; null at top level.
        final Map<String, Variable> scope;
        /// From a library stylesheet: read for what it defines and uses,
        /// never reported on.
        final boolean library;

        Rule(File file, String selector, int line, Map<String, Variable> scope, boolean library) {
            this.file = file;
            this.selector = selector;
            this.line = line;
            this.scope = scope;
            this.library = library;
        }
    }

    private static final class Variable {
        final String value;
        final File file;
        final int line;
        final boolean library;

        Variable(String value, File file, int line, boolean library) {
            this.value = value;
            this.file = file;
            this.line = line;
            this.library = library;
        }
    }

    private final List<Rule> rules = new ArrayList<Rule>();
    /// Top-level definitions: the values the theme has outside any media
    /// query.
    private final Map<String, Variable> variables = new LinkedHashMap<String, Variable>();
    /// Every variable defined anywhere, media blocks included, by name.
    private final Map<String, Variable> definedAnywhere = new LinkedHashMap<String, Variable>();
    /// Variables a media block gives another value; a literal equal to
    /// the top-level value is not the same colour in every mode.
    private final Set<String> redefinedInMedia = new HashSet<String>();
    private boolean parsingLibrary;
    private final List<Finding> findings = new ArrayList<Finding>();

    private ThemeAudit() {
    }

    /// Audits `stylesheets`. With `sources` it also reports UIIDs that neither
    /// the framework nor anything under those directories names; without
    /// them that check is skipped, because a theme alone cannot say what an
    /// application uses.
    public static List<Finding> audit(List<File> stylesheets, List<File> sources) throws IOException {
        return audit(new ArrayList<File>(), stylesheets, sources);
    }

    /// As above, with `libraries` read first for the variables and UIIDs
    /// they define and use -- the stylesheets of installed cn1libs, which the
    /// build merges ahead of the application's -- but never reported on.
    public static List<Finding> audit(List<File> libraries, List<File> stylesheets, List<File> sources)
            throws IOException {
        ThemeAudit a = new ThemeAudit();
        a.parsingLibrary = true;
        for (File f : libraries) {
            a.parse(f, new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        }
        a.parsingLibrary = false;
        for (File f : stylesheets) {
            a.parse(f, new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        }
        a.checkVariables();
        a.checkLiterals();
        a.checkContrast();
        if (sources != null && !sources.isEmpty()) {
            Set<String> names = new HashSet<String>();
            collectFrameworkStrings(names);
            for (File dir : sources) {
                collectStrings(dir, names);
            }
            a.checkUiids(names);
        }
        return a.sorted();
    }

    /// Audits CSS text directly; for tests and for callers holding an
    /// unsaved buffer.
    static List<Finding> auditText(File file, String css) {
        ThemeAudit a = new ThemeAudit();
        a.parse(file, css);
        a.checkVariables();
        a.checkLiterals();
        a.checkContrast();
        return a.sorted();
    }

    static List<Finding> auditText(File file, String css, Set<String> referencedNames) {
        ThemeAudit a = new ThemeAudit();
        a.parse(file, css);
        a.checkUiids(referencedNames);
        return a.sorted();
    }

    /// By file, then line, so output reads top to bottom.
    private List<Finding> sorted() {
        Collections.sort(findings, new Comparator<Finding>() {
            public int compare(Finding x, Finding y) {
                int c = x.file.getPath().compareTo(y.file.getPath());
                return c != 0 ? c : Integer.compare(x.line, y.line);
            }
        });
        return findings;
    }

    // ---------------------------------------------------------------- parsing

    /// Comments become spaces of the same length, newlines kept, so every
    /// offset still maps to its line in the original file.
    static String blankComments(String css) {
        StringBuilder sb = new StringBuilder(css);
        int i = 0;
        while ((i = sb.indexOf("/*", i)) >= 0) {
            int end = sb.indexOf("*/", i + 2);
            end = end < 0 ? sb.length() : end + 2;
            for (int j = i; j < end; j++) {
                if (sb.charAt(j) != '\n') {
                    sb.setCharAt(j, ' ');
                }
            }
            i = end;
        }
        return sb.toString();
    }

    private void parse(File file, String css) {
        String text = blankComments(css);
        int[] lineStarts = lineStarts(text);
        parseBlock(file, text, 0, text.length(), lineStarts, null);
    }

    private void parseBlock(File file, String text, int from, int to, int[] lineStarts,
            Map<String, Variable> scope) {
        int i = from;
        while (i < to) {
            int open = indexOf(text, '{', i, to);
            int semi = indexOf(text, ';', i, to);
            if (semi >= 0 && (open < 0 || semi < open)) {
                // @import, @charset: a statement with no block.
                i = semi + 1;
                continue;
            }
            if (open < 0) {
                return;
            }
            int close = matchingBrace(text, open, to);
            String prelude = text.substring(i, open).trim();
            int preludeAt = i + leadingSpace(text, i, open);
            if (prelude.startsWith("@media") || prelude.startsWith("@supports")) {
                parseBlock(file, text, open + 1, close, lineStarts, new LinkedHashMap<String, Variable>());
            } else if (!prelude.startsWith("@") && prelude.length() > 0) {
                Rule r = new Rule(file, prelude, lineOf(lineStarts, preludeAt), scope, parsingLibrary);
                parseDeclarations(r, text, open + 1, close, lineStarts);
                rules.add(r);
            }
            i = close + 1;
        }
    }

    private void parseDeclarations(Rule r, String text, int from, int to, int[] lineStarts) {
        int start = from;
        int depth = 0;
        boolean quoted = false;
        char quote = 0;
        for (int i = from; i <= to; i++) {
            char c = i < to ? text.charAt(i) : ';';
            if (quoted) {
                if (c == quote) {
                    quoted = false;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                quoted = true;
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ';' && depth <= 0) {
                addDeclaration(r, text, start, i, lineStarts);
                start = i + 1;
            }
        }
    }

    private void addDeclaration(Rule r, String text, int from, int to, int[] lineStarts) {
        String decl = text.substring(from, to);
        int colon = decl.indexOf(':');
        if (colon <= 0) {
            return;
        }
        String property = decl.substring(0, colon).trim();
        String value = decl.substring(colon + 1).trim();
        if (value.endsWith("!important")) {
            value = value.substring(0, value.length() - "!important".length()).trim();
        }
        int line = lineOf(lineStarts, from + leadingSpace(text, from, to));
        r.declarations.add(new Declaration(property, value, line));
        if (property.startsWith("--")) {
            Variable v = new Variable(value, r.file, line, r.library);
            if (!definedAnywhere.containsKey(property)) {
                definedAnywhere.put(property, v);
            }
            if (r.scope == null) {
                variables.put(property, v);
            } else {
                r.scope.put(property, v);
                redefinedInMedia.add(property);
            }
        }
    }

    private static int indexOf(String s, char c, int from, int to) {
        int i = s.indexOf(c, from);
        return i >= 0 && i < to ? i : -1;
    }

    private static int matchingBrace(String s, int open, int to) {
        int depth = 0;
        char quote = 0;
        for (int i = open; i < to; i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return to;
    }

    private static int leadingSpace(String s, int from, int to) {
        int n = 0;
        while (from + n < to && Character.isWhitespace(s.charAt(from + n))) {
            n++;
        }
        return n;
    }

    private static int[] lineStarts(String s) {
        List<Integer> starts = new ArrayList<Integer>();
        starts.add(Integer.valueOf(0));
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                starts.add(Integer.valueOf(i + 1));
            }
        }
        int[] out = new int[starts.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = starts.get(i).intValue();
        }
        return out;
    }

    private static int lineOf(int[] starts, int offset) {
        int lo = 0;
        int hi = starts.length - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (starts[mid] <= offset) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo + 1;
    }

    // ----------------------------------------------------------------- checks

    private void add(Severity s, String check, File file, int line, String message) {
        findings.add(new Finding(s, check, file, line, message));
    }

    private void checkVariables() {
        Set<String> read = new HashSet<String>();
        for (Rule r : rules) {
            for (Declaration d : r.declarations) {
                Matcher m = VAR_REF.matcher(d.value);
                while (m.find()) {
                    String name = m.group(1);
                    read.add(name);
                    if (!r.library && !definedAnywhere.containsKey(name) && m.group(2) == null) {
                        add(Severity.ERROR, "undefined-variable", r.file, d.line,
                                d.property + " reads " + name + ", which no stylesheet defines and which has no fallback");
                    }
                }
            }
        }
        for (Map.Entry<String, Variable> e : definedAnywhere.entrySet()) {
            Variable v = e.getValue();
            if (!read.contains(e.getKey()) && !v.library) {
                add(Severity.WARNING, "unused-variable", v.file, v.line, e.getKey() + " is defined and never read");
            }
        }
    }

    private void checkLiterals() {
        // A colour value a variable names, by normalized value; the first
        // variable wins so the suggestion is stable.
        Map<String, String> named = new LinkedHashMap<String, String>();
        for (Map.Entry<String, Variable> e : variables.entrySet()) {
            String c = normalizeColor(e.getValue().value);
            if (c != null && !named.containsKey(c) && !redefinedInMedia.contains(e.getKey())) {
                named.put(c, e.getKey());
            }
        }
        Map<String, List<Declaration>> unnamed = new LinkedHashMap<String, List<Declaration>>();
        Map<Declaration, Rule> owner = new LinkedHashMap<Declaration, Rule>();
        for (Rule r : rules) {
            if (r.library) {
                continue;
            }
            for (Declaration d : r.declarations) {
                if (d.property.startsWith("--")) {
                    continue;
                }
                Matcher m = COLOR_LITERAL.matcher(d.value);
                while (m.find()) {
                    String c = normalizeColor(m.group());
                    if (c == null) {
                        continue;
                    }
                    String var = named.get(c);
                    if (var != null) {
                        add(Severity.WARNING, "literal-matches-variable", r.file, d.line,
                                r.selector + " { " + d.property + " } writes " + m.group()
                                        + ", the value of " + var + "; use var(" + var + ")");
                    } else {
                        List<Declaration> uses = unnamed.get(c);
                        if (uses == null) {
                            uses = new ArrayList<Declaration>();
                            unnamed.put(c, uses);
                        }
                        uses.add(d);
                        owner.put(d, r);
                    }
                }
            }
        }
        for (Map.Entry<String, List<Declaration>> e : unnamed.entrySet()) {
            Set<Rule> inRules = new HashSet<Rule>();
            for (Declaration d : e.getValue()) {
                inRules.add(owner.get(d));
            }
            if (inRules.size() >= REPEAT_THRESHOLD) {
                Declaration first = e.getValue().get(0);
                Rule r = owner.get(first);
                add(Severity.INFO, "repeated-literal", r.file, first.line,
                        e.getKey() + " is written in " + inRules.size()
                                + " rules and named by no variable; consider a --variable for it");
            }
        }
    }

    private void checkContrast() {
        for (Rule r : rules) {
            if (r.library) {
                continue;
            }
            Declaration fg = null;
            Declaration bg = null;
            for (Declaration d : r.declarations) {
                if ("color".equals(d.property)) {
                    fg = d;
                } else if ("background-color".equals(d.property) || "background".equals(d.property)) {
                    bg = d;
                }
            }
            if (fg == null || bg == null) {
                continue;
            }
            int[] f = toRgb(resolve(fg.value, r.scope));
            int[] b = toRgb(resolve(bg.value, r.scope));
            if (f == null || b == null || f[3] < 255 || b[3] < 255) {
                // Translucent colours depend on what is under them.
                continue;
            }
            double ratio = contrast(f, b);
            if (ratio < MIN_CONTRAST) {
                add(Severity.WARNING, "low-contrast", r.file, fg.line, r.selector + " has text "
                        + fg.value + " on " + bg.value + ": contrast "
                        + String.format(java.util.Locale.ROOT, "%.2f", ratio) + ":1, below the "
                        + MIN_CONTRAST + ":1 WCAG AA asks for body text");
            }
        }
    }

    private void checkUiids(Set<String> referencedNames) {
        // A UIID another rule derives from is used by that rule.
        Set<String> referenced = new HashSet<String>(referencedNames);
        for (Rule r : rules) {
            for (Declaration d : r.declarations) {
                if ("cn1-derive".equals(d.property)) {
                    referenced.add(d.value.trim());
                }
            }
        }
        Set<String> reported = new HashSet<String>();
        for (Rule r : rules) {
            if (r.library) {
                continue;
            }
            for (String part : r.selector.split(",")) {
                String uiid = uiidOf(part.trim());
                if (uiid == null || referenced.contains(uiid) || builtOnPrefix(uiid, referenced)
                        || !reported.add(uiid)) {
                    continue;
                }
                add(Severity.WARNING, "unused-uiid", r.file, r.line, uiid
                        + " is styled but neither the framework nor the scanned sources name it"
                        + " (a UIID built at run time is not seen)");
            }
        }
    }

    private static boolean builtOnPrefix(String uiid, Set<String> referenced) {
        for (int i = uiid.length() - 1; i > 0; i--) {
            if (referenced.contains(uiid.substring(0, i) + PREFIX_MARK)) {
                return true;
            }
        }
        return false;
    }

    /// The UIID a selector styles, or null for one that styles none: `:root`,
    /// `#Constants`, `#Device`, `*`.
    static String uiidOf(String selector) {
        Matcher m = IDENT.matcher(selector);
        return m.find() ? m.group() : null;
    }

    private String resolve(String value, Map<String, Variable> scope) {
        String v = value.trim();
        for (int guard = 0; guard < 16; guard++) {
            String[] ref = wholeVar(v);
            if (ref == null) {
                return v;
            }
            Variable var = scope != null && scope.containsKey(ref[0]) ? scope.get(ref[0]) : variables.get(ref[0]);
            if (var != null) {
                v = var.value.trim();
            } else if (ref[1] != null) {
                v = ref[1];
            } else {
                return v;
            }
        }
        return v;
    }

    /// The name and fallback (null when absent) of a value that is one
    /// `var()` as a whole, or null: `var(--a, red) var(--b)` is two values.
    static String[] wholeVar(String v) {
        if (!v.startsWith("var(") || !v.endsWith(")")) {
            return null;
        }
        int depth = 0;
        for (int i = 3; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0 && i != v.length() - 1) {
                return null;
            }
        }
        String inner = v.substring(4, v.length() - 1);
        int comma = inner.indexOf(',');
        String name = (comma < 0 ? inner : inner.substring(0, comma)).trim();
        if (!name.startsWith("--")) {
            return null;
        }
        return new String[] {name, comma < 0 ? null : inner.substring(comma + 1).trim()};
    }

    // ----------------------------------------------------------------- colour

    /// A colour as `#rrggbb` (or `#rrggbbaa` when not opaque), or null when
    /// `value` is not exactly one colour.
    static String normalizeColor(String value) {
        int[] c = toRgb(value);
        if (c == null) {
            return null;
        }
        String hex = String.format(java.util.Locale.ROOT, "#%02x%02x%02x", c[0], c[1], c[2]);
        return c[3] == 255 ? hex : hex + String.format(java.util.Locale.ROOT, "%02x", c[3]);
    }

    static int[] toRgb(String value) {
        String v = value.trim();
        try {
            if (v.startsWith("#")) {
                String h = v.substring(1);
                if (h.length() == 3) {
                    return new int[] {hex1(h.charAt(0)), hex1(h.charAt(1)), hex1(h.charAt(2)), 255};
                }
                if (h.length() == 6 || h.length() == 8) {
                    int a = h.length() == 8 ? Integer.parseInt(h.substring(6, 8), 16) : 255;
                    return new int[] {Integer.parseInt(h.substring(0, 2), 16),
                        Integer.parseInt(h.substring(2, 4), 16), Integer.parseInt(h.substring(4, 6), 16), a};
                }
                return null;
            }
            if ((v.startsWith("rgb(") || v.startsWith("rgba(")) && v.endsWith(")")) {
                String[] p = v.substring(v.indexOf('(') + 1, v.length() - 1).split("[,\\s/]+");
                if (p.length < 3 || p.length > 4) {
                    return null;
                }
                int a = 255;
                if (p.length == 4) {
                    double alpha = p[3].endsWith("%") ? Double.parseDouble(p[3].substring(0, p[3].length() - 1)) / 100
                            : Double.parseDouble(p[3]);
                    a = (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255);
                }
                return new int[] {channel(p[0]), channel(p[1]), channel(p[2]), a};
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        return null;
    }

    private static int hex1(char c) {
        int d = Character.digit(c, 16);
        if (d < 0) {
            throw new NumberFormatException(String.valueOf(c));
        }
        return d * 17;
    }

    private static int channel(String s) {
        double d = s.endsWith("%") ? Double.parseDouble(s.substring(0, s.length() - 1)) * 2.55 : Double.parseDouble(s);
        return (int) Math.round(Math.max(0, Math.min(255, d)));
    }

    static double contrast(int[] a, int[] b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(int[] c) {
        return 0.2126 * linear(c[0]) + 0.7152 * linear(c[1]) + 0.0722 * linear(c[2]);
    }

    private static double linear(int channel) {
        double s = channel / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    // ------------------------------------------------------ referenced names

    private static final Pattern STRING_LITERAL = Pattern.compile("\"((?:[^\"\\\\\\n]|\\\\.)*)\"(\\s*\\+)?");

    /// Marks an entry of the referenced set as a prefix a name is built on.
    static final char PREFIX_MARK = '*';

    /// The string constants of the framework classes this tool runs with,
    /// which is where every built-in UIID is named.
    private static void collectFrameworkStrings(Set<String> out) throws IOException {
        java.security.CodeSource source = com.codename1.ui.Component.class.getProtectionDomain().getCodeSource();
        if (source == null || source.getLocation() == null) {
            return;
        }
        URL where = source.getLocation();
        File f;
        try {
            f = new File(where.toURI());
        } catch (java.net.URISyntaxException ex) {
            throw new IOException(ex);
        }
        if (f.isDirectory()) {
            collectStrings(f, out);
            return;
        }
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(f))) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                String n = e.getName();
                if (n.startsWith("com/codename1/") && !n.startsWith("com/codename1/designer/") && n.endsWith(".class")) {
                    classStringsLenient(readAll(zip), out);
                }
            }
        }
    }

    /// The string literals of every source file and the string constants of
    /// every class under `dir`. Text formats that name UIIDs (GUI builder
    /// `.gui` and `.xml` files, JSON) are read for quoted strings too.
    static void collectStrings(File dir, Set<String> out) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File c : children) {
            String n = c.getName();
            if (c.isDirectory()) {
                if (!n.startsWith(".")) {
                    collectStrings(c, out);
                }
            } else if (n.endsWith(".class")) {
                classStringsLenient(Files.readAllBytes(c.toPath()), out);
            } else if (n.endsWith(".java") || n.endsWith(".kt") || n.endsWith(".gui") || n.endsWith(".xml")
                    || n.endsWith(".json") || n.endsWith(".py")) {
                Matcher m = STRING_LITERAL.matcher(new String(Files.readAllBytes(c.toPath()), StandardCharsets.UTF_8));
                boolean kotlin = n.endsWith(".kt");
                while (m.find()) {
                    out.add(m.group(1));
                    int template = kotlin ? m.group(1).indexOf('$') : -1;
                    if (template > 0) {
                        // "Tab$state": a Kotlin template builds the name.
                        out.add(m.group(1).substring(0, template) + PREFIX_MARK);
                    }
                    // "SvDesk" + name: every UIID starting with the
                    // literal may be meant, recorded with a trailing '*',
                    // which no UIID can contain.
                    if (m.group(2) != null && m.group(1).length() > 0) {
                        out.add(m.group(1) + PREFIX_MARK);
                    }
                }
            }
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    /// As [#classStrings], but a class that cannot be read (truncated by a
    /// compile still writing it) contributes nothing instead of ending the
    /// audit.
    static void classStringsLenient(byte[] classFile, Set<String> out) {
        try {
            classStrings(classFile, out);
        } catch (IOException ex) {
            // Unreadable: skipped, see above.
        } catch (RuntimeException ex) {
            // A constant pool index out of range: skipped the same way.
        }
    }

    /// The `CONSTANT_String` values of a class file: what the code passes as
    /// string literals, not the names of its members.
    static void classStrings(byte[] classFile, Set<String> out) throws IOException {
        DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(classFile));
        if (in.readInt() != 0xCAFEBABE) {
            return;
        }
        in.readUnsignedShort();
        in.readUnsignedShort();
        int count = in.readUnsignedShort();
        String[] utf8 = new String[count];
        List<Integer> stringRefs = new ArrayList<Integer>();
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1:
                    utf8[i] = in.readUTF();
                    break;
                case 8:
                    stringRefs.add(Integer.valueOf(in.readUnsignedShort()));
                    break;
                case 7: case 16: case 19: case 20:
                    in.readUnsignedShort();
                    break;
                case 15:
                    in.readUnsignedByte();
                    in.readUnsignedShort();
                    break;
                case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18:
                    in.readInt();
                    break;
                case 5: case 6:
                    in.readLong();
                    i++;
                    break;
                default:
                    // An unknown constant: nothing after it can be read.
                    return;
            }
        }
        for (Integer ref : stringRefs) {
            int idx = ref.intValue();
            if (idx > 0 && idx < count && utf8[idx] != null) {
                out.add(utf8[idx]);
            }
        }
    }

    // ----------------------------------------------------------------- output

    /// The findings as a JSON object: `{"findings":[{...}],"errors":n,...}`.
    public static String toJson(List<Finding> findings) {
        StringBuilder sb = new StringBuilder("{\"findings\":[");
        int[] counts = new int[3];
        for (int i = 0; i < findings.size(); i++) {
            Finding f = findings.get(i);
            counts[f.severity.ordinal()]++;
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"severity\":").append(quote(f.severity.name().toLowerCase(java.util.Locale.ROOT)))
                    .append(",\"check\":").append(quote(f.check))
                    .append(",\"file\":").append(quote(f.file.getPath()))
                    .append(",\"line\":").append(f.line)
                    .append(",\"message\":").append(quote(f.message)).append('}');
        }
        sb.append("],\"errors\":").append(counts[0]).append(",\"warnings\":").append(counts[1])
                .append(",\"info\":").append(counts[2]).append('}');
        return sb.toString();
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c < 0x20 || c > 0x7e) {
                sb.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
