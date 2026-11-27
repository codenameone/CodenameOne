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
import com.codename1.python.transpiler.parser.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Every method an attrs class gets, written into the class at compile time
/// as plain Python that names each attribute literally.
///
/// attrs builds `__init__`, `__repr__`, the comparisons and `__hash__` at
/// run time, as closures over the attribute list that read and write fields
/// by computed name. Here a class statement decorated with `@attr.s`,
/// `@attrs`, `@define`, `@mutable` or `@frozen` (imported from `attr` or
/// `attrs`) gets them one step earlier, the way codegen.DataclassInit does
/// for dataclasses:
///
/// ```python
/// @define
/// class Point:
///     x: int
///     y: int = field(default=0, converter=int)
///     # becomes:
///     x: int
///     y: int
///     _cn1_attrs_y = field(default=0, converter=int)
///     def __init__(self, x, y=NOTHING):
///         _cn1_a = Point.__attrs_attrs__
///         self.x = x
///         if y is NOTHING:
///             y = _cn1_attrs_default(_cn1_a[1], self)
///         self._cn1_attrs_v_y = _cn1_attrs_convert(_cn1_a[1], self, y)
///     @property
///     def y(self): ...                  # on_setattr: converts, validates
///     def __repr__(self): ...
///     def __eq__(self, other): ...      # (self.x, self.y) == (other.x, other.y)
///     __hash__ = None
///     def __cn1_attrs_values__(self): return (self.x, self.y, )
///     __cn1_attrs_lowered__ = (("x", "y"), (("x", ...), ("y", _cn1_attrs_y)), ...)
/// ```
///
/// The decorator is replaced by `attr._cn1_finish`, which only builds the
/// `Attribute` list (`__attrs_attrs__`) the methods index, checks that it
/// holds the attributes the build wrote methods for, and installs the frozen
/// guard. `attr.ib()`/`field()` objects stay, under a hidden name, so that
/// defaults, factories, validators and converters are the very objects the
/// author wrote, evaluated where they were written. An `on_setattr` hook
/// (attrs' `define` default converts and validates) is a generated property
/// over a hidden slot; `fields`, `asdict`, `astuple`, `evolve` and
/// `validate` read the values through the generated `__cn1_attrs_values__`.
///
/// What the build cannot see whole is refused in strict mode, naming its
/// line: `make_class`, a decorator applied to anything but a class
/// statement (`define(type(...))`), options that are not constants, a base
/// from another module, a class not at the top of its module.
///
/// Known difference: `object.__setattr__(inst, "x", v)` on an attribute with
/// an on_setattr hook runs the hook (the property's setter), where attrs'
/// slotted class would store the raw value.
public final class AttrsLowering {

    private static final Set<String> CLASSIC = new HashSet<String>(java.util.Arrays.asList("s", "attrs",
            "attributes", "dataclass"));
    private static final Set<String> DEFINE = new HashSet<String>(java.util.Arrays.asList("define", "mutable"));
    private static final Set<String> FIELDS = new HashSet<String>(java.util.Arrays.asList("ib", "attrib", "attr",
            "field"));

    private static final String PRE = "_cn1_attrs_";

    private AttrsLowering() {
    }

    /// The names a module binds to the attrs API.
    private static final class Api {
        final Set<String> modules = new HashSet<String>();
        /// Bound name to its attrs name (define, ib, make_class, ...).
        final Map<String, String> names = new HashMap<String, String>();

        /// The attrs name `e` refers to, or null.
        String of(Expr e) {
            if (e instanceof Name) {
                return names.get(((Name) e).id);
            }
            if (e instanceof Attribute && ((Attribute) e).value instanceof Name
                    && modules.contains(((Name) ((Attribute) e).value).id)) {
                return ((Attribute) e).attr;
            }
            return null;
        }

        boolean decorator(Expr e) {
            String n = of(e);
            return n != null && (CLASSIC.contains(n) || DEFINE.contains(n) || "frozen".equals(n));
        }

        boolean field(Expr e) {
            String n = of(e);
            return n != null && FIELDS.contains(n);
        }
    }

    private static final class Field {
        String name;
        String alias;
        boolean init = true;
        boolean kwOnly;
        boolean hasDefault;
        boolean validator;
        boolean converter;
        boolean onSet;
        /// 0 not compared, 1 compared by value, 2 through its key.
        int eq = 1;
        int order = 1;
        boolean hashed = true;
        /// The class-body statement that makes its attr.ib object, if any.
        Call call;
        Expr plainDefault;

        Field copy() {
            Field f = new Field();
            f.name = name;
            f.alias = alias;
            f.init = init;
            f.kwOnly = kwOnly;
            f.hasDefault = hasDefault;
            f.validator = validator;
            f.converter = converter;
            f.onSet = onSet;
            f.eq = eq;
            f.order = order;
            f.hashed = hashed;
            return f;
        }
    }

    private static final class Model {
        final List<Field> all = new ArrayList<Field>();
        final List<Field> own = new ArrayList<Field>();
        boolean frozen;
        boolean preInit;
        boolean postInit;
        /// The attributes stored behind an on_setattr property.
        final Set<String> hooked = new HashSet<String>();
    }

    /// Lowers the attrs classes of `tree`, the module at `path`.
    public static void rewrite(Node.Module tree, String path) {
        String p = path.replace('\\', '/');
        if (p.matches("(.*/)?attrs?/[^/]*\\.py")) {
            return;
        }
        Api api = new Api();
        for (Stmt s : tree.body) {
            if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    if ("attr".equals(a.name) || "attrs".equals(a.name)) {
                        api.modules.add(a.asname != null ? a.asname : a.name);
                    }
                }
            } else if (s instanceof ImportFrom && ((ImportFrom) s).level == 0
                    && ("attr".equals(((ImportFrom) s).module) || "attrs".equals(((ImportFrom) s).module))) {
                for (Alias a : ((ImportFrom) s).names) {
                    api.names.put(a.asname != null ? a.asname : a.name, a.name);
                }
            }
        }
        if (api.modules.isEmpty() && api.names.isEmpty()) {
            return;
        }
        Map<String, Integer> bindings = new HashMap<String, Integer>();
        for (Stmt s : tree.body) {
            DataclassInit.count(s, bindings);
        }
        Map<String, Model> models = new LinkedHashMap<String, Model>();
        Map<String, ClassDef> plain = new HashMap<String, ClassDef>();
        boolean any = false;
        for (Stmt s : tree.body) {
            if (!(s instanceof ClassDef)) {
                continue;
            }
            ClassDef c = (ClassDef) s;
            Expr deco = null;
            for (Expr d : c.decorators) {
                if (api.decorator(d instanceof Call ? ((Call) d).func : d)) {
                    deco = d;
                }
            }
            if (deco == null) {
                plain.put(c.name, c);
                continue;
            }
            String why = lower(c, deco, api, models, plain, bindings, path);
            if (why != null) {
                c.refusal = "attrs class " + c.name + " cannot be lowered at build time: " + why;
            } else {
                any = true;
            }
        }
        // Whatever still names a class decorator or make_class makes or
        // decorates a class the build cannot see: refused, naming the line.
        refuseDynamic(tree.body, api);
        if (any) {
            List<Alias> names = new ArrayList<Alias>();
            String[] helpers = {"_cn1_finish", "_cn1_default", "_cn1_convert", "_cn1_validating", "_cn1_on_set",
                "_cn1_repr"};
            for (String n : helpers) {
                names.add(new Alias(n, PRE + n.substring(5)));
            }
            names.add(new Alias("NOTHING", PRE + "NOTHING"));
            names.add(new Alias("attrib", PRE + "attrib"));
            ImportFrom imp = new ImportFrom("attr", 0, names);
            int i = 0;
            while (i < tree.body.size() && (tree.body.get(i) instanceof ImportFrom
                    && "__future__".equals(((ImportFrom) tree.body.get(i)).module)
                    || i == 0 && tree.body.get(i) instanceof Node.ExprStmt)) {
                i++;
            }
            DataclassInit.atLine(imp, 1);
            tree.body.add(i, imp);
            Set<String> hooked = new java.util.TreeSet<String>();
            for (Model m : models.values()) {
                hooked.addAll(m.hooked);
            }
            if (!hooked.isEmpty()) {
                rawStores(tree, hooked);
                StringBuilder src = new StringBuilder();
                for (String f : hooked) {
                    src.append("def ").append(PRE).append("raw_").append(f).append("(_cn1_o, _cn1_v):\n")
                            .append("    if getattr(type(_cn1_o), \"__cn1_attrs_raw_").append(f)
                            .append("__\", False):\n        _cn1_o.").append(PRE).append("v_").append(f)
                            .append(" = _cn1_v\n    else:\n        object.__setattr__(_cn1_o, \"").append(f)
                            .append("\", _cn1_v)\n");
                }
                int at = i + 1;
                for (Stmt s : Parser.parse(src.toString(), path).body) {
                    DataclassInit.atLine(s, 1);
                    tree.body.add(at++, s);
                }
            }
        }
    }

    /// `object.__setattr__(o, "x", v)` bypasses an attrs class's on_setattr
    /// hooks; here a hooked attribute is a property, which it would run. So
    /// each such statement, for an `x` some lowered class hooks, calls the
    /// module's `_cn1_attrs_raw_x(o, v)`, which stores the hidden slot of an
    /// instance of such a class and is `object.__setattr__` for any other.
    /// One inside an expression is refused.
    private static void rawStores(Object o, Set<String> hooked) {
        if (!(o instanceof Node) || o instanceof FunctionDef && ((FunctionDef) o).dataclassInit) {
            return;
        }
        for (Class<?> k = o.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
            for (java.lang.reflect.Field fl : k.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(fl.getModifiers())) {
                    continue;
                }
                Object x;
                try {
                    x = fl.get(o);
                } catch (IllegalAccessException e) {
                    continue;
                }
                if (x instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> l = (List<Object>) x;
                    for (int i = 0; i < l.size(); i++) {
                        Object y = l.get(i);
                        String f = y instanceof Node.ExprStmt ? rawField(((Node.ExprStmt) y).value, hooked) : null;
                        if (f != null) {
                            Call old = (Call) ((Node.ExprStmt) y).value;
                            Call c = new Call(new Name(PRE + "raw_" + f));
                            c.args.add(old.args.get(0));
                            c.args.add(old.args.get(2));
                            Node.ExprStmt st = new Node.ExprStmt(c);
                            st.line = ((Node) y).line;
                            c.line = st.line;
                            c.func.line = st.line;
                            l.set(i, st);
                        } else if (y instanceof Expr && rawField((Expr) y, hooked) != null) {
                            ((Node) y).refusal = REFUSE_RAW;
                        } else {
                            rawStores(y, hooked);
                        }
                    }
                } else if (x instanceof Expr && rawField((Expr) x, hooked) != null) {
                    ((Node) x).refusal = REFUSE_RAW;
                } else if (x instanceof Node) {
                    rawStores(x, hooked);
                }
            }
        }
    }

    private static final String REFUSE_RAW = "object.__setattr__ of an attrs attribute with an on_setattr hook"
            + " inside an expression cannot be lowered; make it a statement of its own";

    private static String rawField(Expr e, Set<String> hooked) {
        if (!(e instanceof Call)) {
            return null;
        }
        Call c = (Call) e;
        if (!(c.func instanceof Attribute) || !"__setattr__".equals(((Attribute) c.func).attr)
                || !(((Attribute) c.func).value instanceof Name)
                || !"object".equals(((Name) ((Attribute) c.func).value).id) || c.args.size() != 3
                || !c.keywords.isEmpty() || !isConst(c.args.get(1), Constant.Kind.STR)) {
            return null;
        }
        for (Expr a : c.args) {
            if (a instanceof Node.Starred) {
                return null;
            }
        }
        String f = ((Constant) c.args.get(1)).text;
        return hooked.contains(f) ? f : null;
    }

    private static void refuseDynamic(Object o, Api api) {
        if (o instanceof ClassDef) {
            // A lowered class's decorator is the finisher now; a refused
            // one is reported against the class already.
            ClassDef c = (ClassDef) o;
            boolean refused = c.refusal != null;
            for (Expr d : c.decorators) {
                // A bare decorator naming the API is what was lowered.
                if (!refused && !api.decorator(d)) {
                    refuseDynamic(d, api);
                }
            }
            refuseDynamic(c.bases, api);
            refuseDynamic(c.keywords, api);
            refuseDynamic(c.body, api);
            return;
        }
        if (o instanceof Call && "make_class".equals(api.of(((Call) o).func))) {
            ((Call) o).refusal = "attrs make_class builds a class at run time, which a closed-world build cannot"
                    + " compile; declare the class with a class statement and @define";
        } else if (o instanceof Call && api.decorator(((Call) o).func) && !((Call) o).args.isEmpty()) {
            ((Call) o).refusal = "attrs " + api.of(((Call) o).func) + "(...) applied to a class built at run time"
                    + " cannot be lowered; decorate a class statement instead";
        }
        if (o instanceof Call && api.decorator(((Call) o).func)) {
            // The callee is the API itself; only its arguments are values.
            Call c = (Call) o;
            refuseDynamic(c.args, api);
            refuseDynamic(c.keywords, api);
            return;
        }
        if ((o instanceof Name || o instanceof Attribute) && api.decorator((Expr) o)) {
            // `deco = attr.s`: the decorator escapes as a value, and what it
            // is later applied to cannot be seen at build time.
            ((Node) o).refusal = "attrs " + api.of((Expr) o) + " used as a value, not as a class decorator,"
                    + " cannot be lowered at build time; decorate each class statement with it directly";
            return;
        }
        Infer.children(o, x -> refuseDynamic(x, api));
    }

    private static boolean isConst(Expr e, Constant.Kind k) {
        return e instanceof Constant && ((Constant) e).kind == k;
    }

    private static final Map<Constant.Kind, Boolean> BOOLS = new HashMap<Constant.Kind, Boolean>();

    static {
        BOOLS.put(Constant.Kind.TRUE, Boolean.TRUE);
        BOOLS.put(Constant.Kind.FALSE, Boolean.FALSE);
    }

    /// The constant True or False `e` is, else no value.
    private static Boolean bool(Expr e) {
        return BOOLS.get(e instanceof Constant ? ((Constant) e).kind : Constant.Kind.NONE);
    }

    private static boolean none(Expr e) {
        return isConst(e, Constant.Kind.NONE);
    }

    /// Lowers `c`, decorated with `deco`; returns why not, or null.
    private static String lower(ClassDef c, Expr deco, Api api, Map<String, Model> models,
            Map<String, ClassDef> plain, Map<String, Integer> bindings, String path) {
        if (c.decorators.size() != 1) {
            return "it has decorators besides its attrs one";
        }
        if (bindings.getOrDefault(c.name, 0) != 1) {
            return "its name is bound more than once in the module";
        }
        String kind = api.of(deco instanceof Call ? ((Call) deco).func : deco);
        boolean define = DEFINE.contains(kind) || "frozen".equals(kind);
        boolean frozen = "frozen".equals(kind);
        Boolean auto = define ? null : "dataclass".equals(kind) ? Boolean.TRUE : Boolean.FALSE;
        Boolean eq = null;
        Boolean order = define ? Boolean.FALSE : null;
        Boolean hash = null;
        boolean init = true;
        boolean repr = true;
        boolean kwOnly = false;
        boolean matchArgs = true;
        boolean autoDetect = define || "dataclass".equals(kind);
        Expr onSet = null;
        if (deco instanceof Call) {
            Call dc = (Call) deco;
            if (!dc.args.isEmpty()) {
                return "the decorator takes positional arguments";
            }
            for (Keyword k : dc.keywords) {
                if (k.arg == null) {
                    return "the decorator's options are a **mapping the build cannot read";
                }
                if ("on_setattr".equals(k.arg)) {
                    onSet = none(k.value) ? null : k.value;
                    continue;
                }
                if ("slots".equals(k.arg) || "weakref_slot".equals(k.arg) || "cache_hash".equals(k.arg)
                        || "auto_exc".equals(k.arg) || "repr_ns".equals(k.arg) || "getstate_setstate".equals(k.arg)
                        || "collect_by_mro".equals(k.arg)) {
                    continue;
                }
                Boolean b = bool(k.value);
                boolean isNone = none(k.value);
                if (b == null && !(isNone && ("eq".equals(k.arg) || "order".equals(k.arg) || "hash".equals(k.arg)
                        || "unsafe_hash".equals(k.arg) || "auto_attribs".equals(k.arg) || "cmp".equals(k.arg)))) {
                    return "option " + k.arg + " is not the constant True or False";
                }
                switch (k.arg) {
                    case "frozen":
                        frozen = b;
                        break;
                    case "eq":
                        eq = b;
                        break;
                    case "order":
                        order = b;
                        break;
                    case "cmp":
                        if (b != null) {
                            eq = b;
                            order = b;
                        }
                        break;
                    case "hash":
                    case "unsafe_hash":
                        if (b != null) {
                            hash = b;
                        }
                        break;
                    case "init":
                        init = b;
                        break;
                    case "repr":
                        repr = b;
                        break;
                    case "kw_only":
                        kwOnly = b;
                        break;
                    case "match_args":
                        matchArgs = b;
                        break;
                    case "auto_attribs":
                        auto = b;
                        break;
                    case "auto_detect":
                        autoDetect = b;
                        break;
                    case "str":
                        if (b) {
                            return "str=True is not supported";
                        }
                        break;
                    default:
                        return "option " + k.arg + " is not supported";
                }
            }
        }
        if (frozen && onSet != null) {
            // attrs raises when the class is made; the finisher does too.
            onSet = null;
        }
        Model base = null;
        Model m = new Model();
        m.frozen = frozen;
        for (Expr b : c.bases) {
            if (b instanceof Name && "object".equals(((Name) b).id)) {
                continue;
            }
            if (!(b instanceof Name)) {
                return "a base is not a plain name";
            }
            String bn = ((Name) b).id;
            if (models.containsKey(bn)) {
                if (base != null) {
                    return "it has more than one attrs base";
                }
                base = models.get(bn);
            } else if (!plainBase(bn, plain, models, new HashSet<String>(), m)) {
                return "base " + bn + " is not a class the build can see in this module";
            }
        }
        Set<String> own = new HashSet<String>();
        for (Stmt s : c.body) {
            if (s instanceof FunctionDef) {
                own.add(((FunctionDef) s).name);
            } else if (s instanceof Assign) {
                for (Expr t : ((Assign) s).targets) {
                    if (t instanceof Name) {
                        own.add(((Name) t).id);
                    }
                }
            }
        }
        m.preInit = own.contains("__attrs_pre_init__") || base != null && base.preInit || m.preInit;
        m.postInit = own.contains("__attrs_post_init__") || base != null && base.postInit || m.postInit;
        // The fields this class declares, in body order.
        boolean unannotated = false;
        boolean annotated = false;
        for (Stmt s : c.body) {
            if (s instanceof Assign && ((Assign) s).value instanceof Call
                    && api.field(((Call) ((Assign) s).value).func)) {
                unannotated = true;
            }
            if (s instanceof AnnAssign && ((AnnAssign) s).target instanceof Name
                    && !classVar(((AnnAssign) s).annotation)) {
                annotated = true;
            }
        }
        if (auto == null) {
            auto = !unannotated && annotated;
        }
        if (auto && unannotated) {
            return "an attr.ib() without a type annotation in a class with auto_attribs";
        }
        Map<String, Field> byName = new LinkedHashMap<String, Field>();
        for (Stmt s : c.body) {
            Field f = null;
            if (s instanceof AnnAssign && ((AnnAssign) s).target instanceof Name && auto
                    && !classVar(((AnnAssign) s).annotation)) {
                AnnAssign a = (AnnAssign) s;
                f = new Field();
                f.name = ((Name) a.target).id;
                if (a.value instanceof Call && api.field(((Call) a.value).func)) {
                    f.call = (Call) a.value;
                } else if (a.value != null) {
                    f.plainDefault = a.value;
                    f.hasDefault = true;
                }
            } else if (s instanceof AnnAssign && ((AnnAssign) s).target instanceof Name
                    && ((AnnAssign) s).value instanceof Call && api.field(((Call) ((AnnAssign) s).value).func)) {
                f = new Field();
                f.name = ((Name) ((AnnAssign) s).target).id;
                f.call = (Call) ((AnnAssign) s).value;
            } else if (s instanceof Assign && ((Assign) s).value instanceof Call
                    && api.field(((Call) ((Assign) s).value).func)) {
                Assign a = (Assign) s;
                if (a.targets.size() != 1 || !(a.targets.get(0) instanceof Name)) {
                    return "an attr.ib() assigned to something other than one name (line " + s.line + ")";
                }
                f = new Field();
                f.name = ((Name) a.targets.get(0)).id;
                f.call = (Call) a.value;
            }
            if (f == null) {
                continue;
            }
            if (f.call != null) {
                String why = fieldCall(f, f.call);
                if (why != null) {
                    return why + " (line " + s.line + ")";
                }
            }
            if (f.alias == null) {
                int i = 0;
                while (i < f.name.length() && f.name.charAt(i) == '_') {
                    i++;
                }
                f.alias = f.name.substring(i);
            }
            f.kwOnly |= kwOnly;
            byName.remove(f.name);
            byName.put(f.name, f);
        }
        // @x.validator and @x.default.
        for (Stmt s : c.body) {
            if (!(s instanceof FunctionDef)) {
                continue;
            }
            for (Expr d : ((FunctionDef) s).decorators) {
                if (d instanceof Attribute && ((Attribute) d).value instanceof Name
                        && byName.containsKey(((Name) ((Attribute) d).value).id)) {
                    Field f = byName.get(((Name) ((Attribute) d).value).id);
                    String a = ((Attribute) d).attr;
                    if (f.call == null || !"validator".equals(a) && !"default".equals(a)) {
                        return "decorator @" + f.name + "." + a + " is not one the build can read";
                    }
                    if ("validator".equals(a)) {
                        f.validator = true;
                    } else {
                        f.hasDefault = true;
                    }
                }
            }
        }
        if (base != null) {
            for (Field f : base.all) {
                if (!byName.containsKey(f.name)) {
                    Field g = f.copy();
                    g.kwOnly |= kwOnly;
                    m.all.add(g);
                }
            }
        }
        m.own.addAll(byName.values());
        m.all.addAll(m.own);
        // auto_detect: a method the class writes itself is kept.
        if (autoDetect) {
            init &= !own.contains("__init__");
            repr &= !own.contains("__repr__");
            if (eq == null && (own.contains("__eq__") || own.contains("__ne__"))) {
                eq = Boolean.FALSE;
            }
            if (order == null && (own.contains("__lt__") || own.contains("__le__") || own.contains("__gt__")
                    || own.contains("__ge__"))) {
                order = Boolean.FALSE;
            }
            if (hash == null && own.contains("__hash__")) {
                hash = Boolean.FALSE;
            }
        }
        boolean isEq = eq == null || eq;
        boolean isOrder = order == null ? isEq : order;
        if (isOrder && !isEq) {
            return "order=True needs eq=True";
        }
        boolean hooks = !frozen;
        String src = generate(c.name, m, init, repr, isEq, isOrder, hash, onSet != null, define, hooks, own,
                kwOnly, matchArgs);
        // Rewrite the field statements: each attr.ib object stays under a
        // hidden name, an annotation stays an annotation.
        List<Stmt> body = new ArrayList<Stmt>();
        for (Stmt s : c.body) {
            Field f = fieldOf(s, byName, auto, api);
            if (f == null) {
                for (Expr d : s instanceof FunctionDef ? ((FunctionDef) s).decorators : new ArrayList<Expr>()) {
                    if (d instanceof Attribute && ((Attribute) d).value instanceof Name
                            && byName.containsKey(((Name) ((Attribute) d).value).id)) {
                        Expr nv = new Name(PRE + ((Name) ((Attribute) d).value).id);
                        DataclassInit.atLine(nv, d.line);
                        List<Expr> ds = ((FunctionDef) s).decorators;
                        Attribute na = new Attribute(nv, ((Attribute) d).attr);
                        DataclassInit.atLine(na, d.line);
                        ds.set(ds.indexOf(d), na);
                    }
                }
                body.add(s);
                continue;
            }
            if (s instanceof AnnAssign) {
                AnnAssign a = (AnnAssign) s;
                AnnAssign bare = new AnnAssign(a.target, a.annotation, null);
                DataclassInit.atLine(bare, s.line);
                body.add(bare);
            }
            Expr make;
            if (f.call != null) {
                make = f.call;
            } else {
                Call ib = new Call(new Name(PRE + "attrib"));
                if (f.plainDefault != null) {
                    ib.keywords.add(new Keyword("default", f.plainDefault));
                }
                DataclassInit.atLine(ib.func, s.line);
                ib.line = s.line;
                make = ib;
            }
            List<Expr> t = new ArrayList<Expr>();
            Name hidden = new Name(PRE + f.name);
            hidden.line = s.line;
            t.add(hidden);
            Assign as = new Assign(t, make);
            as.line = s.line;
            body.add(as);
        }
        c.body.clear();
        c.body.addAll(body);
        int line = c.body.isEmpty() ? c.line : c.body.get(c.body.size() - 1).line;
        for (Stmt s : Parser.parse(src, path).body) {
            DataclassInit.atLine(s, line);
            if (s instanceof FunctionDef && "__init__".equals(((FunctionDef) s).name)) {
                ((FunctionDef) s).dataclassInit = true;
            }
            if (s instanceof Assign && ((Assign) s).targets.get(0) instanceof Name
                    && "__cn1_attrs_lowered__".equals(((Name) ((Assign) s).targets.get(0)).id) && onSet != null) {
                ((Node.TupleExpr) ((Assign) s).value).elts.set(4, onSet);
            }
            c.body.add(s);
        }
        Expr fin = new Name(PRE + "finish");
        DataclassInit.atLine(fin, deco.line);
        c.decorators.clear();
        c.decorators.add(fin);
        // As a frozen dataclass: the finisher's __setattr__ decides every
        // store, so the class keeps a layout only with no base.
        c.dataclass = !(frozen && !c.bases.isEmpty());
        c.dataclassFrozen = frozen;
        models.put(c.name, m);
        return null;
    }

    /// Whether `bn` is a class of this module with no attrs in it: no
    /// decorator, no attribute a base would contribute.
    private static boolean plainBase(String bn, Map<String, ClassDef> plain, Map<String, Model> models,
            Set<String> seen, Model m) {
        ClassDef k = plain.get(bn);
        if (k == null || !seen.add(bn) || !k.decorators.isEmpty() || !k.keywords.isEmpty()) {
            return false;
        }
        for (Stmt s : k.body) {
            if (s instanceof FunctionDef && "__attrs_pre_init__".equals(((FunctionDef) s).name)) {
                m.preInit = true;
            }
            if (s instanceof FunctionDef && "__attrs_post_init__".equals(((FunctionDef) s).name)) {
                m.postInit = true;
            }
        }
        for (Expr e : k.bases) {
            if (e instanceof Name && "object".equals(((Name) e).id)) {
                continue;
            }
            if (!(e instanceof Name) || models.containsKey(((Name) e).id)
                    || !plainBase(((Name) e).id, plain, models, seen, m)) {
                return false;
            }
        }
        return true;
    }

    private static Field fieldOf(Stmt s, Map<String, Field> byName, boolean auto, Api api) {
        String n = null;
        if (s instanceof AnnAssign && ((AnnAssign) s).target instanceof Name) {
            AnnAssign a = (AnnAssign) s;
            if (auto && !classVar(a.annotation) || a.value instanceof Call && api.field(((Call) a.value).func)) {
                n = ((Name) a.target).id;
            }
        } else if (s instanceof Assign && ((Assign) s).value instanceof Call
                && api.field(((Call) ((Assign) s).value).func)) {
            n = ((Name) ((Assign) s).targets.get(0)).id;
        }
        Field f = n == null ? null : byName.get(n);
        return f != null && (f.call == null || f.call == (s instanceof Assign ? ((Assign) s).value
                : ((AnnAssign) s).value)) ? f : null;
    }

    private static boolean classVar(Expr ann) {
        Expr a = ann instanceof Node.Subscript ? ((Node.Subscript) ann).value : ann;
        if (a instanceof Name) {
            return "ClassVar".equals(((Name) a).id);
        }
        if (a instanceof Attribute) {
            return "ClassVar".equals(((Attribute) a).attr);
        }
        return a instanceof Constant && ((Constant) a).kind == Constant.Kind.STR
                && ((Constant) a).text.startsWith("ClassVar");
    }

    /// Reads the options of `attr.ib(...)`/`field(...)` into `f`.
    private static String fieldCall(Field f, Call c) {
        if (c.args.size() > 1) {
            return "attr.ib() with more than one positional argument";
        }
        if (c.args.size() == 1) {
            if (c.args.get(0) instanceof Node.Starred) {
                return "attr.ib(*args)";
            }
            f.hasDefault = true;
        }
        int eq = 1;
        int order = -1;
        Boolean hash = null;
        for (Keyword k : c.keywords) {
            if (k.arg == null) {
                return "attr.ib(**mapping) the build cannot read";
            }
            Boolean b = bool(k.value);
            switch (k.arg) {
                case "default":
                case "factory":
                    f.hasDefault |= !("factory".equals(k.arg) && none(k.value));
                    break;
                case "validator":
                    f.validator = !none(k.value);
                    break;
                case "converter":
                    f.converter = !none(k.value);
                    break;
                case "on_setattr":
                    f.onSet = !none(k.value);
                    break;
                case "init":
                    if (b == null) {
                        return "init= is not the constant True or False";
                    }
                    f.init = b;
                    break;
                case "kw_only":
                    if (b == null) {
                        return "kw_only= is not the constant True or False";
                    }
                    f.kwOnly = b;
                    break;
                case "alias":
                    if (!isConst(k.value, Constant.Kind.STR)) {
                        return "alias= is not a string literal";
                    }
                    f.alias = ((Constant) k.value).text;
                    break;
                case "hash":
                    if (b == null && !none(k.value)) {
                        return "hash= is not True, False or None";
                    }
                    hash = b;
                    break;
                case "cmp":
                    if (b == null && !none(k.value)) {
                        return "cmp= is not True, False or None";
                    }
                    if (b != null) {
                        eq = b ? 1 : 0;
                        order = eq;
                    }
                    break;
                case "eq":
                    eq = none(k.value) ? 1 : b == null ? 2 : b ? 1 : 0;
                    break;
                case "order":
                    order = none(k.value) ? -1 : b == null ? 3 : b ? 1 : 0;
                    break;
                case "repr":
                case "metadata":
                case "type":
                    break;
                default:
                    return "attr.ib() option " + k.arg + " is not supported";
            }
        }
        f.eq = eq;
        // An order key of its own (3), the eq key (an eq callable with order
        // True or unset), a plain comparison, or none.
        if (order == 3) {
            f.order = 3;
        } else if (order == -1) {
            f.order = eq;
        } else if (order == 1) {
            f.order = eq == 2 ? 2 : 1;
        } else {
            f.order = 0;
        }
        f.hashed = hash == null ? eq != 0 : hash;
        return null;
    }

    private static String value(Field f, int i, String x, int mode) {
        String v = x + "." + f.name;
        if (mode == 2) {
            return "_cn1_a[" + i + "].eq_key(" + v + ")";
        }
        if (mode == 3) {
            return "_cn1_a[" + i + "].order_key(" + v + ")";
        }
        return v;
    }

    private static boolean keyed(List<Field> all, boolean order) {
        for (Field f : all) {
            int mode = order ? f.order : f.eq;
            if (mode >= 2) {
                return true;
            }
        }
        return false;
    }

    private static String tuple(List<Field> all, String x, boolean order, boolean hash) {
        StringBuilder b = new StringBuilder("(");
        for (int i = 0; i < all.size(); i++) {
            Field f = all.get(i);
            int mode = order ? f.order : f.eq;
            if (hash ? !f.hashed : mode == 0) {
                continue;
            }
            b.append(value(f, i, x, hash ? f.eq == 2 ? 2 : 1 : mode)).append(", ");
        }
        return b.append(')').toString();
    }

    private static boolean hooked(Field f, boolean classHook, boolean define, boolean hooks) {
        return hooks && (f.onSet || classHook || define && (f.converter || f.validator));
    }

    private static String generate(String cls, Model m, boolean init, boolean repr, boolean eq, boolean order,
            Boolean hash, boolean classHook, boolean define, boolean hooks, Set<String> own, boolean kwOnlyClass,
            boolean matchArgs) {
        String table = cls + ".__attrs_attrs__";
        StringBuilder src = new StringBuilder();
        List<Field> all = m.all;
        if (init) {
            StringBuilder sig = new StringBuilder("def __init__(self");
            boolean defaulted = false;
            for (Field f : all) {
                if (f.init && !f.kwOnly) {
                    defaulted |= f.hasDefault;
                    sig.append(", ").append(f.alias).append(defaulted ? "=" + PRE + "NOTHING" : "");
                }
            }
            boolean star = false;
            for (Field f : all) {
                if (f.init && f.kwOnly) {
                    if (!star) {
                        sig.append(", *");
                        star = true;
                    }
                    sig.append(", ").append(f.alias).append(f.hasDefault ? "=" + PRE + "NOTHING" : "");
                }
            }
            src.append(sig).append("):\n    _cn1_a = ").append(table).append('\n');
            if (m.preInit) {
                src.append("    self.__attrs_pre_init__()\n");
            }
            for (int i = 0; i < all.size(); i++) {
                Field f = all.get(i);
                String v;
                if (f.init) {
                    v = f.alias;
                    if (f.hasDefault) {
                        src.append("    if ").append(v).append(" is ").append(PRE).append("NOTHING:\n        ")
                                .append(v).append(" = ").append(PRE).append("default(_cn1_a[").append(i)
                                .append("], self)\n");
                    }
                } else if (f.hasDefault) {
                    v = "_cn1_v";
                    src.append("    _cn1_v = ").append(PRE).append("default(_cn1_a[").append(i).append("], self)\n");
                } else {
                    continue;
                }
                if (f.converter) {
                    v = PRE + "convert(_cn1_a[" + i + "], self, " + v + ")";
                }
                if (m.frozen) {
                    src.append("    object.__setattr__(self, \"").append(f.name).append("\", ").append(v).append(")\n");
                } else if (hooked(f, classHook, define, hooks)) {
                    src.append("    self.").append(PRE).append("v_").append(f.name).append(" = ").append(v)
                            .append('\n');
                } else {
                    src.append("    self.").append(f.name).append(" = ").append(v).append('\n');
                }
            }
            StringBuilder val = new StringBuilder();
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).validator) {
                    val.append("        _cn1_a[").append(i).append("].validator(self, _cn1_a[").append(i)
                            .append("], self.").append(all.get(i).name).append(")\n");
                }
            }
            if (val.length() > 0) {
                src.append("    if ").append(PRE).append("validating():\n").append(val);
            }
            if (m.postInit) {
                src.append("    self.__attrs_post_init__()\n");
            }
        }
        for (int i = 0; i < all.size(); i++) {
            Field f = all.get(i);
            if (hooked(f, classHook, define, hooks) && !own.contains(f.name)) {
                m.hooked.add(f.name);
                src.append("__cn1_attrs_raw_").append(f.name).append("__ = True\n");
                String slot = "self." + PRE + "v_" + f.name;
                src.append("@property\ndef ").append(f.name).append("(self):\n    return ").append(slot).append('\n');
                src.append("@").append(f.name).append(".setter\ndef ").append(f.name).append("(self, _cn1_v):\n    ")
                        .append(slot).append(" = ").append(PRE).append("on_set(").append(table).append('[').append(i)
                        .append("], self, _cn1_v)\n");
            }
        }
        src.append("def __cn1_attrs_values__(self):\n    return (");
        for (Field f : all) {
            if (!f.init && !f.hasDefault) {
                src.append("getattr(self, \"").append(f.name).append("\", ").append(PRE).append("NOTHING), ");
            } else {
                src.append("self.").append(f.name).append(", ");
            }
        }
        src.append(")\n");
        if (repr) {
            src.append("def __repr__(self):\n    return ").append(PRE).append("repr(self, ").append(table)
                    .append(", self.__cn1_attrs_values__())\n");
        }
        if (eq) {
            src.append("def __eq__(self, other):\n    if other.__class__ is not self.__class__:\n")
                    .append("        return NotImplemented\n");
            if (keyed(all, false)) {
                src.append("    _cn1_a = ").append(table).append('\n');
            }
            src.append("    return ").append(tuple(all, "self", false, false)).append(" == ")
                    .append(tuple(all, "other", false, false)).append('\n');
            src.append("def __ne__(self, other):\n    r = self.__eq__(other)\n    if r is NotImplemented:\n")
                    .append("        return NotImplemented\n    return not r\n");
        }
        if (order) {
            for (String[] op : new String[][] {{"__lt__", "<"}, {"__le__", "<="}, {"__gt__", ">"}, {"__ge__", ">="}}) {
                src.append("def ").append(op[0]).append("(self, other):\n    if other.__class__ is self.__class__:\n");
                if (keyed(all, true)) {
                    src.append("        _cn1_a = ").append(table).append('\n');
                }
                src.append("        return ").append(tuple(all, "self", true, false)).append(' ').append(op[1])
                        .append(' ').append(tuple(all, "other", true, false)).append("\n    return NotImplemented\n");
            }
        }
        boolean genHash = hash != null ? hash : eq && m.frozen;
        if (genHash) {
            src.append("def __hash__(self):\n");
            if (keyed(all, false)) {
                src.append("    _cn1_a = ").append(table).append('\n');
            }
            String t = tuple(all, "self", false, true);
            src.append("    return hash((\"").append(cls).append("\", ").append(t.substring(1)).append(")\n");
        } else if (hash == null && eq) {
            src.append("__hash__ = None\n");
        }
        src.append("__cn1_attrs_lowered__ = ((");
        for (Field f : all) {
            src.append('"').append(f.name).append("\", ");
        }
        src.append("), (");
        for (Field f : m.own) {
            src.append("(\"").append(f.name).append("\", ").append(PRE).append(f.name).append("), ");
        }
        src.append("), ").append(kwOnlyClass ? "True" : "False").append(", ").append(m.frozen ? "True" : "False")
                .append(", None, ").append(define ? "True" : "False").append(", ").append(matchArgs ? "True" : "False")
                .append(")\n");
        return src.toString();
    }
}
