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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Lowers openpyxl's declarative descriptor fields to plain properties,
/// right after parsing, with the whole program in hand.
///
/// openpyxl declares the fields of its model classes as class attributes
/// holding descriptor objects (`b = Bool(allow_none=True)`), whose
/// `__set__` validates and stores into the instance dictionary. Every read
/// and write of such a field is then a dynamic descriptor call the type
/// inference cannot see through, so every field reads as untyped. This
/// pass writes what each descriptor does as Python the inference already
/// understands:
///
/// ```python
/// class Font(Serialisable):
///     b = Bool(allow_none=True)
///     # becomes
///     _dm_b = None
///     @property
///     def b(self):
///         return self._dm_b
///     @b.setter
///     def b(self, value):
///         if value is not None:
///             if isinstance(value, str):
///                 if value in ('false', 'f', '0'):
///                     value = False
///             if not isinstance(value, bool):
///                 ...
///         self._dm_b = value
///     __attrs__ = ('b', ...)
/// ```
///
/// Only the kinds of `openpyxl/descriptors/base.py` whose `__set__` is
/// fully known are lowered -- `Typed`, `Integer`, `Float`, `Bool`, `String`,
/// `Text`, `Set`, `NoneSet` and `Alias` -- and only with constant arguments
/// the descriptor would itself only store. Everything else (nested and
/// sequence descriptors, which `Serialisable.from_tree` and `to_tree` read
/// off the class as objects; a `Typed` of a serialisable type, for the
/// same reason) is left as it is.
///
/// `MetaSerialisable` computes `__attrs__` from the descriptor objects it
/// finds in the class dictionary; a lowered field is no longer one, so the
/// pass writes the tuple the metaclass would have computed into the class
/// body, which the metaclass then keeps. Classes whose dictionary cannot be
/// classified entry by entry keep their descriptors.
///
/// The class attribute a getter falls back to before the first store is
/// `None`, where the descriptor (which has no `__get__`) handed back
/// itself: no model reads a field it never set, and `None` is what every
/// caller tests for.
public final class DeclarativeMap {
    private static final String BASE = "openpyxl.descriptors.base:";
    private static final String DESCRIPTOR = BASE + "Descriptor";
    private static final String TYPED = BASE + "Typed";
    private static final String ALIAS = BASE + "Alias";
    private static final String SEQUENCE = "openpyxl.descriptors.sequence:Sequence";
    private static final String BASE_NESTED_SEQUENCE = "openpyxl.descriptors.sequence:NestedSequence";
    private static final String SERIALISABLE = "openpyxl.descriptors.serialisable:Serialisable";
    private static final String STRICT = "openpyxl.descriptors:Strict";
    private static final String META_SERIALISABLE = "openpyxl.descriptors:MetaSerialisable";
    private static final String META_STRICT = "openpyxl.descriptors:MetaStrict";
    /// Builtins a class body may call without making a descriptor.
    private static final Set<String> PLAIN_BUILTINS = new HashSet<String>(Arrays.asList("property", "classmethod",
            "staticmethod", "set", "frozenset", "dict", "list", "tuple", "str", "int", "float", "bool", "bytes",
            "object", "type", "len", "sorted", "range"));
    /// Builtin types an `expected_type` may name.
    private static final Set<String> BUILTIN_TYPES = new HashSet<String>(Arrays.asList("str", "int", "float",
            "bool", "bytes", "tuple", "list", "dict", "set", "frozenset", "object"));
    /// The kinds of `openpyxl/descriptors/base.py` this pass writes out.
    private static final Set<String> KINDS = new HashSet<String>(Arrays.asList("Typed", "String", "Integer",
            "Float", "Text", "Bool", "Set", "NoneSet", "Alias"));
    /// The marker for "resolves outside the program".
    private static final String EXTERNAL = "";
    /// The answer of a three-valued test that cannot be settled.
    private static final int UNKNOWN = -1;

    private final Map<String, Node.Module> modules = new HashMap<String, Node.Module>();
    private final Set<String> packages = new HashSet<String>();
    private final Map<String, Map<String, Integer>> bindings = new HashMap<String, Map<String, Integer>>();
    /// `Class.attr` for every attribute read through a plain name anywhere.
    private final Set<String> dotted = new HashSet<String>();
    private final List<String> report = new ArrayList<String>();
    private int lowered;
    private int aliases;
    private int synthesized;
    /// The program subclasses of each program class, by key.
    private final Map<String, List<String>> children = new HashMap<String, List<String>>();
    /// The class bodies as parsed, before any class is rewritten: a base's
    /// descriptors are read from here once the base itself is lowered.
    private final Map<ClassDef, List<Stmt>> original = new HashMap<ClassDef, List<Stmt>>();
    /// The classes that pass every test a lowering makes, from a first pass
    /// that rewrites nothing.
    private final Set<String> passes = new HashSet<String>();
    /// Model classes whose methods were generated.
    private final Set<String> serialDone = new HashSet<String>();
    /// The classes a lowering may rewrite: they pass, and so does every
    /// program subclass, which would otherwise inherit generated methods
    /// written for this class's dictionary.
    private final Set<String> allowed = new HashSet<String>();
    /// The fields each rewritten class turned into properties.
    private final Map<String, Set<String>> loweredOf = new HashMap<String, Set<String>>();
    private final Set<String> visited = new HashSet<String>();
    private final Set<String> needHelpers = new HashSet<String>();
    /// Modules a class of which binds `property`: their lowerings decorate
    /// with the module-level alias [Infer#PROPERTY_ALIAS] instead, which a
    /// class body cannot shadow.
    private final Set<String> propertyAlias = new HashSet<String>();
    private boolean dry;

    private DeclarativeMap() {
    }

    /// Rewrites the descriptor fields of every openpyxl model class among
    /// `trees` (by path), and answers the fields left alone, one line each.
    public static List<String> rewrite(Map<String, Node.Module> trees) {
        DeclarativeMap d = new DeclarativeMap();
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            String p = e.getKey();
            if (!p.endsWith(".py")) {
                continue;
            }
            String m = p.substring(0, p.length() - 3);
            if (m.endsWith("/__init__")) {
                m = m.substring(0, m.length() - 9);
                d.packages.add(m.replace('/', '.'));
            }
            d.modules.put(m.replace('/', '.'), e.getValue());
        }
        if (!d.modules.containsKey("openpyxl.descriptors.base")) {
            return new ArrayList<String>(d.report);
        }
        for (Map.Entry<String, Node.Module> e : d.modules.entrySet()) {
            Map<String, Integer> n = new HashMap<String, Integer>();
            for (Stmt s : e.getValue().body) {
                DataclassInit.count(s, n);
            }
            d.bindings.put(e.getKey(), n);
            d.collectDotted(e.getValue());
        }
        for (Map.Entry<String, Node.Module> e : d.modules.entrySet()) {
            d.collectSubclassed(e.getKey(), e.getValue().body);
            for (Stmt s : e.getValue().body) {
                if (s instanceof ClassDef) {
                    d.original.put((ClassDef) s, new ArrayList<Stmt>(((ClassDef) s).body));
                }
            }
        }
        d.decide();
        for (Map.Entry<String, Node.Module> e : d.modules.entrySet()) {
            for (Stmt s : e.getValue().body) {
                if (s instanceof ClassDef) {
                    d.ensure(e.getKey() + ":" + ((ClassDef) s).name);
                }
            }
        }
        for (Map.Entry<String, Node.Module> e : d.modules.entrySet()) {
            List<Stmt> body = e.getValue().body;
            int first = -1;
            for (int i = 0; i < body.size() && first < 0; i++) {
                if (body.get(i) instanceof ClassDef
                        && d.needHelpers.contains(e.getKey() + ":" + ((ClassDef) body.get(i)).name)) {
                    first = i;
                }
            }
            if (first >= 0) {
                // Before the first class that uses them: the module has
                // imported serialisable by then, to subclass Serialisable.
                Node.Module helpers = Parser.parse(SerialisableMethods.HELPERS, e.getKey().replace('.', '/') + ".py");
                DataclassInit.atLine(helpers, body.get(first).line);
                body.addAll(first, helpers.body);
                if (d.propertyAlias.contains(e.getKey())) {
                    Node.Module alias = Parser.parse(Infer.PROPERTY_ALIAS + " = property\n",
                            e.getKey().replace('.', '/') + ".py");
                    DataclassInit.atLine(alias, body.get(first).line);
                    body.addAll(first, alias.body);
                }
            }
        }
        if (d.dropMetaclasses()) {
            trees.put(META_PATH, Parser.parse(META_MODULE, META_PATH));
            d.replaceSerialisableBase();
        }
        if (d.synthesized > 0) {
            trees.put(SerialisableMethods.MODULE_PATH, Parser.parse(SerialisableMethods.moduleSource(),
                    SerialisableMethods.MODULE_PATH));
        }
        if (Boolean.getBoolean("cn1.python.declmap.report")) {
            System.err.println("declmap: lowered " + d.lowered + " fields, " + d.aliases //NOPMD SystemPrintln - opt-in diagnostics
                    + " aliases, synthesized " + d.synthesized + " classes; left " + d.report.size());
            for (String r : d.report) {
                System.err.println("declmap: " + r); //NOPMD SystemPrintln - opt-in diagnostics
            }
        }
        return new ArrayList<String>(d.report);
    }

    /// The first pass: every module-level class is tested without being
    /// rewritten, then a class stays allowed only while all its program
    /// subclasses are.
    private void decide() {
        dry = true;
        int mark = report.size();
        for (Map.Entry<String, Node.Module> e : modules.entrySet()) {
            for (Stmt s : e.getValue().body) {
                if (s instanceof ClassDef) {
                    lowerClass(e.getKey(), (ClassDef) s);
                }
            }
        }
        while (report.size() > mark) {
            report.remove(report.size() - 1);
        }
        dry = false;
        allowed.addAll(passes);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String k : new ArrayList<String>(allowed)) {
                for (String ch : children.getOrDefault(k, new ArrayList<String>())) {
                    if (!allowed.contains(ch) || Boolean.getBoolean("cn1.python.declmap.leafOnly")) {
                        allowed.remove(k);
                        changed = true;
                        break;
                    }
                }
            }
        }
    }

    /// Rewrites class `key` after its program bases, so that what a base
    /// turned into properties is known when its subclasses are generated.
    private void ensure(String key) {
        if (!visited.add(key)) {
            return;
        }
        ClassDef c = classOf(key);
        if (c == null) {
            return;
        }
        for (Expr b : c.bases) {
            String r = resolveExpr(moduleOf(key), b);
            if (r != null && !r.isEmpty() && modules.containsKey(moduleOf(r))) {
                ensure(r);
            }
        }
        if (lowerClass(moduleOf(key), c)) {
            needHelpers.add(key);
        }
    }

    /// The class body as parsed.
    private List<Stmt> bodyOf(ClassDef c) {
        List<Stmt> b = original.get(c);
        return b == null ? c.body : b;
    }

    /// Records the program subclasses of every program class named as a
    /// base in `body`, nested classes included.
    private void collectSubclassed(String mod, List<Stmt> body) {
        for (Stmt s : body) {
            if (s instanceof ClassDef) {
                for (Expr b : ((ClassDef) s).bases) {
                    String r = resolveExpr(mod, b);
                    if (r != null && !r.isEmpty()) {
                        children.computeIfAbsent(r, x -> new ArrayList<String>()).add(mod + ":" + ((ClassDef) s).name);
                    }
                }
                collectSubclassed(mod, ((ClassDef) s).body);
            }
        }
    }

    /// Where [#META_MODULE] goes.
    public static final String META_PATH = "openpyxl/_dm_meta.py";
    /// `MetaSerialisable.__new__` and `MetaStrict.__new__` as functions of
    /// the finished class: the same loop over the class's own dictionary,
    /// in its definition order, storing what the metaclass put in it.
    private static final String META_MODULE = "from openpyxl.descriptors.base import Descriptor, Typed, Alias\n"
            + "from openpyxl.descriptors.sequence import Sequence\n"
            + "def strict(cls):\n    for k, v in list(cls.__dict__.items()):\n        if isinstance(v, Descriptor):\n"
            + "            v.name = k\n"
            + "def serialisable(cls):\n    methods = cls.__dict__\n    attrs = []\n    nested = []\n"
            + "    elements = []\n    namespaced = []\n    for k, v in list(methods.items()):\n"
            + "        if isinstance(v, Descriptor):\n            ns = getattr(v, 'namespace', None)\n"
            + "            if ns:\n                namespaced.append((k, \"{%s}%s\" % (ns, k)))\n"
            + "            if getattr(v, 'nested', False):\n                nested.append(k)\n"
            + "                elements.append(k)\n            elif isinstance(v, Sequence):\n"
            + "                elements.append(k)\n            elif isinstance(v, Typed):\n"
            + "                if hasattr(v.expected_type, 'to_tree'):\n                    elements.append(k)\n"
            + "                elif isinstance(v.expected_type, tuple):\n"
            + "                    if any((hasattr(el, \"to_tree\") for el in v.expected_type)):\n"
            + "                        continue\n                else:\n                    attrs.append(k)\n"
            + "            else:\n                if not isinstance(v, Alias):\n                    attrs.append(k)\n"
            + "    if methods.get('__attrs__') is None:\n        cls.__attrs__ = tuple(attrs)\n"
            + "    cls.__namespaced__ = tuple(namespaced)\n    if methods.get('__nested__') is None:\n"
            + "        cls.__nested__ = tuple(sorted(nested))\n    if methods.get('__elements__') is None:\n"
            + "        cls.__elements__ = tuple(sorted(elements))\n    strict(cls)\n";

    /// The source of the module at [#META_PATH].
    public static String metaSource() {
        return META_MODULE;
    }

    /// Takes `metaclass=` off `Serialisable` and `Strict` and runs the
    /// metaclass's work as a call after every class of theirs instead.
    /// A class with a metaclass that defines `__new__` gets no layout --
    /// no typed fields -- and every openpyxl model class inherits this one.
    /// Answers false, changing nothing, unless every class can be told
    /// apart: a class whose bases cannot be followed might be a model.
    private boolean dropMetaclasses() {
        Map<ClassDef, String> plan = new LinkedHashMap<ClassDef, String>();
        for (Map.Entry<String, Node.Module> e : modules.entrySet()) {
            String mod = e.getKey();
            for (Stmt s : e.getValue().body) {
                if (s instanceof ClassDef) {
                    String key = mod + ":" + ((ClassDef) s).name;
                    int ser = derives(key, SERIALISABLE, 0);
                    int str = derives(key, STRICT, 0);
                    if ((ser == UNKNOWN || str == UNKNOWN) && importsDescriptors(mod, e.getValue())) {
                        report.add(mod + ":" + s.line + " " + ((ClassDef) s).name + ": bases unknown, metaclass kept");
                        return false;
                    }
                    if (ser == 1 || str == 1) {
                        plan.put((ClassDef) s, ser == 1 ? "_dm_meta_serialisable" : "_dm_meta_strict");
                    }
                }
                if (mod.startsWith("openpyxl") && nestedModel(mod, s, false)) {
                    report.add(mod + ":" + s.line + ": model class not at module level, metaclass kept");
                    return false;
                }
            }
        }
        if (plan.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, Node.Module> e : modules.entrySet()) {
            List<Stmt> body = e.getValue().body;
            String file = e.getKey().replace('.', '/') + ".py";
            int first = -1;
            for (int i = 0; i < body.size(); i++) {
                String fn = body.get(i) instanceof ClassDef ? plan.get(body.get(i)) : null;
                if (fn == null) {
                    continue;
                }
                ClassDef c = (ClassDef) body.get(i);
                c.keywords.removeIf(k -> "metaclass".equals(k.arg));
                Node.Module call = Parser.parse(fn + "(" + c.name + ")\n", file);
                DataclassInit.atLine(call, c.line);
                body.add(i + 1, call.body.get(0));
                if (first < 0) {
                    first = i;
                }
                i++;
            }
            if (first >= 0) {
                Node.Module imp = Parser.parse("from openpyxl._dm_meta import strict as _dm_meta_strict, "
                        + "serialisable as _dm_meta_serialisable\n", file);
                DataclassInit.atLine(imp, body.get(first).line);
                body.addAll(first, imp.body);
            }
        }
        return true;
    }

    /// Whether `m` imports from the descriptor package: only such a module
    /// can declare a model class whose bases this pass cannot follow.
    private boolean importsDescriptors(String mod, Node.Module m) {
        for (Stmt s : m.body) {
            if (s instanceof ImportFrom) {
                String t = target(mod, (ImportFrom) s);
                if (t != null && t.startsWith("openpyxl.descriptors")) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Whether a class below the top level of `s` derives from a model base.
    private boolean nestedModel(String mod, Stmt s, boolean inner) {
        boolean[] found = {false};
        if (s instanceof ClassDef && inner) {
            for (Expr b : ((ClassDef) s).bases) {
                String r = resolveExpr(mod, b);
                if (r == null || !r.isEmpty() && (derives(r, SERIALISABLE, 0) != 0 || derives(r, STRICT, 0) != 0)) {
                    return true;
                }
            }
        }
        Infer.children(s, x -> {
            if (x instanceof Stmt && !found[0]) {
                found[0] = nestedModel(mod, (Stmt) x, true);
            }
        });
        return found[0];
    }

    private void collectDotted(Object o) {
        if (o instanceof Attribute && ((Attribute) o).value instanceof Name) {
            dotted.add(((Name) ((Attribute) o).value).id + "." + ((Attribute) o).attr);
        }
        Infer.children(o, this::collectDotted);
    }

    // ---- name resolution -------------------------------------------------

    /// The absolute module an import names from `mod`.
    private String target(String mod, ImportFrom f) {
        if (f.level == 0) {
            return f.module;
        }
        String pkg = packages.contains(mod) ? mod : parent(mod);
        for (int i = 1; i < f.level && pkg != null; i++) {
            pkg = parent(pkg);
        }
        if (pkg == null) {
            return null;
        }
        return f.module == null ? pkg : pkg + "." + f.module;
    }

    private static String parent(String m) {
        int dot = m.lastIndexOf('.');
        return dot < 0 ? null : m.substring(0, dot);
    }

    /// `module:Class` for the program class `name` names in `mod`,
    /// [#EXTERNAL] for a name from outside the program, null if unsure.
    private String resolve(String mod, String name, int depth) {
        Node.Module m = modules.get(mod);
        if (m == null) {
            return EXTERNAL;
        }
        if (depth > 16) {
            return null;
        }
        int bound = bindings.get(mod).getOrDefault(name, 0);
        if (bound == 1) {
            for (Stmt s : m.body) {
                if (s instanceof ClassDef && name.equals(((ClassDef) s).name)) {
                    return mod + ":" + name;
                }
                if (s instanceof Assign && ((Assign) s).targets.size() == 1
                        && ((Assign) s).targets.get(0) instanceof Name
                        && name.equals(((Name) ((Assign) s).targets.get(0)).id)
                        && ((Assign) s).value instanceof Name) {
                    // A module-level alias of a class (Coordinate = Integer).
                    return resolve(mod, ((Name) ((Assign) s).value).id, depth + 1);
                }
                if (s instanceof ImportFrom) {
                    ImportFrom f = (ImportFrom) s;
                    for (Alias a : f.names) {
                        if (name.equals(a.asname != null ? a.asname : a.name)) {
                            String t = target(mod, f);
                            if (t == null || modules.containsKey(t + "." + a.name)) {
                                return null;
                            }
                            return resolve(t, a.name, depth + 1);
                        }
                    }
                }
                if (s instanceof Import) {
                    for (Alias a : ((Import) s).names) {
                        String b = a.asname != null ? a.asname : a.name;
                        if (name.equals(b) || b.startsWith(name + ".")) {
                            return modules.containsKey(a.name) ? null : EXTERNAL;
                        }
                    }
                }
            }
            return null;
        }
        if (bound != 0) {
            return agreeingImports(mod, m, name, depth);
        }
        boolean foreignStar = false;
        for (Stmt s : m.body) {
            if (s instanceof ImportFrom && "*".equals(((ImportFrom) s).names.get(0).name)) {
                String t = target(mod, (ImportFrom) s);
                foreignStar |= t == null || !modules.containsKey(t);
                if (t != null && modules.containsKey(t)) {
                    String r = resolve(t, name, depth + 1);
                    if (r != null && !r.isEmpty()) {
                        return r;
                    }
                }
            }
        }
        // Bound nowhere in the module, nor by a star import of the
        // program: a builtin (Exception, object), or a NameError.
        return foreignStar ? null : EXTERNAL;
    }

    /// A name bound more than once, each time by an import of the same
    /// program class (a name listed twice in one import): that class.
    private String agreeingImports(String mod, Node.Module m, String name, int depth) {
        String found = null;
        int seen = 0;
        for (Stmt s : m.body) {
            if (!(s instanceof ImportFrom)) {
                continue;
            }
            ImportFrom f = (ImportFrom) s;
            for (Alias a : f.names) {
                if (name.equals(a.asname != null ? a.asname : a.name)) {
                    String t = target(mod, f);
                    if (t == null || modules.containsKey(t + "." + a.name)) {
                        return null;
                    }
                    String r = resolve(t, a.name, depth + 1);
                    if (r == null || r.isEmpty() || found != null && !found.equals(r)) {
                        return null;
                    }
                    found = r;
                    seen++;
                }
            }
        }
        return seen == bindings.get(mod).getOrDefault(name, 0) ? found : null;
    }

    /// True when `name` in `mod` is bound once, at module level, to a
    /// string or number constant, here or through imports of the program.
    private boolean constantName(String mod, String name, int depth) {
        return constantOf(mod, name, depth) != null;
    }

    /// The constant [#constantName] finds, or null.
    private Constant constantOf(String mod, String name, int depth) {
        Node.Module m = modules.get(mod);
        if (m == null || depth > 16) {
            return null;
        }
        int bound = bindings.get(mod).getOrDefault(name, 0);
        if (bound == 0) {
            // Bound by a star import of a program module, if at all.
            for (Stmt s : m.body) {
                if (s instanceof ImportFrom && "*".equals(((ImportFrom) s).names.get(0).name)) {
                    String t = target(mod, (ImportFrom) s);
                    Constant c = t != null && modules.containsKey(t) ? constantOf(t, name, depth + 1) : null;
                    if (c != null) {
                        return c;
                    }
                }
            }
            return null;
        }
        if (bound != 1) {
            return null;
        }
        for (Stmt s : m.body) {
            if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name
                    && name.equals(((Name) ((Assign) s).targets.get(0)).id)) {
                Expr v = ((Assign) s).value;
                if (v instanceof Constant && ((Constant) v).kind == Constant.Kind.INT) {
                    return (Constant) v;
                }
                String t = stringOf(mod, v, depth);
                return t == null ? null : new Constant(Constant.Kind.STR, t);
            }
            if (s instanceof ImportFrom) {
                for (Alias a : ((ImportFrom) s).names) {
                    if (name.equals(a.asname != null ? a.asname : a.name)) {
                        String t = target(mod, (ImportFrom) s);
                        return t != null && modules.containsKey(t) && !modules.containsKey(t + "." + a.name)
                                ? constantOf(t, a.name, depth + 1) : null;
                    }
                }
            }
        }
        return null;
    }

    /// The string a module-level expression folds to: a string constant,
    /// a constant name, or a `+` of those; null otherwise.
    private String stringOf(String mod, Expr v, int depth) {
        if (v instanceof Constant) {
            return ((Constant) v).kind == Constant.Kind.STR ? ((Constant) v).text : null;
        }
        if (v instanceof Name) {
            Constant c = constantOf(mod, ((Name) v).id, depth + 1);
            return c == null || c.kind != Constant.Kind.STR ? null : c.text;
        }
        if (v instanceof Node.BinOp && "+".equals(((Node.BinOp) v).op)) {
            String l = stringOf(mod, ((Node.BinOp) v).left, depth);
            String r = l == null ? null : stringOf(mod, ((Node.BinOp) v).right, depth);
            return r == null ? null : l + r;
        }
        return null;
    }

    /// For `Cls.attr` naming an entry of a program class's body: the module
    /// of that class and the entry's value as parsed, followed through
    /// further such references; null otherwise.
    private Object[] deref(String mod, Expr e, int depth) {
        if (!(e instanceof Attribute) || !(((Attribute) e).value instanceof Name) || depth > 16) {
            return null;
        }
        String k = resolve(mod, ((Name) ((Attribute) e).value).id, 0);
        if (k == null || k.isEmpty()) {
            return null;
        }
        ClassDef c = classOf(k);
        if (c == null) {
            return null;
        }
        String attr = ((Attribute) e).attr;
        // The last binding wins, as in the class dictionary.
        Expr found = null;
        boolean function = false;
        for (Stmt s : bodyOf(c)) {
            if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name
                    && attr.equals(((Name) ((Assign) s).targets.get(0)).id)) {
                found = ((Assign) s).value;
                function = false;
            } else if (s instanceof FunctionDef && attr.equals(((FunctionDef) s).name)
                    || s instanceof ClassDef && attr.equals(((ClassDef) s).name)) {
                found = null;
                function = true;
            }
        }
        if (function) {
            // A function or class of the body: never a descriptor.
            return new Object[] {moduleOf(k), null};
        }
        if (found == null) {
            return null;
        }
        if (found instanceof Attribute) {
            return deref(moduleOf(k), found, depth + 1);
        }
        return found instanceof Call || namesOf(moduleOf(k), found, depth + 1) != null
                ? new Object[] {moduleOf(k), found} : null;
    }

    /// [#resolve] for an expression: a name, or an attribute of a module
    /// from outside the program.
    private String resolveExpr(String mod, Expr e) {
        if (e instanceof Name) {
            return resolve(mod, ((Name) e).id, 0);
        }
        Expr root = e;
        while (root instanceof Attribute) {
            root = ((Attribute) root).value;
        }
        if (root instanceof Name && EXTERNAL.equals(resolve(mod, ((Name) root).id, 0))) {
            return EXTERNAL;
        }
        return null;
    }

    private ClassDef classOf(String key) {
        int c = key.indexOf(':');
        Node.Module m = modules.get(key.substring(0, c));
        String n = key.substring(c + 1);
        for (Stmt s : m.body) {
            if (s instanceof ClassDef && n.equals(((ClassDef) s).name)) {
                return (ClassDef) s;
            }
        }
        return null;
    }

    private static String moduleOf(String key) {
        return key.substring(0, key.indexOf(':'));
    }

    /// 1 if program class `key` is or derives from `want`, 0 if not,
    /// [#UNKNOWN] if a base cannot be resolved.
    private int derives(String key, String want, int depth) {
        if (key.equals(want)) {
            return 1;
        }
        ClassDef c = classOf(key);
        if (c == null || depth > 32) {
            return UNKNOWN;
        }
        int answer = 0;
        for (Expr b : c.bases) {
            String r = resolveExpr(moduleOf(key), b);
            if (r == null) {
                answer = UNKNOWN;
            } else if (!r.isEmpty()) {
                int d = derives(r, want, depth + 1);
                if (d == 1) {
                    return 1;
                }
                if (d == UNKNOWN) {
                    answer = UNKNOWN;
                }
            }
        }
        return answer;
    }

    /// The value a class attribute of `key`'s chain is assigned, with the
    /// module to read it in, depth first from the left like these
    /// single-inheritance trees' MRO; empty when none is found.
    private Object[] classAttr(String key, String attr, int depth) {
        ClassDef c = classOf(key);
        if (c == null || depth > 32) {
            return new Object[0];
        }
        for (Stmt s : bodyOf(c)) {
            if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name
                    && attr.equals(((Name) ((Assign) s).targets.get(0)).id)) {
                return new Object[] {moduleOf(key), ((Assign) s).value};
            }
        }
        for (Expr b : c.bases) {
            String r = resolveExpr(moduleOf(key), b);
            if (r != null && !r.isEmpty()) {
                Object[] v = classAttr(r, attr, depth + 1);
                if (v.length != 0) {
                    return v;
                }
            }
        }
        return new Object[0];
    }

    /// Whether the type `e` names (in `mod`) has `to_tree`: 1, 0 or
    /// [#UNKNOWN].
    private int hasToTree(String mod, Expr e) {
        if (e instanceof Call && ((Call) e).func instanceof Name && "type".equals(((Name) ((Call) e).func).id)) {
            return 0;
        }
        if (e instanceof Call) {
            // An instance of a program class (Set(values=...)): what its
            // class answers, as hasattr finds no instance to_tree there.
            String k = resolveExpr(mod, ((Call) e).func);
            return k == null || k.isEmpty() || classOf(k) == null ? UNKNOWN : definesToTree(k, 0);
        }
        String r = resolveExpr(mod, e);
        if (r == null) {
            return UNKNOWN;
        }
        return r.isEmpty() ? 0 : definesToTree(r, 0);
    }

    private int definesToTree(String key, int depth) {
        ClassDef c = classOf(key);
        if (c == null || depth > 32) {
            return UNKNOWN;
        }
        for (Stmt s : bodyOf(c)) {
            if (s instanceof FunctionDef && "to_tree".equals(((FunctionDef) s).name)
                    || s instanceof Assign && ((Assign) s).targets.get(0) instanceof Name
                    && "to_tree".equals(((Name) ((Assign) s).targets.get(0)).id)) {
                return 1;
            }
        }
        int answer = 0;
        for (Expr b : c.bases) {
            String r = resolveExpr(moduleOf(key), b);
            if (r == null) {
                answer = UNKNOWN;
            } else if (!r.isEmpty()) {
                int d = definesToTree(r, depth + 1);
                if (d == 1) {
                    return 1;
                }
                if (d == UNKNOWN) {
                    answer = UNKNOWN;
                }
            }
        }
        return answer;
    }

    // ---- classification --------------------------------------------------

    /// What a class-body value is to `MetaSerialisable`.
    private enum Role { NOT_DESCRIPTOR, ATTR, ELEMENT, NONE, UNKNOWN }

    private Role role(String mod, Expr v, Set<String> classNames) {
        if (v instanceof Name) {
            String id = ((Name) v).id;
            if (classNames.contains(id) || PLAIN_BUILTINS.contains(id) && bindings.get(mod).getOrDefault(id, 0) == 0
                    || constantName(mod, id, 0)) {
                return Role.NOT_DESCRIPTOR;
            }
            // A class object is never a descriptor instance.
            String k = resolve(mod, id, 0);
            return k != null && !k.isEmpty() && classOf(k) != null ? Role.NOT_DESCRIPTOR : Role.UNKNOWN;
        }
        Object[] shared = deref(mod, v, 0);
        if (shared != null) {
            return shared[1] == null ? Role.NOT_DESCRIPTOR : role((String) shared[0], (Expr) shared[1], new HashSet<String>());
        }
        if (v instanceof Attribute || v instanceof Node.Subscript) {
            return Role.UNKNOWN;
        }
        if (!(v instanceof Call)) {
            return Role.NOT_DESCRIPTOR;
        }
        Call c = (Call) v;
        String k = resolveExpr(mod, c.func);
        if (k == null) {
            return Role.UNKNOWN;
        }
        if (k.isEmpty()) {
            return Role.NOT_DESCRIPTOR;
        }
        int d = derives(k, DESCRIPTOR, 0);
        if (d != 1) {
            return d == 0 ? Role.NOT_DESCRIPTOR : Role.UNKNOWN;
        }
        if (derives(k, ALIAS, 0) == 1) {
            return Role.NONE;
        }
        Expr nested = keyword(c, "nested");
        if (nested == null) {
            Object[] a = classAttr(k, "nested", 0);
            nested = a.length == 0 ? null : (Expr) a[1];
        }
        if (nested != null && !(nested instanceof Constant)) {
            return Role.UNKNOWN;
        }
        if (nested != null && ((Constant) nested).kind == Constant.Kind.TRUE) {
            return Role.ELEMENT;
        }
        int seq = derives(k, SEQUENCE, 0);
        if (seq != 0) {
            return seq == 1 ? Role.ELEMENT : Role.UNKNOWN;
        }
        int typed = derives(k, TYPED, 0);
        if (typed == UNKNOWN) {
            return Role.UNKNOWN;
        }
        if (typed == 0) {
            return Role.ATTR;
        }
        String tmod = mod;
        Expr t = keyword(c, "expected_type");
        if (t == null) {
            Object[] a = classAttr(k, "expected_type", 0);
            if (a.length == 0) {
                return Role.UNKNOWN;
            }
            tmod = (String) a[0];
            t = (Expr) a[1];
        }
        if (t instanceof Node.TupleExpr) {
            // The metaclass binds a tuple-typed field neither way.
            return Role.NONE;
        }
        int tt = hasToTree(tmod, t);
        return tt == UNKNOWN ? Role.UNKNOWN : tt == 1 ? Role.ELEMENT : Role.ATTR;
    }

    private static Expr keyword(Call c, String name) {
        for (Keyword k : c.keywords) {
            if (name.equals(k.arg)) {
                return k.value;
            }
        }
        return null;
    }

    // ---- lowering --------------------------------------------------------

    /// Whether `v`, a descriptor of program class `k`, is nested to the
    /// metaclass: 1, 0, or [#UNKNOWN].
    private int nestedOf(Call v, String k) {
        Expr nested = keyword(v, "nested");
        if (nested == null) {
            Object[] a = classAttr(k, "nested", 0);
            nested = a.length == 0 ? null : (Expr) a[1];
        }
        if (nested == null) {
            return 0;
        }
        if (!(nested instanceof Constant)) {
            return UNKNOWN;
        }
        Constant.Kind kind = ((Constant) nested).kind;
        return kind == Constant.Kind.TRUE ? 1 : kind == Constant.Kind.FALSE || kind == Constant.Kind.NONE ? 0 : UNKNOWN;
    }

    /// The namespace of descriptor `v` of program class `k`: a one-element
    /// array holding the string or null, or an empty array when it cannot
    /// be told.
    private String[] namespaceOf(String mod, Call v, String k) {
        Expr ns = keyword(v, "namespace");
        String nsMod = mod;
        if (ns == null) {
            Object[] a = classAttr(k, "namespace", 0);
            ns = a.length == 0 ? null : (Expr) a[1];
            nsMod = a.length == 0 ? mod : (String) a[0];
        }
        if (ns instanceof Name) {
            // A module constant (REL_NS): its string.
            Constant c = constantOf(nsMod, ((Name) ns).id, 0);
            ns = c == null ? ns : c;
        }
        if (ns == null || ns instanceof Constant && ((Constant) ns).kind == Constant.Kind.NONE) {
            return new String[] {null};
        }
        if (ns instanceof Constant && ((Constant) ns).kind == Constant.Kind.STR) {
            return new String[] {((Constant) ns).text.isEmpty() ? null : ((Constant) ns).text};
        }
        return new String[0];
    }

    /// The (name, value) pairs of an assignment to several plain names, or
    /// of a tuple of names from a tuple display of the same length, whose
    /// values are constants or displays; null for anything else.
    private static List<Object[]> unpacked(Assign a) {
        List<Object[]> out = new ArrayList<Object[]>();
        for (Expr t : a.targets) {
            if (t instanceof Name) {
                out.add(new Object[] {((Name) t).id, a.value});
            } else if (t instanceof Node.TupleExpr && a.value instanceof Node.TupleExpr
                    && ((Node.TupleExpr) t).elts.size() == ((Node.TupleExpr) a.value).elts.size()) {
                for (int i = 0; i < ((Node.TupleExpr) t).elts.size(); i++) {
                    Expr n = ((Node.TupleExpr) t).elts.get(i);
                    if (!(n instanceof Name)) {
                        return null;
                    }
                    out.add(new Object[] {((Name) n).id, ((Node.TupleExpr) a.value).elts.get(i)});
                }
            } else {
                return null;
            }
        }
        for (Object[] pr : out) {
            Expr v = (Expr) pr[1];
            if (!(v instanceof Constant || literalNames(v) != null)) {
                return null;
            }
        }
        return out;
    }

    /// The names a class-body value of `mod` lists: a display of strings,
    /// another program class's entry (`Base.__elements__`), or a `+` of
    /// those; null otherwise.
    private List<String> namesOf(String mod, Expr e, int depth) {
        if (depth > 16) {
            return null;
        }
        if (e instanceof Node.BinOp && "+".equals(((Node.BinOp) e).op)) {
            List<String> l = namesOf(mod, ((Node.BinOp) e).left, depth + 1);
            List<String> r = l == null ? null : namesOf(mod, ((Node.BinOp) e).right, depth + 1);
            if (r == null) {
                return null;
            }
            List<String> out = new ArrayList<String>(l);
            out.addAll(r);
            return out;
        }
        if (e instanceof Attribute) {
            Object[] d = deref(mod, e, depth + 1);
            return d == null || d[1] == null || d[1] instanceof Call ? null
                    : namesOf((String) d[0], (Expr) d[1], depth + 1);
        }
        return literalNames(e);
    }

    /// The strings of a tuple or list display of string constants, or null.
    private static List<String> literalNames(Expr e) {
        List<Expr> elts = e instanceof Node.TupleExpr ? ((Node.TupleExpr) e).elts
                : e instanceof Node.ListExpr ? ((Node.ListExpr) e).elts : null;
        if (elts == null) {
            return null;
        }
        List<String> out = new ArrayList<String>();
        for (Expr x : elts) {
            if (!(x instanceof Constant) || ((Constant) x).kind != Constant.Kind.STR) {
                return null;
            }
            out.add(((Constant) x).text);
        }
        return out;
    }

    /// Lowers class `c` of module `mod`; true when it now needs the
    /// serialisation helpers [#HELPERS] imports.
    private boolean lowerClass(String mod, ClassDef c) {
        String key = mod + ":" + c.name;
        boolean serial = derives(key, SERIALISABLE, 0) == 1;
        boolean strict = serial || derives(key, STRICT, 0) == 1;
        for (Keyword k : c.keywords) {
            if ("metaclass".equals(k.arg)) {
                String r = resolveExpr(mod, k.value);
                serial |= META_SERIALISABLE.equals(r);
                strict |= serial || META_STRICT.equals(r);
            }
        }
        if (!strict || SERIALISABLE.equals(key)) {
            return false;
        }
        if (!dry && passes.contains(key) && !allowed.contains(key)) {
            // A subclass left alone would inherit the generated methods,
            // which are written for this class's dictionary alone.
            report.add(mod + ":" + c.line + " " + c.name + ": SKIP subclass not lowered");
            return false;
        }
        // The class dictionary in definition order, as the metaclass sees it.
        Map<String, Expr> entries = new LinkedHashMap<String, Expr>();
        // The module a shared entry (`x = Base.x`) was declared in.
        Map<String, String> entryMod = new HashMap<String, String>();
        Map<String, Integer> stores = new HashMap<String, Integer>();
        Set<String> functions = new HashSet<String>();
        Set<String> loads = new HashSet<String>();
        boolean plain = true;
        for (Stmt s : c.body) {
            if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name) {
                String n = ((Name) ((Assign) s).targets.get(0)).id;
                stores.merge(n, 1, Integer::sum);
                Expr val = ((Assign) s).value;
                Object[] sh = entries.containsKey(n) ? null : deref(mod, val, 0);
                if (sh != null && sh[1] != null) {
                    val = (Expr) sh[1];
                    entryMod.put(n, (String) sh[0]);
                }
                entries.putIfAbsent(n, val);
                names(((Assign) s).value, loads);
            } else if (s instanceof FunctionDef || s instanceof ClassDef) {
                String n = s instanceof FunctionDef ? ((FunctionDef) s).name : ((ClassDef) s).name;
                stores.merge(n, 1, Integer::sum);
                functions.add(n);
                // A later def replaces an earlier entry in the dictionary.
                entries.remove(n);
                entryMod.remove(n);
                for (Expr d : s instanceof FunctionDef ? ((FunctionDef) s).decorators : ((ClassDef) s).decorators) {
                    names(d, loads);
                }
            } else if (s instanceof Assign && unpacked((Assign) s) != null) {
                // `a = b = ()` and `a, b = None, None`: one entry per name,
                // each a constant or a display, so never a descriptor.
                for (Object[] pr : unpacked((Assign) s)) {
                    String n = (String) pr[0];
                    stores.merge(n, 1, Integer::sum);
                    entries.putIfAbsent(n, (Expr) pr[1]);
                }
                names(((Assign) s).value, loads);
            } else if (s instanceof Node.ExprStmt && entryCall(((Node.ExprStmt) s).value, entries)) {
                // textRotation.values.add(255): mutates what an entry holds,
                // binds nothing; the entry is then read in the class body.
                names(s, loads);
            } else if (!(s instanceof Node.ExprStmt && ((Node.ExprStmt) s).value instanceof Constant
                    || s instanceof Node.Pass)) {
                plain = false;
                names(s, loads);
            }
        }
        Set<String> initialised = initialised(key);
        List<String> attrs = new ArrayList<String>();
        List<String> elements = new ArrayList<String>();
        List<String> nested = new ArrayList<String>();
        List<String[]> namespaced = new ArrayList<String[]>();
        Map<String, String> elementKind = new HashMap<String, String>();
        Map<String, Stmt[]> replace = new LinkedHashMap<String, Stmt[]>();
        List<String> aliasList = new ArrayList<String>();
        Map<String, Boolean> descs = new LinkedHashMap<String, Boolean>();
        boolean classified = plain;
        for (Map.Entry<String, Expr> e : entries.entrySet()) {
            String n = e.getKey();
            Expr v = e.getValue();
            String em = entryMod.getOrDefault(n, mod);
            Role r = role(em, v, functions);
            if (r == Role.UNKNOWN) {
                report.add(mod + ":" + v.line + " " + c.name + "." + n + ": UNKNOWN role " + resolveExpr(mod, v instanceof Call ? ((Call) v).func : v));
                classified = false;
                continue;
            }
            if (r == Role.NOT_DESCRIPTOR) {
                continue;
            }
            descs.put(n, r == Role.NONE);
            String k = resolveExpr(em, ((Call) v).func);
            String[] ns = namespaceOf(em, (Call) v, k);
            Expr nsKw = keyword((Call) v, "namespace");
            Expr own = nsKw instanceof Name ? entries.get(((Name) nsKw).id) : null;
            if (own instanceof Name) {
                own = constantOf(mod, ((Name) own).id, 0);
            }
            if (ns.length == 0 && own instanceof Constant) {
                // namespace=namespace: the class body's own constant.
                Constant nc = (Constant) own;
                ns = nc.kind == Constant.Kind.NONE ? new String[] {null}
                        : nc.kind == Constant.Kind.STR ? new String[] {nc.text.isEmpty() ? null : nc.text} : ns;
            }
            int isNested = nestedOf((Call) v, k);
            if (ns.length == 0 || isNested == UNKNOWN) {
                report.add(mod + ":" + v.line + " " + c.name + "." + n + ": UNKNOWN ns/nested " + k);
                classified = false;
                continue;
            }
            if (ns[0] != null) {
                namespaced.add(new String[] {n, "{" + ns[0] + "}" + n});
            }
            if (r == Role.ATTR) {
                attrs.add(n);
            } else if (r == Role.ELEMENT) {
                elements.add(n);
                if (isNested == 1) {
                    nested.add(n);
                }
                elementKind.put(n, derives(k, BASE_NESTED_SEQUENCE, 0) == 1 ? "nestedseq"
                        : derives(k, SEQUENCE, 0) == 1 ? "seq" : "other");
            }
            if (r == Role.ELEMENT || ns[0] != null) {
                continue;
            }
            String why = !em.equals(mod) ? "shared from " + em : stores.get(n) != 1 ? "stored twice" : loads.contains(n) ? "read in the class body"
                    : dotted.contains(c.name + "." + n) ? "read as " + c.name + "." + n
                    : n.startsWith("__") ? "private name"
                    : r == Role.ATTR && !initialised.contains(n) ? "not set by __init__" : null;
            String src = why == null ? lowering(mod, c, n, (Call) v) : null;
            if (src != null && stores.containsKey("property")) {
                // A class binding property (property = Sequence(...)) would
                // hand the bare decorator its own descriptor: decorate with
                // the module-level alias of the builtin instead.
                src = src.replace("@property\n", "@" + Infer.PROPERTY_ALIAS + "\n");
                if (!dry) {
                    propertyAlias.add(mod);
                }
            }
            if (src == null) {
                if (why != null) {
                    report.add(mod + ":" + v.line + " " + c.name + "." + n + ": " + why);
                }
                continue;
            }
            if (r != Role.ATTR) {
                aliasList.add(n);
            }
            Node.Module parsed = Parser.parse(src, mod.replace('.', '/') + ".py");
            DataclassInit.atLine(parsed, v.line);
            replace.put(n, parsed.body.toArray(new Stmt[0]));
        }
        List<String> attrsT = attrs;
        List<String> elementsT = sorted(elements);
        List<String> nestedT = sorted(nested);
        Set<String> written = new HashSet<String>(Arrays.asList("__attrs__", "__elements__", "__nested__"));
        Map<String, String> from = new HashMap<String, String>();
        if (serial) {
            for (String t : new String[] {"__attrs__", "__elements__", "__nested__"}) {
                Expr own = entries.get(t);
                if (own == null || own instanceof Constant && ((Constant) own).kind == Constant.Kind.NONE
                        && stores.get(t) == 1) {
                    continue;
                }
                List<String> lit = stores.get(t) == 1 ? namesOf(entryMod.getOrDefault(t, mod), own, 0) : null;
                if (lit == null) {
                    classified = false;
                } else if ("__attrs__".equals(t)) {
                    attrsT = lit;
                } else if ("__elements__".equals(t)) {
                    elementsT = lit;
                } else {
                    nestedT = lit;
                }
                written.remove(t);
            }
            if (!classified) {
                report.add(mod + ":" + c.line + " " + c.name + ": SKIP " + (!plain ? "nonplain body" : "unclassified"));
                return false;
            }
            Set<String> shadow = new HashSet<String>(functions);
            shadow.addAll(entries.keySet());
            if (!inherited(key, descs, shadow, 0, from)) {
                // A base whose dictionary cannot be read: the descriptor a
                // child tag names is not known here.
                report.add(mod + ":" + c.line + " " + c.name + ": SKIP base unresolved");
                return false;
            }
        }
        if (dry) {
            passes.add(key);
            return false;
        }
        Set<String> mine = new HashSet<String>(replace.keySet());
        mine.removeAll(aliasList);
        loweredOf.put(key, mine);
        List<Stmt> body = new ArrayList<Stmt>();
        for (Stmt s : c.body) {
            if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name
                    && replace.containsKey(((Name) ((Assign) s).targets.get(0)).id)) {
                String n = ((Name) ((Assign) s).targets.get(0)).id;
                body.addAll(Arrays.asList(replace.get(n)));
                if (aliasList.contains(n)) {
                    aliases++;
                } else {
                    lowered++;
                }
            } else {
                body.add(s);
            }
        }
        if (serial) {
            Set<String> loweredAttrs = new HashSet<String>(replace.keySet());
            loweredAttrs.removeAll(aliasList);
            for (String n : replace.keySet()) {
                descs.remove(n);
            }
            for (Map.Entry<String, String> f : from.entrySet()) {
                // A field a base turned into a property: no descriptor
                // answers its tag any more, the property's slot holds it.
                if (loweredOf.getOrDefault(f.getValue(), new HashSet<String>()).contains(f.getKey())) {
                    loweredAttrs.add(f.getKey());
                    descs.remove(f.getKey());
                }
            }
            List<String> copied = copiedNames(key, descs.keySet(), loweredAttrs);
            Set<String> custom = new HashSet<String>(functions);
            custom.addAll(entries.keySet());
            ancestorNames(key, custom, 0);
            Set<String> below = new HashSet<String>();
            descendantNames(key, below, 0);
            String src = SerialisableMethods.methods(attrsT, elementsT, nestedT, namespaced, elementKind, written,
                    loweredAttrs, aliasList, custom, below, initSignature(key), descs, copied);
            serialDone.add(key);
            Node.Module parsed = Parser.parse(src, mod.replace('.', '/') + ".py");
            DataclassInit.atLine(parsed, c.line);
            body.addAll(parsed.body);
            synthesized++;
        }
        c.body.clear();
        c.body.addAll(body);
        return serial;
    }

    /// Whether `e` is a call of a method reached from a class-body entry.
    private static boolean entryCall(Expr e, Map<String, Expr> entries) {
        if (!(e instanceof Call) || !(((Call) e).func instanceof Attribute)) {
            return false;
        }
        Expr o = ((Attribute) ((Call) e).func).value;
        while (o instanceof Attribute) {
            o = ((Attribute) o).value;
        }
        return o instanceof Name && entries.containsKey(((Name) o).id);
    }

    /// The names class `key` binds in its own body, as parsed.
    private Set<String> ownNames(String key) {
        Set<String> out = new HashSet<String>();
        ClassDef c = classOf(key);
        if (c == null) {
            return out;
        }
        for (Stmt s : bodyOf(c)) {
            if (s instanceof FunctionDef) {
                out.add(((FunctionDef) s).name);
            } else if (s instanceof Assign) {
                for (Expr t : ((Assign) s).targets) {
                    if (t instanceof Name) {
                        out.add(((Name) t).id);
                    }
                }
            }
        }
        return out;
    }

    /// Adds what the program bases of `key` below `Serialisable` bind.
    private void ancestorNames(String key, Set<String> out, int depth) {
        ClassDef c = classOf(key);
        if (c == null || depth > 32) {
            return;
        }
        for (Expr b : c.bases) {
            String r = resolveExpr(moduleOf(key), b);
            if (r != null && !r.isEmpty() && !SERIALISABLE.equals(r) && classOf(r) != null) {
                out.addAll(ownNames(r));
                ancestorNames(r, out, depth + 1);
            }
        }
    }

    /// Adds what the program subclasses of `key`, at any depth, bind.
    private void descendantNames(String key, Set<String> out, int depth) {
        if (depth > 32) {
            return;
        }
        for (String ch : children.getOrDefault(key, new ArrayList<String>())) {
            out.addAll(ownNames(ch));
            descendantNames(ch, out, depth + 1);
        }
    }

    /// Replaces `Serialisable`'s generic methods, which read each field
    /// with a computed getattr, by calls of the per-class `_dm_` methods,
    /// once every model class has them: a subclass reaches a base method
    /// only through `super()` from a method of its own lineage, and every
    /// such lineage was generated under the `_dm_` names.
    private void replaceSerialisableBase() {
        for (Map.Entry<String, Node.Module> e : modules.entrySet()) {
            for (Stmt s : e.getValue().body) {
                if (s instanceof ClassDef) {
                    String k = e.getKey() + ":" + ((ClassDef) s).name;
                    if (!SERIALISABLE.equals(k) && derives(k, SERIALISABLE, 0) == 1 && !serialDone.contains(k)) {
                        report.add(e.getKey() + ":" + s.line + " " + ((ClassDef) s).name
                                + ": not generated, Serialisable keeps its generic methods");
                        return;
                    }
                }
            }
        }
        ClassDef base = classOf(SERIALISABLE);
        if (base == null) {
            return;
        }
        Set<String> gone = SerialisableMethods.DISPATCH.keySet();
        List<Stmt> kept = new ArrayList<Stmt>();
        for (Stmt s : base.body) {
            if (!(s instanceof FunctionDef && gone.contains(((FunctionDef) s).name))) {
                kept.add(s);
            }
        }
        Node.Module parsed = Parser.parse(SerialisableMethods.baseSource(), "openpyxl/descriptors/serialisable.py");
        DataclassInit.atLine(parsed, base.line);
        kept.addAll(parsed.body);
        base.body.clear();
        base.body.addAll(kept);
    }

    /// Adds to `out` the descriptors the program bases of `key` declare,
    /// nearest first, that nothing nearer (`shadow`) binds: what
    /// `getattr(cls, tag)` finds through the bases. False when a base or
    /// one of its entries cannot be classified.
    private boolean inherited(String key, Map<String, Boolean> out, Set<String> shadow, int depth,
            Map<String, String> from) {
        ClassDef c = classOf(key);
        if (c == null || depth > 32) {
            return false;
        }
        for (Expr b : c.bases) {
            String r = resolveExpr(moduleOf(key), b);
            if (r == null) {
                return false;
            }
            if (r.isEmpty() || SERIALISABLE.equals(r) || STRICT.equals(r)) {
                continue;
            }
            ClassDef bc = classOf(r);
            if (bc == null) {
                return false;
            }
            Set<String> fns = new HashSet<String>();
            for (Stmt s : bodyOf(bc)) {
                if (s instanceof FunctionDef) {
                    fns.add(((FunctionDef) s).name);
                } else if (s instanceof ClassDef) {
                    fns.add(((ClassDef) s).name);
                }
            }
            List<String> bound = new ArrayList<String>(fns);
            for (Stmt s : bodyOf(bc)) {
                if (s instanceof Assign && ((Assign) s).targets.size() == 1
                        && ((Assign) s).targets.get(0) instanceof Name) {
                    String n = ((Name) ((Assign) s).targets.get(0)).id;
                    bound.add(n);
                    Role role = role(moduleOf(r), ((Assign) s).value, fns);
                    if (role == Role.UNKNOWN) {
                        return false;
                    }
                    if (role != Role.NOT_DESCRIPTOR && !shadow.contains(n) && !out.containsKey(n)) {
                        out.put(n, role == Role.NONE);
                        from.put(n, r);
                    }
                }
            }
            shadow.addAll(bound);
            if (!inherited(r, out, shadow, depth + 1, from)) {
                return false;
            }
        }
        return true;
    }

    /// The instance dictionary keys `__copy__` carries over beside what it
    /// serialises: the descriptor fields (stored under their own name),
    /// the slots of the lowered ones and every `self.x` the methods of the
    /// class and its program bases store, in that order.
    private List<String> copiedNames(String key, Set<String> descNames, Set<String> lowered) {
        Set<String> out = new LinkedHashSet<String>(descNames);
        for (String n : lowered) {
            out.add("_dm_" + n);
        }
        String at = key;
        for (int depth = 0; at != null && depth < 32 && !SERIALISABLE.equals(at); depth++) {
            ClassDef k = classOf(at);
            if (k == null) {
                break;
            }
            String owner = k.name;
            Set<String> stores = new LinkedHashSet<String>();
            for (Stmt s : bodyOf(k)) {
                if (s instanceof FunctionDef) {
                    selfStores(s, stores);
                }
            }
            for (String n : stores) {
                if (lowered.contains(n) && at.equals(key)) {
                    out.add("_dm_" + n);
                } else if (n.startsWith("__") && !n.endsWith("__")) {
                    int i = 0;
                    while (i < owner.length() && owner.charAt(i) == '_') {
                        i++;
                    }
                    out.add("_" + owner.substring(i) + n);
                } else {
                    out.add(n);
                }
            }
            String next = null;
            for (Expr b : k.bases) {
                String r = resolveExpr(moduleOf(at), b);
                if (r != null && !r.isEmpty() && next == null && classOf(r) != null) {
                    next = r;
                }
            }
            at = next;
        }
        return new ArrayList<String>(out);
    }

    private static void selfStores(Object o, Set<String> out) {
        if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                if (t instanceof Attribute && ((Attribute) t).value instanceof Name
                        && "self".equals(((Name) ((Attribute) t).value).id)) {
                    out.add(((Attribute) t).attr);
                }
            }
        }
        Infer.children(o, x -> selfStores(x, out));
    }

    private static List<String> sorted(List<String> in) {
        List<String> out = new ArrayList<String>(in);
        java.util.Collections.sort(out);
        return out;
    }

    /// The fields every instance of program class `key` has stored once
    /// its `__init__` returns: the `self.x = ...` statements at the top
    /// level of the nearest `__init__` in the chain. Only those may lose
    /// the descriptor a read before any store would have answered.
    /// The keyword parameters of the nearest `__init__` of `key`, each as
    /// name and default source (null where required); null when a call by
    /// keywords alone cannot stand for `cls(**attrib)` -- a `**` parameter,
    /// positional-only ones, a default that is not a constant.
    private List<String[]> initSignature(String key) {
        FunctionDef f = nearestInit(key);
        if (f == null || f.args.kwarg != null || f.args.positionalOnly > 0 || f.args.positional.isEmpty()) {
            return null;
        }
        List<String[]> out = new ArrayList<String[]>();
        int first = f.args.positional.size() - f.args.defaults.size();
        for (int i = 1; i < f.args.positional.size(); i++) {
            String d = null;
            if (i >= first) {
                d = constantText(f.args.defaults.get(i - first));
                if (d == null) {
                    return null;
                }
            }
            out.add(new String[] {f.args.positional.get(i).name, d});
        }
        for (int i = 0; i < f.args.kwonly.size(); i++) {
            Expr dv = f.args.kwDefaults.get(i);
            String d = dv == null ? null : constantText(dv);
            if (dv != null && d == null) {
                return null;
            }
            out.add(new String[] {f.args.kwonly.get(i).name, d});
        }
        return out;
    }

    /// The nearest `__init__` along the single-base chain of `key`, or null.
    private FunctionDef nearestInit(String key) {
        String at = key;
        for (int depth = 0; at != null && depth < 32; depth++) {
            ClassDef c = classOf(at);
            if (c == null) {
                return null;
            }
            for (Stmt s : bodyOf(c)) {
                if (s instanceof FunctionDef && "__init__".equals(((FunctionDef) s).name)) {
                    return (FunctionDef) s;
                }
            }
            String next = null;
            if (c.bases.size() == 1) {
                String r = resolveExpr(moduleOf(at), c.bases.get(0));
                next = r == null || r.isEmpty() ? null : r;
            }
            at = next;
        }
        return null;
    }

    private Set<String> initialised(String key) {
        Set<String> out = new HashSet<String>();
        String at = key;
        for (int depth = 0; at != null && depth < 32; depth++) {
            ClassDef c = classOf(at);
            if (c == null) {
                return out;
            }
            for (Stmt s : bodyOf(c)) {
                if (s instanceof FunctionDef && "__init__".equals(((FunctionDef) s).name)) {
                    for (Stmt b : ((FunctionDef) s).body) {
                        if (b instanceof Assign) {
                            for (Expr t : ((Assign) b).targets) {
                                if (t instanceof Attribute && ((Attribute) t).value instanceof Name
                                        && "self".equals(((Name) ((Attribute) t).value).id)) {
                                    out.add(((Attribute) t).attr);
                                }
                            }
                        }
                    }
                    return out;
                }
            }
            String next = null;
            if (c.bases.size() == 1) {
                String r = resolveExpr(moduleOf(at), c.bases.get(0));
                next = r == null || r.isEmpty() ? null : r;
            }
            at = next;
        }
        return out;
    }

    private static void names(Object o, Set<String> out) {
        if (o instanceof Name) {
            out.add(((Name) o).id);
        }
        if (o instanceof FunctionDef) {
            return;
        }
        Infer.children(o, x -> names(x, out));
    }

    /// The Python that replaces field `n` of class `c`, or null (with a
    /// report line when the cause is an argument) to leave it alone.
    private String lowering(String mod, ClassDef c, String n, Call v) {
        String k = resolveExpr(mod, v.func);
        if (k == null || !k.startsWith(BASE)) {
            return null;
        }
        String kind = k.substring(BASE.length());
        String where = mod + ":" + v.line + " " + c.name + "." + n + " " + kind + ": ";
        if (!KINDS.contains(kind)) {
            return null;
        }
        if ("Alias".equals(kind)) {
            if (v.args.size() != 1 || !v.keywords.isEmpty() || !(v.args.get(0) instanceof Constant)
                    || ((Constant) v.args.get(0)).kind != Constant.Kind.STR) {
                report.add(where + "non-constant alias");
                return null;
            }
            String a = ((Constant) v.args.get(0)).text;
            return "@property\ndef " + n + "(self):\n    return self." + a + "\n@" + n + ".setter\ndef " + n
                    + "(self, value):\n    self." + a + " = value\n";
        }
        if (!v.args.isEmpty()) {
            report.add(where + "positional argument");
            return null;
        }
        boolean allowNone = false;
        String type = null;
        String values = null;
        for (Keyword kw : v.keywords) {
            if ("allow_none".equals(kw.arg) && !kind.endsWith("Set")) {
                if (!(kw.value instanceof Constant) || ((Constant) kw.value).kind != Constant.Kind.TRUE
                        && ((Constant) kw.value).kind != Constant.Kind.FALSE) {
                    report.add(where + "non-constant allow_none");
                    return null;
                }
                allowNone = ((Constant) kw.value).kind == Constant.Kind.TRUE;
            } else if ("expected_type".equals(kw.arg) && "Typed".equals(kind)) {
                type = typeText(mod, c, kw.value);
                if (type == null) {
                    report.add(where + "expected_type not a plain type");
                    return null;
                }
            } else if ("values".equals(kw.arg) && kind.endsWith("Set")) {
                values = setText(kw.value, "NoneSet".equals(kind));
                if (values == null) {
                    report.add(where + "non-constant values");
                    return null;
                }
            } else {
                report.add(where + "keyword " + kw.arg);
                return null;
            }
        }
        StringBuilder b = new StringBuilder();
        b.append("_dm_").append(n).append(" = None\n@property\ndef ").append(n).append("(self):\n    return self._dm_")
                .append(n).append("\n@").append(n).append(".setter\ndef ").append(n).append("(self, value):\n");
        switch (kind) {
            case "Typed":
                if (type == null) {
                    return null;
                }
                typed(b, n, type, allowNone);
                break;
            case "String":
                typed(b, n, "str", allowNone);
                break;
            case "Integer":
                convertible(b, n, "int", allowNone, false);
                break;
            case "Float":
                convertible(b, n, "float", allowNone, false);
                break;
            case "Text":
                convertible(b, n, "str", allowNone, false);
                break;
            case "Bool":
                convertible(b, n, "bool", allowNone, true);
                break;
            case "Set":
            case "NoneSet":
                if (values == null) {
                    return null;
                }
                if ("NoneSet".equals(kind)) {
                    b.append("    if value == 'none':\n        value = None\n");
                }
                b.append("    if value not in ").append(values).append(":\n        raise ValueError(")
                        .append("'Value must be one of {0}'.format(").append(values).append("))\n");
                b.append("    self._dm_").append(n).append(" = value\n");
                break;
            default:
                return null;
        }
        return b.toString();
    }

    /// `Typed.__set__`, with the message it raises when `DEBUG` is off.
    /// Each store sits under the test that proves its type, so the slot
    /// holds the expected type (or None) whatever the callers pass.
    private static void typed(StringBuilder b, String n, String type, boolean allowNone) {
        String store = "        self._dm_" + n + " = ";
        b.append("    if isinstance(value, ").append(type).append("):\n").append(store).append("value\n        return\n");
        if (allowNone) {
            b.append("    if value is None:\n").append(store).append("None\n        return\n");
        }
        b.append("    raise TypeError(str(self.__class__) + ").append(quote("." + n + " should be ")).append(" + str(")
                .append(type).append(") + ' but value is ' + str(value.__class__))\n");
    }

    /// `Convertible.__set__` through `_convert`, storing the converted
    /// local rather than the rebound parameter, so the slot's type is the
    /// constructor's whatever the callers pass.
    private static void convertible(StringBuilder b, String n, String type, boolean allowNone, boolean bool) {
        String store = "        self._dm_" + n + " = ";
        if (bool) {
            // Bool.__set__ runs before Convertible's, None or not; False
            // then converts to itself.
            b.append("    if isinstance(value, str) and value in ('false', 'f', '0'):\n").append(store)
                    .append("False\n        return\n");
        }
        if (allowNone) {
            b.append("    if value is None:\n").append(store).append("None\n        return\n");
        }
        b.append("    if isinstance(value, ").append(type).append("):\n").append(store).append("value\n        return\n")
                .append("    try:\n        v = ").append(type).append("(value)\n    except:\n")
                .append("        raise TypeError('expected ' + str(").append(type).append("))\n    self._dm_").append(n)
                .append(" = v\n");
    }

    /// Source for an `expected_type` the setter can name from module
    /// scope, or null.
    private String typeText(String mod, ClassDef c, Expr e) {
        if (!EXTERNAL.equals(resolveExpr(mod, e))) {
            // A program type might have a from_tree the generic
            // Serialisable.from_tree would call; only foreign types surely
            // have none.
            return null;
        }
        if (e instanceof Name) {
            String id = ((Name) e).id;
            int bound = bindings.get(mod).getOrDefault(id, 0);
            for (Stmt s : bodyOf(c)) {
                if (s instanceof Assign && ((Assign) s).targets.get(0) instanceof Name
                        && id.equals(((Name) ((Assign) s).targets.get(0)).id)) {
                    return null;
                }
            }
            return bound == 1 || bound == 0 && BUILTIN_TYPES.contains(id) ? id : null;
        }
        if (e instanceof Attribute) {
            String inner = typeText(mod, c, ((Attribute) e).value);
            return inner == null ? null : inner + "." + ((Attribute) e).attr;
        }
        return null;
    }

    /// A set display of the constants of `e`, plus `None` for a `NoneSet`.
    private static String setText(Expr e, boolean none) {
        List<Expr> elts;
        if (e instanceof Node.TupleExpr) {
            elts = ((Node.TupleExpr) e).elts;
        } else if (e instanceof Node.ListExpr) {
            elts = ((Node.ListExpr) e).elts;
        } else if (e instanceof Node.SetExpr) {
            elts = ((Node.SetExpr) e).elts;
        } else {
            return null;
        }
        StringBuilder b = new StringBuilder("{");
        for (Expr x : elts) {
            String t = constantText(x);
            if (t == null) {
                return null;
            }
            b.append(t).append(", ");
        }
        if (none) {
            b.append("None, ");
        }
        if (b.length() == 1) {
            return "set()";
        }
        return b.append('}').toString();
    }

    private static String constantText(Expr x) {
        if (!(x instanceof Constant)) {
            return null;
        }
        Constant k = (Constant) x;
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
                return quote(k.text);
            default:
                return null;
        }
    }

    /// A Python string literal for `s`, ASCII only.
    static String quote(String s) {
        StringBuilder b = new StringBuilder("'");
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'' || ch == '\\') {
                b.append('\\').append(ch);
            } else if (ch < 0x20 || ch > 0x7e) {
                b.append(String.format("\\u%04x", (int) ch));
            } else {
                b.append(ch);
            }
        }
        return b.append('\'').toString();
    }
}
