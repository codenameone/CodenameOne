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
import com.codename1.python.transpiler.ast.Node.AnnAssign;
import com.codename1.python.transpiler.ast.Node.Assign;
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.ExprStmt;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.ImportFrom;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Pass;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.ast.Node.Subscript;
import com.codename1.python.transpiler.parser.Parser;

import java.util.ArrayList;
import java.util.List;

/// Writes named tuple classes out as class statements at build time.
///
/// A base built by a call, `class Version(namedtuple("Version", [...]))`,
/// and a `class TreeForms(typing.NamedTuple)` body of annotated fields are
/// both classes the runtime makes with `type(...)`, so the planner can give
/// neither a layout and every subclass is generic. When the type name and
/// the fields are literals, this pass writes the class `namedtuple` would
/// make as Python source (`__new__`, `_make`, `_replace`, `_asdict`,
/// `__repr__`, `__getnewargs__`, one read-only property per field, a
/// `tuple` base and empty `__slots__`), as CPython's own generated code
/// does. The call base becomes a class statement named `_cn1nt_<class>`
/// placed just before its subclass; a `NamedTuple` class keeps its name,
/// docstring and annotations and gets `tuple` as its base, which is the
/// base CPython gives it too. Anything else -- defaults, `rename`, a
/// computed name, a body with methods -- is left as it is.
public final class NamedTupleLowering {

    private NamedTupleLowering() {
    }

    /// Lowers the named tuple classes of module `m` (from `file`).
    public static void rewrite(Node.Module m, String file) {
        boolean nt = imported(m, "collections", "namedtuple");
        boolean typed = imported(m, "typing", "NamedTuple");
        boolean td = imported(m, "typing", "TypedDict") || imported(m, "typing_extensions", "TypedDict");
        boolean mod = moduleImported(m, "typing");
        if (!nt && !typed && !td && !mod) {
            return;
        }
        List<Stmt> out = new ArrayList<Stmt>();
        boolean changed = false;
        for (Stmt s : m.body) {
            if (s instanceof ClassDef && ((ClassDef) s).bases.size() == 1 && (((ClassDef) s).keywords.isEmpty()
                    || (td || mod) && totalOnly((ClassDef) s))) {
                ClassDef c = (ClassDef) s;
                Expr b = c.bases.get(0);
                List<String> fields = nt ? callFields(b) : null;
                if (fields != null) {
                    String base = "_cn1nt_" + c.name;
                    Node.Module gen = Parser.parse(source(base, str(((Call) b).args.get(0)), fields, null, null), file);
                    DataclassInit.atLine(gen, c.line);
                    out.addAll(gen.body);
                    Node.Module ref = Parser.parse(base + "\n", file);
                    DataclassInit.atLine(ref, c.line);
                    c.bases.set(0, ((ExprStmt) ref.body.get(0)).value);
                    changed = true;
                } else if (c.decorators.isEmpty() && (td && b instanceof Name && "TypedDict".equals(((Name) b).id)
                        || mod && typingAttr(b, "TypedDict"))
                        && typedDict(c, b instanceof Name ? "TypedDict" : "typing.TypedDict", futureAnnotations(m), file, out)) {
                    changed = true;
                    continue;
                } else if ((typed && b instanceof Name && "NamedTuple".equals(((Name) b).id)
                        || mod && typingAttr(b, "NamedTuple")) && c.decorators.isEmpty()) {
                    java.util.Map<String, String> defaults = new java.util.LinkedHashMap<String, String>();
                    List<String> own = annotatedFields(c, defaults);
                    if (own != null) {
                        Node.Module gen = Parser.parse(source(c.name, c.name, own, c, defaults), file);
                        DataclassInit.atLine(gen, c.line);
                        ClassDef g = (ClassDef) gen.body.get(0);
                        // The docstring and the annotations first, as written.
                        List<Stmt> body = new ArrayList<Stmt>();
                        for (Stmt k : c.body) {
                            if (k instanceof AnnAssign && ((AnnAssign) k).value != null) {
                                // The default moved into __new__; a class attribute would only be
                                // shadowed by the field's property.
                                AnnAssign aa = (AnnAssign) k;
                                AnnAssign bare = new AnnAssign(aa.target, aa.annotation, null);
                                bare.line = aa.line;
                                body.add(bare);
                            } else if (!(k instanceof Pass)) {
                                body.add(k);
                            }
                        }
                        body.addAll(g.body);
                        c.body.clear();
                        c.body.addAll(body);
                        c.bases.set(0, g.bases.get(0));
                        changed = true;
                    }
                }
            }
            out.add(s);
        }
        if (changed) {
            m.body.clear();
            m.body.addAll(out);
        }
    }

    /// Whether `b` is `typing.<attr>` through `import typing` ([#moduleImported]).
    private static boolean typingAttr(Expr b, String attr) {
        return b instanceof Attribute && attr.equals(((Attribute) b).attr) && ((Attribute) b).value instanceof Name
                && "typing".equals(((Name) ((Attribute) b).value).id);
    }

    /// Whether `import module` binds `module` once, and nothing else in the
    /// module's top level binds that name.
    private static boolean moduleImported(Node.Module m, String module) {
        int found = 0;
        int other = 0;
        for (Stmt s : m.body) {
            if (s instanceof Node.Import) {
                for (Node.Alias a : ((Node.Import) s).names) {
                    String as = a.asname != null ? a.asname : a.name.split("\\.")[0];
                    if (as.equals(module)) {
                        if (a.asname == null && a.name.equals(module)) {
                            found++;
                        } else {
                            other++;
                        }
                    }
                }
            } else if (s instanceof ImportFrom) {
                for (Node.Alias a : ((ImportFrom) s).names) {
                    other += "*".equals(a.name) || module.equals(a.asname != null ? a.asname : a.name) ? 1 : 0;
                }
            } else if (s instanceof Assign) {
                for (Expr t : ((Assign) s).targets) {
                    other += t instanceof Name && ((Name) t).id.equals(module) ? 1 : 0;
                }
            } else if (s instanceof FunctionDef && ((FunctionDef) s).name.equals(module)
                    || s instanceof ClassDef && ((ClassDef) s).name.equals(module)) {
                other++;
            }
        }
        return found == 1 && other == 0;
    }

    /// Whether the class statement's only keyword is `total=True|False`.
    private static boolean totalOnly(ClassDef c) {
        return c.keywords.size() == 1 && "total".equals(c.keywords.get(0).arg)
                && c.keywords.get(0).value instanceof Constant
                && (((Constant) c.keywords.get(0).value).kind == Constant.Kind.TRUE
                || ((Constant) c.keywords.get(0).value).kind == Constant.Kind.FALSE);
    }

    private static boolean futureAnnotations(Node.Module m) {
        for (Stmt s : m.body) {
            if (s instanceof ImportFrom && "__future__".equals(((ImportFrom) s).module)) {
                for (Node.Alias a : ((ImportFrom) s).names) {
                    if ("annotations".equals(a.name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /// Lowers `class X(TypedDict[, total=B]):` whose body is only
    /// annotations without values (and `pass`) to the functional form
    /// `X = TypedDict("X", {"k": <annotation>, ...}, total=B)`, which makes
    /// the very same object: a `_TypedDictMeta` class whose call answers a
    /// plain dict, with `__annotations__`, `__total__` and the key sets. Each
    /// annotation is evaluated where the class body would have evaluated it
    /// (module scope), or kept as its source text under `from __future__
    /// import annotations`, as CPython keeps it. A docstring, a method, a
    /// base other than `TypedDict` or an annotation this pass cannot write
    /// back as source leaves the class as it is.
    private static boolean typedDict(ClassDef c, String fn, boolean future, String file, List<Stmt> out) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.name).append(" = ").append(fn).append("(\"").append(c.name).append("\", {");
        boolean any = false;
        for (Stmt s : c.body) {
            if (s instanceof Pass) {
                continue;
            }
            if (!(s instanceof AnnAssign) || ((AnnAssign) s).value != null || !(((AnnAssign) s).target instanceof Name)) {
                return false;
            }
            String ann = unparse(((AnnAssign) s).annotation);
            if (ann == null) {
                return false;
            }
            String key = pyStr(((Name) ((AnnAssign) s).target).id);
            String val = future ? pyStr(ann) : ann;
            if (key == null || val == null) {
                return false;
            }
            sb.append(any ? ", " : "").append(key).append(": ").append(val);
            any = true;
        }
        sb.append("}");
        if (!c.keywords.isEmpty()) {
            sb.append(", total=").append(((Constant) c.keywords.get(0).value).kind == Constant.Kind.TRUE
                    ? "True" : "False");
        }
        sb.append(")\n");
        Node.Module gen = Parser.parse(sb.toString(), file);
        DataclassInit.atLine(gen, c.line);
        out.addAll(gen.body);
        return true;
    }

    private static String pyStr(String v) {
        StringBuilder sb = new StringBuilder("'");
        for (int i = 0; i < v.length(); i++) {
            char ch = v.charAt(i);
            if (ch == '\\' || ch == '\'') {
                sb.append('\\');
            }
            if (ch < 32 || ch > 126) {
                return null;
            }
            sb.append(ch);
        }
        return sb.append('\'').toString();
    }

    /// Source text of a type annotation built from names, attributes,
    /// subscripts, tuples, lists, constants and `|`; null for anything else.
    private static String unparse(Expr e) {
        if (e instanceof Name) {
            return ((Name) e).id;
        }
        if (e instanceof Attribute) {
            String v = unparse(((Attribute) e).value);
            return v == null ? null : v + "." + ((Attribute) e).attr;
        }
        if (e instanceof Subscript) {
            String v = unparse(((Subscript) e).value);
            Expr ix = ((Subscript) e).index;
            String i = ix instanceof Node.TupleExpr ? elts(((Node.TupleExpr) ix).elts) : unparse(ix);
            return v == null || i == null ? null : v + "[" + i + "]";
        }
        if (e instanceof Node.TupleExpr) {
            String i = elts(((Node.TupleExpr) e).elts);
            return i == null ? null : "(" + i + (((Node.TupleExpr) e).elts.size() == 1 ? ",)" : ")");
        }
        if (e instanceof Node.ListExpr) {
            String i = elts(((Node.ListExpr) e).elts);
            return i == null ? null : "[" + i + "]";
        }
        if (e instanceof Node.BinOp && "|".equals(((Node.BinOp) e).op)) {
            String l = unparse(((Node.BinOp) e).left);
            String r = unparse(((Node.BinOp) e).right);
            return l == null || r == null ? null : l + " | " + r;
        }
        if (e instanceof Constant) {
            Constant k = (Constant) e;
            switch (k.kind) {
                case NONE:
                    return "None";
                case ELLIPSIS:
                    return "...";
                case STR:
                    return pyStr(k.text);
                default:
                    return null;
            }
        }
        return null;
    }

    private static String elts(List<Expr> es) {
        StringBuilder sb = new StringBuilder();
        for (Expr x : es) {
            String u = unparse(x);
            if (u == null) {
                return null;
            }
            sb.append(sb.length() == 0 ? "" : ", ").append(u);
        }
        return sb.toString();
    }

    /// Whether `from module import name` binds `name` and nothing else in
    /// the module's top level does.
    private static boolean imported(Node.Module m, String module, String name) {
        int from = 0;
        int other = 0;
        for (Stmt s : m.body) {
            if (s instanceof ImportFrom) {
                for (Node.Alias a : ((ImportFrom) s).names) {
                    String as = a.asname != null ? a.asname : a.name;
                    if ("*".equals(a.name)) {
                        other++;
                    } else if (as.equals(name)) {
                        if (module.equals(((ImportFrom) s).module) && ((ImportFrom) s).level == 0 && a.name.equals(name)) {
                            from++;
                        } else {
                            other++;
                        }
                    }
                }
            } else if (s instanceof Assign) {
                for (Expr t : ((Assign) s).targets) {
                    other += t instanceof Name && ((Name) t).id.equals(name) ? 1 : 0;
                }
            } else if (s instanceof FunctionDef && ((FunctionDef) s).name.equals(name)
                    || s instanceof ClassDef && ((ClassDef) s).name.equals(name)) {
                other++;
            }
        }
        return from == 1 && other == 0;
    }

    /// The fields of `namedtuple("Name", <literal fields>)`, or null.
    private static List<String> callFields(Expr b) {
        if (!(b instanceof Call) || !(((Call) b).func instanceof Name)
                || !"namedtuple".equals(((Name) ((Call) b).func).id)) {
            return null;
        }
        Call c = (Call) b;
        if (c.args.size() != 2 || !c.keywords.isEmpty() || str(c.args.get(0)) == null
                || !ident(str(c.args.get(0)))) {
            return null;
        }
        Expr f = c.args.get(1);
        List<String> out = new ArrayList<String>();
        if (str(f) != null) {
            for (String p : str(f).replace(',', ' ').trim().split("\\s+")) {
                out.add(p);
            }
        } else if (f instanceof Node.ListExpr || f instanceof Node.TupleExpr) {
            for (Expr e : f instanceof Node.ListExpr ? ((Node.ListExpr) f).elts : ((Node.TupleExpr) f).elts) {
                if (str(e) == null) {
                    return null;
                }
                out.add(str(e));
            }
        } else {
            return null;
        }
        return valid(out) ? out : null;
    }

    /// The fields of a `NamedTuple` body holding a docstring, `pass`,
    /// annotations (trailing ones may carry a literal default, recorded in
    /// `defaults` as source) and methods that do not replace a generated
    /// member, or null.
    private static List<String> annotatedFields(ClassDef c, java.util.Map<String, String> defaults) {
        List<String> out = new ArrayList<String>();
        List<String> methods = new ArrayList<String>();
        for (int i = 0; i < c.body.size(); i++) {
            Stmt s = c.body.get(i);
            if (s instanceof AnnAssign && ((AnnAssign) s).target instanceof Name) {
                String f = ((Name) ((AnnAssign) s).target).id;
                Expr v = ((AnnAssign) s).value;
                if (v == null) {
                    if (!defaults.isEmpty()) {
                        return null;
                    }
                } else {
                    String lit = literal(v);
                    if (lit == null) {
                        return null;
                    }
                    defaults.put(f, lit);
                }
                out.add(f);
            } else if (s instanceof FunctionDef) {
                String n = ((FunctionDef) s).name;
                if (GENERATED.contains(n) || n.startsWith("_") && !(n.startsWith("__") && n.endsWith("__"))) {
                    return null;
                }
                methods.add(n);
            } else if (!(s instanceof Pass) && !(i == 0 && s instanceof ExprStmt && str(((ExprStmt) s).value) != null)) {
                return null;
            }
        }
        for (String m : methods) {
            if (out.contains(m)) {
                return null;
            }
        }
        return valid(out) ? out : null;
    }

    private static final java.util.Set<String> GENERATED = new java.util.HashSet<String>(java.util.Arrays.asList(
            "__new__", "__init__", "__repr__", "__getnewargs__", "__slots__", "__setattr__", "__getattr__",
            "__getattribute__", "__delattr__", "__init_subclass__", "__class_getitem__"));

    /// The source of an immutable literal default, or null.
    private static String literal(Expr e) {
        if (!(e instanceof Constant)) {
            return null;
        }
        Constant k = (Constant) e;
        switch (k.kind) {
            case NONE:
                return "None";
            case TRUE:
                return "True";
            case FALSE:
                return "False";
            case INT:
            case FLOAT:
                return k.text;
            case STR:
                for (int i = 0; i < k.text.length(); i++) {
                    char ch = k.text.charAt(i);
                    if (ch < 32 || ch > 126 || ch == '\'' || ch == '\\') {
                        return null;
                    }
                }
                return "'" + k.text + "'";
            default:
                return null;
        }
    }

    /// Field names `namedtuple` accepts without `rename`.
    private static boolean valid(List<String> fields) {
        if (fields.isEmpty()) {
            return false;
        }
        for (int i = 0; i < fields.size(); i++) {
            String f = fields.get(i);
            if (!ident(f) || f.startsWith("_") || fields.indexOf(f) != i || KEYWORDS.contains(f)) {
                return false;
            }
        }
        return true;
    }

    private static final java.util.Set<String> KEYWORDS = new java.util.HashSet<String>(java.util.Arrays.asList(
            "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", "continue", "def",
            "del", "elif", "else", "except", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda",
            "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield", "self", "cls",
            "tuple", "property", "dict", "zip", "len", "list", "TypeError", "classmethod"));

    private static String str(Expr e) {
        return e instanceof Constant && ((Constant) e).kind == Constant.Kind.STR ? ((Constant) e).text : null;
    }

    private static boolean ident(String s) {
        if (s == null || s.isEmpty() || !Character.isJavaIdentifierStart(s.charAt(0)) || s.charAt(0) == '$') {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch > 127 || !(Character.isLetterOrDigit(ch) || ch == '_')) {
                return false;
            }
        }
        return true;
    }

    /// The class `namedtuple(typename, fields)` makes, as source, named
    /// `cls`; the docstring is left out when `keep` already has its own.
    private static String source(String cls, String typename, List<String> fields, ClassDef keep,
            java.util.Map<String, String> defaults) {
        int n = fields.size();
        StringBuilder a = new StringBuilder();
        StringBuilder q = new StringBuilder();
        StringBuilder r = new StringBuilder();
        StringBuilder sig = new StringBuilder();
        StringBuilder fd = new StringBuilder();
        for (int i = 0; i < n; i++) {
            String f = fields.get(i);
            a.append(i == 0 ? "" : ", ").append(f);
            String d = defaults == null ? null : defaults.get(f);
            sig.append(i == 0 ? "" : ", ").append(f);
            if (d != null) {
                sig.append('=').append(d);
                fd.append(fd.length() == 0 ? "" : ", ").append('\'').append(f).append("': ").append(d);
            }
            q.append('\'').append(f).append("', ");
            r.append(i == 0 ? "" : ", ").append(f).append("=%r");
        }
        StringBuilder s = new StringBuilder();
        s.append("class ").append(cls).append("(tuple):\n");
        boolean doc = keep != null && !keep.body.isEmpty() && keep.body.get(0) instanceof ExprStmt;
        if (!doc) {
            s.append("    \"").append(typename).append('(').append(a).append(")\"\n");
        }
        s.append("    __slots__ = ()\n");
        s.append("    _fields = (").append(q).append(")\n");
        s.append("    _field_defaults = {").append(fd).append("}\n");
        s.append("    __match_args__ = (").append(q).append(")\n");
        s.append("    def __new__(_cls, ").append(sig).append("):\n");
        s.append("        return tuple.__new__(_cls, (").append(a).append(",))\n");
        s.append("    @classmethod\n    def _make(cls, iterable):\n        result = tuple.__new__(cls, iterable)\n");
        s.append("        if len(result) != ").append(n).append(":\n");
        s.append("            raise TypeError(\"Expected ").append(n).append(" arguments, got %d\" % len(result))\n");
        s.append("        return result\n");
        s.append("    def _replace(self, /, **kwds):\n");
        s.append("        result = self._make([kwds.pop(name) if name in kwds else v for name, v in zip(self._fields, self)])\n");
        s.append("        if kwds:\n            raise TypeError(\"Got unexpected field names: %r\" % list(kwds))\n");
        s.append("        return result\n");
        s.append("    def __repr__(self):\n        return self.__class__.__name__ + \"(").append(r).append(")\" % self\n");
        s.append("    def _asdict(self):\n        return dict(zip(self._fields, self))\n");
        s.append("    def __getnewargs__(self):\n        return tuple(self)\n");
        for (int i = 0; i < n; i++) {
            s.append("    @property\n    def ").append(fields.get(i)).append("(self):\n");
            s.append("        \"Alias for field number ").append(i).append("\"\n");
            s.append("        return self[").append(i).append("]\n");
        }
        if (!cls.equals(typename)) {
            // The synthesized base answers the name namedtuple() was given, as CPython's does.
            s.append(cls).append(".__name__ = '").append(typename).append("'\n");
            s.append(cls).append(".__qualname__ = '").append(typename).append("'\n");
        }
        return s.toString();
    }
}
