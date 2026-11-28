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
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.Keyword;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Lowers a constructor that stores its keyword arguments as attributes,
///
/// ```python
/// class Descriptor:
///     def __init__(self, name=None, **kw):
///         self.name = name
///         for k, v in kw.items():
///             setattr(self, k, v)
/// ```
///
/// to one literal store per keyword the closed program can pass:
///
/// ```python
///         if "expected_type" in kw:
///             self.expected_type = kw["expected_type"]
///         if "allow_none" in kw:
///             self.allow_none = kw["allow_none"]
/// ```
///
/// The keys that can reach `kw` are the literal keywords of every call of
/// the class or of a subclass (openpyxl's `Typed(expected_type=X,
/// allow_none=True)` in class bodies), of every `super().__init__` and
/// `X.__init__` in the family, and the literal keys a subclass's own
/// `__init__` writes into its `**kw` before forwarding it (`Set` replaces
/// `kw['values']`). A key nobody passes simply finds its test false, so the
/// union over all sites is exact for every one of them.
///
/// It refuses anything it cannot see: a family class name used other than
/// to call it, subclass it or test `isinstance` against it (an escaped class
/// object can be called with any keywords); a `**mapping` that is not the
/// enclosing family `__init__`'s own `**kw`; a family `__init__` using its
/// `**kw` other than forwarding it or reading and writing literal keys; a
/// `cls(...)`, `type(...)(...)` or `__class__` call in the family; a family
/// class that defines `__setattr__`, or binds one of the keys to a method or
/// to a family instance (a store there could run code, and the fixed order
/// of the tests would then be observable where the order of `kw` was).
public final class KwAttrInit {

    private KwAttrInit() {
    }

    /// Lowers every qualifying constructor among `trees`; `libTrees` are the
    /// bundled modules, read only so their uses count against the rewrite.
    public static void rewrite(Map<String, Node.Module> trees, Map<String, Node.Module> libTrees) {
        List<Node.Module> all = new ArrayList<Node.Module>(trees.values());
        all.addAll(libTrees.values());
        Map<String, Node.Module> keyed = new HashMap<String, Node.Module>(libTrees);
        keyed.putAll(trees);
        List<ClassDef> classes = new ArrayList<ClassDef>();
        for (Node.Module m : all) {
            collect(m, classes);
        }
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            List<ClassDef> mine = new ArrayList<ClassDef>();
            collect(e.getValue(), mine);
            for (ClassDef root : mine) {
                FunctionDef init = method(root, "__init__");
                if (init == null || init.args.kwarg == null) {
                    continue;
                }
                Node.For loop = loop(init);
                if (loop == null) {
                    continue;
                }
                lower(e.getKey(), root, init, loop, classes, all, keyed);
            }
        }
    }

    private static void collect(Object o, final List<ClassDef> out) {
        if (o instanceof ClassDef) {
            out.add((ClassDef) o);
        }
        Infer.children(o, x -> collect(x, out));
    }

    private static FunctionDef method(ClassDef c, String name) {
        for (Stmt s : c.body) {
            if (s instanceof FunctionDef && ((FunctionDef) s).name.equals(name)) {
                return (FunctionDef) s;
            }
        }
        return null;
    }

    /// The `for k, v in kw.items(): setattr(self, k, v)` statement of `init`,
    /// with `kw` used nowhere else in it, or null.
    private static Node.For loop(FunctionDef init) {
        if (init.args.positional.isEmpty()) {
            return null;
        }
        String kw = init.args.kwarg.name;
        String self = init.args.positional.get(0).name;
        Node.For found = null;
        for (Stmt s : init.body) {
            if (!(s instanceof Node.For)) {
                continue;
            }
            Node.For f = (Node.For) s;
            if (f.isAsync || !f.orelse.isEmpty() || f.body.size() != 1
                    || !(f.target instanceof Node.TupleExpr) || !(f.iter instanceof Call)) {
                continue;
            }
            List<Expr> t = ((Node.TupleExpr) f.target).elts;
            Call it = (Call) f.iter;
            if (t.size() != 2 || !(t.get(0) instanceof Name) || !(t.get(1) instanceof Name)
                    || !it.args.isEmpty() || !it.keywords.isEmpty() || !(it.func instanceof Attribute)
                    || !"items".equals(((Attribute) it.func).attr)
                    || !isName(((Attribute) it.func).value, kw)) {
                continue;
            }
            Stmt b = f.body.get(0);
            if (!(b instanceof Node.ExprStmt) || !(((Node.ExprStmt) b).value instanceof Call)) {
                continue;
            }
            Call sa = (Call) ((Node.ExprStmt) b).value;
            if (isName(sa.func, "setattr") && sa.args.size() == 3 && sa.keywords.isEmpty()
                    && isName(sa.args.get(0), self) && isName(sa.args.get(1), ((Name) t.get(0)).id)
                    && isName(sa.args.get(2), ((Name) t.get(1)).id)) {
                found = f;
            }
        }
        if (found == null || uses(init.body, kw) != 1 || uses(init.body, "setattr") != 1) {
            return null;
        }
        return found;
    }

    private static boolean isName(Object e, String id) {
        return e instanceof Name && ((Name) e).id.equals(id);
    }

    private static int uses(Object o, final String id) {
        final int[] n = {0};
        countUses(o, id, n);
        return n[0];
    }

    private static void countUses(Object o, final String id, final int[] n) {
        if (isName(o, id)) {
            n[0]++;
        }
        Infer.children(o, x -> countUses(x, id, n));
    }

    private static String nameOf(Expr e) {
        if (e instanceof Name) {
            return ((Name) e).id;
        }
        if (e instanceof Attribute) {
            return ((Attribute) e).attr;
        }
        return null;
    }

    /// Context of one family check.
    private static final class Ctx {
        final Set<String> family = new LinkedHashSet<String>();
        final Set<String> keys = new LinkedHashSet<String>();
        final IdentityHashMap<Object, Boolean> allowed = new IdentityHashMap<Object, Boolean>();
        final Set<String> famIds = new HashSet<String>();
        final IdentityHashMap<ClassDef, Boolean> famDefs = new IdentityHashMap<ClassDef, Boolean>();
        Resolver res;
        String mod;
        /// Attributes a family class object is stored in.
        final Set<String> escAttrs = new LinkedHashSet<String>();
        final Set<String> members = new HashSet<String>();
        final IdentityHashMap<Object, Boolean> escClasses = new IdentityHashMap<Object, Boolean>();
        final Set<String> mixAncestors = new HashSet<String>();
        final IdentityHashMap<Object, String> shadowed = new IdentityHashMap<Object, String>();
        final Set<String> classNames = new HashSet<String>();
        ClassDef readCls;
        String refusal;

        List<ClassDef> famDefsList() {
            return new ArrayList<ClassDef>(famDefs.keySet());
        }
    }

    private static String modName(String key) {
        String k = key.replace('\\', '/');
        if (k.endsWith(".py")) {
            k = k.substring(0, k.length() - 3);
        }
        if (k.endsWith("/__init__")) {
            k = k.substring(0, k.length() - 9);
        }
        return k.replace('/', '.');
    }

    /// Resolves the global class a module-level name binds, `mod:Name`, by
    /// following `from ... import` chains; null when it is not a class the
    /// program defines at module level.
    static final class Resolver {
        final Map<String, Node.Module> mods = new HashMap<String, Node.Module>();
        final Set<String> pkgs = new HashSet<String>();

        Resolver(Map<String, Node.Module> trees) {
            for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
                String m = modName(e.getKey());
                mods.put(m, e.getValue());
                if (e.getKey().replace('\\', '/').endsWith("__init__.py")) {
                    pkgs.add(m);
                }
            }
        }

        String target(String mod, Node.ImportFrom f) {
            if (f.level == 0) {
                return f.module;
            }
            String base = pkgs.contains(mod) ? mod : (mod.lastIndexOf('.') < 0 ? "" : mod.substring(0, mod.lastIndexOf('.')));
            for (int i = 1; i < f.level; i++) {
                base = base.lastIndexOf('.') < 0 ? "" : base.substring(0, base.lastIndexOf('.'));
            }
            return f.module == null ? base : (base.isEmpty() ? f.module : base + "." + f.module);
        }

        String resolve(String mod, String name, int depth) {
            Node.Module m = mods.get(mod);
            if (m == null || depth > 12) {
                return null;
            }
            for (Stmt s : m.body) {
                if (s instanceof ClassDef && ((ClassDef) s).name.equals(name)) {
                    return mod + ":" + name;
                }
            }
            for (Stmt s : m.body) {
                // `Coordinate = Integer`: a module-level alias of a class.
                if (alias(s, name) != null) {
                    return resolve(mod, alias(s, name).id, depth + 1);
                }
            }
            List<Node.ImportFrom> froms = new ArrayList<Node.ImportFrom>();
            gather(m, froms);
            for (Node.ImportFrom f : froms) {
                for (Node.Alias a : f.names) {
                    if (!"*".equals(a.name) && name.equals(a.asname != null ? a.asname : a.name)) {
                        String r = resolve(target(mod, f), a.name, depth + 1);
                        if (r != null) {
                            return r;
                        }
                    }
                }
            }
            for (Node.ImportFrom f : froms) {
                if (f.names.size() == 1 && "*".equals(f.names.get(0).name)) {
                    String r = resolve(target(mod, f), name, depth + 1);
                    if (r != null) {
                        return r;
                    }
                }
            }
            return null;
        }

        /// The module `e` names in `mod` (`import a.b as c`, `from a import b`,
        /// `a.b`), or null.
        String moduleOf(String mod, Expr e) {
            if (e instanceof Attribute) {
                String m = moduleOf(mod, ((Attribute) e).value);
                String r = m == null ? null : m + "." + ((Attribute) e).attr;
                return r != null && mods.containsKey(r) ? r : null;
            }
            if (!(e instanceof Name) || mods.get(mod) == null) {
                return null;
            }
            String id = ((Name) e).id;
            List<Node.ImportFrom> froms = new ArrayList<Node.ImportFrom>();
            List<Node.Import> imps = new ArrayList<Node.Import>();
            gather(mods.get(mod), froms);
            gatherImports(mods.get(mod), imps);
            for (Node.ImportFrom f : froms) {
                for (Node.Alias a : f.names) {
                    if (id.equals(a.asname != null ? a.asname : a.name)) {
                        String r = target(mod, f) + "." + a.name;
                        if (mods.containsKey(r)) {
                            return r;
                        }
                    }
                }
            }
            for (Node.Import i : imps) {
                for (Node.Alias a : i.names) {
                    if (a.asname != null ? id.equals(a.asname) : id.equals(a.name.split("\\.")[0])) {
                        String r = a.asname != null ? a.name : id;
                        if (mods.containsKey(r)) {
                            return r;
                        }
                    }
                }
            }
            return null;
        }

        private static void gatherImports(Object o, final List<Node.Import> out) {
            if (o instanceof Node.Import) {
                out.add((Node.Import) o);
            }
            Infer.children(o, x -> gatherImports(x, out));
        }

        /// The class name `s` binds `name` to, when it is `name = Other`.
        static Name alias(Stmt s, String name) {
            if (s instanceof Node.Assign && ((Node.Assign) s).targets.size() == 1
                    && isName(((Node.Assign) s).targets.get(0), name)
                    && ((Node.Assign) s).value instanceof Name
                    && !((Name) ((Node.Assign) s).value).id.equals(name)) {
                return (Name) ((Node.Assign) s).value;
            }
            return null;
        }

        private static void gather(Object o, final List<Node.ImportFrom> out) {
            if (o instanceof Node.ImportFrom) {
                out.add((Node.ImportFrom) o);
            }
            Infer.children(o, x -> gather(x, out));
        }
    }

    private static boolean isFamily(Ctx c, Expr e) {
        if (e instanceof Name) {
            return c.famIds.contains(c.res.resolve(c.mod, ((Name) e).id, 0));
        }
        if (e instanceof Attribute) {
            // `base.Typed`: only a module's attribute can name a class
            // without a Name having escaped first.
            String m = c.res.moduleOf(c.mod, ((Attribute) e).value);
            return m != null && c.famIds.contains(c.res.resolve(m, ((Attribute) e).attr, 0));
        }
        return false;
    }

    private static void lower(String path, ClassDef root, FunctionDef init, Node.For loop,
            List<ClassDef> classes, List<Node.Module> all, Map<String, Node.Module> trees) {
        Ctx c = new Ctx();
        c.res = new Resolver(trees);
        c.famIds.add(modName(path) + ":" + root.name);
        c.family.add(root.name);
        c.famDefs.put(root, Boolean.TRUE);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
                c.mod = modName(e.getKey());
                for (Stmt s : e.getValue().body) {
                    if (!(s instanceof ClassDef) || c.famDefs.containsKey(s)) {
                        continue;
                    }
                    ClassDef k = (ClassDef) s;
                    for (Expr b : k.bases) {
                        if (isFamily(c, b)) {
                            c.famDefs.put(k, Boolean.TRUE);
                            c.famIds.add(c.mod + ":" + k.name);
                            c.family.add(k.name);
                            grew = true;
                            break;
                        }
                    }
                }
            }
        }
        for (ClassDef k : classes) {
            if (!c.famDefs.containsKey(k)) {
                for (Expr b : k.bases) {
                    if (!isTop(trees, k) && (b instanceof Name && c.family.contains(((Name) b).id)
                            || b instanceof Attribute && c.family.contains(((Attribute) b).attr))) {
                        // A base this pass cannot resolve to a module-level
                        // class might be one of the family.
                        c.refusal = k.name + " has an unresolved family base";
                    }
                }
            }
        }
        for (ClassDef k : c.famDefsList()) {
            for (Expr b : k.bases) {
                c.allowed.put(b, Boolean.TRUE);
            }
        }
        for (ClassDef k : classes) {
            for (Expr b : k.bases) {
                if (nameOf(b) != null) {
                    c.classNames.add(nameOf(b));
                }
            }
        }
        String top = path.replace('\\', '/').split("[/.]")[0];
        for (Node.Module m : all) {
            for (Stmt s : m.body) {
                if (s instanceof Node.Assign && ((Node.Assign) s).targets.size() == 1
                        && ((Node.Assign) s).targets.get(0) instanceof Name
                        && Resolver.alias(s, ((Name) ((Node.Assign) s).targets.get(0)).id) != null
                        && once(m, ((Name) ((Node.Assign) s).targets.get(0)).id)) {
                    c.allowed.put(((Node.Assign) s).value, Boolean.TRUE);
                    c.allowed.put(((Node.Assign) s).targets.get(0), Boolean.TRUE);
                    c.family.add(((Name) ((Node.Assign) s).targets.get(0)).id);
                }
            }
        }
        for (Node.Module m : all) {
            if (relevant(m, top)) {
                c.mod = null;
                for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
                    if (e.getValue() == m) { //NOPMD CompareObjectsWithEquals - identity is meant
                        c.mod = modName(e.getKey());
                    }
                }
                if (c.mod == null) {
                    c.refusal = "a bundled module imports " + top;
                }
            } else {
                c.mod = null;
                for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
                    if (e.getValue() == m && modName(e.getKey()).startsWith(top + ".")) { //NOPMD CompareObjectsWithEquals - identity is meant
                        c.mod = modName(e.getKey());
                    }
                }
                if (c.mod == null) {
                    // Neither in the root's package nor importing from it:
                    // its `Sequence` is another one.
                    continue;
                }
            }
            walk(m, c, null, null, loop);
        }
        // Every class an instance holding a stored class could also be: the
        // ancestors of the family and of the classes storing one.
        List<ClassDef> holders = c.famDefsList();
        for (Object k : c.escClasses.keySet()) {
            holders.add((ClassDef) k);
        }
        for (ClassDef k : holders) {
            for (Expr b : k.bases) {
                c.mixAncestors.add(nameOf(b));
            }
        }
        boolean more = true;
        while (more) {
            more = false;
            for (ClassDef k : classes) {
                if (c.mixAncestors.contains(k.name)) {
                    for (Expr b : k.bases) {
                        more |= nameOf(b) != null && c.mixAncestors.add(nameOf(b));
                    }
                }
            }
        }
        for (Object k : c.shadowed.keySet()) {
            if (c.refusal == null && (c.classNames.contains(((ClassDef) k).name)
                    || nameEscapes(all, ((ClassDef) k).name, c))) {
                c.escAttrs.add(c.shadowed.get(k));
            }
        }
        if (c.refusal == null && !c.escAttrs.isEmpty()) {
            for (ClassDef k : c.famDefsList()) {
                for (Stmt st : k.body) {
                    if (st instanceof FunctionDef) {
                        c.members.add(((FunctionDef) st).name);
                    } else if (st instanceof Node.Assign) {
                        for (Expr t : ((Node.Assign) st).targets) {
                            if (t instanceof Name) {
                                c.members.add(((Name) t).id);
                            }
                        }
                    }
                }
            }
            for (String a : c.escAttrs) {
                if (c.keys.contains(a) && kwRead(all, a)) {
                    c.refusal = "a family __init__ reads " + a + " from its **kw";
                }
            }
            for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
                c.mod = modName(e.getKey());
                reads(e.getValue(), c, null, new IdentityHashMap<Object, Boolean>());
            }
        }
        if (c.refusal == null) {
            for (ClassDef k : c.famDefsList()) {
                for (Stmt s : k.body) {
                    // A plain def is not a data descriptor: `self.to_tree = f`
                    // only fills the instance dictionary. A decorated one
                    // (a property) would run code on the store.
                    if (s instanceof FunctionDef && ("__setattr__".equals(((FunctionDef) s).name)
                            || c.keys.contains(((FunctionDef) s).name)
                            && !((FunctionDef) s).decorators.isEmpty())) {
                        c.refusal = k.name + " binds " + ((FunctionDef) s).name;
                    } else if (s instanceof Node.AnnAssign && ((Node.AnnAssign) s).value != null
                            && ((Node.AnnAssign) s).target instanceof Name
                            && c.keys.contains(((Name) ((Node.AnnAssign) s).target).id)
                            && !inert(((Node.AnnAssign) s).value, all, trees)) {
                        c.refusal = k.name + " binds " + ((Name) ((Node.AnnAssign) s).target).id
                                + " to a possible descriptor";
                    } else if (s instanceof Node.Assign && !inert(((Node.Assign) s).value, all, trees)) {
                        // Anything but a constant may be a descriptor (property(...), a
                        // family or foreign descriptor instance) whose store runs code.
                        for (Expr t : ((Node.Assign) s).targets) {
                            if (t instanceof Name && c.keys.contains(((Name) t).id)) {
                                c.refusal = k.name + " binds " + ((Name) t).id + " to a descriptor";
                            }
                        }
                    }
                }
            }
        }
        if (c.refusal != null) {
            return;
        }
        String kw = init.args.kwarg.name;
        String self = init.args.positional.get(0).name;
        StringBuilder b = new StringBuilder("def _cn1_kw(" + self + ", **" + kw + "):\n");
        for (String k : c.keys) {
            if (!ConstArgSpecialize.ident(k)) {
                return;
            }
            b.append("    if \"").append(k).append("\" in ").append(kw).append(":\n        ")
                    .append(self).append('.').append(k).append(" = ").append(kw)
                    .append("[\"").append(k).append("\"]\n");
        }
        if (c.keys.isEmpty()) {
            b.append("    pass\n");
        }
        FunctionDef f = ConstArgSpecialize.parseMethod(b.toString(), loop.line, path);
        int at = init.body.indexOf(loop);
        init.body.remove(at);
        init.body.addAll(at, f.body);
    }

    /// True when some subscript or `get`/`pop` call reads the literal key `a`.
    private static boolean kwRead(List<Node.Module> all, final String a) {
        final boolean[] r = {false};
        for (Node.Module m : all) {
            keyRead(m, a, r);
        }
        return r[0];
    }

    private static void keyRead(Object o, final String a, final boolean[] r) {
        if (o instanceof Node.Subscript && ((Node.Subscript) o).index instanceof Constant
                && a.equals(((Constant) ((Node.Subscript) o).index).text)
                || o instanceof Call && ((Call) o).func instanceof Attribute && !((Call) o).args.isEmpty()
                && ((Call) o).args.get(0) instanceof Constant
                && a.equals(((Constant) ((Call) o).args.get(0)).text)) {
            r[0] = true;
        }
        Infer.children(o, x -> keyRead(x, a, r));
    }

    /// Whether `e` reads a stored family class: `x.<escaped attribute>`, or
    /// the parameter `param` a checked function received one in.
    private static boolean holds(Ctx c, Object e, String param) {
        if (e instanceof Attribute && isName(((Attribute) e).value, "self") && c.readCls != null
                && !c.famDefs.containsKey(c.readCls) && !c.escClasses.containsKey(c.readCls)
                && !c.mixAncestors.contains(c.readCls.name) && !c.classNames.contains(c.readCls.name)) {
            // `self` of a class no holder can be an instance of.
            return false;
        }
        return e instanceof Attribute && c.escAttrs.contains(((Attribute) e).attr)
                || param != null && isName(e, param);
    }

    private static final Set<String> INSPECT = new HashSet<String>(Arrays.asList(
            "isinstance", "issubclass", "hasattr", "str", "repr", "id", "callable", "print"));

    /// Checks every read of a stored family class in `o`. A read may test
    /// it, format it, iterate it, read a member no family class defines,
    /// call it with positional arguments only, or hand it positionally to a
    /// module-level function whose parameter obeys the same rules; anything
    /// else could reach a constructor with keywords no call site shows.
    private static void reads(Object o, final Ctx c, final String param, final IdentityHashMap<Object, Boolean> ok) {
        if (c.refusal != null) {
            return;
        }
        if (o instanceof Node.Assign) {
            for (Expr t : ((Node.Assign) o).targets) {
                ok.put(t, Boolean.TRUE);
            }
        } else if (o instanceof Node.Delete) {
            for (Expr t : ((Node.Delete) o).targets) {
                ok.put(t, Boolean.TRUE);
            }
        } else if (o instanceof Node.AugAssign) {
            ok.put(((Node.AugAssign) o).target, Boolean.TRUE);
        } else if (o instanceof Node.FormattedValue) {
            ok.put(((Node.FormattedValue) o).value, Boolean.TRUE);
        } else if (o instanceof Node.For) {
            ok.put(((Node.For) o).iter, Boolean.TRUE);
        } else if (o instanceof Node.Comprehension) {
            ok.put(((Node.Comprehension) o).iter, Boolean.TRUE);
        } else if (o instanceof Node.If) {
            ok.put(((Node.If) o).test, Boolean.TRUE);
        } else if (o instanceof Node.Compare) {
            ok.put(((Node.Compare) o).left, Boolean.TRUE);
            for (Expr x : ((Node.Compare) o).comparators) {
                ok.put(x, Boolean.TRUE);
            }
        } else if (o instanceof Attribute && holds(c, ((Attribute) o).value, param)) {
            String a = ((Attribute) o).attr;
            // A member is a function or a value: calling a method unbound
            // constructs nothing (a family body never calls `cls(...)`), so
            // only `__init__`, `__call__` and the other dunders are refused.
            if ((!a.startsWith("__") || "__name__".equals(a) || "__doc__".equals(a))
                    && !c.shadowed.containsValue(a)) {
                ok.put(((Attribute) o).value, Boolean.TRUE);
            }
        } else if (o instanceof Call) {
            Call call = (Call) o;
            String n = call.func instanceof Name ? ((Name) call.func).id : null;
            if (holds(c, call.func, param) && call.keywords.isEmpty()) {
                ok.put(call.func, Boolean.TRUE);
            }
            for (int i = 0; i < call.args.size(); i++) {
                Expr x = call.args.get(i);
                if (!holds(c, x, param)) {
                    continue;
                }
                if (n != null && INSPECT.contains(n) && c.res.resolve(c.mod, n, 0) == null) {
                    ok.put(x, Boolean.TRUE);
                } else if (n != null && param == null) {
                    FunctionDef f = topFunction(c, n);
                    if (f != null && i < f.args.positional.size() && f.args.positional.size() >= call.args.size()
                            && f.decorators.isEmpty()) {
                        String p = f.args.positional.get(i).name;
                        if (bindCount(f.body, p) == 0) {
                            ok.put(x, Boolean.TRUE);
                            reads(f.body, c, p, new IdentityHashMap<Object, Boolean>());
                        }
                    }
                }
            }
        } else if (o instanceof ClassDef) {
            ClassDef was = c.readCls;
            c.readCls = (ClassDef) o;
            Infer.children(o, x -> reads(x, c, param, ok));
            c.readCls = was;
            return;
        } else if (o instanceof Attribute && c.readCls != null && isName(((Attribute) o).value, "self")
                && ((Attribute) o).attr.equals(c.shadowed.get(c.readCls))) {
            return;
        }
        if (holds(c, o, param) && !ok.containsKey(o)) {
            c.refusal = "a stored family class is read at " + c.mod + ":" + ((Node) o).line + " "
                    + ConstArgSpecialize.dump(o);
            return;
        }
        Infer.children(o, x -> reads(x, c, param, ok));
    }

    /// The module-level function `name` binds in the current module, or null.
    private static FunctionDef topFunction(Ctx c, String name) {
        Node.Module m = c.res.mods.get(c.mod);
        if (m == null) {
            return null;
        }
        for (Stmt s : m.body) {
            if (s instanceof FunctionDef && ((FunctionDef) s).name.equals(name)) {
                return bindCount(m, name) == 2 ? (FunctionDef) s : null;
            }
        }
        // Imported: resolve through `from x import name`.
        List<Node.ImportFrom> froms = new ArrayList<Node.ImportFrom>();
        Resolver.gather(m, froms);
        for (Node.ImportFrom f : froms) {
            for (Node.Alias a : f.names) {
                if (name.equals(a.asname != null ? a.asname : a.name)) {
                    Node.Module t = c.res.mods.get(c.res.target(c.mod, f));
                    if (t != null) {
                        for (Stmt s : t.body) {
                            if (s instanceof FunctionDef && ((FunctionDef) s).name.equals(a.name)) {
                                return (FunctionDef) s;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /// True when `name` is bound exactly once anywhere in `m`.
    private static boolean once(Node m, String name) {
        return bindCount(m, name) == 1;
    }

    /// Assignments of `name` in `o`, a def or class of it counting two.
    private static int bindCount(Object o, String name) {
        final int[] n = {0};
        binds(o, name, n);
        return n[0];
    }

    private static void binds(Object o, final String name, final int[] n) {
        if (o instanceof Node.Assign) {
            for (Expr t : ((Node.Assign) o).targets) {
                countUses(t, name, n);
            }
        } else if (o instanceof Node.AugAssign) {
            countUses(((Node.AugAssign) o).target, name, n);
        } else if (o instanceof Node.AnnAssign) {
            countUses(((Node.AnnAssign) o).target, name, n);
        } else if (o instanceof Node.For) {
            countUses(((Node.For) o).target, name, n);
        } else if (o instanceof FunctionDef && ((FunctionDef) o).name.equals(name)
                || o instanceof ClassDef && ((ClassDef) o).name.equals(name)) {
            n[0] += 2;
        } else if (o instanceof Node.Global) {
            n[0] += 2;
        }
        Infer.children(o, x -> binds(x, name, n));
    }

    /// True when a Name `name` is used anywhere other than to call it or
    /// test `isinstance` against it (by simple name: a superset).
    private static boolean nameEscapes(List<Node.Module> all, String name, Ctx c) {
        final IdentityHashMap<Object, Boolean> ok = new IdentityHashMap<Object, Boolean>();
        final boolean[] r = {false};
        for (Node.Module m : all) {
            escapes(m, name, ok, r, c);
        }
        return r[0];
    }

    private static void escapes(Object o, final String name, final IdentityHashMap<Object, Boolean> ok,
            final boolean[] r, final Ctx c) {
        if (o instanceof Call) {
            Call call = (Call) o;
            ok.put(call.func, Boolean.TRUE);
            if ((isName(call.func, "_dm_meta_serialisable") || isName(call.func, "_dm_meta_strict"))
                    && call.args.size() == 1 && isName(call.args.get(0), name)) {
                // DeclarativeMap's metaclass functions (META_MODULE): they
                // read the class's dictionary, and a value that is not a
                // Descriptor instance reaches nothing but isinstance there.
                ok.put(call.args.get(0), Boolean.TRUE);
            }
            if (c.family.contains(nameOf(call.func))) {
                // `Sequence(expected_type=IgnoredError)`: stored in the
                // descriptor's attribute, whose reads are then checked and
                // may not read the shadowed attribute off it.
                for (Keyword k : call.keywords) {
                    if (k.arg != null && isName(k.value, name)) {
                        ok.put(k.value, Boolean.TRUE);
                        c.escAttrs.add(k.arg);
                    }
                }
            }
            if ((isName(call.func, "isinstance") || isName(call.func, "issubclass")) && call.args.size() == 2) {
                ok.put(call.args.get(1), Boolean.TRUE);
                if (call.args.get(1) instanceof Node.TupleExpr) {
                    for (Expr x : ((Node.TupleExpr) call.args.get(1)).elts) {
                        ok.put(x, Boolean.TRUE);
                    }
                }
            }
        } else if (o instanceof Node.Assign && isAll((Node.Assign) o)) {
            return;
        } else if ((isName(o, name) || o instanceof Attribute && ((Attribute) o).attr.equals(name))
                && !ok.containsKey(o)) {
            r[0] = true;
        }
        Infer.children(o, x -> escapes(x, name, ok, r, c));
    }

    /// True when `k`'s `__init__` stores `self.<attr>` as a statement of its
    /// own body, before any return.
    private static boolean initStores(ClassDef k, String attr) {
        FunctionDef init = method(k, "__init__");
        if (init == null || init.args.positional.isEmpty()) {
            return false;
        }
        String self = init.args.positional.get(0).name;
        for (Stmt s : init.body) {
            if (s instanceof Node.Return || s instanceof Node.Raise) {
                return false;
            }
            if (s instanceof Node.Assign && ((Node.Assign) s).targets.size() == 1
                    && ((Node.Assign) s).targets.get(0) instanceof Attribute
                    && attr.equals(((Attribute) ((Node.Assign) s).targets.get(0)).attr)
                    && isName(((Attribute) ((Node.Assign) s).targets.get(0)).value, self)) {
                return true;
            }
        }
        return false;
    }

    private static final Set<String> BUILTIN_TYPES = new HashSet<String>(Arrays.asList(
            "bool", "int", "float", "complex", "str", "bytes", "bytearray", "list", "dict", "tuple",
            "set", "frozenset", "object", "type"));

    /// Whether a class-body value cannot be a descriptor: a constant, an
    /// operator over such values, `type(...)` of one, a builtin type, or a
    /// name every module-level binding of which is itself inert. A name found
    /// bound nowhere (an import, a parameter) may be anything, so it is not.
    private static boolean inert(Expr v, List<Node.Module> all, Map<String, Node.Module> trees) {
        return inert(v, all, trees.keySet(), new HashSet<String>());
    }

    private static boolean inert(Expr v, List<Node.Module> all, Set<String> paths, Set<String> seen) {
        if (v == null || v instanceof Constant) {
            return v != null;
        }
        if (v instanceof Node.UnaryOp) {
            return inert(((Node.UnaryOp) v).operand, all, paths, seen);
        }
        if (v instanceof Node.BinOp) {
            return inert(((Node.BinOp) v).left, all, paths, seen) && inert(((Node.BinOp) v).right, all, paths, seen);
        }
        if (v instanceof Call && ((Call) v).func instanceof Name && "type".equals(((Name) ((Call) v).func).id)
                && ((Call) v).args.size() == 1 && ((Call) v).keywords.isEmpty()) {
            return inert(((Call) v).args.get(0), all, paths, seen);
        }
        if (!(v instanceof Name)) {
            return false;
        }
        String id = ((Name) v).id;
        if (!seen.add(id)) {
            return false;
        }
        boolean bound = false;
        for (Node.Module m : all) {
            for (Stmt t : m.body) {
                if (!(t instanceof Node.Assign) && !(t instanceof Node.AnnAssign) && bindsElsewhere(t, id, paths)) {
                    seen.remove(id);
                    return false;
                }
                if (t instanceof Node.Assign) {
                    for (Expr x : ((Node.Assign) t).targets) {
                        if (x instanceof Name && id.equals(((Name) x).id)) {
                            if (!inert(((Node.Assign) t).value, all, paths, seen)) {
                                seen.remove(id);
                                return false;
                            }
                            bound = true;
                        }
                    }
                } else if (t instanceof Node.AnnAssign && ((Node.AnnAssign) t).target instanceof Name
                        && id.equals(((Name) ((Node.AnnAssign) t).target).id)) {
                    if (!inert(((Node.AnnAssign) t).value, all, paths, seen)) {
                        seen.remove(id);
                        return false;
                    }
                    bound = true;
                }
            }
        }
        seen.remove(id);
        return bound || BUILTIN_TYPES.contains(id);
    }

    /// Whether `o` binds `id` in any way but the top-level assignment `inert`
    /// reads: an import, a def or class, a nested assignment, a loop, `with`,
    /// walrus or `except` target. Each may hold anything, a descriptor too.
    private static boolean bindsElsewhere(Object o, String id, Set<String> paths) {
        if (o instanceof Node.ImportFrom && ((Node.ImportFrom) o).level == 0
                && ((Node.ImportFrom) o).module != null && inProgram(((Node.ImportFrom) o).module, paths)) {
            // `from m import id` of a program module: m's own binding is among
            // those `inert` reads. A rename still counts as a binding.
            for (Node.Alias al : ((Node.ImportFrom) o).names) {
                if ("*".equals(al.name) || al.asname != null && id.equals(al.asname)) {
                    return true;
                }
            }
            return false;
        }
        if (o instanceof Node.Alias) {
            Node.Alias al = (Node.Alias) o;
            String n = al.asname != null ? al.asname : al.name.indexOf('.') >= 0
                    ? al.name.substring(0, al.name.indexOf('.')) : al.name;
            if (id.equals(n) || "*".equals(al.name)) {
                return true;
            }
        } else if (o instanceof FunctionDef && id.equals(((FunctionDef) o).name)
                || o instanceof ClassDef && id.equals(((ClassDef) o).name)
                || o instanceof Node.ExceptHandler && id.equals(((Node.ExceptHandler) o).name)
                || o instanceof Node.NamedExpr && id.equals(((Node.NamedExpr) o).target.id)) {
            return true;
        } else if (o instanceof Node.Assign) {
            for (Expr x : ((Node.Assign) o).targets) {
                if (mentions(x, id)) {
                    return true;
                }
            }
        } else if (o instanceof Node.AnnAssign && mentions(((Node.AnnAssign) o).target, id)
                || o instanceof Node.AugAssign && mentions(((Node.AugAssign) o).target, id)
                || o instanceof Node.For && mentions(((Node.For) o).target, id)
                || o instanceof Node.WithItem && ((Node.WithItem) o).vars != null
                        && mentions(((Node.WithItem) o).vars, id)) {
            return true;
        }
        final boolean[] hit = {false};
        Infer.children(o, x -> hit[0] |= !hit[0] && bindsElsewhere(x, id, paths));
        return hit[0];
    }

    private static boolean inProgram(String module, Set<String> paths) {
        String base = module.replace('.', '/');
        for (String p : paths) {
            String q = p.replace('\\', '/');
            // Exact: a suffix match would take an outside `config` for a
            // program's `pkg/config.py`.
            if (q.equals(base + ".py") || q.equals(base + "/__init__.py")) {
                return true;
            }
        }
        return false;
    }

    private static boolean mentions(Object o, String id) {
        if (o instanceof Name) {
            return id.equals(((Name) o).id);
        }
        final boolean[] hit = {false};
        Infer.children(o, x -> hit[0] |= !hit[0] && mentions(x, id));
        return hit[0];
    }

    private static boolean isTop(Map<String, Node.Module> trees, ClassDef k) {
        for (Node.Module m : trees.values()) {
            if (m.body.contains(k)) {
                return true;
            }
        }
        return false;
    }


    private static boolean relevant(Object o, final String top) {
        final boolean[] r = {false};
        imports(o, top, r);
        return r[0];
    }

    private static void imports(Object o, final String top, final boolean[] r) {
        if (r[0]) {
            return;
        }
        if (o instanceof Node.ImportFrom) {
            Node.ImportFrom f = (Node.ImportFrom) o;
            if (f.module != null && (f.module.equals(top) || f.module.startsWith(top + "."))) {
                r[0] = true;
            }
        } else if (o instanceof Node.Import) {
            for (Node.Alias a : ((Node.Import) o).names) {
                if (a.name.equals(top) || a.name.startsWith(top + ".")) {
                    r[0] = true;
                }
            }
        }
        Infer.children(o, x -> imports(x, top, r));
    }


    private static boolean isSuperInit(Call call) {
        return call.func instanceof Attribute && "__init__".equals(((Attribute) call.func).attr)
                && ((Attribute) call.func).value instanceof Call
                && isName(((Call) ((Attribute) call.func).value).func, "super");
    }

    private static void walk(Object o, final Ctx c, final ClassDef cls, final FunctionDef fn,
            final Node.For rootLoop) {
        if (c.refusal != null || o == rootLoop) { //NOPMD CompareObjectsWithEquals - identity is meant
            return;
        }
        ClassDef inCls = cls;
        FunctionDef inFn = fn;
        if (o instanceof ClassDef) {
            inCls = c.famDefs.containsKey(o) ? (ClassDef) o : null;
            {
                // Any class body: `sqref = CellRange` stores the class too.
                for (Stmt st : ((ClassDef) o).body) {
                    if (st instanceof Node.Assign && ((Node.Assign) st).targets.size() == 1
                            && ((Node.Assign) st).targets.get(0) instanceof Name
                            && (((Node.Assign) st).value instanceof Name
                            || ((Node.Assign) st).value instanceof Attribute)
                            && isFamily(c, ((Node.Assign) st).value)) {
                        c.allowed.put(((Node.Assign) st).value, Boolean.TRUE);
                        String k = ((Name) ((Node.Assign) st).targets.get(0)).id;
                        c.escClasses.put(o, Boolean.TRUE);
                        if (inCls == null && initStores((ClassDef) o, k)) {
                            // Every instance stores its own `self.k` first,
                            // so only the class object reads the class; that
                            // is checked not to escape below.
                            c.shadowed.put(o, k);
                        } else {
                            c.escAttrs.add(k);
                        }
                    }
                }
            }
            inFn = null;
        } else if (o instanceof FunctionDef) {
            inFn = (FunctionDef) o;
            if (inCls != null && inFn.args.kwarg != null && !inFn.body.contains(rootLoop)) {
                kwUses(inFn.body, inFn.args.kwarg.name, c);
            }
        } else if (o instanceof Node.Assign && isAll((Node.Assign) o)) {
            return;
        } else if (o instanceof Call) {
            Call call = (Call) o;
            String n = nameOf(call.func);
            boolean site = isFamily(c, call.func);
            if (n != null && (site || "isinstance".equals(n) || "issubclass".equals(n))) {
                c.allowed.put(call.func, Boolean.TRUE);
            }
            if (("isinstance".equals(n) || "issubclass".equals(n)) && call.args.size() == 2) {
                Expr t = call.args.get(1);
                c.allowed.put(t, Boolean.TRUE);
                if (t instanceof Node.TupleExpr) {
                    for (Expr x : ((Node.TupleExpr) t).elts) {
                        c.allowed.put(x, Boolean.TRUE);
                    }
                }
            }
            boolean init = "__init__".equals(n) && call.func instanceof Attribute
                    && (isSuperInit(call) ? cls != null
                    : isFamily(c, ((Attribute) call.func).value));
            if (init && !isSuperInit(call)) {
                c.allowed.put(((Attribute) call.func).value, Boolean.TRUE);
            }
            if (cls != null && (isName(call.func, "cls") || call.func instanceof Call
                    && isName(((Call) call.func).func, "type")
                    || call.func instanceof Attribute && "__class__".equals(((Attribute) call.func).attr))) {
                c.refusal = "a constructor call no name shows in " + cls.name;
                return;
            }
            if (site || init) {
                for (Keyword k : call.keywords) {
                    if (k.arg != null) {
                        c.keys.add(k.arg);
                        if (site && (k.value instanceof Name || k.value instanceof Attribute)
                                && isFamily(c, k.value)) {
                            // `Typed(expected_type=Percentage)`: the class is
                            // stored as the attribute, whose reads are checked.
                            c.allowed.put(k.value, Boolean.TRUE);
                            c.escAttrs.add(k.arg);
                        }
                    } else if (!(cls != null && fn != null && fn.args.kwarg != null
                            && isName(k.value, fn.args.kwarg.name))) {
                        c.refusal = "a **mapping call of " + n + " at line " + call.line;
                        return;
                    } else {
                        c.allowed.put(k.value, Boolean.TRUE);
                    }
                }
            }
        } else if (o instanceof Name) {
            if (!c.allowed.containsKey(o) && isFamily(c, (Name) o)) {
                c.refusal = ((Name) o).id + " escapes in " + c.mod + " at line " + ((Name) o).line;
                return;
            }
        } else if (o instanceof Attribute) {
            String a = ((Attribute) o).attr;
            if (!a.startsWith("__") || "__set__".equals(a) || "__get__".equals(a) || "__delete__".equals(a)
                    || "__init__".equals(a) || "__doc__".equals(a)) {
                // `Descriptor.__set__(self, ...)` reads a member; the class
                // object itself goes nowhere.
                c.allowed.put(((Attribute) o).value, Boolean.TRUE);
            }
            if (!c.allowed.containsKey(o) && isFamily(c, (Attribute) o)) {
                c.refusal = ((Attribute) o).attr + " escapes in " + c.mod + " at line " + ((Attribute) o).line;
                return;
            }
        }
        final ClassDef cc = inCls;
        final FunctionDef ff = inFn;
        Infer.children(o, x -> walk(x, c, cc, ff, rootLoop));
    }

    private static boolean isAll(Node.Assign a) {
        return a.targets.size() == 1 && a.targets.get(0) instanceof Name
                && "__all__".equals(((Name) a.targets.get(0)).id);
    }

    /// Checks that every use of the family `__init__`'s `**kw` named `kw` is
    /// a forward, a literal key or a membership test, and adds those keys.
    private static void kwUses(Object o, final String kw, final Ctx c) {
        if (c.refusal != null) {
            return;
        }
        if (o instanceof Node.Subscript && isName(((Node.Subscript) o).value, kw)) {
            Expr i = ((Node.Subscript) o).index;
            if (!(i instanceof Constant) || ((Constant) i).kind != Constant.Kind.STR) {
                c.refusal = "a computed key of " + kw;
                return;
            }
            c.keys.add(((Constant) i).text);
            c.allowed.put(((Node.Subscript) o).value, Boolean.TRUE);
        } else if (o instanceof Call && ((Call) o).func instanceof Attribute
                && isName(((Attribute) ((Call) o).func).value, kw)) {
            Call call = (Call) o;
            String m = ((Attribute) call.func).attr;
            if (!("get".equals(m) || "pop".equals(m) || "setdefault".equals(m)) || call.args.isEmpty()
                    || !(call.args.get(0) instanceof Constant)
                    || ((Constant) call.args.get(0)).kind != Constant.Kind.STR) {
                c.refusal = kw + "." + m + " in a family __init__";
                return;
            }
            c.keys.add(((Constant) call.args.get(0)).text);
            c.allowed.put(((Attribute) call.func).value, Boolean.TRUE);
        } else if (o instanceof Node.Compare && ((Node.Compare) o).comparators.size() == 1
                && isName(((Node.Compare) o).comparators.get(0), kw)
                && ((Node.Compare) o).left instanceof Constant) {
            c.allowed.put(((Node.Compare) o).comparators.get(0), Boolean.TRUE);
        } else if (o instanceof Call && (isSuperInit((Call) o) || isFamily(c, ((Call) o).func))) {
            // Only a forward into the family keeps the keys inside it.
            for (Keyword k : ((Call) o).keywords) {
                if (k.arg == null && isName(k.value, kw)) {
                    c.allowed.put(k.value, Boolean.TRUE);
                }
            }
        } else if (isName(o, kw) && !c.allowed.containsKey(o)) {
            c.refusal = kw + " used other than by literal key";
            return;
        }
        Infer.children(o, x -> kwUses(x, kw, c));
    }
}
