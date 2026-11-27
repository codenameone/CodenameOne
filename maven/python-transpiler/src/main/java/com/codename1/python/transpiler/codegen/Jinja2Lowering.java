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
package com.codename1.python.transpiler.codegen;

import com.codename1.python.transpiler.ast.Node;
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Name;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Jinja reads `{{ user.name }}` by trying the attribute `name` and then
/// the item; with the name taken from template text that is a getattr with
/// a computed name. Here every literal template source of the program is
/// lexed and tokenized at build time exactly as the bundled `jinja2` does
/// (`_lex`, `_tokenize`), and each `.name` site and each `['literal']` site
/// gets an accessor function with the name written into its code. The
/// generated module `_cn1_jinja2_tables` maps a template source to one tuple
/// per lexed tag, holding that tag's accessors in token order; the runtime
/// tokenizer hands them out by position. Nothing is looked up by attribute
/// name.
///
/// A template whose source is computed at run time has no accessors: a
/// `Template(...)`, `from_string(...)` or `render_template_string(...)` with
/// a source that is not a string literal is refused in strict mode, and a
/// template the build did not see that uses either access fails to parse.
public final class Jinja2Lowering {
    /// The path of the generated module.
    public static final String PATH = "_cn1_jinja2_tables.py";

    private static final Set<String> CONSTRUCTORS = new HashSet<String>(Arrays.asList("Template", "from_string",
            "render_template_string"));
    private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList("False", "None", "True", "and",
            "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif", "else", "except",
            "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass",
            "raise", "return", "try", "while", "with", "yield"));
    private static final String[] OPS2 = {"==", "!=", "<=", ">=", "//", "**"};

    private Jinja2Lowering() {
    }

    /// One accessor: an attribute site (attribute, then item) or a literal
    /// item site (item, then attribute).
    private static final class Site {
        final boolean attr;
        final String key;

        Site(boolean attr, String key) {
            this.attr = attr;
            this.key = key;
        }

        String id() {
            return (attr ? "a:" : "i:") + key;
        }
    }

    /// The generated module's source, or null when the program has no jinja2.
    public static String rewrite(Map<String, Node.Module> trees) {
        return rewrite(trees, java.util.Collections.<String, String>emptyMap());
    }

    /// Whether a project file is one jinja2's loaders may read, which the
    /// build passes to the transpiler beside the modules. Must match
    /// `PythonProjectBuilder.isTemplateFile`.
    public static boolean isTemplateFile(String path) {
        String[] kinds = {".html", ".htm", ".j2", ".jinja", ".jinja2", ".tmpl", ".tpl", ".xml", ".txt", ".md"};
        for (String k : kinds) {
            if (path.endsWith(k)) {
                return true;
            }
        }
        return false;
    }

    /// As [#rewrite(Map)], with `sources` the program's files by path: every
    /// template file among them is lexed into the tables, as a literal
    /// template is, so a `FileSystemLoader` or `PackageLoader` template is
    /// compiled at build time. A loader over a literal directory the build
    /// holds no template of, and a literal template name no file of the
    /// program's loaders has, are refused in strict mode: they would
    /// otherwise fail on the device.
    public static String rewrite(Map<String, Node.Module> trees, Map<String, String> sources) {
        if (!imports(trees)) {
            return null;
        }
        Set<String> literals = new java.util.LinkedHashSet<String>();
        List<String> files = new ArrayList<String>();
        for (Map.Entry<String, String> e : sources.entrySet()) {
            String p = e.getKey().replace('\\', '/');
            if (isTemplateFile(p)) {
                files.add(p);
                String t = e.getValue();
                if (t.contains("{{") || t.contains("{%")) {
                    // Keyed by the whole text, as the loader reads it: raw,
                    // and with universal newlines as a text-mode open gives.
                    literals.add(t);
                    String u = t.replace("\r\n", "\n").replace('\r', '\n');
                    if (!u.equals(t)) {
                        literals.add(u);
                    }
                }
            }
        }
        checkLoaders(trees, files);
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            String path = e.getKey().replace('\\', '/');
            if (path.startsWith("jinja2/") || PATH.equals(path)) {
                continue;
            }
            boolean site = path.startsWith("flask/");
            walk(e.getValue(), literals, site, literalNames(e.getValue()));
        }
        Map<String, String> fns = new LinkedHashMap<String, String>();
        StringBuilder fsrc = new StringBuilder();
        StringBuilder tables = new StringBuilder("\nTABLES = {\n");
        for (String src : literals) {
            List<List<Site>> tags;
            try {
                tags = tags(src);
            } catch (RuntimeException ex) {
                // Not a template this lexer reads; jinja2 would refuse it too.
                continue;
            }
            boolean any = false;
            for (List<Site> t : tags) {
                any |= !t.isEmpty();
            }
            if (!any) {
                continue;
            }
            tables.append("    ").append(str(src)).append(": (");
            for (List<Site> t : tags) {
                tables.append('(');
                for (Site s : t) {
                    String fn = fns.get(s.id());
                    if (fn == null) {
                        fn = "_s" + fns.size();
                        fns.put(s.id(), fn);
                        accessor(fsrc, fn, s);
                    }
                    tables.append(fn).append(", ");
                }
                tables.append("), ");
            }
            tables.append("),\n");
        }
        tables.append("}\n");
        return "# Generated by Jinja2Lowering: one accessor per attribute or literal item site\n"
                + "# of the program's literal templates.\n\nMISS = object()\n" + fsrc + tables;
    }

    /// Refuses a loader over a literal directory without templates, and a
    /// literal `get_template` name no such loader's directory has, when the
    /// program's only loaders are file loaders with literal directories.
    private static void checkLoaders(Map<String, Node.Module> trees, final List<String> files) {
        final List<String> dirs = new ArrayList<String>();
        final List<Call> names = new ArrayList<Call>();
        final List<Call> loaders = new ArrayList<Call>();
        final boolean[] other = {false};
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            String path = e.getKey().replace('\\', '/');
            if (path.startsWith("jinja2/") || path.startsWith("flask/") || PATH.equals(path)) {
                continue;
            }
            scanLoaders(e.getValue(), files, dirs, names, other, loaders);
        }
        if (other[0] || dirs.isEmpty()) {
            return;
        }
        for (Call c : names) {
            String n = ((Constant) c.args.get(0)).text;
            boolean found = false;
            for (String d : dirs) {
                found |= files.contains(d + n);
            }
            if (!found && loaders.get(0).refusal == null) {
                // On the loader: the call naming the template is rebuilt by
                // later passes, and the loader is what cannot provide it.
                loaders.get(0).refusal = "template " + n + " is in no directory of the program's template"
                        + " loaders; the build compiles only the templates it can see";
            }
        }
    }

    private static void scanLoaders(Object o, final List<String> files, final List<String> dirs,
            final List<Call> names, final boolean[] other, final List<Call> loaders) {
        if (o instanceof Call) {
            Call c = (Call) o;
            String f = c.func instanceof Name ? ((Name) c.func).id
                    : c.func instanceof Attribute ? ((Attribute) c.func).attr : null;
            String dir = null;
            boolean loader = false;
            if ("FileSystemLoader".equals(f)) {
                loader = true;
                if (c.args.size() == 1 && isStr(c.args.get(0))) {
                    dir = ((Constant) c.args.get(0)).text;
                }
            } else if ("PackageLoader".equals(f)) {
                loader = true;
                if (!c.args.isEmpty() && c.args.size() <= 2 && isStr(c.args.get(0))
                        && (c.args.size() == 1 || isStr(c.args.get(1))) && c.keywords.isEmpty()) {
                    dir = ((Constant) c.args.get(0)).text.replace('.', '/') + "/"
                            + (c.args.size() == 2 ? ((Constant) c.args.get(1)).text : "templates");
                }
            } else if (f != null && f.endsWith("Loader")) {
                other[0] = true;
            } else if (("get_template".equals(f) || "render_template".equals(f)) && c.args.size() >= 1
                    && isStr(c.args.get(0))) {
                names.add(c);
            }
            if (loader && dir == null) {
                // A computed directory: the build cannot tell which files
                // it reads, so it checks no names against it.
                other[0] = true;
            } else if (dir != null) {
                while (dir.startsWith("./")) {
                    dir = dir.substring(2);
                }
                if (!dir.endsWith("/")) {
                    dir = dir + "/";
                }
                boolean any = false;
                for (String p : files) {
                    any |= p.startsWith(dir);
                }
                if (!any) {
                    c.refusal = f + " reads " + dir + ", where the build sees no template file; templates are"
                            + " compiled at build time from the project's files";
                }
                dirs.add(dir);
                loaders.add(c);
            }
        }
        Infer.children(o, x -> scanLoaders(x, files, dirs, names, other, loaders));
    }

    private static boolean isStr(Node.Expr e) {
        return e instanceof Constant && ((Constant) e).kind == Constant.Kind.STR;
    }

    /// True when a module of the program imports jinja2 or flask, which
    /// imports the generated module.
    private static boolean imports(Map<String, Node.Module> trees) {
        boolean[] hit = {false};
        for (Node.Module m : trees.values()) {
            Infer.children(m, c -> scanImports(c, hit));
        }
        return hit[0];
    }

    private static void scanImports(Object o, boolean[] hit) {
        if (o instanceof Node.Import) {
            for (Node.Alias a : ((Node.Import) o).names) {
                hit[0] |= isJinja(a.name);
            }
        } else if (o instanceof Node.ImportFrom && ((Node.ImportFrom) o).level == 0) {
            hit[0] |= isJinja(((Node.ImportFrom) o).module);
        }
        if (!hit[0]) {
            Infer.children(o, c -> scanImports(c, hit));
        }
    }

    private static boolean isJinja(String m) {
        return m != null && (m.equals("jinja2") || m.startsWith("jinja2.") || m.equals("flask")
                || m.startsWith("flask."));
    }

    private static void accessor(StringBuilder out, String fn, Site s) {
        boolean ident = s.key.matches("[A-Za-z_][A-Za-z0-9_]*") && !KEYWORDS.contains(s.key);
        String attr = "    try:\n        return o." + s.key + "\n    except AttributeError:\n        pass\n";
        String item = "    try:\n        return o[" + str(s.key) + "]\n"
                + "    except (TypeError, LookupError, AttributeError):\n        pass\n";
        out.append("\n\ndef ").append(fn).append("(o):\n");
        if (s.attr) {
            if (ident) {
                out.append(attr);
            }
            out.append(item);
        } else {
            out.append(item);
            if (ident) {
                out.append(attr);
            }
        }
        out.append("    return MISS\n");
    }

    /// Module names bound exactly once, to a string literal.
    private static Set<String> literalNames(Node.Module tree) {
        Map<String, Integer> bindings = new java.util.HashMap<String, Integer>();
        for (Node.Stmt s : tree.body) {
            DataclassInit.count(s, bindings);
        }
        Set<String> out = new HashSet<String>();
        for (Node.Stmt s : tree.body) {
            if (s instanceof Node.Assign && ((Node.Assign) s).targets.size() == 1
                    && ((Node.Assign) s).targets.get(0) instanceof Name
                    && ((Node.Assign) s).value instanceof Constant
                    && ((Constant) ((Node.Assign) s).value).kind == Constant.Kind.STR) {
                String n = ((Name) ((Node.Assign) s).targets.get(0)).id;
                if (bindings.getOrDefault(n, 0) == 1) {
                    out.add(n);
                }
            }
        }
        return out;
    }

    private static void walk(Object o, Set<String> literals, boolean site, Set<String> names) {
        if (o instanceof Constant && ((Constant) o).kind == Constant.Kind.STR) {
            String t = ((Constant) o).text;
            if (t.contains("{{") || t.contains("{%")) {
                literals.add(t);
            }
            return;
        }
        if (!site && o instanceof Call) {
            Call c = (Call) o;
            String f = c.func instanceof Name ? ((Name) c.func).id
                    : c.func instanceof Attribute ? ((Attribute) c.func).attr : null;
            Node.Expr src = c.args.isEmpty() ? null : c.args.get(0);
            if (src == null) {
                for (Node.Keyword k : c.keywords) {
                    if ("source".equals(k.arg)) {
                        src = k.value;
                    }
                }
            }
            if (f != null && CONSTRUCTORS.contains(f) && src != null
                    && !(src instanceof Constant && ((Constant) src).kind == Constant.Kind.STR)
                    && !(src instanceof Name && names.contains(((Name) src).id))) {
                c.refusal = f + " with a template source computed at run time cannot be compiled at build"
                        + " time; give the source as a string literal";
            }
        }
        Infer.children(o, c -> walk(c, literals, site, names));
    }

    /// The sites of each var/block tag, in the order `jinja2._lex` emits them.
    static List<List<Site>> tags(String src) {
        List<List<Site>> out = new ArrayList<List<Site>>();
        int i = 0;
        int n = src.length();
        while (i < n) {
            int j = -1;
            int k = i;
            while (true) {
                k = src.indexOf('{', k);
                if (k < 0 || k + 1 >= n) {
                    break;
                }
                char c = src.charAt(k + 1);
                if (c == '{' || c == '%' || c == '#') {
                    j = k;
                    break;
                }
                k++;
            }
            if (j < 0) {
                break;
            }
            char kind = src.charAt(j + 1);
            int start = j + 2;
            if (start < n && (src.charAt(start) == '-' || src.charAt(start) == '+')) {
                start++;
            }
            String close = kind == '{' ? "}}" : kind == '%' ? "%}" : "#}";
            int e = findClose(src, start, close, kind == '#');
            if (e < 0) {
                throw new IllegalArgumentException("unexpected end of template");
            }
            String inner = src.substring(start, e);
            if (inner.endsWith("-") || inner.endsWith("+")) {
                inner = inner.substring(0, inner.length() - 1);
            }
            i = e + 2;
            if (kind == '#') {
                continue;
            }
            inner = inner.trim();
            if (kind == '%' && "raw".equals(inner)) {
                int m = src.indexOf("{%", i);
                while (m >= 0) {
                    int q = src.indexOf("%}", m);
                    if (q < 0) {
                        throw new IllegalArgumentException("unterminated raw");
                    }
                    if ("endraw".equals(strip(src.substring(m + 2, q)))) {
                        i = q + 2;
                        break;
                    }
                    m = src.indexOf("{%", m + 2);
                }
                continue;
            }
            out.add(sites(inner));
        }
        return out;
    }

    private static String strip(String s) {
        int a = 0;
        int b = s.length();
        while (a < b && " -+\t\r\n".indexOf(s.charAt(a)) >= 0) {
            a++;
        }
        while (b > a && " -+\t\r\n".indexOf(s.charAt(b - 1)) >= 0) {
            b--;
        }
        return s.substring(a, b);
    }

    private static int findClose(String src, int start, String close, boolean comment) {
        int i = start;
        char q = 0;
        int n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (!comment) {
                if (q != 0) {
                    if (c == '\\') {
                        i += 2;
                        continue;
                    }
                    if (c == q) {
                        q = 0;
                    }
                    i++;
                    continue;
                }
                if (c == '\'' || c == '"') {
                    q = c;
                    i++;
                    continue;
                }
            }
            if (src.startsWith(close, i)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /// Tokens as `jinja2._tokenize` makes them: kind ("num", "name", "str",
    /// "op", "end") and text; then the accessor sites in token order.
    private static List<Site> sites(String s) {
        List<String[]> toks = new ArrayList<String[]>();
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (Character.isDigit(c)) {
                int j = i;
                while (j < n && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '_')) {
                    j++;
                }
                if (j + 1 < n && s.charAt(j) == '.' && Character.isDigit(s.charAt(j + 1))) {
                    j++;
                    while (j < n && Character.isDigit(s.charAt(j))) {
                        j++;
                    }
                    toks.add(new String[]{"num", String.valueOf(Double.parseDouble(s.substring(i, j)
                            .replace("_", "")))});
                } else {
                    toks.add(new String[]{"num", new java.math.BigInteger(s.substring(i, j).replace("_", ""))
                            .toString()});
                }
                i = j;
            } else if (Character.isLetter(c) || c == '_') {
                int j = i;
                while (j < n && (Character.isLetterOrDigit(s.charAt(j)) || s.charAt(j) == '_')) {
                    j++;
                }
                toks.add(new String[]{"name", s.substring(i, j)});
                i = j;
            } else if (c == '\'' || c == '"') {
                int j = i + 1;
                StringBuilder buf = new StringBuilder();
                while (j < n && s.charAt(j) != c) {
                    if (s.charAt(j) == '\\' && j + 1 < n) {
                        j++;
                        char x = s.charAt(j);
                        buf.append(x == 'n' ? '\n' : x == 't' ? '\t' : x == 'r' ? '\r' : x);
                    } else {
                        buf.append(s.charAt(j));
                    }
                    j++;
                }
                toks.add(new String[]{"str", buf.toString()});
                i = j + 1;
            } else {
                String two = i + 2 <= n ? s.substring(i, i + 2) : null;
                if (two != null && Arrays.asList(OPS2).contains(two)) {
                    toks.add(new String[]{"op", two});
                    i += 2;
                } else {
                    toks.add(new String[]{"op", String.valueOf(c)});
                    i++;
                }
            }
        }
        toks.add(new String[]{"end", "None"});
        List<Site> out = new ArrayList<Site>();
        for (int t = 0; t < toks.size() - 1; t++) {
            String[] tok = toks.get(t);
            if (!"op".equals(tok[0])) {
                continue;
            }
            if (".".equals(tok[1])) {
                String[] nt = toks.get(t + 1);
                out.add(new Site(true, "end".equals(nt[0]) ? "" : nt[1]));
            } else if ("[".equals(tok[1]) && "str".equals(toks.get(t + 1)[0]) && t + 2 < toks.size()
                    && "op".equals(toks.get(t + 2)[0]) && "]".equals(toks.get(t + 2)[1])) {
                out.add(new Site(false, toks.get(t + 1)[1]));
            }
        }
        return out;
    }

    /// A Python string literal for `s`, ASCII only.
    private static String str(String s) {
        StringBuilder b = new StringBuilder("'");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '\'') {
                b.append('\\').append(c);
            } else if (c == '\n') {
                b.append("\\n");
            } else if (c == '\r') {
                b.append("\\r");
            } else if (c == '\t') {
                b.append("\\t");
            } else if (c < 0x20 || c > 0x7e) {
                b.append('\\').append('u').append(String.format("%04x", (int) c));
            } else {
                b.append(c);
            }
        }
        return b.append('\'').toString();
    }
}
