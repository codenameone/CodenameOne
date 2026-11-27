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

import com.codename1.python.transpiler.ast.Node.Alias;
import com.codename1.python.transpiler.ast.Node.AnnAssign;
import com.codename1.python.transpiler.ast.Node.Assign;
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.BinOp;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.ExprStmt;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.If;
import com.codename1.python.transpiler.ast.Node.Import;
import com.codename1.python.transpiler.ast.Node.ImportFrom;
import com.codename1.python.transpiler.ast.Node.Keyword;
import com.codename1.python.transpiler.ast.Node.Lambda;
import com.codename1.python.transpiler.ast.Node.ListExpr;
import com.codename1.python.transpiler.ast.Node.Module;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.ast.Node.Try;
import com.codename1.python.transpiler.ast.Node.TupleExpr;
import com.codename1.python.transpiler.ast.Node.UnaryOp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The members of every enumeration, defined by the compiler instead of by
/// a metaclass reading the class body.
///
/// CPython's `EnumType.__new__` walks the class namespace and turns each
/// plain value into a member, by name, with `setattr`. Nothing of that can
/// be typed or resolved ahead of time. This pass finds every class deriving
/// from `enum.Enum` (through bases in any module: pptx's `BaseEnum(int,
/// Enum)` and its subclasses) and rewrites the class statement so the body
/// keeps only its methods, followed by one statement per member, in
/// definition order, each naming the member with a build-time string:
///
/// ```python
/// class Color(Enum):
///     def describe(self): ...
/// Color.RED = _cn1_enum._member(Color, "RED", 1)
/// Color.GREEN = _cn1_enum._member(Color, "GREEN", 2)
/// ```
///
/// `_member` makes the member once (the class's own `__new__`/`__init__`
/// included), resolves aliases and `auto()`, and records it in the value
/// and name tables the class's lookups read. Decorators (`@unique`) move
/// after the members, which is when CPython's run. The inference reads the
/// member names back ([#members]) so `Color.RED` is a `Color`, and the
/// stores are trailing class attributes, so each member is a typed static
/// of the class (Layouts#scanTrailing).
///
/// The functional API, `Color = Enum("Color", "RED GREEN")` (or a list of
/// name literals) at the top of a module, becomes the class statement it
/// stands for, each member an `auto()`; the runtime refuses every other
/// form of it.
public final class EnumLowering {
    /// The enumeration bases the library itself defines.
    private static final Set<String> ROOTS = new HashSet<String>(Arrays.asList("Enum", "IntEnum", "StrEnum",
            "Flag", "IntFlag", "ReprEnum"));
    private static final String ENUM_PATH = "enum.py";
    private static final String HELPER = "_cn1_enum";

    /// Member names of each lowered class, in definition order. Weak: a
    /// build daemon transpiles many programs in one JVM, and a strong map
    /// kept every build's syntax trees alive for good. ClassDef keeps
    /// Object's identity equals, so this is still an identity map.
    private static final Map<ClassDef, List<String>> MEMBERS = Collections.synchronizedMap(
            new java.util.WeakHashMap<ClassDef, List<String>>());

    private final Map<String, Module> trees;
    private final Set<String> enums = new HashSet<String>();

    private EnumLowering(Map<String, Module> trees) {
        this.trees = trees;
    }

    /// The members the pass defined for `c`, or null when `c` is no
    /// lowered enumeration.
    public static List<String> members(ClassDef c) {
        return MEMBERS.get(c);
    }

    /// Lowers every enumeration of the program `trees` (path to module).
    public static void rewrite(Map<String, Module> trees) {
        EnumLowering l = new EnumLowering(trees);
        for (String r : ROOTS) {
            l.enums.add(ENUM_PATH + ":" + r);
        }
        for (Map.Entry<String, Module> e : trees.entrySet()) {
            if (!ENUM_PATH.equals(e.getKey())) {
                l.functional(e.getKey(), e.getValue().body);
            }
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map.Entry<String, Module> e : trees.entrySet()) {
                for (Stmt s : e.getValue().body) {
                    if (s instanceof ClassDef && !l.enums.contains(e.getKey() + ":" + ((ClassDef) s).name)
                            && l.derives(e.getKey(), (ClassDef) s)) {
                        l.enums.add(e.getKey() + ":" + ((ClassDef) s).name);
                        changed = true;
                    }
                }
            }
        }
        for (Map.Entry<String, Module> e : trees.entrySet()) {
            boolean[] used = new boolean[1];
            l.walk(e.getKey(), e.getValue().body, used, true);
            if (used[0] && !ENUM_PATH.equals(e.getKey())) {
                List<Alias> a = new ArrayList<Alias>();
                a.add(new Alias("enum", HELPER));
                e.getValue().body.add(firstAfterFuture(e.getValue().body), new Import(a));
            }
        }
        // The members just written, and every other class attribute bound
        // right after its class, become typed statics (Layouts#scanTrailing).
        for (Module m : trees.values()) {
            Layouts.scanTrailing(m);
        }
    }

    /// Rewrites each top-level `Name = Enum("Name", names)` of `body`
    /// (`names` a string of names or a list or tuple of name literals) to
    /// `class Name(Enum)` with one `auto()` member per name.
    private void functional(String path, List<Stmt> body) {
        for (int i = 0; i < body.size(); i++) {
            if (!(body.get(i) instanceof Assign)) {
                continue;
            }
            Assign a = (Assign) body.get(i);
            if (a.targets.size() != 1 || !(a.targets.get(0) instanceof Name) || !(a.value instanceof Call)) {
                continue;
            }
            Call c = (Call) a.value;
            String target = ((Name) a.targets.get(0)).id;
            String root = resolve(path, c.func, 0);
            if (root == null || !root.startsWith(ENUM_PATH + ":") || !ROOTS.contains(root.substring(ENUM_PATH.length() + 1))
                    || c.args.size() != 2 || !c.keywords.isEmpty() || !(c.args.get(0) instanceof Constant)
                    || !target.equals(((Constant) c.args.get(0)).text)) {
                continue;
            }
            List<String> names = new ArrayList<String>();
            Expr n = c.args.get(1);
            if (n instanceof Constant && ((Constant) n).kind == Constant.Kind.STR) {
                for (String x : ((Constant) n).text.replace(',', ' ').split(" ")) {
                    if (!x.isEmpty()) {
                        names.add(x);
                    }
                }
            } else if (n instanceof ListExpr || n instanceof TupleExpr) {
                for (Expr x : n instanceof ListExpr ? ((ListExpr) n).elts : ((TupleExpr) n).elts) {
                    if (!(x instanceof Constant) || ((Constant) x).kind != Constant.Kind.STR) {
                        names = null;
                        break;
                    }
                    names.add(((Constant) x).text);
                }
            } else {
                names = null;
            }
            if (names == null || names.isEmpty()) {
                continue;
            }
            List<Stmt> members = new ArrayList<Stmt>();
            for (String x : names) {
                List<Expr> t = new ArrayList<Expr>();
                t.add(new Name(x));
                members.add(new Assign(t, new Call(new Attribute(new Name(HELPER), "auto"))).at(a.line, a.col));
            }
            List<Expr> bases = new ArrayList<Expr>();
            bases.add(c.func);
            body.set(i, new ClassDef(target, bases, new ArrayList<Keyword>(),
                    members, new ArrayList<Expr>()).at(a.line, a.col));
        }
    }

    private static int firstAfterFuture(List<Stmt> body) {
        int i = 0;
        if (!body.isEmpty() && body.get(0) instanceof ExprStmt && ((ExprStmt) body.get(0)).value instanceof Constant) {
            i = 1;
        }
        while (i < body.size() && body.get(i) instanceof ImportFrom
                && "__future__".equals(((ImportFrom) body.get(i)).module)) {
            i++;
        }
        return i;
    }

    private boolean derives(String path, ClassDef c) {
        if (ENUM_PATH.equals(path) && ROOTS.contains(c.name)) {
            return false;
        }
        for (Expr b : c.bases) {
            String k = resolve(path, b, 0);
            if (k != null && enums.contains(k)) {
                return true;
            }
        }
        return false;
    }

    /// `path:Class` for the class base `b` names in module `path`, or null.
    private String resolve(String path, Expr b, int depth) {
        if (depth > 8) {
            return null;
        }
        if (b instanceof Name) {
            return resolveName(path, ((Name) b).id, depth);
        }
        if (b instanceof Attribute && ((Attribute) b).value instanceof Name) {
            String m = moduleBound(path, ((Name) ((Attribute) b).value).id);
            return m == null ? null : resolveName(m, ((Attribute) b).attr, depth + 1);
        }
        return null;
    }

    /// What `name`, bound at the top of module `path`, is as `path:Class`.
    private String resolveName(String path, String name, int depth) {
        if (ENUM_PATH.equals(path) && ROOTS.contains(name)) {
            return path + ":" + name;
        }
        Module m = trees.get(path);
        if (m == null || depth > 8) {
            return null;
        }
        for (Stmt s : m.body) {
            if (s instanceof ClassDef && ((ClassDef) s).name.equals(name)) {
                return path + ":" + name;
            }
        }
        for (Stmt s : m.body) {
            if (s instanceof ImportFrom) {
                ImportFrom f = (ImportFrom) s;
                for (Alias a : f.names) {
                    if (name.equals(a.asname != null ? a.asname : a.name)) {
                        String mp = modulePath(path, f.module, f.level);
                        return mp == null ? null : resolveName(mp, a.name, depth + 1);
                    }
                    if ("*".equals(a.name)) {
                        String mp = modulePath(path, f.module, f.level);
                        String r = mp == null ? null : resolveName(mp, name, depth + 1);
                        if (r != null) {
                            return r;
                        }
                    }
                }
            }
        }
        return null;
    }

    /// The module path `local` names after `import x [as local]` in `path`.
    private String moduleBound(String path, String local) {
        Module m = trees.get(path);
        if (m == null) {
            return null;
        }
        for (Stmt s : m.body) {
            if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    if (local.equals(a.asname != null ? a.asname : a.name)) {
                        return modulePath(path, a.name, 0);
                    }
                }
            }
            if (s instanceof ImportFrom) {
                ImportFrom f = (ImportFrom) s;
                for (Alias a : f.names) {
                    if (local.equals(a.asname != null ? a.asname : a.name)) {
                        String base = f.module == null ? a.name : f.module + "." + a.name;
                        String mp = modulePath(path, base, f.level);
                        if (mp != null) {
                            return mp;
                        }
                    }
                }
            }
        }
        return null;
    }

    private String modulePath(String from, String module, int level) {
        String dotted = module == null ? "" : module;
        if (level > 0) {
            String pkg = from.endsWith("/__init__.py") ? from.substring(0, from.length() - "/__init__.py".length())
                    : from.lastIndexOf('/') < 0 ? "" : from.substring(0, from.lastIndexOf('/'));
            for (int i = 1; i < level; i++) {
                pkg = pkg.lastIndexOf('/') < 0 ? "" : pkg.substring(0, pkg.lastIndexOf('/'));
            }
            String rest = dotted.replace('.', '/');
            dotted = pkg.isEmpty() ? rest : rest.isEmpty() ? pkg : pkg + "/" + rest;
        } else {
            dotted = dotted.replace('.', '/');
        }
        if ("enum".equals(dotted)) {
            return ENUM_PATH;
        }
        if (trees.containsKey(dotted + ".py")) {
            return dotted + ".py";
        }
        if (trees.containsKey(dotted + "/__init__.py")) {
            return dotted + "/__init__.py";
        }
        return null;
    }

    private void walk(String path, List<Stmt> body, boolean[] used, boolean top) {
        for (int i = 0; i < body.size(); i++) {
            Stmt s = body.get(i);
            if (s instanceof ClassDef) {
                ClassDef c = (ClassDef) s;
                walk(path, c.body, used, false);
                boolean isEnum = top ? enums.contains(path + ":" + c.name) : localEnum(path, c);
                if (isEnum && !(ENUM_PATH.equals(path) && ROOTS.contains(c.name))) {
                    List<Stmt> after = lower(path, c);
                    if (!after.isEmpty()) {
                        used[0] = true;
                        body.addAll(i + 1, after);
                        i += after.size();
                    }
                }
            } else if (s instanceof FunctionDef) {
                walk(path, ((FunctionDef) s).body, used, false);
            } else if (s instanceof If) {
                walk(path, ((If) s).body, used, false);
                walk(path, ((If) s).orelse, used, false);
            } else if (s instanceof Try) {
                walk(path, ((Try) s).body, used, false);
            }
        }
    }

    private boolean localEnum(String path, ClassDef c) {
        for (Expr b : c.bases) {
            String k = resolve(path, b, 0);
            if (k != null && enums.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private List<Stmt> lower(String path, ClassDef c) {
        if (MEMBERS.containsKey(c)) {
            return new ArrayList<Stmt>();
        }
        List<String> names = new ArrayList<String>();
        List<Expr> values = new ArrayList<Expr>();
        List<Boolean> isMember = new ArrayList<Boolean>();
        Set<String> bodyNames = new HashSet<String>();
        Set<String> memberSoFar = new HashSet<String>();
        List<Stmt> kept = new ArrayList<Stmt>();
        for (Stmt s : c.body) {
            String n = null;
            Expr v = null;
            if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name) {
                n = ((Name) ((Assign) s).targets.get(0)).id;
                v = ((Assign) s).value;
            } else if (s instanceof AnnAssign && ((AnnAssign) s).value != null
                    && ((AnnAssign) s).target instanceof Name) {
                n = ((Name) ((AnnAssign) s).target).id;
                v = ((AnnAssign) s).value;
            } else if (s instanceof Assign) {
                for (Expr t : ((Assign) s).targets) {
                    if (t instanceof Name && !excluded(c.name, ((Name) t).id)) {
                        throw new IllegalStateException(path + ": enum " + c.name
                                + " defines member " + ((Name) t).id + " in a statement the compiler cannot lower");
                    }
                }
            }
            if (s instanceof FunctionDef) {
                bodyNames.add(((FunctionDef) s).name);
            }
            if (n == null || excluded(c.name, n) || v instanceof Lambda) {
                if (n != null) {
                    bodyNames.add(n);
                }
                kept.add(s);
                continue;
            }
            boolean member = true;
            if (v instanceof Call && ((Call) v).func instanceof Name && ((Call) v).args.size() == 1) {
                String f = ((Name) ((Call) v).func).id;
                if ("nonmember".equals(f)) {
                    member = false;
                    v = ((Call) v).args.get(0);
                } else if ("member".equals(f)) {
                    v = ((Call) v).args.get(0);
                }
            }
            names.add(n);
            values.add(rebind(v, c.name, bodyNames, memberSoFar, path));
            isMember.add(member);
            bodyNames.add(n);
            if (member) {
                memberSoFar.add(n);
            }
        }
        List<Stmt> out = new ArrayList<Stmt>();
        if (names.isEmpty() && c.decorators.isEmpty()) {
            MEMBERS.put(c, new ArrayList<String>());
            return out;
        }
        c.body.clear();
        c.body.addAll(kept);
        if (c.body.isEmpty()) {
            c.body.add(new ExprStmt(new Constant(Constant.Kind.NONE, "None")));
        }
        List<String> mem = new ArrayList<String>();
        boolean self = ENUM_PATH.equals(path);
        for (int i = 0; i < names.size(); i++) {
            Expr v = values.get(i);
            if (isMember.get(i)) {
                Call m = new Call(self ? new Name("_member") : new Attribute(new Name(HELPER), "_member"));
                m.args.add(new Name(c.name));
                m.args.add(new Constant(Constant.Kind.STR, names.get(i)));
                m.args.add(v);
                v = m;
                mem.add(names.get(i));
            }
            List<Expr> t = new ArrayList<Expr>();
            t.add(new Attribute(new Name(c.name), names.get(i)));
            out.add(new Assign(t, v));
        }
        List<Expr> decos = new ArrayList<Expr>(c.decorators);
        c.decorators.clear();
        for (int i = decos.size() - 1; i >= 0; i--) {
            Call d = new Call(decos.get(i));
            d.args.add(new Name(c.name));
            List<Expr> t = new ArrayList<Expr>();
            t.add(new Name(c.name));
            out.add(new Assign(t, d));
        }
        MEMBERS.put(c, mem);
        return out;
    }

    /// Dunders, sunders and private names are never members.
    private static boolean excluded(String cls, String n) {
        boolean dunder = n.length() > 4 && n.startsWith("__") && n.endsWith("__");
        boolean sunder = n.length() > 2 && n.charAt(0) == '_' && n.charAt(n.length() - 1) == '_'
                && n.charAt(1) != '_' && n.charAt(n.length() - 2) != '_';
        boolean priv = n.startsWith("_" + cls + "__");
        return dunder || sunder || priv;
    }

    /// `value` as it reads outside the class body: a name the body bound
    /// earlier is that attribute of the class (a member's raw value).
    private static Expr rebind(Expr v, String cls, Set<String> body, Set<String> members, String path) {
        if (v instanceof Name) {
            String n = ((Name) v).id;
            if (members.contains(n)) {
                return new Attribute(new Attribute(new Name(cls), n), "_value_");
            }
            return body.contains(n) ? new Attribute(new Name(cls), n) : v;
        }
        if (v instanceof BinOp) {
            BinOp b = (BinOp) v;
            return new BinOp(rebind(b.left, cls, body, members, path), b.op, rebind(b.right, cls, body, members, path));
        }
        if (v instanceof UnaryOp) {
            return new UnaryOp(((UnaryOp) v).op, rebind(((UnaryOp) v).operand, cls, body, members, path));
        }
        if (v instanceof TupleExpr || v instanceof ListExpr) {
            List<Expr> src = v instanceof TupleExpr ? ((TupleExpr) v).elts : ((ListExpr) v).elts;
            List<Expr> e = new ArrayList<Expr>();
            for (Expr x : src) {
                e.add(rebind(x, cls, body, members, path));
            }
            return v instanceof TupleExpr ? new TupleExpr(e) : new ListExpr(e);
        }
        if (v instanceof Call) {
            Call c = (Call) v;
            Call r = new Call(rebind(c.func, cls, body, members, path));
            for (Expr x : c.args) {
                r.args.add(rebind(x, cls, body, members, path));
            }
            r.keywords.addAll(c.keywords);
            return r;
        }
        if (mentions(v, body)) {
            throw new IllegalStateException(path + ": enum " + cls
                    + " computes a member from a name of its own body in an expression the compiler cannot lower");
        }
        return v;
    }

    private static boolean mentions(Object o, Set<String> names) {
        if (o instanceof Name) {
            return names.contains(((Name) o).id);
        }
        if (o instanceof Attribute) {
            return mentions(((Attribute) o).value, names);
        }
        return false;
    }
}
