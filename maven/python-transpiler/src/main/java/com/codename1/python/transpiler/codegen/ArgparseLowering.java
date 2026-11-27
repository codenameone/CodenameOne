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
package com.codename1.python.transpiler.codegen;

import com.codename1.python.transpiler.ast.Node;
import com.codename1.python.transpiler.ast.Node.Alias;
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.ExprStmt;
import com.codename1.python.transpiler.ast.Node.Import;
import com.codename1.python.transpiler.ast.Node.ImportFrom;
import com.codename1.python.transpiler.ast.Node.Keyword;
import com.codename1.python.transpiler.ast.Node.Starred;
import com.codename1.python.transpiler.parser.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Gives every argparse destination a store written with its literal name.
///
/// The bundled argparse keeps parsed values as attributes of a `Namespace`
/// named by each action's `dest` string; CPython stores them with
/// `setattr(namespace, action.dest, value)`, which is reflection. Here each
/// call the build can read,
///
/// ```python
/// p.add_argument("-c", "--count", type=int)
/// p.set_defaults(handler="adder")
/// p.add_subparsers(dest="command")
/// ```
///
/// gets the accessors of its destination as one more keyword, a tuple of
/// `(store, load, has)` lambdas each naming the attribute literally,
///
/// ```python
/// p.add_argument("-c", "--count", type=int, _cn1_acc=(
///     lambda n, v: setattr(n, "count", v),
///     lambda n, d: getattr(n, "count", d),
///     lambda n: hasattr(n, "count")))
/// ```
///
/// (`_cn1_accs=(("handler", acc),)` for `set_defaults`). The dest is
/// derived exactly as argparse derives it: `dest=`, else the first long
/// option, else the first short one, prefix stripped and `-` made `_`; a
/// positional's dest is its name. The stdlib stores through the accessor
/// the action carries, so nothing looks an attribute up by name.
///
/// Only modules that import argparse are touched. A call the build cannot
/// read -- a computed option string or dest, `*args`, `**kwargs`, a dest
/// that is not an identifier -- is refused in strict mode, naming its line,
/// when its parser may reach `parse_args`/`parse_known_args`
/// ([#reachesParse]). A parser the build sees used only to build and print
/// help keeps such an argument, with no accessor: it is never stored.
public final class ArgparseLowering {

    private ArgparseLowering() {
    }

    /// Adds the accessors to the argparse calls of `tree`, the module at `path`.
    public static void rewrite(Node.Module tree, String path) {
        if (path.endsWith("argparse.py") || !importsArgparse(tree)) {
            return;
        }
        // Other prefix characters change how a dest is derived from the
        // option strings: the build reads none of them in such a module.
        boolean prefixChars = mentionsPrefixChars(tree);
        List<Call> calls = new ArrayList<Call>();
        Map<Call, Object> scopes = new IdentityHashMap<Call, Object>();
        collect(tree, tree.body, calls, scopes);
        for (Call c : calls) {
            String m = c.func instanceof Attribute ? ((Attribute) c.func).attr : "Namespace";
            if ("Namespace".equals(m)) {
                if (!namespace(c, path)) {
                    c.refusal = "argparse.Namespace(...) with keywords the build cannot read (**mapping) stores"
                            + " attributes by computed name; pass each attribute as a literal keyword";
                }
            } else if ("set_defaults".equals(m)) {
                if (!defaults(c, path) && reachesParse(c, scopes.get(c))) {
                    c.refusal = "set_defaults(...) with keywords the build cannot read (**mapping) on a parser"
                            + " that reaches parse_args; pass each default as a literal keyword";
                }
            } else {
                boolean arg = "add_argument".equals(m);
                String dest = arg ? prefixChars ? null : argumentDest(c) : subparsersDest(c);
                if (dest != null) {
                    c.keywords.add(new Keyword("_cn1_acc", accessor(dest, path)));
                } else if ((arg || storesDest(c)) && reachesParse(c, scopes.get(c))) {
                    c.refusal = m + "(...) whose destination the build cannot read (a computed option"
                            + " string or dest, *args or **kwargs) on a parser that reaches parse_args or"
                            + " parse_known_args; write the option strings and dest= as literals";
                }
            }
        }
    }

    /// An add_subparsers call that stores a destination: one with a `dest`
    /// keyword (or a `**mapping` that may hold one).
    private static boolean storesDest(Call c) {
        for (Keyword k : c.keywords) {
            if (k.arg == null || "dest".equals(k.arg)) {
                return true;
            }
        }
        return false;
    }

    /// Methods of a parser, group or subparsers object that never parse:
    /// a parser used only through these builds help and never stores.
    private static final Set<String> NO_PARSE = new HashSet<String>(java.util.Arrays.asList("add_argument",
            "add_argument_group", "add_mutually_exclusive_group", "add_subparsers", "add_parser", "set_defaults",
            "print_help", "format_help", "print_usage", "format_usage", "get_default", "register"));

    /// Methods that make a new member of a parser's family.
    private static final Set<String> FAMILY = new HashSet<String>(java.util.Arrays.asList("add_argument_group",
            "add_mutually_exclusive_group", "add_subparsers", "add_parser"));

    /// Whether the parser `c` adds to may reach parse_args or
    /// parse_known_args. Only a parser the build can see whole is exempt:
    /// made by `ArgumentParser(...)` into a name bound once in `scope`,
    /// every member of its family (groups, subparsers, sub-parsers, each a
    /// name bound once from one of [#FAMILY]) used only as the receiver of
    /// a method of [#NO_PARSE]. Anything else -- another receiver, the
    /// parser passed, returned or stored -- may parse.
    private static boolean reachesParse(Call c, Object scope) {
        if (!(c.func instanceof Attribute) || !(((Attribute) c.func).value instanceof Node.Name) || scope == null) {
            return true;
        }
        Map<String, List<Expr>> binds = new HashMap<String, List<Expr>>();
        binds(scope, binds);
        String root = ((Node.Name) ((Attribute) c.func).value).id;
        Set<String> seen = new HashSet<String>();
        boolean made = false;
        while (!made) {
            List<Expr> v = binds.get(root);
            if (!seen.add(root) || v == null || v.size() != 1 || !(v.get(0) instanceof Call)) {
                return true;
            }
            Call mk = (Call) v.get(0);
            if (mk.func instanceof Attribute && FAMILY.contains(((Attribute) mk.func).attr)
                    && ((Attribute) mk.func).value instanceof Node.Name) {
                root = ((Node.Name) ((Attribute) mk.func).value).id;
            } else if (mk.func instanceof Node.Name && "ArgumentParser".equals(((Node.Name) mk.func).id)
                    || mk.func instanceof Attribute && "ArgumentParser".equals(((Attribute) mk.func).attr)) {
                made = true;
            } else {
                return true;
            }
        }
        Set<String> family = new HashSet<String>();
        family.add(root);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Map.Entry<String, List<Expr>> e : binds.entrySet()) {
                if (family.contains(e.getKey())) {
                    continue;
                }
                for (Expr v : e.getValue()) {
                    if (v instanceof Call && ((Call) v).func instanceof Attribute
                            && ((Attribute) ((Call) v).func).value instanceof Node.Name
                            && family.contains(((Node.Name) ((Attribute) ((Call) v).func).value).id)) {
                        if (e.getValue().size() != 1 || !FAMILY.contains(((Attribute) ((Call) v).func).attr)) {
                            return true;
                        }
                        family.add(e.getKey());
                        grew = true;
                    }
                }
            }
        }
        boolean[] bad = new boolean[1];
        uses(scope, family, bad);
        return bad[0];
    }

    private static void binds(Object o, Map<String, List<Expr>> out) {
        if (o instanceof Node.Assign) {
            Node.Assign a = (Node.Assign) o;
            for (Expr t : a.targets) {
                if (t instanceof Node.Name) {
                    out.computeIfAbsent(((Node.Name) t).id, k -> new ArrayList<Expr>()).add(a.value);
                } else {
                    names(t, out);
                }
            }
        } else if (o instanceof Node.For) {
            names(((Node.For) o).target, out);
        } else if (o instanceof Node.AugAssign) {
            names(((Node.AugAssign) o).target, out);
        } else if (o instanceof Node.AnnAssign) {
            names(((Node.AnnAssign) o).target, out);
        } else if (o instanceof Node.NamedExpr) {
            names(((Node.NamedExpr) o).target, out);
        } else if (o instanceof Node.WithItem) {
            names(((Node.WithItem) o).vars, out);
        } else if (o instanceof Node.Delete) {
            names(((Node.Delete) o).targets, out);
        } else if (o instanceof Node.Arg) {
            out.computeIfAbsent(((Node.Arg) o).name, k -> new ArrayList<Expr>()).add(null);
        } else if (o instanceof Node.FunctionDef) {
            out.computeIfAbsent(((Node.FunctionDef) o).name, k -> new ArrayList<Expr>()).add(null);
        } else if (o instanceof Node.ClassDef) {
            out.computeIfAbsent(((Node.ClassDef) o).name, k -> new ArrayList<Expr>()).add(null);
        }
        Infer.children(o, x -> binds(x, out));
    }

    /// Every name bound by target `o`, as a binding that is not one call.
    private static void names(Object o, Map<String, List<Expr>> out) {
        if (o instanceof Node.Name) {
            out.computeIfAbsent(((Node.Name) o).id, k -> new ArrayList<Expr>()).add(null);
        }
        if (o != null) {
            Infer.children(o, x -> names(x, out));
        }
    }

    /// Sets `bad` on any read of a `family` name that is not the receiver
    /// of a [#NO_PARSE] method call.
    private static void uses(Object o, Set<String> family, boolean[] bad) {
        if (bad[0] || o == null) {
            return;
        }
        if (o instanceof Call && ((Call) o).func instanceof Attribute
                && ((Attribute) ((Call) o).func).value instanceof Node.Name
                && family.contains(((Node.Name) ((Attribute) ((Call) o).func).value).id)) {
            if (!NO_PARSE.contains(((Attribute) ((Call) o).func).attr)) {
                bad[0] = true;
                return;
            }
            for (Expr a : ((Call) o).args) {
                uses(a, family, bad);
            }
            for (Keyword k : ((Call) o).keywords) {
                uses(k.value, family, bad);
            }
            return;
        }
        if (o instanceof Node.Assign) {
            // `p = ArgumentParser()` binds a member; the binding is no use.
            for (Expr t : ((Node.Assign) o).targets) {
                if (!(t instanceof Node.Name)) {
                    uses(t, family, bad);
                }
            }
            uses(((Node.Assign) o).value, family, bad);
            return;
        }
        if (o instanceof Node.Name && family.contains(((Node.Name) o).id)) {
            bad[0] = true;
            return;
        }
        Infer.children(o, x -> uses(x, family, bad));
    }

    private static boolean importsArgparse(Node.Module tree) {
        for (Node.Stmt s : tree.body) {
            if (s instanceof ImportFrom && "argparse".equals(((ImportFrom) s).module)) {
                return true;
            }
            if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    if ("argparse".equals(a.name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean mentionsPrefixChars(Object o) {
        if (o instanceof Keyword && "prefix_chars".equals(((Keyword) o).arg)) {
            return true;
        }
        boolean[] hit = new boolean[1];
        Infer.children(o, x -> hit[0] |= mentionsPrefixChars(x));
        return hit[0];
    }

    /// The argparse calls under `o`, each with the body of the function
    /// (or module) it is in, which is where its parser is looked for.
    private static void collect(Object o, Object scope, List<Call> out, Map<Call, Object> scopes) {
        if (o instanceof Call && ((Call) o).func instanceof Attribute) {
            String m = ((Attribute) ((Call) o).func).attr;
            if ("add_argument".equals(m) || "set_defaults".equals(m) || "add_subparsers".equals(m)
                    || "Namespace".equals(m)) {
                out.add((Call) o);
                scopes.put((Call) o, scope);
            }
        } else if (o instanceof Call && ((Call) o).func instanceof Node.Name
                && "Namespace".equals(((Node.Name) ((Call) o).func).id)) {
            // A bare Namespace(...): rewritten as the attribute form, which
            // the stdlib's own Namespace() calls never take keywords in.
            out.add((Call) o);
            scopes.put((Call) o, scope);
        }
        Object inner = o instanceof Node.FunctionDef ? ((Node.FunctionDef) o).body : scope;
        Infer.children(o, x -> collect(x, inner, out, scopes));
    }

    private static String str(Expr e) {
        if (e instanceof Node.Constant && ((Node.Constant) e).kind == Node.Constant.Kind.STR) {
            return ((Node.Constant) e).text;
        }
        return null;
    }

    private static boolean plainKeywords(Call c) {
        for (Keyword k : c.keywords) {
            if (k.arg == null || k.arg.startsWith("_cn1_")) {
                return false;
            }
        }
        return true;
    }

    private static String argumentDest(Call c) {
        if (c.args.isEmpty() || !plainKeywords(c)) {
            return null;
        }
        List<String> opts = new ArrayList<String>();
        for (Expr a : c.args) {
            String s = a instanceof Starred ? null : str(a);
            if (s == null || s.isEmpty()) {
                return null;
            }
            opts.add(s);
        }
        String explicit = null;
        for (Keyword k : c.keywords) {
            if ("dest".equals(k.arg)) {
                explicit = str(k.value);
                if (explicit == null) {
                    return null;
                }
            }
        }
        if (opts.size() == 1 && opts.get(0).charAt(0) != '-') {
            return explicit != null ? null : ident(opts.get(0));
        }
        if (explicit != null) {
            return ident(explicit);
        }
        String first = null;
        for (String o : opts) {
            if (o.charAt(0) != '-') {
                return null;
            }
            if (first == null && o.length() > 1 && o.charAt(1) == '-') {
                first = o;
            }
        }
        if (first == null) {
            first = opts.get(0);
        }
        int i = 0;
        while (i < first.length() && first.charAt(i) == '-') {
            i++;
        }
        return ident(first.substring(i).replace('-', '_'));
    }

    private static String subparsersDest(Call c) {
        if (!c.args.isEmpty() || !plainKeywords(c)) {
            return null;
        }
        for (Keyword k : c.keywords) {
            if ("dest".equals(k.arg)) {
                String d = str(k.value);
                return d == null ? null : ident(d);
            }
        }
        return null;
    }

    private static boolean defaults(Call c, String path) {
        if (!c.args.isEmpty() || c.keywords.isEmpty() || !plainKeywords(c)) {
            return c.args.isEmpty() && plainKeywords(c);
        }
        StringBuilder src = new StringBuilder("(");
        for (Keyword k : c.keywords) {
            src.append("(\"").append(k.arg).append("\", ").append(accessorSource(k.arg)).append("), ");
        }
        src.append(")");
        c.keywords.add(new Keyword("_cn1_accs", parseExpr(src.toString(), path)));
        return true;
    }

    /// `Namespace(a=1, b=x)` becomes `Namespace(_cn1_init=((store_a, 1),
    /// (store_b, x)))`: the values are evaluated in the same order and each
    /// is stored through its own literal store, in keyword order.
    private static boolean namespace(Call c, String path) {
        if (!c.args.isEmpty() || c.keywords.isEmpty() || !plainKeywords(c)) {
            return c.keywords.isEmpty() || plainKeywords(c);
        }
        Node.TupleExpr rows = new Node.TupleExpr(new ArrayList<Expr>());
        for (Keyword k : c.keywords) {
            Expr store = parseExpr("lambda _cn1_n, _cn1_v: setattr(_cn1_n, \"" + k.arg + "\", _cn1_v)", path);
            List<Expr> row = new ArrayList<Expr>();
            row.add(store);
            row.add(k.value);
            rows.elts.add(new Node.TupleExpr(row));
        }
        c.keywords.clear();
        c.keywords.add(new Keyword("_cn1_init", rows));
        return true;
    }

    private static String ident(String s) {
        if (s.isEmpty() || !(Character.isLetter(s.charAt(0)) || s.charAt(0) == '_')) {
            return null;
        }
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch > 127 || !(Character.isLetterOrDigit(ch) || ch == '_')) {
                return null;
            }
        }
        return s;
    }

    private static String accessorSource(String dest) {
        return "(lambda _cn1_n, _cn1_v: setattr(_cn1_n, \"" + dest + "\", _cn1_v), "
                + "lambda _cn1_n, _cn1_d: getattr(_cn1_n, \"" + dest + "\", _cn1_d), "
                + "lambda _cn1_n: hasattr(_cn1_n, \"" + dest + "\"))";
    }

    private static Expr accessor(String dest, String path) {
        return parseExpr(accessorSource(dest), path);
    }

    private static Expr parseExpr(String src, String path) {
        return ((ExprStmt) Parser.parse(src + "\n", path).body.get(0)).value;
    }
}
