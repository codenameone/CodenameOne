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
package com.codename1.impl.javase;

import com.codename1.ai.Tool;
import com.codename1.ai.ToolHandler;
import com.codename1.io.JSONParser;
import com.codename1.mcp.MCPServer;
import com.codename1.project.ProjectLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// MCP tools that let an agent restyle the running application through its
/// `theme.css`, with every change staged and reversible.
///
/// An edit is written straight into the stylesheet, so the simulator's live
/// CSS reload shows it within a second, but the text the file had before the
/// first edit is kept: `sim_style_diff` shows everything staged since,
/// `sim_style_reject` puts that text back (and the theme reloads again), and
/// `sim_style_approve` keeps the changes and forgets the snapshot. Nothing is
/// written anywhere but the stylesheet the simulator already watches.
///
/// The edits work on the source text, never on a compiled theme, so what an
/// agent changes is exactly what a person would review: a rule's declaration
/// replaced or added, a missing rule appended. If the file changes under the
/// staged edits (a person saved it in an editor), the tools refuse to write
/// until the staged set is approved or the agent starts over, rather than
/// overwrite that work with a stale snapshot.
final class SimulatorStyleTools {

    /// Largest diff, in lines times lines, computed exactly; beyond it the
    /// diff reports the file as replaced.
    private static final long MAX_DIFF_CELLS = 4000000L;

    /// The file's text before the first staged edit, or null when nothing is
    /// staged.
    private String original;

    /// The text the last staged edit wrote, to notice edits made elsewhere.
    private String written;

    private SimulatorStyleTools() {
    }

    static void register(MCPServer server) {
        new SimulatorStyleTools().addAll(server);
    }

    private void addAll(MCPServer server) {
        server.addTool(new Tool("sim_style_get",
                "Reads one rule of the application's theme.css as written: give the selector exactly "
                        + "as the stylesheet spells it (\"Button\", \"Button.pressed\"). Answers the "
                        + "declarations and the line, or found=false.",
                "{\"type\":\"object\",\"properties\":{\"selector\":{\"type\":\"string\"}},"
                        + "\"required\":[\"selector\"]}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws Exception {
                        String selector = required(parse(argumentsJson), "selector");
                        String text = read(themeCss());
                        Map<String, Object> out = new LinkedHashMap<String, Object>();
                        out.put("file", themeCss().getPath());
                        CssRule rule = CssRule.find(text, selector);
                        out.put("found", Boolean.valueOf(rule != null));
                        if (rule != null) {
                            out.put("line", Integer.valueOf(rule.line(text)));
                            out.put("declarations", rule.declarations(text));
                        }
                        out.put("staged", Boolean.valueOf(original != null));
                        return JSONParser.toJson(out);
                    }
                }));

        server.addTool(new Tool("sim_style_set",
                "Stages one change to theme.css and lets live CSS reload show it: sets property to "
                        + "value in the rule for selector, adding the declaration, or the rule, when "
                        + "missing. An empty value removes the declaration. Prefer var(--name) over a "
                        + "literal when the theme already names the colour (see sim_style_audit). "
                        + "Answers the staged diff. Review with sim_style_diff; keep with "
                        + "sim_style_approve; undo everything with sim_style_reject.",
                "{\"type\":\"object\",\"properties\":{\"selector\":{\"type\":\"string\"},"
                        + "\"property\":{\"type\":\"string\"},\"value\":{\"type\":\"string\"}},"
                        + "\"required\":[\"selector\",\"property\",\"value\"]}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> a = parse(argumentsJson);
                        String selector = required(a, "selector");
                        String property = required(a, "property");
                        Object v = a.get("value");
                        String value = v == null ? "" : v.toString().trim();
                        if (!isPlainToken(selector, "{};") || !isPlainToken(property, "{};: ")
                                || !isPlainToken(value, "{};")) {
                            throw new IllegalArgumentException("selector, property and value may not contain "
                                    + "braces, semicolons or line breaks");
                        }
                        File css = themeCss();
                        String current = read(css);
                        checkUnchanged(current);
                        String next = CssRule.set(current, selector, property, value);
                        if (original == null) {
                            original = current;
                        }
                        write(css, next);
                        return diffResult(css);
                    }
                }));

        server.addTool(new Tool("sim_style_diff",
                "Shows every theme.css change staged since the last approve or reject, as a unified diff.",
                "{\"type\":\"object\",\"properties\":{}}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws Exception {
                        return diffResult(themeCss());
                    }
                }));

        server.addTool(new Tool("sim_style_approve",
                "Keeps the staged theme.css changes and forgets the snapshot; they are now ordinary edits "
                        + "of the file. Answers the diff that was approved.",
                "{\"type\":\"object\",\"properties\":{}}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws Exception {
                        String result = diffResult(themeCss());
                        original = null;
                        written = null;
                        return result;
                    }
                }));

        server.addTool(new Tool("sim_style_reject",
                "Undoes every staged theme.css change: the file gets back the text it had before the "
                        + "first one, and live CSS reload shows the old theme. Refuses if the file was "
                        + "edited elsewhere since, unless force is true.",
                "{\"type\":\"object\",\"properties\":{\"force\":{\"type\":\"boolean\"}}}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws Exception {
                        Map<String, Object> a = parse(argumentsJson);
                        File css = themeCss();
                        Map<String, Object> out = new LinkedHashMap<String, Object>();
                        if (original == null) {
                            out.put("ok", Boolean.TRUE);
                            out.put("restored", Boolean.FALSE);
                            out.put("message", "Nothing is staged.");
                            return JSONParser.toJson(out);
                        }
                        if (!Boolean.TRUE.equals(a.get("force"))) {
                            checkUnchanged(read(css));
                        }
                        write(css, original);
                        original = null;
                        written = null;
                        out.put("ok", Boolean.TRUE);
                        out.put("restored", Boolean.TRUE);
                        return JSONParser.toJson(out);
                    }
                }));

        server.addTool(new Tool("sim_style_audit",
                "Audits theme.css for design-system drift without compiling it: colour literals a "
                        + "--variable already names, unused or undefined variables, literals repeated "
                        + "enough to deserve a variable, text below WCAG AA contrast, and UIIDs neither "
                        + "the framework nor the project's sources name. Answers JSON findings with "
                        + "file and line.",
                "{\"type\":\"object\",\"properties\":{}}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) throws Exception {
                        return audit();
                    }
                }));
    }

    // ------------------------------------------------------------- the file

    private static File themeCss() {
        String input = System.getProperty(CSSWatcher.CSS_INPUT_PROPERTY);
        if (input != null && !input.contains("${") && input.trim().length() > 0) {
            // Library stylesheets come first; the application's own is last.
            String[] parts = input.split(",");
            return new File(parts[parts.length - 1].trim());
        }
        ProjectLayout layout = SimulatorProject.current();
        if (layout == null || !layout.themeCss().isFile()) {
            throw new IllegalStateException("No theme.css: this simulator was not started from a "
                    + "Codename One project with a stylesheet");
        }
        return layout.themeCss();
    }

    private void checkUnchanged(String current) {
        if (written != null && !written.equals(current)) {
            throw new IllegalStateException("theme.css was edited outside the staged changes. "
                    + "sim_style_approve keeps both and starts a new staged set; "
                    + "sim_style_reject with force=true discards both.");
        }
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private void write(File f, String text) throws IOException {
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
        written = text;
    }

    private String diffResult(File css) throws IOException {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("file", css.getPath());
        out.put("staged", Boolean.valueOf(original != null));
        out.put("diff", original == null ? "" : unifiedDiff(original, read(css), css.getName()));
        return JSONParser.toJson(out);
    }

    // ---------------------------------------------------------------- audit

    private static String audit() throws IOException, InterruptedException {
        String cp = System.getProperty(CSSWatcher.CSS_CLI_CLASSPATH_PROPERTY);
        if (cp == null || cp.contains("${") || cp.trim().length() == 0) {
            throw new IllegalStateException("The CSS compiler's classpath (" + CSSWatcher.CSS_CLI_CLASSPATH_PROPERTY
                    + ") was not passed to this simulator, so the audit cannot run");
        }
        List<String> cmd = new ArrayList<String>();
        cmd.add(new File(System.getProperty("java.home"), "bin/java").getAbsolutePath());
        cmd.add("-Djava.awt.headless=true");
        cmd.add("-cp");
        cmd.add(cp);
        cmd.add("com.codename1.designer.css.CN1CSSCLI");
        cmd.add("-audit");
        cmd.add("-format");
        cmd.add("json");
        cmd.add("-input");
        cmd.add(themeCss().getAbsolutePath());
        ProjectLayout layout = SimulatorProject.current();
        if (layout != null) {
            StringBuilder sources = new StringBuilder();
            for (File d : new File[] {layout.javaSourceDir(), layout.guiBuilderDir(), layout.classesDir()}) {
                if (d != null && d.isDirectory() && d.getPath().indexOf(',') < 0) {
                    sources.append(sources.length() == 0 ? "" : ",").append(d.getAbsolutePath());
                }
            }
            if (sources.length() > 0) {
                cmd.add("-sources");
                cmd.add(sources.toString());
            }
        }
        // One stream, so a chatty stderr cannot fill its pipe while stdout is
        // being read. The JSON is the last line that is an object.
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        p.getOutputStream().close();
        String output = new String(readAll(p.getInputStream()), StandardCharsets.UTF_8);
        p.waitFor();
        String json = null;
        for (String line : output.split("\n")) {
            if (line.trim().startsWith("{\"findings\"")) {
                json = line.trim();
            }
        }
        if (json == null) {
            throw new IOException("The audit failed: " + output.trim());
        }
        return json;
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

    // ----------------------------------------------------------- arguments

    private static Map<String, Object> parse(String json) throws IOException {
        if (json == null || json.trim().length() == 0) {
            return new LinkedHashMap<String, Object>();
        }
        Map<String, Object> m = JSONParser.parseJSON(json);
        return m == null ? new LinkedHashMap<String, Object>() : m;
    }

    private static String required(Map<String, Object> a, String key) {
        Object v = a.get(key);
        if (v == null || v.toString().trim().length() == 0) {
            throw new IllegalArgumentException("Missing: " + key);
        }
        return v.toString().trim();
    }

    /// A value that cannot break out of the declaration it is written into.
    static boolean isPlainToken(String s, String forbidden) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r' || forbidden.indexOf(c) >= 0 || (c == '/' && i + 1 < s.length()
                    && s.charAt(i + 1) == '*')) {
                return false;
            }
        }
        return true;
    }

    // ----------------------------------------------------------------- diff

    /// A unified diff of two texts with three lines of context.
    static String unifiedDiff(String before, String after, String name) {
        if (before.equals(after)) {
            return "";
        }
        String[] a = before.split("\n", -1);
        String[] b = after.split("\n", -1);
        StringBuilder sb = new StringBuilder("--- a/" + name + "\n+++ b/" + name + "\n");
        if ((long) a.length * b.length > MAX_DIFF_CELLS) {
            sb.append("@@ file replaced: ").append(a.length).append(" lines -> ").append(b.length).append(" lines @@\n");
            return sb.toString();
        }
        int[][] lcs = new int[a.length + 1][b.length + 1];
        for (int i = a.length - 1; i >= 0; i--) {
            for (int j = b.length - 1; j >= 0; j--) {
                lcs[i][j] = a[i].equals(b[j]) ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        // Edit script: ' ' keep, '-' remove, '+' add, with each line's
        // position in both texts.
        List<int[]> ops = new ArrayList<int[]>();
        int i = 0;
        int j = 0;
        while (i < a.length || j < b.length) {
            if (i < a.length && j < b.length && a[i].equals(b[j])) {
                ops.add(new int[] {' ', i++, j++});
            } else if (i < a.length && (j == b.length || lcs[i + 1][j] >= lcs[i][j + 1])) {
                // Removals before additions, as diff prints them.
                ops.add(new int[] {'-', i++, j});
            } else {
                ops.add(new int[] {'+', i, j++});
            }
        }
        int k = 0;
        while (k < ops.size()) {
            if (ops.get(k)[0] == ' ') {
                k++;
                continue;
            }
            int start = Math.max(0, k - 3);
            int end = k;
            int lastChange = k;
            while (end < ops.size() && (ops.get(end)[0] != ' ' || end - lastChange <= 6)) {
                if (ops.get(end)[0] != ' ') {
                    lastChange = end;
                }
                end++;
            }
            end = Math.min(ops.size(), lastChange + 4);
            int aCount = 0;
            int bCount = 0;
            for (int x = start; x < end; x++) {
                int op = ops.get(x)[0];
                if (op != '+') {
                    aCount++;
                }
                if (op != '-') {
                    bCount++;
                }
            }
            sb.append("@@ -").append(ops.get(start)[1] + 1).append(',').append(aCount)
                    .append(" +").append(ops.get(start)[2] + 1).append(',').append(bCount).append(" @@\n");
            for (int x = start; x < end; x++) {
                int[] op = ops.get(x);
                sb.append((char) op[0]).append(op[0] == '+' ? b[op[2]] : a[op[1]]).append('\n');
            }
            k = end;
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- rules

    /// A top-level rule of a stylesheet, as offsets into its text.
    static final class CssRule {
        final int selectorStart;
        final int open;
        final int close;

        private CssRule(int selectorStart, int open, int close) {
            this.selectorStart = selectorStart;
            this.open = open;
            this.close = close;
        }

        int line(String text) {
            int n = 1;
            for (int i = 0; i < selectorStart; i++) {
                if (text.charAt(i) == '\n') {
                    n++;
                }
            }
            return n;
        }

        /// The top-level rule whose selector reads `selector`, whitespace
        /// aside. Rules inside `@media` are not matched: an edit there is a
        /// decision about a device class this tool cannot make.
        static CssRule find(String text, String selector) {
            String plain = blankComments(text);
            String want = squash(selector);
            int depth = 0;
            int start = 0;
            int ruleOpen = -1;
            for (int i = 0; i < plain.length(); i++) {
                char c = plain.charAt(i);
                if (c == '{') {
                    if (depth == 0) {
                        ruleOpen = i;
                    }
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        String prelude = plain.substring(start, ruleOpen);
                        if (squash(prelude).equals(want)) {
                            int s = start;
                            while (Character.isWhitespace(plain.charAt(s))) {
                                s++;
                            }
                            return new CssRule(s, ruleOpen, i);
                        }
                        start = i + 1;
                    } else if (depth < 0) {
                        depth = 0;
                        start = i + 1;
                    }
                } else if (c == ';' && depth == 0) {
                    start = i + 1;
                }
            }
            return null;
        }

        /// Each declaration as `property: value`, in order.
        List<String> declarations(String text) {
            List<String> out = new ArrayList<String>();
            for (int[] d : declarationSpans(text)) {
                out.add(text.substring(d[0], d[1]).trim().replaceAll("\\s+", " "));
            }
            return out;
        }

        /// Start and end (before the semicolon) of every declaration, and
        /// the offset of its colon.
        List<int[]> declarationSpans(String text) {
            String plain = blankComments(text);
            List<int[]> out = new ArrayList<int[]>();
            int start = open + 1;
            int depth = 0;
            boolean quoted = false;
            char quote = 0;
            for (int i = open + 1; i <= close; i++) {
                char c = i < close ? plain.charAt(i) : ';';
                if (quoted) {
                    quoted = c != quote;
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
                    int colon = plain.indexOf(':', start);
                    if (colon > start && colon < i && plain.substring(start, i).trim().length() > 0) {
                        out.add(new int[] {start, i, colon});
                    }
                    start = i + 1;
                }
            }
            return out;
        }

        /// `text` with `property: value` set in the rule for `selector`;
        /// an empty `value` removes the declaration.
        static String set(String text, String selector, String property, String value) {
            CssRule rule = find(text, selector);
            if (rule == null) {
                if (value.length() == 0) {
                    return text;
                }
                String sep = text.length() == 0 || text.endsWith("\n") ? "" : "\n";
                return text + sep + "\n" + selector + " {\n    " + property + ": " + value + ";\n}\n";
            }
            List<int[]> spans = rule.declarationSpans(text);
            for (int s = spans.size() - 1; s >= 0; s--) {
                int[] d = spans.get(s);
                if (!text.substring(d[0], d[2]).trim().equals(property)) {
                    continue;
                }
                if (value.length() == 0) {
                    // The declaration, its semicolon, and the line it leaves empty.
                    int from = d[0];
                    int to = d[1] < rule.close && text.charAt(d[1]) == ';' ? d[1] + 1 : d[1];
                    int lineStart = text.lastIndexOf('\n', from + leading(text, from, to)) + 1;
                    int lineEnd = text.indexOf('\n', to);
                    if (lineEnd >= 0 && lineEnd < rule.close
                            && text.substring(lineStart, from + leading(text, from, to)).trim().length() == 0
                            && text.substring(to, lineEnd).trim().length() == 0) {
                        return text.substring(0, lineStart) + text.substring(lineEnd + 1);
                    }
                    return text.substring(0, from) + text.substring(to);
                }
                int valueStart = d[2] + 1;
                return text.substring(0, valueStart) + " " + value + text.substring(d[1]);
            }
            if (value.length() == 0) {
                return text;
            }
            String indent = "    ";
            if (!spans.isEmpty()) {
                int[] last = spans.get(spans.size() - 1);
                int at = last[0] + leading(text, last[0], last[1]);
                int ls = text.lastIndexOf('\n', at) + 1;
                if (ls > rule.open && text.substring(ls, at).trim().length() == 0) {
                    indent = text.substring(ls, at);
                }
            }
            // After the last declaration, which may lack its semicolon.
            int insertAt = rule.close;
            while (insertAt > rule.open + 1 && Character.isWhitespace(text.charAt(insertAt - 1))) {
                insertAt--;
            }
            String before = text.substring(0, insertAt);
            boolean needsSemicolon = !spans.isEmpty() && !before.endsWith(";") && !before.endsWith("{");
            return before + (needsSemicolon ? ";" : "") + "\n" + indent + property + ": " + value + ";"
                    + text.substring(insertAt);
        }

        private static int leading(String s, int from, int to) {
            int n = 0;
            while (from + n < to && Character.isWhitespace(s.charAt(from + n))) {
                n++;
            }
            return n;
        }

        private static String squash(String s) {
            return s.trim().replaceAll("\\s+", " ").replaceAll("\\s*,\\s*", ",");
        }

        /// Comments as spaces of the same length, so offsets still match.
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
    }
}
