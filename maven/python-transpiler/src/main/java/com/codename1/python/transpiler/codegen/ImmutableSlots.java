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
import com.codename1.python.transpiler.ast.Node.AnnAssign;
import com.codename1.python.transpiler.ast.Node.Assign;
import com.codename1.python.transpiler.ast.Node.AugAssign;
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Comp;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.ExprStmt;
import com.codename1.python.transpiler.ast.Node.For;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.If;
import com.codename1.python.transpiler.ast.Node.Keyword;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Raise;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.ast.Node.TupleExpr;
import com.codename1.python.transpiler.ast.Node.While;
import com.codename1.python.transpiler.parser.Parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Lowers soupsieve-style immutable slot classes at build time.
///
/// ```python
/// class Immutable:
///     __slots__ = ('_hash',)
///     def __init__(self, **kwargs):
///         ... super().__setattr__(k, v) for every keyword, then _hash ...
///     def __setattr__(self, name, value):
///         raise AttributeError(...)
/// class SelectorTag(Immutable):
///     __slots__ = ("name", "prefix", "_hash")
///     def __init__(self, name, prefix):
///         super().__init__(name=name, prefix=prefix)
/// ```
///
/// The root stores keyword names through `__setattr__` and reads the slots
/// back with `getattr` over `__slots__` in `__eq__`, `__repr__` and the
/// pickle reducer: reflection, and the bound `__setattr__` leaves every
/// class without a layout. Here each subclass's `super().__init__(...)`
/// becomes the stores it makes, by name and in the call's order, followed by
/// `_hash` computed exactly as the root computes it; each subclass gains
/// `_cn1_names` and `_cn1_values`, which name and read its slots directly;
/// the root's `__eq__`, `__repr__` and the module's `getattr` over
/// `__slots__` read those instead; and the root's `__init__` and
/// `__setattr__` are dropped.
///
/// Immutability is then the compiler's to enforce: an attribute store on
/// `self` in any method of these classes other than the generated
/// constructors, or a store anywhere of a slot name that no other class
/// stores as its own field, is a build error.
///
/// Anything outside that shape leaves every class alone.
public final class ImmutableSlots {
    private final List<String> report = new ArrayList<String>();

    private ImmutableSlots() {
    }

    /// Lowers the immutable slot classes among `trees` (by path) and answers
    /// what it did.
    public static List<String> rewrite(Map<String, Node.Module> trees) {
        ImmutableSlots d = new ImmutableSlots();
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            if (e.getKey().endsWith(".py")) {
                d.module(e.getKey(), e.getValue(), trees);
            }
        }
        return new ArrayList<String>(d.report);
    }

    private void module(String path, Node.Module m, Map<String, Node.Module> trees) {
        for (Stmt s : m.body) {
            if (s instanceof ClassDef && isRoot((ClassDef) s)) {
                lower(path, m, (ClassDef) s, trees);
                return;
            }
        }
    }

    private static boolean isRoot(ClassDef c) {
        if (!c.bases.isEmpty() || !c.keywords.isEmpty() || !c.decorators.isEmpty()) {
            return false;
        }
        List<String> slots = slots(c);
        if (slots == null || slots.size() != 1 || !"_hash".equals(slots.get(0))) {
            return false;
        }
        FunctionDef set = method(c, "__setattr__");
        FunctionDef init = method(c, "__init__");
        return set != null && init != null && init.args.kwarg != null && init.args.positional.size() == 1
                && raisesOnly(set) && method(c, "__eq__") != null && method(c, "__repr__") != null
                && INIT_BODY.equals(ConstArgSpecialize.dump(init.body));
    }

    /// soupsieve's `Immutable.__init__` body, structurally. The subclasses'
    /// generated constructors compute `_hash` the way it does, so a class of
    /// the same shape whose constructor does anything else is not lowered
    /// (and its computed stores then fail the build instead).
    private static final String INIT_BODY = ConstArgSpecialize.dump(((FunctionDef) Parser.parse(
            "def __init__(self, **kwargs):\n"
            + "    temp = []\n"
            + "    for k, v in kwargs.items():\n"
            + "        temp.append(type(v))\n"
            + "        temp.append(v)\n"
            + "        super().__setattr__(k, v)\n"
            + "    super().__setattr__('_hash', hash(tuple(temp)))\n", "<immutable>").body.get(0)).body);

    /// Whether `f` reads an attribute by a computed name: soupsieve's
    /// `__eq__`/`__repr__` walk `__slots__` that way. One that does not is the
    /// class's own and is kept.
    /// Whether `o` reads `.name` anywhere: soupsieve's `__eq__` compares
    /// `_hash` and walks `__slots__`; one that does not is not replaced.
    private static boolean readsAttr(Object o, String name) {
        if (o instanceof List) {
            for (Object x : (List<?>) o) {
                if (readsAttr(x, name)) {
                    return true;
                }
            }
            return false;
        }
        if (o instanceof Attribute && name.equals(((Attribute) o).attr)) {
            return true;
        }
        if (!(o instanceof Node)) {
            return false;
        }
        for (Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (java.lang.reflect.Field fl : c.getDeclaredFields()) {
                Class<?> t = fl.getType();
                if (java.lang.reflect.Modifier.isStatic(fl.getModifiers())
                        || !Node.class.isAssignableFrom(t) && !List.class.isAssignableFrom(t)) {
                    continue;
                }
                try {
                    if (readsAttr(fl.get(o), name)) {
                        return true;
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return false;
    }

    private static boolean readsComputed(FunctionDef f) {
        return readsComputed((Object) f.body);
    }

    private static boolean readsComputed(Object o) {
        if (o instanceof List) {
            for (Object x : (List<?>) o) {
                if (readsComputed(x)) {
                    return true;
                }
            }
            return false;
        }
        if (!(o instanceof Node)) {
            return false;
        }
        if (o instanceof Call && ((Call) o).func instanceof Name && "getattr".equals(((Name) ((Call) o).func).id)
                && ((Call) o).args.size() >= 2 && !(((Call) o).args.get(1) instanceof Constant)) {
            return true;
        }
        for (Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (java.lang.reflect.Field fl : c.getDeclaredFields()) {
                Class<?> t = fl.getType();
                if (java.lang.reflect.Modifier.isStatic(fl.getModifiers())
                        || !Node.class.isAssignableFrom(t) && !List.class.isAssignableFrom(t)) {
                    continue;
                }
                try {
                    if (readsComputed(fl.get(o))) {
                        return true;
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return false;
    }

    private static boolean raisesOnly(FunctionDef f) {
        for (Stmt s : f.body) {
            if (!(s instanceof Raise) && !(s instanceof ExprStmt && ((ExprStmt) s).value instanceof Constant)) {
                return false;
            }
        }
        return true;
    }

    private void lower(String path, Node.Module rootModule, ClassDef root, Map<String, Node.Module> trees) {
        Map<ClassDef, String> subs = new LinkedHashMap<ClassDef, String>();
        Set<String> subNames = new HashSet<String>();
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            for (Stmt s : e.getValue().body) {
                if (s instanceof ClassDef && extendsName((ClassDef) s, root.name)) {
                    subs.put((ClassDef) s, e.getKey());
                    subNames.add(((ClassDef) s).name);
                }
            }
        }
        Map<ClassDef, Call> supers = new IdentityHashMap<ClassDef, Call>();
        for (Map.Entry<ClassDef, String> e : subs.entrySet()) {
            String why = shape(e.getKey(), supers);
            if (why != null) {
                report.add("left " + root.name + " alone: " + e.getKey().name + " " + why);
                return;
            }
        }
        for (Node.Module m : trees.values()) {
            for (Stmt s : m.body) {
                if (s instanceof ClassDef && !subs.containsKey(s)) {
                    for (String n : subNames) {
                        if (extendsName((ClassDef) s, n)) {
                            report.add("left " + root.name + " alone: " + ((ClassDef) s).name + " extends " + n);
                            return;
                        }
                    }
                }
            }
        }
        FunctionDef pickler = null;
        int[] at = null;
        for (Stmt s : rootModule.body) {
            if (s instanceof FunctionDef) {
                int[] where = slotsComp(((FunctionDef) s).body);
                if (where.length > 0) {
                    if (pickler != null) {
                        report.add("left " + root.name + " alone: two module functions read __slots__");
                        return;
                    }
                    pickler = (FunctionDef) s;
                    at = where;
                }
            }
        }
        Set<String> slotNames = new HashSet<String>();
        for (Map.Entry<ClassDef, String> e : subs.entrySet()) {
            ClassDef c = e.getKey();
            slotNames.addAll(slots(c));
            Call sup = supers.get(c);
            FunctionDef init = method(c, "__init__");
            init.dataclassInit = true;
            replaceSuper(init.body, sup, e.getValue());
            List<String> names = slots(c);
            names = names.subList(0, names.size() - 1);
            StringBuilder b = new StringBuilder();
            b.append("def _cn1_names(self) -> tuple:\n    return (");
            for (String n : names) {
                b.append('\'').append(n).append("', ");
            }
            b.append(")\n");
            b.append("def _cn1_values(self) -> tuple:\n    return (");
            for (String n : names) {
                b.append("self.").append(n).append(", ");
            }
            b.append(")\n");
            Node.Module gen = Parser.parse(b.toString(), e.getValue());
            DataclassInit.atLine(gen, c.line);
            c.body.addAll(gen.body);
        }
        rewriteRoot(path, root);
        if (pickler != null) {
            rewritePickler(path, pickler, at);
        }
        Set<ClassDef> all = new HashSet<ClassDef>(subs.keySet());
        all.add(root);
        enforce(trees, all, slotNames);
        report.add("lowered " + root.name + " and " + subs.size() + " subclasses");
    }

    private static boolean extendsName(ClassDef c, String name) {
        if (c.bases.size() != 1) {
            return false;
        }
        Expr b = c.bases.get(0);
        return b instanceof Name && name.equals(((Name) b).id)
                || b instanceof Attribute && name.equals(((Attribute) b).attr) && ((Attribute) b).value instanceof Name;
    }

    /// Why `c` cannot be lowered, or null; records its `super().__init__`.
    private static String shape(ClassDef c, Map<ClassDef, Call> supers) {
        if (!c.keywords.isEmpty() || !c.decorators.isEmpty()) {
            return "has class keywords or decorators";
        }
        List<String> s = slots(c);
        if (s == null || s.isEmpty() || !"_hash".equals(s.get(s.size() - 1))) {
            return "has no literal __slots__ ending in _hash";
        }
        // Its own __repr__ is its own business; the root's reads nested
        // instances through _cn1_names and _cn1_values either way.
        if (method(c, "__setattr__") != null || method(c, "__eq__") != null || method(c, "__ne__") != null) {
            return "overrides a lowered method";
        }
        FunctionDef init = method(c, "__init__");
        if (init == null) {
            return "has no __init__";
        }
        Call found = null;
        int count = 0;
        for (Stmt st : init.body) {
            if (st instanceof ExprStmt && isSuperInit(((ExprStmt) st).value)) {
                found = (Call) ((ExprStmt) st).value;
                count++;
            }
        }
        if (count != 1 || !found.args.isEmpty()) {
            return "does not call super().__init__ once with keywords";
        }
        Set<String> kw = new HashSet<String>();
        for (Keyword k : found.keywords) {
            if (k.arg == null || !kw.add(k.arg)) {
                return "passes ** or a repeated keyword";
            }
        }
        if (!kw.equals(new HashSet<String>(s.subList(0, s.size() - 1)))) {
            return "passes keywords other than its slots";
        }
        supers.put(c, found);
        return null;
    }

    private static boolean isSuperInit(Expr e) {
        if (!(e instanceof Call) || !(((Call) e).func instanceof Attribute)) {
            return false;
        }
        Attribute f = (Attribute) ((Call) e).func;
        return "__init__".equals(f.attr) && f.value instanceof Call && ((Call) f.value).func instanceof Name
                && "super".equals(((Name) ((Call) f.value).func).id) && ((Call) f.value).args.isEmpty();
    }

    /// The `super().__init__(k=v, ...)` statement becomes `self.k = v` for
    /// each keyword, in order, then `_hash` over `(type(v), v, ...)` as the
    /// root computed it.
    private static void replaceSuper(List<Stmt> body, Call sup, String path) {
        for (int i = 0; i < body.size(); i++) {
            Stmt st = body.get(i);
            if (st instanceof ExprStmt && ((ExprStmt) st).value == sup) { //NOPMD CompareObjectsWithEquals - this very call node
                List<Stmt> out = new ArrayList<Stmt>();
                StringBuilder h = new StringBuilder("self._hash = hash((");
                for (Keyword k : sup.keywords) {
                    Node.Module one = Parser.parse("self." + k.arg + " = 0\n", path);
                    Assign a = (Assign) one.body.get(0);
                    Assign store = new Assign(a.targets, k.value);
                    DataclassInit.atLine(a.targets, st.line);
                    store.line = st.line;
                    out.add(store);
                    h.append("type(self.").append(k.arg).append("), self.").append(k.arg).append(", ");
                }
                h.append("))\n");
                Node.Module hm = Parser.parse(h.toString(), path);
                DataclassInit.atLine(hm, st.line);
                out.addAll(hm.body);
                body.remove(i);
                body.addAll(i, out);
                return;
            }
        }
    }

    private static final String EQ = ""
            + "def __eq__(self, other: Any) -> bool:\n"
            + "    stack: list[tuple[Immutable, Any]] = [(self, other)]\n"
            + "    while stack:\n"
            + "        a, b = stack.pop()\n"
            + "        if a is b:\n"
            + "            continue\n"
            + "        if not isinstance(b, a.__base__()) or a._hash != b._hash:\n"
            + "            return False\n"
            + "        for x, y in zip(a._cn1_values(), b._cn1_values()):\n"
            + "            if isinstance(x, Immutable):\n"
            + "                stack.append((x, y))\n"
            + "            elif x.__class__ is tuple and y.__class__ is tuple:\n"
            + "                if len(x) != len(y):\n"
            + "                    return False\n"
            + "                for i, j in zip(x, y):\n"
            + "                    if isinstance(i, Immutable):\n"
            + "                        stack.append((i, j))\n"
            + "                    elif i != j:\n"
            + "                        return False\n"
            + "            elif x != y:\n"
            + "                return False\n"
            + "    return True\n";

    private static final String REPR = ""
            + "def __repr__(self) -> str:\n"
            + "    output: list[str] = []\n"
            + "    stack: list[Any] = [self]\n"
            + "    while stack:\n"
            + "        item = stack.pop()\n"
            + "        if item.__class__ is str:\n"
            + "            output.append(item)\n"
            + "        elif isinstance(item, Immutable):\n"
            + "            stack.append(')')\n"
            + "            keys = item._cn1_names()\n"
            + "            values = item._cn1_values()\n"
            + "            for index in range(len(keys) - 1, -1, -1):\n"
            + "                stack.append(_repr_value(values[index]))\n"
            + "                stack.append(f'{keys[index]}=' if index == 0 else f', {keys[index]}=')\n"
            + "            stack.append(f'{item.__class__.__name__}(')\n"
            + "        else:\n"
            + "            stack.append(',)' if len(item) == 1 else ')')\n"
            + "            for index in range(len(item) - 1, -1, -1):\n"
            + "                stack.append(_repr_value(item[index]))\n"
            + "                if index:\n"
            + "                    stack.append(', ')\n"
            + "            stack.append('(')\n"
            + "    return ''.join(output)\n"
            + "def _cn1_names(self) -> tuple[str, ...]:\n"
            + "    return ()\n"
            + "def _cn1_values(self) -> tuple[Any, ...]:\n"
            + "    return ()\n";

    /// Drops the root's `__init__` and `__setattr__` and makes its `__eq__`
    /// and `__repr__` read the slots through the per-class methods. They are
    /// the root's own algorithms (an explicit stack, never recursion), with
    /// `getattr` over `__slots__` replaced.
    private static void rewriteRoot(String path, ClassDef root) {
        String eq = EQ.replace("Immutable", root.name);
        String repr = REPR.replace("Immutable", root.name);
        for (int i = root.body.size() - 1; i >= 0; i--) {
            Stmt s = root.body.get(i);
            if (s instanceof FunctionDef) {
                String n = ((FunctionDef) s).name;
                if ("__init__".equals(n) || "__setattr__".equals(n)) {
                    root.body.remove(i);
                } else if (("__eq__".equals(n) || "__repr__".equals(n)) && readsComputed((FunctionDef) s)
                        && ("__repr__".equals(n) || readsAttr(((FunctionDef) s).body, "_hash"))
                        && readsAttr(((FunctionDef) s).body, "__slots__")) {
                    Node.Module g = Parser.parse("__eq__".equals(n) ? eq : repr, path);
                    DataclassInit.atLine(g, s.line);
                    root.body.remove(i);
                    root.body.addAll(i, g.body);
                }
            }
        }
    }

    /// Where, under `body`, an assignment's value is a list comprehension
    /// over `X.__slots__`: the path of statement indexes, or empty.
    private static int[] slotsComp(List<Stmt> body) {
        for (int i = 0; i < body.size(); i++) {
            Stmt s = body.get(i);
            if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).value instanceof Comp
                    && slotsOwner(((Assign) s).value) != null) {
                return new int[] {i};
            }
            for (List<Stmt> inner : blocks(s)) {
                int[] r = slotsComp(inner);
                if (r.length > 0) {
                    int[] p = new int[r.length + 1];
                    p[0] = i;
                    System.arraycopy(r, 0, p, 1, r.length);
                    return p;
                }
            }
        }
        return new int[0];
    }

    private static List<List<Stmt>> blocks(Stmt s) {
        List<List<Stmt>> r = new ArrayList<List<Stmt>>();
        if (s instanceof While) {
            r.add(((While) s).body);
        } else if (s instanceof If) {
            r.add(((If) s).body);
            r.add(((If) s).orelse);
        } else if (s instanceof For) {
            r.add(((For) s).body);
        }
        return r;
    }

    /// The name `X` of an `X.__slots__` under `e`, or null.
    private static String slotsOwner(Object e) {
        if (e instanceof Attribute && "__slots__".equals(((Attribute) e).attr) && ((Attribute) e).value instanceof Name) {
            return ((Name) ((Attribute) e).value).id;
        }
        String[] found = new String[1];
        Infer.children(e, x -> {
            if (found[0] == null) {
                found[0] = slotsOwner(x);
            }
        });
        return found[0];
    }

    /// `values = [getattr(obj, s) for s in obj.__slots__[:-1]]` becomes
    /// `values = list(obj._cn1_values())`.
    private static void rewritePickler(String path, FunctionDef f, int[] at) {
        List<Stmt> body = f.body;
        for (int k = 0; k < at.length - 1; k++) {
            Stmt s = body.get(at[k]);
            List<List<Stmt>> bs = blocks(s);
            List<Stmt> next = null;
            for (List<Stmt> b : bs) {
                if (slotsComp(b).length > 0 && next == null) {
                    next = b;
                }
            }
            body = next;
        }
        Assign a = (Assign) body.get(at[at.length - 1]);
        String owner = slotsOwner(a.value);
        Node.Module g = Parser.parse("_ = list(" + owner + "._cn1_values())\n", path);
        Assign repl = new Assign(a.targets, ((Assign) g.body.get(0)).value);
        DataclassInit.atLine(repl, a.line);
        body.set(at[at.length - 1], repl);
    }

    /// The build error for a store that would mutate an immutable instance.
    private static void enforce(Map<String, Node.Module> trees, Set<ClassDef> classes, Set<String> slotNames) {
        Set<String> foreignFields = new HashSet<String>();
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            for (Stmt s : e.getValue().body) {
                if (s instanceof ClassDef) {
                    if (classes.contains(s)) {
                        for (Stmt m : ((ClassDef) s).body) {
                            if (m instanceof FunctionDef && !((FunctionDef) m).dataclassInit) {
                                selfStores(e.getKey(), (FunctionDef) m);
                            }
                        }
                    } else {
                        Set<String> own = new HashSet<String>();
                        WholeProgram.attrStores(s, own);
                        foreignFields.addAll(own);
                    }
                }
            }
        }
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            for (Stmt s : e.getValue().body) {
                if (s instanceof ClassDef) {
                    continue;
                }
                anyStore(e.getKey(), s, slotNames, foreignFields);
            }
            for (Stmt s : e.getValue().body) {
                if (s instanceof ClassDef && !classes.contains(s)) {
                    anyStore(e.getKey(), s, slotNames, foreignFields);
                }
            }
        }
    }

    private static void selfStores(String path, FunctionDef f) {
        if (f.args.positional.isEmpty()) {
            return;
        }
        String self = f.args.positional.get(0).name;
        storesOn(path, f, self);
    }

    private static void storesOn(String path, Object o, String self) {
        Expr t = o instanceof Assign && ((Assign) o).targets.size() == 1 ? ((Assign) o).targets.get(0)
                : o instanceof AugAssign ? ((AugAssign) o).target
                : o instanceof AnnAssign ? ((AnnAssign) o).target : null;
        if (t instanceof Attribute && ((Attribute) t).value instanceof Name && self.equals(((Name) ((Attribute) t).value).id)) {
            throw new PythonSyntaxError(path, ((Node) o).line, 1, "store of '" + ((Attribute) t).attr
                    + "' on an immutable instance outside its constructor");
        }
        if (o instanceof Call && ((Call) o).func instanceof Attribute
                && ("__setattr__".equals(((Attribute) ((Call) o).func).attr)
                || "__delattr__".equals(((Attribute) ((Call) o).func).attr))) {
            throw new PythonSyntaxError(path, ((Node) o).line, 1, "__setattr__ on an immutable instance");
        }
        Infer.children(o, x -> storesOn(path, x, self));
    }

    /// A store anywhere of a slot name that no other class stores as its
    /// own: only an immutable instance can be its receiver.
    private static void anyStore(String path, Stmt s, Set<String> slotNames, Set<String> foreignFields) {
        Set<String> out = new HashSet<String>();
        WholeProgram.attrStores(s, out);
        for (String n : out) {
            if (slotNames.contains(n) && !foreignFields.contains(n)) {
                throw new PythonSyntaxError(path, s.line, 1, "store of '" + n
                        + "', a field of an immutable class, outside its constructor");
            }
        }
    }

    private static FunctionDef method(ClassDef c, String name) {
        for (Stmt s : c.body) {
            if (s instanceof FunctionDef && name.equals(((FunctionDef) s).name)) {
                return (FunctionDef) s;
            }
        }
        return null;
    }

    /// The literal `__slots__` of `c`, or null.
    private static List<String> slots(ClassDef c) {
        for (Stmt s : c.body) {
            Expr t = s instanceof Assign && ((Assign) s).targets.size() == 1 ? ((Assign) s).targets.get(0)
                    : s instanceof AnnAssign ? ((AnnAssign) s).target : null;
            Expr v = s instanceof Assign ? ((Assign) s).value : s instanceof AnnAssign ? ((AnnAssign) s).value : null;
            if (t instanceof Name && "__slots__".equals(((Name) t).id)) {
                if (!(v instanceof TupleExpr)) {
                    return null;
                }
                List<String> r = new ArrayList<String>();
                for (Expr e : ((TupleExpr) v).elts) {
                    if (!(e instanceof Constant) || ((Constant) e).kind != Constant.Kind.STR) {
                        return null;
                    }
                    r.add(((Constant) e).text);
                }
                return r;
            }
        }
        return null;
    }
}
