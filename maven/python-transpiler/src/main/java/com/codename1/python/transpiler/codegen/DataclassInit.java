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

import com.codename1.python.transpiler.api.PythonSyntaxError;
import com.codename1.python.transpiler.ast.Node;
import com.codename1.python.transpiler.ast.Node.Alias;
import com.codename1.python.transpiler.ast.Node.AnnAssign;
import com.codename1.python.transpiler.ast.Node.Assign;
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.Import;
import com.codename1.python.transpiler.ast.Node.ImportFrom;
import com.codename1.python.transpiler.ast.Node.Keyword;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.ast.Node.UnaryOp;
import com.codename1.python.transpiler.parser.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Every method a `@dataclass` class gets, written into the class at
/// compile time as plain Python that reads and writes `self.<field>`.
///
/// CPython's decorator builds the methods by exec'ing source text it puts
/// together from the field list. Nothing is compiled at run time here, so
/// this does the same one step earlier, right after parsing, as if the
/// author had written the methods:
///
/// ```python
/// @dataclass(order=True)
/// class Version:
///     major: int
///     minor: int = 0
///     # added:
///     def __init__(self, major, minor=minor):
///         self.major = major
///         self.minor = minor
///     def __repr__(self): ...      # Version(major=1, minor=0)
///     def __eq__(self, other): ... # (self.major, self.minor) == (...)
///     def __lt__(self, other): ... # and __le__, __gt__, __ge__
///     __hash__ = None
///     __match_args__ = ("major", "minor",)
///     def __cn1_asdict__(self, dict_factory): ...
///     def __cn1_astuple__(self, tuple_factory): ...
///     def __replace__(self, *, major=MISSING, minor=MISSING): ...
/// ```
///
/// The decorator still runs, and only records what CPython's records:
/// `__dataclass_fields__`, `__dataclass_params__`, the frozen
/// `__setattr__`. It checks that the fields it finds are the ones this
/// class wrote methods for, so a base the build could not see (one
/// imported from another module) is an error when the class is made, never
/// a constructor that silently drops a field.
///
/// A default is the class attribute of the same name, read when the method
/// is defined at the end of the class body (an inherited one through the
/// base it was inherited from): the very object the decorator would have
/// used. A `default_factory` is called by name in the constructor. A shape
/// the build cannot lower -- options that are not constants, a factory that
/// is not a name, a field annotated under an `if` -- fails the build naming
/// the line.
public final class DataclassInit {
    private static final Set<String> FIELD_KEYWORDS = new HashSet<String>(java.util.Arrays.asList("default",
            "default_factory", "init", "repr", "hash", "compare", "metadata", "kw_only"));
    private static final String MISSING = "_cn1_dc_MISSING";

    private DataclassInit() {
    }

    /// Rewrites the dataclasses of `tree`, the module at path `path`.
    public static void rewrite(Node.Module tree, String path) {
        Set<String> deco = new HashSet<String>();
        Set<String> field = new HashSet<String>();
        Set<String> modules = new HashSet<String>();
        Set<String> make = new HashSet<String>();
        Set<String> wrapped = new HashSet<String>();
        for (Stmt s : tree.body) {
            if (s instanceof ImportFrom && "pydantic.dataclasses".equals(((ImportFrom) s).module)
                    && ((ImportFrom) s).level == 0) {
                // pydantic's decorator runs the standard one and then wraps
                // the constructor it made; the class is lowered all the same.
                for (Alias a : ((ImportFrom) s).names) {
                    if ("dataclass".equals(a.name)) {
                        wrapped.add(a.asname != null ? a.asname : a.name);
                    }
                }
            }
            if (s instanceof ImportFrom && "dataclasses".equals(((ImportFrom) s).module) && ((ImportFrom) s).level == 0) {
                for (Alias a : ((ImportFrom) s).names) {
                    String bound = a.asname != null ? a.asname : a.name;
                    if ("dataclass".equals(a.name)) {
                        deco.add(bound);
                    } else if ("field".equals(a.name)) {
                        field.add(bound);
                    } else if ("make_dataclass".equals(a.name)) {
                        make.add(bound);
                    }
                }
            } else if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    if ("dataclasses".equals(a.name)) {
                        modules.add(a.asname != null ? a.asname : a.name);
                    }
                }
            }
        }
        deco.addAll(wrapped);
        if (deco.isEmpty() && modules.isEmpty() && make.isEmpty()) {
            return;
        }
        makeDataclasses(tree, make, modules, deco);
        List<ClassDef> decorated = new ArrayList<ClassDef>();
        for (Stmt s : tree.body) {
            decorated(s, deco, modules, decorated);
        }
        if (decorated.isEmpty()) {
            return;
        }
        Map<String, Integer> bindings = new HashMap<String, Integer>();
        for (Stmt s : tree.body) {
            count(s, bindings);
        }
        Set<String> used = new HashSet<String>(deco);
        used.addAll(field);
        used.addAll(modules);
        for (String n : used) {
            if (bindings.getOrDefault(n, 0) != 1) {
                throw error(path, decorated.get(0), "'" + n + "' is bound more than once in this module; a dataclass"
                        + " is lowered at build time only while the names it is made with are bound once, by"
                        + " their import");
            }
        }
        Map<String, ClassDef> classes = new HashMap<String, ClassDef>();
        allClasses(tree, classes);
        Map<String, Model> models = new HashMap<String, Model>();
        boolean pdImport = false;
        for (ClassDef c : decorated) {
            Model m = model(c, path, deco, field, modules, classes, models, bindings);
            models.put(c.name, m);
            specs(c, field, modules);
            boolean pd = false;
            for (Expr d : c.decorators) {
                pd |= isDataclass(d instanceof Call ? ((Call) d).func : d, wrapped, new HashSet<String>());
            }
            generate(c, m, path, pd);
            if (pd) {
                pdImport = true;
            }
        }
        List<Alias> names = new ArrayList<Alias>();
        names.add(new Alias("MISSING", MISSING));
        names.add(new Alias("_repr_enter", "_cn1_dc_repr_enter"));
        names.add(new Alias("_repr_leave", "_cn1_dc_repr_leave"));
        names.add(new Alias("_asdict_inner", "_cn1_dc_asdict_inner"));
        names.add(new Alias("_astuple_inner", "_cn1_dc_astuple_inner"));
        ImportFrom imp = new ImportFrom("dataclasses", 0, names);
        int i = 0;
        while (i < tree.body.size() && (tree.body.get(i) instanceof ImportFrom
                && "__future__".equals(((ImportFrom) tree.body.get(i)).module)
                || i == 0 && tree.body.get(i) instanceof Node.ExprStmt)) {
            i++;
        }
        at(imp, decorated.get(0).line);
        tree.body.add(i, imp);
        if (pdImport) {
            List<Alias> pdNames = new ArrayList<Alias>();
            pdNames.add(new Alias("_cn1_validate", "_cn1_pd_dc_validate"));
            ImportFrom pdImp = new ImportFrom("pydantic.dataclasses", 0, pdNames);
            at(pdImp, decorated.get(0).line);
            tree.body.add(i + 1, pdImp);
        }
    }

    private static void allClasses(Object o, Map<String, ClassDef> out) {
        if (o instanceof ClassDef) {
            out.put(((ClassDef) o).name, (ClassDef) o);
        }
        Infer.children(o, x -> allClasses(x, out));
    }

    private static PythonSyntaxError error(String path, Node at, String why) {
        return new PythonSyntaxError(path, at.line, 1, why);
    }

    /// Every class under `o`, at any depth, that a dataclass decorator
    /// names.
    private static void decorated(Object o, Set<String> deco, Set<String> modules, List<ClassDef> out) {
        if (o instanceof ClassDef) {
            for (Expr d : ((ClassDef) o).decorators) {
                Expr f = d instanceof Call ? ((Call) d).func : d;
                if (isDataclass(f, deco, modules)) {
                    out.add((ClassDef) o);
                    break;
                }
            }
        }
        Infer.children(o, x -> decorated(x, deco, modules, out));
    }

    /// `Name = make_dataclass("Name", [...], flag=True)` with a literal
    /// field list, at module level, becomes the class statement it stands
    /// for, so it is lowered like any other. The runtime builds no class
    /// from a list of names.
    private static void makeDataclasses(Node.Module tree, Set<String> make, Set<String> modules,
            Set<String> deco) {
        boolean imported = false;
        for (int i = 0; i < tree.body.size(); i++) {
            Stmt s = tree.body.get(i);
            if (!(s instanceof Assign) || ((Assign) s).targets.size() != 1
                    || !(((Assign) s).targets.get(0) instanceof Name) || !(((Assign) s).value instanceof Call)) {
                continue;
            }
            Call c = (Call) ((Assign) s).value;
            boolean named = c.func instanceof Name && make.contains(((Name) c.func).id)
                    || c.func instanceof Attribute && "make_dataclass".equals(((Attribute) c.func).attr)
                    && ((Attribute) c.func).value instanceof Name
                    && modules.contains(((Name) ((Attribute) c.func).value).id);
            String target = ((Name) ((Assign) s).targets.get(0)).id;
            if (!named || c.args.size() != 2 || !(c.args.get(0) instanceof Constant)
                    || ((Constant) c.args.get(0)).kind != Constant.Kind.STR
                    || !target.equals(((Constant) c.args.get(0)).text)
                    || !(c.args.get(1) instanceof Node.ListExpr || c.args.get(1) instanceof Node.TupleExpr)) {
                continue;
            }
            List<Expr> items = c.args.get(1) instanceof Node.ListExpr ? ((Node.ListExpr) c.args.get(1)).elts
                    : ((Node.TupleExpr) c.args.get(1)).elts;
            List<Stmt> body = new ArrayList<Stmt>();
            boolean ok = true;
            for (Expr it : items) {
                if (it instanceof Constant && ((Constant) it).kind == Constant.Kind.STR) {
                    body.add(new AnnAssign(new Name(((Constant) it).text),
                            new Constant(Constant.Kind.STR, "typing.Any"), null));
                    continue;
                }
                List<Expr> parts = it instanceof Node.TupleExpr ? ((Node.TupleExpr) it).elts
                        : it instanceof Node.ListExpr ? ((Node.ListExpr) it).elts : null;
                if (parts == null || parts.size() < 2 || parts.size() > 3 || !(parts.get(0) instanceof Constant)
                        || ((Constant) parts.get(0)).kind != Constant.Kind.STR) {
                    ok = false;
                    break;
                }
                body.add(new AnnAssign(new Name(((Constant) parts.get(0)).text), parts.get(1),
                        parts.size() == 3 ? parts.get(2) : null));
            }
            List<Keyword> kws = new ArrayList<Keyword>();
            for (Keyword k : c.keywords) {
                if (k.arg == null || !isBool(k.value)) {
                    ok = false;
                    break;
                }
                kws.add(k);
            }
            if (!ok) {
                continue;
            }
            if (body.isEmpty()) {
                body.add(new Node.Pass());
            }
            Call d = new Call(new Name("_cn1_dataclass"));
            d.keywords.addAll(kws);
            List<Expr> decos = new ArrayList<Expr>();
            decos.add(d);
            ClassDef k = new ClassDef(target, new ArrayList<Expr>(), new ArrayList<Keyword>(), body, decos);
            at(k, s.line);
            tree.body.set(i, k);
            if (!imported) {
                List<Alias> names = new ArrayList<Alias>();
                names.add(new Alias("dataclass", "_cn1_dataclass"));
                ImportFrom imp = new ImportFrom("dataclasses", 0, names);
                at(imp, s.line);
                tree.body.add(i, imp);
                i++;
                imported = true;
                deco.add("_cn1_dataclass");
            }
        }
    }

    /// Moves every `x: T = field(...)` out of the class attributes: the
    /// `Field` objects go into `__dataclass_field_specs__`, a dict the
    /// decorator reads by key, and the attribute keeps the default alone
    /// (or nothing). The runtime then never deletes or rebinds a class
    /// attribute by a name it computed, as CPython's decorator does.
    private static void specs(ClassDef c, Set<String> field, Set<String> modules) {
        int first = -1;
        Node.DictExpr dict = new Node.DictExpr();
        for (int i = 0; i < c.body.size(); i++) {
            Stmt s = c.body.get(i);
            if (!(s instanceof AnnAssign) || !(((AnnAssign) s).target instanceof Name)
                    || !(((AnnAssign) s).value instanceof Call)
                    || !isField(((Call) ((AnnAssign) s).value).func, field, modules)) {
                continue;
            }
            AnnAssign a = (AnnAssign) s;
            String n = ((Name) a.target).id;
            Call fc = (Call) a.value;
            Expr dflt = null;
            for (Keyword k : fc.keywords) {
                if ("default".equals(k.arg)) {
                    dflt = literal(k.value) ? copy(k.value)
                            : new Attribute(new Node.Subscript(new Name("__dataclass_field_specs__"),
                                    new Constant(Constant.Kind.STR, n)), "default");
                }
            }
            dict.keys.add(new Constant(Constant.Kind.STR, n));
            dict.values.add(fc);
            AnnAssign r = new AnnAssign(a.target, a.annotation, dflt);
            at(r, s.line);
            fc.line = s.line;
            c.body.set(i, r);
            if (first < 0) {
                first = i;
            }
        }
        if (first < 0) {
            return;
        }
        List<Expr> t = new ArrayList<Expr>();
        t.add(new Name("__dataclass_field_specs__"));
        Assign spec = new Assign(t, dict);
        spec.line = c.body.get(first).line;
        t.get(0).line = spec.line;
        dict.line = spec.line;
        c.body.add(first, spec);
    }

    /// How many times each module-level name is bound: by a store, a def,
    /// a class or an import, at the top level or in any nested block.
    static void count(Object o, Map<String, Integer> n) {
        if (o instanceof FunctionDef) {
            n.merge(((FunctionDef) o).name, 1, Integer::sum);
            return;
        }
        if (o instanceof ClassDef) {
            n.merge(((ClassDef) o).name, 1, Integer::sum);
            return;
        }
        if (o instanceof Import) {
            for (Alias a : ((Import) o).names) {
                String b = a.asname != null ? a.asname : a.name;
                int dot = b.indexOf('.');
                n.merge(dot < 0 ? b : b.substring(0, dot), 1, Integer::sum);
            }
            return;
        }
        if (o instanceof ImportFrom) {
            for (Alias a : ((ImportFrom) o).names) {
                n.merge(a.asname != null ? a.asname : a.name, 1, Integer::sum);
            }
            return;
        }
        if (o instanceof Name) {
            // Any store or delete through a plain name; loads count too,
            // which only makes the test stricter for names that are read.
            return;
        }
        if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                targets(t, n);
            }
        } else if (o instanceof AnnAssign && ((AnnAssign) o).value != null) {
            targets(((AnnAssign) o).target, n);
        } else if (o instanceof Node.AugAssign) {
            targets(((Node.AugAssign) o).target, n);
        } else if (o instanceof Node.For) {
            targets(((Node.For) o).target, n);
        } else if (o instanceof Node.Global || o instanceof Node.Delete || o instanceof Node.With
                || o instanceof Node.Try || o instanceof Node.NamedExpr) {
            // A rebinding this does not follow name by name: every name the
            // statement mentions counts as bound twice.
            Infer.children(o, x -> {
                if (x instanceof Name) {
                    n.merge(((Name) x).id, 2, Integer::sum);
                }
            });
        }
        if (!(o instanceof Node.Global)) {
            Infer.children(o, x -> count(x, n));
        }
    }

    private static void targets(Expr t, Map<String, Integer> n) {
        if (t instanceof Name) {
            n.merge(((Name) t).id, 1, Integer::sum);
        } else if (t instanceof Node.TupleExpr) {
            for (Expr e : ((Node.TupleExpr) t).elts) {
                targets(e, n);
            }
        } else if (t instanceof Node.ListExpr) {
            for (Expr e : ((Node.ListExpr) t).elts) {
                targets(e, n);
            }
        } else if (t instanceof Node.Starred) {
            targets(((Node.Starred) t).value, n);
        }
    }

    /// One field, as the decorator sees it: a plain field or an `InitVar`.
    private static final class Field {
        final String name;
        boolean initVar;
        boolean init = true;
        boolean kwOnly;
        boolean repr = true;
        boolean compare = true;
        /// Null to follow `compare`.
        Boolean hash;
        boolean hasDefault;
        /// The base this field was inherited through, whose attribute holds
        /// the default; null for a field of the class itself.
        String owner;
        /// The factory, as source: a name or a dotted name.
        String factory;

        Field(String name) {
            this.name = name;
        }

        Field inherited(String base) {
            Field f = new Field(name);
            f.initVar = initVar;
            f.init = init;
            f.kwOnly = kwOnly;
            f.repr = repr;
            f.compare = compare;
            f.hash = hash;
            f.hasDefault = hasDefault;
            f.factory = factory;
            f.owner = base;
            return f;
        }

        /// The parameter default: the class attribute, the "not passed"
        /// marker for a factory, or null for none.
        Expr dflt() {
            if (factory != null) {
                return new Name(MISSING);
            }
            if (!hasDefault) {
                return null;
            }
            return owner == null ? new Name(name) : new Attribute(new Name(owner), name);
        }

        boolean hashed() {
            return hash == null ? compare : hash;
        }
    }

    /// A dataclass: its options and its fields, inherited ones first.
    private static final class Model {
        final List<Field> fields = new ArrayList<Field>();
        final Map<String, Integer> index = new HashMap<String, Integer>();
        /// Every name the class body binds.
        final Set<String> own = new HashSet<String>();
        boolean init = true;
        boolean repr = true;
        boolean eq = true;
        boolean order;
        boolean unsafeHash;
        boolean frozen;
        boolean matchArgs = true;
        boolean kwOnly;
        boolean postInit;
        /// Every base is the module's own, so all fields are known here.
        boolean closed = true;
        boolean dataclassBase;

        void put(Field f) {
            Integer i = index.get(f.name);
            if (i == null) {
                index.put(f.name, fields.size());
                fields.add(f);
            } else {
                fields.set(i, f);
            }
        }
    }

    private static Model model(ClassDef c, String path, Set<String> deco, Set<String> field, Set<String> modules,
            Map<String, ClassDef> classes, Map<String, Model> models, Map<String, Integer> bindings) {
        Model m = new Model();
        for (Expr d : c.decorators) {
            if (!(d instanceof Call) || !isDataclass(((Call) d).func, deco, modules)) {
                continue;
            }
            Call dc = (Call) d;
            if (!dc.args.isEmpty()) {
                throw error(path, c, "@dataclass takes its options by keyword");
            }
            for (Keyword k : dc.keywords) {
                options(path, c, k, m);
            }
        }
        if (m.order && !m.eq) {
            throw error(path, c, "eq must be true if order is true");
        }
        // Reverse MRO order, exact for a single chain of bases.
        for (int i = c.bases.size() - 1; i >= 0; i--) {
            Expr b = c.bases.get(i);
            inherit(b, b instanceof Name ? ((Name) b).id : null, m, classes, models, new HashSet<String>());
        }
        boolean kwOnly = m.kwOnly;
        for (Stmt s : c.body) {
            if (s instanceof FunctionDef) {
                m.own.add(((FunctionDef) s).name);
            } else if (s instanceof Assign) {
                for (Expr t : ((Assign) s).targets) {
                    if (t instanceof Name) {
                        m.own.add(((Name) t).id);
                    }
                }
            } else if (s instanceof AnnAssign && ((AnnAssign) s).target instanceof Name) {
                AnnAssign a = (AnnAssign) s;
                String n = ((Name) a.target).id;
                String kind = kind(a.annotation);
                if ("ClassVar".equals(kind)) {
                    continue;
                }
                if ("KW_ONLY".equals(kind)) {
                    kwOnly = true;
                    continue;
                }
                if ("self".equals(n) || n.startsWith("__") && !n.endsWith("__")) {
                    throw error(path, s, "a dataclass field named '" + n + "' cannot be lowered at build time");
                }
                Field f = new Field(n);
                f.kwOnly = kwOnly;
                f.initVar = "InitVar".equals(kind);
                if (a.value instanceof Call && isField(((Call) a.value).func, field, modules)) {
                    fieldCall(path, s, (Call) a.value, f, bindings);
                } else {
                    f.hasDefault = a.value != null;
                }
                m.put(f);
            } else if (!(s instanceof ClassDef)) {
                boolean[] bad = {false};
                annotations(s, bad);
                if (bad[0]) {
                    throw error(path, s, "a dataclass field annotated inside a conditional or a loop cannot be"
                            + " lowered at build time");
                }
            }
        }
        m.postInit |= m.own.contains("__post_init__");
        String seenDefault = null;
        for (Field f : m.fields) {
            if (!f.init || f.kwOnly) {
                continue;
            }
            if (f.hasDefault || f.factory != null) {
                seenDefault = f.name;
            } else if (seenDefault != null) {
                throw error(path, c, "non-default argument '" + f.name + "' follows default argument '"
                        + seenDefault + "'");
            }
        }
        for (Field f : m.fields) {
            if (f.factory != null) {
                String root = f.factory.indexOf('.') < 0 ? f.factory : f.factory.substring(0, f.factory.indexOf('.'));
                if (m.index.containsKey(root)) {
                    throw error(path, c, "the default_factory of field '" + f.name + "' is named like a field");
                }
            }
            if (m.frozen && "object".equals(f.name)) {
                throw error(path, c, "a frozen dataclass field named 'object' cannot be lowered at build time");
            }
        }
        return m;
    }

    private static void options(String path, ClassDef c, Keyword k, Model m) {
        if (k.arg == null || !isBool(k.value)) {
            throw error(path, c, "@dataclass options must be the constants True or False to be lowered at build"
                    + " time");
        }
        boolean v = isTrue(k.value);
        switch (k.arg) {
            case "init":
                m.init = v;
                break;
            case "repr":
                m.repr = v;
                break;
            case "eq":
                m.eq = v;
                break;
            case "order":
                m.order = v;
                break;
            case "unsafe_hash":
                m.unsafeHash = v;
                break;
            case "frozen":
                m.frozen = v;
                break;
            case "match_args":
                m.matchArgs = v;
                break;
            case "kw_only":
                m.kwOnly = v;
                break;
            case "slots":
            case "weakref_slot":
                // Instances have a fixed layout either way; no new class
                // replaces this one.
                break;
            default:
                throw error(path, c, "unknown @dataclass option " + k.arg);
        }
    }

    /// The fields base `b` gives, reached through `via`, the base the class
    /// names.
    private static void inherit(Expr b, String via, Model m, Map<String, ClassDef> classes,
            Map<String, Model> models, Set<String> seen) {
        if (b instanceof Name && "object".equals(((Name) b).id)) {
            return;
        }
        if (!(b instanceof Name) || !seen.add(((Name) b).id)) {
            m.closed = false;
            return;
        }
        String n = ((Name) b).id;
        Model bm = models.get(n);
        if (bm != null) {
            for (Field f : bm.fields) {
                m.put(f.inherited(via));
            }
            m.postInit |= bm.postInit;
            m.closed &= bm.closed;
            // A dataclass base that kept its layout (non-frozen, so no
            // decorator __setattr__ stands between generated code and its
            // fields) is a plain compiled parent; any other blocks one.
            ClassDef bk = classes.get(n);
            m.dataclassBase |= bk == null || !bk.dataclass || bm.frozen;
            return;
        }
        ClassDef k = classes.get(n);
        if (k == null || !k.decorators.isEmpty()) {
            m.closed = false;
            return;
        }
        if (!plainBase(b, classes, new HashSet<String>())) {
            m.closed = false;
        }
        for (Stmt s : k.body) {
            if (s instanceof FunctionDef && "__post_init__".equals(((FunctionDef) s).name)) {
                m.postInit = true;
            }
        }
        for (int i = k.bases.size() - 1; i >= 0; i--) {
            inherit(k.bases.get(i), via, m, classes, models, seen);
        }
    }

    /// `ClassVar`, `InitVar` or `KW_ONLY` for an annotation that is one,
    /// spelled as the runtime decorator recognizes it; null for a field.
    private static String kind(Expr a) {
        String n = null;
        if (a instanceof Constant && ((Constant) a).kind == Constant.Kind.STR) {
            String s = ((Constant) a).text;
            n = s.startsWith("ClassVar") || s.startsWith("typing.ClassVar") ? "ClassVar"
                    : s.startsWith("InitVar") ? "InitVar"
                    : "KW_ONLY".equals(s) || "dataclasses.KW_ONLY".equals(s) ? "KW_ONLY" : null;
            return n;
        }
        Expr t = a instanceof Node.Subscript ? ((Node.Subscript) a).value : a;
        n = t instanceof Name ? ((Name) t).id : t instanceof Attribute ? ((Attribute) t).attr : null;
        return "ClassVar".equals(n) || "InitVar".equals(n) || "KW_ONLY".equals(n) ? n : null;
    }

    /// Whether `o`, a statement of a class body, annotates a name in a
    /// nested block.
    private static void annotations(Object o, boolean[] bad) {
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Node.Lambda) {
            return;
        }
        if (o instanceof AnnAssign && ((AnnAssign) o).target instanceof Name) {
            bad[0] = true;
            return;
        }
        Infer.children(o, x -> annotations(x, bad));
    }

    /// `field(...)`, read into `f`.
    private static void fieldCall(String path, Stmt s, Call c, Field f, Map<String, Integer> bindings) {
        if (!c.args.isEmpty()) {
            throw error(path, s, "field() takes its options by keyword");
        }
        for (Keyword k : c.keywords) {
            if (k.arg == null || !FIELD_KEYWORDS.contains(k.arg)) {
                throw error(path, s, "field() option " + (k.arg == null ? "**" : k.arg)
                        + " cannot be lowered at build time");
            }
            switch (k.arg) {
                case "default":
                    f.hasDefault = true;
                    break;
                case "default_factory":
                    f.factory = dotted(k.value);
                    if (f.factory == null || bindings.getOrDefault(f.factory.split("\\.")[0], 0) > 1) {
                        throw error(path, s, "a default_factory must be a name or a dotted name bound once to be"
                                + " lowered at build time");
                    }
                    break;
                case "init":
                    f.init = flag(path, s, k);
                    break;
                case "kw_only":
                    f.kwOnly = flag(path, s, k);
                    break;
                case "repr":
                    f.repr = flag(path, s, k);
                    break;
                case "compare":
                    f.compare = flag(path, s, k);
                    break;
                case "hash":
                    f.hash = k.value instanceof Constant && ((Constant) k.value).kind == Constant.Kind.NONE ? null
                            : Boolean.valueOf(flag(path, s, k));
                    break;
                default:
                    // metadata: nothing a generated method reads.
                    break;
            }
        }
        if (f.hasDefault && f.factory != null) {
            throw error(path, s, "cannot specify both default and default_factory");
        }
        if (f.initVar && f.factory != null) {
            throw error(path, s, "field " + f.name + " cannot have a default factory");
        }
    }

    private static boolean flag(String path, Stmt s, Keyword k) {
        if (!isBool(k.value)) {
            throw error(path, s, "field() option " + k.arg + " must be True or False to be lowered at build time");
        }
        return isTrue(k.value);
    }

    private static String dotted(Expr e) {
        if (e instanceof Name) {
            return ((Name) e).id;
        }
        if (e instanceof Attribute) {
            String v = dotted(((Attribute) e).value);
            return v == null ? null : v + "." + ((Attribute) e).attr;
        }
        return null;
    }

    /// Writes the methods of `m` into `c`.
    private static void generate(ClassDef c, Model m, String path, boolean pd) {
        List<Field> plain = new ArrayList<Field>();
        List<Field> std = new ArrayList<Field>();
        List<Field> kw = new ArrayList<Field>();
        List<String> compared = new ArrayList<String>();
        List<String> hashed = new ArrayList<String>();
        List<String> initVars = new ArrayList<String>();
        StringBuilder names = new StringBuilder("(");
        for (Field f : m.fields) {
            names.append('"').append(f.name).append("\", ");
            if (f.initVar) {
                initVars.add(f.name);
            } else {
                plain.add(f);
                if (f.compare) {
                    compared.add(f.name);
                }
                if (f.hashed()) {
                    hashed.add(f.name);
                }
            }
            if (f.init) {
                (f.kwOnly ? kw : std).add(f);
            }
        }
        names.append(')');
        StringBuilder src = new StringBuilder();
        boolean init = m.init && !m.own.contains("__init__");
        List<Expr> initDefaults = new ArrayList<Expr>();
        List<Expr> initKwDefaults = new ArrayList<Expr>();
        if (init) {
            // pydantic's dataclass validates after the standard constructor:
            // that one is generated under its own name, and __init__ runs it
            // and then the validator over one literal accessor per field.
            src.append(pd ? "def __cn1_dc_init__(self" : "def __init__(self");
            for (Field f : std) {
                src.append(", ").append(f.name);
                Expr d = f.dflt();
                if (d != null) {
                    src.append("=None");
                    initDefaults.add(d);
                }
            }
            if (!kw.isEmpty()) {
                src.append(", *");
            }
            for (Field f : kw) {
                src.append(", ").append(f.name);
                Expr d = f.dflt();
                if (d != null) {
                    src.append("=None");
                }
                initKwDefaults.add(d);
            }
            src.append("):\n");
            int stores = 0;
            for (Field f : plain) {
                String v;
                if (f.init) {
                    v = f.factory != null ? f.factory + "() if " + f.name + " is " + MISSING + " else " + f.name
                            : f.name;
                } else if (f.factory != null) {
                    v = f.factory + "()";
                } else if (f.hasDefault) {
                    v = "self.__class__." + f.name;
                } else {
                    continue;
                }
                if (m.frozen) {
                    src.append("    object.__setattr__(self, \"").append(f.name).append("\", ").append(v).append(")\n");
                } else {
                    src.append("    self.").append(f.name).append(" = ").append(v).append('\n');
                }
                c.dataclassFields.add(f.name);
                stores++;
            }
            if (m.postInit) {
                src.append("    self.__post_init__(").append(String.join(", ", initVars)).append(")\n");
                stores++;
            }
            if (stores == 0) {
                src.append("    pass\n");
            }
            if (pd) {
                src.append("def __init__(self, *_cn1_a, **_cn1_k):\n    self.__cn1_dc_init__(*_cn1_a, **_cn1_k)\n")
                        .append("    _cn1_pd_dc_validate(self, len(_cn1_a), (");
                for (Field f : plain) {
                    src.append("(\"").append(f.name).append("\", lambda _cn1_o: _cn1_o.").append(f.name)
                            .append(", lambda _cn1_o, _cn1_v: object.__setattr__(_cn1_o, \"").append(f.name)
                            .append("\", _cn1_v)), ");
                }
                src.append("))\n__cn1_pd_validated__ = True\n");
            }
        }
        if (m.repr && !m.own.contains("__repr__")) {
            src.append("def __repr__(self):\n    if _cn1_dc_repr_enter(self):\n        return \"...\"\n    try:\n")
                    .append("        return self.__class__.__qualname__ + \"(");
            String sep = "";
            for (Field f : plain) {
                if (f.repr) {
                    src.append(sep).append(f.name).append("=\" + repr(self.").append(f.name).append(") + \"");
                    sep = ", ";
                }
            }
            src.append(")\"\n    finally:\n        _cn1_dc_repr_leave(self)\n")
                    .append("__repr__._dataclass_repr = True\n");
            // The fields the repr shows, name and value, for pprint: it
            // reads them here rather than by a computed attribute name.
            src.append("def __cn1_repr_items__(self):\n    return [");
            for (Field f : plain) {
                if (f.repr) {
                    src.append("(\"").append(f.name).append("\", self.").append(f.name).append("), ");
                }
            }
            src.append("]\n");
        }
        if (m.eq && !m.own.contains("__eq__")) {
            src.append("def __eq__(self, other):\n    if self is other:\n        return True\n")
                    .append("    if other.__class__ is self.__class__:\n        return ").append(tuple(compared, "self"))
                    .append(" == ").append(tuple(compared, "other")).append("\n    return NotImplemented\n");
        }
        if (m.order) {
            String[][] ops = {{"__lt__", "<"}, {"__le__", "<="}, {"__gt__", ">"}, {"__ge__", ">="}};
            for (String[] op : ops) {
                if (m.own.contains(op[0])) {
                    throw error(path, c, "Cannot overwrite attribute " + op[0] + " in class " + c.name
                            + ". Consider using functools.total_ordering");
                }
                src.append("def ").append(op[0]).append("(self, other):\n")
                        .append("    if other.__class__ is self.__class__:\n        return ")
                        .append(tuple(compared, "self")).append(' ').append(op[1]).append(' ')
                        .append(tuple(compared, "other")).append("\n    return NotImplemented\n");
            }
        }
        boolean explicitHash = m.own.contains("__hash__");
        if (m.unsafeHash && explicitHash) {
            throw error(path, c, "Cannot overwrite attribute __hash__ in class " + c.name);
        }
        if (m.unsafeHash || !explicitHash && m.eq && m.frozen) {
            src.append("def __hash__(self):\n    return hash(").append(tuple(hashed, "self")).append(")\n");
        } else if (!explicitHash && m.eq) {
            src.append("__hash__ = None\n");
        }
        if (m.matchArgs && !m.own.contains("__match_args__")) {
            src.append("__match_args__ = (");
            for (Field f : std) {
                src.append('"').append(f.name).append("\", ");
            }
            src.append(")\n");
        }
        src.append("def __cn1_asdict__(self, dict_factory):\n    return dict_factory([");
        for (Field f : plain) {
            src.append("(\"").append(f.name).append("\", _cn1_dc_asdict_inner(self.").append(f.name)
                    .append(", dict_factory)), ");
        }
        src.append("])\ndef __cn1_astuple__(self, tuple_factory):\n    return tuple_factory([");
        for (Field f : plain) {
            src.append("_cn1_dc_astuple_inner(self.").append(f.name).append(", tuple_factory), ");
        }
        src.append("])\n");
        List<Expr> replaceDefaults = new ArrayList<Expr>();
        if (!m.own.contains("__replace__")) {
            src.append("def __replace__(self");
            if (!m.fields.isEmpty()) {
                src.append(", *");
            }
            StringBuilder checks = new StringBuilder();
            StringBuilder call = new StringBuilder();
            for (Field f : m.fields) {
                src.append(", ").append(f.name).append("=None");
                Expr d = f.initVar ? f.dflt() : null;
                replaceDefaults.add(d != null ? d : new Name(MISSING));
                if (!f.init) {
                    checks.append("    if ").append(f.name).append(" is not ").append(MISSING)
                            .append(":\n        raise TypeError(\"field ").append(f.name)
                            .append(" is declared with init=False, it cannot be specified with replace()\")\n");
                    continue;
                }
                if (f.initVar) {
                    if (d == null) {
                        checks.append("    if ").append(f.name).append(" is ").append(MISSING)
                                .append(":\n        raise TypeError(\"InitVar '").append(f.name)
                                .append("' must be specified with replace()\")\n");
                    }
                    call.append(f.name).append('=').append(f.name).append(", ");
                } else {
                    call.append(f.name).append("=self.").append(f.name).append(" if ").append(f.name).append(" is ")
                            .append(MISSING).append(" else ").append(f.name).append(", ");
                }
            }
            src.append("):\n").append(checks).append("    return self.__class__(").append(call).append(")\n");
        }
        src.append("__cn1_dc_lowered__ = (").append(names).append(", ")
                .append(init ? m.postInit ? "True" : "False" : "None").append(")\n");
        int line = c.body.isEmpty() ? c.line : c.body.get(c.body.size() - 1).line;
        for (Stmt s : Parser.parse(src.toString(), path).body) {
            if (s instanceof FunctionDef && (pd ? "__cn1_dc_init__" : "__init__").equals(((FunctionDef) s).name)) {
                FunctionDef fd = (FunctionDef) s;
                for (int i = 0; i < initDefaults.size(); i++) {
                    fd.args.defaults.set(i, initDefaults.get(i));
                }
                for (int i = 0; i < initKwDefaults.size(); i++) {
                    fd.args.kwDefaults.set(i, initKwDefaults.get(i));
                }
                fd.dataclassInit = true;
            } else if (s instanceof FunctionDef && "__replace__".equals(((FunctionDef) s).name)) {
                FunctionDef fd = (FunctionDef) s;
                for (int i = 0; i < replaceDefaults.size(); i++) {
                    fd.args.kwDefaults.set(i, replaceDefaults.get(i));
                }
            }
            at(s, line);
            c.body.add(s);
        }
        // A class keeps a layout when every field it stores is known here.
        // A frozen class takes its __setattr__ from the decorator, so
        // generated code never stores its fields directly
        // (Layouts.Cls#frozenStores); it keeps a layout only with no base,
        // so no compiled ancestor's code stores into it either.
        c.dataclass = c.decorators.size() == 1 && m.closed && !m.dataclassBase && !(m.frozen && !c.bases.isEmpty());
        c.dataclassFrozen = m.frozen;
    }

    /// `(x.a, x.b, )`, the compared or hashed fields of `x` as a tuple.
    private static String tuple(List<String> names, String x) {
        StringBuilder b = new StringBuilder("(");
        for (String n : names) {
            b.append(x).append('.').append(n).append(", ");
        }
        return b.append(')').toString();
    }

    private static boolean isDataclass(Expr e, Set<String> deco, Set<String> modules) {
        return e instanceof Name && deco.contains(((Name) e).id)
                || e instanceof Attribute && "dataclass".equals(((Attribute) e).attr)
                && ((Attribute) e).value instanceof Name && modules.contains(((Name) ((Attribute) e).value).id);
    }

    private static boolean isField(Expr e, Set<String> field, Set<String> modules) {
        return e instanceof Name && field.contains(((Name) e).id)
                || e instanceof Attribute && "field".equals(((Attribute) e).attr)
                && ((Attribute) e).value instanceof Name && modules.contains(((Name) ((Attribute) e).value).id);
    }

    /// A literal whose value does not depend on when it is evaluated.
    private static boolean literal(Expr e) {
        if (e instanceof Constant) {
            return true;
        }
        return e instanceof UnaryOp && ("-".equals(((UnaryOp) e).op) || "+".equals(((UnaryOp) e).op))
                && ((UnaryOp) e).operand instanceof Constant;
    }

    private static Expr copy(Expr e) {
        if (e instanceof Constant) {
            return new Constant(((Constant) e).kind, ((Constant) e).text);
        }
        UnaryOp u = (UnaryOp) e;
        return new UnaryOp(u.op, copy(u.operand));
    }

    /// Whether `e` is the constant True or False.
    private static boolean isBool(Expr e) {
        return e instanceof Constant && (((Constant) e).kind == Constant.Kind.TRUE
                || ((Constant) e).kind == Constant.Kind.FALSE);
    }

    private static boolean isTrue(Expr e) {
        return e instanceof Constant && ((Constant) e).kind == Constant.Kind.TRUE;
    }

    private static boolean plainBase(Expr b, Map<String, ClassDef> classes, Set<String> seen) {
        if (b instanceof Name && "object".equals(((Name) b).id)) {
            return true;
        }
        if (!(b instanceof Name) || !seen.add(((Name) b).id)) {
            return false;
        }
        ClassDef k = classes.get(((Name) b).id);
        if (k == null || !k.decorators.isEmpty() || !k.keywords.isEmpty()) {
            return false;
        }
        for (Stmt s : k.body) {
            // An annotation alone binds nothing on an undecorated class, so
            // the decorator neither makes it a field nor reads a default
            // from it; one with a value is a class attribute the decorator's
            // getattr would take as a field's default.
            if (s instanceof AnnAssign && ((AnnAssign) s).value != null) {
                return false;
            }
        }
        for (Expr e : k.bases) {
            if (!plainBase(e, classes, seen)) {
                return false;
            }
        }
        return true;
    }

    /// Gives every node under `o` source line `line`, so generated code
    /// reports against the declaration it replaced.
    static void atLine(Object o, int line) {
        at(o, line);
    }

    private static void at(Object o, int line) {
        if (o instanceof Node) {
            ((Node) o).line = line;
        }
        Infer.children(o, x -> at(x, line));
    }
}
