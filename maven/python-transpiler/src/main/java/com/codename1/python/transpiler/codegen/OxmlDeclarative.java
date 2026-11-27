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
import com.codename1.python.transpiler.ast.Node.BinOp;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.ImportFrom;
import com.codename1.python.transpiler.ast.Node.Keyword;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Slice;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.ast.Node.Subscript;
import com.codename1.python.transpiler.ast.Node.UnaryOp;
import com.codename1.python.transpiler.parser.Parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Evaluates xmlchemy-style declarative element classes at build time.
///
/// python-pptx (and python-docx, which shares the design) declare the
/// children and attributes of an XML element class as class attributes
/// holding descriptor objects:
///
/// ```python
/// class CT_Foo(BaseOxmlElement):
///     ln = ZeroOrOne("a:ln", successors=("a:extLst",))
///     w = OptionalAttribute("w", ST_Coordinate, default=0)
/// ```
///
/// A metaclass walks the class dictionary when the class is made and calls
/// `populate_class_members` on every descriptor, which `setattr`s methods
/// and properties under computed names (`get_or_add_ln`, `_add_ln`,
/// `_insert_ln`, ...). That is reflection the strict build refuses, and the
/// metaclass `__init__` leaves the whole hierarchy without a layout.
///
/// Every argument of those descriptors is a literal (or a tuple bound in the
/// class body), so this pass runs `populate_class_members` here instead: it
/// replaces each descriptor assignment with the members it would have made,
/// as ordinary statically named methods and properties, removes the
/// metaclass `__init__` and empties the descriptor classes, whose bodies are
/// then unreachable. `_add_to_class` skips a name the class already has, own
/// or inherited, and that rule is kept: a generated name the class body or a
/// base defines is not generated.
///
/// The private adder takes `**attrs` and assigns each with `setattr`. Here it
/// takes, as keyword parameters, exactly the names any call site in the
/// program passes to a method of that name, each assigned as a plain
/// attribute. The pass is all or nothing: if any descriptor or call site
/// cannot be evaluated, the program is left untouched and the reason is
/// recorded in the pass's report.
public final class OxmlDeclarative {

    private static final Set<String> KINDS = new HashSet<String>(Arrays.asList("OptionalAttribute",
            "RequiredAttribute", "OneAndOnlyOne", "OneOrMore", "ZeroOrMore", "ZeroOrOne", "ZeroOrOneChoice",
            "Choice"));
    private static final Set<String> MACHINERY = new HashSet<String>(Arrays.asList("BaseAttribute",
            "_BaseChildElement"));
    private static final String PFX = "_ox_";

    private final Map<String, Node.Module> modules = new LinkedHashMap<String, Node.Module>();
    private final Set<String> packages = new HashSet<String>();
    private final List<String> report = new ArrayList<String>();
    private final Map<String, Set<String>> adderKeywords = new HashMap<String, Set<String>>();
    private final Map<ClassDef, Set<String>> names = new IdentityHashMap<ClassDef, Set<String>>();
    private final Map<ClassDef, String> moduleOfClass = new IdentityHashMap<ClassDef, String>();
    private final Map<ClassDef, Map<Stmt, List<Stmt>>> plans = new IdentityHashMap<ClassDef, Map<Stmt, List<Stmt>>>();
    private final Map<ClassDef, List<Stmt>> hoisted = new IdentityHashMap<ClassDef, List<Stmt>>();
    private final Set<Object> lowered = java.util.Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
    /// Tag -> "module:Class" from literal `register_element_cls` calls; a tag
    /// registered to two classes maps to null.
    private final Map<String, String> registry = new HashMap<String, String>();
    /// A `register_element_cls` call that is not a top-level literal one was
    /// seen, so no tag's class is known.
    private boolean registryOpen;
    private String chem;
    private ClassDef meta;
    private boolean failed;

    private OxmlDeclarative() {
    }

    /// Lowers every declarative element class among `trees` (by path) and
    /// answers why it did not, if it did not.
    public static List<String> rewrite(Map<String, Node.Module> trees) {
        OxmlDeclarative d = new OxmlDeclarative();
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
        d.run();
        return new ArrayList<String>(d.report);
    }

    private void run() {
        if (!findChemistry()) {
            return;
        }
        for (Map.Entry<String, Node.Module> e : modules.entrySet()) {
            for (Stmt s : e.getValue().body) {
                if (s instanceof ClassDef) {
                    moduleOfClass.put((ClassDef) s, e.getKey());
                }
            }
            collectAdders(e.getValue());
            collectRegistry(e.getKey(), e.getValue());
        }
        for (Map.Entry<String, Node.Module> e : modules.entrySet()) {
            for (Stmt s : e.getValue().body) {
                if (s instanceof ClassDef) {
                    namesOf((ClassDef) s);
                }
            }
        }
        for (Map.Entry<String, Node.Module> e : modules.entrySet()) {
            if (!e.getKey().equals(chem)) {
                unlowered(e.getKey(), e.getValue());
            }
        }
        if (failed) {
            report.add(0, "left every declarative class alone");
            return;
        }
        apply();
    }

    /// Records `register_element_cls("p:tag", CT_Cls)` at module level. The
    /// parser's lookup answers that class for every element so tagged, which
    /// is what lets the generated accessors narrow what they return.
    private void collectRegistry(String mod, Node.Module m) {
        Map<String, String> where = new HashMap<String, String>();
        for (Stmt s : m.body) {
            if (s instanceof ClassDef) {
                where.put(((ClassDef) s).name, mod + ":" + ((ClassDef) s).name);
            }
            if (s instanceof ImportFrom && ((ImportFrom) s).level == 0) {
                for (Alias a : ((ImportFrom) s).names) {
                    where.put(a.asname != null ? a.asname : a.name, ((ImportFrom) s).module + ":" + a.name);
                }
            }
        }
        for (Stmt s : m.body) {
            if (!(s instanceof Node.ExprStmt) || !(((Node.ExprStmt) s).value instanceof Call)) {
                continue;
            }
            Call c = (Call) ((Node.ExprStmt) s).value;
            if (!(c.func instanceof Name) || !"register_element_cls".equals(((Name) c.func).id)) {
                continue;
            }
            String tag = c.args.size() == 2 && c.keywords.isEmpty() ? str(c.args.get(0)) : null;
            String k = tag != null && c.args.get(1) instanceof Name ? where.get(((Name) c.args.get(1)).id) : null;
            if (k == null) {
                registryOpen = true;
                report.add("register_element_cls not literal at line " + c.line);
                continue;
            }
            if (registry.containsKey(tag) && !k.equals(registry.get(tag))) {
                k = null;
            }
            registry.put(tag, k);
        }
    }

    /// The class the parser makes for `tag`, as "module:Class", or null.
    private String registered(String tag) {
        return registryOpen ? null : registry.get(tag);
    }

    /// Appends, indented, a narrowing of local `v` (known not None) to the
    /// registered class of `tag`, returning it. The import is local because
    /// the element modules import each other's classes only by tag; by the
    /// time an accessor runs every one of them is loaded. The raise is
    /// unreachable while the registry above holds.
    private String narrow(String mod, String tag, String v, String ind) {
        String k = registered(tag);
        if (k == null) {
            return ind + "return " + v + "\n";
        }
        int i = k.indexOf(':');
        return ind + "from " + k.substring(0, i) + " import " + k.substring(i + 1) + " as _ox_k\n" + ind + "if isinstance("
                + v + ", _ox_k):\n" + ind + "    return " + v + "\n" + ind + "raise "
                + chemName("InvalidXmlError", mod) + "("
                + DeclarativeMap.quote("<" + tag + "> is not a " + k.substring(i + 1)) + ")\n";
    }

    /// Finds the module that defines the descriptor kinds and the metaclass
    /// whose `__init__` calls `populate_class_members`.
    private boolean findChemistry() {
        for (Map.Entry<String, Node.Module> e : modules.entrySet()) {
            Set<String> kinds = new HashSet<String>();
            ClassDef m = null;
            for (Stmt s : e.getValue().body) {
                if (!(s instanceof ClassDef)) {
                    continue;
                }
                ClassDef c = (ClassDef) s;
                if (KINDS.contains(c.name)) {
                    kinds.add(c.name);
                }
                if (c.bases.size() == 1 && c.bases.get(0) instanceof Name && "type".equals(((Name) c.bases.get(0)).id)
                        && populates(c)) {
                    m = c;
                }
            }
            if (m != null && kinds.equals(KINDS)) {
                chem = e.getKey();
                meta = m;
                return true;
            }
        }
        return false;
    }

    private static boolean populates(ClassDef c) {
        for (Stmt s : c.body) {
            if (s instanceof FunctionDef && "__init__".equals(((FunctionDef) s).name)) {
                boolean[] hit = new boolean[1];
                calls(s, hit);
                return hit[0];
            }
        }
        return false;
    }

    private static void calls(Object o, boolean[] hit) {
        if (o instanceof Call && ((Call) o).func instanceof Attribute
                && "populate_class_members".equals(((Attribute) ((Call) o).func).attr)) {
            hit[0] = true;
        }
        Infer.children(o, x -> calls(x, hit));
    }

    /// Fails the pass on a descriptor made anywhere but as a class attribute
    /// this pass lowers: its `populate_class_members` would still run.
    private void unlowered(String mod, Object o) {
        if (o instanceof Call && kindOf(mod, ((Call) o).func) != null && !lowered.contains(o)) {
            report.add(mod + ":" + ((Call) o).line + " a descriptor this pass does not lower");
            failed = true;
        }
        Infer.children(o, x -> unlowered(mod, x));
    }

    /// Records the keyword names every `x._add_<name>(...)` call passes.
    private void collectAdders(Object o) {
        if (o instanceof Call && ((Call) o).func instanceof Attribute
                && ((Attribute) ((Call) o).func).attr.startsWith("_add_")) {
            Call c = (Call) o;
            String a = ((Attribute) c.func).attr;
            Set<String> ks = adderKeywords.get(a);
            if (ks == null) {
                ks = new LinkedHashSet<String>();
                adderKeywords.put(a, ks);
            }
            // The generated adder sets attributes in one order and lxml keeps
            // insertion order, so call sites that order them differently would
            // serialize differently from CPython: those are refused.
            List<String> known = new ArrayList<String>();
            for (Keyword k : c.keywords) {
                if (k.arg != null && ks.contains(k.arg)) {
                    known.add(k.arg);
                }
            }
            List<String> prior = new ArrayList<String>(ks);
            prior.retainAll(known);
            if (!prior.equals(known)) {
                ks.add(null);
            }
            for (Keyword k : c.keywords) {
                if (k.arg == null) {
                    // A spread: the names are not known here.
                    ks.add(null);
                } else {
                    ks.add(k.arg);
                }
            }
        }
        Infer.children(o, this::collectAdders);
    }

    /// The names class `c` has once made, own and inherited, lowering it
    /// (and its bases first) on the way.
    private Set<String> namesOf(ClassDef c) {
        Set<String> n = names.get(c);
        if (n != null) {
            return n;
        }
        n = new HashSet<String>();
        names.put(c, n);
        String mod = moduleOfClass.get(c);
        for (Expr b : c.bases) {
            ClassDef bc = b instanceof Name && mod != null ? resolveClass(mod, ((Name) b).id, 0) : null;
            if (bc != null) {
                n.addAll(namesOf(bc));
            }
        }
        for (Stmt s : c.body) {
            bound(s, n);
        }
        if (mod != null && !mod.equals(chem)) {
            lower(mod, c, n);
        }
        return n;
    }

    private static void bound(Stmt s, Set<String> n) {
        if (s instanceof FunctionDef) {
            n.add(((FunctionDef) s).name);
        } else if (s instanceof ClassDef) {
            n.add(((ClassDef) s).name);
        } else if (s instanceof Assign) {
            for (Expr t : ((Assign) s).targets) {
                if (t instanceof Name) {
                    n.add(((Name) t).id);
                }
            }
        } else if (s instanceof AnnAssign && ((AnnAssign) s).target instanceof Name && ((AnnAssign) s).value != null) {
            // A bare annotation binds nothing: `get_or_add_x: Callable[...]`
            // only declares what the metaclass will add.
            n.add(((Name) ((AnnAssign) s).target).id);
        }
    }

    private ClassDef resolveClass(String mod, String name, int depth) {
        Node.Module m = modules.get(mod);
        if (m == null || depth > 4) {
            return null;
        }
        for (Stmt s : m.body) {
            if (s instanceof ClassDef && name.equals(((ClassDef) s).name)) {
                return (ClassDef) s;
            }
        }
        for (Stmt s : m.body) {
            if (s instanceof ImportFrom) {
                ImportFrom f = (ImportFrom) s;
                for (Alias a : f.names) {
                    if (name.equals(a.asname == null ? a.name : a.asname)) {
                        String from = absolute(mod, f);
                        return from == null ? null : resolveClass(from, a.name, depth + 1);
                    }
                }
            }
        }
        return null;
    }

    private String absolute(String mod, ImportFrom f) {
        if (f.level == 0) {
            return f.module;
        }
        String base = packages.contains(mod) ? mod : parent(mod);
        for (int i = 1; i < f.level && base != null; i++) {
            base = parent(base);
        }
        if (base == null) {
            return null;
        }
        return f.module == null || f.module.isEmpty() ? base : base + "." + f.module;
    }

    private static String parent(String m) {
        int i = m.lastIndexOf('.');
        return i < 0 ? null : m.substring(0, i);
    }

    /// The descriptor kind a call makes, or null.
    private String kindOf(String mod, Expr func) {
        if (!(func instanceof Name)) {
            return null;
        }
        String id = ((Name) func).id;
        Node.Module m = modules.get(mod);
        for (Stmt s : m.body) {
            if (s instanceof ImportFrom) {
                ImportFrom f = (ImportFrom) s;
                for (Alias a : f.names) {
                    if (id.equals(a.asname == null ? a.name : a.asname) && KINDS.contains(a.name)
                            && chem.equals(absolute(mod, f))) {
                        return a.name;
                    }
                }
            }
        }
        return null;
    }

    private void lower(String mod, ClassDef c, Set<String> taken) {
        Map<Stmt, List<Stmt>> plan = new IdentityHashMap<Stmt, List<Stmt>>();
        Set<String> unconditional = new HashSet<String>();
        for (int i = 0; i < c.body.size(); i++) {
            Stmt s = c.body.get(i);
            Expr target;
            Expr value;
            if (s instanceof Assign && ((Assign) s).targets.size() == 1) {
                target = ((Assign) s).targets.get(0);
                value = ((Assign) s).value;
            } else if (s instanceof AnnAssign) {
                target = ((AnnAssign) s).target;
                value = ((AnnAssign) s).value;
            } else {
                continue;
            }
            if (!(target instanceof Name) || !(value instanceof Call)) {
                continue;
            }
            Call v = (Call) value;
            String kind = kindOf(mod, v.func);
            if (kind == null) {
                continue;
            }
            lowered.add(v);
            if (v.func instanceof Name && "ZeroOrOneChoice".equals(kind)) {
                for (Expr a : v.args) {
                    if (a instanceof Node.TupleExpr || a instanceof Node.ListExpr) {
                        lowered.addAll(a instanceof Node.TupleExpr ? ((Node.TupleExpr) a).elts : ((Node.ListExpr) a).elts);
                    }
                }
            }
            String prop = ((Name) target).id;
            String where = mod + ":" + s.line + " " + c.name + "." + prop + " " + kind + ": ";
            StringBuilder b = new StringBuilder();
            String why = member(mod, c, i, kind, prop, v, taken, unconditional, b);
            if (why != null) {
                report.add(where + why);
                failed = true;
                continue;
            }
            String file = mod.replace('.', '/') + ".py";
            Node.Module parsed = Parser.parse("class _ox:\n" + indent(b.toString()), file);
            DataclassInit.atLine(parsed, s.line);
            plan.put(s, ((ClassDef) parsed.body.get(0)).body);
        }
        if (!plan.isEmpty()) {
            // A property the metaclass sets replaces a method of that name in
            // the body (setattr runs after the body).
            for (Stmt s : c.body) {
                if (s instanceof FunctionDef && unconditional.contains(((FunctionDef) s).name)
                        && ((FunctionDef) s).decorators.isEmpty()) {
                    plan.put(s, new ArrayList<Stmt>());
                }
            }
            plans.put(c, plan);
        }
    }

    /// Appends the members descriptor `v` makes to `b`; answers why not.
    private String member(String mod, ClassDef c, int at, String kind, String prop, Call v, Set<String> taken,
            Set<String> unconditional, StringBuilder b) {
        if ("OptionalAttribute".equals(kind) || "RequiredAttribute".equals(kind)) {
            if (v.args.size() < 2 || v.args.size() > 3 || str(v.args.get(0)) == null) {
                return "attribute arguments";
            }
            String type = text(v.args.get(1));
            Expr defExpr = v.args.size() == 3 ? v.args.get(2) : null;
            for (Keyword k : v.keywords) {
                if ("default".equals(k.arg) && defExpr == null) {
                    defExpr = k.value;
                } else {
                    return "keyword " + k.arg;
                }
            }
            String type0 = type;
            type = type0 == null ? hoist(c, prop, "type", v.args.get(1)) : type0;
            String def = defExpr == null ? "None" : text(defExpr);
            def = def == null ? hoist(c, prop, "default", defExpr) : def;
            if ("RequiredAttribute".equals(kind) && defExpr != null) {
                return "default on a required attribute";
            }
            attribute(b, prop, str(v.args.get(0)), type, def, "OptionalAttribute".equals(kind));
            unconditional.add(prop);
            taken.add(prop);
            return null;
        }
        if ("Choice".equals(kind)) {
            return "a choice outside a group";
        }
        if ("ZeroOrOneChoice".equals(kind)) {
            return choiceGroup(mod, c, at, prop, v, taken, unconditional, b);
        }
        if (v.args.isEmpty() || str(v.args.get(0)) == null) {
            return "tag name";
        }
        String tag = str(v.args.get(0));
        List<String> succ = new ArrayList<String>();
        Expr se = v.args.size() > 1 ? v.args.get(1) : null;
        if (v.args.size() > 2) {
            return "positional arguments";
        }
        for (Keyword k : v.keywords) {
            if ("successors".equals(k.arg) && se == null) {
                se = k.value;
            } else {
                return "keyword " + k.arg;
            }
        }
        if ("OneAndOnlyOne".equals(kind) && se != null) {
            return "successors on OneAndOnlyOne";
        }
        if (se != null) {
            succ = strings(mod, c, at, se, 0);
            if (succ == null) {
                return "successors are not a literal sequence";
            }
        }
        if ("OneAndOnlyOne".equals(kind)) {
            b.append("@property\ndef ").append(prop).append("(self):\n    child = self.find(").append(qn(tag))
                    .append(")\n    if child is None:\n        raise ").append(chemName("InvalidXmlError", mod))
                    .append("(").append(DeclarativeMap.quote("required ``<" + tag + ">`` child element not present"))
                    .append(")\n").append(narrow(mod, tag, "child", "    "));
            unconditional.add(prop);
            taken.add(prop);
            return null;
        }
        boolean many = "ZeroOrMore".equals(kind) || "OneOrMore".equals(kind);
        if (many) {
            b.append("@property\ndef ").append(prop).append("_lst(self):\n    return self.findall(")
                    .append(qn(tag)).append(")\n");
            unconditional.add(prop + "_lst");
            taken.add(prop + "_lst");
        } else {
            getter(b, prop, tag, mod);
            unconditional.add(prop);
            taken.add(prop);
        }
        String why = childMethods(mod, prop, tag, succ, taken, b);
        if (why != null) {
            return why;
        }
        if ("OneOrMore".equals(kind) && taken.add("add_" + prop)) {
            b.append("def add_").append(prop).append("(self):\n    return self._add_").append(prop).append("()\n");
        }
        if ("ZeroOrOne".equals(kind)) {
            if (taken.add("get_or_add_" + prop)) {
                b.append("def get_or_add_").append(prop).append("(self):\n    child = self.").append(prop)
                        .append("\n    if child is None:\n        child = self._add_").append(prop)
                        .append("()\n    return child\n");
            }
            if (taken.add("_remove_" + prop)) {
                b.append("def _remove_").append(prop).append("(self):\n    self.remove_all(")
                        .append(DeclarativeMap.quote(tag)).append(")\n");
            }
        }
        return null;
    }

    private String choiceGroup(String mod, ClassDef c, int at, String prop, Call v, Set<String> taken,
            Set<String> unconditional, StringBuilder b) {
        if (v.args.isEmpty() || v.args.size() > 2) {
            return "group arguments";
        }
        Expr choices = v.args.get(0);
        Expr se = v.args.size() > 1 ? v.args.get(1) : null;
        for (Keyword k : v.keywords) {
            if ("successors".equals(k.arg) && se == null) {
                se = k.value;
            } else if ("choices".equals(k.arg) && choices == null) {
                choices = k.value;
            } else {
                return "keyword " + k.arg;
            }
        }
        List<Expr> elts = choices instanceof Node.TupleExpr ? ((Node.TupleExpr) choices).elts
                : choices instanceof Node.ListExpr ? ((Node.ListExpr) choices).elts : null;
        if (elts == null) {
            return "choices are not a literal sequence";
        }
        List<String> succ = se == null ? new ArrayList<String>() : strings(mod, c, at, se, 0);
        if (succ == null) {
            return "successors are not a literal sequence";
        }
        List<String> tags = new ArrayList<String>();
        for (Expr x : elts) {
            if (!(x instanceof Call) || !"Choice".equals(kindOf(mod, ((Call) x).func))) {
                return "a choice that is not Choice(...)";
            }
            Call cc = (Call) x;
            if (cc.args.isEmpty() || str(cc.args.get(0)) == null) {
                return "choice tag name";
            }
            tags.add(str(cc.args.get(0)));
        }
        b.append("@property\ndef ").append(prop).append("(self):\n    return self.first_child_found_in(")
                .append(joined(tags)).append(")\n");
        unconditional.add(prop);
        taken.add(prop);
        for (String tag : tags) {
            String p = tag.indexOf(':') >= 0 ? tag.substring(tag.indexOf(':') + 1) : tag;
            getter(b, p, tag, mod);
            unconditional.add(p);
            taken.add(p);
            String why = childMethods(mod, p, tag, succ, taken, b);
            if (why != null) {
                return why;
            }
            if (taken.add("get_or_change_to_" + p)) {
                b.append("def get_or_change_to_").append(p).append("(self):\n    child = self.").append(p)
                        .append("\n    if child is not None:\n        return child\n    self._remove_").append(prop)
                        .append("()\n    return self._add_").append(p).append("()\n");
            }
        }
        if (taken.add("_remove_" + prop)) {
            b.append("def _remove_").append(prop).append("(self):\n");
            for (String tag : tags) {
                b.append("    self.remove_all(").append(DeclarativeMap.quote(tag)).append(")\n");
            }
            if (tags.isEmpty()) {
                b.append("    pass\n");
            }
        }
        return null;
    }

    private String childMethods(String mod, String p, String tag, List<String> succ, Set<String> taken,
            StringBuilder b) {
        if (taken.add("_new_" + p)) {
            b.append("def _new_").append(p).append("(self):\n    _ox_c = ").append(chemName("OxmlElement", mod))
                    .append("(").append(DeclarativeMap.quote(tag)).append(")\n").append(narrow(mod, tag, "_ox_c", "    "));
        }
        if (taken.add("_insert_" + p)) {
            b.append("def _insert_").append(p).append("(self, child):\n    self.insert_element_before(child");
            for (String s : succ) {
                b.append(", ").append(DeclarativeMap.quote(s));
            }
            b.append(")\n    return child\n");
        }
        if (taken.add("_add_" + p)) {
            Set<String> ks = adderKeywords.get("_add_" + p);
            List<String> kw = ks == null ? new ArrayList<String>() : new ArrayList<String>(ks);
            if (kw.contains(null)) {
                return "a call spreads keywords into _add_" + p + ", or two calls order them differently";
            }
            b.append("def _add_").append(p).append("(self");
            for (String k : kw) {
                b.append(", ").append(k).append('=').append(chemName("_ox_unset", mod));
            }
            b.append("):\n    _ox_child = self._new_").append(p).append("()\n");
            for (String k : kw) {
                b.append("    if ").append(k).append(" is not ").append(chemName("_ox_unset", mod))
                        .append(":\n        _ox_child.").append(k).append(" = ").append(k).append('\n');
            }
            b.append("    self._insert_").append(p).append("(_ox_child)\n    return _ox_child\n");
        }
        return null;
    }

    /// Binds `e` once, at module level just before class `c`, as Python
    /// evaluates it once when the class body runs; answers the name.
    private String hoist(ClassDef c, String prop, String what, Expr e) {
        String n = PFX + c.name + "_" + prop + "_" + what;
        List<Expr> t = new ArrayList<Expr>();
        t.add(new Name(n).at(e.line, e.col));
        List<Stmt> l = hoisted.get(c);
        if (l == null) {
            l = new ArrayList<Stmt>();
            hoisted.put(c, l);
        }
        l.add(new Assign(t, e).at(e.line, e.col));
        return n;
    }

    private void getter(StringBuilder b, String prop, String tag, String mod) {
        if (registered(tag) == null) {
            b.append("@property\ndef ").append(prop).append("(self):\n    return self.find(").append(qn(tag))
                    .append(")\n");
            return;
        }
        b.append("@property\ndef ").append(prop).append("(self):\n    _ox_c = self.find(").append(qn(tag))
                .append(")\n    if _ox_c is None:\n        return None\n").append(narrow(mod, tag, "_ox_c", "    "));
    }

    private static void attribute(StringBuilder b, String prop, String attr, String type, String def,
            boolean optional) {
        String clark = attr.indexOf(':') >= 0 ? PFX + "qn(" + DeclarativeMap.quote(attr) + ")"
                : DeclarativeMap.quote(attr);
        b.append("@property\ndef ").append(prop).append("(self):\n    _ox_v = self.get(").append(clark)
                .append(")\n    if _ox_v is None:\n");
        if (optional) {
            b.append("        return ").append(def).append('\n');
        } else {
            b.append("        raise ").append(PFX).append("InvalidXmlError(")
                    .append(DeclarativeMap.quote("required '" + attr + "' attribute not present on element %s"))
                    .append(" % (self.tag,))\n");
        }
        b.append("    return ").append(type).append(".from_xml(_ox_v)\n@").append(prop).append(".setter\ndef ")
                .append(prop).append("(self, value):\n");
        if (optional) {
            // python-pptx and python-docx both clear the attribute for None too.
            b.append("    if value is None or value == ").append(def).append(":\n        if ").append(clark)
                    .append(" in self.attrib:\n            del self.attrib[").append(clark)
                    .append("]\n        return\n");
        }
        b.append("    self.set(").append(clark).append(", ").append(type).append(".to_xml(value))\n");
    }

    private static String qn(String tag) {
        return PFX + "qn(" + DeclarativeMap.quote(tag) + ")";
    }

    private String chemName(String n, String mod) {
        return mod.equals(chem) ? n : n.startsWith(PFX) ? n : PFX + n;
    }

    private static String joined(List<String> tags) {
        StringBuilder b = new StringBuilder();
        for (String t : tags) {
            if (b.length() > 0) {
                b.append(", ");
            }
            b.append(DeclarativeMap.quote(t));
        }
        return b.toString();
    }

    private static String indent(String s) {
        StringBuilder b = new StringBuilder();
        for (String line : s.split("\n", -1)) {
            if (!line.isEmpty()) {
                b.append("    ").append(line);
            }
            b.append('\n');
        }
        return b.toString();
    }

    private static String str(Expr e) {
        return e instanceof Constant && ((Constant) e).kind == Constant.Kind.STR ? ((Constant) e).text : null;
    }

    /// Python source for a literal, a name or a dotted name; null otherwise.
    private static String text(Expr e) {
        if (e instanceof Name) {
            return ((Name) e).id;
        }
        if (e instanceof Attribute) {
            String v = text(((Attribute) e).value);
            return v == null || !(((Attribute) e).value instanceof Name || ((Attribute) e).value instanceof Attribute)
                    ? null : v + "." + ((Attribute) e).attr;
        }
        if (e instanceof UnaryOp && "-".equals(((UnaryOp) e).op) && ((UnaryOp) e).operand instanceof Constant) {
            Constant k = (Constant) ((UnaryOp) e).operand;
            return k.kind == Constant.Kind.INT || k.kind == Constant.Kind.FLOAT ? "-" + k.text : null;
        }
        if (e instanceof Constant) {
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
                    return DeclarativeMap.quote(k.text);
                default:
                    return null;
            }
        }
        return null;
    }

    /// The strings sequence `e` evaluates to in the body of `c` before
    /// statement `at`, or null.
    private List<String> strings(String mod, ClassDef c, int at, Expr e, int depth) {
        if (depth > 8) {
            return null;
        }
        if (e instanceof Node.TupleExpr || e instanceof Node.ListExpr) {
            List<Expr> elts = e instanceof Node.TupleExpr ? ((Node.TupleExpr) e).elts : ((Node.ListExpr) e).elts;
            List<String> out = new ArrayList<String>();
            for (Expr x : elts) {
                if (str(x) == null) {
                    return null;
                }
                out.add(str(x));
            }
            return out;
        }
        if (e instanceof BinOp && "+".equals(((BinOp) e).op)) {
            List<String> l = strings(mod, c, at, ((BinOp) e).left, depth + 1);
            List<String> r = strings(mod, c, at, ((BinOp) e).right, depth + 1);
            if (l == null || r == null) {
                return null;
            }
            l.addAll(r);
            return l;
        }
        if (e instanceof Subscript && ((Subscript) e).index instanceof Slice) {
            Slice s = (Slice) ((Subscript) e).index;
            List<String> v = strings(mod, c, at, ((Subscript) e).value, depth + 1);
            if (v == null || s.step != null) {
                return null;
            }
            Integer lo = s.lower == null ? Integer.valueOf(0) : intOf(s.lower);
            Integer hi = s.upper == null ? Integer.valueOf(v.size()) : intOf(s.upper);
            if (lo == null || hi == null) {
                return null;
            }
            int a = clamp(lo, v.size());
            int z = clamp(hi, v.size());
            return a >= z ? new ArrayList<String>() : new ArrayList<String>(v.subList(a, z));
        }
        if (e instanceof Name) {
            String id = ((Name) e).id;
            for (int i = at - 1; i >= 0; i--) {
                Stmt s = c.body.get(i);
                if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name
                        && id.equals(((Name) ((Assign) s).targets.get(0)).id)) {
                    return strings(mod, c, i, ((Assign) s).value, depth + 1);
                }
            }
            Expr found = null;
            int count = 0;
            for (Stmt s : modules.get(mod).body) {
                Set<String> n = new HashSet<String>();
                bound(s, n);
                if (n.contains(id)) {
                    count++;
                    found = s instanceof Assign ? ((Assign) s).value : null;
                }
            }
            return count == 1 && found != null ? strings(mod, c, 0, found, depth + 1) : null;
        }
        return null;
    }

    private static int clamp(int i, int n) {
        int r = i < 0 ? n + i : i;
        return r < 0 ? 0 : r > n ? n : r;
    }

    private static Integer intOf(Expr e) {
        String t = text(e);
        if (t == null) {
            return null;
        }
        try {
            return Integer.valueOf(t);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void apply() {
        Set<String> touched = new HashSet<String>();
        for (Map.Entry<ClassDef, Map<Stmt, List<Stmt>>> e : plans.entrySet()) {
            ClassDef c = e.getKey();
            List<Stmt> out = new ArrayList<Stmt>();
            for (Stmt s : c.body) {
                List<Stmt> r = e.getValue().get(s);
                if (r == null) {
                    out.add(s);
                } else {
                    out.addAll(r);
                }
            }
            c.body.clear();
            c.body.addAll(out);
            touched.add(moduleOfClass.get(c));
        }
        String file = chem.replace('.', '/') + ".py";
        Node.Module chemMod = modules.get(chem);
        for (String mod : touched) {
            Node.Module m = modules.get(mod);
            int first = -1;
            for (int i = 0; i < m.body.size() && first < 0; i++) {
                if (m.body.get(i) instanceof ClassDef && plans.containsKey(m.body.get(i))) {
                    first = i;
                }
            }
            for (int i = 0; i < m.body.size(); i++) {
                List<Stmt> h = m.body.get(i) instanceof ClassDef ? hoisted.get(m.body.get(i)) : null;
                if (h != null) {
                    m.body.addAll(i, h);
                    i += h.size();
                }
            }
            Node.Module imp = Parser.parse("from " + chem + " import qn as _ox_qn, OxmlElement as _ox_OxmlElement, "
                    + "InvalidXmlError as _ox_InvalidXmlError, _ox_unset\n", mod.replace('.', '/') + ".py");
            DataclassInit.atLine(imp, m.body.get(first).line);
            m.body.addAll(first, imp.body);
        }
        // The metaclass populates nothing now, and nothing makes a descriptor.
        for (int i = 0; i < chemMod.body.size(); i++) {
            Stmt s = chemMod.body.get(i);
            if (s == meta) { //NOPMD CompareObjectsWithEquals - identity is meant
                // The metaclass is `type` itself now; classes naming it as
                // their metaclass drop the keyword below.
                Node.Module sentinel = Parser.parse("_ox_unset = object()\n" + meta.name + " = type\n", file);
                DataclassInit.atLine(sentinel, s.line);
                chemMod.body.remove(i);
                chemMod.body.addAll(i, sentinel.body);
                i += sentinel.body.size() - 1;
            } else if (s instanceof ClassDef && (KINDS.contains(((ClassDef) s).name)
                    || MACHINERY.contains(((ClassDef) s).name))) {
                emptied((ClassDef) s, file);
            }
        }
        for (Node.Module m : modules.values()) {
            for (Stmt s : m.body) {
                if (s instanceof ClassDef) {
                    dropMetaclass((ClassDef) s);
                }
            }
        }
        report.add("lowered " + plans.size() + " classes");
    }

    private void dropMetaclass(ClassDef c) {
        for (int i = c.keywords.size() - 1; i >= 0; i--) {
            Keyword k = c.keywords.get(i);
            if ("metaclass".equals(k.arg) && k.value instanceof Name && meta.name.equals(((Name) k.value).id)
                    && chem.equals(moduleOfClass.get(c))) {
                c.keywords.remove(i);
            }
        }
    }

    private static void emptied(ClassDef c, String file) {
        Node.Module p = Parser.parse("pass\n", file);
        DataclassInit.atLine(p, c.line);
        c.body.clear();
        c.body.addAll(p.body);
    }
}
