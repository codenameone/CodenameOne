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
import com.codename1.python.transpiler.ast.Node.BinOp;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.ImportFrom;
import com.codename1.python.transpiler.ast.Node.Keyword;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.ast.Node.Subscript;
import com.codename1.python.transpiler.ast.Node.TupleExpr;
import com.codename1.python.transpiler.parser.Parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The per-class code a SQLAlchemy declarative model gets, written into the
/// class at compile time as plain Python naming every attribute literally.
///
/// The bundled `sqlalchemy.orm` used to map a class with a metaclass that
/// read `__annotations__`, installed descriptors with `setattr` and kept the
/// instance state in `obj.__dict__[key]`: reflection, and no layout for any
/// model. Here every module-level `class M(Base)` whose base derives from
/// `DeclarativeBase` (or is `Base = declarative_base()`) is rewritten, as if
/// its author had written:
///
/// ```python
/// class User(Base):
///     __tablename__ = "users"
///     id = __cn1_sa__._sa_col(0, "id", "int", False, mapped_column(primary_key=True))
///     addresses = __cn1_sa__._sa_rel(1, "addresses", lambda: Address, True, relationship(...))
///     __cn1_sa_cols__ = (id,)
///     __cn1_sa_rels__ = (addresses,)
///     __cn1_sa_parent__ = None          # the mapped base, for inheritance
///     __cn1_sa_table__ = "users"        # None: single-table inheritance
///     __cn1_sa_args__ = None            # __mapper_args__
///     __cn1_sa_nslots__ = 2
///     def __cn1_sa_kw__(self, kw): ...  # keyword hook for a user __init__'s super() call
///     def __init__(self, *, id=__cn1_sa__._UNSET, addresses=__cn1_sa__._UNSET):
///         self._sa_new()
///         if id is not __cn1_sa__._UNSET:
///             self._sa_v[0] = id                       # a column: its slot, literally
///         if addresses is not __cn1_sa__._UNSET:
///             User.__cn1_sa_rels__[0].__set__(self, addresses)
/// User.__cn1_sa_map__()
/// __cn1_sa__._sa_pair(User.addresses, Address.user)   # back_populates
/// ```
///
/// Each column and relationship owns a slot number of the instance's state
/// list (`obj._sa_v`), fixed here: inherited attributes keep the base's slots
/// and a joined-inheritance primary key shares the inherited one. The
/// annotation is read here too (`Mapped[Optional[int]]` is type "int",
/// optional), a relationship's target is a lambda naming the class, and
/// `back_populates` and a string `order_by` are resolved against the classes
/// of the module.
///
/// What cannot be lowered exactly fails the build naming the construct: a
/// model not at module level, more than one base, class keywords, a
/// `__table__`, a non-literal `__tablename__`, a relationship whose target or
/// `back_populates` is not a mapped class or attribute of the module.
public final class SqlAlchemyLowering {

    private SqlAlchemyLowering() {
    }

    private static final String RT = "__cn1_sa__";

    private static final class Rel {
        final String owner;
        final String key;
        final String target;
        final String back;
        final Stmt at;

        Rel(String owner, String key, String target, String back, Stmt at) {
            this.owner = owner;
            this.key = key;
            this.target = target;
            this.back = back;
            this.at = at;
        }
    }

    private static final class Model {
        final String name;
        final Model parent;
        /// Every attribute key (inherited first) to its slot number.
        final Map<String, Integer> slots = new LinkedHashMap<String, Integer>();
        /// Every attribute key to the descriptor expression the keyword hook sets.
        final Map<String, String> setters = new LinkedHashMap<String, String>();
        final Set<String> rels = new HashSet<String>();
        int mapStmt;

        Model(String name, Model parent) {
            this.name = name;
            this.parent = parent;
            if (parent != null) {
                slots.putAll(parent.slots);
                setters.putAll(parent.setters);
                rels.addAll(parent.rels);
            }
        }
    }

    private static final class Ann {
        String type;
        boolean optional;
        Boolean list;
    }

    /// Lowers the declarative models of `tree`, the module at path `path`.
    public static void rewrite(Node.Module tree, String path) {
        if (path.endsWith("sqlalchemy/orm/__init__.py")) {
            return;
        }
        Set<String> declFn = new HashSet<String>();
        Set<String> baseFn = new HashSet<String>();
        Set<String> colFn = new HashSet<String>();
        Set<String> relFn = new HashSet<String>();
        Set<String> mappedFn = new HashSet<String>();
        for (Stmt s : tree.body) {
            if (!(s instanceof ImportFrom) || ((ImportFrom) s).level != 0) {
                continue;
            }
            String mod = ((ImportFrom) s).module;
            boolean orm = "sqlalchemy.orm".equals(mod) || "sqlalchemy.ext.declarative".equals(mod);
            if (!orm && !"sqlalchemy".equals(mod)) {
                continue;
            }
            for (Alias a : ((ImportFrom) s).names) {
                String bound = a.asname != null ? a.asname : a.name;
                if ("Column".equals(a.name)) {
                    colFn.add(bound);
                } else if (!orm) {
                    continue;
                } else if ("DeclarativeBase".equals(a.name) || "DeclarativeBaseNoMeta".equals(a.name)) {
                    baseFn.add(bound);
                } else if ("declarative_base".equals(a.name)) {
                    declFn.add(bound);
                } else if ("mapped_column".equals(a.name)) {
                    colFn.add(bound);
                } else if ("relationship".equals(a.name)) {
                    relFn.add(bound);
                } else if ("Mapped".equals(a.name) || "WriteOnlyMapped".equals(a.name)
                        || "DynamicMapped".equals(a.name)) {
                    mappedFn.add(bound);
                }
            }
        }
        for (Stmt s : tree.body) {
            if (s instanceof ClassDef) {
                for (Expr b : ((ClassDef) s).bases) {
                    if (b instanceof Attribute && ("DeclarativeBase".equals(((Attribute) b).attr)
                            || "DeclarativeBaseNoMeta".equals(((Attribute) b).attr))) {
                        throw error(path, s, "SQLAlchemy base class " + ((ClassDef) s).name + " names "
                                + Layouts.text(b) + "; write `from sqlalchemy.orm import DeclarativeBase` and"
                                + " derive from that name so the build can map its models");
                    }
                }
            }
        }
        if (baseFn.isEmpty() && declFn.isEmpty()) {
            return;
        }
        Set<String> bases = new HashSet<String>();
        Map<String, Model> models = new LinkedHashMap<String, Model>();
        List<Rel> rels = new ArrayList<Rel>();
        boolean any = false;
        // Bases and models whose instances run an __init__ the user wrote
        // (their own or an ancestor's here): they keep DeclarativeBase's
        // keyword constructor, which that __init__ reaches with super().
        Set<String> userInit = new HashSet<String>();
        for (int i = 0; i < tree.body.size(); i++) {
            Stmt s = tree.body.get(i);
            if (s instanceof Assign && ((Assign) s).value instanceof Call
                    && isFn(((Call) ((Assign) s).value).func, declFn)) {
                Assign a = (Assign) s;
                if (a.targets.size() != 1 || !(a.targets.get(0) instanceof Name)) {
                    throw error(path, s, "declarative_base() must be assigned to one name");
                }
                tree.body.set(i, declarativeBase(path, a, ((Name) a.targets.get(0)).id));
                bases.add(((Name) a.targets.get(0)).id);
                any = true;
                continue;
            }
            if (!(s instanceof ClassDef)) {
                continue;
            }
            ClassDef c = (ClassDef) s;
            if (c.bases.isEmpty()) {
                continue;
            }
            Expr b0 = c.bases.get(0);
            String b0n = b0 instanceof Name ? ((Name) b0).id : null;
            boolean direct = isFn(b0, baseFn);
            boolean onBase = b0n != null && bases.contains(b0n);
            Model parent = b0n != null ? models.get(b0n) : null;
            if (b0n != null && userInit.contains(b0n) || definesInit(c)) {
                userInit.add(c.name);
            }
            if (!direct && !onBase && parent == null) {
                for (Expr b : c.bases) {
                    if (isFn(b, baseFn) || b instanceof Name
                            && (bases.contains(((Name) b).id) || models.containsKey(((Name) b).id))) {
                        throw error(path, c, "SQLAlchemy model class " + c.name
                                + " must name its declarative base first");
                    }
                }
                continue;
            }
            any = true;
            if (c.bases.size() > 1) {
                throw error(path, c, "SQLAlchemy model class " + c.name + " has more than one base;"
                        + " its mapping cannot be generated (mixins and MappedAsDataclass are not supported)");
            }
            if (!c.keywords.isEmpty()) {
                throw error(path, c, "SQLAlchemy model class " + c.name + " takes class keywords;"
                        + " its mapping cannot be generated");
            }
            if (direct) {
                bases.add(c.name);
                if (!binds(c, "metadata")) {
                    addBody(c, path, "metadata = " + RT + ".MetaData()\n", c.line);
                }
                continue;
            }
            String table = null;
            for (Stmt x : c.body) {
                String n = boundName(x);
                if ("__table__".equals(n)) {
                    throw error(path, x, "SQLAlchemy model class " + c.name
                            + " assigns __table__; only __tablename__ mappings are supported");
                }
                if ("__tablename__".equals(n)) {
                    Expr v = x instanceof Assign ? ((Assign) x).value : ((AnnAssign) x).value;
                    if (!(v instanceof Constant) || ((Constant) v).kind != Constant.Kind.STR) {
                        throw error(path, x, "SQLAlchemy model class " + c.name
                                + " has a __tablename__ that is not a string literal");
                    }
                    table = ((Constant) v).text;
                }
            }
            if (parent == null && table == null) {
                // An intermediate base with no table: abstract, maps nothing.
                for (Stmt x : c.body) {
                    if (attrValue(x, colFn, relFn, mappedFn) != null) {
                        throw error(path, x, "SQLAlchemy model class " + c.name
                                + " declares mapped attributes but has no __tablename__");
                    }
                }
                bases.add(c.name);
                continue;
            }
            Model m = new Model(c.name, parent);
            lowerClass(c, path, m, table, colFn, relFn, mappedFn, rels, !userInit.contains(c.name));
            models.put(c.name, m);
            List<Stmt> after = Parser.parse(c.name + ".__cn1_sa_map__()\n", path).body;
            DataclassInit.atLine(after, c.line);
            tree.body.addAll(i + 1, after);
            i += after.size();
            m.mapStmt = i;
        }
        if (!any) {
            return;
        }
        refuseNested(tree.body, path, baseFn, bases, models, false);
        // back_populates, resolved here: each pair is linked once, after the
        // later of its two classes is mapped.
        Map<Integer, List<String>> pairs = new java.util.TreeMap<Integer, List<String>>(
                java.util.Collections.<Integer>reverseOrder());
        Set<String> done = new HashSet<String>();
        for (Rel r : rels) {
            Model t = models.get(r.target);
            if (t == null) {
                throw error(path, r.at, "relationship " + r.owner + "." + r.key + " targets " + r.target
                        + ", which is not a mapped class of this module");
            }
            if (r.back == null) {
                continue;
            }
            if (!t.rels.contains(r.back)) {
                throw error(path, r.at, "relationship " + r.owner + "." + r.key + " has back_populates=\""
                        + r.back + "\", but " + r.target + " has no relationship " + r.back);
            }
            String k1 = r.owner + "." + r.key;
            String k2 = r.target + "." + r.back;
            if (done.contains(k1 + "|" + k2) || done.contains(k2 + "|" + k1)) {
                continue;
            }
            done.add(k1 + "|" + k2);
            int at = Math.max(models.get(r.owner).mapStmt, t.mapStmt);
            List<String> l = pairs.get(at);
            if (l == null) {
                l = new ArrayList<String>();
                pairs.put(at, l);
            }
            l.add(RT + "._sa_pair(" + k1 + ", " + k2 + ")\n");
        }
        for (Map.Entry<Integer, List<String>> e : pairs.entrySet()) {
            StringBuilder b = new StringBuilder();
            for (String x : e.getValue()) {
                b.append(x);
            }
            List<Stmt> st = Parser.parse(b.toString(), path).body;
            DataclassInit.atLine(st, tree.body.get(e.getKey()).line);
            tree.body.addAll(e.getKey() + 1, st);
        }
        int at = 0;
        while (at < tree.body.size() && (tree.body.get(at) instanceof ImportFrom
                && "__future__".equals(((ImportFrom) tree.body.get(at)).module)
                || at == 0 && tree.body.get(0) instanceof Node.ExprStmt
                && ((Node.ExprStmt) tree.body.get(0)).value instanceof Constant)) {
            at++;
        }
        List<Stmt> imp = Parser.parse("import sqlalchemy.orm as " + RT + "\n", path).body;
        DataclassInit.atLine(imp, tree.body.isEmpty() ? 1 : tree.body.get(Math.min(at, tree.body.size() - 1)).line);
        tree.body.addAll(at, imp);
    }

    private static Stmt declarativeBase(String path, Assign a, String name) {
        Call call = (Call) a.value;
        Expr md = null;
        if (call.args.size() == 1 && call.keywords.isEmpty()) {
            md = call.args.get(0);
        } else if (call.args.isEmpty() && call.keywords.size() == 1 && "metadata".equals(call.keywords.get(0).arg)) {
            md = call.keywords.get(0).value;
        } else if (!call.args.isEmpty() || !call.keywords.isEmpty()) {
            throw error(path, a, "declarative_base() takes only metadata= here; its other options are not supported");
        }
        ClassDef c = (ClassDef) Parser.parse("class " + name + "(" + RT + ".DeclarativeBase):\n    metadata = "
                + RT + ".MetaData()\n", path).body.get(0);
        if (md != null) {
            c.body.set(0, new Assign(new ArrayList<Expr>(java.util.Collections.<Expr>singletonList(new Name("metadata"))),
                    md));
        }
        DataclassInit.atLine(c, a.line);
        return c;
    }

    /// The column or relationship value a class-body statement declares, or
    /// null when it declares neither. A bare `x: Mapped[int]` answers its
    /// annotation.
    private static Expr attrValue(Stmt s, Set<String> colFn, Set<String> relFn, Set<String> mappedFn) {
        Expr v;
        Expr ann = null;
        if (s instanceof AnnAssign && ((AnnAssign) s).target instanceof Name) {
            v = ((AnnAssign) s).value;
            ann = ((AnnAssign) s).annotation;
        } else if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name) {
            v = ((Assign) s).value;
        } else {
            return null;
        }
        if (v instanceof Call && (isFn(((Call) v).func, colFn) || isFn(((Call) v).func, relFn))) {
            return v;
        }
        if (ann != null && mappedInner(ann, mappedFn) != null) {
            return v != null ? v : ann;
        }
        return null;
    }

    private static void lowerClass(ClassDef c, String path, Model m, String table, Set<String> colFn,
            Set<String> relFn, Set<String> mappedFn, List<Rel> rels, boolean genInit) {
        List<String> cols = new ArrayList<String>();
        List<String> ownRels = new ArrayList<String>();
        boolean args = false;
        for (int i = 0; i < c.body.size(); i++) {
            Stmt s = c.body.get(i);
            if (s instanceof FunctionDef && "__init__".equals(((FunctionDef) s).name) && !superInitFirst((FunctionDef) s)) {
                // The base __init__ allocates the instance's state slots;
                // a constructor that skips it would store into nothing.
                throw error(path, s, "SQLAlchemy model " + c.name + " defines __init__ without calling"
                        + " super().__init__(...) first; its mapped state would never be allocated");
            }
            if ("__mapper_args__".equals(boundName(s))) {
                args = true;
            }
            Expr v = attrValue(s, colFn, relFn, mappedFn);
            if (v == null) {
                continue;
            }
            String key = boundName(s);
            if (key.startsWith("__") && !key.endsWith("__")) {
                throw error(path, s, "SQLAlchemy model " + c.name + " attribute " + key
                        + " is name-mangled; its mapping cannot name it");
            }
            Expr annExpr = s instanceof AnnAssign ? ((AnnAssign) s).annotation : null;
            Ann ann = annExpr != null ? analyze(c, path, s, mappedInner(annExpr, mappedFn)) : null;
            if (annExpr != null && ann == null && !(v instanceof Call)) {
                throw error(path, s, "SQLAlchemy model " + c.name + " attribute " + key
                        + " has an annotation the build cannot read");
            }
            Integer slot = m.slots.get(key);
            if (slot == null) {
                slot = m.slots.size();
                m.slots.put(key, slot);
            }
            StringBuilder src = new StringBuilder(key).append(" = ").append(RT);
            Call user = v instanceof Call ? (Call) v : null;
            if (user != null && isFn(user.func, relFn)) {
                String target = relTarget(c, path, s, user, ann);
                String back = null;
                for (int k = 0; k < user.keywords.size(); k++) {
                    Keyword kw = user.keywords.get(k);
                    if ("back_populates".equals(kw.arg)) {
                        if (!(kw.value instanceof Constant) || ((Constant) kw.value).kind != Constant.Kind.STR) {
                            throw error(path, s, "relationship " + c.name + "." + key
                                    + " has a back_populates that is not a string literal");
                        }
                        back = ((Constant) kw.value).text;
                    } else if ("order_by".equals(kw.arg) && kw.value instanceof Constant
                            && ((Constant) kw.value).kind == Constant.Kind.STR) {
                        user.keywords.set(k, new Keyword("order_by", thunk(path, s, ((Constant) kw.value).text)));
                    } else if ("primaryjoin".equals(kw.arg) || "secondaryjoin".equals(kw.arg)
                            || "backref".equals(kw.arg)) {
                        throw error(path, s, "relationship " + c.name + "." + key + " uses " + kw.arg
                                + ", which is not supported; declare both sides with back_populates");
                    }
                }
                rels.add(new Rel(c.name, key, target, back, s));
                m.rels.add(key);
                src.append("._sa_rel(").append(slot).append(", \"").append(key).append("\", lambda: ").append(target)
                        .append(", ").append(ann == null || ann.list == null ? "None" : ann.list ? "True" : "False")
                        .append(", None)\n");
                m.setters.put(key, c.name + ".__cn1_sa_rels__[" + ownRels.size() + "]");
                ownRels.add(key);
            } else {
                src.append("._sa_col(").append(slot).append(", \"").append(key).append("\", ")
                        .append(ann == null || ann.type == null ? "None" : DeclarativeMap.quote(ann.type))
                        .append(", ").append(ann != null && ann.optional ? "True" : "False").append(", None)\n");
                m.setters.put(key, c.name + ".__cn1_sa_cols__[" + cols.size() + "]");
                cols.add(key);
            }
            Assign repl = (Assign) Parser.parse(src.toString(), path).body.get(0);
            if (user != null) {
                Call made = (Call) repl.value;
                made.args.set(made.args.size() - 1, user);
            }
            DataclassInit.atLine(repl.targets, s.line);
            DataclassInit.atLine(repl.value, s.line);
            repl.line = s.line;
            c.body.set(i, repl);
        }
        StringBuilder b = new StringBuilder("__cn1_sa_cols__ = (");
        for (String k : cols) {
            b.append(k).append(", ");
        }
        b.append(")\n__cn1_sa_rels__ = (");
        for (String k : ownRels) {
            b.append(k).append(", ");
        }
        b.append(")\n__cn1_sa_parent__ = ").append(m.parent != null ? m.parent.name : "None");
        b.append("\n__cn1_sa_table__ = ").append(table != null ? DeclarativeMap.quote(table) : "None");
        b.append("\n__cn1_sa_args__ = ").append(args ? "__mapper_args__" : "None");
        b.append("\n__cn1_sa_nslots__ = ").append(m.slots.size());
        b.append("\n");
        b.append("def __cn1_sa_kw__(self, kw):\n    pass\n");
        for (Map.Entry<String, String> e : m.setters.entrySet()) {
            b.append("    if ").append(DeclarativeMap.quote(e.getKey())).append(" in kw:\n        ")
                    .append(e.getValue()).append(".__set__(self, kw.pop(").append(DeclarativeMap.quote(e.getKey()))
                    .append("))\n");
        }
        if (genInit && !m.setters.containsKey("self")) {
            // The constructor itself, keyword-only: each key a parameter, a
            // column stored into its slot literally, no keyword dict and no
            // lookup by string. An unknown keyword or a positional argument is
            // a TypeError from the call, as SQLAlchemy's constructor raises.
            String unset = RT + "._UNSET";
            b.append("def __init__(self");
            if (!m.setters.isEmpty()) {
                b.append(", *");
                for (String k : m.setters.keySet()) {
                    b.append(", ").append(k).append("=").append(unset);
                }
            }
            b.append("):\n    self._sa_new()\n");
            for (Map.Entry<String, String> e : m.setters.entrySet()) {
                String k = e.getKey();
                b.append("    if ").append(k).append(" is not ").append(unset).append(":\n        ");
                if (m.rels.contains(k)) {
                    b.append(e.getValue()).append(".__set__(self, ").append(k).append(")\n");
                } else {
                    b.append("self._sa_v[").append(m.slots.get(k)).append("] = ").append(k).append("\n");
                }
            }
        }
        addBody(c, path, b.toString(), c.body.isEmpty() ? c.line : c.body.get(c.body.size() - 1).line);
    }

    /// The class a relationship targets: its first argument (a class name or
    /// a string naming one), else its annotation's.
    private static String relTarget(ClassDef c, String path, Stmt s, Call user, Ann ann) {
        String target = null;
        if (!user.args.isEmpty()) {
            Expr a = user.args.get(0);
            if (a instanceof Name) {
                target = ((Name) a).id;
            } else if (a instanceof Constant && ((Constant) a).kind == Constant.Kind.STR
                    && ((Constant) a).text.matches("[A-Za-z_][A-Za-z_0-9]*")) {
                target = ((Constant) a).text;
            } else {
                throw error(path, s, "relationship " + c.name + "." + boundName(s)
                        + " names its target with an expression the build cannot read; write the class name");
            }
            user.args.remove(0);
            if (!user.args.isEmpty()) {
                throw error(path, s, "relationship " + c.name + "." + boundName(s)
                        + " takes more than one positional argument");
            }
        } else if (ann != null && ann.type != null) {
            target = ann.type;
        }
        if (target == null || !target.matches("[A-Za-z_][A-Za-z_0-9]*")) {
            throw error(path, s, "relationship " + c.name + "." + boundName(s)
                    + " has no target class the build can read; annotate it Mapped[\"Class\"]");
        }
        return target;
    }

    private static Expr thunk(String path, Stmt s, String text) {
        Assign a = (Assign) Parser.parse("_ = " + RT + "._Thunk(lambda: " + text + ")\n", path).body.get(0);
        DataclassInit.atLine(a.value, s.line);
        return a.value;
    }

    /// The `X` of `Mapped[X]` (or WriteOnlyMapped/DynamicMapped), a string
    /// annotation parsed; null when `ann` is not one.
    private static Expr mappedInner(Expr ann, Set<String> mappedFn) {
        if (ann instanceof Constant && ((Constant) ann).kind == Constant.Kind.STR) {
            ann = parseExpr(((Constant) ann).text);
            if (ann == null) {
                return null;
            }
        }
        if (ann instanceof Subscript) {
            Expr v = ((Subscript) ann).value;
            if (isFn(v, mappedFn) || v instanceof Attribute
                    && ("Mapped".equals(((Attribute) v).attr) || "WriteOnlyMapped".equals(((Attribute) v).attr)
                    || "DynamicMapped".equals(((Attribute) v).attr))) {
                return ((Subscript) ann).index;
            }
        }
        return null;
    }

    private static Expr parseExpr(String text) {
        try {
            Stmt s = Parser.parse("_ = " + text.trim() + "\n", "<annotation>").body.get(0);
            return s instanceof Assign ? ((Assign) s).value : null;
        } catch (PythonSyntaxError e) {
            return null;
        }
    }

    private static Ann analyze(ClassDef c, String path, Stmt at, Expr e) {
        if (e == null) {
            return null;
        }
        if (e instanceof Constant && ((Constant) e).kind == Constant.Kind.STR) {
            Expr p = parseExpr(((Constant) e).text);
            if (p == null) {
                throw error(path, at, "SQLAlchemy model " + c.name + " has an annotation the build cannot read: "
                        + ((Constant) e).text);
            }
            return analyze(c, path, at, p);
        }
        if (e instanceof BinOp && "|".equals(((BinOp) e).op)) {
            BinOp b = (BinOp) e;
            Expr rest = isNone(b.right) ? b.left : isNone(b.left) ? b.right : null;
            if (rest == null) {
                throw error(path, at, "SQLAlchemy model " + c.name + " has a union annotation the build cannot map");
            }
            Ann a = analyze(c, path, at, rest);
            a.optional = true;
            return a;
        }
        if (e instanceof Subscript) {
            Subscript s = (Subscript) e;
            String head = Layouts.text(s.value);
            String last = head.substring(head.lastIndexOf('.') + 1);
            if ("Optional".equals(last)) {
                Ann a = analyze(c, path, at, s.index);
                a.optional = true;
                return a;
            }
            if ("Union".equals(last) && s.index instanceof TupleExpr) {
                List<Expr> elts = ((TupleExpr) s.index).elts;
                Expr rest = null;
                boolean none = false;
                for (Expr x : elts) {
                    if (isNone(x)) {
                        none = true;
                    } else if (rest == null) {
                        rest = x;
                    } else {
                        throw error(path, at, "SQLAlchemy model " + c.name
                                + " has a union annotation the build cannot map");
                    }
                }
                Ann a = analyze(c, path, at, rest);
                a.optional = none;
                return a;
            }
            if ("List".equals(last) || "list".equals(last) || "Set".equals(last) || "set".equals(last)) {
                Ann a = analyze(c, path, at, s.index);
                a.list = Boolean.TRUE;
                return a;
            }
            throw error(path, at, "SQLAlchemy model " + c.name + " has an annotation the build cannot map: "
                    + Layouts.text(e));
        }
        if (e instanceof Name || e instanceof Attribute) {
            Ann a = new Ann();
            a.type = Layouts.text(e);
            a.list = Boolean.FALSE;
            return a;
        }
        throw error(path, at, "SQLAlchemy model " + c.name + " has an annotation the build cannot map");
    }

    private static boolean superInitFirst(FunctionDef f) {
        if (f.body.isEmpty() || !(f.body.get(0) instanceof Node.ExprStmt)) {
            return false;
        }
        Expr e = ((Node.ExprStmt) f.body.get(0)).value;
        if (!(e instanceof Call) || !(((Call) e).func instanceof Attribute)) {
            return false;
        }
        Attribute a = (Attribute) ((Call) e).func;
        return "__init__".equals(a.attr) && a.value instanceof Call && ((Call) a.value).func instanceof Name
                && "super".equals(((Name) ((Call) a.value).func).id);
    }

    private static boolean isNone(Expr e) {
        return e instanceof Constant && ((Constant) e).kind == Constant.Kind.NONE;
    }

    private static boolean isFn(Expr f, Set<String> names) {
        return f instanceof Name && names.contains(((Name) f).id);
    }

    private static String boundName(Stmt s) {
        if (s instanceof AnnAssign && ((AnnAssign) s).target instanceof Name) {
            return ((Name) ((AnnAssign) s).target).id;
        }
        if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name) {
            return ((Name) ((Assign) s).targets.get(0)).id;
        }
        return null;
    }

    private static boolean definesInit(ClassDef c) {
        for (Stmt s : c.body) {
            if (s instanceof FunctionDef && "__init__".equals(((FunctionDef) s).name)) {
                return true;
            }
        }
        return binds(c, "__init__");
    }

    private static boolean binds(ClassDef c, String name) {
        for (Stmt s : c.body) {
            if (name.equals(boundName(s))) {
                return true;
            }
        }
        return false;
    }

    private static void addBody(ClassDef c, String path, String src, int line) {
        for (Stmt s : Parser.parse(src, path).body) {
            DataclassInit.atLine(s, line);
            c.body.add(s);
        }
    }

    /// A model class or declarative_base() call not at module level is
    /// refused: its mapping would be generated with nothing to check it against.
    private static void refuseNested(Object o, String path, Set<String> baseFn, Set<String> bases,
            Map<String, Model> models, boolean nested) {
        if (o instanceof ClassDef) {
            ClassDef c = (ClassDef) o;
            if (nested && !c.bases.isEmpty()) {
                Expr b = c.bases.get(0);
                if (isFn(b, baseFn) || b instanceof Name
                        && (bases.contains(((Name) b).id) || models.containsKey(((Name) b).id))) {
                    throw error(path, c, "SQLAlchemy model class " + c.name
                            + " is not defined at module level; its mapping cannot be generated");
                }
            }
            for (Stmt s : c.body) {
                refuseNested(s, path, baseFn, bases, models, true);
            }
            return;
        }
        if (o instanceof FunctionDef) {
            for (Stmt s : ((FunctionDef) o).body) {
                refuseNested(s, path, baseFn, bases, models, true);
            }
            return;
        }
        if (o instanceof List) {
            for (Object x : (List<?>) o) {
                refuseNested(x, path, baseFn, bases, models, nested);
            }
        }
    }

    private static PythonSyntaxError error(String path, Node at, String why) {
        return new PythonSyntaxError(path, at.line, 1, why);
    }
}
